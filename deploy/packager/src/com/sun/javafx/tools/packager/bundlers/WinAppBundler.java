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
import com.sun.javafx.tools.resource.windows.WinResources;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

public class WinAppBundler extends Bundler {
    private BundleParams params;
    private File rootDirectory;
    private File configRoot;

    private final static String EXECUTABLE_NAME = "WinLauncher.exe";
    private static final String TOOL_ICON_SWAP="IconSwap.exe";

    public WinAppBundler() {
        super();
        baseResourceLoader = WinResources.class;
    }

    public final static String WIN_BUNDLER_PREFIX =
            BUNDLER_PREFIX + "windows/";

    @Override
    protected void setBuildRoot(File dir) {
        super.setBuildRoot(dir);
        configRoot = new File(dir, "windows");
        configRoot.mkdirs();
    }

    @Override
    boolean validate(BundleParams p) throws UnsupportedPlatformException, ConfigException {
        if (p.type != Bundler.BundleType.ALL && p.type != Bundler.BundleType.IMAGE) {
            return false;
        }
        return doValidate(p);
    }

    //to be used by chained bundlers, e.g. by EXE bundler to avoid
    // skipping validation if p.type does not include "image"
    boolean doValidate(BundleParams p) throws UnsupportedPlatformException, ConfigException {
        if (!System.getProperty("os.name").toLowerCase().startsWith("win")) {
            throw new Bundler.UnsupportedPlatformException();
        }

        if (WinResources.class.getResource(EXECUTABLE_NAME) == null) {
            throw new Bundler.ConfigException(
                    "This copy of ant-javafx.jar does not support Windows.",
                    "Please use ant-javafx.jar coming with Oracle JDK for Windows.");
        }

        if (p.getMainApplicationJar() == null) {
            throw new Bundler.ConfigException(
                    "Main application jar is missing.",
                    "Make sure to use fx:jar task to create main application jar.");
        }

        //validate required inputs
        testRuntime(p, new String[] {"lib\\ext\\jfxrt.jar", "lib\\jfxrt.jar"});

        return true;
    }

    static String getAppName(BundleParams p) {
        String nm;
        if (p.name != null) {
            nm = p.name;
        } else {
            nm = p.getMainClassName();
        }

        return nm;
    }

    //it is static for the sake of sharing with "Exe" bundles
    // that may skip calls to validate/bundle in this class!
    private static File getRootDir(File outDir, BundleParams p) {
        return new File(outDir, getAppName(p));
    }

    public static File getLauncher(File outDir, BundleParams p) {
        return new File(getRootDir(outDir, p), getAppName(p)+".exe");
    }

    private File getConfig_AppIcon() {
        return new File(configRoot, getAppName(params) + ".ico");
    }

    private final static String TEMPLATE_APP_ICON ="javalogo_white_48.ico";

    //remove
    protected void cleanupConfigFiles() {
        if (getConfig_AppIcon() != null) {
            getConfig_AppIcon().delete();
        }
    }

    private void prepareConfigFiles() throws IOException {
        File iconTarget = getConfig_AppIcon();

        if (params.icon != null && params.icon.exists()) {
            fetchResource(WIN_BUNDLER_PREFIX + iconTarget.getName(),
                    "application icon",
                    params.icon,
                    iconTarget);
        } else {
            fetchResource(WIN_BUNDLER_PREFIX + iconTarget.getName(),
                    "application icon",
                    WinAppBundler.TEMPLATE_APP_ICON,
                    iconTarget);
        }
    }

    @Override
    public boolean bundle(BundleParams p, File outputDirectory) {
        return doBundle(p, outputDirectory, false);
    }

    boolean doBundle(BundleParams p, File outputDirectory, boolean dependentTask) {
        try {
            params = p;
            if (!dependentTask) {
               Log.info("Creating app bundle: " + getAppName(p) +" in " +
                    outputDirectory.getAbsolutePath());
            }

            prepareConfigFiles();

            // Create directory structure
            rootDirectory = getRootDir(outputDirectory, p);
            IOUtils.deleteRecursive(rootDirectory);
            rootDirectory.mkdirs();

            File appDirectory = new File(rootDirectory, "app");
            appDirectory.mkdirs();
            copyApplication(appDirectory);

            // Generate PkgInfo
            File pkgInfoFile = new File(appDirectory, "package.cfg");
            pkgInfoFile.createNewFile();
            writePkgInfo(pkgInfoFile);

            // Copy executable root folder
            File executableFile = getLauncher(outputDirectory, p);
            IOUtils.copyFromURL(
                    WinResources.class.getResource(EXECUTABLE_NAME),
                    executableFile);
            executableFile.setExecutable(true, false);

            //Update branding of exe file
            if (getConfig_AppIcon().exists()) {
                //extract helper tool
                File iconSwapTool = File.createTempFile("iconswap", ".exe");
                iconSwapTool.delete();
                IOUtils.copyFromURL(
                        WinResources.class.getResource(TOOL_ICON_SWAP),
                        iconSwapTool);
                iconSwapTool.setExecutable(true, false);
                iconSwapTool.deleteOnExit();

                //run it on launcher file
                executableFile.setWritable(true);
                ProcessBuilder pb = new ProcessBuilder(
                        iconSwapTool.getAbsolutePath(),
                        getConfig_AppIcon().getAbsolutePath(),
                        executableFile.getAbsolutePath());
                IOUtils.exec(pb, verbose);
                executableFile.setReadOnly();
                iconSwapTool.delete();
            }

            // Copy runtime to PlugIns folder
            File runtimeDirectory = new File(rootDirectory, "runtime");
            copyRuntime(runtimeDirectory);

            IOUtils.copyFile(getConfig_AppIcon(),
                    new File(getRootDir(outputDirectory, p), getAppName(p) +".ico"));

            if (!dependentTask) {
                 Log.info("Result application bundle: " +
                    outputDirectory.getAbsolutePath());
            }
        } catch (IOException ex) {
            System.out.println("Exception: "+ex);
            ex.printStackTrace();
            return false;
        } finally {
            if (verbose) {
                Log.info("  Config files are saved to " +
                        configRoot.getAbsolutePath()  +
                        ". Use them to customize package.");
            } else {
                cleanupConfigFiles();
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "Windows Application Bundler";
    }

    private void copyApplication(File appDirectory) throws IOException {
        if (params.appResources == null) {
            throw new RuntimeException("Null app resources?");
        }
        File srcdir = params.appResources.getBaseDirectory();
        for (String fname : params.appResources.getIncludedFiles()) {
            IOUtils.copyFile(
                    new File(srcdir, fname), new File(appDirectory, fname));
        }
    }

    private void writePkgInfo(File pkgInfoFile) throws FileNotFoundException {
        pkgInfoFile.delete();
        PrintStream out = new PrintStream(pkgInfoFile);
        out.println("app.mainjar=" + params.getMainApplicationJar());
        out.println("app.version=" + params.appVersion);
        //for future AU support (to be able to find app in the registry)
        out.println("app.id="+params.identifier);

        if (params.useJavaFXPackaging()) {
            out.println("app.mainclass=" +
                    JAVAFX_LAUNCHER_CLASS.replaceAll("\\.", "/"));
        } else {
            out.println("app.mainclass=" +
                    params.applicationClass.replaceAll("\\.", "/"));
        }
        //This will be emtry string for correctly packaged JavaFX apps
        out.println("app.classpath=" + params.getAppClassPath());

        List<String> jvmargs = params.getAllJvmOptions();
        int idx = 1;
        for (String a : jvmargs) {
            out.println("jvmarg."+idx+"="+a);
            idx++;
        }
        out.close();
    }

    private void copyRuntime(File runtimeDirectory) throws IOException {
        if (params.runtime == null) {
            //its ok, request to use system JRE
            return;
        }
        runtimeDirectory.mkdirs();

        File srcdir = params.runtime.getBaseDirectory();
        File destDir = new File(runtimeDirectory, srcdir.getName());
        Set<String> filesToCopy = params.runtime.getIncludedFiles();
        for (String fname : filesToCopy) {
            IOUtils.copyFile(
                    new File(srcdir, fname), new File(destDir, fname));
        }
    }
}
