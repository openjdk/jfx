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

#ifndef D3DMESHVIEW_H
#define D3DMESHVIEW_H

#include "D3DContext.h"
#include "D3DLight.h"
#include "D3DMesh.h"
#include "D3DPhongMaterial.h"

class D3DMeshView {
public:
    D3DMeshView(D3DContext *pCtx, D3DMesh *pMesh);
    virtual ~D3DMeshView();
    void setCullingMode(int cMode);
    void setMaterial(D3DPhongMaterial *pMaterial);
    void setWireframe(bool wf);
    void setAmbientLight(float r, float g, float b);
    void setPointLight(int index, float x, float y, float z,
        float r, float g, float b, float w,
        float ca, float la, float qa, float maxRange);
    void computeNumLights();
    void render();

private:
    D3DContext *context;
    D3DMesh *mesh;
    D3DPhongMaterial *material;
    D3DLight lights[3];
    float ambientLightColor[3];
    int numLights;
    bool lightsDirty;
    int cullMode;
    bool wireframe;
};

#endif  /* D3DMESHVIEW_H */

