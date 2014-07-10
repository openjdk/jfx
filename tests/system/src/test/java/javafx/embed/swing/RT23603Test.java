/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javafx.embed.swing;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Region;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import sun.awt.SunToolkit;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.animation.AnimationTimer;
import junit.framework.Assert;

import org.junit.Test;
import org.junit.Ignore;

/**
 * RT-23603: WebView does not display in JFXPanel on initialization
 */
@Ignore("RT-29515")
public class RT23603Test {
    volatile JFrame frame;
    final CountDownLatch l1 = new CountDownLatch(2);

    @Test
    public void test() {
        SwingUtilities.invokeLater(this::initAndShowGUI);

        //
        // wait for frame to be set visible and jfxpanel to be installed
        //
        waitForLatch(l1, 5000);

        //
        // wait for frame to become really visible
        //
        ((SunToolkit)Toolkit.getDefaultToolkit()).realSync();

        // 3 pulses should guarantee the scene is rendered
        final CountDownLatch l2 = new CountDownLatch(3);
        com.sun.javafx.tk.Toolkit.getToolkit().addSceneTkPulseListener(l2::countDown);

        //
        // wait for jfxpanel to be rendered
        //
        waitForLatch(l2, 5000);

        //
        // finally, check that jfxpanel is really visible
        //
        Robot r = null;
        try {
            r = new Robot();
        } catch (AWTException ex) {
            Assert.fail("unexpected error: couldn't create java.awt.Robot: " + ex);
        }
        Point pt = frame.getLocationOnScreen();
        Color color = r.getPixelColor(pt.x + 100, pt.y + 100);
        Assert.assertEquals(color, Color.GREEN);
    }

    private void waitForLatch(CountDownLatch latch, long ms) {
        try {
            latch.await(ms, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
        if (latch.getCount() > 0) {
            Assert.fail("unexpected error: waiting timeout " + ms + "ms elapsed for " + latch);
        }
    }

    public void initAndShowGUI() {
        frame = new JFrame("RT23603");

        final JFXPanel fxPanel = new JFXPanel();

        Platform.runLater(() -> {
            Region rgn = new Region();
            Scene scene = new Scene(rgn);
            rgn.setStyle("-fx-background-color: #00ff00;");
            fxPanel.setScene(scene);

            // start constant pulse activity
            new AnimationTimer() {
                @Override public void handle(long l) {}
            }.start();

            l1.countDown(); // jfxpanel is installed
        });

        frame.getContentPane().setBackground(java.awt.Color.RED);
        frame.getContentPane().setPreferredSize(new Dimension(400, 300));
        frame.pack();

        fxPanel.setSize(400, 300);

        frame.getContentPane().add(fxPanel);
        frame.getContentPane().remove(fxPanel);
        frame.getContentPane().add(fxPanel);
        frame.setVisible(true);

        l1.countDown(); // frame is set visible
    }

    public static void main(String[] args) {
        new RT23603Test().test();
    }
}
