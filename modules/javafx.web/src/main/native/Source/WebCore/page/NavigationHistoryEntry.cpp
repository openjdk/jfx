/*
 * Copyright (C) 2023 Igalia S.L. All rights reserved.
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
#include "NavigationHistoryEntry.h"

#include "EventNames.h"
#include "FrameLoader.h"
#include "HistoryController.h"
#include "JSDOMGlobalObject.h"
#include "LocalDOMWindow.h"
#include "Navigation.h"
#include "ScriptExecutionContext.h"
#include "SerializedScriptValue.h"
#include <JavaScriptCore/JSCJSValueInlines.h>
#include <wtf/TZoneMallocInlines.h>

namespace WebCore {

WTF_MAKE_TZONE_OR_ISO_ALLOCATED_IMPL(NavigationHistoryEntry);

NavigationHistoryEntry::NavigationHistoryEntry(Navigation& navigation, const DocumentState& originalDocumentState, Ref<HistoryItem>&& historyItem, String urlString, WTF::UUID key, RefPtr<SerializedScriptValue>&& state, WTF::UUID id)
    : ActiveDOMObject(navigation.protectedScriptExecutionContext().get())
    , m_navigation(navigation)
    , m_urlString(urlString)
    , m_key(key)
    , m_id(id)
    , m_state(state)
    , m_associatedHistoryItem(WTFMove(historyItem))
    , m_originalDocumentState(originalDocumentState)
{
}

Ref<NavigationHistoryEntry> NavigationHistoryEntry::create(Navigation& navigation, Ref<HistoryItem>&& historyItem)
{
    Ref entry = adoptRef(*new NavigationHistoryEntry(navigation, DocumentState::fromContext(navigation.protectedScriptExecutionContext().get()), WTFMove(historyItem), historyItem->urlString(), historyItem->uuidIdentifier()));
    entry->suspendIfNeeded();
    return entry;
}

Ref<NavigationHistoryEntry> NavigationHistoryEntry::create(Navigation& navigation, const NavigationHistoryEntry& other)
{
    Ref historyItem = other.m_associatedHistoryItem;
    RefPtr state = historyItem->navigationAPIStateObject();
    if (!state)
        state = other.m_state;
    Ref entry = adoptRef(*new NavigationHistoryEntry(navigation, DocumentState::fromContext(other.scriptExecutionContext()), WTFMove(historyItem), other.m_urlString, other.m_key, WTFMove(state), other.m_id));
    entry->suspendIfNeeded();
    return entry;
}

ScriptExecutionContext* NavigationHistoryEntry::scriptExecutionContext() const
{
    return ContextDestructionObserver::scriptExecutionContext();
}

enum EventTargetInterfaceType NavigationHistoryEntry::eventTargetInterface() const
{
    return EventTargetInterfaceType::NavigationHistoryEntry;
}

void NavigationHistoryEntry::eventListenersDidChange()
{
    m_hasDisposeEventListener = hasEventListeners(eventNames().disposeEvent);
}

const String& NavigationHistoryEntry::url() const
{
    RefPtr document = dynamicDowncast<Document>(scriptExecutionContext());
    if (!document || !document->isFullyActive())
        return nullString();
    // https://html.spec.whatwg.org/#dom-navigationhistoryentry-url (Step 4)
    if (document->identifier() != m_originalDocumentState.identifier && (m_originalDocumentState.referrerPolicy == ReferrerPolicy::NoReferrer || m_originalDocumentState.referrerPolicy == ReferrerPolicy::Origin))
        return nullString();
    return m_urlString;
}

String NavigationHistoryEntry::key() const
{
    RefPtr document = dynamicDowncast<Document>(scriptExecutionContext());
    if (!document || !document->isFullyActive())
        return nullString();
    return m_key.toString();
}

String NavigationHistoryEntry::id() const
{
    RefPtr document = dynamicDowncast<Document>(scriptExecutionContext());
    if (!document || !document->isFullyActive())
        return nullString();
    return m_id.toString();
}

uint64_t NavigationHistoryEntry::index() const
{
    RefPtr document = dynamicDowncast<Document>(scriptExecutionContext());
    if (!document || !document->isFullyActive())
        return -1;
    return document->domWindow()->navigation().entries().findIf([this] (auto& entry) {
        return entry.ptr() == this;
    });
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-navigationhistoryentry-samedocument
bool NavigationHistoryEntry::sameDocument() const
{
    RefPtr document = dynamicDowncast<Document>(scriptExecutionContext());
    if (!document || !document->isFullyActive())
        return false;
    RefPtr currentItem = document->frame() ? document->frame()->loader().history().currentItem() : nullptr;
    if (!currentItem)
        return false;
    return currentItem->documentSequenceNumber() == m_associatedHistoryItem->documentSequenceNumber();
}

JSC::JSValue NavigationHistoryEntry::getState(JSDOMGlobalObject& globalObject) const
{
    RefPtr document = dynamicDowncast<Document>(scriptExecutionContext());
    if (!document || !document->isFullyActive())
        return JSC::jsUndefined();

    if (!m_state)
        return JSC::jsUndefined();

    return m_state->deserialize(globalObject, &globalObject, SerializationErrorMode::Throwing);
}

void NavigationHistoryEntry::setState(RefPtr<SerializedScriptValue>&& state)
{
    m_state = state;
    m_associatedHistoryItem->setNavigationAPIStateObject(WTFMove(state));
}

auto NavigationHistoryEntry::DocumentState::fromContext(ScriptExecutionContext* context) -> DocumentState
{
    if (!context)
        return { };
    return { context->identifier(), context->referrerPolicy() };
}

bool NavigationHistoryEntry::virtualHasPendingActivity() const
{
    return m_hasDisposeEventListener && !m_hasDispatchedDisposeEvent && m_navigation;
}

void NavigationHistoryEntry::dispatchDisposeEvent()
{
    ASSERT(!m_hasDispatchedDisposeEvent);
    dispatchEvent(Event::create(eventNames().disposeEvent, { }, Event::IsTrusted::Yes));
    m_hasDispatchedDisposeEvent = true;
}

} // namespace WebCore
