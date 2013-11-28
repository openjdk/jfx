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
#include "D3DContext.h"
#include "D3DPhongShader.h"

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
                { psMtl1_s1t, psMtl1_s2t, psMtl1_s3t},
                { psMtl1_s1c, psMtl1_s2c, psMtl1_s3c},
                { psMtl1_s1m, psMtl1_s2m, psMtl1_s3m}
            },
            {
                { psMtl1_b1n, psMtl1_b2n, psMtl1_b3n},
                { psMtl1_b1t, psMtl1_b2t, psMtl1_b3t},
                { psMtl1_b1c, psMtl1_b2c, psMtl1_b3c},
                { psMtl1_b1m, psMtl1_b2m, psMtl1_b3m}
            }},
        {
            {
                { psMtl1_s1ni, psMtl1_s2ni, psMtl1_s3ni},
                { psMtl1_s1ti, psMtl1_s2ti, psMtl1_s3ti},
                { psMtl1_s1ci, psMtl1_s2ci, psMtl1_s3ci},
                { psMtl1_s1mi, psMtl1_s2mi, psMtl1_s3mi}
            },
            {
                { psMtl1_b1ni, psMtl1_b2ni, psMtl1_b3ni},
                { psMtl1_b1ti, psMtl1_b2ti, psMtl1_b3ti},
                { psMtl1_b1ci, psMtl1_b2ci, psMtl1_b3ci},
                { psMtl1_b1mi, psMtl1_b2mi, psMtl1_b3mi}
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
    return isBumpMap ? BumpSpecified : BumpNone;
}

int D3DPhongShader::getSpecularMode(bool isSpecularMap, bool isSpecularColor) {
    if (isSpecularMap) {
        return isSpecularColor ? SpecMix : SpecTexture;
    }
    return isSpecularColor ? SpecColor : SpecNone;
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
