/*
 * Copyright (c) 2017, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static test.launchertest.Constants.ERROR_ASSERTION_FAILURE;
import static test.launchertest.Constants.ERROR_NONE;
import static test.launchertest.Constants.ERROR_TOOLKIT_IS_RUNNING;
import static test.launchertest.Constants.ERROR_TOOLKIT_NOT_RUNNING;
import static test.launchertest.Constants.ERROR_UNEXPECTED_EXCEPTION;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Unit test for launching modular FX applications
 */
@Timeout(value=15000, unit=TimeUnit.MILLISECONDS)
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
                    fail(testAppName
                            + ": Unexpected 'success' exit; expected:"
                            + testExitCode + " was:" + retVal);
                }
                return;

            case 1:
                fail(testAppName
                        + ": unable to launch java application");

            case ERROR_TOOLKIT_NOT_RUNNING:
                fail(testAppName
                        + ": Toolkit not running prior to loading application class");
            case ERROR_TOOLKIT_IS_RUNNING:
                fail(testAppName
                        + ": Toolkit is running but should not be");

            case ERROR_ASSERTION_FAILURE:
                fail(testAppName
                + ": Assertion failure in test application");

            case ERROR_UNEXPECTED_EXCEPTION:
                fail(testAppName
                + ": unexpected exception");

            default:
                fail(testAppName
                        + ": Unexpected error exit: " + retVal);
        }
    }

    @Test
    public void testLaunchModule() throws Exception {
        doTestLaunchModule(modulePath2, "testapp.TestApp");
    }

    @Test
    public void testLaunchModuleNoMain() throws Exception {
        doTestLaunchModule(modulePath2, "testapp.TestAppNoMain");
    }

    @Test
    public void testLaunchModuleNotApplication() throws Exception {
        doTestLaunchModule(modulePath2, "testapp.TestNotApplication");
    }

    @Test
    public void testModuleTableViewUnexported() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTableViewUnexported");
    }

    @Test
    public void testModuleTableViewExported() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTableViewExported");
    }

    @Test
    public void testModuleTableViewQualExported() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTableViewQualExported");
    }

    @Test
    public void testModuleTableViewOpened() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTableViewOpened");
    }

    @Test
    public void testModuleTableViewQualOpened() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTableViewQualOpened");
    }

    @Test
    public void testModuleTreeTableViewUnexported() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTreeTableViewUnexported");
    }

    @Test
    public void testModuleTreeTableViewExported() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTreeTableViewExported");
    }

    @Test
    public void testModuleTreeTableViewQualExported() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTreeTableViewQualExported");
    }

    @Test
    public void testModuleTreeTableViewOpened() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTreeTableViewOpened");
    }

    @Test
    public void testModuleTreeTableViewQualOpened() throws Exception {
        doTestLaunchModule(modulePath3, "myapp3.AppTreeTableViewQualOpened");
    }

    @Test
    public void testModuleBeansUnexported() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBeansUnexported");
    }

    @Test
    public void testModuleBeansExported() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBeansExported");
    }

    @Test
    public void testModuleBeansQualExported() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBeansQualExported");
    }

    @Test
    public void testModuleBeansOpened() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBeansOpened");
    }

    @Test
    public void testModuleBeansQualOpened() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBeansQualOpened");
    }

    @Test
    public void testModuleBindingsUnexported() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBindingsUnexported");
    }

    @Test
    public void testModuleBindingsExported() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBindingsExported");
    }

    @Test
    public void testModuleBindingsQualExported() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBindingsQualExported");
    }

    @Test
    public void testModuleBindingsOpened() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBindingsOpened");
    }

    @Test
    public void testModuleBindingsQualOpened() throws Exception {
        doTestLaunchModule(modulePath4, "myapp4.AppBindingsQualOpened");
    }

    @Test
    public void testModuleJSCallbackUnexported() throws Exception {
        doTestLaunchModule(modulePath5, "myapp5.AppJSCallbackUnexported");
    }

    @Test
    public void testModuleJSCallbackExported() throws Exception {
        doTestLaunchModule(modulePath5, "myapp5.AppJSCallbackExported");
    }

    @Test
    public void testModuleJSCallbackQualExported() throws Exception {
        doTestLaunchModule(modulePath5, "myapp5.AppJSCallbackQualExported");
    }

    @Test
    public void testModuleJSCallbackOpened() throws Exception {
        doTestLaunchModule(modulePath5, "myapp5.AppJSCallbackOpened");
    }

    @Test
    public void testModuleJSCallbackQualOpened() throws Exception {
        doTestLaunchModule(modulePath5, "myapp5.AppJSCallbackQualOpened");
    }

    @Test
    public void testModuleFXMLUnexported() throws Exception {
        doTestLaunchModule(modulePath6, "myapp6.AppFXMLUnexported");
    }

    @Test
    public void testModuleFXMLExported() throws Exception {
        doTestLaunchModule(modulePath6, "myapp6.AppFXMLExported");
    }

    @Test
    public void testModuleFXMLQualExported() throws Exception {
        doTestLaunchModule(modulePath6, "myapp6.AppFXMLQualExported");
    }

    @Test
    public void testModuleFXMLOpened() throws Exception {
        doTestLaunchModule(modulePath6, "myapp6.AppFXMLOpened");
    }

    @Test
    public void testModuleFXMLQualOpened() throws Exception {
        doTestLaunchModule(modulePath6, "myapp6.AppFXMLQualOpened");
    }

    @Test
    public void testFXMLScriptDeployment() throws Exception {
        doTestLaunchModule(modulePathScript1, "myapp1.FXMLScriptDeployment");
    }

    @Test
    public void testFXMLScriptDeployment2Compile_On() throws Exception {
        doTestLaunchModule(modulePathScript2, "myapp2.FXMLScriptDeployment2Compile_On");
    }

    @Test
    public void testFXMLScriptDeployment2Compile_Off() throws Exception {
        doTestLaunchModule(modulePathScript2, "myapp2.FXMLScriptDeployment2Compile_Off");
    }

    @Test
    public void testFXMLScriptDeployment2Compile_On_Off() throws Exception {
        doTestLaunchModule(modulePathScript2, "myapp2.FXMLScriptDeployment2Compile_On_Off");
    }

    @Test
    public void testFXMLScriptDeployment2Compile_Off_On() throws Exception {
        doTestLaunchModule(modulePathScript2, "myapp2.FXMLScriptDeployment2Compile_Off_On");
    }

    @Test
    public void testFXMLScriptDeployment2Compile_Fail_Compilation() throws Exception {
        doTestLaunchModule(modulePathScript2, "myapp2.FXMLScriptDeployment2Compile_Fail_Compilation");
    }
}
