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

import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.PromptData;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.util.Builder;
import javafx.util.Callback;

/**
 * The builder for the {@link WebEngine} class.
 *
 * @author Sergey Malenkov
 * @deprecated This class is deprecated and will be removed in the next version
 * @since JavaFX 2.0
 */
@Deprecated
public final class WebEngineBuilder
        implements Builder<WebEngine> {

    /**
     * Creates new builder for the {@link WebEngine} class.
     *
     * @return the {@code WebEngineBuilder} object
     */
    public static WebEngineBuilder create() {
        return new WebEngineBuilder();
    }

    /**
     * Creates an instance of the {@link WebEngine} class
     * based on the properties set on this builder.
     */
    public WebEngine build() {
        WebEngine engine = new WebEngine();
        applyTo(engine);
        return engine;
    }

    /**
     * Applies initialized values to the properties of the {@link WebEngine} class.
     *
     * @param engine  the {@link WebEngine} object to initialize
     */
    public void applyTo(WebEngine engine) {
        if (confirmHandlerSet) {
            engine.setConfirmHandler(confirmHandler);
        }
        if (createPopupHandlerSet) {
            engine.setCreatePopupHandler(createPopupHandler);
        }
        if (onAlertSet) {
            engine.setOnAlert(onAlert);
        }
        if (onResizedSet) {
            engine.setOnResized(onResized);
        }
        if (onStatusChangedSet) {
            engine.setOnStatusChanged(onStatusChanged);
        }
        if (onVisibilityChangedSet) {
            engine.setOnVisibilityChanged(onVisibilityChanged);
        }
        if (promptHandlerSet) {
            engine.setPromptHandler(promptHandler);
        }
        if (locationSet) {
            engine.load(location);
        }
    }

    /**
     * Sets the {@link WebEngine#confirmHandlerProperty() confirmHandler}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code confirmHandler} property
     * @return this builder
     */
    public WebEngineBuilder confirmHandler(Callback<String, Boolean> value) {
        confirmHandler = value;
        confirmHandlerSet = true;
        return this;
    }

    private Callback<String, Boolean> confirmHandler;
    private boolean confirmHandlerSet;

    /**
     * Sets the {@link WebEngine#createPopupHandlerProperty() createPopupHandler}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code createPopupHandler} property
     * @return this builder
     */
    public WebEngineBuilder createPopupHandler(Callback<PopupFeatures, WebEngine> value) {
        createPopupHandler = value;
        createPopupHandlerSet = true;
        return this;
    }

    private Callback<PopupFeatures, WebEngine> createPopupHandler;
    private boolean createPopupHandlerSet;

    /**
     * Sets the {@link WebEngine#onAlertProperty() onAlert}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code onAlert} property
     * @return this builder
     */
    public WebEngineBuilder onAlert(EventHandler<WebEvent<String>> value) {
        onAlert = value;
        onAlertSet = true;
        return this;
    }

    private EventHandler<WebEvent<String>> onAlert;
    private boolean onAlertSet;

    /**
     * Sets the {@link WebEngine#onResizedProperty() onResized}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code onResized} property
     * @return this builder
     */
    public WebEngineBuilder onResized(EventHandler<WebEvent<Rectangle2D>> value) {
        onResized = value;
        onResizedSet = true;
        return this;
    }

    private EventHandler<WebEvent<Rectangle2D>> onResized;
    private boolean onResizedSet;

    /**
     * Sets the {@link WebEngine#onStatusChangedProperty() onStatusChanged}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code onStatusChanged} property
     * @return this builder
     */
    public WebEngineBuilder onStatusChanged(EventHandler<WebEvent<String>> value) {
        onStatusChanged = value;
        onStatusChangedSet = true;
        return this;
    }

    private EventHandler<WebEvent<String>> onStatusChanged;
    private boolean onStatusChangedSet;

    /**
     * Sets the {@link WebEngine#onVisibilityChangedProperty() onVisibilityChanged}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code onVisibilityChanged} property
     * @return this builder
     */
    public WebEngineBuilder onVisibilityChanged(EventHandler<WebEvent<Boolean>> value) {
        onVisibilityChanged = value;
        onVisibilityChangedSet = true;
        return this;
    }

    private EventHandler<WebEvent<Boolean>> onVisibilityChanged;
    private boolean onVisibilityChangedSet;

    /**
     * Sets the {@link WebEngine#promptHandlerProperty() promptHandler}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code promptHandler} property
     * @return this builder
     */
    public WebEngineBuilder promptHandler(Callback<PromptData, String> value) {
        promptHandler = value;
        promptHandlerSet = true;
        return this;
    }

    private Callback<PromptData, String> promptHandler;
    private boolean promptHandlerSet;

    /**
     * Sets the {@link WebEngine#locationProperty() location}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code location} property
     * @return this builder
     * @since JavaFX 2.1
     */
    public WebEngineBuilder location(String value) {
        location = value;
        locationSet = true;
        return this;
    }

    private String location;
    private boolean locationSet;
}
