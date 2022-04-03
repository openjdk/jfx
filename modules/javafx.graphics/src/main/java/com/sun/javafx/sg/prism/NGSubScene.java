/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.CompositeMode;
import com.sun.prism.Graphics;
import com.sun.prism.RTTexture;
import com.sun.prism.RenderTarget;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Paint;

/**
 * @author Thor Johannesson
 */
public class NGSubScene extends NGNode {

    // The Scene logical dimensions (pre-pixel scaling)
    private float slWidth, slHeight;
    // The scaled dimensions last used in the rtt
    private double lastScaledW, lastScaledH;
    private RTTexture rtt;
    // ressolveRTT is a temporary render target to "resolve" a msaa render buffer
    // into a normal color render target.
    // REMIND: resolveRTT could be a single shared scratch rtt
    private RTTexture resolveRTT = null;
    private NGNode root = null;
    private boolean renderSG = true;
    // Depth and msaa are immutable states
    private final boolean depthBuffer;
    private final boolean msaa;

    public NGSubScene(boolean depthBuffer, boolean msaa) {
        this.depthBuffer = depthBuffer;
        this.msaa = msaa;
    }

    private NGSubScene() {
        this(false, false);
    }

    public void setRoot(NGNode root) {
        this.root = root;
    }

    private Paint fillPaint;
    public void setFillPaint(Object paint) {
        fillPaint = (Paint)paint;
    }

    private NGCamera camera;
    public void setCamera(NGCamera camera) {
        this.camera = camera == null ? NGCamera.INSTANCE : camera;
    }

    public void setWidth(float width) {
        if (this.slWidth != width) {
            this.slWidth = width;
            geometryChanged();
            invalidateRTT();
        }
    }

    public void setHeight(float height) {
        if (this.slHeight != height) {
            this.slHeight = height;
            geometryChanged();
            invalidateRTT();
        }
    }

    private NGLightBase[] lights;

    public NGLightBase[] getLights() { return lights; }

    public void setLights(NGLightBase[] lights) {
        this.lights = lights;
    }

    public void markContentDirty() {
        visualsChanged();
    }

    @Override
    protected void visualsChanged() {
        renderSG = true;
        super.visualsChanged();
    }

    @Override
    protected void geometryChanged() {
        renderSG = true;
        super.geometryChanged();
    }

    private void invalidateRTT() {
        if (rtt != null) {
            // TODO as possibile optimization by keeping old rtt if SubScene
            // becomes smaller
            rtt.dispose();
            rtt = null;
        }
    }

    @Override
    protected boolean hasOverlappingContents() {
        //TODO verify correctness
        return false;
    }

    private boolean isOpaque = false;
    private void applyBackgroundFillPaint(Graphics g) {
        isOpaque = true;
        if (fillPaint != null) {
            if (fillPaint instanceof Color) {
                Color fillColor = (Color)fillPaint;
                isOpaque = (fillColor.getAlpha() >= 1.0);
                g.clear(fillColor);
            } else {
                if (!fillPaint.isOpaque()) {
                    g.clear();
                    isOpaque = false;
                }
                g.setPaint(fillPaint);
                g.fillRect(0, 0, rtt.getContentWidth(), rtt.getContentHeight());
            }
        } else {
            isOpaque = false;
            // Default is transparent
            g.clear();
        }
    }

    @Override
    public void renderForcedContent(Graphics gOptional) {
        root.renderForcedContent(gOptional);
    }

    private static double hypot(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    // Allow the scaled size in pixels to vary by a distance approximately
    // large enough to affect the sampling result in a LINEAR interpolation.
    // If we move by 1/256th of a pixel from one color to the opposite color
    // then in the worst case the sample value might change by +/- 1 bit.
    static final double THRESHOLD = 1.0 / 256.0;
    @Override
    protected void renderContent(Graphics g) {
        if (slWidth <= 0.0 || slHeight <= 0.0) { return; }
        BaseTransform txform = g.getTransformNoClone();
        double scaleX = hypot(txform.getMxx(), txform.getMyx(), txform.getMzx());
        double scaleY = hypot(txform.getMxy(), txform.getMyy(), txform.getMzy());
        double scaledW = slWidth * scaleX;
        double scaledH = slHeight * scaleY;
        int rtWidth = (int) Math.ceil(scaledW - THRESHOLD);
        int rtHeight = (int) Math.ceil(scaledH - THRESHOLD);
        if (Math.max(Math.abs(scaledW - lastScaledW), Math.abs(scaledH - lastScaledH)) > THRESHOLD) {
            if (rtt != null &&
                (rtWidth != rtt.getContentWidth() ||
                 rtHeight != rtt.getContentHeight()))
            {
                invalidateRTT();
            }
            renderSG = true;
            lastScaledW = scaledW;
            lastScaledH = scaledH;
        }
        if (rtt != null) {
            rtt.lock();
            if (rtt.isSurfaceLost()) {
                renderSG = true;
                rtt = null;
            }
        }

        if (renderSG || !root.isClean()) {
            if (rtt == null) {
                ResourceFactory factory = g.getResourceFactory();
                rtt = factory.createRTTexture(rtWidth, rtHeight,
                                              Texture.WrapMode.CLAMP_TO_ZERO,
                                              msaa);
            }
            Graphics rttGraphics = rtt.createGraphics();
            // The pixel scale factors must be copied to the rttGraphics, otherwise the position
            // of the lights will not be scaled correctly on HiDPI displays like MacBooks' retina
            // displays.
            rttGraphics.setPixelScaleFactors(g.getPixelScaleFactorX(), g.getPixelScaleFactorY());
            rttGraphics.scale((float) scaleX, (float) scaleY);
            rttGraphics.setLights(lights);

            rttGraphics.setDepthBuffer(depthBuffer);
            if (camera != null) {
                rttGraphics.setCamera(camera);
            }
            applyBackgroundFillPaint(rttGraphics);

            root.render(rttGraphics);
            root.clearDirtyTree();
            renderSG = false;
        }
        if (msaa) {
            int x0 = rtt.getContentX();
            int y0 = rtt.getContentY();
            int x1 = x0 + rtWidth;
            int y1 = y0 + rtHeight;
            if ((isOpaque || g.getCompositeMode() == CompositeMode.SRC) &&
                    isDirectBlitTransform(txform, scaleX, scaleY) &&
                    !g.isDepthTest())
            {
                // Round translation to closest pixel
                int tx = (int)(txform.getMxt() + 0.5);
                int ty = (int)(txform.getMyt() + 0.5);
                // Blit SubScene directly to scene surface

                // Intersect src and dst boundaries.
                // On D3D if blit is called outside boundary it will draw
                // nothing. Using intersect prevents that from occurring.
                RenderTarget target = g.getRenderTarget();
                int dstX0 = target.getContentX() + tx;
                int dstY0 = target.getContentY() + ty;
                int dstX1 = dstX0 + rtWidth;
                int dstY1 = dstY0 + rtHeight;
                int dstW = target.getContentWidth();
                int dstH = target.getContentHeight();
                int dX = dstX1 > dstW ? dstW - dstX1 : 0;
                int dY = dstY1 > dstH ? dstH - dstY1 : 0;
                g.blit(rtt, null, x0, y0, x1 + dX, y1 + dY,
                            dstX0, dstY0, dstX1 + dX, dstY1 + dY);
            } else {
                if (resolveRTT != null &&
                        (resolveRTT.getContentWidth() < rtWidth ||
                        (resolveRTT.getContentHeight() < rtHeight)))
                {
                    // If msaa rtt is larger than resolve buffer, then dispose
                    resolveRTT.dispose();
                    resolveRTT = null;
                }
                if (resolveRTT != null) {
                    resolveRTT.lock();
                    if (resolveRTT.isSurfaceLost()) {
                        resolveRTT = null;
                    }
                }
                if (resolveRTT == null) {
                    resolveRTT = g.getResourceFactory().createRTTexture(rtWidth, rtHeight,
                            Texture.WrapMode.CLAMP_TO_ZERO, false);
                }
                // We could potentially reuse g, but any transform in g would
                // affect the blit...
                resolveRTT.createGraphics().blit(rtt, resolveRTT, x0, y0, x1, y1,
                                                 x0, y0, x1, y1);
                g.drawTexture(resolveRTT, 0, 0, (float) (rtWidth / scaleX), (float) (rtHeight / scaleY),
                              0, 0, rtWidth, rtHeight);
                resolveRTT.unlock();
            }
        } else {
            g.drawTexture(rtt, 0, 0, (float) (rtWidth / scaleX), (float) (rtHeight / scaleY),
                          0, 0, rtWidth, rtHeight);
        }
        rtt.unlock();
    }

    private static boolean isDirectBlitTransform(BaseTransform tx, double sx, double sy) {
        if (sx == 1.0 && sy == 1.0) return tx.isTranslateOrIdentity();
        if (!tx.is2D()) return false;
        return (tx.getMxx() == sx &&
                tx.getMxy() == 0.0 &&
                tx.getMyx() == 0.0 &&
                tx.getMyy() == sy);
    }

    public NGCamera getCamera() {
        return camera;
    }
}
