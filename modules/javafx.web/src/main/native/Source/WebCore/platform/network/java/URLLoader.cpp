/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "FrameNetworkingContext.h"
#include "HTTPParsers.h"
#include "MIMETypeRegistry.h"
#include "NetworkingContext.h"
#include "Page.h"
#include "PageSupplementJava.h"
#include "PlatformJavaClasses.h"
#include "ResourceError.h"
#include "ResourceHandle.h"
#include "ResourceHandleClient.h"
#include "ResourceRequest.h"
#include "ResourceResponse.h"
#include "URLLoader.h"
#include "com_sun_webkit_LoadListenerClient.h"
#include "com_sun_webkit_network_URLLoaderBase.h"
#include <wtf/CompletionHandler.h>

namespace WebCore {
class Page;
}

namespace WebCore {

namespace URLLoaderJavaInternal {

static JGClass networkContextClass;
static jmethodID loadMethod;

static JGClass urlLoaderClass;
static jmethodID cancelMethod;

static JGClass formDataElementClass;
static jmethodID createFromFileMethod;
static jmethodID createFromByteArrayMethod;

static void initRefs(JNIEnv* env)
{
    if (!networkContextClass) {
        networkContextClass = JLClass(env->FindClass(
                "com/sun/webkit/network/NetworkContext"));
        ASSERT(networkContextClass);

        loadMethod = env->GetStaticMethodID(
                networkContextClass,
                "fwkLoad",
                "(Lcom/sun/webkit/WebPage;Z"
                "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;"
                "[Lcom/sun/webkit/network/FormDataElement;J)"
                "Lcom/sun/webkit/network/URLLoaderBase;");
        ASSERT(loadMethod);
    }
    if (!urlLoaderClass) {
        urlLoaderClass = JLClass(env->FindClass(
                "com/sun/webkit/network/URLLoaderBase"));
        ASSERT(urlLoaderClass);

        cancelMethod = env->GetMethodID(urlLoaderClass, "fwkCancel", "()V");
        ASSERT(cancelMethod);
    }
    if (!formDataElementClass) {
        formDataElementClass = JLClass(env->FindClass(
                "com/sun/webkit/network/FormDataElement"));
        ASSERT(formDataElementClass);

        createFromByteArrayMethod = env->GetStaticMethodID(
                formDataElementClass,
                "fwkCreateFromByteArray",
                "([B)Lcom/sun/webkit/network/FormDataElement;");
        ASSERT(createFromByteArrayMethod);

        createFromFileMethod = env->GetStaticMethodID(
                formDataElementClass,
                "fwkCreateFromFile",
                "(Ljava/lang/String;)"
                "Lcom/sun/webkit/network/FormDataElement;");
        ASSERT(createFromFileMethod);
    }
}

}

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
    result->m_ref = load(
            true,
            context,
            request,
            result->m_target.get());
    return result;
}

void URLLoader::cancel()
{
    using namespace URLLoaderJavaInternal;
    if (m_ref) {
        JNIEnv* env = WTF::GetJavaEnv();
        initRefs(env);

        env->CallVoidMethod(m_ref, cancelMethod);
        WTF::CheckAndClearException(env);

        m_ref.clear();
    }
}

void URLLoader::loadSynchronously(NetworkingContext* context,
                                  const ResourceRequest& request,
                                  ResourceError& error,
                                  ResourceResponse& response,
                                  Vector<char>& data)
{
    SynchronousTarget target(request, error, response, data);
    load(false, context, request, &target);
}

JLObject URLLoader::load(bool asynchronous,
                         NetworkingContext* context,
                         const ResourceRequest& request,
                         Target* target)
{
    using namespace URLLoaderJavaInternal;
    if (!context) {
        return nullptr;
    }

    auto pageSupplement = context->isValid() ?
        PageSupplementJava::from(static_cast<FrameNetworkingContext*>(context)->frame()) : nullptr;
    if (!pageSupplement) {
        // If NetworkingContext is invalid then we are no longer attached
        // to a Page. This must be an attempt to load from an unload handler,
        // so let's just block it.
        return nullptr;
    }

    JLObject webPage = pageSupplement->jWebPage();
    ASSERT(webPage);

    String headerString;
    for (const auto& header : request.httpHeaderFields()) {
        headerString.append(header.key);
        headerString.append(": ");
        headerString.append(header.value);
        headerString.append("\n");
    }

    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    JLObject loader = env->CallStaticObjectMethod(
            networkContextClass,
            loadMethod,
            (jobject) webPage,
            bool_to_jbool(asynchronous),
            (jstring) request.url().string().toJavaString(env),
            (jstring) request.httpMethod().toJavaString(env),
            (jstring) headerString.toJavaString(env),
            (jobjectArray) toJava(request.httpBody()),
            ptr_to_jlong(target));
    WTF::CheckAndClearException(env);

    return loader;
}

JLObjectArray URLLoader::toJava(const FormData* formData)
{
    using namespace URLLoaderJavaInternal;
    if (!formData) {
        return nullptr;
    }

    const Vector<FormDataElement>& elements = formData->elements();
    size_t size = elements.size();
    if (size == 0) {
        return nullptr;
    }

    JNIEnv* env = WTF::GetJavaEnv();
    initRefs(env);

    JLObjectArray result = env->NewObjectArray(
            size,
            formDataElementClass,
            nullptr);
    for (size_t i = 0; i < size; i++) {
        JLObject resultElement;
        WTF::switchOn(elements[i].data,
            [&] (const Vector<char>& data) -> void {
                JLByteArray byteArray = env->NewByteArray(data.size());
                env->SetByteArrayRegion(
                        (jbyteArray) byteArray,
                        (jsize) 0,
                        (jsize) data.size(),
                        (const jbyte*) data.data());
                resultElement = env->CallStaticObjectMethod(
                        formDataElementClass,
                        createFromByteArrayMethod,
                        (jbyteArray) byteArray);
            },
            [&] (const FormDataElement::EncodedFileData& data) -> void {
                resultElement = env->CallStaticObjectMethod(
                        formDataElementClass,
                        createFromFileMethod,
                        (jstring) data.filename.toJavaString(env));
            },
            [&] (const FormDataElement::EncodedBlobData& data) -> void {
                resultElement = env->CallStaticObjectMethod(
                        formDataElementClass,
                        createFromFileMethod,
                        (jstring) data.url.string().toJavaString(env));
            }
        );
        env->SetObjectArrayElement(
                (jobjectArray) result,
                i,
                (jobject) resultElement);
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

void URLLoader::AsynchronousTarget::didReceiveData(const char* data, int length)
{
    ResourceHandleClient* client = m_handle->client();
    if (client) {
        client->didReceiveData(m_handle, data, length, 0);
    }
}

void URLLoader::AsynchronousTarget::didFinishLoading()
{
    ResourceHandleClient* client = m_handle->client();
    if (client) {
        client->didFinishLoading(m_handle);
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
                                                Vector<char>& data)
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
                "Illegal redirect"));
        return false;
    }
    return true;
}

void URLLoader::SynchronousTarget::didReceiveResponse(
        const ResourceResponse& response)
{
    m_response = response;
}

void URLLoader::SynchronousTarget::didReceiveData(const char* data, int length)
{
    m_data.append(data, length);
}

void URLLoader::SynchronousTarget::didFinishLoading()
{
}

void URLLoader::SynchronousTarget::didFail(const ResourceError& error)
{
    m_error = error;
    m_response.setHTTPStatusCode(404);
}

} // namespace WebCore

static WebCore::ResourceResponse setupResponse(JNIEnv* env,
                          jint status,
                          jstring contentType,
                          jstring contentEncoding,
                          jlong contentLength,
                          jstring headers,
                          jstring url)
{
    using namespace WebCore;
    ResourceResponse response { };

    if (status > 0) {
        response.setHTTPStatusCode(status);
    }

    // Fix for RT-13802: If the mime type is not specified,
    // set the mime type to "text/html" as e.g. the CF port
    // does
    String contentTypeString(env, contentType);
    if (contentTypeString.isEmpty()) {
        contentTypeString = "text/html";
    }
    if (!contentTypeString.isEmpty()) {
        response.setMimeType(
                extractMIMETypeFromMediaType(contentTypeString).convertToLowercaseWithoutLocale());
    }

    String contentEncodingString(env, contentEncoding);
    if (contentEncodingString.isEmpty() && !contentTypeString.isEmpty()) {
        contentEncodingString = extractCharsetFromMediaType(contentTypeString);
    }
    if (!contentEncodingString.isEmpty()) {
        response.setTextEncodingName(contentEncodingString);
    }

    if (contentLength > 0) {
        response.setExpectedContentLength(
                static_cast<long long>(contentLength));
    }

    String headersString(env, headers);
    int splitPos = headersString.find("\n");
    while (splitPos != -1) {
        String s = headersString.left(splitPos);
        int j = s.find(":");
        if (j != -1) {
            String key = s.left(j);
            String val = s.substring(j + 1);
            response.setHTTPHeaderField(key, val);
        }
        headersString = headersString.substring(splitPos + 1);
        splitPos = headersString.find("\n");
    }

    URL kurl = URL(URL(), String(env, url));
    response.setURL(kurl);

    // Setup mime type for local resources
    if (/*kurl.hasPath()*/kurl.pathEnd() != kurl.pathStart() && kurl.protocol() == String("file")) {
        response.setMimeType(MIMETypeRegistry::mimeTypeForPath(kurl.path().toString()));
    }
    return response;
}

JNIEXPORT void JNICALL Java_com_sun_webkit_network_URLLoaderBase_twkDidSendData
  (JNIEnv*, jclass, jlong totalBytesSent, jlong totalBytesToBeSent, jlong data)
{
    using namespace WebCore;
    URLLoader::Target* target =
            static_cast<URLLoader::Target*>(jlong_to_ptr(data));
    ASSERT(target);
    target->didSendData(totalBytesSent, totalBytesToBeSent);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_network_URLLoaderBase_twkWillSendRequest
  (JNIEnv* env, jclass, jint status,
   jstring contentType, jstring contentEncoding, jlong contentLength,
   jstring headers, jstring url, jlong data)
{
    using namespace WebCore;
    URLLoader::Target* target =
            static_cast<URLLoader::Target*>(jlong_to_ptr(data));
    ASSERT(target);

    ResourceResponse response = setupResponse(
            env,
            status,
            contentType,
            contentEncoding,
            contentLength,
            headers,
            url);

    target->willSendRequest(response);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_network_URLLoaderBase_twkDidReceiveResponse
  (JNIEnv* env, jclass, jint status, jstring contentType,
   jstring contentEncoding, jlong contentLength, jstring headers,
   jstring url, jlong data)
{
    using namespace WebCore;
    URLLoader::Target* target =
            static_cast<URLLoader::Target*>(jlong_to_ptr(data));
    ASSERT(target);

    ResourceResponse response = setupResponse(
            env,
            status,
            contentType,
            contentEncoding,
            contentLength,
            headers,
            url);

    target->didReceiveResponse(response);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_network_URLLoaderBase_twkDidReceiveData
  (JNIEnv* env, jclass, jobject byteBuffer, jint position, jint remaining,
   jlong data)
{
    using namespace WebCore;
    URLLoader::Target* target =
            static_cast<URLLoader::Target*>(jlong_to_ptr(data));
    ASSERT(target);
    const char* address =
            static_cast<const char*>(env->GetDirectBufferAddress(byteBuffer));
    target->didReceiveData(address + position, remaining);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_network_URLLoaderBase_twkDidFinishLoading
  (JNIEnv*, jclass, jlong data)
{
    using namespace WebCore;
    URLLoader::Target* target =
            static_cast<URLLoader::Target*>(jlong_to_ptr(data));
    ASSERT(target);
    target->didFinishLoading();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_network_URLLoaderBase_twkDidFail
  (JNIEnv* env, jclass, jint errorCode, jstring url, jstring message,
   jlong data)
{
    using namespace WebCore;
    URLLoader::Target* target =
            static_cast<URLLoader::Target*>(jlong_to_ptr(data));
    ASSERT(target);
    target->didFail(ResourceError(
            String(),
            errorCode,
            URL(env, url),
            String(env, message)));
}
