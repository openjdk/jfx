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
import org.w3c.dom.html.HTMLAnchorElement;

public class HTMLAnchorElementImpl extends HTMLElementImpl implements HTMLAnchorElement {
    HTMLAnchorElementImpl(long peer) {
        super(peer);
    }

    static HTMLAnchorElement getImpl(long peer) {
        return (HTMLAnchorElement)create(peer);
    }


// Attributes
    @Override
    public String getCharset() {
        return getCharsetImpl(getPeer());
    }
    static String getCharsetImpl(long peer) {
        return HTMLAnchorElementNative.getCharset(peer);
    }

    @Override
    public void setCharset(String value) {
        setCharsetImpl(getPeer(), value);
    }
    static void setCharsetImpl(long peer, String value) {
        HTMLAnchorElementNative.setCharset(peer, value);
    }

    @Override
    public String getCoords() {
        return getCoordsImpl(getPeer());
    }
    static String getCoordsImpl(long peer) {
        return HTMLAnchorElementNative.getCoords(peer);
    }

    @Override
    public void setCoords(String value) {
        setCoordsImpl(getPeer(), value);
    }
    static void setCoordsImpl(long peer, String value) {
        HTMLAnchorElementNative.setCoords(peer, value);
    }

    @Override
    public String getHreflang() {
        return getHreflangImpl(getPeer());
    }
    static String getHreflangImpl(long peer) {
        return HTMLAnchorElementNative.getHreflang(peer);
    }

    @Override
    public void setHreflang(String value) {
        setHreflangImpl(getPeer(), value);
    }
    static void setHreflangImpl(long peer, String value) {
        HTMLAnchorElementNative.setHreflang(peer, value);
    }

    @Override
    public String getName() {
        return getNameImpl(getPeer());
    }
    static String getNameImpl(long peer) {
        return HTMLAnchorElementNative.getName(peer);
    }

    @Override
    public void setName(String value) {
        setNameImpl(getPeer(), value);
    }
    static void setNameImpl(long peer, String value) {
        HTMLAnchorElementNative.setName(peer, value);
    }

    public String getPing() {
        return getPingImpl(getPeer());
    }
    static String getPingImpl(long peer) {
        return HTMLAnchorElementNative.getPing(peer);
    }

    public void setPing(String value) {
        setPingImpl(getPeer(), value);
    }
    static void setPingImpl(long peer, String value) {
        HTMLAnchorElementNative.setPing(peer, value);
    }

    @Override
    public String getRel() {
        return getRelImpl(getPeer());
    }
    static String getRelImpl(long peer) {
        return HTMLAnchorElementNative.getRel(peer);
    }

    @Override
    public void setRel(String value) {
        setRelImpl(getPeer(), value);
    }
    static void setRelImpl(long peer, String value) {
        HTMLAnchorElementNative.setRel(peer, value);
    }

    @Override
    public String getRev() {
        return getRevImpl(getPeer());
    }
    static String getRevImpl(long peer) {
        return HTMLAnchorElementNative.getRev(peer);
    }

    @Override
    public void setRev(String value) {
        setRevImpl(getPeer(), value);
    }
    static void setRevImpl(long peer, String value) {
        HTMLAnchorElementNative.setRev(peer, value);
    }

    @Override
    public String getShape() {
        return getShapeImpl(getPeer());
    }
    static String getShapeImpl(long peer) {
        return HTMLAnchorElementNative.getShape(peer);
    }

    @Override
    public void setShape(String value) {
        setShapeImpl(getPeer(), value);
    }
    static void setShapeImpl(long peer, String value) {
        HTMLAnchorElementNative.setShape(peer, value);
    }

    @Override
    public String getTarget() {
        return getTargetImpl(getPeer());
    }
    static String getTargetImpl(long peer) {
        return HTMLAnchorElementNative.getTarget(peer);
    }

    @Override
    public void setTarget(String value) {
        setTargetImpl(getPeer(), value);
    }
    static void setTargetImpl(long peer, String value) {
        HTMLAnchorElementNative.setTarget(peer, value);
    }

    @Override
    public String getType() {
        return getTypeImpl(getPeer());
    }
    static String getTypeImpl(long peer) {
        return HTMLAnchorElementNative.getType(peer);
    }

    @Override
    public void setType(String value) {
        setTypeImpl(getPeer(), value);
    }
    static void setTypeImpl(long peer, String value) {
        HTMLAnchorElementNative.setType(peer, value);
    }

    public String getText() {
        return getTextImpl(getPeer());
    }
    static String getTextImpl(long peer) {
        return HTMLAnchorElementNative.getText(peer);
    }

    public void setText(String value) throws DOMException {
        setTextImpl(getPeer(), value);
    }
    static void setTextImpl(long peer, String value) {
        HTMLAnchorElementNative.setText(peer, value);
    }

    @Override
    public String getHref() {
        return getHrefImpl(getPeer());
    }
    static String getHrefImpl(long peer) {
        return HTMLAnchorElementNative.getHref(peer);
    }

    @Override
    public void setHref(String value) {
        setHrefImpl(getPeer(), value);
    }
    static void setHrefImpl(long peer, String value) {
        HTMLAnchorElementNative.setHref(peer, value);
    }

    public String getOrigin() {
        return getOriginImpl(getPeer());
    }
    static String getOriginImpl(long peer) {
        return HTMLAnchorElementNative.getOrigin(peer);
    }

    public String getProtocol() {
        return getProtocolImpl(getPeer());
    }
    static String getProtocolImpl(long peer) {
        return HTMLAnchorElementNative.getProtocol(peer);
    }

    public void setProtocol(String value) {
        setProtocolImpl(getPeer(), value);
    }
    static void setProtocolImpl(long peer, String value) {
        HTMLAnchorElementNative.setProtocol(peer, value);
    }

    public String getUsername() {
        return getUsernameImpl(getPeer());
    }
    static String getUsernameImpl(long peer) {
        return HTMLAnchorElementNative.getUsername(peer);
    }

    public void setUsername(String value) {
        setUsernameImpl(getPeer(), value);
    }
    static void setUsernameImpl(long peer, String value) {
        HTMLAnchorElementNative.setUsername(peer, value);
    }

    public String getPassword() {
        return getPasswordImpl(getPeer());
    }
    static String getPasswordImpl(long peer) {
        return HTMLAnchorElementNative.getPassword(peer);
    }

    public void setPassword(String value) {
        setPasswordImpl(getPeer(), value);
    }
    static void setPasswordImpl(long peer, String value) {
        HTMLAnchorElementNative.setPassword(peer, value);
    }

    public String getHost() {
        return getHostImpl(getPeer());
    }
    static String getHostImpl(long peer) {
        return HTMLAnchorElementNative.getHost(peer);
    }

    public void setHost(String value) {
        setHostImpl(getPeer(), value);
    }
    static void setHostImpl(long peer, String value) {
        HTMLAnchorElementNative.setHost(peer, value);
    }

    public String getHostname() {
        return getHostnameImpl(getPeer());
    }
    static String getHostnameImpl(long peer) {
        return HTMLAnchorElementNative.getHostname(peer);
    }

    public void setHostname(String value) {
        setHostnameImpl(getPeer(), value);
    }
    static void setHostnameImpl(long peer, String value) {
        HTMLAnchorElementNative.setHostname(peer, value);
    }

    public String getPort() {
        return getPortImpl(getPeer());
    }
    static String getPortImpl(long peer) {
        return HTMLAnchorElementNative.getPort(peer);
    }

    public void setPort(String value) {
        setPortImpl(getPeer(), value);
    }
    static void setPortImpl(long peer, String value) {
        HTMLAnchorElementNative.setPort(peer, value);
    }

    public String getPathname() {
        return getPathnameImpl(getPeer());
    }
    static String getPathnameImpl(long peer) {
        return HTMLAnchorElementNative.getPathname(peer);
    }

    public void setPathname(String value) {
        setPathnameImpl(getPeer(), value);
    }
    static void setPathnameImpl(long peer, String value) {
        HTMLAnchorElementNative.setPathname(peer, value);
    }

    public String getSearch() {
        return getSearchImpl(getPeer());
    }
    static String getSearchImpl(long peer) {
        return HTMLAnchorElementNative.getSearch(peer);
    }

    public void setSearch(String value) {
        setSearchImpl(getPeer(), value);
    }
    static void setSearchImpl(long peer, String value) {
        HTMLAnchorElementNative.setSearch(peer, value);
    }

    public String getHash() {
        return getHashImpl(getPeer());
    }
    static String getHashImpl(long peer) {
        return HTMLAnchorElementNative.getHash(peer);
    }

    public void setHash(String value) {
        setHashImpl(getPeer(), value);
    }
    static void setHashImpl(long peer, String value) {
        HTMLAnchorElementNative.setHash(peer, value);
    }

}

