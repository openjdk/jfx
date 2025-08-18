/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.mtl;

import com.sun.prism.Graphics;
import com.sun.prism.Material;
import com.sun.prism.impl.BaseMeshView;
import com.sun.prism.impl.Disposer;

class MTLMeshView extends BaseMeshView {

    static int count = 0;

    private final MTLContext context;
    private final long nativeHandle;

    // A mesh could be shared among similar MTLMeshView's.
    // The native side of mesh holds necessary data required for rendering.
    // TODO: 3D - Need a mechanism to "decRefCount" Mesh and Material
    //            if we need to do eager clean up
    final private MTLMesh mesh;
    private MTLPhongMaterial material;

    private MTLMeshView(MTLContext context, long nativeHandle, MTLMesh mesh,
            Disposer.Record disposerRecord) {
        super(disposerRecord);
        this.context = context;
        this.mesh = mesh;
        this.nativeHandle = nativeHandle;
        count++;
    }

    static MTLMeshView create(MTLContext context, MTLMesh mesh) {
        long nativeHandle = context.createMTLMeshView(mesh.getNativeHandle());
        return new MTLMeshView(context, nativeHandle, mesh, new MTLMeshViewDisposerRecord(context, nativeHandle));
    }

    @Override
    public void setCullingMode(int cullingMode) {
        context.setCullingMode(nativeHandle, cullingMode);
    }

    @Override
    public void setMaterial(Material material) {
        context.setMaterial(nativeHandle,
                ((MTLPhongMaterial) material).getNativeHandle());
        this.material = (MTLPhongMaterial) material;
    }

    @Override
    public void setWireframe(boolean wireframe) {
        context.setWireframe(nativeHandle, wireframe);
    }

    @Override
    public void setAmbientLight(float r, float g, float b) {
        context.setAmbientLight(nativeHandle, r, g, b);
    }

    @Override
    public void setLight(int index, float x, float y, float z, float r, float g, float b, float w,
            float ca, float la, float qa, float isAttenuated, float maxRange, float dirX, float dirY, float dirZ,
            float innerAngle, float outerAngle, float falloff) {
        if (index >= 0 && index <= 2) {
            context.setLight(nativeHandle, index, x, y, z, r, g, b, w, ca, la, qa, isAttenuated, maxRange,
                    dirX, dirY, dirZ, innerAngle, outerAngle, falloff);
        }
    }

    @Override
    public void render(Graphics g) {
        material.lockTextureMaps();
        context.renderMeshView(nativeHandle, g);
        material.unlockTextureMaps();
    }

    @Override
    public boolean isValid() {
        return !context.isDisposed();
    }

    @Override
    public void dispose() {
        // TODO: 3D - Need a mechanism to "decRefCount" Mesh and Material
        material = null;
        disposerRecord.dispose();
        count--;
    }

    public int getCount() {
        return count;
    }

    private static class MTLMeshViewDisposerRecord implements Disposer.Record {

        private final MTLContext context;
        private long nativeHandle;

        MTLMeshViewDisposerRecord(MTLContext context, long nativeHandle) {
            this.context = context;
            this.nativeHandle = nativeHandle;
        }

        void traceDispose() {}

        @Override
        public void dispose() {
            if (nativeHandle != 0L && !context.isDisposed()) {
                traceDispose();
                context.releaseMTLMeshView(nativeHandle);
                nativeHandle = 0L;
            }
        }
    }
}
