/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.tools.packager.bundlers.BundleParams;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

public class StandardBundlerParam<T> extends BundlerParamInfo<T> {

    public static final String MANIFEST_JAVAFX_MAIN ="JavaFX-Application-Class";
    public static final String MANIFEST_PRELOADER = "JavaFX-Preloader-Class";

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle(StandardBundlerParam.class.getName());

    public StandardBundlerParam(String name, String description, String id,
                                Class<T> valueType,
                                Function<Map<String, ? super Object>, T> defaultValueFunction,
                                BiFunction<String, Map<String, ? super Object>, T> stringConverter) {
        this.name = name;
        this.description = description;
        this.id = id;
        this.valueType = valueType;
        this.defaultValueFunction = defaultValueFunction;
        this.stringConverter = stringConverter;
    }

    public static final StandardBundlerParam<RelativeFileSet> APP_RESOURCES =
            new StandardBundlerParam<>(
                    I18N.getString("param.app-resources.name"),
                    I18N.getString("param.app-resource.description"),
                    BundleParams.PARAM_APP_RESOURCES,
                    RelativeFileSet.class,
                    null, // no default.  Required parameter
                    null // no string translation, tool must provide complex type
            );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<List<RelativeFileSet>> APP_RESOURCES_LIST =
            new StandardBundlerParam<>(
                    I18N.getString("param.app-resources-list.name"),
                    I18N.getString("param.app-resource-list.description"),
                    BundleParams.PARAM_APP_RESOURCES + "List",
                    (Class<List<RelativeFileSet>>) (Object) List.class,
                    p -> new ArrayList<>(Arrays.asList(APP_RESOURCES.fetchFrom(p))), // Default is appResources, as a single item list
                    null // no string translation, tool must provide complex type
            );

    public static final StandardBundlerParam<File> ICON =
            new StandardBundlerParam<>(
                    I18N.getString("param.icon-file.name"),
                    I18N.getString("param.icon-file.description"),
                    BundleParams.PARAM_ICON,
                    File.class,
                    params -> null,
                    (s, p) -> new File(s)
            );


    public static final StandardBundlerParam<String> MAIN_CLASS =
            new StandardBundlerParam<>(
                    I18N.getString("param.main-class.name"),
                    I18N.getString("param.main-class.description"),
                    BundleParams.PARAM_APPLICATION_CLASS,
                    String.class,
                    params -> {
                        extractMainClassInfoFromAppResources(params);
                        return (String) params.get(BundleParams.PARAM_APPLICATION_CLASS);
                    },
                    (s, p) -> s
            );

    public static final StandardBundlerParam<String> APP_NAME =
            new StandardBundlerParam<>(
                    I18N.getString("param.app-name.name"),
                    I18N.getString("param.app-name.description"),
                    BundleParams.PARAM_NAME,
                    String.class,
                    params -> {
                        String s = MAIN_CLASS.fetchFrom(params);
                        if (s == null) return null;

                        int idx = s.lastIndexOf(".");
                        if (idx >= 0) {
                            return s.substring(idx+1);
                        }
                        return s;
                    },
                    (s, p) -> s
            );

    private static Pattern TO_FS_NAME = Pattern.compile("\\s|[\\\\/?:*<>|]"); // keep out invalid/undesireable filename characters

    public static final StandardBundlerParam<String> APP_FS_NAME =
            new StandardBundlerParam<>(
                    I18N.getString("param.app-fs-name.name"),
                    I18N.getString("param.app-fs-name.description"),
                    "name.fs",
                    String.class,
                    params -> TO_FS_NAME.matcher(APP_NAME.fetchFrom(params)).replaceAll(""),
                    (s, p) -> s
            );


    public static final StandardBundlerParam<String> VENDOR =
            new StandardBundlerParam<>(
                    I18N.getString("param.vendor.name"),
                    I18N.getString("param.vendor.description"),
                    BundleParams.PARAM_VENDOR,
                    String.class,
                    params -> I18N.getString("param.vendor.default"),
                    (s, p) -> s
            );

    public static final StandardBundlerParam<String> CATEGORY =
            new StandardBundlerParam<>(
                    I18N.getString("param.category.name"),
                    I18N.getString("param.category.description"),
                    BundleParams.PARAM_CATEGORY,
                    String.class,
                    params -> I18N.getString("param.category.default"),
                    (s, p) -> s
            );

    public static final StandardBundlerParam<String> DESCRIPTION =
            new StandardBundlerParam<>(
                    I18N.getString("param.description.name"),
                    I18N.getString("param.description.description"),
                    BundleParams.PARAM_DESCRIPTION,
                    String.class,
                    params -> params.containsKey(APP_NAME.getID())
                            ? APP_NAME.fetchFrom(params)
                            : I18N.getString("param.description.default"),
                    (s, p) -> s
            );

    public static final StandardBundlerParam<String> COPYRIGHT =
            new StandardBundlerParam<>(
                    I18N.getString("param.copyright.name"),
                    I18N.getString("param.copyright.description"),
                    BundleParams.PARAM_COPYRIGHT,
                    String.class,
                    params -> MessageFormat.format(I18N.getString("param.copyright.default"), new Date()),
                    (s, p) -> s
            );

    // note that each bundler is likely to replace this one with their own converter
    public static final StandardBundlerParam<RelativeFileSet> MAIN_JAR =
            new StandardBundlerParam<>(
                    I18N.getString("param.main-jar.name"),
                    I18N.getString("param.main-jar.description"),
                    "mainJar",
                    RelativeFileSet.class,
                    params -> {
                        extractMainClassInfoFromAppResources(params);
                        return (RelativeFileSet) params.get("mainJar");
                    },
                    (s, p) -> {
                        for (RelativeFileSet rfs : APP_RESOURCES_LIST.fetchFrom(p)) {
                            File appResourcesRoot = rfs.getBaseDirectory();
                            File f = new File(appResourcesRoot, s);
                            if (f.exists()) {
                                return new RelativeFileSet(appResourcesRoot, new LinkedHashSet<>(Arrays.asList(f)));
                            }
                        }
                        throw new IllegalArgumentException(
                                new ConfigException(
                                        MessageFormat.format(I18N.getString("error.main-jar-does-not-exist"), s),
                                        I18N.getString("error.main-jar-does-not-exist.advice")));
                    }
            );

    public static final StandardBundlerParam<String> CLASSPATH =
            new StandardBundlerParam<>(
                    I18N.getString("param.classpath.name"),
                    I18N.getString("param.classpath.description"),
                    "classpath",
                    String.class,
                    params -> {
                        extractMainClassInfoFromAppResources(params);
                        String cp = (String) params.get("classpath");
                        return cp == null ? "" : cp;
                    },
                    (s, p) -> s.replace(File.pathSeparator, " ")
            );

    public static final StandardBundlerParam<Boolean> USE_FX_PACKAGING =
            new StandardBundlerParam<>(
                    I18N.getString("param.use-javafx-packaging.name"),
                    I18N.getString("param.use-javafx-packaging.description"),
                    "fxPackaging",
                    Boolean.class,
                    params -> {
                        extractMainClassInfoFromAppResources(params);
                        Boolean result = (Boolean) params.get("fxPackaging");
                        return (result == null) ? Boolean.FALSE : result;
                    },
                    (s, p) -> Boolean.valueOf(s)
            );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<List<String>> ARGUMENTS =
            new StandardBundlerParam<>(
                    I18N.getString("param.arguments.name"),
                    I18N.getString("param.arguments.description"),
                    "arguments",
                    (Class<List<String>>) (Object) List.class,
                    params -> Collections.emptyList(),
                    (s, p) -> Arrays.asList(s.split("\\s+"))
            );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<List<String>> JVM_OPTIONS =
            new StandardBundlerParam<>(
                    I18N.getString("param.jvm-options.name"),
                    I18N.getString("param.jvm-options.description"),
                    "jvmOptions",
                    (Class<List<String>>) (Object) List.class,
                    params -> Collections.emptyList(),
                    (s, p) -> Arrays.asList(s.split("\\s+"))
            );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<Map<String, String>> JVM_PROPERTIES =
            new StandardBundlerParam<>(
                    I18N.getString("param.jvm-system-properties.name"),
                    I18N.getString("param.jvm-system-properties.description"),
                    "jvmProperties",
                    (Class<Map<String, String>>) (Object) Map.class,
                    params -> Collections.emptyMap(),
                    (s, params) -> {
                        Map<String, String> map = new HashMap<>();
                        try {
                            Properties p = new Properties();
                            p.load(new StringReader(s));
                            for (Map.Entry<Object, Object> entry : p.entrySet()) {
                                map.put((String)entry.getKey(), (String)entry.getValue());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return map;
                    }
            );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<Map<String, String>> USER_JVM_OPTIONS =
            new StandardBundlerParam<>(
                    I18N.getString("param.user-jvm-options.name"),
                    I18N.getString("param.user-jvm-options.description"),
                    "userJvmOptions",
                    (Class<Map<String, String>>) (Object) Map.class,
                    params -> Collections.emptyMap(),
                    (s, params) -> {
                        Map<String, String> map = new HashMap<>();
                        try {
                            Properties p = new Properties();
                            p.load(new StringReader(s));
                            for (Map.Entry<Object, Object> entry : p.entrySet()) {
                                map.put((String)entry.getKey(), (String)entry.getValue());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return map;
                    }
            );

    public static final StandardBundlerParam<String> TITLE =
            new StandardBundlerParam<>(
                    I18N.getString("param.title.name"),
                    I18N.getString("param.title.description"), //?? but what does it do?
                    BundleParams.PARAM_TITLE,
                    String.class,
                    APP_NAME::fetchFrom,
                    (s, p) -> s
            );


    // note that each bundler is likely to replace this one with their own converter
    public static final StandardBundlerParam<String> VERSION =
            new StandardBundlerParam<>(
                    I18N.getString("param.version.name"),
                    I18N.getString("param.version.description"),
                    BundleParams.PARAM_VERSION,
                    String.class,
                    params -> I18N.getString("param.version.default"),
                    (s, p) -> s
            );

    public static final StandardBundlerParam<Boolean> SYSTEM_WIDE =
            new StandardBundlerParam<>(
                    I18N.getString("param.system-wide.name"),
                    I18N.getString("param.system-wide.description"),
                    BundleParams.PARAM_SYSTEM_WIDE,
                    Boolean.class,
                    params -> null,
                    // valueOf(null) is false, and we actually do want null in some cases
                    (s, p) -> (s == null || "null".equalsIgnoreCase(s))? null : Boolean.valueOf(s)
            );

    public static final StandardBundlerParam<Boolean> SERVICE_HINT  =
            new StandardBundlerParam<>(
                    I18N.getString("param.service-hint.name"),
                    I18N.getString("param.service-hint.description"),
                    BundleParams.PARAM_SERVICE_HINT,
                    Boolean.class,
                    params -> false,
                    (s, p) -> (s == null || "null".equalsIgnoreCase(s))? false : Boolean.valueOf(s)
            );

    public static final StandardBundlerParam<Boolean> START_ON_INSTALL  =
            new StandardBundlerParam<>(
                    I18N.getString("param.start-on-install.name"),
                    I18N.getString("param.start-on-install.description"),
                    "startOnInstall",
                    Boolean.class,
                    params -> false,
                    (s, p) -> (s == null || "null".equalsIgnoreCase(s))? false : Boolean.valueOf(s)
            );

    public static final StandardBundlerParam<Boolean> STOP_ON_UNINSTALL  =
            new StandardBundlerParam<>(
                    I18N.getString("param.stop-on-uninstall.name"),
                    I18N.getString("param.stop-on-uninstall.description"),
                    "stopOnUninstall",
                    Boolean.class,
                    params -> true,
                    (s, p) -> (s == null || "null".equalsIgnoreCase(s))? true : Boolean.valueOf(s)
            );

    public static final StandardBundlerParam<Boolean> RUN_AT_STARTUP  =
            new StandardBundlerParam<>(
                    I18N.getString("param.run-at-startup.name"),
                    I18N.getString("param.run-at-startup.description"),
                    "runAtStartup",
                    Boolean.class,
                    params -> false,
                    (s, p) -> (s == null || "null".equalsIgnoreCase(s))? false : Boolean.valueOf(s)
            );

    public static final StandardBundlerParam<Boolean> SIGN_BUNDLE  =
            new StandardBundlerParam<>(
                    I18N.getString("param.sign-bundle.name"),
                    I18N.getString("param.sign-bundle.description"),
                    "signBundle",
                    Boolean.class,
                    params -> null,
                    // valueOf(null) is false, and we actually do want null in some cases
                    (s, p) -> (s == null || "null".equalsIgnoreCase(s))? null : Boolean.valueOf(s)
            );

    public static final StandardBundlerParam<Boolean> SHORTCUT_HINT =
            new StandardBundlerParam<>(
                    I18N.getString("param.desktop-shortcut-hint.name"),
                    I18N.getString("param.desktop-shortcut-hint.description"),
                    BundleParams.PARAM_SHORTCUT,
                    Boolean.class,
                    params -> false,
                    // valueOf(null) is false, and we actually do want null in some cases
                    (s, p) -> (s == null || "null".equalsIgnoreCase(s))? false : Boolean.valueOf(s)
            );

    public static final StandardBundlerParam<Boolean> MENU_HINT =
            new StandardBundlerParam<>(
                    I18N.getString("param.menu-shortcut-hint.name"),
                    I18N.getString("param.menu-shortcut-hint.description"),
                    BundleParams.PARAM_MENU,
                    Boolean.class,
                    params -> true,
                    // valueOf(null) is false, and we actually do want null in some cases
                    (s, p) -> (s == null || "null".equalsIgnoreCase(s))? true : Boolean.valueOf(s)
            );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<List<String>> LICENSE_FILE =
            new StandardBundlerParam<>(
                    I18N.getString("param.license-file.name"),
                    I18N.getString("param.license-file.description"),
                    BundleParams.PARAM_LICENSE_FILE,
                    (Class<List<String>>)(Object)List.class,
                    params -> Collections.<String>emptyList(),
                    (s, p) -> Arrays.asList(s.split(","))
            );

    public static final BundlerParamInfo<String> LICENSE_TYPE =
            new StandardBundlerParam<> (
                    I18N.getString("param.license-type.name"),
                    I18N.getString("param.license-type.description"),
                    BundleParams.PARAM_LICENSE_TYPE,
                    String.class,
                    params -> I18N.getString("param.license-type.default"),
                    (s, p) -> s
            );

    public static final StandardBundlerParam<File> BUILD_ROOT =
            new StandardBundlerParam<>(
                    I18N.getString("param.build-root.name"),
                    I18N.getString("param.build-root.description"),
                    "buildRoot",
                    File.class,
                    params -> {
                        try {
                            return Files.createTempDirectory("fxbundler").toFile();
                        } catch (IOException ioe) {
                            return null;
                        }
                    },
                    (s, p) -> new File(s)
            );

    public static final StandardBundlerParam<String> IDENTIFIER =
            new StandardBundlerParam<>(
                    I18N.getString("param.identifier.name"),
                    I18N.getString("param.identifier.description"),
                    BundleParams.PARAM_IDENTIFIER,
                    String.class,
                    params -> {
                        String s = MAIN_CLASS.fetchFrom(params);
                        if (s == null) return null;

                        int idx = s.lastIndexOf(".");
                        if (idx >= 1) {
                            return s.substring(0, idx);
                        }
                        return s;
                    },
                    (s, p) -> s
            );

    public static final StandardBundlerParam<String> PREFERENCES_ID =
            new StandardBundlerParam<>(
                    I18N.getString("param.preferences-id.name"),
                    I18N.getString("param.preferences-id.description"),
                    "preferencesID",
                    String.class,
                    p -> Optional.ofNullable(IDENTIFIER.fetchFrom(p)).orElse("").replace('.', '/'),
                    (s, p) -> s
            );
    
    public static final StandardBundlerParam<String> PRELOADER_CLASS = 
            new StandardBundlerParam<>(
                    I18N.getString("param.preloader.name"),
                    I18N.getString("param.preloader.description"),
                    "preloader",
                    String.class,
                    p -> null,
                    null
            );

    public static final StandardBundlerParam<Boolean> VERBOSE  =
            new StandardBundlerParam<>(
                    I18N.getString("param.verbose.name"),
                    I18N.getString("param.verbose.description"),
                    "verbose",
                    Boolean.class,
                    params -> false,
                    // valueOf(null) is false, and we actually do want null in some cases
                    (s, p) -> (s == null || "null".equalsIgnoreCase(s))? true : Boolean.valueOf(s)
            );
    
    public static final StandardBundlerParam<File> DROP_IN_RESOURCES_ROOT =
            new StandardBundlerParam<>(
                    I18N.getString("param.drop-in-resources-root.name"),
                    I18N.getString("param.drop-in-resources-root.description"),
                    "dropinResourcesRoot",
                    File.class,
                    params -> null,
                    (s, p) -> new File(s)
            );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<List<Map<String, ? super Object>>> SECONDARY_LAUNCHERS =
            new StandardBundlerParam<>(
                    I18N.getString("param.secondary-launchers.name"),
                    I18N.getString("param.secondary-launchers.description"),
                    "secondaryLaunchers",
                    (Class<List<Map<String, ? super Object>>>) (Object) List.class,
                    params -> new ArrayList<>(1),
                    // valueOf(null) is false, and we actually do want null in some cases
                    (s, p) -> null
            );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<List<Map<String, ? super Object>>> FILE_ASSOCIATIONS =
            new StandardBundlerParam<>(
                    I18N.getString("param.file-associations.name"),
                    I18N.getString("param.file-associations.description"),
                    "fileAssociations",
                    (Class<List<Map<String, ? super Object>>>) (Object) List.class,
                    params -> new ArrayList<>(1),
                    // valueOf(null) is false, and we actually do want null in some cases
                    (s, p) -> null
            );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<List<String>> FA_EXTENSIONS =
            new StandardBundlerParam<>(
                    I18N.getString("param.fa-extension.name"),
                    I18N.getString("param.fa-extension.description"),
                    "fileAssociation.extension",
                    (Class<List<String>>) (Object) List.class,
                    params -> null, // null means not matched to an extension
                    (s, p) -> Arrays.asList(s.split("(,|\\s)+"))
            );

    @SuppressWarnings("unchecked")
    public static final StandardBundlerParam<List<String>> FA_CONTENT_TYPE =
            new StandardBundlerParam<>(
                    I18N.getString("param.fa-content-type.name"),
                    I18N.getString("param.fa-content-type.description"),
                    "fileAssociation.contentType",
                    (Class<List<String>>) (Object) List.class,
                    params -> null, // null means not matched to a content/mime type
                    (s, p) -> Arrays.asList(s.split("(,|\\s)+"))
            );

    public static final StandardBundlerParam<String> FA_DESCRIPTION =
            new StandardBundlerParam<>(
                    I18N.getString("param.fa-description.name"),
                    I18N.getString("param.fa-description.description"),
                    "fileAssociation.description",
                    String.class,
                    params -> APP_NAME.fetchFrom(params) + " File",
                    null
            );

    public static final StandardBundlerParam<File> FA_ICON =
            new StandardBundlerParam<>(
                    I18N.getString("param.fa-icon.name"),
                    I18N.getString("param.fa-icon.description"),
                    "fileAssociation.icon",
                    File.class,
                    ICON::fetchFrom,
                    (s, p) -> new File(s)
            );

    public static void extractMainClassInfoFromAppResources(Map<String, ? super Object> params) {
        boolean hasMainClass = params.containsKey(MAIN_CLASS.getID());
        boolean hasMainJar = params.containsKey(MAIN_JAR.getID());
        boolean hasMainJarClassPath = params.containsKey(CLASSPATH.getID());
        boolean hasPreloader = params.containsKey(PRELOADER_CLASS.getID());

        if (hasMainClass && hasMainJar && hasMainJarClassPath) {
            return;
        }
        // it's a pair.  The [0] is the srcdir [1] is the file relative to sourcedir
        List<String[]> filesToCheck = new ArrayList<>();
        
        if (hasMainJar) {
            RelativeFileSet rfs = MAIN_JAR.fetchFrom(params);
            for (String s : rfs.getIncludedFiles()) {
                filesToCheck.add(new String[]{rfs.getBaseDirectory().toString(), s});
            }
        } else if (hasMainJarClassPath) {
            for (String s : CLASSPATH.fetchFrom(params).split("\\s+")) {
                filesToCheck.add(new String[] {APP_RESOURCES.fetchFrom(params).getBaseDirectory().toString(), s});
            }
        } else {
            RelativeFileSet rfs = APP_RESOURCES.fetchFrom(params);
            if (rfs == null) {
                return;
            }
            for (String s : rfs.getIncludedFiles()) {
                filesToCheck.add(new String[]{rfs.getBaseDirectory().toString(), s});
            }
        }

        String declaredMainClass = (String) params.get(MAIN_CLASS.getID());

        // presume the set iterates in-order
        for (String[] fnames : filesToCheck) {
            try {
                // only sniff jars
                if (!fnames[1].toLowerCase().endsWith(".jar")) continue;

                File file = new File(fnames[0], fnames[1]);
                // that actually exist
                if (!file.exists()) continue;

                JarFile jf = new JarFile(file);
                Manifest m = jf.getManifest();
                Attributes attrs = (m != null) ? m.getMainAttributes() : null;

                if (attrs != null) {
                    String mainClass = attrs.getValue(Attributes.Name.MAIN_CLASS);
                    String fxMain = attrs.getValue(MANIFEST_JAVAFX_MAIN);
                    String preloaderClass = attrs.getValue(MANIFEST_PRELOADER);
                    if (hasMainClass) {
                        if (declaredMainClass.equals(fxMain)) {
                            params.put(USE_FX_PACKAGING.getID(), true);
                        } else if (declaredMainClass.equals(mainClass)) {
                            params.put(USE_FX_PACKAGING.getID(), false);
                        } else {
                            if (fxMain != null) {
                                Log.info(MessageFormat.format(I18N.getString("message.fx-app-does-not-match-specified-main"), fnames[1], fxMain, declaredMainClass));
                            }
                            if (mainClass != null) {
                                Log.info(MessageFormat.format(I18N.getString("message.main-class-does-not-match-specified-main"), fnames[1], mainClass, declaredMainClass));
                            }
                            continue;
                        }
                    } else {
                        if (fxMain != null) {
                            params.put(USE_FX_PACKAGING.getID(), true);
                            params.put(MAIN_CLASS.getID(), fxMain);
                        } else if (mainClass != null) {
                            params.put(USE_FX_PACKAGING.getID(), false);
                            params.put(MAIN_CLASS.getID(), mainClass);
                        } else {
                            continue;
                        }
                    }
                    if (!hasPreloader && preloaderClass != null) {
                        params.put(PRELOADER_CLASS.getID(), preloaderClass);
                    }
                    if (!hasMainJar) {
                        if (fnames[0] == null) {
                            fnames[0] = file.getParentFile().toString();
                        }
                        params.put(MAIN_JAR.getID(), new RelativeFileSet(new File(fnames[0]), new LinkedHashSet<>(Arrays.asList(file))));
                    }
                    if (!hasMainJarClassPath) {
                        String cp = attrs.getValue(Attributes.Name.CLASS_PATH);
                        params.put(CLASSPATH.getID(), cp == null ? "" : cp);
                    }
                    break;
                }
            } catch (IOException ignore) {
                ignore.printStackTrace();
            }
        }
    }

    public static void validateMainClassInfoFromAppResources(Map<String, ? super Object> params) throws ConfigException {
        boolean hasMainClass = params.containsKey(MAIN_CLASS.getID());
        boolean hasMainJar = params.containsKey(MAIN_JAR.getID());
        boolean hasMainJarClassPath = params.containsKey(CLASSPATH.getID());

        if (hasMainClass && hasMainJar && hasMainJarClassPath) {
            return;
        }

        extractMainClassInfoFromAppResources(params);
        if (!params.containsKey(MAIN_CLASS.getID())) {
            if (hasMainJar) {
                throw new ConfigException(
                        MessageFormat.format(I18N.getString("error.no-main-class-with-main-jar"),
                                MAIN_JAR.fetchFrom(params)),
                        MessageFormat.format(I18N.getString("error.no-main-class-with-main-jar.advice"),
                                MAIN_JAR.fetchFrom(params)));
            } else if (hasMainJarClassPath) {
                throw new ConfigException(
                        I18N.getString("error.no-main-class-with-classpath"),
                        I18N.getString("error.no-main-class-with-classpath.advice"));
            } else {
                throw new ConfigException(
                        I18N.getString("error.no-main-class"),
                        I18N.getString("error.no-main-class.advice"));
            }
        }
    }
}
