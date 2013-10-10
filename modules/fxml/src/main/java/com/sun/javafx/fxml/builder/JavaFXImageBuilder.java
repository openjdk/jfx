/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.fxml.builder;

import java.util.AbstractMap;
import java.util.Set;

import javafx.scene.image.Image;
import javafx.util.Builder;

/**
 * JavaFX image builder.
 */
public class JavaFXImageBuilder extends AbstractMap<String, Object> implements Builder<Image> {
    private String      url = "";
    private double      requestedWidth = 0;
    private double      requestedHeight = 0;
    private boolean     preserveRatio = false;
    private boolean     smooth = false;
    private boolean     backgroundLoading = false;

    @Override
    public Image build() {
        return new Image( url, requestedWidth, requestedHeight, preserveRatio, smooth, backgroundLoading);
    }

    @Override
    public Object put(String key, Object value) {
        if ( value != null) {
            String str = value.toString();

            if ( "url".equals( key)) {
                url = str;
            } else if ( "requestedWidth".equals(key)) {
                requestedWidth =  Double.parseDouble( str);
            } else if ( "requestedHeight".equals(key)) {
                requestedHeight =  Double.parseDouble(str);
            } else if ( "preserveRatio".equals(key)) {
                preserveRatio =  Boolean.parseBoolean(str);
            } else if ( "smooth".equals(key)) {
                smooth =  Boolean.parseBoolean(str);
            } else if ( "backgroundLoading".equals(key)) {
                backgroundLoading = Boolean.parseBoolean(str);
            } else {
                throw new IllegalArgumentException("Unknown Image property: " + key);
            }
        }

        return null;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        throw new UnsupportedOperationException();
    }
}
