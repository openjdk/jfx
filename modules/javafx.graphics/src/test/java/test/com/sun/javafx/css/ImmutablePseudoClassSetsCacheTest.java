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

package test.com.sun.javafx.css;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sun.javafx.css.ImmutablePseudoClassSetsCache;
import com.sun.javafx.css.PseudoClassState;

import javafx.css.PseudoClass;

public class ImmutablePseudoClassSetsCacheTest {

    @Test
    void shouldCacheSets() {
        Set<PseudoClass> myOwnSet = Set.of(PseudoClass.getPseudoClass("a"));
        Set<PseudoClass> pseudoClassState = new PseudoClassState();

        pseudoClassState.add(PseudoClass.getPseudoClass("a"));

        Set<PseudoClass> set1 = ImmutablePseudoClassSetsCache.of(new HashSet<>(Set.of(PseudoClass.getPseudoClass("a"))));
        Set<PseudoClass> set2 = ImmutablePseudoClassSetsCache.of(new HashSet<>(myOwnSet));
        Set<PseudoClass> set3 = ImmutablePseudoClassSetsCache.of(myOwnSet);
        Set<PseudoClass> set4 = ImmutablePseudoClassSetsCache.of(pseudoClassState);
        Set<PseudoClass> set5 = ImmutablePseudoClassSetsCache.of(Set.of(PseudoClass.getPseudoClass("b")));

        assertEquals(set1, set2);
        assertEquals(set2, set3);
        assertEquals(set3, set4);
        assertNotEquals(set1, set5);
        assertNotEquals(set2, set5);
        assertNotEquals(set3, set5);
        assertNotEquals(set4, set5);

        assertSame(set1, set2);
        assertSame(set2, set3);
        assertSame(set3, set4);

        assertEquals(myOwnSet, set1);

        // this does not need to be true if this set was not the first one cached
        assertNotSame(myOwnSet, set1);

        // tests if hashCode/equals of BitSet respects contract...
        assertEquals(myOwnSet.hashCode(), pseudoClassState.hashCode());
        assertEquals(myOwnSet, pseudoClassState);
        assertEquals(pseudoClassState, myOwnSet);
    }
}
