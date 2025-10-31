/*
 * Copyright (C) 2013 Google Inc. All rights reserved.
 * Copyright (C) 2014-2023 Apple Inc. All rights reserved.
 * Copyright (C) 2023 ChangSeok Oh <changseok@webkit.org>
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

#include "AnchorPositionEvaluator.h"
#include "BlockEllipsis.h"
#include "CSSBasicShapeValue.h"
#include "CSSCalcSymbolTable.h"
#include "CSSCalcValue.h"
#include "CSSColorSchemeValue.h"
#include "CSSContentDistributionValue.h"
#include "CSSCounterStyleRegistry.h"
#include "CSSDynamicRangeLimitValue.h"
#include "CSSFontFeatureValue.h"
#include "CSSFontStyleWithAngleValue.h"
#include "CSSFontVariationValue.h"
#include "CSSFunctionValue.h"
#include "CSSGridAutoRepeatValue.h"
#include "CSSGridIntegerRepeatValue.h"
#include "CSSGridLineNamesValue.h"
#include "CSSGridLineValue.h"
#include "CSSImageSetValue.h"
#include "CSSImageValue.h"
#include "CSSOffsetRotateValue.h"
#include "CSSPathValue.h"
#include "CSSPrimitiveValue.h"
#include "CSSPrimitiveValueMappings.h"
#include "CSSPropertyParserConsumer+Font.h"
#include "CSSRayValue.h"
#include "CSSReflectValue.h"
#include "CSSSubgridValue.h"
#include "CSSValuePair.h"
#include "CalculationValue.h"
#include "FontPalette.h"
#include "FontSelectionValueInlines.h"
#include "FontSizeAdjust.h"
#include "FrameDestructionObserverInlines.h"
#include "GridPositionsResolver.h"
#include "LineClampValue.h"
#include "LocalFrame.h"
#include "Quirks.h"
#include "QuotesData.h"
#include "RenderStyleInlines.h"
#include "SVGElementTypeHelpers.h"
#include "SVGPathElement.h"
#include "SVGRenderStyle.h"
#include "SVGURIReference.h"
#include "ScrollAxis.h"
#include "ScrollbarColor.h"
#include "ScrollbarGutter.h"
#include "Settings.h"
#include "StyleBasicShape.h"
#include "StyleBuilderState.h"
#include "StyleColorScheme.h"
#include "StyleDynamicRangeLimit.h"
#include "StyleEasingFunction.h"
#include "StylePathData.h"
#include "StylePrimitiveNumericTypes+Conversions.h"
#include "StyleRayFunction.h"
#include "StyleReflection.h"
#include "StyleResolveForFont.h"
#include "StyleScrollMargin.h"
#include "StyleScrollPadding.h"
#include "StyleScrollSnapPoints.h"
#include "StyleTextEdge.h"
#include "TabSize.h"
#include "TextSpacing.h"
#include "TimelineRange.h"
#include "TouchAction.h"
#include "TransformOperationsBuilder.h"
#include "ViewTimeline.h"
#include "WillChangeData.h"
#include <wtf/text/MakeString.h>

namespace WebCore {
namespace Style {

// FIXME: Some of those functions assume the CSS parser only allows valid CSSValue types.
// This might not be true if we pass the CSSValue from js via CSS Typed OM.

class BuilderConverter {
public:
    static WebCore::Length convertLength(BuilderState&, const CSSValue&);
    static WebCore::Length convertLengthOrAuto(BuilderState&, const CSSValue&);
    static WebCore::Length convertLengthOrAutoOrContent(BuilderState&, const CSSValue&);
    static WebCore::Length convertLengthSizing(BuilderState&, const CSSValue&);
    static WebCore::Length convertLengthMaxSizing(BuilderState&, const CSSValue&);
    static WebCore::Length convertLengthAllowingNumber(BuilderState&, const CSSValue&); // Assumes unit is 'px' if input is a number.
    static WebCore::Length convertTextLengthOrNormal(BuilderState&, const CSSValue&); // Converts length by text zoom factor, normal to zero
    static TabSize convertTabSize(BuilderState&, const CSSValue&);
    template<typename T> static T convertComputedLength(BuilderState&, const CSSValue&);
    template<typename T> static T convertLineWidth(BuilderState&, const CSSValue&);
    static LengthSize convertRadius(BuilderState&, const CSSValue&);
    static LengthPoint convertPosition(BuilderState&, const CSSValue&);
    static LengthPoint convertPositionOrAuto(BuilderState&, const CSSValue&);
    static LengthPoint convertPositionOrAutoOrNormal(BuilderState&, const CSSValue&);
    static OptionSet<TextDecorationLine> convertTextDecorationLine(BuilderState&, const CSSValue&);
    static OptionSet<TextTransform> convertTextTransform(BuilderState&, const CSSValue&);
    template<typename T> static T convertNumber(BuilderState&, const CSSValue&);
    template<typename T> static T convertNumberOrAuto(BuilderState&, const CSSValue&);
    static short convertWebkitHyphenateLimitLines(BuilderState&, const CSSValue&);
    template<CSSPropertyID> static RefPtr<StyleImage> convertStyleImage(BuilderState&, CSSValue&);
    static ImageOrientation convertImageOrientation(BuilderState&, const CSSValue&);
    static TransformOperations convertTransform(BuilderState&, const CSSValue&);
    static RefPtr<RotateTransformOperation> convertRotate(BuilderState&, const CSSValue&);
    static RefPtr<ScaleTransformOperation> convertScale(BuilderState&, const CSSValue&);
    static RefPtr<TranslateTransformOperation> convertTranslate(BuilderState&, const CSSValue&);
#if ENABLE(DARK_MODE_CSS)
    static Style::ColorScheme convertColorScheme(BuilderState&, const CSSValue&);
#endif
    static String convertString(BuilderState&, const CSSValue&);
    static String convertStringOrAuto(BuilderState&, const CSSValue&);
    static AtomString convertStringOrAutoAtom(BuilderState& state, const CSSValue& value) { return AtomString { convertStringOrAuto(state, value) }; }
    static String convertStringOrNone(BuilderState&, const CSSValue&);
    static AtomString convertStringOrNoneAtom(BuilderState& state, const CSSValue& value) { return AtomString { convertStringOrNone(state, value) }; }
    static OptionSet<TextEmphasisPosition> convertTextEmphasisPosition(BuilderState&, const CSSValue&);
    static TextAlignMode convertTextAlign(BuilderState&, const CSSValue&);
    static TextAlignLast convertTextAlignLast(BuilderState&, const CSSValue&);
    static RefPtr<StylePathData> convertDPath(BuilderState&, const CSSValue&);
    static RefPtr<PathOperation> convertPathOperation(BuilderState&, const CSSValue&);
    static Resize convertResize(BuilderState&, const CSSValue&);
    static int convertMarqueeRepetition(BuilderState&, const CSSValue&);
    static int convertMarqueeSpeed(BuilderState&, const CSSValue&);
    static RefPtr<QuotesData> convertQuotes(BuilderState&, const CSSValue&);
    static OptionSet<TextUnderlinePosition> convertTextUnderlinePosition(BuilderState&, const CSSValue&);
    static TextUnderlineOffset convertTextUnderlineOffset(BuilderState&, const CSSValue&);
    static TextDecorationThickness convertTextDecorationThickness(BuilderState&, const CSSValue&);
    static RefPtr<StyleReflection> convertReflection(BuilderState&, const CSSValue&);
    static TextEdge convertTextEdge(BuilderState&, const CSSValue&);
    static IntSize convertInitialLetter(BuilderState&, const CSSValue&);
    static float convertTextStrokeWidth(BuilderState&, const CSSValue&);
    static OptionSet<LineBoxContain> convertLineBoxContain(BuilderState&, const CSSValue&);
    static RefPtr<ShapeValue> convertShapeValue(BuilderState&, const CSSValue&);
    static ScrollSnapType convertScrollSnapType(BuilderState&, const CSSValue&);
    static ScrollSnapAlign convertScrollSnapAlign(BuilderState&, const CSSValue&);
    static ScrollSnapStop convertScrollSnapStop(BuilderState&, const CSSValue&);
    static std::optional<ScrollbarColor> convertScrollbarColor(BuilderState&, const CSSValue&);
    static ScrollbarGutter convertScrollbarGutter(BuilderState&, const CSSValue&);
    // scrollbar-width converter is only needed for quirking.
    static ScrollbarWidth convertScrollbarWidth(BuilderState&, const CSSValue&);
    static GridTrackSize convertGridTrackSize(BuilderState&, const CSSValue&);
    static Vector<GridTrackSize> convertGridTrackSizeList(BuilderState&, const CSSValue&);
    static std::optional<GridTrackList> convertGridTrackList(BuilderState&, const CSSValue&);
    static GridPosition convertGridPosition(BuilderState&, const CSSValue&);
    static GridAutoFlow convertGridAutoFlow(BuilderState&, const CSSValue&);
    static Vector<StyleContentAlignmentData> convertContentAlignmentDataList(BuilderState&, const CSSValue&);
    static MasonryAutoFlow convertMasonryAutoFlow(BuilderState&, const CSSValue&);
    static std::optional<float> convertPerspective(BuilderState&, const CSSValue&);
    static std::optional<WebCore::Length> convertMarqueeIncrement(BuilderState&, const CSSValue&);
    static FilterOperations convertFilterOperations(BuilderState&, const CSSValue&);
    static FilterOperations convertAppleColorFilterOperations(BuilderState&, const CSSValue&);
    static ListStyleType convertListStyleType(BuilderState&, const CSSValue&);
#if PLATFORM(IOS_FAMILY)
    static bool convertTouchCallout(BuilderState&, const CSSValue&);
#endif
#if ENABLE(TOUCH_EVENTS)
    static Color convertTapHighlightColor(BuilderState&, const CSSValue&);
#endif
    static OptionSet<TouchAction> convertTouchAction(BuilderState&, const CSSValue&);
#if ENABLE(OVERFLOW_SCROLLING_TOUCH)
    static bool convertOverflowScrolling(BuilderState&, const CSSValue&);
#endif
    static bool convertSmoothScrolling(BuilderState&, const CSSValue&);

    static FontSizeAdjust convertFontSizeAdjust(BuilderState&, const CSSValue&);
    static std::optional<FontSelectionValue> convertFontStyleFromValue(BuilderState&, const CSSValue&);
    static FontSelectionValue convertFontWeight(BuilderState&, const CSSValue&);
    static FontSelectionValue convertFontWidth(BuilderState&, const CSSValue&);
    static FontSelectionValue convertFontStyle(BuilderState&, const CSSValue&);
    static FontFeatureSettings convertFontFeatureSettings(BuilderState&, const CSSValue&);
    static FontVariationSettings convertFontVariationSettings(BuilderState&, const CSSValue&);
    static SVGLengthValue convertSVGLengthValue(BuilderState&, const CSSValue&, ShouldConvertNumberToPxLength = ShouldConvertNumberToPxLength::No);
    static Vector<SVGLengthValue> convertSVGLengthVector(BuilderState&, const CSSValue&, ShouldConvertNumberToPxLength = ShouldConvertNumberToPxLength::No);
    static Vector<SVGLengthValue> convertStrokeDashArray(BuilderState&, const CSSValue&);
    static PaintOrder convertPaintOrder(BuilderState&, const CSSValue&);
    static float convertOpacity(BuilderState&, const CSSValue&);
    static String convertSVGURIReference(BuilderState&, const CSSValue&);
    static StyleSelfAlignmentData convertSelfOrDefaultAlignmentData(BuilderState&, const CSSValue&);
    static StyleContentAlignmentData convertContentAlignmentData(BuilderState&, const CSSValue&);
    static GlyphOrientation convertGlyphOrientation(BuilderState&, const CSSValue&);
    static GlyphOrientation convertGlyphOrientationOrAuto(BuilderState&, const CSSValue&);
    static WebCore::Length convertLineHeight(BuilderState&, const CSSValue&, float multiplier = 1.f);
    static FontPalette convertFontPalette(BuilderState&, const CSSValue&);

    static BreakBetween convertPageBreakBetween(BuilderState&, const CSSValue&);
    static BreakInside convertPageBreakInside(BuilderState&, const CSSValue&);
    static BreakBetween convertColumnBreakBetween(BuilderState&, const CSSValue&);
    static BreakInside convertColumnBreakInside(BuilderState&, const CSSValue&);

    static OptionSet<HangingPunctuation> convertHangingPunctuation(BuilderState&, const CSSValue&);

    static OptionSet<SpeakAs> convertSpeakAs(BuilderState&, const CSSValue&);

    static WebCore::Length convertPositionComponentX(BuilderState&, const CSSValue&);
    static WebCore::Length convertPositionComponentY(BuilderState&, const CSSValue&);

    static GapLength convertGapLength(BuilderState&, const CSSValue&);

    static OffsetRotation convertOffsetRotate(BuilderState&, const CSSValue&);

    static OptionSet<Containment> convertContain(BuilderState&, const CSSValue&);
    static Vector<Style::ScopedName> convertContainerName(BuilderState&, const CSSValue&);

    static OptionSet<MarginTrimType> convertMarginTrim(BuilderState&, const CSSValue&);

    static TextSpacingTrim convertTextSpacingTrim(BuilderState&, const CSSValue&);
    static TextAutospace convertTextAutospace(BuilderState&, const CSSValue&);

    static std::optional<WebCore::Length> convertBlockStepSize(BuilderState&, const CSSValue&);

    static Vector<Style::ScopedName> convertViewTransitionClass(BuilderState&, const CSSValue&);
    static Style::ViewTransitionName convertViewTransitionName(BuilderState&, const CSSValue&);
    static RefPtr<WillChangeData> convertWillChange(BuilderState&, const CSSValue&);

    static Vector<AtomString> convertScrollTimelineName(BuilderState&, const CSSValue&);
    static Vector<ScrollAxis> convertScrollTimelineAxis(BuilderState&, const CSSValue&);
    static Vector<ViewTimelineInsets> convertViewTimelineInset(BuilderState&, const CSSValue&);

    static Vector<ScopedName> convertAnchorName(BuilderState&, const CSSValue&);
    static std::optional<ScopedName> convertPositionAnchor(BuilderState&, const CSSValue&);
    static std::optional<PositionArea> convertPositionArea(BuilderState&, const CSSValue&);

    static BlockEllipsis convertBlockEllipsis(BuilderState&, const CSSValue&);
    static size_t convertMaxLines(BuilderState&, const CSSValue&);

    static LineClampValue convertLineClamp(BuilderState&, const CSSValue&);

    static RefPtr<TimingFunction> convertTimingFunction(BuilderState&, const CSSValue&);

    static NameScope convertNameScope(BuilderState&, const CSSValue&);

    static SingleTimelineRange convertAnimationRangeStart(BuilderState&, const CSSValue&);
    static SingleTimelineRange convertAnimationRangeEnd(BuilderState&, const CSSValue&);

    static Style::ScrollPaddingEdge convertScrollPaddingEdge(BuilderState&, const CSSValue&);
    static Style::ScrollMarginEdge convertScrollMarginEdge(BuilderState&, const CSSValue&);
    static Style::DynamicRangeLimit convertDynamicRangeLimit(BuilderState&, const CSSValue&);

    static Vector<PositionTryFallback> convertPositionTryFallbacks(BuilderState&, const CSSValue&);

    template<CSSValueID, CSSValueID> static WebCore::Length convertPositionComponent(BuilderState&, const CSSValue&);

    template<class ValueType> static const ValueType* requiredDowncast(BuilderState&, const CSSValue&);
    template<class ValueType> static std::optional<std::pair<const ValueType&, const ValueType&>> requiredPairDowncast(BuilderState&, const CSSValue&);

    template<class ValueType> struct TypedListIterator {
        using iterator_category = std::forward_iterator_tag;
        using value_type = const ValueType&;
        using difference_type = ptrdiff_t;
        using pointer = const ValueType*;
        using reference = const ValueType&;

        TypedListIterator(CSSValueList::iterator iterator)
            : iterator(iterator)
        { }
        TypedListIterator& operator++() { ++iterator; return *this; }
        const ValueType& operator*() const { return downcast<ValueType>(*iterator); }
        bool operator!=(const TypedListIterator& other) const { return iterator != other.iterator; }

        CSSValueList::iterator iterator;
    };
    template<class ValueType> struct TypedList {
        TypedList(const CSSValueContainingVector& list)
            : list(list)
        { }

        unsigned size() const { return list->size(); }
        const ValueType& item(unsigned index) const { return downcast<ValueType>(*list->item(index)); }
        TypedListIterator<ValueType> begin() const { return list->begin(); }
        TypedListIterator<ValueType> end() const { return list->end(); }

        Ref<const CSSValueContainingVector> list;
    };
    template<class ListType, class ValueType, unsigned minimumLength = 1> static std::optional<TypedList<ValueType>> requiredListDowncast(BuilderState&, const CSSValue&);

private:
    friend class BuilderCustom;

    static WebCore::Length convertToRadiusLength(BuilderState&, const CSSPrimitiveValue&);
    static WebCore::Length parseSnapCoordinate(BuilderState&, const CSSValue&);

    static GridLength createGridTrackBreadth(BuilderState&, const CSSPrimitiveValue&);
    static GridTrackSize createGridTrackSize(BuilderState&, const CSSValue&);
    static std::optional<GridTrackList> createGridTrackList(BuilderState&, const CSSValue&);
    static GridPosition createGridPosition(BuilderState&, const CSSValue&);
    static NamedGridLinesMap createImplicitNamedGridLinesFromGridArea(BuilderState&, const NamedGridAreaMap&, GridTrackSizingDirection);

    static Style::BasicShape convertBasicShape(BuilderState&, const CSSBasicShapeValue&, std::optional<float> zoom);

    static CSSToLengthConversionData cssToLengthConversionDataWithTextZoomFactor(BuilderState&);
};

template<class ValueType>
inline const ValueType* BuilderConverter::requiredDowncast(BuilderState& builderState, const CSSValue& value)
{
    auto* typedValue = dynamicDowncast<ValueType>(value);
    if (UNLIKELY(!typedValue)) {
        builderState.setCurrentPropertyInvalidAtComputedValueTime();
        return nullptr;
    }
    return typedValue;
}

template<class ValueType>
inline std::optional<std::pair<const ValueType&, const ValueType&>> BuilderConverter::requiredPairDowncast(BuilderState& builderState, const CSSValue& value)
{
    auto* pairValue = requiredDowncast<CSSValuePair>(builderState, value);
    if (UNLIKELY(!pairValue))
        return { };
    auto* firstValue = requiredDowncast<ValueType>(builderState, pairValue->first());
    if (UNLIKELY(!firstValue))
        return { };
    auto* secondValue = requiredDowncast<ValueType>(builderState, pairValue->second());
    if (UNLIKELY(!secondValue))
        return { };
    return { { *firstValue, *secondValue } };
}

template<class ListType, class ValueType, unsigned minimumSize>
inline auto BuilderConverter::requiredListDowncast(BuilderState& builderState, const CSSValue& value) -> std::optional<TypedList<ValueType>>
{
    auto* listValue = requiredDowncast<ListType>(builderState, value);
    if (UNLIKELY(!listValue))
        return { };
    if (UNLIKELY(listValue->size() < minimumSize)) {
        builderState.setCurrentPropertyInvalidAtComputedValueTime();
        return { };
    }
    for (auto& value : *listValue) {
        if (UNLIKELY(!requiredDowncast<ValueType>(builderState, value)))
            return { };
    }
    return TypedList<ValueType> { *listValue };
}

inline WebCore::Length BuilderConverter::convertLength(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };

    CSSToLengthConversionData conversionData = builderState.useSVGZoomRulesForLength() ?
        builderState.cssToLengthConversionData().copyWithAdjustedZoom(1.0f)
        : builderState.cssToLengthConversionData();

    if (primitiveValue->isLength()) {
        auto length = primitiveValue->resolveAsLength<WebCore::Length>(conversionData);
        length.setHasQuirk(primitiveValue->primitiveType() == CSSUnitType::CSS_QUIRKY_EM);
        return length;
    }

    if (primitiveValue->isPercentage())
        return WebCore::Length(primitiveValue->resolveAsPercentage(conversionData), LengthType::Percent);

    if (primitiveValue->isCalculatedPercentageWithLength())
        return WebCore::Length(primitiveValue->cssCalcValue()->createCalculationValue(conversionData, CSSCalcSymbolTable { }));

    ASSERT_NOT_REACHED();
    return WebCore::Length(0, LengthType::Fixed);
}

inline WebCore::Length BuilderConverter::convertLengthAllowingNumber(BuilderState& builderState, const CSSValue& value)
{
    CSSToLengthConversionData conversionData = builderState.useSVGZoomRulesForLength() ?
        builderState.cssToLengthConversionData().copyWithAdjustedZoom(1.0f)
        : builderState.cssToLengthConversionData();

    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };

    if (primitiveValue->isNumberOrInteger())
        return WebCore::Length(primitiveValue->resolveAsNumber(conversionData), LengthType::Fixed);
    return convertLength(builderState, value);
}

inline WebCore::Length BuilderConverter::convertLengthOrAuto(BuilderState& builderState, const CSSValue& value)
{
    if (value.valueID() == CSSValueAuto)
        return WebCore::Length(LengthType::Auto);
    return convertLength(builderState, value);
}

inline WebCore::Length BuilderConverter::convertLengthSizing(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };

    switch (primitiveValue->valueID()) {
    case CSSValueInvalid:
        return convertLength(builderState, value);
    case CSSValueIntrinsic:
        return WebCore::Length(LengthType::Intrinsic);
    case CSSValueMinIntrinsic:
        return WebCore::Length(LengthType::MinIntrinsic);
    case CSSValueMinContent:
    case CSSValueWebkitMinContent:
        return WebCore::Length(LengthType::MinContent);
    case CSSValueMaxContent:
    case CSSValueWebkitMaxContent:
        return WebCore::Length(LengthType::MaxContent);
    case CSSValueWebkitFillAvailable:
        return WebCore::Length(LengthType::FillAvailable);
    case CSSValueFitContent:
    case CSSValueWebkitFitContent:
        return WebCore::Length(LengthType::FitContent);
    case CSSValueAuto:
        return WebCore::Length(LengthType::Auto);
    case CSSValueContent:
        return WebCore::Length(LengthType::Content);
    default:
        ASSERT_NOT_REACHED();
        return { };
    }
}

inline ListStyleType BuilderConverter::convertListStyleType(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };

    if (primitiveValue->isValueID()) {
        if (primitiveValue->valueID() == CSSValueNone)
            return { ListStyleType::Type::None, nullAtom() };
        return { ListStyleType::Type::CounterStyle, makeAtomString(primitiveValue->stringValue()) };
    }
    if (primitiveValue->isCustomIdent()) {
        ASSERT(builderState.document().settings().cssCounterStyleAtRulesEnabled());
        return { ListStyleType::Type::CounterStyle, makeAtomString(primitiveValue->stringValue()) };
    }
#if !ASSERT_ENABLED
    UNUSED_PARAM(builderState);
#endif
    return { ListStyleType::Type::String, makeAtomString(primitiveValue->stringValue()) };
}

inline WebCore::Length BuilderConverter::convertLengthMaxSizing(BuilderState& builderState, const CSSValue& value)
{
    if (value.valueID() == CSSValueNone)
        return WebCore::Length(LengthType::Undefined);
    return convertLengthSizing(builderState, value);
}

inline TabSize BuilderConverter::convertTabSize(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };
    if (primitiveValue->isNumber())
        return TabSize(primitiveValue->resolveAsNumber<float>(builderState.cssToLengthConversionData()), SpaceValueType);
    return TabSize(primitiveValue->resolveAsLength<float>(builderState.cssToLengthConversionData()), LengthValueType);
}

template<typename T>
inline T BuilderConverter::convertComputedLength(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };
    return primitiveValue->resolveAsLength<T>(builderState.cssToLengthConversionData());
}

template<typename T>
inline T BuilderConverter::convertLineWidth(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };
    switch (primitiveValue->valueID()) {
    case CSSValueThin:
        return 1;
    case CSSValueMedium:
        return 3;
    case CSSValueThick:
        return 5;
    case CSSValueInvalid: {
        // Any original result that was >= 1 should not be allowed to fall below 1.
        // This keeps border lines from vanishing.
        T result = convertComputedLength<T>(builderState, value);
        if (builderState.style().usedZoom() < 1.0f && result < 1.0) {
            T originalLength = primitiveValue->resolveAsLength<T>(builderState.cssToLengthConversionData().copyWithAdjustedZoom(1.0));
            if (originalLength >= 1.0)
                return 1;
        }
        float minimumLineWidth = 1 / builderState.document().deviceScaleFactor();
        if (result > 0 && result < minimumLineWidth)
            return minimumLineWidth;
        return floorToDevicePixel(result, builderState.document().deviceScaleFactor());
    }
    default:
        ASSERT_NOT_REACHED();
        return 0;
    }
}

inline WebCore::Length BuilderConverter::convertToRadiusLength(BuilderState& builderState, const CSSPrimitiveValue& value)
{
    auto& conversionData = builderState.cssToLengthConversionData();
    if (value.isPercentage())
        return WebCore::Length(value.resolveAsPercentage(conversionData), LengthType::Percent);
    if (value.isCalculatedPercentageWithLength())
        return WebCore::Length(value.cssCalcValue()->createCalculationValue(conversionData, CSSCalcSymbolTable { }));
    auto length = value.resolveAsLength<WebCore::Length>(conversionData);
    if (length.isNegative())
        return { 0, LengthType::Fixed };
    return length;
}

inline LengthSize BuilderConverter::convertRadius(BuilderState& builderState, const CSSValue& value)
{
    if (!value.isPair())
        return { { 0, LengthType::Fixed }, { 0, LengthType::Fixed } };

    auto pair = requiredPairDowncast<CSSPrimitiveValue>(builderState, value);
    if (!pair)
        return { };

    LengthSize radius {
        convertToRadiusLength(builderState, pair->first),
        convertToRadiusLength(builderState, pair->second)
    };

    ASSERT(!radius.width.isNegative());
    ASSERT(!radius.height.isNegative());
    return radius;
}

inline WebCore::Length BuilderConverter::convertPositionComponentX(BuilderState& builderState, const CSSValue& value)
{
    return convertPositionComponent<CSSValueLeft, CSSValueRight>(builderState, value);
}

inline WebCore::Length BuilderConverter::convertPositionComponentY(BuilderState& builderState, const CSSValue& value)
{
    return convertPositionComponent<CSSValueTop, CSSValueBottom>(builderState, value);
}

template<CSSValueID cssValueFor0, CSSValueID cssValueFor100>
inline WebCore::Length BuilderConverter::convertPositionComponent(BuilderState& builderState, const CSSValue& value)
{
    WebCore::Length length;

    auto* lengthValue = &value;
    bool relativeToTrailingEdge = false;

    if (value.isPair()) {
        auto& first = value.first();
        if (first.valueID() == CSSValueRight || first.valueID() == CSSValueBottom)
            relativeToTrailingEdge = true;
        lengthValue = &value.second();
    }

    if (value.isValueID()) {
        switch (value.valueID()) {
        case cssValueFor0:
            return WebCore::Length(0, LengthType::Percent);
        case cssValueFor100:
            return WebCore::Length(100, LengthType::Percent);
        case CSSValueCenter:
            return WebCore::Length(50, LengthType::Percent);
        default:
            ASSERT_NOT_REACHED();
        }
    }

    length = convertLength(builderState, *lengthValue);

    if (relativeToTrailingEdge)
        length = convertTo100PercentMinusLength(length);

    return length;
}

inline LengthPoint BuilderConverter::convertPosition(BuilderState& builderState, const CSSValue& value)
{
    if (!value.isPair())
        return RenderStyle::initialObjectPosition();

    auto lengthX = convertPositionComponent<CSSValueLeft, CSSValueRight>(builderState, value.first());
    auto lengthY = convertPositionComponent<CSSValueTop, CSSValueBottom>(builderState, value.second());

    return LengthPoint(lengthX, lengthY);
}

inline LengthPoint BuilderConverter::convertPositionOrAutoOrNormal(BuilderState& builderState, const CSSValue& value)
{
    if (value.isPair())
        return convertPosition(builderState, value);
    if (value.valueID() == CSSValueNormal)
        return { { LengthType::Normal }, { LengthType::Normal } };
    return { };
}

inline LengthPoint BuilderConverter::convertPositionOrAuto(BuilderState& builderState, const CSSValue& value)
{
    if (value.isPair())
        return convertPosition(builderState, value);
    return { };
}

inline OptionSet<TextDecorationLine> BuilderConverter::convertTextDecorationLine(BuilderState&, const CSSValue& value)
{
    auto result = RenderStyle::initialTextDecorationLine();
    if (auto* list = dynamicDowncast<CSSValueList>(value)) {
        for (auto& currentValue : *list)
            result.add(fromCSSValue<TextDecorationLine>(currentValue));
    }
    return result;
}

inline OptionSet<TextTransform> BuilderConverter::convertTextTransform(BuilderState&, const CSSValue& value)
{
    auto result = RenderStyle::initialTextTransform();
    if (auto* list = dynamicDowncast<CSSValueList>(value)) {
        for (auto& currentValue : *list)
            result.add(fromCSSValue<TextTransform>(currentValue));
    }
    return result;
}

template<typename T>
inline T BuilderConverter::convertNumber(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };
    return primitiveValue->resolveAsNumber<T>(builderState.cssToLengthConversionData());
}

template<typename T>
inline T BuilderConverter::convertNumberOrAuto(BuilderState& builderState, const CSSValue& value)
{
    if (value.valueID() == CSSValueAuto)
        return -1;
    return convertNumber<T>(builderState, value);
}

inline short BuilderConverter::convertWebkitHyphenateLimitLines(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };
    if (primitiveValue->valueID() == CSSValueNoLimit)
        return -1;
    return primitiveValue->resolveAsNumber<short>(builderState.cssToLengthConversionData());
}

template<CSSPropertyID>
inline RefPtr<StyleImage> BuilderConverter::convertStyleImage(BuilderState& builderState, CSSValue& value)
{
    return builderState.createStyleImage(value);
}

inline ImageOrientation BuilderConverter::convertImageOrientation(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };
    if (primitiveValue->valueID() == CSSValueFromImage)
        return ImageOrientation::Orientation::FromImage;
    return ImageOrientation::Orientation::None;
}

inline TransformOperations BuilderConverter::convertTransform(BuilderState& builderState, const CSSValue& value)
{
    CSSToLengthConversionData conversionData = builderState.useSVGZoomRulesForLength() ?
        builderState.cssToLengthConversionData().copyWithAdjustedZoom(1.0f)
        : builderState.cssToLengthConversionData();
    return createTransformOperations(value, conversionData);
}

inline RefPtr<TranslateTransformOperation> BuilderConverter::convertTranslate(BuilderState& builderState, const CSSValue& value)
{
    CSSToLengthConversionData conversionData = builderState.useSVGZoomRulesForLength() ?
        builderState.cssToLengthConversionData().copyWithAdjustedZoom(1.0f)
        : builderState.cssToLengthConversionData();
    return createTranslate(value, conversionData);
}

inline RefPtr<RotateTransformOperation> BuilderConverter::convertRotate(BuilderState& builderState, const CSSValue& value)
{
    CSSToLengthConversionData conversionData = builderState.useSVGZoomRulesForLength() ?
        builderState.cssToLengthConversionData().copyWithAdjustedZoom(1.0f)
        : builderState.cssToLengthConversionData();
    return createRotate(value, conversionData);
}

inline RefPtr<ScaleTransformOperation> BuilderConverter::convertScale(BuilderState& builderState, const CSSValue& value)
{
    CSSToLengthConversionData conversionData = builderState.useSVGZoomRulesForLength() ?
        builderState.cssToLengthConversionData().copyWithAdjustedZoom(1.0f)
        : builderState.cssToLengthConversionData();
    return createScale(value, conversionData);
}

#if ENABLE(DARK_MODE_CSS)
inline Style::ColorScheme BuilderConverter::convertColorScheme(BuilderState& builderState, const CSSValue& value)
{
    RefPtr colorSchemeValue = requiredDowncast<CSSColorSchemeValue>(builderState, value);
    if (!colorSchemeValue)
        return { };
    return Style::toStyle(colorSchemeValue->colorScheme(), builderState);
}
#endif

inline String BuilderConverter::convertString(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };
    return primitiveValue->stringValue();
}

inline String BuilderConverter::convertStringOrAuto(BuilderState& builderState, const CSSValue& value)
{
    if (value.valueID() == CSSValueAuto)
        return nullAtom();
    return convertString(builderState, value);
}

inline String BuilderConverter::convertStringOrNone(BuilderState& builderState, const CSSValue& value)
{
    if (value.valueID() == CSSValueNone)
        return nullAtom();
    return convertString(builderState, value);
}

inline static OptionSet<TextEmphasisPosition> valueToEmphasisPosition(const CSSPrimitiveValue& primitiveValue)
{
    ASSERT(primitiveValue.isValueID());

    switch (primitiveValue.valueID()) {
    case CSSValueOver:
        return TextEmphasisPosition::Over;
    case CSSValueUnder:
        return TextEmphasisPosition::Under;
    case CSSValueLeft:
        return TextEmphasisPosition::Left;
    case CSSValueRight:
        return TextEmphasisPosition::Right;
    default:
        break;
    }

    ASSERT_NOT_REACHED();
    return RenderStyle::initialTextEmphasisPosition();
}

inline OptionSet<TextEmphasisPosition> BuilderConverter::convertTextEmphasisPosition(BuilderState& builderState, const CSSValue& value)
{
    if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value))
        return valueToEmphasisPosition(*primitiveValue);

    auto list = requiredListDowncast<CSSValueList, CSSPrimitiveValue>(builderState, value);
    if (!list)
        return { };

    OptionSet<TextEmphasisPosition> position;
    for (auto& currentValue : *list)
        position.add(valueToEmphasisPosition(currentValue));
    return position;
}

inline TextAlignMode BuilderConverter::convertTextAlign(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };
    ASSERT(primitiveValue->isValueID());

    const auto& parentStyle = builderState.parentStyle();

    // User agents are expected to have a rule in their user agent stylesheet that matches th elements that have a parent
    // node whose computed value for the 'text-align' property is its initial value, whose declaration block consists of
    // just a single declaration that sets the 'text-align' property to the value 'center'.
    // https://html.spec.whatwg.org/multipage/rendering.html#rendering
    if (primitiveValue->valueID() == CSSValueInternalThCenter) {
        if (parentStyle.textAlign() == RenderStyle::initialTextAlign())
            return TextAlignMode::Center;
        return parentStyle.textAlign();
    }

    if (primitiveValue->valueID() == CSSValueWebkitMatchParent || primitiveValue->valueID() == CSSValueMatchParent) {
        const auto* element = builderState.element();

        if (element && element == builderState.document().documentElement())
            return TextAlignMode::Start;
        if (parentStyle.textAlign() == TextAlignMode::Start)
            return parentStyle.writingMode().isBidiLTR() ? TextAlignMode::Left : TextAlignMode::Right;
        if (parentStyle.textAlign() == TextAlignMode::End)
            return parentStyle.writingMode().isBidiLTR() ? TextAlignMode::Right : TextAlignMode::Left;

        return parentStyle.textAlign();
    }

    return fromCSSValue<TextAlignMode>(value);
}

inline TextAlignLast BuilderConverter::convertTextAlignLast(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };
    ASSERT(primitiveValue->isValueID());

    if (primitiveValue->valueID() != CSSValueMatchParent)
        return fromCSSValue<TextAlignLast>(value);

    auto& parentStyle = builderState.parentStyle();
    if (parentStyle.textAlignLast() == TextAlignLast::Start)
        return parentStyle.writingMode().isBidiLTR() ? TextAlignLast::Left : TextAlignLast::Right;
    if (parentStyle.textAlignLast() == TextAlignLast::End)
        return parentStyle.writingMode().isBidiLTR() ? TextAlignLast::Right : TextAlignLast::Left;
    return parentStyle.textAlignLast();
}

inline RefPtr<StylePathData> BuilderConverter::convertDPath(BuilderState& builderState, const CSSValue& value)
{
    if (RefPtr pathValue = dynamicDowncast<CSSPathValue>(value))
        return StylePathData::create(Style::toStyle(pathValue->path(), builderState));

    ASSERT(is<CSSPrimitiveValue>(value));
    ASSERT(downcast<CSSPrimitiveValue>(value).valueID() == CSSValueNone);
    return nullptr;
}

inline RefPtr<PathOperation> BuilderConverter::convertPathOperation(BuilderState& builderState, const CSSValue& value)
{
    if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value)) {
        if (primitiveValue->isURI()) {
            auto cssURLValue = primitiveValue->stringValue();
            auto fragment = SVGURIReference::fragmentIdentifierFromIRIString(cssURLValue, builderState.document());
            // FIXME: It doesn't work with external SVG references (see https://bugs.webkit.org/show_bug.cgi?id=126133)
            const TreeScope* treeScope = nullptr;
            if (builderState.element())
                treeScope = &builderState.element()->treeScopeForSVGReferences();
            else
                treeScope = &builderState.document();
            auto target = SVGURIReference::targetElementFromIRIString(cssURLValue, *treeScope);
            return ReferencePathOperation::create(cssURLValue, fragment, dynamicDowncast<SVGElement>(target.element.get()));
        }
        ASSERT(primitiveValue->valueID() == CSSValueNone);
        return nullptr;
    }

    if (RefPtr ray = dynamicDowncast<CSSRayValue>(value))
        return RayPathOperation::create(Style::toStyle(ray->ray(), builderState));

    RefPtr<PathOperation> operation;
    auto referenceBox = CSSBoxType::BoxMissing;
    auto processSingleValue = [&](const CSSValue& singleValue) {
        ASSERT(!is<CSSValueList>(singleValue));
        if (RefPtr ray = dynamicDowncast<CSSRayValue>(singleValue))
            operation = RayPathOperation::create(Style::toStyle(ray->ray(), builderState));
        else if (RefPtr shape = dynamicDowncast<CSSBasicShapeValue>(singleValue))
            operation = ShapePathOperation::create(convertBasicShape(builderState, *shape, std::nullopt));
        else
            referenceBox = fromCSSValue<CSSBoxType>(singleValue);
    };

    if (auto* list = dynamicDowncast<CSSValueList>(value)) {
        for (auto& currentValue : *list)
            processSingleValue(currentValue);
    } else
        processSingleValue(value);

    if (operation)
        operation->setReferenceBox(referenceBox);
    else {
        ASSERT(referenceBox != CSSBoxType::BoxMissing);
        operation = BoxPathOperation::create(referenceBox);
    }

    return operation;
}

inline Style::BasicShape BuilderConverter::convertBasicShape(BuilderState& builderState, const CSSBasicShapeValue& value, std::optional<float> zoom)
{
    return WTF::switchOn(value.shape(),
        [&](const auto& shape) {
            return Style::BasicShape { Style::toStyle(shape, builderState) };
        },
        [&](const CSS::PathFunction& path) {
            return Style::BasicShape { Style::overrideToStyle(path, builderState, zoom) };
        }
    );
}

inline Resize BuilderConverter::convertResize(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };

    auto resize = Resize::None;
    if (primitiveValue->valueID() == CSSValueInternalTextareaAuto)
        resize = builderState.document().settings().textAreasAreResizable() ? Resize::Both : Resize::None;
    else
        resize = fromCSSValue<Resize>(value);

    return resize;
}

inline int BuilderConverter::convertMarqueeRepetition(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };
    if (primitiveValue->valueID() == CSSValueInfinite)
        return -1; // -1 means repeat forever.

    ASSERT(primitiveValue->isNumber());
    return primitiveValue->resolveAsNumber<int>(builderState.cssToLengthConversionData());
}

inline int BuilderConverter::convertMarqueeSpeed(BuilderState& builderState, const CSSValue& value)
{
    auto& conversionData = builderState.cssToLengthConversionData();

    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };
    if (primitiveValue->isTime())
        return primitiveValue->resolveAsTime<int, CSS::TimeUnit::Ms>(conversionData);

    // For scrollamount support.
    ASSERT(primitiveValue->isNumber());
    return primitiveValue->resolveAsNumber<int>(conversionData);
}

inline RefPtr<QuotesData> BuilderConverter::convertQuotes(BuilderState& builderState, const CSSValue& value)
{
    if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value)) {
        if (primitiveValue->valueID() == CSSValueNone)
            return QuotesData::create({ });
        ASSERT(primitiveValue->valueID() == CSSValueAuto);
        return nullptr;
    }

    auto list = requiredListDowncast<CSSValueList, CSSPrimitiveValue>(builderState, value);
    if (!list)
        return { };

    Vector<std::pair<String, String>> quotes;
    quotes.reserveInitialCapacity(list->size() / 2);
    for (unsigned i = 0; i < list->size(); i += 2) {
        auto& first = list->item(i);
        if (list->size() <= i + 1)
            break;
        auto& second = list->item(i + 1);
        String startQuote = first.stringValue();
        String endQuote = second.stringValue();
        quotes.append(std::make_pair(startQuote, endQuote));
    }
    return QuotesData::create(quotes);
}

inline static OptionSet<TextUnderlinePosition> valueToUnderlinePosition(const CSSPrimitiveValue& primitiveValue)
{
    ASSERT(primitiveValue.isValueID());

    switch (primitiveValue.valueID()) {
    case CSSValueFromFont:
        return TextUnderlinePosition::FromFont;
    case CSSValueUnder:
        return TextUnderlinePosition::Under;
    case CSSValueLeft:
        return TextUnderlinePosition::Left;
    case CSSValueRight:
        return TextUnderlinePosition::Right;
    case CSSValueAuto:
        return RenderStyle::initialTextUnderlinePosition();
    default:
        break;
    }

    ASSERT_NOT_REACHED();
    return RenderStyle::initialTextUnderlinePosition();
}

inline OptionSet<TextUnderlinePosition> BuilderConverter::convertTextUnderlinePosition(BuilderState& builderState, const CSSValue& value)
{
    if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value))
        return valueToUnderlinePosition(*primitiveValue);

    auto pair = requiredPairDowncast<CSSPrimitiveValue>(builderState, value);
    if (!pair)
        return { };

    auto position = valueToUnderlinePosition(pair->first);
    position.add(valueToUnderlinePosition(pair->second));
    return position;
}

inline TextUnderlineOffset BuilderConverter::convertTextUnderlineOffset(BuilderState& builderState, const CSSValue& value)
{
    return TextUnderlineOffset::createWithLength(BuilderConverter::convertLengthOrAuto(builderState, value));
}

inline TextDecorationThickness BuilderConverter::convertTextDecorationThickness(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };
    switch (primitiveValue->valueID()) {
    case CSSValueAuto:
        return TextDecorationThickness::createWithAuto();
    case CSSValueFromFont:
        return TextDecorationThickness::createFromFont();
    default: {
        auto conversionData = builderState.cssToLengthConversionData();

        if (primitiveValue->isPercentage())
            return TextDecorationThickness::createWithLength(WebCore::Length(clampTo<float>(primitiveValue->resolveAsPercentage(conversionData), minValueForCssLength, maxValueForCssLength), LengthType::Percent));

        if (primitiveValue->isCalculatedPercentageWithLength())
            return TextDecorationThickness::createWithLength(WebCore::Length(primitiveValue->cssCalcValue()->createCalculationValue(conversionData, CSSCalcSymbolTable { })));

        ASSERT(primitiveValue->isLength());
        return TextDecorationThickness::createWithLength(primitiveValue->resolveAsLength<WebCore::Length>(conversionData));
    }
    }
}

inline RefPtr<StyleReflection> BuilderConverter::convertReflection(BuilderState& builderState, const CSSValue& value)
{
    if (is<CSSPrimitiveValue>(value)) {
        ASSERT(value.valueID() == CSSValueNone);
        return nullptr;
    }

    auto* reflectValue = requiredDowncast<CSSReflectValue>(builderState, value);
    if (!reflectValue)
        return { };

    NinePieceImage mask(NinePieceImage::Type::Mask);
    mask.setFill(true);

    builderState.styleMap().mapNinePieceImage(reflectValue->mask(), mask);

    auto reflection = StyleReflection::create();
    reflection->setDirection(fromCSSValueID<ReflectionDirection>(reflectValue->direction()));
    reflection->setOffset(reflectValue->offset().convertToLength<FixedIntegerConversion | PercentConversion | CalculatedConversion>(builderState.cssToLengthConversionData()));
    reflection->setMask(mask);
    return reflection;
}

inline TextEdge BuilderConverter::convertTextEdge(BuilderState& builderState, const CSSValue& value)
{
    auto overValue = [&](CSSValueID valueID) {
        switch (valueID) {
        case CSSValueText:
            return TextEdgeType::Text;
        case CSSValueCap:
            return TextEdgeType::CapHeight;
        case CSSValueEx:
            return TextEdgeType::ExHeight;
        case CSSValueIdeographic:
            return TextEdgeType::CJKIdeographic;
        case CSSValueIdeographicInk:
            return TextEdgeType::CJKIdeographicInk;
        default:
            ASSERT_NOT_REACHED();
            return TextEdgeType::Auto;
        }
    };

    auto underValue = [&](CSSValueID valueID) {
        switch (valueID) {
        case CSSValueText:
            return TextEdgeType::Text;
        case CSSValueAlphabetic:
            return TextEdgeType::Alphabetic;
        case CSSValueIdeographic:
            return TextEdgeType::CJKIdeographic;
        case CSSValueIdeographicInk:
            return TextEdgeType::CJKIdeographicInk;
        default:
            ASSERT_NOT_REACHED();
            return TextEdgeType::Auto;
        }
    };

    // One value was given.
    if (is<CSSPrimitiveValue>(value)) {
        switch (value.valueID()) {
        case CSSValueAuto:
            return { TextEdgeType::Auto, TextEdgeType::Auto };
        case CSSValueLeading:
            return { TextEdgeType::Leading, TextEdgeType::Leading };
            // https://www.w3.org/TR/css-inline-3/#text-edges
            // "If only one value is specified, both edges are assigned that same keyword if possible; else text is assumed as the missing value."
        case CSSValueCap:
        case CSSValueEx:
            return { overValue(value.valueID()), TextEdgeType::Text };
        default:
            return { overValue(value.valueID()), underValue(value.valueID()) };
        }
        }

    // Two values were given.
    auto pair = requiredPairDowncast<CSSPrimitiveValue>(builderState, value);
    if (!pair)
        return { };

    return {
        overValue(pair->first.valueID()),
        underValue(pair->second.valueID())
    };
}

inline IntSize BuilderConverter::convertInitialLetter(BuilderState& builderState, const CSSValue& value)
{
    if (value.valueID() == CSSValueNormal)
        return IntSize();

    auto& conversionData = builderState.cssToLengthConversionData();

    auto pair = requiredPairDowncast<CSSPrimitiveValue>(builderState, value);
    if (!pair)
        return { };

    return {
        pair->first.resolveAsNumber<int>(conversionData),
        pair->second.resolveAsNumber<int>(conversionData)
    };
}

inline float BuilderConverter::convertTextStrokeWidth(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };

    float width = 0;
    switch (primitiveValue->valueID()) {
    case CSSValueThin:
    case CSSValueMedium:
    case CSSValueThick: {
        double result = 1.0 / 48;
        if (primitiveValue->valueID() == CSSValueMedium)
            result *= 3;
        else if (primitiveValue->valueID() == CSSValueThick)
            result *= 5;
        auto emsValue = CSSPrimitiveValue::create(result, CSSUnitType::CSS_EM);
        width = convertComputedLength<float>(builderState, emsValue);
        break;
    }
    case CSSValueInvalid: {
        width = convertComputedLength<float>(builderState, *primitiveValue);
        break;
    }
    default:
        ASSERT_NOT_REACHED();
        return 0;
    }

    return width;
}

inline OptionSet<LineBoxContain> BuilderConverter::convertLineBoxContain(BuilderState& builderState, const CSSValue& value)
{
    if (is<CSSPrimitiveValue>(value)) {
        ASSERT(value.valueID() == CSSValueNone);
        return { };
    }

    auto lineBoxContainValue = requiredDowncast<CSSLineBoxContainValue>(builderState, value);
    if (!lineBoxContainValue)
        return { };
    return lineBoxContainValue->value();
}

inline RefPtr<ShapeValue> BuilderConverter::convertShapeValue(BuilderState& builderState, const CSSValue& value)
{
    if (is<CSSPrimitiveValue>(value)) {
        ASSERT(value.valueID() == CSSValueNone);
        return nullptr;
    }

    if (value.isImage())
        return ShapeValue::create(builderState.createStyleImage(value).releaseNonNull());

    std::optional<Style::BasicShape> shape;
    auto referenceBox = CSSBoxType::BoxMissing;
    auto processSingleValue = [&](const CSSValue& currentValue) {
        if (RefPtr shapeValue = dynamicDowncast<CSSBasicShapeValue>(currentValue))
            shape = convertBasicShape(builderState, *shapeValue, 1);
        else
            referenceBox = fromCSSValue<CSSBoxType>(currentValue);
    };
    if (auto* list = dynamicDowncast<CSSValueList>(value)) {
        for (auto& currentValue : *list)
            processSingleValue(currentValue);
    } else
        processSingleValue(value);


    if (shape)
        return ShapeValue::create(WTFMove(*shape), referenceBox);

    if (referenceBox != CSSBoxType::BoxMissing)
        return ShapeValue::create(referenceBox);

    ASSERT_NOT_REACHED();
    return nullptr;
}

inline ScrollSnapType BuilderConverter::convertScrollSnapType(BuilderState& builderState, const CSSValue& value)
{
    ScrollSnapType type;
    auto list = requiredListDowncast<CSSValueList, CSSPrimitiveValue>(builderState, value);
    if (!list)
        return { };

    auto& firstValue = list->item(0);
    if (firstValue.valueID() == CSSValueNone)
        return type;

    type.axis = fromCSSValue<ScrollSnapAxis>(firstValue);
    if (list->size() == 2)
        type.strictness = fromCSSValue<ScrollSnapStrictness>(list->item(1));
    else
        type.strictness = ScrollSnapStrictness::Proximity;

    return type;
}

inline ScrollSnapAlign BuilderConverter::convertScrollSnapAlign(BuilderState& builderState, const CSSValue& value)
{
    auto list = requiredListDowncast<CSSValueList, CSSPrimitiveValue>(builderState, value);
    if (!list)
        return { };

    ScrollSnapAlign alignment;
    alignment.blockAlign = fromCSSValue<ScrollSnapAxisAlignType>(list->item(0));
    if (list->size() == 1)
        alignment.inlineAlign = alignment.blockAlign;
    else
        alignment.inlineAlign = fromCSSValue<ScrollSnapAxisAlignType>(list->item(1));
    return alignment;
}

inline ScrollSnapStop BuilderConverter::convertScrollSnapStop(BuilderState&, const CSSValue& value)
{
    return fromCSSValue<ScrollSnapStop>(value);
}

inline std::optional<ScrollbarColor> BuilderConverter::convertScrollbarColor(BuilderState& builderState, const CSSValue& value)
{
    if (is<CSSPrimitiveValue>(value)) {
        ASSERT(value.valueID() == CSSValueAuto);
        return std::nullopt;
    }

    auto* pair = requiredDowncast<CSSValuePair>(builderState, value);
    if (!pair)
        return { };

    return ScrollbarColor {
        builderState.createStyleColor(pair->first()),
        builderState.createStyleColor(pair->second()),
    };
}

inline ScrollbarGutter BuilderConverter::convertScrollbarGutter(BuilderState&, const CSSValue& value)
{
    ScrollbarGutter gutter;
    if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value)) {
        if (primitiveValue->valueID() == CSSValueStable)
            gutter.isAuto = false;
        return gutter;
    }

    ASSERT(value.isPair());

    gutter.isAuto = false;
    gutter.bothEdges = true;

    return gutter;
}

inline ScrollbarWidth BuilderConverter::convertScrollbarWidth(BuilderState& builderState, const CSSValue& value)
{
    ScrollbarWidth scrollbarWidth = fromCSSValueDeducingType(builderState, value);
    if (scrollbarWidth == ScrollbarWidth::Thin && builderState.document().quirks().needsScrollbarWidthThinDisabledQuirk())
        return ScrollbarWidth::Auto;

    return scrollbarWidth;
}

inline GridLength BuilderConverter::createGridTrackBreadth(BuilderState& builderState, const CSSPrimitiveValue& primitiveValue)
{
    if (primitiveValue.valueID() == CSSValueMinContent || primitiveValue.valueID() == CSSValueWebkitMinContent)
        return WebCore::Length(LengthType::MinContent);

    if (primitiveValue.valueID() == CSSValueMaxContent || primitiveValue.valueID() == CSSValueWebkitMaxContent)
        return WebCore::Length(LengthType::MaxContent);

    auto& conversionData = builderState.cssToLengthConversionData();

    // Fractional unit.
    if (primitiveValue.isFlex())
        return GridLength(primitiveValue.resolveAsFlex<double>(conversionData));

    auto length = primitiveValue.convertToLength<FixedIntegerConversion | PercentConversion | CalculatedConversion | AutoConversion>(conversionData);
    if (!length.isUndefined())
        return length;
    return WebCore::Length(0.0, LengthType::Fixed);
}

inline GridTrackSize BuilderConverter::createGridTrackSize(BuilderState& builderState, const CSSValue& value)
{
    if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value))
        return GridTrackSize(createGridTrackBreadth(builderState, *primitiveValue));

    auto function = requiredListDowncast<CSSFunctionValue, CSSPrimitiveValue>(builderState, value);
    if (!function)
        return { };

    if (function->size() == 1)
        return GridTrackSize(createGridTrackBreadth(builderState, function->item(0)), FitContentTrackSizing);

    GridLength minTrackBreadth(createGridTrackBreadth(builderState, function->item(0)));
    GridLength maxTrackBreadth(createGridTrackBreadth(builderState, function->item(1)));
    return GridTrackSize(minTrackBreadth, maxTrackBreadth);
}

inline std::optional<GridTrackList> BuilderConverter::createGridTrackList(BuilderState& builderState, const CSSValue& value)
{
    RefPtr<const CSSValueContainingVector> valueList;

    GridTrackList trackList;

    auto* subgridValue = dynamicDowncast<CSSSubgridValue>(value);
    if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value)) {
        if (primitiveValue->valueID() == CSSValueMasonry) {
            trackList.list.append(GridTrackEntryMasonry());
            return { WTFMove(trackList) };
        }
        if (primitiveValue->valueID() == CSSValueNone)
            return { WTFMove(trackList) };
    } else if (subgridValue) {
        valueList = subgridValue;
        trackList.list.append(GridTrackEntrySubgrid());
    } else if (auto* list = dynamicDowncast<CSSValueList>(value))
        valueList = list;
    else
        return std::nullopt;

    // https://drafts.csswg.org/css-grid-2/#computed-tracks
    // The computed track list of a non-subgrid axis is a list alternating between line name sets
    // and track sections, with the first and last items being line name sets.
    auto ensureLineNames = [&](auto& list) {
        if (subgridValue)
            return;
        if (list.isEmpty() || !std::holds_alternative<Vector<String>>(list.last()))
            list.append(Vector<String>());
    };

    auto buildRepeatList = [&](const CSSValue& repeatValue, RepeatTrackList& repeatList) {
        auto vectorValue = requiredDowncast<CSSValueContainingVector>(builderState, repeatValue);
        if (!vectorValue)
            return;
        for (auto& currentValue : *vectorValue) {
            if (auto* namesValue = dynamicDowncast<CSSGridLineNamesValue>(currentValue))
                repeatList.append(Vector<String>(namesValue->names()));
            else {
                ensureLineNames(repeatList);
                repeatList.append(createGridTrackSize(builderState, currentValue));
            }
        }

        if (!repeatList.isEmpty())
            ensureLineNames(repeatList);
    };

    auto addOne = [&](const CSSValue& currentValue) {
        if (auto* namesValue = dynamicDowncast<CSSGridLineNamesValue>(currentValue)) {
            trackList.list.append(Vector<String>(namesValue->names()));
            return;
        }

        ensureLineNames(trackList.list);

        if (auto* repeatValue = dynamicDowncast<CSSGridAutoRepeatValue>(currentValue)) {
            CSSValueID autoRepeatID = repeatValue->autoRepeatID();
            ASSERT(autoRepeatID == CSSValueAutoFill || autoRepeatID == CSSValueAutoFit);

            GridTrackEntryAutoRepeat repeat;
            repeat.type = autoRepeatID == CSSValueAutoFill ? AutoRepeatType::Fill : AutoRepeatType::Fit;

            buildRepeatList(currentValue, repeat.list);
            trackList.list.append(WTFMove(repeat));
        } else if (auto* repeatValue = dynamicDowncast<CSSGridIntegerRepeatValue>(currentValue)) {
            auto repetitions = clampTo(repeatValue->repetitions().resolveAsInteger(builderState.cssToLengthConversionData()), 1, GridPosition::max());

            GridTrackEntryRepeat repeat;
            repeat.repeats = repetitions;

            buildRepeatList(currentValue, repeat.list);
            trackList.list.append(WTFMove(repeat));
        } else
            trackList.list.append(createGridTrackSize(builderState, currentValue));
    };

    if (!valueList)
        addOne(value);
    else {
        for (auto& value : *valueList)
            addOne(value);
    }

    if (!trackList.list.isEmpty())
        ensureLineNames(trackList.list);

    return { WTFMove(trackList) };
}

inline GridPosition BuilderConverter::createGridPosition(BuilderState& builderState, const CSSValue& value)
{
    GridPosition position;

    // We accept the specification's grammar:
    // auto | <custom-ident> | [ <integer> && <custom-ident>? ] | [ span && [ <integer> || <custom-ident> ] ]
    if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value)) {
        if (primitiveValue->isCustomIdent()) {
            position.setNamedGridArea(primitiveValue->stringValue());
            return position;
        }

        ASSERT(primitiveValue->valueID() == CSSValueAuto);
        return position;
    }

    auto* gridLineValue = requiredDowncast<CSSGridLineValue>(builderState, value);
    if (!gridLineValue)
        return { };

    RefPtr uncheckedSpanValue = gridLineValue->spanValue();
    RefPtr uncheckedNumericValue = gridLineValue->numericValue();
    RefPtr uncheckedGridLineName = gridLineValue->gridLineName();

    auto gridLineNumber = uncheckedNumericValue && uncheckedNumericValue->isInteger() ? uncheckedNumericValue->resolveAsInteger(builderState.cssToLengthConversionData()) : 0;
    auto gridLineName = uncheckedGridLineName && uncheckedGridLineName->isCustomIdent() ? uncheckedGridLineName->stringValue() : String();

    if (uncheckedSpanValue && uncheckedSpanValue->valueID() == CSSValueSpan)
        position.setSpanPosition(gridLineNumber > 0 ? gridLineNumber : 1, gridLineName);
    else
        position.setExplicitPosition(gridLineNumber, gridLineName);

    return position;
}

inline NamedGridLinesMap BuilderConverter::createImplicitNamedGridLinesFromGridArea(BuilderState&, const NamedGridAreaMap& namedGridAreas, GridTrackSizingDirection direction)
{
    NamedGridLinesMap namedGridLines;

    for (auto& area : namedGridAreas.map) {
        GridSpan areaSpan = direction == GridTrackSizingDirection::ForRows ? area.value.rows : area.value.columns;
        {
            auto& startVector = namedGridLines.map.add(makeString(area.key, "-start"_s), Vector<unsigned>()).iterator->value;
            startVector.append(areaSpan.startLine());
            std::sort(startVector.begin(), startVector.end());
        }
        {
            auto& endVector = namedGridLines.map.add(makeString(area.key, "-end"_s), Vector<unsigned>()).iterator->value;
            endVector.append(areaSpan.endLine());
            std::sort(endVector.begin(), endVector.end());
        }
    }
    // FIXME: For acceptable performance, should sort once at the end, not as we add each item, or at least insert in sorted order instead of using std::sort each time.

    return namedGridLines;
}

inline Vector<GridTrackSize> BuilderConverter::convertGridTrackSizeList(BuilderState& builderState, const CSSValue& value)
{
    auto validateValue = [](const CSSValue& value) {
        ASSERT_UNUSED(value, !value.isGridLineNamesValue());
        ASSERT_UNUSED(value, !value.isGridAutoRepeatValue());
        ASSERT_UNUSED(value, !value.isGridIntegerRepeatValue());
    };

    if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value)) {
        if (primitiveValue->isValueID()) {
            ASSERT(primitiveValue->valueID() == CSSValueAuto);
        return RenderStyle::initialGridAutoRows();
    }
        // Values coming from CSS Typed OM may not have been converted to a CSSValueList yet.
        validateValue(*primitiveValue);
        return Vector<GridTrackSize>({ convertGridTrackSize(builderState, *primitiveValue) });
    }

    if (auto* valueList = dynamicDowncast<CSSValueList>(value))  {
        return WTF::map(*valueList, [&](auto& currentValue) {
            validateValue(currentValue);
            return convertGridTrackSize(builderState, currentValue);
        });
    }
    validateValue(value);
    return Vector<GridTrackSize>({ convertGridTrackSize(builderState, value) });
}

inline GridTrackSize BuilderConverter::convertGridTrackSize(BuilderState& builderState, const CSSValue& value)
{
    return createGridTrackSize(builderState, value);
}

inline std::optional<GridTrackList> BuilderConverter::convertGridTrackList(BuilderState& builderState, const CSSValue& value)
{
    return createGridTrackList(builderState, value);
}

inline GridPosition BuilderConverter::convertGridPosition(BuilderState& builderState, const CSSValue& value)
{
    return createGridPosition(builderState, value);
}

inline GridAutoFlow BuilderConverter::convertGridAutoFlow(BuilderState& builderState, const CSSValue& value)
{
    ASSERT(!is<CSSPrimitiveValue>(value) || downcast<CSSPrimitiveValue>(value).isValueID());

    auto* list = dynamicDowncast<CSSValueList>(value);
    if (list && !list->size())
            return RenderStyle::initialGridAutoFlow();

    auto* first = requiredDowncast<CSSPrimitiveValue>(builderState, list ? *(list->item(0)) : value);
    if (!first)
        return { };
    auto* second = dynamicDowncast<CSSPrimitiveValue>(list && list->size() == 2 ? list->item(1) : nullptr);

    GridAutoFlow autoFlow;
    switch (first->valueID()) {
    case CSSValueRow:
        if (second && second->valueID() == CSSValueDense)
            autoFlow = AutoFlowRowDense;
        else
            autoFlow = AutoFlowRow;
        break;
    case CSSValueColumn:
        if (second && second->valueID() == CSSValueDense)
            autoFlow = AutoFlowColumnDense;
        else
            autoFlow = AutoFlowColumn;
        break;
    case CSSValueDense:
        if (second && second->valueID() == CSSValueColumn)
            autoFlow = AutoFlowColumnDense;
        else
            autoFlow = AutoFlowRowDense;
        break;
    default:
        ASSERT_NOT_REACHED();
        autoFlow = RenderStyle::initialGridAutoFlow();
        break;
    }

    return autoFlow;
}

inline Vector<StyleContentAlignmentData> BuilderConverter::convertContentAlignmentDataList(BuilderState& builderState, const CSSValue& value)
{
    auto list = requiredListDowncast<CSSValueList, CSSContentDistributionValue>(builderState, value);
    if (!list)
        return { };

    return WTF::map(*list, [&](auto& value) {
        return convertContentAlignmentData(builderState, value);
    });
}

inline MasonryAutoFlow BuilderConverter::convertMasonryAutoFlow(BuilderState& builderState, const CSSValue& value)
{
    auto list = requiredListDowncast<CSSValueList, CSSPrimitiveValue>(builderState, value);
    if (!list)
        return { };

    if (!(list->size() == 1 || list->size() == 2))
        return RenderStyle::initialMasonryAutoFlow();

    auto& firstValue = list->item(0);
    auto* secondValue = list->size() == 2 ? &list->item(1) : nullptr;
    MasonryAutoFlow masonryAutoFlow;
    if (secondValue) {
        ASSERT(firstValue.valueID() == CSSValueID::CSSValuePack || firstValue.valueID() == CSSValueID::CSSValueNext);
        ASSERT(secondValue->valueID() == CSSValueID::CSSValueOrdered);
        if (firstValue.valueID() == CSSValueID::CSSValuePack)
            masonryAutoFlow = { MasonryAutoFlowPlacementAlgorithm::Pack, MasonryAutoFlowPlacementOrder::Ordered };
        else
            masonryAutoFlow = { MasonryAutoFlowPlacementAlgorithm::Next, MasonryAutoFlowPlacementOrder::Ordered };

    } else  {
        if (firstValue.valueID() == CSSValueID::CSSValuePack)
            masonryAutoFlow = { MasonryAutoFlowPlacementAlgorithm::Pack, MasonryAutoFlowPlacementOrder::DefiniteFirst };
        else if (firstValue.valueID() == CSSValueID::CSSValueNext)
            masonryAutoFlow = { MasonryAutoFlowPlacementAlgorithm::Next, MasonryAutoFlowPlacementOrder::DefiniteFirst };
        else if (firstValue.valueID() == CSSValueID::CSSValueOrdered)
            masonryAutoFlow = { MasonryAutoFlowPlacementAlgorithm::Pack, MasonryAutoFlowPlacementOrder::Ordered };
        else {
            ASSERT_NOT_REACHED();
            return RenderStyle::initialMasonryAutoFlow();
        }
    }
    return masonryAutoFlow;
}

inline float zoomWithTextZoomFactor(BuilderState& builderState)
{
    if (auto* frame = builderState.document().frame()) {
        float textZoomFactor = builderState.style().textZoom() != TextZoom::Reset ? frame->textZoomFactor() : 1.0f;
        return builderState.style().usedZoom() * textZoomFactor;
    }
    return builderState.cssToLengthConversionData().zoom();
}

inline CSSToLengthConversionData BuilderConverter::cssToLengthConversionDataWithTextZoomFactor(BuilderState& builderState)
{
    float zoom = zoomWithTextZoomFactor(builderState);
    if (zoom == builderState.cssToLengthConversionData().zoom())
        return builderState.cssToLengthConversionData();

    return builderState.cssToLengthConversionData().copyWithAdjustedZoom(zoom);
}

inline WebCore::Length BuilderConverter::convertTextLengthOrNormal(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };
    auto conversionData = (builderState.useSVGZoomRulesForLength())
        ? builderState.cssToLengthConversionData().copyWithAdjustedZoom(1.0f)
        : cssToLengthConversionDataWithTextZoomFactor(builderState);

    if (primitiveValue->valueID() == CSSValueNormal)
        return RenderStyle::zeroLength();
    if (primitiveValue->isLength())
        return primitiveValue->resolveAsLength<WebCore::Length>(conversionData);
    if (primitiveValue->isPercentage())
        return WebCore::Length(clampTo<float>(primitiveValue->resolveAsPercentage(conversionData), minValueForCssLength, maxValueForCssLength), LengthType::Percent);
    if (primitiveValue->isCalculatedPercentageWithLength())
        return WebCore::Length(primitiveValue->cssCalcValue()->createCalculationValue(conversionData, CSSCalcSymbolTable { }));
    if (primitiveValue->isNumber())
        return WebCore::Length(primitiveValue->resolveAsNumber(conversionData), LengthType::Fixed);
    ASSERT_NOT_REACHED();
    return RenderStyle::zeroLength();
}

inline std::optional<float> BuilderConverter::convertPerspective(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };

    if (primitiveValue->valueID() == CSSValueNone)
        return RenderStyle::initialPerspective();

    auto& conversionData = builderState.cssToLengthConversionData();

    float perspective = -1;
    if (primitiveValue->isLength())
        perspective = primitiveValue->resolveAsLength<float>(conversionData);
    else if (primitiveValue->isNumber())
        perspective = primitiveValue->resolveAsNumber<float>(conversionData) * conversionData.zoom();
    else
        ASSERT_NOT_REACHED();

    return perspective < 0 ? std::optional<float>(std::nullopt) : std::optional<float>(perspective);
}

inline std::optional<WebCore::Length> BuilderConverter::convertMarqueeIncrement(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };

    auto length = primitiveValue->convertToLength<FixedIntegerConversion | PercentConversion | CalculatedConversion>(builderState.cssToLengthConversionData().copyWithAdjustedZoom(1.0f));
    if (length.isUndefined())
        return std::nullopt;
    return length;
}

inline FilterOperations BuilderConverter::convertFilterOperations(BuilderState& builderState, const CSSValue& value)
{
    return builderState.createFilterOperations(value);
}

inline FilterOperations BuilderConverter::convertAppleColorFilterOperations(BuilderState& builderState, const CSSValue& value)
{
    return builderState.createAppleColorFilterOperations(value);
}

// The input value needs to parsed and valid, this function returns std::nullopt if the input was "normal".
inline std::optional<FontSelectionValue> BuilderConverter::convertFontStyleFromValue(BuilderState& builderState, const CSSValue& value)
{
    return Style::fontStyleFromCSSValue(builderState, value);
}

inline FontSelectionValue BuilderConverter::convertFontWeight(BuilderState& builderState, const CSSValue& value)
{
    return Style::fontWeightFromCSSValue(builderState, value);
}

inline FontSelectionValue BuilderConverter::convertFontWidth(BuilderState& builderState, const CSSValue& value)
{
    return Style::fontStretchFromCSSValue(builderState, value);
}

inline FontFeatureSettings BuilderConverter::convertFontFeatureSettings(BuilderState& builderState, const CSSValue& value)
{
    return Style::fontFeatureSettingsFromCSSValue(builderState, value);
}

inline FontVariationSettings BuilderConverter::convertFontVariationSettings(BuilderState& builderState, const CSSValue& value)
{
    return Style::fontVariationSettingsFromCSSValue(builderState, value);
}

inline FontSizeAdjust BuilderConverter::convertFontSizeAdjust(BuilderState& builderState, const CSSValue& value)
{
    return Style::fontSizeAdjustFromCSSValue(builderState, value);
}

#if PLATFORM(IOS_FAMILY)
inline bool BuilderConverter::convertTouchCallout(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };
    return !equalLettersIgnoringASCIICase(primitiveValue->stringValue(), "none"_s);
}
#endif

#if ENABLE(TOUCH_EVENTS)
inline Color BuilderConverter::convertTapHighlightColor(BuilderState& builderState, const CSSValue& value)
{
    return builderState.createStyleColor(value);
}
#endif

inline OptionSet<TouchAction> BuilderConverter::convertTouchAction(BuilderState&, const CSSValue& value)
{
    if (is<CSSPrimitiveValue>(value))
        return fromCSSValue<TouchAction>(value);

    if (auto* list = dynamicDowncast<CSSValueList>(value)) {
        OptionSet<TouchAction> touchActions;
        for (auto& currentValue : *list) {
            auto valueID = currentValue.valueID();
            if (valueID != CSSValuePanX && valueID != CSSValuePanY && valueID != CSSValuePinchZoom)
                return RenderStyle::initialTouchActions();
            touchActions.add(fromCSSValueID<TouchAction>(valueID));
        }
        return touchActions;
    }

    return RenderStyle::initialTouchActions();
}

#if ENABLE(OVERFLOW_SCROLLING_TOUCH)
inline bool BuilderConverter::convertOverflowScrolling(BuilderState&, const CSSValue& value)
{
    return value.valueID() == CSSValueTouch;
}
#endif

inline bool BuilderConverter::convertSmoothScrolling(BuilderState&, const CSSValue& value)
{
    return value.valueID() == CSSValueSmooth;
}

inline SVGLengthValue BuilderConverter::convertSVGLengthValue(BuilderState& builderState, const CSSValue& value, ShouldConvertNumberToPxLength shouldConvertNumberToPxLength)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };
    return SVGLengthValue::fromCSSPrimitiveValue(*primitiveValue, builderState.cssToLengthConversionData(), shouldConvertNumberToPxLength);
}

inline Vector<SVGLengthValue> BuilderConverter::convertSVGLengthVector(BuilderState& builderState, const CSSValue& value, ShouldConvertNumberToPxLength shouldConvertNumberToPxLength)
{
    auto list = requiredListDowncast<CSSValueList, CSSPrimitiveValue>(builderState, value);
    if (!list)
        return { };

    return WTF::map(*list, [&](auto& item) {
        return convertSVGLengthValue(builderState, item, shouldConvertNumberToPxLength);
    });
}

inline Vector<SVGLengthValue> BuilderConverter::convertStrokeDashArray(BuilderState& builderState, const CSSValue& value)
{
    if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value)) {
        if (primitiveValue->valueID() == CSSValueNone)
        return SVGRenderStyle::initialStrokeDashArray();

        // Values coming from CSS-Typed-OM may not have been converted to a CSSValueList yet.
        return Vector { convertSVGLengthValue(builderState, value, ShouldConvertNumberToPxLength::Yes) };
    }

    return convertSVGLengthVector(builderState, value, ShouldConvertNumberToPxLength::Yes);
}

inline PaintOrder BuilderConverter::convertPaintOrder(BuilderState& builderState, const CSSValue& value)
{
    if (is<CSSPrimitiveValue>(value)) {
        ASSERT(value.valueID() == CSSValueNormal);
        return PaintOrder::Normal;
    }

    auto list = requiredListDowncast<CSSValueList, CSSPrimitiveValue>(builderState, value);
    if (!list)
        return { };

    switch (list->item(0).valueID()) {
    case CSSValueFill:
        return list->size() > 1 ? PaintOrder::FillMarkers : PaintOrder::Fill;
    case CSSValueStroke:
        return list->size() > 1 ? PaintOrder::StrokeMarkers : PaintOrder::Stroke;
    case CSSValueMarkers:
        return list->size() > 1 ? PaintOrder::MarkersStroke : PaintOrder::Markers;
    default:
        ASSERT_NOT_REACHED();
        return PaintOrder::Normal;
    }
}

inline float BuilderConverter::convertOpacity(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };

    float opacity = primitiveValue->valueDividingBy100IfPercentage(builderState.cssToLengthConversionData());
    return std::max(0.0f, std::min(1.0f, opacity));
}

inline String BuilderConverter::convertSVGURIReference(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };

    if (primitiveValue->isURI())
        return primitiveValue->stringValue();
    return emptyString();
}

inline StyleSelfAlignmentData BuilderConverter::convertSelfOrDefaultAlignmentData(BuilderState&, const CSSValue& value)
{
    auto alignmentData = RenderStyle::initialSelfAlignment();
    if (value.isPair()) {
        if (value.first().valueID() == CSSValueLegacy) {
            alignmentData.setPositionType(ItemPositionType::Legacy);
            alignmentData.setPosition(fromCSSValue<ItemPosition>(value.second()));
        } else if (value.first().valueID() == CSSValueFirst)
            alignmentData.setPosition(ItemPosition::Baseline);
        else if (value.first().valueID() == CSSValueLast)
            alignmentData.setPosition(ItemPosition::LastBaseline);
        else {
            alignmentData.setOverflow(fromCSSValue<OverflowAlignment>(value.first()));
            alignmentData.setPosition(fromCSSValue<ItemPosition>(value.second()));
        }
    } else
        alignmentData.setPosition(fromCSSValue<ItemPosition>(value));
    return alignmentData;
}

inline StyleContentAlignmentData BuilderConverter::convertContentAlignmentData(BuilderState&, const CSSValue& value)
{
    StyleContentAlignmentData alignmentData = RenderStyle::initialContentAlignment();
    auto* contentValue = dynamicDowncast<CSSContentDistributionValue>(value);
    if (!contentValue)
        return alignmentData;
    if (contentValue->distribution() != CSSValueInvalid)
        alignmentData.setDistribution(fromCSSValueID<ContentDistribution>(contentValue->distribution()));
    if (contentValue->position() != CSSValueInvalid)
        alignmentData.setPosition(fromCSSValueID<ContentPosition>(contentValue->position()));
    if (contentValue->overflow() != CSSValueInvalid)
        alignmentData.setOverflow(fromCSSValueID<OverflowAlignment>(contentValue->overflow()));
    return alignmentData;
}

inline GlyphOrientation BuilderConverter::convertGlyphOrientation(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };

    float angle = std::abs(fmodf(primitiveValue->resolveAsAngle(builderState.cssToLengthConversionData()), 360.0f));
    if (angle <= 45.0f || angle > 315.0f)
        return GlyphOrientation::Degrees0;
    if (angle > 45.0f && angle <= 135.0f)
        return GlyphOrientation::Degrees90;
    if (angle > 135.0f && angle <= 225.0f)
        return GlyphOrientation::Degrees180;
    return GlyphOrientation::Degrees270;
}

inline GlyphOrientation BuilderConverter::convertGlyphOrientationOrAuto(BuilderState& builderState, const CSSValue& value)
{
    if (value.valueID() == CSSValueAuto)
        return GlyphOrientation::Auto;
    return convertGlyphOrientation(builderState, value);
}

inline WebCore::Length BuilderConverter::convertLineHeight(BuilderState& builderState, const CSSValue& value, float multiplier)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };

    auto valueID = primitiveValue->valueID();
    if (valueID == CSSValueNormal)
        return RenderStyle::initialLineHeight();

    if (CSSPropertyParserHelpers::isSystemFontShorthand(valueID))
        return RenderStyle::initialLineHeight();

        auto conversionData = builderState.cssToLengthConversionData().copyForLineHeight(zoomWithTextZoomFactor(builderState));

    if (primitiveValue->isLength() || primitiveValue->isCalculatedPercentageWithLength()) {
        WebCore::Length length;
        if (primitiveValue->isLength())
            length = primitiveValue->resolveAsLength<WebCore::Length>(conversionData);
        else {
            auto value = primitiveValue->cssCalcValue()->createCalculationValue(conversionData, CSSCalcSymbolTable { })->evaluate(builderState.style().computedFontSize());
            length = { clampTo<float>(value, minValueForCssLength, maxValueForCssLength), LengthType::Fixed };
        }
        if (multiplier != 1.f)
            length = WebCore::Length(length.value() * multiplier, LengthType::Fixed);
        return length;
    }

    // Line-height percentages need to inherit as if they were Fixed pixel values. In the example:
    // <div style="font-size: 10px; line-height: 150%;"><div style="font-size: 100px;"></div></div>
    // the inner element should have line-height of 15px. However, in this example:
    // <div style="font-size: 10px; line-height: 1.5;"><div style="font-size: 100px;"></div></div>
    // the inner element should have a line-height of 150px. Therefore, we map percentages to Fixed
    // values and raw numbers to percentages.
    if (primitiveValue->isPercentage()) {
        // FIXME: percentage should not be restricted to an integer here.
        return WebCore::Length((builderState.style().computedFontSize() * primitiveValue->resolveAsPercentage<int>(conversionData)) / 100, LengthType::Fixed);
    }

    ASSERT(primitiveValue->isNumber());
    return WebCore::Length(primitiveValue->resolveAsNumber(conversionData) * 100.0, LengthType::Percent);
}

inline FontPalette BuilderConverter::convertFontPalette(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };

    switch (primitiveValue->valueID()) {
    case CSSValueLight:
        return { FontPalette::Type::Light, nullAtom() };
    case CSSValueDark:
        return { FontPalette::Type::Dark, nullAtom() };
    case CSSValueInvalid:
        ASSERT(primitiveValue->isCustomIdent());
        return { FontPalette::Type::Custom, AtomString { primitiveValue->stringValue() } };
    default:
        ASSERT(primitiveValue->valueID() == CSSValueNormal || CSSPropertyParserHelpers::isSystemFontShorthand(primitiveValue->valueID()));
        return { FontPalette::Type::Normal, nullAtom() };
    }
}

inline OptionSet<SpeakAs> BuilderConverter::convertSpeakAs(BuilderState&, const CSSValue& value)
{
    auto result = RenderStyle::initialSpeakAs();
    if (auto* list = dynamicDowncast<CSSValueList>(value)) {
        for (auto& currentValue : *list) {
            if (!isValueID(currentValue, CSSValueNormal))
                result.add(fromCSSValue<SpeakAs>(currentValue));
        }
    }
    return result;
}

inline OptionSet<HangingPunctuation> BuilderConverter::convertHangingPunctuation(BuilderState&, const CSSValue& value)
{
    auto result = RenderStyle::initialHangingPunctuation();
    if (auto* list = dynamicDowncast<CSSValueList>(value)) {
        for (auto& currentValue : *list)
            result.add(fromCSSValue<HangingPunctuation>(currentValue));
    }
    return result;
}

inline GapLength BuilderConverter::convertGapLength(BuilderState& builderState, const CSSValue& value)
{
    return (value.valueID() == CSSValueNormal) ? GapLength() : GapLength(convertLength(builderState, value));
}

inline OffsetRotation BuilderConverter::convertOffsetRotate(BuilderState& builderState, const CSSValue& value)
{
    RefPtr<const CSSPrimitiveValue> modifierValue;
    RefPtr<const CSSPrimitiveValue> angleValue;

    if (auto* offsetRotateValue = dynamicDowncast<CSSOffsetRotateValue>(value)) {
        modifierValue = offsetRotateValue->modifier();
        angleValue = offsetRotateValue->angle();
    } else if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value)) {
        // Values coming from CSSTypedOM didn't go through the parser and may not have been converted to a CSSOffsetRotateValue.
        if (primitiveValue->valueID() == CSSValueAuto || primitiveValue->valueID() == CSSValueReverse)
            modifierValue = primitiveValue;
        else if (primitiveValue->isAngle())
            angleValue = primitiveValue;
    }

    bool hasAuto = false;
    float angleInDegrees = 0;

    if (angleValue)
        angleInDegrees = angleValue->resolveAsAngle<float>(builderState.cssToLengthConversionData());

    if (modifierValue) {
        switch (modifierValue->valueID()) {
        case CSSValueAuto:
            hasAuto = true;
            break;
        case CSSValueReverse:
            hasAuto = true;
            angleInDegrees += 180.0;
            break;
        default:
            ASSERT_NOT_REACHED();
        }
    }

    return OffsetRotation(hasAuto, angleInDegrees);
}

inline Vector<Style::ScopedName> BuilderConverter::convertContainerName(BuilderState& builderState, const CSSValue& value)
{
    if (is<CSSPrimitiveValue>(value)) {
        ASSERT(value.valueID() == CSSValueNone);
        return { };
    }
    auto list = requiredListDowncast<CSSValueList, CSSPrimitiveValue>(builderState, value);
    if (!list)
        return { };

    return WTF::map(*list, [&](auto& item) {
        return Style::ScopedName { AtomString { item.stringValue() }, builderState.styleScopeOrdinal() };
    });
}

inline OptionSet<MarginTrimType> BuilderConverter::convertMarginTrim(BuilderState&, const CSSValue& value)
{
    // See if value is "block" or "inline" before trying to parse a list
    if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value)) {
        if (primitiveValue->valueID() == CSSValueBlock)
            return { MarginTrimType::BlockStart, MarginTrimType::BlockEnd };
        if (primitiveValue->valueID() == CSSValueInline)
            return { MarginTrimType::InlineStart, MarginTrimType::InlineEnd };
    }
    auto list = dynamicDowncast<CSSValueList>(value);
    if (!list || !list->size())
        return RenderStyle::initialMarginTrim();
    OptionSet<MarginTrimType> marginTrim;
    ASSERT(list->size() <= 4);
    for (auto& item : *list) {
        if (item.valueID() == CSSValueBlockStart)
            marginTrim.add(MarginTrimType::BlockStart);
        if (item.valueID() == CSSValueBlockEnd)
            marginTrim.add(MarginTrimType::BlockEnd);
        if (item.valueID() == CSSValueInlineStart)
            marginTrim.add(MarginTrimType::InlineStart);
        if (item.valueID() == CSSValueInlineEnd)
            marginTrim.add(MarginTrimType::InlineEnd);
    }
    return marginTrim;
}

inline TextSpacingTrim BuilderConverter::convertTextSpacingTrim(BuilderState&, const CSSValue& value)
{
    if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value)) {
        switch (primitiveValue->valueID()) {
        case CSSValueSpaceAll:
            return TextSpacingTrim::TrimType::SpaceAll;
        case CSSValueTrimAll:
            return TextSpacingTrim::TrimType::TrimAll;
        case CSSValueAuto:
            return TextSpacingTrim::TrimType::Auto;
        default:
            ASSERT_NOT_REACHED();
            break;
        }
    }
    return { };
}

inline TextAutospace BuilderConverter::convertTextAutospace(BuilderState& builderState, const CSSValue& value)
{
    if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value)) {
        if (primitiveValue->valueID() == CSSValueNoAutospace)
    return { };
        if (primitiveValue->valueID() == CSSValueAuto)
            return { TextAutospace::Type::Auto };
        if (primitiveValue->valueID() == CSSValueNormal)
            return { TextAutospace::Type::Normal };
    }

    TextAutospace::Options options;

    auto list = requiredListDowncast<CSSValueList, CSSPrimitiveValue>(builderState, value);
    if (!list)
        return { };

    for (auto& value : *list) {
        switch (value.valueID()) {
        case CSSValueIdeographAlpha:
            options.add(TextAutospace::Type::IdeographAlpha);
            break;
        case CSSValueIdeographNumeric:
            options.add(TextAutospace::Type::IdeographNumeric);
            break;
        default:
            ASSERT_NOT_REACHED();
            break;
        }
    }
    return options;
}

inline std::optional<WebCore::Length> BuilderConverter::convertBlockStepSize(BuilderState& builderState, const CSSValue& value)
{
    if (value.valueID() == CSSValueNone)
        return { };
    return convertLength(builderState, value);
}

inline OptionSet<Containment> BuilderConverter::convertContain(BuilderState& builderState, const CSSValue& value)
{
    if (is<CSSPrimitiveValue>(value)) {
        if (value.valueID() == CSSValueNone)
            return RenderStyle::initialContainment();
        if (value.valueID() == CSSValueStrict)
            return RenderStyle::strictContainment();
        return RenderStyle::contentContainment();
    }

    OptionSet<Containment> containment;

    auto list = requiredListDowncast<CSSValueList, CSSPrimitiveValue>(builderState, value);
    if (!list)
        return { };

    for (auto& value : *list) {
        switch (value.valueID()) {
        case CSSValueSize:
            containment.add(Containment::Size);
            break;
        case CSSValueInlineSize:
            containment.add(Containment::InlineSize);
            break;
        case CSSValueLayout:
            containment.add(Containment::Layout);
            break;
        case CSSValuePaint:
            containment.add(Containment::Paint);
            break;
        case CSSValueStyle:
            containment.add(Containment::Style);
            break;
        default:
            ASSERT_NOT_REACHED();
            break;
        };
    }
    return containment;
}

inline Vector<Style::ScopedName> BuilderConverter::convertViewTransitionClass(BuilderState& builderState, const CSSValue& value)
{
    if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value)) {
        if (value.valueID() == CSSValueNone)
            return { };
        return { Style::ScopedName { AtomString { primitiveValue->stringValue() }, builderState.styleScopeOrdinal() } };
    }

    auto list = requiredListDowncast<CSSValueList, CSSPrimitiveValue>(builderState, value);
    if (!list)
        return { };

    return WTF::map(*list, [&](auto& item) {
        return Style::ScopedName { AtomString { item.stringValue() }, builderState.styleScopeOrdinal() };
    });
}

inline Style::ViewTransitionName BuilderConverter::convertViewTransitionName(BuilderState& state, const CSSValue& value)
{
    auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value);
    if (!primitiveValue)
        return Style::ViewTransitionName::createWithNone();

    if (value.valueID() == CSSValueNone)
        return Style::ViewTransitionName::createWithNone();

    if (value.valueID() == CSSValueAuto)
        return Style::ViewTransitionName::createWithAuto(state.styleScopeOrdinal());

    if (value.valueID() == CSSValueMatchElement)
        return Style::ViewTransitionName::createWithMatchElement(state.styleScopeOrdinal());

    return Style::ViewTransitionName::createWithCustomIdent(state.styleScopeOrdinal(), AtomString { primitiveValue->stringValue() });
}

inline RefPtr<WillChangeData> BuilderConverter::convertWillChange(BuilderState& builderState, const CSSValue& value)
{
    if (value.valueID() == CSSValueAuto)
        return nullptr;

    auto willChange = WillChangeData::create();
    auto processSingleValue = [&](const CSSValue& item) {
        auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(item);
        if (!primitiveValue)
            return;
        switch (primitiveValue->valueID()) {
        case CSSValueScrollPosition:
            willChange->addFeature(WillChangeData::Feature::ScrollPosition);
            break;
        case CSSValueContents:
            willChange->addFeature(WillChangeData::Feature::Contents);
            break;
        default:
            if (primitiveValue->isPropertyID()) {
                if (!isExposed(primitiveValue->propertyID(), &builderState.document().settings()))
                    break;
                willChange->addFeature(WillChangeData::Feature::Property, primitiveValue->propertyID());
            }
            break;
        }
    };
    if (auto* list = dynamicDowncast<CSSValueList>(value)) {
        for (auto& item : *list)
            processSingleValue(item);
    } else
        processSingleValue(value);
    return willChange;
}

inline Vector<AtomString> BuilderConverter::convertScrollTimelineName(BuilderState& builderState, const CSSValue& value)
{
    if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value)) {
        if (value.valueID() == CSSValueNone)
            return { };
        return { AtomString { primitiveValue->stringValue() } };
    }

    auto list = requiredListDowncast<CSSValueList, CSSPrimitiveValue>(builderState, value);
    if (!list)
        return { };

    return WTF::map(*list, [&](auto& item) {
        return AtomString { item.stringValue() };
    });
}

inline Vector<ScrollAxis> BuilderConverter::convertScrollTimelineAxis(BuilderState& builderState, const CSSValue& value)
{
    if (is<CSSPrimitiveValue>(value))
        return { fromCSSValueID<ScrollAxis>(value.valueID()) };

    auto list = requiredListDowncast<CSSValueList, CSSPrimitiveValue>(builderState, value);
    if (!list)
        return { };

    return WTF::map(*list, [&](auto& item) {
        return fromCSSValueID<ScrollAxis>(item.valueID());
    });
}

inline Vector<ViewTimelineInsets> BuilderConverter::convertViewTimelineInset(BuilderState& builderState, const CSSValue& value)
{
    // While parsing, consumeViewTimelineInset() and consumeViewTimelineShorthand() yield a CSSValueList exclusively.
    auto list = requiredListDowncast<CSSValueList, CSSValue>(builderState, value);
    if (!list)
        return { };

    return WTF::map(*list, [&](auto& item) -> ViewTimelineInsets {
        // Each item is either a single value or a CSSValuePair.
        if (auto* pair = dynamicDowncast<CSSValuePair>(item))
            return { convertLengthOrAuto(builderState, pair->first()), convertLengthOrAuto(builderState, pair->second()) };
        if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(item))
            return { convertLengthOrAuto(builderState, *primitiveValue), std::nullopt };
        return { };
    });
}

inline Vector<ScopedName> BuilderConverter::convertAnchorName(BuilderState& builderState, const CSSValue& value)
{
    if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value)) {
        if (value.valueID() == CSSValueNone)
            return { };

        ScopedName scopedName {
            .name = AtomString { primitiveValue->stringValue() },
            .scopeOrdinal = builderState.styleScopeOrdinal()
        };

        return { scopedName };
    }

    auto list = requiredListDowncast<CSSValueList, CSSPrimitiveValue>(builderState, value);
    if (!list)
        return { };

    return WTF::map(*list, [&](auto& item) {
        return ScopedName {
            .name = AtomString { item.stringValue() },
            .scopeOrdinal = builderState.styleScopeOrdinal()
        };
    });
}

inline std::optional<ScopedName> BuilderConverter::convertPositionAnchor(BuilderState& builderState, const CSSValue& value)
{
    if (value.valueID() == CSSValueAuto)
        return { };

    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };

    return ScopedName {
        .name = AtomString { primitiveValue->stringValue() },
        .scopeOrdinal = builderState.styleScopeOrdinal()
    };
}

static std::optional<PositionAreaAxis> positionAreaKeywordToAxis(CSSValueID keyword)
{
    switch (keyword) {
    case CSSValueLeft:
    case CSSValueSpanLeft:
    case CSSValueRight:
    case CSSValueSpanRight:
        return PositionAreaAxis::Horizontal;

    case CSSValueTop:
    case CSSValueSpanTop:
    case CSSValueBottom:
    case CSSValueSpanBottom:
        return PositionAreaAxis::Vertical;

    case CSSValueXStart:
    case CSSValueSpanXStart:
    case CSSValueXSelfStart:
    case CSSValueSpanXSelfStart:
    case CSSValueXEnd:
    case CSSValueSpanXEnd:
    case CSSValueXSelfEnd:
    case CSSValueSpanXSelfEnd:
        return PositionAreaAxis::X;

    case CSSValueYStart:
    case CSSValueSpanYStart:
    case CSSValueYSelfStart:
    case CSSValueSpanYSelfStart:
    case CSSValueYEnd:
    case CSSValueSpanYEnd:
    case CSSValueYSelfEnd:
    case CSSValueSpanYSelfEnd:
        return PositionAreaAxis::Y;

    case CSSValueBlockStart:
    case CSSValueSpanBlockStart:
    case CSSValueSelfBlockStart:
    case CSSValueSpanSelfBlockStart:
    case CSSValueBlockEnd:
    case CSSValueSpanBlockEnd:
    case CSSValueSelfBlockEnd:
    case CSSValueSpanSelfBlockEnd:
        return PositionAreaAxis::Block;

    case CSSValueInlineStart:
    case CSSValueSpanInlineStart:
    case CSSValueSelfInlineStart:
    case CSSValueSpanSelfInlineStart:
    case CSSValueInlineEnd:
    case CSSValueSpanInlineEnd:
    case CSSValueSelfInlineEnd:
    case CSSValueSpanSelfInlineEnd:
        return PositionAreaAxis::Inline;

    case CSSValueStart:
    case CSSValueSpanStart:
    case CSSValueSelfStart:
    case CSSValueSpanSelfStart:
    case CSSValueEnd:
    case CSSValueSpanEnd:
    case CSSValueSelfEnd:
    case CSSValueSpanSelfEnd:
    case CSSValueCenter:
    case CSSValueSpanAll:
        return { };

    default:
        ASSERT_NOT_REACHED();
        return { };
    }
}

static PositionAreaTrack positionAreaKeywordToTrack(CSSValueID keyword)
{
    switch (keyword) {
    case CSSValueLeft:
    case CSSValueTop:
    case CSSValueXStart:
    case CSSValueXSelfStart:
    case CSSValueYStart:
    case CSSValueYSelfStart:
    case CSSValueBlockStart:
    case CSSValueSelfBlockStart:
    case CSSValueInlineStart:
    case CSSValueSelfInlineStart:
    case CSSValueStart:
    case CSSValueSelfStart:
        return PositionAreaTrack::Start;

    case CSSValueSpanLeft:
    case CSSValueSpanTop:
    case CSSValueSpanXStart:
    case CSSValueSpanXSelfStart:
    case CSSValueSpanYStart:
    case CSSValueSpanYSelfStart:
    case CSSValueSpanBlockStart:
    case CSSValueSpanSelfBlockStart:
    case CSSValueSpanInlineStart:
    case CSSValueSpanSelfInlineStart:
    case CSSValueSpanStart:
    case CSSValueSpanSelfStart:
        return PositionAreaTrack::SpanStart;

    case CSSValueRight:
    case CSSValueBottom:
    case CSSValueXEnd:
    case CSSValueXSelfEnd:
    case CSSValueYEnd:
    case CSSValueYSelfEnd:
    case CSSValueBlockEnd:
    case CSSValueSelfBlockEnd:
    case CSSValueInlineEnd:
    case CSSValueSelfInlineEnd:
    case CSSValueEnd:
    case CSSValueSelfEnd:
        return PositionAreaTrack::End;

    case CSSValueSpanRight:
    case CSSValueSpanBottom:
    case CSSValueSpanXEnd:
    case CSSValueSpanXSelfEnd:
    case CSSValueSpanYEnd:
    case CSSValueSpanYSelfEnd:
    case CSSValueSpanBlockEnd:
    case CSSValueSpanSelfBlockEnd:
    case CSSValueSpanInlineEnd:
    case CSSValueSpanSelfInlineEnd:
    case CSSValueSpanEnd:
    case CSSValueSpanSelfEnd:
        return PositionAreaTrack::SpanEnd;

    case CSSValueCenter:
        return PositionAreaTrack::Center;
    case CSSValueSpanAll:
        return PositionAreaTrack::SpanAll;

    default:
        ASSERT_NOT_REACHED();
        return PositionAreaTrack::Start;
    }
}

static PositionAreaSelf positionAreaKeywordToSelf(CSSValueID keyword)
{
    switch (keyword) {
    case CSSValueLeft:
    case CSSValueSpanLeft:
    case CSSValueRight:
    case CSSValueSpanRight:
    case CSSValueTop:
    case CSSValueSpanTop:
    case CSSValueBottom:
    case CSSValueSpanBottom:
    case CSSValueXStart:
    case CSSValueSpanXStart:
    case CSSValueXEnd:
    case CSSValueSpanXEnd:
    case CSSValueYStart:
    case CSSValueSpanYStart:
    case CSSValueYEnd:
    case CSSValueSpanYEnd:
    case CSSValueBlockStart:
    case CSSValueSpanBlockStart:
    case CSSValueBlockEnd:
    case CSSValueSpanBlockEnd:
    case CSSValueInlineStart:
    case CSSValueSpanInlineStart:
    case CSSValueInlineEnd:
    case CSSValueSpanInlineEnd:
    case CSSValueStart:
    case CSSValueSpanStart:
    case CSSValueEnd:
    case CSSValueSpanEnd:
    case CSSValueCenter:
    case CSSValueSpanAll:
        return PositionAreaSelf::No;

    case CSSValueXSelfStart:
    case CSSValueSpanXSelfStart:
    case CSSValueXSelfEnd:
    case CSSValueSpanXSelfEnd:
    case CSSValueYSelfStart:
    case CSSValueSpanYSelfStart:
    case CSSValueYSelfEnd:
    case CSSValueSpanYSelfEnd:
    case CSSValueSelfBlockStart:
    case CSSValueSpanSelfBlockStart:
    case CSSValueSelfBlockEnd:
    case CSSValueSpanSelfBlockEnd:
    case CSSValueSelfInlineStart:
    case CSSValueSpanSelfInlineStart:
    case CSSValueSelfInlineEnd:
    case CSSValueSpanSelfInlineEnd:
    case CSSValueSelfStart:
    case CSSValueSpanSelfStart:
    case CSSValueSelfEnd:
    case CSSValueSpanSelfEnd:
        return PositionAreaSelf::Yes;

    default:
        ASSERT_NOT_REACHED();
        return PositionAreaSelf::No;
    }
}

// Expand a one keyword position-area to the equivalent keyword pair value.
static std::pair<CSSValueID, CSSValueID> positionAreaExpandKeyword(CSSValueID dim)
{
    auto maybeAxis = positionAreaKeywordToAxis(dim);
    if (maybeAxis) {
        // Keyword is axis unambiguous, second keyword is span-all.

        // Y/inline axis keyword goes after in the pair.
        auto axis = *maybeAxis;
        if (axis == PositionAreaAxis::Vertical || axis == PositionAreaAxis::Y || axis == PositionAreaAxis::Inline)
            return { CSSValueSpanAll, dim };

        return { dim, CSSValueSpanAll };
    }

    // Keyword is axis ambiguous, it's repeated.
    return { dim, dim };
}

// Get the opposite axis of a given axis. Used to resolve the axis of an axis ambiguous
// keyword, as its axis is the opposite of the other keyword in the pair.
static PositionAreaAxis positionAreaOppositeAxis(PositionAreaAxis axis)
{
    switch (axis) {
    case PositionAreaAxis::Horizontal:
        return PositionAreaAxis::Vertical;
    case PositionAreaAxis::Vertical:
        return PositionAreaAxis::Horizontal;

    case PositionAreaAxis::X:
        return PositionAreaAxis::Y;
    case PositionAreaAxis::Y:
        return PositionAreaAxis::X;

    case PositionAreaAxis::Block:
        return PositionAreaAxis::Inline;
    case PositionAreaAxis::Inline:
        return PositionAreaAxis::Block;
    }

    ASSERT_NOT_REACHED();
    return PositionAreaAxis::Horizontal;
}

inline std::optional<PositionArea> BuilderConverter::convertPositionArea(BuilderState&, const CSSValue& value)
{
    std::pair<CSSValueID, CSSValueID> dimPair;

    if (value.isValueID()) {
        if (value.valueID() == CSSValueNone)
            return { };

        dimPair = positionAreaExpandKeyword(value.valueID());
    } else if (const auto* pair = dynamicDowncast<CSSValuePair>(value)) {
        const auto& first = pair->first();
        const auto& second = pair->second();
        ASSERT(first.isValueID() && second.isValueID());

        // The parsing logic guarantees the keyword pair is in the correct order
        // (horizontal/x/block axis before vertical/Y/inline axis)

        dimPair = { first.valueID(), second.valueID() };
    } else {
        // value MUST be a single ValueID or a pair of ValueIDs, as returned by the parsing logic.
        ASSERT_NOT_REACHED();
        return { };
    }

    auto dim1Axis = positionAreaKeywordToAxis(dimPair.first);
    auto dim2Axis = positionAreaKeywordToAxis(dimPair.second);

    // If both keyword axes are ambiguous, the first one is block axis and second one
    // is inline axis. If only one keyword axis is ambiguous, its axis is the opposite
    // of the other keyword's axis.
    if (!dim1Axis && !dim2Axis) {
        dim1Axis = PositionAreaAxis::Block;
        dim2Axis = PositionAreaAxis::Inline;
    } else if (!dim1Axis)
        dim1Axis = positionAreaOppositeAxis(*dim2Axis);
    else if (!dim2Axis)
        dim2Axis = positionAreaOppositeAxis(*dim1Axis);

    return PositionArea {
        { *dim1Axis, positionAreaKeywordToTrack(dimPair.first), positionAreaKeywordToSelf(dimPair.first) },
        { *dim2Axis, positionAreaKeywordToTrack(dimPair.second), positionAreaKeywordToSelf(dimPair.second) }
    };
}

inline BlockEllipsis BuilderConverter::convertBlockEllipsis(BuilderState& builderState, const CSSValue& value)
{
    if (value.valueID() == CSSValueNone)
        return { };
    if (value.valueID() == CSSValueAuto)
        return { BlockEllipsis::Type::Auto, { } };
    return { BlockEllipsis::Type::String, AtomString { convertString(builderState, value) } };

}

inline size_t BuilderConverter::convertMaxLines(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };

    if (primitiveValue->valueID() == CSSValueNone)
        return 0;
    return convertNumber<size_t>(builderState, value);
}

inline LineClampValue BuilderConverter::convertLineClamp(BuilderState& builderState, const CSSValue& value)
{
    auto* primitiveValue = requiredDowncast<CSSPrimitiveValue>(builderState, value);
    if (!primitiveValue)
        return { };

    if (primitiveValue->primitiveType() == CSSUnitType::CSS_INTEGER)
        return LineClampValue(std::max(primitiveValue->resolveAsInteger<int>(builderState.cssToLengthConversionData()), 1), LineClamp::LineCount);

    if (primitiveValue->primitiveType() == CSSUnitType::CSS_PERCENTAGE)
        return LineClampValue(std::max(primitiveValue->resolveAsPercentage<int>(builderState.cssToLengthConversionData()), 0), LineClamp::Percentage);

    ASSERT(primitiveValue->valueID() == CSSValueNone);
    return LineClampValue();
}

inline RefPtr<TimingFunction> BuilderConverter::convertTimingFunction(BuilderState& builderState, const CSSValue& value)
{
    return Style::createTimingFunction(value, builderState.cssToLengthConversionData());
}

inline NameScope BuilderConverter::convertNameScope(BuilderState& builderState, const CSSValue& value)
{
    if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value)) {
        switch (primitiveValue->valueID()) {
        case CSSValueNone:
            return { };
        case CSSValueAll:
            return { NameScope::Type::All, { } };
        default:
            return { NameScope::Type::Ident, { AtomString { primitiveValue->stringValue() } } };
        }
    }

    auto list = requiredListDowncast<CSSValueList, CSSPrimitiveValue>(builderState, value);
    if (!list)
        return { };

    return { NameScope::Type::Ident, WTF::map(*list, [&](auto& item) {
        return AtomString { item.stringValue() };
    }) };
}

inline Vector<PositionTryFallback> BuilderConverter::convertPositionTryFallbacks(BuilderState& builderState, const CSSValue& value)
{
    auto fallbackForValueList = [&](const CSSValueList& valueList) -> std::optional<PositionTryFallback> {
        if (valueList.separator() != CSSValueList::SpaceSeparator)
            return { };

        auto fallback = PositionTryFallback { };

        for (auto& item : valueList) {
            if (item.isCustomIdent())
                fallback.positionTryRuleName = Style::ScopedName { AtomString { item.customIdent() }, builderState.styleScopeOrdinal() };
            else
                fallback.tactics.append(fromCSSValueID<PositionTryFallback::Tactic>(item.valueID()));
        }
        return fallback;
    };

    if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value)) {
        switch (primitiveValue->valueID()) {
        case CSSValueNone:
            return { };
        default:
            ASSERT_NOT_REACHED();
            return { };
        }
    }

    auto* list = dynamicDowncast<CSSValueList>(value);
    if (!list)
        return { };

    if (auto fallback = fallbackForValueList(*list))
        return { *fallback };

    return WTF::map(*list, [&](auto& item) {
        auto* itemList = dynamicDowncast<CSSValueList>(item);
        if (!itemList)
            return PositionTryFallback { };
        auto fallback = fallbackForValueList(*itemList);
        return fallback ? *fallback : PositionTryFallback { };
    });
}

inline Style::ScrollPaddingEdge BuilderConverter::convertScrollPaddingEdge(BuilderState& builderState, const CSSValue& value)
{
    return Style::scrollPaddingEdgeFromCSSValue(value, builderState);
}

inline Style::ScrollMarginEdge BuilderConverter::convertScrollMarginEdge(BuilderState& builderState, const CSSValue& value)
{
    return Style::scrollMarginEdgeFromCSSValue(value, builderState);
}

inline Style::DynamicRangeLimit BuilderConverter::convertDynamicRangeLimit(BuilderState& builderState, const CSSValue& value)
{
    if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(value)) {
        switch (primitiveValue->valueID()) {
        case CSSValueStandard:
            return Style::DynamicRangeLimit { CSS::Keyword::Standard { } };
        case CSSValueConstrainedHigh:
            return Style::DynamicRangeLimit { CSS::Keyword::ConstrainedHigh { } };
        case CSSValueNoLimit:
            return Style::DynamicRangeLimit { CSS::Keyword::NoLimit { } };
        default:
            break;
        }

        builderState.setCurrentPropertyInvalidAtComputedValueTime();
        return Style::DynamicRangeLimit { CSS::Keyword::NoLimit { } };
    }

    RefPtr dynamicRangeLimit = requiredDowncast<CSSDynamicRangeLimitValue>(builderState, value);
    if (!dynamicRangeLimit)
        return Style::DynamicRangeLimit { CSS::Keyword::NoLimit { } };

    return toStyle(dynamicRangeLimit->dynamicRangeLimit(), builderState);
}

} // namespace Style
} // namespace WebCore
