/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.awt.color.ColorSpace;
import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Window;
import com.sun.javafx.PlatformUtil;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import static org.junit.Assume.assumeTrue;

import test.robot.testharness.VisualTestBase;

public class SRGBTest extends VisualTestBase {

    private static final int SWATCH_SIZE = 200;

    // Avoid testing colors out near the edge
    // of the gamut to avoid clipping.
    static final float LOW = 0.25f;
    static final float MID = 0.50f;
    static final float HIGH = 0.75f;
    static final double COMPONENT_TOLERANCE = 2.0 / 255.0;

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

    // Create a stage in the center of the visual bounds and
    // return a swatch we can set the color on.
    private Rectangle prepareStage() {
        AtomicReference<Rectangle> rectangle = new AtomicReference<>();

        runAndWait(() -> {
            Stage stage = getStage();

            Rectangle swatch = new Rectangle(SWATCH_SIZE, SWATCH_SIZE);
            rectangle.set(swatch);

            HBox root = new HBox(swatch);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setOnShown(e -> {
                Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
                double centerX = screenBounds.getMinX() + screenBounds.getWidth() / 2.0;
                double centerY = screenBounds.getMinY() + screenBounds.getHeight() / 2.0;
                stage.sizeToScene();
                stage.setX(centerX - stage.getWidth() / 2.0);
                stage.setY(centerY - stage.getHeight() / 2.0);
            });
            stage.show();
        });

        waitFirstFrame();
        return rectangle.get();
    }

    // Change the color of the swatch and wait for it to be drawn
    private Color prepareSwatch(Rectangle swatch, TestColor testColor) {
        float r = testColor.red;
        float g = testColor.green;
        float b = testColor.blue;
        Color expected = new Color(r, g, b, 1.0);
        runAndWait(() -> {
            swatch.setFill(expected);
        });
        waitNextFrame();
        return expected;
    }

    private Point2D findCenter(Rectangle swatch) {
        Bounds screenBounds = swatch.localToScreen(swatch.getBoundsInLocal());
        double centerX = screenBounds.getMinX() + screenBounds.getWidth() / 2.0;
        double centerY = screenBounds.getMinY() + screenBounds.getHeight() / 2.0;
        return new Point2D(centerX, centerY);
    }

    @Test
    public void singlePixelTest() {
        Rectangle swatch = prepareStage();
        Robot robot = getRobot();

        for (TestColor testColor : TestColor.values()) {
            Color expected = prepareSwatch(swatch, testColor);
            AtomicReference<Color> actual = new AtomicReference<>();
            runAndWait(() -> {
                Point2D center = findCenter(swatch);
                actual.set(robot.getPixelColor(center));
            });
            assertColorEquals(expected, actual.get(), COMPONENT_TOLERANCE);
        }
    }

    @Test
    public void screenCaptureTest() {
        Rectangle swatch = prepareStage();
        Robot robot = getRobot();

        for (TestColor testColor : TestColor.values()) {
            Color expected = prepareSwatch(swatch, testColor);
            AtomicReference<Color> actual = new AtomicReference<>();
            runAndWait(() -> {
                Point2D center = findCenter(swatch);
                WritableImage image = robot.getScreenCapture(null, center.getX(), center.getY(), 5, 5);
                PixelReader reader = image.getPixelReader();
                actual.set(reader.getColor(3, 3));
            });
            assertColorEquals(expected, actual.get(), COMPONENT_TOLERANCE);
        }
    }

    // The earlier tests only verify that we can round-trip our colors. An AWT
    // Robot understands color spaces and can verify that we actually
    // produced sRGB color on the screen.
    @Test
    public void sRGBPixelTest() throws Exception {
        Rectangle swatch = prepareStage();

        java.awt.Robot awtRobot = new java.awt.Robot();
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        java.awt.Rectangle bounds = environment.getMaximumWindowBounds();
        int centerX = (int) (bounds.getMinX() + bounds.getWidth() / 2.0);
        int centerY = (int) (bounds.getMinY() + bounds.getHeight() / 2.0);

        for (TestColor testColor : TestColor.values()) {
            Color expected = prepareSwatch(swatch, testColor);
            float[] sRGB = {1.0f, 1,0f, 1.0f};
            SwingUtilities.invokeAndWait(() -> {
                ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                java.awt.Color awtColor = awtRobot.getPixelColor(centerX, centerY);
                awtColor.getColorComponents(colorSpace, sRGB);
            });
            Color actual = new Color(sRGB[0], sRGB[1], sRGB[2], 1.0);
            assertColorEquals(expected, actual, COMPONENT_TOLERANCE);
        }
    }

    @Test
    public void windowBackgroundTest() throws Exception {
        assumeTrue(PlatformUtil.isMac());
        final int positionX = 50;
        final int positionY = 50;
        final int centerX = positionX + SWATCH_SIZE / 2;
        final int centerY = positionY + SWATCH_SIZE / 2;
        AtomicReference<Window> window = new AtomicReference<>();
        runAndWait(() -> {
            Application app = Application.GetApplication();
            Window w = app.createWindow(null, com.sun.glass.ui.Screen.getMainScreen(), Window.UNTITLED);
            w.setLevel(Window.Level.TOPMOST);
            w.setSize(SWATCH_SIZE, SWATCH_SIZE);
            w.setPosition(positionX, positionY);
            w.setVisible(true);
            window.set(w);
        });

        try {
            Robot robot = getRobot();
            for (TestColor testColor : TestColor.values()) {
                runAndWait(() -> {
                    window.get().setBackground(testColor.red, testColor.green, testColor.blue);
                });
                waitNextFrame();

                Color expected = new Color(testColor.red, testColor.green, testColor.blue, 1.0f);
                AtomicReference<Color> actual = new AtomicReference<>();
                runAndWait(() -> {
                    actual.set(robot.getPixelColor(centerX, centerY));
                });
                assertColorEquals(expected, actual.get(), COMPONENT_TOLERANCE);
            }
        } finally {
            runAndWait(() -> {
                window.get().close();
            });
        }
    }
}