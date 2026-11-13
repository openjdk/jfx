/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.tk.quantum;

import com.sun.glass.events.WindowEvent;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.Window;
import com.sun.javafx.tk.quantum.WindowStageShim;
import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GlassWindowEventHandlerTest {

    private static final CountDownLatch startupLatch = new CountDownLatch(1);

    private Stage stage;

    @BeforeAll
    public static void initFX() {
        Platform.setImplicitExit(false);
        Util.startup(startupLatch, startupLatch::countDown);
    }

    @AfterAll
    public static void teardown() {
        Util.shutdown();
    }

    @AfterEach
    public void cleanup() {
        Util.runAndWait(() -> {
            if (stage != null) {
                stage.hide();
                stage = null;
            }
        });
    }

    private Window showStage() {
        stage = new Stage();
        stage.setScene(new Scene(new Group(), 200, 200));
        stage.show();
        Window window = WindowStageShim.getPlatformWindow(stage);
        assertNotNull(window);
        assertNotNull(window.getEventHandler());
        return window;
    }

    private void hideStageWhenDeiconifiedDuringWindowEvent(int eventType) {
        Util.runAndWait(() -> {
            Window window = showStage();
            Window.EventHandler handler = window.getEventHandler();

            stage.setIconified(true);
            stage.iconifiedProperty().addListener((obs, oldValue, newValue) -> {
                if (!newValue) {
                    stage.hide();
                }
            });

            handler.handleWindowEvent(window, System.nanoTime(), eventType);

            assertFalse(stage.isShowing());
        });
    }

    @Test
    public void hideStageFromIconifiedListenerDuringMaximize() {
        hideStageWhenDeiconifiedDuringWindowEvent(WindowEvent.MAXIMIZE);
    }

    @Test
    public void hideStageFromIconifiedListenerDuringRestore() {
        hideStageWhenDeiconifiedDuringWindowEvent(WindowEvent.RESTORE);
    }

    @Test
    public void screenChangedEventAfterHide() {
        Util.runAndWait(() -> {
            Window window = showStage();
            Window.EventHandler handler = window.getEventHandler();
            stage.hide();

            Screen screen = Screen.getMainScreen();
            handler.handleScreenChangedEvent(window, System.nanoTime(), screen, screen);
        });
    }

    @Test
    public void levelEventAfterHide() {
        Util.runAndWait(() -> {
            Window window = showStage();
            Window.EventHandler handler = window.getEventHandler();
            stage.hide();

            handler.handleLevelEvent(Window.Level.FLOATING);
        });
    }
}
