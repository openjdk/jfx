/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.network;

import java.net.MalformedURLException;
import static com.sun.webkit.network.URLs.newURL;

/**
 * A collection of static utility methods dealing with network I/O.
 */
public final class Util {

    /**
     * The private default constructor. Ensures non-instantiability.
     */
    private Util() {
        throw new AssertionError();
    }

    /**
     * Converts a string representing a valid URL to an equivalent string
     * suitable for passing into WebKit.
     * @param url the string to be converted. Must represent a valid URL.
     * @return the resulting string suitable for passing into WebKit.
     * @throws NullPointerException if {@code url} is {@code null}.
     * @throws MalformedURLException if {@code url} is not a valid URL.
     */
    public static String adjustUrlForWebKit(String url)
        throws MalformedURLException
    {
        if (newURL(url).getProtocol().equals("file")) {
            // If there are no slashes after "file:", WebKit will
            // assume two slashes and will treat the component
            // that follows as the hostname. The meaning of the resulting
            // URL will be very different from the one in Java.
            // We therefore change no slashes to three slashes here to
            // eliminate that inconsistency.
            // URLs with one or more slashes after "file:" have identical
            // meaning in both Java and WebKit and therefore don't
            // need to be modified.
            int pos = "file:".length();
            if (pos < url.length() && url.charAt(pos) != '/') {
                url = url.substring(0, pos) + "///" + url.substring(pos);
            }
        }
        return url;
    }

    static String formatHeaders(String headers) {
        return headers.trim().replaceAll("(?m)^", "    ");
    }
}
