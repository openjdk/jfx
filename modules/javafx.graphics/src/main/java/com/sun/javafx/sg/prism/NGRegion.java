/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;

import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

import com.sun.glass.ui.Screen;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.logging.PulseLogger;
import static com.sun.javafx.logging.PulseLogger.PULSE_LOGGING_ENABLED;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.BasicStroke;
import com.sun.prism.Graphics;
import com.sun.prism.Image;
import com.sun.prism.RTTexture;
import com.sun.prism.Texture;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.paint.ImagePattern;
import com.sun.prism.paint.Paint;
import com.sun.prism.PrinterGraphics;
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
     * Temporary rect for general use. Because this is a static variable,
     * it is only intended to be used from a single thread, the render thread
     * in this case.
     */
    private static final Rectangle TEMP_RECT = new Rectangle();

    /**
     * Screen to RegionImageCache mapping. This mapping is required as textures
     * are only valid in graphics context used to create them (relies on a one
     * to one mapping between Screen and GraphicsContext).
     */
    private static WeakHashMap<Screen, RegionImageCache> imageCacheMap = new WeakHashMap<>();

    /**
     * Indicates the cached image can be sliced vertically.
     */
    private static final int CACHE_SLICE_V = 0x1;

    /**
     * Indicates the cached image can be sliced horizontally.
     */
    private static final int CACHE_SLICE_H = 0x2;

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
     * The normalized list of CornerRadii have been precomputed at the FX layer to
     * properly account for percentages, insets and radii scaling to prevent
     * the radii from overflowing the dimensions of the region.
     * The List objects are shared with the FX layer and are therefore
     * unmodifiable.  If the normalized list is null then it means that all
     * of the raw radii in the list were already absolute and non-overflowing
     * and so the originals can be used from the arrays of strokes and fills.
     */
    private List<CornerRadii> normalizedFillCorners;
    private List<CornerRadii> normalizedStrokeCorners;

    /**
     * The shape of the region. Usually this will be null (except for things like check box
     * checks, scroll bar down arrows / up arrows, etc). If this is not null, it determines
     * the shape of the region to draw. If it is null, then the assumed shape of the region is
     * one of a rounded rectangle. This shape is a com.sun.javafx.geom.Shape, and is not
     * touched by the FX scene graph except during synchronization, so it is safe to access
     * on the render thread.
     */
    private Shape shape;
    private NGShape ngShape;

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
     * Determined when a background is set on the region, this flag indicates whether this
     * background can be cached. As of this time, the only backgrounds which can be cached
     * are those where there are only solid fills or linear gradients.
     */
    private int cacheMode;

    /**
     * Is the key into the image cache that identifies the required background
     * for the region.
     */
    private Integer cacheKey;

    /**
     * Simple Helper Function for cleanup.
     */
    static Paint getPlatformPaint(javafx.scene.paint.Paint paint) {
        return (Paint)Toolkit.getPaintAccessor().getPlatformPaint(paint);
    }

    // We create a class instance of a no op. Effect internally to handle 3D
    // transform if user didn't use Effect for 3D Transformed Region. This will
    // automatically forces Region rendering path to use the Effect path.
    private static final Offset nopEffect = new Offset(0, 0, null);
    private EffectFilter nopEffectFilter;

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
        this.ngShape = shape == null ? null : NodeHelper.getPeer(((javafx.scene.shape.Shape)shape));
        this.shape = shape == null ? null : ngShape.getShape();
        this.scaleShape = scaleShape;
        this.centerShape = positionShape;
        this.cacheShape = cacheShape;
        // Technically I don't think this is needed because whenever the shape changes, setOpaqueInsets
        // is also called, so this will get invalidated twice.
        invalidateOpaqueRegion();
        cacheKey = null;
        visualsChanged();
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
        cacheKey = null;
        visualsChanged();
        // We only have to clear the background insets when the size changes if the
        // background has fills who's insets are dependent on the size (as would be
        // true only if a CornerRadii of any background fill on the background had
        // a percentage based radius).
        if (background != null && background.isFillPercentageBased()) {
            backgroundInsets = null;
        }
    }

    /**
     * Called by Region whenever an image that was being loaded in the background has
     * finished loading. Nothing changes in terms of metrics or sizes or caches, but
     * we do need to repaint everything.
     */
    public void imagesUpdated() {
        visualsChanged();
    }

    /**
     * Called by the Region when the Border is changed. The Region *must* only call
     * this method if the border object has actually changed, or excessive work may be done.
     *
     * @param b Border, of type javafx.scene.layout.Border
     */
    public void updateBorder(Border b) {
        // Make sure that the border instance we store on this NGRegion is never null
        final Border old = border;
        border = b == null ? Border.EMPTY : b;

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
     * Called by the Region when any parameters are changed.
     * It is only technically needed when a parameter that affects the size
     * of any percentage or overflowing corner radii is changed, but since
     * the data is not processed here in NGRegion, it is set on every update
     * of the peers for any reason.
     * A null value means that the raw radii in the BorderStroke objects
     * themselves were already absolute and non-overflowing.
     *
     * @param normalizedStrokeCorners a precomputed copy of the radii in the
     *        BorderStroke objects that are not percentages and do not overflow
     */
    public void updateStrokeCorners(List<CornerRadii> normalizedStrokeCorners) {
        this.normalizedStrokeCorners = normalizedStrokeCorners;
    }

    /**
     * Returns the normalized (non-percentage, non-overflowing) radii for the
     * selected index into the BorderStroke objects.
     * If a List was synchronized from the Region object, the value from that
     * List, otherwise the raw radii are fetched from the indicated BorderStroke
     * object.
     *
     * @param index the index of the BorderStroke object being processed
     * @return the normalized radii for the indicated BorderStroke object
     */
    private CornerRadii getNormalizedStrokeRadii(int index) {
        return (normalizedStrokeCorners == null
                ? border.getStrokes().get(index).getRadii()
                : normalizedStrokeCorners.get(index));
    }

    /**
     * Called by the Region when the Background has changed. The Region *must* only call
     * this method if the background object has actually changed, or excessive work may be done.
     *
     * @param b    Background, of type javafx.scene.layout.Background. Can be null.
     */
    public void updateBackground(Background b) {
        // NOTE: We don't explicitly invalidate the opaque region in this method, because the
        // Region will always call setOpaqueInsets whenever the background is changed, and
        // setOpaqueInsets always invalidates the opaque region. So we don't have to do it
        // again here. This wasn't immediately obvious and it might be better to combine
        // the updateBackground and setOpaqueInsets methods into one call, so that we
        // can more easily ensure that the opaque region is updated correctly.

        // Make sure that the background instance we store on this NGRegion is never null
        final Background old = background;
        background = b == null ? Background.EMPTY : b;

        final List<BackgroundFill> fills = background.getFills();
        cacheMode = 0;
        if (!PrismSettings.disableRegionCaching && !fills.isEmpty() && (shape == null || cacheShape)) {
            cacheMode = CACHE_SLICE_H | CACHE_SLICE_V;
            for (int i=0, max=fills.size(); i<max && cacheMode != 0; i++) {
                // We need to now inspect the paint to determine whether we can use a cache for this background.
                // If a shape is being used, we don't care about gradients (we cache 'em both), but for a rectangle
                // fill we omit these (so we can do 3-patch scaling). An ImagePattern is deadly to either
                // (well, only deadly to a shape if it turns out to be a writable image).
                final BackgroundFill fill = fills.get(i);
                javafx.scene.paint.Paint paint = fill.getFill();
                if (shape == null) {
                    if (paint instanceof LinearGradient) {
                        LinearGradient linear = (LinearGradient) paint;
                        if (linear.getStartX() != linear.getEndX()) {
                            cacheMode &= ~CACHE_SLICE_H;
                        }
                        if (linear.getStartY() != linear.getEndY()) {
                            cacheMode &= ~CACHE_SLICE_V;
                        }
                    } else if (!(paint instanceof Color)) {
                        //Either radial gradient or image pattern
                        cacheMode = 0;
                    }
                } else if (paint instanceof javafx.scene.paint.ImagePattern) {
                    cacheMode = 0;
                }
            }
        }
        backgroundInsets = null;
        cacheKey = null;

        // Only update the geom if the new background is geometrically different from the old
        if (!background.getOutsets().equals(old.getOutsets())) {
            geometryChanged();
        } else {
            visualsChanged();
        }
    }

    /**
     * Called by the Region when any parameters are changed.
     * It is only technically needed when a parameter that affects the size
     * of any percentage or overflowing corner radii is changed, but since
     * the data is not processed here in NGRegion, it is set on every update
     * of the peers for any reason.
     * A null value means that the raw radii in the BackgroundFill objects
     * themselves were already absolute and non-overflowing.
     *
     * @param normalizedStrokeCorners a precomputed copy of the radii in the
     *        BackgroundFill objects that are not percentages and do not overflow
     */
    public void updateFillCorners(List<CornerRadii> normalizedFillCorners) {
        this.normalizedFillCorners = normalizedFillCorners;
    }

    /**
     * Returns the normalized (non-percentage, non-overflowing) radii for the
     * selected index into the BackgroundFill objects.
     * If a List was synchronized from the Region object, the value from that
     * List, otherwise the raw radii are fetched from the indicated BackgroundFill
     * object.
     *
     * @param index the index of the BackgroundFill object being processed
     * @return the normalized radii for the indicated BackgroundFill object
     */
    private CornerRadii getNormalizedFillRadii(int index) {
        return (normalizedFillCorners == null
                ? background.getFills().get(index).getRadii()
                : normalizedFillCorners.get(index));
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
     * When cleaning the dirty flag, we also have to keep in mind
     * the NGShape used by the NGRegion
     */
    @Override public void clearDirty() {
        super.clearDirty();
        if (ngShape != null) {
            ngShape.clearDirty();
        }
    }

    /**************************************************************************
     *                                                                        *
     * Implementations of methods defined in the parent classes, with the     *
     * exception of rendering methods.                                        *
     *                                                                        *
     *************************************************************************/

    private RegionImageCache getImageCache(final Graphics g) {
        final Screen screen = g.getAssociatedScreen();
        RegionImageCache cache = imageCacheMap.get(screen);
        if (cache != null) {
            RTTexture tex = cache.getBackingStore();
            if (tex.isSurfaceLost()) {
                imageCacheMap.remove(screen);
                cache = null;
            }
        }
        if (cache == null) {
            cache = new RegionImageCache(g.getResourceFactory());
            imageCacheMap.put(screen, cache);
        }
        return cache;
    }

    private Integer getCacheKey(int w, int h) {
        if (cacheKey == null) {
            int key = 31 * w;
            key = key * 37 + h;
            key = key * 47 + background.hashCode();
            if (shape != null) {
                key = key * 73 + shape.hashCode();
            }
            cacheKey = key;
        }
        return cacheKey;
    }

    @Override protected boolean supportsOpaqueRegions() { return true; }

    @Override
    protected boolean hasOpaqueRegion() {
        return super.hasOpaqueRegion() &&
                !Float.isNaN(opaqueTop) && !Float.isNaN(opaqueRight) &&
                !Float.isNaN(opaqueBottom) && !Float.isNaN(opaqueLeft);
    }

    /**
     * The opaque region of an NGRegion takes into account the opaque insets
     * specified by the Region during synchronization. It also takes into
     * account the clip and the effect.
     *
     * @param opaqueRegion
     * @return
     */
    @Override protected RectBounds computeOpaqueRegion(RectBounds opaqueRegion) {
        // TODO what to do if the opaqueRegion has negative width or height due to excessive opaque insets? (RT-26979)
        return (RectBounds) opaqueRegion.deriveWithNewBounds(opaqueLeft, opaqueTop, 0, width - opaqueRight, height - opaqueBottom, 0);
    }

    @Override protected RenderRootResult computeRenderRoot(NodePath path, RectBounds dirtyRegion,
                                                           int cullingIndex, BaseTransform tx,
                                                           GeneralTransform3D pvTx) {

        RenderRootResult result = super.computeRenderRoot(path, dirtyRegion, cullingIndex, tx, pvTx);
        if (result == RenderRootResult.NO_RENDER_ROOT){
            result = computeNodeRenderRoot(path, dirtyRegion, cullingIndex, tx, pvTx);
        }
        return result;
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
        // Use Effect to render a 3D transformed Region that does not contain 3D
        // transformed children. This is done in order to render the Region's
        // content and children into an image in local coordinates using the identity
        // transform. The resulting image will then be correctly transformed in 3D by
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
                nopEffectFilter = new EffectFilter(nopEffect, this);
            }
            nopEffectFilter.render(g);

            return;
        }

        // If the shape is not null, then the shape will define what we need to draw for
        // this region. If the shape is null, then the "shape" of the region is just a
        // rectangle (or rounded rectangle, depending on the Background).
        if (shape != null) {
            renderAsShape(g);
        } else if (width > 0 && height > 0) {
            renderAsRectangle(g);
        }

        // Paint the children
        super.renderContent(g);
    }

    /**************************************************************************
     *                                                                        *
     * Drawing a region background and borders when the Region has been       *
     * specified to have a shape. This is typically used to render some       *
     * portions of a UI Control, such as the tick on a CheckBox, the dot on a *
     * RadioButton, or the disclosure node arrow on a TreeView. In these      *
     * cases, the overall region size is typically very small and can         *
     * therefore easily be cached.                                            *
     *                                                                        *
     *************************************************************************/

    private void renderAsShape(Graphics g) {
        if (!background.isEmpty()) {
            // Note: resizeShape is not cheap. This should be refactored so that we only invoke
            // it if we absolutely have to. Specifically, if the background, shape, and size of the region
            // has not changed since the last time we rendered we could skip all this and render
            // directly out of a cache.
            final Insets outsets = background.getOutsets();
            final Shape outsetShape = resizeShape((float) -outsets.getTop(), (float) -outsets.getRight(),
                                                  (float) -outsets.getBottom(), (float) -outsets.getLeft());
            final RectBounds outsetShapeBounds = outsetShape.getBounds();
            final int textureWidth = Math.round(outsetShapeBounds.getWidth()),
                      textureHeight = Math.round(outsetShapeBounds.getHeight());

            final int border = 1;
            // See if we have a cached representation for this region background already. In UI controls,
            // the arrow in a scroll bar button or the dot in a radio button or the tick in a check box are
            // all examples of cases where we'd like to reuse a cached image for performance reasons rather
            // than re-drawing everything each time.

            RTTexture cached = null;
            Rectangle rect = null;
            // RT-25013: We need to make sure that we do not use a cached image in the case of a
            // scaled region, or things won't look right (they'll looked scaled instead of vector-resized).
            if (cacheMode != 0 && g.getTransformNoClone().isTranslateOrIdentity() && !(g instanceof PrinterGraphics)) {
                final RegionImageCache imageCache = getImageCache(g);
                if (imageCache.isImageCachable(textureWidth, textureHeight)) {
                    final Integer key = getCacheKey(textureWidth, textureHeight);
                    rect = TEMP_RECT;
                    rect.setBounds(0, 0, textureWidth + border, textureHeight + border);
                    boolean render = imageCache.getImageLocation(key, rect, background, shape, g);
                    if (!rect.isEmpty()) {
                        // An empty rect indicates a failure occurred in the imageCache
                        cached = imageCache.getBackingStore();
                    }
                    if (cached != null && render) {
                        Graphics cachedGraphics = cached.createGraphics();

                        // Have to move the origin such that when rendering to x=0, we actually end up rendering
                        // at x=bounds.getMinX(). Otherwise anything rendered to the left of the origin would be lost
                        cachedGraphics.translate(rect.x - outsetShapeBounds.getMinX(),
                                                 rect.y - outsetShapeBounds.getMinY());
                        renderBackgroundShape(cachedGraphics);
                        if (PULSE_LOGGING_ENABLED) {
                            PulseLogger.incrementCounter("Rendering region shape image to cache");
                        }
                    }
                }
            }

            // "cached" might not be null if either there was a cached image, or we just created one.
            // In either case, we need to now render from the cached texture to the graphics
            if (cached != null) {
                // We just draw exactly what it was we have cached
                final float dstX1 = outsetShapeBounds.getMinX();
                final float dstY1 = outsetShapeBounds.getMinY();
                final float dstX2 = outsetShapeBounds.getMaxX();
                final float dstY2 = outsetShapeBounds.getMaxY();

                final float srcX1 = rect.x;
                final float srcY1 = rect.y;
                final float srcX2 = srcX1 + textureWidth;
                final float srcY2 = srcY1 + textureHeight;

                g.drawTexture(cached, dstX1, dstY1, dstX2, dstY2, srcX1, srcY1, srcX2, srcY2);
                if (PULSE_LOGGING_ENABLED) {
                    PulseLogger.incrementCounter("Cached region shape image used");
                }
            } else {
                // no cache, rendering backgrounds directly to graphics
                renderBackgroundShape(g);
            }
        }

        // Note that if you use borders, you're going to pay a premium in performance.
        // I don't think this is strictly necessary (since we won't stretch a cached
        // region shape anyway), so really this code should some how be combined
        // with the caching code that happened above for backgrounds.
        if (!border.isEmpty()) {
            // We only deal with stroke borders, we never deal with ImageBorders when
            // painting a shape on a Region. This is primarily because we don't know
            // how to handle a 9-patch image on a random shape. We'll have to implement
            // this at some point, but today is not that day.
            final List<BorderStroke> strokes = border.getStrokes();
            for (int i = 0, max = strokes.size(); i < max; i++) {
                // Get the BorderStroke. When stroking a shape, we only honor the
                // topStroke, topStyle, widths.top, and insets.
                final BorderStroke stroke = strokes.get(i);
                // We're stroking a path, so there is no point trying to figure out the length.
                // Instead, we just pass -1, telling setBorderStyle to just do a simple stroke
                setBorderStyle(g, stroke, -1, false);
                final Insets insets = stroke.getInsets();
                g.draw(resizeShape((float) insets.getTop(), (float) insets.getRight(),
                                   (float) insets.getBottom(), (float) insets.getLeft()));
            }
        }
    }

    private void renderBackgroundShape(Graphics g) {
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.incrementCounter("NGRegion renderBackgroundShape slow path");
            PulseLogger.addMessage("Slow shape path for " + getName());
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
            final Image prismImage = (Image) Toolkit.getImageAccessor().getPlatformImage(image.getImage());
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
    }

    /**************************************************************************
     *                                                                        *
     * Drawing a region background and borders when the Region has no defined *
     * shape, and is therefore treated as a rounded rectangle. This is the    *
     * most common code path for UI Controls.                                 *
     *                                                                        *
     *************************************************************************/

    private void renderAsRectangle(Graphics g) {
        if (!background.isEmpty()) {
            renderBackgroundRectangle(g);
        }

        if (!border.isEmpty()) {
            renderBorderRectangle(g);
        }
    }

    private void renderBackgroundRectangle(Graphics g) {
        // TODO a big chunk of this only makes sense to do if there actually are background fills,
        // and we should guard against that.

        // cacheWidth is the width of the region used within the cached image. For example,
        // perhaps normally the width of a region is 200px. But instead I will render the
        // region as though it is 20px wide into the cached image. 20px in this case is
        // the cache width. Although it may draw into more pixels than this (for example,
        // drawing the focus rectangle extends beyond the width of the region).
        // left + right background insets give us the left / right slice locations, plus 1 pixel for the center.
        // Round the whole thing up to be a whole number.
        if (backgroundInsets == null) updateBackgroundInsets();
        final double leftInset = backgroundInsets.getLeft() + 1;
        final double rightInset = backgroundInsets.getRight() + 1;
        final double topInset = backgroundInsets.getTop() + 1;
        final double bottomInset = backgroundInsets.getBottom() + 1;

        // If the insets are too large, then we want to use the width of the region instead of the
        // computed cacheWidth. RadioButton, for example, enters this case
        int cacheWidth = roundUp(width);
        if ((cacheMode & CACHE_SLICE_H) != 0) {
            cacheWidth = Math.min(cacheWidth, (int) (leftInset + rightInset));
        }
        int cacheHeight = roundUp(height);
        if ((cacheMode & CACHE_SLICE_V) != 0) {
            cacheHeight = Math.min(cacheHeight, (int) (topInset + bottomInset));
        }

        final Insets outsets = background.getOutsets();
        final int outsetsTop = roundUp(outsets.getTop());
        final int outsetsRight = roundUp(outsets.getRight());
        final int outsetsBottom = roundUp(outsets.getBottom());
        final int outsetsLeft = roundUp(outsets.getLeft());

        // The textureWidth / textureHeight is the width/height of the actual image. This needs to be rounded
        // up to the next whole pixel value.
        final int textureWidth = outsetsLeft + cacheWidth + outsetsRight;
        final int textureHeight = outsetsTop + cacheHeight + outsetsBottom;

        // See if we have a cached representation for this region background already.
        // RT-25013: We need to make sure that we do not use a cached image in the case of a
        // scaled region, or things won't look right (they'll looked scaled instead of vector-resized).
        // RT-25049: Need to only use the cache for pixel aligned regions or the result
        // will not look the same as though drawn by vector
        final boolean cache =
                background.getFills().size() > 1 && // Not worth the overhead otherwise
                cacheMode != 0 &&
                g.getTransformNoClone().isTranslateOrIdentity() &&
                !(g instanceof PrinterGraphics);
        final int border = 1;
        RTTexture cached = null;
        Rectangle rect = null;
        if (cache) {
            RegionImageCache imageCache = getImageCache(g);
            if (imageCache.isImageCachable(textureWidth, textureHeight)) {
                final Integer key = getCacheKey(textureWidth, textureHeight);
                rect = TEMP_RECT;
                rect.setBounds(0, 0, textureWidth + border, textureHeight + border);
                boolean render = imageCache.getImageLocation(key, rect, background, shape, g);
                if (!rect.isEmpty()) {
                    // An empty rect indicates a failure occurred in the imageCache
                    cached = imageCache.getBackingStore();
                }
                if (cached != null && render) {
                    Graphics cacheGraphics = cached.createGraphics();

                    // Have to move the origin such that when rendering to x=0, we actually end up rendering
                    // at x=outsets.getLeft(). Otherwise anything rendered to the left of the origin would be lost
                    // Round up to the nearest pixel
                    cacheGraphics.translate(rect.x + outsetsLeft, rect.y + outsetsTop);

                    // Rendering backgrounds to the cache
                    renderBackgroundRectanglesDirectly(cacheGraphics, cacheWidth, cacheHeight);

                    if (PULSE_LOGGING_ENABLED) {
                        PulseLogger.incrementCounter("Rendering region background image to cache");
                    }
                }
            }
        }

        // "cached" might not be null if either there was a cached image, or we just created one.
        // In either case, we need to now render from the cached texture to the graphics
        if (cached != null) {
            renderBackgroundRectangleFromCache(
                    g, cached, rect, textureWidth, textureHeight,
                    topInset, rightInset, bottomInset, leftInset,
                    outsetsTop, outsetsRight, outsetsBottom, outsetsLeft);
        } else {
            // no cache, rendering backgrounds directly to graphics
            renderBackgroundRectanglesDirectly(g, width, height);
        }

        final List<BackgroundImage> images = background.getImages();
        for (int i = 0, max = images.size(); i < max; i++) {
            final BackgroundImage image = images.get(i);
            Image prismImage = (Image) Toolkit.getImageAccessor().getPlatformImage(image.getImage());
            if (prismImage == null) {
                // The prismImage might be null if the Image has not completed loading.
                // In that case, we simply must skip rendering of that layer this
                // time around.
                continue;
            }

            final int imgUnscaledWidth = (int)image.getImage().getWidth();
            final int imgUnscaledHeight = (int)image.getImage().getHeight();
            final int imgWidth = prismImage.getWidth();
            final int imgHeight = prismImage.getHeight();
            // TODO need to write tests where we use a writable image and draw to it a lot. (RT-26978)
            if (imgWidth != 0 && imgHeight != 0) {
                final BackgroundSize size = image.getSize();
                if (size.isCover()) {
                    // When "cover" is true, we can ignore most properties on the BackgroundSize and
                    // BackgroundRepeat and BackgroundPosition. Because the image will be stretched to
                    // fill the entire space, there is no need to know the repeat or position or
                    // size width / height.
                    final float scale = Math.max(width / imgWidth,height / imgHeight);
                    final Texture texture =
                        g.getResourceFactory().getCachedTexture(prismImage, Texture.WrapMode.CLAMP_TO_EDGE);
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
                    paintTiles(g, prismImage, image.getRepeatX(), image.getRepeatY(),
                               pos.getHorizontalSide(), pos.getVerticalSide(),
                               0, 0, width, height, // the region area to fill with the image
                               0, 0, imgWidth, imgHeight, // The entire image is used
                               (float) tileX, (float) tileY, (float) tileWidth, (float) tileHeight);
                }
            }
        }
    }

    private void renderBackgroundRectangleFromCache(
            Graphics g, RTTexture cached, Rectangle rect, int textureWidth, int textureHeight,
            double topInset, double rightInset, double bottomInset, double leftInset,
            int outsetsTop, int outsetsRight, int outsetsBottom, int outsetsLeft) {

        // All cache operations are padded by (just shy of) half a pixel so
        // that as we are translated by sub-pixel amounts we continue to sample
        // all of the cached pixels out until they become transparent at (or
        // 1-bit worth of non-zero alhpa from) the center of the border pixel
        // around the cache.  If there is an integer translation, then our
        // padding should come up just shy of including new rows/columns of
        // pixels in the rendering and thus have no impact on pixel fill rates.
        final float pad = 0.5f - 1f/256f;
        final float dstWidth = outsetsLeft + width + outsetsRight;
        final float dstHeight = outsetsTop + height + outsetsBottom;
        final boolean sameWidth = textureWidth == dstWidth;
        final boolean sameHeight = textureHeight == dstHeight;
        final float dstX1 = -outsetsLeft - pad;
        final float dstY1 = -outsetsTop - pad;
        final float dstX2 = width + outsetsRight + pad;
        final float dstY2 = height + outsetsBottom + pad;
        final float srcX1 = rect.x - pad;
        final float srcY1 = rect.y - pad;
        final float srcX2 = rect.x + textureWidth + pad;
        final float srcY2 = rect.y + textureHeight + pad;

        // If total destination width is < the source width, then we need to start
        // shrinking the left and right sides to accommodate. Likewise in the other dimension.
        double adjustedLeftInset = leftInset;
        double adjustedRightInset = rightInset;
        double adjustedTopInset = topInset;
        double adjustedBottomInset = bottomInset;
        if (leftInset + rightInset > width) {
            double fraction = width / (leftInset + rightInset);
            adjustedLeftInset *= fraction;
            adjustedRightInset *= fraction;
        }
        if (topInset + bottomInset > height) {
            double fraction = height / (topInset + bottomInset);
            adjustedTopInset *= fraction;
            adjustedBottomInset *= fraction;
        }

        if (sameWidth && sameHeight) {
            g.drawTexture(cached, dstX1, dstY1, dstX2, dstY2, srcX1, srcY1, srcX2, srcY2);
        } else if (sameHeight) {
            // We do 3-patch rendering fixed height
            final float left  = pad + (float) (adjustedLeftInset  + outsetsLeft);
            final float right = pad + (float) (adjustedRightInset + outsetsRight);

            final float dstLeftX = dstX1 + left;
            final float dstRightX = dstX2 - right;
            final float srcLeftX = srcX1 + left;
            final float srcRightX = srcX2 - right;

            g.drawTexture3SliceH(cached,
                                 dstX1, dstY1, dstX2, dstY2,
                                 srcX1, srcY1, srcX2, srcY2,
                                 dstLeftX, dstRightX, srcLeftX, srcRightX);
        } else if (sameWidth) {
            // We do 3-patch rendering fixed width
            final float top    = pad + (float) (adjustedTopInset    + outsetsTop);
            final float bottom = pad + (float) (adjustedBottomInset + outsetsBottom);

            final float dstTopY = dstY1 + top;
            final float dstBottomY = dstY2 - bottom;
            final float srcTopY = srcY1 + top;
            final float srcBottomY = srcY2 - bottom;

            g.drawTexture3SliceV(cached,
                                 dstX1, dstY1, dstX2, dstY2,
                                 srcX1, srcY1, srcX2, srcY2,
                                 dstTopY, dstBottomY, srcTopY, srcBottomY);
        } else {
            // We do 9-patch rendering
            final float left   = pad + (float) (adjustedLeftInset   + outsetsLeft);
            final float top    = pad + (float) (adjustedTopInset    + outsetsTop);
            final float right  = pad + (float) (adjustedRightInset  + outsetsRight);
            final float bottom = pad + (float) (adjustedBottomInset + outsetsBottom);

            final float dstLeftX = dstX1 + left;
            final float dstRightX = dstX2 - right;
            final float srcLeftX = srcX1 + left;
            final float srcRightX = srcX2 - right;
            final float dstTopY = dstY1 + top;
            final float dstBottomY = dstY2 - bottom;
            final float srcTopY = srcY1 + top;
            final float srcBottomY = srcY2 - bottom;

            g.drawTexture9Slice(cached,
                                dstX1, dstY1, dstX2, dstY2,
                                srcX1, srcY1, srcX2, srcY2,
                                dstLeftX, dstTopY, dstRightX, dstBottomY,
                                srcLeftX, srcTopY, srcRightX, srcBottomY);
        }

        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.incrementCounter("Cached region background image used");
        }
    }

    private void renderBackgroundRectanglesDirectly(Graphics g, float width, float height) {
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
                final CornerRadii radii = getNormalizedFillRadii(i);
                // This is a workaround for RT-28435 so we use path rasterizer for small radius's We are
                // keeping old rendering. We do not apply workaround when using Caspian or Embedded
                if (radii.isUniform() &&
                        !(!PlatformImpl.isCaspian() && !(PlatformUtil.isEmbedded() || PlatformUtil.isIOS()) && radii.getTopLeftHorizontalRadius() > 0 && radii.getTopLeftHorizontalRadius() <= 4)) {
                    // If the radii is uniform then we know every corner matches, so we can do some
                    // faster rendering paths.
                    float tlhr = (float) radii.getTopLeftHorizontalRadius();
                    float tlvr = (float) radii.getTopLeftVerticalRadius();
                    if (tlhr == 0 && tlvr == 0) {
                        // The edges are square, so we can do a simple fill rect
                        g.fillRect(l, t, w, h);
                    } else {
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
                    if (PULSE_LOGGING_ENABLED) {
                        PulseLogger.incrementCounter("NGRegion renderBackgrounds slow path");
                        PulseLogger.addMessage("Slow background path for " + getName());
                    }
                    // The edges are not uniform, so we have to render each edge independently
                    // TODO document the issue number which will give us a fast path for rendering
                    // non-uniform corners, and that we want to implement that instead of createPath2
                    // below in such cases. (RT-26979)
                    g.fill(createPath(width, height, t, l, b, r, radii));
                }
            }
        }
    }

    private void renderBorderRectangle(Graphics g) {
        final List<BorderImage> images = border.getImages();
        final List<BorderStroke> strokes = images.isEmpty() ? border.getStrokes() : Collections.emptyList();
        for (int i = 0, max = strokes.size(); i < max; i++) {
            final BorderStroke stroke = strokes.get(i);
            final BorderWidths widths = stroke.getWidths();
            final CornerRadii radii = getNormalizedStrokeRadii(i);
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

            // The Prism Graphics logic can stroke lines only CENTERED and doesn't know what to do with
            // INSIDE or OUTSIDE strokes for lines. The only way to deal with those is
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
                if (!(topStroke instanceof Color && ((Color)topStroke).getOpacity() == 0f) && topStyle != BorderStrokeStyle.NONE) {
                    float w = width - l - r;
                    float h = height - t - b;
                    // The length of each side of the path we're going to stroke
                    final double di = 2 * radii.getTopLeftHorizontalRadius();
                    final double circle = di*Math.PI;
                    final double totalLineLength =
                            circle +
                            2 * (w - di) +
                            2 * (h - di);

                    if (w >= 0 && h >= 0) {
                        setBorderStyle(g, stroke, totalLineLength, true);
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
                            g.draw(createPath(width, height, t, l, b, r, radii));
                        }
                    }
                }
            } else if (radii.isUniform() && radius == 0) {

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
                        g.setStroke(createStroke(topStyle, topWidth, width, true));
                        g.drawLine(l, t, width - r, t);
                    }
                }

                if (!(rightStroke instanceof Color && ((Color)rightStroke).getOpacity() == 0f) && rightStyle != BorderStrokeStyle.NONE) {
                    g.setPaint(getPlatformPaint(rightStroke));
                    if (BorderStrokeStyle.SOLID == rightStyle) {
                        g.fillRect(width - rightInset - rightWidth, topInset,
                                   rightWidth, height - topInset - bottomInset);
                    } else {
                        g.setStroke(createStroke(rightStyle, rightWidth, height, true));
                        g.drawLine(width - r, t, width - r, height - b);
                    }
                }

                if (!(bottomStroke instanceof Color && ((Color)bottomStroke).getOpacity() == 0f) && bottomStyle != BorderStrokeStyle.NONE) {
                    g.setPaint(getPlatformPaint(bottomStroke));
                    if (BorderStrokeStyle.SOLID == bottomStyle) {
                        g.fillRect(leftInset, height - bottomInset - bottomWidth,
                                width - leftInset - rightInset, bottomWidth);
                    } else {
                        g.setStroke(createStroke(bottomStyle, bottomWidth, width, true));
                        g.drawLine(l, height - b, width - r, height - b);
                    }
                }

                if (!(leftStroke instanceof Color && ((Color)leftStroke).getOpacity() == 0f) && leftStyle != BorderStrokeStyle.NONE) {
                    g.setPaint(getPlatformPaint(leftStroke));
                    if (BorderStrokeStyle.SOLID == leftStyle) {
                        g.fillRect(leftInset, topInset, leftWidth, height - topInset - bottomInset);
                    } else {
                        g.setStroke(createStroke(leftStyle, leftWidth, height, true));
                        g.drawLine(l, t, l, height - b);
                    }
                }
            } else {
                // In this case, we have different styles and/or strokes and/or widths on one or
                // more sides, and either the radii are not uniform, or they are uniform but greater
                // than 0. In this case we have to take a much slower rendering path by turning this
                // stroke into a path (or in the current implementation, an array of paths).
                Shape[] paths = createPaths(t, l, b, r, radii);
                if (topStyle != BorderStrokeStyle.NONE) {
                    double rsum = radii.getTopLeftHorizontalRadius() + radii.getTopRightHorizontalRadius();
                    double topLineLength = width + rsum * (Math.PI / 4 - 1);
                    g.setStroke(createStroke(topStyle, topWidth, topLineLength, true));
                    g.setPaint(getPlatformPaint(topStroke));
                    g.draw(paths[0]);
                }
                if (rightStyle != BorderStrokeStyle.NONE) {
                    double rsum = radii.getTopRightVerticalRadius() + radii.getBottomRightVerticalRadius();
                    double rightLineLength = height + rsum * (Math.PI / 4 - 1);
                    g.setStroke(createStroke(rightStyle, rightWidth, rightLineLength, true));
                    g.setPaint(getPlatformPaint(rightStroke));
                    g.draw(paths[1]);
                }
                if (bottomStyle != BorderStrokeStyle.NONE) {
                    double rsum = radii.getBottomLeftHorizontalRadius() + radii.getBottomRightHorizontalRadius();
                    double bottomLineLength = width + rsum * (Math.PI / 4 - 1);
                    g.setStroke(createStroke(bottomStyle, bottomWidth, bottomLineLength, true));
                    g.setPaint(getPlatformPaint(bottomStroke));
                    g.draw(paths[2]);
                }
                if (leftStyle != BorderStrokeStyle.NONE) {
                    double rsum = radii.getTopLeftVerticalRadius() + radii.getBottomLeftVerticalRadius();
                    double leftLineLength = height + rsum * (Math.PI / 4 - 1);
                    g.setStroke(createStroke(leftStyle, leftWidth, leftLineLength, true));
                    g.setPaint(getPlatformPaint(leftStroke));
                    g.draw(paths[3]);
                }
            }
        }

        for (int i = 0, max = images.size(); i < max; i++) {
            final BorderImage ib = images.get(i);
            final Image prismImage = (Image) Toolkit.getImageAccessor().getPlatformImage(ib.getImage());
            if (prismImage == null) {
                // The prismImage might be null if the Image has not completed loading.
                // In that case, we simply must skip rendering of that layer this
                // time around.
                continue;
            }
            final int imgWidth = prismImage.getWidth();
            final int imgHeight = prismImage.getHeight();
            final float imgScale = prismImage.getPixelScale();
            final BorderWidths widths = ib.getWidths();
            final Insets insets = ib.getInsets();
            final BorderWidths slices = ib.getSlices();

            // we will get gaps if we don't round to pixel boundaries
            final int topInset = (int) Math.round(insets.getTop());
            final int rightInset = (int) Math.round(insets.getRight());
            final int bottomInset = (int) Math.round(insets.getBottom());
            final int leftInset = (int) Math.round(insets.getLeft());

            final int topWidth = widthSize(widths.isTopAsPercentage(), widths.getTop(), height);
            final int rightWidth = widthSize(widths.isRightAsPercentage(), widths.getRight(), width);
            final int bottomWidth = widthSize(widths.isBottomAsPercentage(), widths.getBottom(), height);
            final int leftWidth = widthSize(widths.isLeftAsPercentage(), widths.getLeft(), width);

            final int topSlice = sliceSize(slices.isTopAsPercentage(), slices.getTop(), imgHeight, imgScale);
            final int rightSlice = sliceSize(slices.isRightAsPercentage(), slices.getRight(), imgWidth, imgScale);
            final int bottomSlice = sliceSize(slices.isBottomAsPercentage(), slices.getBottom(), imgHeight, imgScale);
            final int leftSlice = sliceSize(slices.isLeftAsPercentage(), slices.getLeft(), imgWidth, imgScale);

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
            paintTiles(g, prismImage, BorderRepeat.STRETCH, BorderRepeat.STRETCH, Side.LEFT, Side.TOP,
                       leftInset, topInset, leftWidth, topWidth, // target bounds
                       0, 0, leftSlice, topSlice, // src image bounds
                       0, 0, leftWidth, topWidth); // tile bounds
            // paint top slice
            float tileWidth = (ib.getRepeatX() == BorderRepeat.STRETCH) ?
                    centerW : (topSlice > 0 ? (centerSliceWidth * topWidth) / topSlice : 0);
            float tileHeight = topWidth;
            paintTiles(
                    g, prismImage, ib.getRepeatX(), BorderRepeat.STRETCH, Side.LEFT, Side.TOP,
                    centerMinX, topInset, centerW, topWidth,
                    leftSlice, 0, centerSliceWidth, topSlice,
                    (centerW - tileWidth) / 2, 0, tileWidth, tileHeight);
            // paint top right corner
            paintTiles(g, prismImage, BorderRepeat.STRETCH, BorderRepeat.STRETCH, Side.LEFT, Side.TOP,
                       centerMaxX, topInset, rightWidth, topWidth,
                       (imgWidth - rightSlice), 0, rightSlice, topSlice,
                       0, 0, rightWidth, topWidth);
            // paint left slice
            tileWidth = leftWidth;
            tileHeight = (ib.getRepeatY() == BorderRepeat.STRETCH) ?
                    centerH : (leftSlice > 0 ? (leftWidth * centerSliceHeight) / leftSlice : 0);
            paintTiles(g, prismImage, BorderRepeat.STRETCH, ib.getRepeatY(), Side.LEFT, Side.TOP,
                       leftInset, centerMinY, leftWidth, centerH,
                       0, topSlice, leftSlice, centerSliceHeight,
                       0, (centerH - tileHeight) / 2, tileWidth, tileHeight);
            // paint right slice
            tileWidth = rightWidth;
            tileHeight = (ib.getRepeatY() == BorderRepeat.STRETCH) ?
                    centerH : (rightSlice > 0 ? (rightWidth * centerSliceHeight) / rightSlice : 0);
            paintTiles(g, prismImage, BorderRepeat.STRETCH, ib.getRepeatY(), Side.LEFT, Side.TOP,
                       centerMaxX, centerMinY, rightWidth, centerH,
                       imgWidth - rightSlice, topSlice, rightSlice, centerSliceHeight,
                       0, (centerH - tileHeight) / 2, tileWidth, tileHeight);
            // paint bottom left corner
            paintTiles(g, prismImage, BorderRepeat.STRETCH, BorderRepeat.STRETCH, Side.LEFT, Side.TOP,
                       leftInset, centerMaxY, leftWidth, bottomWidth,
                       0, imgHeight - bottomSlice, leftSlice, bottomSlice,
                       0, 0, leftWidth, bottomWidth);
            // paint bottom slice
            tileWidth = (ib.getRepeatX() == BorderRepeat.STRETCH) ?
                    centerW : (bottomSlice > 0 ? (centerSliceWidth * bottomWidth) / bottomSlice : 0);
            tileHeight = bottomWidth;
            paintTiles(g, prismImage, ib.getRepeatX(), BorderRepeat.STRETCH, Side.LEFT, Side.TOP,
                       centerMinX, centerMaxY, centerW, bottomWidth,
                       leftSlice, imgHeight - bottomSlice, centerSliceWidth, bottomSlice,
                       (centerW - tileWidth) / 2, 0, tileWidth, tileHeight);
            // paint bottom right corner
            paintTiles(g, prismImage, BorderRepeat.STRETCH, BorderRepeat.STRETCH, Side.LEFT, Side.TOP,
                       centerMaxX, centerMaxY, rightWidth, bottomWidth,
                       imgWidth - rightSlice, imgHeight - bottomSlice, rightSlice, bottomSlice,
                       0, 0, rightWidth, bottomWidth);
            // paint the center slice
            if (ib.isFilled()) {
                // handle no repeat as stretch
                final float imgW = (ib.getRepeatX() == BorderRepeat.STRETCH) ? centerW : centerSliceWidth;
                final float imgH = (ib.getRepeatY() == BorderRepeat.STRETCH) ? centerH : centerSliceHeight;
                paintTiles(g, prismImage, ib.getRepeatX(), ib.getRepeatY(), Side.LEFT, Side.TOP,
                           centerMinX, centerMinY, centerW, centerH,
                           leftSlice, topSlice, centerSliceWidth, centerSliceHeight,
                           0, 0, imgW, imgH);
            }
        }
    }

    /**
     * Visits each of the background fills and takes their radii into account to determine the insets.
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
            final CornerRadii radii = getNormalizedFillRadii(i);
            top = (float) Math.max(top, insets.getTop() + Math.max(radii.getTopLeftVerticalRadius(), radii.getTopRightVerticalRadius()));
            right = (float) Math.max(right, insets.getRight() + Math.max(radii.getTopRightHorizontalRadius(), radii.getBottomRightHorizontalRadius()));
            bottom = (float) Math.max(bottom, insets.getBottom() + Math.max(radii.getBottomRightVerticalRadius(), radii.getBottomLeftVerticalRadius()));
            left = (float) Math.max(left, insets.getLeft() + Math.max(radii.getTopLeftHorizontalRadius(), radii.getBottomLeftHorizontalRadius()));
        }
        backgroundInsets = new Insets(roundUp(top), roundUp(right), roundUp(bottom), roundUp(left));
    }

    private int widthSize(boolean isPercent, double sliceSize, float objSize) {
        //Not strictly correct. See RT-34051
        return (int) Math.round(isPercent ? sliceSize * objSize : sliceSize);
    }

    private int sliceSize(boolean isPercent, double sliceSize, float objSize, float scale) {
        if (isPercent) sliceSize *= objSize;
        if (sliceSize > objSize) sliceSize = objSize;
        return (int) Math.round(sliceSize * scale);
    }

    private int roundUp(double d) {
        return (d - (int)d) == 0 ? (int) d : (int) (d + 1);
    }


    /**
     * Creates a Prism BasicStroke based on the stroke style, width, and line length.
     *
     * @param sb             The BorderStrokeStyle
     * @param strokeWidth    The width of the stroke we're going to draw
     * @param lineLength     The total linear length of this stroke. This is needed for
     *                       handling "dashed" and "dotted" cases, otherwise, it is ignored.
     * @param forceCentered  When this is set to true, the stroke is always centered.
     *                       The "outer/inner" stroking has to be done by moving the line
     * @return A prism BasicStroke
     */
    private BasicStroke createStroke(BorderStrokeStyle sb,
                                     double strokeWidth,
                                     double lineLength,
                                     boolean forceCentered) {
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

        int type;
        if (forceCentered) {
            type = BasicStroke.TYPE_CENTERED;
        } else if (scaleShape) {
            // Note: this is just a workaround that allows us to avoid shape bounds computation with the given stroke.
            // By using inner stroke, we know the shape bounds and the shape will be scaled correctly, but the size of
            // the stroke after the scale will be slightly different, but this should be visible only with big stroke widths
            // See https://javafx-jira.kenai.com/browse/RT-38384
            type = BasicStroke.TYPE_INNER;
        } else {
            switch (sb.getType()) {
                case INSIDE:
                    type = BasicStroke.TYPE_INNER;
                    break;
                case OUTSIDE:
                    type = BasicStroke.TYPE_OUTER;
                    break;
                case CENTERED:
                default:
                    type = BasicStroke.TYPE_CENTERED;
                    break;
            }
        }

        BasicStroke bs;
        if (sb == BorderStrokeStyle.NONE) {
            throw new AssertionError("Should never have been asked to draw a border with NONE");
        } else if (strokeWidth <= 0) {
            // The stroke essentially disappears in this case, but some of the
            // dashing calculations below can produce degenerate dash arrays
            // that are problematic when the strokeWidth is 0.

            // Ideally the calling code would not even be trying to perform a
            // stroke under these conditions, but there are so many unchecked
            // calls to createStroke() in the code that pass the result directly
            // to a Graphics and then use it, that we need to return something
            // valid, even if it represents a NOP.

            bs = new BasicStroke((float) strokeWidth, cap, join,
                    (float) sb.getMiterLimit());
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
                    // 1.4*strokewidth as possible, but we want the spacing to be such that we get an even spacing between
                    // all dashes around the edge. Maybe we can start with the dash phase at half the dash length.
                    final double dashLength = strokeWidth * 2;
                    double gapLength = strokeWidth * 1.4;
                    final double segmentLength = dashLength + gapLength;
                    final double divided = lineLength / segmentLength;
                    final double numSegments = (int) divided;
                    if (numSegments > 0) {
                        final double dashCumulative = numSegments * dashLength;
                        gapLength = (lineLength - dashCumulative) / numSegments;
                    }
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

            bs = new BasicStroke(type, (float) strokeWidth, cap, join,
                    (float) sb.getMiterLimit(),
                    array, dashOffset);
        } else {
            bs = new BasicStroke(type, (float) strokeWidth, cap, join,
                    (float) sb.getMiterLimit());
        }

        return bs;
    }

    private void setBorderStyle(Graphics g, BorderStroke sb, double length, boolean forceCentered) {
        // Any one of, or all of, the sides could be 'none'.
        // Take the first side that isn't.
        final BorderWidths widths = sb.getWidths();
        BorderStrokeStyle bs = sb.getTopStyle();
        double sbWidth = widths.isTopAsPercentage() ? height * widths.getTop() : widths.getTop();
        Paint sbFill = getPlatformPaint(sb.getTopStroke());
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
                    sbFill = getPlatformPaint(sb.getRightStroke());
                }
            }
        }
        if (bs == null || bs == BorderStrokeStyle.NONE) {
            return;
        }

        g.setStroke(createStroke(bs, sbWidth, length, forceCentered));
        g.setPaint(sbFill);
    }

    /**
     * Inserts geometry into the specified Path2D object for the specified
     * corner of a general rounded rectangle.
     *
     * The corner drawn is specified by the quadrant parameter, whose least
     * significant 2 bits specify the following corners and the associated
     * start, corner, and end points (which are always drawn clockwise):
     *
     * 0 - Top Left:      X + 0 , Y + VR,      X, Y,      X + HR, Y + 0
     * 1 - Top Right:     X - HR, Y + 0 ,      X, Y,      X + 0 , Y + VR
     * 2 - Bottom Right:  X + 0 , Y - VR,      X, Y,      X - HR, Y + 0
     * 3 - Bottom Left:   X + HR, Y + 0 ,      X, Y,      X + 0 , Y - VR
     *
     * The associated horizontal and vertical radii are fetched from the
     * indicated CornerRadii object which is assumed to be absolute (not
     * percentage based) and already scaled so that no pair of radii are
     * larger than the indicated width/height of the rounded rectangle being
     * expressed.
     *
     * The tstart and tend parameters specify what portion of the rounded
     * corner should be drawn with 0f => 1f being the entire rounded corner.
     *
     * The newPath parameter indicates whether the path should reach the
     * starting point with a moveTo() command or a lineTo() segment.
     *
     * @param path
     * @param radii
     * @param x
     * @param y
     * @param quadrant
     * @param tstart
     * @param tend
     * @param newPath
     */
    private void doCorner(Path2D path, CornerRadii radii,
                          float x, float y, int quadrant,
                          float tstart, float tend, boolean newPath)
    {
        float dx0, dy0, dx1, dy1;
        float hr, vr;
        switch (quadrant & 0x3) {
            case 0:
                hr = (float) radii.getTopLeftHorizontalRadius();
                vr = (float) radii.getTopLeftVerticalRadius();
                // 0 - Top Left:      X + 0 , Y + VR,      X, Y,      X + HR, Y + 0
                dx0 =  0f;  dy0 =  vr;    dx1 =  hr;  dy1 =  0f;
                break;
            case 1:
                hr = (float) radii.getTopRightHorizontalRadius();
                vr = (float) radii.getTopRightVerticalRadius();
                // 1 - Top Right:     X - HR, Y + 0 ,      X, Y,      X + 0 , Y + VR
                dx0 = -hr;  dy0 =  0f;    dx1 =  0f;  dy1 =  vr;
                break;
            case 2:
                hr = (float) radii.getBottomRightHorizontalRadius();
                vr = (float) radii.getBottomRightVerticalRadius();
                // 2 - Bottom Right:  X + 0 , Y - VR,      X, Y,      X - HR, Y + 0
                dx0 =  0f;  dy0 = -vr;    dx1 = -hr;  dy1 = 0f;
                break;
            case 3:
                hr = (float) radii.getBottomLeftHorizontalRadius();
                vr = (float) radii.getBottomLeftVerticalRadius();
                // 3 - Bottom Left:   X + HR, Y + 0 ,      X, Y,      X + 0 , Y - VR
                dx0 =  hr;  dy0 =  0f;    dx1 =  0f;  dy1 = -vr;
                break;
            default: return; // Can never happen
        }
        if (hr > 0 && vr > 0) {
            path.appendOvalQuadrant(x + dx0, y + dy0, x, y, x + dx1, y + dy1, tstart, tend,
                                    (newPath)
                                        ? Path2D.CornerPrefix.MOVE_THEN_CORNER
                                        : Path2D.CornerPrefix.LINE_THEN_CORNER);
        } else if (newPath) {
            path.moveTo(x, y);
        } else {
            path.lineTo(x, y);
        }
    }

    /**
     * Creates a rounded rectangle path with our width and height, different corner radii,
     * offset with given offsets
     */
    private Path2D createPath(float width, float height, float t, float l, float bo, float ro, CornerRadii radii) {
        float r = width - ro;
        float b = height - bo;
        Path2D path = new Path2D();
        doCorner(path, radii, l, t, 0, 0f, 1f, true);
        doCorner(path, radii, r, t, 1, 0f, 1f, false);
        doCorner(path, radii, r, b, 2, 0f, 1f, false);
        doCorner(path, radii, l, b, 3, 0f, 1f, false);
        path.closePath();
        return path;
    }

    private Path2D makeRoundedEdge(CornerRadii radii,
                                   float x0, float y0, float x1, float y1,
                                   int quadrant)
    {
        Path2D path = new Path2D();
        doCorner(path, radii, x0, y0, quadrant,   0.5f, 1.0f, true);
        doCorner(path, radii, x1, y1, quadrant+1, 0.0f, 0.5f, false);
        return path;
    }

    /**
     * Creates a rounded rectangle path with our width and height, different corner radii, offset with given offsets.
     * Each side as a separate path.  The sides are returned in the CSS standard
     * order of top, right, bottom, left.
     */
    private Path2D[] createPaths(float t, float l, float bo, float ro, CornerRadii radii)
    {
        float r = width - ro;
        float b = height - bo;
        return new Path2D[] {
            makeRoundedEdge(radii, l, t, r, t, 0), // top
            makeRoundedEdge(radii, r, t, r, b, 1), // right
            makeRoundedEdge(radii, r, b, l, b, 2), // bottom
            makeRoundedEdge(radii, l, b, l, t, 3), // left
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
        if (regionWidth <= 0 || regionHeight <= 0 || srcW <= 0 || srcH <= 0) return;

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
//                        System.out.println("g.drawTexture(texture, " + dx1 + ", " + dy1 + ", " + dx2 + ", " + dy2 + ", " + sx1 + ", " + sy1 + ", " + sx2 + ", " + sy2 + ")");
                        g.drawTexture(texture, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2);
                    }
                    dstX += xIncrement;
                }
                dstY += yIncrement;
            }
            texture.unlock();
        }
    }

    final Border getBorder() {
        return border;
    }

    final Background getBackground() {
        return background;
    }

    final float getWidth() {
        return width;
    }

    final float getHeight() {
        return height;
    }

}
