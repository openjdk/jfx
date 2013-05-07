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

package com.sun.javafx.sg.prism;

import com.sun.javafx.sg.PGPhongMaterial;
import com.sun.prism.Image;
import com.sun.prism.TextureMap;
import com.sun.prism.Material;
import com.sun.prism.PhongMaterial;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.paint.Color;

/**
 * TODO: 3D - Need documentation
 */
public class NGPhongMaterial implements PGPhongMaterial {

    private PhongMaterial material;

    private Color diffuseColor;
    private boolean diffuseColorDirty = true;
    private TextureMap diffuseMap = new TextureMap(PhongMaterial.MapType.DIFFUSE);

    private Color specularColor;
    private boolean specularColorDirty = true;
    private float specularPower;
    private boolean specularPowerDirty = true;
    private TextureMap specularMap = new TextureMap(PhongMaterial.MapType.SPECULAR);

    private TextureMap bumpMap = new TextureMap(PhongMaterial.MapType.BUMP);

    private TextureMap selfIllumMap = new TextureMap(PhongMaterial.MapType.SELF_ILLUM);

    Material createMaterial(ResourceFactory f) {
        if (material == null) {
            material = f.createPhongMaterial();
        }
        validate(f);
        return material;
    }

    private static Texture getCachedTexture(ResourceFactory f, Image img) {
        return img != null ? f.getCachedTexture(img, Texture.WrapMode.CLAMP_TO_EDGE) : null;
    }

    private void validate(ResourceFactory f) {

        if (diffuseColorDirty) {
            if (diffuseColor != null) {
                material.setSolidColor(
                        diffuseColor.getRed(), diffuseColor.getGreen(),
                        diffuseColor.getBlue(), diffuseColor.getAlpha());
            } else {
                material.setSolidColor(0, 0, 0, 0);
            }
            diffuseColorDirty = false;
        }

        if (diffuseMap.isDirty()) {
            if (diffuseMap.getImage() == null) { 
                int pixel = 0xffffffff;
                diffuseMap.setImage(Image.fromIntArgbPreData( new int[]{pixel}, 1, 1));
            }
            diffuseMap.setTexture(getCachedTexture(f, diffuseMap.getImage()));
            material.setTextureMap(diffuseMap);
            diffuseMap.setDirty(false);
        }
        if (bumpMap.isDirty()) {
            bumpMap.setTexture(getCachedTexture(f, bumpMap.getImage()));
            material.setTextureMap(bumpMap);
            bumpMap.setDirty(false);
        }
        if (selfIllumMap.isDirty()) {
            selfIllumMap.setTexture(getCachedTexture(f, selfIllumMap.getImage()));
            material.setTextureMap(selfIllumMap);
            selfIllumMap.setDirty(false);
        }
        if (specularMap.isDirty() || specularColorDirty || specularPowerDirty) {
            Image specular = specularMap.getImage();
            if (specular == null && specularColor != null) {
                int ia = (int) (255.0 * specularPower);
                int ir = (int) (255.0 * specularColor.getRed());
                int ig = (int) (255.0 * specularColor.getGreen());
                int ib = (int) (255.0 * specularColor.getBlue());
                int pixel = (ia << 24) | (ir << 16) | (ig << 8) | (ib << 0);

                if (ir != 0 || ig != 0 || ib != 0) {
                    specular = Image.fromIntArgbPreData(new int[]{pixel}, 1, 1);
                }
            }
            specularMap.setTexture(getCachedTexture(f, specular));
            material.setTextureMap(specularMap);
            specularColorDirty = false;
            specularPowerDirty = false;
            specularMap.setDirty(false);
        }
    }

    public void setDiffuseColor(Object diffuseColor) {
        this.diffuseColor = (Color)diffuseColor;
        diffuseColorDirty = true;
    }

    public void setSpecularColor(Object specularColor) {
        this.specularColor = (Color)specularColor;
        specularColorDirty = true;
    }

    public void setSpecularPower(float specularPower) {
        this.specularPower = specularPower;
        specularPowerDirty = true;
    }

    public void setDiffuseMap(Object diffuseMap) {
        this.diffuseMap.setImage((Image)diffuseMap);
        this.diffuseMap.setDirty(true);
    }

    public void setSpecularMap(Object specularMap) {
        this.specularMap.setImage((Image)specularMap);
        this.specularMap.setDirty(true);
    }

    public void setBumpMap(Object bumpMap) {
        this.bumpMap.setImage((Image)bumpMap);
        this.bumpMap.setDirty(true);
    }

    public void setSelfIllumMap(Object selfIllumMap) {
        this.selfIllumMap.setImage((Image)selfIllumMap);
        this.selfIllumMap.setDirty(true);
    }

    // NOTE: This method is used for unit test purpose only.
    Color test_getDiffuseColor() {
        return diffuseColor;
    }
}
