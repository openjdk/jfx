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

import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLDocument;

public class HTMLDocumentImpl extends DocumentImpl implements HTMLDocument {
    HTMLDocumentImpl(long peer) {
        super(peer);
    }

    static HTMLDocument getImpl(long peer) {
        return (HTMLDocument)create(peer);
    }


// Attributes
    public HTMLCollection getEmbeds() {
        return HTMLCollectionImpl.getImpl(getEmbedsImpl(getPeer()));
    }
    static long getEmbedsImpl(long peer) {
        return HTMLDocumentNative.getEmbeds(peer);
    }

    public HTMLCollection getPlugins() {
        return HTMLCollectionImpl.getImpl(getPluginsImpl(getPeer()));
    }
    static long getPluginsImpl(long peer) {
        return HTMLDocumentNative.getPlugins(peer);
    }

    public HTMLCollection getScripts() {
        return HTMLCollectionImpl.getImpl(getScriptsImpl(getPeer()));
    }
    static long getScriptsImpl(long peer) {
        return HTMLDocumentNative.getScripts(peer);
    }

    public int getWidth() {
        return getWidthImpl(getPeer());
    }
    static int getWidthImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLDocumentImpl.getWidthImpl: no wkj_* function exists for"
                + " it in any jfxwebkit build");
    }

    public int getHeight() {
        return getHeightImpl(getPeer());
    }
    static int getHeightImpl(long peer) {
        throw new UnsatisfiedLinkError("com.sun.webkit.dom.HTMLDocumentImpl.getHeightImpl: no wkj_* function exists for"
                + " it in any jfxwebkit build");
    }

    public String getDir() {
        return getDirImpl(getPeer());
    }
    static String getDirImpl(long peer) {
        return HTMLDocumentNative.getDir(peer);
    }

    public void setDir(String value) {
        setDirImpl(getPeer(), value);
    }
    static void setDirImpl(long peer, String value) {
        HTMLDocumentNative.setDir(peer, value);
    }

    public String getDesignMode() {
        return getDesignModeImpl(getPeer());
    }
    static String getDesignModeImpl(long peer) {
        return HTMLDocumentNative.getDesignMode(peer);
    }

    public void setDesignMode(String value) {
        setDesignModeImpl(getPeer(), value);
    }
    static void setDesignModeImpl(long peer, String value) {
        HTMLDocumentNative.setDesignMode(peer, value);
    }

    @Override
    public String getCompatMode() {
        return getCompatModeImpl(getPeer());
    }
    static String getCompatModeImpl(long peer) {
        return HTMLDocumentNative.getCompatMode(peer);
    }

    public String getBgColor() {
        return getBgColorImpl(getPeer());
    }
    static String getBgColorImpl(long peer) {
        return HTMLDocumentNative.getBgColor(peer);
    }

    public void setBgColor(String value) {
        setBgColorImpl(getPeer(), value);
    }
    static void setBgColorImpl(long peer, String value) {
        HTMLDocumentNative.setBgColor(peer, value);
    }

    public String getFgColor() {
        return getFgColorImpl(getPeer());
    }
    static String getFgColorImpl(long peer) {
        return HTMLDocumentNative.getFgColor(peer);
    }

    public void setFgColor(String value) {
        setFgColorImpl(getPeer(), value);
    }
    static void setFgColorImpl(long peer, String value) {
        HTMLDocumentNative.setFgColor(peer, value);
    }

    public String getAlinkColor() {
        return getAlinkColorImpl(getPeer());
    }
    static String getAlinkColorImpl(long peer) {
        return HTMLDocumentNative.getAlinkColor(peer);
    }

    public void setAlinkColor(String value) {
        setAlinkColorImpl(getPeer(), value);
    }
    static void setAlinkColorImpl(long peer, String value) {
        HTMLDocumentNative.setAlinkColor(peer, value);
    }

    public String getLinkColor() {
        return getLinkColorImpl(getPeer());
    }
    static String getLinkColorImpl(long peer) {
        return HTMLDocumentNative.getLinkColor(peer);
    }

    public void setLinkColor(String value) {
        setLinkColorImpl(getPeer(), value);
    }
    static void setLinkColorImpl(long peer, String value) {
        HTMLDocumentNative.setLinkColor(peer, value);
    }

    public String getVlinkColor() {
        return getVlinkColorImpl(getPeer());
    }
    static String getVlinkColorImpl(long peer) {
        return HTMLDocumentNative.getVlinkColor(peer);
    }

    public void setVlinkColor(String value) {
        setVlinkColorImpl(getPeer(), value);
    }
    static void setVlinkColorImpl(long peer, String value) {
        HTMLDocumentNative.setVlinkColor(peer, value);
    }


// Functions
    @Override
    public void open()
    {
        openImpl(getPeer());
    }
    static void openImpl(long peer) {
        HTMLDocumentNative.open(peer);
    }


    @Override
    public void close()
    {
        closeImpl(getPeer());
    }
    static void closeImpl(long peer) {
        HTMLDocumentNative.close(peer);
    }


    @Override
    public void write(String text)
    {
        writeImpl(getPeer()
            , text);
    }
    static void writeImpl(long peer
        , String text) {
        HTMLDocumentNative.write(peer, text);
    }


    @Override
    public void writeln(String text)
    {
        writelnImpl(getPeer()
            , text);
    }
    static void writelnImpl(long peer
        , String text) {
        HTMLDocumentNative.writeln(peer, text);
    }


    public void clear()
    {
        clearImpl(getPeer());
    }
    static void clearImpl(long peer) {
        HTMLDocumentNative.clear(peer);
    }


    public void captureEvents()
    {
        captureEventsImpl(getPeer());
    }
    static void captureEventsImpl(long peer) {
        HTMLDocumentNative.captureEvents(peer);
    }


    public void releaseEvents()
    {
        releaseEventsImpl(getPeer());
    }
    static void releaseEventsImpl(long peer) {
        HTMLDocumentNative.releaseEvents(peer);
    }


}

