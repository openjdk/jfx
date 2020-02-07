/*
 * Copyright (C) 2016-2019 Apple Inc. All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "IDLTypes.h"
#include "JSDOMConvertStrings.h"
#include <JavaScriptCore/ObjectConstructor.h>

namespace WebCore {

namespace Detail {

template<typename IDLStringType>
struct IdentifierConverter;

template<> struct IdentifierConverter<IDLDOMString> {
    static String convert(JSC::ExecState&, const JSC::Identifier& identifier)
    {
        return identifier.string();
    }
};

template<> struct IdentifierConverter<IDLByteString> {
    static String convert(JSC::ExecState& state, const JSC::Identifier& identifier)
    {
        return identifierToByteString(state, identifier);
    }
};

template<> struct IdentifierConverter<IDLUSVString> {
    static String convert(JSC::ExecState& state, const JSC::Identifier& identifier)
    {
        return identifierToUSVString(state, identifier);
    }
};

}

template<typename K, typename V> struct Converter<IDLRecord<K, V>> : DefaultConverter<IDLRecord<K, V>> {
    using ReturnType = typename IDLRecord<K, V>::ImplementationType;
    using KeyType = typename K::ImplementationType;
    using ValueType = typename V::ImplementationType;

    static ReturnType convert(JSC::ExecState& state, JSC::JSValue value)
    {
        auto& vm = state.vm();
        auto scope = DECLARE_THROW_SCOPE(vm);

        // 1. Let result be a new empty instance of record<K, V>.
        // 2. If Type(O) is Undefined or Null, return result.
        if (value.isUndefinedOrNull())
            return { };

        // 3. If Type(O) is not Object, throw a TypeError.
        if (!value.isObject()) {
            throwTypeError(&state, scope);
            return { };
        }

        JSC::JSObject* object = JSC::asObject(value);

        ReturnType result;

        // 4. Let keys be ? O.[[OwnPropertyKeys]]().
        JSC::PropertyNameArray keys(vm, JSC::PropertyNameMode::Strings, JSC::PrivateSymbolMode::Exclude);
        object->methodTable(vm)->getOwnPropertyNames(object, &state, keys, JSC::EnumerationMode(JSC::DontEnumPropertiesMode::Include));

        RETURN_IF_EXCEPTION(scope, { });

        // 5. Repeat, for each element key of keys in List order:
        for (auto& key : keys) {
            // 1. Let desc be ? O.[[GetOwnProperty]](key).
            JSC::PropertyDescriptor descriptor;
            bool didGetDescriptor = object->getOwnPropertyDescriptor(&state, key, descriptor);
            RETURN_IF_EXCEPTION(scope, { });

            // 2. If desc is not undefined and desc.[[Enumerable]] is true:

            // It's necessary to filter enumerable here rather than using the default EnumerationMode,
            // to prevent an observable extra [[GetOwnProperty]] operation in the case of ProxyObject records.
            if (didGetDescriptor && descriptor.enumerable()) {
                // 1. Let typedKey be key converted to an IDL value of type K.
                auto typedKey = Detail::IdentifierConverter<K>::convert(state, key);
                RETURN_IF_EXCEPTION(scope, { });

                // 2. Let value be ? Get(O, key).
                auto subValue = object->get(&state, key);
                RETURN_IF_EXCEPTION(scope, { });

                // 3. Let typedValue be value converted to an IDL value of type V.
                auto typedValue = Converter<V>::convert(state, subValue);
                RETURN_IF_EXCEPTION(scope, { });

                // 4. If typedKey is already a key in result, set its value to typedValue.
                // Note: This can happen when O is a proxy object.
                // FIXME: Handle this case.

                // 5. Otherwise, append to result a mapping (typedKey, typedValue).
                result.append({ typedKey, typedValue });
            }
        }

        // 6. Return result.
        return result;
    }
};

template<typename K, typename V> struct JSConverter<IDLRecord<K, V>> {
    static constexpr bool needsState = true;
    static constexpr bool needsGlobalObject = true;

    template<typename MapType>
    static JSC::JSValue convert(JSC::ExecState& state, JSDOMGlobalObject& globalObject, const MapType& map)
    {
        auto& vm = state.vm();

        // 1. Let result be ! ObjectCreate(%ObjectPrototype%).
        auto result = constructEmptyObject(&state, globalObject.objectPrototype());

        // 2. Repeat, for each mapping (key, value) in D:
        for (const auto& keyValuePair : map) {
            // 1. Let esKey be key converted to an ECMAScript value.
            // Note, this step is not required, as we need the key to be
            // an Identifier, not a JSValue.

            // 2. Let esValue be value converted to an ECMAScript value.
            auto esValue = toJS<V>(state, globalObject, keyValuePair.value);

            // 3. Let created be ! CreateDataProperty(result, esKey, esValue).
            bool created = result->putDirect(vm, JSC::Identifier::fromString(vm, keyValuePair.key), esValue);

            // 4. Assert: created is true.
            ASSERT_UNUSED(created, created);
        }

        // 3. Return result.
        return result;
    }
};

} // namespace WebCore
