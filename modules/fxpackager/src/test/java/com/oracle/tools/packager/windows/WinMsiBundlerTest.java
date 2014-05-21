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

package com.oracle.tools.packager.windows;

import com.oracle.tools.packager.Bundler;
import com.oracle.tools.packager.Log;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.UnsupportedPlatformException;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static com.oracle.tools.packager.StandardBundlerParam.*;
import static org.junit.Assert.*;

public class WinMsiBundlerTest {

    static File tmpBase;
    static File workDir;
    static File appResourcesDir;
    static File fakeMainJar;
    static Set<File> appResources;
    static boolean retain = false;

    @BeforeClass
    public static void prepareApp() {
        // only run on windows
        Assume.assumeTrue(System.getProperty("os.name").toLowerCase().startsWith("win"));

        // only run if we have Wix tools installed
        Assume.assumeNotNull(WinMsiBundler.TOOL_LIGHT_EXECUTABLE.fetchFrom(new HashMap<>()));
        Assume.assumeNotNull(WinMsiBundler.TOOL_CANDLE_EXECUTABLE.fetchFrom(new HashMap<>()));

        Log.setLogger(new Log.Logger(true));

        retain = Boolean.parseBoolean(System.getProperty("RETAIN_PACKAGER_TESTS"));

        workDir = new File("build/tmp/tests", "winmsi");
        appResourcesDir = new File("build/tmp/tests", "appResources");
        fakeMainJar = new File(appResourcesDir, "mainApp.jar");

        appResources = new HashSet<>(Arrays.asList(fakeMainJar));
    }

    @Before
    public void createTmpDir() throws IOException {
        if (retain) {
            tmpBase = new File("build/tmp/tests/winmsi");
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

    /**
     * See if smoke comes out
     */
    @Test
    public void smokeTest() throws IOException, ConfigException, UnsupportedPlatformException {
        Bundler bundler = new WinMsiBundler();

        assertNotNull(bundler.getName());
        assertNotNull(bundler.getID());
        assertNotNull(bundler.getDescription());
        //assertNotNull(bundler.getBundleParameters());

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
        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        bundleParams.put(VERBOSE.getID(), true);

        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        File result = bundler.execute(bundleParams, new File(workDir, "smoke"));
        System.err.println("Bundle at - " + result);
        assertNotNull(result);
        assertTrue(result.exists());
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
        Bundler bundler = new WinMsiBundler();

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);

        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));

        File output = bundler.execute(bundleParams, new File(workDir, "BareMinimum"));
        System.err.println("Bundle at - " + output);
        assertNotNull(output);
        assertTrue(output.exists());
    }

    /**
     * Test a misconfiguration where the wix tools are misconfigured.
     */
    @Test
    public void configLightBad() throws IOException, ConfigException, UnsupportedPlatformException {
        Bundler bundler = new WinMsiBundler();

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);
        bundleParams.put(VERBOSE.getID(), true);

        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));

        bundleParams.put(WinMsiBundler.TOOL_LIGHT_EXECUTABLE.getID(), "c:\\MissingTool.exe");

        File output = bundler.execute(bundleParams, new File(workDir, "BadLight"));
        assertNull(output);
    }

    /**
     * Test a misconfiguration where the wix tools are misconfigured.
     */
    @Test
    public void configLightNull() throws IOException, ConfigException, UnsupportedPlatformException {
        Bundler bundler = new WinMsiBundler();

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);
        bundleParams.put(VERBOSE.getID(), true);

        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));

        bundleParams.put(WinMsiBundler.TOOL_LIGHT_EXECUTABLE.getID(), null);

        File output = bundler.execute(bundleParams, new File(workDir, "NullLight"));
        assertNull(output);
    }

    /**
     * Test a misconfiguration where the Wix tools are misconfigured.
     */
    @Test
    public void configCandleBad() throws IOException, ConfigException, UnsupportedPlatformException {
        Bundler bundler = new WinMsiBundler();

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);
        bundleParams.put(VERBOSE.getID(), true);

        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));

        bundleParams.put(WinMsiBundler.TOOL_CANDLE_EXECUTABLE.getID(), "c:\\MissingTool.exe");

        File output = bundler.execute(bundleParams, new File(workDir, "BadCandle"));
        assertNull(output);
    }

    /**
     * Test a misconfiguration where the wix tools are misconfigured.
     */
    @Test
    public void configCandleNull() throws IOException, ConfigException, UnsupportedPlatformException {
        Bundler bundler = new WinMsiBundler();

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);
        bundleParams.put(VERBOSE.getID(), true);

        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));

        bundleParams.put(WinMsiBundler.TOOL_CANDLE_EXECUTABLE.getID(), null);

        File output = bundler.execute(bundleParams, new File(workDir, "NullCandle"));
        assertNull(output);
    }

    @Test
    public void testValidateVersion() {
        String validVersions[] = {null, "1", "255", "1.0", "255.255.0", "255.255.6000"};
        String invalidVersions[] = {"alpha", "1.0-alpha", "10.300", "300", "10.10.100000"};

        for(String v: validVersions) {
            assertTrue("Expect to be valid ["+v+"]",
                    com.oracle.tools.packager.windows.WinMsiBundler.isVersionStringValid(v));
        }

        for(String v: invalidVersions) {
            assertFalse("Expect to be invalid ["+v+"]",
                    com.oracle.tools.packager.windows.WinMsiBundler.isVersionStringValid(v));
        }
    }

}
