/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import javafx.application.Application;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.javafx.PlatformUtil;

import test.util.Util;

public class SwingNodeContentMemoryLeakTest {

    static CountDownLatch launchLatch = new CountDownLatch(1);

    //Keep week references to all panels that we've ever generated to see if any
    //of them get collected.
    private Collection<WeakReference<JPanel>> panels = new CopyOnWriteArrayList<>();
    private int count = 0;
    private int fail = 0;
    private SwingNode node;
    private long panelCount;

    @BeforeClass
    public static void setupOnce() {
        Util.launch(launchLatch, 50, MyApp.class);
    }

    @AfterClass
    public static void teardownOnce() {
        Util.shutdown();
    }

    @Test
    public void testSwingNodeContentMemoryLeak() throws InterruptedException,
                                                        InvocationTargetException {
        Util.runAndWait(() -> {
            node = new SwingNode();
            Pane root = new Pane();
            root.getChildren().add(node);

            Stage stage = new Stage();
            Scene scene = new Scene(root, 150, 100);
            stage.setScene(scene);
        });

        //Kick off a thread that repeatedly creates new JPanels and resets the swing node's content
        new Thread(() -> {
            while(count < 50) {
                //Lets throw in a little sleep so we can read the output
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                SwingUtilities.invokeLater(() -> {
                    JPanel panel = new JPanel();
                    panels.add(new WeakReference<>(panel));
                    node.setContent(panel);
                });

                panelCount = panels.stream().filter(ref ->
                                                     ref.get() != null).count();
                System.out.println("iteration " + count + " Panels in memory: " + panelCount);

                //I know this doesn't guarantee anything, but prompting a GC gives me more confidence that this
                //truly is a bug.
                System.gc();
                count++;
            }
            // Check if panelCount has not increased beyond certain threshold
            assertFalse(panelCount > count/2);

        }).start();

        // Invoke a noop on EDT thread and wait for a bit to make sure EDT processed node objects
        SwingUtilities.invokeAndWait(() -> {});
        Util.sleep(5000);
    }

    public static class MyApp extends Application {
        @Override
        public void start(Stage stage) throws Exception {
            launchLatch.countDown();
        }
    }
}
