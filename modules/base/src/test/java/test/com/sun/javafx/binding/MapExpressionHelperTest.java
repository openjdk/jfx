/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.binding;

import com.sun.javafx.binding.MapExpressionHelper;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.junit.Before;
import org.junit.Test;
import test.javafx.beans.InvalidationListenerMock;
import test.javafx.beans.value.ChangeListenerMock;
import test.javafx.collections.MockMapObserver;

public class MapExpressionHelperTest {
    private static final ObservableMap<Object, Object> UNDEFINED = null;
    private Object key;
    private Object value;
    private ObservableMap<Object, Object> data1;
    private ObservableMap<Object, Object> data2;

    private MapExpressionHelper helper;
    private MapProperty<Object, Object> observable;

    private InvalidationListenerMock[] invalidationListener;
    private ChangeListenerMock<? super ObservableMap<Object, Object>>[] changeListener;
    private MockMapObserver<Object, Object>[] mapChangeListener;

    @Before
    public void setUp() {
        key = new Object();
        value = new Object();
        data1 = FXCollections.observableHashMap();
        data2 = FXCollections.observableHashMap();
        data2.put(key, value);

        helper = null;
        observable = new SimpleMapProperty<>(data1);

        invalidationListener = new InvalidationListenerMock[]{
            new InvalidationListenerMock(), new InvalidationListenerMock(), new InvalidationListenerMock()
        };
        changeListener = new ChangeListenerMock[]{
            new ChangeListenerMock(UNDEFINED), new ChangeListenerMock(UNDEFINED), new ChangeListenerMock(UNDEFINED)
        };
        mapChangeListener = new MockMapObserver[]{
            new MockMapObserver<>(), new MockMapObserver<>(), new MockMapObserver<>()
        };
    }

    @Test
    public void testRemoveInvalidation() {
        helper = MapExpressionHelper.addListener(helper, observable, changeListener[0]);
        helper = MapExpressionHelper.addListener(helper, observable, mapChangeListener[0]);

        helper = MapExpressionHelper.removeListener(helper, invalidationListener[1]);

        helper = MapExpressionHelper.addListener(helper, observable, invalidationListener[0]);

        helper = MapExpressionHelper.removeListener(helper, invalidationListener[1]);

        helper = MapExpressionHelper.addListener(helper, observable, invalidationListener[2]);
        helper = MapExpressionHelper.addListener(helper, observable, invalidationListener[1]);

        helper = MapExpressionHelper.removeListener(helper, invalidationListener[0]);
        MapExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(null, 0);
        invalidationListener[1].check(observable, 1);
        invalidationListener[2].check(observable, 1);

        helper = MapExpressionHelper.removeListener(helper, invalidationListener[1]);
        MapExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(null, 0);
        invalidationListener[1].check(null, 0);
        invalidationListener[2].check(observable, 1);

        helper = MapExpressionHelper.removeListener(helper, invalidationListener[2]);
        MapExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(null, 0);
        invalidationListener[1].check(null, 0);
        invalidationListener[2].check(null, 0);
    }

    @Test
    public void testRemoveInvalidation8136465() {
        helper = MapExpressionHelper.addListener(helper, observable, invalidationListener[0]);
        helper = MapExpressionHelper.addListener(helper, observable, invalidationListener[1]);

        MapExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(observable, 1);
        invalidationListener[1].check(observable, 1);

        helper = MapExpressionHelper.removeListener(helper, invalidationListener[0]);

        MapExpressionHelper.fireValueChangedEvent(helper);
        invalidationListener[0].check(null, 0);
        invalidationListener[1].check(observable, 1);
    }

    @Test
    public void testRemoveChange() {
        helper = MapExpressionHelper.addListener(helper, observable, invalidationListener[0]);
        helper = MapExpressionHelper.addListener(helper, observable, mapChangeListener[0]);

        helper = MapExpressionHelper.removeListener(helper, changeListener[1]);

        helper = MapExpressionHelper.addListener(helper, observable, changeListener[0]);

        helper = MapExpressionHelper.removeListener(helper, changeListener[1]);

        helper = MapExpressionHelper.addListener(helper, observable, changeListener[2]);
        helper = MapExpressionHelper.addListener(helper, observable, changeListener[1]);

        helper = MapExpressionHelper.removeListener(helper, changeListener[0]);
        observable.set(data2);
        MapExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(null, UNDEFINED, UNDEFINED, 0);
        changeListener[1].check(observable, data1, data2, 1);
        changeListener[2].check(observable, data1, data2, 1);

        helper = MapExpressionHelper.removeListener(helper, changeListener[1]);
        observable.set(data1);
        MapExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(null, UNDEFINED, UNDEFINED, 0);
        changeListener[1].check(null, UNDEFINED, UNDEFINED, 0);
        changeListener[2].check(observable, data2, data1, 1);

        helper = MapExpressionHelper.removeListener(helper, changeListener[2]);
        observable.set(data2);
        MapExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(null, UNDEFINED, UNDEFINED, 0);
        changeListener[1].check(null, UNDEFINED, UNDEFINED, 0);
        changeListener[2].check(null, UNDEFINED, UNDEFINED, 0);
    }

    @Test
    public void testRemoveChange8136465() {
        helper = MapExpressionHelper.addListener(helper, observable, changeListener[0]);
        helper = MapExpressionHelper.addListener(helper, observable, changeListener[1]);

        observable.set(data2);
        MapExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(observable, data1, data2, 1);
        changeListener[1].check(observable, data1, data2, 1);

        helper = MapExpressionHelper.removeListener(helper, changeListener[0]);

        observable.set(data1);
        MapExpressionHelper.fireValueChangedEvent(helper);
        changeListener[0].check(null, UNDEFINED, UNDEFINED, 0);
        changeListener[1].check(observable, data2, data1, 1);
    }

    @Test
    public void testRemoveMapChange() {
        helper = MapExpressionHelper.addListener(helper, observable, invalidationListener[0]);
        helper = MapExpressionHelper.addListener(helper, observable, changeListener[0]);

        helper = MapExpressionHelper.removeListener(helper, mapChangeListener[1]);

        helper = MapExpressionHelper.addListener(helper, observable, mapChangeListener[0]);

        helper = MapExpressionHelper.removeListener(helper, mapChangeListener[1]);

        helper = MapExpressionHelper.addListener(helper, observable, mapChangeListener[2]);
        helper = MapExpressionHelper.addListener(helper, observable, mapChangeListener[1]);

        helper = MapExpressionHelper.removeListener(helper, mapChangeListener[0]);
        observable.set(data2);
        MapExpressionHelper.fireValueChangedEvent(helper);
        mapChangeListener[0].check0();
        mapChangeListener[1].assertAdded(MockMapObserver.Tuple.tup(key, value));
        mapChangeListener[1].clear();
        mapChangeListener[2].assertAdded(MockMapObserver.Tuple.tup(key, value));
        mapChangeListener[2].clear();

        helper = MapExpressionHelper.removeListener(helper, mapChangeListener[1]);
        observable.set(data1);
        MapExpressionHelper.fireValueChangedEvent(helper);
        mapChangeListener[0].check0();
        mapChangeListener[1].check0();
        mapChangeListener[2].assertRemoved(MockMapObserver.Tuple.tup(key, value));
        mapChangeListener[2].clear();

        helper = MapExpressionHelper.removeListener(helper, mapChangeListener[2]);
        observable.set(data2);
        MapExpressionHelper.fireValueChangedEvent(helper);
        mapChangeListener[0].check0();
        mapChangeListener[1].check0();
        mapChangeListener[2].check0();
    }

    @Test
    public void testRemoveMapChange8136465() {
        helper = MapExpressionHelper.addListener(helper, observable, mapChangeListener[0]);
        helper = MapExpressionHelper.addListener(helper, observable, mapChangeListener[1]);

        observable.set(data2);
        MapExpressionHelper.fireValueChangedEvent(helper);
        mapChangeListener[0].assertAdded(MockMapObserver.Tuple.tup(key, value));
        mapChangeListener[0].clear();
        mapChangeListener[1].assertAdded(MockMapObserver.Tuple.tup(key, value));
        mapChangeListener[1].clear();

        helper = MapExpressionHelper.removeListener(helper, mapChangeListener[0]);

        observable.set(data1);
        MapExpressionHelper.fireValueChangedEvent(helper);
        mapChangeListener[0].check0();
        mapChangeListener[1].assertRemoved(MockMapObserver.Tuple.tup(key, value));
        mapChangeListener[1].clear();
    }
}
