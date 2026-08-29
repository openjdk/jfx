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

package com.sun.webkit.security;

import com.sun.webkit.WKJStringCodec;
import com.sun.webkit.WebKitNative;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * The {@code WKJHostPAL} group: the three {@link WCMessageDigest} upcalls of
 * {@code pal/crypto/java/CryptoDigestJava.cpp}, which are what {@code PAL::CryptoDigest} - and
 * therefore WebCrypto and Subresource Integrity - is built on.
 * <p>
 * It lives in this package rather than beside the other groups because
 * {@link WCMessageDigest#getInstance} is {@code protected static}: an upcall target has to be
 * reachable by ordinary Java rules now that there is no JNI to ignore them.
 * <p>
 * <b>{@code system_beep} is deliberately left NULL.</b> {@code PAL::systemBeep} reached
 * {@code java.awt.Toolkit.getDefaultToolkit().beep()} by name through {@code FindClass}, which in
 * {@code javafx.web} - a module that does not require {@code java.desktop} - returned null, so the
 * call did nothing and the {@code ASSERT} was compiled out of a release build. The C header
 * documents the default for a NULL slot as "no-op", which is exactly that behaviour, so leaving the
 * slot NULL preserves it precisely. Filling it means deciding what a beep should do in a JavaFX
 * process, which the header itself calls a behaviour question belonging in its own change.
 * <p>
 * <b>Threading.</b> Any. WebCrypto digests are computed on the main thread and on worker threads,
 * so each target must be safe wherever it is called; they hold no state of their own and forward to
 * an object the caller already owns.
 * <p>
 * This class contains no restricted {@code java.lang.foreign} operation.
 */
public final class PalUpcalls {

    private PalUpcalls() {
    }

    /**
     * Fills the {@code pal} group of a {@code WKJHost} table under construction.
     *
     * @param host the table
     */
    public static void install(MemorySegment host) {
        WebKitNative.installHostSlot(host, "pal.crypto_digest_create", MethodHandles.lookup(),
                "cryptoDigestCreate", FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT));
        WebKitNative.installHostSlot(host, "pal.crypto_digest_add_bytes", MethodHandles.lookup(),
                "cryptoDigestAddBytes", FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT));
        WebKitNative.installHostSlot(host, "pal.crypto_digest_compute_hash", MethodHandles.lookup(),
                "cryptoDigestComputeHash",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
        // pal.system_beep stays NULL; see the class comment.
    }

    /*
     * WCMessageDigest.getInstance(String). 0 for an algorithm Java does not have, which is the same
     * outcome the JNI code produced when getInstance threw and the exception check turned the result
     * into an empty handle; a 0 digest then makes add_bytes and compute_hash no-ops, exactly as the
     * "!m_context->jDigest" guards did. Default when NULL: 0.
     */
    private static long cryptoDigestCreate(MemorySegment algorithm, int algorithmLength) {
        try {
            String name = WebKitNative.readString(algorithm, algorithmLength);
            return WebKitNative.register(name == null ? null : WCMessageDigest.getInstance(name));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("pal.crypto_digest_create", t);
            return 0L;
        }
    }

    /*
     * WCMessageDigest.addBytes(ByteBuffer). The bytes belong to the caller and are valid only for
     * the duration of the call, so the segment is wrapped rather than copied - which is what
     * NewDirectByteBuffer did - and nothing retains it. Default when NULL: no-op.
     */
    private static void cryptoDigestAddBytes(long digest, MemorySegment data, int length) {
        try {
            if (WebKitNative.lookup(digest) instanceof WCMessageDigest target
                    && data.address() != 0L && length > 0) {
                target.addBytes(WebKitNative.resize(data, length).asByteBuffer());
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("pal.crypto_digest_add_bytes", t);
        }
    }

    /*
     * WCMessageDigest.computeHash() -> byte[], through the contract 13 buffer protocol on bytes. A
     * 64 byte buffer covers every algorithm the caller asks for, SHA-512 included, so the overflow
     * path is unreachable in practice; it exists because the protocol has it. The JNI version read
     * the array with GetPrimitiveArrayCritical, which this ABI forbids - a critical region must not
     * re-enter the JVM, and the copy that replaces it is 64 bytes. Default when NULL: WKJ_STR_NULL.
     */
    private static int cryptoDigestComputeHash(long digest, MemorySegment out, int capacity,
                                               MemorySegment length) {
        try {
            byte[] hash = WebKitNative.lookup(digest) instanceof WCMessageDigest target
                    ? target.computeHash()
                    : null;
            return WebKitNative.emitBytes(hash, out, capacity, length);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("pal.crypto_digest_compute_hash", t);
            WebKitNative.writeInt(length, 0);
            return WKJStringCodec.NULL;
        }
    }
}
