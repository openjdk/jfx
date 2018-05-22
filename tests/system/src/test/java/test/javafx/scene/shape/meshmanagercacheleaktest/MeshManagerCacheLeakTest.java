/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.shape.meshmanagercacheleaktest;

import java.util.ArrayList;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import static test.javafx.scene.shape.meshmanagercacheleaktest.Constants.*;

/**
 * Unit test for verifying leak with cache of TriangleMesh in PredefinedMeshManager.
 */
public class MeshManagerCacheLeakTest {

    private final String className = MeshManagerCacheLeakTest.class.getName();
    private final String pkgName = className.substring(0, className.lastIndexOf("."));
    private final String testAppName = pkgName + "." + "MeshManagerCacheLeakApp";

    @Before
    public void setUp() {
        assumeTrue(Platform.isSupported(ConditionalFeature.SCENE3D));
        assumeTrue(Boolean.getBoolean("unstable.test")); // JDK-8201763
    }

    @Test (timeout = 15000)
    public void testSphereCacheLeakTest() throws Exception {
        testMeshManagerCacheLeak("Sphere", "10");
    }

    @Test (timeout = 15000)
    public void testCylinderCacheLeakTest() throws Exception {
        testMeshManagerCacheLeak("Cylinder", "25");
    }

    @Test (timeout = 20000)
    public void testBoxCacheLeakTest() throws Exception {
        testMeshManagerCacheLeak("Box", "350");
    }

    private void testMeshManagerCacheLeak(String shape, String count) throws Exception {
        String[] jvmArgs = {"-Xmx16m"};
        // Launch the test app
        final ArrayList<String> cmd = test.util.Util.createApplicationLaunchCommand(
            testAppName, null, null, jvmArgs);
        // and add our arguments
        cmd.add(String.valueOf(shape));
        cmd.add(String.valueOf(count));
        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        Process process = builder.start();

        // Make sure that the process exited as expected
        int retVal = process.waitFor();
        switch (retVal) {
            case 0:
                fail(testAppName + ": Unexpected exit 0 with cache test of : " + shape);
                break;

            case 1:
                fail(testAppName + ": Unable to launch java application with cache test of : " + shape);
                break;

            case ERROR_NONE:
                break;

            case ERROR_OOM:
                fail(testAppName + ": OOM occured with cache test of : " + shape);
                break;

            case ERROR_LAUNCH:
                fail(testAppName + ": Window was not shown for more than 10 secs, with cache test of : " + shape);
                break;

            default:
                fail(testAppName + ": Unexpected error exit: " + retVal + " with cache test of : " + shape);
                break;
        }
    }
}
