/*
 * Copyright (c) 2012, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"

#include "SharedBuffer.h"
#include "NotImplemented.h"
#include "com_sun_webkit_SharedBuffer.h"

namespace WebCore {

// JDK-8146959
RefPtr<SharedBuffer> SharedBuffer::createFromReadingFile(const String&)
{
  notImplemented();
  return {};
}

extern "C" {

JNIEXPORT jlong JNICALL Java_com_sun_webkit_SharedBuffer_twkCreate
  (JNIEnv*, jclass)
{
   auto buffer_address = &FragmentedSharedBuffer::create().get();
   return ptr_to_jlong(new SharedBufferBuilder(std::move(buffer_address)));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_SharedBuffer_twkSize
  (JNIEnv*, jclass, jlong nativePointer)
{
    FragmentedSharedBuffer* p = static_cast<FragmentedSharedBuffer*>(jlong_to_ptr(nativePointer));
    ASSERT(p);
    return p->size();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_SharedBuffer_twkGetSomeData
  (JNIEnv* env, jclass, jlong nativePointer, jlong position, jbyteArray buffer,
   jint offset, jint length)
{
    FragmentedSharedBuffer* p = static_cast<FragmentedSharedBuffer*>(jlong_to_ptr(nativePointer));
    ASSERT(p);
    ASSERT(position >= 0);
    ASSERT(buffer);
    ASSERT(offset >= 0);
    ASSERT(length >= 0);

    if ((size_t)position >= p->size()) {
        return 0;
    }

    const auto& dataView = p->getSomeData(position);
    const uint8_t* segment = dataView.span().data();
    int len = dataView.size();
    if (len) {
        if (len > length) {
            len = length;
        }
        uint8_t* bufferBody = static_cast<uint8_t*>(
                env->GetPrimitiveArrayCritical(buffer, NULL));
        memcpy(bufferBody + offset, segment, len);
        env->ReleasePrimitiveArrayCritical(buffer, bufferBody, 0);
    }

    return len;
}

JNIEXPORT void JNICALL Java_com_sun_webkit_SharedBuffer_twkAppend
  (JNIEnv* env, jclass, jlong nativePointer, jbyteArray buffer,
   jint offset, jint length)
{
    SharedBufferBuilder* p = static_cast<SharedBufferBuilder*>(jlong_to_ptr(nativePointer));
    ASSERT(p);
    ASSERT(buffer);
    ASSERT(offset >= 0);
    ASSERT(length >= 0);

    char* bufferBody = static_cast<char*>(
            env->GetPrimitiveArrayCritical(buffer, NULL));
    std::span<const uint8_t> spanBuffer(reinterpret_cast<const uint8_t*>(bufferBody + offset), length);
    p->append(spanBuffer);
    env->ReleasePrimitiveArrayCritical(buffer, bufferBody, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_SharedBuffer_twkDispose
  (JNIEnv *, jclass, jlong nativePointer)
{
    FragmentedSharedBuffer* p = static_cast<FragmentedSharedBuffer*>(jlong_to_ptr(nativePointer));
    ASSERT(p);
        UNUSED_PARAM(p);
}

}
}   // namespace WebCore
