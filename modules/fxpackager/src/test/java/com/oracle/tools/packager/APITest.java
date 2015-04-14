/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.oracle.tools.packager.StandardBundlerParam.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class APITest {

    static File tmpBase;
    static File workDir;
    static File appResourcesDir;
    static File fakeMainJar;
    static String runtimeJdk;
    static String runtimeJre;
    static File runtimeJdkFile;
    static File runtimeJreFile;

    @BeforeClass
    public static void prepareApp() {
        Log.setLogger(new Log.Logger(true));
        Log.setDebug(true);

        String tmpBasePath = System.getProperty("tmpBase");
        if (tmpBasePath != null) {
            tmpBase = new File(System.getProperty("tmpBase"));
        } else {
            tmpBase = new File("build/tmp/tests");
        }
        tmpBase.mkdirs();

        workDir = new File(tmpBase, "apiapp");
        appResourcesDir = new File(tmpBase, "appResources");
        fakeMainJar = new File(appResourcesDir, "mainApp.jar");

        runtimeJdk = System.getenv("PACKAGER_JDK_ROOT");
        runtimeJre = System.getenv("PACKAGER_JRE_ROOT");

        runtimeJdkFile = runtimeJdk == null ? null : new File(runtimeJdk);
        runtimeJreFile = runtimeJre == null ? null : new File(runtimeJre);
    }


    @Test
    public void testRuntimes() throws ConfigException, IOException {
        List<Object> runtimes = new ArrayList<>(5);
        runtimes.add(null);
        if (runtimeJdk != null) {
            System.out.println("Runtime JDK as string at " + runtimes.size());
            runtimes.add(runtimeJdk);
            if (runtimeJdkFile.isDirectory()) {
                System.out.println("Runtime JDK as Relative File Set at " + runtimes.size());
                runtimes.add(new RelativeFileSet(
                        runtimeJdkFile, Files.walk(runtimeJdkFile.toPath())
                        .map(path -> path.toFile().getAbsoluteFile())
                        .filter(File::isFile)
                        .collect(Collectors.toSet())
                ));
            }
        }
        if (runtimeJre != null) {
            System.out.println("Runtime JRE as string at " + runtimes.size());
            runtimes.add(runtimeJre);
            if (runtimeJreFile.isDirectory()) {
                System.out.println("Runtime JRE as Relative File Set at " + runtimes.size());
                runtimes.add(new RelativeFileSet(
                        runtimeJreFile, Files.walk(runtimeJreFile.toPath())
                        .map(path -> path.toFile().getAbsoluteFile())
                        .filter(File::isFile)
                        .collect(Collectors.toSet())
                ));
            }
        }
        
        for (int i = 0; i < runtimes.size(); i++) {
            Map<String, Object> params = new HashMap<>();

            // not part of the typical setup, for testing
            params.put(BUILD_ROOT.getID(), tmpBase);
            params.put(VERBOSE.getID(), true);

            params.put(APP_RESOURCES.getID(), new RelativeFileSet(appResourcesDir, Collections.singletonList(fakeMainJar)));
            params.put(MAIN_CLASS.getID(), "hello.HelloRectangle");
            params.put(APP_NAME.getID(), "API Runtime " + i);
            params.put(IDENTIFIER.getID(), "com.oracle.tools.packager.api.tests.runtime" + i);

            Object thisRuntime = runtimes.get(i);
            params.put("runtime", thisRuntime);

            Bundlers bundlers = Bundlers.createBundlersInstance();

            for (Bundler bundler : bundlers.getBundlers("image")) {
                try {
                    bundler.validate(params);
                } catch (UnsupportedPlatformException upe) {
                    continue;
                }
                File output = bundler.execute(params, new File(workDir, "Runtimes" + i));
                System.err.println("Bundle at - " + output);
                assertNotNull(output);
                assertTrue(output.exists());
            }            
        }
    }
}
