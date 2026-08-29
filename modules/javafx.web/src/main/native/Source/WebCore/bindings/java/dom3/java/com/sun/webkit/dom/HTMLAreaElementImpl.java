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

import org.w3c.dom.html.HTMLAreaElement;

public class HTMLAreaElementImpl extends HTMLElementImpl implements HTMLAreaElement {
    HTMLAreaElementImpl(long peer) {
        super(peer);
    }

    static HTMLAreaElement getImpl(long peer) {
        return (HTMLAreaElement)create(peer);
    }


// Attributes
    @Override
    public String getAlt() {
        return getAltImpl(getPeer());
    }
    static String getAltImpl(long peer) {
        return HTMLAreaElementNative.getAlt(peer);
    }

    @Override
    public void setAlt(String value) {
        setAltImpl(getPeer(), value);
    }
    static void setAltImpl(long peer, String value) {
        HTMLAreaElementNative.setAlt(peer, value);
    }

    @Override
    public String getCoords() {
        return getCoordsImpl(getPeer());
    }
    static String getCoordsImpl(long peer) {
        return HTMLAreaElementNative.getCoords(peer);
    }

    @Override
    public void setCoords(String value) {
        setCoordsImpl(getPeer(), value);
    }
    static void setCoordsImpl(long peer, String value) {
        HTMLAreaElementNative.setCoords(peer, value);
    }

    @Override
    public boolean getNoHref() {
        return getNoHrefImpl(getPeer());
    }
    static boolean getNoHrefImpl(long peer) {
        return HTMLAreaElementNative.getNoHref(peer);
    }

    @Override
    public void setNoHref(boolean value) {
        setNoHrefImpl(getPeer(), value);
    }
    static void setNoHrefImpl(long peer, boolean value) {
        HTMLAreaElementNative.setNoHref(peer, value);
    }

    public String getPing() {
        return getPingImpl(getPeer());
    }
    static String getPingImpl(long peer) {
        return HTMLAreaElementNative.getPing(peer);
    }

    public void setPing(String value) {
        setPingImpl(getPeer(), value);
    }
    static void setPingImpl(long peer, String value) {
        HTMLAreaElementNative.setPing(peer, value);
    }

    public String getRel() {
        return getRelImpl(getPeer());
    }
    static String getRelImpl(long peer) {
        return HTMLAreaElementNative.getRel(peer);
    }

    public void setRel(String value) {
        setRelImpl(getPeer(), value);
    }
    static void setRelImpl(long peer, String value) {
        HTMLAreaElementNative.setRel(peer, value);
    }

    @Override
    public String getShape() {
        return getShapeImpl(getPeer());
    }
    static String getShapeImpl(long peer) {
        return HTMLAreaElementNative.getShape(peer);
    }

    @Override
    public void setShape(String value) {
        setShapeImpl(getPeer(), value);
    }
    static void setShapeImpl(long peer, String value) {
        HTMLAreaElementNative.setShape(peer, value);
    }

    @Override
    public String getTarget() {
        return getTargetImpl(getPeer());
    }
    static String getTargetImpl(long peer) {
        return HTMLAreaElementNative.getTarget(peer);
    }

    @Override
    public void setTarget(String value) {
        setTargetImpl(getPeer(), value);
    }
    static void setTargetImpl(long peer, String value) {
        HTMLAreaElementNative.setTarget(peer, value);
    }

    @Override
    public String getAccessKey() {
        return getAccessKeyImpl(getPeer());
    }
    static String getAccessKeyImpl(long peer) {
        return HTMLAreaElementNative.getAccessKey(peer);
    }

    @Override
    public void setAccessKey(String value) {
        setAccessKeyImpl(getPeer(), value);
    }
    static void setAccessKeyImpl(long peer, String value) {
        HTMLAreaElementNative.setAccessKey(peer, value);
    }

    @Override
    public String getHref() {
        return getHrefImpl(getPeer());
    }
    static String getHrefImpl(long peer) {
        return HTMLAreaElementNative.getHref(peer);
    }

    @Override
    public void setHref(String value) {
        setHrefImpl(getPeer(), value);
    }
    static void setHrefImpl(long peer, String value) {
        HTMLAreaElementNative.setHref(peer, value);
    }

    public String getOrigin() {
        return getOriginImpl(getPeer());
    }
    static String getOriginImpl(long peer) {
        return HTMLAreaElementNative.getOrigin(peer);
    }

    public String getProtocol() {
        return getProtocolImpl(getPeer());
    }
    static String getProtocolImpl(long peer) {
        return HTMLAreaElementNative.getProtocol(peer);
    }

    public void setProtocol(String value) {
        setProtocolImpl(getPeer(), value);
    }
    static void setProtocolImpl(long peer, String value) {
        HTMLAreaElementNative.setProtocol(peer, value);
    }

    public String getUsername() {
        return getUsernameImpl(getPeer());
    }
    static String getUsernameImpl(long peer) {
        return HTMLAreaElementNative.getUsername(peer);
    }

    public void setUsername(String value) {
        setUsernameImpl(getPeer(), value);
    }
    static void setUsernameImpl(long peer, String value) {
        HTMLAreaElementNative.setUsername(peer, value);
    }

    public String getPassword() {
        return getPasswordImpl(getPeer());
    }
    static String getPasswordImpl(long peer) {
        return HTMLAreaElementNative.getPassword(peer);
    }

    public void setPassword(String value) {
        setPasswordImpl(getPeer(), value);
    }
    static void setPasswordImpl(long peer, String value) {
        HTMLAreaElementNative.setPassword(peer, value);
    }

    public String getHost() {
        return getHostImpl(getPeer());
    }
    static String getHostImpl(long peer) {
        return HTMLAreaElementNative.getHost(peer);
    }

    public void setHost(String value) {
        setHostImpl(getPeer(), value);
    }
    static void setHostImpl(long peer, String value) {
        HTMLAreaElementNative.setHost(peer, value);
    }

    public String getHostname() {
        return getHostnameImpl(getPeer());
    }
    static String getHostnameImpl(long peer) {
        return HTMLAreaElementNative.getHostname(peer);
    }

    public void setHostname(String value) {
        setHostnameImpl(getPeer(), value);
    }
    static void setHostnameImpl(long peer, String value) {
        HTMLAreaElementNative.setHostname(peer, value);
    }

    public String getPort() {
        return getPortImpl(getPeer());
    }
    static String getPortImpl(long peer) {
        return HTMLAreaElementNative.getPort(peer);
    }

    public void setPort(String value) {
        setPortImpl(getPeer(), value);
    }
    static void setPortImpl(long peer, String value) {
        HTMLAreaElementNative.setPort(peer, value);
    }

    public String getPathname() {
        return getPathnameImpl(getPeer());
    }
    static String getPathnameImpl(long peer) {
        return HTMLAreaElementNative.getPathname(peer);
    }

    public void setPathname(String value) {
        setPathnameImpl(getPeer(), value);
    }
    static void setPathnameImpl(long peer, String value) {
        HTMLAreaElementNative.setPathname(peer, value);
    }

    public String getSearch() {
        return getSearchImpl(getPeer());
    }
    static String getSearchImpl(long peer) {
        return HTMLAreaElementNative.getSearch(peer);
    }

    public void setSearch(String value) {
        setSearchImpl(getPeer(), value);
    }
    static void setSearchImpl(long peer, String value) {
        HTMLAreaElementNative.setSearch(peer, value);
    }

    public String getHash() {
        return getHashImpl(getPeer());
    }
    static String getHashImpl(long peer) {
        return HTMLAreaElementNative.getHash(peer);
    }

    public void setHash(String value) {
        setHashImpl(getPeer(), value);
    }
    static void setHashImpl(long peer, String value) {
        HTMLAreaElementNative.setHash(peer, value);
    }

}

