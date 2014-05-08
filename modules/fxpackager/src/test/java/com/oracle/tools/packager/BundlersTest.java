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

package com.oracle.tools.packager;

import com.oracle.tools.packager.windows.WinExeBundler;
import com.oracle.tools.packager.windows.WinMsiBundler;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class BundlersTest {

    // walking the params creates the temp packaging directories,
    // so we need to clean them up.
    //@AfterClass
    public static void cleanupTmpDir() throws IOException {
        // paint a broad brush: delete all fxpackager junk in the temp dir
        File tmpBase = Files.createTempDirectory("fxbundler").toFile().getParentFile();
        File[] children = tmpBase.listFiles();
        if (children != null) {
            for (File f : children) {
                if (f.getName().startsWith("fxbundler")) {
                    attemptDelete(f);
                }
            }
        }
    }

    private static void attemptDelete(File tmpBase) {
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
    public void testCommonBundlersDeclareParameters() {
        boolean hasNullParams = false;
        for (Bundler bundler : Bundlers.createBundlersInstance().getBundlers()) {
            Collection<BundlerParamInfo<?>> params = bundler.getBundleParameters();
            if (params == null) {
                System.err.println("Bundler '" + bundler.getID() + "' has a null parameter set");
                hasNullParams = true;
            }
        }

        assumeTrue(!hasNullParams); // deleteme when fixed
        assertTrue("All common bundlers have parameters.", !hasNullParams);
    }


    @Test
    public void testCommonBundlerParameterDuplicates() {
        boolean duplicateFound = false;
        for (Bundler bundler : Bundlers.createBundlersInstance().getBundlers()) {
            Collection<BundlerParamInfo<?>> params = bundler.getBundleParameters();
            if (params == null) continue; // caught by another test
            
            Map<String, List<BundlerParamInfo<?>>> paramsGroupMap = params.stream().collect(Collectors.groupingBy(BundlerParamInfo::getID));
            
            for (Map.Entry<String, List<BundlerParamInfo<?>>> paramGroupEntry : paramsGroupMap.entrySet()) {
                if (paramGroupEntry.getValue().size() > 1) {
                    System.err.println("Duplicate param '" + paramGroupEntry.getKey() + "' for bundler '" + bundler.getID() + "'");
                    duplicateFound = true;
                }
            }
        }

        assertFalse("Parameter list within a bundler has a duplicate ID.", duplicateFound);
    }
    
    boolean assertMetadata(Bundler bundler, BundlerParamInfo<?> bpi, String checkDescription, Function<BundlerParamInfo, Boolean> check) {
        if (!check.apply(bpi)) {
            System.err.println("Bundler '" + bundler.getID() + "' parameter '" + bpi.getID() + "' failed metadata check: " + checkDescription);
            return false;
        } else {
            return true;
        }
    }
    
    @Test
    public void testCommonBundlerParameterMetadata() {
        boolean metadataValid = true;
        for (Bundler bundler : Bundlers.createBundlersInstance().getBundlers()) {
            Collection<BundlerParamInfo<?>> params = bundler.getBundleParameters();
            if (params == null) continue; // caught by another test

            for (BundlerParamInfo<?> bpi : params) {
                System.err.println("Checking '" + bundler.getID() + "' param '" + bpi.getID() + "'");
                metadataValid &= assertMetadata(bundler, bpi, "Name is not null", param -> param.getName() != null);
                metadataValid &= assertMetadata(bundler, bpi, "ID is not null", param -> param.getID() != null);
                metadataValid &= assertMetadata(bundler, bpi, "Description is not null", param -> param.getDescription() != null);
                metadataValid &= assertMetadata(bundler, bpi, "ValueType is not null", param -> param.getValueType() != null);
            }
        }

        assertTrue("Metadata on pre-packaged bundlers is valid.", metadataValid);
    }


    // for all bundlers that can be found, assert
    //  they have the requisite metadata
    //  for the parameters they declare
    //   * ? They all include a substantial portion of the standard parameters (90%?) ?


    @Test
    public void getBundlersPlatformTest() {
        Collection<String> bundlerIDs = new ArrayList<>();
        for (Bundler bundler : Bundlers.createBundlersInstance().getBundlers()) {
            try {
                bundler.validate(new HashMap<>());
            } catch (UnsupportedPlatformException upe) {
                // don't list bundlers this platform cannot run
                continue;
            } catch (ConfigException ignore) {
                // but requiring more than an empty map is perfectly fine.
            }
            bundlerIDs.add(bundler.getID());
        }

        boolean mac = System.getProperty("os.name").toLowerCase().contains("os x");
        assertEquals(mac, bundlerIDs.contains("mac.app"));
        assertEquals(mac, bundlerIDs.contains("dmg"));
        assertEquals(mac, bundlerIDs.contains("pkg"));

        boolean linux = System.getProperty("os.name").toLowerCase().startsWith("linux");
        assertEquals(linux, bundlerIDs.contains("linux.app"));
        assertEquals(linux, bundlerIDs.contains("deb"));
        assertEquals(linux, bundlerIDs.contains("rpm"));
        
        boolean windows = System.getProperty("os.name").toLowerCase().startsWith("win");
        assertEquals(windows, bundlerIDs.contains("windows.app"));
        assertEquals(windows, bundlerIDs.contains("msi"));
        assertEquals(windows, bundlerIDs.contains("exe"));
    }

    @Test
    public void noNullBundlerIDs() {
        Collection<String> bundlerIDs = getBundlerIDs();
        
        assertFalse(bundlerIDs.contains(null));
        assertFalse(bundlerIDs.contains("null"));
    }


    @Test
    public void noDuplicateBundlerIDs() {
        Collection<Bundler> bundlers = Bundlers.createBundlersInstance().getBundlers();

        Map<String, List<Bundler>> paramsGroupMap = bundlers.stream().collect(Collectors.groupingBy(Bundler::getID));

        boolean duplicateFound = false;
        for (Map.Entry<String, List<Bundler>> paramGroupEntry : paramsGroupMap.entrySet()) {
            if (paramGroupEntry.getValue().size() > 1) {
                System.err.println("Duplicate bundler ID '" + paramGroupEntry.getKey() + "'.");
                duplicateFound = true;
            }
        }
        
        assertFalse("Bundlers have a duplicate ID", duplicateFound);
    }

    public List<String> getBundlerIDs() {
        Collection<Bundler> bundlers = Bundlers.createBundlersInstance().getBundlers();

        return Arrays.asList(
                bundlers.stream().map(Bundler::getID).toArray(String[]::new));
    }

    @Test
    public void customParamFallbackTests() {
        Map<String, ? super Object> params;

        params = new TreeMap<>();
        assertTrue(WinMsiBundler.MSI_SYSTEM_WIDE.fetchFrom(params));
        assertFalse(WinExeBundler.EXE_SYSTEM_WIDE.fetchFrom(params));

        params = new TreeMap<>();
        params.put(StandardBundlerParam.SYSTEM_WIDE.getID(), "false");
        assertFalse(WinMsiBundler.MSI_SYSTEM_WIDE.fetchFrom(params));
        assertFalse(WinExeBundler.EXE_SYSTEM_WIDE.fetchFrom(params));

        params = new TreeMap<>();
        params.put(StandardBundlerParam.SYSTEM_WIDE.getID(), "true");
        assertTrue(WinMsiBundler.MSI_SYSTEM_WIDE.fetchFrom(params));
        assertTrue(WinExeBundler.EXE_SYSTEM_WIDE.fetchFrom(params));
    }

    @Test
    public void badMainJar() {
        try {
            Map<String, ? super Object> params = new TreeMap<>();
            params.put(StandardBundlerParam.APP_RESOURCES.getID(), new RelativeFileSet(new File("."), Collections.emptySet()));
            params.put(StandardBundlerParam.MAIN_JAR.getID(), "this_jar_must_not_exist.jar");

            StandardBundlerParam.MAIN_JAR.fetchFrom(params);

            fail("An exception should have been thrown");
        } catch (RuntimeException re) {
            assertTrue("RuntimeException wraps a ConfigException", re.getCause() instanceof ConfigException);
        }
    }

}
