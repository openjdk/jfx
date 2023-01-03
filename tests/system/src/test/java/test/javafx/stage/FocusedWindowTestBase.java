/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.stage;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.junit.Assert;

import test.util.Util;

public abstract class FocusedWindowTestBase {

    static CountDownLatch startupLatch = new CountDownLatch(1);

    public static void initFXBase() throws Exception {
        Platform.setImplicitExit(false);
        Util.startup(startupLatch, startupLatch::countDown);
    }

    WeakReference<Stage> closedFocusedStageWeak = null;
    Stage closedFocusedStage = null;

    public void testClosedFocusedStageLeakBase() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Util.runAndWait(() -> {
            closedFocusedStage = new Stage();
            closedFocusedStage.setTitle("Focused Stage");
            closedFocusedStageWeak = new WeakReference<>(closedFocusedStage);
            TextField textField = new TextField();
            closedFocusedStage.setScene(new Scene(textField));
            closedFocusedStage.setOnShown(l -> {
                latch.countDown();
            });
            closedFocusedStage.show();
        });
        Assert.assertTrue("Timeout waiting for closedFocusedStage to show`",
                latch.await(15, TimeUnit.MILLISECONDS));

        CountDownLatch hideLatch = new CountDownLatch(1);
        closedFocusedStage.setOnHidden(a -> {
            hideLatch.countDown();
        });
        Util.runAndWait(() -> closedFocusedStage.close());
        Assert.assertTrue("Timeout waiting for closedFocusedStage to hide`",
                hideLatch.await(15, TimeUnit.MILLISECONDS));

        closedFocusedStage.requestFocus();
        closedFocusedStage = null;
        assertCollectable(closedFocusedStageWeak);
    }

    public static void assertCollectable(WeakReference weakReference) throws Exception {
        int counter = 0;

        System.gc();

        while (counter < 10 && weakReference.get() != null) {
            Thread.sleep(100);
            counter = counter + 1;
            System.gc();
        }

        Assert.assertNull(weakReference.get());
    }
}
