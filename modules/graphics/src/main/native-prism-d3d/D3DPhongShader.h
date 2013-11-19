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
// we have 32 constants for ps 2.0
#define PSR_CONSTANTCOLOR 0
#define PSR_LIGHTCOLOR 4

// SR implies Sampler Registers
#define SR_DIFFUSEMAP 0
#define SR_SPECULARMAP 1
#define SR_BUMPHEIGHTMAP 2
#define SR_SELFILLUMMAP 3

#define SPECULAR_NONE 0
#define SPECULAR_AUTO 1
#define SPECULAR_SPECIFIED 2

#define BUMP_NONE 0
#define BUMP_SPECIFIED 1

typedef const DWORD * ShaderFunction;
ShaderFunction vsMtl1_Obj();
ShaderFunction psMtl1(), psMtl1_i(),
psMtl1_s1n(), psMtl1_s2n(), psMtl1_s3n(),
psMtl1_s1a(), psMtl1_s2a(), psMtl1_s3a(),
psMtl1_s1s(), psMtl1_s2s(), psMtl1_s3s(),

psMtl1_b1n(), psMtl1_b2n(), psMtl1_b3n(),
psMtl1_b1a(), psMtl1_b2a(), psMtl1_b3a(),
psMtl1_b1s(), psMtl1_b2s(), psMtl1_b3s(),

psMtl1_s1ni(), psMtl1_s2ni(), psMtl1_s3ni(),
psMtl1_s1ai(), psMtl1_s2ai(), psMtl1_s3ai(),
psMtl1_s1si(), psMtl1_s2si(), psMtl1_s3si(),

psMtl1_b1ni(), psMtl1_b2ni(), psMtl1_b3ni(),
psMtl1_b1ai(), psMtl1_b2ai(), psMtl1_b3ai(),
psMtl1_b1si(), psMtl1_b2si(), psMtl1_b3si();

class D3DPhongShader {
public:
    D3DPhongShader(IDirect3DDevice9 *dev);
    virtual ~D3DPhongShader();
    IDirect3DVertexShader9 *getVertexShader();
    int getBumpMode(bool isBumpMap);
    int getSpecularMode(bool isSpecularMap, bool isSpecularAlpha);
    HRESULT setPixelShader(int numLights, int specularMode, int bumpMode, int selfIllumMode);

static const int BumpTotal = 2;
static const int SpecTotal = 3;
static const int SelfIlllumTotal = 2;
static const int maxLights = 3;

private:
    IDirect3DDevice9 *device;
    IDirect3DVertexShader9 *vertexShader;
    IDirect3DPixelShader9 *pixelShader0, *pixelShader0_si;
    IDirect3DPixelShader9 *pixelShaders[SelfIlllumTotal][BumpTotal][SpecTotal][maxLights];
};

#endif  /* D3DPHONGSHADER_H */

