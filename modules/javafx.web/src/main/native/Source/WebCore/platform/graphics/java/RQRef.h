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

#include <wtf/RefPtr.h>
#include <wtf/ThreadSafeRefCounted.h>
#include <wtf/java/WKJHandle.h>

namespace WebCore {

/*
 * A Java com.sun.webkit.graphics.Ref held by native code.
 *
 * Two identifiers name the same object and both are needed, which is worth stating because it
 * looks redundant:
 *
 *   m_ref    a wkj_ref, i.e. a registry id for the Java object itself. It is what gets handed
 *            back to Java whenever the object - not a token for it - has to cross the
 *            boundary: CursorJava, PasteboardJava, DragClientJava, PopupMenuJava and
 *            ImageBufferJavaBackend all pass one on. It replaces the global reference the
 *            class used to hold, and it keeps the object reachable for the whole lifetime of
 *            this RQRef.
 *   m_refID  the int the Java side assigned in WCGraphicsManager.createID(), fetched lazily
 *            by operator int32_t(). This is what the render-queue command buffer carries, and
 *            WCGraphicsManager.refMap maps it back to the object - but only between ref() and
 *            deref(), so it cannot stand in for m_ref: before the first operator int32_t()
 *            there is no refMap entry at all, and nothing else would keep the Java object
 *            alive. The two are not interchangeable and neither can be dropped.
 *
 * The reference count is atomic. The thread that draws with the object takes references -
 * the WebKit main thread, or a Web Worker thread for the images behind createImageBitmap -
 * and so does every command buffer that names it (ByteBuffer::m_refList). wkj_rq_release can
 * drop a buffer's references on the event thread while a worker still holds its own, so the
 * destructor runs on whichever thread drops the last one. Its upcalls, graphics.ref_deref and
 * core.release, are documented as safe on any thread.
 */
class RQRef : public ThreadSafeRefCounted<RQRef> {
public:
    /* Adds a reference to `obj`; the caller keeps its own, as the global reference did. */
    inline static RefPtr<RQRef> create(wkj_ref obj)
    {
        return obj ? adoptRef(new RQRef(obj)) : nullptr;
    }

    /* The Java-assigned int id, fetching it and ref()ing the object on first use. */
    explicit operator int32_t();

    /* The registry id, borrowed: ownership stays with this RQRef. */
    operator wkj_ref() const { return m_ref.get(); }

    /*
     * A new id for the same object, owned by the caller. This is what cloneLocalCopy() was:
     * a fresh reference with a scope of its own, which the WKJHandle destructor releases.
     */
    WKJHandle retainedRef() const { return WKJHandle::retained(m_ref.get()); }

    ~RQRef();

private:
    explicit RQRef(wkj_ref obj)
        : m_ref(WKJHandle::retained(obj))
        , m_refID(-1)
    {}

    /*
     * Threads: m_ref is set by the constructor and released by the destructor, and is only
     * read in between. After the constructor's -1, m_refID is written only by
     * operator int32_t, on the thread that draws with the object: with a resolved id at most
     * once, and, while ref_get_id answers -1, with that -1 on every call, so a -1 answer is
     * rewritten only by that same drawing thread. Two threads never resolve it at once: an
     * object reaches a second drawing thread only by being handed over, as an ImageBitmap's
     * image is by a transfer with postMessage, and the hand-over orders the two threads'
     * calls. The destructor reads m_refID after the atomic count has ordered it after the
     * last write.
     */
    WKJHandle m_ref;
    int32_t m_refID;
};

}
