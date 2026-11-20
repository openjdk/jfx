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

#include <d3d12.h>
#include <dxgiformat.h>

namespace D3D12 {
namespace Internal {

enum class LogLevel: unsigned char
{
    Error = 0,
    Warning,
    Info,
    Debug,
    Trace
};

void Log(LogLevel level, const char* file, int line, const char* fmt, ...);

// Translators for debugging
const char* D3DFeatureLevelToString(D3D_FEATURE_LEVEL level);
const char* D3DFeatureLevelToShortString(D3D_FEATURE_LEVEL level);
const char* D3DShaderModelToShortString(D3D_SHADER_MODEL model);
const char* DXGIFormatToString(DXGI_FORMAT format);

} // namespace Internal
} // namespace D3D12

// When not building in DebugNative debug/trace logs should be compiled out
#define D3D12NI_LOG(level, file, line, fmt, ...) do { \
    D3D12::Internal::Log(level, file, line, fmt, __VA_ARGS__); \
} while (0)

#define D3D12NI_LOG_ERROR(fmt, ...) D3D12NI_LOG(D3D12::Internal::LogLevel::Error, __FILE__, __LINE__, fmt, __VA_ARGS__)
#define D3D12NI_LOG_INFO(fmt, ...) D3D12NI_LOG(D3D12::Internal::LogLevel::Info, __FILE__, __LINE__, fmt, __VA_ARGS__)
#define D3D12NI_LOG_WARN(fmt, ...) D3D12NI_LOG(D3D12::Internal::LogLevel::Warning, __FILE__, __LINE__, fmt, __VA_ARGS__)

#ifdef DEBUG
#define D3D12NI_LOG_DEBUG(fmt, ...) D3D12NI_LOG(D3D12::Internal::LogLevel::Debug, __FILE__, __LINE__, fmt, __VA_ARGS__)
#define D3D12NI_LOG_TRACE(fmt, ...) D3D12NI_LOG(D3D12::Internal::LogLevel::Trace, __FILE__, __LINE__, fmt, __VA_ARGS__)
#else // DEBUG
#define D3D12NI_LOG_DEBUG(fmt, ...) do { } while (0)
#define D3D12NI_LOG_TRACE(fmt, ...) do { } while (0)
#endif // DEBUG
