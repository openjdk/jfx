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
package test.javafx.embed.swing;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.swing.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class JFXPanelEmbeddedWindowTest {
    private static JFrame frame;
    private static JFXPanel jfxPanel;
    private static Throwable th;

    public static void main(String[] args) throws Exception {
        init();
        try {
            new JFXPanelEmbeddedWindowTest().testShowThenRemove();
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
        CountDownLatch initLatch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("JFXPanel test");
            frame.setSize(200, 200);
            jfxPanel = new JFXPanel() {
                @Override
                public boolean isFocusOwner() {
                    return true;
                }
            };
            frame.getContentPane().add(jfxPanel);
            frame.setVisible(true);

            Platform.runLater(() -> {
                Scene scene = new Scene(new VBox());
                jfxPanel.setScene(scene);
                Platform.runLater(() -> initLatch.countDown());
            });
        });
        initLatch.await(15, TimeUnit.SECONDS);
    }

    @Test
    public void testShowThenRemove() throws Throwable {
        CountDownLatch innerLatch = new CountDownLatch(1);
        CountDownLatch outerLatch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            Thread.currentThread().setUncaughtExceptionHandler((t, e) -> th = e);
            Platform.runLater( () -> {
                jfxPanel.removeNotify();
                jfxPanel.addNotify();
                jfxPanel.setScene(null);
                innerLatch.countDown();
            });
            try {
                innerLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            SwingUtilities.invokeLater(() -> outerLatch.countDown());
        });
        outerLatch.await(5, TimeUnit.SECONDS);
        if (th != null) {
            throw th;
        }
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (frame != null) {
            SwingUtilities.invokeLater(frame::dispose);
        }
    }
}

