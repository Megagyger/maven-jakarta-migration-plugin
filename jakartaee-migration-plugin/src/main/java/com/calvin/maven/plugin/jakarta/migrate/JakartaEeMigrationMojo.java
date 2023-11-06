package com.calvin.maven.plugin.jakarta.migrate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;


@Mojo(name = "migrate-javax-namespace", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class JakartaEeMigrationMojo extends AbstractMojo {

  final List<String> javaxNamespaceCandidates = List.of(
      "javax/activation",
      "javax/jms",
      "javax/servlet",
      "javax/ws/rs",
      "javax/xml/bind",
      "javax/annotation"//leaving for demo purposes but can remove as I believe both can be on the classpath https://central.sonatype.com/search?q=g:javax.annotation%20%20a:javax.annotation-api&smo=true
  );

  static final String JAVAX_PREFIX = "javax/";


  /**
   * Scope to filter the dependencies.
   */
  @Parameter(property = "migratedDirectory", required = true)
  String migratedDirectory;
  /**
   * Gives access to the Maven project information.
   */
  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  MavenProject project;

  public void execute() throws MojoExecutionException, MojoFailureException {
    File[] artifactsDetected = new File(migratedDirectory).listFiles();
    if (artifactsDetected.length <= 0) {
      getLog().warn(
          "No changes necessary as no artifacts detected in " +
              project.getBuild().getDirectory() + "/" + migratedDirectory
              + "If this isnt expected check you have the maven-dependency plugin configured to copy"
              + " the jars you want to migrate or else remove this plugin from your build");
    }

    Arrays.stream(new File(project.getBuild().getDirectory() + "/jakarta-dependencies").listFiles())
        .forEach(artifact -> {
          List<byte[]> classes;
          try {
            classes = loadClasses(artifact);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          var outputFiles = new HashMap<String, byte[]>();
          boolean jarModified = false;

          for (byte[] bytes : classes) {
            ClassNode classNode = getNode(bytes);
            boolean[] classModified = {false};
            ClassWriter writer = new ClassWriter(0);

            classNode.accept(new ClassRemapper(writer, new Remapper() {
              @Override
              public String map(String internalName) {
                if (javaxNamespaceCandidates.stream().anyMatch(internalName::startsWith)) {
                  classModified[0] = true;
                  return "jakarta/" + internalName.substring(JAVAX_PREFIX.length());
                }
                return internalName;
              }
            }));
            if (classModified[0]) {
              outputFiles.put(classNode.name, writer.toByteArray());
              jarModified = true;
            } else {
              outputFiles.put(classNode.name, bytes);
            }
          }

          if (jarModified) {
            getLog().info("Saving modified jar: " + artifact.getAbsolutePath());
            try {
              outputFiles.putAll(loadNonClasses(artifact));
              saveJar(outputFiles, artifact.getAbsolutePath());
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          } else {
            getLog().warn(
                "No namespace to change. Remove the dependency from the migration plugin "
                    + artifact.getAbsolutePath());
          }
        });
  }

  static List<byte[]> loadClasses(File jarFile) throws IOException {
    var classes = new ArrayList<byte[]>();
    var jar = new JarFile(jarFile);
    jar.stream().forEach(entry -> {
      try {
        readJar(jar, entry, classes);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    jar.close();
    return classes;
  }


  static List<byte[]> readJar(JarFile jar, JarEntry entry, List<byte[]> classes)
      throws IOException {
    var classInputStream = jar.getInputStream(entry);
    if (!entry.getName().endsWith(".class")) {
      return classes;
    }
    classes.add(classInputStream.readAllBytes());
    classInputStream.close();
    return classes;
  }

  static ClassNode getNode(byte[] bytes) {
    var cr = new ClassReader(bytes);
    var cn = new ClassNode();
    cr.accept(cn, ClassReader.EXPAND_FRAMES);
    return cn;
  }

  static Map<String, byte[]> loadNonClasses(File jarFile) throws IOException {
    var files = new HashMap<String, byte[]>();
    var jar = new ZipInputStream(new FileInputStream(jarFile));
    ZipEntry entry;
    while ((entry = jar.getNextEntry()) != null) {
      String name = entry.getName();
      if (name.endsWith(".class") || entry.isDirectory()) {
        continue;
      }
      files.put(name, jar.readAllBytes());
    }
    jar.close();
    return files;
  }

  static void saveJar(Map<String, byte[]> files, String pathToJar) throws IOException {
    var jar = new JarOutputStream(new FileOutputStream(pathToJar));
    files.forEach((name, bytes) -> {
      var ext = name.contains(".") ? "" : ".class";
      try {
        jar.putNextEntry(new ZipEntry(name + ext));
        jar.write(bytes);
        jar.closeEntry();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    jar.close();
  }
}
