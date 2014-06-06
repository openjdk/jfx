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

import static com.oracle.tools.packager.StandardBundlerParam.APP_NAME;
import static com.oracle.tools.packager.StandardBundlerParam.IDENTIFIER;
import static com.oracle.tools.packager.StandardBundlerParam.BUILD_ROOT;
import static com.oracle.tools.packager.StandardBundlerParam.VERBOSE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import com.oracle.tools.packager.AbstractBundler;
import com.oracle.tools.packager.Log;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.UnsupportedPlatformException;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class MacDaemonBundlerTest {

    static File tmpBase;
    static File workDir;
    static boolean retain = false;

    @BeforeClass
    public static void prepareApp() {
        // only run on mac
        Assume.assumeTrue(System.getProperty("os.name").toLowerCase().contains("os x"));

        // and only if we have the correct JRE settings
        String jre = System.getProperty("java.home").toLowerCase();
        Assume.assumeTrue(jre.endsWith("/contents/home/jre") || jre.endsWith("/contents/home/jre"));

        Log.setLogger(new Log.Logger(true));

        retain = Boolean.parseBoolean(System.getProperty("RETAIN_PACKAGER_TESTS"));
        workDir = new File("build/tmp/tests", "macdaemon");
    }

    @Before
    public void createTmpDir() throws IOException {
        if (retain) {
            tmpBase = new File("build/tmp/tests/macdaemon");
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
        AbstractBundler bundler = new MacDaemonBundler();

        assertNotNull(bundler.getName());
        assertNotNull(bundler.getID());
        assertNotNull(bundler.getDescription());
        //assertNotNull(bundler.getBundleParameters());

        Map<String, Object> bundleParams = new HashMap<>();

        bundleParams.put(BUILD_ROOT.getID(), tmpBase);

        bundleParams.put(APP_NAME.getID(), "Smoke Test App");
        bundleParams.put(IDENTIFIER.getID(), "smoke.app");        
        bundleParams.put(VERBOSE.getID(), true);

        boolean valid = bundler.validate(bundleParams);
        assertTrue(valid);

        File result = bundler.execute(bundleParams, new File(workDir, "smoke"));
        System.err.println("Bundle at - " + result);
        assertNotNull(result);
        assertTrue(result.exists());
    }
    
}
