/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.sun.javafx.css.BitSetShim;
import com.sun.javafx.css.PseudoClassState;

import javafx.beans.InvalidationListener;
import javafx.collections.SetChangeListener;
import javafx.css.PseudoClass;

public class BitSetTest {
    private final BitSetShim<PseudoClass> set = BitSetShim.getPseudoClassInstance();
    private final PseudoClass a = PseudoClass.getPseudoClass("a");
    private final PseudoClass b = PseudoClass.getPseudoClass("b");
    private final PseudoClass c = PseudoClass.getPseudoClass("c");

    @Test
    void setShouldProcessAddAndRemoveCorrectly() {
        assertEquals(0, set.size());
        assertTrue(set.isEmpty());

        set.add(a);

        assertEquals(1, set.size());
        assertTrue(set.contains(a));

        set.add(b);

        assertEquals(2, set.size());
        assertTrue(set.contains(a));
        assertTrue(set.contains(b));

        set.remove(a);

        assertEquals(1, set.size());
        assertTrue(set.contains(b));

        set.remove(a);

        assertEquals(1, set.size());
        assertTrue(set.contains(b));

        set.remove(b);

        assertEquals(0, set.size());
        assertTrue(set.isEmpty());

        set.remove(b);

        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
    }

    @Test
    void listenerManagementForInvalidationListenerShouldWorkCorrectly() {
        AtomicInteger invalidated = new AtomicInteger();

        InvalidationListener listener = obs -> invalidated.addAndGet(1);

        set.addListener(listener);
        set.add(a);

        assertEquals(1, invalidated.getAndSet(0));

        set.addListener(listener);  // added listener twice
        set.add(b);

        assertEquals(2, invalidated.getAndSet(0));  // called twice

        set.removeListener(listener);
        set.add(c);

        assertEquals(1, invalidated.getAndSet(0));  // called once

        set.removeListener(listener);
        set.remove(a);

        assertEquals(0, invalidated.getAndSet(0));  // not called
    }

    @Test
    void listenerManagementForSetChangeListenerShouldWorkCorrectly() {
        AtomicInteger changed = new AtomicInteger();

        SetChangeListener<PseudoClass> listener = obs -> changed.addAndGet(1);

        set.addListener(listener);
        set.add(a);

        assertEquals(1, changed.getAndSet(0));

        set.addListener(listener);  // added listener twice
        set.add(b);

        assertEquals(2, changed.getAndSet(0));  // called twice

        set.removeListener(listener);
        set.add(c);

        assertEquals(1, changed.getAndSet(0));  // called once

        set.removeListener(listener);
        set.remove(a);

        assertEquals(0, changed.getAndSet(0));  // not called
    }

    @Test
    void shouldBeEqualAfterGrowAndShrink() {
        PseudoClassState set1 = new PseudoClassState();
        PseudoClassState set2 = new PseudoClassState();

        set1.add(PseudoClassState.getPseudoClass("abc"));
        set2.add(PseudoClassState.getPseudoClass("abc"));

        assertEquals(set1, set2);

        for (int i = 0; i < 1000; i++) {
            // grow internal bit set array:
            set1.add(PseudoClassState.getPseudoClass("" + i));

            assertNotEquals(set1, set2);
        }

        for (int i = 0; i < 1000; i++) {
            set1.remove(PseudoClassState.getPseudoClass("" + i));
        }

        // still equal despite internal array sizes being different size:
        assertEquals(set1, set2);
    }

    @Test
    void twoEmptyBitSetsShouldBeEqual() {

        /*
         * Per Set contract, the empty set is equal to any other empty set.
         */

        assertEquals(Set.of(), new PseudoClassState());
        assertEquals(new PseudoClassState(), Set.of());
    }
}
