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
import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.packager.PackagerLib;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static com.oracle.bundlers.StandardBundlerParam.*;

public class BundleParams {
      
    final protected Map<String, ? super Object> params;
    
    public static final String PARAM_RUNTIME                = "runtime"; // RelativeFileSet
    public static final String PARAM_APP_RESOURCES          = "appResources"; // RelativeFileSet
    public static final String PARAM_TYPE                   = "type"; // BundlerType
    public static final String PARAM_BUNDLE_FORMAT          = "bundleFormat"; // String
    public static final String PARAM_ICON                   = "icon"; // String

    /* Name of bundle file and native launcher */
    public static final String PARAM_NAME                   = "name"; // String

    /* application vendor, used by most of the bundlers */
    public static final String PARAM_VENDOR                 = "vendor"; // String

    /* email name and email, only used for debian */
    public static final String PARAM_EMAIL                  = "email"; // String

    /* Copyright. Used on Mac */
    public static final String PARAM_COPYRIGHT              = "copyright"; // String

    /* GUID on windows for MSI, CFBundleIdentifier on Mac
       If not compatible with requirements then bundler either do not bundle
       or autogenerate */
    public static final String PARAM_IDENTIFIER             = "identifier"; // String

    /* shortcut preferences */
    public static final String PARAM_SHORTCUT               = "shortcutHint"; // boolean
    public static final String PARAM_MENU                   = "menuHint"; // boolean

    /* Application version. Format may differ for different bundlers */
    public static final String PARAM_VERSION                = "appVersion"; // String
    /* Application category. Used at least on Mac/Linux. Value is platform specific */
    public static final String PARAM_CATEGORY               = "applicationCategory"; // String

    /* Optional short application */
    public static final String PARAM_TITLE                  = "title"; // String

    /* Optional application description. Used by MSI and on Linux */
    public static final String PARAM_DESCRIPTION            = "description"; // String

    /* License type. Needed on Linux (rpm) */
    public static final String PARAM_LICENSE_TYPE           = "licenseType"; // String

    /* File(s) with license. Format is OS/bundler specific */
    public static final String PARAM_LICENSE_FILES          = "licenseFiles"; // List<String>

    /* user or system level install.
       null means "default" */
    public static final String PARAM_SYSTEM_WIDE            = "systemWide"; // Boolean

    /* Main application class. Not used directly but used to derive default values */
    public static final String PARAM_APPLICATION_CLASS      = "applicationClass"; // String

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
        return fetchParam(IDENTIFIER);
    }

    public String getPreferencesID() {
        return fetchParam(PREFERENCES_ID);
    }

    public String getTitle() {
        return fetchParam(TITLE);
    }

    public void setTitle(String title) {
        putUnlessNull(PARAM_TITLE, title);
    }

    public String getApplicationClass() {
        return fetchParam(MAIN_CLASS);
    }

    public void setApplicationClass(String applicationClass) {
        putUnlessNull(PARAM_APPLICATION_CLASS, applicationClass);
    }

    public String getAppVersion() {
        return fetchParam(VERSION);
    }

    public void setAppVersion(String version) {
        putUnlessNull(PARAM_VERSION, version);
    }

    public String getDescription() {
        return fetchParam(DESCRIPTION);
    }

    public void setDescription(String s) {
        putUnlessNull(PARAM_DESCRIPTION, s);
    }

    public String getLicenseType() {
        return fetchParam(LICENSE_TYPE);
    }

    public void setLicenseType(String version) {
        putUnlessNull(PARAM_LICENSE_TYPE, version);
    }

    //path is relative to the application root
    public void addLicenseFile(String path) {
        List<String> licenseFile = fetchParam(LICENSE_FILES);
        if (licenseFile == null) {
            licenseFile = new ArrayList<>();
            params.put(PARAM_LICENSE_FILES, licenseFile);
        }
        licenseFile.add(path);
    }

    public Boolean getSystemWide() {
        return fetchParam(SYSTEM_WIDE);
    }

    public void setSystemWide(Boolean b) {
        putUnlessNull(PARAM_SYSTEM_WIDE, b);
    }

    public RelativeFileSet getRuntime() {
        return fetchParam(RUNTIME);
    }

    public boolean isShortcutHint() {
        return fetchParam(SHORTCUT_HINT);
    }

    public void setShortcutHint(boolean v) {
        putUnlessNull(PARAM_SHORTCUT, v);
    }

    public boolean isMenuHint() {
        return fetchParam(MENU_HINT);
    }

    public void setMenuHint(boolean v) {
        putUnlessNull(PARAM_MENU, v);
    }

    public String getName() {
        return fetchParam(APP_NAME);
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


    public List<String> getLicenseFile() {
        return fetchParam(LICENSE_FILES);
    }

    public List<String> getJvmargs() {
        return jvmargs;
    }

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
        params.put(PARAM_RUNTIME, baseDir.toString());
    }

    //Currently unused?
    //
    //public void setRuntime(RelativeFileSet fs) {
    //       runtime = fs;
    //}

    public RelativeFileSet getAppResource() {
        return fetchParam(APP_RESOURCES);
    }

    public void setAppResource(RelativeFileSet fs) {
        putUnlessNull(PARAM_APP_RESOURCES, fs);
    }

    public File getIcon() {
        return fetchParam(ICON);
    }

    public void setIcon(File icon) {
        putUnlessNull(PARAM_ICON, icon);
    }

    public String getApplicationCategory() {
        return fetchParam(CATEGORY);
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
        return fetchParam(COPYRIGHT);
    }

    public void setCopyright(String c) {
        putUnlessNull(PARAM_COPYRIGHT, c);
    }

    public String getIdentifier() {
        return fetchParam(IDENTIFIER);
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
        return fetchParam(VENDOR);
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
