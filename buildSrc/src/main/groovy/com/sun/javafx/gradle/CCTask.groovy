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

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.process.ExecOperations

import javax.inject.Inject

class CCTask extends NativeCompileTask {
    @Input String compiler;
    @Optional @Input List<String> linkerOptions = new ArrayList<String>();
    @Optional @InputDirectory File headers;
    @Optional @Input Closure eachOutputFile; // will be given a File and must return a File
    @Input boolean exe = false;

    @Inject
    CCTask(ExecOperations execOperations) {
        super(execOperations);
    }

    protected File outputFile(File sourceFile) {
        final String outFileName = sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf("."));
        new File("$output/${outFileName}.obj");
    }

    protected void doCompile(File sourceFile, File outputFile) {
        if (eachOutputFile != null) {
            outputFile = eachOutputFile(outputFile);
        }

        // or compile sources using CC
        final int lastDot = outputFile.name.lastIndexOf(".");
        final File pdbFile = new File("$output/${lastDot > 0 ? outputFile.name.substring(0, lastDot) + '.pdb' : outputFile.name + '.pdb'}");

        // TODO the PDB file is never being built -- maybe because it is only built during
        // debug builds, otherwise that flag is ignored "/Fd" or "-Fd"
        execCompile { spec ->
            spec.commandLine(compiler);

            // Add in any additional compilation params
            if (params != null) {
                // A little hack. Only use the -std=c99 flag if compiling .c or .m
                if (sourceFile.name.endsWith(".cpp") || sourceFile.name.endsWith(".cc") || sourceFile.name.endsWith(".mm")) {
                    def stripped = params;
                    stripped.remove("-std=c99");
                    spec.args(stripped)
                } else {
                    spec.args(params)
                }
            }

            if (headers != null) spec.args("-I$headers");

            // Add the source roots in as include directories
            sourceRoots.each { root ->
                final File file = root instanceof File ? (File) root : project.file(root)
                if (file.isDirectory()) spec.args("-I$file");
            }

            // Add the name of the source file to compile
            if (project.IS_WINDOWS) {
                if (exe) {
                    final File exeFile = new File("$output/${lastDot > 0 ? outputFile.name.substring(0, lastDot) + '.exe' : outputFile.name + '.exe'}");
                    spec.args(/*"/Fd$pdbFile",*/ "/Fo$outputFile", "/Fe$exeFile", "$sourceFile")
                } else {
                    spec.args(/*"/Fd$pdbFile",*/ "/Fo$outputFile", "$sourceFile");
                }
            } else {
                spec.args(/*"-Fd$pdbFile",*/ "-o", "$outputFile", "$sourceFile");
            }

            // Add any optional linker options -- used now rarely but can be
            // used for any cc task which isn't going to be followed by a
            // link task
            if (linkerOptions != null && !linkerOptions.isEmpty()) {
                spec.args(linkerOptions);
            }

            if (project.IS_WINDOWS){
                spec.environment(project.WINDOWS_NATIVE_COMPILE_ENVIRONMENT);
            }
        }
    }
}
