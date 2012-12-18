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
package com.sun.scenario;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javafx.util.Callback;

import org.junit.Test;

public class SettingsTest {


    @Test
    public void testStringValue() {
        Settings.set("foo", "foobar");
        assertEquals("foobar", Settings.get("foo"));
    }

    @Test
    public void testBooleanValue() {
        Settings.set("foo", "false");
        assertFalse(Settings.getBoolean("foo"));
        assertFalse(Settings.getBoolean("foo", false));
        assertFalse(Settings.getBoolean("bar", false));
        Settings.set("bar", "true");
        assertTrue(Settings.getBoolean("bar", false));
    }

    @Test
    public void testIntValue() {
        Settings.set("foo", "128");
        assertEquals(128, Settings.getInt("foo", 32));
        assertEquals(32, Settings.getInt("bar", 32));
    }

    private String tmp;

    @Test
    public void testListener() {
        final Callback<String, Void> listener = new Callback<String, Void>() {

            @Override
            public Void call(String key) {
                tmp =  Settings.get(key);
                return null;
            }
        };
        Settings.addPropertyChangeListener(listener);
        Settings.set("foo", "bar");
        assertEquals(tmp, "bar");
        Settings.removePropertyChangeListener(listener);
    }

    @Test
    public void testSystemProperties() {
        System.setProperty("foo", "bar");
        Settings.set("foo", null);
        assertEquals(Settings.get("foo"), "bar");

    }
}
