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

    static boolean isHTMLElementImpl(long peer) {
        return ElementNative.isHTMLElement(peer);
    }


// Constants
    public static final int ALLOW_KEYBOARD_INPUT = 1;

// Attributes
    @Override
    public String getTagName() {
        return getTagNameImpl(getPeer());
    }
    static String getTagNameImpl(long peer) {
        return ElementNative.getTagName(peer);
    }

    @Override
    public NamedNodeMap getAttributes() {
        return NamedNodeMapImpl.getImpl(getAttributesImpl(getPeer()));
    }
    static long getAttributesImpl(long peer) {
        return ElementNative.getAttributes(peer);
    }

    public CSSStyleDeclaration getStyle() {
        return CSSStyleDeclarationImpl.getImpl(getStyleImpl(getPeer()));
    }
    static long getStyleImpl(long peer) {
        return ElementNative.getStyle(peer);
    }

    public String getId() {
        return getIdImpl(getPeer());
    }
    static String getIdImpl(long peer) {
        return ElementNative.getId(peer);
    }

    public void setId(String value) {
        setIdImpl(getPeer(), value);
    }
    static void setIdImpl(long peer, String value) {
        ElementNative.setId(peer, value);
    }

    public double getOffsetLeft() {
        return getOffsetLeftImpl(getPeer());
    }
    static double getOffsetLeftImpl(long peer) {
        return ElementNative.getOffsetLeft(peer);
    }

    public double getOffsetTop() {
        return getOffsetTopImpl(getPeer());
    }
    static double getOffsetTopImpl(long peer) {
        return ElementNative.getOffsetTop(peer);
    }

    public double getOffsetWidth() {
        return getOffsetWidthImpl(getPeer());
    }
    static double getOffsetWidthImpl(long peer) {
        return ElementNative.getOffsetWidth(peer);
    }

    public double getOffsetHeight() {
        return getOffsetHeightImpl(getPeer());
    }
    static double getOffsetHeightImpl(long peer) {
        return ElementNative.getOffsetHeight(peer);
    }

    public double getClientLeft() {
        return getClientLeftImpl(getPeer());
    }
    static double getClientLeftImpl(long peer) {
        return ElementNative.getClientLeft(peer);
    }

    public double getClientTop() {
        return getClientTopImpl(getPeer());
    }
    static double getClientTopImpl(long peer) {
        return ElementNative.getClientTop(peer);
    }

    public double getClientWidth() {
        return getClientWidthImpl(getPeer());
    }
    static double getClientWidthImpl(long peer) {
        return ElementNative.getClientWidth(peer);
    }

    public double getClientHeight() {
        return getClientHeightImpl(getPeer());
    }
    static double getClientHeightImpl(long peer) {
        return ElementNative.getClientHeight(peer);
    }

    public int getScrollLeft() {
        return getScrollLeftImpl(getPeer());
    }
    static int getScrollLeftImpl(long peer) {
        return ElementNative.getScrollLeft(peer);
    }

    public void setScrollLeft(int value) {
        setScrollLeftImpl(getPeer(), value);
    }
    static void setScrollLeftImpl(long peer, int value) {
        ElementNative.setScrollLeft(peer, value);
    }

    public int getScrollTop() {
        return getScrollTopImpl(getPeer());
    }
    static int getScrollTopImpl(long peer) {
        return ElementNative.getScrollTop(peer);
    }

    public void setScrollTop(int value) {
        setScrollTopImpl(getPeer(), value);
    }
    static void setScrollTopImpl(long peer, int value) {
        ElementNative.setScrollTop(peer, value);
    }

    public int getScrollWidth() {
        return getScrollWidthImpl(getPeer());
    }
    static int getScrollWidthImpl(long peer) {
        return ElementNative.getScrollWidth(peer);
    }

    public int getScrollHeight() {
        return getScrollHeightImpl(getPeer());
    }
    static int getScrollHeightImpl(long peer) {
        return ElementNative.getScrollHeight(peer);
    }

    public Element getOffsetParent() {
        return ElementImpl.getImpl(getOffsetParentImpl(getPeer()));
    }
    static long getOffsetParentImpl(long peer) {
        return ElementNative.getOffsetParent(peer);
    }

    public String getInnerHTML() {
        return getInnerHTMLImpl(getPeer());
    }
    static String getInnerHTMLImpl(long peer) {
        return ElementNative.getInnerHTML(peer);
    }

    public void setInnerHTML(String value) throws DOMException {
        setInnerHTMLImpl(getPeer(), value);
    }
    static void setInnerHTMLImpl(long peer, String value) {
        ElementNative.setInnerHTML(peer, value);
    }

    public String getOuterHTML() {
        return getOuterHTMLImpl(getPeer());
    }
    static String getOuterHTMLImpl(long peer) {
        return ElementNative.getOuterHTML(peer);
    }

    public void setOuterHTML(String value) throws DOMException {
        setOuterHTMLImpl(getPeer(), value);
    }
    static void setOuterHTMLImpl(long peer, String value) {
        ElementNative.setOuterHTML(peer, value);
    }

    public String getClassName() {
        return getClassNameImpl(getPeer());
    }
    static String getClassNameImpl(long peer) {
        return ElementNative.getClassName(peer);
    }

    public void setClassName(String value) {
        setClassNameImpl(getPeer(), value);
    }
    static void setClassNameImpl(long peer, String value) {
        ElementNative.setClassName(peer, value);
    }

    public EventListener getOnbeforecopy() {
        return EventListenerImpl.getImpl(getOnbeforecopyImpl(getPeer()));
    }
    static long getOnbeforecopyImpl(long peer) {
        return ElementNative.getOnbeforecopy(peer);
    }

    public void setOnbeforecopy(EventListener value) {
        setOnbeforecopyImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnbeforecopyImpl(long peer, long value) {
        ElementNative.setOnbeforecopy(peer, value);
    }

    public EventListener getOnbeforecut() {
        return EventListenerImpl.getImpl(getOnbeforecutImpl(getPeer()));
    }
    static long getOnbeforecutImpl(long peer) {
        return ElementNative.getOnbeforecut(peer);
    }

    public void setOnbeforecut(EventListener value) {
        setOnbeforecutImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnbeforecutImpl(long peer, long value) {
        ElementNative.setOnbeforecut(peer, value);
    }

    public EventListener getOnbeforepaste() {
        return EventListenerImpl.getImpl(getOnbeforepasteImpl(getPeer()));
    }
    static long getOnbeforepasteImpl(long peer) {
        return ElementNative.getOnbeforepaste(peer);
    }

    public void setOnbeforepaste(EventListener value) {
        setOnbeforepasteImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnbeforepasteImpl(long peer, long value) {
        ElementNative.setOnbeforepaste(peer, value);
    }

    public EventListener getOncopy() {
        return EventListenerImpl.getImpl(getOncopyImpl(getPeer()));
    }
    static long getOncopyImpl(long peer) {
        return ElementNative.getOncopy(peer);
    }

    public void setOncopy(EventListener value) {
        setOncopyImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOncopyImpl(long peer, long value) {
        ElementNative.setOncopy(peer, value);
    }

    public EventListener getOncut() {
        return EventListenerImpl.getImpl(getOncutImpl(getPeer()));
    }
    static long getOncutImpl(long peer) {
        return ElementNative.getOncut(peer);
    }

    public void setOncut(EventListener value) {
        setOncutImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOncutImpl(long peer, long value) {
        ElementNative.setOncut(peer, value);
    }

    public EventListener getOnpaste() {
        return EventListenerImpl.getImpl(getOnpasteImpl(getPeer()));
    }
    static long getOnpasteImpl(long peer) {
        return ElementNative.getOnpaste(peer);
    }

    public void setOnpaste(EventListener value) {
        setOnpasteImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnpasteImpl(long peer, long value) {
        ElementNative.setOnpaste(peer, value);
    }

    public EventListener getOnselectstart() {
        return EventListenerImpl.getImpl(getOnselectstartImpl(getPeer()));
    }
    static long getOnselectstartImpl(long peer) {
        return ElementNative.getOnselectstart(peer);
    }

    public void setOnselectstart(EventListener value) {
        setOnselectstartImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnselectstartImpl(long peer, long value) {
        ElementNative.setOnselectstart(peer, value);
    }

    public EventListener getOnanimationend() {
        return EventListenerImpl.getImpl(getOnanimationendImpl(getPeer()));
    }
    static long getOnanimationendImpl(long peer) {
        return ElementNative.getOnanimationend(peer);
    }

    public void setOnanimationend(EventListener value) {
        setOnanimationendImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnanimationendImpl(long peer, long value) {
        ElementNative.setOnanimationend(peer, value);
    }

    public EventListener getOnanimationiteration() {
        return EventListenerImpl.getImpl(getOnanimationiterationImpl(getPeer()));
    }
    static long getOnanimationiterationImpl(long peer) {
        return ElementNative.getOnanimationiteration(peer);
    }

    public void setOnanimationiteration(EventListener value) {
        setOnanimationiterationImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnanimationiterationImpl(long peer, long value) {
        ElementNative.setOnanimationiteration(peer, value);
    }

    public EventListener getOnanimationstart() {
        return EventListenerImpl.getImpl(getOnanimationstartImpl(getPeer()));
    }
    static long getOnanimationstartImpl(long peer) {
        return ElementNative.getOnanimationstart(peer);
    }

    public void setOnanimationstart(EventListener value) {
        setOnanimationstartImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnanimationstartImpl(long peer, long value) {
        ElementNative.setOnanimationstart(peer, value);
    }

    public EventListener getOntransitionend() {
        return EventListenerImpl.getImpl(getOntransitionendImpl(getPeer()));
    }
    static long getOntransitionendImpl(long peer) {
        return ElementNative.getOntransitionend(peer);
    }

    public void setOntransitionend(EventListener value) {
        setOntransitionendImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOntransitionendImpl(long peer, long value) {
        ElementNative.setOntransitionend(peer, value);
    }

    public EventListener getOnwebkitanimationend() {
        return EventListenerImpl.getImpl(getOnwebkitanimationendImpl(getPeer()));
    }
    static long getOnwebkitanimationendImpl(long peer) {
        return ElementNative.getOnwebkitanimationend(peer);
    }

    public void setOnwebkitanimationend(EventListener value) {
        setOnwebkitanimationendImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnwebkitanimationendImpl(long peer, long value) {
        ElementNative.setOnwebkitanimationend(peer, value);
    }

    public EventListener getOnwebkitanimationiteration() {
        return EventListenerImpl.getImpl(getOnwebkitanimationiterationImpl(getPeer()));
    }
    static long getOnwebkitanimationiterationImpl(long peer) {
        return ElementNative.getOnwebkitanimationiteration(peer);
    }

    public void setOnwebkitanimationiteration(EventListener value) {
        setOnwebkitanimationiterationImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnwebkitanimationiterationImpl(long peer, long value) {
        ElementNative.setOnwebkitanimationiteration(peer, value);
    }

    public EventListener getOnwebkitanimationstart() {
        return EventListenerImpl.getImpl(getOnwebkitanimationstartImpl(getPeer()));
    }
    static long getOnwebkitanimationstartImpl(long peer) {
        return ElementNative.getOnwebkitanimationstart(peer);
    }

    public void setOnwebkitanimationstart(EventListener value) {
        setOnwebkitanimationstartImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnwebkitanimationstartImpl(long peer, long value) {
        ElementNative.setOnwebkitanimationstart(peer, value);
    }

    public EventListener getOnwebkittransitionend() {
        return EventListenerImpl.getImpl(getOnwebkittransitionendImpl(getPeer()));
    }
    static long getOnwebkittransitionendImpl(long peer) {
        return ElementNative.getOnwebkittransitionend(peer);
    }

    public void setOnwebkittransitionend(EventListener value) {
        setOnwebkittransitionendImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnwebkittransitionendImpl(long peer, long value) {
        ElementNative.setOnwebkittransitionend(peer, value);
    }

    public EventListener getOnfocusin() {
        return EventListenerImpl.getImpl(getOnfocusinImpl(getPeer()));
    }
    static long getOnfocusinImpl(long peer) {
        return ElementNative.getOnfocusin(peer);
    }

    public void setOnfocusin(EventListener value) {
        setOnfocusinImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnfocusinImpl(long peer, long value) {
        ElementNative.setOnfocusin(peer, value);
    }

    public EventListener getOnfocusout() {
        return EventListenerImpl.getImpl(getOnfocusoutImpl(getPeer()));
    }
    static long getOnfocusoutImpl(long peer) {
        return ElementNative.getOnfocusout(peer);
    }

    public void setOnfocusout(EventListener value) {
        setOnfocusoutImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnfocusoutImpl(long peer, long value) {
        ElementNative.setOnfocusout(peer, value);
    }

    public EventListener getOnbeforeload() {
        return EventListenerImpl.getImpl(getOnbeforeloadImpl(getPeer()));
    }
    static long getOnbeforeloadImpl(long peer) {
        return ElementNative.getOnbeforeload(peer);
    }

    public void setOnbeforeload(EventListener value) {
        setOnbeforeloadImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnbeforeloadImpl(long peer, long value) {
        ElementNative.setOnbeforeload(peer, value);
    }

    public EventListener getOnabort() {
        return EventListenerImpl.getImpl(getOnabortImpl(getPeer()));
    }
    static long getOnabortImpl(long peer) {
        return ElementNative.getOnabort(peer);
    }

    public void setOnabort(EventListener value) {
        setOnabortImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnabortImpl(long peer, long value) {
        ElementNative.setOnabort(peer, value);
    }

    public EventListener getOnblur() {
        return EventListenerImpl.getImpl(getOnblurImpl(getPeer()));
    }
    static long getOnblurImpl(long peer) {
        return ElementNative.getOnblur(peer);
    }

    public void setOnblur(EventListener value) {
        setOnblurImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnblurImpl(long peer, long value) {
        ElementNative.setOnblur(peer, value);
    }

    public EventListener getOncanplay() {
        return EventListenerImpl.getImpl(getOncanplayImpl(getPeer()));
    }
    static long getOncanplayImpl(long peer) {
        return ElementNative.getOncanplay(peer);
    }

    public void setOncanplay(EventListener value) {
        setOncanplayImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOncanplayImpl(long peer, long value) {
        ElementNative.setOncanplay(peer, value);
    }

    public EventListener getOncanplaythrough() {
        return EventListenerImpl.getImpl(getOncanplaythroughImpl(getPeer()));
    }
    static long getOncanplaythroughImpl(long peer) {
        return ElementNative.getOncanplaythrough(peer);
    }

    public void setOncanplaythrough(EventListener value) {
        setOncanplaythroughImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOncanplaythroughImpl(long peer, long value) {
        ElementNative.setOncanplaythrough(peer, value);
    }

    public EventListener getOnchange() {
        return EventListenerImpl.getImpl(getOnchangeImpl(getPeer()));
    }
    static long getOnchangeImpl(long peer) {
        return ElementNative.getOnchange(peer);
    }

    public void setOnchange(EventListener value) {
        setOnchangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnchangeImpl(long peer, long value) {
        ElementNative.setOnchange(peer, value);
    }

    public EventListener getOnclick() {
        return EventListenerImpl.getImpl(getOnclickImpl(getPeer()));
    }
    static long getOnclickImpl(long peer) {
        return ElementNative.getOnclick(peer);
    }

    public void setOnclick(EventListener value) {
        setOnclickImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnclickImpl(long peer, long value) {
        ElementNative.setOnclick(peer, value);
    }

    public EventListener getOncontextmenu() {
        return EventListenerImpl.getImpl(getOncontextmenuImpl(getPeer()));
    }
    static long getOncontextmenuImpl(long peer) {
        return ElementNative.getOncontextmenu(peer);
    }

    public void setOncontextmenu(EventListener value) {
        setOncontextmenuImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOncontextmenuImpl(long peer, long value) {
        ElementNative.setOncontextmenu(peer, value);
    }

    public EventListener getOndblclick() {
        return EventListenerImpl.getImpl(getOndblclickImpl(getPeer()));
    }
    static long getOndblclickImpl(long peer) {
        return ElementNative.getOndblclick(peer);
    }

    public void setOndblclick(EventListener value) {
        setOndblclickImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOndblclickImpl(long peer, long value) {
        ElementNative.setOndblclick(peer, value);
    }

    public EventListener getOndrag() {
        return EventListenerImpl.getImpl(getOndragImpl(getPeer()));
    }
    static long getOndragImpl(long peer) {
        return ElementNative.getOndrag(peer);
    }

    public void setOndrag(EventListener value) {
        setOndragImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOndragImpl(long peer, long value) {
        ElementNative.setOndrag(peer, value);
    }

    public EventListener getOndragend() {
        return EventListenerImpl.getImpl(getOndragendImpl(getPeer()));
    }
    static long getOndragendImpl(long peer) {
        return ElementNative.getOndragend(peer);
    }

    public void setOndragend(EventListener value) {
        setOndragendImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOndragendImpl(long peer, long value) {
        ElementNative.setOndragend(peer, value);
    }

    public EventListener getOndragenter() {
        return EventListenerImpl.getImpl(getOndragenterImpl(getPeer()));
    }
    static long getOndragenterImpl(long peer) {
        return ElementNative.getOndragenter(peer);
    }

    public void setOndragenter(EventListener value) {
        setOndragenterImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOndragenterImpl(long peer, long value) {
        ElementNative.setOndragenter(peer, value);
    }

    public EventListener getOndragleave() {
        return EventListenerImpl.getImpl(getOndragleaveImpl(getPeer()));
    }
    static long getOndragleaveImpl(long peer) {
        return ElementNative.getOndragleave(peer);
    }

    public void setOndragleave(EventListener value) {
        setOndragleaveImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOndragleaveImpl(long peer, long value) {
        ElementNative.setOndragleave(peer, value);
    }

    public EventListener getOndragover() {
        return EventListenerImpl.getImpl(getOndragoverImpl(getPeer()));
    }
    static long getOndragoverImpl(long peer) {
        return ElementNative.getOndragover(peer);
    }

    public void setOndragover(EventListener value) {
        setOndragoverImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOndragoverImpl(long peer, long value) {
        ElementNative.setOndragover(peer, value);
    }

    public EventListener getOndragstart() {
        return EventListenerImpl.getImpl(getOndragstartImpl(getPeer()));
    }
    static long getOndragstartImpl(long peer) {
        return ElementNative.getOndragstart(peer);
    }

    public void setOndragstart(EventListener value) {
        setOndragstartImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOndragstartImpl(long peer, long value) {
        ElementNative.setOndragstart(peer, value);
    }

    public EventListener getOndrop() {
        return EventListenerImpl.getImpl(getOndropImpl(getPeer()));
    }
    static long getOndropImpl(long peer) {
        return ElementNative.getOndrop(peer);
    }

    public void setOndrop(EventListener value) {
        setOndropImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOndropImpl(long peer, long value) {
        ElementNative.setOndrop(peer, value);
    }

    public EventListener getOndurationchange() {
        return EventListenerImpl.getImpl(getOndurationchangeImpl(getPeer()));
    }
    static long getOndurationchangeImpl(long peer) {
        return ElementNative.getOndurationchange(peer);
    }

    public void setOndurationchange(EventListener value) {
        setOndurationchangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOndurationchangeImpl(long peer, long value) {
        ElementNative.setOndurationchange(peer, value);
    }

    public EventListener getOnemptied() {
        return EventListenerImpl.getImpl(getOnemptiedImpl(getPeer()));
    }
    static long getOnemptiedImpl(long peer) {
        return ElementNative.getOnemptied(peer);
    }

    public void setOnemptied(EventListener value) {
        setOnemptiedImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnemptiedImpl(long peer, long value) {
        ElementNative.setOnemptied(peer, value);
    }

    public EventListener getOnended() {
        return EventListenerImpl.getImpl(getOnendedImpl(getPeer()));
    }
    static long getOnendedImpl(long peer) {
        return ElementNative.getOnended(peer);
    }

    public void setOnended(EventListener value) {
        setOnendedImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnendedImpl(long peer, long value) {
        ElementNative.setOnended(peer, value);
    }

    public EventListener getOnerror() {
        return EventListenerImpl.getImpl(getOnerrorImpl(getPeer()));
    }
    static long getOnerrorImpl(long peer) {
        return ElementNative.getOnerror(peer);
    }

    public void setOnerror(EventListener value) {
        setOnerrorImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnerrorImpl(long peer, long value) {
        ElementNative.setOnerror(peer, value);
    }

    public EventListener getOnfocus() {
        return EventListenerImpl.getImpl(getOnfocusImpl(getPeer()));
    }
    static long getOnfocusImpl(long peer) {
        return ElementNative.getOnfocus(peer);
    }

    public void setOnfocus(EventListener value) {
        setOnfocusImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnfocusImpl(long peer, long value) {
        ElementNative.setOnfocus(peer, value);
    }

    public EventListener getOninput() {
        return EventListenerImpl.getImpl(getOninputImpl(getPeer()));
    }
    static long getOninputImpl(long peer) {
        return ElementNative.getOninput(peer);
    }

    public void setOninput(EventListener value) {
        setOninputImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOninputImpl(long peer, long value) {
        ElementNative.setOninput(peer, value);
    }

    public EventListener getOninvalid() {
        return EventListenerImpl.getImpl(getOninvalidImpl(getPeer()));
    }
    static long getOninvalidImpl(long peer) {
        return ElementNative.getOninvalid(peer);
    }

    public void setOninvalid(EventListener value) {
        setOninvalidImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOninvalidImpl(long peer, long value) {
        ElementNative.setOninvalid(peer, value);
    }

    public EventListener getOnkeydown() {
        return EventListenerImpl.getImpl(getOnkeydownImpl(getPeer()));
    }
    static long getOnkeydownImpl(long peer) {
        return ElementNative.getOnkeydown(peer);
    }

    public void setOnkeydown(EventListener value) {
        setOnkeydownImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnkeydownImpl(long peer, long value) {
        ElementNative.setOnkeydown(peer, value);
    }

    public EventListener getOnkeypress() {
        return EventListenerImpl.getImpl(getOnkeypressImpl(getPeer()));
    }
    static long getOnkeypressImpl(long peer) {
        return ElementNative.getOnkeypress(peer);
    }

    public void setOnkeypress(EventListener value) {
        setOnkeypressImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnkeypressImpl(long peer, long value) {
        ElementNative.setOnkeypress(peer, value);
    }

    public EventListener getOnkeyup() {
        return EventListenerImpl.getImpl(getOnkeyupImpl(getPeer()));
    }
    static long getOnkeyupImpl(long peer) {
        return ElementNative.getOnkeyup(peer);
    }

    public void setOnkeyup(EventListener value) {
        setOnkeyupImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnkeyupImpl(long peer, long value) {
        ElementNative.setOnkeyup(peer, value);
    }

    public EventListener getOnload() {
        return EventListenerImpl.getImpl(getOnloadImpl(getPeer()));
    }
    static long getOnloadImpl(long peer) {
        return ElementNative.getOnload(peer);
    }

    public void setOnload(EventListener value) {
        setOnloadImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnloadImpl(long peer, long value) {
        ElementNative.setOnload(peer, value);
    }

    public EventListener getOnloadeddata() {
        return EventListenerImpl.getImpl(getOnloadeddataImpl(getPeer()));
    }
    static long getOnloadeddataImpl(long peer) {
        return ElementNative.getOnloadeddata(peer);
    }

    public void setOnloadeddata(EventListener value) {
        setOnloadeddataImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnloadeddataImpl(long peer, long value) {
        ElementNative.setOnloadeddata(peer, value);
    }

    public EventListener getOnloadedmetadata() {
        return EventListenerImpl.getImpl(getOnloadedmetadataImpl(getPeer()));
    }
    static long getOnloadedmetadataImpl(long peer) {
        return ElementNative.getOnloadedmetadata(peer);
    }

    public void setOnloadedmetadata(EventListener value) {
        setOnloadedmetadataImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnloadedmetadataImpl(long peer, long value) {
        ElementNative.setOnloadedmetadata(peer, value);
    }

    public EventListener getOnloadstart() {
        return EventListenerImpl.getImpl(getOnloadstartImpl(getPeer()));
    }
    static long getOnloadstartImpl(long peer) {
        return ElementNative.getOnloadstart(peer);
    }

    public void setOnloadstart(EventListener value) {
        setOnloadstartImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnloadstartImpl(long peer, long value) {
        ElementNative.setOnloadstart(peer, value);
    }

    public EventListener getOnmousedown() {
        return EventListenerImpl.getImpl(getOnmousedownImpl(getPeer()));
    }
    static long getOnmousedownImpl(long peer) {
        return ElementNative.getOnmousedown(peer);
    }

    public void setOnmousedown(EventListener value) {
        setOnmousedownImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnmousedownImpl(long peer, long value) {
        ElementNative.setOnmousedown(peer, value);
    }

    public EventListener getOnmouseenter() {
        return EventListenerImpl.getImpl(getOnmouseenterImpl(getPeer()));
    }
    static long getOnmouseenterImpl(long peer) {
        return ElementNative.getOnmouseenter(peer);
    }

    public void setOnmouseenter(EventListener value) {
        setOnmouseenterImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnmouseenterImpl(long peer, long value) {
        ElementNative.setOnmouseenter(peer, value);
    }

    public EventListener getOnmouseleave() {
        return EventListenerImpl.getImpl(getOnmouseleaveImpl(getPeer()));
    }
    static long getOnmouseleaveImpl(long peer) {
        return ElementNative.getOnmouseleave(peer);
    }

    public void setOnmouseleave(EventListener value) {
        setOnmouseleaveImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnmouseleaveImpl(long peer, long value) {
        ElementNative.setOnmouseleave(peer, value);
    }

    public EventListener getOnmousemove() {
        return EventListenerImpl.getImpl(getOnmousemoveImpl(getPeer()));
    }
    static long getOnmousemoveImpl(long peer) {
        return ElementNative.getOnmousemove(peer);
    }

    public void setOnmousemove(EventListener value) {
        setOnmousemoveImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnmousemoveImpl(long peer, long value) {
        ElementNative.setOnmousemove(peer, value);
    }

    public EventListener getOnmouseout() {
        return EventListenerImpl.getImpl(getOnmouseoutImpl(getPeer()));
    }
    static long getOnmouseoutImpl(long peer) {
        return ElementNative.getOnmouseout(peer);
    }

    public void setOnmouseout(EventListener value) {
        setOnmouseoutImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnmouseoutImpl(long peer, long value) {
        ElementNative.setOnmouseout(peer, value);
    }

    public EventListener getOnmouseover() {
        return EventListenerImpl.getImpl(getOnmouseoverImpl(getPeer()));
    }
    static long getOnmouseoverImpl(long peer) {
        return ElementNative.getOnmouseover(peer);
    }

    public void setOnmouseover(EventListener value) {
        setOnmouseoverImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnmouseoverImpl(long peer, long value) {
        ElementNative.setOnmouseover(peer, value);
    }

    public EventListener getOnmouseup() {
        return EventListenerImpl.getImpl(getOnmouseupImpl(getPeer()));
    }
    static long getOnmouseupImpl(long peer) {
        return ElementNative.getOnmouseup(peer);
    }

    public void setOnmouseup(EventListener value) {
        setOnmouseupImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnmouseupImpl(long peer, long value) {
        ElementNative.setOnmouseup(peer, value);
    }

    public EventListener getOnmousewheel() {
        return EventListenerImpl.getImpl(getOnmousewheelImpl(getPeer()));
    }
    static long getOnmousewheelImpl(long peer) {
        return ElementNative.getOnmousewheel(peer);
    }

    public void setOnmousewheel(EventListener value) {
        setOnmousewheelImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnmousewheelImpl(long peer, long value) {
        ElementNative.setOnmousewheel(peer, value);
    }

    public EventListener getOnpause() {
        return EventListenerImpl.getImpl(getOnpauseImpl(getPeer()));
    }
    static long getOnpauseImpl(long peer) {
        return ElementNative.getOnpause(peer);
    }

    public void setOnpause(EventListener value) {
        setOnpauseImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnpauseImpl(long peer, long value) {
        ElementNative.setOnpause(peer, value);
    }

    public EventListener getOnplay() {
        return EventListenerImpl.getImpl(getOnplayImpl(getPeer()));
    }
    static long getOnplayImpl(long peer) {
        return ElementNative.getOnplay(peer);
    }

    public void setOnplay(EventListener value) {
        setOnplayImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnplayImpl(long peer, long value) {
        ElementNative.setOnplay(peer, value);
    }

    public EventListener getOnplaying() {
        return EventListenerImpl.getImpl(getOnplayingImpl(getPeer()));
    }
    static long getOnplayingImpl(long peer) {
        return ElementNative.getOnplaying(peer);
    }

    public void setOnplaying(EventListener value) {
        setOnplayingImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnplayingImpl(long peer, long value) {
        ElementNative.setOnplaying(peer, value);
    }

    public EventListener getOnprogress() {
        return EventListenerImpl.getImpl(getOnprogressImpl(getPeer()));
    }
    static long getOnprogressImpl(long peer) {
        return ElementNative.getOnprogress(peer);
    }

    public void setOnprogress(EventListener value) {
        setOnprogressImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnprogressImpl(long peer, long value) {
        ElementNative.setOnprogress(peer, value);
    }

    public EventListener getOnratechange() {
        return EventListenerImpl.getImpl(getOnratechangeImpl(getPeer()));
    }
    static long getOnratechangeImpl(long peer) {
        return ElementNative.getOnratechange(peer);
    }

    public void setOnratechange(EventListener value) {
        setOnratechangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnratechangeImpl(long peer, long value) {
        ElementNative.setOnratechange(peer, value);
    }

    public EventListener getOnreset() {
        return EventListenerImpl.getImpl(getOnresetImpl(getPeer()));
    }
    static long getOnresetImpl(long peer) {
        return ElementNative.getOnreset(peer);
    }

    public void setOnreset(EventListener value) {
        setOnresetImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnresetImpl(long peer, long value) {
        ElementNative.setOnreset(peer, value);
    }

    public EventListener getOnresize() {
        return EventListenerImpl.getImpl(getOnresizeImpl(getPeer()));
    }
    static long getOnresizeImpl(long peer) {
        return ElementNative.getOnresize(peer);
    }

    public void setOnresize(EventListener value) {
        setOnresizeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnresizeImpl(long peer, long value) {
        ElementNative.setOnresize(peer, value);
    }

    public EventListener getOnscroll() {
        return EventListenerImpl.getImpl(getOnscrollImpl(getPeer()));
    }
    static long getOnscrollImpl(long peer) {
        return ElementNative.getOnscroll(peer);
    }

    public void setOnscroll(EventListener value) {
        setOnscrollImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnscrollImpl(long peer, long value) {
        ElementNative.setOnscroll(peer, value);
    }

    public EventListener getOnseeked() {
        return EventListenerImpl.getImpl(getOnseekedImpl(getPeer()));
    }
    static long getOnseekedImpl(long peer) {
        return ElementNative.getOnseeked(peer);
    }

    public void setOnseeked(EventListener value) {
        setOnseekedImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnseekedImpl(long peer, long value) {
        ElementNative.setOnseeked(peer, value);
    }

    public EventListener getOnseeking() {
        return EventListenerImpl.getImpl(getOnseekingImpl(getPeer()));
    }
    static long getOnseekingImpl(long peer) {
        return ElementNative.getOnseeking(peer);
    }

    public void setOnseeking(EventListener value) {
        setOnseekingImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnseekingImpl(long peer, long value) {
        ElementNative.setOnseeking(peer, value);
    }

    public EventListener getOnselect() {
        return EventListenerImpl.getImpl(getOnselectImpl(getPeer()));
    }
    static long getOnselectImpl(long peer) {
        return ElementNative.getOnselect(peer);
    }

    public void setOnselect(EventListener value) {
        setOnselectImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnselectImpl(long peer, long value) {
        ElementNative.setOnselect(peer, value);
    }

    public EventListener getOnstalled() {
        return EventListenerImpl.getImpl(getOnstalledImpl(getPeer()));
    }
    static long getOnstalledImpl(long peer) {
        return ElementNative.getOnstalled(peer);
    }

    public void setOnstalled(EventListener value) {
        setOnstalledImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnstalledImpl(long peer, long value) {
        ElementNative.setOnstalled(peer, value);
    }

    public EventListener getOnsubmit() {
        return EventListenerImpl.getImpl(getOnsubmitImpl(getPeer()));
    }
    static long getOnsubmitImpl(long peer) {
        return ElementNative.getOnsubmit(peer);
    }

    public void setOnsubmit(EventListener value) {
        setOnsubmitImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnsubmitImpl(long peer, long value) {
        ElementNative.setOnsubmit(peer, value);
    }

    public EventListener getOnsuspend() {
        return EventListenerImpl.getImpl(getOnsuspendImpl(getPeer()));
    }
    static long getOnsuspendImpl(long peer) {
        return ElementNative.getOnsuspend(peer);
    }

    public void setOnsuspend(EventListener value) {
        setOnsuspendImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnsuspendImpl(long peer, long value) {
        ElementNative.setOnsuspend(peer, value);
    }

    public EventListener getOntimeupdate() {
        return EventListenerImpl.getImpl(getOntimeupdateImpl(getPeer()));
    }
    static long getOntimeupdateImpl(long peer) {
        return ElementNative.getOntimeupdate(peer);
    }

    public void setOntimeupdate(EventListener value) {
        setOntimeupdateImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOntimeupdateImpl(long peer, long value) {
        ElementNative.setOntimeupdate(peer, value);
    }

    public EventListener getOnvolumechange() {
        return EventListenerImpl.getImpl(getOnvolumechangeImpl(getPeer()));
    }
    static long getOnvolumechangeImpl(long peer) {
        return ElementNative.getOnvolumechange(peer);
    }

    public void setOnvolumechange(EventListener value) {
        setOnvolumechangeImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnvolumechangeImpl(long peer, long value) {
        ElementNative.setOnvolumechange(peer, value);
    }

    public EventListener getOnwaiting() {
        return EventListenerImpl.getImpl(getOnwaitingImpl(getPeer()));
    }
    static long getOnwaitingImpl(long peer) {
        return ElementNative.getOnwaiting(peer);
    }

    public void setOnwaiting(EventListener value) {
        setOnwaitingImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnwaitingImpl(long peer, long value) {
        ElementNative.setOnwaiting(peer, value);
    }

    public EventListener getOnsearch() {
        return EventListenerImpl.getImpl(getOnsearchImpl(getPeer()));
    }
    static long getOnsearchImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.ElementImpl.getOnsearchImpl: no wkj_* function exists for it"
                + " in any jfxwebkit build");
    }

    public void setOnsearch(EventListener value) {
        setOnsearchImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnsearchImpl(long peer, long value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.ElementImpl.setOnsearchImpl: no wkj_* function exists for it"
                + " in any jfxwebkit build");
    }

    public EventListener getOnwheel() {
        return EventListenerImpl.getImpl(getOnwheelImpl(getPeer()));
    }
    static long getOnwheelImpl(long peer) {
        return ElementNative.getOnwheel(peer);
    }

    public void setOnwheel(EventListener value) {
        setOnwheelImpl(getPeer(), EventListenerImpl.getPeer(value));
    }
    static void setOnwheelImpl(long peer, long value) {
        ElementNative.setOnwheel(peer, value);
    }

    public Element getPreviousElementSibling() {
        return ElementImpl.getImpl(getPreviousElementSiblingImpl(getPeer()));
    }
    static long getPreviousElementSiblingImpl(long peer) {
        return ElementNative.getPreviousElementSibling(peer);
    }

    public Element getNextElementSibling() {
        return ElementImpl.getImpl(getNextElementSiblingImpl(getPeer()));
    }
    static long getNextElementSiblingImpl(long peer) {
        return ElementNative.getNextElementSibling(peer);
    }

    public HTMLCollection getChildren() {
        return HTMLCollectionImpl.getImpl(getChildrenImpl(getPeer()));
    }
    static long getChildrenImpl(long peer) {
        return ElementNative.getChildren(peer);
    }

    public Element getFirstElementChild() {
        return ElementImpl.getImpl(getFirstElementChildImpl(getPeer()));
    }
    static long getFirstElementChildImpl(long peer) {
        return ElementNative.getFirstElementChild(peer);
    }

    public Element getLastElementChild() {
        return ElementImpl.getImpl(getLastElementChildImpl(getPeer()));
    }
    static long getLastElementChildImpl(long peer) {
        return ElementNative.getLastElementChild(peer);
    }

    public int getChildElementCount() {
        return getChildElementCountImpl(getPeer());
    }
    static int getChildElementCountImpl(long peer) {
        return ElementNative.getChildElementCount(peer);
    }


// Functions
    @Override
    public String getAttribute(String name)
    {
        return getAttributeImpl(getPeer()
            , name);
    }
    static String getAttributeImpl(long peer
        , String name) {
        return ElementNative.getAttribute(peer, name);
    }


    @Override
    public void setAttribute(String name
        , String value) throws DOMException
    {
        setAttributeImpl(getPeer()
            , name
            , value);
    }
    static void setAttributeImpl(long peer
        , String name
        , String value) {
        ElementNative.setAttribute(peer, name, value);
    }


    @Override
    public void removeAttribute(String name)
    {
        removeAttributeImpl(getPeer()
            , name);
    }
    static void removeAttributeImpl(long peer
        , String name) {
        ElementNative.removeAttribute(peer, name);
    }


    @Override
    public Attr getAttributeNode(String name)
    {
        return AttrImpl.getImpl(getAttributeNodeImpl(getPeer()
            , name));
    }
    static long getAttributeNodeImpl(long peer
        , String name) {
        return ElementNative.getAttributeNode(peer, name);
    }


    @Override
    public Attr setAttributeNode(Attr newAttr) throws DOMException
    {
        return AttrImpl.getImpl(setAttributeNodeImpl(getPeer()
            , AttrImpl.getPeer(newAttr)));
    }
    static long setAttributeNodeImpl(long peer
        , long newAttr) {
        return ElementNative.setAttributeNode(peer, newAttr);
    }


    @Override
    public Attr removeAttributeNode(Attr oldAttr) throws DOMException
    {
        return AttrImpl.getImpl(removeAttributeNodeImpl(getPeer()
            , AttrImpl.getPeer(oldAttr)));
    }
    static long removeAttributeNodeImpl(long peer
        , long oldAttr) {
        return ElementNative.removeAttributeNode(peer, oldAttr);
    }


    @Override
    public NodeList getElementsByTagName(String name)
    {
        return NodeListImpl.getImpl(getElementsByTagNameImpl(getPeer()
            , name));
    }
    static long getElementsByTagNameImpl(long peer
        , String name) {
        return ElementNative.getElementsByTagName(peer, name);
    }


    @Override
    public boolean hasAttributes()
    {
        return hasAttributesImpl(getPeer());
    }
    static boolean hasAttributesImpl(long peer) {
        return ElementNative.hasAttributes(peer);
    }


    @Override
    public String getAttributeNS(String namespaceURI
        , String localName)
    {
        return getAttributeNSImpl(getPeer()
            , namespaceURI
            , localName);
    }
    static String getAttributeNSImpl(long peer
        , String namespaceURI
        , String localName) {
        return ElementNative.getAttributeNS(peer, namespaceURI, localName);
    }


    @Override
    public void setAttributeNS(String namespaceURI
        , String qualifiedName
        , String value) throws DOMException
    {
        setAttributeNSImpl(getPeer()
            , namespaceURI
            , qualifiedName
            , value);
    }
    static void setAttributeNSImpl(long peer
        , String namespaceURI
        , String qualifiedName
        , String value) {
        ElementNative.setAttributeNS(peer, namespaceURI, qualifiedName, value);
    }


    @Override
    public void removeAttributeNS(String namespaceURI
        , String localName)
    {
        removeAttributeNSImpl(getPeer()
            , namespaceURI
            , localName);
    }
    static void removeAttributeNSImpl(long peer
        , String namespaceURI
        , String localName) {
        ElementNative.removeAttributeNS(peer, namespaceURI, localName);
    }


    @Override
    public NodeList getElementsByTagNameNS(String namespaceURI
        , String localName)
    {
        return NodeListImpl.getImpl(getElementsByTagNameNSImpl(getPeer()
            , namespaceURI
            , localName));
    }
    static long getElementsByTagNameNSImpl(long peer
        , String namespaceURI
        , String localName) {
        return ElementNative.getElementsByTagNameNS(peer, namespaceURI, localName);
    }


    @Override
    public Attr getAttributeNodeNS(String namespaceURI
        , String localName)
    {
        return AttrImpl.getImpl(getAttributeNodeNSImpl(getPeer()
            , namespaceURI
            , localName));
    }
    static long getAttributeNodeNSImpl(long peer
        , String namespaceURI
        , String localName) {
        return ElementNative.getAttributeNodeNS(peer, namespaceURI, localName);
    }


    @Override
    public Attr setAttributeNodeNS(Attr newAttr) throws DOMException
    {
        return AttrImpl.getImpl(setAttributeNodeNSImpl(getPeer()
            , AttrImpl.getPeer(newAttr)));
    }
    static long setAttributeNodeNSImpl(long peer
        , long newAttr) {
        return ElementNative.setAttributeNodeNS(peer, newAttr);
    }


    @Override
    public boolean hasAttribute(String name)
    {
        return hasAttributeImpl(getPeer()
            , name);
    }
    static boolean hasAttributeImpl(long peer
        , String name) {
        return ElementNative.hasAttribute(peer, name);
    }


    @Override
    public boolean hasAttributeNS(String namespaceURI
        , String localName)
    {
        return hasAttributeNSImpl(getPeer()
            , namespaceURI
            , localName);
    }
    static boolean hasAttributeNSImpl(long peer
        , String namespaceURI
        , String localName) {
        return ElementNative.hasAttributeNS(peer, namespaceURI, localName);
    }


    public void focus()
    {
        focusImpl(getPeer());
    }
    static void focusImpl(long peer) {
        ElementNative.focus(peer);
    }


    public void blur()
    {
        blurImpl(getPeer());
    }
    static void blurImpl(long peer) {
        ElementNative.blur(peer);
    }


    public void scrollIntoView(boolean alignWithTop)
    {
        scrollIntoViewImpl(getPeer()
            , alignWithTop);
    }
    static void scrollIntoViewImpl(long peer
        , boolean alignWithTop) {
        ElementNative.scrollIntoView(peer, alignWithTop);
    }


    public void scrollIntoViewIfNeeded(boolean centerIfNeeded)
    {
        scrollIntoViewIfNeededImpl(getPeer()
            , centerIfNeeded);
    }
    static void scrollIntoViewIfNeededImpl(long peer
        , boolean centerIfNeeded) {
        ElementNative.scrollIntoViewIfNeeded(peer, centerIfNeeded);
    }


    public void scrollByLines(int lines)
    {
        scrollByLinesImpl(getPeer()
            , lines);
    }
    static void scrollByLinesImpl(long peer
        , int lines) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.ElementImpl.scrollByLinesImpl: no wkj_* function exists for"
                + " it in any jfxwebkit build");
    }


    public void scrollByPages(int pages)
    {
        scrollByPagesImpl(getPeer()
            , pages);
    }
    static void scrollByPagesImpl(long peer
        , int pages) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.ElementImpl.scrollByPagesImpl: no wkj_* function exists for"
                + " it in any jfxwebkit build");
    }


    public HTMLCollection getElementsByClassName(String name)
    {
        return HTMLCollectionImpl.getImpl(getElementsByClassNameImpl(getPeer()
            , name));
    }
    static long getElementsByClassNameImpl(long peer
        , String name) {
        return ElementNative.getElementsByClassName(peer, name);
    }


    public boolean matches(String selectors) throws DOMException
    {
        return matchesImpl(getPeer()
            , selectors);
    }
    static boolean matchesImpl(long peer
        , String selectors) {
        return ElementNative.matches(peer, selectors);
    }


    public Element closest(String selectors) throws DOMException
    {
        return ElementImpl.getImpl(closestImpl(getPeer()
            , selectors));
    }
    static long closestImpl(long peer
        , String selectors) {
        return ElementNative.closest(peer, selectors);
    }


    public boolean webkitMatchesSelector(String selectors) throws DOMException
    {
        return webkitMatchesSelectorImpl(getPeer()
            , selectors);
    }
    static boolean webkitMatchesSelectorImpl(long peer
        , String selectors) {
        return ElementNative.webkitMatchesSelector(peer, selectors);
    }


    public void webkitRequestFullScreen(short flags)
    {
        webkitRequestFullScreenImpl(getPeer()
            , flags);
    }
    static void webkitRequestFullScreenImpl(long peer
        , short flags) {
        ElementNative.webkitRequestFullScreen(peer, flags);
    }


    public void webkitRequestFullscreen()
    {
        webkitRequestFullscreenImpl(getPeer());
    }
    static void webkitRequestFullscreenImpl(long peer) {
        ElementNative.webkitRequestFullscreen(peer);
    }


    public void remove() throws DOMException
    {
        removeImpl(getPeer());
    }
    static void removeImpl(long peer) {
        ElementNative.remove(peer);
    }


    public Element querySelector(String selectors) throws DOMException
    {
        return ElementImpl.getImpl(querySelectorImpl(getPeer()
            , selectors));
    }
    static long querySelectorImpl(long peer
        , String selectors) {
        return ElementNative.querySelector(peer, selectors);
    }


    public NodeList querySelectorAll(String selectors) throws DOMException
    {
        return NodeListImpl.getImpl(querySelectorAllImpl(getPeer()
            , selectors));
    }
    static long querySelectorAllImpl(long peer
        , String selectors) {
        return ElementNative.querySelectorAll(peer, selectors);
    }



//stubs
    @Override
    public void setIdAttribute(String name, boolean isId) throws DOMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TypeInfo getSchemaTypeInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setIdAttributeNS(String namespaceURI, String localName, boolean isId) throws DOMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

