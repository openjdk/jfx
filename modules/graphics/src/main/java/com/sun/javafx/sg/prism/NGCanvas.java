/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.sg.prism;

import javafx.geometry.VPos;
import javafx.scene.text.Font;
import java.nio.IntBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.LinkedList;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.geom.Arc2D;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.RoundRectangle2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import com.sun.javafx.scene.text.FontHelper;
import com.sun.javafx.text.PrismTextLayout;
import com.sun.javafx.tk.RenderJob;
import com.sun.javafx.tk.ScreenConfigurationAccessor;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.BasicStroke;
import com.sun.prism.CompositeMode;
import com.sun.prism.Graphics;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.Image;
import com.sun.prism.MaskTextureGraphics;
import com.sun.prism.PrinterGraphics;
import com.sun.prism.RTTexture;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Paint;
import com.sun.scenario.effect.Blend;
import com.sun.scenario.effect.Blend.Mode;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.Filterable;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.prism.PrDrawable;
import com.sun.scenario.effect.impl.prism.PrFilterContext;
import com.sun.scenario.effect.impl.prism.PrTexture;
import javafx.scene.text.FontSmoothingType;

/**
 */
public class NGCanvas extends NGNode {
    public static final byte                 ATTR_BASE = 0;
    public static final byte GLOBAL_ALPHA  = ATTR_BASE + 0;
    public static final byte COMP_MODE     = ATTR_BASE + 1;
    public static final byte FILL_PAINT    = ATTR_BASE + 2;
    public static final byte STROKE_PAINT  = ATTR_BASE + 3;
    public static final byte LINE_WIDTH    = ATTR_BASE + 4;
    public static final byte LINE_CAP      = ATTR_BASE + 5;
    public static final byte LINE_JOIN     = ATTR_BASE + 6;
    public static final byte MITER_LIMIT   = ATTR_BASE + 7;
    public static final byte FONT          = ATTR_BASE + 8;
    public static final byte TEXT_ALIGN    = ATTR_BASE + 9;
    public static final byte TEXT_BASELINE = ATTR_BASE + 10;
    public static final byte TRANSFORM     = ATTR_BASE + 11;
    public static final byte EFFECT        = ATTR_BASE + 12;
    public static final byte PUSH_CLIP     = ATTR_BASE + 13;
    public static final byte POP_CLIP      = ATTR_BASE + 14;
    public static final byte ARC_TYPE      = ATTR_BASE + 15;
    public static final byte FILL_RULE     = ATTR_BASE + 16;
    public static final byte DASH_ARRAY    = ATTR_BASE + 17;
    public static final byte DASH_OFFSET   = ATTR_BASE + 18;
    public static final byte FONT_SMOOTH   = ATTR_BASE + 19;

    public static final byte                     OP_BASE = 20;
    public static final byte FILL_RECT         = OP_BASE + 0;
    public static final byte STROKE_RECT       = OP_BASE + 1;
    public static final byte CLEAR_RECT        = OP_BASE + 2;
    public static final byte STROKE_LINE       = OP_BASE + 3;
    public static final byte FILL_OVAL         = OP_BASE + 4;
    public static final byte STROKE_OVAL       = OP_BASE + 5;
    public static final byte FILL_ROUND_RECT   = OP_BASE + 6;
    public static final byte STROKE_ROUND_RECT = OP_BASE + 7;
    public static final byte FILL_ARC          = OP_BASE + 8;
    public static final byte STROKE_ARC        = OP_BASE + 9;
    public static final byte FILL_TEXT         = OP_BASE + 10;
    public static final byte STROKE_TEXT       = OP_BASE + 11;

    public static final byte                PATH_BASE = 40;
    public static final byte PATHSTART    = PATH_BASE + 0;
    public static final byte MOVETO       = PATH_BASE + 1;
    public static final byte LINETO       = PATH_BASE + 2;
    public static final byte QUADTO       = PATH_BASE + 3;
    public static final byte CUBICTO      = PATH_BASE + 4;
    public static final byte CLOSEPATH    = PATH_BASE + 5;
    public static final byte PATHEND      = PATH_BASE + 6;
    public static final byte FILL_PATH    = PATH_BASE + 7;
    public static final byte STROKE_PATH  = PATH_BASE + 8;

    public static final byte                   IMG_BASE = 50;
    public static final byte DRAW_IMAGE      = IMG_BASE + 0;
    public static final byte DRAW_SUBIMAGE   = IMG_BASE + 1;
    public static final byte PUT_ARGB        = IMG_BASE + 2;
    public static final byte PUT_ARGBPRE_BUF = IMG_BASE + 3;

    public static final byte                   FX_BASE = 60;
    public static final byte FX_APPLY_EFFECT = FX_BASE + 0;

    public static final byte                   UTIL_BASE = 70;
    public static final byte RESET           = UTIL_BASE + 0;
    public static final byte SET_DIMS        = UTIL_BASE + 1;

    public static final byte CAP_BUTT   = 0;
    public static final byte CAP_ROUND  = 1;
    public static final byte CAP_SQUARE = 2;

    public static final byte JOIN_MITER = 0;
    public static final byte JOIN_ROUND = 1;
    public static final byte JOIN_BEVEL = 2;

    public static final byte ARC_OPEN   = 0;
    public static final byte ARC_CHORD  = 1;
    public static final byte ARC_PIE    = 2;

    public static final byte SMOOTH_GRAY = (byte) FontSmoothingType.GRAY.ordinal();
    public static final byte SMOOTH_LCD  = (byte) FontSmoothingType.LCD.ordinal();

    public static final byte ALIGN_LEFT       = 0;
    public static final byte ALIGN_CENTER     = 1;
    public static final byte ALIGN_RIGHT      = 2;
    public static final byte ALIGN_JUSTIFY    = 3;

    public static final byte BASE_TOP        = 0;
    public static final byte BASE_MIDDLE     = 1;
    public static final byte BASE_ALPHABETIC = 2;
    public static final byte BASE_BOTTOM     = 3;

    public static final byte FILL_RULE_NON_ZERO = 0;
    public static final byte FILL_RULE_EVEN_ODD = 1;

    static enum InitType {
        CLEAR,
        FILL_WHITE,
        PRESERVE_UPPER_LEFT
    }

    static class RenderBuf {
        final InitType init_type;
        RTTexture tex;
        Graphics g;
        EffectInput input;
        private PixelData savedPixelData = null;

        public RenderBuf(InitType init_type) {
            this.init_type = init_type;
        }

        public void dispose() {
            if (tex != null) tex.dispose();

            tex = null;
            g = null;
            input = null;
        }

        public boolean validate(Graphics resg, int tw, int th) {
            int cw, ch;
            boolean create;
            if (tex == null) {
                cw = ch = 0;
                create = true;
            } else {
                cw = tex.getContentWidth();
                ch = tex.getContentHeight();
                tex.lock();
                create = tex.isSurfaceLost() || cw < tw || ch < th;
            }
            if (create) {
                RTTexture oldtex = tex;
                ResourceFactory factory = (resg == null)
                    ? GraphicsPipeline.getDefaultResourceFactory()
                    : resg.getResourceFactory();
                RTTexture newtex =
                    factory.createRTTexture(tw, th, WrapMode.CLAMP_TO_ZERO);
                this.tex = newtex;
                this.g = newtex.createGraphics();
                this.input = new EffectInput(newtex);
                if (oldtex != null) {
                    if (init_type == InitType.PRESERVE_UPPER_LEFT) {
                        g.setCompositeMode(CompositeMode.SRC);
                        if (oldtex.isSurfaceLost()) {
                            if (savedPixelData != null) {
                                savedPixelData.restore(g, cw, ch);
                            }
                        } else {
                            g.drawTexture(oldtex, 0, 0, cw, ch);
                        }
                        g.setCompositeMode(CompositeMode.SRC_OVER);
                    }
                    oldtex.unlock();
                    oldtex.dispose();
                }
                if (init_type == InitType.FILL_WHITE) {
                    g.clear(Color.WHITE);
                }
                return true;
            } else {
                if (this.g == null) {
                    this.g = tex.createGraphics();
                    if (this.g == null) {
                        tex.dispose();
                        ResourceFactory factory = (resg == null)
                            ? GraphicsPipeline.getDefaultResourceFactory()
                            : resg.getResourceFactory();
                        tex = factory.createRTTexture(tw, th, WrapMode.CLAMP_TO_ZERO);
                        this.g = tex.createGraphics();
                        this.input = new EffectInput(tex);
                        if (savedPixelData != null) {
                            g.setCompositeMode(CompositeMode.SRC);
                            savedPixelData.restore(g, tw, th);
                            g.setCompositeMode(CompositeMode.SRC_OVER);
                        } else if (init_type == InitType.FILL_WHITE) {
                            g.clear(Color.WHITE);
                        }
                        return true;
                    }
                }
            }
            if (init_type == InitType.CLEAR) {
                g.clear();
            }
            return false;
        }

        private void save(int tw, int th) {
            if (tex.isVolatile()) {
                if (savedPixelData == null) {
                    savedPixelData = new PixelData(tw, th);
                }
                savedPixelData.save(tex);
            }
        }
    }

    // Saved pixel data used to preserve the image that backs the canvas if the
    // RTT is volatile.
    private static class PixelData {
        private IntBuffer pixels = null;
        private boolean validPixels = false;
        private int cw, ch;

        private PixelData(int cw, int ch) {
            this.cw = cw;
            this.ch = ch;
            pixels = IntBuffer.allocate(cw*ch);
        }

        private void save(RTTexture tex) {
            int tw = tex.getContentWidth();
            int th = tex.getContentHeight();
            if (cw < tw || ch < th) {
                cw = tw;
                ch = th;
                pixels = IntBuffer.allocate(cw*ch);
            }
            pixels.rewind();
            tex.readPixels(pixels);
            validPixels = true;
        }

        private void restore(Graphics g, int tw, int th) {
            if (validPixels) {
                Image img = Image.fromIntArgbPreData(pixels, tw, th);
                ResourceFactory factory = g.getResourceFactory();
                Texture tempTex =
                    factory.createTexture(img,
                                          Texture.Usage.DEFAULT,
                                          Texture.WrapMode.CLAMP_TO_EDGE);
                g.drawTexture(tempTex, 0, 0, tw, th);
                tempTex.dispose();
            }
        }
    }

    private static Blend BLENDER = new MyBlend(Mode.SRC_OVER, null, null);

    private GrowableDataBuffer thebuf;

    private final float highestPixelScale;
    private int tw, th;
    private int cw, ch;
    private RenderBuf cv;
    private RenderBuf temp;
    private RenderBuf clip;

    private float globalAlpha;
    private Blend.Mode blendmode;
    private Paint fillPaint, strokePaint;
    private float linewidth;
    private int linecap, linejoin;
    private float miterlimit;
    private double[] dashes;
    private float dashOffset;
    private BasicStroke stroke;
    private Path2D path;
    private NGText ngtext;
    private PrismTextLayout textLayout;
    private PGFont pgfont;
    private int smoothing;
    private int align;
    private int baseline;
    private Affine2D transform;
    private Affine2D inverseTransform;
    private boolean inversedirty;
    private LinkedList<Path2D> clipStack;
    private int clipsRendered;
    private boolean clipIsRect;
    private Rectangle clipRect;
    private Effect effect;
    private int arctype;

    static float TEMP_COORDS[] = new float[6];
    private static Arc2D TEMP_ARC = new Arc2D();
    private static RectBounds TEMP_RECTBOUNDS = new RectBounds();

    public NGCanvas() {
        Toolkit tk = Toolkit.getToolkit();
        ScreenConfigurationAccessor screenAccessor = tk.getScreenConfigurationAccessor();
        float hPS = 1.0f;
        for (Object screen : tk.getScreens()) {
            hPS = Math.max(screenAccessor.getRecommendedOutputScaleX(screen), hPS);
            hPS = Math.max(screenAccessor.getRecommendedOutputScaleY(screen), hPS);
        }
        highestPixelScale = (float) Math.ceil(hPS);

        cv = new RenderBuf(InitType.PRESERVE_UPPER_LEFT);
        temp = new RenderBuf(InitType.CLEAR);
        clip = new RenderBuf(InitType.FILL_WHITE);

        path = new Path2D();
        ngtext = new NGText();
        textLayout = new PrismTextLayout();
        transform = new Affine2D();
        clipStack = new LinkedList<Path2D>();
        initAttributes();
    }

    private void initAttributes() {
        globalAlpha = 1.0f;
        blendmode = Mode.SRC_OVER;
        fillPaint = Color.BLACK;
        strokePaint = Color.BLACK;
        linewidth = 1.0f;
        linecap = BasicStroke.CAP_SQUARE;
        linejoin = BasicStroke.JOIN_MITER;
        miterlimit = 10f;
        dashes = null;
        dashOffset = 0.0f;
        stroke = null;
        path.setWindingRule(Path2D.WIND_NON_ZERO);
        // ngtext stores no state between render operations
        // textLayout stores no state between render operations
        pgfont = (PGFont) FontHelper.getNativeFont(Font.getDefault());
        smoothing = SMOOTH_GRAY;
        align = ALIGN_LEFT;
        baseline = VPos.BASELINE.ordinal();
        transform.setToScale(highestPixelScale, highestPixelScale);
        clipStack.clear();
        resetClip(false);
    }

    static final Affine2D TEMP_PATH_TX = new Affine2D();
    static final int numCoords[] = { 2, 2, 4, 6, 0 };
    Shape untransformedPath = new Shape() {

        @Override
        public RectBounds getBounds() {
            if (transform.isTranslateOrIdentity()) {
                RectBounds rb = path.getBounds();
                if (transform.isIdentity()) {
                    return rb;
                } else {
                    float tx = (float) transform.getMxt();
                    float ty = (float) transform.getMyt();
                    return new RectBounds(rb.getMinX() - tx, rb.getMinY() - ty,
                                          rb.getMaxX() - tx, rb.getMaxY() - ty);
                }
            }
            // We could use Shape.accumulate, but that method optimizes the
            // bounds for curves and the optimized code above will simply ask
            // the path for its bounds - which in this case of a Path2D would
            // simply accumulate all of the coordinates in the buffer.  So,
            // we write a simpler accumulator loop here to be consistent with
            // the optimized case above.
            float x0 = Float.POSITIVE_INFINITY;
            float y0 = Float.POSITIVE_INFINITY;
            float x1 = Float.NEGATIVE_INFINITY;
            float y1 = Float.NEGATIVE_INFINITY;
            PathIterator pi = path.getPathIterator(getInverseTransform());
            while (!pi.isDone()) {
                int ncoords = numCoords[pi.currentSegment(TEMP_COORDS)];
                for (int i = 0; i < ncoords; i += 2) {
                    if (x0 > TEMP_COORDS[i+0]) x0 = TEMP_COORDS[i+0];
                    if (x1 < TEMP_COORDS[i+0]) x1 = TEMP_COORDS[i+0];
                    if (y0 > TEMP_COORDS[i+1]) y0 = TEMP_COORDS[i+1];
                    if (y1 < TEMP_COORDS[i+1]) y1 = TEMP_COORDS[i+1];
                }
                pi.next();
            }
            return new RectBounds(x0, y0, x1, y1);
        }

        @Override
        public boolean contains(float x, float y) {
            TEMP_COORDS[0] = x;
            TEMP_COORDS[1] = y;
            transform.transform(TEMP_COORDS, 0, TEMP_COORDS, 0, 1);
            x = TEMP_COORDS[0];
            y = TEMP_COORDS[1];
            return path.contains(x, y);
        }

        @Override
        public boolean intersects(float x, float y, float w, float h) {
            if (transform.isTranslateOrIdentity()) {
                x += transform.getMxt();
                y += transform.getMyt();
                return path.intersects(x, y, w, h);
            }
            PathIterator pi = path.getPathIterator(getInverseTransform());
            int crossings = Shape.rectCrossingsForPath(pi, x, y, x+w, y+h);
            // int mask = (windingRule == WIND_NON_ZERO ? -1 : 2);
            // return (crossings == Shape.RECT_INTERSECTS ||
            //             (crossings & mask) != 0);
            // with wind == NON_ZERO, then mask == -1 and
            // since REC_INTERSECTS != 0, we simplify to:
            return (crossings != 0);
        }

        @Override
        public boolean contains(float x, float y, float w, float h) {
            if (transform.isTranslateOrIdentity()) {
                x += transform.getMxt();
                y += transform.getMyt();
                return path.contains(x, y, w, h);
            }
            PathIterator pi = path.getPathIterator(getInverseTransform());
            int crossings = Shape.rectCrossingsForPath(pi, x, y, x+w, y+h);
            // int mask = (windingRule == WIND_NON_ZERO ? -1 : 2);
            // return (crossings != Shape.RECT_INTERSECTS &&
            //             (crossings & mask) != 0);
            // with wind == NON_ZERO, then mask == -1 we simplify to:
            return (crossings != Shape.RECT_INTERSECTS && crossings != 0);
        }

        public BaseTransform getCombinedTransform(BaseTransform tx) {
            if (transform.isIdentity()) return tx;
            if (transform.equals(tx)) return null;
            Affine2D inv = getInverseTransform();
            if (tx == null || tx.isIdentity()) return inv;
            TEMP_PATH_TX.setTransform(tx);
            TEMP_PATH_TX.concatenate(inv);
            return TEMP_PATH_TX;
        }

        @Override
        public PathIterator getPathIterator(BaseTransform tx) {
            return path.getPathIterator(getCombinedTransform(tx));
        }

        @Override
        public PathIterator getPathIterator(BaseTransform tx, float flatness) {
            return path.getPathIterator(getCombinedTransform(tx), flatness);
        }

        @Override
        public Shape copy() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    };

    private Affine2D getInverseTransform() {
        if (inverseTransform == null) {
            inverseTransform = new Affine2D();
            inversedirty = true;
        }
        if (inversedirty) {
            inverseTransform.setTransform(transform);
            try {
                inverseTransform.invert();
            } catch (NoninvertibleTransformException e) {
                inverseTransform.setToScale(0, 0);
            }
            inversedirty = false;
        }
        return inverseTransform;
    }

    @Override
    protected boolean hasOverlappingContents() {
        return true;
    }

    private static void shapebounds(Shape shape, RectBounds bounds,
                                    BaseTransform transform)
    {
        TEMP_COORDS[0] = TEMP_COORDS[1] = Float.POSITIVE_INFINITY;
        TEMP_COORDS[2] = TEMP_COORDS[3] = Float.NEGATIVE_INFINITY;
        Shape.accumulate(TEMP_COORDS, shape, transform);
        bounds.setBounds(TEMP_COORDS[0], TEMP_COORDS[1],
                         TEMP_COORDS[2], TEMP_COORDS[3]);
    }

    private static void strokebounds(BasicStroke stroke, Shape shape,
                                     RectBounds bounds, BaseTransform transform)
    {
        TEMP_COORDS[0] = TEMP_COORDS[1] = Float.POSITIVE_INFINITY;
        TEMP_COORDS[2] = TEMP_COORDS[3] = Float.NEGATIVE_INFINITY;
        stroke.accumulateShapeBounds(TEMP_COORDS, shape, transform);
        bounds.setBounds(TEMP_COORDS[0], TEMP_COORDS[1],
                            TEMP_COORDS[2], TEMP_COORDS[3]);
    }

    private static void runOnRenderThread(final Runnable r) {
        // We really need a standard mechanism to detect the render thread !
        if (Thread.currentThread().getName().startsWith("QuantumRenderer")) {
            r.run();
        } else {
            FutureTask<Void> f = new FutureTask<Void>(r, null);
            Toolkit.getToolkit().addRenderJob(new RenderJob(f));
            try {
                // block until job is complete
                f.get();
            } catch (ExecutionException ex) {
                throw new AssertionError(ex);
            } catch (InterruptedException ex) {
                // ignore; recovery is impossible
            }
        }
    }

    private boolean printedCanvas(Graphics g) {
       final RTTexture localTex = cv.tex;
       if (!(g instanceof PrinterGraphics) || localTex  == null) {
          return false;
        }
        ResourceFactory factory = g.getResourceFactory();
        boolean isCompatTex = factory.isCompatibleTexture(localTex);
        if (isCompatTex) {
            return false;
        }

        final int tw = localTex.getContentWidth();
        final int th = localTex.getContentHeight();
        final RTTexture tmpTex =
              factory.createRTTexture(tw, th, WrapMode.CLAMP_TO_ZERO);
        final Graphics texg = tmpTex.createGraphics();
        texg.setCompositeMode(CompositeMode.SRC);
        if (cv.savedPixelData == null) {
            final PixelData pd = new PixelData(cw, ch);
            runOnRenderThread(() -> {
              pd.save(localTex);
              pd.restore(texg, tw, th);
            });
        } else {
            cv.savedPixelData.restore(texg, tw, th);
        }
        g.drawTexture(tmpTex, 0, 0, tw, th);
        tmpTex.unlock();
        tmpTex.dispose();
        return true;
    }

    @Override
    protected void renderContent(Graphics g) {
        if (printedCanvas(g)) return;
        initCanvas(g);
        if (cv.tex != null) {
            if (thebuf != null) {
                renderStream(thebuf);
                GrowableDataBuffer.returnBuffer(thebuf);
                thebuf = null;
            }
            float dw = tw / highestPixelScale;
            float dh = th / highestPixelScale;
            g.drawTexture(cv.tex,
                          0, 0, dw, dh,
                          0, 0, tw, th);
            // Must save the pixels every frame if RTT is volatile.
            cv.save(tw, th);
        }
        this.temp.g = this.clip.g = this.cv.g = null;
    }

    @Override
    public void renderForcedContent(Graphics gOptional) {
        if (thebuf != null) {
            initCanvas(gOptional);
            if (cv.tex != null) {
                renderStream(thebuf);
                GrowableDataBuffer.returnBuffer(thebuf);
                thebuf = null;
                cv.save(tw, th);
            }
            this.temp.g = this.clip.g = this.cv.g = null;
        }
    }

    private void initCanvas(Graphics g) {
        if (tw <= 0 || th <= 0) {
            cv.dispose();
            return;
        }
        if (cv.validate(g, tw, th)) {
            // If the texture was recreated then we add a permanent
            // "useful" and extra "lock" status to it.
            cv.tex.contentsUseful();
            cv.tex.makePermanent();
            cv.tex.lock();
        }
    }

    private void clearCanvas(int x, int y, int w, int h) {
        cv.g.setCompositeMode(CompositeMode.CLEAR);
        cv.g.setTransform(BaseTransform.IDENTITY_TRANSFORM);
        cv.g.fillQuad(x, y, x+w, y+h);
        cv.g.setCompositeMode(CompositeMode.SRC_OVER);
    }

    private void resetClip(boolean andDispose) {
        if (andDispose) clip.dispose();
        clipsRendered = 0;
        clipIsRect = true;
        clipRect = null;
    }

    private static final float CLIPRECT_TOLERANCE = 1.0f / 256.0f;
    private static final Rectangle TEMP_RECT = new Rectangle();
    private boolean initClip() {
        boolean clipValidated;
        if (clipIsRect) {
            clipValidated = false;
        } else {
            clipValidated = true;
            if (clip.validate(cv.g, tw, th)) {
                clip.tex.contentsUseful();
                // Reset, but do not dispose - we just validated (and cleared) it...
                resetClip(false);
            }
        }
        int clipSize = clipStack.size();
        while (clipsRendered < clipSize) {
            Path2D clippath = clipStack.get(clipsRendered++);
            if (clipIsRect) {
                if (clippath.checkAndGetIntRect(TEMP_RECT, CLIPRECT_TOLERANCE)) {
                    if (clipRect == null) {
                        clipRect = new Rectangle(TEMP_RECT);
                    } else {
                        clipRect.intersectWith(TEMP_RECT);
                    }
                    continue;
                }
                clipIsRect = false;
                if (!clipValidated) {
                    clipValidated = true;
                    if (clip.validate(cv.g, tw, th)) {
                        clip.tex.contentsUseful();
                        // No need to reset, this is our first fill.
                    }
                }
                if (clipRect != null) {
                    renderClip(new RoundRectangle2D(clipRect.x, clipRect.y,
                                                    clipRect.width, clipRect.height,
                                                    0, 0));
                }
            }
            shapebounds(clippath, TEMP_RECTBOUNDS, BaseTransform.IDENTITY_TRANSFORM);
            TEMP_RECT.setBounds(TEMP_RECTBOUNDS);
            if (clipRect == null) {
                clipRect = new Rectangle(TEMP_RECT);
            } else {
                clipRect.intersectWith(TEMP_RECT);
            }
            renderClip(clippath);
        }
        if (clipValidated && clipIsRect) {
            clip.tex.unlock();
        }
        return !clipIsRect;
    }

    private void renderClip(Shape clippath) {
        temp.validate(cv.g, tw, th);
        temp.g.setPaint(Color.WHITE);
        temp.g.setTransform(BaseTransform.IDENTITY_TRANSFORM);
        temp.g.fill(clippath);
        blendAthruBintoC(temp, Mode.SRC_IN, clip, null, CompositeMode.SRC, clip);
        temp.tex.unlock();
    }

    private Rectangle applyEffectOnAintoC(Effect definput,
                                          Effect effect,
                                          BaseTransform transform,
                                          Rectangle outputClip,
                                          CompositeMode comp,
                                          RenderBuf destbuf)
    {
        FilterContext fctx =
            PrFilterContext.getInstance(destbuf.tex.getAssociatedScreen());
        ImageData id =
            effect.filter(fctx, transform, outputClip, null, definput);
        Rectangle r = id.getUntransformedBounds();
        Filterable f = id.getUntransformedImage();
        Texture tex = ((PrTexture) f).getTextureObject();
        destbuf.g.setTransform(id.getTransform());
        destbuf.g.setCompositeMode(comp);
        destbuf.g.drawTexture(tex, r.x, r.y, r.width, r.height);
        destbuf.g.setTransform(BaseTransform.IDENTITY_TRANSFORM);
        destbuf.g.setCompositeMode(CompositeMode.SRC_OVER);
        Rectangle resultBounds = id.getTransformedBounds(outputClip);
        id.unref();
        return resultBounds;
    }

    private void blendAthruBintoC(RenderBuf drawbuf,
                                  Mode mode,
                                  RenderBuf clipbuf,
                                  RectBounds bounds,
                                  CompositeMode comp,
                                  RenderBuf destbuf)
    {
        BLENDER.setTopInput(drawbuf.input);
        BLENDER.setBottomInput(clipbuf.input);
        BLENDER.setMode(mode);
        Rectangle blendclip;
        if (bounds != null) {
            blendclip = new Rectangle(bounds);
        } else {
            blendclip = null;
        }
        applyEffectOnAintoC(null, BLENDER,
                            BaseTransform.IDENTITY_TRANSFORM, blendclip,
                            comp, destbuf);
    }

    private void setupFill(Graphics gr) {
        gr.setPaint(fillPaint);
    }

    private BasicStroke getStroke() {
        if (stroke == null) {
            stroke = new BasicStroke(linewidth, linecap, linejoin,
                                     miterlimit, dashes, dashOffset);
        }
        return stroke;
    }

    private void setupStroke(Graphics gr) {
        gr.setStroke(getStroke());
        gr.setPaint(strokePaint);
    }

    private static final int prcaps[] = {
        BasicStroke.CAP_BUTT,
        BasicStroke.CAP_ROUND,
        BasicStroke.CAP_SQUARE,
    };
    private static final int prjoins[] = {
        BasicStroke.JOIN_MITER,
        BasicStroke.JOIN_ROUND,
        BasicStroke.JOIN_BEVEL,
    };
    private static final int prbases[] = {
        VPos.TOP.ordinal(),
        VPos.CENTER.ordinal(),
        VPos.BASELINE.ordinal(),
        VPos.BOTTOM.ordinal(),
    };
    private static final Affine2D TEMP_TX = new Affine2D();
    private void renderStream(GrowableDataBuffer buf) {
        while (buf.hasValues()) {
            int token = buf.getByte();
            switch (token) {
                case RESET:
                    initAttributes();
                    // RESET is always followed by SET_DIMS
                    // Setting cwh = twh avoids unnecessary double clears
                    this.cw = this.tw;
                    this.ch = this.th;
                    clearCanvas(0, 0, this.tw, this.th);
                    break;
                case SET_DIMS:
                    int neww = (int) Math.ceil(buf.getFloat() * highestPixelScale);
                    int newh = (int) Math.ceil(buf.getFloat() * highestPixelScale);
                    int clearx = Math.min(neww, this.cw);
                    int cleary = Math.min(newh, this.ch);
                    if (clearx < this.tw) {
                        // tw is set to the final width, we simulate all of
                        // the intermediate changes in size by making sure
                        // that all pixels outside of any size change are
                        // cleared at the stream point where they happened
                        clearCanvas(clearx, 0, this.tw-clearx, this.th);
                    }
                    if (cleary < this.th) {
                        // th is set to the final width, we simulate all of
                        // the intermediate changes in size by making sure
                        // that all pixels outside of any size change are
                        // cleared at the stream point where they happened
                        clearCanvas(0, cleary, this.tw, this.th-cleary);
                    }
                    this.cw = neww;
                    this.ch = newh;
                    break;
                case PATHSTART:
                    path.reset();
                    break;
                case MOVETO:
                    path.moveTo(buf.getFloat(), buf.getFloat());
                    break;
                case LINETO:
                    path.lineTo(buf.getFloat(), buf.getFloat());
                    break;
                case QUADTO:
                    path.quadTo(buf.getFloat(), buf.getFloat(),
                                buf.getFloat(), buf.getFloat());
                    break;
                case CUBICTO:
                    path.curveTo(buf.getFloat(), buf.getFloat(),
                                 buf.getFloat(), buf.getFloat(),
                                 buf.getFloat(), buf.getFloat());
                    break;
                case CLOSEPATH:
                    path.closePath();
                    break;
                case PATHEND:
                    if (highestPixelScale != 1.0f) {
                        TEMP_TX.setToScale(highestPixelScale, highestPixelScale);
                        path.transform(TEMP_TX);
                    }
                    break;
                case PUSH_CLIP:
                {
                    Path2D clippath = (Path2D) buf.getObject();
                    if (highestPixelScale != 1.0f) {
                        TEMP_TX.setToScale(highestPixelScale, highestPixelScale);
                        clippath.transform(TEMP_TX);
                    }
                    clipStack.addLast(clippath);
                    break;
                }
                case POP_CLIP:
                    // Let it be recreated when next needed
                    resetClip(true);
                    clipStack.removeLast();
                    break;
                case ARC_TYPE:
                {
                    byte type = buf.getByte();
                    switch (type) {
                        case ARC_OPEN:  arctype = Arc2D.OPEN;  break;
                        case ARC_CHORD: arctype = Arc2D.CHORD; break;
                        case ARC_PIE:   arctype = Arc2D.PIE;   break;
                    }
                    break;
                }
                case PUT_ARGB:
                {
                    float dx1 = buf.getInt();
                    float dy1 = buf.getInt();
                    int argb = buf.getInt();
                    Graphics gr = cv.g;
                    gr.setExtraAlpha(1.0f);
                    gr.setCompositeMode(CompositeMode.SRC);
                    gr.setTransform(BaseTransform.IDENTITY_TRANSFORM);
                    dx1 *= highestPixelScale;
                    dy1 *= highestPixelScale;
                    float a = ((argb) >>> 24) / 255.0f;
                    float r = (((argb) >> 16) & 0xff) / 255.0f;
                    float g = (((argb) >>  8) & 0xff) / 255.0f;
                    float b = (((argb)      ) & 0xff) / 255.0f;
                    gr.setPaint(new Color(r, g, b, a));
                    // Note that we cannot use fillRect here because SRC
                    // mode does not interact well with antialiasing.
                    // fillQuad does hard edges which matches the concept
                    // of setting adjacent abutting, non-overlapping "pixels"
                    gr.fillQuad(dx1, dy1, dx1+highestPixelScale, dy1+highestPixelScale);
                    gr.setCompositeMode(CompositeMode.SRC_OVER);
                    break;
                }
                case PUT_ARGBPRE_BUF:
                {
                    float dx1 = buf.getInt();
                    float dy1 = buf.getInt();
                    int w  = buf.getInt();
                    int h  = buf.getInt();
                    byte[] data = (byte[]) buf.getObject();
                    Image img = Image.fromByteBgraPreData(data, w, h);
                    Graphics gr = cv.g;
                    ResourceFactory factory = gr.getResourceFactory();
                    Texture tex =
                        factory.getCachedTexture(img, Texture.WrapMode.CLAMP_TO_EDGE);
                    gr.setTransform(BaseTransform.IDENTITY_TRANSFORM);
                    gr.setCompositeMode(CompositeMode.SRC);
                    float dx2 = dx1 + w;
                    float dy2 = dy1 + h;
                    dx1 *= highestPixelScale;
                    dy1 *= highestPixelScale;
                    dx2 *= highestPixelScale;
                    dy2 *= highestPixelScale;
                    gr.drawTexture(tex,
                                   dx1, dy1, dx2, dy2,
                                   0, 0, w, h);
                    tex.contentsNotUseful();
                    tex.unlock();
                    gr.setCompositeMode(CompositeMode.SRC_OVER);
                    break;
                }
                case TRANSFORM:
                {
                    double mxx = buf.getDouble() * highestPixelScale;
                    double mxy = buf.getDouble() * highestPixelScale;
                    double mxt = buf.getDouble() * highestPixelScale;
                    double myx = buf.getDouble() * highestPixelScale;
                    double myy = buf.getDouble() * highestPixelScale;
                    double myt = buf.getDouble() * highestPixelScale;
                    transform.setTransform(mxx, myx, mxy, myy, mxt, myt);
                    inversedirty = true;
                    break;
                }
                case GLOBAL_ALPHA:
                    globalAlpha = buf.getFloat();
                    break;
                case FILL_RULE:
                    if (buf.getByte() == FILL_RULE_NON_ZERO) {
                        path.setWindingRule(Path2D.WIND_NON_ZERO);
                    } else {
                        path.setWindingRule(Path2D.WIND_EVEN_ODD);
                    }
                    break;
                case COMP_MODE:
                    blendmode = (Blend.Mode)buf.getObject();
                    break;
                case FILL_PAINT:
                    fillPaint = (Paint) buf.getObject();
                    break;
                case STROKE_PAINT:
                    strokePaint = (Paint) buf.getObject();
                    break;
                case LINE_WIDTH:
                    linewidth = buf.getFloat();
                    stroke = null;
                    break;
                case LINE_CAP:
                    linecap = prcaps[buf.getUByte()];
                    stroke = null;
                    break;
                case LINE_JOIN:
                    linejoin = prjoins[buf.getUByte()];
                    stroke = null;
                    break;
                case MITER_LIMIT:
                    miterlimit = buf.getFloat();
                    stroke = null;
                    break;
                case DASH_ARRAY:
                    dashes = (double[]) buf.getObject();
                    stroke = null;
                    break;
                case DASH_OFFSET:
                    dashOffset = buf.getFloat();
                    stroke = null;
                    break;
                case FONT:
                    pgfont = (PGFont) buf.getObject();
                    break;
                case FONT_SMOOTH:
                    smoothing = buf.getUByte();
                    break;
                case TEXT_ALIGN:
                    align = buf.getUByte();
                    break;
                case TEXT_BASELINE:
                    baseline = prbases[buf.getUByte()];
                    break;
                case FX_APPLY_EFFECT:
                {
                    Effect e = (Effect) buf.getObject();
                    RenderBuf dest = clipStack.isEmpty() ? cv : temp;
                    BaseTransform tx;
                    if (highestPixelScale != 1.0f) {
                        TEMP_TX.setToScale(highestPixelScale, highestPixelScale);
                        tx = TEMP_TX;
                        cv.input.setPixelScale(highestPixelScale);
                    } else {
                        tx = BaseTransform.IDENTITY_TRANSFORM;
                    }
                    applyEffectOnAintoC(cv.input, e,
                                        tx, null,
                                        CompositeMode.SRC, dest);
                    cv.input.setPixelScale(1.0f);
                    if (dest != cv) {
                        blendAthruBintoC(dest, Mode.SRC_IN, clip,
                                         null, CompositeMode.SRC, cv);
                    }
                    break;
                }
                case EFFECT:
                    effect = (Effect) buf.getObject();
                    break;
                case FILL_PATH:
                case STROKE_PATH:
                case STROKE_LINE:
                case FILL_RECT:
                case CLEAR_RECT:
                case STROKE_RECT:
                case FILL_OVAL:
                case STROKE_OVAL:
                case FILL_ROUND_RECT:
                case STROKE_ROUND_RECT:
                case FILL_ARC:
                case STROKE_ARC:
                case DRAW_IMAGE:
                case DRAW_SUBIMAGE:
                case FILL_TEXT:
                case STROKE_TEXT:
                {
                    RenderBuf dest;
                    boolean tempvalidated;
                    boolean clipvalidated = initClip();
                    if (clipvalidated) {
                        temp.validate(cv.g, tw, th);
                        tempvalidated = true;
                        dest = temp;
                    } else if (blendmode != Blend.Mode.SRC_OVER) {
                        temp.validate(cv.g, tw, th);
                        tempvalidated = true;
                        dest = temp;
                    } else {
                        tempvalidated = false;
                        dest = cv;
                    }
                    if (effect != null) {
                        buf.save();
                        handleRenderOp(token, buf, null, TEMP_RECTBOUNDS);
                        RenderInput ri =
                            new RenderInput(token, buf, transform, TEMP_RECTBOUNDS);
                        // If we are rendering to cv then we need the results of
                        // the effect to be applied "SRC_OVER" onto the canvas.
                        // If we are rendering to temp then either SRC or SRC_OVER
                        // would work since we know it would have been freshly
                        // erased above, but using the more common SRC_OVER may save
                        // having to update the hardware blend equations.
                        Rectangle resultBounds =
                            applyEffectOnAintoC(ri, effect,
                                                transform, clipRect,
                                                CompositeMode.SRC_OVER, dest);
                        if (dest != cv) {
                            TEMP_RECTBOUNDS.setBounds(resultBounds.x, resultBounds.y,
                                                      resultBounds.x + resultBounds.width,
                                                      resultBounds.y + resultBounds.height);
                        }
                    } else {
                        Graphics g = dest.g;
                        g.setExtraAlpha(globalAlpha);
                        g.setTransform(transform);
                        g.setClipRect(clipRect);
                        // If we are not rendering directly to the canvas then
                        // we need to save the bounds for the later stages.
                        RectBounds optSaveBounds =
                            (dest != cv) ? TEMP_RECTBOUNDS : null;
                        handleRenderOp(token, buf, g, optSaveBounds);
                        g.setClipRect(null);
                    }
                    if (clipvalidated) {
                        CompositeMode compmode;
                        if (blendmode == Blend.Mode.SRC_OVER) {
                            // For the SRC_OVER case we can point the clip
                            // operation directly to the screen with the Prism
                            // SRC_OVER composite mode.
                            dest = cv;
                            compmode = CompositeMode.SRC_OVER;
                        } else {
                            // Here we are blending the rendered pixels that
                            // were output to the temp buffer above against the
                            // pixels of the canvas and we need to put them
                            // back into the temp buffer.  We must use SRC
                            // mode here so that the erased (or reduced) pixels
                            // actually get reduced to their new alpha.
                            // assert: dest == temp;
                            compmode = CompositeMode.SRC;
                        }
                        if (clipRect != null) {
                            TEMP_RECTBOUNDS.intersectWith(clipRect);
                        }
                        if (!TEMP_RECTBOUNDS.isEmpty()) {
                            if (dest == cv && cv.g instanceof MaskTextureGraphics) {
                                MaskTextureGraphics mtg = (MaskTextureGraphics) cv.g;
                                int dx = (int) Math.floor(TEMP_RECTBOUNDS.getMinX());
                                int dy = (int) Math.floor(TEMP_RECTBOUNDS.getMinY());
                                int dw = (int) Math.ceil(TEMP_RECTBOUNDS.getMaxX()) - dx;
                                int dh = (int) Math.ceil(TEMP_RECTBOUNDS.getMaxY()) - dy;
                                mtg.drawPixelsMasked(temp.tex, clip.tex,
                                                     dx, dy, dw, dh,
                                                     dx, dy, dx, dy);
                            } else {
                                blendAthruBintoC(temp, Mode.SRC_IN, clip,
                                                 TEMP_RECTBOUNDS, compmode, dest);
                            }
                        }
                    }
                    if (blendmode != Blend.Mode.SRC_OVER) {
                        // We always use SRC mode here because the results of
                        // the blend operation are final and must replace
                        // the associated pixel in the canvas with no further
                        // blending math.
                        if (clipRect != null) {
                            TEMP_RECTBOUNDS.intersectWith(clipRect);
                        }
                        blendAthruBintoC(temp, blendmode, cv,
                                         TEMP_RECTBOUNDS, CompositeMode.SRC, cv);
                    }
                    if (clipvalidated) {
                        clip.tex.unlock();
                    }
                    if (tempvalidated) {
                        temp.tex.unlock();
                    }
                    break;
                }
                default:
                    throw new InternalError("Unrecognized PGCanvas token: "+token);
            }
        }
    }

    /**
     * Calculate bounds and/or render one single rendering operation.
     * All of the data for the rendering operation should be consumed
     * so that the buffer is left at the next token in the stream.
     *
     * @param token the stream token for the rendering op
     * @param buf the GrowableDataBuffer to get rendering info from
     * @param gr  the Graphics to render to, if not null
     * @param bounds the RectBounds to accumulate bounds into, if not null
     */
    public void handleRenderOp(int token, GrowableDataBuffer buf,
                               Graphics gr, RectBounds bounds)
    {
        boolean strokeBounds = false;
        boolean transformBounds = false;
        switch (token) {
            case FILL_PATH:
            {
                if (bounds != null) {
                    shapebounds(path, bounds, BaseTransform.IDENTITY_TRANSFORM);
                }
                if (gr != null) {
                    setupFill(gr);
                    gr.fill(untransformedPath);
                }
                break;
            }
            case STROKE_PATH:
            {
                if (bounds != null) {
                    strokebounds(getStroke(), untransformedPath, bounds, transform);
                }
                if (gr != null) {
                    setupStroke(gr);
                    gr.draw(untransformedPath);
                }
                break;
            }
            case STROKE_LINE:
            {
                float x1 = buf.getFloat();
                float y1 = buf.getFloat();
                float x2 = buf.getFloat();
                float y2 = buf.getFloat();
                if (bounds != null) {
                    bounds.setBoundsAndSort(x1, y1, x2, y2);
                    strokeBounds = true;
                    transformBounds = true;
                }
                if (gr != null) {
                    setupStroke(gr);
                    gr.drawLine(x1, y1, x2, y2);
                }
                break;
            }
            case STROKE_RECT:
            case STROKE_OVAL:
                strokeBounds = true;
            case FILL_RECT:
            case CLEAR_RECT:
            case FILL_OVAL:
            {
                float x = buf.getFloat();
                float y = buf.getFloat();
                float w = buf.getFloat();
                float h = buf.getFloat();
                if (bounds != null) {
                    bounds.setBounds(x, y, x+w, y+h);
                    transformBounds = true;
                }
                if (gr != null) {
                    switch (token) {
                        case FILL_RECT:
                            setupFill(gr);
                            gr.fillRect(x, y, w, h);
                            break;
                        case FILL_OVAL:
                            setupFill(gr);
                            gr.fillEllipse(x, y, w, h);
                            break;
                        case STROKE_RECT:
                            setupStroke(gr);
                            gr.drawRect(x, y, w, h);
                            break;
                        case STROKE_OVAL:
                            setupStroke(gr);
                            gr.drawEllipse(x, y, w, h);
                            break;
                        case CLEAR_RECT:
                            gr.setCompositeMode(CompositeMode.CLEAR);
                            gr.fillRect(x, y, w, h);
                            gr.setCompositeMode(CompositeMode.SRC_OVER);
                            break;
                    }
                }
                break;
            }
            case STROKE_ROUND_RECT:
                strokeBounds = true;
            case FILL_ROUND_RECT:
            {
                float x = buf.getFloat();
                float y = buf.getFloat();
                float w = buf.getFloat();
                float h = buf.getFloat();
                float aw = buf.getFloat();
                float ah = buf.getFloat();
                if (bounds != null) {
                    bounds.setBounds(x, y, x+w, y+h);
                    transformBounds = true;
                }
                if (gr != null) {
                    if (token == FILL_ROUND_RECT) {
                        setupFill(gr);
                        gr.fillRoundRect(x, y, w, h, aw, ah);
                    } else {
                        setupStroke(gr);
                        gr.drawRoundRect(x, y, w, h, aw, ah);
                    }
                }
                break;
            }
            case FILL_ARC:
            case STROKE_ARC:
            {
                float x = buf.getFloat();
                float y = buf.getFloat();
                float w = buf.getFloat();
                float h = buf.getFloat();
                float as = buf.getFloat();
                float ae = buf.getFloat();
                TEMP_ARC.setArc(x, y, w, h, as, ae, arctype);
                if (token == FILL_ARC) {
                    if (bounds != null) {
                        shapebounds(TEMP_ARC, bounds, transform);
                    }
                    if (gr != null) {
                        setupFill(gr);
                        gr.fill(TEMP_ARC);
                    }
                } else {
                    if (bounds != null) {
                        strokebounds(getStroke(), TEMP_ARC, bounds, transform);
                    }
                    if (gr != null) {
                        setupStroke(gr);
                        gr.draw(TEMP_ARC);
                    }
                }
                break;
            }
            case DRAW_IMAGE:
            case DRAW_SUBIMAGE:
            {
                float dx = buf.getFloat();
                float dy = buf.getFloat();
                float dw = buf.getFloat();
                float dh = buf.getFloat();
                Image img = (Image) buf.getObject();
                float sx, sy, sw, sh;
                if (token == DRAW_IMAGE) {
                    sx = sy = 0f;
                    sw = img.getWidth();
                    sh = img.getHeight();
                } else {
                    sx = buf.getFloat();
                    sy = buf.getFloat();
                    sw = buf.getFloat();
                    sh = buf.getFloat();
                    float ps = img.getPixelScale();
                    if (ps != 1.0f) {
                        sx *= ps;
                        sy *= ps;
                        sw *= ps;
                        sh *= ps;
                    }
                }
                if (bounds != null) {
                    bounds.setBounds(dx, dy, dx+dw, dy+dh);
                    transformBounds = true;
                }
                if (gr != null) {
                    ResourceFactory factory = gr.getResourceFactory();
                    Texture tex =
                        factory.getCachedTexture(img, Texture.WrapMode.CLAMP_TO_EDGE);
                    gr.drawTexture(tex,
                                   dx, dy, dx+dw, dy+dh,
                                   sx, sy, sx+sw, sy+sh);
                    tex.unlock();
                }
                break;
            }
            case FILL_TEXT:
            case STROKE_TEXT:
            {
                float x = buf.getFloat();
                float y = buf.getFloat();
                float maxWidth = buf.getFloat();
                boolean rtl = buf.getBoolean();
                String string = (String) buf.getObject();
                int dir = rtl ? PrismTextLayout.DIRECTION_RTL :
                                PrismTextLayout.DIRECTION_LTR;

                textLayout.setContent(string, pgfont);
                textLayout.setAlignment(align);
                textLayout.setDirection(dir);
                float xAlign = 0, yAlign = 0;
                BaseBounds layoutBounds = textLayout.getBounds();
                float layoutWidth = layoutBounds.getWidth();
                float layoutHeight = layoutBounds.getHeight();
                switch (align) {
                    case ALIGN_RIGHT: xAlign = layoutWidth; break;
                    case ALIGN_CENTER: xAlign = layoutWidth / 2; break;
                }
                switch (baseline) {
                    case BASE_ALPHABETIC: yAlign = -layoutBounds.getMinY(); break;
                    case BASE_MIDDLE: yAlign = layoutHeight / 2; break;
                    case BASE_BOTTOM: yAlign = layoutHeight; break;
                }
                float scaleX = 1;
                float layoutX = 0;
                float layoutY = y - yAlign;
                if (maxWidth > 0.0 && layoutWidth > maxWidth) {
                    float sx = maxWidth / layoutWidth;
                    if (rtl) {
                        layoutX = -((x + maxWidth) / sx - xAlign);
                        scaleX = -sx;
                    } else {
                        layoutX = x / sx - xAlign;
                        scaleX = sx;
                    }
                } else {
                    if (rtl) {
                        layoutX = -(x - xAlign + layoutWidth);
                        scaleX = -1;
                    } else {
                        layoutX = x - xAlign;
                    }
                }
                if (bounds != null) {
                    computeTextLayoutBounds(bounds, transform, scaleX, layoutX, layoutY, token);
                }
                if (gr != null) {
                    if (scaleX != 1) {
                        gr.scale(scaleX, 1);
                    }
                    ngtext.setLayoutLocation(-layoutX, -layoutY);
                    if (token == FILL_TEXT) {
                        ngtext.setMode(NGShape.Mode.FILL);
                        ngtext.setFillPaint(fillPaint);
                        if (fillPaint.isProportional() || smoothing == SMOOTH_LCD) {
                            RectBounds textBounds = new RectBounds();
                            computeTextLayoutBounds(textBounds, BaseTransform.IDENTITY_TRANSFORM,
                                                    1, layoutX, layoutY, token);
                            ngtext.setContentBounds(textBounds);
                        }
                    } else {
                        // SMOOTH_LCD does not apply to stroked text
                        if (strokePaint.isProportional() /* || smoothing == SMOOTH_LCD */) {
                            RectBounds textBounds = new RectBounds();
                            computeTextLayoutBounds(textBounds, BaseTransform.IDENTITY_TRANSFORM,
                                                    1, layoutX, layoutY, token);
                            ngtext.setContentBounds(textBounds);
                        }
                        ngtext.setMode(NGShape.Mode.STROKE);
                        ngtext.setDrawStroke(getStroke());
                        ngtext.setDrawPaint(strokePaint);
                    }
                    ngtext.setFont(pgfont);
                    ngtext.setFontSmoothingType(smoothing);
                    ngtext.setGlyphs(textLayout.getRuns());
                    ngtext.renderContent(gr);
                }
                break;
            }
            default:
                throw new InternalError("Unrecognized PGCanvas rendering token: "+token);
        }
        if (bounds != null) {
            if (strokeBounds) {
                BasicStroke s = getStroke();
                if (s.getType() != BasicStroke.TYPE_INNER) {
                    float lw = s.getLineWidth();
                    if (s.getType() == BasicStroke.TYPE_CENTERED) {
                        lw /= 2f;
                    }
                    bounds.grow(lw, lw);
                }
            }
            if (transformBounds) {
                txBounds(bounds, transform);
            }
        }
    }

    void computeTextLayoutBounds(RectBounds bounds, BaseTransform transform,
                                 float scaleX, float layoutX, float layoutY,
                                 int token)
    {
        textLayout.getBounds(null, bounds);
        TEMP_TX.setTransform(transform);
        TEMP_TX.scale(scaleX, 1);
        TEMP_TX.translate(layoutX, layoutY);
        TEMP_TX.transform(bounds, bounds);
        if (token == STROKE_TEXT) {
            int flag = PrismTextLayout.TYPE_TEXT;
            Shape textShape = textLayout.getShape(flag, null);
            RectBounds shapeBounds = new RectBounds();
            strokebounds(getStroke(), textShape, shapeBounds, TEMP_TX);
            bounds.unionWith(shapeBounds);
        }
    }

    static void txBounds(RectBounds bounds, BaseTransform transform) {
        switch (transform.getType()) {
            case BaseTransform.TYPE_IDENTITY:
                break;
            case BaseTransform.TYPE_TRANSLATION:
                float tx = (float) transform.getMxt();
                float ty = (float) transform.getMyt();
                bounds.setBounds(bounds.getMinX() + tx, bounds.getMinY() + ty,
                                 bounds.getMaxX() + tx, bounds.getMaxY() + ty);
                break;
            default:
                BaseBounds txbounds = transform.transform(bounds, bounds);
                if (txbounds != bounds) {
                    bounds.setBounds(txbounds.getMinX(), txbounds.getMinY(),
                                     txbounds.getMaxX(), txbounds.getMaxY());
                }
                break;
        }
    }

    static void inverseTxBounds(RectBounds bounds, BaseTransform transform) {
        switch (transform.getType()) {
            case BaseTransform.TYPE_IDENTITY:
                break;
            case BaseTransform.TYPE_TRANSLATION:
                float tx = (float) transform.getMxt();
                float ty = (float) transform.getMyt();
                bounds.setBounds(bounds.getMinX() - tx, bounds.getMinY() - ty,
                                 bounds.getMaxX() - tx, bounds.getMaxY() - ty);
                break;
            default:
                try {
                    BaseBounds txbounds = transform.inverseTransform(bounds, bounds);
                    if (txbounds != bounds) {
                        bounds.setBounds(txbounds.getMinX(), txbounds.getMinY(),
                                        txbounds.getMaxX(), txbounds.getMaxY());
                    }
                } catch (NoninvertibleTransformException e) {
                    bounds.makeEmpty();
                }
                break;
        }
    }

    public void updateBounds(float w, float h) {
        this.tw = (int) Math.ceil(w * highestPixelScale);
        this.th = (int) Math.ceil(h * highestPixelScale);
        geometryChanged();
    }

    // Returns true if we are falling behind in rendering (i.e. we
    // have unrendered data at the time of the synch.  This tells
    // the FX layer that it should consider emitting a RESET if it
    // detects a full-canvas clear command even if it looks like it
    // is superfluous.
    public boolean updateRendering(GrowableDataBuffer buf) {
        if (buf.isEmpty()) {
            GrowableDataBuffer.returnBuffer(buf);
            return (this.thebuf != null);
        }
        boolean reset = (buf.peekByte(0) == RESET);
        GrowableDataBuffer retbuf;
        if (reset || this.thebuf == null) {
            retbuf = this.thebuf;
            this.thebuf = buf;
        } else {
            this.thebuf.append(buf);
            retbuf = buf;
        }
        geometryChanged();
        if (retbuf != null) {
            GrowableDataBuffer.returnBuffer(retbuf);
            return true;
        }
        return false;
    }

    class RenderInput extends Effect {
        float x, y, w, h;
        int token;
        GrowableDataBuffer buf;
        Affine2D savedBoundsTx = new Affine2D();

        public RenderInput(int token, GrowableDataBuffer buf,
                           BaseTransform boundsTx, RectBounds rb)
        {
            this.token = token;
            this.buf = buf;
            savedBoundsTx.setTransform(boundsTx);
            this.x = rb.getMinX();
            this.y = rb.getMinY();
            this.w = rb.getWidth();
            this.h = rb.getHeight();
        }

        @Override
        public ImageData filter(FilterContext fctx, BaseTransform transform,
                                Rectangle outputClip, Object renderHelper,
                                Effect defaultInput)
        {
            BaseBounds bounds = getBounds(transform, defaultInput);
            if (outputClip != null) {
                bounds.intersectWith(outputClip);
            }
            Rectangle r = new Rectangle(bounds);
            if (r.width < 1) r.width = 1;
            if (r.height < 1) r.height = 1;
            PrDrawable ret = (PrDrawable) Effect.getCompatibleImage(fctx, r.width, r.height);
            if (ret != null) {
                Graphics g = ret.createGraphics();
                g.setExtraAlpha(globalAlpha);
                g.translate(-r.x, -r.y);
                if (transform != null) {
                    g.transform(transform);
                }
                buf.restore();
                handleRenderOp(token, buf, g, null);
            }
            return new ImageData(fctx, ret, r);
        }

        @Override
        public AccelType getAccelType(FilterContext fctx) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public BaseBounds getBounds(BaseTransform transform, Effect defaultInput) {
            RectBounds ret = new RectBounds(x, y, x + w, y + h);
            if (!transform.equals(savedBoundsTx)) {
                inverseTxBounds(ret, savedBoundsTx);
                txBounds(ret, transform);
            }
            return ret;
        }

        @Override
        public boolean reducesOpaquePixels() {
            return false;
        }

        @Override
        public DirtyRegionContainer getDirtyRegions(Effect defaultInput, DirtyRegionPool regionPool) {
            return null; // Never called
        }

    }

    static class MyBlend extends Blend {
        public MyBlend(Mode mode, Effect bottomInput, Effect topInput) {
            super(mode, bottomInput, topInput);
        }

        @Override
        public Rectangle getResultBounds(BaseTransform transform,
                                         Rectangle outputClip,
                                         ImageData... inputDatas)
        {
            // There is a bug in the ImageData class that means that the
            // outputClip will not be taken into account, so we override
            // here and apply it ourselves.
            Rectangle r = super.getResultBounds(transform, outputClip, inputDatas);
            r.intersectWith(outputClip);
            return r;
        }
    }

    static class EffectInput extends Effect {
        RTTexture tex;
        float pixelscale;

        EffectInput(RTTexture tex) {
            this.tex = tex;
            this.pixelscale = 1.0f;
        }

        public void setPixelScale(float scale) {
            this.pixelscale = scale;
        }

        @Override
        public ImageData filter(FilterContext fctx, BaseTransform transform,
                                Rectangle outputClip, Object renderHelper,
                                Effect defaultInput)
        {
            Filterable f = PrDrawable.create(fctx, tex);
            Rectangle r = new Rectangle(tex.getContentWidth(), tex.getContentHeight());
            f.lock();
            ImageData id = new ImageData(fctx, f, r);
            id.setReusable(true);
            if (pixelscale != 1.0f || !transform.isIdentity()) {
                Affine2D a2d = new Affine2D();
                a2d.scale(1.0f / pixelscale, 1.0f / pixelscale);
                a2d.concatenate(transform);
                id = id.transform(a2d);
            }
            return id;
        }

        @Override
        public AccelType getAccelType(FilterContext fctx) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public BaseBounds getBounds(BaseTransform transform, Effect defaultInput) {
            Rectangle r = new Rectangle(tex.getContentWidth(), tex.getContentHeight());
            return transformBounds(transform, new RectBounds(r));
        }

        @Override
        public boolean reducesOpaquePixels() {
            return false;
        }

        @Override
        public DirtyRegionContainer getDirtyRegions(Effect defaultInput, DirtyRegionPool regionPool) {
            return null; // Never called
        }
    }
}
