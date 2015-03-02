/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "BridgeUtils.h"
#include "CallFrame.h"
#include "Identifier.h"
#include "Frame.h"
#include "JavaInstanceJSC.h"
#include "JavaArrayJSC.h"
#include "JavaRef.h"
#include "JavaRuntimeObject.h"
#include "JNIUtilityPrivate.h"
#include "JSDOMBinding.h"
#include "JSDOMGlobalObject.h"
#include "JSMainThreadExecState.h"
#include "JSNode.h"
#include "ScriptController.h"
#include "runtime_array.h"
#include "runtime_object.h"
#include "runtime_root.h"
#include <runtime/JSArray.h>
#include <runtime/JSLock.h>
#include <wtf/text/WTFString.h>
#include <APICast.h>
#include <API/OpaqueJSString.h>
#include <API/JSBase.h>
#include <API/JSStringRef.h>
#include "com_sun_webkit_dom_JSObject.h"

#if 1
#define FIND_CACHE_CLASS(ENV, SIG)      \
  static JGClass cls((ENV)->FindClass(SIG)); return cls
#else
#define FIND_CACHE_CLASS(ENV, SIG)      \
  static jclass cls = (jclass) (ENV)->NewGlobalRef((ENV)->FindClass(SIG)); \
  return cls
#endif

static jclass getJSObjectClass (JNIEnv *env)
{
    FIND_CACHE_CLASS(env, "com/sun/webkit/dom/JSObject");
}

static jclass getJSExceptionClass (JNIEnv *env)
{
    FIND_CACHE_CLASS(env, "netscape/javascript/JSException");
}

static jclass getNodeImplClass (JNIEnv *env)
{
    FIND_CACHE_CLASS(env, "com/sun/webkit/dom/NodeImpl");
}

static jclass getNumberClass (JNIEnv *env)
{
    FIND_CACHE_CLASS(env, "java/lang/Number");
}

static jclass getDoubleClass (JNIEnv *env)
{
    FIND_CACHE_CLASS(env, "java/lang/Double");
}

static jclass getIntegerClass (JNIEnv *env)
{
    FIND_CACHE_CLASS(env, "java/lang/Integer");
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
    return toGlobalRef(scriptController->globalObject(WebCore::mainThreadNormalWorld())->globalExec());
}

JSStringRef asJSStringRef(JNIEnv *env, jstring str)
{
    unsigned int slen = env->GetStringLength(str);
    const jchar* schars = env->GetStringCritical(str, NULL);
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
    if (val == NULL)
        return JSValueMakeNull(ctx);
    JSC::ExecState* exec = toJS(ctx);

    jclass clJSObject = getJSObjectClass(env);
    if (env->IsInstanceOf(val, clJSObject)) {
        static jfieldID fldPeer = env->GetFieldID(clJSObject, "peer", "J");
        static jfieldID fldPeerType = env->GetFieldID(clJSObject, "peer_type", "I");
        jlong peer = env->GetLongField(val, fldPeer);
        jint peer_type = env->GetIntField(val, fldPeerType);
        JSC::JSObject *jobject = 0;
        switch (peer_type) {
        case com_sun_webkit_dom_JSObject_JS_CONTEXT_OBJECT:
            return static_cast<JSObjectRef>(jlong_to_ptr(peer));
        case com_sun_webkit_dom_JSObject_JS_DOM_NODE_OBJECT:
        case com_sun_webkit_dom_JSObject_JS_DOM_WINDOW_OBJECT:
            {
                JSDOMGlobalObject* globalObject = toJSDOMGlobalObject(
                    ((peer_type == com_sun_webkit_dom_JSObject_JS_DOM_WINDOW_OBJECT)
                        ? static_cast<DOMWindow*>(jlong_to_ptr(peer))->document()
                        : &static_cast<Node*>(jlong_to_ptr(peer))->document()),
                    exec);
                return toRef(exec,
                    (peer_type == com_sun_webkit_dom_JSObject_JS_DOM_WINDOW_OBJECT)
                        ? WebCore::toJS(exec, globalObject, static_cast<DOMWindow*>(jlong_to_ptr(peer)))
                        : WebCore::toJS(exec, globalObject, static_cast<Node*>(jlong_to_ptr(peer))));
            }
        }
    }
    jclass clString = getStringClass(env);
    if (env->IsInstanceOf(val, clString)) {
      JSStringRef value = asJSStringRef(env, (jstring) val);
      return JSValueMakeString(ctx, value);
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
    static JGClass clCharacter(env->FindClass("java/lang/Character"));
    if (env->IsInstanceOf(val, clCharacter)) {
        static jmethodID charValueMethod
            = env->GetMethodID(clCharacter, "charValue", "()C");
        return toRef(exec,
            JSC::JSValue((int) env->CallCharMethod(val, charValueMethod)));
    }

    JLObject valClass(JSC::Bindings::callJNIMethod<jobject>(val, "getClass", "()Ljava/lang/Class;"));
    if (JSC::Bindings::callJNIMethod<jboolean>(valClass, "isArray", "()Z")) {
        JLString className((jstring)JSC::Bindings::callJNIMethod<jobject>(valClass, "getName", "()Ljava/lang/String;"));
        const char* classNameC = JSC::Bindings::getCharactersFromJString(className);
        JSC::JSValue arr = JSC::Bindings::JavaArray::convertJObjectToArray(exec, val, classNameC, rootObject, accessControlContext);
        JSC::Bindings::releaseCharactersForJString(className, classNameC);
        return toRef(exec, arr);
    }
    else {
        PassRefPtr<JSC::Bindings::JavaInstance> jinstance = JSC::Bindings::JavaInstance::create(val, rootObject, accessControlContext);
        return toRef(jinstance->createRuntimeObject(exec));
    }
}

jstring JSValue_to_Java_String(JSValueRef value, JNIEnv* env, JSContextRef ctx)
{
    JSStringRef str = JSValueToStringCopy(ctx, value, NULL);
    size_t slen = JSStringGetLength(str);
    const JSChar* schars = JSStringGetCharactersPtr(str);
    jstring result = env->NewString((const jchar*) schars, slen);
    JSStringRelease(str);
    return result;
}

jobject JSValue_to_Java_Object(
    JSValueRef value,
    JNIEnv* env,
    JSContextRef ctx,
    JSC::Bindings::RootObject* rootObject)
{
    JSC::ExecState* exec = toJS(ctx);
    return convertValueToJValue(exec, rootObject, toJS(exec, value),
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
    if (str == NULL) {
        throwNullPointerException(env);
        return NULL;
    }
    JSStringRef script = asJSStringRef(env, str);
    JSValueRef exception = 0;
    JSValueRef value = JSEvaluateScript(ctx, script, object, NULL, 1, &exception);
    JSStringRelease(script);
    if (exception) {
        throwJavaException(env, ctx, exception, rootObject);
        return NULL;
    }
    return WebCore::JSValue_to_Java_Object(value, env, ctx, rootObject);
}

}


PassRefPtr<JSC::Bindings::RootObject> checkJSPeer(
    jlong peer,
    jint peer_type,
    JSObjectRef &object,
    JSContextRef &context)
{
    JSC::Bindings::RootObject *rootObject = NULL;
    switch (peer_type) {
    case com_sun_webkit_dom_JSObject_JS_CONTEXT_OBJECT:
        {
            //based on [JavaJSObject] implementation
            //[Source/WebCore/bridge/jni/jni_jsobject.mm]
            object = static_cast<JSObjectRef>(jlong_to_ptr(peer));
            rootObject = JSC::Bindings::findProtectingRootObject(reinterpret_cast<JSC::JSObject*>(object));
            if (rootObject) {
                context = toRef(rootObject->globalObject()->globalExec());
            }
        }
        break;
    case com_sun_webkit_dom_JSObject_JS_DOM_NODE_OBJECT:
    case com_sun_webkit_dom_JSObject_JS_DOM_WINDOW_OBJECT:
        {
            WebCore::Frame* frame = (peer_type == com_sun_webkit_dom_JSObject_JS_DOM_WINDOW_OBJECT)
                ? static_cast<WebCore::DOMWindow*>(jlong_to_ptr(peer))->document()->frame()
                : static_cast<WebCore::Node*>(jlong_to_ptr(peer))->document().frame();

            rootObject = frame->script().createRootObject(frame).leakRef();
            if (rootObject) {
                context = WebCore::getGlobalContext(&frame->script());
                JSC::ExecState* exec = toJS(context);

                object = const_cast<JSObjectRef>(toRef(exec,
                    (peer_type == com_sun_webkit_dom_JSObject_JS_DOM_WINDOW_OBJECT)
                    ? WebCore::toJS(exec, static_cast<WebCore::JSDOMGlobalObject *>(rootObject->globalObject()), static_cast<WebCore::DOMWindow*>(jlong_to_ptr(peer)))
                    : WebCore::toJS(exec, static_cast<WebCore::JSDOMGlobalObject *>(rootObject->globalObject()), static_cast<WebCore::Node*>(jlong_to_ptr(peer)))));

            }
        }
        break;
    };

    return rootObject;
}



extern "C" {

JNIEXPORT jobject JNICALL Java_com_sun_webkit_dom_JSObject_evalImpl
(JNIEnv *env, jclass clas, jlong peer, jint peer_type, jstring str)
{
    if (str == NULL) {
        throwNullPointerException(env);
        return NULL;
    }
    JSObjectRef object;
    JSContextRef ctx;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));

    return WebCore::executeScript(env, object, ctx, rootObject.get(), str);
}

JNIEXPORT jobject JNICALL Java_com_sun_webkit_dom_JSObject_getMemberImpl
(JNIEnv *env, jclass clas, jlong peer, jint peer_type, jstring str)
{
    if (str == NULL) {
        throwNullPointerException(env);
        return NULL;
    }
    JSObjectRef object;
    JSContextRef ctx;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));

    JSStringRef name = WebCore::asJSStringRef(env, str);
    JSValueRef value = JSObjectGetProperty(ctx, object, name, NULL);
    JSStringRelease(name);
    return WebCore::JSValue_to_Java_Object(value, env, ctx, rootObject.get());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_JSObject_setMemberImpl
(JNIEnv *env, jclass clas, jlong peer, jint peer_type, jstring str, jobject value, jobject accessControlContext)
{
    if (str == NULL) {
        throwNullPointerException(env);
        return;
    }
    JSObjectRef object;
    JSContextRef ctx;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));

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
(JNIEnv *env, jclass clas, jlong peer, jint peer_type, jstring str)
{
    if (str == NULL) {
        throwNullPointerException(env);
        return;
    }
    JSObjectRef object;
    JSContextRef ctx;
    checkJSPeer(peer, peer_type, object, ctx);

    JSStringRef name = WebCore::asJSStringRef(env, str);
    JSObjectDeleteProperty(ctx, object, name, NULL);
    JSStringRelease(name);
}

JNIEXPORT jobject JNICALL Java_com_sun_webkit_dom_JSObject_getSlotImpl
  (JNIEnv *env, jclass clas, jlong peer, jint peer_type, jint index)
{
    JSObjectRef object;
    JSContextRef ctx;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));

    JSValueRef value = JSObjectGetPropertyAtIndex(ctx, object, index, NULL);
    return WebCore::JSValue_to_Java_Object(value, env, ctx, rootObject.get());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_JSObject_setSlotImpl
(JNIEnv *env, jclass clas, jlong peer, jint peer_type, jint index, jobject value, jobject accessControlContext)
{
    JSObjectRef object;
    JSContextRef ctx;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));

    JSValueRef jsvalue = WebCore::Java_Object_to_JSValue(env, ctx, rootObject.get(), value, accessControlContext);
    JSPropertyAttributes attributes = 0;
    JSObjectSetPropertyAtIndex(ctx, object, (unsigned) index, jsvalue, NULL);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_JSObject_toStringImpl
(JNIEnv *env, jclass clas, jlong peer, jint peer_type)
{
    JSObjectRef object;
    JSContextRef ctx;
    checkJSPeer(peer, peer_type, object, ctx);

    JSC::ExecState* exec = toJS(ctx);

    return toJS(object)->toString(exec)->value(exec)
        .toJavaString(env).releaseLocal();
}

JNIEXPORT jobject JNICALL Java_com_sun_webkit_dom_JSObject_callImpl
  (JNIEnv *env, jclass, jlong peer, jint peer_type, jstring methodName, jobjectArray args, jobject accessControlContext)
{
    if (methodName == NULL || args == NULL) {
        throwNullPointerException(env);
        return NULL;
    }
    JSObjectRef object;
    JSContextRef ctx;
    RefPtr<JSC::Bindings::RootObject> rootObject(checkJSPeer(peer, peer_type, object, ctx));
    if (!rootObject || !ctx) {
        env->ThrowNew(getJSExceptionClass(env), "Invalid function reference");
        return NULL;
    }

    JSStringRef name = WebCore::asJSStringRef(env, methodName);
    JSValueRef member = JSObjectGetProperty(ctx, object, name, NULL);
    JSStringRelease(name);
    if (!JSValueIsObject(ctx, member))
        return JSC::Bindings::convertUndefinedToJObject();
    JSObjectRef function = JSValueToObject(ctx, member, NULL);
    if (! JSObjectIsFunction(ctx, function))
        return JSC::Bindings::convertUndefinedToJObject();
    size_t argumentCount = env->GetArrayLength(args);
    JSValueRef *arguments = new JSValueRef[argumentCount];
    for (int i = 0;  i < argumentCount; i++) {
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
        return NULL;
    }
    return WebCore::JSValue_to_Java_Object(result, env, ctx, rootObject.get());
}

}
