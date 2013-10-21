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

#ifndef _JAVA_INPUT_STREAM_CALLBACKS_H_
#define _JAVA_INPUT_STREAM_CALLBACKS_H_

#include <jni.h>
#include <Locator/LocatorStream.h>

class CJavaInputStreamCallbacks : public CStreamCallbacks
{
public:
    CJavaInputStreamCallbacks();
virtual ~CJavaInputStreamCallbacks();

    bool Init(JNIEnv *env, jobject jLocator);

    bool NeedBuffer();
    int  ReadNextBlock();
    int  ReadBlock(int64_t position, int size);
    void CopyBlock(void* destination, int size);
    bool IsSeekable();
    bool IsRandomAccess();
    int64_t Seek(int64_t position);
    void CloseConnection();
    int  Property(int prop, int value);
    int  GetStreamSize();

private:
    jobject          m_ConnectionHolder;

    JavaVM           *m_jvm;
    static bool      m_areJMethodIDsInitialized;
    static jfieldID  m_BufferFID;
    static jmethodID m_NeedBufferMID;
    static jmethodID m_ReadNextBlockMID;
    static jmethodID m_ReadBlockMID;
    static jmethodID m_IsSeekableMID;
    static jmethodID m_IsRandomAccessMID;
    static jmethodID m_SeekMID;
    static jmethodID m_CloseConnectionMID;
    static jmethodID m_PropertyMID;
    static jmethodID m_GetStreamSizeMID;
};

#endif // _JAVA_INPUT_STREAM_CALLBACKS_H_
