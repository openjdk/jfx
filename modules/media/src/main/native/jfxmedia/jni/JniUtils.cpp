/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

#include "JniUtils.h"
#include "Logger.h"
#include <Common/ProductFlags.h>

#if ENABLE_PLATFORM_GSTREAMER
#include <platform/gstreamer/GstJniUtils.h>
#endif // ENABLE_PLATFORM_GSTREAMER

void ThrowJavaException(JNIEnv *env, const char* type, const char* message)
{
    // First check if there's already a pending exception, if there is then do nothing
    // also abort if we're passed a NULL env
    if (env ? env->ExceptionCheck() : true) {
        return;
    }

    jclass klass = NULL;
    if (type) {
        klass = env->FindClass(type);
        if (!klass) {
            // might have caused an exception
            if (env->ExceptionOccurred()) {
                env->ExceptionClear();
            }
        }
    }
    if (!klass) {
        klass = env->FindClass("java/lang/Exception");
        if (!klass) {
            if (env->ExceptionOccurred()) {
                env->ExceptionClear();
            }
            // This shouldn't happen...
            return;
        }
    }
    env->ThrowNew(klass, message);
}

JNIEnv *GetJavaEnvironment(JavaVM *jvm, jboolean &didAttach)
{
    JNIEnv *env = NULL;
    didAttach = false;
    if (jvm) {
        if (jvm->GetEnv((void**)&env, JNI_VERSION_1_4) != JNI_OK) {
            didAttach = true;
            jvm->AttachCurrentThreadAsDaemon((void**)&env, NULL);
        }
    }
    return env;
}

bool CJavaEnvironment::hasException()
{
    return (environment ? (bool)environment->ExceptionCheck() : false);
}

bool CJavaEnvironment::clearException()
{
    if (environment ? environment->ExceptionCheck() : false) {
        environment->ExceptionClear();
        return true;
    }
    return false;
}

/**
 * Check whether there is a pending exception and if so, log its string version
 * and return true, otherwise, i.e., if there is no exception, return false.
 */
bool CJavaEnvironment::reportException()
{
    if (environment) {
        jthrowable exc = environment->ExceptionOccurred();
        if (exc) {
            jclass cid = environment->FindClass("java/lang/Throwable");
            jmethodID mid = environment->GetMethodID(cid, "toString", "()Ljava/lang/String;");
            jstring jmsg = (jstring)environment->CallObjectMethod(exc, mid);
            char* pmsg = (char*)environment->GetStringUTFChars(jmsg, NULL);
            LOGGER_ERRORMSG(pmsg);
            environment->ReleaseStringUTFChars(jmsg, pmsg);
            environment->ExceptionClear();
            environment->DeleteLocalRef(exc);
            environment->DeleteLocalRef(cid);
            return true;
        }
    }
    return false;
}

CJavaEnvironment::CJavaEnvironment(JavaVM *jvm) :
    attached(false),
    environment(NULL)
{
    if (jvm) {
        environment = GetJavaEnvironment(jvm, attached);
    }
}

CJavaEnvironment::~CJavaEnvironment()
{
    if (attached && environment) {
        JavaVM *jvm;
        if (environment->GetJavaVM(&jvm) == JNI_OK) {
            jvm->DetachCurrentThread();
        }
    }
}

JNIEnv *CJavaEnvironment::getEnvironment()
{
    return environment;
}
