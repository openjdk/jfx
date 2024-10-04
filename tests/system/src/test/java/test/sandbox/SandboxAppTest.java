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

package test.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static test.sandbox.Constants.ERROR_NONE;
import static test.sandbox.Constants.ERROR_NO_SECURITY_EXCEPTION;
import static test.sandbox.Constants.ERROR_SECURITY_EXCEPTION;
import static test.sandbox.Constants.ERROR_TIMEOUT;
import static test.sandbox.Constants.ERROR_UNEXPECTED_EXCEPTION;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.sun.javafx.PlatformUtil;

/**
 * Unit test for running JavaFX apps in a sandbox with a restrictive
 * security manager.
 */
@Timeout(value=25000, unit=TimeUnit.MILLISECONDS)
public class SandboxAppTest {

    private static final String className = SandboxAppTest.class.getName();
    private static final String pkgName = className.substring(0, className.lastIndexOf("."));

    private static String getTestPolicyFile(String policy) {
        return SandboxAppTest.class.getResource(policy).toExternalForm();
    }

    private void runSandboxedApp(String appName) throws Exception {
        runSandboxedApp(appName, ERROR_NONE);
    }

    private void runSandboxedApp(String appName, int exitCode) throws Exception {
        runSandboxedApp(appName, exitCode, "test.policy");
    }

    private void runSandboxedApp(String appName, int exitCode, String policy) throws Exception {
        final String testAppName = pkgName + ".app." + appName;
        final String testPolicy = getTestPolicyFile(policy);

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
                assertEquals(exitCode, retVal, testAppName + ": Unexpected 'success' exit code;");
                break;

            case 1:
                fail(testAppName
                        + ": unable to launch java application");

            case ERROR_TIMEOUT:
                fail(testAppName
                        + ": Application timeout");
            case ERROR_SECURITY_EXCEPTION:
                fail(testAppName
                        + ": Application failed with a security exception");
            case ERROR_NO_SECURITY_EXCEPTION:
                fail(testAppName
                        + ": Application did not get expected security exception");
            case ERROR_UNEXPECTED_EXCEPTION:
                fail(testAppName
                        + ": Application failed with unexpected exception");

           default:
               fail(testAppName
                        + ": Unexpected error exit: " + retVal);
        }
    }

    @BeforeEach
    public void setupEach() {
        if (PlatformUtil.isWindows()) {
            assumeTrue(Boolean.getBoolean("unstable.test")); // JDK-8255486
        }
    }

    // TEST CASES

    @Test
    public void testFXApp() throws Exception {
        runSandboxedApp("FXApp");
    }

    @Test
    public void testFXNonApp() throws Exception {
        runSandboxedApp("FXNonApp");
    }

    @Disabled("JDK-8202451")
    @Test
    public void testJFXPanelApp() throws Exception {
        runSandboxedApp("JFXPanelApp");
    }

    @Disabled("JDK-8202451")
    @Test
    public void testJFXPanelImplicitExitApp() throws Exception {
        runSandboxedApp("JFXPanelImplicitExitApp", 0);
    }

    @Test
    public void testFXWebApp() throws Exception {
        runSandboxedApp("FXWebApp", ERROR_NONE, "empty.policy");
    }
}
