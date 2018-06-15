/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tools.packager;

import java.io.File;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

public class DeployParamsTest {

    private File testRoot = null;

    @Before
    public void setUp() {
        testRoot = new File("build/tmp/tests/deployParamsTest");
        testRoot.mkdirs();
    }

    @After
    public void tearDown() {
        if (testRoot != null) {
            testRoot.delete();
        }
    }

    @Test
    public void testValidateAppName1() throws PackagerException {
        DeployParams params = getParamsAppName();

        params.setAppName("Test");
        params.validate();

        params.setAppName("Test Name");
        params.validate();

        params.setAppName("Test - Name !!!");
        params.validate();
    }

    @Test
    public void testValidateAppName2() throws PackagerException {
        DeployParams params = getParamsAppName();

        params.setAppName("Test\nName");
        appName2TestHelper(params);

        params.setAppName("Test\rName");
        appName2TestHelper(params);

        params.setAppName("TestName\\");
        appName2TestHelper(params);

        params.setAppName("Test \" Name");
        appName2TestHelper(params);
    }

    private void appName2TestHelper(DeployParams params) {
        try {
            params.validate();
            fail("An exception should have been thrown");
        } catch (PackagerException pe) { }
    }

    // Returns deploy params initialized to pass all validation, except for
    // app name
    private DeployParams getParamsAppName() {
        DeployParams params = new DeployParams();
        params.setOutdir(testRoot);
        params.setOutfile("Test");
        params.addResource(testRoot, new File(testRoot, "test.jar"));
        params.setApplicationClass("TestClass");
        return params;
    }

}
