/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.control;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.KeyCode;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 */
public class AccordionTest {    
    private Accordion accordion;
    private Toolkit tk;
    private Scene scene;
    private Stage stage;
    private StackPane root;    
    
    @Before public void setup() {
        tk = (StubToolkit)Toolkit.getToolkit();//This step is not needed (Just to make sure StubToolkit is loaded into VM)
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
    
    @Test public void aiobeWhenFocusIsOnAControlInsideTheAccordion_RT22027() {
        Button b1 = new Button("A");
        Button b2 = new Button("B");
        
        TitledPane a = new TitledPane("A", b1);
        TitledPane b = new TitledPane("B", b2);
        
        accordion.getPanes().addAll(a, b);
        accordion.setExpandedPane(a);
        accordion.setLayoutX(200);
        
        root.setPrefSize(800, 800);
        root.getChildren().add(accordion);
        b1.requestFocus();
        show();
        
        root.impl_reapplyCSS();
        root.autosize();
        root.layout();
                
        KeyEventFirer keyboard = new KeyEventFirer(b1);                

        try {        
            keyboard.doKeyPress(KeyCode.HOME);
            tk.firePulse(); 
        } catch (Exception e) {
            fail();
        }
    }
}
