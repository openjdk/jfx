/*
 * Copyright (C) 2012-2015 Apple Inc. All rights reserved.
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

#if ENABLE(ASYNC_SCROLLING)

#include "PlatformWheelEvent.h"
#include "Region.h"
#include "ScrollingCoordinator.h"
#include "WheelEventTestMonitor.h"
#include <wtf/HashMap.h>
#include <wtf/Lock.h>
#include <wtf/ThreadSafeRefCounted.h>
#include <wtf/TypeCasts.h>

namespace WebCore {

class IntPoint;
class ScrollingStateTree;
class ScrollingStateNode;
class ScrollingTreeFrameScrollingNode;
class ScrollingTreeNode;
class ScrollingTreeOverflowScrollProxyNode;
class ScrollingTreePositionedNode;
class ScrollingTreeScrollingNode;

class ScrollingTree : public ThreadSafeRefCounted<ScrollingTree> {
public:
    WEBCORE_EXPORT ScrollingTree();
    WEBCORE_EXPORT virtual ~ScrollingTree();

    virtual bool isThreadedScrollingTree() const { return false; }
    virtual bool isRemoteScrollingTree() const { return false; }
    virtual bool isScrollingTreeIOS() const { return false; }

    // This implies that we'll do hit-testing in the scrolling tree.
    bool asyncFrameOrOverflowScrollingEnabled() const { return m_asyncFrameOrOverflowScrollingEnabled; }
    void setAsyncFrameOrOverflowScrollingEnabled(bool);

    virtual ScrollingEventResult tryToHandleWheelEvent(const PlatformWheelEvent&) = 0;
    WEBCORE_EXPORT bool shouldHandleWheelEventSynchronously(const PlatformWheelEvent&);

    void setMainFrameIsRubberBanding(bool);
    bool isRubberBandInProgress();
    void setMainFrameIsScrollSnapping(bool);
    bool isScrollSnapInProgress();

    virtual void invalidate() { }
    WEBCORE_EXPORT virtual void commitTreeState(std::unique_ptr<ScrollingStateTree>);

    WEBCORE_EXPORT virtual void applyLayerPositions();
    WEBCORE_EXPORT void applyLayerPositionsAfterCommit();

    virtual Ref<ScrollingTreeNode> createScrollingTreeNode(ScrollingNodeType, ScrollingNodeID) = 0;

    WEBCORE_EXPORT ScrollingTreeNode* nodeForID(ScrollingNodeID) const;

    // Called after a scrolling tree node has handled a scroll and updated its layers.
    // Updates FrameView/RenderLayer scrolling state and GraphicsLayers.
    virtual void scrollingTreeNodeDidScroll(ScrollingTreeScrollingNode&, ScrollingLayerPositionAction = ScrollingLayerPositionAction::Sync) = 0;

    // Called for requested scroll position updates.
    virtual void scrollingTreeNodeRequestsScroll(ScrollingNodeID, const FloatPoint& /*scrollPosition*/, ScrollType, ScrollClamping) { }

    // Delegated scrolling/zooming has caused the viewport to change, so update viewport-constrained layers
    WEBCORE_EXPORT void mainFrameViewportChangedViaDelegatedScrolling(const FloatPoint& scrollPosition, const WebCore::FloatRect& layoutViewport, double scale);

    void didScrollByDelegatedScrolling() { m_wasScrolledByDelegatedScrollingSincePreviousCommit = true; }

    void notifyRelatedNodesAfterScrollPositionChange(ScrollingTreeScrollingNode& changedNode);

    virtual void reportSynchronousScrollingReasonsChanged(MonotonicTime, SynchronousScrollingReasons) { }
    virtual void reportExposedUnfilledArea(MonotonicTime, unsigned /* unfilledArea */) { }

#if PLATFORM(IOS_FAMILY)
    virtual void scrollingTreeNodeWillStartPanGesture() { }
    virtual void scrollingTreeNodeWillStartScroll() { }
    virtual void scrollingTreeNodeDidEndScroll() { }
#endif

    WEBCORE_EXPORT TrackingType eventTrackingTypeForPoint(const AtomString& eventName, IntPoint);

#if PLATFORM(MAC)
    virtual void handleWheelEventPhase(PlatformWheelEventPhase) = 0;
    virtual void setActiveScrollSnapIndices(ScrollingNodeID, unsigned /*horizontalIndex*/, unsigned /*verticalIndex*/) { }

    virtual void deferWheelEventTestCompletionForReason(WheelEventTestMonitor::ScrollableAreaIdentifier, WheelEventTestMonitor::DeferReason) { }
    virtual void removeWheelEventTestCompletionDeferralForReason(WheelEventTestMonitor::ScrollableAreaIdentifier, WheelEventTestMonitor::DeferReason) { }
#endif

#if PLATFORM(COCOA)
    WEBCORE_EXPORT virtual void currentSnapPointIndicesDidChange(ScrollingNodeID, unsigned horizontal, unsigned vertical) = 0;
#endif

    void setMainFramePinState(bool pinnedToTheLeft, bool pinnedToTheRight, bool pinnedToTheTop, bool pinnedToTheBottom);

    // Can be called from any thread. Will update what edges allow rubber-banding.
    WEBCORE_EXPORT void setCanRubberBandState(bool canRubberBandAtLeft, bool canRubberBandAtRight, bool canRubberBandAtTop, bool canRubberBandAtBottom);

    bool isHandlingProgrammaticScroll() const { return m_isHandlingProgrammaticScroll; }
    void setIsHandlingProgrammaticScroll(bool isHandlingProgrammaticScroll) { m_isHandlingProgrammaticScroll = isHandlingProgrammaticScroll; }

    void setScrollPinningBehavior(ScrollPinningBehavior);
    WEBCORE_EXPORT ScrollPinningBehavior scrollPinningBehavior();

    WEBCORE_EXPORT bool willWheelEventStartSwipeGesture(const PlatformWheelEvent&);

    WEBCORE_EXPORT void setScrollingPerformanceLoggingEnabled(bool flag);
    bool scrollingPerformanceLoggingEnabled();

    ScrollingTreeFrameScrollingNode* rootNode() const { return m_rootNode.get(); }

    ScrollingNodeID latchedNode();
    void setLatchedNode(ScrollingNodeID);
    void clearLatchedNode();

    bool hasLatchedNode() const { return m_treeState.latchedNodeID; }
    void setOrClearLatchedNode(const PlatformWheelEvent&, ScrollingNodeID);

    bool hasFixedOrSticky() const { return !!m_fixedOrStickyNodeCount; }
    void fixedOrStickyNodeAdded() { ++m_fixedOrStickyNodeCount; }
    void fixedOrStickyNodeRemoved()
    {
        ASSERT(m_fixedOrStickyNodeCount);
        --m_fixedOrStickyNodeCount;
    }

    // A map of overflow scrolling nodes to positioned nodes which need to be updated
    // when the scroller changes, but are not descendants.
    using RelatedNodesMap = HashMap<ScrollingNodeID, Vector<ScrollingNodeID>>;
    RelatedNodesMap& overflowRelatedNodes() { return m_overflowRelatedNodesMap; }

    HashSet<Ref<ScrollingTreeOverflowScrollProxyNode>>& activeOverflowScrollProxyNodes() { return m_activeOverflowScrollProxyNodes; }
    HashSet<Ref<ScrollingTreePositionedNode>>& activePositionedNodes() { return m_activePositionedNodes; }

    WEBCORE_EXPORT String scrollingTreeAsText(ScrollingStateTreeAsTextBehavior = ScrollingStateTreeAsTextBehaviorNormal);

    bool isMonitoringWheelEvents() const { return m_isMonitoringWheelEvents; }
    bool inCommitTreeState() const { return m_inCommitTreeState; }

protected:
    void setMainFrameScrollPosition(FloatPoint);

    WEBCORE_EXPORT virtual ScrollingEventResult handleWheelEvent(const PlatformWheelEvent&);

private:
    using OrphanScrollingNodeMap = HashMap<ScrollingNodeID, RefPtr<ScrollingTreeNode>>;
    void updateTreeFromStateNode(const ScrollingStateNode*, OrphanScrollingNodeMap&, HashSet<ScrollingNodeID>& unvisitedNodes);

    void applyLayerPositionsRecursive(ScrollingTreeNode&);

    void notifyRelatedNodesRecursive(ScrollingTreeNode&);

    Lock m_treeMutex; // Protects the scrolling tree.

    RefPtr<ScrollingTreeFrameScrollingNode> m_rootNode;

    using ScrollingTreeNodeMap = HashMap<ScrollingNodeID, RefPtr<ScrollingTreeNode>>;
    ScrollingTreeNodeMap m_nodeMap;

    RelatedNodesMap m_overflowRelatedNodesMap;

    HashSet<Ref<ScrollingTreeOverflowScrollProxyNode>> m_activeOverflowScrollProxyNodes;
    HashSet<Ref<ScrollingTreePositionedNode>> m_activePositionedNodes;

    struct TreeState {
        ScrollingNodeID latchedNodeID { 0 };
        EventTrackingRegions eventTrackingRegions;
        FloatPoint mainFrameScrollPosition;
        bool mainFrameIsRubberBanding { false };
        bool mainFrameIsScrollSnapping { false };
    };

    Lock m_treeStateMutex;
    TreeState m_treeState;

    struct SwipeState {
        ScrollPinningBehavior scrollPinningBehavior { DoNotPin };
        bool rubberBandsAtLeft { true };
        bool rubberBandsAtRight { true };
        bool rubberBandsAtTop { true };
        bool rubberBandsAtBottom { true };
        bool mainFramePinnedToTheLeft { true };
        bool mainFramePinnedToTheRight { true };
        bool mainFramePinnedToTheTop { true };
        bool mainFramePinnedToTheBottom { true };
    };

    Lock m_swipeStateMutex;
    SwipeState m_swipeState;

    unsigned m_fixedOrStickyNodeCount { 0 };
    bool m_isHandlingProgrammaticScroll { false };
    bool m_isMonitoringWheelEvents { false };
    bool m_scrollingPerformanceLoggingEnabled { false };
    bool m_asyncFrameOrOverflowScrollingEnabled { false };
    bool m_wasScrolledByDelegatedScrollingSincePreviousCommit { false };
    bool m_inCommitTreeState { false };
};

} // namespace WebCore

#define SPECIALIZE_TYPE_TRAITS_SCROLLING_TREE(ToValueTypeName, predicate) \
SPECIALIZE_TYPE_TRAITS_BEGIN(ToValueTypeName) \
    static bool isType(const WebCore::ScrollingTree& tree) { return tree.predicate; } \
SPECIALIZE_TYPE_TRAITS_END()
#endif // ENABLE(ASYNC_SCROLLING)
