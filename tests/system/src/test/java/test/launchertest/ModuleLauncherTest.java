/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * Unit test for launching modular FX applications
 */
public class ModuleLauncherTest {

    private final String modulePath = System.getProperty("launchertest.testapp2.module.path");
    private final String moduleName = "mymod";
    private final int testExitCode = ERROR_NONE;

    private void doTestLaunchModule(String testAppName) throws Exception {
        assertNotNull(testAppName);
        String mpArg = "--module-path=" + modulePath;
        String moduleAppName = "--module=" + moduleName + "/" + testAppName;
        final ArrayList<String> cmd =
                test.util.Util.createApplicationLaunchCommand(
                        moduleAppName,
                        null,
                        null,
                        new String[] { mpArg }
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

            case ERROR_TOOLKIT_NOT_RUNNING:
                throw new AssertionFailedError(testAppName
                        + ": Toolkit not running prior to loading application class");
            case ERROR_TOOLKIT_IS_RUNNING:
                throw new AssertionFailedError(testAppName
                        + ": Toolkit is running but should not be");

            case ERROR_UNEXPECTED_EXCEPTION:
                throw new AssertionFailedError(testAppName
                + ": unexpected exception");

            default:
                throw new AssertionFailedError(testAppName
                        + ": Unexpected error exit: " + retVal);
        }
    }


    @Test (timeout=15000)
    public void testLaunchModule() throws Exception {
        doTestLaunchModule("testapp.TestApp");
    }

    @Test (timeout=15000)
    public void testLaunchModuleNoMain() throws Exception {
        doTestLaunchModule("testapp.TestAppNoMain");
    }

    @Test (timeout=15000)
    public void testLaunchModuleNotApplication() throws Exception {
        doTestLaunchModule("testapp.TestNotApplication");
    }

}
