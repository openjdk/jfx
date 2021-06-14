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

import com.sun.javafx.application.PlatformImpl;
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
 * A theme class can only be instantiated by the JavaFX theming system if it implements a parameterless
 * constructor or a constructor with the following signature:
 * <pre>
 *     public CustomTheme({@link Map Map&lt;String, String&gt;} platformProperties)
 * </pre>
 * If the parameterized constructor is implemented, it receives a full list of platform theme properties
 * when the JavaFX theming system instantiates the class.
 * In order to load a custom theme, call {@link Application#setUserAgentStylesheet(String)}
 * with a theme-URI that contains the name of the theme class:
 * <pre>
 *     Application.setUserAgentStylesheet("theme:com.example.CustomTheme");
 * </pre>
 * It is the responsibility of the theme creator to interpret platform theme properties.
 * <p>
 * Currently, only Windows platforms report theme properties:
 * <ol>
 *     <li>High contrast color scheme (as reported by SystemParametersInfo):
 *     <pre>
 *     Windows.SPI_HighContrastOn             "true" | "false
 *     Windows.SPI_HighContrastColorScheme    hex-color-string
 *     </pre>
 *     <li>System colors (as reported by GetSysColor):
 *     <pre>
 *     Windows.SysColor.COLOR_3DDKSHADOW                 hex-color-string
 *     Windows.SysColor.COLOR_3DFACE                     hex-color-string
 *     Windows.SysColor.COLOR_3DHIGHLIGHT                hex-color-string
 *     Windows.SysColor.COLOR_3DHILIGHT                  hex-color-string
 *     Windows.SysColor.COLOR_3DLIGHT                    hex-color-string
 *     Windows.SysColor.COLOR_3DSHADOW                   hex-color-string
 *     Windows.SysColor.COLOR_ACTIVEBORDER               hex-color-string
 *     Windows.SysColor.COLOR_ACTIVECAPTION              hex-color-string
 *     Windows.SysColor.COLOR_APPWORKSPACE               hex-color-string
 *     Windows.SysColor.COLOR_BACKGROUND                 hex-color-string
 *     Windows.SysColor.COLOR_BTNFACE                    hex-color-string
 *     Windows.SysColor.COLOR_BTNHIGHLIGHT               hex-color-string
 *     Windows.SysColor.COLOR_BTNHILIGHT                 hex-color-string
 *     Windows.SysColor.COLOR_BTNSHADOW                  hex-color-string
 *     Windows.SysColor.COLOR_BTNTEXT                    hex-color-string
 *     Windows.SysColor.COLOR_CAPTIONTEXT                hex-color-string
 *     Windows.SysColor.COLOR_DESKTOP                    hex-color-string
 *     Windows.SysColor.COLOR_GRADIENTACTIVECAPTION      hex-color-string
 *     Windows.SysColor.COLOR_GRADIENTINACTIVECAPTION    hex-color-string
 *     Windows.SysColor.COLOR_GRAYTEXT                   hex-color-string
 *     Windows.SysColor.COLOR_HIGHLIGHT                  hex-color-string
 *     Windows.SysColor.COLOR_HIGHLIGHTTEXT              hex-color-string
 *     Windows.SysColor.COLOR_HOTLIGHT                   hex-color-string
 *     Windows.SysColor.COLOR_INACTIVEBORDER             hex-color-string
 *     Windows.SysColor.COLOR_INACTIVECAPTION            hex-color-string
 *     Windows.SysColor.COLOR_INACTIVECAPTIONTEXT        hex-color-string
 *     Windows.SysColor.COLOR_INFOBK                     hex-color-string
 *     Windows.SysColor.COLOR_INFOTEXT                   hex-color-string
 *     Windows.SysColor.COLOR_MENU                       hex-color-string
 *     Windows.SysColor.COLOR_MENUHILIGHT                hex-color-string
 *     Windows.SysColor.COLOR_MENUBAR                    hex-color-string
 *     Windows.SysColor.COLOR_MENUTEXT                   hex-color-string
 *     Windows.SysColor.COLOR_SCROLLBAR                  hex-color-string
 *     Windows.SysColor.COLOR_WINDOW                     hex-color-string
 *     Windows.SysColor.COLOR_WINDOWFRAME                hex-color-string
 *     Windows.SysColor.COLOR_WINDOWTEXT                 hex-color-string
 *     </pre>
 *     <li>Windows 10 theme colors (as reported by UISettings, introduced in Windows 10 build 10240):
 *     <pre>
 *     Windows.UI.ViewManagement.UISettings.ColorValue_Background      hex-color-string
 *     Windows.UI.ViewManagement.UISettings.ColorValue_Foreground      hex-color-string
 *     Windows.UI.ViewManagement.UISettings.ColorValue_AccentDark3     hex-color-string
 *     Windows.UI.ViewManagement.UISettings.ColorValue_AccentDark2     hex-color-string
 *     Windows.UI.ViewManagement.UISettings.ColorValue_AccentDark1     hex-color-string
 *     Windows.UI.ViewManagement.UISettings.ColorValue_Accent          hex-color-string
 *     Windows.UI.ViewManagement.UISettings.ColorValue_AccentLight1    hex-color-string
 *     Windows.UI.ViewManagement.UISettings.ColorValue_AccentLight2    hex-color-string
 *     Windows.UI.ViewManagement.UISettings.ColorValue_AccentLight3    hex-color-string
 *     </pre>
 * </ol>
 * {@code hex-color-string} is a value that can be parsed by {@link javafx.scene.paint.Color#web(String)}
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

    /**
     * Gets the current theme for the application.
     *
     * @return the current theme if available, {@code null} otherwise
     */
    static Theme currentTheme() {
        return PlatformImpl.getCurrentTheme();
    }

}
