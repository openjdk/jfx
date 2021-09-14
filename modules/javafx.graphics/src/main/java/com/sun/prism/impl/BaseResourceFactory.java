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

package com.sun.prism.impl;

import com.sun.javafx.geom.Rectangle;
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

import javafx.util.Pair;

public abstract class BaseResourceFactory implements ResourceFactory {
    private final Map<Image,Texture> clampTexCache;
    private final Map<Image,Texture> repeatTexCache;
    // Solely used by diffuse and selfillum maps in PhongMaterial for 3D rendering
    private final Map<Image,Texture> mipmapTexCache;

    // Use a WeakHashMap as it automatically removes dead objects when they're
    // collected
    private final WeakHashMap<ResourceFactoryListener,Boolean> listenerMap =
            new WeakHashMap<ResourceFactoryListener,Boolean>();

    private boolean disposed = false;

    private Texture regionTexture;
    private Texture glyphTexture;
    private boolean superShaderAllowed;

    public BaseResourceFactory() {
        this(new WeakHashMap<Image,Texture>(),
             new WeakHashMap<Image,Texture>(),
             new WeakHashMap<Image,Texture>());
    }

    public BaseResourceFactory(Map<Image, Texture> clampTexCache,
                               Map<Image, Texture> repeatTexCache,
                               Map<Image, Texture> mipmapTexCache)
    {
        this.clampTexCache = clampTexCache;
        this.repeatTexCache = repeatTexCache;
        this.mipmapTexCache = mipmapTexCache;
    }

    @Override public void addFactoryListener(ResourceFactoryListener l) {
        listenerMap.put(l, Boolean.TRUE);
    }

    @Override public void removeFactoryListener(ResourceFactoryListener l) {
        // remove will return null if there is no mapping, so it's safe to call
        // with unregistered listeners
        listenerMap.remove(l);
    }

    @Override public boolean isDeviceReady() {
        return !isDisposed();
    }

    protected void clearTextureCache() {
        clearTextureCache(clampTexCache);
        clearTextureCache(repeatTexCache);
        clearTextureCache(mipmapTexCache);
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

    private void disposeResources() {
        clampTexCache.clear();
        repeatTexCache.clear();
        mipmapTexCache.clear();

        if (regionTexture != null) {
            regionTexture.dispose();
            regionTexture = null;
        }
        if (glyphTexture != null) {
            glyphTexture.dispose();
            glyphTexture = null;
        }
    }

    /**
     * Called when the factory is reset. Some resources (based in vram) could
     * be lost.
     */
    protected void notifyReset() {
        disposeResources();

        // Iterate over a *copy* of the key set because listeners may remove
        // themselves during the callback
        ResourceFactoryListener[] notifyList = getFactoryListeners();
        for (ResourceFactoryListener listener : notifyList) {
            if (null != listener) {
                listener.factoryReset();
            }
        }
    }

    @Override
    public void dispose() {
        disposeResources();
        disposed = true;

        // Iterate over a *copy* of the key set because listeners may remove
        // themselves during the callback
        ResourceFactoryListener[] notifyList = getFactoryListeners();
        for (ResourceFactoryListener listener : notifyList) {
            if (null != listener) {
                listener.factoryReleased();
            }
        }
    }

    static long sizeWithMipMap(int w, int h, PixelFormat format) {
        long size = 0;
        int bytesPerPixel = format.getBytesPerPixelUnit();
        while (w > 1 && h > 1) {
            size += ((long) w) * ((long) h);
            w = (w + 1) >> 1;
            h = (h + 1) >> 1;
        }
        size += 1;
        return size * bytesPerPixel;
    }

    @Override
    public Texture getCachedTexture(Image image, WrapMode wrapMode) {
        if (checkDisposed()) return null;

       return  getCachedTexture(image, wrapMode, false);
    }

    @Override
    public Texture getCachedTexture(Image image, WrapMode wrapMode, boolean useMipmap) {
        if (checkDisposed()) return null;

        if (image == null) {
            throw new IllegalArgumentException("Image must be non-null");
        }
        Map<Image,Texture> texCache;
        if (wrapMode == WrapMode.CLAMP_TO_EDGE) {
            // Mipmap not supported with CLAMP mode in current implementation
            if (useMipmap) {
                throw new IllegalArgumentException("Mipmap not supported with CLAMP mode: useMipmap = "
                        + useMipmap + ", wrapMode = " + wrapMode);
            }
            texCache = clampTexCache;
        } else if (wrapMode == WrapMode.REPEAT) {
            texCache = useMipmap ? mipmapTexCache : repeatTexCache;
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

         // Doesn't apply if useMipmap is true
         if (!useMipmap && tex == null) {
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
                        texCache.put(image, tex);
                    }
                }
                othertex.unlock();
            }
        }

        Pair <Integer, Rectangle> idRect = image.getSerial().getIdRect();
        if (tex == null) {
            int w = image.getWidth();
            int h = image.getHeight();
            TextureResourcePool pool = getTextureResourcePool();
            // Mipmap will use more memory
            long size = useMipmap ? sizeWithMipMap(w, h, image.getPixelFormat())
                    : pool.estimateTextureSize(w, h, image.getPixelFormat());
            if (!pool.prepareForAllocation(size)) {
                return null;
            }

            tex = createTexture(image, Usage.DEFAULT, wrapMode, useMipmap);
            if (tex != null) {
                tex.setLastImageSerial(idRect.getKey());
                texCache.put(image, tex);
            }
        } else if (tex.getLastImageSerial() != idRect.getKey()) {
            // If the image was updated only once, then the image is partially updated.
            // Else whole image is updated.
            if (idRect.getKey() - tex.getLastImageSerial() == 1 && idRect.getValue() != null) {
                Rectangle dirtyRect = idRect.getValue();
                tex.update(image.getPixelBuffer(), image.getPixelFormat(),
                        dirtyRect.x, dirtyRect.y, dirtyRect.x, dirtyRect.y,
                        dirtyRect.width, dirtyRect.height,
                        image.getScanlineStride(), false);
            } else {
                tex.update(image, 0, 0, image.getWidth(), image.getHeight(), false);
            }
            tex.setLastImageSerial(idRect.getKey());
        }
        return tex;
    }

    @Override
    public Texture createTexture(Image image, Usage usageHint, WrapMode wrapMode) {
        if (checkDisposed()) return null;

        return createTexture(image, usageHint, wrapMode, false);
    }

    @Override
    public Texture createTexture(Image image, Usage usageHint, WrapMode wrapMode,
            boolean useMipmap) {

        if (checkDisposed()) return null;

        PixelFormat format = image.getPixelFormat();
        int w = image.getWidth();
        int h = image.getHeight();

        Texture tex = createTexture(format, usageHint, wrapMode, w, h, useMipmap);
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

    @Override
    public void setRegionTexture(Texture texture) {
        if (checkDisposed()) return;

        regionTexture = texture;
        superShaderAllowed = PrismSettings.superShader &&
                             regionTexture != null &&
                             glyphTexture != null;
    }

    @Override
    public Texture getRegionTexture() {
        return regionTexture;
    }

    @Override
    public void setGlyphTexture(Texture texture) {
        if (checkDisposed()) return;

        glyphTexture = texture;
        superShaderAllowed = PrismSettings.superShader &&
                             regionTexture != null &&
                             glyphTexture != null;
    }

    @Override
    public Texture getGlyphTexture() {
        return glyphTexture;
    }

    @Override
    public boolean isSuperShaderAllowed() {
        return superShaderAllowed;
    }

    protected boolean canClampToZero() {
        return true;
    }

    protected boolean canClampToEdge() {
        return true;
    }

    protected boolean canRepeat() {
        return true;
    }

    @Override
    public boolean isWrapModeSupported(WrapMode mode) {
        switch (mode) {
            case CLAMP_NOT_NEEDED:
                return true;
            case CLAMP_TO_EDGE:
                return canClampToEdge();
            case REPEAT:
                return canRepeat();
            case CLAMP_TO_ZERO:
                return canClampToZero();
            case CLAMP_TO_EDGE_SIMULATED:
            case CLAMP_TO_ZERO_SIMULATED:
            case REPEAT_SIMULATED:
                throw new InternalError("Cannot test support for simulated wrap modes");
            default:
                throw new InternalError("Unrecognized wrap mode: "+mode);
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    protected boolean checkDisposed() {
        if (PrismSettings.verbose && isDisposed()) {
            try {
                throw new IllegalStateException("attempt to use resource after factory is disposed");
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }

        return isDisposed();
    }

}
