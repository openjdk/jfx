/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.image;

import com.sun.prism.*;

public class CompoundTexture extends CompoundImage implements GraphicsResource {

    protected Texture texTiles[];

    public CompoundTexture(Image image, int maxSize) {
        super(image, maxSize);
        texTiles = new Texture[tiles.length];
    }

    @Override
    public Texture getTile(int x, int y, ResourceFactory factory) {
        int idx = x + y*uSections;
        Texture tex = texTiles[idx];
        if (tex != null) {
            tex.lock();
            if (tex.isSurfaceLost()) {
                texTiles[idx] = tex = null;
            }
        }
        if (tex == null) {
            tex = factory.createTexture(tiles[idx],
                                        Texture.Usage.STATIC,
                                        Texture.WrapMode.CLAMP_TO_EDGE);
            texTiles[idx] = tex;
        }
        return tex;
    }

    @Override
    public void dispose() {
        for (int i = 0; i != texTiles.length; ++i) {
            if (texTiles[i] != null) {
                texTiles[i].dispose();
                texTiles[i] = null;
            }
        }
    }

}
