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

import com.sun.javafx.css.StyleHelper.StyleCacheBucket;
import com.sun.javafx.css.StyleHelper.StyleCacheKey;
import com.sun.javafx.css.converters.FontConverter;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.css.parser.CSSParser;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.paint.Color;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.Test;
import static org.junit.Assert.*;


public class Node_cssStyleMap_Test {
    
    public Node_cssStyleMap_Test() {
    }

    int nchanges = 0;
    int nadds = 0;
    int nremoves = 0;
    boolean disabled = false;
    Group group;
    Rectangle rect;
    Text text;    

    static List<CascadingStyle> createStyleList(List<Declaration> decls) {
        
        final List<CascadingStyle> styles = new ArrayList<CascadingStyle>();
        
        for (Declaration decl : decls) {
            styles.add(
                new CascadingStyle(
                    new Style(decl.rule.selectors.get(0), decl), 
                    PseudoClass.createStatesInstance(),
                    0, 
                    0
                )
            );
        }
        
        return styles;
    }
    
    static Map<String, List<CascadingStyle>> createStyleMap(List<CascadingStyle> styles) {
        
        final Map<String, List<CascadingStyle>> smap = 
            new HashMap<String, List<CascadingStyle>>();
        
        final int max = styles != null ? styles.size() : 0;
        for (int i=0; i<max; i++) {
            final CascadingStyle style = styles.get(i);
            final String property = style.getProperty();
            // This is carefully written to use the minimal amount of hashing.
            List<CascadingStyle> list = smap.get(property);
            if (list == null) {
                list = new ArrayList<CascadingStyle>(5);
                smap.put(property, list);
            }
            list.add(style);
        }
        return smap;
    }
    
    @Test @org.junit.Ignore
    public void testStyleMapTracksChanges() {
                
        final List<Declaration> declsNoState = new ArrayList<Declaration>();
        Collections.addAll(declsNoState, 
            new Declaration("-fx-fill", new ParsedValue<Color,Color>(Color.RED, null), false),
            new Declaration("-fx-stroke", new ParsedValue<Color,Color>(Color.YELLOW, null), false),
            new Declaration("-fx-stroke-width", new ParsedValue<ParsedValue<?,Size>,Double>(
                new ParsedValue<Size,Size>(new Size(3d, SizeUnits.PX), null), 
                SizeConverter.getInstance()), false)
        );
        
        
        final List<Selector> selsNoState = new ArrayList<Selector>();
        Collections.addAll(selsNoState, 
            Selector.createSelector(".rect")
        );
        
        Rule rule = new Rule(selsNoState, declsNoState);        
        
        Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(Origin.USER_AGENT);
        stylesheet.getRules().add(rule);
        
        final List<Declaration> declsDisabledState = new ArrayList<Declaration>();
        Collections.addAll(declsDisabledState, 
            new Declaration("-fx-fill", new ParsedValue<Color,Color>(Color.GRAY, null), false),
            new Declaration("-fx-stroke", new ParsedValue<Color,Color>(Color.DARKGRAY, null), false)
        );
        
        final List<Selector> selsDisabledState = new ArrayList<Selector>();
        Collections.addAll(selsDisabledState, 
            Selector.createSelector(".rect:disabled")
        );
        
        rule = new Rule(selsDisabledState, declsDisabledState);        
        stylesheet.getRules().add(rule);
        
        final List<CascadingStyle> stylesNoState = createStyleList(declsNoState);
        final List<CascadingStyle> stylesDisabledState = createStyleList(declsDisabledState);
        
        // add to this list on wasAdded, check bean on wasRemoved.
        final List<WritableValue> beans = new ArrayList<WritableValue>();
        
        Rectangle rect = new Rectangle(50,50);
        rect.getStyleClass().add("rect");
        rect.impl_setStyleMap(FXCollections.observableMap(new HashMap<WritableValue, List<Style>>()));
        rect.impl_getStyleMap().addListener(new MapChangeListener<WritableValue, List<Style>>() {

            public void onChanged(MapChangeListener.Change<? extends WritableValue, ? extends List<Style>> change) {

                if (change.wasAdded()) {
                    
                    List<Style> styles = change.getValueAdded();
                    for (Style style : styles) {

                        // stroke width comes from ".rect" even for disabled state.
                        if (disabled == false || "-fx-stroke-width".equals(style.getDeclaration().getProperty())) {
                            assertTrue(style.getDeclaration().toString(),declsNoState.contains(style.getDeclaration()));
                            assertTrue(style.getSelector().toString(),selsNoState.contains(style.getSelector()));
                        } else {
                            assertTrue(style.getDeclaration().toString(),declsDisabledState.contains(style.getDeclaration()));
                            assertTrue(style.getSelector().toString(),selsDisabledState.contains(style.getSelector()));                            
                        }
                        Object value = style.getDeclaration().parsedValue.convert(null);
                        WritableValue writable = change.getKey();
                        beans.add(writable);
                        assertEquals(writable.getValue(), value);
                        nadds += 1;                        
                    }
                    
                } if (change.wasRemoved()) {
                    WritableValue writable = change.getKey();
                    assert(beans.contains(writable));
                    nremoves += 1;
                }
            }
        });

        Group root = new Group();
        root.getChildren().add(rect);
        StyleManager.setDefaultUserAgentStylesheet(stylesheet);        
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        // The three no state styles should be applied
        assertEquals(3, nadds);
        assertEquals(0, nremoves);

        rect.setDisable(true);
        disabled = true;
        nadds = 0;
        nremoves = 0;
        
        Toolkit.getToolkit().firePulse();
        
        // The three no state styles should be removed and the 
        // two disabled state styles plus the stroke width style 
        // should be applied. 
        assertEquals(3, nadds);
        assertEquals(3, nremoves);
        
    }
    
    @Test @org.junit.Ignore
    public void testRT_21212() {

        final List<Declaration> rootDecls = new ArrayList<Declaration>();
        Collections.addAll(rootDecls, 
            new Declaration("-fx-font-size", new ParsedValue<ParsedValue<?,Size>,Double>(
                new ParsedValue<Size,Size>(new Size(12, SizeUnits.PX), null), 
                SizeConverter.getInstance()), false)
        );
        
        final List<Selector> rootSels = new ArrayList<Selector>();
        Collections.addAll(rootSels, 
            Selector.createSelector(".root")
        );
        
        Rule rootRule = new Rule(rootSels, rootDecls);        
        
        Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(Origin.USER_AGENT);
        stylesheet.getRules().add(rootRule);

        final List<CascadingStyle> rootStyles = createStyleList(rootDecls);
        final Map<String,List<CascadingStyle>> rootStyleMap = createStyleMap(rootStyles);
        final Map<StyleCacheKey, StyleCacheBucket> styleCache = 
            new HashMap<StyleHelper.StyleCacheKey, StyleHelper.StyleCacheBucket>();
        
        group = new Group();
        group.getStyleClass().add("root");
        
        
        final ParsedValue[] fontValues = new ParsedValue[] {
            new ParsedValue<String,String>("system", null),
            new ParsedValue<ParsedValue<?,Size>,Double>(
                new ParsedValue<Size,Size>(new Size(1.5, SizeUnits.EM), null),
                SizeConverter.getInstance()
            ), 
            null,
            null
        };
        final List<Declaration> textDecls = new ArrayList<Declaration>();
        Collections.addAll(textDecls, 
            new Declaration("-fx-font", new ParsedValue<ParsedValue[], Font>(
                fontValues, FontConverter.getInstance()), false)
        );
        
        final List<Selector> textSels = new ArrayList<Selector>();
        Collections.addAll(textSels, 
            Selector.createSelector(".text")
        );
        
        Rule textRule = new Rule(textSels, textDecls);        
        stylesheet.getRules().add(textRule);
                
        final List<CascadingStyle> styles = createStyleList(textDecls);
        final Map<String,List<CascadingStyle>> styleMap = createStyleMap(styles);
        final Map<String,List<CascadingStyle>> emptyMap = createStyleMap(null);

        text = new Text("HelloWorld");
        group.getChildren().add(text);

        final List<Declaration> expecteds = new ArrayList<Declaration>();
//        expecteds.addAll(rootDecls);
        expecteds.addAll(textDecls);
        text.getStyleClass().add("text");
        text.impl_setStyleMap(FXCollections.observableMap(new HashMap<WritableValue, List<Style>>()));
        text.impl_getStyleMap().addListener(new MapChangeListener<WritableValue, List<Style>>() {

            // a little different than the other tests since we should end up 
            // with font and font-size in the map and nothing else. After all 
            // the changes have been handled, the expecteds list should be empty.
            public void onChanged(MapChangeListener.Change<? extends WritableValue, ? extends List<Style>> change) {
                if (change.wasAdded()) {
                    List<Style> styles = change.getValueAdded();
                    for (Style style : styles) {
                        assertTrue(expecteds.contains(style.getDeclaration()));
                        expecteds.remove(style.getDeclaration());
                    }
                } 
            }
        });
             
        StyleManager.setDefaultUserAgentStylesheet(stylesheet);        
        Scene scene = new Scene(group);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        
        assertEquals(18, text.getFont().getSize(),0);
        assertTrue(Integer.toString(expecteds.size()), expecteds.isEmpty());

        text.getStyleClass().clear();

        Toolkit.getToolkit().firePulse();
        
        // PENDING RT-25002
//        assertEquals(12, text.getFont().getSize(),0);
//        assertTrue(text.impl_getStyleMap().toString(), text.impl_getStyleMap().isEmpty());
        
    } 
    
}
