/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package javafx.collections;

import com.sun.javafx.collections.transformation.TransformationList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.collections.ListChangeListener.Change;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.* ;

/**
 *
 */
@Ignore
public class TransformationListTest {

    private static class TransformationListImpl extends TransformationList<String, String> {

        public TransformationListImpl(List<String> list) {
            super(list);
        }

        @Override
        protected void onSourceChanged(Change<? extends String> change) {
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
    private List<String> list3;

    @Before
    public void setUp() {
        list3 = new ArrayList<String>();
        list2 = new TransformationListImpl(list3);
        list1 = new TransformationListImpl(list2);
    }

    @Test
    public void testDirect() {
        assertEquals(list2, list1.getDirectSource());
        assertEquals(list3, list2.getDirectSource());
    }


    @Test
    public void testBottom() {
        assertEquals(list3, list1.getBottomMostSource());
    }
    


}
