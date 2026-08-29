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
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * FFM facade for the {@code wkj_socket_*} functions of the {@code jfxwebkit} C ABI, used by
 * {@link SocketStreamHandle} to report the progress of a WebSocket connection.
 * <p>
 * The handle is the {@code WebCore::SocketStreamHandleImpl*} that
 * {@code WKJHostNetwork::socket_create} was given, which the Java side has carried as {@code data}
 * ever since. It is the first argument of every C prototype, where the JNI functions took it last;
 * the Java methods keep their own parameter order so that no call site changes.
 * <p>
 * {@link #didReceiveData} must not be bound with {@code Linker.Option.critical(true)}: its body
 * calls {@code SocketStreamHandleClient::didReceiveSocketStreamData}, which delivers the frame to
 * the WebSocket channel and from there to script. The frame is copied through a confined arena
 * instead.
 *
 * @see com.sun.webkit.WebKitNative
 */
final class SocketStreamHandleNative {

    private static final MethodHandle DID_OPEN = WebKitNative.downcall(
            "wkj_socket_did_open",
            FunctionDescriptor.ofVoid(JAVA_LONG));
    private static final MethodHandle DID_RECEIVE_DATA = WebKitNative.downcall(
            "wkj_socket_did_receive_data",
            FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT));
    private static final MethodHandle DID_FAIL = WebKitNative.downcall(
            "wkj_socket_did_fail",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT));
    private static final MethodHandle DID_CLOSE = WebKitNative.downcall(
            "wkj_socket_did_close",
            FunctionDescriptor.ofVoid(JAVA_LONG));

    private SocketStreamHandleNative() {
    }

    static void didOpen(long handle) {
        try {
            DID_OPEN.invokeExact(handle);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * Delivers {@code length} bytes read from the front of {@code buffer}.
     *
     * @param buffer the read buffer
     * @param length the number of bytes read into it
     * @param handle the socket handle
     */
    static void didReceiveData(byte[] buffer, int length, long handle) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment data = arena.allocate(JAVA_BYTE, Math.max(length, 0));
            if (length > 0) {
                MemorySegment.copy(buffer, 0, data, JAVA_BYTE, 0L, length);
            }
            try {
                DID_RECEIVE_DATA.invokeExact(handle, data, length);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    static void didFail(int errorCode, String errorDescription, long handle) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment description = WKJStringCodec.encode(arena, errorDescription);
            try {
                DID_FAIL.invokeExact(handle, errorCode, description,
                        WKJStringCodec.length(errorDescription));
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    static void didClose(long handle) {
        try {
            DID_CLOSE.invokeExact(handle);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }
}
