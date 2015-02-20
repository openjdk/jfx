/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "SocketStreamHandle.h"

#include "JavaEnv.h"
#include "SocketStreamError.h"
#include "SocketStreamHandleClient.h"
#include "WebPage.h"
#include "com_sun_webkit_network_SocketStreamHandle.h"

namespace WebCore {

static jclass GetSocketStreamHandleClass(JNIEnv* env)
{
    static JGClass socketStreamHandleClass(env->FindClass(
            "com/sun/webkit/network/SocketStreamHandle"));
    ASSERT(socketStreamHandleClass);
    return socketStreamHandleClass;
}

SocketStreamHandle::SocketStreamHandle(const URL& url, Page* page,
                                       SocketStreamHandleClient* client)
    : SocketStreamHandleBase(url, client)
{
    String host = url.host();
    bool ssl = url.protocolIs("wss");
    int port = url.hasPort() ? url.port() : (ssl ? 443 : 80);

    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(
            GetSocketStreamHandleClass(env),
            "fwkCreate",
            "(Ljava/lang/String;IZLcom/sun/webkit/WebPage;J)"
            "Lcom/sun/webkit/network/SocketStreamHandle;");
    ASSERT(mid);

    m_ref = JLObject(env->CallStaticObjectMethod(
            GetSocketStreamHandleClass(env),
            mid,
            (jstring) host.toJavaString(env),
            port,
            bool_to_jbool(ssl),
            (jobject) WebPage::jobjectFromPage(page),
            ptr_to_jlong(this)));
    CheckAndClearException(env);
}

SocketStreamHandle::~SocketStreamHandle()
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetMethodID(
            GetSocketStreamHandleClass(env),
            "fwkNotifyDisposed",
            "()V");
    ASSERT(mid);

    env->CallVoidMethod(m_ref, mid);
    CheckAndClearException(env);
}

int SocketStreamHandle::platformSend(const char* data, int len)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    JLByteArray byteArray = env->NewByteArray(len);
    env->SetByteArrayRegion(
            (jbyteArray) byteArray,
            (jsize) 0,
            (jsize) len,
            (const jbyte*) data);

    static jmethodID mid = env->GetMethodID(
            GetSocketStreamHandleClass(env),
            "fwkSend",
            "([B)I");
    ASSERT(mid);

    jint res = env->CallIntMethod(m_ref, mid, (jbyteArray) byteArray);
    CheckAndClearException(env);

    return res;
}

void SocketStreamHandle::platformClose()
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetMethodID(
            GetSocketStreamHandleClass(env),
            "fwkClose",
            "()V");
    ASSERT(mid);

    env->CallVoidMethod(m_ref, mid);
    CheckAndClearException(env);
}

void SocketStreamHandle::didOpen()
{
    m_state = Open;
    m_client->didOpenSocketStream(this);
}

void SocketStreamHandle::didReceiveData(const char* data, int length)
{
    m_client->didReceiveSocketStreamData(this, data, length);
}

void SocketStreamHandle::didFail(int errorCode, const String& errorDescription)
{
    m_client->didFailSocketStream(
            this,
            SocketStreamError(errorCode, m_url.string(), errorDescription));
}

void SocketStreamHandle::didClose()
{
    m_client->didCloseSocketStream(this);
}

} // namespace WebCore

using namespace WebCore;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_sun_webkit_network_SocketStreamHandle_twkDidOpen
  (JNIEnv*, jclass, jlong data)
{
    SocketStreamHandle* handle =
            static_cast<SocketStreamHandle*>(jlong_to_ptr(data));
    ASSERT(handle);
    handle->didOpen();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_network_SocketStreamHandle_twkDidReceiveData
  (JNIEnv* env, jclass, jbyteArray buffer, jint len, jlong data)
{
    SocketStreamHandle* handle =
            static_cast<SocketStreamHandle*>(jlong_to_ptr(data));
    ASSERT(handle);
    jbyte* p = env->GetByteArrayElements(buffer, NULL);
    handle->didReceiveData((const char*) p, len);
    env->ReleaseByteArrayElements(buffer, p, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_network_SocketStreamHandle_twkDidFail
  (JNIEnv* env, jclass, jint errorCode, jstring errorDescription, jlong data)
{
    SocketStreamHandle* handle =
            static_cast<SocketStreamHandle*>(jlong_to_ptr(data));
    ASSERT(handle);
    handle->didFail(errorCode, String(env, errorDescription));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_network_SocketStreamHandle_twkDidClose
  (JNIEnv*, jclass, jlong data)
{
    SocketStreamHandle* handle =
            static_cast<SocketStreamHandle*>(jlong_to_ptr(data));
    ASSERT(handle);
    handle->didClose();
}

#ifdef __cplusplus
}
#endif
