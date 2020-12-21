/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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

import javafx.css.CssMetaData;
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
import javafx.css.StyleableProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Hyperlink;
import javafx.scene.shape.Rectangle;

import org.junit.Before;
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
     *  - CssMetaData_isSettable returns false                                            *
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
        try {
            CssMetaData styleable = ((StyleableProperty)link.visitedProperty()).getCssMetaData();
            assertNull(styleable);
        } catch (ClassCastException ignored) {
            // pass!
        } catch (Exception e) {
            org.junit.Assert.fail(e.toString());
        }
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
