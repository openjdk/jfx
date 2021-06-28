/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Assert;
import org.junit.Test;
import test.util.Util;

public class NonFocusableJFXPanelTest {
    private static Robot robot;
    private static JFrame frame;
    private static int clickCount = 0;
    private static final int WIDTH = 600;
    private static final int HEIGHT = 200;

    @BeforeClass
    public static void init() throws Exception {
        robot = new Robot();
        robot.setAutoDelay(100);
    }

    @Test
    public void testJFXPanelFocus() throws Exception {
        SwingUtilities.invokeAndWait(this::initAndShowGUI);

        Util.sleep(1000);

        Point pt = frame.getLocationOnScreen();
        robot.mouseMove(pt.x + WIDTH/2, pt.y + HEIGHT/2);
        robot.waitForIdle();
        for (int i = 0; i < 5; i++) {
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        }
        Assert.assertFalse("Extra MouseEvent generated", clickCount > 5);
    }

    private void waitForLatch(CountDownLatch latch, long ms) throws Exception {
        Assert.assertTrue(String.format(
            "unexpected error: waiting timeout %d ms elapsed for",
            ms),
            latch.await(ms, TimeUnit.MILLISECONDS));
    }

    public void initAndShowGUI() {

        final CountDownLatch latch = new CountDownLatch(1);

        frame = new JFrame("NonFocusableJFXPanel");

        final JFXPanel fxPanel = new JFXPanel();

        // With Java 10, works fine with .setFocusable(true).
        fxPanel.setFocusable(false);

        Platform.runLater(() -> {
            Button button = new Button("Click me");
            button.setOnMousePressed(e -> {
                ++clickCount;
            });
            Scene scene = new Scene(button);
            fxPanel.setScene(scene);

            latch.countDown(); // jfxpanel is installed
        });

        frame.setSize(WIDTH, HEIGHT);
        frame.getContentPane().add(fxPanel, BorderLayout.CENTER);
        frame.setVisible(true);

        try {
            waitForLatch(latch, 5000);
        } catch (Exception e) {
            Assert.fail("Exception while waiting for latch");
        }
    }


    @AfterClass
    public static void teardown() throws Exception {
        Assert.assertNotNull(frame);
        SwingUtilities.invokeLater(frame::dispose);
    }
}

