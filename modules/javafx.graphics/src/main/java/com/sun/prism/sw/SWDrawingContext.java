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

package com.sun.prism.sw;

import com.sun.glass.utils.NativeLibLoader;
import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.geom.Arc2D;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.IllegalPathStateException;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import com.sun.javafx.scene.text.FontHelper;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.text.PrismTextLayoutFactory;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.util.FXCleaner;
import com.sun.prism.BasicStroke;
import com.sun.prism.CompositeMode;
import com.sun.prism.Graphics;
import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import com.sun.prism.Texture.Usage;

import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Consumer;

import javafx.geometry.VPos;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.DrawingContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;

/**
 * A software-based drawing context for {@link com.sun.prism.Image} that allows direct rendering of shapes, paths, and
 * images into a Prism image buffer.
 * <p>
 * This class provides a familiar JavaFX-style drawing API for Prism images, enabling modification of the image contents
 * using lines, rectangles, ovals, rounded rectangles, arcs, polygons, and images without the need for a Canvas or
 * {@link javafx.scene.image.PixelWriter}.
 * <p>
 * It uses Prism's {@link Graphics} and {@link BasicStroke} for rendering, supporting strokes, fills, alpha
 * transparency, limited blend modes, and automatic dirty-region tracking to efficiently mark affected pixels.
 * <p>
 * Features include:
 * <ul>
 *     <li>Stroke and fill management (line width, line caps, joins, miter limits, dashes).
 *     <li>Global alpha and support for {@link javafx.scene.effect.BlendMode#SRC_OVER} and
 *         {@link javafx.scene.effect.BlendMode#ADD}.
 *     <li>Drawing of lines, rectangles, rounded rectangles, ovals, arcs, polygons, polylines, and images.
 *     <li>Automatic dirty-region tracking for efficient pixel updates.
 * </ul>
 * <p>
 * Limitations:
 * <ul>
 *     <li>Only SRC_OVER blend mode is supported; others will throw an exception.
 *     <li>Arbitrary path clipping is not supported, only {@link #clipRect(double, double, double, double)}.
 * </ul>
 * <p>
 * This class is intended for use in contexts where direct drawing into a Prism image is needed, such as the
 * {@code getDrawingContext()} method in WritableImage, providing a more convenient API than PixelWriter or snapshotting
 * a Canvas.
 *
 * @see DrawingContext
 * @see com.sun.prism.Image
 * @since 28
 */
public class SWDrawingContext implements DrawingContext {

    static {
        NativeLibLoader.loadLibrary("prism_sw");
    }

    private final SWGraphics graphics;
    private final SWResourceFactory resourceFactory;
    private final Consumer<Rectangle> pixelsDirty;
    private final int imageWidth;
    private final int imageHeight;
    private final Affine2D transform = new Affine2D();
    private final Deque<State> stateStack = new ArrayDeque<>();

    private Rectangle clip;  // in device space

    // Common rendering attributes
    private double globalAlpha = 1.0;
    private BlendMode globalBlendMode = BlendMode.SRC_OVER;

    // Fill attributes
    private Paint fill = Color.BLACK;

    // Stroke attributes
    private Paint stroke = Color.BLACK;
    private double lineWidth = 1.0;
    private StrokeLineCap lineCap = StrokeLineCap.SQUARE;
    private StrokeLineJoin lineJoin = StrokeLineJoin.MITER;
    private double miterLimit = 10.0;
    private double[] lineDashes;
    private double lineDashOffset;

    // Path attributes
    private FillRule fillRule = FillRule.NON_ZERO;

    // Image attributes
    private boolean imageSmoothing = true;

    // Text attributes
    private Font font = Font.getDefault();
    private TextAlignment textAlign = TextAlignment.LEFT;
    private VPos textBaseline = VPos.BASELINE;
    private FontSmoothingType fontSmoothingType = FontSmoothingType.GRAY;

    // Path attributes
    private final Path2D path = new Path2D();
    private final float[] coords = new float[6];

    // Cached prism values
    private com.sun.prism.paint.Paint prismFillPaint = com.sun.prism.paint.Color.BLACK;
    private com.sun.prism.paint.Paint prismStrokePaint = com.sun.prism.paint.Color.BLACK;
    private BasicStroke prismStroke;

    private record State(
        double globalAlpha,
        BlendMode globalBlendMode,
        Paint fill,
        Paint stroke,
        double lineWidth,
        StrokeLineCap lineCap,
        StrokeLineJoin lineJoin,
        double miterLimit,
        double[] lineDashes,
        double lineDashOffset,
        FillRule fillRule,
        boolean imageSmoothing,
        Font font,
        TextAlignment textAlign,
        VPos textBaseline,
        FontSmoothingType fontSmoothingType,
        double mxx, double myx, double mxy, double myy, double mxt, double myt,
        Rectangle clip
    ) {}

    /**
     * Constructs a new instance.
     * <p>
     * The provided image must be backed by a writable {@link IntBuffer} with
     * pixel format {@link PixelFormat#INT_ARGB_PRE INT_ARGB_PRE}. The {@code IntBuffer}
     * must be either heap array-backed or a direct buffer, and may be a slice of a larger buffer.
     *
     * @param img a prism image, cannot be {@code null}
     * @param pixelsDirty a consumer of dirty rectangles, cannot be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if the image is not backed by a writable buffer
     *     in a format supported by the software renderer
     */
    public SWDrawingContext(com.sun.prism.Image img, Consumer<Rectangle> pixelsDirty) {
        this.pixelsDirty = Objects.requireNonNull(pixelsDirty, "pixelsDirty");
        this.imageWidth = img.getWidth();  // implicit null check for img
        this.imageHeight = img.getHeight();
        this.resourceFactory = new SWResourceFactory(null);

        SWRTTexture texture = createTexture(resourceFactory, img);

        this.graphics = (SWGraphics)texture.createGraphics();

        FXCleaner.register(this, new StateCleaner(resourceFactory, texture));
    }

    private static SWRTTexture createTexture(SWResourceFactory factory, com.sun.prism.Image img) {
        int width = img.getWidth();
        int height = img.getHeight();

        IntBuffer buffer = switch (img.getPixelBuffer()) {
            case IntBuffer b -> b;
            default -> throw new IllegalStateException("image buffer is not in INT_ARGB_PRE format: " + img.getPixelFormat());
        };

        if (buffer.isReadOnly()) {
            throw new IllegalStateException("image buffer must be writable");
        }

        long size = (long) width * height;

        if (size > buffer.capacity() || (buffer.hasArray() && buffer.arrayOffset() + size > buffer.array().length)) {
            throw new IllegalStateException("image buffer has insufficient capacity to hold " + width + "x" + height + " pixels");
        }

        if (buffer.hasArray()) {
            return new SWRTTexture(factory, width, height, buffer.array(), buffer.arrayOffset());
        }

        if (buffer.isDirect()) {
            return new SWRTTexture(factory, width, height, buffer);
        }

        throw new IllegalStateException("image buffer is neither heap array-backed nor direct");
    }

    private record StateCleaner(SWResourceFactory resourceFactory, SWRTTexture texture) implements Runnable {
        @Override
        public void run() {
            texture.dispose();
            resourceFactory.dispose();
        }
    }

    @Override
    public Paint getStroke() {
        return stroke;
    }

    @Override
    public void setStroke(Paint p) {
        if (p != null) {
            this.stroke = p;
            this.prismStrokePaint = (com.sun.prism.paint.Paint)Toolkit.getToolkit().getPaint(p);
        }
    }

    @Override
    public Paint getFill() {
        return fill;
    }

    @Override
    public void setFill(Paint p) {
        if (p != null) {
            this.fill = p;
            this.prismFillPaint = (com.sun.prism.paint.Paint)Toolkit.getToolkit().getPaint(p);
        }
    }

    @Override
    public double getGlobalAlpha() {
        return globalAlpha;
    }

    @Override
    public void setGlobalAlpha(double alpha) {
        this.globalAlpha = alpha;

        graphics.setExtraAlpha((float) Math.clamp(alpha, 0.0, 1.0));
    }

    @Override
    public BlendMode getGlobalBlendMode() {
        return globalBlendMode;
    }

    @Override
    public void setGlobalBlendMode(BlendMode op) {
        if (op != null) {
            CompositeMode cm = switch (op) {
                case SRC_OVER -> CompositeMode.SRC_OVER;
                default -> throw new UnsupportedOperationException("Unsupported blend mode: " + op);
            };

            this.globalBlendMode = op;

            graphics.setCompositeMode(cm);
        }
    }

    @Override
    public FillRule getFillRule() {
        return fillRule;
    }

    @Override
    public void setFillRule(FillRule fillRule) {
        if(fillRule != null) {
            this.fillRule = fillRule;
        }
    }

    @Override
    public double getLineWidth() {
        return lineWidth;
    }

    @Override
    public void setLineWidth(double lw) {
        if(lw > 0 && lw < Double.POSITIVE_INFINITY && lw != lineWidth) {
            this.lineWidth = lw;

            invalidateStroke();
        }
    }

    @Override
    public StrokeLineCap getLineCap() {
        return lineCap;
    }

    @Override
    public void setLineCap(StrokeLineCap cap) {
        if(cap != null && cap != lineCap) {
            this.lineCap = cap;

            invalidateStroke();
        }
    }

    @Override
    public StrokeLineJoin getLineJoin() {
        return lineJoin;
    }

    @Override
    public void setLineJoin(StrokeLineJoin join) {
        if (join != null && join != lineJoin) {
            this.lineJoin = join;

            invalidateStroke();
        }
    }

    @Override
    public double getMiterLimit() {
        return miterLimit;
    }

    @Override
    public void setMiterLimit(double ml) {
        if (ml > 0.0 && ml < Double.POSITIVE_INFINITY && ml != miterLimit) {
            this.miterLimit = ml;

            invalidateStroke();
        }
    }

    @Override
    public void setLineDashes(double... dashes) {
        double[] newDashes = null;

        if (dashes != null && dashes.length > 0) {
            boolean allZeros = true;

            for (double d : dashes) {
                if (d >= 0.0 && d < Double.POSITIVE_INFINITY) {
                    // Non-NaN, finite, non-negative
                    // Test cannot be inverted or it will not implicitly test for NaN
                    if (d > 0) {
                        allZeros = false;
                    }
                }
                else {
                    return;  // invalid dash found, ignore entire call
                }
            }

            if (!allZeros) {
                int dashlen = dashes.length;

                if ((dashlen & 1) == 0) {
                    newDashes = Arrays.copyOf(dashes, dashlen);
                }
                else {
                    newDashes = Arrays.copyOf(dashes, dashlen * 2);

                    System.arraycopy(dashes, 0, newDashes, dashlen, dashlen);
                }
            }
        }

        if (!Arrays.equals(this.lineDashes, newDashes)) {
            this.lineDashes = newDashes;

            invalidateStroke();
        }
    }

    @Override
    public double[] getLineDashes() {
        return lineDashes == null ? null : Arrays.copyOf(lineDashes, lineDashes.length);
    }

    @Override
    public void setLineDashOffset(double dashOffset) {
        // Per W3C spec: On setting, infinite, and NaN
        // values must be ignored, leaving the value unchanged
        if (dashOffset > Double.NEGATIVE_INFINITY && dashOffset < Double.POSITIVE_INFINITY) {
            if (dashOffset != this.lineDashOffset) {
                this.lineDashOffset = dashOffset;

                invalidateStroke();
            }
        }
    }

    @Override
    public double getLineDashOffset() {
        return lineDashOffset;
    }

    @Override
    public boolean isImageSmoothing() {
        return imageSmoothing;
    }

    @Override
    public void setImageSmoothing(boolean imageSmoothing) {
        this.imageSmoothing = imageSmoothing;
    }

    @Override
    public Affine getTransform() {
        return getTransform(null);
    }

    @Override
    public Affine getTransform(Affine xform) {
        if (xform == null) {
            xform = new Affine();
        }

        xform.setMxx(transform.getMxx());
        xform.setMxy(transform.getMxy());
        xform.setMxz(0);
        xform.setTx(transform.getMxt());
        xform.setMyx(transform.getMyx());
        xform.setMyy(transform.getMyy());
        xform.setMyz(0);
        xform.setTy(transform.getMyt());
        xform.setMzx(0);
        xform.setMzy(0);
        xform.setMzz(1);
        xform.setTz(0);

        return xform;
    }

    @Override
    public void setTransform(
        double mxx, double myx,
        double mxy, double myy,
        double mxt, double myt
    ) {
        transform.setTransform(mxx, myx, mxy, myy, mxt, myt);
        graphics.setTransform(mxx, myx, mxy, myy, mxt, myt);
    }

    @Override
    public void setTransform(Affine xform) {
        if (xform == null) {
            return;
        }

        setTransform(
            xform.getMxx(), xform.getMyx(),
            xform.getMxy(), xform.getMyy(),
            xform.getTx(), xform.getTy()
        );
    }

    @Override
    public void translate(double x, double y) {
        Affine2D t = new Affine2D(transform);

        t.translate(x, y);
        applyTransform(t);
    }

    @Override
    public void scale(double x, double y) {
        Affine2D t = new Affine2D(transform);

        t.scale(x, y);
        applyTransform(t);
    }

    @Override
    public void rotate(double degrees) {
        Affine2D t = new Affine2D(transform);

        t.rotate(Math.toRadians(degrees));
        applyTransform(t);
    }

    @Override
    public void transform(
        double mxx, double myx,
        double mxy, double myy,
        double mxt, double myt
    ) {
        Affine2D t = new Affine2D(transform);

        t.concatenate(mxx, mxy, mxt, myx, myy, myt);
        applyTransform(t);
    }

    @Override
    public void transform(Affine xform) {
        if (xform == null) {
            return;
        }

        Affine2D t = new Affine2D(transform);

        t.concatenate(
            xform.getMxx(), xform.getMxy(), xform.getTx(),
            xform.getMyx(), xform.getMyy(), xform.getTy()
        );
        applyTransform(t);
    }

    private void applyTransform(BaseTransform result) {
        setTransform(
            result.getMxx(), result.getMyx(),
            result.getMxy(), result.getMyy(),
            result.getMxt(), result.getMyt()
        );
    }

    @Override
    public void clipRect(double x, double y, double w, double h) {
        if (transform.getMxy() != 0 || transform.getMyx() != 0) {
            throw new UnsupportedOperationException("clipping is not supported under a transform with rotation or shear");
        }

        /*
         * Immediately transform clip to device space:
         */

        Rectangle r = transformRect(x, y, x + w, y + h);

        if (r == null) {
            clip = new Rectangle(0, 0, 0, 0);  // nothing of the rect is inside the image
        }
        else if (clip == null) {
            clip = r;
        }
        else {
            clip.intersectWith(r);
        }

        applyClip();
    }

    private void applyClip() {
        if (clip == null) {
            graphics.setClipRect(null);
        }
        else {
            graphics.setClipRect(clip);
        }
    }

    private Rectangle transformRect(double x1, double y1, double x2, double y2) {
        double[] src = {x1, y1, x2, y1, x1, y2, x2, y2};
        double[] dst = new double[8];

        transform.transform(src, 0, dst, 0, 4);

        double minX = Math.max(0, Math.floor(Math.min(Math.min(dst[0], dst[2]), Math.min(dst[4], dst[6]))));
        double minY = Math.max(0, Math.floor(Math.min(Math.min(dst[1], dst[3]), Math.min(dst[5], dst[7]))));
        double maxX = Math.min(imageWidth, Math.ceil(Math.max(Math.max(dst[0], dst[2]), Math.max(dst[4], dst[6]))));
        double maxY = Math.min(imageHeight, Math.ceil(Math.max(Math.max(dst[1], dst[3]), Math.max(dst[5], dst[7]))));

        if (maxX <= minX || maxY <= minY) {
            return null;
        }

        return new Rectangle((int) minX, (int) minY, (int) (maxX - minX), (int) (maxY - minY));
    }

    @Override
    public void save() {
        stateStack.push(new State(
            globalAlpha, globalBlendMode, fill, stroke, lineWidth, lineCap, lineJoin,
            miterLimit,
            lineDashes != null ? lineDashes.clone() : null, lineDashOffset,
            fillRule, imageSmoothing,
            font, textAlign, textBaseline, fontSmoothingType,
            transform.getMxx(), transform.getMyx(), transform.getMxy(), transform.getMyy(),
            transform.getMxt(), transform.getMyt(),
            clip != null ? new Rectangle(clip) : null
        ));
    }

    @Override
    public void restore() {
        if (stateStack.isEmpty()) {
            return;
        }

        State s = stateStack.pop();

        clip = s.clip() != null ? new Rectangle(s.clip()) : null;

        setTransform(s.mxx(), s.myx(), s.mxy(), s.myy(), s.mxt(), s.myt());
        applyClip();
        setGlobalAlpha(s.globalAlpha());
        setGlobalBlendMode(s.globalBlendMode());
        setFill(s.fill());
        setStroke(s.stroke());
        setLineWidth(s.lineWidth());
        setLineCap(s.lineCap());
        setLineJoin(s.lineJoin());
        setMiterLimit(s.miterLimit());
        setLineDashes(s.lineDashes());
        setLineDashOffset(s.lineDashOffset());
        setFont(s.font());
        setTextAlign(s.textAlign());
        setTextBaseline(s.textBaseline());
        setFontSmoothingType(s.fontSmoothingType());
        setFillRule(s.fillRule());
        setImageSmoothing(s.imageSmoothing());
    }

    @Override
    public void strokeLine(double x1, double y1, double x2, double y2) {
        applyStrokeParameters();

        graphics.resetPaintBounds();
        graphics.drawLine((float)x1, (float)y1, (float)x2, (float)y2);

        reportGraphicsPaintBounds();
    }

    @Override
    public void strokeRect(double x, double y, double w, double h) {
        if (w != 0 || h != 0) {
            applyStrokeParameters();

            graphics.resetPaintBounds();
            graphics.drawRect((float)x, (float)y, (float)w, (float)h);

            reportGraphicsPaintBounds();
        }
    }

    @Override
    public void clearRect(double x, double y, double w, double h) {
        if (w != 0 && h != 0) {
            graphics.resetPaintBounds();
            graphics.clearQuad((float)x, (float)y, (float)(x + w), (float)(y + h));

            reportGraphicsPaintBounds();
        }
    }

    @Override
    public void fillRect(double x, double y, double w, double h) {
        if (w != 0 && h != 0) {
            graphics.setPaint(prismFillPaint);
            graphics.resetPaintBounds();
            graphics.fillRect((float)x, (float)y, (float)w, (float)h);

            reportGraphicsPaintBounds();
        }
    }

    @Override
    public void strokeRoundRect(double x, double y, double w, double h, double arcWidth, double arcHeight) {
        if (w != 0 || h != 0) {
            applyStrokeParameters();

            graphics.resetPaintBounds();
            graphics.drawRoundRect((float)x, (float)y, (float)w, (float)h, (float)arcWidth, (float)arcHeight);

            reportGraphicsPaintBounds();
        }
    }

    @Override
    public void fillRoundRect(double x, double y, double w, double h, double arcWidth, double arcHeight) {
        if (w != 0 && h != 0) {
            graphics.setPaint(prismFillPaint);
            graphics.resetPaintBounds();
            graphics.fillRoundRect((float)x, (float)y, (float)w, (float)h, (float)arcWidth, (float)arcHeight);

            reportGraphicsPaintBounds();
        }
    }

    @Override
    public void strokeOval(double x, double y, double w, double h) {
        if (w != 0 || h != 0) {
            applyStrokeParameters();

            graphics.resetPaintBounds();
            graphics.drawEllipse((float)x, (float)y, (float)w, (float)h);

            reportGraphicsPaintBounds();
        }
    }

    @Override
    public void fillOval(double x, double y, double w, double h) {
        if (w != 0 && h != 0) {
            applyStrokeParameters();

            graphics.setPaint(prismFillPaint);
            graphics.resetPaintBounds();
            graphics.fillEllipse((float)x, (float)y, (float)w, (float)h);

            reportGraphicsPaintBounds();
        }
    }

    @Override
    public void strokeArc(double x, double y, double w, double h, double startAngle, double arcExtent, ArcType closure) {
        if (w != 0 && h != 0 && closure != null) {
            int arcType = switch (closure) {
                case CHORD -> Arc2D.CHORD;
                case OPEN -> Arc2D.OPEN;
                case ROUND -> Arc2D.PIE;
            };

            applyStrokeParameters();

            graphics.resetPaintBounds();
            graphics.draw(new Arc2D((float)x, (float)y, (float)w, (float)h, (float)startAngle, (float)arcExtent, arcType));

            reportGraphicsPaintBounds();
        }
    }

    @Override
    public void fillArc(double x, double y, double w, double h, double startAngle, double arcExtent, ArcType closure) {
        if (w != 0 && h != 0 && closure != null) {
            int arcType = switch (closure) {
                case CHORD -> Arc2D.CHORD;
                case OPEN -> Arc2D.OPEN;
                case ROUND -> Arc2D.PIE;
            };

            graphics.setPaint(prismFillPaint);
            graphics.resetPaintBounds();
            graphics.fill(new Arc2D((float)x, (float)y, (float)w, (float)h, (float)startAngle, (float)arcExtent, arcType));

            reportGraphicsPaintBounds();
        }
    }

    @Override
    public void strokePolyline(double[] xPoints, double[] yPoints, int nPoints) {
        strokePolyline(xPoints, yPoints, nPoints, false);
    }

    @Override
    public void strokePolygon(double[] xPoints, double[] yPoints, int nPoints) {
        strokePolyline(xPoints, yPoints, nPoints, true);
    }

    private void strokePolyline(double[] xPoints, double[] yPoints, int nPoints, boolean close) {
        if (xPoints != null && yPoints != null && nPoints >= 2 && xPoints.length >= nPoints && yPoints.length >= nPoints) {
            Path2D path = new Path2D();

            path.moveTo((float)xPoints[0], (float)yPoints[0]);

            for (int i = 1; i < nPoints; i++) {
                path.lineTo((float)xPoints[i], (float)yPoints[i]);
            }

            if (close) {
                path.closePath();
            }

            applyStrokeParameters();

            graphics.resetPaintBounds();
            graphics.draw(path);

            reportGraphicsPaintBounds();
        }
    }

    @Override
    public void fillPolygon(double[] xPoints, double[] yPoints, int nPoints) {
        if (xPoints != null && yPoints != null && nPoints >= 3 && xPoints.length >= nPoints && yPoints.length >= nPoints) {
            Path2D path = new Path2D(switch (fillRule) {
                case EVEN_ODD -> Path2D.WIND_EVEN_ODD;
                case NON_ZERO -> Path2D.WIND_NON_ZERO;
            });

            path.moveTo((float)xPoints[0], (float)yPoints[0]);

            for (int i = 1; i < nPoints; i++) {
                path.lineTo((float)xPoints[i], (float)yPoints[i]);
            }

            path.closePath();

            graphics.setPaint(prismFillPaint);
            graphics.resetPaintBounds();
            graphics.fill(path);

            reportGraphicsPaintBounds();
        }
    }

    @Override
    public void beginPath() {
        path.reset();
    }

    @Override
    public void moveTo(double x0, double y0) {
        coords[0] = (float) x0;
        coords[1] = (float) y0;

        transform.transform(coords, 0, coords, 0, 1);

        path.moveTo(coords[0], coords[1]);
    }

    @Override
    public void lineTo(double x1, double y1) {
        coords[0] = (float) x1;
        coords[1] = (float) y1;

        transform.transform(coords, 0, coords, 0, 1);

        if (path.getNumCommands() == 0) {
            path.moveTo(coords[0], coords[1]);
        }

        path.lineTo(coords[0], coords[1]);
    }

    @Override
    public void quadraticCurveTo(double xc, double yc, double x1, double y1) {
        coords[0] = (float) xc;
        coords[1] = (float) yc;
        coords[2] = (float) x1;
        coords[3] = (float) y1;

        transform.transform(coords, 0, coords, 0, 2);

        if (path.getNumCommands() == 0) {
            path.moveTo(coords[0], coords[1]);
        }

        path.quadTo(coords[0], coords[1], coords[2], coords[3]);
    }

    @Override
    public void bezierCurveTo(double xc1, double yc1, double xc2, double yc2, double x1, double y1) {
        coords[0] = (float) xc1;
        coords[1] = (float) yc1;
        coords[2] = (float) xc2;
        coords[3] = (float) yc2;
        coords[4] = (float) x1;
        coords[5] = (float) y1;

        transform.transform(coords, 0, coords, 0, 3);

        if (path.getNumCommands() == 0) {
            path.moveTo(coords[0], coords[1]);
        }

        path.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
    }

    @Override
    public void arcTo(double x1, double y1, double x2, double y2, double radius) {
        if (path.getNumCommands() == 0) {
            moveTo(x1, y1);
            lineTo(x1, y1);
        }
        else {
            try {
                path.arcTo(transform, (float) x1, (float) y1, (float) x2, (float) y2, (float) radius);
            }
            catch (IllegalPathStateException | NoninvertibleTransformException e) {
                lineTo(x1, y1);
            }
        }
    }

    @Override
    public void arc(double centerX, double centerY, double radiusX, double radiusY, double startAngle, double length) {
        Arc2D arc = new Arc2D(
            (float) (centerX - radiusX), // x
            (float) (centerY - radiusY), // y
            (float) (radiusX * 2.0), // w
            (float) (radiusY * 2.0), // h
            (float) startAngle,
            (float) length,
            Arc2D.OPEN
        );

        path.append(arc.getPathIterator(transform), true);
    }

    @Override
    public void rect(double x, double y, double w, double h) {
        coords[0] = (float) x;
        coords[1] = (float) y;
        coords[2] = (float) w;
        coords[3] = 0;
        coords[4] = 0;
        coords[5] = (float) h;

        transform.deltaTransform(coords, 0, coords, 0, 3);

        float x0 = coords[0] + (float) transform.getMxt();
        float y0 = coords[1] + (float) transform.getMyt();
        float dx1 = coords[2];
        float dy1 = coords[3];
        float dx2 = coords[4];
        float dy2 = coords[5];

        path.moveTo(x0, y0);
        path.lineTo(x0 + dx1, y0 + dy1);
        path.lineTo(x0 + dx1 + dx2, y0 + dy1 + dy2);
        path.lineTo(x0 + dx2, y0 + dy2);
        path.closePath();
    }

    @Override
    public void appendSVGPath(String svgpath) {
        if (svgpath == null) return;

        try {
            path.appendSVGPath(transform, svgpath);
        }
        catch (IllegalArgumentException | IllegalPathStateException | NoninvertibleTransformException ex) {
            //Ignore incorrect path
        }
    }

    @Override
    public void closePath() {
        if (path.getNumCommands() > 0) {
            path.closePath();
        }
    }

    @Override
    public void fill() {
        if (path.getNumCommands() == 0) {
            return;
        }

        path.setWindingRule(fillRule == FillRule.EVEN_ODD ? Path2D.WIND_EVEN_ODD : Path2D.WIND_NON_ZERO);

        graphics.setTransform(BaseTransform.IDENTITY_TRANSFORM);
        graphics.setPaint(prismFillPaint);
        graphics.resetPaintBounds();
        graphics.fill(path);
        graphics.setTransform(transform);

        reportGraphicsPaintBounds();
    }

    @Override
    public void stroke() {
        if (path.getNumCommands() == 0) {
            return;
        }

        applyStrokeParameters();

        /*
         * The path coordinates were transformed into device space as they
         * were added, and must be drawn with an identity transform. The stroke
         * widths and dash lengths must still be adjusted if the scale != 1.0
         */

        double scale = Math.sqrt(transform.getMxx() * transform.getMxx() + transform.getMyx() * transform.getMyx());

        if (scale != 1.0) {
            graphics.setStroke(buildStroke(scale));
        }

        graphics.setTransform(BaseTransform.IDENTITY_TRANSFORM);
        graphics.resetPaintBounds();
        graphics.draw(path);
        graphics.setTransform(transform);  // restore transform

        if (scale != 1.0) {
            applyStrokeParameters();  // restore stroke just in case
        }

        reportGraphicsPaintBounds();
    }

    @Override
    public void clip() {
        throw new UnsupportedOperationException("path based clipping is not supported");
    }

    @Override
    public boolean isPointInPath(double x, double y) {

        /*
         * Tests the point against the region the current path would fill. The
         * path is treated as an infinitely thin boundary, so the stroke
         * attributes do not matter. A point lying exactly on the boundary is
         * reported per the fill's half-open rasterization convention (the low
         * edges and corner count as inside, the high ones as outside), rather
         * than treated as inside the way HTML5 does.
         */

        return path.contains((float) x, (float) y);
    }

    @Override
    public Font getFont() {
        return font;
    }

    @Override
    public void setFont(Font f) {
        if (f != null) {
            font = f;
        }
    }

    @Override
    public TextAlignment getTextAlign() {
        return textAlign;
    }

    @Override
    public void setTextAlign(TextAlignment align) {
        if (align != null) {
            textAlign = align;
        }
    }

    @Override
    public VPos getTextBaseline() {
        return textBaseline;
    }

    @Override
    public void setTextBaseline(VPos baseline) {
        if (baseline != null) {
            textBaseline = baseline;
        }
    }

    @Override
    public void setFontSmoothingType(FontSmoothingType fontsmoothing) {
        if (fontsmoothing != null) {
            fontSmoothingType = fontsmoothing;
        }
    }

    @Override
    public FontSmoothingType getFontSmoothingType() {
        return fontSmoothingType;
    }

    @Override
    public void fillText(String text, double x, double y) {
        drawText(text, x, y, 0, false);
    }

    @Override
    public void fillText(String text, double x, double y, double maxWidth) {
        drawText(text, x, y, maxWidth, false);
    }

    @Override
    public void strokeText(String text, double x, double y) {
        drawText(text, x, y, 0, true);
    }

    @Override
    public void strokeText(String text, double x, double y, double maxWidth) {
        drawText(text, x, y, maxWidth, true);
    }

    private void drawText(String text, double x, double y, double maxWidth, boolean stroke) {
        if (text == null || text.isEmpty()) {
            return;
        }

        TextLayout layout = PrismTextLayoutFactory.getFactory().createLayout();

        layout.setContent(text, FontHelper.getNativeFont(font));
        layout.setAlignment(textAlign.ordinal());
        layout.setDirection(TextLayout.DIRECTION_LTR);

        BaseBounds bounds = layout.getBounds();
        float layoutWidth = bounds.getWidth();
        float layoutHeight = bounds.getHeight();

        float xAlign = switch (textAlign) {
            case RIGHT -> layoutWidth;
            case CENTER -> layoutWidth / 2;
            default -> 0;
        };

        float yAlign = switch (textBaseline) {
            case BASELINE -> -bounds.getMinY();
            case CENTER -> layoutHeight / 2;
            case BOTTOM -> layoutHeight;
            default -> 0;
        };

        float scaleX = 1;
        float layoutX = (float) (x - xAlign);

        if (maxWidth > 0 && layoutWidth > maxWidth) {
            scaleX = (float) (maxWidth / layoutWidth);
            layoutX = (float) (x / scaleX - xAlign);
        }

        float layoutY = (float) (y - yAlign);

        Affine2D textTx = new Affine2D();

        textTx.setTransform(
            transform.getMxx() * scaleX, transform.getMyx() * scaleX,
            transform.getMxy(), transform.getMyy(),
            transform.getMxt(), transform.getMyt()
        );

        graphics.setTransform(textTx);

        PGFont pgFont = (PGFont) FontHelper.getNativeFont(font);
        int smoothing = fontSmoothingType == FontSmoothingType.LCD ? FontResource.AA_LCD : FontResource.AA_GREYSCALE;
        FontStrike strike = pgFont.getStrike(textTx, smoothing);

        if (stroke) {
            applyStrokeParameters();
        }
        else {
            graphics.setPaint(prismFillPaint);
        }

        GlyphList[] runs = layout.getRuns();

        graphics.resetPaintBounds();

        for (GlyphList run : runs) {
            if (run.getGlyphCount() == 0) {
                continue;
            }

            Point2D pt = run.getLocation();
            RectBounds lineBounds = run.getLineBounds();
            float runX = pt.x + layoutX;
            float runY = pt.y + layoutY - lineBounds.getMinY();

            if (stroke) {
                graphics.draw(strike.getOutline(run, BaseTransform.getTranslateInstance(runX, runY)));
            }
            else {
                graphics.drawString(run, strike, runX, runY, null, 0, 0);
            }
        }

        graphics.setTransform(transform);

        reportGraphicsPaintBounds();
    }

    @Override
    public void drawImage(Image img, double sx, double sy, double sw, double sh, double dx, double dy, double dw, double dh) {
        if (img == null || img.getProgress() < 1.0) {
            return;
        }

        Object platformImage = Toolkit.getImageAccessor().getPlatformImage(img);

        // Ensure it's a Prism image
        if (!(platformImage instanceof com.sun.prism.Image prismImage)) {
            throw new IllegalArgumentException("PlatformImage must be a Prism Image");
        }

        // Create a texture from the Prism image
        Texture tex = resourceFactory.createTexture(prismImage, Usage.DEFAULT, Texture.WrapMode.CLAMP_TO_EDGE);

        if (tex == null) {
            throw new IllegalStateException("Unable to draw image, insufficient resources");
        }

        try {
            tex.setLinearFiltering(imageSmoothing);

            graphics.resetPaintBounds();
            graphics.drawTexture(tex, (float)dx, (float)dy, (float)(dx + dw), (float)(dy + dh), (float)sx, (float)sy, (float)(sx + sw), (float)(sy + sh));

            reportGraphicsPaintBounds();
        }
        finally {
            tex.dispose();
        }
    }

    private void reportGraphicsPaintBounds() {
        Rectangle painted = graphics.takePaintBounds();

        if (painted != null) {
            markDeviceRectDirty(painted.x, painted.y, painted.x + painted.width, painted.y + painted.height);
        }
    }

    private void applyStrokeParameters() {
        if (prismStroke == null) {
            this.prismStroke = buildStroke(1.0);
        }

        graphics.setStroke(prismStroke);
        graphics.setPaint(prismStrokePaint);
    }

    private BasicStroke buildStroke(double widthScale) {
        return new BasicStroke(
            (float)(lineWidth * widthScale),
            switch (lineCap) {
                case BUTT -> BasicStroke.CAP_BUTT;
                case ROUND -> BasicStroke.CAP_ROUND;
                case SQUARE -> BasicStroke.CAP_SQUARE;
            },
            switch (lineJoin) {
                case BEVEL -> BasicStroke.JOIN_BEVEL;
                case ROUND -> BasicStroke.JOIN_ROUND;
                case MITER -> BasicStroke.JOIN_MITER;
            },
            (float)miterLimit,
            scaleDashes(widthScale),
            (float)(lineDashOffset * widthScale)
        );
    }

    private double[] scaleDashes(double widthScale) {
        if (lineDashes == null) {
            return null;
        }

        double[] scaled = new double[lineDashes.length];

        for (int i = 0; i < lineDashes.length; i++) {
            scaled[i] = lineDashes[i] * widthScale;
        }

        return scaled;
    }

    private void invalidateStroke() {
        this.prismStroke = null;
    }

    // TODO It seems bufferDirty only remembers the last rect; may need to update this only once per frame
    // Note: if called multiple times per frame, then it just updates everything (optimize?)

    /*
     * Reports a dirty rectangle given in device coordinates (already
     * transformed), clipped to the image bounds.
     */
    private void markDeviceRectDirty(double x1, double y1, double x2, double y2) {
        double minX = Math.max(0, Math.floor(Math.min(x1, x2)));
        double minY = Math.max(0, Math.floor(Math.min(y1, y2)));
        double maxX = Math.min(imageWidth, Math.ceil(Math.max(x1, x2)));
        double maxY = Math.min(imageHeight, Math.ceil(Math.max(y1, y2)));

        if (maxX <= minX || maxY <= minY) {
            return;
        }

        pixelsDirty.accept(new Rectangle((int) minX, (int) minY, (int) (maxX - minX), (int) (maxY - minY)));
    }
}
