/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.converters.FontConverter;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.tk.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.css.CssMetaData;
import javafx.css.ParsedValue;
import javafx.css.StyleOrigin;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import static org.junit.Assert.*;

import javafx.util.Pair;
import org.junit.Ignore;
import org.junit.Test;

public class Node_cssStyleMap_Test {
    
    public Node_cssStyleMap_Test() {
    }

    boolean disabled = false;

    private void checkFoundStyle(Property<?> property, Map<StyleableProperty<?>, List<Style>> map, List<Declaration> decls) {

        List<Style> styles = map.get(property);
        assert (styles != null && !styles.isEmpty());

        String pname = ((StyleableProperty<?>)property).getCssMetaData().getProperty();
        Declaration declaration = null;
        for(Declaration decl : decls) {
            if (pname.equals(decl.getProperty())) {
                declaration = decl;
                break;
            }
        }
        assertNotNull(pname, declaration);

        Style style = null;
        for(Style s : styles) {
            if (pname.equals(s.getDeclaration().getProperty())) {
                style = s;
                break;
            }
        }
        assertNotNull(pname, style);

        assert(style.getDeclaration() == declaration);

    }
    
    @Test
    public void testStyleMap() {

        final List<Declaration> declsNoState = new ArrayList<Declaration>();
        Collections.addAll(declsNoState,
            new Declaration("-fx-fill", new ParsedValueImpl<Color,Color>(Color.RED, null), false),
            new Declaration("-fx-stroke", new ParsedValueImpl<Color,Color>(Color.YELLOW, null), false),
            new Declaration("-fx-stroke-width", new ParsedValueImpl<ParsedValue<?,Size>,Number>(
                new ParsedValueImpl<Size,Size>(new Size(3d, SizeUnits.PX), null),
                SizeConverter.getInstance()), false)
        );


        final List<Selector> selsNoState = new ArrayList<Selector>();
        Collections.addAll(selsNoState,
            Selector.createSelector(".rect")
        );

        Rule rule = new Rule(selsNoState, declsNoState);

        Stylesheet stylesheet = new Stylesheet("testStyleMap");
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        stylesheet.getRules().add(rule);

        final List<Declaration> declsDisabledState = new ArrayList<Declaration>();
        Collections.addAll(declsDisabledState,
            new Declaration("-fx-fill", new ParsedValueImpl<Color,Color>(Color.GRAY, null), false),
            new Declaration("-fx-stroke", new ParsedValueImpl<Color,Color>(Color.DARKGRAY, null), false)
        );

        final List<Selector> selsDisabledState = new ArrayList<Selector>();
        Collections.addAll(selsDisabledState,
            Selector.createSelector(".rect:disabled")
        );

        rule = new Rule(selsDisabledState, declsDisabledState);
        stylesheet.getRules().add(rule);

        Rectangle rect = new Rectangle(50,50);
        rect.getStyleClass().add("rect");

        Group root = new Group();
        root.getChildren().add(rect);
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);
        Scene scene = new Scene(root);

        rect.applyCss();

        Map<StyleableProperty<?>, List<Style>> map = rect.impl_findStyles(null);
        assert (map != null && !map.isEmpty());

        checkFoundStyle(rect.fillProperty(), map, declsNoState);
        checkFoundStyle(rect.strokeProperty(), map, declsNoState);
        checkFoundStyle(rect.strokeWidthProperty(), map, declsNoState);

        rect.setDisable(true);
        rect.applyCss();

        map = rect.impl_findStyles(null);
        assert (map != null && !map.isEmpty());

        checkFoundStyle(rect.fillProperty(), map, declsDisabledState);
        checkFoundStyle(rect.strokeProperty(), map, declsDisabledState);
        checkFoundStyle(rect.strokeWidthProperty(), map, declsNoState);

    }

    @Test
    public void testStyleMapChildren() {

        final List<Declaration> declsNoState = new ArrayList<Declaration>();
        Collections.addAll(declsNoState,
                new Declaration("-fx-fill", new ParsedValueImpl<Color,Color>(Color.RED, null), false)
        );

        final List<Selector> selsNoState = new ArrayList<Selector>();
        Collections.addAll(selsNoState,
                Selector.createSelector(".rect")
        );

        Rule rule = new Rule(selsNoState, declsNoState);

        Stylesheet stylesheet = new Stylesheet("testStyleMapChildren");
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        stylesheet.getRules().add(rule);

        Rectangle rect = new Rectangle(50,50);
        rect.getStyleClass().add("rect");

        Group root = new Group();
        Group group = new Group();
        root.getChildren().add(group);
        group.getChildren().add(rect);
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);
        Scene scene = new Scene(root);

        root.applyCss();

        // Even though root and group have no styles, the styles for rect should still be found
        Map<StyleableProperty<?>, List<Style>> map = root.impl_findStyles(null);
        assert (map != null && !map.isEmpty());

        checkFoundStyle(rect.fillProperty(), map, declsNoState);

    }

    @Test
    public void testRT_21212() {

        final List<Declaration> rootDecls = new ArrayList<Declaration>();
        Collections.addAll(rootDecls, 
            new Declaration("-fx-font-size", new ParsedValueImpl<ParsedValue<?,Size>,Number>(
                new ParsedValueImpl<Size,Size>(new Size(12, SizeUnits.PX), null), 
                SizeConverter.getInstance()), false)
        );
        
        final List<Selector> rootSels = new ArrayList<Selector>();
        Collections.addAll(rootSels, 
            Selector.createSelector(".root")
        );
        
        Rule rootRule = new Rule(rootSels, rootDecls);        
        
        Stylesheet stylesheet = new Stylesheet("testRT_21212");
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        stylesheet.getRules().add(rootRule);

        Group group = new Group();
        group.getStyleClass().add("root");
        
        
        final ParsedValue[] fontValues = new ParsedValue[] {
            new ParsedValueImpl<String,String>("system", null),
            new ParsedValueImpl<ParsedValue<?,Size>,Number>(
                new ParsedValueImpl<Size,Size>(new Size(1.5, SizeUnits.EM), null),
                SizeConverter.getInstance()
            ), 
            null,
            null
        };
        final List<Declaration> textDecls = new ArrayList<Declaration>();
        Collections.addAll(textDecls, 
            new Declaration("-fx-font", new ParsedValueImpl<ParsedValue[], Font>(
                fontValues, FontConverter.getInstance()), false)
        );
        
        final List<Selector> textSels = new ArrayList<Selector>();
        Collections.addAll(textSels, 
            Selector.createSelector(".text")
        );
        
        Rule textRule = new Rule(textSels, textDecls);        
        stylesheet.getRules().add(textRule);
                
        Text text = new Text("HelloWorld");
        text.getStyleClass().add("text");
        group.getChildren().add(text);

        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);
        Scene scene = new Scene(group);

        text.applyCss();

        Map<StyleableProperty<?>, List<Style>> map = text.impl_findStyles(null);
        assert (map != null && !map.isEmpty());

        checkFoundStyle(text.fontProperty(), map, textDecls);

    }

    boolean containsProperty(CssMetaData key, Map<String,List<CascadingStyle>> map) {

        if (map.containsKey(key)) return true;
        List<CssMetaData> subProperties = key.getSubProperties();
        if (subProperties != null && !subProperties.isEmpty()) {
            for (CssMetaData subKey: subProperties) {
                if (map.containsKey(subKey)) return true;
            }
        }
        return false;
    }

    @Test
    public void testRT_34799() {

        Stylesheet stylesheet = new Stylesheet("testRT_34799");
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);

        final List<Declaration> txtDecls = new ArrayList<Declaration>();
        Collections.addAll(txtDecls,
                new Declaration("-fx-fill", new ParsedValueImpl<Color,Color>(Color.RED, null), false)
        );

        final List<Selector> textSels = new ArrayList<Selector>();
        Collections.addAll(textSels,
                Selector.createSelector(".rt-34799")
        );

        Rule txtRules = new Rule(textSels, txtDecls);
        stylesheet.getRules().add(txtRules);

        final List<Style> expectedStyles = new ArrayList<>();
        for (Rule rule : stylesheet.getRules()) {
            for (Selector selector : rule.getSelectors()) {
                for (Declaration declaration : rule.getUnobservedDeclarationList()) {
                    expectedStyles.add(
                            new Style(selector, declaration)
                    );
                }
            }
        }

        Text text = new Text("HelloWorld");
        text.getStyleClass().add("rt-34799");

        Group group = new Group();
        group.getStyleClass().add("root");

        group.getChildren().add(text);

        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);
        Scene scene = new Scene(group);

        group.applyCss(); // TODO: force StyleHelper to be created, remove pending RT-34812

        int nExpected = expectedStyles.size();
        assert(nExpected > 0);

        for(CssMetaData cssMetaData : text.getCssMetaData()) {
            List<Style> styles = Node.impl_getMatchingStyles(cssMetaData, text);
            if (styles != null && !styles.isEmpty()) {
                assertTrue(expectedStyles.containsAll(styles));
                assertTrue(styles.containsAll(expectedStyles));
                nExpected -= 1;
            }
        }

        assertEquals(nExpected, 0);

    }

}
