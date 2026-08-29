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

/* utility functions for the generated DOM bindings - the JNI-free half of JavaDOMUtils */

#include "config.h"

#include "DOMException.h"
#include "WKJDOMUtils.h"

#include <cstring>
#include <span>
#include <wtf/Assertions.h>
#include <wtf/text/WTFString.h>

/*
 * The calling thread's pending-exception slot (FFM-ABI-CONTRACT.md sections 2.2 and 13).
 *
 * Defined here because the DOM bindings are today the only code that writes one; move it
 * beside wkj_init when the rest of the C ABI lands. The slot is plain per-thread memory
 * and it is self-contained - the message lives inline in it - so there is nothing to free
 * and no lifetime rule. The library writes it and clears it on entry to every wkj_*
 * function; Java reads it after a fallible call and clears it by storing WKJ_EXC_NONE into
 * `type`. Nothing may touch another thread's slot.
 */
WKJ_EXPORT WKJExceptionSlot* wkj_exception_slot(void)
{
    static thread_local WKJExceptionSlot slot { };
    return &slot;
}

namespace WebCore {

/*
 * Everything in this anonymous namespace carries a WKJ or wkj prefix on purpose: WebCore
 * builds these sources unified, so several .cpp files end up sharing one translation unit,
 * and therefore one anonymous namespace.
 */
namespace {

/*
 * Copies the first `count` code units of `value` into `destination` as UTF-16, with no NUL
 * terminator. `count` must not exceed value.length(), and `destination` must have room.
 *
 * The Latin-1 branch is not an optimisation, it is required: StringImpl::span16() asserts
 * !is8Bit(), and in a release build the assert is compiled out, so calling it on an 8-bit
 * string reads length() bytes past the end of the heap allocation. Most DOM strings are
 * Latin-1. Widening one code unit at a time is what String::toJavaString() did before
 * handing the characters to NewString (StringJava.cpp:62-73).
 */
void wkjCopyToUTF16(const WTF::String& value, uint16_t* destination, unsigned count)
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

} // namespace

void WKJSetPendingException(int32_t type, int32_t code, const WTF::String& message)
{
    WKJExceptionSlot* slot = wkj_exception_slot();

    /*
     * The message lives inline in the slot, so nothing here has to stay alive afterwards.
     * Anything past WKJ_EXC_MESSAGE_MAX code units is dropped and message_length reports
     * what is actually present. No message raised today comes anywhere near the limit:
     * they are all short canned literals from DOMException::description.
     */
    unsigned length = message.isNull() ? 0u : message.length();
    if (length > static_cast<unsigned>(WKJ_EXC_MESSAGE_MAX))
        length = WKJ_EXC_MESSAGE_MAX;

    wkjCopyToUTF16(message, slot->message, length);
    slot->message_length = static_cast<int32_t>(length);

    slot->code = code;
    /* `type` last: it is what Java tests before reading the rest of the slot. */
    slot->type = type;
}

WTF::String WKJString(const uint16_t* s, int32_t length)
{
    /*
     * String::String(env, const JLString&) mapped a null jstring to StringImpl::empty()
     * and a zero-length jstring to the same. Both therefore produce an empty, non-null
     * String here. See the comment on the declaration: this is deliberate, and it is not
     * the same as the null-versus-empty rule that applies to strings going the other way.
     */
    if (!s || length <= 0)
        return emptyString();

    std::span<const char16_t> characters(reinterpret_cast<const char16_t*>(s),
        static_cast<size_t>(length));
    return WTF::String(characters);
}

int32_t WKJReturnString(uint16_t* resultBuf, int32_t resultCap, int32_t* resultLength,
    const WTF::String& value)
{
    if (resultLength)
        *resultLength = 0;

    /* JavaReturn<String>::operator jstring() tested for a pending exception first. */
    if (WKJHasPendingException())
        return WKJ_STR_NULL;

    /* String::toJavaString() returned a null jstring for a null String. */
    if (value.isNull())
        return WKJ_STR_NULL;

    const unsigned length = value.length();
    if (resultLength)
        *resultLength = static_cast<int32_t>(length);

    /* Nothing is written on overflow; the caller grows to *resultLength and calls again. */
    if (!resultBuf || resultCap < 0 || length > static_cast<unsigned>(resultCap))
        return WKJ_STR_OVERFLOW;

    wkjCopyToUTF16(value, resultBuf, length);
    return WKJ_STR_OK;
}

/*
 * The JNI version built org.w3c.dom.DOMException(short legacyCode, String message) and
 * handed it to env->Throw(). The slot carries the same two values, and Java builds the same
 * exception from them: WKJ_EXC_DOM is org.w3c.dom.DOMException, `code` is the legacy code
 * of the DOMException::Description, and the message is description.message, or the literal
 * "Unknown Exception" when the description has no name. Every raise in this file is a DOM
 * exception; the other WKJ_EXC_* kinds exist because JavaExceptionType named them, and no
 * code in the tree ever raised one.
 */
static void raiseDOMErrorException(ExceptionCode ec)
{
#if ASSERT_ENABLED
    if (ec != ExceptionCode::TypeError) {
        WTFLogAlways("Unexpected ExceptionCode: %d", static_cast<int>(ec));
    }
#endif
    auto description = DOMException::description(ec);

    const char* message;
    if (description.name) {
        message = description.message;
    } else {
        message = "Unknown Exception";
    }

    WKJSetPendingException(WKJ_EXC_DOM, static_cast<int32_t>(description.legacyCode),
        String::fromLatin1(message));
}

void raiseTypeErrorException()
{
    raiseDOMErrorException(ExceptionCode::TypeError);
}

void raiseNotSupportedErrorException()
{
    raiseDOMErrorException(ExceptionCode::NotSupportedError);
}

void raiseDOMErrorException(Exception&& ec)
{
    raiseDOMErrorException(ec.code());
}

} // namespace WebCore
