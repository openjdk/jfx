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

package com.sun.webkit.graphics;

import com.sun.webkit.WebKitNative;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * The seven {@code WKJHostTheme} slots whose Java targets live in this package: the widget theme,
 * the scrollbar theme and the media control slider.
 * <p>
 * The rest of the {@code theme} group is in {@code com.sun.webkit.ThemeUpcalls}. The split is not
 * cosmetic: {@link RenderTheme#createWidget}, {@link RenderTheme#getRadioButtonSize},
 * {@link RenderTheme#getSelectionColor}, {@link ScrollBarTheme#createWidget} and
 * {@link ScrollBarTheme#getScrollBarPartRect} are {@code protected}, and
 * {@code RenderMediaControls.fwkGetSliderThumbSize} is package private, so an upcall target for
 * them has to be here. The two slots that fetch a theme from a page are on the other side, with
 * {@code WebPage}.
 * <p>
 * <b>Threading.</b> The WebKit main thread throughout; {@code RenderThemeJava} and
 * {@code ScrollbarThemeJava} are called from layout and paint.
 * <p>
 * This class contains no restricted {@code java.lang.foreign} operation.
 */
public final class RenderThemeUpcalls {

    private RenderThemeUpcalls() {
    }

    /**
     * Fills the part of the {@code theme} group whose targets are in this package.
     *
     * @param host the table
     */
    public static void install(MemorySegment host) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        WebKitNative.installHostSlot(host, "theme.create_widget", lookup, "createWidget",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT,
                        JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT));
        WebKitNative.installHostSlot(host, "theme.get_radio_button_size", lookup,
                "getRadioButtonSize", FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
        WebKitNative.installHostSlot(host, "theme.get_selection_color", lookup, "getSelectionColor",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT));
        WebKitNative.installHostSlot(host, "theme.get_slider_thumb_size", lookup,
                "getSliderThumbSize", FunctionDescriptor.of(JAVA_INT, JAVA_INT));
        WebKitNative.installHostSlot(host, "theme.scroll_bar_create_widget", lookup,
                "scrollBarCreateWidget", FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_LONG,
                        JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
        WebKitNative.installHostSlot(host, "theme.scroll_bar_get_part_rect", lookup,
                "scrollBarGetPartRect",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_INT, ADDRESS));
        WebKitNative.installHostSlot(host, "theme.scroll_bar_get_thickness", lookup,
                "scrollBarGetThickness", FunctionDescriptor.of(JAVA_INT));
    }

    /*
     * RenderTheme.createWidget(long id, int widgetIndex, int state, int w, int h, int bgColor,
     * ByteBuffer extParams) -> Ref. "ext_params" is caller-owned native scratch valid only for the
     * duration of the call, wrapped rather than copied exactly as NewDirectByteBuffer did; it is
     * NULL with length 0 for every widget except SLIDER, PROGRESS_BAR and METER, which is the null
     * ByteBuffer the JNI code passed for an empty extParams vector.
     *
     * A 0 return is meaningful rather than an error: RenderThemeJava reads it as "the Java theme
     * declines this widget" and falls back to WebKit's own rendering. Default when NULL: 0.
     */
    private static long createWidget(long theme, long id, int widgetIndex, int state, int width,
                                     int height, int backgroundColor, MemorySegment extParams,
                                     int extParamsLength) {
        try {
            if (!(WebKitNative.lookup(theme) instanceof RenderTheme target)) {
                return 0L;
            }
            ByteBuffer parameters = extParams.address() == 0L || extParamsLength <= 0
                    ? null
                    : WebKitNative.resize(extParams, extParamsLength).asByteBuffer();
            return WebKitNative.register(target.createWidget(id, widgetIndex, state, width, height,
                    backgroundColor, parameters));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.create_widget", t);
            return 0L;
        }
    }

    /* RenderTheme.getRadioButtonSize(). Default when NULL: 0. */
    private static int getRadioButtonSize(long theme) {
        try {
            return WebKitNative.lookup(theme) instanceof RenderTheme target
                    ? target.getRadioButtonSize()
                    : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.get_radio_button_size", t);
            return 0;
        }
    }

    /*
     * RenderTheme.getSelectionColor(int), packed 0xAARRGGBB. Default when NULL: 0, transparent
     * black - which is what the JNI code produced when the call failed, because it did not test.
     */
    private static int getSelectionColor(long theme, int index) {
        try {
            return WebKitNative.lookup(theme) instanceof RenderTheme target
                    ? target.getSelectionColor(index)
                    : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.get_selection_color", t);
            return 0;
        }
    }

    /*
     * RenderMediaControls.fwkGetSliderThumbSize(int) -> int, width in the high 16 bits and height in
     * the low 16. The packing is kept rather than split into two out parameters because it is what
     * the Java method returns and unpacking it is one line on the C++ side, exactly as today.
     * Default when NULL: 0, a zero sized thumb.
     */
    private static int getSliderThumbSize(int sliderType) {
        try {
            return RenderMediaControls.fwkGetSliderThumbSize(sliderType);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.get_slider_thumb_size", t);
            return 0;
        }
    }

    /*
     * ScrollBarTheme.createWidget(long id, int w, int h, int orientation, int value,
     * int visibleSize, int totalSize) -> Ref. "id" is the Scrollbar*. Default when NULL: 0.
     */
    private static long scrollBarCreateWidget(long theme, long id, int width, int height,
                                              int orientation, int value, int visibleSize,
                                              int totalSize) {
        try {
            if (!(WebKitNative.lookup(theme) instanceof ScrollBarTheme target)) {
                return 0L;
            }
            return WebKitNative.register(target.createWidget(id, width, height, orientation, value,
                    visibleSize, totalSize));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.scroll_bar_create_widget", t);
            return 0L;
        }
    }

    /*
     * ScrollBarTheme.getScrollBarPartRect(long id, int part, int[4]), formerly an int[] the C++
     * allocated with NewIntArray and read back with GetPrimitiveArrayCritical. Writes x, y, width
     * and height and returns 1. Default when NULL: 0.
     */
    private static int scrollBarGetPartRect(long theme, long id, int part, MemorySegment out) {
        try {
            if (!(WebKitNative.lookup(theme) instanceof ScrollBarTheme target)) {
                return 0;
            }
            int[] rect = new int[4];
            target.getScrollBarPartRect(id, part, rect);
            return WebKitNative.writeInts(out, rect, 4) ? 1 : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.scroll_bar_get_part_rect", t);
            return 0;
        }
    }

    /*
     * ScrollBarTheme.getThickness() - a static, so no target ref. The Java implementation already
     * substitutes 12 for an unset thickness, so the library never had to. Default when NULL: 0.
     */
    private static int scrollBarGetThickness() {
        try {
            return ScrollBarTheme.getThickness();
        } catch (Throwable t) {
            WebKitNative.upcallFailed("theme.scroll_bar_get_thickness", t);
            return 0;
        }
    }
}
