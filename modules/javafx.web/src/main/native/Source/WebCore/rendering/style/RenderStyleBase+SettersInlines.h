/*
 * Copyright (C) 2000 Lars Knoll (knoll@kde.org)
 *           (C) 2000 Antti Koivisto (koivisto@kde.org)
 *           (C) 2000 Dirk Mueller (mueller@kde.org)
 * Copyright (C) 2003-2023 Apple Inc. All rights reserved.
 * Copyright (C) 2014-2021 Google Inc. All rights reserved.
 * Copyright (C) 2006 Graham Dennis (graham.dennis@gmail.com)
 * Copyright (C) 2025 Samuel Weinig <sam@webkit.org>
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

#pragma once

#include "RenderStyleBase+GettersInlines.h"
#include "StyleComputedStyle+SettersInlines.h"

namespace WebCore {

// MARK: - Non-property setters

inline void RenderStyleBase::setUsesViewportUnits()
{
    m_computedStyle.setUsesViewportUnits();
}

inline void RenderStyleBase::setUsesContainerUnits()
{
    m_computedStyle.setUsesContainerUnits();
}

inline void RenderStyleBase::setUsesTreeCountingFunctions()
{
    m_computedStyle.setUsesTreeCountingFunctions();
}

inline void RenderStyleBase::setInsideLink(InsideLink insideLink)
{
    m_computedStyle.setInsideLink(insideLink);
}

inline void RenderStyleBase::setIsLink(bool isLink)
{
    m_computedStyle.setIsLink(isLink);
}

inline void RenderStyleBase::setEmptyState(bool emptyState)
{
    m_computedStyle.setEmptyState(emptyState);
}

inline void RenderStyleBase::setFirstChildState()
{
    m_computedStyle.setFirstChildState();
}

inline void RenderStyleBase::setLastChildState()
{
    m_computedStyle.setLastChildState();
}

inline void RenderStyleBase::setHasExplicitlyInheritedProperties()
{
    m_computedStyle.setHasExplicitlyInheritedProperties();
}

inline void RenderStyleBase::setDisallowsFastPathInheritance()
{
    m_computedStyle.setDisallowsFastPathInheritance();
}

inline void RenderStyleBase::setEffectiveInert(bool effectiveInert)
{
    m_computedStyle.setEffectiveInert(effectiveInert);
}

inline void RenderStyleBase::setIsEffectivelyTransparent(bool effectivelyTransparent)
{
    m_computedStyle.setIsEffectivelyTransparent(effectivelyTransparent);
}

inline void RenderStyleBase::setEventListenerRegionTypes(OptionSet<EventListenerRegionType> eventListenerTypes)
{
    m_computedStyle.setEventListenerRegionTypes(eventListenerTypes);
}

inline void RenderStyleBase::setHasAttrContent()
{
    m_computedStyle.setHasAttrContent();
}

inline void RenderStyleBase::setHasDisplayAffectedByAnimations()
{
    m_computedStyle.setHasDisplayAffectedByAnimations();
}

inline void RenderStyleBase::setTransformStyleForcedToFlat(bool value)
{
    m_computedStyle.setTransformStyleForcedToFlat(value);
}

inline void RenderStyleBase::setUsesAnchorFunctions()
{
    m_computedStyle.setUsesAnchorFunctions();
}

inline void RenderStyleBase::setAnchorFunctionScrollCompensatedAxes(EnumSet<BoxAxis> axes)
{
    m_computedStyle.setAnchorFunctionScrollCompensatedAxes(axes);
}

inline void RenderStyleBase::setIsPopoverInvoker()
{
    m_computedStyle.setIsPopoverInvoker();
}

inline void RenderStyleBase::setNativeAppearanceDisabled(bool value)
{
    m_computedStyle.setNativeAppearanceDisabled(value);
}

inline void RenderStyleBase::setIsForceHidden()
{
    m_computedStyle.setIsForceHidden();
}

inline void RenderStyleBase::setAutoRevealsWhenFound()
{
    m_computedStyle.setAutoRevealsWhenFound();
}

inline void RenderStyleBase::setInsideDefaultButton(bool value)
{
    m_computedStyle.setInsideDefaultButton(value);
}

inline void RenderStyleBase::setInsideSubmitButton(bool value)
{
    m_computedStyle.setInsideSubmitButton(value);
}

inline void RenderStyleBase::setUsedPositionOptionIndex(std::optional<size_t> index)
{
    m_computedStyle.setUsedPositionOptionIndex(index);
}

inline void RenderStyleBase::setEffectiveDisplay(DisplayType effectiveDisplay)
{
    m_computedStyle.setEffectiveDisplay(effectiveDisplay);
}

// MARK: - Cache used values

inline void RenderStyleBase::setUsedAppearance(StyleAppearance appearance)
{
    m_computedStyle.setUsedAppearance(appearance);
}

inline void RenderStyleBase::setUsedTouchAction(Style::TouchAction touchAction)
{
    m_computedStyle.setUsedTouchAction(touchAction);
}

inline void RenderStyleBase::setUsedContentVisibility(ContentVisibility usedContentVisibility)
{
    m_computedStyle.setUsedContentVisibility(usedContentVisibility);
}

inline void RenderStyleBase::setUsedZIndex(Style::ZIndex index)
{
    m_computedStyle.setUsedZIndex(index);
}

#if HAVE(CORE_MATERIAL)

inline void RenderStyleBase::setUsedAppleVisualEffectForSubtree(AppleVisualEffect effect)
{
    m_computedStyle.setUsedAppleVisualEffectForSubtree(effect);
}

#endif

#if ENABLE(TEXT_AUTOSIZING)

inline void RenderStyleBase::setAutosizeStatus(AutosizeStatus autosizeStatus)
{
    m_computedStyle.setAutosizeStatus(autosizeStatus);
}

#endif // ENABLE(TEXT_AUTOSIZING)

// MARK: - Pseudo element/style

inline void RenderStyleBase::setHasPseudoStyles(EnumSet<PseudoElementType> set)
{
    m_computedStyle.setHasPseudoStyles(set);
}

inline void RenderStyleBase::setPseudoElementIdentifier(std::optional<Style::PseudoElementIdentifier>&& identifier)
{
    m_computedStyle.setPseudoElementIdentifier(WTF::move(identifier));
}

inline RenderStyle* RenderStyleBase::addCachedPseudoStyle(std::unique_ptr<RenderStyle> pseudo)
{
    return m_computedStyle.addCachedPseudoStyle(WTF::move(pseudo));
}

// MARK: - Custom properties

inline void RenderStyleBase::setCustomPropertyValue(Ref<const Style::CustomProperty>&& value, bool isInherited)
{
    m_computedStyle.setCustomPropertyValue(WTF::move(value), isInherited);
}

// MARK: - Fonts

#if ENABLE(TEXT_AUTOSIZING)

inline void RenderStyleBase::setSpecifiedLineHeight(Style::LineHeight&& lineHeight)
{
    m_computedStyle.setSpecifiedLineHeight(WTF::move(lineHeight));
}

#endif

inline void RenderStyleBase::setLetterSpacingFromAnimation(Style::LetterSpacing&& value)
{
    m_computedStyle.setLetterSpacingFromAnimation(WTF::move(value));
}

inline void RenderStyleBase::setWordSpacingFromAnimation(Style::WordSpacing&& value)
{
    m_computedStyle.setWordSpacingFromAnimation(WTF::move(value));
}

// MARK: - Zoom

inline void RenderStyleBase::setEvaluationTimeZoomEnabled(bool value)
{
    m_computedStyle.setEvaluationTimeZoomEnabled(value);
}

inline void RenderStyleBase::setDeviceScaleFactor(float value)
{
    m_computedStyle.setDeviceScaleFactor(value);
}

inline void RenderStyleBase::setUseSVGZoomRulesForLength(bool value)
{
    m_computedStyle.setUseSVGZoomRulesForLength(value);
}

inline bool RenderStyleBase::setUsedZoom(float zoomLevel)
{
    return m_computedStyle.setUsedZoom(zoomLevel);
}

// MARK: - Aggregates

inline Style::Animations& RenderStyleBase::ensureAnimations()
{
    return m_computedStyle.ensureAnimations();
}

inline Style::Transitions& RenderStyleBase::ensureTransitions()
{
    return m_computedStyle.ensureTransitions();
}

inline Style::BackgroundLayers& RenderStyleBase::ensureBackgroundLayers()
{
    return m_computedStyle.ensureBackgroundLayers();
}

inline Style::MaskLayers& RenderStyleBase::ensureMaskLayers()
{
    return m_computedStyle.ensureMaskLayers();
}

inline void RenderStyleBase::setBackgroundLayers(Style::BackgroundLayers&& layers)
{
    m_computedStyle.setBackgroundLayers(WTF::move(layers));
}

inline void RenderStyleBase::setMaskLayers(Style::MaskLayers&& layers)
{
    m_computedStyle.setMaskLayers(WTF::move(layers));
}

inline void RenderStyleBase::setMaskBorder(Style::MaskBorder&& image)
{
    m_computedStyle.setMaskBorder(WTF::move(image));
}

inline void RenderStyleBase::setBorderImage(Style::BorderImage&& image)
{
    m_computedStyle.setBorderImage(WTF::move(image));
}

inline void RenderStyleBase::setPerspectiveOrigin(Style::PerspectiveOrigin&& origin)
{
    m_computedStyle.setPerspectiveOrigin(WTF::move(origin));
}

inline void RenderStyleBase::setTransformOrigin(Style::TransformOrigin&& origin)
{
    m_computedStyle.setTransformOrigin(WTF::move(origin));
}

inline void RenderStyleBase::setInsetBox(Style::InsetBox&& box)
{
    m_computedStyle.setInsetBox(WTF::move(box));
}

inline void RenderStyleBase::setMarginBox(Style::MarginBox&& box)
{
    m_computedStyle.setMarginBox(WTF::move(box));
}

inline void RenderStyleBase::setPaddingBox(Style::PaddingBox&& box)
{
    m_computedStyle.setPaddingBox(WTF::move(box));
}

inline void RenderStyleBase::setBorderRadius(Style::BorderRadiusValue&& size)
{
    m_computedStyle.setBorderRadius(WTF::move(size));
}

inline void RenderStyleBase::setBorderTop(BorderValue&& value)
{
    m_computedStyle.setBorderTop(WTF::move(value));
}

inline void RenderStyleBase::setBorderRight(BorderValue&& value)
{
    m_computedStyle.setBorderRight(WTF::move(value));
}

inline void RenderStyleBase::setBorderBottom(BorderValue&& value)
{
    m_computedStyle.setBorderBottom(WTF::move(value));
}

inline void RenderStyleBase::setBorderLeft(BorderValue&& value)
{
    m_computedStyle.setBorderLeft(WTF::move(value));
}

// MARK: - Properties/descriptors that are not yet generated

// FIXME: Support descriptors

inline void RenderStyleBase::setPageSize(Style::PageSize&& pageSize)
{
    m_computedStyle.setPageSize(WTF::move(pageSize));
}

} // namespace WebCore
