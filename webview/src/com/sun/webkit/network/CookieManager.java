/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.network;

import java.net.CookieHandler;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An RFC 6265-compliant cookie handler.
 */
public final class CookieManager extends CookieHandler {

    private static final Logger logger =
        Logger.getLogger(CookieManager.class.getName());


    private final CookieStore store = new CookieStore();


    /**
     * Creates a new {@code CookieManager}.
     */
    public CookieManager() {
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String,List<String>> get(URI uri,
            Map<String,List<String>> requestHeaders)
    {
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "uri: [{0}], requestHeaders: {1}",
                    new Object[] {uri, toLogString(requestHeaders)});
        }

        if (uri == null) {
            throw new IllegalArgumentException("uri is null");
        }
        if (requestHeaders == null) {
            throw new IllegalArgumentException("requestHeaders is null");
        }

        String cookieString = get(uri);

        Map<String,List<String>> result;
        if (cookieString != null) {
            result = new HashMap<String,List<String>>();
            result.put("Cookie", Arrays.asList(cookieString));
        } else {
            result = Collections.emptyMap();
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "result: {0}", toLogString(result));
        }
        return result;
    }

    /**
     * Returns the cookie string for a given URI.
     */
    private String get(URI uri) {
        String host = uri.getHost();
        if (host == null || host.length() == 0) {
            logger.log(Level.FINEST, "Null or empty URI host, returning null");
            return null;
        }
        host = canonicalize(host);

        String scheme = uri.getScheme();
        boolean secureProtocol = "https".equalsIgnoreCase(scheme)
                || "javascripts".equalsIgnoreCase(scheme);
        boolean httpApi = "http".equalsIgnoreCase(scheme)
                || "https".equalsIgnoreCase(scheme);

        List<Cookie> cookieList;
        synchronized (store) {
            cookieList = store.get(host, uri.getPath(),
                    secureProtocol, httpApi);
        }

        StringBuilder sb = new StringBuilder();
        for (Cookie cookie : cookieList) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(cookie.getName());
            sb.append('=');
            sb.append(cookie.getValue());
        }

        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(URI uri, Map<String,List<String>> responseHeaders) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "uri: [{0}], responseHeaders: {1}",
                    new Object[] {uri, toLogString(responseHeaders)});
        }

        if (uri == null) {
            throw new IllegalArgumentException("uri is null");
        }
        if (responseHeaders == null) {
            throw new IllegalArgumentException("responseHeaders is null");
        }

        for (Map.Entry<String,List<String>> entry : responseHeaders.entrySet())
        {
            String key = entry.getKey();
            if (!"Set-Cookie".equalsIgnoreCase(key)) {
                continue;
            }
            ExtendedTime currentTime = ExtendedTime.currentTime();
            // RT-15907: Process the list of headers in reverse order,
            // effectively restoring the order in which the headers were
            // received from the server. This is a temporary workaround for
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7059532
            ListIterator<String> it =
                    entry.getValue().listIterator(entry.getValue().size());
            while (it.hasPrevious()) {
                Cookie cookie = Cookie.parse(it.previous(), currentTime);
                if (cookie != null) {
                    put(uri, cookie);
                    currentTime = currentTime.incrementSubtime();
                }
            }
        }
    }

    /**
     * Puts an individual cookie.
     */
    private void put(URI uri, Cookie cookie) {
        logger.log(Level.FINEST, "cookie: {0}", cookie);

        String host = uri.getHost();
        if (host == null || host.length() == 0) {
            logger.log(Level.FINEST, "Null or empty URI host, ignoring cookie");
            return;
        }
        host = canonicalize(host);

        if (PublicSuffixes.isPublicSuffix(cookie.getDomain())) {
            if (cookie.getDomain().equals(host)) {
                cookie.setDomain("");
            } else {
                logger.log(Level.FINEST, "Domain is public suffix, "
                        + "ignoring cookie");
                return;
            }
        }

        if (cookie.getDomain().length() > 0) {
            if (!Cookie.domainMatches(host, cookie.getDomain())) {
                logger.log(Level.FINEST, "Hostname does not match domain, "
                        + "ignoring cookie");
                return;
            } else {
                cookie.setHostOnly(false);
            }
        } else {
            cookie.setHostOnly(true);
            cookie.setDomain(host);
        }

        if (cookie.getPath() == null) {
            cookie.setPath(Cookie.defaultPath(uri));
        }

        boolean httpApi = "http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme());
        if (cookie.getHttpOnly() && !httpApi) {
            logger.log(Level.FINEST, "HttpOnly cookie received from non-HTTP "
                    + "API, ignoring cookie");
            return;
        }

        synchronized (store) {
            Cookie oldCookie = store.get(cookie);
            if (oldCookie != null) {
                if (oldCookie.getHttpOnly() && !httpApi) {
                    logger.log(Level.FINEST, "Non-HTTP API attempts to "
                            + "overwrite HttpOnly cookie, blocked");
                    return;
                }
                cookie.setCreationTime(oldCookie.getCreationTime());
            }

            store.put(cookie);
        }

        logger.log(Level.FINEST, "Stored: {0}", cookie);
    }

    /**
     * Converts a map of HTTP headers to a string suitable for displaying
     * in the log.
     */
    private static String toLogString(Map<String,List<String>> headers) {
        if (headers == null) {
            return null;
        }
        if (headers.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String,List<String>> entry : headers.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                sb.append(String.format("%n    "));
                sb.append(key);
                sb.append(": ");
                sb.append(value);
            }
        }
        return sb.toString();
    }

    /**
     * Canonicalizes a hostname as required by RFC 6265.
     */
    private static String canonicalize(String hostname) {
        // The hostname is already all-ASCII at this point
        return hostname.toLowerCase();
    }
}
