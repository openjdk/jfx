/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ExceptionHandlerTest {

    private static final CountDownLatch startupLatch = new CountDownLatch(1);

    private static volatile Throwable exception;

    public static class TestApp extends Application {
        @Override
        public void start(Stage t) {
            Thread.currentThread().setUncaughtExceptionHandler((t2, e) -> {
                exception = e;
                System.out.println("Exception caught: " + e);
                System.out.flush();
            });
            startupLatch.countDown();
        }
    }

    private class TestException extends RuntimeException {
        public TestException(String msg) {
            super(msg);
        }
    }

    @BeforeClass
    public static void setup() throws Exception {
        new Thread(() -> Application.launch(TestApp.class)).start();
        startupLatch.await();
    }

    @Test
    public void test1() throws Throwable {
        exception = null;
        final CountDownLatch l = new CountDownLatch(1);
        Platform.runLater(() -> {
            throw new TestException("test1");
        });
        Platform.runLater(l::countDown);
        l.await(10000, TimeUnit.MILLISECONDS);
        if (exception == null) {
            throw new RuntimeException("Test FAILED: TestException is not caught");
        }
        if (!(exception instanceof TestException)) {
            throw new RuntimeException("Test FAILED: unexpected exception is caught: " + exception);
        }
    }

}
