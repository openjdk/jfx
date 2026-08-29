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
import com.sun.webkit.WebPage;
import com.sun.webkit.WebKitNative;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * The {@code WKJHostNetwork} group: the eleven upcalls of
 * {@code Source/WebCore/platform/network/java} against {@link NetworkContext},
 * {@link URLLoaderBase}, {@link FormDataElement}, {@link SocketStreamHandle} and
 * {@code CookieJar}. All eleven are filled.
 * <p>
 * <b>Threading.</b> Every one of these is made on the WebKit main thread: {@code URLLoader::load}
 * runs from {@code ResourceHandle} and {@code SocketStreamHandleImpl} from the WebSocket channel,
 * both main-thread by WebCore contract. The completions travel the other way, as the
 * {@code wkj_url_loader_} and {@code wkj_socket_} downcalls, which {@code URLLoader} already routes
 * through {@code Invoker.invokeOnEventThread}.
 * <p>
 * <b>Ownership.</b> The three slots that return a {@code wkj_ref} - the loader, the two form data
 * element factories and the socket - mint a new id the library owns and releases exactly once. The
 * {@code web_page} and {@code loader} ids they receive are borrowed for the duration of the call.
 * <p>
 * {@code socket_send} is the one slot whose caller distinguishes "the upcall threw" from a real
 * result, by calling {@code core.check_and_clear_exception} immediately afterwards - exactly where
 * the JNI code called {@code WTF::CheckAndClearException} and returned {@code nullopt}. Routing the
 * {@code catch} through {@link WebKitNative#upcallFailed} is what keeps that working.
 * <p>
 * This class contains no restricted {@code java.lang.foreign} operation.
 */
public final class NetworkUpcalls {

    private NetworkUpcalls() {
    }

    /**
     * Fills the {@code network} group of a {@code WKJHost} table under construction.
     *
     * @param host the table
     */
    public static void install(MemorySegment host) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        WebKitNative.installHostSlot(host, "network.url_loader_load", lookup, "urlLoaderLoad",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS,
                        JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, JAVA_LONG));
        WebKitNative.installHostSlot(host, "network.url_loader_cancel", lookup, "urlLoaderCancel",
                FunctionDescriptor.ofVoid(JAVA_LONG));
        WebKitNative.installHostSlot(host, "network.form_data_create_from_bytes", lookup,
                "formDataCreateFromBytes", FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT));
        WebKitNative.installHostSlot(host, "network.form_data_create_from_file", lookup,
                "formDataCreateFromFile", FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT));
        WebKitNative.installHostSlot(host, "network.socket_create", lookup, "socketCreate",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_LONG,
                        JAVA_LONG));
        WebKitNative.installHostSlot(host, "network.socket_send", lookup, "socketSend",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT));
        WebKitNative.installHostSlot(host, "network.socket_close", lookup, "socketClose",
                FunctionDescriptor.ofVoid(JAVA_LONG));
        WebKitNative.installHostSlot(host, "network.socket_notify_disposed", lookup,
                "socketNotifyDisposed", FunctionDescriptor.ofVoid(JAVA_LONG));
        WebKitNative.installHostSlot(host, "network.cookie_jar_get", lookup, "cookieJarGet",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT,
                        ADDRESS));
        WebKitNative.installHostSlot(host, "network.cookie_jar_put", lookup, "cookieJarPut",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));
        WebKitNative.installHostSlot(host, "network.get_max_http_connection_count_per_host", lookup,
                "getMaxHttpConnectionCountPerHost", FunctionDescriptor.of(JAVA_INT));
    }

    /*
     * NetworkContext.fwkLoad(WebPage, boolean, String, String, String, FormDataElement[], long).
     * The form data elements arrive as an array of borrowed ids, which may be NULL with count 0 -
     * how the JNI version passed a null array for a request with no body. A synchronous load
     * answers null, and 0 for it is correct: the caller reads 0 as "no loader object", which is what
     * a null jobject meant. Default when NULL: 0.
     */
    private static long urlLoaderLoad(long webPage, int asynchronous, MemorySegment url,
                                      int urlLength, MemorySegment method, int methodLength,
                                      MemorySegment headers, int headersLength,
                                      MemorySegment formElements, int formElementCount,
                                      long target) {
        try {
            WebPage page = WebKitNative.lookup(webPage) instanceof WebPage p ? p : null;
            FormDataElement[] elements = readFormElements(formElements, formElementCount);
            URLLoaderBase loader = NetworkContext.fwkLoad(page, asynchronous != 0,
                    WebKitNative.readString(url, urlLength),
                    WebKitNative.readString(method, methodLength),
                    WebKitNative.readString(headers, headersLength), elements, target);
            return WebKitNative.register(loader);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("network.url_loader_load", t);
            return 0L;
        }
    }

    /*
     * A null array and an empty one are not the same thing to fwkLoad, which logs "[null]" for one
     * and an empty list for the other, so the NULL pointer is passed through as null rather than
     * normalised to a zero length array.
     */
    private static FormDataElement[] readFormElements(MemorySegment elements, int count) {
        long[] refs = WebKitNative.readLongs(elements, count);
        if (refs == null) {
            return null;
        }
        FormDataElement[] values = new FormDataElement[refs.length];
        for (int i = 0; i < refs.length; i++) {
            values[i] = WebKitNative.lookup(refs[i]) instanceof FormDataElement e ? e : null;
        }
        return values;
    }

    /* URLLoaderBase.fwkCancel(). Default when NULL: no-op. */
    private static void urlLoaderCancel(long loader) {
        try {
            if (WebKitNative.lookup(loader) instanceof URLLoaderBase target) {
                target.fwkCancel();
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("network.url_loader_cancel", t);
        }
    }

    /* FormDataElement.fwkCreateFromByteArray(byte[]). Default when NULL: 0. */
    private static long formDataCreateFromBytes(MemorySegment data, int length) {
        try {
            byte[] bytes = WebKitNative.readBytes(data, length);
            return bytes == null ? 0L
                    : WebKitNative.register(FormDataElement.fwkCreateFromByteArray(bytes));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("network.form_data_create_from_bytes", t);
            return 0L;
        }
    }

    /*
     * FormDataElement.fwkCreateFromFile(String), used both for a real file element and, as the JNI
     * code did, for a blob element's URL string. Default when NULL: 0.
     */
    private static long formDataCreateFromFile(MemorySegment path, int pathLength) {
        try {
            String name = WebKitNative.readString(path, pathLength);
            return name == null ? 0L
                    : WebKitNative.register(FormDataElement.fwkCreateFromFile(name));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("network.form_data_create_from_file", t);
            return 0L;
        }
    }

    /*
     * SocketStreamHandle.fwkCreate(String, int, boolean, WebPage, long). Default when NULL: 0.
     */
    private static long socketCreate(MemorySegment host, int hostLength, int port, int ssl,
                                     long webPage, long handle) {
        try {
            WebPage page = WebKitNative.lookup(webPage) instanceof WebPage p ? p : null;
            return WebKitNative.register(SocketStreamHandle.fwkCreate(
                    WebKitNative.readString(host, hostLength), port, ssl != 0, page, handle));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("network.socket_create", t);
            return 0L;
        }
    }

    /* SocketStreamHandle.fwkSend(byte[]) -> int. Default when NULL: 0. */
    private static int socketSend(long socket, MemorySegment data, int length) {
        try {
            byte[] bytes = WebKitNative.readBytes(data, length);
            if (bytes == null
                    || !(WebKitNative.lookup(socket) instanceof SocketStreamHandle target)) {
                return 0;
            }
            return target.fwkSend(bytes);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("network.socket_send", t);
            return 0;
        }
    }

    /* SocketStreamHandle.fwkClose(). Default when NULL: no-op. */
    private static void socketClose(long socket) {
        try {
            if (WebKitNative.lookup(socket) instanceof SocketStreamHandle target) {
                target.fwkClose();
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("network.socket_close", t);
        }
    }

    /* SocketStreamHandle.fwkNotifyDisposed(). Default when NULL: no-op. */
    private static void socketNotifyDisposed(long socket) {
        try {
            if (WebKitNative.lookup(socket) instanceof SocketStreamHandle target) {
                target.fwkNotifyDisposed();
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("network.socket_notify_disposed", t);
        }
    }

    /*
     * CookieJar.fwkGet(String, boolean) -> String. WKJ_STR_NULL becomes the empty string in the
     * caller, which is what the JNI code produced for a null return.
     * Default when NULL: WKJ_STR_NULL.
     */
    private static int cookieJarGet(MemorySegment url, int urlLength, int includeHttpOnly,
                                    MemorySegment out, int capacity, MemorySegment length) {
        try {
            String address = WebKitNative.readString(url, urlLength);
            String cookies = address == null ? null
                    : CookieJar.fwkGet(address, includeHttpOnly != 0);
            return WebKitNative.emitString(cookies, out, capacity, length);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("network.cookie_jar_get", t);
            WebKitNative.writeInt(length, 0);
            return WKJStringCodec.NULL;
        }
    }

    /* CookieJar.fwkPut(String, String). Default when NULL: no-op. */
    private static void cookieJarPut(MemorySegment url, int urlLength, MemorySegment value,
                                     int valueLength) {
        try {
            String address = WebKitNative.readString(url, urlLength);
            String cookie = WebKitNative.readString(value, valueLength);
            if (address != null && cookie != null) {
                CookieJar.fwkPut(address, cookie);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("network.cookie_jar_put", t);
        }
    }

    /*
     * NetworkContext.fwkGetMaximumHTTPConnectionCountPerHost(). Reached only from
     * initializeMaximumHTTPConnectionCountPerHost, which sits inside "#if 0" in
     * ResourceRequestJava.cpp; the slot is filled anyway so that re-enabling that block needs no
     * Java change. Default when NULL: 0.
     */
    private static int getMaxHttpConnectionCountPerHost() {
        try {
            return NetworkContext.fwkGetMaximumHTTPConnectionCountPerHost();
        } catch (Throwable t) {
            WebKitNative.upcallFailed("network.get_max_http_connection_count_per_host", t);
            return 0;
        }
    }
}
