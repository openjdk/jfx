/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import junit.framework.AssertionFailedError;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.Test;

import static launchertest.Constants.*;

/**
 * Unit test for FX support in Java 8 launcher
 */
@RunWith(Parameterized.class)
public class MainLauncherTest {

    private static Collection params = null;

    private static final String[] testAppNames = {
        "launchertest.TestApp",
        "launchertest.TestAppNoMain",
        "launchertest.TestNotApplication"
    };

    @Parameters
    public static Collection getParams() {
        if (params == null) {
            params = new ArrayList();
            for (String name : testAppNames) {
                params.add(new String[] { name });
            }
        }
        return params;
    }

    private String testAppName;

    public MainLauncherTest(String testAppName) {
        this.testAppName = testAppName;
    }

    @Test (timeout=5000)
    public void testMainLauncher() throws Exception {
        final String classpath = System.getProperty("java.class.path");
        ProcessBuilder builder = new ProcessBuilder("java", "-cp", classpath, testAppName);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        Process process = builder.start();
        int retVal = process.waitFor();
        switch (retVal) {
            case 0:
                // SUCCESS
                return;
            case ERROR_START_BEFORE_MAIN:
                throw new AssertionFailedError(testAppName
                        + ": main method not called before start");
            case ERROR_STOP_BEFORE_MAIN:
                throw new AssertionFailedError(testAppName
                        + ": main method not called before stop");
            case ERROR_STOP_BEFORE_START:
                throw new AssertionFailedError(testAppName
                        + ": start method not called before stop");
            case ERROR_TOOLKIT_NOT_RUNNING:
                throw new AssertionFailedError(testAppName
                        + ": Toolkit not running prior to loading application class");
            case ERROR_TOOLKIT_IS_RUNNING:
                throw new AssertionFailedError(testAppName
                        + ": Toolkit is running but should not be");
            default:
                throw new AssertionFailedError(testAppName
                        + ": Unexpected error exit: " + retVal);
        }
    }

}
