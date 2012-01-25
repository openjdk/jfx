/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.control;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 */
public class AccordionTest {

    private Accordion accordion;
    private Scene scene;
    private Stage stage;
    private StackPane root;    
    
    @Before public void setup() {
        accordion = new Accordion();
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
    }

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/

    @Test public void defaultConstructorShouldDefaultToStyleClass_accordion() {
        ControlTestUtils.assertStyleClassContains(accordion, "accordion");
    }

    /*********************************************************************
     * Tests for the properties                                          *
     ********************************************************************/

    @Test public void expandedShouldBeNullByDefaultWithNoPanes() {
        assertNull(accordion.getExpandedPane());
    }

    @Test public void expandedShouldBeNullByDefaultEvenWithPanes() {
        accordion.getPanes().add(new TitledPane());
        assertNull(accordion.getExpandedPane());
    }

    @Test public void settingTheExpandedPaneToAPaneInPanesShouldWork() {
        TitledPane a = new TitledPane();
        TitledPane b = new TitledPane();
        TitledPane c = new TitledPane();
        accordion.getPanes().addAll(a, b, c);
        accordion.setExpandedPane(b);
        assertSame(b, accordion.getExpandedPane());
        accordion.setExpandedPane(a);
        assertSame(a, accordion.getExpandedPane());
        accordion.setExpandedPane(c);
        assertSame(c, accordion.getExpandedPane());
    }

    @Test public void settingTheExpandedPaneToNullWhenItWasNotNullShouldWork() {
        TitledPane a = new TitledPane();
        TitledPane b = new TitledPane();
        TitledPane c = new TitledPane();
        accordion.getPanes().addAll(a, b, c);
        accordion.setExpandedPane(b);
        accordion.setExpandedPane(null);
        assertNull(accordion.getExpandedPane());
    }

    @Test public void settingTheExpandedPaneToAPaneNotInPanesShouldStillChange() {
        TitledPane a = new TitledPane();
        TitledPane b = new TitledPane();
        TitledPane c = new TitledPane();
        TitledPane d = new TitledPane();
        accordion.getPanes().addAll(a, b, c);
        accordion.setExpandedPane(b);
        assertSame(b, accordion.getExpandedPane());
        accordion.setExpandedPane(d);
        assertSame(d, accordion.getExpandedPane());
    }

    @Test public void removingAPaneThatWasExpandedPaneShouldResultInNull() {
        TitledPane a = new TitledPane();
        TitledPane b = new TitledPane();
        TitledPane c = new TitledPane();
        accordion.getPanes().addAll(a, b, c);
        accordion.setExpandedPane(b);
        accordion.getPanes().removeAll(b, c);
        assertNull(accordion.getExpandedPane());
    }

    @Test public void removingAPaneThatWasNotExpandedShouldHaveNoChangeTo_expandedPane() {
        TitledPane a = new TitledPane();
        TitledPane b = new TitledPane();
        TitledPane c = new TitledPane();
        accordion.getPanes().addAll(a, b, c);
        accordion.setExpandedPane(b);
        accordion.getPanes().removeAll(a, c);
        assertSame(b, accordion.getExpandedPane());
    }

    @Test public void removingAPaneThatWasExpandedPaneButIsBoundResultsInNoChange() {
        TitledPane a = new TitledPane();
        TitledPane b = new TitledPane();
        TitledPane c = new TitledPane();
        ObservableValue<TitledPane> value = new SimpleObjectProperty<TitledPane>(b);
        accordion.getPanes().addAll(a, b, c);
        accordion.expandedPaneProperty().bind(value);
        accordion.getPanes().removeAll(b, c);
        assertSame(b, accordion.getExpandedPane());
    }

    @Test public void checkComputedHeight_RT19025() {
        TitledPane a = new TitledPane("A", new javafx.scene.shape.Rectangle(50, 100));
        TitledPane b = new TitledPane("B", new javafx.scene.shape.Rectangle(50, 100));
        TitledPane c = new TitledPane("C", new javafx.scene.shape.Rectangle(50, 100));

        a.setAnimated(false);
        b.setAnimated(false);
        c.setAnimated(false);
        
        accordion.getPanes().addAll(a, b, c);
        root.setPrefSize(100, 300);
        root.getChildren().add(accordion);
        show();
                
        root.impl_reapplyCSS();
        root.autosize();
        root.layout();
        
        assertEquals(54, accordion.prefWidth(-1), 1e-100);
        assertEquals(66, accordion.prefHeight(-1), 1e-100);

        accordion.setExpandedPane(b);
        root.impl_reapplyCSS();
        root.autosize();
        root.layout();

        assertEquals(54, accordion.prefWidth(-1), 1e-100);
        assertEquals(170, accordion.prefHeight(-1), 1e-100);
    }
}
