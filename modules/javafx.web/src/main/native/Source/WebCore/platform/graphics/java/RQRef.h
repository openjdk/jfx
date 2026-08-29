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

#include <wtf/RefCounted.h>
#include <wtf/RefPtr.h>
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
 */
class RQRef : public RefCounted<RQRef> {
public:
    /* Adds a reference to `obj`; the caller keeps its own, as the global reference did. */
    inline static RefPtr<RQRef> create(wkj_ref obj)
    {
        return obj ? adoptRef(new RQRef(obj)) : nullptr;
    }

    /* The Java-assigned int id, fetching it and ref()ing the object on first use. */
    operator int32_t();

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

    WKJHandle m_ref;
    int32_t m_refID;
};

}
