/*
 * Copyright (C) 2025 Apple Inc. All rights reserved.
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
#include "DocumentImmersive.h"

#if ENABLE(MODEL_ELEMENT_IMMERSIVE)

#include "HTMLModelElement.h"
#include "PseudoClassChangeInvalidation.h"

namespace WebCore {

WTF_MAKE_TZONE_ALLOCATED_IMPL(DocumentImmersive);

DocumentImmersive::DocumentImmersive(Document& document)
    : m_document(document)
{
}

bool DocumentImmersive::immersiveEnabled(Document& document)
{
    if (!document.settings().modelElementImmersiveEnabled())
        return false;

    if (!document.isFullyActive())
        return false;

    return false; // Needs client support
}

Element* DocumentImmersive::immersiveElement(Document& document)
{
    RefPtr documentImmersive = document.immersiveIfExists();
    if (!documentImmersive)
        return nullptr;
    return document.ancestorElementInThisScope(documentImmersive->protectedImmersiveElement().get());
}

HTMLModelElement* DocumentImmersive::immersiveElement() const
{
    return m_immersiveElement.get();
}

void DocumentImmersive::exitImmersive(Document& document, Ref<DeferredPromise>&& promise)
{
    RefPtr immersive = document.immersiveIfExists();
    if (!document.isFullyActive() || !immersive) {
        promise->reject(Exception { ExceptionCode::TypeError, "Not in immersive"_s });
        return;
    }
    immersive->exitImmersive([promise = WTF::move(promise)](auto result) {
        if (result.hasException())
            promise->reject(result.releaseException());
        else
            promise->resolve();
    });
}

void DocumentImmersive::requestImmersive(HTMLModelElement* element, CompletionHandler<void(ExceptionOr<void>)>&& completionHandler)
{
    enum class EmitErrorEvent : bool { No, Yes };
    auto handleError = [weakElement = WeakPtr { *element }, weakThis = WeakPtr { *this }](String message, EmitErrorEvent emitErrorEvent, CompletionHandler<void(ExceptionOr<void>)>&& completionHandler) mutable {
        RefPtr protectedThis = weakThis.get();
        RefPtr protectedElement = weakElement.get();
        if (!protectedThis || !protectedElement)
            return completionHandler(Exception { ExceptionCode::TypeError, message });
        RELEASE_LOG_ERROR(Immersive, "%p - DocumentImmersive: %s", protectedThis.get(), message.utf8().data());
        if (emitErrorEvent == EmitErrorEvent::Yes) {
            protectedThis->queueImmersiveEventForElement(DocumentImmersive::EventType::Error, *protectedElement);
            protectedThis->protectedDocument()->scheduleRenderingUpdate(RenderingUpdateStep::Immersive);
        }
        completionHandler(Exception { ExceptionCode::TypeError, message });
    };

    if (!protectedDocument()->isFullyActive())
        return handleError("Cannot request immersive on a document that is not fully active."_s, EmitErrorEvent::No, WTF::move(completionHandler));

    if (RefPtr window = document().window(); !window || !window->consumeTransientActivation())
        return handleError("Cannot request immersive without transient activation."_s, EmitErrorEvent::Yes, WTF::move(completionHandler));

    RefPtr protectedPage = document().page();
    if (!protectedPage || !protectedPage->settings().modelElementImmersiveEnabled())
        return handleError("Immersive API is disabled."_s, EmitErrorEvent::Yes, WTF::move(completionHandler));

    protectedPage->chrome().client().allowImmersiveElement(*element, [weakElement = WeakPtr { *element }, weakThis = WeakPtr { *this }, weakPage = WeakPtr { *protectedPage }, handleError, completionHandler = WTF::move(completionHandler)](auto allowed) mutable {
        if (!allowed)
            return handleError("Immersive request was denied."_s, EmitErrorEvent::Yes, WTF::move(completionHandler));

        RefPtr protectedElement = weakElement.get();
        if (!protectedElement)
            return completionHandler(Exception { ExceptionCode::TypeError });

        protectedElement->ensureImmersivePresentation([weakElement, weakThis, weakPage, handleError, completionHandler = WTF::move(completionHandler)](auto result) mutable {
            if (result.hasException())
                return handleError(result.releaseException().message(), EmitErrorEvent::Yes, WTF::move(completionHandler));

            RefPtr protectedElement = weakElement.get();
            if (!protectedElement)
                return completionHandler(Exception { ExceptionCode::TypeError });

            RefPtr protectedPage = weakPage.get();
            if (!protectedPage) {
                protectedElement->exitImmersivePresentation([] { });
                completionHandler(Exception { ExceptionCode::TypeError });
                return;
            }

            protectedPage->chrome().client().presentImmersiveElement(*protectedElement, result.releaseReturnValue(), [weakElement, weakThis, handleError, completionHandler = WTF::move(completionHandler)](bool success) mutable {
                RefPtr protectedElement = weakElement.get();
                if (!protectedElement)
                    return completionHandler(Exception { ExceptionCode::TypeError });

                RefPtr protectedThis = weakThis.get();
                if (!protectedThis || !success) {
                    protectedElement->exitImmersivePresentation([] { });
                    handleError("Failure to present the immersive element."_s, EmitErrorEvent::Yes, WTF::move(completionHandler));
                    return;
                }

                if (RefPtr oldImmersiveElement = protectedThis->immersiveElement()) {
                    oldImmersiveElement->exitImmersivePresentation([] { });
                    protectedThis->updateElementIsImmersive(oldImmersiveElement.get(), false);
                }

                protectedThis->m_immersiveElement = protectedElement.get();
                protectedThis->updateElementIsImmersive(protectedElement.get(), true);
                completionHandler({ });
            });
        });
    });
}

void DocumentImmersive::exitImmersive(CompletionHandler<void(ExceptionOr<void>)>&& completionHandler)
{
    RefPtr exitingImmersiveElement = immersiveElement();
    if (!exitingImmersiveElement)
        return completionHandler(Exception { ExceptionCode::TypeError, "Not in immersive"_s });

    dismissClientImmersivePresentation(exitingImmersiveElement.get(), [weakElement = WeakPtr { *exitingImmersiveElement }, weakThis = WeakPtr { *this }, completionHandler = WTF::move(completionHandler)]() mutable {
        RefPtr protectedElement = weakElement.get();
        if (!protectedElement)
            return completionHandler(Exception { ExceptionCode::TypeError });

        protectedElement->exitImmersivePresentation([weakElement, weakThis, completionHandler = WTF::move(completionHandler)] () mutable {
            RefPtr protectedThis = weakThis.get();
            RefPtr protectedElement = weakElement.get();
            if (!protectedThis || !protectedElement)
                return completionHandler(Exception { ExceptionCode::TypeError });

            protectedThis->updateElementIsImmersive(protectedElement.get(), false);
            protectedThis->m_immersiveElement = nullptr;
            completionHandler({ });
        });
    });
}

void DocumentImmersive::exitImmersive()
{
    if (!immersiveElement())
        return;

    exitImmersive([weakThis = WeakPtr { *this }](auto result) {
        RefPtr protectedThis = weakThis.get();
        if (!protectedThis)
            return;

        if (result.hasException())
            RELEASE_LOG_ERROR(Immersive, "%p - DocumentImmersive: %s", protectedThis.get(), result.releaseException().message().utf8().data());
    });
}

void DocumentImmersive::exitRemovedImmersiveElement(HTMLModelElement* element, CompletionHandler<void()>&& completionHandler)
{
    ASSERT(element->immersive());

    if (immersiveElement() == element) {
        exitImmersive([completionHandler = WTF::move(completionHandler)] (auto) mutable {
            completionHandler();
        });
    } else {
        element->exitImmersivePresentation([] { });
        updateElementIsImmersive(element, false);
        completionHandler();
    }
}

void DocumentImmersive::updateElementIsImmersive(HTMLModelElement* element, bool isImmersive)
{
    Style::PseudoClassChangeInvalidation styleInvalidation(*element, { { CSSSelector::PseudoClass::Immersive, isImmersive } });
    queueImmersiveEventForElement(DocumentImmersive::EventType::Change, *element);
    document().scheduleRenderingUpdate(RenderingUpdateStep::Immersive);
}

void DocumentImmersive::dismissClientImmersivePresentation(HTMLModelElement* exitingImmersiveElement, CompletionHandler<void()>&& completionHandler)
{
    RefPtr protectedPage = document().page();
    if (!protectedPage)
        return completionHandler();

    protectedPage->chrome().client().dismissImmersiveElement(*exitingImmersiveElement, WTF::move(completionHandler));
}

void DocumentImmersive::dispatchPendingEvents()
{
    auto pendingEvents = std::exchange(m_pendingEvents, { });

    while (!pendingEvents.isEmpty()) {
        auto [eventType, element] = pendingEvents.takeFirst();

        // Let target be element if element is connected and its node document is document, and otherwise let target be document.
        Ref target = [&]() -> Node& {
            if (element->isConnected() && &element->document() == &document())
                return element;
            return document();
        }();

        switch (eventType) {
        case EventType::Change: {
            target->dispatchEvent(Event::create(eventNames().immersivechangeEvent, Event::CanBubble::Yes, Event::IsCancelable::No, Event::IsComposed::Yes));
            break;
        }
        case EventType::Error:
            target->dispatchEvent(Event::create(eventNames().immersiveerrorEvent, Event::CanBubble::Yes, Event::IsCancelable::No, Event::IsComposed::Yes));
            break;
        }
    }
}

void DocumentImmersive::queueImmersiveEventForElement(EventType eventType, Element& target)
{
    m_pendingEvents.append({ eventType, GCReachableRef(target) });
}

void DocumentImmersive::clear()
{
    m_immersiveElement = nullptr;
}

}

#endif
