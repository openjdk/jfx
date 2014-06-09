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

import com.oracle.tools.packager.BundlerParamInfo;
import com.oracle.tools.packager.RelativeFileSet;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.tools.packager.StandardBundlerParam.*;
import static com.oracle.tools.packager.StandardBundlerParam.LICENSE_TYPE;
import static com.oracle.tools.packager.StandardBundlerParam.VENDOR;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.oracle.tools.packager.Bundler;
import com.oracle.tools.packager.Log;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.UnsupportedPlatformException;

public class WinServiceBundlerTest {
    
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

        workDir = new File("build/tmp/tests", "winservice");
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
            tmpBase = new File("build/tmp/tests/winservice");
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
        Bundler bundler = new WinServiceBundler();

        assertNotNull(bundler.getName());
        assertNotNull(bundler.getID());
        assertNotNull(bundler.getDescription());
        //assertNotNull(bundler.getBundleParameters());

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);
        bundleParams.put(APP_NAME.getID(), "Smoke Test");
        bundleParams.put(VERBOSE.getID(), true);

        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        File output = bundler.execute(bundleParams, new File(workDir, "smoke"));
        assertNotNull(output);
        
        // make sure that the service launcher is there
        File launcher = WinServiceBundler.getLauncherSvc(output, bundleParams);
        assertTrue(launcher.exists());
    }

    @Test
    public void winExeService() throws Exception {
        // only run if we have InnoSetup installed
        Assume.assumeNotNull(WinExeBundler.TOOL_INNO_SETUP_COMPILER_EXECUTABLE.fetchFrom(new HashMap<>()));

        Bundler bundler = new WinExeBundler();
        Collection<BundlerParamInfo<?>> parameters = bundler.getBundleParameters();

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(SERVICE_HINT.getID(), true);
        bundleParams.put(START_ON_INSTALL.getID(), true);
        bundleParams.put(STOP_ON_UNINSTALL.getID(), true);
        bundleParams.put(RUN_AT_STARTUP.getID(), true);

        bundleParams.put(APP_NAME.getID(), "Java Packager EXE Service Test #1");
        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        bundleParams.put(MAIN_CLASS.getID(), "hello.HelloService");
        bundleParams.put(MAIN_JAR.getID(), "mainApp.jar");
        bundleParams.put(CLASSPATH.getID(), "mainApp.jar");

        bundleParams.put(DESCRIPTION.getID(), "Does a random heart beat every 30 seconds or so to a log file in tmp");
        bundleParams.put(LICENSE_FILE.getID(), "LICENSE");
        bundleParams.put(LICENSE_TYPE.getID(), "GPL v2 + CLASSPATH");
        bundleParams.put(VENDOR.getID(), "OpenJDK");

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);
        bundleParams.put(VERBOSE.getID(), true);

        // assert it validates
        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        File result = bundler.execute(bundleParams, new File(workDir, "exeService"));
        System.err.println("Bundle at - " + result);
        assertNotNull(result);
        assertTrue(result.exists());
    }


    @Test
    public void winMsiService() throws Exception {
        // only run if we have Wix tools installed
        Assume.assumeNotNull(WinMsiBundler.TOOL_LIGHT_EXECUTABLE.fetchFrom(new HashMap<>()));
        Assume.assumeNotNull(WinMsiBundler.TOOL_CANDLE_EXECUTABLE.fetchFrom(new HashMap<>()));

        Bundler bundler = new WinMsiBundler();
        Collection<BundlerParamInfo<?>> parameters = bundler.getBundleParameters();

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(SERVICE_HINT.getID(), true);
        bundleParams.put(START_ON_INSTALL.getID(), true);
        bundleParams.put(STOP_ON_UNINSTALL.getID(), true);
        bundleParams.put(RUN_AT_STARTUP.getID(), true);

        bundleParams.put(APP_NAME.getID(), "Java Packager MSI Service Test #1");
        bundleParams.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));
        bundleParams.put(MAIN_CLASS.getID(), "hello.HelloService");
        bundleParams.put(MAIN_JAR.getID(), "mainApp.jar");
        bundleParams.put(CLASSPATH.getID(), "mainApp.jar");

        bundleParams.put(DESCRIPTION.getID(), "Does a random heart beat every 30 seconds or so to a log file in tmp");
        bundleParams.put(LICENSE_FILE.getID(), "LICENSE");
        bundleParams.put(LICENSE_TYPE.getID(), "GPL v2 + CLASSPATH");
        bundleParams.put(VENDOR.getID(), "OpenJDK");

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);
        bundleParams.put(VERBOSE.getID(), true);

        // assert it validates
        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        File result = bundler.execute(bundleParams, new File(workDir, "msiService"));
        System.err.println("Bundle at - " + result);
        assertNotNull(result);
        assertTrue(result.exists());
    }

}
