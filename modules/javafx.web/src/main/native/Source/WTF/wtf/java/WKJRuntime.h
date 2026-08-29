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
 * WKJRuntime.h - the successor to wtf/java/JavaEnv.h.
 *
 * JavaEnv.h existed to hand every layer of the port a JNIEnv*: it held the JavaVM, it
 * attached and detached threads, it turned a pending JNI exception into a bool, and it
 * carried the shutdown flag that decided whether an upcall was allowed to happen at all.
 * With the wkj_* ABI there is no environment and no attach, so what is left is much smaller:
 * three things WTF needs in order to talk to Java, and nothing that names the JVM.
 *
 *   1. The installed host table (webkit_java_api.h) reached through named accessors, so no
 *      call site tests `wkj_host` itself.
 *   2. The shutdown gate. This is the one piece of JavaEnv.h that has to be reproduced
 *      deliberately rather than deleted - see THE SHUTDOWN GATE below.
 *   3. The UTF-16 string bridge, which is what wtf/java/StringJava.cpp was.
 *
 * Object references are NOT here: they are WKJHandle, in wtf/java/WKJHandle.h.
 *
 * ------------------------------------------------------------------------------------------
 * THE SHUTDOWN GATE - read this before deleting anything below
 * ------------------------------------------------------------------------------------------
 * WTF::AttachThreadToJavaEnv (JavaEnv.h:83-114) checked g_ShuttingDown FIRST and, when it was
 * set, left m_env null instead of attaching. WC_GETJAVAENV_CHKRET then turned that null
 * environment into an early return at ten call sites across nine files. The flag is set from
 * Java, by MainThread.twkSetShutdown, at the point where the Java side starts tearing down.
 *
 * So the null environment was not only an error path: it was a functioning gate that stopped
 * timers, socket callbacks, frame-loader notifications and popup menus from calling into a
 * Java side that was going away. FFM has no environment and therefore nothing that can be
 * null, so an upcall made where one of those ten early returns used to fire WILL now happen.
 * That is a behaviour change, and the only defence is an explicit test.
 *
 * wkjIsShuttingDown() and WKJ_RETURN_IF_SHUTTING_DOWN are that test, shaped like the macro
 * they replace so that each site is a substitution rather than a deletion:
 *
 *     -    WC_GETJAVAENV_CHKRET(env, false);
 *     +    WKJ_RETURN_IF_SHUTTING_DOWN(false);
 *
 * A site that returns void passes no argument, exactly as before.
 *
 * ------------------------------------------------------------------------------------------
 * STRINGS
 * ------------------------------------------------------------------------------------------
 * wkjMakeString and WKJStringArg reproduce StringJava.cpp exactly, including the two cases
 * that are easy to get wrong:
 *
 *   in   String(JNIEnv*, const JLString&) mapped BOTH a null jstring and a zero-length one to
 *        StringImpl::empty(). A Java null has therefore always reached WebCore as the EMPTY
 *        String, never as the null String, and wkjMakeString keeps that collapse
 *        (FFM-ABI-CONTRACT.md section 11.1). Changing it would change what
 *        element.setAttribute("x", null) does.
 *   out  toJavaString() returned a null jstring for a null String, and it WIDENED an 8-bit
 *        (Latin-1) String one code unit at a time rather than calling span16() on it.
 *        That branch is load-bearing: StringImpl::span16() asserts !is8Bit(), and in a
 *        release build the assert is gone, leaving a length()-byte heap overread.
 *        WKJStringArg branches the same way and always copies, like NewString, so the
 *        pointer it hands out never aliases the String it was built from.
 *
 * WebCore/platform/graphics/java/WKJPlatformJava.h carries a WebCore-namespace copy of these
 * three helpers, put there because WebCore needed them before WTF had a home for them; its
 * own comment says it belongs here. Collapsing it onto this header is a rename with no
 * behaviour change, and is left to the slice that owns that directory.
 */

#pragma once

#include <stdint.h>

#include <webkit_java_api.h>

#include <wtf/Noncopyable.h>
#include <wtf/Vector.h>
#include <wtf/text/WTFString.h>

/*
 * Set by wkj_set_shutdown, i.e. by com.sun.webkit.MainThread.twkSetShutdown. Kept as a plain
 * global with the name it has always had, so that the sites reading it - here and
 * WebCore/platform/ThreadTimers.cpp - change their include and nothing else. Read
 * WTF::wkjIsShuttingDown() rather than this directly in new code.
 */
extern volatile bool g_ShuttingDown;

namespace WTF {

/*
 * The installed callback tables, or nullptr before wkj_init has run. Every caller tests both
 * the table and the individual slot, because contract section 4 lets Java leave any slot NULL.
 */
inline const WKJHostCore* wkjCore()
{
    return wkj_host ? &wkj_host->core : nullptr;
}

inline const WKJHostWTF* wkjWTF()
{
    return wkj_host ? &wkj_host->wtf : nullptr;
}

inline const WKJHostFileSystem* wkjFileSystem()
{
    return wkj_host ? &wkj_host->filesystem : nullptr;
}

/*
 * True once Java has told the library it is shutting down. See THE SHUTDOWN GATE above.
 */
inline bool wkjIsShuttingDown()
{
    return g_ShuttingDown;
}

/*
 * The replacement for WTF::CheckAndClearException(env): true when the last upcall on this
 * thread ended in a Throwable.
 *
 * The meaning is NARROWER than the JNI function it replaces, and that is deliberate.
 * CheckAndClearException reported any pending JNI exception whatever raised it - including
 * a failed FindClass, a failed GetMethodID and an OutOfMemoryError from NewByteArray - and
 * called ExceptionDescribe() before clearing. Under this ABI there is no class lookup and no
 * member-id lookup to fail, and Java catches and logs inside the upcall stub because contract
 * section 4 forbids letting a Throwable escape one, so C++ never sees a pending exception.
 * What is left to report is exactly one thing: the last upcall threw. That is what the JNI
 * code was really asking at the dozen sites that branched on the answer.
 *
 * The sites that only called it to clear should keep only calling it: that swallowing is the
 * existing behaviour and this migration does not change it.
 */
inline bool wkjCheckAndClearException()
{
    const WKJHostCore* core = wkjCore();
    if (!core || !core->check_and_clear_exception)
        return false;
    return core->check_and_clear_exception() != 0;
}

/* A UTF-16 (pointer, length) argument turned into a WTF::String; see STRINGS above. */
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
 *     WKJStringArg jpath(path);
 *     cb->file_exists(jpath.data(), jpath.length());
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
 * `fetch` is anything callable as int32_t(uint16_t* buf, int32_t cap, int32_t* length) - the
 * tail of a WKJ_STR_-returning slot with its earlier arguments already bound:
 *
 *     String name = wkjFetchString([&](uint16_t* b, int32_t c, int32_t* n) {
 *         return cb->path_get_file_name(jpath.data(), jpath.length(), b, c, n);
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
    int32_t status = fetch(buffer.mutableSpan().data(), initialCapacity, &length);

    if (status == WKJ_STR_OVERFLOW && length > 0) {
        buffer.grow(static_cast<size_t>(length));
        status = fetch(buffer.mutableSpan().data(), length, &length);
    }

    if (status != WKJ_STR_OK)
        return { };
    if (!length)
        return WTF::emptyString();
    return WTF::String(std::span<const char16_t>(reinterpret_cast<const char16_t*>(buffer.span().data()),
                                                 static_cast<size_t>(length)));
}

} // namespace WTF

/*
 * The shutdown gate, shaped like the WC_GETJAVAENV_CHKRET it replaces so that converting a
 * call site is a substitution. Takes the value to return, or nothing in a void function.
 */
#define WKJ_RETURN_IF_SHUTTING_DOWN(... /* ret val */) \
    if (WTF::wkjIsShuttingDown()) return __VA_ARGS__;
