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
 * webkit_java_api_theme.h - the WebCore platform-abstraction third of the jfxwebkit C ABI.
 *
 * Scope, exactly (FFM-ABI-CONTRACT.md section 12.1):
 *
 *   Source/WebCore/platform/java     the widget theme, the scrollbar theme, cursors, the
 *                                    context menu, the pasteboard, screen metrics, the
 *                                    shared timer, localized strings, IDN, SharedBuffer,
 *                                    WCWidget and WCPluginWidget
 *   Source/WTF/wtf/java/FileSystemJava.cpp   com.sun.webkit.FileSystem - the filesystem
 *                                    table only. That source belongs to the WTF slice; the
 *                                    table is defined here so the slice is not blocked on
 *                                    a header that has no other owner.
 *
 * It carries two things:
 *
 *   1. the WKJHostTheme and WKJHostFileSystem callback tables, which replace the 43 cached
 *      member-id upcall sites of platform/java and the 11 of FileSystemJava.cpp;
 *   2. the 10 wkj_ entry points that replace the 12 exported JNI functions of platform/java.
 *      The other two, WCWidget_initIDs and WCPluginWidget_initIDs, get no entry point at
 *      all - see WHAT IS DELIBERATELY NOT HERE below.
 *
 * The shared constants this slice uses are NOT here: they live in the generated
 * wkj_constants.h, which replaces all 23 com_sun_webkit_*.h headers and keeps their mangled
 * names, so each call site changed only its include. RenderThemeJava.cpp in particular
 * composes them with the token-pasting macros JNI_EXPAND and JNI_EXPAND_MEDIA, and the
 * composed names are unchanged.
 *
 * ------------------------------------------------------------------------------------------
 * WHY THE STRUCT IS CALLED "theme" WHEN IT COVERS MORE THAN THE THEME
 * ------------------------------------------------------------------------------------------
 * WKJHost has one member per callback group and the group names are fixed by contract
 * section 4; "theme" is the one that names this directory's largest and most-called client
 * (RenderTheme plus ScrollBarTheme paint every frame). The other clients of
 * Source/WebCore/platform/java - cursors, the context menu, the pasteboard, screen metrics,
 * the shared timer, WCWidget, WCPluginWidget, LocalizedStrings and java.net.IDN - have no
 * group of their own and would each need a new WKJHost member, i.e. an ABI shape change per
 * client for tables of one to ten slots. They are therefore sections of this one struct,
 * separated by the dividers below, exactly as WKJHostGraphics carries WCPath, WCFont,
 * WCImage and WCImageDecoder in one table.
 *
 * ------------------------------------------------------------------------------------------
 * INTEGRATION - the edit this header requires in webkit_java_api.h
 * ------------------------------------------------------------------------------------------
 * webkit_java_api.h carried placeholders for the two groups defined here:
 *
 *     typedef struct WKJHostFileSystem  { void (*reserved)(void); } WKJHostFileSystem;
 *     typedef struct WKJHostTheme       { void (*reserved)(void); } WKJHostTheme;
 *
 * Those two lines are deleted and replaced by
 *
 *     #include "webkit_java_api_theme.h"
 *
 * at the same place - above the WKJHost definition, below WKJHostCore, beside the identical
 * include of webkit_java_api_platform.h. The structs below are the real definitions of those
 * two types, not additional types, so the two states cannot coexist in one translation unit;
 * leaving a placeholder in place is a redefinition error, which is exactly what the combined
 * compile check catches.
 *
 * The include direction is the same as webkit_java_api_platform.h's and the opposite of
 * webkit_java_api_dom.h's and webkit_java_api_page.h's: the master includes this header, and
 * this header does not include the master. It cannot. The master needs these two definitions
 * in the MIDDLE of its own body (WKJHost has them as members, so they must be complete types
 * by then), and a mutual include cannot deliver that - whichever header the translation unit
 * names first sets its own guard, so the second include is a no-op and one of the two bodies
 * is processed with the other's types still missing. One-way is the only arrangement that
 * compiles, and it costs nothing: everything this header needs - wkj_ref, WKJ_EXPORT, the
 * WKJ_STR_ codes - is declared above the include point.
 *
 * Verified with cl.exe 14.44.35207 at /W4 /WX, as C (/TC) and as C++ (/TP /EHsc), both
 * standalone and together with webkit_java_api.h, _dom, _page, _platform and wkj_constants.h.
 *
 * ------------------------------------------------------------------------------------------
 * CONVENTIONS - all inherited from webkit_java_api.h; only the additions are restated
 * ------------------------------------------------------------------------------------------
 * Strings         UTF-16, "const uint16_t* s, int32_t s_len" inbound (s == NULL means the
 *                 Java value was null, and collapses to the empty WTF::String exactly as
 *                 wtf/java/StringJava.cpp always did - contract 11.1); caller-provided
 *                 buffer outbound, returning WKJ_STR_OK, WKJ_STR_NULL or WKJ_STR_OVERFLOW
 *                 (contract 13). There is no library-owned string memory anywhere in this
 *                 ABI and therefore no lifetime rule: a caller that needs two strings out of
 *                 Java calls twice, each with its own buffer, and a callback that takes two
 *                 strings takes two independent (pointer, length) pairs.
 * Booleans        int32_t, 0 or 1. FFM has no boolean layout.
 * Java objects    wkj_ref (contract 3). A slot that RETURNS a wkj_ref returns a NEW id that
 *                 the library owns and must release exactly once - hold it in a WKJHandle.
 *                 A wkj_ref PARAMETER is borrowed for the duration of the call.
 * Native objects  int64_t peers, converted with wkj_to_ptr and wkj_from_ptr (contract 12:
 *                 the peer is int64_t, not void*, because Java already holds it as a long).
 * Out-parameters  A fixed-size array out-parameter is written only when the function returns
 *                 1; a return of 0 means "Java gave us nothing" and leaves the buffer alone.
 *                 That is how the WCRectangle and int[] returns of the JNI code are spelled
 *                 without an object crossing the boundary.
 * NULL slots      every callback pointer may be NULL. The library tests it before every call
 *                 and falls back to the default documented on the slot.
 * Upcall failure  a Java exception never propagates. Where the JNI code branched on
 *                 the JNI exception check, the library still calls
 *                 wkj_host->core.check_and_clear_exception() in the same place, and where the
 *                 JNI code only cleared, the library only clears. That swallowing is
 *                 deliberate and must not be "fixed" here.
 * Threading       everything in the WKJHostTheme table is reached from the WebKit main thread
 *                 (which is the JavaFX application thread): the themes from paint, the
 *                 context menu and the cursor from event handling, the pasteboard from the
 *                 editor. WKJHostFileSystem is the exception and is reached from ANY thread -
 *                 see the note on that struct. Every upcall stub therefore lives in one
 *                 Arena.ofShared().
 *
 * JAVA-SIDE CONSTRAINT, stated here so the binding author cannot miss it:
 * Linker.Option.critical(true) is forbidden on every function in this header.
 * wkj_shared_buffer_get_some_data and wkj_shared_buffer_append look like the exception -
 * both are short memcpy bodies over a heap byte[] and the JNI versions did use
 * GetPrimitiveArrayCritical - but SharedBufferBuilder::append allocates, WebKit's allocator
 * is not guaranteed non-blocking, and neither is on a measured hot path. Everything else here
 * either re-enters WebKit (wkj_timer_fire runs the whole timer queue;
 * wkj_context_menu_item_selected dispatches a menu action, which can run script) or happens
 * once per user gesture.
 *
 * ------------------------------------------------------------------------------------------
 * WHAT IS DELIBERATELY NOT HERE
 * ------------------------------------------------------------------------------------------
 * Two of the twelve exported JNI functions of Source/WebCore/platform/java get no wkj_ entry
 * point, because the native-necessity triage (FFM-AUDIT-wtf-webcore.md 15.5, rows 13 and 14)
 * rules both WRAPPER: their bodies are nothing but member-id lookups, so there
 * is no native work left once Java owns method dispatch. They become the widget_ and
 * plugin_widget_ sections of WKJHostTheme below, and the two Java native declarations they
 * implemented are deleted rather than rebound:
 *
 *   Java_com_sun_webkit_WCWidget_initIDs        WidgetJava.cpp:225
 *       orphans WCWidget.initIDs() at WCWidget.java:122 and the static block at :36
 *   Java_com_sun_webkit_WCPluginWidget_initIDs  PluginWidgetJava.cpp:63
 *       orphans WCPluginWidget.initIDs() at WCPluginWidget.java:48 and the static block at :51
 *
 * Also absent, for the same triage reason, are callback slots for
 * com.sun.webkit.plugin.PluginManager and PluginHandler. FFM-AUDIT-wtf-webcore.md section 7
 * counts 14 upcall sites in PluginDataJava.cpp and PluginInfoStoreJava.cpp, but every one of
 * them sits inside a commented-out block: init_plugins() has an empty body and
 * PluginInfoStore has no live function at all. Giving dead comments a C ABI would entrench
 * them.
 */

#ifndef WEBKIT_JAVA_API_THEME_H
#define WEBKIT_JAVA_API_THEME_H

/*
 * Included by webkit_java_api.h, not the other way round - see INTEGRATION above. Naming this
 * header directly is a mistake worth catching, because the two struct definitions below would
 * then be processed before wkj_ref and WKJ_EXPORT exist.
 */
#ifndef WEBKIT_JAVA_API_H
#error "include webkit_java_api.h; it includes this header at the right point"
#endif

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ======================================================================================== */
/* WKJHostTheme - Source/WebCore/platform/java                                              */
/* ======================================================================================== */

/*
 * Replaces the 43 cached-member-id upcall sites of
 * Source/WebCore/platform/java, against com.sun.webkit.{CursorManager, ContextMenu,
 * ContextMenuItem, LocalizedStrings, Timer, WCPasteboard, WCPluginWidget, WCWidget,
 * WebPage}, com.sun.webkit.graphics.{RenderTheme, RenderMediaControls, ScrollBarTheme} and
 * java.net.IDN.
 *
 * The 18 PG_Get*Class accessors of PlatformJavaClasses.cpp disappear with the table: they
 * were class-lookup caching so that method ids could be resolved on the result, and Java
 * resolves its own classes. Only the two that did real work survive, as the get_render_theme
 * and get_scroll_bar_theme slots below.
 */
typedef struct WKJHostTheme {

    /* --- com.sun.webkit.graphics.RenderTheme ------------------------------------------- */

    /*
     * The RenderTheme of a page, as PlatformJavaClasses.cpp's
     * PG_GetRenderThemeObjectFromPage did with its two branches in one function:
     *
     *   web_page != 0  ->  WebPage.getRenderTheme()            (instance)
     *   web_page == 0  ->  WebPage.fwkGetDefaultRenderTheme()  (static)
     *
     * Both branches are live: RenderThemeJava passes 0 for the selection colours and for the
     * radio-button size, and WebPage::jRenderTheme() passes its own page. Returns a new
     * RenderTheme id, or 0. Default when NULL: 0.
     */
    wkj_ref (*get_render_theme)(wkj_ref web_page);

    /*
     * RenderTheme.createWidget(long id, int widgetIndex, int state, int w, int h,
     * int bgColor, ByteBuffer extParams) -> Ref.
     *
     * "id" is the RenderElement* the Java side hands back to WebKit unchanged. "ext_params"
     * points at "ext_params_length" bytes of native-endian scratch owned by the caller and
     * valid only for the duration of the call - Java wraps the address without copying,
     * exactly as NewDirectByteBuffer did, and must not retain it. It is NULL with length 0
     * for every widget except SLIDER, PROGRESS_BAR and METER, which is the null ByteBuffer
     * the JNI code passed for an empty extParams vector.
     *
     * A 0 return is meaningful, not an error: RenderThemeJava reads it as "the Java theme
     * declines this widget" and falls back to WebKit's own rendering.
     * Returns a new Ref id, or 0. Default when NULL: 0.
     */
    wkj_ref (*create_widget)(wkj_ref theme, int64_t id, int32_t widget_index, int32_t state,
                             int32_t width, int32_t height, int32_t background_color,
                             const void* ext_params, int32_t ext_params_length);

    /* RenderTheme.getRadioButtonSize(). Default when NULL: 0. */
    int32_t (*get_radio_button_size)(wkj_ref theme);

    /*
     * RenderTheme.getSelectionColor(int), packed 0xAARRGGBB. "index" is
     * com_sun_webkit_graphics_RenderTheme_BACKGROUND or _FOREGROUND. Default when NULL: 0,
     * i.e. transparent black - which is what the JNI code produced when the call failed,
     * because it did not test.
     */
    int32_t (*get_selection_color)(wkj_ref theme, int32_t index);

    /* --- com.sun.webkit.graphics.RenderMediaControls (static; no target ref) ------------ */

    /*
     * RenderMediaControls.fwkGetSliderThumbSize(int type) -> int, with the width in the high
     * 16 bits and the height in the low 16. "slider_type" is
     * com_sun_webkit_graphics_RenderMediaControls_SLIDER_TYPE_TIME or _VOLUME. The packing is
     * kept rather than split into two out-parameters because it is what the Java method
     * returns and unpacking it is one line on the C++ side, exactly as today.
     * Default when NULL: 0, i.e. a zero-sized thumb.
     */
    int32_t (*get_slider_thumb_size)(int32_t slider_type);

    /* --- com.sun.webkit.graphics.ScrollBarTheme ----------------------------------------- */

    /*
     * WebPage.getScrollBarTheme() on the page a scrollbar belongs to. Unlike get_render_theme
     * there is no static fallback: ScrollbarThemeJava returns an empty rect or skips painting
     * when the scrollbar has been detached or its page is a utility page, and that test
     * happens in C++ before this slot is reached.
     * Returns a new ScrollBarTheme id, or 0. Default when NULL: 0.
     */
    wkj_ref (*get_scroll_bar_theme)(wkj_ref web_page);

    /*
     * ScrollBarTheme.createWidget(long id, int w, int h, int orientation, int value,
     * int visibleSize, int totalSize) -> Ref. "id" is the Scrollbar*.
     * Returns a new Ref id, or 0. Default when NULL: 0.
     */
    wkj_ref (*scroll_bar_create_widget)(wkj_ref theme, int64_t id,
                                        int32_t width, int32_t height, int32_t orientation,
                                        int32_t value, int32_t visible_size,
                                        int32_t total_size);

    /*
     * ScrollBarTheme.getScrollBarPartRect(long id, int part, int[4]), formerly an int[] the
     * C++ allocated with NewIntArray and read back with GetPrimitiveArrayCritical. Writes x,
     * y, width and height and returns 1; returns 0 and writes nothing when Java wrote
     * nothing. "part" is one of the com_sun_webkit_graphics_ScrollBarTheme_ part constants.
     * Default when NULL: 0.
     */
    int32_t (*scroll_bar_get_part_rect)(wkj_ref theme, int64_t id, int32_t part,
                                        int32_t out_xywh[4]);

    /*
     * ScrollBarTheme.getThickness() - a static, so no target ref. Default when NULL: 0. Note
     * that the Java implementation already substitutes 12 for an unset thickness, so the
     * library never had to.
     */
    int32_t (*scroll_bar_get_thickness)(void);

    /* --- com.sun.webkit.CursorManager --------------------------------------------------- */
    /*
     * The manager is a Java singleton reached through CursorManager.getCursorManager(), so
     * neither slot takes a target ref. The JNI code produced a null cursor when the singleton
     * was absent; the defaults below reproduce that without the extra round trip.
     */

    /*
     * CursorManager.getPredefinedCursorID(int type) -> long. "type" is one of the
     * com_sun_webkit_CursorManager_ constants. The result is an opaque platform cursor id
     * that WebCore stores as PlatformCursor and hands back to widget_set_cursor.
     * Default when NULL: 0, which is the null cursor.
     */
    int64_t (*cursor_get_predefined_id)(int32_t type);

    /*
     * CursorManager.getCustomCursorID(WCImageFrame, int x, int y) -> long.
     * Default when NULL: 0.
     */
    int64_t (*cursor_get_custom_id)(wkj_ref image_frame, int32_t hotspot_x,
                                    int32_t hotspot_y);

    /* --- com.sun.webkit.ContextMenu and ContextMenuItem --------------------------------- */

    /* ContextMenu.fwkCreateContextMenu() - static. Returns a new id. Default: 0. */
    wkj_ref (*context_menu_create)(void);

    /* ContextMenuItem.fwkCreateContextMenuItem() - static. Returns a new id. Default: 0. */
    wkj_ref (*context_menu_item_create)(void);

    /* fwkSetType(int), a com_sun_webkit_ContextMenuItem_ constant. Default: no-op. */
    void (*context_menu_item_set_type)(wkj_ref item, int32_t type);

    /* fwkSetAction(int), a WebCore ContextMenuAction. Default when NULL: no-op. */
    void (*context_menu_item_set_action)(wkj_ref item, int32_t action);

    /*
     * fwkSetTitle(String). The library passes NULL for an EMPTY title, not the empty string:
     * the JNI code wrote "title.isEmpty() ? NULL : title.toJavaString(env)" and the Java side
     * may well distinguish them. Do not normalise it, here or on the Java side.
     * Default when NULL: no-op.
     */
    void (*context_menu_item_set_title)(wkj_ref item, const uint16_t* title,
                                        int32_t title_len);

    /* fwkSetSubmenu(ContextMenu); "submenu" may be 0. Default when NULL: no-op. */
    void (*context_menu_item_set_submenu)(wkj_ref item, wkj_ref submenu);

    /* fwkSetChecked(boolean). Default when NULL: no-op. */
    void (*context_menu_item_set_checked)(wkj_ref item, int32_t checked);

    /* fwkSetEnabled(boolean). Default when NULL: no-op. */
    void (*context_menu_item_set_enabled)(wkj_ref item, int32_t enabled);

    /* ContextMenu.fwkAppendItem(ContextMenuItem). Default when NULL: no-op. */
    void (*context_menu_append_item)(wkj_ref menu, wkj_ref item);

    /*
     * ContextMenu.fwkShow(WebPage, long menuCtrlPData, int x, int y). "controller" is the
     * WebCore::ContextMenuController* that wkj_context_menu_item_selected carries back when
     * the user picks an item; the menu outlives this call, so the Java side keeps it.
     * Default when NULL: no-op.
     */
    void (*context_menu_show)(wkj_ref menu, wkj_ref web_page, int64_t controller,
                              int32_t x, int32_t y);

    /* --- com.sun.webkit.WCWidget -------------------------------------------------------- */
    /*
     * This section is the whole replacement for Java_com_sun_webkit_WCWidget_initIDs
     * (WidgetJava.cpp:225), whose body was five method-id lookups. The last two slots are
     * PlatformScreenJava's, which cached its own ids against the same class.
     */

    /* fwkSetBounds(int, int, int, int). Default when NULL: no-op. */
    void (*widget_set_bounds)(wkj_ref widget, int32_t x, int32_t y,
                              int32_t width, int32_t height);

    /* fwkRequestFocus(). Default when NULL: no-op. */
    void (*widget_request_focus)(wkj_ref widget);

    /*
     * fwkSetCursor(long). "cursor_id" is what cursor_get_predefined_id or
     * cursor_get_custom_id returned, i.e. WebCore::PlatformCursor. Default when NULL: no-op.
     */
    void (*widget_set_cursor)(wkj_ref widget, int64_t cursor_id);

    /* fwkSetVisible(boolean). Default when NULL: no-op. */
    void (*widget_set_visible)(wkj_ref widget, int32_t visible);

    /* fwkDestroy(). Called from ~Widget. Default when NULL: no-op. */
    void (*widget_destroy)(wkj_ref widget);

    /*
     * fwkGetScreenDepth(). Default when NULL: 0; the caller substitutes 24, as it always has
     * for a missing page client.
     */
    int32_t (*widget_get_screen_depth)(wkj_ref widget);

    /*
     * fwkGetScreenRect(boolean available) -> WCRectangle, formerly four GetFloatField reads
     * on the returned object. Writes x, y, width and height and returns 1; returns 0 and
     * writes nothing when Java returned null, which the caller turns into an empty rect
     * exactly as before. Default when NULL: 0.
     */
    int32_t (*widget_get_screen_rect)(wkj_ref widget, int32_t available, float out_xywh[4]);

    /* --- com.sun.webkit.WCPluginWidget -------------------------------------------------- */
    /*
     * This section is the whole replacement for Java_com_sun_webkit_WCPluginWidget_initIDs
     * (PluginWidgetJava.cpp:63), whose body was four method-id plus five field-id lookups -
     * the five being the pData long and the four float fields of WCRectangle.
     */

    /*
     * WCPluginWidget.create(WebPage, int w, int h, String url, String mimeType,
     * String[] paramNames, String[] paramValues) -> WCPluginWidget - a static factory.
     *
     * The two String[] arguments cross as parallel arrays: "param_names" and "param_values"
     * each point at "param_count" UTF-16 pointers, and "param_name_lengths" and
     * "param_value_lengths" at "param_count" lengths. A count of 0 means both arrays are
     * empty, which is what strVect2JArray produced for an empty Vector; the four pointers may
     * be NULL in that case. This shape is used rather than a separator-joined single string
     * because a plugin parameter value is arbitrary HTML attribute text, with no character
     * that cannot occur in it.
     *
     * Note that the JNI code passed width and height as 0 unconditionally - PluginWidgetJava
     * declared both locals and never assigned them - and that is preserved.
     * Returns a new WCPluginWidget id, or 0. Default: 0.
     */
    wkj_ref (*plugin_widget_create)(wkj_ref web_page, int32_t width, int32_t height,
                                    const uint16_t* url, int32_t url_len,
                                    const uint16_t* mime_type, int32_t mime_type_len,
                                    const uint16_t* const* param_names,
                                    const int32_t* param_name_lengths,
                                    const uint16_t* const* param_values,
                                    const int32_t* param_value_lengths,
                                    int32_t param_count);

    /*
     * Stores the WebCore::PluginWidgetJava* in the widget's pData field, replacing the
     * SetLongField the constructor did. It is what the three wkj_plugin_widget_ entry points
     * below receive back as their first argument, and it is why those no longer need a field
     * id. Default when NULL: no-op - in which case Java can never call back, which is the
     * same outcome the JNI code had if the field id had failed to resolve.
     */
    void (*plugin_widget_set_peer)(wkj_ref plugin_widget, int64_t peer);

    /*
     * WCPluginWidget.paint(WCGraphicsContext, int x, int y, int w, int h).
     *
     * BUG PRESERVED, deliberately. The JNI call passed context.platformContext() - a
     * WebCore::PlatformContextJava*, i.e. a C++ pointer - where the method signature demands
     * a com.sun.webkit.graphics.WCGraphicsContext object. That is not a WCGraphicsContext and
     * never was. The ABI therefore declares the parameter as the int64_t it actually is
     * rather than as a wkj_ref, so the mistake is visible in the signature instead of hidden
     * behind a cast, and so Java receives a value it can reject rather than a bogus object
     * reference. Fixing it means deciding what a plugin widget should be handed to draw with,
     * which is a behaviour change and not this migration's business.
     * Default when NULL: no-op.
     */
    void (*plugin_widget_paint)(wkj_ref plugin_widget, int64_t graphics_context,
                                int32_t x, int32_t y, int32_t width, int32_t height);

    /* fwkSetNativeContainerBounds(int, int, int, int). Default when NULL: no-op. */
    void (*plugin_widget_set_native_container_bounds)(wkj_ref plugin_widget,
                                                      int32_t x, int32_t y,
                                                      int32_t width, int32_t height);

    /*
     * fwkHandleMouseEvent(String type, int x, int y, int screenX, int screenY, int button,
     * boolean buttonDown, boolean alt, boolean meta, boolean ctrl, boolean shift,
     * long timeStamp) -> boolean, where 1 cancels bubbling. The argument order is the Java
     * one. Default when NULL: 0.
     */
    int32_t (*plugin_widget_handle_mouse_event)(wkj_ref plugin_widget,
                                                const uint16_t* type, int32_t type_len,
                                                int32_t x, int32_t y,
                                                int32_t screen_x, int32_t screen_y,
                                                int32_t button, int32_t button_down,
                                                int32_t alt, int32_t meta,
                                                int32_t ctrl, int32_t shift,
                                                int64_t timestamp);

    /* --- com.sun.webkit.Timer (static; no target ref) ----------------------------------- */
    /*
     * MainThreadSharedTimer. Both slots ran behind WC_GETJAVAENV_CHKRET, which returned early
     * on a null environment - the shutdown gate described in the api/README.md "Phase B
     * hazards" note. With no environment to be null, that gate is gone, so the Java side must
     * tolerate a set_fire_time or a stop arriving during teardown; detaching the host table
     * is how it stops them.
     */

    /* Timer.fwkSetFireTime(double), seconds since the POSIX epoch. Default: no-op. */
    void (*timer_set_fire_time)(double fire_time);

    /* Timer.fwkStopTimer(). Default when NULL: no-op. */
    void (*timer_stop)(void);

    /* --- com.sun.webkit.LocalizedStrings (static; no target ref) ------------------------ */

    /*
     * LocalizedStrings.getLocalizedProperty(String) -> String, contract-13 protocol.
     * WKJ_STR_NULL means the property is unknown, and the caller then returns the NAME it
     * asked for - that fallback is in the C++ today and stays there.
     * Default when NULL: WKJ_STR_NULL.
     */
    int32_t (*get_localized_property)(const uint16_t* name, int32_t name_len,
                                      uint16_t* out_buf, int32_t out_cap,
                                      int32_t* out_length);

    /* --- java.net.IDN (a JDK class; static) --------------------------------------------- */

    /*
     * IDN.toASCII(String, int flags) -> String, contract-13 protocol. "flags" is
     * com_sun_webkit_network_URLLoaderBase_ALLOW_UNASSIGNED, which is the only value the
     * library passes; it is passed rather than assumed so the Java side stays a plain
     * forwarder.
     *
     * FFM-AUDIT-wtf-webcore.md 15.5 row 10 rules this WRAPPER, and it is: java.net.IDN is a
     * JDK API that Java could reach without leaving Java. It keeps a slot because the CALLER
     * is WebCore C++ (URL parsing), not Java - there is no Java native to delete here, only a
     * choice between this upcall and ICU's uidna_nameToASCII. Those two are not equivalent
     * (java.net.IDN is IDNA2003; ICU defaults to UTS-46), so switching is a behaviour change
     * that needs the IDNA test vectors run against both first. Until then, the Java call.
     * Default when NULL: WKJ_STR_NULL, which the caller turns into the empty string.
     */
    int32_t (*idn_to_ascii)(const uint16_t* hostname, int32_t hostname_len, int32_t flags,
                            uint16_t* out_buf, int32_t out_cap, int32_t* out_length);

    /* --- com.sun.webkit.WCPasteboard (static; no target ref) ---------------------------- */
    /*
     * The system clipboard. These are reached only when the Pasteboard is in copy/paste mode;
     * a drag-and-drop Pasteboard never leaves C++.
     */

    /*
     * WCPasteboard.getPlainText() -> String, contract-13 protocol. WKJ_STR_NULL is
     * distinguished from WKJ_STR_OK with length 0, and both reach the caller, which stores
     * the null String and the empty String respectively - the JNI code made the same
     * distinction with "jstr ? String(env, jstr) : String()".
     * Default when NULL: WKJ_STR_NULL.
     */
    int32_t (*pasteboard_get_plain_text)(uint16_t* out_buf, int32_t out_cap,
                                         int32_t* out_length);

    /* WCPasteboard.getHtml() -> String, same protocol. Default when NULL: WKJ_STR_NULL. */
    int32_t (*pasteboard_get_html)(uint16_t* out_buf, int32_t out_cap, int32_t* out_length);

    /* WCPasteboard.writePlainText(String). Default when NULL: no-op. */
    void (*pasteboard_write_plain_text)(const uint16_t* text, int32_t text_len);

    /*
     * WCPasteboard.writeSelection(boolean canSmartCopyOrDelete, String text, String markup).
     * Two live strings in one call: each gets its own (pointer, length) pair, held by its own
     * caller-side local for the duration of the call. That is the whole reason this ABI
     * passes strings flat instead of through any shared or library-owned buffer.
     * Default when NULL: no-op.
     */
    void (*pasteboard_write_selection)(int32_t can_smart_copy_or_delete,
                                       const uint16_t* text, int32_t text_len,
                                       const uint16_t* markup, int32_t markup_len);

    /* WCPasteboard.writeUrl(String url, String markup). Same two-string note as above. */
    void (*pasteboard_write_url)(const uint16_t* url, int32_t url_len,
                                 const uint16_t* markup, int32_t markup_len);

    /* WCPasteboard.writeImage(WCImageFrame). Default when NULL: no-op. */
    void (*pasteboard_write_image)(wkj_ref image_frame);

} WKJHostTheme;

/* ======================================================================================== */
/* WKJHostFileSystem - com.sun.webkit.FileSystem                                            */
/* ======================================================================================== */

/*
 * Replaces the 11 upcall sites of Source/WTF/wtf/java/FileSystemJava.cpp against the ten fwk*
 * statics of com.sun.webkit.FileSystem (pathByAppendingComponent has two C++ overloads that
 * call the same one).
 *
 * THREAD: any. This is the one table in the ABI with no thread confinement - WebKit reaches
 * it from AsyncFileStream (WorkQueue threads) and from WorkerThread as well as from the main
 * thread. Its Java targets must be thread-safe and its upcall stubs must come from a shared
 * arena.
 *
 * This table is also why the library needed a load hook. wtf/java/JavaEnv.cpp:139-149 resolved
 * com.sun.webkit.FileSystem eagerly, at load time, with a comment explaining that the
 * class loader which loaded jfxwebkit is reachable only there: from a WebKit-spawned thread
 * the lookup would have used the system loader and failed. A callback table has no class
 * lookup, so that constraint - and the load-time special case it forced - disappears.
 *
 * The table is defined in this header rather than beside FileSystemJava.cpp because
 * contract 12.1 gives each WKJHost sub-struct exactly one owning header, and the WTF slice
 * has none; defining it here is what stops that slice being blocked on this one.
 */
typedef struct WKJHostFileSystem {

    /* fwkFileExists(String) -> boolean. Default when NULL: 0. */
    int32_t (*file_exists)(const uint16_t* path, int32_t path_len);

    /*
     * fwkGetFileSize(String) -> long. A NEGATIVE result means "no size available" and is what
     * the caller tests; it is not an error code to be normalised. Default when NULL: -1.
     */
    int64_t (*get_file_size)(const uint16_t* path, int32_t path_len);

    /*
     * fwkGetFileMetadata(String, long[3]) -> boolean. On 1, out_metadata holds
     * { modification time in milliseconds since the epoch, length in bytes,
     * FileMetadata::Type as an integer } - the same three slots, in the same order, that the
     * long[] carried. Returns 0 and writes nothing otherwise. Default when NULL: 0.
     */
    int32_t (*get_file_metadata)(const uint16_t* path, int32_t path_len,
                                 int64_t out_metadata[3]);

    /*
     * fwkPathByAppendingComponent(String, String) -> String, contract-13 protocol.
     * Default when NULL: WKJ_STR_NULL.
     */
    int32_t (*path_by_appending_component)(const uint16_t* path, int32_t path_len,
                                           const uint16_t* component, int32_t component_len,
                                           uint16_t* out_buf, int32_t out_cap,
                                           int32_t* out_length);

    /* fwkMakeAllDirectories(String) -> boolean. Default when NULL: 0. */
    int32_t (*make_all_directories)(const uint16_t* path, int32_t path_len);

    /*
     * fwkOpenFile(String path, String mode) -> java.io.RandomAccessFile. The library only
     * ever passes "r"; the argument stays so the Java side is a plain forwarder and so a
     * future write mode needs no ABI change. The returned id IS WebCore's PlatformFileHandle
     * for this port, so 0 is invalidPlatformFileHandle. Returns a new id, or 0. Default: 0.
     */
    wkj_ref (*open_file)(const uint16_t* path, int32_t path_len,
                         const uint16_t* mode, int32_t mode_len);

    /* fwkCloseFile(RandomAccessFile). Default when NULL: no-op. */
    void (*close_file)(wkj_ref file);

    /*
     * fwkReadFromFile(RandomAccessFile, ByteBuffer) -> int. "data" points at "length" bytes
     * owned by the caller and valid for the duration of the call; Java wraps the address
     * without copying, exactly as NewDirectByteBuffer did, and must not retain it. Returns
     * the number of bytes read, or a negative value at end of stream or on error - the caller
     * normalises any negative to -1, as it always did. Default when NULL: -1.
     */
    int32_t (*read_from_file)(wkj_ref file, void* data, int32_t length);

    /*
     * fwkPathGetFileName(String) -> String, contract-13 protocol.
     * Default when NULL: WKJ_STR_NULL.
     */
    int32_t (*path_get_file_name)(const uint16_t* path, int32_t path_len,
                                  uint16_t* out_buf, int32_t out_cap, int32_t* out_length);

    /*
     * fwkSeekFile(RandomAccessFile, long offset). It returns nothing: the caller reports
     * failure by asking wkj_host->core.check_and_clear_exception() straight afterwards, which
     * is exactly where it consulted the JNI exception state and turned a pending one into -1.
     * Default when NULL: no-op.
     */
    void (*seek_file)(wkj_ref file, int64_t offset);

} WKJHostFileSystem;

/* ======================================================================================== */
/* Downcalls: com.sun.webkit.SharedBuffer                                                   */
/* ======================================================================================== */

/*
 * The peer these five share is a WebCore::SharedBufferBuilder* for create, append and dispose
 * and is read as a WebCore::FragmentedSharedBuffer* by size and get_some_data - the same
 * split the JNI functions had, because SharedBufferBuilder is not a FragmentedSharedBuffer
 * and the two casts are of different types. That is preserved rather than tidied.
 */

/* Java_com_sun_webkit_SharedBuffer_twkCreate. Returns a new SharedBufferBuilder peer. */
WKJ_EXPORT int64_t wkj_shared_buffer_create(void);

/* Java_com_sun_webkit_SharedBuffer_twkSize. */
WKJ_EXPORT int64_t wkj_shared_buffer_size(int64_t buffer);

/*
 * Java_com_sun_webkit_SharedBuffer_twkGetSomeData.
 *
 * Copies at most "length" bytes from "position" into dst + offset and returns the number
 * copied, or 0 when "position" is at or past the end. "offset" stays a separate parameter
 * rather than being folded into the pointer so that the Java facade can hand over the whole
 * array segment unchanged, exactly as it handed over the whole byte[].
 */
WKJ_EXPORT int32_t wkj_shared_buffer_get_some_data(int64_t buffer, int64_t position,
                                                   uint8_t* dst, int32_t offset,
                                                   int32_t length);

/* Java_com_sun_webkit_SharedBuffer_twkAppend. Reads "length" bytes from src + offset. */
WKJ_EXPORT void wkj_shared_buffer_append(int64_t buffer, const uint8_t* src,
                                         int32_t offset, int32_t length);

/*
 * Java_com_sun_webkit_SharedBuffer_twkDispose.
 *
 * DOES NOT FREE THE BUFFER, and that is not an oversight in this header. The JNI function
 * (SharedBufferJava.cpp:106-112) casts the pointer and then does nothing with it, so every
 * builder allocated by twkCreate has always leaked. Reproducing it is what keeps this
 * migration behaviour-neutral. Deleting the builder here is a one-line fix, but it is a fix:
 * it must land in its own commit, with evidence that the object really is unreachable by
 * then, because a double free is a far worse failure than a leak.
 */
WKJ_EXPORT void wkj_shared_buffer_dispose(int64_t buffer);

/* ======================================================================================== */
/* Downcalls: com.sun.webkit.Timer and com.sun.webkit.ContextMenu                            */
/* ======================================================================================== */

/*
 * Java_com_sun_webkit_Timer_twkFireTimerEvent. Runs MainThreadSharedTimer::fired(), i.e. the
 * whole WebKit timer queue, which can execute arbitrary script. Main thread only.
 */
WKJ_EXPORT void wkj_timer_fire(void);

/*
 * Java_com_sun_webkit_ContextMenu_twkHandleItemSelected. "controller" is the
 * WebCore::ContextMenuController* that WKJHostTheme.context_menu_show was given, and "action"
 * is the WebCore ContextMenuAction that context_menu_item_set_action carried out. Dispatches
 * the action, which can run script. Main thread only.
 */
WKJ_EXPORT void wkj_context_menu_item_selected(int64_t controller, int32_t action);

/* ======================================================================================== */
/* Downcalls: com.sun.webkit.WCPluginWidget                                                  */
/* ======================================================================================== */

/*
 * "plugin_widget" is the WebCore::PluginWidgetJava* that WKJHostTheme.plugin_widget_set_peer
 * stored in the Java object's pData field. The JNI versions read that field themselves with
 * GetLongField on the receiver; passing it explicitly is what removes the last cached field
 * id from this slice. A 0 peer is ignored, matching the "if (pThis)" guard on all three.
 */

/* Java_com_sun_webkit_WCPluginWidget_twkInvalidateWindowlessPluginRect. */
WKJ_EXPORT void wkj_plugin_widget_invalidate_rect(int64_t plugin_widget, int32_t x, int32_t y,
                                                  int32_t width, int32_t height);

/*
 * Java_com_sun_webkit_WCPluginWidget_twkSetPlugunFocused.
 *
 * The JNI name carries a typo that has been in the ABI since the file was written. The C ABI
 * spells it correctly because nothing binds the old spelling any more: the symbol name is
 * chosen here and matched on the Java side, so there is no compatibility to keep. The Java
 * method keeps its own name; only the symbol changes.
 */
WKJ_EXPORT void wkj_plugin_widget_set_focused(int64_t plugin_widget, int32_t focused);

/*
 * Java_com_sun_webkit_WCPluginWidget_twkConvertToPage.
 *
 * The JNI version took a WCRectangle, read its four float fields, converted, and returned a
 * NEWLY CONSTRUCTED WCRectangle - or null when the peer was 0. Here the caller provides both
 * buffers: in_xywh is read, out_xywh is written, and the return is 1 when out_xywh was
 * written and 0 when it was not, which is the null return. That removes an object argument,
 * four GetFloatField reads, a NewObject and five cached ids.
 *
 * Note that the rectangle goes through an int rect on the way in and back to float on the way
 * out, exactly as the JNI code did with its (int) casts and jdouble arguments; the truncation
 * is existing behaviour, not something this ABI introduces.
 */
WKJ_EXPORT int32_t wkj_plugin_widget_convert_to_page(int64_t plugin_widget,
                                                     const float in_xywh[4],
                                                     float out_xywh[4]);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* WEBKIT_JAVA_API_THEME_H */
