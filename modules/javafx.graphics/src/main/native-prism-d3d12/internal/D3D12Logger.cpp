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

#include "D3D12Logger.hpp"

#include "D3D12Config.hpp"

#include <fstream>
#include <ctime>
#include <mutex>
#include <stdio.h>
#include <stdarg.h>
#include <Windows.h>


namespace {

HANDLE stdOutHandle = GetStdHandle(STD_OUTPUT_HANDLE);
std::ofstream logFile;
std::mutex writeMutex;

} // namespace

namespace D3D12 {
namespace Internal {

void Log(LogLevel level, const char* file, int line, const char* fmt, ...)
{
    // Skip logs when prism.verbose is false.
    if (!Config::IsVerbose()) return;
    // Also, debug/trace logs should be skipped if they are not explicitly enabled
    if (level == LogLevel::Debug && !Config::IsDebug()) return;
    if (level == LogLevel::Trace && !Config::IsTrace()) return;

    va_list varargs;
    char logMsg[1024];
    CONSOLE_SCREEN_BUFFER_INFO consoleInfo;

    const char* levelStr;
    WORD consoleColor = 0;

    switch (level)
    {
    case LogLevel::Error:
        levelStr = "ERROR";
        consoleColor = FOREGROUND_RED | FOREGROUND_INTENSITY;
        break;
    case LogLevel::Warning:
        levelStr = "WARN ";
        consoleColor = FOREGROUND_RED | FOREGROUND_GREEN;
        break;
    case LogLevel::Info:
        levelStr = "INFO ";
        consoleColor = FOREGROUND_RED | FOREGROUND_GREEN | FOREGROUND_BLUE;
        break;
    case LogLevel::Debug:
        levelStr = "DEBUG";
        consoleColor = FOREGROUND_BLUE | FOREGROUND_INTENSITY;
        break;
    case LogLevel::Trace:
        levelStr = "TRACE";
        consoleColor = FOREGROUND_INTENSITY;
        break;
    }

    va_start(varargs, fmt);
    vsnprintf(logMsg, 1024, fmt, varargs);
    va_end(varargs);

    std::string fileStr(file);
    static size_t fileTrimIdx = 0;
    if (fileTrimIdx == 0)
    {
        const char* rootDirName = "native-prism-d3d12";
        size_t rootDirIdx = fileStr.find(rootDirName);
        if (rootDirIdx != std::string::npos)
            fileTrimIdx = rootDirIdx + strlen(rootDirName) + 1;
        else
            fileTrimIdx = std::string::npos;
    }

    std::string actualFileStr = (fileTrimIdx != std::string::npos) ? fileStr.substr(fileTrimIdx) : fileStr;

    char logLine[2048];

    {
        std::lock_guard<std::mutex> printLock(writeMutex);

        if (Config::IsFileLogEnabled() && !logFile.is_open())
        {
            std::time_t now = std::time(0);
            std::tm timeStruct;
            localtime_s(&timeStruct, &now);

            char filename[80];
            strftime(filename, sizeof(filename), "d3d12_log-%y%m%d-%H%M%S.log", &timeStruct);
            logFile.open(filename);
        }

        if (Config::IsColorLogsEnabled())
        {
            GetConsoleScreenBufferInfo(stdOutHandle, &consoleInfo);
            SetConsoleTextAttribute(stdOutHandle, consoleColor);
        }

        snprintf(logLine, sizeof(logLine), "[D3D12-%s] <%s:%d> %s\n", levelStr, actualFileStr.c_str(), line, logMsg);
        fprintf(stderr, "%s", logLine);
        if (logFile.is_open()) logFile << logLine;

        if (Config::IsColorLogsEnabled())
        {
            SetConsoleTextAttribute(stdOutHandle, consoleInfo.wAttributes);
        }
    }
}

const char* D3DFeatureLevelToString(D3D_FEATURE_LEVEL level)
{
    switch (level)
    {
    case D3D_FEATURE_LEVEL_1_0_CORE: return "D3D_FEATURE_LEVEL_1_0_CORE";
    case D3D_FEATURE_LEVEL_9_1: return "D3D_FEATURE_LEVEL_9_1";
    case D3D_FEATURE_LEVEL_9_2: return "D3D_FEATURE_LEVEL_9_2";
    case D3D_FEATURE_LEVEL_9_3: return "D3D_FEATURE_LEVEL_9_3";
    case D3D_FEATURE_LEVEL_10_0: return "D3D_FEATURE_LEVEL_10_0";
    case D3D_FEATURE_LEVEL_10_1: return "D3D_FEATURE_LEVEL_10_1";
    case D3D_FEATURE_LEVEL_11_0: return "D3D_FEATURE_LEVEL_11_0";
    case D3D_FEATURE_LEVEL_11_1: return "D3D_FEATURE_LEVEL_11_1";
    case D3D_FEATURE_LEVEL_12_0: return "D3D_FEATURE_LEVEL_12_0";
    case D3D_FEATURE_LEVEL_12_1: return "D3D_FEATURE_LEVEL_12_1";
    case D3D_FEATURE_LEVEL_12_2: return "D3D_FEATURE_LEVEL_12_2";
    default: return "UNKNOWN";
    }
}

const char* D3DFeatureLevelToShortString(D3D_FEATURE_LEVEL level)
{
    switch (level)
    {
    case D3D_FEATURE_LEVEL_1_0_CORE: return "1_0_CORE";
    case D3D_FEATURE_LEVEL_9_1: return "9_1";
    case D3D_FEATURE_LEVEL_9_2: return "9_2";
    case D3D_FEATURE_LEVEL_9_3: return "9_3";
    case D3D_FEATURE_LEVEL_10_0: return "10_0";
    case D3D_FEATURE_LEVEL_10_1: return "10_1";
    case D3D_FEATURE_LEVEL_11_0: return "11_0";
    case D3D_FEATURE_LEVEL_11_1: return "11_1";
    case D3D_FEATURE_LEVEL_12_0: return "12_0";
    case D3D_FEATURE_LEVEL_12_1: return "12_1";
    case D3D_FEATURE_LEVEL_12_2: return "12_2";
    default: return "UNKNOWN";
    }
}

const char* D3DShaderModelToShortString(D3D_SHADER_MODEL model)
{
    switch (model)
    {
    case D3D_SHADER_MODEL_5_1: return "5.1";
    case D3D_SHADER_MODEL_6_0: return "6.0";
    case D3D_SHADER_MODEL_6_1: return "6.1";
    case D3D_SHADER_MODEL_6_2: return "6.2";
    case D3D_SHADER_MODEL_6_3: return "6.3";
    case D3D_SHADER_MODEL_6_4: return "6.4";
    case D3D_SHADER_MODEL_6_5: return "6.5";
    case D3D_SHADER_MODEL_6_6: return "6.6";
    case D3D_SHADER_MODEL_6_7: return "6.7";
    default: return "UNKNOWN";
    }
}

const char* DXGIFormatToString(DXGI_FORMAT format)
{
    switch (format)
    {
    case DXGI_FORMAT_R32G32B32A32_TYPELESS: return "R32G32B32A32_TYPELESS";
    case DXGI_FORMAT_R32G32B32A32_FLOAT: return "R32G32B32A32_FLOAT";
    case DXGI_FORMAT_R32G32B32A32_UINT: return "R32G32B32A32_UINT";
    case DXGI_FORMAT_R32G32B32A32_SINT: return "R32G32B32A32_SINT";
    case DXGI_FORMAT_R32G32B32_TYPELESS: return "R32G32B32_TYPELESS";
    case DXGI_FORMAT_R32G32B32_FLOAT: return "R32G32B32_FLOAT";
    case DXGI_FORMAT_R32G32B32_UINT: return "R32G32B32_UINT";
    case DXGI_FORMAT_R32G32B32_SINT: return "R32G32B32_SINT";
    case DXGI_FORMAT_R16G16B16A16_TYPELESS: return "R16G16B16A16_TYPELESS";
    case DXGI_FORMAT_R16G16B16A16_FLOAT: return "R16G16B16A16_FLOAT";
    case DXGI_FORMAT_R16G16B16A16_UNORM: return "R16G16B16A16_UNORM";
    case DXGI_FORMAT_R16G16B16A16_UINT: return "R16G16B16A16_UINT";
    case DXGI_FORMAT_R16G16B16A16_SNORM: return "R16G16B16A16_SNORM";
    case DXGI_FORMAT_R16G16B16A16_SINT: return "R16G16B16A16_SINT";
    case DXGI_FORMAT_R32G32_TYPELESS: return "R32G32_TYPELESS";
    case DXGI_FORMAT_R32G32_FLOAT: return "R32G32_FLOAT";
    case DXGI_FORMAT_R32G32_UINT: return "R32G32_UINT";
    case DXGI_FORMAT_R32G32_SINT: return "R32G32_SINT";
    case DXGI_FORMAT_R32G8X24_TYPELESS: return "R32G8X24_TYPELESS";
    case DXGI_FORMAT_D32_FLOAT_S8X24_UINT: return "D32_FLOAT_S8X24_UINT";
    case DXGI_FORMAT_R32_FLOAT_X8X24_TYPELESS: return "R32_FLOAT_X8X24_TYPELESS";
    case DXGI_FORMAT_X32_TYPELESS_G8X24_UINT: return "X32_TYPELESS_G8X24_UINT";
    case DXGI_FORMAT_R10G10B10A2_TYPELESS: return "R10G10B10A2_TYPELESS";
    case DXGI_FORMAT_R10G10B10A2_UNORM: return "R10G10B10A2_UNORM";
    case DXGI_FORMAT_R10G10B10A2_UINT: return "R10G10B10A2_UINT";
    case DXGI_FORMAT_R11G11B10_FLOAT: return "R11G11B10_FLOAT";
    case DXGI_FORMAT_R8G8B8A8_TYPELESS: return "R8G8B8A8_TYPELESS";
    case DXGI_FORMAT_R8G8B8A8_UNORM: return "R8G8B8A8_UNORM";
    case DXGI_FORMAT_R8G8B8A8_UNORM_SRGB: return "R8G8B8A8_UNORM_SRGB";
    case DXGI_FORMAT_R8G8B8A8_UINT: return "R8G8B8A8_UINT";
    case DXGI_FORMAT_R8G8B8A8_SNORM: return "R8G8B8A8_SNORM";
    case DXGI_FORMAT_R8G8B8A8_SINT: return "R8G8B8A8_SINT";
    case DXGI_FORMAT_R16G16_TYPELESS: return "R16G16_TYPELESS";
    case DXGI_FORMAT_R16G16_FLOAT: return "R16G16_FLOAT";
    case DXGI_FORMAT_R16G16_UNORM: return "R16G16_UNORM";
    case DXGI_FORMAT_R16G16_UINT: return "R16G16_UINT";
    case DXGI_FORMAT_R16G16_SNORM: return "R16G16_SNORM";
    case DXGI_FORMAT_R16G16_SINT: return "R16G16_SINT";
    case DXGI_FORMAT_R32_TYPELESS: return "R32_TYPELESS";
    case DXGI_FORMAT_D32_FLOAT: return "D32_FLOAT";
    case DXGI_FORMAT_R32_FLOAT: return "R32_FLOAT";
    case DXGI_FORMAT_R32_UINT: return "R32_UINT";
    case DXGI_FORMAT_R32_SINT: return "R32_SINT";
    case DXGI_FORMAT_R24G8_TYPELESS: return "R24G8_TYPELESS";
    case DXGI_FORMAT_D24_UNORM_S8_UINT: return "D24_UNORM_S8_UINT";
    case DXGI_FORMAT_R24_UNORM_X8_TYPELESS: return "R24_UNORM_X8_TYPELESS";
    case DXGI_FORMAT_X24_TYPELESS_G8_UINT: return "X24_TYPELESS_G8_UINT";
    case DXGI_FORMAT_R8G8_TYPELESS: return "R8G8_TYPELESS";
    case DXGI_FORMAT_R8G8_UNORM: return "R8G8_UNORM";
    case DXGI_FORMAT_R8G8_UINT: return "R8G8_UINT";
    case DXGI_FORMAT_R8G8_SNORM: return "R8G8_SNORM";
    case DXGI_FORMAT_R8G8_SINT: return "R8G8_SINT";
    case DXGI_FORMAT_R16_TYPELESS: return "R16_TYPELESS";
    case DXGI_FORMAT_R16_FLOAT: return "R16_FLOAT";
    case DXGI_FORMAT_D16_UNORM: return "D16_UNORM";
    case DXGI_FORMAT_R16_UNORM: return "R16_UNORM";
    case DXGI_FORMAT_R16_UINT: return "R16_UINT";
    case DXGI_FORMAT_R16_SNORM: return "R16_SNORM";
    case DXGI_FORMAT_R16_SINT: return "R16_SINT";
    case DXGI_FORMAT_R8_TYPELESS: return "R8_TYPELESS";
    case DXGI_FORMAT_R8_UNORM: return "R8_UNORM";
    case DXGI_FORMAT_R8_UINT: return "R8_UINT";
    case DXGI_FORMAT_R8_SNORM: return "R8_SNORM";
    case DXGI_FORMAT_R8_SINT: return "R8_SINT";
    case DXGI_FORMAT_A8_UNORM: return "A8_UNORM";
    case DXGI_FORMAT_R1_UNORM: return "R1_UNORM";
    case DXGI_FORMAT_R9G9B9E5_SHAREDEXP: return "R9G9B9E5_SHAREDEXP";
    case DXGI_FORMAT_R8G8_B8G8_UNORM: return "R8G8_B8G8_UNORM";
    case DXGI_FORMAT_G8R8_G8B8_UNORM: return "G8R8_G8B8_UNORM";
    case DXGI_FORMAT_BC1_TYPELESS: return "BC1_TYPELESS";
    case DXGI_FORMAT_BC1_UNORM: return "BC1_UNORM";
    case DXGI_FORMAT_BC1_UNORM_SRGB: return "BC1_UNORM_SRGB";
    case DXGI_FORMAT_BC2_TYPELESS: return "BC2_TYPELESS";
    case DXGI_FORMAT_BC2_UNORM: return "BC2_UNORM";
    case DXGI_FORMAT_BC2_UNORM_SRGB: return "BC2_UNORM_SRGB";
    case DXGI_FORMAT_BC3_TYPELESS: return "BC3_TYPELESS";
    case DXGI_FORMAT_BC3_UNORM: return "BC3_UNORM";
    case DXGI_FORMAT_BC3_UNORM_SRGB: return "BC3_UNORM_SRGB";
    case DXGI_FORMAT_BC4_TYPELESS: return "BC4_TYPELESS";
    case DXGI_FORMAT_BC4_UNORM: return "BC4_UNORM";
    case DXGI_FORMAT_BC4_SNORM: return "BC4_SNORM";
    case DXGI_FORMAT_BC5_TYPELESS: return "BC5_TYPELESS";
    case DXGI_FORMAT_BC5_UNORM: return "BC5_UNORM";
    case DXGI_FORMAT_BC5_SNORM: return "BC5_SNORM";
    case DXGI_FORMAT_B5G6R5_UNORM: return "B5G6R5_UNORM";
    case DXGI_FORMAT_B5G5R5A1_UNORM: return "B5G5R5A1_UNORM";
    case DXGI_FORMAT_B8G8R8A8_UNORM: return "B8G8R8A8_UNORM";
    case DXGI_FORMAT_B8G8R8X8_UNORM: return "B8G8R8X8_UNORM";
    case DXGI_FORMAT_R10G10B10_XR_BIAS_A2_UNORM: return "R10G10B10_XR_BIAS_A2_UNORM";
    case DXGI_FORMAT_B8G8R8A8_TYPELESS: return "B8G8R8A8_TYPELESS";
    case DXGI_FORMAT_B8G8R8A8_UNORM_SRGB: return "B8G8R8A8_UNORM_SRGB";
    case DXGI_FORMAT_B8G8R8X8_TYPELESS: return "B8G8R8X8_TYPELESS";
    case DXGI_FORMAT_B8G8R8X8_UNORM_SRGB: return "B8G8R8X8_UNORM_SRGB";
    case DXGI_FORMAT_BC6H_TYPELESS: return "BC6H_TYPELESS";
    case DXGI_FORMAT_BC6H_UF16: return "BC6H_UF16";
    case DXGI_FORMAT_BC6H_SF16: return "BC6H_SF16";
    case DXGI_FORMAT_BC7_TYPELESS: return "BC7_TYPELESS";
    case DXGI_FORMAT_BC7_UNORM: return "BC7_UNORM";
    case DXGI_FORMAT_BC7_UNORM_SRGB: return "BC7_UNORM_SRGB";
    case DXGI_FORMAT_AYUV: return "AYUV";
    case DXGI_FORMAT_Y410: return "Y410";
    case DXGI_FORMAT_Y416: return "Y416";
    case DXGI_FORMAT_NV12: return "NV12";
    case DXGI_FORMAT_P010: return "P010";
    case DXGI_FORMAT_P016: return "P016";
    case DXGI_FORMAT_420_OPAQUE: return "420_OPAQUE";
    case DXGI_FORMAT_YUY2: return "YUY2";
    case DXGI_FORMAT_Y210: return "Y210";
    case DXGI_FORMAT_Y216: return "Y216";
    case DXGI_FORMAT_NV11: return "NV11";
    case DXGI_FORMAT_AI44: return "AI44";
    case DXGI_FORMAT_IA44: return "IA44";
    case DXGI_FORMAT_P8: return "P8";
    case DXGI_FORMAT_A8P8: return "A8P8";
    case DXGI_FORMAT_B4G4R4A4_UNORM: return "B4G4R4A4_UNORM";
    case DXGI_FORMAT_P208: return "P208";
    case DXGI_FORMAT_V208: return "V208";
    case DXGI_FORMAT_V408: return "V408";
    case DXGI_FORMAT_SAMPLER_FEEDBACK_MIN_MIP_OPAQUE: return "SAMPLER_FEEDBACK_MIN_MIP_OPAQUE";
    case DXGI_FORMAT_SAMPLER_FEEDBACK_MIP_REGION_USED_OPAQUE: return "SAMPLER_FEEDBACK_MIP_REGION_USED_OPAQUE";
    case DXGI_FORMAT_FORCE_UINT: return "FORCE_UINT";
    default: return "UNKNOWN";
    }
}

} // namespace Internal
} // namespace D3D12
