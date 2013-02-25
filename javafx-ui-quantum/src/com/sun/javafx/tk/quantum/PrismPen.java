/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk.quantum;

import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.stage.StageStyle;

import com.sun.glass.ui.View;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.camera.PrismCameraImpl;
import com.sun.prism.camera.PrismParallelCameraImpl;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Paint;
import com.sun.prism.render.ToolkitInterface;

final class PrismPen extends Object {

    private static PrismCameraImpl DEFAULT_CAMERA = PrismParallelCameraImpl.getInstance();
    private static PaintCollector  collector      = PaintCollector.getInstance();

    Future paintRunnableFuture;
    PrismCameraImpl camera;
    ViewScene scene;
    HashMap caps = new HashMap();
    AtomicBoolean painting;

    // A singleton object to perform the painting, so we don't create a
    // new Runnable instance on every repaint.
    private ViewPainter        painter;
    private PaintRenderJob     paintRenderJob;

    // Indicates whether the surface to which we are rendering has a depth buffer
    private boolean depthBuffer;

    public PrismPen(ViewScene scene, boolean depthBuffer) {
        this.scene = scene;
        this.depthBuffer = depthBuffer;
        this.painting = new AtomicBoolean(false);

        scene.setFillPaint(Color.WHITE);
    }

    boolean getDepthBuffer() {
        return depthBuffer;
    }

    public void repaint() {
        View view = scene.getPlatformView();
        if (view == null) {
            return;
        }

        if (painting.getAndSet(true) == false) {
            Toolkit tk = Toolkit.getToolkit();
            ToolkitInterface toolkit = (ToolkitInterface)tk;
            paintRunnableFuture = toolkit.addRenderJob(paintRenderJob);
        }
    }

    protected Future getFuture() {
        return paintRunnableFuture;
    }

    protected AtomicBoolean getPainting() {
        return painting;
    }

    protected ViewPainter getPainter() {
        return painter;
    }

    protected void setPainter(ViewPainter vp) {
        painter = vp;

        paintRenderJob = new PaintRenderJob(scene, collector.getRendered(), (Runnable)painter);
    }

    protected Color getClearColor() {
        WindowStage windowStage = (scene != null) ? scene.getWindowStage() : null;
        if (windowStage != null && windowStage.getPlatformWindow().isTransparentWindow()) {
            return (Color.TRANSPARENT);
        } else {
            if (scene.fillPaint == null) {
                return Color.WHITE;
            } else if (scene.fillPaint.isOpaque() ||
                    (windowStage != null && windowStage.getPlatformWindow().isUnifiedWindow())) {
                //For bare windows the transparent fill is allowed
                if (scene.fillPaint.getType() == Paint.Type.COLOR) {
                    return (Color)scene.fillPaint;
                } else if (depthBuffer) {
                    // Must set clearColor in order for the depthBuffer to be cleared
                    return Color.TRANSPARENT;
                } else {
                    return null;
                }
            } else {
                return Color.WHITE;
            }
        }
    }

    protected Paint getCurrentPaint() {
        WindowStage windowStage = (scene != null) ? scene.getWindowStage() : null;
        if ((windowStage != null) && windowStage.getStyle() == StageStyle.TRANSPARENT) {
            return (Color.TRANSPARENT.equals(scene.fillPaint) ? null : scene.fillPaint);
        } else {
            if (scene == null || scene.fillPaint == null) {
                return null;
            } else if (scene.fillPaint.isOpaque()) {
                if (scene.fillPaint.getType() == Paint.Type.COLOR)
                    return null;
                else
                    return scene.fillPaint;
            } else {
                return scene.fillPaint;
            }
        }
    }

    void setCamera(final PrismCameraImpl camera) {
        if (camera != null) {
            this.camera = camera;
        } else {
            this.camera = DEFAULT_CAMERA;
        }
    }

    protected PrismCameraImpl getCamera() {
        return camera;
    }
}
