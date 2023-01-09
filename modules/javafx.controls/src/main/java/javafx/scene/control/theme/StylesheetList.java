/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.theme;

import javafx.beans.value.WritableValue;
import javafx.collections.ObservableListBase;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implements a list that allows individual elements to be changed via a {@link WritableValue} wrapper.
 */
class StylesheetList extends ObservableListBase<String> {

    private final List<WritableValue<String>> elements;
    private final List<String> values;

    public StylesheetList() {
        elements = new ArrayList<>();
        values = new ArrayList<>();
    }

    public void lock() {
        beginChange();
    }

    public void unlock() {
        endChange();
    }

    public WritableValue<String> addFirstElement(String value) {
        if (value != null) {
            values.add(0, value);
        }

        WritableValue<String> element = new ElementImpl(value);
        elements.add(0, element);
        return element;
    }

    public WritableValue<String> addLastElement(String value) {
        if (value != null) {
            values.add(value);
        }

        WritableValue<String> element = new ElementImpl(value);
        elements.add(element);
        return element;
    }

    @Override
    public String get(int index) {
        return values.get(index);
    }

    @Override
    public int size() {
        return values.size();
    }

    private class ElementImpl implements WritableValue<String> {
        String currentValue;

        ElementImpl(String initialValue) {
            currentValue = initialValue;
        }

        @Override
        public String getValue() {
            return currentValue;
        }

        @Override
        public void setValue(String newValue) {
            if (Objects.equals(currentValue, newValue)) {
                return;
            }

            int index = 0;
            for (int i = 0, max = elements.size(); i < max; ++i) {
                WritableValue<String> element = elements.get(i);
                if (element == this) {
                    break;
                } else if (element.getValue() != null) {
                    ++index;
                }
            }

            beginChange();

            if (currentValue == null && newValue != null) {
                nextAdd(index, index + 1);
                values.add(index, newValue);
            } else if (currentValue != null && newValue == null) {
                nextRemove(index, currentValue);
                values.remove(index);
            } else if (currentValue != null) {
                nextReplace(index, index + 1, List.of(currentValue));
                values.set(index, newValue);
            }

            currentValue = newValue;

            endChange();
        }
    }

}
