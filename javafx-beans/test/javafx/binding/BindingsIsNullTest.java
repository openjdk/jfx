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
package javafx.binding;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javafx.beans.InvalidationListenerMock;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.junit.Before;
import org.junit.Test;

public class BindingsIsNullTest {

    private ObjectProperty<Object> oo;
    private StringProperty os;
    private InvalidationListenerMock observer;

    @Before
    public void setUp() {
        oo = new SimpleObjectProperty<Object>();
        os = new SimpleStringProperty();
        observer = new InvalidationListenerMock();
    }

    @Test
    public void test_Object_IsNull() {
        final BooleanBinding binding = Bindings.isNull(oo);
        binding.addListener(observer);

        // check initial value
        assertTrue(binding.get());
        DependencyUtils.checkDependencies(binding.getDependencies(), oo);
        observer.reset();

        // change operand
        oo.set(new Object());
        assertFalse(binding.get());
        observer.check(binding, 1);

        // change again
        oo.set(null);
        assertTrue(binding.get());
        observer.check(binding, 1);
    }
    
    @Test
    public void test_Object_IsNotNull() {
        final BooleanBinding binding = Bindings.isNotNull(oo);
        binding.addListener(observer);

        // check initial value
        assertFalse(binding.get());
        DependencyUtils.checkDependencies(binding.getDependencies(), oo);
        observer.reset();

        // change operand
        oo.set(new Object());
        assertTrue(binding.get());
        observer.check(binding, 1);

        // change again
        oo.set(null);
        assertFalse(binding.get());
        observer.check(binding, 1);
    }

    @Test
    public void test_String_IsNull() {
        final BooleanBinding binding = Bindings.isNull(os);
        binding.addListener(observer);

        // check initial value
        assertTrue(binding.get());
        DependencyUtils.checkDependencies(binding.getDependencies(), os);
        observer.reset();

        // change operand
        os.set("Hello World");
        assertFalse(binding.get());
        observer.check(binding, 1);

        // change again
        os.set(null);
        assertTrue(binding.get());
        observer.check(binding, 1);
    }
    
    @Test
    public void test_String_IsNotNull() {
        final BooleanBinding binding = Bindings.isNotNull(os);
        binding.addListener(observer);

        // check initial value
        assertFalse(binding.get());
        DependencyUtils.checkDependencies(binding.getDependencies(), os);
        observer.reset();

        // change operand
        os.set("Hello World");
        assertTrue(binding.get());
        observer.check(binding, 1);

        // change again
        os.set(null);
        assertFalse(binding.get());
        observer.check(binding, 1);
    }

    @Test(expected=NullPointerException.class)
    public void test_IsNull_NPE() {
        Bindings.isNull(null);
    }

    @Test(expected=NullPointerException.class)
    public void test_IsNotNull_NPE() {
        Bindings.isNotNull(null);
    }

}
