/*
 * Copyright (C) 2011 Google Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
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
#include "ThreadableWebSocketChannelClientWrapper.h"

#include "ScriptExecutionContext.h"
#include "WebSocketChannelClient.h"
#include <wtf/RefPtr.h>
#include <wtf/text/StringView.h>

namespace WebCore {

ThreadableWebSocketChannelClientWrapper::ThreadableWebSocketChannelClientWrapper(ScriptExecutionContext& context, WebSocketChannelClient& client)
    : m_context(context)
    , m_client(client)
    , m_failedWebSocketChannelCreation(false)
    , m_suspended(false)
{
}

Ref<ThreadableWebSocketChannelClientWrapper> ThreadableWebSocketChannelClientWrapper::create(ScriptExecutionContext& context, WebSocketChannelClient& client)
{
    return adoptRef(*new ThreadableWebSocketChannelClientWrapper(context, client));
}

WorkerThreadableWebSocketChannel::Peer* ThreadableWebSocketChannelClientWrapper::peer() const
{
    return m_peer.get();
}

void ThreadableWebSocketChannelClientWrapper::didCreateWebSocketChannel(Ref<WorkerThreadableWebSocketChannel::Peer>&& peer)
{
    m_peer = WTFMove(peer);
}

void ThreadableWebSocketChannelClientWrapper::clearPeer()
{
    m_peer = nullptr;
}

bool ThreadableWebSocketChannelClientWrapper::failedWebSocketChannelCreation() const
{
    return m_failedWebSocketChannelCreation;
}

void ThreadableWebSocketChannelClientWrapper::setFailedWebSocketChannelCreation()
{
    m_failedWebSocketChannelCreation = true;
}

String ThreadableWebSocketChannelClientWrapper::subprotocol() const
{
    if (m_subprotocol.isEmpty())
        return emptyString();
    return String(m_subprotocol);
}

void ThreadableWebSocketChannelClientWrapper::setSubprotocol(const String& subprotocol)
{
    unsigned length = subprotocol.length();
    m_subprotocol.resize(length);
    StringView(subprotocol).getCharacters(m_subprotocol.mutableSpan());
}

String ThreadableWebSocketChannelClientWrapper::extensions() const
{
    if (m_extensions.isEmpty())
        return emptyString();
    return String(m_extensions);
}

void ThreadableWebSocketChannelClientWrapper::setExtensions(const String& extensions)
{
    unsigned length = extensions.length();
    m_extensions.resize(length);
    StringView(extensions).getCharacters(m_extensions.mutableSpan());
}

void ThreadableWebSocketChannelClientWrapper::clearClient()
{
    m_client = nullptr;
}

void ThreadableWebSocketChannelClientWrapper::didConnect()
{
    m_pendingTasks.append(makeUnique<ScriptExecutionContext::Task>([this, protectedThis = Ref { *this }] (ScriptExecutionContext&) {
        if (RefPtr client = m_client.get())
            client->didConnect();
    }));

    if (!m_suspended)
        processPendingTasks();
}

void ThreadableWebSocketChannelClientWrapper::didReceiveMessage(String&& message)
{
    m_pendingTasks.append(makeUnique<ScriptExecutionContext::Task>([this, protectedThis = Ref { *this }, message = WTFMove(message).isolatedCopy()] (ScriptExecutionContext&) mutable {
        if (RefPtr client = m_client.get())
            client->didReceiveMessage(WTFMove(message));
    }));

    if (!m_suspended)
        processPendingTasks();
}

void ThreadableWebSocketChannelClientWrapper::didReceiveBinaryData(Vector<uint8_t>&& binaryData)
{
    m_pendingTasks.append(makeUnique<ScriptExecutionContext::Task>([this, protectedThis = Ref { *this }, binaryData = WTFMove(binaryData)] (ScriptExecutionContext&) mutable {
        if (RefPtr client = m_client.get())
            client->didReceiveBinaryData(WTFMove(binaryData));
    }));

    if (!m_suspended)
        processPendingTasks();
}

void ThreadableWebSocketChannelClientWrapper::didUpdateBufferedAmount(unsigned bufferedAmount)
{
    m_pendingTasks.append(makeUnique<ScriptExecutionContext::Task>([this, protectedThis = Ref { *this }, bufferedAmount] (ScriptExecutionContext&) {
        if (RefPtr client = m_client.get())
            client->didUpdateBufferedAmount(bufferedAmount);
    }));

    if (!m_suspended)
        processPendingTasks();
}

void ThreadableWebSocketChannelClientWrapper::didStartClosingHandshake()
{
    m_pendingTasks.append(makeUnique<ScriptExecutionContext::Task>([this, protectedThis = Ref { *this }] (ScriptExecutionContext&) {
        if (RefPtr client = m_client.get())
            client->didStartClosingHandshake();
    }));

    if (!m_suspended)
        processPendingTasks();
}

void ThreadableWebSocketChannelClientWrapper::didClose(unsigned unhandledBufferedAmount, WebSocketChannelClient::ClosingHandshakeCompletionStatus closingHandshakeCompletion, unsigned short code, const String& reason)
{
    m_pendingTasks.append(makeUnique<ScriptExecutionContext::Task>([this, protectedThis = Ref { *this }, unhandledBufferedAmount, closingHandshakeCompletion, code, reason = reason.isolatedCopy()] (ScriptExecutionContext&) {
        if (RefPtr client = m_client.get())
            client->didClose(unhandledBufferedAmount, closingHandshakeCompletion, code, reason);
        }));

    if (!m_suspended)
        processPendingTasks();
}

void ThreadableWebSocketChannelClientWrapper::didReceiveMessageError(String&& reason)
{
    m_pendingTasks.append(makeUnique<ScriptExecutionContext::Task>([this, protectedThis = Ref { *this }, reason = WTFMove(reason).isolatedCopy()] (ScriptExecutionContext&) mutable {
        if (RefPtr client = m_client.get())
            client->didReceiveMessageError(WTFMove(reason));
    }));

    if (!m_suspended)
        processPendingTasks();
}

void ThreadableWebSocketChannelClientWrapper::didUpgradeURL()
{
    m_pendingTasks.append(makeUnique<ScriptExecutionContext::Task>([this, protectedThis = Ref { *this }] (ScriptExecutionContext&) {
        if (RefPtr client = m_client.get())
            client->didUpgradeURL();
    }));

    if (!m_suspended)
        processPendingTasks();
}

void ThreadableWebSocketChannelClientWrapper::suspend()
{
    m_suspended = true;
}

void ThreadableWebSocketChannelClientWrapper::resume()
{
    m_suspended = false;
    processPendingTasks();
}

void ThreadableWebSocketChannelClientWrapper::processPendingTasks()
{
    if (m_suspended)
        return;

    Vector<std::unique_ptr<ScriptExecutionContext::Task>> pendingTasks = WTFMove(m_pendingTasks);
    Ref protectedContext = { *m_context };
    for (auto& task : pendingTasks)
        task->performTask(protectedContext.get());
}

} // namespace WebCore
