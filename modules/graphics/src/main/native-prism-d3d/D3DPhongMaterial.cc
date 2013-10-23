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

#include <iostream>
#include "D3DPhongMaterial.h"

using std::cout;
using std::cerr;
using std::endl;

// Destructor definition
D3DPhongMaterial::~D3DPhongMaterial() {
    context = NULL;
    // The freeing of texture native resources is handled by its Java layer.
    map[DIFFUSE] = NULL;
    map[SPECULAR] = NULL;
    map[BUMP] = NULL;
    map[SELFILLUMINATION] = NULL;
}

D3DPhongMaterial::D3DPhongMaterial(D3DContext *ctx) {
    context = ctx;
    diffuseColor[0] = 0;
    diffuseColor[1] = 0;
    diffuseColor[2] = 0;
    diffuseColor[3] = 0;
    map[DIFFUSE] = NULL;
    map[SPECULAR] = NULL;
    map[BUMP] = NULL;
    map[SELFILLUMINATION] = NULL;
    specularAlpha = false;
    bumpAlpha = false;
}

void D3DPhongMaterial::setSolidColor(float r, float g, float b, float a) {
    diffuseColor[0] = r;
    diffuseColor[1] = g;
    diffuseColor[2] = b;
    diffuseColor[3] = a;
}

float * D3DPhongMaterial::getSolidColor() {
    return diffuseColor;
}

bool D3DPhongMaterial::isBumpMap() {
    return map[BUMP] ? true : false;
}

bool D3DPhongMaterial::isSpecularMap() {
    return map[SPECULAR] ? true : false;
}

bool D3DPhongMaterial::isSelfIllumMap() {
    return map[SELFILLUMINATION] ? true : false;
}

bool D3DPhongMaterial::isSpecularAlpha() {
    return specularAlpha;
}

IDirect3DBaseTexture9 * D3DPhongMaterial::getMap(int type) {
    // Within the range of DIFFUSE, SPECULAR, BUMP, SELFILLUMINATION
    if (type >= 0 && type <= 3) {
        return map[type];
    }
    cerr << "D3DPhongMaterial::getMap -- type is out of range - type = " << type << endl;
    return NULL;
}

void D3DPhongMaterial::setMap(int mapID, IDirect3DBaseTexture9 *texMap,
        bool isSA, bool isBA) {
    // Within the range of DIFFUSE, SPECULAR, BUMP, SELFILLUMINATION
    if (mapID >= 0 && mapID <= 3) {
        map[mapID] = texMap;
        specularAlpha = isSA;
        bumpAlpha = isBA;
    } else {
        cerr << "D3DPhongMaterial::getMap -- mapID is out of range - mapID = " << mapID << endl;
    }
}
