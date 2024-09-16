/*
 * Copyright (c) 2014, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.glass.ui.monocle;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.sun.glass.ui.monocle.IntSetShim;

public final class IntSetTest {

    private Integer[] array;

    private static Collection<Integer[]> parameters() {
        return List.of(
            new Integer[] { 1 },
            new Integer[] { 1, 2 },
            new Integer[] { 1, 2, 3 },
            new Integer[] { 1, 1 },
            new Integer[] { 1, 1, 1 },
            new Integer[] { 1, 1, 2 }
        );
    }

    public IntSetTest(Integer[] array) {
        this.array = array;
    }

    private int[] getIntSetAsArray(IntSetShim s) {
        int[] a = new int[s.size()];
        for (int i = 0; i < s.size(); i++) {
            a[i] = s.get(i);
        }
        Arrays.sort(a);
        return a;
    }

    private int[] getHashSetAsArray(Set<Integer> set) {
        return set.stream().sorted().mapToInt(x -> x).toArray();
    }

    private void assertSet(Set<Integer> expected, IntSetShim actual) {
        Assertions.assertArrayEquals(
                getHashSetAsArray(expected),
                getIntSetAsArray(actual),
                "Expected: " + expected + ", found " + actual);
    }

    @Test
    public void testAddInOrderRemoveInOrder() {
        IntSetShim set = new IntSetShim();
        Set<Integer> hashSet = new HashSet<>();
        assertSet(hashSet, set);
        for (int i = 0; i < array.length; i++) {
            set.addInt(array[i]);
            hashSet.add(array[i]);
            assertSet(hashSet, set);
        }
        for (int i = 0; i < array.length; i++) {
            set.removeInt(array[i]);
            hashSet.remove(array[i]);
            assertSet(hashSet, set);
        }
    }

    @Test
    public void testAddInOrderRemoveInReverse() {
        IntSetShim set = new IntSetShim();
        Set<Integer> hashSet = new HashSet<>();
        assertSet(hashSet, set);
        for (int i = 0; i < array.length; i++) {
            set.addInt(array[i]);
            hashSet.add(array[i]);
            assertSet(hashSet, set);
        }
        for (int i = array.length - 1; i >= 0; i--) {
            set.removeInt(array[i]);
            hashSet.remove(array[i]);
            assertSet(hashSet, set);
        }
    }

    @Test
    public void testAddInReverseRemoveInOrder() {
        IntSetShim set = new IntSetShim();
        Set<Integer> hashSet = new HashSet<>();
        assertSet(hashSet, set);
        for (int i = array.length - 1; i >= 0; i--) {
            set.addInt(array[i]);
            hashSet.add(array[i]);
            assertSet(hashSet, set);
        }
        for (int i = 0; i < array.length; i++) {
            set.removeInt(array[i]);
            hashSet.remove(array[i]);
            assertSet(hashSet, set);
        }
    }

    @Test
    public void testAddInReverseRemoveInReverse() {
        IntSetShim set = new IntSetShim();
        Set<Integer> hashSet = new HashSet<>();
        assertSet(hashSet, set);
        for (int i = array.length - 1; i >= 0; i--) {
            set.addInt(array[i]);
            hashSet.add(array[i]);
            assertSet(hashSet, set);
        }
        for (int i = array.length - 1; i >= 0; i--) {
            set.removeInt(array[i]);
            hashSet.remove(array[i]);
            assertSet(hashSet, set);
        }
    }

}
