/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.glass.ui.mac;

import com.sun.javafx.tk.Toolkit;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.embed.swing.JFXPanel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import test.util.Util;

public class MacOSSystemMenuJFXPanelSwingFirstTest extends MacOSSystemMenuTestBase {

    @Test
    public void test() throws InterruptedException, IOException {
        initSwing(List.of(TEST_MENUS_0, TEST_MENUS_1));
        initJavaFX(List.of());

        JFrame wnd = swingWindows.get(0);
        JFXPanel fxPanel = new JFXPanel();

        wnd.add(fxPanel);
        wnd.setVisible(true);

        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            Scene scene = new Scene(new BorderPane(new Label("Hello World!")));
            fxPanel.setScene(scene);

            latch.countDown();
        });

        latch.await();

        focusSwing(0);
        waitForUser();
//        List<Element> swingElements = getMenusOfFocusedWindow();
//        compareMenus(swingElements, TEST_MENUS_0);

        focusSwing(1);
        waitForUser();
//        swingElements = getMenusOfFocusedWindow();
//        compareMenus(swingElements, TEST_MENUS_1);

        focusSwing(0);
        waitForUser();
//        swingElements = getMenusOfFocusedWindow();
//        compareMenus(swingElements, TEST_MENUS_0);
    }
}
