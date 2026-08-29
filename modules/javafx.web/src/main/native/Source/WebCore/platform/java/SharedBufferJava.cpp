/*
 * Copyright (c) 2012, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "SharedBuffer.h"
#include "NotImplemented.h"
#include "WKJDOMUtils.h"

#include <cstring>
#include <webkit_java_api.h>

namespace WebCore {

// JDK-8146959
RefPtr<SharedBuffer> SharedBuffer::createFromReadingFile(const String&)
{
  notImplemented();
  return {};
}

extern "C" {

WKJ_EXPORT int64_t wkj_shared_buffer_create(void)
{
   WKJCallScope wkjScope;
   auto buffer = SharedBuffer::create();
   return wkj_from_ptr(new SharedBufferBuilder(WTF::move(buffer)));
}

WKJ_EXPORT int64_t wkj_shared_buffer_size(int64_t nativePointer)
{
    WKJCallScope wkjScope;
    FragmentedSharedBuffer* p = static_cast<FragmentedSharedBuffer*>(wkj_to_ptr(nativePointer));
    ASSERT(p);
    return p->size();
}

WKJ_EXPORT int32_t wkj_shared_buffer_get_some_data(int64_t nativePointer, int64_t position,
                                                   uint8_t* buffer, int32_t offset,
                                                   int32_t length)
{
    WKJCallScope wkjScope;
    FragmentedSharedBuffer* p = static_cast<FragmentedSharedBuffer*>(wkj_to_ptr(nativePointer));
    ASSERT(p);
    ASSERT(position >= 0);
    ASSERT(buffer);
    ASSERT(offset >= 0);
    ASSERT(length >= 0);

    if ((size_t)position >= p->size()) {
        return 0;
    }

    const auto& dataView = p->getSomeData(position);
    const uint8_t* segment = dataView.span().data();
    int len = dataView.size();
    if (len) {
        if (len > length) {
            len = length;
        }
        memcpy(buffer + offset, segment, len);
    }

    return len;
}

WKJ_EXPORT void wkj_shared_buffer_append(int64_t nativePointer, const uint8_t* buffer,
                                         int32_t offset, int32_t length)
{
    WKJCallScope wkjScope;
    SharedBufferBuilder* p = static_cast<SharedBufferBuilder*>(wkj_to_ptr(nativePointer));
    ASSERT(p);
    ASSERT(buffer);
    ASSERT(offset >= 0);
    ASSERT(length >= 0);

    std::span<const uint8_t> spanBuffer(buffer + offset, length);
    p->append(spanBuffer);
}

/*
 * KNOWN LEAK, REPRODUCED ON PURPOSE.
 *
 * This body casts the pointer and does nothing with it, exactly as the JNI twkDispose did,
 * so every SharedBufferBuilder allocated by wkj_shared_buffer_create is still leaked. That
 * is a bug (FFM-AUDIT-wtf-webcore.md section 9.2, "Bug, not a verdict"), and deleting the
 * builder here is a one-line fix - but it is a fix, and a migration commit has to be
 * behaviour-neutral. It needs its own commit, with evidence that no SharedBuffer created
 * from this builder is still referenced when Java disposes it, because turning a leak into
 * a double free is a much worse trade.
 */
WKJ_EXPORT void wkj_shared_buffer_dispose(int64_t nativePointer)
{
    WKJCallScope wkjScope;
    FragmentedSharedBuffer* p = static_cast<FragmentedSharedBuffer*>(wkj_to_ptr(nativePointer));
    ASSERT(p);
    UNUSED_PARAM(p);
}

}
}   // namespace WebCore
