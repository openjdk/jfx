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

package test.com.sun.javafx.application.preferences;

import com.sun.javafx.application.preferences.ChangedValue;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ChangedValueTest {

    @Test
    public void testSingleChangedValue() {
        var changes = ChangedValue.getEffectiveChanges(Map.of("k1", 1, "k2", "foo"), Map.of("k1", 2, "k2", "foo"));
        assertEquals(1, changes.size());
        assertEquals(1, changes.get("k1").oldValue());
        assertEquals(2, changes.get("k1").newValue());
    }

    @Test
    public void testMultipleChangedValues() {
        var changes = ChangedValue.getEffectiveChanges(Map.of("k1", 1, "k2", "foo"), Map.of("k1", 2, "k2", "bar"));
        assertEquals(2, changes.size());
        assertEquals(1, changes.get("k1").oldValue());
        assertEquals(2, changes.get("k1").newValue());
        assertEquals("foo", changes.get("k2").oldValue());
        assertEquals("bar", changes.get("k2").newValue());
    }

    @Test
    public void testSingleChangedArrayValue() {
        var changes = ChangedValue.getEffectiveChanges(
            Map.of("k1", new String[] {"foo"}, "k2", new String[] {"bar"}),
            Map.of("k1", new String[] {"foo"}, "k2", new String[] {"baz"}));
        assertEquals(1, changes.size());
        assertArrayEquals(new String[] {"bar"}, (String[])changes.get("k2").oldValue());
        assertArrayEquals(new String[] {"baz"}, (String[])changes.get("k2").newValue());
    }

    @Test
    public void testMultipleChangedArrayValues() {
        var changes = ChangedValue.getEffectiveChanges(
            Map.of("k1", new String[] {"foo"}, "k2", new String[] {"qux"}),
            Map.of("k1", new String[] {"bar"}, "k2", new String[] {"quz"}));
        assertEquals(2, changes.size());
        assertArrayEquals(new String[] {"foo"}, (String[])changes.get("k1").oldValue());
        assertArrayEquals(new String[] {"bar"}, (String[])changes.get("k1").newValue());
        assertArrayEquals(new String[] {"qux"}, (String[])changes.get("k2").oldValue());
        assertArrayEquals(new String[] {"quz"}, (String[])changes.get("k2").newValue());
    }

}
