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

import javafx.scene.CacheHint;
import java.util.List;
import com.sun.glass.ui.Screen;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.BoxBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
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
import static com.sun.javafx.logging.PulseLogger.PULSE_LOGGER;
import static com.sun.javafx.logging.PulseLogger.PULSE_LOGGING_ENABLED;

/**
 * NGNode is the abstract base class peer of Node, forming
 * the basis for Prism and Scenario render graphs.
 * <p>
 * During synchronization, the FX scene graph will pass down to us
 * the transform which takes us from local space to parent space, the
 * content bounds (ie: geom bounds), and the transformed bounds
 * (ie: boundsInParent), and the clippedBounds. The effect bounds have
 * already been passed to the Effect peer (if there is one).
 * <p>
 * Whenever the transformedBounds of the NGNode are changed, we update
 * the dirtyBounds, so that the next time we need to accumulate dirty
 * regions, we will have the information we need to make sure we create
 * an appropriate dirty region.
 * <p>
 * NGNode maintains a single "dirty" flag, which indicates that this
 * node itself is dirty and must contribute to the dirty region. More
 * specifically, it indicates that this node is now dirty with respect
 * to the back buffer. Any rendering of the scene which will go on the
 * back buffer will cause the dirty flag to be cleared, whereas a
 * rendering of the scene which is for an intermediate image will not
 * clear this dirty flag.
 */
public abstract class NGNode {
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

    protected static enum DirtyFlag {
        CLEAN,
        // Means that the node is dirty, but only because of translation
        DIRTY_BY_TRANSLATION,
        DIRTY
    }

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

    /**
     * Indicates that this NGNode is itself dirty and needs its full bounds
     * included in the next repaint. This means it is dirty with respect to
     * the back buffer. We don't bother differentiating between bounds dirty
     * and visuals dirty because we can simply inspect the dirtyBounds to
     * see if it is valid. If so, then bounds must be dirty.
     */
    protected DirtyFlag dirty = DirtyFlag.DIRTY;

    /**
     * The parent of the node. In the case of a normal render graph node,
     * this will be an NGGroup. However, if this node is being used as
     * a clip node, then the parent is the node it is the clip for.
     */
    private NGNode parent;

    /**
     * True if this node is a clip. This means the parent is clipped by this node.
     */
    private boolean isClip;

    /**
     * The node used for specifying the clipping shape for this node. If null,
     * then there is no clip.
     */
    private NGNode clipNode;

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
     * the implementation due to some form of heuristic, currently we
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
     * If this node is an NGGroup, then this flag will be used to indicate
     * whether one or more of its children is dirty. While it would seem this
     * flag should be on NGGroup, the code turns out to be a bit cleaner with
     * this flag in the NGNode class.
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

    private RectBounds opaqueRegion = null;
    private boolean opaqueRegionInvalid = true;
    private DirtyHint hint;

    protected NGNode() { }

    /***************************************************************************
     *                                                                         *
     *                Methods invoked during synchronization                   *
     *                                                                         *
     **************************************************************************/

    /**
     * Called by the FX scene graph to tell us whether we should be visible or not.
     * @param value whether it is visible
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
            // be able to read state back from the NG side. Yuck. Just doing
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

    /**
     * Called by the FX scene graph to tell us what our transform matrix is.
     * @param tx must not be null
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
                // If there's no hint created yet, this is the first setTransformMatrix
                // call and we have nothing to compare to yet.
                hint = new DirtyHint();
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
            markDirtyByTranslation();
        } else {
            markDirty();
        }
    }

    /**
     * Called by the FX scene graph whenever the clip node for this node changes.
     * @param clipNode can be null if the clip node is being cleared
     */
    public void setClipNode(NGNode clipNode) {
        // Whenever the clipNode itself has changed (that is, the reference to
        // the clipNode), we need to be sure to mark this node dirty and to
        // invalidate the cache of this node (if there is one) and all parents.
        if (clipNode != this.clipNode) {
            // Clear the "parent" property of the clip node, if there was one
            if (this.clipNode != null) this.clipNode.setParent(null);
            // Make the "parent" property of the clip node point to this
            if (clipNode != null) clipNode.setParent(this, true);
            // Keep the reference to the new clip node
            this.clipNode = clipNode;
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
    public void setCachedAsBitmap(boolean cached, CacheHint cacheHint) {
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
     * @param effect the effect (can be null to clear it)
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
     * Hierarchy, visibility, and other such miscellaneous NGNode properties   *
     *                                                                         *
     **************************************************************************/

    /**
     * Gets the parent of this node. The parent might be an NGGroup. However,
     * if this node is a clip node on some other node, then the node on which
     * it is set as the clip will be returned. That is, suppose some node A
     * has a clip node B. The method B.getParent() will return A.
     */
    public NGNode getParent() { return parent; }

    /**
     * Only called by this class, or by the NGGroup class.
     */
    public void setParent(NGNode parent) {
        setParent(parent, false);
    }

    private void setParent(NGNode parent, boolean isClip) {
        this.parent = parent;
        this.isClip = isClip;
    }

    protected final Effect getEffect() { return effectFilter == null ? null : effectFilter.getEffect(); }

    /**
     * Gets whether this node's visible property is set
     */
    public boolean isVisible() { return visible; }

    public final BaseTransform getTransform() { return transform; }
    public final float getOpacity() { return opacity; }
    public final Blend.Mode getNodeBlendMode() { return nodeBlendMode; }
    public final boolean isDepthTest() { return depthTest; }
    public final BaseCacheFilter getCacheFilter() { return cacheFilter; }
    public final BaseEffectFilter getEffectFilter() { return effectFilter; }
    public final NGNode getClipNode() { return clipNode; }

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
        if (false && this instanceof NGGroup) {
            List<NGNode> children = ((NGGroup)this).getChildren();
            BaseBounds tmp = TEMP_BOUNDS;
            for (int i=0; i<children.size(); i++) {
                float minX = bounds.getMinX();
                float minY = bounds.getMinY();
                float minZ = bounds.getMinZ();
                float maxX = bounds.getMaxX();
                float maxY = bounds.getMaxY();
                float maxZ = bounds.getMaxZ();
                NGNode child = children.get(i);
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
     */
    public void markDirtyByTranslation() {
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
     * Notifies the parent (whether an NGGroup or just a NGNode) that
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
        NGNode p = parent;
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
    public final boolean isClean() {
        return dirty == DirtyFlag.CLEAN && !childDirty;
    }

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
        if (this instanceof NGGroup) {
            List<NGNode> children = ((NGGroup) this).getChildren();
            for (int i = 0; i < children.size(); ++i) {
                NGNode child = children.get(i);
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
            assert childDirty; // this must be true by this point
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
     * Accumulates the dirty region of an NGGroup. This is implemented here as opposed to
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
        assert childDirty;
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
        BaseTransform originalRenderTx = null;
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

            originalRenderTx = renderTx;
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
        List<NGNode> removed = ((NGGroup) this).getRemovedChildren();
        if (removed != null) {
            NGNode removedChild;
            for (int i = removed.size() - 1; i >= 0; --i) {
                removedChild = removed.get(i);
                removedChild.dirty = DirtyFlag.DIRTY;
                    status = removedChild.accumulateDirtyRegions(myClip,
                            dirtyRegionTemp,regionPool, dirtyRegionContainer, renderTx, pvTx);
                    if (status == DirtyRegionContainer.DTR_CONTAINS_CLIP) {
                        break;
                    }
            }
        }

        List<NGNode> children = ((NGGroup) this).getChildren();
        int num = children.size();
            for (int i=0; i<num && status == DirtyRegionContainer.DTR_OK; i++) {
            NGNode child = children.get(i);
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
            applyTransform(originalRenderTx, dirtyRegionContainer);
            renderTx = originalRenderTx;

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
            // coordinates for this.
            region = computePadding(region);
            region = tx.transform(region, region);
            region = pvTx.transform(region, region);
        }
        return region;
    }

    /**
     * LCD Text creates some painful situations where, due to the LCD text
     * algorithm, we end up with some pixels touched that are normally outside
     * the bounds. To compensate, we need a hook for NGText to add padding.
     */
    protected BaseBounds computePadding(BaseBounds region) {
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
     * @param drc The array of dirty regions.
     * @param cullingRegionsBitsOfParent culling bits of parent. -1 if there's no parent.
     * @param tx The transform for this render operation.
     * @param pvTx Perspective camera transform.
     */
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

    /**
     * Helper method draws culling bits for each node.
     * @param g Graphics.
     */
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
     * Rendering                                                               *
     *                                                                         *
     **************************************************************************/

    /**
     * Render the tree of nodes to the specified G (graphics) object
     * descending from this node as the root. This method is designed to avoid
     * generated trash as much as possible while descending through the
     * render graph while rendering. This is the appropriate method both to
     * initiate painting of an entire scene, and for a branch. The NGGroup
     * implementation must call this method on each child, not doRender directly.
     *
     * @param g The graphics object we're rendering to. This must never be null.
     */
    public final void render(Graphics g) {
        if (PULSE_LOGGING_ENABLED) PULSE_LOGGER.renderIncrementCounter("Nodes visited during render");
        if (debug) System.out.println("render called on " + getClass().getSimpleName());
        // Clear the visuals changed flag
        clearDirty();
        // If it isn't visible, then punt
        if (!visible || opacity == 0f) return;
        // If we are supposed to cull, then go ahead and do so
//        if (cull(clip, tx)) return;

        // We know that we are going to render this node, so we call the
        // doRender method, which subclasses implement to do the actual
        // rendering work.
        doRender(g);
    }

    // This node requires 2D graphics state for rendering
    boolean isShape3D() {
        return false;
    }

    /**
     * Invoked only by the final render method. Implementations
     * of this method should make sure to save & restore the transform state.
     */
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
                new NodeEffectInput(getClipNode(),
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
        // We will punt on 3D completely for caching.
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

    BaseCacheFilter createCacheFilter(CacheHint cacheHint) {
        return new CacheFilter(this, cacheHint);
    }

    BaseEffectFilter createEffectFilter(Effect effect) {
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

        CacheFilter(NGNode node, CacheHint cacheHint) {
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
                    node.renderClip(g);
                } else if (node.getEffectFilter() != null) {
                    node.renderEffect(g);
                } else {
                    node.renderContent(g);
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
            if (node.getEffectFilter() != null) {
                node.renderEffect(g);
            } else {
                node.renderContent(g);
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
            List<NGNode> children = ((NGGroup)node).getChildren();
            if (children.size() != 1) {
                return false;
            }
            NGNode child = children.get(0);
            if (!child.getTransform().is2D()) {
                return false;
            }
            
            NGNode clip = node.getClipNode();
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

        @Override protected BaseNodeEffectInput createNodeEffectInput(NGNode node) {
            return new NodeEffectInput(node);
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

    /***************************************************************************
     *                                                                         *
     * Stuff                                                                   *
     *                                                                         *
     **************************************************************************/

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
            if (bounds.getBoundsType() == BaseBounds.BoundsType.RECTANGLE) {
                return bounds;
            } else {
                //RT-29453 - CCE: in case we get 3D bounds we need to "flatten" them
                return new RectBounds(bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY());
            }
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
