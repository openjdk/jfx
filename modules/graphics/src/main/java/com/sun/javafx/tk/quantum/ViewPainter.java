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
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.javafx.sg.prism.NGPerspectiveCamera;
import com.sun.javafx.sg.prism.NodePath;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.BasicStroke;
import com.sun.prism.Graphics;
import com.sun.prism.GraphicsResource;
import com.sun.prism.Presentable;
import com.sun.prism.ResourceFactory;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Paint;

import static com.sun.javafx.logging.PulseLogger.PULSE_LOGGING_ENABLED;
import static com.sun.javafx.logging.PulseLogger.PULSE_LOGGER;

abstract class ViewPainter implements Runnable {
    private static NodePath<NGNode>[] ROOT_PATHS = new NodePath[PrismSettings.dirtyRegionCount];

    /*
     * This could be a per-scene lock but there is no guarantee that the
     * FX handlers called in GlassViewEventHandler would not modify other scenes.
     */
    protected static final ReentrantLock renderLock = new ReentrantLock();

    // Pen dimensions. Pen width and height are checked on every repaint
    // to match its scene width/height. If any difference is found, the
    // pen surface (Presentable or RTTexture) is recreated.
    protected int penWidth = -1;
    protected int penHeight = -1;
    protected int viewWidth;
    protected int viewHeight;

    protected final SceneState sceneState;

    protected Presentable presentable;
    protected ResourceFactory factory;

    private int width;
    private int height;

    private boolean renderOverlay = false;

    private Rectangle dirtyRect;
    private RectBounds clip;
    private RectBounds dirtyRegionTemp;
    private DirtyRegionPool dirtyRegionPool;
    private DirtyRegionContainer dirtyRegionContainer;
    private Affine3D tx;
    private Affine3D scaleTx;
    private GeneralTransform3D viewProjTx;
    private GeneralTransform3D projTx;
    private NGNode root, overlayRoot;

    protected ViewPainter(GlassScene gs) {
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

    protected void setRoot(NGNode node) {
        root = node;
    }

    protected void setOverlayRoot(NGNode node) {
        overlayRoot = node;
    }

    protected void setRenderOverlay(boolean val) {
        renderOverlay = val;
    }

    private void adjustPerspective(NGCamera camera) {
        if (camera instanceof NGPerspectiveCamera) {
            NGPerspectiveCamera perspCamera = (NGPerspectiveCamera) camera;
            scaleTx.setToScale(width / 2.0, -height / 2.0, 1);
            scaleTx.translate(1, -1);
            projTx.mul(scaleTx);
            viewProjTx = perspCamera.getProjViewTx(viewProjTx);
            projTx.mul(viewProjTx);
        }
    }

    private int setDirtyRect() {
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

    protected void paintImpl(Graphics g) {
        int status = DirtyRegionContainer.DTR_CONTAINS_CLIP;
        if (PrismSettings.dirtyOptsEnabled) {
            long start = PULSE_LOGGING_ENABLED ? System.currentTimeMillis() : 0;
            if (!sceneState.getScene().isEntireSceneDirty() && !renderOverlay) {
                status = setDirtyRect();
                if (status == DirtyRegionContainer.DTR_OK) {
                    root.doPreCulling(dirtyRegionContainer,
                                      tx, projTx);
                }
            }
            if (PULSE_LOGGING_ENABLED) {
                PULSE_LOGGER.renderMessage(start, System.currentTimeMillis(), "Dirty Opts Computed");
            }
        }
        final int dirtyRegionSize = dirtyRegionContainer.size();

        if (!PrismSettings.showDirtyRegions && status == DirtyRegionContainer.DTR_OK) {
            g.setHasPreCullingBits(true);
            if (PULSE_LOGGING_ENABLED && dirtyRegionSize > 1) {
                PULSE_LOGGER.renderMessage(dirtyRegionSize + " different dirty regions to render");
            }
            float pixelScale = (presentable == null ? 1.0f : presentable.getPixelScaleFactor());
            if (!sceneState.getScene().getDepthBuffer() && PrismSettings.occlusionCullingEnabled) {
                for (int i = 0; i < dirtyRegionSize; ++i) {
                    root.getRenderRoot(getRootPath(i), dirtyRegionContainer.getDirtyRegion(i), i, tx, projTx);
                }
            }
            for (int i = 0; i < dirtyRegionSize; ++i) {
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

                    // Disable occlusion culling if depth buffer is enabled for the scene.
                    if (sceneState.getScene().getDepthBuffer() || !PrismSettings.occlusionCullingEnabled) {
                        doPaint(g, null);
                    } else {
                        final NodePath<NGNode> path = getRootPath(i);
                        doPaint(g, path);
                        path.clear();
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

            // save current depth test state
            boolean prevDepthTest = g.isDepthTest();

            g.setDepthTest(false);
            if (status == DirtyRegionContainer.DTR_OK) {
                g.setPaint(new Color(1, 0, 0, .3f));
                for (int i = 0; i < dirtyRegionSize; i++) {
                    RectBounds reg = dirtyRegionContainer.getDirtyRegion(i);
                    g.fillRect(reg.getMinX(), reg.getMinY(),
                               reg.getWidth(), reg.getHeight());
                }
            } else {
                g.setPaint(new Color(1, 0, 0, .3f));
                g.fillRect(0, 0, width, height);
            }

            // restore previous depth test state
            g.setDepthTest(prevDepthTest);

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
        if (renderOverlay) {
            overlayRoot.render(g);
        }
    }

    private static NodePath<NGNode> getRootPath(int i) {
        if (ROOT_PATHS[i] == null) {
            ROOT_PATHS[i] = new NodePath<>();
        }
        return ROOT_PATHS[i];
    }

    protected void disposePresentable() {
        if (presentable instanceof GraphicsResource) {
            ((GraphicsResource)presentable).dispose();
        }
        presentable = null;
    }

    protected boolean validateStageGraphics() {
        if (!sceneState.isValid()) {
            // indicates something happened between the scheduling of the
            // job and the running of this job.
            return false;
        }

        width = viewWidth = sceneState.getWidth();
        height = viewHeight = sceneState.getHeight();

        return sceneState.isWindowVisible() && !sceneState.isWindowMinimized();
    }

    private void doPaint(Graphics g, NodePath<NGNode> renderRootPath) {
        if (PrismSettings.showDirtyRegions) {
            g.setClipRect(null);
        }
        // Null path indicates that occlusion culling is not used
        if (renderRootPath != null) {
            if (renderRootPath.isEmpty()) {
                // empty render path indicates that no rendering is needed.
                // There may be occluded dirty Nodes however, so we need to clear them
                root.clearDirtyTree();
                return;
            }
            // If the path is not empty, the first node must be the root node
            assert(renderRootPath.getCurrentNode() == root);
        }
        long start = PULSE_LOGGING_ENABLED ? System.currentTimeMillis() : 0;
        try {
            GlassScene scene = sceneState.getScene();
            scene.clearEntireSceneDirty();
            g.setLights(scene.getLights());
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
