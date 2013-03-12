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

package com.sun.javafx.sg;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.BoxBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import com.sun.scenario.effect.Blend;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import java.util.List;

import static com.sun.javafx.logging.PulseLogger.PULSE_LOGGING_ENABLED;
import static com.sun.javafx.logging.PulseLogger.PULSE_LOGGER;

/**
 * BaseNode is the abstract base class implementing PGNode, and forming
 * the basis for Prism and Scenario render graphs. Although it is not
 * necessary to extend from BaseNode, doing so will brighten your day.
 * <p>
 * During synchronization, the FX scene graph will pass down to us
 * the transform which takes us from local space to parent space, the
 * content bounds (ie: geom bounds), and the transformed bounds
 * (ie: boundsInParent), and the clippedBounds. The effect bounds have
 * already been passed to the Effect peer (if there is one).
 * <p>
 * Whenever the transformedBounds of the BaseNode are changed, we update
 * the dirtyBounds, so that the next time we need to accumulate dirty
 * regions, we will have the information we need to make sure we create
 * an appropriate dirty region.
 * <p>
 * BaseNode maintains a single "dirty" flag, which indicates that this
 * node itself is dirty and must contribute to the dirty region. More
 * specifically, it indicates that this node is now dirty with respect
 * to the back buffer. Any rendering of the scene which will go on the
 * back buffer will cause the dirty flag to be cleared, whereas a
 * rendering of the scene which is for an intermediate image will not
 * clear this dirty flag.
 *
 */
public abstract class BaseNode<G> implements PGNode {
    public boolean debug = false;

    /**
     * Temporary bounds for use by this class or subclasses, designed to
     * reduce the amount of garbage we generate. If we get to the point
     * where we have multi-threaded rasterization, we might need to make
     * this per-instance instead of static.
     */
    protected static final BaseBounds TEMP_BOUNDS = new BoxBounds();
    protected static final RectBounds TEMP_RECT_BOUNDS = new RectBounds();
    protected static final Affine3D TEMP_TRANSFORM = new Affine3D();

    /**
     * This flag is used to indicate if the next rendering loop should clear
     * all the dirty flags. The dirty flags are meant to indicate which
     * portions of the tree are out of sync with the back buffer, and so
     * CLEAR_DIRTY should always be true when the render operation originated
     * from the back buffer, and should always be false when rendering into
     * an image or some other such operation.
     */
    private static boolean CLEAR_DIRTY = true;
    
    /**
     * The transform for this node. Although we are handed all the bounds
     * during synchronization (including the transformed bounds), we still
     * need the transform so that we can apply it to the clip and so forth
     * while accumulating dirty regions and rendering.
     */
    private BaseTransform transform = BaseTransform.IDENTITY_TRANSFORM;

    /**
     * The cached transformed bounds. This is never null, but is frequently set
     * to be invalid whenever the bounds for the node have changed. These are
     * "complete" bounds, that is, with transforms and effect and clip applied.
     * Note that this is equivalent to boundsInParent in FX.
     */
    protected BaseBounds transformedBounds = new RectBounds();

    /**
     * The cached bounds. This is never null, but is frequently set to be
     * invalid whenever the bounds for the node have changed. These are the
     * "content" bounds, that is, without transforms or filters applied.
     */
    protected BaseBounds contentBounds = new RectBounds();

    /**
     * We keep a reference to the last transform bounds that were valid
     * and known. We do this to significantly speed up the rendering of the
     * scene by culling and clipping based on "dirty" regions, which are
     * essentially the rectangle formed by the union of the dirtyBounds
     * and the transformedBounds.
     */
    private BaseBounds dirtyBounds = new RectBounds();

    /**
     * Whether the node is visible. We need to know about the visibility of
     * the node so that we can determine whether to cull it out, and perform
     * other such optimizations.
     */
    private boolean visible = true;


    protected static enum DirtyFlag {
        CLEAN,
        // Means that the node is dirty, but only because of translation
        DIRTY_BY_TRANSLATION,
        DIRTY
    }

    /**
     * Indicates that this BaseNode is itself dirty and needs its full bounds
     * included in the next repaint. This means it is dirty with respect to
     * the back buffer. We don't bother differentiating between bounds dirty
     * and visuals dirty because we can simply inspect the dirtyBounds to
     * see if it is valid. If so, then bounds must be dirty.
     */
    protected DirtyFlag dirty = DirtyFlag.DIRTY;
    
    /**
     * The parent of the node. In the case of a normal render graph node,
     * this will be an PGGroup. However, if this node is being used as
     * a clip node, then the parent is the node it is the clip for.
     */
    private BaseNode parent;
    
    /**
     * True is this node is a clip. This means the parent is clipped by this node.
     */
    private boolean isClip;

    /**
     * The node used for specifying the clipping shape for this node. If null,
     * then there is no clip.
     */
    private BaseNode clipNode;

    /**
     * The opacity of this node.
     */
    private float opacity = 1f;

    /**
     * The blend mode that controls how the pixels of this node blend into
     * the rest of the scene behind it.
     */
    private Blend.Mode nodeBlendMode;

    /**
     * The depth test flag for this node. It is used when rendering if the window
     * into which we are rendering has a depth buffer.
     */
    private boolean depthTest = true;

    /**
     * A filter used when the node is cached. If null, then the node is not
     * being cached. While in theory this could be created automatically by
     * the implementation due to some form of heuristic, currently the we
     * only set this if the application has requested that the node be cached.
     */
    private BaseCacheFilter cacheFilter;

    /**
     * A filter used whenever an effect is placed on the node. Of course
     * effects can form a kind of tree, such that this one effect might be
     * an accumulation of several different effects. This will be null if
     * there are no effects on the FX scene graph node.
     */
    private BaseEffectFilter effectFilter;

    /**
     * If this node is a PGGroup, then this flag will be used to indicate
     * whether one or more of its children is dirty. While it would seem this
     * flag should be on PGGroup, the code turns out to be a bit cleaner with
     * this flag in the BaseNode class.
     */
     protected boolean childDirty = false;

     /**
      * How many children are going to be accumulated
      */
     protected int dirtyChildrenAccumulated = 0;

     /**
      * Do not iterate over all children in group. Mark group as dirty
      * when threshold was reached. 
      */
     protected final static int DIRTY_CHILDREN_ACCUMULATED_THRESHOLD = 12;

     /**
      * Marks position of this node in dirty regions.
      */
     protected int cullingBits = 0x0;

    /***************************************************************************
     *                                                                         *
     * Implementation of the PGNode interface                                  *
     *                                                                         *
     **************************************************************************/

    /**
     * Called by the FX scene graph to tell us whether we should be visible or not.
     * @param value
     */
    public void setVisible(boolean value) {
        // If the visibility changes, we need to mark this node as being dirty.
        // If this node is being cached, changing visibility should have no
        // effect, since it doesn't affect the rendering of the content in
        // any way. If we were to release the cached image, that might thwart
        // the developer's attempt to improve performance for things that
        // rapidly appear and disappear but which are expensive to render.
        // Ancestors, of course, must still have their caches invalidated.
        if (visible != value) {
            this.visible = value;
            markDirty();
        }
    }

    /**
     * Called by the FX scene graph to tell us what our new content bounds are.
     * @param bounds must not be null
     */
    public void setContentBounds(BaseBounds bounds) {
        // Note, there isn't anything to do here. We're dirty if geom or
        // visuals or transformed bounds or effects or clip have changed.
        // There's no point dealing with it here.
        contentBounds = contentBounds.deriveWithNewBounds(bounds);
    }

    /**
     * Called by the FX scene graph to tell us what our transformed bounds are.
     * @param bounds must not be null
     */
    public void setTransformedBounds(BaseBounds bounds, boolean byTransformChangeOnly) {
        if (transformedBounds.equals(bounds)) {
            // There has been no change, so ignore. It turns out this happens
            // a lot, because when a leaf has dirty bounds, all parents also
            // assume their bounds have changed, and only when they recompute
            // their bounds do we discover otherwise. This check could happen
            // on the FX side, however, then the FX side needs to cache the
            // former content bounds at the time of the last sync or needs to
            // be able to read state back from the PG side. Yuck. Just doing
            // it here for now.
            return;
        }
        // If the transformed bounds have changed, then we need to save off the
        // transformed bounds into the dirty bounds, so that the resulting
        // dirty region will be correct. If this node is cached, we DO NOT
        // invalidate the cache. The cacheFilter will compare its cached
        // transform to the accumulated transform to determine whether the
        // cache needs to be regenerated. So we will not invalidate it here.
        if (dirtyBounds.isEmpty()) {
            dirtyBounds = dirtyBounds.deriveWithNewBounds(transformedBounds);
            dirtyBounds = dirtyBounds.deriveWithUnion(bounds);
        } else {
            // TODO I think this is vestigial from Scenario and will never
            // actually occur in real life... (RT-23956)
            dirtyBounds = dirtyBounds.deriveWithUnion(transformedBounds);
        }
        transformedBounds = transformedBounds.deriveWithNewBounds(bounds);
        if (hasVisuals() && !byTransformChangeOnly) {
            markDirty();
        }
    }

    private DirtyHint hint;
    
    /**
     * Called by the FX scene graph to tell us what our transform matrix is.
     * @param tx must not be null
     * @param transformed new transformed bounds, base on 
     */
    public void setTransformMatrix(BaseTransform tx) {
        // If the transform matrix has changed, then we need to update it,
        // and mark this node as dirty. If this node is cached, we DO NOT
        // invalidate the cache. The cacheFilter will compare its cached
        // transform to the accumulated transform to determine whether the
        // cache needs to be regenerated. So we will not invalidate it here.
        // This approach allows the cached image to be reused in situations
        // where only the translation parameters of the accumulated transform
        // are changing. The scene will still be marked dirty and cached
        // images of any ancestors will be invalidated.
        boolean useHint = false;
        
        // If the parent is cached, try to check if the transformation is only a translation
        if (parent != null && parent.cacheFilter != null) {
            if (hint == null) {
                hint = new DirtyHint();
            // If there's no hint created yet, this is the first setTransformMatrix
            // call and we have nothing to compare to yet.
            } else {
                if (transform.getMxx() == tx.getMxx()
                        && transform.getMxy() == tx.getMxy()
                        && transform.getMyy() == tx.getMyy()
                        && transform.getMyx() == tx.getMyx()
                        && transform.getMxz() == tx.getMxz()
                        && transform.getMyz() == tx.getMyz()
                        && transform.getMzx() == tx.getMzx()
                        && transform.getMzy() == tx.getMzy()
                        && transform.getMzz() == tx.getMzz()
                        && transform.getMzt() == tx.getMzt()) {
                    useHint = true;
                    hint.translateXDelta = tx.getMxt() - transform.getMxt();
                    hint.translateYDelta = tx.getMyt() - transform.getMyt();
                }
            }
        } 

        transform = transform.deriveWithNewTransform(tx);
        if (useHint) {
            markDirtyByTranslation(hint);
        } else {
            markDirty();
        }
    }

    /**
     * Called by the FX scene graph whenever the clip node for this node changes.
     * Note that BaseNode assumes that the PGNode is a BaseNode subclass.
     * @param clipNode can be null if the clip node is being cleared
     */
    public void setClipNode(PGNode clipNode) {
        // Whenever the clipNode itself has changed (that is, the reference to
        // the clipNode), we need to be sure to mark this node dirty and to
        // invalidate the cache of this node (if there is one) and all parents.
        BaseNode newClipNode = (BaseNode)clipNode;
        if (newClipNode != this.clipNode) {
            // Clear the "parent" property of the clip node, if there was one
            if (this.clipNode != null) this.clipNode.setParent(null);
            // Make the "parent" property of the clip node point to this
            if (newClipNode != null) newClipNode.setParent(this, true);
            // Keep the reference to the new clip node
            this.clipNode = newClipNode;
            // Mark this node dirty, invalidate its cache, and all parents.
            visualsChanged();
        }
    }

    /**
     * Called by the FX scene graph whenever the opacity for the node changes.
     * We create a special filter when the opacity is < 1.
     * @param opacity A value between 0 and 1.
     */
    public void setOpacity(float opacity) {
        // Check the argument to make sure it is valid.
        if (opacity < 0 || opacity > 1) {
            throw new IllegalArgumentException("Internal Error: The opacity must be between 0 and 1");
        }
        // If the opacity has changed, react. If this node is being cached,
        // then we do not want to invalidate the cache due to an opacity
        // change. However, as usual, all parent caches must be invalidated.
        if (opacity != this.opacity) {
            this.opacity = opacity;
            markDirty();
        }
    }

    /**
     * Set by the FX scene graph.
     * @param blendMode may be null to indicate "default"
     */
    public void setNodeBlendMode(Blend.Mode blendMode) {
        // If the blend mode has changed, react. If this node is being cached,
        // then we do not want to invalidate the cache due to a compositing
        // change. However, as usual, all parent caches must be invalidated.
        if (this.nodeBlendMode != blendMode) {
            this.nodeBlendMode = blendMode;
            markDirty();
        }
    }

    /**
     * Called by the FX scene graph whenever the derived depth test flag for
     * the node changes.
     * @param depthTest indicates whether to perform a depth test operation
     * (if the window has a depth buffer).
     */
    public void setDepthTest(boolean depthTest) {
        // If the depth test flag has changed, react.
        if (depthTest != this.depthTest) {
            this.depthTest = depthTest;
            // Mark this node dirty, invalidate its cache, and all parents.
            visualsChanged();
        }
    }
    
    /**
     * Called by the FX scene graph whenever "cached" or "cacheHint" changes.
     * These hints provide a way for the developer to indicate whether they
     * want this node to be cached as a raster, which can be quite a performance
     * optimization in some cases (and lethal in others).
     * @param cached specifies whether or not this node should be cached
     * @param cacheHint never null, indicates some hint as to how to cache
     */
    public void setCachedAsBitmap(boolean cached, PGNode.CacheHint cacheHint) {
        // Validate the arguments
        if (cacheHint == null) {
            throw new IllegalArgumentException("Internal Error: cacheHint must not be null");
        }
        
        if (cached) {
            if (cacheFilter == null) {
                cacheFilter = createCacheFilter(cacheHint);
                // We do not technically need to do a render pass here, but if
                // we wait for the next render pass to cache it, then we will
                // cache not the current visuals, but the visuals as defined
                // by any transform changes that happen between now and then.
                // Repainting now encourages the cached version to be as close
                // as possible to the state of the node when the cache hint
                // was set...
                markDirty();
            } else {
                if (!cacheFilter.matchesHint(cacheHint)) {
                    cacheFilter.setHint(cacheHint);
                    // Different hints may have different requirements of
                    // whether the cache is stale.  We do not have enough info
                    // right here to evaluate that, but it will be determined
                    // naturally during a repaint cycle.
                    // If the new hint is more relaxed (QUALITY => SPEED for
                    // instance) then rendering should be quick.
                    // If the new hint is more restricted (SPEED => QUALITY)
                    // then we need to render to improve the results anyway.
                    markDirty();
                }
            }
        } else {
            if (cacheFilter != null) {
                cacheFilter.dispose();
                cacheFilter = null;
                // A cache will often look worse than uncached rendering.  It
                // may look the same in some circumstances, and this may then
                // be an unnecessary rendering pass, but we do not have enough
                // information here to be able to optimize that when possible.
                markDirty();
            }
        }
    }

    /**
     * Called by the FX scene graph to set the effect.
     * @param effect
     */
    public void setEffect(Object effect) {
        // We only need to take action if the effect is different than what was
        // set previously. There are four possibilities. Of these, #1 and #3 matter:
        // 0. effectFilter == null, effect == null
        // 1. effectFilter == null, effect != null
        // 2. effectFilter != null, effectFilter.effect == effect
        // 3. effectFilter != null, effectFilter.effect != effect
        // In any case where the effect is changed, we must both invalidate
        // the cache for this node (if there is one) and all parents, and mark
        // this node as dirty.
        if (effectFilter == null && effect != null) {
            effectFilter = createEffectFilter((Effect)effect);
            visualsChanged();
        } else if (effectFilter != null && effectFilter.getEffect() != effect) {
            effectFilter.dispose();
            effectFilter = null;
            if (effect != null) {
                effectFilter = createEffectFilter((Effect)effect);
            }
            visualsChanged();
        }
    }

    /**
     * Called by the FX scene graph when an effect in the effect chain on the node
     * changes internally.
     */
    public void effectChanged() {
        visualsChanged();
    }

    /**
     * Return true if contentBounds is purely a 2D bounds, ie. it is a
     * RectBounds or its Z dimension is almost zero.
     */
    public boolean isContentBounds2D() {
        return (contentBounds.is2D()
                || (Affine3D.almostZero(contentBounds.getMaxZ())
                && Affine3D.almostZero(contentBounds.getMinZ())));
    }

    /***************************************************************************
     *                                                                         *
     * Hierarchy, visibility, and other such miscellaneous BaseNode properties *
     * not already handled by implementing PGNode interface, bounds, or dirty  *
     * region management.                                                      *
     *                                                                         *
     **************************************************************************/

    /**
     * Gets the parent of this node. The parent might be an PGGroup. However,
     * if this node is a clip node on some other node, then the node on which
     * it is set as the clip will be returned. That is, suppose some node A
     * has a clip node B. The method B.getParent() will return A.
     */
    public BaseNode getParent() { return parent; }

    /**
     * Only called by this class, or by the PGGroup class.
     */
    public void setParent(BaseNode parent) {
        setParent(parent, false);
    }
    
    private void setParent(BaseNode parent, boolean isClip) {
        this.parent = parent;
        this.isClip = isClip;
    }

    protected final Effect getEffect() { return effectFilter == null ? null : effectFilter.getEffect(); }

    /**
     * Gets whether this node's visible property is set
     */
    protected boolean isVisible() { return visible; }

    public final BaseTransform getTransform() { return transform; }
    public final float getOpacity() { return opacity; }
    public final Blend.Mode getNodeBlendMode() { return nodeBlendMode; }
    public final boolean isDepthTest() { return depthTest; }
    public final BaseCacheFilter getCacheFilter() { return cacheFilter; }
    public final BaseEffectFilter getEffectFilter() { return effectFilter; }
    public final BaseNode getClipNode() { return clipNode; }

    public BaseBounds getContentBounds(BaseBounds bounds, BaseTransform tx) {
        if (tx.isTranslateOrIdentity()) {
            bounds = bounds.deriveWithNewBounds(contentBounds);
            if (!tx.isIdentity()) {
                float translateX = (float) tx.getMxt();
                float translateY = (float) tx.getMyt();
                float translateZ = (float) tx.getMzt();
                bounds = bounds.deriveWithNewBounds(
                    bounds.getMinX() + translateX,
                    bounds.getMinY() + translateY,
                    bounds.getMinZ() + translateZ,
                    bounds.getMaxX() + translateX,
                    bounds.getMaxY() + translateY,
                    bounds.getMaxZ() + translateZ);
            }
            return bounds;
        } else {
            // This is a scale / rotate / skew transform.
            // We have contentBounds cached throughout the entire tree.
            // just walk down the tree and add everything up
            return computeBounds(bounds, tx);
        }
    }

    private BaseBounds computeBounds(BaseBounds bounds, BaseTransform tx) {
        // TODO: This code almost worked, but it ignored the local to
        // parent transforms on the nodes.  The short fix is to disable
        // this block and use the more general form below, but we need
        // to revisit this and see if we can make it work more optimally.
        // @see RT-12105 http://javafx-jira.kenai.com/browse/RT-12105
        if (false && this instanceof PGGroup) {
            List<PGNode> children = ((PGGroup)this).getChildren();
            BaseBounds tmp = TEMP_BOUNDS;
            for (int i=0; i<children.size(); i++) {
                float minX = bounds.getMinX();
                float minY = bounds.getMinY();
                float minZ = bounds.getMinZ();
                float maxX = bounds.getMaxX();
                float maxY = bounds.getMaxY();
                float maxZ = bounds.getMaxZ();
                BaseNode child = (BaseNode)children.get(i);
                bounds = child.computeBounds(bounds, tx);
                tmp = tmp.deriveWithNewBounds(minX, minY, minZ, maxX, maxY, maxZ);
                bounds = bounds.deriveWithUnion(tmp);
            }
            return bounds;
        } else {
            bounds = bounds.deriveWithNewBounds(contentBounds);
            return tx.transform(contentBounds, bounds);
        }
    }

    /**
     */
    public final BaseBounds getClippedBounds(BaseBounds bounds, BaseTransform tx) {
        BaseBounds effectBounds = getEffectBounds(bounds, tx);
        if (clipNode != null) {
            // there is a clip in place, so we will save off the effect/content
            // bounds (so as not to generate garbage) and will then get the
            // bounds of the clip node and do an intersection of the two
            float ex1 = effectBounds.getMinX();
            float ey1 = effectBounds.getMinY();
            float ez1 = effectBounds.getMinZ();
            float ex2 = effectBounds.getMaxX();
            float ey2 = effectBounds.getMaxY();
            float ez2 = effectBounds.getMaxZ();
            effectBounds = clipNode.getCompleteBounds(effectBounds, tx);
            effectBounds.intersectWith(ex1, ey1, ez1, ex2, ey2, ez2);
        }
        return effectBounds;
    }

    public final BaseBounds getEffectBounds(BaseBounds bounds, BaseTransform tx) {
        if (effectFilter != null) {
            return effectFilter.getBounds(bounds, tx);
        } else {
            return getContentBounds(bounds, tx);
        }
    }

    public final BaseBounds getCompleteBounds(BaseBounds bounds, BaseTransform tx) {
        if (tx.isIdentity()) {
            bounds = bounds.deriveWithNewBounds(transformedBounds);
            return bounds;
        } else if (transform.isIdentity()) {
            return getClippedBounds(bounds, tx);
        } else {
            double mxx = tx.getMxx();
            double mxy = tx.getMxy();
            double mxz = tx.getMxz();
            double mxt = tx.getMxt();
            double myx = tx.getMyx();
            double myy = tx.getMyy();
            double myz = tx.getMyz();
            double myt = tx.getMyt();
            double mzx = tx.getMzx();
            double mzy = tx.getMzy();
            double mzz = tx.getMzz();
            double mzt = tx.getMzt();
            BaseTransform boundsTx = tx.deriveWithConcatenation(this.transform);
            bounds = getClippedBounds(bounds, tx);
            if (boundsTx == tx) {
                tx.restoreTransform(mxx, mxy, mxz, mxt,
                                    myx, myy, myz, myt,
                                    mzx, mzy, mzz, mzt);
            }
            return bounds;
        }
    }

    /***************************************************************************
     *                                                                         *
     * Dirty States and Dirty Regions                                          *
     *                                                                         *
     **************************************************************************/

    /**
     * Invoked by subclasses whenever some change to the geometry or visuals
     * has occurred. This will mark the node as dirty and invalidate the cache.
     */
    protected void visualsChanged() {
        invalidateCache();
        markDirty();
    }

    protected void geometryChanged() {
        invalidateCache();
        if (hasVisuals()) {
            markDirty();
        }
    }
    
    /**
     * Makes this node dirty, meaning that it needs to be included in the
     * next repaint to the back buffer, and its bounds should be included
     * in the dirty region. This flag means that this node itself is dirty.
     * In contrast, the childDirty flag indicates that a child of the node
     * (maybe a distant child) is dirty. This method does not invalidate the
     * cache of this node. However, it ends up walking up the tree marking
     * all parents as having a dirty child and also invalidating their caches.
     * This method has no effect if the node is already dirty.
     */
    public final void markDirty() {
        if (dirty != DirtyFlag.DIRTY) {
            dirty = DirtyFlag.DIRTY;
            markTreeDirty();
        }
    }
    
    /**
     * Mark the node as DIRTY_BY_TRANSLATION. This will call special cache invalidation
     * @param hint 
     */
    public void markDirtyByTranslation(DirtyHint hint) {
        if (dirty == DirtyFlag.CLEAN) {
            if (parent != null && parent.dirty == DirtyFlag.CLEAN && !parent.childDirty) {
                dirty = DirtyFlag.DIRTY_BY_TRANSLATION;
                parent.childDirty = true;
                parent.dirtyChildrenAccumulated++;
                parent.invalidateCacheByTranslation(hint);
                parent.markTreeDirty();
            } else {
                markDirty();
            }
        }
    }
    
    //Mark tree dirty, but make sure this node's
    // dirtyChildrenAccumulated has not been incremented.
    // Useful when a markTree is called on a node that's not
    // the dirty source of change, e.g. group knows it has new child
    // or one of it's child has been removed
    protected final void markTreeDirtyNoIncrement() {
        if (parent != null && (!parent.childDirty || dirty == DirtyFlag.DIRTY_BY_TRANSLATION)) {
            markTreeDirty();
        }
    }
    
    /**
     * Notifies the parent (whether a PGGroup or just a BaseNode) that
     * a child has become dirty. This walk will continue all the way up
     * to the root of the tree. If a node is encountered which is already
     * dirty, or which already has childDirty set, then this loop will
     * terminate (ie: there is no point going further so we might as well
     * just bail). This method ends up invalidating the cache of each
     * parent up the tree. Since it is possible for a node to already
     * have its dirty bit set, but not have its cache invalidated, this
     * method is careful to make sure the first parent it encounters
     * which is already marked dirty still has its cache invalidated. If
     * this turns out to be expensive due to high occurrence, we can add
     * a quick "invalidated" flag to every node (at the cost of yet
     * another bit).
     */

    protected final void markTreeDirty() {
        BaseNode p = parent;
        boolean atClip = isClip;
        boolean byTranslation = dirty == DirtyFlag.DIRTY_BY_TRANSLATION;
        while (p != null && p.dirty != DirtyFlag.DIRTY && (!p.childDirty || atClip || byTranslation)) {
            if (atClip) {
                p.dirty = DirtyFlag.DIRTY;
            } else if (!byTranslation) {
                p.childDirty = true;
                p.dirtyChildrenAccumulated++;
            }
            p.invalidateCache();
            atClip = p.isClip;
            byTranslation = p.dirty == DirtyFlag.DIRTY_BY_TRANSLATION;
            p = p.parent;
        }
        // if we stopped on a parent that already has dirty children, increase it's
        // dirty children count.
        // Note that when incrementDirty is false, we dont increment in this case.
        if (p != null && p.dirty == DirtyFlag.CLEAN && !atClip && !byTranslation) {
            p.dirtyChildrenAccumulated++;
        }
        // Must make sure this happens. In some cases, a parent might
        // already be marked dirty (for example, its opacity may have
        // changed) but its cache has not been made invalid. This call
        // will make sure it is invalidated in that case
        if (p != null) p.invalidateCache();
    }
    
    /**
     * Gets whether this SGNode is clean. This will return true only if
     * this node and any / all child nodes are clean.
     */
//    public final boolean isClean() {
//        return !dirty && !childDirty;
//    }

    /**
     * Gets whether this node itself is dirty.
     */
//    public final boolean isDirty() {
//        return dirty;
//    }

    /**
     * Clears the dirty flag. This should only happen during rendering.
     */
    protected void clearDirty() {
        dirty = DirtyFlag.CLEAN;
        childDirty = false;
        dirtyBounds.makeEmpty();
        dirtyChildrenAccumulated = 0;
    }
    
    public void clearDirtyTree() {
        clearDirty();
        if (getClipNode() != null) {
            getClipNode().clearDirtyTree();
        }
        if (this instanceof PGGroup) {
            List<PGNode> children = ((PGGroup) this).getChildren();
            for (int i = 0; i < children.size(); ++i) {
                BaseNode child = ((BaseNode)children.get(i));
                if (child.dirty != DirtyFlag.CLEAN || child.childDirty) {
                    child.clearDirtyTree();
                }
            }
        }
    }
    
    /**
     * Invalidates the cache, if it is in use. There are several operations
     * which need to cause the cached raster to become invalid so that a
     * subsequent render operation will result in the cached image being
     * reconstructed.
     */
    protected final void invalidateCache() {
        if (cacheFilter != null) {
            cacheFilter.invalidate();
        }
    }
    
    /**
     * Mark the cache as invalid due to a translation of a child. The cache filter
     * might use this information for optimizations.
     * @param hint 
     */
    protected final void invalidateCacheByTranslation(DirtyHint hint) {
        if (cacheFilter != null) {
            cacheFilter.invalidateByTranslation(hint.translateXDelta, hint.translateYDelta);
        }
    }

    /**
     * Accumulates and returns the dirty regions in transformed coordinates for
     * this node. This method is designed such that a single downward traversal
     * of the tree is sufficient to update the dirty regions.
     * <p>
     * This method only accumulates dirty regions for parts of the tree which lie
     * inside the clip since there is no point in accumulating dirty regions which
     * lie outside the clip. The returned dirty regions bounds  the same object
     * as that passed into the function. The returned dirty regions bounds will
     * always be adjusted such that they do not extend beyond the clip.
     * <p>
     * The given transform is the accumulated transform up to but not including the
     * transform of this node.
     *
     * @param clip must not be null, the clip in scene coordinates, supplied by the
     *        rendering system. At most, this is usually the bounds of the window's
     *        content area, however it might be smaller.
     * @param dirtyRegionTemp must not be null, the dirty region in scene coordinates.
     *        When this method is initially invoked by the rendering system, the
     *        dirtyRegion should be marked as invalid.
     * @param dirtyRegionContainer must not be null, the container of dirty regions in scene
     *        coordinates.
     * @param tx must not be null, the accumulated transform up to but not
     *        including this node's transform. When this method concludes, it must
     *        restore this transform if it was changed within the function.
     * @param pvTx must not be null, it's the perspective transform of the current
     *        perspective camera or identity transform if parallel camera is used.
     * @return The dirty region container. If the returned value is null, then that means
     *         the clip should be used as the dirty region. This is a special
     *         case indicating that there is no more need to walk the tree but
     *         we can take a shortcut. Note that returning null is *always*
     *         safe. Returning something other than null is simply an
     *         optimization for cases where the dirty region is substantially
     *         smaller than the clip.
     * TODO: Only made non-final for the sake of testing (see javafx-sg-prism tests) (RT-23957)
     */

    public /*final*/ int accumulateDirtyRegions(final RectBounds clip,
                                                final RectBounds dirtyRegionTemp,
                                                DirtyRegionPool regionPool,
                                                final DirtyRegionContainer dirtyRegionContainer,
                                                final BaseTransform tx,
                                                final GeneralTransform3D pvTx)
    {
        // Even though a node with 0 visibility or 0 opacity doesn't get
        // rendered, it may contribute to the dirty bounds, for example, if it
        // WAS visible or if it HAD an opacity > 0 last time we rendered then
        // we must honor its dirty region. We have front-loaded this work so
        // that we don't mark nodes as having dirty flags or dirtyBounds if
        // they shouldn't contribute to the dirty region. So we can simply
        // treat all nodes, regardless of their opacity or visibility, as
        // though their dirty regions matter. They do.

        // If this node is clean then we can simply return the dirty region as
        // there is no need to walk any further down this branch of the tree.
        // The node is "clean" if neither it, nor its children, are dirty.
         if (dirty == DirtyFlag.CLEAN && !childDirty) {
             return DirtyRegionContainer.DTR_OK;
         }
         
        // We simply collect this nodes dirty region if it has its dirty flag
        // set, regardless of whether it is a group or not. However, if this
        // node is not dirty, then we can ask the accumulateGroupDirtyRegion
        // method to collect the dirty regions of the children.
        if (dirty != DirtyFlag.CLEAN) {
            return accumulateNodeDirtyRegion(clip, dirtyRegionTemp, dirtyRegionContainer, tx, pvTx);
        } else {
            assert childDirty == true; // this must be true by this point
            return accumulateGroupDirtyRegion(clip, dirtyRegionTemp, regionPool,
                                              dirtyRegionContainer, tx, pvTx);
        }
    }
    
    /**
     * Accumulates the dirty region of a node.
     * TODO: Only made protected for the sake of testing (see javafx-sg-prism tests) (RT-23957)
     */
    protected int accumulateNodeDirtyRegion(final RectBounds clip,
                                            final RectBounds dirtyRegionTemp,
                                            final DirtyRegionContainer dirtyRegionContainer,
                                            final BaseTransform tx,
                                            final GeneralTransform3D pvTx) {

        // Get the dirty bounds of this specific node in scene coordinates
        BaseBounds bb = computeDirtyRegion(dirtyRegionTemp, tx, pvTx);

        // Note: dirtyRegion is strictly a 2D operation. We simply need the largest
        // rectangular bounds of bb. Hence the Z-axis projection of bb; taking
        // minX, minY, maxX and maxY values from this point on.
        dirtyRegionTemp.setMinX(bb.getMinX());
        dirtyRegionTemp.setMinY(bb.getMinY());
        dirtyRegionTemp.setMaxX(bb.getMaxX());
        dirtyRegionTemp.setMaxY(bb.getMaxY());
        
        // If my dirty region is empty, or if it doesn't intersect with the
        // clip, then we can simply return the passed in dirty region since
        // this node's dirty region is not helpful
        if (dirtyRegionTemp.isEmpty() || clip.disjoint(dirtyRegionTemp)) {
            return DirtyRegionContainer.DTR_OK;
        }

        if (dirtyRegionTemp.getMinX() <= clip.getMinX() &&
            dirtyRegionTemp.getMinY() <= clip.getMinY() &&
            dirtyRegionTemp.getMaxX() >= clip.getMaxX() &&
            dirtyRegionTemp.getMaxY() >= clip.getMaxY()) {
            return DirtyRegionContainer.DTR_CONTAINS_CLIP;
        }

        dirtyRegionTemp.setMinX(Math.max(dirtyRegionTemp.getMinX(), clip.getMinX()));
        dirtyRegionTemp.setMinY(Math.max(dirtyRegionTemp.getMinY(), clip.getMinY()));
        dirtyRegionTemp.setMaxX(Math.min(dirtyRegionTemp.getMaxX(), clip.getMaxX()));
        dirtyRegionTemp.setMaxY(Math.min(dirtyRegionTemp.getMaxY(), clip.getMaxY()));

        dirtyRegionContainer.addDirtyRegion(dirtyRegionTemp);
        
        return DirtyRegionContainer.DTR_OK;
    }
    
    /**
     * Accumulates the dirty region of a PGGroup. This is implemented here as opposed to
     * using polymorphism because we wanted to centralize all of the dirty region
     * management code in one place, rather than having it spread between Prism,
     * Scenario, and any other future toolkits.
     * TODO: Only made protected for the sake of testing (see javafx-sg-prism tests) (RT-23957)
     */
    protected int accumulateGroupDirtyRegion(final RectBounds clip,
                                             final RectBounds dirtyRegionTemp,
                                             DirtyRegionPool regionPool,
                                             DirtyRegionContainer dirtyRegionContainer,
                                             final BaseTransform tx,
                                             final GeneralTransform3D pvTx) {
        // We should have only made it to this point if this node has a dirty
        // child. If this node itself is dirty, this method never would get called.
        // If this node was not dirty and had no dirty children, then this
        // method never should have been called. So at this point, the following
        // assertions should be correct.
        assert childDirty == true;
        assert dirty == DirtyFlag.CLEAN;

        int status = DirtyRegionContainer.DTR_OK;

        if (dirtyChildrenAccumulated > DIRTY_CHILDREN_ACCUMULATED_THRESHOLD) {
            status = accumulateNodeDirtyRegion(clip, dirtyRegionTemp, dirtyRegionContainer, tx, pvTx);
            return status;
        }

        // If we got here, then we are following a "bread crumb" trail down to
        // some child (perhaps distant) which is dirty. So we need to iterate
        // over all the children and accumulate their dirty regions. Before doing
        // so we, will save off the transform state and restore it after having
        // called all the children.
        double mxx = tx.getMxx();
        double mxy = tx.getMxy();
        double mxz = tx.getMxz();
        double mxt = tx.getMxt();
        
        double myx = tx.getMyx();
        double myy = tx.getMyy();
        double myz = tx.getMyz();
        double myt = tx.getMyt();
        
        double mzx = tx.getMzx();
        double mzy = tx.getMzy();
        double mzz = tx.getMzz();
        double mzt = tx.getMzt();
        BaseTransform renderTx = tx;
        if (this.transform != null) renderTx = renderTx.deriveWithConcatenation(this.transform);

        // If this group node has a clip, then we will perform some special
        // logic which will cause the dirty region accumulation loops to run
        // faster. We already have a system whereby if a node determines that
        // its dirty region exceeds that of the clip, it simply returns null,
        // short circuiting the accumulation process. We extend that logic
        // here by also taking into account the clipNode on the group. If
        // there is a clip node, then we will union the bounds of the clip
        // node (in boundsInScene space) with the current clip and pass this
        // new clip down to the children. If they determine that their dirty
        // regions exceed the bounds of this new clip, then they will return
        // null. We'll catch that here, and use that information to know that
        // we ought to simply accumulate the bounds of this group as if it
        // were dirty. This process will do all the other optimizations we
        // already have in place for getting the normal dirty region.
        RectBounds myClip = clip;
        //Save current dirty region so we can fast-reset to (something like) the last state
        //and possibly save a few intersects() calls        
        
        DirtyRegionContainer originalDirtyRegion = null;
        if (effectFilter != null) {
            try {
                myClip = new RectBounds();
                BaseBounds myClipBaseBounds = renderTx.inverseTransform(clip, TEMP_BOUNDS);
                myClip.setBounds(myClipBaseBounds.getMinX(),
                                 myClipBaseBounds.getMinY(),
                                 myClipBaseBounds.getMaxX(),
                                 myClipBaseBounds.getMaxY());
            } catch (NoninvertibleTransformException ex) {
                return DirtyRegionContainer.DTR_OK;
            }

            renderTx = BaseTransform.IDENTITY_TRANSFORM;
            originalDirtyRegion = dirtyRegionContainer;
            dirtyRegionContainer = regionPool.checkOut();
        } else if (clipNode != null) {
            originalDirtyRegion = dirtyRegionContainer;
            myClip = new RectBounds();
            BaseBounds clipBounds = clipNode.getCompleteBounds(myClip, renderTx);
            pvTx.transform(clipBounds, clipBounds);
            myClip.deriveWithNewBounds(clipBounds.getMinX(), clipBounds.getMinY(), 0,
                                         clipBounds.getMaxX(), clipBounds.getMaxY(), 0);
            myClip.intersectWith(clip);
            dirtyRegionContainer = regionPool.checkOut();
        }
        

        //Accumulate also removed children to dirty region.
        List<PGNode> removed = ((PGGroup) this).getRemovedChildren();
        if (removed != null) {
            BaseNode removedChild;
            for (int i = removed.size() - 1; i >= 0; --i) {
                removedChild = (BaseNode) removed.get(i);
                removedChild.dirty = DirtyFlag.DIRTY;
                    status = removedChild.accumulateDirtyRegions(myClip,
                            dirtyRegionTemp,regionPool, dirtyRegionContainer, renderTx, pvTx);
                    if (status == DirtyRegionContainer.DTR_CONTAINS_CLIP) {
                        break;
                    }
            }
        }
  
        List<PGNode> children = ((PGGroup) this).getChildren();
        int num = children.size();
            for (int i=0; i<num && status == DirtyRegionContainer.DTR_OK; i++) {
            BaseNode child = (BaseNode) children.get(i);
            // The child will check the dirty bits itself. If we tested it here
            // (as we used to), we are just doing the check twice. True, it might
            // mean fewer method calls, but hotspot will probably inline this all
            // anyway, and doing the check in one place is less error prone.            
                status = child.accumulateDirtyRegions(myClip, dirtyRegionTemp, regionPool,
                                                      dirtyRegionContainer, renderTx, pvTx);
                if (status == DirtyRegionContainer.DTR_CONTAINS_CLIP) {
                break;
            }
        }

        if (effectFilter != null && status == DirtyRegionContainer.DTR_OK) {
            //apply effect on effect dirty regions
            applyEffect(effectFilter, dirtyRegionContainer, regionPool);
            
            if (clipNode != null) {
                myClip = new RectBounds();
                BaseBounds clipBounds = clipNode.getCompleteBounds(myClip, renderTx);
                applyClip(clipBounds, dirtyRegionContainer);
            }

            //apply transform on effect dirty regions
            applyTransform(tx, dirtyRegionContainer);
            
            originalDirtyRegion.merge(dirtyRegionContainer);
            regionPool.checkIn(dirtyRegionContainer);
        }
            
        // If the process of applying the transform caused renderTx to not equal
        // tx, then there is no point restoring it since it will be a different
        // reference and will therefore be gc'd.
        if (renderTx == tx) {
            tx.restoreTransform(mxx, mxy, mxz, mxt, myx, myy, myz, myt, mzx, mzy, mzz, mzt);
        }

        // If the dirty region is null and there is a clip node specified, then what
        // happened is that the dirty region of content within this group exceeded
        // the clip of this group, and thus, we should accumulate the bounds of
        // this group into the dirty region. If the bounds of the group exceeds
        // the bounds of the dirty region, then we end up returning null in the
        // end. But the implementation of accumulateNodeDirtyRegion handles this.
        if (clipNode != null && effectFilter == null) {
            if (status == DirtyRegionContainer.DTR_CONTAINS_CLIP) {
                status = accumulateNodeDirtyRegion(clip, dirtyRegionTemp, originalDirtyRegion, tx, pvTx);
            } else {
                originalDirtyRegion.merge(dirtyRegionContainer);
            }
            regionPool.checkIn(dirtyRegionContainer);
        }        
        return status;
    }

    /**
     * Computes the dirty region for this Node. The specified region is in
     * scene coordinates. The specified tx can be used to convert local bounds
     * to scene bounds (it includes everything up to but not including my own
     * transform).
     * @param pvTx must not be null, it's the perspective transform of the current
     *        perspective camera or identity transform if parallel camera is used.
     */
    private BaseBounds computeDirtyRegion(BaseBounds region,
                                          BaseTransform tx,
                                          GeneralTransform3D pvTx)
    {
        // The passed in region is a scratch object that exists for me to use,
        // such that I don't have to create a temporary object. So I just
        // hijack it right here giving it the dirtyBounds.
        if (!dirtyBounds.isEmpty()) {
            region = region.deriveWithNewBounds(dirtyBounds);
        } else {
            // If dirtyBounds is empty, then we will simply set the bounds to
            // be the same as the transformedBounds (since that means the bounds
            // haven't changed and right now we don't support dirty sub regions
            // for generic nodes). This can happen if, for example, this is
            // a group with a clip and the dirty area of child nodes within
            // the group exceeds the bounds of the clip on the group. Just trust me.
            region = region.deriveWithNewBounds(transformedBounds);
        }
        
        // We shouldn't do anything with empty region, as we may accidentally make
        // it non empty or turn it into some nonsense (like (-1,-1,0,0) )
        if (!region.isEmpty()) {
            // Now that we have the dirty region, we will simply apply the tx
            // to it (after slightly padding it for good luck) to get the scene
            // coordinates for this. Note that for SGText this implementation should not
            // be used (as this shortcut doesn't seem to work well with text).
            region = region.deriveWithNewBounds(region.getMinX() - 1.0f,
                    region.getMinY() - 1.0f,
                    region.getMinZ(),
                    region.getMaxX() + 1.0f,
                    region.getMaxY() + 1.0f,
                    region.getMaxZ());
            region = tx.transform(region, region);
            region = pvTx.transform(region, region);
        }
        return region;
    }
    
    /**
     * Marks if the node has some visuals and that the bounds change
     * should be taken into account when using the dirty region.
     * This will be false for NGGroup (but not for NGRegion)
     * @return true if the node has some visuals
     */
    protected boolean hasVisuals() {
        return true;
    }

    /***************************************************************************
     *                                                                         *
     * Culling                                                                 *
     *                                                                         *
     **************************************************************************/

    /**
      * Culling support for multiple dirty regions.
      * Set culling bits for the whole graph.
      * @param drc Array of dirty regions.
      * @param tx The transform for this render operation.
      * @param pvTx Perspective camera transformation.
      */
     public void doPreCulling(DirtyRegionContainer drc, BaseTransform tx, GeneralTransform3D pvTx) {
         markCullRegions(drc, -1, tx, pvTx);
     }

     /**
      * Set culling bits for the node.
      * @param bounds Bounds of the node.
      * @param regionIndex Index of dirty region used.
      * @param region Dirty region being processed.
      * @return Bit setting encoding node position to dirty region (without the shift)
      */
     protected int setCullBits(BaseBounds bounds, int regionIndex, RectBounds region) {
         int b = 0;
         if (region != null && !region.isEmpty()) {
             if (region.intersects(bounds)) {
                 b = 1;
                 if (region.contains(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight())) {
                     b = 2;
                 }
                 cullingBits = cullingBits |  (b << (2 * regionIndex));
             }
         }
         return b;
     }

     /**
      * Marks placement of the node in dirty region encoded into 2 bit flag:
      * 00 - node outside dirty region
      * 01 - node intersecting dirty region
      * 11 - node completely within dirty region
      *
      * 32 bits = 15 regions max. * 2 bit each.
      *
      * @param dirtyRegions The array of dirty regions.
      * @param cullingRegionsOfParent culling bits of parent. -1 if there's no parent.
      * @param tx The transform for this render operation.
      * @param pvTx Perspective camera transform.
      */
     protected abstract void markCullRegions(
             DirtyRegionContainer drc,
             int cullingRegionsBitsOfParent,
             BaseTransform tx, GeneralTransform3D pvTx);

     /**
      * Helper method draws culling bits for each node.
      * @param g Graphics.
      */
     public abstract void drawCullBits(G g);

    /***************************************************************************
     *                                                                         *
     * Rendering                                                               *
     *                                                                         *
     **************************************************************************/

    /**
     * Render the tree of nodes to the specified G (graphics) object
     * descending from this node as the root. This method is designed to avoid
     * generated trash as much as possible while descending through the
     * render graph while rendering. This is the appropriate method both to
     * initiate painting of an entire scene, and for a branch. The PGGroup
     * implementation must call this method on each child, not doRender directly.
     *
     * @param g The graphics object we're rendering to. This must never be null.
     */
    public final void render(G g) {
        if (PULSE_LOGGING_ENABLED) PULSE_LOGGER.renderIncrementCounter("Nodes visited during render");
        if (debug) System.out.println("render called on " + getClass().getSimpleName());
        // Clear the visuals changed flag
        if (CLEAR_DIRTY) clearDirty();
        // If it isn't visible, then punt
        if (!visible || opacity == 0f) return;
        // If we are supposed to cull, then go ahead and do so
//        if (cull(clip, tx)) return;

        // We know that we are going to render this node, so we call the
        // doRender method, which subclasses implement to do the actual
        // rendering work.
        doRender(g);
    }

    /**
     * Invoked only by the final render method, this abstract method must
     * be implemented by subclasses to actually render the node. Implementations
     * of this method should make sure to save & restore the transform state.
     */
    protected abstract void doRender(G g);

    /***************************************************************************
     *                                                                         *
     * Stuff                                                                   *
     *                                                                         *
     **************************************************************************/

    protected abstract BaseCacheFilter createCacheFilter(PGNode.CacheHint cacheHint);
    protected abstract BaseEffectFilter createEffectFilter(Effect effect);

    public void release() {
    }
    
    public void applyTransform(final BaseTransform tx, DirtyRegionContainer drc) {
        for (int i = 0; i < drc.size(); i++) {
            drc.setDirtyRegion(i, (RectBounds) tx.transform(drc.getDirtyRegion(i), drc.getDirtyRegion(i)));
            if (drc.checkAndClearRegion(i)) {
                --i;
            }
        }
    }
    
    public void applyClip(final BaseBounds clipBounds, DirtyRegionContainer drc) {
        for (int i = 0; i < drc.size(); i++) {
            drc.getDirtyRegion(i).intersectWith(clipBounds);
            if (drc.checkAndClearRegion(i)) {
                --i;
            }
        }
    }

    public void applyEffect(final BaseEffectFilter effectFilter, DirtyRegionContainer drc, DirtyRegionPool regionPool) {
        Effect effect = effectFilter.getEffect();
        EffectDirtyBoundsHelper helper = EffectDirtyBoundsHelper.getInstance();
        helper.setInputBounds(contentBounds);
        helper.setDirtyRegions(drc);
        final DirtyRegionContainer effectDrc = effect.getDirtyRegions(helper, regionPool);
        drc.deriveWithNewContainer(effectDrc);
        regionPool.checkIn(effectDrc);
    }
    
    private static class EffectDirtyBoundsHelper extends Effect {
        private BaseBounds bounds;
        private static EffectDirtyBoundsHelper instance = null;
        private DirtyRegionContainer drc;

        public void setInputBounds(BaseBounds inputBounds) {
            bounds = inputBounds;
        }

        @Override
        public ImageData filter(FilterContext fctx, 
                BaseTransform transform,
                Rectangle outputClip,
                Object renderHelper,
                Effect defaultInput) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BaseBounds getBounds(BaseTransform transform, Effect defaultInput) {
            return bounds;
        }

        @Override
        public Effect.AccelType getAccelType(FilterContext fctx) {
            return null;
        }

        public static EffectDirtyBoundsHelper getInstance() {
            if (instance == null) {
                instance = new EffectDirtyBoundsHelper();
            }
            return instance;
        }

        @Override
        public boolean reducesOpaquePixels() {
            return true;
        }

        private void setDirtyRegions(DirtyRegionContainer drc) {
            this.drc = drc;
        }

        @Override
        public DirtyRegionContainer getDirtyRegions(Effect defaultInput, DirtyRegionPool regionPool) {
            DirtyRegionContainer ret = regionPool.checkOut();
            ret.deriveWithNewContainer(drc);

            return ret;
        }

    }

}
