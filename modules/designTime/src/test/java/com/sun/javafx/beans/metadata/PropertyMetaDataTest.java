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

import com.sun.javafx.beans.metadata.widgets.Widget;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Richard
 */
public class PropertyMetaDataTest {

    private PropertyMetaData findProperty(BeanMetaData md, String name) {
        List<PropertyMetaData> list = md.getProperties();
        for (PropertyMetaData pmd : list) {
            if (name.equals(pmd.getName())) {
                return pmd;
            }
        }
        return null;
    }

    @Test public void testIntrospectedBeanMetaData_nameProperty() throws Exception {
        BeanMetaData<Widget> md = new BeanMetaData<Widget>(Widget.class);
        PropertyMetaData metaData = findProperty(md, "name");
        assertEquals("name", metaData.getName());
        assertEquals("Name", metaData.getDisplayName());
        assertEquals("", metaData.getShortDescription());
        assertEquals("", metaData.getCategory());
        assertEquals(String.class, metaData.getType());

        Widget widget = new Widget();
        widget.setName("Richard");
        assertEquals("Richard", metaData.getGetterMethod().invoke(widget));
        metaData.getSetterMethod().invoke(widget, "John");
        assertEquals("John", widget.getName());
        assertSame(widget.nameProperty(), metaData.getPropertyMethod().invoke(widget));
    }

    @Test public void testIntrospectedBeanMetaData_colorizedProperty() throws Exception {
        BeanMetaData<Widget> md = new BeanMetaData<Widget>(Widget.class);
        PropertyMetaData metaData = findProperty(md, "colorized");
        assertEquals("colorized", metaData.getName());
        assertEquals("Psychodelic", metaData.getDisplayName());
        assertEquals("Colorize me!", metaData.getShortDescription());
        assertEquals("Special", metaData.getCategory());
        assertEquals(boolean.class, metaData.getType());
    }

    @Test public void testIntrospectedBeanMetaData_widgetValueProperty() throws Exception {
        BeanMetaData<Widget> md = new BeanMetaData<Widget>(Widget.class);
        PropertyMetaData metaData = findProperty(md, "widgetValue");
        assertEquals("widgetValue", metaData.getName());
        assertEquals("Widget Value", metaData.getDisplayName());
        assertEquals("", metaData.getShortDescription());
        assertEquals("", metaData.getCategory());
        assertEquals(int.class, metaData.getType());
        assertNull(metaData.getSetterMethod());

        Widget widget = new Widget();
        widget.increment();
        assertEquals(101, metaData.getGetterMethod().invoke(widget));
        assertSame(widget.widgetValueProperty(), metaData.getPropertyMethod().invoke(widget));
    }

//    @Test public void readOnlyPropertyShouldHaveGetterAndPropertyMethodButNoSetter() {
//
//    }
//
//    @Test public void writablePropertyShouldHaveGetterAndSetterAndPropertyMethod() {
//
//    }
//
//    @Test public void immutablePropertyShouldHaveGetterOnly() {
//
//    }
}
