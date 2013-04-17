/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl;

import com.sun.prism.Image;
import com.sun.prism.PixelFormat;
import com.sun.prism.ResourceFactory;
import com.sun.prism.ResourceFactoryListener;
import com.sun.prism.Texture;
import com.sun.prism.Texture.Usage;
import com.sun.prism.Texture.WrapMode;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Collection;

public abstract class BaseResourceFactory implements ResourceFactory {

    private static final Map<Image,Texture> clampTexCache =
        new WeakHashMap<Image,Texture>();
    private static final Map<Image,Texture> repeatTexCache =
        new WeakHashMap<Image,Texture>();

    // Use a WeakHashMap as it automatically removes dead objects when they're
    // collected
    private final WeakHashMap<ResourceFactoryListener,Boolean> listenerMap =
            new WeakHashMap<ResourceFactoryListener,Boolean>();

    @Override public void addFactoryListener(ResourceFactoryListener l) {
        listenerMap.put(l, Boolean.TRUE);
    }

    @Override public void removeFactoryListener(ResourceFactoryListener l) {
        // remove will return null if there is no mapping, so it's safe to call
        // with unregistered listeners
        listenerMap.remove(l);
    }

    @Override public boolean isDeviceReady() {
        return true;
    }

    protected void clearTextureCache() {
        clearTextureCache(clampTexCache);
        clearTextureCache(repeatTexCache);
    }

    protected void clearTextureCache(Map<Image,Texture> texCache) {
        Collection<Texture> texAll = texCache.values();
        for (Texture i : texAll) {
            i.dispose();
        }
        texCache.clear();
    }

    protected ResourceFactoryListener[] getFactoryListeners() {
        return listenerMap.keySet().toArray(new ResourceFactoryListener[0]);
    }

    /**
     * Called when the factory is reset. Some resources (based in vram) could
     * be lost.
     */
    protected void notifyReset() {
        clampTexCache.clear();
        repeatTexCache.clear();

        // Iterate over a *copy* of the key set because listeners may remove
        // themselves during the callback
        ResourceFactoryListener[] notifyList = getFactoryListeners();
        for (ResourceFactoryListener listener : notifyList) {
            if (null != listener) {
                listener.factoryReset();
            }
        }
    }

    /**
     * Called when the factory's data is released
     */
    protected void notifyReleased() {
        clampTexCache.clear();
        repeatTexCache.clear();

        // Iterate over a *copy* of the key set because listeners may remove
        // themselves during the callback
        ResourceFactoryListener[] notifyList = getFactoryListeners();
        for (ResourceFactoryListener listener : notifyList) {
            if (null != listener) {
                listener.factoryReleased();
            }
        }
    }

    @Override
    public Texture getCachedTexture(Image image, WrapMode wrapMode) {
        if (image == null) {
            throw new IllegalArgumentException("Image must be non-null");
        }
        Map<Image,Texture> texCache;
        if (wrapMode == WrapMode.CLAMP_TO_EDGE) {
            texCache = clampTexCache;
        } else if (wrapMode == WrapMode.REPEAT) {
            texCache = repeatTexCache;
        } else {
            throw new IllegalArgumentException("no caching for "+wrapMode);
        }
         Texture tex = texCache.get(image);
         if (tex != null) {
             tex.lock();
             if (tex.isSurfaceLost()) {
                 texCache.remove(image);
                 tex = null;
             }
         }
         int serial = image.getSerial();
         if (tex == null) {
            // Try to share a converted texture from the other cache
            Texture othertex = (wrapMode == WrapMode.REPEAT
                   ? clampTexCache
                   : repeatTexCache).get(image);
            if (othertex != null) {
                othertex.lock();
                if (!othertex.isSurfaceLost()) {
                    // This conversion operation will fail if the texture is
                    // _SIMULATED
                    tex = othertex.getSharedTexture(wrapMode);
                    if (tex != null) {
                        // Technically, our shared texture will maintain that
                        // the contents are useful, but for completeness we
                        // will register both references as "useful"
                        tex.contentsUseful();
                        texCache.put(image, othertex);
                    }
                }
                othertex.unlock();
            }
        }
        if (tex == null) {
            int w = image.getWidth();
            int h = image.getHeight();
            TextureResourcePool pool = getTextureResourcePool();
            long size = pool.estimateTextureSize(w, h, image.getPixelFormat());
            if (!pool.prepareForAllocation(size)) {
                return null;
            }
            tex = createTexture(image, Usage.DEFAULT, wrapMode);
            if (tex != null) {
                tex.setLastImageSerial(serial);
                texCache.put(image, tex);
            }
        } else if (tex.getLastImageSerial() != serial) {
            tex.update(image, 0, 0, image.getWidth(), image.getHeight(), false);
            tex.setLastImageSerial(serial);
        }
        return tex;
    }

    @Override
    public Texture createTexture(Image image,
                                 Usage usageHint,
                                 WrapMode wrapMode)
    {
        PixelFormat format = image.getPixelFormat();
        int w = image.getWidth();
        int h = image.getHeight();
        Texture tex = createTexture(format, usageHint, wrapMode, w, h);
        // creation of a texture does not require flushing the vertex buffer
        // since there are no pending vertices that depend on this new texture,
        // so pass skipFlush=true here...
        if (tex != null) {
            tex.update(image, 0, 0, w, h, true);
            tex.contentsUseful();
        }
        return tex;
    }

    @Override
    public Texture createMaskTexture(int width, int height, WrapMode wrapMode) {
        return createTexture(PixelFormat.BYTE_ALPHA,
                             Usage.DEFAULT, wrapMode,
                             width, height);
    }

    @Override
    public Texture createFloatTexture(int width, int height) {
        return createTexture(PixelFormat.FLOAT_XYZW,
                             Usage.DEFAULT, WrapMode.CLAMP_TO_ZERO,
                             width, height);
    }

    }
