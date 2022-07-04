/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.collections;

import org.junit.jupiter.api.Test;
import javafx.collections.ModifiableObservableListBase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModifiableObservableListBaseTest {

    @Test
    public void testRemoveAllWithEmptyArgumentDoesNotEnumerateBackingList() {
        var list = new MockModifiableObservableList() {
            @Override
            public String get(int index) {
                throw new AssertionError("get() was not elided");
            }
        };

        list.removeAll(Collections.<String>emptyList());
    }

    @Test
    public void testRetainAllWithEmptyArgumentDoesNotCallContains() {
        new MockModifiableObservableList().retainAll(new ArrayList<String>() {
            @Override
            public boolean contains(Object o) {
                throw new AssertionError("contains() was not elided");
            }
        });
    }

    private static class MockModifiableObservableList extends ModifiableObservableListBase<String> {
        final List<String> backingList = new ArrayList<>(List.of("a", "b", "c"));

        @Override
        public String get(int index) {
            return backingList.get(index);
        }

        @Override
        public int size() {
            return backingList.size();
        }

        @Override
        protected void doAdd(int index, String element) {
            backingList.add(index, element);
        }

        @Override
        protected String doSet(int index, String element) {
            return backingList.set(index, element);
        }

        @Override
        protected String doRemove(int index) {
            return backingList.remove(index);
        }
    }

}
