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

package javafx.css;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import java.io.File;
import java.util.List;

/**
 * {@code StyleTheme} is a collection of user-agent stylesheets that specify the appearance of UI controls and
 * other nodes in the application. {@code StyleTheme} is implicitly used by all JavaFX nodes in the scene graph,
 * unless it is overridden by any of the following properties:
 * <ul>
 *     <li>{@link Application#userAgentStylesheetProperty() Application.userAgentStylesheet}
 *     <li>{@link Scene#userAgentStylesheetProperty() Scene.userAgentStylesheet}
 *     <li>{@link SubScene#userAgentStylesheetProperty() SubScene.userAgentStylesheet}
 * </ul>
 * <p>
 * The list of stylesheets that comprise a {@code StyleTheme} can be modified while the application is running,
 * enabling applications to create dynamic themes that respond to changing user preferences.
 * <p>
 * A {@code StyleTheme} can be applied using the {@link Application#setUserAgentStyleTheme(StyleTheme)} method:
 * <pre>{@code
 *     public class App extends Application {
 *         @Override
 *         public void start(Stage primaryStage) {
 *             setUserAgentStyleTheme(new MyCustomTheme());
 *
 *             primaryStage.setScene(...);
 *             primaryStage.show();
 *         }
 *     }
 * }</pre>
 *
 * @since 21
 */
public interface StyleTheme {

    /**
     * Gets the list of stylesheet URLs that comprise this {@code StyleTheme}.
     * <p>
     * The URL is a hierarchical URI of the form [scheme:][//authority][path]. If the URL
     * does not have a [scheme:] component, the URL is considered to be the [path] component only.
     * Any leading '/' character of the [path] is ignored and the [path] is treated as a path relative to
     * the root of the application's classpath.
     * <p>
     * The RFC 2397 "data" scheme for URLs is supported in addition to the protocol handlers that
     * are registered for the application.
     * If a URL uses the "data" scheme and the MIME type is either empty, "text/plain", or "text/css",
     * the payload will be interpreted as a CSS file.
     * If the MIME type is "application/octet-stream", the payload will be interpreted as a binary
     * CSS file (see {@link Stylesheet#convertToBinary(File, File)}).
     * <p>
     * If the list of stylesheets that comprise this {@code StyleTheme} is changed at runtime, this
     * method must return an {@link ObservableList} to allow the CSS subsystem to subscribe to list
     * change notifications.
     *
     * @implSpec Implementations of this method that return an {@link ObservableList} must emit all
     *           change notifications on the JavaFX application thread.
     *
     * @implNote Implementations of this method that return an {@link ObservableList} are encouraged
     *           to minimize the number of subsequent list change notifications that are fired by the
     *           list, as each change notification causes the CSS subsystem to re-apply the referenced
     *           stylesheets.
     *
     * @return the list of stylesheet URLs
     */
    List<String> getStylesheets();

}
