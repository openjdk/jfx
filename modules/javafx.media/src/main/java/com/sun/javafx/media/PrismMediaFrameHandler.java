/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.media;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.WeakHashMap;
import com.sun.glass.ui.Screen;
import com.sun.javafx.tk.RenderJob;
import com.sun.javafx.tk.Toolkit;
import com.sun.media.jfxmedia.control.VideoDataBuffer;
import com.sun.media.jfxmedia.control.VideoFormat;
import com.sun.prism.Graphics;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.MediaFrame;
import com.sun.prism.PixelFormat;
import com.sun.prism.ResourceFactory;
import com.sun.prism.ResourceFactoryListener;
import com.sun.prism.Texture;

/**
 * Prism-specific subclass for handling media frames. The frames uploaded
 * into per-Screen textures, updated on every frame.
 *
 */
public class PrismMediaFrameHandler implements ResourceFactoryListener {
    private final Map<Screen, TextureMapEntry> textures = new WeakHashMap<>(1);

    private static Map<Object, PrismMediaFrameHandler> handlers;
    public synchronized static PrismMediaFrameHandler getHandler(Object provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider must be non-null");
        }
        if (handlers == null) {
            handlers = new WeakHashMap<>(1);
        }
        PrismMediaFrameHandler ret = handlers.get(provider);
        if (ret == null) {
            ret = new PrismMediaFrameHandler(provider);
            handlers.put(provider, ret);
        }
        return ret;
    }

    private WeakReference<ResourceFactory> registeredWithFactory = null;

    private PrismMediaFrameHandler(Object provider) {
    }


    /* NOTE: The following methods will only ever happen on one thread, so thread
     * safety should not be a concern here.
     */

    /**
     * This should only ever be called during a render cycle. Any other time it
     * will return null. Note that a returned texture should be unlocked when
     * the caller no longer needs it.
     *
     * @param g the Graphics context about to be rendered into
     * @return the current media texture valid for rendering into <code>g</code>
     * or null if called outside a render cycle
     */
    public Texture getTexture(Graphics g, VideoDataBuffer currentFrame) {
        Screen screen = g.getAssociatedScreen();
        TextureMapEntry tme = textures.get(screen);

        if (null == currentFrame) {
            // null frame, remove the existing texture
            if (textures.containsKey(screen)) {
                textures.remove(screen);
            }
            return null;
        }

        if (null == tme) {
            // we need to create a new texture for this graphics context
            tme = new TextureMapEntry();
            textures.put(screen, tme);
        }

        if (tme.texture != null) {
            tme.texture.lock();
            if (tme.texture.isSurfaceLost()) {
                tme.texture = null;
            }
        }

        // check if it needs to be updated
        if (null == tme.texture || tme.lastFrameTime != currentFrame.getTimestamp()) {
            updateTexture(g, currentFrame, tme);
        }

        return tme.texture;
    }

    private void updateTexture(Graphics g, VideoDataBuffer vdb, TextureMapEntry tme)
    {
        Screen screen = g.getAssociatedScreen();

        // reset texture if the encoded size changes
        if (tme.texture != null &&
            (tme.encodedWidth != vdb.getEncodedWidth() ||
             tme.encodedHeight != vdb.getEncodedHeight()))
        {
            tme.texture.dispose();
            tme.texture = null;
        }

        PrismFrameBuffer prismBuffer = new PrismFrameBuffer(vdb);
        if (tme.texture == null) {
            ResourceFactory factory = GraphicsPipeline.getDefaultResourceFactory();
            if (registeredWithFactory == null || registeredWithFactory.get() != factory) {
                // make sure we've registered with the resource factory so we know
                // when to purge old textures
                factory.addFactoryListener(this);
                registeredWithFactory = new WeakReference<>(factory);
            }

            tme.texture = GraphicsPipeline.getPipeline().
                getResourceFactory(screen).
                    createTexture(prismBuffer);
            tme.encodedWidth = vdb.getEncodedWidth();
            tme.encodedHeight = vdb.getEncodedHeight();
        }

        // upload frame data, check for null in case createTexture fails
        if (tme.texture != null) {
            tme.texture.update(prismBuffer, false);
        }
        tme.lastFrameTime = vdb.getTimestamp();
    }

    private void releaseData() {
        for (TextureMapEntry tme : textures.values()) {
            if (tme != null && tme.texture != null) {
                tme.texture.dispose();
            }
        }
        textures.clear();
    }

    private final RenderJob releaseRenderJob = new RenderJob(() -> {
        releaseData();
    });

    /**
     * Call this when you no longer need to render movie frames, for example
     * when playback stops.
     */
    public void releaseTextures() {
        Toolkit tk = Toolkit.getToolkit();
        tk.addRenderJob(releaseRenderJob);
    }

    @Override
    public void factoryReset() {
        releaseData();
    }

    @Override
    public void factoryReleased() {
        releaseData();
    }

    /**
     * Bridge class to avoid having to import JFXMedia into a bunch of prism
     * code.
     */
    private class PrismFrameBuffer implements MediaFrame {
        private final PixelFormat videoFormat;
        private final VideoDataBuffer primary;

        public PrismFrameBuffer(VideoDataBuffer sourceBuffer) {
            if (null == sourceBuffer) {
                throw new NullPointerException();
            }

            primary = sourceBuffer;
            switch (primary.getFormat()) {
                case BGRA_PRE:
                    videoFormat = PixelFormat.INT_ARGB_PRE;
                    break;
                case YCbCr_420p:
                    videoFormat = PixelFormat.MULTI_YCbCr_420;
                    break;
                case YCbCr_422:
                    videoFormat = PixelFormat.BYTE_APPLE_422;
                    break;
                // ARGB isn't supported in prism, there's no corresponding PixelFormat
                case ARGB:
                default:
                    throw new IllegalArgumentException("Unsupported video format "+primary.getFormat());
            }
        }

        @Override
        public ByteBuffer getBufferForPlane(int plane) {
            return primary.getBufferForPlane(plane);
        }

        @Override
        public void holdFrame() {
            primary.holdFrame();
        }

        @Override
        public void releaseFrame() {
            primary.releaseFrame();
        }

        @Override
        public PixelFormat getPixelFormat() {
            return videoFormat;
        }

        @Override
        public int getWidth() {
            return primary.getWidth();
        }

        @Override
        public int getHeight() {
            return primary.getHeight();
        }

        @Override
        public int getEncodedWidth() {
            return primary.getEncodedWidth();
        }

        @Override
        public int getEncodedHeight() {
            return primary.getEncodedHeight();
        }

        @Override
        public int planeCount() {
            return primary.getPlaneCount();
        }

        @Override
        public int[] planeStrides() {
            return primary.getPlaneStrides();
        }

        @Override
        public int strideForPlane(int planeIndex) {
            return primary.getStrideForPlane(planeIndex);
        }

        @Override
        public MediaFrame convertToFormat(PixelFormat fmt) {
            if (fmt == getPixelFormat()) {
                return this;
            }

            // This method only supports conversion to INT_ARGB_PRE currently
            if (fmt != PixelFormat.INT_ARGB_PRE) {
                return null;
            }

            VideoDataBuffer newVDB = primary.convertToFormat(VideoFormat.BGRA_PRE);
            if (null == newVDB) {
                return null;
            }
            return new PrismFrameBuffer(newVDB);
        }
    }

    private static class TextureMapEntry {
        public double lastFrameTime = -1; // used to determine if we need to update
        public Texture texture;
        public int encodedWidth;
        public int encodedHeight;
    }
}
