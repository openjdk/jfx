/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.d3d12;

import com.sun.prism.Graphics;
import com.sun.prism.Material;
import com.sun.prism.impl.BaseMeshView;
import com.sun.prism.impl.Disposer;
import com.sun.prism.d3d12.ni.D3D12NativeMeshView;

public class D3D12MeshView extends BaseMeshView {
    private D3D12Context mContext;
    private D3D12MeshViewData mMeshViewData;
    private D3D12PhongMaterial mMaterial;

    protected D3D12MeshView(D3D12Context context, D3D12MeshViewData meshViewData) {
        super(meshViewData);
        this.mContext = context;
        this.mMeshViewData = meshViewData;
    }

    D3D12NativeMeshView getNativeMeshView() {
        return mMeshViewData.mMeshView;
    }

    static public D3D12MeshView create(D3D12Context context, D3D12Mesh mesh) {
        return new D3D12MeshView(context,
                                 new D3D12MeshViewData(
                                    context.getDevice().createMeshView(mesh.getNative())
                                    )
                                 );
    }

    @Override
    public void setCullingMode(int mode) {
        mMeshViewData.getNative().setCullingMode(mode);
    }

    @Override
    public void setMaterial(Material material) {
        this.mMaterial = (D3D12PhongMaterial)material;
        mMeshViewData.getNative().setMaterial(this.mMaterial.getNative());
    }

    @Override
    public void setWireframe(boolean wireframe) {
        mMeshViewData.getNative().setWireframe(wireframe);
    }

    @Override
    public void setAmbientLight(float r, float g, float b) {
        mMeshViewData.getNative().setAmbientLight(r, g, b);
    }

    @Override
    public void setLight(int index, float x, float y, float z, float r, float g, float b, float enabled, float ca, float la,
            float qa, float isAttenuated, float maxRange, float dirX, float dirY, float dirZ, float innerAngle,
            float outerAngle, float falloff) {
        // NOTE: We only support up to 3 point lights at the present
        if (index >= 0 && index <= 2) {
            mMeshViewData.getNative().setLight(index, x, y, z, r, g, b, enabled, ca, la, qa, isAttenuated, maxRange,
                                            dirX, dirY, dirZ, innerAngle, outerAngle, falloff);
        }
    }

    @Override
    public void render(Graphics g) {
        mMaterial.lockTextureMaps();
        mContext.renderMeshView(this, g);
        mMaterial.unlockTextureMaps();
    }

    @Override
    public void dispose() {
        disposerRecord.dispose();
    }

    static class D3D12MeshViewData implements Disposer.Record {

        D3D12NativeMeshView mMeshView;

        D3D12MeshViewData(D3D12NativeMeshView meshView) {
            if (!meshView.isValid()) {
                throw new NullPointerException("Mesh view object is NULL");
            }

            this.mMeshView = meshView;
        }

        D3D12NativeMeshView getNative() {
            return mMeshView;
        }

        @Override
        public void dispose() {
            if (mMeshView.isValid()) {
                mMeshView.dispose();
            }
        }
    }
}
