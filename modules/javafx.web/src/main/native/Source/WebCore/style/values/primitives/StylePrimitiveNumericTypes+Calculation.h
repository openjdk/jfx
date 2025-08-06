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

#include "CalculationValue.h"
#include "StylePrimitiveNumericTypes.h"
#include <wtf/Forward.h>

namespace WebCore {
namespace Style {

// MARK: - Conversion to `Calculation::Child`.

inline Calculation::Child copyCalculation(Ref<CalculationValue> value)
{
    return value->copyRoot();
}

inline Calculation::Child copyCalculation(Calc auto const& value)
{
    return value.protectedCalculation()->copyRoot();
}

template<auto R, typename V> Calculation::Child copyCalculation(const Number<R, V>& value)
{
    return Calculation::number(value.value);
}

template<auto R, typename V> Calculation::Child copyCalculation(const Percentage<R, V>& value)
{
    return Calculation::percentage(value.value);
}

inline Calculation::Child copyCalculation(Numeric auto const& value)
{
    return Calculation::dimension(value.value);
}

inline Calculation::Child copyCalculation(DimensionPercentageNumeric auto const& value)
{
    return WTF::switchOn(value, [](const auto& value) { return copyCalculation(value); });
}

} // namespace Style
} // namespace WebCore
