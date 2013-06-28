/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef BridgeUtils_h
#define BridgeUtils_h

#include "JNIUtility.h"
#include "ScriptController.h"
#include "JSValue.h"
#include <JavaScriptCore/JSObjectRef.h>


namespace WebCore {
  
    /* Returns a local reference to a fresh Java String. */
    jstring JSValue_to_Java_String(JSValueRef value, JNIEnv* env, JSContextRef ctx);
    jobject JSValue_to_Java_Object(JSValueRef value, JNIEnv* env, JSContextRef ctx, JSC::Bindings::RootObject* rootPeer);
    JSValueRef Java_Object_to_JSValue(JNIEnv *env, JSContextRef ctx, JSC::Bindings::RootObject* rootObject, jobject val, jobject accessControlContext);
    JSStringRef asJSStringRef(JNIEnv *env, jstring str);
    JSGlobalContextRef getGlobalContext(WebCore::ScriptController* sc);
    jobject executeScript(JNIEnv* env,
                          JSObjectRef object,
                          JSContextRef ctx,
                          JSC::Bindings::RootObject* rootPeer,
                          jstring script);
}

#endif // BridgeUtils_h
