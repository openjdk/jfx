/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
    private native long twkCreatePeer();

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

    public void handleEvent(Event evt) {
        //call to JS peer if any
        if (jsPeer != 0L && (evt instanceof EventImpl)) {
            twkDispatchEvent(jsPeer, ((EventImpl)evt).getPeer() );
        }
    }
    private native static void twkDispatchEvent(long eventListenerPeer, long eventPeer);

    private EventListenerImpl(EventListener eventListener, long jsPeer) {
        this.eventListener = eventListener;
        this.jsPeer = jsPeer;
    }

    //dispose JavaEL <-> JSstab connection (JSstab die)
    private static void dispose(long peer) {
        EventListener ev = getELfromPeer(peer);
        if (ev != null )
            EL2peer.remove(ev);
        peer2EL.remove(peer);
    }
    //dispose JSstab for JS-native EL
    private native static void twkDisposeJSPeer(long peer);

    private void fwkHandleEvent(long eventPeer) {
        eventListener.handleEvent(EventImpl.getImpl(eventPeer));
    }
}
