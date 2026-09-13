/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

// This code borrows heavily from the following project, with permission from the author:
// https://github.com/andy-goryachev/FxDock
package com.oracle.demo.richtext.settings;

import java.util.HashSet;
import java.util.WeakHashMap;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Stage does not keep track of its normal bounds when minimized, maximized, or switched to full screen.
 *
 * @author Andy Goryachev
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
        String prefix = FxSettingsSchema.getName(win) + ".";
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

        // safeguard measure
        throw new Error("cannot create id: too many windows?");
    }

    public static boolean remove(Window w) {
        monitors.remove(w);
        return monitors.size() == 0;
    }
}
