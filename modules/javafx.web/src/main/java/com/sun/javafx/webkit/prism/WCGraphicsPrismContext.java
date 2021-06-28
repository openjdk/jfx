/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit.prism;

import com.sun.glass.ui.Screen;
import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.Metrics;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.geom.*;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.sg.prism.*;
import com.sun.javafx.text.TextRun;
import com.sun.prism.*;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Gradient;
import com.sun.prism.paint.ImagePattern;
import com.sun.prism.paint.Paint;
import com.sun.scenario.effect.*;
import com.sun.scenario.effect.impl.prism.PrDrawable;
import com.sun.scenario.effect.impl.prism.PrEffectHelper;
import com.sun.scenario.effect.impl.prism.PrFilterContext;
import com.sun.webkit.graphics.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import static com.sun.scenario.effect.Blend.Mode.*;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.prism.PrRenderer;

class WCGraphicsPrismContext extends WCGraphicsContext {

    public enum Type {
        /**
         * Base context associated with the topmost page buffer.
         * Created and disposed during a single render pass.
         */
        PRIMARY,

        /**
         * A context associated with a dedicated buffer representing
         * a separate render target like canvas, buffered image etc.
         * Its life cycle is not limited to a single render pass.
         */
        DEDICATED
    }

    private final static PlatformLogger log =
            PlatformLogger.getLogger(WCGraphicsPrismContext.class.getName());
    @SuppressWarnings("removal")
    private final static boolean DEBUG_DRAW_CLIP_SHAPE = Boolean.valueOf(
            AccessController.doPrivileged((PrivilegedAction<String>) () ->
            System.getProperty("com.sun.webkit.debugDrawClipShape", "false")));

    Graphics baseGraphics;
    private BaseTransform baseTransform;

    private final List<ContextState> states = new ArrayList<ContextState>();

    private ContextState state = new ContextState();

    // Cache for getPlatformGraphics
    private Graphics cachedGraphics = null;

    private int fontSmoothingType;
    private boolean isRootLayerValid = false;

    WCGraphicsPrismContext(Graphics g) {
        state.setClip(g.getClipRect());
        state.setAlpha(g.getExtraAlpha());
        baseGraphics = g;
        initBaseTransform(g.getTransformNoClone());
    }

    WCGraphicsPrismContext() {
    }

    public Type type() {
        return Type.PRIMARY;
    }

    final void initBaseTransform(BaseTransform t) {
        baseTransform = new Affine3D(t);
        state.setTransform((Affine3D)baseTransform);
    }

    private void resetCachedGraphics() {
        cachedGraphics = null;
    }

    @Override
    public Object getPlatformGraphics() {
        return getGraphics(false);
    }

    @Override
    public boolean isValid() {
        Object platformGraphics = getPlatformGraphics();

        // Ensure that graphics is non-null and of the right type
        if (! (platformGraphics instanceof Graphics)) {
            return false;
        }
        Graphics g = (Graphics)platformGraphics;
        return !g.getResourceFactory().isDisposed();
    }

    Graphics getGraphics(boolean checkClip) {
        if (cachedGraphics == null) {
            Layer l = state.getLayerNoClone();
            cachedGraphics = (l != null)
                    ? l.getGraphics()
                    : baseGraphics;

            ResourceFactory rf = cachedGraphics.getResourceFactory();
            if (!rf.isDisposed()) {
                state.apply(cachedGraphics);
            }

            if (log.isLoggable(Level.FINE)) {
                log.fine("getPlatformGraphics for " + this + " : " +
                         cachedGraphics);
            }
        }

        Rectangle clip = cachedGraphics.getClipRectNoClone();
        return (checkClip && clip!=null && clip.isEmpty())
            ? null
            : cachedGraphics;
    }

    public void saveState()
    {
        state.markAsRestorePoint();
        saveStateInternal();
    }

    private void saveStateInternal()
    {
        states.add(state);
        state = state.clone();
    }

    private void startNewLayer(Layer layer) {
        saveStateInternal();

        // layer has the same bounds as clip, so we have to translate
        Rectangle clip = state.getClipNoClone();

        //left-side (post-) translate.
        //NB! an order of transforms is essential!
        Affine3D newTr = new Affine3D(BaseTransform.getTranslateInstance(
                -clip.x,
                -clip.y));
        newTr.concatenate(state.getTransformNoClone());

        //move clip to (0, 0) - start of texture
        clip.x = 0;
        clip.y = 0;
        //no-clone - no-set!

        Graphics g = getGraphics(true);
        if (g != null && g != baseGraphics) {
            layer.init(g);
        }

        state.setTransform(newTr);
        state.setLayer(layer);

        resetCachedGraphics();
    }

    private void renderLayer(final Layer layer) {
        WCTransform cur = getTransform();

        //translate to (layer.getX(), layer.getY())
        setTransform(new WCTransform(
            1.0, 0.0,
            0.0, 1.0,
            layer.getX(), layer.getY()));

        // composite drawing delegated to the layer rendering
        Graphics g = getGraphics(true);
        if (g != null) {
            layer.render(g);
        }

        //restore transform
        setTransform(cur);
    }

    private void restoreStateInternal() {
        int size = states.size();
        if (size == 0) {
            assert false: "Unbalanced restoreState";
            return;
        }

        Layer layer = state.getLayerNoClone();
        state = states.remove(size - 1);
        if (layer != state.getLayerNoClone()) {
            renderLayer(layer);
            layer.dispose();
            if (log.isLoggable(Level.FINE)) {
                log.fine("Popped layer " + layer);
            }
        } else {
            resetCachedGraphics();
        }
    }

    public void restoreState()
    {
        log.fine("restoring state");
        do {
            restoreStateInternal();
        } while ( !state.isRestorePoint() );
    }

    /**
     *  Renders all layers to the underlaying Graphics, but preserves the
     *  current state and the states stack
     */
    private void flushAllLayers() {
        if (state == null) {
            // context disposed
            return;
        }

        if (isRootLayerValid) {
            log.fine("FlushAllLayers: root layer is valid, skipping");
            return;
        }

        if (log.isLoggable(Level.FINE)) {
            log.fine("FlushAllLayers");
        }

        ContextState currentState = state;

        for (int i = states.size() - 1; i >=0; i--) {
            Layer layer = state.getLayerNoClone();
            state = states.get(i);
            if (layer != state.getLayerNoClone()) {
                renderLayer(layer);
            } else {
                resetCachedGraphics();
            }
        }

        Layer layer = state.getLayerNoClone();
        if (layer != null) {
            renderLayer(layer);
        }

        state = currentState;
        isRootLayerValid = true;
    }


    public void dispose() {
        if (!states.isEmpty()) {
            log.fine("Unbalanced saveState/restoreState");
        }
        for (ContextState state: states) {
            if (state.getLayerNoClone() != null) {
                state.getLayerNoClone().dispose();
            }
        }
        states.clear();

        if (state != null && state.getLayerNoClone() != null) {
            state.getLayerNoClone().dispose();
        }
        state = null;
    }


    public void setClip(WCPath path, boolean isOut) {
        Affine3D tr = new Affine3D(state.getTransformNoClone());
        path.transform(
                tr.getMxx(), tr.getMyx(),
                tr.getMxy(), tr.getMyy(),
                tr.getMxt(), tr.getMyt());
        //path now is in node coordinates, as well as clip

        if (!isOut) {
            WCRectangle pathBounds = path.getBounds();

            // path bounds could be fractional so 'inclusive' rounding
            // is used for determining clip rectangle
            int pixelX = (int) Math.floor(pathBounds.getX());
            int pixelY = (int) Math.floor(pathBounds.getY());
            int pixelW = (int) Math.ceil(pathBounds.getMaxX()) - pixelX;
            int pixelH = (int) Math.ceil(pathBounds.getMaxY()) - pixelY;

            state.clip(new Rectangle(pixelX, pixelY, pixelW, pixelH));
        }

        Rectangle clip = state.getClipNoClone();

        if (isOut) {
            path.addRect(clip.x, clip.y, clip.width, clip.height);
            //Out clip path is always EVENODD.
        }

        path.translate(-clip.x, -clip.y);

        Layer layer = new ClipLayer(
            getGraphics(false), clip, path, type() == Type.DEDICATED);

        startNewLayer(layer);

        if (log.isLoggable(Level.FINE)) {
            log.fine("setClip(WCPath " + path.getID() + ")");
            log.fine("Pushed layer " + layer);
        }
    }

    private Rectangle transformClip(Rectangle localClip) {
        if (localClip==null) {
            return null;
        }

        float[] points = new float[] {
            localClip.x, localClip.y,
            localClip.x + localClip.width, localClip.y,
            localClip.x, localClip.y + localClip.height,
            localClip.x  + localClip.width, localClip.y + localClip.height};
        state.getTransformNoClone().transform(points, 0, points, 0, 4);
        float minX = Math.min(
               points[0], Math.min(
               points[2], Math.min(
               points[4], points[6])));
        float maxX = Math.max(
               points[0], Math.max(
               points[2], Math.max(
               points[4], points[6])));
        float minY = Math.min(
               points[1], Math.min(
               points[3], Math.min(
               points[5], points[7])));
        float maxY = Math.max(
               points[1], Math.max(
               points[3], Math.max(
               points[5], points[7])));
        return new Rectangle(new RectBounds(minX, minY, maxX, maxY));

/* #1 loose rotate
        state.getTransformNoClone().transform(localClip, localClip);
*/
/* #2 problem with negative coordinates
        RectBounds rb = TransformedShape.transformedShape(
            new RoundRectangle2D(localClip.x, localClip.y, localClip.width, localClip.height, 0, 0),
            state.getTransformNoClone()).getBounds();
        return rb.isEmpty()
            ? null
            : new Rectangle(rb);
 */
    }

    private void setClip(Rectangle shape) {
        Affine3D tr = state.getTransformNoClone();
        if (tr.getMxy() == 0 && tr.getMxz() == 0
         && tr.getMyx() == 0 && tr.getMyz() == 0
         && tr.getMzx() == 0 && tr.getMzy() == 0) {
            //There is no rotation here: scale + translation.
            //Fast & easy!
            state.clip(transformClip(shape));
            if (log.isLoggable(Level.FINE)) {
                log.fine("setClip({0})", shape);
            }
            if (DEBUG_DRAW_CLIP_SHAPE) {
                //Draw clip shape
                Rectangle rc = state.getClipNoClone();
                if (rc != null && rc.width >= 2 && rc.height >= 2) {
                    WCTransform cur = getTransform();
                    //translate to (layer.getX(), layer.getY())
                    setTransform(new WCTransform(
                        1.0, 0.0,
                        0.0, 1.0,
                        0.0, 0.0));

                    Graphics g2d = getGraphics(true);
                    if (g2d != null) {
                        float fbase = (float)Math.random();
                        g2d.setPaint(new Color(
                                fbase,
                                1f - fbase,
                                0.5f,
                                0.1f));
                        g2d.setStroke(new BasicStroke());
                        g2d.fillRect(rc.x, rc.y, rc.width, rc.height);

                        g2d.setPaint(new Color(
                                1f - fbase,
                                fbase,
                                0.5f,
                                1f));
                        g2d.drawRect(rc.x, rc.y, rc.width, rc.height);
                    }
                    //restore transform
                    setTransform(cur);
                    state.clip(new Rectangle(rc.x+1, rc.y+1, rc.width-2, rc.height-2));
                }
            }
            if (cachedGraphics != null) {
                cachedGraphics.setClipRect(state.getClipNoClone());
            }
        } else {
            //twisted axis set
            WCPath path = new WCPathImpl();
            path.addRect(shape.x, shape.y, shape.width, shape.height);
            setClip(path, false);
        }
    }

    public void setClip(int cx, int cy, int cw, int ch) {
        setClip(new Rectangle(cx, cy, cw, ch));
    }

    public void setClip(WCRectangle c) {
        setClip(new Rectangle((int)c.getX(), (int)c.getY(),
                              (int)c.getWidth(), (int)c.getHeight()));
    }

    public WCRectangle getClip() {
        Rectangle r = state.getClipNoClone();
        return r == null ? null : new WCRectangle(r.x, r.y, r.width, r.height);
    }

    protected Rectangle getClipRectNoClone() {
        return state.getClipNoClone();
    }

    protected Affine3D getTransformNoClone() {
        return state.getTransformNoClone();
    }

    public void translate(float x, float y) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("translate({0},{1})", new Object[] {x, y});
        }
        state.translate(x, y);
        if (cachedGraphics != null) {
            cachedGraphics.translate(x, y);
        }
    }

    public void scale(float sx, float sy) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("scale(" + sx + " " + sy + ")");
        }
        state.scale(sx, sy);
        if (cachedGraphics != null) {
            cachedGraphics.scale(sx, sy);
        }
    }

    public void rotate(float radians) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("rotate(" + radians + ")");
        }
        state.rotate(radians);
        if (cachedGraphics != null) {
            cachedGraphics.setTransform(state.getTransformNoClone());
        }
    }

    // overriden in WCBufferedContext
    protected boolean shouldRenderRect(float x, float y, float w, float h,
                                       DropShadow shadow, BasicStroke stroke)
    {
        return true;
    }

    // overriden in WCBufferedContext
    protected boolean shouldRenderShape(Shape shape, DropShadow shadow, BasicStroke stroke) {
        return true;
    }

    // overriden in WCBufferedContext
    protected boolean shouldCalculateIntersection() {
        return false;
    }

    @Override
    public void fillRect(final float x, final float y, final float w, final float h, final Color color) {
        if (log.isLoggable(Level.FINE)) {
            String format = "fillRect(%f, %f, %f, %f, %s)";
            log.fine(String.format(format, x, y, w, h, color));
        }
        if (!shouldRenderRect(x, y, w, h, state.getShadowNoClone(), null)) {
            return;
        }
        new Composite() {
            @Override void doPaint(Graphics g) {
                Paint paint = (color != null) ? color : state.getPaintNoClone();
                DropShadow shadow = state.getShadowNoClone();
                // TextureMapperJava::drawSolidColor calls fillRect with perspective
                // projection.
                if (shadow != null || !state.getPerspectiveTransformNoClone().isIdentity()) {
                    final NGRectangle node = new NGRectangle();
                    node.updateRectangle(x, y, w, h, 0, 0);
                    render(g, shadow, paint, null, node);
                } else {
                    g.setPaint(paint);
                    g.fillRect(x, y, w, h);
                }
            }
        }.paint();
    }

    @Override
    public void fillRoundedRect(final float x, final float y, final float w, final float h,
        final float topLeftW, final float topLeftH, final float topRightW, final float topRightH,
        final float bottomLeftW, final float bottomLeftH, final float bottomRightW, final float bottomRightH,
        final Color color)
    {
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format("fillRoundedRect(%f, %f, %f, %f, "
                    + "%f, %f, %f, %f, %f, %f, %f, %f, %s)",
                    x, y, w, h, topLeftW, topLeftH, topRightW, topRightH,
                    bottomLeftW, bottomLeftH, bottomRightW, bottomRightH, color));
        }
        if (!shouldRenderRect(x, y, w, h, state.getShadowNoClone(), null)) {
            return;
        }
        new Composite() {
            @Override void doPaint(Graphics g) {
                // Prism only supports single arcWidth/Height.
                // We work around by calculating average width and height here

                float arcW = (topLeftW + topRightW + bottomLeftW + bottomRightW) / 2;
                float arcH = (topLeftH + topRightH + bottomLeftH + bottomRightH) / 2;

                DropShadow shadow = state.getShadowNoClone();
                if (shadow != null) {
                    final NGRectangle node = new NGRectangle();
                    node.updateRectangle(x, y, w, h, arcW, arcH);
                    render(g, shadow, color, null, node);
                } else {
                    g.setPaint(color);
                    g.fillRoundRect(x, y, w, h, arcW, arcH);
                }
            }
        }.paint();
    }

    @Override
    public void clearRect(final float x, final float y, final float w, final float h) {
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format("clearRect(%f, %f, %f, %f)", x, y, w, h));
        }
        if (shouldCalculateIntersection()) {
            // No intersection is applicable for clearRect.
            return;
        }
        new Composite() {
            @Override void doPaint(Graphics g) {
                g.clearQuad(x, y, x + w, y + h);
            }
        }.paint();
    }

    @Override
    public void setFillColor(Color color) {
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format("setFillColor(%s)", color));
        }
        state.setPaint(color);
    }

    @Override
    public void setFillGradient(WCGradient gradient) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("setFillGradient(" + gradient + ")");
        }
        state.setPaint((Gradient) gradient.getPlatformGradient());
    }

    @Override
    public void setTextMode(boolean fill, boolean stroke, boolean clip) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("setTextMode(fill:" + fill + ",stroke:" + stroke + ",clip:" + clip + ")");
        }
        state.setTextMode(fill, stroke, clip);
    }

    @Override
    public void setFontSmoothingType(int fontSmoothingType) {
        this.fontSmoothingType = fontSmoothingType;
    }

    @Override
    public int getFontSmoothingType() {
        return fontSmoothingType;
    }

    @Override
    public void setStrokeStyle(int style) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("setStrokeStyle({0})", style);
        }
        state.getStrokeNoClone().setStyle(style);
    }

    @Override
    public void setStrokeColor(Color color) {
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format("setStrokeColor(%s)", color));
        }
        state.getStrokeNoClone().setPaint(color);
    }

    @Override
    public void setStrokeWidth(float width) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("setStrokeWidth({0})", new Object[] { width });
        }
        state.getStrokeNoClone().setThickness(width);
    }

    @Override
    public void setStrokeGradient(WCGradient gradient) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("setStrokeGradient(" + gradient + ")");
        }
        state.getStrokeNoClone().setPaint((Gradient) gradient.getPlatformGradient());
    }

    @Override
    public void setLineDash(float offset, float... sizes) {
        if (log.isLoggable(Level.FINE)) {
            StringBuilder s = new StringBuilder("[");
            for (int i=0; i < sizes.length; i++) {
                s.append(sizes[i]).append(',');
            }
            s.append(']');
            log.fine("setLineDash({0},{1}", new Object[] {offset, s});
        }
        state.getStrokeNoClone().setDashOffset(offset);
        if (sizes != null) {
            boolean allZero = true;
            for (int i = 0; i < sizes.length; i++) {
                if (sizes[i] != 0) {
                    allZero = false;
                    break;
                }
            }
            if (allZero) {
                sizes = null;
            }
        }
        state.getStrokeNoClone().setDashSizes(sizes);
    }

    @Override
    public void setLineCap(int lineCap) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("setLineCap(" + lineCap + ")");
        }
        state.getStrokeNoClone().setLineCap(lineCap);
    }

    @Override
    public void setLineJoin(int lineJoin) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("setLineJoin(" + lineJoin + ")");
        }
        state.getStrokeNoClone().setLineJoin(lineJoin);
    }

    @Override
    public void setMiterLimit(float miterLimit) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("setMiterLimit(" + miterLimit + ")");
        }
        state.getStrokeNoClone().setMiterLimit(miterLimit);
    }

    @Override
    public void setShadow(float dx, float dy, float blur, Color color) {
        if (log.isLoggable(Level.FINE)) {
            String format = "setShadow(%f, %f, %f, %s)";
            log.fine(String.format(format, dx, dy, blur, color));
        }
        state.setShadow(createShadow(dx, dy, blur, color));
    }

    @Override
    public void drawPolygon(final WCPath path, final boolean shouldAntialias) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("drawPolygon({0})",
                    new Object[] {shouldAntialias});
        }
        if (!shouldRenderShape(((WCPathImpl)path).getPlatformPath(), null,
                                state.getStrokeNoClone().getPlatformStroke()))
        {
            return;
        }
        new Composite() {
            @Override void doPaint(Graphics g) {
                Path2D p2d = (Path2D) path.getPlatformPath();
                g.setPaint(state.getPaintNoClone());
                g.fill(p2d);
                if (state.getStrokeNoClone().apply(g)) {
                    g.draw(p2d);
                }
            }
        }.paint();
    }

    @Override
    public void drawLine(final int x0, final int y0, final int x1, final int y1) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("drawLine({0}, {1}, {2}, {3})",
                    new Object[] {x0, y0, x1, y1});
        }
        Line2D line = new Line2D(x0, y0, x1, y1);
        if (!shouldRenderShape(line, null, state.getStrokeNoClone().getPlatformStroke())) {
            return;
        }
        new Composite() {
            @Override void doPaint(Graphics g) {
                if (state.getStrokeNoClone().apply(g)) {
                    g.drawLine(x0, y0, x1, y1);
                }
            }
        }.paint();
    }

    @Override
    public void drawPattern(
        final WCImage texture,
        final WCRectangle srcRect,
        final WCTransform patternTransform,
        final WCPoint phase,
        final WCRectangle destRect)
    {
        if (log.isLoggable(Level.FINE)) {
            log.fine("drawPattern({0}, {1}, {2}, {3})",
                    new Object[] {destRect.getIntX(), destRect.getIntY(),
                                  destRect.getIntWidth(),
                                  destRect.getIntHeight()});
        }
        if (!shouldRenderRect(destRect.getX(), destRect.getY(),
                              destRect.getWidth(), destRect.getHeight(), null, null))
        {
            return;
        }
        if (texture != null) {
            new Composite() {
                @Override void doPaint(Graphics g) {
                    Image img = ((PrismImage)texture).getImage();

                    // Create subImage only if srcRect doesn't fit the texture bounds. See RT-20193.
                    if (!srcRect.contains(new WCRectangle(0, 0, texture.getWidth(), texture.getHeight()))) {

                        img = img.createSubImage(srcRect.getIntX(),
                                                 srcRect.getIntY(),
                                                 (int)Math.ceil(srcRect.getWidth()),
                                                 (int)Math.ceil(srcRect.getHeight()));
                    }

                    double m[] = patternTransform.getMatrix();
                    Affine3D at = new Affine3D();
                    at.translate(phase.getX(), phase.getY());
                    at.concatenate(m[0], m[2], m[4], m[1], m[3], m[5]);

                    g.setPaint(new ImagePattern(
                               img,
                               srcRect.getX(), srcRect.getY(),
                               srcRect.getWidth(), srcRect.getHeight(),
                               at, false, false));

                    g.fillRect(destRect.getX(), destRect.getY(),
                               destRect.getWidth(), destRect.getHeight());
                }
            }.paint();
        }
    }

    @Override
    public void drawImage(final WCImage img,
                          final float dstx, final float dsty, final float dstw, final float dsth,
                          final float srcx, final float srcy, final float srcw, final float srch)
    {
        if (log.isLoggable(Level.FINE)){
            log.fine("drawImage(img, dst({0},{1},{2},{3}), " +
                    "src({4},{5},{6},{7}))",
                    new Object[] {dstx, dsty, dstw, dsth,
                                  srcx, srcy, srcw, srch});
        }
        if (!shouldRenderRect(dstx, dsty, dstw, dsth, state.getShadowNoClone(), null)) {
            return;
        }
        if (img instanceof PrismImage) {
            new Composite() {
                @Override void doPaint(Graphics g) {
                    PrismImage pi = (PrismImage) img;
                    DropShadow shadow = state.getShadowNoClone();
                    if (shadow != null) {
                        NGImageView node = new NGImageView();
                        node.setImage(pi.getImage());
                        node.setX(dstx);
                        node.setY(dsty);
                        node.setViewport(srcx, srcy, srcw, srch, dstw, dsth);
                        node.setContentBounds(new RectBounds(dstx, dsty, dstx + dstw, dsty + dsth));
                        render(g, shadow, null, null, node);
                    } else {
                        pi.draw(g,
                                (int) dstx, (int) dsty,
                                (int) (dstx + dstw), (int) (dsty + dsth),
                                (int) srcx, (int) srcy,
                                (int) (srcx + srcw), (int) (srcy + srch));
                    }
                }
            }.paint();
        }
    }

    @Override
    public void drawBitmapImage(final ByteBuffer image, final int x, final int y, final int w, final int h) {
        if (!shouldRenderRect(x, y, w, h, null, null)) {
            return;
        }
        new Composite() {
            @Override void doPaint(Graphics g) {
                ResourceFactory rf = g.getResourceFactory();
                if (rf.isDisposed()) {
                    log.fine("WCGraphicsPrismContext::doPaint skip because device has been disposed");
                    return;
                }
                image.order(ByteOrder.nativeOrder());
                Image img = Image.fromByteBgraPreData(image, w, h);
                Texture txt = rf.createTexture(img, Texture.Usage.STATIC, Texture.WrapMode.REPEAT);
                g.drawTexture(txt, x, y, x + w, y + h, 0, 0, w, h);
                txt.dispose();
            }
        }.paint();
    }

    @Override
    public void drawIcon(WCIcon icon, int x, int y) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("UNIMPLEMENTED drawIcon ({0}, {1})",
                    new Object[] {x, y});
        }
    }

    @Override
    public void drawRect(final int x, final int y, final int w, final int h) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("drawRect({0}, {1}, {2}, {3})",
                    new Object[]{x, y, w, h});
        }
        if (!shouldRenderRect(x, y, w, h,
                              null, state.getStrokeNoClone().getPlatformStroke()))
        {
            return;
        }
        new Composite() {
            @Override void doPaint(Graphics g) {
                Paint c = state.getPaintNoClone();
                if (c != null && c.isOpaque()) {
                    g.setPaint(c);
                    g.fillRect(x, y, w, h);
                }

                if (state.getStrokeNoClone().apply(g)) {
                    g.drawRect(x, y, w, h);
                }
            }
        }.paint();
    }

    @Override
    public void drawString(final WCFont f, final int[] glyphs,
                           final float[] advances, final float x, final float y)
    {
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format(
                    "Drawing %d glyphs @(%.1f, %.1f)",
                    glyphs.length, x, y));
        }
        PGFont font = (PGFont)f.getPlatformFont();
        TextRun gl = TextUtilities.createGlyphList(glyphs, advances, x, y);

        DropShadow shadow = state.getShadowNoClone();
        BasicStroke stroke = state.isTextStroke()
                ? state.getStrokeNoClone().getPlatformStroke()
                : null;

        final FontStrike strike = font.getStrike(getTransformNoClone(), getFontSmoothingType());
        if (shouldCalculateIntersection()) {
            Metrics m = strike.getMetrics();
            gl.setMetrics(m.getAscent(), m.getDescent(), m.getLineGap());
            if (!shouldRenderRect(x, y, gl.getWidth(), gl.getHeight(), shadow, stroke)) {
                return;
            }
        }
        new Composite() {
            @Override void doPaint(Graphics g) {
                Paint paint = state.isTextFill()
                        ? state.getPaintNoClone()
                        : null;
                if (shadow != null) {
                    final NGText span = new NGText();
                    span.setGlyphs(new GlyphList[] {gl});
                    span.setFont(font);
                    span.setFontSmoothingType(fontSmoothingType);
                    render(g, shadow, paint, stroke, span);
                } else {
                    if (paint != null) {
                        g.setPaint(paint);
                        g.drawString(gl, strike, x, y, null, 0, 0);
                    }
                    if (stroke != null) {
                        paint = state.getStrokeNoClone().getPaint();
                        if (paint != null) {
                            g.setPaint(paint);
                            g.setStroke(stroke);
                            g.draw(strike.getOutline(gl, BaseTransform.getTranslateInstance(x, y)));
                        }
                    }
                }
            }
        }.paint();
    }

    @Override public void drawString(WCFont f, String str, boolean rtl,
            int from, int to, float x, float y)
    {
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format(
                    "str='%s' (length=%d), from=%d, to=%d, rtl=%b, @(%.1f, %.1f)",
                    str, str.length(), from, to, rtl, x, y));
        }
        TextLayout layout = TextUtilities.createLayout(
                str.substring(from, to), f.getPlatformFont());
        int count = 0;
        GlyphList[] runs = layout.getRuns();
        for (GlyphList run: runs) {
            count += run.getGlyphCount();
        }

        int[] glyphs = new int[count];
        float[] adv = new float[count];
        count = 0;
        for (GlyphList run: layout.getRuns()) {
            int gc = run.getGlyphCount();
            for (int i = 0; i < gc; i++) {
                glyphs[count] = run.getGlyphCode(i);
                adv[count] = run.getPosX(i + 1) - run.getPosX(i);
                count++;
            }
        }

        // adjust x coordinate (see RT-29908)
        if (rtl) {
            x += (TextUtilities.getLayoutWidth(str.substring(from), f.getPlatformFont()) -
                  layout.getBounds().getWidth());
        } else {
            x += TextUtilities.getLayoutWidth(str.substring(0, from), f.getPlatformFont());
        }
        drawString(f, glyphs, adv, x, y);
    }

    @Override
    public void setComposite(int composite) {
        log.fine("setComposite({0})", composite);
        state.setCompositeOperation(composite);
    }

    @Override
    public void drawEllipse(final int x, final int y, final int w, final int h) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("drawEllipse({0}, {1}, {2}, {3})",
                    new Object[] { x, y, w, h});
        }
        if (!shouldRenderRect(x, y, w, h,
                              null, state.getStrokeNoClone().getPlatformStroke()))
        {
            return;
        }
        new Composite() {
            @Override void doPaint(Graphics g) {
                g.setPaint(state.getPaintNoClone());
                g.fillEllipse(x, y, w, h);
                if (state.getStrokeNoClone().apply(g)) {
                    g.drawEllipse(x, y, w, h);
                }
            }
        }.paint();
    }

    private final static BasicStroke focusRingStroke =
        new BasicStroke(1.1f, BasicStroke.CAP_BUTT,
                         BasicStroke.JOIN_ROUND, 0.0f,
                         new float[] {1.0f}, 0.0f);

    @Override
    public void drawFocusRing(final int x, final int y, final int w, final int h, final Color color) {
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format("drawFocusRing: %d, %d, %d, %d, %s", x, y, w, h, color));
        }
        if (!shouldRenderRect(x, y, w, h, null, focusRingStroke)) {
            return;
        }
        new Composite() {
            @Override void doPaint(Graphics g) {
                g.setPaint(color);
                BasicStroke stroke = g.getStroke();
                g.setStroke(focusRingStroke);
                g.drawRoundRect(x, y, w, h, 4, 4);
                g.setStroke(stroke);
            }
        }.paint();
    }

    public void setAlpha(float alpha) {
        log.fine("setAlpha({0})", alpha);

        state.setAlpha(alpha);

        if (null != cachedGraphics) {
            cachedGraphics.setExtraAlpha(state.getAlpha());
        }
    }

    public float getAlpha() {
        return state.getAlpha();
    }

    @Override public void beginTransparencyLayer(float opacity) {
        TransparencyLayer layer = new TransparencyLayer(
                getGraphics(false), state.getClipNoClone(), opacity);

        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format("beginTransparencyLayer(%s)", layer));
        }

        //[saveStateIntertal] will work as [saveState]
        state.markAsRestorePoint();

        startNewLayer(layer);
    }

    @Override public void endTransparencyLayer() {
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format("endTransparencyLayer(%s)", state.getLayerNoClone()));
        }

        //pair to [startNewLayer] that works as [saveState] call
        restoreState();
    }

    @Override
    public void drawWidget(final RenderTheme theme, final Ref widget, final int x, final int y) {
        WCSize s = theme.getWidgetSize(widget);
        if (!shouldRenderRect(x, y, s.getWidth(), s.getHeight(), null, null)) {
            return;
        }
        new Composite() {
            @Override void doPaint(Graphics g) {
                theme.drawWidget(WCGraphicsPrismContext.this, widget, x, y);
            }
        }.paint();
    }

    @Override
    public void drawScrollbar(final ScrollBarTheme theme, final Ref widget, int x, int y,
                              int pressedPart, int hoveredPart)
    {
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format("drawScrollbar(%s, %s, x = %d, y = %d)", theme, widget, x, y));
        }

        WCSize s = theme.getWidgetSize(widget);
        if (!shouldRenderRect(x, y, s.getWidth(), s.getHeight(), null, null)) {
            return;
        }
        new Composite() {
            @Override void doPaint(Graphics g) {
                theme.paint(WCGraphicsPrismContext.this, widget, x, y, pressedPart, hoveredPart);
            }
        }.paint();
    }

    private static Rectangle intersect(Rectangle what, Rectangle with) {
        if (what == null) {
            return with;
        }
        RectBounds b = what.toRectBounds();
        b.intersectWith(with);
        what.setBounds(b);
        return what;
    }

    private static Color4f createColor4f(Color color) {
        return new Color4f(color.getRed(),
                           color.getGreen(),
                           color.getBlue(),
                           color.getAlpha());
    }

    private DropShadow createShadow(float dx, float dy, float blur, Color color) {
        if (dx == 0f && dy == 0f && blur == 0f) {
            return null;
        }
        DropShadow shadow = new DropShadow();
        shadow.setOffsetX((int) dx);
        shadow.setOffsetY((int) dy);
        shadow.setRadius((blur < 0f) ? 0f : (blur > 127f) ? 127f : blur);
        shadow.setColor(createColor4f(color));
        return shadow;
    }

    private void render(Graphics g, Effect effect, Paint paint, BasicStroke stroke, NGNode node) {
        if (node instanceof NGShape) {
            NGShape shape = (NGShape) node;
            Shape realShape = shape.getShape();
            Paint strokePaint = state.getStrokeNoClone().getPaint();
            if ((stroke != null) && (strokePaint != null)) {
                realShape = stroke.createStrokedShape(realShape);
                shape.setDrawStroke(stroke);
                shape.setDrawPaint(strokePaint);
                shape.setMode((paint == null) ? NGShape.Mode.STROKE : NGShape.Mode.STROKE_FILL);
            } else {
                shape.setMode((paint == null) ? NGShape.Mode.EMPTY : NGShape.Mode.FILL);
            }
            shape.setFillPaint(paint);
            shape.setContentBounds(realShape.getBounds());
        }
        boolean culling = g.hasPreCullingBits();
        g.setHasPreCullingBits(false);
        node.setEffect(effect);
        node.render(g);
        g.setHasPreCullingBits(culling);
    }

    private static final class ContextState {
        private final WCStrokeImpl stroke = new WCStrokeImpl();
        private Rectangle clip;
        private Paint paint;
        private float alpha;

        private boolean textFill = true;
        private boolean textStroke = false;
        private boolean textClip = false;
        private boolean restorePoint = false;

        private DropShadow shadow;
        private Affine3D xform;
        private GeneralTransform3D perspectiveTransform;
        private Layer layer;
        private int compositeOperation;

        private ContextState() {
            clip = null;
            paint = Color.BLACK;
            stroke.setPaint(Color.BLACK);
            alpha = 1.0f;
            xform = new Affine3D();
            perspectiveTransform = new GeneralTransform3D();
            compositeOperation = COMPOSITE_SOURCE_OVER;
        }

        private ContextState(ContextState state) {
            stroke.copyFrom(state.getStrokeNoClone());
            setPaint(state.getPaintNoClone());
            clip = state.getClipNoClone();
            if (clip != null) {
                clip = new Rectangle(clip);
            }
            xform = new Affine3D(state.getTransformNoClone());
            perspectiveTransform = new GeneralTransform3D().set(state.getPerspectiveTransformNoClone());
            setShadow(state.getShadowNoClone());
            setLayer(state.getLayerNoClone());
            setAlpha(state.getAlpha());
            setTextMode(state.isTextFill(), state.isTextStroke(), state.isTextClip());
            setCompositeOperation(state.getCompositeOperation());
        }

        @Override
        protected ContextState clone() {
            return new ContextState(this);
        }

        private void apply(Graphics g) {
            g.setTransform(getTransformNoClone());
            g.setPerspectiveTransform(getPerspectiveTransformNoClone());
            g.setClipRect(getClipNoClone());
            g.setExtraAlpha(getAlpha());
        }

        private int getCompositeOperation() {
            return compositeOperation;
        }

        private void setCompositeOperation(int compositeOperation) {
            this.compositeOperation = compositeOperation;
        }

        private WCStrokeImpl getStrokeNoClone() {
            return stroke;
        }

        private Paint getPaintNoClone() {
            return paint;
        }

        private void setPaint(Paint paint) {
            this.paint = paint;
        }

        private Rectangle getClipNoClone() {
            return clip;
        }

        private Layer getLayerNoClone() {
            return layer;
        }

        private void setLayer(Layer layer) {
            this.layer = layer;
        }

        private void setClip(Rectangle area) {
            clip = area;
        }

        private void clip(Rectangle area) {
            if (null == clip) {
                clip = area;
            } else {
                clip.intersectWith(area);
            }
        }

        private void setAlpha(float alpha) {
            this.alpha = alpha;
        }

        private float getAlpha() {
            return alpha;
        }

        private void setTextMode(boolean fill, boolean stroke, boolean clip) {
            textFill = fill;
            textStroke = stroke;
            textClip = clip;
        }

        private boolean isTextFill() {
            return textFill;
        }

        private boolean isTextStroke() {
            return textStroke;
        }

        private boolean isTextClip() {
            return textClip;
        }

        private void markAsRestorePoint() {
            restorePoint = true;
        }

        private boolean isRestorePoint() {
            return restorePoint;
        }

        private void setShadow(DropShadow shadow) {
            this.shadow = shadow;
        }

        private DropShadow getShadowNoClone() {
            return shadow;
        }

        private Affine3D getTransformNoClone() {
            return xform;
        }

        private GeneralTransform3D getPerspectiveTransformNoClone() {
            return perspectiveTransform;
        }

        private void setTransform(final Affine3D at) {
            this.xform.setTransform(at);
        }

        private void setPerspectiveTransform(final GeneralTransform3D gt) {
            this.perspectiveTransform.set(gt);
        }

        private void concatTransform(Affine3D at) {
            xform.concatenate(at);
        }

        private void translate(double dx, double dy) {
            xform.translate(dx, dy);
        }

        private void scale(double sx, double sy) {
            xform.scale(sx,sy);
        }

        private void rotate(double radians) {
            xform.rotate(radians);
        }
    }

    private abstract static class Layer {
        FilterContext fctx;
        PrDrawable buffer;
        Graphics graphics;
        final Rectangle bounds;
        boolean permanent;

        Layer(Graphics g, Rectangle bounds, boolean permanent) {
            this.bounds = new Rectangle(bounds);
            this.permanent = permanent;

            // avoid creating zero-size drawable, see also RT-21410
            int w = Math.max(bounds.width, 1);
            int h = Math.max(bounds.height, 1);
            fctx = getFilterContext(g);
            if (permanent) {
                ResourceFactory f = GraphicsPipeline.getDefaultResourceFactory();
                if (f != null && !f.isDisposed()) {
                    RTTexture rtt = f.createRTTexture(w, h, Texture.WrapMode.CLAMP_NOT_NEEDED);
                    rtt.makePermanent();
                    buffer = ((PrRenderer)Renderer.getRenderer(fctx)).createDrawable(rtt);
                } else {
                    log.fine("Layer :: cannot construct RTT because device disposed or not ready");
                    fctx = null;
                    buffer = null;
                }
            } else {
                buffer = (PrDrawable) Effect.getCompatibleImage(fctx, w, h);
            }
        }

        Graphics getGraphics() {
            if (graphics == null && buffer != null) {
                graphics = buffer.createGraphics();
            }
            return graphics;
        }

        abstract void init(Graphics g);

        abstract void render(Graphics g);

        private void dispose() {
            if (buffer != null) {
                if (permanent) {
                    buffer.flush(); // releases the resource
                } else {
                    Effect.releaseCompatibleImage(fctx, buffer);
                }
                fctx = null;
                buffer = null;
            }
        }

        private double getX() { return (double) bounds.x; }
        private double getY() { return (double) bounds.y; }
    }

    private final class TransparencyLayer extends Layer {
        private final float opacity;

        private TransparencyLayer(Graphics g, Rectangle bounds, float opacity) {
            super(g, bounds, false);
            this.opacity = opacity;
        }

        @Override void init(Graphics g) {
            state.setCompositeOperation(COMPOSITE_SOURCE_OVER);
        }

        @Override void render(Graphics g) {
            new Composite() {
                @Override void doPaint(Graphics g) {
                    float op = g.getExtraAlpha();
                    g.setExtraAlpha(opacity);
                    Affine3D tx = new Affine3D(g.getTransformNoClone());
                    g.setTransform(BaseTransform.IDENTITY_TRANSFORM);
                    g.drawTexture(buffer.getTextureObject(),
                            bounds.x, bounds.y, bounds.width, bounds.height);
                    g.setTransform(tx);
                    g.setExtraAlpha(op);
                }
            }.paint(g);
        }

        @Override public String toString() {
            return String.format("TransparencyLayer[%d,%d + %dx%d, opacity %.2f]",
                bounds.x, bounds.y, bounds.width, bounds.height, opacity);
        }
    }

    private static final class ClipLayer extends Layer {
        private final WCPath normalizedToClipPath;
        private boolean srcover;

        private ClipLayer(Graphics g, Rectangle bounds, WCPath normalizedToClipPath,
                          boolean permanent)
        {
            super(g, bounds, permanent);
            this.normalizedToClipPath = normalizedToClipPath;
            srcover = true;
        }

        @Override void init(Graphics g) {
            RTTexture texture = null;
            ReadbackGraphics readbackGraphics = null;
            try {
                readbackGraphics = (ReadbackGraphics) g;
                texture = readbackGraphics.readBack(bounds);
                getGraphics().drawTexture(texture, 0, 0, bounds.width, bounds.height);
            } finally {
                if (readbackGraphics != null && texture != null) {
                    readbackGraphics.releaseReadBackBuffer(texture);
                }
            }
            srcover = false;
        }

        @Override void render(Graphics g) {
            Path2D p2d = ((WCPathImpl)normalizedToClipPath).getPlatformPath();

            // render normalizedToClipPath to a drawable
            PrDrawable bufferImg = (PrDrawable) Effect.getCompatibleImage(
                    fctx, bounds.width, bounds.height);
            Graphics bufferGraphics = bufferImg.createGraphics();

            bufferGraphics.setPaint(Color.BLACK);
            bufferGraphics.fill(p2d);

            // blend buffer and clipImg onto |g|
            if (g instanceof MaskTextureGraphics && ! (g instanceof PrinterGraphics)) {
                MaskTextureGraphics mg = (MaskTextureGraphics) g;
                if (srcover) {
                    mg.drawPixelsMasked(buffer.getTextureObject(),
                                        bufferImg.getTextureObject(),
                                        bounds.x, bounds.y, bounds.width, bounds.height,
                                        0, 0, 0, 0);
                } else {
                    mg.maskInterpolatePixels(buffer.getTextureObject(),
                                             bufferImg.getTextureObject(),
                                             bounds.x, bounds.y, bounds.width, bounds.height,
                                             0, 0, 0, 0);
                }
            } else {
                Blend blend = new Blend(Blend.Mode.SRC_IN,
                        new PassThrough(bufferImg, bounds.width, bounds.height),
                        new PassThrough(buffer, bounds.width, bounds.height));
                Affine3D tx = new Affine3D(g.getTransformNoClone());
                g.setTransform(BaseTransform.IDENTITY_TRANSFORM);
                PrEffectHelper.render(blend, g, bounds.x, bounds.y, null);
                g.setTransform(tx);
            }

            Effect.releaseCompatibleImage(fctx, bufferImg);
        }

        @Override public String toString() {
            return String.format("ClipLayer[%d,%d + %dx%d, path %s]",
                    bounds.x, bounds.y, bounds.width, bounds.height,
                    normalizedToClipPath);
        }
    }

    private abstract class Composite {
        abstract void doPaint(Graphics g);

        void paint() {
            paint(getGraphics(true));
        }

        void paint(Graphics g) {
            if (g != null) {
                CompositeMode oldCompositeMode = g.getCompositeMode();
                switch (state.getCompositeOperation()) {
                    // decode operations that don't require Blend first
                    case COMPOSITE_COPY:
                        g.setCompositeMode(CompositeMode.SRC);
                        doPaint(g);
                        g.setCompositeMode(oldCompositeMode);
                        break;
                    case COMPOSITE_SOURCE_OVER:
                        g.setCompositeMode(CompositeMode.SRC_OVER);
                        doPaint(g);
                        g.setCompositeMode(oldCompositeMode);
                        break;
                    default:
                        // other operations require usage of Blend
                        blend(g);
                        break;
                }
                isRootLayerValid = false;
            }
        }

        private void blend(Graphics g) {
            FilterContext fctx = getFilterContext(g);
            PrDrawable dstImg = null;
            PrDrawable srcImg = null;
            ReadbackGraphics readBackGraphics = null;
            RTTexture texture = null;
            Rectangle clip = state.getClipNoClone();
            WCImage image = getImage();
            try {
                if (image != null && image instanceof PrismImage) {
                    // blending on canvas
                    dstImg = (PrDrawable) Effect.getCompatibleImage(fctx, clip.width, clip.height);
                    Graphics dstG = dstImg.createGraphics();
                    state.apply(dstG);
                    ((PrismImage) image).draw(dstG,
                            0, 0, clip.width, clip.height,
                            clip.x, clip.y, clip.width, clip.height);
                } else {
                    // blending on page
                    readBackGraphics = (ReadbackGraphics) g;
                    texture = readBackGraphics.readBack(clip);
                    dstImg = PrDrawable.create(fctx, texture);
                }

                srcImg = (PrDrawable) Effect.getCompatibleImage(fctx, clip.width, clip.height);
                Graphics srcG = srcImg.createGraphics();
                state.apply(srcG);
                doPaint(srcG);

                g.clear();
                PrEffectHelper.render(createEffect(dstImg, srcImg, clip.width, clip.height), g, 0, 0, null);

            } finally {
                if (srcImg != null) {
                    Effect.releaseCompatibleImage(fctx, srcImg);
                }
                if (dstImg != null) {
                    if (readBackGraphics != null && texture != null) {
                        readBackGraphics.releaseReadBackBuffer(texture);
                    } else {
                        Effect.releaseCompatibleImage(fctx, dstImg);
                    }
                }
            }
        }

        // provides some syntax sugar for createEffect()
        private Effect createBlend(Blend.Mode mode,
                                   PrDrawable dstImg,
                                   PrDrawable srcImg,
                                   int width,
                                   int height)
        {
            return new Blend(
                    mode,
                    new PassThrough(dstImg, width, height),
                    new PassThrough(srcImg, width, height));
        }

        private Effect createEffect(PrDrawable dstImg,
                                    PrDrawable srcImg,
                                    int width,
                                    int height)
        {
            switch (state.getCompositeOperation()) {
                case COMPOSITE_CLEAR: // same as xor
                case COMPOSITE_XOR:
                    return new Blend(
                            SRC_OVER,
                            createBlend(SRC_OUT, dstImg, srcImg, width, height),
                            createBlend(SRC_OUT, srcImg, dstImg, width, height)
                    );
                case COMPOSITE_SOURCE_IN:
                    return createBlend(SRC_IN, dstImg, srcImg, width, height);
                case COMPOSITE_SOURCE_OUT:
                    return createBlend(SRC_OUT, dstImg, srcImg, width, height);
                case COMPOSITE_SOURCE_ATOP:
                    return createBlend(SRC_ATOP, dstImg, srcImg, width, height);
                case COMPOSITE_DESTINATION_OVER:
                    return createBlend(SRC_OVER, srcImg, dstImg, width, height);
                case COMPOSITE_DESTINATION_IN:
                    return createBlend(SRC_IN, srcImg, dstImg, width, height);
                case COMPOSITE_DESTINATION_OUT:
                    return createBlend(SRC_OUT, srcImg, dstImg, width, height);
                case COMPOSITE_DESTINATION_ATOP:
                    return createBlend(SRC_ATOP, srcImg, dstImg, width, height);
                case COMPOSITE_HIGHLIGHT:
                    return createBlend(ADD, dstImg, srcImg, width, height);
                default:
                    return createBlend(SRC_OVER, dstImg, srcImg, width, height);
            }
        }
    }

    private static final class PassThrough extends Effect {
        private final PrDrawable img;
        private final int width;
        private final int height;

        private PassThrough(PrDrawable img, int width, int height) {
            this.img = img;
            this.width = width;
            this.height = height;
        }

        @Override public ImageData filter(
                FilterContext fctx,
                BaseTransform transform,
                Rectangle outputClip,
                Object renderHelper,
                Effect defaultInput) {
            // We have an unpaired lock() here, because unlocking is done
            // internally by ImageData. See RT-33625 for details.
            img.lock();
            ImageData imgData = new ImageData(fctx, img, new Rectangle(
                                              (int) transform.getMxt(),
                                              (int) transform.getMyt(),
                                              width, height));
            imgData.setReusable(true);
            return imgData;
        }

        @Override public RectBounds getBounds(
                BaseTransform transform,
                Effect defaultInput) {
            return null;
        }

        @Override public AccelType getAccelType(FilterContext fctx) {
            return AccelType.INTRINSIC;
        }

        @Override
        public boolean reducesOpaquePixels() {
            return false;
        }

        @Override
        public DirtyRegionContainer getDirtyRegions(Effect defaultInput, DirtyRegionPool regionPool) {
            return null;
        }
    }

    private static FilterContext getFilterContext(Graphics g) {
        Screen screen = g.getAssociatedScreen();
        if (screen == null) {
            ResourceFactory factory = g.getResourceFactory();
            return PrFilterContext.getPrinterContext(factory);
        } else {
            return PrFilterContext.getInstance(screen);
        }
    }

    @Override
    public void strokeArc(final int x, final int y, final int w, final int h,
                          final int startAngle, final int angleSpan)
    {
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format("strokeArc(%d, %d, %d, %d, %d, %d)",
                                   x, y, w, h, startAngle, angleSpan));
        }
        Arc2D arc = new Arc2D(x, y, w, h, startAngle, angleSpan, Arc2D.OPEN);
        if (state.getStrokeNoClone().isApplicable() &&
            !shouldRenderShape(arc, null, state.getStrokeNoClone().getPlatformStroke()))
        {
            return;
        }
        new Composite() {
            @Override void doPaint(Graphics g) {
                if (state.getStrokeNoClone().apply(g)) {
                    g.draw(arc);
                }
            }
        }.paint();
    }

    @Override
    public WCImage getImage() {
        return null;
    }

    @Override
    public void strokeRect(final float x, final float y, final float w, final float h,
                           final float lineWidth) {
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format("strokeRect_FFFFF(%f, %f, %f, %f, %f)",
                                   x, y, w, h, lineWidth));
        }
        BasicStroke stroke = new BasicStroke(
            lineWidth,
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER,
            Math.max(1.0f, lineWidth),
            state.getStrokeNoClone().getDashSizes(),
            state.getStrokeNoClone().getDashOffset());

        if (!shouldRenderRect(x, y, w, h, null, stroke)) {
            return;
        }
        new Composite() {
            @Override void doPaint(Graphics g) {
                g.setStroke(stroke);
                Paint paint = state.getStrokeNoClone().getPaint();
                if (paint == null) {
                    paint = state.getPaintNoClone();
                }
                g.setPaint(paint);
                g.drawRect(x, y, w, h);
            }
        }.paint();
    }

    @Override
    public void strokePath(final WCPath path) {
        log.fine("strokePath");
        if (path != null) {
            final BasicStroke stroke = state.getStrokeNoClone().getPlatformStroke();
            final DropShadow shadow = state.getShadowNoClone();
            final Path2D p2d = (Path2D)path.getPlatformPath();

            if ((stroke == null && shadow == null) ||
                !shouldRenderShape(p2d, shadow, stroke))
            {
                return;
            }
            new Composite() {
                @Override void doPaint(Graphics g) {
                    if (shadow != null) {
                        final NGPath node = new NGPath();
                        node.updateWithPath2d(p2d);
                        render(g, shadow, null, stroke, node);
                    } else if (stroke != null) {
                        Paint paint = state.getStrokeNoClone().getPaint();
                        if (paint == null) {
                            paint = state.getPaintNoClone();
                        }
                        g.setPaint(paint);
                        g.setStroke(stroke);
                        g.draw(p2d);
                    }
                }
            }.paint();
        }
    }

    @Override
    public void fillPath(final WCPath path) {
        log.fine("fillPath");
        if (path != null) {
            if (!shouldRenderShape(((WCPathImpl)path).getPlatformPath(),
                                   state.getShadowNoClone(), null))
            {
                return;
            }
            new Composite() {
                @Override void doPaint(Graphics g) {
                    Path2D p2d = (Path2D) path.getPlatformPath();
                    Paint paint = state.getPaintNoClone();
                    DropShadow shadow = state.getShadowNoClone();
                    if (shadow != null) {
                        final NGPath node = new NGPath();
                        node.updateWithPath2d(p2d);
                        render(g, shadow, paint, null, node);
                    } else {
                        g.setPaint(paint);
                        g.fill(p2d);
                    }
                }
            }.paint();
        }
    }

    public void setPerspectiveTransform(WCTransform tm) {
        final GeneralTransform3D at = new GeneralTransform3D().set(tm.getMatrix());
        state.setPerspectiveTransform(at);
        resetCachedGraphics();
    }

    public void setTransform(WCTransform tm) {
        final double m[] = tm.getMatrix();
        final Affine3D at = new Affine3D(new Affine2D(m[0], m[1], m[2], m[3], m[4], m[5]));
        if (state.getLayerNoClone() == null) {
            at.preConcatenate(baseTransform);
        }
        state.setTransform(at);
        resetCachedGraphics();
    }

    public WCTransform getTransform() {
        Affine3D xf = state.getTransformNoClone();
        return new WCTransform(xf.getMxx(), xf.getMyx(),
                               xf.getMxy(), xf.getMyy(),
                               xf.getMxt(), xf.getMyt());
    }

    public void concatTransform(WCTransform tm) {
        double m[] = tm.getMatrix();
        Affine3D at = new Affine3D(new Affine2D(m[0], m[1], m[2], m[3], m[4], m[5]));
        state.concatTransform(at);
        resetCachedGraphics();
    }

    @Override
    public void flush() {
        if (!isValid()) {
            log.fine("WCGraphicsPrismContext::flush : GC is invalid");
            return;
        }
        flushAllLayers();
    }

    @Override
    public WCGradient createLinearGradient(WCPoint p1, WCPoint p2) {
        return new WCLinearGradient(p1, p2);
    }

    @Override
    public WCGradient createRadialGradient(WCPoint p1, float r1, WCPoint p2, float r2) {
        return new WCRadialGradient(p1, r1, p2, r2);
    }
}
