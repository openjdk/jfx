/*
 * Copyright (C) 2003, 2004, 2005, 2008, 2009, 2010 Apple Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE COMPUTER, INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE COMPUTER, INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * The Java side of the LiveConnect bridge, with no JNI in it.
 *
 * Everything this file used to provide - getJNIEnv, getJavaVM, getMethodID, the
 * callJNIMethod / callJNIMethodV / callJNIStaticMethod templates and their ten JNICaller
 * specialisations, getJNIField, and the modified-UTF-8 string accessors - existed to invoke
 * a Java method whose name and signature were only known at run time. The Foreign Function
 * and Memory API has no expression for that: there is no method id and no reflective invoke.
 *
 * So the operations, rather than the mechanism, cross the boundary. Each function below is
 * one slot of WKJLiveConnectHost (Source/WebKitLegacy/java/api/webkit_java_api_bridge.h),
 * which Java installs once with wkj_live_connect_init, and each is the direct replacement
 * for one former callJNIMethod call site. What is gone entirely, with nothing replacing it:
 *
 *   getJavaVM / setJavaVM       the last thing in this tree that reached JNI_GetCreatedJavaVMs
 *                               through dlsym on the JVM framework, which is what made libjvm a
 *                               link dependency. Nothing here needs a virtual machine pointer.
 *   getJNIEnv                   an upcall runs on whatever thread called it; there is no
 *                               environment to fetch and nothing to attach.
 *   getJNIField                 a JNI field read by name and signature. It had no caller.
 *   callJNIStaticMethod         likewise: declared, never called.
 *   getCharactersFromJString    modified UTF-8. The one live consumer was Class.getName, which
 *                               now arrives as UTF-16 (webkit_java_api_bridge.h, note 2).
 *
 * Ownership: every function here that returns a WKJHandle returns one the caller owns. Every
 * function that takes a wkj_ref borrows it for the duration of the call. A WTF::String result
 * is null - not empty - where the Java value was null, so that the "<Unknown>" substitutions
 * the JNI code made on a failed lookup still happen in the same places.
 */

#pragma once

#if ENABLE(JAVA_BRIDGE)

#include "JavaType.h"

#include <webkit_java_api_bridge.h>
#include <wtf/Noncopyable.h>
#include <wtf/java/WKJHandle.h>
#include <wtf/text/WTFString.h>

namespace JSC {

namespace Bindings {

JavaType javaTypeFromClassName(const char* name);
JavaType javaTypeFromPrimitiveType(char type);
const char* signatureFromJavaType(JavaType);

/*
 * A WKJJavaValue with no value in it: the replacement for
 * memset(&result, 0, sizeof(jvalue)), which every one of these functions did first.
 */
WKJJavaValue emptyJavaValue();

/*
 * Releases the object a WKJJavaValue holds, if it holds one, when the scope ends. The JNI
 * code relied on the local reference frame that JavaInstance::virtualBegin pushed; ids are
 * not reclaimed by anything, so each one needs a named owner. Declare one of these beside
 * every WKJJavaValue that a call filled in.
 */
class JavaValueScope {
    WTF_MAKE_NONCOPYABLE(JavaValueScope);
public:
    explicit JavaValueScope(WKJJavaValue& value)
        : m_value(value)
    {
    }

    ~JavaValueScope()
    {
        if (m_value.type == WKJ_JT_OBJECT || m_value.type == WKJ_JT_ARRAY)
            WKJRelease(m_value.l);
        m_value.l = 0;
    }

    /* Hands the object to the caller; the scope no longer releases it. */
    [[nodiscard]] wkj_ref leakObject()
    {
        wkj_ref ref = m_value.l;
        m_value.l = 0;
        return ref;
    }

private:
    WKJJavaValue& m_value;
};

/* --- java.lang.Object and java.lang.Class ------------------------------------------- */

WKJHandle javaObjectClass(wkj_ref object);
WTF::String javaClassName(wkj_ref javaClass);
bool javaClassIsArray(wkj_ref javaClass);
WKJHandle javaCreateDummyObject();

/* --- java.lang.reflect.Method -------------------------------------------------------- */

/*
 * The Method that GetMethodID + ToReflectedMethod produced, or a null handle. `signature` is
 * a JNI descriptor; see the resolve_method slot for why it is still built and passed.
 */
WKJHandle javaResolveMethod(wkj_ref object, const WTF::String& name, const WTF::String& signature);

/*
 * com.sun.webkit.Utilities.fwkInvokeWithContext. The result is the returned object, and
 * `exception` receives the Throwable the invocation ended with, if any - which is exactly
 * what dispatchJNICall returned through ExceptionOccurred + ExceptionClear.
 */
WKJHandle javaInvoke(wkj_ref method, wkj_ref instance, const wkj_ref* args, int argumentCount,
    wkj_ref accessControlContext, WKJHandle& exception);

WTF::String javaMethodName(wkj_ref method);
WTF::String javaMethodReturnTypeName(wkj_ref method);
int javaMethodParameterCount(wkj_ref method);
WTF::String javaMethodParameterTypeName(wkj_ref method, int index);
int javaMethodModifiers(wkj_ref method);

/* --- java.lang.reflect.Field --------------------------------------------------------- */

WTF::String javaFieldName(wkj_ref field);
WTF::String javaFieldTypeName(wkj_ref field);
bool javaFieldGet(wkj_ref field, wkj_ref instance, JavaType, WKJJavaValue& result);
bool javaFieldSet(wkj_ref field, wkj_ref instance, JavaType, const WKJJavaValue&);

/* --- Java arrays --------------------------------------------------------------------- */

int javaArrayLength(wkj_ref array);
bool javaArrayGet(wkj_ref array, int index, JavaType, WKJJavaValue& result);
bool javaArraySet(wkj_ref array, int index, JavaType, const WKJJavaValue&);

/* --- boxing, unboxing and strings ---------------------------------------------------- */

WKJHandle javaBox(const WKJJavaValue&);
bool javaUnbox(wkj_ref boxed, JavaType, WKJJavaValue& result);
WKJHandle javaBoxString(const WTF::String&);
WTF::String javaStringValue(wkj_ref string);

/* --- the three LiveConnect objects --------------------------------------------------- */

/*
 * The com.sun.webkit.dom.JSObject.UNDEFINED singleton. One id is kept for the life of the
 * process, as one global reference was, and every caller gets its own reference to it, so
 * that the ownership rule is the same here as everywhere else.
 */
WKJHandle javaUndefinedObject();

WKJHandle javaJSObjectCreate(int64_t peer, int32_t peerType);
WKJHandle javaNodeCachedImpl(int64_t nodePeer);

} // namespace Bindings

} // namespace JSC

#endif // ENABLE(JAVA_BRIDGE)
