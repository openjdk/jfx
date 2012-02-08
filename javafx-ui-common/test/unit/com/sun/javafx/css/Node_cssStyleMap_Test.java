/*
* Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css;

import com.sun.javafx.css.StyleHelper.StyleCacheKey;
import com.sun.javafx.css.converters.SizeConverter;
import javafx.scene.paint.Color;
import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.scene.shape.Rectangle;
import org.junit.Test;
import static org.junit.Assert.*;


public class Node_cssStyleMap_Test {
    
    public Node_cssStyleMap_Test() {
    }

    int nchanges = 0;
    
    @Test
    public void testStyleMapTracksChanges() {

        final List<Declaration> decls = new ArrayList<Declaration>();
        Collections.addAll(decls, 
            new Declaration("-fx-fill", new ParsedValue<Color,Color>(Color.RED, null), false),
            new Declaration("-fx-stroke", new ParsedValue<Color,Color>(Color.YELLOW, null), false),
            new Declaration("-fx-stroke-width", new ParsedValue<ParsedValue<?,Size>,Double>(
                new ParsedValue<Size,Size>(new Size(3d, SizeUnits.PX), null), 
                SizeConverter.getInstance()), false)
        );
        
        final List<Selector> sels = new ArrayList<Selector>();
        Collections.addAll(sels, 
            Selector.createSelector("rect")
        );
        
        Rule rule = new Rule(sels, decls);        
        
        Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(Stylesheet.Origin.USER_AGENT);
        stylesheet.getRules().add(rule);
        
        final List<CascadingStyle> styles = new ArrayList<CascadingStyle>();
        for (Declaration decl : decls) {
            styles.add(
                new CascadingStyle(
                    new Style(sels.get(0), decl), 
                    Collections.EMPTY_LIST,
                    0, 
                    0
                )
            );
        }
        
        // add to this list on wasAdded, check bean on wasRemoved.
        final List<WritableValue> beans = new ArrayList<WritableValue>();
        
        final Map<WritableValue,List<Style>> styleMap = 
                FXCollections.observableMap(new HashMap<WritableValue, List<Style>>());
        
        final Rectangle rect = new Rectangle(50,50) {

            // I'm bypassing StyleManager by creating StyleHelper directly. 
            StyleHelper shelper = null;
            
            @Override
            public Reference<StyleCacheKey> impl_getStyleCacheKey() {
                return shelper.createStyleCacheKey(this);
            }
            
            @Override
            public StyleHelper impl_createStyleHelper() {
                
                // If no styleclass, then create an StyleHelper with no mappings.
                // Otherwise, create a StyleHelper matching the "rect" style class.
                if (this.getStyleClass().isEmpty()) {
                    shelper = StyleHelper.create(Collections.EMPTY_LIST, 0, 0);
                    shelper.valueCache = new HashMap<Reference<StyleCacheKey>, List<StyleHelper.CacheEntry>>();
                } else  {
                    shelper = StyleHelper.create(styles, 0, 0);
                    shelper.valueCache = new HashMap<Reference<StyleCacheKey>, List<StyleHelper.CacheEntry>>();                    
                }
                return shelper;
            }
            
        };
                
        rect.getStyleClass().add("rect");
        rect.impl_setStyleMap(FXCollections.observableMap(styleMap));
        rect.impl_getStyleMap().addListener(new MapChangeListener<WritableValue, List<Style>>() {

            public void onChanged(MapChangeListener.Change<? extends WritableValue, ? extends List<Style>> change) {
                if (change.wasAdded()) {
                    List<Style> styles = change.getValueAdded();
                    for (Style style : styles) {
                        assert(decls.contains(style.getDeclaration()));
                        assert(sels.contains(style.getSelector()));
                        Object value = style.getDeclaration().parsedValue.convert(null);
                        WritableValue writable = change.getKey();
                        beans.add(writable);
                        assertEquals(writable.getValue(), value);
                        nchanges += 1;                        
                    }
                } if (change.wasRemoved()) {
                    WritableValue writable = change.getKey();
                    assert(beans.contains(writable));
                    nchanges -= 1;
                }
            }
        });
        
        rect.impl_processCSS(true);
        assertEquals(decls.size(), nchanges);

        rect.getStyleClass().clear();
        rect.impl_processCSS(true);
        // Nothing new should be added since there are no styles.
        // nchanges is decremented on remove, so it should be zero
        assertEquals(0, nchanges);
        assert(rect.impl_getStyleMap().isEmpty());
        
    }
}
