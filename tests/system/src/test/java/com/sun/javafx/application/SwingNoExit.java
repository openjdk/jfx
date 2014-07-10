/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.application;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import junit.framework.AssertionFailedError;
import util.Util;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.Assert;

/**
 * Test program for Platform.exit() behavior with an embedded JFXPanel.
 */
public class SwingNoExit {

    // Sleep time showing/hiding window in milliseconds
    private static final int SLEEP_TIME = 1000;

    private JFrame frame;
    private JFXPanel fxPanel;

    public void init() {
        // Create Swing window
        frame = new JFrame("JFXPanel 1");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Create javafx panel
        fxPanel = new JFXPanel();
        fxPanel.setPreferredSize(new Dimension(210, 180));
        frame.getContentPane().add(fxPanel, BorderLayout.CENTER);

        // Create scene and add it to the panel
        Util.runAndWait(() -> {
            Group root = new Group();
            Scene scene = new Scene(root);
            scene.setFill(Color.LIGHTYELLOW);
            fxPanel.setScene(scene);
        });

        // show frame
        frame.setLocationRelativeTo(null);
        frame.pack();
        frame.setVisible(true);
    }

    @Test
    public void doTestImplicitExit() throws Throwable {
        final AtomicReference<Throwable> error = new AtomicReference<>(null);

        final CountDownLatch initLatch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            try {
                init();
                initLatch.countDown();
            } catch (Throwable th) {
                error.set(th);
            }
        });
        if (!initLatch.await(Util.TIMEOUT, TimeUnit.MILLISECONDS)) {
            throw new AssertionFailedError("Timeout waiting for JFXPanel to launch and initialize");
        }
        Throwable t = error.get();
        if (t != null) {
            throw t;
        }

        final CountDownLatch runAndWait = new CountDownLatch(1);
        Platform.runLater(() -> {
            Platform.exit();
            runAndWait.countDown();
        });
        if (!runAndWait.await(Util.TIMEOUT, TimeUnit.MILLISECONDS)) {
            throw new AssertionFailedError("Timeout waiting for Platform.exit()");
        }

        final CountDownLatch exitLatch = PlatformImpl.test_getPlatformExitLatch();
        Thread.sleep(SLEEP_TIME);
        // Platform.exit() should not cause FX to exit, while JFXPanel is alive
        Assert.assertEquals("Platform.exit() caused FX to exit, while JFXPanel is alive",
                            1, exitLatch.getCount());

        try {
            SwingUtilities.invokeAndWait(() -> {
                frame.setVisible(false);
                frame.dispose();
            });
        }
        catch (InvocationTargetException ex) {
            throw new AssertionFailedError("Exception while disposing JFrame");
        }

        Thread.sleep(SLEEP_TIME);
        // JFXPanel is gone, implicit exit is false, so FX should have exited now
        Assert.assertEquals("FX is not exited, when the last JFXPanel is disposed",
                            0, exitLatch.getCount());
    }
}
