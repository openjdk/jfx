/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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

D3DMeshView::D3DMeshView(D3DContext *ctx, D3DMesh *pMesh) :
    context(ctx),
    mesh(pMesh)
{}

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

void D3DMeshView::setLight(int index, float x, float y, float z, float r, float g, float b, float lightOn,
        float ca, float la, float qa, float isAttenuated, float maxRange,
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
        light.lightOn = lightOn;
        light.attenuation[0] = ca;
        light.attenuation[1] = la;
        light.attenuation[2] = qa;
        light.attenuation[3] = isAttenuated;
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
    for (int i = 0; i != MAX_NUM_LIGHTS; ++i) {
        n += lights[i].lightOn ? 1 : 0;
    }

    numLights = n;
}

inline void matrixTransposed(D3DMATRIX& r, const D3DMATRIX& a) {
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
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

    // Prepare lights data
    float lightsPosition[MAX_NUM_LIGHTS * 4];      // 3 coords + 1 padding
    float lightsNormDirection[MAX_NUM_LIGHTS * 4]; // 3 coords + 1 padding
    float lightsColor[MAX_NUM_LIGHTS * 4];         // 3 color + 1 padding
    float lightsAttenuation[MAX_NUM_LIGHTS * 4];   // 3 attenuation factors + 1 isAttenuated
    float lightsRange[MAX_NUM_LIGHTS * 4];         // 1 maxRange + 3 padding
    float spotLightsFactors[MAX_NUM_LIGHTS * 4];   // 2 angles + 1 falloff + 1 padding
    for (int i = 0, d = 0, p = 0, c = 0, a = 0, r = 0, s = 0; i < MAX_NUM_LIGHTS; i++) {
        D3DLight& light = lights[i];

        lightsPosition[p++] = light.position[0];
        lightsPosition[p++] = light.position[1];
        lightsPosition[p++] = light.position[2];
        lightsPosition[p++] = 0;

        lightsNormDirection[d++] = light.direction[0];
        lightsNormDirection[d++] = light.direction[1];
        lightsNormDirection[d++] = light.direction[2];
        lightsNormDirection[d++] = 0;

        lightsColor[c++] = light.color[0];
        lightsColor[c++] = light.color[1];
        lightsColor[c++] = light.color[2];
        lightsColor[c++] = 1;

        lightsAttenuation[a++] = light.attenuation[0];
        lightsAttenuation[a++] = light.attenuation[1];
        lightsAttenuation[a++] = light.attenuation[2];
        lightsAttenuation[a++] = light.attenuation[3];

        lightsRange[r++] = light.maxRange;
        lightsRange[r++] = 0;
        lightsRange[r++] = 0;
        lightsRange[r++] = 0;

        if (light.isPointLight() || light.isDirectionalLight()) {
            spotLightsFactors[s++] = -1; // cos(180)
            spotLightsFactors[s++] = 2;  // cos(0) - cos(180)
            spotLightsFactors[s++] = 0;
            spotLightsFactors[s++] = 0;
        } else {
            // preparing for: I = pow((cosAngle - cosOuter) / (cosInner - cosOuter), falloff)
            float cosInner = cos(light.innerAngle * M_PI / 180);
            float cosOuter = cos(light.outerAngle * M_PI / 180);
            spotLightsFactors[s++] = cosOuter;
            spotLightsFactors[s++] = cosInner - cosOuter;
            spotLightsFactors[s++] = light.falloff;
            spotLightsFactors[s++] = 0;
        }
    }

    // Set Vertex Shader constants //

    // ProjViewMatrix position is set from D3DContext.cc::SetProjViewMatrix at VSR_VIEWPROJMATRIX
    // Camera position is set from D3DContext.cc::SetCameraPosition at VSR_CAMERAPOS

    status = SUCCEEDED(device->SetVertexShaderConstantF(VSR_LIGHT_POS, lightsPosition, MAX_NUM_LIGHTS));
    if (!status) {
        cout << "D3DMeshView.render() - SetVertexShaderConstantF(VSR_LIGHT_POS) failed !!!" << endl;
        return;
    }

    status = SUCCEEDED(device->SetVertexShaderConstantF(VSR_LIGHT_DIRS, lightsNormDirection, MAX_NUM_LIGHTS));
    if (!status) {
        cout << "D3DMeshView.render() - SetVertexShaderConstantF (VSR_LIGHT_DIRS) failed !!!" << endl;
        return;
    }

    D3DMATRIX mat;
    matrixTransposed(mat, *(context->GetWorldTx()));
//    std::cerr << "Transposed world transform:\n";
//    fprintf(stderr, "  %5f %5f %5f %5f\n", mat._11, mat._12, mat._13, mat._14);
//    fprintf(stderr, "  %5f %5f %5f %5f\n", mat._21, mat._22, mat._23, mat._24);
//    fprintf(stderr, "  %5f %5f %5f %5f\n", mat._31, mat._32, mat._33, mat._34);
//    fprintf(stderr, "  %5f %5f %5f %5f\n", mat._41, mat._42, mat._43, mat._44);

    SUCCEEDED(device->SetVertexShaderConstantF(VSR_WORLDMATRIX, (float*) mat.m, 3));
    if (!status) {
        cout << "D3DMeshView.render() - SetVertexShaderConstantF(VSR_WORLDMATRIX) failed !!!" << endl;
        return;
    }

    // Set Pixel Shader constants //

    status = SUCCEEDED(device->SetPixelShaderConstantF(PSR_MAT_DIFFUSE_COLOR, material->getDiffuseColor(), 1));
    if (!status) {
        cout << "D3DMeshView.render() - SetPixelShaderConstantF (PSR_MAT_DIFFUSE_COLOR) failed !!!" << endl;
        return;
    }

    status = SUCCEEDED(device->SetPixelShaderConstantF(PSR_MAT_SPECULAR_COLOR, material->getSpecularColor(), 1));
    if (!status) {
        cout << "D3DMeshView.render() - SetPixelShaderConstantF (PSR_MAT_SPECULAR_COLOR) failed !!!" << endl;
        return;
    }

    status = SUCCEEDED(device->SetPixelShaderConstantF(PSR_LIGHT_AMBIENT_COLOR, ambientLightColor, 1));
    if (!status) {
        cout << "D3DMeshView.render() - SetPixelShaderConstantF (PSR_LIGHT_AMBIENT_COLOR) failed !!!" << endl;
        return;
    }

    status = SUCCEEDED(device->SetPixelShaderConstantF(PSR_LIGHT_COLOR, lightsColor, MAX_NUM_LIGHTS));
    if (!status) {
        cout << "D3DMeshView.render() - SetPixelShaderConstantF(PSR_LIGHT_COLOR) failed !!!" << endl;
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

// needed for pixel lighting
//    status = SUCCEEDED(device->SetPixelShaderConstantF(PSR_LIGHT_DIRS, lightsNormDirection, MAX_NUM_LIGHTS));
//    if (!status) {
//        cout << "D3DMeshView.render() - SetPixelShaderConstantF (PSR_LIGHT_DIRS) failed !!!" << endl;
//        return;
//    }
//
//    status = SUCCEEDED(device->SetPixelShaderConstantF(PSR_LIGHT_POS, lightsPosition, MAX_NUM_LIGHTS));
//    if (!status) {
//        cout << "D3DMeshView.render() - SetPixelShaderConstantF(PSR_LIGHT_POS) failed !!!" << endl;
//        return;
//    }

    // Set material properties

    int bm = pShader->getBumpMode(material->isBumpMap());
    int sm = pShader->getSpecularMode(material->isSpecularMap(), material->isSpecularColor());
    int im = material->isSelfIllumMap() ? 1 : 0;

    status = pShader->setPixelShader(numLights, sm, bm, im);
    if (!status) {
        cout << "D3DMeshView.render() - setPixelShader failed !!!" << endl;
        return;
    }

    SUCCEEDED(device->SetTexture(SR_DIFFUSE_MAP, material->getMap(DIFFUSE)));
    SUCCEEDED(device->SetTexture(SR_SPECULAR_MAP, material->getMap(SPECULAR)));
    SUCCEEDED(device->SetTexture(SR_BUMPHEIGHT_MAP, material->getMap(BUMP)));
    SUCCEEDED(device->SetTexture(SR_SELFILLUM_MAP, material->getMap(SELFILLUMINATION)));

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
