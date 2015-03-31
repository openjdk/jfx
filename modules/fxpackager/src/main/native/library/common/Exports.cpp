/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


#include "Platform.h"
#include "Package.h"
#include "PlatformString.h"
#include "PropertyFile.h"
#include "Lock.h"
#include "Java.h"

#include "jni.h"


class UserJVMArgsExports {
private:
    // This is not a class to create an instance of.
    UserJVMArgsExports();

    static jobjectArray MapKeysToJObjectArray(JNIEnv *env, OrderedMap<TString, TString> map) {
        std::vector<TString> keys = map.GetKeys();
        JavaStringArray result(env, keys.size());

        for (unsigned int index = 0; index < keys.size(); index++) {
            jstring item = PlatformString(keys[index]).toJString(env);
            result.SetValue(index, item);
        }

//        JavaStringArray result(env, map.Count());
//
//        for (map::  iterator = map.begin(); iterator != map.end(); iterator++) {
//            jstring item = PlatformString(keys[index]).toJString(env);
//            result.SetValue(index, item);
//        }

        return result.GetData();
    }

public:
    static jstring _getUserJvmOptionDefaultValue(JNIEnv *env, jstring option) {
        if (env == NULL || option == NULL)
            return NULL;

        jstring result = NULL;

        Package& package = Package::GetInstance();
        OrderedMap<TString, TString> defaultuserargs = package.GetDefaultJVMUserArgs();
        TString loption = PlatformString(env, option).toString();

        try {
            TString temp;
            defaultuserargs.GetValue(loption, temp);
            PlatformString value = temp;
            result = value.toJString(env);
        }
        catch (const JavaException&) {
        }

        return result;
    }

    static jobjectArray _getUserJvmOptionDefaultKeys(JNIEnv *env) {
        if (env == NULL)
            return NULL;

        jobjectArray result = NULL;

        Package& package = Package::GetInstance();

        try {
            result = MapKeysToJObjectArray(env, package.GetDefaultJVMUserArgs());
        }
        catch (const JavaException&) {
        }

        return result;
    }

    static jstring _getUserJvmOptionValue(JNIEnv *env, jstring option) {
        if (env == NULL || option == NULL)
            return NULL;

        jstring result = NULL;

        Package& package = Package::GetInstance();
        OrderedMap<TString, TString> userargs = package.GetJVMUserArgs();

        try {
            TString loption = PlatformString(env, option).toString();
            TString temp;
            userargs.GetValue(loption, temp);
            PlatformString value = temp;
            result = value.toJString(env);
        }
        catch (const JavaException&) {
        }

        return result;
    }

    static void _setUserJvmKeysAndValues(JNIEnv *env, jobjectArray options, jobjectArray values) {
        if (env == NULL || options == NULL || values == NULL)
            return;

        Package& package = Package::GetInstance();
        OrderedMap<TString, TString> newMap;

        try {
            JavaStringArray loptions(env, options);
            JavaStringArray lvalues(env, values);

            for (unsigned int index = 0; index < loptions.Count(); index++) {
                TString name = PlatformString(env, loptions.GetValue(index)).toString();
                TString value = PlatformString(env, lvalues.GetValue(index)).toString();
                newMap.Append(name, value);
            }
        }
        catch (const JavaException&) {
            return;
        }

        package.SetJVMUserArgOverrides(newMap);
    }

    static jobjectArray _getUserJvmOptionKeys(JNIEnv *env) {
        if (env == NULL)
            return NULL;

        jobjectArray result = NULL;

        Package& package = Package::GetInstance();

        try {
            result = MapKeysToJObjectArray(env, package.GetJVMUserArgs());
        }
        catch (const JavaException&) {
        }

        return result;
    }
};


extern "C" {
    JNIEXPORT jstring JNICALL Java_jdk_packager_services_userjvmoptions_LauncherUserJvmOptions__1getUserJvmOptionDefaultValue(JNIEnv *env, jclass klass, jstring option) {
        return UserJVMArgsExports::_getUserJvmOptionDefaultValue(env, option);
    }

    JNIEXPORT jobjectArray JNICALL Java_jdk_packager_services_userjvmoptions_LauncherUserJvmOptions__1getUserJvmOptionDefaultKeys(JNIEnv *env, jclass klass) {
        return UserJVMArgsExports::_getUserJvmOptionDefaultKeys(env);
    }

    JNIEXPORT jstring JNICALL Java_jdk_packager_services_userjvmoptions_LauncherUserJvmOptions__1getUserJvmOptionValue(JNIEnv *env, jclass klass, jstring option) {
        return UserJVMArgsExports::_getUserJvmOptionValue(env, option);
    }

    JNIEXPORT void JNICALL Java_jdk_packager_services_userjvmoptions_LauncherUserJvmOptions__1setUserJvmKeysAndValues(JNIEnv *env, jclass klass, jobjectArray options, jobjectArray values) {
        UserJVMArgsExports::_setUserJvmKeysAndValues(env, options, values);
    }

    JNIEXPORT jobjectArray JNICALL Java_jdk_packager_services_userjvmoptions_LauncherUserJvmOptions__1getUserJvmOptionKeys(JNIEnv *env, jclass klass) {
        return UserJVMArgsExports::_getUserJvmOptionKeys(env);
    }
}

#ifdef DEBUG
// Build with debug info. Create a class:
//
// package com;
//
// class DebugExports {
//   static {
//      System.loadLibrary("packager");
//   }
//
//   public static native boolean isdebugged();
//
//   public static native int getpid();
// }
//
// Use the following in Java in the main or somewhere else:
//
// import com.DebugExports;
// import java.util.Arrays;
//
// if (Arrays.asList(args).contains("-debug")) {
//   System.out.println("pid=" + getpid());
//
//   while (true) {
//     if (isdebugged() == true) {
//       break;
//     }
//   }
// }
//
// The call to isdebugger() will wait until a native debugger is attached. The process
// identifier (pid) will be printed to the console for you to attach your debugger to.
extern "C" {
    JNIEXPORT jboolean JNICALL Java_com_DebugExports_isdebugged(JNIEnv *env, jclass klass) {
        jboolean result = false;
        Package& package = Package::GetInstance();

        if (package.Debugging() == dsNative) {
            Platform& platform = Platform::GetInstance();
            result = platform.GetDebugState() != dsNone;
        }

        return result;
    }

    JNIEXPORT jint JNICALL Java_com_DebugExports_getpid(JNIEnv *env, jclass klass) {
        Platform& platform = Platform::GetInstance();
        return platform.GetProcessID();
    }
}
#endif //DEBUG
