/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.sg.PGCamera;
import com.sun.javafx.sg.PGSubScene;
import com.sun.javafx.sg.PGNode;
import com.sun.prism.RTTexture;
import com.sun.prism.Graphics;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.camera.PrismParallelCameraImpl;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Paint;
import com.sun.prism.camera.PrismCameraImpl;

/**
 * @author Thor Johannesson
 */
public class NGSubScene extends NGNode implements PGSubScene {

    private int rtWidth, rtHeight;
    private RTTexture rtt;
    private NGNode root = null;
    private boolean renderSG = true;

    public void setRoot(PGNode root) {
        if (this.root != root) {
            this.root = (NGNode)root;
            visualsChanged();
        } else {
            // FX SubScene will call setRoot either if root has changed or
            // if root is dirty. This is a workaround for dirty region support.
            // Remind: This workaround should be removed when dirty region
            // support is added for SubScene

            // Note FX subScene thinks SG is dirty, but will allow
            // root.isClean() to make that decision at render time. Thus keeping
            // old renderSG state.
            boolean renderSGOld = renderSG;
            visualsChanged();
            renderSG = renderSGOld;
        }
    }

    private Paint fillPaint;
    public void setFillPaint(Object paint) {
        if (fillPaint != paint) {
            fillPaint = (Paint)paint;
            visualsChanged();
        }
    }

    private PrismCameraImpl camera;
    public void setCamera(PGCamera camera) {
        PrismCameraImpl oldCamera = this.camera;
        if (camera != null) {
            this.camera = ((NGCamera) camera).getCameraImpl();
        } else {
            this.camera = PrismParallelCameraImpl.getInstance();
        }
        if (oldCamera != this.camera) {
            visualsChanged();
        }
    }

    public void setWidth(float width) {
        int iWidth = (int)Math.ceil(width);
        if (this.rtWidth != iWidth) {
            this.rtWidth = iWidth;
            geometryChanged();
            invalidateRTT();
        }
    }

    public void setHeight(float height) {
        int iHeight = (int)Math.ceil(height);
        if (this.rtHeight != iHeight) {
            this.rtHeight = iHeight;
            geometryChanged();
            invalidateRTT();
        }
    }

    boolean depthBuffer = false;
    public void setDepthBuffer(boolean depthBuffer) {
        if (this.depthBuffer != depthBuffer) {
            visualsChanged();
        }
        this.depthBuffer = depthBuffer;
    }

    private Object lights[];

    @Override
    public Object[] getLights() { return lights; }

    public void setLights(Object[] lights) {
        this.lights = lights;
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

    private void applyBackgroundFillPaint(Graphics g) {
        if (fillPaint != null) {
            if (fillPaint instanceof Color) {
                g.clear((Color)fillPaint);
            } else {
                if (!fillPaint.isOpaque()) {
                    g.clear();
                }
                g.setPaint(fillPaint);
                g.fillRect(0, 0, rtt.getContentWidth(), rtt.getContentHeight());
            }
        } else {
            // Default is transparent
            g.clear();
        }
    }

    @Override
    protected void renderContent(Graphics g) {
        if (rtt != null) {
            rtt.lock();
            if (rtt.isSurfaceLost()) {
                renderSG = true;
                rtt = null;
            }
        }

        if (!root.isClean() || renderSG) {
            if (rtt == null) {
                ResourceFactory factory = g.getResourceFactory();
                rtt = factory.createRTTexture(rtWidth, rtHeight,
                                              Texture.WrapMode.CLAMP_NOT_NEEDED);
            }
            Graphics rttGraphics = rtt.createGraphics();
            rttGraphics.setLights(lights);

            rttGraphics.setDepthBuffer(depthBuffer);
            if (camera != null) {
                rttGraphics.setCamera(camera);
            }
            applyBackgroundFillPaint(rttGraphics);
            rttGraphics.setTransform(BaseTransform.IDENTITY_TRANSFORM);

            root.render(rttGraphics);
            root.clearDirtyTree();
            renderSG = false;
        }
        g.drawTexture(rtt, rtt.getContentX(), rtt.getContentY(),
                      rtt.getContentWidth(), rtt.getContentHeight());
        rtt.unlock();
    }

}
