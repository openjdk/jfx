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
#include "InlineContentConstrainer.h"

#include "InlineLineBuilder.h"
#include "RenderStyleInlines.h"
#include <limits>
#include <wtf/MathExtras.h>
#include <wtf/PriorityQueue.h>

namespace WebCore {
namespace Layout {

// Ideally, the act of balancing inline content will use the same number of lines as if the inline content
// was laid out via `text-wrap: wrap`. However, adhering to this ideal is expensive (quadratic in the number
// of break opportunities), and not caring about this ideal will allow us to use a more efficient algorithm.
// Typically, if inline content spans many lines, the likelihood of someone caring about the vertical space
// used decreases. So, we ignore this ideal number of lines requirement beyond this threshold.
static const size_t maximumLinesToBalanceWithLineRequirement { 12 };

// Define the penalty associated with show text wider/narrower than ideal bounds.
// Separating stretchability and shrinkability allows us to weight under/over
// filling the ideal bounds differently.
static const InlineLayoutUnit textWrapPrettyStretchability = 10;
static const InlineLayoutUnit textWrapPrettyShrinkability = 10;

// Defines the maximum shrink/stretch factor allowed for text-wrap-pretty.
static const float textWrapPrettyMaxStretch = 2;
static const float textWrapPrettyMaxShrink = 2;

static bool validLineWidthPretty(InlineLayoutUnit candidateLineWidth, InlineLayoutUnit idealLineWidth)
{
    auto difference = candidateLineWidth - idealLineWidth;
    if (difference > 0)
        return difference <= textWrapPrettyStretchability * textWrapPrettyMaxStretch;
    return abs(difference) <= textWrapPrettyShrinkability * textWrapPrettyMaxShrink;
}

// Full implementation of the raggedness function defined in:
// http://www.eprg.org/G53DOC/pdfs/knuth-plass-breaking.pdf
static float computeRaggedness(InlineLayoutUnit candidateLineWidth, InlineLayoutUnit idealLineWidth)
{
    auto difference = candidateLineWidth - idealLineWidth;
    auto intermediate = difference / (difference > 0 ? textWrapPrettyStretchability : textWrapPrettyShrinkability);
    return 100 * abs(pow(intermediate, 3));
};

static float computeCostBalance(InlineLayoutUnit candidateLineWidth, InlineLayoutUnit idealLineWidth)
{
    return computeRaggedness(candidateLineWidth, idealLineWidth);
};

static float computeCostPretty(InlineLayoutUnit candidateLineWidth, InlineLayoutUnit idealLineWidth, size_t breakIndex, size_t numberOfBreakOpportunities, InlineLayoutUnit lastLineWidth)
{
    // Keeping the last line width longer than 20% of the previous is a heuristic to avoid orphan and "orphan-like" paragraph endings
    // (lines that have more than one word but are still sufficiently short to appear like an orphan)
    // FIXME: add support for river minimization.
    if (breakIndex == numberOfBreakOpportunities - 1) {
        const auto minimumLastLineWidth = lastLineWidth * 0.2;
        const auto maximumLastLineWidth = lastLineWidth;
        if (candidateLineWidth < minimumLastLineWidth || candidateLineWidth > maximumLastLineWidth)
            return std::numeric_limits<float>::infinity();
        return 0;
    }
    // text-wrap-mode:pretty disallows stretching/shrinking beyond accepted bounds.
    if (!validLineWidthPretty(candidateLineWidth, idealLineWidth))
        return std::numeric_limits<float>::infinity();

    return computeRaggedness(candidateLineWidth, idealLineWidth);
};

static LayoutUnit computeLineWidthFromSlidingWidth(InlineLayoutUnit indentWidth, SlidingWidth slidingWidth)
{
    return LayoutUnit::fromFloatCeil(indentWidth + slidingWidth.width() + LayoutUnit::epsilon());
}

static bool containsTrailingSoftHyphen(const InlineItem& inlineItem)
{
    if (inlineItem.style().hyphens() == Hyphens::None)
        return false;
    auto* textItem = dynamicDowncast<InlineTextItem>(inlineItem);
    if (!textItem)
        return false;
    return textItem->hasTrailingSoftHyphen();
}

static bool containsPreservedTab(const InlineItem& inlineItem)
{
    auto* textItem = dynamicDowncast<InlineTextItem>(inlineItem);
    if (!textItem)
        return false;
    if (!textItem->isWhitespace())
        return false;
    const auto& textBox = textItem->inlineTextBox();
    if (!TextUtil::shouldPreserveSpacesAndTabs(textBox))
        return false;
    auto start = textItem->start();
    auto length = textItem->length();
    const auto& textContent = textBox.content();
    for (size_t index = start; index < start + length; index++) {
        if (textContent[index] == tabCharacter)
            return true;
    }
    return false;
}

static bool cannotConstrainInlineItem(const InlineItem& inlineItem)
{
    if (!inlineItem.layoutBox().isInlineLevelBox())
        return true;
    if (containsTrailingSoftHyphen(inlineItem))
        return true;
    if (containsPreservedTab(inlineItem))
        return true;
    if (inlineItem.style().boxDecorationBreak() == BoxDecorationBreak::Clone)
        return true;
    return false;
}

static PreviousLine buildPreviousLine(size_t lineIndex, LineLayoutResult lineLayoutResult)
{
    return PreviousLine { lineIndex, lineLayoutResult.contentGeometry.trailingOverflowingContentWidth, !lineLayoutResult.inlineContent.isEmpty() && lineLayoutResult.inlineContent.last().isLineBreak(), !lineLayoutResult.inlineContent.isEmpty(), lineLayoutResult.directionality.inlineBaseDirection, WTFMove(lineLayoutResult.floatContent.suspendedFloats) };
}

InlineContentConstrainer::InlineContentConstrainer(InlineFormattingContext& inlineFormattingContext, const InlineItemList& inlineItemList, const HorizontalConstraints& horizontalConstraints)
    : m_inlineFormattingContext(inlineFormattingContext)
    , m_inlineItemList(inlineItemList)
    , m_horizontalConstraints(horizontalConstraints)
{
    initialize();
}

void InlineContentConstrainer::updateCachedWidths()
{
    // We should only initialize the inline item width cache once.
    ASSERT(!m_hasValidInlineItemWidthCache);
    m_inlineItemWidths = Vector<InlineLayoutUnit>(m_numberOfInlineItems);
    m_firstLineStyleInlineItemWidths = Vector<InlineLayoutUnit>(m_numberOfInlineItems);
    // Cache inline item widths to speed up later computations.
    for (size_t i = 0; i < m_numberOfInlineItems; i++) {
        const auto& item = m_inlineItemList[i];
        m_inlineItemWidths[i] = m_inlineFormattingContext.formattingUtils().inlineItemWidth(item, 0, false);
        m_firstLineStyleInlineItemWidths[i] = m_inlineFormattingContext.formattingUtils().inlineItemWidth(item, 0, true);
    }
    m_hasValidInlineItemWidthCache = true;
}

void InlineContentConstrainer::checkCanConstrainInlineItems()
{
    for (auto& inlineItem : m_inlineItemList) {
        if (cannotConstrainInlineItem(inlineItem)) {
            m_cannotConstrainContent = true;
            return;
        }
    }
}

InlineContentConstrainer::EntryPretty InlineContentConstrainer::layoutSingleLineForPretty(InlineItemRange layoutRange, InlineLayoutUnit idealLineWidth, EntryPretty lastValidEntry)
{
    auto lineBuilder = LineBuilder { m_inlineFormattingContext, m_horizontalConstraints, m_inlineItemList };
    InlineRect lineInitialRect = InlineRect { 0.f, m_horizontalConstraints.logicalLeft, idealLineWidth, 0.f };
    LineLayoutResult lineLayoutResult = lineBuilder.layoutInlineContent({ layoutRange, lineInitialRect }, lastValidEntry.previousLine);
    auto lineEnd = InlineFormattingUtils::leadingInlineItemPositionForNextLine(lineLayoutResult.inlineItemRange.end, lastValidEntry.lineEnd, !lineLayoutResult.floatContent.hasIntrusiveFloat.isEmpty() || !lineLayoutResult.floatContent.placedFloats.isEmpty(), layoutRange.end);
    return { lastValidEntry.accumulatedCost,
        // This function is only called when there are no more viable break points for PrettifyRange.
        // Use the last valid entry's accumulated cost as we must use this breakpoint no matter what.
        layoutRange.startIndex(),
        lastValidEntry.lineIndex + 1,
        lineLayoutResult.contentGeometry.logicalWidth,
        lineEnd,
        buildPreviousLine(lastValidEntry.lineIndex + 1, lineLayoutResult)
    };
}

void InlineContentConstrainer::initialize()
{
    auto lineClamp = m_inlineFormattingContext.layoutState().parentBlockLayoutState().lineClamp();
    auto numberOfVisibleLinesAllowed = lineClamp ? std::make_optional(lineClamp->maximumLines) : std::nullopt;

    if (!m_inlineFormattingContext.layoutState().placedFloats().isEmpty()) {
        m_cannotConstrainContent = true;
        return;
    }

    // if we have a single line content, we don't have anything to be balanced.
    if (numberOfVisibleLinesAllowed == 1) {
        m_hasSingleLineVisibleContent = true;
        return;
    }

    m_numberOfInlineItems = m_inlineItemList.size();
    m_maximumLineWidthConstraint = m_horizontalConstraints.logicalWidth;

    checkCanConstrainInlineItems();
    if (m_cannotConstrainContent)
        return;

    // Perform a line layout with `text-wrap: wrap` to compute useful metrics such as:
    //  - the number of lines used
    //  - the original widths of each line
    //  - forced break locations
    auto layoutRange = InlineItemRange { 0, m_inlineItemList.size() };
    auto lineBuilder = LineBuilder { m_inlineFormattingContext, m_horizontalConstraints, m_inlineItemList };
    auto previousLineEnd = std::optional<InlineItemPosition> { };
    auto previousLine = std::optional<PreviousLine> { };
    auto lineIndex = 0lu;
    while (!layoutRange.isEmpty()) {
        auto lineInitialRect = InlineRect { 0.f, m_horizontalConstraints.logicalLeft, m_horizontalConstraints.logicalWidth, 0.f };
        auto lineLayoutResult = lineBuilder.layoutInlineContent({ layoutRange, lineInitialRect }, previousLine);

        // Record relevant geometry measurements from one line layout
        m_originalLineInlineItemRanges.append(lineLayoutResult.inlineItemRange);
        m_originalLineEndsWithForcedBreak.append(!lineLayoutResult.inlineContent.isEmpty() && lineLayoutResult.inlineContent.last().isLineBreak());
        bool useFirstLineStyle = !lineIndex;
        bool isFirstLineInChunk = !lineIndex || m_originalLineEndsWithForcedBreak[lineIndex - 1];
        SlidingWidth lineSlidingWidth { *this, m_inlineItemList, lineLayoutResult.inlineItemRange.startIndex(), lineLayoutResult.inlineItemRange.endIndex(), useFirstLineStyle, isFirstLineInChunk };
        auto previousLineEndsWithLineBreak = lineIndex ? std::optional<bool> { m_originalLineEndsWithForcedBreak[lineIndex - 1] } : std::nullopt;
        auto textIndent = m_inlineFormattingContext.formattingUtils().computedTextIndent(InlineFormattingUtils::IsIntrinsicWidthMode::No, previousLineEndsWithLineBreak, m_maximumLineWidthConstraint);
        m_originalLineConstraints.append(computeLineWidthFromSlidingWidth(textIndent, lineSlidingWidth));

        // If next line count would match (or exceed) the number of visible lines due to line-clamp, we can bail out early.
        if (numberOfVisibleLinesAllowed && (lineIndex + 1 >= numberOfVisibleLinesAllowed))
            break;

        layoutRange.start = InlineFormattingUtils::leadingInlineItemPositionForNextLine(lineLayoutResult.inlineItemRange.end, previousLineEnd, !lineLayoutResult.floatContent.hasIntrusiveFloat.isEmpty() || !lineLayoutResult.floatContent.placedFloats.isEmpty(), layoutRange.end);
        previousLineEnd = layoutRange.start;
        previousLine = buildPreviousLine(lineIndex, lineLayoutResult);
        lineIndex++;
    }

    // Cache inline item widths after laying out all inline content with LineBuilder.
    updateCachedWidths();
    m_numberOfLinesInOriginalLayout = lineIndex;
}

std::optional<Vector<LayoutUnit>> InlineContentConstrainer::computeParagraphLevelConstraints(TextWrapStyle wrapStyle)
{
    ASSERT(wrapStyle == TextWrapStyle::Balance || wrapStyle == TextWrapStyle::Pretty);

    if (m_cannotConstrainContent || m_hasSingleLineVisibleContent)
        return { };

    // If forced line breaks exist, then we can constrain each forced-break-delimited
    // chunk of text separately. This helps simplify first line/indentation logic.
    Vector<size_t> chunkSizes; // Number of lines per chunk of text
    size_t currentChunkSize = 0;
    for (size_t i = 0; i < m_originalLineInlineItemRanges.size(); i++) {
        currentChunkSize++;
        if (m_originalLineEndsWithForcedBreak[i]) {
            chunkSizes.append(currentChunkSize);
            currentChunkSize = 0;
        }
    }
    if (currentChunkSize > 0)
        chunkSizes.append(currentChunkSize);


    // Constrain each chunk
    auto constrainChunk = [&](auto chunkStart, auto chunkSize) -> std::optional<Vector<LayoutUnit>> {
        const bool isFirstChunk = !chunkStart;
        auto rangeToConstrain = InlineItemRange { m_originalLineInlineItemRanges[chunkStart].startIndex(), m_originalLineInlineItemRanges[chunkStart + chunkSize - 1].endIndex() };
        if (rangeToConstrain.startIndex() >= rangeToConstrain.endIndex())
            return { };
        InlineLayoutUnit totalWidth = 0;
        for (size_t line = 0; line < chunkSize; line++)
            totalWidth += m_originalLineConstraints[chunkStart + line];

        if (wrapStyle == TextWrapStyle::Balance) {
            const InlineLayoutUnit idealLineWidth = totalWidth / chunkSize;
            if (m_numberOfLinesInOriginalLayout <= maximumLinesToBalanceWithLineRequirement)
                return balanceRangeWithLineRequirement(rangeToConstrain, idealLineWidth, chunkSize, isFirstChunk);
            return balanceRangeWithNoLineRequirement(rangeToConstrain, idealLineWidth, isFirstChunk);
        }

        if (wrapStyle == TextWrapStyle::Pretty) {
            const InlineLayoutUnit idealLineWidth = m_maximumLineWidthConstraint  - textWrapPrettyStretchability * textWrapPrettyMaxStretch;
            return prettifyRange(rangeToConstrain, idealLineWidth, isFirstChunk);
        }

        ASSERT_NOT_REACHED();
        return { };
    };

    size_t chunkStart = 0;
    Vector<LayoutUnit> constrainedLineWidths;
    for (auto chunkSize : chunkSizes) {
        auto constrainedLineWidthsForChunk = constrainChunk(chunkStart, chunkSize);
        if (!constrainedLineWidthsForChunk) {
            for (size_t i = 0; i < chunkSize; i++)
                constrainedLineWidths.append(m_maximumLineWidthConstraint);
        } else {
            for (auto constrainedLineWidth : *constrainedLineWidthsForChunk)
                constrainedLineWidths.append(constrainedLineWidth);
        }
        chunkStart += chunkSize;
    }

    return constrainedLineWidths;
}

std::optional<Vector<LayoutUnit>> InlineContentConstrainer::balanceRangeWithLineRequirement(InlineItemRange range, InlineLayoutUnit idealLineWidth, size_t numberOfLines, bool isFirstChunk)
{
    ASSERT(range.startIndex() < range.endIndex());

    // breakOpportunities holds the indices i such that a line break can occur before m_inlineItemList[i].
    auto breakOpportunities = computeBreakOpportunities(range);

    // We need a dummy break opportunity at the beginning for algorithmic base case purposes
    breakOpportunities.insert(0, range.startIndex());
    auto numberOfBreakOpportunities = breakOpportunities.size();

    // Indentation offsets
    auto previousLineEndsWithLineBreak = isFirstChunk ? std::nullopt : std::optional<bool> { true };
    auto firstLineTextIndent = computeTextIndent(previousLineEndsWithLineBreak);
    auto textIndent = computeTextIndent(false);
    // state[i][j] holds the optimal set of line breaks where the jth line break (1-indexed) is
    // right before m_inlineItemList[breakOpportunities[i]]. "Optimal" in this context means the
    // lowest possible accumulated cost.
    Vector<Vector<EntryBalance>> state(numberOfBreakOpportunities, Vector<EntryBalance>(numberOfLines + 1));
    state[0][0].accumulatedCost = 0.f;

    // Special case the first line because of ::first-line styling, indentation, etc.
    SlidingWidth firstLineSlidingWidth { *this, m_inlineItemList, range.startIndex(), range.startIndex(), isFirstChunk, true };
    for (size_t breakIndex = 1; breakIndex < numberOfBreakOpportunities; breakIndex++) {
        auto end = breakOpportunities[breakIndex];
        firstLineSlidingWidth.advanceEndTo(end);

        auto firstLineCandidateWidth = computeLineWidthFromSlidingWidth(firstLineTextIndent, firstLineSlidingWidth);
        if (firstLineCandidateWidth > m_maximumLineWidthConstraint)
            break;

        auto cost = computeCostBalance(firstLineCandidateWidth, idealLineWidth);
        state[breakIndex][1].accumulatedCost = cost;
    }

    // breakOpportunities[firstStartIndex] is the first possible starting position for a candidate line that is NOT the first line
    size_t firstStartIndex = 1;
    SlidingWidth slidingWidth { *this, m_inlineItemList, breakOpportunities[firstStartIndex], breakOpportunities[firstStartIndex], false, false };
    for (size_t breakIndex = 1; breakIndex < numberOfBreakOpportunities; breakIndex++) {
        size_t end = breakOpportunities[breakIndex];
        slidingWidth.advanceEndTo(end);

        // We prune our search space by limiting the possible starting positions for our candidate line.
        while (computeLineWidthFromSlidingWidth(textIndent, slidingWidth) > m_maximumLineWidthConstraint) {
            firstStartIndex++;
            if (firstStartIndex > breakIndex)
                break;
            slidingWidth.advanceStartTo(breakOpportunities[firstStartIndex]);
        }

        // Evaluate all possible lines that break before m_inlineItemList[end]
        auto innerSlidingWidth = slidingWidth;
        for (size_t startIndex = firstStartIndex; startIndex < breakIndex; startIndex++) {
            size_t start = breakOpportunities[startIndex];
            ASSERT(start != range.startIndex());
            innerSlidingWidth.advanceStartTo(start);
            auto candidateLineWidth = computeLineWidthFromSlidingWidth(textIndent, innerSlidingWidth);
            auto candidateLineCost = computeCostBalance(candidateLineWidth, idealLineWidth);
            ASSERT(candidateLineWidth <= m_maximumLineWidthConstraint);

            // Compute the cost of this line based on the line index
            for (size_t lineIndex = 1; lineIndex <= numberOfLines; lineIndex++) {
                auto accumulatedCost = candidateLineCost + state[startIndex][lineIndex - 1].accumulatedCost;
                auto currentAccumulatedCost = state[breakIndex][lineIndex].accumulatedCost;
                if (accumulatedCost < currentAccumulatedCost || WTF::areEssentiallyEqual(accumulatedCost, currentAccumulatedCost)) {
                    state[breakIndex][lineIndex].accumulatedCost = accumulatedCost;
                    state[breakIndex][lineIndex].previousBreakIndex = startIndex;
                }
            }
        }
    }

    // Check if we found no solution
    if (std::isinf(state[numberOfBreakOpportunities - 1][numberOfLines].accumulatedCost))
        return { };

    // breaks[i] equals the index into m_inlineItemList before which the ith line will break
    Vector<size_t> breaks(numberOfLines);
    size_t breakIndex = numberOfBreakOpportunities - 1;
    for (size_t line = numberOfLines; line > 0; line--) {
        breaks[line - 1] = breakOpportunities[breakIndex];
        breakIndex = state[breakIndex][line].previousBreakIndex;
    }

    return computeLineWidthsFromBreaks(range, breaks, isFirstChunk);
}

std::optional<Vector<LayoutUnit>> InlineContentConstrainer::balanceRangeWithNoLineRequirement(InlineItemRange range, InlineLayoutUnit idealLineWidth, bool isFirstChunk)
{
    ASSERT(range.startIndex() < range.endIndex());

    // breakOpportunities holds the indices i such that a line break can occur before m_inlineItemList[i].
    auto breakOpportunities = computeBreakOpportunities(range);
    if (breakOpportunities.size() == 1)
        return { };

    // We need a dummy break opportunity at the beginning for algorithmic base case purposes
    breakOpportunities.insert(0, range.startIndex());
    auto numberOfBreakOpportunities = breakOpportunities.size();

    // Indentation offsets
    auto previousLineEndsWithLineBreak = isFirstChunk ? std::nullopt : std::optional<bool> { true };
    auto firstLineTextIndent = computeTextIndent(previousLineEndsWithLineBreak);
    auto textIndent = computeTextIndent(false);

    // state[i] holds the optimal set of line breaks where the last line break is right
    // before m_inlineItemList[breakOpportunities[i]]. "Optimal" in this context means the
    // lowest possible accumulated cost.
    Vector<EntryBalance> state(numberOfBreakOpportunities);
    state[0].accumulatedCost = 0.f;

    // Special case the first line because of ::first-line styling, indentation, etc.
    SlidingWidth firstLineSlidingWidth { *this, m_inlineItemList, range.startIndex(), range.startIndex(), isFirstChunk, true };
    for (size_t breakIndex = 1; breakIndex < numberOfBreakOpportunities; breakIndex++) {
        auto end = breakOpportunities[breakIndex];
        firstLineSlidingWidth.advanceEndTo(end);

        auto firstLineCandidateWidth = computeLineWidthFromSlidingWidth(firstLineTextIndent, firstLineSlidingWidth);
        if (firstLineCandidateWidth > m_maximumLineWidthConstraint)
            break;

        auto cost = computeCostBalance(firstLineCandidateWidth, idealLineWidth);
        state[breakIndex].accumulatedCost = cost;
    }

    // breakOpportunities[firstStartIndex] is the first possible starting position for a candidate line that is NOT the first line
    size_t firstStartIndex = 1;
    SlidingWidth slidingWidth { *this, m_inlineItemList, breakOpportunities[firstStartIndex], breakOpportunities[firstStartIndex], false, false };
    for (size_t breakIndex = 1; breakIndex < numberOfBreakOpportunities; breakIndex++) {
        size_t end = breakOpportunities[breakIndex];
        slidingWidth.advanceEndTo(end);

        // We prune our search space by limiting the possible starting positions for our candidate line.
        while (computeLineWidthFromSlidingWidth(textIndent, slidingWidth) > m_maximumLineWidthConstraint) {
            firstStartIndex++;
            if (firstStartIndex > breakIndex)
                break;
            slidingWidth.advanceStartTo(breakOpportunities[firstStartIndex]);
        }

        // Evaluate all possible lines that break before m_inlineItemList[end]
        auto innerSlidingWidth = slidingWidth;
        for (size_t startIndex = firstStartIndex; startIndex < breakIndex; startIndex++) {
            size_t start = breakOpportunities[startIndex];
            ASSERT(start != range.startIndex());
            innerSlidingWidth.advanceStartTo(start);
            auto candidateLineWidth = computeLineWidthFromSlidingWidth(textIndent, innerSlidingWidth);
            auto candidateLineCost = computeCostBalance(candidateLineWidth, idealLineWidth);
            ASSERT(candidateLineWidth <= m_maximumLineWidthConstraint);

            auto accumulatedCost = candidateLineCost + state[startIndex].accumulatedCost;
            if (accumulatedCost < state[breakIndex].accumulatedCost) {
                state[breakIndex].accumulatedCost = accumulatedCost;
                state[breakIndex].previousBreakIndex = startIndex;
            }
        }
    }

    // Check if we found no solution
    if (std::isinf(state[numberOfBreakOpportunities - 1].accumulatedCost))
        return { };

    // breaks[i] equals the index into m_inlineItemList before which the ith line will break
    Vector<size_t> breaks;
    size_t breakIndex = numberOfBreakOpportunities - 1;
    do {
        breaks.append(breakOpportunities[breakIndex]);
        breakIndex = state[breakIndex].previousBreakIndex;
    } while (breakIndex);
    breaks.reverse();

    return computeLineWidthsFromBreaks(range, breaks, isFirstChunk);
}

std::optional<Vector<LayoutUnit>> InlineContentConstrainer::prettifyRange(InlineItemRange range, InlineLayoutUnit idealLineWidth, bool isFirstChunk)
{
    ASSERT(range.startIndex() < range.endIndex());

    // breakOpportunities holds the indices i such that a line break can occur before m_inlineItemList[i].
    auto breakOpportunities = computeBreakOpportunities(range);
    if (breakOpportunities.size() == 1)
        return { };

    // We need a dummy break opportunity at the beginning for algorithmic base case purposes
    breakOpportunities.insert(0, range.startIndex());
    auto numberOfBreakOpportunities = breakOpportunities.size();

    // Indentation offsets
    auto previousLineEndsWithLineBreak = isFirstChunk ? std::nullopt : std::optional<bool> { true };
    auto firstLineTextIndent = computeTextIndent(previousLineEndsWithLineBreak);
    auto textIndent = computeTextIndent(false);

    // state[i] holds the optimal set of line breaks where the last line break is right
    // before m_inlineItemList[breakOpportunities[i]]. "Optimal" in this context means the
    // lowest possible accumulated cost.
    Vector<EntryPretty> state(numberOfBreakOpportunities);
    state[0].accumulatedCost = 0.f;
    std::optional<size_t> lastValidBreakpoint;

    // Special case the first line because of ::first-line styling, indentation, etc.
    SlidingWidth firstLineSlidingWidth { *this, m_inlineItemList, range.startIndex(), range.startIndex(), isFirstChunk, true };
    for (size_t breakIndex = 1; breakIndex < numberOfBreakOpportunities; breakIndex++) {
        auto end = breakOpportunities[breakIndex];
        firstLineSlidingWidth.advanceEndTo(end);

        auto firstLineCandidateWidth = computeLineWidthFromSlidingWidth(firstLineTextIndent, firstLineSlidingWidth);
        if (firstLineCandidateWidth > m_maximumLineWidthConstraint)
            break;

        auto cost = computeCostPretty(firstLineCandidateWidth, idealLineWidth, breakIndex, numberOfBreakOpportunities, idealLineWidth);
        if (cost < state[breakIndex].accumulatedCost) {
            lastValidBreakpoint = std::max(lastValidBreakpoint.value_or(0), breakIndex);
            state[breakIndex].accumulatedCost = cost;
            state[breakIndex].previousBreakIndex = range.startIndex();
            state[breakIndex].lineIndex = state[range.startIndex()].lineIndex + 1;
            state[breakIndex].lastLineWidth = firstLineCandidateWidth;
            state[breakIndex].lineEnd = { .index = breakIndex, .offset = 0 };
            auto lineInitialRect = InlineRect { 0.f, m_horizontalConstraints.logicalLeft, firstLineCandidateWidth, 0.f };
            auto lineBuilder = LineBuilder { m_inlineFormattingContext, m_horizontalConstraints, m_inlineItemList };
            auto lineLayoutResult = lineBuilder.layoutInlineContent({ { range.startIndex(), breakIndex }, lineInitialRect }, state[range.startIndex()].previousLine);
            state[breakIndex].previousLine = buildPreviousLine(state[breakIndex].lineIndex, lineLayoutResult);
        }
    }

    // If we are unable to build a valid line from without hyphenation,
    // try to build a valid line using hyphenation from the beginning.
    if (!lastValidBreakpoint.has_value()) {
        auto newEntry = layoutSingleLineForPretty({ breakOpportunities[0], range.endIndex() }, idealLineWidth, state[breakOpportunities[0]]);
        state[newEntry.lineEnd.index] = newEntry;
        lastValidBreakpoint = newEntry.lineEnd.index;
    }
    ASSERT(lastValidBreakpoint.has_value());

    // breakOpportunities[firstStartIndex] is the first possible starting position for a candidate line that is NOT the first line
    size_t firstStartIndex = 1;
    SlidingWidth slidingWidth { *this, m_inlineItemList, breakOpportunities[firstStartIndex], breakOpportunities[firstStartIndex], false, false };
    for (size_t breakIndex = 1; breakIndex < numberOfBreakOpportunities; breakIndex++) {
        size_t end = breakOpportunities[breakIndex];
        slidingWidth.advanceEndTo(end);

        // We prune our search space by limiting the possible starting positions for our candidate line.
        while (computeLineWidthFromSlidingWidth(textIndent, slidingWidth) > m_maximumLineWidthConstraint) {
            firstStartIndex++;
            if (firstStartIndex > breakIndex)
                break;
            slidingWidth.advanceStartTo(breakOpportunities[firstStartIndex]);
        }

        // If the start of our slidingWidth is past the last valid breaking point, we will not be able to find a valid solution.
        // Try to find a solution using hyphenation.
        if (firstStartIndex>lastValidBreakpoint.value()) {
            // If hyphenation does not create a valid solution, we should return early.
            auto newEntry = layoutSingleLineForPretty({ lastValidBreakpoint.value(), range.endIndex() }, idealLineWidth, state[lastValidBreakpoint.value()]);
            state[newEntry.lineEnd.index] = newEntry;
            lastValidBreakpoint = newEntry.lineEnd.index;
        }

        // Evaluate all possible lines that break before m_inlineItemList[end]
        auto innerSlidingWidth = slidingWidth;
        for (size_t startIndex = firstStartIndex; startIndex < breakIndex; startIndex++) {
            size_t start = breakOpportunities[startIndex];
            ASSERT(start != range.startIndex());
            innerSlidingWidth.advanceStartTo(start);
            auto candidateLineWidth = computeLineWidthFromSlidingWidth(textIndent, innerSlidingWidth);
            // TODO: update candidateLineWidth here using state[startIndex].lineEnd.offset.
            auto candidateLineCost = computeCostPretty(candidateLineWidth, idealLineWidth, breakIndex, numberOfBreakOpportunities, state[startIndex].lastLineWidth);
            auto accumulatedCost = candidateLineCost + state[startIndex].accumulatedCost;

            if (accumulatedCost < state[breakIndex].accumulatedCost) {
                state[breakIndex].accumulatedCost = accumulatedCost;
                state[breakIndex].previousBreakIndex = startIndex;
                state[breakIndex].lastLineWidth = candidateLineWidth;
                state[breakIndex].lineEnd = { .index = breakIndex, .offset = 0 };
                state[breakIndex].lineIndex = state[startIndex].lineIndex + 1;
                auto lineInitialRect = InlineRect { 0.f, m_horizontalConstraints.logicalLeft, candidateLineWidth, 0.f };
                auto lineBuilder = LineBuilder { m_inlineFormattingContext, m_horizontalConstraints, m_inlineItemList };
                auto lineLayoutResult = lineBuilder.layoutInlineContent({ { startIndex, breakIndex }, lineInitialRect }, state[startIndex].previousLine);
                state[breakIndex].previousLine = buildPreviousLine(state[breakIndex].lineIndex, lineLayoutResult);
                lastValidBreakpoint = std::max(lastValidBreakpoint.value(), breakIndex);
            }
        }
    }

    // Check if we found no solution
    if (std::isinf(state[numberOfBreakOpportunities - 1].accumulatedCost))
        return { };
    // breaks[i] equals the index into m_inlineItemList before which the ith line will break
    Vector<LayoutUnit> widths;
    size_t breakIndex = numberOfBreakOpportunities - 1;
    do {
        widths.append(state[breakIndex].lastLineWidth);
        breakIndex = state[breakIndex].previousBreakIndex;
    } while (breakIndex);
    widths.reverse();
    return widths;
}

InlineLayoutUnit InlineContentConstrainer::inlineItemWidth(size_t inlineItemIndex, bool useFirstLineStyle) const
{
    if (m_hasValidInlineItemWidthCache)
        return useFirstLineStyle ? m_firstLineStyleInlineItemWidths[inlineItemIndex] : m_inlineItemWidths[inlineItemIndex];
    // If inline items width cache has not yet been initialized, we should explicitly calculate the item's width.
    return m_inlineFormattingContext.formattingUtils().inlineItemWidth(m_inlineItemList[inlineItemIndex], 0, useFirstLineStyle);
}

bool InlineContentConstrainer::shouldTrimLeading(size_t inlineItemIndex, bool useFirstLineStyle, bool isFirstLineInChunk) const
{
    auto& inlineItem = m_inlineItemList[inlineItemIndex];
    auto& style = useFirstLineStyle ? inlineItem.firstLineStyle() : inlineItem.style();

    // Handle line break first so we can focus on other types of white space
    if (inlineItem.isLineBreak())
        return true;

    if (auto* textItem = dynamicDowncast<InlineTextItem>(inlineItem)) {
        if (textItem->isWhitespace()) {
            bool isFirstLineLeadingPreservedWhiteSpace = style.whiteSpaceCollapse() == WhiteSpaceCollapse::Preserve && isFirstLineInChunk;
            return !isFirstLineLeadingPreservedWhiteSpace && style.whiteSpaceCollapse() != WhiteSpaceCollapse::BreakSpaces;
        }
        return false;
    }

    if (inlineItemWidth(inlineItemIndex, useFirstLineStyle) <= 0)
        return true;

    return false;
}

bool InlineContentConstrainer::shouldTrimTrailing(size_t inlineItemIndex, bool useFirstLineStyle) const
{
    auto& inlineItem = m_inlineItemList[inlineItemIndex];
    auto& style = useFirstLineStyle ? inlineItem.firstLineStyle() : inlineItem.style();

    // Handle line break first so we can focus on other types of white space
    if (inlineItem.isLineBreak())
        return true;

    if (auto* textItem = dynamicDowncast<InlineTextItem>(inlineItem)) {
        if (textItem->isWhitespace())
            return style.whiteSpaceCollapse() != WhiteSpaceCollapse::BreakSpaces;
        return false;
    }

    if (inlineItemWidth(inlineItemIndex, useFirstLineStyle) <= 0)
        return true;

    return false;
}

SlidingWidth::SlidingWidth(const InlineContentConstrainer& inlineContentConstrainer, [[maybe_unused]] const InlineItemList& inlineItemList, size_t start, size_t end, bool useFirstLineStyle, bool isFirstLineInChunk)
    : m_inlineContentConstrainer(inlineContentConstrainer)
#if ASSERT_ENABLED
    , m_inlineItemList(inlineItemList)
#endif
    , m_start(start)
    , m_end(start)
    , m_useFirstLineStyle(useFirstLineStyle)
    , m_isFirstLineInChunk(isFirstLineInChunk)
{
    ASSERT(start <= end);
    advanceEndTo(end);
}

InlineLayoutUnit SlidingWidth::width()
{
    return m_totalWidth - m_leadingTrimmableWidth - m_trailingTrimmableWidth;
}

void SlidingWidth::advanceStart()
{
    ASSERT(m_start < m_end);
    auto startItemIndex = m_start;
    auto startItemWidth = m_inlineContentConstrainer.inlineItemWidth(startItemIndex, m_useFirstLineStyle);
    m_totalWidth -= startItemWidth;
    m_start++;

    if (m_inlineContentConstrainer.shouldTrimLeading(startItemIndex, m_useFirstLineStyle, m_isFirstLineInChunk)) {
        m_leadingTrimmableWidth -= startItemWidth;
        return;
    }

    m_firstLeadingNonTrimmedItem = std::nullopt;
    m_leadingTrimmableWidth = 0;
    for (auto current = m_start; current < m_end; current++) {
        if (!m_inlineContentConstrainer.shouldTrimLeading(current, m_useFirstLineStyle, m_isFirstLineInChunk)) {
            m_firstLeadingNonTrimmedItem = current;
            break;
        }
        m_leadingTrimmableWidth += m_inlineContentConstrainer.inlineItemWidth(current, m_useFirstLineStyle);
    }

    // Update trailing logic if necessary:
    //   1: Check if the removed start item was the first trailing item
    //   2: Check if the first non trimmed leading item surpassed the first trailing item
    // In both cases, we should have m_leadingTrimmableWidth + m_trailingTrimmableWidth = m_totalWidth
    if (m_leadingTrimmableWidth + m_trailingTrimmableWidth > m_totalWidth)
        m_trailingTrimmableWidth = m_totalWidth - m_leadingTrimmableWidth;
}

void SlidingWidth::advanceStartTo(size_t newStart)
{
    ASSERT(m_start <= newStart);
    while (m_start < newStart)
        advanceStart();
}

void SlidingWidth::advanceEnd()
{
    ASSERT(m_end < m_inlineItemList.size());
    auto endItemIndex = m_end;
    auto endItemWidth = m_inlineContentConstrainer.inlineItemWidth(endItemIndex, m_useFirstLineStyle);
    m_totalWidth += endItemWidth;
    m_end++;

    if (!m_firstLeadingNonTrimmedItem.has_value()) {
        if (m_inlineContentConstrainer.shouldTrimLeading(endItemIndex, m_useFirstLineStyle, m_isFirstLineInChunk)) {
            m_leadingTrimmableWidth += endItemWidth;
            return;
        }
        m_firstLeadingNonTrimmedItem = endItemIndex;
        return;
    }

    if (m_inlineContentConstrainer.shouldTrimTrailing(m_end - 1, m_useFirstLineStyle)) {
        m_trailingTrimmableWidth += endItemWidth;
        return;
    }

    m_trailingTrimmableWidth = 0;
}

void SlidingWidth::advanceEndTo(size_t newEnd)
{
    ASSERT(m_end <= newEnd);
    while (m_end < newEnd)
        advanceEnd();
}

Vector<size_t> InlineContentConstrainer::computeBreakOpportunities(InlineItemRange range) const
{
    Vector<size_t> breakOpportunities;
    size_t currentIndex = range.startIndex();
    while (currentIndex < range.endIndex()) {
        currentIndex = m_inlineFormattingContext.formattingUtils().nextWrapOpportunity(currentIndex, range, m_inlineItemList.span());
        // FIXME: we should not consider the range end as breaking opportunity.
        breakOpportunities.append(currentIndex);
    }
    return breakOpportunities;
}

Vector<LayoutUnit> InlineContentConstrainer::computeLineWidthsFromBreaks(InlineItemRange inlineItems, const Vector<size_t>& breaks, bool isFirstChunk) const
{
    Vector<LayoutUnit> lineWidths(breaks.size());
    const auto firstLineTextIndent = computeTextIndent(isFirstChunk ? std::nullopt : std::make_optional(true));
    const auto textIndent = computeTextIndent(false);
    for (size_t i = 0; i < breaks.size(); i++) {
        auto start = !i ? inlineItems.startIndex() : breaks[i - 1];
        auto end = breaks[i];
        auto indentWidth = !i ? firstLineTextIndent : textIndent;
        SlidingWidth slidingWidth { *this, m_inlineItemList, start, end, !i && isFirstChunk, !i };
        lineWidths[i] = computeLineWidthFromSlidingWidth(indentWidth, slidingWidth);
    }
    return lineWidths;
}

InlineLayoutUnit InlineContentConstrainer::computeTextIndent(std::optional<bool> previousLineEndsWithLineBreak) const
{
    return m_inlineFormattingContext.formattingUtils().computedTextIndent(InlineFormattingUtils::IsIntrinsicWidthMode::No, previousLineEndsWithLineBreak, m_maximumLineWidthConstraint);
}

}
}
