/*
 * Copyright (c) 2009, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.impl.prism.ps;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.FloatBuffer;
import java.util.Map;
import com.sun.glass.ui.Screen;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.prism.Graphics;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.GraphicsPipeline.ShaderModel;
import com.sun.prism.GraphicsPipeline.ShaderType;
import com.sun.prism.Image;
import com.sun.prism.PixelFormat;
import com.sun.prism.RTTexture;
import com.sun.prism.ResourceFactory;
import com.sun.prism.ResourceFactoryListener;
import com.sun.prism.Texture;
import com.sun.prism.Texture.Usage;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.ps.Shader;
import com.sun.prism.ps.ShaderFactory;
import com.sun.scenario.effect.Effect.AccelType;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.Filterable;
import com.sun.scenario.effect.FloatMap;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.LockableResource;
import com.sun.scenario.effect.impl.EffectPeer;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.hw.ShaderSource;
import com.sun.scenario.effect.impl.prism.PrDrawable;
import com.sun.scenario.effect.impl.prism.PrFilterContext;
import com.sun.scenario.effect.impl.prism.PrImage;
import com.sun.scenario.effect.impl.prism.PrRenderer;
import com.sun.scenario.effect.impl.prism.PrTexture;

import static com.sun.scenario.effect.impl.Renderer.RendererState.*;

public class PPSRenderer extends PrRenderer {

    private ResourceFactory rf;
    private Screen screen;
    private final ShaderSource shaderSource;
    private RendererState state;
    private boolean needsSWDispMap;

    private final ResourceFactoryListener listener =
        new ResourceFactoryListener()
    {
        @Override
        public void factoryReset() {
            dispose();
        }

        @Override
        public void factoryReleased() {
            dispose();
        }
    };

    private PPSRenderer(Screen screen, ShaderSource shaderSource) {
        this.shaderSource = shaderSource;
        this.screen = screen;
        synchronized (this) {
            state = NOTREADY;
        }
    }

    // Must be called on the renderer thread
    private boolean validate() {
        RendererState st = getRendererState();
        switch (st) {
            case NOTREADY:
                if (rf == null) {
                    rf = GraphicsPipeline.getPipeline().getResourceFactory(screen);
                    if (rf == null) {
                        return false;
                    }
                }
                if (rf.isDisposed()) {
                    // This case happens if the ResourceFactory has already been
                    // disposed the first time we try to validate it, meaning
                    // this Renderer was never in the "OK" state. As a result,
                    // we have never added this Renderer as an RF listener, so
                    // we need to explicitly dispose.
                    dispose();

                    return false;
                }

                // Device is now ready, transition to OK.
                rf.addFactoryListener(listener);
                needsSWDispMap = !rf.isFormatSupported(PixelFormat.FLOAT_XYZW);
                synchronized (this) {
                    state = OK;
                }
                return true;

            case OK:
            case LOST:
                return true;

            case DISPOSED:
            default:
                return false;
        }
    }

    @Override
    public PrDrawable createDrawable(RTTexture rtt) {
        if (!validate()) {
            return null;
        }
        return PPSDrawable.create(rtt);
    }

    @Override
    public AccelType getAccelType() {
        return shaderSource.getAccelType();
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
        // even if new peers are added from another thread while we're executing
        // this on the rendering thread, they won't have any native resources
        // since we're on the rendering thread, so no need to synchronize
        for (EffectPeer peer : getPeers()) {
            peer.dispose();
        }
        synchronized (this) {
            state = DISPOSED;
        }
        rf.removeFactoryListener(listener);
        rf = null;
        screen = null;
    }

    /**
     * Marks this renderer as lost.
     *
     * Warning: may be called on the rendering thread
     */
    protected final synchronized void markLost() {
        if (state == NOTREADY || state == OK) {
            state = LOST;
        }
    }

    @Override
    public int getCompatibleWidth(int w) {
        if (!validate()) {
            return -1;
        }
        return PPSDrawable.getCompatibleWidth(rf, w);
    }

    @Override
    public int getCompatibleHeight(int h) {
        if (!validate()) {
            return -1;
        }
        return PPSDrawable.getCompatibleHeight(rf, h);
    }

    @Override
    public final PPSDrawable createCompatibleImage(int w, int h) {
        if (!validate()) {
            return null;
        }
        return PPSDrawable.create(rf, w, h);
    }

    @Override
    public PPSDrawable getCompatibleImage(int w, int h) {
        if (!validate()) {
            return null;
        }
        PPSDrawable im = (PPSDrawable)super.getCompatibleImage(w, h);
        // either we ran out of vram or the device is lost
        if (im == null) {
            markLost();
        }
        return im;
    }

    @Override
    public LockableResource createFloatTexture(int w, int h) {
        if (!validate()) {
            return null;
        }
        Texture prismTex = rf.createFloatTexture(w, h);
        return new PrTexture(prismTex);
    }

    @Override
    public void updateFloatTexture(LockableResource texture, FloatMap map) {
        if (!validate()) {
            return;
        }
        FloatBuffer buf = map.getBuffer();
        int w = map.getWidth();
        int h = map.getHeight();
        Image img = Image.fromFloatMapData(buf, w, h);
        Texture prismTex = ((PrTexture)texture).getTextureObject();
        prismTex.update(img);
    }

    public Shader createShader(String name,
                               Map<String, Integer> samplers,
                               Map<String, Integer> params,
                               boolean isPixcoordUsed)
    {
        if (!validate()) {
            return null;
        }
        if (PrismSettings.verbose) {
            System.out.println("PPSRenderer: scenario.effect - createShader: " + name);
        }
        InputStream pscode = shaderSource.loadSource(name);
        int maxTexCoordIndex = samplers.keySet().size()-1;
        ShaderFactory factory = (ShaderFactory)rf;
        return factory.createShader(pscode, samplers, params,
                                    maxTexCoordIndex,
                                    isPixcoordUsed, false);
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
            peer = (EffectPeer)ctor.newInstance(new Object[]
                { fctx, this, name });
        } catch (Exception e) {
            return null;
        }
        return peer;
    }

    /**
     * Creates a new {@code EffectPeer} instance that is specific to
     * the current shader-based backend.
     *
     * @param fctx the filter context
     * @param name the name of the effect peer
     * @param unrollCount the unroll count
     * @return a new {@code EffectPeer} instance
     */
    private EffectPeer createPlatformPeer(FilterContext fctx, String name,
                                          int unrollCount)
    {
        EffectPeer peer;

        String shaderName = name;
        if (unrollCount > 0) {
            shaderName += "_" + unrollCount;
        }
        try {
            Class klass = Class.forName(rootPkg + ".impl.prism.ps.PPS" + name + "Peer");
            Constructor ctor = klass.getConstructor(new Class[]
                { FilterContext.class, Renderer.class, String.class });
            peer = (EffectPeer)ctor.newInstance(new Object[]
                { fctx, this, shaderName });
        } catch (Exception e) {
            System.err.println("Error: Prism peer not found for: " + name +
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
        } else if (needsSWDispMap && name.equals("DisplacementMap")) {
            PrFilterContext swctx = ((PrFilterContext) fctx).getSoftwareInstance();
            return new PPStoPSWDisplacementMapPeer(swctx, this, name);
        } else {
            // try creating a platform-specific peer
            return createPlatformPeer(fctx, name, unrollCount);
        }
    }

    @Override
    public boolean isImageDataCompatible(final ImageData id) {
        if (getRendererState() == OK) {
            Filterable f = id.getUntransformedImage();
            return (f instanceof PrDrawable && !f.isLost());
        }
        return false;
    }

    @Override
    public void clearImage(Filterable filterable) {
        PPSDrawable img = (PPSDrawable)filterable;
        img.clear();
    }

    @Override
    public ImageData createImageData(FilterContext fctx, Filterable src) {
        if (!validate()) {
            return null;
        }
        if (!(src instanceof PrImage)) {
            throw new IllegalArgumentException("Identity source must be PrImage");
        }
        Image img = ((PrImage)src).getImage();
        int w = img.getWidth();
        int h = img.getHeight();
        PPSDrawable dst = createCompatibleImage(w, h);
        if (dst == null) {
            return null;
        }
        // RT-27561
        // TODO: it is wasteful to create an RTT here; eventually it would
        // be nice if we could use plain Textures as a source Filterable...
        Graphics g = dst.createGraphics();
        ResourceFactory factory = g.getResourceFactory();
        Texture tex = factory.createTexture(img, Usage.DEFAULT,
                                            WrapMode.CLAMP_TO_EDGE);
        g.drawTexture(tex, 0, 0, w, h);
        // NOTE: calling sync() should not be required; ideally calling
        // Texture.dispose() would flush any pending operations that may
        // depend on that texture...
        g.sync();
        tex.dispose();
        BaseTransform tx;
        float ps = img.getPixelScale();
        if (ps != 1.0f) {
            ps = 1.0f / ps;
            tx = BaseTransform.getScaleInstance(ps, ps);
        } else {
            tx = BaseTransform.IDENTITY_TRANSFORM;
        }
        ImageData id = new ImageData(fctx, dst, new Rectangle(w, h), tx);
        return id;
    }

    @Override
    public Filterable transform(FilterContext fctx,
                                Filterable original,
                                BaseTransform transform,
                                Rectangle origBounds,
                                Rectangle xformBounds)
    {
        if (!validate()) {
            return null;
        }
        PPSDrawable dst = getCompatibleImage(xformBounds.width, xformBounds.height);
        if (dst != null) {
            Graphics g = dst.createGraphics();
            g.translate(-xformBounds.x, -xformBounds.y);
            g.transform(transform);
            g.drawTexture(((PPSDrawable)original).getTextureObject(),
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
        if (!validate()) {
            return null;
        }
        PPSDrawable dst = getCompatibleImage(xformBounds.width, xformBounds.height);
        if (dst != null) {
            PPSDrawable orig = (PPSDrawable)original.getUntransformedImage();
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

    private static ShaderSource createShaderSource(String name) {
        Class klass = null;
        try {
            klass = Class.forName(name);
            return (ShaderSource)klass.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            System.err.println(name + " class not found");
            return null;
        } catch (Throwable t) {
            //System.err.println("Error loading renderer:");
            //t.printStackTrace();
            return null;
        }
    }

    public static Renderer createRenderer(FilterContext fctx) {
        Object ref = fctx.getReferent();
        GraphicsPipeline pipe = GraphicsPipeline.getPipeline();
        if (pipe == null || !(ref instanceof Screen)) {
            return null;
        }
        Screen screen = (Screen)ref;
        ShaderSource shaderSource = null;
        if (pipe.supportsShader(ShaderType.HLSL, ShaderModel.SM3)) {
            shaderSource = createShaderSource(rootPkg + ".impl.hw.d3d.D3DShaderSource");
        } else if (pipe.supportsShader(ShaderType.GLSL, ShaderModel.SM3)) {
            shaderSource = createShaderSource(rootPkg + ".impl.es2.ES2ShaderSource");
        } else {
            throw new InternalError("Unknown GraphicsPipeline");
        }
        if (shaderSource == null) {
            return null;
        }
        return new PPSRenderer(screen, shaderSource);
    }
}
