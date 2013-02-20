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

package javafx.binding;

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class When_Object_Test extends WhenTestBase<Object, ObjectProperty<Object>> {
    @SuppressWarnings("unchecked")
    public When_Object_Test() {
        super(
            new Object(), new Object(), new Object(), new Object(),
            new SimpleObjectProperty<Object>(), new SimpleObjectProperty<Object>()
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public Binding<Object>[] generatePropertyPropertyList(ObjectProperty<Object> p0, ObjectProperty<Object>[] probs) {
        return new Binding[] {
        		Bindings.when(cond).then(p0).otherwise(probs[0])
        };
    }

    @Override
    public Binding<Object> generatePropertyProperty(ObjectProperty<Object> op0, ObjectProperty<Object> op1) {
        return Bindings.when(cond).then(op0).otherwise(op1);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Binding<Object>[] generatePropertyPrimitive(ObjectProperty<Object> op0, Object op1) {
        return new Binding[] {
        		Bindings.when(cond).then(op0).otherwise(op1)
        };
    }
    @SuppressWarnings("unchecked")
    @Override
    public Binding<Object>[] generatePrimitiveProperty(Object op0, ObjectProperty<Object> op1) {
        return new Binding[] {
        		Bindings.when(cond).then(op0).otherwise(op1)
        };
    }
    @SuppressWarnings("unchecked")
    @Override
    public Binding<Object>[] generatePrimitivePrimitive(Object op0, Object op1) {
        return new Binding[] {
        		Bindings.when(cond).then(op0).otherwise(op1)
        };
    }
    @Override
    public void check(Object expected, Binding<Object> binding) {
        org.junit.Assert.assertEquals(expected, binding.getValue());
    }
}
