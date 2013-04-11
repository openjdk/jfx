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

package com.sun.javafx.sg.prism;

import com.sun.glass.ui.Screen;
import java.util.List;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.sg.BaseCacheFilter;
import com.sun.javafx.sg.BaseEffectFilter;
import com.sun.javafx.sg.BaseNode;
import com.sun.javafx.sg.BaseNodeEffectInput;
import com.sun.javafx.sg.NodePath;
import com.sun.javafx.sg.PGNode;
import com.sun.prism.CompositeMode;
import com.sun.prism.Graphics;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.RTTexture;
import com.sun.prism.ReadbackGraphics;
import com.sun.prism.Texture;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.paint.Color;
import com.sun.scenario.effect.Blend;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.Filterable;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.prism.PrDrawable;
import com.sun.scenario.effect.impl.prism.PrEffectHelper;
import com.sun.scenario.effect.impl.prism.PrFilterContext;

import static com.sun.javafx.logging.PulseLogger.*;


/**
 * Basic implementation of node.
 */
public abstract class NGNode extends BaseNode<Graphics> {
    protected static float highestPixelScale;
    static {
        // TODO: temporary until RT-27958 is fixed. Screens may be null or could be not initialized
        // when running unit tests
        try {
            for (Screen s : Screen.getScreens()) {
                highestPixelScale = Math.max(s.getScale(), highestPixelScale);
            }
        } catch (RuntimeException ex) {
            System.err.println("WARNING: unable to get max pixel scale for screens");
            highestPixelScale = 1.0f;
        }
    }

    private final static GraphicsPipeline pipeline =
        GraphicsPipeline.getPipeline();

    private final static Boolean effectsSupported =
        (pipeline == null ? false : pipeline.isEffectSupported());

    private RectBounds opaqueRegion = null;
    private boolean opaqueRegionInvalid = true;

    protected NGNode() { }

    /***************************************************************************
     *                                                                         *
     *                     Culling Related Methods.                            *
     *                                                                         *
     **************************************************************************/
    protected void markCullRegions(
            DirtyRegionContainer drc,
            int cullingRegionsBitsOfParent,
            BaseTransform tx,
            GeneralTransform3D pvTx) {
        
        if (tx.isIdentity()) {
            TEMP_BOUNDS.deriveWithNewBounds(transformedBounds);
        } else {
            tx.transform(transformedBounds, TEMP_BOUNDS);
        }

        if (pvTx != null) {
            if (!pvTx.isIdentity()) {
                pvTx.transform(TEMP_BOUNDS, TEMP_BOUNDS);
            }
        }
        TEMP_BOUNDS.deriveWithNewBounds(TEMP_BOUNDS.getMinX(),
                TEMP_BOUNDS.getMinY(),
                0,
                TEMP_BOUNDS.getMaxX(),
                TEMP_BOUNDS.getMaxY(),
                0);

        cullingBits = 0;
        RectBounds region;
        int mask = 0x1; // Check only for intersections
        for(int i = 0; i < drc.size(); i++) {
            region = drc.getDirtyRegion(i);
            if (region == null || region.isEmpty()) {
                break;
            }
            if (cullingRegionsBitsOfParent == -1 ||
                (cullingRegionsBitsOfParent & mask) != 0) {
                setCullBits(TEMP_BOUNDS, i, region);
            }
            mask = mask << 2;
        }//for
        
        // If we are going to cull a node/group that's dirty,
        // make sure it's dirty flags are properly cleared.
        if (cullingBits == 0 && (dirty != DirtyFlag.CLEAN || childDirty)) {
            clearDirtyTree();
        }
        
        if (debug) {
            System.out.printf("%s bits: %s bounds: %s\n",
                this, Integer.toBinaryString(cullingBits), TEMP_BOUNDS);
        }
    }


    public void drawCullBits(Graphics g) {
        if (cullingBits == 0){
            g.setPaint(new Color(0, 0, 0, .3f));
        } else {
            g.setPaint(new Color(0, 1, 0, .3f));
        }
        g.fillRect(
                transformedBounds.getMinX(),
                transformedBounds.getMinY(),
                transformedBounds.getWidth(),
                transformedBounds.getHeight());
    }

    /**
     * Called <strong>after</strong> preCullingBits in order to get the node
     * from which we should begin drawing. This is our support for occlusion culling.
     * This should only be called on the root node.
     *
     * @param path node path to store the node path
     * @return new node path (preferably == path) with first node being a child of this node. Null if renderRoot == this.
     */
    public final NodePath<NGNode> getRenderRoot(NodePath<NGNode> path, RectBounds dirtyRegion, int cullingIndex, BaseTransform tx,
                                      GeneralTransform3D pvTx) {
        // The occlusion culling support depends on dirty opts being enabled
        if (PrismSettings.occlusionCullingEnabled) {
            NodePath<NGNode> rootPath = computeRenderRoot(path, dirtyRegion, cullingIndex, tx, pvTx);
            if (rootPath != null) {
                rootPath.removeRoot();
                return rootPath.size() != 0 ? rootPath : null;
            }
        }
        return null;
    }
    
    /**
     * Searches for the last node that covers all of the specified dirty region with opaque region,
     * in this node's subtree.
     * Such node can serve as a rendering root as all nodes preceding the node will be covered by it. 
     * @param dirtyRegion the current dirty region
     * @param cullingIndex index of culling information
     * @param tx current transform
     * @param pvTx current perspective transform
     * @return New rendering root or null if no such node exists in this subtree
     */
    protected NodePath<NGNode> computeRenderRoot(NodePath<NGNode> path, RectBounds dirtyRegion, int cullingIndex, BaseTransform tx,
                                       GeneralTransform3D pvTx) {
        return computeNodeRenderRoot(path, dirtyRegion, cullingIndex, tx, pvTx);
    }

    private static Point2D[] TEMP_POINTS2D_4 =
            new Point2D[] { new Point2D(), new Point2D(), new Point2D(), new Point2D() };

    // Whether (px, py) is clockwise or counter-clowise to a->b
    private static int ccw(double px, double py, Point2D a, Point2D b) {
        return (int)Math.signum(((b.x - a.x) * (py - a.y)) - (b.y - a.y) * (px - a.x));
    }

    private static boolean pointInConvexQuad(double x, double y, Point2D[] rect) {
        int ccw01 = ccw(x, y, rect[0], rect[1]);
        int ccw12 = ccw(x, y, rect[1], rect[2]);
        int ccw23 = ccw(x, y, rect[2], rect[3]);
        int ccw31 = ccw(x, y, rect[3], rect[0]);
       
        // Possible results after this operation:
        // 0 -> 0 (0x0)
        // 1 -> 1 (0x1)
        // -1 -> Integer.MIN_VALUE (0x80000000)
        ccw01 ^= (ccw01 >>> 1); 
        ccw12 ^= (ccw12 >>> 1); 
        ccw23 ^= (ccw23 >>> 1); 
        ccw31 ^= (ccw31 >>> 1); 
        
        final int union = ccw01 | ccw12 | ccw23 | ccw31;
        // This means all ccw* were either (-1 or 0) or (1 or 0), but not all of them were 0
        return union == 0x80000000 || union == 0x1;
        // Or alternatively...
//        return (union ^ (union << 31)) < 0;
    }
        
    /**
     * Check if this node can serve as rendering root for this dirty region.
     * @param dirtyRegion the current dirty region
     * @param cullingIndex index of culling information, -1 means culling information should not be used
     * @param tx current transform
     * @param pvTx current perspective transform
     * @return this node if this node's opaque region covers the specified dirty region. Null otherwise.
     */
    protected final NodePath<NGNode> computeNodeRenderRoot(NodePath<NGNode> path, RectBounds dirtyRegion, int cullingIndex, BaseTransform tx,
                                       GeneralTransform3D pvTx) {
        
        // Nodes outside of the dirty region can be excluded immediately.
        // This can be used only if the culling information is provided.
        if (cullingIndex != -1) {
            final int bits = cullingBits >> (cullingIndex * 2);
            if ((bits & 0x11) == 0x00) { // The node is neither fully interior (0x10)
                                         // nor intersecting (0x01) the dirty region 
                return null;
            }
        }
        
        if (!isVisible()) {
            return null;
        }

        // Walk down the tree to the very end. Then working our way back up to
        // the top of the tree, look for the first node which which marked as
        // dirty opts 01, and then return it.
        final RectBounds opaqueRegion = getOpaqueRegion();
        if (opaqueRegion == null) return null;
        
        final BaseTransform localToParentTx = getTransform();

        BaseTransform localToSceneTx = TEMP_TRANSFORM.deriveWithNewTransform(tx).deriveWithConcatenation(localToParentTx);
        
        // Now check if the dirty region is fully contained in our opaque region.
        if (checkBoundsInQuad(opaqueRegion, dirtyRegion, localToSceneTx, pvTx)) {
                path.add(this);
                return path;
        }

        return null;
    }
    
    protected static boolean checkBoundsInQuad(RectBounds untransformedQuad,
            RectBounds innerBounds, BaseTransform tx, GeneralTransform3D pvTx) {
        
        if ((pvTx == null || pvTx.isIdentity()) && (tx.getType() & ~(BaseTransform.TYPE_TRANSLATION
                | BaseTransform.TYPE_QUADRANT_ROTATION
                | BaseTransform.TYPE_MASK_SCALE)) == 0) {
            //If there's no pvTx and there's simple transformation that will result in axis-aligned rectangle,
            // we can do a quick test by using bound.contains()
            if (tx.isIdentity()) {
                TEMP_BOUNDS.deriveWithNewBounds(untransformedQuad);
            } else {
                tx.transform(untransformedQuad, TEMP_BOUNDS);
            }

            TEMP_BOUNDS.deriveWithNewBounds(TEMP_BOUNDS.getMinX(),
                    TEMP_BOUNDS.getMinY(),
                    0,
                    TEMP_BOUNDS.getMaxX(),
                    TEMP_BOUNDS.getMaxY(),
                    0);

            return (TEMP_BOUNDS.contains(innerBounds.getMinX(), innerBounds.getMinY())
                    && TEMP_BOUNDS.contains(innerBounds.getMaxX(), innerBounds.getMaxY()));
        } else {
            TEMP_POINTS2D_4[0].setLocation(untransformedQuad.getMinX(), untransformedQuad.getMinY());
            TEMP_POINTS2D_4[1].setLocation(untransformedQuad.getMaxX(), untransformedQuad.getMinY());
            TEMP_POINTS2D_4[2].setLocation(untransformedQuad.getMaxX(), untransformedQuad.getMaxY());
            TEMP_POINTS2D_4[3].setLocation(untransformedQuad.getMinX(), untransformedQuad.getMaxY());

            for (Point2D p : TEMP_POINTS2D_4) {
                tx.transform(p, p);
                if (pvTx != null) {
                    pvTx.transform(p, p);
                }
            }

            return (pointInConvexQuad(innerBounds.getMinX(), innerBounds.getMinY(), TEMP_POINTS2D_4)
                    && pointInConvexQuad(innerBounds.getMaxX(), innerBounds.getMinY(), TEMP_POINTS2D_4)
                    && pointInConvexQuad(innerBounds.getMaxX(), innerBounds.getMaxY(), TEMP_POINTS2D_4)
                    && pointInConvexQuad(innerBounds.getMinX(), innerBounds.getMaxY(), TEMP_POINTS2D_4));
        }
    }

    final RectBounds getOpaqueRegion() {
        if (opaqueRegionInvalid) {
            opaqueRegion = computeOpaqueRegion(opaqueRegion);
            opaqueRegionInvalid = false;
        }
        return opaqueRegion;
    }

    protected void invalidateOpaqueRegion() {
        opaqueRegionInvalid = true;
    }

    protected RectBounds computeOpaqueRegion(RectBounds opaqueRegion) {
        return null;
    }

    /***************************************************************************
     *                                                                         *
     *                     Rendering Related Methods.                          *
     *                                                                         *
     **************************************************************************/

    // This node requires 2D graphics state for rendering
    boolean isShape3D() {
        return false;
    }

    @Override
    protected void doRender(Graphics g) {

        g.setState3D(isShape3D());

        boolean preCullingTurnedOff = false;
        if (PrismSettings.dirtyOptsEnabled) {
            if (g.hasPreCullingBits()) {
                //preculling bits available
                if ((cullingBits & (0x3 << (g.getClipRectIndex() * 2))) == 0) {
                    // If no culling bits are set for this region, this group
                    // does not intersect (nor is covered by) the region
                    return;
                } else if ((cullingBits & (0x2 << (g.getClipRectIndex() * 2))) != 0) {
                    // When this group is fully covered by the region,
                    // turn off the culling checks in the subtree, as everything
                    // gets rendered
                    g.setHasPreCullingBits(false);
                    preCullingTurnedOff = true;
                }
            }
        }

        // Apply Depth test for this node
        // (note that this will only be used if we have a depth buffer for the
        // surface to which we are rendering)
        g.setDepthTest(isDepthTest());

        // save current transform state
        BaseTransform prevXform = g.getTransformNoClone();

        double mxx = prevXform.getMxx();
        double mxy = prevXform.getMxy();
        double mxz = prevXform.getMxz();
        double mxt = prevXform.getMxt();

        double myx = prevXform.getMyx();
        double myy = prevXform.getMyy();
        double myz = prevXform.getMyz();
        double myt = prevXform.getMyt();

        double mzx = prevXform.getMzx();
        double mzy = prevXform.getMzy();
        double mzz = prevXform.getMzz();
        double mzt = prevXform.getMzt();

        // filters are applied in the following order:
        //   transform
        //   blend mode
        //   opacity
        //   cache
        //   clip
        //   effect
        // The clip must be below the cache filter, as this is expected in the
        // BaseCacheFilter in order to apply scrolling optimization
        g.transform(getTransform());
        if (g instanceof ReadbackGraphics && needsBlending()) {
            renderNodeBlendMode(g);
        } else if (getOpacity() < 1f) {
            renderOpacity(g);
        } else if (getCacheFilter() != null) {
            renderCached(g);
        } else if (getClipNode() != null) {
            renderClip(g);
        } else if (getEffectFilter() != null && effectsSupported) {
            renderEffect(g);
        } else {
            renderContent(g);
        }

        if (preCullingTurnedOff) {
            g.setHasPreCullingBits(true);
        }
        
        // restore previous transform state
        g.setTransform3D(mxx, mxy, mxz, mxt,
                         myx, myy, myz, myt,
                         mzx, mzy, mzz, mzt);
        
        if (PULSE_LOGGING_ENABLED) PULSE_LOGGER.renderIncrementCounter("Nodes rendered");
    }

    /**
     * Return true if this node has a blend mode that requires special
     * processing.
     * Regular nodes can handle null or SRC_OVER just by rendering into
     * the existing buffer.
     * Groups override this since they must collect their children into
     * a single rendering pass if their mode is explicitly SRC_OVER.
     * @return true if this node needs special blending support
     */
    protected boolean needsBlending() {
        Blend.Mode mode = getNodeBlendMode();
        return (mode != null && mode != Blend.Mode.SRC_OVER);
    }

    private void renderNodeBlendMode(Graphics g) {
        // The following is safe; curXform will not be mutated below
        BaseTransform curXform = g.getTransformNoClone();

        BaseBounds clipBounds = getClippedBounds(new RectBounds(), curXform);
        if (clipBounds.isEmpty()) {
            clearDirtyTree();
            return;
        }

        if (!isReadbackSupported(g)) {
            if (getOpacity() < 1f) {
                renderOpacity(g);
            } else if (getClipNode() != null) {
                renderClip(g);
            } else {
                renderContent(g);
            }
            return;
        }

        // TODO: optimize this (RT-26936)
        // Extract clip bounds
        Rectangle clipRect = new Rectangle(clipBounds);
        clipRect.intersectWith(PrEffectHelper.getGraphicsClipNoClone(g));

        // render the node content into the first offscreen image
        FilterContext fctx = getFilterContext(g);
        PrDrawable contentImg = (PrDrawable)
            Effect.getCompatibleImage(fctx, clipRect.width, clipRect.height);
        if (contentImg == null) {
            clearDirtyTree();
            return;
        }
        Graphics gContentImg = contentImg.createGraphics();
        gContentImg.setHasPreCullingBits(g.hasPreCullingBits());
        gContentImg.setClipRectIndex(g.getClipRectIndex());
        gContentImg.translate(-clipRect.x, -clipRect.y);
        gContentImg.transform(curXform);
        if (getOpacity() < 1f) {
            renderOpacity(gContentImg);
        } else if (getCacheFilter() != null) {
            renderCached(gContentImg);
        } else if (getClipNode() != null) {
            renderClip(g);
        } else if (getEffectFilter() != null) {
            renderEffect(gContentImg);
        } else {
            renderContent(gContentImg);
        }

        // the above image has already been rendered in device space, so
        // just translate to the node origin in device space here...
        RTTexture bgRTT = ((ReadbackGraphics) g).readBack(clipRect);
        PrDrawable bgPrD = PrDrawable.create(fctx, bgRTT);
        Blend blend = new Blend(getNodeBlendMode(),
                                new PassThrough(bgPrD, clipRect),
                                new PassThrough(contentImg, clipRect));
        CompositeMode oldmode = g.getCompositeMode();
        g.setTransform(null);
        g.setCompositeMode(CompositeMode.SRC);
        PrEffectHelper.render(blend, g, 0, 0, null);
        g.setCompositeMode(oldmode);
        // transform state will be restored in render() method above...

        Effect.releaseCompatibleImage(fctx, contentImg);
        ((ReadbackGraphics) g).releaseReadBackBuffer(bgRTT);
    }

    private void renderRectClip(Graphics g, NGRectangle clipNode) {
        BaseBounds newClip = clipNode.getShape().getBounds();
        if (!clipNode.getTransform().isIdentity()) {
            newClip = clipNode.getTransform().transform(newClip, newClip);
        }
        final BaseTransform curXform = g.getTransformNoClone();
        final Rectangle curClip = g.getClipRectNoClone();
        newClip = curXform.transform(newClip, newClip);
        newClip.intersectWith(PrEffectHelper.getGraphicsClipNoClone(g));
        if (newClip.isEmpty() ||
            newClip.getWidth() == 0 ||
            newClip .getHeight() == 0) {
            clearDirtyTree();
            return;
        }
        // REMIND: avoid garbage by changing setClipRect to accept xywh
        g.setClipRect(new Rectangle(newClip));
        renderForClip(g);
        g.setClipRect(curClip);
        clipNode.clearDirty(); // as render() is not called on the clipNode,
                               // make sure the dirty flags are cleared
    }

    private void renderClip(Graphics g) {
        //  if clip's opacity is 0 there's nothing to render
        if (getClipNode().getOpacity() == 0.0) {
            clearDirtyTree();
            return;
        }

        // The following is safe; curXform will not be mutated below
        BaseTransform curXform = g.getTransformNoClone();

        BaseBounds clipBounds = getClippedBounds(new RectBounds(), curXform);
        if (clipBounds.isEmpty()) {
            clearDirtyTree();
            return;
        }

        if (getClipNode() instanceof NGRectangle) {
            // optimized case for rectangular clip
            NGRectangle rectNode = (NGRectangle)getClipNode();
            if (rectNode.isRectClip(curXform, false)) {
                renderRectClip(g, rectNode);
                return;
            }
        }

        // TODO: optimize this (RT-26936)
        // Extract clip bounds
        Rectangle clipRect = new Rectangle(clipBounds);
        clipRect.intersectWith(PrEffectHelper.getGraphicsClipNoClone(g));

        if (!curXform.is2D()) {
            Rectangle savedClip = g.getClipRect();
            g.setClipRect(clipRect);
            NodeEffectInput clipInput =
                new NodeEffectInput((NGNode) getClipNode(),
                                    NodeEffectInput.RenderType.FULL_CONTENT);
            NodeEffectInput nodeInput =
                new NodeEffectInput(this,
                                    NodeEffectInput.RenderType.CLIPPED_CONTENT);
            Blend blend = new Blend(Blend.Mode.SRC_IN, clipInput, nodeInput);
            PrEffectHelper.render(blend, g, 0, 0, null);
            clipInput.flush();
            nodeInput.flush();
            g.setClipRect(savedClip);
            // There may have been some errors in the application of the
            // effect and we would not know to what extent the nodes were
            // rendered and cleared or left dirty.  clearDirtyTree() will
            // clear both this node its clip node, and it will not recurse
            // to the children unless they are still marked dirty.  It should
            // be cheap if there was no problem and thorough if there was...
            clearDirtyTree();
            return;
        }

        // render the node content into the first offscreen image
        FilterContext fctx = getFilterContext(g);
        PrDrawable contentImg = (PrDrawable)
            Effect.getCompatibleImage(fctx, clipRect.width, clipRect.height);
        if (contentImg == null) {
            clearDirtyTree();
            return;
        }
        Graphics gContentImg = contentImg.createGraphics();
        gContentImg.setExtraAlpha(g.getExtraAlpha());
        gContentImg.setHasPreCullingBits(g.hasPreCullingBits());
        gContentImg.setClipRectIndex(g.getClipRectIndex());
        gContentImg.translate(-clipRect.x, -clipRect.y);
        gContentImg.transform(curXform);
        renderForClip(gContentImg);

        // render the mask (clipNode) into the second offscreen image
        PrDrawable clipImg = (PrDrawable)
            Effect.getCompatibleImage(fctx, clipRect.width, clipRect.height);
        if (clipImg == null) {
            getClipNode().clearDirtyTree();
            Effect.releaseCompatibleImage(fctx, contentImg);
            return;
        }
        Graphics gClipImg = clipImg.createGraphics();
        gClipImg.translate(-clipRect.x, -clipRect.y);
        gClipImg.transform(curXform);
        getClipNode().render(gClipImg);

        // the above images have already been rendered in device space, so
        // just translate to the node origin in device space here...
        g.setTransform(null);
        Blend blend = new Blend(Blend.Mode.SRC_IN,
                                new PassThrough(clipImg, clipRect),
                                new PassThrough(contentImg, clipRect));
        PrEffectHelper.render(blend, g, 0, 0, null);
        // transform state will be restored in render() method above...

        Effect.releaseCompatibleImage(fctx, contentImg);
        Effect.releaseCompatibleImage(fctx, clipImg);
    }

    void renderForClip(Graphics g) {
        if (getEffectFilter() != null) {
            renderEffect(g);
        } else {
            renderContent(g);
        }
    }

    private void renderOpacity(Graphics g) {
        if (getEffectFilter() != null ||
            getCacheFilter() != null ||
            getClipNode() != null || 
            !hasOverlappingContents())
        {
            // if the node has a non-null effect or cached==true, we don't
            // need to bother rendering to an offscreen here because the
            // contents will be flattened as part of rendering the effect
            // (or creating the cached image)
            float ea = g.getExtraAlpha();
            g.setExtraAlpha(ea*getOpacity());
            if (getCacheFilter() != null) {
                renderCached(g);
            } else if (getClipNode() != null) {
                renderClip(g);
            } else if (getEffectFilter() != null) {
                renderEffect(g);
            } else {
                renderContent(g);
            }
            g.setExtraAlpha(ea);
            return;
        }

        FilterContext fctx = getFilterContext(g);
        BaseTransform curXform = g.getTransformNoClone();
        BaseBounds bounds = getContentBounds(new RectBounds(), curXform);
        Rectangle r = new Rectangle(bounds);
        r.intersectWith(PrEffectHelper.getGraphicsClipNoClone(g));
        PrDrawable img = (PrDrawable)
            Effect.getCompatibleImage(fctx, r.width, r.height);
        if (img == null) {
            return;
        }
        Graphics gImg = img.createGraphics();
        gImg.setHasPreCullingBits(g.hasPreCullingBits());
        gImg.setClipRectIndex(g.getClipRectIndex());
        gImg.translate(-r.x, -r.y);
        gImg.transform(curXform);
        renderContent(gImg);
        // img contents have already been rendered in device space, so
        // just translate to the node origin in device space here...
        g.setTransform(null);
        float ea = g.getExtraAlpha();
        g.setExtraAlpha(getOpacity()*ea);
        g.drawTexture(img.getTextureObject(), r.x, r.y, r.width, r.height);
        g.setExtraAlpha(ea);
        // transform state will be restored in render() method above...
        Effect.releaseCompatibleImage(fctx, img);
    }

    private void renderCached(Graphics g) {
        // We will punt on 3D completely for cahcing.
        // The first check is for any of its children contains a 3D Transform.
        // The second check is for any of its parents and itself has a 3D Transform
        if (isContentBounds2D() && g.getTransformNoClone().is2D()) {
            ((CacheFilter) getCacheFilter()).render(g);
        } else {
            renderContent(g);
        }
    }

    protected void renderEffect(Graphics g) {
        ((EffectFilter)getEffectFilter()).render(g);
    }

    protected abstract void renderContent(Graphics g);

    protected abstract boolean hasOverlappingContents();

    /***************************************************************************
     *                                                                         *
     *                       Static Helper Methods.                            *
     *                                                                         *
     **************************************************************************/

    boolean isReadbackSupported(Graphics g) {
        return ((g instanceof ReadbackGraphics) &&
                ((ReadbackGraphics) g).canReadBack());
    }

    /***************************************************************************
     *                                                                         *
     *                      Filters (Cache, Effect, etc).                      *
     *                                                                         *
     **************************************************************************/

    @Override protected BaseCacheFilter createCacheFilter(PGNode.CacheHint cacheHint) {
        return new CacheFilter(this, cacheHint);
    }

    @Override protected BaseEffectFilter createEffectFilter(Effect effect) {
        return new EffectFilter(effect, this);
    }

    static FilterContext getFilterContext(Graphics g) {
        return PrFilterContext.getInstance(g.getAssociatedScreen());
    }

    static class CacheFilter extends BaseCacheFilter {
        // Garbage-reduction variables:
        private final RectBounds TEMP_BOUNDS = new RectBounds();
        private static final DirtyRegionContainer TEMP_CONTAINER = new DirtyRegionContainer(1);
        private static final Affine3D TEMP_CACHEFILTER_TRANSFORM = new Affine3D();
        private RTTexture tempTexture;

        CacheFilter(BaseNode node, CacheHint cacheHint) {
            super(node, cacheHint);
        }

        void render(Graphics g) {
            // The following is safe; xform will not be mutated below
            BaseTransform xform = g.getTransformNoClone();
            FilterContext fctx = getFilterContext(g);

            super.render(g, xform, fctx);
        }

        /**
         * Create the ImageData for the cached bitmap, with the specified bounds.
         */
        @Override
        protected ImageData impl_createImageData(FilterContext fctx,
                                                 Rectangle bounds)
        {
            Filterable ret;
            try {
                ret = Effect.getCompatibleImage(fctx,
                                          bounds.width, bounds.height);
                Texture cachedTex = ((PrDrawable) ret).getTextureObject();
                cachedTex.contentsUseful();
            } catch (Throwable e) {
                ret = null;
            }
            
            return new ImageData(fctx, ret, bounds);
        }
        
        /**
         * Render node to cache.
         * @param cacheData the cache
         * @param cacheBounds cache bounds
         * @param xform transformation
         * @param dirtyBounds null or dirty rectangle to be rendered
         */
        protected void impl_renderNodeToCache(ImageData cacheData,
                                              Rectangle cacheBounds,
                                              BaseTransform xform,
                                              Rectangle dirtyBounds) {
            final PrDrawable image = (PrDrawable) cacheData.getUntransformedImage();
            
            if (image != null) {
                Graphics g = image.createGraphics();
                TEMP_CACHEFILTER_TRANSFORM.setToIdentity();
                TEMP_CACHEFILTER_TRANSFORM.translate(-cacheBounds.x, -cacheBounds.y);
                if (xform != null) {
                    TEMP_CACHEFILTER_TRANSFORM.concatenate(xform);
                }
                if (dirtyBounds != null) {
                    TEMP_CONTAINER.deriveWithNewRegion((RectBounds)TEMP_BOUNDS.deriveWithNewBounds(dirtyBounds));
                    // Culling might save us a lot when there's a dirty region
                    node.doPreCulling(TEMP_CONTAINER, TEMP_CACHEFILTER_TRANSFORM, null);
                    g.setHasPreCullingBits(true);
                    g.setClipRectIndex(0);
                    g.setClipRect(dirtyBounds);
                }
                g.transform(TEMP_CACHEFILTER_TRANSFORM);
                if (node.getClipNode() != null) {
                    ((NGNode)node).renderClip(g);
                } else if (node.getEffectFilter() != null) {
                    ((NGNode)node).renderEffect(g);
                } else {
                    ((NGNode)node).renderContent(g);
                }
            }
        }

        /**
         * Render the node directly to the screen, in the case that the cached
         * image is unexpectedly null.  See RT-6428.
         * Note that for Prism, xform is not needed and is ignored.
         */
        @Override
        protected void impl_renderNodeToScreen(Object implGraphics,
                                               BaseTransform xform/* ignored */)
        {
            Graphics g = (Graphics)implGraphics;
            NGNode implNode = (NGNode)node;

            if (implNode.getEffectFilter() != null) {
                implNode.renderEffect(g);
            } else {
                implNode.renderContent(g);
            }
        }

        /**
         * Render the cached image to the screen, translated by mxt, myt.
         */
        @Override
        protected void impl_renderCacheToScreen(Object implGraphics,
                                                Filterable implImage,
                                                double mxt, double myt)
        {
            Graphics g = (Graphics)implGraphics;

            g.setTransform(screenXform.getMxx(),
                           screenXform.getMyx(),
                           screenXform.getMxy(),
                           screenXform.getMyy(),
                           mxt, myt);
            g.translate((float)cachedX, (float)cachedY);
            Texture cachedTex = ((PrDrawable)implImage).getTextureObject();
            Rectangle cachedBounds = cachedImageData.getUntransformedBounds();
            g.drawTexture(cachedTex, 0, 0,
                          cachedBounds.width, cachedBounds.height);
            // FYI: transform state is restored by the NGNode.render() method
        }

        /**
         * True if we can use scrolling optimization on this node.
         */
        @Override
        protected boolean impl_scrollCacheCapable() {
            if (!(node instanceof NGGroup)) {
                return false;
            }
            List<PGNode> children = ((NGGroup)node).getChildren();
            if (children.size() != 1) {
                return false;
            }
            NGNode child = (NGNode) children.get(0);
            if (!child.getTransform().is2D()) {
                return false;
            }
            
            NGNode clip = (NGNode) node.getClipNode();
            return clip != null && clip instanceof NGRectangle && 
                    ((NGRectangle)clip).isRectClip(BaseTransform.IDENTITY_TRANSFORM, false);
        }

        @Override
        protected void imageDataUnref() {
            if (tempTexture != null) {
                tempTexture.dispose();
                tempTexture = null;
            }
            super.imageDataUnref();
        }


        /**
         * Moves a subregion of the cache, "scrolling" the cache by x/y Delta.
         * On of xDelta/yDelta must be zero. The rest of the pixels will be cleared.
         * @param cachedImageData cache
         * @param xDelta x-axis delta
         * @param yDelta y-axis delta
         */
        @Override
        protected void impl_moveCacheBy(ImageData cachedImageData, double xDelta, double yDelta) {
            PrDrawable drawable = (PrDrawable) cachedImageData.getUntransformedImage();
            final Rectangle r = cachedImageData.getUntransformedBounds();
            int x = (int)Math.max(0, (-xDelta));
            int y = (int)Math.max(0, (-yDelta));
            int destX = (int)Math.max(0, (xDelta));
            int destY = (int) Math.max(0, yDelta);
            int w = r.width - (int) Math.abs(xDelta);
            int h = r.height - (int) Math.abs(yDelta);

            final Graphics g = drawable.createGraphics();
            if (tempTexture != null) {
                tempTexture.lock();
                if (tempTexture.isSurfaceLost()) {
                    tempTexture = null;
                }
            }
            if (tempTexture == null) {
                tempTexture = g.getResourceFactory().
                    createRTTexture(drawable.getPhysicalWidth(), drawable.getPhysicalHeight(),
                                    WrapMode.CLAMP_NOT_NEEDED);
            }
            final Graphics tempG = tempTexture.createGraphics();
            tempG.clear();
            tempG.drawTexture(drawable.getTextureObject(), 0, 0, w, h, x, y, x + w, y + h);
            tempG.sync();

            g.clear();
            g.drawTexture(tempTexture, destX, destY, destX + w, destY + h, 0, 0, w, h);
            tempTexture.unlock();
        }

        /**
         * Get the cache bounds.
         * @param bounds rectangle to store bounds to
         * @param xform transformation
         */
        @Override
        protected Rectangle impl_getCacheBounds(Rectangle bounds, BaseTransform xform) {
            final BaseBounds b = node.getClippedBounds(TEMP_BOUNDS, xform);
            bounds.setBounds(b);
            return bounds;
        }
        
    }

    protected static class EffectFilter extends BaseEffectFilter {

        EffectFilter(Effect effect, NGNode node) {
            super(effect, node);
        }

        void render(Graphics g) {
            NodeEffectInput nodeInput = (NodeEffectInput)getNodeInput();
            PrEffectHelper.render(getEffect(), g, 0, 0, nodeInput);
            nodeInput.flush();
        }

        @Override protected BaseNodeEffectInput createNodeEffectInput(BaseNode node) {
            return new NodeEffectInput((NGNode)node);
        }
    }

    /**
     * A custom effect implementation that has a filter() method that
     * simply wraps the given pre-rendered PrDrawable in an ImageData
     * and returns that result.  This is only used by the renderClip()
     * implementation so we cut some corners here (for example, we assume
     * that the given PrDrawable image is already in device space).
     */
    private static class PassThrough extends Effect {
        private PrDrawable img;
        private Rectangle bounds;

        PassThrough(PrDrawable img, Rectangle bounds) {
            this.img = img;
            this.bounds = bounds;
        }

        @Override
        public ImageData filter(FilterContext fctx,
                                BaseTransform transform,
                                Rectangle outputClip,
                                Object renderHelper,
                                Effect defaultInput)
        {
            return new ImageData(fctx, img, new Rectangle(bounds));
        }

        @Override
        public RectBounds getBounds(BaseTransform transform,
                                  Effect defaultInput)
        {
            return new RectBounds(bounds);
        }

        @Override
        public AccelType getAccelType(FilterContext fctx) {
            return AccelType.INTRINSIC;
        }

        @Override
        public boolean reducesOpaquePixels() {
            return false;
        }
        
        @Override
        public DirtyRegionContainer getDirtyRegions(Effect defaultInput, DirtyRegionPool regionPool) {
            return null; //Never called
        }
    }
}
