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

package com.sun.javafx.application.preferences;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Contains information about a changed value.
 *
 * @param oldValue the old mappings
 * @param newValue the new mappings
 */
public record ChangedValue(Object oldValue, Object newValue) {

    /**
     * Returns a map that contains the new or changed mappings of {@code current} compared to {@code old}.
     * A value has changed if {@link Objects#equals(Object, Object)} or {@link Arrays#equals(Object[], Object[])}
     * returns {@code false} when invoked with the old and new value.
     *
     * @param old the old mappings
     * @param current the current mappings
     * @return a mapping of keys to changed values
     */
    public static Map<String, ChangedValue> getEffectiveChanges(Map<String, Object> old, Map<String, Object> current) {
        Map<String, ChangedValue> changed = null;

        for (Map.Entry<String, Object> entry : current.entrySet()) {
            Object newValue = entry.getValue();
            Object oldValue = old.get(entry.getKey());

            if (!Objects.deepEquals(oldValue, newValue)) {
                if (changed == null) {
                    changed = new HashMap<>();
                }

                changed.put(entry.getKey(), new ChangedValue(oldValue, newValue));
            }
        }

        return changed != null ? changed : Map.of();
    }
}
