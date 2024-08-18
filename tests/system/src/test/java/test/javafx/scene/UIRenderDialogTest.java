/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.javafx.PlatformUtil;

import junit.framework.Assert;
import test.util.Util;

public class UIRenderDialogTest {
    private static CountDownLatch startupLatch = new CountDownLatch(1);
    private static volatile Stage stage;
    private static volatile Alert alert;
    private static final double scale = 1.75;

    public static class TestApp extends Application {

        @Override
        public void start(Stage primaryStage) throws Exception {
            final Button button = new Button("Show Dialog");
            button.setOnAction(e -> {
                final Alert alert = new Alert(Alert.AlertType.NONE);
                alert.initOwner(primaryStage);
                alert.getButtonTypes().add(ButtonType.OK);

                final HBox box = new HBox();
                box.setAlignment(Pos.CENTER);
                box.setPadding(new Insets(8));
                box.setSpacing(8);

                for (int i = 0; i < 4; i++) {
                    box.getChildren().add(new CheckBox("Check"));
                }

                alert.getDialogPane().setContent(box);
                UIRenderDialogTest.alert = alert;
                alert.show();
            });

            Scene scene = new Scene(new StackPane(button));
            primaryStage.setScene(scene);
            stage = primaryStage;
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN,
                    e -> Platform.runLater(startupLatch::countDown));
            stage.show();

            button.fire();
        }
    }

    @BeforeClass
    public static void setupOnce() throws Exception {
        System.setProperty("glass.win.uiScale", String.valueOf(scale));
        System.setProperty("glass.gtk.uiScale", String.valueOf(scale));

        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void teardown() {
        Platform.runLater(() -> {
            if (alert != null) {
                alert.hide();
            }
        });
        Util.shutdown();
    }

    @Test
    public void testCheckBoxTextInDialogDoesNotHaveEllipsis() {
        assumeTrue(PlatformUtil.isLinux() || PlatformUtil.isWindows());

        Assert.assertEquals("Wrong render scale", scale,
                stage.getRenderScaleY(), 0.0001);

        Assert.assertNotNull(alert);
        assertTrue(alert.isShowing());

        for (Node node : ((HBox) alert.getDialogPane().getContent()).getChildrenUnmodifiable()) {
            CheckBox box = (CheckBox) node;
            Assert.assertEquals("Wrong text", "Check", ((Text) box.lookup(".text")).getText());
        }
    }
}
