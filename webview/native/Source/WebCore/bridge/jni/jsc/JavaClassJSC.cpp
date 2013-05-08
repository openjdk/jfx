/*
 * Copyright (C) 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010 Apple Inc. All rights reserved.
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
#include "JavaClassJSC.h"

#if ENABLE(JAVA_BRIDGE)

#include "JSDOMWindow.h"
#include "JavaFieldJSC.h"
#include "JavaMethodJSC.h"
#include "JNIUtilityPrivate.h"
#include <runtime/Identifier.h>
#include <runtime/JSLock.h>

#if PLATFORM(JAVA)
using namespace JSC;
#endif
using namespace JSC::Bindings;

JavaClass::JavaClass(jobject anInstance, RootObject* rootObject, jobject accessControlContext)
{
    jobject aClass = callJNIMethod<jobject>(anInstance, "getClass", "()Ljava/lang/Class;");

    if (!aClass) {
        LOG_ERROR("Unable to call getClass on instance %p", anInstance);
        m_name = fastStrDup("<Unknown>");
        return;
    }

    if (jstring className = (jstring)callJNIMethod<jobject>(aClass, "getName", "()Ljava/lang/String;")) {
        const char* classNameC = getCharactersFromJString(className);
        m_name = fastStrDup(classNameC);
        releaseCharactersForJString(className, classNameC);
    } else
        m_name = fastStrDup("<Unknown>");

    int i;
    JNIEnv* env = getJNIEnv();

    // Get the fields
    jvalue result;
    jobject args[1];
    jmethodID methodId = getMethodID(aClass, "getFields", "()[Ljava/lang/reflect/Field;");
    if (dispatchJNICall(0, rootObject, aClass, false, JavaTypeArray, methodId,
                        args, result, accessControlContext) == NULL) {
        jarray fields = (jarray) result.l;
        int numFields = env->GetArrayLength(fields);
        for (i = 0; i < numFields; i++) {
            jobject aJField = env->GetObjectArrayElement((jobjectArray)fields, i);
            JavaField* aField = new JavaField(env, aJField); // deleted in the JavaClass destructor
            {
                // FIXME: Should we acquire a JSLock here?
                m_fields.set(aField->name().impl(), aField);
            }
            env->DeleteLocalRef(aJField);
        }
        env->DeleteLocalRef(fields);
    }

    // Get the methods
    methodId = getMethodID(aClass, "getMethods", "()[Ljava/lang/reflect/Method;");
    if (dispatchJNICall(0, rootObject, aClass, false, JavaTypeArray, methodId,
                        args, result, accessControlContext) == NULL) {
        jarray methods = (jarray) result.l;
        int numMethods = env->GetArrayLength(methods);
        for (i = 0; i < numMethods; i++) {
            jobject aJMethod = env->GetObjectArrayElement((jobjectArray)methods, i);
            JavaMethod* aMethod = new JavaMethod(env, aJMethod); // deleted in the JavaClass destructor
            MethodList* methodList;
            {
                // FIXME: Should we acquire a JSLock here?

                methodList = m_methods.get(aMethod->name().impl());
                if (!methodList) {
                    methodList = new MethodList();
                    m_methods.set(aMethod->name().impl(), methodList);
                }
            }
            methodList->append(aMethod);
            env->DeleteLocalRef(aJMethod);
        }
        env->DeleteLocalRef(methods);
    }

    env->DeleteLocalRef(aClass);
}

JavaClass::~JavaClass()
{
    fastFree(const_cast<char*>(m_name));

    // FIXME: Should we acquire a JSLock here?

    deleteAllValues(m_fields);
    m_fields.clear();

    MethodListMap::const_iterator end = m_methods.end();
    for (MethodListMap::const_iterator it = m_methods.begin(); it != end; ++it) {
        const MethodList* methodList = it->second;
        deleteAllValues(*methodList);
        delete methodList;
    }
    m_methods.clear();
}

MethodList JavaClass::methodsNamed(PropertyName propertyName, Instance*) const
{
#if ENABLE(JAVA_JSC)
    const UString& name = propertyName.publicName();
    unsigned nameLength = name.length();
    MethodList* methodList;
    int i;
    if (nameLength >= 3 && name[nameLength-1] == ')'
        && (i = name.find('(', 1)) != WTF::notFound) {
        Vector<UString> pnames;
        int pstart = i+1;
        if (pstart < nameLength-1) {
            do {
                int pnext = name.find(',', pstart);
                if (pnext == WTF::notFound)
                    pnext = nameLength-1;
                UString pname = name.substringSharingImpl(pstart, pnext-pstart);
                pnames.append(pname);
                pstart = pnext+1;
            } while (pstart < nameLength);
        }
        size_t plen = pnames.size();
        MethodList* allMethods
            = m_methods.get(name.substringSharingImpl(0, i).impl());
        methodList = NULL;
        size_t numMethods = allMethods == NULL ? 0 : allMethods->size();
        for (size_t methodIndex = 0; methodIndex < numMethods; methodIndex++) {
            JavaMethod* jMethod = static_cast<JavaMethod*>(allMethods->at(methodIndex));
            if (jMethod->numParameters() == plen) {
                // Iterate over parameters.
                for (int i = 0;  ;  i++) {
                    if (i == plen) {
                        if (methodList == NULL)
                            methodList = new MethodList();
                        methodList->append(jMethod);
                        break;
                    }
                    String methodParam = jMethod->parameterAt(i);
                    size_t methodParamLength = methodParam.length();
                    // Compare a UString and a String.
                    // Is there a better way to do it?
                    UString pname = pnames[i];
                    size_t pnameLength = pname.length();
                    // Handle array type names.
                    while (methodParamLength >= 2 && methodParam[0] == '['
                           && pnameLength >= 3 && pname[pnameLength-2] == '['
                           && pname[pnameLength-1] == ']') {
                        // Primitive array type names.
                        if (methodParamLength == 2) {
                          UChar sig1 = methodParam[1];
                          const char *prim;
                          switch (methodParam[1]) {
                          case 'I': prim = "int[]"; break;
                          case 'J': prim = "long[]"; break;
                          case 'B': prim = "byte[]"; break;
                          case 'S': prim = "short[]"; break;
                          case 'F': prim = "float[]"; break;
                          case 'D': prim = "double[]"; break;
                          case 'C': prim = "char[]"; break;
                          case 'Z': prim = "boolean[]"; break;
                          default: prim = NULL;
                          }
                          if (pname == prim) {
                              methodParamLength = 0;
                              pnameLength = 0;
                          } else
                            break;
                        }
                        // Object array type names.
                        else if (methodParamLength > 3
                                && methodParam[1] == 'L'
                                && methodParam[methodParamLength-1] == ';') {
                            pnameLength -= 2;
                            pname = pname.substringSharingImpl(0, pnameLength);
                            methodParamLength -= 3;
                            methodParam = methodParam
                                .substringSharingImpl(2, methodParamLength); 
                        } else {
                          break;
                        }
                    }
                    if (methodParamLength == pnameLength + 10
                        && methodParam.find("java.lang.", 0) == 0) {
                        methodParam = methodParam.substringSharingImpl(10, pnameLength);
                        methodParamLength = pnameLength;
                    }
                    if (methodParamLength == pnameLength) {
                        int k = 0;
                        for (; k < methodParamLength;  k++) {
                            if (methodParam[k] != pname[k]) {
                                break;
                            }
                        }
                        if (k < methodParamLength)
                            break;
                    } else
                        break;
                }
            }
        }
    } else {
        methodList = m_methods.get(name.impl());
    }
#else
    MethodList* methodList = m_methods.get(propertyName.publicName());
#endif

    if (methodList)
        return *methodList;
    return MethodList();
}

Field* JavaClass::fieldNamed(PropertyName propertyName, Instance*) const
{
    return m_fields.get(propertyName.publicName());
}

bool JavaClass::isNumberClass() const
{
    return (!strcmp(m_name, "java.lang.Byte")
        || !strcmp(m_name, "java.lang.Short")
        || !strcmp(m_name, "java.lang.Integer")
        || !strcmp(m_name, "java.lang.Long")
        || !strcmp(m_name, "java.lang.Float")
        || !strcmp(m_name, "java.lang.Double"));
}

bool JavaClass::isBooleanClass() const
{
    return !strcmp(m_name, "java.lang.Boolean");
}

#if ENABLE(JAVA_JSC)
bool JavaClass::isCharacterClass() const
{
    return !strcmp(m_name, "java.lang.Character");
}
#endif

bool JavaClass::isStringClass() const
{
    return !strcmp(m_name, "java.lang.String");
}

#endif // ENABLE(JAVA_BRIDGE)
