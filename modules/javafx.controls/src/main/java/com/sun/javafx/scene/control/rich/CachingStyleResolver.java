/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.rich;

import java.util.HashMap;
import javafx.scene.Node;
import javafx.scene.control.rich.StyleResolver;
import javafx.scene.control.rich.model.CssStyles;
import javafx.scene.control.rich.model.StyleAttrs;
import javafx.scene.image.WritableImage;

/**
 * Caching StyleResolver caches conversion results to avoid re-querying for the same information.
 */
public class CachingStyleResolver implements StyleResolver {
    private final StyleResolver resolver;
    private final HashMap<CssStyles, StyleAttrs> cache = new HashMap<>();

    public CachingStyleResolver(StyleResolver r) {
        this.resolver = r;
    }

    @Override
    public StyleAttrs resolveStyles(StyleAttrs attrs) {
        CssStyles css = attrs.getCssStyles();
        if (css != null) {
            // no conversion is needed
            return attrs;
        }

        StyleAttrs a = cache.get(css);
        if (a == null) {
            a = resolver.resolveStyles(attrs);
            cache.put(css, a);
        }
        return a;
    }

    @Override
    public WritableImage snapshot(Node node) {
        return resolver.snapshot(node);
    }
}