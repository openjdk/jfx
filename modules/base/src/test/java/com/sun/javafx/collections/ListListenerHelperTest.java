/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.collections;

import com.sun.javafx.binding.ExpressionHelper;
import javafx.beans.InvalidationListener;
import javafx.beans.InvalidationListenerMock;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MockListObserver;
import javafx.collections.ObservableList;
import org.junit.Before;
import org.junit.Test;

import java.util.BitSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ListListenerHelperTest {
    
    private InvalidationListenerMock[] invalidationListenerMock;

    private MockListObserver<Object>[] changeListenerMock;

    private ListListenerHelper<Object> helper;

    private ObservableList<Object> list;
    private ListChangeListener.Change<Object> change;

    @Before
    public void setUp() {
        invalidationListenerMock = new InvalidationListenerMock[] {
                new InvalidationListenerMock(),
                new InvalidationListenerMock(),
                new InvalidationListenerMock(),
                new InvalidationListenerMock()
        };
        changeListenerMock = new MockListObserver[] {
                new MockListObserver<Object>(),
                new MockListObserver<Object>(),
                new MockListObserver<Object>(),
                new MockListObserver<Object>()
        };
        helper = null;
        list = FXCollections.emptyObservableList();
        change = new NonIterableChange.SimpleRemovedChange<Object>(0, 1, new Object(), list);
    }

    private void resetAllListeners() {
        for (final InvalidationListenerMock listener : invalidationListenerMock) {
            listener.reset();
        }
        for (final MockListObserver<Object> listener : changeListenerMock) {
            listener.clear();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testAddInvalidationListener_Null() {
        ListListenerHelper.addListener(helper, (InvalidationListener)null);
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveInvalidationListener_Null() {
        ListListenerHelper.removeListener(helper, (InvalidationListener) null);
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveListChangeListener_Null() {
        ListListenerHelper.removeListener(helper, (ListChangeListener<Object>) null);
    }

    @Test(expected = NullPointerException.class)
    public void testAddListChangeListener_Null() {
        ListListenerHelper.addListener(helper, (ListChangeListener<Object>) null);
    }

    @Test
    public void testEmpty() {
        assertFalse(ListListenerHelper.hasListeners(helper));

        // these should be no-ops
        ListListenerHelper.fireValueChangedEvent(helper, change);
        ListListenerHelper.removeListener(helper, invalidationListenerMock[0]);
        ListListenerHelper.removeListener(helper, changeListenerMock[0]);
    }

    @Test
    public void testInvalidation_Simple() {
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[0]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(list, 1);

        helper = ListListenerHelper.removeListener(helper, invalidationListenerMock[1]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(list, 1);
        invalidationListenerMock[1].check(null, 0);

        helper = ListListenerHelper.removeListener(helper, changeListenerMock[0]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(list, 1);
        changeListenerMock[0].check0();

        helper = ListListenerHelper.removeListener(helper, invalidationListenerMock[0]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
    }

    @Test
    public void testInvalidation_AddInvalidation() {
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[0]);
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[1]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(list, 1);
        invalidationListenerMock[1].check(list, 1);
    }

    @Test
    public void testInvalidation_AddChange() {
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[0]);
        helper = ListListenerHelper.addListener(helper, changeListenerMock[0]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(list, 1);
        changeListenerMock[0].check1();
    }
    
    @Test
    public void testInvalidation_ChangeInPulse() {
        final InvalidationListener listener = observable -> {
            helper = ListListenerHelper.addListener(helper, invalidationListenerMock[0]);
        };
        helper = ListListenerHelper.addListener(helper, listener);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        helper = ListListenerHelper.removeListener(helper, listener);
        invalidationListenerMock[0].reset();
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(list, 1);
    }

    @Test
    public void testChange_Simple() {
        helper = ListListenerHelper.addListener(helper, changeListenerMock[0]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check1();
        changeListenerMock[0].clear();

        helper = ListListenerHelper.removeListener(helper, changeListenerMock[1]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check1();
        changeListenerMock[1].check0();
        changeListenerMock[0].clear();

        helper = ListListenerHelper.removeListener(helper, invalidationListenerMock[0]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check1();
        invalidationListenerMock[0].check(null, 0);
        changeListenerMock[0].clear();

        helper = ListListenerHelper.removeListener(helper, changeListenerMock[0]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check0();
        changeListenerMock[0].clear();
    }

    @Test
    public void testChange_AddInvalidation() {
        helper = ListListenerHelper.addListener(helper, changeListenerMock[0]);
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[0]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check1();
        invalidationListenerMock[0].check(list, 1);
    }

    @Test
    public void testChange_AddChange() {
        helper = ListListenerHelper.addListener(helper, changeListenerMock[0]);
        helper = ListListenerHelper.addListener(helper, changeListenerMock[1]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check1();
        changeListenerMock[1].check1();
    }

    @Test
    public void testChange_ChangeInPulse() {
        final ListChangeListener<Object> listener = c -> {
            helper = ListListenerHelper.addListener(helper, changeListenerMock[0]);
        };
        helper = ListListenerHelper.addListener(helper, listener);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        helper = ListListenerHelper.removeListener(helper, listener);
        changeListenerMock[0].clear();
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check1();
    }

    @Test
    public void testGeneric_AddInvalidation() {
        helper = ListListenerHelper.addListener(helper, changeListenerMock[0]);
        helper = ListListenerHelper.addListener(helper, changeListenerMock[1]);

        // first invalidation listener creates the array
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[0]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(list, 1);

        // second and third invalidation listener enlarge the array
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[1]);
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[2]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(list, 1);
        invalidationListenerMock[1].check(list, 1);
        invalidationListenerMock[2].check(list, 1);

        // fourth invalidation listener fits into the array
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[3]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(list, 1);
        invalidationListenerMock[1].check(list, 1);
        invalidationListenerMock[2].check(list, 1);
        invalidationListenerMock[3].check(list, 1);
    }
    
    @Test
    public void testGeneric_AddInvalidationInPulse() {
        final ListChangeListener<Object> addListener = new ListChangeListener<Object>() {
            int counter;
            @Override
            public void onChanged(Change<? extends Object> c) {
                helper = ListListenerHelper.addListener(helper, invalidationListenerMock[counter++]);
            }
        };
        helper = ListListenerHelper.addListener(helper, changeListenerMock[0]);

        helper = ListListenerHelper.addListener(helper, addListener);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        helper = ListListenerHelper.removeListener(helper, addListener);
        resetAllListeners();
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(list, 1);
        invalidationListenerMock[1].check(null, 0);
        invalidationListenerMock[2].check(null, 0);
        invalidationListenerMock[3].check(null, 0);

        helper = ListListenerHelper.addListener(helper, addListener);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        helper = ListListenerHelper.removeListener(helper, addListener);
        resetAllListeners();
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(list, 1);
        invalidationListenerMock[1].check(list, 1);
        invalidationListenerMock[2].check(null, 0);
        invalidationListenerMock[3].check(null, 0);

        helper = ListListenerHelper.addListener(helper, addListener);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        helper = ListListenerHelper.removeListener(helper, addListener);
        resetAllListeners();
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(list, 1);
        invalidationListenerMock[1].check(list, 1);
        invalidationListenerMock[2].check(list, 1);
        invalidationListenerMock[3].check(null, 0);

        helper = ListListenerHelper.addListener(helper, addListener);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        helper = ListListenerHelper.removeListener(helper, addListener);
        resetAllListeners();
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(list, 1);
        invalidationListenerMock[1].check(list, 1);
        invalidationListenerMock[2].check(list, 1);
        invalidationListenerMock[3].check(list, 1);
    }

    @Test
    public void testGeneric_RemoveInvalidation() {
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[0]);
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[1]);
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[2]);
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[3]);

        // remove first element
        helper = ListListenerHelper.removeListener(helper, invalidationListenerMock[0]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
        invalidationListenerMock[1].check(list, 1);
        invalidationListenerMock[2].check(list, 1);
        invalidationListenerMock[3].check(list, 1);

        // remove middle element
        helper = ListListenerHelper.removeListener(helper, invalidationListenerMock[2]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
        invalidationListenerMock[1].check(list, 1);
        invalidationListenerMock[2].check(null, 0);
        invalidationListenerMock[3].check(list, 1);

        // remove last element
        helper = ListListenerHelper.removeListener(helper, invalidationListenerMock[3]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
        invalidationListenerMock[1].check(list, 1);
        invalidationListenerMock[2].check(null, 0);
        invalidationListenerMock[3].check(null, 0);

        // remove last invalidation with single change
        helper = ListListenerHelper.addListener(helper, changeListenerMock[0]);
        helper = ListListenerHelper.removeListener(helper, invalidationListenerMock[1]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[1].check(null, 0);
        changeListenerMock[0].check1();
        changeListenerMock[0].clear();

        // remove invalidation if array is empty
        helper = ListListenerHelper.addListener(helper, changeListenerMock[1]);
        helper = ListListenerHelper.removeListener(helper, invalidationListenerMock[0]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
        changeListenerMock[0].check1();
        changeListenerMock[1].check1();
        changeListenerMock[0].clear();
        changeListenerMock[1].clear();

        // remove last invalidation with two change
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[0]);
        helper = ListListenerHelper.removeListener(helper, invalidationListenerMock[0]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
        changeListenerMock[0].check1();
        changeListenerMock[1].check1();
    }


    @Test
    public void testGeneric_RemoveInvalidationInPulse() {
        final ListChangeListener<Object> removeListener = new ListChangeListener<Object>() {
            int counter;
            @Override
            public void onChanged(Change<? extends Object> c) {
                helper = ListListenerHelper.removeListener(helper, invalidationListenerMock[counter++]);
            }
        };
        helper = ListListenerHelper.addListener(helper, changeListenerMock[0]);
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[0]);
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[3]);
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[1]);
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[2]);

        helper = ListListenerHelper.addListener(helper, removeListener);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        helper = ListListenerHelper.removeListener(helper, removeListener);
        resetAllListeners();
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
        invalidationListenerMock[3].check(list, 1);
        invalidationListenerMock[1].check(list, 1);
        invalidationListenerMock[2].check(list, 1);

        helper = ListListenerHelper.addListener(helper, removeListener);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        helper = ListListenerHelper.removeListener(helper, removeListener);
        resetAllListeners();
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
        invalidationListenerMock[3].check(list, 1);
        invalidationListenerMock[1].check(null, 0);
        invalidationListenerMock[2].check(list, 1);

        helper = ListListenerHelper.addListener(helper, removeListener);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        helper = ListListenerHelper.removeListener(helper, removeListener);
        resetAllListeners();
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
        invalidationListenerMock[3].check(list, 1);
        invalidationListenerMock[1].check(null, 0);
        invalidationListenerMock[2].check(null, 0);

        helper = ListListenerHelper.addListener(helper, removeListener);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        helper = ListListenerHelper.removeListener(helper, removeListener);
        resetAllListeners();
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
        invalidationListenerMock[3].check(null, 0);
        invalidationListenerMock[1].check(null, 0);
        invalidationListenerMock[2].check(null, 0);
    }

    @Test
    public void testGeneric_AddChange() {
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[0]);
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[0]);

        // first change listener creates the array
        helper = ListListenerHelper.addListener(helper, changeListenerMock[0]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check1();
        changeListenerMock[0].clear();

        // second and third change listener enlarge the array
        helper = ListListenerHelper.addListener(helper, changeListenerMock[1]);
        helper = ListListenerHelper.addListener(helper, changeListenerMock[2]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check1();
        changeListenerMock[1].check1();
        changeListenerMock[2].check1();
        resetAllListeners();

        // fourth change listener fits into the array
        helper = ListListenerHelper.addListener(helper, changeListenerMock[3]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check1();
        changeListenerMock[1].check1();
        changeListenerMock[2].check1();
        changeListenerMock[3].check1();
    }

    @Test
    public void testGeneric_AddChangeInPulse() {
        final InvalidationListener addListener = new InvalidationListener() {
            int counter;
            @Override
            public void invalidated(Observable observable) {
                helper = ListListenerHelper.addListener(helper, changeListenerMock[counter++]);

            }
        };
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[0]);

        helper = ListListenerHelper.addListener(helper, addListener);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        helper = ListListenerHelper.removeListener(helper, addListener);
        resetAllListeners();
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check1();
        changeListenerMock[1].check0();
        changeListenerMock[2].check0();
        changeListenerMock[3].check0();

        helper = ListListenerHelper.addListener(helper, addListener);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        helper = ListListenerHelper.removeListener(helper, addListener);
        resetAllListeners();
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check1();
        changeListenerMock[1].check1();
        changeListenerMock[2].check0();
        changeListenerMock[3].check0();

        helper = ListListenerHelper.addListener(helper, addListener);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        helper = ListListenerHelper.removeListener(helper, addListener);
        resetAllListeners();
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check1();
        changeListenerMock[1].check1();
        changeListenerMock[2].check1();
        changeListenerMock[3].check0();

        helper = ListListenerHelper.addListener(helper, addListener);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        helper = ListListenerHelper.removeListener(helper, addListener);
        resetAllListeners();
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check1();
        changeListenerMock[1].check1();
        changeListenerMock[2].check1();
        changeListenerMock[3].check1();
    }

    @Test
    public void testGeneric_RemoveChange() {
        helper = ListListenerHelper.addListener(helper, changeListenerMock[0]);
        helper = ListListenerHelper.addListener(helper, changeListenerMock[1]);
        helper = ListListenerHelper.addListener(helper, changeListenerMock[2]);
        helper = ListListenerHelper.addListener(helper, changeListenerMock[3]);

        // remove first element
        helper = ListListenerHelper.removeListener(helper, changeListenerMock[0]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check0();
        changeListenerMock[1].check1();
        changeListenerMock[2].check1();
        changeListenerMock[3].check1();
        resetAllListeners();

        // remove middle element
        helper = ListListenerHelper.removeListener(helper, changeListenerMock[2]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check0();
        changeListenerMock[1].check1();
        changeListenerMock[2].check0();
        changeListenerMock[3].check1();
        resetAllListeners();

        // remove last element
        helper = ListListenerHelper.removeListener(helper, changeListenerMock[3]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check0();
        changeListenerMock[1].check1();
        changeListenerMock[2].check0();
        changeListenerMock[3].check0();
        resetAllListeners();

        // remove last change with single invalidation
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[0]);
        helper = ListListenerHelper.removeListener(helper, changeListenerMock[1]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(list, 1);
        changeListenerMock[1].check0();
        changeListenerMock[1].clear();

        // remove change if array is empty
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[1]);
        helper = ListListenerHelper.removeListener(helper, changeListenerMock[0]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(list, 1);
        invalidationListenerMock[1].check(list, 1);
        changeListenerMock[0].check0();
        changeListenerMock[0].clear();

        // remove last change with two invalidation
        helper = ListListenerHelper.addListener(helper, changeListenerMock[0]);
        helper = ListListenerHelper.removeListener(helper, changeListenerMock[0]);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(list, 1);
        invalidationListenerMock[1].check(list, 1);
        changeListenerMock[0].check0();
        changeListenerMock[0].clear();
    }


    @Test
    public void testGeneric_RemoveChangeInPulse() {
        final InvalidationListener removeListener = new InvalidationListener() {
            int counter;
            @Override
            public void invalidated(Observable observable) {
                helper = ListListenerHelper.removeListener(helper, changeListenerMock[counter++]);
            }
        };
        helper = ListListenerHelper.addListener(helper, invalidationListenerMock[0]);
        helper = ListListenerHelper.addListener(helper, changeListenerMock[0]);
        helper = ListListenerHelper.addListener(helper, changeListenerMock[3]);
        helper = ListListenerHelper.addListener(helper, changeListenerMock[1]);
        helper = ListListenerHelper.addListener(helper, changeListenerMock[2]);

        helper = ListListenerHelper.addListener(helper, removeListener);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        helper = ListListenerHelper.removeListener(helper, removeListener);
        resetAllListeners();
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check0();
        changeListenerMock[3].check1();
        changeListenerMock[1].check1();
        changeListenerMock[2].check1();

        helper = ListListenerHelper.addListener(helper, removeListener);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        helper = ListListenerHelper.removeListener(helper, removeListener);
        resetAllListeners();
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check0();
        changeListenerMock[3].check1();
        changeListenerMock[1].check0();
        changeListenerMock[2].check1();

        helper = ListListenerHelper.addListener(helper, removeListener);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        helper = ListListenerHelper.removeListener(helper, removeListener);
        resetAllListeners();
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check0();
        changeListenerMock[3].check1();
        changeListenerMock[1].check0();
        changeListenerMock[2].check0();

        helper = ListListenerHelper.addListener(helper, removeListener);
        ListListenerHelper.fireValueChangedEvent(helper, change);
        helper = ListListenerHelper.removeListener(helper, removeListener);
        resetAllListeners();
        ListListenerHelper.fireValueChangedEvent(helper, change);
        changeListenerMock[0].check0();
        changeListenerMock[3].check0();
        changeListenerMock[1].check0();
        changeListenerMock[2].check0();
    }


    @Test
    public void testExceptionNotPropagatedFromSingleInvalidation() {
        helper = ListListenerHelper.addListener(helper,(Observable o) -> {throw new RuntimeException();});
        helper.fireValueChangedEvent(change);
    }

    @Test
    public void testExceptionNotPropagatedFromMultipleInvalidation() {
        BitSet called = new BitSet();

        helper = ListListenerHelper.addListener(helper, (Observable o) -> {called.set(0); throw new RuntimeException();});
        helper = ListListenerHelper.addListener(helper, (Observable o) -> {called.set(1); throw new RuntimeException();});

        helper.fireValueChangedEvent(change);

        assertTrue(called.get(0));
        assertTrue(called.get(1));
    }

    @Test
    public void testExceptionNotPropagatedFromSingleChange() {
        helper = ListListenerHelper.addListener(helper, (ListChangeListener.Change<?> c) -> {
            throw new RuntimeException();
        });
        helper.fireValueChangedEvent(change);
    }

    @Test
    public void testExceptionNotPropagatedFromMultipleChange() {
        BitSet called = new BitSet();

        helper = ListListenerHelper.addListener(helper, (ListChangeListener.Change<?> c) -> {called.set(0); throw new RuntimeException();});
        helper = ListListenerHelper.addListener(helper, (ListChangeListener.Change<?> c) -> {called.set(1); throw new RuntimeException();});
        helper.fireValueChangedEvent(change);

        assertTrue(called.get(0));
        assertTrue(called.get(1));
    }

    @Test
    public void testExceptionNotPropagatedFromMultipleChangeAndInvalidation() {
        BitSet called = new BitSet();

        helper = ListListenerHelper.addListener(helper, (ListChangeListener.Change<?> c) -> {called.set(0); throw new RuntimeException();});
        helper = ListListenerHelper.addListener(helper, (ListChangeListener.Change<?> c) -> {called.set(1); throw new RuntimeException();});
        helper = ListListenerHelper.addListener(helper, (Observable o) -> {called.set(2); throw new RuntimeException();});
        helper = ListListenerHelper.addListener(helper, (Observable o) -> {called.set(3); throw new RuntimeException();});
        helper.fireValueChangedEvent(change);

        assertTrue(called.get(0));
        assertTrue(called.get(1));
        assertTrue(called.get(2));
        assertTrue(called.get(3));
    }

}
