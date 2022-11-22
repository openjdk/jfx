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
import com.sun.webkit.dom.JSObject;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.views.AbstractView;
import org.w3c.dom.views.DocumentView;

public class DOMWindowImpl extends JSObject implements AbstractView, EventTarget {
    // We use a custom hash-table rather than java.util.HashMap,
    // because the latter requires 2 extra objects for each entry:
    // a Long for the key plus a Map.Entry.  Since we have a 'next'
    // field already in the SelfDisposer, we can use it as the entry.
    private static SelfDisposer[] hashTable = new SelfDisposer[64];
    private static int hashCount;

    private static int hashPeer(long peer) {
        return (int) (~peer ^ (peer >> 7)) & (hashTable.length-1);
    }

    private static AbstractView getCachedImpl(long peer) {
        if (peer == 0)
            return null;
        int hash = hashPeer(peer);
        SelfDisposer head = hashTable[hash];
        SelfDisposer prev = null;
        for (SelfDisposer disposer = head; disposer != null;) {
            SelfDisposer next = disposer.next;
            if (disposer.peer == peer) {
                DOMWindowImpl node = (DOMWindowImpl) disposer.get();
                if (node != null) {
                    // the peer need to be deref'ed!
                    DOMWindowImpl.dispose(peer);
                    return node;
                }
                if (prev != null)
                    prev.next = next;
                else
                    hashTable[hash] = next;
                break;
            }
            prev = disposer;
            disposer = next;
        }
        DOMWindowImpl node = (DOMWindowImpl)createInterface(peer);
        SelfDisposer disposer = new SelfDisposer(node, peer);
        Disposer.addRecord(disposer);
        disposer.next = head;
        hashTable[hash] = disposer;
        if (3 * hashCount >= 2 * hashTable.length)
            rehash();
        hashCount++;
        return node;
    }

    static int test_getHashCount() {
        return hashCount;
    }

    private static void rehash() {
        SelfDisposer[] oldTable = hashTable;
        int oldLength = oldTable.length;
        SelfDisposer[] newTable = new SelfDisposer[2*oldLength];
        hashTable = newTable;
        for (int i = oldLength; --i >= 0; ) {
            for (SelfDisposer disposer = oldTable[i];
                    disposer != null;) {
                SelfDisposer next = disposer.next;
                int hash = hashPeer(disposer.peer);
                disposer.next = newTable[hash];
                newTable[hash] = disposer;
                disposer = next;
            }
        }
    }

    private static final class SelfDisposer extends Disposer.WeakDisposerRecord {
        private final long peer;
        SelfDisposer next;
        SelfDisposer(Object referent, final long _peer) {
            super(referent);
            peer = _peer;
        }

        public void dispose() {
            int hash = hashPeer(peer);
            SelfDisposer head = hashTable[hash];
            SelfDisposer prev = null;
            for (SelfDisposer disposer = head; disposer != null;) {
                SelfDisposer next = disposer.next;
                if (disposer.peer == peer) {
                    disposer.clear();
                    if (prev != null)
                        prev.next = next;
                    else
                        hashTable[hash] = next;
                    hashCount--;
                    break;
                }
                prev = disposer;
                disposer = next;
            }
            DOMWindowImpl.dispose(peer);
        }
    }

    DOMWindowImpl(long peer) {
        super(peer, JS_DOM_WINDOW_OBJECT);
    }

    static AbstractView createInterface(long peer) {
        if (peer == 0L) return null;
        return new DOMWindowImpl(peer);
    }

    static AbstractView create(long peer) {
        return getCachedImpl(peer);
    }

    static long getPeer(AbstractView arg) {
        return (arg == null) ? 0L : ((DOMWindowImpl)arg).getPeer();
    }

    native private static void dispose(long peer);

    static AbstractView getImpl(long peer) {
        return (AbstractView)create(peer);
    }


// Attributes
    public Element getFrameElement() {
        return ElementImpl.getImpl(getFrameElementImpl(getPeer()));
    }
    native static long getFrameElementImpl(long peer);

    public boolean getOffscreenBuffering() {
        return getOffscreenBufferingImpl(getPeer());
    }
    native static boolean getOffscreenBufferingImpl(long peer);

    public int getOuterHeight() {
        return getOuterHeightImpl(getPeer());
    }
    native static int getOuterHeightImpl(long peer);

    public int getOuterWidth() {
        return getOuterWidthImpl(getPeer());
    }
    native static int getOuterWidthImpl(long peer);

    public int getInnerHeight() {
        return getInnerHeightImpl(getPeer());
    }
    native static int getInnerHeightImpl(long peer);

    public int getInnerWidth() {
        return getInnerWidthImpl(getPeer());
    }
    native static int getInnerWidthImpl(long peer);

    public int getScreenX() {
        return getScreenXImpl(getPeer());
    }
    native static int getScreenXImpl(long peer);

    public int getScreenY() {
        return getScreenYImpl(getPeer());
    }
    native static int getScreenYImpl(long peer);

    public int getScreenLeft() {
        return getScreenLeftImpl(getPeer());
    }
    native static int getScreenLeftImpl(long peer);

    public int getScreenTop() {
        return getScreenTopImpl(getPeer());
    }
    native static int getScreenTopImpl(long peer);

    public int getScrollX() {
        return getScrollXImpl(getPeer());
    }
    native static int getScrollXImpl(long peer);

    public int getScrollY() {
        return getScrollYImpl(getPeer());
    }
    native static int getScrollYImpl(long peer);

    public int getPageXOffset() {
        return getPageXOffsetImpl(getPeer());
    }
    native static int getPageXOffsetImpl(long peer);

    public int getPageYOffset() {
        return getPageYOffsetImpl(getPeer());
    }
    native static int getPageYOffsetImpl(long peer);

    public boolean getClosed() {
        return getClosedImpl(getPeer());
    }
    native static boolean getClosedImpl(long peer);

    public int getLength() {
        return getLengthImpl(getPeer());
    }
    native static int getLengthImpl(long peer);

    public String getName() {
        return getNameImpl(getPeer());
    }
    native static String getNameImpl(long peer);

    public void setName(String value) {
        setNameImpl(getPeer(), value);
    }
    native static void setNameImpl(long peer, String value);

    public String getStatus() {
        return getStatusImpl(getPeer());
    }
    native static String getStatusImpl(long peer);

    public void setStatus(String value) {
        setStatusImpl(getPeer(), value);
    }
    native static void setStatusImpl(long peer, String value);

    public String getDefaultStatus() {
        return getDefaultStatusImpl(getPeer());
    }
    native static String getDefaultStatusImpl(long peer);

    public void setDefaultStatus(String value) {
        setDefaultStatusImpl(getPeer(), value);
    }
    native static void setDefaultStatusImpl(long peer, String value);

    public AbstractView getSelf() {
        return DOMWindowImpl.getImpl(getSelfImpl(getPeer()));
    }
    native static long getSelfImpl(long peer);

    public AbstractView getWindow() {
        return DOMWindowImpl.getImpl(getWindowImpl(getPeer()));
    }
    native static long getWindowImpl(long peer);

    public AbstractView getFrames() {
        return DOMWindowImpl.getImpl(getFramesImpl(getPeer()));
    }
    native static long getFramesImpl(long peer);

    public AbstractView getOpener() {
        return DOMWindowImpl.getImpl(getOpenerImpl(getPeer()));
    }
    native static long getOpenerImpl(long peer);

    public AbstractView getParent() {
        return DOMWindowImpl.getImpl(getParentImpl(getPeer()));
    }
    native static long getParentImpl(long peer);

    public AbstractView getTop() {
        return DOMWindowImpl.getImpl(getTopImpl(getPeer()));
    }
    native static long getTopImpl(long peer);

    public Document getDocumentEx() {
        return DocumentImpl.getImpl(getDocumentExImpl(getPeer()));
    }
    native static long getDocumentExImpl(long peer);

    public double getDevicePixelRatio() {
        return getDevicePixelRatioImpl(getPeer());
    }
    native static double getDevicePixelRatioImpl(long peer);

    public EventListener getOnanimationend() {
        return EventListenerImpl.getImpl(getOnanimationendImpl(getPeer()));
    }
    native static long getOnanimationendImpl(long peer);

    public void setOnanimationend(EventListener value) {
        setOnanimationendImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnanimationendImpl(long peer, long value);

    public EventListener getOnanimationiteration() {
        return EventListenerImpl.getImpl(getOnanimationiterationImpl(getPeer()));
    }
    native static long getOnanimationiterationImpl(long peer);

    public void setOnanimationiteration(EventListener value) {
        setOnanimationiterationImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnanimationiterationImpl(long peer, long value);

    public EventListener getOnanimationstart() {
        return EventListenerImpl.getImpl(getOnanimationstartImpl(getPeer()));
    }
    native static long getOnanimationstartImpl(long peer);

    public void setOnanimationstart(EventListener value) {
        setOnanimationstartImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnanimationstartImpl(long peer, long value);

    public EventListener getOntransitionend() {
        return EventListenerImpl.getImpl(getOntransitionendImpl(getPeer()));
    }
    native static long getOntransitionendImpl(long peer);

    public void setOntransitionend(EventListener value) {
        setOntransitionendImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOntransitionendImpl(long peer, long value);

    public EventListener getOnwebkitanimationend() {
        return EventListenerImpl.getImpl(getOnwebkitanimationendImpl(getPeer()));
    }
    native static long getOnwebkitanimationendImpl(long peer);

    public void setOnwebkitanimationend(EventListener value) {
        setOnwebkitanimationendImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnwebkitanimationendImpl(long peer, long value);

    public EventListener getOnwebkitanimationiteration() {
        return EventListenerImpl.getImpl(getOnwebkitanimationiterationImpl(getPeer()));
    }
    native static long getOnwebkitanimationiterationImpl(long peer);

    public void setOnwebkitanimationiteration(EventListener value) {
        setOnwebkitanimationiterationImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnwebkitanimationiterationImpl(long peer, long value);

    public EventListener getOnwebkitanimationstart() {
        return EventListenerImpl.getImpl(getOnwebkitanimationstartImpl(getPeer()));
    }
    native static long getOnwebkitanimationstartImpl(long peer);

    public void setOnwebkitanimationstart(EventListener value) {
        setOnwebkitanimationstartImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnwebkitanimationstartImpl(long peer, long value);

    public EventListener getOnwebkittransitionend() {
        return EventListenerImpl.getImpl(getOnwebkittransitionendImpl(getPeer()));
    }
    native static long getOnwebkittransitionendImpl(long peer);

    public void setOnwebkittransitionend(EventListener value) {
        setOnwebkittransitionendImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnwebkittransitionendImpl(long peer, long value);

    public EventListener getOnabort() {
        return EventListenerImpl.getImpl(getOnabortImpl(getPeer()));
    }
    native static long getOnabortImpl(long peer);

    public void setOnabort(EventListener value) {
        setOnabortImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnabortImpl(long peer, long value);

    public EventListener getOnblur() {
        return EventListenerImpl.getImpl(getOnblurImpl(getPeer()));
    }
    native static long getOnblurImpl(long peer);

    public void setOnblur(EventListener value) {
        setOnblurImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnblurImpl(long peer, long value);

    public EventListener getOncanplay() {
        return EventListenerImpl.getImpl(getOncanplayImpl(getPeer()));
    }
    native static long getOncanplayImpl(long peer);

    public void setOncanplay(EventListener value) {
        setOncanplayImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOncanplayImpl(long peer, long value);

    public EventListener getOncanplaythrough() {
        return EventListenerImpl.getImpl(getOncanplaythroughImpl(getPeer()));
    }
    native static long getOncanplaythroughImpl(long peer);

    public void setOncanplaythrough(EventListener value) {
        setOncanplaythroughImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOncanplaythroughImpl(long peer, long value);

    public EventListener getOnchange() {
        return EventListenerImpl.getImpl(getOnchangeImpl(getPeer()));
    }
    native static long getOnchangeImpl(long peer);

    public void setOnchange(EventListener value) {
        setOnchangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnchangeImpl(long peer, long value);

    public EventListener getOnclick() {
        return EventListenerImpl.getImpl(getOnclickImpl(getPeer()));
    }
    native static long getOnclickImpl(long peer);

    public void setOnclick(EventListener value) {
        setOnclickImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnclickImpl(long peer, long value);

    public EventListener getOncontextmenu() {
        return EventListenerImpl.getImpl(getOncontextmenuImpl(getPeer()));
    }
    native static long getOncontextmenuImpl(long peer);

    public void setOncontextmenu(EventListener value) {
        setOncontextmenuImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOncontextmenuImpl(long peer, long value);

    public EventListener getOndblclick() {
        return EventListenerImpl.getImpl(getOndblclickImpl(getPeer()));
    }
    native static long getOndblclickImpl(long peer);

    public void setOndblclick(EventListener value) {
        setOndblclickImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOndblclickImpl(long peer, long value);

    public EventListener getOndrag() {
        return EventListenerImpl.getImpl(getOndragImpl(getPeer()));
    }
    native static long getOndragImpl(long peer);

    public void setOndrag(EventListener value) {
        setOndragImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOndragImpl(long peer, long value);

    public EventListener getOndragend() {
        return EventListenerImpl.getImpl(getOndragendImpl(getPeer()));
    }
    native static long getOndragendImpl(long peer);

    public void setOndragend(EventListener value) {
        setOndragendImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOndragendImpl(long peer, long value);

    public EventListener getOndragenter() {
        return EventListenerImpl.getImpl(getOndragenterImpl(getPeer()));
    }
    native static long getOndragenterImpl(long peer);

    public void setOndragenter(EventListener value) {
        setOndragenterImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOndragenterImpl(long peer, long value);

    public EventListener getOndragleave() {
        return EventListenerImpl.getImpl(getOndragleaveImpl(getPeer()));
    }
    native static long getOndragleaveImpl(long peer);

    public void setOndragleave(EventListener value) {
        setOndragleaveImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOndragleaveImpl(long peer, long value);

    public EventListener getOndragover() {
        return EventListenerImpl.getImpl(getOndragoverImpl(getPeer()));
    }
    native static long getOndragoverImpl(long peer);

    public void setOndragover(EventListener value) {
        setOndragoverImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOndragoverImpl(long peer, long value);

    public EventListener getOndragstart() {
        return EventListenerImpl.getImpl(getOndragstartImpl(getPeer()));
    }
    native static long getOndragstartImpl(long peer);

    public void setOndragstart(EventListener value) {
        setOndragstartImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOndragstartImpl(long peer, long value);

    public EventListener getOndrop() {
        return EventListenerImpl.getImpl(getOndropImpl(getPeer()));
    }
    native static long getOndropImpl(long peer);

    public void setOndrop(EventListener value) {
        setOndropImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOndropImpl(long peer, long value);

    public EventListener getOndurationchange() {
        return EventListenerImpl.getImpl(getOndurationchangeImpl(getPeer()));
    }
    native static long getOndurationchangeImpl(long peer);

    public void setOndurationchange(EventListener value) {
        setOndurationchangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOndurationchangeImpl(long peer, long value);

    public EventListener getOnemptied() {
        return EventListenerImpl.getImpl(getOnemptiedImpl(getPeer()));
    }
    native static long getOnemptiedImpl(long peer);

    public void setOnemptied(EventListener value) {
        setOnemptiedImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnemptiedImpl(long peer, long value);

    public EventListener getOnended() {
        return EventListenerImpl.getImpl(getOnendedImpl(getPeer()));
    }
    native static long getOnendedImpl(long peer);

    public void setOnended(EventListener value) {
        setOnendedImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnendedImpl(long peer, long value);

    public EventListener getOnerror() {
        return EventListenerImpl.getImpl(getOnerrorImpl(getPeer()));
    }
    native static long getOnerrorImpl(long peer);

    public void setOnerror(EventListener value) {
        setOnerrorImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnerrorImpl(long peer, long value);

    public EventListener getOnfocus() {
        return EventListenerImpl.getImpl(getOnfocusImpl(getPeer()));
    }
    native static long getOnfocusImpl(long peer);

    public void setOnfocus(EventListener value) {
        setOnfocusImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnfocusImpl(long peer, long value);

    public EventListener getOninput() {
        return EventListenerImpl.getImpl(getOninputImpl(getPeer()));
    }
    native static long getOninputImpl(long peer);

    public void setOninput(EventListener value) {
        setOninputImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOninputImpl(long peer, long value);

    public EventListener getOninvalid() {
        return EventListenerImpl.getImpl(getOninvalidImpl(getPeer()));
    }
    native static long getOninvalidImpl(long peer);

    public void setOninvalid(EventListener value) {
        setOninvalidImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOninvalidImpl(long peer, long value);

    public EventListener getOnkeydown() {
        return EventListenerImpl.getImpl(getOnkeydownImpl(getPeer()));
    }
    native static long getOnkeydownImpl(long peer);

    public void setOnkeydown(EventListener value) {
        setOnkeydownImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnkeydownImpl(long peer, long value);

    public EventListener getOnkeypress() {
        return EventListenerImpl.getImpl(getOnkeypressImpl(getPeer()));
    }
    native static long getOnkeypressImpl(long peer);

    public void setOnkeypress(EventListener value) {
        setOnkeypressImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnkeypressImpl(long peer, long value);

    public EventListener getOnkeyup() {
        return EventListenerImpl.getImpl(getOnkeyupImpl(getPeer()));
    }
    native static long getOnkeyupImpl(long peer);

    public void setOnkeyup(EventListener value) {
        setOnkeyupImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnkeyupImpl(long peer, long value);

    public EventListener getOnload() {
        return EventListenerImpl.getImpl(getOnloadImpl(getPeer()));
    }
    native static long getOnloadImpl(long peer);

    public void setOnload(EventListener value) {
        setOnloadImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnloadImpl(long peer, long value);

    public EventListener getOnloadeddata() {
        return EventListenerImpl.getImpl(getOnloadeddataImpl(getPeer()));
    }
    native static long getOnloadeddataImpl(long peer);

    public void setOnloadeddata(EventListener value) {
        setOnloadeddataImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnloadeddataImpl(long peer, long value);

    public EventListener getOnloadedmetadata() {
        return EventListenerImpl.getImpl(getOnloadedmetadataImpl(getPeer()));
    }
    native static long getOnloadedmetadataImpl(long peer);

    public void setOnloadedmetadata(EventListener value) {
        setOnloadedmetadataImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnloadedmetadataImpl(long peer, long value);

    public EventListener getOnloadstart() {
        return EventListenerImpl.getImpl(getOnloadstartImpl(getPeer()));
    }
    native static long getOnloadstartImpl(long peer);

    public void setOnloadstart(EventListener value) {
        setOnloadstartImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnloadstartImpl(long peer, long value);

    public EventListener getOnmousedown() {
        return EventListenerImpl.getImpl(getOnmousedownImpl(getPeer()));
    }
    native static long getOnmousedownImpl(long peer);

    public void setOnmousedown(EventListener value) {
        setOnmousedownImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnmousedownImpl(long peer, long value);

    public EventListener getOnmouseenter() {
        return EventListenerImpl.getImpl(getOnmouseenterImpl(getPeer()));
    }
    native static long getOnmouseenterImpl(long peer);

    public void setOnmouseenter(EventListener value) {
        setOnmouseenterImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnmouseenterImpl(long peer, long value);

    public EventListener getOnmouseleave() {
        return EventListenerImpl.getImpl(getOnmouseleaveImpl(getPeer()));
    }
    native static long getOnmouseleaveImpl(long peer);

    public void setOnmouseleave(EventListener value) {
        setOnmouseleaveImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnmouseleaveImpl(long peer, long value);

    public EventListener getOnmousemove() {
        return EventListenerImpl.getImpl(getOnmousemoveImpl(getPeer()));
    }
    native static long getOnmousemoveImpl(long peer);

    public void setOnmousemove(EventListener value) {
        setOnmousemoveImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnmousemoveImpl(long peer, long value);

    public EventListener getOnmouseout() {
        return EventListenerImpl.getImpl(getOnmouseoutImpl(getPeer()));
    }
    native static long getOnmouseoutImpl(long peer);

    public void setOnmouseout(EventListener value) {
        setOnmouseoutImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnmouseoutImpl(long peer, long value);

    public EventListener getOnmouseover() {
        return EventListenerImpl.getImpl(getOnmouseoverImpl(getPeer()));
    }
    native static long getOnmouseoverImpl(long peer);

    public void setOnmouseover(EventListener value) {
        setOnmouseoverImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnmouseoverImpl(long peer, long value);

    public EventListener getOnmouseup() {
        return EventListenerImpl.getImpl(getOnmouseupImpl(getPeer()));
    }
    native static long getOnmouseupImpl(long peer);

    public void setOnmouseup(EventListener value) {
        setOnmouseupImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnmouseupImpl(long peer, long value);

    public EventListener getOnmousewheel() {
        return EventListenerImpl.getImpl(getOnmousewheelImpl(getPeer()));
    }
    native static long getOnmousewheelImpl(long peer);

    public void setOnmousewheel(EventListener value) {
        setOnmousewheelImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnmousewheelImpl(long peer, long value);

    public EventListener getOnpause() {
        return EventListenerImpl.getImpl(getOnpauseImpl(getPeer()));
    }
    native static long getOnpauseImpl(long peer);

    public void setOnpause(EventListener value) {
        setOnpauseImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnpauseImpl(long peer, long value);

    public EventListener getOnplay() {
        return EventListenerImpl.getImpl(getOnplayImpl(getPeer()));
    }
    native static long getOnplayImpl(long peer);

    public void setOnplay(EventListener value) {
        setOnplayImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnplayImpl(long peer, long value);

    public EventListener getOnplaying() {
        return EventListenerImpl.getImpl(getOnplayingImpl(getPeer()));
    }
    native static long getOnplayingImpl(long peer);

    public void setOnplaying(EventListener value) {
        setOnplayingImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnplayingImpl(long peer, long value);

    public EventListener getOnprogress() {
        return EventListenerImpl.getImpl(getOnprogressImpl(getPeer()));
    }
    native static long getOnprogressImpl(long peer);

    public void setOnprogress(EventListener value) {
        setOnprogressImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnprogressImpl(long peer, long value);

    public EventListener getOnratechange() {
        return EventListenerImpl.getImpl(getOnratechangeImpl(getPeer()));
    }
    native static long getOnratechangeImpl(long peer);

    public void setOnratechange(EventListener value) {
        setOnratechangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnratechangeImpl(long peer, long value);

    public EventListener getOnreset() {
        return EventListenerImpl.getImpl(getOnresetImpl(getPeer()));
    }
    native static long getOnresetImpl(long peer);

    public void setOnreset(EventListener value) {
        setOnresetImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnresetImpl(long peer, long value);

    public EventListener getOnresize() {
        return EventListenerImpl.getImpl(getOnresizeImpl(getPeer()));
    }
    native static long getOnresizeImpl(long peer);

    public void setOnresize(EventListener value) {
        setOnresizeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnresizeImpl(long peer, long value);

    public EventListener getOnscroll() {
        return EventListenerImpl.getImpl(getOnscrollImpl(getPeer()));
    }
    native static long getOnscrollImpl(long peer);

    public void setOnscroll(EventListener value) {
        setOnscrollImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnscrollImpl(long peer, long value);

    public EventListener getOnseeked() {
        return EventListenerImpl.getImpl(getOnseekedImpl(getPeer()));
    }
    native static long getOnseekedImpl(long peer);

    public void setOnseeked(EventListener value) {
        setOnseekedImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnseekedImpl(long peer, long value);

    public EventListener getOnseeking() {
        return EventListenerImpl.getImpl(getOnseekingImpl(getPeer()));
    }
    native static long getOnseekingImpl(long peer);

    public void setOnseeking(EventListener value) {
        setOnseekingImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnseekingImpl(long peer, long value);

    public EventListener getOnselect() {
        return EventListenerImpl.getImpl(getOnselectImpl(getPeer()));
    }
    native static long getOnselectImpl(long peer);

    public void setOnselect(EventListener value) {
        setOnselectImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnselectImpl(long peer, long value);

    public EventListener getOnstalled() {
        return EventListenerImpl.getImpl(getOnstalledImpl(getPeer()));
    }
    native static long getOnstalledImpl(long peer);

    public void setOnstalled(EventListener value) {
        setOnstalledImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnstalledImpl(long peer, long value);

    public EventListener getOnsubmit() {
        return EventListenerImpl.getImpl(getOnsubmitImpl(getPeer()));
    }
    native static long getOnsubmitImpl(long peer);

    public void setOnsubmit(EventListener value) {
        setOnsubmitImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnsubmitImpl(long peer, long value);

    public EventListener getOnsuspend() {
        return EventListenerImpl.getImpl(getOnsuspendImpl(getPeer()));
    }
    native static long getOnsuspendImpl(long peer);

    public void setOnsuspend(EventListener value) {
        setOnsuspendImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnsuspendImpl(long peer, long value);

    public EventListener getOntimeupdate() {
        return EventListenerImpl.getImpl(getOntimeupdateImpl(getPeer()));
    }
    native static long getOntimeupdateImpl(long peer);

    public void setOntimeupdate(EventListener value) {
        setOntimeupdateImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOntimeupdateImpl(long peer, long value);

    public EventListener getOnvolumechange() {
        return EventListenerImpl.getImpl(getOnvolumechangeImpl(getPeer()));
    }
    native static long getOnvolumechangeImpl(long peer);

    public void setOnvolumechange(EventListener value) {
        setOnvolumechangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnvolumechangeImpl(long peer, long value);

    public EventListener getOnwaiting() {
        return EventListenerImpl.getImpl(getOnwaitingImpl(getPeer()));
    }
    native static long getOnwaitingImpl(long peer);

    public void setOnwaiting(EventListener value) {
        setOnwaitingImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnwaitingImpl(long peer, long value);

    public EventListener getOnsearch() {
        return EventListenerImpl.getImpl(getOnsearchImpl(getPeer()));
    }
    native static long getOnsearchImpl(long peer);

    public void setOnsearch(EventListener value) {
        setOnsearchImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnsearchImpl(long peer, long value);

    public EventListener getOnwheel() {
        return EventListenerImpl.getImpl(getOnwheelImpl(getPeer()));
    }
    native static long getOnwheelImpl(long peer);

    public void setOnwheel(EventListener value) {
        setOnwheelImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnwheelImpl(long peer, long value);

    public EventListener getOnbeforeunload() {
        return EventListenerImpl.getImpl(getOnbeforeunloadImpl(getPeer()));
    }
    native static long getOnbeforeunloadImpl(long peer);

    public void setOnbeforeunload(EventListener value) {
        setOnbeforeunloadImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnbeforeunloadImpl(long peer, long value);

    public EventListener getOnhashchange() {
        return EventListenerImpl.getImpl(getOnhashchangeImpl(getPeer()));
    }
    native static long getOnhashchangeImpl(long peer);

    public void setOnhashchange(EventListener value) {
        setOnhashchangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnhashchangeImpl(long peer, long value);

    public EventListener getOnmessage() {
        return EventListenerImpl.getImpl(getOnmessageImpl(getPeer()));
    }
    native static long getOnmessageImpl(long peer);

    public void setOnmessage(EventListener value) {
        setOnmessageImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnmessageImpl(long peer, long value);

    public EventListener getOnoffline() {
        return EventListenerImpl.getImpl(getOnofflineImpl(getPeer()));
    }
    native static long getOnofflineImpl(long peer);

    public void setOnoffline(EventListener value) {
        setOnofflineImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnofflineImpl(long peer, long value);

    public EventListener getOnonline() {
        return EventListenerImpl.getImpl(getOnonlineImpl(getPeer()));
    }
    native static long getOnonlineImpl(long peer);

    public void setOnonline(EventListener value) {
        setOnonlineImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnonlineImpl(long peer, long value);

    public EventListener getOnpagehide() {
        return EventListenerImpl.getImpl(getOnpagehideImpl(getPeer()));
    }
    native static long getOnpagehideImpl(long peer);

    public void setOnpagehide(EventListener value) {
        setOnpagehideImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnpagehideImpl(long peer, long value);

    public EventListener getOnpageshow() {
        return EventListenerImpl.getImpl(getOnpageshowImpl(getPeer()));
    }
    native static long getOnpageshowImpl(long peer);

    public void setOnpageshow(EventListener value) {
        setOnpageshowImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnpageshowImpl(long peer, long value);

    public EventListener getOnpopstate() {
        return EventListenerImpl.getImpl(getOnpopstateImpl(getPeer()));
    }
    native static long getOnpopstateImpl(long peer);

    public void setOnpopstate(EventListener value) {
        setOnpopstateImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnpopstateImpl(long peer, long value);

    public EventListener getOnstorage() {
        return EventListenerImpl.getImpl(getOnstorageImpl(getPeer()));
    }
    native static long getOnstorageImpl(long peer);

    public void setOnstorage(EventListener value) {
        setOnstorageImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnstorageImpl(long peer, long value);

    public EventListener getOnunload() {
        return EventListenerImpl.getImpl(getOnunloadImpl(getPeer()));
    }
    native static long getOnunloadImpl(long peer);

    public void setOnunload(EventListener value) {
        setOnunloadImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnunloadImpl(long peer, long value);


// Functions
    public DOMSelectionImpl getSelection()
    {
        return DOMSelectionImpl.getImpl(getSelectionImpl(getPeer()));
    }
    native static long getSelectionImpl(long peer);


    public void focus()
    {
        focusImpl(getPeer());
    }
    native static void focusImpl(long peer);


    public void blur()
    {
        blurImpl(getPeer());
    }
    native static void blurImpl(long peer);


    public void close()
    {
        closeImpl(getPeer());
    }
    native static void closeImpl(long peer);


    public void print()
    {
        printImpl(getPeer());
    }
    native static void printImpl(long peer);


    public void stop()
    {
        stopImpl(getPeer());
    }
    native static void stopImpl(long peer);


    public void alert(String message)
    {
        alertImpl(getPeer()
            , message);
    }
    native static void alertImpl(long peer
        , String message);


    public boolean confirm(String message)
    {
        return confirmImpl(getPeer()
            , message);
    }
    native static boolean confirmImpl(long peer
        , String message);


    public String prompt(String message
        , String defaultValue)
    {
        return promptImpl(getPeer()
            , message
            , defaultValue);
    }
    native static String promptImpl(long peer
        , String message
        , String defaultValue);


    public boolean find(String string
        , boolean caseSensitive
        , boolean backwards
        , boolean wrap
        , boolean wholeWord
        , boolean searchInFrames
        , boolean showDialog)
    {
        return findImpl(getPeer()
            , string
            , caseSensitive
            , backwards
            , wrap
            , wholeWord
            , searchInFrames
            , showDialog);
    }
    native static boolean findImpl(long peer
        , String string
        , boolean caseSensitive
        , boolean backwards
        , boolean wrap
        , boolean wholeWord
        , boolean searchInFrames
        , boolean showDialog);


    public void scrollBy(int x
        , int y)
    {
        scrollByImpl(getPeer()
            , x
            , y);
    }
    native static void scrollByImpl(long peer
        , int x
        , int y);


    public void scrollTo(int x
        , int y)
    {
        scrollToImpl(getPeer()
            , x
            , y);
    }
    native static void scrollToImpl(long peer
        , int x
        , int y);


    public void scroll(int x
        , int y)
    {
        scrollImpl(getPeer()
            , x
            , y);
    }
    native static void scrollImpl(long peer
        , int x
        , int y);


    public void moveBy(float x
        , float y)
    {
        moveByImpl(getPeer()
            , x
            , y);
    }
    native static void moveByImpl(long peer
        , float x
        , float y);


    public void moveTo(float x
        , float y)
    {
        moveToImpl(getPeer()
            , x
            , y);
    }
    native static void moveToImpl(long peer
        , float x
        , float y);


    public void resizeBy(float x
        , float y)
    {
        resizeByImpl(getPeer()
            , x
            , y);
    }
    native static void resizeByImpl(long peer
        , float x
        , float y);


    public void resizeTo(float width
        , float height)
    {
        resizeToImpl(getPeer()
            , width
            , height);
    }
    native static void resizeToImpl(long peer
        , float width
        , float height);


    public CSSStyleDeclaration getComputedStyle(Element element
        , String pseudoElement)
    {
        return CSSStyleDeclarationImpl.getImpl(getComputedStyleImpl(getPeer()
            , ElementImpl.getPeer(element)
            , pseudoElement));
    }
    native static long getComputedStyleImpl(long peer
        , long element
        , String pseudoElement);


    public void captureEvents()
    {
        captureEventsImpl(getPeer());
    }
    native static void captureEventsImpl(long peer);


    public void releaseEvents()
    {
        releaseEventsImpl(getPeer());
    }
    native static void releaseEventsImpl(long peer);


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


    public String atob(String string) throws DOMException
    {
        return atobImpl(getPeer()
            , string);
    }
    native static String atobImpl(long peer
        , String string);


    public String btoa(String string) throws DOMException
    {
        return btoaImpl(getPeer()
            , string);
    }
    native static String btoaImpl(long peer
        , String string);


    public void clearTimeout(int handle)
    {
        clearTimeoutImpl(getPeer()
            , handle);
    }
    native static void clearTimeoutImpl(long peer
        , int handle);


    public void clearInterval(int handle)
    {
        clearIntervalImpl(getPeer()
            , handle);
    }
    native static void clearIntervalImpl(long peer
        , int handle);



//stubs
    public DocumentView getDocument() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

