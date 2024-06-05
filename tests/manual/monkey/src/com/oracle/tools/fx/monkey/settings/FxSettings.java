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

import java.io.File;
import java.io.IOException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.stage.Modality;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * This facility coordinates saving UI settings to and from persistent media.
 * All the calls, except useProvider(), are expected to happen in an FX application thread.
 *
 * When using {@link FxSettingsFileProvider}, the settings file "ui-settings.properties"
 * is placed in the specified directory in the user home.
 *
 * TODO handle i/o errors - set handler?
 */
public class FxSettings {
    public static final boolean LOG = Boolean.getBoolean("FxSettings.LOG");
    private static final Duration SAVE_DELAY = Duration.millis(100);
    private static ISettingsProvider provider;
    private static boolean save;
    private static Timeline saveTimer;

    /** call this in Application.init() */
    public static synchronized void useProvider(ISettingsProvider p) {
        if (provider != null) {
            throw new IllegalArgumentException("provider is already set");
        }

        provider = p;

        Window.getWindows().addListener((ListChangeListener.Change<? extends Window> ch) -> {
            while (ch.next()) {
                if (ch.wasAdded()) {
                    for (Window w: ch.getAddedSubList()) {
                        handleWindowOpening(w);
                    }
                } else if (ch.wasRemoved()) {
                    for (Window w: ch.getRemoved()) {
                        handleWindowClosing(w);
                    }
                }
            }
        });

        try {
            provider.load();
        } catch (IOException e) {
            throw new Error(e);
        }

        saveTimer = new Timeline(new KeyFrame(SAVE_DELAY, (ev) -> save()));
    }

    public static void useDirectory(String dir) {
        File d = new File(System.getProperty("user.home"), dir);
        useProvider(new FxSettingsFileProvider(d));
    }

    public static void setName(Window w, String name) {
        // TODO
    }

    private static void handleWindowOpening(Window w) {
        if (w instanceof PopupWindow) {
            return;
        }

        if (w instanceof Stage s) {
            if (s.getModality() != Modality.NONE) {
                return;
            }
        }

        restoreWindow(w);
    }

    public static void restoreWindow(Window w) {
        WindowMonitor m = WindowMonitor.getFor(w);
        if (m != null) {
            FxSettingsSchema.restoreWindow(m, w);

            Node p = w.getScene().getRoot();
            FxSettingsSchema.restoreNode(p);
        }
    }

    private static void handleWindowClosing(Window w) {
        if (w instanceof PopupWindow) {
            return;
        }

        storeWindow(w);

        boolean last = WindowMonitor.remove(w);
        if (last) {
            if (saveTimer != null) {
                saveTimer.stop();
                save();
            }
        }
    }

    public static void storeWindow(Window w) {
        WindowMonitor m = WindowMonitor.getFor(w);
        if (m != null) {
            FxSettingsSchema.storeWindow(m, w);

            Node p = w.getScene().getRoot();
            FxSettingsSchema.storeNode(p);
        }
    }

    public static void set(String key, String value) {
        if (provider != null) {
            provider.set(key, value);
            triggerSave();
        }
    }

    public static String get(String key) {
        if (provider == null) {
            return null;
        }
        return provider.get(key);
    }

    public static void setStream(String key, SStream s) {
        if (provider != null) {
            provider.set(key, s);
            triggerSave();
        }
    }

    public static SStream getStream(String key) {
        if (provider == null) {
            return null;
        }
        return provider.getSStream(key);
    }

    public static void setInt(String key, int value) {
        set(key, String.valueOf(value));
    }

    public static int getInt(String key, int defaultValue) {
        String v = get(key);
        if (v != null) {
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
            }
        }
        return defaultValue;
    }

    public static void setBoolean(String key, boolean value) {
        set(key, String.valueOf(value));
    }

    public static Boolean getBoolean(String key) {
        String v = get(key);
        if (v != null) {
            if ("true".equals(v)) {
                return Boolean.TRUE;
            } else if ("false".equals(v)) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    private static synchronized void triggerSave() {
        save = true;
        if (saveTimer != null) {
            saveTimer.stop();
            saveTimer.play();
        }
    }

    private static void save() {
        try {
            save = false;
            provider.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void restore(Node n) {
        FxSettingsSchema.restoreNode(n);
    }

    public static void store(Node n) {
        FxSettingsSchema.storeNode(n);
    }
}
