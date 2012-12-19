/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.resource.mac.MacResources;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


//Implementation is based on AppBundlerTask (rev 22)
public class MacAppBundler extends Bundler {
    private BundleParams params = null;
    private File configRoot = null;

    public final static String MAC_BUNDLER_PREFIX =
            BUNDLER_PREFIX + "macosx" + File.separator;

    private static final String EXECUTABLE_NAME = "JavaAppLauncher";
    static final String TEMPLATE_BUNDLE_ICON = "GenericApp.icns";
    private static final String OS_TYPE_CODE = "APPL";
    private static final String TEMPLATE_INFO_PLIST=  "Info.plist.template";

    public MacAppBundler() {
        super();
        baseResourceLoader = MacResources.class;
    }

    @Override
    protected void setBuildRoot(File dir) {
        super.setBuildRoot(dir);
        configRoot = new File(dir, "macosx");
        configRoot.mkdirs();
    }

    @Override
    boolean validate(BundleParams p) throws UnsupportedPlatformException, ConfigException {
        if (p.type != Bundler.BundleType.ALL && p.type != Bundler.BundleType.IMAGE) {
            return false;
        }
        return doValidate(p);
    }

    //used by chained bundlers to reuse validation logic
    boolean doValidate(BundleParams p) throws UnsupportedPlatformException, ConfigException {
        if (!System.getProperty("os.name").toLowerCase().contains("os x")) {
            throw new Bundler.UnsupportedPlatformException();
        }

        if (p.getMainApplicationJar() == null) {
            throw new Bundler.ConfigException(
                    "Main application jar is missing.",
                    "Make sure to use fx:jar task to create main application jar.");
        }

        //validate required inputs
        testRuntime(p, "Contents/Home/jre/lib/jfxrt.jar");

        return true;
    }

    private File getConfig_InfoPlist() {
        return new File(configRoot, "Info.plist");
    }

    private File getConfig_Icon() {
        return new File(configRoot, params.name + ".icns");
    }

    private void prepareConfigFiles() throws IOException {
        File infoPlistFile = getConfig_InfoPlist();
        infoPlistFile.createNewFile();
        writeInfoPlist(infoPlistFile);

        // Copy icon to Resources folder
        prepareIcon();
    }

    @Override
    public boolean bundle(BundleParams p, File outputDirectory) {
        return doBundle(p, outputDirectory, false);
    }

    boolean doBundle(BundleParams p, File outputDirectory, boolean dependentTask) {
        try {
            params = p;

            //prepare config resources (we will copy them to the bundle later)
            // NB: explicitly saving them to simplify customization
            prepareConfigFiles();

            // Create directory structure
            File rootDirectory = new File(outputDirectory, p.name + ".app");
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
                    MacResources.class.getResource(EXECUTABLE_NAME),
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
        } catch (IOException ex) {
            Log.verbose(ex);
            return false;
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
        return true;
    }

    protected void cleanupConfigFiles() {
        if (getConfig_Icon() != null) {
            getConfig_Icon().delete();
        }
        if (getConfig_InfoPlist() != null) {
            getConfig_InfoPlist().delete();
        }
    }

    @Override
    public String toString() {
        return "Mac Application Bundler";
    }

    private void copyClassPathEntries(File javaDirectory) throws IOException {
        if (params.appResources == null) {
            throw new RuntimeException("Null app resources?");
        }
        File srcdir = params.appResources.getBaseDirectory();
        for (String fname : params.appResources.getIncludedFiles()) {
            IOUtils.copyFile(
                    new File(srcdir, fname), new File(javaDirectory, fname));
        }
    }

    private void copyRuntime(File plugInsDirectory) throws IOException {
        if (params.runtime == null) {
            //request to use system runtime => do not bundle
            return;
        }
        plugInsDirectory.mkdirs();

        File srcdir = params.runtime.getBaseDirectory();
        File destDir = new File(plugInsDirectory, srcdir.getName());
        Set<String> filesToCopy = params.runtime.getIncludedFiles();

        // We don't need the symlink to libjli or the JRE's info.plist.
        // We are going to load it directly.
        filesToCopy.remove("Contents/MacOS/libjli.dylib");
        filesToCopy.remove("Contents/Info.plist");
        for (String fname : filesToCopy) {
            IOUtils.copyFile(
                    new File(srcdir, fname), new File(destDir, fname));
        }
    }


    private void prepareIcon() throws IOException {
        File icon = params.icon;
        if (icon == null || !params.icon.exists()) {
            fetchResource(MAC_BUNDLER_PREFIX+params.name+".icns",
                    "icon",
                    TEMPLATE_BUNDLE_ICON,
                    getConfig_Icon());
        } else {
            fetchResource(MAC_BUNDLER_PREFIX+params.name+".icns",
                    "icon",
                    icon,
                    getConfig_Icon());
        }
    }

    private String getLauncherName() {
        if (params.name != null) {
            return params.name;
        } else {
            return params.getMainClassName();
        }
    }

    private String getBundleName() {
        //TODO: Check to see what rules/limits are in place for CFBundleName
        if (params.name != null) {
            return params.name;
        } else {
            String nm = params.getMainClassName();
            if (nm.length() > 16) {
                nm = nm.substring(0, 16);
            }
            return nm;
        }
    }

    private String getBundleIdentifier() {
        //TODO: Check to see what rules/limits are in place for CFBundleIdentifier
        if (params.identifier != null) {
            return params.identifier;
        } else {
            return "unknown."+params.applicationClass;
        }
    }

    private void writeInfoPlist(File file) throws IOException {
        Log.verbose("Preparing Info.plist: "+file.getAbsolutePath());

        //prepare config for exe
        //Note: do not need CFBundleDisplayName if we do not support localization
        Map<String, String> data = new HashMap<String, String>();
        data.put("DEPLOY_ICON_FILE", getConfig_Icon().getName());
        data.put("DEPLOY_BUNDLE_IDENTIFIER",
                getBundleIdentifier());
        data.put("DEPLOY_BUNDLE_NAME",
                getBundleName());
        data.put("DEPLOY_BUNDLE_COPYRIGHT",
                params.copyright != null ? params.copyright : "Unknown");
        data.put("DEPLOY_LAUNCHER_NAME", getLauncherName());
        if (params.runtime != null) {
            data.put("DEPLOY_JAVA_RUNTIME_NAME",
                params.runtime.getBaseDirectory().getName());
        } else {
            data.put("DEPLOY_JAVA_RUNTIME_NAME", "");
        }
        data.put("DEPLOY_BUNDLE_SHORT_VERSION",
                params.appVersion != null ? params.appVersion : "1.0.0");
        data.put("DEPLOY_BUNDLE_CATEGORY",
                params.applicationCategory != null ?
                   params.applicationCategory : "unknown");
        data.put("DEPLOY_MAIN_JAR_NAME", params.getMainApplicationJar());

        StringBuilder sb = new StringBuilder();
        List<String> jvmOptions = params.getAllJvmOptions();
        for (String o : jvmOptions) {
            sb.append("    <string>").append(o).append("</string>\n");
        }
        data.put("DEPLOY_JVM_OPTIONS", sb.toString());

        if (params.useJavaFXPackaging()) {
            data.put("DEPLOY_LAUNCHER_CLASS", JAVAFX_LAUNCHER_CLASS);
        } else {
            data.put("DEPLOY_LAUNCHER_CLASS", params.applicationClass);
        }
        // This will be an empty string for correctly packaged JavaFX apps
        data.put("DEPLOY_APP_CLASSPATH", params.getAppClassPath());

        //TODO: Add remainder of the classpath

        Writer w = new BufferedWriter(new FileWriter(file));
        w.write(preprocessTextResource(
                MAC_BUNDLER_PREFIX + getConfig_InfoPlist().getName(),
                "Bundle config file", TEMPLATE_INFO_PLIST, data));
        w.close();

    }

    private void writePkgInfo(File file) throws IOException {
        Writer out = new BufferedWriter(new FileWriter(file));

        //hardcoded as it does not seem we need to change it ever
        String signature = "????";

        try {
            out.write(OS_TYPE_CODE + signature);
            out.flush();
        } finally {
            out.close();
        }
    }

}
