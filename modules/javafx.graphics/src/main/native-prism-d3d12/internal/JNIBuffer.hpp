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


namespace D3D12 {
namespace Internal {

template <typename JArrayType>
class JNIBuffer
{
    JNIEnv* mJNIEnv;
    JArrayType mArray;
    void* mData;
    size_t mElementCount;

    template <typename T>
    size_t BytesPerElement() const
    {
        D3D12NI_ASSERT(false, "Bytes per element not defined for this JNIBuffer type");
        return 0;
    }

    template <>
    size_t BytesPerElement<jbyteArray>() const
    {
        return sizeof(jbyte);
    }

    template <>
    size_t BytesPerElement<jshortArray>() const
    {
        return sizeof(jshort);
    }

    template <>
    size_t BytesPerElement<jintArray>() const
    {
        return sizeof(jint);
    }

    template <>
    size_t BytesPerElement<jfloatArray>() const
    {
        return sizeof(jfloat);
    }

    void InitFromNIOBuffer(jobject buffer)
    {
        mData = mJNIEnv->GetDirectBufferAddress(buffer);
        if (mData == nullptr) return;

        mElementCount = static_cast<size_t>(mJNIEnv->GetDirectBufferCapacity(buffer));
        mArray = nullptr;
    }

    void InitFromJArray(JArrayType array)
    {
        mData = mJNIEnv->GetPrimitiveArrayCritical(array, NULL);
        if (mData == nullptr) return;

        mElementCount = static_cast<size_t>(mJNIEnv->GetArrayLength(array));
        mArray = array;
    }

public:
    JNIBuffer(JNIEnv* env, jobject buffer, JArrayType array)
        : mJNIEnv(env)
        , mArray(nullptr)
        , mData(nullptr)
        , mElementCount(0)
    {
        if (array != nullptr) InitFromJArray(array);
        else InitFromNIOBuffer(buffer);
    }

    ~JNIBuffer()
    {
        if (mArray != nullptr)
        {
            mJNIEnv->ReleasePrimitiveArrayCritical(mArray, mData, 0);
        }
    }

    inline void* Data() const
    {
        return mData;
    }

    inline size_t Count() const
    {
        return mElementCount;
    }

    inline size_t Size() const
    {
        return mElementCount * BytesPerElement<JArrayType>();
    }

    inline size_t BytesPerElement() const
    {
        return BytesPerElement<JArrayType>();
    }

    inline operator bool() const
    {
        return (mData != nullptr);
    }
};

} // namespace Internal
} // namespace D3D12
