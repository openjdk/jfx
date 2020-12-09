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

package test.robot.javafx.embed.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sun.javafx.PlatformUtil;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.JFXPanelShim;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import static test.util.Util.TIMEOUT;

public class JFXPanelHiDPITest {

    static CountDownLatch launchLatch;

    private static Timer t;
    private static MyApp myApp;

    // Any scale value greater than 1.0 can be used to run this test.
    // Since this value is applied via system properties in setupOnce()
    // the test can be run only once.
    // If needed, change it manually and run again.
    private static final double SCALE = 1.25; // 1.25, 1.5, 1.75
    private static final int PANEL_WIDTH = 500;
    private static final int PANEL_HEIGHT = 400;

    @BeforeClass
    public static void setupOnce() throws Exception {
        assumeTrue(PlatformUtil.isWindows());
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        System.setProperty("sun.java2d.uiScale", String.format("%f%%", SCALE * 100));
        System.setProperty("glass.win.uiScale", String.valueOf(SCALE));
        launchLatch = new CountDownLatch(1);

        // Start the Application
        SwingUtilities.invokeLater(() -> myApp = new MyApp());

        assertTrue("Timeout waiting for Application to launch",
                launchLatch.await(5 * TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testScale() throws Exception {
        // Get the Swing-side BackBuffer
        BufferedImage pixelsIm = JFXPanelShim.getPixelsIm(myApp.fxPanel);
        assertEquals((int) Math.ceil(PANEL_WIDTH * SCALE), pixelsIm.getWidth());
        assertEquals((int) Math.ceil(PANEL_HEIGHT * SCALE), pixelsIm.getHeight());

        int bgColor = new java.awt.Color(244, 244, 244).getRGB();
        for (int i = 10; i < PANEL_HEIGHT - 10; i++) {
            int color = pixelsIm.getRGB(10, i);
            if (color != bgColor) {
                fail("color index at 10, " + i + " was " + color + ", but should be "  + bgColor);
            }
        }
    }

    @AfterClass
    public static void teardownOnce() {
        if (myApp != null) {
            SwingUtilities.invokeLater(myApp::dispose);
        }
    }

    public static class MyApp extends JFrame {

        private final JFXPanel fxPanel;

        public MyApp() {
            super("JFXPanel with Label");
            fxPanel = new JFXPanel();

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout());

            JPanel swingPanel = new JPanel();
            JLabel swingLabel1 = new JLabel("This is a Swing JLabel");
            JLabel swingLabel2 = new JLabel("A JavaFX label is drawn on top of a yellow rectangle in the center of the JFrame");
            swingPanel.setLayout(new BoxLayout(swingPanel, BoxLayout.Y_AXIS));
            swingPanel.add(swingLabel1);
            swingPanel.add(swingLabel2);
            add(swingPanel, BorderLayout.NORTH);

            // Create javafx panel
            fxPanel.setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
            add(fxPanel, BorderLayout.CENTER);

            // create JavaFX scene
            createScene(fxPanel);

            // show frame
            pack();
            setVisible(true);

            t = new Timer(1000, e -> {
                t.stop();
                launchLatch.countDown();
            });
            t.start();
        }

        private void createScene(final JFXPanel fxPanel) {
            Platform.runLater(() -> {
                StackPane root = new StackPane();

                Rectangle rect = new Rectangle(PANEL_WIDTH - 100, (double) PANEL_HEIGHT / 8);
                rect.setFill(Color.YELLOW);
                Label label = new Label("This is a JavaFX Label");
                label.setStyle("-fx-font-size: 24");

                root.getChildren().addAll(rect, label);

                final Scene scene = new Scene(root);

                // Add scene to JFXPanel
                fxPanel.setScene(scene);
            });
        }
    }

}
