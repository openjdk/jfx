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

import com.sun.webkit.graphics.WCRectangle;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * FFM facade for the {@code wkj_plugin_widget_*} functions of the {@code jfxwebkit} C ABI, used by
 * {@code WCPluginWidget}.
 * <p>
 * All three take the {@code WebCore::PluginWidgetJava*} explicitly. The JNI versions read it off the
 * receiver themselves with {@code GetLongField}, which is the last cached field id this slice had;
 * the {@code plugin_widget_} section of {@code WKJHostTheme} now sets the field from Java and the
 * peer travels as an ordinary argument. A zero peer is ignored by the library, reproducing the
 * {@code if (pThis)} guard all three carried.
 * <p>
 * {@code WCPluginWidget.initIDs} is gone rather than migrated: its C body cached the method and
 * field ids that the host table replaces, so there is nothing left for it to do.
 *
 * @see com.sun.webkit.WebKitNative
 */
final class WCPluginWidgetNative {

    private static final MethodHandle INVALIDATE_RECT = WebKitNative.downcall(
            "wkj_plugin_widget_invalidate_rect",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
    private static final MethodHandle SET_FOCUSED = WebKitNative.downcall(
            "wkj_plugin_widget_set_focused",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));
    private static final MethodHandle CONVERT_TO_PAGE = WebKitNative.downcall(
            "wkj_plugin_widget_convert_to_page",
            FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, ADDRESS));

    /** A rectangle crosses the ABI as four floats, {@code x, y, width, height}. */
    private static final int RECT_FLOATS = 4;

    private WCPluginWidgetNative() {
    }

    static void invalidateRect(long pluginWidget, int x, int y, int width, int height) {
        try {
            INVALIDATE_RECT.invokeExact(pluginWidget, x, y, width, height);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    static void setFocused(long pluginWidget, boolean focused) {
        try {
            SET_FOCUSED.invokeExact(pluginWidget, focused ? 1 : 0);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * Converts widget coordinates to page coordinates. The caller provides both rectangles, so the
     * {@code WCRectangle} argument, its four {@code GetFloatField} reads and the {@code NewObject}
     * that built the result have all left the ABI.
     * <p>
     * The rectangle goes through an integer rectangle inside the library and comes back as floats,
     * exactly as the JNI code did with its {@code (int)} casts; the truncation is existing
     * behaviour, not something introduced here.
     *
     * @param pluginWidget the plugin widget handle
     * @param rc the rectangle in widget coordinates
     * @return the rectangle in page coordinates, or {@code null} when the peer named no widget
     */
    static WCRectangle convertToPage(long pluginWidget, WCRectangle rc) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment in = arena.allocateFrom(JAVA_FLOAT,
                    rc.getX(), rc.getY(), rc.getWidth(), rc.getHeight());
            MemorySegment out = arena.allocate(JAVA_FLOAT, RECT_FLOATS);
            int written;
            try {
                written = (int) CONVERT_TO_PAGE.invokeExact(pluginWidget, in, out);
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
            if (written == 0) {
                return null;
            }
            float[] xywh = new float[RECT_FLOATS];
            MemorySegment.copy(out, JAVA_FLOAT, 0L, xywh, 0, RECT_FLOATS);
            return new WCRectangle(xywh[0], xywh[1], xywh[2], xywh[3]);
        }
    }
}
