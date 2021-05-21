/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import javafx.collections.ObservableList;
import java.util.Map;

/**
 * A theme is a set of stylesheets that define the visual appearance of an application.
 * <p>
 * Themes can be static (i.e. the set of stylesheets is defined by the theme creator and never changes),
 * or they can respond to the platform theme exposed by the operating system. Platform theme properties
 * are key-value pairs that describe the underlying platform theme. Different operating systems generally
 * have a different set of platform theme properties. For example, Windows 10 has accent colors that are
 * not available in earlier versions of Windows.
 * <p>
 * A theme implementation must have either
 * <ol>
 *     <li>a parameterless constructor, or
 *     <li>a single-argument constructor that accepts a {@link Map Map&lt;String, String&gt;}, which is
 *         the set of theme properties reported by the platform.
 * </ol>
 * Themes are instantiated by the JavaFX theming system. In order to load a custom theme, call
 * {@link Application#setUserAgentStylesheet(String)} with a theme-URI that contains the name of the
 * theme class:
 * <pre><code>
 *     Application.setUserAgentStylesheet("theme:com.example.CustomTheme");
 * </code></pre>
 * It is the responsibility of the theme creator to interpret platform theme properties.
 * <p>
 * Currently, only Windows platforms report theme properties:
 * <ol>
 *     <li>High contrast color scheme (as reported by SystemParametersInfo):
 *     <ul>
 *         <li>Windows.SPI_HighContrastOn
 *         <li>Windows.SPI_HighContrastColorScheme
*      </ul>
 *     <li>System colors (as reported by GetSysColor):
 *     <ul>
 *         <li>Windows.SysColor.COLOR_3DDKSHADOW
 *         <li>Windows.SysColor.COLOR_3DFACE
 *         <li>Windows.SysColor.COLOR_3DHIGHLIGHT
 *         <li>Windows.SysColor.COLOR_3DHILIGHT
 *         <li>Windows.SysColor.COLOR_3DLIGHT
 *         <li>Windows.SysColor.COLOR_3DSHADOW
 *         <li>Windows.SysColor.COLOR_ACTIVEBORDER
 *         <li>Windows.SysColor.COLOR_ACTIVECAPTION
 *         <li>Windows.SysColor.COLOR_APPWORKSPACE
 *         <li>Windows.SysColor.COLOR_BACKGROUND
 *         <li>Windows.SysColor.COLOR_BTNFACE
 *         <li>Windows.SysColor.COLOR_BTNHIGHLIGHT
 *         <li>Windows.SysColor.COLOR_BTNHILIGHT
 *         <li>Windows.SysColor.COLOR_BTNSHADOW
 *         <li>Windows.SysColor.COLOR_BTNTEXT
 *         <li>Windows.SysColor.COLOR_CAPTIONTEXT
 *         <li>Windows.SysColor.COLOR_DESKTOP
 *         <li>Windows.SysColor.COLOR_GRADIENTACTIVECAPTION
 *         <li>Windows.SysColor.COLOR_GRADIENTINACTIVECAPTION
 *         <li>Windows.SysColor.COLOR_GRAYTEXT
 *         <li>Windows.SysColor.COLOR_HIGHLIGHT
 *         <li>Windows.SysColor.COLOR_HIGHLIGHTTEXT
 *         <li>Windows.SysColor.COLOR_HOTLIGHT
 *         <li>Windows.SysColor.COLOR_INACTIVEBORDER
 *         <li>Windows.SysColor.COLOR_INACTIVECAPTION
 *         <li>Windows.SysColor.COLOR_INACTIVECAPTIONTEXT
 *         <li>Windows.SysColor.COLOR_INFOBK
 *         <li>Windows.SysColor.COLOR_INFOTEXT
 *         <li>Windows.SysColor.COLOR_MENU
 *         <li>Windows.SysColor.COLOR_MENUHILIGHT
 *         <li>Windows.SysColor.COLOR_MENUBAR
 *         <li>Windows.SysColor.COLOR_MENUTEXT
 *         <li>Windows.SysColor.COLOR_SCROLLBAR
 *         <li>Windows.SysColor.COLOR_WINDOW
 *         <li>Windows.SysColor.COLOR_WINDOWFRAME
 *         <li>Windows.SysColor.COLOR_WINDOWTEXT
 *     </ul>
 *     <li>Windows 10 theme colors (as reported by UISettings, introduced in Windows 10 build 10240):
 *     <ul>
 *         <li>Windows.UI.ViewManagement.UISettings.ColorValue_Background
 *         <li>Windows.UI.ViewManagement.UISettings.ColorValue_Foreground
 *         <li>Windows.UI.ViewManagement.UISettings.ColorValue_AccentDark3
 *         <li>Windows.UI.ViewManagement.UISettings.ColorValue_AccentDark2
 *         <li>Windows.UI.ViewManagement.UISettings.ColorValue_AccentDark1
 *         <li>Windows.UI.ViewManagement.UISettings.ColorValue_Accent
 *         <li>Windows.UI.ViewManagement.UISettings.ColorValue_AccentLight1
 *         <li>Windows.UI.ViewManagement.UISettings.ColorValue_AccentLight2
 *         <li>Windows.UI.ViewManagement.UISettings.ColorValue_AccentLight3
 *     </ul>
 * </ol>
 */
public interface Theme {

    /**
     * The list of stylesheets that comprise this theme.
     */
    ObservableList<String> getStylesheets();

    /**
     * Occurs when the platform theme has changed.
     *
     * @param properties theme properties reported by the platform
     */
    void platformThemeChanged(Map<String, String> properties);

}
