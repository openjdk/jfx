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

#include <jni.h>
#include <string>


// uncomment to add Profiler to build
// NOTE: Profiler was designed to be fast and give general information on
// how the Pipeline behaves rather than to be precise. It was only tested
// on single-Stage applications and will probably break on multiple devices
// or Stages. Use with (relative) caution.
//#define D3D12NI_INCLUDE_PROFILER


namespace D3D12 {
namespace Internal {

class Config
{
    JNIEnv* mJNIEnv;
    jclass mPrismSettingsClass;

    jclass mSystemClass;
    jmethodID mGetPropertyMethodID;

    struct {
        bool verbose;
        bool debug;
        bool trace;
        bool printShaderLoad;
        bool debugLayers;
        bool gpuDebug;
        bool breakOnError;
        bool colorLogs;
        bool vsync;
        bool fileLog;
        bool apiOpts;
        bool clearOpts;
        bool dred;
        bool profilerSummary;
        uint32_t mainRingBufferThreshold;
        uint32_t constantRingBufferThreshold;
        uint32_t srvRingHeapThreshold;
    } mSettings;

    Config();
    ~Config() = default;

    Config(const Config&) = delete;
    Config(Config&&) = delete;
    Config& operator=(const Config&) = delete;
    Config& operator=(Config&&) = delete;

    std::string GetPropertyInternal(const char* propertyName);
    jfieldID GetSettingsFieldID(const char* name, const char* signature);
    bool GetBool(const char* name);
    int GetInt(const char* name);
    bool GetBoolProperty(const char* name); // defaults to false when TryGet fails
    int GetIntProperty(const char* name); // defaults to 0 when TryGet fails

    bool TryParseInt(const std::string& text, int& result);

    // below TryGet's return whether property exists or not
    // true when @p result is valid, false when getting property failed
    bool TryGetBoolProperty(const char* name, bool& result);
    bool TryGetIntProperty(const char* name, int& result);

public:
    static Config& Instance();

    bool LoadConfiguration(JNIEnv* env, jclass psClass);

    // pre-fetched properties and settings
    inline static bool IsVerbose()
    {
        return Instance().mSettings.verbose;
    }

    inline static bool IsDebug()
    {
        return Instance().mSettings.debug;
    }

    inline static bool IsTrace()
    {
        return Instance().mSettings.trace;
    }

    inline static bool IsPrintShaderLoadEnabled()
    {
        return Instance().mSettings.printShaderLoad;
    }

    inline static bool IsDebugLayerEnabled()
    {
        return Instance().mSettings.debugLayers;
    }

    inline static bool IsGpuDebugEnabled()
    {
        return Instance().mSettings.gpuDebug;
    }

    inline static bool IsBreakOnErrorEnabled()
    {
        return Instance().mSettings.breakOnError;
    }

    inline static bool IsColorLogsEnabled()
    {
        return Instance().mSettings.colorLogs;
    }

    inline static bool IsFileLogEnabled()
    {
        return Instance().mSettings.fileLog;
    }

    inline static bool IsVsyncEnabled()
    {
        return Instance().mSettings.vsync;
    }

    inline static bool IsApiOptsEnabled()
    {
        return Instance().mSettings.apiOpts;
    }

    inline static bool IsClearOptsEnabled()
    {
        return Instance().mSettings.clearOpts;
    }

    inline static bool IsDREDEnabled()
    {
        return Instance().mSettings.dred;
    }

    inline static bool IsProfilerSummaryEnabled()
    {
        #ifdef D3D12NI_INCLUDE_PROFILER
        return Instance().mSettings.profilerSummary;
        #else
        return false;
        #endif
    }

    inline static uint32_t MainRingBufferThreshold()
    {
        return Instance().mSettings.mainRingBufferThreshold;
    }

    inline static uint32_t ConstantRingBufferThreshold()
    {
        return Instance().mSettings.constantRingBufferThreshold;
    }

    inline static uint32_t SRVRingHeapThreshold()
    {
        return Instance().mSettings.srvRingHeapThreshold;
    }
};

} // namespace Internal
} // namespace D3D12
