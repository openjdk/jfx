/*
 * Copyright (c) 2017, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.embed.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javafx.application.Application;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;
import test.util.memory.JMemoryBuddy;

public class SwingNodeMemoryLeakTest {

    final static int TOTAL_SWINGNODE = 10;
    static CountDownLatch launchLatch = new CountDownLatch(1);
    final static int GC_ATTEMPTS = 10;
    ArrayList<WeakReference<SwingNode>> weakRefArrSN =
                                      new ArrayList(TOTAL_SWINGNODE);
    ArrayList<WeakReference<JLabel>> weakRefArrJL =
                                      new ArrayList(TOTAL_SWINGNODE);

    @BeforeAll
    public static void setupOnce() {
        Util.launch(launchLatch, 50, MyApp.class);
    }

    @AfterAll
    public static void teardownOnce() {
        Util.shutdown();
    }

    @Test
    public void testSwingNodeMemoryLeak() throws InterruptedException, InvocationTargetException {
        Util.runAndWait(() -> {
            testSwingNodeObjectsInStage();
        });

        // Invoke a noop on EDT thread and wait for a bit to make sure EDT processed node objects
        SwingUtilities.invokeAndWait(() -> {});
        Util.sleep(500);

        JMemoryBuddy.assertCollectable(weakRefArrSN);
        JMemoryBuddy.assertCollectable(weakRefArrJL);
    }

    private void testSwingNodeObjectsInStage() {

        Stage tempStage[] = new Stage[TOTAL_SWINGNODE];

        for (int i = 0; i < TOTAL_SWINGNODE; i++) {
            BorderPane root = new BorderPane();
            SwingNode sw = new SwingNode();
            JLabel label = new JLabel("SWING");
            sw.setContent(label);

            weakRefArrSN.add(i, new WeakReference<SwingNode>(sw));
            weakRefArrJL.add(i, new WeakReference<JLabel>(label));

            root.centerProperty().set(sw);

            Stage stage = new Stage();
            Scene scene = new Scene(root, 150, 100);
            stage.setScene(scene);

            tempStage[i] = stage;
        }
        if (TOTAL_SWINGNODE != weakRefArrSN.size()) {
            System.out.println("TOTAL_SWINGNODE != weakRefArr.size()");
        }
        assertEquals(0, getCleanedUpSwingNodeCount());
        assertEquals(0, getCleanedUpJLabelCount());
        assertEquals(TOTAL_SWINGNODE, weakRefArrSN.size());
        assertEquals(TOTAL_SWINGNODE, weakRefArrJL.size());

        for (int i = 0; i < TOTAL_SWINGNODE; i++) {
            if (tempStage[i] != null) {
                tempStage[i].close();
                tempStage[i] = null;
            }
        }
    }

    private int getCleanedUpSwingNodeCount() {
        int count = 0;
        for (WeakReference<SwingNode> ref : weakRefArrSN) {
            if (ref.get() == null) {
                count++;
            }
        }
        return count;
    }

    private int getCleanedUpJLabelCount() {
        int count = 0;
        for (WeakReference<JLabel> ref : weakRefArrJL) {
            if (ref.get() == null) {
                count++;
            }
        }
        return count;
    }

    public static class MyApp extends Application {
        @Override
        public void start(Stage stage) throws Exception {
            launchLatch.countDown();
        }
    }

}
