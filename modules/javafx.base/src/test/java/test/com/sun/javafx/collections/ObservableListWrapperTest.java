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

package test.com.sun.javafx.collections;

import com.sun.javafx.collections.ObservableListWrapper;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ObservableListWrapperTest {

    @Test
    public void testRemoveAllWithEmptyArgumentDoesNotEnumerateBackingList() {
        var list = new ObservableListWrapper<>(new ArrayList<>(List.of("a", "b", "c")) {
            @Override
            public String get(int index) {
                throw new AssertionError("get() was not elided");
            }
        });

        list.removeAll(Collections.<String>emptyList());
    }

    @Test
    public void testRetainAllWithEmptyArgumentDoesNotCallContains() {
        var list = new ObservableListWrapper<>(new ArrayList<>(List.of("a", "b", "c")));

        list.retainAll(new ArrayList<String>() {
            @Override
            public boolean contains(Object o) {
                throw new AssertionError("contains() was not elided");
            }
        });
    }

    @Test
    public void testRemoveAllWithNullArgumentThrowsNPE() {
        var list = new ObservableListWrapper<>(List.of("a", "b", "c"));
        assertThrows(NullPointerException.class, () -> list.removeAll((Collection<?>) null));
    }

    @Test
    public void testRemoveAllWithEmptyListArgumentWorksCorrectly() {
        var list = new ObservableListWrapper<>(new ArrayList<>(List.of("a", "b", "c")));
        assertFalse(list.removeAll(List.of()));
        assertEquals(List.of("a", "b", "c"), list);
    }

    @Test
    public void testRetainAllWithNullArgumentThrowsNPE() {
        var list = new ObservableListWrapper<>(List.of("a", "b", "c"));
        assertThrows(NullPointerException.class, () -> list.retainAll((Collection<?>) null));
    }

    @Test
    public void testRetainAllWithEmptyListArgumentWorksCorrectly() {
        var list = new ObservableListWrapper<>(new ArrayList<>(List.of("a", "b", "c")));
        assertTrue(list.retainAll(List.of()));
        assertTrue(list.isEmpty());
    }

}
