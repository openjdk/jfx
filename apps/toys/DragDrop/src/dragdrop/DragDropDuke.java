/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DragDropDuke extends Application {
    private Image image;
    private ProgressBar progressBar;
    private ImageView imageView;
    private Label posLabel;

    @Override
    public void start(Stage stage) {
        progressBar = new ProgressBar();
        progressBar.prefWidthProperty().bind(stage.widthProperty().multiply(0.80d));

        new Thread(this::download).start();

        Text text = new Text("Instructions:\n" +
                "1. Wait for the image download.\n" +
                "2. Drag image and notice it's a transparent png.\n" +
                "3. Notice Position changes from \"Mouse Move\" to \"Mouse Drag\".\n" +
                "4. Drag over and drop on the tomato square (should change colors).\n" +
                "5. Open another instance of this Window and drag from one\ninstance to another.\n" +
                "6. While dragging image, press ALT+F4 (should stop Drag).");

        imageView = new ImageView(image);
        imageView.setFitHeight(200D);
        imageView.setPreserveRatio(true);

        Pane pane = new Pane();
        pane.setPrefSize(200, 200);
        pane.setBackground(Background.fill(Color.TOMATO));
        HBox hBox = new HBox(imageView, pane);
        hBox.setAlignment(Pos.CENTER);

        posLabel = new Label();
        VBox vBox = new VBox(text, posLabel, progressBar, hBox);
        vBox.setSpacing(5.0);
        vBox.setAlignment(Pos.CENTER);
        stage.setScene(new Scene(vBox, 480, 480));
        stage.setTitle("Drag & Drop Duke");

        imageView.setOnDragDetected(event -> {
            ClipboardContent content = new ClipboardContent();
            content.putImage(image);

            Dragboard dragboard = imageView.startDragAndDrop(TransferMode.ANY);
            dragboard.setContent(content);
            dragboard.setDragViewOffsetX(100);
            dragboard.setDragViewOffsetY(100);
            dragboard.setDragView(image);
        });

        stage.getScene().setOnMouseMoved(e -> showPos("Mouse Move", e.getSceneX(), e.getSceneY()));
        stage.getScene().setOnDragOver(e -> showPos("Mouse Drag", e.getSceneX(), e.getScreenY()));

        pane.setOnDragOver(e -> {
            e.acceptTransferModes(TransferMode.ANY);
            pane.setBackground(Background.fill(Color.CADETBLUE));
        });
        pane.setOnDragDropped(e -> pane.setBackground(Background.fill(Color.LAWNGREEN)));
        stage.show();
    }

    private void showPos(String prefix, double x, double y) {
        posLabel.setText(String.format("%s -> x: %f, y: %f", prefix, x, y));
    }

    private void download() {
        try {
            subDownload();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void subDownload() throws IOException {
        URL url = new URL("http://cr.openjdk.java.net/~jeff/Duke/png/Hips.png");
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        int cl = urlConn.getContentLength();

        int total = 0;
        int read = 0;
        try (InputStream in = urlConn.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream(cl)) {
            byte[] buf = new byte[1024];

            while ((read = in.read(buf)) > 0) {
                out.write(buf, 0, read);
                total += read;
                double percent = (double) total / (double) cl * 100d;

                Platform.runLater(() -> progressBar.setProgress(percent / 100));
            }

            Platform.runLater(() -> {
                image = new Image(new ByteArrayInputStream(out.toByteArray()));
                imageView.setImage(image);
            });
        }
    }

    public static String info() {
        return "Drag & Drop Duke";
    }

    public static void main(String[] args) {
        Application.launch(DragDropDuke.class, args);
    }
}
