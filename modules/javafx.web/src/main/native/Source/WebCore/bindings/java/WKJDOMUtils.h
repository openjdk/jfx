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
 * WKJDOMUtils.h - the helpers the generated DOM bindings use, with no JNI in them.
 *
 * This is the replacement for JavaDOMUtils.h. The six constructs the JNI DOM bindings were
 * built from map one for one (FFM-ABI-CONTRACT.md section 9, string returns as amended by
 * section 13):
 *
 *   String(env, x)              ->  WKJString(x, x_length)
 *   JavaReturn<String>(env, x)  ->  WKJReturnString(result_buf, result_cap, result_length, x)
 *   JavaReturn<T>(env, x)       ->  WKJReturnPeer<T>(x)
 *   raiseOnDOMError(env, x)     ->  raiseOnDOMError(x)
 *   raiseTypeErrorException(env)->  raiseTypeErrorException()
 *   jlong_to_ptr(peer)          ->  wkj_to_ptr(peer)        (webkit_java_api.h)
 *
 * The two behaviours that carried information in the JNI version, and that are therefore
 * reproduced exactly here rather than tidied up:
 *
 *   - JavaReturn<T>::operator jlong() and JavaReturn<String>::operator jstring() first
 *     asked env->ExceptionCheck() and returned 0 / NULL when an exception was pending.
 *     WKJReturnPeer and WKJReturnString ask the thread exception slot in the same place,
 *     so the control flow of a throwing getter is unchanged. WKJReturnPeer keeps the
 *     leakRef(); the matching deref() is still in the Java dispose path.
 *
 *   - String::toJavaString() returned a null jstring for a null WTF::String and an empty
 *     jstring for an empty one. WKJReturnString returns WKJ_STR_NULL and WKJ_STR_OK with a
 *     length of 0 respectively, which is how the ABI spells the same distinction.
 *
 * Read the comment on WKJString before changing it: its null handling matches the JNI
 * String(env, const JLString&) constructor, which is not what one would guess.
 *
 * There is no library-owned string memory here and no lifetime rule to honour: the caller
 * provides the buffer (contract 13). The earlier per-thread arena was withdrawn because its
 * invariant could not survive reentrancy - a Java upcall can make further downcalls while
 * an outer C frame still holds a returned pointer - and because the exception check that
 * guarded a returned string was itself a wkj_* call, so it invalidated the value it guarded.
 */

#pragma once

#include <stdint.h>

#include <webkit_java_api.h>

#include <wtf/GetPtr.h>
#include <wtf/Ref.h>
#include <wtf/RefPtr.h>
#include <wtf/text/WTFString.h>
#include "ExceptionOr.h"

// Note that a pointer to a Node is not necessarily the same address
// as a pointer to an Element: a static_cast between the two is not
// necessarily a no-op.  (Though with most C++ implementations it will be.)
// Therefore, if p is an int64_t (or a void*) then:
//   static_cast<Element*>(static_cast<Node*>(p))
// is not necessarily the same as:
//   static_cast<Element*>(p)
#define wkj_to_Nodeptr(p) static_cast<Node*>(wkj_to_ptr(p))

namespace WebCore {

/*
 * True when this thread has an exception waiting for Java to throw. This is the
 * replacement for env->ExceptionCheck(); it reads the thread's slot, which is plain
 * library memory, and makes no call into Java.
 */
inline bool WKJHasPendingException()
{
    return wkj_exception_slot()->type != WKJ_EXC_NONE;
}

/*
 * Discards any exception pending on this thread.
 *
 * Every exported wkj_* function calls this on entry (webkit_java_api.h, rule 4). 48 of the
 * 124 throwing DOM functions return void, so a missed check on the Java side would leave
 * the slot dirty and the next unrelated call on that thread would throw an exception
 * belonging to the previous one; clearing on entry bounds a missed check to the call that
 * caused it. WKJCallScope below is the one-liner the generated bodies use.
 */
inline void WKJClearPendingException()
{
    wkj_exception_slot()->type = WKJ_EXC_NONE;
}

/*
 * Declare one of these at the top of every exported wkj_* function, beside the existing
 * WebCore::JSMainThreadNullState, to satisfy the clear-on-entry rule.
 */
class WKJCallScope {
public:
    WKJCallScope() { WKJClearPendingException(); }
};

/*
 * Records an exception for Java to throw when the current call returns. `type` is one of
 * the WKJ_EXC_* constants and `code` is the DOM exception code. The message is copied into
 * the thread's slot, which holds it inline, so there is nothing to keep alive afterwards;
 * a message longer than WKJ_EXC_MESSAGE_MAX code units is truncated.
 *
 * This helper is not in the section 9 list; it exists because the exception slot has to be
 * filled from somewhere, and having one writer keeps the rules in one place.
 */
void WKJSetPendingException(int32_t type, int32_t code, const WTF::String& message);

/*
 * A UTF-16 string argument, as (pointer, length), turned into a WTF::String.
 *
 * This reproduces String::String(env, const JLString&) from wtf/java/StringJava.cpp
 * case by case, including the case that is easy to get wrong:
 *
 *   s == nullptr      -> the EMPTY string, not the null String. The JNI constructor did
 *                        `if (!s) m_impl = StringImpl::empty();`, so a Java null argument
 *                        has always arrived in WebCore as an empty String, never a null
 *                        one. Returning a null String here instead would change what
 *                        WebCore sees for every DOM setter called with null, which is a
 *                        behaviour change, not a cleanup.
 *   length <= 0       -> the empty string (the JNI constructor tested GetStringLength).
 *   otherwise         -> a copy of the length UTF-16 code units at s.
 *
 * The pointer is not retained; the data is copied.
 */
WTF::String WKJString(const uint16_t* s, int32_t length);

/*
 * A WTF::String returned to Java as UTF-16, copied into the caller's buffer (contract 13).
 * The library owns no string memory and returns no pointer, so there is no lifetime rule.
 *
 *   an exception is pending      -> *resultLength = 0, WKJ_STR_NULL (what JavaReturn did)
 *   a null String                -> *resultLength = 0, WKJ_STR_NULL (what toJavaString did)
 *   value.length() > resultCap   -> *resultLength = value.length(), WKJ_STR_OVERFLOW, and
 *                                   nothing is written; the facade grows once and retries
 *   otherwise                    -> value.length() code units written, WKJ_STR_OK
 *
 * An empty non-null String is WKJ_STR_OK with a length of 0, which is how null stays
 * distinguishable from empty on the way out. The buffer is not NUL terminated.
 */
int32_t WKJReturnString(uint16_t* resultBuf, int32_t resultCap, int32_t* resultLength,
    const WTF::String& value);

/*
 * A ref-counted WebCore object returned to Java as a peer.
 *
 *   an exception is pending -> 0, and the reference this function took is dropped
 *   otherwise               -> the pointer, with one reference leaked to Java
 *
 * The leaked reference is dropped by the deref() in the Java dispose path, exactly as it
 * was for JavaReturn<T>. Do not change that pairing here.
 */
template<typename T> int64_t WKJReturnPeer(T*);
template<typename T> int64_t WKJReturnPeer(RefPtr<T>);

void raiseTypeErrorException();
void raiseNotSupportedErrorException();

void raiseDOMErrorException(Exception&&);

template<typename T> T raiseOnDOMError(ExceptionOr<T>&&);
template<typename T> T* raiseOnDOMError(ExceptionOr<Ref<T>>&&);
template<typename T> T* raiseOnDOMError(ExceptionOr<RefPtr<T>>&&);
String raiseOnDOMError(ExceptionOr<String>&&);
void raiseOnDOMError(ExceptionOr<void>&&);

inline void raiseOnDOMError(ExceptionOr<void>&& possibleException)
{
    if (possibleException.hasException())
        raiseDOMErrorException(possibleException.releaseException());
}

inline String raiseOnDOMError(ExceptionOr<String>&& exceptionOrReturnValue)
{
    if (exceptionOrReturnValue.hasException()) {
        raiseDOMErrorException(exceptionOrReturnValue.releaseException());
        return emptyString();
    }
    return exceptionOrReturnValue.releaseReturnValue();
}

template<typename T> inline T* raiseOnDOMError(ExceptionOr<Ref<T>>&& exceptionOrReturnValue)
{
    if (exceptionOrReturnValue.hasException()) {
        raiseDOMErrorException(exceptionOrReturnValue.releaseException());
        return nullptr;
    }
    return WTF::getPtr(exceptionOrReturnValue.returnValue());
}

template<typename T> inline T* raiseOnDOMError(ExceptionOr<RefPtr<T>>&& exceptionOrReturnValue)
{
    if (exceptionOrReturnValue.hasException()) {
        raiseDOMErrorException(exceptionOrReturnValue.releaseException());
        return nullptr;
    }
    return WTF::getPtr(exceptionOrReturnValue.returnValue());
}

template<typename T> inline T raiseOnDOMError(ExceptionOr<T>&& exceptionOrReturnValue)
{
    if (exceptionOrReturnValue.hasException()) {
        raiseDOMErrorException(exceptionOrReturnValue.releaseException());
        return static_cast<T>(NULL);
    }
    return exceptionOrReturnValue.releaseReturnValue();
}

template<typename T> inline int64_t WKJReturnPeer(T* returnValue)
{
    RefPtr<T> value { returnValue };
    // There was an exception; the reference taken above is dropped when value goes away.
    if (WKJHasPendingException())
        return 0;
    // The paired deref() call is in the dispose Java method.
    return wkj_from_ptr(WTF::move(value).leakRef());
}

template<typename T> inline int64_t WKJReturnPeer(RefPtr<T> returnValue)
{
    // There was an exception; the reference held by returnValue is dropped on return.
    if (WKJHasPendingException())
        return 0;
    // The paired deref() call is in the dispose Java method.
    return wkj_from_ptr(WTF::move(returnValue).leakRef());
}

} // namespace WebCore
