/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

#include <cstdint>
#include <functional>


namespace D3D12 {

// C-style-allocated unique ptr
// used ex. to dynamically allocate D3D12_MESSAGE in D3D12Debug.cpp because of message size being dynamic
struct CFree
{
    void operator()(void* p) const noexcept
    {
        std::free(p);
    }
};

template <typename T>
using UniqueCPtr = std::unique_ptr<T, CFree>;

template <typename T>
UniqueCPtr<T> MakeUniqueCPtr(std::size_t size) noexcept
{
    return UniqueCPtr<T>(reinterpret_cast<T*>(std::malloc(size)));
}

// smart-pointer container shorthand for D3D12 objects
template <typename T>
using Ptr = Microsoft::WRL::ComPtr<T>;

// aliases for DXGI-specific objects used by the backend
// this is to help with versioning the interfaces in a unified manner
using DXGIFactoryPtr = Ptr<IDXGIFactory2>;
using DXGIDebugPtr = Ptr<IDXGIDebug>;
using DXGIInfoQueuePtr = Ptr<IDXGIInfoQueue>;
using DXGISwapChainPtr = Ptr<IDXGISwapChain3>;
using DXGIAdapterPtr = Ptr<IDXGIAdapter1>;
using DXGIOutputPtr = Ptr<IDXGIOutput>;

// Non-D3D-specific Blob object pointer
using D3DBlobPtr = Ptr<ID3DBlob>;

// aliases for D3D12-specific interfaces
using D3D12DevicePtr = Ptr<ID3D12Device4>;
using D3D12CommandAllocatorPtr = Ptr<ID3D12CommandAllocator>;
using D3D12CommandQueuePtr = Ptr<ID3D12CommandQueue>;
using D3D12DeviceRemovedExtendedData = Ptr<ID3D12DeviceRemovedExtendedData>;
using D3D12DeviceRemovedExtendedDataSettings = Ptr<ID3D12DeviceRemovedExtendedDataSettings>;
using D3D12DescriptorHeapPtr = Ptr<ID3D12DescriptorHeap>;
using D3D12FencePtr = Ptr<ID3D12Fence>;
using D3D12GraphicsCommandListPtr = Ptr<ID3D12GraphicsCommandList1>;
using D3D12PageablePtr = Ptr<ID3D12Pageable>;
using D3D12PipelineStatePtr = Ptr<ID3D12PipelineState>;
using D3D12ResourcePtr = Ptr<ID3D12Resource>;
using D3D12RootSignaturePtr = Ptr<ID3D12RootSignature>;

using D3D12DebugPtr = Ptr<ID3D12Debug3>;
using D3D12InfoQueuePtr = Ptr<ID3D12InfoQueue>;
using D3D12DebugDevicePtr = Ptr<ID3D12DebugDevice2>;

// smart-pointer container for internal objects
template <typename T>
using NIPtr = std::shared_ptr<T>;

// weak reference to NIPtr objects - for places like PhongMaterial
template <typename T>
using NIWeakPtr = std::weak_ptr<T>;

template <typename T>
bool WeakPtrIsEmpty(const NIWeakPtr<T>& ptr)
{
    return !ptr.owner_before(NIWeakPtr<T>()) && !NIWeakPtr<T>().owner_before(ptr);
}

// forward declaration of potentially the most important objects
class NativeInstance;
class NativeDevice;

// const char* comparator for (unordered_)maps
// without it maps hash and compare raw pointers
class CStringComparer
{
public:
    bool operator()(const char* const a, const char* const b) const
    {
        return std::strcmp(a, b) < 0;
    }
};

// Helper structs for in case we need to shuffle components
// ex. see NativeDevice::UpdateTexture()
struct Coords_XY_FLOAT
{
    float x;
    float y;
};

struct Coords_XYZ_FLOAT
{
    float x;
    float y;
    float z;
};

struct Coords_XYZW_FLOAT
{
    float x;
    float y;
    float z;
    float w;
};

struct Coords_UV_FLOAT
{
    float u;
    float v;
};

struct Coords_Box_UINT32
{
    uint32_t x0;
    uint32_t y0;
    uint32_t x1;
    uint32_t y1;
};

struct Pixel_RGB8_UNORM
{
    uint8_t r;
    uint8_t g;
    uint8_t b;
};

struct Pixel_RGBA8_UNORM
{
    uint8_t r;
    uint8_t g;
    uint8_t b;
    uint8_t a;
};

struct Pixel_RGBA32_FLOAT
{
    float r;
    float g;
    float b;
    float a;
};

struct Pixel_BGRA8_UNORM
{
    uint8_t b;
    uint8_t g;
    uint8_t r;
    uint8_t a;
};

// Light spec matching Shader definitions
// TODO: D3D12: cleanup - try avoiding code duplication and share below structs with HLSL headers
struct VSLightSpec
{
    Coords_XYZW_FLOAT pos;
    Coords_XYZW_FLOAT normDir;
};

struct PSColorSpec
{
    Pixel_RGBA32_FLOAT diffuse;
    Pixel_RGBA32_FLOAT specular;
    Pixel_RGBA32_FLOAT ambientLight;
};

struct PSLightSpec
{
    Pixel_RGBA32_FLOAT color;
    Pixel_RGBA32_FLOAT attenuation; // { r=constant, g=linear, b=quadratic, a=on/off }
    Pixel_RGBA32_FLOAT maxRange; // { r=maxRange, _, _, _ }
    Pixel_RGBA32_FLOAT spotLightFactors; // precalculated factors based on input data
                                         // { r=cos(outer), g=cos(inner)-cos(outer), b=falloff, _ }
};

// Phong Shader configuration
enum class PhongShaderMappingVariant: uint8_t
{
    SIMPLE,
    BUMP
};

enum class PhongShaderSpecularVariant: uint8_t
{
    NONE,
    TEXTURE,
    COLOR,
    MIX
};

struct PhongShaderSpec
{
    uint32_t lightCount;
    bool isSelfIllum;
    PhongShaderMappingVariant mapping;
    PhongShaderSpecularVariant specular;
};

// vertex definition for 2D
struct Vertex_2D
{
    Coords_XYZ_FLOAT pos;
    Pixel_RGBA8_UNORM color;
    Coords_UV_FLOAT uv1;
    Coords_UV_FLOAT uv2;
};

// Determines which rendering mode Shader belongs to.
// Mostly used to ensure vertex-pixel shader combination is correct and to
// initialize PSOs correctly
enum class ShaderPipelineMode: unsigned char
{
    UI_2D = 0,
    PHONG_3D,
    COMPUTE,
    MAX_ENUM
};

// Determines types of waits CheckpointQueue performs
// See CheckpointQueue.hpp
enum class CheckpointType: uint32_t
{
    ALL = 0, // when passed to CheckpointQueue::WaitForNextCheckpoint it will empty the queue
    MIDFRAME = (1 << 0),
    ENDFRAME = (1 << 1),
    TRANSFER = (1 << 2),
    ANY = 0xFFFFFFFF, // For situations where checkpoint type doesn't matter, ex. RingContainer
};

// for objects needing GPU pipeline synchronization (ex. Ring Container)
// RenderThread will pass those callbacks to them upon initialization
using CheckpointCallback = std::function<void(CheckpointType)>;

// like above but Signal will return a fence value. Seems like only SwapChain needs it.
// See NativeSwapChain::Present() for more details
using SignalCallback = std::function<uint64_t(CheckpointType)>;

// mirrors CompositeMode.java
enum class CompositeMode: unsigned char
{
    CLEAR = 0,
    SRC,
    SRC_OVER,
    DST_OUT,
    ADD,
    MAX_ENUM
};

// mirrors CullFace.java
enum class CullFace: unsigned char
{
    NONE = 0,
    BACK,
    FRONT,
    MAX_ENUM,
};

// mirrors MapType in PhongMaterial.java
enum class TextureMapType: unsigned char
{
    DIFFUSE = 0,
    SPECULAR,
    BUMP,
    SELF_ILLUM,
    MAX_ENUM,
};

// mirrors PixelFormat in PixelFormat.java
enum class PixelFormat: unsigned char
{
    INT_ARGB_PRE = 0,
    BYTE_BGRA_PRE,
    BYTE_RGB,
    BYTE_GRAY,
    BYTE_ALPHA,
    MULTI_YCbCr_42,
    BYTE_APPLE_422,
    FLOAT_XYZW,
};

// mirrors Prism's Texture.Usage enum
enum class TextureUsage: unsigned int
{
    DEFAULT = 0,
    DYNAMIC,
    STATIC,
};

// mirrors Prism's Texture.WrapMode enum, excluding _SIMULATED modes.
// _SIMULATED modes borrow from non-SIMULATED ones and are processed on
// Java-side.
enum class TextureWrapMode: unsigned int
{
    CLAMP_NOT_NEEDED = 0,
    CLAMP_TO_ZERO,
    CLAMP_TO_EDGE,
    REPEAT,
    MAX_ENUM
};

// decides how Shader resource should be bound
// see D3D12Shader.hpp
enum class ResourceAssignmentType: uint32_t
{
    DESCRIPTOR,
    DESCRIPTOR_TABLE_TEXTURES,
    DESCRIPTOR_TABLE_CBUFFERS,
    DESCRIPTOR_TABLE_SAMPLERS
};

} // namespace D3D12
