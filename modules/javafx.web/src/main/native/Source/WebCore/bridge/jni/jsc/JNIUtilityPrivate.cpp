/*
 * Copyright (C) 2003, 2010 Apple, Inc.  All rights reserved.
 * Copyright 2009, The Android Open Source Project
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

#include "config.h"
#include <wkj_constants.h>
#include "JNIUtilityPrivate.h"

#if ENABLE(JAVA_BRIDGE)

#include "runtime_array.h"
#include "runtime_object.h"
#include "runtime_root.h"
#include <JavaScriptCore/JSArray.h>
#include <JavaScriptCore/JSLock.h>

#include "JavaArrayJSC.h"
#include "JavaInstanceJSC.h"
#include "JavaRuntimeObject.h"

#include "JSNode.h"
#include "Node.h"

namespace JSC {

namespace Bindings {

static char16_t toJCharValue(const JSValue& value, JSGlobalObject* globalObject)
{
    // If JS type is string and target Java type is char, then
    // return the first unicode character.
    if (value.isString()) {
        String stringValue = value.toString(globalObject)->value(globalObject);
        return stringValue[0];
    }
    return static_cast<char16_t>(value.toNumber(globalObject));
}

WKJHandle convertUndefinedToJObject()
{
    /*
     * The JSObject.UNDEFINED singleton. The lookup that used to be here - the class lookup,
     * then the static field id, then the field read, cached in a function-local global
     * reference - is now one host slot, and the caching lives with it in javaUndefinedObject().
     */
    return javaUndefinedObject();
}

WKJJavaValue convertValueToJValue(JSGlobalObject* globalObject, RootObject* rootObject, JSValue value, JavaType javaType, const char* javaClassName)
{
    JSLockHolder lock(globalObject);

    WKJJavaValue result = emptyJavaValue();

    switch (javaType) {
    case JavaTypeArray:
    case JavaTypeObject:
        {
            result.type = static_cast<int32_t>(javaType);

            // FIXME: JavaJSObject::convertValueToJObject functionality is almost exactly the same,
            // these functions should use common code.

            if (value.isObject()) {
                JSObject* object = asObject(value);
                if (object->inherits(JavaRuntimeObject::info())) {
                    // Unwrap a Java instance.
                    JavaRuntimeObject* runtimeObject = static_cast<JavaRuntimeObject*>(object);
                    JavaInstance* instance = runtimeObject->getInternalJavaInstance();
                    if (instance) {
                        // Since instance->javaInstance() is a weak reference, taking a strong one to safeguard javaInstance() from GC
                        WKJHandle jlinstance = WKJHandle::retained(instance->javaInstance());
                        if (!jlinstance) {
                            LOG_ERROR("Could not get javaInstance for %llu in JNIUtilityPrivate::convertValueToJValue",
                                static_cast<unsigned long long>(instance->javaInstance()));
                            return result;
                        }
                        // The strong reference taken above is what the caller receives.
                        result.l = jlinstance.leakRef();
                    }
                } else if (object->classInfo() == RuntimeArray::info()) {
                    // Input is a JavaScript Array that was originally created from a Java Array
                    RuntimeArray* imp = static_cast<RuntimeArray*>(object);
                    JavaArray* array = static_cast<JavaArray*>(imp->getConcreteArray());

                    // Since array->javaArray() is a weak reference, taking a strong one to safeguard javaInstance() from GC
                    WKJHandle jlinstancearray = WKJHandle::retained(array->javaArray());
                    if (!jlinstancearray) {
                        LOG_ERROR("Could not get javaArrayInstance for %llu in JNIUtilityPrivate::convertValueToJValue",
                            static_cast<unsigned long long>(array->javaArray()));
                        return result;
                    }
                    result.l = jlinstancearray.leakRef();
                } else if ((!result.l && (!strcmp(javaClassName, "java.lang.Object")))
                           || (!strcmp(javaClassName, "netscape.javascript.JSObject"))) {
                    // Wrap objects in JSObject instances.
                    if (object->inherits(WebCore::JSNode::info())) {
                        WebCore::JSNode* jsnode = static_cast<WebCore::JSNode*>(object);
                        WebCore::Node *peer = &jsnode->wrapped();
                        peer->ref(); //deref is in NodeImpl disposer
                        result.l = javaNodeCachedImpl(wkj_from_ptr(peer)).leakRef();
                    } else {
                        rootObject->gcProtect(object);
                        result.l = javaJSObjectCreate(wkj_from_ptr(object),
                            com_sun_webkit_dom_JSObject_JS_CONTEXT_OBJECT).leakRef();
                    }
                }
            }

            // Create an appropriate Java object if target type is java.lang.Object or other wrapper Objects {Integer, Double, Boolean}.
            if (!result.l) {
                if (value.isString() && !strcmp(javaClassName, "java.lang.Object")) {
                    String stringValue = asString(value)->value(globalObject);
                    result.l = javaBoxString(stringValue).leakRef();
                } else if (value.isString() && !strcmp(javaClassName, "java.lang.Character")) {
                    WKJJavaValue charValue = emptyJavaValue();
                    charValue.type = WKJ_JT_CHAR;
                    charValue.i = static_cast<int32_t>(toJCharValue(value, globalObject));
                    result.l = javaBox(charValue).leakRef();
                } else if (value.isNumber()) {
                    if (value.isInt32() && (!strcmp(javaClassName, "java.lang.Number") || !strcmp(javaClassName, "java.lang.Integer") || !strcmp(javaClassName, "java.lang.Object"))) {
                        WKJJavaValue intValue = emptyJavaValue();
                        intValue.type = WKJ_JT_INT;
                        intValue.i = value.asInt32();
                        result.l = javaBox(intValue).leakRef();
                    } else if (!strcmp(javaClassName, "java.lang.Number") || !strcmp(javaClassName, "java.lang.Double") || !strcmp(javaClassName, "java.lang.Object")) {
                        WKJJavaValue doubleValue = emptyJavaValue();
                        doubleValue.type = WKJ_JT_DOUBLE;
                        doubleValue.d = value.asNumber();
                        result.l = javaBox(doubleValue).leakRef();
                    }
                } else if (value.isBoolean() && (!strcmp(javaClassName, "java.lang.Boolean") || !strcmp(javaClassName, "java.lang.Object"))) {
                    WKJJavaValue boolValue = emptyJavaValue();
                    boolValue.type = WKJ_JT_BOOLEAN;
                    boolValue.i = value.asBoolean() ? 1 : 0;
                    result.l = javaBox(boolValue).leakRef();
                } else if (value.isUndefined()) {
                    result.l = convertUndefinedToJObject().leakRef();
                }
            }

            // Convert value to a string if the target type is a java.lang.String, and we're not
            // converting from a null.
            if (!result.l && !strcmp(javaClassName, "java.lang.String")) {
                if (!value.isNull()) {
                    String stringValue = value.toString(globalObject)->value(globalObject);
                    result.l = javaBoxString(stringValue).leakRef();
                }
            }
        }
        break;

    case JavaTypeBoolean:
        {
            result.type = WKJ_JT_BOOLEAN;
            /*
             * PRESERVED DEFECT. This was `result.z = (jboolean) value.toNumber(globalObject)`
             * and jboolean is unsigned char, so a JS number of 256 arrives in Java as false
             * and 257 as true. It is the only place in the tree where a value other than 0
             * or 1 could reach a jboolean, and widening it to int32_t here would silently
             * change what an application sees.
             *
             * The cast to uint8_t is therefore deliberate and is the same conversion the JNI
             * cast performed. The Java side reads a non-zero value as true, which is what the
             * JVM did with the jboolean this replaces. Fixing the truncation is a behaviour
             * change and belongs in its own commit with its own JavaScriptBridgeTest case;
             * see FFM-ABI-CONTRACT.md section 13.1, finding 7.
             */
            result.i = static_cast<int32_t>(static_cast<uint8_t>(value.toNumber(globalObject)));
        }
        break;

    case JavaTypeByte:
        {
            result.type = WKJ_JT_BYTE;
            result.i = static_cast<int8_t>(value.toNumber(globalObject));
        }
        break;

    case JavaTypeChar:
        {
            result.type = WKJ_JT_CHAR;
            result.i = static_cast<int32_t>(toJCharValue(value, globalObject));
        }
        break;

    case JavaTypeShort:
        {
            result.type = WKJ_JT_SHORT;
            result.i = static_cast<int16_t>(value.toNumber(globalObject));
        }
        break;

    case JavaTypeInt:
        {
            result.type = WKJ_JT_INT;
            result.i = static_cast<int32_t>(value.toNumber(globalObject));
        }
        break;

    case JavaTypeLong:
        {
            result.type = WKJ_JT_LONG;
            result.j = static_cast<int64_t>(value.toNumber(globalObject));
        }
        break;

    case JavaTypeFloat:
        {
            result.type = WKJ_JT_FLOAT;
            /* Narrowed to float exactly as the (jfloat) cast did, then carried as its exact
               double widening; the Java side narrows it back. */
            result.d = static_cast<double>(static_cast<float>(value.toNumber(globalObject)));
        }
        break;

    case JavaTypeDouble:
        {
            result.type = WKJ_JT_DOUBLE;
            result.d = value.toNumber(globalObject);
        }
        break;

    case JavaTypeInvalid:
    case JavaTypeVoid:
        break;
    }
    return result;
}

WKJHandle javaValueToObject(const WKJJavaValue& value, JavaType jtype)
{
    switch (jtype) {
    case JavaTypeObject:
    case JavaTypeArray:
        /* Already an object; the caller gets its own reference to it. */
        return WKJHandle::retained(value.l);
    case JavaTypeBoolean:
    case JavaTypeChar:
    case JavaTypeByte:
    case JavaTypeShort:
    case JavaTypeInt:
    case JavaTypeLong:
    case JavaTypeFloat:
    case JavaTypeDouble:
        /* Boolean.valueOf, Character.valueOf, ... - fourteen upcalls, now one host slot. */
        return javaBox(value);
    default:
        abort();
    }
}

WKJHandle dispatchJavaCall(int count, RootObject*, wkj_ref instance, JavaType returnType,
    wkj_ref method, const wkj_ref* args, WKJJavaValue& result, wkj_ref accessControlContext)
{
    // Since instance is a weak reference, taking a strong one to safeguard it from GC
    WKJHandle jlinstance = WKJHandle::retained(instance);

    if (!jlinstance) {
        LOG_ERROR("Could not get javaInstance for %llu in JNIUtilityPrivate::dispatchJavaCall",
            static_cast<unsigned long long>(instance));
        return WKJHandle();
    }

    /*
     * com.sun.webkit.Utilities.fwkInvokeWithContext(method, instance, args, acc) - the same
     * static method the JNI code called, and still the place the actual Method.invoke
     * happens. What has gone is the machinery around it: GetObjectClass, ToReflectedMethod,
     * FindClass twice, NewObjectArray and SetObjectArrayElement per argument. Java builds the
     * Object[] now.
     */
    WKJHandle exception;
    WKJHandle invocationResult = javaInvoke(method, instance, args, count, accessControlContext,
        exception);
    wkj_ref r = invocationResult.get();

    switch (returnType) {
    case JavaTypeVoid:
        {
        }
        break;
    case JavaTypeArray:
    case JavaTypeObject:
    // Since we can't convert java.lang.Character to any JS primitive, we have
    // to treat it as JS foreign object.
    case JavaTypeChar:
        /* The tag says only that an object is held, which is all a reader of it needs to
           know; the caller switches on the return type it asked for, as it always did. */
        result.type = WKJ_JT_OBJECT;
        result.l = invocationResult.leakRef();
        break;

    case JavaTypeBoolean:
    case JavaTypeByte:
    case JavaTypeShort:
    case JavaTypeInt:
    case JavaTypeLong:
    case JavaTypeFloat:
    case JavaTypeDouble:
        /* booleanValue(), byteValue(), ... on the boxed result, as the JNI code did. */
        javaUnbox(r, returnType, result);
        break;

    case JavaTypeInvalid:
        /* Nothing to do */
        break;
    }
    return exception;
}

} // end of namespace Bindings

} // end of namespace JSC

#endif // ENABLE(JAVA_BRIDGE)
