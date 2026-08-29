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

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLElement;

public class HTMLElementImpl extends ElementImpl implements HTMLElement {
    HTMLElementImpl(long peer) {
        super(peer);
    }

    static HTMLElement getImpl(long peer) {
        return (HTMLElement)create(peer);
    }


// Attributes
    @Override
    public String getId() {
        return getIdImpl(getPeer());
    }
    static String getIdImpl(long peer) {
        return HTMLElementNative.getId(peer);
    }

    @Override
    public void setId(String value) {
        setIdImpl(getPeer(), value);
    }
    static void setIdImpl(long peer, String value) {
        HTMLElementNative.setId(peer, value);
    }

    @Override
    public String getTitle() {
        return getTitleImpl(getPeer());
    }
    static String getTitleImpl(long peer) {
        return HTMLElementNative.getTitle(peer);
    }

    @Override
    public void setTitle(String value) {
        setTitleImpl(getPeer(), value);
    }
    static void setTitleImpl(long peer, String value) {
        HTMLElementNative.setTitle(peer, value);
    }

    @Override
    public String getLang() {
        return getLangImpl(getPeer());
    }
    static String getLangImpl(long peer) {
        return HTMLElementNative.getLang(peer);
    }

    @Override
    public void setLang(String value) {
        setLangImpl(getPeer(), value);
    }
    static void setLangImpl(long peer, String value) {
        HTMLElementNative.setLang(peer, value);
    }

    public boolean getTranslate() {
        return getTranslateImpl(getPeer());
    }
    static boolean getTranslateImpl(long peer) {
        return HTMLElementNative.getTranslate(peer);
    }

    public void setTranslate(boolean value) {
        setTranslateImpl(getPeer(), value);
    }
    static void setTranslateImpl(long peer, boolean value) {
        HTMLElementNative.setTranslate(peer, value);
    }

    @Override
    public String getDir() {
        return getDirImpl(getPeer());
    }
    static String getDirImpl(long peer) {
        return HTMLElementNative.getDir(peer);
    }

    @Override
    public void setDir(String value) {
        setDirImpl(getPeer(), value);
    }
    static void setDirImpl(long peer, String value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLElementImpl.setDirImpl: no wkj_* function exists for it"
                + " in any jfxwebkit build");
    }

    public int getTabIndex() {
        return getTabIndexImpl(getPeer());
    }
    static int getTabIndexImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLElementImpl.getTabIndexImpl: no wkj_* function exists"
                + " for it in any jfxwebkit build");
    }

    public void setTabIndex(int value) {
        setTabIndexImpl(getPeer(), value);
    }
    static void setTabIndexImpl(long peer, int value) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLElementImpl.setTabIndexImpl: no wkj_* function exists"
                + " for it in any jfxwebkit build");
    }

    public boolean getDraggable() {
        return getDraggableImpl(getPeer());
    }
    static boolean getDraggableImpl(long peer) {
        return HTMLElementNative.getDraggable(peer);
    }

    public void setDraggable(boolean value) {
        setDraggableImpl(getPeer(), value);
    }
    static void setDraggableImpl(long peer, boolean value) {
        HTMLElementNative.setDraggable(peer, value);
    }

    public String getWebkitdropzone() {
        return getWebkitdropzoneImpl(getPeer());
    }
    static String getWebkitdropzoneImpl(long peer) {
        return HTMLElementNative.getWebkitdropzone(peer);
    }

    public void setWebkitdropzone(String value) {
        setWebkitdropzoneImpl(getPeer(), value);
    }
    static void setWebkitdropzoneImpl(long peer, String value) {
        HTMLElementNative.setWebkitdropzone(peer, value);
    }

    public boolean getHidden() {
        return getHiddenImpl(getPeer());
    }
    static boolean getHiddenImpl(long peer) {
        return HTMLElementNative.getHidden(peer);
    }

    public void setHidden(boolean value) {
        setHiddenImpl(getPeer(), value);
    }
    static void setHiddenImpl(long peer, boolean value) {
        HTMLElementNative.setHidden(peer, value);
    }

    public String getAccessKey() {
        return getAccessKeyImpl(getPeer());
    }
    static String getAccessKeyImpl(long peer) {
        return HTMLElementNative.getAccessKey(peer);
    }

    public void setAccessKey(String value) {
        setAccessKeyImpl(getPeer(), value);
    }
    static void setAccessKeyImpl(long peer, String value) {
        HTMLElementNative.setAccessKey(peer, value);
    }

    public String getInnerText() {
        return getInnerTextImpl(getPeer());
    }
    static String getInnerTextImpl(long peer) {
        return HTMLElementNative.getInnerText(peer);
    }

    public void setInnerText(String value) throws DOMException {
        setInnerTextImpl(getPeer(), value);
    }
    static void setInnerTextImpl(long peer, String value) {
        HTMLElementNative.setInnerText(peer, value);
    }

    public String getOuterText() {
        return getOuterTextImpl(getPeer());
    }
    static String getOuterTextImpl(long peer) {
        return HTMLElementNative.getOuterText(peer);
    }

    public void setOuterText(String value) throws DOMException {
        setOuterTextImpl(getPeer(), value);
    }
    static void setOuterTextImpl(long peer, String value) {
        HTMLElementNative.setOuterText(peer, value);
    }

    @Override
    public HTMLCollection getChildren() {
        return HTMLCollectionImpl.getImpl(getChildrenImpl(getPeer()));
    }
    static long getChildrenImpl(long peer) {
        return HTMLElementNative.getChildren(peer);
    }

    public String getContentEditable() {
        return getContentEditableImpl(getPeer());
    }
    static String getContentEditableImpl(long peer) {
        return HTMLElementNative.getContentEditable(peer);
    }

    public void setContentEditable(String value) throws DOMException {
        setContentEditableImpl(getPeer(), value);
    }
    static void setContentEditableImpl(long peer, String value) {
        HTMLElementNative.setContentEditable(peer, value);
    }

    public boolean getIsContentEditable() {
        return getIsContentEditableImpl(getPeer());
    }
    static boolean getIsContentEditableImpl(long peer) {
        return HTMLElementNative.getIsContentEditable(peer);
    }

    public boolean getSpellcheck() {
        return getSpellcheckImpl(getPeer());
    }
    static boolean getSpellcheckImpl(long peer) {
        return HTMLElementNative.getSpellcheck(peer);
    }

    public void setSpellcheck(boolean value) {
        setSpellcheckImpl(getPeer(), value);
    }
    static void setSpellcheckImpl(long peer, boolean value) {
        HTMLElementNative.setSpellcheck(peer, value);
    }

    public String getTitleDisplayString() {
        return getTitleDisplayStringImpl(getPeer());
    }
    static String getTitleDisplayStringImpl(long peer) {
        return HTMLElementNative.getTitleDisplayString(peer);
    }


// Functions
    public Element insertAdjacentElement(String where
        , Element element) throws DOMException
    {
        return ElementImpl.getImpl(insertAdjacentElementImpl(getPeer()
            , where
            , ElementImpl.getPeer(element)));
    }
    static long insertAdjacentElementImpl(long peer
        , String where
        , long element) {
        return HTMLElementNative.insertAdjacentElement(peer, where, element);
    }


    public void insertAdjacentHTML(String where
        , String html) throws DOMException
    {
        insertAdjacentHTMLImpl(getPeer()
            , where
            , html);
    }
    static void insertAdjacentHTMLImpl(long peer
        , String where
        , String html) {
        HTMLElementNative.insertAdjacentHTML(peer, where, html);
    }


    public void insertAdjacentText(String where
        , String text) throws DOMException
    {
        insertAdjacentTextImpl(getPeer()
            , where
            , text);
    }
    static void insertAdjacentTextImpl(long peer
        , String where
        , String text) {
        HTMLElementNative.insertAdjacentText(peer, where, text);
    }


    public void click()
    {
        clickImpl(getPeer());
    }
    static void clickImpl(long peer) {
        HTMLElementNative.click(peer);
    }


}

