# JavaFX Incubator Modules

## Overview

This document includes instructions for adding a JavaFX incubator module to the JavaFX build.

## Adding an Incubator module

To add an incubator module, do the following:

1. Add an entry for your module in `settings-incubator.gradle` and `build-incubator.gradle` in the designated place
2. Create your module under `modules/jfx.incubator.YOURMODULENAME`, including your source code and test code as is done for other modules
3. Add the needed build logic in `modules/jfx.incubator.YOURMODULENAME/project.gradle`

Here is an example patch:

```
diff --git a/incubator-build.gradle b/incubator-build.gradle
index 330019ca2c..73c9f68b73 100644
--- a/incubator-build.gradle
+++ b/incubator-build.gradle
@@ -33,4 +33,7 @@ ext.incubatorProjectNames = [
     // BEGIN: incubator placeholder
     //'incubator.mymod'
     // END: incubator placeholder
+
+    // TODO: incubator template -- rename module, then remove this TODO comment
+    'incubator.myfeature'
 ]
diff --git a/incubator-settings.gradle b/incubator-settings.gradle
index 11c48f9e2e..0aa1258f6b 100644
--- a/incubator-settings.gradle
+++ b/incubator-settings.gradle
@@ -29,3 +29,7 @@
 //include("incubator.mymod")
 //project(":incubator.mymod").projectDir = file("modules/jfx.incubator.mymod")
 // END: incubator placeholder
+
+// TODO: incubator template -- rename module, then remove this TODO comment
+include("incubator.myfeature")
+project(":incubator.myfeature").projectDir = file("modules/jfx.incubator.myfeature")
diff --git a/modules/jfx.incubator.myfeature/project.gradle b/modules/jfx.incubator.myfeature/project.gradle
new file mode 100644
index 0000000000..be3b2541b3
--- /dev/null
+++ b/modules/jfx.incubator.myfeature/project.gradle
@@ -0,0 +1,118 @@
+/*
+ * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
+ * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
+ *
+ * This code is free software; you can redistribute it and/or modify it
+ * under the terms of the GNU General Public License version 2 only, as
+ * published by the Free Software Foundation.  Oracle designates this
+ * particular file as subject to the "Classpath" exception as provided
+ * by Oracle in the LICENSE file that accompanied this code.
+ *
+ * This code is distributed in the hope that it will be useful, but WITHOUT
+ * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
+ * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
+ * version 2 for more details (a copy is included in the LICENSE file that
+ * accompanied this code).
+ *
+ * You should have received a copy of the GNU General Public License version
+ * 2 along with this work; if not, write to the Free Software Foundation,
+ * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
+ *
+ * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
+ * or visit www.oracle.com if you need additional information or have any
+ * questions.
+ */
+
+// TODO: incubator template -- follow instruction below, then remove this comment
+// To create an incubator module:
+// 1) apply the changes from this patch
+// 2) Look for "TODO: incubator template" comments, and replace "myfeature"
+//    with the name of your feature
+// 3) Refactor / rename the files under "modules/javafx.incubator.myfeature"
+//    to match the name of your feature.
+// 4) Remove this comment block
+project(":incubator.myfeature") {
+    project.ext.buildModule = true
+    project.ext.includeSources = true
+    project.ext.moduleRuntime = true
+    project.ext.moduleName = "jfx.incubator.myfeature"
+    project.ext.incubating = true
+
+    sourceSets {
+        main
+        shims {
+            java {
+                compileClasspath += sourceSets.main.output
+                runtimeClasspath += sourceSets.main.output
+            }
+        }
+        test {
+            java {
+                compileClasspath += sourceSets.shims.output
+                runtimeClasspath += sourceSets.shims.output
+            }
+        }
+    }
+
+    project.ext.moduleSourcePath = defaultModuleSourcePath
+    project.ext.moduleSourcePathShim = defaultModuleSourcePathShim
+
+    commonModuleSetup(project, [ 'base', 'graphics', 'controls', 'incubator.myfeature' ])
+
+    dependencies {
+        testImplementation project(":base").sourceSets.test.output
+        testImplementation project(":graphics").sourceSets.test.output
+        testImplementation project(":controls").sourceSets.test.output
+        implementation project(':base')
+        implementation project(':graphics')
+        implementation project(':controls')
+    }
+
+    test {
+        jvmArgs "-Djavafx.toolkit=test.com.sun.javafx.pgstub.StubToolkit"
+    }
+
+    def modulePath = "${project.sourceSets.main.java.getDestinationDirectory().get().getAsFile()}"
+    modulePath += File.pathSeparator + "${rootProject.projectDir}/modules/javafx.controls/build/classes/java/main"
+    modulePath += File.pathSeparator + "${rootProject.projectDir}/modules/javafx.graphics/build/classes/java/main"
+    modulePath += File.pathSeparator + "${rootProject.projectDir}/modules/javafx.base/build/classes/java/main"
+
+// TODO: incubator template -- follow instruction below, then remove this comment block
+// The following block is used if and only if you have .css resource files
+// in your incubator module. If you do, uncomment the block and make the
+// appropriate changes for the location of your resource files. Otherwise,
+// delete the block.
+
+//    processResources {
+//      doLast {
+//        def cssFiles = fileTree(dir: "$moduleDir/com/sun/javafx/scene/control/skin")
+//        cssFiles.include "**/*.css"
+//        cssFiles.each { css ->
+//            logger.info("converting CSS to BSS ${css}");
+//
+//            javaexec {
+//                executable = JAVA
+//                workingDir = project.projectDir
+//                jvmArgs += patchModuleArgs
+//                jvmArgs += "--module-path=$modulePath"
+//                jvmArgs += "--add-modules=javafx.graphics"
+//                mainClass = "com.sun.javafx.css.parser.Css2Bin"
+//                args css
+//            }
+//        }
+//      }
+//    }
+//
+//    def copyShimBssTask = project.task("copyShimBss", type: Copy,
+//                            dependsOn: [project.tasks.getByName("compileJava"),
+//                                        project.tasks.getByName("processResources")]) {
+//        from project.moduleDir
+//        into project.moduleShimsDir
+//        include "**/*.bss"
+//    }
+//    processShimsResources.dependsOn(copyShimBssTask)
+
+    addMavenPublication(project, [ 'graphics' , 'controls'])
+
+    addValidateSourceSets(project, sourceSets)
+}
```
