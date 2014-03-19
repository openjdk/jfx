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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.oracle.bundlers.JreUtils.Rule.suffix;
import static com.oracle.bundlers.JreUtils.Rule.suffixNeg;
import static com.oracle.bundlers.StandardBundlerParam.IDENTIFIER;
import static com.oracle.bundlers.StandardBundlerParam.APP_NAME;
import static com.oracle.bundlers.StandardBundlerParam.VERBOSE;

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

    public static final BundlerParamInfo<String> MAC_APP_STORE_SIGNING_KEY_USER = new StandardBundlerParam<>(
            I18N.getString("param.signing-key-name.name"),
            I18N.getString("param.signing-key-name.description"),
            "mac.signing-key-user-name",
            String.class,
            null,
            params -> {
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PrintStream ps = new PrintStream(baos)) {
                    ProcessBuilder pb = new ProcessBuilder(
                            "dscacheutil",
                            "-q", "user", "-a", "name", System.getProperty("user.name"));

                    IOUtils.exec(pb, Log.isDebug(), false, ps);

                    String commandOutput = baos.toString();

                    Pattern pattern = Pattern.compile(".*gecos: (.*)");
                    Matcher matcher = pattern.matcher(commandOutput);
                    if (matcher.matches()) {
                        return (matcher.group(1));
                    }
                } catch (IOException ioe) {
                    Log.info("Error retrieving gecos name");
                    Log.debug(ioe);
                }
                return null;
            },
            false,
            null);

    public static final BundlerParamInfo<String> MAC_APP_STORE_APP_SIGNING_KEY = new StandardBundlerParam<>(
            I18N.getString("param.signing-key-app.name"),
            I18N.getString("param.signing-key-app.description"),
            "mac.signing-key-app",
            String.class,
            null,
            params -> "3rd Party Mac Developer Application: " + MAC_APP_STORE_SIGNING_KEY_USER.fetchFrom(params),
            false,
            (s, p) -> s);

    public static final BundlerParamInfo<String> MAC_APP_STORE_PKG_SIGNING_KEY = new StandardBundlerParam<>(
            I18N.getString("param.signing-key-pkg.name"),
            I18N.getString("param.signing-key-pkg.description"),
            "mac.signing-key-pkg",
            String.class,
            null,
            params -> "3rd Party Mac Developer Installer: " + MAC_APP_STORE_SIGNING_KEY_USER.fetchFrom(params),
            false,
            (s, p) -> s);

    public static final StandardBundlerParam<File> MAC_APP_STORE_ENTITLEMENTS  = new StandardBundlerParam<>(
            I18N.getString("param.mac-app-store-entitlements.name"),
            I18N.getString("param.mac-app-store-entitlements.description"),
            "mac.app-store-entitlements",
            File.class,
            null,
            params -> null,
            false,
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
            File appLocation = prepareAppBundle(p);

            prepareEntitlements(p);

            List<String> args = new ArrayList<>();
            args.addAll(Arrays.asList(
                    "codesign",
                    "-s", MAC_APP_STORE_APP_SIGNING_KEY.fetchFrom(p), // sign with this key
                    "-f", // replace all existing signatures
                    "--entitlements", getConfig_Entitlements(p).toString() // entitlements
            ));

            // sign all dylibs and jars
            List<String> signTargets = Files.walk(appLocation.toPath())
                    .map(Path::toString)
                    .filter(s -> (s.endsWith(".jar")
                            || s.endsWith(".dylib"))
                    )
                    .collect(Collectors.toList());

            args.addAll(signTargets);
            ProcessBuilder pb = new ProcessBuilder(args);
            IOUtils.exec(pb, VERBOSE.fetchFrom(p));

            // sign all contained executables with an inherit entitlement
            Files.find(appLocation.toPath().resolve("Contents"), Integer.MAX_VALUE,
                    (path, attr) -> (Files.isExecutable(path) && Files.isRegularFile(path)))
                    .filter(path -> (!path.toString().endsWith(".dylib")))
                    .forEach(path -> {
                        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PrintStream ps = new PrintStream(baos)) {
                            ProcessBuilder pb2 = new ProcessBuilder("codesign",
                                    "-s", MAC_APP_STORE_APP_SIGNING_KEY.fetchFrom(p), // sign with this key
                                    "-f", // replace all existing signatures
                                    "--prefix", IDENTIFIER.fetchFrom(p), // use the identifier as a prefix
                                    "--entitlements", getConfig_Inherit_Entitlements(p).toString(), // entitlements
                                    path.toString());
                            IOUtils.exec(pb2, VERBOSE.fetchFrom(p));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

            // sign all plugins and frameworks
            Consumer<? super Path> signIdentifiedByPList = path -> {
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PrintStream ps = new PrintStream(baos)) {
                    ProcessBuilder pb2 = new ProcessBuilder("/usr/libexec/PlistBuddy",
                            "-c", "Print :CFBundleIdentifier", path.resolve("Contents/Info.plist").toString());
                    IOUtils.exec(pb2, VERBOSE.fetchFrom(p), false, ps);
                    String bundleID = baos.toString();

                    pb2 = new ProcessBuilder("codesign",
                            "-s", MAC_APP_STORE_APP_SIGNING_KEY.fetchFrom(p), // sign with this key
                            "-f", // replace all existing signatures
                            //"-i", bundleID, // sign the bundle's CFBundleIdentifier
                            path.toString());
                    IOUtils.exec(pb2, VERBOSE.fetchFrom(p));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };
            Path pluginsPath = appLocation.toPath().resolve("Contents/PlugIns");
            if (Files.isDirectory(pluginsPath)) {
                Files.list(pluginsPath)
                        .forEach(signIdentifiedByPList);
            }
            Path frameworkPath = appLocation.toPath().resolve("Contents/Frameworks");
            if (Files.isDirectory(frameworkPath)) {
                Files.list(frameworkPath)
                        .forEach(signIdentifiedByPList);
            }

            // sign the app itself
            pb = new ProcessBuilder("codesign",
                    "-s", MAC_APP_STORE_APP_SIGNING_KEY.fetchFrom(p), // sign with this key
                    "-f", // replace all existing signatures
                    "--entitlements", getConfig_Entitlements(p).toString(), // entitlements
                    appLocation.toString());
            IOUtils.exec(pb, VERBOSE.fetchFrom(p));

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

            // more stringent app store validations
            // check the icons, make sure it has hidpi icons
            // check the category, make sure it fits in the list apple has provided
            // make sure we have settings for signatures
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
