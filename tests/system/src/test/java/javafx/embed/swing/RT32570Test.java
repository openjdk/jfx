/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Dimension;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import org.junit.Test;
import testharness.VisualTestBase;

/**
 * Tests that a JPopupMenu shown from inside of SwingNode second and
 * all subsequent times is repainted.
 *
 * https://javafx-jira.kenai.com/browse/RT-32570
 */
public class RT32570Test extends VisualTestBase {

    private static final double TOLERANCE = 0.07;
    private static final int WIDTH = 100;
    private static final int HEIGHT = 50;

    private volatile SwingNode swingNode;
    private Scene testScene;
    private JPopupMenu popup;
    private JLabel label;

    private volatile boolean popped;

    @Test(timeout=5000)
    public void test() throws Exception {
        runAndWait(() -> {
            swingNode = new SwingNode();
            Group group = new Group();
            group.getChildren().add(swingNode);

            testScene = new Scene(group, WIDTH, HEIGHT);
            Stage stage = getStage();
            stage.setScene(testScene);
            stage.show();
        });

        SwingUtilities.invokeAndWait(() -> {
            label = new JLabel();
            label.setMinimumSize(new Dimension(WIDTH, HEIGHT));
            label.setBackground(java.awt.Color.GREEN);
            label.setOpaque(true);

            popup = new JPopupMenu();
            JMenuItem item = new JMenuItem();
            item.setPreferredSize(new Dimension(WIDTH, HEIGHT));
            item.setBackground(java.awt.Color.RED);
            popup.add(item);

            swingNode.setContent(label);
        });

        waitFirstFrame();

        SwingUtilities.invokeAndWait(() -> popup.show(label, 0, 0));
        SwingUtilities.invokeAndWait(() -> popup.setVisible(false));
        SwingUtilities.invokeAndWait(() -> popup.show(label, 0, 0));

        // Wait for the popup to be shown (second time):
        while (!popped) {
            runAndWait(() -> {
                // If it's not shown, the background remains green
                Color color = getColor(testScene, WIDTH / 2, HEIGHT / 2);
                popped = !testColorEquals(Color.GREEN, color, TOLERANCE);
            });
            try { Thread.sleep(100); } catch(Exception e) {}
        }

        // Verify the popup content is painted:
        runAndWait(() -> {
            Color color = getColor(testScene, WIDTH / 2, HEIGHT / 2);
            assertColorEquals(Color.RED, color, TOLERANCE);
        });
    }
}

