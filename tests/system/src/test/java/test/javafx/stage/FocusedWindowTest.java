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

package test.javafx.stage;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

public class FocusedWindowTest {

    static CountDownLatch startupLatch;
    static Stage stage = null;

    static {
        System.setProperty("glass.platform","Monocle");
        System.setProperty("monocle.platform","Headless");
    }

    public static class TestApp extends Application {

        @Override
        public void start(Stage primaryStage) throws Exception {
            primaryStage.setTitle("Primary Stage");
            primaryStage.setScene(new Scene(new TextField()));

            primaryStage.setOnShown(l -> {
                Platform.runLater(() -> startupLatch.countDown());
            });
            primaryStage.show();
            Platform.setImplicitExit(false);
            stage = primaryStage;
        }
    }

    @BeforeClass
    public static void initFX() throws Exception {
        startupLatch = new CountDownLatch(1);
        new Thread(() -> Application.launch(TestApp.class, (String[]) null)).start();
        Assert.assertTrue("Timeout waiting for FX runtime to start", startupLatch.await(15, TimeUnit.SECONDS));
        Platform.runLater(() -> stage.close());
    }

    static WeakReference<Stage> closedFocusedStageWeak = null;
    static Stage closedFocusedStage = null;

    @Test
    public void testLeak() throws Exception {
        Util.runAndWait(() -> {
            closedFocusedStage = new Stage();
            closedFocusedStage.setTitle("Focused Stage");
            closedFocusedStageWeak = new WeakReference<>(closedFocusedStage);
            TextField textField = new TextField();
            closedFocusedStage.setScene(new Scene(textField));
        });
        Util.runAndWait(() -> {
            closedFocusedStage.show();
        });
        Util.runAndWait(() -> {
            closedFocusedStage.close();
        });
        Util.runAndWait(() -> {
            closedFocusedStage.requestFocus();
        });
        closedFocusedStage = null;
        assertCollectable(closedFocusedStageWeak);
    }

    public static void assertCollectable(WeakReference weakReference) throws Exception {
        int counter = 0;

        System.gc();
        System.runFinalization();

        while (counter < 10 && weakReference.get() != null) {
            Thread.sleep(100);
            counter = counter + 1;
            System.gc();
            System.runFinalization();
        }

        Assert.assertNull(weakReference.get());
    }

    @AfterClass
    public static void teardownOnce() {
        Platform.runLater(() -> {
            Platform.exit();
        });
    }
}