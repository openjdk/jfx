/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

import com.sun.glass.ui.monocle.util.IntSet;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class IntSetTest {

    private Integer[] array;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Integer[][] sets = {
                { 1 },
                { 1, 2 },
                { 1, 2, 3},
                { 1, 1 },
                { 1, 1, 1 },
                { 1, 1, 2 },
        };
        return Arrays.asList(sets).stream()
                .map(d -> new Object[] { d })
                .collect(Collectors.toList());
    }

    public IntSetTest(Integer[] array) {
        this.array = array;
    }

    private int[] getIntSetAsArray(IntSet s) {
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

    private void assertSet(Set<Integer> expected, IntSet actual) {
        Assert.assertArrayEquals(
                "Expected: " + expected + ", found " + actual,
                getHashSetAsArray(expected),
                getIntSetAsArray(actual));
    }

    @Test
    public void testAddInOrderRemoveInOrder() {
        IntSet set = new IntSet();
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
        IntSet set = new IntSet();
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
        IntSet set = new IntSet();
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
        IntSet set = new IntSet();
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
