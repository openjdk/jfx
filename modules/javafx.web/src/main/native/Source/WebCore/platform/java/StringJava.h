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

#include <stdint.h>

#include "PlatformJavaClasses.h"

#include <memory>

#include <wtf/Noncopyable.h>
#include <wtf/StdLibExtras.h>
#include <wtf/Vector.h>
#include <wtf/text/AtomString.h>

namespace WebCore {
using WTF::AtomString;

/*
 * A Vector<AtomString> presented to a callback slot as a Java String[] would have been.
 *
 * This replaces strVect2JArray(), which built a real Java array of strings. There is no
 * array object in the C ABI - contract 2 says a sequence crosses as (pointer, count) - so a
 * String[] becomes two parallel arrays: one of UTF-16 pointers and one of lengths, both of
 * `count` entries. The Java side reads `count` pairs and builds the String[] itself.
 *
 * Every element is materialised into its own buffer, which is the same reason PasteboardJava
 * holds one WKJStringArg per string: contract 13 leaves the caller owning all string memory,
 * so two strings alive at once need two buffers, and here a whole vector is alive at once.
 *
 * The instance owns the buffers, so it must outlive the call it is passed to. An empty vector
 * gives count() == 0 and NULL pointers, which is what strVect2JArray's zero-length array did.
 */
class WKJStringArrayArg final {
    WTF_MAKE_NONCOPYABLE(WKJStringArrayArg);
public:
    explicit WKJStringArrayArg(const Vector<AtomString>& strings)
    {
        const size_t count = strings.size();
        if (!count)
            return;

        m_storage.reserveInitialCapacity(count);
        m_data.reserveInitialCapacity(count);
        m_lengths.reserveInitialCapacity(count);

        for (size_t i = 0; i < count; ++i) {
            // makeUniqueWithoutFastMallocCheck, because WKJStringArg is not FastMalloc-annotated.
            m_storage.append(makeUniqueWithoutFastMallocCheck<WKJStringArg>(strings[i].string()));
            m_data.append(m_storage[i]->data());
            m_lengths.append(m_storage[i]->length());
        }
    }

    const uint16_t* const* data() const { return m_data.isEmpty() ? nullptr : m_data.span().data(); }
    const int32_t* lengths() const { return m_lengths.isEmpty() ? nullptr : m_lengths.span().data(); }
    int32_t count() const { return static_cast<int32_t>(m_data.size()); }

private:
    Vector<std::unique_ptr<WKJStringArg>> m_storage;
    Vector<const uint16_t*> m_data;
    Vector<int32_t> m_lengths;
};

} // namespace WebCore
