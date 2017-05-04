/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package myapp4;

import javafx.beans.property.adapter.JavaBeanDoubleProperty;
import javafx.beans.property.adapter.JavaBeanDoublePropertyBuilder;
import javafx.beans.property.adapter.JavaBeanObjectProperty;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.beans.property.adapter.ReadOnlyJavaBeanStringProperty;
import javafx.beans.property.adapter.ReadOnlyJavaBeanStringPropertyBuilder;
import myapp4.pkg2.POJO;
import myapp4.pkg2.RefClass;

import static myapp4.Constants.*;

/**
 * Modular test application for testing JavaFX beans.
 * This is launched by ModuleLauncherTest.
 */
public class AppBeansExported {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            new AppBeansExported().doTest();
            System.exit(ERROR_NONE);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            System.exit(ERROR_ASSERTION_FAILURE);
        }
    }

    private final double EPSILON = 1.0e-4;

    private void assertEquals(double expected, double observed) {
        if (Math.abs(expected - observed) > EPSILON) {
            throw new AssertionError("expected:<" + expected + "> but was:<" + observed + ">");
        }
    }

    private void assertEquals(String expected, String observed) {
        if (!expected.equals(observed)) {
            throw new AssertionError("expected:<" + expected + "> but was:<" + observed + ">");
        }
    }

    private void assertSame(Object expected, Object observed) {
        if (expected != observed) {
            throw new AssertionError("expected:<" + expected + "> but was:<" + observed + ">");
        }
    }

    public void doTest() throws Exception {
        String name = "test object";
        double val = 1.2;
        RefClass obj = new RefClass();

        POJO bean = new POJO(name, val, obj);

        JavaBeanDoubleProperty valProp = JavaBeanDoublePropertyBuilder.create()
                .bean(bean)
                .name("val")
                .build();

        double retVal = valProp.get();
        assertEquals(val, retVal);

        val = 2.5;
        valProp.set(val);
        retVal = valProp.get();
        assertEquals(val, retVal);

        JavaBeanObjectProperty<RefClass> objProp = JavaBeanObjectPropertyBuilder.create()
                .bean(bean)
                .name("obj")
                .build();

        RefClass retObj = objProp.get();
        assertSame(obj, retObj);

        obj = new RefClass();
        objProp.set(obj);
        retObj = objProp.get();
        assertSame(obj, retObj);

        ReadOnlyJavaBeanStringProperty namePropRO = ReadOnlyJavaBeanStringPropertyBuilder.create()
                .bean(bean)
                .name("name")
                .build();

        String retName = namePropRO.get();
        assertEquals(name, retName);
    }

}
