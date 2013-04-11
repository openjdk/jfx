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
import com.sun.prism.Material;
import com.sun.prism.PhongMaterial;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;
import com.sun.prism.paint.Color;

/**
 * TODO: 3D - Need documentation
 */
public class NGPhongMaterial implements PGPhongMaterial {

    private Color diffuseColor;
    private Color specularColor;
    private float specularPower;
    private Image diffuseMap;
    private Image specularMap;
    private Image bumpMap;
    private Image selfIlluminationMap;
    private PhongMaterial material;
    private boolean diffuseColorDirty = true;
    private boolean specularColorDirty = true;
    private boolean specularPowerDirty = true;
    private boolean diffuseMapDirty = true;
    private boolean specularMapDirty = true;
    private boolean bumpMapDirty = true;
    private boolean selfIlluminationMapDirty = true;

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

        if (diffuseMapDirty) {
            material.setMap(PhongMaterial.MapType.DIFFUSE, getCachedTexture(f, diffuseMap));
            diffuseMapDirty = false;
        }
        if (bumpMapDirty) {
            material.setMap(PhongMaterial.MapType.BUMP, getCachedTexture(f, bumpMap));
            bumpMapDirty = false;
        }
        if (selfIlluminationMapDirty) {
            material.setMap(PhongMaterial.MapType.SELF_ILLUM, getCachedTexture(f, selfIlluminationMap));
            selfIlluminationMapDirty = false;
        }
        if (specularMapDirty || specularColorDirty || specularPowerDirty) {
            Image specular = specularMap;
            if (specular == null && specularColor != null) {
                int ia = (int) (255.0 * specularPower);
                int ir = (int) (255.0 * specularColor.getRed());
                int ig = (int) (255.0 * specularColor.getGreen());
                int ib = (int) (255.0 * specularColor.getBlue());
                int pixel = (ia << 24) | (ir << 16) | (ig << 8) | (ib << 0);

                if (ir != 0 || ig != 0 || ib != 0) {
                    specular = Image.fromIntArgbPreData(
                            new int[]{pixel, pixel, pixel, pixel}, 2, 2);
                }
            }
            material.setMap(PhongMaterial.MapType.SPECULAR, getCachedTexture(f, specular));
            specularColorDirty = false;
            specularPowerDirty = false;
            specularMapDirty = false;
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
        this.diffuseMap = (Image)diffuseMap;
        diffuseMapDirty = true;
    }

    public void setSpecularMap(Object specularMap) {
        this.specularMap = (Image)specularMap;
        specularMapDirty = true;
    }

    public void setBumpMap(Object bumpMap) {
        this.bumpMap = (Image)bumpMap;
        bumpMapDirty = true;
    }

    public void setSelfIlluminationMap(Object selfIlluminationMap) {
        this.selfIlluminationMap = (Image)selfIlluminationMap;
        selfIlluminationMapDirty = true;
    }

    // NOTE: This method is used for unit test purpose only.
    Color test_getDiffuseColor() {
        return diffuseColor;
    }
}
