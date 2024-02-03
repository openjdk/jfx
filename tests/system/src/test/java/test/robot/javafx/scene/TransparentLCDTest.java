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

import com.sun.javafx.PlatformUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.util.Util;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.Timeout;

/**
 * Basic visual tests using glass Robot to sample pixels.
 */
public class TransparentLCDTest {

    private static final boolean DEBUG = false;

    private static final int WIDTH = 400;
    private static final int HEIGHT = 300;

    // We use a very tight color tolerance, which requires some extra care.
    // We force a screen scale of 1, stay away from the edes of the window,
    // and only sample where we expect to read the background color or in
    // the middle of the text fill area.
    private static final double TOLERANCE = 2.0 / 255.0;

    private static final int TEXT_X_LEFT = 5;
    private static final int TEXT_Y_BOTTOM = 30;

    // The following are chosen to test before the first part of the 'V', after
    // the first part of the 'V', and in the middle of the first part of the 'V'
    private static final int SAMPLE_X_START = TEXT_X_LEFT;
    private static final int SAMPLE_Y = 11;
    private static final int TEST_WIDTH = 12;
    private static final int TEST_X_LEFT = 0;
    private static final int TEST_X_RIGHT = TEST_WIDTH - 1;
    private static final int TEST_X_MID = TEST_WIDTH / 2;

    private static final Color transpColor = Color.color(0.5, 0.5, 0.5, 0.6);
    private static final Color opaqueColor = makeOpaque(transpColor);

    private Robot robot;
    private Stage testStage;
    private Scene testScene;

    private static Color makeOpaque(Color c) {
        double a = c.getOpacity();
        double r = c.getRed() * a + (1.0 - a);
        double g = c.getGreen() * a + (1.0 - a);
        double b = c.getBlue() * a + (1.0 - a);
        return Color.color(r, g, b);
    }

    private boolean isGrayScale(List<Color> colors) {
        long nonGrayCount = colors.stream()
                .filter(c -> c.getRed() != c.getGreen() || c.getRed() != c.getBlue())
                .count();
        return nonGrayCount == 0;
    }

   protected void assertColorEquals(Color expected, Color actual) {
        if (!testColorEquals(expected, actual, TOLERANCE)) {
            fail("expected:" + colorToString(expected) +
                    " but was:" + colorToString(actual));
        }
    }

    protected boolean testColorEquals(Color expected, Color actual, double delta) {
        double deltaRed = Math.abs(expected.getRed() - actual.getRed());
        double deltaGreen = Math.abs(expected.getGreen() - actual.getGreen());
        double deltaBlue = Math.abs(expected.getBlue() - actual.getBlue());
        double deltaOpacity = Math.abs(expected.getOpacity() - actual.getOpacity());
        return (deltaRed <= delta && deltaGreen <= delta && deltaBlue <= delta && deltaOpacity <= delta);
    }

    protected static String colorToString(Color c) {
        int r = (int)(c.getRed() * 255.0);
        int g = (int)(c.getGreen() * 255.0);
        int b = (int)(c.getBlue() * 255.0);
        int a = (int)(c.getOpacity() * 255.0);
        return "rgba(" + r + "," + g + "," + b + "," + a + ")";
    }

    private List<Color> getColors(Scene scene, int x, int y, int width) {
        x += scene.getX() + scene.getWindow().getX();
        y += scene.getY() + scene.getWindow().getY();
        Image image = robot.getScreenCapture(null, x, y, width, 1);
        List<Color> colors = new ArrayList<>(width);
        for (int i = 0; i < width; i++) {
            colors.add(image.getPixelReader().getColor(i, 0));
        }
        return colors;
    }

    // This must be called on the FX app thread
    private Stage createStage() {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setAlwaysOnTop(true);
        return stage;
    }

    @BeforeAll
    public static void doSetupOnce() {
        System.setProperty("prism.lcdText", "true");
        System.setProperty("glass.win.uiScale", "1");
        System.setProperty("glass.gtk.uiScale", "1");

        Platform.setImplicitExit(false);
        final CountDownLatch launchLatch = new CountDownLatch(1);
        Util.startup(launchLatch, launchLatch::countDown);
        assertEquals(0, launchLatch.getCount());
    }

    @AfterAll
    public static void doTeardownOnce() {
        Util.shutdown();
    }

    @BeforeEach
    public void doSetup() {
        // LCD text is disabled on macOS
        assumeFalse(PlatformUtil.isMac());

        // Test is not valid for SW pipeline. We don't have a utility to
        // check the GraphicsPipeline, so we check for 3D support instead.
        assumeTrue(Platform.isSupported(ConditionalFeature.SCENE3D));

        Util.runAndWait(() -> robot = new Robot());
        Util.parkCursor(robot);
    }

    @AfterEach
    public void doTeardown() {
        Platform.runLater(() -> {
            if (testStage != null) {
                testStage.hide();
            }
        });
    }

    // Called by the test methods to run the test either using an opaque color,
    // which should use LCD, and a transparent color, which should not.
    private void runTest(boolean opaque) {
        final Color textColor = opaque ? opaqueColor : transpColor;

        Font font = Font.font("System", FontWeight.BOLD, 36);

        Util.runAndWait(() -> {
            testStage = createStage();

            Pane root = new Pane();
            testScene = new Scene(root, WIDTH, HEIGHT);

            Text text = new Text("V");
            text.setFont(font);
            text.setFill(textColor);
            text.setFontSmoothingType(FontSmoothingType.LCD);
            text.setLayoutX(TEXT_X_LEFT);
            text.setLayoutY(TEXT_Y_BOTTOM);
            root.getChildren().add(text);

            testStage.setScene(testScene);
            testStage.show();
        });

        Util.waitForIdle(testScene);
        Util.sleep(1000);

        Util.runAndWait(() -> {
            if (DEBUG) {
                System.err.println("transpColor = " + colorToString(transpColor));
                System.err.println("opaqueColor = " + colorToString(opaqueColor));
                System.err.println("");
            }
            List<Color> colors = getColors(testScene, SAMPLE_X_START, SAMPLE_Y, TEST_WIDTH);

            if (DEBUG) {
                colors.stream()
                        .map(TransparentLCDTest::colorToString)
                        .forEach(System.err::println);
            }

            // Verify the colors outside and in the middle of the text
            Color cLeft = colors.get(TEST_X_LEFT);
            Color cRight = colors.get(TEST_X_RIGHT);
            Color cMid = colors.get(TEST_X_MID);

            assertColorEquals(Color.WHITE, cLeft);
            assertColorEquals(Color.WHITE, cRight);
            assertColorEquals(opaqueColor, cMid);

            // Check whether LCD or GRAY scale AA is used
            boolean isGray = isGrayScale(colors);
            if (opaque) {
                assertFalse(isGray, "opaque color should use LCD antialiasing");
            } else {
                assertTrue(isGray, "transparent color should use GRAY scale antialiasing");
            }

        });
    }

    @Test
    @Timeout(15)
    public void testTransparentLCD() {
        runTest(false);
    }

    @Test
    @Timeout(15)
    public void testOpaqueLCD() {
        runTest(true);
    }

}
