/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include "glass_screen.h"
#include "glass_general.h"

#include <X11/Xatom.h>
#include <gdk/gdk.h>
#include <gdk/gdkx.h>

float OverrideUIScale = -1.0f;
int DEFAULT_DPI = 96;

float getUIScale(GdkScreen* screen) {
    float uiScale;
    if (OverrideUIScale > 0.0f) {
        uiScale = OverrideUIScale;
    } else {
        gdouble resolution = gdk_screen_get_resolution(screen);
        uiScale = (float) (resolution / DEFAULT_DPI);
        if (int(resolution) == DEFAULT_DPI) {
            // These legacy settings should only be consulted if the DPI is
            // 96. They are global and affect all monitors and only allow
            // integers. And they are not reliable when scaling is
            // fractional. KDE in Ubuntu 22 sets GDK_SCALE to the floor of
            // the actual scaling factor. KDE in Ubuntu 24 sets the
            // Gnome "scaling-factor" instead. Both settings are obsolete in
            // their respective toolkits and are not trustworthy.
            char *scale_str = getenv("GDK_SCALE");
            int gdk_scale = (scale_str == NULL) ? -1 : atoi(scale_str);
            if (gdk_scale > 0) {
                uiScale = (float) gdk_scale;
            } else {
                guint gnome_scale = glass_settings_get_guint_opt("org.gnome.desktop.interface",
                                                                 "scaling-factor", 0);
                if (gnome_scale > 0) {
                    uiScale = (float) gnome_scale;
                }
            }
        }
    }
    return uiScale;
}

// createJavaScreen, the C that built a com.sun.glass.ui.Screen at commit 033187ad90 for process_configure's
// Window.notifyMoveToAnotherScreen (with the work-area and current-desktop reads it alone used), served only
// that JNI upcall: the notify_move_to_another_screen slot passes the monitor index and Java builds the Screen
// with its own copy of this arithmetic (GtkGlassNative).
//
// getUIScale above has no caller left in this library, and OverrideUIScale - which ggtk_application_init_gtk
// still writes - no reader but getUIScale; both are kept deliberately, as the oracle the Java copy is checked
// against. GtkGlassShim binds getUIScale by its mangled name and writes OverrideUIScale directly, and
// GtkRobotParityTest compares the two implementations on the same GdkScreen. They are production code kept
// alive by a test, and they go only together with that test.

glong getScreenPtrForLocation(gint x, gint y) {
    //Note: we are relying on the fact that javafx_screen_id == gdk_monitor_id
    return gdk_screen_get_monitor_at_point(gdk_screen_get_default(), x, y);
}

void screen_settings_changed(GdkScreen* screen, gpointer user_data) {
    (void)screen;
    (void)user_data;

    if (glass_app_cb.notify_screen_settings_changed) {
        glass_app_cb.notify_screen_settings_changed(); // LOG_EXCEPTION site: the status is ignored
    }
}
