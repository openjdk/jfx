/*
 * Copyright (c) 2008, 2012 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.javafx.experiments.scheduleapp.control;

import com.javafx.experiments.scheduleapp.ConferenceScheduleApp;
import java.awt.image.BufferedImage;
import java.io.File;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.SnapshotResult;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;
import javax.imageio.ImageIO;

public class TestVirtualKeyboard extends Application {

    @Override public void start(Stage primaryStage) throws Exception {
        final Region r1 = new Region();
        r1.getStyleClass().setAll("key");
        r1.setPrefSize(20, 20);
        r1.relocate(10, 10);

        final Region r1p = new Region();
        r1p.getStyleClass().setAll("key");
        r1p.setPrefSize(20, 20);
        r1p.relocate(10, 40);
        r1p.fireEvent(MouseEvent.impl_mouseEvent(20, 50, 0, 0, MouseButton.PRIMARY, 1, false, false, false, false, false, true, false, false, true, MouseEvent.MOUSE_PRESSED));

        final Region r2 = new Region();
        r2.getStyleClass().setAll("key", "special");
        r2.setPrefSize(20, 20);
        r2.relocate(40, 10);

        final Region r2p = new Region();
        r2p.getStyleClass().setAll("key", "special");
        r2p.setPrefSize(20, 20);
        r2p.relocate(40, 40);
        r2p.fireEvent(MouseEvent.impl_mouseEvent(50, 50, 0, 0, MouseButton.PRIMARY, 1, false, false, false, false, false, true, false, false, true, MouseEvent.MOUSE_PRESSED));

        final Region r3 = new Region();
        r3.getStyleClass().setAll("key", "short");
        r3.setPrefSize(20, 20);
        r3.relocate(70, 10);

        final Region r3p = new Region();
        r3p.getStyleClass().setAll("key", "short");
        r3p.setPrefSize(20, 20);
        r3p.relocate(70, 40);
        r3p.fireEvent(MouseEvent.impl_mouseEvent(80, 50, 0, 0, MouseButton.PRIMARY, 1, false, false, false, false, false, true, false, false, true, MouseEvent.MOUSE_PRESSED));

        Group root = new Group(r1, r2, r3, r1p, r2p, r3p);
        Scene scene = new Scene(root, 768, 392);
        scene.setFill(Color.rgb(76, 76, 76));
        scene.getStylesheets().add(ConferenceScheduleApp.class.getResource("SchedulerStyleSheet.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();

        Platform.runLater(new Runnable() {
            @Override public void run() {
                saveImage(r1, "key.png");
                saveImage(r1p, "key-pressed.png");
                saveImage(r2, "special-key.png");
                saveImage(r2p, "special-key-pressed.png");
                saveImage(r3, "short-key.png");
                saveImage(r3p, "short-key-pressed.png");
            }
        });
    }

    void saveImage(final Node n, final String imageName) {
        final WritableImage image = new WritableImage(21, 21);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        n.snapshot(new Callback<SnapshotResult, Void>() {
            @Override public Void call(SnapshotResult param) {
                try {
                    BufferedImage img = javafx.embed.swing.SwingFXUtils.fromFXImage(image, new BufferedImage(21, 21, BufferedImage.TYPE_INT_ARGB));
                    ImageIO.write(img, "png", new File(imageName));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }, params, image);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
