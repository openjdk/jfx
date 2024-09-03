/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.launchertest;

import java.awt.BorderLayout;

import javafx.application.Platform;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import static test.launchertest.Constants.*;

/**
 * Test that an AWT app that starts the JavaFX Platform with only daemon
 * threads doesn't exit prematurely. It should remain alive until
 * Platform.exit is called.
 *
 * This is launched by MainLauncherTest.
 */
public class TestAWTAppDaemon {

    private static void createSwing() {
        // Start up FX platform
        Platform.startup(() -> {});

        final JFrame frame = new JFrame("TestAWTAppDaemon");
        frame.setLayout(new BorderLayout());

        JPanel swingPanel = new JPanel();
        frame.getContentPane().add(swingPanel, BorderLayout.CENTER);

        // show frame
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Show window for 1 second then close and dispost it
        Util.sleep(1000);
        frame.setVisible(false);
        frame.dispose();

        // Sleep for 5 seconds to ensure that we don't exit prematurely,
        // then exit normally.
        Thread thr = new Thread(() -> {
            Util.sleep(5000);
            System.exit(ERROR_NONE);
        });
        thr.setDaemon(true);
        thr.start();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Util.setupTimeoutThread();
        SwingUtilities.invokeLater(TestAWTAppDaemon::createSwing);
    }

}
