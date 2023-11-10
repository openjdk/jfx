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

#include <iostream>
#include "D3DPhongMaterial.h"

using std::cout;
using std::cerr;
using std::endl;

// Destructor definition
D3DPhongMaterial::~D3DPhongMaterial() {
    context = NULL;
    // The freeing of texture native resources is handled by its Java layer.
    map.fill(NULL);
}

D3DPhongMaterial::D3DPhongMaterial(D3DContext *ctx) :
    context(ctx)
{}

void D3DPhongMaterial::setDiffuseColor(float r, float g, float b, float a) {
    diffuseColor[0] = r;
    diffuseColor[1] = g;
    diffuseColor[2] = b;
    diffuseColor[3] = a;
}

float * D3DPhongMaterial::getDiffuseColor() {
    return diffuseColor;
}

void D3DPhongMaterial::setSpecularColor(bool set, float r, float g, float b, float a) {
    specularColorSet = set;
    specularColor[0] = r;
    specularColor[1] = g;
    specularColor[2] = b;
    specularColor[3] = a;
}

float * D3DPhongMaterial::getSpecularColor() {
    return specularColor;
}

bool D3DPhongMaterial::isBumpMap() {
    return map[MapType::BUMP] != NULL;
}

bool D3DPhongMaterial::isSpecularMap() {
    return map[MapType::SPECULAR] != NULL;
}

bool D3DPhongMaterial::isSelfIllumMap() {
    return map[MapType::SELF_ILLUMINATION] != NULL;
}

bool D3DPhongMaterial::isSpecularColor() {
    return specularColorSet;
}

IDirect3DBaseTexture9 * D3DPhongMaterial::getMap(MapType type) {
    return map[type];
}

D3DTEXTUREFILTERTYPE D3DPhongMaterial::getMinFilterType(MapType type) {
    return minFilter[type];
}

D3DTEXTUREFILTERTYPE D3DPhongMaterial::getMagFilterType(MapType type) {
    return magFilter[type];
}

D3DTEXTUREFILTERTYPE D3DPhongMaterial::getMipFilterType(MapType type) {
    return mipFilter[type];
}

void D3DPhongMaterial::setMap(MapType type, IDirect3DBaseTexture9 *texMap, D3DTEXTUREFILTERTYPE min,
        D3DTEXTUREFILTERTYPE mag, D3DTEXTUREFILTERTYPE mip) {
    map[type] = texMap;
    minFilter[type] = min;
    magFilter[type] = mag;
    mipFilter[type] = mip;
}
