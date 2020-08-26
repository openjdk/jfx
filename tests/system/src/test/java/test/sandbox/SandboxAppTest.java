/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.sandbox;

import com.sun.javafx.PlatformUtil;
import java.util.ArrayList;
import junit.framework.AssertionFailedError;
import org.junit.Test;
import org.junit.Ignore;

import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static test.sandbox.Constants.*;

/**
 * Unit test for running JavaFX apps in a sandbox with a restrictive
 * security manager.
 */
public class SandboxAppTest {

    private static final String className = SandboxAppTest.class.getName();
    private static final String pkgName = className.substring(0, className.lastIndexOf("."));

    private static String getTestPolicyFile() {
        return SandboxAppTest.class.getResource("test.policy").toExternalForm();
    }

    private void runSandboxedApp(String appName) throws Exception {
        runSandboxedApp(appName, ERROR_NONE);
    }

    private void runSandboxedApp(String appName, int exitCode) throws Exception {
        final String testAppName = pkgName + ".app." + appName;
        final String testPolicy = getTestPolicyFile();

        final ArrayList<String> cmd =
                test.util.Util.createApplicationLaunchCommand(
                        testAppName,
                        null,
                        testPolicy
                );

        final ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        Process process = builder.start();
        int retVal = process.waitFor();
        switch (retVal) {
            case 0:
            case ERROR_NONE:
                assertEquals(testAppName + ": Unexpected 'success' exit code;",
                        exitCode, retVal);
                break;

            case 1:
                throw new AssertionFailedError(testAppName
                        + ": unable to launch java application");

            case ERROR_TIMEOUT:
                throw new AssertionFailedError(testAppName
                        + ": Application timeout");
            case ERROR_SECURITY_EXCEPTION:
                throw new AssertionFailedError(testAppName
                        + ": Application failed with a security exception");
            case ERROR_NO_SECURITY_EXCEPTION:
                throw new AssertionFailedError(testAppName
                        + ": Application did not get expected security exception");
            case ERROR_UNEXPECTED_EXCEPTION:
                throw new AssertionFailedError(testAppName
                        + ": Application failed with unexpected exception");

           default:
                throw new AssertionFailedError(testAppName
                        + ": Unexpected error exit: " + retVal);
        }
    }

    // TEST CASES

    @Test (timeout = 25000)
    public void testFXApp() throws Exception {
        runSandboxedApp("FXApp");
    }

    @Test (timeout = 25000)
    public void testFXNonApp() throws Exception {
        runSandboxedApp("FXNonApp");
    }

    @Ignore("JDK-8202451")
    @Test (timeout = 25000)
    public void testJFXPanelApp() throws Exception {
        runSandboxedApp("JFXPanelApp");
    }

    @Ignore("JDK-8202451")
    @Test (timeout = 25000)
    public void testJFXPanelImplicitExitApp() throws Exception {
        runSandboxedApp("JFXPanelImplicitExitApp", 0);
    }

}
