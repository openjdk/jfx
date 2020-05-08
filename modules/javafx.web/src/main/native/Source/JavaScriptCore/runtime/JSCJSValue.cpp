/*
 *  Copyright (C) 1999-2001 Harri Porten (porten@kde.org)
 *  Copyright (C) 2001 Peter Kelly (pmk@post.com)
 *  Copyright (C) 2003-2019 Apple Inc. All rights reserved.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Library General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Library General Public License for more details.
 *
 *  You should have received a copy of the GNU Library General Public License
 *  along with this library; see the file COPYING.LIB.  If not, write to
 *  the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 *  Boston, MA 02110-1301, USA.
 *
 */

#include "config.h"
#include "JSCJSValue.h"

#include "BooleanConstructor.h"
#include "BooleanPrototype.h"
#include "CustomGetterSetter.h"
#include "Error.h"
#include "ExceptionHelpers.h"
#include "GetterSetter.h"
#include "JSBigInt.h"
#include "JSCInlines.h"
#include "JSFunction.h"
#include "JSGlobalObject.h"
#include "NumberObject.h"
#include <wtf/MathExtras.h>

namespace JSC {

// ECMA 9.4
double JSValue::toInteger(ExecState* exec) const
{
    if (isInt32())
        return asInt32();
    double d = toNumber(exec);
    return std::isnan(d) ? 0.0 : trunc(d);
}

double JSValue::toIntegerPreserveNaN(ExecState* exec) const
{
    if (isInt32())
        return asInt32();
    return trunc(toNumber(exec));
}

double JSValue::toLength(ExecState* exec) const
{
    // ECMA 7.1.15
    // http://www.ecma-international.org/ecma-262/6.0/#sec-tolength
    double d = toInteger(exec);
    if (d <= 0)
        return 0.0;
    if (std::isinf(d))
        return maxSafeInteger();
    return std::min(d, maxSafeInteger());
}

double JSValue::toNumberSlowCase(ExecState* exec) const
{
    ASSERT(!isInt32() && !isDouble());
    if (isCell())
        return asCell()->toNumber(exec);
    if (isTrue())
        return 1.0;
    return isUndefined() ? PNaN : 0; // null and false both convert to 0.
}

Optional<double> JSValue::toNumberFromPrimitive() const
{
    if (isEmpty())
        return WTF::nullopt;
    if (isNumber())
        return asNumber();
    if (isBoolean())
        return asBoolean();
    if (isUndefined())
        return PNaN;
    if (isNull())
        return 0;
    return WTF::nullopt;
}

JSObject* JSValue::toObjectSlowCase(ExecState* exec, JSGlobalObject* globalObject) const
{
    VM& vm = exec->vm();
    auto scope = DECLARE_THROW_SCOPE(vm);
    ASSERT(!isCell());

    if (isInt32() || isDouble())
        return constructNumber(exec, globalObject, asValue());
    if (isTrue() || isFalse())
        return constructBooleanFromImmediateBoolean(exec, globalObject, asValue());

    ASSERT(isUndefinedOrNull());
    throwException(exec, scope, createNotAnObjectError(exec, *this));
    return nullptr;
}

JSValue JSValue::toThisSlowCase(ExecState* exec, ECMAMode ecmaMode) const
{
    ASSERT(!isCell());

    if (ecmaMode == StrictMode)
        return *this;

    if (isInt32() || isDouble())
        return constructNumber(exec, exec->lexicalGlobalObject(), asValue());
    if (isTrue() || isFalse())
        return constructBooleanFromImmediateBoolean(exec, exec->lexicalGlobalObject(), asValue());
    ASSERT(isUndefinedOrNull());
    return exec->globalThisValue();
}

JSObject* JSValue::synthesizePrototype(ExecState* exec) const
{
    VM& vm = exec->vm();
    auto scope = DECLARE_THROW_SCOPE(vm);

    if (isCell()) {
        if (isString())
            return exec->lexicalGlobalObject()->stringPrototype();
        if (isBigInt())
            return exec->lexicalGlobalObject()->bigIntPrototype();
        ASSERT(isSymbol());
        return exec->lexicalGlobalObject()->symbolPrototype();
    }

    if (isNumber())
        return exec->lexicalGlobalObject()->numberPrototype();
    if (isBoolean())
        return exec->lexicalGlobalObject()->booleanPrototype();

    ASSERT(isUndefinedOrNull());
    throwException(exec, scope, createNotAnObjectError(exec, *this));
    return nullptr;
}

// ECMA 8.7.2
bool JSValue::putToPrimitive(ExecState* exec, PropertyName propertyName, JSValue value, PutPropertySlot& slot)
{
    VM& vm = exec->vm();
    auto scope = DECLARE_THROW_SCOPE(vm);

    if (Optional<uint32_t> index = parseIndex(propertyName))
        RELEASE_AND_RETURN(scope, putToPrimitiveByIndex(exec, index.value(), value, slot.isStrictMode()));

    // Check if there are any setters or getters in the prototype chain
    JSObject* obj = synthesizePrototype(exec);
    EXCEPTION_ASSERT(!!scope.exception() == !obj);
    if (UNLIKELY(!obj))
        return false;
    JSValue prototype;
    if (propertyName != vm.propertyNames->underscoreProto) {
        while (true) {
            Structure* structure = obj->structure(vm);
            if (structure->hasReadOnlyOrGetterSetterPropertiesExcludingProto() || structure->typeInfo().hasPutPropertySecurityCheck())
                break;
            prototype = obj->getPrototype(vm, exec);
            RETURN_IF_EXCEPTION(scope, false);

            if (prototype.isNull())
                return typeError(exec, scope, slot.isStrictMode(), ReadonlyPropertyWriteError);
            obj = asObject(prototype);
        }
    }

    for (; ; obj = asObject(prototype)) {
        Structure* structure = obj->structure(vm);
        if (UNLIKELY(structure->typeInfo().hasPutPropertySecurityCheck())) {
            obj->methodTable(vm)->doPutPropertySecurityCheck(obj, exec, propertyName, slot);
            RETURN_IF_EXCEPTION(scope, false);
        }
        unsigned attributes;
        PropertyOffset offset = structure->get(vm, propertyName, attributes);
        if (offset != invalidOffset) {
            if (attributes & PropertyAttribute::ReadOnly)
                return typeError(exec, scope, slot.isStrictMode(), ReadonlyPropertyWriteError);

            JSValue gs = obj->getDirect(offset);
            if (gs.isGetterSetter())
                RELEASE_AND_RETURN(scope, callSetter(exec, *this, gs, value, slot.isStrictMode() ? StrictMode : NotStrictMode));

            if (gs.isCustomGetterSetter())
                return callCustomSetter(exec, gs, attributes & PropertyAttribute::CustomAccessor, obj, slot.thisValue(), value);

            // If there's an existing property on the object or one of its
            // prototypes it should be replaced, so break here.
            break;
        }

        prototype = obj->getPrototype(vm, exec);
        RETURN_IF_EXCEPTION(scope, false);
        if (prototype.isNull())
            break;
    }

    return typeError(exec, scope, slot.isStrictMode(), ReadonlyPropertyWriteError);
}

bool JSValue::putToPrimitiveByIndex(ExecState* exec, unsigned propertyName, JSValue value, bool shouldThrow)
{
    VM& vm = exec->vm();
    auto scope = DECLARE_THROW_SCOPE(vm);

    if (propertyName > MAX_ARRAY_INDEX) {
        PutPropertySlot slot(*this, shouldThrow);
        return putToPrimitive(exec, Identifier::from(vm, propertyName), value, slot);
    }

    JSObject* prototype = synthesizePrototype(exec);
    EXCEPTION_ASSERT(!!scope.exception() == !prototype);
    if (UNLIKELY(!prototype))
        return false;
    bool putResult = false;
    bool success = prototype->attemptToInterceptPutByIndexOnHoleForPrototype(exec, *this, propertyName, value, shouldThrow, putResult);
    RETURN_IF_EXCEPTION(scope, false);
    if (success)
        return putResult;

    return typeError(exec, scope, shouldThrow, ReadonlyPropertyWriteError);
}

void JSValue::dump(PrintStream& out) const
{
    dumpInContext(out, 0);
}

void JSValue::dumpInContext(PrintStream& out, DumpContext* context) const
{
    dumpInContextAssumingStructure(
        out, context, (!!*this && isCell()) ? asCell()->structure() : nullptr);
}

void JSValue::dumpInContextAssumingStructure(
    PrintStream& out, DumpContext* context, Structure* structure) const
{
    if (!*this)
        out.print("<JSValue()>");
    else if (isInt32())
        out.printf("Int32: %d", asInt32());
    else if (isDouble()) {
#if USE(JSVALUE64)
        out.printf("Double: %lld, %lf", (long long)reinterpretDoubleToInt64(asDouble()), asDouble());
#else
        union {
            double asDouble;
            uint32_t asTwoInt32s[2];
        } u;
        u.asDouble = asDouble();
        out.printf("Double: %08x:%08x, %lf", u.asTwoInt32s[1], u.asTwoInt32s[0], asDouble());
#endif
    } else if (isCell()) {
        if (structure->classInfo()->isSubClassOf(JSString::info())) {
            JSString* string = asString(asCell());
            out.print("String");
            if (string->isRope())
                out.print(" (rope)");
            const StringImpl* impl = string->tryGetValueImpl();
            if (impl) {
                if (impl->isAtom())
                    out.print(" (atomic)");
                if (impl->isAtom())
                    out.print(" (identifier)");
                if (impl->isSymbol())
                    out.print(" (symbol)");
            } else
                out.print(" (unresolved)");
            out.print(": ", impl);
        } else if (structure->classInfo()->isSubClassOf(RegExp::info()))
            out.print("RegExp: ", *jsCast<RegExp*>(asCell()));
        else if (structure->classInfo()->isSubClassOf(Symbol::info()))
            out.print("Symbol: ", RawPointer(asCell()));
        else if (structure->classInfo()->isSubClassOf(Structure::info()))
            out.print("Structure: ", inContext(*jsCast<Structure*>(asCell()), context));
        else if (structure->classInfo()->isSubClassOf(JSObject::info())) {
            out.print("Object: ", RawPointer(asCell()));
            out.print(" with butterfly ", RawPointer(asObject(asCell())->butterfly()));
            out.print(" (Structure ", inContext(*structure, context), ")");
        } else {
            out.print("Cell: ", RawPointer(asCell()));
            out.print(" (", inContext(*structure, context), ")");
        }
#if USE(JSVALUE64)
        out.print(", StructureID: ", asCell()->structureID());
#endif
    } else if (isTrue())
        out.print("True");
    else if (isFalse())
        out.print("False");
    else if (isNull())
        out.print("Null");
    else if (isUndefined())
        out.print("Undefined");
    else
        out.print("INVALID");
}

void JSValue::dumpForBacktrace(PrintStream& out) const
{
    if (!*this)
        out.print("<JSValue()>");
    else if (isInt32())
        out.printf("%d", asInt32());
    else if (isDouble())
        out.printf("%lf", asDouble());
    else if (isCell()) {
        VM& vm = asCell()->vm();
        if (asCell()->inherits<JSString>(vm)) {
            JSString* string = asString(asCell());
            const StringImpl* impl = string->tryGetValueImpl();
            if (impl)
                out.print("\"", impl, "\"");
            else
                out.print("(unresolved string)");
        } else if (asCell()->inherits<Structure>(vm)) {
            out.print("Structure[ ", asCell()->structure()->classInfo()->className);
#if USE(JSVALUE64)
            out.print(" ID: ", asCell()->structureID());
#endif
            out.print("]: ", RawPointer(asCell()));
        } else {
            out.print("Cell[", asCell()->structure()->classInfo()->className);
#if USE(JSVALUE64)
            out.print(" ID: ", asCell()->structureID());
#endif
            out.print("]: ", RawPointer(asCell()));
        }
    } else if (isTrue())
        out.print("True");
    else if (isFalse())
        out.print("False");
    else if (isNull())
        out.print("Null");
    else if (isUndefined())
        out.print("Undefined");
    else
        out.print("INVALID");
}

bool JSValue::isValidCallee()
{
    return asObject(asCell())->globalObject();
}

JSString* JSValue::toStringSlowCase(ExecState* exec, bool returnEmptyStringOnError) const
{
    VM& vm = exec->vm();
    auto scope = DECLARE_THROW_SCOPE(vm);

    auto errorValue = [&] () -> JSString* {
        if (returnEmptyStringOnError)
            return jsEmptyString(vm);
        return nullptr;
    };

    ASSERT(!isString());
    if (isInt32()) {
        auto integer = asInt32();
        if (static_cast<unsigned>(integer) <= 9)
            return vm.smallStrings.singleCharacterString(integer + '0');
        return jsNontrivialString(vm, vm.numericStrings.add(integer));
    }
    if (isDouble())
        return jsString(vm, vm.numericStrings.add(asDouble()));
    if (isTrue())
        return vm.smallStrings.trueString();
    if (isFalse())
        return vm.smallStrings.falseString();
    if (isNull())
        return vm.smallStrings.nullString();
    if (isUndefined())
        return vm.smallStrings.undefinedString();
    if (isSymbol()) {
        throwTypeError(exec, scope, "Cannot convert a symbol to a string"_s);
        return errorValue();
    }
    if (isBigInt()) {
        JSBigInt* bigInt = asBigInt(*this);
        if (auto digit = bigInt->singleDigitValueForString())
            return vm.smallStrings.singleCharacterString(*digit + '0');
        JSString* returnString = jsNontrivialString(vm, bigInt->toString(exec, 10));
        RETURN_IF_EXCEPTION(scope, errorValue());
        return returnString;
    }

    ASSERT(isCell());
    JSValue value = asCell()->toPrimitive(exec, PreferString);
    RETURN_IF_EXCEPTION(scope, errorValue());
    ASSERT(!value.isObject());
    JSString* result = value.toString(exec);
    RETURN_IF_EXCEPTION(scope, errorValue());
    return result;
}

String JSValue::toWTFStringSlowCase(ExecState* exec) const
{
    VM& vm = exec->vm();
    if (isInt32())
        return vm.numericStrings.add(asInt32());
    if (isDouble())
        return vm.numericStrings.add(asDouble());
    if (isTrue())
        return vm.propertyNames->trueKeyword.string();
    if (isFalse())
        return vm.propertyNames->falseKeyword.string();
    if (isNull())
        return vm.propertyNames->nullKeyword.string();
    if (isUndefined())
        return vm.propertyNames->undefinedKeyword.string();
    return toString(exec)->value(exec);
}

} // namespace JSC
