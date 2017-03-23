/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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


import com.oracle.tools.packager.StandardBundlerParam;
import com.oracle.tools.packager.BundlerParamInfo;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.Log;
import com.oracle.tools.packager.Platform;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jdk.packager.builders.AbstractAppImageBuilder;
import jdk.packager.internal.Module;
import jdk.tools.jlink.internal.packager.AppRuntimeImageBuilder;


public final class JLinkBundlerHelper {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle(JLinkBundlerHelper.class.getName());

    private JLinkBundlerHelper() {}

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<Boolean> DETECT_MODULES =
            new StandardBundlerParam<>(
                    I18N.getString("param.detect-modules.name"),
                    I18N.getString("param.detect-modules.description"),
                    "detect-modules",
                    Boolean.class,
                    p -> Boolean.FALSE,
                    (s, p) -> Boolean.valueOf(s));

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<Map<String, String>> JLINK_OPTIONS =
            new StandardBundlerParam<>(
                    I18N.getString("param.jlink-options.name"),
                    I18N.getString("param.jlink-options.description"),
                    "jlinkOptions",
                    (Class<Map<String, String>>) (Object) Map.class,
                    p -> Collections.emptyMap(),
                    (s, p) -> {
                        try {
                            Properties props = new Properties();
                            props.load(new StringReader(s));
                            return new LinkedHashMap<>((Map)props);
                        } catch (IOException e) {
                            return new LinkedHashMap<>();
                        }
                    });

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<String> JLINK_BUILDER =
            new StandardBundlerParam<>(
                    I18N.getString("param.jlink-builder.name"),
                    I18N.getString("param.jlink-builder.description"),
                    "jlink.builder",
                    String.class,
                    null,
                    (s, p) -> s);

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<Integer> DEBUG =
            new StandardBundlerParam<>(
                    "",
                    "",
                    "-J-Xdebug",
                    Integer.class,
                    p -> null,
                    (s, p) -> {
                        return Integer.valueOf(s);
                    });

    public static String listOfPathToString(List<Path> value) {
        String result = "";

        for (Path path : value) {
            if (result.length() > 0) {
                result += File.pathSeparator;
            }

            result += path.toString();
        }

        return result;
    }

    public static String setOfStringToString(Set<String> value) {
        String result = "";

        for (String element : value) {
            if (result.length() > 0) {
                result += ",";
            }

            result += element;
        }

        return result;
    }

    public static File getMainJar(Map<String, ? super Object> params) {
        File result = null;
        RelativeFileSet fileset = StandardBundlerParam.MAIN_JAR.fetchFrom(params);

        if (fileset != null) {
            String filename = fileset.getIncludedFiles().iterator().next();
            result = fileset.getBaseDirectory().toPath().resolve(filename).toFile();

            if (result == null || !result.exists()) {
                String srcdir = StandardBundlerParam.SOURCE_DIR.fetchFrom(params);

                if (srcdir != null) {
                    result = new File(srcdir + File.separator + filename);
                }
            }
        }

        return result;
    }

    public static String getMainClass(Map<String, ? super Object> params) {
        String result = "";
        File mainJar = getMainJar(params);

        if (mainJar != null) {
            result = StandardBundlerParam.MAIN_CLASS.fetchFrom(params);
        }
        else {
            String mainModule = StandardBundlerParam.MODULE.fetchFrom(params);

            if (mainModule != null) {
                int index = mainModule.indexOf("/");

                if (index > 0) {
                    result = mainModule.substring(index + 1);
                }
            }
        }

        return result;
    }

    public static String getMainModule(Map<String, ? super Object> params) {
        String result = "";
        String mainModule = StandardBundlerParam.MODULE.fetchFrom(params);

        if (mainModule != null) {
            int index = mainModule.indexOf("/");

            if (index > 0) {
                result = mainModule.substring(0, index);
            }
            else {
                result = mainModule;
            }
        }

        return result;
    }

    public static String getJDKVersion(Map<String, ? super Object> params) {
        String result = "";
        List<Path> modulePath = StandardBundlerParam.MODULE_PATH.fetchFrom(params);
        Set<String> limitModules = StandardBundlerParam.LIMIT_MODULES.fetchFrom(params);
        Path javaBasePath = findPathOfModule(modulePath, "java.base.jmod");
        Set<String> addModules = getRedistributableModules(modulePath, StandardBundlerParam.ADD_MODULES.fetchFrom(params), limitModules);

        if (javaBasePath != null && javaBasePath.toFile().exists()) {
            result = RedistributableModules.getModuleVersion(javaBasePath.toFile(),
                        modulePath, addModules, limitModules);
        }

        return result;
    }

    public static Path getJDKHome(Map<String, ? super Object> params) {
        Path result = null;
        List<Path> modulePath = StandardBundlerParam.MODULE_PATH.fetchFrom(params);
        Path javaBasePath = findPathOfModule(modulePath, "java.base.jmod");

        if (javaBasePath != null && javaBasePath.toFile().exists()) {
            result = javaBasePath.getParent();

            // On a developer build the JDK Home isn't where we expect it
            // relative to the jmods directory. Do some extra
            // processing to find it.
            if (result != null) {
                boolean found = false;
                Path bin = result.resolve("bin");

                if (Files.exists(bin)) {
                    final String exe = (Platform.getPlatform() == Platform.WINDOWS) ? ".exe" : "";
                    Path javaExe = bin.resolve("java" + exe);

                    if (Files.exists(javaExe)) {
                        found = true;
                    }
                }

                if (!found) {
                    result = result.resolve(".." + File.separator + "jdk");
                }
            }
        }

        return result;
    }

    private static Set<String> getRedistributableModules(List<Path> modulePath, Set<String> addModules, Set<String> limitModules) {
        ModuleHelper moduleHelper = new ModuleHelper(modulePath, addModules, limitModules);
        return removeInvalidModules(modulePath, moduleHelper.modules());
    }

    public static void execute(Map<String, ? super Object> params, AbstractAppImageBuilder imageBuilder) throws IOException, Exception {
        List<Path> modulePath = StandardBundlerParam.MODULE_PATH.fetchFrom(params);
        Set<String> addModules = StandardBundlerParam.ADD_MODULES.fetchFrom(params);
        Set<String> limitModules = StandardBundlerParam.LIMIT_MODULES.fetchFrom(params);
        boolean stripNativeCommands = StandardBundlerParam.STRIP_NATIVE_COMMANDS.fetchFrom(params);
        Map<String, String> userArguments = JLINK_OPTIONS.fetchFrom(params);
        Path outputDir = imageBuilder.getRoot();
        String excludeFileList = imageBuilder.getExcludeFileList();
        Set<String> jars = getResourceFileJarList(params, Module.JarType.UnnamedJar);
        File mainJar = getMainJar(params);
        Module.ModuleType mainJarType = Module.ModuleType.Unknown;

        if (mainJar != null) {
            mainJarType = new Module(mainJar).getModuleType();
        }

        //--------------------------------------------------------------------
        // Modules

        boolean detectModules = DETECT_MODULES.fetchFrom(params);

        // The default for an unnamed jar is ALL_DEFAULT with the
        // non-redistributable modules removed.
        if (mainJarType == Module.ModuleType.UnnamedJar && !detectModules) {
            addModules.add(ModuleHelper.ALL_RUNTIME);
        }
        else if (mainJarType == Module.ModuleType.Unknown || mainJarType == Module.ModuleType.ModularJar) {
            String mainModule = getMainModule(params);
            addModules.add(mainModule);

            // Error if any of the srcfiles are modular jars.
            Set<String> modularJars = getResourceFileJarList(params, Module.JarType.ModularJar);

            if (!modularJars.isEmpty()) {
                throw new Exception(MessageFormat.format(I18N.getString("error.srcfiles.contain.modules"), modularJars.toString()));
            }
        }

        Set<String> redistModules = getRedistributableModules(modulePath, addModules, limitModules);
        addModules.addAll(redistModules);

        //--------------------------------------------------------------------
        // Jars

        // Bundle with minimum dependencies that unnamed jars depend on.
        if (detectModules && !jars.isEmpty()) {
            Log.info(MessageFormat.format(I18N.getString("using.experimental.feature"), "--" + DETECT_MODULES.getID()));
            Collection<String> detectedModules = JDepHelper.calculateModules(jars, modulePath);

            if (!detectedModules.isEmpty()) {
                addModules.addAll(detectedModules);
            }
        }

        Log.info(MessageFormat.format(I18N.getString("message.modules"), addModules.toString()));

        if (StandardBundlerParam.VERBOSE.fetchFrom(params)) {
            Log.info("outputDir = " + outputDir.toString());
            Log.info("modulePath = " + modulePath.toString());
            Log.info("addModules = " + addModules.toString());
            Log.info("limitModules = " + limitModules.toString());
            Log.info("excludeFileList = " + excludeFileList);
            Log.info("stripNativeCommands = " + stripNativeCommands);
            Log.info("userArguments = " + userArguments.toString());
        }

        AppRuntimeImageBuilder appRuntimeBuilder = new AppRuntimeImageBuilder();
        appRuntimeBuilder.setOutputDir(outputDir);
        appRuntimeBuilder.setModulePath(modulePath);
        appRuntimeBuilder.setAddModules(addModules);
        appRuntimeBuilder.setLimitModules(limitModules);
        appRuntimeBuilder.setExcludeFileList(excludeFileList);
        appRuntimeBuilder.setStripNativeCommands(stripNativeCommands);
        appRuntimeBuilder.setUserArguments(userArguments);

        appRuntimeBuilder.build();
        imageBuilder.prepareApplicationFiles();
    }

    // Returns the path to the JDK modules in the user defined module path.
    public static Path findPathOfModule(List<Path> modulePath, String moduleName) {
        Path result = null;

        for (Path path : modulePath) {
            Path moduleNamePath = path.resolve(moduleName);

            if (Files.exists(moduleNamePath)) {
                result = path;
                break;
            }
        }

        return result;
    }

    private static Set<String> getResourceFileJarList(Map<String, ? super Object> params, Module.JarType Query) {
        Set<String> files = new LinkedHashSet();

        String srcdir = StandardBundlerParam.SOURCE_DIR.fetchFrom(params);

        for (RelativeFileSet appResources : StandardBundlerParam.APP_RESOURCES_LIST.fetchFrom(params)) {
            for (String resource : appResources.getIncludedFiles()) {
                if (resource.endsWith(".jar")) {
                    String filename = srcdir + File.separator + resource;

                    switch (Query) {
                        case All: {
                            files.add(filename);
                            break;
                        }
                        case ModularJar: {
                            Module module = new Module(new File(filename));

                            if (module.getModuleType() == Module.ModuleType.ModularJar) {
                                files.add(filename);
                            }
                            break;
                        }
                        case UnnamedJar: {
                            Module module = new Module(new File(filename));

                            if (module.getModuleType() == Module.ModuleType.UnnamedJar) {
                                files.add(filename);
                            }
                            break;
                        }
                    }
                }
            }
        }

        return files;
    }

    private static Set<String> removeInvalidModules(List<Path> modulePath, Set<String> modules) {
        Set<String> result = new LinkedHashSet();
        ModuleManager mm = new ModuleManager(modulePath);
        List<Module> lmodules = mm.getModules(EnumSet.of(ModuleManager.SearchType.ModularJar,
                                              ModuleManager.SearchType.Jmod,
                                              ModuleManager.SearchType.ExplodedModule));

        HashMap<String, Module> validModules = new HashMap<>();

        for (Module module : lmodules) {
            validModules.put(module.getModuleName(), module);
        }

        for (String name : modules) {
            if (validModules.containsKey(name)) {
                result.add(name);
            }
            else {
                Log.info(MessageFormat.format(I18N.getString("warning.module.does.not.exist"), name));
            }
        }

        return result;
    }

    private static class ModuleHelper {
        // The token for "all modules on the module path".
        private static final String ALL_MODULE_PATH = "ALL-MODULE-PATH";

        // The token for "all redistributable runtime modules".
        public static final String ALL_RUNTIME = "ALL-RUNTIME";

        private final Set<String> modules = new HashSet<>();
        private enum Macros {None, AllModulePath, AllRuntime}

        public ModuleHelper(List<Path> paths, Set<String> roots, Set<String> limitMods) {
            Macros macro = Macros.None;

            for (Iterator<String> iterator = roots.iterator(); iterator.hasNext();) {
                String module = iterator.next();

                switch (module) {
                    case ALL_MODULE_PATH:
                        iterator.remove();
                        macro = Macros.AllModulePath;
                        break;
                    case ALL_RUNTIME:
                        iterator.remove();
                        macro = Macros.AllRuntime;
                        break;
                    default:
                        this.modules.add(module);
                }
            }

            switch (macro) {
                case AllModulePath:
                    this.modules.addAll(getModuleNamesFromPath(paths));
                    break;
                case AllRuntime:
                    Set<String> m = RedistributableModules.getRedistributableModules(paths);

                    if (m != null) {
                        this.modules.addAll(m);
                    }

                    break;
            }
        }

        public Set<String> modules() {
            return modules;
        }

        private static Set<String> getModuleNamesFromPath(List<Path> Value) {
                Set<String> result = new LinkedHashSet();
                ModuleManager mm = new ModuleManager(Value);
                List<Module> modules = mm.getModules(EnumSet.of(ModuleManager.SearchType.ModularJar,
                                                     ModuleManager.SearchType.Jmod,
                                                     ModuleManager.SearchType.ExplodedModule));

                for (Module module : modules) {
                    result.add(module.getModuleName());
                }

                return result;
        }
    }
}
