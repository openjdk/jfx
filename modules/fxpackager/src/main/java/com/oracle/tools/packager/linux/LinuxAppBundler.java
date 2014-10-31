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

package com.oracle.tools.packager.linux;

import com.oracle.tools.packager.AbstractBundler;
import com.oracle.tools.packager.BundlerParamInfo;
import com.oracle.tools.packager.JreUtils;
import com.oracle.tools.packager.JreUtils.Rule;
import com.oracle.tools.packager.StandardBundlerParam;
import com.oracle.tools.packager.Log;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.IOUtils;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.UnsupportedPlatformException;
import com.sun.javafx.tools.packager.bundlers.BundleParams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;

import static com.oracle.tools.packager.StandardBundlerParam.*;

public class LinuxAppBundler extends AbstractBundler {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle(LinuxAppBundler.class.getName());
    
    protected static final String LINUX_BUNDLER_PREFIX =
            BUNDLER_PREFIX + "linux" + File.separator;
    private static final String EXECUTABLE_NAME = "JavaAppLauncher";
    private static final String LIBRARY_NAME    = "libpackager.so";

    public static final BundlerParamInfo<File> ICON_PNG = new StandardBundlerParam<>(
            I18N.getString("param.icon-png.name"),
            I18N.getString("param.icon-png.description"),
            "icon.png",
            File.class,
            params -> {
                File f = ICON.fetchFrom(params);
                if (f != null && !f.getName().toLowerCase().endsWith(".png")) {
                    Log.info(MessageFormat.format(I18N.getString("message.icon-not-png"), f));
                    return null;
                }
                return f;
            },
            (s, p) -> new File(s));

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
    //   http://www.oracle.com/technetwork/java/javase/jre-8-readme-2095710.html
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
            I18N.getString("param.runtime.name"),
            I18N.getString("param.runtime.description"),
            BundleParams.PARAM_RUNTIME,
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
            if (re.getCause() instanceof ConfigException) {
                throw (ConfigException) re.getCause();
            } else {
                throw new ConfigException(re);
            }
        }
    }

    //used by chained bundlers to reuse validation logic
    boolean doValidate(Map<String, ? super Object> p) throws UnsupportedPlatformException, ConfigException {
        if (!System.getProperty("os.name").toLowerCase().startsWith("linux")) {
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
        testRuntime(LINUX_RUNTIME.fetchFrom(p), new String[] {
                "lib/[^/]+/[^/]+/libjvm.so", // most reliable
                "lib/rt.jar", // fallback canary for JDK 8
        });
        if (USE_FX_PACKAGING.fetchFrom(p)) {
            testRuntime(LINUX_RUNTIME.fetchFrom(p), new String[] {"lib/ext/jfxrt.jar", "lib/jfxrt.jar"});
        }

        return true;
    }

    //it is static for the sake of sharing with "installer" bundlers
    // that may skip calls to validate/bundle in this class!
    public static File getRootDir(File outDir, Map<String, ? super Object> p) {
        return new File(outDir, APP_FS_NAME.fetchFrom(p));
    }

    public static String getLauncherName(Map<String, ? super Object> p) {
        return APP_FS_NAME.fetchFrom(p);
    }

    public static String getLauncherCfgName(Map<String, ? super Object> p) {
        return "app/" + APP_FS_NAME.fetchFrom(p) +".cfg";
    }

    File doBundle(Map<String, ? super Object> p, File outputDirectory, boolean dependentTask) {
        Map<String, ? super Object> originalParams = new HashMap<>(p);
        try {
            if (!outputDirectory.isDirectory() && !outputDirectory.mkdirs()) {
                throw new RuntimeException(MessageFormat.format(I18N.getString("error.cannot-create-output-dir"), outputDirectory.getAbsolutePath()));
            }
            if (!outputDirectory.canWrite()) {
                throw new RuntimeException(MessageFormat.format(I18N.getString("error.cannot-write-to-output-dir"), outputDirectory.getAbsolutePath()));
            }

            // Create directory structure
            File rootDirectory = getRootDir(outputDirectory, p);
            IOUtils.deleteRecursive(rootDirectory);
            rootDirectory.mkdirs();

            if (!dependentTask) {
                Log.info(MessageFormat.format(I18N.getString("message.creating-bundle-location"), rootDirectory.getAbsolutePath()));
            }

            File runtimeDirectory = new File(rootDirectory, "runtime");

            File appDirectory = new File(rootDirectory, "app");
            appDirectory.mkdirs();

            // create the primary launcher
            createLauncherForEntryPoint(p, rootDirectory);

            // Copy library to the launcher folder
            IOUtils.copyFromURL(
                    LinuxResources.class.getResource(LIBRARY_NAME),
                    new File(rootDirectory, LIBRARY_NAME));

            // create the secondary launchers, if any
            List<Map<String, ? super Object>> entryPoints = StandardBundlerParam.SECONDARY_LAUNCHERS.fetchFrom(p);
            for (Map<String, ? super Object> entryPoint : entryPoints) {
                Map<String, ? super Object> tmp = new HashMap<>(originalParams);
                tmp.putAll(entryPoint);
                createLauncherForEntryPoint(tmp, rootDirectory);
            }

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

    private void createLauncherForEntryPoint(Map<String, ? super Object> p, File rootDir) throws IOException {
        // Copy executable to Linux folder
        File executableFile = new File(rootDir, getLauncherName(p));
        IOUtils.copyFromURL(
                RAW_EXECUTABLE_URL.fetchFrom(p),
                executableFile);

        executableFile.setExecutable(true, false);
        executableFile.setWritable(true, true); //for str

        // Generate PkgInfo
        writePkgInfo(p, rootDir);
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

    private void writePkgInfo(Map<String, ? super Object> params, File rootDir) throws FileNotFoundException {
        File pkgInfoFile = new File(rootDir, getLauncherCfgName(params));

        pkgInfoFile.delete();
        PrintStream out = new PrintStream(pkgInfoFile);
        if (LINUX_RUNTIME.fetchFrom(params) == null) {
            out.println("app.runtime=");                    
        } else {
            out.println("app.runtime=$APPDIR/runtime");
        }
        out.println("app.mainjar=" + MAIN_JAR.fetchFrom(params).getIncludedFiles().iterator().next());
        out.println("app.version=" + VERSION.fetchFrom(params));

        //use '/' in the clas name (instead of '.' to simplify native code
        out.println("app.mainclass=" +
                MAIN_CLASS.fetchFrom(params).replaceAll("\\.", "/"));

        StringBuilder macroedPath = new StringBuilder();
        for (String s : CLASSPATH.fetchFrom(params).split("[ ;:]+")) {
            macroedPath.append("$PACKAGEDIR/");
            macroedPath.append(s);
            macroedPath.append(":");
        }
        macroedPath.deleteCharAt(macroedPath.length() - 1);
        out.println("app.classpath=" + macroedPath.toString());

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

        String preloader = PRELOADER_CLASS.fetchFrom(params);
        if (preloader != null) {
            out.println("jvmarg."+idx+"=-Djavafx.preloader="+preloader);
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

        // add command line args
        List<String> args = ARGUMENTS.fetchFrom(params);
        idx = 1;
        for (String a : args) {
            out.println("arg."+idx+"="+a);
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
                ARGUMENTS,
                CLASSPATH,
                JVM_OPTIONS,
                JVM_PROPERTIES,
                LINUX_RUNTIME,
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
