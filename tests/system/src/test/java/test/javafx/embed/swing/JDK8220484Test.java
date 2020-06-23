/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import junit.framework.AssertionFailedError;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static test.util.Util.TIMEOUT;

public class JDK8220484Test {
    static CountDownLatch launchLatch;

    private static MyApp myApp;

    private static Timer t;
    static int cnt;

    @BeforeClass
    public static void setupOnce() {
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        System.setProperty("sun.java2d.uiScale", "125%");
        System.setProperty("glass.win.uiScale", "1.25");
        System.setProperty("glass.gtk.uiScale", "1.25");
        launchLatch = new CountDownLatch(1);

        // Start the Application
        SwingUtilities.invokeLater(() -> {
            myApp = new MyApp();
        });

        try {
            if (!launchLatch.await(5 * TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for Application to launch (" + (5 * TIMEOUT) + " seconds)");
            }
        } catch (InterruptedException ex) {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            err.initCause(ex);
            throw err;
        }
    }

    @AfterClass
    public static void teardownOnce() {
        Platform.exit();
    }

    @Test
    public void testScale() throws Exception {
        // Get the Swing-side BackBuffer
        Field fpixelsIm = JFXPanel.class.getDeclaredField("pixelsIm");
        fpixelsIm.setAccessible(true);
        BufferedImage pixelsIm = (BufferedImage) fpixelsIm.get(myApp.jfxPanel);


        assertEquals(127, pixelsIm.getWidth());
        assertEquals(127, pixelsIm.getHeight());

        // if all is ok, this area has a gray shading
        // if the buffer is off, there is a dark gray diagonal which should be the right border
        Color c = new Color(181, 181, 181);
        int colorOfDiagonal = c.getRGB();
        for (int x = 10; x < 45; x++) {
            for (int y = 90; y < 115; y++) {
                if(colorOfDiagonal == pixelsIm.getRGB( x, y )) {
                    fail( "image is skewed" );
                }
            }
        }
    }

    public static class MyApp extends JFrame {

        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private final JFXPanel jfxPanel;

        public MyApp() {
            super("JFXPanel Scaling");
            jfxPanel = new JFXPanel();
            setLayout(null);
            jfxPanel.setSize(new Dimension(100, 100));
            add(jfxPanel);
            setSize(500, 500);
            setVisible(true);

            Platform.runLater(() -> initFX(jfxPanel));

            // Give it time to paint and resize the buffers
            // the issues only appears if the buffer has been resized, not on the initial creation.
            cnt = 0;
            t = new Timer(500, (e) -> {
                switch (cnt) {
                    case 0:
                        jfxPanel.setSize(new Dimension(201, 201));
                        break;
                    case 1:
                        jfxPanel.setSize(new Dimension(101, 101));
                        break;
                    case 2:
                        t.stop();
                        launchLatch.countDown();
                        break;
                }
                cnt++;
            });
            t.start();
        }

        private static void initFX(JFXPanel fxPanel) {
            Scene scene = new Scene(new Button("Test"));
            fxPanel.setScene(scene);
        }
    }
}
