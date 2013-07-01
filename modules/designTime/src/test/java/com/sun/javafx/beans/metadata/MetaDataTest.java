/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.beans.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

/**
 *
 * @author Richard
 */
public class MetaDataTest {

    public MetaDataTest() {
    }

    @Test public void testDecapitalize_null() {
        assertNull(MetaData.decapitalize(null));
    }

    @Test public void testDecapitalize_emptyString() {
        assertEquals("", MetaData.decapitalize(""));
    }

    @Test public void testDecapitalize_emptySpaces() {
        assertEquals("", MetaData.decapitalize("  "));
    }

    @Test public void testDecapitalize_leadingSpaces() {
        assertEquals("someProperty", MetaData.decapitalize("  SomeProperty"));
    }

    @Test public void testDecapitalize_trailingSpaces() {
        assertEquals("someProperty", MetaData.decapitalize("SomeProperty   "));
    }

    @Test public void testDecapitalize_allCaps() {
        assertEquals("URL", MetaData.decapitalize("URL"));
    }

    @Test public void testDecapitalize_MixedCase() {
        assertEquals("someProperty", MetaData.decapitalize("SomeProperty"));
    }

    @Test public void testDecapitalize_MixedCase2() {
        assertEquals("someURL", MetaData.decapitalize("SomeURL"));
    }

    @Test public void testToDisplayName_null() {
        assertNull(MetaData.toDisplayName(null));
    }

    @Test public void testToDisplayName_emptyString() {
        assertEquals("", MetaData.toDisplayName(""));
    }

    @Test public void testToDisplayName_emptySpaces() {
        assertEquals("", MetaData.toDisplayName("  "));
    }

    @Test public void testToDisplayName_leadingSpaces() {
        assertEquals("Some Property", MetaData.toDisplayName("  SomeProperty"));
    }

    @Test public void testToDisplayName_trailingSpaces() {
        assertEquals("Some Property", MetaData.toDisplayName("SomeProperty   "));
    }

    @Test public void testToDisplayName_allCaps() {
        assertEquals("URL", MetaData.toDisplayName("URL"));
    }

    @Test public void testToDisplayName_MixedCase() {
        assertEquals("Some Property", MetaData.toDisplayName("SomeProperty"));
    }

    @Test public void testToDisplayName_MixedCase2() {
        assertEquals("Some URL", MetaData.toDisplayName("SomeURL"));
    }

    @Test public void testToDisplayName_TrailingNumbers() {
        assertEquals("Widget 3", MetaData.toDisplayName("widget3"));
    }

    @Test public void testToDisplayName_TrailingNumbers2() {
        assertEquals("Widget 3", MetaData.toDisplayName("Widget3"));
    }

    @Test public void testToDisplayName_TrailingNumbers3() {
        assertEquals("Some Widget 3", MetaData.toDisplayName("SomeWidget3"));
    }

    @Test public void testToDisplayName_UnderscoresToSpaces() {
        assertEquals("Some Property", MetaData.toDisplayName("some_property"));
    }

    @Test public void testToDisplayName_UnderscoresToSpaces2() {
        assertEquals("Some Property", MetaData.toDisplayName("Some__Property"));
    }

    @Test public void testToDisplayName_UnderscoresToSpaces3() {
        assertEquals("Some Property", MetaData.toDisplayName("_Some_Property"));
    }

    @Test public void testToDisplayName_UnderscoresToSpaces4() {
        assertEquals("Some Property", MetaData.toDisplayName("SomeProperty_"));
    }

    @Test public void testToDisplayName_UnderscoresToSpaces5() {
        assertEquals("Some 3 Property", MetaData.toDisplayName("Some_3Property"));
    }
}
