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

package com.sun.prism.es2;

import com.sun.prism.Graphics;
import com.sun.prism.Material;
import com.sun.prism.impl.BaseMeshView;
import com.sun.prism.impl.Disposer;

/**
 * TODO: 3D - Need documentation
 */
class ES2MeshView extends BaseMeshView {

    static int count = 0;
    private final ES2Context context;
    private final long nativeHandle;
    private float ambientLightRed = 0;
    private float ambientLightBlue = 0;
    private float ambientLightGreen = 0;

    // NOTE: We only support up to 3 point lights at the present
    private ES2Light[] lights = new ES2Light[3];

    // TODO: 3D - Need a mechanism to "decRefCount" Mesh and Material
    //            if we need to do eager clean up
    final private ES2Mesh mesh;
    private ES2PhongMaterial material;

    private ES2MeshView(ES2Context context, long nativeHandle, ES2Mesh mesh,
            Disposer.Record disposerRecord) {
        super(disposerRecord);
        this.context = context;
        this.mesh = mesh;
        this.nativeHandle = nativeHandle;
        count++;
    }

    static ES2MeshView create(ES2Context context, ES2Mesh mesh) {
        long nativeHandle = context.createES2MeshView(mesh);
        return new ES2MeshView(context, nativeHandle, mesh, new ES2MeshViewDisposerRecord(context, nativeHandle));
    }

    @Override
    public void setCullingMode(int cullingMode) {
        context.setCullingMode(nativeHandle, cullingMode);
    }

    @Override
    public void setMaterial(Material material) {
        context.setMaterial(nativeHandle, material);
        this.material = (ES2PhongMaterial) material;
    }

    @Override
    public void setWireframe(boolean wireframe) {
        context.setWireframe(nativeHandle, wireframe);
    }

    @Override
    public void setAmbientLight(float r, float g, float b) {
        ambientLightRed = r;
        ambientLightGreen = g;
        ambientLightBlue = b;
        context.setAmbientLight(nativeHandle, r, g, b);
    }

    float getAmbientLightRed() {
        return ambientLightRed;
    }

    float getAmbientLightGreen() {
        return ambientLightGreen;
    }

    float getAmbientLightBlue() {
        return ambientLightBlue;
    }

    @Override
    public void setPointLight(int index, float x, float y, float z, float r, float g, float b, float w,
            float ca, float la, float qa, float maxRange) {
        // NOTE: We only support up to 3 point lights at the present
        if (index >= 0 && index <= 2) {
            lights[index] = new ES2Light(x, y, z, r, g, b, w, ca, la, qa, maxRange);
            context.setPointLight(nativeHandle, index, x, y, z, r, g, b, w, ca, la, qa, maxRange);
        }
    }

    ES2Light[] getPointLights() {
        return lights;
    }

    @Override
    public void render(Graphics g) {
        material.lockTextureMaps();
        context.renderMeshView(nativeHandle, g, this);
        material.unlockTextureMaps();
    }

    ES2PhongMaterial getMaterial() {
        return material;
    }

    @Override
    public void dispose() {
        // TODO: 3D - Need a mechanism to "decRefCount" Mesh and Material
        material = null;
        lights = null;
        disposerRecord.dispose();
        count--;
    }

    public int getCount() {
        return count;
    }

    static class ES2MeshViewDisposerRecord implements Disposer.Record {

        private final ES2Context context;
        private long nativeHandle;

        ES2MeshViewDisposerRecord(ES2Context context, long nativeHandle) {
            this.context = context;
            this.nativeHandle = nativeHandle;
        }

        void traceDispose() { }

        @Override
        public void dispose() {
            if (nativeHandle != 0L) {
                traceDispose();
                context.releaseES2MeshView(nativeHandle);
                nativeHandle = 0L;
            }
        }
    }
}
