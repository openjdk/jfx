/**
 * Copyright (C) 2003-2023 Apple Inc. All rights reserved.
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

#include "RenderBlock.h"
#include "RenderObjectInlines.h"
#include "RenderStyleInlines.h"

namespace WebCore {

inline LayoutUnit RenderBlock::endOffsetForContent() const { return !writingMode().isLogicalLeftInlineStart() ? logicalLeftOffsetForContent() : logicalWidth() - logicalRightOffsetForContent(); }
inline LayoutUnit RenderBlock::logicalMarginBoxHeightForChild(const RenderBox& child) const { return isHorizontalWritingMode() ? child.marginBoxRect().height() : child.marginBoxRect().width(); }
inline LayoutUnit RenderBlock::startOffsetForContent() const { return writingMode().isLogicalLeftInlineStart() ? logicalLeftOffsetForContent() : logicalWidth() - logicalRightOffsetForContent(); }
inline LayoutUnit RenderBlock::logicalRightOffsetForLine(LayoutUnit position, LayoutUnit logicalHeight) const { return adjustLogicalRightOffsetForLine(logicalRightFloatOffsetForLine(position, logicalRightOffsetForContent(), logicalHeight)); }
inline LayoutUnit RenderBlock::logicalLeftOffsetForLine(LayoutUnit position, LayoutUnit logicalHeight) const { return adjustLogicalLeftOffsetForLine(logicalLeftFloatOffsetForLine(position, logicalLeftOffsetForContent(), logicalHeight)); }

inline LayoutUnit RenderBlock::endOffsetForLine(LayoutUnit position, LayoutUnit logicalHeight) const
{
    return !writingMode().isLogicalLeftInlineStart() ? logicalLeftOffsetForLine(position, logicalHeight) : logicalWidth() - logicalRightOffsetForLine(position, logicalHeight);
}

inline bool RenderBlock::shouldSkipCreatingRunsForObject(RenderObject& object)
{
    return object.isFloating() || (object.isOutOfFlowPositioned() && !object.style().isOriginalDisplayInlineType() && !object.container()->isRenderInline());
}

inline LayoutUnit RenderBlock::startOffsetForLine(LayoutUnit position, LayoutUnit logicalHeight) const
{
    return writingMode().isLogicalLeftInlineStart() ? logicalLeftOffsetForLine(position, logicalHeight)
        : logicalWidth() - logicalRightOffsetForLine(position, logicalHeight);
}

inline RenderPtr<RenderBlock> RenderBlock::createAnonymousWithParentRendererAndDisplay(const RenderBox& parent, DisplayType display)
{
    return createAnonymousBlockWithStyleAndDisplay(parent.protectedDocument(), parent.style(), display);
}

inline RenderPtr<RenderBox> RenderBlock::createAnonymousBoxWithSameTypeAs(const RenderBox& renderer) const
{
    return createAnonymousBlockWithStyleAndDisplay(protectedDocument(), renderer.style(), style().display());
}

inline RenderPtr<RenderBlock> RenderBlock::createAnonymousBlock(DisplayType display) const
{
    return createAnonymousBlockWithStyleAndDisplay(protectedDocument(), style(), display);
}

// Versions that can compute line offsets with the fragment and page offset passed in. Used for speed to avoid having to
// compute the fragment all over again when you already know it.
inline LayoutUnit RenderBlock::availableLogicalWidthForLine(LayoutUnit position, LayoutUnit logicalHeight) const
{
    auto logicalRightOffsetForLine = adjustLogicalRightOffsetForLine(logicalRightFloatOffsetForLine(position, logicalRightOffsetForContent(), logicalHeight));
    auto logicalLeftOffsetForLine = adjustLogicalLeftOffsetForLine(logicalLeftFloatOffsetForLine(position, logicalLeftOffsetForContent(), logicalHeight));
    return std::max(0_lu, logicalRightOffsetForLine - logicalLeftOffsetForLine);
}

} // namespace WebCore
