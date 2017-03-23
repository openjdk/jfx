/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.value.WritableValue;

/**
 * StyleableProperty allows a {@link javafx.beans.property} to be styled from
 * CSS.
 * <p>This interface allows coordination between CSS
 * processing and a <code>javafx.beans.property</code>. The implementation
 * ensure that the priority for setting the value is, in increasing order
 * and assuming equal importance:
 * <ol>
 * <li>a style from a user agent stylesheet in
 * {@link javafx.application.Application#setUserAgentStylesheet(java.lang.String)}</li>
 * <li>value set from code, for example calling {@link javafx.scene.Node#setOpacity(double)}</li>
 * <li>a style from an author stylesheet in {@link javafx.scene.Scene#getStylesheets()}
 * or {@link javafx.scene.Parent#getStylesheets()}</li>
 * <li>a style from {@link javafx.scene.Node#setStyle(java.lang.String)}</li>
 * </ol>
 * <p>The {@link javafx.css.StyleablePropertyFactory StyleablePropertyFactory}
 * greatly simplifies creating a StyleableProperty and its corresponding CssMetaData.</p>
 * @param <T> the specific property
 * @since JavaFX 8.0
 * @see javafx.css.StyleablePropertyFactory
 */
public interface StyleableProperty<T> extends WritableValue<T> {

    /**
     * This method is called from CSS code to set the value of the property.
     * @param origin the origin
     * @param value the value
     */
    void applyStyle(StyleOrigin origin, T value);

    /**
     * Tells the origin of the value of the property. This is needed to
     * determine whether or not CSS can override the value.
     * @return the style origin
     */
    StyleOrigin getStyleOrigin();

    /**
     * Reflect back the CssMetaData that corresponds to this
     * <code>javafx.beans.property.StyleableProperty</code>
     * @return the corresponding CssMetaData
     */
    CssMetaData<? extends Styleable, T> getCssMetaData();

}
