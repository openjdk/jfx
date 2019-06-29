/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "PlatformJavaClasses.h"
#include "RenderingQueue.h"
#include "RQRef.h"

#include <wtf/java/JavaRef.h>
#include <wtf/HashMap.h>
#include <wtf/NeverDestroyed.h>

#include "com_sun_webkit_graphics_WCRenderQueue.h"

namespace WebCore {

typedef HashMap<char*, RefPtr<ByteBuffer> > Addr2ByteBuffer;

static Addr2ByteBuffer& getAddr2ByteBuffer()
{
    static NeverDestroyed<Addr2ByteBuffer> container;
    return container.get();
}

/*static*/
RefPtr<RenderingQueue> RenderingQueue::create(
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
    if (m_buffer && !m_buffer->hasFreeSpace(size)) {
        flushBuffer();
        if (m_autoFlush) {
            flush();
        }
    }
    if (!m_buffer) {
        m_buffer = RefPtr<ByteBuffer>(ByteBuffer::create(std::max(m_capacity, size)));
    }
    return *this;
}

void RenderingQueue::flush() {
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID midFwkFlush = env->GetMethodID(
            PG_GetRenderQueueClass(env), "fwkFlush", "()V");
    ASSERT(midFwkFlush);

    env->CallVoidMethod(getWCRenderingQueue(), midFwkFlush);
    WTF::CheckAndClearException(env);
}

void RenderingQueue::disposeGraphics() {
    JNIEnv* env = WTF::GetJavaEnv();
    // The method is called from the dtor which potentially can be called after VM detach.
    // So the check for nullptr.
    if (!env)
       return;

    static jmethodID midFwkDisposeGraphics = env->GetMethodID(
        PG_GetRenderQueueClass(env), "fwkDisposeGraphics", "()V");
    ASSERT(midFwkDisposeGraphics);

    env->CallVoidMethod(getWCRenderingQueue(), midFwkDisposeGraphics);
    WTF::CheckAndClearException(env);
}

/*
 * The method is called on Event thread (so, it's not concurrent with JS and the release of resources).
 */
RenderingQueue& RenderingQueue::flushBuffer() {
    if (isEmpty()) {
        return *this;
    }
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID midFwkAddBuffer = env->GetMethodID(PG_GetRenderQueueClass(env),
        "fwkAddBuffer", "(Ljava/nio/ByteBuffer;)V");
    ASSERT(midFwkAddBuffer);

    Addr2ByteBuffer &a2bb = getAddr2ByteBuffer();
    a2bb.set(m_buffer->bufferAddress(), m_buffer);
    env->CallVoidMethod(
        getWCRenderingQueue(),
        midFwkAddBuffer,
        (jobject)(m_buffer->createDirectByteBuffer(env)));
    WTF::CheckAndClearException(env);

    m_buffer = nullptr;

    return *this;
}
}


JNIEXPORT void JNICALL Java_com_sun_webkit_graphics_WCRenderQueue_twkRelease
    (JNIEnv* env, jobject, jobjectArray bufs)
{
    using namespace WebCore;
    /*
     * This method should be called on the Event thread to synchronize with JavaScript
     * by thread. JavaScript may access resources kept in ByteBuffer::m_refList,
     * so when a resource is dereferenced (as a result of ByteBuffer destruction)
     * it should be thread safe.
     */
    Addr2ByteBuffer& a2bb = getAddr2ByteBuffer();
    for (int i = 0; i < env->GetArrayLength(bufs); ++i) {
        char *key = (char *)env->GetDirectBufferAddress(
            JLObject(env->GetObjectArrayElement(bufs, i)));
        if (key != 0) {
            a2bb.remove(key);
        }
    }
}
