/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
package dragdrop;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class DragDropKeys extends Application {
    @Override
    public void start(Stage stage) {
        Text text = new Text("Instructions:\n" +
                             "1. Press and release any key.\n" +
                             "2. Notice key events appearing in text area.\n" +
                             "3. While dragging image, press any key.\n" +
                             "4. Notice the key events while dragging.\n" +
                             "5. Also notice that the image should be RED, GREEN, BLUE.\n");
        Image image = createImage(180, 180);
        ImageView imageView = new ImageView(image);
        imageView.setOnDragDetected(event -> {
            ClipboardContent content = new ClipboardContent();
            content.putImage(image);
            Dragboard dragboard = imageView.startDragAndDrop(TransferMode.ANY);
            dragboard.setContent(content);
            dragboard.setDragView(image);
        });
        TextArea textArea = new TextArea("KeyEvent log:\n");
        textArea.setEditable(false);

        VBox vBox = new VBox(text, imageView, textArea);
        vBox.setSpacing(5.0);
        vBox.setAlignment(Pos.BOTTOM_CENTER);
        stage.setScene(new Scene(vBox, 480, 480));
        stage.setTitle("KeyEvents During Drag & Drop");
        stage.addEventFilter(KeyEvent.KEY_PRESSED, event ->
                textArea.appendText(event.getEventType().getName() + ": " + event.getCode().getName() + "\n"));
        stage.addEventFilter(KeyEvent.KEY_RELEASED, event ->
                textArea.appendText(event.getEventType().getName() + ": " + event.getCode().getName() + "\n"));
        stage.show();
    }

    private static Image createImage(int width, int height) {
        WritableImage image = new WritableImage(width, height);
        PixelWriter pixelWriter = image.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x < width * 0.33) {
                    pixelWriter.setColor(x, y, Color.RED);
                } else if (x < width * 0.66) {
                    pixelWriter.setColor(x, y, Color.GREEN);
                } else {
                    pixelWriter.setColor(x, y, Color.BLUE);
                }
            }
        }

        return image;
    }

    public static String info() {
        return "Drag and press keys";
    }

    public static void main(String[] args) {
        Application.launch(DragDropKeys.class, args);
    }
}
