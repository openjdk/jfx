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

package com.oracle.tools.packager;

import jdk.tools.jlink.Jlink;
import jdk.tools.jlink.builder.ImageBuilder;
import jdk.tools.jlink.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;


public class JLinkBundlerHelper {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle(JLinkBundlerHelper.class.getName());

    //TODO Remove and replace with programmatic implementation JDK-8149975
    private static final String[] JRE_MODULES = {"java.se",
                                                "java.smartcardio",
                                                "javafx.base",
                                                "javafx.controls",
                                                "javafx.deploy",
                                                "javafx.fxml",
                                                "javafx.graphics",
                                                "javafx.media",
                                                "javafx.swing",
                                                "javafx.web",
                                                "javafx.base",
                                                "javafx.deploy",
                                                "javafx.graphics",
                                                "javafx.swing",
                                                "javafx.controls",
                                                "javafx.fxml",
                                                "javafx.media",
                                                "javafx.web",
                                                "jdk.packager.services",
                                                "jdk.accessibility",
                                                "jdk.charsets",
                                                "jdk.crypto.ec",
                                                "jdk.crypto.pkcs11",
                                                "jdk.dynalink",
                                                "jdk.httpserver",
                                                "jdk.internal.le",
                                                "jdk.jfr",
                                                "jdk.jvmstat",
                                                "jdk.jvmstat.rmi",
                                                "jdk.localedata",
                                                "jdk.management",
                                                "jdk.management.cmm",
                                                "jdk.management.resource",
                                                "jdk.naming.dns",
                                                "jdk.naming.rmi",
                                                "jdk.pack200",
                                                "jdk.scripting.nashorn",
                                                "jdk.scripting.nashorn.shell",
                                                "jdk.sctp",
                                                "jdk.security.auth",
                                                "jdk.security.jgss",
                                                "jdk.snmp",
                                                "jdk.vm.cds",
                                                "jdk.vm.ci",
                                                "jdk.xml.dom",
                                                "jdk.zipfs",
                                                "jdk.crypto.mscapi",
                                                "jdk.crypto.ucrypto",
                                                "jdk.deploy.osx"}; // going away JDK-8148187

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<List<Path>> MODULE_PATH =
            new StandardBundlerParam<>(
                    I18N.getString("param.module-path.name"),
                    I18N.getString("param.module-path.description"),
                    "modulepath",
                    (Class<List<Path>>) (Object)List.class,
                    p -> new ArrayList(),
                    (s, p) -> Arrays.asList(s.split("(\\s" + File.pathSeparator + ")+")).stream()
                        .map(ss -> new File(ss).toPath())
                        .collect(Collectors.toList()));

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<String> JDK_MODULE_PATH =
            new StandardBundlerParam<>(
                    I18N.getString("param.jdk-module-path.name"),
                    I18N.getString("param.jdk-module-path.description"),
                    "jdkmodulepath",
                    String.class,
                    p -> Paths.get(System.getProperty("java.home"), "jmods").toAbsolutePath().toString(),
                    (s, p) -> String.valueOf(s));

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<Set<String>> ADD_MODULES =
            new StandardBundlerParam<>(
                    I18N.getString("param.add-modules.name"),
                    I18N.getString("param.add-modules.description"),
                    "addmods",
                    (Class<Set<String>>) (Object) Set.class,
                    p -> new LinkedHashSet(),
                    (s, p) -> new LinkedHashSet<>(Arrays.asList(s.split("[,;: ]+"))));

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<Set<String>> LIMIT_MODULES =
            new StandardBundlerParam<>(
                    I18N.getString("param.limit-modules.name"),
                    I18N.getString("param.limit-modules.description"),
                    "limitmods",
                    (Class<Set<String>>) (Object) Set.class,
                    p -> new LinkedHashSet(),
                    (s, p) -> new LinkedHashSet<>(Arrays.asList(s.split("[,;: ]+"))));

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<Boolean> DETECT_MODULES =
            new StandardBundlerParam<>(
                    I18N.getString("param.detect-modules.name"),
                    I18N.getString("param.detect-modules.description"),
                    "detectmods",
                    Boolean.class,
                    p -> Boolean.FALSE,
                    (s, p) -> Boolean.valueOf(s));

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<Boolean> DETECT_JRE_MODULES =
            new StandardBundlerParam<>(
                    I18N.getString("param.detect-jre-modules.name"),
                    I18N.getString("param.detect-jre-modules.description"),
                    "detectjremods",
                    Boolean.class,
                    p -> Boolean.FALSE,
                    (s, p) -> Boolean.valueOf(s));

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<Boolean> STRIP_NATIVE_COMMANDS =
            new StandardBundlerParam<>(
                    I18N.getString("param.strip-executables.name"),
                    I18N.getString("param.strip-executables.description"),
                    "stripexecutables",
                    Boolean.class,
                    p -> Boolean.TRUE,
                    (s, p) -> Boolean.valueOf(s));

    @SuppressWarnings("unchecked")
    public static final BundlerParamInfo<Map<String, Object>> JLINK_OPTIONS =
            new StandardBundlerParam<>(
                    I18N.getString("param.jlink-options.name"),
                    I18N.getString("param.jlink-options.description"),
                    "jlinkOptions",
                    (Class<Map<String, Object>>) (Object) Map.class,
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


    public static void execute(Map<String, ? super Object> params, File outputParentDir, ImageBuilder imageBuilder) {
        String jdkmodulePath = JDK_MODULE_PATH.fetchFrom(params);
        List<Path> modulePath = MODULE_PATH.fetchFrom(params);
        Set<String> addModules = ADD_MODULES.fetchFrom(params);
        Set<String> limitModules = LIMIT_MODULES.fetchFrom(params);

        boolean detectModules = DETECT_MODULES.fetchFrom(params);
        boolean detectJreModules = DETECT_JRE_MODULES.fetchFrom(params);
        File jdkModulePathFile = new File(jdkmodulePath);

        if (!jdkModulePathFile.exists() || !jdkModulePathFile.isDirectory()) {
            Log.info("JDK Module path doesn't exist: " + jdkmodulePath);
            //TODO fail?
            jdkModulePathFile = null;
        }

        Collection<String> moduleNames = getModuleNamesForApp(getResourceFileJarList(params),
                                                              jdkModulePathFile,
                                                              modulePath,
                                                              detectJreModules,
                                                              detectModules,
                                                              addModules.isEmpty());

        if (moduleNames != null) {
            addModules.addAll(moduleNames);
        }

        // Add JDK path to the module path for JLINK.
        if (jdkModulePathFile != null) {
            modulePath.add(jdkModulePathFile.toPath());
        }

        Path output = outputParentDir.toPath();

        // jlink main arguments
        Jlink.JlinkConfiguration jlinkConfig = new Jlink.JlinkConfiguration(output,
                                                                            modulePath,
                                                                            addModules,
                                                                            limitModules);

        // plugin configuration
        List<Plugin> plugins = new ArrayList<>();

        if (STRIP_NATIVE_COMMANDS.fetchFrom(params)) {
            plugins.add(Jlink.newPlugin(
                    "strip-native-commands",
                    Collections.singletonMap("strip-native-commands", "on"),
                    null));
        }

        plugins.add(Jlink.newPlugin(
                "exclude-files",
                Collections.singletonMap("exclude-files", getExcludeFileList()),
                null));

        // add user supplied jlink arguments
        for (Map.Entry<String, Object> entry : JLINK_OPTIONS.fetchFrom(params).entrySet()) {
            Object o = entry.getValue();
            if (o instanceof String) {
                String key = entry.getKey();
                String value = (String)entry.getValue();
                plugins.add(Jlink.newPlugin(key,
                            Collections.singletonMap(key, value),
                            null));
            }
        }

        plugins.add(Jlink.newPlugin("installed-modules", Collections.emptyMap(), null));

        //TODO --compress-resources

        Jlink.PluginsConfiguration pluginConfig = new Jlink.PluginsConfiguration(plugins, imageBuilder, null);

        // Build the image
        Jlink jlink = new Jlink();

        try {
            jlink.build(jlinkConfig, pluginConfig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    private static Set<String> getModuleNamesFromPath(Path Value) {
        return getModuleNamesFromPath(new ArrayList<Path>(Arrays.asList(Value)));
    }

    //TODO make this per bundler.
    private static String getExcludeFileList() {
        // strip debug symbols
        String result = "*diz";

        if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0) {
            // strip mac osx quicktime
            result += ",*libjfxmedia_qtkit.dylib";
        }

        return result;
    }

    private static Set<String> getAllJDKModuleNames(File jdkModulePathFile) {
        Set<String> result;

        if (jdkModulePathFile != null) {
            result = getModuleNamesFromPath(jdkModulePathFile.toPath());
            Set<String> javaseModules = new HashSet<>(Arrays.asList(JRE_MODULES));

            //TODO JDK-8149975 programmatically determine JRE vs JDK modules
            result.retainAll(javaseModules); // strip out JDK modules
        }
        else {
            result = new LinkedHashSet();
        }

        return result;
    }

    private static Collection<String> getAllDetectedModuleNames(List<String> appJars, File jdkModulePathFile, List<Path> modulePath) {
            List<Path> lmodulePath = new ArrayList();
            lmodulePath.addAll(modulePath);

            // Add JDK modules to the module path.
            if (jdkModulePathFile != null) {
                lmodulePath.add(jdkModulePathFile.toPath());
            }

            // Ask Jdeps for the list of dependent modules.
            Collection<String> detectedModules = JDepHelper.calculateModules(appJars, lmodulePath);
            Log.info("Automatically adding detected modules " + detectedModules);

            return detectedModules;
    }

    public static List<String> getResourceFileJarList(Map<String, ? super Object> params) {
        List<String> files = new ArrayList();

        for (RelativeFileSet rfs : StandardBundlerParam.APP_RESOURCES_LIST.fetchFrom(params)) {
            for (String s : rfs.files) {
                if (s.endsWith(".jar")) {
                    files.add(rfs.getBaseDirectory() + File.separator + s);
                }
            }
        }

        return files;
    }

    private static Collection<String> getModuleNamesForApp(List<String> jars,
                                                           File jdkModulePathFile,
                                                           List<Path> modulePath,
                                                           boolean detectJreModules,
                                                           boolean detectModules,
                                                           boolean manualModules) {
        Collection<String> result = new LinkedHashSet();

        Set<String> moduleNames = null;
        Set<String> jdkModuleNames = null;
        Collection<String> detectedModules = null;

        // There are five options. 1. Manual, If none the options below apply,
        // user specified modules with -addmods with no detection so they want
        // complete manual control.

        // 2. Detect JRE modules or detect other modules.
        if (detectJreModules || detectModules) {
            detectedModules = getAllDetectedModuleNames(jars, jdkModulePathFile, modulePath);

            // 3. All JRE modules and detect other modules.
            if (!detectJreModules) {
                // Only retain Java SE Modules.
                jdkModuleNames = getAllJDKModuleNames(jdkModulePathFile);
            }

            // 4. Detect JRE modules and all other modules.
            if (!detectModules) {
                // Add all modules on user specified path (-modulepath).
                moduleNames = getModuleNamesFromPath(modulePath);
            }
        }
        else if (manualModules) {
            // 5. All Modules.
            // Only retain Java SE Modules.
            jdkModuleNames = getAllJDKModuleNames(jdkModulePathFile);

            // Add all modules on user specified path (-modulepath).
            moduleNames = getModuleNamesFromPath(modulePath);
        }

        if (moduleNames != null) {
            result.addAll(moduleNames);
        }

        if (jdkModuleNames != null) {
            result.addAll(jdkModuleNames);
        }

        if (detectedModules != null) {
            result.addAll(detectedModules);
        }

        return result;
    }
}
