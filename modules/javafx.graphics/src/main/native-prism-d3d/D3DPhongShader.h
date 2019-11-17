/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef D3DPHONGSHADER_H
#define D3DPHONGSHADER_H

// VSR implies Vertex Shader Registers
#define VSR_VIEWPROJMATRIX  0  // 4 total
#define VSR_CAMERAPOS 4        // 1 total
// lighting
// 5 lights (3 in use, 2 reserved)
// with 2 registers = 10 registers
#define VSR_LIGHTS 10
// 8 ambient points + 2 coords : 10 registers
#define VSR_AMBIENTCOLOR 20
// world
#define VSR_WORLDMATRIX 30

// PSR implies Pixel Shader Registers
// we have 224 float constants for ps 3.0
#define PSR_DIFFUSECOLOR 0
#define PSR_SPECULARCOLOR 1
#define PSR_LIGHTCOLOR 4        // 3 lights + 2 reserve
#define PSR_LIGHT_ATTENUATION 9 // 3 lights + 2 reserve
#define PSR_LIGHT_RANGE 14      // 3 lights + 2 reserve

// SR implies Sampler Registers
#define SR_DIFFUSEMAP 0
#define SR_SPECULARMAP 1
#define SR_BUMPHEIGHTMAP 2
#define SR_SELFILLUMMAP 3

enum SpecType {
    SpecNone,
    SpecTexture, // map only w/o alpha
    SpecColor,   // color w/o map
    SpecMix,     // map & color
    SpecTotal
};

enum BumpType {
    BumpNone,
    BumpSpecified,
    BumpTotal
};

typedef const DWORD * ShaderFunction;
ShaderFunction vsMtl1_Obj();
ShaderFunction psMtl1(), psMtl1_i(),
psMtl1_s1n(), psMtl1_s2n(), psMtl1_s3n(),
psMtl1_s1t(), psMtl1_s2t(), psMtl1_s3t(),
psMtl1_s1c(), psMtl1_s2c(), psMtl1_s3c(),
psMtl1_s1m(), psMtl1_s2m(), psMtl1_s3m(),

psMtl1_b1n(), psMtl1_b2n(), psMtl1_b3n(),
psMtl1_b1t(), psMtl1_b2t(), psMtl1_b3t(),
psMtl1_b1c(), psMtl1_b2c(), psMtl1_b3c(),
psMtl1_b1m(), psMtl1_b2m(), psMtl1_b3m(),

psMtl1_s1ni(), psMtl1_s2ni(), psMtl1_s3ni(),
psMtl1_s1ti(), psMtl1_s2ti(), psMtl1_s3ti(),
psMtl1_s1ci(), psMtl1_s2ci(), psMtl1_s3ci(),
psMtl1_s1mi(), psMtl1_s2mi(), psMtl1_s3mi(),

psMtl1_b1ni(), psMtl1_b2ni(), psMtl1_b3ni(),
psMtl1_b1ti(), psMtl1_b2ti(), psMtl1_b3ti(),
psMtl1_b1ci(), psMtl1_b2ci(), psMtl1_b3ci(),
psMtl1_b1mi(), psMtl1_b2mi(), psMtl1_b3mi();

class D3DPhongShader {
public:
    D3DPhongShader(IDirect3DDevice9 *dev);
    virtual ~D3DPhongShader();
    IDirect3DVertexShader9 *getVertexShader();
    int getBumpMode(bool isBumpMap);
    int getSpecularMode(bool isSpecularMap, bool isSpecularColor);
    HRESULT setPixelShader(int numLights, int specularMode, int bumpMode, int selfIllumMode);

static const int SelfIlllumTotal = 2;
static const int maxLights = 3;

private:
    IDirect3DDevice9 *device;
    IDirect3DVertexShader9 *vertexShader;
    IDirect3DPixelShader9 *pixelShader0, *pixelShader0_si;
    IDirect3DPixelShader9 *pixelShaders[SelfIlllumTotal][BumpTotal][SpecTotal][maxLights];
};

#endif  /* D3DPHONGSHADER_H */

