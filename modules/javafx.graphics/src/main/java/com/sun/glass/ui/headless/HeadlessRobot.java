/*
 * Copyright (c) 2025, Gluon. All rights reserved.
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
package com.sun.glass.ui.headless;

import com.sun.glass.events.KeyEvent;
import com.sun.glass.events.MouseEvent;
import com.sun.glass.ui.Application;
import com.sun.glass.ui.GlassRobot;
import com.sun.glass.ui.Window;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;

public class HeadlessRobot extends GlassRobot {

    final int multiplierX = 40;
    final int multiplierY = 40;
    private final HeadlessApplication application;
    private HeadlessWindow activeWindow = null;

    private double mouseX, mouseY;

    private final SpecialKeys specialKeys = new SpecialKeys();
    private final MouseState mouseState = new MouseState();
    private final char[] NO_CHAR = { };

    public HeadlessRobot(HeadlessApplication application) {
        this.application = application;
    }

    void windowAdded(HeadlessWindow window) {
        if (this.activeWindow == null) activeWindow = window;
    }

    void windowRemoved(HeadlessWindow window) {
        if (this.activeWindow == window) activeWindow = null;
    }

    @Override
    public void create() {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void keyPress(KeyCode keyCode) {
        checkWindowFocused();
        if (activeWindow == null) return;
        HeadlessView view = (HeadlessView)activeWindow.getView();
        int code = keyCode.getCode();
        processSpecialKeys(code, true);
        char[] keyval = getKeyChars(code);
        int mods = getKeyModifiers();
        if (view != null) {
            view.notifyKey(KeyEvent.PRESS, code, keyval, mods);
            if (keyval.length > 0) {
                view.notifyKey(KeyEvent.TYPED, 0, keyval, mods);
            }
        }
    }

    @Override
    public void keyRelease(KeyCode keyCode) {
        checkWindowFocused();
        if (activeWindow == null) return;
        HeadlessView view = (HeadlessView)activeWindow.getView();
        int code = keyCode.getCode();
        processSpecialKeys(code, false);
        int mods = getKeyModifiers();
        char[] keyval = new char[1];
        keyval[0] = (char) code;
        if (view != null) {
            view.notifyKey(KeyEvent.RELEASE, code, keyval, mods);
        }
    }

    @Override
    public double getMouseX() {
        return this.mouseX;
    }

    @Override
    public double getMouseY() {
        return this.mouseY;
    }

    @Override
    public void mouseMove(double x, double y) {
        this.mouseX = x;
        this.mouseY = y;
        checkWindowEnterExit();
        if (activeWindow == null) return;
        HeadlessView view = (HeadlessView)activeWindow.getView();
        if (view == null) return;
        int wx = activeWindow.getX();
        int wy = activeWindow.getY();
        int buttonEvent = MouseEvent.BUTTON_NONE;
        int mouseEvent = MouseEvent.MOVE;
        if (mouseState.pressedButtons.size() > 0) {
            MouseButton button = mouseState.pressedButtons.stream().findFirst().get();
            buttonEvent = getGlassEventButton(button);
            mouseEvent = MouseEvent.DRAG;
        }
        int modifiers = 0;
        view.notifyMouse(mouseEvent, buttonEvent, (int)mouseX-wx, (int)mouseY-wy, (int)mouseX, (int)mouseY, modifiers, false, false);
    }

    @Override
    public void mousePress(MouseButton... buttons) {
        Application.checkEventThread();
        mouseState.pressedButtons.addAll(Arrays.asList(buttons));
        checkWindowEnterExit();
        HeadlessView view = (HeadlessView)activeWindow.getView();
        if (view == null) {
            view = (HeadlessView)activeWindow.getView();
            if (view == null) {
                return;
            }
        }
        int wx = activeWindow.getX();
        int wy = activeWindow.getY();
        int modifiers = getModifiers(buttons);
        int buttonCode = getGlassEventButton(buttons);
        view.notifyMouse(MouseEvent.DOWN, buttonCode, (int)mouseX-wx, (int)mouseY-wy, (int)mouseX, (int)mouseY, modifiers, true, true);
        if (buttonCode == MouseEvent.BUTTON_RIGHT) {
            view.notifyMenu((int)mouseX-wx, (int)mouseY-wy, (int)mouseX, (int)mouseY, false);
        }
    }

    @Override
    public void mouseRelease(MouseButton... buttons) {
        Application.checkEventThread();
        mouseState.pressedButtons.removeAll(Arrays.asList(buttons));
        checkWindowEnterExit();
        if (this.activeWindow == null) {
            return;
        }
        HeadlessView view = (HeadlessView) activeWindow.getView();
        int wx = activeWindow.getX();
        int wy = activeWindow.getY();
        int modifiers = getModifiers(buttons);
        view.notifyMouse(MouseEvent.UP, getGlassEventButton(buttons), (int) mouseX - wx, (int) mouseY - wy, (int) mouseX, (int) mouseY, modifiers, true, true);
    }

    @Override
    public void mouseWheel(int wheelAmt) {
        checkWindowFocused();

        final int dff = wheelAmt > 0 ? -1 : 1;
        HeadlessView view = (HeadlessView) activeWindow.getView();

        int wx = activeWindow.getX();
        int wy = activeWindow.getY();
        int repeat = Math.abs(wheelAmt);
        for (int i = 0; i < repeat; i++) {
            view.notifyScroll((int) mouseX, (int) mouseY, wx, wy, 0, dff, 0, 0, 0, 0, 0, multiplierX, multiplierY);
        }
    }

    @Override
    public Color getPixelColor(double x, double y) {
        HeadlessWindow topWindow = getTopWindow();
        return topWindow.getColor((int)x, (int) y);
    }

    @Override
    public WritableImage getScreenCapture(WritableImage image, double x, double y, double width, double height, boolean scaleToFit) {
        return super.getScreenCapture(image, x, y, width, height, scaleToFit);
    }

    @Override
    public void getScreenCapture(int x, int y, int width, int height, int[] data, boolean scaleToFit) {
        checkWindowFocused();
        activeWindow.getScreenCapture(x, y, width, height, data, scaleToFit);
    }

    private void checkActiveWindowExists() {
        if ((this.activeWindow != null) && (!this.activeWindow.isVisible())) {
            this.activeWindow = null;
        }
    }
    private void checkWindowFocused() {
        checkActiveWindowExists();
        this.activeWindow = getFocusedWindow();
    }

    private void checkWindowEnterExit() {
        checkActiveWindowExists();
        Window oldWindow = activeWindow;
        this.activeWindow = getTargetWindow(this.mouseX, this.mouseY);

        if (this.activeWindow == null) {
            if (oldWindow != null) {
                HeadlessView oldView = (HeadlessView)oldWindow.getView();
                if (oldView != null) {
                    oldView.notifyMouse(MouseEvent.EXIT, MouseEvent.BUTTON_NONE, 0, 0,0,0, 0, true, true);
                }
            }
            return;
        }
        int wx = activeWindow.getX();
        int wy = activeWindow.getY();

        if (activeWindow != oldWindow) {
            HeadlessView view = (HeadlessView)activeWindow.getView();
            int modifiers = 0;
            view.notifyMouse(MouseEvent.ENTER, MouseEvent.BUTTON_NONE, (int) mouseX - wx, (int) mouseY - wy, (int) mouseX, (int) mouseY, modifiers, true, true);
            if (oldWindow != null) {
                HeadlessView oldView = (HeadlessView)oldWindow.getView();
                if (oldView != null) {
                    int owx = oldWindow.getX();
                    int owy = oldWindow.getY();
                    oldView.notifyMouse(MouseEvent.EXIT, MouseEvent.BUTTON_NONE, (int) mouseX - owx, (int) mouseY - owy, (int) mouseX, (int) mouseY, modifiers, true, true);
                }
            }
        }
    }

    private HeadlessWindow getTopWindow() {
        List<Window> windows = Window.getWindows().stream()
                .filter(win -> win.getView() != null)
                .filter(win -> !win.isClosed())
                .filter(win -> !win.isMinimized()).toList();
        if (windows.isEmpty()) return null;
        return (HeadlessWindow)windows.get(windows.size() -1);
    }

    private HeadlessWindow getFocusedWindow() {
        List<Window> windows = Window.getWindows().stream()
                .filter(win -> win.getView()!= null)
                .filter(win -> !win.isClosed())
                .filter(win -> win.isFocused()).toList();
        if (windows.isEmpty()) return null;
        return (HeadlessWindow)windows.get(windows.size() -1);
    }

    private HeadlessWindow getTargetWindow(double x, double y) {
        List<Window> windows = Window.getWindows().stream()
                .filter(win -> win.getView()!= null)
                .filter(win -> !win.isClosed())
                .filter(win -> (x >= win.getX() && x <= win.getX() + win.getWidth()
                        && y >= win.getY() && y <= win.getY()+ win.getHeight())).toList();
        if (windows.isEmpty()) {
            return null;
        }
        if (windows.size() == 1) return (HeadlessWindow)windows.get(0);
        return (HeadlessWindow)windows.get(windows.size() -1);
    }

    int getModifiers(MouseButton... buttons) {
        int modifiers = KeyEvent.MODIFIER_NONE;
        for (int i = 0; i < buttons.length; i++) {
            modifiers |= switch (buttons[i]) {
                case NONE -> KeyEvent.MODIFIER_NONE;
                case PRIMARY -> KeyEvent.MODIFIER_BUTTON_PRIMARY;
                case MIDDLE -> KeyEvent.MODIFIER_BUTTON_MIDDLE;
                case SECONDARY -> KeyEvent.MODIFIER_BUTTON_SECONDARY;
                case BACK -> KeyEvent.MODIFIER_BUTTON_BACK;
                case FORWARD -> KeyEvent.MODIFIER_BUTTON_FORWARD;
            };
        }
        return modifiers;
    }

    int getGlassEventButton(MouseButton[] buttons) {
        if ((buttons == null) || (buttons.length == 0)) return 0;
        return getGlassEventButton(buttons[0]);
    }

    int getGlassEventButton(MouseButton button) {
        return switch (button) {
            case NONE -> MouseEvent.BUTTON_NONE;
            case PRIMARY -> MouseEvent.BUTTON_LEFT;
            case MIDDLE -> MouseEvent.BUTTON_OTHER;
            case SECONDARY -> MouseEvent.BUTTON_RIGHT;
            case BACK -> MouseEvent.BUTTON_BACK;
            case FORWARD -> MouseEvent.BUTTON_FORWARD;
        };
    }

    private void processSpecialKeys(int c, boolean on) {
        if (c == KeyEvent.VK_CONTROL) {
            this.specialKeys.keyControl = on;
        }
        if (c == KeyEvent.VK_SHIFT) {
            this.specialKeys.keyShift = on;
        }
        if (c == KeyEvent.VK_COMMAND) {
            this.specialKeys.keyCommand = on;
        }
        if (c == KeyEvent.VK_ALT) {
            this.specialKeys.keyAlt = on;
        }
    }
    private char[] getKeyChars(int key) {
        char c = '\000';
        boolean shifted = this.specialKeys.keyShift;
        // TODO: implement configurable keyboard mappings.
        // The following is only for US keyboards
        if (key >= KeyEvent.VK_A && key <= KeyEvent.VK_Z) {
            shifted ^= this.specialKeys.capsLock;
            if (shifted) {
                c = (char) (key - KeyEvent.VK_A + 'A');
            } else {
                c = (char) (key - KeyEvent.VK_A + 'a');
            }
        } else if (key >= KeyEvent.VK_NUMPAD0 && key <= KeyEvent.VK_NUMPAD9) {
            if (this.specialKeys.numLock) {
                c = (char) (key - KeyEvent.VK_NUMPAD0 + '0');
            }
        } else if (key >= KeyEvent.VK_0 && key <= KeyEvent.VK_9) {
            if (shifted) {
                switch (key) {
                    case KeyEvent.VK_0: c = ')'; break;
                    case KeyEvent.VK_1: c = '!'; break;
                    case KeyEvent.VK_2: c = '@'; break;
                    case KeyEvent.VK_3: c = '#'; break;
                    case KeyEvent.VK_4: c = '$'; break;
                    case KeyEvent.VK_5: c = '%'; break;
                    case KeyEvent.VK_6: c = '^'; break;
                    case KeyEvent.VK_7: c = '&'; break;
                    case KeyEvent.VK_8: c = '*'; break;
                    case KeyEvent.VK_9: c = '('; break;
                }
            } else {
                c = (char) (key - KeyEvent.VK_0 + '0');
            }
        } else if (key == KeyEvent.VK_SPACE) {
            c = ' ';
        } else if (key == KeyEvent.VK_TAB) {
            c = '\t';
        } else if (key == KeyEvent.VK_ENTER) {
            c = (char)13;
        } else if (key == KeyEvent.VK_MULTIPLY) {
            c = '*';
        } else if (key == KeyEvent.VK_DIVIDE) {
            c = '/';
        } else if (shifted) {
            switch (key) {
                case KeyEvent.VK_BACK_QUOTE: c = '~'; break;
                case KeyEvent.VK_COMMA: c = '<'; break;
                case KeyEvent.VK_PERIOD: c = '>'; break;
                case KeyEvent.VK_SLASH: c = '?'; break;
                case KeyEvent.VK_SEMICOLON: c = ':'; break;
                case KeyEvent.VK_QUOTE: c = '\"'; break;
                case KeyEvent.VK_BRACELEFT: c = '{'; break;
                case KeyEvent.VK_BRACERIGHT: c = '}'; break;
                case KeyEvent.VK_BACK_SLASH: c = '|'; break;
                case KeyEvent.VK_MINUS: c = '_'; break;
                case KeyEvent.VK_EQUALS: c = '+'; break;
            }        } else {
            switch (key) {
                case KeyEvent.VK_BACK_QUOTE: c = '`'; break;
                case KeyEvent.VK_COMMA: c = ','; break;
                case KeyEvent.VK_PERIOD: c = '.'; break;
                case KeyEvent.VK_SLASH: c = '/'; break;
                case KeyEvent.VK_SEMICOLON: c = ';'; break;
                case KeyEvent.VK_QUOTE: c = '\''; break;
                case KeyEvent.VK_BRACELEFT: c = '['; break;
                case KeyEvent.VK_BRACERIGHT: c = ']'; break;
                case KeyEvent.VK_BACK_SLASH: c = '\\'; break;
                case KeyEvent.VK_MINUS: c = '-'; break;
                case KeyEvent.VK_EQUALS: c = '='; break;
            }
        }
        return c == '\000' ? NO_CHAR : new char[] { c };
    }


    private int getKeyModifiers() {
        int answer = 0;
        if (this.specialKeys.keyControl) answer |= KeyEvent.MODIFIER_CONTROL;
        if (this.specialKeys.keyShift) answer |= KeyEvent.MODIFIER_SHIFT;
        if (this.specialKeys.keyCommand) answer |= KeyEvent.MODIFIER_COMMAND;
        if (this.specialKeys.keyAlt) answer |= KeyEvent.MODIFIER_ALT;
        return answer;
    }

    class SpecialKeys {
        boolean keyControl;
        boolean keyShift;
        boolean keyCommand;
        boolean keyAlt;
        boolean capsLock;
        boolean numLock;
    }

    class MouseState {
        final HashSet<MouseButton> pressedButtons = new HashSet<>();
    }
}
