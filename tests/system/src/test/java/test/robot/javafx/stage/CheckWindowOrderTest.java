/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.javafx.stage;

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

// See JDK8220272
public class CheckWindowOrderTest {
    static Scene scene;
    static Stage stage;
    static Stage firstWindow;
    static Stage secondWindow;
    static Stage lastWindow;
    static CountDownLatch startupLatch = new CountDownLatch(4);

    @Test(timeout = 15000)
    public void topWindowShouldBeTheLast() throws Exception {
        Thread.sleep(400);
        Assert.assertTrue("Last Window Should be Focused", lastWindow.isFocused());
    }

    @BeforeClass
    public static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void exit() {
        Util.shutdown(lastWindow, secondWindow, firstWindow, stage);
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            stage = primaryStage;

            scene = new Scene(new Label("Primary Stage"), 640, 480);
            primaryStage.setScene(scene);
            primaryStage.setOnShown(e -> Platform.runLater(startupLatch::countDown));
            primaryStage.show();

            firstWindow = new TestStage(primaryStage, "First Window");
            firstWindow.show();

            secondWindow = new TestStage(primaryStage, "Second Window");
            secondWindow.show();

            lastWindow = openLastWindow(secondWindow);
            lastWindow.show();
        }

        TestStage openLastWindow(Window owner) {
            TestStage stage = new TestStage(owner, "Last Window");
            stage.initModality(Modality.WINDOW_MODAL);

            return stage;
        }
    }

    static class TestStage extends Stage {
        TestStage(Window owner, String title) {
            initOwner(owner);
            setTitle(title);

            this.setScene(new Scene(new Label("Hello World!"), 400, 400));

            setOnShown(e -> Platform.runLater(startupLatch::countDown));
        }
    }
}
