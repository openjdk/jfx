/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.network;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class CookieJar {

    private CookieJar() {
    }

    private static void fwkPut(String url, String cookie) {
        CookieHandler handler = CookieHandler.getDefault();
        if (handler != null) {
            URI uri = null;
            try {
                uri = new URI(url);
                // RT-12200: Rewrite the scheme to javascript to avoid
                // modifying HttpOnly cookies
                uri = new URI("javascript", uri.getRawSchemeSpecificPart(),
                        uri.getRawFragment());
            } catch (URISyntaxException e) {
                return;
            }

            Map<String, List<String>> headers = new HashMap<String, List<String>>();
            List<String> val = new ArrayList<String>();
            val.add(cookie);
            headers.put("Set-Cookie", val);
            try {
                handler.put(uri, headers);
            } catch (IOException e) {
            }
        }
    }

    private static String fwkGet(String url, boolean includeHttpOnlyCookies) {
        CookieHandler handler = CookieHandler.getDefault();
        if (handler != null) {
            URI uri = null;
            try {
                uri = new URI(url);
                if (!includeHttpOnlyCookies) {
                    // RT-12200: Rewrite the scheme to javascript to avoid
                    // returning HttpOnly cookies
                    uri = new URI("javascript", uri.getRawSchemeSpecificPart(),
                            uri.getRawFragment());
                }
            } catch (URISyntaxException e) {
                return null;
            }

            Map<String, List<String>> headers = new HashMap<String, List<String>>();
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
}
