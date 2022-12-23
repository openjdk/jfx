/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.webkit.network;

import java.net.URI;

public class CookieShim {

    Cookie cookie;

    private CookieShim(Cookie cookie) {
        this.cookie = cookie;
    }

    public static CookieShim parse(String setCookieString, ExtendedTimeShim currentTime) {
        Cookie c = Cookie.parse(setCookieString, currentTime.getExtendedTime());
        if (c == null) {
            return null;
        }
        return new CookieShim(c);
    }

    public String getName() {
        return cookie.getName();
    }

    public String getValue() {
        return cookie.getValue();
    }

    public long getExpiryTime() {
        return cookie.getExpiryTime();
    }

    public String getDomain() {
        return cookie.getDomain();
    }

    public String getPath() {
        return cookie.getPath();
    }

    public ExtendedTimeShim getCreationTime() {
        return new ExtendedTimeShim(cookie.getCreationTime());
    }

    public long getLastAccessTime() {
        return cookie.getLastAccessTime();
    }

    public boolean getPersistent() {
        return cookie.getPersistent();
    }

    public boolean getHostOnly() {
        return cookie.getHostOnly();
    }

    public boolean getSecureOnly() {
        return cookie.getSecureOnly();
    }

    public boolean getHttpOnly() {
        return cookie.getHttpOnly();
    }

    public boolean hasExpired() {
        return cookie.hasExpired();
    }

    public boolean equals(CookieShim cs) {
        return cookie.equals(cs != null ? cs.cookie: null);
    }

    @Override
    public int hashCode() {
        return cookie.hashCode();
    }

    public static boolean domainMatches(String domain, String cookieDomain) {
        return Cookie.domainMatches(domain, cookieDomain);
    }

    public static String defaultPath(URI uri) {
        return Cookie.defaultPath(uri);
    }

    public static boolean pathMatches(String path, String cookiePath) {
        return Cookie.pathMatches(path, cookiePath);
    }

}
