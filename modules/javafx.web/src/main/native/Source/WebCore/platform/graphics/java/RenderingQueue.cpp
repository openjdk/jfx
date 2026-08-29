/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "RenderingQueue.h"

#include "RQRef.h"
#include "WKJPlatformJava.h"

#include <wtf/HashMap.h>
#include <wtf/NeverDestroyed.h>

// WKJ_EXPORT, wkj_rq_release and WKJHostGraphics all arrive with webkit_java_api.h, which
// WKJPlatformJava.h includes; webkit_java_api_platform.h is not includable on its own.


namespace WebCore {

typedef HashMap<char*, RefPtr<ByteBuffer> > Addr2ByteBuffer;

static Addr2ByteBuffer& getAddr2ByteBuffer()
{
    static NeverDestroyed<Addr2ByteBuffer> container;
    return container.get();
}

void ByteBuffer::addToRenderQueue(wkj_ref renderQueue)
{
    ASSERT(!isEmpty());

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->rq_add_buffer)
        return;

    m_nio_holder = WKJHandle(cb->rq_add_buffer(renderQueue, m_buffer, m_position));
    wkjCheckAndClearException();
}

/*static*/
RefPtr<RenderingQueue> RenderingQueue::create(
    wkj_ref jRQ,
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
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->rq_flush)
        return;

    cb->rq_flush(getWCRenderingQueue());
    wkjCheckAndClearException();
}

void RenderingQueue::disposeGraphics() {
    // The method is called from the dtor which potentially can be called after VM detach.
    // So the check for a host table, which is what the null environment check meant.
    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->rq_dispose_graphics)
       return;

    cb->rq_dispose_graphics(getWCRenderingQueue());
    wkjCheckAndClearException();
}

/*
 * The method is called on Event thread (so, it's not concurrent with JS and the release of resources).
 */
RenderingQueue& RenderingQueue::flushBuffer() {
    if (isEmpty()) {
        return *this;
    }

    Addr2ByteBuffer &a2bb = getAddr2ByteBuffer();
    a2bb.set(m_buffer->bufferAddress(), m_buffer);
    m_buffer->addToRenderQueue(getWCRenderingQueue());

    m_buffer = nullptr;

    return *this;
}

} // namespace WebCore

extern "C" {

WKJ_EXPORT void wkj_rq_release(const int64_t* buffer_addrs, int32_t count)
{
    using namespace WebCore;
    /*
     * This method should be called on the Event thread to synchronize with JavaScript
     * by thread. JavaScript may access resources kept in ByteBuffer::m_refList,
     * so when a resource is dereferenced (as a result of ByteBuffer destruction)
     * it should be thread safe.
     */
    if (!buffer_addrs)
        return;

    Addr2ByteBuffer& a2bb = getAddr2ByteBuffer();
    for (int32_t i = 0; i < count; ++i) {
        char* key = static_cast<char*>(wkj_to_ptr(buffer_addrs[i]));
        if (key != 0) {
            a2bb.remove(key);
        }
    }
}

} // extern "C"
