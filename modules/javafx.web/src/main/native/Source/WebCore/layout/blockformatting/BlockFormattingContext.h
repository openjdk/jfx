/*
 * Copyright (C) 2018 Apple Inc. All rights reserved.
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

#if ENABLE(LAYOUT_FORMATTING_CONTEXT)

#include "FormattingContext.h"
#include "MarginTypes.h"
#include <wtf/HashMap.h>
#include <wtf/IsoMalloc.h>

namespace WebCore {

class LayoutUnit;

namespace Layout {

class BlockFormattingState;
class Box;
class FloatingContext;

// This class implements the layout logic for block formatting contexts.
// https://www.w3.org/TR/CSS22/visuren.html#block-formatting
class BlockFormattingContext : public FormattingContext {
    WTF_MAKE_ISO_ALLOCATED(BlockFormattingContext);
public:
    BlockFormattingContext(const Box& formattingContextRoot, BlockFormattingState&);

    void layout() const override;

private:
    void layoutFormattingContextRoot(FloatingContext&, const Box&) const;
    void placeInFlowPositionedChildren(const Box&) const;

    void computeWidthAndMargin(const Box&, Optional<LayoutUnit> usedAvailableWidth = { }) const;
    void computeHeightAndMargin(const Box&) const;

    void computeStaticHorizontalPosition(const Box&) const;
    void computeStaticVerticalPosition(const FloatingContext&, const Box&) const;
    void computeStaticPosition(const FloatingContext&, const Box&) const;
    void computeFloatingPosition(const FloatingContext&, const Box&) const;
    void computePositionToAvoidFloats(const FloatingContext&, const Box&) const;

    void computeEstimatedVerticalPosition(const Box&) const;
    void computeEstimatedVerticalPositionForAncestors(const Box&) const;
    void computeEstimatedVerticalPositionForFormattingRoot(const Box&) const;
    void computeEstimatedVerticalPositionForFloatClear(const FloatingContext&, const Box&) const;

    IntrinsicWidthConstraints computedIntrinsicWidthConstraints() const override;
    LayoutUnit verticalPositionWithMargin(const Box&, const UsedVerticalMargin&) const;

    // This class implements positioning and sizing for boxes participating in a block formatting context.
    class Geometry : public FormattingContext::Geometry {
    public:
        static HeightAndMargin inFlowHeightAndMargin(const LayoutState&, const Box&, UsedVerticalValues);
        static WidthAndMargin inFlowWidthAndMargin(LayoutState&, const Box&, UsedHorizontalValues);

        static Point staticPosition(const LayoutState&, const Box&);
        static LayoutUnit staticVerticalPosition(const LayoutState&, const Box&);
        static LayoutUnit staticHorizontalPosition(const LayoutState&, const Box&);

        static IntrinsicWidthConstraints intrinsicWidthConstraints(LayoutState&, const Box&);

    private:
        static HeightAndMargin inFlowNonReplacedHeightAndMargin(const LayoutState&, const Box&, UsedVerticalValues);
        static WidthAndMargin inFlowNonReplacedWidthAndMargin(const LayoutState&, const Box&, UsedHorizontalValues);
        static WidthAndMargin inFlowReplacedWidthAndMargin(const LayoutState&, const Box&, UsedHorizontalValues);
        static Point staticPositionForOutOfFlowPositioned(const LayoutState&, const Box&);
    };

    // This class implements margin collapsing for block formatting context.
    class MarginCollapse {
    public:
        static UsedVerticalMargin::CollapsedValues collapsedVerticalValues(const LayoutState&, const Box&, const UsedVerticalMargin::NonCollapsedValues&);

        static EstimatedMarginBefore estimatedMarginBefore(const LayoutState&, const Box&);
        static LayoutUnit marginBeforeIgnoringCollapsingThrough(const LayoutState&, const Box&, const UsedVerticalMargin::NonCollapsedValues&);
        static void updateMarginAfterForPreviousSibling(const LayoutState&, const Box&);
        static void updatePositiveNegativeMarginValues(const LayoutState&, const Box&);

        static bool marginBeforeCollapsesWithParentMarginBefore(const LayoutState&, const Box&);
        static bool marginBeforeCollapsesWithFirstInFlowChildMarginBefore(const LayoutState&, const Box&);
        static bool marginBeforeCollapsesWithParentMarginAfter(const LayoutState&, const Box&);
        static bool marginBeforeCollapsesWithPreviousSiblingMarginAfter(const LayoutState&, const Box&);

        static bool marginAfterCollapsesWithParentMarginAfter(const LayoutState&, const Box&);
        static bool marginAfterCollapsesWithLastInFlowChildMarginAfter(const LayoutState&, const Box&);
        static bool marginAfterCollapsesWithParentMarginBefore(const LayoutState&, const Box&);
        static bool marginAfterCollapsesWithNextSiblingMarginBefore(const LayoutState&, const Box&);
        static bool marginAfterCollapsesWithSiblingMarginBeforeWithClearance(const LayoutState&, const Box&);

        static bool marginsCollapseThrough(const LayoutState&, const Box&);

    private:
        enum class MarginType { Before, After };
        static PositiveAndNegativeVerticalMargin::Values positiveNegativeValues(const LayoutState&, const Box&, MarginType);
        static PositiveAndNegativeVerticalMargin::Values positiveNegativeMarginBefore(const LayoutState&, const Box&, const UsedVerticalMargin::NonCollapsedValues&);
        static PositiveAndNegativeVerticalMargin::Values positiveNegativeMarginAfter(const LayoutState&, const Box&, const UsedVerticalMargin::NonCollapsedValues&);
    };

    class Quirks {
    public:
        static bool needsStretching(const LayoutState&, const Box&);
        static HeightAndMargin stretchedInFlowHeight(const LayoutState&, const Box&, HeightAndMargin);

        static bool shouldIgnoreCollapsedQuirkMargin(const LayoutState&, const Box&);
        static bool shouldIgnoreMarginBefore(const LayoutState&, const Box&);
        static bool shouldIgnoreMarginAfter(const LayoutState&, const Box&);
    };

    void setEstimatedMarginBefore(const Box&, const EstimatedMarginBefore&) const;
    void removeEstimatedMarginBefore(const Box& layoutBox) const { m_estimatedMarginBeforeList.remove(&layoutBox); }
    bool hasEstimatedMarginBefore(const Box&) const;
    Optional<LayoutUnit> usedAvailableWidthForFloatAvoider(const FloatingContext&, const Box&) const;
#ifndef NDEBUG
    EstimatedMarginBefore estimatedMarginBefore(const Box& layoutBox) const { return m_estimatedMarginBeforeList.get(&layoutBox); }
    bool hasPrecomputedMarginBefore(const Box&) const;
#endif

    BlockFormattingState& formattingState() const { return downcast<BlockFormattingState>(FormattingContext::formattingState()); }

private:
    mutable HashMap<const Box*, EstimatedMarginBefore> m_estimatedMarginBeforeList;
};

}
}
#endif
