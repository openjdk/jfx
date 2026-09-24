/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

/*
 * The exports of glass_gtk_api.h: the ABI version, the sizeof probes, the four callback-table installers, the
 * window / view id getters, and the test hook. The call sites that dial the tables live in the files whose JNI
 * calls they replaced (glass_window.cpp, glass_window_ime.cpp, GlassView.cpp, glass_dnd.cpp, glass_screen.cpp,
 * glass_general.cpp); the functions of the former natives live where their Java_* functions were at commit
 * 033187ad90 (GlassApplication.cpp, glass_key.cpp, GlassWindow.cpp, GlassView.cpp, GlassDnDClipboard.cpp,
 * glass_dnd.cpp).
 */

#include "glass_gtk_api.h"
#include "glass_general.h"
#include "glass_window.h"
#include "glass_view.h"

#include <cstring>

// The installed tables. Static storage, so every slot is NULL - every call site makes no call - until Java
// installs a table.
GgtkAppCallbacks glass_app_cb;
GgtkWindowCallbacks glass_window_cb;
GgtkViewCallbacks glass_view_cb;
GgtkDndCallbacks glass_dnd_cb;

extern "C" {

int32_t ggtk_abi_version(void) {
    return GLASS_GTK_ABI_VERSION;
}

int32_t ggtk_sizeof_app_callbacks(void) {
    return (int32_t) sizeof(GgtkAppCallbacks);
}

int32_t ggtk_sizeof_window_callbacks(void) {
    return (int32_t) sizeof(GgtkWindowCallbacks);
}

int32_t ggtk_sizeof_view_callbacks(void) {
    return (int32_t) sizeof(GgtkViewCallbacks);
}

int32_t ggtk_sizeof_dnd_callbacks(void) {
    return (int32_t) sizeof(GgtkDndCallbacks);
}

int32_t ggtk_sizeof_dnd_data(void) {
    return (int32_t) sizeof(GgtkDndData);
}

int32_t ggtk_sizeof_dnd_target_value(void) {
    return (int32_t) sizeof(GgtkDndTargetValue);
}

int32_t ggtk_app_set_callbacks(const GgtkAppCallbacks* cb) {
    if (cb != NULL) {
        glass_app_cb = *cb;
    } else {
        memset(&glass_app_cb, 0, sizeof(glass_app_cb));
    }
    return GGTK_OK;
}

int32_t ggtk_window_set_callbacks(const GgtkWindowCallbacks* cb) {
    if (cb != NULL) {
        glass_window_cb = *cb;
    } else {
        memset(&glass_window_cb, 0, sizeof(glass_window_cb));
    }
    return GGTK_OK;
}

int32_t ggtk_view_set_callbacks(const GgtkViewCallbacks* cb) {
    if (cb != NULL) {
        glass_view_cb = *cb;
    } else {
        memset(&glass_view_cb, 0, sizeof(glass_view_cb));
    }
    return GGTK_OK;
}

int32_t ggtk_dnd_set_callbacks(const GgtkDndCallbacks* cb) {
    if (cb != NULL) {
        glass_dnd_cb = *cb;
    } else {
        memset(&glass_dnd_cb, 0, sizeof(glass_dnd_cb));
    }
    return GGTK_OK;
}

int64_t ggtk_window_get_id(ggtk_window_t window) {
    return (window != NULL) ? ((WindowContext*) window)->get_window_id() : 0;
}

int64_t ggtk_window_get_view_id(ggtk_window_t window) {
    return (window != NULL) ? ((WindowContext*) window)->get_view_id() : 0;
}

int64_t ggtk_view_get_id(ggtk_view_t view) {
    return (view != NULL) ? ((GlassView*) view)->id : 0;
}

#define GGTK_FIRE(table, slot, ...) \
    ((table).slot != NULL ? (table).slot(__VA_ARGS__) : GGTK_ERR_NOT_INSTALLED)

int32_t ggtk_test_fire_callback(int32_t slot, int64_t id, void* out, void* out2) {
    static const uint16_t key_chars[] = { 0x0041, 0xD800 };
    static const char text[] = {
        0x41, (char) 0xC3, (char) 0xA9, (char) 0xF0, (char) 0x9F, (char) 0x98, (char) 0x80, 0x5A, 0
    };
    static const char mime[] = "text/plain";

    switch (slot) {
        case 0:   return (glass_app_cb.notify_screen_settings_changed != NULL)
                          ? glass_app_cb.notify_screen_settings_changed() : GGTK_ERR_NOT_INSTALLED;
        case 1:   return GGTK_FIRE(glass_app_cb, get_application_name, (char**) out);

        case 100: return GGTK_FIRE(glass_window_cb, is_enabled, id, (int32_t*) out);
        case 101: return GGTK_FIRE(glass_window_cb, notify_state_changed, id, 1001);
        case 102: return GGTK_FIRE(glass_window_cb, notify_focus, id, 1001);
        case 103: return GGTK_FIRE(glass_window_cb, notify_focus_disabled, id);
        case 104: return GGTK_FIRE(glass_window_cb, notify_focus_ungrab, id);
        case 105: return GGTK_FIRE(glass_window_cb, notify_destroy, id);
        case 106: return GGTK_FIRE(glass_window_cb, notify_close, id);
        case 107: return GGTK_FIRE(glass_window_cb, notify_resize, id, 1001, 1002, 1003);
        case 108: return GGTK_FIRE(glass_window_cb, notify_move, id, 1001, 1002);
        case 109: return GGTK_FIRE(glass_window_cb, notify_move_to_another_screen, id, 1001);
        case 110: return GGTK_FIRE(glass_window_cb, notify_level_changed, id, 1001);
        case 111: return GGTK_FIRE(glass_window_cb, non_client_hit_test, id, 1001, 1002, (int32_t*) out);

        case 200: return GGTK_FIRE(glass_view_cb, notify_view, id, 1001);
        case 201: return GGTK_FIRE(glass_view_cb, notify_resize, id, 1001, 1002);
        case 202: return GGTK_FIRE(glass_view_cb, notify_repaint, id, 1001, 1002, 1003, 1004);
        case 203: return GGTK_FIRE(glass_view_cb, notify_mouse, id, 1001, 1002, 1003, 1004, 1005, 1006, 1007,
                                   1, 0);
        case 204: return GGTK_FIRE(glass_view_cb, notify_menu, id, 1001, 1002, 1003, 1004, 1);
        case 205: return GGTK_FIRE(glass_view_cb, notify_scroll, id, 1001, 1002, 1003, 1004, 1.5, 2.5, 1005,
                                   1006, 1007, 1008, 1009, 3.5, 4.5);
        case 206: return GGTK_FIRE(glass_view_cb, notify_key, id, 1001, 1002, key_chars, 2, 1003);
        case 207: return GGTK_FIRE(glass_view_cb, notify_input_method_preedit, id, text, 8, 1001, 1002);
        case 208: return GGTK_FIRE(glass_view_cb, notify_input_method_commit, id, text, 8);
        case 209: return GGTK_FIRE(glass_view_cb, notify_input_method_candidate_pos_request, id, 1001,
                                   (double*) out, (int32_t*) out2);

        case 300: return GGTK_FIRE(glass_dnd_cb, notify_drag_enter, id, 1001, 1002, 1003, 1004, 1005,
                                   (int32_t*) out);
        case 301: return GGTK_FIRE(glass_dnd_cb, notify_drag_over, id, 1001, 1002, 1003, 1004, 1005,
                                   (int32_t*) out);
        case 302: return GGTK_FIRE(glass_dnd_cb, notify_drag_drop, id, 1001, 1002, 1003, 1004, 1005);
        case 303: return GGTK_FIRE(glass_dnd_cb, notify_drag_leave, id);
        case 304: return GGTK_FIRE(glass_dnd_cb, source_get_data, mime, 10, (int32_t) id, (GgtkDndData*) out);

        default:  return GGTK_ERR_INVALID_ARG;
    }
}

#undef GGTK_FIRE

} // extern "C"
