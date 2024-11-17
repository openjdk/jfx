/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sun.javafx.css.FixedCapacitySet;

public class FixedCapacitySetTest {
    private final Set<Object> elements = new HashSet<>();

    @ParameterizedTest
    @EnumSource(Scenario.class)
    void sizeShouldMatchExpected(Scenario scenario) {
        FixedCapacitySet<Object> set = createSet(scenario);

        assertEquals(scenario.size, set.size(), "must have size " + scenario.size + ": " + scenario);
    }

    @ParameterizedTest
    @EnumSource(Scenario.class)
    void containsShouldFindExpectedElements(Scenario scenario) {
        FixedCapacitySet<Object> set = createSet(scenario);

        for(int i = 0; i < scenario.size; i++) {
            assertTrue(set.contains("" + i), "must contain " + i + ": " + scenario);
        }
    }

    @ParameterizedTest
    @EnumSource(Scenario.class)
    void containsShouldNotFindMissingElements(Scenario scenario) {
        FixedCapacitySet<Object> set = createSet(scenario);

        for(int i = 0; i < 100; i++) {
            assertFalse(set.contains("A" + i), "must not contain A" + i + ": " + scenario);
        }
    }

    @ParameterizedTest
    @EnumSource(Scenario.class)
    void iterationShouldResultInExpectedElements(Scenario scenario) {
        FixedCapacitySet<Object> set = createSet(scenario);
        Set<Object> iterationResult = new HashSet<>();

        set.forEach(iterationResult::add);

        assertEquals(elements, iterationResult, "iteration must return " + elements + ": " + scenario);
    }

    @ParameterizedTest
    @EnumSource(Scenario.class)
    void iterationShouldThrowExceptionWhenThereAreNoMoreElements(Scenario scenario) {
        FixedCapacitySet<Object> set = createSet(scenario);

        Iterator<Object> iterator = set.iterator();

        for(int i = 0; i < scenario.size; i++) {
            iterator.next();
        }

        assertThrows(NoSuchElementException.class, iterator::next, "exception expected when iterating past last element: " + scenario);
    }

    @ParameterizedTest
    @EnumSource(Scenario.class)
    void addShouldRejectNullValues(Scenario scenario) {
        FixedCapacitySet<Object> set = createSet(scenario);

        if(scenario.size != 0) {
            assertThrows(NullPointerException.class, () -> set.add(null), "exception expected when adding null: " + scenario);
        }
    }

    @ParameterizedTest
    @EnumSource(Scenario.class)
    void addShouldRejectExceedingCapacity(Scenario scenario) {
        FixedCapacitySet<Object> set = createSet(scenario);

        // fill up partially filled sets first:
        while(set.size() < scenario.capacity) {
            set.add("" + set.size());
        }

        if(scenario.size != 0) {
            assertThrows(IllegalStateException.class, () -> set.add("A"), "exception expected when exceeding capacity: " + scenario);
        }
    }

    @ParameterizedTest
    @EnumSource(Scenario.class)
    void addShouldIgnoreDuplicateElement(Scenario scenario) {
        FixedCapacitySet<Object> set = createSet(scenario);

        if(scenario.size != 0) {
            assertFalse(set.add("0"), "adding duplicate element should return false: " + scenario);
        }
    }

    @ParameterizedTest
    @EnumSource(Scenario.class)
    void hashCodeShouldMatchContract(Scenario scenario) {
        FixedCapacitySet<Object> set = createSet(scenario);

        assertEquals(elements.hashCode(), set.hashCode(), "hashCode must match contract: " + scenario);
    }

    @ParameterizedTest
    @EnumSource(Scenario.class)
    void isSuperSetOfShouldWork(Scenario scenario) {
        FixedCapacitySet<Object> set = createSet(scenario);

        assertTrue(set.isSuperSetOf(set), "isSuperSetOf with same set should return true: " + scenario);
        assertTrue(set.isSuperSetOf(elements), "isSuperSetOf with equivalent set should return true: " + scenario);

        List<Object> extendedSet = Stream.concat(elements.stream(), Stream.of("A")).toList();

        assertTrue(set.isSuperSetOf(extendedSet), "isSuperSetOf with extended equivalent set should return true: " + scenario);

        if(scenario.size > 1) {
            Set<Object> partialSet = new HashSet<>(elements);

            partialSet.remove("1");

            assertFalse(set.isSuperSetOf(partialSet), "isSuperSetOf with collection missing an element should return false: " + scenario);
        }

        if(scenario.size == 0) {
            assertTrue(set.isSuperSetOf(Set.of()), "isSuperSetOf with empty set should return true: " + scenario);
        }

        if(scenario.size != 0) {
            assertFalse(set.isSuperSetOf(Set.of("A")), "isSuperSetOf with collection containing missing element should return false: " + scenario);
        }
    }

    @ParameterizedTest
    @EnumSource(Scenario.class)
    void removalMethodsShouldThrowUnsupportedOperationException(Scenario scenario) {
        FixedCapacitySet<Object> set = createSet(scenario);

        if(scenario.size != 0) {
            assertThrows(UnsupportedOperationException.class, () -> set.remove("0"), "expected exception when calling remove: " + scenario);
            assertThrows(UnsupportedOperationException.class, () -> set.clear(), "expected exception when calling clear: " + scenario);
        }

        assertThrows(UnsupportedOperationException.class, () -> set.iterator().remove(), "expected exception when calling remove on iterator: " + scenario);
    }

    @ParameterizedTest
    @EnumSource(Scenario.class)
    void afterFreezingAddShouldThrowUnsupportedOperationException(Scenario scenario) {
        FixedCapacitySet<Object> set = createSet(scenario);

        set.freeze();

        assertThrows(UnsupportedOperationException.class, () -> set.add("A"), "expected exception when calling add on frozen set: " + scenario);
    }

    @Test
    void creationShouldRejectNegativeSize() {
        assertThrows(NegativeArraySizeException.class, () -> FixedCapacitySet.of(-1));
    }

    @Test
    void openAddressedSpecialCasewhenMultipleElementsHashToLastBucketShouldStoreElementsInStartingBuckets() {
        FixedCapacitySet<Object> set = FixedCapacitySet.of(10);  // 10 results in open addressed variant, with 16 buckets

        // carefully selected elements that all hash to last bucket (bucket 15):
        set.addAll(Set.of("10", "21", "32"));

        assertTrue(set.contains("10"));
        assertTrue(set.contains("21"));
        assertTrue(set.contains("32"));
    }

    private FixedCapacitySet<Object> createSet(Scenario scenario) {
        FixedCapacitySet<Object> set = FixedCapacitySet.of(scenario.capacity);

        for(int i = 0; i < scenario.size; i++) {
            set.add("" + i);
            elements.add("" + i);
        }

        return set;
    }

    enum Scenario {
        EMPTY(0, 0),
        ONE_FULLY_FILLED(1, 1),
        TWO_FULLY_FILLED(2, 2),
        THREE_FULLY_FILLED(3, 3),
        FIVE_FULLY_FILLED(5, 5),
        NINE_FULLY_FILLED(9, 9),
        TEN_FULLY_FILLED(10, 10),
        TWENTY_FULLY_FILLED(20, 20),

        ONE_BUT_EMPTY(1, 0),
        TWO_BUT_EMPTY(2, 0),
        TWO_HALF_FILLED(2, 1),
        THREE_PARTIALLY_FILLED(3, 2),
        FIVE_PARTIALLY_FILLED(5, 4),
        TWENTY_ALMOST_FILLED(20, 19);

        private int capacity;
        private int size;

        Scenario(int capacity, int size) {
            this.capacity = capacity;
            this.size = size;
        }
    }
}
