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

package test.com.sun.javafx;

import com.sun.javafx.UnmodifiableArrayList;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UnmodifiableArrayListTest {

    @Test
    void testCopyOfNullFilteredList_nullList() {
        assertThrows(NullPointerException.class, () -> UnmodifiableArrayList.copyOfNullFiltered((List<?>)null));
    }

    @Test
    void testCopyOfNullFilteredList_nullArray() {
        assertThrows(NullPointerException.class, () -> UnmodifiableArrayList.copyOfNullFiltered((Object[])null));
    }

    @Test
    void testCopyOfNullFilteredList_randomAccess() {
        var list = new ArrayList<String>();
        list.add("a");
        list.add(null);
        list.add("b");
        list.add(null);

        assertEquals(List.of("a", "b"), UnmodifiableArrayList.copyOfNullFiltered(list));
    }

    @Test
    void testCopyOfNullFilteredList_nonRandomAccess() {
        var list = new LinkedList<String>();
        list.add("a");
        list.add(null);
        list.add("b");
        list.add(null);

        assertEquals(List.of("a", "b"), UnmodifiableArrayList.copyOfNullFiltered(list));
    }

    @Test
    void testCopyOfNullFilteredArray() {
        var list = new String[4];
        list[0] = "a";
        list[1] = null;
        list[2] = "b";
        list[3] = null;

        assertEquals(List.of("a", "b"), UnmodifiableArrayList.copyOfNullFiltered(list));
    }
}
