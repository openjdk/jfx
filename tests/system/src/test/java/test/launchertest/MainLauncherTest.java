/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static test.launchertest.Constants.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.javafx.PlatformUtil;

/**
 * Unit test for FX support in Java 8 launcher
 */
@Timeout(value=15000, unit=TimeUnit.MILLISECONDS)
public final class MainLauncherTest {

    private static final String className = MainLauncherTest.class.getName();
    private static final String pkgName = className.substring(0, className.lastIndexOf("."));

    public static class TestData {
        final String testAppName;
        final String testPldrName;
        final boolean headless;
        final int testExitCode;

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
            this.testAppName = pkgName + "." + appName;
            this.testPldrName = pldrName == null ? null : pkgName + "." +  pldrName;
            this.headless = headless;
            this.testExitCode = exitCode;
        }
    }

    private static Collection<TestData> parameters() {
        return List.of(
            new TestData("TestApp"),
            new TestData("TestAppNoMain"),
            new TestData("TestNotApplication"),
            new TestData("TestStartupApp1", ERROR_NONE),
            new TestData("TestStartupApp2", ERROR_NONE),
            new TestData("TestStartupAppNoMain", ERROR_NONE),
            new TestData("TestStartupJFXPanel", ERROR_NONE),
            new TestData("TestStartupNotApplication", ERROR_NONE),
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
            new TestData("TestAWTAppDaemon", ERROR_NONE),
            new TestData("TestAppDaemon", ERROR_NONE),
            new TestData("TestAppPlatformExitAWT", ERROR_NONE)
        );
    };

    @ParameterizedTest
    @MethodSource("parameters")
    public void testMainLauncher(TestData d) throws Exception {
        if (d.headless) {
            // Headless tests currently only run on Linux
            assumeTrue(PlatformUtil.isLinux());
        }

        final ArrayList<String> cmd =
                test.util.Util.createApplicationLaunchCommand(
                        d.testAppName,
                        d.testPldrName,
                        null
                        );

        final ProcessBuilder builder = new ProcessBuilder(cmd);

        if (d.headless) {
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
                if (retVal != d.testExitCode) {
                    fail(d.testAppName
                            + ": Unexpected 'success' exit; expected:"
                            + d.testExitCode + " was:" + retVal);
                }
                return;
            case 1:
                fail(d.testAppName
                        + ": unable to launch java application");
            case ERROR_TOOLKIT_NOT_RUNNING:
                fail(d.testAppName
                        + ": Toolkit not running prior to loading application class");
            case ERROR_TOOLKIT_IS_RUNNING:
                fail(d.testAppName
                        + ": Toolkit is running but should not be");

            case ERROR_INIT_BEFORE_MAIN:
                fail(d.testAppName
                        + ": main method not called before init");
            case ERROR_START_BEFORE_MAIN:
                fail(d.testAppName
                        + ": main method not called before start");
            case ERROR_STOP_BEFORE_MAIN:
                fail(d.testAppName
                        + ": main method not called before stop");

            case ERROR_START_BEFORE_INIT:
                fail(d.testAppName
                        + ": init method not called before start");
            case ERROR_STOP_BEFORE_INIT:
                fail(d.testAppName
                        + ": init method not called before stop");

            case ERROR_STOP_BEFORE_START:
                fail(d.testAppName
                        + ": start method not called before stop");

            case ERROR_CLASS_INIT_WRONG_THREAD:
                fail(d.testAppName
                        + ": class initialization called on wrong thread");
            case ERROR_MAIN_WRONG_THREAD:
                fail(d.testAppName
                        + ": main called on wrong thread");
            case ERROR_CONSTRUCTOR_WRONG_THREAD:
                fail(d.testAppName
                        + ": constructor called on wrong thread");
            case ERROR_INIT_WRONG_THREAD:
                fail(d.testAppName
                        + ": init called on wrong thread");
            case ERROR_START_WRONG_THREAD:
                fail(d.testAppName
                        + ": start called on wrong thread");
            case ERROR_STOP_WRONG_THREAD:
                fail(d.testAppName
                        + ": stop called on wrong thread");

            case ERROR_PRELOADER_CLASS_INIT_WRONG_THREAD:
                fail(d.testAppName
                        + ": preloader class initialization called on wrong thread");
            case ERROR_PRELOADER_CONSTRUCTOR_WRONG_THREAD:
                fail(d.testAppName
                        + ": preloader constructor called on wrong thread");
            case ERROR_PRELOADER_INIT_WRONG_THREAD:
                fail(d.testAppName
                        + ": preloader init called on wrong thread");
            case ERROR_PRELOADER_START_WRONG_THREAD:
                fail(d.testAppName
                        + ": preloader start called on wrong thread");
            case ERROR_PRELOADER_STOP_WRONG_THREAD:
                fail(d.testAppName
                        + ": preloader stop called on wrong thread");

            case ERROR_CONSTRUCTOR_WRONG_CCL:
                fail(d.testAppName
                        + ": constructor has wrong CCL");
            case ERROR_START_WRONG_CCL:
                fail(d.testAppName
                        + ": start has wrong CCL");
            case ERROR_LAUNCH_SUCCEEDED:
                fail(d.testAppName
                + ": Application.launch unexpectedly succeeded");
            case ERROR_STARTUP_SUCCEEDED:
                fail(d.testAppName
                + ": Plataform.startup unexpectedly succeeded");
            case ERROR_STARTUP_FAILED:
                fail(d.testAppName
                + ": Plataform.startup failed");

            case ERROR_ASSERTION_FAILURE:
                fail(d.testAppName
                + ": Assertion failure in test application");

            case ERROR_UNEXPECTED_EXCEPTION:
                fail(d.testAppName
                + ": unexpected exception");

            default:
                fail(d.testAppName
                        + ": Unexpected error exit: " + retVal);
        }
    }
}
