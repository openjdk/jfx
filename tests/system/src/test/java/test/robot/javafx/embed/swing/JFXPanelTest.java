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
import org.junit.Assume;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import javafx.embed.swing.JFXPanel;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import java.awt.Robot;
import java.awt.event.InputEvent;
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
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
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

    @AfterClass
    public static void teardown() throws Exception {
        stop = true;
        if (frame != null) {
            SwingUtilities.invokeLater(frame::dispose);
        }
    }
}

