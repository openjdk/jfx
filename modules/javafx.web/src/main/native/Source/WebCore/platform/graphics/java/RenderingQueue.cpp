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
#include <wtf/Lock.h>
#include <wtf/NeverDestroyed.h>
#include <wtf/java/WKJRuntime.h>
#include "WKJDOMUtils.h"

// WKJ_EXPORT, wkj_rq_release and WKJHostGraphics all arrive with webkit_java_api.h, which
// WKJPlatformJava.h includes; webkit_java_api_platform.h is not includable on its own.


namespace WebCore {

typedef HashMap<char*, RefPtr<ByteBuffer> > Addr2ByteBuffer;

/*
 * a2bb holds every command buffer handed to a Java WCRenderQueue and not yet released, keyed
 * by the buffer address that flushBuffer then passes to rq_add_buffer (through
 * ByteBuffer::addToRenderQueue), and its reference keeps the buffer alive while Java reads it.
 * flushBuffer adds on the thread that owns the RenderingQueue: the WebKit main thread, or a
 * Web Worker thread (see flushBuffer). wkj_rq_release removes on the event thread, which is
 * the main thread. addr2ByteBufferLock guards every access to the table and nothing else. No
 * upcall runs while it is held: a value leaves the table by move and is dropped after the
 * lock is released, because the last reference to a ByteBuffer runs its destructor, which
 * releases Java objects through upcalls.
 */
static Lock addr2ByteBufferLock;

static Addr2ByteBuffer& getAddr2ByteBuffer() WTF_REQUIRES_LOCK(addr2ByteBufferLock)
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
    // Called from the destructor. JNI skipped this on a thread with no JNIEnv; the port gates
    // it on the shutdown flag instead. See THE SHUTDOWN GATE in wtf/java/WKJRuntime.h.
    WKJ_RETURN_IF_SHUTTING_DOWN();

    const WKJHostGraphics* cb = wkjGraphics();
    if (!cb || !cb->rq_dispose_graphics)
       return;

    cb->rq_dispose_graphics(getWCRenderingQueue());
    wkjCheckAndClearException();
}

/*
 * Called on the event thread, which is the WebKit main thread, so there it is not concurrent
 * with JavaScript or with the release of resources in wkj_rq_release.
 *
 * A Web Worker thread gets here too, through createImageBitmap and the structured clone of an
 * ImageBitmap it holds. In the JNI build that commit 939aa61ead replaced, that thread had no
 * JNIEnv past WorkerThread::createGlobalScope and crashed on the fwkAddBuffer call that follows
 * a2bb.set; an upcall stub attaches the thread by itself, so the call now completes. For a
 * worker's buffer, wkj_rq_release on the event thread can take it out of a2bb as soon as Java
 * has it, before m_buffer lets go of it here, so either thread may drop the last reference.
 * That is why a2bb is locked and why ByteBuffer and RQRef count references atomically.
 */
RenderingQueue& RenderingQueue::flushBuffer() {
    if (isEmpty()) {
        return *this;
    }

    // a2bb.set(address, m_buffer), with a replaced value dropped outside the lock. There is
    // none in practice: an address stays in a2bb only while its buffer is alive.
    RefPtr<ByteBuffer> replaced;
    {
        Locker locker { addr2ByteBufferLock };
        auto result = getAddr2ByteBuffer().add(m_buffer->bufferAddress(), m_buffer);
        if (!result.isNewEntry)
            replaced = std::exchange(result.iterator->value, m_buffer);
    }
    replaced = nullptr;

    m_buffer->addToRenderQueue(getWCRenderingQueue());

    m_buffer = nullptr;

    return *this;
}

} // namespace WebCore

extern "C" {

WKJ_EXPORT void wkj_rq_release(const int64_t* buffer_addrs, int32_t count)
{
    using namespace WebCore;
    WKJCallScope wkjScope;
    /*
     * This method should be called on the Event thread to synchronize with JavaScript
     * by thread. JavaScript may access resources kept in ByteBuffer::m_refList,
     * so when a resource is dereferenced (as a result of ByteBuffer destruction)
     * it should be thread safe.
     */
    if (!buffer_addrs)
        return;

    /*
     * The buffers leave a2bb under its lock and are dropped after it, in the order given,
     * when "released" goes out of scope. A buffer that a Web Worker thread is still flushing
     * keeps a reference there, and the worker then drops the last one instead.
     */
    Vector<RefPtr<ByteBuffer>> released;
    if (count > 0)
        released.reserveInitialCapacity(static_cast<size_t>(count));
    {
        Locker locker { addr2ByteBufferLock };
        Addr2ByteBuffer& a2bb = getAddr2ByteBuffer();
        for (int32_t i = 0; i < count; ++i) {
            char* key = static_cast<char*>(wkj_to_ptr(buffer_addrs[i]));
            if (key != 0) {
                if (RefPtr<ByteBuffer> buffer = a2bb.take(key))
                    released.append(WTF::move(buffer));
            }
        }
    }
}

} // extern "C"
