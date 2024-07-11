/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk.quantum;

import com.sun.glass.events.GestureEvent;
import com.sun.glass.events.KeyEvent;
import com.sun.glass.events.MouseEvent;
import com.sun.glass.events.ViewEvent;
import com.sun.glass.events.TouchEvent;
import com.sun.glass.events.SwipeGesture;
import com.sun.glass.ui.Accessible;
import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.ClipboardAssistance;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.collections.TrackableObservableList;
import com.sun.javafx.logging.PulseLogger;
import static com.sun.javafx.logging.PulseLogger.PULSE_LOGGING_ENABLED;
import com.sun.javafx.scene.input.KeyCodeMap;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodHighlight;
import javafx.scene.input.InputMethodTextRun;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.input.TransferMode;
import javafx.scene.input.ZoomEvent;

import java.security.AccessController;
import java.security.PrivilegedAction;

class GlassViewEventHandler extends View.EventHandler {

    static boolean zoomGestureEnabled;
    static boolean rotateGestureEnabled;
    static boolean scrollGestureEnabled;
    static {
        @SuppressWarnings("removal")
        var dummy = AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            zoomGestureEnabled = Boolean.valueOf(System.getProperty("com.sun.javafx.gestures.zoom", "false"));
            rotateGestureEnabled = Boolean.valueOf(System.getProperty("com.sun.javafx.gestures.rotate", "false"));
            scrollGestureEnabled = Boolean.valueOf(System.getProperty("com.sun.javafx.gestures.scroll", "false"));
            return null;
        });
    }

    private ViewScene scene;
    private final GlassSceneDnDEventHandler dndHandler;
    private final GestureRecognizers gestures;

    public GlassViewEventHandler(final ViewScene scene) {
        this.scene = scene;

        dndHandler = new GlassSceneDnDEventHandler(scene);

        gestures = new GestureRecognizers();
        if (PlatformUtil.isWindows() || PlatformUtil.isIOS() || PlatformUtil.isEmbedded()) {
            gestures.add(new SwipeGestureRecognizer(scene));
        }
        if (zoomGestureEnabled) {
            gestures.add(new ZoomGestureRecognizer(scene));
        }
        if (rotateGestureEnabled) {
            gestures.add(new RotateGestureRecognizer(scene));
        }
        if (scrollGestureEnabled) {
            gestures.add(new ScrollGestureRecognizer(scene));
        }
    }

    // Default fullscreen allows limited keyboard input.
    // It will only receive events from the following keys:
    // DOWN, UP, LEFT, RIGHT, SPACE, TAB, PAGE_UP, PAGE_DOWN,
    // HOME, END, ENTER.
    private static boolean allowableFullScreenKeys(int key) {
        switch (key) {
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_UP:
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_TAB:
            case KeyEvent.VK_PAGE_DOWN:
            case KeyEvent.VK_PAGE_UP:
            case KeyEvent.VK_HOME:
            case KeyEvent.VK_END:
            case KeyEvent.VK_ENTER:
                return true;
        }
        return false;
    }

    private boolean checkFullScreenKeyEvent(int type, int key, char chars[], int modifiers) {
        return scene.getWindowStage().isTrustedFullScreen() || allowableFullScreenKeys(key);
    }

    private final PaintCollector collector = PaintCollector.getInstance();

    private static EventType<javafx.scene.input.KeyEvent> keyEventType(int glassType) {
        switch (glassType) {
            case com.sun.glass.events.KeyEvent.PRESS:
                return javafx.scene.input.KeyEvent.KEY_PRESSED;
            case com.sun.glass.events.KeyEvent.RELEASE:
                return javafx.scene.input.KeyEvent.KEY_RELEASED;
            case com.sun.glass.events.KeyEvent.TYPED:
                return javafx.scene.input.KeyEvent.KEY_TYPED;
            default:
                if (QuantumToolkit.verbose) {
                    System.err.println("Unknown Glass key event type: " + glassType);
                }
                return null;
        }
    }

    private final KeyEventNotification keyNotification = new KeyEventNotification();
    private class KeyEventNotification implements PrivilegedAction<Void> {
        View view;
        long time;
        int type;
        int key;
        char[] chars;
        int modifiers;

        private KeyCode lastKeyCode;

        @Override
        public Void run() {
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newInput(keyEventType(type).toString());
            }
            WindowStage stage = scene.getWindowStage();
            try {
                boolean shiftDown = (modifiers & KeyEvent.MODIFIER_SHIFT) != 0;
                boolean controlDown = (modifiers & KeyEvent.MODIFIER_CONTROL) != 0;
                boolean altDown = (modifiers & KeyEvent.MODIFIER_ALT) != 0;
                boolean metaDown = (modifiers & KeyEvent.MODIFIER_WINDOWS) != 0;

                String str = new String(chars);
                String text = str; // TODO: this must be a text like "HOME", "F1", or "A"

                javafx.scene.input.KeyEvent keyEvent = new javafx.scene.input.KeyEvent(
                        keyEventType(type),
                        str, text,
                        KeyCodeMap.valueOf(key) ,
                        shiftDown, controlDown, altDown, metaDown);

                KeyCode keyCode = KeyCodeMap.valueOf(key);
                switch (type) {
                    case com.sun.glass.events.KeyEvent.PRESS:
                    case com.sun.glass.events.KeyEvent.RELEASE:
                        lastKeyCode = keyCode;
                        break;

                    case com.sun.glass.events.KeyEvent.TYPED:
                        keyCode = lastKeyCode;
                        break;
                }

                if (stage != null) {
                    if (keyCode == KeyCode.ESCAPE) {
                        stage.setInAllowedEventHandler(false);
                    } else {
                        stage.setInAllowedEventHandler(true);
                    }
                }

                switch (type) {
                    case com.sun.glass.events.KeyEvent.PRESS:
                        if (view.isInFullscreen() && stage != null) {
                            if (stage.getSavedFullScreenExitKey() != null
                                && stage.getSavedFullScreenExitKey().match(keyEvent)) {
                                stage.exitFullScreen();
                            }
                        }
                        /* NOBREAK */
                    case com.sun.glass.events.KeyEvent.RELEASE:
                    case com.sun.glass.events.KeyEvent.TYPED:
                        if (view.isInFullscreen()) {
                            if (!checkFullScreenKeyEvent(type, key, chars, modifiers)) {
                                break;
                            }
                        }
                        if (scene.sceneListener != null) {
                            scene.sceneListener.keyEvent(keyEvent);
                        }
                        break;
                    default:
                        if (QuantumToolkit.verbose) {
                            System.out.println("handleKeyEvent: unhandled type: " + type);
                        }
                }
            } finally {
                if (stage != null) {
                    stage.setInAllowedEventHandler(false);
                }
                if (PULSE_LOGGING_ENABLED) {
                    PulseLogger.newInput(null);
                }
            }
            return null;
        }
    }

    @SuppressWarnings("removal")
    @Override public void handleKeyEvent(View view, long time, int type, int key,
                                         char[] chars, int modifiers)
    {
        keyNotification.view = view;
        keyNotification.time = time;
        keyNotification.type = type;
        keyNotification.key = key;
        keyNotification.chars = chars;
        keyNotification.modifiers = modifiers;

        QuantumToolkit.runWithoutRenderLock(() -> {
            return AccessController.doPrivileged(keyNotification, scene.getAccessControlContext());
        });
    }

    private static EventType<javafx.scene.input.MouseEvent> mouseEventType(int glassType) {
        switch (glassType) {
            case com.sun.glass.events.MouseEvent.DOWN:
                return javafx.scene.input.MouseEvent.MOUSE_PRESSED;
            case com.sun.glass.events.MouseEvent.UP:
                return javafx.scene.input.MouseEvent.MOUSE_RELEASED;
            case com.sun.glass.events.MouseEvent.ENTER:
                return javafx.scene.input.MouseEvent.MOUSE_ENTERED;
            case com.sun.glass.events.MouseEvent.EXIT:
                return javafx.scene.input.MouseEvent.MOUSE_EXITED;
            case com.sun.glass.events.MouseEvent.MOVE:
                return javafx.scene.input.MouseEvent.MOUSE_MOVED;
            case com.sun.glass.events.MouseEvent.DRAG:
                return javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
            case com.sun.glass.events.MouseEvent.WHEEL:
                throw new IllegalArgumentException("WHEEL event cannot "
                        + "be translated to MouseEvent, must be translated to "
                        + "ScrollEvent");
            default:
                if (QuantumToolkit.verbose) {
                    System.err.println("Unknown Glass mouse event type: " + glassType);
                }
                return null;
        }
    }

    private static MouseButton mouseEventButton(int glassButton) {
        switch (glassButton) {
            case com.sun.glass.events.MouseEvent.BUTTON_LEFT:
                return MouseButton.PRIMARY;
            case com.sun.glass.events.MouseEvent.BUTTON_RIGHT:
                return MouseButton.SECONDARY;
            case com.sun.glass.events.MouseEvent.BUTTON_OTHER:
                return MouseButton.MIDDLE;
            case com.sun.glass.events.MouseEvent.BUTTON_BACK:
                return MouseButton.BACK;
            case com.sun.glass.events.MouseEvent.BUTTON_FORWARD:
                return MouseButton.FORWARD;
            default:
                return MouseButton.NONE;
        }
    }

    // Used to determine whether a DRAG operation has been initiated on this window
    private int mouseButtonPressedMask = 0;

    private final MouseEventNotification mouseNotification = new MouseEventNotification();
    private class MouseEventNotification implements PrivilegedAction<Void> {
        View view;
        long time;
        int type;
        int button;
        int x, y, xAbs, yAbs;
        int modifiers;
        boolean isPopupTrigger;
        boolean isSynthesized;

        @Override
        public Void run() {
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newInput(mouseEventType(type).toString());
            }

            int buttonMask;
            switch (button) {
                case MouseEvent.BUTTON_LEFT:
                    buttonMask = KeyEvent.MODIFIER_BUTTON_PRIMARY;
                    break;
                case MouseEvent.BUTTON_OTHER:
                    buttonMask = KeyEvent.MODIFIER_BUTTON_MIDDLE;
                    break;
                case MouseEvent.BUTTON_RIGHT:
                    buttonMask = KeyEvent.MODIFIER_BUTTON_SECONDARY;
                    break;
                case MouseEvent.BUTTON_BACK:
                    buttonMask = KeyEvent.MODIFIER_BUTTON_BACK;
                    break;
                case MouseEvent.BUTTON_FORWARD:
                    buttonMask = KeyEvent.MODIFIER_BUTTON_FORWARD;
                    break;
                default:
                    buttonMask = 0;
                    break;
            }

            switch (type) {
                case MouseEvent.MOVE:
                    if (button != MouseEvent.BUTTON_NONE) {
                        //RT-11305: the drag hasn't been started on this window -- ignore the event
                        return null;
                    }
                    break;
                case MouseEvent.UP:
                    if ((mouseButtonPressedMask & buttonMask) == 0) {
                        //RT-11305: the mouse button hasn't been pressed on this window -- ignore the event
                        return null;
                    }
                    mouseButtonPressedMask &= ~buttonMask;
                    break;
                case MouseEvent.DOWN:
                    mouseButtonPressedMask |= buttonMask;
                    break;
                case MouseEvent.ENTER:
                case MouseEvent.EXIT:
                    break;
                case MouseEvent.CLICK:
                    // Don't send click events to FX, as they are generated in Scene
                    return null;
                default:
                    if (QuantumToolkit.verbose) {
                        System.out.println("handleMouseEvent: unhandled type: " + type);
                    }
            }

            WindowStage stage = scene.getWindowStage();
            try {
                if (stage != null) {
                    switch (type) {
                        case MouseEvent.UP:
                        case MouseEvent.DOWN:
                            stage.setInAllowedEventHandler(true);
                            break;
                        default:
                            stage.setInAllowedEventHandler(false);
                            break;
                    }
                }

                if (scene.sceneListener != null) {
                    boolean shiftDown = (modifiers & KeyEvent.MODIFIER_SHIFT) != 0;
                    boolean controlDown = (modifiers & KeyEvent.MODIFIER_CONTROL) != 0;
                    boolean altDown = (modifiers & KeyEvent.MODIFIER_ALT) != 0;
                    boolean metaDown = (modifiers & KeyEvent.MODIFIER_WINDOWS) != 0;
                    boolean primaryButtonDown = (modifiers & KeyEvent.MODIFIER_BUTTON_PRIMARY) != 0;
                    boolean middleButtonDown = (modifiers & KeyEvent.MODIFIER_BUTTON_MIDDLE) != 0;
                    boolean secondaryButtonDown = (modifiers & KeyEvent.MODIFIER_BUTTON_SECONDARY) != 0;
                    boolean backButtonDown = (modifiers & KeyEvent.MODIFIER_BUTTON_BACK) != 0;
                    boolean forwardButtonDown = (modifiers & KeyEvent.MODIFIER_BUTTON_FORWARD) != 0;
                    final Window w = view.getWindow();
                    double pScaleX, pScaleY, spx, spy, sx, sy;
                    if (w != null) {
                        pScaleX = w.getPlatformScaleX();
                        pScaleY = w.getPlatformScaleY();
                        Screen scr = w.getScreen();
                        if (scr != null) {
                            spx = scr.getPlatformX();
                            spy = scr.getPlatformY();
                            sx = scr.getX();
                            sy = scr.getY();
                        } else {
                            spx = spy = sx = sy = 0.0;
                        }
                    } else {
                        pScaleX = pScaleY = 1.0;
                        spx = spy = sx = sy = 0.0;
                    }

                    scene.sceneListener.mouseEvent(mouseEventType(type),
                            x / pScaleX, y / pScaleY,
                            sx + (xAbs - spx) / pScaleX, sy + (yAbs - spy) / pScaleY,
                            mouseEventButton(button), isPopupTrigger, isSynthesized,
                            shiftDown, controlDown, altDown, metaDown,
                            primaryButtonDown, middleButtonDown, secondaryButtonDown,
                            backButtonDown, forwardButtonDown);
                }
            } finally {
                if (stage != null) {
                    stage.setInAllowedEventHandler(false);
                }
                if (PULSE_LOGGING_ENABLED) {
                    PulseLogger.newInput(null);
                }
            }
            return null;
        }
    }

    @SuppressWarnings("removal")
    @Override
    public void handleMouseEvent(View view, long time, int type, int button,
                                 int x, int y, int xAbs, int yAbs,
                                 int modifiers, boolean isPopupTrigger, boolean isSynthesized)
    {
        mouseNotification.view = view;
        mouseNotification.time = time;
        mouseNotification.type = type;
        mouseNotification.button = button;
        mouseNotification.x = x;
        mouseNotification.y = y;
        mouseNotification.xAbs = xAbs;
        mouseNotification.yAbs = yAbs;
        mouseNotification.modifiers = modifiers;
        mouseNotification.isPopupTrigger = isPopupTrigger;
        mouseNotification.isSynthesized = isSynthesized;

        QuantumToolkit.runWithoutRenderLock(() -> {
            return AccessController.doPrivileged(mouseNotification, scene.getAccessControlContext());
        });
    }

    @SuppressWarnings("removal")
    @Override public void handleMenuEvent(final View view,
                                          final int x, final int y, final int xAbs, final int yAbs,
                                          final boolean isKeyboardTrigger)
    {
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.newInput("MENU_EVENT");
        }
        WindowStage stage = scene.getWindowStage();
        try {
            if (stage != null) {
                stage.setInAllowedEventHandler(true);
            }
            QuantumToolkit.runWithoutRenderLock(() -> {
                return AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    if (scene.sceneListener != null) {
                        double pScaleX, pScaleY, spx, spy, sx, sy;
                        final Window w = view.getWindow();
                        if (w != null) {
                            pScaleX = w.getPlatformScaleX();
                            pScaleY = w.getPlatformScaleY();
                            Screen scr = w.getScreen();
                            if (scr != null) {
                                spx = scr.getPlatformX();
                                spy = scr.getPlatformY();
                                sx = scr.getX();
                                sy = scr.getY();
                            } else {
                                spx = spy = sx = sy = 0.0;
                            }
                        } else {
                            pScaleX = pScaleY = 1.0;
                            spx = spy = sx = sy = 0.0;
                        }
                        scene.sceneListener.menuEvent(x / pScaleX, y / pScaleY,
                                                      sx + (xAbs - spx) / pScaleX,
                                                      sy + (yAbs - spy) / pScaleY,
                                                      isKeyboardTrigger);
                    }
                    return null;
                }, scene.getAccessControlContext());
            });
        } finally {
            if (stage != null) {
                stage.setInAllowedEventHandler(false);
            }
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newInput(null);
            }
        }
    }

    @SuppressWarnings("removal")
    @Override public void handleScrollEvent(final View view, final long time,
                                            final int x, final int y, final int xAbs, final int yAbs,
                                            final double deltaX, final double deltaY, final int modifiers,
                                            final int lines, final int chars,
                                            final int defaultLines, final int defaultChars,
                                            final double xMultiplier, final double yMultiplier)
    {
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.newInput("SCROLL_EVENT");
        }
        WindowStage stage = scene.getWindowStage();
        try {
            if (stage != null) {
                stage.setInAllowedEventHandler(false);
            }
            QuantumToolkit.runWithoutRenderLock(() -> {
                return AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    if (scene.sceneListener != null) {
                        final Window w = view.getWindow();
                        double pScaleX, pScaleY, spx, spy, sx, sy;
                        if (w != null) {
                            pScaleX = w.getPlatformScaleX();
                            pScaleY = w.getPlatformScaleY();
                            Screen scr = w.getScreen();
                            if (scr != null) {
                                spx = scr.getPlatformX();
                                spy = scr.getPlatformY();
                                sx = scr.getX();
                                sy = scr.getY();
                            } else {
                                spx = spy = sx = sy = 0.0;
                            }
                        } else {
                            pScaleX = pScaleY = 1.0;
                            spx = spy = sx = sy = 0.0;
                        }
                        scene.sceneListener.scrollEvent(ScrollEvent.SCROLL,
                            deltaX / pScaleX, deltaY / pScaleY, 0, 0,
                            xMultiplier, yMultiplier,
                            0, // touchCount
                            chars, lines, defaultChars, defaultLines,
                            x / pScaleX, y / pScaleY,
                            sx + (xAbs - spx) / pScaleX,
                            sy + (yAbs - spy) / pScaleY,
                            (modifiers & KeyEvent.MODIFIER_SHIFT) != 0,
                            (modifiers & KeyEvent.MODIFIER_CONTROL) != 0,
                            (modifiers & KeyEvent.MODIFIER_ALT) != 0,
                            (modifiers & KeyEvent.MODIFIER_WINDOWS) != 0,
                            false, // this is always indirect
                            false); // this has no inertia
                    }
                    return null;
                }, scene.getAccessControlContext());
            });
        } finally {
            if (stage != null) {
                stage.setInAllowedEventHandler(false);
            }
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newInput(null);
            }
        }
    }

    private static byte inputMethodEventAttrValue(int pos, int[] attrBoundary, byte[] attrValue) {
        if (attrBoundary != null) {
            for (int current = 0; current < attrBoundary.length-1; current++) {
                if (pos >= attrBoundary[current] &&
                    pos < attrBoundary[current+1]) {
                    return attrValue[current];
                }
            }
        }
        return View.IME_ATTR_INPUT_ERROR;
    }

    private static ObservableList<InputMethodTextRun> inputMethodEventComposed(
            String text, int commitCount, int[] clauseBoundary, int[] attrBoundary, byte[] attrValue)
    {
        ObservableList<InputMethodTextRun> composed = new TrackableObservableList<>() {
            @Override
            protected void onChanged(ListChangeListener.Change<InputMethodTextRun> c) {
            }
        };

        if (commitCount < text.length()) {
            if (clauseBoundary == null) {
                // Create one single segment as UNSELECTED_RAW
                composed.add(new InputMethodTextRun(
                        text.substring(commitCount),
                        InputMethodHighlight.UNSELECTED_RAW));
            } else {
                for (int current = 0; current < clauseBoundary.length-1; current++) {
                    if (clauseBoundary[current] < commitCount) {
                        continue;
                    }

                    InputMethodHighlight highlight;
                    switch (inputMethodEventAttrValue(clauseBoundary[current], attrBoundary, attrValue)) {
                        case View.IME_ATTR_TARGET_CONVERTED:
                            highlight = InputMethodHighlight.SELECTED_CONVERTED;
                            break;
                        case View.IME_ATTR_CONVERTED:
                            highlight = InputMethodHighlight.UNSELECTED_CONVERTED;
                            break;
                        case View.IME_ATTR_TARGET_NOTCONVERTED:
                            highlight = InputMethodHighlight.SELECTED_RAW;
                            break;
                        case View.IME_ATTR_INPUT:
                        case View.IME_ATTR_INPUT_ERROR:
                        default:
                            highlight = InputMethodHighlight.UNSELECTED_RAW;
                            break;
                    }
                    composed.add(new InputMethodTextRun(
                            text.substring(clauseBoundary[current],
                                           clauseBoundary[current+1]),
                            highlight));
                }
            }
        }
        return composed;
    }

    @SuppressWarnings("removal")
    @Override public void handleInputMethodEvent(final long time, final String text,
                                                 final int[] clauseBoundary,
                                                 final int[] attrBoundary, final byte[] attrValue,
                                                 final int commitCount, final int cursorPos)
    {
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.newInput("INPUT_METHOD_EVENT");
        }
        WindowStage stage = scene.getWindowStage();
        try {
            if (stage != null) {
                stage.setInAllowedEventHandler(true);
            }
            QuantumToolkit.runWithoutRenderLock(() -> {
                return AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    if (scene.sceneListener != null) {
                        String t = text != null ? text : "";
                        EventType<InputMethodEvent> eventType =
                                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED;
                        ObservableList<InputMethodTextRun> composed = inputMethodEventComposed(
                                t, commitCount, clauseBoundary, attrBoundary, attrValue);
                        String committed = t.substring(0, commitCount);
                        scene.sceneListener.inputMethodEvent(eventType, composed, committed, cursorPos);
                    }
                    return null;
                }, scene.getAccessControlContext());
            });
        } finally {
            if (stage != null) {
                stage.setInAllowedEventHandler(false);
            }
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newInput(null);
            }
        }
    }

    @Override
    public double[] getInputMethodCandidatePos(int offset) {
        Point2D p2d = scene.inputMethodRequests.getTextLocation(offset);
        double[] ret = new double[2];
        ret[0] = p2d.getX();
        ret[1] = p2d.getY();

        View view = scene.getPlatformView();
        if (view != null) {
            Window window = view.getWindow();
            if (window != null) {
                Screen screen = window.getScreen();
                if (screen != null) {
                    ret[0] = screen.toPlatformX((float) p2d.getX());
                    ret[1] = screen.toPlatformY((float) p2d.getY());
                }
            }
        }

        return ret;
    }

    private static TransferMode actionToTransferMode(int dropActions) {
        if (dropActions == Clipboard.ACTION_NONE) {
            return null;
        } else if (dropActions == Clipboard.ACTION_COPY
                //IE drop action for URL copy;  XXX: should be fixed in Glass
                || dropActions == (Clipboard.ACTION_COPY | Clipboard.ACTION_REFERENCE) )
        {
            return TransferMode.COPY;
        } else if (dropActions == Clipboard.ACTION_MOVE
                //IE drop action for URL move;  XXX: should be fixed in Glass
                || dropActions == (Clipboard.ACTION_MOVE | Clipboard.ACTION_REFERENCE) )
        {
            return TransferMode.MOVE;
        } else if (dropActions == Clipboard.ACTION_REFERENCE) {
            return TransferMode.LINK;
        } else if (dropActions == Clipboard.ACTION_COPY_OR_MOVE) {
            if (QuantumToolkit.verbose) {
                System.err.println("Ambiguous drop action: " + Integer.toHexString(dropActions));
            }
        } else {
            if (QuantumToolkit.verbose) {
                System.err.println("Unknown drop action: " + Integer.toHexString(dropActions));
            }
        }
        return null;
    }

    private static int transferModeToAction(TransferMode tm) {
        if (tm == null) {
            return Clipboard.ACTION_NONE;
        }

        switch (tm) {
            case COPY:
                return Clipboard.ACTION_COPY;
            case MOVE:
                return Clipboard.ACTION_MOVE;
            case LINK:
                return Clipboard.ACTION_REFERENCE;
            default:
                return Clipboard.ACTION_NONE;
        }
    }

    @Override public int handleDragEnter(View view,
                                         final int x, final int y, final int xAbs, final int yAbs,
                                         final int recommendedDropAction,
                                         final ClipboardAssistance dropTargetAssistant)
    {
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.newInput("DRAG_ENTER");
        }
        TransferMode action;
        try {
            action = QuantumToolkit.runWithoutRenderLock(() -> {
                return dndHandler.handleDragEnter(x, y, xAbs, yAbs,
                        actionToTransferMode(recommendedDropAction),
                        dropTargetAssistant);
            });
        } finally {
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newInput(null);
            }
        }
        return transferModeToAction(action);
    }

    @Override public void handleDragLeave(View view, final ClipboardAssistance dropTargetAssistant) {
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.newInput("DRAG_LEAVE");
        }
        try {
            QuantumToolkit.runWithoutRenderLock(() -> {
                dndHandler.handleDragLeave(dropTargetAssistant);
                return null;
            });
        } finally {
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newInput(null);
            }
        }
    }

    @Override public int handleDragDrop(View view,
                                        final int x, final int y, final int xAbs, final int yAbs,
                                        final int recommendedDropAction,
                                        final ClipboardAssistance dropTargetAssistant)
    {
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.newInput("DRAG_DROP");
        }
        TransferMode action;
        try {
            action = QuantumToolkit.runWithoutRenderLock(() -> {
                return dndHandler.handleDragDrop(x, y, xAbs, yAbs,
                    actionToTransferMode(recommendedDropAction),
                    dropTargetAssistant);
            });
        } finally {
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newInput(null);
            }
        }
        return transferModeToAction(action);
    }

    @Override public int handleDragOver(View view,
                                        final int x, final int y, final int xAbs, final int yAbs,
                                        final int recommendedDropAction,
                                        final ClipboardAssistance dropTargetAssistant)
    {
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.newInput("DRAG_OVER");
        }
        TransferMode action;
        try {
            action = QuantumToolkit.runWithoutRenderLock(() -> {
                return dndHandler.handleDragOver(x, y, xAbs, yAbs,
                    actionToTransferMode(recommendedDropAction),
                    dropTargetAssistant);
            });
        } finally {
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newInput(null);
            }
        }
        return transferModeToAction(action);
    }

    private ClipboardAssistance dropSourceAssistant;

    @Override public void handleDragStart(View view, final int button,
                                          final int x, final int y, final int xAbs, final int yAbs,
                                          final ClipboardAssistance assistant)
    {
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.newInput("DRAG_START");
        }
        dropSourceAssistant = assistant;
        try {
            QuantumToolkit.runWithoutRenderLock(() -> {
                dndHandler.handleDragStart(button, x, y, xAbs, yAbs, assistant);
                return null;
            });
        } finally {
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newInput(null);
            }
        }
    }

    @Override public void handleDragEnd(View view, final int performedAction) {
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.newInput("DRAG_END");
        }
        try {
            QuantumToolkit.runWithoutRenderLock(() -> {
                dndHandler.handleDragEnd(actionToTransferMode(performedAction), dropSourceAssistant);
                return null;
            });
        } finally {
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newInput(null);
            }
        }
    }

    // TODO - dropTargetListener.dropActionChanged
    // TODO - dragSourceListener.dropActionChanged

    private final ViewEventNotification viewNotification = new ViewEventNotification();
    private class ViewEventNotification implements PrivilegedAction<Void> {
        View view;
        long time;
        int type;

        @Override
        public Void run() {
            if (scene.sceneListener == null) {
                return null;
            }
            switch (type) {
                case ViewEvent.REPAINT: {
                    Window w = view.getWindow();
                    if (w != null && w.getMinimumWidth() == view.getWidth() && !w.isVisible()) {
                        // RT-21057 - ignore initial minimum size setting if not visible
                        break;
                    }
                    if (QuantumToolkit.drawInPaint && w != null && w.isVisible()) {
                        WindowStage stage = scene.getWindowStage();
                        if (stage != null) {
                            collector.liveRepaintRenderJob(scene);
                        }
                    }
                    scene.entireSceneNeedsRepaint();
                    break;
                }
                case ViewEvent.RESIZE: {
                    final Window w = view.getWindow();
                    float pScaleX = (w == null) ? 1.0f : w.getPlatformScaleX();
                    float pScaleY = (w == null) ? 1.0f : w.getPlatformScaleY();
                    scene.sceneListener.changedSize(view.getWidth()  / pScaleX,
                                                    view.getHeight() / pScaleY);
                    scene.entireSceneNeedsRepaint();
                    QuantumToolkit.runWithRenderLock(() -> {
                        scene.updateSceneState();
                        return null;
                    });
                    if (QuantumToolkit.liveResize && w != null && w.isVisible()) {
                        WindowStage stage = scene.getWindowStage();
                        if (stage != null) {
                            collector.liveRepaintRenderJob(scene);
                        }
                    }
                    break;
                }
                case ViewEvent.MOVE: {
                    // MOVE events can be "synthesized" and the window will
                    // be null if this is synthesized during a "REMOVE" event
                    final Window w = view.getWindow();
                    float pScaleX = (w == null) ? 1.0f : w.getPlatformScaleX();
                    float pScaleY = (w == null) ? 1.0f : w.getPlatformScaleY();
                    scene.sceneListener.changedLocation(view.getX() / pScaleX,
                                                        view.getY() / pScaleY);
                    break;
                }
                case ViewEvent.FULLSCREEN_ENTER:
                case ViewEvent.FULLSCREEN_EXIT:
                    if (scene.getWindowStage() != null) {
                        scene.getWindowStage().fullscreenChanged(type == ViewEvent.FULLSCREEN_ENTER);
                    }
                    break;
                case ViewEvent.ADD:
                case ViewEvent.REMOVE:
                    // unhandled
                    break;
                default:
                    throw new RuntimeException("handleViewEvent: unhandled type: " + type);
            }
            return null;
        }
    }

    @SuppressWarnings("removal")
    @Override public void handleViewEvent(View view, long time, final int type) {
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.newInput("VIEW_EVENT: "+ViewEvent.getTypeString(type));
        }
        viewNotification.view = view;
        viewNotification.time = time;
        viewNotification.type = type;
        try {
            QuantumToolkit.runWithoutRenderLock(() -> {
                return AccessController.doPrivileged(viewNotification, scene.getAccessControlContext());
            });
        }
        finally {
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newInput(null);
            }
        }
    }

    @SuppressWarnings("removal")
    @Override public void handleScrollGestureEvent(
            View view, final long time, final int type,
            final int modifiers, final boolean isDirect, final boolean isInertia, final int touchCount,
            final int x, final int y, final int xAbs, final int yAbs, final double dx, final double dy,
            final double totaldx, final double totaldy, final double multiplierX, final double multiplierY)
    {
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.newInput("SCROLL_GESTURE_EVENT");
        }
        WindowStage stage = scene.getWindowStage();
        try {
            if (stage != null) {
                stage.setInAllowedEventHandler(false);
            }
            QuantumToolkit.runWithoutRenderLock(() -> {
                return AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    if (scene.sceneListener != null) {
                        EventType<ScrollEvent> eventType;
                        switch(type) {
                            case GestureEvent.GESTURE_STARTED:
                                eventType = ScrollEvent.SCROLL_STARTED;
                                break;
                            case GestureEvent.GESTURE_PERFORMED:
                                eventType = ScrollEvent.SCROLL;
                                break;
                            case GestureEvent.GESTURE_FINISHED:
                                eventType = ScrollEvent.SCROLL_FINISHED;
                                break;
                            default:
                                throw new RuntimeException("Unknown scroll event type: " + type);
                        }
                        final Window w = view.getWindow();
                        double pScaleX, pScaleY, spx, spy, sx, sy;
                        if (w != null) {
                            pScaleX = w.getPlatformScaleX();
                            pScaleY = w.getPlatformScaleY();
                            Screen scr = w.getScreen();
                            if (scr != null) {
                                spx = scr.getPlatformX();
                                spy = scr.getPlatformY();
                                sx = scr.getX();
                                sy = scr.getY();
                            } else {
                                spx = spy = sx = sy = 0.0;
                            }
                        } else {
                            pScaleX = pScaleY = 1.0;
                            spx = spy = sx = sy = 0.0;
                        }
                        scene.sceneListener.scrollEvent(eventType,
                                dx / pScaleX, dy / pScaleY, totaldx / pScaleX, totaldy / pScaleY,
                                multiplierX, multiplierY,
                                touchCount,
                                0, 0, 0, 0,
                                x == View.GESTURE_NO_VALUE ? Double.NaN : x / pScaleX,
                                y == View.GESTURE_NO_VALUE ? Double.NaN : y / pScaleY,
                                xAbs == View.GESTURE_NO_VALUE ? Double.NaN : sx + (xAbs - spx) / pScaleX,
                                yAbs == View.GESTURE_NO_VALUE ? Double.NaN : sy + (yAbs - spy) / pScaleY,
                                (modifiers & KeyEvent.MODIFIER_SHIFT) != 0,
                                (modifiers & KeyEvent.MODIFIER_CONTROL) != 0,
                                (modifiers & KeyEvent.MODIFIER_ALT) != 0,
                                (modifiers & KeyEvent.MODIFIER_WINDOWS) != 0,
                                isDirect, isInertia);
                    }
                    return null;
                }, scene.getAccessControlContext());
            });
        } finally {
            if (stage != null) {
                stage.setInAllowedEventHandler(false);
            }
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newInput(null);
            }
        }
    }

    @SuppressWarnings("removal")
    @Override public void handleZoomGestureEvent(
            View view, final long time, final int type,
            final int modifiers, final boolean isDirect, final boolean isInertia,
            final int originx, final int originy,
            final int originxAbs, final int originyAbs,
            final double scale, double expansion,
            final double totalscale, double totalexpansion)
    {
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.newInput("ZOOM_GESTURE_EVENT");
        }
        WindowStage stage = scene.getWindowStage();
        try {
            if (stage != null) {
                stage.setInAllowedEventHandler(false);
            }
            QuantumToolkit.runWithoutRenderLock(() -> {
                return AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    if (scene.sceneListener != null) {
                        EventType<ZoomEvent> eventType;
                        switch (type) {
                            case GestureEvent.GESTURE_STARTED:
                                eventType = ZoomEvent.ZOOM_STARTED;
                                break;
                            case GestureEvent.GESTURE_PERFORMED:
                                eventType = ZoomEvent.ZOOM;
                                break;
                            case GestureEvent.GESTURE_FINISHED:
                                eventType = ZoomEvent.ZOOM_FINISHED;
                                break;
                            default:
                                throw new RuntimeException("Unknown scroll event type: " + type);
                        }
                        final Window w = view.getWindow();
                        double pScaleX, pScaleY, spx, spy, sx, sy;
                        if (w != null) {
                            pScaleX = w.getPlatformScaleX();
                            pScaleY = w.getPlatformScaleY();
                            Screen scr = w.getScreen();
                            if (scr != null) {
                                spx = scr.getPlatformX();
                                spy = scr.getPlatformY();
                                sx = scr.getX();
                                sy = scr.getY();
                            } else {
                                spx = spy = sx = sy = 0.0;
                            }
                        } else {
                            pScaleX = pScaleY = 1.0;
                            spx = spy = sx = sy = 0.0;
                        }
                        // REMIND: Scale the [total]scale params too?
                        scene.sceneListener.zoomEvent(eventType, scale, totalscale,
                                originx == View.GESTURE_NO_VALUE ? Double.NaN : originx / pScaleX,
                                originy == View.GESTURE_NO_VALUE ? Double.NaN : originy / pScaleY,
                                originxAbs == View.GESTURE_NO_VALUE ? Double.NaN : sx + (originxAbs - spx) / pScaleX,
                                originyAbs == View.GESTURE_NO_VALUE ? Double.NaN : sy + (originyAbs - spy) / pScaleY,
                                (modifiers & KeyEvent.MODIFIER_SHIFT) != 0,
                                (modifiers & KeyEvent.MODIFIER_CONTROL) != 0,
                                (modifiers & KeyEvent.MODIFIER_ALT) != 0,
                                (modifiers & KeyEvent.MODIFIER_WINDOWS) != 0,
                                isDirect, isInertia);
                    }
                    return null;
                }, scene.getAccessControlContext());
            });
        } finally {
            if (stage != null) {
                stage.setInAllowedEventHandler(false);
            }
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newInput(null);
            }
        }
    }

    @SuppressWarnings("removal")
    @Override public void handleRotateGestureEvent(
            View view, final long time, final int type,
            final int modifiers, final boolean isDirect, final boolean isInertia,
            final int originx, final int originy,
            final int originxAbs, final int originyAbs,
            final double dangle, final double totalangle)
    {
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.newInput("ROTATE_GESTURE_EVENT");
        }
        WindowStage stage = scene.getWindowStage();
        try {
            if (stage != null) {
                stage.setInAllowedEventHandler(false);
            }
            QuantumToolkit.runWithoutRenderLock(() -> {
                return AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    if (scene.sceneListener != null) {
                        EventType<RotateEvent> eventType;
                        switch (type) {
                            case GestureEvent.GESTURE_STARTED:
                                eventType = RotateEvent.ROTATION_STARTED;
                                break;
                            case GestureEvent.GESTURE_PERFORMED:
                                eventType = RotateEvent.ROTATE;
                                break;
                            case GestureEvent.GESTURE_FINISHED:
                                eventType = RotateEvent.ROTATION_FINISHED;
                                break;
                            default:
                                throw new RuntimeException("Unknown scroll event type: " + type);
                        }
                        final Window w = view.getWindow();
                        double pScaleX, pScaleY, spx, spy, sx, sy;
                        if (w != null) {
                            pScaleX = w.getPlatformScaleX();
                            pScaleY = w.getPlatformScaleY();
                            Screen scr = w.getScreen();
                            if (scr != null) {
                                spx = scr.getPlatformX();
                                spy = scr.getPlatformY();
                                sx = scr.getX();
                                sy = scr.getY();
                            } else {
                                spx = spy = sx = sy = 0.0;
                            }
                        } else {
                            pScaleX = pScaleY = 1.0;
                            spx = spy = sx = sy = 0.0;
                        }
                        scene.sceneListener.rotateEvent(eventType, dangle, totalangle,
                                originx == View.GESTURE_NO_VALUE ? Double.NaN : originx / pScaleX,
                                originy == View.GESTURE_NO_VALUE ? Double.NaN : originy / pScaleY,
                                originxAbs == View.GESTURE_NO_VALUE ? Double.NaN : sx + (originxAbs - spx) / pScaleX,
                                originyAbs == View.GESTURE_NO_VALUE ? Double.NaN : sy + (originyAbs - spy) / pScaleY,
                                (modifiers & KeyEvent.MODIFIER_SHIFT) != 0,
                                (modifiers & KeyEvent.MODIFIER_CONTROL) != 0,
                                (modifiers & KeyEvent.MODIFIER_ALT) != 0,
                                (modifiers & KeyEvent.MODIFIER_WINDOWS) != 0,
                                isDirect, isInertia);
                    }
                    return null;
                }, scene.getAccessControlContext());
            });
        } finally {
            if (stage != null) {
                stage.setInAllowedEventHandler(false);
            }
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newInput(null);
            }
        }
    }

    @SuppressWarnings("removal")
    @Override public void handleSwipeGestureEvent(
            View view, final long time, int type,
            final int modifiers, final boolean isDirect,
            boolean isInertia, final int touchCount,
            final int dir, final int x, final int y, final int xAbs, final int yAbs)
    {
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.newInput("SWIPE_EVENT");
        }
        WindowStage stage = scene.getWindowStage();
        try {
            if (stage != null) {
                stage.setInAllowedEventHandler(false);
            }
            QuantumToolkit.runWithoutRenderLock(() -> {
                return AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    if (scene.sceneListener != null) {
                        EventType<SwipeEvent> eventType;
                        switch (dir) {
                            case SwipeGesture.DIR_UP:
                                eventType = SwipeEvent.SWIPE_UP;
                                break;
                            case SwipeGesture.DIR_DOWN:
                                eventType = SwipeEvent.SWIPE_DOWN;
                                break;
                            case SwipeGesture.DIR_LEFT:
                                eventType = SwipeEvent.SWIPE_LEFT;
                                break;
                            case SwipeGesture.DIR_RIGHT:
                                eventType = SwipeEvent.SWIPE_RIGHT;
                                break;
                            default:
                                throw new RuntimeException("Unknown swipe event direction: " + dir);
                        }
                        final Window w = view.getWindow();
                        double pScaleX, pScaleY, spx, spy, sx, sy;
                        if (w != null) {
                            pScaleX = w.getPlatformScaleX();
                            pScaleY = w.getPlatformScaleY();
                            Screen scr = w.getScreen();
                            if (scr != null) {
                                spx = scr.getPlatformX();
                                spy = scr.getPlatformY();
                                sx = scr.getX();
                                sy = scr.getY();
                            } else {
                                spx = spy = sx = sy = 0.0;
                            }
                        } else {
                            pScaleX = pScaleY = 1.0;
                            spx = spy = sx = sy = 0.0;
                        }
                        scene.sceneListener.swipeEvent(eventType, touchCount,
                                x == View.GESTURE_NO_VALUE ? Double.NaN : x / pScaleX,
                                y == View.GESTURE_NO_VALUE ? Double.NaN : y / pScaleY,
                                xAbs == View.GESTURE_NO_VALUE ? Double.NaN : sx + (xAbs - spx) / pScaleX,
                                yAbs == View.GESTURE_NO_VALUE ? Double.NaN : sy + (yAbs - spy) / pScaleY,
                                (modifiers & KeyEvent.MODIFIER_SHIFT) != 0,
                                (modifiers & KeyEvent.MODIFIER_CONTROL) != 0,
                                (modifiers & KeyEvent.MODIFIER_ALT) != 0,
                                (modifiers & KeyEvent.MODIFIER_WINDOWS) != 0,
                                isDirect);
                    }
                    return null;
                }, scene.getAccessControlContext());
            });
        } finally {
            if (stage != null) {
                stage.setInAllowedEventHandler(false);
            }
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newInput(null);
            }
        }
    }

    @SuppressWarnings("removal")
    @Override public void handleBeginTouchEvent(
            View view, final long time, final int modifiers,
            final boolean isDirect, final int touchEventCount)
    {
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.newInput("BEGIN_TOUCH_EVENT");
        }
        WindowStage stage = scene.getWindowStage();
        try {
            if (stage != null) {
                stage.setInAllowedEventHandler(true);
            }
            QuantumToolkit.runWithoutRenderLock(() -> {
                return AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    if (scene.sceneListener != null) {
                        scene.sceneListener.touchEventBegin(time, touchEventCount,
                                isDirect,
                                (modifiers & KeyEvent.MODIFIER_SHIFT) != 0,
                                (modifiers & KeyEvent.MODIFIER_CONTROL) != 0,
                                (modifiers & KeyEvent.MODIFIER_ALT) != 0,
                                (modifiers & KeyEvent.MODIFIER_WINDOWS) != 0);
                    }
                    return null;
                }, scene.getAccessControlContext());
            });
        } finally {
            if (stage != null) {
                stage.setInAllowedEventHandler(false);
            }
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newInput(null);
            }
        }

        gestures.notifyBeginTouchEvent(time, modifiers, isDirect, touchEventCount);
    }

    @SuppressWarnings("removal")
    @Override public void handleNextTouchEvent(
            View view, final long time, final int type, final long touchId,
            final int x, final int y, final int xAbs, final int yAbs)
    {
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.newInput("NEXT_TOUCH_EVENT");
        }
        WindowStage stage = scene.getWindowStage();
        try {
            if (stage != null) {
                stage.setInAllowedEventHandler(true);
            }
            QuantumToolkit.runWithoutRenderLock(() -> {
                    return AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    if (scene.sceneListener != null) {
                        TouchPoint.State state;
                        switch (type) {
                            case TouchEvent.TOUCH_PRESSED:
                                state = TouchPoint.State.PRESSED;
                                break;
                            case TouchEvent.TOUCH_MOVED:
                                state = TouchPoint.State.MOVED;
                                break;
                            case TouchEvent.TOUCH_STILL:
                                state = TouchPoint.State.STATIONARY;
                                break;
                            case TouchEvent.TOUCH_RELEASED:
                                state = TouchPoint.State.RELEASED;
                                break;
                            default:
                                throw new RuntimeException("Unknown touch state: " + type);
                        }
                        final Window w = view.getWindow();
                        double pScaleX, pScaleY, spx, spy, sx, sy;
                        if (w != null) {
                            pScaleX = w.getPlatformScaleX();
                            pScaleY = w.getPlatformScaleY();
                            Screen scr = w.getScreen();
                            if (scr != null) {
                                spx = scr.getPlatformX();
                                spy = scr.getPlatformY();
                                sx = scr.getX();
                                sy = scr.getY();
                            } else {
                                spx = spy = sx = sy = 0.0;
                            }
                        } else {
                            pScaleX = pScaleY = 1.0;
                            spx = spy = sx = sy = 0.0;
                        }
                        scene.sceneListener.touchEventNext(state, touchId,
                                x / pScaleX, y / pScaleY,
                                sx + (xAbs - spx) / pScaleX,
                                sy + (yAbs - spy) / pScaleY);
                    }
                    return null;
                }, scene.getAccessControlContext());
            });
        } finally {
            if (stage != null) {
                stage.setInAllowedEventHandler(false);
            }
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newInput(null);
            }
        }

        gestures.notifyNextTouchEvent(time, type, touchId, x, y, xAbs, yAbs);
    }

    @SuppressWarnings("removal")
    @Override public void handleEndTouchEvent(View view, long time) {
        if (PULSE_LOGGING_ENABLED) {
            PulseLogger.newInput("END_TOUCH_EVENT");
        }
        WindowStage stage = scene.getWindowStage();
        try {
            if (stage != null) {
                stage.setInAllowedEventHandler(true);
            }
            QuantumToolkit.runWithoutRenderLock(() -> {
                return AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    if (scene.sceneListener != null) {
                        scene.sceneListener.touchEventEnd();
                    }
                    return null;
                }, scene.getAccessControlContext());
            });
        } finally {
            if (stage != null) {
                stage.setInAllowedEventHandler(false);
            }
            if (PULSE_LOGGING_ENABLED) {
                PulseLogger.newInput(null);
            }
        }

        gestures.notifyEndTouchEvent(time);
    }

    @Override
    public Accessible getSceneAccessible() {
        if (scene != null && scene.sceneListener != null) {
            return scene.sceneListener.getSceneAccessible();
        }
        return null;
    }
}
