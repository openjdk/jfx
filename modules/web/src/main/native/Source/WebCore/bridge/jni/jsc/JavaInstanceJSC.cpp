/*
 * Copyright (C) 2003, 2008, 2010 Apple Inc. All rights reserved.
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
#include "JavaInstanceJSC.h"

#if ENABLE(JAVA_BRIDGE)
#include "BridgeUtils.h"
#include "JavaRuntimeObject.h"
#include "JNIUtilityPrivate.h"
#include "JSDOMBinding.h"
#include "JavaArrayJSC.h"
#include "JavaClassJSC.h"
#include "JavaMethodJSC.h"
#include "JavaStringJSC.h"
#include "Logging.h"
#include "jni_jsobject.h"
#include "runtime_method.h"
#include "runtime_object.h"
#include "runtime_root.h"
#include <runtime/ArgList.h>
#include <runtime/Error.h>
#include <runtime/FunctionPrototype.h>
#include <runtime/JSLock.h>

#include <APICast.h>

using namespace JSC::Bindings;
using namespace JSC;
using namespace WebCore;

JavaInstance::JavaInstance(jobject instance, PassRefPtr<RootObject> rootObject, jobject accessControlContext)
    : Instance(rootObject)
{
    m_instance = JobjectWrapper::create(instance);
    m_class = 0;
    m_accessControlContext = JobjectWrapper::create(accessControlContext);
}

JavaInstance::~JavaInstance()
{
    delete m_class;
}

RuntimeObject* JavaInstance::newRuntimeObject(ExecState* exec)
{
    return JavaRuntimeObject::create(exec, exec->lexicalGlobalObject(), this);
}

#define NUM_LOCAL_REFS 64

void JavaInstance::virtualBegin()
{
    getJNIEnv()->PushLocalFrame(NUM_LOCAL_REFS);
}

void JavaInstance::virtualEnd()
{
    getJNIEnv()->PopLocalFrame(0);
}

Class* JavaInstance::getClass() const
{
    if (!m_class) {
        jobject acc  = accessControlContext();
        m_class = new JavaClass (m_instance->instance(), rootObject(), acc);
    }
    return m_class;
}

JSValue JavaInstance::stringValue(ExecState* exec) const
{
    JSLockHolder lock(exec);

    jobject obj = m_instance->instance();
    jobject acc  = accessControlContext();
    jmethodID methodId = getMethodID(obj, "toString", "()Ljava/lang/String;");
    jvalue result;
    jthrowable ex = dispatchJNICall(0, rootObject(), obj, false,
                                    JavaTypeObject, methodId,
                                    NULL, result, acc);
    if (ex != 0) {
        // FIXME duplicates code in JavaInstance::invokeMethod
        JSValue exceptionDescription
            = (JavaInstance::create(ex, rootObject(), accessControlContext())
               ->createRuntimeObject(exec));
	exec->vm().throwException(exec, createError(exec,
                                     (exceptionDescription.toString(exec)
                                      ->value(exec))));
        return jsUndefined();
    }

    jstring stringValue = (jstring) result.l;
    JNIEnv* env = getJNIEnv();
    const jchar* c = getUCharactersFromJStringInEnv(env, stringValue);
    String u((const UChar*)c, (int)env->GetStringLength(stringValue));
    releaseUCharactersForJStringInEnv(env, stringValue, c);
    return jsString(exec, u);
}

static JSValue numberValueForCharacter(jobject obj) {
    return jsNumber((int) callJNIMethod<jchar>(obj, "charValue", "()C"));
}

static JSValue numberValueForNumber(jobject obj) {
return jsNumber(callJNIMethod<jdouble>(obj, "doubleValue", "()D"));
}


JSValue JavaInstance::numberValue(ExecState*) const
{
    jobject obj = m_instance->instance();
    JavaClass* aClass = static_cast<JavaClass*>(getClass());
    if (aClass->isCharacterClass())
      return numberValueForCharacter(obj);
    if (aClass->isBooleanClass())
        return jsNumber((int)
                        // Replaced the following line to work around possible GCC bug, see RT-22725
	                // callJNIMethod<jboolean>(obj, "booleanValue", "()Z"));
                        callJNIMethod(obj, JavaTypeBoolean, "booleanValue", "()Z", 0).z);
    return numberValueForNumber(obj);
}

JSValue JavaInstance::booleanValue() const
{
    // Changed the call to work around possible GCC bug, see RT-22725
    jboolean booleanValue = callJNIMethod(m_instance->instance(), JavaTypeBoolean, "booleanValue", "()Z", 0).z;
    return jsBoolean(booleanValue);
}

class JavaRuntimeMethod : public RuntimeMethod {
public:
    typedef RuntimeMethod Base;

    static JavaRuntimeMethod* create(ExecState* exec, JSGlobalObject* globalObject, const String& name, Bindings::Method *method)
    {
        // FIXME: deprecatedGetDOMStructure uses the prototype off of the wrong global object
        // We need to pass in the right global object for "i".
        Structure* domStructure = WebCore::deprecatedGetDOMStructure<JavaRuntimeMethod>(exec);
        JavaRuntimeMethod* _method = new (NotNull, allocateCell<JavaRuntimeMethod>(*exec->heap())) JavaRuntimeMethod(globalObject, domStructure, method);
        _method->finishCreation(exec->vm(), name);
        return _method;
    }

    static Structure* createStructure(VM& globalData, JSGlobalObject* globalObject, JSValue prototype)
    {
        return Structure::create(globalData, globalObject, prototype, TypeInfo(ObjectType, StructureFlags), &s_info);
    }

    static const ClassInfo s_info;

private:
    JavaRuntimeMethod(JSGlobalObject* globalObject, Structure* structure, Bindings::Method *method)
        : RuntimeMethod(globalObject, structure, method)
    {
    }

    void finishCreation(VM& globalData, const String& name)
    {
        Base::finishCreation(globalData, name);
        ASSERT(inherits(&s_info));
    }
};

const ClassInfo JavaRuntimeMethod::s_info = { "JavaRuntimeMethod", &RuntimeMethod::s_info, 0, 0, CREATE_METHOD_TABLE(JavaRuntimeMethod) };

JSValue JavaInstance::getMethod(ExecState* exec, PropertyName propertyName)
{
    Method *method = getClass()->methodNamed(propertyName, this);
    return JavaRuntimeMethod::create(exec, exec->lexicalGlobalObject(), propertyName.publicName(), method);
}

JSValue JavaInstance::invokeMethod(ExecState* exec, RuntimeMethod* runtimeMethod)
{
    if (!asObject(runtimeMethod)->inherits(&JavaRuntimeMethod::s_info))
        exec->vm().throwException(exec, createTypeError(exec, "Attempt to invoke non-Java method on Java object."));

    int count = exec->argumentCount();
#if 0
    const MethodList& methodList = *runtimeMethod->methods();
    size_t numMethods = methodList.size();

    Method* method = 0;
    // Try to find a good match for the overloaded method.  The
    // fundamental problem is that JavaScript doesn't have the
    // notion of method overloading and Java does.  We could
    // get a bit more sophisticated and attempt to does some
    // type checking as we as checking the number of parameters.
    for (size_t methodIndex = 0; methodIndex < numMethods; methodIndex++) {
        Method* aMethod = methodList[methodIndex];
        if (aMethod->numParameters() == count) {
            method = aMethod;
            break;
        }
    }
#else
    Method* method = runtimeMethod->method();
#endif

    if (!method) {
        LOG(LiveConnect, "JavaInstance::invokeMethod unable to find an appropriate method");
        return jsUndefined();
    }

    const JavaMethod* jMethod = static_cast<const JavaMethod*>(method);
    LOG(LiveConnect, "JavaInstance::invokeMethod call %s %s on %p", String(jMethod->name().impl()).utf8().data(), jMethod->signature(), m_instance->instance());

    if (jMethod->numParameters() != count) {
        LOG(LiveConnect, "JavaInstance::invokeMethod unable to find an appropriate method with specified signature");
        return jsUndefined();
    }
	
    Vector<jobject> jArgs(count);

    for (int i = 0; i < count; i++) {
        CString javaClassName = jMethod->parameterAt(i).utf8();
        JavaType jtype = javaTypeFromClassName(javaClassName.data());
        jvalue jarg = convertValueToJValue(exec, m_rootObject.get(),
            exec->argument(i), jtype, javaClassName.data());
        jArgs[i] = jvalueToJObject(jarg, jtype);
        LOG(LiveConnect, "JavaInstance::invokeMethod arg[%d] = %s", i, exec->argument(i).toString(exec)->value(exec).ascii().data());
    }

    jvalue result;

    // Try to use the JNI abstraction first, otherwise fall back to
    // normal JNI.  The JNI dispatch abstraction allows the Java plugin
    // to dispatch the call on the appropriate internal VM thread.
    RootObject* rootObject = this->rootObject();
    if (jMethod->isStatic())
        return exec->vm().throwException(exec, createTypeError(exec, "invoking static method"));
    if (!rootObject)
        return jsUndefined();

    bool handled = false;
    if (rootObject->nativeHandle()) {
        jobject obj = m_instance->instance();
        const char *callingURL = 0; // FIXME, need to propagate calling URL to Java
        jmethodID methodId = getMethodID(obj, jMethod->name().utf8().data(), jMethod->signature());

        jthrowable ex = dispatchJNICall(exec->argumentCount(), rootObject,
                                        obj, jMethod->isStatic(),
                                        jMethod->returnType(), methodId,
                                        jArgs.data(), result,
                                        accessControlContext());
        if (ex != NULL) {
          JSValue exceptionDescription
            = (JavaInstance::create(ex, rootObject, accessControlContext())
               ->createRuntimeObject(exec));
          exec->vm().throwException(exec, exceptionDescription);
          return jsUndefined();
        }
    }

    JSValue resultValue;
    switch (jMethod->returnType()) {
    case JavaTypeVoid:
        {
            resultValue = jsUndefined();
        }
        break;

    case JavaTypeArray:
      /* ... fall through ... */
    case JavaTypeObject:
        {
            JNIEnv* env = getJNIEnv();
            resultValue = toJS(exec, WebCore::Java_Object_to_JSValue(env, toRef(exec), rootObject, result.l, accessControlContext()));
        }
        break;

    case JavaTypeBoolean:
        {
            resultValue = jsBoolean(result.z);
        }
        break;

    case JavaTypeByte:
        {
            resultValue = jsNumber(result.b);
        }
        break;

    case JavaTypeChar:
        {
            resultValue = jsNumber(result.c);
        }
        break;

    case JavaTypeShort:
        {
            resultValue = jsNumber(result.s);
        }
        break;

    case JavaTypeInt:
        {
            resultValue = jsNumber(result.i);
        }
        break;

    case JavaTypeLong:
        {
            resultValue = jsNumber(result.j);
        }
        break;

    case JavaTypeFloat:
        {
            resultValue = jsNumber(result.f);
        }
        break;

    case JavaTypeDouble:
        {
            resultValue = jsNumber(result.d);
        }
        break;

    case JavaTypeInvalid:
        {
            resultValue = jsUndefined();
        }
        break;
    }

    return resultValue;
}

JSValue JavaInstance::defaultValue(ExecState* exec, PreferredPrimitiveType hint) const
{
    if (hint == PreferString)
        return stringValue(exec);
    if (hint == PreferNumber)
        return numberValue(exec);
    JavaClass* aClass = static_cast<JavaClass*>(getClass());
    if (aClass->isStringClass())
        return stringValue(exec);

    if (aClass->isNumberClass())
        return numberValueForNumber(m_instance->instance());
    if (aClass->isCharacterClass())
        return numberValueForCharacter(m_instance->instance());
    if (aClass->isBooleanClass())
        return booleanValue();
    return valueOf(exec);
}

JSValue JavaInstance::valueOf(ExecState* exec) const
{
    return stringValue(exec);
}

#endif // ENABLE(JAVA_BRIDGE)
