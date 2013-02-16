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
import com.sun.javafx.tools.packager.bundlers.Bundler.ConfigException;
import com.sun.javafx.tools.packager.bundlers.Bundler.UnsupportedPlatformException;
import com.sun.javafx.tools.resource.linux.LinuxResources;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

public class LinuxAppBundler extends Bundler {
    private BundleParams params = null;

    protected static final String LINUX_BUNDLER_PREFIX =
             BUNDLER_PREFIX + "linux" + File.separator;
    private static final String EXECUTABLE_NAME = "JavaAppLauncher";
    private static final String LAUNCHER_CLASS = "com.javafx.main.Main";

    @Override
    boolean validate(BundleParams p) throws UnsupportedPlatformException, ConfigException {
        if (p.type != Bundler.BundleType.ALL && p.type != Bundler.BundleType.IMAGE) {
            return false;
        }
        return doValidate(p);
    }

    //used by chained bundlers to reuse validation logic
    boolean doValidate(BundleParams p) throws UnsupportedPlatformException, ConfigException {
        if (!System.getProperty("os.name").toLowerCase().startsWith("linux")) {
            throw new Bundler.UnsupportedPlatformException();
        }

        if (LinuxResources.class.getResource(EXECUTABLE_NAME) == null) {
            throw new Bundler.ConfigException(
                    "This copy of ant-javafx.jar does not support Linux.",
                    "Please use ant-javafx.jar coming with Oracle JDK for Linux.");
        }

        if (p.getMainApplicationJar() == null) {
            throw new Bundler.ConfigException(
                    "Main application jar is missing.",
                    "Make sure to use fx:jar task to create main application jar.");
        }

        //validate required inputs
        testRuntime(p, new String[] {"lib/ext/jfxrt.jar", "lib/jfxrt.jar"});
        testRuntime(p, new String[] { "lib/rt.jar" });

        return true;
    }

    //it is static for the sake of sharing with "installer" bundlers
    // that may skip calls to validate/bundle in this class!
    private static File getRootDir(File outDir, BundleParams p) {
        return new File(outDir, getLauncherName(p));
    }

    private static String getLauncherName(BundleParams p) {
        String nm;
        if (p.name != null) {
            nm = p.name;
        } else {
            nm = p.getMainClassName();
        }
        nm = nm.replaceAll(" ", "");

        return nm;
    }

    public static File getLauncher(File outDir, BundleParams p) {
        return new File(getRootDir(outDir, p), getLauncherName(p));
    }

    @Override
    public boolean bundle(BundleParams p, File outputDirectory) {
        return doBundle(p, outputDirectory, false);
    }

    boolean doBundle(BundleParams p, File outputDirectory, boolean dependentTask) {
        try {
            params = p;

            // Create directory structure
            File rootDirectory = new File(outputDirectory, getLauncherName(p));
            IOUtils.deleteRecursive(rootDirectory);
            rootDirectory.mkdirs();

            if (!dependentTask) {
                Log.info("Creating app bundle: " + rootDirectory.getAbsolutePath());
            }

            File runtimeDirectory = new File(rootDirectory, "runtime");

            File appDirectory = new File(rootDirectory, "app");
            appDirectory.mkdirs();

            // Copy executable to MacOS folder
            File executableFile = getLauncher(outputDirectory, p);
            IOUtils.copyFromURL(
                    LinuxResources.class.getResource(EXECUTABLE_NAME),
                    executableFile);

            executableFile.setExecutable(true, false);
            executableFile.setWritable(true, true); //for str

            // Generate PkgInfo
            File pkgInfoFile = new File(appDirectory, "package.cfg");
            pkgInfoFile.createNewFile();
            writePkgInfo(pkgInfoFile);

            // Copy runtime to PlugIns folder
            copyRuntime(runtimeDirectory);

            // Copy class path entries to Java folder
            copyApplication(appDirectory);

            // Copy icon to Resources folder
//            copyIcon(resourcesDirectory);

        } catch (IOException ex) {
            System.out.println("Exception: "+ex);
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Linux Application Bundler";
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

        //use '/' in the clas name (instead of '.' to simplify native code
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
            //request to use system runtime
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
