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
package com.oracle.tools.packager.mac;

import com.oracle.tools.packager.AbstractBundler;
import com.oracle.tools.packager.BundlerParamInfo;
import com.oracle.tools.packager.EnumeratedBundlerParam;
import com.oracle.tools.packager.JreUtils;
import com.oracle.tools.packager.JreUtils.Rule;
import com.oracle.tools.packager.StandardBundlerParam;
import com.oracle.tools.packager.Log;
import com.sun.javafx.tools.packager.bundlers.BundleParams;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.IOUtils;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.UnsupportedPlatformException;

import java.io.*;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;

import static com.oracle.tools.packager.StandardBundlerParam.*;
import static com.oracle.tools.packager.mac.MacBaseInstallerBundler.SIGNING_KEY_USER;
import static com.oracle.tools.packager.mac.MacBaseInstallerBundler.getPredefinedImage;

public class MacAppBundler extends AbstractBundler {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle(MacAppBundler.class.getName());

    public final static String MAC_BUNDLER_PREFIX =
            BUNDLER_PREFIX + "macosx" + File.separator;

    private static final String EXECUTABLE_NAME      = "JavaAppLauncher";
    private final static String LIBRARY_NAME         = "libpackager.dylib";
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
                    I18N.getString("param.category-name"),
                    I18N.getString("param.category-name.description"),
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
                    I18N.getString("param.cfbundle-name.name"),
                    I18N.getString("param.cfbundle-name.description"),
                    "mac.CFBundleName",
                    String.class,
                    params -> null,
                    (s, p) -> s);

    public static final BundlerParamInfo<String> MAC_CF_BUNDLE_IDENTIFIER =
            new StandardBundlerParam<>(
                    I18N.getString("param.cfbundle-identifier.name"),
                    I18N.getString("param.cfbundle-identifier.description"),
                    "mac.CFBundleIdentifier",
                    String.class,
                    IDENTIFIER::fetchFrom,
                    (s, p) -> s);

    public static final BundlerParamInfo<String> MAC_CF_BUNDLE_VERSION =
            new StandardBundlerParam<>(
                    I18N.getString("param.cfbundle-version.name"),
                    I18N.getString("param.cfbundle-version.description"),
                    "mac.CFBundleVersion",
                    String.class,
                    p -> {
                        String s = VERSION.fetchFrom(p);
                        if (validCFBundleVersion(s)) {
                            return s;
                        } else {
                            return "100";
                        }
                    },
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
            I18N.getString("param.raw-executable-url.name"),
            I18N.getString("param.raw-executable-url.description"),
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
            I18N.getString("param.default-icon-icns"),
            I18N.getString("param.default-icon-icns.description"),
            ".mac.default.icns",
            String.class,
            params -> TEMPLATE_BUNDLE_ICON,
            (s, p) -> s);

    public static final BundlerParamInfo<Rule[]> MAC_RULES = new StandardBundlerParam<>(
            "",
            "",
            ".mac.runtime.rules",
            Rule[].class,
            MacAppBundler::createMacRuntimeRules,
            (s, p) -> null
    );

    public static final BundlerParamInfo<RelativeFileSet> MAC_RUNTIME = new StandardBundlerParam<>(
            I18N.getString("param.runtime.name"),
            I18N.getString("param.runtime.description"),
            BundleParams.PARAM_RUNTIME,
            RelativeFileSet.class,
            params -> extractMacRuntime(System.getProperty("java.home"), params),
            MacAppBundler::extractMacRuntime
    );

    public static final BundlerParamInfo<String> DEVELOPER_ID_APP_SIGNING_KEY = new StandardBundlerParam<>(
            I18N.getString("param.signing-key-developer-id-app.name"),
            I18N.getString("param.signing-key-developer-id-app.description"),
            "mac.signing-key-developer-id-app",
            String.class,
            params -> MacBaseInstallerBundler.findKey("Developer ID Application: " + SIGNING_KEY_USER.fetchFrom(params), VERBOSE.fetchFrom(params)),
            (s, p) -> s);

    public static final BundlerParamInfo<String> BUNDLE_ID_SIGNING_PREFIX = new StandardBundlerParam<>(
            I18N.getString("param.bundle-id-signing-prefix.name"),
            I18N.getString("param.bundle-id-signing-prefix.description"),
            "mac.bundle-id-signing-prefix",
            String.class,
            params -> IDENTIFIER.fetchFrom(params) + ".",
            (s, p) -> s);

    public static final BundlerParamInfo<File> ICON_ICNS = new StandardBundlerParam<>(
            I18N.getString("param.icon-icns.name"),
            I18N.getString("param.icon-icns.description"),
            "icon.icns",
            File.class,
            params -> {
                File f = ICON.fetchFrom(params);
                if (f != null && !f.getName().toLowerCase().endsWith(".icns")) {
                    Log.info(MessageFormat.format(I18N.getString("message.icon-not-icns"), f));
                    return null;
                }
                return f;
            },
            (s, p) -> new File(s));

    public static RelativeFileSet extractMacRuntime(String base, Map<String, ? super Object> params) {
        if (base.isEmpty()) {
            return null;
        }

        File workingBase = new File(base);
        workingBase = workingBase.getAbsoluteFile();
        try {
            workingBase = workingBase.getCanonicalFile();
        } catch (IOException ignore) {
            // we tried, workingBase will remain absolute and not canonical.
        }
        
        if (workingBase.getName().equals("jre")) {
            workingBase = workingBase.getParentFile();
        }
        if (workingBase.getName().equals("Home")) {
            workingBase = workingBase.getParentFile();
        }
        if (workingBase.getName().equals("Contents")) {
            workingBase = workingBase.getParentFile();
        }
        return JreUtils.extractJreAsRelativeFileSet(workingBase.toString(),
                MAC_RULES.fetchFrom(params), true);
    }

    public MacAppBundler() {
        super();
        baseResourceLoader = MacResources.class;
    }

    public static boolean validCFBundleVersion(String v) {
        // CFBundleVersion (String - iOS, OS X) specifies the build version
        // number of the bundle, which identifies an iteration (released or
        // unreleased) of the bundle. The build version number should be a
        // string comprised of three non-negative, period-separated integers
        // with the first integer being greater than zero. The string should
        // only contain numeric (0-9) and period (.) characters. Leading zeros
        // are truncated from each integer and will be ignored (that is,
        // 1.02.3 is equivalent to 1.2.3). This key is not localizable.

        if (v == null) {
            return false;
        }

        String p[] = v.split("\\.");
        if (p.length > 3 || p.length < 1) {
            Log.verbose(I18N.getString("message.version-string-too-many-components"));
            return false;
        }

        try {
            BigInteger n = new BigInteger(p[0]);
            if (BigInteger.ONE.compareTo(n) > 0) {
                Log.verbose(I18N.getString("message.version-string-first-number-not-zero"));
                return false;
            }
            if (p.length > 1) {
                n = new BigInteger(p[1]);
                if (BigInteger.ZERO.compareTo(n) > 0) {
                    Log.verbose(I18N.getString("message.version-string-no-negative-numbers"));
                    return false;
                }
            }
            if (p.length > 2) {
                n = new BigInteger(p[2]);
                if (BigInteger.ZERO.compareTo(n) > 0) {
                    Log.verbose(I18N.getString("message.version-string-no-negative-numbers"));
                    return false;
                }
            }
        } catch (NumberFormatException ne) {
            Log.verbose(I18N.getString("message.version-string-numbers-only"));
            Log.verbose(ne);
            return false;
        }

        return true;
    }

    @Override
    public boolean validate(Map<String, ? super Object> params) throws UnsupportedPlatformException, ConfigException {
        try {
            return doValidate(params);
        } catch (RuntimeException re) {
            if (re.getCause() instanceof ConfigException) {
                throw (ConfigException) re.getCause();
            } else {
                throw new ConfigException(re);
            }
        }
    }

    //to be used by chained bundlers, e.g. by EXE bundler to avoid
    // skipping validation if p.type does not include "image"
    public boolean doValidate(Map<String, ? super Object> p) throws UnsupportedPlatformException, ConfigException {
        if (!System.getProperty("os.name").toLowerCase().contains("os x")) {
            throw new UnsupportedPlatformException();
        }

        StandardBundlerParam.validateMainClassInfoFromAppResources(p);

        Map<String, String> userJvmOptions = USER_JVM_OPTIONS.fetchFrom(p);
        if (userJvmOptions != null) {
            for (Map.Entry<String, String> entry : userJvmOptions.entrySet()) {
                if (entry.getValue() == null || entry.getValue().isEmpty()) {
                    throw new ConfigException(
                            MessageFormat.format(I18N.getString("error.empty-user-jvm-option-value"), entry.getKey()),
                            I18N.getString("error.empty-user-jvm-option-value.advice"));
                }
            }
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
        testRuntime(MAC_RUNTIME.fetchFrom(p), new String[] {
                "Contents/Home/(jre/)?lib/[^/]+/libjvm.dylib", // most reliable
                "Contents/Home/(jre/)?lib/rt.jar", // fallback canary for JDK 8
        });
        if (USE_FX_PACKAGING.fetchFrom(p)) {
            testRuntime(MAC_RUNTIME.fetchFrom(p), new String[] {"Contents/Home/(jre/)?lib/ext/jfxrt.jar", "Contents/Home/(jre/)?lib/jfxrt.jar"});
        }

        // validate short version
        if (!validCFBundleVersion(MAC_CF_BUNDLE_VERSION.fetchFrom(p))) {
            throw new ConfigException(
                    I18N.getString("error.invalid-cfbundle-version"),
                    I18N.getString("error.invalid-cfbundle-version.advice"));
        }
        
        // reject explicitly set sign to true and no valid signature key
        if (Optional.ofNullable(SIGN_BUNDLE.fetchFrom(p)).orElse(Boolean.FALSE)) {
            String signingIdentity = DEVELOPER_ID_APP_SIGNING_KEY.fetchFrom(p);
            if (signingIdentity == null) {
                throw new ConfigException(
                        I18N.getString("error.explicit-sign-no-cert"),
                        I18N.getString("error.explicit-sign-no-cert.advice"));
            }
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
        if (!outputDirectory.isDirectory() && !outputDirectory.mkdirs()) {
            throw new RuntimeException(MessageFormat.format(I18N.getString("error.cannot-create-output-dir"), outputDirectory.getAbsolutePath()));
        }
        if (!outputDirectory.canWrite()) {
            throw new RuntimeException(MessageFormat.format(I18N.getString("error.cannot-write-to-output-dir"), outputDirectory.getAbsolutePath()));
        }

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

            // Copy library to the MacOS folder
            IOUtils.copyFromURL(
                    MacResources.class.getResource(LIBRARY_NAME),
                    new File(macOSDirectory, LIBRARY_NAME));

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

            // copy file association icons
            for (Map<String, ? super Object> fa : FILE_ASSOCIATIONS.fetchFrom(p)) {
                File f = FA_ICON.fetchFrom(fa);
                if (f != null && f.exists()) {
                    IOUtils.copyFile(f,
                            new File(resourcesDirectory, f.getName()));
                }
            }


            // Generate Info.plist
            IOUtils.copyFile(getConfig_InfoPlist(p),
                    new File(contentsDirectory, "Info.plist"));

            // maybe sign
            if (Optional.ofNullable(SIGN_BUNDLE.fetchFrom(p)).orElse(Boolean.TRUE)) {
                String signingIdentity = DEVELOPER_ID_APP_SIGNING_KEY.fetchFrom(p);
                if (signingIdentity != null) {
                    MacBaseInstallerBundler.signAppBundle(p, rootDirectory, signingIdentity, BUNDLE_ID_SIGNING_PREFIX.fetchFrom(p));
                }
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
        List<RelativeFileSet> resourcesList = APP_RESOURCES_LIST.fetchFrom(params);
        if (resourcesList == null) {
            throw new RuntimeException(I18N.getString("message.null-classpath"));
        }
        
        for (RelativeFileSet classPath : resourcesList) {
            File srcdir = classPath.getBaseDirectory();
            for (String fname : classPath.getIncludedFiles()) {
                IOUtils.copyFile(
                        new File(srcdir, fname), new File(javaDirectory, fname));
            }
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
        File destDir = new File(plugInsDirectory, "Java");
        Set<String> filesToCopy = runTime.getIncludedFiles();

        for (String fname : filesToCopy) {
            IOUtils.copyFile(
                    new File(srcdir, fname), new File(destDir, fname));
        }
    }

    private void prepareIcon(Map<String, ? super Object> params) throws IOException {
        File icon = ICON_ICNS.fetchFrom(params);
        if (icon == null || !icon.exists()) {
            fetchResource(MAC_BUNDLER_PREFIX+ APP_NAME.fetchFrom(params) +".icns",
                    "icon",
                    DEFAULT_ICNS_ICON.fetchFrom(params),
                    getConfig_Icon(params),
                    VERBOSE.fetchFrom(params),
                    DROP_IN_RESOURCES_ROOT.fetchFrom(params));
        } else {
            fetchResource(MAC_BUNDLER_PREFIX+ APP_NAME.fetchFrom(params) +".icns",
                    "icon",
                    icon,
                    getConfig_Icon(params),
                    VERBOSE.fetchFrom(params),
                    DROP_IN_RESOURCES_ROOT.fetchFrom(params));
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

    private void writeInfoPlist(File file, Map<String, ? super Object> params) throws IOException {
        Log.verbose(MessageFormat.format(I18N.getString("message.preparing-info-plist"), file.getAbsolutePath()));

        //prepare config for exe
        //Note: do not need CFBundleDisplayName if we do not support localization
        Map<String, String> data = new HashMap<>();
        data.put("DEPLOY_ICON_FILE", getConfig_Icon(params).getName());
        data.put("DEPLOY_BUNDLE_IDENTIFIER",
                MAC_CF_BUNDLE_IDENTIFIER.fetchFrom(params));
        data.put("DEPLOY_BUNDLE_NAME",
                getBundleName(params));
        data.put("DEPLOY_BUNDLE_COPYRIGHT",
                COPYRIGHT.fetchFrom(params) != null ? COPYRIGHT.fetchFrom(params) : "Unknown");
        data.put("DEPLOY_LAUNCHER_NAME", getLauncherName(params));
        if (MAC_RUNTIME.fetchFrom(params) != null) {
            data.put("DEPLOY_JAVA_RUNTIME_NAME", "$APPDIR/plugins/Java");
        } else {
            data.put("DEPLOY_JAVA_RUNTIME_NAME", "");
        }
        data.put("DEPLOY_BUNDLE_SHORT_VERSION",
                VERSION.fetchFrom(params) != null ? VERSION.fetchFrom(params) : "1.0.0");
        data.put("DEPLOY_BUNDLE_CFBUNDLE_VERSION",
                MAC_CF_BUNDLE_VERSION.fetchFrom(params) != null ? MAC_CF_BUNDLE_VERSION.fetchFrom(params) : "100");
        data.put("DEPLOY_BUNDLE_CATEGORY",
                //TODO parameters should provide set of values for IDEs
                MAC_CATEGORY.validatedFetchFrom(params));

        data.put("DEPLOY_MAIN_JAR_NAME", MAIN_JAR.fetchFrom(params).getIncludedFiles().iterator().next());

        data.put("DEPLOY_PREFERENCES_ID", PREFERENCES_ID.fetchFrom(params).toLowerCase());

        StringBuilder sb = new StringBuilder();
        List<String> jvmOptions = JVM_OPTIONS.fetchFrom(params);

        String newline = ""; //So we don't add unneccessary extra line after last append
        for (String o : jvmOptions) {
            sb.append(newline).append("    <string>").append(o).append("</string>");
            newline = "\n";
        }

        Map<String, String> jvmProps = JVM_PROPERTIES.fetchFrom(params);
        for (Map.Entry<String, String> entry : jvmProps.entrySet()) {
            sb.append(newline)
                    .append("    <string>-D")
                    .append(entry.getKey())
                    .append("=")
                    .append(entry.getValue())
                    .append("</string>");
            newline = "\n";
        }

        String preloader = PRELOADER_CLASS.fetchFrom(params);
        if (preloader != null) {
            sb.append(newline)
                    .append("    <string>-Djavafx.preloader=")
                    .append(preloader)
                    .append("</string>");
            //newline = "\n";
        }

        data.put("DEPLOY_JVM_OPTIONS", sb.toString());

        sb = new StringBuilder();
        List<String> args = ARGUMENTS.fetchFrom(params);
        newline = ""; //So we don't add unneccessary extra line after last append
        for (String o : args) {
            sb.append(newline).append("    <string>").append(o).append("</string>");
            newline = "\n";
        }
        data.put("DEPLOY_ARGUMENTS", sb.toString());

        newline = "";
        sb = new StringBuilder();
        Map<String, String> overridableJVMOptions = USER_JVM_OPTIONS.fetchFrom(params);
        for (Map.Entry<String, String> arg: overridableJVMOptions.entrySet()) {
            sb.append(newline)
                .append("      <key>").append(arg.getKey()).append("</key>\n")
                .append("      <string>").append(arg.getValue()).append("</string>");
            newline = "\n";
        }
        data.put("DEPLOY_JVM_USER_OPTIONS", sb.toString());


        data.put("DEPLOY_LAUNCHER_CLASS", MAIN_CLASS.fetchFrom(params));

        StringBuilder macroedPath = new StringBuilder();
        for (String s : CLASSPATH.fetchFrom(params).split("[ ;:]+")) {
            macroedPath.append(s);
            macroedPath.append(":");
        }
        macroedPath.deleteCharAt(macroedPath.length() - 1);

        data.put("DEPLOY_APP_CLASSPATH", macroedPath.toString());

        //TODO: Add remainder of the classpath

        StringBuilder bundleDocumentTypes = new StringBuilder();
        StringBuilder exportedTypes = new StringBuilder();
        for (Map<String, ? super Object> fileAssociation : FILE_ASSOCIATIONS.fetchFrom(params)) {

            List<String> extensions = FA_EXTENSIONS.fetchFrom(fileAssociation);
            
            if (extensions == null) {
                Log.info(I18N.getString("message.creating-association-with-null-extension"));
            }

            List<String> mimeTypes = FA_CONTENT_TYPE.fetchFrom(fileAssociation);
            String itemContentType = MAC_CF_BUNDLE_IDENTIFIER.fetchFrom(params) + "." + ((extensions == null || extensions.isEmpty())
                    ? "mime"
                    : extensions.get(0));
            String description = FA_DESCRIPTION.fetchFrom(fileAssociation);
            File icon = FA_ICON.fetchFrom(fileAssociation); //TODO FA_ICON_ICNS

            bundleDocumentTypes.append("    <dict>\n")
                .append("      <key>LSItemContentTypes</key>\n")
                .append("      <array>\n")
                .append("        <string>")
                .append(itemContentType)
                .append("</string>\n")
                .append("      </array>\n")
                .append("\n")
                .append("      <key>CFBundleTypeName</key>\n")
                .append("      <string>")
                .append(description)
                .append("</string>\n")
                .append("\n")
                .append("      <key>LSHandlerRank</key>\n")
                .append("      <string>Owner</string>\n") //TODO make a bundler arg
                .append("\n")
                .append("      <key>CFBundleTypeRole</key>\n")
                .append("      <string>Editor</string>\n") // TODO make a bundler arg
                .append("\n")
                .append("      <key>LSIsAppleDefaultForType</key>\n")
                .append("      <true/>\n") // TODO make a bundler arg
                .append("\n");

            if (icon != null && icon.exists()) {
                //?
                bundleDocumentTypes.append("      <key>CFBundleTypeIconFile</key>\n")
                        .append("      <string>")
                        .append(icon.getName())
                        .append("</string>\n");
            }
            bundleDocumentTypes.append("    </dict>\n");

            exportedTypes.append("    <dict>\n")
                .append("      <key>UTTypeIdentifier</key>\n")
                .append("      <string>")
                .append(itemContentType)
                .append("</string>\n")
                .append("\n")
                .append("      <key>UTTypeDescription</key>\n")
                .append("      <string>")
                .append(description)
                .append("</string>\n")
                .append("      <key>UTTypeConformsTo</key>\n")
                .append("      <array>\n")
                .append("          <string>public.data</string>\n") //TODO expose this?
                .append("      </array>\n")
                .append("\n");
            
            if (icon != null && icon.exists()) {
                exportedTypes.append("      <key>UTTypeIconFile</key>\n")
                    .append("      <string>")
                    .append(icon.getName())
                    .append("</string>\n")
                    .append("\n");
            }

            exportedTypes.append("\n")
                .append("      <key>UTTypeTagSpecification</key>\n")
                .append("      <dict>\n")
            //TODO expose via param? .append("        <key>com.apple.ostype</key>\n");
            //TODO expose via param? .append("        <string>ABCD</string>\n")
                .append("\n");

            if (extensions != null && !extensions.isEmpty()) {
                exportedTypes.append("        <key>public.filename-extension</key>\n")
                    .append("        <array>\n");

                for (String ext : extensions) {
                    exportedTypes.append("          <string>")
                        .append(ext)
                        .append("</string>\n");
                }
                exportedTypes.append("        </array>\n");
            }
            if (mimeTypes != null && !mimeTypes.isEmpty()) {
                exportedTypes.append("        <key>public.mime-type</key>\n")
                    .append("        <array>\n");

                for (String mime : mimeTypes) {
                    exportedTypes.append("          <string>")
                        .append(mime)
                        .append("</string>\n");
                }
                exportedTypes.append("        </array>\n");
            }
            exportedTypes.append("      </dict>\n")
                    .append("    </dict>\n");
        }
        String associationData;
        if (bundleDocumentTypes.length() > 0) {
            associationData = "\n  <key>CFBundleDocumentTypes</key>\n  <array>\n"
                    + bundleDocumentTypes.toString()
                    + "  </array>\n\n  <key>UTExportedTypeDeclarations</key>\n  <array>\n"
                    + exportedTypes.toString()
                    + "  </array>\n";
        } else {
            associationData = "";
        }
        data.put("DEPLOY_FILE_ASSOCIATIONS", associationData);


        Writer w = new BufferedWriter(new FileWriter(file));
        w.write(preprocessTextResource(
                MAC_BUNDLER_PREFIX + getConfig_InfoPlist(params).getName(),
                I18N.getString("resource.bundle-config-file"), TEMPLATE_INFO_PLIST, data,
                VERBOSE.fetchFrom(params),
                DROP_IN_RESOURCES_ROOT.fetchFrom(params)));
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

    public static Rule[] createMacRuntimeRules(Map<String, ? super Object> params) {
        if (!System.getProperty("os.name").toLowerCase().contains("os x")) {
            // we will never get a sensible answer unless we are running on OSX,
            // so quit now and return null indicating 'no sensible value'
            return null;
        }

        //Subsetting of JRE is restricted.
        //JRE README defines what is allowed to strip:
        //   ﻿http://www.oracle.com/technetwork/java/javase/jre-8-readme-2095710.html
        //

        List<Rule> rules = new ArrayList<>();

        File baseDir;

        if (params.containsKey(MAC_RUNTIME.getID())) {
            Object o = params.get(MAC_RUNTIME.getID());
            if (o instanceof RelativeFileSet) {

                baseDir = ((RelativeFileSet)o).getBaseDirectory();
            } else {
                baseDir = new File(o.toString());
            }
        } else {
            baseDir = new File(System.getProperty("java.home"));
        }

        // we accept either pointing at the directories typically installed at:
        // /Libraries/Java/JavaVirtualMachine/jdk1.8.0_40/
        //   * .
        //   * Contents/Home
        //   * Contents/Home/jre
        // /Library/Internet\ Plug-Ins/JavaAppletPlugin.plugin/
        //   * .
        //   * /Contents/Home
        // version may change, and if we don't detect any Contents/Home or Contents/Home/jre we will
        // presume we are at a root.

        if (!baseDir.exists()) {
            throw new RuntimeException(I18N.getString("error.non-existent-runtime"),
                new ConfigException(I18N.getString("error.non-existent-runtime"),
                    I18N.getString("error.non-existent-runtime.advice")));
        }

        boolean isJRE;
        boolean isJDK;

        try {
            String path = baseDir.getCanonicalPath();
            if (path.endsWith("/Contents/Home/jre")) {
                baseDir = baseDir.getParentFile().getParentFile().getParentFile();
            } else if (path.endsWith("/Contents/Home")) {
                baseDir = baseDir.getParentFile().getParentFile();
            }

            isJRE = new File(baseDir, "Contents/Home/lib/jli/libjli.dylib").exists();
            isJDK = new File(baseDir, "Contents/Home/jre/lib/jli/libjli.dylib").exists();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!(isJRE || isJDK)) {
            throw new RuntimeException(I18N.getString("error.cannot-detect-runtime-in-directory"),
                    new ConfigException(I18N.getString("error.cannot-detect-runtime-in-directory"),
                            I18N.getString("error.cannot-detect-runtime-in-directory.advice")));
        }

        // we need the Info.plist for signing
        rules.add(Rule.suffix("/contents/info.plist"));
        
        // Strip some JRE specific stuff
        if (isJRE) {
            rules.add(Rule.suffixNeg("/contents/disabled.plist"));
            rules.add(Rule.suffixNeg("/contents/enabled.plist"));
            rules.add(Rule.substrNeg("/contents/frameworks/"));
        }
        
        // strip out command line tools
        rules.add(Rule.suffixNeg("home/bin"));
        if (isJDK) {
            rules.add(Rule.suffixNeg("home/jre/bin"));
        }

        // strip out JRE stuff
        if (isJRE) {
            // update helper
            rules.add(Rule.suffixNeg("resources"));
            // interfacebuilder files
            rules.add(Rule.suffixNeg("lib/nibs"));
            // browser integration
            rules.add(Rule.suffixNeg("lib/libnpjp2.dylib"));
            // java webstart
            rules.add(Rule.suffixNeg("lib/security/javaws.policy"));
            rules.add(Rule.suffixNeg("lib/shortcuts"));

            // general deploy libraries
            rules.add(Rule.suffixNeg("lib/deploy"));
            rules.add(Rule.suffixNeg("lib/deploy.jar"));
            rules.add(Rule.suffixNeg("lib/javaws.jar"));
            rules.add(Rule.suffixNeg("lib/libdeploy.dylib"));
            rules.add(Rule.suffixNeg("lib/plugin.jar"));
        }

        // strip out man pages
        rules.add(Rule.suffixNeg("home/man"));

        // this is the build hashes, strip or keep?
        //rules.add(Rule.suffixNeg("home/release"));

        // strip out JDK stuff like JavaDB, JNI Headers, etc
        if (isJDK) {
            rules.add(Rule.suffixNeg("home/db"));
            rules.add(Rule.suffixNeg("home/demo"));
            rules.add(Rule.suffixNeg("home/include"));
            rules.add(Rule.suffixNeg("home/lib"));
            rules.add(Rule.suffixNeg("home/sample"));
            rules.add(Rule.suffixNeg("home/src.zip"));
            rules.add(Rule.suffixNeg("home/javafx-src.zip"));
        }

        //"home/rt" is not part of the official builds
        // but we may be creating this symlink to make older NB projects
        // happy. Make sure to not include it into final artifact
        rules.add(Rule.suffixNeg("home/rt"));

        //rules.add(Rule.suffixNeg("jre/lib/ext")); //need some of jars there for https to work

        // strip out flight recorder
        rules.add(Rule.suffixNeg("lib/jfr.jar"));

        return rules.toArray(new Rule[rules.size()]);
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
                // APP_RESOURCES_LIST, // ??
                ARGUMENTS,
                BUNDLE_ID_SIGNING_PREFIX,
                CLASSPATH,
                DEVELOPER_ID_APP_SIGNING_KEY,
                ICON_ICNS,
                JVM_OPTIONS,
                JVM_PROPERTIES,
                MAC_CATEGORY,
                MAC_CF_BUNDLE_IDENTIFIER,
                MAC_CF_BUNDLE_NAME,
                MAC_CF_BUNDLE_VERSION,
                MAC_RUNTIME,
                MAIN_CLASS,
                MAIN_JAR,
                PREFERENCES_ID,
                PRELOADER_CLASS,
                USER_JVM_OPTIONS,
                VERSION
        );
    }


    @Override
    public File execute(Map<String, ? super Object> params, File outputParentDir) {
        return doBundle(params, outputParentDir, false);
    }
}
