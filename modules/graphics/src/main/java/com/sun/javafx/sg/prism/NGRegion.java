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

import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderImage;
import javafx.scene.layout.BorderRepeat;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import java.util.List;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.logging.PulseLogger;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.BasicStroke;
import com.sun.prism.Graphics;
import com.sun.prism.Image;
import com.sun.prism.RTTexture;
import com.sun.prism.Texture;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.paint.ImagePattern;
import com.sun.prism.paint.Paint;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.Offset;

/**
 * Implementation of the Region peer. This behaves like an NGGroup, in that
 * it has children, but like a leaf node, in that it also draws itself if it has
 * a Background or Border which contains non-transparent fills / strokes / images.
 */
public class NGRegion extends NGGroup {
    /**
     * This scratch transform is used when transforming shapes. Because this is
     * a static variable, it is only intended to be used from a single thread,
     * the render thread in this case.
     */
    private static final Affine2D SCRATCH_AFFINE = new Affine2D();

    /**
     * The texture cache used by all regions for storing background fills / shapes which
     * can be easily cached and reused.
     */
    private static final RegionImageCache CACHE = new RegionImageCache();

    /**
     * The background to use for drawing. Since this is an immutable object, I can simply refer to
     * its fields / methods directly when rendering. I will make sure this is not ever null at
     * the time that we do the sync, so that the code in this class can assume non-null.
     */
    private Background background = Background.EMPTY;

    /**
     * The combined insets of all the backgrounds. As of right now, Background doesn't store
     * this information itself, although it probably could (and probably should).
     */
    private Insets backgroundInsets = Insets.EMPTY;

    /**
     * The border to use for drawing. Similar to background, this is not-null and immutable.
     */
    private Border border = Border.EMPTY;

    /**
     * The shape of the region. Usually this will be null (except for things like check box
     * checks, scroll bar down arrows / up arrows, etc). If this is not null, it determines
     * the shape of the region to draw. If it is null, then the assumed shape of the region is
     * one of a rounded rectangle. This shape is a com.sun.javafx.geom.Shape, and is not
     * touched by the FX scene graph except during synchronization, so it is safe to access
     * on the render thread.
     */
    private Shape shape;

    /**
     * Whether we should scale the shape to match the bounds of the region. Only applies
     * if the shape is not null.
     */
    private boolean scaleShape = true;

    /**
     * Whether we should center the shape within the bounds of the region. Only applies
     * if the shape is not null.
     */
    private boolean centerShape = true;

    /**
     * Whether we should attempt to use region caching for a region with a shape.
     */
    private boolean cacheShape = false;

    /**
     * A cached set of the opaque insets as given to us during synchronization. We hold
     * on to this so that we can determine the opaque insets in the computeOpaqueRegion method.
     */
    private float opaqueTop = Float.NaN,
            opaqueRight = Float.NaN,
            opaqueBottom = Float.NaN,
            opaqueLeft = Float.NaN;

    /**
     * The width and height of the region.
     */
    private float width, height;

    /**
     * Simple Helper Function for cleanup.
     */
    static Paint getPlatformPaint(javafx.scene.paint.Paint paint) {
        return (Paint)Toolkit.getPaintAccessor().getPlatformPaint(paint);
    }
    
    /**
     * Determined when a background is set on the region, this flag indicates whether this
     * background can be cached. As of this time, the only backgrounds which can be cached
     * are those where there are only solid fills or vertical linear gradients.
     */
    private boolean backgroundCanBeCached;

    // We create a class instance of a no op. Effect internally to handle 3D
    // transform if user didn't use Effect for 3D Transformed Region. This will
    // automatically forces Region rendering path to use the Effect path.
    private static Offset nopEffect = new Offset(0, 0, null);
    private BaseEffectFilter nopEffectFilter;

    /**************************************************************************
     *                                                                        *
     * Methods used during synchronization only.                              *
     *                                                                        *
     *************************************************************************/

    /**
     * Called by the Region during synchronization. The Region *should* ensure that this is only
     * called when one of these properties has changed. The cost of calling it excessively is
     * only that the opaque region is invalidated excessively. Updating the shape and
     * associated booleans is actually a very cheap operation.
     *
     * @param shape    The shape, may be null.
     * @param scaleShape whether to scale the shape
     * @param positionShape whether to center the shape
     */
    public void updateShape(Object shape, boolean scaleShape, boolean positionShape, boolean cacheShape) {
        this.shape = shape == null ? null : ((NGShape) ((javafx.scene.shape.Shape)shape).impl_getPeer()).getShape();
        this.scaleShape = scaleShape;
        this.centerShape = positionShape;
        this.cacheShape = cacheShape;
        invalidateOpaqueRegion();
    }

    /**
     * Called by the Region whenever the width or height of the region has changed.
     * The Region *should* only call this when the width or height have actually changed.
     *
     * @param width     The width of the region, not including insets or outsets
     * @param height    The height of the region, not including insets or outsets
     */
    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
        invalidateOpaqueRegion();
        backgroundInsets = null;
    }

    /**
     * Called by the Region when the Border is changed. The Region *must* only call
     * this method if the border object has actually changed, or excessive work may be done.
     *
     * @param b Border, of type javafx.scene.layout.Border
     */
    public void updateBorder(Object b) {
        // Make sure that the border instance we store on this NGRegion is never null
        final Border old = border;
        border = b == null ? Border.EMPTY : (Border) b;

        // Determine whether the geometry has changed, or if only the visuals have
        // changed. Geometry changes will require more work, and an equals check
        // on the border objects is generally very fast (either for identity or
        // for !equals. It is a bit longer for when they really are equal, but faster
        // than a geometryChanged!)
        if (!border.getOutsets().equals(old.getOutsets())) {
            geometryChanged();
        } else {
            visualsChanged();
        }
    }

    /**
     * Called by the Region when the Background has changed. The Region *must* only call
     * this method if the background object has actually changed, or excessive work may be done.
     *
     * @param b    Background, of type javafx.scene.layout.Background. Can be null.
     */
    public void updateBackground(Object b) {
        // Make sure that the background instance we store on this NGRegion is never null
        final Background old = background;
        background = b == null ? Background.EMPTY : (Background) b;

        final List<BackgroundFill> fills = background.getFills();
        backgroundCanBeCached = !PrismSettings.disableRegionCaching && !fills.isEmpty() && (shape == null || cacheShape);
        if (backgroundCanBeCached) {
            for (int i=0, max=fills.size(); i<max && backgroundCanBeCached; i++) {
                // We need to now inspect the paint to determine whether we can use a cache for this background.
                // If a shape is being used, we don't care about gradients (we cache 'em both), but for a rectangle
                // fill we omit these (so we can do 3-patch scaling). An ImagePattern is deadly to either
                // (well, only deadly to a shape if it turns out to be a writable image).
                final BackgroundFill fill = fills.get(i);
                javafx.scene.paint.Paint paint = fill.getFill();
                if ((shape == null && paint instanceof RadialGradient) || paint instanceof javafx.scene.paint.ImagePattern) {
                    backgroundCanBeCached = false;
                }
                if (shape == null && paint instanceof LinearGradient) {
                    LinearGradient linear = (LinearGradient) paint;
                    if (linear.getStartX() != linear.getEndX()) {
                        backgroundCanBeCached = false;
                    }
                }
            }
        }
        backgroundInsets = null;

        // Only update the geom if the new background is geometrically different from the old
        if (!background.getOutsets().equals(old.getOutsets())) {
            geometryChanged();
        } else {
            visualsChanged();
        }
    }

    /**
     * Visits each of the background fills and takes their raddi into account to determine the insets.
     * The backgroundInsets variable is cleared whenever the fills change, or whenever the size of the
     * region has changed (because if the size of the region changed and a radius is percentage based
     * then we need to recompute the insets).
     */
    private void updateBackgroundInsets() {
        float top=0, right=0, bottom=0, left=0;
        final List<BackgroundFill> fills = background.getFills();
        for (int i=0, max=fills.size(); i<max; i++) {
            // We need to now inspect the paint to determine whether we can use a cache for this background.
            // If a shape is being used, we don't care about gradients (we cache 'em both), but for a rectangle
            // fill we omit these (so we can do 3-patch scaling). An ImagePattern is deadly to either
            // (well, only deadly to a shape if it turns out to be a writable image).
            final BackgroundFill fill = fills.get(i);
            final Insets insets = fill.getInsets();
            final CornerRadii radii = normalize(fill.getRadii());
            top = (float) Math.max(top, insets.getTop() + Math.max(radii.getTopLeftVerticalRadius(), radii.getTopRightVerticalRadius()));
            right = (float) Math.max(right, insets.getRight() + Math.max(radii.getTopRightHorizontalRadius(), radii.getBottomRightHorizontalRadius()));
            bottom = (float) Math.max(bottom, insets.getBottom() + Math.max(radii.getBottomRightVerticalRadius(), radii.getBottomLeftVerticalRadius()));
            left = (float) Math.max(left, insets.getLeft() + Math.max(radii.getTopLeftHorizontalRadius(), radii.getBottomLeftHorizontalRadius()));
        }
        backgroundInsets = new Insets(roundUp(top), roundUp(right), roundUp(bottom), roundUp(left));
    }

    /**
     * Called by the Region whenever it knows that the opaque insets have changed. The
     * Region <strong>must</strong> make sure that these opaque insets include the opaque
     * inset information from the Border and Background as well, the NGRegion will not
     * recompute this information. This is done because Border and Background are immutable,
     * and as such this information is computed once and stored rather than recomputed
     * each time we have to render. Any developer supplied opaque insets must be combined
     * with the Border / Background intrinsic opaque insets prior to this call and passed
     * as the arguments to this method.
     *
     * @param top       The top, if NaN then there is no opaque inset at all
     * @param right     The right, must not be NaN or Infinity, etc.
     * @param bottom    The bottom, must not be NaN or Infinity, etc.
     * @param left      The left, must not be NaN or Infinity, etc.
     */
    public void setOpaqueInsets(float top, float right, float bottom, float left) {
        opaqueTop = top;
        opaqueRight = right;
        opaqueBottom = bottom;
        opaqueLeft = left;
        invalidateOpaqueRegion();
    }

    /**
     * Overridden so as to invalidate the opaque region, since the opaque region computation
     * must take opacity into account.
     *
     * @param opacity A value between 0 and 1.
     */
    @Override public void setOpacity(float opacity) {
        // We certainly don't want to do any work if opacity hasn't changed!
        final float old = getOpacity();
        if (old != opacity) {
            super.setOpacity(opacity);
            // Even though the opacity has changed, for example from .5 to .6,
            // we don't need to invalidate the opaque region unless it has toggled
            // from 1 to !1, or from !1 to 1.
            if (old < 1 || opacity < 1) invalidateOpaqueRegion();
        }
    }

    /**
     * Overridden so that we can invalidate the opaque region when the clip
     * has changed.
     *
     * @param clipNode can be null if the clip node is being cleared
     */
    @Override public void setClipNode(NGNode clipNode) {
        super.setClipNode(clipNode);
        invalidateOpaqueRegion();
    }

    /**
     * Overridden so as to invalidate the opaque region when the effect changes.
     *
     * @param effect
     */
    @Override public void setEffect(Object effect) {
        // Lets only do work if we have to
        Effect old = getEffect();
        if (old != effect) {
            super.setEffect(effect);
            // The only thing we do with the effect in #computeOpaqueRegion(RectBounds) is to check
            // whether the effect is null / not null, and whether a not null effect reduces opaque
            // pixels. If the answer to these question has not changed from last time, then there
            // is no need to recompute the opaque region.
            if (old == null || effect == null ||
                    old.reducesOpaquePixels() != ((Effect)effect).reducesOpaquePixels()){
                invalidateOpaqueRegion();
            }
        }
    }

    /**************************************************************************
     *                                                                        *
     * Implementations of methods defined in the parent classes, with the     *
     * exception of rendering methods.                                        *
     *                                                                        *
     *************************************************************************/

    /**
     * The opaque region of an NGRegion takes into account the opaque insets
     * specified by the Region during synchronization. It also takes into
     * account the clip and the effect.
     *
     * @param opaqueRegion
     * @return
     */
    @Override protected RectBounds computeOpaqueRegion(RectBounds opaqueRegion) {
        final NGNode clip = getClipNode();
        final Effect effect = getEffect();
        // compute opaque region
        if ((effect == null || !effect.reducesOpaquePixels()) &&
                getOpacity() == 1f &&
                (clip == null ||
                (clip instanceof NGRectangle && ((NGRectangle)clip).isRectClip(BaseTransform.IDENTITY_TRANSFORM, true))))
        {
            if (Float.isNaN(opaqueTop) || Float.isNaN(opaqueRight) || Float.isNaN(opaqueBottom) || Float.isNaN(opaqueLeft)) {
                return null;
            }

            // TODO what to do if the opaqueRegion has negative width or height due to excessive opaque insets? (RT-26979)
            if (opaqueRegion == null) {
                opaqueRegion = new RectBounds(opaqueLeft, opaqueTop, width - opaqueRight, height - opaqueBottom);
            } else {
                opaqueRegion.deriveWithNewBounds(opaqueLeft, opaqueTop, 0, width - opaqueRight, height - opaqueBottom, 0);
            }

            if (clip != null) { // We already know that clip is rectangular
                // RT-25095: If this node has a clip who's opaque region cannot be determined, then
                // we cannot determine any opaque region for this node (in fact, it might not have one).
                final RectBounds clipOpaqueRegion = ((NGRectangle)clip).getOpaqueRegion();
                if (clipOpaqueRegion == null) {
                    return null;
                } else {
                    opaqueRegion.intersectWith(clipOpaqueRegion);
                }
            }

            return opaqueRegion;
        }
        return null;
    }

    @Override protected NodePath<NGNode> computeRenderRoot(NodePath<NGNode> path, RectBounds dirtyRegion,
                                                           int cullingIndex, BaseTransform tx,
                                                           GeneralTransform3D pvTx) {

        NodePath<NGNode> childPath = super.computeRenderRoot(path, dirtyRegion, cullingIndex, tx, pvTx);
        if (childPath != null) {
            childPath.add(this);
        } else {
            childPath = computeNodeRenderRoot(path, dirtyRegion, cullingIndex, tx, pvTx);
        }
        return childPath;
    }

    @Override protected boolean hasVisuals() {
        // This isn't entirely accurate -- the background might
        // not be empty but still not draw anything since a BackgroundFill
        // might be TRANSPARENT. The same is true of the border, which
        // might have BorderStrokes but perhaps none of them draw.
        return !border.isEmpty() || !background.isEmpty();
    }

    @Override protected boolean hasOverlappingContents() {
        // It may be that this can be optimized further, but I'm a bit
        // worried about it as I would have to check that the children do not
        // overlap with the strokes, and the strokes don't overlap each other,
        // and there are no backgrounds, etc. So there are a few fast paths
        // that could be used, but not sure it is really of any benefit in
        // the real cases.
        return true;
    }

    /**************************************************************************
     *                                                                        *
     * Region drawing.                                                        *
     *                                                                        *
     *************************************************************************/

    @Override protected void renderContent(Graphics g) {
        // Use Effect to render 3D transformed Region that does not contain 3D
        // transformed children.
        // This is done in order to render the Region's content and children
        // into an image in local coordinates using the identity transform.
        // The resulting image will then be correctly transformed in 3D by
        // the composite transform used to render this Region.
        // However, we avoid doing this for Regions whose children have a 3D
        // transform, because it will flatten the transforms of those children
        // and not look correct.
        if (!g.getTransformNoClone().is2D() && this.isContentBounds2D()) {
            assert (getEffectFilter() == null);

            // Use Effect to render 3D transformed Region.
            // We will need to use a no op. Effect internally since user
            // didn't use Effect for this Region
            if (nopEffectFilter == null) {
                nopEffectFilter = createEffectFilter(nopEffect);
            }
            ((EffectFilter)nopEffectFilter).render(g);

            return;
        }

        // If the shape is not null, then the shape will define what we need to draw for
        // this region. If the shape is null, then the "shape" of the region is just a
        // rectangle (or rounded rectangle, depending on the Background).
        if (shape != null) {
            if (!background.isEmpty()) {
                final Insets outsets = background.getOutsets();
                final Shape outsetShape = resizeShape((float) -outsets.getTop(), (float) -outsets.getRight(), (float) -outsets.getBottom(), (float) -outsets.getLeft());
                final RectBounds outsetShapeBounds = outsetShape.getBounds();
                final int textureWidth = Math.round(outsetShapeBounds.getWidth()),
                          textureHeight = Math.round(outsetShapeBounds.getHeight());

                // See if we have a cached representation for this region background already. In UI controls,
                // the arrow in a scroll bar button or the dot in a radio button or the tick in a check box are
                // all examples of cases where we'd like to reuse a cached image for performance reasons rather
                // than re-drawing everything each time.

                // RT-25013: We need to make sure that we do not use a cached image in the case of a
                // scaled region, or things won't look right (they'll looked scaled instead of vector-resized).
                final boolean cache = backgroundCanBeCached && g.getTransformNoClone().isTranslateOrIdentity();
                RTTexture cached = cache ? CACHE.getImage(g.getAssociatedScreen(), textureWidth, textureHeight, background, shape) : null;
                if (cached != null) {
                    cached.lock();
                    if (cached.isSurfaceLost()) {
                        cached = null;
                    }
                }
                // If there is not a cached texture already, then we need to render everything
                if (cached == null) {
                    // We will here check to see if we CAN cache the region background. If not, then
                    // we will render as normal. If we can cache it, however, then we will setup a
                    // texture and swizzle rendering onto the RTTexture's graphics, and then at the
                    // end do render from the texture onto the graphics object we were passed.
                    Graphics old = null;
                    if (cache && CACHE.isImageCachable(textureWidth, textureHeight)) {
                        old = g;
                        cached = g.getResourceFactory().createRTTexture(textureWidth, textureHeight,
                                                                        WrapMode.CLAMP_TO_ZERO);
                        cached.contentsUseful();
                        g = cached.createGraphics();
                        // Have to move the origin such that when rendering to x=0, we actually end up rendering
                        // at x=bounds.getMinX(). Otherwise anything rendered to the left of the origin would be lost
                        g.translate(-outsetShapeBounds.getMinX(), -outsetShapeBounds.getMinY());
                        CACHE.setImage(cached, old.getAssociatedScreen(), textureWidth, textureHeight, background, shape);
                        if (PulseLogger.PULSE_LOGGING_ENABLED) {
                            PulseLogger.PULSE_LOGGER.renderIncrementCounter("Region shape image cached");
                        }
                    }

                    // We first need to draw each background fill. We don't pay any attention
                    // to the radii of the BackgroundFill, but we do honor the insets and
                    // the fill paint itself.
                    final List<BackgroundFill> fills = background.getFills();
                    for (int i = 0, max = fills.size(); i < max; i++) {
                        final BackgroundFill fill = fills.get(i);
                        // Get the paint for this BackgroundFill. It should not be possible
                        // for it to ever be null
                        final Paint paint = getPlatformPaint(fill.getFill());
                        assert paint != null;
                        g.setPaint(paint);
                        // Adjust the box within which we will fit the shape based on the
                        // insets. The resize shape method will resize the shape to fit
                        final Insets insets = fill.getInsets();
                        g.fill(resizeShape((float) insets.getTop(), (float) insets.getRight(),
                                           (float) insets.getBottom(), (float) insets.getLeft()));
                    }
                    // We now need to draw each background image. Only the "cover" property
                    // of BackgroundImage, and the "image" property itself, have any impact
                    // on how the image is applied to a Shape.
                    final List<BackgroundImage> images = background.getImages();
                    for (int i = 0, max = images.size(); i < max; i++) {
                        final BackgroundImage image = images.get(i);
                        final Image prismImage = (Image) image.getImage().impl_getPlatformImage();
                        if (prismImage == null) {
                            // The prismImage might be null if the Image has not completed loading.
                            // In that case, we simply must skip rendering of that layer this
                            // time around.
                            continue;
                        }
                        // We need to translate the shape based on 0 insets. This will for example
                        // center and / or position the shape if necessary.
                        final Shape translatedShape = resizeShape(0, 0, 0, 0);
                        // Now ensure that the ImagePattern is based on the x/y position of the
                        // shape and not on the 0,0 position of the region.
                        final RectBounds bounds = translatedShape.getBounds();
                        ImagePattern pattern = image.getSize().isCover() ?
                                new ImagePattern(prismImage, bounds.getMinX(), bounds.getMinY(),
                                                 bounds.getWidth(), bounds.getHeight(), false, false) :
                                new ImagePattern(prismImage, bounds.getMinX(), bounds.getMinY(),
                                                 prismImage.getWidth(), prismImage.getHeight(), false, false);
                        g.setPaint(pattern);
                        // Go ahead and finally fill!
                        g.fill(translatedShape);
                    }
                    // If old != null then that means we were rendering into the "cached" texture, and
                    // therefore need to reset the graphics.
                    if (old != null) {
                        g = old;
                    }
                }
                // cached might not be null if either there was a cached image, or we just created one.
                // In either case, we need to now render from the cached texture to the graphics
                if (cached != null) {
                    // We just draw exactly what it was we have cached
                    final float dstX1 = outsetShapeBounds.getMinX();
                    final float dstY1 = outsetShapeBounds.getMinY();
                    final float dstX2 = outsetShapeBounds.getMaxX();
                    final float dstY2 = outsetShapeBounds.getMaxY();

                    final float srcX1 = 0f;
                    final float srcY1 = 0f;
                    final float srcX2 = srcX1 + textureWidth;
                    final float srcY2 = srcY1 + textureHeight;

                    g.drawTexture(cached, dstX1, dstY1, dstX2, dstY2, srcX1, srcY1, srcX2, srcY2);
                    if (PulseLogger.PULSE_LOGGING_ENABLED) {
                        PulseLogger.PULSE_LOGGER.renderIncrementCounter("Cached Region shape image used");
                    }
                    cached.unlock();
                }
            }

            if (!border.isEmpty()) {
                // We only deal with stroke borders, we never deal with ImageBorders when
                // painting a shape on a Region. This is primarily because we don't know
                // how to handle a 9-patch image on a random shape.
                final List<BorderStroke> strokes = border.getStrokes();
                for (int i = 0, max = strokes.size(); i < max; i++) {
                    // Get the BorderStroke. When stroking a shape, we only honor the
                    // topStroke, topStyle, widths.top, and insets.
                    final BorderStroke stroke = strokes.get(i);
                    // We're stroking a path, so there is no point trying to figure out the length.
                    // Instead, we just pass -1, telling setBorderStyle to just do a simple stroke
                    setBorderStyle(g, stroke, -1);
                    final Insets insets = stroke.getInsets();
                    g.draw(resizeShape((float) insets.getTop(), (float) insets.getRight(),
                                       (float) insets.getBottom(), (float) insets.getLeft()));
                }
            }
        } else if (width > 0 && height > 0) {
            if (!background.isEmpty()) {
                final Insets outsets = background.getOutsets();
                // cacheWidth is the width of the region used within the cached image. For example,
                // perhaps normally the width of a region is 200px. But instead I will render the
                // region as though it is 20px wide instead into the cached image. 20px in this
                // case is the cache width. Although it may draw into more pixels than this (for
                // example, drawing the focus rectangle extends beyond the width of the region).
                // left + right background insets give us the left / right slice locations, plus 1 pixel for the center.
                // Round the whole thing up to be a whole number.
                if (backgroundInsets == null) updateBackgroundInsets();
                final double leftInset = backgroundInsets.getLeft() + 1;
                final double rightInset = backgroundInsets.getRight() + 1;
                int cacheWidth = (int) (leftInset + rightInset);
                // If the insets are too large, then we want to use the width of the region instead of the
                // computed cacheWidth. RadioButton enters this case
                cacheWidth = Math.min(cacheWidth, roundUp(width));
                // The textureWidth / textureHeight is the width/height of the actual image. This needs to be rounded
                // up to the next whole pixel value.
                final int textureWidth = cacheWidth + roundUp(outsets.getLeft()) + roundUp(outsets.getRight()),
                          textureHeight = roundUp(height) + roundUp(outsets.getTop()) + roundUp(outsets.getBottom());

                // See if we have a cached representation for this region background already.
                // RT-25013: We need to make sure that we do not use a cached image in the case of a
                // scaled region, or things won't look right (they'll looked scaled instead of vector-resized).
                // RT_25049: Need to only use the cache for pixel aligned regions or the result
                // will not look the same as though drawn by vector
                final boolean cache =
                        height < 256 &&
                        background.getFills().size() > 1 && // Not worth the overhead otherwise
                        (width - (int)width == 0) &&
                        backgroundCanBeCached &&
                        g.getTransformNoClone().isTranslateOrIdentity();
                RTTexture cached = cache ? CACHE.getImage(g.getAssociatedScreen(), textureWidth, textureHeight, background) : null;
                if (cached != null) {
                    cached.lock();
                    if (cached.isSurfaceLost()) {
                        cached = null;
                    }
                }
                // If there is not a cached texture already, then we need to render everything
                if (cached == null) {
                    // We will here check to see if we CAN cache the region background. If not, then
                    // we will render as normal. If we can cache it, however, then we will setup a
                    // texture and swizzle rendering onto the RTTexture's graphics, and then at the
                    // end do 3-patch from the texture onto the graphics object we were passed.
                    Graphics old = null;
                    float oldWidth = width;
                    if (cache && CACHE.isImageCachable(textureWidth, textureHeight)) {
                        old = g;
                        width = cacheWidth;
                        cached = g.getResourceFactory().createRTTexture(textureWidth, textureHeight,
                                                                        WrapMode.CLAMP_TO_ZERO);
                        cached.contentsUseful();
                        g = cached.createGraphics();
                        // Have to move the origin such that when rendering to x=0, we actually end up rendering
                        // at x=outsets.getLeft(). Otherwise anything rendered to the left of the origin would be lost
                        // Round up to the nearest pixel
                        g.translate(roundUp(outsets.getLeft()), roundUp(outsets.getTop()));
                        CACHE.setImage(cached, old.getAssociatedScreen(), textureWidth, textureHeight, background);
                        if (PulseLogger.PULSE_LOGGING_ENABLED) {
                            PulseLogger.PULSE_LOGGER.renderIncrementCounter("Region background image cached");
                        }
                    }

                    // Paint in order each BackgroundFill.
                    final List<BackgroundFill> fills = background.getFills();
                    for (int i = 0, max = fills.size(); i < max; i++) {
                        final BackgroundFill fill = fills.get(i);
                        final Insets insets = fill.getInsets();
                        final float t = (float) insets.getTop(),
                                l = (float) insets.getLeft(),
                                b = (float) insets.getBottom(),
                                r = (float) insets.getRight();
                        // w and h is the width and height of the area to be filled (width and height less insets)
                        float w = width - l - r;
                        float h = height - t - b;
                        // Only setup and paint for those areas which have positive width and height. This means, if
                        // the insets are such that the right edge is left of the left edge, then we have a negative
                        // width and will not paint it. TODO we need to document this fact (RT-26924)
                        if (w > 0 && h > 0) {
                            // Could optimize this such that if paint is transparent then we go no further.
                            final Paint paint = getPlatformPaint(fill.getFill());
                            g.setPaint(paint);
                            final CornerRadii radii = fill.getRadii();
                            // This is a workaround for RT-28435 so we use path rasterizer for small radius's We are
                            // keeping old rendering. We do not apply workaround when using Caspian or Embedded
                            if (radii.isUniform() &&
                                    !(!PlatformImpl.isCaspian() && !PlatformUtil.isEmbedded() && radii.getTopLeftHorizontalRadius() > 0 && radii.getTopLeftHorizontalRadius() <= 4)) {
                                // If the radii is uniform then we know every corner matches, so we can do some
                                // faster rendering paths.
                                float tlhr = (float) radii.getTopLeftHorizontalRadius();
                                float tlvr = (float) radii.getTopLeftVerticalRadius();
                                if (tlhr == 0 && tlvr == 0) {
                                    // The edges are square, so we can do a simple fill rect
                                    g.fillRect(l, t, w, h);
                                } else {
                                    // Fix the horizontal and vertical radii if they are percentage based
                                    if (radii.isTopLeftHorizontalRadiusAsPercentage()) tlhr = tlhr * width;
                                    if (radii.isTopLeftVerticalRadiusAsPercentage()) tlvr = tlvr * height;
                                    // The edges are rounded, so we need to compute the arc width and arc height
                                    // and fill a round rect
                                    float arcWidth = tlhr + tlhr;
                                    float arcHeight = tlvr + tlvr;
                                    // If the arc width and arc height are so large as to exceed the width / height of
                                    // the region, then we clamp to the width / height of the region (which will give
                                    // the look of a circle on that corner)
                                    if (arcWidth > w) arcWidth = w;
                                    if (arcHeight > h) arcHeight = h;
                                    g.fillRoundRect(l, t, w, h, arcWidth, arcHeight);
                                }
                            } else {
                                // The edges are not uniform, so we have to render each edge independently
                                // TODO document the issue number which will give us a fast path for rendering
                                // non-uniform corners, and that we want to implement that instead of createPath2
                                // below in such cases. (RT-26979)
                                g.fill(createPath(t, l, b, r, normalize(radii)));
                            }
                        }
                    }

                    // If old != null then that means we were rendering into the "cached" texture, and
                    // therefore need to reset the graphics and width (because above we had changed
                    // graphics to point to the cached texture's graphics, and we had changed the
                    // width to be only big enough for the smallest texture we could save).
                    if (old != null) {
                        g = old;
                        width = oldWidth;
                    }
                }

                // cached might not be null if either there was a cached image, or we just created one.
                // In either case, we need to now render from the cached texture to the graphics
                if (cached != null) {
                    final double dstWidth = width + roundUp(outsets.getLeft()) + roundUp(outsets.getRight());
                    if (cached.getContentWidth() == dstWidth) {
                        // If the destination width is the same as the content width of the cached image,
                        // then we can just draw the cached image directly, no need for 3-patch rendering
                        final float dstX1 = -roundUp(outsets.getLeft());
                        final float dstY1 = -roundUp(outsets.getTop());
                        final float dstX2 = (width + roundUp(outsets.getRight()));
                        final float dstY2 = (height + roundUp(outsets.getBottom()));

                        final float srcX1 = 0f;
                        final float srcY1 = 0f;
                        final float srcX2 = srcX1 + textureWidth;
                        final float srcY2 = srcY1 + textureHeight;

                        g.drawTexture(cached, dstX1, dstY1, dstX2, dstY2, srcX1, srcY1, srcX2, srcY2);

                        if (PulseLogger.PULSE_LOGGING_ENABLED) {
                            PulseLogger.PULSE_LOGGER.renderIncrementCounter("Cached Region background image used");
                        }
                    } else {
                        // We do 3-patch rendering, because our height is fixed (ie: the same background but at
                        // different heights will have different cached images)
                        final float dstX1 = -roundUp(outsets.getLeft());
                        final float dstY1 = -roundUp(outsets.getTop());
                        final float dstX2 = (width + roundUp(outsets.getRight()));
                        final float dstY2 = (height + roundUp(outsets.getBottom()));

                        final float srcX1 = 0f;
                        final float srcY1 = 0f;
                        final float srcX2 = srcX1 + textureWidth;
                        final float srcY2 = srcY1 + textureHeight;

                        final float right = (float) (rightInset + roundUp(outsets.getRight())),
                        left = (float) (leftInset + roundUp(outsets.getLeft()));

                        final float dstLeftX = dstX1 + left;
                        final float dstRightX = dstX2 - right;
                        final float srcLeftX = srcX1 + left;
                        final float srcRightX = srcX2 - right;

//                        System.out.println("\noutsets=" + outsets + ", width=" + width + ", height=" + height + ", contentX=" + cached.getContentX() + ", contentY=" + cached.getContentY());
//                        System.out.println("dstX1=" + dstX1 + ", dstY1=" + dstY1 + ", dstLeftX=" + dstLeftX + ", dstY2=" + dstY2 + ", srcX1=" + srcX1 + ", srcY1=" + srcY1 + ", srcLeftX=" + srcLeftX + ", srcY2=" + srcY2);
//                        System.out.println("dstLeftX=" + dstLeftX + ", dstY1=" + dstY1 + ", dstRightX=" + dstRightX + ", dstY2=" + dstY2 + ", srcLeftX=" + srcLeftX + ", srcY1=" + srcY1 + ", srcRightX=" + srcRightX + ", srcY2=" + srcY2);
//                        System.out.println("dstRightX=" + dstRightX + ", dstY1=" + dstY1 + ", dstX2=" + dstX2 + ", dstY2=" + dstY2 + ", srcRightX=" + srcRightX + ", srcY1=" + srcY1 + ", srcX2=" + srcX2 + ", srcY2=" + srcY2);

                        // These assertions must hold, or rendering artifacts are highly likely to occur
                        assert dstX1 != dstLeftX;
                        assert dstLeftX != dstRightX;
                        assert dstRightX != dstX2;

                        g.drawTexture(cached, dstX1, dstY1, dstLeftX, dstY2, srcX1, srcY1, srcLeftX, srcY2);
                        g.drawTexture(cached, dstLeftX, dstY1, dstRightX, dstY2, srcLeftX, srcY1, srcRightX, srcY2);
                        g.drawTexture(cached, dstRightX, dstY1, dstX2, dstY2, srcRightX, srcY1, srcX2, srcY2);

                        if (PulseLogger.PULSE_LOGGING_ENABLED) {
                            PulseLogger.PULSE_LOGGER.renderIncrementCounter("Cached Region background image used");
                        }
                    }
                    cached.unlock();
                }

                final List<BackgroundImage> images = background.getImages();
                for (int i = 0, max = images.size(); i < max; i++) {
                    final BackgroundImage image = images.get(i);
                    Image img = (Image) image.getImage().impl_getPlatformImage();
                    final int imgUnscaledWidth = (int)image.getImage().getWidth();
                    final int imgUnscaledHeight = (int)image.getImage().getHeight();
                    final int imgWidth = img.getWidth();
                    final int imgHeight = img.getHeight();
                    // TODO need to write tests which demonstrate this works when the image hasn't loaded yet. (RT-26978)
                    // TODO need to write tests where we use a writable image and draw to it a lot. (RT-26978)
                    if (img != null && imgWidth != 0 && imgHeight != 0) {
                        final BackgroundSize size = image.getSize();
                        if (size.isCover()) {
                            // When "cover" is true, we can ignore most properties on the BackgroundSize and
                            // BackgroundRepeat and BackgroundPosition. Because the image will be stretched to
                            // fill the entire space, there is no need to know the repeat or position or
                            // size width / height.
                            final float scale = Math.max(width / imgWidth,height / imgHeight);
                            final Texture texture =
                                g.getResourceFactory().getCachedTexture(img, Texture.WrapMode.CLAMP_TO_EDGE);
                            g.drawTexture(texture,
                                    0, 0, width, height,
                                    0, 0, width/scale, height/scale
                            );
                            texture.unlock();
                        } else {
                            // Other than "cover", all other modes need to pay attention to the repeat,
                            // size, and position in order to determine how to render. This next block
                            // of code is responsible for determining the width and height of the area
                            // that we are going to fill. The size might be percentage based, in which
                            // case we need to multiply by the width or height.
                            final double w = size.isWidthAsPercentage() ? size.getWidth() * width : size.getWidth();
                            final double h = size.isHeightAsPercentage() ? size.getHeight() * height : size.getHeight();

                            // Now figure out the width and height of each tile to be drawn. The actual image
                            // dimensions may be one thing, but we need to figure out what the size of the image
                            // in the destination is going to be.
                            final double tileWidth, tileHeight;
                            if (size.isContain()) {
                                // In the case of "contain", we compute the destination size based on the largest
                                // possible scale such that the aspect ratio is maintained, yet one side of the
                                // region is completely filled.
                                final float scaleX = width / imgUnscaledWidth;
                                final float scaleY = height / imgUnscaledHeight;
                                final float scale = Math.min(scaleX, scaleY);
                                tileWidth = Math.ceil(scale * imgUnscaledWidth);
                                tileHeight = Math.ceil(scale * imgUnscaledHeight);
                            } else if (size.getWidth() >= 0 && size.getHeight() >= 0) {
                                // The width and height have been expressly defined. Note that AUTO is -1,
                                // and all other negative values are disallowed, so by checking >= 0, we
                                // are essentially saying "if neither is AUTO"
                                tileWidth = w;
                                tileHeight = h;
                            } else if (w >= 0) {
                                // In this case, the width is specified, but the height is AUTO
                                tileWidth = w;
                                final double scale = tileWidth / imgUnscaledWidth;
                                tileHeight = imgUnscaledHeight * scale;
                            } else if (h >= 0) {
                                // Here the height is specified and the width is AUTO
                                tileHeight = h;
                                final double scale = tileHeight / imgUnscaledHeight;
                                tileWidth = imgUnscaledWidth * scale;
                            } else {
                                // Both are auto.
                                tileWidth = imgUnscaledWidth;
                                tileHeight = imgUnscaledHeight;
                            }

                            // Now figure out where we are going to place the images within the region.
                            // For example, the developer can ask for 20px or 20%, and we need to first
                            // determine where to place the image. This starts by figuring out the pixel
                            // based value for the position.
                            final BackgroundPosition pos = image.getPosition();
                            final double tileX, tileY;

                            if (pos.getHorizontalSide() == Side.LEFT) {
                                final double position = pos.getHorizontalPosition();
                                if (pos.isHorizontalAsPercentage()) {
                                    tileX = (position * width) - (position * tileWidth);
                                } else {
                                    tileX = position;
                                }
                            } else {
                                if (pos.isHorizontalAsPercentage()) {
                                    final double position = 1 - pos.getHorizontalPosition();
                                    tileX = (position * width) - (position * tileWidth);
                                } else {
                                    tileX = width - tileWidth- pos.getHorizontalPosition();
                                }
                            }

                            if (pos.getVerticalSide() == Side.TOP) {
                                final double position = pos.getVerticalPosition();
                                if (pos.isVerticalAsPercentage()) {
                                    tileY = (position * height) - (position * tileHeight);
                                } else {
                                    tileY = position;
                                }
                            } else {
                                if (pos.isVerticalAsPercentage()) {
                                    final double position = 1 - pos.getVerticalPosition();
                                    tileY = (position * height) - (position * tileHeight);
                                } else {
                                    tileY = height - tileHeight - pos.getVerticalPosition();
                                }
                            }

                            // Now that we have acquired or computed all the data, we'll let paintTiles
                            // do the actual rendering operation.
                            paintTiles(g, img, image.getRepeatX(), image.getRepeatY(),
                                       pos.getHorizontalSide(), pos.getVerticalSide(),
                                       0, 0, width, height, // the region area to fill with the image
                                       0, 0, imgWidth, imgHeight, // The entire image is used
                                       (float) tileX, (float) tileY, (float) tileWidth, (float) tileHeight);
                        }
                    }
                }
            }

            if (!border.isEmpty()) {
                final List<BorderStroke> strokes = border.getStrokes();
                for (int i = 0, max = strokes.size(); i < max; i++) {
                    final BorderStroke stroke = strokes.get(i);
                    final BorderWidths widths = stroke.getWidths();
                    final CornerRadii radii = normalize(stroke.getRadii());
                    final Insets insets = stroke.getInsets();

                    final javafx.scene.paint.Paint topStroke = stroke.getTopStroke();
                    final javafx.scene.paint.Paint rightStroke = stroke.getRightStroke();
                    final javafx.scene.paint.Paint bottomStroke = stroke.getBottomStroke();
                    final javafx.scene.paint.Paint leftStroke = stroke.getLeftStroke();

                    final float topInset = (float) insets.getTop();
                    final float rightInset = (float) insets.getRight();
                    final float bottomInset = (float) insets.getBottom();
                    final float leftInset = (float) insets.getLeft();

                    final float topWidth = (float) (widths.isTopAsPercentage() ? height * widths.getTop() : widths.getTop());
                    final float rightWidth = (float) (widths.isRightAsPercentage() ? width * widths.getRight() : widths.getRight());
                    final float bottomWidth = (float) (widths.isBottomAsPercentage() ? height * widths.getBottom() : widths.getBottom());
                    final float leftWidth = (float) (widths.isLeftAsPercentage() ? width * widths.getLeft() : widths.getLeft());

                    final BorderStrokeStyle topStyle = stroke.getTopStyle();
                    final BorderStrokeStyle rightStyle = stroke.getRightStyle();
                    final BorderStrokeStyle bottomStyle = stroke.getBottomStyle();
                    final BorderStrokeStyle leftStyle = stroke.getLeftStyle();

                    final StrokeType topType = topStyle.getType();
                    final StrokeType rightType = rightStyle.getType();
                    final StrokeType bottomType = bottomStyle.getType();
                    final StrokeType leftType = leftStyle.getType();

                    // The Prism Graphics logic always strokes on the line, it doesn't know about
                    // INSIDE or OUTSIDE or how to handle those. The only way to deal with those is
                    // to compensate for them here. So we will adjust the bounds that we are going
                    // to stroke to take into account the insets (obviously), and also where we
                    // want the stroked line to appear (inside, or outside, or centered).
                    final float t = topInset +
                            (topType == StrokeType.OUTSIDE ? -topWidth / 2 :
                             topType == StrokeType.INSIDE ? topWidth / 2 : 0);
                    final float l = leftInset +
                            (leftType == StrokeType.OUTSIDE ? -leftWidth / 2 :
                             leftType == StrokeType.INSIDE ? leftWidth / 2 : 0);
                    final float b = bottomInset +
                            (bottomType == StrokeType.OUTSIDE ? -bottomWidth / 2 :
                             bottomType == StrokeType.INSIDE ? bottomWidth / 2 : 0);
                    final float r = rightInset +
                            (rightType == StrokeType.OUTSIDE ? -rightWidth / 2 :
                             rightType == StrokeType.INSIDE ? rightWidth / 2 : 0);

                    // If the radii are uniform, then reading any one value is sufficient to
                    // know what the radius is for all values
                    final float radius = (float) radii.getTopLeftHorizontalRadius();
                    if (stroke.isStrokeUniform()) {
                        // If the stroke is uniform, then that means that the style, width, and stroke of
                        // all four sides is the same.
                        float w = width - l - r;
                        float h = height - t - b;
                        // The length of each side of the path we're going to stroke
                        final double di = 2 * radii.getTopLeftHorizontalRadius();
                        final double circle = di*Math.PI;
                        final double totalLineLength =
                                circle +
                                2 * (width - di) +
                                2 * (height - di);

                        if (w >= 0 && h >= 0) {
                            setBorderStyle(g, stroke, totalLineLength);
                            if (radii.isUniform() && radius == 0) {
                                // We're just drawing a squared stroke on all four sides of the same style
                                // and width and color, so a simple drawRect call is all that is needed.
                                g.drawRect(l, t, w, h);
                            } else if (radii.isUniform()) {
                                // The radii are uniform, but are not squared up, so we have to
                                // draw a rounded rectangle.
                                float ar = radius + radius;
                                if (ar > w) ar = w;
                                if (ar > h) ar = h;
                                g.drawRoundRect(l, t, w, h, ar, ar);
                            } else {
                                // We do not have uniform radii, so we need to create a path that represents
                                // the stroke and then draw that.
                                g.draw(createPath(t, l, b, r, radii));
                            }
                        }
                    } else if (radii.isUniform() && radius == 0) {
                        // The length of each side of the path we're going to stroke
                        final double totalLineLength = 2 * width + 2 * height;

                        // We have different styles, or widths, or strokes on one or more sides, and
                        // therefore we have to draw each side independently. However, the corner radii
                        // are all 0, so we don't have to go to the trouble of constructing some complicated
                        // path to represent the border, we just draw each line independently.
                        // Note that in each of these checks, if the stroke is identity equal to the TRANSPARENT
                        // Color or the style is identity equal to BorderStrokeStyle.NONE, then we skip that
                        // side. It is possible however to have a Color as the stroke which is effectively
                        // TRANSPARENT and a style that is effectively NONE, but we are not checking for those
                        // cases and will in those cases be doing more work than necessary.
                        // TODO make sure CSS uses TRANSPARENT and NONE when possible (RT-26943)
                        if (!(topStroke instanceof Color && ((Color)topStroke).getOpacity() == 0f) && topStyle != BorderStrokeStyle.NONE) {
                            g.setPaint(getPlatformPaint(topStroke));
                            if (BorderStrokeStyle.SOLID == topStyle) {
                                g.fillRect(leftInset, topInset, width - leftInset - rightInset, topWidth);
                            } else {
                                g.setStroke(createStroke(topStyle, topWidth, totalLineLength));
                                g.drawLine(l, t, width - r, t);
                            }
                        }

                        if (!(rightStroke instanceof Color && ((Color)rightStroke).getOpacity() == 0f) && rightStyle != BorderStrokeStyle.NONE) {
                            g.setPaint(getPlatformPaint(rightStroke));
                            if (BorderStrokeStyle.SOLID == rightStyle) {
                                g.fillRect(width - rightInset - rightWidth, topInset,
                                           rightWidth, height - topInset - bottomInset);
                            } else {
                                g.setStroke(createStroke(rightStyle, rightWidth, totalLineLength));
                                g.drawLine(width - r, topInset, width - r, height - bottomInset);
                            }
                        }

                        if (!(bottomStroke instanceof Color && ((Color)bottomStroke).getOpacity() == 0f) && bottomStyle != BorderStrokeStyle.NONE) {
                            g.setPaint(getPlatformPaint(bottomStroke));
                            if (BorderStrokeStyle.SOLID == bottomStyle) {
                                g.fillRect(leftInset, height - bottomInset - bottomWidth,
                                        width - leftInset - rightInset, bottomWidth);
                            } else {
                                g.setStroke(createStroke(bottomStyle, bottomWidth, totalLineLength));
                                g.drawLine(l, height - b, width - r, height - b);
                            }
                        }

                        if (!(leftStroke instanceof Color && ((Color)leftStroke).getOpacity() == 0f) && leftStyle != BorderStrokeStyle.NONE) {
                            g.setPaint(getPlatformPaint(leftStroke));
                            if (BorderStrokeStyle.SOLID == leftStyle) {
                                g.fillRect(leftInset, topInset, leftWidth, height - topInset - bottomInset);
                            } else {
                                g.setStroke(createStroke(leftStyle, leftWidth, totalLineLength));
                                g.drawLine(l, topInset, l, height - bottomInset);
                            }
                        }
                    } else {
                        // In this case, we have different styles and/or strokes and/or widths on one or
                        // more sides, and either the radii are not uniform, or they are uniform but greater
                        // than 0. In this case we have to take a much slower rendering path by turning this
                        // stroke into a path (or in the current implementation, an array of paths).
                        Shape[] paths = createPaths(t, l, b, r, radii);
                        // TODO This is incorrect for an ellipse (RT-26942)
                        final double totalLineLength =
                                // TOP
                                (width - radii.getTopLeftHorizontalRadius() - radii.getTopRightHorizontalRadius()) +
                                (Math.PI * radii.getTopLeftHorizontalRadius() / 4) +
                                (Math.PI * radii.getTopRightHorizontalRadius() / 4) +
                                // RIGHT
                                (height - radii.getTopRightVerticalRadius() - radii.getBottomRightVerticalRadius()) +
                                (Math.PI * radii.getTopRightVerticalRadius() / 4) +
                                (Math.PI * radii.getBottomRightVerticalRadius() / 4) +
                                // BOTTOM
                                (width - radii.getBottomLeftHorizontalRadius() - radii.getBottomRightHorizontalRadius()) +
                                (Math.PI * radii.getBottomLeftHorizontalRadius() / 4) +
                                (Math.PI * radii.getBottomRightHorizontalRadius() / 4) +
                                // LEFT
                                (height - radii.getTopLeftVerticalRadius() - radii.getBottomLeftVerticalRadius()) +
                                (Math.PI * radii.getTopLeftVerticalRadius() / 4) +
                                (Math.PI * radii.getBottomLeftVerticalRadius() / 4);

                        if (topStyle != BorderStrokeStyle.NONE) {
                            g.setStroke(createStroke(topStyle, topWidth, totalLineLength));
                            g.setPaint(getPlatformPaint(topStroke));
                            g.draw(paths[0]);
                        }
                        if (rightStyle != BorderStrokeStyle.NONE) {
                            g.setStroke(createStroke(rightStyle, rightWidth, totalLineLength));
                            g.setPaint(getPlatformPaint(rightStroke));
                            g.draw(paths[1]);
                        }
                        if (bottomStyle != BorderStrokeStyle.NONE) {
                            g.setStroke(createStroke(bottomStyle, bottomWidth, totalLineLength));
                            g.setPaint(getPlatformPaint(bottomStroke));
                            g.draw(paths[2]);
                        }
                        if (leftStyle != BorderStrokeStyle.NONE) {
                            g.setStroke(createStroke(leftStyle, leftWidth, totalLineLength));
                            g.setPaint(getPlatformPaint(leftStroke));
                            g.draw(paths[3]);
                        }
                    }
                }

                final List<BorderImage> images = border.getImages();
                for (int i = 0, max = images.size(); i < max; i++) {
                    final BorderImage ib = images.get(i);
                    final Image img = (Image) ib.getImage().impl_getPlatformImage();
                    final int imgWidth = img.getWidth();
                    final int imgHeight = img.getHeight();
                    if (img != null) {
                        final BorderWidths widths = ib.getWidths();
                        final Insets insets = ib.getInsets();
                        final BorderWidths slices = ib.getSlices();

                        // we will get gaps if we don't round to pixel boundaries
                        final int topInset = (int) Math.round(insets.getTop());
                        final int rightInset = (int) Math.round(insets.getRight());
                        final int bottomInset = (int) Math.round(insets.getBottom());
                        final int leftInset = (int) Math.round(insets.getLeft());

                        final int topWidth = (int) Math.round(
                                widths.isTopAsPercentage() ? height * widths.getTop() : widths.getTop());
                        final int rightWidth = (int) Math.round(
                                widths.isRightAsPercentage() ? width * widths.getRight() : widths.getRight());
                        final int bottomWidth = (int) Math.round(
                                widths.isBottomAsPercentage() ? height * widths.getBottom() : widths.getBottom());
                        final int leftWidth = (int) Math.round(
                                widths.isLeftAsPercentage() ? width * widths.getLeft() : widths.getLeft());

                        final int topSlice = (int) Math.round((
                                slices.isTopAsPercentage() ? height * slices.getTop() : slices.getTop()) * img.getPixelScale());
                        final int rightSlice = (int) Math.round((
                                slices.isRightAsPercentage() ? width * slices.getRight() : slices.getRight()) * img.getPixelScale());
                        final int bottomSlice = (int) Math.round((
                                slices.isBottomAsPercentage() ? height * slices.getBottom() : slices.getBottom()) * img.getPixelScale());
                        final int leftSlice = (int) Math.round((
                                slices.isLeftAsPercentage() ? width * slices.getLeft() : slices.getLeft()) * img.getPixelScale());

                        // handle case where region is too small to fit in borders
                        if ((leftInset + leftWidth + rightInset + rightWidth) > width
                                || (topInset + topWidth + bottomInset + bottomWidth) > height) {
                            continue;
                        }

                        // calculate some things we can share
                        final int centerMinX = leftInset + leftWidth;
                        final int centerMinY = topInset + topWidth;
                        final int centerW = Math.round(width) - rightInset - rightWidth - centerMinX;
                        final int centerH = Math.round(height) - bottomInset - bottomWidth - centerMinY;
                        final int centerMaxX = centerW + centerMinX;
                        final int centerMaxY = centerH + centerMinY;
                        final int centerSliceWidth = imgWidth - leftSlice - rightSlice;
                        final int centerSliceHeight = imgHeight - topSlice - bottomSlice;
                        // paint top left corner
                        paintTiles(g, img, BorderRepeat.STRETCH, BorderRepeat.STRETCH, Side.LEFT, Side.TOP,
                                   leftInset, topInset, leftWidth, topWidth, // target bounds
                                   0, 0, leftSlice, topSlice, // src image bounds
                                   0, 0, leftWidth, topWidth); // tile bounds
                        // paint top slice
                        float tileWidth = (ib.getRepeatX() == BorderRepeat.STRETCH) ?
                                centerW : (topSlice > 0 ? (centerSliceWidth * topWidth) / topSlice : 0);
                        float tileHeight = topWidth;
                        paintTiles(
                                g, img, ib.getRepeatX(), BorderRepeat.STRETCH, Side.LEFT, Side.TOP,
                                centerMinX, topInset, centerW, topWidth,
                                leftSlice, 0, centerSliceWidth, topSlice,
                                (centerW - tileWidth) / 2, 0, tileWidth, tileHeight);
                        // paint top right corner
                        paintTiles(g, img, BorderRepeat.STRETCH, BorderRepeat.STRETCH, Side.LEFT, Side.TOP,
                                   centerMaxX, topInset, rightWidth, topWidth,
                                   (imgWidth - rightSlice), 0, rightSlice, topSlice,
                                   0, 0, rightWidth, topWidth);
                        // paint left slice
                        tileWidth = leftWidth;
                        tileHeight = (ib.getRepeatY() == BorderRepeat.STRETCH) ?
                                centerH : (leftSlice > 0 ? (leftWidth * centerSliceHeight) / leftSlice : 0);
                        paintTiles(g, img, BorderRepeat.STRETCH, ib.getRepeatY(), Side.LEFT, Side.TOP,
                                   leftInset, centerMinY, leftWidth, centerH,
                                   0, topSlice, leftSlice, centerSliceHeight,
                                   0, (centerH - tileHeight) / 2, tileWidth, tileHeight);
                        // paint right slice
                        tileWidth = rightWidth;
                        tileHeight = (ib.getRepeatY() == BorderRepeat.STRETCH) ?
                                centerH : (rightSlice > 0 ? (rightWidth * centerSliceHeight) / rightSlice : 0);
                        paintTiles(g, img, BorderRepeat.STRETCH, ib.getRepeatY(), Side.LEFT, Side.TOP,
                                   centerMaxX, centerMinY, rightWidth, centerH,
                                   imgWidth - rightSlice, topSlice, rightSlice, centerSliceHeight,
                                   0, (centerH - tileHeight) / 2, tileWidth, tileHeight);
                        // paint bottom left corner
                        paintTiles(g, img, BorderRepeat.STRETCH, BorderRepeat.STRETCH, Side.LEFT, Side.TOP,
                                   leftInset, centerMaxY, leftWidth, bottomWidth,
                                   0, imgHeight - bottomSlice, leftSlice, bottomSlice,
                                   0, 0, leftWidth, bottomWidth);
                        // paint bottom slice
                        tileWidth = (ib.getRepeatX() == BorderRepeat.STRETCH) ?
                                centerW : (bottomSlice > 0 ? (centerSliceWidth * bottomWidth) / bottomSlice : 0);
                        tileHeight = bottomWidth;
                        paintTiles(g, img, ib.getRepeatX(), BorderRepeat.STRETCH, Side.LEFT, Side.TOP,
                                   centerMinX, centerMaxY, centerW, bottomWidth,
                                   leftSlice, imgHeight - bottomSlice, centerSliceWidth, bottomSlice,
                                   (centerW - tileWidth) / 2, 0, tileWidth, tileHeight);
                        // paint bottom right corner
                        paintTiles(g, img, BorderRepeat.STRETCH, BorderRepeat.STRETCH, Side.LEFT, Side.TOP,
                                   centerMaxX, centerMaxY, rightWidth, bottomWidth,
                                   imgWidth - rightSlice, imgHeight - bottomSlice, rightSlice, bottomSlice,
                                   0, 0, rightWidth, bottomWidth);
                        // paint the center slice
                        if (ib.isFilled()) {
                            // we will get gaps if we don't round to pixel boundaries
                            final int areaX = leftInset + leftWidth;
                            final int areaY = topInset + topWidth;
                            final int areaW = Math.round(width) - rightInset - rightWidth - areaX;
                            final int areaH = Math.round(height) - bottomInset - bottomWidth - areaY;
                            // handle no repeat as stretch
                            final float imgW = (ib.getRepeatX() == BorderRepeat.STRETCH) ? centerW : centerSliceWidth;
                            final float imgH = (ib.getRepeatY() == BorderRepeat.STRETCH) ? centerH : centerSliceHeight;
                            paintTiles(g, img, ib.getRepeatX(), ib.getRepeatY(), Side.LEFT, Side.TOP,
                                       areaX, areaY, areaW, areaH,
                                       leftSlice, topSlice, centerSliceWidth, centerSliceHeight,
                                       0, 0, imgW, imgH);
                        }
                    }
                }
            }
        }

        // Paint the children
        super.renderContent(g);
    }

    private int roundUp(double d) {
        return (d - (int)d) == 0 ? (int) d : (int) (d + 1);
    }

    private CornerRadii normalize(CornerRadii radii) {
        final double tlvr = radii.isTopLeftVerticalRadiusAsPercentage() ? height * radii.getTopLeftVerticalRadius() : radii.getTopLeftVerticalRadius();
        final double tlhr = radii.isTopLeftHorizontalRadiusAsPercentage() ? width * radii.getTopLeftHorizontalRadius() : radii.getTopLeftHorizontalRadius();
        final double trvr = radii.isTopRightVerticalRadiusAsPercentage() ? height * radii.getTopRightVerticalRadius() : radii.getTopRightVerticalRadius();
        final double trhr = radii.isTopRightHorizontalRadiusAsPercentage() ? width * radii.getTopRightHorizontalRadius() : radii.getTopRightHorizontalRadius();
        final double brvr = radii.isBottomRightVerticalRadiusAsPercentage() ? height * radii.getBottomRightVerticalRadius() : radii.getBottomRightVerticalRadius();
        final double brhr = radii.isBottomRightHorizontalRadiusAsPercentage() ? width * radii.getBottomRightHorizontalRadius() : radii.getBottomRightHorizontalRadius();
        final double blvr = radii.isBottomLeftVerticalRadiusAsPercentage() ? height * radii.getBottomLeftVerticalRadius() : radii.getBottomLeftVerticalRadius();
        final double blhr = radii.isBottomLeftHorizontalRadiusAsPercentage() ? width * radii.getBottomLeftHorizontalRadius() : radii.getBottomLeftHorizontalRadius();
        return new CornerRadii(tlhr, tlvr, trvr, trhr, brhr, brvr, blvr, blhr, false, false, false, false, false, false, false, false);
    }

    /**
     * Creates a Prism BasicStroke based on the stroke style, width, and line length.
     *
     * @param sb             The BorderStrokeStyle
     * @param strokeWidth    The width of the stroke we're going to draw
     * @param lineLength     The total linear length of this stroke. This is needed for
     *                       handling "dashed" and "dotted" cases, otherwise, it is ignored.
     * @return A prism BasicStroke
     */
    private BasicStroke createStroke(BorderStrokeStyle sb, double strokeWidth, double lineLength) {
        int cap;
        if (sb.getLineCap() == StrokeLineCap.BUTT) {
            cap = BasicStroke.CAP_BUTT;
        } else if (sb.getLineCap() == StrokeLineCap.SQUARE) {
            cap = BasicStroke.CAP_SQUARE;
        } else {
            cap = BasicStroke.CAP_ROUND;
        }

        int join;
        if (sb.getLineJoin() == StrokeLineJoin.BEVEL) {
            join = BasicStroke.JOIN_BEVEL;
        } else if (sb.getLineJoin() == StrokeLineJoin.MITER) {
            join = BasicStroke.JOIN_MITER;
        } else {
            join = BasicStroke.JOIN_ROUND;
        }

        // If we're doing an INNER or OUTER stroke, then double the width. We end
        // up trimming off the inner portion when doing an OUTER, or the outer
        // portion when doing an INNER.
        // NOTE: It doesn't appear that we have any code to actually draw INNER or OUTER strokes
//        if (sb.getType() != StrokeType.CENTERED) {
//            strokeWidth *= 2.0f;
//        }

        BasicStroke bs;
        if (sb == BorderStrokeStyle.NONE) {
            throw new AssertionError("Should never have been asked to draw a border with NONE");
        } else if (sb.getDashArray().size() > 0) {
            List<Double> dashArray = sb.getDashArray();
            double[] array;
            float dashOffset;
            if (dashArray == BorderStrokeStyle.DOTTED.getDashArray()) {
                // NOTE: IF line length is > 0, then we are going to do some math to try to make the resulting
                // dots look pleasing. It is set to -1 if we are stroking a random path (vs. a rounded rect), in
                // which case we are going to just scale the dotting pattern based on the stroke width, but we won't
                // try to adjust the phase to make it look better.
                if (lineLength > 0) {
                    // For DOTTED we want the dash array to be 0, val, where the "val" is as close to strokewidth*2 as
                    // possible, but we want the spacing to be such that we get an even spacing between all dots around
                    // the edge.
                    double remainder = lineLength % (strokeWidth * 2);
                    double numSpaces = lineLength / (strokeWidth * 2);
                    double spaceWidth = (strokeWidth * 2) + (remainder / numSpaces);
                    array = new double[] {0, spaceWidth};
                    dashOffset = 0;
                } else {
                    array = new double[] {0, strokeWidth * 2};
                    dashOffset = 0;
                }
            } else if (dashArray == BorderStrokeStyle.DASHED.getDashArray()) {
                // NOTE: IF line length is > 0, then we are going to do some math to try to make the resulting
                // dash look pleasing. It is set to -1 if we are stroking a random path (vs. a rounded rect), in
                // which case we are going to just scale the dashing pattern based on the stroke width, but we won't
                // try to adjust the phase to make it look better.
                if (lineLength > 0) {
                    // For DASHED we want the dash array to be 2*strokewidth, val where "val" is as close to
                    // 1.4*strokewidth as possible, but we want the sapcing to be such that we get an even spacing between
                    // all dashes around the edge. Maybe we can start with the dash phase at half the dash length.
                    final double dashLength = strokeWidth * 2;
                    double gapLength = strokeWidth * 1.4;
                    final double segmentLength = dashLength + gapLength;
                    final double divided = lineLength / segmentLength;
                    final double numSegments = (int) divided;
                    final double dashCumulative = numSegments * dashLength;
                    gapLength = (lineLength - dashCumulative) / numSegments;
                    array = new double[] {dashLength, gapLength};
                    dashOffset = (float) (dashLength*.6);
                } else {
                    array = new double[] {2 * strokeWidth, 1.4 * strokeWidth};
                    dashOffset = 0;
                }
            } else {
                // If we are not DASHED or DOTTED or we're stroking a path and not a basic rounded rectangle
                // so we just take what we've been given.
                array = new double[dashArray.size()];
                for (int i=0; i<array.length; i++) {
                    array[i] = dashArray.get(i);
                }
                dashOffset = (float) sb.getDashOffset();
            }

            bs = new BasicStroke((float) strokeWidth, cap, join,
                    (float) sb.getMiterLimit(),
                    array, dashOffset);
        } else {
            bs = new BasicStroke((float) strokeWidth, cap, join,
                    (float) sb.getMiterLimit());
        }

        return bs;
    }

    private void setBorderStyle(Graphics g, BorderStroke sb, double length) {
        // Any one of, or all of, the sides could be 'none'.
        // Take the first side that isn't.
        final BorderWidths widths = sb.getWidths();
        BorderStrokeStyle bs = sb.getTopStyle();
        double sbWidth = widths.isTopAsPercentage() ? height * widths.getTop() : widths.getTop();
        Object sbFill = getPlatformPaint(sb.getTopStroke());
        if (bs == null) {
            bs = sb.getLeftStyle();
            sbWidth = widths.isLeftAsPercentage() ? width * widths.getLeft() : widths.getLeft();
            sbFill = getPlatformPaint(sb.getLeftStroke());
            if (bs == null) {
                bs = sb.getBottomStyle();
                sbWidth = widths.isBottomAsPercentage() ? height * widths.getBottom() : widths.getBottom();
                sbFill = getPlatformPaint(sb.getBottomStroke());
                if (bs == null) {
                    bs = sb.getRightStyle();
                    sbWidth = widths.isRightAsPercentage() ? width * widths.getRight() : widths.getRight();
                    sbWidth = widths.isRightAsPercentage() ? width * widths.getRight() : widths.getRight();
                    sbFill = sb.getRightStroke();
                }
            }
        }
        if (bs == null || bs == BorderStrokeStyle.NONE) {
            return;
        }
        g.setStroke(createStroke(bs, sbWidth, length));
        g.setPaint((Paint) sbFill);
    }

    // If we generate the coordinates for the "start point, corner, end point"
    // triplets for each corner arc on the border going clockwise from the
    // upper left, we get the following pattern (X, Y == corner coords):
    //
    // 0 - Top Left:      X + 0, Y + R,      X, Y,      X + R, Y + 0
    // 1 - Top Right:     X - R, Y + 0,      X, Y,      X + 0, Y + R
    // 2 - Bottom Right:  X + 0, Y - R,      X, Y,      X - R, Y + 0
    // 3 - Bottom Left:   X + R, Y + 0,      X, Y,      X + 0, Y - R
    //
    // The start and end points are just the corner coordinate + {-R, 0, +R}.
    // If we view these four lines as the following line with appropriate
    // values for A, B, C, D:
    //
    //     General form:  X + A, Y + B,      X, Y,      X + C, Y + D
    //
    // We see that C == B and D == -A in every case so we really only have
    // 2 constants and the following reduced general form:
    //
    //     Reduced form:  X + A, Y + B,      X, Y,      X + B, Y - A
    //
    // You might note that these values are actually related to the sin
    // and cos of 90 degree angles and the relationship between (A,B) and (C,D)
    // is just that of a 90 degree rotation.  We can thus use the following
    // trigonometric "quick lookup" array and the relationships:
    //
    // 1. cos(quadrant) == sin(quadrant + 1)
    // 2. dx,dy for the end point
    //      == dx,dy for the start point + 90 degrees
    //      == dy,-dx
    //
    // Note that the array goes through 6 quadrants to allow us to look
    // 2 quadrants past a complete circle.  We need to go one quadrant past
    // so that we can compute cos(4th quadrant) == sin(5th quadrant) and we
    // also need to allow one more quadrant because the makeRoundedEdge
    // method always computes 2 consecutive rounded corners at a time.
    private static final float SIN_VALUES[] = { 1f, 0f, -1f, 0f, 1f, 0f};

    private void doCorner(Path2D path, float x, float y, float r, int quadrant) {
        if (r > 0) {
            float dx = r * SIN_VALUES[quadrant + 1]; // cos(quadrant)
            float dy = r * SIN_VALUES[quadrant];
            path.appendOvalQuadrant(x + dx, y + dy, x, y, x + dy, y - dx, 0f, 1f,
                                    (quadrant == 0)
                                        ? Path2D.CornerPrefix.MOVE_THEN_CORNER
                                        : Path2D.CornerPrefix.LINE_THEN_CORNER);
        } else if (quadrant == 0) {
            path.moveTo(x, y);
        } else {
            path.lineTo(x, y);
        }
    }

    /** Creates a rounded rectangle path with our width and height, different corner radii, offset with given offsets */
    private Path2D createPath(float t, float l, float bo, float ro, CornerRadii radii) {
        float r = width - ro;
        float b = height - bo;
        // TODO have to teach this method how to handle vertical radii (RT-26941)
        float tlr = (float) radii.getTopLeftHorizontalRadius();
        float trr = (float) radii.getTopRightHorizontalRadius();
        float blr = (float) radii.getBottomLeftHorizontalRadius();
        float brr = (float) radii.getBottomRightHorizontalRadius();
        float ratio = getReducingRatio(r - l, b - t, tlr, trr, blr, brr);
        if (ratio < 1.0f) {
            tlr *= ratio;
            trr *= ratio;
            blr *= ratio;
            brr *= ratio;
        }
        Path2D path = new Path2D();
        doCorner(path, l, t, tlr, 0);
        doCorner(path, r, t, trr, 1);
        doCorner(path, r, b, brr, 2);
        doCorner(path, l, b, blr, 3);
        path.closePath();
        return path;
    }

    private Path2D makeRoundedEdge(float x0, float y0, float x1, float y1,
                                   float r0, float r1, int quadrant)
    {
        Path2D path = new Path2D();
        if (r0 > 0) {
            float dx = r0 * SIN_VALUES[quadrant + 1];  // cos(quadrant)
            float dy = r0 * SIN_VALUES[quadrant];
            path.appendOvalQuadrant(x0 + dx, y0 + dy, x0, y0, x0 + dy, y0 - dx,
                                    0.5f, 1f, Path2D.CornerPrefix.MOVE_THEN_CORNER);
        } else {
            path.moveTo(x0, y0);
        }
        if (r1 > 0) {
            float dx = r1 * SIN_VALUES[quadrant + 2];  // cos(quadrant + 1)
            float dy = r1 * SIN_VALUES[quadrant + 1];
            path.appendOvalQuadrant(x1 + dx, y1 + dy, x1, y1, x1 + dy, y1 - dx,
                                    0f, 0.5f, Path2D.CornerPrefix.LINE_THEN_CORNER);
        } else {
            path.lineTo(x1, y1);
        }
        return path;
    }

    /**
     * Creates a rounded rectangle path with our width and heigh, different corner radii, offset with given offsets.
     * Each side as a separate path.  The sides are returned in the CSS standard
     * order of top, right, bottom, left.
     */
    private Path2D[] createPaths(float t, float l, float bo, float ro, CornerRadii radii)
    {
        // TODO have to teach how to handle the other 4 radii (RT-26941)
        float tlr = (float) radii.getTopLeftHorizontalRadius(),
            trr = (float) radii.getTopRightHorizontalRadius(),
            blr = (float) radii.getBottomLeftHorizontalRadius(),
            brr = (float) radii.getBottomRightHorizontalRadius();
        float r = width - ro;
        float b = height - bo;
        float ratio = getReducingRatio(r - l, b - t, tlr, trr, blr, brr);
        if (ratio < 1.0f) {
            tlr *= ratio;
            trr *= ratio;
            blr *= ratio;
            brr *= ratio;
        }
        return new Path2D[] {
            makeRoundedEdge(l, t, r, t, tlr, trr, 0), // top
            makeRoundedEdge(r, t, r, b, trr, brr, 1), // right
            makeRoundedEdge(r, b, l, b, brr, blr, 2), // bottom
            makeRoundedEdge(l, b, l, t, blr, tlr, 3), // left
        };
    }

    /**
     * Create a bigger or smaller version of shape. If not scaleShape then the shape is just centered rather
     * than resized. Proportions are not maintained when resizing. This is necessary so as to ensure
     * that the fill never looks scaled. For example, a tile-imaged based background will look stretched
     * if we were to render a scaled shape. Instead, we produce a new shape based on the scaled size and
     * then fill that shape without additional transforms.
     */
    private Shape resizeShape(float topOffset, float rightOffset, float bottomOffset, float leftOffset) {
        // The bounds of the shape, before any centering / scaling takes place
        final RectBounds bounds = shape.getBounds();
        if (scaleShape) {
            // First we need to modify the transform to scale the shape so that it will fit
            // within the insets.
            SCRATCH_AFFINE.setToIdentity();
            SCRATCH_AFFINE.translate(leftOffset, topOffset);
            // width & height are the width and height of the region. w & h are the width and height
            // of the box within which the new shape must fit.
            final float w = width - leftOffset - rightOffset;
            final float h = height - topOffset - bottomOffset;
            SCRATCH_AFFINE.scale(w / bounds.getWidth(), h / bounds.getHeight());
            // If we also need to center it, we need to adjust the transform so as to place
            // the shape in the center of the bounds
            if (centerShape) {
                SCRATCH_AFFINE.translate(-bounds.getMinX(), -bounds.getMinY());
            }
            return SCRATCH_AFFINE.createTransformedShape(shape);
        } else if (centerShape) {
            // We are only centering. In this case, what we want is for the
            // original shape to be centered. If there are offsets (insets)
            // then we must pre-scale about the center to account for it.
            final float boundsWidth = bounds.getWidth();
            final float boundsHeight = bounds.getHeight();
            float newW = boundsWidth - leftOffset - rightOffset;
            float newH = boundsHeight - topOffset - bottomOffset;
            SCRATCH_AFFINE.setToIdentity();
            SCRATCH_AFFINE.translate(leftOffset + (width - boundsWidth)/2 - bounds.getMinX(),
                                     topOffset + (height - boundsHeight)/2 - bounds.getMinY());
            if (newH != boundsHeight || newW != boundsWidth) {
                SCRATCH_AFFINE.translate(bounds.getMinX(), bounds.getMinY());
                SCRATCH_AFFINE.scale(newW / boundsWidth, newH / boundsHeight);
                SCRATCH_AFFINE.translate(-bounds.getMinX(), -bounds.getMinY());
            }
            return SCRATCH_AFFINE.createTransformedShape(shape);
        } else if (topOffset != 0 || rightOffset != 0 || bottomOffset != 0 || leftOffset != 0) {
            // We are neither centering nor scaling, but we still have to resize the
            // shape because we have to fit within the bounds defined by the offsets
            float newW = bounds.getWidth() - leftOffset - rightOffset;
            float newH = bounds.getHeight() - topOffset - bottomOffset;
            SCRATCH_AFFINE.setToIdentity();
            SCRATCH_AFFINE.translate(leftOffset, topOffset);
            SCRATCH_AFFINE.translate(bounds.getMinX(), bounds.getMinY());
            SCRATCH_AFFINE.scale(newW / bounds.getWidth(), newH / bounds.getHeight());
            SCRATCH_AFFINE.translate(-bounds.getMinX(), -bounds.getMinY());
            return SCRATCH_AFFINE.createTransformedShape(shape);
        } else {
            // Nothing has changed, so we can simply return!
            return shape;
        }
    }

    private void paintTiles(Graphics g, Image img, BorderRepeat repeatX, BorderRepeat repeatY, Side horizontalSide, Side verticalSide,
            final float regionX, final float regionY, final float regionWidth, final float regionHeight,
            final int srcX, final int srcY, final int srcW, final int srcH,
            float tileX, float tileY, float tileWidth, float tileHeight)
    {
        BackgroundRepeat rx = null;
        BackgroundRepeat ry = null;

        switch (repeatX) {
            case REPEAT: rx = BackgroundRepeat.REPEAT; break;
            case STRETCH: rx = BackgroundRepeat.NO_REPEAT; break;
            case ROUND: rx = BackgroundRepeat.ROUND; break;
            case SPACE: rx = BackgroundRepeat.SPACE; break;
        }

        switch (repeatY) {
            case REPEAT: ry = BackgroundRepeat.REPEAT; break;
            case STRETCH: ry = BackgroundRepeat.NO_REPEAT; break;
            case ROUND: ry = BackgroundRepeat.ROUND; break;
            case SPACE: ry = BackgroundRepeat.SPACE; break;
        }

        paintTiles(g, img, rx, ry, horizontalSide, verticalSide, regionX, regionY, regionWidth, regionHeight,
                   srcX, srcY, srcW, srcH, tileX, tileY, tileWidth, tileHeight);
    }

    /**
     * Paints a subsection (srcX,srcY,srcW,srcH) of an image tiled or stretched to fill the destination area
     * (regionWidth,regionHeight). It is assumed we are pre-transformed to the correct origin, top left or destination area. When
     * tiling the first tile is positioned within the rectangle (tileX,tileY,tileW,tileH).
     *
     * Drawing two images next to each other on a non-pixel boundary can not be done simply so we use integers here. This
     * assumption may be wrong when drawing though a scale transform.
     *
     * @param g        The graphics context to draw image into
     * @param img       The image to draw
     * @param repeatX   The horizontal repeat style for filling the area with the src image
     * @param repeatY   The vertical repeat style for filling the area with the src image
     * @param horizontalSide The left or right
     * @param verticalSide The top or bottom
     * @param regionX      The top left corner X of the area of the graphics context to fill with our img
     * @param regionY      The top left corner Y of the area of the graphics context to fill with our img
     * @param regionWidth      The width of the area of the graphics context to fill with our img
     * @param regionHeight      The height of the area of the graphics context to fill with our img
     * @param srcX      The top left corner X of the area of the image to paint with
     * @param srcY      The top left corner Y of the area of the image to paint with
     * @param srcW      The width of the area of the image to paint with, -1 to use the original image width
     * @param srcH      The height of the area of the image to paint with, -1 to use the original image height
     * @param tileX     The top left corner X of the area of the first tile within the destination rectangle. In some
     *                  cases we begin by drawing the center tile, and working to the left & right (for example), so
     *                  this value is not always the same as regionX.
     * @param tileY     The top left corner Y of the area of the first tile within the destination rectangle
     * @param tileWidth The width of the area of the first tile within the destination rectangle, if <= 0, then the use intrinsic value
     * @param tileHeight The height of the area of the first tile within the destination rectangle, if <= 0, then the use intrinsic value
     */
    private void paintTiles(Graphics g, Image img, BackgroundRepeat repeatX, BackgroundRepeat repeatY, Side horizontalSide, Side verticalSide,
            final float regionX, final float regionY, final float regionWidth, final float regionHeight,
            final int srcX, final int srcY, final int srcW, final int srcH,
            float tileX, float tileY, float tileWidth, float tileHeight)
    {
        // If the destination width/height is 0 or the src width / height is 0 then we have
        // nothing to draw, so we can just bail.
        if (regionWidth == 0 || regionHeight == 0 || srcW == 0 || srcH == 0) return;

        // At this point we should have real values for the image source coordinates
        assert srcX >= 0 && srcY >= 0 && srcW > 0 && srcH > 0;

        // If we are repeating in both the x & y directions, then we can take a fast path and just
        // use the ImagePattern directly instead of having to issue a large number of drawTexture calls.
        // This is the generally common case where we are tiling the background in both dimensions.
        // Note that this only works if the anchor point is the top-left, otherwise the ImagePattern would
        // not give the correct expected results.
        if (tileX == 0 && tileY == 0 && repeatX == BackgroundRepeat.REPEAT && repeatY == BackgroundRepeat.REPEAT) {
            if (srcX != 0 || srcY != 0 || srcW != img.getWidth() || srcH != img.getHeight()) {
                img = img.createSubImage(srcX, srcY, srcW, srcH);
            }
            g.setPaint(new ImagePattern(img, 0, 0, tileWidth, tileHeight, false, false));
            g.fillRect(regionX, regionY, regionWidth, regionHeight);
        } else {
            // If SPACE repeat mode is being used, then we need to take special action if there is not enough
            // space to have more than one tile. Basically, it needs to act as NO_REPEAT in that case (see
            // section 3.4 of the spec for details under rules for SPACE).
            if (repeatX == BackgroundRepeat.SPACE && (regionWidth < (tileWidth * 2))) {
                repeatX = BackgroundRepeat.NO_REPEAT;
            }

            if (repeatY == BackgroundRepeat.SPACE && (regionHeight < (tileHeight * 2))) {
                repeatY = BackgroundRepeat.NO_REPEAT;
            }

            // The following variables are computed and used in order to lay out the tiles in the x and y directions.
            // "count" is used to keep track of the number of tiles to lay down in the x and y directions.
            final int countX, countY;
            // The amount to increment the dstX and dstY by during the rendering loop. This may be positive or
            //negative and will include any space between tiles.
            final float xIncrement, yIncrement;

            // Based on the repeat mode, populate the above variables
            if (repeatX == BackgroundRepeat.REPEAT) {
                // In some cases we have a large positive offset but are in repeat mode. What we need
                // to do is tile, but we want to do so in such a way that we are "anchored" to the center,
                // or right, or whatnot. That is what offsetX will be used for.
                float offsetX = 0;
                if (tileX != 0) {
                    float mod = tileX % tileWidth;
                    tileX = mod == 0 ? 0 : tileX < 0 ? mod : mod - tileWidth;
                    offsetX = tileX;
                }
                countX = (int) Math.max(1, Math.ceil((regionWidth - offsetX) / tileWidth));
                xIncrement = horizontalSide == Side.RIGHT ? -tileWidth : tileWidth;
            } else if (repeatX == BackgroundRepeat.SPACE) {
                tileX = 0; // Space will always start from the top left
                countX = (int) (regionWidth / tileWidth);
                float remainder = (regionWidth % tileWidth);
                xIncrement = tileWidth + (remainder / (countX - 1));
            } else if (repeatX == BackgroundRepeat.ROUND) {
                tileX = 0; // Round will always start from the top left
                countX = (int) (regionWidth / tileWidth);
                tileWidth = regionWidth / (int)(regionWidth / tileWidth);
                xIncrement = tileWidth;
            } else { // no repeat
                countX = 1;
                xIncrement = horizontalSide == Side.RIGHT ? -tileWidth : tileWidth;
            }

            if (repeatY == BackgroundRepeat.REPEAT) {
                float offsetY = 0;
                if (tileY != 0) {
                    float mod = tileY % tileHeight;
                    tileY = mod == 0 ? 0 : tileY < 0 ? mod : mod - tileHeight;
                    offsetY = tileY;
                }
                countY = (int) Math.max(1, Math.ceil((regionHeight - offsetY) / tileHeight));
                yIncrement = verticalSide == Side.BOTTOM ? -tileHeight : tileHeight;
            } else if (repeatY == BackgroundRepeat.SPACE) {
                tileY = 0; // Space will always start from the top left
                countY = (int) (regionHeight / tileHeight);
                float remainder = (regionHeight % tileHeight);
                yIncrement = tileHeight + (remainder / (countY - 1));
            } else if (repeatY == BackgroundRepeat.ROUND) {
                tileY = 0; // Round will always start from the top left
                countY = (int) (regionHeight / tileHeight);
                tileHeight = regionHeight / (int)(regionHeight / tileHeight);
                yIncrement = tileHeight;
            } else { // no repeat
                countY = 1;
                yIncrement = verticalSide == Side.BOTTOM ? -tileHeight : tileHeight;
            }

            // paint loop
            final Texture texture =
                g.getResourceFactory().getCachedTexture(img, Texture.WrapMode.CLAMP_TO_EDGE);
            final int srcX2 = srcX + srcW;
            final int srcY2 = srcY + srcH;
            final float regionX2 = regionX + regionWidth;
            final float regionY2 = regionY + regionHeight;

            float dstY = regionY + tileY;
            for (int y = 0; y < countY; y++) {
                float dstY2 = dstY + tileHeight;
                float dstX = regionX + tileX;
                for (int x = 0; x < countX; x++) {
                    float dstX2 = dstX + tileWidth;
                    // We don't want to end up rendering if we find that the destination rect is completely
                    // off of the region rendering area
                    boolean skipRender = false;
                    float dx1 = dstX < regionX ? regionX : dstX;
                    float dy1 = dstY < regionY ? regionY : dstY;
                    if (dx1 > regionX2 || dy1 > regionY2) skipRender = true;

                    float dx2 = dstX2 > regionX2 ? regionX2 : dstX2;
                    float dy2 = dstY2 > regionY2 ? regionY2 : dstY2;
                    if (dx2 < regionX || dy2 < regionY) skipRender = true;

                    if (!skipRender) {
                        // We know that dstX, dstY, dstX2, dstY2 overlap the region drawing area. Now we need
                        // to compute the source rectangle, and then draw.
                        float sx1 = dstX < regionX ? srcX + srcW * (-tileX / tileWidth) : srcX;
                        float sy1 = dstY < regionY ? srcY + srcH * (-tileY / tileHeight) : srcY;
                        float sx2 = dstX2 > regionX2 ? srcX2 - srcW * ((dstX2 - regionX2) / tileWidth) : srcX2;
                        float sy2 = dstY2 > regionY2 ? srcY2 - srcH * ((dstY2 - regionY2) / tileHeight) : srcY2;
                        g.drawTexture(texture, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2);
                    }
                    dstX += xIncrement;
                }
                dstY += yIncrement;
            }
            texture.unlock();
        }
    }

    private float getReducingRatio(float w, float h,
                                   float tlr, float trr,
                                   float blr, float brr)
    {
        float ratio = 1.0f;
        // working clockwise TRBL
        if (tlr + trr > w) { // top radii
            ratio = Math.min(ratio, w / (tlr + trr));
        }
        if (trr + brr > h) { // right radii
            ratio = Math.min(ratio, h / (trr + brr));
        }
        if (brr + blr > w) { // bottom radii
            ratio = Math.min(ratio, w / (brr + blr));
        }
        if (blr + tlr > h) { // left radii
            ratio = Math.min(ratio, h / (blr + tlr));
        }
        return ratio;
    }

}
