/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

#include "PlatformSupport.h"
#include "glass_general.h"
#include <gtk/gtk.h>

namespace
{
    void putColor(JNIEnv* env, jobject prefs, GtkStyle* style, const char* lookupColorName, const char* prefColorName) {
        GdkColor color;
        if (gtk_style_lookup_color(style, lookupColorName, &color)) {
            env->CallObjectMethod(prefs, jMapPut,
                env->NewStringUTF(prefColorName),
                env->CallStaticObjectMethod(
                    jColorCls, jColorRgb,
                    (int)(CLAMP((double)color.red / 65535.0, 0.0, 1.0) * 255.0),
                    (int)(CLAMP((double)color.green / 65535.0, 0.0, 1.0) * 255.0),
                    (int)(CLAMP((double)color.blue / 65535.0, 0.0, 1.0) * 255.0),
                    1.0));
        }

        CHECK_JNI_EXCEPTION(env);
    }

    void putString(JNIEnv* env, jobject preferences, const char* name, const char* value) {
        env->CallObjectMethod(preferences, jMapPut,
            env->NewStringUTF(name),
            env->NewStringUTF(value));
    }
}

PlatformSupport::~PlatformSupport() {
    if (preferences) {
        env->DeleteGlobalRef(preferences);
    }
}

jobject PlatformSupport::collectPreferences() const {
    jobject prefs = env->NewObject(jHashMapCls, jHashMapInit);
    if (EXCEPTION_OCCURED(env)) return NULL;

    GtkStyle* style = gtk_style_new();

    // Platform-independent color keys
    putColor(env, prefs, style, "theme_bg_color", "javafx.backgroundColor");
    putColor(env, prefs, style, "theme_fg_color", "javafx.foregroundColor");

    // Platform-specific color keys
    putColor(env, prefs, style, "theme_fg_color", "GTK.theme_fg_color");
    putColor(env, prefs, style, "theme_bg_color", "GTK.theme_bg_color");
    putColor(env, prefs, style, "theme_base_color", "GTK.theme_base_color");
    putColor(env, prefs, style, "theme_selected_bg_color", "GTK.theme_selected_bg_color");
    putColor(env, prefs, style, "theme_selected_fg_color", "GTK.theme_selected_fg_color");
    putColor(env, prefs, style, "insensitive_bg_color", "GTK.insensitive_bg_color");
    putColor(env, prefs, style, "insensitive_fg_color", "GTK.insensitive_fg_color");
    putColor(env, prefs, style, "insensitive_base_color", "GTK.insensitive_base_color");
    putColor(env, prefs, style, "theme_unfocused_fg_color", "GTK.theme_unfocused_fg_color");
    putColor(env, prefs, style, "theme_unfocused_bg_color", "GTK.theme_unfocused_bg_color");
    putColor(env, prefs, style, "theme_unfocused_base_color", "GTK.theme_unfocused_base_color");
    putColor(env, prefs, style, "theme_unfocused_selected_bg_color", "GTK.theme_unfocused_selected_bg_color");
    putColor(env, prefs, style, "theme_unfocused_selected_fg_color", "GTK.theme_unfocused_selected_fg_color");
    putColor(env, prefs, style, "borders", "GTK.borders");
    putColor(env, prefs, style, "unfocused_borders", "GTK.unfocused_borders");
    putColor(env, prefs, style, "warning_color", "GTK.warning_color");
    putColor(env, prefs, style, "error_color", "GTK.error_color");
    putColor(env, prefs, style, "success_color", "GTK.success_color");

    g_object_unref(style);

    GtkSettings* settings = gtk_settings_get_default();
    gchar* themeName;
    g_object_get(settings, "gtk-theme-name", &themeName, NULL);
    putString(env, prefs, "GTK.theme_name", themeName);
    g_object_unref(settings);

    return prefs;
}

void PlatformSupport::updatePreferences(jobject application) const {
    if (application == NULL) {
        return;
    }

    jobject newPreferences = collectPreferences();

    jboolean preferencesChanged =
        newPreferences != NULL &&
        !env->CallBooleanMethod(newPreferences, jObjectEquals, preferences);

    if (!EXCEPTION_OCCURED(env) && preferencesChanged) {
        if (preferences) {
            env->DeleteGlobalRef(preferences);
        }

        preferences = env->NewGlobalRef(newPreferences);

        jobject unmodifiablePreferences = env->CallStaticObjectMethod(
            jCollectionsCls, jCollectionsUnmodifiableMap, newPreferences);

        if (!EXCEPTION_OCCURED(env)) {
            env->CallVoidMethod(application, jApplicationNotifyPreferencesChanged, unmodifiablePreferences);
            env->DeleteLocalRef(unmodifiablePreferences);
        }
    }

    env->DeleteLocalRef(newPreferences);
}
