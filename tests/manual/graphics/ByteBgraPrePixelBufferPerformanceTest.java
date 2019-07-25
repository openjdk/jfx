/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ByteBgraPrePixelBufferPerformanceTest extends Application {

    private static final int SCENE_WIDTH = 1000;
    private static final int SCENE_HEIGHT = 1000;
    private static final int IMAGE_WIDTH = SCENE_WIDTH;
    private static final int IMAGE_HEIGHT = SCENE_HEIGHT;
    private static final int COPY_BUFFER_WIDTH = IMAGE_WIDTH;
    private static final int COPY_BUFFER_HEIGHT = IMAGE_HEIGHT;

    private PixelBuffer<ByteBuffer> pixelBuffer;
    private ByteBuffer byteBuffer;
    private ArrayList<Color> colors = new ArrayList<>();
    private int count = 0;
    private List<ByteBuffer> copyBuffers = new ArrayList<>();

    private VBox wImgContainer = new VBox(8);
    private VBox pbImgContainer = new VBox(8);

    private ByteBuffer createBuffer(int w, int h, Color c) {
        ByteBuffer bf = ByteBuffer.allocateDirect(w * h * 4);
        byte red = (byte) Math.round(c.getRed() * 255.0);
        byte green = (byte) Math.round(c.getGreen() * 255.0);
        byte blue = (byte) Math.round(c.getBlue() * 255.0);
        byte alpha = (byte) 255;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bf.put(blue);
                bf.put(green);
                bf.put(red);
                bf.put(alpha);
            }
        }
        bf.rewind();
        return bf;
    }

    private void createCopyBuffers() {
        for (Color clr : colors) {
            ByteBuffer buf = createBuffer(COPY_BUFFER_WIDTH, COPY_BUFFER_HEIGHT, clr);
            copyBuffers.add(buf);
        }
    }

    private void updateDirectByteBuffer(ByteBuffer buf) {
        ByteBuffer src = copyBuffers.get(count++);
        buf.put(src);

        buf.rewind();
        src.rewind();
        if (count >= copyBuffers.size()) {
            count = 0;
        }
    }

    private WritableImage createWImageFromBuffer(int w, int h, Color c) {
        byteBuffer = createBuffer(w, h, c);
        WritableImage img = new WritableImage(w, h);
        PixelFormat<ByteBuffer> pf = PixelFormat.getByteBgraPreInstance();
        PixelWriter pw = img.getPixelWriter();
        pw.setPixels(0, 0, w, h, pf, byteBuffer, w * 4);
        return img;
    }

    private WritableImage createWImageFromPixelBuffer(int w, int h, Color c) {
        ByteBuffer byteBuffer = createBuffer(w, h, c);
        PixelFormat<ByteBuffer> pf = PixelFormat.getByteBgraPreInstance();
        pixelBuffer = new PixelBuffer<>(w, h, byteBuffer, pf);
        return new WritableImage(pixelBuffer);
    }

    private void loadWritableImage() {
        final Image bImage = createWImageFromBuffer(IMAGE_WIDTH, IMAGE_HEIGHT, Color.BLUE);
        ImageView bIv = new ImageView(bImage);
        final TextField tf = new TextField("100");
        final Label clr = new Label("Color");
        Button updateWritableImage = new Button("Update WritableImage");
        updateWritableImage.setOnAction(e -> {
            int numIter = Integer.parseInt(tf.getText());

            double t1 = System.nanoTime();
            for (int i = 0; i < numIter; i++) {
                updateDirectByteBuffer(byteBuffer);
                PixelWriter pw = ((WritableImage) bImage).getPixelWriter();
                PixelFormat<ByteBuffer> pf = PixelFormat.getByteBgraPreInstance();
                pw.setPixels(0, 0, COPY_BUFFER_WIDTH, COPY_BUFFER_HEIGHT, pf, byteBuffer, COPY_BUFFER_WIDTH * 4);
            }
            double t2 = System.nanoTime();
            double t3 = t2 - t1;

            clr.setText(colors.get(count).toString());
            System.out.println("WI: AVERAGE time to update the Image: [" + t3 / (long) numIter + "]nano sec,  [" + (t3 / 1000000.0) / (double) numIter + "]ms");
        });
        wImgContainer.getChildren().addAll(updateWritableImage, tf, clr, bIv);
    }

    private void loadPBImage() {
        Image pbImage = createWImageFromPixelBuffer(IMAGE_WIDTH, IMAGE_HEIGHT, Color.BLUE);
        ImageView pbIv = new ImageView(pbImage);

        final TextField tf = new TextField("100");
        final Label clr = new Label("Color");

        Button updatePixelBuffer = new Button("Update PixelBuffer");
        updatePixelBuffer.setOnAction(e -> {
            int numIter = Integer.parseInt(tf.getText());

            double t1 = System.nanoTime();
            for (int i = 0; i < numIter; i++) {
                pixelBuffer.updateBuffer(pixBuf -> {
                    updateDirectByteBuffer(pixBuf.getBuffer());
                    return new Rectangle2D(0, 0, COPY_BUFFER_WIDTH, COPY_BUFFER_HEIGHT);
                });
            }

            double t2 = System.nanoTime();
            double t3 = t2 - t1;

            clr.setText(colors.get(count).toString());
            System.out.println("PB: AVERAGE time to update the Image: [" + t3 / (long) numIter + "]nano sec,  [" + (t3 / 1000000.0) / (double) numIter + "]ms");

        });
        pbImgContainer.getChildren().addAll(updatePixelBuffer, tf, clr, pbIv);
    }

    @Override
    public void start(Stage pbImageStage) {
        colors.add(Color.GREEN);
        colors.add(Color.BLUE);
        colors.add(Color.RED);

        createCopyBuffers();
        VBox pbRoot = new VBox(12);
        Scene pbScene = new Scene(pbRoot, SCENE_WIDTH, SCENE_HEIGHT);
        loadPBImage();
        pbRoot.getChildren().add(pbImgContainer);
        pbImageStage.setScene(pbScene);
        pbImageStage.setTitle("PixelBuffer");
        pbImageStage.setX(10);
        pbImageStage.setY(10);
        pbImageStage.show();

        VBox wImRoot = new VBox(12);
        Scene wImScene = new Scene(wImRoot, SCENE_WIDTH, SCENE_HEIGHT);
        loadWritableImage();
        wImRoot.getChildren().add(wImgContainer);

        Stage wImStage = new Stage();
        wImStage.setScene(wImScene);
        wImStage.setTitle("WritableImage-PixelWriter");

        wImStage.setX(SCENE_WIDTH + 50);
        wImStage.setY(10);
        wImStage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
