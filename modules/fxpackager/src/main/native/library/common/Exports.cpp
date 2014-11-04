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

    static jobjectArray MapKeysToJObjectArray(JNIEnv *env, std::map<TString, TValueIndex> map) {
        JavaStringArray result(env, map.size());
        unsigned int index = 0;

        for (std::map<TString, TValueIndex>::iterator iterator = map.begin();
            iterator != map.end();
            iterator++) {

            jstring item = PlatformString(iterator->first).toJString(env);
            result.SetValue(index, item);

            index++;
        }

        return result.GetData();
    }

public:
    static jstring _getUserJvmOptionDefaultValue(JNIEnv *env, jstring option) {
        jstring result;

        Package& package = Package::GetInstance();
        std::map<TString, TValueIndex> defaultuserargs = package.GetDefaultJVMUserArgs();
        TString loption = PlatformString(env, option).toString();
        PlatformString value = defaultuserargs[loption].value;

        try {
            result = value.toJString(env);
        }
        catch (const JavaException&) {
            return NULL;
        }

        return result;
    }

    static jobjectArray _getUserJvmOptionDefaultKeys(JNIEnv *env) {
        jobjectArray result;

        Package& package = Package::GetInstance();

        try {
            result = MapKeysToJObjectArray(env, package.GetDefaultJVMUserArgs());
        }
        catch (const JavaException&) {
            return NULL;
        }

        return result;
    }

    static jstring _getUserJvmOptionValue(JNIEnv *env, jstring option) {
        jstring result;

        Package& package = Package::GetInstance();
        std::map<TString, TValueIndex> userargs = package.GetJVMUserArgs();

        try {
            TString loption = PlatformString(env, option).toString();
            PlatformString value = userargs[loption].value;
            result = value.toJString(env);
        }
        catch (const JavaException&) {
            return NULL;
        }

        return result;
    }

    static void _setUserJvmKeysAndValues(JNIEnv *env, jobjectArray options, jobjectArray values) {
        Package& package = Package::GetInstance();
        std::map<TString, TValueIndex> newMap;

        try {
            JavaStringArray loptions(env, options);
            JavaStringArray lvalues(env, values);

            for (unsigned int index = 0; index < loptions.Count(); index++) {
                TString name = PlatformString(env, loptions.GetValue(index)).toString();
                TValueIndex value;
                value.value = PlatformString(env, lvalues.GetValue(index)).toString();
                value.index = index;
                newMap.insert(std::map<TString, TValueIndex>::value_type(name, value));
            }
        }
        catch (const JavaException&) {
            return;
        }

        package.SetJVMUserArgOverrides(newMap);
    }

    static jobjectArray _getUserJvmOptionKeys(JNIEnv *env) {
        jobjectArray result;

        Package& package = Package::GetInstance();

        try {
            result = MapKeysToJObjectArray(env, package.GetJVMUserArgs());
        }
        catch (const JavaException&) {
            return NULL;
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
class DebugExports {
private:
    // This is not a class to create an instance of.
    DebugExports();

public:
    static jint _getpid(JNIEnv *env) {
        Platform& platform = Platform::GetInstance();
        return platform.GetProcessID();
    }

    static jboolean _isdebugged(JNIEnv *env) {
        Platform& platform = Platform::GetInstance();
        return platform.GetDebugState() != Platform::dsNone;
    }
};

JNIEXPORT jint JNICALL Java_someclass__getpid(JNIEnv *env) {
    return DebugExports::_getpid(env);
}


JNIEXPORT jboolean JNICALL Java_someclass__isdebugged(JNIEnv *env) {
    return DebugExports::_isdebugged(env);
}

/* Use the following in Java in the main or somewhere else:
    static native boolean isdebugged();

    if (Arrays.asList(args).contains("-debug")) {
        System.out.println("pid=" + getpid());


        while (true) {
            if (isdebugged() == true) {
                break;
            }
        }
    }
*/

#endif //DEBUG
