/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

/*
 * webkit_java_api_pal.h - the PAL slice of the ABI: Source/WebCore/PAL/pal.
 *
 * PAL is WebKit's platform abstraction layer, and its Java port is two files with four
 * upcalls between them:
 *
 *   pal/crypto/java/CryptoDigestJava.cpp  com.sun.webkit.security.WCMessageDigest
 *   pal/system/java/SoundJava.cpp         java.awt.Toolkit.getDefaultToolkit().beep()
 *
 * They get a group of their own rather than being folded into "theme" because they are a
 * different layer with a different owner, and because WKJHostTheme is already the catch-all
 * for WebCore/platform/java; adding a third unrelated client to it would make it the
 * catch-all for the whole library.
 *
 * ------------------------------------------------------------------------------------------
 * A NOTE ON system_beep, WHICH IS AN AWT DEPENDENCY
 * ------------------------------------------------------------------------------------------
 * PAL::systemBeep() reached java.awt.Toolkit - i.e. the java.desktop module - from inside
 * javafx.web, by name, through FindClass. That is a real dependency on a module javafx.web
 * does not require, and it fails silently today: FindClass returns null, the ASSERT is
 * compiled out of a release build, and the call does nothing. This ABI does not change that
 * either way. It is recorded here because a slot named system_beep makes the dependency
 * visible for the first time, and because whoever implements the Java side has to decide
 * what it should do - which is a behaviour question, not a migration one, and belongs in its
 * own change.
 *
 * ------------------------------------------------------------------------------------------
 * INTEGRATION - the edit this header requires in webkit_java_api.h
 * ------------------------------------------------------------------------------------------
 *     #include "webkit_java_api_pal.h"      beside the _platform, _theme and _wtf includes
 *     WKJHostPAL pal;                       member of WKJHost
 *
 * One-way include, for the same reason as the other area headers: WKJHost needs WKJHostPAL
 * as a complete type in the middle of its own body.
 *
 * CONVENTIONS - inherited from webkit_java_api.h; only the additions are restated.
 * Java objects are wkj_ref (contract 3); a slot that RETURNS one returns a NEW id the
 * library owns and must release exactly once, held in a WKJHandle. Byte arrays come out
 * through the contract-13 buffer protocol, with WKJ_STR_OK / WKJ_STR_NULL / WKJ_STR_OVERFLOW
 * counting bytes rather than UTF-16 code units. Every slot may be NULL.
 *
 * THREAD: any. WebCrypto digests are computed on the main thread and on worker threads.
 */

#ifndef WEBKIT_JAVA_API_PAL_H
#define WEBKIT_JAVA_API_PAL_H

/*
 * Included by webkit_java_api.h, not the other way round - see INTEGRATION above.
 */
#ifndef WEBKIT_JAVA_API_H
#error "include webkit_java_api.h; it includes this header at the right point"
#endif

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ======================================================================================== */
/* WKJHostPAL - WebCore/PAL/pal                                                             */
/* ======================================================================================== */

typedef struct WKJHostPAL {

    /* --- com.sun.webkit.security.WCMessageDigest (PAL::CryptoDigest) -------------------- */

    /*
     * WCMessageDigest.getInstance(String algorithm) -> WCMessageDigest.
     *
     * The algorithm arrives as its java.security.MessageDigest name - "SHA-1", "SHA-224",
     * "SHA-256", "SHA-384" or "SHA-512" - which is what the C++ already computed from
     * CryptoDigest::Algorithm and passed as a jstring. Keeping the string, rather than
     * passing the enum and having Java map it, leaves the mapping in the one place that
     * owns the enum and keeps the Java side a plain forwarder.
     *
     * Returns a new digest id, or 0 when Java has no such algorithm - which is the same
     * outcome the JNI code produced when getInstance threw and the exception check turned it
     * into an empty handle. A 0 digest makes add_bytes and compute_hash no-ops, exactly as
     * the "!m_context->jDigest" guards did.
     * Default when NULL: 0.
     */
    wkj_ref (*crypto_digest_create)(const uint16_t* algorithm, int32_t algorithm_len);

    /*
     * WCMessageDigest.addBytes(ByteBuffer). "data" points at "length" bytes owned by the
     * caller and valid only for the duration of the call; Java wraps the address without
     * copying, exactly as NewDirectByteBuffer did, and must not retain it.
     * Default when NULL: no-op.
     */
    void (*crypto_digest_add_bytes)(wkj_ref digest, const void* data, int32_t length);

    /*
     * WCMessageDigest.computeHash() -> byte[], through the contract-13 buffer protocol on
     * bytes: WKJ_STR_OK with *out_length bytes written, WKJ_STR_NULL when Java returned
     * null, WKJ_STR_OVERFLOW with *out_length set to the size required and nothing written.
     *
     * A 64-byte buffer covers every algorithm above (SHA-512 is the largest at 64), so the
     * overflow path is unreachable in practice; it exists because the protocol has it and
     * because an algorithm added later must not silently truncate. The JNI version read the
     * returned byte[] with GetPrimitiveArrayCritical, which is forbidden on this ABI
     * (contract 13.1, finding 6): a critical region must not re-enter the JVM, and the copy
     * this replaces it with is 64 bytes.
     * Default when NULL: WKJ_STR_NULL.
     */
    int32_t (*crypto_digest_compute_hash)(wkj_ref digest, uint8_t* out_buf, int32_t out_cap,
                                          int32_t* out_length);

    /* --- PAL::systemBeep (static; no target ref) ---------------------------------------- */

    /*
     * Sounds the system beep. See the AWT note at the top of this header: the JNI
     * implementation called java.awt.Toolkit.getDefaultToolkit().beep() and did nothing at
     * all if that class could not be found, which in javafx.web is the usual case.
     * Default when NULL: no-op.
     */
    void (*system_beep)(void);

} WKJHostPAL;

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* WEBKIT_JAVA_API_PAL_H */
