/*
 * Copyright (C) 2020 Apple Inc. All rights reserved.
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
#include "PseudoClassChangeInvalidation.h"

#include "ElementChildIteratorInlines.h"
#include "ElementRareData.h"
#include "StyleInvalidationFunctions.h"

namespace WebCore {
namespace Style {

Vector<PseudoClassInvalidationKey, 4> makePseudoClassInvalidationKeys(CSSSelector::PseudoClass pseudoClass, const Element& element)
{
    Vector<PseudoClassInvalidationKey, 4> keys;

    if (!element.idForStyleResolution().isEmpty())
        keys.append(makePseudoClassInvalidationKey(pseudoClass, InvalidationKeyType::Id, element.idForStyleResolution()));

    if (element.hasClass()) {
        keys.appendContainerWithMapping(element.classNames(), [&](auto& className) {
            return makePseudoClassInvalidationKey(pseudoClass, InvalidationKeyType::Class, className);
        });
    }

    keys.append(makePseudoClassInvalidationKey(pseudoClass, InvalidationKeyType::Tag, element.localNameLowercase()));
    keys.append(makePseudoClassInvalidationKey(pseudoClass, InvalidationKeyType::Universal));

    return keys;
};

void PseudoClassChangeInvalidation::computeInvalidation(CSSSelector::PseudoClass pseudoClass, Value value, InvalidationScope invalidationScope)
{
    bool shouldInvalidateCurrent = false;
    bool mayAffectStyleInShadowTree = false;

    traverseRuleFeatures(m_element, [&] (const RuleFeatureSet& features, bool mayAffectShadowTree) {
        if (mayAffectShadowTree && features.pseudoClasses.contains(pseudoClass))
            mayAffectStyleInShadowTree = true;
        if (m_element.shadowRoot() && features.pseudoClassesAffectingHost.contains(pseudoClass))
            shouldInvalidateCurrent = true;
    });

    if (mayAffectStyleInShadowTree) {
        // FIXME: We should do fine-grained invalidation for shadow tree.
        m_element.invalidateStyleForSubtree();
    }

    if (shouldInvalidateCurrent)
        m_element.invalidateStyle();

    for (auto& key : makePseudoClassInvalidationKeys(pseudoClass, m_element))
        collectRuleSets(key, value, invalidationScope);
}

void PseudoClassChangeInvalidation::collectRuleSets(const PseudoClassInvalidationKey& key, Value value, InvalidationScope invalidationScope)
{
    auto collect = [&](auto& ruleSets, std::optional<MatchElement> onlyMatchElement = { }) {
    auto* invalidationRuleSets = ruleSets.pseudoClassInvalidationRuleSets(key);
    if (!invalidationRuleSets)
        return;

    for (auto& invalidationRuleSet : *invalidationRuleSets) {
            if (onlyMatchElement && invalidationRuleSet.matchElement != onlyMatchElement)
                continue;

        // For focus/hover we flip the whole ancestor chain. We only need to do deep invalidation traversal in the change root.
        auto shouldInvalidate = [&] {
            bool invalidatesAllDescendants = invalidationRuleSet.matchElement == MatchElement::Ancestor && isUniversalInvalidation(key);
            switch (invalidationScope) {
            case InvalidationScope::All:
                return true;
            case InvalidationScope::SelfChildrenAndSiblings:
                return !invalidatesAllDescendants;
            case InvalidationScope::Descendants:
                return invalidatesAllDescendants;
            }
            ASSERT_NOT_REACHED();
            return true;
        }();
        if (!shouldInvalidate)
            continue;

        if (value == Value::Any) {
            Invalidator::addToMatchElementRuleSets(m_beforeChangeRuleSets, invalidationRuleSet);
            Invalidator::addToMatchElementRuleSets(m_afterChangeRuleSets, invalidationRuleSet);
            continue;
        }

        bool invalidateBeforeChange = invalidationRuleSet.isNegation == IsNegation::Yes ? value == Value::True : value == Value::False;
        if (invalidateBeforeChange)
            Invalidator::addToMatchElementRuleSets(m_beforeChangeRuleSets, invalidationRuleSet);
        else
            Invalidator::addToMatchElementRuleSets(m_afterChangeRuleSets, invalidationRuleSet);
    }
    };

    collect(m_element.styleResolver().ruleSets());

    if (auto* shadowRoot = m_element.shadowRoot())
        collect(shadowRoot->styleScope().resolver().ruleSets(), MatchElement::Host);
}

void PseudoClassChangeInvalidation::invalidateBeforeChange()
{
    Invalidator::invalidateWithMatchElementRuleSets(m_element, m_beforeChangeRuleSets);
}

void PseudoClassChangeInvalidation::invalidateAfterChange()
{
    Invalidator::invalidateWithMatchElementRuleSets(m_element, m_afterChangeRuleSets);
}


}
}
