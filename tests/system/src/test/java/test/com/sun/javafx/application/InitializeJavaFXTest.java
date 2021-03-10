/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class InitializeJavaFXTest {

    public static final CountDownLatch appLatch = new CountDownLatch(1);

    public static class InitializeApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            appLatch.countDown();
        }
    }

    public static void initializeApplication() throws Exception {
        new Thread(() -> {
            Application.launch(InitializeApp.class);
        }).start();
        appLatch.await(5, TimeUnit.SECONDS);
    }

    public static void initializeStartup() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(() -> {
            latch.countDown();
        });
        latch.await(5, TimeUnit.SECONDS);
    }

    @BeforeClass
    public static void initialize() throws Exception {
        System.out.println("Calling Startup!");
        initializeStartup();
        System.out.println("Called Startup!");
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            System.out.println("start called!");
        }
    }

    @Test
    public void testStartupThenLaunchInFX() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                System.out.println("Calling launch!");
                Application.launch(TestApp.class);
                System.out.println("Finished launch!");
            } catch (IllegalStateException e) {
                latch.countDown();
            } catch (Exception e) {
                System.out.println("got exception:  " + e);
                e.printStackTrace();
            }
        });
        Assert.assertTrue("Timeout", latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testStartupThenLaunch() throws Exception {
        try {
            System.out.println("Calling launch!");
            Application.launch(TestApp.class);
            System.out.println("Finished launch!");
            throw new Exception("We excpect an error!");
        } catch (IllegalStateException e) {
            System.out.println("Works!");
        } catch (Exception e) {
            throw e;
        }
    }
}
