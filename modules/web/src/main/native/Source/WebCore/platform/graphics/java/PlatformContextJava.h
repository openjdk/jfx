/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef PlatformContextJava_h
#define PlatformContextJava_h

#include "GraphicsContext.h"
#include "Noncopyable.h"
#include "RenderingQueue.h"
#include "Path.h"
#include "com_sun_webkit_graphics_WCRenderQueue.h"
#include <jni.h>

namespace WebCore {

    PassRefPtr<RQRef> copyPath(PassRefPtr<RQRef> p);

    class PlatformContextJava {
        WTF_MAKE_NONCOPYABLE(PlatformContextJava);
    public:
        PlatformContextJava(const JLObject &jRQ, bool autoFlush = false) {
            m_rq = RenderingQueue::create(
                jRQ,
                com_sun_webkit_graphics_WCRenderQueue_MAX_QUEUE_SIZE / RenderingQueue::MAX_BUFFER_COUNT,
                autoFlush);
        }

        RenderingQueue& rq() const {
            return *m_rq;
        }

        PassRefPtr<RenderingQueue> rq_ref() {
            return m_rq;
        }

        void beginPath() {
            m_path.clear();
        }

        void addPath(PlatformPathPtr pPath) {
            JNIEnv* env = WebCore_GetJavaEnv();

            static jmethodID mid = env->GetMethodID(PG_GetPathClass(env),
                "addPath", "(Lcom/sun/webkit/graphics/WCPath;)V");
            ASSERT(mid);

            env->CallVoidMethod((jobject)*m_path.platformPath(), mid, (jobject)*pPath);
            CheckAndClearException(env);
        }

        PlatformPathPtr platformPath() {
            return m_path.platformPath();
        }

    private:
        RefPtr<RenderingQueue> m_rq;
        Path m_path;
    };
}

#endif  // PlatformContextJava_h
