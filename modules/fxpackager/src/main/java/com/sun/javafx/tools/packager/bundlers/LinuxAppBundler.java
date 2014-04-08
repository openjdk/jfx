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

import com.oracle.bundlers.AbstractBundler;
import com.oracle.bundlers.BundlerParamInfo;
import com.oracle.bundlers.JreUtils;
import com.oracle.bundlers.JreUtils.Rule;
import com.oracle.bundlers.StandardBundlerParam;
import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.resource.linux.LinuxResources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;

import static com.oracle.bundlers.StandardBundlerParam.*;

public class LinuxAppBundler extends AbstractBundler {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle("com.oracle.bundlers.linux.LinuxAppBundler");
    
    protected static final String LINUX_BUNDLER_PREFIX =
            BUNDLER_PREFIX + "linux" + File.separator;
    private static final String EXECUTABLE_NAME = "JavaAppLauncher";

    public static final BundlerParamInfo<URL> RAW_EXECUTABLE_URL = new StandardBundlerParam<>(
            I18N.getString("param.raw-executable-url.name"),
            I18N.getString("param.raw-executable-url.description"),
            "linux.launcher.url",
            URL.class,
            params -> LinuxResources.class.getResource(EXECUTABLE_NAME),
            (s, p) -> {
                try {
                    return new URL(s);
                } catch (MalformedURLException e) {
                    Log.info(e.toString());
                    return null;
                }
            });

    //Subsetting of JRE is restricted.
    //JRE README defines what is allowed to strip:
    //   ﻿http://www.oracle.com/technetwork/java/javase/jre-7-readme-430162.html //TODO update when 8 goes GA
    //
    public static final BundlerParamInfo<Rule[]> LINUX_JRE_RULES = new StandardBundlerParam<>(
            "",
            "",
            ".linux.runtime.rules",
            Rule[].class,
            params -> new Rule[]{
                    Rule.prefixNeg("/bin"),
                    Rule.prefixNeg("/plugin"),
                    //Rule.prefixNeg("/lib/ext"), //need some of jars there for https to work
                    Rule.suffix("deploy.jar"), //take deploy.jar
                    Rule.prefixNeg("/lib/deploy"),
                    Rule.prefixNeg("/lib/desktop"),
                    Rule.substrNeg("libnpjp2.so")
            },
            (s, p) ->  null
    );

    public static final BundlerParamInfo<RelativeFileSet> LINUX_RUNTIME = new StandardBundlerParam<>(
            RUNTIME.getName(),
            RUNTIME.getDescription(),
            RUNTIME.getID(),
            RelativeFileSet.class,
            params -> JreUtils.extractJreAsRelativeFileSet(System.getProperty("java.home"),
                    LINUX_JRE_RULES.fetchFrom(params)),
            (s, p) -> JreUtils.extractJreAsRelativeFileSet(s, LINUX_JRE_RULES.fetchFrom(p))
    );

    @Override
    public boolean validate(Map<String, ? super Object> p) throws UnsupportedPlatformException, ConfigException {
        try {
            if (p == null) throw new ConfigException(
                    I18N.getString("error.parameters-null"),
                    I18N.getString("error.parameters-null.advice"));

            return doValidate(p);
        } catch (RuntimeException re) {
            throw new ConfigException(re);
        }
    }

    //used by chained bundlers to reuse validation logic
    boolean doValidate(Map<String, ? super Object> p) throws UnsupportedPlatformException, ConfigException {
        if (!System.getProperty("os.name").toLowerCase().startsWith("linux")) {
            throw new UnsupportedPlatformException();
        }

        if (RAW_EXECUTABLE_URL.fetchFrom(p) == null) {
            throw new ConfigException(
                    I18N.getString("error.no-linux-resources"),
                    I18N.getString("error.no-linux-resources.advice"));
        }

        if (MAIN_JAR.fetchFrom(p) == null) {
            throw new ConfigException(
                    I18N.getString("error.no-application-jar"),
                    I18N.getString("error.no-application-jar.advice"));
        }

        //validate required inputs
        if (USE_FX_PACKAGING.fetchFrom(p)) {
            testRuntime(p, new String[] {"lib/ext/jfxrt.jar", "lib/jfxrt.jar"});
        }
        testRuntime(p, new String[] { "lib/rt.jar" });

        return true;
    }

    //it is static for the sake of sharing with "installer" bundlers
    // that may skip calls to validate/bundle in this class!
    private static File getRootDir(File outDir, Map<String, ? super Object> p) {
        return new File(outDir, APP_NAME.fetchFrom(p));
    }

    public static File getLauncher(File outDir, Map<String, ? super Object> p) {
        return new File(getRootDir(outDir, p), APP_NAME.fetchFrom(p));
    }

    File doBundle(Map<String, ? super Object> p, File outputDirectory, boolean dependentTask) {
        try {

            outputDirectory.mkdirs();

            // Create directory structure
            File rootDirectory = new File(outputDirectory, APP_NAME.fetchFrom(p));
            IOUtils.deleteRecursive(rootDirectory);
            rootDirectory.mkdirs();

            if (!dependentTask) {
                Log.info(MessageFormat.format(I18N.getString("message.creating-bundle-location"), rootDirectory.getAbsolutePath()));
            }

            File runtimeDirectory = new File(rootDirectory, "runtime");

            File appDirectory = new File(rootDirectory, "app");
            appDirectory.mkdirs();

            // Copy executable to Linux folder
            File executableFile = getLauncher(outputDirectory, p);
            IOUtils.copyFromURL(
                    RAW_EXECUTABLE_URL.fetchFrom(p),
                    executableFile);

            executableFile.setExecutable(true, false);
            executableFile.setWritable(true, true); //for str

            // Generate PkgInfo
            File pkgInfoFile = new File(appDirectory, "package.cfg");
            pkgInfoFile.createNewFile();
            writePkgInfo(p, pkgInfoFile);

            // Copy runtime to PlugIns folder
            copyRuntime(p, runtimeDirectory);

            // Copy class path entries to Java folder
            copyApplication(p, appDirectory);

            // Copy icon to Resources folder
//FIXME            copyIcon(resourcesDirectory);

            return rootDirectory;
        } catch (IOException ex) {
            Log.info("Exception: "+ex);
            Log.debug(ex);
            return null;
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    private void copyApplication(Map<String, ? super Object> params, File appDirectory) throws IOException {
        RelativeFileSet appResources = APP_RESOURCES.fetchFrom(params);
        if (appResources == null) {
            throw new RuntimeException("Null app resources?");
        }
        File srcdir = appResources.getBaseDirectory();
        for (String fname : appResources.getIncludedFiles()) {
            IOUtils.copyFile(
                    new File(srcdir, fname), new File(appDirectory, fname));
        }
    }

    private void writePkgInfo(Map<String, ? super Object> params, File pkgInfoFile) throws FileNotFoundException {
        pkgInfoFile.delete();
        PrintStream out = new PrintStream(pkgInfoFile);
        out.println("app.mainjar=" + MAIN_JAR.fetchFrom(params).getIncludedFiles().iterator().next());
        out.println("app.version=" + VERSION.fetchFrom(params));

        //use '/' in the clas name (instead of '.' to simplify native code
        if (USE_FX_PACKAGING.fetchFrom(params)) {
            out.println("app.mainclass=" +
                    JAVAFX_LAUNCHER_CLASS.replaceAll("\\.", "/"));
        } else {
            out.println("app.mainclass=" +
                    MAIN_CLASS.fetchFrom(params).replaceAll("\\.", "/"));
        }
        //This will be emtry string for correctly packaged JavaFX apps
        out.println("app.classpath=" + MAIN_JAR_CLASSPATH.fetchFrom(params));

        List<String> jvmargs = JVM_OPTIONS.fetchFrom(params);
        int idx = 1;
        for (String a : jvmargs) {
            out.println("jvmarg."+idx+"="+a);
            idx++;
        }

        //app.id required for setting user preferences (Java Preferences API)
        out.println("app.preferences.id=" + PREFERENCES_ID.fetchFrom(params));

        Map<String, String> overridableJVMOptions = USER_JVM_OPTIONS.fetchFrom(params);
        idx = 1;
        for (Map.Entry<String, String> arg: overridableJVMOptions.entrySet()) {
            if (arg.getKey() == null || arg.getValue() == null) {
                Log.info(I18N.getString("message.jvm-user-arg-is-null"));
            }
            else {
                out.println("jvmuserarg."+idx+".name="+arg.getKey());
                out.println("jvmuserarg."+idx+".value="+arg.getValue());
            }
            idx++;
        }
        out.close();
    }

    private void copyRuntime(Map<String, ? super Object> params, File runtimeDirectory) throws IOException {
        RelativeFileSet runtime = LINUX_RUNTIME.fetchFrom(params);
        if (runtime == null) {
            //request to use system runtime
            return;
        }
        runtimeDirectory.mkdirs();

        File srcdir = runtime.getBaseDirectory();
        File destDir = new File(runtimeDirectory, srcdir.getName());
        Set<String> filesToCopy = runtime.getIncludedFiles();
        for (String fname : filesToCopy) {
            IOUtils.copyFile(
                    new File(srcdir, fname), new File(destDir, fname));
        }
    }

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
        return "linux.app";
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
                LINUX_RUNTIME,
                USE_FX_PACKAGING,
                USER_JVM_OPTIONS,
                VERSION
        );
    }

    @Override
    public File execute(Map<String, ? super Object> params, File outputParentDir) {
        return doBundle(params, outputParentDir, false);
    }
}
