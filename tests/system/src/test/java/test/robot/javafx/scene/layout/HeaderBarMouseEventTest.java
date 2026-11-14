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

package test.robot.javafx.scene.layout;

import com.sun.glass.events.KeyEvent;
import com.sun.glass.events.MouseEvent;
import com.sun.glass.ui.ClipboardAssistance;
import com.sun.glass.ui.HeaderButtonOverlay;
import com.sun.glass.ui.View;
import com.sun.glass.ui.ViewShim;
import com.sun.glass.ui.Window;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.tk.HeaderAreaType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HeaderBar;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import test.robot.testharness.VisualTestBase;
import test.util.Util;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Checks how extended windows route mouse input between the window buttons and the application.
 */
@Timeout(20)
public class HeaderBarMouseEventTest extends VisualTestBase {

    private Stage stage;
    private Window window;
    private View view;
    private HeaderButtonOverlay overlay;

    @BeforeEach
    public void showStage() {
        assumeTrue(PlatformUtil.isWindows() || PlatformUtil.isLinux(), "Requires a window button overlay");

        runAndWait(() -> {
            var root = new BorderPane();
            root.setTop(new HeaderBar());
            stage = getStage();
            stage.initStyle(StageStyle.EXTENDED);
            stage.setScene(new Scene(root, 600, 320));
            stage.show();
            stage.centerOnScreen();
        });

        waitFirstFrame();
        runAndWait(() -> getRobot().mouseMove(stage.getX() + stage.getWidth() / 2, stage.getY() + stage.getHeight() / 2));
        waitNextFrame();
        runAndWait(() -> {
            window = Window.getWindows().stream()
                .filter(candidate -> candidate.headerButtonOverlayProperty().get() instanceof HeaderButtonOverlay node
                    && node.getScene() == stage.getScene())
                .findFirst().orElseThrow();

            view = window.getView();
            overlay = (HeaderButtonOverlay)window.headerButtonOverlayProperty().get();
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void windowButtonHandlesClicks(boolean synthesized) {
        runAndWait(() -> {
            var closeButton = overlay.lookup(".-FX-INTERNAL-close-button");
            var bounds = closeButton.localToScene(closeButton.getBoundsInLocal());
            int x = (int)Math.round(bounds.getCenterX() * window.getPlatformScaleX());
            int y = (int)Math.round(bounds.getCenterY() * window.getPlatformScaleY());
            int[] closeRequests = {0};

            stage.setOnCloseRequest(event -> {
                closeRequests[0]++;
                event.consume();
            });

            var handler = new RecordingHandler();
            View.EventHandler original = view.getEventHandler();
            view.setEventHandler(handler);

            try {
                // The application owns the cursor before the press on the window button.
                sendMouse(MouseEvent.ENTER, view.getWidth() / 2, view.getHeight() / 2, false, synthesized);
                handler.mouseEvents.clear();

                sendMouse(MouseEvent.DOWN, x, y, false, synthesized);
                assertTrue(closeButton.getPseudoClassStates().contains(PseudoClass.getPseudoClass("pressed")));
                sendMouse(MouseEvent.UP, x, y, false, synthesized);
                assertFalse(closeButton.getPseudoClassStates().contains(PseudoClass.getPseudoClass("pressed")));
                assertEquals(1, closeRequests[0]);
                assertTrue(stage.isShowing());

                assertEquals(List.of(new ReceivedMouseEvent(MouseEvent.EXIT, false, synthesized)), handler.mouseEvents);
            } finally {
                view.setEventHandler(original);
            }
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void changingInputSourceKeepsEventsInOverlay(boolean synthesizedMove) {
        runAndWait(() -> {
            var closeButton = overlay.lookup(".-FX-INTERNAL-close-button");
            var bounds = closeButton.localToScene(closeButton.getBoundsInLocal());
            int x = (int)Math.round(bounds.getCenterX() * window.getPlatformScaleX());
            int y = (int)Math.round(bounds.getCenterY() * window.getPlatformScaleY());
            int[] closeRequests = {0};

            stage.setOnCloseRequest(event -> {
                closeRequests[0]++;
                event.consume();
            });

            var handler = new RecordingHandler();
            View.EventHandler original = view.getEventHandler();
            view.setEventHandler(handler);

            try {
                sendMouse(MouseEvent.MOVE, x, y, false, synthesizedMove);
                assertEquals(List.of(new ReceivedMouseEvent(MouseEvent.EXIT, false, synthesizedMove)), handler.mouseEvents);

                sendMouse(MouseEvent.DOWN, x, y, false, !synthesizedMove);
                sendMouse(MouseEvent.UP, x, y, false, !synthesizedMove);
                sendMouse(MouseEvent.MOVE, x, y, false, synthesizedMove);
                assertEquals(1, closeRequests[0]);
                assertTrue(stage.isShowing());
                assertEquals(List.of(new ReceivedMouseEvent(MouseEvent.EXIT, false, synthesizedMove)), handler.mouseEvents);

                sendMouse(MouseEvent.MOVE, view.getWidth() / 2, view.getHeight() / 2, false, synthesizedMove);
                assertEquals(List.of(
                    new ReceivedMouseEvent(MouseEvent.EXIT, false, synthesizedMove),
                    new ReceivedMouseEvent(MouseEvent.ENTER, false, synthesizedMove),
                    new ReceivedMouseEvent(MouseEvent.MOVE, false, synthesizedMove)), handler.mouseEvents);
            } finally {
                view.setEventHandler(original);
            }
        });
    }

    @Test
    public void applicationOwnsCursorOverWindowButtonInFullscreen() {
        runAndWait(() -> {
            var closeButton = overlay.lookup(".-FX-INTERNAL-close-button");
            var bounds = closeButton.localToScene(closeButton.getBoundsInLocal());
            int x = (int)Math.round(bounds.getCenterX() * window.getPlatformScaleX());
            int y = (int)Math.round(bounds.getCenterY() * window.getPlatformScaleY());

            var handler = new RecordingHandler();
            View.EventHandler original = view.getEventHandler();
            view.setEventHandler(handler);

            try {
                sendMouse(MouseEvent.MOVE, x, y, false, false);
                assertTrue(closeButton.getPseudoClassStates().contains(PseudoClass.getPseudoClass("hover")));
                handler.mouseEvents.clear();

                ViewShim.setInFullscreen(view, true);
                sendMouse(MouseEvent.MOVE, x, y, false, false);
                assertFalse(closeButton.getPseudoClassStates().contains(PseudoClass.getPseudoClass("hover")));
                assertEquals(List.of(
                    new ReceivedMouseEvent(MouseEvent.ENTER, false, false),
                    new ReceivedMouseEvent(MouseEvent.MOVE, false, false)), handler.mouseEvents);
            } finally {
                ViewShim.setInFullscreen(view, false);
                view.setEventHandler(original);
            }
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void applicationInputPreservesFlagsAndStartsEachDragOnce(boolean synthesized) {
        runAndWait(() -> {
            var handler = new RecordingHandler();
            View.EventHandler original = view.getEventHandler();
            view.setEventHandler(handler);

            try {
                int x = view.getWidth() / 2;
                int y = view.getHeight() / 2;

                for (int gesture = 1; gesture <= 2; gesture++) {
                    handler.mouseEvents.clear();
                    sendMouse(MouseEvent.DOWN, x, y, false, synthesized);
                    sendMouse(MouseEvent.DRAG, x + 10, y, false, synthesized);
                    sendMouse(MouseEvent.DRAG, x + 20, y, false, synthesized);
                    sendMouse(MouseEvent.UP, x + 20, y, true, synthesized);

                    assertEquals(List.of(
                        new ReceivedMouseEvent(MouseEvent.DOWN, false, synthesized),
                        new ReceivedMouseEvent(MouseEvent.DRAG, false, synthesized),
                        new ReceivedMouseEvent(MouseEvent.DRAG, false, synthesized),
                        new ReceivedMouseEvent(MouseEvent.UP, true, synthesized)), handler.mouseEvents);
                    assertEquals(gesture, handler.dragStarts);
                }
            } finally {
                view.setEventHandler(original);
            }
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void doubleClickReachesHeaderHitTest(boolean synthesized) {
        assumeTrue(PlatformUtil.isWindows(), "Windows handles header double-clicks in WinView");
        runAndWait(() -> {
            var handler = new RecordingHandler();
            View.EventHandler original = view.getEventHandler();
            view.setEventHandler(handler);
            try {
                int x = view.getWidth() / 2;
                int y = (int)Math.round(10 * window.getPlatformScaleY());
                sendMouse(MouseEvent.DOWN, x, y, false, synthesized);
                sendMouse(MouseEvent.UP, x, y, false, synthesized);
                sendMouse(MouseEvent.DOWN, x, y, false, synthesized);
                sendMouse(MouseEvent.UP, x, y, false, synthesized);
                assertEquals(1, handler.headerHitTests);
            } finally {
                view.setEventHandler(original);
            }
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void doubleClickMaximizesWindowAndReachesScene(boolean synthesized) {
        assumeTrue(PlatformUtil.isWindows(), "Windows handles header double-clicks in WinView");
        CountDownLatch maximized = new CountDownLatch(1);
        List<ReceivedMouseEvent> mouseEvents = new ArrayList<>();

        runAndWait(() -> {
            assertFalse(stage.isMaximized());
            stage.maximizedProperty().addListener((_, _, newValue) -> {
                if (newValue) {
                    maximized.countDown();
                }
            });

            Scene scene = stage.getScene();
            scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event ->
                mouseEvents.add(new ReceivedMouseEvent(MouseEvent.DOWN, event.isPopupTrigger(), event.isSynthesized())));
            scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, event ->
                mouseEvents.add(new ReceivedMouseEvent(MouseEvent.UP, event.isPopupTrigger(), event.isSynthesized())));

            HeaderBar headerBar = (HeaderBar)((BorderPane)scene.getRoot()).getTop();
            Bounds bounds = headerBar.localToScene(headerBar.getLayoutBounds());
            int x = (int)Math.round(bounds.getCenterX() * window.getPlatformScaleX());
            int y = (int)Math.round(bounds.getCenterY() * window.getPlatformScaleY());
            assertEquals(HeaderAreaType.DRAGBAR, view.getEventHandler().pickHeaderArea(
                x / window.getPlatformScaleX(), y / window.getPlatformScaleY()));

            sendMouse(MouseEvent.DOWN, x, y, false, synthesized);
            sendMouse(MouseEvent.UP, x, y, false, synthesized);
            sendMouse(MouseEvent.DOWN, x, y, false, synthesized);
            sendMouse(MouseEvent.UP, x, y, false, synthesized);
        });

        Util.waitForLatch(maximized, 5, "Header double-click did not maximize the window");

        runAndWait(() -> {
            assertTrue(stage.isMaximized());
            assertEquals(List.of(
                new ReceivedMouseEvent(MouseEvent.DOWN, false, synthesized),
                new ReceivedMouseEvent(MouseEvent.UP, false, synthesized),
                new ReceivedMouseEvent(MouseEvent.DOWN, false, synthesized),
                new ReceivedMouseEvent(MouseEvent.UP, false, synthesized)), mouseEvents);
        });
    }

    private void sendMouse(int type, int x, int y, boolean popupTrigger, boolean synthesized) {
        int button = type == MouseEvent.DOWN || type == MouseEvent.UP || type == MouseEvent.DRAG
            ? MouseEvent.BUTTON_LEFT
            : MouseEvent.BUTTON_NONE;

        int modifiers = type == MouseEvent.DOWN || type == MouseEvent.DRAG
            ? KeyEvent.MODIFIER_BUTTON_PRIMARY
            : 0;

        ViewShim.notifyMouse(
            view, type, button, x, y,
            window.getX() + view.getX() + x, window.getY() + view.getY() + y,
            modifiers, popupTrigger, synthesized);
    }

    private record ReceivedMouseEvent(int type, boolean popupTrigger, boolean synthesized) {}

    private static class RecordingHandler extends View.EventHandler {
        final List<ReceivedMouseEvent> mouseEvents = new ArrayList<>();
        int dragStarts;
        int headerHitTests;

        @Override
        public void handleMouseEvent(View view, long time, int type, int button, int x, int y, int xAbs, int yAbs,
                                     int modifiers, boolean isPopupTrigger, boolean isSynthesized) {
            mouseEvents.add(new ReceivedMouseEvent(type, isPopupTrigger, isSynthesized));
        }

        @Override
        public void handleDragStart(View view, int button, int x, int y, int xAbs, int yAbs,
                                    ClipboardAssistance assistant) {
            dragStarts++;
        }

        @Override
        public HeaderAreaType pickHeaderArea(double x, double y) {
            headerHitTests++;
            return null;
        }
    }
}
