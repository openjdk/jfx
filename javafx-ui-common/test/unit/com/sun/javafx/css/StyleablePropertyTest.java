/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.*;
import java.util.List;

import javafx.scene.Node;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.FontConverter;
import com.sun.javafx.css.parser.CSSParser;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.sg.PGNode;
import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.WritableValue;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;


public class StyleablePropertyTest {

    public StyleablePropertyTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    private static StyleableProperty get(List<StyleableProperty> list, String prop) {
        for (StyleableProperty styleable : list) {
            if (prop.equals(styleable.getProperty())) return styleable;
        }
        return null;
    }
    /**
     * Test of getStyleables method, of class StyleableProperty.
     */
    @Test
    public void testGetStyleables_Node() {
        Node node = new TestNode();
        List<StyleableProperty> expResult = TestNode.impl_CSS_STYLEABLES();
        List result = StyleableProperty.getStyleables(node);
        assertEquals(expResult, result);
    }

    /**
     * Test of getStyleables method, of class StyleableProperty.
     */
    @Test
    public void testGetStyleables_Styleable() {
        Node node = new TestNode();
        Styleable styleable = node.impl_getStyleable();
        assertNotNull(styleable);
        List<StyleableProperty> expResult = TestNode.impl_CSS_STYLEABLES();
        List result = StyleableProperty.getStyleables(styleable);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetJavafxBeansProperty() {
            TestNode testNode = new TestNode();
            WritableValue prop = TestNodeBase.StyleableProperties.TEST.getWritableValue(testNode);
            assert(prop != null);
            StyleableProperty result = ((Property)prop).getStyleableProperty();
            assert(result == TestNodeBase.StyleableProperties.TEST);
    }

    /**
     * Test of getProperty method, of class StyleableProperty.
     */
    @Test
    public void testGetProperty() {
        
        String expResult = "-fx-test";
        String result = TestNodeBase.StyleableProperties.TEST.getProperty();
        assertEquals(expResult, result);
    }

    /**
     * Test of getConverter method, of class StyleableProperty.
     */
    @Test
    public void testGetConverter() {
        
        StyleConverter expResult = BooleanConverter.getInstance();
        StyleConverter result = TestNodeBase.StyleableProperties.TEST.getConverter();
        assertEquals(expResult, result);
    }

    /**
     * Test of getInitialValue method, of class StyleableProperty.
     */
    @Test
    public void testGetInitialValue() {
        
        TestNode testNode = new TestNode();
        Double expResult = testNode.getXyzzy();
        Double result = (Double)TestNode.StyleableProperties.XYZZY.getInitialValue(testNode);
        assertEquals(expResult, result);

    }

    /**
     * Test of getSubProperties method, of class StyleableProperty.
     */
    @Test
    public void testGetSubProperties() {
        
        StyleableProperty<TestNode,Font> fontProp = 
                new StyleableProperty.FONT<TestNode>("-fx-font", Font.getDefault()) {

            @Override
            public boolean isSettable(TestNode n) {
                return true;
            }

            @Override
            public WritableValue<Font> getWritableValue(TestNode n) {
                return null;
            }
        };
        
        List<StyleableProperty> list = fontProp.getSubProperties();
        assertNotNull(list);

    }

    /**
     * Test of isInherits method, of class StyleableProperty.
     */
    @Test
    public void testIsInherits() {

        boolean expResult = false;
        boolean result = TestNode.StyleableProperties.XYZZY.isInherits();
        assertEquals(expResult, result);

    }

    /**
     * Test of toString method, of class StyleableProperty.
     */
    @Test
    public void testToString() {

        StyleableProperty<TestNode,Font> fontProp = 
                new StyleableProperty.FONT<TestNode>("-fx-font", Font.getDefault()) {

            @Override
            public boolean isSettable(TestNode n) {
                return true;
            }

            @Override
            public WritableValue<Font> getWritableValue(TestNode n) {
                return null;
            }
        };
        
        String string = fontProp.toString();
        assertNotNull(string);
        
    }

    /**
     * Test of equals method, of class StyleableProperty.
     */
    @Test
    public void testEquals() {
        TestNode testNode = new TestNode();
        Node node = new Node() {

            @Override
            protected PGNode impl_createPGNode() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public BaseBounds impl_computeGeomBounds(BaseBounds bb, BaseTransform bt) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            protected boolean impl_computeContains(double d, double d1) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Object impl_processMXNode(MXNodeAlgorithm mxna, MXNodeAlgorithmContext mxnac) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
        
        StyleableProperty testNodeOpacity = get(TestNode.impl_CSS_STYLEABLES(), "-fx-opacity");
        StyleableProperty nodeOpacity = get(Node.impl_CSS_STYLEABLES(), "-fx-opacity");
        
        assertTrue(testNodeOpacity.equals(nodeOpacity));
    }

    /**
     * Helper for testGetMatchingStyles
     */
    List<CascadingStyle> match(Node node, Stylesheet stylesheet) {
        
        List<CascadingStyle> styles = new ArrayList<CascadingStyle>();

        int ord = 0;
        for (Rule rule : stylesheet.getRules()) {
            final List<Match> matches = rule.matches(node);
            if (matches == null || matches.isEmpty()) continue;
            for (Match match : matches) {
                if (match == null) continue;
                for (Declaration declaration : rule.getDeclarations()) {
                    styles.add(
                        new CascadingStyle(
                            new Style(match.selector, declaration), 
                            match.pseudoclasses,
                            match.specificity, 
                            ord++
                        )
                    );
                }
            }
        }
        
        return styles;
    }
    
    Group createGroup(final CascadingStyle... styles ) {
    
        final List<CascadingStyle> cascadingStyles = new ArrayList<CascadingStyle>();
        Collections.addAll(cascadingStyles, styles);
        
        final Group group = new Group() {
            // I'm bypassing StyleManager by creating StyleHelper directly. 
            StyleHelper shelper = null;
            
            @Override
            public Reference<StyleHelper.StyleCacheKey> impl_getStyleCacheKey() {
                return shelper.createStyleCacheKey(this);
            }

            @Override public StyleHelper impl_getStyleHelper() {
                if (shelper == null) shelper = impl_createStyleHelper();
                return shelper;
            }
                        
            @Override
            public StyleHelper impl_createStyleHelper() {
                shelper = StyleHelper.create(cascadingStyles, 0, 0);
                shelper.valueCache = new HashMap<StyleHelper.StyleCacheKey, List<StyleHelper.CacheEntry>>();
                shelper.keysInUse = new HashMap<StyleHelper.StyleCacheKey, Reference<StyleHelper.StyleCacheKey>>();
                return shelper;
            }
            
        };
        group.getStyleClass().add("root");
        
        return group;
    }
    
    Rectangle createRectangle(final CascadingStyle... styles ) {
    
        final List<CascadingStyle> cascadingStyles = new ArrayList<CascadingStyle>();
        Collections.addAll(cascadingStyles, styles);
        
        Rectangle rectangle = new Rectangle() {
            // I'm bypassing StyleManager by creating StyleHelper directly. 
            StyleHelper shelper = null;
            
            @Override
            public Reference<StyleHelper.StyleCacheKey> impl_getStyleCacheKey() {
                return shelper.createStyleCacheKey(this);
            }
            
            @Override public StyleHelper impl_getStyleHelper() {
                if (shelper == null) shelper = impl_createStyleHelper();
                return shelper;
            }
            
            @Override
            public StyleHelper impl_createStyleHelper() {
                shelper = StyleHelper.create(cascadingStyles, 0, 1);
                shelper.valueCache = new HashMap<StyleHelper.StyleCacheKey, List<StyleHelper.CacheEntry>>();
                shelper.keysInUse = new HashMap<StyleHelper.StyleCacheKey, Reference<StyleHelper.StyleCacheKey>>();
                return shelper;
            }
            
        };
        rectangle.getStyleClass().add("rect");

        return rectangle;
    }

    static class RectangleWithFont extends Rectangle {
    
        final List<CascadingStyle> cascadingStyles = new ArrayList<CascadingStyle>();
        
        // I'm bypassing StyleManager by creating StyleHelper directly. 
        StyleHelper shelper = null;
        
        RectangleWithFont(final CascadingStyle... styles) {
            super();
            getStyleClass().add("rect");
            Collections.addAll(cascadingStyles, styles);
        }    
        
        @Override
        public Reference<StyleHelper.StyleCacheKey> impl_getStyleCacheKey() {
            return shelper.createStyleCacheKey(this);
        }

        @Override public StyleHelper impl_getStyleHelper() {
            if (shelper == null) shelper = impl_createStyleHelper();
            return shelper;
        }

        @Override
        public StyleHelper impl_createStyleHelper() {
            shelper = StyleHelper.create(cascadingStyles, 0, 1);
            shelper.valueCache = new HashMap<StyleHelper.StyleCacheKey, List<StyleHelper.CacheEntry>>();
            shelper.keysInUse = new HashMap<StyleHelper.StyleCacheKey, Reference<StyleHelper.StyleCacheKey>>();
            return shelper;
        }
            
        ObjectProperty<Font> font = new StyleableObjectProperty<Font>() {

            @Override
            public Object getBean() {
                return RectangleWithFont.this;
            }

            @Override
            public String getName() {
                return "font";
            }

            @Override
            public StyleableProperty getStyleableProperty() {
                return FONT;
            }

        };

        private static final StyleableProperty<RectangleWithFont,Font> FONT =
            new StyleableProperty.FONT<RectangleWithFont>("-fx-font", Font.getDefault()) {

                @Override
                public boolean isSettable(RectangleWithFont node) {
                    return true;
                }

                @Override
                public WritableValue<Font> getWritableValue(RectangleWithFont node) {
                    return node.font;
                }
            };
            
        public static List<StyleableProperty> impl_CSS_STYLEABLES() {
            List<StyleableProperty> styleables = new ArrayList<StyleableProperty>();
            styleables.addAll(Rectangle.impl_CSS_STYLEABLES());
            styleables.add(FONT);
            return styleables;
        }
        
        @Override public List<StyleableProperty> impl_getStyleableProperties() {
            return impl_CSS_STYLEABLES();
        }
            
    }    
    
    static int ord = 0;
    static CascadingStyle createCascadingStyle(Selector selector, Declaration declaration) {
        List<String> pseudoclasses = null;
        if (selector instanceof SimpleSelector) {
            pseudoclasses = ((SimpleSelector)selector).getPseudoclasses();
        } else {
            pseudoclasses = new ArrayList(); 
            for (SimpleSelector sel : ((CompoundSelector)selector).getSelectors()) {
                for (String pclass : sel.getPseudoclasses()) {
                    if (pseudoclasses.contains(pclass) == false) 
                        pseudoclasses.add(pclass);
                }
            }
        }
        return new CascadingStyle(
            new Style(selector, declaration),
            pseudoclasses,
            0,
            ord++
        );
    }
        
    @Test
    public void testGetMatchingStyles() {

        
        final Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(Stylesheet.Origin.USER_AGENT);
        
        final List<Rule> rules = stylesheet.getRules();
        
        List<String> rootStyleClass = new ArrayList<String>();
        rootStyleClass.add("root");
        
        List<String> rectStyleClass = new ArrayList<String>();
        rootStyleClass.add("rect");
        
        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        Selector root = new SimpleSelector("*", rootStyleClass, null, null);
        
        ParsedValue fxBaseValue = CSSParser.getInstance().parseExpr("-fx-base", "red");
        Declaration fxBase = new Declaration("-fx-base", fxBaseValue, false);

        ParsedValue<String,String> fxColorValue = new ParsedValue<String,String>(fxBase.getProperty(), null, true);
        Declaration fxColor = new Declaration("-fx-color", fxColorValue, false);
        
        List<Selector> selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, root); 
        
        List<Declaration> declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxBase, fxColor);
        
        Rule baseRule = new Rule(selectors, declarations);
        rules.add(baseRule);
        
        //
        // .rect { -fx-fill: -fx-color; }
        //
        Selector rect = new SimpleSelector("*", rectStyleClass, null, null);
        
        ParsedValue fxFillValue = CSSParser.getInstance().parseExpr("-fx-fill", "-fx-color");
        Declaration fxFill = new Declaration("-fx-fill", fxFillValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rect); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFill);
        
        Rule rectRule = new Rule(selectors, declarations);
        rules.add(rectRule);
        
        // .rect:hover { -fx-fill: yellow; }
        List<String> pseudoclasses = new ArrayList<String>();
        pseudoclasses.add("hover");
        
        Selector rectHover = new SimpleSelector("*", rectStyleClass, pseudoclasses, null);
        
        ParsedValue<Color,Color> fxFillHoverValue = new ParsedValue<Color,Color>(Color.YELLOW, null);        
        Declaration fxFillHover = new Declaration("-fx-fill", fxFillHoverValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rectHover); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFillHover);
        
        Rule rectHoverRule = new Rule(selectors, declarations);
        rules.add(rectHoverRule);
                
        final Group group = createGroup(
            createCascadingStyle(root, fxBase), 
            createCascadingStyle(root, fxColor)
        );
        
        final Rectangle rectangle = createRectangle(
            createCascadingStyle(rect, fxFill), 
            createCascadingStyle(rectHover, fxFillHover)                
        );
        
        List<Style> expecteds = new ArrayList<Style>();
        Collections.addAll(expecteds, 
                new Style(root, fxBase), 
                new Style(root, fxColor), 
                new Style(rect, fxFill),
                new Style(rectHover, fxFillHover)
        );
        
        group.getChildren().add(rectangle);        
        group.impl_processCSS(true);
        
        final StyleableProperty FILL = get(StyleableProperty.getStyleables(rectangle), "-fx-fill");
        final List<Style> actuals = FILL.getMatchingStyles(rectangle);

//        System.out.println("matchingStyles: " + matchingStyles);
//        System.out.println("expecteds: " + expecteds);
//        System.out.println("actuals: " + actuals);
        
        assertEquals(expecteds.size(), actuals.size(), 0);
        
        for (Style style : expecteds) {
            if (!actuals.remove(style)) fail();
        }
        assertTrue(actuals.isEmpty());
    }

    @Test
    public void testGetMatchingStylesWithInlineStyleOnParent() {

        
        final Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(Stylesheet.Origin.USER_AGENT);
        
        final List<Rule> rules = stylesheet.getRules();
        
        List<String> rootStyleClass = new ArrayList<String>();
        rootStyleClass.add("root");
        
        List<String> rectStyleClass = new ArrayList<String>();
        rootStyleClass.add("rect");
        
        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        Selector root = new SimpleSelector("*", rootStyleClass, null, null);
        
        ParsedValue fxBaseValue = CSSParser.getInstance().parseExpr("-fx-base", "red");
        Declaration fxBase = new Declaration("-fx-base", fxBaseValue, false);

        ParsedValue<String,String> fxColorValue = new ParsedValue<String,String>(fxBase.getProperty(), null, true);
        Declaration fxColor = new Declaration("-fx-color", fxColorValue, false);
        
        List<Selector> selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, root); 
        
        List<Declaration> declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxBase, fxColor);
        
        Rule baseRule = new Rule(selectors, declarations);
        rules.add(baseRule);
        
        //
        // .rect { -fx-fill: -fx-color; }
        //
        Selector rect = new SimpleSelector("*", rectStyleClass, null, null);
        
        ParsedValue fxFillValue = CSSParser.getInstance().parseExpr("-fx-fill", "-fx-color");
        Declaration fxFill = new Declaration("-fx-fill", fxFillValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rect); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFill);
        
        Rule rectRule = new Rule(selectors, declarations);
        rules.add(rectRule);
        
        // .rect:hover { -fx-fill: yellow; }
        List<String> pseudoclasses = new ArrayList<String>();
        pseudoclasses.add("hover");
        
        Selector rectHover = new SimpleSelector("*", rectStyleClass, pseudoclasses, null);
        
        ParsedValue<Color,Color> fxFillHoverValue = new ParsedValue<Color,Color>(Color.YELLOW, null);        
        Declaration fxFillHover = new Declaration("-fx-fill", fxFillHoverValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rectHover); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFillHover);
        
        Rule rectHoverRule = new Rule(selectors, declarations);
        rules.add(rectHoverRule);
                
        final Group group = createGroup(
            createCascadingStyle(root, fxBase), 
            createCascadingStyle(root, fxColor)
        );
        group.setStyle("-fx-base: green;");
        
        final Rectangle rectangle = createRectangle(
            createCascadingStyle(rect, fxFill), 
            createCascadingStyle(rectHover, fxFillHover)                
        );
        
        List<Style> expecteds = new ArrayList<Style>();
        Collections.addAll(expecteds, 
                new Style(SimpleSelector.getUniversalSelector(), 
                          new Declaration("-fx-base", new ParsedValue<Color,Color>(Color.GREEN, null), false)), 
                new Style(root, fxBase),
                new Style(root, fxColor), 
                new Style(rect, fxFill),
                new Style(rectHover, fxFillHover)
        );
        
        group.getChildren().add(rectangle);        
        group.impl_processCSS(true);
        
        final StyleableProperty FILL = get(StyleableProperty.getStyleables(rectangle), "-fx-fill");
        final List<Style> actuals = FILL.getMatchingStyles(rectangle);

//        System.out.println("matchingStyles: " + matchingStyles);
//        System.out.println("expecteds: " + expecteds);
//        System.out.println("actuals: " + actuals);
        
        assertEquals(expecteds.size(), actuals.size(), 0);
        
        // inline style should be first
        assertEquals(expecteds.get(0), actuals.get(0));
        
        for (Style style : expecteds) {
            if (!actuals.remove(style)) fail();
        }
        assertTrue(actuals.isEmpty());
    }

    @Test
    public void testGetMatchingStylesWithInlineStyleOnLeaf() {

        
        final Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(Stylesheet.Origin.USER_AGENT);
        
        final List<Rule> rules = stylesheet.getRules();
        
        List<String> rootStyleClass = new ArrayList<String>();
        rootStyleClass.add("root");
        
        List<String> rectStyleClass = new ArrayList<String>();
        rootStyleClass.add("rect");
        
        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        Selector root = new SimpleSelector("*", rootStyleClass, null, null);
        
        ParsedValue fxBaseValue = CSSParser.getInstance().parseExpr("-fx-base", "red");
        Declaration fxBase = new Declaration("-fx-base", fxBaseValue, false);

        ParsedValue<String,String> fxColorValue = new ParsedValue<String,String>(fxBase.getProperty(), null, true);
        Declaration fxColor = new Declaration("-fx-color", fxColorValue, false);
        
        List<Selector> selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, root); 
        
        List<Declaration> declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxBase, fxColor);
        
        Rule baseRule = new Rule(selectors, declarations);
        rules.add(baseRule);
        
        //
        // .rect { -fx-fill: -fx-color; }
        //
        Selector rect = new SimpleSelector("*", rectStyleClass, null, null);
        
        ParsedValue fxFillValue = CSSParser.getInstance().parseExpr("-fx-fill", "-fx-color");
        Declaration fxFill = new Declaration("-fx-fill", fxFillValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rect); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFill);
        
        Rule rectRule = new Rule(selectors, declarations);
        rules.add(rectRule);
        
        // .rect:hover { -fx-fill: yellow; }
        List<String> pseudoclasses = new ArrayList<String>();
        pseudoclasses.add("hover");
        
        Selector rectHover = new SimpleSelector("*", rectStyleClass, pseudoclasses, null);
        
        ParsedValue<Color,Color> fxFillHoverValue = new ParsedValue<Color,Color>(Color.YELLOW, null);        
        Declaration fxFillHover = new Declaration("-fx-fill", fxFillHoverValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rectHover); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFillHover);
        
        Rule rectHoverRule = new Rule(selectors, declarations);
        rules.add(rectHoverRule);
                
        final Group group = createGroup(
            createCascadingStyle(root, fxBase), 
            createCascadingStyle(root, fxColor)
        );
        
        final Rectangle rectangle = createRectangle(
            createCascadingStyle(rect, fxFill), 
            createCascadingStyle(rectHover, fxFillHover)                
        );
        rectangle.setStyle("-fx-base: green;");
        
        List<Style> expecteds = new ArrayList<Style>();
        Collections.addAll(expecteds, 
                new Style(SimpleSelector.getUniversalSelector(), 
                          new Declaration("-fx-base", new ParsedValue<Color,Color>(Color.GREEN, null), false)), 
                new Style(root, fxBase),
                new Style(root, fxColor), 
                new Style(rect, fxFill),
                new Style(rectHover, fxFillHover)
        );
        
        group.getChildren().add(rectangle);        
        group.impl_processCSS(true);
        
        final StyleableProperty FILL = get(StyleableProperty.getStyleables(rectangle), "-fx-fill");
        final List<Style> actuals = FILL.getMatchingStyles(rectangle);

//        System.out.println("matchingStyles: " + matchingStyles);
//        System.out.println("expecteds: " + expecteds);
//        System.out.println("actuals: " + actuals);

        assertEquals(expecteds.size(), actuals.size(), 0);
        
        // inline style should be first
        assertEquals(expecteds.get(0), actuals.get(0));
        
        for (Style style : expecteds) {
            if (!actuals.remove(style)) fail();
        }
        assertTrue(actuals.isEmpty());
    }   
    
    @Test
    public void testGetMatchingStylesWithInlineStyleOnRootAndLeaf() {

        
        final Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(Stylesheet.Origin.USER_AGENT);
        
        final List<Rule> rules = stylesheet.getRules();
        
        List<String> rootStyleClass = new ArrayList<String>();
        rootStyleClass.add("root");
        
        List<String> rectStyleClass = new ArrayList<String>();
        rootStyleClass.add("rect");
        
        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        Selector root = new SimpleSelector("*", rootStyleClass, null, null);
        
        ParsedValue fxBaseValue = CSSParser.getInstance().parseExpr("-fx-base", "red");
        Declaration fxBase = new Declaration("-fx-base", fxBaseValue, false);

        ParsedValue<String,String> fxColorValue = new ParsedValue<String,String>(fxBase.getProperty(), null, true);
        Declaration fxColor = new Declaration("-fx-color", fxColorValue, false);
        
        List<Selector> selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, root); 
        
        List<Declaration> declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxBase, fxColor);
        
        Rule baseRule = new Rule(selectors, declarations);
        rules.add(baseRule);
        
        //
        // .rect { -fx-fill: -fx-color; }
        //
        Selector rect = new SimpleSelector("*", rectStyleClass, null, null);
        
        ParsedValue fxFillValue = CSSParser.getInstance().parseExpr("-fx-fill", "-fx-color");
        Declaration fxFill = new Declaration("-fx-fill", fxFillValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rect); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFill);
        
        Rule rectRule = new Rule(selectors, declarations);
        rules.add(rectRule);
        
        // .rect:hover { -fx-fill: yellow; }
        List<String> pseudoclasses = new ArrayList<String>();
        pseudoclasses.add("hover");
        
        Selector rectHover = new SimpleSelector("*", rectStyleClass, pseudoclasses, null);
        
        ParsedValue<Color,Color> fxFillHoverValue = new ParsedValue<Color,Color>(Color.YELLOW, null);        
        Declaration fxFillHover = new Declaration("-fx-fill", fxFillHoverValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rectHover); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFillHover);
        
        Rule rectHoverRule = new Rule(selectors, declarations);
        rules.add(rectHoverRule);
                
        final Group group = createGroup(
            createCascadingStyle(root, fxBase), 
            createCascadingStyle(root, fxColor)
        );
        group.setStyle("-fx-color: yellow;");
        
        final Rectangle rectangle = createRectangle(
            createCascadingStyle(rect, fxFill), 
            createCascadingStyle(rectHover, fxFillHover)                
        );
        rectangle.setStyle("-fx-base: green;");
        
        List<Style> expecteds = new ArrayList<Style>();
        Collections.addAll(expecteds, 
                new Style(SimpleSelector.getUniversalSelector(), 
                          new Declaration("-fx-color", new ParsedValue<Color,Color>(Color.YELLOW, null), false)), 
                new Style(SimpleSelector.getUniversalSelector(), 
                          new Declaration("-fx-base", new ParsedValue<Color,Color>(Color.GREEN, null), false)), 
                new Style(root, fxBase),
                new Style(root, fxColor), 
                new Style(rect, fxFill),
                new Style(rectHover, fxFillHover)
        );
        
        group.getChildren().add(rectangle);        
        group.impl_processCSS(true);
        
        final StyleableProperty FILL = get(StyleableProperty.getStyleables(rectangle), "-fx-fill");
        final List<Style> actuals = FILL.getMatchingStyles(rectangle);

//        System.out.println("matchingStyles: " + matchingStyles);
//        System.out.println("expecteds: " + expecteds);
//        System.out.println("actuals: " + actuals);

        assertEquals(expecteds.size(), actuals.size(), 0);
        
        // inline style should be first
        assertEquals(expecteds.get(0), actuals.get(0));
        
        for (Style style : expecteds) {
            if (!actuals.remove(style)) fail();
        }
        assertTrue(actuals.isEmpty());
    } 

    @Test
    public void testGetMatchingStylesShouldNotReturnAncestorPropertyIfNotInherited() {

        
        final Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(Stylesheet.Origin.USER_AGENT);
        
        final List<Rule> rules = stylesheet.getRules();
        
        List<String> rootStyleClass = new ArrayList<String>();
        rootStyleClass.add("root");
        
        List<String> rectStyleClass = new ArrayList<String>();
        rootStyleClass.add("rect");
        
        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        Selector root = new SimpleSelector("*", rootStyleClass, null, null);
        
        ParsedValue fxBaseValue = CSSParser.getInstance().parseExpr("-fx-base", "red");
        Declaration fxBase = new Declaration("-fx-base", fxBaseValue, false);

        ParsedValue<String,String> fxColorValue = new ParsedValue<String,String>(fxBase.getProperty(), null, true);
        Declaration fxColor = new Declaration("-fx-color", fxColorValue, false);

        ParsedValue<Color,Color> fxFillShouldNotMatchValue = new ParsedValue<Color,Color>(Color.RED, null);        
        Declaration fxFillShouldNotMatch = new Declaration("-fx-fill", fxFillShouldNotMatchValue, false);
        
        List<Selector> selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, root); 
        
        List<Declaration> declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxBase, fxColor, fxFillShouldNotMatch);
        
        Rule baseRule = new Rule(selectors, declarations);
        rules.add(baseRule);
        
        //
        // .rect { -fx-fill: -fx-color; }
        //
        Selector rect = new SimpleSelector("*", rectStyleClass, null, null);
        
        ParsedValue fxFillValue = CSSParser.getInstance().parseExpr("-fx-fill", "-fx-color");
        Declaration fxFill = new Declaration("-fx-fill", fxFillValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rect); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFill);
        
        Rule rectRule = new Rule(selectors, declarations);
        rules.add(rectRule);
        
        // .rect:hover { -fx-fill: yellow; }
        List<String> pseudoclasses = new ArrayList<String>();
        pseudoclasses.add("hover");
        
        Selector rectHover = new SimpleSelector("*", rectStyleClass, pseudoclasses, null);
        
        ParsedValue<Color,Color> fxFillHoverValue = new ParsedValue<Color,Color>(Color.YELLOW, null);        
        Declaration fxFillHover = new Declaration("-fx-fill", fxFillHoverValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rectHover); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFillHover);
        
        Rule rectHoverRule = new Rule(selectors, declarations);
        rules.add(rectHoverRule);
                
        final Group group = createGroup(
            createCascadingStyle(root, fxBase), 
            createCascadingStyle(root, fxColor),
            createCascadingStyle(root, fxFillShouldNotMatch)
        );
        
        final Rectangle rectangle = createRectangle(
            createCascadingStyle(rect, fxFill), 
            createCascadingStyle(rectHover, fxFillHover)                
        );
        
        List<Style> expecteds = new ArrayList<Style>();
        Collections.addAll(expecteds, 
                new Style(root, fxBase),
                new Style(root, fxColor), 
                new Style(rect, fxFill),
                new Style(rectHover, fxFillHover)
        );
        
        group.getChildren().add(rectangle);        
        group.impl_processCSS(true);
        
        final StyleableProperty FILL = get(StyleableProperty.getStyleables(rectangle), "-fx-fill");
        final List<Style> actuals = FILL.getMatchingStyles(rectangle);

//        System.out.println("matchingStyles: " + matchingStyles);
//        System.out.println("expecteds: " + expecteds);
//        System.out.println("actuals: " + actuals);

        assertEquals(expecteds.size(), actuals.size(), 0);
                
        for (Style style : expecteds) {
            if (!actuals.remove(style)) fail();
        }
        assertTrue(actuals.isEmpty());
    } 


    @Test
    public void testGetMatchingStylesShouldNotReturnInlineAncestorPropertyIfNotInherited() {

        
        final Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(Stylesheet.Origin.USER_AGENT);
        
        final List<Rule> rules = stylesheet.getRules();
        
        List<String> rootStyleClass = new ArrayList<String>();
        rootStyleClass.add("root");
        
        List<String> rectStyleClass = new ArrayList<String>();
        rootStyleClass.add("rect");
        
        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        Selector root = new SimpleSelector("*", rootStyleClass, null, null);
        
        ParsedValue fxBaseValue = CSSParser.getInstance().parseExpr("-fx-base", "red");
        Declaration fxBase = new Declaration("-fx-base", fxBaseValue, false);

        ParsedValue<String,String> fxColorValue = new ParsedValue<String,String>(fxBase.getProperty(), null, true);
        Declaration fxColor = new Declaration("-fx-color", fxColorValue, false);

        ParsedValue<Color,Color> fxFillShouldNotMatchValue = new ParsedValue<Color,Color>(Color.RED, null);        
        Declaration fxFillShouldNotMatch = new Declaration("-fx-fill", fxFillShouldNotMatchValue, false);
        
        List<Selector> selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, root); 
        
        List<Declaration> declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxBase, fxColor, fxFillShouldNotMatch);
        
        Rule baseRule = new Rule(selectors, declarations);
        rules.add(baseRule);
        
        //
        // .rect { -fx-fill: -fx-color; }
        //
        Selector rect = new SimpleSelector("*", rectStyleClass, null, null);
        
        ParsedValue fxFillValue = CSSParser.getInstance().parseExpr("-fx-fill", "-fx-color");
        Declaration fxFill = new Declaration("-fx-fill", fxFillValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rect); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFill);
        
        Rule rectRule = new Rule(selectors, declarations);
        rules.add(rectRule);
        
        // .rect:hover { -fx-fill: yellow; }
        List<String> pseudoclasses = new ArrayList<String>();
        pseudoclasses.add("hover");
        
        Selector rectHover = new SimpleSelector("*", rectStyleClass, pseudoclasses, null);
        
        ParsedValue<Color,Color> fxFillHoverValue = new ParsedValue<Color,Color>(Color.YELLOW, null);        
        Declaration fxFillHover = new Declaration("-fx-fill", fxFillHoverValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rectHover); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFillHover);
        
        Rule rectHoverRule = new Rule(selectors, declarations);
        rules.add(rectHoverRule);
                
        final Group group = createGroup(
            createCascadingStyle(root, fxBase), 
            createCascadingStyle(root, fxColor)
        );
        group.setStyle("-fx-fill: black;");
        
        final Rectangle rectangle = createRectangle(
            createCascadingStyle(rect, fxFill), 
            createCascadingStyle(rectHover, fxFillHover)                
        );
        
        List<Style> expecteds = new ArrayList<Style>();
        Collections.addAll(expecteds, 
                new Style(root, fxBase),
                new Style(root, fxColor), 
                new Style(rect, fxFill),
                new Style(rectHover, fxFillHover)
        );
        
        group.getChildren().add(rectangle);        
        group.impl_processCSS(true);
        
        final StyleableProperty FILL = get(StyleableProperty.getStyleables(rectangle), "-fx-fill");
        final List<Style> actuals = FILL.getMatchingStyles(rectangle);

//        System.out.println("matchingStyles: " + matchingStyles);
//        System.out.println("expecteds: " + expecteds);
//        System.out.println("actuals: " + actuals);
                
        for (Style style : expecteds) {
            actuals.remove(style);
        }
        assertTrue(actuals.toString(), actuals.isEmpty());
    }    
    
    @Test
    public void testGetMatchingStylesReturnsInheritedProperty() {

        
        final Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(Stylesheet.Origin.USER_AGENT);
        
        final List<Rule> rules = stylesheet.getRules();
        
        List<String> rootStyleClass = new ArrayList<String>();
        rootStyleClass.add("root");
        
        List<String> rectStyleClass = new ArrayList<String>();
        rectStyleClass.add("rect");
        
        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        Selector root = new SimpleSelector("*", rootStyleClass, null, null);
        
        ParsedValue<Color,Color> fxFontShouldInheritValue = CSSParser.getInstance().parseExpr("-fx-font", "12px system");       
        Declaration fxFontShouldInherit = new Declaration("-fx-font", fxFontShouldInheritValue, false);
        
        List<Selector> selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, root); 
        
        List<Declaration> declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFontShouldInherit);
        
        Rule baseRule = new Rule(selectors, declarations);
        rules.add(baseRule);
        
        //
        // .rect { -fx-fill: -fx-color; }
        //
        Selector rect = new SimpleSelector("*", rectStyleClass, null, null);
        
        ParsedValue fxFillValue = CSSParser.getInstance().parseExpr("-fx-fill", "red");
        Declaration fxFill = new Declaration("-fx-fill", fxFillValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rect); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFill);
        
        Rule rectRule = new Rule(selectors, declarations);
        rules.add(rectRule);
               
        final Group group = createGroup(
            createCascadingStyle(root, fxFontShouldInherit)
        );
        
        final RectangleWithFont rectangle  = new RectangleWithFont(
            createCascadingStyle(rect, fxFill) 
        );
        
        List<Style> expecteds = new ArrayList<Style>();
        Collections.addAll(expecteds, 
                new Style(root, fxFontShouldInherit)
        );
        
        group.getChildren().add(rectangle);        
        group.impl_processCSS(true);
        
        final StyleableProperty FONT = get(StyleableProperty.getStyleables(rectangle), "-fx-font");
        final List<Style> actuals = FONT.getMatchingStyles(rectangle);

//        System.out.println("matchingStyles: " + matchingStyles);
//        System.out.println("expecteds: " + expecteds);
//        System.out.println("actuals: " + actuals);

        assertEquals(expecteds.size(), actuals.size(), 0);
                
        for (Style style : expecteds) {
            if (!actuals.remove(style)) fail();
        }
        assertTrue(actuals.isEmpty());
    }
    
    @Test
    public void testGetMatchingStylesReturnsSubProperty() {

        
        final Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(Stylesheet.Origin.USER_AGENT);
        
        final List<Rule> rules = stylesheet.getRules();
        
        List<String> rootStyleClass = new ArrayList<String>();
        rootStyleClass.add("root");
        
        List<String> rectStyleClass = new ArrayList<String>();
        rectStyleClass.add("rect");
        
        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        Selector root = new SimpleSelector("*", rootStyleClass, null, null);
        
        ParsedValue<Color,Color> fxFontShouldInheritValue = CSSParser.getInstance().parseExpr("-fx-font", "12px system");       
        Declaration fxFontShouldInherit = new Declaration("-fx-font", fxFontShouldInheritValue, false);
        
        List<Selector> selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, root); 
        
        List<Declaration> declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFontShouldInherit);
        
        Rule baseRule = new Rule(selectors, declarations);
        rules.add(baseRule);
        
        //
        // .rect { -fx-fill: -fx-color; }
        //
        Selector rect = new SimpleSelector("*", rectStyleClass, null, null);
        
        ParsedValue fxFillValue = CSSParser.getInstance().parseExpr("-fx-fill", "red");
        Declaration fxFill = new Declaration("-fx-fill", fxFillValue, false);

        ParsedValue fxFontFamilyValue = CSSParser.getInstance().parseExpr("-fx-font-family", "arial");
        Declaration fxFontFamily = new Declaration("-fx-font-family", fxFontFamilyValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rect); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFill, fxFontFamily);
        
        Rule rectRule = new Rule(selectors, declarations);
        rules.add(rectRule);
               
        final Group group = createGroup(
            createCascadingStyle(root, fxFontShouldInherit)
        );
        
        final RectangleWithFont rectangle  = new RectangleWithFont(
            createCascadingStyle(rect, fxFill),
            createCascadingStyle(rect, fxFontFamily)
        );
        
        List<Style> expecteds = new ArrayList<Style>();
        Collections.addAll(expecteds, 
                new Style(rect, fxFontFamily),
                new Style(root, fxFontShouldInherit)
        );
        
        group.getChildren().add(rectangle);        
        group.impl_processCSS(true);
        
        final StyleableProperty FONT = get(StyleableProperty.getStyleables(rectangle), "-fx-font");
        final List<Style> actuals = FONT.getMatchingStyles(rectangle);

//        System.out.println("matchingStyles: " + matchingStyles);
//        System.out.println("expecteds: " + expecteds);
//        System.out.println("actuals: " + actuals);

        assertEquals(expecteds.size(), actuals.size(), 0);
                
        for (Style style : expecteds) {
            if (!actuals.remove(style)) fail();
        }
        assertTrue(actuals.isEmpty());
    }    
}
