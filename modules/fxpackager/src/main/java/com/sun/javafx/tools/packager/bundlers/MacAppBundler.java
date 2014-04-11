/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.tools.packager.bundlers;

import com.oracle.bundlers.*;
import com.oracle.bundlers.JreUtils.Rule;
import com.oracle.bundlers.mac.MacBaseInstallerBundler;
import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.resource.mac.MacResources;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;

import static com.oracle.bundlers.StandardBundlerParam.*;
import static com.oracle.bundlers.mac.MacBaseInstallerBundler.SIGNING_KEY_USER;
import static com.oracle.bundlers.mac.MacBaseInstallerBundler.getPredefinedImage;

public class MacAppBundler extends AbstractBundler {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle("com.oracle.bundlers.mac.MacAppBundler");

    public final static String MAC_BUNDLER_PREFIX =
            BUNDLER_PREFIX + "macosx" + File.separator;

    private static final String EXECUTABLE_NAME      = "JavaAppLauncher";
    private static final String TEMPLATE_BUNDLE_ICON = "GenericApp.icns";
    private static final String OS_TYPE_CODE         = "APPL";
    private static final String TEMPLATE_INFO_PLIST  = "Info.plist.template";

    private static Map<String, String> getMacCategories() {
        Map<String, String> map = new HashMap<>();
        map.put("Business", "public.app-category.business");
        map.put("Developer Tools", "public.app-category.developer-tools");
        map.put("Education", "public.app-category.education");
        map.put("Entertainment", "public.app-category.entertainment");
        map.put("Finance", "public.app-category.finance");
        map.put("Games", "public.app-category.games");
        map.put("Graphics & Design", "public.app-category.graphics-design");
        map.put("Healthcare & Fitness", "public.app-category.healthcare-fitness");
        map.put("Lifestyle", "public.app-category.lifestyle");
        map.put("Medical", "public.app-category.medical");
        map.put("Music", "public.app-category.music");
        map.put("News", "public.app-category.news");
        map.put("Photography", "public.app-category.photography");
        map.put("Productivity", "public.app-category.productivity");
        map.put("Reference", "public.app-category.reference");
        map.put("Social Networking", "public.app-category.social-networking");
        map.put("Sports", "public.app-category.sports");
        map.put("Travel", "public.app-category.travel");
        map.put("Utilities", "public.app-category.utilities");
        map.put("Video", "public.app-category.video");
        map.put("Weather", "public.app-category.weather");

        map.put("Action Games", "public.app-category.action-games");
        map.put("Adventure Games", "public.app-category.adventure-games");
        map.put("Arcade Games", "public.app-category.arcade-games");
        map.put("Board Games", "public.app-category.board-games");
        map.put("Card Games", "public.app-category.card-games");
        map.put("Casino Games", "public.app-category.casino-games");
        map.put("Dice Games", "public.app-category.dice-games");
        map.put("Educational Games", "public.app-category.educational-games");
        map.put("Family Games", "public.app-category.family-games");
        map.put("Kids Games", "public.app-category.kids-games");
        map.put("Music Games", "public.app-category.music-games");
        map.put("Puzzle Games", "public.app-category.puzzle-games");
        map.put("Racing Games", "public.app-category.racing-games");
        map.put("Role Playing Games", "public.app-category.role-playing-games");
        map.put("Simulation Games", "public.app-category.simulation-games");
        map.put("Sports Games", "public.app-category.sports-games");
        map.put("Strategy Games", "public.app-category.strategy-games");
        map.put("Trivia Games", "public.app-category.trivia-games");
        map.put("Word Games", "public.app-category.word-games");

        return map;
    }

    public static final EnumeratedBundlerParam<String> MAC_CATEGORY =
            new EnumeratedBundlerParam<>(
                    "Category",
                    "Mac App Store Categories. Note that the key is the string to display to the user and the value is the id of the category",
                    "mac.category",
                    String.class,
                    params -> params.containsKey(CATEGORY.getID())
                            ? CATEGORY.fetchFrom(params)
                            : "Unknown",
                    (s, p) -> s,
                    getMacCategories(),
                    false //strict - for MacStoreBundler this should be strict
            );

    public static final BundlerParamInfo<String> MAC_CF_BUNDLE_NAME =
            new StandardBundlerParam<>(
                    "CFBundleName",
                    "The name of the app as it appears in the Menu Bar.  This can be different from the application name.  This name should be less than 16 characters long and be suitable for displaying in the menu bar and the app’s Info window.",
                    "mac.CFBundleName",
                    String.class,
                    params -> null,
                    (s, p) -> s);

    public static final BundlerParamInfo<File> CONFIG_ROOT = new StandardBundlerParam<>(
            I18N.getString("param.config-root.name"),
            I18N.getString("param.config-root.description"),
            "configRoot",
            File.class,
            params -> {
                File configRoot = new File(BUILD_ROOT.fetchFrom(params), "macosx");
                configRoot.mkdirs();
                return configRoot;
            },
            (s, p) -> new File(s));

    public static final BundlerParamInfo<URL> RAW_EXECUTABLE_URL = new StandardBundlerParam<>(
            "Launcher URL",
            "Override the packager default launcher with a custom launcher.",
            "mac.launcher.url",
            URL.class,
            params -> MacResources.class.getResource(EXECUTABLE_NAME),
            (s, p) -> {
                try {
                    return new URL(s);
                } catch (MalformedURLException e) {
                    Log.info(e.toString());
                    return null;
                }
            });

    public static final BundlerParamInfo<String> DEFAULT_ICNS_ICON = new StandardBundlerParam<>(
            "Default Icon",
            "The Default Icon for when a user does not specify an icns file.",
            ".mac.default.icns",
            String.class,
            params -> TEMPLATE_BUNDLE_ICON,
            (s, p) -> s);

    //Subsetting of JRE is restricted.
    //JRE README defines what is allowed to strip:
    //   ﻿http://www.oracle.com/technetwork/java/javase/jre-7-readme-430162.html //TODO update when 8 goes GA
    //
    public static final BundlerParamInfo<Rule[]> MAC_JDK_RULES = new StandardBundlerParam<>(
            "",
            "",
            ".mac-jdk.runtime.rules",
            Rule[].class,
            params -> new Rule[]{
                    Rule.suffixNeg("macos/libjli.dylib"),
                    Rule.suffixNeg("resources"),
                    Rule.suffixNeg("home/bin"),
                    Rule.suffixNeg("home/db"),
                    Rule.suffixNeg("home/demo"),
                    Rule.suffixNeg("home/include"),
                    Rule.suffixNeg("home/lib"),
                    Rule.suffixNeg("home/man"),
                    Rule.suffixNeg("home/release"),
                    Rule.suffixNeg("home/sample"),
                    Rule.suffixNeg("home/src.zip"),
                    //"home/rt" is not part of the official builds
                    // but we may be creating this symlink to make older NB projects
                    // happy. Make sure to not include it into final artifact
                    Rule.suffixNeg("home/rt"),
                    Rule.suffixNeg("jre/bin"),
                    Rule.suffixNeg("jre/bin/rmiregistry"),
                    Rule.suffixNeg("jre/bin/tnameserv"),
                    Rule.suffixNeg("jre/bin/keytool"),
                    Rule.suffixNeg("jre/bin/klist"),
                    Rule.suffixNeg("jre/bin/ktab"),
                    Rule.suffixNeg("jre/bin/policytool"),
                    Rule.suffixNeg("jre/bin/orbd"),
                    Rule.suffixNeg("jre/bin/servertool"),
                    Rule.suffixNeg("jre/bin/javaws"),
                    Rule.suffixNeg("jre/bin/java"),
                    //Rule.suffixNeg("jre/lib/ext"), //need some of jars there for https to work
                    Rule.suffixNeg("jre/lib/nibs"),
                    //keep core deploy APIs but strip plugin dll
                    //Rule.suffixNeg("jre/lib/deploy"),
                    //Rule.suffixNeg("jre/lib/deploy.jar"),
                    //Rule.suffixNeg("jre/lib/javaws.jar"),
                    //Rule.suffixNeg("jre/lib/libdeploy.dylib"),
                    //Rule.suffixNeg("jre/lib/plugin.jar"),
                    Rule.suffixNeg("jre/lib/libnpjp2.dylib"),
                    Rule.suffixNeg("jre/lib/security/javaws.policy"),
                    Rule.substrNeg("Contents/Info.plist")
            },
            (s, p) -> null
    );

    public static final BundlerParamInfo<RelativeFileSet> MAC_RUNTIME = new StandardBundlerParam<>(
            RUNTIME.getName(),
            RUNTIME.getDescription(),
            RUNTIME.getID(),
            RelativeFileSet.class,
            params -> extractMacRuntime(System.getProperty("java.home"), params),
            MacAppBundler::extractMacRuntime
    );

    public static final BundlerParamInfo<String> DEVELOPER_ID_APP_SIGNING_KEY = new StandardBundlerParam<>(
            I18N.getString("param.signing-key-developer-id-app.name"),
            I18N.getString("param.signing-key-developer-id-app.description"),
            "mac.signing-key-developer-id-app",
            String.class,
            params -> {
                String key = "Developer ID Application: " + SIGNING_KEY_USER.fetchFrom(params);
                try {
                    IOUtils.exec(new ProcessBuilder("security", "find-certificate", "-c", key), VERBOSE.fetchFrom(params));
                    return key;
                } catch (IOException ioe) {
                    return null;
                }
            },
            (s, p) -> s);


    public static RelativeFileSet extractMacRuntime(String base, Map<String, ? super Object> params) {
        if (base.endsWith("/Home")) {
            throw new IllegalArgumentException(I18N.getString("message.no-mac-jre-support"));
        } else if (base.endsWith("/Home/jre")) {
            File baseDir = new File(base).getParentFile().getParentFile().getParentFile();
            return JreUtils.extractJreAsRelativeFileSet(baseDir.toString(),
                    MAC_JDK_RULES.fetchFrom(params));
        } else {
            // for now presume we are pointed to the top of a JDK
            return JreUtils.extractJreAsRelativeFileSet(base,
                    MAC_JDK_RULES.fetchFrom(params));
        }
    }

    public MacAppBundler() {
        super();
        baseResourceLoader = MacResources.class;
    }

    @Override
    public boolean validate(Map<String, ? super Object> params) throws UnsupportedPlatformException, ConfigException {
        try {
            return doValidate(params);
        } catch (RuntimeException re) {
            throw new ConfigException(re);
        }
    }

    //to be used by chained bundlers, e.g. by EXE bundler to avoid
    // skipping validation if p.type does not include "image"
    public boolean doValidate(Map<String, ? super Object> p) throws UnsupportedPlatformException, ConfigException {
        if (!System.getProperty("os.name").toLowerCase().contains("os x")) {
            throw new UnsupportedPlatformException();
        }

        if (getPredefinedImage(p) != null) {
            return true;
        }

        if (MAIN_JAR.fetchFrom(p) == null) {
            throw new ConfigException(
                    I18N.getString("error.no-application-jar"),
                    I18N.getString("error.no-application-jar.advice"));
        }

        //validate required inputs
        if (USE_FX_PACKAGING.fetchFrom(p)) {
            testRuntime(p, new String[] {"Contents/Home/jre/lib/ext/jfxrt.jar", "Contents/Home/jre/lib/jfxrt.jar"});
        }

        return true;
    }


    private File getConfig_InfoPlist(Map<String, ? super Object> params) {
        return new File(CONFIG_ROOT.fetchFrom(params), "Info.plist");
    }

    private File getConfig_Icon(Map<String, ? super Object> params) {
        return new File(CONFIG_ROOT.fetchFrom(params), APP_NAME.fetchFrom(params) + ".icns");
    }

    private void prepareConfigFiles(Map<String, ? super Object> params) throws IOException {
        File infoPlistFile = getConfig_InfoPlist(params);
        infoPlistFile.createNewFile();
        writeInfoPlist(infoPlistFile, params);

        // Copy icon to Resources folder
        prepareIcon(params);
    }

    public File doBundle(Map<String, ? super Object> p, File outputDirectory, boolean dependentTask) {
        File rootDirectory = null;
        try {
            final File predefinedImage = getPredefinedImage(p);
            if (predefinedImage != null) {
                return predefinedImage;
            }

            // side effect is temp dir is created if not specified
            BUILD_ROOT.fetchFrom(p);

            //prepare config resources (we will copy them to the bundle later)
            // NB: explicitly saving them to simplify customization
            prepareConfigFiles(p);

            // Create directory structure
            rootDirectory = new File(outputDirectory, APP_NAME.fetchFrom(p) + ".app");
            IOUtils.deleteRecursive(rootDirectory);
            rootDirectory.mkdirs();

            if (!dependentTask) {
                Log.info(MessageFormat.format(I18N.getString("message.creating-app-bundle"), rootDirectory.getAbsolutePath()));
            }

            File contentsDirectory = new File(rootDirectory, "Contents");
            contentsDirectory.mkdirs();

            File macOSDirectory = new File(contentsDirectory, "MacOS");
            macOSDirectory.mkdirs();

            File javaDirectory = new File(contentsDirectory, "Java");
            javaDirectory.mkdirs();

            File plugInsDirectory = new File(contentsDirectory, "PlugIns");

            File resourcesDirectory = new File(contentsDirectory, "Resources");
            resourcesDirectory.mkdirs();

            // Generate PkgInfo
            File pkgInfoFile = new File(contentsDirectory, "PkgInfo");
            pkgInfoFile.createNewFile();
            writePkgInfo(pkgInfoFile);

            // Copy executable to MacOS folder
            File executableFile = new File(macOSDirectory, getLauncherName(p));
            IOUtils.copyFromURL(
                    RAW_EXECUTABLE_URL.fetchFrom(p),
                    executableFile);

            executableFile.setExecutable(true, false);

            // Copy runtime to PlugIns folder
            copyRuntime(plugInsDirectory, p);

            // Copy class path entries to Java folder
            copyClassPathEntries(javaDirectory, p);

//TODO: Need to support adding native libraries.
            // Copy library path entries to MacOS folder
            //copyLibraryPathEntries(macOSDirectory);

            /*********** Take care of "config" files *******/
            // Copy icon to Resources folder
            IOUtils.copyFile(getConfig_Icon(p),
                    new File(resourcesDirectory, getConfig_Icon(p).getName()));
            // Generate Info.plist
            IOUtils.copyFile(getConfig_InfoPlist(p),
                    new File(contentsDirectory, "Info.plist"));

            // maybe sign
            String signingIdentity = DEVELOPER_ID_APP_SIGNING_KEY.fetchFrom(p);
            if (signingIdentity != null) {
                MacBaseInstallerBundler.signAppBundle(p, rootDirectory, signingIdentity, IDENTIFIER.fetchFrom(p) + ".");
            }
        } catch (IOException ex) {
            Log.info(ex.toString());
            Log.verbose(ex);
            return null;
        } finally {
            if (!VERBOSE.fetchFrom(p)) {
                //cleanup
                cleanupConfigFiles(p);
            } else {
                Log.info(MessageFormat.format(I18N.getString("message.config-save-location"), CONFIG_ROOT.fetchFrom(p).getAbsolutePath()));
            }
        }
        return rootDirectory;
    }

    public String getAppName(Map<String, ? super Object> params) {
        return APP_NAME.fetchFrom(params) + ".app";
    }

    public void cleanupConfigFiles(Map<String, ? super Object> params) {
        //Since building the app can be bypassed, make sure configRoot was set
        if (CONFIG_ROOT.fetchFrom(params) != null) {
            if (getConfig_Icon(params) != null) {
                getConfig_Icon(params).delete();
            }
            if (getConfig_InfoPlist(params) != null) {
                getConfig_InfoPlist(params).delete();
            }
        }
    }


    private void copyClassPathEntries(File javaDirectory, Map<String, ? super Object> params) throws IOException {
        RelativeFileSet classPath = APP_RESOURCES.fetchFrom(params);
        if (classPath == null) {
            throw new RuntimeException(I18N.getString("message.null-classpath"));
        }
        File srcdir = classPath.getBaseDirectory();
        for (String fname : classPath.getIncludedFiles()) {
            IOUtils.copyFile(
                    new File(srcdir, fname), new File(javaDirectory, fname));
        }
    }

    private void copyRuntime(File plugInsDirectory, Map<String, ? super Object> params) throws IOException {
        RelativeFileSet runTime = MAC_RUNTIME.fetchFrom(params);
        if (runTime == null) {
            //request to use system runtime => do not bundle
            return;
        }
        plugInsDirectory.mkdirs();

        File srcdir = runTime.getBaseDirectory();
        File destDir = new File(plugInsDirectory, srcdir.getName());
        Set<String> filesToCopy = runTime.getIncludedFiles();

        for (String fname : filesToCopy) {
            IOUtils.copyFile(
                    new File(srcdir, fname), new File(destDir, fname));
        }
    }

    private void prepareIcon(Map<String, ? super Object> params) throws IOException {
        File icon = ICON.fetchFrom(params);
        if (icon == null || !icon.exists()) {
            fetchResource(MAC_BUNDLER_PREFIX+ APP_NAME.fetchFrom(params) +".icns",
                    "icon",
                    DEFAULT_ICNS_ICON.fetchFrom(params),
                    getConfig_Icon(params),
                    VERBOSE.fetchFrom(params));
        } else {
            fetchResource(MAC_BUNDLER_PREFIX+ APP_NAME.fetchFrom(params) +".icns",
                    "icon",
                    icon,
                    getConfig_Icon(params),
                    VERBOSE.fetchFrom(params));
        }
    }

    private String getLauncherName(Map<String, ? super Object> params) {
        if (APP_NAME.fetchFrom(params) != null) {
            return APP_NAME.fetchFrom(params);
        } else {
            return MAIN_CLASS.fetchFrom(params);
        }
    }

    private String getBundleName(Map<String, ? super Object> params) {
        //TODO: Check to see what rules/limits are in place for CFBundleName
        if (MAC_CF_BUNDLE_NAME.fetchFrom(params) != null) {
            String bn = MAC_CF_BUNDLE_NAME.fetchFrom(params);
            if (bn.length() > 16) {
                Log.info(MessageFormat.format(I18N.getString("message.bundle-name-too-long-warning"), MAC_CF_BUNDLE_NAME.getID(), bn));
            }
            return MAC_CF_BUNDLE_NAME.fetchFrom(params);
        } else if (APP_NAME.fetchFrom(params) != null) {
            return APP_NAME.fetchFrom(params);
        } else {
            String nm = MAIN_CLASS.fetchFrom(params);
            if (nm.length() > 16) {
                nm = nm.substring(0, 16);
            }
            return nm;
        }
    }

    private String getBundleIdentifier(Map<String, ? super Object> params) {
        //TODO: Check to see what rules/limits are in place for CFBundleIdentifier
        return  IDENTIFIER.fetchFrom(params);
    }

    private void writeInfoPlist(File file, Map<String, ? super Object> params) throws IOException {
        Log.verbose(MessageFormat.format(I18N.getString("message.preparing-info-plist"), file.getAbsolutePath()));

        //prepare config for exe
        //Note: do not need CFBundleDisplayName if we do not support localization
        Map<String, String> data = new HashMap<>();
        data.put("DEPLOY_ICON_FILE", getConfig_Icon(params).getName());
        data.put("DEPLOY_BUNDLE_IDENTIFIER",
                getBundleIdentifier(params));
        data.put("DEPLOY_BUNDLE_NAME",
                getBundleName(params));
        data.put("DEPLOY_BUNDLE_COPYRIGHT",
                COPYRIGHT.fetchFrom(params) != null ? COPYRIGHT.fetchFrom(params) : "Unknown");
        data.put("DEPLOY_LAUNCHER_NAME", getLauncherName(params));
        if (MAC_RUNTIME.fetchFrom(params) != null) {
            data.put("DEPLOY_JAVA_RUNTIME_NAME",
                    MAC_RUNTIME.fetchFrom(params).getBaseDirectory().getName());
        } else {
            data.put("DEPLOY_JAVA_RUNTIME_NAME", "");
        }
        data.put("DEPLOY_BUNDLE_SHORT_VERSION",
                VERSION.fetchFrom(params) != null ? VERSION.fetchFrom(params) : "1.0.0");
        data.put("DEPLOY_BUNDLE_CATEGORY",
                //TODO parameters should provide set of values for IDEs
                MAC_CATEGORY.validatedFetchFrom(params));

        //TODO NOT THE WAY TODO THIS but good enough for first pass
        data.put("DEPLOY_MAIN_JAR_NAME", new BundleParams(params).getMainApplicationJar());
//        data.put("DEPLOY_MAIN_JAR_NAME", MAIN_JAR.fetchFrom(params).toString());

        data.put("DEPLOY_PREFERENCES_ID", PREFERENCES_ID.fetchFrom(params).toLowerCase());

        StringBuilder sb = new StringBuilder();
        List<String> jvmOptions = JVM_OPTIONS.fetchFrom(params);

        String newline = ""; //So we don't add unneccessary extra line after last append
        for (String o : jvmOptions) {
            sb.append(newline).append("    <string>").append(o).append("</string>");
            newline = "\n";
        }
        data.put("DEPLOY_JVM_OPTIONS", sb.toString());

        newline = "";
        sb = new StringBuilder();
        Map<String, String> overridableJVMOptions = USER_JVM_OPTIONS.fetchFrom(params);
        for (Map.Entry<String, String> arg: overridableJVMOptions.entrySet()) {
            sb.append(newline);
            sb.append("      <key>").append(arg.getKey()).append("</key>\n");
            sb.append("      <string>").append(arg.getValue()).append("</string>");
            newline = "\n";
        }
        data.put("DEPLOY_JVM_USER_OPTIONS", sb.toString());


        //TODO UNLESS we are supporting building for jre7, this is unnecessary
//        if (params.useJavaFXPackaging()) {
//            data.put("DEPLOY_LAUNCHER_CLASS", JAVAFX_LAUNCHER_CLASS);
//        } else {
        data.put("DEPLOY_LAUNCHER_CLASS", MAIN_CLASS.fetchFrom(params));
//        }
        // This will be an empty string for correctly packaged JavaFX apps
        data.put("DEPLOY_APP_CLASSPATH", MAIN_JAR_CLASSPATH.fetchFrom(params));

        //TODO: Add remainder of the classpath

        Writer w = new BufferedWriter(new FileWriter(file));
        w.write(preprocessTextResource(
                MAC_BUNDLER_PREFIX + getConfig_InfoPlist(params).getName(),
                I18N.getString("resource.bundle-config-file"), TEMPLATE_INFO_PLIST, data,
                VERBOSE.fetchFrom(params)));
        w.close();

    }

    private void writePkgInfo(File file) throws IOException {

        //hardcoded as it does not seem we need to change it ever
        String signature = "????";

        try (Writer out = new BufferedWriter(new FileWriter(file))) {
            out.write(OS_TYPE_CODE + signature);
            out.flush();
        }
    }

    //////////////////////////////////////////////////////////////////////////////////
    // Implement Bundler
    //////////////////////////////////////////////////////////////////////////////////

    @Override
    public String getName() {
        return I18N.getString("bundler.name");
    }

    @Override
    public String getDescription() {
        return I18N.getString("bundler.description");
    }

    @Override
    public String getID() {
        return "mac.app";
    }

    @Override
    public String getBundleType() {
        return "IMAGE";
    }

    @Override
    public Collection<BundlerParamInfo<?>> getBundleParameters() {
        return getAppBundleParameters();
    }

    public static Collection<BundlerParamInfo<?>> getAppBundleParameters() {
        return Arrays.asList(
                APP_NAME,
                APP_RESOURCES,
                BUILD_ROOT,
                JVM_OPTIONS,
                MAIN_CLASS,
                MAIN_JAR,
                MAIN_JAR_CLASSPATH,
                PREFERENCES_ID,
                RAW_EXECUTABLE_URL,
                MAC_RUNTIME,
                USER_JVM_OPTIONS,
                VERSION,
                ICON,
                MAC_CATEGORY
        );
    }


    @Override
    public File execute(Map<String, ? super Object> params, File outputParentDir) {
        return doBundle(params, outputParentDir, false);
    }
}
