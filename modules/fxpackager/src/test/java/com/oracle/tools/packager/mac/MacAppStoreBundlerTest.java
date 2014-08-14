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
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.IOUtils;
import com.oracle.tools.packager.Log;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.UnsupportedPlatformException;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.oracle.tools.packager.StandardBundlerParam.*;
import static com.oracle.tools.packager.mac.MacAppBundler.*;
import static com.oracle.tools.packager.mac.MacAppStoreBundler.*;
import static org.junit.Assert.*;

public class MacAppStoreBundlerTest {

    static final int MIN_SIZE = 0x100000; // 1MiB

    static File tmpBase;
    static File workDir;
    static File appResourcesDir;
    static File fakeMainJar;
    static File hdpiIcon;
    static Set<File> appResources;
    static boolean retain = false;

    @BeforeClass
    public static void prepareApp() throws IOException {
        // only run on mac
        Assume.assumeTrue(System.getProperty("os.name").toLowerCase().contains("os x"));

        // and only if we have the correct JRE settings
        String jre = System.getProperty("java.home").toLowerCase();
        Assume.assumeTrue(jre.endsWith("/contents/home/jre") || jre.endsWith("/contents/home/jre"));

        // make sure we have a default signing key
        String signingKeyName = MacAppStoreBundler.MAC_APP_STORE_APP_SIGNING_KEY.fetchFrom(new TreeMap<>());
        Assume.assumeNotNull(signingKeyName);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PrintStream ps = new PrintStream(baos)) {
            System.err.println("Checking for valid certificate");
            ProcessBuilder pb = new ProcessBuilder(
                    "security",
                    "find-certificate", "-c", signingKeyName);

            IOUtils.exec(pb, Log.isDebug(), false, ps);

            String commandOutput = baos.toString();
            Assume.assumeTrue(commandOutput.contains(signingKeyName));
            System.err.println("Valid certificate present");
        } catch (Throwable t) {
            System.err.println("Valid certificate not present, skipping test.");
            Assume.assumeTrue(false);
        }


        Log.setLogger(new Log.Logger(true));

        retain = Boolean.parseBoolean(System.getProperty("RETAIN_PACKAGER_TESTS"));

        workDir = new File("build/tmp/tests", "macappstore");
        hdpiIcon = new File("build/tmp/tests", "GenericAppHiDPI.icns");
        appResourcesDir = new File("build/tmp/tests", "appResources");
        fakeMainJar = new File(appResourcesDir, "mainApp.jar");

        appResources = new HashSet<>(Arrays.asList(fakeMainJar));
    }

    @Before
    public void createTmpDir() throws IOException {
        if (retain) {
            tmpBase = new File("build/tmp/tests/macappstore");
        } else {
            tmpBase = BUILD_ROOT.fetchFrom(new TreeMap<>());
        }
        tmpBase.mkdir();
    }

    @After
    public void maybeCleanupTmpDir() {
        if (!retain) {
            attemptDelete(tmpBase);
        }
    }

    private void attemptDelete(File tmpBase) {
        if (tmpBase.isDirectory()) {
            File[] children = tmpBase.listFiles();
            if (children != null) {
                for (File f : children) {
                    attemptDelete(f);
                }
            }
        }
        boolean success;
        try {
            success = !tmpBase.exists() || tmpBase.delete();
        } catch (SecurityException se) {
            success = false;
        }
        if (!success) {
            System.err.println("Could not clean up " + tmpBase.toString());
        }
    }

    @Test
    public void showSigningKeyNames() {
        System.err.println(MacBaseInstallerBundler.SIGNING_KEY_USER.fetchFrom(new TreeMap<>()));
        System.err.println(MacAppStoreBundler.MAC_APP_STORE_APP_SIGNING_KEY.fetchFrom(new TreeMap<>()));
    }

    /**
     * See if smoke comes out
     */
    @Test
    public void smokeTest() throws IOException, ConfigException, UnsupportedPlatformException {
        AbstractBundler bundler = new MacAppStoreBundler();

        assertNotNull(bundler.getName());
        assertNotNull(bundler.getID());
        assertNotNull(bundler.getDescription());

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);

        bundleParams.put(APP_NAME.getID(), "Smoke Test");
        bundleParams.put(MAIN_CLASS.getID(), "hello.TestPackager");
        bundleParams.put(PREFERENCES_ID.getID(), "the/really/long/preferences/id");
        bundleParams.put(MAIN_JAR.getID(),
                new RelativeFileSet(fakeMainJar.getParentFile(),
                        new HashSet<>(Arrays.asList(fakeMainJar)))
        );
        bundleParams.put(CLASSPATH.getID(), fakeMainJar.toString());
        bundleParams.put(IDENTIFIER.getID(), "com.example.javapacakger.hello.TestPackager");
        bundleParams.put(MacAppBundler.MAC_CATEGORY.getID(), "public.app-category.developer-tools");
        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        bundleParams.put(VERBOSE.getID(), true);

        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        File result = bundler.execute(bundleParams, new File(workDir, "smoke"));
        System.err.println("Bundle at - " + result);

        checkFiles(result);
    }

    private void checkFiles(File result) throws IOException {
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.length() > MIN_SIZE);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(baos, true);
        IOUtils.exec(
                new ProcessBuilder("pkgutil", "--payload-files", result.getCanonicalPath()),
                false, false, printStream);

        String output = baos.toString();

        Pattern jreInfoPListPattern = Pattern.compile("/PlugIns/[^/]+/Contents/Info\\.plist");
        Matcher matcher = jreInfoPListPattern.matcher(output);
        assertTrue("Insure that info.plist is packed in for embedded jre", matcher.find());

        assertFalse("Insure JFX Media isn't packed in", output.contains("/libjfxmedia_qtkit.dylib"));
    }

    @Test
    public void configureEverything() throws Exception {
        AbstractBundler bundler = new MacAppStoreBundler();
        Collection<BundlerParamInfo<?>> parameters = bundler.getBundleParameters();

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(APP_NAME.getID(), "Everything App Name");
        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        bundleParams.put(BUNDLE_ID_SIGNING_PREFIX.getID(), "everything.signing.prefix.");
        bundleParams.put(ICON_ICNS.getID(), hdpiIcon);
        bundleParams.put(JVM_OPTIONS.getID(), "-Xms128M");
        bundleParams.put(JVM_PROPERTIES.getID(), "everything.jvm.property=everything.jvm.property.value");
        bundleParams.put(MAC_CATEGORY.getID(), "public.app-category.developer-tools");
        bundleParams.put(MAC_CF_BUNDLE_IDENTIFIER.getID(), "com.example.everything.cf-bundle-identifier");
        bundleParams.put(MAC_CF_BUNDLE_NAME.getID(), "Everything CF Bundle Name");
        bundleParams.put(MAC_CF_BUNDLE_VERSION.getID(), "8.2.0");
        bundleParams.put(MAC_RUNTIME.getID(), System.getProperty("java.home"));
        bundleParams.put(MAIN_CLASS.getID(), "hello.TestPackager");
        bundleParams.put(MAIN_JAR.getID(), "mainApp.jar");
        bundleParams.put(CLASSPATH.getID(), "mainApp.jar");
        bundleParams.put(PREFERENCES_ID.getID(), "everything/preferences/id");
        bundleParams.put(USER_JVM_OPTIONS.getID(), "-Xmx=256M\n");
        bundleParams.put(VERSION.getID(), "1.2.3.4");

        bundleParams.put(MAC_APP_STORE_APP_SIGNING_KEY.getID(), "3rd Party Mac Developer Application");
        bundleParams.put(MAC_APP_STORE_ENTITLEMENTS.getID(), null);
        bundleParams.put(MAC_APP_STORE_PKG_SIGNING_KEY.getID(), "3rd Party Mac Developer Installer");

        // assert they are set
        for (BundlerParamInfo bi : parameters) {
            assertNotNull("Bundle args Contains " + bi.getID(), bundleParams.containsKey(bi.getID()));
        }

        // and only those are set
        bundleParamLoop:
        for (String s : bundleParams.keySet()) {
            for (BundlerParamInfo<?> bpi : parameters) {
                if (s.equals(bpi.getID())) {
                    continue bundleParamLoop;
                }
            }
            fail("Enumerated parameters does not contain " + s);
        }

        // assert they resolve
        for (BundlerParamInfo bi : parameters) {
            bi.fetchFrom(bundleParams);
        }

        // now that we are done scoping out parameters add more esoteric values
        bundleParams.put(BUILD_ROOT.getID(), tmpBase);
        bundleParams.put(VERBOSE.getID(), true);

        // assert it validates
        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        // only run the bundle with full tests
        Assume.assumeTrue(Boolean.parseBoolean(System.getProperty("FULL_TEST")));

        File result = bundler.execute(bundleParams, new File(workDir, "everything"));
        System.err.println("Bundle at - " + result);

        checkFiles(result);
    }

    /**
     * User a JRE instead of a JDK
     */
    @Test
    public void testJRE() throws IOException, ConfigException, UnsupportedPlatformException {

        Assume.assumeTrue(new File("/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/").isDirectory());

        AbstractBundler bundler = new MacAppStoreBundler();

        assertNotNull(bundler.getName());
        assertNotNull(bundler.getID());
        assertNotNull(bundler.getDescription());

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);

        bundleParams.put(APP_NAME.getID(), "Smoke Test");
        bundleParams.put(MAIN_CLASS.getID(), "hello.TestPackager");
        bundleParams.put(PREFERENCES_ID.getID(), "the/really/long/preferences/id");
        bundleParams.put(MAIN_JAR.getID(),
                new RelativeFileSet(fakeMainJar.getParentFile(),
                        new HashSet<>(Arrays.asList(fakeMainJar)))
        );
        bundleParams.put(CLASSPATH.getID(), fakeMainJar.toString());
        bundleParams.put(IDENTIFIER.getID(), "com.example.javapacakger.hello.TestPackager");
        bundleParams.put(MacAppBundler.MAC_CATEGORY.getID(), "public.app-category.developer-tools");
        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        bundleParams.put(VERBOSE.getID(), true);

        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        File result = bundler.execute(bundleParams, new File(workDir, "jre"));
        System.err.println("Bundle at - " + result);

        checkFiles(result);
    }
}
