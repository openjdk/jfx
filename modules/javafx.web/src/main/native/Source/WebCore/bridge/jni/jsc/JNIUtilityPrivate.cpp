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
#include "JNIUtilityPrivate.h"

#if ENABLE(JAVA_BRIDGE)

#include "jni_jsobject.h"
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

#include "com_sun_webkit_dom_JSObject.h"
#define JSOBJECT_CLASSNAME "com/sun/webkit/dom/JSObject"

namespace JSC {

namespace Bindings {

static jchar toJCharValue(const JSValue& value, JSGlobalObject* globalObject)
{
    // If JS type is string and target Java type is char, then
    // return the first unicode character.
    if (value.isString()) {
        String stringValue = value.toString(globalObject)->value(globalObject);
        return (jchar)stringValue[0];
    }
    return (jchar)value.toNumber(globalObject);
}

jobject convertUndefinedToJObject()
{
    static JGObject jgoUndefined;
    if (!jgoUndefined) {
        JNIEnv* env = getJNIEnv();
        jclass clazz = env->FindClass(JSOBJECT_CLASSNAME);
        jgoUndefined = JLObject(env->GetStaticObjectField(
            clazz,
            env->GetStaticFieldID(clazz, "UNDEFINED", "Ljava/lang/String;")));
    }
    return jgoUndefined;
}

jvalue convertValueToJValue(JSGlobalObject* globalObject, RootObject* rootObject, JSValue value, JavaType javaType, const char* javaClassName)
{
    JSLockHolder lock(globalObject);

    VM& vm = globalObject->vm();

    jvalue result;
    memset(&result, 0, sizeof(jvalue));

    switch (javaType) {
    case JavaTypeArray:
    case JavaTypeObject:
        {
            // FIXME: JavaJSObject::convertValueToJObject functionality is almost exactly the same,
            // these functions should use common code.

            if (value.isObject()) {
                JSObject* object = asObject(value);
                if (object->inherits(vm, JavaRuntimeObject::info())) {
                    // Unwrap a Java instance.
                    JavaRuntimeObject* runtimeObject = static_cast<JavaRuntimeObject*>(object);
                    JavaInstance* instance = runtimeObject->getInternalJavaInstance();
                    if (instance) {
                        // Since instance->javaInstance() is WeakGlobalRef, creating a localref to safeguard javaInstance() from GC
                        JLObject jlinstance(instance->javaInstance(), true);
                        if (!jlinstance) {
                            LOG_ERROR("Could not get javaInstance for %p in JNIUtilityPrivate::convertValueToJValue", (jobject)jlinstance);
                            return result;
                        }
                        result.l = instance->javaInstance();
                    }
                } else if (object->classInfo(vm) == RuntimeArray::info()) {
                    // Input is a JavaScript Array that was originally created from a Java Array
                    RuntimeArray* imp = static_cast<RuntimeArray*>(object);
                    JavaArray* array = static_cast<JavaArray*>(imp->getConcreteArray());

                    // Since array->javaArray() is WeakGlobalRef, creating a localref to safeguard javaInstance() from GC
                    JLObject jlinstancearray(array->javaArray(), true);
                    if (!jlinstancearray) {
                        LOG_ERROR("Could not get javaArrayInstance for %p in JNIUtilityPrivate::convertValueToJValue", (jobject)jlinstancearray);
                        return result;
                    }
                    result.l = array->javaArray();
                } else if ((!result.l && (!strcmp(javaClassName, "java.lang.Object")))
                           || (!strcmp(javaClassName, "netscape.javascript.JSObject"))) {
                    // Wrap objects in JSObject instances.
                    JNIEnv* env = getJNIEnv();
                    if (object->inherits(vm, WebCore::JSNode::info())) {
                        WebCore::JSNode* jsnode = static_cast<WebCore::JSNode*>(object);
                        static JGClass nodeImplClass = env->FindClass("com/sun/webkit/dom/NodeImpl");
                        static jmethodID getImplID = env->GetStaticMethodID(nodeImplClass, "getCachedImpl",
                                                                     "(J)Lorg/w3c/dom/Node;");
                        WebCore::Node *peer = &jsnode->wrapped();
                        peer->ref(); //deref is in NodeImpl disposer
                        result.l = env->CallStaticObjectMethod(
                            nodeImplClass,
                            getImplID,
                            ptr_to_jlong(peer));
                    } else {
                        static JGClass jsObjectClass = env->FindClass(JSOBJECT_CLASSNAME);
                        static jmethodID constructorID = env->GetMethodID(jsObjectClass, "<init>", "(JI)V");
                        if (constructorID) {
                            rootObject->gcProtect(object);
                            jlong nativeHandle = ptr_to_jlong(object);
                            result.l = env->NewObject(jsObjectClass, constructorID,
                                nativeHandle,
                                com_sun_webkit_dom_JSObject_JS_CONTEXT_OBJECT);
                        }
                    }
                }
            }

            // Create an appropriate Java object if target type is java.lang.Object or other wrapper Objects {Integer, Double, Boolean}.
            if (!result.l) {
                if (value.isString() && !strcmp(javaClassName, "java.lang.Object")) {
                    String stringValue = asString(value)->value(globalObject);
                    JNIEnv* env = getJNIEnv();
                    jobject javaString = stringValue.toJavaString(env).releaseLocal();
                    result.l = javaString;
                } else if (value.isString() && !strcmp(javaClassName, "java.lang.Character")) {
                    JNIEnv* env = getJNIEnv();
                    static JGClass clazz(env->FindClass("java/lang/Character"));
                    jmethodID meth = env->GetStaticMethodID(clazz, "valueOf", "(C)Ljava/lang/Character;");
                    jchar charValue = toJCharValue(value, globalObject);
                    jobject javaChar = env->CallStaticObjectMethod(clazz, meth, charValue);
                    result.l = javaChar;
                } else if (value.isNumber()) {
                    JNIEnv* env = getJNIEnv();
                    if (value.isInt32() && (!strcmp(javaClassName, "java.lang.Number") || !strcmp(javaClassName, "java.lang.Integer") || !strcmp(javaClassName, "java.lang.Object"))) {
                        static JGClass clazz(env->FindClass("java/lang/Integer"));
                        jmethodID meth = env->GetStaticMethodID(clazz, "valueOf", "(I)Ljava/lang/Integer;");
                        result.l = env->CallStaticObjectMethod(clazz, meth, (jint) value.asInt32());
                    } else if (!strcmp(javaClassName, "java.lang.Number") || !strcmp(javaClassName, "java.lang.Double") || !strcmp(javaClassName, "java.lang.Object")) {
                        jdouble doubleValue = (jdouble) value.asNumber();
                        static JGClass clazz = env->FindClass("java/lang/Double");
                        jmethodID meth = env->GetStaticMethodID(clazz, "valueOf", "(D)Ljava/lang/Double;");
                        jobject javaDouble = env->CallStaticObjectMethod(clazz, meth, doubleValue);
                        result.l = javaDouble;
                    }
                } else if (value.isBoolean() && (!strcmp(javaClassName, "java.lang.Boolean") || !strcmp(javaClassName, "java.lang.Object"))) {
                    bool boolValue = value.asBoolean();
                    JNIEnv* env = getJNIEnv();
                    static JGClass clazz(env->FindClass("java/lang/Boolean"));
                    jmethodID meth = env->GetStaticMethodID(clazz, "valueOf", "(Z)Ljava/lang/Boolean;");
                    jobject javaBoolean = env->CallStaticObjectMethod(clazz, meth, boolValue);
                    result.l = javaBoolean;
                } else if (value.isUndefined()) {
                    result.l = convertUndefinedToJObject();
                }
            }

            // Convert value to a string if the target type is a java.lang.String, and we're not
            // converting from a null.
            if (!result.l && !strcmp(javaClassName, "java.lang.String")) {
                if (!value.isNull()) {
                    String stringValue = value.toString(globalObject)->value(globalObject);
                    JNIEnv* env = getJNIEnv();
                    jobject javaString = stringValue.toJavaString(env).releaseLocal();
                    result.l = javaString;
                }
            }
        }
        break;

    case JavaTypeBoolean:
        {
            result.z = (jboolean)value.toNumber(globalObject);
        }
        break;

    case JavaTypeByte:
        {
            result.b = (jbyte)value.toNumber(globalObject);
        }
        break;

    case JavaTypeChar:
        {
            result.c = toJCharValue(value, globalObject);
        }
        break;

    case JavaTypeShort:
        {
            result.s = (jshort)value.toNumber(globalObject);
        }
        break;

    case JavaTypeInt:
        {
            result.i = (jint)value.toNumber(globalObject);
        }
        break;

    case JavaTypeLong:
        {
            result.j = (jlong)value.toNumber(globalObject);
        }
        break;

    case JavaTypeFloat:
        {
            result.f = (jfloat)value.toNumber(globalObject);
        }
        break;

    case JavaTypeDouble:
        {
            result.d = (jdouble)value.toNumber(globalObject);
        }
        break;

    case JavaTypeInvalid:
    case JavaTypeVoid:
        break;
    }
    return result;
}

jobject jvalueToJObject(jvalue value, JavaType jtype) {
    JNIEnv* env = getJNIEnv();
    jmethodID meth;
    switch (jtype) {
    case JavaTypeObject:
    case JavaTypeArray:
        return value.l;
    case JavaTypeBoolean: {
      static JGClass clsZ(env->FindClass("java/lang/Boolean"));
      meth = env->GetStaticMethodID(clsZ, "valueOf", "(Z)Ljava/lang/Boolean;");
      return env->CallStaticObjectMethod(clsZ, meth, value.z);
    }
    case JavaTypeChar: {
      static JGClass clsC(env->FindClass("java/lang/Character"));
      meth = env->GetStaticMethodID(clsC, "valueOf",
                                    "(C)Ljava/lang/Character;");
      return env->CallStaticObjectMethod(clsC, meth, value.c);
    }
    case JavaTypeByte: {
      static JGClass clsB(env->FindClass("java/lang/Byte"));
      meth = env->GetStaticMethodID(clsB, "valueOf", "(B)Ljava/lang/Byte;");
      return env->CallStaticObjectMethod(clsB, meth, value.b);
    }
    case JavaTypeShort: {
      static JGClass clsS(env->FindClass("java/lang/Short"));
      meth = env->GetStaticMethodID(clsS, "valueOf", "(S)Ljava/lang/Short;");
      return env->CallStaticObjectMethod(clsS, meth, value.s);
    }
    case JavaTypeInt: {
      static JGClass clsI(env->FindClass("java/lang/Integer"));
      meth = env->GetStaticMethodID(clsI, "valueOf", "(I)Ljava/lang/Integer;");
      return env->CallStaticObjectMethod(clsI, meth, value.i);
    }
    case JavaTypeLong: {
      static JGClass clsJ(env->FindClass("java/lang/Long"));
      meth = env->GetStaticMethodID(clsJ, "valueOf", "(J)Ljava/lang/Long;");
      return env->CallStaticObjectMethod(clsJ, meth, value.j);
    }
    case JavaTypeFloat: {
      static JGClass clsF(env->FindClass("java/lang/Float"));
      meth = env->GetStaticMethodID(clsF, "valueOf", "(F)Ljava/lang/Float;");
      return env->CallStaticObjectMethod(clsF, meth, value.f);
    }
    case JavaTypeDouble: {
      static JGClass clsD(env->FindClass("java/lang/Double"));
      meth = env->GetStaticMethodID(clsD, "valueOf", "(D)Ljava/lang/Double;");
      return env->CallStaticObjectMethod(clsD, meth, value.d);
    }
    default:
        abort();
    }
}

jthrowable dispatchJNICall(int count, RootObject*, jobject obj, bool isStatic, JavaType returnType, jmethodID methodId, jobject* args, jvalue& result, jobject accessControlContext) {

    // Since obj is WeakGlobalRef, creating a localref to safeguard instance() from GC
    JLObject jlinstance(obj, true);

    if (!jlinstance) {
        LOG_ERROR("Could not get javaInstance for %p in JNIUtilityPrivate::dispatchJNICall", (jobject)jlinstance);
        return NULL;
    }

    JNIEnv* env = getJNIEnv();
    jclass objClass = env->GetObjectClass(obj);
    jobject rmethod = env->ToReflectedMethod(objClass, methodId, isStatic);
    jclass utilityCls = env->FindClass("com/sun/webkit/Utilities");
    jclass objectCls = env->FindClass("java/lang/Object");
    jobjectArray argsArray = env->NewObjectArray(count, objectCls, NULL);
    for (int i = 0;  i < count; i++)
      env->SetObjectArrayElement(argsArray, i, args[i]);
    jmethodID invokeMethod =
        env->GetStaticMethodID(utilityCls, "fwkInvokeWithContext",
                               "(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;Ljava/security/AccessControlContext;)Ljava/lang/Object;");
    jobject r = env->CallStaticObjectMethod(utilityCls, invokeMethod,
                                            rmethod, obj, argsArray,
                                            accessControlContext);

    jthrowable ex = env->ExceptionOccurred();
    env->ExceptionClear();

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
        result.l = r;
        break;

    case JavaTypeBoolean:
        result.z = callJNIMethod<jboolean>(r, "booleanValue", "()Z");
        break;

    case JavaTypeByte:
        result.b = callJNIMethod<jbyte>(r, "byteValue", "()B");
        break;

    case JavaTypeShort:
        result.s = callJNIMethod<jshort>(r, "shortValue", "()S");
        break;

    case JavaTypeInt:
        result.i = callJNIMethod<jint>(r, "intValue", "()I");
        break;

    case JavaTypeLong:
        result.j = callJNIMethod<jlong>(r, "longValue", "()J");
        break;

    case JavaTypeFloat:
        result.f = callJNIMethod<jfloat>(r, "floatValue", "()F");
        break;

    case JavaTypeDouble:
        result.d = callJNIMethod<jdouble>(r, "doubleValue", "()D");
        break;

    case JavaTypeInvalid:
        /* Nothing to do */
        break;
    }
    return ex;
}

} // end of namespace Bindings

} // end of namespace JSC

#endif // ENABLE(JAVA_BRIDGE)
