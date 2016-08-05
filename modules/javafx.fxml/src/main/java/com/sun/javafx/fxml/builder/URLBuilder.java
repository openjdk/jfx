/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import javafx.util.Builder;

/**
 * Builder for constructing URL instances.
 */
public class URLBuilder extends AbstractMap<String, Object> implements Builder<URL> {
    private ClassLoader classLoader;

    private Object value = null;

    public static final String VALUE_KEY = "value";

    public URLBuilder(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Object put(String key, Object value) {
        if (key == null) {
            throw new NullPointerException();
        }

        if (key.equals(VALUE_KEY)) {
            this.value = value;
        } else {
            throw new IllegalArgumentException(key + " is not a valid property.");
        }

        return null;
    }

    @Override
    public URL build() {
        if (value == null) {
            throw new IllegalStateException();
        }

        URL url;
        if (value instanceof URL) {
            url = (URL)value;
        } else {
            String spec = value.toString();

            if (spec.startsWith("/")) {
                // FIXME: JIGSAW -- use Class.getResourceAsStream if resource is in a module
                url = classLoader.getResource(spec);
            } else {
                try {
                    url = new URL(spec);
                } catch (MalformedURLException exception) {
                    throw new RuntimeException(exception);
                }
            }
        }

        return url;
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        throw new UnsupportedOperationException();
    }
}
