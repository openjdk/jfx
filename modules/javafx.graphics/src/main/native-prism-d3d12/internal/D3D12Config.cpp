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

#include "D3D12Config.hpp"

#include "D3D12Common.hpp"
#include "D3D12Constants.hpp"

#define TRY_GET_BOOL_PROP(x) do { \
    bool x; \
    if (TryGetBoolProperty("prism.d3d12." #x, x)) mSettings.x = x; \
} while(0)

#define TRY_GET_UINT_PROP(x) do { \
    int x; \
    if (TryGetIntProperty("prism.d3d12." #x, x)) mSettings.x = static_cast<uint32_t>(x); \
} while(0)


namespace D3D12 {
namespace Internal {

Config::Config()
    : mJNIEnv(nullptr)
    , mPrismSettingsClass(nullptr)
    , mSystemClass(nullptr)
    , mGetPropertyMethodID(nullptr)
{
    D3D12NI_ZERO_STRUCT(mSettings);
}

std::string Config::GetPropertyInternal(const char* propertyName)
{
    jstring propStr = mJNIEnv->NewStringUTF(propertyName);
    jstring propValueStr = reinterpret_cast<jstring>(mJNIEnv->CallStaticObjectMethod(mSystemClass, mGetPropertyMethodID, propStr));
    if (mJNIEnv->ExceptionCheck() == JNI_TRUE)
    {
        mJNIEnv->ExceptionClear();
        return std::string();
    }

    if (propValueStr == nullptr) return std::string();

    jboolean isCopy = false;
    const char* propValue = mJNIEnv->GetStringUTFChars(propValueStr, &isCopy);
    std::string ret(propValue);
    mJNIEnv->ReleaseStringUTFChars(propValueStr, propValue);
    return ret;
}

jfieldID Config::GetSettingsFieldID(const char* name, const char* signature)
{
    jfieldID id = mJNIEnv->GetStaticFieldID(mPrismSettingsClass, name, signature);
    if (mJNIEnv->ExceptionCheck() == JNI_TRUE)
    {
        mJNIEnv->ExceptionClear();
        return nullptr;
    }

    return id;
}

Config& Config::Instance()
{
    static Config instance;
    return instance;
}

bool Config::LoadConfiguration(JNIEnv* env, jclass psClass)
{
    // to fetch Prism settings
    mJNIEnv = env;
    mPrismSettingsClass = psClass;

    // to fetch System properties from JVM
    mSystemClass = mJNIEnv->FindClass("java/lang/System");
    if (mSystemClass == nullptr) return false;

    mGetPropertyMethodID = mJNIEnv->GetStaticMethodID(mSystemClass, "getProperty", "(Ljava/lang/String;)Ljava/lang/String;");
    if (mGetPropertyMethodID == nullptr) return false;

    // default settings
    mSettings.apiOpts = true;
    mSettings.clearOpts = true;
    mSettings.mainRingBufferThreshold = 24 * 1024 * 1024;
    mSettings.constantRingBufferThreshold = 8 * 1024 * 1024;
    mSettings.srvRingHeapThreshold = 32 * 1024;

    // fetch configuration
    mSettings.verbose = GetBool("verbose");
    mSettings.debug = GetBool("debug");
    mSettings.trace = GetBoolProperty("prism.d3d12.trace");
    mSettings.printShaderLoad = GetBoolProperty("prism.d3d12.printShaderLoad");
    mSettings.debugLayers = GetBoolProperty("prism.d3d12.debugLayers");
    mSettings.gpuDebug = GetBoolProperty("prism.d3d12.gpuDebug");
    mSettings.breakOnError = GetBoolProperty("prism.d3d12.breakOnError");
    mSettings.colorLogs = GetBoolProperty("prism.d3d12.colorLogs");
    mSettings.fileLog = GetBoolProperty("prism.d3d12.fileLog");
    mSettings.dred = GetBoolProperty("prism.d3d12.dred");
    mSettings.profilerSummary = GetBoolProperty("prism.d3d12.profilerSummary");

    TRY_GET_BOOL_PROP(apiOpts);
    TRY_GET_BOOL_PROP(clearOpts);
    TRY_GET_UINT_PROP(mainRingBufferThreshold);
    TRY_GET_UINT_PROP(constantRingBufferThreshold);
    TRY_GET_UINT_PROP(srvRingHeapThreshold);

    mSettings.vsync = GetBool("isVsyncEnabled");

    // cleanup leftovers
    mJNIEnv = nullptr;
    mPrismSettingsClass = nullptr;
    mSystemClass = nullptr;
    mGetPropertyMethodID = nullptr;

    return true;
}

int Config::GetInt(const char* name)
{
    jfieldID id = GetSettingsFieldID(name, "I");
    return id ? mJNIEnv->GetStaticIntField(mPrismSettingsClass, id) : 0;
}

bool Config::GetBool(const char* name)
{
    jfieldID id = GetSettingsFieldID(name, "Z");
    return id && mJNIEnv->GetStaticBooleanField(mPrismSettingsClass, id);
}

bool Config::GetBoolProperty(const char* name)
{
    bool result = false;
    if (!TryGetBoolProperty(name, result)) return false;
    return result;
}

int Config::GetIntProperty(const char* name)
{
    int result = false;
    if (!TryGetIntProperty(name, result)) return 0;
    return result;
}

bool Config::TryParseInt(const std::string& text, int& result)
{
    try {
        std::size_t strLen = text.length();
        std::size_t parseStop = 0;
        result = std::stoi(text.c_str(), &parseStop);
        if (parseStop < strLen)
        {
            result = 0;
            return false;
        }
        return true;
    } catch (std::exception) {
        result = 0;
        return false;
    }
}

bool Config::TryGetBoolProperty(const char* name, bool& result)
{
    std::string propValue = GetPropertyInternal(name);
    if (propValue.empty()) return false;

    if (propValue.compare("true") == 0) result = true;
    else if (propValue.compare("false") == 0) result = false;
    else
    {
        int parsed = 0;
        if (!TryParseInt(propValue, parsed)) return false;
        result = (parsed > 0);
    }

    return true;
}

bool Config::TryGetIntProperty(const char* name, int& result)
{
    std::string propValue = GetPropertyInternal(name);
    if (propValue.empty()) return false;

    if (!TryParseInt(propValue, result)) return false;
    return true;
}

} // namespace Internal
} // namespace D3D12
