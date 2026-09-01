/*
 * Copyright (C) 2024-2025 Samuel Weinig <sam@webkit.org>
 * Copyright (C) 2025 Apple Inc. All rights reserved.
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
#include "StyleDropShadowFunction.h"

#include "CSSDropShadowFunction.h"
#include "CSSFilterFunctionDescriptor.h"
#include "CSSPrimitiveValue.h"
#include "Document.h"
#include "DropShadowFilterOperationWithStyleColor.h"
#include "FilterOperation.h"
#include "RenderStyle+GettersInlines.h"
#include "RenderStyle.h"
#include "StyleColor.h"
#include "StylePrimitiveNumericTypes+Conversions.h"
#include "StylePrimitiveNumericTypes+Evaluation.h"

namespace WebCore {
namespace Style {

CSS::DropShadow toCSSDropShadow(Ref<DropShadowFilterOperationWithStyleColor> operation, const RenderStyle& style)
{
    return {
        .color = toCSS(operation->styleColor(), style),
        .location = {
            toCSS(Length<CSS::AllUnzoomed> { static_cast<float>(operation->location().x()) }, style),
            toCSS(Length<CSS::AllUnzoomed> { static_cast<float>(operation->location().y()) }, style)
        },
        .stdDeviation = toCSS(Length<CSS::NonnegativeUnzoomed> { static_cast<float>(operation->stdDeviation()) }, style),
    };
}

Ref<FilterOperation> createFilterOperation(const CSS::DropShadow& filter, const BuilderState& state)
{
    const auto& zoomFactor = state.style().usedZoomForLength();
    int x = roundForImpreciseConversion<int>(toStyle(filter.location.x(), state).resolveZoom(zoomFactor));
    int y = roundForImpreciseConversion<int>(toStyle(filter.location.y(), state).resolveZoom(zoomFactor));
    int stdDeviation = filter.stdDeviation ? roundForImpreciseConversion<int>(toStyle(*filter.stdDeviation, state).resolveZoom(zoomFactor)) : 0;
    auto color = filter.color ? toStyleColor(*filter.color, state, ForVisitedLink::No) : Style::Color { CurrentColor { } };

    return DropShadowFilterOperationWithStyleColor::create(
        IntPoint { x, y },
        stdDeviation,
        color
    );
}

} // namespace Style
} // namespace WebCore
