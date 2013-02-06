/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.property.adapter;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.fail;

/**
*/
public class JavaBeanPropertyBuilderHelperTest {

    private JavaBeanPropertyBuilderHelper helperPOJOBean;
    private JavaBeanPropertyBuilderHelper helperPOJOBeanWithNonStandardNames;

    @Before
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

    @Test(expected = NullPointerException.class)
    public void testSetup_WithNameIsNull() {
        try {
            helperPOJOBean.name(null);
            helperPOJOBean.getDescriptor();
        } catch (NoSuchMethodException e) {
            fail();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetup_WithNameIsEmpty() {
        try {
            helperPOJOBean.name("");
            helperPOJOBean.getDescriptor();
        } catch (NoSuchMethodException e) {
            fail();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testSetup_WithBeanClassIsNull() {
        try {
            helperPOJOBean.beanClass(null);
            helperPOJOBean.getDescriptor();
        } catch (NoSuchMethodException e) {
            fail();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testSetup_WithNonStandardNames_WithNameIsNull() {
        try {
            helperPOJOBeanWithNonStandardNames.name(null);
            helperPOJOBeanWithNonStandardNames.getDescriptor();
        } catch (NoSuchMethodException e) {
            fail();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testSetup_WithNonStandardNames_WithBeanClassIsNull() {
        try {
            helperPOJOBeanWithNonStandardNames.beanClass(null);
            helperPOJOBeanWithNonStandardNames.getDescriptor();
        } catch (NoSuchMethodException e) {
            fail();
        }
    }

    @Test(expected = NoSuchMethodException.class)
    public void testSetup_WithNonStandardNames_WithGetterNameIsNull() throws NoSuchMethodException {
        helperPOJOBeanWithNonStandardNames.getterName(null);
        helperPOJOBeanWithNonStandardNames.getDescriptor();
    }

    @Test(expected = NoSuchMethodException.class)
    public void testSetup_WithNonStandardNames_WithSetterNameIsNull() throws NoSuchMethodException {
        helperPOJOBeanWithNonStandardNames.setterName(null);
        helperPOJOBeanWithNonStandardNames.getDescriptor();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetup_WithNonStandardNames_WithNameIsEmpty() {
        try {
            helperPOJOBeanWithNonStandardNames.name("");
            helperPOJOBeanWithNonStandardNames.getDescriptor();
        } catch (NoSuchMethodException e) {
            fail();
        }
    }

    @Test(expected = NoSuchMethodException.class)
    public void testSetup_WithNonStandardNames_WithGetterNameIsEmpty() throws NoSuchMethodException {
        helperPOJOBeanWithNonStandardNames.getterName("");
        helperPOJOBeanWithNonStandardNames.getDescriptor();
    }

    @Test(expected = NoSuchMethodException.class)
    public void testSetup_WithNonStandardNames_WithSetterNameIsEmpty() throws NoSuchMethodException {
        helperPOJOBeanWithNonStandardNames.setterName("");
        helperPOJOBeanWithNonStandardNames.getDescriptor();
    }

    @Test(expected = NullPointerException.class)
    public void testSetup_WithNonStandardAccessors_WithNameIsNull() throws NoSuchMethodException {
        helperPOJOBeanWithNonStandardNames.getterName(null);
        helperPOJOBeanWithNonStandardNames.setterName(null);
        try {
            final Method getter = POJOBeanWithNonStandardNames.class.getMethod("readX");
            final Method setter = POJOBeanWithNonStandardNames.class.getMethod("writeX", Object.class);
            helperPOJOBeanWithNonStandardNames.getter(getter);
            helperPOJOBeanWithNonStandardNames.setter(setter);

            helperPOJOBeanWithNonStandardNames.name(null);
        } catch (NoSuchMethodException e) {
            fail("Error in test code. Should not happen.");
        }
        helperPOJOBeanWithNonStandardNames.getDescriptor();
    }

    @Test(expected = NoSuchMethodException.class)
    public void testSetup_WithNonStandardAccessors_WithGetterIsNull() throws NoSuchMethodException {
        helperPOJOBeanWithNonStandardNames.getterName(null);
        helperPOJOBeanWithNonStandardNames.setterName(null);
        try {
            final Method setter = POJOBeanWithNonStandardNames.class.getMethod("writeX", Object.class);
            helperPOJOBeanWithNonStandardNames.setter(setter);

            helperPOJOBeanWithNonStandardNames.getter(null);
        } catch (NoSuchMethodException e) {
            fail("Error in test code. Should not happen.");
        }
        helperPOJOBeanWithNonStandardNames.getDescriptor();
    }

    @Test(expected = NoSuchMethodException.class)
    public void testSetup_WithNonStandardAccessors_WithSetterIsNull() throws NoSuchMethodException {
        helperPOJOBeanWithNonStandardNames.getterName(null);
        helperPOJOBeanWithNonStandardNames.setterName(null);
        try {
            final Method getter = POJOBeanWithNonStandardNames.class.getMethod("readX");
            helperPOJOBeanWithNonStandardNames.getter(getter);

            helperPOJOBeanWithNonStandardNames.setter(null);
        } catch (NoSuchMethodException e) {
            fail("Error in test code. Should not happen.");
        }
        helperPOJOBeanWithNonStandardNames.getDescriptor();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetup_WithNonStandardAccessors_WithNameIsEmpty() throws NoSuchMethodException {
        helperPOJOBeanWithNonStandardNames.getterName(null);
        helperPOJOBeanWithNonStandardNames.setterName(null);
        try {
            final Method getter = POJOBeanWithNonStandardNames.class.getMethod("readX");
            final Method setter = POJOBeanWithNonStandardNames.class.getMethod("writeX", Object.class);
            helperPOJOBeanWithNonStandardNames.getter(getter);
            helperPOJOBeanWithNonStandardNames.setter(setter);

            helperPOJOBeanWithNonStandardNames.name("");
        } catch (NoSuchMethodException e) {
            fail("Error in test code. Should not happen.");
        }
        helperPOJOBeanWithNonStandardNames.getDescriptor();
    }

    public static class POJOBean {
        private Object x;

        public POJOBean(Object x) {this.x = x;}

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
