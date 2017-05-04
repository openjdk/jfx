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

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import myapp4.pkg4.MyProps;

import static myapp4.Constants.*;

// This logic is copied from AppBindingsExported.

/**
 * Modular test application for testing JavaFX beans.
 * This is launched by ModuleLauncherTest.
 */
public class AppBindingsOpened {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            new AppBindingsOpened().doTest();
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
        MyProps root = new MyProps();
        MyProps a = new MyProps();
        MyProps b = new MyProps();

        root.setNext(a);
        a.setNext(b);
        a.setFoo(1.2);
        b.setFoo(2.3);

        DoubleBinding binding1 = Bindings.selectDouble(root, "next", "foo");
        assertEquals(1.2, binding1.get());
        a.setFoo(3.4);
        assertEquals(3.4, binding1.get());

        ObjectBinding<MyProps> binding2 = Bindings.select(root, "next", "next");
        assertEquals(2.3, binding2.get().getFoo());
        b.setFoo(4.5);
        assertEquals(4.5, binding2.get().getFoo());

        RootProps root2 = new RootProps();
        MyProps c = new MyProps();
        MyProps d = new MyProps();

        root2.setNext(c);
        c.setNext(d);
        c.setFoo(1.2);
        d.setFoo(2.3);

        DoubleBinding binding3 = Bindings.selectDouble(root2, "next", "foo");
        assertEquals(1.2, binding3.get());
        c.setFoo(3.4);
        assertEquals(3.4, binding3.get());

        ObjectBinding<MyProps> binding4 = Bindings.select(root2, "next", "next");
        assertEquals(2.3, binding4.get().getFoo());
        d.setFoo(4.5);
        assertEquals(4.5, binding4.get().getFoo());
    }
}
