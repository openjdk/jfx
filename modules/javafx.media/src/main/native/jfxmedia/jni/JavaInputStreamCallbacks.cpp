/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

#include "JavaInputStreamCallbacks.h"
#include "JniUtils.h"
#include <Common/VSMemory.h>
#if TARGET_OS_LINUX
#include <stdlib.h>
#include <string.h>
#endif // TARGET_OS_LINUX

jfieldID  CJavaInputStreamCallbacks::m_BufferFID = 0;
jmethodID CJavaInputStreamCallbacks::m_NeedBufferMID = 0;
jmethodID CJavaInputStreamCallbacks::m_ReadNextBlockMID = 0;
jmethodID CJavaInputStreamCallbacks::m_ReadBlockMID = 0;
jmethodID CJavaInputStreamCallbacks::m_IsSeekableMID = 0;
jmethodID CJavaInputStreamCallbacks::m_IsRandomAccessMID = 0;
jmethodID CJavaInputStreamCallbacks::m_SeekMID = 0;
jmethodID CJavaInputStreamCallbacks::m_CloseConnectionMID = 0;
jmethodID CJavaInputStreamCallbacks::m_PropertyMID = 0;
jmethodID CJavaInputStreamCallbacks::m_GetStreamSizeMID = 0;

CJavaInputStreamCallbacks::CJavaInputStreamCallbacks()
    : m_ConnectionHolder(0)
{}

CJavaInputStreamCallbacks::~CJavaInputStreamCallbacks()
{}

bool CJavaInputStreamCallbacks::Init(JNIEnv *env, jobject jLocator)
{
    env->GetJavaVM(&m_jvm);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        m_jvm = NULL;
        return false;
    }

    CJavaEnvironment javaEnv(m_jvm);

    static jmethodID createConnectionHolder = 0;
    if (0 == createConnectionHolder)
    {
        jclass klass = env->GetObjectClass(jLocator);
        createConnectionHolder = env->GetMethodID(klass, "createConnectionHolder", "()Lcom/sun/media/jfxmedia/locator/ConnectionHolder;");
        env->DeleteLocalRef(klass);
        if (javaEnv.reportException())
            return false;
    }

    m_ConnectionHolder = env->NewGlobalRef(env->CallObjectMethod(jLocator, createConnectionHolder));
    if (NULL == m_ConnectionHolder)
    {
        javaEnv.reportException();
        return false;
    }

    static bool methodIDsInitialized = false;
    bool hasException = false;
    if (!methodIDsInitialized)
    {
        // Get the parent abstract class. It's wrong to get method ids from the concrete implementation
        // because it crashes jvm when it tries to call virtual methods.
        // See https://javafx-jira.kenai.com/browse/RT-37115
        jclass klass = NULL;
        klass = env->FindClass("com/sun/media/jfxmedia/locator/ConnectionHolder");
        hasException = javaEnv.reportException();

        if (!hasException)
        {
            m_BufferFID = env->GetFieldID(klass, "buffer", "Ljava/nio/ByteBuffer;");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_NeedBufferMID = env->GetMethodID(klass, "needBuffer", "()Z");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_ReadNextBlockMID = env->GetMethodID(klass, "readNextBlock", "()I");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_ReadBlockMID = env->GetMethodID(klass, "readBlock", "(JI)I");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_IsSeekableMID = env->GetMethodID(klass, "isSeekable", "()Z");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_IsRandomAccessMID = env->GetMethodID(klass, "isRandomAccess", "()Z");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_SeekMID = env->GetMethodID(klass, "seek", "(J)J");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_CloseConnectionMID = env->GetMethodID(klass, "closeConnection", "()V");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_PropertyMID = env->GetMethodID(klass, "property", "(II)I");
            hasException = javaEnv.reportException();
        }

        if (!hasException)
        {
            m_GetStreamSizeMID = env->GetMethodID(klass, "getStreamSize", "()I");
            hasException = javaEnv.reportException();
        }

        if (NULL != klass)
            env->DeleteLocalRef(klass);

        methodIDsInitialized = !hasException;
    }

    return methodIDsInitialized;
}

bool CJavaInputStreamCallbacks::NeedBuffer()
{
    bool result = false;
    CJavaEnvironment javaEnv(m_jvm);
    JNIEnv *pEnv = javaEnv.getEnvironment();

    if (pEnv) {
        jobject connection = pEnv->NewLocalRef(m_ConnectionHolder);
        if (connection) {
            result = (pEnv->CallBooleanMethod(connection, m_NeedBufferMID) == JNI_TRUE);
            pEnv->DeleteLocalRef(connection);
        }

        javaEnv.reportException();
    }

    return result;
}

int CJavaInputStreamCallbacks::ReadNextBlock()
{
    int result = -1;
    CJavaEnvironment javaEnv(m_jvm);
    JNIEnv *pEnv = javaEnv.getEnvironment();

    if (pEnv) {
        jobject connection = pEnv->NewLocalRef(m_ConnectionHolder);
        if (connection) {
            result = pEnv->CallIntMethod(connection, m_ReadNextBlockMID);
            pEnv->DeleteLocalRef(connection);
        }

        if (javaEnv.clearException()) {
            result = -2;
        }
    }

    return result;
}

int CJavaInputStreamCallbacks::ReadBlock(int64_t position, int size)
{
    int result = -1;
    CJavaEnvironment javaEnv(m_jvm);
    JNIEnv *pEnv = javaEnv.getEnvironment();

    if (pEnv) {
        jobject connection = pEnv->NewLocalRef(m_ConnectionHolder);
        if (connection) {
            result = pEnv->CallIntMethod(connection, m_ReadBlockMID, position, size);
            pEnv->DeleteLocalRef(connection);
        }

        if (javaEnv.clearException()) {
            result = -2;
        }
    }

    return result;
}

void CJavaInputStreamCallbacks::CopyBlock(void* destination, int size)
{
    CJavaEnvironment javaEnv(m_jvm);
    JNIEnv *pEnv = javaEnv.getEnvironment();
    if (pEnv) {
        jobject connection = pEnv->NewLocalRef(m_ConnectionHolder);
        if (connection) {
            jobject buffer = pEnv->GetObjectField(connection, m_BufferFID);
            void *data = pEnv->GetDirectBufferAddress(buffer);

            memcpy(destination, data, size);
            pEnv->DeleteLocalRef(buffer);
            pEnv->DeleteLocalRef(connection);
        }
    }
 }

bool CJavaInputStreamCallbacks::IsSeekable()
{
    CJavaEnvironment javaEnv(m_jvm);
    JNIEnv *pEnv = javaEnv.getEnvironment();
    bool result = false;

    if (pEnv) {
        jobject connection = pEnv->NewLocalRef(m_ConnectionHolder);
        if (connection) {
            result = (pEnv->CallBooleanMethod(connection, m_IsSeekableMID) == JNI_TRUE);
            pEnv->DeleteLocalRef(connection);
        }

        javaEnv.reportException();
    }

    return result;
}

bool CJavaInputStreamCallbacks::IsRandomAccess()
{
    CJavaEnvironment javaEnv(m_jvm);
    JNIEnv *pEnv = javaEnv.getEnvironment();
    bool result = false;

    if (pEnv) {
        jobject connection = pEnv->NewLocalRef(m_ConnectionHolder);
        if (connection) {
            result = (pEnv->CallBooleanMethod(connection, m_IsRandomAccessMID) == JNI_TRUE);
            pEnv->DeleteLocalRef(connection);
        }

        javaEnv.reportException();
    }

    return result;
}

int64_t CJavaInputStreamCallbacks::Seek(int64_t position)
{
    CJavaEnvironment javaEnv(m_jvm);
    JNIEnv *pEnv = javaEnv.getEnvironment();
    jlong result = -1;

    if (pEnv) {
        jobject connection = pEnv->NewLocalRef(m_ConnectionHolder);
        if (connection) {
            result = pEnv->CallLongMethod(connection, m_SeekMID, (jlong)position);
            pEnv->DeleteLocalRef(connection);
        }

        javaEnv.reportException();
    }

    return (int64_t)result;
}

void CJavaInputStreamCallbacks::CloseConnection()
{
    CJavaEnvironment javaEnv(m_jvm);
    JNIEnv *pEnv = javaEnv.getEnvironment();

    if (pEnv) {
        jobject connection = pEnv->NewLocalRef(m_ConnectionHolder);
        if (connection) {
            pEnv->CallVoidMethod(connection, m_CloseConnectionMID);
            pEnv->DeleteLocalRef(connection);

            javaEnv.reportException();
        }

        pEnv->DeleteGlobalRef(m_ConnectionHolder);
        m_ConnectionHolder = NULL;
    }
}

int CJavaInputStreamCallbacks::Property(int prop, int value)
{
    CJavaEnvironment javaEnv(m_jvm);
    JNIEnv *pEnv = javaEnv.getEnvironment();
    int result = 0;

    if (pEnv) {
        jobject connection = pEnv->NewLocalRef(m_ConnectionHolder);
        if (connection) {
            result = pEnv->CallIntMethod(connection, m_PropertyMID, (jint)prop, (jint)value);
            pEnv->DeleteLocalRef(connection);
        }

        javaEnv.reportException();
    }

    return result;
}

int CJavaInputStreamCallbacks::GetStreamSize()
{
    CJavaEnvironment javaEnv(m_jvm);
    JNIEnv *pEnv = javaEnv.getEnvironment();
    int result = 0;

    if (pEnv) {
        jobject connection = pEnv->NewLocalRef(m_ConnectionHolder);
        if (connection) {
            result = pEnv->CallIntMethod(connection, m_GetStreamSizeMID);
            pEnv->DeleteLocalRef(connection);
        }

        javaEnv.reportException();
    }

    return result;
}
