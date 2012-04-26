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
import java.util.List;
import javafx.beans.value.WritableValue;


public class LabeledImpl extends Label {

    private Labeled labeled;
    
    private final static StyleableProperty graphicProperty;
    static {
        StyleableProperty prop = null;
        final List<StyleableProperty> properties = Labeled.impl_CSS_STYLEABLES();
        for(int n=0, nMax=properties.size(); n<nMax; n++) {
            final StyleableProperty styleable = properties.get(n);
            if ("-fx-graphic".equals(styleable.getProperty())) {
                prop = styleable;
                break;
            }
        }
        graphicProperty = prop;
    }

    public LabeledImpl(final Labeled labeled) {
        this.labeled = labeled;
        setLabelFor(labeled);
        // For calls to setXXX added or removed, update the onPropertyChanged
        // method below
        setFont(labeled.getFont()); // set font or rely on skin's css?
        setText(labeled.getText());
        setTextFill(labeled.getTextFill());
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
                                        
                } else if (valueModel == labeled.textFillProperty()) {
                    
                    //
                    // Fix for RT-10554. Since this Label's properties are set by the
                    // ChangeListener, from the CSS perspective it looks like they were
                    // set by the user and CSS won't override the value.
                    //
                    StyleableProperty styleable = 
                        StyleableProperty.getStyleableProperty(labeled.textFillProperty());
                    styleable.set(LabeledImpl.this, labeled.getTextFill());
                                        
                } else if (valueModel == labeled.alignmentProperty()) {
                    //setAlignment(labeled.getAlignment());
                    StyleableProperty styleable = 
                        StyleableProperty.getStyleableProperty(labeled.alignmentProperty());
                    styleable.set(LabeledImpl.this, labeled.getAlignment());
                    
                } else if (valueModel == labeled.textAlignmentProperty()) {
                    //setTextAlignment(labeled.getTextAlignment());
                    StyleableProperty styleable = 
                        StyleableProperty.getStyleableProperty(labeled.textAlignmentProperty());
                    styleable.set(LabeledImpl.this, labeled.getTextAlignment());
                    
                } else if (valueModel == labeled.textOverrunProperty()) {
                    //setTextOverrun(labeled.getTextOverrun());
                    StyleableProperty styleable = 
                        StyleableProperty.getStyleableProperty(labeled.textOverrunProperty());
                    styleable.set(LabeledImpl.this, labeled.getTextOverrun());
                    
                } else if (valueModel == labeled.wrapTextProperty()) {
                    //setWrapText(labeled.isWrapText());
                    StyleableProperty styleable = 
                        StyleableProperty.getStyleableProperty(labeled.wrapTextProperty());
                    styleable.set(LabeledImpl.this, labeled.isWrapText());
                                        
                } else if (valueModel == labeled.fontProperty()) {
                    //setFont(labeled.getFont());
                    StyleableProperty styleable = 
                        StyleableProperty.getStyleableProperty(labeled.fontProperty());
                    styleable.set(LabeledImpl.this, labeled.getFont());
                                        
                } else if (valueModel == labeled.graphicProperty()) {
                    //setGraphic(labeled.getGraphic());
                    WritableValue fromVal = graphicProperty.getWritableValue(labeled);
                    WritableValue toVal = graphicProperty.getWritableValue(LabeledImpl.this);
                    toVal.setValue(fromVal.getValue());
                } else if (valueModel == labeled.underlineProperty()) {
                    //setUnderline(labeled.isUnderline());
                    StyleableProperty styleable = 
                        StyleableProperty.getStyleableProperty(labeled.underlineProperty());
                    styleable.set(LabeledImpl.this, labeled.isUnderline());
                    
                } else if (valueModel == labeled.contentDisplayProperty()) {
                    //setContentDisplay(labeled.getContentDisplay());
                    StyleableProperty styleable = 
                        StyleableProperty.getStyleableProperty(labeled.contentDisplayProperty());
                    styleable.set(LabeledImpl.this, labeled.getContentDisplay());
                    
                } else if (valueModel == labeled.graphicTextGapProperty()) {
                    //setGraphicTextGap(labeled.getGraphicTextGap());
                    StyleableProperty styleable = 
                        StyleableProperty.getStyleableProperty(labeled.graphicTextGapProperty());
                    styleable.set(LabeledImpl.this, labeled.getGraphicTextGap());
                    
                }
            }
        };
        labeled.textProperty().addListener(shuttler);
        labeled.textFillProperty().addListener(shuttler);
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
}
