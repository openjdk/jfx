/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.ImageCursor;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

public class ImageCursorGetBestSizeTest {
    static CountDownLatch startupLatch;
    static Stage stage;

    private static final double INIT_SIZE = 100.d;

    public static class TestApp extends Application {

        @Override
        public void start(Stage primaryStage) throws Exception {
            primaryStage.setScene(new Scene(new Group()));
            stage = primaryStage;
            stage.setWidth(INIT_SIZE);
            stage.setHeight(INIT_SIZE);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN,
                e -> Platform.runLater(startupLatch::countDown));
            stage.show();
        }
    }

    @BeforeClass
    public static void initFX() {
        startupLatch = new CountDownLatch(1);

        new Thread(() -> Application.launch(TestApp.class, (String[]) null)).start();
        try {
            if (!startupLatch.await(15, TimeUnit.SECONDS)) {
                Assert.fail("Timeout waiting for FX runtime to start");
            }
        } catch (InterruptedException ex) {
            Assert.fail("Unexpected exception: " + ex);
        }
    }

    @Test(timeout = 20000)
    public void testImageCursorGetBestSize() throws Exception {
        Util.runAndWait(() -> {
            Assert.assertNotNull(ImageCursor.getBestSize(10, 20));
        });
    }

    @AfterClass
    public static void teardown() {
        Platform.runLater(stage::hide);
        Platform.exit();
    }
}
