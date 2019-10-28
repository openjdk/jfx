/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import jdk.swing.interop.SwingInterOpUtils;
import junit.framework.AssertionFailedError;
import org.junit.BeforeClass;
import org.junit.Test;
import javafx.scene.Scene;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JFXPanelTest {

    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);


    @BeforeClass
    public static void doSetupOnce() {
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            launchLatch.countDown();
        });


        try {
            if (!launchLatch.await(5000, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for Application to launch");
            }
        } catch (InterruptedException ex) {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            err.initCause(ex);
            throw err;
        }

        assertEquals(0, launchLatch.getCount());
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

        Thread.sleep(100); // there should be no pressed event after the initial one. Let's wait for 0.1s and check again.

        assertEquals(1, pressedEventCounter[0]);
    }


}