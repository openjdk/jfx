/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import junit.framework.AssertionFailedError;
import org.junit.Test;

import static org.junit.Assert.*;
import static test.launchertest.Constants.*;

/**
 * Unit test for legacy FX .jar support in Java 8 (and later) launcher
 */
public class JarLauncherTest {

    private final String testAppName = System.getProperty("launchertest.testapp1.jar");
    private final int testExitCode = ERROR_NONE;

    @Test (timeout = 15000)
    public void testJarLauncher() throws Exception {
        assertNotNull(testAppName);
        final ArrayList<String> cmd =
                test.util.Util.createApplicationLaunchCommand(
                        testAppName,
                        null,
                        null
                        );

        final ProcessBuilder builder = new ProcessBuilder(cmd);

        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        Process process = builder.start();
        int retVal = process.waitFor();
        switch (retVal) {
            case 0:// SUCCESS
            case ERROR_NONE:
                if (retVal != testExitCode) {
                    throw new AssertionFailedError(testAppName
                            + ": Unexpected 'success' exit; expected:"
                            + testExitCode + " was:" + retVal);
                }
                return;

            case 1:
                throw new AssertionFailedError(testAppName
                        + ": unable to launch java application");

            case ERROR_UNEXPECTED_EXCEPTION:
                throw new AssertionFailedError(testAppName
                + ": unexpected exception");

            default:
                throw new AssertionFailedError(testAppName
                        + ": Unexpected error exit: " + retVal);
        }
    }

}
