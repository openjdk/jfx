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
package com.sun.javafx.css;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javafx.css.PseudoClass;

/**
 * A cache for immutable sets of {@link PseudoClass}es.
 */
public class ImmutablePseudoClassSetsCache {
    private static final Map<Set<PseudoClass>, Set<PseudoClass>> CACHE = new HashMap<>();

    /**
     * Returns an immutable set of {@link PseudoClass}es.
     * <p>
     * Note: this method may or may not return the same instance for the same set of
     * {@link PseudoClass}es.
     *
     * @param pseudoClasses a set of {@link PseudoClass} to make immutable, cannot be {@code null}
     * @return an immutable set of {@link PseudoClass}es, never {@code null}
     */
    public static Set<PseudoClass> of(Set<PseudoClass> pseudoClasses) {
        Set<PseudoClass> immutableSet = Set.copyOf(pseudoClasses);

        /*
         * Note, the computeIfAbsent looks a bit odd, but it does serve a purpose.
         *
         * If the given set was already immutable; the copyOf will return the same
         * set; however, that may or may not be the set that is already in the cache.
         *
         * If the set already exists in the cache, then the exact same set or another
         * set is returned which was present already. This is okay.
         *
         * If the set didn't already exist, it is stored in the cache and that same set
         * is returned.
         */

        return CACHE.computeIfAbsent(immutableSet, k -> immutableSet);
    }
}
