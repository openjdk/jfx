/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.events.WindowEvent;
import com.sun.glass.ui.Screen;
import com.sun.javafx.tk.Toolkit;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.application.Platform;

final class MonocleWindowManager {

    private static MonocleWindowManager instance = new MonocleWindowManager();

    /** The window stack. Windows are in Z-order, from back to front. */
    private MonocleWindow[] windows = new MonocleWindow[0];
    private int nextID = 1;

    private MonocleWindow focusedWindow = null;

    private MonocleWindowManager() {
        //singleton
    }

    static MonocleWindowManager getInstance() {
        return instance;
    }

    private int getWindowIndex(MonocleWindow window) {
        for (int i = 0; i < windows.length; i++) {
            // Any two MonocleWindow objects represent different windows, so
            // equality can be determined by reference comparison.
            if (windows[i] == window) {
                return i;
            }
        }
        return -1;
    }
    void toBack(MonocleWindow window) {
        int index = getWindowIndex(window);
        if (index != 0 && index != -1) {
            System.arraycopy(windows, 0, windows, 1, index);
            windows[0] = window;
        }
    }

    void toFront(MonocleWindow window) {
        int index = getWindowIndex(window);
        if (index != windows.length - 1 && index != -1) {
            System.arraycopy(windows, index + 1, windows, index,
                             windows.length - index - 1);
            windows[windows.length - 1] = window;
        }
    }

    int addWindow(MonocleWindow window) {
        int index = getWindowIndex(window);
        if (index == -1) {
            windows = Arrays.copyOf(windows, windows.length + 1);
            windows[windows.length - 1] = window;
        }
        return nextID++;

    }

    boolean closeWindow(MonocleWindow window) {
        int index = getWindowIndex(window);
        if (index != -1) {
            System.arraycopy(windows, index + 1, windows, index,
                             windows.length - index - 1);
            windows = Arrays.copyOf(windows, windows.length - 1);
        }
        List<MonocleWindow> windowsToNotify = new ArrayList<MonocleWindow>();
        for (MonocleWindow otherWindow : windows) {
            if (otherWindow.getOwner() == window) {
                windowsToNotify.add(otherWindow);
            }
        }
        for (int i = 0; i < windowsToNotify.size(); i++) {
            windowsToNotify.get(i).notifyClose();
        }
        window.notifyDestroy();
        return true;

    }

    boolean minimizeWindow(MonocleWindow window) {
        return true;
    }

    boolean maximizeWindow(MonocleWindow window) {
        return true;
    }

    boolean requestFocus(MonocleWindow window) {
        int index = getWindowIndex(window);
        if (index != -1) {
            focusedWindow = window;
            window.notifyFocus(WindowEvent.FOCUS_GAINED);
            return true;
        } else {
            return false;
        }
    }

    boolean grabFocus(MonocleWindow window) {
        return true;
    }

    void ungrabFocus(MonocleWindow window) {

    }

    MonocleWindow getWindowForLocation(int x, int y) {
        for (int i = windows.length - 1; i >=0 ; i--) {
            MonocleWindow w = windows[i];
            if (x >= w.getX() && y >= w.getY()
                   && x < w.getX() + w.getWidth()
                   && y < w.getY() + w.getHeight()
                   && w.isEnabled()) {
                return w;
            }
        }
        return null;
    }

    void notifyFocusDisabled(MonocleWindow window) {
        if (window != null) {
            window._notifyFocusDisabled();
        }
    }

    MonocleWindow getFocusedWindow() {
        return focusedWindow;
    }

    void repaintAll() {
        for (int i = 0; i < windows.length; i++) {
            MonocleView view = (MonocleView) windows[i].getView();
            if (view != null) {
                view.notifyRepaint();
            }
        }
    }

    static void repaintFromNative () {
        Platform.runLater(new Runnable () {

            @Override
            public void run() {
                Screen.notifySettingsChanged();
                instance.getFocusedWindow().setFullScreen(true);
                instance.repaintAll();
                Toolkit.getToolkit().requestNextPulse();
            }
        });
    }

}
