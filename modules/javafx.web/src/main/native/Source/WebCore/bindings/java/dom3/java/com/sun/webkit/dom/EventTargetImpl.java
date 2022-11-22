/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
import org.w3c.dom.DOMException;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

public class EventTargetImpl implements EventTarget {
    private static class SelfDisposer implements DisposerRecord {
        private final long peer;
        SelfDisposer(final long peer) {
            this.peer = peer;
        }
        public void dispose() {
            EventTargetImpl.dispose(peer);
        }
    }

    EventTargetImpl(long peer) {
        this.peer = peer;
        Disposer.addRecord(this, new SelfDisposer(peer));
    }

    static EventTarget create(long peer) {
        if (peer == 0L) return null;
        return new EventTargetImpl(peer);
    }

    private final long peer;

    long getPeer() {
        return peer;
    }

    @Override public boolean equals(Object that) {
        return (that instanceof EventTargetImpl) && (peer == ((EventTargetImpl)that).peer);
    }

    @Override public int hashCode() {
        long p = peer;
        return (int) (p ^ (p >> 17));
    }

    static long getPeer(EventTarget arg) {
        return (arg == null) ? 0L : ((EventTargetImpl)arg).getPeer();
    }

    native private static void dispose(long peer);

    static EventTarget getImpl(long peer) {
        return (EventTarget)create(peer);
    }


// Functions
    public void addEventListener(String type
        , EventListener listener
        , boolean useCapture)
    {
        addEventListenerImpl(getPeer()
            , type
            , EventListenerImpl.getPeer(listener)
            , useCapture);
    }
    native static void addEventListenerImpl(long peer
        , String type
        , long listener
        , boolean useCapture);


    public void removeEventListener(String type
        , EventListener listener
        , boolean useCapture)
    {
        removeEventListenerImpl(getPeer()
            , type
            , EventListenerImpl.getPeer(listener)
            , useCapture);
    }
    native static void removeEventListenerImpl(long peer
        , String type
        , long listener
        , boolean useCapture);


    public boolean dispatchEvent(Event event) throws DOMException
    {
        return dispatchEventImpl(getPeer()
            , EventImpl.getPeer(event));
    }
    native static boolean dispatchEventImpl(long peer
        , long event);


}

