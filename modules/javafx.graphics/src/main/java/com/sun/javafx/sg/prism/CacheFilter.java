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

import com.sun.javafx.logging.PulseLogger;
import javafx.scene.CacheHint;
import java.util.List;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.prism.Graphics;
import com.sun.prism.RTTexture;
import com.sun.prism.Texture;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.Filterable;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.prism.PrDrawable;
import com.sun.scenario.effect.impl.prism.PrFilterContext;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;

/**
 * Base implementation of the Node.cache and cacheHint APIs.
 *
 * When all or a portion of the cacheHint becomes enabled, we should try *not*
 * to re-render the cache.  This avoids a big hiccup at the beginning of the
 * "use SPEED only while animating" use case:
 *   0) Under DEFAULT, we should already have a cached image
 *   1) scale/rotate caching is enabled (no expensive re-render required)
 *   2) animation happens, using the cached image
 *   3) animation completes, caching is disable and the node is re-rendered (at
 *      full-fidelity) with the final transform.
 *
 * Certain transform combinations are not supported, notably scaling by unequal
 * amounts in the x and y directions while also rotating.  Other than simple
 * translation, animations in this case will require re-rendering every frame.
 *
 * Ideally, a simple change to a Node's translation should never regenerate the
 * cached image.
 *
 * The CacheFilter is also capable of optimizing the scrolling of the cached contents.
 * For example, the ScrollView UI Control can define its content area as being cached,
 * such that when the user scrolls, we can shift the old content area and adjust the
 * dirty region so that it only includes the "newly exposed" area.
 */
public class CacheFilter {
    /**
     * Defines the state when we're in the midst of scrolling a cached image
     */
    private static enum ScrollCacheState {
        CHECKING_PRECONDITIONS,
        ENABLED,
        DISABLED
    }

    // Garbage-reduction variables:
    private static final Rectangle TEMP_RECT = new Rectangle();
    private static final DirtyRegionContainer TEMP_CONTAINER = new DirtyRegionContainer(1);
    private static final Affine3D TEMP_CACHEFILTER_TRANSFORM = new Affine3D();
    private static final RectBounds TEMP_BOUNDS = new RectBounds();
    // Fun with floating point
    private static final double EPSILON = 0.0000001;

    private RTTexture tempTexture;
    private double lastXDelta;
    private double lastYDelta;
    private ScrollCacheState scrollCacheState = ScrollCacheState.CHECKING_PRECONDITIONS;
    // Note: this ImageData is always created and assumed to be untransformed.
    private ImageData cachedImageData;
    private Rectangle cacheBounds = new Rectangle();
    // Used to draw into the cache
    private final Affine2D cachedXform = new Affine2D();

    // The scale and rotate used to draw into the cache
    private double cachedScaleX;
    private double cachedScaleY;
    private double cachedRotate;

    private double cachedX;
    private double cachedY;
    private NGNode node;

    // Used to draw the cached image to the screen
    private final Affine2D screenXform = new Affine2D();

    // Cache hint settings
    private boolean scaleHint;
    private boolean rotateHint;
    // We keep this around for the sake of matchesHint
    private CacheHint cacheHint;

    // Was the last paint unsupported by the cache?  If so, will need to
    // regenerate the cache next time.
    private boolean wasUnsupported = false;

    /**
     * Compute the dirty region that must be re-rendered after scrolling
     */
    private Rectangle computeDirtyRegionForTranslate() {
        if (lastXDelta != 0) {
            if (lastXDelta > 0) {
                TEMP_RECT.setBounds(0, 0, (int)lastXDelta, cacheBounds.height);
            } else {
                TEMP_RECT.setBounds(cacheBounds.width + (int)lastXDelta, 0, -(int)lastXDelta, cacheBounds.height);
            }
        } else {
            if (lastYDelta > 0) {
                TEMP_RECT.setBounds(0, 0, cacheBounds.width, (int)lastYDelta);
            } else {
                TEMP_RECT.setBounds(0, cacheBounds.height + (int)lastYDelta, cacheBounds.width, -(int)lastYDelta);
            }
        }
        return TEMP_RECT;
    }

    protected CacheFilter(NGNode node, CacheHint cacheHint) {
        this.node = node;
        this.scrollCacheState = ScrollCacheState.CHECKING_PRECONDITIONS;
        setHint(cacheHint);
    }

    public void setHint(CacheHint cacheHint) {
        this.cacheHint = cacheHint;
        this.scaleHint = (cacheHint == CacheHint.SPEED ||
                          cacheHint == CacheHint.SCALE ||
                          cacheHint == CacheHint.SCALE_AND_ROTATE);
        this.rotateHint = (cacheHint == CacheHint.SPEED ||
                           cacheHint == CacheHint.ROTATE ||
                           cacheHint == CacheHint.SCALE_AND_ROTATE);
    }

    // These two methods exist only for the sake of testing.
    final boolean isScaleHint() { return scaleHint; }
    final boolean isRotateHint() { return rotateHint; }

    /**
     * Indicates whether this CacheFilter's hint matches the CacheHint
     * passed in.
     */
    boolean matchesHint(CacheHint cacheHint) {
        return this.cacheHint == cacheHint;
    }

    /**
     * Are we attempting to use cache for an unsupported transform mode?  Mostly
     * this is for trying to rotate while scaling the object by different
     * amounts in the x and y directions (this also includes shearing).
     */
    boolean unsupported(double[] xformInfo) {
        double scaleX = xformInfo[0];
        double scaleY = xformInfo[1];
        double rotate = xformInfo[2];

        // If we're trying to rotate...
        if (rotate > EPSILON || rotate < -EPSILON) {
            // ...and if scaleX != scaleY.  This can be in the render xform, or
            // may have made it into the cached image.
            if (scaleX > scaleY + EPSILON || scaleY > scaleX + EPSILON ||
                scaleX < scaleY - EPSILON || scaleY < scaleX - EPSILON ||
                cachedScaleX > cachedScaleY + EPSILON ||
                cachedScaleY > cachedScaleX + EPSILON ||
                cachedScaleX < cachedScaleY - EPSILON ||
                cachedScaleY < cachedScaleX - EPSILON ) {
                    return true;
            }
        }
        return false;
    }

    private boolean isXformScrollCacheCapable(double[] xformInfo) {
        if (unsupported(xformInfo)) {
            return false;
        }
        double rotate = xformInfo[2];
        return rotateHint || rotate == 0;
    }

    /*
     * Do we need to regenerate the cached image?
     * Assumes that caller locked and validated the cachedImageData.untximage
     * if not null...
     */
    private boolean needToRenderCache(BaseTransform renderXform, double[] xformInfo,
                                      float pixelScaleX, float pixelScaleY)
    {
        if (cachedImageData == null) {
            return true;
        }

        if (lastXDelta != 0 || lastYDelta != 0) {
            if (Math.abs(lastXDelta) >= cacheBounds.width || Math.abs(lastYDelta) >= cacheBounds.height ||
                    Math.rint(lastXDelta) != lastXDelta || Math.rint(lastYDelta) != lastYDelta) {
                lastXDelta = lastYDelta = 0;
                return true;
            }
            if (scrollCacheState == ScrollCacheState.CHECKING_PRECONDITIONS) {
                if (scrollCacheCapable() && isXformScrollCacheCapable(xformInfo)) {
                    scrollCacheState = ScrollCacheState.ENABLED;
                } else {
                    scrollCacheState = ScrollCacheState.DISABLED;
                    return true;
                }
            }
        }

        // TODO: is == sufficient for floating point comparison here? (RT-23963)
        if (cachedXform.getMxx() == renderXform.getMxx() &&
            cachedXform.getMyy() == renderXform.getMyy() &&
            cachedXform.getMxy() == renderXform.getMxy() &&
            cachedXform.getMyx() == renderXform.getMyx()) {
            // It's just a translation - use cached Image
            return false;
        }
        // Not just a translation - if was or is unsupported, then must rerender
        if (wasUnsupported || unsupported(xformInfo)) {
            return true;
        }

        double scaleX = xformInfo[0];
        double scaleY = xformInfo[1];
        double rotate = xformInfo[2];
        if (scaleHint) {
            if (cachedScaleX < pixelScaleX || cachedScaleY < pixelScaleY) {
                // We have moved onto a screen with a higher pixelScale and
                // our cache was less than that pixel scale.  Even though
                // we have the scaleHint, we always cache at a minimum of
                // the pixel scale of the screen so we need to re-cache.
                return true;
            }
            if (rotateHint) {
                return false;
            } else {
                // Not caching for rotate: regenerate cache if rotate changed
                if (cachedRotate - EPSILON < rotate && rotate < cachedRotate + EPSILON) {
                    return false;
                } else {
                    return true;
                }
            }
        } else {
            if (rotateHint) {
                // Not caching for scale: regenerate cache if scale changed
                if (cachedScaleX - EPSILON < scaleX && scaleX < cachedScaleX + EPSILON &&
                    cachedScaleY - EPSILON < scaleY && scaleY < cachedScaleY + EPSILON) {
                    return false;
                } else {// Scale is not "equal enough" - regenerate
                    return true;
                }
            }
            else { // Not caching for anything; always regenerate
                return true;
            }
        }
    }

    /*
     * Given the new xform info, update the screenXform as needed to correctly
     * paint the cache to the screen.
     */
    void updateScreenXform(double[] xformInfo) {
        // screenXform will be the difference between the cachedXform and the
        // render xform.

        if (scaleHint) {
            if (rotateHint) {
                double screenScaleX = xformInfo[0] / cachedScaleX;
                double screenScaleY = xformInfo[1] / cachedScaleY;
                double screenRotate = xformInfo[2] - cachedRotate;

                screenXform.setToScale(screenScaleX, screenScaleY);
                screenXform.rotate(screenRotate);
            } else {
                double screenScaleX = xformInfo[0] / cachedScaleX;
                double screenScaleY = xformInfo[1] / cachedScaleY;
                screenXform.setToScale(screenScaleX, screenScaleY);
            }
        } else {
            if (rotateHint) {
                double screenRotate = xformInfo[2] - cachedRotate;
                screenXform.setToRotation(screenRotate, 0.0, 0.0);
            } else {
                // No caching, cache already rendered with xform; just paint it
                screenXform.setTransform(BaseTransform.IDENTITY_TRANSFORM);
            }
        }
    }

    public void invalidate() {
        if (scrollCacheState == ScrollCacheState.ENABLED) {
            scrollCacheState = ScrollCacheState.CHECKING_PRECONDITIONS;
        }
        imageDataUnref();
        lastXDelta = lastYDelta = 0;
    }

    void imageDataUnref() {
        if (tempTexture != null) {
            tempTexture.dispose();
            tempTexture = null;
        }
        if (cachedImageData != null) {
            // While we hold on to this ImageData we leave the texture
            // unlocked so it can be reclaimed, but the default unref()
            // method assumes it was locked.
            Filterable implImage = cachedImageData.getUntransformedImage();
            if (implImage != null) {
                implImage.lock();
            }
            cachedImageData.unref();
            cachedImageData = null;
        }
    }

    void invalidateByTranslation(double translateXDelta, double translateYDelta) {
        if (cachedImageData == null) {
            return;
        }

        if (scrollCacheState == ScrollCacheState.DISABLED) {
            imageDataUnref();
        } else {
             // When both mxt and myt change, we don't currently use scroll optimization
            if (translateXDelta != 0 && translateYDelta != 0) {
                imageDataUnref();
            } else {
                lastYDelta = translateYDelta;
                lastXDelta = translateXDelta;
            }
        }
    }

    public void dispose() {
        invalidate();
        node = null;
    }

    /*
     * unmatrix() and the supporting functions are based on the code from
     * "Decomposing A Matrix Into Simple Transformations" by Spencer W. Thomas
     * from Graphics Gems II, as found at
     * http://tog.acm.org/resources/GraphicsGems/
     * which states, "All code here can be used without restrictions."
     *
     * The code was reduced from handling a 4x4 matrix (3D w/ perspective)
     * to handle just a 2x2 (2D scale/rotate, w/o translate, as that is handled
     * separately).
     */

    /**
     * Given a BaseTransform, decompose it into values for scaleX, scaleY and
     * rotate.
     *
     * The return value is a double[3], the values being:
     *   [0]: scaleX
     *   [1]: scaleY
     *   [2]: rotation angle, in radians, between *** and ***
     *
     * From unmatrix() in unmatrix.c
     */
    double[] unmatrix(BaseTransform xform) {
        double[] retVal = new double[3];

        double[][] row = {{xform.getMxx(), xform.getMxy()},
            {xform.getMyx(), xform.getMyy()}};
        final double xSignum = unitDir(row[0][0]);
        final double ySignum = unitDir(row[1][1]);

        // Compute X scale factor and normalize first row.
        // tran[U_SCALEX] = V3Length(&row[0]);
        // row[0] = *V3Scale(&row[0], 1.0);

        double scaleX = xSignum * v2length(row[0]);
        v2scale(row[0], xSignum);

        // Compute XY shear factor and make 2nd row orthogonal to 1st.
        // tran[U_SHEARXY] = V3Dot(&row[0], &row[1]);
        // (void)V3Combine(&row[1], &row[0], &row[1], 1.0, -tran[U_SHEARXY]);
        //
        // "this is too large by the y scaling factor"
        double shearXY = v2dot(row[0], row[1]);

        // Combine into row[1]
        v2combine(row[1], row[0], row[1], 1.0, -shearXY);

        // Now, compute Y scale and normalize 2nd row
        // tran[U_SCALEY] = V3Length(&row[1]);
        // V3Scale(&row[1], 1.0);
        // tran[U_SHEARXY] /= tran[U_SCALEY];

        double scaleY = ySignum * v2length(row[1]);
        v2scale(row[1], ySignum);

        // Now extract the rotation. (This is new code, not from the Gem.)
        //
        // In our matrix, we now have
        // [   cos(theta)    -sin(theta)    ]
        // [   sin(theta)     cos(theta)    ]
        //
        // TODO: assert: all 4 values are sane (RT-23962)
        //
        double sin = row[1][0];
        double cos = row[0][0];
        double angleRad = 0.0;

        // Recall:
        // arcsin works for theta: -90 -> 90
        // arccos works for theta:   0 -> 180
        if (sin >= 0) {
            // theta is 0 -> 180, use acos()
            angleRad = Math.acos(cos);
        } else {
            if (cos > 0) {
                // sin < 0, cos > 0, so theta is 270 -> 360, aka -90 -> 0
                // use asin(), add 360
                angleRad = 2.0 * Math.PI + Math.asin(sin);
            } else {
                // sin < 0, cos < 0, so theta 180 -> 270
                // cos from 180 -> 270 is inverse of cos from 0->90,
                // so take acos(-cos) and add 180
                angleRad = Math.PI + Math.acos(-cos);
            }
        }

        retVal[0] = scaleX;
        retVal[1] = scaleY;
        retVal[2] = angleRad;

        return retVal;
    }

    /**
     * Return the unit distance in a direction compatible with the matrix element.
     * @param v the matrix element representing a scale factor
     * @return -1.0 if the matrix element is negative, otherwise 1.0
     */
    double unitDir(double v) {
        return v < 0.0 ? -1.0 : 1.0;
    }

    /**
     * make a linear combination of two vectors and return the result
     * result = (v0 * scalarA) + (v1 * scalarB)
     *
     * From V3Combine() in GGVecLib.c
     */
    void v2combine(double v0[], double v1[], double result[], double scalarA, double scalarB) {
        // make a linear combination of two vectors and return the result.
        // result = (a * ascl) + (b * bscl)
        /*
        Vector3 *V3Combine (a, b, result, ascl, bscl)
        Vector3 *a, *b, *result;
        double ascl, bscl;
        {
                result->x = (ascl * a->x) + (bscl * b->x);
                result->y = (ascl * a->y) + (bscl * b->y);
                result->z = (ascl * a->z) + (bscl * b->z);
                return(result);
        */

        result[0] = scalarA*v0[0] + scalarB*v1[0];
        result[1] = scalarA*v0[1] + scalarB*v1[1];
    }

    /**
     * dot product of 2 vectors of length 2
     */
    double v2dot(double v0[], double v1[]) {
        return v0[0]*v1[0] + v0[1]*v1[1];
    }

    /**
     * scale v[] to be relative to newLen
     *
     * From V3Scale() in GGVecLib.c
     */
    void v2scale(double v[], double newLen) {
        double len = v2length(v);
        if (len != 0) {
            v[0] *= newLen / len;
            v[1] *= newLen / len;
        }
    }

    /**
     * returns length of input vector
     *
     * Based on V3Length() in GGVecLib.c
     */
    double v2length(double v[]) {
        return Math.sqrt(v[0]*v[0] + v[1]*v[1]);
    }

    void render(Graphics g) {
        // The following is safe; xform will not be mutated below
        BaseTransform xform = g.getTransformNoClone();
        FilterContext fctx = PrFilterContext.getInstance(g.getAssociatedScreen()); // getFilterContext

        double[] xformInfo = unmatrix(xform);
        boolean isUnsupported = unsupported(xformInfo);

        lastXDelta = lastXDelta * xformInfo[0];
        lastYDelta = lastYDelta * xformInfo[1];

        if (cachedImageData != null) {
            Filterable implImage = cachedImageData.getUntransformedImage();
            if (implImage != null) {
                implImage.lock();
                if (!cachedImageData.validate(fctx)) {
                    implImage.unlock();
                    invalidate();
                }
            }
        }
        float pixelScaleX = g.getPixelScaleFactorX();
        float pixelScaleY = g.getPixelScaleFactorY();
        if (needToRenderCache(xform, xformInfo, pixelScaleX, pixelScaleY)) {
            if (PulseLogger.PULSE_LOGGING_ENABLED) {
                PulseLogger.incrementCounter("CacheFilter rebuilding");
            }
            if (cachedImageData != null) {
                Filterable implImage = cachedImageData.getUntransformedImage();
                if (implImage != null) {
                    implImage.unlock();
                }
                invalidate();
            }
            if (scaleHint) {
                // do not cache the image at a small scale factor when
                // scaleHint is set as it leads to poor rendering results
                // when image is scaled up.
                cachedScaleX = Math.max(pixelScaleX, xformInfo[0]);
                cachedScaleY = Math.max(pixelScaleY, xformInfo[1]);
                cachedRotate = 0;
                cachedXform.setTransform(cachedScaleX, 0.0,
                                         0.0, cachedScaleX,
                                         0.0, 0.0);
                updateScreenXform(xformInfo);
            } else {
                cachedScaleX = xformInfo[0];
                cachedScaleY = xformInfo[1];
                cachedRotate = xformInfo[2];

                // Update the cachedXform to the current xform (ignoring translate).
                cachedXform.setTransform(xform.getMxx(), xform.getMyx(),
                                         xform.getMxy(), xform.getMyy(),
                                         0.0, 0.0);

                // screenXform is always identity in this case, as we've just
                // rendered into the cache using the render xform.
                screenXform.setTransform(BaseTransform.IDENTITY_TRANSFORM);
            }

            cacheBounds = getCacheBounds(cacheBounds, cachedXform);
            cachedImageData = createImageData(fctx, cacheBounds);
            renderNodeToCache(cachedImageData, cacheBounds, cachedXform, null);

            // cachedBounds includes effects, and is in *scene* coords
            Rectangle cachedBounds = cachedImageData.getUntransformedBounds();

            // Save out the (un-transformed) x & y coordinates.  This accounts
            // for effects and other reasons the untranslated location may not
            // be 0,0.
            cachedX = cachedBounds.x;
            cachedY = cachedBounds.y;

        } else {
            if (scrollCacheState == ScrollCacheState.ENABLED &&
                    (lastXDelta != 0 || lastYDelta != 0) ) {
                moveCacheBy(cachedImageData, lastXDelta, lastYDelta);
                renderNodeToCache(cachedImageData, cacheBounds, cachedXform, computeDirtyRegionForTranslate());
                lastXDelta = lastYDelta = 0;
            }
            // Using the cached image; calculate screenXform to paint to screen.
            if (isUnsupported) {
                // Only way we should be using the cached image in the
                // unsupported case is for a change in translate only.  No other
                // xform should be needed, so use identity.

                // TODO: assert cachedXform == render xform (ignoring translate)
                //   or  assert xforminfo == cachedXform info (RT-23962)
                screenXform.setTransform(BaseTransform.IDENTITY_TRANSFORM);
            } else {
                updateScreenXform(xformInfo);
            }
        }
        // If this render is unsupported, remember for next time.  We'll need
        // to regenerate the cache once we're in a supported scenario again.
        wasUnsupported = isUnsupported;

        Filterable implImage = cachedImageData.getUntransformedImage();
        if (implImage == null) {
            if (PulseLogger.PULSE_LOGGING_ENABLED) {
                PulseLogger.incrementCounter("CacheFilter not used");
            }
            renderNodeToScreen(g);
        } else {
            double mxt = xform.getMxt();
            double myt = xform.getMyt();
            renderCacheToScreen(g, implImage, mxt, myt);
            implImage.unlock();
        }
    }

    /**
     * Create the ImageData for the cached bitmap, with the specified bounds.
     */
    ImageData createImageData(FilterContext fctx, Rectangle bounds) {
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
    void renderNodeToCache(ImageData cacheData,
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
                node.doPreCulling(TEMP_CONTAINER, TEMP_CACHEFILTER_TRANSFORM, new GeneralTransform3D());
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
     */
    void renderNodeToScreen(Object implGraphics) {
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
    void renderCacheToScreen(Object implGraphics, Filterable implImage,
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
    boolean scrollCacheCapable() {
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
        if (clip == null || !clip.isRectClip(BaseTransform.IDENTITY_TRANSFORM, false)) {
            return false;
        }

        if (node instanceof NGRegion) {
            NGRegion region = (NGRegion) node;
            if (!region.getBorder().isEmpty()) {
                return false;
            }
            final Background background = region.getBackground();

            if (!background.isEmpty()) {
                if (!background.getImages().isEmpty()
                        || background.getFills().size() != 1) {
                    return false;
                }
                BackgroundFill fill = background.getFills().get(0);
                javafx.scene.paint.Paint fillPaint = fill.getFill();
                BaseBounds clipBounds = clip.getCompleteBounds(TEMP_BOUNDS, BaseTransform.IDENTITY_TRANSFORM);

                return fillPaint.isOpaque() && fillPaint instanceof Color && fill.getInsets().equals(Insets.EMPTY)
                        && clipBounds.getMinX() == 0 && clipBounds.getMinY() == 0
                        && clipBounds.getMaxX() == region.getWidth() && clipBounds.getMaxY() == region.getHeight();
            }
        }

        return true;
    }

    /**
     * Moves a subregion of the cache, "scrolling" the cache by x/y Delta.
     * On of xDelta/yDelta must be zero. The rest of the pixels will be cleared.
     * @param cachedImageData cache
     * @param xDelta x-axis delta
     * @param yDelta y-axis delta
     */
    void moveCacheBy(ImageData cachedImageData, double xDelta, double yDelta) {
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
                                Texture.WrapMode.CLAMP_NOT_NEEDED);
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
    Rectangle getCacheBounds(Rectangle bounds, BaseTransform xform) {
        final BaseBounds b = node.getClippedBounds(TEMP_BOUNDS, xform);
        bounds.setBounds(b);
        return bounds;
    }

    BaseBounds computeDirtyBounds(BaseBounds region, BaseTransform tx, GeneralTransform3D pvTx) {
        // For now, we just use the computed dirty bounds of the Node and
        // round them out before the transforms.
        // Later, we could use the bounds of the cache
        // to compute the dirty region directly (and more accurately).
        // See RT-34928 for more details.
        if (!node.dirtyBounds.isEmpty()) {
            region = region.deriveWithNewBounds(node.dirtyBounds);
        } else {
            region = region.deriveWithNewBounds(node.transformedBounds);
        }

        if (!region.isEmpty()) {
            region.roundOut();
            region = node.computePadding(region);
            region = tx.transform(region, region);
            region = pvTx.transform(region, region);
        }
        return region;
    }
}
