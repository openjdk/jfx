/*
 * Copyright (C) 2003, 2004, 2005, 2007, 2009 Apple Inc. All rights reserved.
 * Copyright 2010, The Android Open Source Project
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

#include "config.h"
#include "JavaFieldJSC.h"

#if ENABLE(JAVA_BRIDGE)

#include "BridgeUtils.h"
#include "JNIUtilityPrivate.h"
#include "JavaArrayJSC.h"
#include "Logging.h"
#include "runtime_array.h"
#include "runtime_object.h"

#include <JavaScriptCore/Error.h>
#include <JavaScriptCore/APICast.h>

using namespace JSC;
using namespace JSC::Bindings;
using namespace WebCore;

JavaField::JavaField(JNIEnv* env, jobject aField)
{
    // Get field type name
    jstring fieldTypeName = 0;
    jclass fieldType = static_cast<jclass>(callJNIMethod<jobject>(aField, "getType", "()Ljava/lang/Class;"));
    if (fieldType)
        fieldTypeName = static_cast<jstring>(callJNIMethod<jobject>(fieldType, "getName", "()Ljava/lang/String;"));
    if (!fieldTypeName)
        fieldTypeName = env->NewStringUTF("<Unknown>");
    m_typeClassName = JavaString(env, fieldTypeName);

    m_type = javaTypeFromClassName(m_typeClassName.utf8());
    env->DeleteLocalRef(fieldType);
    env->DeleteLocalRef(fieldTypeName);

    // Get field name
    jstring fieldName = static_cast<jstring>(callJNIMethod<jobject>(aField, "getName", "()Ljava/lang/String;"));
    if (!fieldName)
        fieldName = env->NewStringUTF("<Unknown>");
    m_name = JavaString(env, fieldName);
    env->DeleteLocalRef(fieldName);

    m_field = JobjectWrapper::create(aField);
}

JSValue JavaField::valueFromInstance(JSGlobalObject* globalObject, const Instance* i) const
{
    VM& vm = globalObject->vm();
    auto scope = DECLARE_THROW_SCOPE(vm);

    const JavaInstance* instance = static_cast<const JavaInstance*>(i);

    JSValue jsresult = jsUndefined();
    jobject jfield = m_field->instance();
    // Since jfield is WeakGlobalRef, creating a localref to safeguard instance() from GC
    JLObject jlfield(jfield, true);

    if (!jlfield) {
        LOG_ERROR("Could not get javaInstance for %p in JavaField::valueFromInstance", (jobject)jlfield);
        return jsresult;
    }

    jobject jinstance = instance->javaInstance();
    // Since jinstance is WeakGlobalRef, creating a localref to safeguard instance() from GC
    JLObject jlinstance(jinstance, true);

    if (!jlinstance) {
        LOG_ERROR("Could not get javaInstance for %p in JavaField::valueFromInstance", (jobject)jlinstance);
        return jsresult;
    }

    JNIEnv* env = getJNIEnv();
    jclass utilityCls = env->FindClass("com/sun/webkit/Utilities");
    jmethodID getMethod =
        env->GetStaticMethodID(utilityCls, "fwkGetFieldWithContext",
                                            "(Ljava/lang/reflect/Field;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    jobject rObj = env->CallStaticObjectMethod(utilityCls,
                                               getMethod,
                                               jfield,
                                               jinstance,
                                               instance->accessControlContext());
    jthrowable ex = env->ExceptionOccurred();
    env->ExceptionClear();
    if (ex != NULL) {
        JSValue exceptionDescription
          = (JavaInstance::create(ex, instance->rootObject(), instance->accessControlContext())
             ->createRuntimeObject(globalObject));
        throwException(globalObject, scope, exceptionDescription);
        return jsresult;
    }
    if (!rObj) {
        return jsNull();
    }

    switch (m_type) {
    case JavaTypeArray:
    case JavaTypeObject:
    // Since we can't convert java.lang.Character to any JS primitive, we have
    // to treat it as JS foreign object.
    case JavaTypeChar:
        {
            const char* arrayType = typeClassName();
            if (arrayType[0] == '[')
                jsresult = JavaArray::convertJObjectToArray(globalObject, rObj, arrayType, instance->rootObject(), instance->accessControlContext());
            else
                jsresult = toJS(globalObject, WebCore::Java_Object_to_JSValue(getJNIEnv(), toRef(globalObject), instance->rootObject(), rObj, instance->accessControlContext()));
        }
        break;

    case JavaTypeBoolean:
        jsresult = jsBoolean(callJNIMethod<jboolean>(rObj, "booleanValue", "()Z"));
        break;

    case JavaTypeByte:
        jsresult = jsNumber(callJNIMethod<jbyte>(rObj, "byteValue", "()B"));
        break;

    case JavaTypeShort:
        jsresult = jsNumber(callJNIMethod<jshort>(rObj, "shortValue", "()S"));
        break;

    case JavaTypeInt:
        jsresult = jsNumber(static_cast<int>(callJNIMethod<jint>(rObj, "intValue", "()I")));
        break;

    case JavaTypeLong:
        jsresult = jsNumber(static_cast<double>(callJNIMethod<jlong>(rObj, "longValue", "()J")));
        break;
    case JavaTypeFloat:
        jsresult = jsNumber(static_cast<double>(callJNIMethod<jfloat>(rObj, "floatValue", "()F")));
        break;

    case JavaTypeDouble:
        jsresult = jsNumber(static_cast<double>(callJNIMethod<jdouble>(rObj, "doubleValue", "()D")));
        break;

    default:
        break;
    }
#if !PLATFORM(JAVA)  // debug build issue
    LOG(LiveConnect, "JavaField::valueFromInstance getting %s = %s", String(name().impl()).utf8().data(), jsresult.toString(globalObject)->value(globalObject).ascii().data());
#endif

    return jsresult;
}

bool JavaField::setValueToInstance(JSGlobalObject* globalObject, const Instance* i, JSValue aValue) const
{
    VM& vm = globalObject->vm();
    auto scope = DECLARE_THROW_SCOPE(vm);

    const JavaInstance* instance = static_cast<const JavaInstance*>(i);
    jvalue javaValue = convertValueToJValue(globalObject, i->rootObject(), aValue, m_type, typeClassName());
#if !PLATFORM(JAVA)
    LOG(LiveConnect, "JavaField::setValueToInstance setting value %s to %s", String(name().impl()).utf8().data(), aValue.toString(globalObject)->value(globalObject).ascii().data());
#endif

    jobject jfield = m_field->instance();
    // Since jfield is WeakGlobalRef, creating a localref to safeguard instance() from GC
    JLObject jlfield(jfield, true);

    if (!jlfield) {
        LOG_ERROR("Could not get Instance for %p in JavaField::setValueToInstance", (jobject)jlfield);
        return false;
    }

    jobject jinstance = instance->javaInstance();
    // Since jinstance is WeakGlobalRef, creating a localref to safeguard javaInstance() from GC
    JLObject jlinstance(jinstance, true);

    if (!jlinstance) {
        LOG_ERROR("Could not get javaInstance for %p in JavaField::setValueToInstance", (jobject)jlinstance);
        return false;
    }

    JNIEnv* env = getJNIEnv();
    jobject boxedObj = jvalueToJObject(javaValue, m_type);
    jclass utilityCls = env->FindClass("com/sun/webkit/Utilities");
    jmethodID setMethod =
        env->GetStaticMethodID(utilityCls, "fwkSetFieldWithContext",
                                            "(Ljava/lang/reflect/Field;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V");
    env->CallStaticVoidMethod(utilityCls,
                              setMethod,
                              jfield,
                              jinstance,
                              boxedObj,
                              instance->accessControlContext());
    jthrowable ex = env->ExceptionOccurred();
    env->ExceptionClear();
    if (ex != NULL) {
        JSValue exceptionDescription
          = (JavaInstance::create(ex, instance->rootObject(), instance->accessControlContext())
             ->createRuntimeObject(globalObject));
        throwException(globalObject, scope, exceptionDescription);
        return false;
    }

    return true;
}

#endif // ENABLE(JAVA_BRIDGE)
