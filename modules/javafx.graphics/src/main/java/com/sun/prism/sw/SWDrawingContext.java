/*
 * Copyright (c) 2012, 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.ui.Screen;
import com.sun.glass.utils.NativeLibLoader;
import com.sun.javafx.geom.Arc2D;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.util.FXCleaner;
import com.sun.prism.BasicStroke;
import com.sun.prism.CompositeMode;
import com.sun.prism.Graphics;
import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import com.sun.prism.Texture.Usage;

import java.nio.IntBuffer;
import java.util.Objects;
import java.util.function.Consumer;

import javafx.scene.effect.BlendMode;
import javafx.scene.image.DrawingContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelBuffer;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

// TODO dashes
// TODO save/restore
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
 *   <li>Stroke and fill management (line width, line caps, joins, miter limits).</li>
 *   <li>Global alpha and support for {@link javafx.scene.effect.BlendMode#SRC_OVER} and
 *       {@link javafx.scene.effect.BlendMode#ADD}.</li>
 *    <li>Drawing of lines, rectangles, rounded rectangles, ovals, arcs, polygons, polylines, and images.</li>
 *    <li>Automatic dirty-region tracking for efficient pixel updates.</li>
 * </ul>
 * <p>
 * Limitations:
 * <ul>
 *   <li>Dashed strokes and save/restore state are not yet implemented.</li> TODO
 *   <li>Only SRC_OVER and ADD blend modes are supported; others will throw an exception.</li>
 * </ul>
  * <p>
 * This class is intended for use in contexts where direct drawing into a Prism image is needed, such as the
 * {@code getDrawingContext()} method in WritableImage, providing a more convenient API than PixelWriter or snapshotting
 * a Canvas.
 *
 * @see DrawingContext
 * @see com.sun.prism.Image
 * @since 26
 */
public class SWDrawingContext implements DrawingContext {
    private static final double SQRT2 = Math.sqrt(2);

    static {
        NativeLibLoader.loadLibrary("prism_sw");
    }

    private final Graphics graphics;
    private final SWResourceFactory resourceFactory;
    private final Consumer<Rectangle> pixelsDirty;

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

    // Path attributes
    private FillRule fillRule = FillRule.NON_ZERO;

    // Image attributes
    private boolean imageSmoothing = true;

    // Cached prism values
    private com.sun.prism.paint.Paint prismFillPaint = com.sun.prism.paint.Color.BLACK;
    private com.sun.prism.paint.Paint prismStrokePaint = com.sun.prism.paint.Color.BLACK;
    private BasicStroke prismStroke;

    /**
     * Constructs a new instance.
     * <p>
     * The provided image must be backed by a writable {@link PixelBuffer} with
     * format {@link PixelFormat#INT_ARGB_PRE INT_ARGB_PRE}.
     *
     * @param img a prism image, cannot be {@code null}
     * @param pixelsDirty a consumer of dirty rectangles, cannot be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if the image is not backed by a writable pixel buffer
     *     in the correct format.
     */
    public SWDrawingContext(com.sun.prism.Image img, Consumer<Rectangle> pixelsDirty) {
        int[] data = switch (img.getPixelBuffer()) {
            case IntBuffer ib -> ib.array();
            default -> throw new IllegalStateException("img must contain an accessible int buffer backed by an int array");
        };

        this.pixelsDirty = Objects.requireNonNull(pixelsDirty, "pixelsDirty");
        this.resourceFactory = new SWResourceFactory(Screen.getMainScreen()); // Note, actual screen is irrelevant, we just need one

        SWRTTexture texture = new SWRTTexture(resourceFactory, img.getWidth(), img.getHeight(), data);

        this.graphics = texture.createGraphics();

        FXCleaner.register(this, new StateCleaner(resourceFactory, texture));
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
        this.globalAlpha = Math.clamp(alpha, 0.0, 1.0);

        graphics.setExtraAlpha((float) globalAlpha);
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
                case ADD -> CompositeMode.ADD;
                default -> throw new IllegalArgumentException("Unsupported blend mode: " + op);
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
    public boolean isImageSmoothing() {
        return imageSmoothing;
    }

    @Override
    public void setImageSmoothing(boolean imageSmoothing) {
        this.imageSmoothing = imageSmoothing;
    }

    @Override
    public void strokeLine(double x1, double y1, double x2, double y2) {
        applyStrokeParameters();

        graphics.drawLine((float)x1, (float)y1, (float)x2, (float)y2);

        markStrokeRectDirty(x1, y1, Math.abs(x2 - x1), Math.abs(y2 - y1));
    }

    @Override
    public void strokeRect(double x, double y, double w, double h) {
        if(w != 0 || h != 0) {
            applyStrokeParameters();

            graphics.drawRect((float)x, (float)y, (float)w, (float)h);

            markStrokeRectDirty(x, y, w, h);
        }
    }

    @Override
    public void clearRect(double x, double y, double w, double h) {
        if (w != 0 && h != 0) {
            graphics.clearQuad((float)x, (float)y, (float)(x + w), (float)(x + h));

            markRectDirty(x, y, w, h);
        }
    }

    @Override
    public void fillRect(double x, double y, double w, double h) {
        if (w != 0 && h != 0) {
            graphics.setPaint(prismFillPaint);
            graphics.fillRect((float)x, (float)y, (float)w, (float)h);

            markRectDirty(x, y, w, h);
        }
    }

    @Override
    public void strokeRoundRect(double x, double y, double w, double h, double arcWidth, double arcHeight) {
        if (w != 0 || h != 0) {
            applyStrokeParameters();

            graphics.drawRoundRect((float)x, (float)y, (float)w, (float)h, (float)arcWidth, (float)arcHeight);

            markStrokeRectDirty(x, y, w, h);
        }
    }

    @Override
    public void fillRoundRect(double x, double y, double w, double h, double arcWidth, double arcHeight) {
        if (w != 0 && h != 0) {
            graphics.setPaint(prismFillPaint);
            graphics.fillRoundRect((float)x, (float)y, (float)w, (float)h, (float)arcWidth, (float)arcHeight);

            markRectDirty(x, y, w, h);
        }
    }

    @Override
    public void strokeOval(double x, double y, double w, double h) {
        if (w != 0 || h != 0) {
            applyStrokeParameters();

            graphics.drawEllipse((float)x, (float)y, (float)w, (float)h);

            markStrokeRectDirty(x, y, w, h);
        }
    }

    @Override
    public void fillOval(double x, double y, double w, double h) {
        if (w != 0 && h != 0) {
            applyStrokeParameters();

            graphics.setPaint(prismFillPaint);
            graphics.fillEllipse((float)x, (float)y, (float)w, (float)h);

            markRectDirty(x, y, w, h);
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

            graphics.draw(new Arc2D((float)x, (float)y, (float)w, (float)h, (float)startAngle, (float)arcExtent, arcType));

            markStrokeRectDirty(x, y, w, h);
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
            graphics.fill(new Arc2D((float)x, (float)y, (float)w, (float)h, (float)startAngle, (float)arcExtent, arcType));

            markRectDirty(x, y, w, h);
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
            double minX = xPoints[0];
            double maxX = xPoints[0];
            double minY = yPoints[0];
            double maxY = yPoints[0];

            path.moveTo((float)xPoints[0], (float)yPoints[0]);

            for (int i = 1; i < nPoints; i++) {
                path.lineTo((float)xPoints[i], (float)yPoints[i]);

                minX = Math.min(minX, xPoints[i]);
                minY = Math.min(minY, yPoints[i]);
                maxX = Math.max(maxX, xPoints[i]);
                maxY = Math.max(maxY, yPoints[i]);
            }

            if (close) {
                path.closePath();
            }

            applyStrokeParameters();

            graphics.draw(path);

            markStrokeRectDirty(minX, minY, maxX - minX, maxY - minY);
        }
    }

    @Override
    public void fillPolygon(double[] xPoints, double[] yPoints, int nPoints) {
        if (xPoints != null && yPoints != null && nPoints >= 3 && xPoints.length >= nPoints && yPoints.length >= nPoints) {
            Path2D path = new Path2D(switch (fillRule) {
                case EVEN_ODD -> Path2D.WIND_EVEN_ODD;
                case NON_ZERO -> Path2D.WIND_NON_ZERO;
            });
            double minX = xPoints[0];
            double maxX = xPoints[0];
            double minY = yPoints[0];
            double maxY = yPoints[0];

            path.moveTo((float)xPoints[0], (float)yPoints[0]);

            for (int i = 1; i < nPoints; i++) {
                path.lineTo((float)xPoints[i], (float)yPoints[i]);

                minX = Math.min(minX, xPoints[i]);
                minY = Math.min(minY, yPoints[i]);
                maxX = Math.max(maxX, xPoints[i]);
                maxY = Math.max(maxY, yPoints[i]);
            }

            path.closePath();

            graphics.setPaint(prismFillPaint);
            graphics.fill(path);

            markRectDirty(minX, minY, maxX - minX, maxY - minY);
        }
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

            graphics.drawTexture(tex, (float)dx, (float)dy, (float)(dx + dw), (float)(dy + dh), (float)sx, (float)sy, (float)(sx + sw), (float)(sy + sh));

            markRectDirty(dx, dy, dw, dh);
        }
        finally {
            tex.dispose();
        }
    }

    private void applyStrokeParameters() {
        if(prismStroke == null) {
            this.prismStroke = new BasicStroke(
                (float)lineWidth,
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
                (float)miterLimit
            );
        }

        graphics.setStroke(prismStroke);
        graphics.setPaint(prismStrokePaint);
    }

    private void invalidateStroke() {
        this.prismStroke = null;
    }

    private void markStrokeRectDirty(double x, double y, double w, double h) {
        // Base half-width expansion
        double halfWidth = lineWidth * 0.5;

        // Determine additional expansion factor based on caps and joins
        double expansionFactor = switch (lineJoin) {
            case MITER -> Math.max(miterLimit, lineCap == StrokeLineCap.SQUARE ? SQRT2 : 1.0);
            case BEVEL, ROUND -> lineCap == StrokeLineCap.SQUARE ? SQRT2 : 1.0;
        };

        // Total expansion radius
        double r = halfWidth * expansionFactor;

        // Expand the rectangle
        double dirtyX = x - r;
        double dirtyY = y - r;
        double dirtyW = w + r * 2.0;
        double dirtyH = h + r * 2.0;

        markRectDirty(dirtyX, dirtyY, dirtyW, dirtyH);
    }

    // TODO It seems bufferDirty only remembers the last rect; may need to update this only once per frame
    // Note: if called multiple times per frame, then it just updates everything (optimize?)
    private void markRectDirty(double x, double y, double w, double h) {
        int fx = (int)Math.floor(x);
        int fy = (int)Math.floor(y);

        pixelsDirty.accept(new Rectangle(fx, fy, (int)Math.ceil(x + w) - fx, (int)Math.ceil(y + h) - fy));
    }
}
