/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.glass.ui.gtk;

import com.sun.javafx.PlatformUtil;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;

import static org.junit.Assert.fail;

public class Gtk2RemovalCommon {

    private static final CountDownLatch startupLatch = new CountDownLatch(1);
    private static final PrintStream defaultErrorStream = System.err;
    protected static final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public static void doSetup(boolean forceGtk2) throws Exception {
        if (!PlatformUtil.isLinux()) return;

        if (forceGtk2) {
            System.setProperty("jdk.gtk.version", "2");
        }

        System.setErr(new PrintStream(out, true));

        Platform.startup(() -> {
            startupLatch.countDown();
        });

        if (!startupLatch.await(15, TimeUnit.SECONDS)) {
            System.setErr(defaultErrorStream);
            System.err.println(out.toString());
            fail("Timeout waiting for FX runtime to start");
        }

        Thread.sleep(250);
        System.setErr(defaultErrorStream);
    }

    public static void doTeardown() {
        if (!PlatformUtil.isLinux()) return;

        Platform.exit();
    }

}
