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
import com.sun.webkit.WebKitNative;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;

//navive code driven life circle.
//single time peer usage
final class EventListenerImpl implements EventListener {
    private static final Map<EventListener, Long> EL2peer =
            new WeakHashMap<EventListener, Long>();
    private static final Map<Long, WeakReference<EventListener>> peer2EL =
            new HashMap<Long, WeakReference<EventListener>>();

    private static final class SelfDisposer implements DisposerRecord {
        private final long peer;
        private SelfDisposer(final long peer) {
            this.peer = peer;
        }

        @Override
        public void dispose() {
            //dispose JavaEL <-> JSstab connection (JavaEL die)
            EventListenerImpl.dispose(peer);
            EventListenerImpl.twkDisposeJSPeer(peer);
        }
    }

    private final EventListener eventListener;
    private final long jsPeer;

    static long getPeer(EventListener eventListener) {
        if (eventListener == null) {
            return 0L;
        }

        Long peer = EL2peer.get(eventListener);
        if (peer != null) {
            return peer;
        }

        //[eventListener] is the Java EventListener.
        EventListenerImpl eli = new EventListenerImpl(eventListener, 0L);
        peer = eli.twkCreatePeer();
        EL2peer.put(eventListener, peer);
        peer2EL.put(peer, new WeakReference<EventListener>(eventListener));

        return peer;
    }
    /**
     * Creates the native listener that forwards to this one.
     * <p>
     * The registry entry is <em>strong</em> and it is minted here only so that the library has
     * something to retain: {@code wkj_event_listener_create} borrows the id and retains what it
     * keeps, so the count goes to two inside the call and back to one when this method releases its
     * own, leaving the library holding the only reference. That reference is what keeps this
     * object, and with it the application's listener, alive - {@link #EL2peer} is a
     * {@link WeakHashMap} and {@link #peer2EL} holds {@link WeakReference}s, so nothing else does.
     *
     * @return the native listener peer
     */
    private long twkCreatePeer() {
        long id = WebKitNative.register(this);
        try {
            return EventListenerNative.create(id);
        } finally {
            WebKitNative.release(id);
        }
    }

    private static EventListener getELfromPeer(long peer) {
        WeakReference<EventListener> wr = peer2EL.get(peer);
        return wr == null ? null : wr.get();
    }

    static EventListener getImpl(long peer) {
        if (peer == 0)
            return null;

        EventListener ev = getELfromPeer(peer);
        if (ev != null) {
            // the peer need to be deref'ed!
            twkDisposeJSPeer(peer);
            return ev;
        }

        //[peer] is the JS EventListener.
        EventListener el = new EventListenerImpl(null, peer);
        EL2peer.put(el, peer);
        peer2EL.put(peer, new WeakReference<EventListener>(el));
        Disposer.addRecord(el, new SelfDisposer(peer));

        return el;
    }

    @Override
    public void handleEvent(Event evt) {
        //call to JS peer if any
        if (jsPeer != 0L && (evt instanceof EventImpl)) {
            twkDispatchEvent(jsPeer, ((EventImpl)evt).getPeer() );
        }
    }
    private static void twkDispatchEvent(long eventListenerPeer, long eventPeer) {
        EventListenerNative.dispatchEvent(eventListenerPeer, eventPeer);
    }

    private EventListenerImpl(EventListener eventListener, long jsPeer) {
        this.eventListener = eventListener;
        this.jsPeer = jsPeer;
    }

    //dispose JavaEL <-> JSstab connection (JSstab die)
    // Package private, not private: the WKJEventListenerCallbacks dispose slot dispatches here
    // through EventListenerNative, and an FFM upcall stub is an ordinary Java call where JNI could
    // reach a private member.
    static void dispose(long peer) {
        EventListener ev = getELfromPeer(peer);
        if (ev != null )
            EL2peer.remove(ev);
        peer2EL.remove(peer);
    }
    //dispose JSstab for JS-native EL
    private static void twkDisposeJSPeer(long peer) {
        EventListenerNative.disposeJSPeer(peer);
    }

    // Package private for the same reason as dispose above: this is the handle_event slot.
    void fwkHandleEvent(long eventPeer) {
        eventListener.handleEvent(EventImpl.getImpl(eventPeer));
    }
}
