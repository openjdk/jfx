/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.webkit;

import java.io.File;
import static java.util.Arrays.asList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @test
 * @bug 8242361
 * @summary Check if webkit main thread <-> java integration works correctly
 */
public class MainThreadTest {
    @Test (timeout = 15000)
    public void testMainThreadDoesNotSegfault() throws Exception {
        // This is an indirect test of the webkit main thread <-> java
        // integration. It was observed, that using a data-url caused the
        // JVM to segfault. That case is executed by this test:
        //
        // A new JVM is started with a custom launcher (classpath based). This
        // launcher sets up the module layer required for OpenJFX and starts
        // the test application. That way the OpenJFX classes are not loaded by
        // the system class loader, but by the classloader that is associated
        // with the new module layer.
        //

        final String appModulePath = System.getProperty("launchertest.testapp7.module.path");
        final String workerModulePath = System.getProperty("worker.module.path");
        final String javaLibraryPath = System.getProperty("java.library.path");
        final String workerJavaCmd = System.getProperty("worker.java.cmd");

        final List<String> cmd = asList(
            workerJavaCmd,
            "-cp", appModulePath + "/mymod",
            "-Djava.library.path=" + javaLibraryPath,
            "-Dmodule.path=" + appModulePath + "/mymod" + File.pathSeparator + workerModulePath,
            "myapp7.DataUrlWithModuleLayerLauncher"
        );

        final ProcessBuilder builder = new ProcessBuilder(cmd);

        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        Process process = builder.start();
        int retVal = process.waitFor();

        assertEquals("Process did not exit cleanly", 0, retVal);
    }
}
