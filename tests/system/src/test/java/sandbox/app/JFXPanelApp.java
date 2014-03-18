/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package sandbox.app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import static sandbox.Constants.*;

/**
 * JFXPanel application to test running with a security manager installed.
 * The main program in this class will run the test such that the application
 * will be shutdown by an explicit call to System.exit().
 */
public class JFXPanelApp {

    private void initApp(final boolean implicitExit) throws Exception {
        final JFrame frame = new JFrame("JFXPanel Test");
        frame.setLayout(new BorderLayout());

        JPanel swingPanel = new JPanel();
        swingPanel.setLayout(new FlowLayout());
        frame.getContentPane().add(swingPanel, BorderLayout.NORTH);

        JButton swingButton = new JButton("Swing Button");
        swingButton.addActionListener(e -> System.err.println("Hi"));
        swingPanel.add(swingButton);

        // Create javafx panel
        final JFXPanel jfxPanel = new JFXPanel();
        jfxPanel.setPreferredSize(new Dimension(400,300));
        frame.getContentPane().add(jfxPanel, BorderLayout.CENTER);

        // create JavaFX scene
        createScene(jfxPanel);
        if (!implicitExit) {
            Platform.setImplicitExit(false);
        }

        // show frame
        frame.pack();
        frame.setVisible(true);

        // Hide the frame after the specified amount of time
        Timer timer = new Timer(SHOWTIME, e -> {
            if (implicitExit) {
                frame.setVisible(false);
                frame.dispose();
            } else {
                System.exit(ERROR_NONE);
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void createScene(final JFXPanel jfxPanel) throws Exception {
        final AtomicReference<Throwable> err = new AtomicReference<>(null);
        Platform.runLater(() -> {
            try {
                final Scene scene = Util.createScene();
                jfxPanel.setScene(scene);
            } catch (Error | Exception t) {
                err.set(t);
            }
        });
        Throwable t = err.get();
        if (t instanceof Error) {
            throw (Error)t;
        } else if (t instanceof Exception) {
            throw (Exception)t;
        }
    }

    public JFXPanelApp(boolean implicitExit) {
        try {
            try {
                // Ensure that we are running with a restrictive
                // security manager
                System.getProperty("sun.something");
                System.err.println("*** Did not get expected security exception");
                System.exit(ERROR_NO_SECURITY_EXCEPTION);
            } catch (SecurityException ex) {
                // This is expected
            }
            initApp(implicitExit);
        } catch (SecurityException ex) {
            ex.printStackTrace(System.err);
            System.exit(ERROR_SECURITY_EXCEPTION);
        } catch (ExceptionInInitializerError ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof SecurityException) {
                System.exit(ERROR_SECURITY_EXCEPTION);
            }
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        } catch (Error | Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(ERROR_UNEXPECTED_EXCEPTION);
        }
    }

    public static void runTest(final boolean implicitExit) {
        Util.setupTimeoutThread();
        SwingUtilities.invokeLater(() -> new JFXPanelApp(implicitExit));
    }

    public static void main(String[] args) {
        runTest(false);
    }

}
