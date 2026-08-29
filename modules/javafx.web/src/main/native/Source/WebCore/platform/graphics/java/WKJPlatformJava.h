/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

/*
 * WKJPlatformJava.h - the C++ glue the graphics and network Java-port directories share.
 *
 * Everything here is a thin, header-only wrapper over webkit_java_api_platform.h. It exists
 * because those two directories make 90 upcalls between them and every one of them needs the
 * same three things: the installed callback table (or nullptr if wkj_init has not run), a
 * WTF::String built from a (pointer, length) pair, and a WTF::String handed out as one.
 *
 * WHERE THIS BELONGS. Conceptually it is the successor to the string half of
 * wtf/java/StringJava.cpp and to platform/java/PlatformJavaClasses.h - it should end up in
 * WTF, beside WKJHandle.h, once wtf/text/WTFString.h loses its two JNI string conversions
 * (FFM-ABI-CONTRACT.md section 10 lists both as Phase-B work). It sits
 * here for now because platform/graphics/java and platform/network/java are the only users
 * and both are on the WebCore include path (WebCore/PlatformJava.cmake), so
 * `#include "WKJPlatformJava.h"` resolves from either. Moving it is a rename and nothing else.
 *
 * BEHAVIOUR. wkjMakeString and WKJStringArg reproduce StringJava.cpp exactly, including the
 * two cases that are easy to get wrong:
 *
 *   in   the JNI String constructor mapped BOTH a null Java string and a
 *        zero-length one to StringImpl::empty(). A Java null has therefore always
 *        reached WebCore as the empty String, never as the null String, and wkjMakeString
 *        keeps that (contract 11.1).
 *   out  the JNI toJavaString() returned a null Java string for a null String and widened an
 *        8-bit (Latin-1) String rather than calling span16() on it - span16() asserts
 *        !is8Bit(), and in a release build that assert is gone, leaving a length()-byte heap
 *        overread. WKJStringArg branches the same way and, like NewString, always copies, so
 *        the pointer it hands out never aliases the String it was built from.
 */

#pragma once

#include <stdint.h>

#include <webkit_java_api.h>

#include <wtf/Noncopyable.h>
#include <wtf/Vector.h>
#include <wtf/text/WTFString.h>

namespace WebCore {

/*
 * The installed callback tables, or nullptr before wkj_init has run. Every caller tests both
 * the table and the individual slot, because contract 4 lets Java leave any slot NULL.
 */
inline const WKJHostGraphics* wkjGraphics()
{
    return wkj_host ? &wkj_host->graphics : nullptr;
}

inline const WKJHostNetwork* wkjNetwork()
{
    return wkj_host ? &wkj_host->network : nullptr;
}

inline const WKJHostMedia* wkjMedia()
{
    return wkj_host ? &wkj_host->media : nullptr;
}

/*
 * The replacement for WTF::CheckAndClearException: true when the last upcall on this
 * thread ended in a Throwable. Java has already caught and logged it, so this only reports
 * and clears - which is what the JNI code did at every one of its call sites, a dozen of
 * which branched on the result and the rest of which discarded it. Keep that split exactly:
 * do not start propagating.
 */
inline bool wkjCheckAndClearException()
{
    if (!wkj_host || !wkj_host->core.check_and_clear_exception)
        return false;
    return wkj_host->core.check_and_clear_exception() != 0;
}

/* A UTF-16 (pointer, length) argument turned into a WTF::String; see BEHAVIOUR above. */
inline WTF::String wkjMakeString(const uint16_t* s, int32_t length)
{
    if (!s || length <= 0)
        return WTF::emptyString();
    return WTF::String(std::span<const char16_t>(reinterpret_cast<const char16_t*>(s),
                                              static_cast<size_t>(length)));
}

/*
 * A WTF::String presented to a callback slot as (pointer, length). Hold it in a named local
 * for the duration of the call:
 *
 *     WKJStringArg family(familyName);
 *     cb->get_font(family.data(), family.length(), bold, italic, size);
 *
 * data() is nullptr only for a null String, which is how the ABI spells Java null; an empty
 * String gives a non-null pointer and length 0.
 */
class WKJStringArg {
    WTF_MAKE_NONCOPYABLE(WKJStringArg);
public:
    explicit WKJStringArg(const WTF::String& value)
    {
        if (value.isNull())
            return;

        m_length = static_cast<int32_t>(value.length());
        if (!m_length) {
            m_data = &s_empty;
            return;
        }

        m_buffer.grow(static_cast<size_t>(m_length));
        for (int32_t i = 0; i < m_length; ++i)
            m_buffer[static_cast<size_t>(i)] = static_cast<uint16_t>(value.characterAt(static_cast<unsigned>(i)));
        m_data = m_buffer.span().data();
    }

    const uint16_t* data() const { return m_data; }
    int32_t length() const { return m_length; }

private:
    static inline const uint16_t s_empty = 0;

    WTF::Vector<uint16_t, 64> m_buffer;
    const uint16_t* m_data { nullptr };
    int32_t m_length { 0 };
};

/*
 * Runs the contract-13 string protocol over a caller-provided buffer and returns the result.
 * `fetch` is anything callable as int32_t(uint16_t* buf, int32_t cap, int32_t* length) - i.e.
 * the tail of a WKJ_STR_-returning slot with its earlier arguments already bound:
 *
 *     String ext = wkjFetchString([&](uint16_t* b, int32_t c, int32_t* n) {
 *         return cb->image_decoder_get_filename_extension(decoder, b, c, n);
 *     });
 *
 * WKJ_STR_NULL comes back as the null String, so the caller decides whether null collapses to
 * empty - which is what the JNI code did per site rather than uniformly. On WKJ_STR_OVERFLOW
 * the buffer is grown once to the reported size and the call repeated.
 */
template<typename Fetch>
inline WTF::String wkjFetchString(const Fetch& fetch)
{
    constexpr int32_t initialCapacity = 256;

    WTF::Vector<uint16_t, initialCapacity> buffer(static_cast<size_t>(initialCapacity));
    int32_t length = 0;
    int32_t status = fetch(buffer.span().data(), initialCapacity, &length);

    if (status == WKJ_STR_OVERFLOW && length > 0) {
        buffer.grow(static_cast<size_t>(length));
        status = fetch(buffer.span().data(), length, &length);
    }

    if (status != WKJ_STR_OK)
        return { };
    if (!length)
        return WTF::emptyString();
    return WTF::String(std::span<const char16_t>(reinterpret_cast<const char16_t*>(buffer.span().data()),
                                              static_cast<size_t>(length)));
}

} // namespace WebCore
