/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include "config.h"
#include "SocketStreamHandleImpl.h"

#include "PageSupplementJava.h"
#include "SocketStreamError.h"
#include "SocketStreamHandleClient.h"
#include "WKJPlatformJava.h"

namespace WebCore {

SocketStreamHandleImpl::SocketStreamHandleImpl(const URL& url, Page* page,
                                       SocketStreamHandleClient& client, const StorageSessionProvider* provider)
    : SocketStreamHandle(url, client)
    , m_storageSessionProvider(provider)
{
    String host = url.host().toString();
    bool ssl = url.protocolIs("wss"_s);
    int port = url.port().value_or(ssl ? 443 : 80);

    const WKJHostNetwork* cb = wkjNetwork();
    if (!cb || !cb->socket_create)
        return;

    WKJStringArg hostArg(host);
    WKJHandle webPage = PageSupplementJava::from(page)->jWebPage();

    m_ref = WKJHandle(cb->socket_create(hostArg.data(), hostArg.length(), port,
                                        ssl ? 1 : 0, webPage.get(), wkj_from_ptr(this)));
    wkjCheckAndClearException();
}

SocketStreamHandleImpl::~SocketStreamHandleImpl()
{
    // The host table can be gone by the time this runs; that is what the null environment
    // check in the JNI version meant.
    const WKJHostNetwork* cb = wkjNetwork();
    if (!cb || !cb->socket_notify_disposed || !m_ref)
        return;

    cb->socket_notify_disposed(m_ref.get());
    wkjCheckAndClearException();
}

std::optional<size_t> SocketStreamHandleImpl::platformSendInternal(const uint8_t* data, size_t len)
{
    const WKJHostNetwork* cb = wkjNetwork();
    if (!cb || !cb->socket_send)
        return { };

    int32_t res = cb->socket_send(m_ref.get(), data, static_cast<int32_t>(len));

    // The JNI version distinguished "the upcall threw" from a real result here and returned
    // nullopt for the former; keep that split exactly.
    if (wkjCheckAndClearException()) {
        return { };
    }
    return { static_cast<size_t>(res) };
}

void SocketStreamHandleImpl::platformClose()
{
    const WKJHostNetwork* cb = wkjNetwork();
    if (!cb || !cb->socket_close)
        return;

    cb->socket_close(m_ref.get());
    wkjCheckAndClearException();
}

void SocketStreamHandleImpl::didOpen()
{
    if (m_state == Connecting) {
        m_state = Open;
        m_client.didOpenSocketStream(*this);
    }
}

void SocketStreamHandleImpl::didReceiveData(const uint8_t* data, int length)
{
    std::span<const uint8_t> span(data, length);
    m_client.didReceiveSocketStreamData(*this, span);
}

void SocketStreamHandleImpl::didFail(int errorCode, const String& errorDescription)
{
    if (m_state == Open) {
        m_client.didFailSocketStream(
                *this,
                SocketStreamError(errorCode, m_url.string(), errorDescription));
    }
}

void SocketStreamHandleImpl::didClose()
{
    if (m_state == Closed)
        return;
    m_state = Closed;

    m_client.didCloseSocketStream(*this);
}

} // namespace WebCore

extern "C" {

WKJ_EXPORT void wkj_socket_did_open(int64_t handle_peer)
{
    using namespace WebCore;
    SocketStreamHandleImpl* handle =
            static_cast<SocketStreamHandleImpl*>(wkj_to_ptr(handle_peer));
    ASSERT(handle);
    handle->didOpen();
}

WKJ_EXPORT void wkj_socket_did_receive_data(int64_t handle_peer, const uint8_t* data,
                                            int32_t length)
{
    using namespace WebCore;
    SocketStreamHandleImpl* handle =
            static_cast<SocketStreamHandleImpl*>(wkj_to_ptr(handle_peer));
    ASSERT(handle);
    handle->didReceiveData(data, length);
}

WKJ_EXPORT void wkj_socket_did_fail(int64_t handle_peer, int32_t error_code,
                                    const uint16_t* description, int32_t description_len)
{
    using namespace WebCore;
    SocketStreamHandleImpl* handle =
            static_cast<SocketStreamHandleImpl*>(wkj_to_ptr(handle_peer));
    ASSERT(handle);
    handle->didFail(error_code, wkjMakeString(description, description_len));
}

WKJ_EXPORT void wkj_socket_did_close(int64_t handle_peer)
{
    using namespace WebCore;
    SocketStreamHandleImpl* handle =
            static_cast<SocketStreamHandleImpl*>(wkj_to_ptr(handle_peer));
    ASSERT(handle);
    handle->didClose();
}

}
