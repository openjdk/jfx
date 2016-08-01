/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.net.MalformedURLException;
import java.net.NetPermission;
import java.net.URL;
import java.net.URLStreamHandler;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A collection of static methods for URL creation.
 */
public final class URLs {

    /**
     * The mapping between WebPane-specific protocol names and their
     * respective handlers.
     */
    private static final Map<String,URLStreamHandler> handlerMap;
    static {
        Map<String,URLStreamHandler> map =
                new HashMap<String,URLStreamHandler>(2);
        map.put("about", new com.sun.webkit.network.about.Handler());
        map.put("data", new com.sun.webkit.network.data.Handler());
        handlerMap = Collections.unmodifiableMap(map);
    }

    private static final Permission streamHandlerPermission =
        new NetPermission("specifyStreamHandler");

    /**
     * The private default constructor. Ensures non-instantiability.
     */
    private URLs() {
        throw new AssertionError();
    }


    /**
     * Creates a {@code URL} object from the {@code String} representation.
     * This method is equivalent to the {@link URL#URL(String)} constructor
     * with the additional support for WebPane-specific protocol handlers.
     * @param spec the {@code String} to parse as a {@code URL}.
     * @throws MalformedURLException if the string specifies an unknown
     *         protocol.
     */
    public static URL newURL(String spec) throws MalformedURLException {
        return newURL(null, spec);
    }

    /**
     * Creates a URL by parsing the given spec within a specified context.
     * This method is equivalent to the {@link URL#URL(URL,String)}
     * constructor with the additional support for WebPane-specific protocol
     * handlers.
     * @param context the context in which to parse the specification.
     * @param spec the {@code String} to parse as a {@code URL}.
     * @throws MalformedURLException if no protocol is specified, or an
     *         unknown protocol is found.
     */
    public static URL newURL(final URL context, final String spec)
        throws MalformedURLException
    {
        try {
            // Try the standard protocol handler selection procedure
            return new URL(context, spec);
        } catch (MalformedURLException ex) {

            // Try WebPane-specific protocol handler, if any
            int colonPosition = spec.indexOf(':');
            final URLStreamHandler handler = (colonPosition != -1) ?
                handlerMap.get(spec.substring(0, colonPosition).toLowerCase()) :
                null;

            if (handler == null) throw ex;

            try {
                // We should be able to specify one of our stream handlers for the URL
                // when running as an applet or a web start app.
                return AccessController.doPrivileged((PrivilegedAction<URL>) () -> {
                    try {
                        return new URL(context, spec, handler);
                    } catch (MalformedURLException muex) {
                        throw new RuntimeException(muex);
                    }
                }, null, streamHandlerPermission);

            } catch (RuntimeException re) {
                if (re.getCause() instanceof MalformedURLException) {
                    throw (MalformedURLException)re.getCause();
                }
                throw re;
            }
        }
    }
}
