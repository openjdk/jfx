/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package test.robot.javafx.scene.layout;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.robot.Robot;
import test.robot.testharness.VisualTestBase;

/**
 */
public abstract class RegionUITestBase extends VisualTestBase {
    static final int WIDTH = 400;
    static final int HEIGHT = 300;
    static final int REGION_TOP = 50;
    static final int REGION_LEFT = 50;
    static final int REGION_RIGHT = 350;
    static final int REGION_BOTTOM = 250;
    static final int REGION_WIDTH = 300;
    static final int REGION_HEIGHT = 200;
    static final Color SCENE_FILL = Color.WHITE;

    protected Stage stage;
    protected Scene scene;
    protected Group root;
    protected Region region;
    private volatile WritableImage screenCapture;

    @Override
    public void doSetup() {
        super.doSetup();
        screenCapture = null;
        runAndWait(() -> {
            stage = getStage();
            region = new Region();
            region.setPrefSize(REGION_WIDTH, REGION_HEIGHT);
            region.relocate(REGION_LEFT, REGION_TOP);
            scene = new Scene(root = new Group(region), WIDTH, HEIGHT);
            scene.setFill(SCENE_FILL);
            stage.setScene(scene);
            stage.setAlwaysOnTop(true);
            stage.show();
        });
    }

    protected void setStyle(final String style) {
        runAndWait(() -> region.setStyle(style));
        waitFirstFrame();
    }

    static final double TOLERANCE = 0.07;

    protected boolean checkIntegralUIScale() {
        AtomicBoolean integralUIScale = new AtomicBoolean(false);
        runAndWait(() -> {
            Window window = scene.getWindow();
            double outScaleX = window.getOutputScaleX();
            double outScaleY = window.getOutputScaleY();

            if (outScaleX == Math.rint(outScaleX)
                    && outScaleY == Math.rint(outScaleY)) {
                integralUIScale.set(true);
            }
        });
        return integralUIScale.get();
    }

    protected void assertColorEquals(Color expected, int x, int y, double tolerance) {
        Color actual = getColorFromScreenCapture(x, y);
        try {
            assertColorEquals(expected, actual, tolerance);
        } catch (AssertionError error) {
            actual = getColorThreadSafe(x, y);
            try {
                assertColorEquals(expected, actual, tolerance);
            } catch (AssertionError ex) {
                throw new AssertionError(ex.getMessage() + " at position x=" + x + ", y=" + y);
            }
        }
    }

    protected void assertColorDoesNotEqual(Color notExpected, int x, int y, double tolerance) {
        Color actual = getColorThreadSafe(x, y);
        assertColorDoesNotEqual(notExpected, actual, tolerance);
    }

    private Color getColorFromScreenCapture(int x, int y) {
        if (screenCapture == null) {
            runAndWait(() -> {
                screenCapture = new WritableImage((int)scene.getWidth(), (int)scene.getHeight());
                getRobot().getScreenCapture(screenCapture,
                    scene.getX() + scene.getWindow().getX(),
                    scene.getY() + scene.getWindow().getY(),
                    scene.getWidth(), scene.getHeight());
            });
        }
        return screenCapture.getPixelReader().getColor(x, y);
    }

    private Color getColorThreadSafe(int x, int y) {
        AtomicReference<Color> color = new AtomicReference<>();
        runAndWait(() -> color.set(getColor(scene, x, y)));
        return color.get();
    }

    /**
     * Using the information available in Background and Border for the region,
     * this method will check that the scene color is used for those places that
     * are outside the region boundary (including the empty part of a rounded corner)
     * and that the expected color is used for the interior of the region. If the
     * given expected color is null, then this test will just make sure that
     * the color IS NOT the scene background. For example, when doing a gradient fill
     * or image fill it might not be possible to really know the expected color, but
     * at least we can make sure we're not seeing the scene background color.
     */
    protected void checkRegionCornersAndBoundariesForFills() {
        Background background = region.getBackground();
        Border border = region.getBorder();
        if (border != null) {
            throw new AssertionError("No implementation of this method for borders yet");
        }

        if (region.getShape() != null) {
            throw new AssertionError("No implementation of this method for regions with a shape yet");
        }

        Paint lastFill = SCENE_FILL;
        for (BackgroundFill fill : background.getFills()) {
            checkRegionCornersAndBoundariesOfBackgroundFill(fill, lastFill);
            lastFill = fill.getFill();
        }
    }

    protected void checkRegionLeftBoundary(BackgroundFill fill, Paint outsideFill) {
        checkRegionLeftBoundary(fill, fill.getFill(), outsideFill);
    }

    protected void checkRegionLeftBoundary(BackgroundFill fill, Paint insideFill, Paint outsideFill) {
        checkRegionLeftBoundary(fill, insideFill, outsideFill, 1, TOLERANCE);
    }

    protected void checkRegionLeftBoundary(BackgroundFill fill, Paint insideFill, Paint outsideFill, int distance, double tolerance) {
        checkRegionLeftBoundary(fill.getInsets().getLeft(), insideFill, outsideFill, distance, tolerance);
    }

    protected void checkRegionLeftBoundary(Paint outsideFill) {
        checkRegionLeftBoundary(0, null, outsideFill, 1, TOLERANCE);
    }

    protected void checkRegionLeftBoundary(double leftInset, Paint insideFill, Paint outsideFill, int distance, double tolerance) {
        List<TestParameters> params = new ArrayList<>();
        final int x1 = (int) (region.getLayoutX() + leftInset);
        final int midY = HEIGHT / 2;
        params.add(new TestParameters(outsideFill, insideFill, x1 - 1 - distance, midY, tolerance)); // left outside
        params.add(new TestParameters(insideFill, outsideFill, x1 + distance, midY, tolerance)); // left inside
        runTests(params);
    }

    protected void checkRegionTopBoundary(BackgroundFill fill, Paint outsideFill) {
        checkRegionTopBoundary(fill, fill.getFill(), outsideFill);
    }

    protected void checkRegionTopBoundary(BackgroundFill fill, Paint insideFill, Paint outsideFill) {
        checkRegionTopBoundary(fill, insideFill, outsideFill, 1, TOLERANCE);
    }

    protected void checkRegionTopBoundary(BackgroundFill fill, Paint insideFill, Paint outsideFill, int distance, double tolerance) {
        double outermostTop = fill.getInsets().getTop();
        checkRegionTopBoundary(outermostTop, insideFill, outsideFill, distance, tolerance);
    }

    protected void checkRegionTopBoundary(Paint outsideFill) {
        checkRegionTopBoundary(0, null, outsideFill, 1, TOLERANCE);
    }

    protected void checkRegionTopBoundary(double topInset, Paint insideFill, Paint outsideFill, int distance, double tolerance) {
        List<TestParameters> params = new LinkedList<>();
        final int y1 = (int) (region.getLayoutY() + topInset);
        final int midX = WIDTH / 2;
        params.add(new TestParameters(outsideFill, insideFill, midX, y1 - 1 - distance, tolerance)); // top outside
        params.add(new TestParameters(insideFill, outsideFill, midX, y1 + distance, tolerance)); // top inside
        runTests(params);
    }

    protected void checkRegionRightBoundary(BackgroundFill fill, Paint outsideFill) {
        checkRegionRightBoundary(fill, fill.getFill(), outsideFill);
    }

    protected void checkRegionRightBoundary(BackgroundFill fill, Paint insideFill, Paint outsideFill) {
        checkRegionRightBoundary(fill, insideFill, outsideFill, 1, TOLERANCE);
    }

    protected void checkRegionRightBoundary(BackgroundFill fill, Paint insideFill, Paint outsideFill, int distance, double tolerance) {
        double outermostRight = fill.getInsets().getRight();
        checkRegionRightBoundary(outermostRight, insideFill, outsideFill, distance, tolerance);
    }

    protected void checkRegionRightBoundary(Paint outsideFill) {
        checkRegionRightBoundary(0, null, outsideFill, 1, TOLERANCE);
    }

    protected void checkRegionRightBoundary(double rightInset, Paint insideFill, Paint outsideFill, int distance, double tolerance) {
        List<TestParameters> params = new LinkedList<>();
        final int x2 = (int) Math.ceil(region.getLayoutX() + region.getWidth() - rightInset);
        final int midY = HEIGHT / 2;
        params.add(new TestParameters(outsideFill, insideFill, x2 + distance, midY, tolerance)); // right outside
        params.add(new TestParameters(insideFill, outsideFill, x2 - 1 - distance, midY, tolerance)); // right inside
        runTests(params);
    }

    protected void checkRegionBottomBoundary(BackgroundFill fill, Paint outsideFill) {
        checkRegionBottomBoundary(fill, fill.getFill(), outsideFill);
    }

    protected void checkRegionBottomBoundary(BackgroundFill fill, Paint insideFill, Paint outsideFill) {
        checkRegionBottomBoundary(fill, insideFill, outsideFill, 1, TOLERANCE);
    }

    protected void checkRegionBottomBoundary(BackgroundFill fill, Paint insideFill, Paint outsideFill, int distance, double tolerance) {
        double outermostBottom = fill.getInsets().getBottom();
        checkRegionBottomBoundary(outermostBottom, insideFill, outsideFill, distance, tolerance);
    }

    protected void checkRegionBottomBoundary(Paint outsideFill) {
        checkRegionBottomBoundary(0, null, outsideFill, 0, TOLERANCE);
    }

    protected void checkRegionBottomBoundary(double bottomInset, Paint insideFill, Paint outsideFill, int distance, double tolerance) {
        List<TestParameters> params = new LinkedList<>();
        final int y2 = (int) Math.ceil(region.getLayoutY() + region.getHeight() - bottomInset);
        final int midX = WIDTH / 2;
        params.add(new TestParameters(outsideFill, insideFill, midX, y2 + distance, tolerance)); // bottom outside
        params.add(new TestParameters(insideFill, outsideFill, midX, y2 - 1 - distance, tolerance)); // bottom inside
        runTests(params);
    }

    // .5 * 1/Math.sqrt(2). If you multiply this value by the diameter of the
    // circle, then you will get half the length of the side of the square which
    // is inscribed within the circle. This will tell me the point of the arc where
    // I want to do my testing.
    static final float HALF_SQRT_HALF = .35355339059327f;

    protected void checkRegionTopLeftCorner(BackgroundFill fill, Paint outsideFill) {
        checkRegionTopLeftCorner(fill, fill.getFill(), outsideFill);
    }

    protected void checkRegionTopLeftCorner(BackgroundFill fill, Paint insideFill, Paint outsideFill) {
        checkRegionTopLeftCorner(fill, insideFill, outsideFill, 1, TOLERANCE);
    }

    protected void checkRegionTopLeftCorner(BackgroundFill fill, Paint insideFill, Paint outsideFill, int distance, double tolerance) {
        List<TestParameters> params = new LinkedList<>();

        // Now check the corners of each side. Each corner is a quarter-ellipse. What needs to be done is,
        // for each background fill, figure out the x,y that constitutes the center of the arc (45 degrees)
        // and then, for each point, find the one that is closest to the corner of the window. That point
        // will be the one that constitutes the edge of the arc, and we can then test just outside that point
        // and just inside that point.

        final CornerRadii radii = fill.getRadii();
        double hr = radii.getTopLeftHorizontalRadius();
        double h = radii.isTopLeftHorizontalRadiusAsPercentage() ? hr * region.getWidth() : hr;
        double vr = radii.getTopLeftVerticalRadius();
        double v = radii.isTopLeftVerticalRadiusAsPercentage() ? vr * region.getHeight() : vr;
        double topLeftX = region.getLayoutX() + (fill.getInsets().getLeft() + (h - h * 2 * HALF_SQRT_HALF));
        double topLeftY = region.getLayoutY() + (fill.getInsets().getTop() + (v - v * 2 * HALF_SQRT_HALF));
        params.add(new TestParameters(outsideFill, insideFill, (int) topLeftX - 1 - distance, (int) topLeftY - 1 - distance, tolerance)); // outside
        params.add(new TestParameters(insideFill, outsideFill, (int) topLeftX + distance, (int) topLeftY + distance, tolerance)); // inside
        runTests(params);
    }

    protected void checkRegionTopRightCorner(BackgroundFill fill, Paint outsideFill) {
        checkRegionTopRightCorner(fill, fill.getFill(), outsideFill);
    }

    protected void checkRegionTopRightCorner(BackgroundFill fill, Paint insideFill, Paint outsideFill) {
        checkRegionTopRightCorner(fill, insideFill, outsideFill, 1, TOLERANCE);
    }

    protected void checkRegionTopRightCorner(BackgroundFill fill, Paint insideFill, Paint outsideFill, int distance, double tolerance) {
        List<TestParameters> params = new LinkedList<>();
        final CornerRadii radii = fill.getRadii();
        double hr = radii.getTopRightHorizontalRadius();
        double h = radii.isTopRightHorizontalRadiusAsPercentage() ? hr * region.getWidth() : hr;
        double vr = radii.getTopRightVerticalRadius();
        double v = radii.isTopRightVerticalRadiusAsPercentage() ? vr * region.getHeight() : vr;
        double topRightX = region.getLayoutX() + region.getWidth() - (fill.getInsets().getRight() + (h - h * 2 * HALF_SQRT_HALF));
        double topRightY = region.getLayoutY() + (fill.getInsets().getTop() + (v - v * 2 * HALF_SQRT_HALF));
        params.add(new TestParameters(outsideFill, insideFill, (int) Math.ceil(topRightX + distance), (int) topRightY - 1 - distance, tolerance)); // outside
        params.add(new TestParameters(insideFill, outsideFill, (int) Math.ceil(topRightX - distance) - 1, (int) topRightY + distance, tolerance)); // inside
        runTests(params);
    }

    protected void checkRegionBottomRightCorner(BackgroundFill fill, Paint outsideFill) {
        checkRegionBottomRightCorner(fill, fill.getFill(), outsideFill);
    }

    protected void checkRegionBottomRightCorner(BackgroundFill fill, Paint insideFill, Paint outsideFill) {
        checkRegionBottomRightCorner(fill, insideFill, outsideFill, 1, TOLERANCE);
    }

    protected void checkRegionBottomRightCorner(BackgroundFill fill, Paint insideFill, Paint outsideFill, int distance, double tolerance) {
        List<TestParameters> params = new LinkedList<>();
        final CornerRadii radii = fill.getRadii();
        double hr = radii.getBottomRightHorizontalRadius();
        double h = radii.isBottomRightHorizontalRadiusAsPercentage() ? hr * region.getWidth() : hr;
        double vr = radii.getBottomRightVerticalRadius();
        double v = radii.isBottomRightVerticalRadiusAsPercentage() ? vr * region.getHeight() : vr;
        double bottomRightX = region.getLayoutX() + region.getWidth() - (fill.getInsets().getRight() + (h - h * 2 * HALF_SQRT_HALF));
        double bottomRightY = region.getLayoutY() + region.getHeight() - (fill.getInsets().getBottom() + (v - v * 2 * HALF_SQRT_HALF));
        params.add(new TestParameters(outsideFill, insideFill, (int) Math.ceil(bottomRightX + distance), (int) Math.ceil(bottomRightY + distance), tolerance)); // outside
        params.add(new TestParameters(insideFill, outsideFill, (int) Math.ceil(bottomRightX - distance) - 1, (int) Math.ceil(bottomRightY - distance) - 1, tolerance)); // inside
        runTests(params);
    }

    protected void checkRegionBottomLeftCorner(BackgroundFill fill, Paint outsideFill) {
        checkRegionBottomLeftCorner(fill, fill.getFill(), outsideFill);
    }

    protected void checkRegionBottomLeftCorner(BackgroundFill fill, Paint insideFill, Paint outsideFill) {
        checkRegionBottomLeftCorner(fill, insideFill, outsideFill, 1, TOLERANCE);
    }

    protected void checkRegionBottomLeftCorner(BackgroundFill fill, Paint insideFill, Paint outsideFill, int distance, double tolerance) {
        List<TestParameters> params = new LinkedList<>();
        final CornerRadii radii = fill.getRadii();
        double hr = radii.getBottomLeftHorizontalRadius();
        double h = radii.isBottomLeftHorizontalRadiusAsPercentage() ? hr * region.getWidth() : hr;
        double vr = radii.getBottomLeftVerticalRadius();
        double v = radii.isBottomLeftVerticalRadiusAsPercentage() ? vr * region.getHeight() : vr;
        double bottomLeftX = region.getLayoutX() + (fill.getInsets().getLeft() + (h - h * 2 * HALF_SQRT_HALF));
        double bottomLeftY = region.getLayoutY() + region.getHeight() - (fill.getInsets().getBottom() + (v - v * 2 * HALF_SQRT_HALF));
        params.add(new TestParameters(outsideFill, insideFill, (int) bottomLeftX - 1 - distance, (int) Math.ceil(bottomLeftY + distance), tolerance)); // outside
        params.add(new TestParameters(insideFill, outsideFill, (int) bottomLeftX + distance, (int) Math.ceil(bottomLeftY - 1 - distance), tolerance)); // inside
        runTests(params);
    }

    protected void checkRegionCornersAndBoundariesOfBackgroundFill(BackgroundFill fill, Paint outsideFill) {
        checkRegionCornersAndBoundariesOfBackgroundFill(fill, fill.getFill(), outsideFill);
    }

    protected void checkRegionCornersAndBoundariesOfBackgroundFill(BackgroundFill fill, Paint insideFill, Paint lastFill) {
        checkRegionCornersAndBoundariesOfBackgroundFill(fill, insideFill, lastFill, 1, TOLERANCE);
    }

    protected void checkRegionCornersAndBoundariesOfBackgroundFill(BackgroundFill fill, Paint insideFill, Paint lastFill, int distance, double tolerance) {
        checkRegionLeftBoundary(fill, insideFill, lastFill, distance, tolerance);
        checkRegionTopLeftCorner(fill, insideFill, lastFill, distance, tolerance);
        checkRegionTopBoundary(fill, insideFill, lastFill, distance, tolerance);
        checkRegionTopRightCorner(fill, insideFill, lastFill, distance, tolerance);
        checkRegionRightBoundary(fill, insideFill, lastFill, distance, tolerance);
        checkRegionBottomRightCorner(fill, insideFill, lastFill, distance, tolerance);
        checkRegionBottomBoundary(fill, insideFill, lastFill, distance, tolerance);
        checkRegionBottomLeftCorner(fill, insideFill, lastFill, distance, tolerance);
    }

    private void runTests(List<TestParameters> params) {
        for (TestParameters p : params) {
            boolean exactMatch = p.expected instanceof Color;
            if (exactMatch) {
                assertColorEquals((Color) p.expected, p.x, p.y, p.tolerance);
            } else {
                if (!(p.notExpected instanceof Color) ||
                        p.expected == p.notExpected ||
                        (p.expected != null && p.expected.equals(p.notExpected))) {
                    // I won't be able to distinguish, so just have to skip
                    System.err.println("WARNING: Had to skip RegionUITest case because there was not an easy " +
                            "way to distinguish pass vs. fail");
                    continue;
                }
                assertColorDoesNotEqual((Color) p.notExpected, p.x, p.y, p.tolerance);
            }
        }
    }

    private static final class TestParameters {
        Paint expected;
        Paint notExpected;
        int x, y;
        double tolerance;

        TestParameters(Paint expected, Paint notExpected, int x, int y, double tolerance) {
            this.expected = expected;
            this.notExpected = notExpected;
            this.x = x;
            this.y = y;
            this.tolerance = tolerance;
        }
    }
}
