/*
 * Copyright (C) 2015 Google Inc. All rights reserved.
 * Copyright (C) 2016-2023 Apple Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "config.h"
#include "StyleColor.h"

#include "CSSKeywordColor.h"
#include "Document.h"
#include "RenderStyle.h"
#include "RenderTheme.h"
#include "StyleAbsoluteColor.h"
#include "StyleColorLayers.h"
#include "StyleColorMix.h"
#include "StyleColorResolutionState.h"
#include "StyleContrastColor.h"
#include "StyleHexColor.h"
#include "StyleKeywordColor.h"
#include "StyleLightDarkColor.h"
#include "StyleRelativeColor.h"
#include <wtf/text/TextStream.h>

namespace WebCore {
namespace Style {

Color::Color(Color::ColorKind&& color)
    : value { WTFMove(color) }
{
}

Color::Color(EmptyToken token)
    : value { token }
{
}

Color::Color()
    : value { CurrentColor { } }
{
}

Color::Color(WebCore::Color color)
    : value { ResolvedColor { WTFMove(color) } }
{
}

Color::Color(SRGBA<uint8_t> color)
    : value { ResolvedColor { WebCore::Color { color } } }
{
}

Color::Color(ResolvedColor&& color)
    : value { WTFMove(color) }
{
}

Color::Color(CurrentColor&& color)
    : value { WTFMove(color) }
{
}

Color::Color(ColorLayers&& colorLayers)
    : value { makeIndirectColor(WTFMove(colorLayers)) }
{
}

Color::Color(ColorMix&& colorMix)
    : value { makeIndirectColor(WTFMove(colorMix)) }
{
}

Color::Color(ContrastColor&& contrastColor)
    : value { makeIndirectColor(WTFMove(contrastColor)) }
{
}

Color::Color(RelativeColor<RGBFunctionModernRelative>&& relative)
    : value { makeIndirectColor(WTFMove(relative)) }
{
}

Color::Color(RelativeColor<HSLFunctionModern>&& relative)
    : value { makeIndirectColor(WTFMove(relative)) }
{
}

Color::Color(RelativeColor<HWBFunction>&& relative)
    : value { makeIndirectColor(WTFMove(relative)) }
{
}

Color::Color(RelativeColor<LabFunction>&& relative)
    : value { makeIndirectColor(WTFMove(relative)) }
{
}

Color::Color(RelativeColor<LCHFunction>&& relative)
    : value { makeIndirectColor(WTFMove(relative)) }
{
}

Color::Color(RelativeColor<OKLabFunction>&& relative)
    : value { makeIndirectColor(WTFMove(relative)) }
{
}

Color::Color(RelativeColor<OKLCHFunction>&& relative)
    : value { makeIndirectColor(WTFMove(relative)) }
{
}

Color::Color(RelativeColor<ColorRGBFunction<ExtendedA98RGB<float>>>&& relative)
    : value { makeIndirectColor(WTFMove(relative)) }
{
}

Color::Color(RelativeColor<ColorRGBFunction<ExtendedDisplayP3<float>>>&& relative)
    : value { makeIndirectColor(WTFMove(relative)) }
{
}

Color::Color(RelativeColor<ColorRGBFunction<ExtendedProPhotoRGB<float>>>&& relative)
    : value { makeIndirectColor(WTFMove(relative)) }
{
}

Color::Color(RelativeColor<ColorRGBFunction<ExtendedRec2020<float>>>&& relative)
    : value { makeIndirectColor(WTFMove(relative)) }
{
}

Color::Color(RelativeColor<ColorRGBFunction<ExtendedSRGBA<float>>>&& relative)
    : value { makeIndirectColor(WTFMove(relative)) }
{
}

Color::Color(RelativeColor<ColorRGBFunction<ExtendedLinearSRGBA<float>>>&& relative)
    : value { makeIndirectColor(WTFMove(relative)) }
{
}

Color::Color(RelativeColor<ColorXYZFunction<XYZA<float, WhitePoint::D50>>>&& relative)
    : value { makeIndirectColor(WTFMove(relative)) }
{
}

Color::Color(RelativeColor<ColorXYZFunction<XYZA<float, WhitePoint::D65>>>&& relative)
    : value { makeIndirectColor(WTFMove(relative)) }
{
}

Color::Color(const Color& other)
    : value { copy(other.value) }
{
}

Color& Color::operator=(const Color& other)
{
    value = copy(other.value);
    return *this;
}

Color::Color(Color&&) = default;
Color& Color::operator=(Color&&) = default;

Color::~Color() = default;

bool Color::operator==(const Color& other) const = default;

Color Color::currentColor()
{
    return Color { CurrentColor { } };
}

Color::ColorKind Color::copy(const Color::ColorKind& other)
{
    return WTF::switchOn(other,
        []<typename T>(const T& color) -> Color::ColorKind {
            return color;
        },
        []<typename T>(const UniqueRef<T>& color) -> Color::ColorKind {
            return makeUniqueRef<T>(color.get());
        }
    );
}

String Color::debugDescription() const
{
    TextStream ts;
    ts << *this;
    return ts.release();
}

WebCore::Color Color::resolveColor(const WebCore::Color& currentColor) const
{
    return switchOn([&](const auto& kind) { return WebCore::Style::resolveColor(kind, currentColor); });
}

bool Color::containsCurrentColor() const
{
    return switchOn([](const auto& kind) { return WebCore::Style::containsCurrentColor(kind); });
}

bool Color::isCurrentColor() const
{
    return std::holds_alternative<CurrentColor>(value);
}

bool Color::isColorMix() const
{
    return std::holds_alternative<UniqueRef<ColorMix>>(value);
}

bool Color::isContrastColor() const
{
    return std::holds_alternative<UniqueRef<ContrastColor>>(value);
}

bool Color::isRelativeColor() const
{
    return std::holds_alternative<UniqueRef<RelativeColor<RGBFunctionModernRelative>>>(value)
        || std::holds_alternative<UniqueRef<RelativeColor<HSLFunctionModern>>>(value)
        || std::holds_alternative<UniqueRef<RelativeColor<HWBFunction>>>(value)
        || std::holds_alternative<UniqueRef<RelativeColor<LabFunction>>>(value)
        || std::holds_alternative<UniqueRef<RelativeColor<LCHFunction>>>(value)
        || std::holds_alternative<UniqueRef<RelativeColor<OKLabFunction>>>(value)
        || std::holds_alternative<UniqueRef<RelativeColor<OKLCHFunction>>>(value)
        || std::holds_alternative<UniqueRef<RelativeColor<ColorRGBFunction<ExtendedA98RGB<float>>>>>(value)
        || std::holds_alternative<UniqueRef<RelativeColor<ColorRGBFunction<ExtendedDisplayP3<float>>>>>(value)
        || std::holds_alternative<UniqueRef<RelativeColor<ColorRGBFunction<ExtendedProPhotoRGB<float>>>>>(value)
        || std::holds_alternative<UniqueRef<RelativeColor<ColorRGBFunction<ExtendedRec2020<float>>>>>(value)
        || std::holds_alternative<UniqueRef<RelativeColor<ColorRGBFunction<ExtendedSRGBA<float>>>>>(value)
        || std::holds_alternative<UniqueRef<RelativeColor<ColorRGBFunction<ExtendedLinearSRGBA<float>>>>>(value)
        || std::holds_alternative<UniqueRef<RelativeColor<ColorXYZFunction<XYZA<float, WhitePoint::D50>>>>>(value)
        || std::holds_alternative<UniqueRef<RelativeColor<ColorXYZFunction<XYZA<float, WhitePoint::D65>>>>>(value);
}

bool Color::isResolvedColor() const
{
    return std::holds_alternative<ResolvedColor>(value);
}

const WebCore::Color& Color::resolvedColor() const
{
    ASSERT(isResolvedColor());
    return std::get<ResolvedColor>(value).color;
}

template<typename T> Color::ColorKind Color::makeIndirectColor(T&& colorType)
{
    return { makeUniqueRef<T>(WTFMove(colorType)) };
}

// MARK: - MarkableTraits

bool Color::MarkableTraits::isEmptyValue(const Color& color)
{
    return std::holds_alternative<EmptyToken>(color.value);
}

Color Color::MarkableTraits::emptyValue()
{
    return Color(EmptyToken());
}

WebCore::Color resolveColor(const Color& value, const WebCore::Color& currentColor)
{
    return value.resolveColor(currentColor);
}

bool containsCurrentColor(const Color& value)
{
    return value.containsCurrentColor();
}

// MARK: - Serialization

String serializationForCSS(const CSS::SerializationContext& context, const Color& value)
{
    return WTF::switchOn(value, [&](const auto& kind) { return WebCore::Style::serializationForCSS(context, kind); });
}

void serializationForCSS(StringBuilder& builder, const CSS::SerializationContext& context, const Color& value)
{
    return WTF::switchOn(value, [&](const auto& kind) { WebCore::Style::serializationForCSS(builder, context, kind); });
}

// MARK: - TextStream.

TextStream& operator<<(TextStream& ts, const Color& value)
{
    ts << "Style::Color[";
    WTF::switchOn(value, [&](const auto& kind) { ts << kind; });
    ts << "]";

    return ts;
}

// MARK: - Conversion

Color toStyleColor(const CSS::Color& value, ColorResolutionState& state)
{
    return WTF::switchOn(value, [&](const auto& color) { return toStyleColor(color, state); });
}

Color toStyleColor(const CSS::Color& value, Ref<const Document> document, const RenderStyle& style, const CSSToLengthConversionData& conversionData, ForVisitedLink forVisitedLink)
{
    auto resolutionState = ColorResolutionState {
        .document = document,
        .style = style,
        .conversionData = conversionData,
        .forVisitedLink = forVisitedLink
    };
    return toStyleColor(value, resolutionState);
}

Color toStyleColorWithResolvedCurrentColor(const CSS::Color& value, Ref<const Document> document, RenderStyle& style, const CSSToLengthConversionData& conversionData, ForVisitedLink forVisitedLink)
{
    // FIXME: 'currentcolor' should be resolved at use time to make it inherit correctly. https://bugs.webkit.org/show_bug.cgi?id=210005
    if (CSS::containsCurrentColor(value)) {
        // Color is an inherited property so depending on it effectively makes the property inherited.
        style.setHasExplicitlyInheritedProperties();
        style.setDisallowsFastPathInheritance();
    }

    return toStyleColor(value, document, style, conversionData, forVisitedLink);
}

auto ToCSS<Color>::operator()(const Color& value, const RenderStyle& style) -> CSS::Color
{
    return CSS::Color { CSS::ResolvedColor { style.colorResolvingCurrentColor(value) } };
}

auto ToStyle<CSS::Color>::operator()(const CSS::Color& value, const BuilderState& builderState, ForVisitedLink forVisitedLink) -> Color
{
    return toStyleColor(value, builderState.document(), builderState.style(), builderState.cssToLengthConversionData(), forVisitedLink);
}

auto ToStyle<CSS::Color>::operator()(const CSS::Color& value, const BuilderState& builderState) -> Color
{
    return toStyle(value, builderState, ForVisitedLink::No);
}

} // namespace Style
} // namespace WebCore
