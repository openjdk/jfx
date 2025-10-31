/*
 * Copyright (C) 2023 Apple Inc. All rights reserved.
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
#include "RubyFormattingContext.h"

#include "InlineContentAligner.h"
#include "InlineFormattingContext.h"
#include "InlineLine.h"
#include "RenderStyleInlines.h"

namespace WebCore {
namespace Layout {

static inline InlineLayoutUnit halfOfAFullWidthCharacter(const Box& annotationBox)
{
    return annotationBox.style().computedFontSize() / 2.f;
}

static inline size_t baseContentIndex(size_t rubyBaseStart, const InlineDisplay::Boxes& boxes)
{
    auto baseContentIndex = rubyBaseStart + 1;
    if (boxes[baseContentIndex].layoutBox().isRubyAnnotationBox())
        ++baseContentIndex;
    return baseContentIndex;
}

static RubyPosition rubyPosition(const Box& rubyBaseLayoutBox)
{
    ASSERT(rubyBaseLayoutBox.isRubyBase());
    auto computedRubyPosition = rubyBaseLayoutBox.style().rubyPosition();
    if (!rubyBaseLayoutBox.writingMode().isVerticalTypographic())
        return computedRubyPosition;
    // inter-character: If the writing mode of the enclosing ruby container is vertical, this value has the same effect as over.
    return rubyBaseLayoutBox.style().isInterCharacterRubyPosition() ? RubyPosition::Over : computedRubyPosition;
}

static inline InlineRect annotationMarginBoxVisualRect(const Box& annotationBox, InlineLayoutUnit lineHeight, InlineFormattingContext& inlineFormattingContext)
{
    auto marginBoxLogicalRect = InlineRect { BoxGeometry::marginBoxRect(inlineFormattingContext.geometryForBox(annotationBox)) };
    auto writingMode = inlineFormattingContext.root().writingMode();
    if (writingMode.isHorizontal())
        return marginBoxLogicalRect;
    auto visualTopLeft = marginBoxLogicalRect.topLeft().transposedPoint();
    if (writingMode.isBlockFlipped())
        return InlineRect { visualTopLeft, marginBoxLogicalRect.size().transposedSize() };
    visualTopLeft.move(lineHeight - marginBoxLogicalRect.height(), { });
    return InlineRect { visualTopLeft, marginBoxLogicalRect.size().transposedSize() };
}

static InlineLayoutUnit baseLogicalWidthFromRubyBaseEnd(const Box& rubyBaseLayoutBox, const Line::RunList& lineRuns, const InlineContentBreaker::ContinuousContent::RunList& candidateRuns)
{
    ASSERT(rubyBaseLayoutBox.isRubyBase());
    // Canidate content is supposed to hold the base content and in case of soft wrap opportunities, line may have some base content too.
    auto baseLogicalWidth = InlineLayoutUnit { 0.f };
    auto hasSeenRubyBaseStart = false;
    for (auto& candidateRun : makeReversedRange(candidateRuns)) {
        auto& inlineItem = candidateRun.inlineItem;
        if (inlineItem.isInlineBoxStart() && &inlineItem.layoutBox() == &rubyBaseLayoutBox) {
            hasSeenRubyBaseStart = true;
            break;
        }
        baseLogicalWidth += candidateRun.contentWidth();
    }
    if (hasSeenRubyBaseStart)
        return baseLogicalWidth;
    // Let's check the line for the rest of the base content.
    for (auto& lineRun : makeReversedRange(lineRuns)) {
        if ((lineRun.isInlineBoxStart() || lineRun.isLineSpanningInlineBoxStart()) && &lineRun.layoutBox() == &rubyBaseLayoutBox)
            break;
        baseLogicalWidth += lineRun.logicalWidth();
    }
    return baseLogicalWidth;
}

static bool annotationOverlapCheck(const InlineDisplay::Box& adjacentDisplayBox, const InlineLayoutRect& overhangingRect, InlineLayoutUnit lineLogicalHeight, InlineFormattingContext& inlineFormattingContext)
{
    // We are in the middle of a line, should not see any line breaks or ellipsis boxes here.
    ASSERT(!adjacentDisplayBox.isEllipsis() && !adjacentDisplayBox.isRootInlineBox());
    // Skip empty content like <span></span>
    if (adjacentDisplayBox.visualRectIgnoringBlockDirection().isEmpty())
        return false;

    if (adjacentDisplayBox.inkOverflow().intersects(overhangingRect))
        return true;
    auto& adjacentLayoutBox = adjacentDisplayBox.layoutBox();
    // Adjacent ruby may have overlapping annotation.
    if (adjacentLayoutBox.isRubyBase() && adjacentLayoutBox.associatedRubyAnnotationBox())
        return annotationMarginBoxVisualRect(*adjacentLayoutBox.associatedRubyAnnotationBox(), lineLogicalHeight, inlineFormattingContext).intersects(overhangingRect);
    return false;
}

InlineLayoutUnit RubyFormattingContext::annotationBoxLogicalWidth(const Box& rubyBaseLayoutBox, InlineFormattingContext& inlineFormattingContext)
{
    ASSERT(rubyBaseLayoutBox.isRubyBase());
    auto* annotationBox = rubyBaseLayoutBox.associatedRubyAnnotationBox();
    if (!annotationBox)
        return { };

    inlineFormattingContext.integrationUtils().layoutWithFormattingContextForBox(*annotationBox);

    return inlineFormattingContext.geometryForBox(*annotationBox).marginBoxWidth();
}

InlineLayoutUnit RubyFormattingContext::baseEndAdditionalLogicalWidth(const Box& rubyBaseLayoutBox, const Line::RunList& lineRuns, const InlineContentBreaker::ContinuousContent::RunList& candidateRuns, InlineFormattingContext& inlineFormattingContext)
{
    ASSERT(rubyBaseLayoutBox.isRubyBase());
    if (hasInterlinearAnnotation(rubyBaseLayoutBox)) {
        // Base is supposed be at least as wide as the annotation is.
        // Let's adjust the inline box end width to accomodate such overflowing interlinear annotations.
        auto rubyBaseContentWidth = baseLogicalWidthFromRubyBaseEnd(rubyBaseLayoutBox, lineRuns, candidateRuns);
        ASSERT(rubyBaseContentWidth >= 0);
        return std::max(0.f, annotationBoxLogicalWidth(rubyBaseLayoutBox, inlineFormattingContext) - rubyBaseContentWidth);
    }
    // While inter-character annotations don't participate in inline layout, they take up space.
    return annotationBoxLogicalWidth(rubyBaseLayoutBox, inlineFormattingContext);
}

size_t RubyFormattingContext::applyRubyAlignOnBaseContent(size_t rubyBaseStart, Line& line, UncheckedKeyHashMap<const Box*, InlineLayoutUnit>& alignmentOffsetList, InlineFormattingContext& inlineFormattingContext)
{
    auto& runs = line.runs();
    if (runs.isEmpty()) {
        ASSERT_NOT_REACHED();
        return rubyBaseStart;
    }
    auto& rubyBaseLayoutBox = runs[rubyBaseStart].layoutBox();
    auto rubyBaseEnd = [&]() -> std::optional<size_t> {
        auto& rubyBox = rubyBaseLayoutBox.parent();
        for (auto index = rubyBaseStart + 1; index < runs.size(); ++index) {
            if (&runs[index].layoutBox().parent() == &rubyBox)
                return index;
        }
        // We somehow managed to break content inside the base.
        return { };
    }();
    if (rubyBaseEnd && *rubyBaseEnd - rubyBaseStart == 1) {
        // Blank base needs no alignment.
        return *rubyBaseEnd;
    }
    auto* annotationBox = rubyBaseLayoutBox.associatedRubyAnnotationBox();
    if (!annotationBox)
        return rubyBaseStart + 1;

    inlineFormattingContext.integrationUtils().layoutWithFormattingContextForBox(*annotationBox);

    auto annotationBoxLogicalWidth = InlineLayoutUnit { inlineFormattingContext.geometryForBox(*annotationBox).marginBoxWidth() };
    auto baseContentLogicalWidth = (rubyBaseEnd ? runs[*rubyBaseEnd].logicalLeft() : runs.last().logicalRight()) - runs[rubyBaseStart].logicalRight();
    if (annotationBoxLogicalWidth <= baseContentLogicalWidth)
        return rubyBaseStart + 1;

    auto spaceToDistribute = annotationBoxLogicalWidth - baseContentLogicalWidth;
    auto alignmentOffset = InlineContentAligner::applyRubyAlign(rubyBaseLayoutBox.style().rubyAlign(), line.runs(), { rubyBaseStart, rubyBaseEnd ? *rubyBaseEnd + 1 : runs.size() }, spaceToDistribute);
    if (rubyBaseEnd) {
    // Reset the spacing we added at LineBuilder.
        auto& rubyBaseEndRun = runs[*rubyBaseEnd];
    rubyBaseEndRun.shrinkHorizontally(spaceToDistribute);
    rubyBaseEndRun.moveHorizontally(2 * alignmentOffset);
    }

    ASSERT(!alignmentOffsetList.contains(&rubyBaseLayoutBox));
    alignmentOffsetList.add(&rubyBaseLayoutBox, alignmentOffset);
    return rubyBaseEnd.value_or(runs.size());
}

UncheckedKeyHashMap<const Box*, InlineLayoutUnit> RubyFormattingContext::applyRubyAlign(Line& line, InlineFormattingContext& inlineFormattingContext)
{
    UncheckedKeyHashMap<const Box*, InlineLayoutUnit> alignmentOffsetList;
    // https://drafts.csswg.org/css-ruby/#interlinear-inline
    // Within each base and annotation box, how the extra space is distributed when its content is narrower than
    // the measure of the box is specified by its ruby-align property.
    auto& runs = line.runs();
    for (size_t index = 0; index < runs.size(); ++index) {
        auto& lineRun = runs[index];
        if (lineRun.isInlineBoxStart() && lineRun.layoutBox().isRubyBase())
            index = applyRubyAlignOnBaseContent(index, line, alignmentOffsetList, inlineFormattingContext);
    }
    return alignmentOffsetList;
}

InlineLayoutUnit RubyFormattingContext::applyRubyAlignOnAnnotationBox(Line& line, InlineLayoutUnit spaceToDistribute, InlineFormattingContext& inlineFormattingContext)
{
    return InlineContentAligner::applyRubyAlign(inlineFormattingContext.root().style().rubyAlign(), line.runs(), { 0, line.runs().size() }, spaceToDistribute);
}

void RubyFormattingContext::applyAlignmentOffsetList(InlineDisplay::Boxes& displayBoxes, const UncheckedKeyHashMap<const Box*, InlineLayoutUnit>& alignmentOffsetList, RubyBasesMayNeedResizing rubyBasesMayNeedResizing, InlineFormattingContext& inlineFormattingContext)
{
    if (alignmentOffsetList.isEmpty())
        return;
    InlineContentAligner::applyRubyBaseAlignmentOffset(displayBoxes, alignmentOffsetList, rubyBasesMayNeedResizing == RubyBasesMayNeedResizing::No ? InlineContentAligner::AdjustContentOnlyInsideRubyBase::Yes : InlineContentAligner::AdjustContentOnlyInsideRubyBase::No, inlineFormattingContext);
}

void RubyFormattingContext::applyAnnotationAlignmentOffset(InlineDisplay::Boxes& displayBoxes, InlineLayoutUnit alignmentOffset, InlineFormattingContext& inlineFormattingContext)
{
    if (!alignmentOffset)
        return;
    InlineContentAligner::applyRubyAnnotationAlignmentOffset(displayBoxes, alignmentOffset, inlineFormattingContext);
}

InlineLayoutUnit RubyFormattingContext::baseEndAdditionalLogicalWidth(const Box& rubyBaseLayoutBox, const InlineDisplay::Box&, InlineLayoutUnit baseContentWidth, InlineFormattingContext& inlineFormattingContext)
{
    if (!hasInterCharacterAnnotation(rubyBaseLayoutBox)) {
        // FIXME: We may want to include interlinear annotations here too so that applyAlignmentOffsetList would not need to initiate resizing (only moving base content).
        if (baseContentWidth)
            return { };
        auto* annotationBox = rubyBaseLayoutBox.associatedRubyAnnotationBox();
        if (!annotationBox)
            return { };
        auto& annotationBoxLogicalGeometry = inlineFormattingContext.geometryForBox(*annotationBox);
        return annotationBoxLogicalGeometry.marginBoxWidth();
    }
    // Note that intercharacter annotation stays vertical even when the ruby itself is vertical (which makes it look like interlinear).
    return annotationBoxLogicalWidth(rubyBaseLayoutBox, inlineFormattingContext);
}

InlineLayoutPoint RubyFormattingContext::placeAnnotationBox(const Box& rubyBaseLayoutBox, const Rect& rubyBaseMarginBoxLogicalRect, InlineFormattingContext& inlineFormattingContext)
{
    ASSERT(rubyBaseLayoutBox.isRubyBase());
    auto* annotationBox = rubyBaseLayoutBox.associatedRubyAnnotationBox();
    if (!annotationBox) {
        ASSERT_NOT_REACHED();
        return { };
    }
    auto& annotationBoxLogicalGeometry = inlineFormattingContext.geometryForBox(*annotationBox);

    if (hasInterlinearAnnotation(rubyBaseLayoutBox)) {
        // Move it over/under the base and make it border box positioned.
        auto leftOffset = annotationBoxLogicalGeometry.marginStart();
        auto topOffset = rubyPosition(rubyBaseLayoutBox) == RubyPosition::Over ? -annotationBoxLogicalGeometry.marginBoxHeight() : rubyBaseMarginBoxLogicalRect.height();
        topOffset += annotationBoxLogicalGeometry.marginBefore();

        auto logicalTopLeft = rubyBaseMarginBoxLogicalRect.topLeft();
        logicalTopLeft.move(LayoutSize { leftOffset, topOffset });
        return logicalTopLeft;
    }
    // Inter-character annotation box is stretched to the size of the base content box and vertically centered.
    auto annotationContentBoxLogicalHeight = annotationBoxLogicalGeometry.contentBoxHeight();
    auto annotationBorderTop = annotationBoxLogicalGeometry.borderBefore();
    auto borderBoxRight = rubyBaseMarginBoxLogicalRect.right() - annotationBoxLogicalGeometry.marginBoxWidth() + annotationBoxLogicalGeometry.marginStart();
    return { borderBoxRight, rubyBaseMarginBoxLogicalRect.top() + ((rubyBaseMarginBoxLogicalRect.height() - annotationContentBoxLogicalHeight) / 2) - annotationBorderTop };
}

InlineLayoutSize RubyFormattingContext::sizeAnnotationBox(const Box& rubyBaseLayoutBox, const Rect& rubyBaseMarginBoxLogicalRect, InlineFormattingContext& inlineFormattingContext)
{
    // FIXME: This is where we should take advantage of the ruby-column setup.
    ASSERT(rubyBaseLayoutBox.isRubyBase());
    auto* annotationBox = rubyBaseLayoutBox.associatedRubyAnnotationBox();
    if (!annotationBox) {
        ASSERT_NOT_REACHED();
        return { };
    }
    auto& annotationBoxLogicalGeometry = inlineFormattingContext.geometryForBox(*annotationBox);
    if (hasInterlinearAnnotation(rubyBaseLayoutBox)) {
        // Layout the annotation box again if we decided to change its size.
        auto newWidth = std::max(rubyBaseMarginBoxLogicalRect.width(), annotationBoxLogicalGeometry.marginBoxWidth());
        if (newWidth != annotationBoxLogicalGeometry.marginBoxWidth())
            inlineFormattingContext.integrationUtils().layoutWithFormattingContextForBox(*annotationBox, newWidth);

        return { newWidth - annotationBoxLogicalGeometry.horizontalMarginBorderAndPadding(), annotationBoxLogicalGeometry.contentBoxHeight() };
    }

    return annotationBoxLogicalGeometry.contentBoxSize();
}

void RubyFormattingContext::adjustLayoutBoundsAndStretchAncestorRubyBase(LineBox& lineBox, InlineLevelBox& rubyBaseInlineBox, MaximumLayoutBoundsStretchMap& descendantRubySet, const InlineFormattingContext& inlineFormattingContext)
{
    auto& rubyBaseLayoutBox = rubyBaseInlineBox.layoutBox();
    ASSERT(rubyBaseLayoutBox.isRubyBase());

    auto stretchAncestorRubyBaseIfApplicable = [&](auto layoutBounds) {
        auto& rootBox = inlineFormattingContext.root();
        for (auto* ancestor = &rubyBaseLayoutBox.parent(); ancestor != &rootBox; ancestor = &ancestor->parent()) {
            if (ancestor->isRubyBase()) {
                auto* ancestorInlineBox = lineBox.inlineLevelBoxFor(*ancestor);
                if (!ancestorInlineBox) {
                    ASSERT_NOT_REACHED();
                    break;
                }
                auto previousDescendantLayoutBounds = descendantRubySet.get(ancestorInlineBox);
                descendantRubySet.set(ancestorInlineBox, InlineLevelBox::AscentAndDescent { std::max(previousDescendantLayoutBounds.ascent, layoutBounds.ascent), std::max(previousDescendantLayoutBounds.descent, layoutBounds.descent) });
                break;
            }
        }
    };

    auto layoutBounds = rubyBaseInlineBox.layoutBounds();
    auto* annotationBox = rubyBaseLayoutBox.associatedRubyAnnotationBox();
    if (!annotationBox || !hasInterlinearAnnotation(rubyBaseLayoutBox)) {
        // Make sure descendant rubies with annotations are propagated.
        stretchAncestorRubyBaseIfApplicable(layoutBounds);
        return;
    }

    auto over = InlineLayoutUnit { };
    auto under = InlineLayoutUnit { };
    auto annotationBoxLogicalHeight = InlineLayoutUnit { inlineFormattingContext.geometryForBox(*annotationBox).marginBoxHeight() };
    auto isAnnotationBefore = rubyPosition(rubyBaseLayoutBox) == RubyPosition::Over;
    if (isAnnotationBefore)
        over = annotationBoxLogicalHeight;
    else
        under = annotationBoxLogicalHeight;

    // FIXME: The spec says annotation should not stretch the line unless line-height is not normal and annotation does not fit (i.e. line is sized too small for the annotation)
    // Legacy ruby behaves slightly differently by stretching the line box as needed.
    auto isFirstFormattedLine = !lineBox.lineIndex();
    auto descendantLayoutBounds = descendantRubySet.get(&rubyBaseInlineBox);
    auto ascent = std::max(rubyBaseInlineBox.ascent(), descendantLayoutBounds.ascent);
    auto descent = std::max(rubyBaseInlineBox.descent(), descendantLayoutBounds.descent);

    if (rubyBaseInlineBox.isPreferredLineHeightFontMetricsBased()) {
        auto extraSpaceForAnnotation = InlineLayoutUnit { };
        if (!isFirstFormattedLine) {
            // Note that annotation may leak into the half leading space (gap between lines).
            auto lineGap = rubyBaseLayoutBox.style().metricsOfPrimaryFont().intLineSpacing();
            extraSpaceForAnnotation = std::max(0.f, (lineGap - (ascent + descent)) / 2);
        }
        auto ascentWithAnnotation = (ascent + over) - extraSpaceForAnnotation;
        auto descentWithAnnotation = (descent + under) - extraSpaceForAnnotation;

        layoutBounds.ascent = std::max(ascentWithAnnotation, layoutBounds.ascent);
        layoutBounds.descent = std::max(descentWithAnnotation, layoutBounds.descent);
    } else {
        auto ascentWithAnnotation = ascent + over;
        auto descentWithAnnotation = descent + under;
        // FIXME: Normally we would check if there's space for both the ascent and the descent parts of the content
        // but in order to keep ruby tight we let subsequent lines (potentially) overlap each other by
        // only checking against total height (this affects the annotation box vertical placement by letting it overlap the previous line's descent)
        // However we have to make sure there's enough space for the annotation box on the first line.
        // This tight content arrangement is a legacy ruby behavior (see placeChildInlineBoxesInBlockDirection) and we may wanna reconsider it at some point.
        if (isFirstFormattedLine) {
            layoutBounds.ascent = std::max(ascentWithAnnotation, layoutBounds.ascent);
            layoutBounds.descent = std::max(descentWithAnnotation, layoutBounds.descent);
        } else if (layoutBounds.height() < ascentWithAnnotation + descentWithAnnotation) {
            // In case line-height does not produce enough space for annotation.
            auto extraSpaceNeededForAnnotation = (ascentWithAnnotation + descentWithAnnotation) - layoutBounds.height();
            // Note that this makes annotation leak into previous/next line's (bottom/top) half leading. It ensures though that we don't
            // overly stretch lines and break (logical) vertical rhythm too much.
            if (isAnnotationBefore)
                layoutBounds.ascent += extraSpaceNeededForAnnotation;
            else
                layoutBounds.descent += extraSpaceNeededForAnnotation;
        }
    }

    rubyBaseInlineBox.setLayoutBounds(layoutBounds);
    stretchAncestorRubyBaseIfApplicable(layoutBounds);
}

void RubyFormattingContext::applyAnnotationContributionToLayoutBounds(LineBox& lineBox, const InlineFormattingContext& inlineFormattingContext)
{
    // In order to ensure consistent spacing of lines, documents with ruby typically ensure that the line-height is
    // large enough to accommodate ruby between lines of text. Therefore, ordinarily, ruby annotation containers and ruby annotation
    // boxes do not contribute to the measured height of a line's inline contents;
    // line-height calculations are performed using only the ruby base container, exactly as if it were a normal inline.
    // However, if the line-height specified on the ruby container is less than the distance between the top of the top ruby annotation
    // container and the bottom of the bottom ruby annotation container, then additional leading is added on the appropriate side(s).
    MaximumLayoutBoundsStretchMap descentRubySet;
    for (auto& inlineLevelBox : makeReversedRange(lineBox.nonRootInlineLevelBoxes())) {
        if (!inlineLevelBox.isInlineBox() || !inlineLevelBox.layoutBox().isRubyBase())
            continue;
        adjustLayoutBoundsAndStretchAncestorRubyBase(lineBox, inlineLevelBox, descentRubySet, inlineFormattingContext);
    }
}

InlineLayoutUnit RubyFormattingContext::overhangForAnnotationBefore(const Box& rubyBaseLayoutBox, size_t rubyBaseStart, const InlineDisplay::Boxes& boxes, InlineLayoutUnit lineLogicalHeight, InlineFormattingContext& inlineFormattingContext)
{
    // [root inline box][ruby container][ruby base][ruby annotation]
    ASSERT(rubyBaseStart >= 2);
    auto* annotationBox = rubyBaseLayoutBox.associatedRubyAnnotationBox();
    if (!annotationBox || !hasInterlinearAnnotation(rubyBaseLayoutBox) || rubyBaseStart <= 2)
        return { };
    if (rubyBaseStart + 1 >= boxes.size()) {
        // We have to have some base content.
        ASSERT_NOT_REACHED();
        return { };
    }
    auto isHorizontalWritingMode = inlineFormattingContext.root().writingMode().isHorizontal();
    auto baseContentStart = baseContentIndex(rubyBaseStart, boxes);
    if (baseContentStart >= boxes.size()) {
        ASSERT_NOT_REACHED();
        return { };
    }
    // FIXME: Usually the first content box is visually the leftmost, but we should really look for content shifted to the left through negative margins on inline boxes.
    auto gapBetweenBaseAndContent = [&] {
        auto contentVisualRect = boxes[baseContentStart].visualRectIgnoringBlockDirection();
        auto baseVisualRect = boxes[rubyBaseStart].visualRectIgnoringBlockDirection();
        if (isHorizontalWritingMode)
            return std::max(0.f, contentVisualRect.x() - baseVisualRect.x());
        return std::max(0.f, contentVisualRect.y() - baseVisualRect.y());
    };
    auto overhangValue = std::min(halfOfAFullWidthCharacter(*annotationBox), gapBetweenBaseAndContent());
    auto wouldAnnotationOrBaseOverlapAdjacentContent = [&] {
        // Check of adjacent (previous) content for overlapping.
        auto overhangingAnnotationVisualRect = annotationMarginBoxVisualRect(*annotationBox, lineLogicalHeight, inlineFormattingContext);
        auto baseContentBoxRect = boxes[baseContentStart].inkOverflow();
        // This is how much the annotation box/base content would be closer to content outside of base.
        auto offset = isHorizontalWritingMode ? InlineLayoutPoint(-overhangValue, 0.f) : InlineLayoutPoint(0.f, -overhangValue);
        overhangingAnnotationVisualRect.moveBy(offset);
        baseContentBoxRect.moveBy(offset);

        for (size_t index = 1; index < rubyBaseStart - 1; ++index) {
            auto& previousDisplayBox = boxes[index];
            if (annotationOverlapCheck(previousDisplayBox, overhangingAnnotationVisualRect, lineLogicalHeight, inlineFormattingContext))
                return true;
            if (annotationOverlapCheck(previousDisplayBox, baseContentBoxRect, lineLogicalHeight, inlineFormattingContext))
                return true;
        }
        return false;
    };
    return wouldAnnotationOrBaseOverlapAdjacentContent() ? 0.f : overhangValue;
}

InlineLayoutUnit RubyFormattingContext::overhangForAnnotationAfter(const Box& rubyBaseLayoutBox, WTF::Range<size_t> rubyBaseRange, const InlineDisplay::Boxes& boxes, InlineLayoutUnit lineLogicalHeight, InlineFormattingContext& inlineFormattingContext)
{
    auto* annotationBox = rubyBaseLayoutBox.associatedRubyAnnotationBox();
    if (!annotationBox || !hasInterlinearAnnotation(rubyBaseLayoutBox))
        return { };

    if (!rubyBaseRange || rubyBaseRange.distance() == 1 || rubyBaseRange.end() == boxes.size())
        return { };

    auto isHorizontalWritingMode = inlineFormattingContext.root().writingMode().isHorizontal();
    // FIXME: Usually the last content box is visually the rightmost, but negative margin may override it.
    // FIXME: Currently justified content always expands producing 0 value for gapBetweenBaseEndAndContent.
    auto rubyBaseContentEnd = rubyBaseRange.end() - 1;
    auto gapBetweenBaseEndAndContent = [&] {
        auto baseStartVisualRect = boxes[rubyBaseRange.begin()].visualRectIgnoringBlockDirection();
        auto baseContentEndVisualRect = boxes[rubyBaseContentEnd].visualRectIgnoringBlockDirection();
        if (isHorizontalWritingMode)
            return std::max(0.f, baseStartVisualRect.maxX() - baseContentEndVisualRect.maxX());
        return std::max(0.f, baseStartVisualRect.maxY() - baseContentEndVisualRect.maxY());
    };
    auto overhangValue = std::min(halfOfAFullWidthCharacter(*annotationBox), gapBetweenBaseEndAndContent());
    auto wouldAnnotationOrBaseOverlapLineContent = [&] {
        // Check of adjacent (next) content for overlapping.
        auto overhangingAnnotationVisualRect = annotationMarginBoxVisualRect(*annotationBox, lineLogicalHeight, inlineFormattingContext);
        auto baseContentBoxRect = boxes[rubyBaseContentEnd].inkOverflow();
        // This is how much the base content would be closer to content outside of base.
        auto offset = isHorizontalWritingMode ? InlineLayoutPoint(overhangValue, 0.f) : InlineLayoutPoint(0.f, overhangValue);
        overhangingAnnotationVisualRect.moveBy(offset);
        baseContentBoxRect.moveBy(offset);

        for (size_t index = boxes.size() - 1; index >= rubyBaseRange.end(); --index) {
            auto& previousDisplayBox = boxes[index];
            if (annotationOverlapCheck(previousDisplayBox, overhangingAnnotationVisualRect, lineLogicalHeight, inlineFormattingContext))
                return true;
            if (annotationOverlapCheck(previousDisplayBox, baseContentBoxRect, lineLogicalHeight, inlineFormattingContext))
                return true;
        }
        return false;
    };
    return wouldAnnotationOrBaseOverlapLineContent() ? 0.f : overhangValue;
}

bool RubyFormattingContext::hasInterlinearAnnotation(const Box& rubyBaseLayoutBox)
{
    ASSERT(rubyBaseLayoutBox.isRubyBase());
    return rubyBaseLayoutBox.associatedRubyAnnotationBox() && !hasInterCharacterAnnotation(rubyBaseLayoutBox);
}

bool RubyFormattingContext::hasInterCharacterAnnotation(const Box& rubyBaseLayoutBox)
{
    ASSERT(rubyBaseLayoutBox.isRubyBase());
    if (!rubyBaseLayoutBox.writingMode().isHorizontal()) {
        // If the writing mode of the enclosing ruby container is vertical, this value has the same effect as over.
        return false;
    }

    if (auto* annotationBox = rubyBaseLayoutBox.associatedRubyAnnotationBox())
        return annotationBox->style().isInterCharacterRubyPosition();
    return false;
}

void RubyFormattingContext::applyRubyOverhang(InlineFormattingContext& parentFormattingContext, InlineLayoutUnit lineLogicalHeight, InlineDisplay::Boxes& displayBoxes, const Vector<WTF::Range<size_t>>& interlinearRubyColumnRangeList)
{
    // FIXME: We are only supposed to apply overhang when annotation box is wider than base, but at this point we can't tell (this needs to be addressed together with annotation box sizing).
    if (interlinearRubyColumnRangeList.isEmpty())
        return;

    auto isHorizontalWritingMode = parentFormattingContext.root().writingMode().isHorizontal();
    for (auto startEndPair : interlinearRubyColumnRangeList) {
        ASSERT(startEndPair);
        if (startEndPair.distance() == 1)
            continue;

        auto rubyBaseStart = startEndPair.begin();
        auto& rubyBaseLayoutBox = displayBoxes[rubyBaseStart].layoutBox();
        ASSERT(rubyBaseLayoutBox.isRubyBase());
        ASSERT(hasInterlinearAnnotation(rubyBaseLayoutBox));
        if (rubyBaseLayoutBox.style().rubyOverhang() == RubyOverhang::None)
            continue;

        auto beforeOverhang = overhangForAnnotationBefore(rubyBaseLayoutBox, rubyBaseStart, displayBoxes, lineLogicalHeight, parentFormattingContext);
        auto afterOverhang = overhangForAnnotationAfter(rubyBaseLayoutBox, { rubyBaseStart, startEndPair.end() }, displayBoxes, lineLogicalHeight, parentFormattingContext);

        // FIXME: If this turns out to be a pref bottleneck, make sure we pass in the accumulated shift to overhangForAnnotationBefore/after and
        // offset all box geometry as we check for overlap.
        auto moveBoxRangeToVisualLeft = [&](auto start, auto end, auto shiftValue) {
            for (auto index = start; index <= end; ++index) {
                auto& displayBox = displayBoxes[index];
                isHorizontalWritingMode ? displayBox.moveHorizontally(-shiftValue) : displayBox.moveVertically(-shiftValue);

                auto& layoutBox = displayBox.layoutBox();
                if (displayBox.isInlineLevelBox() && !displayBox.isRootInlineBox())
                    parentFormattingContext.geometryForBox(layoutBox).moveHorizontally(LayoutUnit { -shiftValue });

                if (layoutBox.isRubyBase() && layoutBox.associatedRubyAnnotationBox())
                    parentFormattingContext.geometryForBox(*layoutBox.associatedRubyAnnotationBox()).moveHorizontally(LayoutUnit { -shiftValue });
            }
        };
        auto hasJustifiedAdjacentAfterContent = [&] {
            if (startEndPair.end() == displayBoxes.size())
                return false;
            auto& afterRubyBaseDisplayBox = displayBoxes[startEndPair.end()];
            if (afterRubyBaseDisplayBox.layoutBox().isRubyBase()) {
                // Adjacent content is also a ruby base.
                return false;
            }
            return !!afterRubyBaseDisplayBox.expansion().horizontalExpansion;
        }();

        if (beforeOverhang) {
            // When "before" adjacent content slightly pulls the rest of the content on the line leftward, justify content should stay intact.
            moveBoxRangeToVisualLeft(rubyBaseStart, hasJustifiedAdjacentAfterContent ? startEndPair.end() : displayBoxes.size() - 1, beforeOverhang);
        }
        if (afterOverhang) {
            // Normally we shift all the "after" boxes to the left here as one monolithic content
            // but in case of justified alignment we can only move the adjacent run under the annotation
            // and expand the justified space to keep the rest of the runs stationary.
            if (hasJustifiedAdjacentAfterContent) {
                auto& afterRubyBaseDisplayBox = displayBoxes[startEndPair.end()];
                auto expansion = afterRubyBaseDisplayBox.expansion();
                auto inflateValue = afterOverhang + beforeOverhang;
                afterRubyBaseDisplayBox.setExpansion({ expansion.behavior, expansion.horizontalExpansion + inflateValue });
                isHorizontalWritingMode ? afterRubyBaseDisplayBox.expandHorizontally(inflateValue) : afterRubyBaseDisplayBox.expandVertically(inflateValue);
                moveBoxRangeToVisualLeft(startEndPair.end(), startEndPair.end(), afterOverhang);
            } else
                moveBoxRangeToVisualLeft(startEndPair.end(), displayBoxes.size() - 1, afterOverhang);
        }
    }
}

}
}

