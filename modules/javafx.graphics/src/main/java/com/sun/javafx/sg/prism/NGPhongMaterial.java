/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.prism.Image;
import com.sun.prism.Material;
import com.sun.prism.PhongMaterial;
import com.sun.prism.ResourceFactory;
import com.sun.prism.TextureMap;
import com.sun.prism.paint.Color;

/**
 * TODO: 3D - Need documentation
 */
public class NGPhongMaterial {

    private static final Image WHITE_1X1 = Image.fromIntArgbPreData(new int[]{0xffffffff}, 1, 1);
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

        // Check whether the material is valid; dispose and recreate if needed
        if (material != null && !material.isValid()) {
            disposeMaterial();
        }

        if (material == null) {
            material = f.createPhongMaterial();
        }
        validate(f);
        return material;
    }

    private void disposeMaterial() {
        diffuseColorDirty = true;
        specularColorDirty = true;
        specularPowerDirty = true;
        diffuseMap.setDirty(true);
        specularMap.setDirty(true);
        bumpMap.setDirty(true);
        selfIllumMap.setDirty(true);

        material.dispose();
        material = null;
    }

    private void validate(ResourceFactory f) {

        if (diffuseColorDirty) {
            if (diffuseColor != null) {
                material.setDiffuseColor(
                        diffuseColor.getRed(), diffuseColor.getGreen(),
                        diffuseColor.getBlue(), diffuseColor.getAlpha());
            } else {
                material.setDiffuseColor(0, 0, 0, 0);
            }
            diffuseColorDirty = false;
        }

        if (diffuseMap.isDirty()) {
            if (diffuseMap.getImage() == null) {
                diffuseMap.setImage(WHITE_1X1);
            }
            material.setTextureMap(diffuseMap);
        }
        if (bumpMap.isDirty()) {
            material.setTextureMap(bumpMap);
        }
        if (selfIllumMap.isDirty()) {
            material.setTextureMap(selfIllumMap);
        }
        if (specularMap.isDirty()) {
            material.setTextureMap(specularMap);
        }
        if (specularColorDirty || specularPowerDirty) {
            if (specularColor != null) {
                float r = specularColor.getRed();
                float g = specularColor.getGreen();
                float b = specularColor.getBlue();
                material.setSpecularColor(true, r, g, b, specularPower);
            } else {
                material.setSpecularColor(false, 1, 1, 1, specularPower);
            }
            specularColorDirty = false;
            specularPowerDirty = false;
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
        // prevent undefined behavior in GLSL pow(0, 0), see RT-36235
        if (specularPower < 0.001f) {
            specularPower = 0.001f;
        }
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
