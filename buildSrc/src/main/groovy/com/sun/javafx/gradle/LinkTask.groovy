/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction


class LinkTask extends DefaultTask {
    @Input List<String> linkParams = new ArrayList<String>();
    @InputDirectory File objectDir;
    @OutputFile File lib;
    @Input String linker;

    @TaskAction void compile() {
        // Link & generate the library (.dll, .so, .dylib)
        lib.getParentFile().mkdirs();
        project.exec({
            commandLine(linker);
            if ((project.IS_LINUX) && (project.IS_STATIC_BUILD)) {
                if (linker.equals("ld")) {
                    args("-r");
                    args("-o");
                } else {
                    args("rcs");
                }
                args("$lib");
            }
            // Exclude parfait files (.bc)
            args(objectDir.listFiles().findAll{ !it.getAbsolutePath().endsWith(".bc") });
            if (project.IS_WINDOWS) {
                args("/out:$lib");
            } else {
                if (! ((project.IS_LINUX) && (project.IS_STATIC_BUILD))) {
                    args("-o", "$lib");
                }
            }
            if (project.IS_DEBUG_NATIVE && !project.IS_WINDOWS) args("-g");
            if (linkParams != null) args(linkParams);
            if (project.IS_WINDOWS){
                final String libPath = lib.toString();
                final String libPrefix = libPath.substring(0, libPath.lastIndexOf("."))
                args("/pdb:${libPrefix}.pdb",
                    "/map:${libPrefix}.map");
                environment(project.WINDOWS_NATIVE_COMPILE_ENVIRONMENT);
            }
        });
    }
}

