package com.calvin.usage.example;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Arrays;
import org.junit.jupiter.api.Test;


class MigratedPackageTest {

  @Test
  void whenMigratedDependency_shouldNotComeFromLocalRepository() {
    String className = "edu.umd.cs.findbugs.annotations.Nullable";
    try {
      Class<?> clazz = Class.forName(className);
      ProtectionDomain protectionDomain = clazz.getProtectionDomain();
      CodeSource codeSource = protectionDomain.getCodeSource();
      URL location = codeSource.getLocation();
      assertThat(location.getFile()).doesNotContain(".m2");

    } catch (ClassNotFoundException e) {
      System.err.println("Class not found: " + className);
    }
  }


  @Test
  void whenUsingMigratedJar_shouldNotHaveJavaxAnnotationImports() {
    String className = "edu.umd.cs.findbugs.annotations.Nullable";
    try {
      Class<?> loadedClass = Class.forName(className);
      Arrays.stream(loadedClass.getAnnotations()).sequential().forEach(System.out::println);
      Package[] importedPackages = loadedClass.getPackage().getPackages();

      if (importedPackages.length > 0) {
        for (Package importedPackage : importedPackages) {
          if (importedPackage.getName().contains("annotation")) {
            assertThat(importedPackage.getName()).doesNotContain("javax");
          }
          if (importedPackage.getName().contains("jakarta")) {
            System.out.println(importedPackage);
          }
        }
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException();
    }
  }
}