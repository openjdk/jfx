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
#include <JavaScriptCore/Identifier.h>
#include <JavaScriptCore/JSLock.h>

using namespace JSC;
using namespace JSC::Bindings;

JavaClass::JavaClass(wkj_ref anInstance, RootObject* rootObject, wkj_ref accessControlContext)
{
    // Since anInstance is a weak reference, taking a strong one to safeguard instance() from GC
    WKJHandle jlinstance = WKJHandle::retained(anInstance);
    WKJHandle dummyInstance;

    if (!jlinstance) {
        LOG_ERROR("Could not get javaInstance for %llu in JavaClass Constructor", static_cast<unsigned long long>(anInstance));
        dummyInstance = createDummyObject();
        anInstance = dummyInstance.get();
        if (anInstance == 0) {
            LOG_ERROR("Could not createDummyObject for %llu in JavaClass Constructor", static_cast<unsigned long long>(anInstance));
            m_name = fastStrDup("<Unknown>");
            return;
        }
    }

    WKJHandle aClass = javaObjectClass(anInstance);

    if (!aClass) {
        LOG_ERROR("Unable to call getClass on instance %llu", static_cast<unsigned long long>(anInstance));
        m_name = fastStrDup("<Unknown>");
        return;
    }

    /*
     * The class name arrives as UTF-16 and is converted here once. The JNI code read it with
     * GetStringUTFChars, which is modified UTF-8: the two agree for every name in the Basic
     * Multilingual Plane, and this conversion is what makes the difference impossible for
     * the rest (webkit_java_api_bridge.h, faithfulness note 2). Everything downstream -
     * strcmp against ASCII literals here, array-descriptor parsing in BridgeUtils - is
     * unchanged.
     */
    String className = javaClassName(aClass.get());
    if (!className.isNull())
        m_name = fastStrDup(className.utf8().data());
    else
        m_name = fastStrDup("<Unknown>");

    int i;

    // Get the fields
    {
        WKJJavaValue result = emptyJavaValue();
        JavaValueScope resultScope(result);
        WKJHandle methodId = javaResolveMethod(aClass.get(), "getFields"_s,
            "()[Ljava/lang/reflect/Field;"_s);
        if (!dispatchJavaCall(0, rootObject, aClass.get(), JavaTypeArray, methodId.get(),
                              nullptr, result, accessControlContext)) {
            wkj_ref fields = result.l;
            int numFields = javaArrayLength(fields);
            for (i = 0; i < numFields; i++) {
                WKJJavaValue element = emptyJavaValue();
                JavaValueScope elementScope(element);
                if (!javaArrayGet(fields, i, JavaTypeObject, element))
                    continue;
                JavaField* aField = new JavaField(element.l); // deleted in the JavaClass destructor
                {
                    // FIXME: Should we acquire a JSLock here?
                    m_fields.set(aField->name().impl(), aField);
                }
            }
        }
    }

    // Get the methods
    {
        WKJJavaValue result = emptyJavaValue();
        JavaValueScope resultScope(result);
        WKJHandle methodId = javaResolveMethod(aClass.get(), "getMethods"_s,
            "()[Ljava/lang/reflect/Method;"_s);
        if (!dispatchJavaCall(0, rootObject, aClass.get(), JavaTypeArray, methodId.get(),
                              nullptr, result, accessControlContext)) {
            wkj_ref methods = result.l;
            int numMethods = javaArrayLength(methods);
            for (i = 0; i < numMethods; i++) {
                WKJJavaValue element = emptyJavaValue();
                JavaValueScope elementScope(element);
                if (!javaArrayGet(methods, i, JavaTypeObject, element))
                    continue;
                JavaMethod* aMethod = new JavaMethod(element.l); // deleted in the JavaClass destructor
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
            }
        }
    }
}

JavaClass::~JavaClass()
{
    fastFree(const_cast<char*>(m_name));

    // FIXME: Should we acquire a JSLock here?

//    deleteAllValues(m_fields);  todo tav
    m_fields.clear();

    MethodListMap::const_iterator end = m_methods.end();
    for (MethodListMap::const_iterator it = m_methods.begin(); it != end; ++it) {
        const MethodList* methodList = it->value;
//        deleteAllValues(*methodList); todo tav
        delete methodList;
    }
    m_methods.clear();
}

WKJHandle JavaClass::createDummyObject()
{
    /*
     * FindClass("java/lang/Object") + GetMethodID("<init>") + NewObject, in one host slot.
     * The three separate failure messages the JNI version logged had one cause between them
     * - the JVM would not give us a java.lang.Object - and one outcome, so they are one
     * message now.
     */
    WKJHandle instance = javaCreateDummyObject();
    if (!instance)
        LOG_ERROR("Unable to create NewObject in JavaClass::createDummyObject");
    return instance;
}

Method* JavaClass::methodNamed(PropertyName propertyName, Instance*) const
{
    const String name(propertyName.publicName());
    if (name.isNull())
        return nullptr;
    unsigned nameLength = name.length();
    MethodList* methodList;
    size_t i;
    if (nameLength >= 3 && name[nameLength-1] == ')'
        && (i = name.find('(', 1)) != WTF::notFound) {
        Vector<String> pnames;
        size_t pstart = i + 1;
        if (pstart < nameLength-1) {
            do {
                size_t pnext = name.find(',', pstart);
                if (pnext == WTF::notFound)
                    pnext = nameLength-1;
                String pname = name.substringSharingImpl(pstart, pnext-pstart);
                pnames.append(pname);
                pstart = pnext+1;
            } while (pstart < nameLength);
        }
        size_t plen = pnames.size();
        MethodList* allMethods
            = m_methods.get(name.substringSharingImpl(0, i).impl());
        methodList = nullptr;
        size_t numMethods = allMethods == nullptr ? 0 : allMethods->size();
        for (size_t methodIndex = 0; methodIndex < numMethods; methodIndex++) {
            JavaMethod* jMethod = static_cast<JavaMethod*>(allMethods->at(methodIndex));
            if (size_t(jMethod->numParameters()) == plen) {
                // Iterate over parameters.
                for (size_t i = 0;  ;  i++) {
                    if (i == plen) {
                        if (methodList == nullptr)
                            methodList = new MethodList();
                        methodList->append(jMethod);
                        break;
                    }
                    String methodParam = jMethod->parameterAt(i);
                    size_t methodParamLength = methodParam.length();
                    String pname = pnames[i];
                    size_t pnameLength = pname.length();
                    // Handle array type names.
                    while (methodParamLength >= 2 && methodParam[0] == '['
                           && pnameLength >= 3 && pname[pnameLength-2] == '['
                           && pname[pnameLength-1] == ']') {
                        // Primitive array type names.
                        if (methodParamLength == 2) {
                          ASCIILiteral prim;
                          switch (methodParam[1]) {
                          case 'I': prim = "int[]"_s; break;
                          case 'J': prim = "long[]"_s; break;
                          case 'B': prim = "byte[]"_s; break;
                          case 'S': prim = "short[]"_s; break;
                          case 'F': prim = "float[]"_s; break;
                          case 'D': prim = "double[]"_s; break;
                          case 'C': prim = "char[]"_s; break;
                          case 'Z': prim = "boolean[]"_s; break;
                          default: prim = { };
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
                        && methodParam.find("java.lang."_s, 0) == 0) {
                        methodParam = methodParam.substringSharingImpl(10, pnameLength);
                        methodParamLength = pnameLength;
                    }
                    if (methodParamLength == pnameLength) {
                        size_t k = 0;
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
    if (methodList)
        return methodList->at(0);
    return nullptr;
}

Field* JavaClass::fieldNamed(PropertyName propertyName, Instance*) const
{
    String name(propertyName.publicName());
    if (name.isNull())
        return nullptr;
    return m_fields.get(name.impl());
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

bool JavaClass::isCharacterClass() const
{
    return !strcmp(m_name, "java.lang.Character");
}

bool JavaClass::isStringClass() const
{
    return !strcmp(m_name, "java.lang.String");
}

#endif // ENABLE(JAVA_BRIDGE)
