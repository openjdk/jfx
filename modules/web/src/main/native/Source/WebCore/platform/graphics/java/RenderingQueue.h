/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef RenderingQueue_h
#define RenderingQueue_h

#include <jni.h>
#include "RQRef.h"
#include <wtf/Vector.h>
#include <wtf/RefCounted.h>
#include <wtf/HashSet.h>
#include <wtf/java/DbgUtils.h>

namespace WebCore {

    class RQRef;

    class ByteBuffer : public RefCounted<ByteBuffer> {
        RQ_LOG_INSTANCE_COUNT(ByteBuffer)
    public:
        static PassRefPtr<ByteBuffer> create(int capacity) {
            return adoptRef(new ByteBuffer(capacity));
        }

        JLObject createDirectByteBuffer(JNIEnv* env) {
            ASSERT(!isEmpty());
            JLObject ret(env->NewDirectByteBuffer(m_buffer, m_position));
            m_nio_holder = ret;
            return ret;
        }

        char *bufferAddress() { return m_buffer; }

        void putRef(PassRefPtr<RQRef> ref) {
            ASSERT(m_position + sizeof(jint) <= m_capacity);
            RefPtr<RQRef> repeatable_use_holder(ref);
            m_refList.append(repeatable_use_holder);
            putInt((jint)*repeatable_use_holder);
        }

        void putInt(jint i) {
            ASSERT(m_position + sizeof(jint) <= m_capacity);
            *(jint*)(m_buffer + m_position) = i;
            m_position += sizeof(jint);
        }

        void putFloat(jfloat f) {
            ASSERT(m_position + sizeof(jfloat) <= m_capacity);
            *(jfloat*)(m_buffer + m_position) = f;
            m_position += sizeof(jfloat);
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
        int m_position;
        int m_capacity;
        JGObject m_nio_holder;
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
        RQ_LOG_INSTANCE_COUNT(RenderingQueue)
    public:
        static const size_t MAX_BUFFER_COUNT = 8;

        static PassRefPtr<RenderingQueue> create(
            const JLObject &jRQ,
            int capacity,
            bool autoFlush);

        int capacity() { return m_capacity; }

        RenderingQueue& operator << (PassRefPtr<RQRef> r) {
            m_buffer->putRef(r);
            return *this;
        }

        RenderingQueue& operator << (jint i) {
            m_buffer->putInt(i);
            return *this;
        }

        RenderingQueue& operator << (jfloat f) {
            m_buffer->putFloat(f);
            return *this;
        }

        RenderingQueue& freeSpace(int size);
        RenderingQueue& flushBuffer();

        bool isEmpty() {
            return m_buffer == NULL || m_buffer->isEmpty();
        }

        JLObject getWCRenderingQueue() {
            return m_rqoRenderingQueue->cloneLocalCopy();
        }

        //this method need for enclosed Queue serialization
        //used in [BufferImage::draw]
        PassRefPtr<RQRef> getRQRenderingQueue() {
            return m_rqoRenderingQueue;
        }

        ~RenderingQueue() {
        }

    private:
        RenderingQueue(const JLObject &jRQ, int capacity, bool autoFlush) :
            m_rqoRenderingQueue(RQRef::create(jRQ)),
            m_capacity(capacity),
            m_autoFlush(autoFlush),
            m_buffer(NULL)
        {}

        void flush();

        int m_capacity;
        bool m_autoFlush;
        RefPtr<ByteBuffer> m_buffer; // ref to the current ByteBuffer

        //we need to have RQRef here due to [deref]
        //callback in destructor. Texture need to be released.
        RefPtr<RQRef> m_rqoRenderingQueue;
    };
}

#endif
