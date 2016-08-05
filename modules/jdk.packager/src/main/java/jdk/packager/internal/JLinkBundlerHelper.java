/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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


import static com.oracle.tools.packager.StandardBundlerParam.APP_RESOURCES;
import static com.oracle.tools.packager.StandardBundlerParam.MAIN_CLASS;
import static com.oracle.tools.packager.StandardBundlerParam.MAIN_JAR;
import jdk.tools.jlink.internal.packager.AppRuntimeImageBuilder;

import com.oracle.tools.packager.BundlerParamInfo;
import com.oracle.tools.packager.StandardBundlerParam;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.Log;

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


public class JLinkBundlerHelper {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle(JLinkBundlerHelper.class.getName());

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<List<Path>> MODULE_PATH =
            new StandardBundlerParam<>(
                    I18N.getString("param.module-path.name"),
                    I18N.getString("param.module-path.description"),
                    "module-path",
                    (Class<List<Path>>) (Object)List.class,
                    p -> new ArrayList(),
                    (s, p) -> Arrays.asList(s.split("[;:]")).stream()
                        .map(ss -> new File(ss).toPath())
                        .collect(Collectors.toList()));

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<String> MODULE =
            new StandardBundlerParam<>(
                    I18N.getString("param.main.module.name"),
                    I18N.getString("param.main.module.description"),
                    "module",
                    String.class,
                    p -> null,
                    (s, p) -> {
                        return String.valueOf(s);
                    });

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<Set<String>> ADD_MODULES =
            new StandardBundlerParam<>(
                    I18N.getString("param.add-modules.name"),
                    I18N.getString("param.add-modules.description"),
                    "add-modules",
                    (Class<Set<String>>) (Object) Set.class,
                    p -> new LinkedHashSet(),
                    (s, p) -> new LinkedHashSet<>(Arrays.asList(s.split("[,;: ]+"))));

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<Set<String>> LIMIT_MODULES =
            new StandardBundlerParam<>(
                    I18N.getString("param.limit-modules.name"),
                    I18N.getString("param.limit-modules.description"),
                    "limit-modules",
                    (Class<Set<String>>) (Object) Set.class,
                    p -> new LinkedHashSet(),
                    (s, p) -> new LinkedHashSet<>(Arrays.asList(s.split("[,;: ]+"))));

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<Boolean> STRIP_NATIVE_COMMANDS =
            new StandardBundlerParam<>(
                    I18N.getString("param.strip-executables.name"),
                    I18N.getString("param.strip-executables.description"),
                    "strip-native-commands",
                    Boolean.class,
                    p -> Boolean.TRUE,
                    (s, p) -> Boolean.valueOf(s));

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
    public static final BundlerParamInfo<Integer> DEBUG_PORT =
            new StandardBundlerParam<>(
                    I18N.getString("param.main.module.name"),
                    I18N.getString("param.main.module.description"),
                    "-Xdebug",
                    Integer.class,
                    p -> null,
                    (s, p) -> {
                        return Integer.valueOf(s);
                    });

    public static String ListOfPathToString(List<Path> value) {
        String result = "";

        for (Path path : value) {
            if (result.length() > 0) {
                result += File.pathSeparator;
            }

            result += path.toString();
        }

        return result;
    }

    public static String SetOfStringToString(Set<String> value) {
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
        RelativeFileSet fileset = MAIN_JAR.fetchFrom(params);

        if (fileset != null) {
            result = new File(fileset.getIncludedFiles().iterator().next());
        }

        return result;
    }

    public static String getMainClass(Map<String, ? super Object> params) {
        String result = "";
        File mainJar = getMainJar(params);

        if (mainJar != null) {
            result = MAIN_CLASS.fetchFrom(params);
        }
        else {
            String mainModule = MODULE.fetchFrom(params);

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
        String mainModule = MODULE.fetchFrom(params);

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

    public static void execute(Map<String, ? super Object> params, AbstractAppImageBuilder imageBuilder) throws IOException, Exception {
        List<Path> modulePath = MODULE_PATH.fetchFrom(params);
        Set<String> addModules = ADD_MODULES.fetchFrom(params);
        Set<String> limitModules = LIMIT_MODULES.fetchFrom(params);
        boolean stripNativeCommands = STRIP_NATIVE_COMMANDS.fetchFrom(params);
        Map<String, String> userArguments = JLINK_OPTIONS.fetchFrom(params);
        Path outputDir = imageBuilder.getRoot();
        String excludeFileList = imageBuilder.getExcludeFileList();
        Set<String> jars = getResourceFileJarList(params, Module.JarType.UnnamedJar);
        setupDefaultModulePathIfNecessary(modulePath);
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
                throw new Exception(String.format(I18N.getString("error.srcfiles.contain.modules"), modularJars.toString()));
            }
        }

        ModuleHelper moduleHelper = new ModuleHelper(modulePath, addModules, limitModules);
        addModules.addAll(moduleHelper.modules());

        //--------------------------------------------------------------------
        // Jars

        // Bundle with minimum dependencies that unnamed jars depend on.
        if (detectModules && !jars.isEmpty()) {
            Log.info(String.format(I18N.getString("using.experimental.feature"), "--" + DETECT_MODULES.getID()));
            Collection<String> detectedModules = JDepHelper.calculateModules(jars, modulePath);

            if (!detectedModules.isEmpty()) {
                addModules.addAll(detectedModules);
            }
        }

        Log.info(String.format(I18N.getString("message.modules"), addModules.toString()));

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
    public static Path findModulePath(List<Path> modulePath, String moduleName) {
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

    private static Path setupDefaultModulePathIfNecessary(List<Path> modulePath) {
        Path result = null;
        Path userDefinedJdkModulePath = findModulePath(modulePath, "java.base.jmod");

        //TODO Fix JDK-8158977

        // Add the default JDK module path to the module path.
        if (userDefinedJdkModulePath != null) {
            result = userDefinedJdkModulePath;
        }
        else {
            Path jdkModulePath = Paths.get(System.getProperty("java.home"), "jmods").toAbsolutePath();

            if (jdkModulePath != null && Files.exists(jdkModulePath)) {
                result = jdkModulePath;
                modulePath.add(result);
            }
        }

        if (result == null) {
            Log.info(String.format(I18N.getString("warning.no.jdk.modules.found")));
        }

        return result;
    }

    private static Set<String> getResourceFileJarList(Map<String, ? super Object> params, Module.JarType Query) {
        Set<String> files = new LinkedHashSet();

        for (RelativeFileSet appResources : StandardBundlerParam.APP_RESOURCES_LIST.fetchFrom(params)) {
            for (String resource : appResources.getIncludedFiles()) {
                if (resource.endsWith(".jar")) {
                    String filename = appResources.getBaseDirectory() + File.separator + resource;

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

    /**
     * This helper class
     */
    private static class ModuleHelper {
        private static final Set<String> REDISTRIBUTBLE_MODULES = Set.of(
            "java.base",
            "java.compiler",
            "java.datatransfer",
            "java.desktop",
            "java.httpclient",
            "java.instrument",
            "java.logging",
            "java.management",
            "java.naming",
            "java.prefs",
            "java.rmi",
            "java.scripting",
            "java.security.jgss",
            "java.security.sasl",
            "java.sql",
            "java.sql.rowset",
            "java.xml",
            "java.xml.crypto",
            "javafx.base",
            "javafx.controls",
            "javafx.fxml",
            "javafx.graphics",
            "javafx.media",
            "javafx.swing",
            "javafx.web",
            "jdk.accessibility",
            "jdk.dynalink",
            "jdk.httpserver",
            "jdk.jfr",
            "jdk.jsobject",
            "jdk.management",
            "jdk.management.cmm",
            "jdk.management.jfr",
            "jdk.management.resource",
            "jdk.net",
            "jdk.scripting.nashorn",
            "jdk.sctp",
            "jdk.security.auth",
            "jdk.security.jgss",
            "jdk.unsupported",
            "jdk.vm.cds",
            "jdk.xml.dom");

        // The token for "all modules on the module path"
        private static final String ALL_MODULE_PATH = "ALL-MODULE-PATH";

        public static final String ALL_RUNTIME = "ALL-RUNTIME";

        private final Set<String> modules = new HashSet<>();

        public ModuleHelper(List<Path> paths, Set<String> roots, Set<String> limitMods) {
            boolean found = false;

            for (Iterator<String> iterator = roots.iterator(); iterator.hasNext();) {
                String module = iterator.next();

                switch (module) {
                    case ALL_MODULE_PATH:
                        iterator.remove();

                        if (!found) {
                            modules.addAll(getModuleNamesFromPath(paths));
                            found = true;
                        }
                        break;
                    case ALL_RUNTIME:
                        iterator.remove();

                        if (!found) {
                            modules.addAll(REDISTRIBUTBLE_MODULES);
                            found = true;
                        }
                        break;
                    default:
                        modules.add(module);
                }
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
