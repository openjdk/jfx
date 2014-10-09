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

package com.oracle.tools.packager;

import com.sun.javafx.tools.packager.PackagerException;
import org.junit.*;

import java.io.File;

public class CLITest {

    static File tmpBase;
    static File workDir;
    static File appResourcesDir;
    static File fakeMainJar;

    @BeforeClass
    public static void prepareApp() {
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
        // on mac, require a full test
        Assume.assumeTrue(!System.getProperty("os.name").toLowerCase().contains("os x") || Boolean.parseBoolean(System.getProperty("FULL_TEST")));

        com.sun.javafx.tools.packager.Main.main("-deploy",
                "-verbose", // verbose is required or test will call System.exit() on failures and break the build
                "-srcfiles", fakeMainJar.getCanonicalPath(),
                "-outdir", workDir.getCanonicalPath(),
                "-outfile", "SimpleTest",
                "-appclass", "hello.HelloRectangle",
                "-native",
                "-name", "SimpleTest");
    }

    @Test
    public void smokeParams() throws Exception {

        String packagerJdkRoot = System.getenv("PACKAGER_JDK_ROOT");
        String runtime = packagerJdkRoot == null
                ? System.getProperty("java.home")
                : packagerJdkRoot;

        com.sun.javafx.tools.packager.Main.main("-deploy",
                "-verbose", // verbose is required or test will call System.exit() on failures and break the build
                "-srcfiles", fakeMainJar.getCanonicalPath(),
                "-outdir", workDir.getCanonicalPath(),
                "-outfile", "SmokeParams",
                "-appclass", "hello.HelloRectangle",
                "-nosign",
                "-native", "image",
                "-name", "SmokeParams",
                "-BOptionThatWillNeverExist=true",
                "-BuserJvmOptions=-Xmx1g",
                "-BuserJvmOptions=-Xms512m",
                "-BdesktopHint=false",
                "-BshortcutHint=true",
                "-Bruntime=" + runtime);
    }

    @Test
    public void propsViaBundlerArgs() throws Exception {
        com.sun.javafx.tools.packager.Main.main("-deploy",
                "-verbose", // verbose is required or test will call System.exit() on failures and break the build
                "-srcfiles", fakeMainJar.getCanonicalPath(),
                "-outdir", workDir.getCanonicalPath(),
                "-outfile", "PropsViaBundlerArgs",
                "-appclass", "hello.HelloRectangle",
                "-native", "image",
                "-name", "PropsViaBundlerArgs",
                "-BjvmOptions=-Dsqe.foo.bar=baz -Dsqe.qux.corge=grault",
                "-BuserJvmOptions=-Xmx=1g\n-Xms=512m",
                "-BjvmProperties=sqe.aba.caba=dabacaba"
        );
    }

    @Test(expected = PackagerException.class)
    public void duplicateNameClash() throws Exception {
        com.sun.javafx.tools.packager.Main.main("-deploy",
                "-verbose", // verbose is required or test will call System.exit() on failures and break the build
                "-srcfiles", fakeMainJar.getCanonicalPath(),
                "-outdir", workDir.getCanonicalPath(),
                "-outfile", "DuplicateNameClash",
                "-appclass", "hello.HelloRectangle",
                "-native", "image",
                "-name", "DuplicateNameClash",
                "-Bname=DuplicateTest");
    }

    @Test(expected = PackagerException.class)
    public void duplicateNameMatch() throws Exception {
        com.sun.javafx.tools.packager.Main.main("-deploy",
                "-verbose", // verbose is required or test will call System.exit() on failures and break the build
                "-srcfiles", fakeMainJar.getCanonicalPath(),
                "-outdir", workDir.getCanonicalPath(),
                "-outfile", "DuplicateNameMatch",
                "-appclass", "hello.HelloRectangle",
                "-native", "image",
                "-name", "DuplicateNameMatch",
                "-Bname=DuplicateNameMatch");
    }

    @Test(expected = PackagerException.class)
    public void invalidSrcDir() throws Exception {
        com.sun.javafx.tools.packager.Main.main("-deploy",
                "-verbose", // verbose is required or test will call System.exit() on failures and break the build
                "-srcdir", fakeMainJar.getCanonicalPath(), // should be a directory, not a jar
                "-outdir", workDir.getCanonicalPath(),
                "-outfile", "InvalidSrcDir",
                "-appclass", "hello.HelloRectangle",
                "-native", "image",
                "-name", "InvalidSrcDir");
    }
}
