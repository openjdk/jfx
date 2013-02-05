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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class When_String_Test extends WhenTestBase<String, StringProperty> {
    public When_String_Test() {
        super(
                null, "Hello", "Hello World", "",
                new SimpleStringProperty(), new SimpleStringProperty()
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public Binding<String>[] generatePropertyPropertyList(StringProperty p0, StringProperty[] probs) {
        return new Binding[] {
        		Bindings.when(cond).then(p0).otherwise(probs[0])
        };
    }

    @Override
    public Binding<String> generatePropertyProperty(StringProperty op0, StringProperty op1) {
        return Bindings.when(cond).then(op0).otherwise(op1);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Binding<String>[] generatePropertyPrimitive(StringProperty op0, String op1) {
        return new Binding[] {
        		Bindings.when(cond).then(op0).otherwise(op1)
        };
    }
    @SuppressWarnings("unchecked")
    @Override
    public Binding<String>[] generatePrimitiveProperty(String op0, StringProperty op1) {
        return new Binding[] {
        		Bindings.when(cond).then(op0).otherwise(op1)
        };
    }
    @SuppressWarnings("unchecked")
    @Override
    public Binding<String>[] generatePrimitivePrimitive(String op0, String op1) {
        return new Binding[] {
        		Bindings.when(cond).then(op0).otherwise(op1)
        };
    }
    @Override
    public void check(String expected, Binding<String> binding) {
        org.junit.Assert.assertEquals(expected, binding.getValue());
    }
}
