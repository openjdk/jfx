/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.tk.quantum;

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class WindowSceneInitDisposeTest {

    private static final CountDownLatch startupLatch = new CountDownLatch(1);

    private static Stage primaryStage;

    private static volatile Throwable exception;

    public static class TestApp extends Application {
        @Override
        public void start(Stage t) {
            primaryStage = t;
            t.setScene(new Scene(new Group()));
            t.setWidth(100);
            t.setHeight(100);
            t.show();

            Thread.currentThread().setUncaughtExceptionHandler((t2, e) -> {
                System.err.println("Exception caught in thread: " + t2);
                e.printStackTrace();
                exception = e;
            });

            startupLatch.countDown();
        }
    }

    @BeforeClass
    public static void setup() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void shutdown() {
        Util.shutdown(primaryStage);
    }

    @Test
    public void test1() throws Throwable {
        final CountDownLatch l = new CountDownLatch(1);
        Platform.runLater(() -> {
            final Stage t = new Stage();
            t.setScene(new Scene(new Group()));
            t.show();
            Platform.runLater(() -> {
                try {
                    t.hide();
                } finally {
                    l.countDown();
                }
            });
        });
        l.await();
        if (exception != null) {
            throw exception;
        }
    }

    @Test
    public void test2() throws Throwable {
        final CountDownLatch l = new CountDownLatch(1);
        Platform.runLater(() -> {
            final Stage t = new Stage();
            t.show();
            t.setScene(new Scene(new Group()));
            Platform.runLater(() -> {
                try {
                    t.hide();
                } finally {
                    l.countDown();
                }
            });
        });
        l.await();
        if (exception != null) {
            throw exception;
        }
    }

    @Test
    public void test3() throws Throwable {
        final CountDownLatch l = new CountDownLatch(1);
        Platform.runLater(() -> {
            final Stage t = new Stage();
            t.show();
            Platform.runLater(() -> {
                try {
                    t.hide();
                } finally {
                    l.countDown();
                }
            });
        });
        l.await();
        if (exception != null) {
            throw exception;
        }
    }

    @Test
    public void test4() throws Throwable {
        final CountDownLatch l = new CountDownLatch(1);
        Platform.runLater(() -> {
            final Stage t = new Stage();
            t.setScene(new Scene(new Group()));
            t.show();
            Platform.runLater(() -> {
                try {
                    t.setScene(null);
                    t.hide();
                } finally {
                    l.countDown();
                }
            });
        });
        l.await();
        if (exception != null) {
            throw exception;
        }
    }

    @Test
    public void test5() throws Throwable {
        final CountDownLatch l = new CountDownLatch(1);
        Platform.runLater(() -> {
            final Stage t = new Stage();
            t.setScene(new Scene(new Group()));
            t.show();
            Platform.runLater(() -> {
                try {
                    t.hide();
                    t.setScene(null);
                } finally {
                    l.countDown();
                }
            });
        });
        l.await();
        if (exception != null) {
            throw exception;
        }
    }

    @Test
    public void test6() throws Throwable {
        final CountDownLatch l = new CountDownLatch(1);
        Platform.runLater(() -> {
            final Stage t = new Stage();
            t.setScene(new Scene(new Group()));
            t.show();
            Platform.runLater(() -> {
                try {
                    t.setScene(new Scene(new Group()));
                    t.hide();
                } finally {
                    l.countDown();
                }
            });
        });
        l.await();
        if (exception != null) {
            throw exception;
        }
    }

    @Test
    public void test7() throws Throwable {
        final CountDownLatch l = new CountDownLatch(1);
        Platform.runLater(() -> {
            final Stage t = new Stage();
            final Scene s = new Scene(new Group());
            t.setScene(s);
            t.show();
            final Stage p = new Stage();
            p.show();
            Platform.runLater(() -> {
                try {
                    p.setScene(s);
                    p.hide();
                    t.hide();
                } finally {
                    l.countDown();
                }
            });
        });
        l.await();
        if (exception != null) {
            throw exception;
        }
    }

}
