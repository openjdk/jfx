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

import com.sun.prism.impl.BaseMesh;
import com.sun.prism.impl.Disposer;
import com.sun.prism.d3d12.ni.D3D12NativeMesh;

class D3D12Mesh extends BaseMesh {
    static int count = 0;

    D3D12Context mContext;
    D3D12MeshData mMeshData;

    private D3D12Mesh(D3D12Context context, D3D12MeshData meshData) {
        super(meshData);
        mContext = context;
        mMeshData = meshData;
        ++count;
    }

    static public D3D12Mesh create(D3D12Context context) {
        return new D3D12Mesh(context,
                             new D3D12MeshData(
                                context.getDevice().createMesh()
                                )
                             );
    }

    D3D12NativeMesh getNative() {
        return mMeshData.getResource();
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public boolean buildNativeGeometry(float[] vertexBuffer, int vertexBufferLength,
                                       int[] indexBufferInt, int indexBufferLength) {
        return mMeshData.getResource().buildGeometryBuffers(vertexBuffer, vertexBufferLength,
                                                            indexBufferInt, indexBufferLength);
    }

    @Override
    public boolean buildNativeGeometry(float[] vertexBuffer, int vertexBufferLength,
                                       short[] indexBufferShort, int indexBufferLength) {
        return mMeshData.getResource().buildGeometryBuffers(vertexBuffer, vertexBufferLength,
                                                            indexBufferShort, indexBufferLength);
    }

    @Override
    public void dispose() {
        disposerRecord.dispose();
        --count;
    }

    static class D3D12MeshData implements Disposer.Record {

        private D3D12NativeMesh mMesh;

        D3D12MeshData(D3D12NativeMesh mesh) {
            if (!mesh.isValid()) {
                throw new NullPointerException("Mesh object is NULL");
            }

            mMesh = mesh;
        }

        D3D12NativeMesh getResource() {
            return mMesh;
        }

        @Override
        public void dispose() {
            mMesh.dispose();
        }

        public boolean isValid() {
            return mMesh.isValid();
        }
    }
}
