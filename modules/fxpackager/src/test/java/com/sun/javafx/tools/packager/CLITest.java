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

package com.sun.javafx.tools.packager;

import org.junit.*;

import java.io.File;

public class CLITest {

    static File tmpBase;
    static File workDir;
    static File appResourcesDir;
    static File fakeMainJar;

    @BeforeClass
    public static void prepareApp() {
        // on mac, require a full test
        Assume.assumeTrue(!System.getProperty("os.name").toLowerCase().contains("os x") || Boolean.parseBoolean(System.getProperty("FULL_TEST")));


        Log.setLogger(new Log.Logger(true));

        String tmpBasePath = System.getProperty("tmpBase");
        if (tmpBasePath != null) {
            tmpBase = new File(System.getProperty("tmpBase"));
        } else {
            tmpBase = new File("build/tmp/tests");
        }
        tmpBase.mkdirs();

        workDir = new File(tmpBase, "cliapp");
        appResourcesDir = new File(tmpBase, "appResources");
        fakeMainJar = new File(appResourcesDir, "mainApp.jar");
    }

    @Test
    public void simpleTest() throws Exception {
        com.sun.javafx.tools.packager.Main.main("-deploy",
                "-verbose", // verbose is required or test will call System.exit() on failures and break the build
                "-srcfiles", fakeMainJar.getCanonicalPath(),
                "-outdir", workDir.getCanonicalPath(),
                "-outfile", "SimpleTest",
                "-appclass", "hello.TestPackager",
                "-native",
                "-name", "SimpleTest");
    }

    @Test(expected = PackagerException.class)
    public void duplicateNameClash() throws Exception {
        com.sun.javafx.tools.packager.Main.main("-deploy",
                "-verbose", // verbose is required or test will call System.exit() on failures and break the build
                "-srcfiles", fakeMainJar.getCanonicalPath(),
                "-outdir", workDir.getCanonicalPath(),
                "-outfile", "SimpleTest",
                "-appclass", "hello.TestPackager",
                "-native",
                "-name", "SimpleTest",
                "-Bname=DuplicateTest");
    }

    @Test(expected = PackagerException.class)
    public void duplicateNameMatch() throws Exception {
        com.sun.javafx.tools.packager.Main.main("-deploy",
                "-verbose", // verbose is required or test will call System.exit() on failures and break the build
                "-srcfiles", fakeMainJar.getCanonicalPath(),
                "-outdir", workDir.getCanonicalPath(),
                "-outfile", "SimpleTest",
                "-appclass", "hello.TestPackager",
                "-native",
                "-name", "SimpleTest",
                "-Bname=SimpleTest");
    }

}
