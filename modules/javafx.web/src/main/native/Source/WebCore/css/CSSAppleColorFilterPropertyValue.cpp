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

#include "config.h"
#include "CSSAppleColorFilterPropertyValue.h"

#include "CSSPrimitiveNumericTypes+CSSValueVisitation.h"
#include "CSSPrimitiveNumericTypes+Serialization.h"
#include "CSSValuePool.h"
#include "DeprecatedCSSOMFilterFunctionValue.h"
#include "DeprecatedCSSOMPrimitiveValue.h"
#include "DeprecatedCSSOMValueList.h"

namespace WebCore {

Ref<CSSAppleColorFilterPropertyValue> CSSAppleColorFilterPropertyValue::create(CSS::AppleColorFilterProperty filter)
{
    return adoptRef(*new CSSAppleColorFilterPropertyValue(WTFMove(filter)));
}

CSSAppleColorFilterPropertyValue::CSSAppleColorFilterPropertyValue(CSS::AppleColorFilterProperty filter)
    : CSSValue(ClassType::AppleColorFilterProperty)
    , m_filter(WTFMove(filter))
{
}

String CSSAppleColorFilterPropertyValue::customCSSText(const CSS::SerializationContext& context) const
{
    return CSS::serializationForCSS(context, m_filter);
}

bool CSSAppleColorFilterPropertyValue::equals(const CSSAppleColorFilterPropertyValue& other) const
{
    return m_filter == other.m_filter;
}

IterationStatus CSSAppleColorFilterPropertyValue::customVisitChildren(NOESCAPE const Function<IterationStatus(CSSValue&)>& func) const
{
    return CSS::visitCSSValueChildren(func, m_filter);
}

Ref<DeprecatedCSSOMValue> CSSAppleColorFilterPropertyValue::createDeprecatedCSSOMWrapper(CSSStyleDeclaration& owner) const
{
    return WTF::switchOn(m_filter,
        [&](CSS::Keyword::None) -> Ref<DeprecatedCSSOMValue> {
            return DeprecatedCSSOMPrimitiveValue::create(CSSPrimitiveValue::create(CSSValueNone), owner);
        },
        [&](const auto& list) -> Ref<DeprecatedCSSOMValue> {
            auto values = list.value.template map<Vector<Ref<DeprecatedCSSOMValue>, 4>>([&](const auto& value) {
                return WTF::switchOn(value,
                    [&](const auto& function) -> Ref<DeprecatedCSSOMValue> {
                        return DeprecatedCSSOMFilterFunctionValue::create(CSS::FilterFunction { function }, owner);
                    }
                );
            });

            return DeprecatedCSSOMValueList::create(WTFMove(values), CSSValue::SpaceSeparator, owner);
        }
    );
}

} // namespace WebCore
