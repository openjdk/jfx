/*
 * Copyright (C) 2024 Samuel Weinig <sam@webkit.org>
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
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "CSSPrimitiveNumericTypes.h"
#include "CSSPrimitiveValue.h"
#include "CSSPrimitiveValueMappings.h"
#include "CSSValuePair.h"
#include "CSSValuePool.h"

namespace WebCore {
namespace CSS {

// MARK: - Conversion from strongly typed `CSS::` value types to `WebCore::CSSValue` types.

template<typename CSSType> struct CSSValueCreation;

template<typename CSSType> Ref<CSSValue> createCSSValue(const CSSType& value)
{
    return CSSValueCreation<CSSType>{}(value);
}

template<CSSValueID Id> struct CSSValueCreation<Constant<Id>> {
    Ref<CSSValue> operator()(const Constant<Id>&)
    {
        return CSSPrimitiveValue::create(Id);
    }
};

template<VariantLike CSSType> struct CSSValueCreation<CSSType> {
    Ref<CSSValue> operator()(const CSSType& value)
    {
        return WTF::switchOn(value, [](const auto& alternative) { return createCSSValue(alternative); });
    }
};

template<TupleLike CSSType> requires (std::tuple_size_v<CSSType> == 1) struct CSSValueCreation<CSSType> {
    Ref<CSSValue> operator()(const CSSType& value)
    {
        return createCSSValue(get<0>(value));;
    }
};

template<NumericRaw CSSType> struct CSSValueCreation<CSSType> {
    Ref<CSSValue> operator()(const CSSType& raw)
    {
        return CSSPrimitiveValue::create(raw.value, toCSSUnitType(raw.unit));
    }
};

template<Calc CSSType> struct CSSValueCreation<CSSType> {
    Ref<CSSValue> operator()(const CSSType& calc)
    {
        return CSSPrimitiveValue::create(calc.protectedCalc());
    }
};

template<typename CSSType> struct CSSValueCreation<SpaceSeparatedPoint<CSSType>> {
    Ref<CSSValue> operator()(const SpaceSeparatedPoint<CSSType>& value)
    {
        return CSSValuePair::create(
            createCSSValue(value.x()),
            createCSSValue(value.y())
        );
    }
};

template<typename CSSType> struct CSSValueCreation<SpaceSeparatedSize<CSSType>> {
    Ref<CSSValue> operator()(const SpaceSeparatedSize<CSSType>& value)
    {
        return CSSValuePair::create(
            createCSSValue(value.width()),
            createCSSValue(value.height())
        );
    }
};

} // namespace CSS
} // namespace WebCore
