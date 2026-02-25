/*
 * Copyright (c) 2012, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.property.adapter;

import com.sun.javafx.property.adapter.JavaBeanPropertyBuilderHelper;
import com.sun.javafx.property.adapter.PropertyDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 */
public class JavaBeanPropertyBuilderHelperTest {

    private JavaBeanPropertyBuilderHelper helperPOJOBean;
    private JavaBeanPropertyBuilderHelper helperPOJOBeanWithNonStandardNames;

    @BeforeEach
    public void setUp() {
        helperPOJOBean = new JavaBeanPropertyBuilderHelper();
        helperPOJOBean.beanClass(POJOBean.class);
        helperPOJOBean.name("x");

        helperPOJOBeanWithNonStandardNames = new JavaBeanPropertyBuilderHelper();
        helperPOJOBeanWithNonStandardNames.beanClass(POJOBeanWithNonStandardNames.class);
        helperPOJOBeanWithNonStandardNames.name("x");
        helperPOJOBeanWithNonStandardNames.getterName("readX");
        helperPOJOBeanWithNonStandardNames.setterName("writeX");
    }

    @Test
    public void testSetup_WithNameIsNull() {
        assertThrows(NullPointerException.class, () -> {
            helperPOJOBean.name(null);
            helperPOJOBean.getDescriptor();
        });
    }

    @Test
    public void testSetup_WithNameIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            helperPOJOBean.name("");
            helperPOJOBean.getDescriptor();
        });
    }

    @Test
    public void testSetup_WithBeanClassIsNull() {
        assertThrows(NullPointerException.class, () -> {
            helperPOJOBean.beanClass(null);
            helperPOJOBean.getDescriptor();
        });
    }

    @Test
    public void testSetup_WithNonStandardNames_WithNameIsNull() {
        assertThrows(NullPointerException.class, () -> {
            helperPOJOBeanWithNonStandardNames.name(null);
            helperPOJOBeanWithNonStandardNames.getDescriptor();
        });
    }

    @Test
    public void testSetup_WithNonStandardNames_WithBeanClassIsNull() {
        assertThrows(NullPointerException.class, () -> {
            helperPOJOBeanWithNonStandardNames.beanClass(null);
            helperPOJOBeanWithNonStandardNames.getDescriptor();
        });
    }

    @Test
    public void testSetup_WithNonStandardNames_WithGetterNameIsNull() {
        assertThrows(NoSuchMethodException.class, () -> {
            helperPOJOBeanWithNonStandardNames.getterName(null);
            helperPOJOBeanWithNonStandardNames.getDescriptor();
        });
    }

    @Test
    public void testSetup_WithNonStandardNames_WithSetterNameIsNull() {
        assertThrows(NoSuchMethodException.class, () -> {
            helperPOJOBeanWithNonStandardNames.setterName(null);
            helperPOJOBeanWithNonStandardNames.getDescriptor();
        });
    }

    @Test
    public void testSetup_WithNonStandardAccessors_WithNameIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            helperPOJOBeanWithNonStandardNames.name("");
            helperPOJOBeanWithNonStandardNames.getDescriptor();
        });
    }

    @Test
    public void testReusabilityWhenChangeOfBeanClass() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object x = new Object();

        PropertyDescriptor descriptor = helperPOJOBean.getDescriptor();
        assertEquals(x, descriptor.getGetter().invoke(new POJOBean(x)));
        descriptor.getSetter().invoke(new POJOBean(x), new Object());

        helperPOJOBean.beanClass(POJOBean2.class);

        descriptor = helperPOJOBean.getDescriptor();
        assertEquals(x, descriptor.getGetter().invoke(new POJOBean2(x)));
        descriptor.getSetter().invoke(new POJOBean2(x), new Object());
    }

    public static class POJOBean {
        private Object x;

        public POJOBean(Object x) {this.x = x;}

        public Object getX() {return x;}
        public void setX(Object x) {this.x = x;}
    }

    public static class POJOBean2 {
        private Object x;

        public POJOBean2(Object x) {this.x = x;}

        public Object getX() {return x;}
        public void setX(Object x) {this.x = x;}
    }

    public static class POJOBeanWithNonStandardNames {
        private Object x;

        public POJOBeanWithNonStandardNames(Object x) {this.x = x;}

        public Object readX() {return x;}
        public void writeX(Object x) {this.x = x;}
    }
}
