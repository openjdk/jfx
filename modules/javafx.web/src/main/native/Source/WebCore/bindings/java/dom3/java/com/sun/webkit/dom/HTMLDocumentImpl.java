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
    native static long getEmbedsImpl(long peer);

    public HTMLCollection getPlugins() {
        return HTMLCollectionImpl.getImpl(getPluginsImpl(getPeer()));
    }
    native static long getPluginsImpl(long peer);

    public HTMLCollection getScripts() {
        return HTMLCollectionImpl.getImpl(getScriptsImpl(getPeer()));
    }
    native static long getScriptsImpl(long peer);

    public int getWidth() {
        return getWidthImpl(getPeer());
    }
    native static int getWidthImpl(long peer);

    public int getHeight() {
        return getHeightImpl(getPeer());
    }
    native static int getHeightImpl(long peer);

    public String getDir() {
        return getDirImpl(getPeer());
    }
    native static String getDirImpl(long peer);

    public void setDir(String value) {
        setDirImpl(getPeer(), value);
    }
    native static void setDirImpl(long peer, String value);

    public String getDesignMode() {
        return getDesignModeImpl(getPeer());
    }
    native static String getDesignModeImpl(long peer);

    public void setDesignMode(String value) {
        setDesignModeImpl(getPeer(), value);
    }
    native static void setDesignModeImpl(long peer, String value);

    public String getCompatMode() {
        return getCompatModeImpl(getPeer());
    }
    native static String getCompatModeImpl(long peer);

    public String getBgColor() {
        return getBgColorImpl(getPeer());
    }
    native static String getBgColorImpl(long peer);

    public void setBgColor(String value) {
        setBgColorImpl(getPeer(), value);
    }
    native static void setBgColorImpl(long peer, String value);

    public String getFgColor() {
        return getFgColorImpl(getPeer());
    }
    native static String getFgColorImpl(long peer);

    public void setFgColor(String value) {
        setFgColorImpl(getPeer(), value);
    }
    native static void setFgColorImpl(long peer, String value);

    public String getAlinkColor() {
        return getAlinkColorImpl(getPeer());
    }
    native static String getAlinkColorImpl(long peer);

    public void setAlinkColor(String value) {
        setAlinkColorImpl(getPeer(), value);
    }
    native static void setAlinkColorImpl(long peer, String value);

    public String getLinkColor() {
        return getLinkColorImpl(getPeer());
    }
    native static String getLinkColorImpl(long peer);

    public void setLinkColor(String value) {
        setLinkColorImpl(getPeer(), value);
    }
    native static void setLinkColorImpl(long peer, String value);

    public String getVlinkColor() {
        return getVlinkColorImpl(getPeer());
    }
    native static String getVlinkColorImpl(long peer);

    public void setVlinkColor(String value) {
        setVlinkColorImpl(getPeer(), value);
    }
    native static void setVlinkColorImpl(long peer, String value);


// Functions
    public void open()
    {
        openImpl(getPeer());
    }
    native static void openImpl(long peer);


    public void close()
    {
        closeImpl(getPeer());
    }
    native static void closeImpl(long peer);


    public void write(String text)
    {
        writeImpl(getPeer()
            , text);
    }
    native static void writeImpl(long peer
        , String text);


    public void writeln(String text)
    {
        writelnImpl(getPeer()
            , text);
    }
    native static void writelnImpl(long peer
        , String text);


    public void clear()
    {
        clearImpl(getPeer());
    }
    native static void clearImpl(long peer);


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


}

