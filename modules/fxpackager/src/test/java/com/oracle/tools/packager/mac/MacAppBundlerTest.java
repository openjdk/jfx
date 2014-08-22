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
import com.oracle.tools.packager.IOUtils;
import com.oracle.tools.packager.Log;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.UnsupportedPlatformException;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
import static org.junit.Assert.*;

public class MacAppBundlerTest {

    static File tmpBase;
    static File workDir;
    static File appResourcesDir;
    static File fakeMainJar;
    static File hdpiIcon;
    static String runtimeJdk;
    static String runtimeJre;
    static Set<File> appResources;
    static boolean retain = false;

    @BeforeClass
    public static void prepareApp() {
        // only run on mac
        Assume.assumeTrue(System.getProperty("os.name").toLowerCase().contains("os x"));

        runtimeJdk = System.getenv("PACKAGER_JDK_ROOT");
        runtimeJre = System.getenv("PACKAGER_JRE_ROOT");

        // and only if we have the correct JRE settings
        String jre = System.getProperty("java.home").toLowerCase();
        Assume.assumeTrue(runtimeJdk != null || jre.endsWith("/contents/home/jre") || jre.endsWith("/contents/home/jre"));

        Log.setLogger(new Log.Logger(true));

        retain = Boolean.parseBoolean(System.getProperty("RETAIN_PACKAGER_TESTS"));

        workDir = new File("build/tmp/tests", "macapp");
        hdpiIcon = new File("build/tmp/tests", "GenericAppHiDPI.icns");
        appResourcesDir = new File("build/tmp/tests", "appResources");
        fakeMainJar = new File(appResourcesDir, "mainApp.jar");

        appResources = new HashSet<>(Arrays.asList(fakeMainJar));
    }

    @Before
    public void createTmpDir() throws IOException {
        if (retain) {
            tmpBase = new File("build/tmp/tests/macapp");
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
    public void testValidateVersion() {
        MacAppBundler b = new MacAppBundler();
        String validVersions[] = {"1", "255", "1.0", "1.0.0", "255.255.0", "255.255.6000"};
        String invalidVersions[] = {null, "alpha", "1.0-alpha", "0.300", "-300", "1.-1", "1.1.-1"};

        for(String v: validVersions) {
            assertTrue("Expect to be valid ["+v+"]",
                    MacAppBundler.validCFBundleVersion(v));
            try {
                Map<String, Object> params = new HashMap<>();
                params.put(BUILD_ROOT.getID(), tmpBase);
                params.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));

                if (runtimeJdk != null) {
                    params.put(MAC_RUNTIME.getID(), runtimeJdk);
                }

                params.put(VERSION.getID(), v);
                b.validate(params);
            } catch (ConfigException ce) {
                ce.printStackTrace();
                assertTrue("Expect to be valid via '" + VERSION.getID() + "' ["+v+"]",
                        false);
            } catch (UnsupportedPlatformException ignore) {
            }
            try {
                Map<String, Object> params = new HashMap<>();
                params.put(BUILD_ROOT.getID(), tmpBase);
                params.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));

                if (runtimeJdk != null) {
                    params.put(MAC_RUNTIME.getID(), runtimeJdk);
                }

                params.put(MAC_CF_BUNDLE_VERSION.getID(), v);
                b.validate(params);
            } catch (ConfigException ce) {
                assertTrue("Expect to be valid via '" + VERSION.getID() + "' ["+v+"]",
                        false);
            } catch (UnsupportedPlatformException ignore) {
            }
        }

        for(String v: invalidVersions) {
            assertFalse("Expect to be invalid ["+v+"]",
                    MacAppBundler.validCFBundleVersion(v));
            try {
                Map<String, Object> params = new HashMap<>();
                params.put(BUILD_ROOT.getID(), tmpBase);
                params.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));

                if (runtimeJdk != null) {
                    params.put(MAC_RUNTIME.getID(), runtimeJdk);
                }

                params.put(VERSION.getID(), v);
                b.validate(params);
                assertFalse("Invalid appVersion is not the mac.CFBundleVersion", MAC_CF_BUNDLE_VERSION.fetchFrom(params).equals(VERSION.fetchFrom(params)));
            } catch (ConfigException ce) {
                ce.printStackTrace();
                assertTrue("Expect to be ignored when invalid via '" + VERSION.getID() + "' ["+v+"]",
                        false);
            } catch (UnsupportedPlatformException ignore) {
            }
            try {
                Map<String, Object> params = new HashMap<>();
                params.put(BUILD_ROOT.getID(), tmpBase);
                params.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));

                if (runtimeJdk != null) {
                    params.put(MAC_RUNTIME.getID(), runtimeJdk);
                }

                params.put(MAC_CF_BUNDLE_VERSION.getID(), v);
                b.validate(params);
                assertTrue("Expect to be invalid via '" + VERSION.getID() + "' ["+v+"]",
                        false);
            } catch (ConfigException | UnsupportedPlatformException ignore) {
            }
        }
    }


    /**
     * See if smoke comes out
     */
    @Test
    public void smokeTest() throws IOException, ConfigException, UnsupportedPlatformException {
        AbstractBundler bundler = new MacAppBundler();

        assertNotNull(bundler.getName());
        assertNotNull(bundler.getID());
        assertNotNull(bundler.getDescription());
        //assertNotNull(bundler.getBundleParameters());

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);

        bundleParams.put(APP_NAME.getID(), "Smoke Test App");
        bundleParams.put(MAC_CF_BUNDLE_NAME.getID(), "Smoke");
        bundleParams.put(MAIN_CLASS.getID(), "hello.TestPackager");
        bundleParams.put(PREFERENCES_ID.getID(), "the/really/long/preferences/id");
        bundleParams.put(MAIN_JAR.getID(),
                new RelativeFileSet(fakeMainJar.getParentFile(),
                        new HashSet<>(Arrays.asList(fakeMainJar)))
        );
        bundleParams.put(CLASSPATH.getID(), fakeMainJar.toString());
        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        bundleParams.put(VERBOSE.getID(), true);
        bundleParams.put(DEVELOPER_ID_APP_SIGNING_KEY.getID(), null); // force no signing
        bundleParams.put(ICON.getID(), "java-logo2.gif"); // force no signing

        if (runtimeJdk != null) {
            bundleParams.put(MAC_RUNTIME.getID(), runtimeJdk);
        }

        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        File result = bundler.execute(bundleParams, new File(workDir, "smoke"));
        System.err.println("Bundle at - " + result);
        assertNotNull(result);
        assertTrue(result.exists());
    }

    /**
     * Build smoke test and mark it as quarantined, possibly signed
     */
    @Test
    public void quarantinedAppTest() throws IOException, ConfigException, UnsupportedPlatformException {
        AbstractBundler bundler = new MacAppBundler();

        assertNotNull(bundler.getName());
        assertNotNull(bundler.getID());
        assertNotNull(bundler.getDescription());
        //assertNotNull(bundler.getBundleParameters());

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);

        bundleParams.put(APP_NAME.getID(), "Quarantined Test App");
        bundleParams.put(MAC_CF_BUNDLE_NAME.getID(), "Quarantine");
        bundleParams.put(MAIN_CLASS.getID(), "hello.TestPackager");
        bundleParams.put(PREFERENCES_ID.getID(), "the/really/long/preferences/id");
        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        bundleParams.put(VERBOSE.getID(), true);

        if (runtimeJdk != null) {
            bundleParams.put(MAC_RUNTIME.getID(), runtimeJdk);
        }

        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        File result = bundler.execute(bundleParams, new File(workDir, "quarantine"));
        System.err.println("Bundle at - " + result);
        assertNotNull(result);
        assertTrue(result.exists());

        // mark it as though it's been downloaded
        ProcessBuilder pb = new ProcessBuilder(
                "xattr", "-w", "com.apple.quarantine",
                "0000;" + Long.toHexString(System.currentTimeMillis() / 1000L) + ";Java Unit Tests;|com.oracle.jvm.8u",
                result.toString());
        IOUtils.exec(pb, true);
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
        Bundler bundler = new MacAppBundler();

        Map<String, Object> bundleParams = new HashMap<>();

        // not part of the typical setup, for testing
        bundleParams.put(BUILD_ROOT.getID(), tmpBase);

        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));

        if (runtimeJdk != null) {
            bundleParams.put(MAC_RUNTIME.getID(), runtimeJdk);
        }

        File output = bundler.execute(bundleParams, new File(workDir, "BareMinimum"));
        System.err.println("Bundle at - " + output);
        assertNotNull(output);
        assertTrue(output.exists());
    }


    /**
     * Test a misconfiguration where the runtime is misconfigured.
     */
    @Test(expected = ConfigException.class)
    public void runtimeBad() throws IOException, ConfigException, UnsupportedPlatformException {
        Bundler bundler = new MacAppBundler();

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);
        bundleParams.put(VERBOSE.getID(), true);

        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        bundleParams.put(MAC_RUNTIME.getID(), APP_RESOURCES.fetchFrom(bundleParams));

        bundler.validate(bundleParams);
    }

    @Test
    public void configureEverything() throws Exception {
        AbstractBundler bundler = new MacAppBundler();
        Collection<BundlerParamInfo<?>> parameters = bundler.getBundleParameters();

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(APP_NAME.getID(), "Everything App Name");
        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        bundleParams.put(BUNDLE_ID_SIGNING_PREFIX.getID(), "everything.signing.prefix.");
        bundleParams.put(DEVELOPER_ID_APP_SIGNING_KEY.getID(), "Developer ID Application");
        bundleParams.put(ICON_ICNS.getID(), hdpiIcon);
        bundleParams.put(JVM_OPTIONS.getID(), "-Xms128M");
        bundleParams.put(JVM_PROPERTIES.getID(), "everything.jvm.property=everything.jvm.property.value");
        bundleParams.put(MAC_CATEGORY.getID(), "public.app-category.developer-tools");
        bundleParams.put(MAC_CF_BUNDLE_IDENTIFIER.getID(), "com.example.everything.cf-bundle-identifier");
        bundleParams.put(MAC_CF_BUNDLE_NAME.getID(), "Everything CF Bundle Name");
        bundleParams.put(MAC_CF_BUNDLE_VERSION.getID(), "8.2.0");
        bundleParams.put(MAC_RUNTIME.getID(), runtimeJdk == null ? System.getProperty("java.home") : runtimeJdk);
        bundleParams.put(MAIN_CLASS.getID(), "hello.TestPackager");
        bundleParams.put(MAIN_JAR.getID(), "mainApp.jar");
        bundleParams.put(CLASSPATH.getID(), "mainApp.jar");
        bundleParams.put(PREFERENCES_ID.getID(), "everything/preferences/id");
        bundleParams.put(USER_JVM_OPTIONS.getID(), "-Xmx=256M\n");
        bundleParams.put(VERSION.getID(), "1.2.3.4");

        // assert they are set
        for (BundlerParamInfo bi :parameters) {
            assertTrue("Bundle args should contain " + bi.getID(), bundleParams.containsKey(bi.getID()));
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

        // only run the bundle with full tests
        Assume.assumeTrue(Boolean.parseBoolean(System.getProperty("FULL_TEST")));

        // but first remove signing keys, test servers don't have these...
        bundleParams.remove(DEVELOPER_ID_APP_SIGNING_KEY.getID());

        File result = bundler.execute(bundleParams, new File(workDir, "everything"));
        System.err.println("Bundle at - " + result);
        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Ignore // this test is noisy and only valid for by-hand validation
    @Test
    public void jvmUserOptionsTest() throws IOException, ConfigException, UnsupportedPlatformException {

        for (String name : Arrays.asList("", "example", "com.example", "com.example.helloworld", "com.example.hello.world", "com.example.hello.world.app")) {

            AbstractBundler bundler = new MacAppBundler();

            Map<String, Object> bundleParams = new HashMap<>();

            bundleParams.put(BUILD_ROOT.getID(), tmpBase);

            bundleParams.put(APP_NAME.getID(), "User JVM Options App - " + name);
            bundleParams.put(MAC_CF_BUNDLE_NAME.getID(), name + ".application");
            bundleParams.put(MAIN_CLASS.getID(), "hello.TestPackager");
            bundleParams.put(IDENTIFIER.getID(), name);
            bundleParams.put(PREFERENCES_ID.getID(), name.replace(".", "/"));
            bundleParams.put(MAIN_JAR.getID(),
                    new RelativeFileSet(fakeMainJar.getParentFile(),
                            new HashSet<>(Arrays.asList(fakeMainJar)))
            );
            bundleParams.put(CLASSPATH.getID(), fakeMainJar.toString());
            bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
            bundleParams.put(VERBOSE.getID(), true);
            bundleParams.put(DEVELOPER_ID_APP_SIGNING_KEY.getID(), null); // force no signing

            if (runtimeJdk != null) {
                bundleParams.put(MAC_RUNTIME.getID(), runtimeJdk);
            }

            boolean valid = bundler.validate(bundleParams);
            assertTrue(valid);

            File result = bundler.execute(bundleParams, new File(workDir, "UserOpts-" + name.replace(".", "-")));
            System.err.println("Bundle at - " + result);
            assertNotNull(result);
            assertTrue(result.exists());
        }
    }


    /**
     * User a JRE instead of a JDK
     */
    @Test(expected = ConfigException.class)
    public void testJRE() throws IOException, ConfigException, UnsupportedPlatformException {
        String jre = runtimeJre == null ? "/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/" : runtimeJre;
        Assume.assumeTrue(new File(jre).isDirectory());

        Bundler bundler = new MacAppBundler();

        Map<String, Object> bundleParams = new HashMap<>();

        // not part of the typical setup, for testing
        bundleParams.put(BUILD_ROOT.getID(), tmpBase);

        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        bundleParams.put(MAC_RUNTIME.getID(), jre);

        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        File output = bundler.execute(bundleParams, new File(workDir, "JRETest"));
        System.err.println("Bundle at - " + output);
        assertNotNull(output);
        assertTrue(output.exists());
    }


}
