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

import javafx.scene.CacheHint;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.Filterable;
import com.sun.scenario.effect.ImageData;

/*
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
 */
public abstract class BaseCacheFilter {
    private double lastXDelta;
    private double lastYDelta;
    
    private static final Rectangle TEMP_RECT = new Rectangle();

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

    private static enum ScrollCacheState {
        CHECKING_PRECONDITIONS,
        ENABLED,
        DISABLED
    }
    private ScrollCacheState scrollCacheState = ScrollCacheState.CHECKING_PRECONDITIONS;
    
    // Note: this ImageData is always created and assumed to be untransformed.
    protected ImageData cachedImageData;
    private Rectangle cacheBounds = new Rectangle();
    

    // Used to draw into the cache
    private Affine2D cachedXform = new Affine2D();

    // The scale and rotate used to draw into the cache
    private double cachedScaleX;
    private double cachedScaleY;
    private double cachedRotate;

    protected double cachedX;
    protected double cachedY;
    protected BaseNode node;

    // Used to draw the cached image to the screen
    protected Affine2D screenXform = new Affine2D();

    // Cache hint settings
    private boolean scaleHint;
    private boolean rotateHint;
    // We keep this around for the sake of matchesHint
    private CacheHint cacheHint;

    // Was the last paint unsupported by the cache?  If so, will need to
    // regenerate the cache next time.
    private boolean wasUnsupported = false;

    // Fun with floating point
    private static final double EPSILON = 0.0000001;

    protected BaseCacheFilter(BaseNode node, CacheHint cacheHint) {
        this.node = node;
        this.scrollCacheState = ScrollCacheState.CHECKING_PRECONDITIONS;
        setHint(cacheHint);
    }

    public void setHint(CacheHint cacheHint) {
        this.cacheHint = cacheHint;
        this.scaleHint = (cacheHint == CacheHint.SCALE ||
                          cacheHint == CacheHint.SCALE_AND_ROTATE);
        this.rotateHint = (cacheHint == CacheHint.ROTATE ||
                           cacheHint == CacheHint.SCALE_AND_ROTATE);
    }

    /**
     * Indicates whether this BaseCacheFilter's hint matches the CacheHint
     * passed in.
     */
    boolean matchesHint(CacheHint cacheHint) {
        return this.cacheHint == cacheHint;
    }

    /**
     * Implemented by concrete subclasses to create the ImageData for the bitmap
     * cache.
     */
    protected abstract ImageData impl_createImageData(FilterContext fctx,
                                                      Rectangle bounds);
    
    /**
     * The bounds of the cache
     */
    protected abstract Rectangle impl_getCacheBounds(Rectangle bounds, BaseTransform xform);
    
    /**
     * Render node to cache
     */
    protected abstract void impl_renderNodeToCache(ImageData imageData,
                                                   Rectangle cacheBounds,
                                                   BaseTransform xform,
                                                   Rectangle dirtyBounds);

    /**
     * Called on concrete subclasses to render the node directly to the screen,
     * in the case that the cached image is unexpectedly null.  See RT-6428.
     */
    protected abstract void impl_renderNodeToScreen(Object implGraphics,
                                                    BaseTransform xform);

    /**
     * Called on concrete subclasses to render the cached image to the screen,
     * translated by mxt, myt.
     */
    protected abstract void impl_renderCacheToScreen(Object implGraphics,
                                                Filterable implImage,
                                                double mxt, double myt);
    
    /**
     * Is it possible to use scroll optimization?
     */
    protected abstract boolean impl_scrollCacheCapable();
    
    /**
     * Move the subregion of the cache by specified delta
     */
    protected abstract void impl_moveCacheBy(ImageData cachedImageData,
            double lastXDelta, double lastYDelta);

    /**
     * Render the cached node to the screen, updating the cached image as
     * necessary given the current cacheHint and specified xform.
     */
    public void render(Object implGraphics, BaseTransform xform,
                       FilterContext fctx) {

        // Note: xform should not be modified, for the sake of Prism

        double mxx = xform.getMxx();
        double myx = xform.getMyx();
        double mxy = xform.getMxy();
        double myy = xform.getMyy();
        double mxt = xform.getMxt();
        double myt = xform.getMyt();

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
        if (needToRenderCache(fctx, xform, xformInfo)) {
            if (cachedImageData != null) {
                Filterable implImage = cachedImageData.getUntransformedImage();
                if (implImage != null) {
                    implImage.unlock();
                }
                invalidate();
            }
            // Update the cachedXform to the current xform (ignoring translate).
            cachedXform.setTransform(mxx, myx, mxy, myy, 0.0, 0.0);
            cachedScaleX = xformInfo[0];
            cachedScaleY = xformInfo[1];
            cachedRotate = xformInfo[2];
            
            cacheBounds = impl_getCacheBounds(cacheBounds, cachedXform);
            cachedImageData = impl_createImageData(fctx, cacheBounds);
            impl_renderNodeToCache(cachedImageData, cacheBounds, cachedXform, null);

            // cachedBounds includes effects, and is in *scene* coords
            Rectangle cachedBounds = cachedImageData.getUntransformedBounds();

            // Save out the (un-transformed) x & y coordinates.  This accounts
            // for effects and other reasons the untranslated location may not
            // be 0,0.
            cachedX = cachedBounds.x;
            cachedY = cachedBounds.y;

            // screenXform is always identity in this case, as we've just
            // rendered into the cache using the render xform.
            screenXform.setTransform(BaseTransform.IDENTITY_TRANSFORM);
        } else {
            if (scrollCacheState == ScrollCacheState.ENABLED && 
                    (lastXDelta != 0 || lastYDelta != 0) ) {
                impl_moveCacheBy(cachedImageData, lastXDelta, lastYDelta);
                impl_renderNodeToCache(cachedImageData, cacheBounds, cachedXform, computeDirtyRegionForTranslate());
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
            impl_renderNodeToScreen(implGraphics, xform);
        } else {
            impl_renderCacheToScreen(implGraphics, implImage, mxt, myt);
            implImage.unlock();
        }
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
    private boolean needToRenderCache(FilterContext fctx, BaseTransform renderXform,
                                      double[] xformInfo) {
        if (cachedImageData == null) {
            return true;
        }
        
        if (lastXDelta != 0 || lastYDelta != 0) {
            if (Math.abs(lastXDelta) >= cacheBounds.width || Math.abs(lastYDelta) >= cacheBounds.height ||
                    Math.rint(lastXDelta) != lastXDelta || Math.rint(lastYDelta) != lastYDelta) {
                node.clearDirtyTree(); // Need to clear dirty (by translation) flags in the children
                lastXDelta = lastYDelta = 0;
                return true;
            }
            if (scrollCacheState == ScrollCacheState.CHECKING_PRECONDITIONS) {
                if (impl_scrollCacheCapable() && isXformScrollCacheCapable(xformInfo)) {
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

    protected void imageDataUnref() {
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
        final double xSignum = Math.signum(row[0][0]);
        final double ySignum = Math.signum(row[1][1]);

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
    double[] v2scale(double v[], double newLen) {
        double len = v2length(v);
        double[] retVal = v;
        if (len != 0) {
            v[0] *= newLen / len;
            v[1] *= newLen / len;
        }
        return retVal;
    }

    /**
     * returns length of input vector
     *
     * Based on V3Length() in GGVecLib.c
     */
    double v2length(double v[]) {
        return Math.sqrt(v[0]*v[0] + v[1]*v[1]);
    }
}

