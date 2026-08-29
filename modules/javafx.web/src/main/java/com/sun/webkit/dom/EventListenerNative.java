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

package com.sun.webkit.dom;

import com.sun.webkit.WKJLayouts;
import com.sun.webkit.WebKitNative;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * FFM facade for the {@code wkj_event_listener_*} functions of the {@code jfxwebkit} C ABI, used by
 * {@code EventListenerImpl}. It also installs {@code WKJEventListenerCallbacks}, the two upcalls
 * that were cached method ids on that class.
 * <p>
 * The table is installed from this class initializer, which is early enough by construction: the
 * library cannot fire {@code handle_event} for a listener that {@link #create} has not made, and
 * {@link #create} runs this initializer first.
 * <p>
 * <b>Ownership.</b> {@link #create} borrows the id it is given and the library retains what it
 * keeps, so this is the one structure in the DOM surface that pins a Java object - and it pins it
 * strongly, exactly as the {@code NewGlobalRef} in {@code EventListenerManager} did. That is not an
 * optimisation to reconsider: {@code EventListenerImpl.EL2peer} is a {@link java.util.WeakHashMap}
 * and {@code peer2EL} holds {@link java.lang.ref.WeakReference}s, so the native reference is the
 * only thing keeping the {@code EventListenerImpl} and the application's listener alive. A weak
 * registration would collect it at the next GC. The retained id is released by
 * {@code EventListenerManager::unregisterListener}.
 * <p>
 * <b>Failure.</b> {@link #handleEvent} swallows a {@link Throwable} without rethrowing and without
 * logging one line per event. That is load bearing rather than lazy: {@code JavaEventListener}
 * ended in {@code WTF::CheckAndClearException} and discarded the result, so a listener that threw
 * has always been silent and dispatch has always continued to the next listener. It is still routed
 * through {@code WebKitNative.upcallFailed} so that {@code check_and_clear_exception} stays
 * accurate.
 *
 * @see com.sun.webkit.WebKitNative
 */
final class EventListenerNative {

    private static final MethodHandle CREATE = WebKitNative.downcall(
            "wkj_event_listener_create",
            FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));
    private static final MethodHandle DISPATCH_EVENT = WebKitNative.downcall(
            "wkj_event_listener_dispatch_event",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG));
    // Deliberately not bound with Linker.Option.critical(true): the deref may destroy the listener,
    // which runs the dispose callback and therefore re-enters Java before the call returns.
    private static final MethodHandle DISPOSE_JS_PEER = WebKitNative.downcall(
            "wkj_event_listener_dispose_js_peer",
            FunctionDescriptor.ofVoid(JAVA_LONG));
    private static final MethodHandle INSTALL_CALLBACKS = WebKitNative.downcall(
            "wkj_install_event_listener_callbacks",
            FunctionDescriptor.ofVoid(ADDRESS));

    /** The slots of {@code WKJEventListenerCallbacks}, in declaration order. */
    private static final int CALLBACK_SLOTS =
            WKJLayouts.slotCount(WKJLayouts.EVENT_LISTENER_CALLBACKS);

    static {
        installCallbacks();
    }

    private EventListenerNative() {
    }

    /**
     * Creates the {@code WebCore::EventListener} that forwards to a Java listener and returns its
     * peer.
     *
     * @param self the registry id of the {@code EventListenerImpl}, borrowed
     * @return the listener peer
     */
    static long create(long self) {
        try {
            return (long) CREATE.invokeExact(self);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * Dispatches an event to the JavaScript side listener a Java listener wraps. The library does
     * nothing when either peer is zero or when no script execution context is on the stack, which is
     * the three way guard the JNI function carried.
     *
     * @param listenerPeer the listener peer
     * @param eventPeer the event peer
     */
    static void dispatchEvent(long listenerPeer, long eventPeer) {
        try {
            DISPATCH_EVENT.invokeExact(listenerPeer, eventPeer);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * Drops the reference Java holds on a listener. The deref may destroy the listener, which runs
     * {@link #dispose} before this returns.
     *
     * @param peer the listener peer, zero does nothing
     */
    static void disposeJSPeer(long peer) {
        try {
            DISPOSE_JS_PEER.invokeExact(peer);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    private static void installCallbacks() {
        MemorySegment callbacks = WebKitNative.upcallTable(
                stub("handleEvent", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG)),
                stub("dispose", FunctionDescriptor.ofVoid(JAVA_LONG)));
        if (callbacks.byteSize() != (long) CALLBACK_SLOTS * ADDRESS.byteSize()) {
            throw new AssertionError("WKJEventListenerCallbacks has " + CALLBACK_SLOTS + " slots");
        }
        try {
            INSTALL_CALLBACKS.invokeExact(callbacks);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    private static MemorySegment stub(String name, FunctionDescriptor descriptor) {
        MethodHandle target;
        try {
            target = MethodHandles.lookup().findStatic(EventListenerNative.class, name,
                    descriptor.toMethodType());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("no upcall target " + name + descriptor.toMethodType(), e);
        }
        return WebKitNative.upcallStub(target, descriptor);
    }

    /**
     * {@code EventListenerImpl.fwkHandleEvent(long)}. An id that is unknown, that is zero, or whose
     * object is no longer an {@code EventListenerImpl} is ignored, which is what a JNI call on a
     * cleared reference would have amounted to.
     * <p>
     * {@code eventPeer} has already been {@code ref()}ed for Java by the library, and
     * {@code EventImpl.getImpl} drops that reference through its disposer - or immediately, on a
     * cache hit. Neither side adds or removes one here, which is the pairing the leak tests measure.
     *
     * @param listener the registry id of the listener
     * @param eventPeer the event peer
     */
    static void handleEvent(long listener, long eventPeer) {
        try {
            if (WebKitNative.lookup(listener) instanceof EventListenerImpl impl) {
                impl.fwkHandleEvent(eventPeer);
            }
        } catch (Throwable t) {
            // Swallowed on purpose, and without a per event log line: see the class comment.
            WebKitNative.upcallFailed("event listener callback handle_event", t);
        }
    }

    /**
     * {@code EventListenerImpl.dispose(long)}: the C++ listener has been destroyed, so Java drops it
     * from its peer tables. {@code listenerPeer} is the value {@link #create} returned and is a key
     * and nothing else - by the time this runs the C++ object is already being destroyed and
     * nothing may dereference it.
     *
     * @param listenerPeer the listener peer
     */
    static void dispose(long listenerPeer) {
        try {
            EventListenerImpl.dispose(listenerPeer);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("event listener callback dispose", t);
        }
    }
}
