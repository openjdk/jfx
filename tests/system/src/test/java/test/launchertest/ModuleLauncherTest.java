/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.util.ArrayList;
import junit.framework.AssertionFailedError;
import org.junit.Test;

import static org.junit.Assert.*;
import static test.launchertest.Constants.*;

/**
 * Unit test for launching modular FX applications
 */
public class ModuleLauncherTest {

    private static final String modulePath2 = System.getProperty("launchertest.testapp2.module.path");
    private static final String modulePath3 = System.getProperty("launchertest.testapp3.module.path");
    private static final String modulePath4 = System.getProperty("launchertest.testapp4.module.path");
    private static final String modulePath5 = System.getProperty("launchertest.testapp5.module.path");
    private static final String modulePath6 = System.getProperty("launchertest.testapp6.module.path");
    private static final String modulePathScript1 = System.getProperty("launchertest.testscriptapp1.module.path");
    private static final String modulePathScript2 = System.getProperty("launchertest.testscriptapp2.module.path");

    private static final String moduleName = "mymod";

    private final int testExitCode = ERROR_NONE;

    private void doTestLaunchModule(String appModulePath, String testAppName) throws Exception {
        final String javafxModulePath = System.getProperty("worker.module.path");
        String modulePath;
        if (javafxModulePath != null) {
            modulePath = javafxModulePath + File.pathSeparator + appModulePath;
        } else {
            modulePath = appModulePath;
        }
        assertNotNull(testAppName);
        System.err.println("The following Unknown module WARNING messages are expected:");
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

            case ERROR_ASSERTION_FAILURE:
                throw new AssertionFailedError(testAppName
                + ": Assertion failure in test application");

            case ERROR_UNEXPECTED_EXCEPTION:
                throw new AssertionFailedError(testAppName
                + ": unexpected exception");

            default:
                throw new AssertionFailedError(testAppName
                        + ": Unexpected error exit: " + retVal);
        }
    }


    @Test (timeout = 15000)
    public void testLaunchModule() throws Exception {
        doTestLaunchModule(modulePath2, "testapp.TestApp");
    }

    @Test (timeout = 15000)
    public void testLaunchModuleNoMain() throws Exception {
        doTestLaunchModule(modulePath2, "testapp.TestAppNoMain");
    }

    @Test (timeout = 15000)
    public void testLaunchModuleNotApplication() throws Exception {
        doTestLaunchModule(modulePath2, "testapp.TestNotApplication");
    }

    @Test (timeout = 15000)
    public void testModuleTableViewUnexported() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTableViewUnexported");
    }

    @Test (timeout = 15000)
    public void testModuleTableViewExported() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTableViewExported");
    }

    @Test (timeout = 15000)
    public void testModuleTableViewQualExported() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTableViewQualExported");
    }

    @Test (timeout = 15000)
    public void testModuleTableViewOpened() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTableViewOpened");
    }

    @Test (timeout = 15000)
    public void testModuleTableViewQualOpened() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTableViewQualOpened");
    }

    @Test (timeout = 15000)
    public void testModuleTreeTableViewUnexported() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTreeTableViewUnexported");
    }

    @Test (timeout = 15000)
    public void testModuleTreeTableViewExported() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTreeTableViewExported");
    }

    @Test (timeout = 15000)
    public void testModuleTreeTableViewQualExported() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTreeTableViewQualExported");
    }

    @Test (timeout = 15000)
    public void testModuleTreeTableViewOpened() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTreeTableViewOpened");
    }

    @Test (timeout = 15000)
    public void testModuleTreeTableViewQualOpened() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTreeTableViewQualOpened");
    }

    @Test (timeout = 15000)
    public void testModuleBeansUnexported() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBeansUnexported");
    }

    @Test (timeout = 15000)
    public void testModuleBeansExported() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBeansExported");
    }

    @Test (timeout = 15000)
    public void testModuleBeansQualExported() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBeansQualExported");
    }

    @Test (timeout = 15000)
    public void testModuleBeansOpened() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBeansOpened");
    }

    @Test (timeout = 15000)
    public void testModuleBeansQualOpened() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBeansQualOpened");
    }

    @Test (timeout = 15000)
    public void testModuleBindingsUnexported() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBindingsUnexported");
    }

    @Test (timeout = 15000)
    public void testModuleBindingsExported() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBindingsExported");
    }

    @Test (timeout = 15000)
    public void testModuleBindingsQualExported() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBindingsQualExported");
    }

    @Test (timeout = 15000)
    public void testModuleBindingsOpened() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBindingsOpened");
    }

    @Test (timeout = 15000)
    public void testModuleBindingsQualOpened() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBindingsQualOpened");
    }

    @Test (timeout = 15000)
    public void testModuleJSCallbackUnexported() throws Exception {
        doTestLaunchModule(modulePath5, "myapp5.AppJSCallbackUnexported");
    }

    @Test (timeout = 15000)
    public void testModuleJSCallbackExported() throws Exception {
        doTestLaunchModule(modulePath5, "myapp5.AppJSCallbackExported");
    }

    @Test (timeout = 15000)
    public void testModuleJSCallbackQualExported() throws Exception {
        doTestLaunchModule(modulePath5, "myapp5.AppJSCallbackQualExported");
    }

    @Test (timeout = 15000)
    public void testModuleJSCallbackOpened() throws Exception {
        doTestLaunchModule(modulePath5, "myapp5.AppJSCallbackOpened");
    }

    @Test (timeout = 15000)
    public void testModuleJSCallbackQualOpened() throws Exception {
        doTestLaunchModule(modulePath5, "myapp5.AppJSCallbackQualOpened");
    }

    @Test (timeout = 15000)
    public void testModuleFXMLUnexported() throws Exception {
        doTestLaunchModule(modulePath6, "myapp6.AppFXMLUnexported");
    }

    @Test (timeout = 15000)
    public void testModuleFXMLExported() throws Exception {
        doTestLaunchModule(modulePath6, "myapp6.AppFXMLExported");
    }

    @Test (timeout = 15000)
    public void testModuleFXMLQualExported() throws Exception {
        doTestLaunchModule(modulePath6, "myapp6.AppFXMLQualExported");
    }

    @Test (timeout = 15000)
    public void testModuleFXMLOpened() throws Exception {
        doTestLaunchModule(modulePath6, "myapp6.AppFXMLOpened");
    }

    @Test (timeout = 15000)
    public void testModuleFXMLQualOpened() throws Exception {
        doTestLaunchModule(modulePath6, "myapp6.AppFXMLQualOpened");
    }

    @Test (timeout = 15000)
    public void testFXMLScriptDeployment() throws Exception {
        doTestLaunchModule(modulePathScript1, "myapp1.FXMLScriptDeployment");
    }

    @Test (timeout = 15000)
    public void testFXMLScriptDeployment2Compile_On() throws Exception {
        doTestLaunchModule(modulePathScript2, "myapp2.FXMLScriptDeployment2Compile_On");
    }

    @Test (timeout = 15000)
    public void testFXMLScriptDeployment2Compile_Off() throws Exception {
        doTestLaunchModule(modulePathScript2, "myapp2.FXMLScriptDeployment2Compile_Off");
    }

    @Test (timeout = 15000)
    public void testFXMLScriptDeployment2Compile_On_Off() throws Exception {
        doTestLaunchModule(modulePathScript2, "myapp2.FXMLScriptDeployment2Compile_On_Off");
    }

    @Test (timeout = 15000)
    public void testFXMLScriptDeployment2Compile_Off_On() throws Exception {
        doTestLaunchModule(modulePathScript2, "myapp2.FXMLScriptDeployment2Compile_Off_On");
    }
    @Test (timeout = 15000)
    public void testFXMLScriptDeployment2Compile_Fail_Compilation() throws Exception {
        doTestLaunchModule(modulePathScript2, "myapp2.FXMLScriptDeployment2Compile_Fail_Compilation");
    }
}
