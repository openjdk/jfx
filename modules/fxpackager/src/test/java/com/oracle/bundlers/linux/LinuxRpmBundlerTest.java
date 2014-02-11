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

package com.oracle.bundlers.linux;

import com.oracle.bundlers.Bundler;
import com.oracle.bundlers.StandardBundlerParam;
import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.packager.bundlers.ConfigException;
import com.sun.javafx.tools.packager.bundlers.LinuxRPMBundler;
import com.sun.javafx.tools.packager.bundlers.RelativeFileSet;
import com.sun.javafx.tools.packager.bundlers.UnsupportedPlatformException;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.junit.Assert.assertNotNull;

public class LinuxRpmBundlerTest {

    static File tmpBase;
    static File workDir;
    static File appResourcesDir;
    static File fakeMainJar;
    static Set<File> appResources;
    
    @BeforeClass
    public static void prepareApp() {
        // only run on linux
        Assume.assumeTrue(System.getProperty("os.name").toLowerCase().startsWith("linux"));

        Log.setLogger(new Log.Logger(true));
        
        tmpBase = new File(System.getProperty("tmpBase"));
        tmpBase.mkdirs();
        workDir = new File(tmpBase, "linuxrpm");
        appResourcesDir = new File(tmpBase, "appResources");
        fakeMainJar = new File(appResourcesDir, "mainApp.jar");

        appResources = new HashSet<>(Arrays.asList(fakeMainJar));
        
    }
    
    /**
     * See if smoke comes out
     */
    @Test
    public void smokeTest() throws IOException, ConfigException, UnsupportedPlatformException {
        Bundler bundler = new LinuxRPMBundler();
        ((LinuxRPMBundler)bundler).setVerbose(true);
        
        assertNotNull(bundler.getName());
        assertNotNull(bundler.getID());
        assertNotNull(bundler.getDescription());
        //assertNotNull(bundler.getBundleParameters());

        Map<String, Object> bundleParams = new HashMap<>();
        
       
        tmpBase.mkdirs();
        // not part of the typical setup, for testing
        bundleParams.put(StandardBundlerParam.BUILD_ROOT.getID(), Files.createTempDirectory(tmpBase.toPath(), "fxpackager").toFile());

        bundleParams.put(StandardBundlerParam.NAME.getID(), "Smoke");
        bundleParams.put(StandardBundlerParam.MAIN_CLASS.getID(), "hello.HelloRectangle");
        bundleParams.put(StandardBundlerParam.APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));

        bundler.execute(bundleParams, new File(workDir, "smoke"));
        
        try {
            System.getProperties().store(System.out, "Dump");
            System.out.println(new File(".").getCanonicalPath());
            System.out.println(new File(System.getProperty("tmpBase")).getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        Bundler bundler = new LinuxRPMBundler();
        ((LinuxRPMBundler)bundler).setVerbose(true);

        Map<String, Object> bundleParams = new HashMap<>();

        // not part of the typical setup, for testing
        bundleParams.put(StandardBundlerParam.BUILD_ROOT.getID(), Files.createTempDirectory(tmpBase.toPath(), "fxpackager").toFile());

        bundleParams.put(StandardBundlerParam.APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, appResources));

        File output = bundler.execute(bundleParams, new File(workDir, "BareMinimum"));
        System.out.println(output);
        Assume.assumeTrue(output.isFile());
    }

}
