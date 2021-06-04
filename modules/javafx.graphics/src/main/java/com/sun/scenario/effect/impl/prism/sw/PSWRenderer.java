/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.impl.prism.sw;

import java.lang.reflect.Constructor;
import com.sun.glass.ui.Screen;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.Graphics;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.Image;
import com.sun.prism.RTTexture;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.Texture.Usage;
import com.sun.prism.Texture.WrapMode;
import com.sun.scenario.effect.Effect.AccelType;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.Filterable;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.prism.PrDrawable;
import com.sun.scenario.effect.impl.prism.PrImage;
import com.sun.scenario.effect.impl.prism.PrRenderer;
import com.sun.scenario.effect.impl.sw.RendererDelegate;

import static com.sun.scenario.effect.impl.Renderer.RendererState.*;

public class PSWRenderer extends PrRenderer {

    private final Screen screen;
    private final ResourceFactory resourceFactory;
    private final RendererDelegate delegate;
    private RendererState state;

    private PSWRenderer(Screen screen, RendererDelegate delegate) {
        this.screen = screen;
        this.resourceFactory = null;
        this.delegate = delegate;
        synchronized (this) {
            state = OK;
        }
    }

    private PSWRenderer(ResourceFactory factory, RendererDelegate delegate) {
        this.screen = null;
        this.resourceFactory = factory;
        this.delegate = delegate;
        synchronized (this) {
            state = OK;
        }
    }

    @Override
    public PrDrawable createDrawable(RTTexture rtt) {
        return PSWDrawable.create(rtt);
    }

    /**
     * Returns a {@code JSW} (Java/CPU) renderer for the given screen.
     *
     * @return a {@code JSW} (Java/CPU) renderer
     */
    public synchronized static PSWRenderer createJSWInstance(Screen screen) {
        PSWRenderer ret = null;
        try {
            Class klass = Class.forName(rootPkg + ".impl.sw.java.JSWRendererDelegate");
            RendererDelegate delegate = (RendererDelegate)klass.getDeclaredConstructor().newInstance();
            ret = new PSWRenderer(screen, delegate);
        } catch (Throwable e) {}
        return ret;
    }

    /**
     * Returns a {@code JSW} (Java/CPU) renderer for the given screen.
     *
     * @return a {@code JSW} (Java/CPU) renderer
     */
    public synchronized static PSWRenderer createJSWInstance(ResourceFactory factory) {
        PSWRenderer ret = null;
        try {
            Class klass = Class.forName(rootPkg + ".impl.sw.java.JSWRendererDelegate");
            RendererDelegate delegate = (RendererDelegate)klass.getDeclaredConstructor().newInstance();
            ret = new PSWRenderer(factory, delegate);
        } catch (Throwable e) {}
        return ret;
    }

    public synchronized static PSWRenderer createJSWInstance(FilterContext fctx) {
        PSWRenderer ret = null;
        try {
            ResourceFactory factory = (ResourceFactory)fctx.getReferent();
            ret = createJSWInstance(factory);
        } catch (Throwable e) {}
        return ret;
    }

    /**
     * Returns an {@code SSE} (SIMD/CPU) renderer for the given screen.
     *
     * @return an {@code SSE} (SIMD/CPU) renderer
     */
    private synchronized static PSWRenderer createSSEInstance(Screen screen) {
        PSWRenderer ret = null;
        try {
            Class klass = Class.forName(rootPkg + ".impl.sw.sse.SSERendererDelegate");
            RendererDelegate delegate = (RendererDelegate)klass.getDeclaredConstructor().newInstance();
            ret = new PSWRenderer(screen, delegate);
        } catch (Throwable e) {}
        return ret;
    }

    public static Renderer createRenderer(FilterContext fctx) {
        Object ref = fctx.getReferent();
        GraphicsPipeline pipe = GraphicsPipeline.getPipeline();
        if (pipe == null || !(ref instanceof Screen)) {
            return null;
        }
        Screen screen = (Screen)ref;
        Renderer renderer = createSSEInstance(screen);
        if (renderer == null) {
            renderer = createJSWInstance(screen);
        }
        return renderer;
    }

    @Override
    public AccelType getAccelType() {
        return delegate.getAccelType();
    }

    /**
     * Warning: may be called on the rendering thread
     */
    @Override
    public synchronized RendererState getRendererState() {
        return state;
    }

    @Override
    protected Renderer getBackupRenderer() {
        return this;
    }

    /**
     * Disposes this renderer (flushes the associated images).
     *
     * Warning: must be called only on the rendering thread (for example in
     * response to device reset event).
     *
     * May be called multiple times.
     */
    protected void dispose() {
        synchronized (this) {
            state = DISPOSED;
        }
    }

    /**
     * Marks this renderer as lost.
     *
     * Warning: may be called on the rendering thread
     */
    protected final synchronized void markLost() {
        if (state == OK) {
            state = LOST;
        }
    }

    @Override
    public int getCompatibleWidth(int w) {
        if (screen != null) {
            return PSWDrawable.getCompatibleWidth(screen, w);
        } else {
            return resourceFactory.getRTTWidth(w, WrapMode.CLAMP_TO_EDGE);
        }
    }

    @Override
    public int getCompatibleHeight(int h) {
        if (screen != null) {
            return PSWDrawable.getCompatibleHeight(screen, h);
        } else {
            return resourceFactory.getRTTHeight(h, WrapMode.CLAMP_TO_EDGE);
        }
    }

    @Override
    public final PSWDrawable createCompatibleImage(int w, int h) {
        if (screen != null) {
            return PSWDrawable.create(screen, w, h);
        } else {
            RTTexture rtt =
                resourceFactory.createRTTexture(w, h, WrapMode.CLAMP_TO_EDGE);
            return PSWDrawable.create(rtt);
        }
    }

    @Override
    public PSWDrawable getCompatibleImage(int w, int h) {
        PSWDrawable im = (PSWDrawable)super.getCompatibleImage(w, h);
        // either we ran out of vram or the device is lost
        if (im == null) {
            markLost();
        }
        return im;
    }

    /**
     * Creates a new {@code EffectPeer} instance that can be used by
     * any of the Prism-based backend implementations.  For example,
     * we can implement the {@code Reflection} effect using only
     * Prism operations, so we can share that implemenation across all
     * of the Prism-based backends.
     *
     * @param fctx the filter context
     * @param name the name of the effect peer
     * @return a new {@code EffectPeer} instance
     */
    private EffectPeer createIntrinsicPeer(FilterContext fctx, String name) {
        Class klass = null;
        EffectPeer peer;
        try {
            klass = Class.forName(rootPkg + ".impl.prism.Pr" + name + "Peer");
            Constructor ctor = klass.getConstructor(new Class[]
                { FilterContext.class, Renderer.class, String.class });
            peer = (EffectPeer)ctor.newInstance(new Object[] {fctx, this, name});
        } catch (Exception e) {
            return null;
        }
        return peer;
    }

    /**
     * Creates a new {@code EffectPeer} instance that is specific to
     * the current software-based backend.
     *
     * @param fctx the filter context
     * @param name the name of the effect peer
     * @param unrollCount the unroll count
     * @return a new {@code EffectPeer} instance
     */
    private EffectPeer createPlatformPeer(FilterContext fctx, String name,
                                          int unrollCount)
    {
        String klassName = delegate.getPlatformPeerName(name, unrollCount);
        EffectPeer peer;
        try {
            Class klass = Class.forName(klassName);
            Constructor ctor = klass.getConstructor(new Class[]
                { FilterContext.class, Renderer.class, String.class });
            peer = (EffectPeer)ctor.newInstance(new Object[] {fctx, this, name});
        } catch (Exception e) {
            System.err.println("Error: " + getAccelType() +
                               " peer not found for: " + name +
                               " due to error: " + e.getMessage());
            return null;
        }
        return peer;
    }

    @Override
    protected EffectPeer createPeer(FilterContext fctx, String name,
                                    int unrollCount)
    {
        if (PrRenderer.isIntrinsicPeer(name)) {
            // create an intrinsic peer (one that's handled by Prism)
            return createIntrinsicPeer(fctx, name);
        } else {
            // try creating a platform-specific peer
            return createPlatformPeer(fctx, name, unrollCount);
        }
    }

    @Override
    public boolean isImageDataCompatible(final ImageData id) {
        return (getRendererState() == OK &&
                id.getUntransformedImage() instanceof PSWDrawable);
    }

    @Override
    public void clearImage(Filterable filterable) {
        PSWDrawable img = (PSWDrawable)filterable;
        img.clear();
    }

    @Override
    public ImageData createImageData(FilterContext fctx, Filterable src) {
        if (!(src instanceof PrImage)) {
            throw new IllegalArgumentException("Identity source must be PrImage");
        }
        Image img = ((PrImage)src).getImage();
        int w = img.getWidth();
        int h = img.getHeight();
        PSWDrawable dst = createCompatibleImage(w, h);
        if (dst == null) {
            return null;
        }
        // RT-27561
        // TODO: it is wasteful to create an RTT here; eventually it would
        // be nice if we could use plain Textures as a source Filterable...
        Graphics g = dst.createGraphics();
        ResourceFactory factory = g.getResourceFactory();
        Texture tex =
            factory.createTexture(img, Usage.DEFAULT, WrapMode.CLAMP_TO_EDGE);
        g.drawTexture(tex, 0, 0, w, h);
        // NOTE: calling sync() should not be required; ideally calling
        // Texture.dispose() would flush any pending operations that may
        // depend on that texture...
        g.sync();
        tex.dispose();
        return new ImageData(fctx, dst, new Rectangle(w, h));
    }

    @Override
    public Filterable transform(FilterContext fctx,
                                Filterable original,
                                BaseTransform transform,
                                Rectangle origBounds,
                                Rectangle xformBounds)
    {
        PSWDrawable dst = (PSWDrawable)
            getCompatibleImage(xformBounds.width, xformBounds.height);
        if (dst != null) {
            Graphics g = dst.createGraphics();
            g.translate(-xformBounds.x, -xformBounds.y);
            g.transform(transform);
            g.drawTexture(((PSWDrawable)original).getTextureObject(),
                          origBounds.x, origBounds.y,
                          origBounds.width, origBounds.height);
        }
        return dst;
    }

    @Override
    public ImageData transform(FilterContext fctx, ImageData original,
                               BaseTransform transform,
                               Rectangle origBounds,
                               Rectangle xformBounds)
    {
        PSWDrawable dst = (PSWDrawable)
            getCompatibleImage(xformBounds.width, xformBounds.height);
        if (dst != null) {
            PSWDrawable orig = (PSWDrawable)original.getUntransformedImage();
            Graphics g = dst.createGraphics();
            g.translate(-xformBounds.x, -xformBounds.y);
            g.transform(transform);
            g.drawTexture(orig.getTextureObject(),
                          origBounds.x, origBounds.y,
                          origBounds.width, origBounds.height);
        }
        original.unref();
        return new ImageData(fctx, dst, xformBounds);
    }
}
