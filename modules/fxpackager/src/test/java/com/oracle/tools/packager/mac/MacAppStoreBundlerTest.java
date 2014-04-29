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

import com.oracle.tools.packager.*;
import com.oracle.tools.packager.IOUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.*;

import static com.oracle.tools.packager.StandardBundlerParam.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MacAppStoreBundlerTest {

    static File tmpBase;
    static File workDir;
    static File appResourcesDir;
    static File fakeMainJar;
    static Set<File> appResources;
    static boolean retain = false;

    @BeforeClass
    public static void prepareApp() throws IOException {
        // only run on mac
        Assume.assumeTrue(System.getProperty("os.name").toLowerCase().contains("os x"));

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
        appResourcesDir = new File("build/tmp/tests", "appResources");
        fakeMainJar = new File(appResourcesDir, "mainApp.jar");

        appResources = new HashSet<>(Arrays.asList(fakeMainJar));
    }

    @Before
    public void createTmpDir() throws IOException {
        if (retain) {
            tmpBase = new File("build/tmp/tests/macappstore");
        } else {
            tmpBase = Files.createTempDirectory("fxpackagertests").toFile();
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

        bundleParams.put(APP_NAME.getID(), "Smoke");
        bundleParams.put(MAIN_CLASS.getID(), "hello.TestPackager");
        bundleParams.put(PREFERENCES_ID.getID(), "the/really/long/preferences/id");
        bundleParams.put(MAIN_JAR.getID(),
                new RelativeFileSet(fakeMainJar.getParentFile(),
                        new HashSet<>(Arrays.asList(fakeMainJar)))
        );
        bundleParams.put(MAIN_JAR_CLASSPATH.getID(), fakeMainJar.toString());
        bundleParams.put(IDENTIFIER.getID(), "com.example.javapacakger.hello.TestPackager");
        bundleParams.put(MacAppBundler.MAC_CATEGORY.getID(), "public.app-category.developer-tools");
        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        bundleParams.put(VERBOSE.getID(), true);

        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        File result = bundler.execute(bundleParams, new File(workDir, "smoke"));
        System.err.println("Bundle at - " + result);
        assertNotNull(result);
        assertTrue(result.exists());
    }

//    /**
//     * The bare minimum configuration needed to make it work
//     * <ul>
//     *     <li>Where to build it</li>
//     *     <li>The jar containing the application (with a main-class attribute)</li>
//     * </ul>
//     *
//     * All other values will be driven off of those two values.
//     */
//    @Test
//    public void minimumConfig() throws IOException, ConfigException, UnsupportedPlatformException {
//        Bundler bundler = new MacAppStoreBundler();
//
//        Map<String, Object> bundleParams = new HashMap<>();
//
//        bundleParams.put(StandardBundlerParam.BUILD_ROOT.getID(), tmpBase);
//
//        bundleParams.put(StandardBundlerParam.APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
//
//        File output = bundler.execute(bundleParams, new File(workDir, "BareMinimum"));
//        System.err.println("Bundle written to " + output);
//        assertTrue(output.isFile());
//        //TODO assert file name
//    }
}
