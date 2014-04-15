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
import com.oracle.bundlers.JreUtils;
import com.oracle.bundlers.StandardBundlerParam;
import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.packager.bundlers.ConfigException;
import com.sun.javafx.tools.packager.bundlers.IOUtils;
import com.sun.javafx.tools.packager.bundlers.MacAppBundler;
import com.sun.javafx.tools.packager.bundlers.UnsupportedPlatformException;
import com.sun.javafx.tools.resource.mac.MacResources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;

import static com.oracle.bundlers.JreUtils.Rule.suffix;
import static com.oracle.bundlers.JreUtils.Rule.suffixNeg;
import static com.oracle.bundlers.StandardBundlerParam.*;

public class MacAppStoreBundler extends MacBaseInstallerBundler {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle("com.oracle.bundlers.mac.MacAppStoreBundler");

    private static final String TEMPLATE_BUNDLE_ICON_HIDPI = "GenericAppHiDPI.icns";
    private final static String DEFAULT_ENTITLEMENTS = "MacAppStore.entitlements";
    private final static String DEFAULT_INHERIT_ENTITLEMENTS = "MacAppStore_Inherit.entitlements";

    //Subsetting of JRE is restricted.
    //JRE README defines what is allowed to strip:
    //   ﻿http://www.oracle.com/technetwork/java/javase/jre-7-readme-430162.html //TODO update when 8 goes GA
    //
    public static final JreUtils.Rule[] MAC_APP_STORE_JDK_RULES =  new JreUtils.Rule[]{
            suffixNeg("macos/libjli.dylib"),
            suffixNeg("resources"),
            suffixNeg("home/bin"),
            suffixNeg("home/db"),
            suffixNeg("home/demo"),
            suffixNeg("home/include"),
            suffixNeg("home/lib"),
            suffixNeg("home/man"),
            suffixNeg("home/release"),
            suffixNeg("home/sample"),
            suffixNeg("home/src.zip"),
            //"home/rt" is not part of the official builds
            // but we may be creating this symlink to make older NB projects
            // happy. Make sure to not include it into final artifact
            suffixNeg("home/rt"),
            suffixNeg("jre/bin"),
            suffixNeg("bin/rmiregistry"),
            suffixNeg("bin/tnameserv"),
            suffixNeg("bin/keytool"),
            suffixNeg("bin/klist"),
            suffixNeg("bin/ktab"),
            suffixNeg("bin/policytool"),
            suffixNeg("bin/orbd"),
            suffixNeg("bin/servertool"),
            suffixNeg("bin/javaws"),
            suffixNeg("bin/java"),
            //Rule.suffixNeg("jre/lib/ext"), //need some of jars there for https to work
            suffixNeg("jre/lib/nibs"),
            //keep core deploy APIs but strip plugin dll
            //Rule.suffixNeg("jre/lib/deploy"),
            //Rule.suffixNeg("jre/lib/deploy.jar"),
            //Rule.suffixNeg("jre/lib/javaws.jar"),
            //Rule.suffixNeg("jre/lib/libdeploy.dylib"),
            //Rule.suffixNeg("jre/lib/plugin.jar"),
            suffixNeg("lib/libnpjp2.dylib"),
            suffixNeg("lib/security/javaws.policy"),

            // jfxmedia uses QuickTime, which is not allowed as of OSX 10.9
            suffixNeg("lib/libjfxmedia.dylib"),

            // the plist is needed for signing
            suffix("Info.plist"),

    };

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
        Log.info("Building Mac App Store Bundle for " + APP_NAME.fetchFrom(p));

        // first, load in some overrides
        // icns needs @2 versions, so load in the @2 default
        p.put(MacAppBundler.DEFAULT_ICNS_ICON.getID(), TEMPLATE_BUNDLE_ICON_HIDPI);

        // next we need to change the jdk/jre stripping to strip gstreamer
        p.put(MacAppBundler.MAC_JDK_RULES.getID(), MAC_APP_STORE_JDK_RULES);

        // now we create the app
        File appImageDir = APP_IMAGE_BUILD_ROOT.fetchFrom(p);
        try {
            appImageDir.mkdirs();

            // first, make sure we don't use the local signing key
            p.put(MacAppBundler.DEVELOPER_ID_APP_SIGNING_KEY.getID(), null);
            File appLocation = prepareAppBundle(p);

            prepareEntitlements(p);

            String signingIdentity = MAC_APP_STORE_APP_SIGNING_KEY.fetchFrom(p);
            String identifierPrefix = IDENTIFIER.fetchFrom(p) + ".";
            String entitlementsFile = getConfig_Entitlements(p).toString();
            String inheritEntitlements = getConfig_Inherit_Entitlements(p).toString();

            signAppBundle(p, appLocation, signingIdentity, identifierPrefix, entitlementsFile, inheritEntitlements);
            ProcessBuilder pb;

            // create the final pkg file
            File finalPKG = new File(outdir, APP_NAME.fetchFrom(p)+".pkg");
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
                    Log.info("[DEBUG] Intermediate application bundle image: "+
                            appImageDir.getAbsolutePath());
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
        APP_BUNDLER.fetchFrom(params).cleanupConfigFiles(params);
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
        return MacAppBundler.MAC_BUNDLER_PREFIX+ APP_NAME.fetchFrom(params) +".entitlements";
    }

    private String getInheritEntitlementsFileName(Map<String, ? super Object> params) {
        return MacAppBundler.MAC_BUNDLER_PREFIX+ APP_NAME.fetchFrom(params) +"_Inherit.entitlements";
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
            throw new ConfigException(re);
        }
    }

    @Override
    public File execute(Map<String, ? super Object> params, File outputParentDir) {
        return bundle(params, outputParentDir);
    }
}
