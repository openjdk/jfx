/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.webkit.Disposer;
import com.sun.webkit.DisposerRecord;
import com.sun.webkit.Invoker;
import java.util.concurrent.atomic.AtomicInteger;
import netscape.javascript.JSException;

class JSObject extends netscape.javascript.JSObject {
    // Package private, not private: JSObjectNative answers with this instance for a
    // WKJ_JS_KIND_UNDEFINED result, where the library used to construct the value itself.
    static final String UNDEFINED = new String("undefined");
    static final int JS_CONTEXT_OBJECT  = 0;
    static final int JS_DOM_NODE_OBJECT  = 1;
    static final int JS_DOM_WINDOW_OBJECT  = 2;

    // Dummy object used as a placeholder for the former access control context.
    // This is passed to the native WebKit code where it is stored (as an opaque
    // object) and later passed back to Java code.
    // We do this, rather than removing the parameter, in order to keep the
    // native WebKit code the same across different release families.
    private static final Object DUMMY_ACC = new Object();

    private final long peer;     // C++ peer - now it is the DOMObject instance
    private final int peer_type; // JS_XXXX const

    // for testing purposes only
    private static AtomicInteger peerCount = new AtomicInteger();

    JSObject(long peer, int peer_type) {
        this.peer = peer;
        this.peer_type = peer_type;

        if (peer_type == JS_CONTEXT_OBJECT) {
            // if peer type is JS_CONTEXT_OBJECT, the JSObject is already GC Protected
            // from native side and we want to add JSObject to Disposer, only in this case.
            Disposer.addRecord(this, new SelfDisposer(peer, peer_type));
            peerCount.incrementAndGet();
        }
    }

    long getPeer() {
        return peer;
    }

    /**
     * The peer type of this object. It exists because {@code JSObjectNative} describes a
     * {@code JSObject} argument to the library as a peer and a peer type: the two
     * {@code GetFieldID} reads the C++ used to make are now Java reading its own fields, so the
     * library needs neither the ids nor the names {@code peer} and {@code peer_type}.
     *
     * @return {@link #JS_CONTEXT_OBJECT}, {@link #JS_DOM_NODE_OBJECT} or
     *         {@link #JS_DOM_WINDOW_OBJECT}
     */
    int getPeerType() {
        return peer_type;
    }

    // for testing purposes only
    static int test_getPeerCount() {
        return peerCount.get();
    }

    private static void unprotectImpl(long peer, int peer_type) {
        JSObjectNative.unprotect(peer, peer_type);
    }

    @Override
    public Object eval(String s) throws JSException {
        Invoker.getInvoker().checkEventThread();
        return evalImpl(peer, peer_type, s);
    }
    private static Object evalImpl(long peer, int peer_type,
                                   String name) {
        return JSObjectNative.eval(peer, peer_type, name);
    }

    @Override
    public Object getMember(String name) {
        Invoker.getInvoker().checkEventThread();
        return getMemberImpl(peer, peer_type, name);
    }
    private static Object getMemberImpl(long peer, int peer_type,
                                        String name) {
        return JSObjectNative.getMember(peer, peer_type, name);
    }

    @Override
    public void setMember(String name, Object value) throws JSException {
        Invoker.getInvoker().checkEventThread();
        setMemberImpl(peer, peer_type, name, value, DUMMY_ACC);
    }
    private static void setMemberImpl(long peer, int peer_type,
                                      String name, Object value,
                                      Object acc) {
        JSObjectNative.setMember(peer, peer_type, name, value, acc);
    }

    @Override
    public void removeMember(String name) throws JSException {
        Invoker.getInvoker().checkEventThread();
        removeMemberImpl(peer, peer_type, name);
    }
    private static void removeMemberImpl(long peer, int peer_type,
                                         String name) {
        JSObjectNative.removeMember(peer, peer_type, name);
    }

    @Override
    public Object getSlot(int index) throws JSException {
        Invoker.getInvoker().checkEventThread();
        return getSlotImpl(peer, peer_type, index);
    }
    private static Object getSlotImpl(long peer, int peer_type,
                                      int index) {
        return JSObjectNative.getSlot(peer, peer_type, index);
    }

    @Override
    public void setSlot(int index, Object value) throws JSException {
        Invoker.getInvoker().checkEventThread();
        setSlotImpl(peer, peer_type, index, value, DUMMY_ACC);
    }
    private static void setSlotImpl(long peer, int peer_type,
                                    int index, Object value,
                                    Object acc) {
        JSObjectNative.setSlot(peer, peer_type, index, value, acc);
    }

    @Override
    public Object call(String methodName, Object... args) throws JSException {
        Invoker.getInvoker().checkEventThread();
        return callImpl(peer, peer_type, methodName, args, DUMMY_ACC);
    }
    private static Object callImpl(long peer, int peer_type,
                                   String methodName, Object[] args,
                                   Object acc) {
        return JSObjectNative.call(peer, peer_type, methodName, args, acc);
    }

    @Override
    public String toString() {
        Invoker.getInvoker().checkEventThread();
        return toStringImpl(peer, peer_type);
    }
    private static String toStringImpl(long peer, int peer_type) {
        return JSObjectNative.stringValue(peer, peer_type);
    }

    @Override
    public boolean equals(Object other) {
        return other == this
          || (other != null && other.getClass() == JSObject.class
              && peer == ((JSObject) other).peer);
    }

    @Override
    public int hashCode() {
        return (int) (peer ^ (peer >> 17));
    }

    // Package private, not private: JSObjectNative builds the JSException from the described
    // JavaScript value, where throwJavaException used to reach this method through a cached id.
    static JSException fwkMakeException(Object value) {
        String msg = value == null ? null : value.toString();
        // Would like to set wrappedException, but can't do that while
        // also setting the message.  Perhaps we should create a subclass.
        JSException ex
            = new JSException(value == null ? null : value.toString());
        if (value instanceof Throwable)
            ex.initCause((Throwable) value);
        return ex;
    }

    private static final class SelfDisposer implements DisposerRecord {
        long peer;
        final int peer_type;

        private SelfDisposer(long peer, int peer_type) {
            this.peer = peer;
            this.peer_type = peer_type;
        }

        @Override public void dispose() {
            if (peer != 0) {
                JSObject.unprotectImpl(peer, peer_type);
                peer = 0;
                peerCount.decrementAndGet();
            }
        }
    }
}
