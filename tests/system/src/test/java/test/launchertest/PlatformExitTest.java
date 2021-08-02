/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.launchertest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import junit.framework.AssertionFailedError;
import org.junit.Test;
import test.util.Util;

import static test.launchertest.Constants.*;

/**
 * Unit test for Platform.exit
 */
public class PlatformExitTest {

    private static final String className = PlatformExitTest.class.getName();
    private static final String pkgName = className.substring(0, className.lastIndexOf("."));
    private static final String testAppName = pkgName + "." + "PlatformExitApp";

    @Test (timeout = 15000)
    public void testPlatformExit() throws Exception {

        final ArrayList<String> cmd =
                Util.createApplicationLaunchCommand(testAppName, null, null);

        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        final InputStream in = process.getInputStream();

        // Wait for the process to exit
        int retVal = process.waitFor();
        switch (retVal) {
            case 0:// SUCCESS
            case ERROR_NONE:
                break;

            case 1:
                throw new AssertionFailedError(testAppName
                        + ": unable to launch java application");

            case ERROR_TIMEOUT:
                throw new AssertionFailedError(testAppName
                        + ": application timeout");

            case ERROR_UNEXPECTED_EXCEPTION:
                throw new AssertionFailedError(testAppName
                        + ": unexpected exception");

            default:
                throw new AssertionFailedError(testAppName
                        + ": Unexpected error exit: " + retVal);
        }

        // Read the output of the forked process and check for warning string
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder = stringBuilder.append(line).append("\n");
        }
        if (stringBuilder.indexOf("Java has been detached") >= 0) {
            System.err.println(stringBuilder);
            throw new AssertionFailedError(testAppName + ": tried to use JNI after Java was detached");
        }
    }

}
