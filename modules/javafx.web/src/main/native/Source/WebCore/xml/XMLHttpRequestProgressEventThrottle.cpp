/*
 * Copyright (C) 2010 Julien Chaffraix <jchaffraix@webkit.org>  All right reserved.
 * Copyright (C) 2012 Nokia Corporation and/or its subsidiary(-ies)
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
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "config.h"
#include "XMLHttpRequestProgressEventThrottle.h"

#include "EventNames.h"
#include "EventTarget.h"
#include "XMLHttpRequestProgressEvent.h"

namespace WebCore {

const Seconds XMLHttpRequestProgressEventThrottle::minimumProgressEventDispatchingInterval { 50_ms }; // 50 ms per specification.

XMLHttpRequestProgressEventThrottle::XMLHttpRequestProgressEventThrottle(EventTarget* target)
    : m_target(target)
    , m_dispatchDeferredEventsTimer(*this, &XMLHttpRequestProgressEventThrottle::dispatchDeferredEvents)
{
    ASSERT(target);
}

XMLHttpRequestProgressEventThrottle::~XMLHttpRequestProgressEventThrottle() = default;

void XMLHttpRequestProgressEventThrottle::dispatchThrottledProgressEvent(bool lengthComputable, unsigned long long loaded, unsigned long long total)
{
    m_lengthComputable = lengthComputable;
    m_loaded = loaded;
    m_total = total;

    if (!m_target->hasEventListeners(eventNames().progressEvent))
        return;

    if (m_deferEvents) {
        // Only store the latest progress event while suspended.
        m_deferredProgressEvent = XMLHttpRequestProgressEvent::create(eventNames().progressEvent, lengthComputable, loaded, total);
        return;
    }

    if (!isActive()) {
        // The timer is not active so the least frequent event for now is every byte. Just dispatch the event.

        // We should not have any throttled progress event.
        ASSERT(!m_hasThrottledProgressEvent);

        dispatchEvent(XMLHttpRequestProgressEvent::create(eventNames().progressEvent, lengthComputable, loaded, total));
        startRepeating(minimumProgressEventDispatchingInterval);
        m_hasThrottledProgressEvent = false;
        return;
    }

    // The timer is already active so minimumProgressEventDispatchingInterval is the least frequent event.
    m_hasThrottledProgressEvent = true;
}

void XMLHttpRequestProgressEventThrottle::dispatchReadyStateChangeEvent(Event& event, ProgressEventAction progressEventAction)
{
    if (progressEventAction == FlushProgressEvent)
        flushProgressEvent();

    dispatchEvent(event);
}

void XMLHttpRequestProgressEventThrottle::dispatchEvent(Event& event)
{
    if (m_deferEvents) {
        if (m_deferredEvents.size() > 1 && event.type() == eventNames().readystatechangeEvent && event.type() == m_deferredEvents.last()->type()) {
            // Readystatechange events are state-less so avoid repeating two identical events in a row on resume.
            return;
        }
        m_deferredEvents.append(event);
    } else
        m_target->dispatchEvent(event);
}

void XMLHttpRequestProgressEventThrottle::dispatchProgressEvent(const AtomString& type)
{
    ASSERT(type == eventNames().loadstartEvent || type == eventNames().progressEvent || type == eventNames().loadEvent || type == eventNames().loadendEvent || type == eventNames().abortEvent || type == eventNames().errorEvent || type == eventNames().timeoutEvent);

    if (type == eventNames().loadstartEvent) {
        m_lengthComputable = false;
        m_loaded = 0;
        m_total = 0;
    }

    if (m_target->hasEventListeners(type))
        dispatchEvent(XMLHttpRequestProgressEvent::create(type, m_lengthComputable, m_loaded, m_total));
}

void XMLHttpRequestProgressEventThrottle::flushProgressEvent()
{
    if (m_deferEvents && m_deferredProgressEvent) {
        // Move the progress event to the queue, to get it in the right order on resume.
        m_deferredEvents.append(m_deferredProgressEvent.releaseNonNull());
        return;
    }

    if (!hasEventToDispatch())
        return;
    Ref<Event> event = XMLHttpRequestProgressEvent::create(eventNames().progressEvent, m_lengthComputable, m_loaded, m_total);
    m_hasThrottledProgressEvent = false;

    // We stop the timer as this is called when no more events are supposed to occur.
    stop();

    dispatchEvent(WTFMove(event));
}

void XMLHttpRequestProgressEventThrottle::dispatchDeferredEvents()
{
    ASSERT(m_deferEvents);
    m_deferEvents = false;

    // Take over the deferred events before dispatching them which can potentially add more.
    auto deferredEvents = WTFMove(m_deferredEvents);

    RefPtr<Event> deferredProgressEvent = WTFMove(m_deferredProgressEvent);

    for (auto& deferredEvent : deferredEvents)
        dispatchEvent(deferredEvent);

    // The progress event will be in the m_deferredEvents vector if the load was finished while suspended.
    // If not, just send the most up-to-date progress on resume.
    if (deferredProgressEvent)
        dispatchEvent(*deferredProgressEvent);
}

void XMLHttpRequestProgressEventThrottle::fired()
{
    ASSERT(isActive());
    if (!hasEventToDispatch()) {
        // No progress event was queued since the previous dispatch, we can safely stop the timer.
        stop();
        return;
    }

    dispatchEvent(XMLHttpRequestProgressEvent::create(eventNames().progressEvent, m_lengthComputable, m_loaded, m_total));
    m_hasThrottledProgressEvent = false;
}

bool XMLHttpRequestProgressEventThrottle::hasEventToDispatch() const
{
    return m_hasThrottledProgressEvent && isActive();
}

void XMLHttpRequestProgressEventThrottle::suspend()
{
    // If re-suspended before deferred events have been dispatched, just stop the dispatch
    // and continue the last suspend.
    if (m_dispatchDeferredEventsTimer.isActive()) {
        ASSERT(m_deferEvents);
        m_dispatchDeferredEventsTimer.stop();
        return;
    }
    ASSERT(!m_deferredProgressEvent);
    ASSERT(m_deferredEvents.isEmpty());
    ASSERT(!m_deferEvents);

    m_deferEvents = true;
    // If we have a progress event waiting to be dispatched,
    // just defer it.
    if (hasEventToDispatch()) {
        m_deferredProgressEvent = XMLHttpRequestProgressEvent::create(eventNames().progressEvent, m_lengthComputable, m_loaded, m_total);
        m_hasThrottledProgressEvent = false;
    }
    stop();
}

void XMLHttpRequestProgressEventThrottle::resume()
{
    ASSERT(!m_hasThrottledProgressEvent);

    if (m_deferredEvents.isEmpty() && !m_deferredProgressEvent) {
        m_deferEvents = false;
        return;
    }

    // Do not dispatch events inline here, since ScriptExecutionContext is iterating over
    // the list of active DOM objects to resume them, and any activated JS event-handler
    // could insert new active DOM objects to the list.
    // m_deferEvents is kept true until all deferred events have been dispatched.
    m_dispatchDeferredEventsTimer.startOneShot(0_s);
}

} // namespace WebCore
