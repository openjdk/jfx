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
 * WKJPageSupport.h - the two helpers the WebKitLegacy core C ABI needs and the DOM
 * bindings do not.
 *
 * WKJDOMUtils.h covers strings crossing the boundary in a *downcall*: WKJString for an
 * argument coming in and WKJReturnString for a result going out into the caller's buffer.
 * This slice also has 60 upcalls, where a WTF::String has to be handed to Java as a
 * (pointer, length) pair that stays valid for the duration of the call. That is what
 * WKJStringArg does; there is nothing in WKJDOMUtils.h for it because no DOM binding makes
 * an upcall.
 *
 * The header is inline only, so it needs no entry in Source/WebKitLegacy/PlatformJava.cmake.
 */

#pragma once

#include <cstring>
#include <webkit_java_api.h>
#include <webkit_java_api_page.h>
#include <wtf/Noncopyable.h>
#include <wtf/Vector.h>
#include <wtf/text/WTFString.h>

namespace WebCore {

/*
 * A WTF::String materialised as UTF-16 for as long as this object lives, which is what a
 * callback argument needs: the library owns the memory and Java copies it before the
 * callback returns.
 *
 * The null handling is String::toJavaString()'s, which is what every one of these call
 * sites used:
 *
 *   a null String  -> data() == nullptr, length() == 0. The ABI spells that "Java null",
 *                     which is what the null jstring toJavaString returned meant.
 *   an empty String-> data() != nullptr, length() == 0, i.e. the empty string.
 *
 * The 8-bit branch widens one code unit at a time rather than calling span16():
 * StringImpl::span16() asserts !is8Bit(), and with the assert compiled out it would read
 * length() bytes past the end. StringJava.cpp:62-73 branched the same way.
 */
class WKJStringArg final {
    WTF_MAKE_NONCOPYABLE(WKJStringArg);
public:
    explicit WKJStringArg(const String& value)
    {
        if (value.isNull())
            return;

        const unsigned length = value.length();
        m_buffer.resize(static_cast<size_t>(length) + 1);
        if (value.is8Bit()) {
            auto characters = value.span8();
            for (unsigned i = 0; i < length; ++i)
                m_buffer[i] = characters[i];
        } else if (length) {
            auto characters = value.span16();
            std::memcpy(m_buffer.data(), characters.data(), length * sizeof(char16_t));
        }
        m_buffer[length] = 0;
        m_length = static_cast<int32_t>(length);
        m_isNull = false;
    }

    /* nullptr for a null String; otherwise length() code units, NUL terminated. */
    const uint16_t* data() const
    {
        return m_isNull ? nullptr : reinterpret_cast<const uint16_t*>(m_buffer.data());
    }

    int32_t length() const { return m_length; }

private:
    Vector<char16_t, 64> m_buffer;
    int32_t m_length { 0 };
    bool m_isNull { true };
};

} // namespace WebCore
