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

#if COMPILER(GCC)
#pragma GCC diagnostic ignored "-Wunused-parameter"
#endif

#include <wkj_constants.h>

#include "FrameNetworkingContext.h"
#include "HTTPParsers.h"
#include "MIMETypeRegistry.h"
#include "NetworkingContext.h"
#include "Page.h"
#include "PageSupplementJava.h"
#include "ResourceError.h"
#include "ResourceHandle.h"
#include "ResourceHandleClient.h"
#include "ResourceRequest.h"
#include "ResourceResponse.h"
#include "SharedBuffer.h"
#include "URLLoader.h"
#include "NetworkLoadMetrics.h"
#include "WKJPlatformJava.h"
#include <wtf/CompletionHandler.h>

namespace WebCore {
class Page;
}

namespace WebCore {

URLLoader::URLLoader()
{
}

URLLoader::~URLLoader()
{
    cancel();
}

std::unique_ptr<URLLoader> URLLoader::loadAsynchronously(NetworkingContext* context,
                                                    ResourceHandle* handle,
                                                    const ResourceRequest& request)
{
    std::unique_ptr<URLLoader> result = std::unique_ptr<URLLoader>(new URLLoader());
    result->m_target = std::unique_ptr<AsynchronousTarget>(new AsynchronousTarget(handle));
    result->m_ref = WKJHandle(load(
            true,
            context,
            request,
            result->m_target.get()));
    return result;
}

void URLLoader::cancel()
{
    if (m_ref) {
        const WKJHostNetwork* cb = wkjNetwork();
        if (cb && cb->url_loader_cancel) {
            cb->url_loader_cancel(m_ref.get());
            wkjCheckAndClearException();
        }

        m_ref.clear();
    }
}

void URLLoader::loadSynchronously(NetworkingContext* context,
                                  const ResourceRequest& request,
                                  ResourceError& error,
                                  ResourceResponse& response,
                                  Vector<uint8_t>& data)
{
    SynchronousTarget target(request, error, response, data);

    // The loader the synchronous path creates is dropped on the floor here exactly as it was
    // before: fwkLoad returned an object the JNI code never stored. Releasing the id keeps
    // the registry from growing, which the JNI local ref did for free.
    WKJHandle loader { load(false, context, request, &target) };
}

/*
 * Returns a NEW id for the Java URLLoaderBase, owned by the caller.
 */
wkj_ref URLLoader::load(bool asynchronous,
                        NetworkingContext* context,
                        const ResourceRequest& request,
                        Target* target)
{
    if (!context) {
        return 0;
    }

    auto pageSupplement = context->isValid() ?
        PageSupplementJava::from(static_cast<FrameNetworkingContext*>(context)->frame()) : nullptr;
    if (!pageSupplement) {
        // If NetworkingContext is invalid then we are no longer attached
        // to a Page. This must be an attempt to load from an unload handler,
        // so let's just block it.
        return 0;
    }

    WKJHandle webPage = pageSupplement->jWebPage();
    ASSERT(webPage);

    String headerString;
    for (const auto& header : request.httpHeaderFields()) {
        headerString = makeString(headerString, header.key, WTF::String::fromUTF8(": "), header.value, WTF::String::fromUTF8("\n"));
       /* headerString.append(header.key);
        headerString.append(": ");
        headerString.append(header.value);
        headerString.append("\n");*/
    }

    const WKJHostNetwork* cb = wkjNetwork();
    if (!cb || !cb->url_loader_load)
        return 0;

    // The FormDataElement[] is built first and released after the call, which is the scope
    // the JNI local refs to the array and its elements had.
    Vector<WKJHandle> elements = toJava(request.httpBody().get());
    Vector<wkj_ref> elementIds(elements.size());
    for (size_t i = 0; i < elements.size(); ++i)
        elementIds[i] = elements[i].get();

    WKJStringArg url(request.url().string());
    WKJStringArg method(request.httpMethod());
    WKJStringArg headers(headerString);

    wkj_ref loader = cb->url_loader_load(webPage.get(), asynchronous ? 1 : 0,
                                         url.data(), url.length(),
                                         method.data(), method.length(),
                                         headers.data(), headers.length(),
                                         elementIds.isEmpty() ? nullptr : elementIds.span().data(),
                                         static_cast<int32_t>(elementIds.size()),
                                         wkj_from_ptr(target));
    wkjCheckAndClearException();

    return loader;
}

/*
 * The FormDataElement[] the JNI version built with NewObjectArray. Each handle owns one id
 * and releases it when the vector goes away, which is what the local refs did at the end of
 * the native frame.
 */
Vector<WKJHandle> URLLoader::toJava(const FormData* formData)
{
    if (!formData) {
        return { };
    }

    const Vector<FormDataElement>& elements = formData->elements();
    size_t size = elements.size();
    if (size == 0) {
        return { };
    }

    const WKJHostNetwork* cb = wkjNetwork();
    if (!cb)
        return { };

    Vector<WKJHandle> result;
    result.reserveInitialCapacity(size);

    for (size_t i = 0; i < size; i++) {
        WKJHandle resultElement;
        WTF::switchOn(elements[i].data,
            [&] (const Vector<uint8_t>& data) -> void {
                if (!cb->form_data_create_from_bytes)
                    return;
                resultElement = WKJHandle(cb->form_data_create_from_bytes(
                        data.span().data(), static_cast<int32_t>(data.size())));
            },
            [&] (const FormDataElement::EncodedFileData& data) -> void {
                if (!cb->form_data_create_from_file)
                    return;
                WKJStringArg filename(data.filename);
                resultElement = WKJHandle(cb->form_data_create_from_file(
                        filename.data(), filename.length()));
            },
            [&] (const FormDataElement::EncodedBlobData& data) -> void {
                if (!cb->form_data_create_from_file)
                    return;
                WKJStringArg blobURL(data.url.string());
                resultElement = WKJHandle(cb->form_data_create_from_file(
                        blobURL.data(), blobURL.length()));
            }
        );
        result.append(WTF::move(resultElement));
    }

    return result;
}

URLLoader::Target::~Target()
{
}

URLLoader::AsynchronousTarget::AsynchronousTarget(ResourceHandle* handle)
    : m_handle(handle)
{
}

void URLLoader::AsynchronousTarget::didSendData(long totalBytesSent,
                                                long totalBytesToBeSent)
{
    ResourceHandleClient* client = m_handle->client();
    if (client) {
        client->didSendData(m_handle, totalBytesSent, totalBytesToBeSent);
    }
}


bool URLLoader::AsynchronousTarget::willSendRequest(const ResourceResponse& response)
{
    m_handle->willSendRequest(response);
    return false;
}

void URLLoader::AsynchronousTarget::didReceiveResponse(
        const ResourceResponse& response)
{
    ResourceHandleClient* client = m_handle->client();
    if (client) {
        client->didReceiveResponseAsync(m_handle, ResourceResponse(response), [] () {});
    }
}

void URLLoader::AsynchronousTarget::didReceiveData(const SharedBuffer* data, int length)
{
    ResourceHandleClient* client = m_handle->client();
    if (client) {
        client->didReceiveData(m_handle, *data, length);
    }
}

void URLLoader::AsynchronousTarget::didFinishLoading()
{
    ResourceHandleClient* client = m_handle->client();
    if (client) {
        client->didFinishLoading(m_handle, {});
    }
}

void URLLoader::AsynchronousTarget::didFail(const ResourceError& error)
{
    ResourceHandleClient* client = m_handle->client();
    if (client) {
        client->didFail(m_handle, error);
    }
}

URLLoader::SynchronousTarget::SynchronousTarget(const ResourceRequest& request,
                                                ResourceError& error,
                                                ResourceResponse& response,
                                                Vector<uint8_t>& data)
    : m_request(request)
    , m_error(error)
    , m_response(response)
    , m_data(data)
{
    m_error = ResourceError();
}

void URLLoader::SynchronousTarget::didSendData(long, long)
{
}

bool URLLoader::SynchronousTarget::willSendRequest(const ResourceResponse& response)
{
    // The following code was adapted from the Windows port
    // FIXME: This needs to be fixed to follow redirects correctly even
    // for cross-domain requests
    String location = response.httpHeaderField(HTTPHeaderName::Location);
    URL newURL = URL(response.url(), location);
    if (!protocolHostAndPortAreEqual(m_request.url(), newURL)) {
        didFail(ResourceError(
                String(),
                com_sun_webkit_LoadListenerClient_INVALID_RESPONSE,
                m_request.url(),
                "Illegal redirect"_s));
        return false;
    }
    return true;
}

void URLLoader::SynchronousTarget::didReceiveResponse(
        const ResourceResponse& response)
{
    m_response = response;
}

void URLLoader::SynchronousTarget::didReceiveData(const SharedBuffer* data, int length)
{
    m_data.append(data->span());
}

void URLLoader::SynchronousTarget::didFinishLoading()
{
}

void URLLoader::SynchronousTarget::didFail(const ResourceError& error)
{
    m_error = error;
    m_response.setHTTPStatusCode(404);
}

namespace {

/*
 * Replaces the six-argument response marshalling of twkWillSendRequest and
 * twkDidReceiveResponse, unchanged apart from taking (pointer, length) pairs.
 */
WebCore::ResourceResponse setupResponse(int32_t status,
                          const uint16_t* contentType, int32_t contentTypeLen,
                          const uint16_t* contentEncoding, int32_t contentEncodingLen,
                          int64_t contentLength,
                          const uint16_t* headers, int32_t headersLen,
                          const uint16_t* url, int32_t urlLen)
{
    using namespace WebCore;
    ResourceResponse response { };

    if (status > 0) {
        response.setHTTPStatusCode(status);
    }

    // Fix for JDK-8113134: If the mime type is not specified,
    // set the mime type to "text/html" as e.g. the CF port
    // does
    String contentTypeString = wkjMakeString(contentType, contentTypeLen);
    if (contentTypeString.isEmpty()) {
        contentTypeString = "text/html"_s;
    }
    if (!contentTypeString.isEmpty()) {
        response.setMimeType(extractMIMETypeFromMediaType(contentTypeString).convertToASCIILowercase());
    }

    String contentEncodingString = wkjMakeString(contentEncoding, contentEncodingLen);
    if (contentEncodingString.isEmpty() && !contentTypeString.isEmpty()) {
        contentEncodingString = extractCharsetFromMediaType(contentTypeString).toString();
    }
    if (!contentEncodingString.isEmpty()) {
        response.setTextEncodingName(WTF::move(contentEncodingString));
    }

    if (contentLength > 0) {
        response.setExpectedContentLength(
                static_cast<long long>(contentLength));
    }

    String headersString = wkjMakeString(headers, headersLen);
    int splitPos = headersString.find("\n"_s);
    while (splitPos != -1) {
        String s = headersString.left(splitPos);
        int j = s.find(":"_s);
        if (j != -1) {
            String key = s.left(j);
            String val = s.substring(j + 1);
            response.setHTTPHeaderField(key, val);
        }
        headersString = headersString.substring(splitPos + 1);
        splitPos = headersString.find("\n"_s);
    }

    URL kurl = URL(URL(), wkjMakeString(url, urlLen));

    // Setup mime type for local resources
    if (/*kurl.hasPath()*/kurl.pathEnd() != kurl.pathStart() && kurl.protocol() == String("file"_s)) {
        response.setMimeType(MIMETypeRegistry::mimeTypeForPath(kurl.path().toString()));
    }
    // set response after protocol check
    response.setURL(WTF::move(kurl));
    return response;
}

}

} // namespace WebCore

extern "C" {

WKJ_EXPORT void wkj_url_loader_did_send_data(int64_t target_peer, int64_t total_bytes_sent,
                                             int64_t total_bytes_to_be_sent)
{
    using namespace WebCore;
    URLLoader::Target* target =
            static_cast<URLLoader::Target*>(wkj_to_ptr(target_peer));
    ASSERT(target);
    target->didSendData(static_cast<long>(total_bytes_sent),
                        static_cast<long>(total_bytes_to_be_sent));
}

WKJ_EXPORT void wkj_url_loader_will_send_request(int64_t target_peer, int32_t status,
                                                 const uint16_t* content_type,
                                                 int32_t content_type_len,
                                                 const uint16_t* content_encoding,
                                                 int32_t content_encoding_len,
                                                 int64_t content_length,
                                                 const uint16_t* headers, int32_t headers_len,
                                                 const uint16_t* url, int32_t url_len)
{
    using namespace WebCore;
    URLLoader::Target* target =
            static_cast<URLLoader::Target*>(wkj_to_ptr(target_peer));
    ASSERT(target);

    ResourceResponse response = setupResponse(
            status,
            content_type, content_type_len,
            content_encoding, content_encoding_len,
            content_length,
            headers, headers_len,
            url, url_len);

    target->willSendRequest(response);
}

WKJ_EXPORT void wkj_url_loader_did_receive_response(int64_t target_peer, int32_t status,
                                                    const uint16_t* content_type,
                                                    int32_t content_type_len,
                                                    const uint16_t* content_encoding,
                                                    int32_t content_encoding_len,
                                                    int64_t content_length,
                                                    const uint16_t* headers,
                                                    int32_t headers_len,
                                                    const uint16_t* url, int32_t url_len)
{
    using namespace WebCore;
    URLLoader::Target* target =
            static_cast<URLLoader::Target*>(wkj_to_ptr(target_peer));
    ASSERT(target);

    ResourceResponse response = setupResponse(
            status,
            content_type, content_type_len,
            content_encoding, content_encoding_len,
            content_length,
            headers, headers_len,
            url, url_len);

    target->didReceiveResponse(response);
}

WKJ_EXPORT void wkj_url_loader_did_receive_data(int64_t target_peer, const uint8_t* data,
                                                int32_t position, int32_t remaining)
{
    using namespace WebCore;
    URLLoader::Target* target =
            static_cast<URLLoader::Target*>(wkj_to_ptr(target_peer));
    ASSERT(target);
    // `data` is what GetDirectBufferAddress returned; `position` and `remaining` are
    // unchanged. NOTE, pre-existing and deliberately left alone: the pointer arithmetic below
    // advances a SharedBuffer* by `position` OBJECTS, not by `position` bytes, and the buffer
    // is built from `data` rather than from `data + position`. It is only harmless because
    // URLLoader.java has always passed a buffer whose position is 0. See the migration report.
    Ref<SharedBuffer> tmp_buf = SharedBuffer::create(std::span<const uint8_t>(data, remaining));
    target->didReceiveData(tmp_buf->makeContiguous().ptr() + position, remaining);
    //target->didReceiveData((SharedBuffer*)(address) + position, remaining);
}

WKJ_EXPORT void wkj_url_loader_did_finish_loading(int64_t target_peer)
{
    using namespace WebCore;
    URLLoader::Target* target =
            static_cast<URLLoader::Target*>(wkj_to_ptr(target_peer));
    ASSERT(target);
    target->didFinishLoading();
}

WKJ_EXPORT void wkj_url_loader_did_fail(int64_t target_peer, int32_t error_code,
                                        const uint16_t* url, int32_t url_len,
                                        const uint16_t* message, int32_t message_len)
{
    using namespace WebCore;
    URLLoader::Target* target =
            static_cast<URLLoader::Target*>(wkj_to_ptr(target_peer));
    ASSERT(target);
    target->didFail(ResourceError(
            String(),
            error_code,
            URL(URL(), wkjMakeString(url, url_len)),
            wkjMakeString(message, message_len)));
}

} // extern "C"
