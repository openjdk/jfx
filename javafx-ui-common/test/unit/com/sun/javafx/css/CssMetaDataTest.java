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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javafx.beans.value.WritableValue;
import javafx.css.CssMetaData;
import javafx.css.FontCssMetaData;
import javafx.css.StyleOrigin;
import javafx.css.StyleableProperty;
import javafx.css.StyleConverter;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.parser.CSSParser;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.sg.PGNode;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.Test;

import static org.junit.Assert.*;


public class CssMetaDataTest {

    public CssMetaDataTest() {
    }
    
    private static CssMetaData get(List<CssMetaData<? extends Node, ?>> list, String prop) {
        for (CssMetaData styleable : list) {
            if (prop.equals(styleable.getProperty())) return styleable;
        }
        return null;
    }
    /**
     * Test of getStyleables method, of class CssMetaData.
     */
    @Test
    public void testGetStyleables_Node() {
        Node node = new TestNode();
        List<CssMetaData<? extends Node, ?>> expResult = TestNode.getClassCssMetaData();
        List<CssMetaData<? extends Node, ?>> result = node.getCssMetaData();
        assertEquals(expResult, result);
    }

    /**
     * Test of getStyleables method, of class CssMetaData.
     */
    @Test
    public void testGetStyleables_Styleable() {
        Node node = new TestNode();
        Styleable styleable = node.impl_getStyleable();
        assertNotNull(styleable);
        List<CssMetaData<? extends Node, ?>> expResult = TestNode.getClassCssMetaData();
        List result = styleable.getCssMetaData();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetJavafxBeansProperty() {
            TestNode testNode = new TestNode();
            WritableValue prop = TestNodeBase.StyleableProperties.TEST.getStyleableProperty(testNode);
            assert(prop != null);
            CssMetaData result = ((StyleableProperty)prop).getCssMetaData();
            assert(result == TestNodeBase.StyleableProperties.TEST);
    }

    /**
     * Test of getProperty method, of class CssMetaData.
     */
    @Test
    public void testGetProperty() {
        
        String expResult = "-fx-test";
        String result = TestNodeBase.StyleableProperties.TEST.getProperty();
        assertEquals(expResult, result);
    }

    /**
     * Test of getConverter method, of class CssMetaData.
     */
    @Test
    public void testGetConverter() {
        
        StyleConverter expResult = BooleanConverter.getInstance();
        StyleConverter result = TestNodeBase.StyleableProperties.TEST.getConverter();
        assertEquals(expResult, result);
    }

    /**
     * Test of getInitialValue method, of class CssMetaData.
     */
    @Test
    public void testGetInitialValue() {
        
        TestNode testNode = new TestNode();
        Double expResult = testNode.getXyzzy();
        Double result = (Double)TestNode.StyleableProperties.XYZZY.getInitialValue(testNode);
        assertEquals(expResult, result);

    }

    /**
     * Test of getSubProperties method, of class CssMetaData.
     */
    @Test
    public void testGetSubProperties() {
        
        CssMetaData<TestNode,Font> fontProp = 
                new FontCssMetaData<TestNode>("-fx-font", Font.getDefault()) {

            @Override
            public boolean isSettable(TestNode n) {
                return true;
            }

            @Override
            public StyleableProperty<Font> getStyleableProperty(TestNode n) {
                return null;
            }
        };
        
        List<CssMetaData<? extends Node, ?>> list = fontProp.getSubProperties();
        assertNotNull(list);

    }

    /**
     * Test of isInherits method, of class CssMetaData.
     */
    @Test
    public void testIsInherits() {

        boolean expResult = false;
        boolean result = TestNode.StyleableProperties.XYZZY.isInherits();
        assertEquals(expResult, result);

    }

    /**
     * Test of toString method, of class CssMetaData.
     */
    @Test
    public void testToString() {

        CssMetaData<TestNode,Font> fontProp = 
                new FontCssMetaData<TestNode>("-fx-font", Font.getDefault()) {

            @Override
            public boolean isSettable(TestNode n) {
                return true;
            }

            @Override
            public StyleableProperty<Font> getStyleableProperty(TestNode n) {
                return null;
            }
        };
        
        String string = fontProp.toString();
        assertNotNull(string);
        
    }

    /**
     * Test of equals method, of class CssMetaData.
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
        
        CssMetaData testNodeOpacity = get(TestNode.getClassCssMetaData(), "-fx-opacity");
        CssMetaData nodeOpacity = get(Node.getClassCssMetaData(), "-fx-opacity");
        
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
                            match.pseudoClasses,
                            match.specificity, 
                            ord++
                        )
                    );
                }
            }
        }
        
        return styles;
    }
        
    static int ord = 0;
    static CascadingStyle createCascadingStyle(Selector selector, Declaration declaration) {
        
        long[] pseudoClasses = null;
        if (selector instanceof SimpleSelector) {
            
            pseudoClasses = ((SimpleSelector)selector).getPseudoClassStates();
        } else {
            
            pseudoClasses = new long[0];
            for (SimpleSelector sel : ((CompoundSelector)selector).getSelectors()) {
                
                 long[] selectorPseudoClasses = sel.getPseudoClassStates();
                 if (pseudoClasses.length < selectorPseudoClasses.length) {
                     
                     long[] temp = Arrays.copyOf(selectorPseudoClasses, selectorPseudoClasses.length);
                     for (int n=0; n<pseudoClasses.length; n++) {
                         temp[n] |= pseudoClasses[n];
                     }
                     pseudoClasses = temp;
                     
                 } else {
                     
                     int nMax = Math.min(selectorPseudoClasses.length, pseudoClasses.length);
                     for (int n=0; n<nMax; n++) {
                         pseudoClasses[n] |= selectorPseudoClasses[n];
                     }                     
                     
                 }
            }
        }
        
        return new CascadingStyle(
            new Style(selector, declaration),
            pseudoClasses,
            0,
            ord++
        );
    }
        
    @Test @org.junit.Ignore
    public void testGetMatchingStyles() {

        
        final Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        StyleManager.setDefaultUserAgentStylesheet(stylesheet);
        
        final List<Rule> rules = stylesheet.getRules();
        
        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        List<String> rootStyleClass = new ArrayList<String>();
        rootStyleClass.add("root");
        
        Selector root = new SimpleSelector("*", rootStyleClass, null, null);
        
        ParsedValueImpl fxBaseValue = CSSParser.getInstance().parseExpr("-fx-base", "red");
        Declaration fxBase = new Declaration("-fx-base", fxBaseValue, false);

        ParsedValueImpl<String,String> fxColorValue = new ParsedValueImpl<String,String>(fxBase.getProperty(), null, true);
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
        List<String> rectStyleClass = new ArrayList<String>();
        rectStyleClass.add("rect");
        
        Selector rect = new SimpleSelector("*", rectStyleClass, null, null);
        
        ParsedValueImpl fxFillValue = CSSParser.getInstance().parseExpr("-fx-fill", "-fx-color");
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
        
        ParsedValueImpl<Color,Color> fxFillHoverValue = new ParsedValueImpl<Color,Color>(Color.YELLOW, null);        
        Declaration fxFillHover = new Declaration("-fx-fill", fxFillHoverValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rectHover); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFillHover);
        
        Rule rectHoverRule = new Rule(selectors, declarations);
        rules.add(rectHoverRule);

        List<Style> expecteds = new ArrayList<Style>();
        Collections.addAll(expecteds, 
                new Style(root, fxBase), 
                new Style(root, fxColor), 
                new Style(rect, fxFill),
                new Style(rectHover, fxFillHover)
        );
        
        final Rectangle rectangle = new Rectangle();
        rectangle.getStyleClass().add("rect");
        
        final Group group = new Group();
        group.getChildren().add(rectangle);
        
        Scene scene = new Scene(group);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        
        final CssMetaData FILL = get(rectangle.getCssMetaData(), "-fx-fill");
        final List<Style> actuals = StyleManager.getMatchingStyles(FILL, rectangle);

//        System.err.println("matchingStyles: " + matchingStyles);
//        System.err.println("expecteds: " + expecteds);
//        System.err.println("actuals: " + actuals);
        
        assertEquals(expecteds.size(), actuals.size(), 0);
        
        for (Style style : expecteds) {
            if (!actuals.remove(style)) fail();
        }
        assertTrue(actuals.isEmpty());
    }

    @Test @org.junit.Ignore
    public void testGetMatchingStylesWithInlineStyleOnParent() {

        
        final Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        StyleManager.setDefaultUserAgentStylesheet(stylesheet);
        
        final List<Rule> rules = stylesheet.getRules();
        
        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        List<String> rootStyleClass = new ArrayList<String>();
        rootStyleClass.add("root");
        
        Selector root = new SimpleSelector("*", rootStyleClass, null, null);
        
        ParsedValueImpl fxBaseValue = CSSParser.getInstance().parseExpr("-fx-base", "red");
        Declaration fxBase = new Declaration("-fx-base", fxBaseValue, false);

        ParsedValueImpl<String,String> fxColorValue = new ParsedValueImpl<String,String>(fxBase.getProperty(), null, true);
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
        List<String> rectStyleClass = new ArrayList<String>();
        rectStyleClass.add("rect");
        
        Selector rect = new SimpleSelector("*", rectStyleClass, null, null);
        
        ParsedValueImpl fxFillValue = CSSParser.getInstance().parseExpr("-fx-fill", "-fx-color");
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
        
        ParsedValueImpl<Color,Color> fxFillHoverValue = new ParsedValueImpl<Color,Color>(Color.YELLOW, null);        
        Declaration fxFillHover = new Declaration("-fx-fill", fxFillHoverValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rectHover); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFillHover);
        
        Rule rectHoverRule = new Rule(selectors, declarations);
        rules.add(rectHoverRule);

        // Declaration now checks origin, so we need to make this expected
        // value look like it came from an inline
        final Declaration decl = new Declaration("-fx-base", new ParsedValueImpl<Color,Color>(Color.GREEN, null), false);
    
        Stylesheet ss = new Stylesheet() {
            {
                setOrigin(StyleOrigin.INLINE);
                getRules().add(
                    new Rule(Arrays.asList(SimpleSelector.getUniversalSelector()), Arrays.asList(decl))
                );
            }
        };
       
        List<Style> expecteds = new ArrayList<Style>();
        Collections.addAll(expecteds, 
                new Style(SimpleSelector.getUniversalSelector(), decl),
                new Style(root, fxBase),
                new Style(root, fxColor), 
                new Style(rect, fxFill),
                new Style(rectHover, fxFillHover)
        );
                
        final Rectangle rectangle = new Rectangle();
        rectangle.getStyleClass().add("rect");
        
        final Group group = new Group();
        group.setStyle("-fx-base: green;");
        group.getChildren().add(rectangle);        
        
        Scene scene = new Scene(group);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();        
                
        final CssMetaData FILL = get(rectangle.getCssMetaData(), "-fx-fill");
        final List<Style> actuals = StyleManager.getMatchingStyles(FILL, rectangle);

//        System.err.println("matchingStyles: " + matchingStyles);
//        System.err.println("expecteds: " + expecteds);
//        System.err.println("actuals: " + actuals);
        
        assertEquals(expecteds.size(), actuals.size(), 0);

        // inline style should be first
        assertEquals(expecteds.get(0), actuals.get(0));
        
        for (Style style : expecteds) {
            if (!actuals.remove(style)) fail();
        }
        assertTrue(actuals.isEmpty());
    }

    @Test @org.junit.Ignore
    public void testGetMatchingStylesWithInlineStyleOnLeaf() {

        
        final Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        StyleManager.setDefaultUserAgentStylesheet(stylesheet);
        
        final List<Rule> rules = stylesheet.getRules();
        
        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        List<String> rootStyleClass = new ArrayList<String>();
        rootStyleClass.add("root");
        
        Selector root = new SimpleSelector("*", rootStyleClass, null, null);
        
        ParsedValueImpl fxBaseValue = CSSParser.getInstance().parseExpr("-fx-base", "red");
        Declaration fxBase = new Declaration("-fx-base", fxBaseValue, false);

        ParsedValueImpl<String,String> fxColorValue = new ParsedValueImpl<String,String>(fxBase.getProperty(), null, true);
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
        List<String> rectStyleClass = new ArrayList<String>();
        rectStyleClass.add("rect");
        
        Selector rect = new SimpleSelector("*", rectStyleClass, null, null);
        
        ParsedValueImpl fxFillValue = CSSParser.getInstance().parseExpr("-fx-fill", "-fx-color");
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
        
        ParsedValueImpl<Color,Color> fxFillHoverValue = new ParsedValueImpl<Color,Color>(Color.YELLOW, null);        
        Declaration fxFillHover = new Declaration("-fx-fill", fxFillHoverValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rectHover); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFillHover);
        
        Rule rectHoverRule = new Rule(selectors, declarations);
        rules.add(rectHoverRule);
                
        // Declaration now checks origin, so we need to make this expected
        // value look like it came from an inline
        final Declaration decl = new Declaration("-fx-base", new ParsedValueImpl<Color,Color>(Color.GREEN, null), false);
    
        Stylesheet ss = new Stylesheet() {
            {
                setOrigin(StyleOrigin.INLINE);
                getRules().add(
                    new Rule(Arrays.asList(SimpleSelector.getUniversalSelector()), Arrays.asList(decl))
                );
            }
        };
       
        List<Style> expecteds = new ArrayList<Style>();
        Collections.addAll(expecteds, 
                new Style(SimpleSelector.getUniversalSelector(), decl),
                new Style(root, fxBase),
                new Style(root, fxColor), 
                new Style(rect, fxFill),
                new Style(rectHover, fxFillHover)
        );
        
        final Rectangle rectangle = new Rectangle();
        rectangle.getStyleClass().add("rect");
        rectangle.setStyle("-fx-base: green;");
        
        final Group group = new Group();        
        group.getChildren().add(rectangle);        
        
        Scene scene = new Scene(group);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        final CssMetaData FILL = get(rectangle.getCssMetaData(), "-fx-fill");
        final List<Style> actuals = StyleManager.getMatchingStyles(FILL, rectangle);
                
//        System.err.println("matchingStyles: " + matchingStyles);
//        System.err.println("expecteds: " + expecteds);
//        System.err.println("actuals: " + actuals);

        assertEquals(expecteds.size(), actuals.size(), 0);
        
        // inline style should be first
        assertEquals(expecteds.get(0), actuals.get(0));
        
        for (Style style : expecteds) {
            if (!actuals.remove(style)) fail();
        }
        assertTrue(actuals.isEmpty());
    }   
    
    @Test @org.junit.Ignore
    public void testGetMatchingStylesWithInlineStyleOnRootAndLeaf() {


        final Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        StyleManager.setDefaultUserAgentStylesheet(stylesheet);
        
        final List<Rule> rules = stylesheet.getRules();
        
        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        List<String> rootStyleClass = new ArrayList<String>();
        rootStyleClass.add("root");
        
        Selector root = new SimpleSelector("*", rootStyleClass, null, null);
        
        ParsedValueImpl fxBaseValue = CSSParser.getInstance().parseExpr("-fx-base", "red");
        Declaration fxBase = new Declaration("-fx-base", fxBaseValue, false);

        ParsedValueImpl<String,String> fxColorValue = new ParsedValueImpl<String,String>(fxBase.getProperty(), null, true);
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
        List<String> rectStyleClass = new ArrayList<String>();
        rectStyleClass.add("rect");
        
        Selector rect = new SimpleSelector("*", rectStyleClass, null, null);
        
        ParsedValueImpl fxFillValue = CSSParser.getInstance().parseExpr("-fx-fill", "-fx-color");
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
        
        ParsedValueImpl<Color,Color> fxFillHoverValue = new ParsedValueImpl<Color,Color>(Color.YELLOW, null);        
        Declaration fxFillHover = new Declaration("-fx-fill", fxFillHoverValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rectHover); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFillHover);
        
        Rule rectHoverRule = new Rule(selectors, declarations);
        rules.add(rectHoverRule);

        // Declaration now checks origin, so we need to make this expected
        // value look like it came from an inline
        final Declaration gdecl = new Declaration("-fx-base", new ParsedValueImpl<Color,Color>(Color.GREEN, null), false);
        final Declaration ydecl = new Declaration("-fx-color", new ParsedValueImpl<Color,Color>(Color.YELLOW, null), false);
    
        Stylesheet ss = new Stylesheet() {
            {
                setOrigin(StyleOrigin.INLINE);
                Collections.addAll(getRules(),
                    new Rule(Arrays.asList(SimpleSelector.getUniversalSelector()), Arrays.asList(gdecl)),
                    new Rule(Arrays.asList(SimpleSelector.getUniversalSelector()), Arrays.asList(ydecl))
                );
            }
        };
       
        List<Style> expecteds = new ArrayList<Style>();
        Collections.addAll(expecteds, 
                new Style(SimpleSelector.getUniversalSelector(), ydecl),
                new Style(SimpleSelector.getUniversalSelector(), gdecl),
                new Style(root, fxBase),
                new Style(root, fxColor), 
                new Style(rect, fxFill),
                new Style(rectHover, fxFillHover)
        );
        
        final Rectangle rectangle = new Rectangle();
        rectangle.getStyleClass().add("rect");
        rectangle.setStyle("-fx-base: green;");
                
        final Group group = new Group();
        group.setStyle("-fx-color: yellow;");
        group.getChildren().add(rectangle);        

        Scene scene = new Scene(group);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        
        final CssMetaData FILL = get(rectangle.getCssMetaData(), "-fx-fill");
        final List<Style> actuals = StyleManager.getMatchingStyles(FILL, rectangle);

//        System.err.println("matchingStyles: " + matchingStyles);
//        System.err.println("expecteds: " + expecteds);
//        System.err.println("actuals: " + actuals);

        assertEquals(expecteds.size(), actuals.size(), 0);
        
        // inline style should be first
        assertEquals(expecteds.get(0), actuals.get(0));
        
        for (Style style : expecteds) {
            if (!actuals.remove(style)) fail(style.toString());
        }
        assertTrue(actuals.isEmpty());
    } 

    @Test @org.junit.Ignore
    public void testGetMatchingStylesShouldNotReturnAncestorPropertyIfNotInherited() {

        
        final Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        StyleManager.setDefaultUserAgentStylesheet(stylesheet);
        
        final List<Rule> rules = stylesheet.getRules();
        
        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        List<String> rootStyleClass = new ArrayList<String>();
        rootStyleClass.add("root");
        
        Selector root = new SimpleSelector("*", rootStyleClass, null, null);
        
        ParsedValueImpl fxBaseValue = CSSParser.getInstance().parseExpr("-fx-base", "red");
        Declaration fxBase = new Declaration("-fx-base", fxBaseValue, false);

        ParsedValueImpl<String,String> fxColorValue = new ParsedValueImpl<String,String>(fxBase.getProperty(), null, true);
        Declaration fxColor = new Declaration("-fx-color", fxColorValue, false);

        ParsedValueImpl<Color,Color> fxFillShouldNotMatchValue = new ParsedValueImpl<Color,Color>(Color.RED, null);        
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
        List<String> rectStyleClass = new ArrayList<String>();
        rectStyleClass.add("rect");
        
        Selector rect = new SimpleSelector("*", rectStyleClass, null, null);
        
        ParsedValueImpl fxFillValue = CSSParser.getInstance().parseExpr("-fx-fill", "-fx-color");
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
        
        ParsedValueImpl<Color,Color> fxFillHoverValue = new ParsedValueImpl<Color,Color>(Color.YELLOW, null);        
        Declaration fxFillHover = new Declaration("-fx-fill", fxFillHoverValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rectHover); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFillHover);
        
        Rule rectHoverRule = new Rule(selectors, declarations);
        rules.add(rectHoverRule);
                
        List<Style> expecteds = new ArrayList<Style>();
        Collections.addAll(expecteds, 
                new Style(root, fxBase),
                new Style(root, fxColor), 
                new Style(rect, fxFill),
                new Style(rectHover, fxFillHover)
        );
        
        final Rectangle rectangle = new Rectangle();
        rectangle.getStyleClass().add("rect");
        
        final Group group = new Group();
        group.getChildren().add(rectangle);        

        Scene scene = new Scene(group);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
                
        final CssMetaData FILL = get(rectangle.getCssMetaData(), "-fx-fill");
        final List<Style> actuals = StyleManager.getMatchingStyles(FILL, rectangle);

//        System.err.println("matchingStyles: " + matchingStyles);
//        System.err.println("expecteds: " + expecteds);
//        System.err.println("actuals: " + actuals);

        assertEquals(expecteds.size(), actuals.size(), 0);
                
        for (Style style : expecteds) {
            if (!actuals.remove(style)) fail();
        }
        assertTrue(actuals.isEmpty());
    } 


    @Test @org.junit.Ignore
    public void testGetMatchingStylesShouldNotReturnInlineAncestorPropertyIfNotInherited() {
        
        final Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        StyleManager.setDefaultUserAgentStylesheet(stylesheet);
        
        final List<Rule> rules = stylesheet.getRules();
        
        List<String> rootStyleClass = new ArrayList<String>();
        rootStyleClass.add("root");
        
        List<String> rectStyleClass = new ArrayList<String>();
        rectStyleClass.add("rect");
        
        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        Selector root = new SimpleSelector("*", rootStyleClass, null, null);
        
        ParsedValueImpl fxBaseValue = CSSParser.getInstance().parseExpr("-fx-base", "red");
        Declaration fxBase = new Declaration("-fx-base", fxBaseValue, false);

        ParsedValueImpl<String,String> fxColorValue = new ParsedValueImpl<String,String>(fxBase.getProperty(), null, true);
        Declaration fxColor = new Declaration("-fx-color", fxColorValue, false);

        ParsedValueImpl<Color,Color> fxFillShouldNotMatchValue = new ParsedValueImpl<Color,Color>(Color.RED, null);        
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
        
        ParsedValueImpl fxFillValue = CSSParser.getInstance().parseExpr("-fx-fill", "-fx-color");
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
        
        ParsedValueImpl<Color,Color> fxFillHoverValue = new ParsedValueImpl<Color,Color>(Color.YELLOW, null);        
        Declaration fxFillHover = new Declaration("-fx-fill", fxFillHoverValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, rectHover); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFillHover);
        
        Rule rectHoverRule = new Rule(selectors, declarations);
        rules.add(rectHoverRule);
                
        List<Style> expecteds = new ArrayList<Style>();
        Collections.addAll(expecteds, 
                new Style(root, fxBase),
                new Style(root, fxColor), 
                new Style(rect, fxFill),
                new Style(rectHover, fxFillHover)
        );
        
        final Rectangle rectangle = new Rectangle();
        rectangle.getStyleClass().add("rect");
        
        final Group group = new Group();
        group.getChildren().add(rectangle);        
        
        Scene scene = new Scene(group);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
                
        final CssMetaData FILL = get(rectangle.getCssMetaData(), "-fx-fill");
        final List<Style> actuals = StyleManager.getMatchingStyles(FILL, rectangle);

//        System.err.println("matchingStyles: " + matchingStyles);
//        System.err.println("expecteds: " + expecteds);
//        System.err.println("actuals: " + actuals);
                
        for (Style style : expecteds) {
            actuals.remove(style);
        }
        assertTrue(actuals.toString(), actuals.isEmpty());
    }    
    
    @Test @org.junit.Ignore
    public void testGetMatchingStylesReturnsInheritedProperty() {


        final Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        StyleManager.setDefaultUserAgentStylesheet(stylesheet);
        
        final List<Rule> rules = stylesheet.getRules();
        
        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        List<String> rootStyleClass = new ArrayList<String>();
        rootStyleClass.add("root");
        
        Selector root = new SimpleSelector("*", rootStyleClass, null, null);
        
        ParsedValueImpl<Color,Color> fxFontShouldInheritValue = CSSParser.getInstance().parseExpr("-fx-font", "12px system");       
        Declaration fxFontShouldInherit = new Declaration("-fx-font", fxFontShouldInheritValue, false);
        
        List<Selector> selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, root); 
        
        List<Declaration> declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFontShouldInherit);
        
        Rule baseRule = new Rule(selectors, declarations);
        rules.add(baseRule);
        
        //
        // .text { -fx-fill: -fx-color; }
        //
        List<String> textStyleClass = new ArrayList<String>();
        textStyleClass.add("text");
        
        Selector textSelector = new SimpleSelector("*", textStyleClass, null, null);
        
        ParsedValueImpl fxFillValue = CSSParser.getInstance().parseExpr("-fx-fill", "red");
        Declaration fxFill = new Declaration("-fx-fill", fxFillValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, textSelector); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFill);
        
        Rule rectRule = new Rule(selectors, declarations);
        rules.add(rectRule);
               
        List<Style> expecteds = new ArrayList<Style>();
        Collections.addAll(expecteds, 
                new Style(root, fxFontShouldInherit)
        );
        
        final Text text = new Text("text");
        text.getStyleClass().add("text");
        
        final Group group = new Group();
        group.getChildren().add(text);        
        
        Scene scene = new Scene(group);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
                
        final CssMetaData FONT = get(text.getCssMetaData(), "-fx-font");
        final List<Style> actuals = StyleManager.getMatchingStyles(FONT, text);

//        System.err.println("matchingStyles: " + matchingStyles);
//        System.err.println("expecteds: " + expecteds);
//        System.err.println("actuals: " + actuals);

        assertEquals(expecteds.size(), actuals.size(), 0);
                
        for (Style style : expecteds) {
            if (!actuals.remove(style)) fail();
        }
        assertTrue(actuals.isEmpty());
    }
    
    @Test @org.junit.Ignore
    public void testGetMatchingStylesReturnsSubProperty() {

        final Stylesheet stylesheet = new Stylesheet();
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        StyleManager.setDefaultUserAgentStylesheet(stylesheet);        
                
        final List<Rule> rules = stylesheet.getRules();
        
        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        List<String> rootStyleClass = new ArrayList<String>();
        rootStyleClass.add("root");
        
        Selector root = new SimpleSelector("*", rootStyleClass, null, null);
        
        ParsedValueImpl<Color,Color> fxFontShouldInheritValue = CSSParser.getInstance().parseExpr("-fx-font", "12px system");       
        Declaration fxFontShouldInherit = new Declaration("-fx-font", fxFontShouldInheritValue, false);
        
        List<Selector> selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, root); 
        
        List<Declaration> declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFontShouldInherit);
        
        Rule baseRule = new Rule(selectors, declarations);
        rules.add(baseRule);
        
        //
        // .text { -fx-fill: -fx-color; }
        //
        List<String> rectStyleClass = new ArrayList<String>();
        rectStyleClass.add("text");
        
        Selector textSelector = new SimpleSelector("*", rectStyleClass, null, null);
        
        ParsedValueImpl fxFillValue = CSSParser.getInstance().parseExpr("-fx-fill", "red");
        Declaration fxFill = new Declaration("-fx-fill", fxFillValue, false);

        ParsedValueImpl fxFontFamilyValue = CSSParser.getInstance().parseExpr("-fx-font-family", "arial");
        Declaration fxFontFamily = new Declaration("-fx-font-family", fxFontFamilyValue, false);
        
        selectors = new ArrayList<Selector>();
        Collections.addAll(selectors, textSelector); 
        
        declarations = new ArrayList<Declaration>(); 
        Collections.addAll(declarations, fxFill, fxFontFamily);
        
        Rule rectRule = new Rule(selectors, declarations);
        rules.add(rectRule);

        List<Style> expecteds = new ArrayList<Style>();
        Collections.addAll(expecteds, 
                new Style(textSelector, fxFontFamily),
                new Style(root, fxFontShouldInherit)
        );
        
        final Text text  = new Text();
        text.getStyleClass().add("text");
        
        final Group group = new Group();
        group.getChildren().add(text);        

        Scene scene = new Scene(group);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
                        
        final CssMetaData FONT = get(text.getCssMetaData(), "-fx-font");
        final List<Style> actuals = StyleManager.getMatchingStyles(FONT, text);

//        System.err.println("matchingStyles: " + matchingStyles);
//        System.err.println("expecteds: " + expecteds);
//        System.err.println("actuals: " + actuals);

        assertEquals(expecteds.size(), actuals.size(), 0);
                
        for (Style style : expecteds) {
            if (!actuals.remove(style)) fail();
        }
        assertTrue(actuals.isEmpty());
    }    
    
    @Test
    public void testRT18097() {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            URL base = cl.getResource("javafx/..");
            File f = new File(base.toURI());
//            System.err.println(f.getPath());
            recursiveCheck(f, f.getPath().length());
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            fail(ex.getMessage());
        }
    }

    private static void checkClass(Class someClass) {
        
        if (javafx.scene.Node.class.isAssignableFrom(someClass) &&
                Modifier.isAbstract(someClass.getModifiers()) == false) {
            
            String what = someClass.getName();
            try {
                // should get NoSuchMethodException if ctor is not public
//                Constructor ctor = someClass.getConstructor((Class[])null);
                Method m = someClass.getMethod("getClassCssMetaData", (Class[]) null);
//                Node node = (Node)ctor.newInstance((Object[])null);
                Node node = (Node)someClass.newInstance();
                for (CssMetaData styleable : (List<CssMetaData<? extends Node, ?>>) m.invoke(null)) {
                    
                    what = someClass.getName() + " " + styleable.getProperty();
                    WritableValue writable = styleable.getStyleableProperty(node);
                    assertNotNull(what, writable);
                    
                    Object defaultValue = writable.getValue();
                    Object initialValue = styleable.getInitialValue((Node) someClass.newInstance());
                    
                    if (defaultValue instanceof Number) {
                        // 5 and 5.0 are not the same according to equals,
                        // but they should be...
                        assert(initialValue instanceof Number);
                        double d1 = ((Number)defaultValue).doubleValue();
                        double d2 = ((Number)initialValue).doubleValue();
                        assertEquals(what, d1, d2, .001);
                        
                    } else if (defaultValue != null && defaultValue.getClass().isArray()) {
                        assertTrue(what, Arrays.equals((Object[])defaultValue, (Object[])initialValue));
                    } else {
                        assertEquals(what, defaultValue, initialValue);
                    }
                    
                }

            } catch (NoSuchMethodException ex) {
                System.err.println("NoSuchMethodException: " + what);
            } catch (IllegalAccessException ex) {
                System.err.println("IllegalAccessException: " + what);
            } catch (IllegalArgumentException ex) {
                System.err.println("IllegalArgumentException: " + what);
            } catch (InvocationTargetException ex) {
                System.err.println("InvocationTargetException: " + what);
            } catch (InstantiationException ex) {
                System.err.println("InstantiationException: " + what);                
            }
        }
    }

    private static void checkDirectory(File directory, final int pathLength) {
        if (directory.isDirectory()) {
            
            for (File file : directory.listFiles()) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    final int len = file.getPath().length() - ".class".length();
                    final String clName = 
                        file.getPath().substring(pathLength+1, len).replace(File.separatorChar,'.');
                    try {
                        final Class cl = Class.forName(clName);
                        if (cl != null) checkClass(cl);
                    } catch(ClassNotFoundException ex) {
                        System.err.println(ex.toString() + " " + clName);
                    }
                }
            }
        }
    }

    private static void recursiveCheck(File directory, int pathLength) {
        if (directory.isDirectory()) {
//            System.err.println(directory.getPath());
            checkDirectory(directory, pathLength);

            for (File subFile : directory.listFiles()) {
                recursiveCheck(subFile, pathLength);
            }
        }
    }    
    
    @Test
    public void testRT_21185() {

        Color c1 = new Color(.1,.2,.3,1.0);
        Color c2 = new Color(.1,.2,.3,1.0);
                
        Rectangle rect = new Rectangle();
        rect.setFill(c1);
        
        
        CssMetaData fill = ((StyleableProperty)rect.fillProperty()).getCssMetaData();
        StyleOrigin origin = ((StyleableProperty)rect.fillProperty()).getStyleOrigin();

        // set should not change the value if the values are equal and origin is same
        assertEquals(c1, c2);
        fill.set(rect, c2, origin);
        assert(c1 == rect.getFill()); // instance should not change.

        // set should change the value if the values are not equal.
        c2 = new Color(.3,.2,.1,1.0);
        fill.set(rect, c2, origin);
        assert(c2 == rect.getFill());
        
        // set should change the value if the origin is not the same
        fill.set(rect, c2, StyleOrigin.INLINE);
        origin = ((StyleableProperty)rect.fillProperty()).getStyleOrigin();
        assert(origin == StyleOrigin.INLINE);
        
        // set should change the value if one is null and the other is not.
        rect.setFill(null);
        fill.set(rect, c2, origin);
        assert(c2 == rect.getFill());
        
        // set should change the value if one is null and the other is not
        fill.set(rect, null, origin);
        assertNull(rect.getFill());
        
    }
    
    
    @Test  @org.junit.Ignore
    public void testRT_24606() {

        final Stylesheet stylesheet = CSSParser.getInstance().parse(
                ".root { -fx-base: red; }" +
                ".group { -fx-color: -fx-base; }" +
                ".text { -fx-fill: -fx-color; }" 
        );
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        StyleManager.setDefaultUserAgentStylesheet(stylesheet);        
       
        final Text text = new Text("HelloWorld");
        text.getStyleClass().add("text");
        text.setFill(Color.BLUE);
        
        final Group group = new Group();
        group.getStyleClass().add("group");
        group.getChildren().add(text);

        final Group root = new Group();
        root.getChildren().add(group);
        
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        CssMetaData prop = ((StyleableProperty)text.fillProperty()).getCssMetaData();
        List list = StyleManager.getMatchingStyles(prop, text);
        
        assertEquals(3, list.size(), 0);
                
    }    
    
}
