/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import test.util.Util;
import test.util.memory.JMemoryBuddy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static test.util.Util.TIMEOUT;

public class Scene_dirtyNodesLeakTest {

    @BeforeAll
    public static void initFX() throws Exception {
        CountDownLatch startupLatch = new CountDownLatch(1);
        Platform.setImplicitExit(false);
        Platform.startup(startupLatch::countDown);
        Assertions.assertTrue(startupLatch.await(TIMEOUT, TimeUnit.MILLISECONDS),
                "Timeout waiting for FX runtime to start");
    }

    @Test
    public void leakDirtyNodeAndParentRemoved() throws Exception {
        JMemoryBuddy.memoryTest((checker) -> {
            CountDownLatch showingLatch = new CountDownLatch(1);
            Util.runAndWait(() -> {
                Stage stage = new Stage();
                Label label = new Label("Hello!!");
                StackPane root = new StackPane(label);
                Scene scene = new Scene(root);
                stage.setScene(scene);

                checker.setAsReferenced(scene);
                checker.assertCollectable(label);
                stage.setOnShown(l -> {
                    Platform.runLater(() -> {
                        root.getChildren().clear();
                        stage.close();
                        showingLatch.countDown();
                    });
                });
                stage.show();
            });
            try {
                Assertions.assertTrue(showingLatch.await(15, TimeUnit.SECONDS), "Timeout waiting for setOnShown");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
