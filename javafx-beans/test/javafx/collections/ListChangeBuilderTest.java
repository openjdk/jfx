/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.collections.ObservableListWrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ListChangeBuilderTest {

    private ListChangeBuilder<String> builder;
    private ObservableListWrapper<String> observableList;
    private ArrayList<String> list;
    private MockListObserver<String> observer;
    
    @Before
    public void setUp() {
        observer = new MockListObserver<String>();
        list = new ArrayList<String>(Arrays.asList("a", "b", "c", "d"));
        observableList = new ObservableListWrapper<String>(list);
        observableList.addListener(observer);
        builder = new ListChangeBuilder<String>(observableList);
    }

    @Test
    public void testAddRemove() {
        builder.beginChange();
        list.remove(2);
        builder.nextRemove(2, "c");
        list.add(2, "cc");
        list.add(3, "ccc");
        builder.nextAdd(2, 4);
        list.remove(2);
        builder.nextRemove(2, "cc");
        list.remove(3);
        builder.nextRemove(3, "d");
        list.add(0, "aa");
        builder.nextAdd(0, 1);
        builder.endChange();

        assertEquals(list, Arrays.asList("aa", "a", "b", "ccc"));

        observer.checkAddRemove(0, observableList, Collections.EMPTY_LIST, 0, 1);
        observer.checkAddRemove(1, observableList, Arrays.asList("c", "d"), 3, 4);
    }

    @Test
    public void testAddRemove_2() {
        builder.beginChange();
        list.add("e");
        builder.nextAdd(4, 5);
        list.add(3, "dd");
        builder.nextAdd(3, 4);
        list.remove(4);
        builder.nextRemove(4, "d");

        list.remove(0);
        builder.nextRemove(0, "a");
        builder.endChange();

        assertEquals(list, Arrays.asList("b", "c", "dd", "e"));

        observer.checkAddRemove(0, observableList, Arrays.asList("a"), 0, 0);
        observer.checkAddRemove(1, observableList, Arrays.asList("d"), 2, 4);
    }

    @Test
    public void testAddRemove_3() {
        builder.beginChange();
        list.add("e");
        builder.nextAdd(4, 5);

        list.set(0, "aa");
        builder.nextReplace(0, 1, Arrays.asList("a"));

        list.remove(4);
        builder.nextRemove(4, "e");

        list.remove(0);
        builder.nextRemove(0, "aa");
        builder.endChange();

        assertEquals(list, Arrays.asList("b", "c", "d"));

        observer.check1AddRemove(observableList, Arrays.asList("a"), 0, 0);

    }
    
    @Test
    public void testAddRemove_4() {
        builder.beginChange();

        list.add("e");
        builder.nextAdd(4, 5);

        list.remove(1);
        builder.nextRemove(1, "b");

        list.add(1, "bb");
        builder.nextAdd(1, 2);

        builder.endChange();

        assertEquals(list, Arrays.asList("a", "bb", "c", "d", "e"));

        observer.checkAddRemove(0, observableList, Arrays.asList("b"), 1, 2);
        observer.checkAddRemove(1, observableList, Collections.EMPTY_LIST, 4, 5);
    }

    @Test
    public void testAdd() {
        builder.beginChange();

        list.add(1, "aa");
        builder.nextAdd(1, 2);
        list.add(5, "e");
        builder.nextAdd(5, 6);
        list.add(1, "aa");
        builder.nextAdd(1, 2);
        list.add(2, "aa");
        builder.nextAdd(2, 3);
        list.add(4, "aa");
        builder.nextAdd(4, 5);

        builder.endChange();
        
        assertEquals(list, Arrays.asList("a", "aa","aa","aa","aa", "b", "c", "d", "e"));

        observer.checkAddRemove(0, observableList, Collections.EMPTY_LIST, 1, 5);
        observer.checkAddRemove(1, observableList, Collections.EMPTY_LIST, 8, 9);

    }

    @Test
    public void testRemove() {
        builder.beginChange();
        list.remove(0);
        builder.nextRemove(0, "a");
        list.remove(2);
        builder.nextRemove(2, "d");
        list.remove(0);
        builder.nextRemove(0, "b");
        builder.endChange();

        assertEquals(list, Arrays.asList("c"));

        observer.checkAddRemove(0, observableList, Arrays.asList("a", "b"), 0, 0);
        observer.checkAddRemove(1, observableList, Arrays.asList("d"), 1, 1);

    }

    @Test
    public void testRemove_2() {
        builder.beginChange();
        list.remove(1);
        builder.nextRemove(1, "b");
        list.remove(2);
        builder.nextRemove(2, "d");
        list.remove(0);
        builder.nextRemove(0, "a");
        builder.endChange();

        assertEquals(list, Arrays.asList("c"));

        observer.checkAddRemove(0, observableList, Arrays.asList("a", "b"), 0, 0);
        observer.checkAddRemove(1, observableList, Arrays.asList("d"), 1, 1);

    }

    @Test
    public void testUpdate() {
        builder.beginChange();
        builder.nextUpdate(1);
        builder.nextUpdate(0);
        builder.nextUpdate(3);
        builder.endChange();

        observer.checkUpdate(0, observableList, 0, 2);
        observer.checkUpdate(1, observableList, 3, 4);
    }

    @Test
    public void testUpdate_2() {
        builder.beginChange();
        builder.nextUpdate(3);
        builder.nextUpdate(1);
        builder.nextUpdate(0);
        builder.nextUpdate(0);
        builder.nextUpdate(2);
        builder.endChange();

        observer.checkUpdate(0, observableList, 0, 4);
    }

    @Test
    public void testPermutation() {
        builder.beginChange();

        builder.nextPermutation(0, 4, new int[] {3, 2, 1, 0});
        builder.nextPermutation(1, 4, new int[] {3, 2, 1});
        builder.endChange();

        observer.check1Permutation(observableList, new int[] {1, 2, 3, 0});
    }

    @Test
    public void testUpdateAndAddRemove() {
        builder.beginChange();
        builder.nextUpdate(1);
        builder.nextUpdate(2);
        list.remove(2);
        builder.nextRemove(2, "c");
        list.add(2, "cc");
        list.add(3, "ccc");
        builder.nextAdd(2, 4);
        builder.nextUpdate(2);
        list.remove(2);
        builder.nextRemove(2, "cc");
        list.remove(3);
        builder.nextRemove(3, "d");
        list.add(0, "aa");
        builder.nextAdd(0, 1);
        builder.endChange();

        assertEquals(list, Arrays.asList("aa", "a", "b", "ccc"));

        observer.checkAddRemove(0, observableList, Collections.EMPTY_LIST, 0, 1);
        observer.checkAddRemove(1, observableList, Arrays.asList("c", "d"), 3, 4);
        observer.checkUpdate(2, observableList, 2, 3);
    }

    @Test
    public void testUpdateAndAddRemove_2() {
        builder.beginChange();
        builder.nextUpdate(0);
        builder.nextUpdate(1);
        list.add(1,"aaa");
        list.add(1,"aa");
        builder.nextAdd(1, 3);
        builder.endChange();

        assertEquals(list, Arrays.asList("a", "aa", "aaa", "b", "c", "d"));

        observer.checkAddRemove(0, observableList, Collections.EMPTY_LIST, 1, 3);
        observer.checkUpdate(1, observableList, 0, 1);
        observer.checkUpdate(2, observableList, 3, 4);
    }

    @Test
    public void testUpdateAndAddRemove_3() {
        builder.beginChange();
        builder.nextUpdate(2);
        builder.nextUpdate(3);
        list.add(1,"aa");
        builder.nextAdd(1, 2);
        list.remove(0);
        builder.nextRemove(0, "a");
        builder.endChange();

        assertEquals(list, Arrays.asList("aa", "b", "c", "d"));

        observer.checkAddRemove(0, observableList, Arrays.asList("a"), 0, 1);
        observer.checkUpdate(1, observableList, 2, 4);
    }

    @Test
    public void testUpdateAndPermutation() {
        builder.beginChange();

        builder.nextUpdate(1);
        builder.nextUpdate(2);
        builder.nextPermutation(1, 4, new int[] {3, 2, 1});
        builder.endChange();

        observer.checkPermutation(0, observableList, 1, 4, new int[] {3, 2, 1});
        observer.checkUpdate(1, observableList, 2, 4);
    }

    @Test
    public void testUpdateAndPermutation_2() {
        builder.beginChange();

        builder.nextUpdate(0);
        builder.nextUpdate(2);
        builder.nextPermutation(0, 4, new int[] {1, 3, 2, 0});
        builder.endChange();

        observer.checkPermutation(0, observableList, 0, 4, new int[] {1, 3, 2, 0});
        observer.checkUpdate(1, observableList, 1, 3);
    }

    @Test(expected=IllegalStateException.class)
    public void testNextAddWithoutBegin() {
        builder.nextAdd(0, 1);
    }

    @Test(expected=IllegalStateException.class)
    public void testNextRemoveWithoutBegin() {
        builder.nextRemove(0, (String)null);
    }

    @Test(expected=IllegalStateException.class)
    public void testNextRemove2WithoutBegin() {
        builder.nextRemove(0, Collections.EMPTY_LIST);
    }

    @Test(expected=IllegalStateException.class)
    public void testNextUpdateWithoutBegin() {
        builder.nextUpdate(0);
    }

    @Test(expected=IllegalStateException.class)
    public void testNextSetWithoutBegin() {
        builder.nextSet(0, null);
    }

    @Test(expected=IllegalStateException.class)
    public void testNextReplaceWithoutBegin() {
        builder.nextReplace(0, 1, Collections.EMPTY_LIST);
    }
    
    @Test
    public void testEmpty() {
        builder.beginChange();
        builder.endChange();
        
        observer.check0();
    }
    
    @Test
    public void testToString_Update() {
        observableList.removeListener(observer);
        observableList.addListener(new ListChangeListener<String>() {

            @Override
            public void onChanged(ListChangeListener.Change<? extends String> change) {
                assertNotNull(change.toString());
            }
            
        });
        builder.beginChange();
        
        builder.nextUpdate(0);
        
        builder.endChange();
    }
    
    @Test
    public void testToString_Add() {
        observableList.removeListener(observer);
        observableList.addListener(new ListChangeListener<String>() {

            @Override
            public void onChanged(ListChangeListener.Change<? extends String> change) {
                assertNotNull(change.toString());
            }
            
        });
        builder.beginChange();
        
        builder.nextAdd(0, 1);
        
        builder.endChange();
    }
    
    @Test
    public void testToString_Remove() {
        observableList.removeListener(observer);
        observableList.addListener(new ListChangeListener<String>() {

            @Override
            public void onChanged(ListChangeListener.Change<? extends String> change) {
                assertNotNull(change.toString());
            }
            
        });
        builder.beginChange();
        
        builder.nextRemove(0, "");
        
        builder.endChange();
    }
    
    @Test
    public void testToString_Composed() {
        observableList.removeListener(observer);
        observableList.addListener(new ListChangeListener<String>() {

            @Override
            public void onChanged(ListChangeListener.Change<? extends String> change) {
                assertNotNull(change.toString());
            }
            
        });
        builder.beginChange();
        
        builder.nextUpdate(0);
        
        builder.nextAdd(0, 3);
        
        builder.endChange();
    }
    
    @Test
    public void testToString_Permutation() {
        observableList.removeListener(observer);
        observableList.addListener(new ListChangeListener<String>() {

            @Override
            public void onChanged(ListChangeListener.Change<? extends String> change) {
                assertNotNull(change.toString());
            }
            
        });
        builder.beginChange();
        
        builder.nextPermutation(0, 2, new int[] {1, 0});
        
        builder.endChange();
    }

}
