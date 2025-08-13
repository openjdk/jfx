/*
 * Copyright (C) 2024 Apple Inc. All rights reserved.
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
#include "WorkerWebTransportSession.h"

#include "ScriptExecutionContext.h"
#include "WebTransport.h"
#include "WebTransportBidirectionalStreamConstructionParameters.h"
#include "WritableStreamSink.h"

namespace WebCore {

Ref<WorkerWebTransportSession> WorkerWebTransportSession::create(ScriptExecutionContextIdentifier contextID, WebTransportSessionClient& client)
{
    ASSERT(!RunLoop::isMain());
    return adoptRef(*new WorkerWebTransportSession(contextID, client));
}

WorkerWebTransportSession::~WorkerWebTransportSession() = default;

WorkerWebTransportSession::WorkerWebTransportSession(ScriptExecutionContextIdentifier contextID, WebTransportSessionClient& client)
    : m_contextID(contextID)
    , m_client(client)
{
    ASSERT(!RunLoop::isMain());
}

void WorkerWebTransportSession::attachSession(Ref<WebTransportSession>&& session)
{
    ASSERT(!m_session);
    m_session = WTFMove(session);
}

void WorkerWebTransportSession::receiveDatagram(std::span<const uint8_t> span, bool withFin, std::optional<Exception>&& exception)
{
    ASSERT(RunLoop::isMain());
    ScriptExecutionContext::postTaskTo(m_contextID, [vector = Vector<uint8_t> { span }, withFin, exception = WTFMove(exception), weakClient = m_client] (auto&) mutable {
        RefPtr client = weakClient.get();
        if (!client)
            return;
        client->receiveDatagram(vector.span(), withFin, WTFMove(exception));
    });
}

void WorkerWebTransportSession::networkProcessCrashed()
{
    ASSERT(RunLoop::isMain());
    ScriptExecutionContext::postTaskTo(m_contextID, [weakClient = m_client] (auto&) mutable {
        RefPtr client = weakClient.get();
        if (!client)
            return;
        client->networkProcessCrashed();
    });
}

void WorkerWebTransportSession::receiveIncomingUnidirectionalStream(WebTransportStreamIdentifier identifier)
{
    ASSERT(RunLoop::isMain());
    ScriptExecutionContext::postTaskTo(m_contextID, [identifier, weakClient = m_client] (auto&) mutable {
        RefPtr client = weakClient.get();
        if (!client)
            return;
        client->receiveIncomingUnidirectionalStream(identifier);
    });
}

void WorkerWebTransportSession::receiveBidirectionalStream(WebTransportBidirectionalStreamConstructionParameters&& parameters)
{
    ASSERT(RunLoop::isMain());
    ScriptExecutionContext::postTaskTo(m_contextID, [parameters = WTFMove(parameters), weakClient = m_client] (auto&) mutable {
        RefPtr client = weakClient.get();
        if (!client)
            return;
        client->receiveBidirectionalStream(WTFMove(parameters));
    });
}

void WorkerWebTransportSession::streamReceiveBytes(WebTransportStreamIdentifier identifier, std::span<const uint8_t> data, bool withFin, std::optional<Exception>&& exception)
{
    ASSERT(RunLoop::isMain());
    ScriptExecutionContext::postTaskTo(m_contextID, [identifier, data = Vector<uint8_t> { data }, withFin, exception = WTFMove(exception),  weakClient = m_client] (auto&) mutable {
        RefPtr client = weakClient.get();
        if (!client)
            return;
        client->streamReceiveBytes(identifier, data.span(), withFin, WTFMove(exception));
    });
}

Ref<WebTransportSendPromise> WorkerWebTransportSession::sendDatagram(std::span<const uint8_t> datagram)
{
    ASSERT(!RunLoop::isMain());
    if (RefPtr session = m_session)
        return session->sendDatagram(datagram);
    return WebTransportSendPromise::createAndReject();
}

Ref<WritableStreamPromise> WorkerWebTransportSession::createOutgoingUnidirectionalStream()
{
    ASSERT(!RunLoop::isMain());
    if (RefPtr session = m_session)
        return session->createOutgoingUnidirectionalStream();
    return WritableStreamPromise::createAndReject();
}

Ref<BidirectionalStreamPromise> WorkerWebTransportSession::createBidirectionalStream()
{
    ASSERT(!RunLoop::isMain());
    if (RefPtr session = m_session)
        return session->createBidirectionalStream();
    return BidirectionalStreamPromise::createAndReject();
}

void WorkerWebTransportSession::terminate(WebTransportSessionErrorCode code, CString&& reason)
{
    ASSERT(!RunLoop::isMain());
    if (RefPtr session = m_session)
        session->terminate(code, WTFMove(reason));
}

void WorkerWebTransportSession::cancelReceiveStream(WebTransportStreamIdentifier identifier, std::optional<WebTransportStreamErrorCode> errorCode)
{
    ASSERT(!RunLoop::isMain());
    if (RefPtr session = m_session)
        session->cancelReceiveStream(identifier, errorCode);
}

void WorkerWebTransportSession::cancelSendStream(WebTransportStreamIdentifier identifier, std::optional<WebTransportStreamErrorCode> errorCode)
{
    ASSERT(!RunLoop::isMain());
    if (RefPtr session = m_session)
        session->cancelSendStream(identifier, errorCode);
}

void WorkerWebTransportSession::destroyStream(WebTransportStreamIdentifier identifier, std::optional<WebTransportStreamErrorCode> errorCode)
{
    ASSERT(!RunLoop::isMain());
    if (RefPtr session = m_session)
        session->destroyStream(identifier, errorCode);
}

}
