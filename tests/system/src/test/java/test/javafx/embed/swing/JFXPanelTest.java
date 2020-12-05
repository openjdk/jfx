/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.PlatformUtil;
import org.junit.Assume;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import junit.framework.AssertionFailedError;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import javafx.scene.Group;
import javafx.scene.Scene;
import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class JFXPanelTest {
    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    JFrame jframe;

    // Application class. An instance is created and initialized before running
    // the first test, and it lives through the execution of all tests.
    public static class MyApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            Platform.setImplicitExit(false);
            Assert.assertTrue(Platform.isFxApplicationThread());
            Assert.assertNotNull(primaryStage);

            launchLatch.countDown();
        }
    }

    @BeforeClass
    public static void doSetupOnce() throws Exception {
        // Start the Application
        new Thread(() -> Application.launch(MyApp.class, (String[]) null)).start();

        if (!launchLatch.await(5000, TimeUnit.MILLISECONDS)) {
            throw new AssertionFailedError("Timeout waiting for Application to launch");
        }

        Assert.assertEquals(0, launchLatch.getCount());
    }

    @AfterClass
    public static void doTeardownOnce() {
        Platform.exit();
    }

    @After
    public void doCleanup() {
        if (jframe != null) {
            SwingUtilities.invokeLater(() -> jframe.dispose());
        }
    }

    static class TestFXPanel extends JFXPanel {
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
            TestFXPanel dummyFXPanel = new TestFXPanel();
            dummyFXPanel.setPreferredSize(new Dimension(100, 100));
            TestFXPanel fxPnl = new TestFXPanel();
            fxPnl.setPreferredSize(new Dimension(100, 100));
            jframe = new JFrame();
            JPanel jpanel = new JPanel();
            jpanel.add(dummyFXPanel);
            jpanel.add(fxPnl);
            jframe.setContentPane(jpanel);
            jframe.pack();
            jframe.setVisible(true);

            Platform.runLater(() -> {
                Scene dummyScene = new Scene(new Group());
                dummyFXPanel.setScene(dummyScene);
                Scene scene = new Scene(new Group());
                fxPnl.setScene(scene);

                scene.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_PRESSED, (event -> {
                    pressedEventCounter[0] += 1;
                    firstPressedEventLatch.countDown();
                }));

                SwingUtilities.invokeLater(() -> {
                    MouseEvent e = new MouseEvent(fxPnl, MouseEvent.MOUSE_PRESSED, 0, MouseEvent.BUTTON1_DOWN_MASK,
                            5, 5, 1, false, MouseEvent.BUTTON1);

                    fxPnl.processMouseEventPublic(e);
                });
            });
        });

        Assert.assertTrue(firstPressedEventLatch.await(5000, TimeUnit.MILLISECONDS));

        Thread.sleep(500); // there should be no pressed event after the initial one. Let's wait for 0.5s and check again.

        Assert.assertEquals(1, pressedEventCounter[0]);
    }

    @Test
    public void testClickOnEmptyJFXPanel() throws Exception {
        CountDownLatch firstPressedEventLatch = new CountDownLatch(1);

        SwingUtilities.invokeLater(() -> {
            TestFXPanel fxPnl = new TestFXPanel();

            MouseEvent e = new MouseEvent(fxPnl, MouseEvent.MOUSE_PRESSED, 0, MouseEvent.BUTTON1_DOWN_MASK,
                    5, 5, 1, false, MouseEvent.BUTTON1);

            fxPnl.processMouseEventPublic(e);

            firstPressedEventLatch.countDown();
        });

        Assert.assertTrue(firstPressedEventLatch.await(5000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void setSceneOnFXThread() throws Exception {

        CountDownLatch completionLatch = new CountDownLatch(1);

        SwingUtilities.invokeLater(() -> {
            JFXPanel fxPanel = new JFXPanel();
            fxPanel.setPreferredSize(new Dimension(100, 100));
            jframe = new JFrame();
            JPanel jpanel = new JPanel();
            jpanel.add(fxPanel);
            jframe.add(jpanel);
            jframe.pack();
            jframe.setVisible(true);

            Platform.runLater(() -> {
                Scene scene = new Scene(new Group());
                fxPanel.setScene(scene);
                completionLatch.countDown();
            });
        });

        Assert.assertTrue("Timeout waiting for setScene to complete",
                completionLatch.await(5000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void setSceneOnSwingThread() throws Exception {

        CountDownLatch completionLatch = new CountDownLatch(1);

        SwingUtilities.invokeLater(() -> {
            JFXPanel fxPanel = new JFXPanel();
            fxPanel.setPreferredSize(new Dimension(100, 100));
            jframe = new JFrame();
            JPanel jpanel = new JPanel();
            jpanel.add(fxPanel);
            jframe.add(jpanel);
            jframe.pack();
            jframe.setVisible(true);

            Platform.runLater(() -> {
                Scene scene = new Scene(new Group());
                SwingUtilities.invokeLater(() -> {
                    fxPanel.setScene(scene);
                    completionLatch.countDown();
                });
            });
        });

        Assert.assertTrue("Timeout waiting for setScene to complete",
                completionLatch.await(5000, TimeUnit.MILLISECONDS));
    }
}
