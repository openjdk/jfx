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
import com.oracle.tools.packager.Bundler;
import com.oracle.tools.packager.BundlerParamInfo;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.Log;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.UnsupportedPlatformException;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.oracle.tools.packager.StandardBundlerParam.*;
import static com.oracle.tools.packager.mac.MacAppBundler.*;
import static com.oracle.tools.packager.mac.MacDmgBundler.*;
import static com.oracle.tools.packager.mac.MacBaseInstallerBundler.MAC_APP_IMAGE;
import static org.junit.Assert.*;

public class MacDmgBundlerTest {

    static final int MIN_SIZE=0x100000; // 1MiB

    static File tmpBase;
    static File workDir;
    static File appResourcesDir;
    static File fakeMainJar;
    static File hdpiIcon;
    static String runtimeJdk;
    static Set<File> appResources;
    static boolean retain = false;
    static boolean full_tests = false;

    @BeforeClass
    public static void prepareApp() {
        // only run on mac
        Assume.assumeTrue(System.getProperty("os.name").toLowerCase().contains("os x"));

        runtimeJdk = System.getenv("PACKAGER_JDK_ROOT");

        // and only if we have the correct JRE settings
        String jre = System.getProperty("java.home").toLowerCase();
        Assume.assumeTrue(runtimeJdk != null || jre.endsWith("/contents/home/jre") || jre.endsWith("/contents/home/jre"));

        Log.setLogger(new Log.Logger(true));
        Log.setDebug(true);

        retain = Boolean.parseBoolean(System.getProperty("RETAIN_PACKAGER_TESTS"));
        full_tests = Boolean.parseBoolean(System.getProperty("FULL_TEST"));

        workDir = new File("build/tmp/tests", "macdmg");
        hdpiIcon = new File("build/tmp/tests", "GenericAppHiDPI.icns");
        appResourcesDir = new File("build/tmp/tests", "appResources");
        fakeMainJar = new File(appResourcesDir, "mainApp.jar");

        appResources = new HashSet<>(Arrays.asList(fakeMainJar,
                new File(appResourcesDir, "LICENSE"),
                new File(appResourcesDir, "LICENSE2")
        ));
    }

    @Before
    public void createTmpDir() throws IOException {
        if (retain) {
            tmpBase = new File("build/tmp/tests/macdmg");
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

    /**
     * See if smoke comes out
     */
    @Test
    public void smokeTest() throws IOException, ConfigException, UnsupportedPlatformException {
        // only run with full tests
        Assume.assumeTrue(full_tests);

        AbstractBundler bundler = new MacDmgBundler();

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
        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        bundleParams.put(LICENSE_FILE.getID(), Arrays.asList("LICENSE", "LICENSE2"));
        bundleParams.put(VERBOSE.getID(), true);
        bundleParams.put(SYSTEM_WIDE.getID(), false);

        if (runtimeJdk != null) {
            bundleParams.put(MAC_RUNTIME.getID(), runtimeJdk);
        }

        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        File result = bundler.execute(bundleParams, new File(workDir, "smoke"));
        System.err.println("Bundle at - " + result);
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.length() > MIN_SIZE);
    }

    /**
     * The bare minimum configuration needed to make it work
     * <ul>
     *     <li>Where to build it</li>
     *     <li>The jar containing the application (with a main-class attribute)</li>
     * </ul>
     *
     * All other values will be driven off of those two values.
     */
    @Test
    public void minimumConfig() throws IOException, ConfigException, UnsupportedPlatformException {
        // only run with full tests
        Assume.assumeTrue(full_tests);

        Bundler bundler = new MacDmgBundler();

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);

        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));

        if (runtimeJdk != null) {
            bundleParams.put(MAC_RUNTIME.getID(), runtimeJdk);
        }

        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        File output = bundler.execute(bundleParams, new File(workDir, "BareMinimum"));
        System.err.println("Bundle at - " + output);
        assertNotNull(output);
        assertTrue(output.exists());
        assertTrue(output.length() > MIN_SIZE);
    }

    /**
     * Create a DMG with an external app rather than a self-created one.
     */
    @Test
    public void externalApp() throws IOException, ConfigException, UnsupportedPlatformException {
        // only run with full tests
        Assume.assumeTrue(full_tests);

        // first create the external app
        Bundler appBundler = new MacAppBundler();

        Map<String, Object> appBundleParams = new HashMap<>();

        appBundleParams.put(BUILD_ROOT.getID(), tmpBase);

        appBundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        appBundleParams.put(APP_NAME.getID(), "External APP DMG Test");
        appBundleParams.put(IDENTIFIER.getID(), "com.example.dmg.external");
        appBundleParams.put(VERBOSE.getID(), true);

        if (runtimeJdk != null) {
            appBundleParams.put(MAC_RUNTIME.getID(), runtimeJdk);
        }

        boolean valid = appBundler.validate(appBundleParams);
        assertTrue(valid);

        File appOutput = appBundler.execute(appBundleParams, new File(workDir, "DMGExternalApp1"));
        System.err.println("App at - " + appOutput);
        assertNotNull(appOutput);
        assertTrue(appOutput.exists());

        // now create the DMG referencing this external app
        Bundler dmgBundler = new MacDmgBundler();

        Map<String, Object> dmgBundleParams = new HashMap<>();

        dmgBundleParams.put(BUILD_ROOT.getID(), tmpBase);

        dmgBundleParams.put(MAC_APP_IMAGE.getID(), appOutput);
        dmgBundleParams.put(APP_NAME.getID(), "External APP DMG Test");
        dmgBundleParams.put(IDENTIFIER.getID(), "com.example.dmg.external");

        dmgBundleParams.put(VERBOSE.getID(), true);

        if (runtimeJdk != null) {
            dmgBundleParams.put(MAC_RUNTIME.getID(), runtimeJdk);
        }

        valid = dmgBundler.validate(dmgBundleParams);
        assertTrue(valid);

        File dmgOutput = dmgBundler.execute(dmgBundleParams, new File(workDir, "DMGExternalApp2"));
        System.err.println(".dmg at - " + dmgOutput);
        assertNotNull(dmgOutput);
        assertTrue(dmgOutput.exists());
        assertTrue(dmgOutput.length() > MIN_SIZE);
    }

    /**
     * Create a DMG with an external app rather than a self-created one.
     */
    @Test
    public void externalSimpleApp() throws IOException, ConfigException, UnsupportedPlatformException {
        // first create the external app
        Bundler appBundler = new MacAppBundler();

        Map<String, Object> appBundleParams = new HashMap<>();

        appBundleParams.put(BUILD_ROOT.getID(), tmpBase);

        appBundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        appBundleParams.put(APP_NAME.getID(), "External APP DMG Test");
        appBundleParams.put(IDENTIFIER.getID(), "com.example.dmg.external");
        appBundleParams.put(VERBOSE.getID(), true);

        if (runtimeJdk != null) {
            appBundleParams.put(MAC_RUNTIME.getID(), runtimeJdk);
        }

        boolean valid = appBundler.validate(appBundleParams);
        assertTrue(valid);

        File appOutput = appBundler.execute(appBundleParams, new File(workDir, "DMGExternalApp1"));
        System.err.println("App at - " + appOutput);
        assertNotNull(appOutput);
        assertTrue(appOutput.exists());

        // now create the DMG referencing this external app
        Bundler dmgBundler = new MacDmgBundler();

        Map<String, Object> dmgBundleParams = new HashMap<>();

        dmgBundleParams.put(BUILD_ROOT.getID(), tmpBase);
        dmgBundleParams.put(SIMPLE_DMG.getID(), true);

        dmgBundleParams.put(MAC_APP_IMAGE.getID(), appOutput);
        dmgBundleParams.put(APP_NAME.getID(), "External APP DMG Test");
        dmgBundleParams.put(IDENTIFIER.getID(), "com.example.dmg.external");

        dmgBundleParams.put(VERBOSE.getID(), true);

        if (runtimeJdk != null) {
            dmgBundleParams.put(MAC_RUNTIME.getID(), runtimeJdk);
        }

        valid = dmgBundler.validate(dmgBundleParams);
        assertTrue(valid);

        File dmgOutput = dmgBundler.execute(dmgBundleParams, new File(workDir, "DMGExternalApp3"));
        System.err.println(".dmg at - " + dmgOutput);
        assertNotNull(dmgOutput);
        assertTrue(dmgOutput.exists());
        assertTrue(dmgOutput.length() > MIN_SIZE);
    }

    @Test(expected = ConfigException.class)
    public void externanNoAppName() throws ConfigException, UnsupportedPlatformException {
        Bundler dmgBundler = new MacDmgBundler();

        Map<String, Object> dmgBundleParams = new HashMap<>();

        dmgBundleParams.put(BUILD_ROOT.getID(), tmpBase);

        dmgBundleParams.put(MAC_APP_IMAGE.getID(), ".");
        dmgBundleParams.put(IDENTIFIER.getID(), "net.example.bogus");
        dmgBundleParams.put(VERBOSE.getID(), true);

        dmgBundler.validate(dmgBundleParams);
    }

    @Test(expected = ConfigException.class)
    public void externanNoID() throws ConfigException, UnsupportedPlatformException {
        Bundler dmgBundler = new MacDmgBundler();

        Map<String, Object> dmgBundleParams = new HashMap<>();

        dmgBundleParams.put(BUILD_ROOT.getID(), tmpBase);

        dmgBundleParams.put(MAC_APP_IMAGE.getID(), ".");
        dmgBundleParams.put(APP_NAME.getID(), "Bogus App");
        dmgBundleParams.put(VERBOSE.getID(), true);

        dmgBundler.validate(dmgBundleParams);
    }

    @Test(expected = ConfigException.class)
    public void invalidLicenseFile() throws ConfigException, UnsupportedPlatformException {
        Bundler bundler = new MacDmgBundler();

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);

        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        bundleParams.put(LICENSE_FILE.getID(), "BOGUS_LICENSE");

        bundler.validate(bundleParams);
    }

    @Test
    public void configureEverything() throws Exception {
        AbstractBundler bundler = new MacDmgBundler();
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
        bundleParams.put(MAC_RUNTIME.getID(), runtimeJdk == null ? System.getProperty("java.home") : runtimeJdk);
        bundleParams.put(MAIN_CLASS.getID(), "hello.TestPackager");
        bundleParams.put(MAIN_JAR.getID(), "mainApp.jar");
        bundleParams.put(CLASSPATH.getID(), "mainApp.jar");
        bundleParams.put(PREFERENCES_ID.getID(), "everything/preferences/id");
        bundleParams.put(USER_JVM_OPTIONS.getID(), "-Xmx=256M\n");
        bundleParams.put(VERSION.getID(), "1.2.3.4");

        bundleParams.put(LICENSE_FILE.getID(), "LICENSE");
        bundleParams.put(SIMPLE_DMG.getID(), true);
        bundleParams.put(SYSTEM_WIDE.getID(), true);

        // assert they are set
        for (BundlerParamInfo bi :parameters) {
            assertNotNull("Bundle args Contains " + bi.getID(), bundleParams.containsKey(bi.getID()));
        }

        // and only those are set
        bundleParamLoop:
        for (String s :bundleParams.keySet()) {
            for (BundlerParamInfo<?> bpi : parameters) {
                if (s.equals(bpi.getID())) {
                    continue bundleParamLoop;
                }
            }
            fail("Enumerated parameters does not contain " + s);
        }

        // assert they resolve
        for (BundlerParamInfo bi :parameters) {
            bi.fetchFrom(bundleParams);
        }

        // add verbose now that we are done scoping out parameters
        bundleParams.put(BUILD_ROOT.getID(), tmpBase);
        bundleParams.put(VERBOSE.getID(), true);

        // assert it validates
        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        File result = bundler.execute(bundleParams, new File(workDir, "everything"));
        System.err.println("Bundle at - " + result);
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.length() > MIN_SIZE);
    }
}
