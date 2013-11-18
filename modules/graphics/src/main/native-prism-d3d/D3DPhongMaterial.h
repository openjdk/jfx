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

#ifndef D3DPHONGMATERIAL_H
#define D3DPHONGMATERIAL_H

#include "D3DContext.h"

// See MaterialPhong.h, MaterialPhongShaders.h

#define DIFFUSE 0
#define SPECULAR 1
#define BUMP 2
#define SELFILLUMINATION 3

class D3DPhongMaterial {
public:
    D3DPhongMaterial(D3DContext *pCtx);
    virtual ~D3DPhongMaterial();
    void setSolidColor(float r, float g, float b, float a);
    float *getSolidColor();
    void setMap(int mapID, IDirect3DBaseTexture9 *texMap, bool isSA, bool isBA);
    bool isBumpMap();
    bool isSpecularMap();
    bool isSelfIllumMap();
    bool isSpecularAlpha();
    IDirect3DBaseTexture9 * getMap(int type);

private:
    D3DContext *context;
    float diffuseColor[4];
    IDirect3DBaseTexture9 *map[4];
    bool specularAlpha, bumpAlpha;

};

#endif  /* D3DPHONGMATERIAL_H */
