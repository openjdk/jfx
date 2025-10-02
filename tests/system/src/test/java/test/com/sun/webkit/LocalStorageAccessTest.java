/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.sun.javafx.PlatformUtil;

/**
 * @test
 * @bug 8264990
 * @summary Check if access to local storage works without causing a segfault
 */
@Timeout(value=15000, unit=TimeUnit.MILLISECONDS)
public class LocalStorageAccessTest {
    @Test
    public void testMainThreadDoesNotSegfault() throws Exception {
        if (PlatformUtil.isWindows()) {
            assumeTrue(Boolean.getBoolean("unstable.test")); // JDK-8265661
        }

        // This is an indirect test of the webkit file system implementation.
        // It was observed, that accessing local storage causes a segfault
        // in the JVM. That case is executed by this test.
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

        final List<String> cmd = new ArrayList<>();
        cmd.add(workerJavaCmd);

        cmd.addAll(List.of(
            "--enable-native-access=ALL-UNNAMED",
            "-cp", appModulePath + "/mymod",
            "-Djava.library.path=" + javaLibraryPath,
            "-Dmodule.path=" + appModulePath + "/mymod" + File.pathSeparator + workerModulePath,
            "myapp7.LocalStorageAccessWithModuleLayerLauncher"
        ));

        final ProcessBuilder builder = new ProcessBuilder(cmd);

        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        Process process = builder.start();
        int retVal = process.waitFor();

        assertEquals(0, retVal, "Process did not exit cleanly");
    }
}
