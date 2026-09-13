/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.mtl;

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.prism.Image;
import com.sun.prism.PhongMaterial;
import com.sun.prism.Texture;
import com.sun.prism.TextureMap;
import com.sun.prism.impl.BasePhongMaterial;
import com.sun.prism.impl.Disposer;

class MTLPhongMaterial extends BasePhongMaterial {

    static int count = 0;

    private final MTLContext context;
    private final long nativeHandle;
    private final TextureMap maps[] = new TextureMap[MAX_MAP_TYPE];

    private MTLPhongMaterial(MTLContext context, long nativeHandle,
            Disposer.Record disposerRecord) {
        super(disposerRecord);
        this.context = context;
        this.nativeHandle = nativeHandle;
        count++;
    }

    static MTLPhongMaterial create(MTLContext context) {
        long nativeHandle = context.createMTLPhongMaterial();
        return new MTLPhongMaterial(context, nativeHandle, new MTLPhongMaterialDisposerRecord(context, nativeHandle));
    }

    long getNativeHandle() {
        return nativeHandle;
    }

    @Override
    public void setDiffuseColor(float r, float g, float b, float a) {
        context.setDiffuseColor(nativeHandle, r, g, b, a);
    }

    @Override
    public void setSpecularColor(boolean set, float r, float g, float b, float a) {
        context.setSpecularColor(nativeHandle, set, r, g, b, a);
    }

    @Override
    public void setTextureMap(TextureMap map) {
        maps[map.getType().ordinal()] = map;
    }

    private Texture setupTexture(TextureMap map, boolean useMipmap) {
        Image image = map.getImage();
        Texture texture = (image == null) ? null
                : context.getResourceFactory().getCachedTexture(image, Texture.WrapMode.REPEAT, useMipmap);
        long hTexture = 0;
        if (texture instanceof MTLTexture mtlTex) {
            hTexture = mtlTex.getNativeHandle();
        }
        context.setMap(nativeHandle, map.getType().ordinal(), hTexture);
        return texture;
    }

    @Override
    public void lockTextureMaps() {
        for (int i = 0; i < MAX_MAP_TYPE; i++) {
            Texture texture = maps[i].getTexture();
            if (!maps[i].isDirty() && texture != null) {
                texture.lock();
                if (!texture.isSurfaceLost()) {
                    continue;
                }
            }
            // Enable mipmap if platform isn't embedded and map is diffuse or self illum
            boolean useMipmap = (!PlatformUtil.isEmbedded()) && (i == PhongMaterial.DIFFUSE || i == PhongMaterial.SELF_ILLUM);
            texture = setupTexture(maps[i], useMipmap);
            maps[i].setTexture(texture);
            maps[i].setDirty(false);
            if (maps[i].getImage() != null && texture == null) {
                String logname = PhongMaterial.class.getName();
                PlatformLogger.getLogger(logname).warning(
                        "Warning: Low on texture resources. Cannot create texture.");
            }
        }
    }

    @Override
    public void unlockTextureMaps() {
        for (int i = 0; i < MAX_MAP_TYPE; i++) {
            Texture texture = maps[i].getTexture();
            if (texture != null) {
                texture.unlock();
            }
        }
    }

    @Override
    public boolean isValid() {
        return !context.isDisposed();
    }

    @Override
    public void dispose() {
        disposerRecord.dispose();
        count--;
    }

    public int getCount() {
        return count;
    }

    private static class MTLPhongMaterialDisposerRecord implements Disposer.Record {

        private final MTLContext context;
        private long nativeHandle;

        MTLPhongMaterialDisposerRecord(MTLContext context, long nativeHandle) {
            this.context = context;
            this.nativeHandle = nativeHandle;
        }

        void traceDispose() {}

        @Override
        public void dispose() {
            if (nativeHandle != 0L && !context.isDisposed()) {
                traceDispose();
                context.releaseMTLPhongMaterial(nativeHandle);
                nativeHandle = 0L;
            }
        }
    }
}
