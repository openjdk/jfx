/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "RenderingQueue.h"
#include "RQRef.h"
#include "JavaRef.h"
#include "JavaEnv.h"
#include <wtf/HashMap.h>

#include "com_sun_webkit_graphics_WCRenderQueue.h"

namespace WebCore {

    typedef HashMap<char*, RefPtr<ByteBuffer> > Addr2ByteBuffer;

    static Addr2ByteBuffer &getAddr2ByteBuffer()
    {
        DEFINE_STATIC_LOCAL(Addr2ByteBuffer, container, ());
        return container;
    }

    /*static*/
    PassRefPtr<RenderingQueue> RenderingQueue::create(
        const JLObject &jRQ,
        int capacity,
        bool autoFlush)
    {
        return adoptRef(new RenderingQueue(
            jRQ,
            capacity,
            autoFlush));
    }

    RenderingQueue& RenderingQueue::freeSpace(int size) {
        if (m_buffer != NULL && !m_buffer->hasFreeSpace(size)) {
            flushBuffer();
            if (m_autoFlush) {
                flush();
            }
        }
        if (m_buffer == NULL) {
            m_buffer = RefPtr<ByteBuffer>(ByteBuffer::create(std::max(m_capacity, size)));
        }
        return *this;
    }

    void RenderingQueue::flush() {
        JNIEnv* env = WebCore_GetJavaEnv();

        static jmethodID midFwkFlush = env->GetMethodID(
                PG_GetRenderQueueClass(env), "fwkFlush", "()V");
        ASSERT(midFwkFlush);

        env->CallVoidMethod(getWCRenderingQueue(), midFwkFlush);
        CheckAndClearException(env);
    }

    /*
     * The method is called on Event thread (so, it's not concurrent with JS and the release of resources).
     */
    RenderingQueue& RenderingQueue::flushBuffer() {
        if (isEmpty()) {
            return *this;
        }
        JNIEnv* env = WebCore_GetJavaEnv();

        static jmethodID midFwkAddBuffer = env->GetMethodID(PG_GetRenderQueueClass(env),
            "fwkAddBuffer", "(Ljava/nio/ByteBuffer;)V");
        ASSERT(midFwkAddBuffer);

        Addr2ByteBuffer &a2bb = getAddr2ByteBuffer();
        a2bb.set(m_buffer->bufferAddress(), m_buffer);
        env->CallVoidMethod(
            getWCRenderingQueue(),
            midFwkAddBuffer,
            (jobject)(m_buffer->createDirectByteBuffer(env)));
        CheckAndClearException(env);

        m_buffer = NULL;

        return *this;
    }
}

using namespace WebCore;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_sun_webkit_graphics_WCRenderQueue_twkRelease
    (JNIEnv* env, jobject clazz, jobjectArray bufs)
{
    /*
     * This method should be called on the Event thread to synchronize with JavaScript
     * by thread. JavaScript may access resources kept in ByteBuffer::m_refList,
     * so when a resource is dereferenced (as a result of ByteBuffer destruction)
     * it should be thread safe.
     */
    Addr2ByteBuffer &a2bb = getAddr2ByteBuffer();
    for (int i = 0; i < env->GetArrayLength(bufs); ++i) {
        char *key = (char *)env->GetDirectBufferAddress(
            JLObject(env->GetObjectArrayElement(bufs, i)));
        if (key != 0) {
            a2bb.remove(key);
        }
    }
}

#ifdef __cplusplus
}
#endif
