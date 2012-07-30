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
import com.sun.javafx.css.Stylesheet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.value.WritableValue;
import javafx.scene.Parent;
import javafx.scene.layout.Region;


public class LabeledImpl extends Label {

    public LabeledImpl(final Labeled labeled) {
        this.shuttler = new Shuttler(this, labeled);
    }
        
    final private Shuttler shuttler;
    
    private static void initialize(Shuttler shuttler, LabeledImpl labeledImpl, Labeled labeled) {
        
        labeledImpl.setText(labeled.getText());
        labeled.textProperty().addListener(shuttler);
        
        labeledImpl.setGraphic(labeled.getGraphic());
        labeled.graphicProperty().addListener(shuttler);
        
        final List<StyleableProperty> styleables = StyleableProperties.STYLEABLES_TO_MIRROR;
        
        for(int n=0, nMax=styleables.size(); n<nMax; n++) {
            final StyleableProperty styleable = styleables.get(n);
            
            // the Labeled isn't necessarily a Label, so skip the skin or
            // we'll get an argument type mismatch on the invocation of the
            // skin constructor. 
            if ("-fx-skin".equals(styleable.getProperty())) continue;
            
            final WritableValue fromVal = styleable.getWritableValue(labeled);
            if (fromVal instanceof Observable) {
                // listen for changes to this property
                ((Observable)fromVal).addListener(shuttler);
                // set this LabeledImpl's property to the same value as the Labeled. 
                final Stylesheet.Origin origin = StyleableProperty.getOrigin(fromVal);
                styleable.set(labeledImpl, fromVal.getValue(), origin);
            }
        }
    }
    
    private static class Shuttler implements InvalidationListener {
        
        private final LabeledImpl labeledImpl;
        private final Labeled labeled; 
        
        Shuttler(LabeledImpl labeledImpl, Labeled labeled) {
            this.labeledImpl = labeledImpl;
            this.labeled = labeled;
            initialize(this, labeledImpl, labeled);

        }
        
        @Override public void invalidated(Observable valueModel) {
          
            if (valueModel == labeled.textProperty()) {
                labeledImpl.setText(labeled.getText());
            } else if (valueModel == labeled.graphicProperty()) {
                // If the user set the graphic, then mirror that.
                // Otherwise, the graphic was set via the imageUrlProperty which
                // will be mirrored onto the labeledImpl by the next block.
                Stylesheet.Origin origin = StyleableProperty.getOrigin(labeled.graphicProperty());
                if (origin == null || origin == Stylesheet.Origin.USER) {
                    labeledImpl.setGraphic(labeled.getGraphic());
                }
                
            } else if (valueModel instanceof WritableValue) { 
                WritableValue writable = (WritableValue)valueModel;
                StyleableProperty styleable = 
                        StyleableProperty.getStyleableProperty(writable);
                if (styleable != null) {
                    Stylesheet.Origin origin = StyleableProperty.getOrigin(writable);
                    styleable.set(labeledImpl, writable.getValue(), origin);
                }
            }
        }
    }

    /** Protected for unit test purposes */
    static final class StyleableProperties {

        static final List<StyleableProperty> STYLEABLES_TO_MIRROR;
        static {
            //
            // We do this as we only want to mirror the Labeled's keys,
            // none of Parent's, otherwise all of the properties on Parent,
            // like opacity, would be applied twice (once to the Labeled and 
            // again to the LabeledImpl). 
            //
            // Note, however, that this subset is not the list of properties
            // for this LabeledImpl that can be styled. For that, we want all
            // the properites that are inherited by virtue of LabeledImpl 
            // being a Label. This allows for the LabledImpl to be styled
            // with styles like .menu-button .label { -fx-opacity: 80%; }
            // If just this subset were returned (by impl_CSS_STYLEABLE) then
            // -fx-opacity (for example) would be meaningless to the Labeled. 
            // 
            final List<StyleableProperty> labeledStyleables = Labeled.impl_CSS_STYLEABLES();
            final List<StyleableProperty> parentStyleables = Region.impl_CSS_STYLEABLES();
            final List<StyleableProperty> styleables = 
                new ArrayList<StyleableProperty>(labeledStyleables);
            styleables.removeAll(parentStyleables);
            STYLEABLES_TO_MIRROR = Collections.unmodifiableList(styleables);
        }
    }

}
