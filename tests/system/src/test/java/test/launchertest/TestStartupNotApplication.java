/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package test.launchertest;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import javafx.application.Platform;

import static test.launchertest.Constants.*;

/**
 * Test Platform.startup from class that is not an Application.
 * This is launched by MainLauncherTest.
 */
public class TestStartupNotApplication {

    private static void assertEquals(String expected, String actual) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        System.err.println("Assertion failed: expected (" + expected + ") != actual (" + actual + ")");
        System.exit(ERROR_ASSERTION_FAILURE);
    }

    public static void main(String[] args) {
        try {
            Platform.runLater(() -> {
                // do nothing
            });
            System.exit(ERROR_TOOLKIT_IS_RUNNING);
        } catch (IllegalStateException ex) {
            // OK
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }

        final Semaphore sem = new Semaphore(0);
        final ArrayList<String> list = new ArrayList<>();
        final String keyStartup = "Startup runnable";
        final String keyRunLater0 = "runLater #0";
        final String keyRunLater1 = "runLater #1";
        try {
            Platform.startup(() -> {
                list.add(keyStartup);
                sem.release();
            });
            Platform.runLater(() -> {
                list.add(keyRunLater0);
                sem.release();
            });
            sem.acquire(2);
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            System.exit(ERROR_STARTUP_FAILED);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }

        Platform.runLater(() -> {
            list.add(keyRunLater1);
            sem.release();
        });
        try {
            sem.acquire();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
        assertEquals(keyStartup, list.get(0));
        assertEquals(keyRunLater0, list.get(1));
        assertEquals(keyRunLater1, list.get(2));

        System.exit(ERROR_NONE);
    }

}
