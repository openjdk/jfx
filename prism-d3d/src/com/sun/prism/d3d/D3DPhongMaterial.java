/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.d3d;

import com.sun.prism.PhongMaterial;
import com.sun.prism.Texture;
import com.sun.prism.TextureMap;
import com.sun.prism.impl.BaseGraphicsResource;
import com.sun.prism.impl.Disposer;

/**
 * TODO: 3D - Need documentation
 */
class D3DPhongMaterial extends BaseGraphicsResource implements PhongMaterial {

    static int count = 0;

    private final D3DContext context;
    private final long nativeHandle;
    private TextureMap maps[] = new TextureMap[MAX_MAP_TYPE];
    
    private D3DPhongMaterial(D3DContext context, long nativeHandle,
            Disposer.Record disposerRecord) {
        super(disposerRecord);
        this.context = context;
        this.nativeHandle = nativeHandle;
        count++;
    }

    static D3DPhongMaterial create(D3DContext context) {
        long nativeHandle = context.createD3DPhongMaterial();
        return new D3DPhongMaterial(context, nativeHandle, new D3DPhongMaterialDisposerRecord(context, nativeHandle));
    }

    long getNativeHandle() {
        return nativeHandle;
    }

    public void setSolidColor(float r, float g, float b, float a) {
        context.setSolidColor(nativeHandle, r, g, b, a);
    }

    public void setTextureMap(TextureMap map) {
        boolean isSpecularAlpha = false;
        boolean isBumpAlpha = false;
        switch (map.getType()) {
            case SPECULAR:
                isSpecularAlpha = map.getTexture() == null ?
                        false : !map.getTexture().getPixelFormat().isOpaque();
                maps[SPECULAR] = map;
                break;
            case BUMP:
                isBumpAlpha = map.getTexture() == null ?
                        false : !map.getTexture().getPixelFormat().isOpaque();
                maps[BUMP] = map;
                break;
            case DIFFUSE:
                maps[DIFFUSE] = map;
                break;
            case SELF_ILLUM:
                maps[SELF_ILLUM] = map;
                break;
            default:
                // NOP
        }
        long hTexture;
        Texture texture = map.getTexture();
        if (texture != null) {
            // TODO: 3D - This is a workaround only. See JIRA RT-29543 for detail.
            texture.contentsUseful();
            texture.makePermanent();

            hTexture = ((D3DTexture) texture).getNativeTextureObject();
        } else {
            hTexture = 0;
        }
        context.setMap(nativeHandle, map.getType().ordinal(),
                hTexture, isSpecularAlpha, isBumpAlpha);
    }

    @Override
    public void dispose() {
        disposerRecord.dispose();
        count--;
    }

    public int getCount() {
        return count;
    }

    static class D3DPhongMaterialDisposerRecord implements Disposer.Record {

        private final D3DContext context;
        private long nativeHandle;

        D3DPhongMaterialDisposerRecord(D3DContext context, long nativeHandle) {
            this.context = context;
            this.nativeHandle = nativeHandle;
        }

        void traceDispose() {
        }

        public void dispose() {
            if (nativeHandle != 0L) {
                traceDispose();
                context.releaseD3DPhongMaterial(nativeHandle);
                nativeHandle = 0L;
            }
        }
    }
}
