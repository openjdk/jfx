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

package com.oracle.bundlers.windows;

import static com.oracle.bundlers.StandardBundlerParam.APP_NAME;
import static com.oracle.bundlers.StandardBundlerParam.APP_RESOURCES;
import static com.oracle.bundlers.StandardBundlerParam.BUILD_ROOT;
import static com.oracle.bundlers.StandardBundlerParam.MAIN_CLASS;
import static com.oracle.bundlers.StandardBundlerParam.VERBOSE;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.oracle.bundlers.Bundler;
import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.packager.bundlers.ConfigException;
import com.sun.javafx.tools.packager.bundlers.RelativeFileSet;
import com.sun.javafx.tools.packager.bundlers.UnsupportedPlatformException;
import com.sun.javafx.tools.packager.bundlers.WinAppBundler;
import com.sun.javafx.tools.packager.bundlers.WinServiceBundler;

public class WinServiceBundlerTest {
    
    static File tmpBase;
    static File workDir;
    static boolean retain = false;
    
    @BeforeClass
    public static void prepareApp() {
        // only run on windows
        Assume.assumeTrue(System.getProperty("os.name").toLowerCase().startsWith("win"));

        Log.setLogger(new Log.Logger(true));

        retain = Boolean.parseBoolean(System.getProperty("RETAIN_PACKAGER_TESTS"));
        workDir = new File("build/tmp/tests", "winservice");
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
        bundleParams.put(APP_NAME.getID(), "Smoke");
        bundleParams.put(VERBOSE.getID(), true);

        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        File output = bundler.execute(bundleParams, new File(workDir, "smoke"));
        assertNotNull(output);
        
        // make sure that the service launcher is there
        File launcher = WinServiceBundler.getLauncherSvc(output, bundleParams);
        assertTrue(launcher.exists());
    }

    
}
