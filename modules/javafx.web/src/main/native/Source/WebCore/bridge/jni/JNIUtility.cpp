/*
 * Copyright (C) 2003, 2004, 2005, 2007, 2008, 2009, 2010 Apple Inc. All rights reserved.
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
#include "JNIUtility.h"

#if ENABLE(JAVA_BRIDGE)

#include <cstring>
#include <wtf/Vector.h>

namespace JSC {

namespace Bindings {

/*
 * The C ABI carries a Java type as an int32_t, and it carries the values of JavaType so that
 * this code can pass its existing JavaType straight through. JavaType.h warns that the order
 * of its enumerators must not change; these bind that warning to a compile error.
 */
static_assert(static_cast<int32_t>(JavaTypeInvalid) == WKJ_JT_INVALID, "JavaType drifted");
static_assert(static_cast<int32_t>(JavaTypeVoid) == WKJ_JT_VOID, "JavaType drifted");
static_assert(static_cast<int32_t>(JavaTypeObject) == WKJ_JT_OBJECT, "JavaType drifted");
static_assert(static_cast<int32_t>(JavaTypeBoolean) == WKJ_JT_BOOLEAN, "JavaType drifted");
static_assert(static_cast<int32_t>(JavaTypeByte) == WKJ_JT_BYTE, "JavaType drifted");
static_assert(static_cast<int32_t>(JavaTypeChar) == WKJ_JT_CHAR, "JavaType drifted");
static_assert(static_cast<int32_t>(JavaTypeShort) == WKJ_JT_SHORT, "JavaType drifted");
static_assert(static_cast<int32_t>(JavaTypeInt) == WKJ_JT_INT, "JavaType drifted");
static_assert(static_cast<int32_t>(JavaTypeLong) == WKJ_JT_LONG, "JavaType drifted");
static_assert(static_cast<int32_t>(JavaTypeFloat) == WKJ_JT_FLOAT, "JavaType drifted");
static_assert(static_cast<int32_t>(JavaTypeDouble) == WKJ_JT_DOUBLE, "JavaType drifted");
static_assert(static_cast<int32_t>(JavaTypeArray) == WKJ_JT_ARRAY, "JavaType drifted");

/*
 * Every call below starts here. A table that was never installed behaves exactly like a
 * table of NULL slots, which in turn behaves like the JNI code did when it could not find
 * the class or the method: nothing happens and the caller gets a zero.
 */
static const WKJLiveConnectHost* host()
{
    return wkj_live_connect_host;
}

/*
 * Runs a WKJ_STR_* producing callback and returns its result as a WTF::String, growing the
 * buffer once if the first attempt did not fit. A null String means the Java value was null
 * or the slot was missing; that distinction is what the "<Unknown>" substitutions in
 * JavaClass, JavaField and JavaMethod are keyed on.
 *
 * The inline buffer is sized for what actually travels here: class names, method names and
 * field names. A name longer than that costs one extra call and no correctness.
 */
template<typename Fetch> static WTF::String fetchString(Fetch&& fetch)
{
    char16_t inlineBuffer[128];
    int32_t length = 0;

    int32_t status = fetch(reinterpret_cast<uint16_t*>(inlineBuffer),
        static_cast<int32_t>(sizeof(inlineBuffer) / sizeof(inlineBuffer[0])), &length);
    if (status == WKJ_STR_OK)
        return WTF::String(std::span<const char16_t>(inlineBuffer, static_cast<size_t>(length)));
    if (status != WKJ_STR_OVERFLOW || length <= 0)
        return WTF::String();

    Vector<char16_t> buffer(static_cast<size_t>(length));
    status = fetch(reinterpret_cast<uint16_t*>(buffer.mutableSpan().data()), length, &length);
    if (status != WKJ_STR_OK || length <= 0)
        return WTF::String();

    return WTF::String(std::span<const char16_t>(buffer.span().data(), static_cast<size_t>(length)));
}

/*
 * A WTF::String as the (pointer, length) pair the ABI takes. A null String is passed as a
 * null pointer. The characters are only valid for the duration of the call, which is all
 * any slot is allowed to keep them for.
 *
 * A Latin-1 String has to be widened rather than reinterpreted: StringImpl::span16() asserts
 * !is8Bit() and, with the assert compiled out, reads past the end of the allocation.
 */
class UTF16Argument {
    WTF_MAKE_NONCOPYABLE(UTF16Argument);
public:
    explicit UTF16Argument(const WTF::String& value)
    {
        if (value.isNull())
            return;

        m_length = static_cast<int32_t>(value.length());
        if (!value.is8Bit()) {
            m_data = reinterpret_cast<const uint16_t*>(value.span16().data());
            return;
        }

        auto characters = value.span8();
        m_widened.grow(static_cast<size_t>(m_length));
        for (int32_t i = 0; i < m_length; ++i)
            m_widened[static_cast<size_t>(i)] = characters[static_cast<size_t>(i)];
        m_data = reinterpret_cast<const uint16_t*>(m_widened.span().data());
    }

    const uint16_t* data() const { return m_data; }
    int32_t length() const { return m_length; }

private:
    const uint16_t* m_data { nullptr };
    int32_t m_length { 0 };
    Vector<char16_t> m_widened;
};

WKJJavaValue emptyJavaValue()
{
    WKJJavaValue value;
    std::memset(&value, 0, sizeof(value));
    value.type = WKJ_JT_INVALID;
    return value;
}

/* --- java.lang.Object and java.lang.Class -------------------------------------------- */

WKJHandle javaObjectClass(wkj_ref object)
{
    if (!object || !host() || !host()->object_get_class)
        return WKJHandle();
    return WKJHandle(host()->object_get_class(object));
}

WTF::String javaClassName(wkj_ref javaClass)
{
    if (!javaClass || !host() || !host()->class_get_name)
        return WTF::String();

    return fetchString([&](uint16_t* buffer, int32_t capacity, int32_t* length) {
        return host()->class_get_name(javaClass, buffer, capacity, length);
    });
}

bool javaClassIsArray(wkj_ref javaClass)
{
    if (!javaClass || !host() || !host()->class_is_array)
        return false;
    return host()->class_is_array(javaClass) != 0;
}

WKJHandle javaCreateDummyObject()
{
    if (!host() || !host()->create_dummy_object)
        return WKJHandle();
    return WKJHandle(host()->create_dummy_object());
}

/* --- java.lang.reflect.Method -------------------------------------------------------- */

WKJHandle javaResolveMethod(wkj_ref object, const WTF::String& name, const WTF::String& signature)
{
    if (!object || !host() || !host()->resolve_method)
        return WKJHandle();

    UTF16Argument nameArgument(name);
    UTF16Argument signatureArgument(signature);
    return WKJHandle(host()->resolve_method(object, nameArgument.data(), nameArgument.length(),
        signatureArgument.data(), signatureArgument.length()));
}

WKJHandle javaInvoke(wkj_ref method, wkj_ref instance, const wkj_ref* args, int argumentCount,
    wkj_ref accessControlContext, WKJHandle& exception)
{
    exception.clear();

    if (!method || !host() || !host()->invoke)
        return WKJHandle();

    wkj_ref thrown = 0;
    WKJHandle result(host()->invoke(method, instance, args, static_cast<int32_t>(argumentCount),
        accessControlContext, &thrown));
    exception = WKJHandle(thrown);
    return result;
}

WTF::String javaMethodName(wkj_ref method)
{
    if (!method || !host() || !host()->method_get_name)
        return WTF::String();

    return fetchString([&](uint16_t* buffer, int32_t capacity, int32_t* length) {
        return host()->method_get_name(method, buffer, capacity, length);
    });
}

WTF::String javaMethodReturnTypeName(wkj_ref method)
{
    if (!method || !host() || !host()->method_get_return_type_name)
        return WTF::String();

    return fetchString([&](uint16_t* buffer, int32_t capacity, int32_t* length) {
        return host()->method_get_return_type_name(method, buffer, capacity, length);
    });
}

int javaMethodParameterCount(wkj_ref method)
{
    if (!method || !host() || !host()->method_get_parameter_count)
        return 0;
    return host()->method_get_parameter_count(method);
}

WTF::String javaMethodParameterTypeName(wkj_ref method, int index)
{
    if (!method || !host() || !host()->method_get_parameter_type_name)
        return WTF::String();

    return fetchString([&](uint16_t* buffer, int32_t capacity, int32_t* length) {
        return host()->method_get_parameter_type_name(method, static_cast<int32_t>(index),
            buffer, capacity, length);
    });
}

int javaMethodModifiers(wkj_ref method)
{
    if (!method || !host() || !host()->method_get_modifiers)
        return 0;
    return host()->method_get_modifiers(method);
}

/* --- java.lang.reflect.Field --------------------------------------------------------- */

WTF::String javaFieldName(wkj_ref field)
{
    if (!field || !host() || !host()->field_get_name)
        return WTF::String();

    return fetchString([&](uint16_t* buffer, int32_t capacity, int32_t* length) {
        return host()->field_get_name(field, buffer, capacity, length);
    });
}

WTF::String javaFieldTypeName(wkj_ref field)
{
    if (!field || !host() || !host()->field_get_type_name)
        return WTF::String();

    return fetchString([&](uint16_t* buffer, int32_t capacity, int32_t* length) {
        return host()->field_get_type_name(field, buffer, capacity, length);
    });
}

bool javaFieldGet(wkj_ref field, wkj_ref instance, JavaType type, WKJJavaValue& result)
{
    result = emptyJavaValue();
    if (!field || !host() || !host()->field_get)
        return false;
    return host()->field_get(field, instance, static_cast<int32_t>(type), &result) != 0;
}

bool javaFieldSet(wkj_ref field, wkj_ref instance, JavaType type, const WKJJavaValue& value)
{
    if (!field || !host() || !host()->field_set)
        return false;
    return host()->field_set(field, instance, static_cast<int32_t>(type), &value) != 0;
}

/* --- Java arrays --------------------------------------------------------------------- */

int javaArrayLength(wkj_ref array)
{
    if (!array || !host() || !host()->array_length)
        return 0;
    return host()->array_length(array);
}

bool javaArrayGet(wkj_ref array, int index, JavaType type, WKJJavaValue& result)
{
    result = emptyJavaValue();
    if (!array || !host() || !host()->array_get)
        return false;
    return host()->array_get(array, static_cast<int32_t>(index), static_cast<int32_t>(type),
        &result) != 0;
}

bool javaArraySet(wkj_ref array, int index, JavaType type, const WKJJavaValue& value)
{
    if (!array || !host() || !host()->array_set)
        return false;
    return host()->array_set(array, static_cast<int32_t>(index), static_cast<int32_t>(type),
        &value) != 0;
}

/* --- boxing, unboxing and strings ---------------------------------------------------- */

WKJHandle javaBox(const WKJJavaValue& value)
{
    if (!host() || !host()->box)
        return WKJHandle();
    return WKJHandle(host()->box(&value));
}

bool javaUnbox(wkj_ref boxed, JavaType type, WKJJavaValue& result)
{
    result = emptyJavaValue();
    if (!boxed || !host() || !host()->unbox)
        return false;
    return host()->unbox(boxed, static_cast<int32_t>(type), &result) != 0;
}

WKJHandle javaBoxString(const WTF::String& value)
{
    if (value.isNull() || !host() || !host()->box_string)
        return WKJHandle();

    UTF16Argument argument(value);
    return WKJHandle(host()->box_string(argument.data(), argument.length()));
}

WTF::String javaStringValue(wkj_ref string)
{
    if (!string || !host() || !host()->string_value)
        return WTF::String();

    return fetchString([&](uint16_t* buffer, int32_t capacity, int32_t* length) {
        return host()->string_value(string, buffer, capacity, length);
    });
}

/* --- the three LiveConnect objects --------------------------------------------------- */

WKJHandle javaUndefinedObject()
{
    /*
     * The JNI code cached the JSObject.UNDEFINED field in a function-local static global
     * resolved on first use and never released. The same shape, with an id: one reference
     * held for the life of the process, and a fresh reference handed to each caller so that
     * "the receiver owns what it is given" holds here too.
     */
    static WKJHandle undefined;
    if (!undefined && host() && host()->undefined_object)
        undefined = WKJHandle(host()->undefined_object());
    return WKJHandle::retained(undefined.get());
}

WKJHandle javaJSObjectCreate(int64_t peer, int32_t peerType)
{
    if (!host() || !host()->jsobject_create)
        return WKJHandle();
    return WKJHandle(host()->jsobject_create(peer, peerType));
}

WKJHandle javaNodeCachedImpl(int64_t nodePeer)
{
    if (!host() || !host()->node_get_cached_impl)
        return WKJHandle();
    return WKJHandle(host()->node_get_cached_impl(nodePeer));
}

/* --- type names, unchanged ------------------------------------------------------------ */

JavaType javaTypeFromClassName(const char* name)
{
    JavaType type;

    if (!strcmp("byte", name))
        type = JavaTypeByte;
    else if (!strcmp("short", name))
        type = JavaTypeShort;
    else if (!strcmp("int", name))
        type = JavaTypeInt;
    else if (!strcmp("long", name))
        type = JavaTypeLong;
    else if (!strcmp("float", name))
        type = JavaTypeFloat;
    else if (!strcmp("double", name))
        type = JavaTypeDouble;
    else if (!strcmp("char", name))
        type = JavaTypeChar;
    else if (!strcmp("boolean", name))
        type = JavaTypeBoolean;
    else if (!strcmp("void", name))
        type = JavaTypeVoid;
    else if ('[' == name[0])
        type = JavaTypeArray;
    else
        type = JavaTypeObject;

    return type;
}

const char* signatureFromJavaType(JavaType type)
{
    switch (type) {
    case JavaTypeVoid:
        return "V";

    case JavaTypeArray:
        return "[";

    case JavaTypeObject:
        return "L";

    case JavaTypeBoolean:
        return "Z";

    case JavaTypeByte:
        return "B";

    case JavaTypeChar:
        return "C";

    case JavaTypeShort:
        return "S";

    case JavaTypeInt:
        return "I";

    case JavaTypeLong:
        return "J";

    case JavaTypeFloat:
        return "F";

    case JavaTypeDouble:
        return "D";

    case JavaTypeInvalid:
    default:
        break;
    }
    return "";
}

JavaType javaTypeFromPrimitiveType(char type)
{
    switch (type) {
    case 'V':
        return JavaTypeVoid;

    case 'L':
        return JavaTypeObject;

    case '[':
        return JavaTypeArray;

    case 'Z':
        return JavaTypeBoolean;

    case 'B':
        return JavaTypeByte;

    case 'C':
        return JavaTypeChar;

    case 'S':
        return JavaTypeShort;

    case 'I':
        return JavaTypeInt;

    case 'J':
        return JavaTypeLong;

    case 'F':
        return JavaTypeFloat;

    case 'D':
        return JavaTypeDouble;

    default:
        break;
    }
    return JavaTypeInvalid;
}

} // namespace Bindings

} // namespace JSC

#endif // ENABLE(JAVA_BRIDGE)
