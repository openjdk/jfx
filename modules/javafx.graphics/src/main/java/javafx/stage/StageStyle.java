/*
 * Copyright (c) 2008, 2025, Oracle and/or its affiliates. All rights reserved.
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

package javafx.stage;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderButtonType;

/**
 * This enum defines the possible styles for a {@code Stage}.
 * @since JavaFX 2.0
 */
public enum StageStyle {

    /**
     * Defines a normal {@code Stage} style with a solid white background and platform decorations.
     */
    DECORATED,

    /**
     * Defines a {@code Stage} style with a solid white background and no window
     * decorations, such as a title bar, borders, or window controls.
     * This style allows window operations such as resize, minimize, maximize
     * and fullscreen to be either programmatically controlled or achieved through
     * platform-specific functions, such as key shortcuts or menu options.
     */
    UNDECORATED,

    /**
     * Defines a {@code Stage} style with a transparent background and no decorations.
     * This is a conditional feature; to check if it is supported use
     * {@link javafx.application.Platform#isSupported(javafx.application.ConditionalFeature)}.
     * If the feature is not supported by the platform, this style downgrades
     * to {@code StageStyle.UNDECORATED}
     */
    TRANSPARENT,

    /**
     * Defines a lightweight {@code Stage} style with a solid white background and minimal
     * decorations, intended for supporting tasks such as tool palettes.
     * <p>
     * Utility stages may restrict window operations like maximize, minimize,
     * and fullscreen depending on the platform. They are designed to float above
     * primary windows without acting as a main application stage.
     */
    UTILITY,

    /**
     * Defines a {@code Stage} style with platform decorations and eliminates the border between
     * client area and decorations. The client area background is unified with the decorations.
     * This is a conditional feature, to check if it is supported see
     * {@link javafx.application.Platform#isSupported(javafx.application.ConditionalFeature)}.
     * If the feature is not supported by the platform, this style downgrades to {@code StageStyle.DECORATED}
     * <p>
     * NOTE: To see the effect, the {@code Scene} covering the {@code Stage} should have {@code Color.TRANSPARENT}
     * @since JavaFX 8.0
     */
    UNIFIED,

    /**
     * Defines a {@code Stage} style in which the client area is extended into the header bar area, removing
     * the separation between the two areas and allowing applications to place scene graph nodes in the header
     * bar area of the stage.
     * <p>
     * This is a conditional feature, to check if it is supported see {@link Platform#isSupported(ConditionalFeature)}.
     * If the feature is not supported by the platform, this style downgrades to {@link StageStyle#DECORATED}.
     *
     * <h4>Usage</h4>
     * An extended window has the default header buttons (iconify, maximize, close), but no system-provided
     * draggable header bar. Applications need to provide their own header bar by placing a {@link HeaderBar}
     * control in the scene graph. The {@code HeaderBar} control should be positioned at the top of the window
     * and its width should extend the entire width of the window, as otherwise the layout of the default window
     * buttons and the header bar content might not be aligned correctly. Usually, {@code HeaderBar} is combined
     * with a {@link BorderPane} root container:
     * <pre>{@code
     * public class MyApp extends Application {
     *     @Override
     *     public void start(Stage stage) {
     *         var headerBar = new HeaderBar();
     *         var root = new BorderPane();
     *         root.setTop(headerBar);
     *
     *         stage.setScene(new Scene(root));
     *         stage.initStyle(StageStyle.EXTENDED);
     *         stage.show();
     *     }
     * }
     * }</pre>
     *
     * <h4>Color scheme</h4>
     * The color scheme of the default header buttons is automatically adjusted to remain easily recognizable
     * by inspecting the {@link Scene#fillProperty() Scene.fill} property to gauge the brightness of the user
     * interface. Applications should set the scene fill to a color that matches the user interface of the header
     * bar area, even if the scene fill is not visible because it is obscured by other controls.
     *
     * <h4>Custom header buttons</h4>
     * If more control over the header buttons is desired, applications can opt out of the default header buttons
     * by setting {@link HeaderBar#setPrefButtonHeight(Stage, double)} to zero and providing custom header buttons
     * instead. Any JavaFX control can be used as a custom header button by setting its semantic type with the
     * {@link HeaderBar#setButtonType(Node, HeaderButtonType)} method.
     *
     * <h4>Title text</h4>
     * An extended stage has no title text. Applications that require title text need to provide their own
     * implementation by placing a {@code Label} or a similar control in the custom header bar.
     * Note that the value of {@link Stage#titleProperty()} may still be used by the platform, for example
     * in the title of miniaturized preview windows.
     *
     * @since 25
     * @deprecated This is a preview feature which may be changed or removed in a future release.
     */
    @Deprecated(since = "25")
    EXTENDED
}
