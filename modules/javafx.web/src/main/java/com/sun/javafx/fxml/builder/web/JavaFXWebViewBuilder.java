/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.fxml.builder.web;

import java.util.AbstractMap;
import java.util.Set;

import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.util.Builder;

/**
 * JavaFX WebView builder.
 *
 * TODO: Implement this to fix RT-40037.
 *
 * This should be implemented using the pattern from
 * JavaFXBuilderFactory$ObjectBuilderWrapper$ObjectBuilder modified to use
 * setters and getters rather than builder methods for all WebView properties
 * (including those inherited from Parent and Node). The only special-cased
 * properties should be those that need to delegate to WebEngine. Once this is
 * done, the remaining 4 legacy builder classes can be removed from this package,
 * as can the JavaFXBuilderFactory$ObjectBuilderWrapper class.
 */
public class JavaFXWebViewBuilder extends AbstractMap<String, Object> implements Builder<WebView> {

    private final WebView view = new WebView();

    private String      location = "";
    // ...

    @Override
    public WebView build() {
        // Note that the WebView node is already constructed. All we need to
        // do is handle the delegated WebEngine properties

        WebEngine engine = view.getEngine();
        if (location != null) {
            engine.load(location);
        }
        // ...
        return view;
    }

    @Override
    public Object put(String key, Object value) {
        if ( value != null) {
            String str = value.toString();

            if ( "location".equals( key)) {
                location = str;
            } else if ( "onAlert".equals(key)) {
                // ...
            } else {
                // Handle generic attribute
            }
        }

        return null;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
