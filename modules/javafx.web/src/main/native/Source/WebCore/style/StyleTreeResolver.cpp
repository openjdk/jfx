/*
 * Copyright (C) 1999 Lars Knoll (knoll@kde.org)
 *           (C) 1999 Antti Koivisto (koivisto@kde.org)
 *           (C) 2001 Peter Kelly (pmk@post.com)
 *           (C) 2001 Dirk Mueller (mueller@kde.org)
 *           (C) 2007 David Smith (catfish.man@gmail.com)
 * Copyright (C) 2004-2010, 2012-2016 Apple Inc. All rights reserved.
 *           (C) 2007 Eric Seidel (eric@webkit.org)
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

#include "config.h"
#include "StyleTreeResolver.h"

#include "CSSAnimationController.h"
#include "CSSFontSelector.h"
#include "ComposedTreeAncestorIterator.h"
#include "ComposedTreeIterator.h"
#include "DocumentTimeline.h"
#include "ElementIterator.h"
#include "Frame.h"
#include "HTMLBodyElement.h"
#include "HTMLMeterElement.h"
#include "HTMLNames.h"
#include "HTMLProgressElement.h"
#include "HTMLSlotElement.h"
#include "LoaderStrategy.h"
#include "NodeRenderStyle.h"
#include "Page.h"
#include "PlatformStrategies.h"
#include "Quirks.h"
#include "RenderElement.h"
#include "RenderStyle.h"
#include "RenderView.h"
#include "RuntimeEnabledFeatures.h"
#include "Settings.h"
#include "ShadowRoot.h"
#include "StyleFontSizeFunctions.h"
#include "StyleResolver.h"
#include "StyleScope.h"
#include "Text.h"

namespace WebCore {

namespace Style {

TreeResolver::TreeResolver(Document& document)
    : m_document(document)
{
}

TreeResolver::~TreeResolver() = default;

TreeResolver::Scope::Scope(Document& document)
    : styleResolver(document.styleScope().resolver())
    , sharingResolver(document, styleResolver.ruleSets(), selectorFilter)
{
    document.setIsResolvingTreeStyle(true);
}

TreeResolver::Scope::Scope(ShadowRoot& shadowRoot, Scope& enclosingScope)
    : styleResolver(shadowRoot.styleScope().resolver())
    , sharingResolver(shadowRoot.documentScope(), styleResolver.ruleSets(), selectorFilter)
    , shadowRoot(&shadowRoot)
    , enclosingScope(&enclosingScope)
{
    styleResolver.setOverrideDocumentElementStyle(enclosingScope.styleResolver.overrideDocumentElementStyle());
}

TreeResolver::Scope::~Scope()
{
    if (!shadowRoot)
        styleResolver.document().setIsResolvingTreeStyle(false);

    styleResolver.setOverrideDocumentElementStyle(nullptr);
}

TreeResolver::Parent::Parent(Document& document)
    : element(nullptr)
    , style(*document.renderStyle())
{
}

TreeResolver::Parent::Parent(Element& element, const RenderStyle& style, Change change, DescendantsToResolve descendantsToResolve)
    : element(&element)
    , style(style)
    , change(change)
    , descendantsToResolve(descendantsToResolve)
{
}

void TreeResolver::pushScope(ShadowRoot& shadowRoot)
{
    m_scopeStack.append(adoptRef(*new Scope(shadowRoot, scope())));
}

void TreeResolver::pushEnclosingScope()
{
    ASSERT(scope().enclosingScope);
    m_scopeStack.append(*scope().enclosingScope);
}

void TreeResolver::popScope()
{
    return m_scopeStack.removeLast();
}

std::unique_ptr<RenderStyle> TreeResolver::styleForElement(Element& element, const RenderStyle& inheritedStyle)
{
    if (element.hasCustomStyleResolveCallbacks()) {
        RenderStyle* shadowHostStyle = scope().shadowRoot ? m_update->elementStyle(*scope().shadowRoot->host()) : nullptr;
        if (auto customStyle = element.resolveCustomStyle(inheritedStyle, shadowHostStyle)) {
            if (customStyle->relations)
                commitRelations(WTFMove(customStyle->relations), *m_update);

            return WTFMove(customStyle->renderStyle);
        }
    }

    if (auto style = scope().sharingResolver.resolve(element, *m_update))
        return style;

    auto elementStyle = scope().styleResolver.styleForElement(element, &inheritedStyle, parentBoxStyle(), RuleMatchingBehavior::MatchAllRules, &scope().selectorFilter);

    if (elementStyle.relations)
        commitRelations(WTFMove(elementStyle.relations), *m_update);

    return WTFMove(elementStyle.renderStyle);
}

static void resetStyleForNonRenderedDescendants(Element& current)
{
    for (auto& child : childrenOfType<Element>(current)) {
        if (child.needsStyleRecalc()) {
            child.resetComputedStyle();
            child.resetStyleRelations();
            child.setHasValidStyle();
        }

        if (child.childNeedsStyleRecalc())
            resetStyleForNonRenderedDescendants(child);
    }
    current.clearChildNeedsStyleRecalc();
}

static bool affectsRenderedSubtree(Element& element, const RenderStyle& newStyle)
{
    if (newStyle.display() != DisplayType::None)
        return true;
    if (element.renderOrDisplayContentsStyle())
        return true;
    if (element.rendererIsNeeded(newStyle))
        return true;
    return false;
}

static DescendantsToResolve computeDescendantsToResolve(Change change, Validity validity, DescendantsToResolve parentDescendantsToResolve)
{
    if (parentDescendantsToResolve == DescendantsToResolve::All)
        return DescendantsToResolve::All;
    if (validity >= Validity::SubtreeInvalid)
        return DescendantsToResolve::All;
    switch (change) {
    case NoChange:
        return DescendantsToResolve::None;
    case NoInherit:
        return DescendantsToResolve::ChildrenWithExplicitInherit;
    case Inherit:
        return DescendantsToResolve::Children;
    case Detach:
        return DescendantsToResolve::All;
    };
    ASSERT_NOT_REACHED();
    return DescendantsToResolve::None;
};

ElementUpdates TreeResolver::resolveElement(Element& element)
{
    if (m_didSeePendingStylesheet && !element.renderer() && !m_document.isIgnoringPendingStylesheets()) {
        m_document.setHasNodesWithMissingStyle();
        return { };
    }

    auto newStyle = styleForElement(element, parent().style);

    if (!affectsRenderedSubtree(element, *newStyle))
        return { };

    auto* existingStyle = element.renderOrDisplayContentsStyle();

    if (m_didSeePendingStylesheet && (!existingStyle || existingStyle->isNotFinal())) {
        newStyle->setIsNotFinal();
        m_document.setHasNodesWithNonFinalStyle();
    }

    auto update = createAnimatedElementUpdate(WTFMove(newStyle), element, parent().change);
    auto descendantsToResolve = computeDescendantsToResolve(update.change, element.styleValidity(), parent().descendantsToResolve);

    if (&element == m_document.documentElement()) {
        m_documentElementStyle = RenderStyle::clonePtr(*update.style);
        scope().styleResolver.setOverrideDocumentElementStyle(m_documentElementStyle.get());

        if (update.change != NoChange && existingStyle && existingStyle->computedFontPixelSize() != update.style->computedFontPixelSize()) {
            // "rem" units are relative to the document element's font size so we need to recompute everything.
            // In practice this is rare.
            scope().styleResolver.invalidateMatchedPropertiesCache();
            descendantsToResolve = DescendantsToResolve::All;
        }
    }

    // This is needed for resolving color:-webkit-text for subsequent elements.
    // FIXME: We shouldn't mutate document when resolving style.
    if (&element == m_document.body())
        m_document.setTextColor(update.style->visitedDependentColor(CSSPropertyColor));

    // FIXME: These elements should not change renderer based on appearance property.
    if (element.hasTagName(HTMLNames::meterTag) || is<HTMLProgressElement>(element)) {
        if (existingStyle && update.style->appearance() != existingStyle->appearance()) {
            update.change = Detach;
            descendantsToResolve = DescendantsToResolve::All;
        }
    }

    auto beforeUpdate = resolvePseudoStyle(element, update, PseudoId::Before);
    auto afterUpdate = resolvePseudoStyle(element, update, PseudoId::After);

#if ENABLE(POINTER_EVENTS) && PLATFORM(IOS_FAMILY)
    // FIXME: Track this exactly.
    if (update.style->touchActions() != TouchAction::Auto && !m_document.quirks().shouldDisablePointerEventsQuirk() && RuntimeEnabledFeatures::sharedFeatures().pointerEventsEnabled())
        m_document.setMayHaveElementsWithNonAutoTouchAction();
#endif

    return { WTFMove(update), descendantsToResolve, WTFMove(beforeUpdate), WTFMove(afterUpdate) };
}

ElementUpdate TreeResolver::resolvePseudoStyle(Element& element, const ElementUpdate& elementUpdate, PseudoId pseudoId)
{
    if (elementUpdate.style->display() == DisplayType::None)
        return { };
    if (!elementUpdate.style->hasPseudoStyle(pseudoId))
        return { };

    auto pseudoStyle = scope().styleResolver.pseudoStyleForElement(element, { pseudoId }, *elementUpdate.style, &scope().selectorFilter);
    if (!pseudoElementRendererIsNeeded(pseudoStyle.get()))
        return { };

    PseudoElement* pseudoElement = pseudoId == PseudoId::Before ? element.beforePseudoElement() : element.afterPseudoElement();
    if (!pseudoElement) {
        auto newPseudoElement = PseudoElement::create(element, pseudoId);
        pseudoElement = newPseudoElement.ptr();
        if (pseudoId == PseudoId::Before)
            element.setBeforePseudoElement(WTFMove(newPseudoElement));
        else
            element.setAfterPseudoElement(WTFMove(newPseudoElement));
    }

    return createAnimatedElementUpdate(WTFMove(pseudoStyle), *pseudoElement, elementUpdate.change);
}

const RenderStyle* TreeResolver::parentBoxStyle() const
{
    // 'display: contents' doesn't generate boxes.
    for (unsigned i = m_parentStack.size(); i; --i) {
        auto& parent = m_parentStack[i - 1];
        if (parent.style.display() == DisplayType::None)
            return nullptr;
        if (parent.style.display() != DisplayType::Contents)
            return &parent.style;
    }
    ASSERT_NOT_REACHED();
    return nullptr;
}

ElementUpdate TreeResolver::createAnimatedElementUpdate(std::unique_ptr<RenderStyle> newStyle, Element& element, Change parentChange)
{
    auto* oldStyle = element.renderOrDisplayContentsStyle();

    bool shouldRecompositeLayer = false;

    // New code path for CSS Animations and CSS Transitions.
    if (RuntimeEnabledFeatures::sharedFeatures().webAnimationsCSSIntegrationEnabled()) {
        // First, we need to make sure that any new CSS animation occuring on this element has a matching WebAnimation
        // on the document timeline. Note that we get timeline() on the Document here because we need a timeline created
        // in case no Web Animations have been created through the JS API.
        if (element.document().pageCacheState() == Document::NotInPageCache && !element.document().renderView()->printing()) {
            if (oldStyle && (oldStyle->hasTransitions() || newStyle->hasTransitions()))
                m_document.timeline().updateCSSTransitionsForElement(element, *oldStyle, *newStyle);

            if ((oldStyle && oldStyle->hasAnimations()) || newStyle->hasAnimations())
                m_document.timeline().updateCSSAnimationsForElement(element, oldStyle, *newStyle);
        }
    }

    if (auto timeline = m_document.existingTimeline()) {
        // Now we can update all Web animations, which will include CSS Animations as well
        // as animations created via the JS API.
        auto animatedStyle = RenderStyle::clonePtr(*newStyle);
        shouldRecompositeLayer = timeline->resolveAnimationsForElement(element, *animatedStyle);
        newStyle = WTFMove(animatedStyle);
    }

    // Old code path for CSS Animations and CSS Transitions.
    if (!RuntimeEnabledFeatures::sharedFeatures().webAnimationsCSSIntegrationEnabled()) {
        auto& animationController = m_document.frame()->animation();

        auto animationUpdate = animationController.updateAnimations(element, *newStyle, oldStyle);
        shouldRecompositeLayer = animationUpdate.animationChangeRequiresRecomposite;

        if (animationUpdate.style)
            newStyle = WTFMove(animationUpdate.style);
    }

    auto change = oldStyle ? determineChange(*oldStyle, *newStyle) : Detach;

    auto validity = element.styleValidity();
    if (validity >= Validity::SubtreeAndRenderersInvalid || parentChange == Detach)
        change = Detach;

    shouldRecompositeLayer |= element.styleResolutionShouldRecompositeLayer();

    return { WTFMove(newStyle), change, shouldRecompositeLayer };
}

void TreeResolver::pushParent(Element& element, const RenderStyle& style, Change change, DescendantsToResolve descendantsToResolve)
{
    scope().selectorFilter.pushParent(&element);

    Parent parent(element, style, change, descendantsToResolve);

    if (auto* shadowRoot = element.shadowRoot()) {
        pushScope(*shadowRoot);
        parent.didPushScope = true;
    }
    else if (is<HTMLSlotElement>(element) && downcast<HTMLSlotElement>(element).assignedNodes()) {
        pushEnclosingScope();
        parent.didPushScope = true;
    }

    m_parentStack.append(WTFMove(parent));
}

void TreeResolver::popParent()
{
    auto& parentElement = *parent().element;

    parentElement.setHasValidStyle();
    parentElement.clearChildNeedsStyleRecalc();

    if (parent().didPushScope)
        popScope();

    scope().selectorFilter.popParent();

    m_parentStack.removeLast();
}

void TreeResolver::popParentsToDepth(unsigned depth)
{
    ASSERT(depth);
    ASSERT(m_parentStack.size() >= depth);

    while (m_parentStack.size() > depth)
        popParent();
}

static bool shouldResolvePseudoElement(const PseudoElement* pseudoElement)
{
    if (!pseudoElement)
        return false;
    return pseudoElement->needsStyleRecalc();
}

static bool shouldResolveElement(const Element& element, DescendantsToResolve parentDescendantsToResolve)
{
    if (element.styleValidity() != Validity::Valid)
        return true;
    if (shouldResolvePseudoElement(element.beforePseudoElement()))
        return true;
    if (shouldResolvePseudoElement(element.afterPseudoElement()))
        return true;

    switch (parentDescendantsToResolve) {
    case DescendantsToResolve::None:
        return false;
    case DescendantsToResolve::Children:
    case DescendantsToResolve::All:
        return true;
    case DescendantsToResolve::ChildrenWithExplicitInherit:
        auto* existingStyle = element.renderOrDisplayContentsStyle();
        return existingStyle && existingStyle->hasExplicitlyInheritedProperties();
    };
    ASSERT_NOT_REACHED();
    return false;
}

static void clearNeedsStyleResolution(Element& element)
{
    element.setHasValidStyle();
    if (auto* before = element.beforePseudoElement())
        before->setHasValidStyle();
    if (auto* after = element.afterPseudoElement())
        after->setHasValidStyle();
}

static bool hasLoadingStylesheet(const Style::Scope& styleScope, const Element& element, bool checkDescendants)
{
    if (!styleScope.hasPendingSheetsInBody())
        return false;
    if (styleScope.hasPendingSheetInBody(element))
        return true;
    if (!checkDescendants)
        return false;
    for (auto& descendant : descendantsOfType<Element>(element)) {
        if (styleScope.hasPendingSheetInBody(descendant))
            return true;
    };
    return false;
}

static std::unique_ptr<RenderStyle> createInheritedDisplayContentsStyleIfNeeded(const RenderStyle& parentElementStyle, const RenderStyle* parentBoxStyle)
{
    if (parentElementStyle.display() != DisplayType::Contents)
        return nullptr;
    if (parentBoxStyle && !parentBoxStyle->inheritedNotEqual(&parentElementStyle))
        return nullptr;
    // Compute style for imaginary unstyled <span> around the text node.
    auto style = RenderStyle::createPtr();
    style->inheritFrom(parentElementStyle);
    return style;
}

void TreeResolver::resolveComposedTree()
{
    ASSERT(m_parentStack.size() == 1);
    ASSERT(m_scopeStack.size() == 1);

    auto descendants = composedTreeDescendants(m_document);
    auto it = descendants.begin();
    auto end = descendants.end();

    while (it != end) {
        popParentsToDepth(it.depth());

        auto& node = *it;
        auto& parent = this->parent();

        ASSERT(node.isConnected());
        ASSERT(node.containingShadowRoot() == scope().shadowRoot);
        ASSERT(node.parentElement() == parent.element || is<ShadowRoot>(node.parentNode()) || node.parentElement()->shadowRoot());

        if (is<Text>(node)) {
            auto& text = downcast<Text>(node);

            if ((text.styleValidity() >= Validity::SubtreeAndRenderersInvalid && parent.change != Detach) || parent.style.display() == DisplayType::Contents) {
                TextUpdate textUpdate;
                textUpdate.inheritedDisplayContentsStyle = createInheritedDisplayContentsStyleIfNeeded(parent.style, parentBoxStyle());

                m_update->addText(text, parent.element, WTFMove(textUpdate));
            }

            text.setHasValidStyle();
            it.traverseNextSkippingChildren();
            continue;
        }

        auto& element = downcast<Element>(node);

        if (it.depth() > Settings::defaultMaximumRenderTreeDepth) {
            resetStyleForNonRenderedDescendants(element);
            it.traverseNextSkippingChildren();
            continue;
        }

        auto* style = element.renderOrDisplayContentsStyle();
        auto change = NoChange;
        auto descendantsToResolve = DescendantsToResolve::None;

        bool shouldResolve = shouldResolveElement(element, parent.descendantsToResolve);
        if (shouldResolve) {
            if (!element.hasDisplayContents())
                element.resetComputedStyle();
            element.resetStyleRelations();

            if (element.hasCustomStyleResolveCallbacks())
                element.willRecalcStyle(parent.change);

            auto elementUpdates = resolveElement(element);

            if (element.hasCustomStyleResolveCallbacks())
                element.didRecalcStyle(elementUpdates.update.change);

            style = elementUpdates.update.style.get();
            change = elementUpdates.update.change;
            descendantsToResolve = elementUpdates.descendantsToResolve;

            if (elementUpdates.update.style)
                m_update->addElement(element, parent.element, WTFMove(elementUpdates));

            clearNeedsStyleResolution(element);
        }

        if (!style)
            resetStyleForNonRenderedDescendants(element);

        bool shouldIterateChildren = style && (element.childNeedsStyleRecalc() || descendantsToResolve != DescendantsToResolve::None);

        if (!m_didSeePendingStylesheet)
            m_didSeePendingStylesheet = hasLoadingStylesheet(m_document.styleScope(), element, !shouldIterateChildren);

        if (!shouldIterateChildren) {
            it.traverseNextSkippingChildren();
            continue;
        }

        pushParent(element, *style, change, descendantsToResolve);

        it.traverseNext();
    }

    popParentsToDepth(1);
}

std::unique_ptr<Update> TreeResolver::resolve()
{
    auto& renderView = *m_document.renderView();

    Element* documentElement = m_document.documentElement();
    if (!documentElement) {
        m_document.styleScope().resolver();
        return nullptr;
    }
    if (!documentElement->childNeedsStyleRecalc() && !documentElement->needsStyleRecalc())
        return nullptr;

    m_didSeePendingStylesheet = m_document.styleScope().hasPendingSheetsBeforeBody();

    m_update = makeUnique<Update>(m_document);
    m_scopeStack.append(adoptRef(*new Scope(m_document)));
    m_parentStack.append(Parent(m_document));

    // Pseudo element removal and similar may only work with these flags still set. Reset them after the style recalc.
    renderView.setUsesFirstLineRules(renderView.usesFirstLineRules() || scope().styleResolver.usesFirstLineRules());
    renderView.setUsesFirstLetterRules(renderView.usesFirstLetterRules() || scope().styleResolver.usesFirstLetterRules());

    resolveComposedTree();

    renderView.setUsesFirstLineRules(scope().styleResolver.usesFirstLineRules());
    renderView.setUsesFirstLetterRules(scope().styleResolver.usesFirstLetterRules());

    ASSERT(m_scopeStack.size() == 1);
    ASSERT(m_parentStack.size() == 1);
    m_parentStack.clear();
    popScope();

    if (m_update->roots().isEmpty())
        return { };

    return WTFMove(m_update);
}

static Vector<Function<void ()>>& postResolutionCallbackQueue()
{
    static NeverDestroyed<Vector<Function<void ()>>> vector;
    return vector;
}

static Vector<RefPtr<Frame>>& memoryCacheClientCallsResumeQueue()
{
    static NeverDestroyed<Vector<RefPtr<Frame>>> vector;
    return vector;
}

void queuePostResolutionCallback(Function<void ()>&& callback)
{
    postResolutionCallbackQueue().append(WTFMove(callback));
}

static void suspendMemoryCacheClientCalls(Document& document)
{
    Page* page = document.page();
    if (!page || !page->areMemoryCacheClientCallsEnabled())
        return;

    page->setMemoryCacheClientCallsEnabled(false);

    memoryCacheClientCallsResumeQueue().append(&page->mainFrame());
}

static unsigned resolutionNestingDepth;

PostResolutionCallbackDisabler::PostResolutionCallbackDisabler(Document& document, DrainCallbacks drainCallbacks)
    : m_drainCallbacks(drainCallbacks)
{
    ++resolutionNestingDepth;

    if (resolutionNestingDepth == 1)
        platformStrategies()->loaderStrategy()->suspendPendingRequests();

    // FIXME: It's strange to build this into the disabler.
    suspendMemoryCacheClientCalls(document);
}

PostResolutionCallbackDisabler::~PostResolutionCallbackDisabler()
{
    if (resolutionNestingDepth == 1) {
        if (m_drainCallbacks == DrainCallbacks::Yes) {
            // Get size each time through the loop because a callback can add more callbacks to the end of the queue.
            auto& queue = postResolutionCallbackQueue();
            for (size_t i = 0; i < queue.size(); ++i)
                queue[i]();
            queue.clear();
        }

        auto& queue = memoryCacheClientCallsResumeQueue();
        for (size_t i = 0; i < queue.size(); ++i) {
            if (auto* page = queue[i]->page())
                page->setMemoryCacheClientCallsEnabled(true);
        }
        queue.clear();

        platformStrategies()->loaderStrategy()->resumePendingRequests();
    }

    --resolutionNestingDepth;
}

bool postResolutionCallbacksAreSuspended()
{
    return resolutionNestingDepth;
}

}
}
