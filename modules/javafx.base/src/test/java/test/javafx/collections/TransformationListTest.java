/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.transformation.TransformationList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.* ;

/**
 *
 */
@Disabled
public class TransformationListTest {

    private static class TransformationListImpl extends TransformationList<String, String> {

        public TransformationListImpl(ObservableList<String> list) {
            super(list);
        }

        @Override
        protected void sourceChanged(Change<? extends String> change) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String get(int index) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean addAll(String... es) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean setAll(String... es) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean setAll(Collection<? extends String> clctn) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getSourceIndex(int i) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getViewIndex(int index) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean removeAll(String... es) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean retainAll(String... es) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void remove(int i, int i1) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }

    private TransformationList<String, String> list1, list2;
    private ObservableList<String> list3;

    @BeforeEach
    public void setUp() {
        list3 = FXCollections.observableArrayList();
        list2 = new TransformationListImpl(list3);
        list1 = new TransformationListImpl(list2);
    }

    @Test
    public void testDirect() {
        assertEquals(list2, list1.getSource());
        assertEquals(list3, list2.getSource());
    }

}
