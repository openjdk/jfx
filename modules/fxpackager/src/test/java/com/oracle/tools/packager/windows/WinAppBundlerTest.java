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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import static com.oracle.tools.packager.StandardBundlerParam.*;
import static com.oracle.tools.packager.windows.WinAppBundler.ICON_ICO;
import static com.oracle.tools.packager.windows.WindowsBundlerParam.WIN_RUNTIME;
import static org.junit.Assert.*;

public class WinAppBundlerTest {

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

        Log.setLogger(new Log.Logger(true));

        retain = Boolean.parseBoolean(System.getProperty("RETAIN_PACKAGER_TESTS"));

        workDir = new File("build/tmp/tests", "winapp");
        appResourcesDir = new File("build/tmp/tests", "appResources");
        fakeMainJar = new File(appResourcesDir, "mainApp.jar");

        appResources = new HashSet<>(Arrays.asList(fakeMainJar));
    }

    @Before
    public void createTmpDir() throws IOException {
        if (retain) {
            tmpBase = new File("build/tmp/tests/winapp");
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
        Bundler bundler = new WinAppBundler();

        assertNotNull(bundler.getName());
        assertNotNull(bundler.getID());
        assertNotNull(bundler.getDescription());
        //assertNotNull(bundler.getBundleParameters());

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);

        bundleParams.put(APP_NAME.getID(), "Smoke Test");
        bundleParams.put(MAIN_CLASS.getID(), "hello.TestPackager");
        bundleParams.put(PREFERENCES_ID.getID(), "the/really/long/preferences/id");
        bundleParams.put(MAIN_JAR.getID(),
                new RelativeFileSet(fakeMainJar.getParentFile(),
                        new HashSet<>(Arrays.asList(fakeMainJar)))
        );
        bundleParams.put(MAIN_JAR_CLASSPATH.getID(), fakeMainJar.toString());
        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        bundleParams.put(VERBOSE.getID(), true);
        bundleParams.put(ICON.getID(), "java-logo2.gif"); // force no signing

        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        File output = bundler.execute(bundleParams, new File(workDir, "smoke"));
        validatePackageCfg(output);
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
        Bundler bundler = new WinAppBundler();

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);

        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));

        File output = bundler.execute(bundleParams, new File(workDir, "BareMinimum"));
        validatePackageCfg(output);
        assertTrue(output.isDirectory());
    }
    
    public void validatePackageCfg(File root) throws IOException {
        try (FileInputStream fis = new FileInputStream(new File(root, "app\\package.cfg"))) {
            Properties p = new Properties();
            p.load(fis);
            
            // - verify we have app.mainjar, app.version, app.id, app.preferences, app.mainclass, and app.classpath
            assertNotNull(p.getProperty("app.mainjar"));
            assertNotNull(p.getProperty("app.version"));
            assertNotNull(p.getProperty("app.id"));
            assertNotNull(p.getProperty("app.preferences.id"));
            assertNotNull(p.getProperty("app.mainclass"));
            assertNotNull(p.getProperty("app.classpath"));

            // - make sure 'app.classpath=null' doesn't show up, prefer 'app.classpath='
            assertFalse(p.getProperty("app.classpath").equals("null"));
        }
    }

    @Test
    public void configureEverything() throws Exception {
        AbstractBundler bundler = new WinAppBundler();
        Collection<BundlerParamInfo<?>> parameters = bundler.getBundleParameters();

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(APP_NAME.getID(), "Everything App Name");
        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        bundleParams.put(ICON_ICO.getID(), new File(appResourcesDir, "javalogo_white_48.ico"));
        bundleParams.put(JVM_OPTIONS.getID(), "-Xms128M");
        bundleParams.put(JVM_PROPERTIES.getID(), "everything.jvm.property=everything.jvm.property.value");
        bundleParams.put(MAIN_CLASS.getID(), "hello.TestPackager");
        bundleParams.put(MAIN_JAR.getID(), "mainApp.jar");
        bundleParams.put(MAIN_JAR_CLASSPATH.getID(), "mainApp.jar");
        bundleParams.put(PREFERENCES_ID.getID(), "everything.preferences.id");
        bundleParams.put(USER_JVM_OPTIONS.getID(), "-Xmx=256M\n");
        bundleParams.put(VERSION.getID(), "1.2.3.4");
        bundleParams.put(WIN_RUNTIME.getID(), System.getProperty("java.home"));

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

        File result = bundler.execute(bundleParams, new File(workDir, "everything"));
        System.err.println("Bundle at - " + result);
        assertNotNull(result);
        assertTrue(result.exists());
    }
}
