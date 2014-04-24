/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.scene.control.infrastructure.ControlTestUtils;
import com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
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
        root.applyCss();
        root.autosize();
        root.layout();
        
        final double expectedPrefWidth = PlatformImpl.isCaspian() ? 54 : 
                                         PlatformImpl.isModena()  ? 52 :
                                         0;
        
        assertEquals(expectedPrefWidth, accordion.prefWidth(-1), 1e-100);
        assertEquals(60, accordion.prefHeight(-1), 1e-100);

        accordion.setExpandedPane(b);
        root.applyCss();
        root.autosize();
        root.layout();

        assertEquals(expectedPrefWidth, accordion.prefWidth(-1), 1e-100);
        
        final double expectedPrefHeight = PlatformImpl.isCaspian() ? 170 : 
                                          PlatformImpl.isModena()  ? 161 :
                                          0;
        assertEquals(expectedPrefHeight, accordion.prefHeight(-1), 1e-100);
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
        
        root.applyCss();
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
