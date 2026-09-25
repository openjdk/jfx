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
 * In the JNI build that commit 939aa61ead replaced, Java set the g_ShuttingDown flag, through
 * MainThread.twkSetShutdown, when its side started tearing down, and exactly two places read
 * it:
 *
 *   - WTF::AttachThreadToJavaEnv (JavaEnv.h), which then left m_env null on every thread,
 *     attached or not. Only one caller branched on that environment,
 *     scheduleDispatchFunctionsOnMainThread in MainThreadJava.cpp, so that call stopped on
 *     every thread.
 *   - ThreadTimers.cpp, which stopped installing the shared timer.
 *
 * Nothing else looked at the flag. WC_GETJAVAENV_CHKRET, the three hand-written JNIEnv
 * tests (`if (!env)` and return in ImageDecoderJava.cpp and RenderingQueue.cpp, `if (env)`
 * around the deref call in RQRef.cpp), and the copy and clear guards of JavaRef.h around
 * NewGlobalRef and DeleteGlobalRef all asked jvm->GetEnv, which answers only whether the
 * CALLING THREAD is attached. They skipped their work on a thread with no JNIEnv and never
 * on an attached one. The FX thread, which is WebKit's main thread, is always attached, so
 * on it every one of them kept working until the JVM halted. A Web Worker thread after
 * WorkerThread::createGlobalScope is not attached, so there they skipped their work from
 * the start. The FFM build does that work there, except that the ImageDecoderJava
 * constructor keeps the JNI result with a thread test of its own (FFM-ABI-CONTRACT.md
 * section 13.3).
 *
 * FFM has no environment and no attach, so "is this thread attached" cannot be asked, and an
 * upcall stub attaches a thread by itself. The port therefore gates on the flag, and more
 * widely than JNI did. Once wkj_set_shutdown has set it, on EVERY thread, the FX thread
 * included:
 *
 *   - the retain, retain_weak, release and is_live slots of the published wkj_host
 *     (WKJRuntime.cpp) return 0 or do nothing, so a WKJHandle copy holds 0, a weak id reports
 *     its object gone, and nothing is released;
 *   - every WKJ_RETURN_IF_SHUTTING_DOWN site returns early. Among them are the ten former
 *     WC_GETJAVAENV_CHKRET sites and the three hand-written tests above. Most are destructors
 *     and dispose paths; two are MainThreadSharedTimer::setFireInterval and stop, so the
 *     shared timer is neither re-armed nor stopped;
 *   - MainThreadJava.cpp and ThreadTimers.cpp test the flag directly, as JNI did.
 *
 * The first two points are stricter than JNI on every attached thread. That deviation is
 * recorded in FFM-ABI-CONTRACT.md section 13.3; it lasts from the shutdown hooks WebPage
 * installs to the end of the process.
 *
 * wkjIsShuttingDown() and WKJ_RETURN_IF_SHUTTING_DOWN are the flag test, the macro shaped
 * like the one it replaces so that each site is a substitution rather than a deletion:
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
 *        WKJStringArg keeps that 8-bit branch (wkjCopyToUTF16 below) and always copies,
 *        like NewString, so the pointer it hands out never aliases the String it was built
 *        from; the 16-bit case is a single memcpy rather than a per-unit loop.
 *
 * WebCore/platform/graphics/java/WKJPlatformJava.h carries a WebCore-namespace copy of these
 * three helpers, put there because WebCore needed them before WTF had a home for them; its
 * own comment says it belongs here. Collapsing it onto this header is a rename with no
 * behaviour change, and is left to the slice that owns that directory.
 */

#pragma once

#include <cstring>
#include <stdint.h>

#include <webkit_java_api.h>

#include <wtf/Noncopyable.h>
#include <wtf/Vector.h>
#include <wtf/text/WTFString.h>

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
 * True once Java has told the library it is shutting down. The atomic storage stays private to
 * WKJRuntime.cpp so this widely included header does not expose its synchronization mechanism.
 * See THE SHUTDOWN GATE above.
 */
bool wkjIsShuttingDown();

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
 * Copies the first `count` code units of `value` into `destination` as UTF-16, with no NUL
 * terminator. `count` must not exceed value.length(), and `destination` must have room.
 *
 * The Latin-1 branch is not an optimisation, it is required: StringImpl::span16() asserts
 * !is8Bit(), and in a release build the assert is compiled out, so calling it on an 8-bit
 * string reads length() bytes past the end of the heap allocation. Most DOM strings are
 * Latin-1. The 16-bit branch is one memcpy; only the 8-bit branch widens per unit, because
 * it has to.
 */
inline void wkjCopyToUTF16(const WTF::String& value, uint16_t* destination, unsigned count)
{
    if (!count)
        return;

    if (value.is8Bit()) {
        auto characters = value.span8();
        for (unsigned i = 0; i < count; ++i)
            destination[i] = characters[i];
    } else {
        auto characters = value.span16();
        std::memcpy(destination, characters.data(), count * sizeof(char16_t));
    }
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
        wkjCopyToUTF16(value, m_buffer.mutableSpan().data(), static_cast<unsigned>(m_length));
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
 * the buffer is grown once to the reported size and the call repeated. The repeat runs the
 * Java slot again, so `fetch` must be safe to call twice (FFM-ABI-CONTRACT.md section 13).
 */
template<typename Fetch>
inline WTF::String wkjFetchString(const Fetch& fetch)
{
    constexpr int32_t initialCapacity = 256;

    WTF::Vector<uint16_t, initialCapacity> buffer(static_cast<size_t>(initialCapacity));
    int32_t length = 0;
    int32_t status = fetch(buffer.mutableSpan().data(), initialCapacity, &length);

    /*
     * Retry only on a coherent overflow: a reported length greater than the capacity just
     * offered. A non-positive length, or one that fits the buffer the library claims was too
     * small, is a protocol violation - fall through, and the status test below turns it into
     * the null String. The bound also keeps Vector::grow() from shrinking, which asserts.
     */
    if (status == WKJ_STR_OVERFLOW && length > initialCapacity) {
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
 * The do/while wrapper keeps the hidden `if` from binding a following `else` at a call
 * site, which the bare statement form silently did.
 */
#define WKJ_RETURN_IF_SHUTTING_DOWN(... /* ret val */) \
    do { \
        if (WTF::wkjIsShuttingDown()) \
            return __VA_ARGS__; \
    } while (0)
