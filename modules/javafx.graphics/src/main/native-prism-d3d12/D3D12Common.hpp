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

#define NOMINMAX

// TODO: D3D12: This should be un-silenced and conversion should be done via WinAPI
#ifndef _SILENCE_CXX17_CODECVT_HEADER_DEPRECATION_WARNING
#define _SILENCE_CXX17_CODECVT_HEADER_DEPRECATION_WARNING
#endif

#include <d3d12.h>
#include <dxgi1_4.h>
#include <dxgidebug.h>
#include <dxcapi.h>
#include <d3d12shader.h>
#include <comdef.h>
#include <wrl/client.h>
#include <jni.h>
#include <array>
#include <memory>
#include <string>
#include <cstring>
#include <sstream>
#include <cmath>
#include <cassert>
#include <algorithm>
#include <functional>

#include "internal/D3D12Logger.hpp"

#include "Common_D3D12ShaderResourceDataHeader.hpp"

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


// currently supported D3D12 Feature Level
#define D3D12NI_FEATURE_LEVEL D3D_FEATURE_LEVEL_12_0

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

// forward declaration of potentially the most important objects
class NativeInstance;
class NativeDevice;

// fast & easy allocation routines
template <typename T>
NIPtr<T>* AllocateNIObject()
{
    NIPtr<T>* newPtr = new NIPtr<T>();
    *newPtr = std::make_shared<T>();
    return newPtr;
}

template <typename T>
NIPtr<T>* AllocateNIInstanceObject(const NIPtr<NativeInstance>& instance)
{
    NIPtr<T>* newPtr = new NIPtr<T>();
    *newPtr = std::make_shared<T>(instance);
    return newPtr;
}

template <typename T>
NIPtr<T>* AllocateNIDeviceObject(const NIPtr<NativeDevice>& device)
{
    NIPtr<T>* newPtr = new NIPtr<T>();
    *newPtr = std::make_shared<T>(device);
    return newPtr;
}

template <typename T, typename ...Args>
NIPtr<T>* CreateNIObject(Args&&... args)
{
    NIPtr<T>* niPtr = AllocateNIObject<T>();
    NIPtr<T>& obj = *niPtr;
    if (!obj->Init(std::forward<Args>(args)...))
    {
        delete niPtr;
        return nullptr;
    }

    return niPtr;
}

template <typename T, typename ...Args>
NIPtr<T>* CreateNIInstanceObject(const NIPtr<NativeInstance>& instance, Args&&... args)
{
    NIPtr<T>* niPtr = AllocateNIInstanceObject<T>(instance);
    NIPtr<T>& obj = *niPtr;
    if (!obj->Init(std::forward<Args>(args)...))
    {
        delete niPtr;
        return nullptr;
    }

    return niPtr;
}

template <typename T, typename ...Args>
NIPtr<T>* CreateNIDeviceObject(const NIPtr<NativeDevice>& device, Args&&... args)
{
    NIPtr<T>* niPtr = AllocateNIDeviceObject<T>(device);
    NIPtr<T>& obj = *niPtr;
    if (!obj->Init(std::forward<Args>(args)...))
    {
        delete niPtr;
        return nullptr;
    }

    return niPtr;
}

template <typename T>
const NIPtr<T>& GetNIObject(jlong ptr)
{
    return *reinterpret_cast<NIPtr<T>*>(ptr);
}

template <typename T>
void FreeNIObject(jlong ptr)
{
    NIPtr<T>* niPtr = reinterpret_cast<NIPtr<T>*>(ptr);
    delete niPtr;
}


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



// bytes per pixel calculator
// only covers pixel formats supported by JFX
// TODO: D3D12: Many of these utility functions should probably be moved to D3D12Utils.hpp
constexpr uint32_t GetDXGIFormatBPP(DXGI_FORMAT format)
{
    switch (format)
    {
    case DXGI_FORMAT_R32G32B32A32_FLOAT:
        return sizeof(float) * 4;
    case DXGI_FORMAT_B8G8R8A8_UNORM:
    case DXGI_FORMAT_B8G8R8X8_UNORM:
        return sizeof(uint8_t) * 4;
    case DXGI_FORMAT_R8_UNORM:
    case DXGI_FORMAT_A8_UNORM:
    case DXGI_FORMAT_NV12:
        return sizeof(uint8_t) * 1;
    case DXGI_FORMAT_R16_UINT:
        return sizeof(uint16_t) * 1;
    case DXGI_FORMAT_R32_UINT:
        return sizeof(uint32_t) * 1;
    // TODO: D3D12: DXGI_FORMAT_NV12
    default:
        return 0;
    }
}

constexpr bool IsDepthFormat(DXGI_FORMAT format)
{
    switch (format)
    {
    case DXGI_FORMAT_D16_UNORM:
    case DXGI_FORMAT_D24_UNORM_S8_UINT:
    case DXGI_FORMAT_D32_FLOAT:
    case DXGI_FORMAT_D32_FLOAT_S8X24_UINT:
        return true;
    default:
        return false;
    }
}

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

struct Pixel_RGB32_FLOAT
{
    float r;
    float g;
    float b;
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

// vertices array incoming to DrawQuads contains 7 floats - XYZ, UV1 and UV2
const uint32_t FLOATS_PER_2D_VERTEX = 7;
// colors array incoming to DrawQuads contains 4 unsigned chars - RGBA
const uint32_t COLORS_PER_2D_VERTEX = 4;

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

// to return errors from RenderThread::Signal()
#define D3D12NI_INVALID_FENCE_VALUE 0
// mimics D3D9's D3DADAPTER_DEFAULT macro to pick something if upper layers don't care
// or don't provide us with a native handle (see Web OpacityTest/ShadowTest tests)
#define D3D12NI_DEFAULT_ADAPTER 0
#define D3D12NI_FLOATS_PER_2D_VERTEX 7
#define D3D12NI_CHARS_PER_2D_VERTEX 4


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

inline const char* ResourceAssignmentTypeToString(ResourceAssignmentType type)
{
    switch (type)
    {
    case ResourceAssignmentType::DESCRIPTOR: return "DESCRIPTOR";
    case ResourceAssignmentType::DESCRIPTOR_TABLE_TEXTURES: return "DESCRIPTOR_TABLE_TEXTURES";
    case ResourceAssignmentType::DESCRIPTOR_TABLE_CBUFFERS: return "DESCRIPTOR_TABLE_CBUFFERS";
    case ResourceAssignmentType::DESCRIPTOR_TABLE_SAMPLERS: return "DESCRIPTOR_TABLE_SAMPLERS";
    default: return "UNKNOWN";
    }
}

inline const wchar_t* CompositeModeToWString(CompositeMode mode)
{
    switch (mode)
    {
    case CompositeMode::CLEAR: return L"CLEAR";
    case CompositeMode::SRC: return L"SRC";
    case CompositeMode::SRC_OVER: return L"SRC_OVER";
    case CompositeMode::DST_OUT: return L"DST_OUT";
    case CompositeMode::ADD: return L"ADD";
    default: return L"UNKNOWN";
    }
}

inline size_t GetPixelFormatBPP(PixelFormat f)
{
    switch (f) {
    case PixelFormat::BYTE_GRAY:
    case PixelFormat::BYTE_ALPHA:
        return 1;
    case PixelFormat::BYTE_RGB:
        return 3;
    case PixelFormat::INT_ARGB_PRE:
    case PixelFormat::BYTE_BGRA_PRE:
        return 4;
    case PixelFormat::FLOAT_XYZW:
        return 16;
    default:
        return 0;
    }
}

} // namespace D3D12


// suppresses compilation warnings when a variable is unused
// this is effectively a noop but it is often used in tandem with
// D3D12NI_ASSERT as asserts are compiled out on Release
#define D3D12NI_UNUSED(x) (void)(x)

// checks provided hr, if it fails prints errMsg with hr's value and returns ret
#define D3D12NI_RET_IF_FAILED(hr, ret, errMsg) do { \
    if (FAILED(hr)) { \
        _com_error __e(hr); \
        D3D12NI_LOG_ERROR("%s: %x (%ws)", errMsg, hr, __e.ErrorMessage()); \
        return (ret); \
    } \
} while (0)

// same as above but for void-returning calls
#define D3D12NI_VOID_RET_IF_FAILED(hr, errMsg) do { \
    if (FAILED(hr)) { \
        _com_error __e(hr); \
        D3D12NI_LOG_ERROR("%s: %x (%ws)", errMsg, hr, __e.ErrorMessage()); \
        return; \
    } \
} while (0)

// same as above but also runs device-specific error message reporting and checks if device was removed
#define D3D12NI_DEV_RET_IF_FAILED(device, hr, ret, errMsg) do { \
    if (FAILED(hr)) { \
        _com_error __e(hr); \
        D3D12NI_LOG_ERROR("%s: %x (%ws)", errMsg, hr, __e.ErrorMessage()); \
        device->ReportErrorMessages(); \
        if (hr == DXGI_ERROR_DEVICE_REMOVED) { \
            device->ExamineDeviceRemoved(); \
        } \
        return (ret); \
    } \
} while (0)

// same as above but also runs device-specific error message reporting and checks if device was removed
#define D3D12NI_DEV_VOID_RET_IF_FAILED(device, hr, errMsg) do { \
    if (FAILED(hr)) { \
        _com_error __e(hr); \
        D3D12NI_LOG_ERROR("%s: %x (%ws)", errMsg, hr, __e.ErrorMessage()); \
        device->ReportErrorMessages(); \
        if (hr == DXGI_ERROR_DEVICE_REMOVED) { \
            device->ExamineDeviceRemoved(); \
        } \
        return; \
    } \
} while (0)

// zeros a structure, frequently used to initialize D3D12_*_DESC structs
#define D3D12NI_ZERO_STRUCT(x) do { \
    memset(&x, 0x0, sizeof(x)); \
} while (0)

// custom assert call that also logs an error message to the logger
#ifdef DEBUG

#define D3D12NI_ASSERT(x, msg, ...) do { \
    if (!(x)) \
    { \
        D3D12NI_LOG_ERROR(msg, __VA_ARGS__); \
        assert(0 == msg); \
    } \
} while (0)

#else // DEBUG

#define D3D12NI_ASSERT(x, msg, ...) do { D3D12NI_UNUSED(x); } while (0)

#endif // DEBUG


namespace D3D12 {

// Descriptor-related structs used to move to RenderPayload
template <typename DescriptorType>
struct DescriptorBinding
{
    uint32_t rootIndex;
    DescriptorType handle;

    DescriptorBinding()
        : rootIndex(UINT32_MAX)
        , handle()
    {}

    DescriptorBinding(uint32_t idx, DescriptorType h)
        : rootIndex(idx)
        , handle(h)
    {}
};

template <typename DescriptorType>
using DescriptorMappings = std::array<DescriptorBinding<DescriptorType>, 4>;

struct Descriptors
{
    DescriptorMappings<D3D12_GPU_VIRTUAL_ADDRESS> CBVs;
    uint32_t CBVCount;
    DescriptorMappings<D3D12_GPU_DESCRIPTOR_HANDLE> DTs;
    uint32_t DTCount;

    Descriptors()
        : CBVs()
        , CBVCount(0)
        , DTs()
        , DTCount(0)
    {}

    void AddConstantBufferView(uint32_t rootIndex, const D3D12_GPU_VIRTUAL_ADDRESS& address)
    {
        D3D12NI_ASSERT(CBVCount < 4, "Cannot add more CBVs");

        CBVs[CBVCount].rootIndex = rootIndex;
        CBVs[CBVCount].handle = address;
        ++CBVCount;
    }

    void AddDescriptorTable(uint32_t rootIndex, const D3D12_GPU_DESCRIPTOR_HANDLE& handle)
    {
        D3D12NI_ASSERT(DTCount < 4, "Cannot add more DTs");

        DTs[DTCount].rootIndex = rootIndex;
        DTs[DTCount].handle = handle;
        ++DTCount;
    }
};

} // namespace D3D12
