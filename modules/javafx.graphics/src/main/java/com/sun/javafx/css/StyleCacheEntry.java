/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
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

import javafx.css.PseudoClass;
import javafx.scene.text.Font;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Caches the calculated value of each styleable property a specific combination of
 * pseudo-class states and font size (see {@link Key}). An entry is shared by every
 * {@code Styleable} that has that same combination.
 * <p>
 * {@link #get(String)} returning {@code null} means only that no one has evaluated that
 * property for this entry yet; it should never be treated as "no style applies"(!). This is
 * because when an entry is absent, it could be because the property was bound, or the property
 * didn't exist at the time of evaluation (because a Skin with that property wasn't attached yet
 * or Skins were swapped).
 * <p>
 * Therefore, callers must always resolve a miss with a real lookup before trusting the result; only
 * a value actually stored via {@link #put(String, CalculatedValue)} can be trusted to be reused.
 * <p>
 * Entries are (currently) keyed purely by property name, with no check that a cached value's type
 * still matches what the current lookup expects. This is harmless as long as every {@code
 * Styleable} that could share an entry agrees on the type behind a given property name (which
 * is generally standard for CSS properties). It is not a problem for two different {@code
 * CssMetaData} instances to have the same property name, only if their types would also differ.
 */
public final class StyleCacheEntry {

    public StyleCacheEntry() {
    }

    /**
     * Returns the {@link CalculatedValue} for the given property, or {@code null}
     * if none was cached.
     *
     * @param property a property to look up, cannot be {@code null}
     * @return a {@link CalculatedValue}, or {@code null} if none has been cached yet
     */
    public CalculatedValue get(String property) {

        CalculatedValue cv = null;
        if (calculatedValues != null && ! calculatedValues.isEmpty()) {
            cv = calculatedValues.get(property);
        }
        return cv;
    }

    /**
     * Stores the given {@link CalculatedValue} for a property.
     *
     * @param property a property to store a {@link CalculatedValue} for, cannot be {@code null}
     * @param calculatedValue a {@link CalculatedValue} to store, cannot be {@code null}
     */
    public void put(String property, CalculatedValue calculatedValue) {

        if (calculatedValues == null) {
            this.calculatedValues = new HashMap<>(5);
        }

        calculatedValues.put(property, calculatedValue);
    }

    public final static class Key {

        private final Set<PseudoClass>[] pseudoClassStates;
        private final double fontSize;
        private int hash = Integer.MIN_VALUE;

        public Key(Set<PseudoClass>[] pseudoClassStates, Font font) {

            this.pseudoClassStates = new Set[pseudoClassStates.length];
            for (int n=0; n<pseudoClassStates.length; n++) {
                this.pseudoClassStates[n] = ImmutablePseudoClassSetsCache.of(pseudoClassStates[n]);
            }
            this.fontSize = font != null ? font.getSize() : Font.getDefault().getSize();

        }

        @Override public String toString() {
            return Arrays.toString(pseudoClassStates) + ", " + fontSize;
        }

        public static int hashCode(double value) {
            long bits = Double.doubleToLongBits(value);
            return (int)(bits ^ (bits >>> 32));
        }

        @Override
        public int hashCode() {
            if (hash == Integer.MIN_VALUE) {

                hash = hashCode(fontSize);

                final int iMax = pseudoClassStates.length;

                for (int i=0; i<iMax; i++) {

                    final Set<PseudoClass> states = pseudoClassStates[i];
                    if (states != null) {
                        hash = 67 * (hash + states.hashCode());
                    }
                }
            }
            return hash;
        }

        @Override
        public boolean equals(Object obj) {

            if (obj == this) return true;

            if (obj == null || obj.getClass() != this.getClass()) return false;

            final Key other = (Key) obj;

            if (this.hash != other.hash) return false;

            //
            // double == double is not reliable since a double is kind of
            // a fuzzy value. And Double.compare is too precise.
            // For javafx, most sizes are rounded to the nearest tenth
            // (see SizeUnits.round) so comparing  here to the nearest
            // millionth is more than adequate.
            //
            // We assume that both fsize values are > 0, which is a safe assumption
            // because Font doesn't allow sizes < 0.
            final double diff = fontSize - other.fontSize;
            // Math.abs(diff, 0.000001) is too slow
            if (diff < -0.000001 || 0.000001 < diff) {
                return false;
            }

            if (pseudoClassStates.length != other.pseudoClassStates.length) {
                return false;
            }

            for (int i=0; i<pseudoClassStates.length; i++) {

                final Set<PseudoClass> this_pcs = pseudoClassStates[i];
                final Set<PseudoClass> other_pcs = other.pseudoClassStates[i];

                // if one is null, the other must be too
                if (this_pcs == null ? other_pcs != null : !this_pcs.equals(other_pcs)) {
                    return false;
                }
            }

            return true;
        }

    }

//    private final Reference<StyleCacheEntry> sharedCacheRef;
    private Map<String,CalculatedValue> calculatedValues;
//    private CalculatedValue  font; // for use in converting font relative sizes
}
