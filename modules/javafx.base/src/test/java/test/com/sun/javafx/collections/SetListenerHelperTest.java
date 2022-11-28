/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.binding.SetExpressionHelper;
import com.sun.javafx.collections.SetListenerHelper;
import javafx.beans.InvalidationListener;
import test.javafx.beans.InvalidationListenerMock;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import test.javafx.collections.MockSetObserver;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import org.junit.Before;
import org.junit.Test;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SetListenerHelperTest {

    private InvalidationListenerMock[] invalidationListenerMock;

    private MockSetObserver<Object>[] changeListenerMock;

    private SetListenerHelper<Object> helper;

    private ObservableSet<Object> set;
    private SetChangeListener.Change<Object> change;

    @Before
    public void setUp() {
        invalidationListenerMock = new InvalidationListenerMock[] {
                new InvalidationListenerMock(),
                new InvalidationListenerMock(),
                new InvalidationListenerMock(),
                new InvalidationListenerMock()
        };
        changeListenerMock = new MockSetObserver[] {
                new MockSetObserver<>(),
                new MockSetObserver<>(),
                new MockSetObserver<>(),
                new MockSetObserver<>()
        };
        helper = null;
        set = FXCollections.observableSet();
        change = new SetExpressionHelper.SimpleChange<>(set).setRemoved(new Object());
    }

    private void resetAllListeners() {
        for (final InvalidationListenerMock listener : invalidationListenerMock) {
            listener.reset();
        }
        for (final MockSetObserver<Object> listener : changeListenerMock) {
            listener.clear();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testAddInvalidationListener_Null() {
        SetListenerHelper.addListener(helper, (InvalidationListener)null);
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveInvalidationListener_Null() {
        SetListenerHelper.removeListener(helper, (InvalidationListener) null);
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveSetChangeListener_Null() {
        SetListenerHelper.removeListener(helper, (SetChangeListener<Object>) null);
    }

    @Test(expected = NullPointerException.class)
    public void testAddSetChangeListener_Null() {
        SetListenerHelper.addListener(helper, (SetChangeListener<Object>) null);
    }

    @Test
    public void testEmpty() {
        assertFalse(SetListenerHelper.hasListeners(helper));

        // these should be no-ops
        SetListenerHelper.fireValueChangedEvent(helper, change);
        SetListenerHelper.removeListener(helper, invalidationListenerMock[0]);
        SetListenerHelper.removeListener(helper, changeListenerMock[0]);
    }

    @Test
    public void testInvalidation_Simple() {
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[0]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(set, 1);

        helper = SetListenerHelper.removeListener(helper, invalidationListenerMock[1]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(set, 1);
        invalidationListenerMock[1].check(null, 0);

        helper = SetListenerHelper.removeListener(helper, changeListenerMock[0]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(set, 1);
        assertEquals(0, changeListenerMock[0].getCallsNumber());

        helper = SetListenerHelper.removeListener(helper, invalidationListenerMock[0]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
    }

    @Test
    public void testInvalidation_AddInvalidation() {
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[0]);
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[1]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(set, 1);
        invalidationListenerMock[1].check(set, 1);
    }

    @Test
    public void testInvalidation_AddChange() {
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[0]);
        helper = SetListenerHelper.addListener(helper, changeListenerMock[0]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(set, 1);
        assertEquals(1, changeListenerMock[0].getCallsNumber());
    }

    @Test
    public void testInvalidation_ChangeInPulse() {
        final InvalidationListener listener = observable -> {
            helper = SetListenerHelper.addListener(helper, invalidationListenerMock[0]);
        };
        helper = SetListenerHelper.addListener(helper, listener);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        helper = SetListenerHelper.removeListener(helper, listener);
        invalidationListenerMock[0].reset();
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(set, 1);
    }

    @Test
    public void testChange_Simple() {
        helper = SetListenerHelper.addListener(helper, changeListenerMock[0]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(1, changeListenerMock[0].getCallsNumber());
        changeListenerMock[0].clear();

        helper = SetListenerHelper.removeListener(helper, changeListenerMock[1]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(1, changeListenerMock[0].getCallsNumber());
        assertEquals(0, changeListenerMock[1].getCallsNumber());
        changeListenerMock[0].clear();

        helper = SetListenerHelper.removeListener(helper, invalidationListenerMock[0]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(1, changeListenerMock[0].getCallsNumber());
        invalidationListenerMock[0].check(null, 0);
        changeListenerMock[0].clear();

        helper = SetListenerHelper.removeListener(helper, changeListenerMock[0]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(0, changeListenerMock[0].getCallsNumber());
        changeListenerMock[0].clear();
    }

    @Test
    public void testChange_AddInvalidation() {
        helper = SetListenerHelper.addListener(helper, changeListenerMock[0]);
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[0]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(1, changeListenerMock[0].getCallsNumber());
        invalidationListenerMock[0].check(set, 1);
    }

    @Test
    public void testChange_AddChange() {
        helper = SetListenerHelper.addListener(helper, changeListenerMock[0]);
        helper = SetListenerHelper.addListener(helper, changeListenerMock[1]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(1, changeListenerMock[0].getCallsNumber());
        assertEquals(1, changeListenerMock[1].getCallsNumber());
    }

    @Test
    public void testChange_ChangeInPulse() {
        final SetChangeListener<Object> listener = change1 -> {
            helper = SetListenerHelper.addListener(helper, changeListenerMock[0]);
        };
        helper = SetListenerHelper.addListener(helper, listener);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        helper = SetListenerHelper.removeListener(helper, listener);
        changeListenerMock[0].clear();
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(1, changeListenerMock[0].getCallsNumber());
    }

    @Test
    public void testGeneric_AddInvalidation() {
        helper = SetListenerHelper.addListener(helper, changeListenerMock[0]);
        helper = SetListenerHelper.addListener(helper, changeListenerMock[1]);

        // first invalidation listener creates the array
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[0]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(set, 1);

        // second and third invalidation listener enlarge the array
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[1]);
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[2]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(set, 1);
        invalidationListenerMock[1].check(set, 1);
        invalidationListenerMock[2].check(set, 1);

        // fourth invalidation listener fits into the array
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[3]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(set, 1);
        invalidationListenerMock[1].check(set, 1);
        invalidationListenerMock[2].check(set, 1);
        invalidationListenerMock[3].check(set, 1);
    }

    @Test
    public void testGeneric_AddInvalidationInPulse() {
        final SetChangeListener<Object> addListener = new SetChangeListener<>() {
            int counter;
            @Override
            public void onChanged(Change<? extends Object> change) {
                helper = SetListenerHelper.addListener(helper, invalidationListenerMock[counter++]);
            }
        };
        helper = SetListenerHelper.addListener(helper, changeListenerMock[0]);

        helper = SetListenerHelper.addListener(helper, addListener);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        helper = SetListenerHelper.removeListener(helper, addListener);
        resetAllListeners();
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(set, 1);
        invalidationListenerMock[1].check(null, 0);
        invalidationListenerMock[2].check(null, 0);
        invalidationListenerMock[3].check(null, 0);

        helper = SetListenerHelper.addListener(helper, addListener);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        helper = SetListenerHelper.removeListener(helper, addListener);
        resetAllListeners();
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(set, 1);
        invalidationListenerMock[1].check(set, 1);
        invalidationListenerMock[2].check(null, 0);
        invalidationListenerMock[3].check(null, 0);

        helper = SetListenerHelper.addListener(helper, addListener);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        helper = SetListenerHelper.removeListener(helper, addListener);
        resetAllListeners();
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(set, 1);
        invalidationListenerMock[1].check(set, 1);
        invalidationListenerMock[2].check(set, 1);
        invalidationListenerMock[3].check(null, 0);

        helper = SetListenerHelper.addListener(helper, addListener);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        helper = SetListenerHelper.removeListener(helper, addListener);
        resetAllListeners();
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(set, 1);
        invalidationListenerMock[1].check(set, 1);
        invalidationListenerMock[2].check(set, 1);
        invalidationListenerMock[3].check(set, 1);
    }

    @Test
    public void testGeneric_RemoveInvalidation() {
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[0]);
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[1]);
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[2]);
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[3]);

        // remove first element
        helper = SetListenerHelper.removeListener(helper, invalidationListenerMock[0]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
        invalidationListenerMock[1].check(set, 1);
        invalidationListenerMock[2].check(set, 1);
        invalidationListenerMock[3].check(set, 1);

        // remove middle element
        helper = SetListenerHelper.removeListener(helper, invalidationListenerMock[2]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
        invalidationListenerMock[1].check(set, 1);
        invalidationListenerMock[2].check(null, 0);
        invalidationListenerMock[3].check(set, 1);

        // remove last element
        helper = SetListenerHelper.removeListener(helper, invalidationListenerMock[3]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
        invalidationListenerMock[1].check(set, 1);
        invalidationListenerMock[2].check(null, 0);
        invalidationListenerMock[3].check(null, 0);

        // remove last invalidation with single change
        helper = SetListenerHelper.addListener(helper, changeListenerMock[0]);
        helper = SetListenerHelper.removeListener(helper, invalidationListenerMock[1]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[1].check(null, 0);
        assertEquals(1, changeListenerMock[0].getCallsNumber());
        changeListenerMock[0].clear();

        // remove invalidation if array is empty
        helper = SetListenerHelper.addListener(helper, changeListenerMock[1]);
        helper = SetListenerHelper.removeListener(helper, invalidationListenerMock[0]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
        assertEquals(1, changeListenerMock[0].getCallsNumber());
        assertEquals(1, changeListenerMock[1].getCallsNumber());
        changeListenerMock[0].clear();
        changeListenerMock[1].clear();

        // remove last invalidation with two change
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[0]);
        helper = SetListenerHelper.removeListener(helper, invalidationListenerMock[0]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
        assertEquals(1, changeListenerMock[0].getCallsNumber());
        assertEquals(1, changeListenerMock[1].getCallsNumber());
    }


    @Test
    public void testGeneric_RemoveInvalidationInPulse() {
        final SetChangeListener<Object> removeListener = new SetChangeListener<>() {
            int counter;
            @Override
            public void onChanged(Change<? extends Object> change) {
                helper = SetListenerHelper.removeListener(helper, invalidationListenerMock[counter++]);
            }
        };
        helper = SetListenerHelper.addListener(helper, changeListenerMock[0]);
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[0]);
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[3]);
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[1]);
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[2]);

        helper = SetListenerHelper.addListener(helper, removeListener);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        helper = SetListenerHelper.removeListener(helper, removeListener);
        resetAllListeners();
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
        invalidationListenerMock[3].check(set, 1);
        invalidationListenerMock[1].check(set, 1);
        invalidationListenerMock[2].check(set, 1);

        helper = SetListenerHelper.addListener(helper, removeListener);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        helper = SetListenerHelper.removeListener(helper, removeListener);
        resetAllListeners();
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
        invalidationListenerMock[3].check(set, 1);
        invalidationListenerMock[1].check(null, 0);
        invalidationListenerMock[2].check(set, 1);

        helper = SetListenerHelper.addListener(helper, removeListener);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        helper = SetListenerHelper.removeListener(helper, removeListener);
        resetAllListeners();
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
        invalidationListenerMock[3].check(set, 1);
        invalidationListenerMock[1].check(null, 0);
        invalidationListenerMock[2].check(null, 0);

        helper = SetListenerHelper.addListener(helper, removeListener);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        helper = SetListenerHelper.removeListener(helper, removeListener);
        resetAllListeners();
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(null, 0);
        invalidationListenerMock[3].check(null, 0);
        invalidationListenerMock[1].check(null, 0);
        invalidationListenerMock[2].check(null, 0);
    }

    @Test
    public void testGeneric_AddChange() {
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[0]);
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[0]);

        // first change listener creates the array
        helper = SetListenerHelper.addListener(helper, changeListenerMock[0]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(1, changeListenerMock[0].getCallsNumber());
        changeListenerMock[0].clear();

        // second and third change listener enlarge the array
        helper = SetListenerHelper.addListener(helper, changeListenerMock[1]);
        helper = SetListenerHelper.addListener(helper, changeListenerMock[2]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(1, changeListenerMock[0].getCallsNumber());
        assertEquals(1, changeListenerMock[1].getCallsNumber());
        assertEquals(1, changeListenerMock[2].getCallsNumber());
        resetAllListeners();

        // fourth change listener fits into the array
        helper = SetListenerHelper.addListener(helper, changeListenerMock[3]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(1, changeListenerMock[0].getCallsNumber());
        assertEquals(1, changeListenerMock[1].getCallsNumber());
        assertEquals(1, changeListenerMock[2].getCallsNumber());
        assertEquals(1, changeListenerMock[3].getCallsNumber());
    }

    @Test
    public void testGeneric_AddChangeInPulse() {
        final InvalidationListener addListener = new InvalidationListener() {
            int counter;
            @Override
            public void invalidated(Observable observable) {
                helper = SetListenerHelper.addListener(helper, changeListenerMock[counter++]);

            }
        };
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[0]);

        helper = SetListenerHelper.addListener(helper, addListener);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        helper = SetListenerHelper.removeListener(helper, addListener);
        resetAllListeners();
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(1, changeListenerMock[0].getCallsNumber());
        assertEquals(0, changeListenerMock[1].getCallsNumber());
        assertEquals(0, changeListenerMock[2].getCallsNumber());
        assertEquals(0, changeListenerMock[3].getCallsNumber());

        helper = SetListenerHelper.addListener(helper, addListener);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        helper = SetListenerHelper.removeListener(helper, addListener);
        resetAllListeners();
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(1, changeListenerMock[0].getCallsNumber());
        assertEquals(1, changeListenerMock[1].getCallsNumber());
        assertEquals(0, changeListenerMock[2].getCallsNumber());
        assertEquals(0, changeListenerMock[3].getCallsNumber());

        helper = SetListenerHelper.addListener(helper, addListener);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        helper = SetListenerHelper.removeListener(helper, addListener);
        resetAllListeners();
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(1, changeListenerMock[0].getCallsNumber());
        assertEquals(1, changeListenerMock[1].getCallsNumber());
        assertEquals(1, changeListenerMock[2].getCallsNumber());
        assertEquals(0, changeListenerMock[3].getCallsNumber());

        helper = SetListenerHelper.addListener(helper, addListener);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        helper = SetListenerHelper.removeListener(helper, addListener);
        resetAllListeners();
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(1, changeListenerMock[0].getCallsNumber());
        assertEquals(1, changeListenerMock[1].getCallsNumber());
        assertEquals(1, changeListenerMock[2].getCallsNumber());
        assertEquals(1, changeListenerMock[3].getCallsNumber());
    }

    @Test
    public void testGeneric_RemoveChange() {
        helper = SetListenerHelper.addListener(helper, changeListenerMock[0]);
        helper = SetListenerHelper.addListener(helper, changeListenerMock[1]);
        helper = SetListenerHelper.addListener(helper, changeListenerMock[2]);
        helper = SetListenerHelper.addListener(helper, changeListenerMock[3]);

        // remove first element
        helper = SetListenerHelper.removeListener(helper, changeListenerMock[0]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(0, changeListenerMock[0].getCallsNumber());
        assertEquals(1, changeListenerMock[1].getCallsNumber());
        assertEquals(1, changeListenerMock[2].getCallsNumber());
        assertEquals(1, changeListenerMock[3].getCallsNumber());
        resetAllListeners();

        // remove middle element
        helper = SetListenerHelper.removeListener(helper, changeListenerMock[2]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(0, changeListenerMock[0].getCallsNumber());
        assertEquals(1, changeListenerMock[1].getCallsNumber());
        assertEquals(0, changeListenerMock[2].getCallsNumber());
        assertEquals(1, changeListenerMock[3].getCallsNumber());
        resetAllListeners();

        // remove last element
        helper = SetListenerHelper.removeListener(helper, changeListenerMock[3]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(0, changeListenerMock[0].getCallsNumber());
        assertEquals(1, changeListenerMock[1].getCallsNumber());
        assertEquals(0, changeListenerMock[2].getCallsNumber());
        assertEquals(0, changeListenerMock[3].getCallsNumber());
        resetAllListeners();

        // remove last change with single invalidation
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[0]);
        helper = SetListenerHelper.removeListener(helper, changeListenerMock[1]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(set, 1);
        assertEquals(0, changeListenerMock[1].getCallsNumber());
        changeListenerMock[1].clear();

        // remove change if array is empty
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[1]);
        helper = SetListenerHelper.removeListener(helper, changeListenerMock[0]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(set, 1);
        invalidationListenerMock[1].check(set, 1);
        assertEquals(0, changeListenerMock[0].getCallsNumber());
        changeListenerMock[0].clear();

        // remove last change with two invalidation
        helper = SetListenerHelper.addListener(helper, changeListenerMock[0]);
        helper = SetListenerHelper.removeListener(helper, changeListenerMock[0]);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        invalidationListenerMock[0].check(set, 1);
        invalidationListenerMock[1].check(set, 1);
        assertEquals(0, changeListenerMock[0].getCallsNumber());
        changeListenerMock[0].clear();
    }


    @Test
    public void testGeneric_RemoveChangeInPulse() {
        final InvalidationListener removeListener = new InvalidationListener() {
            int counter;
            @Override
            public void invalidated(Observable observable) {
                helper = SetListenerHelper.removeListener(helper, changeListenerMock[counter++]);
            }
        };
        helper = SetListenerHelper.addListener(helper, invalidationListenerMock[0]);
        helper = SetListenerHelper.addListener(helper, changeListenerMock[0]);
        helper = SetListenerHelper.addListener(helper, changeListenerMock[3]);
        helper = SetListenerHelper.addListener(helper, changeListenerMock[1]);
        helper = SetListenerHelper.addListener(helper, changeListenerMock[2]);

        helper = SetListenerHelper.addListener(helper, removeListener);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        helper = SetListenerHelper.removeListener(helper, removeListener);
        resetAllListeners();
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(0, changeListenerMock[0].getCallsNumber());
        assertEquals(1, changeListenerMock[3].getCallsNumber());
        assertEquals(1, changeListenerMock[1].getCallsNumber());
        assertEquals(1, changeListenerMock[2].getCallsNumber());

        helper = SetListenerHelper.addListener(helper, removeListener);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        helper = SetListenerHelper.removeListener(helper, removeListener);
        resetAllListeners();
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(0, changeListenerMock[0].getCallsNumber());
        assertEquals(1, changeListenerMock[3].getCallsNumber());
        assertEquals(0, changeListenerMock[1].getCallsNumber());
        assertEquals(1, changeListenerMock[2].getCallsNumber());

        helper = SetListenerHelper.addListener(helper, removeListener);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        helper = SetListenerHelper.removeListener(helper, removeListener);
        resetAllListeners();
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(0, changeListenerMock[0].getCallsNumber());
        assertEquals(1, changeListenerMock[3].getCallsNumber());
        assertEquals(0, changeListenerMock[1].getCallsNumber());
        assertEquals(0, changeListenerMock[2].getCallsNumber());

        helper = SetListenerHelper.addListener(helper, removeListener);
        SetListenerHelper.fireValueChangedEvent(helper, change);
        helper = SetListenerHelper.removeListener(helper, removeListener);
        resetAllListeners();
        SetListenerHelper.fireValueChangedEvent(helper, change);
        assertEquals(0, changeListenerMock[0].getCallsNumber());
        assertEquals(0, changeListenerMock[3].getCallsNumber());
        assertEquals(0, changeListenerMock[1].getCallsNumber());
        assertEquals(0, changeListenerMock[2].getCallsNumber());
    }



    @Test
    public void testExceptionNotPropagatedFromSingleInvalidation() {
        helper = SetListenerHelper.addListener(helper,(Observable o) -> {throw new RuntimeException();});
        SetListenerHelper.fireValueChangedEvent(helper,change);
    }

    @Test
    public void testExceptionNotPropagatedFromMultipleInvalidation() {
        BitSet called = new BitSet();

        helper = SetListenerHelper.addListener(helper, (Observable o) -> {called.set(0); throw new RuntimeException();});
        helper = SetListenerHelper.addListener(helper, (Observable o) -> {called.set(1); throw new RuntimeException();});

        SetListenerHelper.fireValueChangedEvent(helper,change);

        assertTrue(called.get(0));
        assertTrue(called.get(1));
    }

    @Test
    public void testExceptionNotPropagatedFromSingleChange() {
        helper = SetListenerHelper.addListener(helper, (SetChangeListener.Change<? extends Object> c) -> {
            throw new RuntimeException();
        });
        SetListenerHelper.fireValueChangedEvent(helper,change);
    }

    @Test
    public void testExceptionNotPropagatedFromMultipleChange() {
        BitSet called = new BitSet();

        helper = SetListenerHelper.addListener(helper, (SetChangeListener.Change<? extends Object> c) -> {called.set(0); throw new RuntimeException();});
        helper = SetListenerHelper.addListener(helper, (SetChangeListener.Change<? extends Object> c) -> {called.set(1); throw new RuntimeException();});
        SetListenerHelper.fireValueChangedEvent(helper,change);

        assertTrue(called.get(0));
        assertTrue(called.get(1));
    }

    @Test
    public void testExceptionNotPropagatedFromMultipleChangeAndInvalidation() {
        BitSet called = new BitSet();

        helper = SetListenerHelper.addListener(helper, (SetChangeListener.Change<? extends Object> c) -> {called.set(0); throw new RuntimeException();});
        helper = SetListenerHelper.addListener(helper, (SetChangeListener.Change<? extends Object> c) -> {called.set(1); throw new RuntimeException();});
        helper = SetListenerHelper.addListener(helper, (Observable o) -> {called.set(2); throw new RuntimeException();});
        helper = SetListenerHelper.addListener(helper, (Observable o) -> {called.set(3); throw new RuntimeException();});
        SetListenerHelper.fireValueChangedEvent(helper,change);

        assertTrue(called.get(0));
        assertTrue(called.get(1));
        assertTrue(called.get(2));
        assertTrue(called.get(3));
    }


    @Test
    public void testExceptionHandledByThreadUncaughtHandlerInSingleInvalidation() {
        AtomicBoolean called = new AtomicBoolean(false);

        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> called.set(true));

        helper = SetListenerHelper.addListener(helper,(Observable o) -> {throw new RuntimeException();});
        SetListenerHelper.fireValueChangedEvent(helper,change);

        assertTrue(called.get());
    }


    @Test
    public void testExceptionHandledByThreadUncaughtHandlerInMultipleInvalidation() {
        AtomicInteger called = new AtomicInteger(0);

        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> called.incrementAndGet());

        helper = SetListenerHelper.addListener(helper, (Observable o) -> {throw new RuntimeException();});
        helper = SetListenerHelper.addListener(helper, (Observable o) -> {throw new RuntimeException();});
        SetListenerHelper.fireValueChangedEvent(helper,change);

        assertEquals(2, called.get());
    }

    @Test
    public void testExceptionHandledByThreadUncaughtHandlerInSingleChange() {
        AtomicBoolean called = new AtomicBoolean(false);

        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> called.set(true));
        helper = SetListenerHelper.addListener(helper, (SetChangeListener.Change<? extends Object> c) -> {throw new RuntimeException();});
        SetListenerHelper.fireValueChangedEvent(helper,change);

        assertTrue(called.get());
    }

    @Test
    public void testExceptionHandledByThreadUncaughtHandlerInMultipleChange() {
        AtomicInteger called = new AtomicInteger(0);

        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> called.incrementAndGet());

        helper = SetListenerHelper.addListener(helper, (SetChangeListener.Change<? extends Object> c) -> {throw new RuntimeException();});
        helper = SetListenerHelper.addListener(helper, (SetChangeListener.Change<? extends Object> c) -> {throw new RuntimeException();});
        SetListenerHelper.fireValueChangedEvent(helper,change);

        assertEquals(2, called.get());
    }

    @Test
    public void testExceptionHandledByThreadUncaughtHandlerInMultipleChangeAndInvalidation() {
        AtomicInteger called = new AtomicInteger(0);

        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> called.incrementAndGet());

        helper = SetListenerHelper.addListener(helper, (SetChangeListener.Change<? extends Object> c) -> { throw new RuntimeException();});
        helper = SetListenerHelper.addListener(helper, (SetChangeListener.Change<? extends Object> c) -> { throw new RuntimeException();});
        helper = SetListenerHelper.addListener(helper, (Observable o) -> { throw new RuntimeException();});
        helper = SetListenerHelper.addListener(helper, (Observable o) -> {throw new RuntimeException();});
        SetListenerHelper.fireValueChangedEvent(helper,change);

        assertEquals(4, called.get());
    }

}
