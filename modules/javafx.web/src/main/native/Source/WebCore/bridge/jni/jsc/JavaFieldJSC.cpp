/*
 * Copyright (C) 2003, 2004, 2005, 2007, 2008, 2009, 2010 Apple Inc. All rights reserved.
 * Copyright 2010, The Android Open Source Project
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

JavaField::JavaField(wkj_ref aField)
{
    /*
     * Get field type name. This is getType().getName() in one call: the intermediate Class
     * object the JNI code fetched was used for nothing else, and a null from either half
     * lands on the same "<Unknown>" it always did.
     */
    String fieldTypeName = javaFieldTypeName(aField);
    if (fieldTypeName.isNull())
        fieldTypeName = "<Unknown>"_s;
    m_typeClassName = JavaString(fieldTypeName);

    m_type = javaTypeFromClassName(m_typeClassName.utf8());

    // Get field name
    String fieldName = javaFieldName(aField);
    if (fieldName.isNull())
        fieldName = "<Unknown>"_s;
    m_name = JavaString(fieldName);

    m_field = JobjectWrapper::create(aField);
}

JSValue JavaField::valueFromInstance(JSGlobalObject* globalObject, const Instance* i) const
{
    const JavaInstance* instance = static_cast<const JavaInstance*>(i);

    JSValue jsresult = jsUndefined();
    wkj_ref jfield = m_field->instance();
    // Since jfield is a weak reference, taking a strong one to safeguard instance() from GC
    WKJHandle jlfield = WKJHandle::retained(jfield);

    if (!jlfield) {
        LOG_ERROR("Could not get javaInstance for %llu in JavaField::valueFromInstance", static_cast<unsigned long long>(jfield));
        return jsresult;
    }

    wkj_ref jinstance = instance->javaInstance();
    // Since jinstance is a weak reference, taking a strong one to safeguard instance() from GC
    WKJHandle jlinstance = WKJHandle::retained(jinstance);

    if (!jlinstance) {
        LOG_ERROR("Could not get javaInstance for %llu in JavaField::valueFromInstance", static_cast<unsigned long long>(jinstance));
        return jsresult;
    }

    WKJJavaValue value = emptyJavaValue();
    JavaValueScope valueScope(value);

    switch (m_type) {
    case JavaTypeArray:
    case JavaTypeObject:
    // Since we can't convert java.lang.Character to any JS primitive, we have
    // to treat it as JS foreign object.
    case JavaTypeChar:
        {
            javaFieldGet(jfield, jinstance, JavaTypeObject, value);
            wkj_ref anObject = value.l;
            if (!anObject)
                return jsNull();

            const char* arrayType = typeClassName();
            if (arrayType[0] == '[')
                jsresult = JavaArray::convertJObjectToArray(globalObject, anObject, arrayType, instance->rootObject(), instance->accessControlContext());
            else if (anObject)

            jsresult = toJS(globalObject, WebCore::Java_Object_to_JSValue(toRef(globalObject), instance->rootObject(), anObject, instance->accessControlContext()));
        }
        break;

    case JavaTypeBoolean:
        javaFieldGet(jfield, jinstance, JavaTypeBoolean, value);
        jsresult = jsBoolean(value.i != 0);
        break;

    case JavaTypeByte:
        javaFieldGet(jfield, jinstance, JavaTypeByte, value);
        jsresult = jsNumber(value.i);
        break;

    case JavaTypeShort:
        javaFieldGet(jfield, jinstance, JavaTypeShort, value);
        jsresult = jsNumber(value.i);
        break;

    case JavaTypeInt:
        javaFieldGet(jfield, jinstance, JavaTypeInt, value);
        jsresult = jsNumber(static_cast<int>(value.i));
        break;

    case JavaTypeLong:
        javaFieldGet(jfield, jinstance, JavaTypeLong, value);
        jsresult = jsNumber(static_cast<double>(value.j));
        break;
    case JavaTypeFloat:
        javaFieldGet(jfield, jinstance, JavaTypeFloat, value);
        jsresult = jsNumber(static_cast<double>(static_cast<float>(value.d)));
        break;

    case JavaTypeDouble:
        javaFieldGet(jfield, jinstance, JavaTypeDouble, value);
        jsresult = jsNumber(value.d);
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
    const JavaInstance* instance = static_cast<const JavaInstance*>(i);
    WKJJavaValue javaValue = convertValueToJValue(globalObject, i->rootObject(), aValue, m_type, typeClassName());
    JavaValueScope javaValueScope(javaValue);
#if !PLATFORM(JAVA)
    LOG(LiveConnect, "JavaField::setValueToInstance setting value %s to %s", String(name().impl()).utf8().data(), aValue.toString(globalObject)->value(globalObject).ascii().data());
#endif

    wkj_ref jfield = m_field->instance();
    // Since jfield is a weak reference, taking a strong one to safeguard instance() from GC
    WKJHandle jlfield = WKJHandle::retained(jfield);

    if (!jlfield) {
        LOG_ERROR("Could not get Instance for %llu in JavaField::setValueToInstance", static_cast<unsigned long long>(jfield));
        return false;
    }

    wkj_ref jinstance = instance->javaInstance();
    // Since jinstance is a weak reference, taking a strong one to safeguard javaInstance() from GC
    WKJHandle jlinstance = WKJHandle::retained(jinstance);

    if (!jlinstance) {
        LOG_ERROR("Could not get javaInstance for %llu in JavaField::setValueToInstance", static_cast<unsigned long long>(jinstance));
        return false;
    }

    switch (m_type) {
    case JavaTypeArray:
    case JavaTypeObject:
    case JavaTypeBoolean:
    case JavaTypeByte:
    case JavaTypeChar:
    case JavaTypeShort:
    case JavaTypeInt:
    case JavaTypeLong:
    case JavaTypeFloat:
    case JavaTypeDouble:
        /*
         * Field.set / setBoolean / setByte / setChar / setShort / setInt / setLong / setFloat
         * / setDouble, chosen by m_type on the Java side of the slot rather than by nine
         * call sites here. The value carries its own type, so the two cannot disagree.
         */
        javaFieldSet(jfield, jinstance, m_type, javaValue);
        break;

    default:
        abort();
    }
    return true;
}

#endif // ENABLE(JAVA_BRIDGE)
