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

package test.com.sun.glass.ui.gtk;

import com.sun.glass.ui.gtk.GtkGlassShim;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bindings of {@code com.sun.glass.ui.gtk.GtkGlassNative} in a process whose toolkit is up: the exact set of
 * system symbols it binds, in binding order, then the {@code libglassgtk3.so} exports of the callback tables, and
 * that each lies in the one mapping of its library the process has - the instance {@code libglassgtk3.so} was
 * linked against and {@code GtkApplication}'s library query opened, not a second copy. Runs in
 * {@link GtkGlassChild} on the X11 display of {@code DISPLAY}.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(300)
public class GtkGlassNativeBindingTest {

    /** Every symbol the facade binds, in binding order. */
    static final List<String> SYMBOLS = List.of(
            "libgdk-3.so.0!gdk_threads_add_timeout_full",
            "libgdk-3.so.0!gdk_threads_add_idle_full",
            "libgtk-3.so.0!gtk_main",
            "libgtk-3.so.0!gtk_main_quit",
            "libglib-2.0.so.0!g_malloc",
            "libglib-2.0.so.0!g_free",
            "libgdk_pixbuf-2.0.so.0!gdk_pixbuf_new_from_data",
            "libgdk-3.so.0!gdk_display_get_default",
            "libgdk-3.so.0!gdk_cursor_new_from_pixbuf",
            "libgobject-2.0.so.0!g_object_unref",
            "libgdk-3.so.0!gdk_display_get_default_cursor_size",
            "libgdk-3.so.0!gdk_x11_get_default_xdisplay",
            "libgdk-3.so.0!gdk_x11_get_default_screen",
            "libgdk-3.so.0!gdk_x11_display_get_xdisplay",
            "libgdk-3.so.0!gdk_screen_get_default",
            "libgdk-3.so.0!gdk_screen_get_resolution",
            "libgdk-3.so.0!gdk_display_get_device_manager",
            "libgdk-3.so.0!gdk_device_manager_get_client_pointer",
            "libgdk-3.so.0!gdk_device_get_position",
            "libgdk-3.so.0!gdk_get_default_root_window",
            "libgdk-3.so.0!gdk_pixbuf_get_from_window",
            "libgdk-3.so.0!gdk_keymap_get_for_display",
            "libgdk-3.so.0!gdk_keyval_to_lower",
            "libgdk-3.so.0!gdk_keymap_get_entries_for_keyval",
            "libgdk-3.so.0!gdk_keymap_translate_keyboard_state",
            "libgdk_pixbuf-2.0.so.0!gdk_pixbuf_add_alpha",
            "libgdk_pixbuf-2.0.so.0!gdk_pixbuf_get_pixels",
            "libglib-2.0.so.0!g_hash_table_new",
            "libglib-2.0.so.0!g_direct_hash",
            "libglib-2.0.so.0!g_direct_equal",
            "libglib-2.0.so.0!g_hash_table_insert",
            "libglib-2.0.so.0!g_hash_table_iter_init",
            "libglib-2.0.so.0!g_hash_table_iter_next",
            "libgio-2.0.so.0!g_settings_new",
            "libgio-2.0.so.0!g_settings_get_uint",
            "libgio-2.0.so.0!g_settings_schema_source_get_default",
            "libgio-2.0.so.0!g_settings_schema_source_lookup",
            "libgio-2.0.so.0!g_settings_schema_has_key",
            "libgio-2.0.so.0!g_settings_schema_unref",
            "libX11.so.6!XQueryExtension",
            "libX11.so.6!XWarpPointer",
            "libX11.so.6!XRootWindow",
            "libX11.so.6!XSync",
            "libX11.so.6!XInternAtom",
            "libX11.so.6!XkbQueryExtension",
            "libX11.so.6!XkbGetState",
            "libX11.so.6!XkbGetNamedIndicator",
            "libXtst.so.6!XTestQueryExtension",
            "libXtst.so.6!XTestGrabControl",
            "libXtst.so.6!XTestFakeKeyEvent",
            "libXtst.so.6!XTestFakeButtonEvent",
            "libc.so.6!getenv",
            "libc.so.6!atoi",
            "libc.so.6!fputs",
            "libc.so.6!fflush",
            "libc.so.6!stderr",
            "libgtk-3.so.0!gtk_show_uri",
            "libglib-2.0.so.0!g_error_free",
            "libgtk-3.so.0!gtk_settings_get_default",
            "libgobject-2.0.so.0!g_object_get",
            "libgdk-3.so.0!gdk_display_supports_composite",
            "libgdk-3.so.0!gdk_screen_is_composited",
            "libc.so.6!strlen",
            "libgdk-3.so.0!gdk_screen_get_n_monitors",
            "libgdk-3.so.0!gdk_screen_get_monitor_geometry",
            "libgdk-3.so.0!gdk_screen_get_system_visual",
            "libgdk-3.so.0!gdk_visual_get_depth",
            "libgdk-3.so.0!gdk_rectangle_intersect",
            "libgdk-3.so.0!gdk_screen_get_width",
            "libgdk-3.so.0!gdk_screen_get_height",
            "libgdk-3.so.0!gdk_screen_get_width_mm",
            "libgdk-3.so.0!gdk_screen_get_height_mm",
            "libgdk-3.so.0!gdk_screen_get_monitor_width_mm",
            "libgdk-3.so.0!gdk_screen_get_monitor_height_mm",
            "libgdk-3.so.0!gdk_screen_get_root_window",
            "libgdk-3.so.0!gdk_x11_window_get_xid",
            "libX11.so.6!XGetWindowProperty",
            "libX11.so.6!XFree",
            "libgtk-3.so.0!gtk_style_new",
            "libgtk-3.so.0!gtk_style_lookup_color",
            "libgobject-2.0.so.0!g_object_class_find_property",
            "libgio-2.0.so.0!g_network_monitor_get_default",
            "libgio-2.0.so.0!g_network_monitor_get_network_metered",
            "libgobject-2.0.so.0!g_signal_connect_data",
            "libgobject-2.0.so.0!g_signal_handler_disconnect",
            "libgtk-3.so.0!gtk_clipboard_get",
            "libgdk-3.so.0!gdk_atom_intern_static_string",
            "libgdk-3.so.0!gdk_atom_intern",
            "libgdk-3.so.0!gdk_atom_name",
            "libgtk-3.so.0!gtk_target_list_new",
            "libgtk-3.so.0!gtk_target_list_add",
            "libgtk-3.so.0!gtk_target_list_add_text_targets",
            "libgtk-3.so.0!gtk_target_list_add_image_targets",
            "libgtk-3.so.0!gtk_target_list_unref",
            "libgtk-3.so.0!gtk_target_table_new_from_list",
            "libgtk-3.so.0!gtk_target_table_free",
            "libgtk-3.so.0!gtk_clipboard_set_with_data",
            "libgtk-3.so.0!gtk_clipboard_wait_for_text",
            "libgtk-3.so.0!gtk_clipboard_wait_for_uris",
            "libgtk-3.so.0!gtk_clipboard_wait_for_contents",
            "libgtk-3.so.0!gtk_clipboard_wait_for_image",
            "libgtk-3.so.0!gtk_clipboard_wait_for_targets",
            "libgtk-3.so.0!gtk_targets_include_text",
            "libgtk-3.so.0!gtk_targets_include_image",
            "libgtk-3.so.0!gtk_selection_data_get_target",
            "libgtk-3.so.0!gtk_selection_data_get_length",
            "libgtk-3.so.0!gtk_selection_data_get_data",
            "libgtk-3.so.0!gtk_selection_data_free",
            "libgtk-3.so.0!gtk_selection_data_set_text",
            "libgtk-3.so.0!gtk_selection_data_set",
            "libgtk-3.so.0!gtk_selection_data_set_uris",
            "libgtk-3.so.0!gtk_selection_data_set_pixbuf",
            "libgdk_pixbuf-2.0.so.0!gdk_pixbuf_get_has_alpha",
            "libgdk_pixbuf-2.0.so.0!gdk_pixbuf_get_width",
            "libgdk_pixbuf-2.0.so.0!gdk_pixbuf_get_height",
            "libgdk_pixbuf-2.0.so.0!gdk_pixbuf_get_rowstride",
            "libglib-2.0.so.0!g_strv_length",
            "libglib-2.0.so.0!g_strfreev",
            "libglib-2.0.so.0!g_filename_from_uri",
            "libglib-2.0.so.0!g_filename_to_uri",
            "libgtk-3.so.0!gtk_file_chooser_native_new",
            "libgtk-3.so.0!gtk_file_chooser_set_current_name",
            "libgtk-3.so.0!gtk_file_chooser_set_do_overwrite_confirmation",
            "libgtk-3.so.0!gtk_file_chooser_set_select_multiple",
            "libgtk-3.so.0!gtk_file_chooser_set_current_folder",
            "libgtk-3.so.0!gtk_file_chooser_add_filter",
            "libgtk-3.so.0!gtk_file_chooser_set_filter",
            "libgtk-3.so.0!gtk_file_chooser_get_filter",
            "libgtk-3.so.0!gtk_file_chooser_get_filenames",
            "libgtk-3.so.0!gtk_file_chooser_get_filename",
            "libgtk-3.so.0!gtk_file_filter_new",
            "libgtk-3.so.0!gtk_file_filter_set_name",
            "libgtk-3.so.0!gtk_file_filter_add_pattern",
            "libgtk-3.so.0!gtk_native_dialog_run",
            "libglib-2.0.so.0!g_slist_length",
            "libglib-2.0.so.0!g_slist_nth",
            "libglib-2.0.so.0!g_slist_append",
            "libglib-2.0.so.0!g_slist_index",
            "libglib-2.0.so.0!g_slist_free",
            "libgdk-3.so.0!gdk_x11_window_lookup_for_display",
            "libgdk-3.so.0!gdk_window_get_user_data",
            "libgtk-3.so.0!gtk_widget_get_toplevel");

    /**
     * The exports of {@code libglassgtk3.so} ({@code glass_gtk_api.h}) the facade binds when the toolkit installs the
     * callback tables, after {@link #SYMBOLS}, in binding order: the ABI version first, the sizeof probes, the table
     * installers, then one function per former JNI native of {@code GtkApplication}, {@code GtkWindow},
     * {@code GtkView} and {@code GtkDnDClipboard}, in the order of the header - all but {@code _terminateLoop}
     * ({@code gtk_main_quit}, in {@link #SYMBOLS}), {@code _getNativeView}, {@code _scheduleRepaint} and
     * {@code pushTargetActionToSystem}, which Java answers itself.
     */
    static final List<String> GLASS_SYMBOLS = List.of(
            "libglassgtk3.so!ggtk_abi_version",
            "libglassgtk3.so!ggtk_sizeof_app_callbacks",
            "libglassgtk3.so!ggtk_sizeof_window_callbacks",
            "libglassgtk3.so!ggtk_sizeof_view_callbacks",
            "libglassgtk3.so!ggtk_sizeof_dnd_callbacks",
            "libglassgtk3.so!ggtk_sizeof_dnd_data",
            "libglassgtk3.so!ggtk_sizeof_dnd_target_value",
            "libglassgtk3.so!ggtk_app_set_callbacks",
            "libglassgtk3.so!ggtk_window_set_callbacks",
            "libglassgtk3.so!ggtk_view_set_callbacks",
            "libglassgtk3.so!ggtk_dnd_set_callbacks",
            "libglassgtk3.so!ggtk_application_init_gtk",
            "libglassgtk3.so!ggtk_application_init",
            "libglassgtk3.so!ggtk_application_run_loop",
            "libglassgtk3.so!ggtk_application_get_key_code_for_char",
            "libglassgtk3.so!ggtk_window_create",
            "libglassgtk3.so!ggtk_window_close",
            "libglassgtk3.so!ggtk_window_set_view",
            "libglassgtk3.so!ggtk_window_update_view_size",
            "libglassgtk3.so!ggtk_window_minimize",
            "libglassgtk3.so!ggtk_window_maximize",
            "libglassgtk3.so!ggtk_window_set_bounds",
            "libglassgtk3.so!ggtk_window_set_visible",
            "libglassgtk3.so!ggtk_window_set_resizable",
            "libglassgtk3.so!ggtk_window_request_focus",
            "libglassgtk3.so!ggtk_window_set_focusable",
            "libglassgtk3.so!ggtk_window_grab_focus",
            "libglassgtk3.so!ggtk_window_ungrab_focus",
            "libglassgtk3.so!ggtk_window_set_title",
            "libglassgtk3.so!ggtk_window_set_level",
            "libglassgtk3.so!ggtk_window_set_alpha",
            "libglassgtk3.so!ggtk_window_set_background",
            "libglassgtk3.so!ggtk_window_set_enabled",
            "libglassgtk3.so!ggtk_window_set_minimum_size",
            "libglassgtk3.so!ggtk_window_set_system_minimum_size",
            "libglassgtk3.so!ggtk_window_set_maximum_size",
            "libglassgtk3.so!ggtk_window_set_icon",
            "libglassgtk3.so!ggtk_window_to_front",
            "libglassgtk3.so!ggtk_window_to_back",
            "libglassgtk3.so!ggtk_window_set_cursor_type",
            "libglassgtk3.so!ggtk_window_set_cursor",
            "libglassgtk3.so!ggtk_window_show_system_menu",
            "libglassgtk3.so!ggtk_window_is_visible",
            "libglassgtk3.so!ggtk_window_get_native_window",
            "libglassgtk3.so!ggtk_view_enable_input_method_events",
            "libglassgtk3.so!ggtk_view_create",
            "libglassgtk3.so!ggtk_view_get_x",
            "libglassgtk3.so!ggtk_view_get_y",
            "libglassgtk3.so!ggtk_view_set_parent",
            "libglassgtk3.so!ggtk_view_close",
            "libglassgtk3.so!ggtk_view_upload_pixels_direct",
            "libglassgtk3.so!ggtk_view_upload_pixels_int",
            "libglassgtk3.so!ggtk_view_upload_pixels_byte",
            "libglassgtk3.so!ggtk_view_enter_fullscreen",
            "libglassgtk3.so!ggtk_view_exit_fullscreen",
            "libglassgtk3.so!ggtk_dnd_is_owner",
            "libglassgtk3.so!ggtk_dnd_push_to_system",
            "libglassgtk3.so!ggtk_dnd_target_get_supported_actions",
            "libglassgtk3.so!ggtk_dnd_target_get_mimes",
            "libglassgtk3.so!ggtk_dnd_target_get_data");

    private static List<String> allSymbols() {
        List<String> all = new ArrayList<>(SYMBOLS);
        all.addAll(GLASS_SYMBOLS);
        return all;
    }

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() {
        GtkGlassChildJvm.requireDisplay();
        run = GtkGlassChildJvm.run(GtkGlassNativeBindingTest.class, "bindingScenario", List.of());
    }

    @Test
    public void facadeBindsExactlyTheseSymbolsInThisOrder() {
        assertEquals(String.join(",", allSymbols()), run.values().get("bound"), run::describe);
    }

    @Test
    public void everySymbolLiesInTheOnlyMappingOfItsLibrary() {
        for (String symbol : allSymbols()) {
            String soname = symbol.substring(0, symbol.indexOf('!'));
            String file = run.values().get("file." + symbol);
            assertTrue(file != null && Path.of(file).getFileName().toString().startsWith(soname),
                    symbol + " is not inside " + soname + ": " + file);
            assertEquals("1", run.values().get("mappedFiles." + soname),
                    "files mapped for " + soname + ": " + run.values().get("mappedFileNames." + soname));
        }
    }

    /**
     * The two struct layouts of the facade that need a display, against the libraries themselves: {@code XkbGetState}
     * writes nothing past {@code XKB_STATE_REC_SIZE} bytes, and every {@code GdkKeymapKey} read in steps of
     * {@code GDK_KEYMAP_KEY_SIZE} names a key that XKB maps to the keyval it was asked for.
     */
    @Test
    public void structLayoutsMatchTheLibraries() {
        assertEquals("status=0 canary=intact", run.values().get("layout.xkbStateRec"), run::describe);
        String keys = run.values().get("layout.gdkKeymapKey");
        assertTrue(keys != null && keys.endsWith(" mismatches="), keys);
        assertTrue(!keys.startsWith("entries=0 ") && !keys.contains(" laterEntries=0 "), keys);
    }

    /** Runs in {@link GtkGlassChild}. */
    static void bindingScenario(Map<String, String> out) throws Exception {
        List<String> bound = GtkGlassChild.onFx(GtkGlassShim::boundSymbols);
        out.put("bound", String.join(",", bound));
        out.put("layout.xkbStateRec", GtkGlassChild.onFx(GtkGlassShim::xkbStateRecLayout));
        out.put("layout.gdkKeymapKey", GtkGlassChild.onFx(GtkGlassShim::keymapKeyLayout));
        List<long[]> ranges = new ArrayList<>();
        List<String> files = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of("/proc/self/maps"))) {
            String[] fields = line.trim().split("\\s+", 6);
            if (fields.length < 6 || !fields[5].startsWith("/")) {
                continue;
            }
            String[] range = fields[0].split("-");
            ranges.add(new long[] {Long.parseUnsignedLong(range[0], 16), Long.parseUnsignedLong(range[1], 16)});
            files.add(fields[5]);
        }
        Set<String> sonames = new TreeSet<>();
        for (String symbol : bound) {
            sonames.add(symbol.substring(0, symbol.indexOf('!')));
            long address = GtkGlassShim.boundAddress(symbol);
            for (int i = 0; i < ranges.size(); i++) {
                if (Long.compareUnsigned(address, ranges.get(i)[0]) >= 0
                        && Long.compareUnsigned(address, ranges.get(i)[1]) < 0) {
                    out.put("file." + symbol, files.get(i));
                }
            }
        }
        for (String soname : sonames) {
            Set<String> mapped = new TreeSet<>();
            for (String file : files) {
                if (Path.of(file).getFileName().toString().startsWith(soname)) {
                    mapped.add(realPath(file));
                }
            }
            out.put("mappedFiles." + soname, Integer.toString(mapped.size()));
            out.put("mappedFileNames." + soname, String.join(",", mapped));
        }
    }

    private static String realPath(String file) {
        try {
            return Path.of(file).toRealPath().toString();
        } catch (IOException e) {
            return file;
        }
    }
}
