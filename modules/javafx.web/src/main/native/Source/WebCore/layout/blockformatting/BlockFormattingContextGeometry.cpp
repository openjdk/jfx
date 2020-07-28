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

#include "config.h"
#include "BlockFormattingContext.h"

#if ENABLE(LAYOUT_FORMATTING_CONTEXT)

#include "FormattingContext.h"
#include "InlineFormattingState.h"
#include "LayoutChildIterator.h"
#include "Logging.h"
#include <wtf/text/TextStream.h>

namespace WebCore {
namespace Layout {

HeightAndMargin BlockFormattingContext::Geometry::inFlowNonReplacedHeightAndMargin(const LayoutState& layoutState, const Box& layoutBox, UsedVerticalValues usedValues)
{
    ASSERT(layoutBox.isInFlow() && !layoutBox.replaced());
    ASSERT(layoutBox.isOverflowVisible());

    auto compute = [&]() -> HeightAndMargin {

        // 10.6.3 Block-level non-replaced elements in normal flow when 'overflow' computes to 'visible'
        //
        // If 'margin-top', or 'margin-bottom' are 'auto', their used value is 0.
        // If 'height' is 'auto', the height depends on whether the element has any block-level children and whether it has padding or borders:
        // The element's height is the distance from its top content edge to the first applicable of the following:
        // 1. the bottom edge of the last line box, if the box establishes a inline formatting context with one or more lines
        // 2. the bottom edge of the bottom (possibly collapsed) margin of its last in-flow child, if the child's bottom margin
        //    does not collapse with the element's bottom margin
        // 3. the bottom border edge of the last in-flow child whose top margin doesn't collapse with the element's bottom margin
        // 4. zero, otherwise
        // Only children in the normal flow are taken into account (i.e., floating boxes and absolutely positioned boxes are ignored,
        // and relatively positioned boxes are considered without their offset). Note that the child box may be an anonymous block box.

        auto& displayBox = layoutState.displayBoxForLayoutBox(layoutBox);
        auto containingBlockWidth = layoutState.displayBoxForLayoutBox(*layoutBox.containingBlock()).contentBoxWidth();
        auto computedVerticalMargin = Geometry::computedVerticalMargin(layoutBox, UsedHorizontalValues { containingBlockWidth });
        auto nonCollapsedMargin = UsedVerticalMargin::NonCollapsedValues { computedVerticalMargin.before.valueOr(0), computedVerticalMargin.after.valueOr(0) };
        auto borderAndPaddingTop = displayBox.borderTop() + displayBox.paddingTop().valueOr(0);
        auto height = usedValues.height ? usedValues.height.value() : computedHeightValue(layoutState, layoutBox, HeightType::Normal);

        if (height) {
            auto borderAndPaddingBottom = displayBox.borderBottom() + displayBox.paddingBottom().valueOr(0);
            auto contentHeight = layoutBox.style().boxSizing() == BoxSizing::ContentBox ? *height : *height - (borderAndPaddingTop + borderAndPaddingBottom);
            return { contentHeight, nonCollapsedMargin };
        }

        if (!is<Container>(layoutBox) || !downcast<Container>(layoutBox).hasInFlowChild())
            return { 0, nonCollapsedMargin };

        // 1. the bottom edge of the last line box, if the box establishes a inline formatting context with one or more lines
        if (layoutBox.establishesInlineFormattingContext()) {
            auto& lineBoxes = downcast<InlineFormattingState>(layoutState.establishedFormattingState(layoutBox)).lineBoxes();
            // Even empty containers generate one line.
            ASSERT(!lineBoxes.isEmpty());
            return { lineBoxes.last().logicalBottom() - borderAndPaddingTop, nonCollapsedMargin };
        }

        // 2. the bottom edge of the bottom (possibly collapsed) margin of its last in-flow child, if the child's bottom margin...
        auto* lastInFlowChild = downcast<Container>(layoutBox).lastInFlowChild();
        ASSERT(lastInFlowChild);
        if (!MarginCollapse::marginAfterCollapsesWithParentMarginAfter(layoutState, *lastInFlowChild)) {
            auto& lastInFlowDisplayBox = layoutState.displayBoxForLayoutBox(*lastInFlowChild);
            auto bottomEdgeOfBottomMargin = lastInFlowDisplayBox.bottom() + (lastInFlowDisplayBox.hasCollapsedThroughMargin() ? LayoutUnit() : lastInFlowDisplayBox.marginAfter());
            return { bottomEdgeOfBottomMargin - borderAndPaddingTop, nonCollapsedMargin };
        }

        // 3. the bottom border edge of the last in-flow child whose top margin doesn't collapse with the element's bottom margin
        auto* inFlowChild = lastInFlowChild;
        while (inFlowChild && MarginCollapse::marginBeforeCollapsesWithParentMarginAfter(layoutState, *inFlowChild))
            inFlowChild = inFlowChild->previousInFlowSibling();
        if (inFlowChild) {
            auto& inFlowDisplayBox = layoutState.displayBoxForLayoutBox(*inFlowChild);
            return { inFlowDisplayBox.top() + inFlowDisplayBox.borderBox().height() - borderAndPaddingTop, nonCollapsedMargin };
        }

        // 4. zero, otherwise
        return { 0, nonCollapsedMargin };
    };

    auto heightAndMargin = compute();
    LOG_WITH_STREAM(FormattingContextLayout, stream << "[Height][Margin] -> inflow non-replaced -> height(" << heightAndMargin.height << "px) margin(" << heightAndMargin.nonCollapsedMargin.before << "px, " << heightAndMargin.nonCollapsedMargin.after << "px) -> layoutBox(" << &layoutBox << ")");
    return heightAndMargin;
}

WidthAndMargin BlockFormattingContext::Geometry::inFlowNonReplacedWidthAndMargin(const LayoutState& layoutState, const Box& layoutBox, UsedHorizontalValues usedValues)
{
    ASSERT(layoutBox.isInFlow());

    auto compute = [&]() {

        // 10.3.3 Block-level, non-replaced elements in normal flow
        //
        // The following constraints must hold among the used values of the other properties:
        // 'margin-left' + 'border-left-width' + 'padding-left' + 'width' + 'padding-right' + 'border-right-width' + 'margin-right' = width of containing block
        //
        // 1. If 'width' is not 'auto' and 'border-left-width' + 'padding-left' + 'width' + 'padding-right' + 'border-right-width'
        //    (plus any of 'margin-left' or 'margin-right' that are not 'auto') is larger than the width of the containing block, then
        //    any 'auto' values for 'margin-left' or 'margin-right' are, for the following rules, treated as zero.
        //
        // 2. If all of the above have a computed value other than 'auto', the values are said to be "over-constrained" and one of the used values will
        //    have to be different from its computed value. If the 'direction' property of the containing block has the value 'ltr', the specified value
        //    of 'margin-right' is ignored and the value is calculated so as to make the equality true. If the value of 'direction' is 'rtl',
        //    this happens to 'margin-left' instead.
        //
        // 3. If there is exactly one value specified as 'auto', its used value follows from the equality.
        //
        // 4. If 'width' is set to 'auto', any other 'auto' values become '0' and 'width' follows from the resulting equality.
        //
        // 5. If both 'margin-left' and 'margin-right' are 'auto', their used values are equal. This horizontally centers the element with respect to the
        //    edges of the containing block.

        auto& style = layoutBox.style();
        auto* containingBlock = layoutBox.containingBlock();
        auto containingBlockWidth = usedValues.containingBlockWidth.valueOr(0);
        auto& displayBox = layoutState.displayBoxForLayoutBox(layoutBox);

        auto width = computedValueIfNotAuto(usedValues.width ? Length { usedValues.width.value(), Fixed } : style.logicalWidth(), containingBlockWidth);
        auto computedHorizontalMargin = Geometry::computedHorizontalMargin(layoutBox, usedValues);
        UsedHorizontalMargin usedHorizontalMargin;
        auto borderLeft = displayBox.borderLeft();
        auto borderRight = displayBox.borderRight();
        auto paddingLeft = displayBox.paddingLeft().valueOr(0);
        auto paddingRight = displayBox.paddingRight().valueOr(0);
        auto contentWidth = [&] {
            ASSERT(width);
            return style.boxSizing() == BoxSizing::ContentBox ? *width : *width - (borderLeft + paddingLeft + paddingRight + borderRight);
        };

        // #1
        if (width) {
            auto horizontalSpaceForMargin = containingBlockWidth - (computedHorizontalMargin.start.valueOr(0) + borderLeft + paddingLeft + contentWidth() + paddingRight + borderRight + computedHorizontalMargin.end.valueOr(0));
            if (horizontalSpaceForMargin < 0)
                usedHorizontalMargin = { computedHorizontalMargin.start.valueOr(0), computedHorizontalMargin.end.valueOr(0) };
        }

        // #2
        if (width && computedHorizontalMargin.start && computedHorizontalMargin.end) {
            if (containingBlock->style().isLeftToRightDirection()) {
                usedHorizontalMargin.start = *computedHorizontalMargin.start;
                usedHorizontalMargin.end = containingBlockWidth - (usedHorizontalMargin.start + borderLeft + paddingLeft + contentWidth() + paddingRight + borderRight);
            } else {
                usedHorizontalMargin.end = *computedHorizontalMargin.end;
                usedHorizontalMargin.start = containingBlockWidth - (borderLeft + paddingLeft + contentWidth() + paddingRight + borderRight + usedHorizontalMargin.end);
            }
        }

        // #3
        if (!computedHorizontalMargin.start && width && computedHorizontalMargin.end) {
            usedHorizontalMargin.end = *computedHorizontalMargin.end;
            usedHorizontalMargin.start = containingBlockWidth - (borderLeft + paddingLeft  + contentWidth() + paddingRight + borderRight + usedHorizontalMargin.end);
        } else if (computedHorizontalMargin.start && !width && computedHorizontalMargin.end) {
            usedHorizontalMargin = { *computedHorizontalMargin.start, *computedHorizontalMargin.end };
            width = containingBlockWidth - (usedHorizontalMargin.start + borderLeft + paddingLeft + paddingRight + borderRight + usedHorizontalMargin.end);
        } else if (computedHorizontalMargin.start && width && !computedHorizontalMargin.end) {
            usedHorizontalMargin.start = *computedHorizontalMargin.start;
            usedHorizontalMargin.end = containingBlockWidth - (usedHorizontalMargin.start + borderLeft + paddingLeft + contentWidth() + paddingRight + borderRight);
        }

        // #4
        if (!width) {
            usedHorizontalMargin = { computedHorizontalMargin.start.valueOr(0), computedHorizontalMargin.end.valueOr(0) };
            width = containingBlockWidth - (usedHorizontalMargin.start + borderLeft + paddingLeft + paddingRight + borderRight + usedHorizontalMargin.end);
        }

        // #5
        if (!computedHorizontalMargin.start && !computedHorizontalMargin.end) {
            auto horizontalSpaceForMargin = containingBlockWidth - (borderLeft + paddingLeft  + contentWidth() + paddingRight + borderRight);
            usedHorizontalMargin = { horizontalSpaceForMargin / 2, horizontalSpaceForMargin / 2 };
        }

        ASSERT(width);

        return WidthAndMargin { contentWidth(), usedHorizontalMargin, computedHorizontalMargin };
    };

    auto widthAndMargin = compute();
    LOG_WITH_STREAM(FormattingContextLayout, stream << "[Width][Margin] -> inflow non-replaced -> width(" << widthAndMargin.width << "px) margin(" << widthAndMargin.usedMargin.start << "px, " << widthAndMargin.usedMargin.end << "px) -> layoutBox(" << &layoutBox << ")");
    return widthAndMargin;
}

WidthAndMargin BlockFormattingContext::Geometry::inFlowReplacedWidthAndMargin(const LayoutState& layoutState, const Box& layoutBox, UsedHorizontalValues usedValues)
{
    ASSERT(layoutBox.isInFlow() && layoutBox.replaced());

    // 10.3.4 Block-level, replaced elements in normal flow
    //
    // 1. The used value of 'width' is determined as for inline replaced elements.
    // 2. Then the rules for non-replaced block-level elements are applied to determine the margins.

    // #1
    usedValues.width = inlineReplacedWidthAndMargin(layoutState, layoutBox, usedValues).width;
    // #2
    auto nonReplacedWidthAndMargin = inFlowNonReplacedWidthAndMargin(layoutState, layoutBox, usedValues);

    LOG_WITH_STREAM(FormattingContextLayout, stream << "[Width][Margin] -> inflow replaced -> width(" << *usedValues.width << "px) margin(" << nonReplacedWidthAndMargin.usedMargin.start << "px, " << nonReplacedWidthAndMargin.usedMargin.end << "px) -> layoutBox(" << &layoutBox << ")");
    return { *usedValues.width, nonReplacedWidthAndMargin.usedMargin, nonReplacedWidthAndMargin.computedMargin };
}

LayoutUnit BlockFormattingContext::Geometry::staticVerticalPosition(const LayoutState& layoutState, const Box& layoutBox)
{
    // https://www.w3.org/TR/CSS22/visuren.html#block-formatting
    // In a block formatting context, boxes are laid out one after the other, vertically, beginning at the top of a containing block.
    // The vertical distance between two sibling boxes is determined by the 'margin' properties.
    // Vertical margins between adjacent block-level boxes in a block formatting context collapse.
    if (auto* previousInFlowSibling = layoutBox.previousInFlowSibling()) {
        auto& previousInFlowDisplayBox = layoutState.displayBoxForLayoutBox(*previousInFlowSibling);
        return previousInFlowDisplayBox.bottom() + previousInFlowDisplayBox.marginAfter();
    }
    return layoutState.displayBoxForLayoutBox(*layoutBox.containingBlock()).contentBoxTop();
}

LayoutUnit BlockFormattingContext::Geometry::staticHorizontalPosition(const LayoutState& layoutState, const Box& layoutBox)
{
    // https://www.w3.org/TR/CSS22/visuren.html#block-formatting
    // In a block formatting context, each box's left outer edge touches the left edge of the containing block (for right-to-left formatting, right edges touch).
    return layoutState.displayBoxForLayoutBox(*layoutBox.containingBlock()).contentBoxLeft() + layoutState.displayBoxForLayoutBox(layoutBox).marginStart();
}

Point BlockFormattingContext::Geometry::staticPosition(const LayoutState& layoutState, const Box& layoutBox)
{
    return { staticHorizontalPosition(layoutState, layoutBox), staticVerticalPosition(layoutState, layoutBox) };
}

HeightAndMargin BlockFormattingContext::Geometry::inFlowHeightAndMargin(const LayoutState& layoutState, const Box& layoutBox, UsedVerticalValues usedValues)
{
    ASSERT(layoutBox.isInFlow());

    // 10.6.2 Inline replaced elements, block-level replaced elements in normal flow, 'inline-block'
    // replaced elements in normal flow and floating replaced elements
    if (layoutBox.replaced())
        return inlineReplacedHeightAndMargin(layoutState, layoutBox, usedValues);

    HeightAndMargin heightAndMargin;
    // TODO: Figure out the case for the document element. Let's just complicated-case it for now.
    if (layoutBox.isOverflowVisible() && !layoutBox.isDocumentBox())
        heightAndMargin = inFlowNonReplacedHeightAndMargin(layoutState, layoutBox, usedValues);
    else {
        // 10.6.6 Complicated cases
        // Block-level, non-replaced elements in normal flow when 'overflow' does not compute to 'visible' (except if the 'overflow' property's value has been propagated to the viewport).
        auto usedHorizontalValues = UsedHorizontalValues { layoutState.displayBoxForLayoutBox(*layoutBox.containingBlock()).contentBoxWidth() };
        heightAndMargin = complicatedCases(layoutState, layoutBox, usedValues, usedHorizontalValues);
    }

    if (!Quirks::needsStretching(layoutState, layoutBox))
        return heightAndMargin;

    heightAndMargin = Quirks::stretchedInFlowHeight(layoutState, layoutBox, heightAndMargin);

    LOG_WITH_STREAM(FormattingContextLayout, stream << "[Height][Margin] -> inflow non-replaced -> streched to viewport -> height(" << heightAndMargin.height << "px) margin(" << heightAndMargin.nonCollapsedMargin.before << "px, " << heightAndMargin.nonCollapsedMargin.after << "px) -> layoutBox(" << &layoutBox << ")");
    return heightAndMargin;
}

WidthAndMargin BlockFormattingContext::Geometry::inFlowWidthAndMargin(LayoutState& layoutState, const Box& layoutBox, UsedHorizontalValues usedValues)
{
    ASSERT(layoutBox.isInFlow());

    if (!layoutBox.replaced()) {
        if (layoutBox.establishesTableFormattingContext()) {
            // This is a special table "fit-content size" behavior handling. Not in the spec though.
            // Table returns its final width as min/max. Use this final width value to computed horizontal margins etc.
            usedValues.width = Geometry::shrinkToFitWidth(layoutState, layoutBox, usedValues);
        }
        return inFlowNonReplacedWidthAndMargin(layoutState, layoutBox, usedValues);
    }
    return inFlowReplacedWidthAndMargin(layoutState, layoutBox, usedValues);
}

FormattingContext::IntrinsicWidthConstraints BlockFormattingContext::Geometry::intrinsicWidthConstraints(LayoutState& layoutState, const Box& layoutBox)
{
    auto fixedMarginBorderAndPadding = [&](auto& layoutBox) {
        auto& style = layoutBox.style();
        return fixedValue(style.marginStart()).valueOr(0)
            + LayoutUnit { style.borderLeftWidth() }
            + fixedValue(style.paddingLeft()).valueOr(0)
            + fixedValue(style.paddingRight()).valueOr(0)
            + LayoutUnit { style.borderRightWidth() }
            + fixedValue(style.marginEnd()).valueOr(0);
    };

    auto computedIntrinsicWidthConstraints = [&]() -> IntrinsicWidthConstraints {
        auto& style = layoutBox.style();
        if (auto width = fixedValue(style.logicalWidth()))
            return { *width, *width };

        // Minimum/maximum width can't be depending on the containing block's width.
        if (!style.logicalWidth().isAuto())
            return { };

        if (auto* replaced = layoutBox.replaced()) {
            if (replaced->hasIntrinsicWidth()) {
                auto replacedWidth = replaced->intrinsicWidth();
                return { replacedWidth, replacedWidth };
            }
            return { };
        }

        if (layoutBox.establishesFormattingContext())
            return layoutState.createFormattingContext(layoutBox)->computedIntrinsicWidthConstraints();

        if (!is<Container>(layoutBox) || !downcast<Container>(layoutBox).hasInFlowOrFloatingChild())
            return { };

        auto intrinsicWidthConstraints = IntrinsicWidthConstraints { };
        auto& formattingState = layoutState.formattingStateForBox(layoutBox);
        for (auto& child : childrenOfType<Box>(downcast<Container>(layoutBox))) {
            if (child.isOutOfFlowPositioned())
                continue;
            auto childIntrinsicWidthConstraints = formattingState.intrinsicWidthConstraintsForBox(child);
            ASSERT(childIntrinsicWidthConstraints);

            // FIXME Check for box-sizing: border-box;
            auto marginBorderAndPadding = fixedMarginBorderAndPadding(child);
            intrinsicWidthConstraints.minimum = std::max(intrinsicWidthConstraints.minimum, childIntrinsicWidthConstraints->minimum + marginBorderAndPadding);
            intrinsicWidthConstraints.maximum = std::max(intrinsicWidthConstraints.maximum, childIntrinsicWidthConstraints->maximum + marginBorderAndPadding);
        }
        return intrinsicWidthConstraints;
    };
    // FIXME Check for box-sizing: border-box;
    auto intrinsicWidthConstraints = constrainByMinMaxWidth(layoutBox, computedIntrinsicWidthConstraints());
    intrinsicWidthConstraints.expand(fixedMarginBorderAndPadding(layoutBox));
    return intrinsicWidthConstraints;
}

}
}

#endif
