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
import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.resource.mac.MacResources;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static com.oracle.bundlers.StandardBundlerParam.*;
import static com.oracle.bundlers.StandardBundlerParam.USER_JVM_OPTIONS;
import static com.oracle.bundlers.StandardBundlerParam.VERSION;
import static com.oracle.bundlers.mac.MacBaseInstallerBundler.getPredefinedImage;

public class MacAppBundler extends AbstractBundler {
    private File configRoot = null;
    private Map<String, ? super Object> params;

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

    public static final EnumeratedBundlerParam<String> MAC_CATEGORY  =
            new EnumeratedBundlerParam<>(
                    "Category",
                    "Mac Categories. Note that the key is the string to display to the user and the value is the id of the category",
                    "LSApplicationCategoryType",
                    String.class,
                    null,
                    params -> "Unknown",
                    false,
                    s -> s,
                    getMacCategories(),
                    false //strict - for MacStoreBundler this should be strict
            );

    public static final BundlerParamInfo<URL> RAW_EXECUTABLE_URL = new StandardBundlerParam<>(
            "Launcher URL", "Override the packager default launcher with a custom launcher.", "mac.launcher.url",
            URL.class, null, params -> MacResources.class.getResource(EXECUTABLE_NAME),
            false, s -> {
        try {
            return new URL(s);
        } catch (MalformedURLException e) {
            Log.info(e.toString());
            return null;
        }
    });

    private void setBuildRoot(File dir) {
        configRoot = new File(dir, "macosx");
        configRoot.mkdirs();
    }

    public MacAppBundler() {
        super();
        baseResourceLoader = MacResources.class;
    }

    @Override
    public boolean validate(Map<String, ? super Object> params) throws UnsupportedPlatformException, ConfigException {
        logParameters(params);
        return doValidate(params);
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

        if (StandardBundlerParam.MAIN_JAR.fetchFrom(p) == null) {
            throw new ConfigException(
                    "Main application jar is missing.",
                    "Make sure to use fx:jar task to create main application jar.");
        }

        //validate required inputs
        if (USE_FX_PACKAGING.fetchFrom(p)) {
            testRuntime(p, new String[] {"Contents/Home/jre/lib/ext/jfxrt.jar", "Contents/Home/jre/lib/jfxrt.jar"});
        }

        return true;
    }


    private File getConfig_InfoPlist() {
        return new File(configRoot, "Info.plist");
    }

    private File getConfig_Icon() {
        return new File(configRoot, NAME.fetchFrom(params) + ".icns");
    }

    private void prepareConfigFiles() throws IOException {
        File infoPlistFile = getConfig_InfoPlist();
        infoPlistFile.createNewFile();
        writeInfoPlist(infoPlistFile);

        // Copy icon to Resources folder
        prepareIcon();
    }

    public File doBundle(Map<String, ? super Object> p, File outputDirectory, boolean dependentTask) {
        File rootDirectory = null;
        try {
            final File predefinedImage = getPredefinedImage(p);
            if (predefinedImage != null) {
                return predefinedImage;
            }
            params = p;

            File file = BUILD_ROOT.fetchFrom(p);
            setBuildRoot(file);

            //prepare config resources (we will copy them to the bundle later)
            // NB: explicitly saving them to simplify customization
            prepareConfigFiles();

            // Create directory structure
            rootDirectory = new File(outputDirectory, NAME.fetchFrom(p) + ".app");
            IOUtils.deleteRecursive(rootDirectory);
            rootDirectory.mkdirs();

            if (!dependentTask) {
                Log.info("Creating app bundle: " + rootDirectory.getAbsolutePath());
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
            File executableFile = new File(macOSDirectory, getLauncherName());
            IOUtils.copyFromURL(
                    RAW_EXECUTABLE_URL.fetchFrom(p),
                    executableFile);

            executableFile.setExecutable(true, false);

            // Copy runtime to PlugIns folder
            copyRuntime(plugInsDirectory);

            // Copy class path entries to Java folder
            copyClassPathEntries(javaDirectory);

//TODO: Need to support adding native libraries.
            // Copy library path entries to MacOS folder
            //copyLibraryPathEntries(macOSDirectory);

            /*********** Take care of "config" files *******/
            // Copy icon to Resources folder
            IOUtils.copyFile(getConfig_Icon(),
                    new File(resourcesDirectory, getConfig_Icon().getName()));
            // Generate Info.plist
            IOUtils.copyFile(getConfig_InfoPlist(),
                    new File(contentsDirectory, "Info.plist"));
        } catch (ConfigException e) {
            Log.info("Bundler " + getName() + " skipped because of a configuration problem: " + e.getMessage() + "\nAdvice to fix: " + e.getAdvice());
        } catch (IOException ex) {
            Log.verbose(ex);
            return null;
        } finally {
            if (!verbose) {
                //cleanup
                cleanupConfigFiles();
            } else {
                Log.info("Config files are saved to " +
                        configRoot.getAbsolutePath()  +
                        ". Use them to customize package.");
            }
        }
        return rootDirectory;
    }

    public String getAppName() {
        return  NAME.fetchFrom(params) + ".app";
    }

    protected void cleanupConfigFiles() {
        //Since building the app can be bypassed, make sure configRoot was set
        if (configRoot != null) {
            if (getConfig_Icon() != null) {
                getConfig_Icon().delete();
            }
            if (getConfig_InfoPlist() != null) {
                getConfig_InfoPlist().delete();
            }
        }
    }


    private void copyClassPathEntries(File javaDirectory) throws IOException {
        RelativeFileSet classPath = APP_RESOURCES.fetchFrom(params);
        if (classPath == null) {
            throw new RuntimeException("Null app resources?");
        }
        File srcdir = classPath.getBaseDirectory();
        for (String fname : classPath.getIncludedFiles()) {
            IOUtils.copyFile(
                    new File(srcdir, fname), new File(javaDirectory, fname));
        }
    }

    private void copyRuntime(File plugInsDirectory) throws IOException {
        RelativeFileSet runTime = RUNTIME.fetchFrom(params);
        if (runTime == null) {
            //request to use system runtime => do not bundle
            return;
        }
        plugInsDirectory.mkdirs();

        File srcdir = runTime.getBaseDirectory();
        File destDir = new File(plugInsDirectory, srcdir.getName());
        Set<String> filesToCopy = runTime.getIncludedFiles();

        // We don't need the symlink to libjli or the JRE's info.plist.
        // We are going to load it directly.
        filesToCopy.remove("Contents/MacOS/libjli.dylib");
        filesToCopy.remove("Contents/Info.plist");
        for (String fname : filesToCopy) {
            IOUtils.copyFile(
                    new File(srcdir, fname), new File(destDir, fname));
        }
    }


    // get Name from bundle params
    private String NAME() {
        return NAME.fetchFrom(params);
    }

    private void prepareIcon() throws IOException {
        File icon = ICON.fetchFrom(params);
        if (icon == null || !icon.exists()) {
            fetchResource(MAC_BUNDLER_PREFIX+ NAME() +".icns",
                    "icon",
                    TEMPLATE_BUNDLE_ICON,
                    getConfig_Icon());
        } else {
            fetchResource(MAC_BUNDLER_PREFIX+ NAME() +".icns",
                    "icon",
                    icon,
                    getConfig_Icon());
        }
    }

    private String getLauncherName() {
        if (NAME() != null) {
            return NAME();
        } else {
            return MAIN_CLASS.fetchFrom(params);
        }
    }

    private String getBundleName() {
        //TODO: Check to see what rules/limits are in place for CFBundleName
        if (NAME() != null) {
            return NAME();
        } else {
            String nm = MAIN_CLASS.fetchFrom(params);
            if (nm.length() > 16) {
                nm = nm.substring(0, 16);
            }
            return nm;
        }
    }

    private String getBundleIdentifier() {
        //TODO: Check to see what rules/limits are in place for CFBundleIdentifier
        return  IDENTIFIER.fetchFrom(params);
    }

    private void writeInfoPlist(File file) throws IOException {
        Log.verbose("Preparing Info.plist: "+file.getAbsolutePath());

        //prepare config for exe
        //Note: do not need CFBundleDisplayName if we do not support localization
        Map<String, String> data = new HashMap<>();
        data.put("DEPLOY_ICON_FILE", getConfig_Icon().getName());
        data.put("DEPLOY_BUNDLE_IDENTIFIER",
                getBundleIdentifier().toLowerCase());
        data.put("DEPLOY_BUNDLE_NAME",
                getBundleName());
        data.put("DEPLOY_BUNDLE_COPYRIGHT",
                COPYRIGHT.fetchFrom(params) != null ? COPYRIGHT.fetchFrom(params) : "Unknown");
        data.put("DEPLOY_LAUNCHER_NAME", getLauncherName());
        if (RUNTIME.fetchFrom(params) != null) {
            data.put("DEPLOY_JAVA_RUNTIME_NAME",
                    RUNTIME.fetchFrom(params).getBaseDirectory().getName());
        } else {
            data.put("DEPLOY_JAVA_RUNTIME_NAME", "");
        }
        data.put("DEPLOY_BUNDLE_SHORT_VERSION",
                VERSION.fetchFrom(params) != null ? VERSION.fetchFrom(params) : "1.0.0");
        data.put("DEPLOY_BUNDLE_CATEGORY",
                //TODO parameters should provide set of values for IDEs
                CATEGORY.fetchFrom(params) != null ?
                        CATEGORY.fetchFrom(params) : "unknown");

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
                MAC_BUNDLER_PREFIX + getConfig_InfoPlist().getName(),
                "Bundle config file", TEMPLATE_INFO_PLIST, data));
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
        return "Mac Application Image";
    }

    @Override
    public String getDescription() {
        return "A Directory based image of a mac Application with an optionally co-bundled JRE.  Used as a base for the Installer bundlers";
    }

    @Override
    public String getID() {
        return "mac.app";
    }

    @Override
    public BundleType getBundleType() {
        return BundleType.IMAGE;
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
                RUNTIME,
                USE_FX_PACKAGING,
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
