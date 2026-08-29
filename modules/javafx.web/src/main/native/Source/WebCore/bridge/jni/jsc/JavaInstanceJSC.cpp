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
#include "runtime_method.h"
#include "runtime_object.h"
#include "runtime_root.h"
#include "JavaArrayJSC.h"
#include "JavaClassJSC.h"
#include "JavaMethodJSC.h"
#include "JavaStringJSC.h"
#include "Logging.h"

#include <JavaScriptCore/APICast.h>
#include <JavaScriptCore/ArgList.h>
#include <JavaScriptCore/Error.h>
#include <JavaScriptCore/FunctionPrototype.h>
#include <JavaScriptCore/JSLock.h>
#include <JavaScriptCore/JSString.h>

using namespace JSC::Bindings;
using namespace JSC;
using namespace WebCore;

JavaInstance::JavaInstance(wkj_ref instance, RefPtr<RootObject>&& rootObject, wkj_ref accessControlContext)
    : Instance(WTF::move(rootObject))
{
    m_instance = JobjectWrapper::create(instance);
    m_class = 0;
    m_accessControlContext = JobjectWrapper::create(accessControlContext, true);
}

JavaInstance::~JavaInstance()
{
    delete m_class;
}

RuntimeObject* JavaInstance::newRuntimeObject(JSGlobalObject* globalObject)
{
    return JavaRuntimeObject::create(globalObject, this);
}

/*
 * These bracketed every Instance operation with PushLocalFrame(64) / PopLocalFrame(0), so
 * that the local references the JNI calls inside produced were reclaimed in one go.
 *
 * There is no local reference table any more, and nothing reclaims a registry id implicitly:
 * every id has a named owner - a WKJHandle, a JobjectWrapper or a JavaValueScope - which
 * releases it when the scope ends. That is why they are empty rather than deleted; the
 * Instance protocol still calls them, and there is nothing left for them to do.
 */
void JavaInstance::virtualBegin()
{
}

void JavaInstance::virtualEnd()
{
}

Class* JavaInstance::getClass() const
{
    if (!m_class) {
        wkj_ref acc = accessControlContext();
        m_class = new JavaClass(m_instance->instance(), rootObject(), acc);
    }
    return m_class;
}

JSValue JavaInstance::stringValue(JSGlobalObject* globalObject) const
{
    JSLockHolder lock(globalObject);

    VM& vm = globalObject->vm();
    auto scope = DECLARE_THROW_SCOPE(vm);

    wkj_ref obj = m_instance->instance();
    // Since m_instance->instance() is a weak reference, taking a strong one to safeguard instance() from GC
    WKJHandle jlinstance = WKJHandle::retained(obj);

    if (!jlinstance) {
        LOG_ERROR("Could not get javaInstance for %llu in JavaInstance::stringValue", static_cast<unsigned long long>(obj));
        return jsUndefined();
    }

    wkj_ref acc  = accessControlContext();

    WKJHandle methodId = javaResolveMethod(obj, "toString"_s, "()Ljava/lang/String;"_s);
    WKJJavaValue result = emptyJavaValue();
    JavaValueScope resultScope(result);
    WKJHandle ex = dispatchJavaCall(0, rootObject(), obj,
                                    JavaTypeObject, methodId.get(),
                                    nullptr, result, acc);
    if (ex) {
        // FIXME duplicates code in JavaInstance::invokeMethod
        JSValue exceptionDescription
            = (JavaInstance::create(ex.get(), rootObject(), accessControlContext())
               ->createRuntimeObject(globalObject));
        throwException(globalObject, scope, createError(globalObject,
                                (exceptionDescription.toString(globalObject)
                                    ->value(globalObject))));
        return jsUndefined();
    }

    /*
     * The characters of the java.lang.String toString() returned. The JNI code read them
     * with GetStringChars and would have dereferenced a null Java string had a toString override
     * returned null; an empty string is used for that case instead.
     */
    String u = javaStringValue(result.l);
    if (u.isNull())
        u = emptyString();
    return jsString(vm, u);
}

static JSValue numberValueForCharacter(wkj_ref obj) {

    // Since obj is a weak reference, taking a strong one to safeguard instance() from GC
    WKJHandle jlinstance = WKJHandle::retained(obj);

    if (!jlinstance) {
        LOG_ERROR("Could not get javaInstance for %llu in JavaInstance::numberValueForCharacter", static_cast<unsigned long long>(obj));
        return jsUndefined();
    }

    WKJJavaValue value = emptyJavaValue();
    javaUnbox(obj, JavaTypeChar, value);
    return jsNumber(value.i);
}

static JSValue numberValueForNumber(wkj_ref obj) {

    // Since obj is a weak reference, taking a strong one to safeguard instance() from GC
    WKJHandle jlinstance = WKJHandle::retained(obj);

    if (!jlinstance) {
        LOG_ERROR("Could not get javaInstance for %llu in JavaInstance::numberValueForNumber", static_cast<unsigned long long>(obj));
        return jsUndefined();
    }

    WKJJavaValue value = emptyJavaValue();
    javaUnbox(obj, JavaTypeDouble, value);
    return jsNumber(value.d);
}


JSValue JavaInstance::numberValue(JSGlobalObject*) const
{
    wkj_ref obj = m_instance->instance();
    // Since obj is a weak reference, taking a strong one to safeguard instance() from GC
    WKJHandle jlinstance = WKJHandle::retained(obj);

    if (!jlinstance) {
        LOG_ERROR("Could not get javaInstance for %llu in JavaInstance::numberValue", static_cast<unsigned long long>(obj));
        return jsUndefined();
    }

    JavaClass* aClass = static_cast<JavaClass*>(getClass());
    if (aClass->isCharacterClass())
        return numberValueForCharacter(obj);
    if (aClass->isBooleanClass()) {
        // The GCC workaround for JDK-8126601 - calling through the JavaType-taking
        // callJNIMethod rather than the template - has nothing left to work around: there is
        // one unbox slot and the type is a value it takes, not a template argument.
        WKJJavaValue value = emptyJavaValue();
        javaUnbox(obj, JavaTypeBoolean, value);
        return jsNumber(value.i);
    }
    return numberValueForNumber(obj);
}

JSValue JavaInstance::booleanValue() const
{
    // Since m_instance->instance() is a weak reference, taking a strong one to safeguard instance() from GC
    WKJHandle jlinstance = WKJHandle::retained(m_instance->instance());

    if (!jlinstance) {
        LOG_ERROR("Could not get javaInstance for %llu in JavaInstance::booleanValue", static_cast<unsigned long long>(m_instance->instance()));
        return jsUndefined();
    }

    WKJJavaValue value = emptyJavaValue();
    javaUnbox(m_instance->instance(), JavaTypeBoolean, value);
    return jsBoolean(value.i != 0);
}

class JavaRuntimeMethod : public RuntimeMethod {
public:
    typedef RuntimeMethod Base;

    static JavaRuntimeMethod* create(JSGlobalObject* lexicalGlobalObject, JSGlobalObject* globalObject, const String& name, Bindings::Method* method)
    {
        VM& vm = globalObject->vm();
        // FIXME: deprecatedGetDOMStructure uses the prototype off of the wrong global object
        // We need to pass in the right global object for "i".
        Structure* domStructure = WebCore::deprecatedGetDOMStructure<JavaRuntimeMethod>(lexicalGlobalObject);
        JavaRuntimeMethod* _method = new (NotNull, allocateCell<JavaRuntimeMethod>(vm)) JavaRuntimeMethod(vm, domStructure, method);
        _method->finishCreation(vm, name);
        return _method;
    }

    static Structure* createStructure(VM& vm, JSC::JSGlobalObject* globalObject, JSC::JSValue prototype)
    {
        return Structure::create(vm, globalObject, prototype, TypeInfo(InternalFunctionType, StructureFlags), &s_info);
    }

    static const ClassInfo s_info;

private:
    JavaRuntimeMethod(VM& vm, Structure* structure, Bindings::Method *method)
        : RuntimeMethod(vm, structure, method)
    {
    }

    void finishCreation(VM& vm, const String& name)
    {
        Base::finishCreation(vm, name);
        ASSERT(inherits(&s_info));
    }
};

const ClassInfo JavaRuntimeMethod::s_info = { "JavaRuntimeMethod"_s, &RuntimeMethod::s_info, nullptr, nullptr, CREATE_METHOD_TABLE(JavaRuntimeMethod) };

JSValue JavaInstance::getMethod(JSGlobalObject* lexicalGlobalObject, PropertyName propertyName)
{
    JavaClass* aClass = static_cast<JavaClass*>(getClass());
    Method *method = aClass->methodNamed(propertyName, this);
    return JavaRuntimeMethod::create(lexicalGlobalObject, lexicalGlobalObject, propertyName.publicName(), method);
}

JSValue JavaInstance::invokeMethod(JSGlobalObject* globalObject, CallFrame* callFrame, RuntimeMethod* runtimeMethod)
{
    VM& vm = globalObject->vm();
    auto scope = DECLARE_THROW_SCOPE(vm);

    ASSERT(globalObject->vm().apiLock().currentThreadIsHoldingLock());

    if (!asObject(runtimeMethod)->inherits(&JavaRuntimeMethod::s_info))
        throwException(globalObject, scope, createTypeError(globalObject, "Attempt to invoke non-Java method on Java object."_s));

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
#if !PLATFORM(JAVA)
        LOG(LiveConnect, "JavaInstance::invokeMethod unable to find an appropriate method");
#endif
        return jsUndefined();
    }

    const JavaMethod* jMethod = static_cast<const JavaMethod*>(method);
    // Since we can't convert java.lang.Character to any JS primitive, we have
    // to handle valueOf method call.
    wkj_ref obj = m_instance->instance();
    JavaClass* aClass = static_cast<JavaClass*>(getClass());
    if (aClass->isCharacterClass() && jMethod->name() == "valueOf"_s)
        return numberValueForCharacter(obj);

    // Since m_instance->instance() is a weak reference, taking a strong one to safeguard instance() from GC
    WKJHandle jlinstance = WKJHandle::retained(obj);

    if (!jlinstance) {
        LOG_ERROR("Could not get javaInstance for %llu in JavaInstance::invokeMethod", static_cast<unsigned long long>(obj));
        return jsUndefined();
    }
#if !PLATFORM(JAVA)
    LOG(LiveConnect, "JavaInstance::invokeMethod call %s %s on %llu", String(jMethod->name().impl()).utf8().data(), jMethod->signature(), static_cast<unsigned long long>(m_instance->instance()));
#endif

    const int count = callFrame->argumentCount();
    if (jMethod->numParameters() != count) {
#if !PLATFORM(JAVA)
        LOG(LiveConnect, "JavaInstance::invokeMethod unable to find an appropriate method with specified signature");
#endif
        return jsUndefined();
    }

    /*
     * The arguments, as Java objects. jArgs is what the invocation takes; argOwners holds
     * the one reference each of them needs to survive the call, which the JNI code got for
     * free from the local reference frame.
     */
    Vector<WKJHandle> argOwners(count);
    Vector<wkj_ref> jArgs(count);

    for (int i = 0; i < count; i++) {
        CString javaClassName = jMethod->parameterAt(i).utf8();
        JavaType jtype = javaTypeFromClassName(javaClassName.data());
        WKJJavaValue jarg = convertValueToJValue(globalObject, m_rootObject.get(),
            callFrame->argument(i), jtype, javaClassName.data());
        JavaValueScope jargScope(jarg);
        argOwners[i] = javaValueToObject(jarg, jtype);
        jArgs[i] = argOwners[i].get();
#if !PLATFORM(JAVA)
        LOG(LiveConnect, "JavaInstance::invokeMethod arg[%d] = %s", i, callFrame->argument(i).toString(globalObject)->value(globalObject).ascii().data());
#endif
    }

    WKJJavaValue result = emptyJavaValue();
    JavaValueScope resultScope(result);

    // Try to use the JNI abstraction first, otherwise fall back to
    // normal JNI.  The JNI dispatch abstraction allows the Java plugin
    // to dispatch the call on the appropriate internal VM thread.
    RootObject* rootObject = this->rootObject();
    if (jMethod->isStatic())
        return throwException(globalObject, scope, createTypeError(globalObject, "invoking static method"_s));
    if (!rootObject)
        return jsUndefined();

    // bool handled = false;
    if (rootObject->nativeHandle()) {
        wkj_ref obj = m_instance->instance();
        // Since m_instance->instance() is a weak reference, taking a strong one to safeguard instance() from GC
        WKJHandle jlinstance = WKJHandle::retained(obj);

        if (!jlinstance) {
            LOG_ERROR("Could not get javaInstance for %llu in JavaInstance::invokeMethod", static_cast<unsigned long long>(obj));
            return jsUndefined();
        }

        // const char *callingURL = 0; // FIXME, need to propagate calling URL to Java
        /*
         * The method is still looked up by name and JNI descriptor rather than kept from the
         * enumeration that produced this JavaMethod. That is deliberate: the search has to
         * land on the same java.lang.reflect.Method that GetMethodID + ToReflectedMethod
         * produced, because com.sun.webkit.Utilities.fwkInvokeWithContext makes its security
         * decision on method.getDeclaringClass().
         */
        WKJHandle methodId = javaResolveMethod(obj, jMethod->name(),
            String::fromUTF8(jMethod->signature()));

        WKJHandle ex = dispatchJavaCall(callFrame->argumentCount(), rootObject,
                                        obj, jMethod->returnType(), methodId.get(),
                                        jArgs.span().data(), result,
                                        accessControlContext());
        if (ex) {
            JSValue exceptionDescription
              = (JavaInstance::create(ex.get(), rootObject, accessControlContext())
                 ->createRuntimeObject(globalObject));
            throwException(globalObject, scope, exceptionDescription);
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
    // Since we can't convert java.lang.Character to any JS primitive, we have
    // to treat it as JS foreign object.
    case JavaTypeChar:
        {
            resultValue = toJS(globalObject, WebCore::Java_Object_to_JSValue(toRef(globalObject), rootObject, result.l, accessControlContext()));
        }
        break;

    case JavaTypeBoolean:
        {
            resultValue = jsBoolean(result.i != 0);
        }
        break;

    case JavaTypeByte:
        {
            resultValue = jsNumber(result.i);
        }
        break;

    case JavaTypeShort:
        {
            resultValue = jsNumber(result.i);
        }
        break;

    case JavaTypeInt:
        {
            resultValue = jsNumber(result.i);
        }
        break;

    case JavaTypeLong:
        {
            resultValue = jsNumber(static_cast<double>(result.j));
        }
        break;

    case JavaTypeFloat:
        {
            resultValue = jsNumber(static_cast<double>(static_cast<float>(result.d)));
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

JSValue JavaInstance::defaultValue(JSGlobalObject* globalObject, PreferredPrimitiveType hint) const
{
    if (hint == PreferString)
        return stringValue(globalObject);
    if (hint == PreferNumber)
        return numberValue(globalObject);

    JavaClass* aClass = static_cast<JavaClass*>(getClass());
    if (aClass->isStringClass())
        return stringValue(globalObject);

    wkj_ref obj = m_instance->instance();
    // Since m_instance->instance() is a weak reference, taking a strong one to safeguard instance() from GC
    WKJHandle jlinstance = WKJHandle::retained(obj);

    if (!jlinstance) {
        LOG_ERROR("Could not get javaInstance for %llu in JavaInstance::defaultValue", static_cast<unsigned long long>(obj));
        return jsUndefined();
    }

    if (aClass->isNumberClass())
        return numberValueForNumber(m_instance->instance());
    if (aClass->isBooleanClass())
        return booleanValue();
    return valueOf(globalObject);
}

JSValue JavaInstance::valueOf(JSGlobalObject* globalObject) const
{
    return stringValue(globalObject);
}

#endif // ENABLE(JAVA_BRIDGE)
