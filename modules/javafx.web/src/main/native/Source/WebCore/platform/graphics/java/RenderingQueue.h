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

#pragma once

#include <wtf/Vector.h>
#include <wtf/RefCounted.h>
#include <wtf/HashSet.h>
#include <wtf/java/WKJHandle.h>

#include "RQRef.h"

namespace WebCore {

class RQRef;

class ByteBuffer : public RefCounted<ByteBuffer> {
public:
    static RefPtr<ByteBuffer> create(int capacity) {
        return adoptRef(new ByteBuffer(capacity));
    }

    /*
     * Hands this buffer to Java's WCRenderQueue, which wraps the address without copying and
     * appends it to the queue. This is NewDirectByteBuffer followed by fwkAddBuffer: the
     * number of Java calls is unchanged at one, because wrapping an address is a JNI service
     * that has no counterpart in the C ABI and so moves to the Java side of the callback.
     *
     * The id that comes back is the Java ByteBuffer object. It is held here, in m_nio_holder,
     * for exactly as long as this C++ ByteBuffer lives - that is, until wkj_rq_release drops
     * the last reference to it - so the Java object cannot be collected while the queue still
     * refers to the memory. That is the lifetime the global reference gave it.
     */
    void addToRenderQueue(wkj_ref renderQueue);

    char* bufferAddress() { return m_buffer; }

    void putRef(RefPtr<RQRef> ref) {
        ASSERT(m_position + sizeof(int32_t) <= m_capacity);
        RefPtr<RQRef> repeatable_use_holder(ref);
        m_refList.append(repeatable_use_holder);
        putInt(static_cast<int32_t>(*repeatable_use_holder));
    }

    void putInt(int32_t i) {
        ASSERT(m_position + sizeof(int32_t) <= m_capacity);
        memcpy((m_buffer + m_position), &i, sizeof(int32_t));
        m_position += sizeof(int32_t);
    }

    void putFloat(float f) {
        ASSERT(m_position + sizeof(float) <= m_capacity);
        memcpy((m_buffer + m_position), &f, sizeof(float));
        m_position += sizeof(float);
    }

    bool hasFreeSpace(int size) { return m_position + size <= m_capacity; }

    bool isEmpty() { return m_position == 0; }

    ~ByteBuffer() {
        delete[] m_buffer;
    }

private:
    ByteBuffer(int capacity) :
        m_buffer(new char[capacity]),
        m_capacity(capacity),
        m_position(0)
    {}

    char* m_buffer;
    int m_capacity;
    int m_position;
    WKJHandle m_nio_holder;
    Vector< RefPtr<RQRef> > m_refList;
};

/*
 * A lifecycle of an instance of RenderingQueue (RQ) used to draw to ImageBufferJava
 * may continue after the RQ is flushed to java (e.g. when it's used for html5 canvas).
 * All rendering operations are written to a byte buffer. When the RQ is flushed, the
 * buffer is sent to an instance of Java's WCRenderingQueue class for processing.
 *
 * Also note that JavaScript may draw into canvas on the Event thread in time
 * other than WebPage::updateContent is called. Thus it may happen concurrently
 * with rendering (performed on the Render thread on the java side).
 */
class RenderingQueue : public RefCounted<RenderingQueue> {
public:
    static const size_t MAX_BUFFER_COUNT = 8;

    static RefPtr<RenderingQueue> create(
        wkj_ref jRQ,
        int capacity,
        bool autoFlush);

    int capacity() { return m_capacity; }

    RenderingQueue& operator << (RefPtr<RQRef> r) {
        m_buffer->putRef(r);
        return *this;
    }

    RenderingQueue& operator << (int32_t i) {
        m_buffer->putInt(i);
        return *this;
    }

    RenderingQueue& operator << (float f) {
        m_buffer->putFloat(f);
        return *this;
    }

    RenderingQueue& freeSpace(int size);
    RenderingQueue& flushBuffer();

    bool isEmpty() {
        return m_buffer == nullptr || m_buffer->isEmpty();
    }

    /*
     * The Java WCRenderQueue, borrowed: the id belongs to m_rqoRenderingQueue and stays valid
     * for the lifetime of this object. The JNI version minted a local ref per call; borrowing
     * the owner's id is the same reference with no registry traffic.
     */
    wkj_ref getWCRenderingQueue() const {
        return wkj_ref(*m_rqoRenderingQueue);
    }

    //this method need for enclosed Queue serialization
    //used in [BufferImage::draw]
    RefPtr<RQRef> getRQRenderingQueue() {
        return m_rqoRenderingQueue;
    }

    ~RenderingQueue() {
        disposeGraphics();
    }

private:
    RenderingQueue(wkj_ref jRQ, int capacity, bool autoFlush) :
        m_rqoRenderingQueue(RQRef::create(jRQ)),
        m_capacity(capacity),
        m_autoFlush(autoFlush),
        m_buffer(nullptr)
    {}

    void flush();
    void disposeGraphics();

    //we need to have RQRef here due to [deref]
    //callback in destructor. Texture need to be released.
    RefPtr<RQRef> m_rqoRenderingQueue;

    int m_capacity;
    bool m_autoFlush;
    RefPtr<ByteBuffer> m_buffer; // ref to the current ByteBuffer

};
} // namespace WebCore
