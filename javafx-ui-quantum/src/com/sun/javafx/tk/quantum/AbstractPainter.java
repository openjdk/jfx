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

import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.jmx.HighlightRegion;
import com.sun.javafx.runtime.SystemProperties;
import com.sun.javafx.sg.NodePath;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.BasicStroke;
import com.sun.prism.Graphics;
import com.sun.prism.GraphicsResource;
import com.sun.prism.Presentable;
import com.sun.prism.ResourceFactory;
import com.sun.prism.camera.PrismPerspectiveCameraImpl;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.paint.Color;

import static com.sun.javafx.logging.PulseLogger.PULSE_LOGGING_ENABLED;
import static com.sun.javafx.logging.PulseLogger.PULSE_LOGGER;
import com.sun.prism.camera.PrismCameraImpl;

abstract class AbstractPainter implements Runnable {
    
    private static final NodePath<NGNode> NODE_PATH = new NodePath<>();

    /*
     * This could be a per-scene lock but there is no guarantee that the 
     * FX handlers called in GlassViewEventHandler would not modify other scenes.
     */
    protected static final ReentrantLock renderLock = new ReentrantLock();

    protected static final PaintCollector collector = PaintCollector.getInstance();

    protected SceneState sceneState;

    protected Presentable       presentable;
    protected ResourceFactory   factory;

    protected int               width;
    protected int               height;
    
    Rectangle                   dirtyRect;
    RectBounds                  clip;
    RectBounds                  dirtyRegionTemp;
    DirtyRegionPool             dirtyRegionPool;
    DirtyRegionContainer        dirtyRegionContainer;
    Affine3D                    tx;
    Affine3D                    scaleTx;
    GeneralTransform3D          viewProjTx;
    GeneralTransform3D          projTx;
    NGNode                      root, overlayRoot;

    protected AbstractPainter(GlassScene gs) {
        sceneState = gs.getSceneState();
        if (sceneState == null) {
            throw new NullPointerException("Scene state is null");
        }
        if (PrismSettings.dirtyOptsEnabled) {
            tx = new Affine3D();
            viewProjTx = new GeneralTransform3D();
            projTx = new GeneralTransform3D();
            scaleTx = new Affine3D();
            clip = new RectBounds();
            dirtyRect = new Rectangle();
            dirtyRegionTemp = new RectBounds();
            dirtyRegionPool = new DirtyRegionPool(PrismSettings.dirtyRegionCount);
            dirtyRegionContainer = dirtyRegionPool.checkOut();
        }
    }

    protected void setPaintBounds(int w, int h) {
        width  = w;
        height = h;
    }
    
    protected void setRoot(NGNode node) {
        root = node;
    }

    protected NGNode getRoot() {
        return (root);
    }

    protected void setOverlayRoot(NGNode node) {
        overlayRoot = node;
    }
    
    protected NGNode getOverlayRoot() {
        return (overlayRoot);
    }
    
    protected abstract void doPaint(Graphics g, NodePath<NGNode> renderRoot);
    
    private void adjustPerspective(PrismCameraImpl camera) {
        if (camera instanceof PrismPerspectiveCameraImpl) {
            PrismPerspectiveCameraImpl perspCamera = (PrismPerspectiveCameraImpl) camera;
            scaleTx.setToScale(width / 2.0, -height / 2.0, 1);
            scaleTx.translate(1, -1);
            projTx.mul(scaleTx);
            viewProjTx = perspCamera.getProjViewTx(viewProjTx);
            projTx.mul(viewProjTx);
        }
    }

    private int setDirtyRect(Graphics g) {
        clip.setBounds(0, 0, width, height);
        dirtyRegionTemp.makeEmpty();
        dirtyRegionContainer.reset();
        tx.setToIdentity();
        projTx.setIdentity();
        adjustPerspective(sceneState.getScene().getCamera());
        int status = root.accumulateDirtyRegions(clip, dirtyRegionTemp,
                                                 dirtyRegionPool, dirtyRegionContainer, 
                                                 tx, projTx);
        dirtyRegionContainer.roundOut();

        return status;
    }

    public void paintImpl(Graphics g) {
        int status = DirtyRegionContainer.DTR_CONTAINS_CLIP;
        if (PrismSettings.dirtyOptsEnabled) {
            long start = PULSE_LOGGING_ENABLED ? System.currentTimeMillis() : 0;
            if (!sceneState.getScene().isEntireSceneDirty()) {
                status = setDirtyRect(g);
                if (status == DirtyRegionContainer.DTR_OK) {
                    root.doPreCulling(dirtyRegionContainer,
                                      tx, projTx);
                }
            }
            if (PULSE_LOGGING_ENABLED) {
                PULSE_LOGGER.renderMessage(start, System.currentTimeMillis(), "Dirty Opts Computed");
            }
        }

        if (!PrismSettings.showDirtyRegions && status == DirtyRegionContainer.DTR_OK) {
            g.setHasPreCullingBits(true);
            if (PULSE_LOGGING_ENABLED && dirtyRegionContainer.size() > 1) {
                PULSE_LOGGER.renderMessage(dirtyRegionContainer.size() + " different dirty regions to render");
            }
            float pixelScale = (presentable == null ? 1.0f : presentable.getPixelScaleFactor());
            for (int i = 0; i < dirtyRegionContainer.size(); i++) {
                final RectBounds dirtyRegion = dirtyRegionContainer.getDirtyRegion(i);
                // make sure we are not trying to render in some invalid region
                if (dirtyRegion.getWidth() > 0 && dirtyRegion.getHeight() > 0) {
                    // set the clip rectangle using integer
                    // bounds since a fractional bounding box will
                    // still require a complete repaint on
                    // pixel boundaries
                    dirtyRect.setBounds(dirtyRegion);
                    if (pixelScale != 1.0f) {
                        dirtyRect.x *= pixelScale;
                        dirtyRect.y *= pixelScale;
                        dirtyRect.width *= pixelScale;
                        dirtyRect.height *= pixelScale;
                    }
                    g.setClipRect(dirtyRect);
                    g.setClipRectIndex(i);

                    if (g.isDepthTest()) {
                        doPaint(g, null);
                    } else {
                        doPaint(g, root.getRenderRoot(NODE_PATH, dirtyRegion, i, tx, projTx));
                        NODE_PATH.clear();
                    }
                }
            }
        } else {
            g.setHasPreCullingBits(false);
            g.setClipRect(null);
            this.doPaint(g, null);
        }
        
        if (PrismSettings.showDirtyRegions) {
            if (PrismSettings.showCull) {
                 root.drawCullBits(g);
            }
            g.setDepthTest(false);
            if (status == DirtyRegionContainer.DTR_OK) {
                g.setPaint(new Color(1, 0, 0, .3f));
                for (int i = 0; i < dirtyRegionContainer.size(); i++) {
                    RectBounds reg = dirtyRegionContainer.getDirtyRegion(i);
                    g.fillRect(reg.getMinX(), reg.getMinY(), 
                               reg.getWidth(), reg.getHeight());                        
                }
            } else {
                g.setPaint(new Color(1, 0, 0, .3f));
                g.fillRect(0, 0, width, height);
            }
        }
        
        if (SystemProperties.isDebug()) {
            Set<HighlightRegion> highlightRegions =
                    Toolkit.getToolkit().getHighlightedRegions();
            if (highlightRegions != null) {
                g.setStroke(new BasicStroke(1f,
                                            BasicStroke.CAP_BUTT,
                                            BasicStroke.JOIN_BEVEL,
                                            10f));
                for (HighlightRegion region: highlightRegions) {
                    if (sceneState.getScene().equals(region.getTKScene())) {
                        g.setPaint(new Color(1, 1, 1, 1));
                        g.drawRect((float) region.getMinX(),
                                   (float) region.getMinY(),
                                   (float) region.getWidth(),
                                   (float) region.getHeight());
                        g.setPaint(new Color(0, 0, 0, 1));
                        g.drawRect((float) region.getMinX() - 1,
                                   (float) region.getMinY() - 1,
                                   (float) region.getWidth() + 2,
                                   (float) region.getHeight() + 2);
                    }
                }
            }
        }
        if (overlayRoot != null) {
            overlayRoot.render(g);
        }
    }

    void disposePresentable() {
        if (presentable instanceof GraphicsResource) {
            ((GraphicsResource)presentable).dispose();
        }
        presentable = null;
    }
    
    protected boolean validateStageGraphics() {
        return true;
    }
}
