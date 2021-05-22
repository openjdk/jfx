/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet

class JavaHeaderTask extends DefaultTask {
    @OutputDirectory File output;
    @Input List sourceRoots = new ArrayList();
    @Input FileCollection classpath;
    private final PatternFilterable patternSet = new PatternSet();

//    @InputFiles public void setSource(Object source) {
//        sourceRoots.clear();
//        sourceRoots.add(source);
//    }
//
    public JavaHeaderTask source(Object... sources) {
        for (Object source : sources) {
            sourceRoots.add(source);
        }
        return this;
    }

    public JavaHeaderTask include(Iterable includes) {
        patternSet.include(includes);
        return this;
    }

    public JavaHeaderTask include(String... includes) {
        patternSet.include(includes);
        return this;
    }

    public JavaHeaderTask exclude(Iterable excludes) {
        patternSet.exclude(excludes);
        return this;
    }

    @TaskAction void runJavaH() {
        // For each .java file we need to figure out what class
        // the .java file belongs in and convert to a class name.
        List classNames = [];
        def ps = patternSet;
        sourceRoots.each { root ->
            logger.info("javah: looking for classes in source root '$root'")
            FileTree files = project.files(root).getAsFileTree().matching(ps);
            files.visit({ fileTreeElement ->
                if (!fileTreeElement.isDirectory() && fileTreeElement.getName().endsWith(".class")) {
                    logger.info("javah:\tconsidering file '$fileTreeElement.name'")
                    String path = fileTreeElement.getPath();
                    String className = path.substring(0, path.length() - 6).replace("/", ".");
                    boolean skip = false;
                    int dollar = className.lastIndexOf('$');
                    if (dollar > 0) {
                        String lastPart = className.substring(dollar + 1);
                        skip = lastPart.matches("[0123456789]*");
                    }
                    if (!skip) classNames.add(className)
                }
            })
        }
        // Execute javah
        project.exec({
            commandLine("$project.JAVAH", "-d", "$output", "-classpath", "${classpath.asPath}");
            args(classNames);
        });
    }
}

