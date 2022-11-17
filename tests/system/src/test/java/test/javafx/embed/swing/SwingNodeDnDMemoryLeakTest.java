/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;

import java.awt.dnd.DropTarget;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import javax.swing.JLabel;

import javafx.application.Application;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class SwingNodeDnDMemoryLeakTest {

    final static int TOTAL_SWINGNODE = 10;
    static CountDownLatch launchLatch = new CountDownLatch(1);
    final static int GC_ATTEMPTS = 10;
    ArrayList<WeakReference<SwingNode>> weakRefArrSN =
                                      new ArrayList(TOTAL_SWINGNODE);

    @BeforeClass
    public static void setupOnce() throws Exception {
        Util.launch(launchLatch, MyApp.class);
    }

    @AfterClass
    public static void teardownOnce() {
        Util.shutdown();
    }

    @Test
    public void testSwingNodeMemoryLeak() {
        Util.runAndWait(() -> {
            testSwingNodeObjectsInStage();
        });
        attemptGCSwingNode();
        assertEquals(TOTAL_SWINGNODE, getCleanedUpSwingNodeCount());
    }

    private void testSwingNodeObjectsInStage() {

        Stage tempStage[] = new Stage[TOTAL_SWINGNODE];

        for (int i = 0; i < TOTAL_SWINGNODE; i++) {
            BorderPane root = new BorderPane();
            SwingNode sw = new SwingNode();
            JLabel label = new JLabel("SWING");

            //Need to set Drop Target to show the leak
            label.setDropTarget(new DropTarget());

            sw.setContent(label);

            WeakReference<SwingNode> ref = new WeakReference<SwingNode>(sw);
            weakRefArrSN.add(i, ref);

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
        assertEquals(TOTAL_SWINGNODE, weakRefArrSN.size());

        for (int i = 0; i < TOTAL_SWINGNODE; i++) {
            if (tempStage[i] != null) {
                tempStage[i].close();
                tempStage[i] = null;
            }
        }
    }

    private void attemptGCSwingNode() {
        // Attempt gc GC_ATTEMPTS times
        for (int i = 0; i < GC_ATTEMPTS; i++) {
            System.gc();
            if (getCleanedUpSwingNodeCount() == TOTAL_SWINGNODE) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.err.println("InterruptedException occurred during Thread.sleep()");
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

    public static class MyApp extends Application {
        @Override
        public void start(Stage stage) throws Exception {
            launchLatch.countDown();
        }
    }

}
