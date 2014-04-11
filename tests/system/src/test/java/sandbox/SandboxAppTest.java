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

package sandbox;

import com.sun.javafx.PlatformUtil;
import java.io.File;
import junit.framework.AssertionFailedError;
import org.junit.Test;

import static org.junit.Assert.*;
import static sandbox.Constants.*;

/**
 * Unit test for running JavaFX apps in a sandbox with a restrictive
 * security manager.
 */
public class SandboxAppTest {

    private static final String className = SandboxAppTest.class.getName();
    private static final String pkgName = className.substring(0, className.lastIndexOf("."));

    private String getJfxrtDir(final String classpath) {
        final String jfxrt = "jfxrt.jar";
        String cp = classpath;
        int idx = cp.replace('\\', '/').indexOf("/" + jfxrt);
        assertTrue("No " + jfxrt + " in classpath", idx >= 0);
        cp = cp.substring(0, idx);
        idx = cp.lastIndexOf(File.pathSeparator);
        if (idx >= 0) {
            cp = cp.substring(idx, cp.length());
        }
        return cp;
    }

    private static String getTestPolicyFile() {
        return SandboxAppTest.class.getResource("test.policy").toExternalForm();
    }

    private void runSandboxedApp(String appName) throws Exception {
        runSandboxedApp(appName, ERROR_NONE);
    }

    private void runSandboxedApp(String appName, int exitCode) throws Exception {
        final String testAppName = pkgName + ".app." + appName;
        final String classpath = System.getProperty("java.class.path");
        final String jfxrtDir = getJfxrtDir(classpath);
        final String testPolicy = getTestPolicyFile();
        ProcessBuilder builder;
        builder = new ProcessBuilder("java",
                "-Djava.ext.dirs=" + jfxrtDir,
                "-Djava.security.manager",
                "-Djava.security.policy=" + testPolicy,
                "-cp", classpath,
                testAppName);
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

    @Test (timeout=10000)
    public void testFXApp() throws Exception {
        runSandboxedApp("FXApp");
    }

    @Test (timeout=10000)
    public void testFXNonApp() throws Exception {
        runSandboxedApp("FXNonApp");
    }

    @Test (timeout=10000)
    public void testJFXPanelApp() throws Exception {
        runSandboxedApp("JFXPanelApp");
    }

    @Test (timeout=10000)
    public void testJFXPanelImplicitExitApp() throws Exception {
        // Test skipped on Mac OS X due to 8037776
        if (PlatformUtil.isMac()) {
            System.err.println("*** Skipping test on Mac OS X");
            return;
        }
        runSandboxedApp("JFXPanelImplicitExitApp", 0);
    }

}
