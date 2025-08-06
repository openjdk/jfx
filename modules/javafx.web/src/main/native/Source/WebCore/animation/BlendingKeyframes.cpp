/*
 * Copyright (C) 1999 Antti Koivisto (koivisto@kde.org)
 * Copyright (C) 2004, 2005, 2006, 2007, 2008 Apple Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this library; see the file COPYING.LIB.  If not, write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 *
 */

#include "config.h"
#include "BlendingKeyframes.h"

#include "Animation.h"
#include "CSSAnimation.h"
#include "CSSCustomPropertyValue.h"
#include "CSSKeyframeRule.h"
#include "CSSPrimitiveValue.h"
#include "CSSPropertyAnimation.h"
#include "CSSPropertyNames.h"
#include "CSSValue.h"
#include "CompositeOperation.h"
#include "ComputedStyleDependencies.h"
#include "Element.h"
#include "KeyframeEffect.h"
#include "RenderObject.h"
#include "RenderStyleInlines.h"
#include "StyleProperties.h"
#include "StyleResolver.h"
#include "TransformOperations.h"
#include "TranslateTransformOperation.h"
#include <wtf/ZippedRange.h>

namespace WebCore {

BlendingKeyframes::~BlendingKeyframes() = default;

void BlendingKeyframes::clear()
{
    m_keyframes.clear();
    m_properties.clear();
    m_propertiesSetToInherit.clear();
    m_propertiesSetToCurrentColor.clear();
    m_usesRelativeFontWeight = false;
    m_containsCSSVariableReferences = false;
    m_usesAnchorFunctions = false;
}

bool BlendingKeyframes::operator==(const BlendingKeyframes& o) const
{
    if (m_keyframes.size() != o.m_keyframes.size())
        return false;

    for (auto [keyframe1, keyframe2] : zippedRange(m_keyframes, o.m_keyframes)) {
        if (keyframe1.offset() != keyframe2.offset())
            return false;
        if (keyframe1.style() != keyframe2.style())
            return false;
    }

    return true;
}

void BlendingKeyframes::insert(BlendingKeyframe&& keyframe)
{
    if (!keyframe.usesRangeOffset() && (keyframe.offset() < 0 || keyframe.offset() > 1))
        return;

    analyzeKeyframe(keyframe);

    bool inserted = false;
    size_t i = 0;
    for (; i < m_keyframes.size(); ++i) {
        if (m_keyframes[i].offset() > keyframe.offset()) {
            // insert before
            m_keyframes.insert(i, WTFMove(keyframe));
            inserted = true;
            break;
        }
    }

    if (!inserted)
        m_keyframes.append(WTFMove(keyframe));

    auto& insertedKeyframe = m_keyframes[i];
    for (auto& property : insertedKeyframe.properties())
        m_properties.add(property);
}

bool BlendingKeyframes::hasImplicitKeyframes() const
{
    return size() && (m_keyframes[0].offset() || m_keyframes[size() - 1].offset() != 1);
}

bool BlendingKeyframes::hasImplicitKeyframeForProperty(AnimatableCSSProperty property) const
{
    return hasImplicitKeyframes() && (!m_explicitFromProperties.contains(property) || !m_explicitToProperties.contains(property));
}

void BlendingKeyframes::copyKeyframes(const BlendingKeyframes& other)
{
    for (auto& keyframe : other) {
        auto copy = keyframe;
        insert(WTFMove(copy));
    }
}

static const StyleRuleKeyframe& zeroPercentKeyframe()
{
    static LazyNeverDestroyed<Ref<StyleRuleKeyframe>> rule;
    static std::once_flag onceFlag;
    std::call_once(onceFlag, [] {
        rule.construct(StyleRuleKeyframe::create(MutableStyleProperties::create()));
        rule.get()->setKey({ CSSValueNormal, 0 });
    });
    return rule.get().get();
}

static const StyleRuleKeyframe& hundredPercentKeyframe()
{
    static LazyNeverDestroyed<Ref<StyleRuleKeyframe>> rule;
    static std::once_flag onceFlag;
    std::call_once(onceFlag, [] {
        rule.construct(StyleRuleKeyframe::create(MutableStyleProperties::create()));
        rule.get()->setKey({ CSSValueNormal, 1 });
    });
    return rule.get().get();
}

void BlendingKeyframes::fillImplicitKeyframes(const KeyframeEffect& effect, const RenderStyle& underlyingStyle)
{
    if (isEmpty())
        return;

    ASSERT(effect.target());
    auto& element = *effect.target();
    if (!element.isConnected())
        return;

    auto& styleResolver = element.styleResolver();

    // We need to establish which properties are implicit for 0% and 100%.
    // We start each list off with the full list of properties, and see if
    // any 0% and 100% keyframes specify them.
    UncheckedKeyHashSet<AnimatableCSSProperty> expectedExplicitProperties;
    UncheckedKeyHashSet<AnimatableCSSProperty> zeroKeyframeExplicitProperties;

    BlendingKeyframe* implicitZeroKeyframe = nullptr;

    auto isSuitableKeyframeForImplicitValues = [&](const BlendingKeyframe& keyframe) {
        if (keyframe.usesRangeOffset())
            return false;

        auto* timingFunction = keyframe.timingFunction();

        // If there is no timing function set on the keyframe, then it uses the element's
        // timing function, which makes this keyframe suitable.
        if (!timingFunction)
            return true;

        if (auto* cssAnimation = dynamicDowncast<CSSAnimation>(effect.animation())) {
            auto* animationWideTimingFunction = cssAnimation->backingAnimation().defaultTimingFunctionForKeyframes();
            // If we're dealing with a CSS Animation and if that CSS Animation's backing animation
            // has a default timing function set, then if that keyframe's timing function matches,
            // that keyframe is suitable.
            if (animationWideTimingFunction)
                return timingFunction == animationWideTimingFunction;
            // Otherwise, the keyframe will be suitable if its timing function matches the default.
            return timingFunction == &CubicBezierTimingFunction::defaultTimingFunction();
        }

        return false;
    };

    for (auto& keyframe : m_keyframes) {
        if (keyframe.offset() <= 0) {
            for (auto property : keyframe.properties())
                zeroKeyframeExplicitProperties.add(property);
            if (!implicitZeroKeyframe && isSuitableKeyframeForImplicitValues(keyframe))
                implicitZeroKeyframe = &keyframe;
        }
        if (keyframe.hasResolvedOffset())
            expectedExplicitProperties.formUnion(keyframe.properties());
    }

    auto addImplicitKeyframe = [&](double key, const UncheckedKeyHashSet<AnimatableCSSProperty>& implicitProperties, const StyleRuleKeyframe& keyframeRule, BlendingKeyframe* existingImplicitBlendingKeyframe) {
        // If we're provided an existing implicit keyframe, we need to add all the styles for the implicit properties.
        if (existingImplicitBlendingKeyframe) {
            ASSERT(existingImplicitBlendingKeyframe->style());
            auto keyframeStyle = RenderStyle::clonePtr(*existingImplicitBlendingKeyframe->style());
            for (auto property : implicitProperties) {
                CSSPropertyAnimation::blendProperty(effect, property, *keyframeStyle, underlyingStyle, underlyingStyle, 1, CompositeOperation::Replace);
                existingImplicitBlendingKeyframe->addProperty(property);
            }
            existingImplicitBlendingKeyframe->setStyle(WTFMove(keyframeStyle));
            return;
        }

        // Otherwise we create a new keyframe.
        BlendingKeyframe blendingKeyframe(key, { nullptr });
        blendingKeyframe.setStyle(styleResolver.styleForKeyframe(element, underlyingStyle, { nullptr }, keyframeRule, blendingKeyframe));
        for (auto property : implicitProperties)
            blendingKeyframe.addProperty(property);
        // Step 2 of https://drafts.csswg.org/css-animations-2/#keyframes defines the
        // default composite property as "replace" for CSS Animations.
        if (is<CSSAnimation>(effect.animation()))
            blendingKeyframe.setCompositeOperation(CompositeOperation::Replace);
        insert(WTFMove(blendingKeyframe));
    };

    auto zeroKeyframeImplicitProperties = expectedExplicitProperties.differenceWith(zeroKeyframeExplicitProperties);
    if (!zeroKeyframeImplicitProperties.isEmpty())
        addImplicitKeyframe(0, zeroKeyframeImplicitProperties, zeroPercentKeyframe(), implicitZeroKeyframe);

    UncheckedKeyHashSet<AnimatableCSSProperty> oneKeyframeExplicitProperties;
    BlendingKeyframe* implicitOneKeyframe = nullptr;

    for (auto& keyframe : m_keyframes) {
        if (keyframe.offset() >= 1) {
            for (auto property : keyframe.properties())
                oneKeyframeExplicitProperties.add(property);
            if (!implicitOneKeyframe && isSuitableKeyframeForImplicitValues(keyframe))
                implicitOneKeyframe = &keyframe;
        }
    }

    auto oneKeyframeImplicitProperties = expectedExplicitProperties.differenceWith(oneKeyframeExplicitProperties);
    if (!oneKeyframeImplicitProperties.isEmpty())
        addImplicitKeyframe(1, oneKeyframeImplicitProperties, hundredPercentKeyframe(), implicitOneKeyframe);
}

bool BlendingKeyframes::containsAnimatableCSSProperty() const
{
    for (auto property : m_properties) {
        if (CSSPropertyAnimation::isPropertyAnimatable(property))
            return true;
    }
    return false;
}

bool BlendingKeyframes::containsDirectionAwareProperty() const
{
    for (auto& keyframe : m_keyframes) {
        if (keyframe.containsDirectionAwareProperty())
            return true;
    }
    return false;
}

bool BlendingKeyframes::usesContainerUnits() const
{
    for (auto& keyframe : m_keyframes) {
        if (keyframe.style()->usesContainerUnits())
            return true;
    }
    return false;
}

void BlendingKeyframes::addProperty(const AnimatableCSSProperty& property)
{
    ASSERT(!std::holds_alternative<CSSPropertyID>(property) || std::get<CSSPropertyID>(property) != CSSPropertyCustom);
    m_properties.add(property);
}

bool BlendingKeyframes::containsProperty(const AnimatableCSSProperty& property) const
{
    return m_properties.contains(property);
}

bool BlendingKeyframes::usesRelativeFontWeight() const
{
    return m_usesRelativeFontWeight;
}

bool BlendingKeyframes::hasCSSVariableReferences() const
{
    return m_containsCSSVariableReferences;
}

bool BlendingKeyframes::hasColorSetToCurrentColor() const
{
    return m_propertiesSetToCurrentColor.contains(CSSPropertyColor);
}

bool BlendingKeyframes::hasPropertySetToCurrentColor() const
{
    return !m_propertiesSetToCurrentColor.isEmpty();
}

const UncheckedKeyHashSet<AnimatableCSSProperty>& BlendingKeyframes::propertiesSetToInherit() const
{
    return m_propertiesSetToInherit;
}

void BlendingKeyframes::updatePropertiesMetadata(const StyleProperties& properties)
{
    for (auto propertyReference : properties) {
        auto* cssValue = propertyReference.value();
        if (!cssValue)
            continue;

        if (!m_containsCSSVariableReferences && cssValue->hasVariableReferences())
            m_containsCSSVariableReferences = true;

        if (auto* primitiveValue = dynamicDowncast<CSSPrimitiveValue>(cssValue)) {
            auto propertyID = propertyReference.id();
            auto valueId = primitiveValue->valueID();

            if (valueId == CSSValueInherit)
                m_propertiesSetToInherit.add(propertyID);
            else if (valueId == CSSValueCurrentcolor)
                m_propertiesSetToCurrentColor.add(propertyID);
            else if (!m_usesRelativeFontWeight && propertyID == CSSPropertyFontWeight && (valueId == CSSValueBolder || valueId == CSSValueLighter))
                m_usesRelativeFontWeight = true;

            if (Style::AnchorPositionEvaluator::propertyAllowsAnchorFunction(propertyID) || Style::AnchorPositionEvaluator::propertyAllowsAnchorSizeFunction(propertyID)) {
                auto dependencies = cssValue->computedStyleDependencies();
                if (dependencies.anchors)
                    m_usesAnchorFunctions = true;
            }
        } else if (auto* customPropertyValue = dynamicDowncast<CSSCustomPropertyValue>(cssValue)) {
            if (customPropertyValue->isInherit())
                m_propertiesSetToInherit.add(customPropertyValue->name());
            else if (customPropertyValue->isCurrentColor())
                m_propertiesSetToCurrentColor.add(customPropertyValue->name());
        }
    }
}

void BlendingKeyframes::analyzeKeyframe(const BlendingKeyframe& keyframe)
{
    auto* style = keyframe.style();
    if (!style)
        return;

    auto analyzeSizeDependentTransform = [&] {
        if (m_hasWidthDependentTransform && m_hasHeightDependentTransform)
            return;

        if (keyframe.animatesProperty(CSSPropertyTransform)) {
            for (auto& operation : style->transform()) {
                if (RefPtr translate = dynamicDowncast<TranslateTransformOperation>(operation.get())) {
                    if (translate->x().isPercent())
                        m_hasWidthDependentTransform = true;
                    if (translate->y().isPercent())
                        m_hasHeightDependentTransform = true;
                }
            }
        }

        if (keyframe.animatesProperty(CSSPropertyTranslate)) {
            if (auto* translate = style->translate()) {
                if (translate->x().isPercent())
                    m_hasWidthDependentTransform = true;
                if (translate->y().isPercent())
                    m_hasHeightDependentTransform = true;
            }
        }
    };

    auto analyzeDiscreteTransformInterval = [&] {
        if (!m_hasDiscreteTransformInterval && keyframe.animatesProperty(CSSPropertyTransform))
            m_hasDiscreteTransformInterval = style->transform().containsNonInvertibleMatrix({ });
    };

    auto analyzeExplicitlyInheritedKeyframeProperty = [&] {
        if (!m_hasExplicitlyInheritedKeyframeProperty)
            m_hasExplicitlyInheritedKeyframeProperty = style->hasExplicitlyInheritedProperties();
    };

    auto analyzeKeyframeForExplicitProperties = [&] {
        auto& properties = keyframe.properties();
        if (!keyframe.offset())
            m_explicitFromProperties.add(properties.begin(), properties.end());
        if (keyframe.offset() == 1)
            m_explicitToProperties.add(properties.begin(), properties.end());
    };

    auto analyzeKeyframeRangeOffset = [&] {
        if (!m_hasKeyframeNotUsingRangeOffset)
            m_hasKeyframeNotUsingRangeOffset = !keyframe.usesRangeOffset();
    };

    analyzeSizeDependentTransform();
    analyzeDiscreteTransformInterval();
    analyzeExplicitlyInheritedKeyframeProperty();
    analyzeKeyframeForExplicitProperties();
    analyzeKeyframeRangeOffset();
}

void BlendingKeyframes::updatedComputedOffsets(NOESCAPE const Function<double(const BlendingKeyframe::Offset&)>& callback)
{
    for (auto& keyframe : m_keyframes)
        keyframe.setComputedOffset(callback(keyframe.specifiedOffset()));

    std::ranges::stable_sort(m_keyframes, [](auto& lhs, auto& rhs) {
        return lhs.offset() < rhs.offset();
    });
}

BlendingKeyframe::BlendingKeyframe(Offset&& offset, std::unique_ptr<RenderStyle>&& style)
    : m_specifiedOffset(WTFMove(offset))
    , m_style(WTFMove(style))
{
    if (!usesRangeOffset())
        m_computedOffset = m_specifiedOffset.value;
}

BlendingKeyframe::BlendingKeyframe(const BlendingKeyframe& source)
    : m_specifiedOffset(source.m_specifiedOffset)
    , m_computedOffset(source.m_computedOffset)
    , m_properties(source.m_properties)
    , m_style(RenderStyle::clonePtr(*source.style()))
    , m_timingFunction(source.m_timingFunction)
    , m_compositeOperation(source.m_compositeOperation)
    , m_containsDirectionAwareProperty(source.m_containsDirectionAwareProperty)
{
}

void BlendingKeyframe::addProperty(const AnimatableCSSProperty& property)
{
    ASSERT(!std::holds_alternative<CSSPropertyID>(property) || std::get<CSSPropertyID>(property) != CSSPropertyCustom);
    m_properties.add(property);
}

bool BlendingKeyframe::animatesProperty(KeyframeInterpolation::Property property) const
{
    return WTF::switchOn(property, [&](AnimatableCSSProperty& animatableCSSProperty) {
        return m_properties.contains(animatableCSSProperty);
    }, [] (auto&) {
        ASSERT_NOT_REACHED();
        return false;
    });
}

bool BlendingKeyframe::usesRangeOffset() const
{
    return m_specifiedOffset.name != SingleTimelineRange::Name::Omitted && m_specifiedOffset.name != SingleTimelineRange::Name::Normal;
}

} // namespace WebCore
