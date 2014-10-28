/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package launchertest;

import com.sun.javafx.PlatformUtil;
import java.util.ArrayList;
import java.util.Collection;
import junit.framework.AssertionFailedError;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.Test;

import static launchertest.Constants.*;
import static org.junit.Assume.*;

/**
 * Unit test for FX support in Java 8 launcher
 */
@RunWith(Parameterized.class)
public class MainLauncherTest {

    private static final String className = MainLauncherTest.class.getName();
    private static final String pkgName = className.substring(0, className.lastIndexOf("."));

    private static Collection params = null;

    public static class TestData {
        final String appName;
        final String pldrName;
        final boolean headless;
        final int exitCode;

        public TestData(String appName) {
            this(appName, 0);
        }

        public TestData(String appName, int exitCode) {
            this(appName, null, exitCode);
        }

        public TestData(String appName, String pldrName, int exitCode) {
            this(appName, pldrName, false, exitCode);
        }

        public TestData(String appName, boolean headless, int exitCode) {
            this(appName, null, headless, exitCode);
        }

        public TestData(String appName, String pldrName, boolean headless, int exitCode) {
            this.appName = pkgName + "." + appName;
            this.pldrName = pldrName == null ? null : pkgName + "." +  pldrName;
            this.headless = headless;
            this.exitCode = exitCode;
        }
    }

    private static final TestData[] testData = {
        new TestData("TestApp"),
        new TestData("TestAppNoMain"),
        new TestData("TestNotApplication"),
        new TestData("TestAppThreadCheck", ERROR_NONE),
        new TestData("TestAppNoMainThreadCheck", ERROR_NONE),
        new TestData("TestNotApplicationThreadCheck", ERROR_NONE),
        new TestData("TestAppThreadCheck", "TestPreloader", ERROR_NONE),
        new TestData("TestAppNoMainThreadCheck", "TestPreloader", ERROR_NONE),
        new TestData("TestAppCCL", ERROR_NONE),
        new TestData("TestAppCCL1", ERROR_NONE),
        new TestData("TestAppCCL2", ERROR_NONE),
        new TestData("TestAppNoMainCCL", ERROR_NONE),
        new TestData("TestAppNoMainCCL2", ERROR_NONE),
        new TestData("TestAppNoMainCCL3", ERROR_NONE),
        new TestData("TestNotApplicationCCL", ERROR_NONE),
        new TestData("TestHeadlessApp", true, ERROR_NONE),
    };

    @Parameters
    public static Collection getParams() {
        if (params == null) {
            params = new ArrayList();
            for (TestData data : testData) {
                params.add(new TestData[] { data });
            }
        }
        return params;
    }

    private final String testAppName;
    private final String testPldrName;
    private final boolean headless;
    private final int testExitCode;

    public MainLauncherTest(TestData testData) {
        this.testAppName = testData.appName;
        this.testPldrName = testData.pldrName;
        this.headless = testData.headless;
        this.testExitCode = testData.exitCode;
    }

    @Test (timeout=5000)
    public void testMainLauncher() throws Exception {
        if (headless) {
            // Headless tests currently only run on Linux
            assumeTrue(PlatformUtil.isLinux());
        }

        final String classpath = System.getProperty("java.class.path");
        ProcessBuilder builder;
        if (testPldrName != null) {
            builder = new ProcessBuilder("java", "-cp", classpath,
                    "-Djavafx.preloader=" + testPldrName, testAppName);
        } else {
            builder = new ProcessBuilder("java", "-cp", classpath, testAppName);
        }
        if (headless) {
            // Set DISPLAY variable to empty to run in headless mode on Linux
            builder.environment().put("DISPLAY", "");
        }
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

            case ERROR_INIT_BEFORE_MAIN:
                throw new AssertionFailedError(testAppName
                        + ": main method not called before init");
            case ERROR_START_BEFORE_MAIN:
                throw new AssertionFailedError(testAppName
                        + ": main method not called before start");
            case ERROR_STOP_BEFORE_MAIN:
                throw new AssertionFailedError(testAppName
                        + ": main method not called before stop");

            case ERROR_START_BEFORE_INIT:
                throw new AssertionFailedError(testAppName
                        + ": init method not called before start");
            case ERROR_STOP_BEFORE_INIT:
                throw new AssertionFailedError(testAppName
                        + ": init method not called before stop");

            case ERROR_STOP_BEFORE_START:
                throw new AssertionFailedError(testAppName
                        + ": start method not called before stop");

            case ERROR_CLASS_INIT_WRONG_THREAD:
                throw new AssertionFailedError(testAppName
                        + ": class initialization called on wrong thread");
            case ERROR_MAIN_WRONG_THREAD:
                throw new AssertionFailedError(testAppName
                        + ": main called on wrong thread");
            case ERROR_CONSTRUCTOR_WRONG_THREAD:
                throw new AssertionFailedError(testAppName
                        + ": constructor called on wrong thread");
            case ERROR_INIT_WRONG_THREAD:
                throw new AssertionFailedError(testAppName
                        + ": init called on wrong thread");
            case ERROR_START_WRONG_THREAD:
                throw new AssertionFailedError(testAppName
                        + ": start called on wrong thread");
            case ERROR_STOP_WRONG_THREAD:
                throw new AssertionFailedError(testAppName
                        + ": stop called on wrong thread");

            case ERROR_PRELOADER_CLASS_INIT_WRONG_THREAD:
                throw new AssertionFailedError(testAppName
                        + ": preloader class initialization called on wrong thread");
            case ERROR_PRELOADER_CONSTRUCTOR_WRONG_THREAD:
                throw new AssertionFailedError(testAppName
                        + ": preloader constructor called on wrong thread");
            case ERROR_PRELOADER_INIT_WRONG_THREAD:
                throw new AssertionFailedError(testAppName
                        + ": preloader init called on wrong thread");
            case ERROR_PRELOADER_START_WRONG_THREAD:
                throw new AssertionFailedError(testAppName
                        + ": preloader start called on wrong thread");
            case ERROR_PRELOADER_STOP_WRONG_THREAD:
                throw new AssertionFailedError(testAppName
                        + ": preloader stop called on wrong thread");

            case ERROR_CONSTRUCTOR_WRONG_CCL:
                throw new AssertionFailedError(testAppName
                        + ": constructor has wrong CCL");
            case ERROR_START_WRONG_CCL:
                throw new AssertionFailedError(testAppName
                        + ": start has wrong CCL");

            case ERROR_UNEXPECTED_EXCEPTION:
                throw new AssertionFailedError(testAppName
                + ": unexpected exception");
            case ERROR_LAUNCH_SUCCEEDED:
                throw new AssertionFailedError(testAppName
                + ": Application.launch unexpectedly succeeded");

            default:
                throw new AssertionFailedError(testAppName
                        + ": Unexpected error exit: " + retVal);
        }
    }

}
