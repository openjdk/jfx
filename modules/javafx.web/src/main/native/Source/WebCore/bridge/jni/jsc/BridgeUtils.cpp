/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"

#include "BridgeUtils.h"

#include <JavaScriptCore/CallFrame.h>
#include <JavaScriptCore/Identifier.h>

#include "Document.h"
#include "Frame.h"
#include "JavaInstanceJSC.h"
#include "JavaArrayJSC.h"
#include "JavaRuntimeObject.h"
#include "JNIUtilityPrivate.h"
#include "JSDOMBinding.h"
#include "JSDOMGlobalObject.h"
#include "JSExecState.h"
#include "JSNode.h"
#include "ScriptController.h"
#include "runtime_array.h"
#include "runtime_object.h"
#include "runtime_root.h"
#include <wtf/java/JavaRef.h>
#include <wtf/text/WTFString.h>
#include <JavaScriptCore/JSArray.h>
#include <JavaScriptCore/JSLock.h>
#include <JavaScriptCore/APICast.h>
#include <JavaScriptCore/OpaqueJSString.h>
#include <JavaScriptCore/JSBase.h>
#include <JavaScriptCore/JSStringRef.h>

#include "com_sun_webkit_dom_JSObject.h"

#if 1
#define FIND_CACHE_CLASS(ENV, SIG)      \
  static JGClass cls((ENV)->FindClass(SIG)); return cls
#else
#define FIND_CACHE_CLASS(ENV, SIG)      \
  static jclass cls = (jclass) (ENV)->NewGlobalRef((ENV)->FindClass(SIG)); \
  return cls
#endif

#if 0
static jclass getNodeImplClass (JNIEnv *env)
{
    FIND_CACHE_CLASS(env, "com/sun/webkit/dom/NodeImpl");
}

static jclass getDoubleClass (JNIEnv *env)
{
    FIND_CACHE_CLASS(env, "java/lang/Double");
}

static jclass getIntegerClass (JNIEnv *env)
{
    FIND_CACHE_CLASS(env, "java/lang/Integer");
}
#endif

static jclass getJSObjectClass (JNIEnv *env)
{
    FIND_CACHE_CLASS(env, "com/sun/webkit/dom/JSObject");
}

static jclass getJSExceptionClass (JNIEnv *env)
{
    FIND_CACHE_CLASS(env, "netscape/javascript/JSException");
}

static jclass getNumberClass (JNIEnv *env)
{
    FIND_CACHE_CLASS(env, "java/lang/Number");
}

static jclass getBooleanClass (JNIEnv *env)
{
    FIND_CACHE_CLASS(env, "java/lang/Boolean");
}

static jclass getStringClass (JNIEnv *env)
{
    FIND_CACHE_CLASS(env, "java/lang/String");
}

static jclass getNullPointerExceptionClass (JNIEnv *env)
{
    FIND_CACHE_CLASS(env, "java/lang/NullPointerException");
}

static void throwNullPointerException (JNIEnv *env)
{
    jclass clJSException = getNullPointerExceptionClass(env);
    env->Throw((jthrowable) env->NewObject(clJSException,
            env->GetMethodID(clJSException, "<init>", "()V")));
}

namespace WebCore {

JSGlobalContextRef getGlobalContext(WebCore::ScriptController* scriptController)
{
    return toGlobalRef(scriptController->globalObject(WebCore::mainThreadNormalWorld()));
}

JSStringRef asJSStringRef(JNIEnv *env, jstring str)
{
    unsigned int slen = env->GetStringLength(str);
    const jchar* schars = env->GetStringCritical(str, nullptr);
    JSStringRef name = JSStringCreateWithCharacters((const JSChar*) schars, slen);
    env->ReleaseStringCritical(str, schars);
    return name;
}

JSValueRef Java_Object_to_JSValue(
    JNIEnv *env,
    JSContextRef ctx,
    JSC::Bindings::RootObject* rootObject,
    jobject val,
    jobject accessControlContext)
{
    if (val == nullptr)
        return JSValueMakeNull(ctx);
    JSC::JSGlobalObject* lexicalGlobalObject = toJS(ctx);
    JSC::JSLockHolder lock(lexicalGlobalObject);

    jclass clJSObject = getJSObjectClass(env);
    if (env->IsInstanceOf(val, clJSObject)) {
        static jfieldID fldPeer = env->GetFieldID(clJSObject, "peer", "J");
        static jfieldID fldPeerType = env->GetFieldID(clJSObject, "peer_type", "I");
        jlong peer = env->GetLongField(val, fldPeer);
        jint peer_type = env->GetIntField(val, fldPeerType);
        switch (peer_type) {
        case com_sun_webkit_dom_JSObject_JS_CONTEXT_OBJECT:
            return static_cast<JSObjectRef>(jlong_to_ptr(peer));
        case com_sun_webkit_dom_JSObject_JS_DOM_NODE_OBJECT:
        case com_sun_webkit_dom_JSObject_JS_DOM_WINDOW_OBJECT:
            {
                JSDOMGlobalObject* globalObject = toJSDOMGlobalObject(
                    ((peer_type == com_sun_webkit_dom_JSObject_JS_DOM_WINDOW_OBJECT)
                        ? *static_cast<DOMWindow*>(jlong_to_ptr(peer))->document()
                        : static_cast<Node*>(jlong_to_ptr(peer))->document()),
                    normalWorld(lexicalGlobalObject->vm()));
                return toRef(lexicalGlobalObject,
                    (peer_type == com_sun_webkit_dom_JSObject_JS_DOM_WINDOW_OBJECT)
                        ? WebCore::toJS(lexicalGlobalObject, globalObject, static_cast<DOMWindow*>(jlong_to_ptr(peer)))
                        : WebCore::toJS(lexicalGlobalObject, globalObject, static_cast<Node*>(jlong_to_ptr(peer))));
            }
        }
    }
    jclass clString = getStringClass(env);
    if (env->IsInstanceOf(val, clString)) {
        JSStringRef value = asJSStringRef(env, (jstring) val);
        JSValueRef jsvalue = JSValueMakeString(ctx, value);
        JSStringRelease(value);
        return jsvalue;
    }
    jclass clBoolean = getBooleanClass(env);
    if (env->IsInstanceOf(val, clBoolean)) {
        static jmethodID booleanValueMethod = env->GetMethodID(clBoolean, "booleanValue", "()Z");
        jboolean value = env->CallBooleanMethod(val, booleanValueMethod);
        return JSValueMakeBoolean(ctx, value);
    }
    jclass clNumber = getNumberClass(env);
    if (env->IsInstanceOf(val, clNumber)) {
        static jmethodID doubleValueMethod = env->GetMethodID(clNumber, "doubleValue", "()D");
        jdouble value = env->CallDoubleMethod(val, doubleValueMethod);
        return JSValueMakeNumber(ctx, value);
    }

    JLObject valClass(JSC::Bindings::callJNIMethod<jobject>(val, "getClass", "()Ljava/lang/Class;"));
    if (JSC::Bindings::callJNIMethod<jboolean>(valClass, "isArray", "()Z")) {
        JLString className((jstring)JSC::Bindings::callJNIMethod<jobject>(valClass, "getName", "()Ljava/lang/String;"));
        const char* classNameC = JSC::Bindings::getCharactersFromJString(className);
        JSC::JSValue arr = JSC::Bindings::JavaArray::convertJObjectToArray(lexicalGlobalObject, val, classNameC, rootObject, accessControlContext);
        JSC::Bindings::releaseCharactersForJString(className, classNameC);
        return toRef(lexicalGlobalObject, arr);
    }
    else {
        // All other Java Object types including java.lang.Character will be wrapped inside JavaInstance.
        RefPtr<JSC::Bindings::JavaInstance> jinstance = JSC::Bindings::JavaInstance::create(val, rootObject, accessControlContext);
        return toRef(jinstance->createRuntimeObject(lexicalGlobalObject));
    }
}

jstring JSValue_to_Java_String(JSValueRef value, JNIEnv* env, JSContextRef ctx)
{
    JSStringRef str = JSValueToStringCopy(ctx, value, nullptr);
    size_t slen = JSStringGetLength(str);
    const JSChar* schars = JSStringGetCharactersPtr(str);
    jstring result = env->NewString((const jchar*) schars, slen);
    JSStringRelease(str);
    return result;
}

jobject JSValue_to_Java_Object(
    JSValueRef value,
    JNIEnv*,
    JSContextRef ctx,
    JSC::Bindings::RootObject* rootObject)
{
    JSC::JSGlobalObject* globalObject = toJS(ctx);
    return convertValueToJValue(globalObject, rootObject, toJS(globalObject, value),
        JSC::Bindings::JavaTypeObject, "java.lang.Object").l;
}

static void throwJavaException(
    JNIEnv* env,
    JSContextRef ctx,
    JSValueRef exception,
    JSC::Bindings::RootObject* rootObject)
{
    jclass clJSObject = getJSObjectClass(env);
    jobject jex = JSValue_to_Java_Object(exception, env, ctx, rootObject);
    static jmethodID makeID =
        env->GetStaticMethodID(clJSObject, "fwkMakeException",
                        "(Ljava/lang/Object;)Lnetscape/javascript/JSException;");

    env->Throw(JLocalRef<jthrowable>((jthrowable)env->CallStaticObjectMethod(
            clJSObject,
            makeID,
            jex)));
}

jobject executeScript(
    JNIEnv* env,
    JSObjectRef object,
    JSContextRef ctx,
    JSC::Bindings::RootObject *rootObject,
    jstring str)
{
    if (str == nullptr) {
        throwNullPointerException(env);
        return nullptr;
    }
    JSStringRef script = asJSStringRef(env, str);
    JSValueRef exception = 0;
    JSValueRef value = JSEvaluateScript(ctx, script, object, nullptr, 1, &exception);
    JSStringRelease(script);
    if (exception) {
        throwJavaException(env, ctx, exception, rootObject);
        return nullptr;
    }
    return WebCore::JSValue_to_Java_Object(value, env, ctx, rootObject);
}

}


RefPtr<JSC::Bindings::RootObject> checkJSPeer(
    jlong peer,
    jint peer_type,
    JSObjectRef &object,
    JSContextRef &context)
{
    JSC::Bindings::RootObject *rootObject = nullptr;
    switch (peer_type) {
    case com_sun_webkit_dom_JSObject_JS_CONTEXT_OBJECT:
        {
            object = static_cast<JSObjectRef>(jlong_to_ptr(peer));
            rootObject = JSC::Bindings::findProtectingRootObject(reinterpret_cast<JSC::JSObject*>(object));
            if (rootObject) {
                context = toRef(rootObject->globalObject());
            }
        }
        break;
    case com_sun_webkit_dom_JSObject_JS_DOM_NODE_OBJECT:
    case com_sun_webkit_dom_JSObject_JS_DOM_WINDOW_OBJECT:
        {
            WebCore::Frame* frame = (peer_type == com_sun_webkit_dom_JSObject_JS_DOM_WINDOW_OBJECT)
                ? static_cast<WebCore::DOMWindow*>(jlong_to_ptr(peer))->document()->frame()
                : static_cast<WebCore::Node*>(jlong_to_ptr(peer))->document().frame();

            if (!frame) {
                return rootObject;
            }
            rootObject = &(frame->script().createRootObject(frame).leakRef());
            if (rootObject) {
                context = WebCore::getGlobalContext(&frame->script());
                JSC::JSGlobalObject* JSGlobalObject = toJS(context);
                JSC::JSLockHolder lock(JSGlobalObject);

                object = const_cast<JSObjectRef>(toRef(JSGlobalObject,
                    (peer_type == com_sun_webkit_dom_JSObject_JS_DOM_WINDOW_OBJECT)
                    ? WebCore::toJS(JSGlobalObject, static_cast<WebCore::JSDOMGlobalObject *>(rootObject->globalObject()), static_cast<WebCore::DOMWindow*>(jlong_to_ptr(peer)))
                    : WebCore::toJS(JSGlobalObject, static_cast<WebCore::JSDOMGlobalObject *>(rootObject->globalObject()), static_cast<WebCore::Node*>(jlong_to_ptr(peer)))));

            }
        }
        break;
    };

    return rootObject;
}



extern "C" {

JNIEXPORT jobject JNICALL Java_com_sun_webkit_dom_JSObject_evalImpl
(JNIEnv *env, jclass, jlong peer, jint peer_type, jstring str)
{
    if (str == nullptr) {
        throwNullPointerException(env);
        return nullptr;
    }
    JSObjectRef object;
    JSContextRef ctx;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));
    if (rootObject.get() == nullptr) {
        throwNullPointerException(env);
        return nullptr;
    }

    return WebCore::executeScript(env, object, ctx, rootObject.get(), str);
}

JNIEXPORT jobject JNICALL Java_com_sun_webkit_dom_JSObject_getMemberImpl
(JNIEnv *env, jclass, jlong peer, jint peer_type, jstring str)
{
    if (str == nullptr) {
        throwNullPointerException(env);
        return nullptr;
    }
    JSObjectRef object;
    JSContextRef ctx;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));

    if (rootObject.get() == nullptr) {
        throwNullPointerException(env);
        return nullptr;
    }
    JSStringRef name = WebCore::asJSStringRef(env, str);
    JSValueRef value = JSObjectGetProperty(ctx, object, name, nullptr);
    JSStringRelease(name);
    return WebCore::JSValue_to_Java_Object(value, env, ctx, rootObject.get());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_JSObject_setMemberImpl
(JNIEnv *env, jclass, jlong peer, jint peer_type, jstring str, jobject value, jobject accessControlContext)
{
    if (str == nullptr) {
        throwNullPointerException(env);
        return;
    }
    JSObjectRef object;
    JSContextRef ctx;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));
    if (rootObject.get() == nullptr) {
        throwNullPointerException(env);
        return;
    }
    JSStringRef name = WebCore::asJSStringRef(env, str);
    JSValueRef jsvalue = WebCore::Java_Object_to_JSValue(env, ctx, rootObject.get(), value, accessControlContext);
    JSPropertyAttributes attributes = 0;
    JSValueRef exception = 0;
    JSObjectSetProperty(ctx, object, name, jsvalue, attributes, &exception);
    JSStringRelease(name);
    if (exception)
        WebCore::throwJavaException(env, ctx, exception, rootObject.get());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_JSObject_removeMemberImpl
(JNIEnv *env, jclass, jlong peer, jint peer_type, jstring str)
{
    if (str == nullptr) {
        throwNullPointerException(env);
        return;
    }
    JSObjectRef object;
    JSContextRef ctx;
    if (!checkJSPeer(peer, peer_type, object, ctx)) {
        throwNullPointerException(env);
        return;
    }

    JSStringRef name = WebCore::asJSStringRef(env, str);
    JSObjectDeleteProperty(ctx, object, name, nullptr);
    JSStringRelease(name);
}

JNIEXPORT jobject JNICALL Java_com_sun_webkit_dom_JSObject_getSlotImpl
  (JNIEnv *env, jclass, jlong peer, jint peer_type, jint index)
{
    JSObjectRef object;
    JSContextRef ctx;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));
    if (rootObject.get() == nullptr) {
        throwNullPointerException(env);
        return nullptr;
    }

    JSValueRef value = JSObjectGetPropertyAtIndex(ctx, object, index, nullptr);
    return WebCore::JSValue_to_Java_Object(value, env, ctx, rootObject.get());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_JSObject_setSlotImpl
(JNIEnv *env, jclass, jlong peer, jint peer_type, jint index, jobject value, jobject accessControlContext)
{
    JSObjectRef object;
    JSContextRef ctx;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));
    if (rootObject.get() == nullptr) {
        throwNullPointerException(env);
        return;
    }

    JSValueRef jsvalue = WebCore::Java_Object_to_JSValue(env, ctx, rootObject.get(), value, accessControlContext);
    JSObjectSetPropertyAtIndex(ctx, object, (unsigned) index, jsvalue, nullptr);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_JSObject_toStringImpl
(JNIEnv *env, jclass, jlong peer, jint peer_type)
{
    JSObjectRef object;
    JSContextRef ctx;
    if (!checkJSPeer(peer, peer_type, object, ctx)) {
        return nullptr;
    }

    JSC::JSGlobalObject* JSGlobalObject = toJS(ctx);
    JSC::JSLockHolder lock(JSGlobalObject);

    return toJS(object)->toString(JSGlobalObject)->value(JSGlobalObject)
        .toJavaString(env).releaseLocal();
}

JNIEXPORT jobject JNICALL Java_com_sun_webkit_dom_JSObject_callImpl
  (JNIEnv *env, jclass, jlong peer, jint peer_type, jstring methodName, jobjectArray args, jobject accessControlContext)
{
    if (methodName == nullptr || args == nullptr) {
        throwNullPointerException(env);
        return nullptr;
    }
    JSObjectRef object;
    JSContextRef ctx;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));
    if (!rootObject || !rootObject.get() || !ctx) {
        env->ThrowNew(getJSExceptionClass(env), "Invalid function reference");
        return nullptr;
    }

    JSStringRef name = WebCore::asJSStringRef(env, methodName);
    JSValueRef member = JSObjectGetProperty(ctx, object, name, nullptr);
    JSStringRelease(name);
    if (!JSValueIsObject(ctx, member))
        return JSC::Bindings::convertUndefinedToJObject();
    JSObjectRef function = JSValueToObject(ctx, member, nullptr);
    if (! JSObjectIsFunction(ctx, function))
        return JSC::Bindings::convertUndefinedToJObject();
    size_t argumentCount = env->GetArrayLength(args);
    JSValueRef *arguments = new JSValueRef[argumentCount];
    for (size_t i = 0;  i < argumentCount; i++) {
      JLObject jarg(env->GetObjectArrayElement(args, i));
        arguments[i] = WebCore::Java_Object_to_JSValue(env, ctx, rootObject.get(), jarg, accessControlContext);
    }
    JSValueRef exception = 0;
    JSValueRef result = JSObjectCallAsFunction(ctx, function, object,
                                               argumentCount,  arguments,
                                               &exception);
    delete[] arguments;
    if (exception) {
        WebCore::throwJavaException(env, ctx, exception, rootObject.get());
        return nullptr;
    }
    return WebCore::JSValue_to_Java_Object(result, env, ctx, rootObject.get());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_JSObject_unprotectImpl
(JNIEnv*, jclass, jlong peer, jint peer_type)
{
    JSObjectRef object;
    JSContextRef ctx;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));
    if (!rootObject || !rootObject.get() || !peer || !ctx) {
        return;
    }

    rootObject->gcUnprotect(toJS(object));
}

}
