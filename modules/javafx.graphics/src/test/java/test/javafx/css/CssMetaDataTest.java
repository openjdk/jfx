/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.css;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.sun.javafx.css.CascadingStyle;
import com.sun.javafx.css.ParsedValueImpl;
import com.sun.javafx.css.PseudoClassState;
import com.sun.javafx.css.StyleManager;
import com.sun.javafx.scene.AbstractNode;
import test.com.sun.javafx.css.TestNode;
import test.com.sun.javafx.css.TestNodeBase;
import javafx.beans.value.WritableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.css.converter.BooleanConverter;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.css.CompoundSelector;
import com.sun.javafx.css.SimpleSelector;
import javafx.css.CssMetaData;
import javafx.css.CssParser;
import javafx.css.CssParserShim;
import javafx.css.Declaration;
import javafx.css.DeclarationShim;
import javafx.css.FontCssMetaData;
import javafx.css.ParsedValue;
import javafx.css.PseudoClass;
import javafx.css.Rule;
import javafx.css.RuleShim;
import javafx.css.Selector;
import javafx.css.SelectorShim;
import javafx.css.Style;
import javafx.css.StyleConverter;
import javafx.css.StyleOrigin;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.Stylesheet;
import javafx.css.StylesheetShim;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

enum TestEnum {
    LEFT,
    CENTER,
    RIGHT,
    JUSTIFY
}

public class CssMetaDataTest {

    public CssMetaDataTest() {
    }

    private static CssMetaData get(List<CssMetaData<? extends Styleable, ?>> list, String prop) {
        for (CssMetaData styleable : list) {
            if (prop.equals(styleable.getProperty())) return styleable;
        }
        return null;
    }

    private static void resetStyleManager() {
        StyleManager sm = StyleManager.getInstance();
        sm.userAgentStylesheetContainers.clear();
        sm.platformUserAgentStylesheetContainers.clear();
        sm.stylesheetContainerMap.clear();
        sm.cacheContainerMap.clear();
        sm.hasDefaultUserAgentStylesheet = false;
    }

    @AfterEach
    public void cleanup() {
        resetStyleManager();
    }

    /**
     * Test of getCssMetaData method of class Styleable.
     */
    @Test
    public void testGetCssMetaData_Styleable() {
        Styleable styleable = new TestNode();
        List<CssMetaData<? extends Styleable, ?>> expResult = TestNode.getClassCssMetaData();
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
                new FontCssMetaData<>("-fx-font", Font.getDefault()) {

                    @Override
                    public boolean isSettable(TestNode n) {
                        return true;
                    }

                    @Override
                    public StyleableProperty<Font> getStyleableProperty(TestNode n) {
                        return null;
                    }
                };

        List<CssMetaData<? extends Styleable, ?>> list = fontProp.getSubProperties();
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
                new FontCssMetaData<>("-fx-font", Font.getDefault()) {

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
        Node node = new AbstractNode() {
        };

        CssMetaData testNodeOpacity = get(TestNode.getClassCssMetaData(), "-fx-opacity");
        CssMetaData nodeOpacity = get(Node.getClassCssMetaData(), "-fx-opacity");

        assertTrue(testNodeOpacity.equals(nodeOpacity));
    }

    static int ord = 0;
    static CascadingStyle createCascadingStyle(Selector selector, Declaration declaration) {

        Set<PseudoClass> pseudoClasses = null;
        if (selector instanceof SimpleSelector s) {

            pseudoClasses = s.getPseudoClassStates();
        } else {

            pseudoClasses = new PseudoClassState();
            for (SimpleSelector sel : ((CompoundSelector)selector).getSelectors()) {

                Set<PseudoClass> selectorPseudoClasses = sel.getPseudoClassStates();
                pseudoClasses.addAll(selectorPseudoClasses);
            }
        }

        return new CascadingStyle(
                new Style(selector, declaration),
                pseudoClasses,
                0,
                ord++
        );
    }

    @Disabled("JDK-8234142")
    @Test
    public void testGetMatchingStyles() {


        final Stylesheet stylesheet = StylesheetShim.getStylesheet();
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);

        final List<Rule> rules = stylesheet.getRules();

        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        List<String> rootStyleClass = new ArrayList<>();
        rootStyleClass.add("root");

        Selector root = new SimpleSelector("*", rootStyleClass, null, null);

        ParsedValue fxBaseValue = new CssParserShim().parseExpr("-fx-base", "red");
        Declaration fxBase = DeclarationShim.getDeclaration("-fx-base", fxBaseValue, false);

        ParsedValueImpl<String,String> fxColorValue = new ParsedValueImpl<>(fxBase.getProperty(), null, true);
        Declaration fxColor = DeclarationShim.getDeclaration("-fx-color", fxColorValue, false);

        List<Selector> selectors = new ArrayList<>();
        Collections.addAll(selectors, root);

        List<Declaration> declarations = new ArrayList<>();
        Collections.addAll(declarations, fxBase, fxColor);

        Rule baseRule = RuleShim.getRule(selectors, declarations);
        rules.add(baseRule);

        //
        // .rect { -fx-fill: -fx-color; }
        //
        List<String> rectStyleClass = new ArrayList<>();
        rectStyleClass.add("rect");

        Selector rect = new SimpleSelector("*", rectStyleClass, null, null);

        ParsedValue fxFillValue = new CssParserShim().parseExpr("-fx-fill", "-fx-color");
        Declaration fxFill = DeclarationShim.getDeclaration("-fx-fill", fxFillValue, false);

        selectors = new ArrayList<>();
        Collections.addAll(selectors, rect);

        declarations = new ArrayList<>();
        Collections.addAll(declarations, fxFill);

        Rule rectRule = RuleShim.getRule(selectors, declarations);
        rules.add(rectRule);

        // .rect:hover { -fx-fill: yellow; }
        List<String> pseudoclasses = new ArrayList<>();
        pseudoclasses.add("hover");

        Selector rectHover = new SimpleSelector("*", rectStyleClass, pseudoclasses, null);

        ParsedValueImpl<Color,Color> fxFillHoverValue = new ParsedValueImpl<>(Color.YELLOW, null);
        Declaration fxFillHover = DeclarationShim.getDeclaration("-fx-fill", fxFillHoverValue, false);

        selectors = new ArrayList<>();
        Collections.addAll(selectors, rectHover);

        declarations = new ArrayList<>();
        Collections.addAll(declarations, fxFillHover);

        Rule rectHoverRule = RuleShim.getRule(selectors, declarations);
        rules.add(rectHoverRule);

        List<Style> expecteds = new ArrayList<>();
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
        final List<Style> actuals = NodeHelper.getMatchingStyles(FILL, rectangle);

        //        System.err.println("matchingStyles: " + matchingStyles);
        //        System.err.println("expecteds: " + expecteds);
        //        System.err.println("actuals: " + actuals);

        assertEquals(expecteds.size(), actuals.size(), 0);

        for (Style style : expecteds) {
            if (!actuals.remove(style)) fail();
        }
        assertTrue(actuals.isEmpty());
    }

    @Disabled("JDK-8234142")
    @Test
    public void testGetMatchingStylesWithInlineStyleOnParent() {

        final Stylesheet stylesheet = StylesheetShim.getStylesheet();
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);

        final List<Rule> rules = stylesheet.getRules();

        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        List<String> rootStyleClass = new ArrayList<>();
        rootStyleClass.add("root");

        Selector root = new SimpleSelector("*", rootStyleClass, null, null);

        ParsedValue fxBaseValue = new CssParserShim().parseExpr("-fx-base", "red");
        Declaration fxBase = DeclarationShim.getDeclaration("-fx-base", fxBaseValue, false);

        ParsedValueImpl<String,String> fxColorValue = new ParsedValueImpl<>(fxBase.getProperty(), null, true);
        Declaration fxColor = DeclarationShim.getDeclaration("-fx-color", fxColorValue, false);

        List<Selector> selectors = new ArrayList<>();
        Collections.addAll(selectors, root);

        List<Declaration> declarations = new ArrayList<>();
        Collections.addAll(declarations, fxBase, fxColor);

        Rule baseRule = RuleShim.getRule(selectors, declarations);
        rules.add(baseRule);

        //
        // .rect { -fx-fill: -fx-color; }
        //
        List<String> rectStyleClass = new ArrayList<>();
        rectStyleClass.add("rect");

        Selector rect = new SimpleSelector("*", rectStyleClass, null, null);

        ParsedValue fxFillValue = new CssParserShim().parseExpr("-fx-fill", "-fx-color");
        Declaration fxFill = DeclarationShim.getDeclaration("-fx-fill", fxFillValue, false);

        selectors = new ArrayList<>();
        Collections.addAll(selectors, rect);

        declarations = new ArrayList<>();
        Collections.addAll(declarations, fxFill);

        Rule rectRule = RuleShim.getRule(selectors, declarations);
        rules.add(rectRule);

        // .rect:hover { -fx-fill: yellow; }
        List<String> pseudoclasses = new ArrayList<>();
        pseudoclasses.add("hover");

        Selector rectHover = new SimpleSelector("*", rectStyleClass, pseudoclasses, null);

        ParsedValueImpl<Color,Color> fxFillHoverValue = new ParsedValueImpl<>(Color.YELLOW, null);
        Declaration fxFillHover = DeclarationShim.getDeclaration("-fx-fill", fxFillHoverValue, false);

        selectors = new ArrayList<>();
        Collections.addAll(selectors, rectHover);

        declarations = new ArrayList<>();
        Collections.addAll(declarations, fxFillHover);

        Rule rectHoverRule = RuleShim.getRule(selectors, declarations);
        rules.add(rectHoverRule);

        // Declaration now checks origin, so we need to make this expected
        // value look like it came from an inline
        final Declaration decl = DeclarationShim.getDeclaration("-fx-base", new ParsedValueImpl<Color,Color>(Color.GREEN, null), false);

        Stylesheet ss = new StylesheetShim(null) {
            {
                setOrigin(StyleOrigin.INLINE);
                getRules().add(
                        RuleShim.getRule(Arrays.asList(SelectorShim.getUniversalSelector()), Arrays.asList(decl))
                );
            }
        };

        List<Style> expecteds = new ArrayList<>();
        Collections.addAll(expecteds,
                           new Style(SelectorShim.getUniversalSelector(), decl),
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
        final List<Style> actuals = NodeHelper.getMatchingStyles(FILL, rectangle);

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

    @Disabled("JDK-8234142")
    @Test
    public void testGetMatchingStylesWithInlineStyleOnLeaf() {


        final Stylesheet stylesheet = StylesheetShim.getStylesheet();
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);

        final List<Rule> rules = stylesheet.getRules();

        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        List<String> rootStyleClass = new ArrayList<>();
        rootStyleClass.add("root");

        Selector root = new SimpleSelector("*", rootStyleClass, null, null);

        ParsedValue fxBaseValue = new CssParserShim().parseExpr("-fx-base", "red");
        Declaration fxBase = DeclarationShim.getDeclaration("-fx-base", fxBaseValue, false);

        ParsedValueImpl<String,String> fxColorValue = new ParsedValueImpl<>(fxBase.getProperty(), null, true);
        Declaration fxColor = DeclarationShim.getDeclaration("-fx-color", fxColorValue, false);

        List<Selector> selectors = new ArrayList<>();
        Collections.addAll(selectors, root);

        List<Declaration> declarations = new ArrayList<>();
        Collections.addAll(declarations, fxBase, fxColor);

        Rule baseRule = RuleShim.getRule(selectors, declarations);
        rules.add(baseRule);

        //
        // .rect { -fx-fill: -fx-color; }
        //
        List<String> rectStyleClass = new ArrayList<>();
        rectStyleClass.add("rect");

        Selector rect = new SimpleSelector("*", rectStyleClass, null, null);

        ParsedValue fxFillValue = new CssParserShim().parseExpr("-fx-fill", "-fx-color");
        Declaration fxFill = DeclarationShim.getDeclaration("-fx-fill", fxFillValue, false);

        selectors = new ArrayList<>();
        Collections.addAll(selectors, rect);

        declarations = new ArrayList<>();
        Collections.addAll(declarations, fxFill);

        Rule rectRule = RuleShim.getRule(selectors, declarations);
        rules.add(rectRule);

        // .rect:hover { -fx-fill: yellow; }
        List<String> pseudoclasses = new ArrayList<>();
        pseudoclasses.add("hover");

        Selector rectHover = new SimpleSelector("*", rectStyleClass, pseudoclasses, null);

        ParsedValueImpl<Color,Color> fxFillHoverValue = new ParsedValueImpl<>(Color.YELLOW, null);
        Declaration fxFillHover = DeclarationShim.getDeclaration("-fx-fill", fxFillHoverValue, false);

        selectors = new ArrayList<>();
        Collections.addAll(selectors, rectHover);

        declarations = new ArrayList<>();
        Collections.addAll(declarations, fxFillHover);

        Rule rectHoverRule = RuleShim.getRule(selectors, declarations);
        rules.add(rectHoverRule);

        // Declaration now checks origin, so we need to make this expected
        // value look like it came from an inline
        final Declaration decl = DeclarationShim.getDeclaration("-fx-base", new ParsedValueImpl<Color,Color>(Color.GREEN, null), false);

        Stylesheet ss = new StylesheetShim(null) {
            {
                setOrigin(StyleOrigin.INLINE);
                getRules().add(
                        RuleShim.getRule(Arrays.asList(SelectorShim.getUniversalSelector()), Arrays.asList(decl))
                );
            }
        };

        List<Style> expecteds = new ArrayList<>();
        Collections.addAll(expecteds,
                           new Style(SelectorShim.getUniversalSelector(), decl),
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
        final List<Style> actuals = NodeHelper.getMatchingStyles(FILL, rectangle);

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

    @Disabled("JDK-8234142")
    @Test
    public void testGetMatchingStylesWithInlineStyleOnRootAndLeaf() {


        final Stylesheet stylesheet = StylesheetShim.getStylesheet();
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);

        final List<Rule> rules = stylesheet.getRules();

        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        List<String> rootStyleClass = new ArrayList<>();
        rootStyleClass.add("root");

        Selector root = new SimpleSelector("*", rootStyleClass, null, null);

        ParsedValue fxBaseValue = new CssParserShim().parseExpr("-fx-base", "red");
        Declaration fxBase = DeclarationShim.getDeclaration("-fx-base", fxBaseValue, false);

        ParsedValueImpl<String,String> fxColorValue = new ParsedValueImpl<>(fxBase.getProperty(), null, true);
        Declaration fxColor = DeclarationShim.getDeclaration("-fx-color", fxColorValue, false);

        List<Selector> selectors = new ArrayList<>();
        Collections.addAll(selectors, root);

        List<Declaration> declarations = new ArrayList<>();
        Collections.addAll(declarations, fxBase, fxColor);

        Rule baseRule = RuleShim.getRule(selectors, declarations);
        rules.add(baseRule);

        //
        // .rect { -fx-fill: -fx-color; }
        //
        List<String> rectStyleClass = new ArrayList<>();
        rectStyleClass.add("rect");

        Selector rect = new SimpleSelector("*", rectStyleClass, null, null);

        ParsedValue fxFillValue = new CssParserShim().parseExpr("-fx-fill", "-fx-color");
        Declaration fxFill = DeclarationShim.getDeclaration("-fx-fill", fxFillValue, false);

        selectors = new ArrayList<>();
        Collections.addAll(selectors, rect);

        declarations = new ArrayList<>();
        Collections.addAll(declarations, fxFill);

        Rule rectRule = RuleShim.getRule(selectors, declarations);
        rules.add(rectRule);

        // .rect:hover { -fx-fill: yellow; }
        List<String> pseudoclasses = new ArrayList<>();
        pseudoclasses.add("hover");

        Selector rectHover = new SimpleSelector("*", rectStyleClass, pseudoclasses, null);

        ParsedValueImpl<Color,Color> fxFillHoverValue = new ParsedValueImpl<>(Color.YELLOW, null);
        Declaration fxFillHover = DeclarationShim.getDeclaration("-fx-fill", fxFillHoverValue, false);

        selectors = new ArrayList<>();
        Collections.addAll(selectors, rectHover);

        declarations = new ArrayList<>();
        Collections.addAll(declarations, fxFillHover);

        Rule rectHoverRule = RuleShim.getRule(selectors, declarations);
        rules.add(rectHoverRule);

        // Declaration now checks origin, so we need to make this expected
        // value look like it came from an inline
        final Declaration gdecl = DeclarationShim.getDeclaration("-fx-base", new ParsedValueImpl<Color,Color>(Color.GREEN, null), false);
        final Declaration ydecl = DeclarationShim.getDeclaration("-fx-color", new ParsedValueImpl<Color,Color>(Color.YELLOW, null), false);

        Stylesheet ss = new StylesheetShim(null) {
            {
                setOrigin(StyleOrigin.INLINE);
                Collections.addAll(getRules(),
                                   RuleShim.getRule(Arrays.asList(SelectorShim.getUniversalSelector()), Arrays.asList(gdecl)),
                                   RuleShim.getRule(Arrays.asList(SelectorShim.getUniversalSelector()), Arrays.asList(ydecl))
                );
            }
        };

        List<Style> expecteds = new ArrayList<>();
        Collections.addAll(expecteds,
                           new Style(SelectorShim.getUniversalSelector(), ydecl),
                           new Style(SelectorShim.getUniversalSelector(), gdecl),
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
        final List<Style> actuals = NodeHelper.getMatchingStyles(FILL, rectangle);

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

    @Disabled("JDK-8234142")
    @Test
    public void testGetMatchingStylesShouldNotReturnAncestorPropertyIfNotInherited() {


        final Stylesheet stylesheet = StylesheetShim.getStylesheet();
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);

        final List<Rule> rules = stylesheet.getRules();

        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        List<String> rootStyleClass = new ArrayList<>();
        rootStyleClass.add("root");

        Selector root = new SimpleSelector("*", rootStyleClass, null, null);

        ParsedValue fxBaseValue = new CssParserShim().parseExpr("-fx-base", "red");
        Declaration fxBase = DeclarationShim.getDeclaration("-fx-base", fxBaseValue, false);

        ParsedValueImpl<String,String> fxColorValue = new ParsedValueImpl<>(fxBase.getProperty(), null, true);
        Declaration fxColor = DeclarationShim.getDeclaration("-fx-color", fxColorValue, false);

        ParsedValueImpl<Color,Color> fxFillShouldNotMatchValue = new ParsedValueImpl<>(Color.RED, null);
        Declaration fxFillShouldNotMatch = DeclarationShim.getDeclaration("-fx-fill", fxFillShouldNotMatchValue, false);

        List<Selector> selectors = new ArrayList<>();
        Collections.addAll(selectors, root);

        List<Declaration> declarations = new ArrayList<>();
        Collections.addAll(declarations, fxBase, fxColor, fxFillShouldNotMatch);

        Rule baseRule = RuleShim.getRule(selectors, declarations);
        rules.add(baseRule);

        //
        // .rect { -fx-fill: -fx-color; }
        //
        List<String> rectStyleClass = new ArrayList<>();
        rectStyleClass.add("rect");

        Selector rect = new SimpleSelector("*", rectStyleClass, null, null);

        ParsedValue fxFillValue = new CssParserShim().parseExpr("-fx-fill", "-fx-color");
        Declaration fxFill = DeclarationShim.getDeclaration("-fx-fill", fxFillValue, false);

        selectors = new ArrayList<>();
        Collections.addAll(selectors, rect);

        declarations = new ArrayList<>();
        Collections.addAll(declarations, fxFill);

        Rule rectRule = RuleShim.getRule(selectors, declarations);
        rules.add(rectRule);

        // .rect:hover { -fx-fill: yellow; }
        List<String> pseudoclasses = new ArrayList<>();
        pseudoclasses.add("hover");

        Selector rectHover = new SimpleSelector("*", rectStyleClass, pseudoclasses, null);

        ParsedValueImpl<Color,Color> fxFillHoverValue = new ParsedValueImpl<>(Color.YELLOW, null);
        Declaration fxFillHover = DeclarationShim.getDeclaration("-fx-fill", fxFillHoverValue, false);

        selectors = new ArrayList<>();
        Collections.addAll(selectors, rectHover);

        declarations = new ArrayList<>();
        Collections.addAll(declarations, fxFillHover);

        Rule rectHoverRule = RuleShim.getRule(selectors, declarations);
        rules.add(rectHoverRule);

        List<Style> expecteds = new ArrayList<>();
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
        final List<Style> actuals = NodeHelper.getMatchingStyles(FILL, rectangle);

        //        System.err.println("matchingStyles: " + matchingStyles);
        //        System.err.println("expecteds: " + expecteds);
        //        System.err.println("actuals: " + actuals);

        assertEquals(expecteds.size(), actuals.size(), 0);

        for (Style style : expecteds) {
            if (!actuals.remove(style)) fail();
        }
        assertTrue(actuals.isEmpty());
    }

    @Disabled("JDK-8234142")
    @Test
    public void testGetMatchingStylesShouldNotReturnInlineAncestorPropertyIfNotInherited() {

        final Stylesheet stylesheet = StylesheetShim.getStylesheet();
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);

        final List<Rule> rules = stylesheet.getRules();

        List<String> rootStyleClass = new ArrayList<>();
        rootStyleClass.add("root");

        List<String> rectStyleClass = new ArrayList<>();
        rectStyleClass.add("rect");

        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        Selector root = new SimpleSelector("*", rootStyleClass, null, null);

        ParsedValue fxBaseValue = new CssParserShim().parseExpr("-fx-base", "red");
        Declaration fxBase = DeclarationShim.getDeclaration("-fx-base", fxBaseValue, false);

        ParsedValueImpl<String,String> fxColorValue = new ParsedValueImpl<>(fxBase.getProperty(), null, true);
        Declaration fxColor = DeclarationShim.getDeclaration("-fx-color", fxColorValue, false);

        ParsedValueImpl<Color,Color> fxFillShouldNotMatchValue = new ParsedValueImpl<>(Color.RED, null);
        Declaration fxFillShouldNotMatch = DeclarationShim.getDeclaration("-fx-fill", fxFillShouldNotMatchValue, false);

        List<Selector> selectors = new ArrayList<>();
        Collections.addAll(selectors, root);

        List<Declaration> declarations = new ArrayList<>();
        Collections.addAll(declarations, fxBase, fxColor, fxFillShouldNotMatch);

        Rule baseRule = RuleShim.getRule(selectors, declarations);
        rules.add(baseRule);

        //
        // .rect { -fx-fill: -fx-color; }
        //
        Selector rect = new SimpleSelector("*", rectStyleClass, null, null);

        ParsedValue fxFillValue = new CssParserShim().parseExpr("-fx-fill", "-fx-color");
        Declaration fxFill = DeclarationShim.getDeclaration("-fx-fill", fxFillValue, false);

        selectors = new ArrayList<>();
        Collections.addAll(selectors, rect);

        declarations = new ArrayList<>();
        Collections.addAll(declarations, fxFill);

        Rule rectRule = RuleShim.getRule(selectors, declarations);
        rules.add(rectRule);

        // .rect:hover { -fx-fill: yellow; }
        List<String> pseudoclasses = new ArrayList<>();
        pseudoclasses.add("hover");

        Selector rectHover = new SimpleSelector("*", rectStyleClass, pseudoclasses, null);

        ParsedValueImpl<Color,Color> fxFillHoverValue = new ParsedValueImpl<>(Color.YELLOW, null);
        Declaration fxFillHover = DeclarationShim.getDeclaration("-fx-fill", fxFillHoverValue, false);

        selectors = new ArrayList<>();
        Collections.addAll(selectors, rectHover);

        declarations = new ArrayList<>();
        Collections.addAll(declarations, fxFillHover);

        Rule rectHoverRule = RuleShim.getRule(selectors, declarations);
        rules.add(rectHoverRule);

        List<Style> expecteds = new ArrayList<>();
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
        final List<Style> actuals = NodeHelper.getMatchingStyles(FILL, rectangle);

        //        System.err.println("matchingStyles: " + matchingStyles);
        //        System.err.println("expecteds: " + expecteds);
        //        System.err.println("actuals: " + actuals);

        for (Style style : expecteds) {
            actuals.remove(style);
        }
        assertTrue(actuals.isEmpty(), actuals.toString());
    }

    @Disabled("JDK-8234142")
    @Test
    public void testGetMatchingStylesReturnsInheritedProperty() {

        final Stylesheet stylesheet = StylesheetShim.getStylesheet();
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);

        final List<Rule> rules = stylesheet.getRules();

        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        List<String> rootStyleClass = new ArrayList<>();
        rootStyleClass.add("root");

        Selector root = new SimpleSelector("*", rootStyleClass, null, null);

        ParsedValue<Color,Color> fxFontShouldInheritValue = new CssParserShim().parseExpr("-fx-font", "12px system");
        Declaration fxFontShouldInherit = DeclarationShim.getDeclaration("-fx-font", fxFontShouldInheritValue, false);

        List<Selector> selectors = new ArrayList<>();
        Collections.addAll(selectors, root);

        List<Declaration> declarations = new ArrayList<>();
        Collections.addAll(declarations, fxFontShouldInherit);

        Rule baseRule = RuleShim.getRule(selectors, declarations);
        rules.add(baseRule);

        //
        // .text { -fx-fill: -fx-color; }
        //
        List<String> textStyleClass = new ArrayList<>();
        textStyleClass.add("text");

        Selector textSelector = new SimpleSelector("*", textStyleClass, null, null);

        ParsedValue fxFillValue = new CssParserShim().parseExpr("-fx-fill", "red");
        Declaration fxFill = DeclarationShim.getDeclaration("-fx-fill", fxFillValue, false);

        selectors = new ArrayList<>();
        Collections.addAll(selectors, textSelector);

        declarations = new ArrayList<>();
        Collections.addAll(declarations, fxFill);

        Rule rectRule = RuleShim.getRule(selectors, declarations);
        rules.add(rectRule);

        List<Style> expecteds = new ArrayList<>();
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
        final List<Style> actuals = NodeHelper.getMatchingStyles(FONT, text);

        //        System.err.println("matchingStyles: " + matchingStyles);
        //        System.err.println("expecteds: " + expecteds);
        //        System.err.println("actuals: " + actuals);

        assertEquals(expecteds.size(), actuals.size(), 0);

        for (Style style : expecteds) {
            if (!actuals.remove(style)) fail();
        }
        assertTrue(actuals.isEmpty());
    }

    @Disabled("JDK-8234142")
    @Test
    public void testGetMatchingStylesReturnsSubProperty() {

        final Stylesheet stylesheet = StylesheetShim.getStylesheet();
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);

        final List<Rule> rules = stylesheet.getRules();

        //
        // .root { -fx-base: red; -fx-color: -fx-base; }
        //
        List<String> rootStyleClass = new ArrayList<>();
        rootStyleClass.add("root");

        Selector root = new SimpleSelector("*", rootStyleClass, null, null);

        ParsedValue<Color,Color> fxFontShouldInheritValue = new CssParserShim().parseExpr("-fx-font", "12px system");
        Declaration fxFontShouldInherit = DeclarationShim.getDeclaration("-fx-font", fxFontShouldInheritValue, false);

        List<Selector> selectors = new ArrayList<>();
        Collections.addAll(selectors, root);

        List<Declaration> declarations = new ArrayList<>();
        Collections.addAll(declarations, fxFontShouldInherit);

        Rule baseRule = RuleShim.getRule(selectors, declarations);
        rules.add(baseRule);

        //
        // .text { -fx-fill: -fx-color; }
        //
        List<String> rectStyleClass = new ArrayList<>();
        rectStyleClass.add("text");

        Selector textSelector = new SimpleSelector("*", rectStyleClass, null, null);

        ParsedValue fxFillValue = new CssParserShim().parseExpr("-fx-fill", "red");
        Declaration fxFill = DeclarationShim.getDeclaration("-fx-fill", fxFillValue, false);

        ParsedValue fxFontFamilyValue = new CssParserShim().parseExpr("-fx-font-family", "arial");
        Declaration fxFontFamily = DeclarationShim.getDeclaration("-fx-font-family", fxFontFamilyValue, false);

        selectors = new ArrayList<>();
        Collections.addAll(selectors, textSelector);

        declarations = new ArrayList<>();
        Collections.addAll(declarations, fxFill, fxFontFamily);

        Rule rectRule = RuleShim.getRule(selectors, declarations);
        rules.add(rectRule);

        List<Style> expecteds = new ArrayList<>();
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
        final List<Style> actuals = NodeHelper.getMatchingStyles(FONT, text);

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
            File f = System.getProperties().containsKey("CSS_META_DATA_TEST_DIR") ?
                    new File(System.getProperties().get("CSS_META_DATA_TEST_DIR").toString()) :
                    null;
            if (f == null) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                URL base = cl.getResource("javafx/../javafx");
                f = new File(base.toURI());
            }
            //System.err.println(f.getPath());
            assertTrue(f.isDirectory(), "" + f.getCanonicalPath() + " is not a directory");
            recursiveCheck(f, f.getPath().length() - 7);
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
                Method m = someClass.getMethod("getClassCssMetaData", (Class[]) null);
                Node node = (Node)someClass.getDeclaredConstructor().newInstance();
                List<CssMetaData<? extends Styleable, ?>> list = (List<CssMetaData<? extends Styleable, ?>>)m.invoke(null);
                if(list == null || list.isEmpty()) return;

                for (CssMetaData styleable : list) {

                    what = someClass.getName() + " " + styleable.getProperty();
                    WritableValue writable = styleable.getStyleableProperty(node);
                    assertNotNull(writable, what);

                    Object defaultValue = writable.getValue();
                    Object initialValue = styleable.getInitialValue((Node) someClass.getDeclaredConstructor().newInstance());

                    if (defaultValue instanceof Number) {
                        // 5 and 5.0 are not the same according to equals,
                        // but they should be...
                        assert(initialValue instanceof Number);
                        double d1 = ((Number)defaultValue).doubleValue();
                        double d2 = ((Number)initialValue).doubleValue();
                        assertEquals(d1, d2, .001, what);

                    } else if (defaultValue != null && defaultValue.getClass().isArray()) {
                        assertTrue(Arrays.equals((Object[])defaultValue, (Object[])initialValue), what);
                    } else {
                        assertEquals(defaultValue, initialValue, what);
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

    @Disabled("JDK-8234143") // Tested CssMetaData#set method, which is deprecated.
    @Test
    public void testRT_21185() {

        Color c1 = new Color(.1,.2,.3,1.0);
        Color c2 = new Color(.1,.2,.3,1.0);

        Rectangle rect = new Rectangle();
        rect.setFill(c1);

        StyleableProperty fill = (StyleableProperty)rect.fillProperty();
        StyleOrigin origin = ((StyleableProperty)rect.fillProperty()).getStyleOrigin();

        // set should not change the value if the values are equal and origin is same
        assertEquals(c1, c2);
        fill.applyStyle(origin, c2);

        assertSame(c1,rect.getFill()); // instance should not change.

        // set should change the value if the values are not equal.
        c2 = new Color(.3,.2,.1,1.0);
        fill.applyStyle(origin, c2);
        assertSame(c2,rect.getFill());

        // set should change the value if the origin is not the same
        fill.applyStyle(StyleOrigin.INLINE, c2);
        origin = ((StyleableProperty)rect.fillProperty()).getStyleOrigin();
        assertSame(StyleOrigin.INLINE, origin);

        // set should change the value if one is null and the other is not.
        rect.setFill(null);
        fill.applyStyle(origin, c2);
        assertSame(c2, rect.getFill());

        // set should change the value if one is null and the other is not
        fill.applyStyle(origin, null);
        assertNull(rect.getFill());

    }

    @Disabled("JDK-8234142")
    @Test
    public void testRT_24606() {

        final Stylesheet stylesheet = new CssParser().parse(
                ".root { -fx-base: red; }" +
                        ".group { -fx-color: -fx-base; }" +
                        ".text { -fx-fill: -fx-color; }"
        );
        stylesheet.setOrigin(StyleOrigin.USER_AGENT);
        StyleManager.getInstance().setDefaultUserAgentStylesheet(stylesheet);

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
        List list = NodeHelper.getMatchingStyles(prop, text);

        assertEquals(3, list.size(), 0);

    }

    @Test
    public void testStyleConverterReturnType() {
        final CssMetaData<Pane, TestEnum> TEST_ENUM =
                new CssMetaData<>("-test-enum", StyleConverter.getEnumConverter(TestEnum.class), TestEnum.LEFT, false) {
                    @Override
                    public boolean isSettable(Pane styleable) {
                        return false;
                    }

                    @Override
                    public StyleableProperty<TestEnum> getStyleableProperty(Pane styleable) {
                        return null;
                    }
                };
    }

}
