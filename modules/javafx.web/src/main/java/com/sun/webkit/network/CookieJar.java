/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class CookieJar {

    private CookieJar() {
    }

    private static void fwkPut(String url, String cookie) {
        @SuppressWarnings("removal")
        CookieHandler handler =
            AccessController.doPrivileged((PrivilegedAction<CookieHandler>) CookieHandler::getDefault);
        if (handler != null) {
            URI uri = null;
            try {
                uri = new URI(url);
                uri = rewriteToFilterOutHttpOnlyCookies(uri);
            } catch (URISyntaxException e) {
                return;
            }

            Map<String, List<String>> headers = new HashMap<>();
            List<String> val = new ArrayList<>();
            val.add(cookie);
            headers.put("Set-Cookie", val);
            try {
                handler.put(uri, headers);
            } catch (IOException e) {
            }
        }
    }

    private static String fwkGet(String url, boolean includeHttpOnlyCookies) {
        @SuppressWarnings("removal")
        CookieHandler handler =
            AccessController.doPrivileged((PrivilegedAction<CookieHandler>) CookieHandler::getDefault);
        if (handler != null) {
            URI uri = null;
            try {
                uri = new URI(url);
                if (!includeHttpOnlyCookies) {
                    uri = rewriteToFilterOutHttpOnlyCookies(uri);
                }
            } catch (URISyntaxException e) {
                return null;
            }

            Map<String, List<String>> headers = new HashMap<>();
            Map<String, List<String>> val = null;
            try {
                val = handler.get(uri, headers);
            } catch (IOException e) {
                return null;
            }
            if (val != null) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, List<String>> entry: val.entrySet()) {
                    String key = entry.getKey();
                    if ("Cookie".equalsIgnoreCase(key)) {
                        for (String s : entry.getValue()) {
                            if (sb.length() > 0) {
                                sb.append("; ");
                            }
                            sb.append(s);
                        }
                    }
                }
                return sb.toString();
            }
        }
        return null;
    }

    private static URI rewriteToFilterOutHttpOnlyCookies(URI uri)
        throws URISyntaxException
    {
        // RT-12200, RT-31072: Rewrite the 'https' scheme to
        // 'javascripts' to filter out HttpOnly cookies but
        // keep Secure cookies. Rewrite any other scheme
        // to 'javascript' to filter out both HttpOnly and
        // Secure cookies.
        return new URI(
                uri.getScheme().equalsIgnoreCase("https")
                        ? "javascripts" : "javascript",
                uri.getRawSchemeSpecificPart(),
                uri.getRawFragment());
    }
}
