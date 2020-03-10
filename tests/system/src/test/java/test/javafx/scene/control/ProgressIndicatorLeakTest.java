/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.skin.ProgressIndicatorSkin;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ProgressIndicatorLeakTest {

    static CountDownLatch startupLatch;
    static WeakReference<Node> detIndicator = null;
    static Stage stage = null;

    public static class TestApp extends Application {

        @Override
        public void start(Stage primaryStage) throws Exception {
            ProgressIndicator indicator = new ProgressIndicator(-1);
            indicator.setSkin(new ProgressIndicatorSkin(indicator));
            Scene scene = new Scene(indicator);
            primaryStage.setScene(scene);
            stage = primaryStage;
            indicator.setProgress(1.0);
            Assert.assertEquals("size is wrong", 1, indicator.getChildrenUnmodifiable().size());
            detIndicator = new WeakReference<Node>(indicator.getChildrenUnmodifiable().get(0));
            indicator.setProgress(-1.0);
            indicator.setProgress(1.0);

            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> {
                Platform.runLater(() -> {
                    startupLatch.countDown();
                });
            });
            primaryStage.show();
        }
    }

    @BeforeClass
    public static void initFX() throws Exception {
        startupLatch = new CountDownLatch(1);
        new Thread(() -> Application.launch(TestApp.class, (String[]) null)).start();

        Assert.assertTrue("Timeout waiting for FX runtime to start", startupLatch.await(15, TimeUnit.SECONDS));
    }


    @Test
    public void memoryTest() throws Exception {
        assertCollectable(detIndicator);
    }

    public static void assertCollectable(WeakReference weakReference) throws Exception {
        int counter = 0;

        createGarbage();
        System.gc();
        System.runFinalization();

        while (counter < 10 && weakReference.get() != null) {
            Thread.sleep(100);
            counter = counter + 1;
            createGarbage();
            System.gc();
            System.runFinalization();
        }

        if (weakReference.get() != null) {
            throw new AssertionError("Content of WeakReference was not collected. content: " + weakReference.get());
        }
    }
    public static void createGarbage() {
        LinkedList list = new LinkedList<Integer>();
        int counter = 0;
        while (counter < 999999) {
            counter += 1;
            list.add(1);
        }
    }

    @AfterClass
    public static void teardownOnce() {
        Platform.runLater(() -> {
            stage.hide();
            Platform.exit();
        });
    }

}
