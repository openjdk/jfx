/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include <wtf/java/JavaEnv.h>
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

SocketStreamHandleImpl::SocketStreamHandleImpl(const URL& url, Page* page,
                                       SocketStreamHandleClient& client)
    : SocketStreamHandle(url, client)
{
    String host = url.host().toString();
    bool ssl = url.protocolIs("wss");
    int port = url.port().value_or(ssl ? 443 : 80);

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

SocketStreamHandleImpl::~SocketStreamHandleImpl()
{
    WC_GETJAVAENV_CHKRET(env);

    static jmethodID mid = env->GetMethodID(
            GetSocketStreamHandleClass(env),
            "fwkNotifyDisposed",
            "()V");
    ASSERT(mid);

    env->CallVoidMethod(m_ref, mid);
    CheckAndClearException(env);
}

void SocketStreamHandleImpl::platformSend(const uint8_t* data, size_t len, Function<void(bool)>&& completionHandler)
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
    if (CheckAndClearException(env)) {
        completionHandler(false);
    } else {
        completionHandler(res == (int)len);
    }
}

void SocketStreamHandleImpl::platformSendHandshake(const uint8_t*, size_t, const std::optional<CookieRequestHeaderFieldProxy>&, Function<void(bool, bool)>&&)
{
}
void SocketStreamHandleImpl::platformClose()
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

void SocketStreamHandleImpl::didOpen()
{
    m_state = Open;
    m_client.didOpenSocketStream(*this);
}

void SocketStreamHandleImpl::didReceiveData(const char* data, int length)
{
    m_client.didReceiveSocketStreamData(*this, data, length);
}

void SocketStreamHandleImpl::didFail(int errorCode, const String& errorDescription)
{
    m_client.didFailSocketStream(
            *this,
            SocketStreamError(errorCode, m_url.string(), errorDescription));
}

void SocketStreamHandleImpl::didClose()
{
    m_client.didCloseSocketStream(*this);
}

} // namespace WebCore

using namespace WebCore;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_sun_webkit_network_SocketStreamHandle_twkDidOpen
  (JNIEnv*, jclass, jlong data)
{
    SocketStreamHandleImpl* handle =
            static_cast<SocketStreamHandleImpl*>(jlong_to_ptr(data));
    ASSERT(handle);
    handle->didOpen();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_network_SocketStreamHandle_twkDidReceiveData
  (JNIEnv* env, jclass, jbyteArray buffer, jint len, jlong data)
{
    SocketStreamHandleImpl* handle =
            static_cast<SocketStreamHandleImpl*>(jlong_to_ptr(data));
    ASSERT(handle);
    jbyte* p = env->GetByteArrayElements(buffer, NULL);
    handle->didReceiveData((const char*) p, len);
    env->ReleaseByteArrayElements(buffer, p, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_network_SocketStreamHandle_twkDidFail
  (JNIEnv* env, jclass, jint errorCode, jstring errorDescription, jlong data)
{
    SocketStreamHandleImpl* handle =
            static_cast<SocketStreamHandleImpl*>(jlong_to_ptr(data));
    ASSERT(handle);
    handle->didFail(errorCode, String(env, errorDescription));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_network_SocketStreamHandle_twkDidClose
  (JNIEnv*, jclass, jlong data)
{
    SocketStreamHandleImpl* handle =
            static_cast<SocketStreamHandleImpl*>(jlong_to_ptr(data));
    ASSERT(handle);
    handle->didClose();
}

#ifdef __cplusplus
}
#endif
