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

package javafx.scene.control.theme;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.value.WritableValue;
import javafx.collections.ObservableList;
import javafx.css.StyleTheme;

/**
 * {@link ThemeBase} is a {@link StyleTheme} implementation that simplifies toggling or modifying
 * stylesheets while retaining the order in which the stylesheets were originally added.
 * <p>
 * Stylesheet URIs can be added to the theme by calling {@link #addFirst(String)} or {@link #addLast(String)}.
 * The value of a stylesheet URI can be changed at any time with the {@link WritableValue} wrapper
 * that is returned by {@code addFirst} and {@code addLast}.
 *
 * @since 21
 */
public abstract class ThemeBase implements StyleTheme {

    private final StylesheetList stylesheetList = new StylesheetList();

    private final InvalidationListener preferencesChanged = observable -> {
        try {
            stylesheetList.lock();
            onPreferencesChanged();
        } finally {
            stylesheetList.unlock();
        }
    };

    /**
     * Creates a new instance of the {@code ThemeBase} class.
     */
    protected ThemeBase() {
        Platform.getPreferences().addListener(new WeakInvalidationListener(preferencesChanged));
    }

    @Override
    public final ObservableList<String> getStylesheets() {
        return stylesheetList;
    }

    /**
     * Adds a new stylesheet URL at the front of the list of stylesheets.
     * <p>
     * The returned {@link WritableValue} can be used to change the value of the URL at runtime.
     * If the value is set to {@code null}, the stylesheet will not be included in the CSS cascade.
     *
     * @param url the stylesheet URL, or {@code null}
     * @return a {@code WritableValue} that represents the stylesheet URL in the list of stylesheets
     */
    protected final WritableValue<String> addFirst(String url) {
        return stylesheetList.addFirstElement(url);
    }

    /**
     * Adds a new stylesheet URL at the back of the list of stylesheets.
     * <p>
     * The returned {@link WritableValue} can be used to change the value of the URL at runtime.
     * If the value is set to {@code null}, the stylesheet will not be included in the CSS cascade.
     *
     * @param url the stylesheet URL, or {@code null}
     * @return a {@code WritableValue} that represents the stylesheet URL in the list of stylesheets
     */
    protected final WritableValue<String> addLast(String url) {
        return stylesheetList.addLastElement(url);
    }

    /**
     * Occurs when platform preferences have changed.
     */
    protected void onPreferencesChanged() {}

}
