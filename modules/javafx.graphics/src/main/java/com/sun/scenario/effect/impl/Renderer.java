/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.Affine2D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.Effect;
import com.sun.scenario.effect.Effect.AccelType;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.Filterable;
import com.sun.scenario.effect.FloatMap;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.LockableResource;

public abstract class Renderer {

    /**
     * Enumeration describing the lifecycle states of the renderer.
     * When the renderer is created, it is in the {@code OK} state.
     *
     * It could become {@code LOST} at some point. This may happen for
     * example if the renderer is susceptible to display changes.
     * <p>
     * When the renderer is in the {@code LOST} state it can't be used
     * for rendering, instead a {@link #getBackupRenderer() backup renderer}
     * must be used.
     * <p>
     * Sometime later the renderer could enter the {@code DISPOSED} state,
     * at which point it will be removed from the cache and a new renderer
     * will be created for the particular context.
     * <p>
     * Thus the lifecycle of a renderer is:
     * {@code OK} [=> {@code LOST} [=> {@code DISPOSED}]]
     */
    public static enum RendererState {
        /**
         * Renderer can be used for rendering.
         */
        OK,
        /**
         * Renderer is lost, a backup renderer must be used.
         */
        LOST,
        /**
         * Renderer is disposed, it is no longer usable and must be replaced.
         */
        DISPOSED
    }

    public static final String rootPkg = "com.sun.scenario.effect";
    private static final Map<FilterContext, Renderer> rendererMap =
        new HashMap<FilterContext, Renderer>(1);
    private Map<String, EffectPeer> peerCache =
        Collections.synchronizedMap(new HashMap<String, EffectPeer>(5));
    private final ImagePool imagePool;

    protected static final boolean verbose = AccessController.doPrivileged(
            (PrivilegedAction<Boolean>) () -> Boolean.getBoolean("decora.verbose"));

    protected Renderer() {
        this.imagePool = new ImagePool();
    }

    /**
     * Returns the {@link AccelType} used by default for peers of this renderer.
     *
     * Note that the Renderer may specialize in peers of this type, and
     * it may create them in general by default, but the renderers all
     * look for an Intrinsic peer for a given operation as well so the
     * actual peer implementaiton for a given effect may sometimes differ
     * from this {@code AccelType}.  Care should be taken if the actual
     * {@code AccelType} for a specific operation is needed, then the
     * {@link EffectPeer#getAccelType()} should be consulted directly
     * in those cases.
     *
     * @return the {@code AccelType} used by typical peers of this renderer
     */
    public abstract AccelType getAccelType();

    public abstract int getCompatibleWidth(int w);
    public abstract int getCompatibleHeight(int h);
    public abstract PoolFilterable createCompatibleImage(int w, int h);

    public PoolFilterable getCompatibleImage(int w, int h) {
        return imagePool.checkOut(this, w, h);
    }

    public void releaseCompatibleImage(Filterable image) {
        if (image instanceof PoolFilterable) {
            ImagePool pool = ((PoolFilterable) image).getImagePool();
            if (pool != null) {
                pool.checkIn((PoolFilterable) image);
                return;
            }
//        } else {
            // Error?
        }
        image.unlock();
    }

    /**
     * This is a temporary workaround for a PowerVR SGX issue.  See
     * ImagePool for more details.
     */
    public void releasePurgatory() {
        imagePool.releasePurgatory();
    }

    /**
     * Mainly used by {@code ImagePool} for the purpose of clearing
     * an image before handing it back to the user.
     *
     * @param image the image to be cleared
     */
    public abstract void clearImage(Filterable image);

    /**
     * Mainly used by the {@code Identity} effect for the purpose of
     * creating a cached {@code ImageData} from the given platform-specific
     * image (e.g. a {@code BufferedImage} wrapped in a {@code J2DImage}).
     *
     * @param fctx the filter context
     * @param platformImage the platform-specific source image to be copied
     * into the new {@code ImageData} object
     * @return a new {@code ImageData}
     */
    public abstract ImageData createImageData(FilterContext fctx,
                                              Filterable src);

    public ImageData transform(FilterContext fctx, ImageData img,
                               int xpow2scales, int ypow2scales)
    {
        if (!img.getTransform().isIdentity()) {
            throw new InternalError("transform by powers of 2 requires untransformed source");
        }
        if ((xpow2scales | ypow2scales) == 0) {
            return img;
        }
        Affine2D at = new Affine2D();
        // Any amount of upscaling and up to 1 level of downscaling
        // can be handled by the filters themselves...
        while (xpow2scales < -1 || ypow2scales < -1) {
            Rectangle origbounds = img.getUntransformedBounds();
            Rectangle newbounds = new Rectangle(origbounds);
            double xscale = 1.0;
            double yscale = 1.0;
            if (xpow2scales < 0) {
                // To avoid loss, only scale down one step at a time
                xscale = 0.5;
                newbounds.width = (origbounds.width + 1) / 2;
                newbounds.x /= 2;
                xpow2scales++;
            }
            if (ypow2scales < 0) {
                // To avoid loss, only scale down one step at a time
                yscale = 0.5;
                newbounds.height = (origbounds.height + 1) / 2;
                newbounds.y /= 2;
                ypow2scales++;
            }
            at.setToScale(xscale, yscale);
            img = transform(fctx, img, at, origbounds, newbounds);
        }
        if ((xpow2scales | ypow2scales) != 0) {
            // assert xscale >= -1 and yscale >= -1
            double xscale = (xpow2scales < 0) ? 0.5 : 1 << xpow2scales;
            double yscale = (ypow2scales < 0) ? 0.5 : 1 << ypow2scales;
            at.setToScale(xscale, yscale);
            img = img.transform(at);
        }
        return img;
    }

    public abstract Filterable transform(FilterContext fctx,
                                         Filterable original,
                                         BaseTransform transform,
                                         Rectangle origBounds,
                                         Rectangle xformBounds);
    public abstract ImageData transform(FilterContext fctx, ImageData original,
                                        BaseTransform transform,
                                        Rectangle origBounds,
                                        Rectangle xformBounds);

    // NOTE: these two methods are only relevant to HW codepaths; should
    // find a way to push them down a level...
    public LockableResource createFloatTexture(int w, int h) {
        throw new InternalError();
    }
    public void updateFloatTexture(LockableResource texture, FloatMap map) {
        throw new InternalError();
    }

    /**
     * Returns a (cached) instance of peer given the context, name and unroll
     * count.
     *
     * @param fctx filter context - same as this renderer's context
     * @param name not-unrolled name of the peer
     * @param unrollCount
     * @return cached peer for this name and unroll count
     */
    public final synchronized EffectPeer
        getPeerInstance(FilterContext fctx, String name, int unrollCount)
    {
        // first look for a previously cached peer using only the base name
        // (e.g. GaussianBlur); software peers do not (currently) have
        // unrolled loops, so this step should locate those...
        EffectPeer peer = peerCache.get(name);
        if (peer != null) {
            return peer;
        }
        // failing that, if there is a positive unrollCount, we attempt
        // to find a previously cached hardware peer for that unrollCount
        if (unrollCount > 0) {
            peer = peerCache.get(name + "_" + unrollCount);
            if (peer != null) {
                return peer;
            }
        }

        peer = createPeer(fctx, name, unrollCount);
        if (peer == null) {
            throw new RuntimeException("Could not create peer  " + name +
                                       " for renderer " + this);
        }
        // use the peer's unique name as the hashmap key
        peerCache.put(peer.getUniqueName(), peer);

        return peer;
    }


    /**
     * Returns this renderer's current state.
     *
     * @return the state
     * @see RendererState
     */
    public abstract RendererState getRendererState();

    /**
     * Creates a new peer given the context, name and unroll count.
     *
     * @param fctx context shared with the renderer
     * @param name of the peer
     * @param unrollCount unroll count
     * @return new peer
     */
    protected abstract EffectPeer createPeer(FilterContext fctx,
                                             String name, int unrollCount);

    /**
     * Returns current cache of peers.
     */
    protected Collection<EffectPeer> getPeers() {
        return peerCache.values();
    }

    /**
     * This method can be used by subclasses to create a backup renderer,
     * either a SW (Java) renderer or an SSE (native) renderer, depending
     * on what is available.
     *
     * @return an instance of Renderer that uses CPU filtering
     */
    protected static Renderer getSoftwareRenderer() {
        return RendererFactory.getSoftwareRenderer();
    }

    /**
     * Returns an instance of backup renderer to be used if this renderer
     * is in {@code LOST} state.
     *
     * @return backup renderer
     */
    protected abstract Renderer getBackupRenderer();

    /**
     * Returns a {@code Renderer} instance that is most appropriate
     * for the given size of the source data.  The default implementation
     * simply returns "this" renderer, but subclasses may override this
     * method and return a different renderer depending on the size of
     * the operation.  For example, a GPU-based renderer may wish to
     * return a software renderer for small-sized operations (because of
     * lower overhead, etc).
     *
     * @param approxW approximate input width
     * @param approxH approximate input height
     * @return the {@code Renderer} best suited for this size
     */
    protected Renderer getRendererForSize(Effect effect, int approxW, int approxH) {
        return this;
    }

    /**
     * Returns a renderer associated with given filter context based on the
     * environment and flags set.
     *
     * Renderers are per filter context cached.
     *
     * @param fctx context to create the renderer for
     * @return renderer
     */
    public static synchronized Renderer getRenderer(FilterContext fctx) {
        if (fctx == null) {
            throw new IllegalArgumentException("FilterContext must be non-null");
        }

        Renderer r = rendererMap.get(fctx);
        if (r != null) {
            if (r.getRendererState() == RendererState.OK) {
                return r;
            }
            if (r.getRendererState() == RendererState.LOST) {
                // use the backup while the renderer is in lost state, until
                // it is disposed (or forever if it can't be disposed/reset)
                // Note: we don't add it to the cache to prevent permanent
                // association of the backup renderer and this filter context.
                return r.getBackupRenderer();
            }
            if (r.getRendererState() == RendererState.DISPOSED) {
                r = null;
                // we remove disposed renderers below instead of here to cover
                // cases where we never use a context which the disposed
                // renderer is associated with
            }
        }

        if (r == null) {
            // clean up all disposed renderers first
            Collection<Renderer> renderers = rendererMap.values();
            for (Iterator<Renderer> iter = renderers.iterator(); iter.hasNext();)
            {
                Renderer ren = iter.next();
                if (ren.getRendererState() == RendererState.DISPOSED) {
                    ren.imagePool.dispose();
                    iter.remove();
                }
            }

            r = RendererFactory.createRenderer(fctx);
            if (r == null) {
                throw new RuntimeException("Error creating a Renderer");
            } else {
                if (verbose) {
                    String klassName = r.getClass().getName();
                    String rname = klassName.substring(klassName.lastIndexOf(".")+1);
                    Object screen = fctx.getReferent();
                    System.out.println("Created " + rname +
                        " (AccelType=" + r.getAccelType() +
                        ") for " + screen);
                }
            }
            rendererMap.put(fctx, r);
        }
        return r;
    }

    /**
     * Returns a renderer that is most optimal for the approximate size
     * of the filtering operation.
     *
     * @param fctx context to create the renderer for
     * @param effect uses in the rendering
     * @param approxW approximate input width
     * @param approxH approximate input height
     * @return renderer
     */
    public static Renderer getRenderer(FilterContext fctx, Effect effect,
                                       int approxW, int approxH) {
        return getRenderer(fctx).getRendererForSize(effect, approxW, approxH);
    }

    /**
     * Determines whether the passed {@code ImageData} is compatible with this
     * renderer (that is, if it can be used as a input source for this
     * renderer's peers).
     *
     * @param id {@code ImageData} to be checked
     * @return true if this image data is compatible, false otherwise
     */
    public abstract boolean isImageDataCompatible(ImageData id);
}
