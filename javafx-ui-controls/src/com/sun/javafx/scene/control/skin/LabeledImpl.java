/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.javafx.scene.control.skin;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;

import com.sun.javafx.css.StyleableProperty;


public class LabeledImpl extends Label {

    private Labeled labeled;

    public LabeledImpl(final Labeled labeled) {
        this.labeled = labeled;
        setLabelFor(labeled);
        // For calls to setXXX added or removed, update the onPropertyChanged
        // method below
        setFont(labeled.getFont()); // set font or rely on skin's css?
        setText(labeled.getText());
        setGraphic(labeled.getGraphic());
        setAlignment(labeled.getAlignment());
        setContentDisplay(labeled.getContentDisplay());
        setGraphicTextGap(labeled.getGraphicTextGap());
        setTextAlignment(labeled.getTextAlignment());
        setTextOverrun(labeled.getTextOverrun());
        setUnderline(labeled.isUnderline());
        setWrapText(labeled.isWrapText());
        InvalidationListener shuttler = new InvalidationListener() {
            @Override public void invalidated(Observable valueModel) {
                if (valueModel == labeled.textProperty()) {
                    setText(labeled.getText());
                } else if (valueModel == labeled.alignmentProperty()) {
                    setAlignment(labeled.getAlignment());
                } else if (valueModel == labeled.textAlignmentProperty()) {
                    setTextAlignment(labeled.getTextAlignment());
                } else if (valueModel == labeled.textOverrunProperty()) {
                    setTextOverrun(labeled.getTextOverrun());
                } else if (valueModel == labeled.wrapTextProperty()) {
                    setWrapText(labeled.isWrapText());
                } else if (valueModel == labeled.fontProperty()) {
                    setFont(labeled.getFont());
                } else if (valueModel == labeled.graphicProperty()) {
                    setGraphic(labeled.getGraphic());
                } else if (valueModel == labeled.underlineProperty()) {
                    setUnderline(labeled.isUnderline());
                } else if (valueModel == labeled.contentDisplayProperty()) {
                    setContentDisplay(labeled.getContentDisplay());
                } else if (valueModel == labeled.graphicTextGapProperty()) {
                    setGraphicTextGap(labeled.getGraphicTextGap());
                }
            }
        };
        labeled.textProperty().addListener(shuttler);
        labeled.alignmentProperty().addListener(shuttler);
        labeled.textAlignmentProperty().addListener(shuttler);
        labeled.textOverrunProperty().addListener(shuttler);
        labeled.wrapTextProperty().addListener(shuttler);
        labeled.fontProperty().addListener(shuttler);
        labeled.graphicProperty().addListener(shuttler);
        labeled.underlineProperty().addListener(shuttler);
        labeled.contentDisplayProperty().addListener(shuttler);
        labeled.graphicTextGapProperty().addListener(shuttler);
    }

    /*
     * Fix for RT-10554. Since this Label's properties are set by the
     * ChangeListener, from the CSS perspective it looks like they were
     * set by the user and CSS won't override the value. The fix here is
     * to proxy the call to impl_cssSettable since the user would be
     * be calling setXXX on the wrapped Labeled, not the skin.
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public boolean impl_cssSettable(StyleableProperty key) {

        // The list of properties here must be the same as those handled
        // by the onPropertyChanged method
        final String property = key.getProperty();
        if ("-fx-font".equals(property) ||
            "-fx-text".equals(property) ||
            "-fx-graphic".equals(property) ||
            "-fx-alignment".equals(property) ||
            "-fx-text-alignment".equals(property) ||
            "-fx-text-overrun".equals(property) ||
            "-fx-underline".equals(property) ||
            "-fx-wrap-text".equals(property) ||
            "-fx-content-display".equals(property) ||
            "-fx-graphic-text-gap".equals(property)) {
            return labeled.impl_cssSettable(key);
        }
        return super.impl_cssSettable(key);
    }

    /*
     * Fix for RT-10617. This Label's properties are set by the
     * ChangeListener.  CSS won't override the value. The fix here is
     * to proxy the call to impl_cssSettable since the user would be
     * be calling setXXX on the wrapped Labeled, not the skin.
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_cssSet(StyleableProperty key, Object value) {

        // The list of properties here must be the same as those handled
        // by the onPropertyChanged method
        final String property = key.getProperty();
        if ("-fx-font".equals(property) ||
            "-fx-text".equals(property) ||
            "-fx-graphic".equals(property) ||
            "-fx-alignment".equals(property) ||
            "-fx-text-alignment".equals(property) ||
            "-fx-text-overrun".equals(property) ||
            "-fx-underline".equals(property) ||
            "-fx-wrap-text".equals(property) ||
            "-fx-content-display".equals(property) ||
            "-fx-graphic-text-gap".equals(property)) {
            return;
        }
        super.impl_cssSet(key, value);
    }
}
