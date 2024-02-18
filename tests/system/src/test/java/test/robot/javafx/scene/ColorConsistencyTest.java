/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.swing.SwingUtilities;

import java.awt.color.ColorSpace;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.text.DecimalFormat;

import test.util.Util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.opentest4j.AssertionFailedError;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class ColorConsistencyTest {

    static Stage stage;
    static CountDownLatch startupLatch = new CountDownLatch(1);

    static Rectangle swatch;
    static javafx.scene.robot.Robot fxRobot;
    static java.awt.Robot awtRobot;
    static int awtCenterX;
    static int awtCenterY;

    private static final int SWATCH_SIZE = 200;

    static final float LOW = 0.25f;
    static final float MID = 0.50f;
    static final float HIGH = 0.75f;
    static final double COMPONENT_TOLERANCE = 0.01;

    private enum TestColor {
        COLOR_01(LOW, LOW, LOW),
        COLOR_02(LOW, LOW, MID),
        COLOR_03(LOW, LOW, HIGH),

        COLOR_04(LOW, MID, LOW),
        COLOR_05(LOW, MID, MID),
        COLOR_06(LOW, MID, HIGH),

        COLOR_07(LOW, HIGH, LOW),
        COLOR_08(LOW, HIGH, MID),
        COLOR_09(LOW, HIGH, HIGH),

        COLOR_10(MID, LOW, LOW),
        COLOR_11(MID, LOW, MID),
        COLOR_12(MID, LOW, HIGH),

        COLOR_13(MID, MID, LOW),
        COLOR_14(MID, MID, MID),
        COLOR_15(MID, MID, HIGH),

        COLOR_16(MID, HIGH, LOW),
        COLOR_17(MID, HIGH, MID),
        COLOR_18(MID, HIGH, HIGH),

        COLOR_19(HIGH, LOW, LOW),
        COLOR_20(HIGH, LOW, MID),
        COLOR_21(HIGH, LOW, HIGH),

        COLOR_22(HIGH, MID, LOW),
        COLOR_23(HIGH, MID, MID),
        COLOR_24(HIGH, MID, HIGH),

        COLOR_25(HIGH, HIGH, LOW),
        COLOR_26(HIGH, HIGH, MID),
        COLOR_27(HIGH, HIGH, HIGH);

        public final float red;
        public final float green;
        public final float blue;

        TestColor(float r, float g, float b) {
            red = r;
            green = g;
            blue = b;
        }
    };

    private static javafx.scene.paint.Color setSwatchColor(TestColor testColor) {
        float r = testColor.red;
        float g = testColor.green;
        float b = testColor.blue;

        javafx.scene.paint.Color color = new javafx.scene.paint.Color(r, g, b, 1.0);
        Util.runAndWait(() -> {
            swatch.setFill(color);
        });

        Util.sleep(25);

        return color;
    }

    private static String colorToString(javafx.scene.paint.Color c) {
        final DecimalFormat f = new DecimalFormat("#.##");
        float r = (float)(c.getRed() * 255.0);
        float g = (float)(c.getGreen() * 255.0);
        float b = (float)(c.getBlue() * 255.0);
        float a = (float)(c.getOpacity() * 255.0);
        return "rgba(" + f.format(r) + "," + f.format(g) + "," + f.format(b) + "," + f.format(a) + ")";
    }

    private static void assertColorEquals(javafx.scene.paint.Color expected, javafx.scene.paint.Color actual, double delta) {
        if (!testColorEquals(expected, actual, delta)) {
            throw new AssertionFailedError("expected:" + colorToString(expected)
                    + " but was:" + colorToString(actual));
        }
    }

    private static boolean testColorEquals(javafx.scene.paint.Color expected, javafx.scene.paint.Color actual, double delta) {
        double deltaRed = Math.abs(expected.getRed() - actual.getRed());
        double deltaGreen = Math.abs(expected.getGreen() - actual.getGreen());
        double deltaBlue = Math.abs(expected.getBlue() - actual.getBlue());
        double deltaOpacity = Math.abs(expected.getOpacity() - actual.getOpacity());
        return (deltaRed <= delta && deltaGreen <= delta && deltaBlue <= delta && deltaOpacity <= delta);
    }

    @BeforeAll
    public static void initFX() throws Exception {
        Util.launch(startupLatch, SwatchApp.class);

        awtRobot = new java.awt.Robot();
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        java.awt.Rectangle bounds = environment.getMaximumWindowBounds();
        awtCenterX = (int) (bounds.getMinX() + bounds.getWidth() / 2.0);
        awtCenterY = (int) (bounds.getMinY() + bounds.getHeight() / 2.0);
    }

    @AfterAll
    public static void exit() {
        Util.shutdown(stage);
    }

    @ParameterizedTest
    @Order(1)
    @EnumSource(TestColor.class)
    public void singlePixel(TestColor testColor)
    {
        javafx.scene.paint.Color expected = setSwatchColor(testColor);

        AtomicReference<javafx.scene.paint.Color> actual = new AtomicReference<>();
        Util.runAndWait(() -> {
            // Use the JavaFX Robot to sample a pixel
            Bounds screenBounds = swatch.localToScreen(swatch.getBoundsInLocal());
            double centerX = screenBounds.getMinX() + screenBounds.getWidth() / 2.0;
            double centerY = screenBounds.getMinY() + screenBounds.getHeight() / 2.0;
            actual.set(fxRobot.getPixelColor(centerX, centerY));
        });
        assertColorEquals(expected, actual.get(), COMPONENT_TOLERANCE);
    }

    @ParameterizedTest
    @Order(2)
    @EnumSource(TestColor.class)
    public void screenImage(TestColor testColor)
    {
        javafx.scene.paint.Color expected = setSwatchColor(testColor);

        AtomicReference<javafx.scene.paint.Color> actual = new AtomicReference<>();
        Util.runAndWait(() -> {
            // Use the JavaFX Robot to capture an image
            Bounds screenBounds = swatch.localToScreen(swatch.getBoundsInLocal());
            double centerX = screenBounds.getMinX() + screenBounds.getWidth() / 2.0;
            double centerY = screenBounds.getMinY() + screenBounds.getHeight() / 2.0;
            WritableImage image = fxRobot.getScreenCapture(null, centerX, centerY, 5, 5);
            PixelReader reader = image.getPixelReader();
            actual.set(reader.getColor(3, 3));
        });
        assertColorEquals(expected, actual.get(), COMPONENT_TOLERANCE);
    }

    @ParameterizedTest
    @Order(3)
    @EnumSource(TestColor.class)
    public void sRGBColor(TestColor testColor) throws Exception
    {
        javafx.scene.paint.Color expected = setSwatchColor(testColor);

        // Use the AWT Robot to sample a pixel since it knows how to do
        // color space conversions.
        float[]  sRGB = {1.0f, 1,0f, 1.0f};
        SwingUtilities.invokeAndWait(() -> {
            ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
            java.awt.Color awtColor = awtRobot.getPixelColor(awtCenterX, awtCenterY);
            awtColor.getColorComponents(colorSpace, sRGB);
        });

        javafx.scene.paint.Color actual = new javafx.scene.paint.Color(sRGB[0], sRGB[1], sRGB[2], 1.0);
        assertColorEquals(expected, actual, COMPONENT_TOLERANCE);
    }

    public static class SwatchApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            stage = primaryStage;
            fxRobot = new javafx.scene.robot.Robot();

            javafx.scene.paint.Color color = new javafx.scene.paint.Color(0.5, 1.0, 0.5, 1.0);
            swatch = new Rectangle(SWATCH_SIZE, SWATCH_SIZE);
            swatch.setFill(color);

            HBox wrapper = new HBox(swatch);
            Scene scene = new Scene(wrapper);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setTitle("JavaFX Swatch");
            stage.setScene(scene);
            stage.setAlwaysOnTop(true);
            stage.setOnShown(e -> {
                Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
                double centerX = screenBounds.getMinX() + screenBounds.getWidth() / 2.0;
                double centerY = screenBounds.getMinY() + screenBounds.getHeight() / 2.0;
                stage.sizeToScene();
                stage.setX(centerX - stage.getWidth() / 2.0);
                stage.setY(centerY - stage.getHeight() / 2.0);
                startupLatch.countDown();
            });
            stage.show();
        }
    }
}