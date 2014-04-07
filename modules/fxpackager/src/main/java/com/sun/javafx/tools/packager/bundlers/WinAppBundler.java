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
import com.oracle.bundlers.StandardBundlerParam;
import com.oracle.bundlers.windows.WindowsBundlerParam;
import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.resource.windows.WinResources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;

import static com.oracle.bundlers.JreUtils.*;
import static com.oracle.bundlers.StandardBundlerParam.*;
import static com.oracle.bundlers.windows.WindowsBundlerParam.BIT_ARCH_64;
import static com.oracle.bundlers.windows.WindowsBundlerParam.BIT_ARCH_64_RUNTIME;

public class WinAppBundler extends AbstractBundler {

    private static final ResourceBundle I18N = 
            ResourceBundle.getBundle("com.oracle.bundlers.windows.WinAppBundler");

    public static final BundlerParamInfo<File> CONFIG_ROOT = new WindowsBundlerParam<>(
            I18N.getString("param.config-root.name"),
            I18N.getString("param.config-root.description"), 
            "configRoot",
            File.class, null, params -> {
                File imagesRoot = new File(BUILD_ROOT.fetchFrom(params), "windows");
                imagesRoot.mkdirs();
                return imagesRoot;
            }, false, (s, p) -> null);

    //Subsetting of JRE is restricted.
    //JRE README defines what is allowed to strip:
    //   ﻿http://www.oracle.com/technetwork/java/javase/jre-7-readme-430162.html //TODO update when 8 goes GA
    public static final BundlerParamInfo<Rule[]> WIN_JRE_RULES = new StandardBundlerParam<>(
            "",
            "",
            ".win.runtime.rules",
            Rule[].class,
            null,
            params -> new Rule[]{
                Rule.prefixNeg("\\bin\\new_plugin"),
                Rule.prefixNeg("\\lib\\deploy"),
                Rule.suffixNeg(".pdb"),
                Rule.suffixNeg(".map"),
                Rule.suffixNeg("axbridge.dll"),
                Rule.suffixNeg("eula.dll"),
                Rule.substrNeg("javacpl"),
                Rule.suffixNeg("wsdetect.dll"),
                Rule.substrNeg("eployjava1.dll"), //NP and IE versions
                Rule.substrNeg("bin\\jp2"),
                Rule.substrNeg("bin\\jpi"),
                //Rule.suffixNeg("lib\\ext"), //need some of jars there for https to work
                Rule.suffixNeg("ssv.dll"),
                Rule.substrNeg("npjpi"),
                Rule.substrNeg("npoji"),
                Rule.suffixNeg(".exe"),
                //keep core deploy files as JavaFX APIs use them
                //Rule.suffixNeg("deploy.dll"),
                Rule.suffixNeg("deploy.jar"),
                //Rule.suffixNeg("javaws.jar"),
                //Rule.suffixNeg("plugin.jar"),
                Rule.suffix(".jar")
            },
            false,
            (s, p) -> null
    );

    public static final BundlerParamInfo<RelativeFileSet> WIN_RUNTIME = new StandardBundlerParam<>(
            RUNTIME.getName(),
            RUNTIME.getDescription(),
            RUNTIME.getID(),
            RelativeFileSet.class,
            null,
            params -> extractJreAsRelativeFileSet(System.getProperty("java.home"),
                    WIN_JRE_RULES.fetchFrom(params)),
            false,
            (s, p) -> extractJreAsRelativeFileSet(s,
                    WIN_JRE_RULES.fetchFrom(p))
    );

    private final static String EXECUTABLE_NAME = "WinLauncher.exe";

    private static final String TOOL_ICON_SWAP="IconSwap.exe";

    public static final BundlerParamInfo<URL> RAW_EXECUTABLE_URL = new WindowsBundlerParam<>(
            I18N.getString("param.raw-executable-url.name"),
            I18N.getString("param.raw-executable-url.description"),
            "win.launcher.url",
            URL.class, null, params -> WinResources.class.getResource(EXECUTABLE_NAME), 
            false, (s, p) -> {
                try {
                    return new URL(s);
                } catch (MalformedURLException e) {
                    Log.info(e.toString());
                    return null;
                }
            });

    public static final BundlerParamInfo<Boolean> REBRAND_EXECUTABLE = new WindowsBundlerParam<>(
            I18N.getString("param.rebrand-executable.name"),
            I18N.getString("param.rebrand-executable.description"),
            "win.launcher.rebrand",
            Boolean.class, null, params -> Boolean.TRUE, 
            false, (s, p) -> Boolean.valueOf(s));

    public WinAppBundler() {
        super();
        baseResourceLoader = WinResources.class;
    }

    public final static String WIN_BUNDLER_PREFIX =
            BUNDLER_PREFIX + "windows/";

    File getConfigRoot(Map<String, ? super Object> params) {
        return CONFIG_ROOT.fetchFrom(params);
    }

    @Override
    public boolean validate(Map<String, ? super Object> params) throws UnsupportedPlatformException, ConfigException {
        try {
            if (params == null) throw new ConfigException(
                    I18N.getString("error.parameters-null"),
                    I18N.getString("error.parameters-null.advice"));

            return doValidate(params);
        } catch (RuntimeException re) {
            throw new ConfigException(re);
        }
    }

    //to be used by chained bundlers, e.g. by EXE bundler to avoid
    // skipping validation if p.type does not include "image"
    boolean doValidate(Map<String, ? super Object> p) throws UnsupportedPlatformException, ConfigException {
        if (!System.getProperty("os.name").toLowerCase().startsWith("win")) {
            throw new UnsupportedPlatformException();
        }

        if (WinResources.class.getResource(TOOL_ICON_SWAP) == null) {
            throw new ConfigException(
                    I18N.getString("error.no-windows-resources"),
                    I18N.getString("error.no-windows-resources.advice"));
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

        //validate runtime bit-architectire
        testRuntimeBitArchitecture(p);

        return true;
    }

    private static void testRuntimeBitArchitecture(Map<String, ? super Object> params) throws ConfigException {
        if ("true".equalsIgnoreCase(System.getProperty("fxpackager.disableBitArchitectureMismatchCheck"))) {
            Log.debug(I18N.getString("message.disable-bit-architecture-check"));
            return;
        }

        if (BIT_ARCH_64.fetchFrom(params) != BIT_ARCH_64_RUNTIME.fetchFrom(params)) {
            throw new ConfigException(
                    I18N.getString("error.bit-architecture-mismatch"),
                    I18N.getString("error.bit-architecture-mismatch.advice"));
        }
    }

    static String getAppName(Map<String, ? super Object>  p) {
        return APP_NAME.fetchFrom(p);
    }

    //it is static for the sake of sharing with "Exe" bundles
    // that may skip calls to validate/bundle in this class!
    private static File getRootDir(File outDir, Map<String, ? super Object> p) {
        return new File(outDir, getAppName(p));
    }

    public static File getLauncher(File outDir, Map<String, ? super Object> p) {
        return new File(getRootDir(outDir, p), getAppName(p)+".exe");
    }

    private File getConfig_AppIcon(Map<String, ? super Object> params) {
        return new File(getConfigRoot(params), getAppName(params) + ".ico");
    }

    private final static String TEMPLATE_APP_ICON ="javalogo_white_48.ico";

    //remove
    protected void cleanupConfigFiles(Map<String, ? super Object> params) {
        if (getConfig_AppIcon(params) != null) {
            getConfig_AppIcon(params).delete();
        }
    }

    private void prepareConfigFiles(Map<String, ? super Object> params) throws IOException {
        File iconTarget = getConfig_AppIcon(params);

        File icon = ICON.fetchFrom(params);
        if (icon != null && icon.exists()) {
            fetchResource(WIN_BUNDLER_PREFIX + iconTarget.getName(),
                    I18N.getString("resource.application-icon"),
                    icon,
                    iconTarget,
                    VERBOSE.fetchFrom(params));
        } else {
            fetchResource(WIN_BUNDLER_PREFIX + iconTarget.getName(),
                    I18N.getString("resource.application-icon"),
                    WinAppBundler.TEMPLATE_APP_ICON,
                    iconTarget,
                    VERBOSE.fetchFrom(params));
        }
    }

    public boolean bundle(Map<String, ? super Object> p, File outputDirectory) {
        return doBundle(p, outputDirectory, false) != null;
    }

    File doBundle(Map<String, ? super Object> p, File outputDirectory, boolean dependentTask) {
        try {
            outputDirectory.mkdirs();

            if (!dependentTask) {
                Log.info(MessageFormat.format(I18N.getString("message.creating-app-bundle"), getAppName(p), outputDirectory.getAbsolutePath()));
            }

            prepareConfigFiles(p);

            // Create directory structure
            File rootDirectory = getRootDir(outputDirectory, p);
            IOUtils.deleteRecursive(rootDirectory);
            rootDirectory.mkdirs();

            File appDirectory = new File(rootDirectory, "app");
            appDirectory.mkdirs();
            copyApplication(p, appDirectory);

            // Generate PkgInfo
            File pkgInfoFile = new File(appDirectory, "package.cfg");
            pkgInfoFile.createNewFile();
            writePkgInfo(p, pkgInfoFile);

            // Copy executable root folder
            File executableFile = getLauncher(outputDirectory, p);
            IOUtils.copyFromURL(
                    RAW_EXECUTABLE_URL.fetchFrom(p),
                    executableFile);
            executableFile.setExecutable(true, false);

            //Update branding of exe file
            if (REBRAND_EXECUTABLE.fetchFrom(p) && getConfig_AppIcon(p).exists()) {
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
                        getConfig_AppIcon(p).getAbsolutePath(),
                        executableFile.getAbsolutePath());
                IOUtils.exec(pb, VERBOSE.fetchFrom(p));
                executableFile.setReadOnly();
                iconSwapTool.delete();
            }

            // Copy runtime to PlugIns folder
            File runtimeDirectory = new File(rootDirectory, "runtime");
            copyRuntime(p, runtimeDirectory);

            IOUtils.copyFile(getConfig_AppIcon(p),
                    new File(getRootDir(outputDirectory, p), getAppName(p) + ".ico"));

            if (!dependentTask) {
                Log.info(MessageFormat.format(I18N.getString("message.result-dir"), outputDirectory.getAbsolutePath()));
            }

            return rootDirectory;
        } catch (IOException ex) {
            Log.info("Exception: "+ex);
            Log.debug(ex);
            return null;
        } finally {
            if (VERBOSE.fetchFrom(p)) {
                Log.info(MessageFormat.format(I18N.getString("message.config-save-location"), getConfigRoot(p).getAbsolutePath()));
            } else {
                cleanupConfigFiles(p);
            }
        }

    }

    @Override
    public String toString() {
        return "Windows Application Bundler";
    }

    private void copyApplication(Map<String, ? super Object> params, File appDirectory) throws IOException {
        RelativeFileSet appResource = APP_RESOURCES.fetchFrom(params);
        if (appResource == null) {
            throw new RuntimeException("Null app resources?");
        }
        File srcdir = appResource.getBaseDirectory();
        for (String fname : appResource.getIncludedFiles()) {
            IOUtils.copyFile(
                    new File(srcdir, fname), new File(appDirectory, fname));
        }
    }

    private void writePkgInfo(Map<String, ? super Object> params, File pkgInfoFile) throws FileNotFoundException {
        pkgInfoFile.delete();

        PrintStream out = new PrintStream(pkgInfoFile);
        out.println("app.mainjar=" + MAIN_JAR.fetchFrom(params).getIncludedFiles().iterator().next());
        out.println("app.version=" + VERSION.fetchFrom(params));
        //for future AU support (to be able to find app in the registry)
        out.println("app.id=" + IDENTIFIER.fetchFrom(params));
        out.println("app.preferences.id=" + PREFERENCES_ID.fetchFrom(params));

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
        Map<String, String> jvmProps = JVM_PROPERTIES.fetchFrom(params);
        for (Map.Entry<String, String> entry : jvmProps.entrySet()) {
            out.println("jvmarg."+idx+"=-D"+entry.getKey()+"="+entry.getValue());
            idx++;
        }


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
        RelativeFileSet runtime = WIN_RUNTIME.fetchFrom(params);
        if (runtime == null) {
            //its ok, request to use system JRE
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
        return "windows.app";
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
                CONFIG_ROOT,
                ICON,
                IDENTIFIER,
                JVM_OPTIONS,
                JVM_PROPERTIES,
                MAIN_CLASS,
                MAIN_JAR,
                MAIN_JAR_CLASSPATH,
                PREFERENCES_ID,
                RAW_EXECUTABLE_URL,
                WIN_RUNTIME,
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
