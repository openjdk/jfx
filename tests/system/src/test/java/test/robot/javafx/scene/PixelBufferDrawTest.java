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

package test.robot.javafx.scene;

import static org.junit.Assert.fail;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

/*
 * Test to verify that when the shared PixelBuffer is updated,
 * all the WritableImages created using that PixelBuffer are redrawn.
 * The test verifies four combinations,
 * 1. INT_ARGB_PRE pixel format and direct java.nio.IntBuffer
 * 2. INT_ARGB_PRE pixel format and Indirect java.nio.IntBuffer
 * 3. BYTE_BGRA_PRE pixel format and direct java.nio.ByteBuffer
 * 4. BYTE_BGRA_PRE pixel format and Indirect java.nio.ByteBuffer
 *
 * Test Steps:
 * 1. Create PixelBuffer and fill with INIT_COLOR.
 * 2. Create 4 WritableImages using PixelBuffer and add to scene.
 *.3. Verify that, color of pixel (0.5 * WIDTH, 0.45 * HEIGHT) and
 *    (0.5 * WIDTH, 0.55 * HEIGHT) is INIT_COLOR.
 * 4. Update the upper half of the PixelBuffer(0, 0, WIDTH, HEIGHT / 2)
 *    to TEST_COLOR using the modifyXXXBuffer.
 * 5. Verify that, color of pixel (0.5 * WIDTH, 0.45 * HEIGHT) is TEST_COLOR
 *    and color of pixel (0.5 * WIDTH, 0.55 * HEIGHT) is INIT_COLOR.
 *
 * Additionally this test also verifies that, an IllegalStateException is thrown
 * when PixelBuffer.updateBuffer() is called on Non JavaFX Application Thread.
 */

public class PixelBufferDrawTest {

    private static HBox root;
    private static Stage stage;
    private static Scene scene;
    private static Robot robot;
    private static CountDownLatch startupLatch = new CountDownLatch(1);

    private static final int DELAY = 500;
    private static final int NUM_IMAGES = 4;
    private static final int IMAGE_WIDTH = 24;
    private static final int IMAGE_HEIGHT = IMAGE_WIDTH;
    private static final int SCENE_WIDTH = IMAGE_WIDTH * NUM_IMAGES + NUM_IMAGES - 1;
    private static final int SCENE_HEIGHT = IMAGE_HEIGHT;
    private static final Color TEST_COLOR = new Color(0.2, 0.6, 0.8, 1);
    private static final Color INIT_COLOR = new Color(0.92, 0.56, 0.1, 1);
    private volatile Color actualColor = Color.BLACK;
    private PixelBuffer<ByteBuffer> bytePixelBuffer;
    private PixelBuffer<IntBuffer> intPixelBuffer;
    private IntBuffer sourceIntBuffer;
    private ByteBuffer sourceByteBuffer;

    private Callback<PixelBuffer<ByteBuffer>, Rectangle2D> byteBufferCallback = pixelBuffer -> {
        ByteBuffer dst = pixelBuffer.getBuffer();
        dst.put(sourceByteBuffer);
        sourceByteBuffer.rewind();
        dst.rewind();
        return new Rectangle2D(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT / 2);
    };

    private static ByteBuffer createByteBuffer(int w, int h, boolean isDirect, Color c) {
        byte red = (byte) Math.round(c.getRed() * 255.0);
        byte green = (byte) Math.round(c.getGreen() * 255.0);
        byte blue = (byte) Math.round(c.getBlue() * 255.0);
        byte alpha = (byte) Math.round(c.getOpacity() * 255.0);
        ByteBuffer byteBuffer;

        if (isDirect) {
            byteBuffer = ByteBuffer.allocateDirect(w * h * 4);
        } else {
            byteBuffer = ByteBuffer.allocate(w * h * 4);
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                byteBuffer.put(blue);
                byteBuffer.put(green);
                byteBuffer.put(red);
                byteBuffer.put(alpha);
            }
        }
        byteBuffer.rewind();
        return byteBuffer;
    }

    private void createBytePixelBuffer(boolean isDirect) {
        ByteBuffer sharedBuffer = createByteBuffer(IMAGE_WIDTH, IMAGE_HEIGHT, isDirect, INIT_COLOR);
        sourceByteBuffer = createByteBuffer(IMAGE_WIDTH, IMAGE_HEIGHT / 2, isDirect, TEST_COLOR);
        PixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteBgraPreInstance();
        bytePixelBuffer = new PixelBuffer<>(IMAGE_WIDTH, IMAGE_HEIGHT, sharedBuffer, pixelFormat);
    }

    private Callback<PixelBuffer<IntBuffer>, Rectangle2D> intBufferCallback = pixelBuffer -> {
        IntBuffer dst = pixelBuffer.getBuffer();
        dst.put(sourceIntBuffer);
        sourceIntBuffer.rewind();
        dst.rewind();
        return new Rectangle2D(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT / 2);
    };

    private static IntBuffer createIntBuffer(int w, int h, boolean isDirect, Color c) {
        int red = (int) Math.round(c.getRed() * 255.0);
        int green = (int) Math.round(c.getGreen() * 255.0);
        int blue = (int) Math.round(c.getBlue() * 255.0);
        int alpha = (int) Math.round(c.getOpacity() * 255.0);
        int color = alpha << 24 | red << 16 | green << 8 | blue;
        IntBuffer intBuffer;

        if (isDirect) {
            ByteBuffer bf = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder());
            intBuffer = bf.asIntBuffer();
        } else {
            intBuffer = IntBuffer.allocate(w * h);
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                intBuffer.put(color);
            }
        }
        intBuffer.rewind();
        return intBuffer;
    }

    private void createIntPixelBuffer(boolean isDirect) {
        IntBuffer sharedBuffer = createIntBuffer(IMAGE_WIDTH, IMAGE_HEIGHT, isDirect, INIT_COLOR);
        sourceIntBuffer = createIntBuffer(IMAGE_WIDTH, IMAGE_HEIGHT / 2, isDirect, TEST_COLOR);
        PixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbPreInstance();
        intPixelBuffer = new PixelBuffer<>(IMAGE_WIDTH, IMAGE_HEIGHT, sharedBuffer, pixelFormat);
    }

    private ImageView createImageViewPB(PixelBuffer<? extends Buffer> pixelBuffer) {
        return new ImageView(new WritableImage(pixelBuffer));
    }

    private void compareColor(Color exp, Color act) {
        final double COMPARE_DELTA = 0.01;
        Assert.assertEquals(exp.getRed(), act.getRed(), COMPARE_DELTA);
        Assert.assertEquals(exp.getBlue(), act.getBlue(), COMPARE_DELTA);
        Assert.assertEquals(exp.getGreen(), act.getGreen(), COMPARE_DELTA);
        Assert.assertEquals(exp.getOpacity(), act.getOpacity(), COMPARE_DELTA);
    }

    private void verifyColor(Color color1, Color color2) {
        for (int i = 0; i < root.getChildren().size(); i++) {
            final int x = (int) (scene.getWindow().getX() + scene.getX() +
                    root.getChildren().get(i).getLayoutX() + IMAGE_WIDTH / 2);
            final int y = (int) (scene.getWindow().getY() + scene.getY() +
                    root.getChildren().get(i).getLayoutY() + IMAGE_HEIGHT * 0.45);

            Util.runAndWait(() -> actualColor = robot.getPixelColor(x, y));
            compareColor(color1, actualColor);

            final int x1 = (int) (scene.getWindow().getX() + scene.getX() +
                    root.getChildren().get(i).getLayoutX() + IMAGE_WIDTH / 2);
            final int y1 = (int) (scene.getWindow().getY() + scene.getY() +
                    root.getChildren().get(i).getLayoutY() + IMAGE_HEIGHT * 0.55);

            Util.runAndWait(() -> actualColor = robot.getPixelColor(x1, y1));
            compareColor(color2, actualColor);
        }
    }

    private <T extends Buffer> void performTest(PixelBuffer<T> pixelBuffer, Callback<PixelBuffer<T>, Rectangle2D> callback) {
        // Step #2
        Util.runAndWait(() -> {
            for (int i = 0; i < NUM_IMAGES; i++) {
                root.getChildren().add(createImageViewPB(pixelBuffer));
            }
        });
        delay();
        // Step #3
        verifyColor(INIT_COLOR, INIT_COLOR);

        // Step #4
        Util.runAndWait(() -> pixelBuffer.updateBuffer(callback));
        delay();

        // Step #5
        verifyColor(TEST_COLOR, INIT_COLOR);
    }

    @Test
    public void testIntArgbPreDirectBuffer() {
        // Step #1
        createIntPixelBuffer(true);
        performTest(intPixelBuffer, intBufferCallback);
    }

    @Test
    public void testIntArgbPreIndirectBuffer() {
        // Step #1
        createIntPixelBuffer(false);
        performTest(intPixelBuffer, intBufferCallback);
    }

    @Test
    public void testByteBgraPreDirectBuffer() {
        // Step #1
        createBytePixelBuffer(true);
        performTest(bytePixelBuffer, byteBufferCallback);
    }

    @Test
    public void testByteBgraPreIndirectBuffer() {
        // Step #1
        createBytePixelBuffer(false);
        performTest(bytePixelBuffer, byteBufferCallback);
    }

    @Test
    public void testUpdateBufferOnNonFxAppThread() {
        createBytePixelBuffer(true);
        try {
            bytePixelBuffer.updateBuffer(byteBufferCallback);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
        }
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            stage = primaryStage;
            robot = new Robot();
            root = new HBox(1);
            scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e ->
                    Platform.runLater(startupLatch::countDown));
            stage.setAlwaysOnTop(true);
            stage.show();
        }
    }

    @BeforeClass
    public static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void exit() {
        Util.shutdown(stage);
    }

    @Before
    public void before() {
        Util.parkCursor(robot);
    }

    @After
    public void cleanupTest() {
        Util.runAndWait(() -> {
            root = new HBox(1);
            scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
            stage.setScene(scene);
        });
    }

    private static void delay() {
        Util.sleep(DELAY);
    }
}
