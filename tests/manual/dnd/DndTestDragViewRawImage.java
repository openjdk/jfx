/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;

public class DndTestDragViewRawImage extends Application {
    Image image = createImage(240, 240);

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) {
        ImageView imageView = new ImageView(image);
        imageView.setOnDragDetected(event -> {
            ClipboardContent content = new ClipboardContent();
            content.putImage(image);
            Dragboard dragboard = imageView.startDragAndDrop(TransferMode.ANY);
            dragboard.setContent(content);
            dragboard.setDragView(image);
        });

        Label label = new Label("Click the image and drag. " +
                "The drag image displayed with the cursor (drag view) " +
                "should match the source image");

        VBox vBox = new VBox(label, imageView);
        vBox.setSpacing(5.0);
        vBox.setAlignment(Pos.CENTER);
        stage.setScene(new Scene(vBox, 480, 480));
        stage.setTitle("Drag View Image Colors");
        stage.show();
    }

    private static Image createImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x < width * 0.33) {
                    image.setRGB(x, y, 0xFF0000);
                } else if (x < width * 0.66) {
                    image.setRGB(x, y, 0x00FF00);
                } else {
                    image.setRGB(x, y, 0x0000FF);
                }
            }
        }
        return SwingFXUtils.toFXImage(image, null);
    }
}
