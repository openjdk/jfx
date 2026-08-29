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
#include "JavaArrayJSC.h"

#if ENABLE(JAVA_BRIDGE)

#include "JNIUtilityPrivate.h"
#include "JavaInstanceJSC.h"
#include "JobjectWrapper.h"
#include "runtime_array.h"
#include "runtime_object.h"
#include "runtime_root.h"
#include <JavaScriptCore/Error.h>

#include "Logging.h"

using namespace JSC;
using namespace JSC::Bindings;
using namespace WebCore;

JSValue JavaArray::convertJObjectToArray(JSGlobalObject* globalObject, wkj_ref anObject, const char* type, RefPtr<RootObject>&& rootObject, wkj_ref accessControlContext)
{
    if (type[0] != '[')
        return jsUndefined();

    return RuntimeArray::create(globalObject, new JavaArray(anObject, type, WTF::move(rootObject), accessControlContext));
}

JavaArray::JavaArray(wkj_ref array, const char* type, RefPtr<RootObject>&& rootObject, wkj_ref accessControlContext)
    : Array(WTF::move(rootObject))
{
    m_array = JobjectWrapper::create(array);

    // Java array are fixed length, so we can cache length.

    // Since m_array->instance() is a weak reference, taking a strong one to safeguard instance() from GC
    WKJHandle jlarrayinstance = WKJHandle::retained(m_array->instance());

    if (!jlarrayinstance) {
        LOG_ERROR("Could not get javaInstance for %llu in JavaArray Constructor", static_cast<unsigned long long>(m_array->instance()));
        m_length = 0;
    } else {
        m_length = static_cast<unsigned int>(javaArrayLength(m_array->instance()));
    }

    m_type = strdup(type);
    m_accessControlContext = JobjectWrapper::create(accessControlContext, true);
}

JavaArray::~JavaArray()
{
    free(const_cast<char*>(m_type));
}

RootObject* JavaArray::rootObject() const
{
    return m_rootObject && m_rootObject->isValid() ? m_rootObject.get() : 0;
}

bool JavaArray::setValueAt(JSGlobalObject* globalObject, unsigned index, JSValue aValue) const
{
    // Since javaArray() is a weak reference, taking a strong one to safeguard instance() from GC
    WKJHandle jlinstance = WKJHandle::retained(javaArray());

    if (!jlinstance) {
        LOG_ERROR("Could not get javaInstance for %llu in JavaArray::setValueAt", static_cast<unsigned long long>(javaArray()));
        return false;
    }

    char* javaClassName = 0;

    JavaType arrayType = javaTypeFromPrimitiveType(m_type[1]);
    if (m_type[1] == 'L') {
        // The type of the array will be something like:
        // "[Ljava.lang.string;". This is guaranteed, so no need
        // for extra sanity checks.
        javaClassName = strdup(&m_type[2]);
        javaClassName[strchr(javaClassName, ';')-javaClassName] = 0;
    }
    WKJJavaValue aJValue = convertValueToJValue(globalObject, m_rootObject.get(), aValue, arrayType, javaClassName);
    JavaValueScope aJValueScope(aJValue);

    switch (arrayType) {
    case JavaTypeObject:
    case JavaTypeBoolean:
    case JavaTypeByte:
    case JavaTypeChar:
    case JavaTypeShort:
    case JavaTypeInt:
    case JavaTypeLong:
    case JavaTypeFloat:
    case JavaTypeDouble:
        {
            /*
             * SetObjectArrayElement and the eight Set<Type>ArrayRegion calls, chosen by
             * arrayType on the Java side of the slot. The case list is exactly the one the
             * JNI switch had: an array of arrays still falls through to the default and is
             * silently not written, which is what this code has always done.
             */
            javaArraySet(javaArray(), static_cast<int>(index), arrayType, aJValue);
            break;
        }
    default:
        break;
    }

    if (javaClassName)
        free(const_cast<char*>(javaClassName));
    return true;
}

JSValue JavaArray::valueAt(JSGlobalObject* globalObject, unsigned index) const
{
    // Since javaArray() is a weak reference, taking a strong one to safeguard instance() from GC
    WKJHandle jlinstance = WKJHandle::retained(javaArray());

    if (!jlinstance) {
        LOG_ERROR("Could not get javaInstance for %llu in JavaArray::valueAt", static_cast<unsigned long long>(javaArray()));
        return jsUndefined();
    }

    JavaType arrayType = javaTypeFromPrimitiveType(m_type[1]);
    WKJJavaValue element = emptyJavaValue();
    JavaValueScope elementScope(element);

    switch (arrayType) {
    case JavaTypeObject:
        {
            javaArrayGet(javaArray(), static_cast<int>(index), JavaTypeObject, element);
            wkj_ref anObject = element.l;

            // No object?
            if (!anObject)
                return jsNull();

            // Nested array?
            if (m_type[1] == '[')
                return JavaArray::convertJObjectToArray(globalObject, anObject,
                        m_type + 1, rootObject(), accessControlContext());
            // or array of other object type?
            return JavaInstance::create(anObject, rootObject(),
                    accessControlContext())->createRuntimeObject(globalObject);
        }

    case JavaTypeBoolean:
        {
            javaArrayGet(javaArray(), static_cast<int>(index), JavaTypeBoolean, element);
            return jsBoolean(element.i != 0);
        }

    case JavaTypeByte:
        {
            javaArrayGet(javaArray(), static_cast<int>(index), JavaTypeByte, element);
            return jsNumber(element.i);
        }

    case JavaTypeChar:
        {
            javaArrayGet(javaArray(), static_cast<int>(index), JavaTypeChar, element);
            return jsNumber(element.i);
        }

    case JavaTypeShort:
        {
            javaArrayGet(javaArray(), static_cast<int>(index), JavaTypeShort, element);
            return jsNumber(element.i);
        }

    case JavaTypeInt:
        {
            javaArrayGet(javaArray(), static_cast<int>(index), JavaTypeInt, element);
            return jsNumber(element.i);
        }

    case JavaTypeLong:
        {
            javaArrayGet(javaArray(), static_cast<int>(index), JavaTypeLong, element);
            return jsNumber(static_cast<double>(element.j));
        }

    case JavaTypeFloat:
        {
            javaArrayGet(javaArray(), static_cast<int>(index), JavaTypeFloat, element);
            return jsNumber(static_cast<double>(static_cast<float>(element.d)));
        }

    case JavaTypeDouble:
        {
            javaArrayGet(javaArray(), static_cast<int>(index), JavaTypeDouble, element);
            return jsNumber(element.d);
        }
    default:
        break;
    }
    return jsUndefined();
}

unsigned int JavaArray::getLength() const
{
    return m_length;
}

#endif // ENABLE(JAVA_BRIDGE)
