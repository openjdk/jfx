/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.packager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public final class Module {
    private String FFileName;
    private ModuleType FModuleType;

    private enum JarType {Unknown, UnnamedJar, ModularJar}


    public enum ModuleType {Unknown, UnnamedJar, ModularJar, Jmod, ExplodedModule}

    public Module(File AFile) {
        super();
        FFileName = AFile.getPath();
        FModuleType = getModuleType(AFile);
    }

    public String getFileName() {
        return FFileName;
    }

    public String getModulePath() {
        File file = new File(getFileName());
        return file.getParent();
    }

    public String getModuleName() {
        File file = new File(getFileName());
        return getFileWithoutExtension(file.getName());
    }

    public ModuleType getModuleType() {
        return FModuleType;
    }

    public List<Module> getRequiredModules() {
        List<Module> result = new ArrayList();

        List<String> files = new ArrayList();
        files.add(getFileName());
        Collection<String> detectedModules = JDepHelper.calculateModules(files, null);

        for (String filename : detectedModules) {
            Module module = new Module(new File(filename));
            result.add(module);
        }

        return result;
    }

    private static ModuleType getModuleType(File AFile) {
        ModuleType result = ModuleType.Unknown;
        String filename = AFile.getAbsolutePath();

        if (AFile.isFile()) {
            if (filename.endsWith(".jmod")) {
                result = ModuleType.Jmod;
            }
            else if (filename.endsWith(".jar")) {
                JarType status = isModularJar(filename);

                if (status == JarType.ModularJar) {
                    result = ModuleType.ModularJar;
                }
                else if (status == JarType.UnnamedJar) {
                    result = ModuleType.UnnamedJar;
                }
            }
        }
        else if (AFile.isDirectory()) {
            File moduleInfo = new File(filename + File.separator + "module-info.class");

            if (moduleInfo.exists()) {
                result = ModuleType.ExplodedModule;
            }
        }

        return result;
    }

    private static JarType isModularJar(String FileName) {
        JarType result = JarType.Unknown;
        List<String> classNames = new ArrayList<String>();

        try {
            ZipInputStream zip = new ZipInputStream(new FileInputStream(FileName));
            result = JarType.UnnamedJar;

            try {
                for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    if (entry.getName().matches("module-info.class")) {
                        result = JarType.ModularJar;
                        break;
                    }
                }

                zip.close();
            } catch (IOException ex) {
            }
        } catch (FileNotFoundException e) {
        }

        return result;
    }

    private static String getFileWithoutExtension(String FileName) {
        return FileName.replaceFirst("[.][^.]+$", "");
    }
}
