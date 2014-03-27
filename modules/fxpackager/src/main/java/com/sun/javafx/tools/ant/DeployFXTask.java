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

package com.sun.javafx.tools.ant;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import com.sun.javafx.tools.ant.Platform.Jvmarg;
import com.sun.javafx.tools.ant.Platform.Property;
import com.sun.javafx.tools.packager.DeployParams;
import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.packager.PackagerException;
import com.sun.javafx.tools.packager.PackagerLib;
import com.sun.javafx.tools.packager.bundlers.BundleType;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicAttribute;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.DataType;

/**
 * Generates package for Web deployment and redistribution of application.
 * Package includes of set of jar files, JNLP file and HTML file.
 *
 * Minimal example:
 * <pre>
 *   &lt;fx:deploy width="600" height="400"
 *                 outdir="web-dist" outfile="Fish"&gt;
 *       &lt;info title="Sample application"/&gt;
 *       &lt;fx:application refid="myapp"/&gt;
 *       &lt;fx:resources refid="myresources"/&gt;
 *   &lt;/fx:deploy&gt;
 * </pre>
 * Above example will generate HTML/JNLP files into the web-dist directory
 * and use "Fish" as prefix for generated files. Details about application and
 * its resources are defined elsewhere in the application and resource elements.
 * <p>
 * Minimal complete example:
 * <pre>
 *   &lt;fx:deploy width="600" height="400"
 *                 outdir="web-dist" outfile="Fish"&gt;
 *       &lt;info title="Sample application"/&gt;
 *       &lt;fx:application name="SampleApp"
 *              mainClass="testapp.MainApp"
 *              preloaderClass="testpreloader.Preloader"/&gt;
 *       &lt;fx:resources&gt;
 *              &lt;fx:fileset requiredFor="preloader" dir="dist"&gt;
 *                &lt;include name="preloader.jar"/&gt;
 *             &lt;/fx:fileset&gt;
 *              &lt;fx:fileset dir="dist"&gt;
 *                &lt;include name="helloworld.jar"/&gt;
 *             &lt;/fx:fileset&gt;
 *       &lt;/fx:resources&gt;
 *   &lt;/fx:deploy&gt;
 * </pre>
 * Same as above but here application and resource details are defined in place.
 * Note that using references helps with reducing code duplication as fx:jar need
 * to be used for double clickable jars.
 *
 * @ant.task name="deploy" category="javafx"
 */
public class DeployFXTask extends Task implements DynamicAttribute {
    private String width = null;
    private String height = null;
    private String embeddedWidth = null;
    private String embeddedHeight = null;
    private String outfile = null;
    private String outdir = null;
    private boolean embedJNLP;
    private boolean isExtension = false;

    //Before FCS default is to include DT files with app
    // to ensure tests are using latest and compatible.
    //After FCS default is to use shared copy.
    private boolean includeDT = false;

    private String updateMode="background";
    private Info appInfo = null;
    private Application app = null;
    private Resources resources = null;
    private Preferences prefs = null;
    private String codebase = null;

    //container to embed application into
    //could be either string id or js code. If it is string id then it needs to
    //be escaped
    private String placeholder;

    private PackagerLib packager;
    private DeployParams deployParams;

    private Callbacks callbacks;

    boolean offlineAllowed = true;

    //default native bundle settings
    // use NONE to avoid large disk space and build time overhead
    BundleType nativeBundles = BundleType.NONE;
    String bundleFormat = null;

    private boolean verbose = false;
    public void setVerbose(boolean v) {
        verbose = v;
    }

    public void setCodebase(String str) {
        codebase = str;
    }

    public DeployFXTask() {
        packager = new PackagerLib();
        deployParams = new DeployParams();
    }

    @Override
    public void execute() {
        deployParams.setOutfile(outfile);
        deployParams.setOutdir(new File(outdir));
        deployParams.setOfflineAllowed(offlineAllowed);
        deployParams.setVerbose(verbose);
        deployParams.setCodebase(codebase);

        if (width != null) {
            deployParams.setWidth(Integer.valueOf(width));
        }

        if (height != null) {
            deployParams.setHeight(Integer.valueOf(height));
        }

        if (embeddedWidth != null && embeddedHeight != null) {
            deployParams.setEmbeddedDimensions(embeddedWidth, embeddedHeight);
        }

        deployParams.setEmbedJNLP(embedJNLP);
        if (perms != null) {
           deployParams.setEmbedCertifcates(perms.embed);
           deployParams.setAllPermissions(perms.elevated);
        }

        if (app != null) {
            deployParams.setApplicationClass(app.get().mainClass);
            deployParams.setPreloader(app.get().preloaderClass);
            deployParams.setAppId(app.get().id);
            deployParams.setAppName(app.get().name);
            deployParams.setParams(app.get().parameters);
            deployParams.setArguments(app.get().getArguments());
            deployParams.setHtmlParams(app.get().htmlParameters);
            deployParams.setFallback(app.get().fallbackApp);
            deployParams.setSwingAppWithEmbeddedJavaFX(
                    app.get().embeddedIntoSwing);
            deployParams.setVersion(app.get().version);
            deployParams.setId(app.get().id);
            deployParams.setServiceHint(app.get().daemon);
        }

        if (appInfo != null) {
            deployParams.setTitle(appInfo.title);
            deployParams.setVendor(appInfo.vendor);
            deployParams.setDescription(appInfo.appDescription);
            deployParams.setCategory(appInfo.category);
            deployParams.setLicenseType(appInfo.licenseType);
            deployParams.setCopyright(appInfo.copyright);
            deployParams.setEmail(appInfo.email);

            for (Info.Icon i: appInfo.icons) {
                if (i instanceof Info.Splash) {
                   deployParams.addIcon(i.href, i.kind, i.width, i.height, i.depth,
                        ((Info.Splash) i).mode);
                } else {
                   deployParams.addIcon(i.href, i.kind, i.width, i.height, i.depth,
                        DeployParams.RunMode.WEBSTART);
                }
            }
        }

        deployParams.setUpdateMode(updateMode);
        deployParams.setExtension(isExtension);
        deployParams.setIncludeDT(includeDT);

        if (platform != null) {
            Platform pl = platform.get();
            if (pl.j2se != null) {
                deployParams.setJRE(pl.j2se);
            }
            if (pl.javafx != null) {
                deployParams.setJavafx(pl.javafx);
            }

            //only pass it further if it was explicitly set
            // as we do not want to override default
            if (pl.javaRoot != null) {
                if (Platform.USE_SYSTEM_JRE.equals(pl.javaRoot)) {
                    deployParams.setJavaRuntimeSource(null);
                } else {
                    deployParams.setJavaRuntimeSource(new File(pl.javaRoot));
                }
            }
            for (Property p: pl.properties) {
                deployParams.addJvmProperty(p.name, p.value);
            }
            for (Jvmarg a: pl.jvmargs) {
                deployParams.addJvmArg(a.value);
            }
            for (Property a: pl.jvmUserArgs) {
                deployParams.addJvmUserArg(a.name, a.value);
            }
        }

        if (callbacks != null) {
            deployParams.setCallbacks(callbacks.callbacks);
        }

        if (prefs != null) {
            deployParams.setNeedShortcut(prefs.getShortcut());
            deployParams.setNeedInstall(prefs.getInstall());
            deployParams.setNeedMenu(prefs.getMenu());
            deployParams.setSystemWide(prefs.getSystemInstall());
        }

        for (Template t: templateList) {
            deployParams.addTemplate(t.infile, t.outfile);
        }

        for (BundleArgument ba : bundleArgumentList) {
            deployParams.addBundleArgument(ba.arg, ba.value);
        }

        deployParams.setPlaceholder(placeholder);

        if (resources != null) {
            for (FileSet fs: resources.getResources()) {
                   Utils.addResources(deployParams, fs);
            }
        }

        deployParams.setBundleType(nativeBundles);
        deployParams.setTargetFormat(bundleFormat);

        Log.setLogger(new AntLog(this.getProject()));

        try {
            packager.generateDeploymentPackages(deployParams);
        } catch (PackagerException pe) {
            throw new BuildException(pe.getCause().getMessage(),
                    pe.getCause());
        } catch (Exception e) {
            throw new BuildException(e.getMessage(), e);
        } finally {
            Log.setLogger(null);
        }
    }

    /**
     * Set to true if we are generating an 'extension' JNLP.
     *
     * @ant.not-required Default is false.
     */
    public void setExtension(boolean v) {
        isExtension = v;
    }

    public void setNativeBundles(String v) {
        if ("false".equals(v) || "none".equals(v)) {
            nativeBundles = BundleType.NONE;
        } else if ("all".equals(v) || "true".equals(v)) {
            nativeBundles = BundleType.ALL;
        } else if ("image".equals(v)) {
            nativeBundles = BundleType.IMAGE;
        } else if ("installer".equals(v)) {
            nativeBundles = BundleType.INSTALLER;
        } else {
            //assume it is request to build only specific format (like exe or msi)
            nativeBundles = BundleType.INSTALLER;
            bundleFormat = (v != null) ? v.toLowerCase() : null;
        }
    }

    /**
     * Indicates the preferences for when checks for application updates
     * are performed. Supported modes are always, timeout and background.
     *
     * @ant.not-required Default is background.
     */
    public void setUpdateMode(String v) {
        String l = v.toLowerCase();
        if ("eager".equals(l)) {
            //workaround for doc bug in 2.0
            l="always";
        }
        if (!"always".equals(l) && !"background".equals(l)
                && !"timeout".equals(l)) {
            throw new BuildException("Unknown update mode: ["+l+"]." +
                    "Supported modes are: 'always', 'timeout' and 'background'");
        }
        updateMode = l;
    }

    /**
     * Indicates if the application can be launched offline.
     *
     * If application is already downloaded and update mode is eager then
     * the check will timeout after a few seconds, in which case the cached
     * application will be launched instead.
     *
     * Given a reasonably fast server connection,
     * the latest version of the application will usually be run,
     * but it is not guaranteed. The application, however, can be run offline.
     *
     * @ant.not-required Default is true.
     */
    public void setOfflineAllowed(boolean v) {
        offlineAllowed = v;
    }

    /**
     * Application width for embedding application into Web page
     *
     * @ant.optional
     */
    public void setEmbeddedWidth(String w) {
        embeddedWidth = w;
    }

    /**
     * Application width. Used for webstart and embedded applications
     * unless emdeddedWidth is specified
     *
     * @ant.required
     */
    public void setWidth(String v) {
        width = v;
    }

    /**
     * Application width for embedding application into Web page
     *
     * @ant.optional
     */
    public void setEmbeddedHeight(String w) {
        embeddedHeight = w;
    }

    /**
     * Application height. Used for webstart and embedded applications
     * unless emdeddedHeight is specified
     *
     * @ant.required
     */
    public void setHeight(String v) {
        height = v;
    }

    /**
     * Enable embedding JNLP descriptor into Web page.
     * Reduces number of network connections to be made on startup and
     * help to improve startup time.
     *
     * @ant.not-required Default is false.
     */
    public void setEmbedJNLP(boolean v) {
        embedJNLP = v;
    }

    /**
     * Directory where application package will be saved.
     *
     * @ant.required
     */
    public void setOutdir(String v) {
        outdir = v;
    }

    /**
     * Prefix to be used for new generated files.
     *
     * @ant.required
     */
    public void setOutfile(String v) {
        outfile = v;
    }

    /**
     * If true then web deployment is done using javascript files
     * on java.com. Otherwise copy of javascript file is included into
     * application package.
     *
     * @ant.not-required Before FCS default is false. For FCS default is true.
     */
    public void setIncludeDT(Boolean v) {
        includeDT = v;
    }

    /**
     * Placeholder in the web page where application will be embedded.
     * This is expected to be Javascript DOM object.
     *
     * @ant.required Either reference or id of placeholder is required.
     */
    public void setPlaceholderRef(String p) {
        this.placeholder = p;
    }

    /**
     * Id of the placeholder in the web page where application will be embedded.
     * Javascript's document.getElementById() is expected to be able to resolve it.
     *
     * @ant.required Either reference or id of placeholder is required.
     */
    public void setPlaceholderId(String id) {
        //raw id of the placeholder, need to escape it
        this.placeholder = "'"+id+"'";
    }

    public Info createInfo() {
        appInfo = new Info();
        return appInfo;
    }

    public Application createApplication() {
        app = new Application();
        return app;
    }

    public Preferences createPreferences() {
        prefs = new Preferences();
        return prefs;
    }

    public Callbacks createCallbacks() {
        if (callbacks != null) {
            throw new BuildException("Only one callbacks element is supported.");
        }
        callbacks = new Callbacks();
        return callbacks;
    }

    public Resources createResources() {
        if (resources != null) {
            throw new BuildException("Only one resources element is supported.");
        }
        resources = new Resources();
        return resources;
    }

    List<Template> templateList = new LinkedList<>();

    public Template createTemplate() {
        Template t = new Template();
        templateList.add(t);
        return t;
    }

    Platform platform;

    public Platform createPlatform() {
        platform = new Platform();
        return platform;
    }

    private Permissions perms = null;

    public Permissions createPermissions() {
        perms = new Permissions();
        return perms;
    }

    List<BundleArgument> bundleArgumentList = new LinkedList<>();

    public BundleArgument createBundleArgument() {
        BundleArgument ba = new BundleArgument();
        bundleArgumentList.add(ba);
        return ba;
    }

    @Override
    public void setDynamicAttribute(String name, String value) throws BuildException {
        //Use qName and value - can't really validate anything until we know which bundlers we have, so this has
        //to done (way) downstream
        bundleArgumentList.add(new BundleArgument(name, value));
    }


    /**
     * Definition of security permissions needed by application.
     * By default it is assumed that application may run in sandbox.
     * Requesting elevated permissions assumes that application jar
     * files are signed.
     *
     * @ant.type name="Permissions" category="javafx"
     */
    public static class Permissions extends DataType {
        boolean embed = false;
        boolean elevated = true;

        /**
         * If set to false then application can run in sandbox.
         *
         * @ant.not-required Default is true.
         */
        public void setElevated(boolean v) {
            elevated = v;
        }

        /**
         * If true then certificate used to sign jar files will be cached
         * in the JNLP file. This allows to ask user to accept elevating
         * permissions earlier and improves startup time.
         * <p>
         * This has no effect if application is run in the sandbox.
         *
         * @ant.not-required By default is false.
         */
        public void setCacheCertificates(boolean v) {
            embed = v;
        }
    }

    /**
     * Template to preprocess.
     * <p>
     * Template is the HTML file containing markers to be replaced with
     * javascript or HTML snippets needed to deploy JavaFX application on the
     * Web page. This allows to deploy application into "real" Web pages
     * and simplify development process if application is tightly
     * integrated with the page (e.g. uses javascript to communicate to it).
     * <p>
     * Marker has the form of #XXX# or #XXX(id)#. Where id is identifier
     * of an application and XXX is one of following:
     * <ul>
     *   <li>DT.SCRIPT.URL - location of dtjava.js
     *   <li>DT.SCRIPT.CODE - script element to include dtjava.js
     *   <li>DT.EMBED.CODE.DYNAMIC - code to embed application into given placeholder
     *         It is expected it will be wrapped into function()
     *   <li>DT.EMBED.CODE.ONLOAD - all code needed to embed application into Web page
     *               using onload hook (except inclusion of dtjava.js)
     *   <li>DT.LAUNCH.CODE - code need to launch application.
     *          Expected to be wrappend into function().
     * </ul>
     *
     * Page with multiple different applications can be processed multiple times
     * - one per application. To avoid confusion markers need to use
     * application ids  (alphanumeric string no spaces).
     * <p>
     * If input and output files are the same then template is processed in place.
     * <p>
     * Example:
     * <pre>
     *     &lt;template file="App_template.html" tofile="App.html"/&gt;
     * </pre>
     *
     * @ant.type name="Template" category="javafx"
     */
    public static class Template extends DataType {
        File infile = null;
        File outfile = null;

        /**
         * Input file.
         *
         * @ant.required
         */
        public void setFile(File f) {
            infile = f;
        }

        /**
         * Output file (after preprocessing).
         *
         * @ant.not-required Default is the same as input file.
         */
        public void setTofile(File f) {
            outfile = f;
        }
    }

    /**
     * An argument to be passed off to the bundlers.
     *
     * Each bundler uses a set of arguments that may be shared across
     * the different bundlers or it may be specific to each bundler.
     *
     * Some bundlers declare argument types that are not known to the JDK
     * and may be specific to the particular bundler (such as Mac App Store
     * categories).  These arguments allow you to set and adjust these a
     * rguments.
     *
     * @ant.type name="BundleArgument" category="javafx"
     */
    public static class BundleArgument extends DataType {
        String arg = null;
        String value = null;

        BundleArgument() {

        }

        BundleArgument(String arg, String value) {
            this.arg = arg;
            this.value = value;
        }

        /**
         * Name of the bundle argument.
         *
         * @ant.required
         */
        public void setArg(String arg) {
            this.arg = arg;
        }

        /**
         * Value for the bundle argument.
         *
         * @ant.not-required Default is a literal null
         */
        public void setValue(String value) {
            this.value = value;
        }
    }
}
