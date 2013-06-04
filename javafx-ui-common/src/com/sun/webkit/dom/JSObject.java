/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AccessControlContext;
import java.security.AccessController;
import netscape.javascript.JSException;

class JSObject extends netscape.javascript.JSObject {
    private static final String UNDEFINED = new String("undefined");
    static final int JS_CONTEXT_OBJECT  = 0;
    static final int JS_DOM_NODE_OBJECT  = 1;
    static final int JS_DOM_WINDOW_OBJECT  = 2;

    private final long peer;     // C++ peer - now it is the DOMObject instance
    private final int peer_type; // JS_XXXX const

    JSObject(long peer, int peer_type) {
        this.peer = peer;
        this.peer_type = peer_type;
    }

    long getPeer() {
        return peer;
    }

    @Override
    public Object eval(String s) throws JSException {
        return evalImpl(peer, peer_type, s);
    }
    private static native Object evalImpl(long peer, int peer_type,
                                          String name);

    @Override
    public Object getMember(String name) {
        return getMemberImpl(peer, peer_type, name);
    }
    private static native Object getMemberImpl(long peer, int peer_type,
                                               String name);

    @Override
    public void setMember(String name, Object value) throws JSException {
        setMemberImpl(peer, peer_type, name, value,
                      AccessController.getContext());
    }
    private static native void setMemberImpl(long peer, int peer_type,
                                             String name, Object value,
                                             AccessControlContext acc);

    @Override
    public void removeMember(String name) throws JSException {
        removeMemberImpl(peer, peer_type, name);
    }
    private static native void removeMemberImpl(long peer, int peer_type,
                                                String name);

    @Override
    public Object getSlot(int index) throws JSException {
        return getSlotImpl(peer, peer_type, index);
    }
    private static native Object getSlotImpl(long peer, int peer_type,
                                             int index);

    @Override
    public void setSlot(int index, Object value) throws JSException {
        setSlotImpl(peer, peer_type, index, value,
                    AccessController.getContext());
    }
    private static native void setSlotImpl(long peer, int peer_type,
                                           int index, Object value,
                                           AccessControlContext acc);

    @Override
    public Object call(String methodName, Object... args) throws JSException {
        return callImpl(peer, peer_type, methodName, args,
                        AccessController.getContext());
    }
    private static native Object callImpl(long peer, int peer_type,
                                          String methodName, Object[] args,
                                          AccessControlContext acc);

    @Override
    public String toString() {
        return toStringImpl(peer, peer_type);
    }
    private static native String toStringImpl(long peer, int peer_type);

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

    private static JSException fwkMakeException(Object value) {
        String msg = value == null ? null : value.toString();
        // Would like to set wrappedException, but can't do that while
        // also setting the message.  Perhaps we should create a subclass.
        JSException ex
            = new JSException(value == null ? null : value.toString());
        if (value instanceof Throwable)
            ex.initCause((Throwable) value);
        return ex;
    }
}
