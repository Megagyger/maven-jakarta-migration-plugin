## Jakarta EE Migration Maven

This plugin is intended to be used to rename the jakarta namespace for external dependencies we have no control over.\
This is due to SpringBoot3 and JakartaEE9 usage. \
Therefore instead of forking we can use ASM to change the bytecode at compile time. 
Check the usage-example module for usage


### Usage Example
Basic App to demonstrate using an ASM remapper on a list of deps handed to the plugin.\
_maven-dependency-plugin_ is configured to copy **findbugs** dependencies to a custom configured dir.\
The migration plugin will then update the **javax** namespace to **jakarta** if it finds any matching the list of javax namespaces in the mojo for those deps.

Examine the classes after the build completes in the migrated directory to see the change.\
NOTE: Currently not working with spring boot maven plugin. The bootified jar contains the original findbugs and not the migrated jar
