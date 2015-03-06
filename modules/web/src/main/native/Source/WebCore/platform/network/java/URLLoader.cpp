/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "URLLoader.h"

#include "FrameNetworkingContextJava.h"
#include "HTTPParsers.h"
#include "JavaEnv.h"
#include "MIMETypeRegistry.h"
#include "ResourceError.h"
#include "ResourceHandle.h"
#include "ResourceRequest.h"
#include "ResourceResponse.h"
#include "ResourceHandleClient.h"
#include "WebPage.h"
#include "com_sun_webkit_LoadListenerClient.h"
#include "com_sun_webkit_network_URLLoader.h"

namespace WebCore {
class Page;
}

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
                "Lcom/sun/webkit/network/URLLoader;");
        ASSERT(loadMethod);
    }
    if (!urlLoaderClass) {
        urlLoaderClass = JLClass(env->FindClass(
                "com/sun/webkit/network/URLLoader"));
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

namespace WebCore {

URLLoader::URLLoader()
{
}

URLLoader::~URLLoader()
{
    cancel();
}

PassOwnPtr<URLLoader> URLLoader::loadAsynchronously(NetworkingContext* context,
                                                    ResourceHandle* handle)
{
    OwnPtr<URLLoader> result = adoptPtr(new URLLoader());
    result->m_target = adoptPtr(new AsynchronousTarget(handle));
    result->m_ref = load(
            true,
            context,
            handle->firstRequest(),
            result->m_target.get());
    return result->m_ref ? result.release() : PassOwnPtr<URLLoader>();
}

void URLLoader::cancel()
{
    if (m_ref) {
        JNIEnv* env = WebCore_GetJavaEnv();
        initRefs(env);

        env->CallVoidMethod(m_ref, cancelMethod);
        CheckAndClearException(env);

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
    if (!context) {
        return NULL;
    }

    if (!context->isValid()) {
        // If NetworkingContext is invalid then we are no longer attached
        // to a Page. This must be an attempt to load from an unload handler,
        // so let's just block it.
        return NULL;
    }

    Page* page = static_cast<FrameNetworkingContextJava*>(context)->page();
    ASSERT(page);

    JLObject webPage = WebPage::jobjectFromPage(page);
    ASSERT(webPage);

    String headerString;
    const HTTPHeaderMap& headerMap = request.httpHeaderFields();
    for (
        HTTPHeaderMap::const_iterator it = headerMap.begin();
        headerMap.end() != it;
        ++it)
    {
        headerString.append(it->key);
        headerString.append(": ");
        headerString.append(it->value);
        headerString.append("\n");
    }

    JNIEnv* env = WebCore_GetJavaEnv();
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
    CheckAndClearException(env);

    return loader;
}

JLObjectArray URLLoader::toJava(const FormData* formData)
{
    if (!formData) {
        return NULL;
    }

    const Vector<FormDataElement>& elements = formData->elements();
    size_t size = elements.size();
    if (size == 0) {
        return NULL;
    }

    JNIEnv* env = WebCore_GetJavaEnv();
    initRefs(env);

    JLObjectArray result = env->NewObjectArray(
            size,
            formDataElementClass,
            NULL);
    for (size_t i = 0; i < size; i++) {
        JLObject resultElement;
        if (elements[i].m_type == FormDataElement::encodedFile) {
            resultElement = env->CallStaticObjectMethod(
                    formDataElementClass,
                    createFromFileMethod,
                    (jstring) elements[i].m_filename.toJavaString(env));
        } else {
            const Vector<char>& data = elements[i].m_data;
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
        }
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

bool URLLoader::AsynchronousTarget::willSendRequest(
        const String& newUrl,
        const String& newMethod,
        const ResourceResponse& response)
{
    ResourceHandleClient* client = m_handle->client();
    if (client) {
        ResourceRequest request = m_handle->firstRequest();
        request.setURL(URL(URL(), newUrl));
        request.setHTTPMethod(newMethod);
        client->willSendRequest(m_handle, request, response);
    }
    return true;
}

void URLLoader::AsynchronousTarget::didReceiveResponse(
        const ResourceResponse& response)
{
    ResourceHandleClient* client = m_handle->client();
    if (client) {
        client->didReceiveResponse(m_handle, response);
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
        client->didFinishLoading(m_handle, 0.0);
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

void URLLoader::SynchronousTarget::didSendData(long totalBytesSent,
                                               long totalBytesToBeSent)
{
}

bool URLLoader::SynchronousTarget::willSendRequest(
        const String& newUrl,
        const String&,
        const ResourceResponse& response)
{
    // The following code was adapted from the Windows port
    // FIXME: This needs to be fixed to follow redirects correctly even
    // for cross-domain requests
    if (!protocolHostAndPortAreEqual(m_request.url(), URL(URL(), newUrl))) {
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

using namespace WebCore;

#ifdef __cplusplus
extern "C" {
#endif

static void setupResponse(ResourceResponse& response,
                          JNIEnv* env,
                          jint status,
                          jstring contentType,
                          jstring contentEncoding,
                          jlong contentLength,
                          jstring headers,
                          jstring url)
{
    if (status > 0) {
        response.setHTTPStatusCode(status);
    }

    // Fix for RT-13802: If the mime type is not specified
    // and the expected content length is 0 or not specified,
    // set the mime type to "text/html" as e.g. the CF port
    // does
    String contentTypeString(env, contentType);
    if (contentTypeString.isEmpty() && contentLength <= 0) {
        contentTypeString = "text/html";
    }
    if (!contentTypeString.isEmpty()) {
        response.setMimeType(
                extractMIMETypeFromMediaType(contentTypeString).lower());
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
        response.setMimeType(MIMETypeRegistry::getMIMETypeForPath(kurl.path()));
    }
}

JNIEXPORT void JNICALL Java_com_sun_webkit_network_URLLoader_twkDidSendData
  (JNIEnv*, jclass, jlong totalBytesSent, jlong totalBytesToBeSent, jlong data)
{
    URLLoader::Target* target =
            static_cast<URLLoader::Target*>(jlong_to_ptr(data));
    ASSERT(target);
    target->didSendData(totalBytesSent, totalBytesToBeSent);
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_network_URLLoader_twkWillSendRequest
  (JNIEnv* env, jclass, jstring newUrl, jstring newMethod, jint status,
   jstring contentType, jstring contentEncoding, jlong contentLength,
   jstring headers, jstring url, jlong data)
{
    URLLoader::Target* target =
            static_cast<URLLoader::Target*>(jlong_to_ptr(data));
    ASSERT(target);

    ResourceResponse response;
    setupResponse(
            response,
            env,
            status,
            contentType,
            contentEncoding,
            contentLength,
            headers,
            url);

    return bool_to_jbool(target->willSendRequest(
            String(env, newUrl),
            String(env, newMethod),
            response));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_network_URLLoader_twkDidReceiveResponse
  (JNIEnv* env, jclass, jint status, jstring contentType,
   jstring contentEncoding, jlong contentLength, jstring headers,
   jstring url, jlong data)
{
    URLLoader::Target* target =
            static_cast<URLLoader::Target*>(jlong_to_ptr(data));
    ASSERT(target);

    ResourceResponse response;
    setupResponse(
            response,
            env,
            status,
            contentType,
            contentEncoding,
            contentLength,
            headers,
            url);

    target->didReceiveResponse(response);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_network_URLLoader_twkDidReceiveData
  (JNIEnv* env, jclass, jobject byteBuffer, jint position, jint remaining,
   jlong data)
{
    URLLoader::Target* target =
            static_cast<URLLoader::Target*>(jlong_to_ptr(data));
    ASSERT(target);
    const char* address =
            static_cast<const char*>(env->GetDirectBufferAddress(byteBuffer));
    target->didReceiveData(address + position, remaining);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_network_URLLoader_twkDidFinishLoading
  (JNIEnv*, jclass, jlong data)
{
    URLLoader::Target* target =
            static_cast<URLLoader::Target*>(jlong_to_ptr(data));
    ASSERT(target);
    target->didFinishLoading();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_network_URLLoader_twkDidFail
  (JNIEnv* env, jclass, jint errorCode, jstring url, jstring message,
   jlong data)
{
    URLLoader::Target* target =
            static_cast<URLLoader::Target*>(jlong_to_ptr(data));
    ASSERT(target);
    target->didFail(ResourceError(
            String(),
            errorCode,
            String(env, url),
            String(env, message)));
}

#ifdef __cplusplus
}
#endif
