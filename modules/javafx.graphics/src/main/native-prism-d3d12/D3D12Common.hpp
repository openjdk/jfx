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
#include <dxgi1_5.h>
#include <dxgidebug.h>
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
#include <algorithm>
#include <functional>

#include "D3D12Types.hpp"
#include "internal/D3D12Logger.hpp"

#include "Common_D3D12ShaderResourceDataHeader.hpp"

namespace D3D12 {

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

#define D3D12NI_JNI_ENV_CHECK(env, ret) do { \
    if (env->ExceptionCheck()) { \
        env->ExceptionClear(); \
        return ret; \
    } \
} while(0)

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
