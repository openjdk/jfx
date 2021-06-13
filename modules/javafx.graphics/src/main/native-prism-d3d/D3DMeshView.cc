/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
#define _USE_MATH_DEFINES
#include <math.h>
#include "D3DMeshView.h"
#include "D3DPhongShader.h"

using std::cout;
using std::cerr;
using std::endl;

// Destructor definition
D3DMeshView::~D3DMeshView() {
    context = NULL;
    // The freeing of native resources is handled by its Java layer.
    mesh = NULL;
    material = NULL;
}

D3DMeshView::D3DMeshView(D3DContext *ctx, D3DMesh *pMesh) {
    context = ctx;
    mesh = pMesh;
    material = NULL;
    ambientLightColor[0] = 0;
    ambientLightColor[1] = 0;
    ambientLightColor[2] = 0;
    numLights = 0;
    ZeroMemory(lights, sizeof(D3DLight) * 3);
    lightsDirty = TRUE;
    cullMode = D3DCULL_NONE;
    wireframe = FALSE;
}

void D3DMeshView::setCullingMode(int cMode) {
    cullMode = cMode;
}

void D3DMeshView::setMaterial(D3DPhongMaterial *pMaterial) {
    material = pMaterial;
}

void D3DMeshView::setWireframe(bool wf) {
    wireframe = wf;
}

void D3DMeshView::setAmbientLight(float r, float g, float b) {
    ambientLightColor[0] = r;
    ambientLightColor[1] = g;
    ambientLightColor[2] = b;
}

void D3DMeshView::setLight(int index, float x, float y, float z, float r, float g, float b, float w,
        float ca, float la, float qa, float maxRange,
        float dirX, float dirY, float dirZ, float innerAngle, float outerAngle, float falloff) {
    // NOTE: We only support up to 3 point lights at the present
    if (index >= 0 && index <= MAX_NUM_LIGHTS - 1) {
        D3DLight& light = lights[index];
        light.position[0] = x;
        light.position[1] = y;
        light.position[2] = z;
        light.color[0] = r;
        light.color[1] = g;
        light.color[2] = b;
        light.w = w;
        light.attenuation[0] = ca;
        light.attenuation[1] = la;
        light.attenuation[2] = qa;
        light.maxRange = maxRange;
        light.direction[0] = dirX;
        light.direction[1] = dirY;
        light.direction[2] = dirZ;
        light.innerAngle = innerAngle;
        light.outerAngle = outerAngle;
        light.falloff = falloff;
        lightsDirty = TRUE;
    }
}

void D3DMeshView::computeNumLights() {
    if (!lightsDirty)
        return;
    lightsDirty = false;

    int n = 0;
    for (int i = 0; i != MAX_NUM_LIGHTS; ++i)
        n += lights[i].w ? 1 : 0;

    numLights = n;
}

inline void matrixTransposed(D3DMATRIX& r, const D3DMATRIX& a) {
    for (int i=0; i<4; i++) {
        for (int j=0; j<4; j++) {
            r.m[j][i] = a.m[i][j];
        }
    }
}

void D3DMeshView::render() {
    RETURN_IF_NULL(context);
    RETURN_IF_NULL(material);
    RETURN_IF_NULL(mesh);

    IDirect3DDevice9 *device = context->Get3DDevice();
    RETURN_IF_NULL(device);

    HRESULT status = SUCCEEDED(device->SetFVF(mesh->getVertexFVF()));
    if (!status) {
        cout << "D3DMeshView.render() - SetFVF failed !!!" << endl;
        return;
    }

    D3DPhongShader *pShader = context->getPhongShader();
    RETURN_IF_NULL(pShader);

    status = SUCCEEDED(device->SetVertexShader(pShader->getVertexShader()));
    if (!status) {
        cout << "D3DMeshView.render() - SetVertexShader failed !!!" << endl;
        return;
    }

    computeNumLights();
    // We only support up to 3 point lights at the present
    for (int i = 0; i < MAX_NUM_LIGHTS; i++) {
        status = SUCCEEDED(device->SetVertexShaderConstantF(VSR_LIGHTS + i*2, lights[i].position, 1));
    }

    float lightsNormDirection[MAX_NUM_LIGHTS * 4]; // 3 lights x (3 coords + 1 padding)
    for (int i = 0, d = 0; i < MAX_NUM_LIGHTS; i++) {
        if (lights[i].isPointLight()) {
            lightsNormDirection[d++] = 0;
            lightsNormDirection[d++] = 0;
            lightsNormDirection[d++] = 1;
            lightsNormDirection[d++] = 0;
        } else {
            float dirX = lights[i].direction[0];
            float dirY = lights[i].direction[1];
            float dirZ = lights[i].direction[2];
            float length = sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
            lightsNormDirection[d++] = dirX / length;
            lightsNormDirection[d++] = dirY / length;
            lightsNormDirection[d++] = dirZ / length;
            lightsNormDirection[d++] = 0;
        }
    }

    status = SUCCEEDED(device->SetVertexShaderConstantF(VSR_DIRS, lightsNormDirection, MAX_NUM_LIGHTS));
    if (!status) {
        cout << "D3DMeshView.render() - SetVertexShaderConstantF (VSR_DIRS) failed !!!" << endl;
        return;
    }

    status = SUCCEEDED(device->SetVertexShaderConstantF(VSR_AMBIENTCOLOR, ambientLightColor, 1));
    if (!status) {
        cout << "D3DMeshView.render() - SetVertexShaderConstantF (VSR_AMBIENTCOLOR) failed !!!" << endl;
        return;
    }

    status = SUCCEEDED(device->SetPixelShaderConstantF(PSR_DIFFUSECOLOR, material->getDiffuseColor(), 1));
    if (!status) {
        cout << "D3DMeshView.render() - SetPixelShaderConstantF (PSR_DIFFUSECOLOR) failed !!!" << endl;
        return;
    }

    status = SUCCEEDED(device->SetPixelShaderConstantF(PSR_SPECULARCOLOR, material->getSpecularColor(), 1));
    if (!status) {
        cout << "D3DMeshView.render() - SetPixelShaderConstantF (PSR_SPECULARCOLOR) failed !!!" << endl;
        return;
    }

    float lightsColor[MAX_NUM_LIGHTS * 4];        // 3 lights x (3 color + 1 padding)
    float lightsAttenuation[MAX_NUM_LIGHTS * 4];  // 3 lights x (3 attenuation factors + 1 padding)
    float lightsRange[MAX_NUM_LIGHTS * 4];        // 3 lights x (1 maxRange + 3 padding)
    float spotLightsFactors[MAX_NUM_LIGHTS * 4];  // 3 lights x (2 angles + 1 falloff + 1 padding)
    for (int i = 0, c = 0, a = 0, r = 0, s = 0; i < MAX_NUM_LIGHTS; i++) {
        float w = lights[i].w;
        lightsColor[c++] = lights[i].color[0] * w;
        lightsColor[c++] = lights[i].color[1] * w;
        lightsColor[c++] = lights[i].color[2] * w;
        lightsColor[c++] = 1;

        lightsAttenuation[a++] = lights[i].attenuation[0];
        lightsAttenuation[a++] = lights[i].attenuation[1];
        lightsAttenuation[a++] = lights[i].attenuation[2];
        lightsAttenuation[a++] = 0;

        lightsRange[r++] = lights[i].maxRange;
        lightsRange[r++] = 0;
        lightsRange[r++] = 0;
        lightsRange[r++] = 0;

        if (lights[i].isPointLight()) {
            spotLightsFactors[s++] = -1; // cos(180)
            spotLightsFactors[s++] = 2;  // cos(0) - cos(180)
            spotLightsFactors[s++] = 0;
            spotLightsFactors[s++] = 0;
        } else {
            // preparing for: I = pow((cosAngle - cosOuter) / (cosInner - cosOuter), falloff)
            float cosInner = cos(lights[i].innerAngle * M_PI / 180);
            float cosOuter = cos(lights[i].outerAngle * M_PI / 180);
            spotLightsFactors[s++] = cosOuter;
            spotLightsFactors[s++] = cosInner - cosOuter;
            spotLightsFactors[s++] = lights[i].falloff;
            spotLightsFactors[s++] = 0;
        }
    }

    status = SUCCEEDED(device->SetPixelShaderConstantF(PSR_LIGHTCOLOR, lightsColor, MAX_NUM_LIGHTS));
    if (!status) {
        cout << "D3DMeshView.render() - SetPixelShaderConstantF(PSR_LIGHTCOLOR) failed !!!" << endl;
        return;
    }

    status = SUCCEEDED(device->SetPixelShaderConstantF(PSR_LIGHT_ATTENUATION, lightsAttenuation, MAX_NUM_LIGHTS));
    if (!status) {
        cout << "D3DMeshView.render() - SetPixelShaderConstantF(PSR_LIGHT_ATTENUATION) failed !!!" << endl;
        return;
    }

    status = SUCCEEDED(device->SetPixelShaderConstantF(PSR_LIGHT_RANGE, lightsRange, MAX_NUM_LIGHTS));
    if (!status) {
        cout << "D3DMeshView.render() - SetPixelShaderConstantF(PSR_LIGHT_RANGE) failed !!!" << endl;
        return;
    }

    status = SUCCEEDED(device->SetPixelShaderConstantF(PSR_SPOTLIGHT_FACTORS, spotLightsFactors, MAX_NUM_LIGHTS));
    if (!status) {
        cout << "D3DMeshView.render() - SetPixelShaderConstantF(PSR_SPOTLIGHT_FACTORS) failed !!!" << endl;
        return;
    }

    int bm = pShader->getBumpMode(material->isBumpMap());
    int sm = pShader->getSpecularMode(material->isSpecularMap(), material->isSpecularColor());
    int im = material->isSelfIllumMap() ? 1 : 0;

    status = pShader->setPixelShader(numLights, sm, bm, im);
    if (!status) {
        cout << "D3DMeshView.render() - setPixelShader failed !!!" << endl;
        return;
    }

    SUCCEEDED(device->SetTexture(SR_DIFFUSEMAP, material->getMap(DIFFUSE)));
    SUCCEEDED(device->SetTexture(SR_SPECULARMAP, material->getMap(SPECULAR)));
    SUCCEEDED(device->SetTexture(SR_BUMPHEIGHTMAP, material->getMap(BUMP)));
    SUCCEEDED(device->SetTexture(SR_SELFILLUMMAP, material->getMap(SELFILLUMINATION)));

    D3DMATRIX mat;
    matrixTransposed(mat, *(context->GetWorldTx()));
//    std::cerr << "Transposed world transform:\n";
//    fprintf(stderr, "  %5f %5f %5f %5f\n", mat._11, mat._12, mat._13, mat._14);
//    fprintf(stderr, "  %5f %5f %5f %5f\n", mat._21, mat._22, mat._23, mat._24);
//    fprintf(stderr, "  %5f %5f %5f %5f\n", mat._31, mat._32, mat._33, mat._34);
//    fprintf(stderr, "  %5f %5f %5f %5f\n", mat._41, mat._42, mat._43, mat._44);

    SUCCEEDED(device->SetVertexShaderConstantF(VSR_WORLDMATRIX, (float*) mat.m, 3));

    if (context->state.cullMode != cullMode) {
        context->state.cullMode = cullMode;
        SUCCEEDED(device->SetRenderState(D3DRS_CULLMODE, D3DCULL(cullMode)));
    }
    if (context->state.wireframe != wireframe) {
        context->state.wireframe = wireframe;
        SUCCEEDED(device->SetRenderState(D3DRS_FILLMODE,
                wireframe ? D3DFILL_WIREFRAME : D3DFILL_SOLID));
    }

    SUCCEEDED(device->SetStreamSource(0, mesh->getVertexBuffer(), 0, PRIMITIVE_VERTEX_SIZE));
    SUCCEEDED(device->SetIndices(mesh->getIndexBuffer()));
    SUCCEEDED(device->DrawIndexedPrimitive(D3DPT_TRIANGLELIST, 0, 0,
            mesh->getNumVertices(), 0, (mesh->getNumIndices()/3)));
}
