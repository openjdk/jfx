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

import com.oracle.tools.packager.AbstractBundler;
import com.oracle.tools.packager.BundlerParamInfo;
import com.oracle.tools.packager.StandardBundlerParam;
import com.oracle.tools.packager.Log;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.IOUtils;
import com.oracle.tools.packager.UnsupportedPlatformException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.oracle.tools.packager.StandardBundlerParam.*;

public abstract class MacBaseInstallerBundler extends AbstractBundler {

    private static final ResourceBundle I18N =
            ResourceBundle.getBundle(MacBaseInstallerBundler.class.getName());

    //This could be generalized more to be for any type of Image Bundler
    public static final BundlerParamInfo<MacAppBundler> APP_BUNDLER = new StandardBundlerParam<>(
            I18N.getString("param.app-bundler.name"),
            I18N.getString("param.app-bundle.description"),
            "mac.app.bundler",
            MacAppBundler.class,
            params -> new MacAppBundler(),
            (s, p) -> null);

    public final BundlerParamInfo<File> APP_IMAGE_BUILD_ROOT = new StandardBundlerParam<>(
            I18N.getString("param.app-image-build-root.name"),
            I18N.getString("param.app-image-build-root.description"),
            "mac.app.imageRoot",
            File.class,
            params -> {
                File imageDir = IMAGES_ROOT.fetchFrom(params);
                if (!imageDir.exists()) imageDir.mkdirs();
                try {
                    return Files.createTempDirectory(imageDir.toPath(), "image-").toFile();
                } catch (IOException e) {
                    return new File(imageDir, getID()+ ".image");
                }
            },
            (s, p) -> new File(s));

    public static final StandardBundlerParam<File> MAC_APP_IMAGE = new StandardBundlerParam<>(
            I18N.getString("param.app-image.name"),
            I18N.getString("param.app-image.description"),
            "mac.app.image",
            File.class,
            params -> null,
            (s, p) -> new File(s));


    public static final BundlerParamInfo<MacDaemonBundler> DAEMON_BUNDLER = new StandardBundlerParam<>(
            I18N.getString("param.daemon-bundler.name"),
            I18N.getString("param.daemon-bundler.description"),
            "mac.daemon.bundler",
            MacDaemonBundler.class,
            params -> new MacDaemonBundler(),
            (s, p) -> null);


    public final BundlerParamInfo<File> DAEMON_IMAGE_BUILD_ROOT = new StandardBundlerParam<>(
            I18N.getString("param.daemon-image-build-root.name"),
            I18N.getString("param.daemon-image-build-root.description"),
            "mac.daemon.image",
            File.class,
            params -> {
                File imageDir = IMAGES_ROOT.fetchFrom(params);
                if (!imageDir.exists()) imageDir.mkdirs();
                return new File(imageDir, getID()+ ".daemon");
            },
            (s, p) -> new File(s));


    public static final BundlerParamInfo<File> CONFIG_ROOT = new StandardBundlerParam<>(
            I18N.getString("param.config-root.name"),
            I18N.getString("param.config-root.description"),
            "configRoot",
            File.class,
            params -> {
                File imagesRoot = new File(BUILD_ROOT.fetchFrom(params), "macosx");
                imagesRoot.mkdirs();
                return imagesRoot;
            },
            (s, p) -> null);

    public static final BundlerParamInfo<String> SIGNING_KEY_USER = new StandardBundlerParam<>(
            I18N.getString("param.signing-key-name.name"),
            I18N.getString("param.signing-key-name.description"),
            "mac.signing-key-user-name",
            String.class,
            params -> "",
            null);

    public static final BundlerParamInfo<String> INSTALLER_NAME = new StandardBundlerParam<> (
            I18N.getString("param.installer-name.name"),
            I18N.getString("param.installer-name.description"),
            "mac.installerName",
            String.class,
            params -> {
                String nm = APP_NAME.fetchFrom(params);
                if (nm == null) return null;

                String version = VERSION.fetchFrom(params);
                if (version == null) {
                    return nm;
                } else {
                    return nm + "-" + version;
                }
            },
            (s, p) -> s);

    public static File getPredefinedImage(Map<String, ? super Object> p) {
        File applicationImage = null;
        if (MAC_APP_IMAGE.fetchFrom(p) != null) {
            applicationImage = MAC_APP_IMAGE.fetchFrom(p);
            Log.debug("Using App Image from " + applicationImage);
            if (!applicationImage.exists()) {
                throw new RuntimeException(
                        MessageFormat.format(I18N.getString("message.app-image-dir-does-not-exist"), MAC_APP_IMAGE.getID(), applicationImage.toString()));
            }
        }
        return applicationImage;
    }

    protected void validateAppImageAndBundeler(Map<String, ? super Object> params) throws ConfigException, UnsupportedPlatformException {
        if (MAC_APP_IMAGE.fetchFrom(params) != null) {
            File applicationImage = MAC_APP_IMAGE.fetchFrom(params);
            if (!applicationImage.exists()) {
                throw new ConfigException(
                        MessageFormat.format(I18N.getString("message.app-image-dir-does-not-exist"), MAC_APP_IMAGE.getID(), applicationImage.toString()),
                        MessageFormat.format(I18N.getString("message.app-image-dir-does-not-exist.advice"), MAC_APP_IMAGE.getID()));
            }
            if (APP_NAME.fetchFrom(params) == null) {
                throw new ConfigException(
                        I18N.getString("message.app-image-requires-app-name"),
                        I18N.getString("message.app-image-requires-app-name.advice"));
            }
            if (IDENTIFIER.fetchFrom(params) == null) {
                throw new ConfigException(
                        I18N.getString("message.app-image-requires-identifier"),
                        I18N.getString("message.app-image-requires-identifier.advice"));
            }
        } else {
            APP_BUNDLER.fetchFrom(params).doValidate(params);
        }
    }

    protected File prepareAppBundle(Map<String, ? super Object> p) {
        File predefinedImage = getPredefinedImage(p);
        if (predefinedImage != null) {
            return predefinedImage;
        }

        File appImageRoot = APP_IMAGE_BUILD_ROOT.fetchFrom(p);
        return APP_BUNDLER.fetchFrom(p).doBundle(p, appImageRoot, true);
    }

    protected File prepareDaemonBundle(Map<String, ? super Object> p) {
        File daemonImageRoot = DAEMON_IMAGE_BUILD_ROOT.fetchFrom(p);
        return DAEMON_BUNDLER.fetchFrom(p).doBundle(p, daemonImageRoot, true);
    }

    public static void signAppBundle(Map<String, ? super Object> params, File appLocation, String signingIdentity, String identifierPrefix) throws IOException {
        signAppBundle(params, appLocation, signingIdentity, identifierPrefix, null, null);
    }

    public static void signAppBundle(Map<String, ? super Object> params, File appLocation, String signingIdentity, String identifierPrefix, String entitlementsFile, String inheritedEntitlements) throws IOException {

        AtomicReference<IOException> toThrow = new AtomicReference<>();

        // sign all dylibs and jars
        Files.walk(appLocation.toPath())
                .filter(p -> (p.toString().endsWith(".jar")
                                || p.toString().endsWith(".dylib"))
                ).forEach(p -> {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    if (toThrow.get() != null) return;

                    List<String> args = new ArrayList<>();
                    args.addAll(Arrays.asList(
                            "codesign",
                            "-f")); // replace all existing signatures
                    if (signingIdentity != null) {
                        args.add("-s");
                        args.add(signingIdentity); // sign with this key
                    }
                    if (entitlementsFile != null) {
                        args.add("--entitlements");
                        args.add(entitlementsFile); // entitlements
                    }
                    args.add("-vvvv"); // super verbo1se output
                    args.add(p.toString());


                    try {
                        Set<PosixFilePermission> oldPermissions = Files.getPosixFilePermissions(p);
                        File f = p.toFile();
                        f.setWritable(true, true);

                        ProcessBuilder pb = new ProcessBuilder(args);
                        IOUtils.exec(pb, VERBOSE.fetchFrom(params));

                        Files.setPosixFilePermissions(p, oldPermissions);
                    } catch (IOException ioe) {
                        toThrow.set(ioe);
                    }
                });

        IOException ioe = toThrow.get();
        if (ioe != null) {
            throw ioe;
        }

        // sign all contained executables with an inherit entitlement
        Files.find(appLocation.toPath().resolve("Contents"), Integer.MAX_VALUE,
                (path, attr) -> (Files.isExecutable(path) && Files.isRegularFile(path)))
                .filter(path -> (!path.toString().endsWith(".dylib") && !path.toString().contains("/Contents/MacOS/")))
                .forEachOrdered(path -> {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    if (toThrow.get() != null) return;

                    List<String> args = new ArrayList<>();
                    args.addAll(Arrays.asList("codesign",
                            "--deep",
//                            "--prefix", identifierPrefix, // use the identifier as a prefix
                            "-f")); // replace all existing signatures
                    if (signingIdentity != null) {
                        args.add("-s");
                        args.add(signingIdentity); // sign with this key
                    }
                    if (inheritedEntitlements != null) {
                        args.add("--entitlements");
                        args.add(inheritedEntitlements); // entitlements
                    }
                    args.add("-vvvv"); // super verbose output
                    args.add(path.toString()); // this is what we are signing

                    try {
                        Set<PosixFilePermission> oldPermissions = Files.getPosixFilePermissions(path);
                        File f = path.toFile();
                        f.setWritable(true, true);

                        ProcessBuilder pb = new ProcessBuilder(args);
                        IOUtils.exec(pb, VERBOSE.fetchFrom(params));

                        Files.setPosixFilePermissions(path, oldPermissions);
                    } catch (IOException e) {
                        toThrow.set(e);
                    }
                });

        ioe = toThrow.get();
        if (ioe != null) {
            throw ioe;
        }

        // sign all plugins and frameworks
        Consumer<? super Path> signIdentifiedByPList = path -> {
            //noinspection ThrowableResultOfMethodCallIgnored
            if (toThrow.get() != null) return;

            try {
                List<String> args = new ArrayList<>();
                args.addAll(Arrays.asList("codesign",
                        "-s", signingIdentity, // sign with this key
                        "-f", // replace all existing signatures
                        "--prefix", identifierPrefix, // use the identifier as a prefix
                        //"-i", bundleID, // sign the bundle's CFBundleIdentifier
                        "-vvvv"));
                if (signingIdentity != null) {
                    args.add("-s");
                    args.add(signingIdentity); // sign with this key
                }
                args.add(path.toString());
                ProcessBuilder pb = new ProcessBuilder(args);
                IOUtils.exec(pb, VERBOSE.fetchFrom(params));
            } catch (IOException e) {
                toThrow.set(e);
            }
        };

        Path pluginsPath = appLocation.toPath().resolve("Contents/PlugIns");
        if (Files.isDirectory(pluginsPath)) {
            Files.list(pluginsPath)
                    .forEach(signIdentifiedByPList);

            ioe = toThrow.get();
            if (ioe != null) {
                throw ioe;
            }
        }
        Path frameworkPath = appLocation.toPath().resolve("Contents/Frameworks");
        if (Files.isDirectory(frameworkPath)) {
            Files.list(frameworkPath)
                    .forEach(signIdentifiedByPList);

            ioe = toThrow.get();
            if (ioe != null) {
                throw ioe;
            }
        }

        // sign the app itself
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("codesign",
                "-s", signingIdentity, // sign with this key
                "--deep", // sign deeply, including plugins
                "-f")); // replace all existing signatures
        if (entitlementsFile != null) {
            args.add("--entitlements");
            args.add(entitlementsFile); // entitlements
        }
        args.add("-vvvv"); // super verbose output
        args.add(appLocation.toString());

        ProcessBuilder pb = new ProcessBuilder(args.toArray(new String[args.size()]));
        IOUtils.exec(pb, VERBOSE.fetchFrom(params));
    }

    @Override
    public Collection<BundlerParamInfo<?>> getBundleParameters() {
        Collection<BundlerParamInfo<?>> results = new LinkedHashSet<>();

        results.addAll(MacAppBundler.getAppBundleParameters());
        results.addAll(Arrays.asList(
                APP_BUNDLER,
                CONFIG_ROOT,
                APP_IMAGE_BUILD_ROOT,
                MAC_APP_IMAGE
        ));

        return results;
    }

    @Override
    public String getBundleType() {
        return "INSTALLER";
    }
    
    public static String findKey(String key, boolean verbose) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PrintStream ps = new PrintStream(baos)) {
            ProcessBuilder pb = new ProcessBuilder(
                    "security", "find-certificate", 
                    "-c", key, // look for key in common name
                    "-a" // return all matches, not just the first
            );

            IOUtils.exec(pb, verbose, false, ps);
            Pattern p = Pattern.compile("\"alis\"<blob>=\"([^\"]+)\"");
            System.out.println(baos.toString());
            Matcher m = p.matcher(baos.toString());
            if (!m.find()) {
                Log.info("Did not find a key matching '" + key + "'");
                return null;
            }
            String matchedKey = m.group(1);
            if (m.find()) {
                Log.info("Found more than one key matching '"  + key + "'");
                return null;
            }
            Log.debug("Using key '" + matchedKey + "'");
            return matchedKey;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            Log.verbose(ioe);
            return null;
        }
    }
}
