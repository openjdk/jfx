/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A cache to store values from lookup.
 * Consider that there are some number of StyleHelpers and that the
 * StyleHelpers are shared. For a particular node, a style might come
 * from its StyleHelper or the StyleHelper of one of its parents (ignoring
 * Node.style for now). What makes a style unique is the set of StyleHelpers
 * that go into its calculation. So, if node N has StyleHelper A and its
 * parents have StyleHelper B and C, the opacity style (say) for N is going
 * to be unique to the set of StyleHelpers [A B C]. Because StyleHelpers
 * are chosen by the rules they match, and because StyleHelpers are shared,
 * every node that has the set of StyleHelpers [A B C] will match the same
 * selector for opacity (for a given pseudoclass state). Further, the value for
 * opacity (in the given pseudoclass state) will not change for the given
 * set of StyleHelpers. Therefore, rather than trying to cache a calculated
 * value with an individual StyleHelper, the value can be cached with a key
 * that uniquely identifies the set [A B C]. Subsequent lookups for the
 * property do not need to be recalculated even if there are lookups in the
 * value. Incidentally, resolved references will also be unique to a set of
 * StyleHelpers and would only need to be resolved once (for a given
 * pseudoclass state).
 *
 * Node.style puts a slight wrinkle in that the style might come from the
 * Node rather than the cache. This can be handled in a relatively
 * straight-forward manner. If Node.style is not empty or null and it
 * contains the property, then that style should be used if the style
 * compares less than the style that would have been applied. If there is
 * some parent with Node.style that would affect the child Node's style,
 * then the cached value can be used.
 *
 * The key is comprised of this helper's key, plus the
 * keys of all this node's parents' helpers.
 *
 * The values in the cache styles that apply are determined
 * by the node's state and the state of its parents. This unique combination
 * of states reflects the state of the node and its parents at the time the
 * style was first determined. Provided the node and its parents are in the
 * same state, then the styles applied will be the same as what is in the
 * cache.
 *
 * The value could be a Map, but there should not be a large number of
 * entries. Computing the key from the long[], doing the lookup and
 * resolving collisions is probably just as bad, if not worse, than
 * finding the matching set of states by comparing the long[].
 *
 * Since all StyleHelpers are relevant to a Scene, valueCache is
 * created by StyleManager.StylesheetContainer and is passed in.
 * Note that all StyleHelper instances within a given Scene all
 * share the same valueCache!
 */
public final class StyleCache {

    public StyleCache() {
        // no-op
    }

    public void clear() {
        if (entries == null) return;
        Thread.dumpStack();
        entries.clear();
    }

    public StyleCacheEntry getStyleCacheEntry(StyleCacheEntry.Key key) {

        StyleCacheEntry entry = null;
        if (entries != null) {
            entry = entries.get(key);
        }
        return entry;
    }

    public void addStyleCacheEntry(StyleCacheEntry.Key key, StyleCacheEntry entry) {
        if (entries == null) {
            entries = new HashMap<>(5);
        }
        entries.put(key, entry);
    }

    public static final class Key {

        public Key(int[] styleMapIds, int count) {
            this.styleMapIds = new int[count];
            System.arraycopy(styleMapIds, 0, this.styleMapIds, 0, count);
            }

        public Key(Key other) {
            this(other.styleMapIds, other.styleMapIds.length);
        }

        public int[] getStyleMapIds() {
            return styleMapIds;
        }

        @Override public String toString() {
            return Arrays.toString(styleMapIds);
        }

        @Override
        public int hashCode() {
            if (hash == Integer.MIN_VALUE) {
                hash = 3;
                if (styleMapIds != null) {
                    for (int i=0; i<styleMapIds.length; i++) {
                        final int id = styleMapIds[i];
                        hash = 17 * (hash + id);
                    }
                }
            }
            return hash;
        }

        @Override
        public boolean equals(Object obj) {

            if (obj == this) return true;

            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }

            final Key other = (Key) obj;

            if (this.hash != other.hash) return false;

            // if one is null, so too must the other
            if ((this.styleMapIds == null) ^ (other.styleMapIds == null)) {
                return false;
            }

            // if one is null, so is the other
            if (this.styleMapIds == null) {
                return true;
            }

            if (this.styleMapIds.length != other.styleMapIds.length) {
                return false;
            }

            for (int i=0; i<styleMapIds.length; i++) {
                if (styleMapIds[i] != other.styleMapIds[i]) {
                    return false;
                }
            }

            return true;

        }

        final int[] styleMapIds;
        private int hash = Integer.MIN_VALUE;
    }

    private Map<StyleCacheEntry.Key,StyleCacheEntry> entries;

}
