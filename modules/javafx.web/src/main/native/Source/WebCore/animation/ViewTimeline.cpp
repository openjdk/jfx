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
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "config.h"
#include "ViewTimeline.h"

#include "AnimationTimelinesController.h"
#include "CSSNumericFactory.h"
#include "CSSPropertyParserConsumer+Timeline.h"
#include "Document.h"
#include "Element.h"
#include "LegacyRenderSVGModelObject.h"
#include "RenderBlock.h"
#include "RenderBox.h"
#include "RenderBoxInlines.h"
#include "RenderInline.h"
#include "RenderLayerScrollableArea.h"
#include "RenderSVGModelObject.h"
#include "ScrollAnchoringController.h"
#include "ScrollingConstraints.h"
#include "StyleBuilderConverter.h"
#include "StyleScrollPadding.h"
#include "WebAnimation.h"

namespace WebCore {

static bool isValidInset(RefPtr<CSSPrimitiveValue>& inset)
{
    return !inset || inset->valueID() == CSSValueAuto || inset->isLength() || inset->isPercentage();
}

ExceptionOr<Ref<ViewTimeline>> ViewTimeline::create(Document& document, ViewTimelineOptions&& options)
{
    auto viewTimeline = adoptRef(*new ViewTimeline(options.axis));

    auto specifiedInsetsOrException = viewTimeline->validateSpecifiedInsets(options.inset, document);
    if (specifiedInsetsOrException.hasException())
        return specifiedInsetsOrException.releaseException();

    auto specifiedInsets = specifiedInsetsOrException.releaseReturnValue();
    if (!isValidInset(specifiedInsets.start) || !isValidInset(specifiedInsets.end))
        return Exception { ExceptionCode::TypeError };

    viewTimeline->m_specifiedInsets = WTFMove(specifiedInsets);
    viewTimeline->setSubject(options.subject.get());
    if (auto subject = options.subject)
        subject->protectedDocument()->updateLayoutIgnorePendingStylesheets();
    viewTimeline->cacheCurrentTime();

    return viewTimeline;
}

Ref<ViewTimeline> ViewTimeline::create(const AtomString& name, ScrollAxis axis, ViewTimelineInsets&& insets)
{
    return adoptRef(*new ViewTimeline(name, axis, WTFMove(insets)));
}

ViewTimeline::ViewTimeline(ScrollAxis axis)
    : ScrollTimeline(nullAtom(), axis)
{
}

ViewTimeline::ViewTimeline(const AtomString& name, ScrollAxis axis, ViewTimelineInsets&& insets)
    : ScrollTimeline(name, axis)
    , m_insets(WTFMove(insets))
{
}

ExceptionOr<ViewTimeline::SpecifiedViewTimelineInsets> ViewTimeline::validateSpecifiedInsets(const ViewTimelineInsetValue inset, const Document& document)
{
    // https://drafts.csswg.org/scroll-animations-1/#dom-viewtimeline-viewtimeline

    // FIXME: note that we use CSSKeywordish instead of CSSKeywordValue to match Chrome,
    // issue being tracked at https://github.com/w3c/csswg-drafts/issues/11477.

    // If a DOMString value is provided as an inset, parse it as a <'view-timeline-inset'> value;
    if (auto* insetString = std::get_if<String>(&inset)) {
        if (insetString->isEmpty())
            return Exception { ExceptionCode::TypeError };
        CSSTokenizer tokenizer(*insetString);
        auto tokenRange = tokenizer.tokenRange();
        tokenRange.consumeWhitespace();
        auto consumedInset = CSSPropertyParserHelpers::consumeViewTimelineInsetListItem(tokenRange, Ref { document }->cssParserContext());
        if (!consumedInset)
            return Exception { ExceptionCode::TypeError };

        if (auto insetPair = dynamicDowncast<CSSValuePair>(consumedInset)) {
            return { {
                dynamicDowncast<CSSPrimitiveValue>(insetPair->protectedFirst()),
                dynamicDowncast<CSSPrimitiveValue>(insetPair->protectedSecond())
            } };
        } else
            return { { dynamicDowncast<CSSPrimitiveValue>(consumedInset), nullptr } };
    }

    auto cssPrimitiveValueForCSSNumericValue = [&](RefPtr<CSSNumericValue> numericValue) -> ExceptionOr<RefPtr<CSSPrimitiveValue>> {
        if (RefPtr insetValue = dynamicDowncast<CSSUnitValue>(*numericValue))
            return dynamicDowncast<CSSPrimitiveValue>(insetValue->toCSSValue());
        return nullptr;
    };

    auto cssPrimitiveValueForCSSKeywordValue = [&](RefPtr<CSSKeywordValue> keywordValue) -> ExceptionOr<RefPtr<CSSPrimitiveValue>> {
        if (keywordValue->value() != "auto"_s)
            return Exception { ExceptionCode::TypeError };
        return nullptr;
    };

    auto cssPrimitiveValueForIndividualInset = [&](ViewTimelineIndividualInset individualInset) -> ExceptionOr<RefPtr<CSSPrimitiveValue>> {
        if (auto* numericInset = std::get_if<RefPtr<CSSNumericValue>>(&individualInset))
            return cssPrimitiveValueForCSSNumericValue(*numericInset);
        if (auto* stringInset = std::get_if<String>(&individualInset))
            return cssPrimitiveValueForCSSKeywordValue(CSSKeywordValue::rectifyKeywordish(*stringInset));
        ASSERT(std::holds_alternative<RefPtr<CSSKeywordValue>>(individualInset));
        return cssPrimitiveValueForCSSKeywordValue(CSSKeywordValue::rectifyKeywordish(std::get<RefPtr<CSSKeywordValue>>(individualInset)));
    };

    // if a sequence is provided, the first value represents the start inset and the second value represents the end inset.
    // If the sequence has only one value, it is duplicated. If it has zero values or more than two values, or if it contains
    // a CSSKeywordValue whose value is not "auto", throw a TypeError.
    auto insetList = std::get<Vector<ViewTimelineIndividualInset>>(inset);
    auto numberOfInsets = insetList.size();

    if (!numberOfInsets || numberOfInsets > 2)
        return Exception { ExceptionCode::TypeError };

    auto startInsetOrException = cssPrimitiveValueForIndividualInset(insetList.at(0));
    if (startInsetOrException.hasException())
        return startInsetOrException.releaseException();
    auto startInset = startInsetOrException.releaseReturnValue();

    if (numberOfInsets == 1)
        return { { startInset, startInset } };

    auto endInsetOrException = cssPrimitiveValueForIndividualInset(insetList.at(1));
    if (endInsetOrException.hasException())
        return endInsetOrException.releaseException();
    auto endInset = endInsetOrException.releaseReturnValue();
    return { { startInset, endInset } };
}

const Element* ViewTimeline::subject() const
{
    if (auto subject = m_subject.styleable())
        return &subject->element;
    return nullptr;
}

void ViewTimeline::setSubject(Element* subject)
{
    if (subject)
        setSubject(Styleable::fromElement(*subject));
    else {
        removeTimelineFromDocument(m_subject.element().get());
        m_subject = WeakStyleable();
    }
}

void ViewTimeline::setSubject(const Styleable& styleable)
{
    if (m_subject == styleable)
        return;

    auto previousSubject = m_subject.element();
    m_subject = styleable;

    if (previousSubject && &previousSubject->document() == &styleable.element.document())
        return;

    removeTimelineFromDocument(previousSubject.get());

    styleable.element.protectedDocument()->ensureTimelinesController().addTimeline(*this);
}

AnimationTimelinesController* ViewTimeline::controller() const
{
    if (auto subject = m_subject.styleable())
        return &subject->element.document().ensureTimelinesController();
    return nullptr;
}

StickinessAdjustmentData StickinessAdjustmentData::computeStickinessAdjustmentData(const StickyPositionViewportConstraints& constraints, ScrollTimeline::ResolvedScrollDirection scrollDirection, float scrollContainerSize, float subjectSize, float subjectOffset)
{
    // For a sticky container, determine the amount of adjustment that is possible, which is the distance from the edge of the sticky container
    // to the edge of its containing block. We also need to determine where the subject element is relative to the scroller when the stickiness
    // occurs, so that we can properly adjust the start and end of the range, as well as for a specific animation-range.

    StickinessAdjustmentData data;

    auto computeSubjectStickinessLocation = [] (float stickyBoxStuckPosition, float stickyBoxStaticPosition, float scrollContainerSize, float subjectSize, float subjectOffset) {
        float subjectPositionInScroller = stickyBoxStuckPosition + subjectOffset - stickyBoxStaticPosition;
        if (subjectPositionInScroller > scrollContainerSize)
            return StickinessLocation::BeforeEntry;
        if (subjectPositionInScroller + subjectSize > scrollContainerSize)
            return StickinessLocation::DuringEntry;
        if (subjectPositionInScroller + subjectSize < 0)
            return StickinessLocation::AfterExit;
        if (subjectPositionInScroller < 0)
            return StickinessLocation::DuringExit;
        return StickinessLocation::WhileContained;
    };

    if (scrollDirection.isVertical) {
        if (constraints.hasAnchorEdge(ViewportConstraints::AnchorEdgeTop)) {
            data.stickyTopOrLeftAdjustment = constraints.containingBlockRect().maxY() - constraints.stickyBoxRect().maxY();
            data.topOrLeftAdjustmentLocation = computeSubjectStickinessLocation(constraints.topOffset(), constraints.stickyBoxRect().y(), scrollContainerSize, subjectSize, subjectOffset);
        }
        if (constraints.hasAnchorEdge(ViewportConstraints::AnchorEdgeBottom)) {
            data.stickyBottomOrRightAdjustment = constraints.containingBlockRect().y() - constraints.stickyBoxRect().y();
            data.bottomOrRightAdjustmentLocation = computeSubjectStickinessLocation(scrollContainerSize - constraints.bottomOffset() - constraints.stickyBoxRect().height(), constraints.stickyBoxRect().y(), scrollContainerSize, subjectSize, subjectOffset);
        }
    } else {
        if (constraints.hasAnchorEdge(ViewportConstraints::AnchorEdgeLeft)) {
            data.stickyTopOrLeftAdjustment = constraints.containingBlockRect().maxX() - constraints.stickyBoxRect().maxX();
            data.topOrLeftAdjustmentLocation = computeSubjectStickinessLocation(constraints.leftOffset(), constraints.stickyBoxRect().x(), scrollContainerSize, subjectSize, subjectOffset);
        }
        if (constraints.hasAnchorEdge(ViewportConstraints::AnchorEdgeRight)) {
            data.stickyBottomOrRightAdjustment = constraints.containingBlockRect().x() - constraints.stickyBoxRect().x();
            data.bottomOrRightAdjustmentLocation = computeSubjectStickinessLocation(scrollContainerSize - constraints.rightOffset() - constraints.stickyBoxRect().width(), constraints.stickyBoxRect().x(), scrollContainerSize, subjectSize, subjectOffset);
        }
    }
    return data;
}

float StickinessAdjustmentData::entryDistanceAdjustment() const
{
    float entryDistanceAdjustment = 0;
    if (topOrLeftAdjustmentLocation == StickinessLocation::DuringEntry)
        entryDistanceAdjustment += stickyTopOrLeftAdjustment;
    if (bottomOrRightAdjustmentLocation == StickinessLocation::DuringEntry)
        entryDistanceAdjustment -= stickyBottomOrRightAdjustment;
    return entryDistanceAdjustment;
}

float StickinessAdjustmentData::exitDistanceAdjustment() const
{
    float exitDistanceAdjustment = 0;
    if (topOrLeftAdjustmentLocation == StickinessLocation::DuringExit)
        exitDistanceAdjustment += stickyTopOrLeftAdjustment;
    if (bottomOrRightAdjustmentLocation == StickinessLocation::DuringExit)
        exitDistanceAdjustment -= stickyBottomOrRightAdjustment;
    return exitDistanceAdjustment;
}

float StickinessAdjustmentData::rangeStartAdjustment() const
{
    auto rangeStartAdjustment = 0;
    if (topOrLeftAdjustmentLocation == StickinessLocation::BeforeEntry)
        rangeStartAdjustment += stickyTopOrLeftAdjustment;
    if (bottomOrRightAdjustmentLocation != StickinessLocation::BeforeEntry)
        rangeStartAdjustment += stickyBottomOrRightAdjustment;
    return rangeStartAdjustment;
}

float StickinessAdjustmentData::rangeEndAdjustment() const
{
    auto rangeEndAdjustment = 0;
    if (topOrLeftAdjustmentLocation != StickinessLocation::AfterExit)
        rangeEndAdjustment += stickyTopOrLeftAdjustment;
    if (bottomOrRightAdjustmentLocation == StickinessLocation::AfterExit)
        rangeEndAdjustment += stickyBottomOrRightAdjustment;
    return rangeEndAdjustment;
}

void ViewTimeline::cacheCurrentTime()
{
    auto previousCurrentTimeData = m_cachedCurrentTimeData;

    auto pointForLocalToContainer = [](const ScrollableArea& area) -> FloatPoint {
        // For subscrollers we need to ajust the point fed into localToContainerPoint as
        // the returned value can be outside of the scroller.
        if (is<RenderLayerScrollableArea>(area))
            return area.scrollOffset();
        return { };
    };

    m_cachedCurrentTimeData = [&] -> CurrentTimeData {
        auto subject = m_subject.styleable();
        if (!subject)
            return { };

        CheckedPtr subjectRenderer = subject->renderer();
        if (!subjectRenderer)
            return { };

        CheckedPtr sourceRenderer = sourceScrollerRenderer();
        auto* sourceScrollableArea = scrollableAreaForSourceRenderer(sourceRenderer.get(), subject->element.document());
        if (!sourceScrollableArea)
            return { };

        auto scrollDirection = resolvedScrollDirection();
        if (!scrollDirection)
            return { };

        float scrollOffset = scrollDirection->isVertical ? sourceScrollableArea->scrollOffset().y() : sourceScrollableArea->scrollOffset().x();
        float scrollContainerSize = scrollDirection->isVertical ? sourceScrollableArea->visibleHeight() : sourceScrollableArea->visibleWidth();

        // https://drafts.csswg.org/scroll-animations-1/#view-timelines-ranges
        // Transforms and sticky position offsets are ignored, but relative and absolute positioning are accounted for.
        OptionSet<MapCoordinatesMode> options { IgnoreStickyOffsets };
        auto subjectOffsetFromSource = subjectRenderer->localToContainerPoint(pointForLocalToContainer(*sourceScrollableArea), sourceRenderer.get(), options);
        float subjectOffset = scrollDirection->isVertical ? subjectOffsetFromSource.y() : subjectOffsetFromSource.x();

        // Ensure borders are subtracted.
        auto scrollerPaddingBoxOrigin = sourceRenderer->paddingBoxRect().location();
        subjectOffset -= scrollDirection->isVertical ? scrollerPaddingBoxOrigin.y() : scrollerPaddingBoxOrigin.x();

        auto subjectBounds = [&] -> FloatSize {
            if (CheckedPtr subjectRenderBox = dynamicDowncast<RenderBox>(subjectRenderer.get()))
                return subjectRenderBox->contentBoxRect().size();
            if (CheckedPtr subjectRenderInline = dynamicDowncast<RenderInline>(subjectRenderer.get()))
                return subjectRenderInline->borderBoundingBox().size();
            if (CheckedPtr subjectRenderSVGModelObject = dynamicDowncast<RenderSVGModelObject>(subjectRenderer.get()))
                return subjectRenderSVGModelObject->borderBoxRectEquivalent().size();
            if (is<LegacyRenderSVGModelObject>(subjectRenderer.get()))
                return subjectRenderer->objectBoundingBox().size();
            return { };
        }();

        auto subjectSize = scrollDirection->isVertical ? subjectBounds.height() : subjectBounds.width();

        if (m_specifiedInsets) {
            RefPtr subjectElement { &subject->element };
            auto computedInset = [&](const RefPtr<CSSPrimitiveValue>& specifiedInset) -> std::optional<Length> {
                if (specifiedInset)
                    return SingleTimelineRange::lengthForCSSValue(specifiedInset, subjectElement);
                return { };
            };
            m_insets = { computedInset(m_specifiedInsets->start), computedInset(m_specifiedInsets->end) };
        }

        enum class PaddingEdge : bool { Start, End };
        auto scrollPadding = [&](PaddingEdge edge) {
            auto& style = sourceRenderer->style();
            if (edge == PaddingEdge::Start)
                return scrollDirection->isVertical ? style.scrollPaddingTop() : style.scrollPaddingLeft();
            return scrollDirection->isVertical ? style.scrollPaddingBottom() : style.scrollPaddingRight();
    };

        bool hasInsetsStart = m_insets.start.has_value();
        bool hasInsetsEnd = m_insets.end.has_value();

        float insetStart = 0;
        float insetEnd = 0;
        if (hasInsetsStart && hasInsetsEnd) {
            if (m_insets.start->isAuto())
                insetStart = Style::evaluate(scrollPadding(PaddingEdge::Start), scrollContainerSize);
            else
                insetStart = floatValueForOffset(*m_insets.start, scrollContainerSize);

            if (m_insets.end->isAuto())
                insetEnd = Style::evaluate(scrollPadding(PaddingEdge::End), scrollContainerSize);
            else
                insetEnd = floatValueForOffset(*m_insets.end, scrollContainerSize);
        } else if (hasInsetsStart) {
            if (m_insets.start->isAuto()) {
                insetStart = Style::evaluate(scrollPadding(PaddingEdge::Start), scrollContainerSize);
                insetEnd = Style::evaluate(scrollPadding(PaddingEdge::End), scrollContainerSize);
            } else {
                insetStart = floatValueForOffset(*m_insets.start, scrollContainerSize);
                insetEnd = insetStart;
            }
        } else if (hasInsetsEnd) {
            insetStart = Style::evaluate(scrollPadding(PaddingEdge::Start), scrollContainerSize);\

            if (m_insets.end->isAuto())
                insetEnd = Style::evaluate(scrollPadding(PaddingEdge::End), scrollContainerSize);
            else
                insetEnd = floatValueForOffset(*m_insets.end, scrollContainerSize);
        } else {
            insetStart = Style::evaluate(scrollPadding(PaddingEdge::Start), scrollContainerSize);
            insetEnd = Style::evaluate(scrollPadding(PaddingEdge::End), scrollContainerSize);
        }

        StickinessAdjustmentData stickyData;
        if (auto stickyContainer = dynamicDowncast<RenderBoxModelObject>(this->stickyContainer())) {
            FloatRect constrainingRect = stickyContainer->constrainingRectForStickyPosition();
            StickyPositionViewportConstraints constraints;
            stickyContainer->computeStickyPositionConstraints(constraints, constrainingRect);
            stickyData = StickinessAdjustmentData::computeStickinessAdjustmentData(constraints, *scrollDirection, scrollContainerSize, subjectSize, subjectOffset);
        }

        return {
            scrollOffset,
            scrollContainerSize,
            subjectOffset,
            subjectSize,
            insetStart,
            insetEnd,
            stickyData
        };
    }();

    auto metricsChanged = previousCurrentTimeData.scrollContainerSize != m_cachedCurrentTimeData.scrollContainerSize
        || previousCurrentTimeData.subjectOffset != m_cachedCurrentTimeData.subjectOffset
        || previousCurrentTimeData.subjectSize != m_cachedCurrentTimeData.subjectSize
        || previousCurrentTimeData.insetStart != m_cachedCurrentTimeData.insetStart
        || previousCurrentTimeData.insetEnd != m_cachedCurrentTimeData.insetEnd
        || previousCurrentTimeData.stickinessData != m_cachedCurrentTimeData.stickinessData;

    if (metricsChanged) {
        for (auto& animation : m_animations)
            animation->progressBasedTimelineSourceDidChangeMetrics();
    }
}

AnimationTimeline::ShouldUpdateAnimationsAndSendEvents ViewTimeline::documentWillUpdateAnimationsAndSendEvents()
{
    cacheCurrentTime();
    if (m_subject.element() && m_subject.element()->isConnected())
        return AnimationTimeline::ShouldUpdateAnimationsAndSendEvents::Yes;
    return AnimationTimeline::ShouldUpdateAnimationsAndSendEvents::No;
}

TimelineRange ViewTimeline::defaultRange() const
{
    return TimelineRange::defaultForViewTimeline();
}

Element* ViewTimeline::source() const
{
    if (CheckedPtr sourceRender = sourceScrollerRenderer())
        return sourceRender->element();
    return nullptr;
}

const RenderBox* ViewTimeline::sourceScrollerRenderer() const
{
    auto subject = m_subject.styleable();
    if (!subject)
        return nullptr;

    CheckedPtr subjectRenderer = subject->renderer();
    if (!subjectRenderer)
        return { };

    // https://drafts.csswg.org/scroll-animations-1/#dom-scrolltimeline-source
    // Determine source renderer by looking for the nearest ancestor that establishes a scroll container
    return subjectRenderer->enclosingScrollableContainer();
}

const RenderElement* ViewTimeline::stickyContainer() const
{
    auto subject = m_subject.styleable();
    if (!subject)
        return nullptr;

    CheckedPtr renderer = subject->renderer();

    auto scrollerRenderer = sourceScrollerRenderer();
    while (renderer && renderer != scrollerRenderer) {
        if (renderer->isStickilyPositioned())
            return renderer.get();
        renderer = renderer->containingBlock();
    }
            return nullptr;
}

ScrollTimeline::Data ViewTimeline::computeTimelineData() const
{
    if (!m_cachedCurrentTimeData.scrollOffset && !m_cachedCurrentTimeData.scrollContainerSize)
        return { };

    auto rangeStart = m_cachedCurrentTimeData.subjectOffset - m_cachedCurrentTimeData.scrollContainerSize;
    auto range = m_cachedCurrentTimeData.subjectSize + m_cachedCurrentTimeData.scrollContainerSize;
    auto rangeEnd = rangeStart + range;

    return {
        m_cachedCurrentTimeData.scrollOffset,
        rangeStart + m_cachedCurrentTimeData.insetEnd + m_cachedCurrentTimeData.stickinessData.rangeStartAdjustment(),
        rangeEnd - m_cachedCurrentTimeData.insetStart + m_cachedCurrentTimeData.stickinessData.rangeEndAdjustment()
    };
}

std::pair<double, double> ViewTimeline::intervalForTimelineRangeName(const ScrollTimeline::Data& data, const SingleTimelineRange::Name name) const
{
    auto subjectRangeStart = [&]() {
        switch (name) {
        case SingleTimelineRange::Name::Normal:
        case SingleTimelineRange::Name::Omitted:
        case SingleTimelineRange::Name::Cover:
        case SingleTimelineRange::Name::Entry:
        case SingleTimelineRange::Name::EntryCrossing:
            return data.rangeStart;
        case SingleTimelineRange::Name::Contain:
            return data.rangeStart + m_cachedCurrentTimeData.subjectSize + m_cachedCurrentTimeData.stickinessData.entryDistanceAdjustment();
        case SingleTimelineRange::Name::Exit:
        case SingleTimelineRange::Name::ExitCrossing:
            return data.rangeEnd - m_cachedCurrentTimeData.subjectSize - m_cachedCurrentTimeData.stickinessData.exitDistanceAdjustment();
        default:
            break;
        }
        ASSERT_NOT_REACHED();
        return 0.f;
    }();

    auto subjectRangeEnd = [&]() {
        switch (name) {
        case SingleTimelineRange::Name::Normal:
        case SingleTimelineRange::Name::Omitted:
        case SingleTimelineRange::Name::Cover:
        case SingleTimelineRange::Name::Exit:
        case SingleTimelineRange::Name::ExitCrossing:
            return data.rangeEnd;
        case SingleTimelineRange::Name::Contain:
            return data.rangeEnd - m_cachedCurrentTimeData.subjectSize - m_cachedCurrentTimeData.stickinessData.exitDistanceAdjustment();
        case SingleTimelineRange::Name::Entry:
        case SingleTimelineRange::Name::EntryCrossing:
            return data.rangeStart + m_cachedCurrentTimeData.subjectSize + m_cachedCurrentTimeData.stickinessData.entryDistanceAdjustment();
        default:
            break;
        }
        ASSERT_NOT_REACHED();
        return 0.f;
    }();

    if (subjectRangeEnd < subjectRangeStart)
        std::swap(subjectRangeStart, subjectRangeEnd);

    return { subjectRangeStart, subjectRangeEnd };
}

template<typename F> double ViewTimeline::mapOffsetToTimelineRange(const ScrollTimeline::Data& data, const SingleTimelineRange::Name name, F&& valueWithinSubjectRange) const
{
    auto timelineRange = data.rangeEnd - data.rangeStart;
    ASSERT(timelineRange);
    auto [subjectRangeStart, subjectRangeEnd] = intervalForTimelineRangeName(data, name);
    auto subjectRange = subjectRangeEnd - subjectRangeStart;
    auto positionWithinContainer = subjectRangeStart + valueWithinSubjectRange(subjectRange);
    auto positionWithinTimelineRange = positionWithinContainer - data.rangeStart;
    return positionWithinTimelineRange / timelineRange;
}

std::pair<double, double> ViewTimeline::offsetIntervalForTimelineRangeName(const SingleTimelineRange::Name name) const
{
    auto data = computeTimelineData();
    auto computeOffset = [&](double offset) {
        return mapOffsetToTimelineRange(data, name, [&](const float& subjectRange) {
            return offset * subjectRange;
        });
    };
    return { computeOffset(0), computeOffset(1) };
}

std::pair<double, double> ViewTimeline::offsetIntervalForAttachmentRange(const TimelineRange& attachmentRange) const
{
    auto data = computeTimelineData();
    auto timelineRange = data.rangeEnd - data.rangeStart;
    ASSERT(timelineRange);

    auto offsetForSingleTimelineRange = [&](const SingleTimelineRange& rangeToConvert) {
        auto [conversionRangeStart, conversionRangeEnd] = intervalForTimelineRangeName(data, rangeToConvert.name);
        auto conversionRange = conversionRangeEnd - conversionRangeStart;
        auto convertedValue = floatValueForOffset(rangeToConvert.offset, conversionRange);
        auto position = conversionRangeStart + convertedValue;
        return (position - data.rangeStart) / timelineRange;
    };

    return { offsetForSingleTimelineRange(attachmentRange.start), offsetForSingleTimelineRange(attachmentRange.end) };
}

std::pair<WebAnimationTime, WebAnimationTime> ViewTimeline::intervalForAttachmentRange(const TimelineRange& attachmentRange) const
{
    // https://drafts.csswg.org/scroll-animations-1/#view-timelines-ranges
    auto data = computeTimelineData();
    auto timelineRange = data.rangeEnd - data.rangeStart;
    if (!timelineRange)
        return { WebAnimationTime::fromPercentage(0), WebAnimationTime::fromPercentage(100) };

    auto computeTime = [&](const SingleTimelineRange& rangeToConvert) {
        auto mappedOffset = mapOffsetToTimelineRange(data, rangeToConvert.name, [&](const float& subjectRange) {
            return floatValueForOffset(rangeToConvert.offset, subjectRange);
        });
        return WebAnimationTime::fromPercentage(mappedOffset * 100);
    };

    auto attachmentRangeOrDefault = attachmentRange.isDefault() ? defaultRange() : attachmentRange;
    return {
        computeTime(attachmentRangeOrDefault.start),
        computeTime(attachmentRangeOrDefault.end),
    };
}

Ref<CSSNumericValue> ViewTimeline::startOffset() const
{
    return CSSNumericFactory::px(computeTimelineData().rangeStart);
}

Ref<CSSNumericValue> ViewTimeline::endOffset() const
{
    return CSSNumericFactory::px(computeTimelineData().rangeEnd);
}

WTF::TextStream& operator<<(WTF::TextStream& ts, const StickinessAdjustmentData& stickiness)
{
    ts << "[ TopOrLeftAdjustment: " << stickiness.stickyTopOrLeftAdjustment << ", TopOrLeftLocation: " << stickiness.topOrLeftAdjustmentLocation << ", BottomOrRightAdjustment: " << stickiness.stickyBottomOrRightAdjustment << ", BottomOrRightLocation: " << stickiness.bottomOrRightAdjustmentLocation << " ]";
    return ts;
}

WTF::TextStream& operator<<(WTF::TextStream& ts, const StickinessAdjustmentData::StickinessLocation& stickiness)
{
    switch (stickiness) {
    case StickinessAdjustmentData::StickinessLocation::BeforeEntry: ts << "BeforeEntry"; break;
    case StickinessAdjustmentData::StickinessLocation::DuringEntry: ts << "DuringEntry"; break;
    case StickinessAdjustmentData::StickinessLocation::WhileContained: ts << "WhileContained"; break;
    case StickinessAdjustmentData::StickinessLocation::DuringExit: ts << "DuringExit"; break;
    case StickinessAdjustmentData::StickinessLocation::AfterExit: ts << "AfterExit"; break;
    }
    return ts;
}

} // namespace WebCore
