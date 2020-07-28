/*
 * Copyright (C) 2012 Apple Inc. All rights reserved.
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
#include "ScrollingTreeScrollingNode.h"

#if ENABLE(ASYNC_SCROLLING)

#include "Logging.h"
#include "ScrollingStateScrollingNode.h"
#include "ScrollingStateTree.h"
#include "ScrollingTree.h"
#include <wtf/text/TextStream.h>

namespace WebCore {

ScrollingTreeScrollingNode::ScrollingTreeScrollingNode(ScrollingTree& scrollingTree, ScrollingNodeType nodeType, ScrollingNodeID nodeID)
    : ScrollingTreeNode(scrollingTree, nodeType, nodeID)
{
}

ScrollingTreeScrollingNode::~ScrollingTreeScrollingNode() = default;

void ScrollingTreeScrollingNode::commitStateBeforeChildren(const ScrollingStateNode& stateNode)
{
    const ScrollingStateScrollingNode& state = downcast<ScrollingStateScrollingNode>(stateNode);

    if (state.hasChangedProperty(ScrollingStateScrollingNode::ScrollableAreaSize))
        m_scrollableAreaSize = state.scrollableAreaSize();

    if (state.hasChangedProperty(ScrollingStateScrollingNode::TotalContentsSize)) {
        if (scrollingTree().isRubberBandInProgress())
            m_totalContentsSizeForRubberBand = m_totalContentsSize;
        else
            m_totalContentsSizeForRubberBand = state.totalContentsSize();

        m_totalContentsSize = state.totalContentsSize();
    }

    if (state.hasChangedProperty(ScrollingStateScrollingNode::ReachableContentsSize))
        m_reachableContentsSize = state.reachableContentsSize();

    if (state.hasChangedProperty(ScrollingStateScrollingNode::ScrollPosition)) {
        m_lastCommittedScrollPosition = state.scrollPosition();
        if (m_isFirstCommit && !state.hasChangedProperty(ScrollingStateScrollingNode::RequestedScrollPosition))
            m_currentScrollPosition = m_lastCommittedScrollPosition;
    }

    if (state.hasChangedProperty(ScrollingStateScrollingNode::ParentRelativeScrollableRect))
        m_parentRelativeScrollableRect = state.parentRelativeScrollableRect();

    if (state.hasChangedProperty(ScrollingStateScrollingNode::ScrollOrigin))
        m_scrollOrigin = state.scrollOrigin();

#if ENABLE(CSS_SCROLL_SNAP)
    if (state.hasChangedProperty(ScrollingStateScrollingNode::HorizontalSnapOffsets))
        m_snapOffsetsInfo.horizontalSnapOffsets = state.horizontalSnapOffsets();

    if (state.hasChangedProperty(ScrollingStateScrollingNode::VerticalSnapOffsets))
        m_snapOffsetsInfo.verticalSnapOffsets = state.verticalSnapOffsets();

    if (state.hasChangedProperty(ScrollingStateScrollingNode::HorizontalSnapOffsetRanges))
        m_snapOffsetsInfo.horizontalSnapOffsetRanges = state.horizontalSnapOffsetRanges();

    if (state.hasChangedProperty(ScrollingStateScrollingNode::VerticalSnapOffsetRanges))
        m_snapOffsetsInfo.verticalSnapOffsetRanges = state.verticalSnapOffsetRanges();

    if (state.hasChangedProperty(ScrollingStateScrollingNode::CurrentHorizontalSnapOffsetIndex))
        m_currentHorizontalSnapPointIndex = state.currentHorizontalSnapPointIndex();

    if (state.hasChangedProperty(ScrollingStateScrollingNode::CurrentVerticalSnapOffsetIndex))
        m_currentVerticalSnapPointIndex = state.currentVerticalSnapPointIndex();
#endif

    if (state.hasChangedProperty(ScrollingStateScrollingNode::ScrollableAreaParams))
        m_scrollableAreaParameters = state.scrollableAreaParameters();

    if (state.hasChangedProperty(ScrollingStateScrollingNode::ScrollContainerLayer))
        m_scrollContainerLayer = state.scrollContainerLayer();

    if (state.hasChangedProperty(ScrollingStateScrollingNode::ScrolledContentsLayer))
        m_scrolledContentsLayer = state.scrolledContentsLayer();
}

void ScrollingTreeScrollingNode::commitStateAfterChildren(const ScrollingStateNode& stateNode)
{
    const ScrollingStateScrollingNode& scrollingStateNode = downcast<ScrollingStateScrollingNode>(stateNode);
    if (scrollingStateNode.hasChangedProperty(ScrollingStateScrollingNode::RequestedScrollPosition)) {
        const auto& requestedScrollData = scrollingStateNode.requestedScrollData();
        scrollingTree().scrollingTreeNodeRequestsScroll(scrollingNodeID(), requestedScrollData.scrollPosition, requestedScrollData.scrollType, requestedScrollData.clamping);
    }

    m_isFirstCommit = false;
}

ScrollingEventResult ScrollingTreeScrollingNode::handleWheelEvent(const PlatformWheelEvent&)
{
    return ScrollingEventResult::DidNotHandleEvent;
}

FloatPoint ScrollingTreeScrollingNode::clampScrollPosition(const FloatPoint& scrollPosition) const
{
    return scrollPosition.constrainedBetween(minimumScrollPosition(), maximumScrollPosition());
}

FloatPoint ScrollingTreeScrollingNode::minimumScrollPosition() const
{
    auto minimumScrollOffset = FloatPoint { };
    return ScrollableArea::scrollPositionFromOffset(minimumScrollOffset, toFloatSize(scrollOrigin()));
}

FloatPoint ScrollingTreeScrollingNode::maximumScrollPosition() const
{
    FloatPoint contentSizePoint(totalContentsSize());
    auto maximumScrollOffset = FloatPoint(contentSizePoint - scrollableAreaSize()).expandedTo(FloatPoint());
    return ScrollableArea::scrollPositionFromOffset(maximumScrollOffset, toFloatSize(scrollOrigin()));
}

bool ScrollingTreeScrollingNode::scrollLimitReached(const PlatformWheelEvent& wheelEvent) const
{
    FloatPoint oldScrollPosition = currentScrollPosition();
    FloatPoint newScrollPosition = oldScrollPosition + FloatSize(wheelEvent.deltaX(), -wheelEvent.deltaY());
    newScrollPosition = newScrollPosition.constrainedBetween(minimumScrollPosition(), maximumScrollPosition());
    return newScrollPosition == oldScrollPosition;
}

FloatPoint ScrollingTreeScrollingNode::adjustedScrollPosition(const FloatPoint& scrollPosition, ScrollClamping clamping) const
{
    if (clamping == ScrollClamping::Clamped)
        return clampScrollPosition(scrollPosition);

    return scrollPosition;
}

void ScrollingTreeScrollingNode::scrollBy(const FloatSize& delta, ScrollClamping clamp)
{
    scrollTo(currentScrollPosition() + delta, ScrollType::User, clamp);
}

void ScrollingTreeScrollingNode::scrollTo(const FloatPoint& position, ScrollType scrollType, ScrollClamping clamp)
{
    if (position == m_currentScrollPosition)
        return;

    if (scrollType == ScrollType::Programmatic)
        stopScrollAnimations();

    scrollingTree().setIsHandlingProgrammaticScroll(scrollType == ScrollType::Programmatic);

    m_currentScrollPosition = adjustedScrollPosition(position, clamp);

    LOG_WITH_STREAM(Scrolling, stream << "ScrollingTreeScrollingNode " << scrollingNodeID() << " scrollTo " << position << " (delta from last committed position " << (m_lastCommittedScrollPosition - m_currentScrollPosition) << ")");

    updateViewportForCurrentScrollPosition();
    currentScrollPositionChanged();

    scrollingTree().setIsHandlingProgrammaticScroll(false);
}

void ScrollingTreeScrollingNode::currentScrollPositionChanged()
{
    repositionScrollingLayers();
    repositionRelatedLayers();

    scrollingTree().notifyRelatedNodesAfterScrollPositionChange(*this);
    scrollingTree().scrollingTreeNodeDidScroll(*this);
}

bool ScrollingTreeScrollingNode::scrollPositionAndLayoutViewportMatch(const FloatPoint& position, Optional<FloatRect>)
{
    return position == m_currentScrollPosition;
}

void ScrollingTreeScrollingNode::applyLayerPositions()
{
    repositionScrollingLayers();
    repositionRelatedLayers();
}

void ScrollingTreeScrollingNode::wasScrolledByDelegatedScrolling(const FloatPoint& position, Optional<FloatRect> overrideLayoutViewport, ScrollingLayerPositionAction scrollingLayerPositionAction)
{
    bool scrollPositionChanged = !scrollPositionAndLayoutViewportMatch(position, overrideLayoutViewport);
    if (!scrollPositionChanged && scrollingLayerPositionAction != ScrollingLayerPositionAction::Set)
        return;

    m_currentScrollPosition = adjustedScrollPosition(position, ScrollClamping::Unclamped);
    updateViewportForCurrentScrollPosition(overrideLayoutViewport);

    repositionRelatedLayers();

    scrollingTree().notifyRelatedNodesAfterScrollPositionChange(*this);
    scrollingTree().scrollingTreeNodeDidScroll(*this, scrollingLayerPositionAction);
    scrollingTree().didScrollByDelegatedScrolling();
}

LayoutPoint ScrollingTreeScrollingNode::parentToLocalPoint(LayoutPoint point) const
{
    return point - toLayoutSize(parentRelativeScrollableRect().location());
}

LayoutPoint ScrollingTreeScrollingNode::localToContentsPoint(LayoutPoint point) const
{
    return point + LayoutPoint(currentScrollPosition());
}

ScrollingTreeScrollingNode* ScrollingTreeScrollingNode::scrollingNodeForPoint(LayoutPoint parentPoint) const
{
    if (auto* node = ScrollingTreeNode::scrollingNodeForPoint(parentPoint))
        return node;

    if (parentRelativeScrollableRect().contains(parentPoint))
        return const_cast<ScrollingTreeScrollingNode*>(this);

    return nullptr;
}

void ScrollingTreeScrollingNode::dumpProperties(TextStream& ts, ScrollingStateTreeAsTextBehavior behavior) const
{
    ScrollingTreeNode::dumpProperties(ts, behavior);
    ts.dumpProperty("scrollable area size", m_scrollableAreaSize);
    ts.dumpProperty("total content size", m_totalContentsSize);
    if (m_totalContentsSizeForRubberBand != m_totalContentsSize)
        ts.dumpProperty("total content size for rubber band", m_totalContentsSizeForRubberBand);
    if (m_reachableContentsSize != m_totalContentsSize)
        ts.dumpProperty("reachable content size", m_reachableContentsSize);
    ts.dumpProperty("last committed scroll position", m_lastCommittedScrollPosition);

    if (!m_parentRelativeScrollableRect.isEmpty())
        ts.dumpProperty("parent relative scrollable rect", m_parentRelativeScrollableRect);

    if (m_scrollOrigin != IntPoint())
        ts.dumpProperty("scroll origin", m_scrollOrigin);

#if ENABLE(CSS_SCROLL_SNAP)
    if (m_snapOffsetsInfo.horizontalSnapOffsets.size())
        ts.dumpProperty("horizontal snap offsets", m_snapOffsetsInfo.horizontalSnapOffsets);

    if (m_snapOffsetsInfo.verticalSnapOffsets.size())
        ts.dumpProperty("vertical snap offsets", m_snapOffsetsInfo.verticalSnapOffsets);

    if (m_currentHorizontalSnapPointIndex)
        ts.dumpProperty("current horizontal snap point index", m_currentHorizontalSnapPointIndex);

    if (m_currentVerticalSnapPointIndex)
        ts.dumpProperty("current vertical snap point index", m_currentVerticalSnapPointIndex);

#endif

    ts.dumpProperty("scrollable area parameters", m_scrollableAreaParameters);
}

} // namespace WebCore

#endif // ENABLE(ASYNC_SCROLLING)
