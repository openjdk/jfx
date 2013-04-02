/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.sg.NodePath;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.prism.Graphics;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.camera.PrismCameraImpl;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Paint;

import static com.sun.javafx.logging.PulseLogger.PULSE_LOGGING_ENABLED;
import static com.sun.javafx.logging.PulseLogger.PULSE_LOGGER;

abstract class ViewPainter extends AbstractPainter {

    // Pen dimensions. Pen width and height are checked on every repaint
    // to match its scene width/height. If any difference is found, the
    // pen surface (Presentable or RTTexture) is recreated.
    protected int               penWidth = -1;
    protected int               penHeight = -1;
    protected int               viewWidth;
    protected int               viewHeight;
    
    protected boolean           valid;

    protected ViewPainter(GlassScene gs) {
        super(gs);
    }
        
    @Override protected boolean validateStageGraphics() {
        valid = super.validateStageGraphics();
        
        if (!valid) {
            return false;
        }

        SceneState viewState = scene.getViewState();
        if (!viewState.isValid()) {
            // indicates something happened between the scheduling of the 
            // job and the running of this job. 
            return false;
        }

        viewWidth = viewState.getWidth();
        viewHeight = viewState.getHeight();

        setPaintBounds(viewWidth, viewHeight);

        if (factory == null) {
            // the factory really should not be null as
            // we really want all that factory work on the event thread
            try {
                viewState.lock();
                factory = GraphicsPipeline.getDefaultResourceFactory();
            } finally {
                /*
                 * Don't flush to the screen if there was a failure
                 * creating the graphics factory
                 */
                viewState.unlock();
            }
            return ((factory != null) && factory.isDeviceReady());
        }
        return (viewState.isWindowVisible() && ! viewState.isWindowMinimized()
                                   && factory.isDeviceReady());
    }
    
    @Override protected void doPaint(Graphics g, NodePath<NGNode> renderRootPath) {
        if (PrismSettings.showDirtyRegions) {
            g.setClipRect(null);
        }
        long start = PULSE_LOGGING_ENABLED ? System.currentTimeMillis() : 0;
        try {
            scene.clearEntireSceneDirty();
            g.setDepthBuffer(scene.getDepthBuffer());
            Color clearColor = scene.getClearColor();
            if (clearColor != null) {
                g.clear(clearColor);
            }
            Paint curPaint = scene.getCurrentPaint();
            if (curPaint != null) {
                if (curPaint.getType() != com.sun.prism.paint.Paint.Type.COLOR) {
                    g.getRenderTarget().setOpaque(curPaint.isOpaque());
                }
                g.setPaint(curPaint);
                g.fillQuad(0, 0, width, height);
            }
            g.setCamera(scene.getCamera());
            g.setRenderRoot(renderRootPath);
            root.render(g);
        } finally {
            if (PULSE_LOGGING_ENABLED) {
                PULSE_LOGGER.renderMessage(start, System.currentTimeMillis(), "Painted");
            }
        }
    }
    
}
