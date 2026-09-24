/*
 * Copyright (c) 2017, 2026, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import test.util.Util;

@Timeout(value=15000, unit=TimeUnit.MILLISECONDS)
public class SwingNodeJDialogTest extends SwingNodeBase {

    // SwingNode used to pass the JavaFX stage's native window handle to JLightweightFrame
    // (Application.overrideNativeWindowHandle, a JNI call into a non-public JDK method), which made the
    // stage the native owner of a JDialog parented to the SwingNode content and kept the dialog above it.
    // Commit 8492cb03b0 removed that call, so the tests asserting the dialog is on top no longer hold.
    private static final String NO_NATIVE_OWNER =
            "JDialog is not natively owned by the JavaFX stage since 8492cb03b0 removed overrideNativeWindowHandle";

    @BeforeAll
    public static void init() throws Exception {
        if (Util.isOnWayland()) {
            assumeTrue(Runtime.version().feature() >= 24);
        }
    }

    @Disabled(NO_NATIVE_OWNER)
    @Test
    public void testJDialogAbove()throws InterruptedException, InvocationTargetException {
        myApp.createStageAndDialog();
        myApp.showDialog();

        testAbove(true);

        myApp.closeStageAndDialog();
    }

    @Disabled(NO_NATIVE_OWNER)
    @Test
    public void testNodeRemovalAfterShow()throws InterruptedException, InvocationTargetException {
        myApp.createStageAndDialog();
        myApp.showDialog();

        testAbove(true);

        myApp.detachSwingNode();
        testAbove(false);

        myApp.closeStageAndDialog();
        myApp.attachSwingNode();
    }

    @Test
    public void testNodeRemovalBeforeShow() throws InterruptedException, InvocationTargetException {
        myApp.createStageAndDialog();
        myApp.detachSwingNode();
        myApp.showDialog();

        testAbove(false);

        myApp.closeStageAndDialog();
        myApp.attachSwingNode();
    }

    @Disabled(NO_NATIVE_OWNER)
    @Test
    public void testStageCloseAfterShow()throws InvocationTargetException, InterruptedException {
        myApp.createStageAndDialog();
        myApp.showDialog();
        testAbove(true);
        myApp.closeStage();
        myApp.disposeDialog();
    }

    @Disabled(NO_NATIVE_OWNER)
    @Test
    public void testStageCloseBeforeShow()throws InvocationTargetException, InterruptedException {
        myApp.createStageAndDialog();
        myApp.closeStage();
        myApp.showDialog();
        testAbove(true);
        myApp.disposeDialog();
    }


    @Test
    public void testNodeRemovalBeforeShowHoldEDT() throws InterruptedException, InvocationTargetException {
        myApp.createAndShowStage();
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(()-> {
            myApp.createDialogRunnable.run();
            latch.countDown();
            Util.sleep(LONG_WAIT_TIME);
            myApp.dialog.setVisible(true);
        });
        latch.await();
        myApp.detachSwingNode();
        testAbove(false);
        myApp.closeStageAndDialog();
        myApp.attachSwingNode();
    }

    @Test
    public void testStageCloseBeforeShowHoldEDT() throws InvocationTargetException, InterruptedException {
        myApp.createAndShowStage();
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(()-> {
            myApp.createDialogRunnable.run();
            latch.countDown();
            Util.sleep(LONG_WAIT_TIME);
            myApp.dialog.setVisible(true);
        });
        latch.await();
        myApp.closeStage();
        testAbove(false);
        myApp.disposeDialog();
    }
}
