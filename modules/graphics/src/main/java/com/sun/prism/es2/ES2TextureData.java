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

package com.sun.prism.es2;

import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.Disposer;
import com.sun.prism.impl.PrismTrace;

class ES2TextureData implements Disposer.Record {
    protected final ES2Context context;
    private int texID;
    private long size;

    private boolean lastAssociatedFilterMode = true;
    private WrapMode lastAssociatedWrapMode = WrapMode.REPEAT;

    // Caller must call PrismTrace.somethingCreated(...);
    protected ES2TextureData(ES2Context context, int texID, long size) {
        this.context = context;
        this.texID = texID;
        this.size = size;
    }

    ES2TextureData(ES2Context context, int texID, int w, int h, long size) {
        this.context = context;
        this.texID = texID;
        this.size = size;
        PrismTrace.textureCreated(texID, w, h, size);
    }

    public int getTexID() {
        return texID;
    }

    public long getSize() {
        return size;
    }

    public boolean isFiltered() {
        return lastAssociatedFilterMode;
    }

    public void setFiltered(boolean filterMode) {
        this.lastAssociatedFilterMode = filterMode;
    }

    public WrapMode getWrapMode() {
        return lastAssociatedWrapMode;
    }

    public void setWrapMode(WrapMode wrapMode) {
        this.lastAssociatedWrapMode = wrapMode;
    }

    void traceDispose() {
        PrismTrace.textureDisposed(texID);
    }

    public void dispose() {
        if (texID != 0) {
            traceDispose();
            GLContext glCtx = context.getGLContext();
            //context.disposeTexture(texID);
            // 4 is the number of textures track in BaseShaderContext
            for (int i = 0; i < glCtx.getNumBoundTexture(); i++) {
                if (texID == glCtx.getBoundTexture(i)) {
                    // flush the vertex buffer in case any pending operations
                    // depend on the texture that is about to be diposed
                    context.flushVertexBuffer();
                    // the given texture is currently bound, so unbind it now
                    glCtx.updateActiveTextureUnit(i);
                    glCtx.setBoundTexture(0);
                }
            }
            // delete the texture
            glCtx.deleteTexture(texID);
            this.texID = 0;
        }
    }
}
