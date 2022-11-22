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

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.html.HTMLCollection;

public class ElementImpl extends NodeImpl implements Element {
    ElementImpl(long peer) {
        super(peer);
    }

    static Element getImpl(long peer) {
        return (Element)create(peer);
    }

    native static boolean isHTMLElementImpl(long peer);


// Constants
    public static final int ALLOW_KEYBOARD_INPUT = 1;

// Attributes
    public String getTagName() {
        return getTagNameImpl(getPeer());
    }
    native static String getTagNameImpl(long peer);

    public NamedNodeMap getAttributes() {
        return NamedNodeMapImpl.getImpl(getAttributesImpl(getPeer()));
    }
    native static long getAttributesImpl(long peer);

    public CSSStyleDeclaration getStyle() {
        return CSSStyleDeclarationImpl.getImpl(getStyleImpl(getPeer()));
    }
    native static long getStyleImpl(long peer);

    public String getId() {
        return getIdImpl(getPeer());
    }
    native static String getIdImpl(long peer);

    public void setId(String value) {
        setIdImpl(getPeer(), value);
    }
    native static void setIdImpl(long peer, String value);

    public double getOffsetLeft() {
        return getOffsetLeftImpl(getPeer());
    }
    native static double getOffsetLeftImpl(long peer);

    public double getOffsetTop() {
        return getOffsetTopImpl(getPeer());
    }
    native static double getOffsetTopImpl(long peer);

    public double getOffsetWidth() {
        return getOffsetWidthImpl(getPeer());
    }
    native static double getOffsetWidthImpl(long peer);

    public double getOffsetHeight() {
        return getOffsetHeightImpl(getPeer());
    }
    native static double getOffsetHeightImpl(long peer);

    public double getClientLeft() {
        return getClientLeftImpl(getPeer());
    }
    native static double getClientLeftImpl(long peer);

    public double getClientTop() {
        return getClientTopImpl(getPeer());
    }
    native static double getClientTopImpl(long peer);

    public double getClientWidth() {
        return getClientWidthImpl(getPeer());
    }
    native static double getClientWidthImpl(long peer);

    public double getClientHeight() {
        return getClientHeightImpl(getPeer());
    }
    native static double getClientHeightImpl(long peer);

    public int getScrollLeft() {
        return getScrollLeftImpl(getPeer());
    }
    native static int getScrollLeftImpl(long peer);

    public void setScrollLeft(int value) {
        setScrollLeftImpl(getPeer(), value);
    }
    native static void setScrollLeftImpl(long peer, int value);

    public int getScrollTop() {
        return getScrollTopImpl(getPeer());
    }
    native static int getScrollTopImpl(long peer);

    public void setScrollTop(int value) {
        setScrollTopImpl(getPeer(), value);
    }
    native static void setScrollTopImpl(long peer, int value);

    public int getScrollWidth() {
        return getScrollWidthImpl(getPeer());
    }
    native static int getScrollWidthImpl(long peer);

    public int getScrollHeight() {
        return getScrollHeightImpl(getPeer());
    }
    native static int getScrollHeightImpl(long peer);

    public Element getOffsetParent() {
        return ElementImpl.getImpl(getOffsetParentImpl(getPeer()));
    }
    native static long getOffsetParentImpl(long peer);

    public String getInnerHTML() {
        return getInnerHTMLImpl(getPeer());
    }
    native static String getInnerHTMLImpl(long peer);

    public void setInnerHTML(String value) throws DOMException {
        setInnerHTMLImpl(getPeer(), value);
    }
    native static void setInnerHTMLImpl(long peer, String value);

    public String getOuterHTML() {
        return getOuterHTMLImpl(getPeer());
    }
    native static String getOuterHTMLImpl(long peer);

    public void setOuterHTML(String value) throws DOMException {
        setOuterHTMLImpl(getPeer(), value);
    }
    native static void setOuterHTMLImpl(long peer, String value);

    public String getClassName() {
        return getClassNameImpl(getPeer());
    }
    native static String getClassNameImpl(long peer);

    public void setClassName(String value) {
        setClassNameImpl(getPeer(), value);
    }
    native static void setClassNameImpl(long peer, String value);

    public EventListener getOnbeforecopy() {
        return EventListenerImpl.getImpl(getOnbeforecopyImpl(getPeer()));
    }
    native static long getOnbeforecopyImpl(long peer);

    public void setOnbeforecopy(EventListener value) {
        setOnbeforecopyImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnbeforecopyImpl(long peer, long value);

    public EventListener getOnbeforecut() {
        return EventListenerImpl.getImpl(getOnbeforecutImpl(getPeer()));
    }
    native static long getOnbeforecutImpl(long peer);

    public void setOnbeforecut(EventListener value) {
        setOnbeforecutImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnbeforecutImpl(long peer, long value);

    public EventListener getOnbeforepaste() {
        return EventListenerImpl.getImpl(getOnbeforepasteImpl(getPeer()));
    }
    native static long getOnbeforepasteImpl(long peer);

    public void setOnbeforepaste(EventListener value) {
        setOnbeforepasteImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnbeforepasteImpl(long peer, long value);

    public EventListener getOncopy() {
        return EventListenerImpl.getImpl(getOncopyImpl(getPeer()));
    }
    native static long getOncopyImpl(long peer);

    public void setOncopy(EventListener value) {
        setOncopyImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOncopyImpl(long peer, long value);

    public EventListener getOncut() {
        return EventListenerImpl.getImpl(getOncutImpl(getPeer()));
    }
    native static long getOncutImpl(long peer);

    public void setOncut(EventListener value) {
        setOncutImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOncutImpl(long peer, long value);

    public EventListener getOnpaste() {
        return EventListenerImpl.getImpl(getOnpasteImpl(getPeer()));
    }
    native static long getOnpasteImpl(long peer);

    public void setOnpaste(EventListener value) {
        setOnpasteImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnpasteImpl(long peer, long value);

    public EventListener getOnselectstart() {
        return EventListenerImpl.getImpl(getOnselectstartImpl(getPeer()));
    }
    native static long getOnselectstartImpl(long peer);

    public void setOnselectstart(EventListener value) {
        setOnselectstartImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnselectstartImpl(long peer, long value);

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

    public EventListener getOnfocusin() {
        return EventListenerImpl.getImpl(getOnfocusinImpl(getPeer()));
    }
    native static long getOnfocusinImpl(long peer);

    public void setOnfocusin(EventListener value) {
        setOnfocusinImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnfocusinImpl(long peer, long value);

    public EventListener getOnfocusout() {
        return EventListenerImpl.getImpl(getOnfocusoutImpl(getPeer()));
    }
    native static long getOnfocusoutImpl(long peer);

    public void setOnfocusout(EventListener value) {
        setOnfocusoutImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnfocusoutImpl(long peer, long value);

    public EventListener getOnbeforeload() {
        return EventListenerImpl.getImpl(getOnbeforeloadImpl(getPeer()));
    }
    native static long getOnbeforeloadImpl(long peer);

    public void setOnbeforeload(EventListener value) {
        setOnbeforeloadImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    native static void setOnbeforeloadImpl(long peer, long value);

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

    public Element getPreviousElementSibling() {
        return ElementImpl.getImpl(getPreviousElementSiblingImpl(getPeer()));
    }
    native static long getPreviousElementSiblingImpl(long peer);

    public Element getNextElementSibling() {
        return ElementImpl.getImpl(getNextElementSiblingImpl(getPeer()));
    }
    native static long getNextElementSiblingImpl(long peer);

    public HTMLCollection getChildren() {
        return HTMLCollectionImpl.getImpl(getChildrenImpl(getPeer()));
    }
    native static long getChildrenImpl(long peer);

    public Element getFirstElementChild() {
        return ElementImpl.getImpl(getFirstElementChildImpl(getPeer()));
    }
    native static long getFirstElementChildImpl(long peer);

    public Element getLastElementChild() {
        return ElementImpl.getImpl(getLastElementChildImpl(getPeer()));
    }
    native static long getLastElementChildImpl(long peer);

    public int getChildElementCount() {
        return getChildElementCountImpl(getPeer());
    }
    native static int getChildElementCountImpl(long peer);


// Functions
    public String getAttribute(String name)
    {
        return getAttributeImpl(getPeer()
            , name);
    }
    native static String getAttributeImpl(long peer
        , String name);


    public void setAttribute(String name
        , String value) throws DOMException
    {
        setAttributeImpl(getPeer()
            , name
            , value);
    }
    native static void setAttributeImpl(long peer
        , String name
        , String value);


    public void removeAttribute(String name)
    {
        removeAttributeImpl(getPeer()
            , name);
    }
    native static void removeAttributeImpl(long peer
        , String name);


    public Attr getAttributeNode(String name)
    {
        return AttrImpl.getImpl(getAttributeNodeImpl(getPeer()
            , name));
    }
    native static long getAttributeNodeImpl(long peer
        , String name);


    public Attr setAttributeNode(Attr newAttr) throws DOMException
    {
        return AttrImpl.getImpl(setAttributeNodeImpl(getPeer()
            , AttrImpl.getPeer(newAttr)));
    }
    native static long setAttributeNodeImpl(long peer
        , long newAttr);


    public Attr removeAttributeNode(Attr oldAttr) throws DOMException
    {
        return AttrImpl.getImpl(removeAttributeNodeImpl(getPeer()
            , AttrImpl.getPeer(oldAttr)));
    }
    native static long removeAttributeNodeImpl(long peer
        , long oldAttr);


    public NodeList getElementsByTagName(String name)
    {
        return NodeListImpl.getImpl(getElementsByTagNameImpl(getPeer()
            , name));
    }
    native static long getElementsByTagNameImpl(long peer
        , String name);


    public boolean hasAttributes()
    {
        return hasAttributesImpl(getPeer());
    }
    native static boolean hasAttributesImpl(long peer);


    public String getAttributeNS(String namespaceURI
        , String localName)
    {
        return getAttributeNSImpl(getPeer()
            , namespaceURI
            , localName);
    }
    native static String getAttributeNSImpl(long peer
        , String namespaceURI
        , String localName);


    public void setAttributeNS(String namespaceURI
        , String qualifiedName
        , String value) throws DOMException
    {
        setAttributeNSImpl(getPeer()
            , namespaceURI
            , qualifiedName
            , value);
    }
    native static void setAttributeNSImpl(long peer
        , String namespaceURI
        , String qualifiedName
        , String value);


    public void removeAttributeNS(String namespaceURI
        , String localName)
    {
        removeAttributeNSImpl(getPeer()
            , namespaceURI
            , localName);
    }
    native static void removeAttributeNSImpl(long peer
        , String namespaceURI
        , String localName);


    public NodeList getElementsByTagNameNS(String namespaceURI
        , String localName)
    {
        return NodeListImpl.getImpl(getElementsByTagNameNSImpl(getPeer()
            , namespaceURI
            , localName));
    }
    native static long getElementsByTagNameNSImpl(long peer
        , String namespaceURI
        , String localName);


    public Attr getAttributeNodeNS(String namespaceURI
        , String localName)
    {
        return AttrImpl.getImpl(getAttributeNodeNSImpl(getPeer()
            , namespaceURI
            , localName));
    }
    native static long getAttributeNodeNSImpl(long peer
        , String namespaceURI
        , String localName);


    public Attr setAttributeNodeNS(Attr newAttr) throws DOMException
    {
        return AttrImpl.getImpl(setAttributeNodeNSImpl(getPeer()
            , AttrImpl.getPeer(newAttr)));
    }
    native static long setAttributeNodeNSImpl(long peer
        , long newAttr);


    public boolean hasAttribute(String name)
    {
        return hasAttributeImpl(getPeer()
            , name);
    }
    native static boolean hasAttributeImpl(long peer
        , String name);


    public boolean hasAttributeNS(String namespaceURI
        , String localName)
    {
        return hasAttributeNSImpl(getPeer()
            , namespaceURI
            , localName);
    }
    native static boolean hasAttributeNSImpl(long peer
        , String namespaceURI
        , String localName);


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


    public void scrollIntoView(boolean alignWithTop)
    {
        scrollIntoViewImpl(getPeer()
            , alignWithTop);
    }
    native static void scrollIntoViewImpl(long peer
        , boolean alignWithTop);


    public void scrollIntoViewIfNeeded(boolean centerIfNeeded)
    {
        scrollIntoViewIfNeededImpl(getPeer()
            , centerIfNeeded);
    }
    native static void scrollIntoViewIfNeededImpl(long peer
        , boolean centerIfNeeded);


    public void scrollByLines(int lines)
    {
        scrollByLinesImpl(getPeer()
            , lines);
    }
    native static void scrollByLinesImpl(long peer
        , int lines);


    public void scrollByPages(int pages)
    {
        scrollByPagesImpl(getPeer()
            , pages);
    }
    native static void scrollByPagesImpl(long peer
        , int pages);


    public HTMLCollection getElementsByClassName(String name)
    {
        return HTMLCollectionImpl.getImpl(getElementsByClassNameImpl(getPeer()
            , name));
    }
    native static long getElementsByClassNameImpl(long peer
        , String name);


    public boolean matches(String selectors) throws DOMException
    {
        return matchesImpl(getPeer()
            , selectors);
    }
    native static boolean matchesImpl(long peer
        , String selectors);


    public Element closest(String selectors) throws DOMException
    {
        return ElementImpl.getImpl(closestImpl(getPeer()
            , selectors));
    }
    native static long closestImpl(long peer
        , String selectors);


    public boolean webkitMatchesSelector(String selectors) throws DOMException
    {
        return webkitMatchesSelectorImpl(getPeer()
            , selectors);
    }
    native static boolean webkitMatchesSelectorImpl(long peer
        , String selectors);


    public void webkitRequestFullScreen(short flags)
    {
        webkitRequestFullScreenImpl(getPeer()
            , flags);
    }
    native static void webkitRequestFullScreenImpl(long peer
        , short flags);


    public void webkitRequestFullscreen()
    {
        webkitRequestFullscreenImpl(getPeer());
    }
    native static void webkitRequestFullscreenImpl(long peer);


    public void remove() throws DOMException
    {
        removeImpl(getPeer());
    }
    native static void removeImpl(long peer);


    public Element querySelector(String selectors) throws DOMException
    {
        return ElementImpl.getImpl(querySelectorImpl(getPeer()
            , selectors));
    }
    native static long querySelectorImpl(long peer
        , String selectors);


    public NodeList querySelectorAll(String selectors) throws DOMException
    {
        return NodeListImpl.getImpl(querySelectorAllImpl(getPeer()
            , selectors));
    }
    native static long querySelectorAllImpl(long peer
        , String selectors);



//stubs
    public void setIdAttribute(String name, boolean isId) throws DOMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public TypeInfo getSchemaTypeInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public void setIdAttributeNS(String namespaceURI, String localName, boolean isId) throws DOMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

