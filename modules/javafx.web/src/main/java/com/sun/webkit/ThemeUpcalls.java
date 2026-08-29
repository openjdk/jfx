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

import com.sun.webkit.graphics.WCImageFrame;
import com.sun.webkit.graphics.WCRectangle;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.net.IDN;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * The part of the {@code WKJHostTheme} group whose Java targets live in {@code com.sun.webkit}: the
 * two page-to-theme lookups, the cursor manager, the context menu, {@link WCWidget},
 * {@link WCPluginWidget}, the shared timer, {@link LocalizedStrings}, {@code java.net.IDN} and
 * {@link WCPasteboard}. The other seven slots of the group are in
 * {@code com.sun.webkit.graphics.RenderThemeUpcalls}, beside the theme classes themselves.
 * <p>
 * 35 of the 36 slots here are filled. <b>{@code theme.plugin_widget_paint} is deliberately left
 * NULL</b>, because it has no callable Java target: the C header records that its second argument
 * is a {@code WebCore::PlatformContextJava*} where {@code WCPluginWidget.paint} demands a
 * {@code com.sun.webkit.graphics.WCGraphicsContext}, declares the parameter as the {@code int64_t}
 * it really is so that the mistake is visible in the signature, and says outright that fixing it
 * means deciding what a plugin widget should be handed to draw with. A {@code long} cannot be
 * turned into a {@code WCGraphicsContext} here - it is a C++ pointer, not a {@code wkj_ref} - so
 * the honest thing is the documented default, a no-op, which is also what the JNI call achieved:
 * it passed an object that was not a {@code WCGraphicsContext} and could only fail.
 * <p>
 * <b>Shutdown.</b> {@code timer_set_fire_time} and {@code timer_stop} ran behind
 * {@code WC_GETJAVAENV_CHKRET}, which returned early on a null environment. There is no environment
 * to be null now, so the Java side must tolerate either arriving during teardown; both simply
 * forward to {@link Timer}, whose own state machine already does.
 * <p>
 * This class contains no restricted {@code java.lang.foreign} operation.
 */
final class ThemeUpcalls {

    private ThemeUpcalls() {
    }

    /**
     * Fills the part of the {@code theme} group whose targets are in this package.
     *
     * @param host the table
     */
    static void install(MemorySegment host) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        slot(host, lookup, "get_render_theme", "getRenderTheme",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));
        slot(host, lookup, "get_scroll_bar_theme", "getScrollBarTheme",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));

        slot(host, lookup, "cursor_get_predefined_id", "cursorGetPredefinedId",
                FunctionDescriptor.of(JAVA_LONG, JAVA_INT));
        slot(host, lookup, "cursor_get_custom_id", "cursorGetCustomId",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT));

        slot(host, lookup, "context_menu_create", "contextMenuCreate",
                FunctionDescriptor.of(JAVA_LONG));
        slot(host, lookup, "context_menu_item_create", "contextMenuItemCreate",
                FunctionDescriptor.of(JAVA_LONG));
        slot(host, lookup, "context_menu_item_set_type", "contextMenuItemSetType",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
        slot(host, lookup, "context_menu_item_set_action", "contextMenuItemSetAction",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
        slot(host, lookup, "context_menu_item_set_title", "contextMenuItemSetTitle",
                FunctionDescriptor.ofVoid(JAVA_LONG, ADDRESS, JAVA_INT));
        slot(host, lookup, "context_menu_item_set_submenu", "contextMenuItemSetSubmenu",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG));
        slot(host, lookup, "context_menu_item_set_checked", "contextMenuItemSetChecked",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
        slot(host, lookup, "context_menu_item_set_enabled", "contextMenuItemSetEnabled",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
        slot(host, lookup, "context_menu_append_item", "contextMenuAppendItem",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG));
        slot(host, lookup, "context_menu_show", "contextMenuShow",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT));

        slot(host, lookup, "widget_set_bounds", "widgetSetBounds",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
        slot(host, lookup, "widget_request_focus", "widgetRequestFocus",
                FunctionDescriptor.ofVoid(JAVA_LONG));
        slot(host, lookup, "widget_set_cursor", "widgetSetCursor",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG));
        slot(host, lookup, "widget_set_visible", "widgetSetVisible",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
        slot(host, lookup, "widget_destroy", "widgetDestroy",
                FunctionDescriptor.ofVoid(JAVA_LONG));
        slot(host, lookup, "widget_get_screen_depth", "widgetGetScreenDepth",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
        slot(host, lookup, "widget_get_screen_rect", "widgetGetScreenRect",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS));

        slot(host, lookup, "plugin_widget_create", "pluginWidgetCreate",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT,
                        ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT));
        slot(host, lookup, "plugin_widget_set_peer", "pluginWidgetSetPeer",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG));
        // theme.plugin_widget_paint stays NULL; see the class comment.
        slot(host, lookup, "plugin_widget_set_native_container_bounds",
                "pluginWidgetSetNativeContainerBounds",
                FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
        slot(host, lookup, "plugin_widget_handle_mouse_event", "pluginWidgetHandleMouseEvent",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT,
                        JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT,
                        JAVA_INT, JAVA_LONG));

        slot(host, lookup, "timer_set_fire_time", "timerSetFireTime",
                FunctionDescriptor.ofVoid(JAVA_DOUBLE));
        slot(host, lookup, "timer_stop", "timerStop", FunctionDescriptor.ofVoid());

        slot(host, lookup, "get_localized_property", "getLocalizedProperty",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
        slot(host, lookup, "idn_to_ascii", "idnToAscii",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT,
                        ADDRESS));

        slot(host, lookup, "pasteboard_get_plain_text", "pasteboardGetPlainText",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
        slot(host, lookup, "pasteboard_get_html", "pasteboardGetHtml",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
        slot(host, lookup, "pasteboard_write_plain_text", "pasteboardWritePlainText",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
        slot(host, lookup, "pasteboard_write_selection", "pasteboardWriteSelection",
                FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));
        slot(host, lookup, "pasteboard_write_url", "pasteboardWriteUrl",
                FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));
        slot(host, lookup, "pasteboard_write_image", "pasteboardWriteImage",
                FunctionDescriptor.ofVoid(JAVA_LONG));
    }

    private static void slot(MemorySegment host, MethodHandles.Lookup lookup, String name,
                             String method, FunctionDescriptor descriptor) {
        WebKitNative.installHostSlot(host, "theme." + name, lookup, method, descriptor);
    }

    // ------------------------------------------------------------------- page to theme

    /*
     * The RenderTheme of a page, as PG_GetRenderThemeObjectFromPage did with its two branches in one
     * function: a page id selects WebPage.getRenderTheme(), and 0 selects the static
     * WebPage.fwkGetDefaultRenderTheme(). Both branches are live - RenderThemeJava passes 0 for the
     * selection colours and for the radio button size. Default when NULL: 0.
     */
    private static long getRenderTheme(long webPage) {
        try {
            if (webPage == 0L) {
                return WebKitNative.register(WebPage.fwkGetDefaultRenderTheme());
            }
            return WebKitNative.lookup(webPage) instanceof WebPage page
                    ? WebKitNative.register(page.getRenderTheme())
                    : 0L;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.get_render_theme", t);
            return 0L;
        }
    }

    /*
     * WebPage.getScrollBarTheme(). Unlike get_render_theme there is no static fallback:
     * ScrollbarThemeJava returns an empty rect or skips painting when the scrollbar has been
     * detached or its page is a utility page, and that test happens in C++ before this slot is
     * reached. Default when NULL: 0.
     */
    private static long getScrollBarTheme(long webPage) {
        try {
            return WebKitNative.lookup(webPage) instanceof WebPage page
                    ? WebKitNative.register(page.getScrollBarTheme())
                    : 0L;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.get_scroll_bar_theme", t);
            return 0L;
        }
    }

    // ------------------------------------------------------------------- CursorManager

    /*
     * CursorManager.getPredefinedCursorID(int) -> long, an opaque platform cursor id that WebCore
     * stores as PlatformCursor and hands back to widget_set_cursor. The manager is a Java singleton,
     * so there is no target ref; the JNI code produced a null cursor when the singleton was absent,
     * which is what 0 means. Default when NULL: 0.
     */
    private static long cursorGetPredefinedId(int type) {
        try {
            CursorManager<?> manager = CursorManager.getCursorManager();
            return manager == null ? 0L : manager.getPredefinedCursorID(type);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.cursor_get_predefined_id", t);
            return 0L;
        }
    }

    /* CursorManager.getCustomCursorID(WCImageFrame, int x, int y) -> long. Default when NULL: 0. */
    private static long cursorGetCustomId(long imageFrame, int hotspotX, int hotspotY) {
        try {
            CursorManager<?> manager = CursorManager.getCursorManager();
            if (manager == null
                    || !(WebKitNative.lookup(imageFrame) instanceof WCImageFrame frame)) {
                return 0L;
            }
            return manager.getCustomCursorID(frame, hotspotX, hotspotY);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.cursor_get_custom_id", t);
            return 0L;
        }
    }

    // --------------------------------------------------- ContextMenu and ContextMenuItem

    private static ContextMenuItem item(long item) {
        return WebKitNative.lookup(item) instanceof ContextMenuItem target ? target : null;
    }

    /* ContextMenu.fwkCreateContextMenu() - static. Default when NULL: 0. */
    private static long contextMenuCreate() {
        try {
            return WebKitNative.register(ContextMenu.fwkCreateContextMenu());
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.context_menu_create", t);
            return 0L;
        }
    }

    /* ContextMenuItem.fwkCreateContextMenuItem() - static. Default when NULL: 0. */
    private static long contextMenuItemCreate() {
        try {
            return WebKitNative.register(ContextMenuItem.fwkCreateContextMenuItem());
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.context_menu_item_create", t);
            return 0L;
        }
    }

    /* fwkSetType(int). Default when NULL: no-op. */
    private static void contextMenuItemSetType(long menuItem, int type) {
        try {
            ContextMenuItem target = item(menuItem);
            if (target != null) {
                target.fwkSetType(type);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.context_menu_item_set_type", t);
        }
    }

    /* fwkSetAction(int). Default when NULL: no-op. */
    private static void contextMenuItemSetAction(long menuItem, int action) {
        try {
            ContextMenuItem target = item(menuItem);
            if (target != null) {
                target.fwkSetAction(action);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.context_menu_item_set_action", t);
        }
    }

    /*
     * fwkSetTitle(String). The library passes NULL for an EMPTY title, not the empty string - the
     * JNI code wrote "title.isEmpty() ? NULL : title.toJavaString(env)" - and that distinction is
     * carried through here rather than normalised. Default when NULL: no-op.
     */
    private static void contextMenuItemSetTitle(long menuItem, MemorySegment title,
                                                int titleLength) {
        try {
            ContextMenuItem target = item(menuItem);
            if (target != null) {
                target.fwkSetTitle(WebKitNative.readString(title, titleLength));
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.context_menu_item_set_title", t);
        }
    }

    /* fwkSetSubmenu(ContextMenu); the submenu may be 0. Default when NULL: no-op. */
    private static void contextMenuItemSetSubmenu(long menuItem, long submenu) {
        try {
            ContextMenuItem target = item(menuItem);
            if (target != null) {
                target.fwkSetSubmenu(WebKitNative.lookup(submenu) instanceof ContextMenu menu
                        ? menu : null);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.context_menu_item_set_submenu", t);
        }
    }

    /* fwkSetChecked(boolean). Default when NULL: no-op. */
    private static void contextMenuItemSetChecked(long menuItem, int checked) {
        try {
            ContextMenuItem target = item(menuItem);
            if (target != null) {
                target.fwkSetChecked(checked != 0);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.context_menu_item_set_checked", t);
        }
    }

    /* fwkSetEnabled(boolean). Default when NULL: no-op. */
    private static void contextMenuItemSetEnabled(long menuItem, int enabled) {
        try {
            ContextMenuItem target = item(menuItem);
            if (target != null) {
                target.fwkSetEnabled(enabled != 0);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.context_menu_item_set_enabled", t);
        }
    }

    /* ContextMenu.fwkAppendItem(ContextMenuItem). Default when NULL: no-op. */
    private static void contextMenuAppendItem(long menu, long menuItem) {
        try {
            ContextMenuItem target = item(menuItem);
            if (WebKitNative.lookup(menu) instanceof ContextMenu contextMenu && target != null) {
                contextMenu.fwkAppendItem(target);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.context_menu_append_item", t);
        }
    }

    /*
     * ContextMenu.fwkShow(WebPage, long menuCtrlPData, int x, int y). "controller" is the
     * WebCore::ContextMenuController* that wkj_context_menu_item_selected carries back when the user
     * picks an item; the menu outlives this call, so the Java side keeps it.
     * Default when NULL: no-op.
     */
    private static void contextMenuShow(long menu, long webPage, long controller, int x, int y) {
        try {
            if (WebKitNative.lookup(menu) instanceof ContextMenu contextMenu) {
                WebPage page = WebKitNative.lookup(webPage) instanceof WebPage p ? p : null;
                contextMenu.fwkShow(page, controller, x, y);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.context_menu_show", t);
        }
    }

    // ------------------------------------------------------------------------ WCWidget

    private static WCWidget widget(long widget) {
        return WebKitNative.lookup(widget) instanceof WCWidget target ? target : null;
    }

    /* fwkSetBounds(int, int, int, int). Default when NULL: no-op. */
    private static void widgetSetBounds(long widget, int x, int y, int width, int height) {
        try {
            WCWidget target = widget(widget);
            if (target != null) {
                target.fwkSetBounds(x, y, width, height);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.widget_set_bounds", t);
        }
    }

    /* fwkRequestFocus(). Default when NULL: no-op. */
    private static void widgetRequestFocus(long widget) {
        try {
            WCWidget target = widget(widget);
            if (target != null) {
                target.fwkRequestFocus();
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.widget_request_focus", t);
        }
    }

    /*
     * fwkSetCursor(long). The id is what cursor_get_predefined_id or cursor_get_custom_id returned,
     * that is WebCore::PlatformCursor. Default when NULL: no-op.
     */
    private static void widgetSetCursor(long widget, long cursorId) {
        try {
            WCWidget target = widget(widget);
            if (target != null) {
                target.fwkSetCursor(cursorId);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.widget_set_cursor", t);
        }
    }

    /* fwkSetVisible(boolean). Default when NULL: no-op. */
    private static void widgetSetVisible(long widget, int visible) {
        try {
            WCWidget target = widget(widget);
            if (target != null) {
                target.fwkSetVisible(visible != 0);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.widget_set_visible", t);
        }
    }

    /* fwkDestroy(). Called from ~Widget. Default when NULL: no-op. */
    private static void widgetDestroy(long widget) {
        try {
            WCWidget target = widget(widget);
            if (target != null) {
                target.fwkDestroy();
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.widget_destroy", t);
        }
    }

    /*
     * fwkGetScreenDepth(). Default when NULL: 0, and the caller substitutes 24 as it always has for
     * a missing page client.
     */
    private static int widgetGetScreenDepth(long widget) {
        try {
            WCWidget target = widget(widget);
            return target == null ? 0 : target.fwkGetScreenDepth();
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.widget_get_screen_depth", t);
            return 0;
        }
    }

    /*
     * fwkGetScreenRect(boolean) -> WCRectangle, formerly four GetFloatField reads on the returned
     * object. Returns 0 and writes nothing when Java returned null, which the caller turns into an
     * empty rect exactly as before. Default when NULL: 0.
     */
    private static int widgetGetScreenRect(long widget, int available, MemorySegment out) {
        try {
            WCWidget target = widget(widget);
            WCRectangle rect = target == null ? null : target.fwkGetScreenRect(available != 0);
            if (rect == null) {
                return 0;
            }
            float[] xywh = { rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight() };
            return WebKitNative.writeFloats(out, xywh, 4) ? 1 : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.widget_get_screen_rect", t);
            return 0;
        }
    }

    // ------------------------------------------------------------------ WCPluginWidget

    private static WCPluginWidget pluginWidget(long widget) {
        return WebKitNative.lookup(widget) instanceof WCPluginWidget target ? target : null;
    }

    /*
     * WCPluginWidget.create(WebPage, int w, int h, String url, String mimeType, String[] paramNames,
     * String[] paramValues) -> WCPluginWidget, a static factory. The two String[] arguments cross as
     * parallel arrays of pointers and lengths; a count of 0 means both are empty, which is what
     * strVect2JArray produced for an empty Vector, and the four pointers may be NULL in that case.
     *
     * The JNI code passed width and height as 0 unconditionally - PluginWidgetJava declared both
     * locals and never assigned them - and that is preserved by passing them through unchanged.
     * Default when NULL: 0.
     */
    private static long pluginWidgetCreate(long webPage, int width, int height, MemorySegment url,
                                           int urlLength, MemorySegment mimeType,
                                           int mimeTypeLength, MemorySegment paramNames,
                                           MemorySegment paramNameLengths, MemorySegment paramValues,
                                           MemorySegment paramValueLengths, int paramCount) {
        try {
            if (!(WebKitNative.lookup(webPage) instanceof WebPage page)) {
                return 0L;
            }
            String[] names = WebKitNative.readStringArray(paramNames, paramNameLengths, paramCount);
            String[] values =
                    WebKitNative.readStringArray(paramValues, paramValueLengths, paramCount);
            if (names == null || values == null) {
                return 0L;
            }
            return WebKitNative.register(WCPluginWidget.create(page, width, height,
                    WebKitNative.readString(url, urlLength),
                    WebKitNative.readString(mimeType, mimeTypeLength), names, values));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.plugin_widget_create", t);
            return 0L;
        }
    }

    /*
     * Stores the WebCore::PluginWidgetJava* in the widget, replacing the SetLongField the C++
     * constructor did. Default when NULL: no-op - in which case Java can never call back, which is
     * the same outcome the JNI code had if the field id had failed to resolve.
     */
    private static void pluginWidgetSetPeer(long pluginWidget, long peer) {
        try {
            WCPluginWidget target = pluginWidget(pluginWidget);
            if (target != null) {
                target.setPeer(peer);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.plugin_widget_set_peer", t);
        }
    }

    /* fwkSetNativeContainerBounds(int, int, int, int). Default when NULL: no-op. */
    private static void pluginWidgetSetNativeContainerBounds(long pluginWidget, int x, int y,
                                                             int width, int height) {
        try {
            WCPluginWidget target = pluginWidget(pluginWidget);
            if (target != null) {
                target.fwkSetNativeContainerBounds(x, y, width, height);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.plugin_widget_set_native_container_bounds", t);
        }
    }

    /*
     * fwkHandleMouseEvent(...) -> boolean, where 1 cancels bubbling. The argument order is the Java
     * one. Default when NULL: 0.
     */
    private static int pluginWidgetHandleMouseEvent(long pluginWidget, MemorySegment type,
                                                    int typeLength, int x, int y, int screenX,
                                                    int screenY, int button, int buttonDown,
                                                    int alt, int meta, int ctrl, int shift,
                                                    long timestamp) {
        try {
            WCPluginWidget target = pluginWidget(pluginWidget);
            if (target == null) {
                return 0;
            }
            return target.fwkHandleMouseEvent(WebKitNative.readString(type, typeLength), x, y,
                    screenX, screenY, button, buttonDown != 0, alt != 0, meta != 0, ctrl != 0,
                    shift != 0, timestamp) ? 1 : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.plugin_widget_handle_mouse_event", t);
            return 0;
        }
    }

    // --------------------------------------------------------------------------- Timer

    /* Timer.fwkSetFireTime(double), seconds since the POSIX epoch. Default when NULL: no-op. */
    private static void timerSetFireTime(double fireTime) {
        try {
            Timer.fwkSetFireTime(fireTime);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.timer_set_fire_time", t);
        }
    }

    /* Timer.fwkStopTimer(). Default when NULL: no-op. */
    private static void timerStop() {
        try {
            Timer.fwkStopTimer();
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.timer_stop", t);
        }
    }

    // ------------------------------------------------------- LocalizedStrings and IDN

    /*
     * LocalizedStrings.getLocalizedProperty(String) -> String. WKJ_STR_NULL means the property is
     * unknown, and the caller then returns the name it asked for; that fallback is in the C++ and
     * stays there. Default when NULL: WKJ_STR_NULL.
     */
    private static int getLocalizedProperty(MemorySegment name, int nameLength, MemorySegment out,
                                            int capacity, MemorySegment length) {
        try {
            String property = WebKitNative.readString(name, nameLength);
            return WebKitNative.emitString(
                    property == null ? null : LocalizedStrings.getLocalizedProperty(property),
                    out, capacity, length);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.get_localized_property", t);
            WebKitNative.writeInt(length, 0);
            return WKJStringCodec.NULL;
        }
    }

    /*
     * java.net.IDN.toASCII(String, int flags) -> String. The flags value is
     * URLLoaderBase.ALLOW_UNASSIGNED, the only value the library passes; it is passed rather than
     * assumed so that this stays a plain forwarder.
     *
     * java.net.IDN is IDNA2003 and ICU's uidna_nameToASCII defaults to UTS-46, so the two are not
     * equivalent and swapping this upcall for the ICU call would be a behaviour change that needs
     * the IDNA test vectors run against both first. Until then, the Java call.
     * Default when NULL: WKJ_STR_NULL, which the caller turns into the empty string.
     */
    private static int idnToAscii(MemorySegment hostname, int hostnameLength, int flags,
                                  MemorySegment out, int capacity, MemorySegment length) {
        try {
            String name = WebKitNative.readString(hostname, hostnameLength);
            return WebKitNative.emitString(name == null ? null : IDN.toASCII(name, flags), out,
                    capacity, length);
        } catch (Throwable t) {
            // IllegalArgumentException from a hostname that is not valid IDNA reaches here, which is
            // the same "no result" the JNI code produced when the call threw and it cleared the
            // exception.
            WebKitNative.upcallFailed("theme.idn_to_ascii", t);
            WebKitNative.writeInt(length, 0);
            return WKJStringCodec.NULL;
        }
    }

    // -------------------------------------------------------------------- WCPasteboard

    /*
     * WCPasteboard.getPlainText() -> String. WKJ_STR_NULL is distinguished from WKJ_STR_OK with
     * length 0 and both reach the caller, which stores the null String and the empty String
     * respectively - the JNI code made the same distinction with "jstr ? String(env, jstr) :
     * String()". Default when NULL: WKJ_STR_NULL.
     */
    private static int pasteboardGetPlainText(MemorySegment out, int capacity,
                                              MemorySegment length) {
        try {
            return WebKitNative.emitString(WCPasteboard.getPlainText(), out, capacity, length);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.pasteboard_get_plain_text", t);
            WebKitNative.writeInt(length, 0);
            return WKJStringCodec.NULL;
        }
    }

    /* WCPasteboard.getHtml() -> String. Default when NULL: WKJ_STR_NULL. */
    private static int pasteboardGetHtml(MemorySegment out, int capacity, MemorySegment length) {
        try {
            return WebKitNative.emitString(WCPasteboard.getHtml(), out, capacity, length);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.pasteboard_get_html", t);
            WebKitNative.writeInt(length, 0);
            return WKJStringCodec.NULL;
        }
    }

    /* WCPasteboard.writePlainText(String). Default when NULL: no-op. */
    private static void pasteboardWritePlainText(MemorySegment text, int textLength) {
        try {
            WCPasteboard.writePlainText(WebKitNative.readString(text, textLength));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.pasteboard_write_plain_text", t);
        }
    }

    /*
     * WCPasteboard.writeSelection(boolean, String text, String markup). Two live strings in one
     * call, each with its own pointer and length pair - which is the whole reason this ABI passes
     * strings flat rather than through any shared buffer. Default when NULL: no-op.
     */
    private static void pasteboardWriteSelection(int canSmartCopyOrDelete, MemorySegment text,
                                                 int textLength, MemorySegment markup,
                                                 int markupLength) {
        try {
            WCPasteboard.writeSelection(canSmartCopyOrDelete != 0,
                    WebKitNative.readString(text, textLength),
                    WebKitNative.readString(markup, markupLength));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.pasteboard_write_selection", t);
        }
    }

    /* WCPasteboard.writeUrl(String url, String markup). Default when NULL: no-op. */
    private static void pasteboardWriteUrl(MemorySegment url, int urlLength, MemorySegment markup,
                                           int markupLength) {
        try {
            WCPasteboard.writeUrl(WebKitNative.readString(url, urlLength),
                    WebKitNative.readString(markup, markupLength));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.pasteboard_write_url", t);
        }
    }

    /* WCPasteboard.writeImage(WCImageFrame). Default when NULL: no-op. */
    private static void pasteboardWriteImage(long imageFrame) {
        try {
            if (WebKitNative.lookup(imageFrame) instanceof WCImageFrame frame) {
                WCPasteboard.writeImage(frame);
            }
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.pasteboard_write_image", t);
        }
    }
}
