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

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
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

    private static final String className = MeshManagerCacheLeakTest.class.getName();
    private static final String pkgName = className.substring(0, className.lastIndexOf("."));
    private final String testAppName = pkgName + "." + "MeshManagerCacheLeakApp";

    @Before
    public void setUp() {
        assumeTrue(Platform.isSupported(ConditionalFeature.SCENE3D));
    }

    @Test
    public void testMeshManagerCacheLeak() throws Exception {
        // Initilaize the socket
        final ServerSocket service = new ServerSocket(0);
        final int port = service.getLocalPort();
        String[] jvmArgs = {"-Xmx16m"};
        String[] testArgs = { "Sphere", "Cylinder", "Box"};
        String[] numShapes = { "10", "25", "350"};

        for (int i = 0; i < testArgs.length; ++i) {
            // Launch the test app
            final ArrayList<String> cmd
                    = test.util.Util.createApplicationLaunchCommand(testAppName,
                            null, null, jvmArgs);
            // and add our arguments
            cmd.add(String.valueOf(port));
            cmd.add(String.valueOf(testArgs[i]));
            cmd.add(String.valueOf(numShapes[i]));
            ProcessBuilder builder = new ProcessBuilder(cmd);
            builder.redirectError(ProcessBuilder.Redirect.INHERIT);
            builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            Process process = builder.start();

            // Accept a connection from the test app
            final Socket socket = service.accept();
            final InputStream in = socket.getInputStream();

            // Read the "handshake" token
            int handshake = in.read();
            Assert.assertEquals("Socket handshake failed,", SOCKET_HANDSHAKE, handshake);

            // Read the status code from the test app.
            int status = in.read();
            switch (status) {
                case STATUS_OK:
                    break;
                case STATUS_OOM:
                    fail(testAppName
                        + ": OOM occured with cache of : " + testArgs[i]);
                    break;
                default:
                    fail(testAppName + ": Unexpected status: " + status + " with cache of : " + testArgs[i]);
            }

            // Make sure that the process exited as expected
            int retVal = process.waitFor();
            switch (retVal) {
                case ERROR_NONE:
                    break;

                case 0:
                    fail(testAppName + ": Unexpected exit 0 with cache test of : " + testArgs[i]);
                    break;

                case 1:
                    fail(testAppName + ": Unable to launch java application with cache test of : " + testArgs[i]);
                    break;

                case ERROR_SOCKET:
                    fail(testAppName + ": Error connecting to socket with cache test of : " + testArgs[i]);
                    break;

                case ERROR_OOM:
                    fail(testAppName + ": OOM occured with cache test of : " + testArgs[i]);
                    break;

                case ERROR_LAUNCH:
                    fail(testAppName + ": Window was not shown for more than 10 secs, with cache test of : " + testArgs[i]);
                    break;

                default:
                    fail(testAppName + ": Unexpected error exit: " + retVal + " with cache test of : " + testArgs[i]);
            }
        }
    }
}
