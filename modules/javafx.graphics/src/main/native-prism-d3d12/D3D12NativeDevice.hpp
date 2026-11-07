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

#pragma once

#include "D3D12Common.hpp"

#include "D3D12NativeMesh.hpp"
#include "D3D12NativeMeshView.hpp"
#include "D3D12NativePhongMaterial.hpp"
#include "D3D12NativeRenderTarget.hpp"
#include "D3D12NativeShader.hpp"
#include "D3D12NativeSwapChain.hpp"
#include "D3D12NativeTexture.hpp"

#include "internal/D3D12Buffer.hpp"
#include "internal/D3D12DescriptorAllocator.hpp"
#include "internal/D3D12RenderingContext.hpp"
#include "internal/D3D12Waitable.hpp"
#include "internal/D3D12Debug.hpp"
#include "internal/D3D12Matrix.hpp"
#include "internal/JNIBuffer.hpp"
#include "internal/MemoryView.hpp"

#include "shaders/D3D12ShaderLibrary.hpp"

#include <vector>
#include <memory>
#include <atomic>


namespace D3D12 {

class NativeDevice: public std::enable_shared_from_this<NativeDevice>
{
    // quads have 4 vertices in the array
    using QuadVertices = std::array<float, FLOATS_PER_2D_VERTEX * 4>;
    using QuadColors = std::array<unsigned char, COLORS_PER_2D_VERTEX * 4>;

    NIPtr<NativeInstance> mNativeInstance;
    DXGIAdapterPtr mAdapter;
    D3D12DevicePtr mDevice;
    std::atomic<bool> mDeviceWorks;
    Internal::DebugContextPtr mDebugContext;
    uint32_t mFenceValue;
    uint32_t mFrameCounter; // for debugging ex. triggering a breakpoint after X frames
    uint32_t mProfilerTransferWaitSourceID;
    uint32_t mProfilerFrameTimeID;

    NIPtr<Internal::RenderingContext> mRenderingContext;
    NIPtr<Internal::DescriptorAllocator> mRTVAllocator;
    NIPtr<Internal::DescriptorAllocator> mDSVAllocator;
    NIPtr<Internal::DescriptorAllocator> mSRVAllocator;
    NIPtr<Shaders::ShaderLibrary> mShaderLibrary;
    NIPtr<Shaders::Shader> mPassthroughVS;
    NIPtr<Shaders::Shader> mPhongVS;
    NIPtr<Shaders::Shader> mCurrent2DShader;
    CompositeMode m2DCompositeMode;

    struct Transforms
    {
        Coords_XYZW_FLOAT cameraPos;
        Internal::Matrix<float> worldTransform;
        Internal::Matrix<float> viewProjTransform;
    } mTransforms;

    const NIPtr<Shaders::Shader>& GetPhongPixelShader(const PhongShaderSpec& spec) const;
    void AssembleVertexQuadForBlit(const Coords_Box_UINT32& src, uint64_t srcTotalWidth, uint64_t srcTotalHeight,
                                   const Coords_Box_UINT32& dst, uint64_t dstTotalWidth, uint64_t dstTotalHeight,
                                   QuadVertices& vertices, QuadColors& colors);

public:
    NativeDevice(const NIPtr<NativeInstance>& instance);
    ~NativeDevice();

    bool Init(const DXGIAdapterPtr& adapter, const NIPtr<Shaders::ShaderLibrary>& shaderLibrary);
    void Release();

    // separate, internal buffer creator
    // used for 2D Index Buffer and internals of Mesh
    NIPtr<Internal::Buffer> CreateBuffer(const void* initialData, size_t size, bool cpuWriteable, D3D12_RESOURCE_STATES finalState);

    bool CheckFormatSupport(DXGI_FORMAT format);
    NIPtr<NativeMesh>* CreateMesh();
    NIPtr<NativeMeshView>* CreateMeshView(const NIPtr<NativeMesh>& mesh);
    NIPtr<NativePhongMaterial>* CreatePhongMaterial();
    NIPtr<NativeRenderTarget>* CreateRenderTarget(const NIPtr<NativeTexture>& texture, bool enableDirtyBBox);
    NIPtr<NativeShader>* CreateShader(const std::string& name, void* buf, UINT size);
    NIPtr<NativeTexture>* CreateTexture(UINT width, UINT height, DXGI_FORMAT format, D3D12_RESOURCE_FLAGS flags,
                                        TextureUsage usage, TextureWrapMode wrapMode, int samples, bool useMipmap);
    int GetMaximumMSAASampleSize(DXGI_FORMAT format) const;
    int GetMaximumTextureSize() const;
    void MarkDisposed(const D3D12PageablePtr& pageable);
    void MarkDisposed(const NIPtr<Internal::ITrackedResource>& resource);
    void MarkDisposed(const NIPtr<NativeRenderTarget>& rtt); // specialization for RTTs to do some extra cleanup in the process

    void Clear(float r, float g, float b, float a, bool clearDepth);
    void ClearTextureUnit(uint32_t unit);
    void RenderQuads(const Internal::MemoryView<float>& vertices, const Internal::MemoryView<unsigned char>& colors,
                     uint32_t vertexCount);
    void RenderMeshView(const NIPtr<NativeMeshView>& meshView);
    void SetCompositeMode(CompositeMode mode);
    void UnsetPixelShader();
    void SetPixelShader(const NIPtr<NativeShader>& ps);
    void SetRenderTarget(const NIPtr<NativeRenderTarget>& target, bool enableDepthTest);
    void SetScissor(bool enabled, int x1, int y1, int x2, int y2);
    bool SetShaderConstants(const NIPtr<NativeShader>& shader, const char* name, const void* data, size_t size);
    void SetTexture(uint32_t unit, const NIPtr<NativeTexture>& texture);
    void SetCameraPos(const Coords_XYZW_FLOAT& pos);
    void SetWorldTransform(const Internal::Matrix<float>& matrix);
    void SetViewProjTransform(const Internal::Matrix<float>& matrix);
    bool Blit(const NIPtr<NativeRenderTarget>& srcRT, const Coords_Box_UINT32& src,
              const NIPtr<Internal::IRenderTarget>& dstRT, const Coords_Box_UINT32& dst);
    bool ReadTexture(const NIPtr<NativeTexture>& texture, void* buffer, size_t pixelCount,
                     uint32_t srcx, uint32_t srcy, uint32_t srcw, uint32_t srch);
    bool UpdateTexture(const NIPtr<NativeTexture>& texture, const void* data, size_t
         pixelCount, PixelFormat srcFormat,
                       uint32_t dstx, uint32_t dsty, uint32_t srcx, uint32_t srcy, uint32_t srcw, uint32_t srch, uint32_t srcstride);

    bool PrepareSwapChain(const NIPtr<NativeSwapChain>& swapChain, const D3D12_RECT& dirtyRegion);
    bool Present(const NIPtr<NativeSwapChain>& swapChain);
    void FinishFrame();
    bool WaitUntilIdle();

    void ExamineDeviceRemoved();
    void ReportErrorMessages();

    inline bool IsDeviceReady() const
    {
        return mDeviceWorks;
    }

    const D3D12DevicePtr& GetDevice()
    {
        return mDevice;
    }

    const NIPtr<Internal::DescriptorAllocator>& GetRTVDescriptorAllocator() const
    {
        return mRTVAllocator;
    }

    const NIPtr<Internal::DescriptorAllocator>& GetDSVDescriptorAllocator() const
    {
        return mDSVAllocator;
    }

    const NIPtr<Internal::DescriptorAllocator>& GetSRVDescriptorAllocator() const
    {
        return mSRVAllocator;
    }

    const NIPtr<Shaders::Shader>& GetInternalShader(const std::string_view& name) const
    {
        return mShaderLibrary->GetShaderData(name);
    }

    const NIPtr<Internal::RenderingContext>& GetRenderingContext() const
    {
        return mRenderingContext;
    }
};

} // namespace D3D12
