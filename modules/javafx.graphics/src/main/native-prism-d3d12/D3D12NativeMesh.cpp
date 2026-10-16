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

#include "D3D12NativeMesh.hpp"

#include "D3D12NativeDevice.hpp"

#include "internal/JNIBuffer.hpp"

#include <com_sun_prism_d3d12_ni_D3D12NativeMesh.h>


namespace D3D12 {

NativeMesh::NativeMesh(const NIPtr<NativeDevice>& nativeDevice)
    : mNativeDevice(nativeDevice)
    , mVertexBuffer()
    , mIndexBuffer()
    , mIndexBufferFormat(DXGI_FORMAT_UNKNOWN)
    , mIndexCount(0)
{
}

bool NativeMesh::Init()
{
    // not much to do here, most heavy lifting is done by BuildGeometryBuffers()
    return true;
}

void NativeMesh::Release()
{
    if (mNativeDevice)
    {
        mNativeDevice->MarkDisposed(mVertexBuffer);
        mNativeDevice->MarkDisposed(mIndexBuffer);
    }

    // clear current references so we don't double-dispose of VB/IB
    mVertexBuffer.reset();
    mIndexBuffer.reset();
}

bool NativeMesh::BuildGeometryBuffers(const void* vbData, size_t vbSize, const void* ibData, size_t ibSize, DXGI_FORMAT ibFormat)
{
    if (mVertexBuffer)
    {
        mNativeDevice->MarkDisposed(mVertexBuffer);
        mVertexBuffer.reset();
    }

    if (mIndexBuffer)
    {
        mNativeDevice->MarkDisposed(mIndexBuffer);
        mIndexBuffer.reset();
    }

    mVertexBuffer = mNativeDevice->CreateBuffer(vbData, vbSize, false, D3D12_RESOURCE_STATE_VERTEX_AND_CONSTANT_BUFFER);
    if (!mVertexBuffer)
    {
        D3D12NI_LOG_ERROR("Failed to create a Vertex Buffer for mesh");
        return false;
    }

    mIndexBuffer = mNativeDevice->CreateBuffer(ibData, ibSize, false, D3D12_RESOURCE_STATE_INDEX_BUFFER);
    if (!mIndexBuffer)
    {
        D3D12NI_LOG_ERROR("Failed to create an Index Buffer for mesh");
        return false;
    }

    mIndexBufferFormat = ibFormat;
    mIndexCount = static_cast<uint32_t>(ibSize / GetDXGIFormatBPP(ibFormat));

    return true;
}

} // namespace D3D12


JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeMesh_nReleaseNativeObject
    (JNIEnv* env, jobject obj, jlong ptr)
{
    if (!ptr) return;

    D3D12::GetNIObject<D3D12::NativeMesh>(ptr)->Release();
    D3D12::FreeNIObject<D3D12::NativeMesh>(ptr);
}

JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeMesh_nBuildGeometryBuffersI
    (JNIEnv* env, jobject obj, jlong ptr, jfloatArray verts, jint vertsLength, jintArray indices, jint indicesLength)
{
    if (!ptr) return false;
    if (!verts) return false;
    if (!indices) return false;
    if (vertsLength <= 0 || vertsLength > (SIZE_MAX / sizeof(float))) return false;
    if (indicesLength <= 0 || indicesLength > (SIZE_MAX / sizeof(int))) return false;

    D3D12::Internal::JNIBuffer<jfloatArray> vertsBuffer(env, nullptr, verts);
    D3D12::Internal::JNIBuffer<jintArray> indicesBuffer(env, nullptr, indices);
    if (!vertsBuffer || !indicesBuffer) return false;

    if (vertsLength > vertsBuffer.Count() || indicesLength > indicesBuffer.Count()) return false;

    size_t vertsLengthBytes = vertsLength * sizeof(float);
    size_t indicesLengthBytes = indicesLength * sizeof(int);

    return D3D12::GetNIObject<D3D12::NativeMesh>(ptr)->BuildGeometryBuffers(
        vertsBuffer.Data(), vertsLengthBytes, indicesBuffer.Data(), indicesLengthBytes, DXGI_FORMAT_R32_UINT
    );
}

JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeMesh_nBuildGeometryBuffersS
    (JNIEnv* env, jobject obj, jlong ptr, jfloatArray verts, jint vertsLength, jshortArray indices, jint indicesLength)
{
    if (!ptr) return false;
    if (!verts) return false;
    if (!indices) return false;
    if (vertsLength <= 0 || vertsLength > (SIZE_MAX / sizeof(float))) return false;
    if (indicesLength <= 0 || indicesLength > (SIZE_MAX / sizeof(short))) return false;

    D3D12::Internal::JNIBuffer<jfloatArray> vertsBuffer(env, nullptr, verts);
    D3D12::Internal::JNIBuffer<jshortArray> indicesBuffer(env, nullptr, indices);
    if (!vertsBuffer || !indicesBuffer) return false;

    if (vertsLength > vertsBuffer.Count() || indicesLength > indicesBuffer.Count()) return false;

    size_t vertsLengthBytes = vertsLength * sizeof(float);
    size_t indicesLengthBytes = indicesLength * sizeof(short);

    return D3D12::GetNIObject<D3D12::NativeMesh>(ptr)->BuildGeometryBuffers(
        vertsBuffer.Data(), vertsLengthBytes, indicesBuffer.Data(), indicesLengthBytes, DXGI_FORMAT_R16_UINT
    );
}
