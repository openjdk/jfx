/*
 * Copyright (c) 2017, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.javafx.embed.swing;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import javax.swing.SwingUtilities;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import test.util.Util;

public class SwingNodePlatformExitCrashTest extends SwingNodeBase {

    @Test
    public void testPlatformExitBeforeShowHoldEDT() throws InvocationTargetException, InterruptedException {
        myApp.createAndShowStage();
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(()-> {
            myApp.createDialogRunnable.run();
            latch.countDown();
            Util.sleep(LONG_WAIT_TIME);
            myApp.dialog.setVisible(true);
        });
        latch.await();
        testAbove(false);
        runWaitSleep(()-> Platform.exit());
        myApp.disposeDialog();
    }

    @BeforeAll
    public static void skipShutDown() {
        // This test requires JDK 24 or later on Wayland
        Assumptions.assumeTrue(!Util.isOnWayland() || Runtime.version().feature() >= 24);
        // Skip shutdown as Toolkit shutdown is already done in Platform.exit
        // and will cause hang if called
        doShutdown = false;
    }
}
