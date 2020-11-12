/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import com.sun.javafx.scene.SceneHelper;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertPseudoClassDoesNotExist;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertPseudoClassExists;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.StyleableProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import org.junit.Before;
import org.junit.Test;

import test.com.sun.javafx.pgstub.StubToolkit;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventGenerator;
import javafx.scene.control.skin.TitledPaneSkin;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;

/**
 *
 * @author srikalyc
 */
public class TitledPaneTest {
    private TitledPane titledPane;//Empty string
    private TitledPane titledPaneWithTitleAndNode;//With title And Node
    private Node node;
    private Toolkit tk;
    private Scene scene;
    private Stage stage;
    private StackPane root;

    @Before public void setup() {
        node = new Rectangle();
        tk = (StubToolkit)Toolkit.getToolkit();//This step is not needed (Just to make sure StubToolkit is loaded into VM)
        titledPane = new TitledPane();
        titledPane.setSkin(new TitledPaneSkin(titledPane));
        titledPaneWithTitleAndNode = new TitledPane("title", node);
        root = new StackPane();
        scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);
    }

    /*********************************************************************
     * Helper methods                                                    *
     ********************************************************************/
    private void show() {
        stage.show();
        stage.requestFocus();
    }

    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/

    @Test public void defaultConstructorShouldSetStyleClassTo_titledpane() {
        assertStyleClassContains(titledPane, "titled-pane");
    }

    @Test public void defaultConstructorShouldEmptyTitleAndNullContent() {
        assertEquals(titledPane.textProperty().get(), "");
        assertNull(titledPane.contentProperty().get());
    }

    @Test public void twoArgConstructorShouldSetStyleClassTo_titledpane() {
        assertStyleClassContains(titledPane, "titled-pane");
    }

    @Test public void twoArgConstructorShouldEmptyTitleAndNullContent() {
        assertEquals(titledPaneWithTitleAndNode.textProperty().get(), "title");
        assertSame(titledPaneWithTitleAndNode.contentProperty().getValue(), node);
    }

    @Test public void defaultExpandedIsTrue() {
        assertTrue(titledPane.isExpanded());
    }

    @Test public void defaultAnimated() {
        assertTrue(titledPane.isAnimated());
    }

    @Test public void defaultCollapsible() {
        assertTrue(titledPane.isCollapsible());
    }


    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/

    @Test public void checkContentPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<Node>(null);
        titledPane.contentProperty().bind(objPr);
        assertEquals("ContentProperty cannot be bound", titledPane.contentProperty().getValue(), null);
        Node nde = new Rectangle();
        objPr.setValue(nde);
        assertEquals("ContentProperty cannot be bound", titledPane.contentProperty().getValue(), nde);
    }

    @Test public void checkExpandedPropertyBind() {
        BooleanProperty objPr = new SimpleBooleanProperty(true);
        titledPane.expandedProperty().bind(objPr);
        assertEquals("Expanded cannot be bound", titledPane.expandedProperty().getValue(), true);
        objPr.setValue(false);
        assertEquals("Expanded cannot be bound", titledPane.expandedProperty().getValue(), false);
    }

    @Test public void checkAnimatedPropertyBind() {
        BooleanProperty objPr = new SimpleBooleanProperty(true);
        titledPane.animatedProperty().bind(objPr);
        assertEquals("Animated cannot be bound", titledPane.animatedProperty().getValue(), true);
        objPr.setValue(false);
        assertEquals("Animated cannot be bound", titledPane.animatedProperty().getValue(), false);
    }

    @Test public void checkCollapsiblePropertyBind() {
        BooleanProperty objPr = new SimpleBooleanProperty(true);
        titledPane.collapsibleProperty().bind(objPr);
        assertEquals("Collapsible cannot be bound", titledPane.collapsibleProperty().getValue(), true);
        objPr.setValue(false);
        assertEquals("Collapsible cannot be bound", titledPane.collapsibleProperty().getValue(), false);
    }


    @Test public void contentPropertyHasBeanReference() {
        assertSame(titledPane, titledPane.contentProperty().getBean());
    }

    @Test public void contenPropertyHasName() {
        assertEquals("content", titledPane.contentProperty().getName());
    }

    @Test public void animatedPropertyHasBeanReference() {
        assertSame(titledPane, titledPane.animatedProperty().getBean());
    }

    @Test public void animatedPropertyHasName() {
        assertEquals("animated", titledPane.animatedProperty().getName());
    }

    @Test public void collapsiblePropertyHasBeanReference() {
        assertSame(titledPane, titledPane.collapsibleProperty().getBean());
    }

    @Test public void collapsiblePropertyHasName() {
        assertEquals("collapsible", titledPane.collapsibleProperty().getName());
    }


    /*********************************************************************
     * Check for Pseudo classes                                          *
     ********************************************************************/
    @Test public void settingExpandedTrueSetsPseudoClass() {
        titledPane.setExpanded(true);
        assertPseudoClassExists(titledPane, "expanded");
        assertPseudoClassDoesNotExist(titledPane, "collapsed");
    }

    @Test public void clearingExpandedClearsPseudoClass() {
        titledPane.setExpanded(true);
        titledPane.setExpanded(false);
        assertPseudoClassDoesNotExist(titledPane, "expanded");
        assertPseudoClassExists(titledPane, "collapsed");
    }


    /*********************************************************************
     * CSS related Tests                                                 *
     ********************************************************************/
    @Test public void whenAnimatedIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)titledPane.animatedProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(titledPane));
        BooleanProperty other = new SimpleBooleanProperty();
        titledPane.animatedProperty().bind(other);
        assertFalse(styleable.isSettable(titledPane));
    }

    @Test public void whenAnimatedIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)titledPane.animatedProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(titledPane));
    }

    @Test public void whenCollapsibleIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)titledPane.collapsibleProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(titledPane));
        BooleanProperty other = new SimpleBooleanProperty();
        titledPane.collapsibleProperty().bind(other);
        assertFalse(styleable.isSettable(titledPane));
    }

    @Test public void whenCollapsibleIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)titledPane.collapsibleProperty()).getCssMetaData();
        ((StyleableProperty)titledPane.collapsibleProperty()).applyStyle(null, Boolean.FALSE);
        assertTrue(styleable.isSettable(titledPane));
    }



    /*********************************************************************
     * Miscellaneous Tests                                               *
     ********************************************************************/
    @Test public void setContentAndSeeValueIsReflectedInModel() {
        Node nde = new Rectangle();
        titledPane.setContent(nde);
        assertSame(titledPane.contentProperty().getValue(), nde);
    }

    @Test public void setContentAndSeeValue() {
        Node nde = new Rectangle();
        titledPane.setContent(nde);
        assertSame(titledPane.getContent(), nde);
    }

    @Test public void setExpandedAndSeeValueIsReflectedInModel() {
        titledPane.setExpanded(true);
        assertTrue(titledPane.expandedProperty().getValue());
    }

    @Test public void setExpandedAndSeeValue() {
        titledPane.setExpanded(false);
        assertFalse(titledPane.isExpanded());
    }

    @Test public void setAnimatedAndSeeValueIsReflectedInModel() {
        titledPane.setAnimated(true);
        assertTrue(titledPane.animatedProperty().getValue());
    }

    @Test public void setAnimatedAndSeeValue() {
        titledPane.setAnimated(false);
        assertFalse(titledPane.isAnimated());
    }

    @Test public void setCollapsibleAndSeeValueIsReflectedInModel() {
        titledPane.setCollapsible(true);
        assertTrue(titledPane.collapsibleProperty().getValue());
    }

    @Test public void setCollapsibleAndSeeValue() {
        titledPane.setCollapsible(false);
        assertFalse(titledPane.isCollapsible());
    }

    @Test public void setAlignment_RT20069() {
        titledPane.setExpanded(false);
        titledPane.setAnimated(false);
        titledPane.setStyle("-fx-alignment: BOTTOM_RIGHT;");

        root.getChildren().add(titledPane);
        show();

        assertEquals(Pos.BOTTOM_RIGHT, titledPane.getAlignment());
    }

    @Test public void keyboardFocusOnNonCollapsibleTitledPane_RT19660() {
        Button button = new Button("Button");

        titledPane.setCollapsible(false);
        titledPane.setExpanded(true);
        titledPane.setAnimated(false);
        titledPane.setContent(button);

        root.getChildren().add(titledPane);
        show();

        tk.firePulse();
        assertTrue(titledPane.isFocused());

        KeyEventFirer keyboard = new KeyEventFirer(titledPane);
        keyboard.doKeyPress(KeyCode.ENTER);

        tk.firePulse();
        assertTrue(titledPane.isExpanded());
        assertTrue(titledPane.isFocused());

        keyboard.doKeyPress(KeyCode.TAB);
        tk.firePulse();
        assertFalse(titledPane.isFocused());
        assertTrue(button.isFocused());
    }

    @Test public void mouseFocusOnNonCollapsibleTitledPane_RT19660() {
        Button button = new Button("Button");

        titledPane.setCollapsible(false);
        titledPane.setExpanded(true);
        titledPane.setAnimated(false);
        titledPane.setContent(button);

        root.getChildren().add(titledPane);
        show();

        tk.firePulse();
        assertTrue(titledPane.isFocused());

        double xval = (titledPane.localToScene(titledPane.getLayoutBounds())).getMinX();
        double yval = (titledPane.localToScene(titledPane.getLayoutBounds())).getMinY();

        final MouseEventGenerator generator = new MouseEventGenerator();
        SceneHelper.processMouseEvent(scene,
            generator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, xval+20, yval+20));
        SceneHelper.processMouseEvent(scene,
            generator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, xval+20, yval+20));

        tk.firePulse();
        assertTrue(titledPane.isExpanded());
        assertTrue(titledPane.isFocused());

        KeyEventFirer keyboard = new KeyEventFirer(titledPane);
        keyboard.doKeyPress(KeyCode.TAB);
        tk.firePulse();
        assertFalse(titledPane.isFocused());
        assertTrue(button.isFocused());
    }
}
