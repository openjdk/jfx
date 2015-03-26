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
#include <runtime/Error.h>

#include <APICast.h>

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

JSValue JavaField::valueFromInstance(ExecState* exec, const Instance* i) const
{
    const JavaInstance* instance = static_cast<const JavaInstance*>(i);

    JSValue jsresult = jsUndefined();
    jobject jfield = m_field->instance();
    jobject jinstance = instance->javaInstance();    
    switch (m_type) {
    case JavaTypeArray:
    case JavaTypeObject:
        {
            jobject anObject = callJNIMethod<jobject>(jfield, "get", "(Ljava/lang/Object;)Ljava/lang/Object;", jinstance);
            if (!anObject)
                return jsNull();

            const char* arrayType = typeClassName();
            if (arrayType[0] == '[')
                jsresult = JavaArray::convertJObjectToArray(exec, anObject, arrayType, instance->rootObject(), instance->accessControlContext());
            else if (anObject)

            jsresult = toJS(exec, WebCore::Java_Object_to_JSValue(getJNIEnv(), toRef(exec), instance->rootObject(), anObject, instance->accessControlContext()));
        }
        break;

    case JavaTypeBoolean:
        jsresult = jsBoolean(callJNIMethod<jboolean>(jfield, "getBoolean", "(Ljava/lang/Object;)Z", jinstance));
        break;

    case JavaTypeByte:
        jsresult = jsNumber(callJNIMethod<jbyte>(jfield, "getByte", "(Ljava/lang/Object;)B", jinstance));
        break;

    case JavaTypeChar:
        jsresult = jsNumber(callJNIMethod<jchar>(jfield, "getChar", "(Ljava/lang/Object;)C", jinstance));
        break;

    case JavaTypeShort:
        jsresult = jsNumber(callJNIMethod<jshort>(jfield, "getShort", "(Ljava/lang/Object;)S", jinstance));
        break;
                            
    case JavaTypeInt:
        jsresult = jsNumber(static_cast<int>(callJNIMethod<jint>(jfield, "getInt", "(Ljava/lang/Object;)I", jinstance)));
        break;

    case JavaTypeLong:
        jsresult = jsNumber(static_cast<double>(callJNIMethod<jlong>(jfield, "getLong", "(Ljava/lang/Object;)J", jinstance)));
        break;
    case JavaTypeFloat:
        jsresult = jsNumber(static_cast<double>(callJNIMethod<jfloat>(jfield, "getFloat", "(Ljava/lang/Object;)F", jinstance)));
        break;

    case JavaTypeDouble:
        jsresult = jsNumber(static_cast<double>(callJNIMethod<jdouble>(jfield, "getDouble", "(Ljava/lang/Object;)D", jinstance)));
        break;

    default:
        break;
    }

    LOG(LiveConnect, "JavaField::valueFromInstance getting %s = %s", String(name().impl()).utf8().data(), jsresult.toString(exec)->value(exec).ascii().data());

    return jsresult;
}

void JavaField::setValueToInstance(ExecState* exec, const Instance* i, JSValue aValue) const
{
    const JavaInstance* instance = static_cast<const JavaInstance*>(i);
    jvalue javaValue = convertValueToJValue(exec, i->rootObject(), aValue, m_type, typeClassName());
    LOG(LiveConnect, "JavaField::setValueToInstance setting value %s to %s", String(name().impl()).utf8().data(), aValue.toString(exec)->value(exec).ascii().data());

    jobject jfield = m_field->instance();    
    jobject jinstance = instance->javaInstance();

    switch (m_type) {
    case JavaTypeArray:
    case JavaTypeObject:
        callJNIMethod<void>(jfield, "set", "(Ljava/lang/Object;Ljava/lang/Object;)V", jinstance, javaValue.l);
        break;

    case JavaTypeBoolean:
        callJNIMethod<void>(jfield, "setBoolean", "(Ljava/lang/Object;Z)V", jinstance, javaValue.z);
        break;

    case JavaTypeByte:
        callJNIMethod<void>(jfield, "setByte", "(Ljava/lang/Object;B)V", jinstance, javaValue.b);
        break;

    case JavaTypeChar:
        callJNIMethod<void>(jfield, "setChar", "(Ljava/lang/Object;C)V", jinstance, javaValue.c);
        break;

    case JavaTypeShort:
        callJNIMethod<void>(jfield, "setShort", "(Ljava/lang/Object;S)V", jinstance, javaValue.s);
        break;

    case JavaTypeInt:
        callJNIMethod<void>(jfield, "setInt", "(Ljava/lang/Object;I)V", jinstance, javaValue.i);
        break;

    case JavaTypeLong:
        callJNIMethod<void>(jfield, "setLong", "(Ljava/lang/Object;J)V", jinstance, javaValue.j);
        break;

    case JavaTypeFloat:
        callJNIMethod<void>(jfield, "setFloat", "(Ljava/lang/Object;F)V", jinstance, javaValue.f);
        break;

    case JavaTypeDouble:
        callJNIMethod<void>(jfield, "setDouble", "(Ljava/lang/Object;D)V", jinstance, javaValue.d);
        break;

    default:
        abort();
    }
}

#endif // ENABLE(JAVA_BRIDGE)
