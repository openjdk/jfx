/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates. All rights reserved.
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

#ifndef D3DPHONGMATERIAL_H
#define D3DPHONGMATERIAL_H

#include "D3DContext.h"

// See MaterialPhong.h, MaterialPhongShaders.h

// Map type numbered sequence used for sampling registers (we have 4 sampling registers for vs 3.0).
// Order is defined by com.sun.prism.PhongMaterial's MapType enum
enum map_type : unsigned int {
    diffuse,
    specular,
    bump,
    self_illumination,
    num_map_types
};

class D3DPhongMaterial {
public:
    D3DPhongMaterial(D3DContext *pCtx);
    virtual ~D3DPhongMaterial();
    void setDiffuseColor(float r, float g, float b, float a);
    float *getDiffuseColor();
    void setSpecularColor(bool set, float r, float g, float b, float a);
    float *getSpecularColor();
    void setMap(map_type mapID, IDirect3DBaseTexture9 *texMap, D3DTEXTUREFILTERTYPE min,
            D3DTEXTUREFILTERTYPE mag, D3DTEXTUREFILTERTYPE mip);
    bool isBumpMap();
    bool isSpecularMap();
    bool isSpecularColor();
    bool isSelfIllumMap();
    IDirect3DBaseTexture9 * getMap(map_type type);
    D3DTEXTUREFILTERTYPE getMinFilterType(map_type type);
    D3DTEXTUREFILTERTYPE getMagFilterType(map_type type);
    D3DTEXTUREFILTERTYPE getMipFilterType(map_type type);

private:
    D3DContext *context = NULL;
    float diffuseColor[4] = {0};
    float specularColor[4] = {1, 1, 1, 32};
    bool specularColorSet = false;
    IDirect3DBaseTexture9 *map[map_type::num_map_types] = {NULL};
    D3DTEXTUREFILTERTYPE minFilter[map_type::num_map_types] = {NULL};
    D3DTEXTUREFILTERTYPE magFilter[map_type::num_map_types] = {NULL};
    D3DTEXTUREFILTERTYPE mipFilter[map_type::num_map_types] = {NULL};
};

#endif  /* D3DPHONGMATERIAL_H */
