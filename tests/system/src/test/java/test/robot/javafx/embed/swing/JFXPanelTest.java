/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package test.robot.javafx.embed.swing;

import com.sun.javafx.PlatformUtil;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import org.junit.Assume;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import javafx.embed.swing.JFXPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class JFXPanelTest {
    private static Robot robot;
    private static JFrame frame;
    private static volatile boolean stop;

    public static void main(String[] args) throws Exception {
        init();
        try {
            new JFXPanelTest().testJFXPanelNew();
            teardown();
        } catch (Throwable th) {
            th.printStackTrace();
            System.exit(1);
        } finally {
            System.exit(0);
        }
    }

    @BeforeClass
    public static void init() throws Exception {
        Assume.assumeTrue(PlatformUtil.isMac());
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        robot = new Robot();
        robot.waitForIdle();
        robot.setAutoDelay(10);
        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame("JFXPanel init test");
            JMenuBar menubar = new JMenuBar();
            JMenu menu = new JMenu("te-e-e-e-e-e-e-e-e-e-e-e-e-e-e-e-e-e-e-e-e-e-e-e-e-e-e-e-e-e-e-e-st");
            menu.add(new JMenuItem("1"));
            menubar.add(menu);
            frame.setJMenuBar(menubar);
            frame.setSize(200, 200);
            frame.setVisible(true);
        });
        robot.waitForIdle();
    }

    @Test
    public void testJFXPanelNew() throws Exception {
        CountDownLatch beginLatch = new CountDownLatch(1);
        new Thread(() -> {
            try {
                beginLatch.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
            while (!stop) {
                robot.mouseMove(300, 10);
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
            }
        }).start();
        beginLatch.countDown();
        CountDownLatch endLatch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            new JFXPanel();
            stop = true;
            endLatch.countDown();
        });
        endLatch.await(5, TimeUnit.SECONDS);
        Assert.assertTrue("It seems FX initialization is deadlocked", stop);
    }

    class TestFXPanel extends JFXPanel {
        protected void processMouseEventPublic(MouseEvent e) {
            processMouseEvent(e);
        }
    };

    @Test
    public void testNoDoubleClickOnFirstClick() throws Exception {

        CountDownLatch firstPressedEventLatch = new CountDownLatch(1);

        // It's an array, so we can mutate it inside of lambda statement
        int[] pressedEventCounter = {0};

        SwingUtilities.invokeLater(() -> {
            TestFXPanel fxPnl = new TestFXPanel();
            fxPnl.setPreferredSize(new Dimension(100, 100));
            JFrame jframe = new JFrame();
            JPanel jpanel = new JPanel();
            jpanel.add(fxPnl);
            jframe.setContentPane(jpanel);
            jframe.pack();
            jframe.setVisible(true);

            Platform.runLater(() -> {
                Group grp = new Group();
                Scene scene = new Scene(new Group());
                scene.getRoot().requestFocus();

                scene.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_PRESSED, (event -> {
                    pressedEventCounter[0] += 1;
                    firstPressedEventLatch.countDown();
                }));

                fxPnl.setScene(scene);

                SwingUtilities.invokeLater(() -> {
                    MouseEvent e = new MouseEvent(fxPnl, MouseEvent.MOUSE_PRESSED, 0, MouseEvent.BUTTON1_DOWN_MASK,
                            5, 5, 1, false, MouseEvent.BUTTON1);

                    fxPnl.processMouseEventPublic(e);
                });
            });
        });

        if(!firstPressedEventLatch.await(5000, TimeUnit.MILLISECONDS)) {
            throw new Exception();
        };

        Thread.sleep(500); // there should be no pressed event after the initial one. Let's wait for 0.5s and check again.

        Assert.assertEquals(1, pressedEventCounter[0]);
    }


    @AfterClass
    public static void teardown() throws Exception {
        stop = true;
        if (frame != null) {
            SwingUtilities.invokeLater(frame::dispose);
        }
    }
}

