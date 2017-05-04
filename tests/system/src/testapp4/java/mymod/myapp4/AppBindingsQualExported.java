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

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import myapp4.pkg3.MyProps;

import static myapp4.Constants.*;

// This logic is copied from AppBindingsUnexported.

/**
 * Modular test application for testing JavaFX beans.
 * This is launched by ModuleLauncherTest.
 */
public class AppBindingsQualExported {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            new AppBindingsQualExported().doTest();
            System.exit(ERROR_NONE);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            System.exit(ERROR_ASSERTION_FAILURE);
        }
    }

    private void checkException(RuntimeException ex) {
        Throwable cause = ex.getCause();
        if (! (cause instanceof IllegalAccessException)) {
            System.err.println("ERROR: unexpected cause: " + cause);
            throw ex;
        }

        String message = cause.getMessage();
        if (message == null) {
            System.err.println("ERROR: detail message of cause is null");
            throw ex;
        }

        boolean badMessage = false;
        if (!message.contains(" cannot access class ")) badMessage = true;
        if (!message.contains(" does not open ")) badMessage = true;
        if (!message.endsWith(" to javafx.base")) badMessage = true;
        if (badMessage) {
            System.err.println("ERROR: detail message not formatted correctly: " + message);
            throw ex;
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

    private Logger logger;
    private Handler logHandler;
    private final List<Throwable> errs = new ArrayList<>();

    private void initLogger() {
        Locale.setDefault(Locale.US);

        // Initialize Logger
        logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                final Throwable t = record.getThrown();
                // detect any Throwable:
                if (t != null) {
                    errs.add(t);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        logger = Logger.getLogger("javafx.beans");
        logger.addHandler(logHandler);
    }

    public void doTest() throws Exception {
        initLogger();

        MyProps root = new MyProps();
        MyProps a = new MyProps();
        MyProps b = new MyProps();

        root.setNext(a);
        a.setNext(b);
        a.setFoo(1.2);
        b.setFoo(2.3);

        try {
            Bindings.selectDouble(root, "next", "foo");
            throw new AssertionError("ERROR: did not get the expected exception");
        } catch (UndeclaredThrowableException ex) {
            checkException(ex);
        }

        try {
            Bindings.select(root, "next", "next");
            throw new AssertionError("ERROR: did not get the expected exception");
        } catch (UndeclaredThrowableException ex) {
            checkException(ex);
        }

        RootProps root2 = new RootProps();
        MyProps c = new MyProps();
        MyProps d = new MyProps();

        root2.setNext(c);
        c.setNext(d);
        c.setFoo(1.2);
        d.setFoo(2.3);

        // In this case, the binding will succeed; calling get() will return 0;
        // the first time it is called it will log a warning.
        DoubleBinding binding3 = Bindings.selectDouble(root2, "next", "foo");
        assertEquals(0, binding3.get()); // This will log a warning
        c.setFoo(3.4);
        assertEquals(0, binding3.get()); // No warning here

        // In this case, the binding will succeed; calling get() will return null;
        // the first time it is called it will log a warning.
        ObjectBinding<MyProps> binding4 = Bindings.select(root2, "next", "next");
        assertSame(null, binding4.get()); // This will log a warning
        assertSame(null, binding4.get()); // No warning here

        // Assert that we got 2 warnings
        final int expectedExceptions = 2; // First call to get for each binding

        if (errs.isEmpty()) {
            throw new AssertionError("ERROR: did not get the expected exception");
        }

        assertEquals(expectedExceptions, errs.size());

        for (Throwable t : errs) {
            if (!(t instanceof RuntimeException)) {
                throw new AssertionError("ERROR: unexpected exception: ", t);
            }
            checkException((RuntimeException) t);
        }
    }

}
