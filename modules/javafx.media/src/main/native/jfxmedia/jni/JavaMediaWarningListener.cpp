/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include <jni.h>

#include "JniUtils.h"
#include "JavaMediaWarningListener.h"
#include <Common/VSMemory.h>

CJavaMediaWarningListener::CJavaMediaWarningListener(JNIEnv *env)
: CMediaWarningListener()
{
    env->GetJavaVM(&m_jvm);
}

CJavaMediaWarningListener::~CJavaMediaWarningListener()
{
}

void CJavaMediaWarningListener::Warning(int warningCode, const char* warningMessage)
{
    CJavaEnvironment javaEnv(m_jvm);
    JNIEnv *pEnv = javaEnv.getEnvironment();
    if (pEnv) {
        jclass mediaUtilsClass = pEnv->FindClass("com/sun/media/jfxmediaimpl/MediaUtils");
        jmethodID errorMethodID = pEnv->GetStaticMethodID(mediaUtilsClass,
                                                          "nativeWarning",
                                                          "(ILjava/lang/String;)V");
        char* message = NULL == warningMessage ? (char*)"" : (char*)warningMessage;
        jstring jmessage = pEnv->NewStringUTF(message);
        pEnv->CallStaticVoidMethod(mediaUtilsClass, errorMethodID,
                                   (jint)warningCode, jmessage);
        pEnv->DeleteLocalRef(jmessage);
        pEnv->DeleteLocalRef(mediaUtilsClass);
    }
}
