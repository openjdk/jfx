/*
 * Copyright (C) 2025-2026 Samuel Weinig <sam@webkit.org>
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

#include <WebCore/RenderStyleBase.h>
#include <WebCore/StyleComputedStyle+GettersInlines.h>

namespace WebCore {

// MARK: - Non-property getters

inline bool RenderStyleBase::usesViewportUnits() const
{
    return m_computedStyle.usesViewportUnits();
}

inline bool RenderStyleBase::usesContainerUnits() const
{
    return m_computedStyle.usesContainerUnits();
}

inline bool RenderStyleBase::useTreeCountingFunctions() const
{
    return m_computedStyle.useTreeCountingFunctions();
}

inline InsideLink RenderStyleBase::insideLink() const
{
    return m_computedStyle.insideLink();
}

inline bool RenderStyleBase::isLink() const
{
    return m_computedStyle.isLink();
}

inline bool RenderStyleBase::emptyState() const
{
    return m_computedStyle.emptyState();
}

inline bool RenderStyleBase::firstChildState() const
{
    return m_computedStyle.firstChildState();
}

inline bool RenderStyleBase::lastChildState() const
{
    return m_computedStyle.lastChildState();
}

inline bool RenderStyleBase::hasExplicitlyInheritedProperties() const
{
    return m_computedStyle.hasExplicitlyInheritedProperties();
}

inline bool RenderStyleBase::disallowsFastPathInheritance() const
{
    return m_computedStyle.disallowsFastPathInheritance();
}

inline bool RenderStyleBase::effectiveInert() const
{
    return m_computedStyle.effectiveInert();
}

inline bool RenderStyleBase::isEffectivelyTransparent() const
{
    return m_computedStyle.isEffectivelyTransparent();
}

inline bool RenderStyleBase::insideDefaultButton() const
{
    return m_computedStyle.insideDefaultButton();
}

inline bool RenderStyleBase::insideSubmitButton() const
{
    return m_computedStyle.insideSubmitButton();
}

inline bool RenderStyleBase::isInSubtreeWithBlendMode() const
{
    return m_computedStyle.isInSubtreeWithBlendMode();
}

inline bool RenderStyleBase::isForceHidden() const
{
    return m_computedStyle.isForceHidden();
}

inline bool RenderStyleBase::hasDisplayAffectedByAnimations() const
{
    return m_computedStyle.hasDisplayAffectedByAnimations();
}

inline bool RenderStyleBase::transformStyleForcedToFlat() const
{
    return m_computedStyle.transformStyleForcedToFlat();
}

inline bool RenderStyleBase::usesAnchorFunctions() const
{
    return m_computedStyle.usesAnchorFunctions();
}

inline EnumSet<BoxAxis> RenderStyleBase::anchorFunctionScrollCompensatedAxes() const
{
    return m_computedStyle.anchorFunctionScrollCompensatedAxes();
}

inline bool RenderStyleBase::isPopoverInvoker() const
{
    return m_computedStyle.isPopoverInvoker();
}

inline bool RenderStyleBase::autoRevealsWhenFound() const
{
    return m_computedStyle.autoRevealsWhenFound();
}

inline bool RenderStyleBase::nativeAppearanceDisabled() const
{
    return m_computedStyle.nativeAppearanceDisabled();
}

inline OptionSet<EventListenerRegionType> RenderStyleBase::eventListenerRegionTypes() const
{
    return m_computedStyle.eventListenerRegionTypes();
}

inline bool RenderStyleBase::hasAttrContent() const
{
    return m_computedStyle.hasAttrContent();
}

inline std::optional<size_t> RenderStyleBase::usedPositionOptionIndex() const
{
    return m_computedStyle.usedPositionOptionIndex();
}

inline constexpr DisplayType RenderStyleBase::originalDisplay() const
{
    return m_computedStyle.originalDisplay();
}

inline DisplayType RenderStyleBase::effectiveDisplay() const
{
    return m_computedStyle.effectiveDisplay();
}

inline StyleAppearance RenderStyleBase::usedAppearance() const
{
    return m_computedStyle.usedAppearance();
}

inline ContentVisibility RenderStyleBase::usedContentVisibility() const
{
    return m_computedStyle.usedContentVisibility();
}

inline Style::TouchAction RenderStyleBase::usedTouchAction() const
{
    return m_computedStyle.usedTouchAction();
}

inline Style::ZIndex RenderStyleBase::usedZIndex() const
{
    return m_computedStyle.usedZIndex();
}

#if HAVE(CORE_MATERIAL)

inline AppleVisualEffect RenderStyleBase::usedAppleVisualEffectForSubtree() const
{
    return m_computedStyle.usedAppleVisualEffectForSubtree();
}

#endif

#if ENABLE(TEXT_AUTOSIZING)

inline AutosizeStatus RenderStyleBase::autosizeStatus() const
{
    return m_computedStyle.autosizeStatus();
}

#endif // ENABLE(TEXT_AUTOSIZING)

// MARK: - Pseudo element/style

inline bool RenderStyleBase::hasAnyPublicPseudoStyles() const
{
    return m_computedStyle.hasAnyPublicPseudoStyles();
}

inline bool RenderStyleBase::hasPseudoStyle(PseudoElementType pseudo) const
{
    return m_computedStyle.hasPseudoStyle(pseudo);
}

inline std::optional<PseudoElementType> RenderStyleBase::pseudoElementType() const
{
    return m_computedStyle.pseudoElementType();
}

inline const AtomString& RenderStyleBase::pseudoElementNameArgument() const
{
    return m_computedStyle.pseudoElementNameArgument();
}

inline std::optional<Style::PseudoElementIdentifier> RenderStyleBase::pseudoElementIdentifier() const
{
    return m_computedStyle.pseudoElementIdentifier();
}

inline RenderStyle* RenderStyleBase::getCachedPseudoStyle(const Style::PseudoElementIdentifier& pseudoElementIdentifier) const
{
    return m_computedStyle.getCachedPseudoStyle(pseudoElementIdentifier);
}

// MARK: - Custom properties

inline const Style::CustomPropertyData& RenderStyleBase::inheritedCustomProperties() const
{
    return m_computedStyle.inheritedCustomProperties();
}

inline const Style::CustomPropertyData& RenderStyleBase::nonInheritedCustomProperties() const
{
    return m_computedStyle.nonInheritedCustomProperties();
}

inline const Style::CustomProperty* RenderStyleBase::customPropertyValue(const AtomString& property) const
{
    return m_computedStyle.customPropertyValue(property);
}

inline bool RenderStyleBase::customPropertyValueEqual(const RenderStyleBase& other, const AtomString& property) const
{
    return m_computedStyle.customPropertyValueEqual(other.m_computedStyle, property);
}

inline bool RenderStyleBase::customPropertiesEqual(const RenderStyleBase& other) const
{
    return m_computedStyle.customPropertiesEqual(other.m_computedStyle);
}

inline void RenderStyleBase::deduplicateCustomProperties(const RenderStyleBase& other)
{
    m_computedStyle.deduplicateCustomProperties(other.m_computedStyle);
}

// MARK: - Custom paint

inline void RenderStyleBase::addCustomPaintWatchProperty(const AtomString& property)
{
    m_computedStyle.addCustomPaintWatchProperty(property);
}

// MARK: - Zoom

inline bool RenderStyleBase::evaluationTimeZoomEnabled() const
{
    return m_computedStyle.evaluationTimeZoomEnabled();
}

inline float RenderStyleBase::deviceScaleFactor() const
{
    return m_computedStyle.deviceScaleFactor();
}

inline bool RenderStyleBase::useSVGZoomRulesForLength() const
{
    return m_computedStyle.useSVGZoomRulesForLength();
}

inline float RenderStyleBase::usedZoom() const
{
    return m_computedStyle.usedZoom();
}

inline Style::ZoomFactor RenderStyleBase::usedZoomForLength() const
{
    return m_computedStyle.usedZoomForLength();
}

// MARK: - Fonts

inline const FontCascade& RenderStyleBase::fontCascade() const
{
    return m_computedStyle.fontCascade();
}

inline CheckedRef<const FontCascade> RenderStyleBase::checkedFontCascade() const
{
    return m_computedStyle.checkedFontCascade();
}

inline FontCascade& RenderStyleBase::mutableFontCascadeWithoutUpdate()
{
    return m_computedStyle.mutableFontCascadeWithoutUpdate();
}

inline void RenderStyleBase::setFontCascade(FontCascade&& fontCascade)
{
    m_computedStyle.setFontCascade(WTF::move(fontCascade));
}

inline const FontCascadeDescription& RenderStyleBase::fontDescription() const
{
    return m_computedStyle.fontDescription();
}

inline FontCascadeDescription& RenderStyleBase::mutableFontDescriptionWithoutUpdate()
{
    return m_computedStyle.mutableFontDescriptionWithoutUpdate();
}

inline void RenderStyleBase::setFontDescription(FontCascadeDescription&& description)
{
    m_computedStyle.setFontDescription(WTF::move(description));
}

inline bool RenderStyleBase::setFontDescriptionWithoutUpdate(FontCascadeDescription&& description)
{
    return m_computedStyle.setFontDescriptionWithoutUpdate(WTF::move(description));
}

inline const FontMetrics& RenderStyleBase::metricsOfPrimaryFont() const
{
    return m_computedStyle.metricsOfPrimaryFont();
}

inline std::pair<FontOrientation, NonCJKGlyphOrientation> RenderStyleBase::fontAndGlyphOrientation()
{
    return m_computedStyle.fontAndGlyphOrientation();
}

inline Style::WebkitLocale RenderStyleBase::computedLocale() const
{
    return m_computedStyle.computedLocale();
}

inline float RenderStyleBase::computedFontSize() const
{
    return m_computedStyle.computedFontSize();
}

inline const Style::LineHeight& RenderStyleBase::specifiedLineHeight() const
{
    return m_computedStyle.specifiedLineHeight();
}

inline void RenderStyleBase::synchronizeLetterSpacingWithFontCascade()
{
    m_computedStyle.synchronizeLetterSpacingWithFontCascade();
}

inline void RenderStyleBase::synchronizeLetterSpacingWithFontCascadeWithoutUpdate()
{
    m_computedStyle.synchronizeLetterSpacingWithFontCascadeWithoutUpdate();
}

inline void RenderStyleBase::synchronizeWordSpacingWithFontCascade()
{
    m_computedStyle.synchronizeWordSpacingWithFontCascade();
}

inline void RenderStyleBase::synchronizeWordSpacingWithFontCascadeWithoutUpdate()
{
    m_computedStyle.synchronizeWordSpacingWithFontCascadeWithoutUpdate();
}

inline float RenderStyleBase::usedLetterSpacing() const
{
    return m_computedStyle.usedLetterSpacing();
}

inline float RenderStyleBase::usedWordSpacing() const
{
    return m_computedStyle.usedWordSpacing();
}

// MARK: Used Counter Directives

inline const CounterDirectiveMap& RenderStyleBase::usedCounterDirectives() const
{
    return m_computedStyle.usedCounterDirectives();
}

// MARK: - Aggregates

inline const Style::InsetBox& RenderStyleBase::insetBox() const
{
    return m_computedStyle.insetBox();
}

inline const Style::MarginBox& RenderStyleBase::marginBox() const
{
    return m_computedStyle.marginBox();
}

inline const Style::PaddingBox& RenderStyleBase::paddingBox() const
{
    return m_computedStyle.paddingBox();
}

inline const Style::ScrollMarginBox& RenderStyleBase::scrollMarginBox() const
{
    return m_computedStyle.scrollMarginBox();
}

inline const Style::ScrollPaddingBox& RenderStyleBase::scrollPaddingBox() const
{
    return m_computedStyle.scrollPaddingBox();
}

inline const Style::ScrollTimelines& RenderStyleBase::scrollTimelines() const
{
    return m_computedStyle.scrollTimelines();
}

inline const Style::ViewTimelines& RenderStyleBase::viewTimelines() const
{
    return m_computedStyle.viewTimelines();
}

inline const Style::Animations& RenderStyleBase::animations() const
{
    return m_computedStyle.animations();
}

inline const Style::Transitions& RenderStyleBase::transitions() const
{
    return m_computedStyle.transitions();
}

inline const Style::BackgroundLayers& RenderStyleBase::backgroundLayers() const
{
    return m_computedStyle.backgroundLayers();
}

inline const Style::MaskLayers& RenderStyleBase::maskLayers() const
{
    return m_computedStyle.maskLayers();
}

inline const Style::MaskBorder& RenderStyleBase::maskBorder() const
{
    return m_computedStyle.maskBorder();
}

inline const Style::BorderImage& RenderStyleBase::borderImage() const
{
    return m_computedStyle.borderImage();
}

inline const Style::TransformOrigin& RenderStyleBase::transformOrigin() const
{
    return m_computedStyle.transformOrigin();
}

inline const Style::PerspectiveOrigin& RenderStyleBase::perspectiveOrigin() const
{
    return m_computedStyle.perspectiveOrigin();
}

inline const BorderData& RenderStyleBase::border() const
{
    return m_computedStyle.border();
}

inline const Style::BorderRadius& RenderStyleBase::borderRadii() const
{
    return m_computedStyle.borderRadii();
}

inline const BorderValue& RenderStyleBase::borderBottom() const
{
    return m_computedStyle.borderBottom();
}

inline const BorderValue& RenderStyleBase::borderLeft() const
{
    return m_computedStyle.borderLeft();
}

inline const BorderValue& RenderStyleBase::borderRight() const
{
    return m_computedStyle.borderRight();
}

inline const BorderValue& RenderStyleBase::borderTop() const
{
    return m_computedStyle.borderTop();
}

// MARK: - Properties/descriptors that are not yet generated

inline CursorType RenderStyleBase::cursorType() const
{
    return m_computedStyle.cursorType();
}

// FIXME: Support descriptors

inline const Style::PageSize& RenderStyleBase::pageSize() const
{
    return m_computedStyle.pageSize();
}

} // namespace WebCore
