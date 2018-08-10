/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javafx.application;

import java.net.URI;

import com.sun.javafx.application.HostServicesDelegate;

/**
 * This class provides HostServices for an Application. This includes
 * methods to get the code base and document base for an Application,
 * and to show a web page in a browser.
 *
 * @since JavaFX 2.0
 */
public final class HostServices {

    private final HostServicesDelegate delegate;

    /**
     * Package scope constructor to create the HostServices object.
     *
     * @param app the application class
     */
    HostServices(Application app) {
        delegate = HostServicesDelegate.getInstance(app);
    }

    /**
     * Gets the code base URI for this application.
     * This method returns
     * the directory containing the application jar file. If the
     * application is not packaged in a jar file, this method
     * returns the empty string.
     *
     * @return the code base URI for this application.
     */
    public final String getCodeBase() {
        return delegate.getCodeBase();
    }

    /**
     * Gets the document base URI for this application.
     * This method returns
     * the URI of the current directory.
     *
     * @return the document base URI for this application.
     */
    public final String getDocumentBase() {
        return delegate.getDocumentBase();
    }

    /**
     * Resolves the specified relative URI against the base URI and returns
     * the resolved URI.
     *
     * <p>Example:</p>
     * <pre>
     *     HostServices services = getHostServices();
     *     String myImage = services.resolveURI(services.getDocumentBase(),
     *                                          "image.jpg");
     *     Image image = new Image(myImage);
     * </pre>
     *
     * @param base the base URI against which to resolve the relative URI
     *
     * @param rel the relative URI to be resolved
     *
     * @throws NullPointerException if either the <code>base</code> or the
     * <code>rel</code> strings are null.
     * @throws IllegalArgumentException if there is an error parsing either
     * the <code>base</code> or <code>rel</code> URI strings, or if there is
     * any other error in resolving the URI.
     *
     * @return the fully resolved URI.
     */
    public final String resolveURI(String base, String rel) {
        URI uri = URI.create(base).resolve(rel);
        return uri.toString();
    }

    /**
     * Opens the specified URI in a new browser window or tab.
     * The determination of whether it is a new browser window or a tab in
     * an existing browser window will be made by the browser preferences.
     * Note that this will respect the pop-up blocker settings of the default
     * browser; it will not try to circumvent them.
     *
     * @param uri the URI of the web page that will be opened in a browser.
     */
    public final void showDocument(String uri) {
        delegate.showDocument(uri);
    }

}
