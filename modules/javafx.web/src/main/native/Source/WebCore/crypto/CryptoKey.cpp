/*
 * Copyright (C) 2013 Apple Inc. All rights reserved.
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
#include "CryptoKey.h"

#if ENABLE(WEB_CRYPTO)
#include "CryptoAlgorithmRegistry.h"
#include "CryptoKeyAES.h"
#include "CryptoKeyEC.h"
#include "CryptoKeyHMAC.h"
#include "CryptoKeyOKP.h"
#include "CryptoKeyRSA.h"
#include "CryptoKeyRaw.h"
#include "WebCoreOpaqueRoot.h"
#include <wtf/CryptographicallyRandomNumber.h>

namespace WebCore {

CryptoKey::CryptoKey(CryptoAlgorithmIdentifier algorithmIdentifier, Type type, bool extractable, CryptoKeyUsageBitmap usages)
    : m_algorithmIdentifier(algorithmIdentifier)
    , m_type(type)
    , m_extractable(extractable)
    , m_usages(usages)
{
}

CryptoKey::~CryptoKey() = default;

auto CryptoKey::usages() const -> Vector<CryptoKeyUsage>
{
    // The result is ordered alphabetically.
    Vector<CryptoKeyUsage> result;
    if (m_usages & CryptoKeyUsageDecrypt)
        result.append(CryptoKeyUsage::Decrypt);
    if (m_usages & CryptoKeyUsageDeriveBits)
        result.append(CryptoKeyUsage::DeriveBits);
    if (m_usages & CryptoKeyUsageDeriveKey)
        result.append(CryptoKeyUsage::DeriveKey);
    if (m_usages & CryptoKeyUsageEncrypt)
        result.append(CryptoKeyUsage::Encrypt);
    if (m_usages & CryptoKeyUsageSign)
        result.append(CryptoKeyUsage::Sign);
    if (m_usages & CryptoKeyUsageUnwrapKey)
        result.append(CryptoKeyUsage::UnwrapKey);
    if (m_usages & CryptoKeyUsageVerify)
        result.append(CryptoKeyUsage::Verify);
    if (m_usages & CryptoKeyUsageWrapKey)
        result.append(CryptoKeyUsage::WrapKey);
    return result;
}

WebCoreOpaqueRoot root(CryptoKey* key)
{
    return WebCoreOpaqueRoot { key };
}

#if !OS(DARWIN) || PLATFORM(GTK)
Vector<uint8_t> CryptoKey::randomData(size_t size)
{
    Vector<uint8_t> result(size);
    cryptographicallyRandomValues(result.mutableSpan());
    return result;
}
#endif

RefPtr<CryptoKey> CryptoKey::create(CryptoKey::Data&& data)
{
    switch (data.keyClass) {
    case CryptoKeyClass::AES: {
        if (data.jwk)
            return CryptoKeyAES::importJwk(data.algorithmIdentifier, WTFMove(*data.jwk), data.extractable, data.usages, [](auto, auto) { return true; });
        break;
    }
    case CryptoKeyClass::EC: {
        if (data.namedCurveString && data.jwk)
            return CryptoKeyEC::importJwk(data.algorithmIdentifier, *data.namedCurveString, WTFMove(*data.jwk), data.extractable, data.usages);
        break;
    }
    case CryptoKeyClass::HMAC:
        if (data.hashAlgorithmIdentifier && data.lengthBits && data.jwk)
            return CryptoKeyHMAC::importJwk(*data.lengthBits, *data.hashAlgorithmIdentifier, WTFMove(*data.jwk), data.extractable, data.usages, [](auto, auto) { return true; });
        break;
    case CryptoKeyClass::OKP:
        if (data.namedCurveString && data.key && data.type) {
            if (auto namedCurve = CryptoKeyOKP::namedCurveFromString(*data.namedCurveString))
                return CryptoKeyOKP::create(data.algorithmIdentifier, *namedCurve, *data.type, WTFMove(*data.key), data.extractable, data.usages);
        }
        break;
    case CryptoKeyClass::RSA: {
        if (data.jwk)
            return CryptoKeyRSA::importJwk(data.algorithmIdentifier, data.hashAlgorithmIdentifier, WTFMove(*data.jwk), data.extractable, data.usages);
        break;
    }
    case CryptoKeyClass::Raw:
        if (data.key)
            return CryptoKeyRaw::create(data.algorithmIdentifier, WTFMove(*data.key), data.usages);
        break;
    }

    return nullptr;
}

} // namespace WebCore
#endif // ENABLE(WEB_CRYPTO)
