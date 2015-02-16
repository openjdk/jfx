/* 
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.fxml.builder.web;

// TODO: remove this class as part of fixing RT-40037.

import javafx.util.Builder;
import javafx.util.Callback;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.PromptData;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;

/**
 * The builder for the {@link WebView} class.
 *
 * @author Sergey Malenkov
 * @deprecated This class is deprecated and will be removed in the next version
 * @since JavaFX 2.0
 */
@Deprecated
public final class WebViewBuilder
        extends ParentBuilder<WebViewBuilder>
        implements Builder<WebView> {

    /**
     * Creates new builder for the {@link WebView} class.
     *
     * @return the {@code WebViewBuilder} object
     */
    public static WebViewBuilder create() {
        return new WebViewBuilder();
    }

    /**
     * Creates an instance of the {@link WebView} class
     * based on the properties set on this builder.
     */
    public WebView build() {
        WebView x = new WebView();
        applyTo(x);
        return x;
    }

    /**
     * Applies initialized values to the properties of the {@link WebView} class.
     *
     * @param view  the {@link WebView} object to initialize
     */
    public void applyTo(WebView view) {
        super.applyTo(view);
        if (fontScaleSet) {
            view.setFontScale(fontScale);
        }
        if (maxHeightSet) {
            view.setMaxHeight(maxHeight);
        }
        if (maxWidthSet) {
            view.setMaxWidth(maxWidth);
        }
        if (minHeightSet) {
            view.setMinHeight(minHeight);
        }
        if (minWidthSet) {
            view.setMinWidth(minWidth);
        }
        if (prefHeightSet) {
            view.setPrefHeight(prefHeight);
        }
        if (prefWidthSet) {
            view.setPrefWidth(prefWidth);
        }
        if (engineBuilder != null) {
            engineBuilder.applyTo(view.getEngine());
        }
    }

    /**
     * Sets the {@link WebView#fontScaleProperty() fontScale}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code fontScale} property
     * @return this builder
     */
    public WebViewBuilder fontScale(double value) {
        fontScale = value;
        fontScaleSet = true;
        return this;
    }

    private double fontScale;
    private boolean fontScaleSet;

    /**
     * Sets the {@link WebView#maxHeightProperty() maxHeight}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code maxHeight} property
     * @return this builder
     */
    public WebViewBuilder maxHeight(double value) {
        maxHeight = value;
        maxHeightSet = true;
        return this;
    }

    private double maxHeight;
    private boolean maxHeightSet;

    /**
     * Sets the {@link WebView#maxWidthProperty() maxWidth}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code maxWidth} property
     * @return this builder
     */
    public WebViewBuilder maxWidth(double value) {
        maxWidth = value;
        maxWidthSet = true;
        return this;
    }

    private double maxWidth;
    private boolean maxWidthSet;

    /**
     * Sets the {@link WebView#minHeightProperty() minHeight}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code minHeight} property
     * @return this builder
     */
    public WebViewBuilder minHeight(double value) {
        minHeight = value;
        minHeightSet = true;
        return this;
    }

    private double minHeight;
    private boolean minHeightSet;

    /**
     * Sets the {@link WebView#minWidthProperty() minWidth}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code minWidth} property
     * @return this builder
     */
    public WebViewBuilder minWidth(double value) {
        minWidth = value;
        minWidthSet = true;
        return this;
    }

    private double minWidth;
    private boolean minWidthSet;

    /**
     * Sets the {@link WebView#prefHeightProperty() prefHeight}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code prefHeight} property
     * @return this builder
     */
    public WebViewBuilder prefHeight(double value) {
        prefHeight = value;
        prefHeightSet = true;
        return this;
    }

    private double prefHeight;
    private boolean prefHeightSet;

    /**
     * Sets the {@link WebView#prefWidthProperty() prefWidth}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code prefWidth} property
     * @return this builder
     */
    public WebViewBuilder prefWidth(double value) {
        prefWidth = value;
        prefWidthSet = true;
        return this;
    }

    private double prefWidth;
    private boolean prefWidthSet;

    /**
     * Sets the {@link WebEngine#confirmHandlerProperty() confirmHandler}
     * property for the {@link WebView#getEngine() engine}
     * property of the instance constructed by this builder.
     *
     * @param value  new value of the {@code confirmHandler} property
     * @return this builder
     * @since JavaFX 2.1
     */
    public WebViewBuilder confirmHandler(Callback<String, Boolean> value) {
        engineBuilder().confirmHandler(value);
        return this;
    }

    /**
     * Sets the {@link WebEngine#createPopupHandlerProperty() createPopupHandler}
     * property for the {@link WebView#getEngine() engine}
     * property of the instance constructed by this builder.
     *
     * @param value  new value of the {@code createPopupHandler} property
     * @return this builder
     * @since JavaFX 2.1
     */
    public WebViewBuilder createPopupHandler(Callback<PopupFeatures, WebEngine> value) {
        engineBuilder().createPopupHandler(value);
        return this;
    }

    /**
     * Sets the {@link WebEngine#onAlertProperty() onAlert}
     * property for the {@link WebView#getEngine() engine}
     * property of the instance constructed by this builder.
     *
     * @param value  new value of the {@code onAlert} property
     * @return this builder
     * @since JavaFX 2.1
     */
    public WebViewBuilder onAlert(EventHandler<WebEvent<String>> value) {
        engineBuilder().onAlert(value);
        return this;
    }

    /**
     * Sets the {@link WebEngine#onResizedProperty() onResized}
     * property for the {@link WebView#getEngine() engine}
     * property of the instance constructed by this builder.
     *
     * @param value  new value of the {@code onResized} property
     * @return this builder
     * @since JavaFX 2.1
     */
    public WebViewBuilder onResized(EventHandler<WebEvent<Rectangle2D>> value) {
        engineBuilder().onResized(value);
        return this;
    }

    /**
     * Sets the {@link WebEngine#onStatusChangedProperty() onStatusChanged}
     * property for the {@link WebView#getEngine() engine}
     * property of the instance constructed by this builder.
     *
     * @param value  new value of the {@code onStatusChanged} property
     * @return this builder
     * @since JavaFX 2.1
     */
    public WebViewBuilder onStatusChanged(EventHandler<WebEvent<String>> value) {
        engineBuilder().onStatusChanged(value);
        return this;
    }

    /**
     * Sets the {@link WebEngine#onVisibilityChangedProperty() onVisibilityChanged}
     * property for the {@link WebView#getEngine() engine}
     * property of the instance constructed by this builder.
     *
     * @param value  new value of the {@code onVisibilityChanged} property
     * @return this builder
     * @since JavaFX 2.1
     */
    public WebViewBuilder onVisibilityChanged(EventHandler<WebEvent<Boolean>> value) {
        engineBuilder().onVisibilityChanged(value);
        return this;
    }

    /**
     * Sets the {@link WebEngine#promptHandlerProperty() promptHandler}
     * property for the {@link WebView#getEngine() engine}
     * property of the instance constructed by this builder.
     *
     * @param value  new value of the {@code promptHandler} property
     * @return this builder
     * @since JavaFX 2.1
     */
    public WebViewBuilder promptHandler(Callback<PromptData, String> value) {
        engineBuilder().promptHandler(value);
        return this;
    }

    /**
     * Sets the {@link WebEngine#locationProperty() location}
     * property for the {@link WebView#getEngine() engine}
     * property of the instance constructed by this builder.
     *
     * @param value  new value of the {@code location} property
     * @return this builder
     * @since JavaFX 2.1
     */
    public WebViewBuilder location(String value) {
        engineBuilder().location(value);
        return this;
    }

    private WebEngineBuilder engineBuilder;

    private WebEngineBuilder engineBuilder() {
        if (engineBuilder == null) {
            engineBuilder = WebEngineBuilder.create();
        }
        return engineBuilder;
    }
}
