/*
 * Copyright (C) 2017 Apple Inc. All rights reserved.
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
#include "DocumentTimeline.h"

#include "AnimationPlaybackEvent.h"
#include "CSSAnimation.h"
#include "CSSPropertyAnimation.h"
#include "CSSTransition.h"
#include "DOMWindow.h"
#include "DeclarativeAnimation.h"
#include "Document.h"
#include "GraphicsLayer.h"
#include "KeyframeEffect.h"
#include "Microtasks.h"
#include "Node.h"
#include "Page.h"
#include "PseudoElement.h"
#include "RenderElement.h"
#include "RenderLayer.h"
#include "RenderLayerBacking.h"

static const Seconds defaultAnimationInterval { 15_ms };
static const Seconds throttledAnimationInterval { 30_ms };

namespace WebCore {

Ref<DocumentTimeline> DocumentTimeline::create(Document& document)
{
    return adoptRef(*new DocumentTimeline(document, 0_s));
}

Ref<DocumentTimeline> DocumentTimeline::create(Document& document, DocumentTimelineOptions&& options)
{
    return adoptRef(*new DocumentTimeline(document, Seconds::fromMilliseconds(options.originTime)));
}

DocumentTimeline::DocumentTimeline(Document& document, Seconds originTime)
    : AnimationTimeline()
    , m_tickScheduleTimer(*this, &DocumentTimeline::scheduleAnimationResolution)
    , m_document(&document)
    , m_originTime(originTime)
{
    if (m_document && m_document->page() && !m_document->page()->isVisible())
        suspendAnimations();
}

DocumentTimeline::~DocumentTimeline() = default;

void DocumentTimeline::detachFromDocument()
{
    m_currentTimeClearingTaskQueue.close();
    m_elementsWithRunningAcceleratedAnimations.clear();

    auto& animationsToRemove = m_animations;
    while (!animationsToRemove.isEmpty())
        animationsToRemove.first()->remove();

    unscheduleAnimationResolution();
    m_document = nullptr;
}

static inline bool compareDeclarativeAnimationOwningElementPositionsInDocumentTreeOrder(Element* lhsOwningElement, Element* rhsOwningElement)
{
    // With regard to pseudo-elements, the sort order is as follows:
    //     - element
    //     - ::before
    //     - ::after
    //     - element children

    // We could be comparing two pseudo-elements that are hosted on the same element.
    if (is<PseudoElement>(lhsOwningElement) && is<PseudoElement>(rhsOwningElement)) {
        auto* lhsPseudoElement = downcast<PseudoElement>(lhsOwningElement);
        auto* rhsPseudoElement = downcast<PseudoElement>(rhsOwningElement);
        if (lhsPseudoElement->hostElement() == rhsPseudoElement->hostElement())
            return lhsPseudoElement->isBeforePseudoElement();
    }

    // Or comparing a pseudo-element that is compared to another non-pseudo element, in which case
    // we want to see if it's hosted on that other element, and if not use its host element to compare.
    if (is<PseudoElement>(lhsOwningElement)) {
        auto* lhsHostElement = downcast<PseudoElement>(lhsOwningElement)->hostElement();
        if (rhsOwningElement == lhsHostElement)
            return false;
        lhsOwningElement = lhsHostElement;
    }

    if (is<PseudoElement>(rhsOwningElement)) {
        auto* rhsHostElement = downcast<PseudoElement>(rhsOwningElement)->hostElement();
        if (lhsOwningElement == rhsHostElement)
            return true;
        rhsOwningElement = rhsHostElement;
    }

    return lhsOwningElement->compareDocumentPosition(*rhsOwningElement) & Node::DOCUMENT_POSITION_FOLLOWING;
}

Vector<RefPtr<WebAnimation>> DocumentTimeline::getAnimations() const
{
    ASSERT(m_document);

    Vector<RefPtr<WebAnimation>> cssTransitions;
    Vector<RefPtr<WebAnimation>> cssAnimations;
    Vector<RefPtr<WebAnimation>> webAnimations;

    // First, let's get all qualifying animations in their right group.
    for (const auto& animation : m_allAnimations) {
        if (!animation || !animation->isRelevant() || animation->timeline() != this || !is<KeyframeEffect>(animation->effect()))
            continue;

        auto* target = downcast<KeyframeEffect>(animation->effect())->target();
        if (!target || !target->isDescendantOf(*m_document))
            continue;

        if (is<CSSTransition>(animation.get()) && downcast<CSSTransition>(animation.get())->owningElement())
            cssTransitions.append(animation.get());
        else if (is<CSSAnimation>(animation.get()) && downcast<CSSAnimation>(animation.get())->owningElement())
            cssAnimations.append(animation.get());
        else
            webAnimations.append(animation.get());
    }

    // Now sort CSS Transitions by their composite order.
    std::sort(cssTransitions.begin(), cssTransitions.end(), [](auto& lhs, auto& rhs) {
        // https://drafts.csswg.org/css-transitions-2/#animation-composite-order
        auto* lhsTransition = downcast<CSSTransition>(lhs.get());
        auto* rhsTransition = downcast<CSSTransition>(rhs.get());

        auto* lhsOwningElement = lhsTransition->owningElement();
        auto* rhsOwningElement = rhsTransition->owningElement();

        // If the owning element of A and B differs, sort A and B by tree order of their corresponding owning elements.
        if (lhsOwningElement != rhsOwningElement)
            return compareDeclarativeAnimationOwningElementPositionsInDocumentTreeOrder(lhsOwningElement, rhsOwningElement);

        // Otherwise, if A and B have different transition generation values, sort by their corresponding transition generation in ascending order.
        if (lhsTransition->generationTime() != rhsTransition->generationTime())
            return lhsTransition->generationTime() < rhsTransition->generationTime();

        // Otherwise, sort A and B in ascending order by the Unicode codepoints that make up the expanded transition property name of each transition
        // (i.e. without attempting case conversion and such that ‘-moz-column-width’ sorts before ‘column-width’).
        return lhsTransition->transitionProperty().utf8() < rhsTransition->transitionProperty().utf8();
    });

    // Now sort CSS Animations by their composite order.
    std::sort(cssAnimations.begin(), cssAnimations.end(), [](auto& lhs, auto& rhs) {
        // https://drafts.csswg.org/css-animations-2/#animation-composite-order
        auto* lhsOwningElement = downcast<CSSAnimation>(lhs.get())->owningElement();
        auto* rhsOwningElement = downcast<CSSAnimation>(rhs.get())->owningElement();

        // If the owning element of A and B differs, sort A and B by tree order of their corresponding owning elements.
        if (lhsOwningElement != rhsOwningElement)
            return compareDeclarativeAnimationOwningElementPositionsInDocumentTreeOrder(lhsOwningElement, rhsOwningElement);

        // Otherwise, sort A and B based on their position in the computed value of the animation-name property of the (common) owning element.
        // In our case, this matches the time at which the animations were created and thus their relative position in m_allAnimations.
        return false;
    });

    // Finally, we can concatenate the sorted CSS Transitions, CSS Animations and Web Animations in their relative composite order.
    Vector<RefPtr<WebAnimation>> animations;
    animations.appendRange(cssTransitions.begin(), cssTransitions.end());
    animations.appendRange(cssAnimations.begin(), cssAnimations.end());
    animations.appendRange(webAnimations.begin(), webAnimations.end());
    return animations;
}

void DocumentTimeline::updateThrottlingState()
{
    scheduleAnimationResolution();
}

Seconds DocumentTimeline::animationInterval() const
{
    if (!m_document || !m_document->page())
        return Seconds::infinity();
    return m_document->page()->isLowPowerModeEnabled() ? throttledAnimationInterval : defaultAnimationInterval;
}

void DocumentTimeline::suspendAnimations()
{
    if (animationsAreSuspended())
        return;

    if (!m_cachedCurrentTime)
        m_cachedCurrentTime = Seconds(liveCurrentTime());

    for (const auto& animation : m_animations)
        animation->setSuspended(true);

    m_isSuspended = true;

    applyPendingAcceleratedAnimations();

    unscheduleAnimationResolution();
}

void DocumentTimeline::resumeAnimations()
{
    if (!animationsAreSuspended())
        return;

    m_cachedCurrentTime = WTF::nullopt;

    m_isSuspended = false;

    for (const auto& animation : m_animations)
        animation->setSuspended(false);

    scheduleAnimationResolution();
}

bool DocumentTimeline::animationsAreSuspended()
{
    return m_isSuspended;
}

unsigned DocumentTimeline::numberOfActiveAnimationsForTesting() const
{
    unsigned count = 0;
    for (const auto& animation : m_animations) {
        if (!animation->isSuspended())
            ++count;
    }
    return count;
}

DOMHighResTimeStamp DocumentTimeline::liveCurrentTime() const
{
    return m_document->domWindow()->nowTimestamp();
}

Optional<Seconds> DocumentTimeline::currentTime()
{
    if (!m_document || !m_document->domWindow())
        return AnimationTimeline::currentTime();

    auto& mainDocumentTimeline = m_document->timeline();
    if (&mainDocumentTimeline != this) {
        if (auto mainDocumentTimelineCurrentTime = mainDocumentTimeline.currentTime())
            return *mainDocumentTimelineCurrentTime - m_originTime;
        return WTF::nullopt;
    }

    if (!m_cachedCurrentTime)
        cacheCurrentTime(liveCurrentTime());

    return m_cachedCurrentTime.value() - m_originTime;
}

void DocumentTimeline::cacheCurrentTime(DOMHighResTimeStamp newCurrentTime)
{
    m_cachedCurrentTime = Seconds(newCurrentTime);
    // We want to be sure to keep this time cached until we've both finished running JS and finished updating
    // animations, so we schedule the invalidation task and register a whenIdle callback on the VM, which will
    // fire syncronously if no JS is running.
    m_waitingOnVMIdle = true;
    if (!m_currentTimeClearingTaskQueue.hasPendingTasks())
        m_currentTimeClearingTaskQueue.enqueueTask(std::bind(&DocumentTimeline::maybeClearCachedCurrentTime, this));
    m_document->vm().whenIdle([this, protectedThis = makeRefPtr(this)]() {
        m_waitingOnVMIdle = false;
        maybeClearCachedCurrentTime();
    });
}

void DocumentTimeline::maybeClearCachedCurrentTime()
{
    // We want to make sure we only clear the cached current time if we're not currently running
    // JS or waiting on all current animation updating code to have completed. This is so that
    // we're guaranteed to have a consistent current time reported for all work happening in a given
    // JS frame or throughout updating animations in WebCore.
    if (!m_isSuspended && !m_waitingOnVMIdle && !m_currentTimeClearingTaskQueue.hasPendingTasks())
        m_cachedCurrentTime = WTF::nullopt;
}

void DocumentTimeline::animationTimingDidChange(WebAnimation& animation)
{
    AnimationTimeline::animationTimingDidChange(animation);
    scheduleAnimationResolution();
}

void DocumentTimeline::removeAnimation(WebAnimation& animation)
{
    AnimationTimeline::removeAnimation(animation);

    if (m_animations.isEmpty())
        unscheduleAnimationResolution();
}

void DocumentTimeline::scheduleAnimationResolution()
{
    if (m_isSuspended || m_animations.isEmpty() || m_animationResolutionScheduled)
        return;

    if (!m_document || !m_document->page())
        return;

    m_document->page()->renderingUpdateScheduler().scheduleTimedRenderingUpdate();
    m_animationResolutionScheduled = true;
}

void DocumentTimeline::unscheduleAnimationResolution()
{
    m_tickScheduleTimer.stop();
    m_animationResolutionScheduled = false;
}

void DocumentTimeline::updateAnimationsAndSendEvents(DOMHighResTimeStamp timestamp)
{
    // We need to freeze the current time even if no animation is running.
    // document.timeline.currentTime may be called from a rAF callback and
    // it has to match the rAF timestamp.
    if (!m_isSuspended)
        cacheCurrentTime(timestamp);

    if (m_isSuspended || m_animations.isEmpty() || !m_animationResolutionScheduled)
        return;

    internalUpdateAnimationsAndSendEvents();
    applyPendingAcceleratedAnimations();

    m_animationResolutionScheduled = false;
    scheduleNextTick();
}

void DocumentTimeline::internalUpdateAnimationsAndSendEvents()
{
    m_numberOfAnimationTimelineInvalidationsForTesting++;

    // https://drafts.csswg.org/web-animations/#update-animations-and-send-events

    // 1. Update the current time of all timelines associated with doc passing now as the timestamp.

    Vector<RefPtr<WebAnimation>> animationsToRemove;
    Vector<RefPtr<CSSTransition>> completedTransitions;

    for (auto& animation : m_animations) {
        if (animation->timeline() != this) {
            ASSERT(!animation->timeline());
            animationsToRemove.append(animation);
            continue;
        }

        // This will notify the animation that timing has changed and will call automatically
        // schedule invalidation if required for this animation.
        animation->tick();

        if (!animation->isRelevant() && !animation->needsTick())
            animationsToRemove.append(animation);

        if (!animation->needsTick() && is<CSSTransition>(animation) && animation->playState() == WebAnimation::PlayState::Finished) {
            auto* transition = downcast<CSSTransition>(animation.get());
            if (transition->owningElement())
                completedTransitions.append(transition);
        }
    }

    // 2. Perform a microtask checkpoint.
    MicrotaskQueue::mainThreadQueue().performMicrotaskCheckpoint();

    // 3. Let events to dispatch be a copy of doc's pending animation event queue.
    // 4. Clear doc's pending animation event queue.
    auto pendingAnimationEvents = WTFMove(m_pendingAnimationEvents);

    // 5. Perform a stable sort of the animation events in events to dispatch as follows.
    std::stable_sort(pendingAnimationEvents.begin(), pendingAnimationEvents.end(), [] (const Ref<AnimationPlaybackEvent>& lhs, const Ref<AnimationPlaybackEvent>& rhs) {
        // 1. Sort the events by their scheduled event time such that events that were scheduled to occur earlier, sort before events scheduled to occur later
        // and events whose scheduled event time is unresolved sort before events with a resolved scheduled event time.
        // 2. Within events with equal scheduled event times, sort by their composite order. FIXME: We don't do this.
        if (lhs->timelineTime() && !rhs->timelineTime())
            return false;
        if (!lhs->timelineTime() && rhs->timelineTime())
            return true;
        if (!lhs->timelineTime() && !rhs->timelineTime())
            return true;
        return lhs->timelineTime().value() < rhs->timelineTime().value();
    });

    // 6. Dispatch each of the events in events to dispatch at their corresponding target using the order established in the previous step.
    for (auto& pendingEvent : pendingAnimationEvents)
        pendingEvent->target()->dispatchEvent(pendingEvent);

    // This will cancel any scheduled invalidation if we end up removing all animations.
    for (auto& animation : animationsToRemove)
        removeAnimation(*animation);

    // Now that animations that needed removal have been removed, let's update the list of completed transitions.
    // This needs to happen after dealing with the list of animations to remove as the animation may have been
    // removed from the list of completed transitions otherwise.
    for (auto& completedTransition : completedTransitions)
        transitionDidComplete(completedTransition);
}

void DocumentTimeline::transitionDidComplete(RefPtr<CSSTransition> transition)
{
    ASSERT(transition);
    removeAnimation(*transition);
    if (is<KeyframeEffect>(transition->effect())) {
        if (auto* target = downcast<KeyframeEffect>(transition->effect())->target()) {
            m_elementToCompletedCSSTransitionByCSSPropertyID.ensure(target, [] {
                return HashMap<CSSPropertyID, RefPtr<CSSTransition>> { };
            }).iterator->value.set(transition->property(), transition);
        }
    }
}

void DocumentTimeline::scheduleNextTick()
{
    // There is no tick to schedule if we don't have any relevant animations.
    if (m_animations.isEmpty())
        return;

    for (const auto& animation : m_animations) {
        if (!animation->isRunningAccelerated()) {
            scheduleAnimationResolution();
            return;
        }
    }

    Seconds scheduleDelay = Seconds::infinity();

    for (const auto& animation : m_animations) {
        auto animationTimeToNextRequiredTick = animation->timeToNextTick();
        if (animationTimeToNextRequiredTick < animationInterval()) {
            scheduleAnimationResolution();
            return;
        }
        scheduleDelay = std::min(scheduleDelay, animationTimeToNextRequiredTick);
    }

    if (scheduleDelay < Seconds::infinity())
        m_tickScheduleTimer.startOneShot(scheduleDelay);
}

bool DocumentTimeline::computeExtentOfAnimation(RenderElement& renderer, LayoutRect& bounds) const
{
    if (!renderer.element())
        return true;

    KeyframeEffect* matchingEffect = nullptr;
    for (const auto& animation : animationsForElement(*renderer.element())) {
        auto* effect = animation->effect();
        if (is<KeyframeEffect>(effect)) {
            auto* keyframeEffect = downcast<KeyframeEffect>(effect);
            if (keyframeEffect->animatedProperties().contains(CSSPropertyTransform))
                matchingEffect = downcast<KeyframeEffect>(effect);
        }
    }

    if (matchingEffect)
        return matchingEffect->computeExtentOfTransformAnimation(bounds);

    return true;
}

bool DocumentTimeline::isRunningAnimationOnRenderer(RenderElement& renderer, CSSPropertyID property) const
{
    if (!renderer.element())
        return false;

    for (const auto& animation : animationsForElement(*renderer.element())) {
        auto playState = animation->playState();
        if (playState != WebAnimation::PlayState::Running && playState != WebAnimation::PlayState::Paused)
            continue;
        auto* effect = animation->effect();
        if (is<KeyframeEffect>(effect) && downcast<KeyframeEffect>(effect)->animatedProperties().contains(property))
            return true;
    }

    return false;
}

bool DocumentTimeline::isRunningAcceleratedAnimationOnRenderer(RenderElement& renderer, CSSPropertyID property) const
{
    if (!renderer.element())
        return false;

    for (const auto& animation : animationsForElement(*renderer.element())) {
        auto playState = animation->playState();
        if (playState != WebAnimation::PlayState::Running && playState != WebAnimation::PlayState::Paused)
            continue;
        auto* effect = animation->effect();
        if (is<KeyframeEffect>(effect)) {
            auto* keyframeEffect = downcast<KeyframeEffect>(effect);
            if (keyframeEffect->isRunningAccelerated() && keyframeEffect->animatedProperties().contains(property))
                return true;
        }
    }

    return false;
}

std::unique_ptr<RenderStyle> DocumentTimeline::animatedStyleForRenderer(RenderElement& renderer)
{
    std::unique_ptr<RenderStyle> result;

    if (auto* element = renderer.element()) {
        for (const auto& animation : animationsForElement(*element)) {
            if (is<KeyframeEffect>(animation->effect()))
                downcast<KeyframeEffect>(animation->effect())->getAnimatedStyle(result);
        }
    }

    if (!result)
        result = RenderStyle::clonePtr(renderer.style());

    return result;
}

void DocumentTimeline::animationWasAddedToElement(WebAnimation& animation, Element& element)
{
    AnimationTimeline::animationWasAddedToElement(animation, element);
    updateListOfElementsWithRunningAcceleratedAnimationsForElement(element);
}

void DocumentTimeline::animationWasRemovedFromElement(WebAnimation& animation, Element& element)
{
    AnimationTimeline::animationWasRemovedFromElement(animation, element);
    updateListOfElementsWithRunningAcceleratedAnimationsForElement(element);
}

void DocumentTimeline::animationAcceleratedRunningStateDidChange(WebAnimation& animation)
{
    m_acceleratedAnimationsPendingRunningStateChange.add(&animation);

    if (is<KeyframeEffect>(animation.effect())) {
        if (auto* target = downcast<KeyframeEffect>(animation.effect())->target())
            updateListOfElementsWithRunningAcceleratedAnimationsForElement(*target);
    }
}

void DocumentTimeline::updateListOfElementsWithRunningAcceleratedAnimationsForElement(Element& element)
{
    auto animations = animationsForElement(element);

    if (animations.isEmpty()) {
        m_elementsWithRunningAcceleratedAnimations.remove(&element);
        return;
    }

    for (const auto& animation : animations) {
        if (!animation->isRunningAccelerated()) {
            m_elementsWithRunningAcceleratedAnimations.remove(&element);
            return;
        }
    }

    m_elementsWithRunningAcceleratedAnimations.add(&element);
}

void DocumentTimeline::applyPendingAcceleratedAnimations()
{
    auto acceleratedAnimationsPendingRunningStateChange = m_acceleratedAnimationsPendingRunningStateChange;
    m_acceleratedAnimationsPendingRunningStateChange.clear();

    bool hasForcedLayout = false;
    for (auto& animation : acceleratedAnimationsPendingRunningStateChange) {
        if (!hasForcedLayout) {
            auto* effect = animation->effect();
            if (is<KeyframeEffect>(effect))
                hasForcedLayout |= downcast<KeyframeEffect>(effect)->forceLayoutIfNeeded();
        }
        animation->applyPendingAcceleratedActions();
    }
}

bool DocumentTimeline::resolveAnimationsForElement(Element& element, RenderStyle& targetStyle)
{
    bool hasNonAcceleratedAnimationProperty = false;

    for (const auto& animation : animationsForElement(element)) {
        animation->resolve(targetStyle);

        if (hasNonAcceleratedAnimationProperty)
            continue;

        auto* effect = animation->effect();
        if (!effect || !is<KeyframeEffect>(effect))
            continue;

        auto* keyframeEffect = downcast<KeyframeEffect>(effect);
        for (auto cssPropertyId : keyframeEffect->animatedProperties()) {
            if (!CSSPropertyAnimation::animationOfPropertyIsAccelerated(cssPropertyId)) {
                hasNonAcceleratedAnimationProperty = true;
                break;
            }
        }
    }

    return !hasNonAcceleratedAnimationProperty;
}

bool DocumentTimeline::runningAnimationsForElementAreAllAccelerated(Element& element) const
{
    return m_elementsWithRunningAcceleratedAnimations.contains(&element);
}

void DocumentTimeline::enqueueAnimationPlaybackEvent(AnimationPlaybackEvent& event)
{
    m_pendingAnimationEvents.append(event);
}

Vector<std::pair<String, double>> DocumentTimeline::acceleratedAnimationsForElement(Element& element) const
{
    auto* renderer = element.renderer();
    if (renderer && renderer->isComposited()) {
        auto* compositedRenderer = downcast<RenderBoxModelObject>(renderer);
        if (auto* graphicsLayer = compositedRenderer->layer()->backing()->graphicsLayer())
            return graphicsLayer->acceleratedAnimationsForTesting();
    }
    return { };
}

unsigned DocumentTimeline::numberOfAnimationTimelineInvalidationsForTesting() const
{
    return m_numberOfAnimationTimelineInvalidationsForTesting;
}

} // namespace WebCore
