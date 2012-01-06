/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 */

package javafx.scene.control;

import com.sun.javafx.css.StyleableProperty;
import static javafx.scene.control.ControlTestUtils.*;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.scene.control.skin.SplitPaneSkin;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.layout.StackPane;
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

    @Before public void setup() {
        tk = (StubToolkit)Toolkit.getToolkit();//This step is not needed (Just to make sure StubToolkit is loaded into VM)
        splitPane = new SplitPane();
        splitPane.setSkin(new SplitPaneSkin(splitPane));
        divider1 = new SplitPane.Divider();
        divider2 = new SplitPane.Divider();
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
    @Test public void whenOrientationIsBound_impl_cssSettable_ReturnsFalse() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(splitPane.orientationProperty());
        assertTrue(styleable.isSettable(splitPane));
        ObjectProperty<Orientation> other = new SimpleObjectProperty<Orientation>(Orientation.VERTICAL);
        splitPane.orientationProperty().bind(other);
        assertFalse(styleable.isSettable(splitPane));
    }

    @Test public void whenOrientationIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsTrue() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(splitPane.orientationProperty());
        styleable.set(splitPane, Orientation.VERTICAL);
        assertTrue(styleable.isSettable(splitPane));
    }

    @Test public void canSpecifyOrientationViaCSS() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(splitPane.orientationProperty());
        styleable.set(splitPane, Orientation.VERTICAL);
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

    @Test public void checkDividerPositions_RT18805() {        
        final Button l = new Button("Left Button");
        final Button c = new Button("Center Button");
        final Button r = new Button("Right Button");

        StackPane spLeft = new StackPane();
        spLeft.getChildren().add(l);
        spLeft.setMinWidth(100);
        spLeft.setMaxWidth(150);

        StackPane spCenter = new StackPane();
        spCenter.getChildren().add(c);

        StackPane spRight = new StackPane();
        spRight.getChildren().add(r);
        spRight.setMinWidth(100);
        spRight.setMaxWidth(150);


        splitPane.getItems().addAll(spLeft, spCenter, spRight);
        StackPane sp = new StackPane();
        sp.setPrefSize(600, 400);
        sp.getChildren().add(splitPane);

        sp.autosize();
        sp.layout();
        double pos[] = splitPane.getDividerPositions();
        assertEquals(pos[0], 0.25, 0.0);
        assertEquals(pos[1], 0.75, 0.0);
    }
    
}
