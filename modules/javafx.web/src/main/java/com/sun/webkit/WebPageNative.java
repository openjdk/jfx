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

import com.sun.javafx.logging.PlatformLogger;
import com.sun.webkit.graphics.WCPoint;
import com.sun.webkit.graphics.WCRectangle;
import com.sun.webkit.graphics.WCRenderQueue;
import com.sun.webkit.network.NetworkContext;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ConcurrentHashMap;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * FFM facade for the page half of the {@code wkj_*} C ABI declared by
 * {@code Source/WebKitLegacy/java/api/webkit_java_api_page.h}, used by {@link WebPage}. It carries
 * both directions: the downcalls that used to be {@code Java_com_sun_webkit_WebPage_twk*}, and the
 * per-page callback tables that used to be the cached {@code jmethodID}s of {@code ChromeClientJava},
 * {@code FrameLoaderClientJava}, {@code EditorClientJava}, {@code InspectorClientJava},
 * {@code ProgressTrackerClientJava} and {@code WebPage.cpp} itself.
 * <p>
 * Handles stay {@code long}: a page is a {@code WebCore::WebPage*}, a frame a
 * {@code WebCore::Frame*} and both are {@code JAVA_LONG} on the wire (contract section 12). A Java
 * object is never held by native code; it is a {@code wkj_ref}, an id minted by
 * {@link WebKitNative#register}.
 * <p>
 * No downcall here uses {@code Linker.Option.critical(true)}, and none may: nearly every one of
 * them can synchronously run script, fire a load event or open modal UI, all of which re-enter the
 * JVM, and a critical downcall forbids that.
 * <p>
 * One entry point is deliberately absent because it still has a JNI form:
 * {@code twkExecuteScript}, which belongs to the LiveConnect slice. {@code twkGetIconURL} is absent
 * for a different reason: the native-necessity triage marked it for deletion rather than migration,
 * and {@link WebPage#getIcon} now answers null in Java, which is what the C body did for every
 * input.
 *
 * @see com.sun.webkit.WebKitNative
 */
final class WebPageNative {

    private static final PlatformLogger log =
            PlatformLogger.getLogger(WebPageNative.class.getName());

    // ---------------------------------------------------------------- lifecycle

    private static final MethodHandle SET_STARTUP_OPTIONS = WebKitNative.downcall(
            "wkj_set_startup_options",
            FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle PAGE_CREATE = WebKitNative.downcall(
            "wkj_page_create",
            FunctionDescriptor.of(JAVA_LONG, JAVA_INT, ADDRESS, JAVA_LONG));
    private static final MethodHandle PAGE_SET_CALLBACKS = WebKitNative.downcall(
            "wkj_page_set_callbacks",
            FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_LONG));
    private static final MethodHandle PAGE_INIT = WebKitNative.downcall(
            "wkj_page_init",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_FLOAT));
    private static final MethodHandle PAGE_DESTROY = WebKitNative.downcall(
            "wkj_page_destroy",
            FunctionDescriptor.ofVoid(JAVA_LONG));

    // ---------------------------------------------------------------- frame tree

    private static final MethodHandle PAGE_MAIN_FRAME = WebKitNative.downcall(
            "wkj_page_main_frame",
            FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));
    private static final MethodHandle FRAME_PARENT = WebKitNative.downcall(
            "wkj_frame_parent",
            FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));
    private static final MethodHandle FRAME_CHILDREN = WebKitNative.downcall(
            "wkj_frame_children",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT));

    // ---------------------------------------------------------------- frame content

    private static final MethodHandle FRAME_NAME = WebKitNative.downcall(
            "wkj_frame_name",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
    private static final MethodHandle FRAME_URL = WebKitNative.downcall(
            "wkj_frame_url",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
    private static final MethodHandle FRAME_INNER_TEXT = WebKitNative.downcall(
            "wkj_frame_inner_text",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
    private static final MethodHandle FRAME_RENDER_TREE = WebKitNative.downcall(
            "wkj_frame_render_tree",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
    private static final MethodHandle FRAME_CONTENT_TYPE = WebKitNative.downcall(
            "wkj_frame_content_type",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
    private static final MethodHandle FRAME_TITLE = WebKitNative.downcall(
            "wkj_frame_title",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
    private static final MethodHandle FRAME_HTML = WebKitNative.downcall(
            "wkj_frame_html",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));

    // ---------------------------------------------------------------- navigation

    private static final MethodHandle FRAME_OPEN = WebKitNative.downcall(
            "wkj_frame_open",
            FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT));
    private static final MethodHandle FRAME_LOAD = WebKitNative.downcall(
            "wkj_frame_load",
            FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));
    private static final MethodHandle FRAME_IS_LOADING = WebKitNative.downcall(
            "wkj_frame_is_loading",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
    private static final MethodHandle FRAME_STOP = WebKitNative.downcall(
            "wkj_frame_stop",
            FunctionDescriptor.ofVoid(JAVA_LONG));
    private static final MethodHandle PAGE_STOP_ALL = WebKitNative.downcall(
            "wkj_page_stop_all",
            FunctionDescriptor.ofVoid(JAVA_LONG));
    private static final MethodHandle FRAME_REFRESH = WebKitNative.downcall(
            "wkj_frame_refresh",
            FunctionDescriptor.ofVoid(JAVA_LONG));
    private static final MethodHandle PAGE_GO_BACK_FORWARD = WebKitNative.downcall(
            "wkj_page_go_back_forward",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT));
    private static final MethodHandle FRAME_CLEAR_NAME = WebKitNative.downcall(
            "wkj_frame_clear_name",
            FunctionDescriptor.ofVoid(JAVA_LONG));

    // ---------------------------------------------------------------- find, zoom, preferences

    private static final MethodHandle PAGE_FIND = WebKitNative.downcall(
            "wkj_page_find",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT));
    private static final MethodHandle FRAME_FIND = WebKitNative.downcall(
            "wkj_frame_find",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT));
    private static final MethodHandle FRAME_GET_ZOOM = WebKitNative.downcall(
            "wkj_frame_get_zoom",
            FunctionDescriptor.of(JAVA_FLOAT, JAVA_LONG, JAVA_INT));
    private static final MethodHandle FRAME_SET_ZOOM = WebKitNative.downcall(
            "wkj_frame_set_zoom",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_FLOAT, JAVA_INT));
    private static final MethodHandle PAGE_OVERRIDE_PREFERENCE = WebKitNative.downcall(
            "wkj_page_override_preference",
            FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));
    private static final MethodHandle PAGE_RESET_FOR_TESTING = WebKitNative.downcall(
            "wkj_page_reset_for_testing",
            FunctionDescriptor.ofVoid(JAVA_LONG));

    // ---------------------------------------------------------------- geometry and painting

    private static final MethodHandle PAGE_SET_BOUNDS = WebKitNative.downcall(
            "wkj_page_set_bounds",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle FRAME_HEIGHT = WebKitNative.downcall(
            "wkj_frame_height",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
    private static final MethodHandle FRAME_ADJUST_HEIGHT = WebKitNative.downcall(
            "wkj_frame_adjust_height",
            FunctionDescriptor.of(JAVA_FLOAT, JAVA_LONG, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT));
    private static final MethodHandle FRAME_VISIBLE_RECT = WebKitNative.downcall(
            "wkj_frame_visible_rect",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS));
    private static final MethodHandle FRAME_CONTENT_SIZE = WebKitNative.downcall(
            "wkj_frame_content_size",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS));
    private static final MethodHandle FRAME_SCROLL_TO = WebKitNative.downcall(
            "wkj_frame_scroll_to",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT));
    private static final MethodHandle FRAME_SET_TRANSPARENT = WebKitNative.downcall(
            "wkj_frame_set_transparent",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
    private static final MethodHandle FRAME_SET_BACKGROUND_COLOR = WebKitNative.downcall(
            "wkj_frame_set_background_color",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
    private static final MethodHandle PAGE_PRE_PAINT = WebKitNative.downcall(
            "wkj_page_pre_paint",
            FunctionDescriptor.ofVoid(JAVA_LONG));
    private static final MethodHandle PAGE_UPDATE_RENDERING = WebKitNative.downcall(
            "wkj_page_update_rendering",
            FunctionDescriptor.ofVoid(JAVA_LONG));
    private static final MethodHandle PAGE_UPDATE_CONTENT = WebKitNative.downcall(
            "wkj_page_update_content",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle PAGE_POST_PAINT = WebKitNative.downcall(
            "wkj_page_post_paint",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle PAGE_BEGIN_PRINTING = WebKitNative.downcall(
            "wkj_page_begin_printing",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_FLOAT, JAVA_FLOAT));
    private static final MethodHandle PAGE_END_PRINTING = WebKitNative.downcall(
            "wkj_page_end_printing",
            FunctionDescriptor.ofVoid(JAVA_LONG));
    private static final MethodHandle PAGE_PRINT = WebKitNative.downcall(
            "wkj_page_print",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_FLOAT));

    // ---------------------------------------------------------------- encoding

    private static final MethodHandle PAGE_GET_ENCODING = WebKitNative.downcall(
            "wkj_page_get_encoding",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
    private static final MethodHandle PAGE_SET_ENCODING = WebKitNative.downcall(
            "wkj_page_set_encoding",
            FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT));

    // ---------------------------------------------------------------- input events

    private static final MethodHandle PAGE_FOCUS_EVENT = WebKitNative.downcall(
            "wkj_page_focus_event",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT));
    private static final MethodHandle PAGE_KEY_EVENT = WebKitNative.downcall(
            "wkj_page_key_event",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS,
                    JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_DOUBLE));
    private static final MethodHandle PAGE_MOUSE_EVENT = WebKitNative.downcall(
            "wkj_page_mouse_event",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT, JAVA_DOUBLE));
    private static final MethodHandle PAGE_WHEEL_EVENT = WebKitNative.downcall(
            "wkj_page_wheel_event",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_FLOAT, JAVA_FLOAT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_DOUBLE));
    private static final MethodHandle PAGE_PROCESS_DRAG = WebKitNative.downcall(
            "wkj_page_process_drag",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS,
                    JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));

    // ---------------------------------------------------------------- input method editing

    private static final MethodHandle PAGE_INPUT_TEXT_CHANGE = WebKitNative.downcall(
            "wkj_page_input_text_change",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS,
                    JAVA_INT, JAVA_INT));
    private static final MethodHandle PAGE_CARET_POSITION_CHANGE = WebKitNative.downcall(
            "wkj_page_caret_position_change",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT));
    private static final MethodHandle PAGE_TEXT_LOCATION = WebKitNative.downcall(
            "wkj_page_text_location",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS));
    private static final MethodHandle PAGE_INSERT_POSITION_OFFSET = WebKitNative.downcall(
            "wkj_page_insert_position_offset",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
    private static final MethodHandle PAGE_COMMITTED_TEXT_LENGTH = WebKitNative.downcall(
            "wkj_page_committed_text_length",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
    private static final MethodHandle PAGE_COMMITTED_TEXT = WebKitNative.downcall(
            "wkj_page_committed_text",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
    private static final MethodHandle PAGE_SELECTED_TEXT = WebKitNative.downcall(
            "wkj_page_selected_text",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));

    // ---------------------------------------------------------------- editing commands

    private static final MethodHandle FRAME_COPY = WebKitNative.downcall(
            "wkj_frame_copy",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
    private static final MethodHandle PAGE_EXECUTE_COMMAND = WebKitNative.downcall(
            "wkj_page_execute_command",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));
    private static final MethodHandle PAGE_QUERY_COMMAND_ENABLED = WebKitNative.downcall(
            "wkj_page_query_command_enabled",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT));
    private static final MethodHandle PAGE_QUERY_COMMAND_STATE = WebKitNative.downcall(
            "wkj_page_query_command_state",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT));
    private static final MethodHandle PAGE_QUERY_COMMAND_VALUE = WebKitNative.downcall(
            "wkj_page_query_command_value",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT,
                    ADDRESS));
    private static final MethodHandle PAGE_IS_EDITABLE = WebKitNative.downcall(
            "wkj_page_is_editable",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
    private static final MethodHandle PAGE_SET_EDITABLE = WebKitNative.downcall(
            "wkj_page_set_editable",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));

    // ---------------------------------------------------------------- settings

    private static final MethodHandle PAGE_GET_USE_PAGE_CACHE = WebKitNative.downcall(
            "wkj_page_get_use_page_cache",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
    private static final MethodHandle PAGE_SET_USE_PAGE_CACHE = WebKitNative.downcall(
            "wkj_page_set_use_page_cache",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
    private static final MethodHandle PAGE_IS_SCRIPT_ENABLED = WebKitNative.downcall(
            "wkj_page_is_script_enabled",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
    private static final MethodHandle PAGE_SET_SCRIPT_ENABLED = WebKitNative.downcall(
            "wkj_page_set_script_enabled",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
    private static final MethodHandle PAGE_IS_CONTEXT_MENU_ENABLED = WebKitNative.downcall(
            "wkj_page_is_context_menu_enabled",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
    private static final MethodHandle PAGE_SET_CONTEXT_MENU_ENABLED = WebKitNative.downcall(
            "wkj_page_set_context_menu_enabled",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
    private static final MethodHandle PAGE_GET_DEVELOPER_EXTRAS = WebKitNative.downcall(
            "wkj_page_get_developer_extras",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
    private static final MethodHandle PAGE_SET_DEVELOPER_EXTRAS = WebKitNative.downcall(
            "wkj_page_set_developer_extras",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
    private static final MethodHandle PAGE_SET_USER_STYLESHEET = WebKitNative.downcall(
            "wkj_page_set_user_stylesheet",
            FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT));
    private static final MethodHandle PAGE_GET_USER_AGENT = WebKitNative.downcall(
            "wkj_page_get_user_agent",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
    private static final MethodHandle PAGE_SET_USER_AGENT = WebKitNative.downcall(
            "wkj_page_set_user_agent",
            FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT));
    private static final MethodHandle PAGE_SET_LOCAL_STORAGE_PATH = WebKitNative.downcall(
            "wkj_page_set_local_storage_path",
            FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT));
    private static final MethodHandle PAGE_SET_LOCAL_STORAGE_ENABLED = WebKitNative.downcall(
            "wkj_page_set_local_storage_enabled",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
    private static final MethodHandle FRAME_UNLOAD_LISTENER_COUNT = WebKitNative.downcall(
            "wkj_frame_unload_listener_count",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG));

    // ---------------------------------------------------------------- inspector and workers

    private static final MethodHandle PAGE_INSPECTOR_CONNECT = WebKitNative.downcall(
            "wkj_page_inspector_connect",
            FunctionDescriptor.ofVoid(JAVA_LONG));
    private static final MethodHandle PAGE_INSPECTOR_DISCONNECT = WebKitNative.downcall(
            "wkj_page_inspector_disconnect",
            FunctionDescriptor.ofVoid(JAVA_LONG));
    private static final MethodHandle PAGE_INSPECTOR_DISPATCH = WebKitNative.downcall(
            "wkj_page_inspector_dispatch",
            FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT));
    private static final MethodHandle WORKER_THREAD_COUNT = WebKitNative.downcall(
            "wkj_worker_thread_count",
            FunctionDescriptor.of(JAVA_INT));

    /*
     * The one plain C symbol this facade binds that is not a wkj_* function. twkDoJSCGarbageCollection
     * was a JNI wrapper whose whole body was a call to this already exported function; the wrapper is
     * gone, so Java binds what it wrapped. The declaration now reads WKJ_EXPORT rather than JNIEXPORT,
     * which is the same macro, the same symbol and the same mapfile entry, so it resolves unchanged.
     */
    private static final MethodHandle DO_JSC_GARBAGE_COLLECTION = WebKitNative.downcall(
            "WebPage_doJSCGarbageCollection",
            FunctionDescriptor.ofVoid());

    // ---------------------------------------------------------------- process-wide callbacks

    private static final MethodHandle INSTALL_NETWORK_CALLBACKS = WebKitNative.downcall(
            "wkj_install_network_callbacks",
            FunctionDescriptor.ofVoid(ADDRESS));

    /*
     * Slot counts, one per callback table in webkit_java_api_page.h. They are asserted against the
     * number of stubs actually written, so a slot added to the C struct without a Java stub - or the
     * other way round - fails at class initialization with a message rather than by dispatching
     * through a pointer one place further along the table than it should.
     *
     * A table of N function pointers has the layout of N ADDRESS elements on every ABI this module
     * supports: every member is a pointer, so there is no padding and no alignment question, which
     * is why the tables below are allocated as sequences. The counts themselves come from the named
     * layouts in {@link WKJLayouts}, which {@code WebKitLayoutTest} checks against the C
     * {@code sizeof} and {@code offsetof}, so a slot added to a C struct changes this file by
     * failing here rather than by being silently ignored.
     */
    private static final int CHROME_SLOTS = WKJLayouts.slotCount(WKJLayouts.CHROME_CALLBACKS);
    private static final int FRAME_LOADER_SLOTS =
            WKJLayouts.slotCount(WKJLayouts.FRAME_LOADER_CALLBACKS);
    private static final int EDITOR_SLOTS = WKJLayouts.slotCount(WKJLayouts.EDITOR_CALLBACKS);
    private static final int INSPECTOR_SLOTS = WKJLayouts.slotCount(WKJLayouts.INSPECTOR_CALLBACKS);
    private static final int PROGRESS_SLOTS = WKJLayouts.slotCount(WKJLayouts.PROGRESS_CALLBACKS);
    private static final int NOTIFY_SLOTS =
            WKJLayouts.slotCount(WKJLayouts.PAGE_NOTIFY_CALLBACKS);
    private static final int DRAG_SLOTS = WKJLayouts.slotCount(WKJLayouts.DRAG_CALLBACKS);
    private static final int PAGE_CALLBACKS_SLOTS = WKJLayouts.slotCount(WKJLayouts.PAGE_CALLBACKS);
    private static final int NETWORK_SLOTS = WKJLayouts.slotCount(WKJLayouts.NETWORK_CALLBACKS);

    /**
     * The one {@code WKJPageCallbacks} the whole process shares. Only the {@code wkj_ref} is per
     * page, so one table built once is what the header asks for, and it lives in
     * {@link WebKitNative}'s never closed upcall arena because the library keeps the pointer for the
     * life of every page.
     */
    private static final MemorySegment PAGE_CALLBACKS = buildPageCallbacks();

    /** Page handle to the {@code wkj_ref} of the Java {@link WebPage} that owns it. */
    private static final ConcurrentHashMap<Long, Long> PAGE_REFS = new ConcurrentHashMap<>();

    /** Page {@code wkj_ref} to the {@code wkj_ref} of its host {@code WCWidget}, minted on demand. */
    private static final ConcurrentHashMap<Long, Long> HOST_WINDOW_REFS = new ConcurrentHashMap<>();

    /**
     * The file chooser and the prompt open modal UI, so a {@code WKJ_STR_OVERFLOW} retry has to be
     * served from the answer the user already gave rather than by running the dialog again. Both are
     * confined to the calling thread and cleared as soon as the value has been handed over.
     */
    private static final ThreadLocal<String[]> PENDING_CHOSEN_FILES = new ThreadLocal<>();
    private static final ThreadLocal<String> PENDING_PROMPT = new ThreadLocal<>();

    static {
        // NetworkContext.canHandleURL belongs to no page, so it is installed once for the process
        // rather than through wkj_page_set_callbacks. Last, so that every field above is assigned
        // before the library can call back into this class.
        installNetworkCallbacks();

        // The colour chooser table is process wide for the same reason, but it lives in
        // ColorChooserNative because two of its three slots are called on a ColorChooser rather than
        // on a page. Nothing on the Java side would touch that class before the library needs
        // create_and_show, so its installation is triggered from here - the one class the page
        // static initializer is guaranteed to have run.
        ColorChooserNative.install();

        // WKJPopupCallbacks is process wide for the same reason as the colour chooser table, and
        // is triggered from here for the same reason: nothing on the Java side would load
        // PopupMenuNative before the library needs the create slot.
        PopupMenuNative.install();
    }

    private WebPageNative() {
    }

    // =====================================================================================
    // Downcalls
    // =====================================================================================

    static void initWebCore(boolean useJIT, boolean useDFGJIT, boolean useCSS3D) {
        try {
            SET_STARTUP_OPTIONS.invokeExact(bool(useJIT), bool(useDFGJIT), bool(useCSS3D));
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * Creates a page, installs the process-wide callback tables on it and tells the library which
     * registry id names the Java {@link WebPage} that owns it, in one call. This was
     * {@code twkCreatePage} followed by {@code wkj_page_set_callbacks}; page creation takes the
     * tables and the id now, because {@code PageSupplementJava} holds a {@code wkj_ref} rather than
     * a JNI global reference, and the frame loader client reads the page out of it while the page is
     * being built.
     * <p>
     * The id is registered before the call and released again if the call does not produce a page,
     * so that a failed creation cannot leave an entry behind. The library retains the id for the
     * life of the page; the reference registered here is the Java side's own, and
     * {@link #destroyPage} drops it.
     *
     * @param page the Java page
     * @param editable whether the page starts editable
     * @return the page handle
     */
    static long createPage(WebPage page, boolean editable) {
        long ref = WebKitNative.register(page);
        long pPage;
        try {
            pPage = (long) PAGE_CREATE.invokeExact(bool(editable), PAGE_CALLBACKS, ref);
        } catch (Throwable t) {
            WebKitNative.unregister(ref);
            throw new AssertionError(t);
        }
        if (pPage == 0L) {
            WebKitNative.unregister(ref);
            throw new IllegalStateException("wkj_page_create could not create a page");
        }
        PAGE_REFS.put(pPage, ref);
        return pPage;
    }

    /**
     * Re-attaches the callback tables to a live page, with the {@code wkj_ref} the page was created
     * with. Creation already attaches them, so this is only for a page that
     * {@link #detachCallbacks} has silenced; it is provisioned, as its counterpart in the C header
     * is, rather than used by the current dispose path.
     *
     * @param pPage the page handle
     */
    static void setCallbacks(long pPage) {
        Long ref = PAGE_REFS.get(pPage);
        if (ref == null) {
            throw new IllegalStateException("no registry id for page " + pPage);
        }
        try {
            PAGE_SET_CALLBACKS.invokeExact(pPage, PAGE_CALLBACKS, (long) ref);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * Stops every callback from a live page by passing a null table, which is the ABI's answer to
     * "Java is going away but the page has not been destroyed yet". The current dispose path does
     * not need it - it destroys the page, and the upcall arena is process-wide and never closed, so
     * there are no stubs to outlive - and it must not be used before {@link #destroyPage}, because
     * the library fires callbacks while it tears a page down and the JNI form delivered them.
     *
     * @param pPage the page handle
     */
    static void detachCallbacks(long pPage) {
        try {
            PAGE_SET_CALLBACKS.invokeExact(pPage, MemorySegment.NULL, 0L);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void init(long pPage, boolean usePlugins, float devicePixelScale) {
        try {
            PAGE_INIT.invokeExact(pPage, bool(usePlugins), devicePixelScale);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * Destroys the page and then drops the registry entries it owned. The order matters: the library
     * fires callbacks while it tears the page down, and detaching first would silently swallow them,
     * which the JNI form did not do.
     *
     * @param pPage the page handle
     */
    static void destroyPage(long pPage) {
        try {
            PAGE_DESTROY.invokeExact(pPage);
        } catch (Throwable t) {
            throw new AssertionError(t);
        } finally {
            Long ref = PAGE_REFS.remove(pPage);
            if (ref != null) {
                Long hostWindow = HOST_WINDOW_REFS.remove(ref);
                if (hostWindow != null) {
                    WebKitNative.unregister(hostWindow);
                }
                WebKitNative.unregister(ref);
            }
        }
    }

    static long getMainFrame(long pPage) {
        try {
            return (long) PAGE_MAIN_FRAME.invokeExact(pPage);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static long getParentFrame(long pFrame) {
        try {
            return (long) FRAME_PARENT.invokeExact(pFrame);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * The local child frames of {@code pFrame}. Sized with a counting call first, as the header
     * prescribes; unlike the JNI form it is never {@code null} and never carries a trailing zero.
     *
     * @param pFrame the frame handle
     * @return the child frame handles, possibly empty
     */
    static long[] getChildFrames(long pFrame) {
        int count;
        try {
            count = (int) FRAME_CHILDREN.invokeExact(pFrame, MemorySegment.NULL, 0);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
        if (count <= 0) {
            return new long[0];
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(JAVA_LONG, count);
            int written;
            try {
                written = (int) FRAME_CHILDREN.invokeExact(pFrame, out, count);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
            int n = Math.min(written, count);
            long[] children = new long[n];
            MemorySegment.copy(out, JAVA_LONG, 0L, children, 0, n);
            return children;
        }
    }

    static String getName(long pFrame) {
        return stringCall(FRAME_NAME, pFrame);
    }

    static String getURL(long pFrame) {
        return stringCall(FRAME_URL, pFrame);
    }

    static String getInnerText(long pFrame) {
        return stringCall(FRAME_INNER_TEXT, pFrame);
    }

    static String getRenderTree(long pFrame) {
        return stringCall(FRAME_RENDER_TREE, pFrame);
    }

    static String getContentType(long pFrame) {
        return stringCall(FRAME_CONTENT_TYPE, pFrame);
    }

    static String getTitle(long pFrame) {
        return stringCall(FRAME_TITLE, pFrame);
    }

    static String getHtml(long pFrame) {
        return stringCall(FRAME_HTML, pFrame);
    }

    static void open(long pFrame, String url) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment urlSegment = WKJStringCodec.encode(arena, url);
            try {
                FRAME_OPEN.invokeExact(pFrame, urlSegment, WKJStringCodec.length(url));
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    /**
     * Loads {@code text} as substitute data. The content is encoded as <em>modified</em> UTF-8 and
     * not as standard UTF-8, because that is what {@code GetStringUTFChars} produced and the
     * {@code ResourceResponse} the library builds still declares the charset UTF-8. The two differ
     * for {@code U+0000} and for supplementary characters; preserving the difference is what makes
     * this a migration rather than a behaviour change, and the C header says so at the same point.
     *
     * @param pFrame the frame handle
     * @param text the content
     * @param contentType the MIME type, may be {@code null}
     */
    static void load(long pFrame, String text, String contentType) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment content = WKJStringCodec.encodeModifiedUtf8(arena, text);
            int contentLength = (int) content.byteSize();
            MemorySegment type = WKJStringCodec.encode(arena, contentType);
            try {
                FRAME_LOAD.invokeExact(pFrame, content, contentLength, type,
                        WKJStringCodec.length(contentType));
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    static boolean isLoading(long pFrame) {
        return intCall(FRAME_IS_LOADING, pFrame) != 0;
    }

    static void stop(long pFrame) {
        voidCall(FRAME_STOP, pFrame);
    }

    static void stopAll(long pPage) {
        voidCall(PAGE_STOP_ALL, pPage);
    }

    static void refresh(long pFrame) {
        voidCall(FRAME_REFRESH, pFrame);
    }

    static boolean goBackForward(long pPage, int distance) {
        try {
            return (int) PAGE_GO_BACK_FORWARD.invokeExact(pPage, distance) != 0;
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void reset(long pFrame) {
        voidCall(FRAME_CLEAR_NAME, pFrame);
    }

    static boolean findInPage(long pPage, String stringToFind, boolean forward, boolean wrap,
                              boolean matchCase) {
        return find(PAGE_FIND, pPage, stringToFind, forward, wrap, matchCase);
    }

    static boolean findInFrame(long pFrame, String stringToFind, boolean forward, boolean wrap,
                               boolean matchCase) {
        return find(FRAME_FIND, pFrame, stringToFind, forward, wrap, matchCase);
    }

    static float getZoomFactor(long pFrame, boolean textOnly) {
        try {
            return (float) FRAME_GET_ZOOM.invokeExact(pFrame, bool(textOnly));
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void setZoomFactor(long pFrame, float zoomFactor, boolean textOnly) {
        try {
            FRAME_SET_ZOOM.invokeExact(pFrame, zoomFactor, bool(textOnly));
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void overridePreference(long pPage, String key, String value) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment keySegment = WKJStringCodec.encode(arena, key);
            MemorySegment valueSegment = WKJStringCodec.encode(arena, value);
            try {
                PAGE_OVERRIDE_PREFERENCE.invokeExact(pPage, keySegment, WKJStringCodec.length(key),
                        valueSegment, WKJStringCodec.length(value));
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    static void resetToConsistentStateBeforeTesting(long pPage) {
        voidCall(PAGE_RESET_FOR_TESTING, pPage);
    }

    static void setBounds(long pPage, int x, int y, int w, int h) {
        try {
            PAGE_SET_BOUNDS.invokeExact(pPage, x, y, w, h);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static int getFrameHeight(long pFrame) {
        return intCall(FRAME_HEIGHT, pFrame);
    }

    static float adjustFrameHeight(long pFrame, float oldTop, float oldBottom, float bottomLimit) {
        try {
            return (float) FRAME_ADJUST_HEIGHT.invokeExact(pFrame, oldTop, oldBottom, bottomLimit);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * The visible content rectangle as {@code x, y, width, height}, or {@code null} when the frame
     * has no view - which is the null array the JNI form returned in that case.
     *
     * @param pFrame the frame handle
     * @return the rectangle, or {@code null}
     */
    static int[] getVisibleRect(long pFrame) {
        return intArrayCall(FRAME_VISIBLE_RECT, pFrame, 4);
    }

    /**
     * The content size as {@code width, height}, or {@code null} when the frame has no view.
     *
     * @param pFrame the frame handle
     * @return the size, or {@code null}
     */
    static int[] getContentSize(long pFrame) {
        return intArrayCall(FRAME_CONTENT_SIZE, pFrame, 2);
    }

    static void scrollToPosition(long pFrame, int x, int y) {
        try {
            FRAME_SCROLL_TO.invokeExact(pFrame, x, y);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void setTransparent(long pFrame, boolean isTransparent) {
        voidCall(FRAME_SET_TRANSPARENT, pFrame, bool(isTransparent));
    }

    static void setBackgroundColor(long pFrame, int backgroundColor) {
        voidCall(FRAME_SET_BACKGROUND_COLOR, pFrame, backgroundColor);
    }

    static void prePaint(long pPage) {
        voidCall(PAGE_PRE_PAINT, pPage);
    }

    static void updateRendering(long pPage) {
        voidCall(PAGE_UPDATE_RENDERING, pPage);
    }

    static int beginPrinting(long pPage, float width, float height) {
        try {
            return (int) PAGE_BEGIN_PRINTING.invokeExact(pPage, width, height);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void endPrinting(long pPage) {
        voidCall(PAGE_END_PRINTING, pPage);
    }

    /**
     * Encodes the page's drawing commands into a render queue. The queue crosses as a
     * {@code wkj_ref} where the JNI form passed the {@code WCRenderQueue} itself; the library does
     * not keep the id beyond the call, because the {@code RenderingQueue} it builds retains its own,
     * so the reference registered here is released as soon as the call returns.
     *
     * @param pPage the page handle
     * @param rq the render queue the commands are encoded into
     * @param x the left edge of the region to update
     * @param y the top edge of the region to update
     * @param w the width of the region to update
     * @param h the height of the region to update
     */
    static void updateContent(long pPage, WCRenderQueue rq, int x, int y, int w, int h) {
        long ref = WebKitNative.register(rq);
        try {
            PAGE_UPDATE_CONTENT.invokeExact(pPage, ref, x, y, w, h);
        } catch (Throwable t) {
            throw new AssertionError(t);
        } finally {
            WebKitNative.release(ref);
        }
    }

    /**
     * Encodes the commands that finish a paint cycle, as {@link #updateContent} does.
     *
     * @param pPage the page handle
     * @param rq the render queue the commands are encoded into
     * @param x the left edge of the painted region
     * @param y the top edge of the painted region
     * @param w the width of the painted region
     * @param h the height of the painted region
     */
    static void postPaint(long pPage, WCRenderQueue rq, int x, int y, int w, int h) {
        long ref = WebKitNative.register(rq);
        try {
            PAGE_POST_PAINT.invokeExact(pPage, ref, x, y, w, h);
        } catch (Throwable t) {
            throw new AssertionError(t);
        } finally {
            WebKitNative.release(ref);
        }
    }

    /**
     * Renders one page of a print job into a render queue, as {@link #updateContent} does.
     *
     * @param pPage the page handle
     * @param rq the render queue the commands are encoded into
     * @param pageNumber the zero based page index
     * @param width the page width
     */
    static void print(long pPage, WCRenderQueue rq, int pageNumber, float width) {
        long ref = WebKitNative.register(rq);
        try {
            PAGE_PRINT.invokeExact(pPage, ref, pageNumber, width);
        } catch (Throwable t) {
            throw new AssertionError(t);
        } finally {
            WebKitNative.release(ref);
        }
    }

    static String getEncoding(long pPage) {
        return stringCall(PAGE_GET_ENCODING, pPage);
    }

    static void setEncoding(long pPage, String encoding) {
        stringArgCall(PAGE_SET_ENCODING, pPage, encoding);
    }

    static void processFocusEvent(long pPage, int id, int direction) {
        try {
            PAGE_FOCUS_EVENT.invokeExact(pPage, id, direction);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * Delivers a key event. The four modifier flags stay four parameters, as the C header keeps
     * them, so the JNI parameter list is reproduced one for one and no bitmask has to be agreed
     * between the two sides.
     *
     * @param pPage the page handle
     * @param type a {@code WCKeyEvent} type constant
     * @param text the typed text, may be {@code null}
     * @param keyIdentifier the key identifier, may be {@code null}
     * @param windowsVirtualKeyCode a {@code WCKeyEvent} key code
     * @param shift whether shift was down
     * @param control whether control was down
     * @param alt whether alt was down
     * @param meta whether meta was down
     * @param when the event timestamp, in seconds
     * @return true when WebKit consumed the event
     */
    static boolean processKeyEvent(long pPage, int type, String text, String keyIdentifier,
                                   int windowsVirtualKeyCode, boolean shift, boolean control,
                                   boolean alt, boolean meta, double when) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment typed = WebKitNative.allocString(arena, text);
            MemorySegment identifier = WebKitNative.allocString(arena, keyIdentifier);
            try {
                return (int) PAGE_KEY_EVENT.invokeExact(pPage, type, typed,
                        WebKitNative.stringLength(text), identifier,
                        WebKitNative.stringLength(keyIdentifier), windowsVirtualKeyCode,
                        bool(shift), bool(control), bool(alt), bool(meta), when) != 0;
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    static boolean processMouseEvent(long pPage, int id, int button, int buttonMask, int clickCount,
                                     int x, int y, int sx, int sy, boolean shift, boolean control,
                                     boolean alt, boolean meta, boolean popupTrigger, double when) {
        try {
            return (int) PAGE_MOUSE_EVENT.invokeExact(pPage, id, button, buttonMask, clickCount,
                    x, y, sx, sy, bool(shift), bool(control), bool(alt), bool(meta),
                    bool(popupTrigger), when) != 0;
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static boolean processMouseWheelEvent(long pPage, int x, int y, int sx, int sy, float dx,
                                          float dy, boolean shift, boolean control, boolean alt,
                                          boolean meta, double when) {
        try {
            return (int) PAGE_WHEEL_EVENT.invokeExact(pPage, x, y, sx, sy, dx, dy, bool(shift),
                    bool(control), bool(alt), bool(meta), when) != 0;
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * The drag entry point. A {@code null} {@code mimeTypes} is the drag <em>source</em> branch and
     * reaches the library as a negative element count, which is the test the JNI form made on a null
     * {@code jobjectArray}.
     *
     * @param page the page handle
     * @param commandId one of the {@code WebPage.DND_*} constants
     * @param mimeTypes the mime types, {@code null} for the drag source branch
     * @param values the values, parallel to {@code mimeTypes}
     * @param x the x position
     * @param y the y position
     * @param screenX the screen x position
     * @param screenY the screen y position
     * @param dndActionId the {@code java.awt.dnd.DnDConstants} action
     * @return the action to show, or 0
     */
    static int processDrag(long page, int commandId, String[] mimeTypes, String[] values, int x,
                           int y, int screenX, int screenY, int dndActionId) {
        try (Arena arena = Arena.ofConfined()) {
            int count = mimeTypes == null ? -1 : mimeTypes.length;
            MemorySegment mimes = MemorySegment.NULL;
            MemorySegment mimeLengths = MemorySegment.NULL;
            MemorySegment valuePointers = MemorySegment.NULL;
            MemorySegment valueLengths = MemorySegment.NULL;
            if (count > 0) {
                mimes = arena.allocate(ADDRESS, count);
                mimeLengths = arena.allocate(JAVA_INT, count);
                valuePointers = arena.allocate(ADDRESS, count);
                valueLengths = arena.allocate(JAVA_INT, count);
                for (int i = 0; i < count; i++) {
                    String mime = mimeTypes[i];
                    String value = values == null || i >= values.length ? null : values[i];
                    mimes.setAtIndex(ADDRESS, i, WKJStringCodec.encode(arena, mime));
                    mimeLengths.setAtIndex(JAVA_INT, i, WKJStringCodec.length(mime));
                    valuePointers.setAtIndex(ADDRESS, i, WKJStringCodec.encode(arena, value));
                    valueLengths.setAtIndex(JAVA_INT, i, WKJStringCodec.length(value));
                }
            }
            try {
                return (int) PAGE_PROCESS_DRAG.invokeExact(page, commandId, mimes, mimeLengths,
                        valuePointers, valueLengths, count, x, y, screenX, screenY, dndActionId);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    static boolean processInputTextChange(long pPage, String committed, String composed,
                                          int[] attributes, int caretPosition) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment committedSegment = WKJStringCodec.encode(arena, committed);
            MemorySegment composedSegment = WKJStringCodec.encode(arena, composed);
            MemorySegment attributeSegment = MemorySegment.NULL;
            int attributeCount = 0;
            if (attributes != null && attributes.length > 0) {
                attributeSegment = arena.allocateFrom(JAVA_INT, attributes);
                attributeCount = attributes.length;
            }
            try {
                return (int) PAGE_INPUT_TEXT_CHANGE.invokeExact(pPage, committedSegment,
                        WKJStringCodec.length(committed), composedSegment,
                        WKJStringCodec.length(composed), attributeSegment, attributeCount,
                        caretPosition) != 0;
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    static boolean processCaretPositionChange(long pPage, int caretPosition) {
        try {
            return (int) PAGE_CARET_POSITION_CHANGE.invokeExact(pPage, caretPosition) != 0;
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * The caret rectangle as {@code x, y, width, height}. The library zeroes the four slots before
     * it does anything else, so a frame with no view yields four zeroes rather than {@code null} -
     * which is what the JNI form's freshly allocated array gave the caller.
     *
     * @param pPage the page handle
     * @param charIndex the character index
     * @return the four element rectangle
     */
    static int[] getTextLocation(long pPage, int charIndex) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(JAVA_INT, 4);
            try {
                // The status is deliberately ignored: the library zeroes the four slots before it
                // can fail, and the JNI form handed back that zero filled array either way.
                int status = (int) PAGE_TEXT_LOCATION.invokeExact(pPage, charIndex, out);
                if (status == 0 && log.isLoggable(PlatformLogger.Level.FINER)) {
                    log.finer("getTextLocation: the frame has no view");
                }
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
            int[] location = new int[4];
            MemorySegment.copy(out, JAVA_INT, 0L, location, 0, 4);
            return location;
        }
    }

    static int getInsertPositionOffset(long pPage) {
        return intCall(PAGE_INSERT_POSITION_OFFSET, pPage);
    }

    static int getCommittedTextLength(long pPage) {
        return intCall(PAGE_COMMITTED_TEXT_LENGTH, pPage);
    }

    static String getCommittedText(long pPage) {
        return stringCall(PAGE_COMMITTED_TEXT, pPage);
    }

    static String getSelectedText(long pPage) {
        return stringCall(PAGE_SELECTED_TEXT, pPage);
    }

    static boolean copy(long pFrame) {
        return intCall(FRAME_COPY, pFrame) != 0;
    }

    static boolean executeCommand(long page, String command, String value) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment commandSegment = WKJStringCodec.encode(arena, command);
            MemorySegment valueSegment = WKJStringCodec.encode(arena, value);
            try {
                return (int) PAGE_EXECUTE_COMMAND.invokeExact(page, commandSegment,
                        WKJStringCodec.length(command), valueSegment,
                        WKJStringCodec.length(value)) != 0;
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    static boolean queryCommandEnabled(long page, String command) {
        return commandQuery(PAGE_QUERY_COMMAND_ENABLED, page, command);
    }

    static boolean queryCommandState(long page, String command) {
        return commandQuery(PAGE_QUERY_COMMAND_STATE, page, command);
    }

    static String queryCommandValue(long page, String command) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment commandSegment = WKJStringCodec.encode(arena, command);
            int commandLength = WKJStringCodec.length(command);
            MemorySegment resultLength = arena.allocate(JAVA_INT);
            MemorySegment resultBuffer = arena.allocate(JAVA_CHAR, WKJStringCodec.CAPACITY);
            int status;
            try {
                status = (int) PAGE_QUERY_COMMAND_VALUE.invokeExact(page, commandSegment,
                        commandLength, resultBuffer, WKJStringCodec.CAPACITY, resultLength);
                if (status == WKJStringCodec.OVERFLOW) {
                    int required = resultLength.get(JAVA_INT, 0);
                    resultBuffer = arena.allocate(JAVA_CHAR, required);
                    status = (int) PAGE_QUERY_COMMAND_VALUE.invokeExact(page, commandSegment,
                            commandLength, resultBuffer, required, resultLength);
                }
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
            return WKJStringCodec.decode(status, resultBuffer, resultLength);
        }
    }

    static boolean isEditable(long page) {
        return intCall(PAGE_IS_EDITABLE, page) != 0;
    }

    static void setEditable(long page, boolean editable) {
        voidCall(PAGE_SET_EDITABLE, page, bool(editable));
    }

    static boolean getUsePageCache(long page) {
        return intCall(PAGE_GET_USE_PAGE_CACHE, page) != 0;
    }

    static void setUsePageCache(long page, boolean usePageCache) {
        voidCall(PAGE_SET_USE_PAGE_CACHE, page, bool(usePageCache));
    }

    static boolean isJavaScriptEnabled(long page) {
        return intCall(PAGE_IS_SCRIPT_ENABLED, page) != 0;
    }

    static void setJavaScriptEnabled(long page, boolean enable) {
        voidCall(PAGE_SET_SCRIPT_ENABLED, page, bool(enable));
    }

    static boolean isContextMenuEnabled(long page) {
        return intCall(PAGE_IS_CONTEXT_MENU_ENABLED, page) != 0;
    }

    static void setContextMenuEnabled(long page, boolean enable) {
        voidCall(PAGE_SET_CONTEXT_MENU_ENABLED, page, bool(enable));
    }

    static boolean getDeveloperExtrasEnabled(long page) {
        return intCall(PAGE_GET_DEVELOPER_EXTRAS, page) != 0;
    }

    static void setDeveloperExtrasEnabled(long page, boolean enabled) {
        voidCall(PAGE_SET_DEVELOPER_EXTRAS, page, bool(enabled));
    }

    static void setUserStyleSheetLocation(long page, String url) {
        stringArgCall(PAGE_SET_USER_STYLESHEET, page, url);
    }

    static String getUserAgent(long page) {
        return stringCall(PAGE_GET_USER_AGENT, page);
    }

    static void setUserAgent(long page, String userAgent) {
        stringArgCall(PAGE_SET_USER_AGENT, page, userAgent);
    }

    static void setLocalStorageDatabasePath(long page, String path) {
        stringArgCall(PAGE_SET_LOCAL_STORAGE_PATH, page, path);
    }

    static void setLocalStorageEnabled(long page, boolean enabled) {
        voidCall(PAGE_SET_LOCAL_STORAGE_ENABLED, page, bool(enabled));
    }

    static int getUnloadEventListenersCount(long pFrame) {
        return intCall(FRAME_UNLOAD_LISTENER_COUNT, pFrame);
    }

    static void connectInspectorFrontend(long pPage) {
        voidCall(PAGE_INSPECTOR_CONNECT, pPage);
    }

    static void disconnectInspectorFrontend(long pPage) {
        voidCall(PAGE_INSPECTOR_DISCONNECT, pPage);
    }

    static void dispatchInspectorMessageFromFrontend(long pPage, String message) {
        stringArgCall(PAGE_INSPECTOR_DISPATCH, pPage, message);
    }

    static int workerThreadCount() {
        try {
            return (int) WORKER_THREAD_COUNT.invokeExact();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * Runs a JavaScriptCore garbage collection. This was {@code twkDoJSCGarbageCollection}, a JNI
     * wrapper around the plain C {@code WebPage_doJSCGarbageCollection}; the wrapper is gone and the
     * function it wrapped is bound directly.
     */
    static void doJSCGarbageCollection() {
        try {
            DO_JSC_GARBAGE_COLLECTION.invokeExact();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    // =====================================================================================
    // Downcall helpers
    // =====================================================================================

    private static int bool(boolean value) {
        return value ? 1 : 0;
    }

    private static void voidCall(MethodHandle handle, long peer) {
        try {
            handle.invokeExact(peer);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    private static void voidCall(MethodHandle handle, long peer, int argument) {
        try {
            handle.invokeExact(peer, argument);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    private static int intCall(MethodHandle handle, long peer) {
        try {
            return (int) handle.invokeExact(peer);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    private static void stringArgCall(MethodHandle handle, long peer, String argument) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = WKJStringCodec.encode(arena, argument);
            try {
                handle.invokeExact(peer, segment, WKJStringCodec.length(argument));
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    private static boolean commandQuery(MethodHandle handle, long page, String command) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = WKJStringCodec.encode(arena, command);
            try {
                return (int) handle.invokeExact(page, segment, WKJStringCodec.length(command)) != 0;
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    private static boolean find(MethodHandle handle, long peer, String stringToFind,
                                boolean forward, boolean wrap, boolean matchCase) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = WKJStringCodec.encode(arena, stringToFind);
            try {
                return (int) handle.invokeExact(peer, segment, WKJStringCodec.length(stringToFind),
                        bool(forward), bool(wrap), bool(matchCase)) != 0;
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    /*
     * The caller-provided-buffer string protocol of contract section 13, for every getter whose only
     * argument is a handle. It grows once on WKJ_STR_OVERFLOW, to exactly the capacity the library
     * asked for, so a second overflow is a protocol disagreement rather than a retry.
     */
    private static String stringCall(MethodHandle handle, long peer) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment resultLength = arena.allocate(JAVA_INT);
            MemorySegment resultBuffer = arena.allocate(JAVA_CHAR, WKJStringCodec.CAPACITY);
            int status;
            try {
                status = (int) handle.invokeExact(peer, resultBuffer, WKJStringCodec.CAPACITY,
                        resultLength);
                if (status == WKJStringCodec.OVERFLOW) {
                    int required = resultLength.get(JAVA_INT, 0);
                    resultBuffer = arena.allocate(JAVA_CHAR, required);
                    status = (int) handle.invokeExact(peer, resultBuffer, required, resultLength);
                }
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
            return WKJStringCodec.decode(status, resultBuffer, resultLength);
        }
    }

    /*
     * The out-parameter array shape: the library fills `count` int32_t and answers 1, or answers 0
     * and Java produces the null array the JNI form returned.
     */
    private static int[] intArrayCall(MethodHandle handle, long peer, int count) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(JAVA_INT, count);
            int filled;
            try {
                filled = (int) handle.invokeExact(peer, out);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
            if (filled == 0) {
                return null;
            }
            int[] values = new int[count];
            MemorySegment.copy(out, JAVA_INT, 0L, values, 0, count);
            return values;
        }
    }

    // =====================================================================================
    // Upcalls: the callback tables of webkit_java_api_page.h
    // =====================================================================================

    /*
     * Builds the six sub-tables and the WKJPageCallbacks aggregate that points at them, once, in the
     * process-wide upcall arena. The order of the writes below is the declaration order of each C
     * struct and must stay that way: these are sequences of function pointers, so a slot inserted in
     * the header without the matching line here would shift every later slot.
     */
    private static MemorySegment buildPageCallbacks() {
        MemorySegment chrome = table(CHROME_SLOTS, new MemorySegment[] {
            stub("chromeGetHostWindow", FunctionDescriptor.of(JAVA_LONG, JAVA_LONG)),
            stub("chromeGetWindowBounds", FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS,
                    ADDRESS, ADDRESS, ADDRESS)),
            stub("chromeSetWindowBounds", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT,
                    JAVA_INT, JAVA_INT)),
            stub("chromeGetPageBounds", FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, ADDRESS,
                    ADDRESS, ADDRESS)),
            stub("chromeScreenToWindow", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_FLOAT, JAVA_FLOAT,
                    ADDRESS, ADDRESS)),
            stub("chromeWindowToScreen", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_FLOAT, JAVA_FLOAT,
                    ADDRESS, ADDRESS)),
            stub("chromeSetFocus", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT)),
            stub("chromeTransferFocus", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT)),
            stub("chromeSetCursor", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG)),
            stub("chromeSetTooltip", FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT)),
            stub("chromeSetScrollbarsVisible", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT)),
            stub("chromeSetStatusbarText", FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT)),
            stub("chromeCreateWindow", FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT,
                    JAVA_INT, JAVA_INT, JAVA_INT)),
            stub("chromeShowWindow", FunctionDescriptor.ofVoid(JAVA_LONG)),
            stub("chromeCloseWindow", FunctionDescriptor.ofVoid(JAVA_LONG)),
            stub("chromeAlert", FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT)),
            stub("chromeConfirm", FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT)),
            stub("chromePrompt", FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT,
                    ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS)),
            stub("chromeCanRunBeforeUnload", FunctionDescriptor.of(JAVA_INT, JAVA_LONG)),
            stub("chromeRunBeforeUnload", FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS,
                    JAVA_INT)),
            stub("chromeAddMessageToConsole", FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT,
                    JAVA_INT, ADDRESS, JAVA_INT)),
            stub("chromePrint", FunctionDescriptor.ofVoid(JAVA_LONG)),
            stub("chromeChooseFile", FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT,
                    JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS)),
        });

        // The one Java method two clients call: FrameLoaderClientJava and
        // ProgressTrackerClientJava cached the same jmethodID, so the same stub goes in both tables.
        MemorySegment fireLoadEvent = stub("loaderFireLoadEvent", FunctionDescriptor.ofVoid(
                JAVA_LONG, JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, JAVA_DOUBLE,
                JAVA_INT));

        MemorySegment frameLoader = table(FRAME_LOADER_SLOTS, new MemorySegment[] {
            stub("loaderFrameCreated", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG)),
            stub("loaderFrameDestroyed", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG)),
            stub("loaderSetRequestUrl", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG, JAVA_INT,
                    ADDRESS, JAVA_INT)),
            stub("loaderRemoveRequestUrl", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG,
                    JAVA_INT)),
            fireLoadEvent,
            stub("loaderFireResourceLoadEvent", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG,
                    JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, JAVA_DOUBLE, JAVA_INT)),
            stub("loaderPermitNavigate", FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG,
                    ADDRESS, JAVA_INT)),
            stub("loaderPermitRedirect", FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG,
                    ADDRESS, JAVA_INT)),
            stub("loaderPermitAcceptResource", FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG,
                    ADDRESS, JAVA_INT)),
            stub("loaderPermitNewWindow", FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG,
                    ADDRESS, JAVA_INT)),
            stub("loaderPermitSubmitData", FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG,
                    ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT)),
            stub("loaderDidClearWindowObject", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG,
                    ADDRESS, ADDRESS)),
        });

        MemorySegment editor = table(EDITOR_SLOTS, new MemorySegment[] {
            stub("editorSetInputMethodState", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT)),
        });

        MemorySegment inspector = table(INSPECTOR_SLOTS, new MemorySegment[] {
            stub("inspectorRepaintAll", FunctionDescriptor.ofVoid(JAVA_LONG)),
            stub("inspectorSendMessageToFrontend", FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS,
                    JAVA_INT)),
        });

        MemorySegment progress = table(PROGRESS_SLOTS, new MemorySegment[] {
            fireLoadEvent,
        });

        MemorySegment notify = table(NOTIFY_SLOTS, new MemorySegment[] {
            stub("notifyRepaint", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT)),
            stub("notifyScroll", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT,
                    JAVA_INT, JAVA_INT, JAVA_INT)),
        });

        MemorySegment drag = table(DRAG_SLOTS, new MemorySegment[] {
            stub("dragStartDrag", FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG, JAVA_INT,
                    JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT,
                    JAVA_INT)),
        });

        return table(PAGE_CALLBACKS_SLOTS, new MemorySegment[] {
            chrome, frameLoader, editor, inspector, progress, notify, drag,
        });
    }

    private static void installNetworkCallbacks() {
        MemorySegment network = table(NETWORK_SLOTS, new MemorySegment[] {
            stub("networkCanHandleUrl", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT)),
        });
        try {
            INSTALL_NETWORK_CALLBACKS.invokeExact(network);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    private static MemorySegment table(int slots, MemorySegment[] pointers) {
        if (pointers.length != slots) {
            throw new AssertionError("a callback table of " + slots + " slots was given "
                    + pointers.length + " function pointers");
        }
        return WebKitNative.upcallTable(pointers);
    }

    /*
     * Derives the upcall target's Java signature from the descriptor rather than restating it, so a
     * descriptor that does not match the method it names fails here, at class initialization, with
     * the method name in the message - instead of corrupting the stack at the first callback.
     */
    private static MemorySegment stub(String name, FunctionDescriptor descriptor) {
        MethodHandle target;
        try {
            target = MethodHandles.lookup().findStatic(WebPageNative.class, name,
                    descriptor.toMethodType());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("no upcall target " + name + descriptor.toMethodType(), e);
        }
        return WebKitNative.upcallStub(target, descriptor);
    }

    // ------------------------------------------------------------------ upcall targets: chrome

    static long chromeGetHostWindow(long ref) {
        try {
            WebPage page = page(ref);
            if (page == null) {
                return 0L;
            }
            return HOST_WINDOW_REFS.computeIfAbsent(ref,
                    key -> WebKitNative.register(page.getHostWindow()));
        } catch (Throwable t) {
            failed("get_host_window", t);
            return 0L;
        }
    }

    static int chromeGetWindowBounds(long ref, MemorySegment x, MemorySegment y, MemorySegment width,
                                     MemorySegment height) {
        try {
            WebPage page = page(ref);
            return page == null ? 0 : emitRectangle(page.fwkGetWindowBounds(), x, y, width, height);
        } catch (Throwable t) {
            failed("get_window_bounds", t);
            return 0;
        }
    }

    static void chromeSetWindowBounds(long ref, int x, int y, int width, int height) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkSetWindowBounds(x, y, width, height);
            }
        } catch (Throwable t) {
            failed("set_window_bounds", t);
        }
    }

    static int chromeGetPageBounds(long ref, MemorySegment x, MemorySegment y, MemorySegment width,
                                   MemorySegment height) {
        try {
            WebPage page = page(ref);
            return page == null ? 0 : emitRectangle(page.fwkGetPageBounds(), x, y, width, height);
        } catch (Throwable t) {
            failed("get_page_bounds", t);
            return 0;
        }
    }

    static void chromeScreenToWindow(long ref, float x, float y, MemorySegment outX,
                                     MemorySegment outY) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                emitPoint(page.fwkScreenToWindow(new WCPoint(x, y)), outX, outY);
            }
        } catch (Throwable t) {
            failed("screen_to_window", t);
        }
    }

    static void chromeWindowToScreen(long ref, float x, float y, MemorySegment outX,
                                     MemorySegment outY) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                emitPoint(page.fwkWindowToScreen(new WCPoint(x, y)), outX, outY);
            }
        } catch (Throwable t) {
            failed("window_to_screen", t);
        }
    }

    static void chromeSetFocus(long ref, int focused) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkSetFocus(focused != 0);
            }
        } catch (Throwable t) {
            failed("set_focus", t);
        }
    }

    static void chromeTransferFocus(long ref, int forward) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkTransferFocus(forward != 0);
            }
        } catch (Throwable t) {
            failed("transfer_focus", t);
        }
    }

    static void chromeSetCursor(long ref, long platformCursor) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkSetCursor(platformCursor);
            }
        } catch (Throwable t) {
            failed("set_cursor", t);
        }
    }

    static void chromeSetTooltip(long ref, MemorySegment text, int textLength) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkSetTooltip(readString(text, textLength));
            }
        } catch (Throwable t) {
            failed("set_tooltip", t);
        }
    }

    static void chromeSetScrollbarsVisible(long ref, int visible) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkSetScrollbarsVisible(visible != 0);
            }
        } catch (Throwable t) {
            failed("set_scrollbars_visible", t);
        }
    }

    static void chromeSetStatusbarText(long ref, MemorySegment text, int textLength) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkSetStatusbarText(readString(text, textLength));
            }
        } catch (Throwable t) {
            failed("set_statusbar_text", t);
        }
    }

    /**
     * {@code window.open()}. Returns the <em>page handle</em> of the new page rather than a registry
     * id, because {@code ChromeClientJava::createWindow} needs the {@code WebCore::Page} and an id
     * cannot give it one. That is what removes {@code WebPage::pageFromJObject} and the
     * {@code WebPage.getPage} upcall with it.
     *
     * @param ref the registry id of the opening page
     * @param menuBar whether the new window should show a menu bar
     * @param statusBar whether it should show a status bar
     * @param toolBar whether it should show a tool bar
     * @param resizable whether it should be resizable
     * @return the new page handle, or 0 if Java declined
     */
    static long chromeCreateWindow(long ref, int menuBar, int statusBar, int toolBar,
                                   int resizable) {
        try {
            WebPage page = page(ref);
            if (page == null) {
                return 0L;
            }
            WebPage created = page.fwkCreateWindow(menuBar != 0, statusBar != 0, toolBar != 0,
                    resizable != 0);
            return created == null ? 0L : created.getPage();
        } catch (Throwable t) {
            failed("create_window", t);
            return 0L;
        }
    }

    static void chromeShowWindow(long ref) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkShowWindow();
            }
        } catch (Throwable t) {
            failed("show_window", t);
        }
    }

    static void chromeCloseWindow(long ref) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkCloseWindow();
            }
        } catch (Throwable t) {
            failed("close_window", t);
        }
    }

    static void chromeAlert(long ref, MemorySegment text, int textLength) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkAlert(readString(text, textLength));
            }
        } catch (Throwable t) {
            failed("alert", t);
        }
    }

    static int chromeConfirm(long ref, MemorySegment text, int textLength) {
        try {
            WebPage page = page(ref);
            return page != null && page.fwkConfirm(readString(text, textLength)) ? 1 : 0;
        } catch (Throwable t) {
            failed("confirm", t);
            return 0;
        }
    }

    /**
     * {@code window.prompt()}. The dialog is modal, so a {@code WKJ_STR_OVERFLOW} retry is served
     * from the answer the user already gave: running it twice would ask the user twice.
     *
     * @param ref the registry id of the page
     * @param text the prompt text
     * @param textLength its length in code units
     * @param defaultValue the default value
     * @param defaultValueLength its length in code units
     * @param resultBuffer the caller's buffer
     * @param resultCapacity its capacity in code units
     * @param resultLength where to write the code unit count
     * @return {@code WKJ_STR_OK}, {@code WKJ_STR_NULL} or {@code WKJ_STR_OVERFLOW}
     */
    static int chromePrompt(long ref, MemorySegment text, int textLength,
                            MemorySegment defaultValue, int defaultValueLength,
                            MemorySegment resultBuffer, int resultCapacity,
                            MemorySegment resultLength) {
        try {
            WebPage page = page(ref);
            if (page == null) {
                return WKJStringCodec.NULL;
            }
            String answer = PENDING_PROMPT.get();
            if (answer == null) {
                answer = page.fwkPrompt(readString(text, textLength),
                        readString(defaultValue, defaultValueLength));
            }
            int status = WKJStringCodec.emit(answer,
                    outSegment(resultBuffer, (long) resultCapacity * Character.BYTES),
                    resultCapacity, outSegment(resultLength, Integer.BYTES));
            if (status == WKJStringCodec.OVERFLOW) {
                PENDING_PROMPT.set(answer);
            } else {
                PENDING_PROMPT.remove();
            }
            return status;
        } catch (Throwable t) {
            PENDING_PROMPT.remove();
            failed("prompt", t);
            return WKJStringCodec.NULL;
        }
    }

    static int chromeCanRunBeforeUnload(long ref) {
        try {
            WebPage page = page(ref);
            return page != null && page.fwkCanRunBeforeUnloadConfirmPanel() ? 1 : 0;
        } catch (Throwable t) {
            failed("can_run_before_unload", t);
            return 0;
        }
    }

    static int chromeRunBeforeUnload(long ref, MemorySegment message, int messageLength) {
        try {
            WebPage page = page(ref);
            return page != null
                    && page.fwkRunBeforeUnloadConfirmPanel(readString(message, messageLength))
                    ? 1 : 0;
        } catch (Throwable t) {
            failed("run_before_unload", t);
            return 0;
        }
    }

    static void chromeAddMessageToConsole(long ref, MemorySegment message, int messageLength,
                                          int lineNumber, MemorySegment sourceId,
                                          int sourceIdLength) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkAddMessageToConsole(readString(message, messageLength), lineNumber,
                        readString(sourceId, sourceIdLength));
            }
        } catch (Throwable t) {
            failed("add_message_to_console", t);
        }
    }

    static void chromePrint(long ref) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkPrint();
            }
        } catch (Throwable t) {
            failed("print", t);
        }
    }

    /**
     * The file chooser. Returns the number of files chosen, or -1 when Java returned {@code null},
     * which the library distinguishes from an empty selection. As for the prompt, the dialog is
     * modal, so an overflow retry is served from the selection the user already made.
     *
     * @param ref the registry id of the page
     * @param initialFileName the initial file name
     * @param initialFileNameLength its length in code units
     * @param allowMultiple whether more than one file may be chosen
     * @param mimeFilters the mime filter string
     * @param mimeFiltersLength its length in code units
     * @param resultBuffer the caller's buffer for the paths, written end to end
     * @param resultCapacity its capacity in code units
     * @param resultLengths the caller's buffer for the per-path lengths
     * @param resultLengthsCapacity its capacity in elements
     * @param requiredUnits where to write the total code units needed on overflow
     * @return the number of files, 0 for none, -1 for null
     */
    static int chromeChooseFile(long ref, MemorySegment initialFileName, int initialFileNameLength,
                                int allowMultiple, MemorySegment mimeFilters, int mimeFiltersLength,
                                MemorySegment resultBuffer, int resultCapacity,
                                MemorySegment resultLengths, int resultLengthsCapacity,
                                MemorySegment requiredUnits) {
        try {
            WebPage page = page(ref);
            if (page == null) {
                return -1;
            }
            String[] files = PENDING_CHOSEN_FILES.get();
            if (files == null) {
                files = page.fwkChooseFile(readString(initialFileName, initialFileNameLength),
                        allowMultiple != 0, readString(mimeFilters, mimeFiltersLength));
                if (files == null) {
                    return -1;
                }
            }
            int count = files.length;
            int units = 0;
            for (String file : files) {
                units += WKJStringCodec.length(file);
            }
            // The two NULL tests are not the documented protocol, they are insurance: writing
            // through a null buffer would be a JVM crash rather than an exception, and reporting
            // the overflow instead costs the caller one more call.
            if (count > resultLengthsCapacity || units > resultCapacity
                    || (count > 0 && resultLengths.address() == 0L)
                    || (units > 0 && resultBuffer.address() == 0L)) {
                PENDING_CHOSEN_FILES.set(files);
                writeInt(requiredUnits, units);
                return count;
            }
            PENDING_CHOSEN_FILES.remove();
            MemorySegment paths = outSegment(resultBuffer, (long) units * Character.BYTES);
            MemorySegment lengths = outSegment(resultLengths, (long) count * Integer.BYTES);
            int at = 0;
            for (int i = 0; i < count; i++) {
                String file = files[i];
                int length = WKJStringCodec.length(file);
                if (length > 0) {
                    MemorySegment.copy(file.toCharArray(), 0, paths, JAVA_CHAR,
                            (long) at * Character.BYTES, length);
                }
                lengths.setAtIndex(JAVA_INT, i, length);
                at += length;
            }
            writeInt(requiredUnits, units);
            return count;
        } catch (Throwable t) {
            PENDING_CHOSEN_FILES.remove();
            failed("choose_file", t);
            return -1;
        }
    }

    // ------------------------------------------------------------ upcall targets: frame loader

    static void loaderFrameCreated(long ref, long frame) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkFrameCreated(frame);
            }
        } catch (Throwable t) {
            failed("frame_created", t);
        }
    }

    static void loaderFrameDestroyed(long ref, long frame) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkFrameDestroyed(frame);
            }
        } catch (Throwable t) {
            failed("frame_destroyed", t);
        }
    }

    static void loaderSetRequestUrl(long ref, long frame, int id, MemorySegment url,
                                    int urlLength) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkSetRequestURL(frame, id, readString(url, urlLength));
            }
        } catch (Throwable t) {
            failed("set_request_url", t);
        }
    }

    static void loaderRemoveRequestUrl(long ref, long frame, int id) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkRemoveRequestURL(frame, id);
            }
        } catch (Throwable t) {
            failed("remove_request_url", t);
        }
    }

    static void loaderFireLoadEvent(long ref, long frame, int state, MemorySegment url,
                                    int urlLength, MemorySegment contentType,
                                    int contentTypeLength, double progress, int errorCode) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkFireLoadEvent(frame, state, readString(url, urlLength),
                        readString(contentType, contentTypeLength), progress, errorCode);
            }
        } catch (Throwable t) {
            failed("fire_load_event", t);
        }
    }

    static void loaderFireResourceLoadEvent(long ref, long frame, int state, int id,
                                            MemorySegment contentType, int contentTypeLength,
                                            double progress, int errorCode) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkFireResourceLoadEvent(frame, state, id,
                        readString(contentType, contentTypeLength), progress, errorCode);
            }
        } catch (Throwable t) {
            failed("fire_resource_load_event", t);
        }
    }

    static int loaderPermitNavigate(long ref, long frame, MemorySegment url, int urlLength) {
        try {
            WebPage page = page(ref);
            return page != null && page.fwkPermitNavigateAction(frame, readString(url, urlLength))
                    ? 1 : 0;
        } catch (Throwable t) {
            failed("permit_navigate", t);
            return 0;
        }
    }

    static int loaderPermitRedirect(long ref, long frame, MemorySegment url, int urlLength) {
        try {
            WebPage page = page(ref);
            return page != null && page.fwkPermitRedirectAction(frame, readString(url, urlLength))
                    ? 1 : 0;
        } catch (Throwable t) {
            failed("permit_redirect", t);
            return 0;
        }
    }

    static int loaderPermitAcceptResource(long ref, long frame, MemorySegment url, int urlLength) {
        try {
            WebPage page = page(ref);
            return page != null
                    && page.fwkPermitAcceptResourceAction(frame, readString(url, urlLength))
                    ? 1 : 0;
        } catch (Throwable t) {
            failed("permit_accept_resource", t);
            return 0;
        }
    }

    static int loaderPermitNewWindow(long ref, long frame, MemorySegment url, int urlLength) {
        try {
            WebPage page = page(ref);
            return page != null && page.fwkPermitNewWindowAction(frame, readString(url, urlLength))
                    ? 1 : 0;
        } catch (Throwable t) {
            failed("permit_new_window", t);
            return 0;
        }
    }

    static int loaderPermitSubmitData(long ref, long frame, MemorySegment url, int urlLength,
                                      MemorySegment httpMethod, int httpMethodLength,
                                      int isSubmit) {
        try {
            WebPage page = page(ref);
            return page != null && page.fwkPermitSubmitDataAction(frame, readString(url, urlLength),
                    readString(httpMethod, httpMethodLength), isSubmit != 0) ? 1 : 0;
        } catch (Throwable t) {
            failed("permit_submit_data", t);
            return 0;
        }
    }

    static void loaderDidClearWindowObject(long ref, long frame, MemorySegment jsContext,
                                           MemorySegment jsWindowObject) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkDidClearWindowObject(jsContext.address(), jsWindowObject.address());
            }
        } catch (Throwable t) {
            failed("did_clear_window_object", t);
        }
    }

    // ------------------------------------------- upcall targets: editor, inspector, notification

    static void editorSetInputMethodState(long ref, int enabled) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.setInputMethodState(enabled != 0);
            }
        } catch (Throwable t) {
            failed("set_input_method_state", t);
        }
    }

    static void inspectorRepaintAll(long ref) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkRepaintAll();
            }
        } catch (Throwable t) {
            failed("repaint_all", t);
        }
    }

    /*
     * WebPage.fwkSendInspectorMessageToFrontend returns a boolean that InspectorClientJava has
     * always discarded, so the slot is void. Plumbing the result through would not be behaviour
     * neutral.
     */
    static void inspectorSendMessageToFrontend(long ref, MemorySegment message,
                                               int messageLength) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkSendInspectorMessageToFrontend(readString(message, messageLength));
            }
        } catch (Throwable t) {
            failed("send_message_to_frontend", t);
        }
    }

    static void notifyRepaint(long ref, int x, int y, int width, int height) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkRepaint(x, y, width, height);
            }
        } catch (Throwable t) {
            failed("repaint", t);
        }
    }

    static void notifyScroll(long ref, int x, int y, int width, int height, int deltaX,
                             int deltaY) {
        try {
            WebPage page = page(ref);
            if (page != null) {
                page.fwkScroll(x, y, width, height, deltaX, deltaY);
            }
        } catch (Throwable t) {
            failed("scroll", t);
        }
    }

    // --------------------------------------------------------------------- upcall targets: drag

    /*
     * WebPage.fwkStartDrag, the one upcall of DragClientJava and the seventh member of
     * WKJPageCallbacks.
     *
     * "image" is the com.sun.webkit.graphics.WCImage or WCImageFrame the drag is drawn from, as a
     * registry id, 0 when there is none. The JNI code passed whichever of the two the graphics layer
     * produced, with a comment that the rasters are too different to convert in native code, and
     * fwkStartDrag still takes it as Object and hands it to WCImage.getImage - so Java still
     * receives one object of one of two classes and decides which.
     *
     * The mime types and their values are two parallel arrays of `count` UTF-16 strings.
     */
    static void dragStartDrag(long ref, long image, int offsetX, int offsetY, int eventX,
                              int eventY, MemorySegment mimeTypes, MemorySegment mimeTypeLengths,
                              MemorySegment values, MemorySegment valueLengths, int count,
                              int isImageSource) {
        try {
            WebPage page = page(ref);
            if (page == null) {
                return;
            }
            String[] types = WebKitNative.readStringArray(mimeTypes, mimeTypeLengths, count);
            String[] data = WebKitNative.readStringArray(values, valueLengths, count);
            if (types == null || data == null) {
                return;
            }
            page.fwkStartDrag(WebKitNative.lookup(image), offsetX, offsetY, eventX, eventY, types,
                    data, isImageSource != 0);
        } catch (Throwable t) {
            failed("start_drag", t);
        }
    }

    // ------------------------------------------------------------------ upcall targets: network

    /*
     * NetworkContext.canHandleURL is a static Java method that belongs to no page, which is why it
     * has its own process-wide table rather than a slot in WKJPageCallbacks.
     */
    static int networkCanHandleUrl(MemorySegment url, int urlLength) {
        try {
            return NetworkContext.canHandleURL(readString(url, urlLength)) ? 1 : 0;
        } catch (Throwable t) {
            failed("can_handle_url", t);
            return 0;
        }
    }

    // =====================================================================================
    // Upcall helpers
    // =====================================================================================

    private static WebPage page(long ref) {
        return WebKitNative.lookup(ref) instanceof WebPage page ? page : null;
    }

    /*
     * An upcall target may not let a Throwable escape: an exception crossing the boundary terminates
     * the JVM. WebKit swallowed every failed upcall through CheckAndClearException, so logging and
     * returning the documented default is what preserves behaviour.
     */
    /*
     * One place, so that check_and_clear_exception cannot miss a failure. WebKitNative records the
     * per-thread flag that WKJHostCore::check_and_clear_exception reports and logs the throwable;
     * about a dozen C++ sites branch on that answer, and they have to see a page callback's failure
     * exactly as they would see a core one.
     */
    private static void failed(String slot, Throwable t) {
        WebKitNative.upcallFailed("page callback " + slot, t);
    }

    @SuppressWarnings("restricted")
    private static String readString(MemorySegment s, int length) {
        if (s.address() == 0L) {
            return null;
        }
        if (length <= 0) {
            return "";
        }
        char[] chars = new char[length];
        MemorySegment.copy(s.reinterpret((long) length * Character.BYTES), JAVA_CHAR, 0L, chars, 0,
                length);
        return new String(chars);
    }

    @SuppressWarnings("restricted")
    private static MemorySegment reinterpret(MemorySegment segment, long byteSize) {
        return segment.reinterpret(byteSize);
    }

    /*
     * A pointer arriving through an upcall is a zero length segment carrying only its address, so
     * every out parameter has to be given the size the C prototype promises before anything can be
     * written through it. A NULL pointer is passed through untouched: the callers below all test it.
     */
    private static MemorySegment outSegment(MemorySegment segment, long byteSize) {
        return segment.address() == 0L ? segment : reinterpret(segment, byteSize);
    }

    private static void writeInt(MemorySegment target, int value) {
        if (target.address() != 0L) {
            reinterpret(target, Integer.BYTES).set(JAVA_INT, 0, value);
        }
    }

    private static void writeFloat(MemorySegment target, float value) {
        if (target.address() != 0L) {
            reinterpret(target, Float.BYTES).set(JAVA_FLOAT, 0, value);
        }
    }

    /*
     * The four WCRectangle field reads ChromeClientJava used to make become these four out
     * parameters; a null rectangle answers 0 and the library falls back to an empty rect, which is
     * what the JNI code did.
     */
    private static int emitRectangle(WCRectangle bounds, MemorySegment x, MemorySegment y,
                                     MemorySegment width, MemorySegment height) {
        if (bounds == null) {
            return 0;
        }
        writeFloat(x, bounds.getX());
        writeFloat(y, bounds.getY());
        writeFloat(width, bounds.getWidth());
        writeFloat(height, bounds.getHeight());
        return 1;
    }

    private static void emitPoint(WCPoint point, MemorySegment outX, MemorySegment outY) {
        if (point == null) {
            return;
        }
        writeFloat(outX, point.getX());
        writeFloat(outY, point.getY());
    }
}
