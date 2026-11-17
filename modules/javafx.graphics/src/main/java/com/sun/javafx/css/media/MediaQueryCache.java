/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css.media;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A cache for {@link MediaQuery} instances that is used to deduplicate media queries. More specifically,
 * this cache ensures that only a single instance of any distinct media query exists at any point in time.
 * This cache holds weak references, ensuring that media queries that are no longer in use will be eligible
 * for garbage collection.
 */
public final class MediaQueryCache {

    private MediaQueryCache() {}

    private static final Map<MediaQuery, WeakReference<MediaQuery>> CACHE = new WeakHashMap<>();

    @SuppressWarnings("unchecked")
    public static synchronized <T extends MediaQuery> T getCachedMediaQuery(T query) {
        if (CACHE.get(query) instanceof WeakReference<MediaQuery> wref
                && wref.get() instanceof MediaQuery cachedQuery) {
            return (T)cachedQuery;
        }

        CACHE.put(query, new WeakReference<>(query));
        return query;
    }
}
