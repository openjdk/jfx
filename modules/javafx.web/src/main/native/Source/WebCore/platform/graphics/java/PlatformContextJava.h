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

#pragma once

#include "GraphicsContext.h"
#include "Path.h"
#include "RenderingQueue.h"
#include "com_sun_webkit_graphics_WCRenderQueue.h"
#include <jni.h>
#include <wtf/Noncopyable.h>

namespace WebCore {

    RefPtr<RQRef> copyPath(RefPtr<RQRef> p);

    class PlatformContextJava {
        WTF_MAKE_NONCOPYABLE(PlatformContextJava);
    public:
        PlatformContextJava(const JLObject& jRQ, RefPtr<RQRef> jTheme, bool autoFlush = false)
            : m_rq(RenderingQueue::create(jRQ, com_sun_webkit_graphics_WCRenderQueue_MAX_QUEUE_SIZE / RenderingQueue::MAX_BUFFER_COUNT, autoFlush))
            , m_jRenderTheme(jTheme)
        {}

        PlatformContextJava(const JLObject& jRQ, bool autoFlush = false)
            : PlatformContextJava(jRQ, nullptr, autoFlush)
        {}

        RenderingQueue& rq() const {
            return *m_rq;
        }

        RefPtr<RenderingQueue> rq_ref() {
            return m_rq;
        }

        RefPtr<RQRef> jRenderTheme() const {
            return m_jRenderTheme;
        }

        void setJRenderTheme(RefPtr<RQRef> jTheme) {
            m_jRenderTheme = jTheme;
        }

        void beginPath() {
            m_path.clear();
        }

        void addPath(PlatformPathPtr pPath) {
            JNIEnv* env = WTF::GetJavaEnv();

            static jmethodID mid = env->GetMethodID(PG_GetPathClass(env),
                "addPath", "(Lcom/sun/webkit/graphics/WCPath;)V");
            ASSERT(mid);

            env->CallVoidMethod((jobject)*m_path.platformPath(), mid, (jobject)*pPath);
            WTF::CheckAndClearException(env);
        }

        PlatformPathPtr platformPath() {
            return m_path.platformPath();
        }

    private:
        RefPtr<RenderingQueue> m_rq;
        RefPtr<RQRef> m_jRenderTheme;
        Path m_path;
    };
}
