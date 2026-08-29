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
#include "JavaMethodJSC.h"

#if ENABLE(JAVA_BRIDGE)

#include <JavaScriptCore/JSObject.h>
#include <wtf/text/StringBuilder.h>

using namespace JSC;
using namespace JSC::Bindings;

JavaMethod::JavaMethod(wkj_ref aMethod)
{
    /*
     * Get return type name: getReturnType().getName() as one call, since the intermediate
     * Class was used for nothing else. A null name is the "<Unknown>" the JNI code
     * substituted - and this now also covers a null return type, which the JNI code passed
     * on to GetStringLength unchecked.
     */
    String returnTypeName = javaMethodReturnTypeName(aMethod);
    if (returnTypeName.isNull())
        returnTypeName = "<Unknown>"_s;
    m_returnTypeClassName = JavaString(returnTypeName);
    m_returnType = javaTypeFromClassName(m_returnTypeClassName.utf8());

    // Get method name
    String methodName = javaMethodName(aMethod);
    if (methodName.isNull())
        methodName = "<Unknown>"_s;
    m_name = JavaString(methodName);

    // Get parameters
    int numParams = javaMethodParameterCount(aMethod);
    for (int i = 0; i < numParams; i++) {
        String parameterName = javaMethodParameterTypeName(aMethod, i);
        if (parameterName.isNull())
            parameterName = "<Unknown>"_s;
        m_parameters.append(JavaString(parameterName).impl());
    }

    // Created lazily.
    m_signature = 0;

    int modifiers = javaMethodModifiers(aMethod);
    m_isStatic = (modifiers & 0x8) != 0;
}

JavaMethod::~JavaMethod()
{
    if (m_signature)
        fastFree(m_signature);
}

// JNI method signatures use '/' between components of a class name, but
// we get '.' between components from the reflection API.
static void appendClassName(StringBuilder& builder, const char* className)
{
    char* c = fastStrDup(className);

    char* result = c;
    while (*c) {
        if (*c == '.')
            *c = '/';
        c++;
    }

    builder.append(ASCIILiteral::fromLiteralUnsafe(result));

    fastFree(result);
}

const char* JavaMethod::signature() const
{
    if (!m_signature) {
        // FIXME: Should we acquire a JSLock here?

        StringBuilder signatureBuilder;
        signatureBuilder.append('(');
        for (unsigned int i = 0; i < m_parameters.size(); i++) {
            CString javaClassName = parameterAt(i).utf8();
            JavaType type = javaTypeFromClassName(javaClassName.data());
            if (type == JavaTypeArray)
                appendClassName(signatureBuilder, javaClassName.data());
            else {
                signatureBuilder.append(ASCIILiteral::fromLiteralUnsafe(signatureFromJavaType(type)));
                if (type == JavaTypeObject) {
                    appendClassName(signatureBuilder, javaClassName.data());
                    signatureBuilder.append(';');
                }
            }
        }
        signatureBuilder.append(')');

        const char* returnType = m_returnTypeClassName.utf8();
        if (m_returnType == JavaTypeArray)
            appendClassName(signatureBuilder, returnType);
        else {
            signatureBuilder.append(ASCIILiteral::fromLiteralUnsafe(signatureFromJavaType(m_returnType)));
            if (m_returnType == JavaTypeObject) {
                appendClassName(signatureBuilder, returnType);
                signatureBuilder.append(';');
            }
        }

        String signatureString = signatureBuilder.toString();
        m_signature = fastStrDup(signatureString.utf8().data());
    }

    return m_signature;
}

#endif // ENABLE(JAVA_BRIDGE)
