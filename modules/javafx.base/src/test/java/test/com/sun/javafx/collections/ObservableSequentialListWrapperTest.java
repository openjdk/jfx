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

import com.sun.javafx.collections.ObservableSequentialListWrapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static org.junit.jupiter.api.Assertions.*;

public class ObservableSequentialListWrapperTest {

    @Test
    public void testAddAllWithEmptyCollectionArgumentDoesNotEnumerateCollection() {
        var list = new ObservableSequentialListWrapper<String>(List.of()) {
            @Override
            public Iterator<String> iterator() {
                throw new AssertionError("iterator() was not elided");
            }

            @Override
            public ListIterator<String> listIterator() {
                throw new AssertionError("listIterator() was not elided");
            }
        };

        assertDoesNotThrow(() -> list.addAll(0, List.of()));
    }

    @Test
    public void testAddAllWithInvalidIndexThrowsIOOBE() {
        var nonEmptyList = new ObservableSequentialListWrapper<>(new ArrayList<>(List.of("a", "b", "c")));
        assertThrows(IndexOutOfBoundsException.class, () -> nonEmptyList.addAll(-1, List.of()));
        assertThrows(IndexOutOfBoundsException.class, () -> nonEmptyList.addAll(4, List.of()));
        assertDoesNotThrow(() -> nonEmptyList.addAll(3, List.of("d", "e")));

        var emptyList = new ObservableSequentialListWrapper<>(new ArrayList<>());
        assertThrows(IndexOutOfBoundsException.class, () -> emptyList.addAll(-1, List.of()));
        assertThrows(IndexOutOfBoundsException.class, () -> emptyList.addAll(1, List.of()));
        assertDoesNotThrow(() -> emptyList.addAll(0, List.of("d", "e")));
    }

}
