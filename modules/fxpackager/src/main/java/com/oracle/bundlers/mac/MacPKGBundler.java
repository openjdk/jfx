/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.bundlers.mac;

import com.oracle.bundlers.BundlerParamInfo;
import com.oracle.bundlers.StandardBundlerParam;

import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.packager.bundlers.ConfigException;
import com.sun.javafx.tools.packager.bundlers.IOUtils;
import com.sun.javafx.tools.packager.bundlers.UnsupportedPlatformException;
import com.sun.javafx.tools.resource.mac.MacResources;

import java.io.*;
import java.text.MessageFormat;import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static com.oracle.bundlers.StandardBundlerParam.APP_NAME;
import static com.oracle.bundlers.StandardBundlerParam.BUILD_ROOT;
import static com.oracle.bundlers.StandardBundlerParam.IDENTIFIER;
import static com.oracle.bundlers.StandardBundlerParam.SERVICE_HINT;
import static com.oracle.bundlers.StandardBundlerParam.VERBOSE;

public class MacPKGBundler extends MacBaseInstallerBundler {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle("com.oracle.bundlers.mac.MacPKGBundler");

    public final static String MAC_BUNDLER_PREFIX =
            BUNDLER_PREFIX + "macosx" + File.separator;
    
    private static final String TEMPLATE_PREINSTALL_SCRIPT = "preinstall.template";
    private static final String TEMPLATE_POSTINSTALL_SCRIPT = "postinstall.template";
    
    private static final BundlerParamInfo<File> PACKAGES_ROOT = new StandardBundlerParam<>(
            I18N.getString("param.packages-root.name"),
            I18N.getString("param.packages-root.description"),
            "mac.pkg.packagesRoot",
            File.class,
            null,
            params -> {
                File packagesRoot = new File(BUILD_ROOT.fetchFrom(params), "packages");
                packagesRoot.mkdirs();
                return packagesRoot;
            },
            false,
            (s, p) -> new File(s));

    
    protected final BundlerParamInfo<File> SCRIPTS_DIR = new StandardBundlerParam<>(
            I18N.getString("param.scripts-dir.name"),
            I18N.getString("param.scripts-dir.description"),
            "mac.pkg.scriptsDir",
            File.class,
            null,
            params -> {
                File scriptsDir = new File(CONFIG_ROOT.fetchFrom(params), "scripts");
                scriptsDir.mkdirs();
                return scriptsDir;
            },
            false,
            (s, p) -> new File(s));
    
    
    public MacPKGBundler() {
        super();
        baseResourceLoader = MacResources.class;
    }

    //@Override
    public File bundle(Map<String, ? super Object> p, File outdir) {
        Log.info(MessageFormat.format(I18N.getString("message.building-pkg"), APP_NAME.fetchFrom(p)));

        File appImageDir = APP_IMAGE_BUILD_ROOT.fetchFrom(p);
        File daemonImageDir = DAEMON_IMAGE_BUILD_ROOT.fetchFrom(p);

        try {
            appImageDir.mkdirs();
            prepareAppBundle(p);
            
            if (SERVICE_HINT.fetchFrom(p)) {
                daemonImageDir.mkdirs();
                prepareDaemonBundle(p);
            }

            return createPKG(p, outdir);
        } catch (Exception ex) {
            return null;
        }
    }

    private File getPackages_AppPackage(Map<String, ? super Object> params) {
        return new File(PACKAGES_ROOT.fetchFrom(params), APP_NAME.fetchFrom(params) + "-app.pkg");
    }

    private File getPackages_DaemonPackage(Map<String, ? super Object> params) {
        return new File(PACKAGES_ROOT.fetchFrom(params), APP_NAME.fetchFrom(params) + "-daemon.pkg");
    }
    
    private void cleanupPackagesFiles(Map<String, ? super Object> params) {
        if (getPackages_AppPackage(params) != null) {
            getPackages_AppPackage(params).delete();
        }
        if (getPackages_DaemonPackage(params) != null) {
            getPackages_DaemonPackage(params).delete();
        }
    }
    
    private File getScripts_PreinstallFile(Map<String, ? super Object> params) {
        return new File(SCRIPTS_DIR.fetchFrom(params), "preinstall");
    }

    private File getScripts_PostinstallFile(Map<String, ? super Object> params) {
        return new File(SCRIPTS_DIR.fetchFrom(params), "postinstall");
    }

    private void cleanupPackageScripts(Map<String, ? super Object> params) {
        if (getScripts_PreinstallFile(params) != null) {
            getScripts_PreinstallFile(params).delete();
        }
        if (getScripts_PostinstallFile(params) != null) {
            getScripts_PostinstallFile(params).delete();
        }
    }
    
    private String getDaemonIdentifier(Map<String, ? super Object> params) {
        return IDENTIFIER.fetchFrom(params).toLowerCase() + ".daemon";
    }
    
    private void preparePackageScripts(Map<String, ? super Object> params) throws IOException
    {
        Log.verbose(I18N.getString("message.preparing-scripts"));
        
        Map<String, String> data = new HashMap<>();

        data.put("DEPLOY_DAEMON_IDENTIFIER", getDaemonIdentifier(params));
        data.put("DEPLOY_LAUNCHD_PLIST_FILE",
                IDENTIFIER.fetchFrom(params).toLowerCase() + ".launchd.plist");
        
        Writer w = new BufferedWriter(new FileWriter(getScripts_PreinstallFile(params)));
        String content = preprocessTextResource(
                MAC_BUNDLER_PREFIX + getScripts_PreinstallFile(params).getName(),
                I18N.getString("resource.pkg-preinstall-script"),
                TEMPLATE_PREINSTALL_SCRIPT,
                data,
                VERBOSE.fetchFrom(params));
        w.write(content);
        w.close();
        getScripts_PreinstallFile(params).setExecutable(true, false);
        
        w = new BufferedWriter(new FileWriter(getScripts_PostinstallFile(params)));
        content = preprocessTextResource(
                MAC_BUNDLER_PREFIX + getScripts_PostinstallFile(params).getName(),
                I18N.getString("resource.pkg-postinstall-script"),
                TEMPLATE_POSTINSTALL_SCRIPT,
                data,
                VERBOSE.fetchFrom(params));
        w.write(content);
        w.close();
        getScripts_PostinstallFile(params).setExecutable(true, false);        
    }
    
    private File createPKG(Map<String, ? super Object> params, File outdir) {
        //generic find attempt
        try {
            String appLocation =
                    APP_IMAGE_BUILD_ROOT.fetchFrom(params) + "/" + APP_NAME.fetchFrom(params) + ".app";
            File predefinedImage = getPredefinedImage(params);
            if (predefinedImage != null) {
                appLocation = predefinedImage.getAbsolutePath();
            }
            
            String daemonLocation = DAEMON_IMAGE_BUILD_ROOT.fetchFrom(params) + "/" + APP_NAME.fetchFrom(params) + ".daemon";
            
            File appPKG = getPackages_AppPackage(params);
            File daemonPKG = getPackages_DaemonPackage(params);

            // build application package
            ProcessBuilder pb = new ProcessBuilder("pkgbuild",
                    "--component",
                    appLocation,
                    "--install-location",
                    "/Applications",
                    appPKG.getAbsolutePath());
            IOUtils.exec(pb, VERBOSE.fetchFrom(params));

            // build daemon package if requested
            if (SERVICE_HINT.fetchFrom(params)) {
                preparePackageScripts(params);
                
                pb = new ProcessBuilder("pkgbuild",
                        "--identifier",
                        APP_NAME.fetchFrom(params) + ".daemon",
                        "--root",
                        daemonLocation,
                        "--scripts",
                        SCRIPTS_DIR.fetchFrom(params).getAbsolutePath(),
                        daemonPKG.getAbsolutePath());
                IOUtils.exec(pb, VERBOSE.fetchFrom(params));
            }

            // build final package
            File finalPKG = new File(outdir, APP_NAME.fetchFrom(params)+".pkg");
            outdir.mkdirs();

            List<String> commandLine = new ArrayList<>();
            commandLine.add("productbuild");
            commandLine.add("--package");
            commandLine.add(appPKG.getAbsolutePath());
            if (SERVICE_HINT.fetchFrom(params)) {
                commandLine.add("--package");
                commandLine.add(daemonPKG.getAbsolutePath());            
            }
            commandLine.add(finalPKG.getAbsolutePath());

            pb = new ProcessBuilder(commandLine);
            IOUtils.exec(pb, VERBOSE.fetchFrom(params));
            
            return finalPKG;
        } catch (Exception ignored) {
            Log.verbose(ignored);
            return null;
        } finally {
            if (!VERBOSE.fetchFrom(params)) {
                cleanupPackagesFiles(params);
                
                if (SERVICE_HINT.fetchFrom(params)) {
                    cleanupPackageScripts(params);
                }
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////////
    // Implement Bundler
    //////////////////////////////////////////////////////////////////////////////////

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
        return "pkg";
    }

    @Override
    public Collection<BundlerParamInfo<?>> getBundleParameters() {
        //Add PKG Specific parameters as required
        return super.getBundleParameters();
    }

    @Override
    public boolean validate(Map<String, ? super Object> params) throws UnsupportedPlatformException, ConfigException {
        try {
            if (params == null) throw new ConfigException(
                    I18N.getString("error.parameters-null"),
                    I18N.getString("error.parameters-null.advice"));

            // hdiutil is always available so there's no need to test for availability.
            //run basic validation to ensure requirements are met

            //run basic validation to ensure requirements are met
            //we are not interested in return code, only possible exception
            APP_BUNDLER.fetchFrom(params).doValidate(params);
            return true;
        } catch (RuntimeException re) {
            throw new ConfigException(re);
        }
    }

    @Override
    public File execute(Map<String, ? super Object> params, File outputParentDir) {
        return bundle(params, outputParentDir);
    }

}
