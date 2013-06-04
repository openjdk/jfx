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

import java.nio.IntBuffer;

import com.sun.glass.ui.Screen;
import com.sun.javafx.sg.NodePath;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.prism.Graphics;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.RTTexture;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.camera.PrismCameraImpl;
import com.sun.prism.impl.Disposer;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Paint;

final class EmbeddedPainter extends AbstractPainter {
    
    private RTTexture       texture;
    private EmbeddedScene   escene;

    protected EmbeddedPainter(EmbeddedScene es) {
        super(es);
        setRoot(es.getRoot());
        escene = es;
    }
    
    @Override protected boolean validateStageGraphics() {
        boolean valid = super.validateStageGraphics();
        
        if (!valid) {
            return false;
        }
        
        if (escene.host == null) {
            return false;
        }
        
        if ((escene.width <= 0) || (escene.height <= 0)) {
            return false;
        }

        setPaintBounds(escene.width, escene.height);
        
        return true;
    }
        
    @Override
    public void run() {
        if (!validateStageGraphics()) {
            return;
        }

        if (factory == null) {
            Screen mainScreen = Screen.getMainScreen();
            GraphicsPipeline pipeline = GraphicsPipeline.getPipeline();
            factory = pipeline.getResourceFactory(mainScreen);
            if (!factory.isDeviceReady()) {
                return;
            }
        }

        escene.sizeLock.lock();
        
        try {
            if ((texture == null) || (escene.textureBits == null) || escene.needsReset) {
                texture = factory.createRTTexture(escene.width, escene.height,
                                                  WrapMode.CLAMP_NOT_NEEDED);
                if (texture == null) {
                    return;
                }
                escene.textureBits = IntBuffer.allocate(escene.width * escene.height);
                escene.needsReset = false;
            }

            Graphics g = texture.createGraphics();
            if (g == null) {
                escene.needsReset = true;
                escene.entireSceneNeedsRepaint();
                return;
            }
            paintImpl(g);
            
            escene.textureBits.rewind();
            texture.readPixels(escene.textureBits);
            
            escene.sceneRepainted();
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        } finally {
            Disposer.cleanUp();
            escene.sizeLock.unlock();
        }
    }
    
    @Override protected void doPaint(Graphics g, NodePath<NGNode> renderRoot) {
        escene.clearEntireSceneDirty();
        g.setDepthBuffer(escene.getDepthBuffer());
        g.clear(Color.TRANSPARENT);
        Paint fillPaint = escene.getFillPaint();
        if (fillPaint != null) {
            g.getRenderTarget().setOpaque(fillPaint.isOpaque());
            g.setPaint(fillPaint);
            g.fillQuad(0, 0, escene.width, escene.height);
        }
        PrismCameraImpl camera = escene.getCamera();
        g.setCamera(camera);
        escene.getRoot().render(g); // Ignoring occlusion culling for now
    }
}
