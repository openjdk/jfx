/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.binding;

import com.sun.javafx.collections.NonIterableChange;
import javafx.beans.InvalidationListener;
import javafx.beans.InvalidationListenerMock;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListenerMock;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ChangeListenerMock;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueStub;
import javafx.beans.value.WeakChangeListenerMock;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MockListObserver;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListenerMock;
import org.junit.Before;
import org.junit.Test;

public class ListExpressionHelperTest {

    private static final ObservableList<Object> UNDEFINED = FXCollections.observableArrayList();


    private Object listItem;
    private ObservableList<Object> data1;
    private ObservableList<Object> data2;
//    private ListChangeListener.Change change_1_2;
//    private ListChangeListener.Change change_2_1;

    private ListExpressionHelper helper;
    private ListProperty<Object> observable;

    private InvalidationListenerMock[] invalidationListener;
    private ChangeListenerMock<? super ObservableList<Object>>[] changeListener;
    private MockListObserver<Object>[] listChangeListener;

    @Before
    public void setUp() {
        listItem = new Object();
        data1 = FXCollections.observableArrayList();
        data2 = FXCollections.observableArrayList(listItem);
//        change_1_2 = new NonIterableChange.SimpleAddChange(0, 1, data2);
//        change_2_1 = new NonIterableChange.SimpleRemovedChange(0, 1, listItem, data1);
        helper = null;
        observable = new SimpleListProperty<Object>(data1);

        invalidationListener = new InvalidationListenerMock[] {
                new InvalidationListenerMock(), new InvalidationListenerMock(), new InvalidationListenerMock(), new InvalidationListenerMock()
        };
        changeListener = new ChangeListenerMock[] {
                new ChangeListenerMock(UNDEFINED), new ChangeListenerMock(UNDEFINED), new ChangeListenerMock(UNDEFINED), new ChangeListenerMock(UNDEFINED)
        };
        listChangeListener = new MockListObserver[] {
                new MockListObserver<Object>(), new MockListObserver<Object>(), new MockListObserver<Object>(), new MockListObserver<Object>()
        };
    }
    
    @Test(expected = NullPointerException.class)
    public void testAddInvalidation_Null_X() {
        ListExpressionHelper.addListener(null, null, invalidationListener[0]);
    }

    @Test(expected = NullPointerException.class)
    public void testAddInvalidation_X_Null() {
        ListExpressionHelper.addListener(null, observable, (InvalidationListener)null);
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveInvalidation_Null() {
        ListExpressionHelper.removeListener(null, (InvalidationListener) null);
    }

    @Test(expected = NullPointerException.class)
    public void testAddChange_Null_X() {
        ListExpressionHelper.addListener(null, null, changeListener[0]);
    }

    @Test(expected = NullPointerException.class)
    public void testAddChange_X_Null() {
        ListExpressionHelper.addListener(null, observable, (ChangeListener) null);
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveChange_Null() {
        ListExpressionHelper.removeListener(null, (ChangeListener) null);
    }

    @Test(expected = NullPointerException.class)
    public void testAddListChange_Null_X() {
        ListExpressionHelper.addListener(null, null, listChangeListener[0]);
    }

    @Test(expected = NullPointerException.class)
    public void testAddListChange_X_Null() {
        ListExpressionHelper.addListener(null, observable, (ListChangeListener) null);
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveListChange_Null() {
        ListExpressionHelper.removeListener(null, (ListChangeListener) null);
    }

    @Test
    public void testEmpty() {
        final ListChangeListener.Change change = new NonIterableChange.SimpleRemovedChange(0, 1, new Object(), observable);
        ListExpressionHelper.removeListener(null, invalidationListener[0]);
        ListExpressionHelper.removeListener(null, changeListener[0]);
        ListExpressionHelper.removeListener(null, listChangeListener[0]);
        ListExpressionHelper.fireValueChangedEvent((ListExpressionHelper)null);
        ListExpressionHelper.fireValueChangedEvent(null, change);
    }

    @Test
    public void testSingleInvalidation() {
        helper = ListExpressionHelper.addListener(helper, observable, invalidationListener[0]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);

        data2.remove(0);
        ListExpressionHelper.fireValueChangedEvent(helper, new NonIterableChange.SimpleRemovedChange(0, 1, listItem, data2));
        invalidationListener[0].check(observable, 1);
        data2.add(listItem);

        helper = ListExpressionHelper.removeListener(helper, invalidationListener[1]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        invalidationListener[1].check(null, 0);

        helper = ListExpressionHelper.removeListener(helper, changeListener[1]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        changeListener[1].check(null, UNDEFINED, UNDEFINED, 0);

        helper = ListExpressionHelper.removeListener(helper, listChangeListener[1]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        listChangeListener[1].check0();

        helper = ListExpressionHelper.addListener(helper, observable, invalidationListener[1]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        invalidationListener[1].check(observable, 1);

        helper = ListExpressionHelper.removeListener(helper, invalidationListener[1]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        invalidationListener[1].check(null, 0);

        helper = ListExpressionHelper.addListener(helper, observable, changeListener[1]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        changeListener[1].check(observable, data1, data2, 1);

        helper = ListExpressionHelper.removeListener(helper, changeListener[1]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        changeListener[1].check(null, UNDEFINED, UNDEFINED, 0);

        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[1]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        listChangeListener[1].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[1].clear();

        helper = ListExpressionHelper.removeListener(helper, listChangeListener[1]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        listChangeListener[1].check0();

        helper = ListExpressionHelper.removeListener(helper, invalidationListener[0]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(null, 0);
    }

    @Test
    public void testSingleChange() {
        helper = ListExpressionHelper.addListener(helper, observable, changeListener[0]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(observable, data1, data2, 1);

        data2.remove(0);
        ListExpressionHelper.fireValueChangedEvent(helper, new NonIterableChange.SimpleRemovedChange(0, 1, listItem, data2));
        changeListener[0].check(observable, data2, data2, 1);
        data2.add(listItem);

        helper = ListExpressionHelper.removeListener(helper, invalidationListener[1]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(observable, data2, data1, 1);
        invalidationListener[1].check(null, 0);

        helper = ListExpressionHelper.removeListener(helper, changeListener[1]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(observable, data1, data2, 1);
        changeListener[1].check(null, UNDEFINED, UNDEFINED, 0);

        helper = ListExpressionHelper.removeListener(helper, listChangeListener[1]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(observable, data2, data1, 1);
        listChangeListener[1].check0();

        helper = ListExpressionHelper.addListener(helper, observable, invalidationListener[1]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(observable, data1, data2, 1);
        invalidationListener[1].check(observable, 1);

        helper = ListExpressionHelper.removeListener(helper, invalidationListener[1]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(observable, data2, data1, 1);
        invalidationListener[1].check(null, 0);

        helper = ListExpressionHelper.addListener(helper, observable, changeListener[1]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(observable, data1, data2, 1);
        changeListener[1].check(observable, data1, data2, 1);

        helper = ListExpressionHelper.removeListener(helper, changeListener[1]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(observable, data2, data1, 1);
        changeListener[1].check(null, UNDEFINED, UNDEFINED, 0);

        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[1]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(observable, data1, data2, 1);
        listChangeListener[1].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[1].clear();

        helper = ListExpressionHelper.removeListener(helper, listChangeListener[1]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(observable, data2, data1, 1);
        listChangeListener[1].check0();

        helper = ListExpressionHelper.removeListener(helper, invalidationListener[0]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(observable, data1, data2, 1);
    }

    @Test
    public void testSingleListChange() {
        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[0]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[0].clear();

        data2.remove(0);
        ListExpressionHelper.fireValueChangedEvent(helper, new NonIterableChange.SimpleRemovedChange(0, 0, listItem, data2));
        listChangeListener[0].check1AddRemove(observable, FXCollections.singletonObservableList(listItem), 0, 0);
        listChangeListener[0].clear();
        data2.add(listItem);

        helper = ListExpressionHelper.removeListener(helper, invalidationListener[1]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check1AddRemove(observable, FXCollections.singletonObservableList(listItem), 0, 0);
        listChangeListener[0].clear();
        invalidationListener[1].check(null, 0);

        helper = ListExpressionHelper.removeListener(helper, changeListener[1]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[0].clear();
        changeListener[1].check(null, UNDEFINED, UNDEFINED, 0);

        helper = ListExpressionHelper.removeListener(helper, listChangeListener[1]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check1AddRemove(observable, FXCollections.singletonObservableList(listItem), 0, 0);
        listChangeListener[0].clear();
        listChangeListener[1].check0();

        helper = ListExpressionHelper.addListener(helper, observable, invalidationListener[1]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[0].clear();
        invalidationListener[1].check(observable, 1);

        helper = ListExpressionHelper.removeListener(helper, invalidationListener[1]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check1AddRemove(observable, FXCollections.singletonObservableList(listItem), 0, 0);
        listChangeListener[0].clear();
        invalidationListener[1].check(null, 0);

        helper = ListExpressionHelper.addListener(helper, observable, changeListener[1]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[0].clear();
        changeListener[1].check(observable, data1, data2, 1);

        helper = ListExpressionHelper.removeListener(helper, changeListener[1]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check1AddRemove(observable, FXCollections.singletonObservableList(listItem), 0, 0);
        listChangeListener[0].clear();
        changeListener[1].check(null, UNDEFINED, UNDEFINED, 0);

        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[1]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[0].clear();
        listChangeListener[1].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[1].clear();

        helper = ListExpressionHelper.removeListener(helper, listChangeListener[1]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check1AddRemove(observable, FXCollections.singletonObservableList(listItem), 0, 0);
        listChangeListener[0].clear();
        listChangeListener[1].check0();

        helper = ListExpressionHelper.removeListener(helper, listChangeListener[0]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check0();
    }

    @Test
    public void testAddInvalidation() {
        final InvalidationListener weakListener = new WeakInvalidationListenerMock();

        helper = ListExpressionHelper.addListener(helper, observable, changeListener[0]);
        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[0]);

        helper = ListExpressionHelper.addListener(helper, observable, weakListener);
        helper = ListExpressionHelper.addListener(helper, observable, invalidationListener[0]);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);

        helper = ListExpressionHelper.addListener(helper, observable, weakListener);
        helper = ListExpressionHelper.addListener(helper, observable, invalidationListener[1]);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        invalidationListener[1].check(observable, 1);

        helper = ListExpressionHelper.addListener(helper, observable, weakListener);
        helper = ListExpressionHelper.addListener(helper, observable, invalidationListener[2]);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        invalidationListener[1].check(observable, 1);
        invalidationListener[2].check(observable, 1);
    }

    @Test
    public void testRemoveInvalidation() {
        helper = ListExpressionHelper.addListener(helper, observable, changeListener[0]);
        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[0]);

        helper = ListExpressionHelper.removeListener(helper, invalidationListener[1]);

        helper = ListExpressionHelper.addListener(helper, observable, invalidationListener[0]);

        helper = ListExpressionHelper.removeListener(helper, invalidationListener[1]);

        helper = ListExpressionHelper.addListener(helper, observable, invalidationListener[2]);
        helper = ListExpressionHelper.addListener(helper, observable, invalidationListener[1]);

        helper = ListExpressionHelper.removeListener(helper, invalidationListener[0]);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(null, 0);
        invalidationListener[1].check(observable, 1);
        invalidationListener[2].check(observable, 1);

        helper = ListExpressionHelper.removeListener(helper, invalidationListener[1]);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(null, 0);
        invalidationListener[1].check(null, 0);
        invalidationListener[2].check(observable, 1);

        helper = ListExpressionHelper.removeListener(helper, invalidationListener[2]);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(null, 0);
        invalidationListener[1].check(null, 0);
        invalidationListener[2].check(null, 0);
    }

    @Test
    public void testAddInvalidationWhileLocked() {
        final ChangeListener<Object> addingListener = new ChangeListener() {
            int index = 0;
            @Override public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (index < invalidationListener.length) {
                    helper = ListExpressionHelper.addListener(helper, ListExpressionHelperTest.this.observable, invalidationListener[index++]);
                }
            }
        };
        helper = ListExpressionHelper.addListener(helper, observable, addingListener);
        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[0]);

        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);

        invalidationListener[0].reset();
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);

        invalidationListener[1].reset();
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        invalidationListener[1].check(observable, 1);

        invalidationListener[2].reset();
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        invalidationListener[1].check(observable, 1);
        invalidationListener[2].check(observable, 1);

        invalidationListener[3].reset();
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        invalidationListener[1].check(observable, 1);
        invalidationListener[2].check(observable, 1);
        invalidationListener[3].check(observable, 1);
    }

    @Test
    public void testRemoveInvalidationWhileLocked() {
        final ChangeListener<Object> removingListener = new ChangeListener() {
            int index = 0;
            @Override public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if (index < invalidationListener.length) {
                    helper = ListExpressionHelper.removeListener(helper, invalidationListener[index++]);
                }
            }
        };
        helper = ListExpressionHelper.addListener(helper, observable, removingListener);
        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[0]);
        helper = ListExpressionHelper.addListener(helper, observable, invalidationListener[0]);
        helper = ListExpressionHelper.addListener(helper, observable, invalidationListener[2]);
        helper = ListExpressionHelper.addListener(helper, observable, invalidationListener[1]);

        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].reset();
        invalidationListener[1].check(observable, 1);
        invalidationListener[2].check(observable, 1);

        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(null, 0);
        invalidationListener[1].reset();
        invalidationListener[2].check(observable, 1);

        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(null, 0);
        invalidationListener[1].check(null, 0);
        invalidationListener[2].reset();

        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(null, 0);
        invalidationListener[1].check(null, 0);
        invalidationListener[2].check(null, 0);
    }

    @Test
    public void testAddChange() {
        final ChangeListener<Object> weakListener = new WeakChangeListenerMock();

        helper = ListExpressionHelper.addListener(helper, observable, invalidationListener[0]);
        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[0]);

        helper = ListExpressionHelper.addListener(helper, observable, weakListener);
        helper = ListExpressionHelper.addListener(helper, observable, changeListener[0]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(observable, data1, data2, 1);

        helper = ListExpressionHelper.addListener(helper, observable, weakListener);
        helper = ListExpressionHelper.addListener(helper, observable, changeListener[1]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(observable, data2, data1, 1);
        changeListener[1].check(observable, data2, data1, 1);

        helper = ListExpressionHelper.addListener(helper, observable, weakListener);
        helper = ListExpressionHelper.addListener(helper, observable, changeListener[2]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(observable, data1, data2, 1);
        changeListener[1].check(observable, data1, data2, 1);
        changeListener[2].check(observable, data1, data2, 1);
    }

    @Test
    public void testRemoveChange() {
        helper = ListExpressionHelper.addListener(helper, observable, invalidationListener[0]);
        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[0]);

        helper = ListExpressionHelper.removeListener(helper, changeListener[1]);

        helper = ListExpressionHelper.addListener(helper, observable, changeListener[0]);

        helper = ListExpressionHelper.removeListener(helper, changeListener[1]);

        helper = ListExpressionHelper.addListener(helper, observable, changeListener[2]);
        helper = ListExpressionHelper.addListener(helper, observable, changeListener[1]);

        helper = ListExpressionHelper.removeListener(helper, changeListener[0]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(null, UNDEFINED, UNDEFINED, 0);
        changeListener[1].check(observable, data1, data2, 1);
        changeListener[2].check(observable, data1, data2, 1);

        helper = ListExpressionHelper.removeListener(helper, changeListener[1]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(null, UNDEFINED, UNDEFINED, 0);
        changeListener[1].check(null, UNDEFINED, UNDEFINED, 0);
        changeListener[2].check(observable, data2, data1, 1);

        helper = ListExpressionHelper.removeListener(helper, changeListener[2]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(null, UNDEFINED, UNDEFINED, 0);
        changeListener[1].check(null, UNDEFINED, UNDEFINED, 0);
        changeListener[2].check(null, UNDEFINED, UNDEFINED, 0);
    }

    @Test
    public void testAddChangeWhileLocked() {
        final InvalidationListener addingListener = new InvalidationListener() {
            int index = 0;
            @Override public void invalidated(Observable observable) {
                if (index < invalidationListener.length) {
                    helper = ListExpressionHelper.addListener(helper, ListExpressionHelperTest.this.observable, changeListener[index++]);
                }
            }
        };
        helper = ListExpressionHelper.addListener(helper, observable, addingListener);
        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[0]);

        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);

        changeListener[0].reset();
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(observable, data2, data1, 1);

        changeListener[1].reset();
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(observable, data1, data2, 1);
        changeListener[1].check(observable, data1, data2, 1);

        changeListener[2].reset();
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(observable, data2, data1, 1);
        changeListener[1].check(observable, data2, data1, 1);
        changeListener[2].check(observable, data2, data1, 1);

        changeListener[3].reset();
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(observable, data1, data2, 1);
        changeListener[1].check(observable, data1, data2, 1);
        changeListener[2].check(observable, data1, data2, 1);
        changeListener[3].check(observable, data1, data2, 1);
    }

    @Test
    public void testRemoveChangeWhileLocked() {
        final InvalidationListener removingListener = new InvalidationListener() {
            int index = 0;
            @Override public void invalidated(Observable observable) {
                if (index < invalidationListener.length) {
                    helper = ListExpressionHelper.removeListener(helper, changeListener[index++]);
                }
            }
        };
        helper = ListExpressionHelper.addListener(helper, observable, removingListener);
        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[0]);
        helper = ListExpressionHelper.addListener(helper, observable, changeListener[0]);
        helper = ListExpressionHelper.addListener(helper, observable, changeListener[2]);
        helper = ListExpressionHelper.addListener(helper, observable, changeListener[1]);

        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].reset();
        changeListener[1].check(observable, data1, data2, 1);
        changeListener[2].check(observable, data1, data2, 1);

        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(null, UNDEFINED, UNDEFINED, 0);
        changeListener[1].reset();
        changeListener[2].check(observable, data2, data1, 1);

        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(null, UNDEFINED, UNDEFINED, 0);
        changeListener[1].check(null, UNDEFINED, UNDEFINED, 0);
        changeListener[2].reset();

        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(null, UNDEFINED, UNDEFINED, 0);
        changeListener[1].check(null, UNDEFINED, UNDEFINED, 0);
        changeListener[2].check(null, UNDEFINED, UNDEFINED, 0);
    }

    @Test
    public void testAddListChange() {
        final ChangeListener<Object> weakListener = new WeakListChangeListenerMock();

        helper = ListExpressionHelper.addListener(helper, observable, invalidationListener[0]);
        helper = ListExpressionHelper.addListener(helper, observable, changeListener[0]);

        helper = ListExpressionHelper.addListener(helper, observable, weakListener);
        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[0]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[0].clear();

        helper = ListExpressionHelper.addListener(helper, observable, weakListener);
        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[1]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check1AddRemove(observable, FXCollections.singletonObservableList(listItem), 0, 0);
        listChangeListener[0].clear();
        listChangeListener[1].check1AddRemove(observable, FXCollections.singletonObservableList(listItem), 0, 0);
        listChangeListener[1].clear();

        helper = ListExpressionHelper.addListener(helper, observable, weakListener);
        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[2]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[0].clear();
        listChangeListener[1].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[1].clear();
        listChangeListener[2].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[2].clear();

        helper = ListExpressionHelper.addListener(helper, observable, weakListener);
        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[3]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check1AddRemove(observable, FXCollections.singletonObservableList(listItem), 0, 0);
        listChangeListener[1].check1AddRemove(observable, FXCollections.singletonObservableList(listItem), 0, 0);
        listChangeListener[2].check1AddRemove(observable, FXCollections.singletonObservableList(listItem), 0, 0);
        listChangeListener[3].check1AddRemove(observable, FXCollections.singletonObservableList(listItem), 0, 0);
    }

    @Test
    public void testRemoveListChange() {
        helper = ListExpressionHelper.addListener(helper, observable, invalidationListener[0]);
        helper = ListExpressionHelper.addListener(helper, observable, changeListener[0]);

        helper = ListExpressionHelper.removeListener(helper, listChangeListener[1]);

        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[0]);

        helper = ListExpressionHelper.removeListener(helper, listChangeListener[1]);

        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[2]);
        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[1]);

        helper = ListExpressionHelper.removeListener(helper, listChangeListener[0]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check0();
        listChangeListener[1].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[1].clear();
        listChangeListener[2].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[2].clear();

        helper = ListExpressionHelper.removeListener(helper, listChangeListener[1]);
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check0();
        listChangeListener[1].check0();
        listChangeListener[2].check1AddRemove(observable, FXCollections.singletonObservableList(listItem), 0, 0);
        listChangeListener[2].clear();

        helper = ListExpressionHelper.removeListener(helper, listChangeListener[2]);
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check0();
        listChangeListener[1].check0();
        listChangeListener[2].check0();
    }

    @Test
    public void testAddListChangeWhileLocked() {
        final InvalidationListener addingListener = new InvalidationListener() {
            int index = 0;
            @Override public void invalidated(Observable observable) {
                if (index < invalidationListener.length) {
                    helper = ListExpressionHelper.addListener(helper, ListExpressionHelperTest.this.observable, listChangeListener[index++]);
                }
            }
        };
        helper = ListExpressionHelper.addListener(helper, observable, addingListener);
        helper = ListExpressionHelper.addListener(helper, observable, changeListener[0]);

        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);

        listChangeListener[0].clear();
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check1AddRemove(observable, FXCollections.singletonObservableList(listItem), 0, 0);
        listChangeListener[0].clear();

        listChangeListener[1].clear();
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[0].clear();
        listChangeListener[1].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[1].clear();

        listChangeListener[2].clear();
        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check1AddRemove(observable, FXCollections.singletonObservableList(listItem), 0, 0);
        listChangeListener[0].clear();
        listChangeListener[1].check1AddRemove(observable, FXCollections.singletonObservableList(listItem), 0, 0);
        listChangeListener[1].clear();
        listChangeListener[2].check1AddRemove(observable, FXCollections.singletonObservableList(listItem), 0, 0);
        listChangeListener[2].clear();

        listChangeListener[3].clear();
        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[1].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[2].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[3].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
    }

    @Test
    public void testRemoveListChangeWhileLocked() {
        final InvalidationListener removingListener = new InvalidationListener() {
            int index = 0;
            @Override public void invalidated(Observable observable) {
                if (index < invalidationListener.length) {
                    helper = ListExpressionHelper.removeListener(helper, listChangeListener[index++]);
                }
            }
        };
        helper = ListExpressionHelper.addListener(helper, observable, removingListener);
        helper = ListExpressionHelper.addListener(helper, observable, changeListener[0]);
        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[0]);
        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[2]);
        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[1]);

        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].clear();
        listChangeListener[1].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[2].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 1);
        listChangeListener[2].clear();

        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check0();
        listChangeListener[1].clear();
        listChangeListener[2].check1AddRemove(observable, FXCollections.singletonObservableList(listItem), 0, 0);

        observable.set(data2);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check0();
        listChangeListener[1].check0();
        listChangeListener[2].clear();

        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        listChangeListener[0].check0();
        listChangeListener[1].check0();
        listChangeListener[2].check0();
    }


    @Test
    public void testFireValueChangedEvent() {
        helper = ListExpressionHelper.addListener(helper, observable, invalidationListener[0]);
        helper = ListExpressionHelper.addListener(helper, observable, changeListener[0]);
        helper = ListExpressionHelper.addListener(helper, observable, listChangeListener[0]);

        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        changeListener[0].check(null, UNDEFINED, UNDEFINED, 0);
        listChangeListener[0].check0();

        observable.set(null);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        changeListener[0].check(observable, data1, null, 1);
        listChangeListener[0].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 0);
        listChangeListener[0].clear();

        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        changeListener[0].check(null, UNDEFINED, UNDEFINED, 0);
        listChangeListener[0].check0();

        observable.set(data1);
        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        changeListener[0].check(observable, null, data1, 1);
        listChangeListener[0].check1AddRemove(observable, FXCollections.emptyObservableList(), 0, 0);
        listChangeListener[0].clear();

        ListExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        changeListener[0].check(null, UNDEFINED, UNDEFINED, 0);
        listChangeListener[0].check0();
    }
}
