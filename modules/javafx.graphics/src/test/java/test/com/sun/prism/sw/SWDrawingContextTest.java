/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.prism.sw;

import com.sun.glass.ui.Screen;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.sw.SWDrawingContext;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javafx.geometry.VPos;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import test.com.sun.javafx.pgstub.StubImageLoaderFactory;
import test.com.sun.javafx.pgstub.StubPlatformImageInfo;
import test.com.sun.javafx.pgstub.StubToolkit;

public class SWDrawingContextTest {

    private static final int WIDTH = 64;
    private static final int HEIGHT = 64;

    /**
     * Screens are normally installed by the native glass layer. Provide a fake
     * one so that SWDrawingContext can call Screen.getMainScreen().
     */
    private static final Field SCREENS_FIELD;

    static {
        try {
            SCREENS_FIELD = Screen.class.getDeclaredField("screens");

            SCREENS_FIELD.setAccessible(true);
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private final Harness h = createHarness(WIDTH, HEIGHT);

    @BeforeAll
    public static void installMainScreen() throws Exception {
        Screen screen = new Screen(
            0L, 24,
            0, 0, 1920, 1080,
            0, 0, 1920, 1080,
            0, 0, 1920, 1080,
            96, 96,
            1f, 1f, 1f, 1f
        );

        SCREENS_FIELD.set(null, Collections.singletonList(screen));
    }

    @AfterAll
    public static void uninstallMainScreen() throws Exception {
        SCREENS_FIELD.set(null, null);
    }

    @Test
    public void shouldConstructWithIntBufferImage() {
        assertTrue(h.image.isWritable());
    }

    @Test
    public void constructorShouldRejectNullImage() {
        assertThrows(NullPointerException.class, () -> new SWDrawingContext(null, _ -> {}));
    }

    @Test
    public void constructorShouldRejectNullDirtyConsumer() {
        com.sun.prism.Image image = prismImage(WIDTH, HEIGHT);

        assertThrows(NullPointerException.class, () -> new SWDrawingContext(image, null));
    }

    @Test
    public void constructorShouldRejectByteBufferImage() {
        com.sun.prism.Image image = com.sun.prism.Image.fromByteBgraPreData(ByteBuffer.allocate(WIDTH * HEIGHT * 4), WIDTH, HEIGHT);

        assertThrows(IllegalStateException.class, () -> new SWDrawingContext(image, _ -> {}));
    }

    @Test
    public void constructorShouldRejectReadOnlyHeapIntBuffer() {
        IntBuffer readOnly = IntBuffer.allocate(WIDTH * HEIGHT).asReadOnlyBuffer();
        com.sun.prism.Image image = com.sun.prism.Image.fromIntArgbPreData(readOnly, WIDTH, HEIGHT);

        assertThrows(IllegalStateException.class, () -> new SWDrawingContext(image, _ -> {}));
    }

    @Test
    public void constructorShouldRejectReadOnlyDirectIntBuffer() {
        IntBuffer readOnly = ByteBuffer.allocateDirect(WIDTH * HEIGHT * Integer.BYTES)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
            .asReadOnlyBuffer();
        com.sun.prism.Image image = com.sun.prism.Image.fromIntArgbPreData(readOnly, WIDTH, HEIGHT);

        assertThrows(IllegalStateException.class, () -> new SWDrawingContext(image, _ -> {}));
    }

    @Test
    public void constructorShouldRejectTooSmallIntBuffer() {
        IntBuffer small = IntBuffer.allocate(WIDTH * HEIGHT - 1);
        com.sun.prism.Image image = com.sun.prism.Image.fromIntArgbPreData(small, WIDTH, HEIGHT);

        assertThrows(IllegalStateException.class, () -> new SWDrawingContext(image, _ -> {}));
    }

    @Test
    public void shouldDrawIntoHeapIntBuffer() {
        IntBuffer buffer = IntBuffer.allocate(WIDTH * HEIGHT);

        assertTrue(buffer.hasArray());
        assertEquals(0, buffer.arrayOffset());
        assertUsableBuffer(buffer);
    }

    @Test
    public void shouldDrawIntoHeapSliceOfLargerBuffer() {
        int paddingSize = 7;
        int sentinel = 0x11223344;
        int[] array = new int[2 * paddingSize + WIDTH * HEIGHT];

        Arrays.fill(array, 0, paddingSize, sentinel);
        Arrays.fill(array, paddingSize + WIDTH * HEIGHT, paddingSize + WIDTH * HEIGHT + paddingSize, sentinel);

        IntBuffer buffer = IntBuffer.wrap(array);

        buffer.position(paddingSize);

        IntBuffer slice = buffer.slice();

        assertTrue(slice.hasArray());
        assertEquals(paddingSize, slice.arrayOffset());
        assertUsableBuffer(slice);

        for (int i = 0; i < paddingSize; i++) {
            assertEquals(sentinel, array[i], "pixel before the slice must be untouched");
            assertEquals(sentinel, array[i + WIDTH * HEIGHT + paddingSize], "pixel after the slice must be untouched");
        }
    }

    @Test
    public void shouldDrawIntoDirectIntBuffer() {
        IntBuffer buffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer();

        assertTrue(buffer.isDirect());
        assertUsableBuffer(buffer);
    }

    @Test
    public void shouldDrawIntoDirectSliceOfLargerBuffer() {
        int paddingSize = 7;
        int sentinel = 0x11223344;
        IntBuffer buffer = ByteBuffer.allocateDirect((2 * paddingSize + WIDTH * HEIGHT) * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer();

        for (int i = 0; i < paddingSize; i++) {
            buffer.put(i, sentinel);
            buffer.put(i + WIDTH * HEIGHT + paddingSize, sentinel);
        }

        buffer.position(paddingSize);

        IntBuffer slice = buffer.slice();

        assertTrue(slice.isDirect());
        assertUsableBuffer(slice);

        for (int i = 0; i < paddingSize; i++) {
            assertEquals(sentinel, buffer.get(i), "pixel before the slice must be untouched");
            assertEquals(sentinel, buffer.get(i + WIDTH * HEIGHT + paddingSize), "pixel after the slice must be untouched");
        }
    }

    @Test
    public void shouldHaveDefaultAttributeValues() {
        assertEquals(Color.BLACK, h.context.getStroke());
        assertEquals(Color.BLACK, h.context.getFill());
        assertEquals(1.0, h.context.getGlobalAlpha());
        assertEquals(BlendMode.SRC_OVER, h.context.getGlobalBlendMode());
        assertEquals(FillRule.NON_ZERO, h.context.getFillRule());
        assertEquals(1.0, h.context.getLineWidth());
        assertEquals(StrokeLineCap.SQUARE, h.context.getLineCap());
        assertEquals(StrokeLineJoin.MITER, h.context.getLineJoin());
        assertEquals(10.0, h.context.getMiterLimit());
        assertTrue(h.context.isImageSmoothing());
    }

    @Test
    public void shouldGetAndSetStroke() {
        h.context.setStroke(Color.RED);

        assertEquals(Color.RED, h.context.getStroke());

        h.context.setStroke(null);  // a null value is ignored and leaves the value unchanged

        assertEquals(Color.RED, h.context.getStroke());
    }

    @Test
    public void shouldGetAndSetFill() {
        h.context.setFill(Color.BLUE);

        assertEquals(Color.BLUE, h.context.getFill());

        h.context.setFill(null);  // a null value is ignored and leaves the value unchanged

        assertEquals(Color.BLUE, h.context.getFill());
    }

    @Test
    public void shouldStoreTheSetAlphaAndClampForRendering() {
        h.context.setGlobalAlpha(2.0);

        assertEquals(2.0, h.context.getGlobalAlpha());

        h.context.setFill(Color.RED);
        h.context.fillRect(0, 0, 10, 10);

        assertPixel(5, 5, Color.RED);  // the out-of-range alpha is clamped to 1.0 for rendering

        h.context.setGlobalAlpha(-1.0);

        assertEquals(-1.0, h.context.getGlobalAlpha());

        h.context.setGlobalAlpha(0.5);

        assertEquals(0.5, h.context.getGlobalAlpha());
    }

    @Test
    public void shouldIgnoreNullAndRejectUnsupportedBlendModes() {
        h.context.setGlobalBlendMode(null);  // a null value is ignored and leaves the value unchanged

        assertEquals(BlendMode.SRC_OVER, h.context.getGlobalBlendMode());

        // unsupported blend modes are rejected and leave the value unchanged
        assertThrows(UnsupportedOperationException.class, () -> h.context.setGlobalBlendMode(BlendMode.MULTIPLY));

        assertEquals(BlendMode.SRC_OVER, h.context.getGlobalBlendMode());
    }

    @Test
    public void shouldGetAndSetFillRule() {
        h.context.setFillRule(FillRule.EVEN_ODD);

        assertEquals(FillRule.EVEN_ODD, h.context.getFillRule());

        h.context.setFillRule(null);  // a null value is ignored and leaves the value unchanged

        assertEquals(FillRule.EVEN_ODD, h.context.getFillRule());
    }

    @Test
    public void shouldGetAndSetLineWidth() {
        h.context.setLineWidth(3.5);

        assertEquals(3.5, h.context.getLineWidth());

        // non-positive, infinite and NaN values are ignored
        h.context.setLineWidth(0.0);
        h.context.setLineWidth(-2.0);
        h.context.setLineWidth(Double.POSITIVE_INFINITY);
        h.context.setLineWidth(Double.NaN);

        assertEquals(3.5, h.context.getLineWidth());
    }

    @Test
    public void shouldGetAndSetLineCap() {
        h.context.setLineCap(StrokeLineCap.ROUND);

        assertEquals(StrokeLineCap.ROUND, h.context.getLineCap());

        h.context.setLineCap(null);  // a null value is ignored and leaves the value unchanged

        assertEquals(StrokeLineCap.ROUND, h.context.getLineCap());
    }

    @Test
    public void shouldGetAndSetLineJoin() {
        h.context.setLineJoin(StrokeLineJoin.ROUND);

        assertEquals(StrokeLineJoin.ROUND, h.context.getLineJoin());

        h.context.setLineJoin(null);  // a null value is ignored and leaves the value unchanged

        assertEquals(StrokeLineJoin.ROUND, h.context.getLineJoin());
    }

    @Test
    public void shouldGetAndSetMiterLimit() {
        h.context.setMiterLimit(5.0);

        assertEquals(5.0, h.context.getMiterLimit());

        // non-positive and infinite values are ignored.
        h.context.setMiterLimit(0.0);
        h.context.setMiterLimit(-1.0);
        h.context.setMiterLimit(Double.POSITIVE_INFINITY);

        assertEquals(5.0, h.context.getMiterLimit());
    }

    @Test
    public void shouldGetAndSetLineDashes() {
        assertNull(h.context.getLineDashes());

        h.context.setLineDashes(1, 2);

        assertArrayEquals(new double[] {1, 2}, h.context.getLineDashes());

        h.context.setLineDashes(5, 10, 15);  // odd length is doubled

        assertArrayEquals(new double[] {5, 10, 15, 5, 10, 15}, h.context.getLineDashes());

        h.context.setLineDashes(null);  // null disables dashing

        assertNull(h.context.getLineDashes());

        h.context.setLineDashes(2, 3);
        h.context.setLineDashes();  // empty disables dashing

        assertNull(h.context.getLineDashes());

        h.context.setLineDashes(2, 3);
        h.context.setLineDashes(0, 0);  // all zeros disables dashing

        assertNull(h.context.getLineDashes());

        h.context.setLineDashes(2, 3);
        h.context.setLineDashes(-1, 2);  // negative value is ignored

        assertArrayEquals(new double[] {2, 3}, h.context.getLineDashes());

        h.context.setLineDashes(2, 3);
        h.context.setLineDashes(Double.NaN, 2);  // NaN value is ignored

        assertArrayEquals(new double[] {2, 3}, h.context.getLineDashes());

        double[] copy = h.context.getLineDashes();

        copy[0] = 99;

        assertArrayEquals(new double[] {2, 3}, h.context.getLineDashes());  // getter returns a copy
    }

    @Test
    public void shouldGetAndSetLineDashOffset() {
        assertEquals(0.0, h.context.getLineDashOffset());

        h.context.setLineDashOffset(2.5);

        assertEquals(2.5, h.context.getLineDashOffset());

        h.context.setLineDashOffset(Double.POSITIVE_INFINITY);  // infinite is ignored
        h.context.setLineDashOffset(Double.NEGATIVE_INFINITY);
        h.context.setLineDashOffset(Double.NaN);  // NaN is ignored

        assertEquals(2.5, h.context.getLineDashOffset());
    }

    @Test
    public void shouldStrokeWithDashes() {
        h.context.setStroke(Color.RED);
        h.context.setLineWidth(1);
        h.context.setLineDashes(5, 2);
        h.context.setLineCap(StrokeLineCap.BUTT);
        h.context.strokeLine(0, 10.5, WIDTH, 10.5);

        long pattern = 0;

        for (int x = 0; x < WIDTH; x++) {
            int argb = h.image.getArgb(x, 10);

            pattern <<= 1;

            if (argb == 0xffff0000) {
                pattern++;
            }
        }

        assertEquals(0b1111100111110011111001111100111110011111001111100111110011111001L, pattern);
    }

    @Test
    public void shouldStrokeWithDashesAndSquareCaps() {
        h.context.setStroke(Color.RED);
        h.context.setLineWidth(1);
        h.context.setLineDashes(4, 3);
        h.context.setLineCap(StrokeLineCap.SQUARE);
        h.context.strokeLine(0.5, 10.5, WIDTH, 10.5);

        long pattern = 0;

        for (int x = 0; x < WIDTH; x++) {
            int argb = h.image.getArgb(x, 10);

            pattern <<= 1;

            if (argb == 0xffff0000) {
                pattern++;
            }
        }

        // square caps take up half a pixel on both ends of a dash, so pattern is the same as a 5,2 dash with butt caps
        assertEquals(0b1111100111110011111001111100111110011111001111100111110011111001L, pattern);
    }

    @Test
    public void shouldSaveAndRestoreLineDashes() {
        h.context.setLineDashes(5, 5);
        h.context.setLineDashOffset(2.0);

        h.context.save();

        h.context.setLineDashes(1, 2, 3);
        h.context.setLineDashOffset(7.0);

        h.context.restore();

        assertArrayEquals(new double[] {5, 5}, h.context.getLineDashes());
        assertEquals(2.0, h.context.getLineDashOffset());
    }

    @Test
    public void shouldGetAndSetImageSmoothing() {
        h.context.setImageSmoothing(false);

        assertFalse(h.context.isImageSmoothing());

        h.context.setImageSmoothing(true);

        assertTrue(h.context.isImageSmoothing());
    }

    @Test
    public void shouldPaintPixelsForFillRect() {
        h.context.setFill(Color.RED);
        h.context.fillRect(10, 10, 40, 30);

        assertPixel(10, 10, Color.RED);
        assertPixel(30, 25, Color.RED);
        assertPixel(49, 39, Color.RED);

        // outside the rectangle is transparent
        assertPixel(9, 10, Color.TRANSPARENT);
        assertPixel(10, 9, Color.TRANSPARENT);
        assertPixel(50, 10, Color.TRANSPARENT);
        assertPixel(10, 40, Color.TRANSPARENT);
    }

    @Test
    public void shouldIgnoreZeroSizeFillRect() {
        h.context.setFill(Color.RED);
        h.context.fillRect(10, 10, 0, 10);
        h.context.fillRect(10, 10, 10, 0);

        assertTrue(h.dirtyRects.isEmpty());
        assertPixel(10, 10, Color.TRANSPARENT);
    }

    @Test
    public void shouldApplyGlobalAlphaToFillRect() {
        h.context.setFill(Color.RED);
        h.context.setGlobalAlpha(0.5);
        h.context.fillRect(10, 10, 40, 30);

        assertPixel(30, 25, argb(127, 255, 0, 0));
    }

    @Test
    public void shouldCompositeFillRectOverBackground() {
        h.context.setFill(Color.WHITE);
        h.context.fillRect(0, 0, WIDTH, HEIGHT);

        h.context.setFill(new Color(1.0, 0.0, 0.0, 0.5));
        h.context.fillRect(10, 10, 40, 30);

        assertPixel(30, 25, argb(255, 255, 128, 128));
    }

    @Test
    public void shouldClearRegion() {
        h.context.setFill(Color.RED);
        h.context.fillRect(0, 0, WIDTH, HEIGHT);
        h.context.clearRect(10, 20, 30, 40);

        assertPixel(30, 30, Color.TRANSPARENT);

        // the cleared rectangle itself must be transparent
        assertPixel(10, 20, Color.TRANSPARENT);
        assertPixel(39, 20, Color.TRANSPARENT);
        assertPixel(39, 49, Color.TRANSPARENT);
        assertPixel(10, 49, Color.TRANSPARENT);

        // outside the cleared rectangle, the fill must be intact
        assertPixel(5, 15, Color.RED);
        assertPixel(50, 50, Color.RED);
    }

    @Test
    public void shouldPaintStrokeRectBorder() {
        h.context.setStroke(Color.GREEN);
        h.context.setLineWidth(2);
        h.context.strokeRect(10, 10, 30, 20);

        // pixels on the border
        assertPixel(15, 10, Color.GREEN);
        assertPixel(39, 10, Color.GREEN);
        assertPixel(10, 15, Color.GREEN);
        assertPixel(10, 29, Color.GREEN);

        assertPixel(25, 20, Color.TRANSPARENT);  // interior is not painted by a stroke
        assertPixel(5, 5, Color.TRANSPARENT);  // outside the rectangle
    }

    @Test
    public void shouldPaintStrokeLine() {
        h.context.setStroke(Color.BLUE);
        h.context.setLineWidth(2);
        h.context.strokeLine(10, 10, 30, 10);

        assertPixel(20, 10, Color.BLUE);  // middle of the line
        assertPixel(20, 5, Color.TRANSPARENT);  // above the line
    }

    @Test
    public void shouldPaintFillOval() {
        h.context.setFill(Color.GREEN);
        h.context.fillOval(10, 10, 40, 40);

        // center of the oval (Color.GREEN is the CSS color #008000)
        assertPixel(30, 30, Color.GREEN);

        // outside the ellipse
        assertPixel(11, 11, Color.TRANSPARENT);
        assertPixel(50, 30, Color.TRANSPARENT);
    }

    @Test
    public void shouldPaintFillRoundRect() {
        h.context.setFill(Color.CYAN);
        h.context.fillRoundRect(10, 10, 40, 40, 10, 10);

        assertPixel(30, 30, Color.CYAN);  // center
        assertPixel(30, 12, Color.CYAN);  // mid-way along the top edge

        // corners not covered (because rounded)
        assertPixel(10, 10, Color.TRANSPARENT);
        assertPixel(10, 50, Color.TRANSPARENT);
        assertPixel(50, 10, Color.TRANSPARENT);
        assertPixel(50, 50, Color.TRANSPARENT);

        // 5 pixels from corner (towards center) pixels should be covered
        assertPixel(15, 15, Color.CYAN);
        assertPixel(15, 45, Color.CYAN);
        assertPixel(45, 15, Color.CYAN);
        assertPixel(45, 45, Color.CYAN);
    }

    @Test
    public void shouldPaintFillArc() {
        h.context.setFill(Color.MAGENTA);
        h.context.fillArc(10, 10, 40, 40, 0, 90, ArcType.ROUND);

        assertPixel(37, 23, Color.MAGENTA);  // inside the pie (north-east quadrant)
        assertPixel(23, 23, Color.TRANSPARENT);  // outside the pie (north-west quadrant)
    }

    @Test
    public void shouldPaintStrokeArc() {
        h.context.setStroke(Color.BLACK);
        h.context.setLineWidth(2);
        h.context.strokeArc(10, 10, 40, 40, 0, 90, ArcType.OPEN);

        assertPixel(44, 16, Color.BLACK);  // a point on the stroked arc
        assertPixel(16, 16, Color.TRANSPARENT);  // a point outside the arc extent
    }

    @Test
    public void shouldPaintFillPolygon() {
        h.context.setFill(Color.ORANGE);
        h.context.fillPolygon(
            new double[] {10, 54, 32},
            new double[] {10, 10, 54},
            3
        );

        assertPixel(32, 25, Color.ORANGE);  // inside the triangle
        assertPixel(10, 40, Color.TRANSPARENT);  // outside the triangle
    }

    @Test
    public void shouldFillEvenWoundRegionWithNonZero() {

        /*
         * A pentagram: the central pentagon is covered by two of the star's
         * arms (winding number 2), so it is filled under NON_ZERO.
         */

        h.context.setFill(Color.ORANGE);
        h.context.fillPolygon(
            new double[] {32, 46,  9, 55, 18},
            new double[] { 8, 51, 25, 25, 51},
            5
        );

        assertPixel(30, 30, Color.ORANGE);  // inside the central pentagon
        assertPixel(30, 15, Color.ORANGE);  // inside a star arm
    }

    @Test
    public void shouldLeaveEvenWoundRegionEmptyWithEvenOdd() {

        /*
         * A pentagram: the central pentagon has an even winding number
         * and must be empty under EVEN_ODD, while the star arms (winding 1)
         * are still painted.
         */

        h.context.setFillRule(FillRule.EVEN_ODD);
        h.context.setFill(Color.ORANGE);
        h.context.fillPolygon(
            new double[] {32, 46,  9, 55, 18},
            new double[] { 8, 51, 25, 25, 51},
            5
        );

        assertPixel(30, 30, Color.TRANSPARENT);  // inside the central pentagon
        assertPixel(30, 15, Color.ORANGE);  // inside a star arm
    }

    @Test
    public void shouldPaintStrokePolygon() {
        h.context.setStroke(Color.WHITE);
        h.context.setLineWidth(2);
        h.context.strokePolygon(
            new double[] {10, 54, 32},
            new double[] {10, 10, 54},
            3
        );

        // a point on the bottom edge of the triangle
        assertPixel(32, 53, Color.WHITE);

        // the interior is not painted by a stroke
        assertPixel(32, 25, Color.TRANSPARENT);
    }

    @Test
    public void shouldPaintStrokePolyline() {
        h.context.setStroke(Color.WHITE);
        h.context.setLineWidth(2);
        h.context.strokePolyline(
            new double[] {10, 30, 30},
            new double[] {10, 10, 30},
            3
        );

        // a point on the vertical segment
        assertPixel(30, 20, Color.WHITE);

        // a point on the horizontal segment
        assertPixel(20, 10, Color.WHITE);

        // not on the polyline
        assertPixel(10, 30, Color.TRANSPARENT);
    }

    @Test
    public void shouldFillPath() {
        h.context.setFill(Color.RED);
        h.context.beginPath();
        h.context.moveTo(10, 10);
        h.context.lineTo(50, 10);
        h.context.lineTo(30, 50);
        h.context.closePath();
        h.context.fill();

        assertPixel(30, 30, Color.RED);  // inside the triangle
        assertPixel(10, 5, Color.TRANSPARENT);  // above the top edge
    }

    @Test
    public void shouldStrokePath() {
        h.context.setStroke(Color.RED);
        h.context.setLineWidth(2);
        h.context.beginPath();
        h.context.moveTo(10, 10);
        h.context.lineTo(50, 10);
        h.context.lineTo(30, 50);
        h.context.closePath();
        h.context.stroke();

        assertPixel(30, 10, Color.RED);  // on the top edge
        assertPixel(30, 30, Color.TRANSPARENT);  // the interior is not painted
    }

    @Test
    public void shouldFillRectPath() {
        h.context.setFill(Color.RED);
        h.context.beginPath();
        h.context.rect(10, 10, 40, 30);
        h.context.fill();

        assertPixel(30, 25, Color.RED);  // inside the rectangle
        assertPixel(5, 5, Color.TRANSPARENT);  // outside
    }

    @Test
    public void shouldFillArcPath() {
        h.context.setFill(Color.RED);
        h.context.beginPath();
        h.context.arc(30, 30, 20, 20, 0, 360);
        h.context.fill();

        assertPixel(30, 15, Color.RED);  // inside the circle
        assertPixel(30, 8, Color.TRANSPARENT);  // outside the radius
    }

    @Test
    public void shouldFillQuadraticCurvePath() {
        h.context.setFill(Color.RED);
        h.context.beginPath();
        h.context.moveTo(10, 30);
        h.context.quadraticCurveTo(30, 0, 50, 30);
        h.context.closePath();
        h.context.fill();

        assertPixel(30, 20, Color.RED);  // below the curve, above the base line
        assertPixel(30, 10, Color.TRANSPARENT);  // above the curve apex
    }

    @Test
    public void shouldFillBezierCurvePath() {
        h.context.setFill(Color.RED);
        h.context.beginPath();
        h.context.moveTo(10, 30);
        h.context.bezierCurveTo(20, 0, 40, 0, 50, 30);
        h.context.closePath();
        h.context.fill();

        assertPixel(30, 20, Color.RED);  // below the curve
        assertPixel(30, 4, Color.TRANSPARENT);  // above the curve apex
    }

    @Test
    public void isPointInPathShouldWork() {
        h.context.beginPath();
        h.context.moveTo(10, 10);
        h.context.lineTo(50, 10);
        h.context.lineTo(30, 50);
        h.context.closePath();

        assertTrue(h.context.isPointInPath(30, 30));  // inside
        assertFalse(h.context.isPointInPath(5, 5));  // outside
    }

    @Test
    public void clipShouldThrow() {
        assertThrows(UnsupportedOperationException.class, () -> h.context.clip());
    }

    @Test
    public void arcToShouldRoundCorner() {
        h.context.setFill(Color.RED);
        h.context.beginPath();
        h.context.moveTo(10, 30);
        h.context.arcTo(30, 30, 30, 10, 10);
        h.context.closePath();
        h.context.fill();

        assertPixel(24, 24, Color.RED);  // inside the path
        assertPixel(29, 29, Color.TRANSPARENT);  // cut off by the rounded corner
    }

    @Test
    public void arcToOnAnEmptyPathShouldRenderADotWithRoundCaps() {
        h.context.setStroke(Color.RED);
        h.context.setLineWidth(6);
        h.context.setLineCap(StrokeLineCap.ROUND);

        h.context.beginPath();
        h.context.arcTo(10, 10, 30, 10, 5);  // can't draw arc without starting point
        h.context.stroke();

        assertPixel(10, 10, Color.RED);  // center of the dot
        assertPixel(11, 10, Color.RED);  // within the round cap radius
        assertPixel(15, 10, Color.TRANSPARENT);  // outside the dot
    }

    @Test
    public void appendSVGPathShouldPaint() {
        h.context.setFill(Color.RED);
        h.context.beginPath();
        h.context.appendSVGPath("M 10 10 L 50 10 L 30 50 Z");
        h.context.fill();

        assertPixel(30, 30, Color.RED);  // inside the triangle
        assertPixel(10, 5, Color.TRANSPARENT);  // outside
    }

    @Test
    public void appendSVGPathShouldApplyCurrentTransform() {
        h.context.setFill(Color.RED);
        h.context.setTransform(1, 0, 0, 1, 20, 20);
        h.context.beginPath();
        h.context.appendSVGPath("M 10 10 L 50 10 L 30 50 Z");
        h.context.fill();

        assertPixel(50, 50, Color.RED);  // (30, 30) inside the translated triangle
        assertPixel(20, 20, Color.TRANSPARENT);  // outside the translated triangle
    }

    @Test
    public void shouldApplyTransformImmediatelyAtPathConstruction() {
        h.context.setFill(Color.RED);
        h.context.beginPath();
        h.context.moveTo(10, 10);
        h.context.lineTo(50, 10);
        h.context.lineTo(30, 50);
        h.context.closePath();

        h.context.setTransform(1, 0, 0, 1, 20, 20);  // transform changed after construction

        h.context.fill();

        // the coordinates were transformed when added, so the path is unaffected
        assertPixel(30, 30, Color.RED);  // inside the triangle
        assertPixel(40, 40, Color.TRANSPARENT);  // would be inside if the transform applied now
    }

    @Test
    public void beginPathShouldReset() {
        h.context.setFill(Color.RED);
        h.context.beginPath();
        h.context.moveTo(0, 0);
        h.context.lineTo(WIDTH, 0);
        h.context.lineTo(0, HEIGHT);
        h.context.closePath();

        h.context.beginPath();  // reset the first path
        h.context.moveTo(10, 10);
        h.context.lineTo(50, 10);
        h.context.lineTo(30, 50);
        h.context.closePath();
        h.context.fill();

        assertPixel(30, 30, Color.RED);  // the second path is painted
        assertPixel(5, 50, Color.TRANSPARENT);  // the reset path is not
    }

    @Test
    public void shouldFillPathWithEvenOddFillRule() {
        h.context.setFill(Color.RED);
        h.context.setFillRule(FillRule.EVEN_ODD);
        h.context.beginPath();
        h.context.moveTo(10, 10);
        h.context.lineTo(40, 40);
        h.context.lineTo(10, 40);
        h.context.closePath();
        h.context.moveTo(25, 10);
        h.context.lineTo(54, 40);
        h.context.lineTo(25, 40);
        h.context.closePath();
        h.context.fill();

        assertPixel(12, 15, Color.RED);  // covered by one triangle (winding 1)
        assertPixel(30, 35, Color.TRANSPARENT);  // covered by both triangles (winding 2)
    }

    @Test
    public void shouldNotAffectPathOnSaveRestore() {
        h.context.beginPath();
        h.context.moveTo(10, 10);
        h.context.lineTo(50, 10);
        h.context.lineTo(30, 50);
        h.context.closePath();

        h.context.save();
        h.context.restore();

        assertTrue(h.context.isPointInPath(30, 30));  // the path is not saved or restored
    }

    @Test
    public void shouldPaintDrawImage() {
        Image source = createSolidFxImage(8, 8, argb(255, 0, 255, 0));

        h.context.drawImage(source, 0, 0, 8, 8, 10, 10, 8, 8);

        assertPixel(14, 14, Color.LIME);

        // outside the destination rectangle
        assertPixel(9, 9, Color.TRANSPARENT);
        assertPixel(18, 14, Color.TRANSPARENT);
    }

    @Test
    public void shouldScaleDrawImage() {
        Image source = createSolidFxImage(8, 8, argb(255, 0, 255, 0));

        h.context.drawImage(source, 0, 0, 8, 8, 10, 10, 40, 40);

        assertPixel(30, 30, Color.LIME);

        // outside the destination rectangle
        assertPixel(9, 9, Color.TRANSPARENT);
    }

    @Test
    public void shouldIgnoreNullImage() {
        h.context.setFill(Color.RED);
        h.context.fillRect(0, 0, WIDTH, HEIGHT);

        int dirtyCount = h.dirtyRects.size();

        h.context.drawImage(null, 0, 0);

        assertEquals(dirtyCount, h.dirtyRects.size());
        assertPixel(10, 10, Color.RED);
    }

    @Test
    public void shouldIgnoreInProgressImage() {
        h.context.setFill(Color.RED);
        h.context.fillRect(0, 0, WIDTH, HEIGHT);

        StubImageLoaderFactory factory = ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory();

        factory.registerImage("file:slow.png", new StubPlatformImageInfo(8, 8));
        Image inProgress = new Image("file:slow.png", true);

        h.context.drawImage(inProgress, 5, 5);

        // the image has not finished loading, so nothing is drawn
        assertPixel(10, 10, Color.RED);
    }

    @Test
    public void shouldReportDirtyRegionForFillRect() {
        h.context.setFill(Color.RED);
        h.context.fillRect(10, 10, 50, 30);

        assertEquals(1, h.dirtyRects.size());
        assertEquals(new Rectangle(10, 10, 50, 30), h.dirtyRects.get(0));
    }

    @Test
    public void shouldClipDirtyRegionToImageBounds() {
        h.context.setFill(Color.RED);
        h.context.fillRect(60, 60, 10, 10);

        // only the part inside the image can be dirty
        assertEquals(1, h.dirtyRects.size());
        assertEquals(new Rectangle(60, 60, 4, 4), h.dirtyRects.get(0));
    }

    @Test
    public void shouldClipPixelsAndDirtyRegionForOffscreenFill() {
        h.context.setFill(Color.RED);
        h.context.fillRect(-5, -5, 10, 10);

        // the renderer clips to the image bounds
        assertPixel(0, 0, Color.RED);
        assertPixel(4, 4, Color.RED);
        assertPixel(5, 5, Color.TRANSPARENT);

        // the reported dirty region should also be clipped
        assertEquals(new Rectangle(0, 0, 5, 5), h.dirtyRects.get(0));
    }

    @Test
    public void shouldReportDirtyRegionContainingPaintedLine() {
        h.context.setStroke(Color.BLUE);
        h.context.strokeLine(10, 10, 30, 10);

        assertEquals(1, h.dirtyRects.size());
        assertTrue(h.dirtyRects.get(0).contains(paintedLineBounds(10, 10, 30, 10)), "dirty region must cover the painted line");
    }

    @Test
    public void shouldReportDirtyRegionForReversedLine() {
        h.context.setStroke(Color.BLUE);
        h.context.strokeLine(30, 10, 10, 10);

        assertEquals(1, h.dirtyRects.size());
        assertTrue(h.dirtyRects.get(0).contains(paintedLineBounds(10, 10, 30, 10)), "dirty region must cover the painted line regardless of point order");
    }

    @Test
    public void shouldGetAndSetTransform() {
        h.context.setTransform(2, 0, 0, 3, 4, 5);

        Affine a = h.context.getTransform();

        assertEquals(2.0, a.getMxx());
        assertEquals(0.0, a.getMyx());
        assertEquals(0.0, a.getMxy());
        assertEquals(3.0, a.getMyy());
        assertEquals(4.0, a.getTx());
        assertEquals(5.0, a.getTy());

        h.context.setTransform(new Affine(1, 0, 7, 0, 1, 8));

        assertEquals(7.0, h.context.getTransform().getTx());
        assertEquals(8.0, h.context.getTransform().getTy());

        h.context.setTransform((Affine) null);  // a null value is ignored and leaves the value unchanged

        assertEquals(7.0, h.context.getTransform().getTx());
        assertEquals(8.0, h.context.getTransform().getTy());
    }

    @Test
    public void shouldGetTransformFillingProvidedAffine() {
        h.context.setTransform(2, 0, 0, 3, 4, 5);

        Affine out = new Affine();
        Affine returned = h.context.getTransform(out);

        assertSame(out, returned);
        assertEquals(2.0, out.getMxx());
        assertEquals(0.0, out.getMyx());
        assertEquals(0.0, out.getMxy());
        assertEquals(3.0, out.getMyy());
        assertEquals(4.0, out.getTx());
        assertEquals(5.0, out.getTy());

        Affine fresh = h.context.getTransform((Affine) null);  // creates a new object

        assertNotSame(fresh, out);
        assertEquals(4.0, fresh.getTx());
        assertEquals(5.0, fresh.getTy());
    }

    @Test
    public void shouldTranslateDrawingWithTransform() {
        h.context.setFill(Color.RED);

        h.context.setTransform(1, 0, 0, 1, 10, 20);
        h.context.fillRect(0, 0, 5, 5);

        assertPixel(12, 22, Color.RED);  // inside the translated rectangle
        assertPixel(3, 3, Color.TRANSPARENT);  // outside the translated rectangle
    }

    @Test
    public void shouldRotateDrawingWithTransform() {
        h.context.setFill(Color.RED);

        /*
         * 45 degree rotation combined with a translation. Maps the 10x10 rect
         * at (10, 10) onto a diamond centered at (32, 32), whose vertices lie on
         * the diagonals (32, 25), (39, 32), (32, 39), (25, 32).
         */

        Affine rotate = new Affine();

        // rotate by 45 degrees, then translate so the diamond lands roughly centered
        rotate.appendRotation(45);
        rotate.prependTranslation(32, 10);

        h.context.setTransform(rotate);
        h.context.fillRect(10, 10, 10, 10);

        assertPixel(32, 32, Color.RED);  // center of the rotated rectangle
        assertPixel(34, 34, Color.RED);  // inside, near the lower-right corner
        assertPixel(22, 32, Color.TRANSPARENT);  // outside, left of the diamond
        assertPixel(32, 16, Color.TRANSPARENT);  // outside, above the diamond
    }

    @Test
    public void shouldShearDrawingWithTransform() {
        h.context.setFill(Color.RED);

        h.context.setTransform(1, 0, 1, 1, 0, 0);
        h.context.fillRect(10, 10, 10, 10);

        assertPixel(30, 15, Color.RED);  // inside the sheared rectangle
        assertPixel(10, 10, Color.TRANSPARENT);  // outside the sheared rectangle
    }

    @Test
    public void shouldReportTransformedDirtyRegion() {
        h.context.setFill(Color.RED);

        h.context.setTransform(1, 0, 0, 1, 10, 20);
        h.context.fillRect(0, 0, 5, 5);

        assertEquals(1, h.dirtyRects.size());
        assertEquals(new Rectangle(10, 20, 5, 5), h.dirtyRects.get(0));
    }

    @Test
    public void pathStrokeWidthShouldMatchBasicShapeStrokeUnderScale() {
        h.context.setTransform(2, 0, 0, 2, 0, 0);  // scale x2
        h.context.setStroke(Color.RED);
        h.context.setLineWidth(3);

        // basic stroked line
        h.context.strokeLine(10, 10, 30, 10);

        // path stroked line
        h.context.beginPath();
        h.context.moveTo(10, 30);
        h.context.lineTo(30, 30);
        h.context.stroke();

        double basicLineStrokeWidth = paintedInColumn(40, 15, 30);
        double pathLineStrokeWidth = paintedInColumn(40, 55, 64);

        // a 3 pixel wide line, with scale of 2 should be 6 pixels wide:
        assertEquals(6, basicLineStrokeWidth);
        assertEquals(6, pathLineStrokeWidth);
    }

    @Test
    public void clipShouldNotMoveWhenTheTransformChangesLater() {
        h.context.setFill(Color.RED);
        h.context.clipRect(10, 10, 10, 10);  // frozen clip in device space

        h.context.setTransform(2, 0, 0, 2, 0, 0);  // the transform changes after the clip was set

        h.context.fillRect(0, 0, WIDTH, HEIGHT);

        assertPixel(15, 15, Color.RED);  // inside the frozen clip
        assertPixel(30, 30, Color.TRANSPARENT);  // outside it
    }

    @Test
    public void clipRectShouldNotResetTheCurrentPath() {
        h.context.setFill(Color.RED);
        h.context.beginPath();
        h.context.moveTo(0, 0);
        h.context.lineTo(WIDTH, 0);
        h.context.lineTo(0, HEIGHT);
        h.context.closePath();

        h.context.clipRect(10, 10, 30, 30);

        h.context.fill();  // the path must still be intact after clipRect

        assertPixel(20, 20, Color.RED);  // inside the clip and the path
        assertPixel(5, 5, Color.TRANSPARENT);  // outside the clip
    }

    @Test
    public void everyFillShouldReportDirtyRegionCoveringThePixels() {
        h.context.setFill(Color.RED);

        assertDirtyCoversOnEmptyImage("fillRect", () -> h.context.fillRect(10, 10, 40, 20));
        assertDirtyCoversOnEmptyImage("fillRoundRect", () -> h.context.fillRoundRect(10, 10, 40, 20, 8, 8));
        assertDirtyCoversOnEmptyImage("fillOval", () -> h.context.fillOval(10, 10, 40, 20));
        assertDirtyCoversOnEmptyImage("fillArc", () -> h.context.fillArc(10, 10, 40, 20, 0, 90, ArcType.OPEN));
        assertDirtyCoversOnEmptyImage("fillPolygon", () -> h.context.fillPolygon(new double[] {10, 50, 30}, new double[] {10, 10, 50}, 3));
        assertDirtyCoversOnEmptyImage("fillText", () -> h.context.fillText("M", 10, 20));
        assertDirtyCoversOnEmptyImage("path fill", () -> {
            h.context.beginPath();
            h.context.moveTo(10, 10);
            h.context.lineTo(50, 10);
            h.context.lineTo(30, 50);
            h.context.closePath();
            h.context.fill();
        });
    }

    @Test
    public void everyStrokeShouldReportDirtyRegionCoveringTheStroke() {
        h.context.setStroke(Color.RED);
        h.context.setLineWidth(6);  // the stroke extends beyond the shape geometry

        assertDirtyCoversOnEmptyImage("strokeLine", () -> h.context.strokeLine(10, 10, 50, 10));
        assertDirtyCoversOnEmptyImage("strokeRect", () -> h.context.strokeRect(10, 10, 40, 20));
        assertDirtyCoversOnEmptyImage("strokeRoundRect", () -> h.context.strokeRoundRect(10, 10, 40, 20, 8, 8));
        assertDirtyCoversOnEmptyImage("strokeOval", () -> h.context.strokeOval(10, 10, 40, 20));
        assertDirtyCoversOnEmptyImage("strokeArc", () -> h.context.strokeArc(10, 10, 40, 20, 0, 90, ArcType.OPEN));
        assertDirtyCoversOnEmptyImage("strokePolygon", () -> h.context.strokePolygon(new double[] {10, 50, 30}, new double[] {10, 10, 50}, 3));
        assertDirtyCoversOnEmptyImage("strokePolyline", () -> h.context.strokePolyline(new double[] {10, 10, 50}, new double[] {10, 50, 50}, 3));
        assertDirtyCoversOnEmptyImage("strokeText", () -> h.context.strokeText("M", 10, 20));
        assertDirtyCoversOnEmptyImage("path stroke", () -> {
            h.context.beginPath();
            h.context.moveTo(10, 10);
            h.context.lineTo(50, 10);
            h.context.lineTo(30, 50);
            h.context.closePath();
            h.context.stroke();
        });
    }

    @Test
    public void italicFillTextDirtyShouldCoverGlyphsThatOverhangTheAdvance() {
        h.context.setFill(Color.RED);
        h.context.setFont(Font.font("System", FontPosture.ITALIC, 20));

        assertDirtyCoversOnEmptyImage("fillText italic", () -> h.context.fillText("M", 10, 20));
    }

    @Test
    public void imageShouldReportDirtyRegionCoveringTheChangedPixels() {
        Image source = createSolidFxImage(8, 8, argb(255, 0, 255, 0));

        assertDirtyCoversOnEmptyImage("drawImage", () -> h.context.drawImage(source, 0, 0, 8, 8, 10, 10, 40, 20));
    }

    @Test
    public void clearShouldReportDirtyRegionCoveringTheChangedPixels() {
        assertDirtyCoversChanged(Color.RED, "clearRect", () -> h.context.clearRect(10, 10, 40, 20));
    }

    private void assertDirtyCoversOnEmptyImage(String name, Runnable operation) {
        assertDirtyCoversChanged(Color.TRANSPARENT, name, operation);
    }

    @Test
    public void shouldClipRect() {
        h.context.setFill(Color.RED);
        h.context.fillRect(0, 0, WIDTH, HEIGHT);

        h.context.clipRect(10, 10, 20, 20);
        h.context.setFill(Color.BLUE);
        h.context.fillRect(0, 0, WIDTH, HEIGHT);

        assertPixel(20, 20, Color.BLUE);  // inside the clip
        assertPixel(5, 5, Color.RED);  // outside the clip
    }

    @Test
    public void shouldClipRectWithTransform() {
        h.context.setFill(Color.RED);
        h.context.fillRect(0, 0, WIDTH, HEIGHT);

        h.context.setTransform(1, 0, 0, 1, 10, 0);
        h.context.clipRect(0, 0, 10, HEIGHT);
        h.context.setFill(Color.BLUE);
        h.context.fillRect(0, 0, WIDTH, HEIGHT);

        assertPixel(15, 20, Color.BLUE);  // inside the transformed clip
        assertPixel(5, 20, Color.RED);  // outside the transformed clip
    }

    @Test
    public void shouldIntersectClipRects() {
        h.context.setFill(Color.RED);
        h.context.fillRect(0, 0, WIDTH, HEIGHT);

        h.context.clipRect(10, 10, 30, 30);
        h.context.clipRect(20, 20, 30, 30);
        h.context.setFill(Color.BLUE);
        h.context.fillRect(0, 0, WIDTH, HEIGHT);

        assertPixel(25, 25, Color.BLUE);  // inside the intersection of both clips
        assertPixel(15, 15, Color.RED);  // inside the first clip only
    }

    @Test
    public void shouldThrowWhenClippingUnderRotatedTransform() {
        h.context.setTransform(0, 1, -1, 0, 0, 0);

        assertThrows(UnsupportedOperationException.class, () -> h.context.clipRect(0, 0, 10, 10));
    }

    @Test
    public void rotatedTransformsShouldBeAllowedWithClip() {
        h.context.setFill(Color.RED);
        h.context.clipRect(0, 0, 10, 10);

        /*
         * A clip is set in device space, so changing the transform
         * afterwards (even to a rotation) is allowed and should not move it
         */

        h.context.setTransform(0, 1, -1, 0, 0, 0);
        h.context.rotate(45);
        h.context.transform(0, 1, -1, 0, 0, 0);
        h.context.transform(new Affine(0, -1, 0, 1, 0, 0));

        h.context.setTransform(1, 0, 0, 1, 0, 0);
        h.context.fillRect(0, 0, WIDTH, HEIGHT);

        assertPixel(5, 5, Color.RED);  // inside the clip
        assertPixel(40, 40, Color.TRANSPARENT);  // outside it
    }

    @Test
    public void shouldTranslateAndScaleWithConcatenateMethods() {
        h.context.setFill(Color.RED);

        h.context.translate(10, 20);
        h.context.scale(2, 2);
        h.context.fillRect(0, 0, 5, 5);

        assertPixel(12, 22, Color.RED);  // inside the translated and scaled rectangle
        assertPixel(8, 8, Color.TRANSPARENT);  // outside
        assertPixel(25, 25, Color.TRANSPARENT);  // outside
    }

    @Test
    public void shouldConcatenateTransformOrder() {
        h.context.translate(10, 20);
        h.context.rotate(90);

        Affine a = h.context.getTransform();

        assertEquals(0.0, a.getMxx());
        assertEquals(1.0, a.getMyx());
        assertEquals(-1.0, a.getMxy());
        assertEquals(-0.0, a.getMyy());
        assertEquals(10.0, a.getTx());
        assertEquals(20.0, a.getTy());
    }

    @Test
    public void shouldConcatenateAffineTransform() {
        h.context.transform(new Affine(2, 0, 5, 0, 3, 7));  // scale(2, 3) + translate(5, 7)

        Affine a = h.context.getTransform();

        assertEquals(2.0, a.getMxx());
        assertEquals(0.0, a.getMyx());
        assertEquals(0.0, a.getMxy());
        assertEquals(3.0, a.getMyy());
        assertEquals(5.0, a.getTx());
        assertEquals(7.0, a.getTy());

        h.context.transform(null);  // a null value is ignored and leaves the value unchanged

        assertEquals(5.0, h.context.getTransform().getTx());
    }

    @Test
    public void shouldConcatenateTransformWithSixDoubles() {
        h.context.setTransform(1, 0, 0, 1, 10, 20);
        h.context.transform(2, 0, 0, 3, 0, 0);

        Affine a = h.context.getTransform();

        assertEquals(2.0, a.getMxx());
        assertEquals(3.0, a.getMyy());
        assertEquals(10.0, a.getTx());
        assertEquals(20.0, a.getTy());
    }

    @Test
    public void shouldClipRectWithScaleTransform() {
        h.context.setFill(Color.RED);
        h.context.fillRect(0, 0, WIDTH, HEIGHT);

        h.context.setTransform(2, 0, 0, 2, 0, 0);
        h.context.clipRect(0, 0, 10, 10);
        h.context.setFill(Color.BLUE);
        h.context.fillRect(0, 0, WIDTH, HEIGHT);

        assertPixel(15, 15, Color.BLUE);  // inside the scaled clip
        assertPixel(25, 25, Color.RED);  // outside the scaled clip
    }

    @Test
    public void shouldSaveAndRestoreAttributes() {
        h.context.setFill(Color.RED);
        h.context.setGlobalAlpha(0.5);

        h.context.save();

        h.context.setFill(Color.BLUE);
        h.context.setGlobalAlpha(1.0);

        h.context.restore();

        assertEquals(Color.RED, h.context.getFill());
        assertEquals(0.5, h.context.getGlobalAlpha());
    }

    @Test
    public void shouldSaveAndRestoreTransform() {
        h.context.setTransform(1, 0, 0, 1, 10, 20);

        h.context.save();

        h.context.setTransform(2, 0, 0, 2, 0, 0);

        h.context.restore();

        Affine a = h.context.getTransform();

        assertEquals(1.0, a.getMxx());
        assertEquals(10.0, a.getTx());
        assertEquals(20.0, a.getTy());
    }

    @Test
    public void shouldSaveAndRestoreClip() {
        h.context.setFill(Color.RED);
        h.context.fillRect(0, 0, WIDTH, HEIGHT);

        h.context.clipRect(10, 10, 30, 30);
        h.context.save();

        h.context.clipRect(20, 20, 30, 30);
        h.context.setFill(Color.BLUE);
        h.context.fillRect(0, 0, WIDTH, HEIGHT);

        assertPixel(25, 25, Color.BLUE);  // inside the intersection of both clips
        assertPixel(15, 15, Color.RED);  // inside the first clip only

        h.context.restore();

        h.context.setFill(Color.GREEN);
        h.context.fillRect(0, 0, WIDTH, HEIGHT);

        assertPixel(15, 15, Color.GREEN);  // inside the restored clip
        assertPixel(5, 5, Color.RED);  // outside the restored clip
    }

    @Test
    public void shouldRestoreWithEmptyStackDoNothing() {
        h.context.setFill(Color.RED);

        h.context.restore();

        assertEquals(Color.RED, h.context.getFill());
    }

    @Test
    public void shouldSaveAndRestoreTextAttributes() {
        h.context.setFont(Font.font(24));
        h.context.setTextAlign(TextAlignment.CENTER);
        h.context.setTextBaseline(VPos.CENTER);
        h.context.setFontSmoothingType(FontSmoothingType.LCD);

        h.context.save();

        h.context.setFont(Font.font(12));
        h.context.setTextAlign(TextAlignment.RIGHT);
        h.context.setTextBaseline(VPos.BOTTOM);
        h.context.setFontSmoothingType(FontSmoothingType.GRAY);

        h.context.restore();

        assertEquals(Font.font(24), h.context.getFont());
        assertEquals(TextAlignment.CENTER, h.context.getTextAlign());
        assertEquals(VPos.CENTER, h.context.getTextBaseline());
        assertEquals(FontSmoothingType.LCD, h.context.getFontSmoothingType());
    }

    @Test
    public void shouldGetAndSetFont() {
        Font f = Font.font(24);

        h.context.setFont(f);

        assertEquals(f, h.context.getFont());

        h.context.setFont(null);  // a null value is ignored and leaves the value unchanged

        assertEquals(f, h.context.getFont());
    }

    @Test
    public void shouldGetAndSetTextAlign() {
        h.context.setTextAlign(TextAlignment.CENTER);

        assertEquals(TextAlignment.CENTER, h.context.getTextAlign());

        h.context.setTextAlign(null);  // a null value is ignored and leaves the value unchanged

        assertEquals(TextAlignment.CENTER, h.context.getTextAlign());
    }

    @Test
    public void shouldGetAndSetTextBaseline() {
        h.context.setTextBaseline(VPos.CENTER);

        assertEquals(VPos.CENTER, h.context.getTextBaseline());

        h.context.setTextBaseline(null);  // a null value is ignored and leaves the value unchanged

        assertEquals(VPos.CENTER, h.context.getTextBaseline());
    }

    @Test
    public void shouldGetAndSetFontSmoothingType() {
        h.context.setFontSmoothingType(FontSmoothingType.LCD);

        assertEquals(FontSmoothingType.LCD, h.context.getFontSmoothingType());

        h.context.setFontSmoothingType(null);  // a null value is ignored and leaves the value unchanged

        assertEquals(FontSmoothingType.LCD, h.context.getFontSmoothingType());
    }

    @Test
    public void shouldFillText() {
        h.context.setFill(Color.BLACK);
        h.context.setFont(Font.font(24));

        h.context.fillText("M", 10, 20);

        assertTrue(hasPaintedPixel(8, 5, 40, 40));  // the glyph should be painted at the given position
        assertFalse(hasPaintedPixel(50, 50, 55, 55));  // nothing should be painted far away
    }

    @Test
    public void shouldStrokeText() {
        h.context.setStroke(Color.BLACK);
        h.context.setFont(Font.font(24));

        h.context.strokeText("M", 10, 20);

        assertTrue(hasPaintedPixel(8, 5, 40, 40));  // the glyph outline should be painted at the given position
    }

    @Test
    public void shouldIgnoreNullText() {
        h.context.fillText(null, 10, 20);
        h.context.strokeText(null, 10, 20);

        assertFalse(hasPaintedPixel(0, 0, WIDTH, HEIGHT));
    }

    private static class Harness {
        final com.sun.prism.Image image;
        final List<Rectangle> dirtyRects = new ArrayList<>();

        SWDrawingContext context;

        Harness(com.sun.prism.Image image) {
            this.image = image;
        }
    }

    private static Harness createHarness(int w, int h) {
        IntBuffer buffer = IntBuffer.allocate(w * h);
        com.sun.prism.Image image = com.sun.prism.Image.fromIntArgbPreData(buffer, w, h);
        Harness harness = new Harness(image);

        harness.context = new SWDrawingContext(image, rect -> harness.dirtyRects.add(rect));

        return harness;
    }

    private static com.sun.prism.Image prismImage(int w, int h) {
        return com.sun.prism.Image.fromIntArgbPreData(IntBuffer.allocate(w * h), w, h);
    }

    /*
     * Test draw inside the given buffer, and checks if the correct pixels were modified.
     */
    private static void assertUsableBuffer(IntBuffer buffer) {
        com.sun.prism.Image image = com.sun.prism.Image.fromIntArgbPreData(buffer, WIDTH, HEIGHT);
        SWDrawingContext context = new SWDrawingContext(image, _ -> { });

        context.setFill(Color.WHITE);
        context.fillRect(0, 0, WIDTH, HEIGHT);
        context.setFill(Color.RED);
        context.fillRect(5, 5, 15, 15);

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                boolean inside = x >= 5 && y >= 5 && x < 20 && y < 20;

                assertEquals(
                    inside ? 0xFFFF0000 : 0xFFFFFFFF,
                    buffer.get(x + y * WIDTH),
                    "expected [" + x + ", " + y + "] to be " + (inside ? "red" : "white")
                );
            }
        }
    }

    private static Image createSolidFxImage(int w, int h, int argbpre) {
        int[] pixels = new int[w * h];

        Arrays.fill(pixels, argbpre);

        com.sun.prism.Image prismImage = com.sun.prism.Image.fromIntArgbPreData(pixels, w, h);
        StubImageLoaderFactory factory = ((StubToolkit) Toolkit.getToolkit()).getImageLoaderFactory();

        factory.registerImage(prismImage, new StubPlatformImageInfo(w, h));

        return Toolkit.getImageAccessor().fromPlatformImage(prismImage);
    }

    private static int argb(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void assertPixel(int x, int y, Color expectedColor) {
        assertPixel(x, y, argb(
            (int) Math.round(expectedColor.getOpacity() * 255),
            (int) Math.round(expectedColor.getRed() * 255),
            (int) Math.round(expectedColor.getGreen() * 255),
            (int) Math.round(expectedColor.getBlue() * 255)
        ));
    }

    private void assertPixel(int x, int y, int expectedArgb) {
        assertEquals("0x%08x".formatted(expectedArgb), "0x%08x".formatted(h.image.getArgb(x, y)), "pixel at (" + x + ", " + y + ")");
    }

    private boolean hasPaintedPixel(int x1, int y1, int x2, int y2) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                if (h.image.getArgb(x, y) != 0) {
                    return true;
                }
            }
        }

        return false;
    }

    private int paintedInColumn(int x, int y1, int y2) {
        int count = 0;

        for (int y = y1; y < y2; y++) {
            if (h.image.getArgb(x, y) != 0) {
                count++;
            }
        }

        return count;
    }

    /*
     * Bounding box of the pixels painted by a SQUARE-capped stroke of the
     * default width (1.0) drawn between the two given points.
     */
    private static Rectangle paintedLineBounds(double x1, double y1, double x2, double y2) {
        double minX = Math.min(x1, x2) - 0.5;
        double maxX = Math.max(x1, x2) + 0.5;
        double minY = Math.min(y1, y2) - 0.5;
        double maxY = Math.max(y1, y2) + 0.5;

        return new Rectangle(
            (int) Math.floor(minX),
            (int) Math.floor(minY),
            (int) Math.ceil(maxX) - (int) Math.floor(minX),
            (int) Math.ceil(maxY) - (int) Math.floor(minY)
        );
    }

    /*
     * Runs a drawing operation and assert that a single dirty rectangle was
     * reported and that it covers every pixel the operation changed.
     */
    private void assertDirtyCoversChanged(Color background, String name, Runnable operation) {
        h.context.save();
        h.context.clearRect(0, 0, WIDTH, HEIGHT);
        h.context.setFill(background);
        h.context.fillRect(0, 0, WIDTH, HEIGHT);
        h.context.restore();

        int[] before = snapshotPixels();

        h.dirtyRects.clear();

        operation.run();

        int[] after = snapshotPixels();
        Rectangle changed = boundsOfChangedPixels(before, after);

        assertNotNull(changed, name + " should have changed some pixels");
        assertTrue(h.dirtyRects.size() == 1, name + " should have reported a single dirty region");

        Rectangle dirty = h.dirtyRects.get(0);

        assertTrue(
            dirty.contains(changed.x, changed.y, changed.width, changed.height),
            name + ": dirty region " + dirty + " must cover the changed pixels " + changed
        );
    }

    private int[] snapshotPixels() {
        return ((IntBuffer) h.image.getPixelBuffer()).array().clone();
    }

    private static Rectangle boundsOfChangedPixels(int[] before, int[] after) {
        int minX = WIDTH;
        int minY = HEIGHT;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (before[y * WIDTH + x] != after[y * WIDTH + x]) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        return maxX < 0 ? null : new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
