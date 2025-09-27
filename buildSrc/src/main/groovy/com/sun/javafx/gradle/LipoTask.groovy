/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

import javax.inject.Inject

class LipoTask extends DefaultTask {
    @InputDirectory File libDir;
    @OutputFile File lib;

    private final ExecOperations execOperations;

    @Inject
    LipoTask(ExecOperations execOperations) {
        this.execOperations = execOperations;
    }

    @TaskAction void compile() {
        List<String> libNames = [];
        List<File> files = libDir.listFiles();
        files.each { file ->
            String fileName = file.getName();
            // process only thin libraries
            if (!file.isDirectory() && (fileName.indexOf("armv7") != -1 || fileName.indexOf("i386") != -1 || fileName.indexOf("arm64") != -1 || fileName.indexOf("x86_64") != -1)) {
                libNames.add(file.getAbsolutePath())
            }
        }
        // Create a fat library (.a)
        execOperations.exec { spec ->
            spec.commandLine("lipo", "-create", "-output", "$lib");
            spec.args(libNames);
        }
    }
}

