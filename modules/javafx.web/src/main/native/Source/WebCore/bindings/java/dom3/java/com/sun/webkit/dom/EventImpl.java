/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventTarget;

public class EventImpl implements Event {
    private static class SelfDisposer implements DisposerRecord {
        private final long peer;
        SelfDisposer(final long peer) {
            this.peer = peer;
        }

        @Override
        public void dispose() {
            EventImpl.dispose(peer);
        }
    }

    EventImpl(long peer) {
        this.peer = peer;
        Disposer.addRecord(this, new SelfDisposer(peer));
    }

    static Event create(long peer) {
        if (peer == 0L) return null;
        switch (EventImpl.getCPPTypeImpl(peer)) {
        case TYPE_MouseEvent: return new MouseEventImpl(peer);
        case TYPE_KeyboardEvent: return new KeyboardEventImpl(peer);
        case TYPE_WheelEvent: return new WheelEventImpl(peer);
        case TYPE_UIEvent: return new UIEventImpl(peer);
        case TYPE_MutationEvent: return new MutationEventImpl(peer);
        }
        return new EventImpl(peer);
    }

    private final long peer;

    long getPeer() {
        return peer;
    }

    @Override public boolean equals(Object that) {
        return (that instanceof EventImpl) && (peer == ((EventImpl)that).peer);
    }

    @Override public int hashCode() {
        long p = peer;
        return (int) (p ^ (p >> 17));
    }

    static long getPeer(Event arg) {
        return (arg == null) ? 0L : ((EventImpl)arg).getPeer();
    }

    private static void dispose(long peer) {
        EventNative.dispose(peer);
    }

    private static final int TYPE_WheelEvent = 1;
    private static final int TYPE_MouseEvent = 2;
    private static final int TYPE_KeyboardEvent = 3;
    private static final int TYPE_UIEvent = 4;
    private static final int TYPE_MutationEvent = 5;
    private static int getCPPTypeImpl(long peer) {
        return EventNative.getCPPType(peer);
    }

    static Event getImpl(long peer) {
        return (Event)create(peer);
    }


// Constants
    public static final int NONE = 0;
    public static final int CAPTURING_PHASE = 1;
    public static final int AT_TARGET = 2;
    public static final int BUBBLING_PHASE = 3;
    public static final int MOUSEDOWN = 1;
    public static final int MOUSEUP = 2;
    public static final int MOUSEOVER = 4;
    public static final int MOUSEOUT = 8;
    public static final int MOUSEMOVE = 16;
    public static final int MOUSEDRAG = 32;
    public static final int CLICK = 64;
    public static final int DBLCLICK = 128;
    public static final int KEYDOWN = 256;
    public static final int KEYUP = 512;
    public static final int KEYPRESS = 1024;
    public static final int DRAGDROP = 2048;
    public static final int FOCUS = 4096;
    public static final int BLUR = 8192;
    public static final int SELECT = 16384;
    public static final int CHANGE = 32768;

// Attributes
    @Override
    public String getType() {
        return getTypeImpl(getPeer());
    }
    static String getTypeImpl(long peer) {
        return EventNative.getType(peer);
    }

    @Override
    public EventTarget getTarget() {
        return (EventTarget)NodeImpl.getImpl(getTargetImpl(getPeer()));
    }
    static long getTargetImpl(long peer) {
        return EventNative.getTarget(peer);
    }

    @Override
    public EventTarget getCurrentTarget() {
        return (EventTarget)NodeImpl.getImpl(getCurrentTargetImpl(getPeer()));
    }
    static long getCurrentTargetImpl(long peer) {
        return EventNative.getCurrentTarget(peer);
    }

    @Override
    public short getEventPhase() {
        return getEventPhaseImpl(getPeer());
    }
    static short getEventPhaseImpl(long peer) {
        return EventNative.getEventPhase(peer);
    }

    @Override
    public boolean getBubbles() {
        return getBubblesImpl(getPeer());
    }
    static boolean getBubblesImpl(long peer) {
        return EventNative.getBubbles(peer);
    }

    @Override
    public boolean getCancelable() {
        return getCancelableImpl(getPeer());
    }
    static boolean getCancelableImpl(long peer) {
        return EventNative.getCancelable(peer);
    }

    @Override
    public long getTimeStamp() {
        return getTimeStampImpl(getPeer());
    }
    static long getTimeStampImpl(long peer) {
        return EventNative.getTimeStamp(peer);
    }

    public boolean getDefaultPrevented() {
        return getDefaultPreventedImpl(getPeer());
    }
    static boolean getDefaultPreventedImpl(long peer) {
        return EventNative.getDefaultPrevented(peer);
    }

    public boolean getIsTrusted() {
        return getIsTrustedImpl(getPeer());
    }
    static boolean getIsTrustedImpl(long peer) {
        return EventNative.getIsTrusted(peer);
    }

    public EventTarget getSrcElement() {
        return (EventTarget)NodeImpl.getImpl(getSrcElementImpl(getPeer()));
    }
    static long getSrcElementImpl(long peer) {
        return EventNative.getSrcElement(peer);
    }

    public boolean getReturnValue() {
        return getReturnValueImpl(getPeer());
    }
    static boolean getReturnValueImpl(long peer) {
        return EventNative.getReturnValue(peer);
    }

    public void setReturnValue(boolean value) {
        setReturnValueImpl(getPeer(), value);
    }
    static void setReturnValueImpl(long peer, boolean value) {
        EventNative.setReturnValue(peer, value);
    }

    public boolean getCancelBubble() {
        return getCancelBubbleImpl(getPeer());
    }
    static boolean getCancelBubbleImpl(long peer) {
        return EventNative.getCancelBubble(peer);
    }

    public void setCancelBubble(boolean value) {
        setCancelBubbleImpl(getPeer(), value);
    }
    static void setCancelBubbleImpl(long peer, boolean value) {
        EventNative.setCancelBubble(peer, value);
    }


// Functions
    @Override
    public void stopPropagation()
    {
        stopPropagationImpl(getPeer());
    }
    static void stopPropagationImpl(long peer) {
        EventNative.stopPropagation(peer);
    }


    @Override
    public void preventDefault()
    {
        preventDefaultImpl(getPeer());
    }
    static void preventDefaultImpl(long peer) {
        EventNative.preventDefault(peer);
    }


    @Override
    public void initEvent(String eventTypeArg
        , boolean canBubbleArg
        , boolean cancelableArg)
    {
        initEventImpl(getPeer()
            , eventTypeArg
            , canBubbleArg
            , cancelableArg);
    }
    static void initEventImpl(long peer
        , String eventTypeArg
        , boolean canBubbleArg
        , boolean cancelableArg) {
        EventNative.initEvent(peer, eventTypeArg, canBubbleArg, cancelableArg);
    }


    public void stopImmediatePropagation()
    {
        stopImmediatePropagationImpl(getPeer());
    }
    static void stopImmediatePropagationImpl(long peer) {
        EventNative.stopImmediatePropagation(peer);
    }


}

