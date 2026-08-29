/*
 * Copyright (c) 2020, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include <webkit_java_api.h>

#include <wtf/java/WKJHandle.h>
#include <wtf/java/WKJRuntime.h>

namespace PAL {

namespace CryptoDigestInternal {

/*
 * The installed PAL table, or nullptr before wkj_init has run. It replaces
 * GetMessageDigestClass(), which cached a global reference to
 * com.sun.webkit.security.WCMessageDigest purely so that member ids could be resolved on it.
 */
inline const WKJHostPAL* wkjPAL()
{
    return wkj_host ? &wkj_host->pal : nullptr;
}

struct AlgorithmName {
    const uint16_t* data;
    int32_t length;
};

/*
 * The java.security.MessageDigest algorithm names, as UTF-16 literals: the same five strings
 * the JNI version built with NewStringUTF, with no allocation and no encoding conversion on
 * the way to Java. The mapping stays here, next to the enum that owns it, rather than moving
 * into the Java forwarder.
 */
AlgorithmName toJavaMessageDigestAlgorithm(CryptoDigest::Algorithm algorithm)
{
    static const uint16_t sha1[] = { 'S', 'H', 'A', '-', '1' };
    static const uint16_t sha224[] = { 'S', 'H', 'A', '-', '2', '2', '4' };
    static const uint16_t sha256[] = { 'S', 'H', 'A', '-', '2', '5', '6' };
    static const uint16_t sha384[] = { 'S', 'H', 'A', '-', '3', '8', '4' };
    static const uint16_t sha512[] = { 'S', 'H', 'A', '-', '5', '1', '2' };
    static const uint16_t empty = 0;

    switch (algorithm) {
        case CryptoDigest::Algorithm::SHA_1:
            return { sha1, 5 };
        case CryptoDigest::Algorithm::DEPRECATED_SHA_224:
            return { sha224, 7 };
        case CryptoDigest::Algorithm::SHA_256:
            return { sha256, 7 };
        case CryptoDigest::Algorithm::SHA_384:
            return { sha384, 7 };
        case CryptoDigest::Algorithm::SHA_512:
            return { sha512, 7 };
    }

    // Unreachable while the switch stays exhaustive. The JNI version defaulted to "", whose
    // getInstance("") threw and left a null digest; a non-null pointer with length 0 is how
    // this ABI spells the empty string, so that path is unchanged.
    return { &empty, 0 };
}

} // namespace CryptoDigestInternal

/*
 * jDigest is the named owner of the id that crypto_digest_create minted, and the WKJHandle
 * destructor is where it is released - exactly the role the JGObject had. Nothing reclaims a
 * leaked id, so this ownership has to be explicit.
 */
struct CryptoDigestContext {
    WKJHandle jDigest { };
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

    const WKJHostPAL* cb = wkjPAL();
    if (!cb || !cb->crypto_digest_create)
        return digest;

    AlgorithmName name = toJavaMessageDigestAlgorithm(algorithm);
    // Adopts the new id; a 0 return leaves a null handle, which is what the JNI code produced
    // when getInstance threw and the exception check turned the result into an empty JLObject.
    digest->m_context->jDigest = WKJHandle { cb->crypto_digest_create(name.data, name.length) };
    WTF::wkjCheckAndClearException();

    return digest;
}

void CryptoDigest::addBytes(std::span<const uint8_t> input)
{
    using namespace CryptoDigestInternal;

    const WKJHostPAL* cb = wkjPAL();
    if (!m_context->jDigest || !cb || !cb->crypto_digest_add_bytes)
        return;

    /*
     * The bytes are wrapped by Java without copying for the duration of the call, which is
     * what NewDirectByteBuffer did.
     *
     * No exception check here, deliberately: the JNI version had none either (it called
     * CallVoidMethod and moved on), so adding one would change which call sees a failure.
     * That omission is a pre-existing defect, not something this migration decides.
     */
    cb->crypto_digest_add_bytes(m_context->jDigest.get(), input.data(),
                                static_cast<int32_t>(input.size()));
}

Vector<uint8_t> CryptoDigest::computeHash()
{
    using namespace CryptoDigestInternal;

    const WKJHostPAL* cb = wkjPAL();
    if (!m_context->jDigest || !cb || !cb->crypto_digest_compute_hash)
        return { };

    /*
     * 64 bytes is SHA-512, the largest of the five algorithms above, so the overflow branch is
     * unreachable today.
     *
     * It deliberately does NOT retry. Everywhere else in this ABI a WKJ_STR_OVERFLOW is
     * answered by growing the buffer and calling again, but this slot is not idempotent:
     * java.security.MessageDigest.digest() RESETS the digest, so a second call would return
     * the hash of nothing. An algorithm wider than 64 bytes must therefore widen this buffer
     * rather than rely on a retry, and until then an overflow is reported as no hash - the
     * same empty Vector the JNI version returned when GetPrimitiveArrayCritical failed.
     *
     * That critical region is also why this is a copy: contract 13.1 forbids
     * GetPrimitiveArrayCritical-style pinning on this ABI, and 64 bytes is not worth pinning.
     *
     * No exception check, matching the JNI version, which read the returned byte[] without
     * one.
     */
    uint8_t buffer[64];
    int32_t length = 0;
    int32_t status = cb->crypto_digest_compute_hash(m_context->jDigest.get(), buffer,
                                                    static_cast<int32_t>(sizeof(buffer)), &length);
    if (status != WKJ_STR_OK || length <= 0)
        return { };

    Vector<uint8_t> result;
    result.append(std::span<const uint8_t>(buffer, static_cast<size_t>(length)));
    return result;
}

} // namespace PAL
