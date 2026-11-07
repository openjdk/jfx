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

#include "D3D12NativeDevice.hpp"

#include "D3D12Constants.hpp"

#include "D3D12NativeInstance.hpp"

#include "internal/D3D12Logger.hpp"
#include "internal/D3D12Profiler.hpp"
#include "internal/D3D12TextureUploader.hpp"
#include "internal/D3D12Utils.hpp"
#include "internal/JNIString.hpp"
#include "shaders/D3D12MipmapGenComputeShader.hpp"

#include <com_sun_prism_d3d12_ni_D3D12NativeDevice.h>

#include <algorithm>


namespace D3D12 {

const NIPtr<Shaders::Shader>& NativeDevice::GetPhongPixelShader(const PhongShaderSpec& spec) const
{
    std::array<char, 16> name;
    size_t nameLen = Constants::PHONG_PS_NAME.length();
    memcpy(name.data(), Constants::PHONG_PS_NAME.c_str(), nameLen);

    const auto appendCharToName = [](std::array<char, 16>& name, size_t& len, char c)
    {
        name[len++] = c;
    };

    uint32_t lightCount = spec.lightCount;
    if (lightCount > 3) lightCount = 3;

    if (lightCount == 0)
    {
        // no light count - only determine whether we need self illumination or not
        if (spec.isSelfIllum)
        {
            appendCharToName(name, nameLen, '_');
            appendCharToName(name, nameLen, 'i');
        }

        appendCharToName(name, nameLen, 0);
        return GetInternalShader(name.data());
    }

    char mapping = 0;
    switch (spec.mapping)
    {
    case PhongShaderMappingVariant::SIMPLE: mapping = 's'; break;
    case PhongShaderMappingVariant::BUMP: mapping = 'b'; break;
    }

    char specular = 0;
    switch (spec.specular)
    {
    case PhongShaderSpecularVariant::NONE: specular = 'n'; break;
    case PhongShaderSpecularVariant::TEXTURE: specular = 't'; break;
    case PhongShaderSpecularVariant::COLOR: specular = 'c'; break;
    case PhongShaderSpecularVariant::MIX: specular = 'm'; break;
    }

    char light = '0' + spec.lightCount;

    appendCharToName(name, nameLen, '_');
    appendCharToName(name, nameLen, mapping);
    appendCharToName(name, nameLen, light);
    appendCharToName(name, nameLen, specular);
    if (spec.isSelfIllum) appendCharToName(name, nameLen, 'i');

    appendCharToName(name, nameLen, 0);
    return GetInternalShader(name.data());
}

void NativeDevice::AssembleVertexQuadForBlit(const Coords_Box_UINT32& src, uint64_t srcTotalWidth, uint64_t srcTotalHeight,
                                             const Coords_Box_UINT32& dst, uint64_t dstTotalWidth, uint64_t dstTotalHeight,
                                             QuadVertices& vertices, QuadColors& colors)
{
    uint32_t srcWidth = src.x1 - src.x0;
    uint32_t srcHeight = src.y1 - src.y0;
    uint32_t dstWidth = dst.x1 - dst.x0;
    uint32_t dstHeight = dst.y1 - dst.y0;

    // default viewport coordinates are from -1 to 1
    // so destination has to be doubled and shifted from 0-2 range to -1-1 range
    Coords_UV_FLOAT srcTexelSize{ 1.0f / srcTotalWidth, 1.0f / srcTotalHeight };
    Coords_UV_FLOAT dstTexelSize{ 2.0f / dstTotalWidth, 2.0f / dstTotalHeight };

    // dstBox V coord is inverted because of d3d12's coordinate system
    Coords_UV_FLOAT srcBoxTexelsMin{ src.x0 * srcTexelSize.u, src.y0 * srcTexelSize.v };
    Coords_UV_FLOAT srcBoxTexelsMax{ src.x1 * srcTexelSize.u, src.y1 * srcTexelSize.v };
    Coords_UV_FLOAT dstBoxTexelsMin{ (dst.x0 * dstTexelSize.u) - 1.0f, (dst.y1 * dstTexelSize.v) - 1.0f };
    Coords_UV_FLOAT dstBoxTexelsMax{ (dst.x1 * dstTexelSize.u) - 1.0f, (dst.y0 * dstTexelSize.v) - 1.0f };

    // Build a fullscreen quad used for slow blit path
    uint32_t idx = 0;

    // vertex 0
    vertices[idx + 0] = dstBoxTexelsMin.u; // pos.x
    vertices[idx + 1] = dstBoxTexelsMin.v; // pos.y
    vertices[idx + 3] = srcBoxTexelsMin.u; // uv1.u
    vertices[idx + 4] = srcBoxTexelsMin.v; // uv1.v
    idx += FLOATS_PER_2D_VERTEX;

    // vertex 1
    vertices[idx + 0] = dstBoxTexelsMin.u; // pos.x
    vertices[idx + 1] = dstBoxTexelsMax.v; // pos.y
    vertices[idx + 3] = srcBoxTexelsMin.u; // uv1.u
    vertices[idx + 4] = srcBoxTexelsMax.v; // uv1.v
    idx += FLOATS_PER_2D_VERTEX;

    // vertex 2
    vertices[idx + 0] = dstBoxTexelsMax.u; // pos.x
    vertices[idx + 1] = dstBoxTexelsMin.v; // pos.y
    vertices[idx + 3] = srcBoxTexelsMax.u; // uv1.u
    vertices[idx + 4] = srcBoxTexelsMin.v; // uv1.v
    idx += FLOATS_PER_2D_VERTEX;

    // vertex 3
    vertices[idx + 0] = dstBoxTexelsMax.u; // pos.x
    vertices[idx + 1] = dstBoxTexelsMax.v; // pos.y
    vertices[idx + 3] = srcBoxTexelsMax.u; // uv1.u
    vertices[idx + 4] = srcBoxTexelsMax.v; // uv1.v

    // fill remaining fields
    for (size_t i = 0; i < vertices.size(); i += FLOATS_PER_2D_VERTEX)
    {
        vertices[i + 2] = 0.0f; // pos.z
        vertices[i + 5] = vertices[i + 3]; // uv2.u == uv1.u
        vertices[i + 6] = vertices[i + 4]; // uv2.v == uv1.v
    }

    for (size_t i = 0; i < colors.size(); ++i)
    {
        colors[i] = 255;
    }
}


NativeDevice::NativeDevice(const NIPtr<NativeInstance>& instance)
    : mNativeInstance(instance)
    , mAdapter(nullptr)
    , mDevice()
    , mDeviceWorks(false)
    , mFenceValue(0)
    , mFrameCounter(0)
    , mProfilerTransferWaitSourceID(0)
    , mProfilerFrameTimeID(0)
    , mRenderingContext()
    , mRTVAllocator()
    , mDSVAllocator()
    , mShaderLibrary()
    , mPassthroughVS()
    , mPhongVS()
    , mCurrent2DShader()
    , m2DCompositeMode()
{
}

NativeDevice::~NativeDevice()
{
    D3D12NI_LOG_DEBUG("Destroying device");

    if (mDevice)
    {
        Internal::Debug::Instance().ReleaseDeviceAndReportLiveObjects(mDebugContext);
        mDebugContext.reset();
        mDevice.Reset();
    }

    D3D12NI_LOG_DEBUG("Device destroyed");
}

bool NativeDevice::Init(const DXGIAdapterPtr& adapter, const NIPtr<Shaders::ShaderLibrary>& shaderLibrary)
{
    if (adapter == nullptr) return false;

    mAdapter = adapter;

    // we fully duplicate the Shader Library from Instance because Shader objects can have data which
    // in multi-adapter scenarios could be accessed by multiple threads (specifically other Device's
    // RenderThread). Thread safety is guaranteed within one Device and its Render Thread, so this
    // duplication gives full separation and guarantees no threading woes.
    mShaderLibrary = shaderLibrary->Duplicate();

    // we're asking for FL 12_0 for highest compatibility
    // we probably won't need anything higher than that
    // FL 11_0 is an option but requires extra care to resource binding in D3D12ResourceManager - see Sampler Heaps
    HRESULT hr = D3D12CreateDevice(mAdapter.Get(), D3D12NI_FEATURE_LEVEL, IID_PPV_ARGS(&mDevice));
    if (FAILED(hr))
    {
        _com_error err(hr);
        D3D12NI_LOG_ERROR("Failed to create D3D12 Device: %x (%ws)", hr, err.ErrorMessage());
        return false;
    }

    std::wstringstream wss;
    wss << L"Main D3D12 Device (adapter 0x" << mAdapter.Get() << ')';
    mDevice->SetName(wss.str().c_str());

    DXGI_ADAPTER_DESC adapterDesc;
    mAdapter->GetDesc(&adapterDesc);
    D3D12NI_LOG_INFO("Device created using adapter %S", adapterDesc.Description);

    mDebugContext = Internal::Debug::Instance().InitDeviceDebug(mDevice);
    if (!mDebugContext)
    {
        D3D12NI_LOG_ERROR("Failed to initialize debug facilities for Device");
        Release();
        return false;
    }

    mFenceValue = 0;

    mRenderingContext = std::make_shared<Internal::RenderingContext>(shared_from_this());
    if (!mRenderingContext->Init())
    {
        D3D12NI_LOG_ERROR("Failed to initialize Rendering Context");
        Release();
        return false;
    }

    mRTVAllocator = std::make_shared<Internal::DescriptorAllocator>(shared_from_this());
    if (!mRTVAllocator->Init(D3D12_DESCRIPTOR_HEAP_TYPE_RTV, false))
    {
        D3D12NI_LOG_ERROR("Failed to create RTV Descriptor Allocator");
        Release();
        return false;
    }

    mDSVAllocator = std::make_shared<Internal::DescriptorAllocator>(shared_from_this());
    if (!mDSVAllocator->Init(D3D12_DESCRIPTOR_HEAP_TYPE_DSV, false))
    {
        D3D12NI_LOG_ERROR("Failed to create DSV Descriptor Allocator");
        Release();
        return false;
    }

    mSRVAllocator = std::make_shared<Internal::DescriptorAllocator>(shared_from_this());
    if (!mSRVAllocator->Init(D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV, false))
    {
        D3D12NI_LOG_ERROR("Failed to create SRV Descriptor Allocator");
        Release();
        return false;
    }

    mRTVAllocator->SetName("RenderTargetView Descriptor Heap");
    mDSVAllocator->SetName("DepthStencilView Descriptor Heap");
    mSRVAllocator->SetName("CBV/SRV/UAV Descriptor Heap");

    mPassthroughVS = GetInternalShader(Constants::PASSTHROUGH_VS_NAME);
    mPhongVS = GetInternalShader(Constants::PHONG_VS_NAME);

    mProfilerTransferWaitSourceID = Internal::Profiler::Instance().RegisterSource("NativeDevice Transfer Wait");
    mProfilerFrameTimeID = Internal::Profiler::Instance().RegisterSource("Frame Time");

    mDeviceWorks = true;
    Internal::Profiler::Instance().TimingStart(mProfilerFrameTimeID);
    return true;
}

void NativeDevice::Release()
{
    D3D12NI_LOG_DEBUG("Releasing Device resources");

    if (mRenderingContext)
    {
        mRenderingContext->Release();
        mRenderingContext.reset();
    }

    mPassthroughVS.reset();
    mPhongVS.reset();
    mCurrent2DShader.reset();

    if (mShaderLibrary) mShaderLibrary.reset();
    if (mRTVAllocator) mRTVAllocator.reset();
    if (mDSVAllocator) mDSVAllocator.reset();
    if (mSRVAllocator) mSRVAllocator.reset();
}

NIPtr<Internal::Buffer> NativeDevice::CreateBuffer(const void* initialData, size_t size, bool cpuWriteable, D3D12_RESOURCE_STATES finalState)
{
    NIPtr<Internal::Buffer> ret = std::make_shared<Internal::Buffer>(shared_from_this());
    if (!ret->Init(initialData, size, cpuWriteable ? D3D12_HEAP_TYPE_UPLOAD : D3D12_HEAP_TYPE_DEFAULT, finalState))
        return nullptr;

    return ret;
}

bool NativeDevice::CheckFormatSupport(DXGI_FORMAT format)
{
    if (format == DXGI_FORMAT_UNKNOWN)
    {
        // textures' format must be known
        // it also might mean conversion from PixelFormat failed
        // (ex. we stumbled upon an unknown or unsupported enum)
        return false;
    }

    D3D12_FEATURE_DATA_FORMAT_SUPPORT fmtSupport;
    D3D12NI_ZERO_STRUCT(fmtSupport);
    fmtSupport.Format = format;

    HRESULT hr = mDevice->CheckFeatureSupport(D3D12_FEATURE_FORMAT_SUPPORT, &fmtSupport, sizeof(fmtSupport));
    if (FAILED(hr))
    {
        _com_error err(hr);
        D3D12NI_LOG_ERROR("Failed to check format support: %x (%ws)", hr, err.ErrorMessage());
        return false;
    }

    return (fmtSupport.Support1 & D3D12_FORMAT_SUPPORT1_TEXTURE2D);
}

NIPtr<NativeMesh>* NativeDevice::CreateMesh()
{
    return CreateNIDeviceObject<NativeMesh>(shared_from_this());
}

NIPtr<NativeMeshView>* NativeDevice::CreateMeshView(const NIPtr<NativeMesh>& mesh)
{
    return CreateNIDeviceObject<NativeMeshView>(shared_from_this(), mesh);
}

NIPtr<NativePhongMaterial>* NativeDevice::CreatePhongMaterial()
{
    return CreateNIDeviceObject<NativePhongMaterial>(shared_from_this());
}

NIPtr<NativeRenderTarget>* NativeDevice::CreateRenderTarget(const NIPtr<NativeTexture>& texture, bool enableDirtyBBox)
{
    return CreateNIDeviceObject<NativeRenderTarget>(shared_from_this(), texture, enableDirtyBBox);
}

NIPtr<NativeShader>* NativeDevice::CreateShader(const std::string& name, void* buf, UINT size)
{
    return CreateNIObject<NativeShader>(name, buf, size);
}

NIPtr<NativeTexture>* NativeDevice::CreateTexture(UINT width, UINT height, DXGI_FORMAT format, D3D12_RESOURCE_FLAGS flags,
                                                  TextureUsage usage, TextureWrapMode wrapMode, int samples, bool useMipmap)
{
    return CreateNIDeviceObject<NativeTexture>(shared_from_this(), width, height, format, flags, usage, wrapMode, samples, useMipmap);
}

int NativeDevice::GetMaximumMSAASampleSize(DXGI_FORMAT format) const
{
    int maxSamples = 2;

    D3D12_FEATURE_DATA_MULTISAMPLE_QUALITY_LEVELS msaaLevels;
    D3D12NI_ZERO_STRUCT(msaaLevels);
    msaaLevels.Format = format;

    for (int i = maxSamples; i <= Constants::MAX_MSAA_SAMPLE_COUNT; i *= 2)
    {
        msaaLevels.SampleCount = i;
        HRESULT hr = mDevice->CheckFeatureSupport(D3D12_FEATURE_MULTISAMPLE_QUALITY_LEVELS, &msaaLevels, sizeof(msaaLevels));
        if (SUCCEEDED(hr))
        {
            maxSamples = i;
        }
        else
        {
            break;
        }
    }

    return maxSamples;
}

int NativeDevice::GetMaximumTextureSize() const
{
    return D3D12_REQ_TEXTURE2D_U_OR_V_DIMENSION;
}

void NativeDevice::MarkDisposed(const D3D12PageablePtr& pageable)
{
    // This MIGHT be called when cleaning up on close, when RenderingContext is already destroyed.
    // Prevent any potential issues - if RenderingContext is gone pageables can be
    // safely cleaned by the main thread.
    if (mRenderingContext && pageable) mRenderingContext->Dispose(pageable);
}

void NativeDevice::MarkDisposed(const NIPtr<Internal::ITrackedResource>& resource)
{
    if (mRenderingContext && resource) mRenderingContext->Dispose(resource);
}

void NativeDevice::MarkDisposed(const NIPtr<NativeRenderTarget>& rtt)
{
    if (mRenderingContext && rtt)
    {
        mRenderingContext->UnsetRenderTargetIfSet(rtt.get());
        mRenderingContext->Dispose(rtt);
    }
}

void NativeDevice::Clear(float r, float g, float b, float a, bool clearDepth)
{
    mRenderingContext->Clear(r, g, b, a, clearDepth);
}

void NativeDevice::ClearTextureUnit(uint32_t unit)
{
    mRenderingContext->ClearTextureUnit(unit);
}

void NativeDevice::RenderQuads(const Internal::MemoryView<float>& vertices, const Internal::MemoryView<unsigned char>& colors,
                               uint32_t vertexCount)
{
    // vertex count size check, also serves as an overflow check
    if (vertexCount > Constants::MAX_BATCH_VERTICES)
    {
        D3D12NI_LOG_ERROR("Provided too many quads to render (%d provided, max %d)", vertexCount / 4, Constants::MAX_BATCH_QUADS);
        return;
    }

    // set up common parameters
    Internal::Matrix<float> wvp = mTransforms.viewProjTransform.Mul(mTransforms.worldTransform);
    mPassthroughVS->SetConstants("WorldViewProj", wvp.Data(), sizeof(Internal::Matrix<float>));

    mRenderingContext->SetCompositeMode(m2DCompositeMode);
    mRenderingContext->SetVertexShader(mPassthroughVS);
    mRenderingContext->SetPixelShader(mCurrent2DShader);
    mRenderingContext->SetCullMode(D3D12_CULL_MODE_NONE);
    mRenderingContext->SetFillMode(D3D12_FILL_MODE_SOLID);

    mRenderingContext->DrawQuads(vertices, colors, vertexCount, mTransforms.worldTransform);
}

void NativeDevice::RenderMeshView(const NIPtr<NativeMeshView>& meshView)
{
    // set shaders and their constants
    mRenderingContext->SetVertexShader(mPhongVS);

    const NIPtr<NativePhongMaterial>& material = meshView->GetMaterial();

    PhongShaderSpec spec;
    spec.mapping = material->GetMappingVariant();
    spec.lightCount = meshView->GetEnabledLightCount();
    spec.specular = material->GetSpecularVariant();
    spec.isSelfIllum = material->IsSelfIllum();

    const NIPtr<Shaders::Shader>& ps = GetPhongPixelShader(spec);
    mRenderingContext->SetPixelShader(ps);

    // Transform data is set by RenderingContext, so here just set the Light data from MeshView
    uint32_t lightCount = meshView->GetEnabledLightCount();

    for (uint32_t i = 0; i < lightCount; ++i)
    {
        mPhongVS->SetConstantsInArray("gLight", i, meshView->GetVSLightSpecPtr(i), sizeof(VSLightSpec));
        ps->SetConstantsInArray("gLight", i, meshView->GetPSLightSpecPtr(i), sizeof(PSLightSpec));
    }

    mPhongVS->SetConstants("gData", &mTransforms, sizeof(Transforms));
    ps->SetConstants("gColor", &meshView->GetPSColorSpec(), sizeof(PSColorSpec));

    const NIPtr<NativeMesh>& mesh = meshView->GetMesh();

    mRenderingContext->SetCompositeMode(CompositeMode::SRC_OVER);
    mRenderingContext->SetCullMode(meshView->GetCullMode());
    mRenderingContext->SetFillMode(meshView->GetFillMode());

    // NOTE: D3D uses setDeviceParametersFor3D to force linear filtering on texture in slot 0
    // and to enforce linear filtering when drawing 3D. This emulates this behavior, but has to
    // be updated when JDK-8092272 is done
    if (material->GetMap(TextureMapType::DIFFUSE))
    {
        material->GetMap(TextureMapType::DIFFUSE)->SetSamplerParameters(TextureWrapMode::REPEAT, true);
    }

    for (uint32_t i = 0; i < static_cast<uint32_t>(TextureMapType::MAX_ENUM); ++i)
    {
        mRenderingContext->SetTexture(i, material->GetMap(static_cast<TextureMapType>(i)));
    }

    mRenderingContext->DrawMeshView(meshView);
}

void NativeDevice::SetCompositeMode(CompositeMode mode)
{
    m2DCompositeMode = mode;
}

void NativeDevice::UnsetPixelShader()
{
    mCurrent2DShader.reset();
}

void NativeDevice::SetPixelShader(const NIPtr<NativeShader>& ps)
{
    mCurrent2DShader = ps;
}

void NativeDevice::SetRenderTarget(const NIPtr<NativeRenderTarget>& target, bool enableDepthTest)
{
    target->SetDepthTestEnabled(enableDepthTest);
    mRenderingContext->SetRenderTarget(target);
}

void NativeDevice::SetScissor(bool enabled, int x1, int y1, int x2, int y2)
{
    D3D12_RECT scissor;
    scissor.left = x1;
    scissor.top = y1;
    scissor.right = x2;
    scissor.bottom = y2;
    mRenderingContext->SetScissor(enabled, scissor);
}

bool NativeDevice::SetShaderConstants(const NIPtr<NativeShader>& shader, const char* name, const void* data, size_t size)
{
    return shader->SetConstants(name, data, size);
}

void NativeDevice::SetTexture(uint32_t unit, const NIPtr<NativeTexture>& texture)
{
    mRenderingContext->SetTexture(unit, texture);
}

void NativeDevice::SetCameraPos(const Coords_XYZW_FLOAT& pos)
{
    mTransforms.cameraPos = pos;
}

void NativeDevice::SetWorldTransform(const Internal::Matrix<float>& matrix)
{
    mTransforms.worldTransform = matrix;
}

void NativeDevice::SetViewProjTransform(const Internal::Matrix<float>& matrix)
{
    mTransforms.viewProjTransform = matrix;
}

// NOTE: Because Blit() might want to fetch some internal underlying objects that SwapChain
// does not expose (ex. a NativeTexture via NativeRenderTarget::GetTexture()) we force source
// to be a NativeRenderTarget. I'm pretty sure we won't need it to be the generic IRenderTarget
// anyway...
bool NativeDevice::Blit(const NIPtr<NativeRenderTarget>& srcRT, const Coords_Box_UINT32& src,
                        const NIPtr<Internal::IRenderTarget>& dstRT, const Coords_Box_UINT32& dst)
{
    if (dstRT->GetMSAASamples() > 1)
    {
        // TODO: D3D12: should it? I'm pretty sure D3D9's StretchRect did not support it.
        D3D12NI_LOG_ERROR("BlitTexture() does not support MSAA destination textures");
        return false;
    }

    if (src.x1 > srcRT->GetWidth() || src.y1 > srcRT->GetHeight() ||
        dst.x1 > dstRT->GetWidth() || dst.y1 > dstRT->GetHeight())
    {
        D3D12NI_LOG_ERROR("Invalid Blit dimensions");
        return false;
    }

    uint32_t srcWidth = src.x1 - src.x0;
    uint32_t srcHeight = src.y1 - src.y0;
    uint32_t dstWidth = dst.x1 - dst.x0;
    uint32_t dstHeight = dst.y1 - dst.y0;

    if (srcWidth == dstWidth && srcHeight == dstHeight)
    {
        if (srcRT->GetMSAASamples() == 1)
        {
            mRenderingContext->CopyTexture(dstRT, dst.x0, dst.y0, srcRT, src.x0, src.y0, srcWidth, srcHeight);
        }
        else
        {
            // use ResolveSubresourceRegion
            mRenderingContext->ResolveRegion(dstRT, dst.x0, dst.y0, srcRT, src.x0, src.y0, srcWidth, srcHeight, dstRT->GetFormat());
        }
    }
    else
    {
        NIPtr<Internal::TextureBase> sourceTexture;
        NIPtr<NativeTexture> intermediateTexture;

        if (srcRT->GetMSAASamples() > 1)
        {
            // create intermediate tex, use ResolveSubresource() to populate it
            intermediateTexture = std::make_shared<NativeTexture>(shared_from_this());
            if (!intermediateTexture->Init(static_cast<uint32_t>(srcRT->GetWidth()), static_cast<uint32_t>(srcRT->GetHeight()), srcRT->GetFormat(),
                D3D12_RESOURCE_FLAG_NONE, TextureUsage::DEFAULT, TextureWrapMode::CLAMP_NOT_NEEDED, 1, false))
            {
                D3D12NI_LOG_ERROR("BlitTexture: Failed to create intermediate texture for source RT resolve");
                return false;
            }

            mRenderingContext->Resolve(intermediateTexture, srcRT, srcRT->GetFormat());
            sourceTexture = intermediateTexture;
        }
        else
        {
            // NOTE: Assumes srcRT will NOT be NativeSwapChain. Otherwise this can desync with Render Thread.
            // (technically it cannot be a SwapChain because we enforced srcRT to be NativeRenderTarget...)
            sourceTexture = srcRT->GetTexture();
        }

        const NIPtr<Shaders::Shader>& blitShader = GetInternalShader("BlitPS");

        // temporarily store whatever context state we have right now
        mRenderingContext->StashParamters();
        mPassthroughVS->SetConstants("WorldViewProj", Internal::Matrix<float>::IDENTITY.Data(), sizeof(Internal::Matrix<float>));

        mRenderingContext->SetVertexShader(mPassthroughVS);
        mRenderingContext->SetPixelShader(blitShader);

        mRenderingContext->SetCullMode(D3D12_CULL_MODE_NONE);
        mRenderingContext->SetFillMode(D3D12_FILL_MODE_SOLID);

        mRenderingContext->SetTexture(0, sourceTexture);
        mRenderingContext->SetRenderTarget(dstRT);
        mRenderingContext->SetCompositeMode(CompositeMode::SRC);

        // prepare quad vertices for blitting
        QuadVertices vertices;
        QuadColors colors;
        AssembleVertexQuadForBlit(src, sourceTexture->GetWidth(), sourceTexture->GetHeight(),
                                  dst, dstRT->GetWidth(), dstRT->GetHeight(),
                                  vertices, colors);

        Internal::MemoryView<float> verticesView(vertices.data(), vertices.size() * sizeof(float));
        Internal::MemoryView<unsigned char> colorsView(colors.data(), colors.size() * sizeof(unsigned char));
        mRenderingContext->DrawQuads(verticesView, colorsView, 4, Internal::Matrix<float>::IDENTITY);

        // restore original context parameters
        mRenderingContext->RestoreStashedParameters();
    }

    return true;
}

bool NativeDevice::ReadTexture(const NIPtr<NativeTexture>& texture, void* buffer, size_t bufferSize,
                               uint32_t srcx, uint32_t srcy, uint32_t srcw, uint32_t srch)
{
    DXGI_FORMAT format = texture->GetFormat();
    size_t bpp = GetDXGIFormatBPP(format);
    size_t targetbpp = (format == DXGI_FORMAT_B8G8R8X8_UNORM) ? 3 : bpp;
    if (srcw == 0 || srch == 0 ||
        srcw > (std::numeric_limits<uint32_t>::max() / srch / targetbpp))
    {
        D3D12NI_LOG_ERROR("Failed to readback Texture, target dimensions are too big");
        return false;
    }

    if (srcw * srch * targetbpp > bufferSize)
    {
        D3D12NI_LOG_ERROR("Failed to readback Texture, target buffer is too small");
        return false;
    }

    if (srcx + srcw > texture->GetWidth() || srcy + srch > texture->GetHeight())
    {
        D3D12NI_LOG_ERROR("Failed to readback Texture, read coordinates exceed texture dimensions");
        return false;
    }

    size_t readbackStride = Internal::Utils::Align<size_t>(srcw * bpp, D3D12_TEXTURE_DATA_PITCH_ALIGNMENT);
    size_t readbackBufferSize = srch * readbackStride;

    // path here is reverse to UpdateTexture but using a READBACK buffer
    // TODO: D3D12: consider using a separate Command Queue for transfer operations
    // TODO: D3D12: maybe this should be more than a simple Readback resource? investigate
    //              performance reasons, maybe we could avoid allocation here
    NIPtr<Internal::Buffer> readbackBuffer = std::make_shared<Internal::Buffer>(shared_from_this());
    if (!readbackBuffer->Init(nullptr, readbackBufferSize, D3D12_HEAP_TYPE_READBACK, D3D12_RESOURCE_STATE_COPY_DEST))
    {
        D3D12NI_LOG_ERROR("Failed to initialize readback buffer for texture read");
        return false;
    }

    mRenderingContext->CopyTextureToBuffer(readbackBuffer, static_cast<uint32_t>(readbackStride), texture, srcx, srcy, srcw, srch);

    // Flush the Command Queue to ensure data was read and wait for it
    Internal::Profiler::Instance().MarkEvent(mProfilerTransferWaitSourceID, Internal::Profiler::Event::Wait);
    if (!mRenderingContext->FlushAndWait(CheckpointType::TRANSFER))
    {
        D3D12NI_LOG_ERROR("Failed to submit texture read operation to Render Thread");
        return false;
    }

    void* readbackPtr = readbackBuffer->Map();
    if (readbackPtr == nullptr)
    {
        D3D12NI_LOG_ERROR("Failed to map readback buffer for texture read");
        return false;
    }

    if (format == DXGI_FORMAT_B8G8R8X8_UNORM)
    {
        size_t srcStrideElems = readbackStride / bpp;

        for (size_t y = 0; y < srch; ++y)
        {
            const Pixel_BGRA8_UNORM* srcp = reinterpret_cast<const Pixel_BGRA8_UNORM*>(readbackPtr) + (y * srcStrideElems);
            Pixel_RGB8_UNORM* dstp = reinterpret_cast<Pixel_RGB8_UNORM*>(buffer) + (y * srcw);
            for (size_t x = 0; x < srcw; ++x)
            {
                dstp[x].r = srcp[x].r;
                dstp[x].g = srcp[x].g;
                dstp[x].b = srcp[x].b;
            }
        }
    }
    else
    {
        for (size_t y = 0; y < srch; ++y)
        {
            const uint8_t* srcPtr = reinterpret_cast<const uint8_t*>(readbackPtr) + (y * readbackStride);
            uint8_t* dstPtr = reinterpret_cast<uint8_t*>(buffer) + (y * srcw * bpp);

            memcpy(dstPtr, srcPtr, srcw * bpp);
        }
    }

    readbackBuffer->Unmap();
    mRenderingContext->Dispose(readbackBuffer);

    return true;
}

bool NativeDevice::UpdateTexture(const NIPtr<NativeTexture>& texture, const void* data, size_t dataSizeBytes, PixelFormat srcFormat,
                                 uint32_t dstx, uint32_t dsty, uint32_t srcx, uint32_t srcy, uint32_t srcw, uint32_t srch, uint32_t srcStride)
{
    if (!mRenderingContext->UpdateTexture(texture, dstx, dsty, data, dataSizeBytes, srcx, srcy, srcw, srch, srcStride, srcFormat))
    {
        return false;
    }

    return mRenderingContext->GenerateMipmaps(texture);
}

bool NativeDevice::PrepareSwapChain(const NIPtr<NativeSwapChain>& swapChain, const D3D12_RECT& dirtyRegion)
{
    return mRenderingContext->PrepareSwapChain(swapChain, dirtyRegion);
}

bool NativeDevice::Present(const NIPtr<NativeSwapChain>& swapChain)
{
    return mRenderingContext->Present(swapChain);
}

void NativeDevice::FinishFrame()
{
    mRenderingContext->FinishFrame();

    mFrameCounter++;
    Internal::Profiler::Instance().MarkFrameEnd();
    Internal::Profiler::Instance().TimingEnd(mProfilerFrameTimeID);
    Internal::Profiler::Instance().TimingStart(mProfilerFrameTimeID);
}

bool NativeDevice::WaitUntilIdle()
{
    if (mRenderingContext)
    {
        if (!mRenderingContext->WaitForGPU())
        {
            D3D12NI_LOG_ERROR("Failed to wait until GPU is idle");
            return false;
        }
    }

    return true;
}

void NativeDevice::ExamineDeviceRemoved()
{
    if (mDeviceWorks && mDebugContext)
    {
        mDeviceWorks = mDebugContext->ExamineDeviceRemoved();
    }
}

void NativeDevice::ReportErrorMessages()
{
    if (mDebugContext) mDebugContext->ReportErrorMessages();
}


} // namespace D3D12


#ifdef __cplusplus
extern "C"
{
#endif

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nReleaseNativeObject
    (JNIEnv* env, jobject obj, jlong ptr)
{
    if (!ptr) return;

    // Device needs an explicit Release() call to free up internal objects
    // This makes sure those objects remove cleanly while NativeDevice and its
    // D3D12DevicePtr are still valid
    D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->Release();
    D3D12::FreeNIObject<D3D12::NativeDevice>(ptr);
}

JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nIsDeviceReady
    (JNIEnv* env, jobject obj, jlong ptr)
{
    if (!ptr) return false;

    return D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->IsDeviceReady();
}

JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nCheckFormatSupport
    (JNIEnv* env, jobject obj, jlong ptr, jint format)
{
    if (!ptr) return false;
    if (format < 0) return false;

    return D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->CheckFormatSupport(static_cast<DXGI_FORMAT>(format));
}

JNIEXPORT jlong JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nCreateMesh
    (JNIEnv* env, jobject obj, jlong ptr)
{
    if (!ptr) return 0;

    return reinterpret_cast<jlong>(D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->CreateMesh());
}

JNIEXPORT jlong JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nCreateMeshView
    (JNIEnv* env, jobject obj, jlong ptr, jlong meshPtr)
{
    if (!ptr) return 0;
    if (!meshPtr) return 0;

    const D3D12::NIPtr<D3D12::NativeMesh>& mesh = D3D12::GetNIObject<D3D12::NativeMesh>(meshPtr);

    if (!mesh) return 0;

    return reinterpret_cast<jlong>(D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->CreateMeshView(mesh));
}

JNIEXPORT jlong JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nCreatePhongMaterial
    (JNIEnv* env, jobject obj, jlong ptr)
{
    if (!ptr) return 0;

    return reinterpret_cast<jlong>(D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->CreatePhongMaterial());
}

JNIEXPORT jlong JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nCreateRenderTarget
    (JNIEnv* env, jobject obj, jlong ptr, jlong texturePtr, jboolean enableDirtyBBox)
{
    if (!ptr) return 0;
    if (!texturePtr) return 0;

    const D3D12::NIPtr<D3D12::NativeTexture>& texture = D3D12::GetNIObject<D3D12::NativeTexture>(texturePtr);
    if (!texture) return 0;

    return reinterpret_cast<jlong>(D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->CreateRenderTarget(texture, (enableDirtyBBox > 0)));
}

JNIEXPORT jlong JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nCreateShader
    (JNIEnv* env, jobject obj, jlong ptr, jstring name, jobject codeBBuf)
{
    if (!ptr) return 0;

    void* codeBuf = env->GetDirectBufferAddress(codeBBuf);
    jlong codeBufSize = env->GetDirectBufferCapacity(codeBBuf);
    if (codeBuf == nullptr || codeBufSize <= 0)
    {
        D3D12NI_LOG_ERROR("Failed to get shader code buffer address");
        return 0;
    }

    D3D12::Internal::JNIString nameJString(env, name);
    if (nameJString == nullptr)
    {
        D3D12NI_LOG_ERROR("Failed to get shader name string");
        return false;
    }

    std::string nameStr(nameJString);

    return reinterpret_cast<jlong>(
        D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->CreateShader(nameStr, codeBuf, static_cast<UINT>(codeBufSize))
    );
}

JNIEXPORT jlong JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nCreateTexture
    (JNIEnv* env, jobject obj, jlong ptr, jint width, jint height, jint format,
     jint usage, jint wrapMode, jint samples, jboolean useMipmap, jboolean isRTT)
{
    if (!ptr) return 0;

    D3D12_RESOURCE_FLAGS flags = D3D12_RESOURCE_FLAG_NONE;
    if (isRTT) flags |= D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET;

    return reinterpret_cast<jlong>(D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->CreateTexture(
        static_cast<UINT>(width), static_cast<UINT>(height), static_cast<DXGI_FORMAT>(format), flags,
        static_cast<D3D12::TextureUsage>(usage), static_cast<D3D12::TextureWrapMode>(wrapMode),
        samples, static_cast<bool>(useMipmap)
    ));
}

JNIEXPORT jint JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nGetMaximumMSAASampleSize
    (JNIEnv* env, jobject obj, jlong ptr, jint dxgiFormat)
{
    if (!ptr) return 0;

    return static_cast<jint>(D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->GetMaximumMSAASampleSize(static_cast<DXGI_FORMAT>(dxgiFormat)));
}

JNIEXPORT jint JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nGetMaximumTextureSize
    (JNIEnv* env, jobject obj, jlong ptr)
{
    if (!ptr) return 0;

    return static_cast<jint>(D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->GetMaximumTextureSize());
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nClear
    (JNIEnv* env, jobject obj, jlong ptr, jfloat r, jfloat g, jfloat b, jfloat a, jboolean clearDepth)
{
    if (!ptr) return;

    D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->Clear(r, g, b, a, static_cast<bool>(clearDepth));
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nRenderQuads
    (JNIEnv* env, jobject obj, jlong ptr, jfloatArray vertices, jbyteArray colors, jint elementCount)
{
    if (!ptr) return;
    if (elementCount <= 0) return;

    D3D12::Internal::JNIBuffer<jfloatArray> vertsArray(env, nullptr, vertices);
    D3D12::Internal::JNIBuffer<jbyteArray> colorsArray(env, nullptr, colors);
    if (!vertsArray || !colorsArray) return;

    uint32_t elems = static_cast<uint32_t>(elementCount);
    if (elems > (std::numeric_limits<uint32_t>::max() / D3D12NI_FLOATS_PER_2D_VERTEX) ||
        elems * D3D12NI_FLOATS_PER_2D_VERTEX > vertsArray.Count() ||
        elems * D3D12NI_CHARS_PER_2D_VERTEX > colorsArray.Count()) return;

    D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->RenderQuads(
        D3D12::Internal::MemoryView<float>(reinterpret_cast<const float*>(vertsArray.Data()), vertsArray.Size()),
        D3D12::Internal::MemoryView<unsigned char>(reinterpret_cast<const unsigned char*>(colorsArray.Data()), colorsArray.Size()),
        elems
    );
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nRenderMeshView
    (JNIEnv* env, jobject obj, jlong ptr, jlong meshViewPtr)
{
    if (!ptr) return;
    if (!meshViewPtr) return;

    const D3D12::NIPtr<D3D12::NativeMeshView>& meshView = D3D12::GetNIObject<D3D12::NativeMeshView>(meshViewPtr);
    if (!meshView) return;

    D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->RenderMeshView(meshView);
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nSetCompositeMode
    (JNIEnv* env, jobject obj, jlong ptr, jint compositeMode)
{
    if (!ptr) return;

    if (compositeMode < static_cast<jint>(D3D12::CompositeMode::CLEAR) ||
        compositeMode >= static_cast<jint>(D3D12::CompositeMode::MAX_ENUM))
    {
        D3D12NI_LOG_ERROR("Invalid compositeMode received on native backend");
        return;
    }

    D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->SetCompositeMode(static_cast<D3D12::CompositeMode>(compositeMode));
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nSetPixelShader
    (JNIEnv* env, jobject obj, jlong ptr, jlong pixelShaderPtr)
{
    if (!ptr) return;

    if (!pixelShaderPtr)
    {
        // NOTE: I didn't observe this path being taken by Prism ever. It might not work
        //       or cause some unexpected issues if it ever gets hit.
        D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->UnsetPixelShader();
        return;
    }

    const D3D12::NIPtr<D3D12::NativeShader>& ps = D3D12::GetNIObject<D3D12::NativeShader>(pixelShaderPtr);
    if (!ps) return;

    D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->SetPixelShader(ps);
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nSetRenderTarget
    (JNIEnv* env, jobject obj, jlong ptr, jlong renderTargetPtr, jboolean enableDepthTest)
{
    if (!ptr) return;
    if (!renderTargetPtr) return;

    const D3D12::NIPtr<D3D12::NativeRenderTarget>& renderTarget = D3D12::GetNIObject<D3D12::NativeRenderTarget>(renderTargetPtr);
    if (!renderTarget) return;

    D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->SetRenderTarget(renderTarget, static_cast<bool>(enableDepthTest));
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nSetScissor
    (JNIEnv* env, jobject obj, jlong ptr, jboolean enabled, jint x1, jint y1, jint x2, jint y2)
{
    if (!ptr) return;

    D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->SetScissor(enabled, x1, y1, x2, y2);
}

JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nSetShaderConstantsF
    (JNIEnv* env, jobject obj, jlong ptr, jlong shaderPtr, jstring name, jobject floatBuf, jint offset, jint count)
{
    if (!ptr) return false;
    if (!shaderPtr) return false;
    if (!name) return false;
    if (!floatBuf) return false;
    if (offset < 0) return false;
    if (count <= 0) return false;

    const D3D12::NIPtr<D3D12::NativeShader>& shader = D3D12::GetNIObject<D3D12::NativeShader>(shaderPtr);
    if (!shader) return false;

    D3D12::Internal::JNIString nameJStr(env, name);
    D3D12::Internal::JNIBuffer<jfloatArray> buffer(env, floatBuf, nullptr);
    if (!buffer) return false;

    size_t sizeBytes = static_cast<size_t>(count) * sizeof(jfloat);
    size_t offsetBytes = static_cast<size_t>(offset) * sizeof(jfloat);
    if (offsetBytes > std::numeric_limits<size_t>::max() - sizeBytes ||
        offsetBytes + sizeBytes > buffer.Size()) return false;

    const uint8_t* srcPtr = reinterpret_cast<const uint8_t*>(buffer.Data()) + offsetBytes;

    return D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->SetShaderConstants(shader, nameJStr, srcPtr, sizeBytes);
}

JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nSetShaderConstantsI
    (JNIEnv* env, jobject obj, jlong ptr, jlong shaderPtr, jstring name, jobject intBuf, jint offset, jint count)
{
    if (!ptr) return false;
    if (!shaderPtr) return false;
    if (!name) return false;
    if (!intBuf) return false;
    if (offset < 0) return false;
    if (count <= 0) return false;

    const D3D12::NIPtr<D3D12::NativeShader>& shader = D3D12::GetNIObject<D3D12::NativeShader>(shaderPtr);
    if (!shader) return false;

    D3D12::Internal::JNIString nameJStr(env, name);
    D3D12::Internal::JNIBuffer<jintArray> buffer(env, intBuf, nullptr);
    if (!buffer) return false;

    size_t sizeBytes = static_cast<size_t>(count) * sizeof(jint);
    size_t offsetBytes = static_cast<size_t>(offset) * sizeof(jint);
    if (offsetBytes > std::numeric_limits<size_t>::max() - sizeBytes ||
        offsetBytes + sizeBytes > buffer.Size()) return false;

    const uint8_t* srcPtr = reinterpret_cast<const uint8_t*>(buffer.Data()) + offsetBytes;

    return D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->SetShaderConstants(shader, nameJStr, srcPtr, sizeBytes);
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nSetTexture
    (JNIEnv* env, jobject obj, jlong ptr, jint unit, jlong texturePtr)
{
    if (!ptr) return;
    if (unit < 0 || unit >= D3D12::Constants::MAX_TEXTURE_UNITS) return;

    if (texturePtr)
    {
        const D3D12::NIPtr<D3D12::NativeTexture>& tex = D3D12::GetNIObject<D3D12::NativeTexture>(texturePtr);
        if (!tex) return;

        D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->SetTexture(static_cast<uint32_t>(unit), tex);
    }
    else
    {
        D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->ClearTextureUnit(static_cast<uint32_t>(unit));
    }
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nSetCameraPos
    (JNIEnv* env, jobject obj, jlong ptr, jdouble x, jdouble y, jdouble z)
{
    if (!ptr) return;

    D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->SetCameraPos(D3D12::Coords_XYZW_FLOAT{
        static_cast<float>(x), static_cast<float>(y), static_cast<float>(z), 1.0f
    });
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nSetViewProjTransform
    (JNIEnv* env, jobject obj, jlong ptr,
     jdouble m00, jdouble m01, jdouble m02, jdouble m03,
     jdouble m10, jdouble m11, jdouble m12, jdouble m13,
     jdouble m20, jdouble m21, jdouble m22, jdouble m23,
     jdouble m30, jdouble m31, jdouble m32, jdouble m33)
{
    if (!ptr) return;

    D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->SetViewProjTransform(D3D12::Internal::Matrix<float>(
        static_cast<float>(m00), static_cast<float>(m01), static_cast<float>(m02), static_cast<float>(m03),
        static_cast<float>(m10), static_cast<float>(m11), static_cast<float>(m12), static_cast<float>(m13),
        static_cast<float>(m20), static_cast<float>(m21), static_cast<float>(m22), static_cast<float>(m23),
        static_cast<float>(m30), static_cast<float>(m31), static_cast<float>(m32), static_cast<float>(m33)
    ));
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nSetWorldTransform
    (JNIEnv* env, jobject obj, jlong ptr,
     jdouble m00, jdouble m01, jdouble m02, jdouble m03,
     jdouble m10, jdouble m11, jdouble m12, jdouble m13,
     jdouble m20, jdouble m21, jdouble m22, jdouble m23,
     jdouble m30, jdouble m31, jdouble m32, jdouble m33)
{
    if (!ptr) return;

    D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->SetWorldTransform(D3D12::Internal::Matrix<float>(
        static_cast<float>(m00), static_cast<float>(m01), static_cast<float>(m02), static_cast<float>(m03),
        static_cast<float>(m10), static_cast<float>(m11), static_cast<float>(m12), static_cast<float>(m13),
        static_cast<float>(m20), static_cast<float>(m21), static_cast<float>(m22), static_cast<float>(m23),
        static_cast<float>(m30), static_cast<float>(m31), static_cast<float>(m32), static_cast<float>(m33)
    ));
}

JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nBlit
    (JNIEnv* env, jobject obj, jlong ptr, jlong srcRTPtr, jint srcX0, jint srcY0, jint srcX1, jint srcY1,
                                          jlong dstRTPtr, jint dstX0, jint dstY0, jint dstX1, jint dstY1)
{
    if (!ptr) return false;
    if (!srcRTPtr) return false;
    if (!dstRTPtr) return false;
    if (srcX0 < 0 || srcY0 < 0 || srcX1 < 0 || srcY1 < 0) return false;
    if (srcX0 > srcX1 || srcY0 > srcY1) return false;
    if (dstX0 < 0 || dstY0 < 0 || dstX1 < 0 || dstY1 < 0) return false;
    if (dstX0 > dstX1 || dstY0 > dstY1) return false;

    const D3D12::NIPtr<D3D12::NativeRenderTarget>& srcRT = D3D12::GetNIObject<D3D12::NativeRenderTarget>(srcRTPtr);
    if (!srcRT) return false;

    const D3D12::NIPtr<D3D12::NativeRenderTarget>& dstRT = D3D12::GetNIObject<D3D12::NativeRenderTarget>(dstRTPtr);
    if (!dstRT) return false;

    D3D12::Coords_Box_UINT32 srcBox{static_cast<uint32_t>(srcX0), static_cast<uint32_t>(srcY0),
                                    static_cast<uint32_t>(srcX1), static_cast<uint32_t>(srcY1)};
    D3D12::Coords_Box_UINT32 dstBox{static_cast<uint32_t>(dstX0), static_cast<uint32_t>(dstY0),
                                    static_cast<uint32_t>(dstX1), static_cast<uint32_t>(dstY1)};

    return D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->Blit(srcRT, srcBox, dstRT, dstBox);
}

JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nBlitToSwapChain
    (JNIEnv* env, jobject obj, jlong ptr, jlong srcRTPtr, jint srcX0, jint srcY0, jint srcX1, jint srcY1,
                                          jlong dstSwapChainPtr, jint dstX0, jint dstY0, jint dstX1, jint dstY1)
{
    if (!ptr) return false;
    if (!srcRTPtr) return false;
    if (!dstSwapChainPtr) return false;
    if (srcX0 < 0 || srcY0 < 0 || srcX1 < 0 || srcY1 < 0) return false;
    if (srcX0 > srcX1 || srcY0 > srcY1) return false;
    if (dstX0 < 0 || dstY0 < 0 || dstX1 < 0 || dstY1 < 0) return false;
    if (dstX0 > dstX1 || dstY0 > dstY1) return false;

    const D3D12::NIPtr<D3D12::NativeRenderTarget>& srcRT = D3D12::GetNIObject<D3D12::NativeRenderTarget>(srcRTPtr);
    if (!srcRT) return false;

    const D3D12::NIPtr<D3D12::NativeSwapChain>& dstSwapChain = D3D12::GetNIObject<D3D12::NativeSwapChain>(dstSwapChainPtr);
    if (!dstSwapChain) return false;

    D3D12::Coords_Box_UINT32 srcBox{static_cast<uint32_t>(srcX0), static_cast<uint32_t>(srcY0),
                                    static_cast<uint32_t>(srcX1), static_cast<uint32_t>(srcY1)};
    D3D12::Coords_Box_UINT32 dstBox{static_cast<uint32_t>(dstX0), static_cast<uint32_t>(dstY0),
                                    static_cast<uint32_t>(dstX1), static_cast<uint32_t>(dstY1)};

    return D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->Blit(srcRT, srcBox, dstSwapChain, dstBox);
}

JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nReadTextureB
    (JNIEnv* env, jobject obj, jlong ptr, jlong srcTexturePtr, jobject buf, jbyteArray array, jint x, jint y, jint w, jint h)
{
    if (!ptr) return false;
    if (!srcTexturePtr) return false;
    if (x < 0 || y < 0 || w < 0 || h < 0) return false;

    const D3D12::NIPtr<D3D12::NativeTexture>& tex = D3D12::GetNIObject<D3D12::NativeTexture>(srcTexturePtr);
    if (!tex) return false;

    D3D12::Internal::JNIBuffer<jbyteArray> data(env, buf, array);
    if (!data) return false;

    return D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->ReadTexture(tex, data.Data(), data.Size(),
        static_cast<UINT>(x), static_cast<UINT>(y), static_cast<UINT>(w), static_cast<UINT>(h)
    );
}

JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nReadTextureI
    (JNIEnv* env, jobject obj, jlong ptr, jlong srcTexturePtr, jobject buf, jintArray array, jint x, jint y, jint w, jint h)
{
    if (!ptr) return false;
    if (!srcTexturePtr) return false;
    if (x < 0 || y < 0 || w < 0 || h < 0) return false;

    const D3D12::NIPtr<D3D12::NativeTexture>& tex = D3D12::GetNIObject<D3D12::NativeTexture>(srcTexturePtr);
    if (!tex) return false;

    D3D12::Internal::JNIBuffer<jintArray> data(env, buf, array);
    if (!data) return false;

    return D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->ReadTexture(tex, data.Data(), data.Size(),
        static_cast<UINT>(x), static_cast<UINT>(y), static_cast<UINT>(w), static_cast<UINT>(h)
    );
}

JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nUpdateTextureF
    (JNIEnv* env, jobject obj, jlong ptr, jlong texturePtr, jobject dataBuf, jfloatArray dataArray, jint pixelFormat,
     jint dstx, jint dsty, jint srcx, jint srcy, jint srcw, jint srch, jint srcscan)
{
    if (!ptr) return false;
    if (!texturePtr) return false;
    if (dstx < 0 || dsty < 0 || srcx < 0 || srcy < 0 || srcw <= 0 || srch <= 0) return false;

    const D3D12::NIPtr<D3D12::NativeTexture>& tex = D3D12::GetNIObject<D3D12::NativeTexture>(texturePtr);
    if (!tex) return false;

    D3D12::Internal::JNIBuffer<jfloatArray> data(env, dataBuf, dataArray);
    if (!data) return false;

    return D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->UpdateTexture(tex, data.Data(), data.Size(),
        static_cast<D3D12::PixelFormat>(pixelFormat), dstx, dsty, srcx, srcy, srcw, srch, srcscan
    );
}

JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nUpdateTextureI
    (JNIEnv* env, jobject obj, jlong ptr, jlong texturePtr, jobject dataBuf, jintArray dataArray, jint pixelFormat,
     jint dstx, jint dsty, jint srcx, jint srcy, jint srcw, jint srch, jint srcscan)
{
    if (!ptr) return false;
    if (!texturePtr) return false;
    if (dstx < 0 || dsty < 0 || srcx < 0 || srcy < 0 || srcw <= 0 || srch <= 0) return false;

    const D3D12::NIPtr<D3D12::NativeTexture>& tex = D3D12::GetNIObject<D3D12::NativeTexture>(texturePtr);
    if (!tex) return false;

    D3D12::Internal::JNIBuffer<jintArray> data(env, dataBuf, dataArray);
    if (!data) return false;

    return D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->UpdateTexture(tex, data.Data(), data.Size(),
        static_cast<D3D12::PixelFormat>(pixelFormat), dstx, dsty, srcx, srcy, srcw, srch, srcscan
    );
}

JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nUpdateTextureB
    (JNIEnv* env, jobject obj, jlong ptr, jlong texturePtr, jobject dataBuf, jbyteArray dataArray, jint pixelFormat,
     jint dstx, jint dsty, jint srcx, jint srcy, jint srcw, jint srch, jint srcscan)
{
    if (!ptr) return false;
    if (!texturePtr) return false;

    const D3D12::NIPtr<D3D12::NativeTexture>& tex = D3D12::GetNIObject<D3D12::NativeTexture>(texturePtr);
    if (!tex) return false;

    D3D12::Internal::JNIBuffer<jbyteArray> data(env, dataBuf, dataArray);
    if (!data) return false;

    return D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->UpdateTexture(tex, data.Data(), data.Size(),
        static_cast<D3D12::PixelFormat>(pixelFormat), dstx, dsty, srcx, srcy, srcw, srch, srcscan
    );
}

JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nPrepareSwapChain
    (JNIEnv* env, jobject obj, jlong ptr, jlong swapChainPtr, jlong left, jlong top, jlong right, jlong bottom)
{
    if (!ptr) return false;
    if (!swapChainPtr) return false;

    const D3D12::NIPtr<D3D12::NativeSwapChain>& swapChain = D3D12::GetNIObject<D3D12::NativeSwapChain>(swapChainPtr);
    if (!swapChain) return false;

    D3D12_RECT dirtyRegion {
        static_cast<LONG>(left),
        static_cast<LONG>(top),
        static_cast<LONG>(right),
        static_cast<LONG>(bottom)
    };

    return D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->PrepareSwapChain(swapChain, dirtyRegion);
}

JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nPresent
    (JNIEnv* env, jobject obj, jlong ptr, jlong swapChainPtr)
{
    if (!ptr) return false;
    if (!swapChainPtr) return false;

    const D3D12::NIPtr<D3D12::NativeSwapChain>& swapChain = D3D12::GetNIObject<D3D12::NativeSwapChain>(swapChainPtr);
    if (!swapChain) return false;

    return D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->Present(swapChain);
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nFinishFrame
    (JNIEnv* env, jobject obj, jlong ptr)
{
    if (!ptr) return;

    D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->FinishFrame();
}

JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeDevice_nWaitUntilIdle
    (JNIEnv* env, jobject obj, jlong ptr)
{
    if (!ptr) return false;

    return D3D12::GetNIObject<D3D12::NativeDevice>(ptr)->WaitUntilIdle();
}

#ifdef __cplusplus
}
#endif
