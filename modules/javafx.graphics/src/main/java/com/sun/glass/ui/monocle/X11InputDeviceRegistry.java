/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

import com.sun.glass.events.MouseEvent;

/** InputDeviceRegistry used when the system property Dx11.input=true is set,
 * indicating that we would like to receive input events through Xlib instead
 * of directly from Linux input devices.
 */
class X11InputDeviceRegistry extends InputDeviceRegistry {

    private MouseState state;
    private static X xLib = X.getX();

    X11InputDeviceRegistry() {
        InputDevice device = new InputDevice() {
            @Override
            public boolean isTouch() {
                return false;
            }

            @Override
            public boolean isMultiTouch() {
                return false;
            }

            @Override
            public boolean isRelative() {
                return true;
            }

            @Override
            public boolean is5Way() {
                return false;
            }

            @Override
            public boolean isFullKeyboard() {
                return false;
            }
        };
        // Start the thread to listen to the X11 event queue
        Thread x11InputThread = new Thread(() -> {
            NativePlatform platform =
                    NativePlatformFactory.getNativePlatform();
            X11Screen screen = (X11Screen) platform.getScreen();
            long display = screen.getDisplay();
            long window = screen.getNativeHandle();
            RunnableProcessor runnableProcessor =
                    platform.getRunnableProcessor();
            runnableProcessor.invokeLater(() -> {
                devices.add(device);
            });
            state = new MouseState();
            X.XEvent event = new X.XEvent();
            while (true) {
                xLib.XNextEvent(display, event.p);
                if (X.XEvent.getWindow(event.p) != window) {
                    continue;
                }
                processXEvent(event, runnableProcessor);
            }
        });
        x11InputThread.setName("X11 Input");
        x11InputThread.setDaemon(true);
        x11InputThread.start();
    }

    /** Dispatch the X Event to the appropriate processor.  All processors run
     * via invokeLater
     * @param event
     * @param runnableProcessor
     */
    private void processXEvent(X.XEvent event,
                               RunnableProcessor runnableProcessor) {
        switch (X.XEvent.getType(event.p)) {
            case X.ButtonPress: {
                X.XButtonEvent buttonEvent = new X.XButtonEvent(event);
                int button = X.XButtonEvent.getButton(buttonEvent.p);
                runnableProcessor.invokeLater(new ButtonPressProcessor(button));
                break;
            }
            case X.ButtonRelease: {
                X.XButtonEvent buttonEvent = new X.XButtonEvent(event);
                int button = X.XButtonEvent.getButton(buttonEvent.p);
                runnableProcessor.invokeLater(
                        new ButtonReleaseProcessor(button));
                break;
            }
            case X.MotionNotify: {
                X.XMotionEvent motionEvent = new X.XMotionEvent(event);
                int x = X.XMotionEvent.getX(motionEvent.p);
                int y = X.XMotionEvent.getY(motionEvent.p);
                runnableProcessor.invokeLater(new MotionProcessor(x, y));
                break;
            }
        }
    }

    private class ButtonPressProcessor implements Runnable {
        private int button;
        ButtonPressProcessor(int button) {
            this.button = button;
        }
        @Override
        public void run() {
            MouseInput.getInstance().getState(state);
            int glassButton = buttonToGlassButton(button);
            if (glassButton != MouseEvent.BUTTON_NONE) {
                state.pressButton(glassButton);
            }
            MouseInput.getInstance().setState(state, false);
        }
    }

    private class ButtonReleaseProcessor implements Runnable {
        private int button;
        ButtonReleaseProcessor(int button) {
            this.button = button;
        }
        @Override
        public void run() {
            MouseInput.getInstance().getState(state);
            int glassButton = buttonToGlassButton(button);
            if (glassButton != MouseEvent.BUTTON_NONE) {
                state.releaseButton(glassButton);
            }
            MouseInput.getInstance().setState(state, false);
        }
    }

    private class MotionProcessor implements Runnable {
        private int x;
        private int y;
        MotionProcessor(int x, int y) {
            this.x = x;
            this.y = y;
        }
        @Override
        public void run() {
            MouseInput.getInstance().getState(state);
            state.setX(x);
            state.setY(y);
            MouseInput.getInstance().setState(state, false);
        }
    }

    /** Convenience method to convert X11 button codes to glass button codes
     *
     * @param button
     */
    private static int buttonToGlassButton(int button) {
        switch (button) {
            case X.Button1: return MouseEvent.BUTTON_LEFT;
            case X.Button2: return MouseEvent.BUTTON_OTHER;
            case X.Button3: return MouseEvent.BUTTON_RIGHT;
            default: return MouseEvent.BUTTON_NONE;
        }
    }

}
