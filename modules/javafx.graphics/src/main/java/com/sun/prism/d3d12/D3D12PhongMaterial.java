/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.d3d12;

import com.sun.prism.Image;
import com.sun.prism.PhongMaterial;
import com.sun.prism.Texture;
import com.sun.prism.TextureMap;
import com.sun.prism.impl.BasePhongMaterial;
import com.sun.prism.impl.Disposer;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.prism.d3d12.ni.D3D12NativePhongMaterial;
import com.sun.prism.d3d12.ni.D3D12NativeTexture;

class D3D12PhongMaterial extends BasePhongMaterial {

    private final D3D12Context mContext;
    private final D3D12PhongMaterialData mMaterialData;
    private final TextureMap maps[] = new TextureMap[MAX_MAP_TYPE];

    private D3D12PhongMaterial(D3D12Context context, D3D12PhongMaterialData materialData) {
        super(materialData);
        this.mContext = context;
        this.mMaterialData = materialData;
    }

    static public D3D12PhongMaterial create(D3D12Context context) {
        return new D3D12PhongMaterial(context,
                                      new D3D12PhongMaterialData(
                                        context.getDevice().createPhongMaterial()
                                        )
                                     );
    }

    D3D12NativePhongMaterial getNative() {
        return mMaterialData.getNative();
    }

    @Override
    public void setDiffuseColor(float r, float g, float b, float a) {
        mMaterialData.getNative().setDiffuseColor(r, g, b, a);
    }

    @Override
    public void setSpecularColor(boolean set, float r, float g, float b, float a) {
        mMaterialData.getNative().setSpecularColor(set, r, g, b, a);
    }

    @Override
    public void setTextureMap(TextureMap map) {
        maps[map.getType().ordinal()] = map;
    }

    private Texture setupTexture(TextureMap map, boolean useMipmap) {
        Image image = map.getImage();
        Texture texture = (image == null) ? null
                : mContext.getResourceFactory().getCachedTexture(image, Texture.WrapMode.REPEAT, useMipmap);
        D3D12NativeTexture nativeTexture = (texture == null) ? null : ((D3D12Texture)texture).getNativeTexture();
        mMaterialData.getNative().setTextureMap(nativeTexture, map.getType());
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
            // Enable mipmap if map is diffuse or self illum.
            boolean useMipmap = (i == PhongMaterial.DIFFUSE) || (i == PhongMaterial.SELF_ILLUM);
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
    public void dispose() {
        disposerRecord.dispose();
    }

    static class D3D12PhongMaterialData implements Disposer.Record {

        D3D12NativePhongMaterial mMaterial;

        D3D12PhongMaterialData(D3D12NativePhongMaterial material) {
            if (!material.isValid()) {
                throw new NullPointerException("Phong material object is NULL");
            }

            this.mMaterial = material;
        }

        D3D12NativePhongMaterial getNative() {
            return mMaterial;
        }

        @Override
        public void dispose() {
            if (mMaterial.isValid()) {
                mMaterial.dispose();
            }
        }
    }
}
