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

package test.com.sun.javafx.binding;

import com.sun.javafx.binding.ListenerManager;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ListenerManagerTest implements ObservableValue<String> {
    private final List<String> notifications = new ArrayList<>();

    private final ChangeListener<String> cl1 = (obs, o, n) -> notifications.add("CL1: " + o + " -> " + n);
    private final ChangeListener<String> cl2 = (obs, o, n) -> notifications.add("CL2: " + o + " -> " + n);

    private Object data;
    private String value = "0";

    private final ListenerManager<String, ListenerManagerTest> helper = new ListenerManager<>() {
        @Override
        protected Object getData(ListenerManagerTest instance) {
            return data;
        }

        @Override
        protected void setData(ListenerManagerTest instance, Object data) {
            ListenerManagerTest.this.data = data;
        }
    };

    @Test
    void shouldNotifyChangeListeners() {
        value = "A";

        helper.fireValueChanged(this, "0", data);  // checks if passing null data works fine

        assertEquals(List.of(), notifications);  // expect nothing, as there are no listeners

        helper.addListener(this, cl1);

        assertNotNull(data);

        value = "B";

        helper.fireValueChanged(this, "A", data);

        assertEquals(List.of("CL1: A -> B"), notifications);

        helper.addListener(this, cl2);

        notifications.clear();
        value = "C";

        helper.fireValueChanged(this, "B", data);

        assertEquals(List.of("CL1: B -> C", "CL2: B -> C"), notifications);

        helper.removeListener(this, cl1);

        notifications.clear();
        value = "D";

        helper.fireValueChanged(this, "C", data);

        assertEquals(List.of("CL2: C -> D"), notifications);

        notifications.clear();
        value = "E";

        helper.fireValueChanged(this, "D", data);

        assertEquals(List.of("CL2: D -> E"), notifications);
    }

    @Override
    public void addListener(InvalidationListener listener) {
    }

    @Override
    public void removeListener(InvalidationListener listener) {
    }

    @Override
    public void addListener(ChangeListener<? super String> listener) {
    }

    @Override
    public void removeListener(ChangeListener<? super String> listener) {
    }

    @Override
    public String getValue() {
        return value;
    }
}
