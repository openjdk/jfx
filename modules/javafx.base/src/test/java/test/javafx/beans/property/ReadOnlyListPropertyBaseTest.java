/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.beans.property;

import com.sun.javafx.beans.BeanErrors;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyListPropertyBase;
import javafx.beans.property.ReadOnlyListPropertyBaseShim;
import javafx.collections.ListChangeListener;
import test.javafx.beans.InvalidationListenerMock;
import test.javafx.beans.value.ChangeListenerMock;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static test.util.MoreAssertions.assertThrows;

public class ReadOnlyListPropertyBaseTest {

    private static final Object UNDEFINED = null;
    private static final Object DEFAULT = null;
    private static final ObservableList<Object> VALUE_1 = FXCollections.observableArrayList();
    private static final ObservableList<Object> VALUE_2 = FXCollections.observableArrayList(new Object());

    private ReadOnlyPropertyMock property;
    private InvalidationListenerMock invalidationListener;
    private ChangeListenerMock<Object> changeListener;

    @Before
    public void setUp() throws Exception {
        property = new ReadOnlyPropertyMock();
        invalidationListener = new InvalidationListenerMock();
        changeListener = new ChangeListenerMock<Object>(UNDEFINED);
    }

    @Test
    public void testInvalidationListener() {
        property.addListener(invalidationListener);
        property.get();
        invalidationListener.reset();
        property.set(VALUE_1);
        invalidationListener.check(property, 1);
        property.removeListener(invalidationListener);
        invalidationListener.reset();
        property.set(VALUE_2);
        invalidationListener.check(null, 0);
    }

    @Test
    public void testChangeListener() {
        property.addListener(changeListener);
        property.get();
        changeListener.reset();
        property.set(VALUE_1);
        changeListener.check(property, DEFAULT, VALUE_1, 1);
        property.removeListener(changeListener);
        changeListener.reset();
        property.set(VALUE_2);
        changeListener.check(null, UNDEFINED, UNDEFINED, 0);
    }

    private static class ReadOnlyPropertyMock extends ReadOnlyListPropertyBase<Object> {

        private ObservableList<Object> value;

        public ReadOnlyPropertyMock() {}

        public ReadOnlyPropertyMock(ObservableList<Object> value) {
            this.value = value;
            value.addListener((ListChangeListener<? super Object>)this::fireValueChangedEvent);
        }

        @Override
        public Object getBean() {
            // not used
            return null;
        }

        @Override
        public String getName() {
            // not used
            return null;
        }

        private void set(ObservableList<Object> value) {
            this.value = value;
            ReadOnlyListPropertyBaseShim.fireValueChangedEvent(this);
        }

        @Override
        public ObservableList<Object> get() {
            return value;
        }

        @Override
        public ReadOnlyIntegerProperty sizeProperty() {
            fail("Not in use");
            return null;
        }

        @Override
        public ReadOnlyBooleanProperty emptyProperty() {
            fail("Not in use");
            return null;
        }
    }

    @Test
    public void testContentBoundPropertyThrowsExceptionWhenBidirectionalContentBindingIsAdded() {
        var target = new ReadOnlyPropertyMock(FXCollections.observableArrayList());
        var source = new ReadOnlyPropertyMock(FXCollections.observableArrayList());
        target.bindContent(source);
        var ex = assertThrows(IllegalStateException.class, () -> target.bindContentBidirectional(source));
        assertEquals(BeanErrors.CONTENT_BIND_CONFLICT_BIDIRECTIONAL.getMessage(target), ex.getMessage());
    }

    @Test
    public void testBidirectionalContentBoundPropertyThrowsExceptionWhenContentBindingIsAdded() {
        var target = new ReadOnlyPropertyMock(FXCollections.observableArrayList());
        var source = new ReadOnlyPropertyMock(FXCollections.observableArrayList());
        target.bindContentBidirectional(source);
        var ex = assertThrows(IllegalStateException.class, () -> target.bindContent(source));
        assertEquals(BeanErrors.CONTENT_BIND_CONFLICT_UNIDIRECTIONAL.getMessage(target), ex.getMessage());
    }

    @Test
    public void testContentBindingIsReplaced() {
        var target = new ReadOnlyPropertyMock(FXCollections.observableArrayList());
        var source = new ReadOnlyPropertyMock(FXCollections.observableArrayList());
        target.bindContent(source);
        target.bindContent(source);

        int[] calls = new int[1];
        target.addListener((ListChangeListener<Object>)change -> calls[0]++);

        source.add("foo");
        assertEquals(1, calls[0]);
    }

    @Test
    public void testContentBindingIsRemoved() {
        var target = new ReadOnlyPropertyMock(FXCollections.observableArrayList());
        var source = new ReadOnlyPropertyMock(FXCollections.observableArrayList());
        target.bindContent(source);
        source.add("foo");
        assertEquals(1, target.size());

        target.unbindContent();
        source.add("bar");
        assertEquals(1, target.size());
    }

    @Test
    public void testBidirectionalContentBindingIsReplaced() {
        var target = new ReadOnlyPropertyMock(FXCollections.observableArrayList());
        var source = new ReadOnlyPropertyMock(FXCollections.observableArrayList());
        target.bindContentBidirectional(source);
        target.bindContentBidirectional(source);

        int[] calls = new int[1];
        target.addListener((ListChangeListener<Object>) change -> calls[0]++);

        source.add("foo");
        assertEquals(1, calls[0]);
    }

    @Test
    public void testBidirectionalContentBindingIsRemoved() {
        var target = new ReadOnlyPropertyMock(FXCollections.observableArrayList());
        var source = new ReadOnlyPropertyMock(FXCollections.observableArrayList());
        target.bindContentBidirectional(source);
        source.add("foo");
        assertEquals(1, target.size());

        target.unbindContentBidirectional(source);
        source.add("bar");
        assertEquals(1, target.size());
    }

}
