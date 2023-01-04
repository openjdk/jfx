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

package javafx.application;

import javafx.scene.paint.Color;
import java.util.Map;

/**
 * Contains UI preferences of the current platform.
 * <p>
 * {@code PlatformPreferences} implements {@link Map} to expose platform preferences as key-value pairs.
 * For convenience, {@link #getString}, {@link #getBoolean} and {@link #getColor} are provided as typed
 * alternatives to the untyped {@link #get} method.
 * <p>
 * The preferences that are reported by the platform may be dependent on the operating system version.
 * Applications should always test whether a preference is available, or use the {@link #getString(String, String)},
 * {@link #getBoolean(String, boolean)} or {@link #getColor(String, Color)} overloads that accept a fallback
 * value if the preference is not available.
 * <p>
 * The following list contains all preferences that are potentially available on the specified platforms:
 *
 * <table>
 *     <caption></caption>
 *     <tbody>
 *         <tr><th colspan="2" scope="colgroup">Windows</th></tr>
 *         <tr><td>{@code Windows.SPI.HighContrast}</td><td>{@link Boolean}</td></tr>
 *         <tr><td>{@code Windows.SPI.HighContrastColorScheme}</td><td>{@link String}</td></tr>
 *         <tr><td>{@code Windows.SysColor.COLOR_3DFACE}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Windows.SysColor.COLOR_BTNTEXT}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Windows.SysColor.COLOR_GRAYTEXT}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Windows.SysColor.COLOR_HIGHLIGHT}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Windows.SysColor.COLOR_HIGHLIGHTTEXT}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Windows.SysColor.COLOR_HOTLIGHT}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Windows.SysColor.COLOR_WINDOW}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Windows.SysColor.COLOR_WINDOWTEXT}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Windows.UIColor.Background}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Windows.UIColor.Foreground}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Windows.UIColor.AccentDark3}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Windows.UIColor.AccentDark2}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Windows.UIColor.AccentDark1}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Windows.UIColor.Accent}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Windows.UIColor.AccentLight1}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Windows.UIColor.AccentLight2}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Windows.UIColor.AccentLight3}</td><td>{@link Color}</td></tr>
 *         <tr></tr>
 *
 *         <tr><th colspan="2" scope="colgroup">macOS</th></tr>
 *         <tr><td>{@code macOS.NSColor.labelColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.secondaryLabelColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.tertiaryLabelColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.quaternaryLabelColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.textColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.placeholderTextColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.selectedTextColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.textBackgroundColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.selectedTextBackgroundColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.keyboardFocusIndicatorColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.unemphasizedSelectedTextColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.unemphasizedSelectedTextBackgroundColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.linkColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.separatorColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.selectedContentBackgroundColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.unemphasizedSelectedContentBackgroundColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.selectedMenuItemTextColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.gridColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.headerTextColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.alternatingContentBackgroundColors}</td><td>{@link Color}{@code []}</td></tr>
 *         <tr><td>{@code macOS.NSColor.controlAccentColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.controlColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.controlBackgroundColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.controlTextColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.disabledControlTextColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.selectedControlColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.selectedControlTextColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.alternateSelectedControlTextColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.currentControlTint}</td><td>{@link String}</td></tr>
 *         <tr><td>{@code macOS.NSColor.windowBackgroundColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.windowFrameTextColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.underPageBackgroundColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.findHighlightColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.highlightColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.shadowColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.systemBlueColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.systemBrownColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.systemGrayColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.systemGreenColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.systemIndigoColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.systemOrangeColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.systemPinkColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.systemPurpleColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.systemRedColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.systemTealColor}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code macOS.NSColor.systemYellowColor}</td><td>{@link Color}</td></tr>
 *         <tr></tr>
 *
 *         <tr><th colspan="2" scope="colgroup">Linux</th></tr>
 *         <tr><td>{@code Linux.GTK.ThemeName}</td><td>{@link String}</td></tr>
 *         <tr><td>{@code Linux.GTK.Colors.theme_fg_color}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Linux.GTK.Colors.theme_bg_color}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Linux.GTK.Colors.theme_base_color}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Linux.GTK.Colors.theme_selected_bg_color}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Linux.GTK.Colors.theme_selected_fg_color}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Linux.GTK.Colors.insensitive_bg_color}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Linux.GTK.Colors.insensitive_fg_color}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Linux.GTK.Colors.insensitive_base_color}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Linux.GTK.Colors.theme_unfocused_fg_color}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Linux.GTK.Colors.theme_unfocused_bg_color}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Linux.GTK.Colors.theme_unfocused_base_color}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Linux.GTK.Colors.theme_unfocused_selected_bg_color}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Linux.GTK.Colors.theme_unfocused_selected_fg_color}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Linux.GTK.Colors.borders}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Linux.GTK.Colors.unfocused_borders}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Linux.GTK.Colors.warning_color}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Linux.GTK.Colors.error_color}</td><td>{@link Color}</td></tr>
 *         <tr><td>{@code Linux.GTK.Colors.success_color}</td><td>{@link Color}</td></tr>
 *         <tr></tr>
 *     </tbody>
 * </table>
 *
 * @since 21
 */
public interface PlatformPreferences extends Map<String, Object> {

    /**
     * Returns the {@link String} instance to which the specified key is mapped.
     *
     * @param key the key
     * @return the {@code String} instance to which the {@code key} is mapped, or
     *         {@code null} if the key is not mapped to a {@code String} instance
     */
    String getString(String key);

    /**
     * Returns the {@link String} instance to which the specified key is mapped,
     * or a fallback value if the key is not mapped to a {@code String} instance.
     *
     * @param key the key
     * @return the {@code String} instance to which the {@code key} is mapped, or
     *         {@code fallbackValue} if the key is not mapped to a {@code String}
     *         instance
     */
    default String getString(String key, String fallbackValue) {
        String value = getString(key);
        return value != null ? value : fallbackValue;
    }

    /**
     * Returns the {@link Boolean} instance to which the specified key is mapped.
     *
     * @param key the key
     * @return the {@code Boolean} instance to which the {@code key} is mapped, or
     *         {@code null} if the key is not mapped to a {@code Boolean} instance
     */
    Boolean getBoolean(String key);

    /**
     * Returns the {@code boolean} value to which the specified key is mapped,
     * or a fallback value if the key is not mapped to a {@code boolean} value.
     *
     * @param key the key
     * @return the {@code boolean} value to which the {@code key} is mapped, or
     *         {@code fallbackValue} if the key is not mapped to a {@code boolean}
     *         value
     */
    default boolean getBoolean(String key, boolean fallbackValue) {
        Boolean value = getBoolean(key);
        return value != null ? value : fallbackValue;
    }

    /**
     * Returns the {@link Color} instance to which the specified key is mapped.
     *
     * @param key the key
     * @return the {@code Color} instance to which the {@code key} is mapped, or
     *         {@code null} if the key is not mapped to a {@code Color} instance
     */
    Color getColor(String key);

    /**
     * Returns the {@link Color} instance to which the specified key is mapped,
     * or a fallback value if the key is not mapped to a {@code Color} instance.
     *
     * @param key the key
     * @return the {@code Color} instance to which the {@code key} is mapped, or
     *         {@code fallbackValue} if the key is not mapped to a {@code Color}
     *         instance
     */
    default Color getColor(String key, Color fallbackValue) {
        Color value = getColor(key);
        return value != null ? value : fallbackValue;
    }

    /**
     * Adds the specified listener to this {@code PlatformPreferences} instance.
     *
     * @param listener the {@code PlatformPreferencesListener}
     */
    void addListener(PlatformPreferencesListener listener);

    /**
     * Removes the specified listener from this {@code PlatformPreferences} instance.
     *
     * @param listener the {@code PlatformPreferencesListener}
     */
    void removeListener(PlatformPreferencesListener listener);

}
