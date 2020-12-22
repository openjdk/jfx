/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import javafx.css.CssMetaData;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.*;

import test.com.sun.javafx.pgstub.StubToolkit;
import javafx.scene.control.skin.SplitPaneSkin;
import com.sun.javafx.tk.Toolkit;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.StyleableProperty;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import static org.junit.Assert.*;


import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author srikalyc
 */
public class SplitPaneTest {
    private SplitPane splitPane;//Empty string
    private SplitPane.Divider divider1;
    private SplitPane.Divider divider2;
    private Toolkit tk;
    private Scene scene;
    private Stage stage;
    private StackPane root;

    @Before public void setup() {
        tk = (StubToolkit)Toolkit.getToolkit();//This step is not needed (Just to make sure StubToolkit is loaded into VM)
        splitPane = new SplitPane();
        splitPane.setSkin(new SplitPaneSkin(splitPane));
        divider1 = new SplitPane.Divider();
        divider2 = new SplitPane.Divider();

        root = new StackPane();
        scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);
    }

    /*********************************************************************
     * Helper methods (NOTE TESTS)                                       *
     ********************************************************************/
    private void add2NodesToSplitPane() {
        splitPane.getItems().add(new Button("Button One"));
        splitPane.getItems().add(new Button("Button Two"));
    }
    private void add3NodesToSplitPane() {
        add2NodesToSplitPane();
        splitPane.getItems().add(new Button("Button Three"));
    }

    private void add4NodesToSplitPane() {
        add3NodesToSplitPane();
        splitPane.getItems().add(new Button("Button Four"));
    }

    private void show() {
        stage.show();
    }


    private double convertDividerPostionToAbsolutePostion(double pos, double edge) {
        return (Math.round(pos * edge)) - 3;  // 3 is half the divider width.
    }

    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/

    @Test public void defaultConstructorShouldSetStyleClassTo_splitpane() {
        assertStyleClassContains(splitPane, "split-pane");
    }

    @Test public void defaultFocusTraversibleIsFalse() {
        assertFalse(splitPane.isFocusTraversable());
    }

    @Test public void defaultOrientation() {
        assertSame(splitPane.getOrientation(), Orientation.HORIZONTAL);
    }

    @Test public void defaultDividerPosition() {
        assertEquals(divider1.getPosition(), 0.5, 0.0);
    }

    @Test public void defaultPositionOf_N_DividersAddedToSplitPaneWhenNewNodeAreAdded() {
        add4NodesToSplitPane();
        assertEquals(splitPane.getDividers().get(0).getPosition(), 0.5, 0.0);
        assertEquals(splitPane.getDividers().get(1).getPosition(), 0.5, 0.0);
        assertEquals(splitPane.getDividers().get(1).getPosition(), 0.5, 0.0);
    }

    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/

    @Test public void checkHBarPolicyPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<Orientation>(Orientation.VERTICAL);
        splitPane.orientationProperty().bind(objPr);
        assertSame("orientationProperty cannot be bound", splitPane.orientationProperty().getValue(), Orientation.VERTICAL);
        objPr.setValue(Orientation.HORIZONTAL);
        assertSame("orientationProperty cannot be bound", splitPane.orientationProperty().getValue(), Orientation.HORIZONTAL);
    }

    @Test public void checkDividerPositionPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(0.6);
        divider1.positionProperty().bind(objPr);
        assertEquals("positionProperty cannot be bound", divider1.positionProperty().getValue(), 0.6, 0.0);
        objPr.setValue(0.9);
        assertEquals("positionProperty cannot be bound", divider1.positionProperty().getValue(), 0.9, 0.0);
    }

    @Test public void checkOrientationPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<Orientation>(Orientation.HORIZONTAL);
        splitPane.orientationProperty().bind(objPr);
        assertSame("orientationProperty cannot be bound", splitPane.orientationProperty().getValue(), Orientation.HORIZONTAL);
        objPr.setValue(Orientation.VERTICAL);
        assertSame("orientationProperty cannot be bound", splitPane.orientationProperty().getValue(), Orientation.VERTICAL);
    }

    @Test public void orientationPropertyHasBeanReference() {
        assertSame(splitPane, splitPane.orientationProperty().getBean());
    }

    @Test public void orientationPropertyHasName() {
        assertEquals("orientation", splitPane.orientationProperty().getName());
    }

    @Test public void positionPropertyHasBeanReference() {
        assertSame(divider1, divider1.positionProperty().getBean());
    }

    @Test public void positionPropertyHasName() {
        assertEquals("position", divider1.positionProperty().getName());
    }



    /*********************************************************************
     * Check for Pseudo classes                                          *
     ********************************************************************/
    @Test public void settingVerticalOrientationSetsVerticalPseudoClass() {
        splitPane.setOrientation(Orientation.VERTICAL);
        assertPseudoClassExists(splitPane, "vertical");
        assertPseudoClassDoesNotExist(splitPane, "horizontal");
    }

    @Test public void clearingVerticalOrientationClearsVerticalPseudoClass() {
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        assertPseudoClassDoesNotExist(splitPane, "vertical");
        assertPseudoClassExists(splitPane, "horizontal");
    }

    @Test public void settingHorizontalOrientationSetsHorizontalPseudoClass() {
        splitPane.setOrientation(Orientation.HORIZONTAL);
        assertPseudoClassExists(splitPane, "horizontal");
        assertPseudoClassDoesNotExist(splitPane, "vertical");
    }

    @Test public void clearingHorizontalOrientationClearsHorizontalPseudoClass() {
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setOrientation(Orientation.VERTICAL);
        assertPseudoClassDoesNotExist(splitPane, "horizontal");
        assertPseudoClassExists(splitPane, "vertical");
    }



    /*********************************************************************
     * CSS related Tests                                                 *
     ********************************************************************/
    @Test public void whenOrientationIsBound_CssMetaData_isSettable_ReturnsFalse() {
        CssMetaData styleable = ((StyleableProperty)splitPane.orientationProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(splitPane));
        ObjectProperty<Orientation> other = new SimpleObjectProperty<Orientation>(Orientation.VERTICAL);
        splitPane.orientationProperty().bind(other);
        assertFalse(styleable.isSettable(splitPane));
    }

    @Test public void whenOrientationIsSpecifiedViaCSSAndIsNotBound_CssMetaData_isSettable_ReturnsTrue() {
        CssMetaData styleable = ((StyleableProperty)splitPane.orientationProperty()).getCssMetaData();
        assertTrue(styleable.isSettable(splitPane));
    }

    @Test public void canSpecifyOrientationViaCSS() {
        ((StyleableProperty)splitPane.orientationProperty()).applyStyle(null, Orientation.VERTICAL);
        assertSame(Orientation.VERTICAL, splitPane.getOrientation());
    }

    /*********************************************************************
     * Miscellaneous Tests                                         *
     ********************************************************************/
    @Test public void setOrientationAndSeeValueIsReflectedInModel() {
        splitPane.setOrientation(Orientation.HORIZONTAL);
        assertSame(splitPane.orientationProperty().getValue(), Orientation.HORIZONTAL);
    }

    @Test public void setOrientationAndSeeValue() {
        splitPane.setOrientation(Orientation.VERTICAL);
        assertSame(splitPane.getOrientation(), Orientation.VERTICAL);
    }

    @Test public void setPositionAndSeeValueIsReflectedInModel() {
        divider1.setPosition(0.2);
        assertEquals(divider1.positionProperty().getValue(), 0.2, 0.0);
    }

    @Test public void setPositionAndSeeValue() {
        divider1.setPosition(0.3);
        assertEquals(divider1.getPosition(), 0.3, 0.0);
    }

    @Test public void addingNnodesToSplitPaneCreatesNminus1Dividers() {
        add3NodesToSplitPane();
        assertNotNull(splitPane.getDividers());
        assertEquals(splitPane.getDividers().size(), 2, 0.0);
    }

    @Test public void setMultipleDividerPositionsAndValidate() {
        add3NodesToSplitPane();
        splitPane.setDividerPosition(0, 0.4);
        splitPane.setDividerPosition(1, 0.6);
        assertNotNull(splitPane.getDividers());
        assertEquals(splitPane.getDividers().size(), 2, 0.0);
        assertEquals(splitPane.getDividers().get(0).getPosition(), 0.4, 0.0);
        assertEquals(splitPane.getDividers().get(1).getPosition(), 0.6, 0.0);
    }

    @Test public void addingNonExistantDividerPositionToSplitPaneCachesItAndAppliesWhenNewNodeAreAdded() {
        add2NodesToSplitPane();
        splitPane.setDividerPosition(2, 0.4);//2 is a non existant divider position, but still position value 0.4 is cached

        splitPane.getItems().add(new Button("Button Three"));
        splitPane.getItems().add(new Button("Button Four"));
        assertNotNull(splitPane.getDividers());
        assertEquals(splitPane.getDividers().size(), 3, 0.0);
        assertEquals(splitPane.getDividers().get(2).getPosition(), 0.4, 0.0);
    }

    @Test public void zeroDivider() {
        StackPane spCenter = new StackPane();
        splitPane.getItems().addAll(spCenter);

        root.setPrefSize(400, 400);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        assertEquals(0, splitPane.getDividers().size());
        assertEquals(398, spCenter.getLayoutBounds().getWidth(), 1e-100);
    }

    @Test public void oneDividerPanelsAreEquallySized() {
        StackPane spLeft = new StackPane();
        StackPane spRight = new StackPane();

        splitPane.getItems().addAll(spLeft, spRight);

        root.setPrefSize(400, 400);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double w = 398; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], w);

        assertEquals(196, p0, 1e-100);
        assertEquals(196, spLeft.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(196, spRight.getLayoutBounds().getWidth(), 1e-100);
    }

    @Test public void twoDividersHaveTheSamePosition() {
        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        splitPane.getItems().addAll(spLeft, spCenter, spRight);

        root.setPrefSize(400, 400);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double w = 398; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], w);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], w);

        assertEquals(196, p0, 1e-100);
        assertEquals(202, p1, 1e-100);
        assertEquals(196, spLeft.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(0, spCenter.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(190, spRight.getLayoutBounds().getWidth(), 1e-100);
    }

    @Test public void twoDividersHaveTheDifferentPositions() {
        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        splitPane.setDividerPosition(0, 0.20);
        splitPane.setDividerPosition(1, 0.80);
        splitPane.getItems().addAll(spLeft, spCenter, spRight);

        root.setPrefSize(400, 400);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double w = 398; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], w);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], w);

        assertEquals(77, p0, 1e-100);
        assertEquals(315, p1, 1e-100);
        assertEquals(77, spLeft.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(232, spCenter.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(77, spRight.getLayoutBounds().getWidth(), 1e-100);
    }

    @Test public void threePanelsAllAreSetToMin() {
        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        spLeft.setMinWidth(28);
        spCenter.setMinWidth(29);
        spRight.setMinWidth(29);

        splitPane.setDividerPosition(0, 0.20);
        splitPane.setDividerPosition(1, 0.80);
        splitPane.getItems().addAll(spLeft, spCenter, spRight);

        root.setPrefSize(100, 100);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double w = 98; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], w);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], w);

        assertEquals(28, p0, 1e-100);
        assertEquals(63, p1, 1e-100);
        assertEquals(28, spLeft.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(29, spCenter.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(29, spRight.getLayoutBounds().getWidth(), 1e-100);
    }

    @Test public void threePanelsAllAreSetToMax() {
        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        spLeft.setMaxWidth(28);
        spCenter.setMaxWidth(29);
        spRight.setMaxWidth(29);

        splitPane.setDividerPosition(0, 0.20);
        splitPane.setDividerPosition(1, 0.80);
        splitPane.getItems().addAll(spLeft, spCenter, spRight);

        root.setPrefSize(100, 100);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double w = 98; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], w);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], w);

        assertEquals(28, p0, 1e-100);
        assertEquals(63, p1, 1e-100);
        assertEquals(28, spLeft.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(29, spCenter.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(29, spRight.getLayoutBounds().getWidth(), 1e-100);
    }

    @Test public void threePanelsSetToMinMaxMin() {
        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        spLeft.setMinWidth(28);
        spCenter.setMaxWidth(29);
        spRight.setMinWidth(29);

        splitPane.setDividerPosition(0, 0.20);
        splitPane.setDividerPosition(1, 0.80);
        splitPane.getItems().addAll(spLeft, spCenter, spRight);

        root.setPrefSize(100, 100);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double w = 98; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], w);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], w);

        assertEquals(28, p0, 1e-100);
        assertEquals(63, p1, 1e-100);
        assertEquals(28, spLeft.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(29, spCenter.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(29, spRight.getLayoutBounds().getWidth(), 1e-100);
    }

    @Test public void setDividerLessThanMin() {
        StackPane spLeft = new StackPane();
        StackPane spRight = new StackPane();

        spLeft.setMinWidth(80);
        splitPane.getItems().addAll(spLeft, spRight);
        splitPane.setDividerPositions(0);

        root.setPrefSize(100, 100);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double w = 98; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], w);

        assertEquals(80, p0, 1e-100);
        assertEquals(80, spLeft.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(12, spRight.getLayoutBounds().getWidth(), 1e-100);
    }

    @Test public void setDividerGreaterThanMax() {
        StackPane spLeft = new StackPane();
        StackPane spRight = new StackPane();

        spLeft.setMaxWidth(80);
        splitPane.getItems().addAll(spLeft, spRight);
        splitPane.setDividerPositions(1.5);

        root.setPrefSize(100, 100);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double w = 98; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], w);

        assertEquals(80, p0, 1e-100);
        assertEquals(80, spLeft.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(12, spRight.getLayoutBounds().getWidth(), 1e-100);
    }

    @Test public void setTwoDividerGreaterThanMax() {
        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        splitPane.getItems().addAll(spLeft, spCenter, spRight);
        splitPane.setDividerPositions(1.5, 1.5);

        root.setPrefSize(100, 100);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double w = 98; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], w);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], w);

        assertEquals(86, p0, 1e-100);
        assertEquals(92, p1, 1e-100);
        assertEquals(86, spLeft.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(0, spCenter.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(0, spRight.getLayoutBounds().getWidth(), 1e-100);
    }

    @Test public void checkDividerPositions_RT18805() {
        Button l = new Button("Left Button");
        Button c = new Button("Center Button");
        Button r = new Button("Left Button");

        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        spLeft.getChildren().add(l);
        spCenter.getChildren().add(c);
        spRight.getChildren().add(r);

        spLeft.setMinWidth(100);
        spLeft.setMaxWidth(150);
        spRight.setMaxWidth(100);
        spRight.setMaxWidth(150);

        splitPane.getItems().addAll(spLeft, spCenter, spRight);

        root.setPrefSize(600, 400);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double w = 598; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], w);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], w);

        assertEquals(150, p0, 1e-100);
        assertEquals(442, p1, 1e-100);
        assertEquals(150, spLeft.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(286, spCenter.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(150, spRight.getLayoutBounds().getWidth(), 1e-100);
    }

    @Test public void growSplitPaneBy5px_RT18855() {
        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        spLeft.setMinWidth(77);
        spRight.setMinWidth(77);

        splitPane.setDividerPosition(0, 0.20);
        splitPane.setDividerPosition(1, 0.80);
        splitPane.getItems().addAll(spLeft, spCenter, spRight);

        root.setPrefSize(400, 400);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double w = 398; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], w);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], w);

        assertEquals(77, p0, 1e-100);
        assertEquals(315, p1, 1e-100);
        assertEquals(77, spLeft.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(232, spCenter.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(77, spRight.getLayoutBounds().getWidth(), 1e-100);

        root.applyCss();
        root.resize(405, 400);
        root.layout();

        w = 403;
        pos = splitPane.getDividerPositions();
        p0 = convertDividerPostionToAbsolutePostion(pos[0], w);
        p1 = convertDividerPostionToAbsolutePostion(pos[1], w);

        assertEquals(78, p0, 1e-100);
        assertEquals(319, p1, 1e-100);
        assertEquals(78, spLeft.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(235, spCenter.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(78, spRight.getLayoutBounds().getWidth(), 1e-100);
    }

    @Test public void growSplitPaneBy5pxWithFixedDividers_RT18806() {
        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        spLeft.setMinWidth(77);
        spRight.setMinWidth(77);

        splitPane.setDividerPosition(0, 0.20);
        splitPane.setDividerPosition(1, 0.80);
        splitPane.getItems().addAll(spLeft, spCenter, spRight);

        SplitPane.setResizableWithParent(spLeft, false);
        SplitPane.setResizableWithParent(spRight, false);

        root.setPrefSize(400, 400);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double w = 398; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], w);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], w);

        assertEquals(77, p0, 1e-100);
        assertEquals(315, p1, 1e-100);
        assertEquals(77, spLeft.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(232, spCenter.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(77, spRight.getLayoutBounds().getWidth(), 1e-100);

        root.applyCss();
        root.resize(405, 400);
        root.layout();

        w = 403;
        pos = splitPane.getDividerPositions();
        p0 = convertDividerPostionToAbsolutePostion(pos[0], w);
        p1 = convertDividerPostionToAbsolutePostion(pos[1], w);

        assertEquals(77, p0, 1e-100);
        assertEquals(320, p1, 1e-100);
        assertEquals(77, spLeft.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(237, spCenter.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(77, spRight.getLayoutBounds().getWidth(), 1e-100);
    }

    @Test public void resizeSplitPaneAllPanesAreSetToMax() {
        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        spLeft.setMaxWidth(28);
        spCenter.setMaxWidth(29);
        spRight.setMaxWidth(29);

        splitPane.setDividerPosition(0, 0.20);
        splitPane.setDividerPosition(1, 0.80);
        splitPane.getItems().addAll(spLeft, spCenter, spRight);

        root.setPrefSize(100, 100);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double w = 98; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], w);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], w);

        assertEquals(28, p0, 1e-100);
        assertEquals(63, p1, 1e-100);
        assertEquals(28, spLeft.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(29, spCenter.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(29, spRight.getLayoutBounds().getWidth(), 1e-100);

        root.applyCss();
        root.resize(405, 400);
        root.layout();

        w = 403;
        pos = splitPane.getDividerPositions();
        p0 = convertDividerPostionToAbsolutePostion(pos[0], w);
        p1 = convertDividerPostionToAbsolutePostion(pos[1], w);

        assertEquals(28, p0, 1e-100);
        assertEquals(63, p1, 1e-100);
        assertEquals(28, spLeft.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(29, spCenter.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(29, spRight.getLayoutBounds().getWidth(), 1e-100);
    }

    /*
     * Vertical SplitPane
     */
    @Test public void oneDividerPanelsAreEquallySized_VerticalSplitPane() {
        StackPane spLeft = new StackPane();
        StackPane spRight = new StackPane();

        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().addAll(spLeft, spRight);

        root.setPrefSize(400, 400);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double h = 398; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], h);

        assertEquals(196, p0, 1e-100);
        assertEquals(196, spLeft.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(196, spRight.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void twoDividersHaveTheSamePosition_VerticalSplitPane() {
        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().addAll(spLeft, spCenter, spRight);

        root.setPrefSize(400, 400);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double h = 398; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], h);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], h);

        assertEquals(196, p0, 1e-100);
        assertEquals(202, p1, 1e-100);
        assertEquals(196, spLeft.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, spCenter.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(190, spRight.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void twoDividersHaveTheDifferentPositions_VerticalSplitPane() {
        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPosition(0, 0.20);
        splitPane.setDividerPosition(1, 0.80);
        splitPane.getItems().addAll(spLeft, spCenter, spRight);

        root.setPrefSize(400, 400);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double h = 398; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], h);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], h);

        assertEquals(77, p0, 1e-100);
        assertEquals(315, p1, 1e-100);
        assertEquals(77, spLeft.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(232, spCenter.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(77, spRight.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void threePanelsAllAreSetToMin_VerticalSplitPane() {
        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        spLeft.setMinHeight(28);
        spCenter.setMinHeight(29);
        spRight.setMinHeight(29);

        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPosition(0, 0.20);
        splitPane.setDividerPosition(1, 0.80);
        splitPane.getItems().addAll(spLeft, spCenter, spRight);

        root.setPrefSize(100, 100);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double h = 98; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], h);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], h);

        assertEquals(28, p0, 1e-100);
        assertEquals(63, p1, 1e-100);
        assertEquals(28, spLeft.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(29, spCenter.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(29, spRight.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void threePanelsAllAreSetToMax_VerticalSplitPane() {
        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        spLeft.setMaxHeight(28);
        spCenter.setMaxHeight(29);
        spRight.setMaxHeight(29);

        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPosition(0, 0.20);
        splitPane.setDividerPosition(1, 0.80);
        splitPane.getItems().addAll(spLeft, spCenter, spRight);

        root.setPrefSize(100, 100);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double h = 98; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], h);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], h);

        assertEquals(28, p0, 1e-100);
        assertEquals(63, p1, 1e-100);
        assertEquals(28, spLeft.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(29, spCenter.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(29, spRight.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void threePanelsSetToMinMaxMin_VerticalSplitPane() {
        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        spLeft.setMinHeight(28);
        spCenter.setMaxHeight(29);
        spRight.setMinHeight(29);

        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPosition(0, 0.20);
        splitPane.setDividerPosition(1, 0.80);
        splitPane.getItems().addAll(spLeft, spCenter, spRight);

        root.setPrefSize(100, 100);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double h = 98; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], h);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], h);

        assertEquals(28, p0, 1e-100);
        assertEquals(63, p1, 1e-100);
        assertEquals(28, spLeft.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(29, spCenter.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(29, spRight.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void setDividerLessThanMin_VerticalSplitPane() {
        StackPane spLeft = new StackPane();
        StackPane spRight = new StackPane();

        spLeft.setMinHeight(80);

        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().addAll(spLeft, spRight);
        splitPane.setDividerPositions(0);

        root.setPrefSize(100, 100);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double h = 98; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], h);

        assertEquals(80, p0, 1e-100);
        assertEquals(80, spLeft.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(12, spRight.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void setDividerGreaterThanMax_VerticalSplitPane() {
        StackPane spLeft = new StackPane();
        StackPane spRight = new StackPane();

        spLeft.setMaxHeight(80);

        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().addAll(spLeft, spRight);
        splitPane.setDividerPositions(1.5);

        root.setPrefSize(100, 100);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double h = 98; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], h);

        assertEquals(80, p0, 1e-100);
        assertEquals(80, spLeft.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(12, spRight.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void setTwoDividerGreaterThanMax_VerticalSplitPane() {
        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().addAll(spLeft, spCenter, spRight);
        splitPane.setDividerPositions(1.5, 1.5);

        root.setPrefSize(100, 100);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double h = 98; // The height minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], h);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], h);

        assertEquals(86, p0, 1e-100);
        assertEquals(92, p1, 1e-100);
        assertEquals(86, spLeft.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, spCenter.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(0, spRight.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void checkDividerPositions_RT18805_VerticalSplitPane() {
        Button l = new Button("Left Button");
        Button c = new Button("Center Button");
        Button r = new Button("Left Button");

        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        spLeft.getChildren().add(l);
        spCenter.getChildren().add(c);
        spRight.getChildren().add(r);

        spLeft.setMinHeight(100);
        spLeft.setMaxHeight(150);
        spRight.setMaxHeight(100);
        spRight.setMaxHeight(150);

        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().addAll(spLeft, spCenter, spRight);

        root.setPrefSize(400, 600);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double h = 598; // The height minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], h);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], h);

        assertEquals(150, p0, 1e-100);
        assertEquals(442, p1, 1e-100);
        assertEquals(150, spLeft.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(286, spCenter.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(150, spRight.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void growSplitPaneBy5px_RT18855_VerticalSplitPane() {
        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        spLeft.setMinHeight(77);
        spRight.setMinHeight(77);

        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPosition(0, 0.20);
        splitPane.setDividerPosition(1, 0.80);
        splitPane.getItems().addAll(spLeft, spCenter, spRight);

        root.setPrefSize(400, 400);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double h = 398; // The height minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], h);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], h);

        assertEquals(77, p0, 1e-100);
        assertEquals(315, p1, 1e-100);
        assertEquals(77, spLeft.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(232, spCenter.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(77, spRight.getLayoutBounds().getHeight(), 1e-100);

        root.applyCss();
        root.resize(400, 405);
        root.layout();

        h = 403;
        pos = splitPane.getDividerPositions();
        p0 = convertDividerPostionToAbsolutePostion(pos[0], h);
        p1 = convertDividerPostionToAbsolutePostion(pos[1], h);

        assertEquals(78, p0, 1e-100);
        assertEquals(319, p1, 1e-100);
        assertEquals(78, spLeft.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(235, spCenter.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(78, spRight.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void growSplitPaneBy5pxWithFixedDividers_RT18806_VerticalSplitPane() {
        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        spLeft.setMinHeight(77);
        spRight.setMinHeight(77);

        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPosition(0, 0.20);
        splitPane.setDividerPosition(1, 0.80);
        splitPane.getItems().addAll(spLeft, spCenter, spRight);

        SplitPane.setResizableWithParent(spLeft, false);
        SplitPane.setResizableWithParent(spRight, false);

        root.setPrefSize(400, 400);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double h = 398; // The height minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], h);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], h);

        assertEquals(77, p0, 1e-100);
        assertEquals(315, p1, 1e-100);
        assertEquals(77, spLeft.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(232, spCenter.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(77, spRight.getLayoutBounds().getHeight(), 1e-100);

        root.applyCss();
        root.resize(400, 405);
        root.layout();

        h = 403;
        pos = splitPane.getDividerPositions();
        p0 = convertDividerPostionToAbsolutePostion(pos[0], h);
        p1 = convertDividerPostionToAbsolutePostion(pos[1], h);

        assertEquals(77, p0, 1e-100);
        assertEquals(320, p1, 1e-100);
        assertEquals(77, spLeft.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(237, spCenter.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(77, spRight.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void resizeSplitPaneAllPanesAreSetToMax_VerticalSplitPane() {
        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        spLeft.setMaxHeight(28);
        spCenter.setMaxHeight(29);
        spRight.setMaxHeight(29);

        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPosition(0, 0.20);
        splitPane.setDividerPosition(1, 0.80);
        splitPane.getItems().addAll(spLeft, spCenter, spRight);

        root.setPrefSize(100, 100);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double h = 98; // The height minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], h);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], h);

        assertEquals(28, p0, 1e-100);
        assertEquals(63, p1, 1e-100);
        assertEquals(28, spLeft.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(29, spCenter.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(29, spRight.getLayoutBounds().getHeight(), 1e-100);

        root.applyCss();
        root.resize(400, 405);
        root.layout();

        h = 403;
        pos = splitPane.getDividerPositions();
        p0 = convertDividerPostionToAbsolutePostion(pos[0], h);
        p1 = convertDividerPostionToAbsolutePostion(pos[1], h);

        assertEquals(28, p0, 1e-100);
        assertEquals(63, p1, 1e-100);
        assertEquals(28, spLeft.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(29, spCenter.getLayoutBounds().getHeight(), 1e-100);
        assertEquals(29, spRight.getLayoutBounds().getHeight(), 1e-100);
    }

    @Test public void positionDividersWithANonResizablePanel_RT22929() {
        StackPane spLeft = new StackPane();
        StackPane spCenter = new StackPane();
        StackPane spRight = new StackPane();

        spRight.setMinWidth(20);
        spRight.setPrefWidth(20);
        spRight.setMaxWidth(30);

        splitPane.setDividerPosition(0, 0.50);
        splitPane.setDividerPosition(1, 0.50);
        splitPane.getItems().addAll(spLeft, spCenter, spRight);

        root.setPrefSize(100, 100);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double w = 98; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], w);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], w);

        assertEquals(46, p0, 1e-100);
        assertEquals(62, p1, 1e-100);
        assertEquals(46, spLeft.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(10, spCenter.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(30, spRight.getLayoutBounds().getWidth(), 1e-100);

        splitPane.setDividerPosition(0, 0.20);
        root.layout();

        pos = splitPane.getDividerPositions();
        p0 = convertDividerPostionToAbsolutePostion(pos[0], w);
        p1 = convertDividerPostionToAbsolutePostion(pos[1], w);
        assertEquals(17, p0, 1e-100);
        assertEquals(62, p1, 1e-100);

        splitPane.setDividerPosition(1, 0.25);
        root.layout();

        pos = splitPane.getDividerPositions();
        p0 = convertDividerPostionToAbsolutePostion(pos[0], w);
        p1 = convertDividerPostionToAbsolutePostion(pos[1], w);
        assertEquals(17, p0, 1e-100);
        assertEquals(62, p1, 1e-100);
    }

    @Test public void threeDividersHaveTheSamePosition() {
        StackPane sp1 = new StackPane();
        StackPane sp2 = new StackPane();
        StackPane sp3 = new StackPane();
        StackPane sp4 = new StackPane();

        splitPane.getItems().addAll(sp1, sp2, sp3, sp4);

        root.setPrefSize(400, 400);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();
        root.layout();

        double w = 398; // The width minus the insets.
        double pos[] = splitPane.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], w);
        double p1 = convertDividerPostionToAbsolutePostion(pos[1], w);
        double p2 = convertDividerPostionToAbsolutePostion(pos[2], w);

        assertEquals(190, p0, 1e-100);
        assertEquals(196, p1, 1e-100);
        assertEquals(202, p2, 1e-100);
        assertEquals(190, sp1.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(0, sp2.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(0, sp3.getLayoutBounds().getWidth(), 1e-100);
        assertEquals(190, sp4.getLayoutBounds().getWidth(), 1e-100);
    }

    @Test public void addItemsInRunLater_RT23063() {
        final SplitPane sp = new SplitPane();
        Stage st = new Stage();
        st.setScene(new Scene(sp, 2000, 2000));
        st.show();

        Runnable runnable = () -> {
            StackPane rightsp = new StackPane();
            Label right = new Label("right");
            rightsp.getChildren().add(right);

            StackPane leftsp = new StackPane();
            Label left = new Label("left");
            leftsp.getChildren().add(left);

            sp.getItems().addAll(rightsp, leftsp);
        };
        Platform.runLater(runnable);

        sp.applyCss();
        sp.resize(400, 400);
        sp.layout();

        assertEquals(1, sp.getDividerPositions().length);

        double pos[] = sp.getDividerPositions();
        double p0 = convertDividerPostionToAbsolutePostion(pos[0], 398);
        assertEquals(196, p0, 1e-100);
    }

    @Test public void test_rt_36392() {
        AnchorPane item0 = new AnchorPane();
        item0.setId("xxx");

        VBox item1 = new VBox();
        item1.setId("myvbox");

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(item0, item1);

        AnchorPane page = new AnchorPane();
        page.setId("AnchorPane");
        page.getChildren().add(splitPane);

        StageLoader sl = new StageLoader(page);

        VBox myvbox = (VBox) page.lookup("#myvbox");
        myvbox.getChildren().add(new Button("Hello world !!!"));

        sl.dispose();
    }
}
