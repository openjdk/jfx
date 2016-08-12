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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.lang.reflect.Layer;
import java.lang.reflect.Module;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This program gets a list of all the modules in the current boot layer,
 * and saves that list (removing the excluded modules) and saving it to disk
 * to be included in the jdk.packager.jmod for use by the Java Packager.
 */
public final class RedistributableFiles {
    private static final String JAVA_SE = "java.se";

    private static final String ADD_MODULES = "--add-modules";
    private static final String MODULE_PATH = "--module-path";
    private static final String EXCLUDE_MODULES = "--exclude-modules";
    private static final String EXCLUDE_FILELIST = "--exclude-filelist";
    private static final String OUT_FILE = "--out-file";

    private RedistributableFiles() {}

    static private Set<String> defaultPlatformModules() {
        return Layer.boot()
                    .modules()
                    .stream()
                    .map(Module::getName)
                    .sorted()
                    .collect(Collectors.toSet());
    }

    private static String nextArg(String args[], int i) {
        return (i == args.length - 1) ? "" : args[i + 1];
    }

    private static Set<String> loadFromFile(String filename) {
        Set<String> result = null;
        BufferedReader br = null;

        File file = new File(filename);

        if (file.exists()) {
            FileInputStream stream = null;

            try {
                stream = new FileInputStream(file);
            } catch (FileNotFoundException ex) {
            }

            if (stream != null) {
                try {
                    br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                } catch (UnsupportedEncodingException ex) {
                }

                if (br != null) {
                    result = new LinkedHashSet<>();
                    String line;

                    try {
                        while ((line = br.readLine()) != null) {
                            result.add(line);
                        }
                    } catch (IOException ex) {
                    }
                }
            }
        }

        return result;
    }

    private static void saveToFile(String filename, List<String> modules) throws Exception {
        PrintWriter writer = null;

        try {
            File file = new File(filename);
            file.getParentFile().mkdirs();
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"));

            for (String s : modules) {
                writer.println(s);
            }

            writer.flush();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Set<String> addModules = null;
        List<Path> modulePath = null;
        Set<String> excludeModules = null;
        String outfile = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            String next = nextArg(args, i++);

            switch (arg) {
                case ADD_MODULES:
                    System.out.println(ADD_MODULES + "=" + next);
                    addModules = new LinkedHashSet<String>(Arrays.asList(next.split(",")));
                    break;
                case MODULE_PATH:
                    System.out.println(MODULE_PATH + "=" + next);
                    modulePath = Arrays.asList(next.split("[;:]")).stream()
                                       .map(ss -> new File(ss).toPath())
                                       .collect(Collectors.toList());
                    break;
                case EXCLUDE_MODULES:
                    System.out.println(EXCLUDE_MODULES + "=" + next);
                    excludeModules = new LinkedHashSet<String>(Arrays.asList(next.split(",")));
                    saveToFile("ExcludedModules.list", new ArrayList<>(excludeModules));
                    break;
                case EXCLUDE_FILELIST:
                    excludeModules = loadFromFile(next);
                    break;
                case OUT_FILE:
                    System.out.println(OUT_FILE + "=" + next);
                    outfile = next;
                    break;
            }
        }

        if (addModules == null) {
            addModules = new LinkedHashSet<>();
        }

        if (modulePath != null && outfile != null) {
            Set<String> results = defaultPlatformModules();
            results.addAll(addModules);

            if (excludeModules != null) {
                for (String module : excludeModules) {
                    results.remove(module);
                }
            }

            List<String> list = new ArrayList<>(results);
            Collections.sort(list);
            saveToFile(outfile, list);
        }
    }
}
