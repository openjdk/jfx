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

package com.sun.webkit.network;

import com.sun.webkit.WKJStringCodec;
import com.sun.webkit.WebKitNative;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * FFM facade for the {@code wkj_url_loader_*} functions of the {@code jfxwebkit} C ABI, used by
 * {@link URLLoaderBase} to report the progress of a load.
 * <p>
 * The target handle is the {@code WebCore::URLLoader::Target*} that
 * {@code WKJHostNetwork::url_loader_load} was given, which the Java loader has carried as
 * {@code data} ever since. It is the first argument of every C prototype, where the JNI functions
 * took it last; the Java methods keep their own parameter order so that no call site changes.
 * <p>
 * {@code twkWillSendRequest} and {@code twkDidReceiveResponse} carry the same seven values and both
 * build a {@code ResourceResponse} from them. They stay two flat parameter lists rather than a
 * struct, for the reason contract section 12 gives: a struct crossing this boundary would need a
 * Java {@code StructLayout} and a per call allocation, while flat pointer and length pairs need
 * neither.
 * <p>
 * Nothing here uses {@code Linker.Option.critical(true)}. In particular
 * {@link #didReceiveData} must not: its body creates a {@code SharedBuffer} and dispatches into
 * {@code ResourceHandleClient}, which reaches arbitrary WebKit work including script.
 *
 * @see com.sun.webkit.WebKitNative
 */
final class URLLoaderNative {

    private static final MethodHandle DID_SEND_DATA = WebKitNative.downcall(
            "wkj_url_loader_did_send_data",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG, JAVA_LONG));
    private static final MethodHandle WILL_SEND_REQUEST = WebKitNative.downcall(
            "wkj_url_loader_will_send_request",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT,
                    JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));
    private static final MethodHandle DID_RECEIVE_RESPONSE = WebKitNative.downcall(
            "wkj_url_loader_did_receive_response",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT,
                    JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));
    private static final MethodHandle DID_RECEIVE_DATA = WebKitNative.downcall(
            "wkj_url_loader_did_receive_data",
            FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT));
    private static final MethodHandle DID_FINISH_LOADING = WebKitNative.downcall(
            "wkj_url_loader_did_finish_loading",
            FunctionDescriptor.ofVoid(JAVA_LONG));
    private static final MethodHandle DID_FAIL = WebKitNative.downcall(
            "wkj_url_loader_did_fail",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));

    private URLLoaderNative() {
    }

    static void didSendData(long totalBytesSent, long totalBytesToBeSent, long target) {
        try {
            DID_SEND_DATA.invokeExact(target, totalBytesSent, totalBytesToBeSent);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void willSendRequest(int status, String contentType, String contentEncoding,
                                long contentLength, String headers, String url, long target) {
        response(WILL_SEND_REQUEST, status, contentType, contentEncoding, contentLength, headers,
                url, target);
    }

    static void didReceiveResponse(int status, String contentType, String contentEncoding,
                                   long contentLength, String headers, String url, long target) {
        response(DID_RECEIVE_RESPONSE, status, contentType, contentEncoding, contentLength, headers,
                url, target);
    }

    private static void response(MethodHandle handle, int status, String contentType,
                                 String contentEncoding, long contentLength, String headers,
                                 String url, long target) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment type = WKJStringCodec.encode(arena, contentType);
            MemorySegment encoding = WKJStringCodec.encode(arena, contentEncoding);
            MemorySegment header = WKJStringCodec.encode(arena, headers);
            MemorySegment location = WKJStringCodec.encode(arena, url);
            try {
                handle.invokeExact(target, status,
                        type, WKJStringCodec.length(contentType),
                        encoding, WKJStringCodec.length(contentEncoding),
                        contentLength,
                        header, WKJStringCodec.length(headers),
                        location, WKJStringCodec.length(url));
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    /**
     * Delivers one chunk of a response body. The base address of the direct buffer is handed over,
     * which is the address {@code GetDirectBufferAddress} produced for the JNI form, and
     * {@code position} and {@code remaining} keep their meaning: {@link MemorySegment#ofBuffer}
     * answers a segment starting at the buffer's position, so the position is cleared on a
     * duplicate first or the library would read it twice.
     *
     * @param byteBuffer the direct buffer holding the chunk
     * @param position the position of the chunk within the buffer
     * @param remaining the number of bytes at {@code position}
     * @param target the loader target handle
     */
    static void didReceiveData(ByteBuffer byteBuffer, int position, int remaining, long target) {
        MemorySegment data = byteBuffer != null && byteBuffer.isDirect()
                ? MemorySegment.ofBuffer(byteBuffer.duplicate().clear())
                : MemorySegment.NULL;
        try {
            DID_RECEIVE_DATA.invokeExact(target, data, position, remaining);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void didFinishLoading(long target) {
        try {
            DID_FINISH_LOADING.invokeExact(target);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void didFail(int errorCode, String url, String message, long target) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment location = WKJStringCodec.encode(arena, url);
            MemorySegment text = WKJStringCodec.encode(arena, message);
            try {
                DID_FAIL.invokeExact(target, errorCode,
                        location, WKJStringCodec.length(url),
                        text, WKJStringCodec.length(message));
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }
}
