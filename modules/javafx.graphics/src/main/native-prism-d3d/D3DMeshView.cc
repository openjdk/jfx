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

void D3DMeshView::setPointLight(int index, float x, float y, float z,
        float r, float g, float b, float w,
        float ca, float la, float qa, float maxRange) {
    // NOTE: We only support up to 3 point lights at the present
    if (index >= 0 && index <= 2) {
        lights[index].position[0] = x;
        lights[index].position[1] = y;
        lights[index].position[2] = z;
        lights[index].color[0] = r;
        lights[index].color[1] = g;
        lights[index].color[2] = b;
        lights[index].w = w;
        lights[index].attenuation[0] = ca;
        lights[index].attenuation[1] = la;
        lights[index].attenuation[2] = qa;
        lights[index].maxRange = maxRange;
        lightsDirty = TRUE;
    }
}

void D3DMeshView::computeNumLights() {
    if (!lightsDirty)
        return;
    lightsDirty = false;

    int n = 0;
    for (int i = 0; i != 3; ++i)
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
    for (int i = 0; i < 3; i++) {
        status = SUCCEEDED(device->SetVertexShaderConstantF(VSR_LIGHTS + i*2, lights[i].position, 1));
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

    float lightsColor[12];       // 3 lights x (3 color + 1 padding)
    float lightsAttenuation[12]; // 3 lights x (3 attenuation factors + 1 padding)
    float lightsRange[12];       // 3 lights x (1 maxRange + 3 padding)
    for (int i = 0, c = 0, a = 0, r = 0; i < 3; i++) {
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
    }
    status = SUCCEEDED(device->SetPixelShaderConstantF(PSR_LIGHTCOLOR, lightsColor, 3));
    if (!status) {
        cout << "D3DMeshView.render() - SetPixelShaderConstantF (PSR_LIGHTCOLOR) failed !!!" << endl;
        return;
    }
    status = SUCCEEDED(device->SetPixelShaderConstantF(PSR_LIGHT_ATTENUATION, lightsAttenuation, 3));
    if (!status) {
        cout << "D3DMeshView.render() - SetPixelShaderConstantF (PSR_LIGHT_ATTENUATION) failed !!!" << endl;
        return;
    }
    status = SUCCEEDED(device->SetPixelShaderConstantF(PSR_LIGHT_RANGE, lightsRange, 3));
    if (!status) {
        cout << "D3DMeshView.render() - SetPixelShaderConstantF (PSR_LIGHT_RANGE) failed !!!" << endl;
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
