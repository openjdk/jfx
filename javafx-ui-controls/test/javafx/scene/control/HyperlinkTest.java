/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 */

package javafx.scene.control;

import com.sun.javafx.css.StyleableProperty;
import static javafx.scene.control.ControlTestUtils.assertPseudoClassDoesNotExist;
import static javafx.scene.control.ControlTestUtils.assertPseudoClassExists;
import static javafx.scene.control.ControlTestUtils.assertStyleClassContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.shape.Rectangle;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class HyperlinkTest {
    private Hyperlink link;
    
    @Before public void setup() {
        link = new Hyperlink();
    }
    
    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/
    
    @Test public void defaultConstructorShouldHaveNoGraphicAndEmptyString() {
        assertNull(link.getGraphic());
        assertEquals("", link.getText());
    }
    
    @Test public void oneArgConstructorShouldHaveNoGraphicAndSpecifiedString() {
        Hyperlink link2 = new Hyperlink(null);
        assertNull(link2.getGraphic());
        assertNull(link2.getText());
        
        link2 = new Hyperlink("");
        assertNull(link2.getGraphic());
        assertEquals("", link2.getText());
        
        link2 = new Hyperlink("Hello");
        assertNull(link2.getGraphic());
        assertEquals("Hello", link2.getText());
    }
    
    @Test public void twoArgConstructorShouldHaveSpecifiedGraphicAndSpecifiedString() {
        Hyperlink link2 = new Hyperlink(null, null);
        assertNull(link2.getGraphic());
        assertNull(link2.getText());

        Rectangle rect = new Rectangle();
        link2 = new Hyperlink("Hello", rect);
        assertSame(rect, link2.getGraphic());
        assertEquals("Hello", link2.getText());
    }

    @Test public void defaultConstructorShouldSetStyleClassTo_hyperlink() {
        assertStyleClassContains(link, "hyperlink");
    }
    
    @Test public void oneArgConstructorShouldSetStyleClassTo_hyperlink() {
        Hyperlink link2 = new Hyperlink(null);
        assertStyleClassContains(link2, "hyperlink");
    }
    
    @Test public void twoArgConstructorShouldSetStyleClassTo_hyperlink() {
        Hyperlink link2 = new Hyperlink(null, null);
        assertStyleClassContains(link2, "hyperlink");
    }
    
    @Test public void defaultConstructorShouldSetAlignmentToCENTER_LEFT() {
        assertEquals(Pos.CENTER_LEFT, link.getAlignment());
    }
    
    @Test public void oneArgConstructorShouldSetAlignmentToCENTER_LEFT() {
        Hyperlink link2 = new Hyperlink(null);
        assertEquals(Pos.CENTER_LEFT, link2.getAlignment());
    }
    
    @Test public void twoArgConstructorShouldSetAlignmentToCENTER_LEFT() {
        Hyperlink link2 = new Hyperlink(null, null);
        assertEquals(Pos.CENTER_LEFT, link2.getAlignment());
    }
    
    @Test public void defaultConstructorShouldSetCursorToHAND() {
        assertSame(Cursor.HAND, link.getCursor());
    }
    
    @Test public void oneArgConstructorShouldSetCursorToHAND() {
        Hyperlink link2 = new Hyperlink(null);
        assertSame(Cursor.HAND, link2.getCursor());
    }
    
    @Test public void twoArgConstructorShouldSetCursorToHAND() {
        Hyperlink link2 = new Hyperlink(null, null);
        assertSame(Cursor.HAND, link2.getCursor());
    }

    /********************************************************************************
     *                                                                              *
     *                         Tests for visited property                           *
     *                                                                              *
     *  - set is honored                                                            *
     *  - can be bound                                                              *
     *  - is false by default                                                       *
     *  - impl_cssSettable returns false                                            *
     *                                                                              *
     *******************************************************************************/

    @Test public void visitedDefaultValueIsFalse() {
        assertFalse(link.isVisited());
        assertFalse(link.visitedProperty().get());
    }
    
    @Test public void settingVisitedShouldWork() {
        link.setVisited(true);
        assertTrue(link.isVisited());
    }
    
    @Test public void settingVisitedAndThenCreatingAModelAndReadingTheValueStillWorks() {
        link.setVisited(true);
        assertTrue(link.visitedProperty().get());
    }
    
    @Test public void visitedCanBeBound() {
        BooleanProperty other = new SimpleBooleanProperty(true);
        link.visitedProperty().bind(other);
        assertTrue(link.isVisited());
    }
    
    @Test public void visitedPropertyIsNotStyleable() {
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(link.visitedProperty());
        assertNull(styleable);
    }
    
    @Ignore ("replaced by visitedPropertyIsNotStyleable")
    @Test public void whenVisitedIsBound_impl_cssSettable_ReturnsFalse() {
        // will return null!
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(link.visitedProperty());
        assertFalse(styleable.isSettable(link));
        BooleanProperty other = new SimpleBooleanProperty(true);
        link.visitedProperty().bind(other);
    }
    
    @Ignore ("replaced by visitedPropertyIsNotStyleable")
    @Test public void whenVisitedIsSpecifiedViaCSSAndIsNotBound_impl_cssSettable_ReturnsFalse() {
        // will return null!
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(link.visitedProperty());
        styleable.set(link,true);
        assertFalse(styleable.isSettable(link));
    }
    
    @Ignore ("replaced by visitedPropertyIsNotStyleable")
    @Test public void cannotSpecifyVisitedViaCSS() {
        // will return null!
        StyleableProperty styleable = StyleableProperty.getStyleableProperty(link.visitedProperty());
        styleable.set(link,true);
        assertFalse(link.isVisited());
    }
    
    @Test public void settingVisitedSetsPseudoClass() {
        link.setVisited(true);
        assertPseudoClassExists(link, "visited");
    }
    
    @Test public void clearingVisitedClearsPseudoClass() {
        link.setVisited(true);
        link.setVisited(false);
        assertPseudoClassDoesNotExist(link, "visted");
    }

    @Test public void visitedBeanIsCorrect() {
        assertSame(link, link.visitedProperty().getBean());
    }

    @Test public void visitedNameIsCorrect() {
        assertEquals("visited", link.visitedProperty().getName());
    }
    
    /********************************************************************************
     *                                                                              *
     *                         Tests for onAction property                          *
     *                                                                              *
     *******************************************************************************/

    @Test public void onActionIsNullByDefault() {
        assertNull(link.getOnAction());
        assertNull(link.onActionProperty().getValue());
    }
    
    @Test public void onActionCanBeSet() {
        final EventHandler<ActionEvent> handler = new EventHandlerStub();
        link.setOnAction(handler);
        assertEquals(handler, link.getOnAction());
    }
    
    @Test public void onActionSetToNonDefaultValueIsReflectedInModel() {
        final EventHandler<ActionEvent> handler = new EventHandlerStub();
        link.setOnAction(handler);
        assertEquals(handler, link.onActionProperty().getValue());
    }
    
    @Test public void onActionCanBeCleared() {
        final EventHandler<ActionEvent> handler = new EventHandlerStub();
        link.setOnAction(handler);
        link.setOnAction(null);
        assertNull(link.getOnAction());
    }
    
    @Test public void onActionCanBeBound() {
        final EventHandler<ActionEvent> handler = new EventHandlerStub();
        ObjectProperty<EventHandler<ActionEvent>> other = new SimpleObjectProperty<EventHandler<ActionEvent>>(handler);
        link.onActionProperty().bind(other);
        assertEquals(handler, link.getOnAction());
    }
    
    @Test public void onActionCalledWhenHyperlinkIsFired() {
        final EventHandlerStub handler = new EventHandlerStub();
        link.setOnAction(handler);
        link.fire();
        assertTrue(handler.called);
    }
    
    @Test public void onActionCalledWhenNullWhenHyperlinkIsFiredIsNoOp() {
        link.fire(); // should throw no exceptions, if it does, the test fails
    }
    
    public static final class EventHandlerStub implements EventHandler<ActionEvent> {
        boolean called = false;
        @Override public void handle(ActionEvent event) {
            called = true;
        }
    };
    
    /********************************************************************************
     *                                                                              *
     *                            Tests for fire method                             *
     *                                                                              *
     *******************************************************************************/

    @Test public void visitedIsSetTrueWhenFireIsCalledAndVisitedIsNotBound_wasFalse() {
        assertFalse(link.isVisited());
        link.fire();
        assertTrue(link.isVisited());
    }

    @Test public void visitedIsSetTrueWhenFireIsCalledAndVisitedIsNotBound_wasTrue() {
        link.setVisited(true);
        link.fire();
        assertTrue(link.isVisited());
    }

    @Test public void noExceptionWhenVisitedIsBoundsAndFireIsCalled() {
        BooleanProperty other = new SimpleBooleanProperty(true);
        link.visitedProperty().bind(other);
        link.fire();
        // If this executed, then the test passes (even though there are no assertions)
    }


//    @Test TODO belongs in skin test
//    public void changingVPosShouldCauseRequestLayoutToBeCalled() {
//        scene.getRoot().layout();
//        assertFalse(instance.isNeedsLayout());
//        instance.setVpos(VPos.TOP);
//        assertTrue(instance.isNeedsLayout());
//    }
}
