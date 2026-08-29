/*
 * Copyright (C) 2022 Apple Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#if ENABLE(JAVA_BRIDGE)

#include "JNIUtility.h"
#include <JavaScriptCore/JSCJSValue.h>

namespace JSC {

class JSObject;

namespace Bindings {

class RootObject;

/*
 * The JavaScript value as a Java value of the requested type. Every object it produces is
 * owned by the caller; declare a JavaValueScope beside the result, which is what replaces
 * the JNI local reference frame that used to reclaim these.
 */
WKJJavaValue convertValueToJValue(JSGlobalObject*, RootObject*, JSValue, JavaType,
    const char* javaClassName);

/* The com.sun.webkit.dom.JSObject.UNDEFINED singleton, as a reference the caller owns. */
WKJHandle convertUndefinedToJObject();

/*
 * Invokes a Java method through com.sun.webkit.Utilities.fwkInvokeWithContext and converts
 * its result to `result` according to `returnType`. Returns the Throwable the invocation
 * ended with, or a null handle.
 *
 * This was dispatchJNICall, which took a JNI method id and turned it into a
 * java.lang.reflect.Method with ToReflectedMethod before handing it to Java. The Method now
 * arrives as a wkj_ref, so the conversion - and with it the isStatic argument that only
 * ToReflectedMethod needed - is gone. Nothing is lost with it: JavaClass always passed
 * false, and JavaInstance::invokeMethod throws a TypeError for a static method before it
 * ever reaches here.
 */
WKJHandle dispatchJavaCall(int argumentCount, RootObject*, wkj_ref instance, JavaType returnType,
    wkj_ref method, const wkj_ref* args, WKJJavaValue& result, wkj_ref accessControlContext);

/* The value as a Java object, boxing it if it is a primitive. The caller owns the result. */
WKJHandle javaValueToObject(const WKJJavaValue&, JavaType);

} // namespace Bindings

} // namespace JSC

#endif // ENABLE(JAVA_BRIDGE)
