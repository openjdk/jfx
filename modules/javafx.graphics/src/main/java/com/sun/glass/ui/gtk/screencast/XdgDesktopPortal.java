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

package com.sun.glass.ui.gtk.screencast;

import java.io.BufferedReader;
import java.io.IOException;

import static java.util.concurrent.TimeUnit.SECONDS;

public class XdgDesktopPortal {
    private static final String METHOD_GTK = "gtk";
    private static final String METHOD_SCREENCAST = "dbusScreencast";
    private static final String METHOD_REMOTE_DESKTOP = "dbusRemoteDesktop";

    private static final String method;
    private static final boolean isRemoteDesktop;
    private static final boolean isScreencast;

    private XdgDesktopPortal() {}

    static {
        String waylandDisplay = System.getenv("WAYLAND_DISPLAY");
        boolean isOnWayland = waylandDisplay != null && !waylandDisplay.isBlank();

        String defaultMethod = METHOD_GTK;
        if (isOnWayland) {
            Integer gnomeShellVersion = null;

            if ("gnome".equals(getDesktop())) {
                gnomeShellVersion = getGnomeShellMajorVersion();
            }

            defaultMethod = (gnomeShellVersion != null && gnomeShellVersion >= 47)
                    ? METHOD_REMOTE_DESKTOP
                    : METHOD_SCREENCAST;
        }

        String m = System.getProperty("javafx.robot.screenshotMethod", defaultMethod);

        if (!METHOD_REMOTE_DESKTOP.equals(m)
                && !METHOD_SCREENCAST.equals(m)
                && !METHOD_GTK.equals(m)) {
            m = defaultMethod;
        }

        isRemoteDesktop = METHOD_REMOTE_DESKTOP.equals(m);
        isScreencast = METHOD_SCREENCAST.equals(m);
        method = m;
    }

    public static String getMethod() {
        return method;
    }

    public static boolean isRemoteDesktop() {
        return isRemoteDesktop;
    }

    public static boolean isScreencast() {
        return isScreencast;
    }

    private static String getDesktop() {
        String gnome = "gnome";
        String gsi = System.getenv("GNOME_DESKTOP_SESSION_ID");
        if (gsi != null) {
            return gnome;
        }

        String desktop = System.getenv("XDG_CURRENT_DESKTOP");
        return (desktop != null && desktop.toLowerCase().contains(gnome))
                ? gnome : null;
    }

    private static Integer getGnomeShellMajorVersion() {
        try {
            Process process =
                    new ProcessBuilder("/usr/bin/gnome-shell", "--version")
                            .start();
            try (BufferedReader reader = process.inputReader()) {
                if (process.waitFor(2, SECONDS) &&  process.exitValue() == 0) {
                    String line = reader.readLine();
                    if (line != null) {
                        String[] versionComponents = line
                                .replaceAll("[^\\d.]", "")
                                .split("\\.");

                        if (versionComponents.length >= 1) {
                            return Integer.parseInt(versionComponents[0]);
                        }
                    }
                }
            }
        } catch (IOException
                 | InterruptedException
                 | IllegalThreadStateException
                 | NumberFormatException ignored) {
        }

        return null;
    }
}
