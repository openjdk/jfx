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

package com.sun.javafx.tk.quantum;

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

public class CloseWindowTest {

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

            Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    System.out.println("e = " + e);
                    exception = e;
                }
            });
        }
    }

    @BeforeClass
    public static void setup() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Application.launch(TestApp.class);
            }
        }).start();
    }

    @AfterClass
    public static void shutdown() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                primaryStage.hide();
            }
        });
    }

    @Test
    public void test1() throws Throwable {
        final CountDownLatch l = new CountDownLatch(1);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                final Stage t = new Stage();
                t.setScene(new Scene(new Group()));
                t.show();
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            t.hide();
                            t.hide();
                        } catch (Throwable z) {
                            exception = z;
                        } finally {
                            l.countDown();
                        }
                    }
                });
            }
        });
        l.await();
        if (exception != null) {
            throw exception;
        }
    }

    @Test
    public void test2() throws Throwable {
        final CountDownLatch l = new CountDownLatch(1);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                final Stage t = new Stage();
                t.setScene(new Scene(new Group()));
                t.show();
                final Stage p = new Stage();
                p.initOwner(t);
                p.setScene(new Scene(new Group()));
                p.show();
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            t.hide();
                            p.hide();
                        } catch (Throwable z) {
                            exception = z;
                        } finally {
                            l.countDown();
                        }
                    }
                });
            }
        });
        l.await();
        if (exception != null) {
            throw exception;
        }
    }

    @Test
    public void test3() throws Throwable {
        final CountDownLatch l = new CountDownLatch(1);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                final Stage t = new Stage();
                t.setScene(new Scene(new Group()));
                t.show();
                final Stage p = new Stage();
                p.initOwner(t);
                p.initModality(Modality.WINDOW_MODAL);
                p.setScene(new Scene(new Group()));
                p.show();
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            t.hide();
                            p.hide();
                        } catch (Throwable z) {
                            exception = z;
                        } finally {
                            l.countDown();
                        }
                    }
                });
            }
        });
        l.await();
        if (exception != null) {
            throw exception;
        }
    }

    @Test
    public void test4() throws Throwable {
        final CountDownLatch l = new CountDownLatch(1);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                final Stage t = new Stage();
                t.setScene(new Scene(new Group()));
                t.show();
                final Stage p = new Stage();
                p.initOwner(t);
                p.setScene(new Scene(new Group()));
                p.show();
                final Stage s = new Stage();
                s.initOwner(p);
                s.setScene(new Scene(new Group()));
                p.show();
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            t.hide();
                            s.hide();
                            p.hide();
                        } catch (Throwable z) {
                            exception = z;
                        } finally {
                            l.countDown();
                        }
                    }
                });
            }
        });
        l.await();
        if (exception != null) {
            throw exception;
        }
    }

}
