/*
 * Copyright (c) 2017, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "CryptoDigest.h"
#include <jni.h>
#include <wtf/java/JavaEnv.h>
#include <wtf/java/JavaRef.h>

namespace PAL {

namespace CryptoDigestInternal {

inline jclass GetMessageDigestClass(JNIEnv* env)
{
    static JGClass messageDigestCls(
        env->FindClass("com/sun/webkit/security/WCMessageDigest"));
    ASSERT(messageDigestCls);
    return messageDigestCls;
}

inline JLObject GetMessageDigestInstance(jstring algorithm)
{
    JNIEnv* env = WTF::GetJavaEnv();
    if (!env) {
        return { };
    }

    static jmethodID midGetInstance = env->GetStaticMethodID(
        GetMessageDigestClass(env),
        "getInstance",
        "(Ljava/lang/String;)Lcom/sun/webkit/security/WCMessageDigest;");
    ASSERT(midGetInstance);
    JLObject jDigest = env->CallStaticObjectMethod(GetMessageDigestClass(env), midGetInstance, algorithm);
    if (WTF::CheckAndClearException(env)) {
        return { };
    }
    return jDigest;
}

jstring toJavaMessageDigestAlgorithm(CryptoDigest::Algorithm algorithm)
{
    JNIEnv* env = WTF::GetJavaEnv();

    const char* algorithmStr = "";
    switch (algorithm) {
        case CryptoDigest::Algorithm::SHA_1:
            algorithmStr = "SHA-1";
            break;
        case CryptoDigest::Algorithm::SHA_224:
            algorithmStr = "SHA-224";
            break;
        case CryptoDigest::Algorithm::SHA_256:
            algorithmStr = "SHA-256";
            break;
        case CryptoDigest::Algorithm::SHA_384:
            algorithmStr = "SHA-384";
            break;
        case CryptoDigest::Algorithm::SHA_512:
            algorithmStr = "SHA-512";
            break;
    }
    return env->NewStringUTF(algorithmStr);
}

} // namespace CryptoDigestInternal

struct CryptoDigestContext {
    JGObject jDigest { };
};

CryptoDigest::CryptoDigest()
    : m_context(new CryptoDigestContext)
{
}

CryptoDigest::~CryptoDigest()
{
}

std::unique_ptr<CryptoDigest> CryptoDigest::create(CryptoDigest::Algorithm algorithm)
{
    using namespace CryptoDigestInternal;
    auto digest = std::unique_ptr<CryptoDigest>(new CryptoDigest);
    digest->m_context->jDigest = GetMessageDigestInstance(toJavaMessageDigestAlgorithm(algorithm));
    return digest;
}

void CryptoDigest::addBytes(std::span<const uint8_t> input)
{
    using namespace CryptoDigestInternal;

    JNIEnv* env = WTF::GetJavaEnv();
    if (!m_context->jDigest || !env) {
        return;
    }

    static jmethodID midUpdate = env->GetMethodID(
        GetMessageDigestClass(env),
        "addBytes",
        "(Ljava/nio/ByteBuffer;)V");
    ASSERT(midUpdate);
    env->CallVoidMethod(jobject(m_context->jDigest), midUpdate, env->NewDirectByteBuffer(const_cast<void*>(reinterpret_cast<const void*>(input.data())), input.size()));
}

Vector<uint8_t> CryptoDigest::computeHash()
{
    using namespace CryptoDigestInternal;

    JNIEnv* env = WTF::GetJavaEnv();
    if (!m_context->jDigest || !env) {
        return { };
    }

    static jmethodID midDigest = env->GetMethodID(
        GetMessageDigestClass(env),
        "computeHash",
        "()[B");
    ASSERT(midDigest);

    JLocalRef<jbyteArray> jDigestBytes = static_cast<jbyteArray>(env->CallObjectMethod(jobject(m_context->jDigest), midDigest));
    void* digest = env->GetPrimitiveArrayCritical(static_cast<jbyteArray>(jDigestBytes), 0);
    if (!digest) {
        return { };
    }

    Vector<uint8_t> result;
    std::span<const uint8_t> createSpan(reinterpret_cast<const uint8_t*>(digest), env->GetArrayLength(jDigestBytes));
    result.append(createSpan);
    env->ReleasePrimitiveArrayCritical(jDigestBytes, digest, 0);
    return result;
}

} // namespace PAL
