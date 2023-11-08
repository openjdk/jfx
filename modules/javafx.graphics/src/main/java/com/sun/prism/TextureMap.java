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

package com.sun.prism;

import com.sun.javafx.scene.paint.TextureData;

/**
 * A wrapper class to hold map related information for the PhongMaterial
 */
public class TextureMap {

    private final PhongMaterial.MapType type;
    private Image image;
    private TextureData textureData;
    /**
     * The texture resource is requested from the resource manager with the image (and maybe later textureData) as
     * arguments. It's not taken from the material like image, textureData, and type are. We could consider some
     * refactoring in the texture area (texture could be removed from this class, or the whole class might be removed).
     */
    private Texture texture;
    private boolean dirty;

    public TextureMap(PhongMaterial.MapType type) {
        this.type = type;
    }

    public PhongMaterial.MapType getType() {
        return type;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public Texture getTexture() {
        return texture;
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    public TextureData getTextureData() {
        return textureData;
    }

    public void setTextureData(TextureData textureData) {
        this.textureData = textureData;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
