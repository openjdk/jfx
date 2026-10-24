/**
 * Copyright (C) 2000 Lars Knoll (knoll@kde.org)
 * Copyright (C) 2000 Antti Koivisto (koivisto@kde.org)
 * Copyright (C) 2000 Dirk Mueller (mueller@kde.org)
 * Copyright (C) 2003-2025 Apple Inc. All rights reserved.
 * Copyright (C) 2006 Graham Dennis (graham.dennis@gmail.com)
 * Copyright (C) 2014-2021 Google Inc. All rights reserved.
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
 */

#pragma once

#include "RenderStyle+GettersInlines.h"

#define RENDER_STYLE_PROPERTIES_SETTERS_INLINES_INCLUDE_TRAP 1
#include "RenderStyleProperties+SettersInlines.h"
#undef RENDER_STYLE_PROPERTIES_SETTERS_INLINES_INCLUDE_TRAP

namespace WebCore {

// MARK: - Initialization

inline void RenderStyle::inheritFrom(const RenderStyle& other)
{
    return m_computedStyle.inheritFrom(other.m_computedStyle);
}

inline void RenderStyle::inheritIgnoringCustomPropertiesFrom(const RenderStyle& other)
{
    return m_computedStyle.inheritIgnoringCustomPropertiesFrom(other.m_computedStyle);
}

inline void RenderStyle::inheritUnicodeBidiFrom(const RenderStyle& other)
{
    return m_computedStyle.inheritUnicodeBidiFrom(other.m_computedStyle);
}

inline void RenderStyle::inheritColumnPropertiesFrom(const RenderStyle& other)
{
    return m_computedStyle.inheritColumnPropertiesFrom(other.m_computedStyle);
}

inline void RenderStyle::fastPathInheritFrom(const RenderStyle& other)
{
    return m_computedStyle.fastPathInheritFrom(other.m_computedStyle);
}

inline void RenderStyle::copyNonInheritedFrom(const RenderStyle& other)
{
    return m_computedStyle.copyNonInheritedFrom(other.m_computedStyle);
}

inline void RenderStyle::copyContentFrom(const RenderStyle& other)
{
    return m_computedStyle.copyContentFrom(other.m_computedStyle);
}

inline void RenderStyle::copyPseudoElementBitsFrom(const RenderStyle& other)
{
    return m_computedStyle.copyPseudoElementBitsFrom(other.m_computedStyle);
}

// MARK: - Style adjustment utilities

inline void RenderStyle::setPageScaleTransform(float scale)
{
    m_computedStyle.setPageScaleTransform(scale);
}

inline void RenderStyle::setColumnStylesFromPaginationMode(PaginationMode paginationMode)
{
    m_computedStyle.setColumnStylesFromPaginationMode(paginationMode);
}

inline void RenderStyle::adjustAnimations()
{
    m_computedStyle.adjustAnimations();
}

inline void RenderStyle::adjustTransitions()
{
    m_computedStyle.adjustTransitions();
}

inline void RenderStyle::adjustBackgroundLayers()
{
    m_computedStyle.adjustBackgroundLayers();
}

inline void RenderStyle::adjustMaskLayers()
{
    m_computedStyle.adjustMaskLayers();
}

inline void RenderStyle::adjustScrollTimelines()
{
    m_computedStyle.adjustScrollTimelines();
}

inline void RenderStyle::adjustViewTimelines()
{
    m_computedStyle.adjustViewTimelines();
}

inline void RenderStyle::addToTextDecorationLineInEffect(Style::TextDecorationLine value)
{
    m_computedStyle.addToTextDecorationLineInEffect(value);
}

inline void RenderStyle::containIntrinsicWidthAddAuto()
{
    m_computedStyle.containIntrinsicWidthAddAuto();
}

inline void RenderStyle::containIntrinsicHeightAddAuto()
{
    m_computedStyle.containIntrinsicHeightAddAuto();
}

inline void RenderStyle::setGridAutoFlowDirection(Style::GridAutoFlow::Direction direction)
{
    m_computedStyle.setGridAutoFlowDirection(direction);
}

inline void RenderStyle::resetBorderBottom()
{
    m_computedStyle.resetBorderBottom();
}

inline void RenderStyle::resetBorderLeft()
{
    m_computedStyle.resetBorderLeft();
}

inline void RenderStyle::resetBorderRight()
{
    m_computedStyle.resetBorderRight();
}

inline void RenderStyle::resetBorderTop()
{
    m_computedStyle.resetBorderTop();
}

inline void RenderStyle::resetMargin()
{
    m_computedStyle.resetMargin();
}

inline void RenderStyle::resetPadding()
{
    m_computedStyle.resetPadding();
}

inline void RenderStyle::resetBorder()
{
    m_computedStyle.resetBorder();
}

inline void RenderStyle::resetBorderExceptRadius()
{
    m_computedStyle.resetBorderExceptRadius();
}

inline void RenderStyle::resetBorderRadius()
{
    m_computedStyle.resetBorderRadius();
}

} // namespace WebCore
