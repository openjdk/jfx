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
#include "D3DPhongShader.h"
#include "D3DContext.h"

using std::cout;
using std::cerr;
using std::endl;

IDirect3DVertexShader9 *createVertexShader(IDirect3DDevice9 *d, ShaderFunction pFun()) {
    IDirect3DVertexShader9 * s;
    return SUCCEEDED(d->CreateVertexShader(pFun(), &s)) ? s : 0;
}

IDirect3DPixelShader9 *createPixelShader(IDirect3DDevice9 *d, ShaderFunction pFun()) {
    IDirect3DPixelShader9 * s;
    return SUCCEEDED(d->CreatePixelShader(pFun(), &s)) ? s : 0;
}

// Destructor definition
D3DPhongShader::~D3DPhongShader() {
    // Freeing of native resources
    vertexShader->Release();
    pixelShader0->Release();
    pixelShader0_si->Release();
    for (int siType = 0; siType != SelfIlllumTotal; ++siType) {
        for (int bType = 0; bType != BumpTotal; ++bType) {
            for (int sType = 0; sType != SpecTotal; ++sType) {
                for (int i = 0; i != maxLights; ++i) {
                    pixelShaders[siType][bType][sType][i]->Release();
                }
            }
        }
    }
    device = NULL;
}

D3DPhongShader::D3DPhongShader(IDirect3DDevice9 *dev) {
    device = dev;

    static ShaderFunction(* const sFuncArr[SelfIlllumTotal][BumpTotal][SpecTotal][maxLights])() = {
        {
            {
                { psMtl1_s1n, psMtl1_s2n, psMtl1_s3n},
                { psMtl1_s1a, psMtl1_s2a, psMtl1_s3a},
                { psMtl1_s1s, psMtl1_s2s, psMtl1_s3s}
            },
            {
                { psMtl1_b1n, psMtl1_b2n, psMtl1_b3n},
                { psMtl1_b1a, psMtl1_b2a, psMtl1_b3a},
                { psMtl1_b1s, psMtl1_b2s, psMtl1_b3s}
            }},
        {
            {
                { psMtl1_s1ni, psMtl1_s2ni, psMtl1_s3ni},
                { psMtl1_s1ai, psMtl1_s2ai, psMtl1_s3ai},
                { psMtl1_s1si, psMtl1_s2si, psMtl1_s3si}
            },
            {
                { psMtl1_b1ni, psMtl1_b2ni, psMtl1_b3ni},
                { psMtl1_b1ai, psMtl1_b2ai, psMtl1_b3ai},
                { psMtl1_b1si, psMtl1_b2si, psMtl1_b3si}
            }}
    };

    vertexShader = createVertexShader(device, vsMtl1_Obj);
    pixelShader0 = createPixelShader(device, psMtl1);
    pixelShader0_si = createPixelShader(device, psMtl1_i);

    for (int siType=0; siType!=SelfIlllumTotal; ++siType) {
        for (int bType=0; bType!=BumpTotal; ++bType) {
            for (int sType=0; sType!=SpecTotal; ++sType) {
                for (int i=0; i!=maxLights; ++i) {
                    pixelShaders[siType][bType][sType][i] =
                            createPixelShader(dev, sFuncArr[siType][bType][sType][i]);
                }
            }
        }
    }
}

IDirect3DVertexShader9 * D3DPhongShader::getVertexShader() {
    return vertexShader;
}

int D3DPhongShader::getBumpMode(bool isBumpMap) {
    return isBumpMap ? BUMP_SPECIFIED : BUMP_NONE;
}

int D3DPhongShader::getSpecularMode(bool isSpecularMap, bool isSpecularAlpha){
        return isSpecularMap ?
            isSpecularAlpha ? SPECULAR_SPECIFIED : SPECULAR_AUTO : SPECULAR_NONE;
}

HRESULT D3DPhongShader::setPixelShader(int numLights, int specularMode,
        int bumpMode, int selfIllumMode) {

    if (numLights < 0 || numLights > maxLights
            || selfIllumMode < 0 || selfIllumMode >= SelfIlllumTotal
            || bumpMode < 0 || bumpMode >= BumpTotal
            || specularMode < 0 || specularMode >= SpecTotal) {
        return D3DERR_INVALIDCALL;
    }

    IDirect3DPixelShader9 *pshd = NULL;
    if (numLights == 0) {
        pshd = selfIllumMode ? pixelShader0 : pixelShader0_si;
    } else {
        pshd = pixelShaders[selfIllumMode][bumpMode][specularMode][numLights - 1];
    }

    return SUCCEEDED(device->SetPixelShader(pshd));
}
