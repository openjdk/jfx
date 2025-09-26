/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

import javax.inject.Inject

class ExportedSymbolsTask extends DefaultTask {
    @OutputFile File outputFile;
    @InputDirectory File libDir;
    @Optional @Input List<String> excludes;

    private final ExecOperations execOperations;

    @Inject
    ExportedSymbolsTask(ExecOperations execOperations) {
        this.execOperations = execOperations;
    }

    @TaskAction void generateExportedSymbols() {
        // Get symbols only from .a libraries
        List<String> libNames = [];
        List<File> files = libDir.listFiles();
        files.each { file ->
            if (!file.isDirectory() && file.getName().endsWith(".a") && !excludes.contains(file.getName())) {
                libNames.add(file.getAbsolutePath());
            }
        }

        def baos = new ByteArrayOutputStream();

        // Execute nm on .a libraries
        execOperations.exec { spec ->
            spec.commandLine("nm", "-jg");
            spec.args(libNames);
            spec.setStandardOutput(baos);
        };

        def bais = new ByteArrayInputStream(baos.toByteArray());

        outputFile.withWriter { out ->
            bais.eachLine { line ->
                // Remove unnecessary lines
                line = (line =~ /^(?!(_Java|_JNI_)).*$/).replaceFirst("");
                line = (line =~ /^(_Java|_JNI_).*(.eh)$/).replaceFirst("");

                if (!line.isEmpty()) {
                    out.writeLine(line);
                }
            }
        }

        baos.close();
        bais.close();
    }
}

