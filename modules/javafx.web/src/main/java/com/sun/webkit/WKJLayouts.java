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

package com.sun.webkit;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * The Java mirror of every {@code struct} the {@code wkj_*} C ABI declares, one
 * {@link MemoryLayout} per C type, keyed by its C name.
 * <p>
 * There is one copy of each layout and it is the copy production writes through:
 * {@link WebKitNative} builds and installs its {@code WKJHost} from {@link #HOST},
 * {@code JSObjectNative} marshals through {@link #JS_VALUE}, {@code LiveConnectNative} installs
 * {@link #LIVE_CONNECT_HOST}, and {@code WebPageNative} sizes its callback tables from the page
 * layouts below. That is deliberate: a layout that nothing writes through cannot drift in a way
 * that matters, and {@code WebKitLayoutTest} compares every layout here against the {@code sizeof}
 * and {@code offsetof} the library exports, so a struct that changes shape in C fails one test
 * instead of corrupting memory at the first callback.
 * <p>
 * <b>Shapes, not slot counts.</b> Every member is named exactly as the C header names it, so the
 * offset check is per member rather than per struct: a group that gains a slot in the middle is
 * caught where it happens, and {@code WebKitNative.hostSlotOffset("graphics.rq_flush")} resolves
 * through the same names the C compiler used.
 * <p>
 * The declaration order of every list below is the declaration order of the C struct and must stay
 * that way. These are sequences of function pointers: a name inserted out of order here moves every
 * later slot and installs each callback in its neighbour's place, which no compiler can catch.
 * <p>
 * This class contains no restricted {@code java.lang.foreign} operation and loads no library.
 *
 * @see com.sun.webkit.WebKitNative
 */
public final class WKJLayouts {

    /** Capacity of the inline message buffer of {@code WKJExceptionSlot}, in UTF-16 code units. */
    public static final int EXCEPTION_MESSAGE_MAX = 256;

    private static final Map<String, MemoryLayout> ALL = new LinkedHashMap<>();

    /**
     * {@code WKJExceptionSlot} as {@code webkit_java_api.h} declares it (contract section 13). The
     * message is inline rather than a pointer, so the slot is self contained: there is no arena
     * behind it, nothing to free, and no "valid until the next call" rule for a facade to get
     * wrong. Three {@code int32_t} followed by {@code uint16_t[256]} align to four and need no
     * trailing padding; adding one would make this layout 528 bytes against the C struct's 524,
     * which is exactly what the layout versus sizeof test exists to catch.
     */
    public static final StructLayout EXCEPTION_SLOT = struct("WKJExceptionSlot",
            JAVA_INT.withName("type"),
            JAVA_INT.withName("code"),
            JAVA_INT.withName("message_length"),
            MemoryLayout.sequenceLayout(EXCEPTION_MESSAGE_MAX, JAVA_CHAR).withName("message"));

    /**
     * {@code WKJHostCore}: the seven slots {@code webkit_java_api.h} declares, all function
     * pointers. The group is final at seven; the four perf logger slots that were once proposed for
     * it are not here, because the {@code LOG_PERF_RECORD} macro they existed for had a single call
     * site, in a file that was in no build list and has since been deleted.
     */
    public static final StructLayout HOST_CORE = pointers("WKJHostCore",
            "retain", "retain_weak", "release", "is_live", "hash_code", "equals",
            "check_and_clear_exception");

    /*
     * The seven groups that are still `struct { void (*reserved)(void); }` in the C header, one per
     * client whose upcalls have not been audited yet. The member exists because an empty struct is
     * not valid C; each group is replaced by its real slots, and this list shrinks, as its client
     * is migrated. They are laid out here rather than skipped because every one of them contributes
     * eight bytes to sizeof(WKJHost), and the library rejects a table whose size does not match its
     * own.
     */
    private static final StructLayout HOST_WEB_PAGE = placeholder("WKJHostWebPage");
    private static final StructLayout HOST_FRAME_LOADER = placeholder("WKJHostFrameLoader");
    private static final StructLayout HOST_CHROME = placeholder("WKJHostChrome");
    private static final StructLayout HOST_EDITOR = placeholder("WKJHostEditor");
    private static final StructLayout HOST_CONTEXT_MENU = placeholder("WKJHostContextMenu");
    private static final StructLayout HOST_INSPECTOR = placeholder("WKJHostInspector");
    private static final StructLayout HOST_DRAG = placeholder("WKJHostDrag");

    /**
     * {@code WKJHostGraphics}, from {@code webkit_java_api_platform.h}: the 69 upcalls of
     * {@code Source/WebCore/platform/graphics/java} against {@code WCGraphicsManager},
     * {@code Ref}, {@code WCRenderQueue}, {@code WCPath}, {@code WCFont},
     * {@code WCFontCustomPlatformData}, {@code WCTextRun}, {@code WCImage},
     * {@code WCImageDecoder} and {@code WCImageFrame}.
     */
    public static final StructLayout HOST_GRAPHICS = pointers("WKJHostGraphics",
            "create_path", "copy_path", "create_rt_image", "create_buffered_context_rq",
            "create_transform", "get_font", "get_image_decoder",
            "create_font_custom_platform_data", "create_shared_buffer", "load_from_resource",
            "create_frame",
            "ref_get_id", "ref_ref", "ref_deref",
            "rq_flush", "rq_dispose_graphics", "rq_add_buffer", "rq_ref_int_array",
            "rq_ref_float_array",
            "path_move_to", "path_add_line_to", "path_add_quad_curve_to",
            "path_add_bezier_curve_to", "path_add_arc_to", "path_add_arc", "path_add_ellipse",
            "path_add_rect", "path_add_path", "path_close_subpath", "path_is_empty",
            "path_transform", "path_contains", "path_stroke_contains", "path_get_bounds",
            "font_get_x_height", "font_get_cap_height", "font_get_ascent", "font_get_descent",
            "font_get_line_spacing", "font_get_line_gap", "font_has_uniform_line_metrics",
            "font_get_glyph_width", "font_get_glyph_bounding_box", "font_get_glyph_codes",
            "font_derive", "font_equals", "font_hash_code", "font_get_text_runs",
            "font_custom_data_create_font",
            "text_run_is_left_to_right", "text_run_get_glyph_count", "text_run_get_start",
            "text_run_get_end", "text_run_get_char_offset", "text_run_get_glyph",
            "text_run_get_glyph_pos_and_advance",
            "image_to_data", "image_get_pixel_buffer", "image_draw_pixel_buffer",
            "image_decoder_destroy", "image_decoder_add_image_data",
            "image_decoder_get_image_size", "image_decoder_get_frame_count",
            "image_decoder_get_frame", "image_decoder_get_frame_duration",
            "image_decoder_get_frame_size", "image_decoder_get_frame_complete",
            "image_decoder_get_filename_extension",
            "image_frame_get_size");

    /**
     * {@code WKJHostNetwork}, from {@code webkit_java_api_platform.h}: the 11 upcalls of
     * {@code Source/WebCore/platform/network/java} against {@code NetworkContext},
     * {@code URLLoaderBase}, {@code FormDataElement}, {@code SocketStreamHandle} and
     * {@code CookieJar}.
     */
    public static final StructLayout HOST_NETWORK = pointers("WKJHostNetwork",
            "url_loader_load", "url_loader_cancel", "form_data_create_from_bytes",
            "form_data_create_from_file", "socket_create", "socket_send", "socket_close",
            "socket_notify_disposed", "cookie_jar_get", "cookie_jar_put",
            "get_max_http_connection_count_per_host");

    /**
     * {@code WKJHostMedia}, from {@code webkit_java_api_platform.h}: the 16 upcalls of
     * {@code MediaPlayerPrivateJava}. The first two are {@code WCGraphicsManager} methods rather
     * than {@code WCMediaPlayer} ones and so take no target ref.
     */
    public static final StructLayout HOST_MEDIA = pointers("WKJHostMedia",
            "create_player", "get_supported_types", "dispose", "load", "cancel_load",
            "prepare_to_play", "play", "pause", "get_current_time", "seek", "set_rate",
            "set_preserves_pitch", "set_volume", "set_mute", "set_size", "set_preload");

    /**
     * {@code WKJHostFileSystem}, from {@code webkit_java_api_theme.h}: the ten upcalls of
     * {@code wtf/java/FileSystemJava.cpp} against {@code com.sun.webkit.FileSystem}.
     */
    public static final StructLayout HOST_FILE_SYSTEM = pointers("WKJHostFileSystem",
            "file_exists", "get_file_size", "get_file_metadata", "path_by_appending_component",
            "make_all_directories", "open_file", "close_file", "read_from_file",
            "path_get_file_name", "seek_file");

    /**
     * {@code WKJHostTheme}, from {@code webkit_java_api_theme.h}: the 43 upcalls of
     * {@code Source/WebCore/platform/java} as a whole - the widget and scrollbar themes first, but
     * also the cursor, context menu, pasteboard, screen, shared timer, {@code WCWidget},
     * {@code WCPluginWidget}, localized strings and IDN clients, none of which has a group of its
     * own.
     */
    public static final StructLayout HOST_THEME = pointers("WKJHostTheme",
            "get_render_theme", "create_widget", "get_radio_button_size", "get_selection_color",
            "get_slider_thumb_size",
            "get_scroll_bar_theme", "scroll_bar_create_widget", "scroll_bar_get_part_rect",
            "scroll_bar_get_thickness",
            "cursor_get_predefined_id", "cursor_get_custom_id",
            "context_menu_create", "context_menu_item_create", "context_menu_item_set_type",
            "context_menu_item_set_action", "context_menu_item_set_title",
            "context_menu_item_set_submenu", "context_menu_item_set_checked",
            "context_menu_item_set_enabled", "context_menu_append_item", "context_menu_show",
            "widget_set_bounds", "widget_request_focus", "widget_set_cursor", "widget_set_visible",
            "widget_destroy", "widget_get_screen_depth", "widget_get_screen_rect",
            "plugin_widget_create", "plugin_widget_set_peer", "plugin_widget_paint",
            "plugin_widget_set_native_container_bounds", "plugin_widget_handle_mouse_event",
            "timer_set_fire_time", "timer_stop",
            "get_localized_property", "idn_to_ascii",
            "pasteboard_get_plain_text", "pasteboard_get_html", "pasteboard_write_plain_text",
            "pasteboard_write_selection", "pasteboard_write_url", "pasteboard_write_image");

    /**
     * {@code WKJHostWTF}, from {@code webkit_java_api_wtf.h}: the one upcall of
     * {@code Source/WTF/wtf/java} that is not a file system call, against
     * {@code com.sun.webkit.MainThread}.
     */
    public static final StructLayout HOST_WTF = pointers("WKJHostWTF",
            "main_thread_schedule_dispatch");

    /**
     * {@code WKJHostPAL}, from {@code webkit_java_api_pal.h}: the three
     * {@code com.sun.webkit.security.WCMessageDigest} upcalls of
     * {@code pal/crypto/java/CryptoDigestJava.cpp} and the system beep of
     * {@code pal/system/java/SoundJava.cpp}.
     */
    public static final StructLayout HOST_PAL = pointers("WKJHostPAL",
            "crypto_digest_create", "crypto_digest_add_bytes", "crypto_digest_compute_hash",
            "system_beep");

    /**
     * {@code WKJHost}: an {@code int32_t}, four bytes of padding the C compiler inserts before the
     * first pointer aligned group, and the sixteen groups above. The padding is declared rather
     * than left implicit because {@link MemoryLayout#structLayout} inserts none, and without it
     * this layout is 1348 bytes against the C struct's 1352 - which {@code wkj_init} rejects with
     * {@code WKJ_INIT_ERR_HOST_SIZE}.
     */
    public static final StructLayout HOST = struct("WKJHost",
            JAVA_INT.withName("size"),
            MemoryLayout.paddingLayout(4),
            HOST_CORE.withName("core"),
            HOST_WEB_PAGE.withName("webpage"),
            HOST_FRAME_LOADER.withName("frameloader"),
            HOST_CHROME.withName("chrome"),
            HOST_EDITOR.withName("editor"),
            HOST_CONTEXT_MENU.withName("contextmenu"),
            HOST_INSPECTOR.withName("inspector"),
            HOST_DRAG.withName("drag"),
            HOST_GRAPHICS.withName("graphics"),
            HOST_NETWORK.withName("network"),
            HOST_MEDIA.withName("media"),
            HOST_FILE_SYSTEM.withName("filesystem"),
            HOST_THEME.withName("theme"),
            HOST_WTF.withName("wtf"),
            HOST_PAL.withName("pal"));

    /**
     * {@code WKJJavaValue}, from {@code webkit_java_api_bridge.h}: the tagged union LiveConnect
     * reads a Java field, writes a Java field and boxes a primitive through. Two {@code int32_t},
     * an {@code int64_t}, a {@code double} and a {@code wkj_ref} pack to 32 bytes with no padding
     * anywhere - {@code type} and {@code i} are adjacent and need none.
     */
    public static final StructLayout JAVA_VALUE = struct("WKJJavaValue",
            JAVA_INT.withName("type"),
            JAVA_INT.withName("i"),
            JAVA_LONG.withName("j"),
            JAVA_DOUBLE.withName("d"),
            JAVA_LONG.withName("l"));

    /**
     * {@code WKJJSValue}, from {@code webkit_java_api_bridge.h}: the described form of one
     * JavaScript value. Two {@code int32_t}, then a {@code double} which forces eight byte
     * alignment, three eight byte fields, the string pointer, and two trailing {@code int32_t}
     * that fill the last eight bytes exactly - so the struct is 56 bytes with no tail padding.
     */
    public static final StructLayout JS_VALUE = struct("WKJJSValue",
            JAVA_INT.withName("kind"),
            JAVA_INT.withName("peer_type"),
            JAVA_DOUBLE.withName("number"),
            JAVA_LONG.withName("peer"),
            JAVA_LONG.withName("string_handle"),
            JAVA_LONG.withName("object"),
            ADDRESS.withName("string"),
            JAVA_INT.withName("string_cap"),
            JAVA_INT.withName("string_length"));

    /**
     * {@code WKJLiveConnectHost}, from {@code webkit_java_api_bridge.h}: its own size, four bytes
     * of padding, and the 26 reflective upcalls that let the library expose an arbitrary Java
     * object to page script. Installed by {@code wkj_live_connect_init} rather than by
     * {@code wkj_init}, because the table belongs to one subsystem and a use of the library that
     * never exposes a Java object to script never needs it.
     */
    public static final StructLayout LIVE_CONNECT_HOST = struct("WKJLiveConnectHost",
            concat(new MemoryLayout[] { JAVA_INT.withName("size"), MemoryLayout.paddingLayout(4) },
                    slots("object_get_class", "class_get_name", "class_is_array",
                            "create_dummy_object", "resolve_method", "invoke", "method_get_name",
                            "method_get_return_type_name", "method_get_parameter_count",
                            "method_get_parameter_type_name", "method_get_modifiers",
                            "field_get_name", "field_get_type_name", "field_get", "field_set",
                            "array_length", "array_get", "array_set", "box", "unbox", "box_string",
                            "string_value", "describe_object", "undefined_object",
                            "jsobject_create", "node_get_cached_impl")));

    /** {@code WKJEventListenerCallbacks}, from {@code webkit_java_api_events.h}. */
    public static final StructLayout EVENT_LISTENER_CALLBACKS =
            pointers("WKJEventListenerCallbacks", "handle_event", "dispose");

    /* The per page callback tables of webkit_java_api_page.h, installed by wkj_page_create. */

    /** {@code WKJChromeCallbacks}: the 23 upcalls of {@code ChromeClientJava}. */
    public static final StructLayout CHROME_CALLBACKS = pointers("WKJChromeCallbacks",
            "get_host_window", "get_window_bounds", "set_window_bounds", "get_page_bounds",
            "screen_to_window", "window_to_screen", "set_focus", "transfer_focus", "set_cursor",
            "set_tooltip", "set_scrollbars_visible", "set_statusbar_text", "create_window",
            "show_window", "close_window", "alert", "confirm", "prompt", "can_run_before_unload",
            "run_before_unload", "add_message_to_console", "print", "choose_file");

    /** {@code WKJFrameLoaderCallbacks}: the twelve upcalls of {@code FrameLoaderClientJava}. */
    public static final StructLayout FRAME_LOADER_CALLBACKS = pointers("WKJFrameLoaderCallbacks",
            "frame_created", "frame_destroyed", "set_request_url", "remove_request_url",
            "fire_load_event", "fire_resource_load_event", "permit_navigate", "permit_redirect",
            "permit_accept_resource", "permit_new_window", "permit_submit_data",
            "did_clear_window_object");

    /** {@code WKJNetworkCallbacks}: installed by {@code wkj_install_network_callbacks}. */
    public static final StructLayout NETWORK_CALLBACKS =
            pointers("WKJNetworkCallbacks", "can_handle_url");

    /** {@code WKJEditorCallbacks}: the one upcall of {@code EditorClientJava}. */
    public static final StructLayout EDITOR_CALLBACKS =
            pointers("WKJEditorCallbacks", "set_input_method_state");

    /** {@code WKJInspectorCallbacks}: the two upcalls of {@code InspectorClientJava}. */
    public static final StructLayout INSPECTOR_CALLBACKS =
            pointers("WKJInspectorCallbacks", "repaint_all", "send_message_to_frontend");

    /** {@code WKJProgressCallbacks}: the one upcall of {@code ProgressTrackerClientJava}. */
    public static final StructLayout PROGRESS_CALLBACKS =
            pointers("WKJProgressCallbacks", "fire_load_event");

    /** {@code WKJPageNotifyCallbacks}: the repaint and scroll notifications of the page. */
    public static final StructLayout PAGE_NOTIFY_CALLBACKS =
            pointers("WKJPageNotifyCallbacks", "repaint", "scroll");

    /** {@code WKJBackForwardCallbacks}: installed by {@code wkj_back_forward_set_callbacks}. */
    public static final StructLayout BACK_FORWARD_CALLBACKS = pointers("WKJBackForwardCallbacks",
            "list_changed", "create_entry", "item_destroyed");

    /** {@code WKJColorChooserCallbacks}: the three upcalls of {@code ColorChooserJava}. */
    public static final StructLayout COLOR_CHOOSER_CALLBACKS = pointers("WKJColorChooserCallbacks",
            "create_and_show", "show", "hide");

    /** {@code WKJDragCallbacks}: the one upcall of {@code DragClientJava}. */
    public static final StructLayout DRAG_CALLBACKS = pointers("WKJDragCallbacks", "start_drag");

    /** {@code WKJPopupCallbacks}: the six upcalls of {@code PopupMenuJava}. */
    public static final StructLayout POPUP_CALLBACKS = pointers("WKJPopupCallbacks",
            "create", "append_item", "set_selected_item", "show", "hide", "destroy");

    /**
     * {@code WKJPageCallbacks}: the seven sub-tables of one page, as pointers. All seven are
     * present, {@code drag} included - a table of six would leave the library reading its seventh
     * pointer past the end of the allocation.
     */
    public static final StructLayout PAGE_CALLBACKS = pointers("WKJPageCallbacks",
            "chrome", "frame_loader", "editor", "inspector", "progress", "notify", "drag");

    private WKJLayouts() {
    }

    /**
     * Returns every struct layout this ABI declares, keyed by its C name. The layout test drives
     * itself from the C side and asserts that this map covers it exactly, so a struct added to a
     * header with no Java layout fails rather than going unchecked.
     *
     * @return the declared layouts
     */
    public static Map<String, MemoryLayout> all() {
        return Map.copyOf(ALL);
    }

    /**
     * Returns the number of function pointer slots a callback table has, for the facade that fills
     * it. Taking the count from the layout rather than from a literal is what makes a slot added to
     * a C header fail once, loudly, instead of silently shifting a table.
     *
     * @param layout one of the layouts above
     * @return the member count
     */
    public static int slotCount(StructLayout layout) {
        return layout.memberLayouts().size();
    }

    private static StructLayout struct(String name, MemoryLayout... members) {
        StructLayout layout = MemoryLayout.structLayout(members).withName(name);
        ALL.put(name, layout);
        return layout;
    }

    private static StructLayout pointers(String name, String... slots) {
        return struct(name, slots(slots));
    }

    /*
     * An empty struct is not valid C, so a group whose upcalls have not been audited yet carries
     * one `void (*reserved)(void)`. It contributes its eight bytes to sizeof(WKJHost) and is left
     * NULL, which the library is required to tolerate.
     */
    private static StructLayout placeholder(String name) {
        return pointers(name, "reserved");
    }

    private static MemoryLayout[] slots(String... names) {
        List<MemoryLayout> members = new ArrayList<>(names.length);
        for (String slot : names) {
            members.add(ADDRESS.withName(slot));
        }
        return members.toArray(MemoryLayout[]::new);
    }

    private static MemoryLayout[] concat(MemoryLayout[] head, MemoryLayout[] tail) {
        MemoryLayout[] all = new MemoryLayout[head.length + tail.length];
        System.arraycopy(head, 0, all, 0, head.length);
        System.arraycopy(tail, 0, all, head.length, tail.length);
        return all;
    }
}
