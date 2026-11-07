/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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

#define D3D12NI_DINFO_SET_ARGS(x) #x, x


namespace D3D12 {
namespace Internal {

class Information
{
protected:
    bool checkAndClearException(JNIEnv* env) const
    {
        if (!env->ExceptionCheck()) return false;
        env->ExceptionClear();
        return true;
    }

    bool setJavaString(JNIEnv* env, jobject dinfoObject, jclass dinfoClass, const char* name, const std::string& string) const
    {
        if (string.size() == 0) return true; // skip if string is empty

        jfieldID id = env->GetFieldID(dinfoClass, name, "Ljava/lang/String;");
        if (checkAndClearException(env)) return false;

        jobject jString = env->NewStringUTF(string.c_str());
        if (jString == nullptr) return false;

        env->SetObjectField(dinfoObject, id, jString);
        env->DeleteLocalRef(jString);
        return true;
    }

    bool setJavaInt(JNIEnv* env, jobject dinfoObject, jclass dinfoClass, const char* name, int value) const
    {
        jfieldID id = env->GetFieldID(dinfoClass, name, "I");
        if (checkAndClearException(env)) return false;
        env->SetIntField(dinfoObject, id, value);
        return true;
    }

    bool setJavaLong(JNIEnv* env, jobject dinfoObject, jclass dinfoClass, const char* name, uint64_t value) const
    {
        jfieldID id = env->GetFieldID(dinfoClass, name, "J");
        if (checkAndClearException(env)) return false;
        env->SetLongField(dinfoObject, id, value);
        return true;
    }

public:
    virtual bool ToJObject(JNIEnv* env, jobject dinfoObject) const = 0;
};

class AdapterInformation: public Information
{
public:
    std::string description;

    uint32_t vendorID;
    uint32_t deviceID;
    uint32_t subSysID;
    uint32_t revision;

    uint64_t videoMemory;
    uint64_t systemMemory;
    uint64_t sharedMemory;

    virtual bool ToJObject(JNIEnv* env, jobject dinfoObject) const override
    {
        jclass dinfoClass = env->GetObjectClass(dinfoObject);
        if (dinfoClass == nullptr) return false;

        if (!setJavaString(env, dinfoObject, dinfoClass, D3D12NI_DINFO_SET_ARGS(description))) return false;

        if (!setJavaInt(env, dinfoObject, dinfoClass, D3D12NI_DINFO_SET_ARGS(vendorID))) return false;
        if (!setJavaInt(env, dinfoObject, dinfoClass, D3D12NI_DINFO_SET_ARGS(deviceID))) return false;
        if (!setJavaInt(env, dinfoObject, dinfoClass, D3D12NI_DINFO_SET_ARGS(subSysID))) return false;
        if (!setJavaInt(env, dinfoObject, dinfoClass, D3D12NI_DINFO_SET_ARGS(revision))) return false;

        if (!setJavaLong(env, dinfoObject, dinfoClass, D3D12NI_DINFO_SET_ARGS(videoMemory))) return false;
        if (!setJavaLong(env, dinfoObject, dinfoClass, D3D12NI_DINFO_SET_ARGS(systemMemory))) return false;
        if (!setJavaLong(env, dinfoObject, dinfoClass, D3D12NI_DINFO_SET_ARGS(sharedMemory))) return false;

        OSVERSIONINFO osInfo; osInfo.dwOSVersionInfoSize = sizeof(osInfo);
        if (GetVersionEx( &osInfo )) {
            if (!setJavaInt(env, dinfoObject, dinfoClass, "osMajorVersion", osInfo.dwMajorVersion)) return false;
            if (!setJavaInt(env, dinfoObject, dinfoClass, "osMinorVersion", osInfo.dwMinorVersion)) return false;
            if (!setJavaInt(env, dinfoObject, dinfoClass, "osBuildNumber", osInfo.dwBuildNumber)) return false;
        }

        return true;
    }
};

class DeviceInformation: public Information
{
public:
    std::string description;
    std::string featureLevel;
    std::string shaderModel;

    uint64_t deviceError;
    std::string deviceErrorReason;

    virtual bool ToJObject(JNIEnv* env, jobject dinfoObject) const override
    {
        jclass dinfoClass = env->GetObjectClass(dinfoObject);
        if (dinfoClass == nullptr) return false;

        if (!setJavaString(env, dinfoObject, dinfoClass, D3D12NI_DINFO_SET_ARGS(description))) return false;
        if (!setJavaString(env, dinfoObject, dinfoClass, D3D12NI_DINFO_SET_ARGS(featureLevel))) return false;
        if (!setJavaString(env, dinfoObject, dinfoClass, D3D12NI_DINFO_SET_ARGS(shaderModel))) return false;

        if (!setJavaLong(env, dinfoObject, dinfoClass, D3D12NI_DINFO_SET_ARGS(deviceError))) return false;
        if (!setJavaString(env, dinfoObject, dinfoClass, D3D12NI_DINFO_SET_ARGS(deviceErrorReason))) return false;

        return true;
    }
};

} // namespace Internal
} // namespace D3D12
