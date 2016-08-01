/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGPerspectiveCamera;
import com.sun.javafx.sg.prism.NodePath;
import com.sun.prism.Graphics;
import com.sun.prism.GraphicsResource;
import com.sun.prism.Image;
import com.sun.prism.Presentable;
import com.sun.prism.RTTexture;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Paint;
import com.sun.javafx.logging.PulseLogger;
import static com.sun.javafx.logging.PulseLogger.PULSE_LOGGING_ENABLED;

/**
 * Responsible for "painting" a scene. It invokes as appropriate API on the root NGNode
 * of a scene to determine dirty regions, render roots, etc. Also calls the render root
 * to render. Also invokes code to print dirty opts and paint overdraw rectangles according
 * to debug flags.
 */
abstract class ViewPainter implements Runnable {
    /**
     * An array of initially empty ROOT_PATHS. They are created on demand as
     * needed. Each path is associated with a different dirty region. We have
     * up to PrismSettings.dirtyRegionCount max dirty regions
     */
    private static NodePath[] ROOT_PATHS = new NodePath[PrismSettings.dirtyRegionCount];

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
    protected boolean freshBackBuffer;

    private int width;
    private int height;

    /**
     * root is the root node of the scene. overlayRoot is the root node of any
     * overlay which may be present (such as used for full screen overlay).
     */
    private NGNode root, overlayRoot;

    // These variables are all used as part of the dirty region optimizations,
    // and if dirty opts are turned off via a runtime flag, then these fields
    // are never initialized or used.
    private Rectangle dirtyRect;
    private RectBounds clip;
    private RectBounds dirtyRegionTemp;
    private DirtyRegionPool dirtyRegionPool;
    private DirtyRegionContainer dirtyRegionContainer;
    private Affine3D tx;
    private Affine3D scaleTx;
    private GeneralTransform3D viewProjTx;
    private GeneralTransform3D projTx;

    /**
     * This is used for drawing dirty regions and overdraw rectangles in cases where we are
     * not drawing the entire scene every time (specifically, when depth buffer is disabled).
     * In those cases we will draw the scene to the sceneBuffer, clear the actual back buffer,
     * blit the sceneBuffer into the back buffer, and then scribble on top of the back buffer
     * with the dirty regions and/or overdraw rectangles.
     *
     * When the depthBuffer is enabled on a scene, we always end up drawing the entire scene
     * anyway, so we don't bother with this sceneBuffer in that case. Of course, if dirty
     * region / overdraw rectangle drawing is turned off, then we don't use this. Thus,
     * only when you are doing some kind of debugging would this field be used and the
     * extra buffer copy incurred.
     */
    private RTTexture sceneBuffer;

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

    protected final void setRoot(NGNode node) {
        root = node;
    }

    protected final void setOverlayRoot(NGNode node) {
        overlayRoot = node;
    }

    private void adjustPerspective(NGCamera camera) {
        // This should definitely be true since this is only called by setDirtyRect
        assert PrismSettings.dirtyOptsEnabled;
        if (camera instanceof NGPerspectiveCamera) {
            scaleTx.setToScale(width / 2.0, -height / 2.0, 1);
            scaleTx.translate(1, -1);
            projTx.mul(scaleTx);
            viewProjTx = camera.getProjViewTx(viewProjTx);
            projTx.mul(viewProjTx);
        }
    }

    protected void paintImpl(final Graphics backBufferGraphics) {
        // We should not be painting anything with a width / height
        // that is <= 0, so we might as well bail right off.
        if (width <= 0 || height <= 0 || backBufferGraphics == null) {
            root.renderForcedContent(backBufferGraphics);
            return;
        }

        // This "g" variable might represent the back buffer graphics, or it
        // might be reassigned to the sceneBuffer graphics.
        Graphics g = backBufferGraphics;
        // Take into account the pixel scale factor for retina displays
        final float pixelScaleX = getPixelScaleFactorX();
        final float pixelScaleY = getPixelScaleFactorY();
        // Cache pixelScale in Graphics for use in 3D shaders such as camera and light positions.
        g.setPixelScaleFactors(pixelScaleX, pixelScaleY);

        // Initialize renderEverything based on various conditions that will cause us to render
        // the entire scene every time.
        boolean renderEverything = overlayRoot != null ||
                freshBackBuffer ||
                sceneState.getScene().isEntireSceneDirty() ||
                sceneState.getScene().getDepthBuffer() ||
                !PrismSettings.dirtyOptsEnabled;
        // We are going to draw dirty opt boxes either if we're supposed to show the dirty
        // regions, or if we're supposed to show the overdraw boxes.
        final boolean showDirtyOpts = PrismSettings.showDirtyRegions || PrismSettings.showOverdraw;
        // If showDirtyOpts is turned on and we're not using a depth buffer
        // then we will render the scene to an intermediate texture, and then at the end we'll
        // draw that intermediate texture to the back buffer.
        if (showDirtyOpts && !sceneState.getScene().getDepthBuffer()) {
            final int bufferWidth = (int) Math.ceil(width * pixelScaleX);
            final int bufferHeight = (int) Math.ceil(height * pixelScaleY);
            // Check whether the sceneBuffer texture needs to be reconstructed
            if (sceneBuffer != null) {
                sceneBuffer.lock();
                if (sceneBuffer.isSurfaceLost() ||
                        bufferWidth != sceneBuffer.getContentWidth() ||
                        bufferHeight != sceneBuffer.getContentHeight()) {
                    sceneBuffer.unlock();
                    sceneBuffer.dispose();
                    sceneBuffer = null;
                }
            }
            // If sceneBuffer is null, we need to create a new texture. In this
            // case we will also need to render the whole scene (so don't bother
            // with dirty opts)
            if (sceneBuffer == null) {
                sceneBuffer = g.getResourceFactory().createRTTexture(
                        bufferWidth,
                        bufferHeight,
                        Texture.WrapMode.CLAMP_TO_ZERO,
                        false);
                renderEverything = true;
            }
            sceneBuffer.contentsUseful();
            // Hijack the "g" graphics variable
            g = sceneBuffer.createGraphics();
            g.setPixelScaleFactors(pixelScaleX, pixelScaleY);
            g.scale(pixelScaleX, pixelScaleY);
        } else if (sceneBuffer != null) {
            // We're in a situation where we have previously rendered to the sceneBuffer, but in
            // this render pass for whatever reason we're going to draw directly to the back buffer.
            // In this case we need to release the sceneBuffer.
            sceneBuffer.dispose();
            sceneBuffer = null;
        }

        // The status will be set only if we're rendering with dirty regions
        int status = -1;

        // If we're rendering with dirty regions, then we'll call the root node to accumulate
        // the dirty regions and then again to do the pre culling.
        if (!renderEverything) {
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newPhase("Dirty Opts Computed");
            }
            clip.setBounds(0, 0, width, height);
            dirtyRegionTemp.makeEmpty();
            dirtyRegionContainer.reset();
            tx.setToIdentity();
            projTx.setIdentity();
            adjustPerspective(sceneState.getCamera());
            status = root.accumulateDirtyRegions(clip, dirtyRegionTemp,
                                                     dirtyRegionPool, dirtyRegionContainer,
                                                     tx, projTx);
            dirtyRegionContainer.roundOut();
            if (status == DirtyRegionContainer.DTR_OK) {
                root.doPreCulling(dirtyRegionContainer, tx, projTx);
            }
        }

        // We're going to need to iterate over the dirty region container a lot, so we
        // might as well save this reference.
        final int dirtyRegionSize = status == DirtyRegionContainer.DTR_OK ? dirtyRegionContainer.size() : 0;

        if (dirtyRegionSize > 0) {
            // We set this flag on Graphics so that subsequent code in the render paths of
            // NGNode know whether they ought to be paying attention to dirty region
            // culling bits.
            g.setHasPreCullingBits(true);

            // Find the render roots. There is a different render root for each dirty region
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newPhase("Render Roots Discovered");
            }
            for (int i = 0; i < dirtyRegionSize; ++i) {
                NodePath path = getRootPath(i);
                path.clear();
                root.getRenderRoot(getRootPath(i), dirtyRegionContainer.getDirtyRegion(i), i, tx, projTx);
            }

            // For debug purposes, write out to the pulse logger the number and size of the dirty
            // regions that are being used to render this pulse.
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.addMessage(dirtyRegionSize + " different dirty regions to render");
                for (int i=0; i<dirtyRegionSize; i++) {
                    PulseLogger.addMessage("Dirty Region " + i + ": " + dirtyRegionContainer.getDirtyRegion(i));
                    PulseLogger.addMessage("Render Root Path " + i + ": " + getRootPath(i));
                }
            }

            // If -Dprism.printrendergraph=true then we want to print out the render graph to the
            // pulse logger, annotated with all the dirty opts. Invisible nodes are skipped.
            if (PULSE_LOGGING_ENABLED && PrismSettings.printRenderGraph) {
                StringBuilder s = new StringBuilder();
                List<NGNode> roots = new ArrayList<>();
                for (int i = 0; i < dirtyRegionSize; i++) {
                    final RectBounds dirtyRegion = dirtyRegionContainer.getDirtyRegion(i);
                    // TODO it should be impossible to have ever created a dirty region that was empty...
                    if (dirtyRegion.getWidth() > 0 && dirtyRegion.getHeight() > 0) {
                        NodePath nodePath = getRootPath(i);
                        if (!nodePath.isEmpty()) {
                            roots.add(nodePath.last());
                        }
                    }
                }
                root.printDirtyOpts(s, roots);
                PulseLogger.addMessage(s.toString());
            }

            // Paint each dirty region
            for (int i = 0; i < dirtyRegionSize; ++i) {
                final RectBounds dirtyRegion = dirtyRegionContainer.getDirtyRegion(i);
                // TODO it should be impossible to have ever created a dirty region that was empty...
                // Make sure we are not trying to render in some invalid region
                if (dirtyRegion.getWidth() > 0 && dirtyRegion.getHeight() > 0) {
                    // Set the clip rectangle using integer bounds since a fractional bounding box will
                    // still require a complete repaint on pixel boundaries
                    int x0, y0;
                    dirtyRect.x = x0 = (int) Math.floor(dirtyRegion.getMinX() * pixelScaleX);
                    dirtyRect.y = y0 = (int) Math.floor(dirtyRegion.getMinY() * pixelScaleY);
                    dirtyRect.width  = (int) Math.ceil (dirtyRegion.getMaxX() * pixelScaleX) - x0;
                    dirtyRect.height = (int) Math.ceil (dirtyRegion.getMaxY() * pixelScaleY) - y0;
                    g.setClipRect(dirtyRect);
                    g.setClipRectIndex(i);
                    doPaint(g, getRootPath(i));
                }
            }
        } else {
            // There are no dirty regions, so just paint everything
            g.setHasPreCullingBits(false);
            g.setClipRect(null);
            this.doPaint(g, null);
        }
        root.renderForcedContent(g);

        // If we have an overlay then we need to render it too.
        if (overlayRoot != null) {
            overlayRoot.render(g);
        }

        // If we're showing dirty regions or overdraw, then we're going to need to draw
        // over-top the normal scene. If we have been drawing do the back buffer, then we
        // will just draw on top of it. If we have been drawing to the sceneBuffer, then
        // we will first blit the sceneBuffer into the back buffer, and then draw directly
        // on the back buffer.
        if (showDirtyOpts) {
            if (sceneBuffer != null) {
                g.sync();
                backBufferGraphics.clear();
                backBufferGraphics.drawTexture(sceneBuffer, 0, 0, width, height,
                        sceneBuffer.getContentX(), sceneBuffer.getContentY(),
                        sceneBuffer.getContentX() + sceneBuffer.getContentWidth(),
                        sceneBuffer.getContentY() + sceneBuffer.getContentHeight());
                sceneBuffer.unlock();
            }

            if (PrismSettings.showOverdraw) {
                // We are going to show the overdraw rectangles.
                if (dirtyRegionSize > 0) {
                    // In this case we have dirty regions, so we will iterate over them all
                    // and draw each dirty region's overdraw individually
                    for (int i = 0; i < dirtyRegionSize; i++) {
                        final Rectangle clip = new Rectangle(dirtyRegionContainer.getDirtyRegion(i));
                        backBufferGraphics.setClipRectIndex(i);
                        paintOverdraw(backBufferGraphics, clip);
                        backBufferGraphics.setPaint(new Color(1, 0, 0, .3f));
                        backBufferGraphics.drawRect(clip.x, clip.y, clip.width, clip.height);
                    }
                } else {
                    // In this case there were no dirty regions, so the clip is the entire scene
                    final Rectangle clip = new Rectangle(0, 0, width, height);
                    assert backBufferGraphics.getClipRectIndex() == 0;
                    paintOverdraw(backBufferGraphics, clip);
                    backBufferGraphics.setPaint(new Color(1, 0, 0, .3f));
                    backBufferGraphics.drawRect(clip.x, clip.y, clip.width, clip.height);
                }
            } else {
                // We are going to show the dirty regions
                if (dirtyRegionSize > 0) {
                    // We have dirty regions to draw
                    backBufferGraphics.setPaint(new Color(1, 0, 0, .3f));
                    for (int i = 0; i < dirtyRegionSize; i++) {
                        final RectBounds reg = dirtyRegionContainer.getDirtyRegion(i);
                        backBufferGraphics.fillRect(reg.getMinX(), reg.getMinY(), reg.getWidth(), reg.getHeight());
                    }
                } else {
                    // No dirty regions, fill the entire view area
                    backBufferGraphics.setPaint(new Color(1, 0, 0, .3f));
                    backBufferGraphics.fillRect(0, 0, width, height);
                }
            }
            root.clearPainted();
        }
    }

    /**
     * Utility method for painting the overdraw rectangles. Right now we're using a computationally
     * intensive approach of having an array of integers (image data) that we then write to in the
     * NGNodes, recording how many times each pixel position has been touched (well, technically, we're
     * just recording the bounds of drawn objects, so some pixels might be "red" but actually were never
     * drawn).
     *
     * @param g
     * @param clip
     */
    private void paintOverdraw(final Graphics g, final Rectangle clip) {
        final int[] pixels = new int[clip.width * clip.height];
        root.drawDirtyOpts(BaseTransform.IDENTITY_TRANSFORM, projTx, clip, pixels, g.getClipRectIndex());
        final Image image = Image.fromIntArgbPreData(pixels, clip.width, clip.height);
        final Texture texture = factory.getCachedTexture(image, Texture.WrapMode.CLAMP_TO_EDGE);
        g.drawTexture(texture, clip.x, clip.y, clip.x+clip.width, clip.y+clip.height, 0, 0, clip.width, clip.height);
        texture.unlock();
    }

    private static NodePath getRootPath(int i) {
        if (ROOT_PATHS[i] == null) {
            ROOT_PATHS[i] = new NodePath();
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

    protected float getPixelScaleFactorX() {
        return presentable == null ? 1.0f : presentable.getPixelScaleFactorX();
    }

    protected float getPixelScaleFactorY() {
        return presentable == null ? 1.0f : presentable.getPixelScaleFactorY();
    }

    private void doPaint(Graphics g, NodePath renderRootPath) {
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
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.newPhase("Painting");
        }
        GlassScene scene = sceneState.getScene();
        scene.clearEntireSceneDirty();
        g.setLights(scene.getLights());
        g.setDepthBuffer(scene.getDepthBuffer());
        Color clearColor = sceneState.getClearColor();
        if (clearColor != null) {
            g.clear(clearColor);
        }
        Paint curPaint = sceneState.getCurrentPaint();
        if (curPaint != null) {
            if (curPaint.getType() != com.sun.prism.paint.Paint.Type.COLOR) {
                g.getRenderTarget().setOpaque(curPaint.isOpaque());
            }
            g.setPaint(curPaint);
            g.fillQuad(0, 0, width, height);
        }
        g.setCamera(sceneState.getCamera());
        g.setRenderRoot(renderRootPath);
        root.render(g);
    }
}
