/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;

public class CssStyleHelperTest {

    private static CountDownLatch startupLatch = new CountDownLatch(1);
    private static StackPane root;
    private static Stage stage;
    private static Label label1;
    private static Button button;
    private static CheckBox checkBox;
    private static Label label2;

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            stage = primaryStage;
            label1 = new Label("Label1");
            button = new Button("aButton");
            checkBox = new CheckBox("aCheckBox");
            label2 = new Label("Label2");

            root = new StackPane();
            root.getChildren().addAll(label1, button, checkBox, label2);
            Scene scene = new Scene(root, 200, 200);
            scene.getStylesheets().add(getClass().getResource("RootFont.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setOnShown(l -> {
                Platform.runLater(() -> startupLatch.countDown());
            });
            primaryStage.show();
        }
    }

    @BeforeAll
    public static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    public static void teardownOnce() {
        Util.shutdown();
    }

    @Test
    public void testCssIsCorrectlyAppliedToLabelOnStageHideAndShow() throws Exception {
        // sanity
        Assertions.assertNull(label1.getBackground(), "Label1 should have no background");
        Assertions.assertNull(label2.getBackground(), "Label2 should have no background");

        startupLatch = new CountDownLatch(1);
        Util.runAndWait(() -> {
            stage.hide();
            stage.show();
        });
        assertTrue(startupLatch.await(15, TimeUnit.SECONDS), "Timeout waiting for Stage to show after hide");

        Assertions.assertNull(label1.getBackground(), "Label1 should have no background");
        Assertions.assertNull(label2.getBackground(), "Label2 should have no background");
    }
}
