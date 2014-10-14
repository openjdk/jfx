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

package com.oracle.tools.packager.mac;

import com.oracle.tools.packager.BundlerParamInfo;
import com.oracle.tools.packager.JreUtils;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.StandardBundlerParam;
import com.oracle.tools.packager.Log;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.IOUtils;
import com.oracle.tools.packager.UnsupportedPlatformException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.oracle.tools.packager.StandardBundlerParam.*;
import static com.oracle.tools.packager.mac.MacAppBundler.*;

public class MacAppStoreBundler extends MacBaseInstallerBundler {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle(MacAppStoreBundler.class.getName());

    private static final String TEMPLATE_BUNDLE_ICON_HIDPI = "GenericAppHiDPI.icns";
    private final static String DEFAULT_ENTITLEMENTS = "MacAppStore.entitlements";
    private final static String DEFAULT_INHERIT_ENTITLEMENTS = "MacAppStore_Inherit.entitlements";

    public static final BundlerParamInfo<String> MAC_APP_STORE_APP_SIGNING_KEY = new StandardBundlerParam<>(
            I18N.getString("param.signing-key-app.name"),
            I18N.getString("param.signing-key-app.description"),
            "mac.signing-key-app",
            String.class,
            params -> {
                String key = "3rd Party Mac Developer Application: " + SIGNING_KEY_USER.fetchFrom(params);
                try {
                    IOUtils.exec(new ProcessBuilder("security", "find-certificate", "-c", key), VERBOSE.fetchFrom(params));
                    return key;
                } catch (IOException ioe) {
                    return null;
                }
            },
            (s, p) -> s);

    public static final BundlerParamInfo<String> MAC_APP_STORE_PKG_SIGNING_KEY = new StandardBundlerParam<>(
            I18N.getString("param.signing-key-pkg.name"),
            I18N.getString("param.signing-key-pkg.description"),
            "mac.signing-key-pkg",
            String.class,
            params -> {
                String key = "3rd Party Mac Developer Installer: " + SIGNING_KEY_USER.fetchFrom(params);
                try {
                    IOUtils.exec(new ProcessBuilder("security", "find-certificate", "-c", key), VERBOSE.fetchFrom(params));
                    return key;
                } catch (IOException ioe) {
                    return null;
                }
            },
            (s, p) -> s);

    public static final StandardBundlerParam<File> MAC_APP_STORE_ENTITLEMENTS  = new StandardBundlerParam<>(
            I18N.getString("param.mac-app-store-entitlements.name"),
            I18N.getString("param.mac-app-store-entitlements.description"),
            "mac.app-store-entitlements",
            File.class,
            params -> null,
            (s, p) -> new File(s));

    public MacAppStoreBundler() {
        super();
        baseResourceLoader = MacResources.class;
    }

    //@Override
    public File bundle(Map<String, ? super Object> p, File outdir) {
        Log.info(MessageFormat.format(I18N.getString("message.building-bundle"), APP_NAME.fetchFrom(p)));
        if (!outdir.isDirectory() && !outdir.mkdirs()) {
            throw new RuntimeException(MessageFormat.format(I18N.getString("error.cannot-create-output-dir"), outdir.getAbsolutePath()));
        }
        if (!outdir.canWrite()) {
            throw new RuntimeException(MessageFormat.format(I18N.getString("error.cannot-write-to-output-dir"), outdir.getAbsolutePath()));
        }

        // first, load in some overrides
        // icns needs @2 versions, so load in the @2 default
        p.put(DEFAULT_ICNS_ICON.getID(), TEMPLATE_BUNDLE_ICON_HIDPI);

        // next we need to change the jdk/jre stripping to strip gstreamer
        p.put(MAC_RULES.getID(), createMacAppStoreRuntimeRules(p));

        // now we create the app
        File appImageDir = APP_IMAGE_BUILD_ROOT.fetchFrom(p);
        try {
            appImageDir.mkdirs();

            // first, make sure we don't use the local signing key
            p.put(DEVELOPER_ID_APP_SIGNING_KEY.getID(), null);
            File appLocation = prepareAppBundle(p);

            prepareEntitlements(p);

            String signingIdentity = MAC_APP_STORE_APP_SIGNING_KEY.fetchFrom(p);
            String identifierPrefix = BUNDLE_ID_SIGNING_PREFIX.fetchFrom(p);
            String entitlementsFile = getConfig_Entitlements(p).toString();
            String inheritEntitlements = getConfig_Inherit_Entitlements(p).toString();

            signAppBundle(p, appLocation, signingIdentity, identifierPrefix, entitlementsFile, inheritEntitlements);
            ProcessBuilder pb;

            // create the final pkg file
            File finalPKG = new File(outdir, INSTALLER_NAME.fetchFrom(p)+"-MacAppStore.pkg");
            outdir.mkdirs();

            pb = new ProcessBuilder("productbuild",
                    "--component", appLocation.toString(), "/Applications",
                    "--sign", MAC_APP_STORE_PKG_SIGNING_KEY.fetchFrom(p),
                    "--product", appLocation + "/Contents/Info.plist",
                    finalPKG.getAbsolutePath());
            IOUtils.exec(pb, VERBOSE.fetchFrom(p));
            return finalPKG;
        } catch (Exception ex) {
            Log.info("App Store Ready Bundle failed : " + ex.getMessage());
            ex.printStackTrace();
            Log.debug(ex);
            return null;
        } finally {
            try {
                if (appImageDir != null && !Log.isDebug()) {
                    IOUtils.deleteRecursive(appImageDir);
                } else if (appImageDir != null) {
                    Log.info(MessageFormat.format(I18N.getString("mesasge.intermediate-bundle-location"), appImageDir.getAbsolutePath()));
                }
                if (!VERBOSE.fetchFrom(p)) {
                    //cleanup
                    cleanupConfigFiles(p);
                } else {
                    Log.info(MessageFormat.format(I18N.getString("message.config-save-location"), CONFIG_ROOT.fetchFrom(p).getAbsolutePath()));
                }
            } catch (FileNotFoundException ex) {
                //noinspection ReturnInsideFinallyBlock
                return null;
            }
        }
    }

    protected void cleanupConfigFiles(Map<String, ? super Object> params) {
        if (getConfig_Entitlements(params) != null) {
            getConfig_Entitlements(params).delete();
        }
        if (getConfig_Inherit_Entitlements(params) != null) {
            getConfig_Inherit_Entitlements(params).delete();
        }
        if (MAC_APP_IMAGE.fetchFrom(params) == null) {
            APP_BUNDLER.fetchFrom(params).cleanupConfigFiles(params);
        }
    }

    private File getConfig_Entitlements(Map<String, ? super Object> params) {
        return new File(CONFIG_ROOT.fetchFrom(params), APP_NAME.fetchFrom(params) + ".entitlements");
    }

    private File getConfig_Inherit_Entitlements(Map<String, ? super Object> params) {
        return new File(CONFIG_ROOT.fetchFrom(params), APP_NAME.fetchFrom(params) + "_Inherit.entitlements");
    }

    private void prepareEntitlements(Map<String, ? super Object> params) throws IOException {
        File entitlements = MAC_APP_STORE_ENTITLEMENTS.fetchFrom(params);
        if (entitlements == null || !entitlements.exists()) {
            fetchResource(getEntitlementsFileName(params),
                    I18N.getString("resource.mac-app-store-entitlements"),
                    DEFAULT_ENTITLEMENTS,
                    getConfig_Entitlements(params),
                    VERBOSE.fetchFrom(params));
        } else {
            fetchResource(getEntitlementsFileName(params),
                    I18N.getString("resource.mac-app-store-entitlements"),
                    entitlements,
                    getConfig_Entitlements(params),
                    VERBOSE.fetchFrom(params));
        }
        fetchResource(getInheritEntitlementsFileName(params),
                I18N.getString("resource.mac-app-store-inherit-entitlements"),
                DEFAULT_INHERIT_ENTITLEMENTS,
                getConfig_Inherit_Entitlements(params),
                VERBOSE.fetchFrom(params));
    }

    private String getEntitlementsFileName(Map<String, ? super Object> params) {
        return MAC_BUNDLER_PREFIX+ APP_NAME.fetchFrom(params) +".entitlements";
    }

    private String getInheritEntitlementsFileName(Map<String, ? super Object> params) {
        return MAC_BUNDLER_PREFIX+ APP_NAME.fetchFrom(params) +"_Inherit.entitlements";
    }


    public static JreUtils.Rule[] createMacAppStoreRuntimeRules(Map<String, ? super Object> params) {
        //Subsetting of JRE is restricted.
        //JRE README defines what is allowed to strip:
        //   ﻿http://www.oracle.com/technetwork/java/javase/jre-8-readme-2095710.html
        //

        List<JreUtils.Rule> rules = new ArrayList<>();

        rules.addAll(Arrays.asList(createMacRuntimeRules(params)));

        File baseDir;

        if (params.containsKey(MAC_RUNTIME.getID())) {
            Object o = params.get(MAC_RUNTIME.getID());
            if (o instanceof RelativeFileSet) {

                baseDir = ((RelativeFileSet) o).getBaseDirectory();
            } else {
                baseDir = new File(o.toString());
            }
        } else {
            baseDir = new File(System.getProperty("java.home"));
        }

        // we accept either pointing at the directories typically installed at:
        // /Libraries/Java/JavaVirtualMachine/jdk1.8.0_40/
        //   * .
        //   * Contents/Home
        //   * Contents/Home/jre
        // /Library/Internet\ Plug-Ins/JavaAppletPlugin.plugin/
        //   * .
        //   * /Contents/Home
        // version may change, and if we don't detect any Contents/Home or Contents/Home/jre we will
        // presume we are at a root.


        try {
            String path = baseDir.getCanonicalPath();
            if (path.endsWith("/Contents/Home/jre")) {
                baseDir = baseDir.getParentFile().getParentFile().getParentFile();
            } else if (path.endsWith("/Contents/Home")) {
                baseDir = baseDir.getParentFile().getParentFile();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!baseDir.exists()) {
            throw new RuntimeException(I18N.getString("error.non-existent-runtime"),
                    new ConfigException(I18N.getString("error.non-existent-runtime"),
                            I18N.getString("error.non-existent-runtime.advice")));
        }

        if (new File(baseDir, "Contents/Home/lib/libjfxmedia_qtkit.dylib").exists()
            || new File(baseDir, "Contents/Home/jre/lib/libjfxmedia_qtkit.dylib").exists())
        {
            rules.add(JreUtils.Rule.suffixNeg("/lib/libjfxmedia_qtkit.dylib"));
        } else {
            rules.add(JreUtils.Rule.suffixNeg("/lib/libjfxmedia.dylib"));
        }
        return rules.toArray(new JreUtils.Rule[rules.size()]);
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
        return "mac.appStore";
    }

    @Override
    public Collection<BundlerParamInfo<?>> getBundleParameters() {
        Collection<BundlerParamInfo<?>> results = new LinkedHashSet<>();
        results.addAll(getAppBundleParameters());
        results.addAll(getPKGBundleParameters());
        return results;
    }

    public Collection<BundlerParamInfo<?>> getPKGBundleParameters() {
        Collection<BundlerParamInfo<?>> results = new LinkedHashSet<>();

        results.addAll(getAppBundleParameters());
        results.remove(DEVELOPER_ID_APP_SIGNING_KEY);
        results.addAll(Arrays.asList(
                MAC_APP_STORE_APP_SIGNING_KEY,
                MAC_APP_STORE_ENTITLEMENTS,
                MAC_APP_STORE_PKG_SIGNING_KEY
        ));

        return results;
    }

    @Override
    public boolean validate(Map<String, ? super Object> params) throws UnsupportedPlatformException, ConfigException {
        try {
            if (params == null) {
                throw new ConfigException(
                        I18N.getString("error.parameters-null"),
                        I18N.getString("error.parameters-null.advice"));
            }

            // hdiutil is always available so there's no need to test for availability.
            //run basic validation to ensure requirements are met

            // Mac App Store apps cannot use the system runtime
            if (params.containsKey(MAC_RUNTIME.getID()) && params.get(MAC_RUNTIME.getID()) == null) {
                throw new ConfigException(
                        I18N.getString("error.no-system-runtime"),
                        I18N.getString("error.no-system-runtime.advice"));
            }

            //we need to change the jdk/jre stripping to strip qtkit code
            params.put(MAC_RULES.getID(), createMacAppStoreRuntimeRules(params));

            //we are not interested in return code, only possible exception
            validateAppImageAndBundeler(params);

            // reject explicitly set to not sign
            if (!Optional.ofNullable(SIGN_BUNDLE.fetchFrom(params)).orElse(Boolean.TRUE)) {
                throw new ConfigException(
                        I18N.getString("error.must-sign-app-store"),
                        I18N.getString("error.must-sign-app-store.advice"));
            }

            // make sure we have settings for signatures
            if (MAC_APP_STORE_APP_SIGNING_KEY.fetchFrom(params) == null) {
                throw new ConfigException(
                        I18N.getString("error.no-app-signing-key"),
                        I18N.getString("error.no-app-signing-key.advice"));
            }
            if (MAC_APP_STORE_PKG_SIGNING_KEY.fetchFrom(params) == null) {
                throw new ConfigException(
                        I18N.getString("error.no-pkg-signing-key"),
                        I18N.getString("error.no-pkg-signing-key.advice"));
            }

            // things we could check...
            // check the icons, make sure it has hidpi icons
            // check the category, make sure it fits in the list apple has provided
            // validate bundle identifier is reverse dns
            //  check for \a+\.\a+\..

            return true;
        } catch (RuntimeException re) {
            if (re.getCause() instanceof ConfigException) {
                throw (ConfigException) re.getCause();
            } else {
                throw new ConfigException(re);
            }
        }
    }

    @Override
    public File execute(Map<String, ? super Object> params, File outputParentDir) {
        return bundle(params, outputParentDir);
    }
}
