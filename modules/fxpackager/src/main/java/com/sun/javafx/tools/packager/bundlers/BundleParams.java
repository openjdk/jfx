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

import com.oracle.bundlers.BundlerParamInfo;
import com.oracle.bundlers.StandardBundlerParam;
import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.packager.PackagerLib;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class BundleParams {
      
    final protected Map<String, ? super Object> params;
    
    public static final String PARAM_RUNTIME                = "runtime"; // RelativeFileSet //KEY
    public static final String PARAM_APP_RESOURCES          = "appResources"; // RelativeFileSet //KEY
    public static final String PARAM_TYPE                   = "type"; // BundlerType
    public static final String PARAM_BUNDLE_FORMAT          = "bundleFormat"; // String
    public static final String PARAM_ICON                   = "icon"; // String //KEY

    /* Name of bundle file and native launcher.
       Also used as CFBundleName on Mac */
    public static final String PARAM_APP_NAME               = "appName"; // String //KEY
    public static final String PARAM_NAME                   = "name"; // String //KEY

    /* application vendor, used by most of the bundlers */
    public static final String PARAM_VENDOR                 = "vendor"; // String //KEY

    /* email name and email, only used for debian */
    public static final String PARAM_EMAIL                  = "email"; // String  //KEY

    /* Copyright. Used on Mac */
    public static final String PARAM_COPYRIGHT              = "copyright"; // String //KEY

    /* GUID on windows for MSI, CFBundleIdentifier on Mac
       If not compatible with requirements then bundler either do not bundle
       or autogenerate */
    public static final String PARAM_IDENTIFIER             = "identifier"; // String  //KEY

    /* shortcut preferences */
    public static final String PARAM_SHORTCUT               = "shortcutHint"; // boolean //KEY
    public static final String PARAM_MENU                   = "menuHint"; // boolean //KEY

    /* Application version. Format may differ for different bundlers */
    public static final String PARAM_VERSION                = "appVersion"; // String //KEY
    /* Application category. Used at least on Mac/Linux. Value is platform specific */
    public static final String PARAM_CATEGORY               = "applicationCategory"; // String //KEY

    /* Optional short application */
    public static final String PARAM_TITLE                  = "title"; // String //KEY

    /* Optional application description. Used by MSI and on Linux */
    public static final String PARAM_DESCRIPTION            = "description"; // String //KEY

    /* License type. Needed on Linux (rpm) */
    public static final String PARAM_LICENSE_TYPE           = "licenseType"; // String //KEY

    /* File(s) with license. Format is OS/bundler specific */
    public static final String PARAM_LICENSE_FILES          = "licenseFiles"; // List<String> //KEY

    /* user or system level install.
       null means "default" */
    public static final String PARAM_SYSTEM_WIDE            = "systemWide"; // Boolean //KEY

    /* Main application class. Not used directly but used to derive default values */
    public static final String PARAM_APPLICATION_CLASS      = "applicationClass"; // String //KEY

    //list of jvm args (in theory string can contain spaces and need to be escaped
    private List<String> jvmargs = new LinkedList<>();
    //list of jvm args (in theory string can contain spaces and need to be escaped
    private Map<String, String> jvmUserArgs = new HashMap<>();

    //list of jvm properties (can also be passed as VM args
    private Map<String, String> jvmProperties = new HashMap<>();

    /**
     * create a new bundle with all default values
     */
    public BundleParams() {
        params = new HashMap<>();
    }

    /**
     * Create a bundle params with a copy of the params 
     * @param params map of initial parameters to be copied in.
     */
    public BundleParams(Map<String, ?> params) {
        this.params = new HashMap<>(params);
    }

    public void addAllBundleParams(Map<String, ? super Object> p) {
        params.putAll(p);
    }

    public <C> C fetchParam(BundlerParamInfo<C> paramInfo) {
        return paramInfo.fetchFrom(params);
    }
    
    @SuppressWarnings("unchecked")
    public <C> C fetchParamWithDefault(Class<C> klass, C defaultValue, String... keys) {
        for (String key : keys) {
            Object o = params.get(key);
            if (klass.isInstance(o)) {
                return (C) o;
            } else if (params.containsKey(keys) && o == null) {
                return null;
                // } else if (o != null) {
                // TODO log an error.
            }
        }
        return defaultValue;
    }

    public <C> C fetchParam(Class<C> klass, String... keys) {
        return fetchParamWithDefault(klass, null, keys);
    }

    //NOTE: we do not care about application parameters here
    // as they will be embeded into jar file manifest and
    // java launcher will take care of them!
    
    public Map<String, ? super Object> getBundleParamsAsMap() {
        return new HashMap<String, Object>(params);
    }

    public void setJvmargs(List<String> jvmargs) {
        this.jvmargs = jvmargs;
    }

    public void setJvmUserArgs(Map<String, String> userArgs) {
        this.jvmUserArgs = userArgs;
    }

    public void setJvmProperties(Map<String, String> jvmProperties) {
        this.jvmProperties = jvmProperties;
    }

    public List<String> getAllJvmOptions() {
        List<String> all = new LinkedList<>();
        all.addAll(jvmargs);
        for(String k: jvmProperties.keySet()) {
            all.add("-D"+k+"="+jvmProperties.get(k)); //TODO: We are not escaping values here...
        }
        return all;
    }

    public Map<String, String> getAllJvmUserOptions() {
        Map<String, String> all = new HashMap<>();
        all.putAll(jvmUserArgs);
        return all;
    }

    public String getApplicationID() {
        return fetchParam(StandardBundlerParam.IDENTIFIER);        
    }

    public String getPreferencesID() {
        return fetchParam(StandardBundlerParam.PREFERENCES_ID);
    }

    public String getTitle() {
        return fetchParam(StandardBundlerParam.TITLE); //FIXME title
    }

    public void setTitle(String title) {
        putUnlessNull(PARAM_TITLE, title);
    }

    public String getApplicationClass() {
        return fetchParam(StandardBundlerParam.MAIN_CLASS);
    }

    public void setApplicationClass(String applicationClass) {
        putUnlessNull(PARAM_APPLICATION_CLASS, applicationClass);
    }

    public String getAppVersion() {
        return fetchParam(StandardBundlerParam.VERSION);
    }

    public void setAppVersion(String version) {
        putUnlessNull(PARAM_VERSION, version);
    }

    public String getDescription() {
        return fetchParam(StandardBundlerParam.NAME); //FIXME DESCRIPTION);
    }

    public void setDescription(String s) {
        putUnlessNull(PARAM_DESCRIPTION, s);
    }

    public String getLicenseType() {
        return fetchParam(StandardBundlerParam.LICENSE_TYPE); //FIXME
    }

    public void setLicenseType(String version) {
        putUnlessNull(PARAM_LICENSE_TYPE, version);
    }

    //path is relative to the application root
    public void addLicenseFile(String path) {
        @SuppressWarnings("unchecked") 
        List<String> licenseFile = fetchParam(List.class, PARAM_LICENSE_FILES);
        if (licenseFile == null) {
            licenseFile = new ArrayList<>();
            params.put(PARAM_LICENSE_FILES, licenseFile);
        }
        licenseFile.add(path);
    }

    public Boolean getSystemWide() {
        return fetchParam(StandardBundlerParam.SYSTEM_WIDE);
    }

    public void setSystemWide(Boolean b) {
        putUnlessNull(PARAM_SYSTEM_WIDE, b);
    }

    public RelativeFileSet getRuntime() {
        return fetchParam(StandardBundlerParam.RUNTIME);
    }

    public boolean isShortcutHint() {
        return fetchParam(StandardBundlerParam.SHORTCUT_HINT);
    }

    public void setShortcutHint(boolean v) {
        putUnlessNull(PARAM_SHORTCUT, v);
    }

    public boolean isMenuHint() {
        return fetchParam(StandardBundlerParam.MENU_HINT);
    }

    public void setMenuHint(boolean v) {
        putUnlessNull(PARAM_MENU, v);
    }

    public String getName() {
        return fetchParam(StandardBundlerParam.NAME);
    }

    public void setName(String name) {
        putUnlessNull(PARAM_NAME, name);
    }

    public BundleType getType() {
        return fetchParam(BundleType.class, PARAM_TYPE); 
    }

    public void setType(BundleType type) {
        putUnlessNull(PARAM_TYPE, type);
    }

    public String getBundleFormat() {
        return fetchParam(String.class, PARAM_BUNDLE_FORMAT); 
    }
    
    public void setBundleFormat(String t) {
        putUnlessNull(PARAM_BUNDLE_FORMAT, t);
    }

    public static boolean shouldExclude(File baseDir, File f, Rule ruleset[]) {
        if (ruleset == null) {
            return false;
        }

        String fname = f.getAbsolutePath().toLowerCase().substring(
                baseDir.getAbsolutePath().length());
        //first rule match defines the answer
        for (Rule r: ruleset) {
            if (r.match(fname)) {
                return !r.treatAsAccept();
            }
        }
        //default is include
        return false;
    }

    public static void walk(File base, File root, Rule ruleset[], Set<File> files) {
        if (!root.isDirectory()) {
            if (root.isFile()) {
                files.add(root);
            }
            return;
        }

        File[] lst = root.listFiles();
        if (lst != null) {
            for (File f : lst) {
                //ignore symbolic links!
                if (IOUtils.isNotSymbolicLink(f) && !shouldExclude(base, f, ruleset)) {
                    if (f.isDirectory()) {
                        walk(base, f, ruleset, files);
                    } else if (f.isFile()) {
                        //add to list
                        files.add(f);
                    }
                }
            }
        }
    }

    
    @SuppressWarnings("unchecked")
    public List<String> getLicenseFile() {
        return fetchParam(List.class, PARAM_LICENSE_FILES);
    }

    public List<String> getJvmargs() {
        return jvmargs;
    }

    public static class Rule {
        String regex;
        boolean includeRule;
        Type type;
        enum Type {SUFFIX, PREFIX, SUBSTR, REGEX}

        private Rule(String regex, boolean includeRule, Type type) {
            this.regex = regex;
            this.type = type;
            this.includeRule = includeRule;
        }

        boolean match(String str) {
            if (type == Type.SUFFIX) {
                return str.endsWith(regex);
            }
            if (type == Type.PREFIX) {
                return str.startsWith(regex);
            }
            if (type == Type.SUBSTR) {
                return str.contains(regex);
            }
            return str.matches(regex);
        }

        boolean treatAsAccept() {return includeRule;}

        static Rule suffix(String s) {
            return new Rule(s, true, Type.SUFFIX);
        }
        static Rule suffixNeg(String s) {
            return new Rule(s, false, Type.SUFFIX);
        }
        static Rule prefix(String s) {
            return new Rule(s, true, Type.PREFIX);
        }
        static Rule prefixNeg(String s) {
            return new Rule(s, false, Type.PREFIX);
        }
        static Rule substr(String s) {
            return new Rule(s, true, Type.SUBSTR);
        }
        static Rule substrNeg(String s) {
            return new Rule(s, false, Type.SUBSTR);
        }
    }

    //Subsetting of JRE is restricted.
    //JRE README defines what is allowed to strip:
    //   ﻿http://www.oracle.com/technetwork/java/javase/jre-7-readme-430162.html
    //
    //In addition to this we may need to keep deploy jars and deploy.dll
    // because JavaFX apps might need JNLP services
    //Also, embedded launcher uses deploy.jar to access registry
    // (although this is not needed)
    public static Rule macRules[] = {
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
//        Rule.suffixNeg("jre/lib/ext"), //need some of jars there for https to work
        Rule.suffixNeg("jre/lib/nibs"),
//keep core deploy APIs but strip plugin dll
//        Rule.suffixNeg("jre/lib/deploy"),
//        Rule.suffixNeg("jre/lib/deploy.jar"),
//        Rule.suffixNeg("jre/lib/javaws.jar"),
//        Rule.suffixNeg("jre/lib/libdeploy.dylib"),
//        Rule.suffixNeg("jre/lib/plugin.jar"),
        Rule.suffixNeg("jre/lib/libnpjp2.dylib"),
        Rule.suffixNeg("jre/lib/security/javaws.policy")
    };

    public static Rule[] winRules = {
        Rule.prefixNeg("\\bin\\new_plugin"),
        Rule.suffix("deploy.jar"), //take deploy.jar
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
//        Rule.suffixNeg("lib\\ext"), //need some of jars there for https to work
        Rule.suffixNeg("ssv.dll"),
        Rule.substrNeg("npjpi"),
        Rule.substrNeg("npoji"),
        Rule.suffixNeg(".exe"),
//keep core deploy files as JavaFX APIs use them
//        Rule.suffixNeg("deploy.dll"),
//        Rule.suffixNeg("deploy.jar"),
//        Rule.suffixNeg("javaws.jar"),
//        Rule.suffixNeg("plugin.jar"),
        Rule.suffix(".jar")
    };

    public static Rule[] linuxRules = {
        Rule.prefixNeg("/bin"),
        Rule.prefixNeg("/plugin"),
//        Rule.prefixNeg("/lib/ext"), //need some of jars there for https to work
        Rule.suffix("deploy.jar"), //take deploy.jar
        Rule.prefixNeg("/lib/deploy"),
        Rule.prefixNeg("/lib/desktop"),
        Rule.substrNeg("libnpjp2.so")
    };

    //Validation approach:
    //  - JRE marker (rt.jar)
    //  - FX marker (jfxrt.jar)
    //  - JDK marker (tools.jar)
    private static boolean checkJDKRoot(File jdkRoot) {
        File rtJar = new File(jdkRoot, "jre/lib/rt.jar");
        if (!rtJar.exists()) {
            Log.verbose("rt.jar is not found at " + rtJar.getAbsolutePath());
            return false;
        }

        File jfxJar = new File(jdkRoot, "jre/lib/ext/jfxrt.jar");
        if (!jfxJar.exists()) {
            //Try again with new location
            jfxJar = new File(jdkRoot, "jre/lib/jfxrt.jar");
            if (!jfxJar.exists()) {
                Log.verbose("jfxrt.jar is not found at " + jfxJar.getAbsolutePath());
                return false;
            }
        }


        File toolsJar = new File(jdkRoot, "lib/tools.jar");
        if (!toolsJar.exists()) {
            Log.verbose("tools.jar is not found at " + toolsJar.getAbsolutePath());
            return false;
        }

        return true;
    }

    //Depending on platform and user input we may get different "references"
    //Should support
    //   - java.home
    //   - reference to JDK install folder
    //   - should NOT support JRE dir
    //Note: input could be null (then we asked to use system JRE)
    //       or it must be valid directory
    //Returns null on validation failure. Returns jre root if ok.
    static File validateRuntimeLocation(File javaHome) {
        if (javaHome == null) {
            return null;
        }
        File jdkRoot;

        boolean isMac = System.getProperty("os.name").toLowerCase().contains("os x");

        File rtJar = new File(javaHome, "lib/rt.jar");
        if (rtJar.exists()) { //must be "java.home" case
                              //i.e. we are in JRE folder
            jdkRoot = javaHome.getParentFile();
        } else { //expect it to be root of JDK installation folder
            //On Mac it could be jdk/ or jdk/Contents/Home
            //Norm to jdk/Contents/Home for validation
            if (isMac) {
                File f = new File(javaHome, "Contents/Home");
                if (f.exists() && f.isDirectory()) {
                    javaHome = f;
                }
            }
            jdkRoot = javaHome;
        }

        if (!checkJDKRoot(jdkRoot)) {
            throw new RuntimeException(
                    "Can not find JDK artifacts in specified location: "
                    + javaHome.getAbsolutePath());
        }

        return new File(jdkRoot, "jre");
    }

    //select subset of given runtime using predefined rules
    public void setRuntime(File baseDir) {
        baseDir = validateRuntimeLocation(baseDir);

        //mistake or explicit intent to use system runtime
        if (baseDir == null) {
            Log.verbose("No Java runtime to embed. Package will need system Java.");
            params.put(PARAM_RUNTIME, null);
            return;
        }
        doSetRuntime(baseDir);
    }

    public void setDefaultRuntime() {
        File f = new File(System.getProperty("java.home"));
        doSetRuntime(f);
    }

    //input dir "jdk/jre" (i.e. jre folder in the jdk)
    private void doSetRuntime(File baseDir) {
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("os x");

        //Normalization: on MacOS we need to point to the top of JDK dir
        // (other platforms are fine)
        if (isMac) {
            //On Mac we need Bundle root, not jdk/Contents/Home
            baseDir = baseDir.getParentFile().getParentFile().getParentFile();
        }

        Set<File> lst = new HashSet<>();

        Rule ruleset[];
        if (System.getProperty("os.name").startsWith("Mac")) {
            ruleset = macRules;
        } else if (System.getProperty("os.name").startsWith("Win")) {
            ruleset = winRules;
        } else {
            //must be linux
            ruleset = linuxRules;
        }

        walk(baseDir, baseDir, ruleset, lst);

        params.put(PARAM_RUNTIME, new RelativeFileSet(baseDir, lst));
    }

    //Currently unused?
    //
    //public void setRuntime(RelativeFileSet fs) {
    //       runtime = fs;
    //}

    public RelativeFileSet getAppResource() {
        return fetchParam(StandardBundlerParam.APP_RESOURCES);
    }

    public void setAppResource(RelativeFileSet fs) {
        putUnlessNull(PARAM_APP_RESOURCES, fs);
    }

    public File getIcon() {
        return fetchParam(StandardBundlerParam.ICON);
    }

    public void setIcon(File icon) {
        putUnlessNull(PARAM_ICON, icon);
    }

    public String getApplicationCategory() {
        return fetchParam(String.class, PARAM_CATEGORY); //FIXME
    }

    public void setApplicationCategory(String category) {
        putUnlessNull(PARAM_CATEGORY, category);
    }

    public String getMainClassName() {
        String applicationClass = getApplicationClass();
        
        if (applicationClass == null) {
            return null;
        }

        int idx = applicationClass.lastIndexOf(".");
        if (idx >= 0) {
            return applicationClass.substring(idx+1);
        }
        return applicationClass;
    }

    public String getCopyright() {
        return fetchParam(StandardBundlerParam.COPYRIGHT);
    }

    public void setCopyright(String c) {
        putUnlessNull(PARAM_COPYRIGHT, c);
    }

    public String getIdentifier() {
        return fetchParam(StandardBundlerParam.IDENTIFIER);
    }

    public void setIdentifier(String s) {
        putUnlessNull(PARAM_IDENTIFIER, s);
    }

    private String mainJar = null;
    private String mainJarClassPath = null;
    private boolean useFXPackaging = true;

    //are we packaging JavaFX application or regular executable Jar?
    public boolean useJavaFXPackaging() {
        if (mainJar == null) {
            //this will find out answer
            getMainApplicationJar();
        }
        return useFXPackaging;
    }

    //For regular executable Jars we need to take care of classpath
    //For JavaFX executable jars we do not need to pay attention to ClassPath entry in manifest
    public String getAppClassPath() {
        if (mainJar == null) {
            //this will find out answer
            getMainApplicationJar();
        }
        if (useFXPackaging || mainJarClassPath == null) {
            return "";
        }
        return mainJarClassPath;
    }

    //assuming that application was packaged according to the rules
    // we must have application jar, i.e. jar where we embed launcher
    // and have main application class listed as main class!
    //If there are more than one, or none - it will be treated as deployment error
    //
    //Note we look for both JavaFX executable jars and regular executable jars
    //As long as main "application" entry point is the same it is main class
    // (i.e. for FX jar we will use JavaFX manifest entry ...)
    public String getMainApplicationJar() {
        if (mainJar != null) {
            return mainJar;
        }
        
        RelativeFileSet appResources = getAppResource();
        String applicationClass = getApplicationClass();

        if (appResources == null || applicationClass == null) {
            return null;
        }
        File srcdir = appResources.getBaseDirectory();
        for (String fname : appResources.getIncludedFiles()) {
            JarFile jf;
            try {
                jf = new JarFile(new File(srcdir, fname));
                Manifest m = jf.getManifest();
                Attributes attrs = (m != null) ? m.getMainAttributes() : null;
                if (attrs != null) {
                    boolean javaMain = applicationClass.equals(
                               attrs.getValue(Attributes.Name.MAIN_CLASS));
                    boolean fxMain = applicationClass.equals(
                               attrs.getValue(PackagerLib.MANIFEST_JAVAFX_MAIN));
                    if (javaMain || fxMain) {
                        useFXPackaging = fxMain;
                        mainJar = fname;
                        mainJarClassPath = attrs.getValue(Attributes.Name.CLASS_PATH);
                        return mainJar;
                    }
                }
            } catch (IOException ignore) {
            }
        }
        return null;
    }

    public String getVendor() {
        return fetchParam(StandardBundlerParam.VENDOR);
    }

    public void setVendor(String vendor) {
       putUnlessNull(PARAM_VENDOR, vendor);
    }

    public String getEmail() {
        return fetchParam(String.class, PARAM_EMAIL);
    }

    public void setEmail(String email) {
        putUnlessNull(PARAM_EMAIL, email);
    }
    
    public void putUnlessNull(String param, Object value) {
        if (value != null) {
            params.put(param, value);
        }
    }

}
