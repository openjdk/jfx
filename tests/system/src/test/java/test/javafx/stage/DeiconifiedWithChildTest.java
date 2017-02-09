/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

public class DeiconifiedWithChildTest {
    static CountDownLatch startupLatch;
    static Stage stage;
    static Stage childStage;

    public static void main(String[] args) throws Exception {
        initFX();
        try {
            new DeiconifiedWithChildTest().testDeiconifiedPosition();
        } finally {
            teardown();
        }
    }

    public static class TestApp extends Application {

        @Override
        public void start(Stage primaryStage) throws Exception {
            primaryStage.setScene(new Scene(new VBox(), 200, 200));
            stage = primaryStage;
            stage.show();

            childStage = new Stage();
            childStage.initOwner(stage);
            childStage.setScene(new Scene(new VBox(), 100, 100));
            childStage.addEventHandler(WindowEvent.WINDOW_SHOWN, e ->
                    Platform.runLater(startupLatch::countDown));
            childStage.show();
        }
    }

    @BeforeClass
    public static void initFX() {
        startupLatch = new CountDownLatch(1);
        new Thread(() -> Application.launch(TestApp.class, (String[])null)).start();
        try {
            if (!startupLatch.await(15, TimeUnit.SECONDS)) {
                fail("Timeout waiting for FX runtime to start");
            }
        } catch (InterruptedException ex) {
            fail("Unexpected exception: " + ex);
        }
    }

    @Test
    public void testDeiconifiedPosition() throws Exception {
        Thread.sleep(200);
        Assert.assertTrue(stage.isShowing());
        Assert.assertTrue(childStage.isShowing());
        Assert.assertFalse(stage.isIconified());

        double x = childStage.getX();
        double y = childStage.getY();

        Platform.runLater(() -> stage.setIconified(true));
        Thread.sleep(200);
        Platform.runLater(() -> stage.setIconified(false));
        Thread.sleep(200);
        Assert.assertEquals("Child window was moved", x, childStage.getX(), 0.1);
        Assert.assertEquals("Child window was moved", y, childStage.getY(), 0.1);
    }

    @AfterClass
    public static void teardown() {
        Platform.runLater(childStage::hide);
        Platform.runLater(stage::hide);
        Platform.exit();
    }
}
