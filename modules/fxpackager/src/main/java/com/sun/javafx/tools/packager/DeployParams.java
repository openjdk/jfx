/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tools.packager;

import com.sun.javafx.tools.ant.Callback;
import com.sun.javafx.tools.packager.bundlers.*;
import com.sun.javafx.tools.packager.bundlers.Bundler.BundleType;
import com.sun.javafx.tools.resource.DeployResource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class DeployParams extends CommonParams {
    public enum RunMode {
        WEBSTART, EMBEDDED, STANDALONE, ALL
    }

    final List<DeployResource> resources = new ArrayList<>();

    String id;
    String title;
    String vendor;
    String email;
    String description;
    String category;
    String licenseType;
    String copyright;
    String version;
    Boolean systemWide;
    Boolean serviceHint;

    String applicationClass;
    String preloader;

    List<Param> params;
    List<HtmlParam> htmlParams;
    List<String> arguments; //unnamed arguments

    int width;
    int height;
    String embeddedWidth = null;
    String embeddedHeight = null;

    String appName;
    String codebase;

    boolean embedJNLP = true;
    boolean embedCertificates = false;
    boolean allPermissions = false;
    String updateMode = "background";
    boolean isExtension = false;
    boolean isSwingApp = false;

    boolean needShortcut = false;
    boolean needMenu = false;
    boolean needInstall = false;

    String outfile;
    //if true then we cobundle js and image files needed
    // for web deployment with the application
    boolean includeDT;

    String placeholder = null;
    String appId = null;

    // didn't have a setter...
    boolean offlineAllowed = true;

    List<JSCallback> callbacks;

    //list of HTML templates to process
    List<Template> templates = new LinkedList<>();

    String jrePlatform = "1.6+";
    String fxPlatform = PackagerLib.JAVAFX_VERSION+"+";
    File javaRuntimeToUse = null;
    boolean javaRuntimeWasSet = false;

    //list of jvm args (in theory string can contain spaces and need to be escaped
    List<String> jvmargs = new LinkedList<>();
    Map<String, String> jvmUserArgs = new HashMap<>();

    //list of jvm properties (can also be passed as VM args
    // but keeping them separate make it a bit more convinient for JNLP generation)
    Map<String, String> properties = new HashMap<>();
    
    // raw arguments to the bundler
    Map<String, ? super Object> bundlerArguments = new HashMap<>();

    String fallbackApp = "com.javafx.main.NoJavaFXFallback";

    public void setJavaRuntimeSource(File src) {
        javaRuntimeToUse = src;
        javaRuntimeWasSet = true;
    }

    public void setCodebase(String codebase) {
        this.codebase = codebase;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setSystemWide(Boolean systemWide) {
        this.systemWide = systemWide;
    }

    public void setServiceHint(Boolean serviceHint) {
        this.serviceHint = serviceHint;
    }

    public void setJRE(String v) {
        jrePlatform = v;
    }

    public void setSwingAppWithEmbeddedJavaFX(boolean v) {
        isSwingApp = v;
    }

    public void setNeedInstall(boolean b) {
        needInstall = b;
    }

    public void setOfflineAllowed(boolean b) {
        offlineAllowed = b;
    }

    public void setNeedShortcut(boolean b) {
        needShortcut = b;
    }

    public void setNeedMenu(boolean b) {
        needMenu = b;
    }

    public void setEmbeddedDimensions(String w, String h) {
        embeddedWidth = w;
        embeddedHeight = h;
    }

    public void setFallback(String v) {
        if (v == null) {
            return;
        }

        if ("none".equals(v) || "null".equals(v)) {
            fallbackApp = null;
        } else {
            fallbackApp = v;
        }
    }

    public void setJavafx(String v) {
        fxPlatform = v;
    }

    public void addJvmArg(String v) {
        jvmargs.add(v);
    }

    public void addJvmUserArg(String n, String v) {
        jvmUserArgs.put(n, v);
    }

    public void addJvmProperty(String n, String v) {
        properties.put(n, v);
    }

    public void setAllPermissions(boolean allPermissions) {
        this.allPermissions = allPermissions;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setArguments(List<String> args) {
        this.arguments = args;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEmbedJNLP(boolean embedJNLP) {
        this.embedJNLP = embedJNLP;
    }

    public void setEmbedCertifcates(boolean v) {
        embedCertificates = v;
    }

    public void setPlaceholder(String p) {
        placeholder = p;
    }

    public void setAppId(String id) {
        appId = id;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setHtmlParams(List<HtmlParam> htmlParams) {
        this.htmlParams = htmlParams;
    }

    public void setOutfile(String outfile) {
        this.outfile = outfile;
    }

    public void setParams(List<Param> params) {
        this.params = params;
    }

    public void setPreloader(String preloader) {
        this.preloader = preloader;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUpdateMode(String updateMode) {
        this.updateMode = updateMode;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setExtension(boolean isExtension) {
        this.isExtension = isExtension;
    }

    public void setApplicationClass(String applicationClass) {
        this.applicationClass = applicationClass;
    }

    public void setIncludeDT(boolean doEmbed) {
        includeDT = doEmbed;
    }

    public void setJSCallbacks(List<JSCallback> list) {
        callbacks = list;
    }

    public void setCallbacks(List<Callback> list) {
        List<JSCallback> jslist = new ArrayList<>(list.size());
        for (Callback cb: list) {
            jslist.add(new JSCallback(cb.getName(), cb.getCmd()));
        }
        callbacks = jslist;
    }

    static class Template {
        File in;
        File out;

        Template(File in, File out) {
            this.in = in;
            this.out = out;
        }
    }

    public void addTemplate(File in, File out) {
        templates.add(new Template(in, out));
    }

    //we need to expand as in some cases
    // (most notably javafxpackager)
    //we may get "." as filename and assumption is we include
    // everything in the given folder
    // (IOUtils.copyfiles() have recursive behavior)
    List<File> expandFileset(File root) {
        List<File> files = new LinkedList<>();
        if (IOUtils.isNotSymbolicLink(root)) {
           if (root.isDirectory()) {
               File[] children = root.listFiles();
               if (children != null) {
                   for (File f : children) {
                       files.addAll(expandFileset(f));
                   }
               }
           } else {
               files.add(root);
           }
        }
        return files;
    }

    @Override
    public void addResource(File baseDir, String path) {
        File file = new File(baseDir, path);
        try {
            //normalize top level dir
            // to strip things like "." in the path
            // or it can confuse symlink detection logic
            file = file.getCanonicalFile();
        } catch (IOException ignored) {}
        for (File f: expandFileset(file)) {
           resources.add(new DeployResource(baseDir, f));
        }
    }

    @Override
    public void addResource(File baseDir, File file) {
        try {
            //normalize initial file
            // to strip things like "." in the path
            // or it can confuse symlink detection logic
            file = file.getCanonicalFile();
        } catch (IOException ignored) {}
        for (File f: expandFileset(file)) {
           resources.add(new DeployResource(baseDir, f));
        }
    }

    public void addResource(File baseDir, String path, String type) {
        resources.add(new DeployResource(baseDir, path, type));
    }

    public void addResource(File baseDir, File file, String type) {
        resources.add(new DeployResource(baseDir, file, type));
    }

    public void addResource(File baseDir, File file, String mode, String type, String os, String arch) {
        resources.add(new DeployResource(baseDir, file, mode, type, os, arch));
    }

    @Override
    public void validate() throws PackagerException {
        if (outdir == null) {
            throw new PackagerException("ERR_MissingArgument", "-outdir");
        }
        if (outfile == null) {
            throw new PackagerException("ERR_MissingArgument", "-outfile");
        }
        if (resources.isEmpty()) {
            throw new PackagerException("ERR_MissingAppResources");
        }
        if (applicationClass == null) {
            throw new PackagerException("ERR_MissingArgument", "-appclass");
        }
    }

    //could be icon or splash
    static class Icon {
        final static int UNDEFINED = -1;

        String href;
        String kind;
        int width = UNDEFINED;
        int height = UNDEFINED;
        int depth = UNDEFINED;
        RunMode mode = RunMode.WEBSTART;

        Icon(String href, String kind, int w, int h, int d, RunMode m) {
            mode = m;
            this.href = href;
            this.kind = kind;
            if (w > 0) {
                width = w;
            }
            if (h > 0) {
                height = h;
            }
            if (d > 0) {
                depth = d;
            }
        }
    }

    List<Icon> icons = new LinkedList<>();

    public void addIcon(String href, String kind, int w, int h, int d, RunMode m) {
        icons.add(new Icon(href, kind, w, h, d, m));
    }

    BundleType bundleType = BundleType.NONE;
    String targetFormat = null; //means any

    public void setBundleType(BundleType type) {
        bundleType = type;
    }
    
    public BundleType getBundleType() {
        return bundleType;
    }

    public void setTargetFormat(String t) {
        targetFormat = t;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    private String getArch() {
        String arch = System.getProperty("os.arch").toLowerCase();

        if ("x86".equals(arch) || "i386".equals(arch) || "i486".equals(arch)
                || "i586".equals(arch) || "i686".equals(arch)) {
            arch = "x86";
        } else if ("x86_64".equals(arch) || "amd64".equals("arch")) {
            arch = "x86_64";
        }

        return arch;
    }
    
    public void addBundleArgument(String key, String value) {
        bundlerArguments.put(key, value);
    }

    public BundleParams getBundleParams() {
        if (bundleType != BundleType.NONE) {
            BundleParams bundleParams = new BundleParams();

            //construct app resources
            //  relative to output folder!
            Set<File> files = new HashSet<>();
            String currentOS = System.getProperty("os.name").toLowerCase();
            String currentArch = getArch();

            for (DeployResource r: resources) {
                String os = r.getOs();
                String arch = r.getArch();
                //skip resources for other OS
                // and nativelib jars (we are including raw libraries)
                if ((os == null || currentOS.contains(os.toLowerCase())) &&
                        (arch == null || currentArch.startsWith(arch.toLowerCase()))
                        && r.getType() != DeployResource.Type.nativelib) {
                    files.add(new File(outdir, r.getRelativePath()));
                    if (r.getType() == DeployResource.Type.license) {
                        bundleParams.addLicenseFile(r.getRelativePath());
                    }
                }
            }
            RelativeFileSet appResources = new RelativeFileSet(outdir, files);

            bundleParams.setIdentifier(id);
            bundleParams.setAppResource(appResources);

            if (javaRuntimeWasSet) {
                bundleParams.setRuntime(javaRuntimeToUse);
            } else {
                bundleParams.setDefaultRuntime();
            }
            bundleParams.setApplicationClass(applicationClass);
            bundleParams.setName(this.appName);
            bundleParams.setAppVersion(version);
            bundleParams.setType(bundleType);
            bundleParams.setBundleFormat(targetFormat);
            bundleParams.setVendor(vendor);
            bundleParams.setEmail(email);
            bundleParams.setShortcutHint(needShortcut);
            bundleParams.setMenuHint(needMenu);
            bundleParams.setSystemWide(systemWide);
            bundleParams.setServiceHint(serviceHint);
            bundleParams.setCopyright(copyright);
            bundleParams.setApplicationCategory(category);
            bundleParams.setLicenseType(licenseType);
            bundleParams.setDescription(description);
            bundleParams.setTitle(title);
            bundleParams.setVerbose(verbose);

            bundleParams.setJvmProperties(properties);
            bundleParams.setJvmargs(jvmargs);
            bundleParams.setJvmUserArgs(jvmUserArgs);

            File appIcon = null;
            for (Icon ic: icons) {
                //NB: in theory we should be paying attention to RunMode but
                // currently everything is marked as webstart internally and runmode
                // is not publicly documented property
                if (/* (ic.mode == RunMode.ALL || ic.mode == RunMode.STANDALONE) && */
                    (ic.kind == null || ic.kind.equals("default"))) {
                    //could be full path or something relative to the output folder
                    appIcon = new File(ic.href);
                    if (!appIcon.exists()) {
                        Log.debug("Icon ["+ic.href+"] is not valid absolute path. "+
                                "Assume it is relative to the output dir.");
                        appIcon = new File(outdir, ic.href);
                    }
                }
            }

            bundleParams.setIcon(appIcon);

            // check for collisions
            TreeSet<String> keys = new TreeSet<>(bundlerArguments.keySet());
            keys.retainAll(bundleParams.getBundleParamsAsMap().keySet());

            if (!keys.isEmpty()) {
                throw new RuntimeException("Deploy Params and Bundler Arguments overlap in the following values:" + keys.toString());
            }
            
            bundleParams.addAllBundleParams(bundlerArguments);
            
            return bundleParams;
        } else {
            return null;
        }
    }
}
