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

import java.awt.BorderLayout;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javax.swing.JFrame;
import junit.framework.AssertionFailedError;
import util.Util;

import static org.junit.Assert.*;
import static util.Util.TIMEOUT;

/**
 * Test program for Platform implicit exit behavior using an embedded JFXPanel.
 * Each of the tests must be run in a separate JVM which is why each
 * is in its own subclass.
 */
public class SwingExitCommon {

    // Sleep time showing/hiding window in milliseconds
    private static final int SLEEP_TIME = 1000;

    // Used to launch the application before running any test
    private static final CountDownLatch initialized = new CountDownLatch(1);

    // Value of the implicit exit flag for the given test
    private static volatile boolean implicitExit;

    private JFrame frame;
    private JFXPanel fxPanel;

    public void init() {
        assertTrue(SwingUtilities.isEventDispatchThread());
        assertEquals(1, initialized.getCount());
        assertTrue(Platform.isImplicitExit());
        if (!implicitExit) {
            Platform.setImplicitExit(false);
            assertFalse(Platform.isImplicitExit());
        }

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

        initialized.countDown();
        assertEquals(0, initialized.getCount());
    }

    private void doTestCommon(boolean implicitExit,
            boolean reEnableImplicitExit, boolean appShouldExit) {

        SwingExitCommon.implicitExit = implicitExit;

        final Throwable[] testError = new Throwable[1];
        final Thread testThread = Thread.currentThread();

        // Start the Application
        SwingUtilities.invokeLater(() -> {
            try {
                init();
            } catch (Throwable th) {
                testError[0] = th;
                testThread.interrupt();
            }
        });

        try {
            if (!initialized.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for JFXPanel to launch and initialize");
            }

            Thread.sleep(SLEEP_TIME);
            try {
                SwingUtilities.invokeAndWait(() -> {
                    frame.setVisible(false);
                    frame.dispose();
                });
            }
            catch (InvocationTargetException ex) {
                AssertionFailedError err = new AssertionFailedError("Exception while disposing JFrame");
                err.initCause(ex.getCause());
                throw err;
            }

            final CountDownLatch exitLatch = PlatformImpl.test_getPlatformExitLatch();

            if (reEnableImplicitExit) {
                Thread.sleep(SLEEP_TIME);
                assertEquals(1, exitLatch.getCount());
                assertFalse(Platform.isImplicitExit());
                Platform.setImplicitExit(true);
                assertTrue(Platform.isImplicitExit());
            }

            if (!appShouldExit) {
                Thread.sleep(SLEEP_TIME);
                assertEquals(1, exitLatch.getCount());
                Platform.exit();
            }

            if (!exitLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for Platform to exit");
            }
        } catch (InterruptedException ex) {
            Util.throwError(testError[0]);
        }
    }

    // ========================== TEST CASES ==========================

    // Implementation of SingleImplicitTest.testImplicitExit
    public void doTestImplicitExit() {
        // implicitExit, no re-enable, should exit
        doTestCommon(true, false, true);
    }

    // Implementation of testExplicitExit
    public void doTestExplicitExit() {
        // no implicitExit, no re-enable, should not exit
        doTestCommon(false, false, false);
    }

    // Implementation of testExplicitExitReEnable
    public void doTestExplicitExitReEnable() {
        // no implicitExit, re-enable, should exit
        doTestCommon(false, true, true);
    }

}
