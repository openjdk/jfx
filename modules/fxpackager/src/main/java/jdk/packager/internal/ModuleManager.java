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
package jdk.packager.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public final class ModuleManager {
    private List<String> folders = new ArrayList();

    public enum SearchType {UnnamedJar, ModularJar, Jmod, ExplodedModule}

    public ModuleManager(String folders) {
        super();
        String lfolders = folders.replaceAll("^\"|\"$", "");
        List<Path> paths = new ArrayList();

        for (String folder : Arrays.asList(lfolders.split(File.pathSeparator))) {
            File file = new File(folder);
            paths.add(file.toPath());
        }

        initialize(paths);
    }

    public ModuleManager(List<Path> Paths) {
        super();
        initialize(Paths);
    }

    private void initialize(List<Path> Paths) {
        for (Path path : Paths) {
            folders.add(path.toString().replaceAll("^\"|\"$", ""));
        }
    }

    public List<Module> getModules() {
        return getModules(EnumSet.of(SearchType.UnnamedJar,
                SearchType.ModularJar, SearchType.Jmod, SearchType.ExplodedModule));
    }

    public List<Module> getModules(EnumSet<SearchType> Search) {
        List<Module> result = new ArrayList();

        for (String folder : folders) {
            result.addAll(getAllModulesInDirectory(folder, Search));
        }

        return result;
    }

    private static List<Module> getAllModulesInDirectory(String Folder, EnumSet<SearchType> Search) {
        List<Module> result = new ArrayList();
        File lfolder = new File(Folder);
        File[] files = lfolder.listFiles();

        for (File file : files) {
            Module module = new Module(file);

            switch (module.getModuleType()) {
                case Unknown:
                    break;
                case UnnamedJar:
                    if (Search.contains(SearchType.UnnamedJar)) {
                        result.add(module);
                    }
                    break;
                case ModularJar:
                    if (Search.contains(SearchType.ModularJar)) {
                        result.add(module);
                    }
                    break;
                case Jmod:
                    if (Search.contains(SearchType.Jmod)) {
                        result.add(module);
                    }
                    break;
                case ExplodedModule:
                    if (Search.contains(SearchType.ExplodedModule)) {
                        result.add(module);
                    }
                    break;
            }
        }

        return result;
    }
}