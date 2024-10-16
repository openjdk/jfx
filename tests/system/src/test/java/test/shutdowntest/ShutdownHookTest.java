/*
 * Copyright (c) 2014, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.shutdowntest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static test.shutdowntest.Constants.ERROR_NONE;
import static test.shutdowntest.Constants.ERROR_SOCKET;
import static test.shutdowntest.Constants.SOCKET_HANDSHAKE;
import static test.shutdowntest.Constants.STATUS_ILLEGAL_STATE;
import static test.shutdowntest.Constants.STATUS_OK;
import static test.shutdowntest.Constants.STATUS_RUNNABLE_EXECUTED;
import static test.shutdowntest.Constants.STATUS_UNEXPECTED_EXCEPTION;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Unit test for calling Platform runLater from a ShutdownHook.
 */
@Timeout(value=15000, unit=TimeUnit.MILLISECONDS)
public class ShutdownHookTest {

    private static final String className = ShutdownHookTest.class.getName();
    private static final String pkgName = className.substring(0, className.lastIndexOf("."));

    private final String testAppName = pkgName + "." + "ShutdownHookApp";

    @Test
    public void testShutdownHook() throws Exception {
        // Initilaize the socket
        final ServerSocket service = new ServerSocket(0);
        final int port = service.getLocalPort();

        // Launch the test app
        final ArrayList<String> cmd
                = test.util.Util.createApplicationLaunchCommand(
                        testAppName,
                        null,
                        null
                );
        // and add our argument
        cmd.add(String.valueOf(port));
        ProcessBuilder builder;
        builder = new ProcessBuilder(cmd);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        Process process = builder.start();

        // Accept a connection from the test app
        final Socket socket = service.accept();
        final InputStream in = socket.getInputStream();

        // Read the "handshake" token
        int handshake = in.read();
        assertEquals(SOCKET_HANDSHAKE, handshake, "Socket handshake failed,");

        // Read the status code from the shutdown hook
        int status = in.read();
        switch (status) {
            case STATUS_OK:
                break;
            case STATUS_ILLEGAL_STATE:
                fail(testAppName
                    + ": IllegalStateException from Platform.runLater");
                break;
            case STATUS_RUNNABLE_EXECUTED:
                fail(testAppName
                    + ": Unexpected execution of Platform.runLater Runnable from ShutdownHook");
                break;
            case STATUS_UNEXPECTED_EXCEPTION:
                fail(testAppName + ": Unexpected exception");
                break;
            default:
                fail(testAppName + ": Unexpected status: " + status);
        }

        // Make sure that the process exited as expected
        int retVal = process.waitFor();
        switch (retVal) {
            case ERROR_NONE:
                break;

            case ERROR_SOCKET:
                fail(testAppName + ": Error connecting to socket");
                break;

            case 0:
                fail(testAppName + ": Unexpected exit 0");
                break;

            case 1:
                fail(testAppName + ": Unable to launch java application");
                break;

            default:
                fail(testAppName + ": Unexpected error exit: " + retVal);
        }
    }

}
