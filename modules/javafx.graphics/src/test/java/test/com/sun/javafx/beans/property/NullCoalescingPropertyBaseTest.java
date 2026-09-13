/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.beans.property;

import com.sun.javafx.beans.property.NullCoalescingPropertyBase;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NullCoalescingPropertyBaseTest {

    @Test
    void nullEvaluatesToBaseValue() {
        var base = new SimpleStringProperty("foo");
        var property = new NullCoalescingPropertyImpl<>(base, true);
        property.set(null);
        assertEquals("foo", property.get());
        assertEquals("foo", property.getValue());
    }

    @Test
    void nonNullEvaluatesToLocalValue() {
        var base = new SimpleStringProperty("foo");
        var property = new NullCoalescingPropertyImpl<>(base, true);
        property.set("bar");
        assertEquals("bar", property.get());
        assertEquals("bar", property.getValue());
    }

    @Test
    void baseValueChangeIsVisible() {
        var base = new SimpleStringProperty("foo");
        var property = new NullCoalescingPropertyImpl<>(base, true);
        property.set(null);
        assertEquals("foo", property.get());
        base.set("bar");
        assertEquals("bar", property.get());
    }

    @Test
    void propertiesCanBeChained() {
        var base = new SimpleStringProperty("foo");
        var property1 = new NullCoalescingPropertyImpl<>(base, true);
        var property2 = new NullCoalescingPropertyImpl<>(property1, true);

        property2.set(null);
        assertEquals("foo", property2.get());

        property1.set("bar");
        assertEquals("bar", property2.get());

        property2.set("baz");
        assertEquals("baz", property2.get());
    }

    @Test
    void bindingOverridesBaseValueIfNotNull() {
        var base = new SimpleStringProperty("foo");
        var property = new NullCoalescingPropertyImpl<>(base, true);
        assertEquals("foo", property.get());

        var bindSource = new SimpleStringProperty("bar");
        property.bind(bindSource);
        assertEquals("bar", property.get());

        bindSource.set(null);
        assertEquals("foo", property.get());
    }

    @Test
    void changeEventObservableIsCorrectlyReported() {
        var base = new SimpleStringProperty();
        var property = new NullCoalescingPropertyImpl<>(base, true);
        var trace = new ArrayList<>();
        property.addListener(((observable, _, _) -> trace.add(observable)));
        base.set("foo");
        assertEquals(List.of(property), trace);
        base.set("bar");
        assertEquals(List.of(property, property), trace);
    }

    @Test
    void currentValueInOnInvalidatedMethodIsCorrect() {
        var actual = new String[1];
        var base = new SimpleStringProperty();
        var property = new NullCoalescingPropertyImpl<>(base, true) {
            @Override
            protected void onInvalidated() {
                actual[0] = get();
            }
        };

        base.set("foo");
        assertEquals("foo", actual[0]);

        property.set("bar");
        assertEquals("bar", actual[0]);

        property.set(null);
        assertEquals("foo", actual[0]);
    }

    @Test
    void notificationsAreOnlyFiredWhenCurrentValueHasChanged() {
        var invalidatedCount = new int[1];
        var listenerCount = new int[1];
        var base = new SimpleStringProperty();
        var property = new NullCoalescingPropertyImpl<>(base, true) {
            @Override
            protected void onInvalidated() {
                invalidatedCount[0]++;
            }
        };

        property.addListener((_, _, _) -> listenerCount[0]++);

        base.set("foo");
        assertEquals(1, invalidatedCount[0]);
        assertEquals(1, listenerCount[0]);

        property.set("foo");
        assertEquals(1, invalidatedCount[0]);
        assertEquals(1, listenerCount[0]);

        base.set("bar");
        assertEquals(1, invalidatedCount[0]);
        assertEquals(1, listenerCount[0]);

        property.set(null);
        assertEquals(2, invalidatedCount[0]);
        assertEquals(2, listenerCount[0]);
    }

    @Test
    void baseChangedNotificationsAreNotFiredWhenPropertyIsDisconnected() {
        var invalidatedCount = new int[1];
        var base = new SimpleStringProperty();
        var property = new NullCoalescingPropertyImpl<>(base, false) {
            @Override
            protected void onInvalidated() {
                invalidatedCount[0]++;
            }
        };

        base.set("foo");
        assertEquals(0, invalidatedCount[0]);

        base.set("bar");
        assertEquals(0, invalidatedCount[0]);

        property.connect();
        assertEquals(1, invalidatedCount[0]);

        base.set("baz");
        assertEquals(2, invalidatedCount[0]);
    }

    @Test
    void connectingPropertyUpdatesCurrentValue() {
        var base = new SimpleStringProperty("foo");
        var property = new NullCoalescingPropertyImpl<>(base, false);
        assertEquals("foo", property.getValue());
        base.set("bar");
        assertEquals("foo", property.getValue());
        property.connect();
        assertEquals("bar", property.getValue());
    }

    private static class NullCoalescingPropertyImpl<T> extends NullCoalescingPropertyBase<T> {
        public NullCoalescingPropertyImpl(ObservableValue<T> baseValue, boolean connected) {
            super(baseValue);

            if (connected) {
                connect();
            }
        }

        @Override public Object getBean() { return null; }
        @Override public String getName() { return ""; }
    }
}
