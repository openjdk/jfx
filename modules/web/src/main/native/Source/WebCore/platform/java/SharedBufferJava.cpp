/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "SharedBuffer.h"
#include "com_sun_webkit_SharedBuffer.h"

using namespace WebCore;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_com_sun_webkit_SharedBuffer_twkCreate
  (JNIEnv*, jclass)
{
    return ptr_to_jlong(SharedBuffer::create().leakRef());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_SharedBuffer_twkSize
  (JNIEnv*, jclass, jlong nativePointer)
{
    SharedBuffer* p = static_cast<SharedBuffer*>(jlong_to_ptr(nativePointer));
    ASSERT(p);
    return p->size();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_SharedBuffer_twkGetSomeData
  (JNIEnv* env, jclass, jlong nativePointer, jlong position, jbyteArray buffer,
   jint offset, jint length)
{
    SharedBuffer* p = static_cast<SharedBuffer*>(jlong_to_ptr(nativePointer));
    ASSERT(p);
    ASSERT(position >= 0);
    ASSERT(buffer);
    ASSERT(offset >= 0);
    ASSERT(length >= 0);

    const char* segment;
    unsigned len = p->getSomeData(segment, position);
    if (len) {
        if (len > length) {
            len = length;
        }
        char* bufferBody = static_cast<char*>(
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
    SharedBuffer* p = static_cast<SharedBuffer*>(jlong_to_ptr(nativePointer));
    ASSERT(p);
    ASSERT(buffer);
    ASSERT(offset >= 0);
    ASSERT(length >= 0);

    char* bufferBody = static_cast<char*>(
            env->GetPrimitiveArrayCritical(buffer, NULL));
    p->append(bufferBody + offset, length);
    env->ReleasePrimitiveArrayCritical(buffer, bufferBody, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_SharedBuffer_twkDispose
  (JNIEnv *, jclass, jlong nativePointer)
{
    SharedBuffer* p = static_cast<SharedBuffer*>(jlong_to_ptr(nativePointer));
    ASSERT(p);
    p->deref();
}

#ifdef __cplusplus
}
#endif
