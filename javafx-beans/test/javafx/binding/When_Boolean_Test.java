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

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class When_Boolean_Test extends WhenTestBase<Boolean, BooleanProperty> {
    public When_Boolean_Test() {
        super(
            false, true, true, false,
            new SimpleBooleanProperty(), new SimpleBooleanProperty()
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public Binding<Boolean>[] generatePropertyPropertyList(BooleanProperty p0, BooleanProperty[] probs) {
        return new Binding[] {
        		Bindings.when(cond).then(p0).otherwise(probs[0])
        };
    }

    @Override
    public Binding<Boolean> generatePropertyProperty(BooleanProperty op0, BooleanProperty op1) {
        return Bindings.when(cond).then(op0).otherwise(op1);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Binding<Boolean>[] generatePropertyPrimitive(BooleanProperty op0, Boolean op1) {
        return new Binding[] {
        		Bindings.when(cond).then(op0).otherwise(op1)
        };
    }
    @SuppressWarnings("unchecked")
    @Override
    public Binding<Boolean>[] generatePrimitiveProperty(Boolean op0, BooleanProperty op1) {
        return new Binding[] {
        		Bindings.when(cond).then(op0.booleanValue()).otherwise(op1)
        };
    }
    @SuppressWarnings("unchecked")
    @Override
    public Binding<Boolean>[] generatePrimitivePrimitive(Boolean op0, Boolean op1) {
        return new Binding[] {
        		Bindings.when(cond).then(op0.booleanValue()).otherwise(op1)
        };
    }
    @Override
    public void check(Boolean expected, Binding<Boolean> binding) {
        org.junit.Assert.assertEquals(expected, binding.getValue());
    }
}
