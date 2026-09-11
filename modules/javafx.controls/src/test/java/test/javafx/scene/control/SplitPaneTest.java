/*
 * Copyright (c) 2010, 2026, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertPseudoClassDoesNotExist;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertPseudoClassExists;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.assertStyleClassContains;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.css.CssMetaData;
import javafx.css.StyleableProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.skin.SplitPaneSkin;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.javafx.binding.DoubleConstant;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.pgstub.StubToolkit;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;

/**
 *
 * @author srikalyc
 */
public class SplitPaneTest {

    // Render scales and scene-graph bounds are stored with float precision.
    private static final double EPSILON = 1e-4;

    private SplitPane splitPane;
    private SplitPane.Divider divider1;
    private SplitPane.Divider divider2;
    private Scene scene;
    private Stage stage;
    private StackPane root;
    private StageLoader stageLoader;

    @BeforeEach
    public void setup() {
        assertTrue(Toolkit.getToolkit() instanceof StubToolkit);  // Ensure StubToolkit is loaded

        splitPane = new SplitPane();
        splitPane.setSkin(new SplitPaneSkin(splitPane));
        divider1 = new SplitPane.Divider();
        divider2 = new SplitPane.Divider();

        root = new StackPane();
        scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);
    }

    @AfterEach
    public void cleanup() {
        if (stageLoader != null) stageLoader.dispose();
        if (stage.isShowing()) stage.hide();
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
        ObjectProperty objPr = new SimpleObjectProperty<>(Orientation.VERTICAL);
        splitPane.orientationProperty().bind(objPr);
        assertSame(splitPane.orientationProperty().getValue(), Orientation.VERTICAL, "orientationProperty cannot be bound");
        objPr.setValue(Orientation.HORIZONTAL);
        assertSame(splitPane.orientationProperty().getValue(), Orientation.HORIZONTAL, "orientationProperty cannot be bound");
    }

    @Test public void checkDividerPositionPropertyBind() {
        DoubleProperty objPr = new SimpleDoubleProperty(0.6);
        divider1.positionProperty().bind(objPr);
        assertEquals(divider1.positionProperty().getValue(), 0.6, 0.0, "positionProperty cannot be bound");
        objPr.setValue(0.9);
        assertEquals(divider1.positionProperty().getValue(), 0.9, 0.0, "positionProperty cannot be bound");
    }

    @Test public void checkOrientationPropertyBind() {
        ObjectProperty objPr = new SimpleObjectProperty<>(Orientation.HORIZONTAL);
        splitPane.orientationProperty().bind(objPr);
        assertSame(splitPane.orientationProperty().getValue(), Orientation.HORIZONTAL, "orientationProperty cannot be bound");
        objPr.setValue(Orientation.VERTICAL);
        assertSame(splitPane.orientationProperty().getValue(), Orientation.VERTICAL, "orientationProperty cannot be bound");
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
        ObjectProperty<Orientation> other = new SimpleObjectProperty<>(Orientation.VERTICAL);
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

    @ParameterizedTest
    @MethodSource("renderScalesAndOrientations")
    public void contentDividersAndClipsArePixelAligned(double scaleX, double scaleY, Orientation orientation) {
        setRenderScales(scaleX, scaleY);

        splitPane.setOrientation(orientation);
        splitPane.setDividerPositions(0.301, 0.704);
        splitPane.getItems().addAll(new StackPane(), new StackPane(), new StackPane());
        splitPane.setPadding(new Insets(0.35, 0.55, 0.45, 0.65));

        root.setPrefSize(211.3, 173.7);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        root.autosize();

        List<Region> dividers = getDividerRegions();
        assertEquals(2, dividers.size());

        for (int i = 0; i < dividers.size(); i++) {
            // Dividers can have unequal thicknesses and fractional sizes.
            double dividerPadding = i == 0 ? 0.7 : 1.3;
            dividers.get(i).setPadding(new Insets(0, dividerPadding, 0, dividerPadding));
        }

        root.layout();

        List<Region> content = getContentRegions();
        assertEquals(3, content.size());

        List<Region> arranged = Stream.concat(content.stream(), dividers.stream())
            .sorted(Comparator.comparingDouble(node -> getMainMin(node.getBoundsInParent(), orientation)))
            .toList();

        for (Region node : arranged) {
            assertBoundsPixelAligned(node.getBoundsInParent(), scaleX, scaleY);
        }

        for (int i = 1; i < arranged.size(); i++) {
            assertEquals(
                getMainMax(arranged.get(i - 1).getBoundsInParent(), orientation),
                getMainMin(arranged.get(i).getBoundsInParent(), orientation),
                EPSILON,
                "Adjacent content and divider bounds must share a pixel boundary");
        }

        double expectedStart = orientation == Orientation.HORIZONTAL
            ? splitPane.snappedLeftInset()
            : splitPane.snappedTopInset();

        double expectedEnd = orientation == Orientation.HORIZONTAL
            ? splitPane.snapPositionX(splitPane.snapSizeX(splitPane.getWidth()) - splitPane.snappedRightInset())
            : splitPane.snapPositionY(splitPane.snapSizeY(splitPane.getHeight()) - splitPane.snappedBottomInset());

        assertEquals(expectedStart, getMainMin(arranged.getFirst().getBoundsInParent(), orientation), EPSILON);
        assertEquals(expectedEnd, getMainMax(arranged.getLast().getBoundsInParent(), orientation), EPSILON);

        for (Region contentRegion : content) {
            Rectangle clip = (Rectangle) contentRegion.getClip();
            assertEquals(contentRegion.getLayoutBounds().getWidth(), clip.getWidth(), EPSILON);
            assertEquals(contentRegion.getLayoutBounds().getHeight(), clip.getHeight(), EPSILON);
            assertPixelAligned(contentRegion.getLayoutX() + clip.getWidth(), scaleX);
            assertPixelAligned(contentRegion.getLayoutY() + clip.getHeight(), scaleY);
        }

        double scale = orientation == Orientation.HORIZONTAL ? scaleX : scaleY;

        double minSize = orientation == Orientation.HORIZONTAL
            ? splitPane.minWidth(-1)
            : splitPane.minHeight(-1);

        double prefSize = orientation == Orientation.HORIZONTAL
            ? splitPane.prefWidth(-1)
            : splitPane.prefHeight(-1);

        assertPixelAligned(minSize, scale);
        assertPixelAligned(prefSize, scale);
    }

    @ParameterizedTest
    @EnumSource(Orientation.class)
    public void unsnappedPreservesFractionalLayoutDragAndResize(Orientation orientation) {
        setRenderScales(1.25, 1.5);

        splitPane.setOrientation(orientation);
        splitPane.setSnapToPixel(false);
        splitPane.setManaged(false);
        splitPane.setPadding(Insets.EMPTY);
        splitPane.setDividerPosition(0, 0.503);
        splitPane.getItems().addAll(new StackPane(), new StackPane());

        root.setPrefSize(250, 200);
        root.getChildren().add(splitPane);
        show();

        root.applyCss();
        Region divider = getDividerRegions().getFirst();
        divider.setPadding(new Insets(0, 0.7, 0, 0.7));

        double width = 101.25;
        double height = 83.75;
        splitPane.resize(width, height);
        splitPane.relocate(10, 10);
        splitPane.layout();

        double mainSize = orientation == Orientation.HORIZONTAL ? width : height;
        double dividerSize = getMainSize(divider.getBoundsInParent(), orientation);
        double expectedDividerPosition = mainSize * 0.503 - dividerSize / 2;
        assertEquals(expectedDividerPosition, getMainMin(divider.getBoundsInParent(), orientation), EPSILON);
        assertEquals(0.503, splitPane.getDividerPositions()[0], EPSILON);
        assertAdjacent(getContentRegions(), divider, orientation);

        double dragDelta = 0.37;
        double dividerPositionBeforeDrag = getMainMin(divider.getBoundsInParent(), orientation);
        MouseEventFirer firer = new MouseEventFirer(divider);
        firer.fireMousePressed();
        firer.fireMouseEvent(
            MouseEvent.MOUSE_DRAGGED,
            orientation == Orientation.HORIZONTAL ? dragDelta : 0,
            orientation == Orientation.VERTICAL ? dragDelta : 0);

        splitPane.layout();

        assertEquals(
            dividerPositionBeforeDrag + dragDelta,
            getMainMin(divider.getBoundsInParent(), orientation), EPSILON,
            "Dragging must not round when snapToPixel is false");

        List<Region> contentBeforeResize = getContentRegions();
        double firstSize = getMainSize(contentBeforeResize.get(0).getBoundsInParent(), orientation);
        double secondSize = getMainSize(contentBeforeResize.get(1).getBoundsInParent(), orientation);

        double resizeDelta = 0.75;
        if (orientation == Orientation.HORIZONTAL) {
            splitPane.resize(width + resizeDelta, height);
        } else {
            splitPane.resize(width, height + resizeDelta);
        }

        splitPane.layout();

        List<Region> contentAfterResize = getContentRegions();

        assertEquals(
            firstSize + resizeDelta / 2,
            getMainSize(contentAfterResize.get(0).getBoundsInParent(), orientation),
            EPSILON);

        assertEquals(
            secondSize + resizeDelta / 2,
            getMainSize(contentAfterResize.get(1).getBoundsInParent(), orientation),
            EPSILON);

        assertAdjacent(contentAfterResize, divider, orientation);
    }

    @ParameterizedTest
    @EnumSource(Orientation.class)
    public void layoutBelowMinimumSizeFitsSnappedContentArea(Orientation orientation) {
        setRenderScales(1.25, 1.5);

        splitPane.setOrientation(orientation);
        splitPane.setManaged(false);
        splitPane.setPadding(new Insets(0.35, 0.55, 0.45, 0.65));
        splitPane.setDividerPositions(0.3, 0.7);

        for (int i = 0; i < 3; i++) {
            Region content = new Region();
            content.setMinSize(70.2, 60.2);
            splitPane.getItems().add(content);
        }

        root.setPrefSize(250, 200);
        root.getChildren().add(splitPane);
        show();
        root.applyCss();

        List<Region> dividers = getDividerRegions();
        dividers.get(0).setPadding(new Insets(0, 0.7, 0, 0.7));
        dividers.get(1).setPadding(new Insets(0, 1.3, 0, 1.3));

        splitPane.resize(100.8, 80);
        splitPane.relocate(10, 10);
        splitPane.layout();

        List<Region> content = getContentRegions();
        List<Region> arranged = Stream.concat(content.stream(), dividers.stream())
            .sorted(Comparator.comparingDouble(node -> getMainMin(node.getBoundsInParent(), orientation)))
            .toList();

        for (Region node : arranged) {
            assertBoundsPixelAligned(node.getBoundsInParent(), 1.25, 1.5);
        }

        for (Region contentRegion : content) {
            assertTrue(getMainSize(contentRegion.getBoundsInParent(), orientation) > 0);
        }

        for (int i = 1; i < arranged.size(); i++) {
            assertEquals(
                getMainMax(arranged.get(i - 1).getBoundsInParent(), orientation),
                getMainMin(arranged.get(i).getBoundsInParent(), orientation),
                EPSILON,
                "Content and dividers must remain adjacent below the minimum size");
        }

        double expectedStart = orientation == Orientation.HORIZONTAL
            ? splitPane.snappedLeftInset()
            : splitPane.snappedTopInset();

        double expectedEnd = orientation == Orientation.HORIZONTAL
            ? splitPane.snapPositionX(splitPane.snapSizeX(splitPane.getWidth()) - splitPane.snappedRightInset())
            : splitPane.snapPositionY(splitPane.snapSizeY(splitPane.getHeight()) - splitPane.snappedBottomInset());

        assertEquals(expectedStart, getMainMin(arranged.getFirst().getBoundsInParent(), orientation), EPSILON);
        assertEquals(expectedEnd, getMainMax(arranged.getLast().getBoundsInParent(), orientation), EPSILON);
    }

    @ParameterizedTest
    @EnumSource(Orientation.class)
    public void measurementUsesSnappedDependentDimension(Orientation orientation) {
        setRenderScales(1.25, 1.5);

        Orientation bias = orientation == Orientation.HORIZONTAL ? Orientation.VERTICAL : Orientation.HORIZONTAL;
        BiasedRegion content = new BiasedRegion(bias);

        if (orientation == Orientation.HORIZONTAL) {
            content.setMaxHeight(20.25);
        } else {
            content.setMaxWidth(20.25);
        }

        splitPane.setOrientation(orientation);
        splitPane.getItems().add(content);
        splitPane.setPadding(Insets.EMPTY);

        root.setPrefSize(200, 160);
        root.getChildren().add(splitPane);
        show();
        root.applyCss();

        content.resetDependentDimension();
        double measuredSize;
        double dependentScale;

        if (orientation == Orientation.HORIZONTAL) {
            measuredSize = splitPane.minWidth(80.25);
            dependentScale = 1.5;
        } else {
            measuredSize = splitPane.minHeight(90.25);
            dependentScale = 1.25;
        }

        assertTrue(content.getDependentDimension() >= 0, "Skin must not measure content-biased children with -1");
        assertPixelAligned(content.getDependentDimension(), dependentScale);
        double expectedDependentDimension = Math.ceil(20.25 * dependentScale) / dependentScale;
        assertEquals(
            expectedDependentDimension, content.getDependentDimension(), EPSILON,
            "measurement must use the bounded cross-size that layout allocates");
        assertPixelAligned(measuredSize, orientation == Orientation.HORIZONTAL ? 1.25 : 1.5);
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

    /**
     * Verifies that a divider position change of the {@link SplitPane} does not hang the layout.
     * Previously, this may happen when the divider position changed to a large number (>1),
     * which can hang the layout as it resulted in multiple layout requests (through SplitPaneSkin.layoutChildren).
     * See also: JDK-8277122
     */
    @Test
    public void testDividerOverOneDoesNotHangLayout() {
        testSetDividerPositionDoesNotHangLayout(10);
    }

    /**
     * Verifies that a divider position change of the {@link SplitPane} does not hang the layout.
     * Previously, this may happen when the divider position changed to a negative number (<1),
     * which can hang the layout as it resulted in multiple layout requests (through SplitPaneSkin.layoutChildren).
     * See also: JDK-8277122
     */
    @Test
    public void testDividerUnderZeroDoesNotHangLayout() {
        testSetDividerPositionDoesNotHangLayout(-1);
    }

    private static Stream<Arguments> renderScalesAndOrientations() {
        return Stream.of(
            Arguments.of(1.25, 1.5, Orientation.HORIZONTAL),
            Arguments.of(1.25, 1.5, Orientation.VERTICAL),
            Arguments.of(1.5, 1.25, Orientation.HORIZONTAL),
            Arguments.of(1.5, 1.25, Orientation.VERTICAL),
            Arguments.of(2.25, 2.75, Orientation.HORIZONTAL),
            Arguments.of(2.25, 2.75, Orientation.VERTICAL));
    }

    private void setRenderScales(double scaleX, double scaleY) {
        stage.renderScaleXProperty().bind(DoubleConstant.valueOf(scaleX));
        stage.renderScaleYProperty().bind(DoubleConstant.valueOf(scaleY));
    }

    private List<Region> getContentRegions() {
        return splitPane.getChildrenUnmodifiable().stream()
            .filter(node -> node instanceof Region && node.getClip() instanceof Rectangle)
            .map(node -> (Region) node)
            .toList();
    }

    private List<Region> getDividerRegions() {
        return splitPane.getChildrenUnmodifiable().stream()
            .filter(node -> node instanceof Region && node.getStyleClass().contains("split-pane-divider"))
            .map(node -> (Region) node)
            .toList();
    }

    private static void assertAdjacent(List<Region> content, Region divider, Orientation orientation) {
        List<Region> arranged = Stream.concat(content.stream(), Stream.of(divider))
            .sorted(Comparator.comparingDouble(node -> getMainMin(node.getBoundsInParent(), orientation)))
            .toList();

        for (int i = 1; i < arranged.size(); i++) {
            assertEquals(
                getMainMax(arranged.get(i - 1).getBoundsInParent(), orientation),
                getMainMin(arranged.get(i).getBoundsInParent(), orientation),
                EPSILON);
        }
    }

    private static void assertBoundsPixelAligned(Bounds bounds, double scaleX, double scaleY) {
        assertPixelAligned(bounds.getMinX(), scaleX);
        assertPixelAligned(bounds.getMaxX(), scaleX);
        assertPixelAligned(bounds.getMinY(), scaleY);
        assertPixelAligned(bounds.getMaxY(), scaleY);
    }

    private static void assertPixelAligned(double value, double scale) {
        double pixels = value * scale;
        assertEquals(Math.rint(pixels), pixels, EPSILON,
            () -> value + " logical units is not aligned at render scale " + scale);
    }

    private static double getMainMin(Bounds bounds, Orientation orientation) {
        return orientation == Orientation.HORIZONTAL ? bounds.getMinX() : bounds.getMinY();
    }

    private static double getMainMax(Bounds bounds, Orientation orientation) {
        return orientation == Orientation.HORIZONTAL ? bounds.getMaxX() : bounds.getMaxY();
    }

    private static double getMainSize(Bounds bounds, Orientation orientation) {
        return orientation == Orientation.HORIZONTAL ? bounds.getWidth() : bounds.getHeight();
    }

    private static final class BiasedRegion extends Region {
        private final Orientation bias;
        private double dependentDimension = -1;

        private BiasedRegion(Orientation bias) {
            this.bias = bias;
        }

        @Override
        public Orientation getContentBias() {
            return bias;
        }

        @Override
        protected double computeMinWidth(double height) {
            if (bias == Orientation.VERTICAL) {
                dependentDimension = height;
            }
            return 10.25;
        }

        @Override
        protected double computeMinHeight(double width) {
            if (bias == Orientation.HORIZONTAL) {
                dependentDimension = width;
            }
            return 10.25;
        }

        private void resetDependentDimension() {
            dependentDimension = -1;
        }

        private double getDependentDimension() {
            return dependentDimension;
        }
    }

    private void testSetDividerPositionDoesNotHangLayout(double dividerPosition) {
        AtomicInteger layoutCounter = new AtomicInteger();
        ComboBox<String> cbx = new ComboBox<>(FXCollections.observableArrayList("1", "2", "3")) {
            @Override
            protected void layoutChildren() {
                layoutCounter.incrementAndGet();
                super.layoutChildren();
            }
        };
        SplitPane pane = new SplitPane(new Label("AAAAA"), new TabPane(new Tab("Test", cbx)));
        StackPane root = new StackPane(pane);

        stageLoader = new StageLoader(root);

        Toolkit.getToolkit().firePulse();

        pane.setDividerPosition(0, dividerPosition);

        Toolkit.getToolkit().firePulse();

        // Reset layout counter
        layoutCounter.set(0);

        cbx.getSelectionModel().select(0);
        Toolkit.getToolkit().firePulse();

        assertTrue(layoutCounter.get() > 0);
    }

}
