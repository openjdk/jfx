/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
// This code borrows heavily from the following project, with permission from the author:
// https://github.com/andy-goryachev/FxDock
package com.oracle.tools.fx.monkey.settings;

import java.util.HashSet;
import java.util.WeakHashMap;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Stage does not keep track of its normal bounds when minimized, maximized, or switched to full screen.
 */
class WindowMonitor {
    private final String id;
    private double x;
    private double y;
    private double width;
    private double height;
    private double x2;
    private double y2;
    private double w2;
    private double h2;
    private static final WeakHashMap<Window, WindowMonitor> monitors = new WeakHashMap<>(4);

    public WindowMonitor(Window w, String id) {
        this.id = id;

        x = w.getX();
        y = w.getY();
        width = w.getWidth();
        height = w.getHeight();

        w.xProperty().addListener((p) -> updateX(w));
        w.yProperty().addListener((p) -> updateY(w));
        w.widthProperty().addListener((p) -> updateWidth(w));
        w.heightProperty().addListener((p) -> updateHeight(w));

        if (w instanceof Stage s) {
            s.iconifiedProperty().addListener((p) -> updateIconified(s));
            s.maximizedProperty().addListener((p) -> updateMaximized(s));
            s.fullScreenProperty().addListener((p) -> updateFullScreen(s));
        }
    }

    public String getID() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    private void updateX(Window w) {
        x2 = x;
        x = w.getX();
    }

    private void updateY(Window w) {
        y2 = y;
        y = w.getY();
    }

    private void updateWidth(Window w) {
        w2 = width;
        width = w.getWidth();
    }

    private void updateHeight(Window w) {
        h2 = height;
        height = w.getHeight();
    }

    private void updateIconified(Stage s) {
        if (s.isIconified()) {
            x = x2;
            y = y2;
        }
    }

    private void updateMaximized(Stage s) {
        if (s.isMaximized()) {
            x = x2;
            y = y2;
        }
    }

    private void updateFullScreen(Stage s) {
        if (s.isFullScreen()) {
            x = x2;
            y = y2;
            width = w2;
            height = h2;
        }
    }

    public static WindowMonitor getFor(Window w) {
        if (w == null) {
            return null;
        }
        WindowMonitor m = monitors.get(w);
        if (m == null) {
            String id = createID(w);
            if (id == null) {
                return null;
            }
            m = new WindowMonitor(w, id);
            monitors.put(w, m);
        }
        return m;
    }

    public static WindowMonitor getFor(Node n) {
        Window w = windowFor(n);
        if (w != null) {
            return getFor(w);
        }
        return null;
    }

    private static Window windowFor(Node n) {
        Scene sc = n.getScene();
        if (sc != null) {
            Window w = sc.getWindow();
            if (w != null) {
                return w;
            }
        }
        return null;
    }

    private static String createID(Window win) {
        String prefix = FxSettingsSchema.getName(win);
        if (prefix != null) {
            HashSet<String> ids = new HashSet<>();
            for (Window w: Window.getWindows()) {
                if (w == win) {
                    continue;
                }
                WindowMonitor m = monitors.get(w);
                if (m == null) {
                    return null;
                }
                String id = m.getID();
                if (id.startsWith(prefix)) {
                    ids.add(id);
                }
            }

            for (int i = 0; i < 100_000; i++) {
                String id = prefix + i;
                if (!ids.contains(id)) {
                    return id;
                }
            }
        }
        return null;
    }

    public static boolean remove(Window w) {
        monitors.remove(w);
        return monitors.size() == 0;
    }
}
