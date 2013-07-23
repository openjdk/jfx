/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.webkit.prism;

import com.sun.javafx.font.FontStrike;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.geom.*;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.sg.prism.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.javafx.webkit.prism.TextUtilities.getLayoutWidth;
import static com.sun.scenario.effect.Blend.Mode.*;

class WCGraphicsPrismContext extends WCGraphicsContext {

    private final static Logger log =
        Logger.getLogger(WCGraphicsPrismContext.class.getName());

    private Graphics baseGraphics;

    private final List<ContextState> states = new ArrayList<ContextState>();

    private ContextState state = new ContextState();

    private boolean isInitialized;

    // Cache for getPlatformGraphics
    private Graphics cachedGraphics = null;

    private int fontSmoothingType;

    WCGraphicsPrismContext(Graphics g) {
        init(g, true);
    }

    WCGraphicsPrismContext() {
    }

    final void init(Graphics g, boolean inherit) {
        if (isInitialized) {
            return;
        }
        if (g != null && inherit) {
            state.setClip(g.getClipRect());
            state.setTransform(new Affine3D(g.getTransformNoClone()));
            state.setAlpha(g.getExtraAlpha());
        }
        baseGraphics = g;
        isInitialized = true;
    }

    private void resetCachedGraphics() {
        cachedGraphics = null;
    }

    @Override
    public Object getPlatformGraphics() {
        return getGraphics(false);
    }

    Graphics getGraphics(boolean checkClip) {
        if (cachedGraphics == null) {
            Layer l = state.getLayerNoClone();
            cachedGraphics = (l != null)
                    ? l.getGraphics()
                    : baseGraphics;

            state.apply(cachedGraphics);

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

        layer.dispose();
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
                getGraphics(false), clip, path);

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
                log.log(Level.FINE, "setClip({0})", shape);
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

    public void translate(float x, float y) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "translate({0},{1})", new Object[] {x, y});
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

    @Override
    public void fillRect(final float x, final float y, final float w, final float h, final Integer rgba) {
        if (log.isLoggable(Level.FINE)) {
            String format = (rgba != null)
                    ? "fillRect(%f, %f, %f, %f, 0x%x)"
                    : "fillRect(%f, %f, %f, %f, null)";
            log.fine(String.format(format, x, y, w, h, rgba));
        }

        new Composite() {
            @Override void doPaint(Graphics g) {
                Paint paint = (rgba != null) ? createColor(rgba) : state.getPaintNoClone();
                DropShadow shadow = state.getShadowNoClone();
                if (shadow != null) {
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

    @Override public void fillRoundedRect(final float x, final float y, final float w, final float h,
            final float topLeftW, final float topLeftH, final float topRightW, final float topRightH,
            final float bottomLeftW, final float bottomLeftH, final float bottomRightW, final float bottomRightH,
            final int rgba) {
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format("fillRoundedRect(%f, %f, %f, %f, "
                    + "%f, %f, %f, %f, %f, %f, %f, %f, 0x%x)",
                    x, y, w, h, topLeftW, topLeftH, topRightW, topRightH,
                    bottomLeftW, bottomLeftH, bottomRightW, bottomRightH, rgba));
        }

        new Composite() {
            @Override void doPaint(Graphics g) {
                // Prism only supports single arcWidth/Height.
                // We work around by calculating average width and height here

                float arcW = (topLeftW + topRightW + bottomLeftW + bottomRightW) / 2;
                float arcH = (topLeftH + topRightH + bottomLeftH + bottomRightH) / 2;

                Paint paint = createColor(rgba);
                DropShadow shadow = state.getShadowNoClone();
                if (shadow != null) {
                    final NGRectangle node = new NGRectangle();
                    node.updateRectangle(x, y, w, h, arcW, arcH);
                    render(g, shadow, paint, null, node);
                } else {
                    g.setPaint(paint);
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

        new Composite() {
            @Override void doPaint(Graphics g) {
                g.clearQuad(x, y, x + w, y + h);
            }
        }.paint();
    }

    @Override
    public void setFillColor(int rgba) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, String.format("setFillColor(0x%x)", rgba));
        }
        state.setPaint(createColor(rgba));
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
            log.log(Level.FINE, "setStrokeStyle({0})", style);
        }
        state.getStrokeNoClone().setStyle(style);
    }

    @Override
    public void setStrokeColor(int rgba) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, String.format("setStrokeColor(0x%x)", rgba));
        }
        state.getStrokeNoClone().setPaint(createColor(rgba));
    }

    @Override
    public void setStrokeWidth(float width) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "setStrokeWidth({0})", new Object[] { width });
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
            log.log(Level.FINE, "setLineDash({0},{1}", new Object[] {offset, s});
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
    public void setShadow(float dx, float dy, float blur, int rgba) {
        if (log.isLoggable(Level.FINE)) {
            String format = "setShadow(%f, %f, %f, 0x%x)";
            log.fine(String.format(format, dx, dy, blur, rgba));
        }
        state.setShadow(createShadow(dx, dy, blur, rgba));
    }

    @Override
    public void drawPolygon(final float [] pnts, final boolean shouldAntialias) {
        if (log.isLoggable(Level.FINE)) {
            StringBuilder s = new StringBuilder("[");
            for (int i=0; i < pnts.length; i++) {
                s.append(pnts[i]).append(',');
            }
            s.append(']');
            log.log(Level.FINE, "drawPolygon({0},{1})",
                    new Object[] { s, shouldAntialias});
        }

        new Composite() {
            @Override void doPaint(Graphics g) {
                Path2D p2d = new Path2D();
                if (pnts != null && pnts.length != 0 && pnts.length % 2 == 0) {
                    p2d.moveTo(pnts[0], pnts[1]);
                    for (int i = 1; i < pnts.length / 2; i++) {
                        float px = pnts[i * 2 + 0];
                        float py = pnts[i * 2 + 1];
                        p2d.lineTo(px, py);
                    }
                    p2d.closePath();
                }
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
            log.log(Level.FINE, "drawLine({0}, {1}, {2}, {3})",
                    new Object[] {x0, y0, x1, y1});
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
            log.log(Level.FINE, "drawPattern({0}, {1}, {2}, {3})",
                    new Object[] {destRect.getIntX(), destRect.getIntY(),
                                  destRect.getIntWidth(),
                                  destRect.getIntHeight()});
        }

        if (texture != null) {
            new Composite() {
                @Override void doPaint(Graphics g) {
                    // The handling of pattern transform is modeled after the WebKit
                    // ImageCG.cpp's Image::drawPattern()
                    float adjustedX = phase.getX()
                            + srcRect.getX() * (float) patternTransform.getMatrix()[0];
                    float adjustedY = phase.getY()
                            + srcRect.getY() * (float) patternTransform.getMatrix()[3];
                    float scaledTileWidth =
                            srcRect.getWidth() * (float) patternTransform.getMatrix()[0];
                    float scaledTileHeight =
                            srcRect.getHeight() * (float) patternTransform.getMatrix()[3];

                    Image img = ((PrismImage)texture).getImage();

                    // Create subImage only if srcRect doesn't fit the texture bounds. See RT-20193.
                    if (!srcRect.contains(new WCRectangle(0, 0, texture.getWidth(), texture.getHeight()))) {

                        img = img.createSubImage(srcRect.getIntX(),
                                                 srcRect.getIntY(),
                                                 (int)Math.ceil(srcRect.getWidth()),
                                                 (int)Math.ceil(srcRect.getHeight()));
                    }
                    g.setPaint(new ImagePattern(
                               img,
                               adjustedX, adjustedY,
                               scaledTileWidth, scaledTileHeight,
                               false, false));

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
            log.log(Level.FINE, "drawImage(img, dst({0},{1},{2},{3}), " +
                    "src({4},{5},{6},{7}))",
                    new Object[] {dstx, dsty, dstw, dsth,
                                  srcx, srcy, srcw, srch});
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
        new Composite() {
            @Override void doPaint(Graphics g) {
                image.order(ByteOrder.nativeOrder());
                Image img = Image.fromByteBgraPreData(image, w, h);
                ResourceFactory rf = g.getResourceFactory();
                Texture txt = rf.createTexture(img, Texture.Usage.STATIC, Texture.WrapMode.REPEAT);
                g.drawTexture(txt, x, y, x + w, y + h, 0, 0, w, h);
                txt.dispose();
            }
        }.paint();
    }

    @Override
    public void drawIcon(WCIcon icon, int x, int y) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "UNIMPLEMENTED drawIcon ({0}, {1})",
                    new Object[] {x, y});
        }
    }

    public void drawRect(final int x, final int y, final int w, final int h) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "drawRect({0}, {1}, {2}, {3})",
                    new Object[]{x, y, w, h});
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

    @Override public void drawString(final WCFont f, final int[] glyphs,
            final float[] advances, final float x, final float y)
    {
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format(
                    "Drawing %d glyphs @(%.1f, %.1f)",
                    glyphs.length, x, y));
        }
        new Composite() {
            @Override void doPaint(Graphics g) {
                PGFont font = (PGFont) f.getPlatformFont();
                GlyphList gl = TextUtilities.createGlyphList(glyphs, advances, x, y);
                Paint paint = state.isTextFill()
                        ? state.getPaintNoClone()
                        : null;
                BasicStroke stroke = state.isTextStroke()
                        ? state.getStrokeNoClone().getPlatformStroke()
                        : null;
                DropShadow shadow = state.getShadowNoClone();
                if (shadow != null) {
                    final NGText span = new NGText();
                    span.setGlyphs(new GlyphList[] {gl});
                    span.setFont(font);
                    span.setFontSmoothingType(fontSmoothingType);
                    render(g, shadow, paint, stroke, span);
                } else {
                    FontStrike strike = font.getStrike(g.getTransformNoClone(), fontSmoothingType);
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
            x += (getLayoutWidth(str.substring(from), f.getPlatformFont()) -
                  layout.getBounds().getWidth());
        } else {
            x += getLayoutWidth(str.substring(0, from), f.getPlatformFont());
        }
        drawString(f, glyphs, adv, x, y);
    }

    public void setComposite(int composite) {
        log.log(Level.FINE, "setComposite({0})", composite);
        state.setCompositeOperation(composite);
    }

    public void drawEllipse(final int x, final int y, final int w, final int h) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "drawEllipse({0}, {1}, {2}, {3})",
                    new Object[] { x, y, w, h});
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

    public void drawFocusRing(final int x, final int y, final int w, final int h, final int rgba) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE,
                    String.format("drawFocusRing: %d, %d, %d, %d, 0x%x",
                                  x, y, w, h, rgba));
        }

        new Composite() {
            @Override void doPaint(Graphics g) {
                g.setPaint(createColor(rgba));
                BasicStroke stroke = g.getStroke();
                g.setStroke(focusRingStroke);
                g.drawRoundRect(x, y, w, h, 4, 4);
                g.setStroke(stroke);
            }
        }.paint();
    }

    public void setAlpha(float alpha) {
        log.log(Level.FINE, "setAlpha({0})", alpha);

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
        new Composite() {
            @Override void doPaint(Graphics g) {
                theme.drawWidget(WCGraphicsPrismContext.this, widget, x, y);
            }
        }.paint();
    }

    @Override
    public void drawScrollbar(ScrollBarTheme theme, Ref widget, int x, int y,
                              int pressedPart, int hoveredPart)
    {
        theme.paint(this, widget, x, y, pressedPart, hoveredPart);
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

    static Color createColor(int rgba) {
        float a = (0xFF & (rgba >> 24)) / 255.0f;
        float r = (0xFF & (rgba >> 16)) / 255.0f;
        float g = (0xFF & (rgba >> 8)) / 255.0f;
        float b = (0xFF & (rgba)) / 255.0f;
        return new Color(r, g, b, a);
    }

    private static Color4f createColor4f(int rgba) {
        float a = (0xFF & (rgba >> 24)) / 255.0f;
        float r = (0xFF & (rgba >> 16)) / 255.0f;
        float g = (0xFF & (rgba >> 8)) / 255.0f;
        float b = (0xFF & (rgba)) / 255.0f;
        return new Color4f(r, g, b, a);
    }

    private DropShadow createShadow(float dx, float dy, float blur, int rgba) {
        if (dx == 0f && dy == 0f && blur == 0f) {
            return null;
        }
        DropShadow shadow = new DropShadow();
        shadow.setOffsetX((int) dx);
        shadow.setOffsetY((int) dy);
        shadow.setRadius((blur < 0f) ? 0f : (blur > 127f) ? 127f : blur);
        shadow.setColor(createColor4f(rgba));
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
        private Layer layer;
        private int compositeOperation;

        private ContextState() {
            clip = null;
            paint = Color.BLACK;
            stroke.setPaint(Color.BLACK);
            alpha = 1.0f;
            xform = new Affine3D();
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
            //TODO: Verify if we need to apply more properties from state
            g.setTransform(getTransformNoClone());
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

        private void setTransform(final Affine3D at) {
            this.xform.setTransform(at);
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
        final FilterContext fctx;
        final PrDrawable buffer;
        Graphics graphics;
        final Rectangle bounds;

        Layer(Graphics g, Rectangle bounds) {
            this.bounds = new Rectangle(bounds);

            // avoid creating zero-size drawable, see also RT-21410
            int w = Math.max(bounds.width, 1);
            int h = Math.max(bounds.height, 1);
            fctx = PrFilterContext.getInstance(g.getAssociatedScreen());
            buffer = (PrDrawable) Effect.getCompatibleImage(fctx, w, h);
        }

        Graphics getGraphics() {
            if (graphics == null) {
                graphics = buffer.createGraphics();
            }
            return graphics;
        }

        abstract void init(Graphics g);

        abstract void render(Graphics g);

        private void dispose() {
            Effect.releaseCompatibleImage(fctx, buffer);
        }

        private double getX() { return (double) bounds.x; }
        private double getY() { return (double) bounds.y; }
    }

    private final class TransparencyLayer extends Layer {
        private final float opacity;

        private TransparencyLayer(Graphics g, Rectangle bounds, float opacity) {
            super(g, bounds);
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
                    g.drawTexture(buffer.getTextureObject(),
                            0, 0, bounds.width, bounds.height);
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

        private ClipLayer(Graphics g, Rectangle bounds, WCPath normalizedToClipPath) {
            super(g, bounds);
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
            if (g instanceof MaskTextureGraphics) {
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
                PrEffectHelper.render(blend, g, 0, 0, null);
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
            }
        }

        private void blend(Graphics g) {
            FilterContext fctx = PrFilterContext.getInstance(g.getAssociatedScreen());
            PrDrawable dstImg = null;
            PrDrawable srcImg = null;
            ReadbackGraphics readBackGraphics = null;
            RTTexture texture = null;
            Rectangle clip = state.getClipNoClone();
            WCImage image = getImage();
            try {
                if (image != null && image instanceof PrismImage) {
                    // blending on canvas
                    dstImg = (PrDrawable) Effect.createCompatibleImage(fctx, clip.width, clip.height);
                    Graphics dstG = dstImg.createGraphics();
                    ((PrismImage) image).draw(dstG,
                            0, 0, clip.width, clip.height,
                            clip.x, clip.y, clip.width, clip.height);
                } else {
                    // blending on page
                    readBackGraphics = (ReadbackGraphics) g;
                    texture = readBackGraphics.readBack(clip);
                    dstImg = PrDrawable.create(fctx, texture);
                }

                srcImg = (PrDrawable) Effect.createCompatibleImage(fctx, clip.width, clip.height);
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
            return new ImageData(fctx, img, new Rectangle(
                    (int) transform.getMxt(), (int) transform.getMyt(),
                    width, height));
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

    @Override public void strokeArc(final int x, final int y, final int w, final int h,
                                    final int startAngle, final int angleSpan)
    {
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format("strokeArc(%d, %d, %d, %d, %d, %d)",
                                   x, y, w, h, startAngle, angleSpan));
        }
        new Composite() {
            @Override void doPaint(Graphics g) {
                if (state.getStrokeNoClone().apply(g)) {
                    g.draw(new Arc2D(x, y, w, h, startAngle, angleSpan, Arc2D.OPEN));
                }
            }
        }.paint();
    }

    public WCImage getImage() {
        return null;
    }

    public void strokeRect(final float x, final float y, final float w, final float h,
                           final float lineWidth) {
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format("strokeRect_FFFFF(%f, %f, %f, %f, %f)",
                                   x, y, w, h, lineWidth));
        }

        new Composite() {
            @Override void doPaint(Graphics g) {
                g.setStroke(new BasicStroke(
                        lineWidth,
                        BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER,
                        Math.max(1.0f, lineWidth)));
                Paint paint = state.getStrokeNoClone().getPaint();
                if (paint == null) {
                    paint = state.getPaintNoClone();
                }
                g.setPaint(paint);
                g.drawRect(x, y, w, h);
            }
        }.paint();
    }

    public void strokePath(final WCPath path) {
        log.fine("strokePath");
        if (path != null) {
            new Composite() {
                @Override void doPaint(Graphics g) {
                    Path2D p2d = (Path2D) path.getPlatformPath();
                    BasicStroke stroke = state.getStrokeNoClone().getPlatformStroke();
                    DropShadow shadow = state.getShadowNoClone();
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

    public void fillPath(final WCPath path) {
        log.fine("fillPath");
        if (path != null) {
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

    public void setTransform(WCTransform tm) {
        double m[] = tm.getMatrix();
        Affine3D at = new Affine3D(new Affine2D(m[0], m[1], m[2], m[3], m[4], m[5]));
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

    public void dispose() {
        if (!states.isEmpty()) {
            new IllegalStateException("Unbalanced saveState/restoreState")
                    .printStackTrace();
        }
        while (!states.isEmpty()) {
            restoreStateInternal();
        }
        // |state| is now the initial state. It may have a layer, too
        Layer layer = state.getLayerNoClone();
        if (layer != null) {
            renderLayer(layer);
        }
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
