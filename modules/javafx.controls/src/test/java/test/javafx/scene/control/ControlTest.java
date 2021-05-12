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

import javafx.css.CssMetaData;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import com.sun.javafx.scene.control.Logging;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.ControlShim;
import javafx.scene.control.Skin;
import javafx.scene.control.Tooltip;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;

import static org.junit.Assert.*;

/**
 * Things every Control needs to test:
 *  - Properties
 *     - Set the value, see that it changes
 *     - Set an illegal value, see that it is rejected
 *     - Bind to some other value, see that when the binding fires, the property is updated (vague)
 *  - CSS
 *     - Change state, watch the pseudo class state change
 *     - Default style-class has what you expect
 *     - (2nd order) CssMetaData_isSettable returns true for thing in their default state
 *     - (2nd order) CssMetaData_isSettable returns false for things manually specified
 *     - (3rd order) getCssMetaData includes all the public properties
 *  - Methods
 *     - For all methods, calling the method mutates the state appropriately
 */
public class ControlTest {
    private static final double MIN_WIDTH = 35;
    private static final double MIN_HEIGHT = 45;
    private static final double MAX_WIDTH = 2000;
    private static final double MAX_HEIGHT = 2011;
    private static final double PREF_WIDTH = 100;
    private static final double PREF_HEIGHT = 130;
    private static final double BASELINE_OFFSET = 10;

    private ControlStub c;
    private SkinStub<ControlStub> s;
    private ResizableRectangle skinNode;

    private Level originalLogLevel = null;

    @Before public void setUp() {
        c = new ControlStub();
        s = new SkinStub<ControlStub>(c);
        skinNode = new ResizableRectangle();
        skinNode.resize(20, 20);
        skinNode.minWidth = MIN_WIDTH;
        skinNode.minHeight = MIN_HEIGHT;
        skinNode.maxWidth = MAX_WIDTH;
        skinNode.maxHeight = MAX_HEIGHT;
        skinNode.prefWidth = PREF_WIDTH;
        skinNode.prefHeight = PREF_HEIGHT;
        skinNode.baselineOffset = BASELINE_OFFSET;
        s.setNode(skinNode);
    }

    private void disableLogging() {
        final PlatformLogger logger = Logging.getControlsLogger();
        logger.disableLogging();
    }

    private void enableLogging() {
        final PlatformLogger logger = Logging.getControlsLogger();
        logger.enableLogging();
    }

    @Test public void focusTraversableIsTrueByDefault() {
        assertTrue(c.isFocusTraversable());
    }

    @Test public void resizableIsTrueByDefault() {
        assertTrue(c.isResizable());
    }

    @Test public void modifyingTheControlWidthUpdatesTheLayoutBounds() {
        c.resize(173, c.getHeight());
        assertEquals(173, c.getLayoutBounds().getWidth(), 0);
    }

    @Test public void modifyingTheControlWidthLeadsToRequestLayout() {
        // Make sure the Control has it's needsLayout flag cleared
        c.layout();
        assertTrue(!c.isNeedsLayout());
        // Run the test
        c.resize(173, c.getHeight());
        assertTrue(c.isNeedsLayout());
    }

    @Test public void modifyingTheControlHeightUpdatesTheLayoutBounds() {
        c.resize(c.getWidth(), 173);
        assertEquals(173, c.getLayoutBounds().getHeight(), 0);
    }

    @Test public void modifyingTheControlHeightLeadsToRequestLayout() {
        // Make sure the Control has it's needsLayout flag cleared
        c.layout();
        assertTrue(!c.isNeedsLayout());
        // Run the test
        c.resize(c.getWidth(), 173);
        assertTrue(c.isNeedsLayout());
    }

    @Test public void multipleModificationsToWidthAndHeightAreReflectedInLayoutBounds() {
        c.resize(723, 234);
        c.resize(992, 238);
        assertEquals(992, c.getLayoutBounds().getWidth(), 0);
        assertEquals(238, c.getLayoutBounds().getHeight(), 0);
    }

    @Test public void containsDelegatesToTheSkinWhenSet() {
        c.setSkin(s);
        skinNode.resize(100, 100);
        assertTrue(c.getSkin().getNode() != null);
        assertTrue(c.contains(50, 50));
    }

    @Test public void intersectsReturnsTrueWhenThereIsNoSkin() {
        c.relocate(0, 0);
        c.resize(100, 100);
        assertTrue(c.intersects(50, 50, 100, 100));
    }

    @Test public void intersectsDelegatesToTheSkinWhenSet() {
        c.setSkin(s);
        skinNode.resize(100, 100);
        assertTrue(c.intersects(50, 50, 100, 100));
    }

    @Test public void skinIsResizedToExactSizeOfControlOnLayout() {
        c.setSkin(s);
        c.resize(67, 998);
        c.layout();
        assertEquals(0, s.getNode().getLayoutBounds().getMinX(), 0);
        assertEquals(0, s.getNode().getLayoutBounds().getMinY(), 0);
        assertEquals(67, s.getNode().getLayoutBounds().getWidth(), 0);
        assertEquals(998, s.getNode().getLayoutBounds().getHeight(), 0);
    }

    @Test public void skinWithNodeLargerThanControlDoesntAffectLayoutBounds() {
        s.setNode(new Rectangle(0, 0, 1000, 1001));
        c.setSkin(s);
        c.resize(50, 40);
        c.layout();
        // Sanity check: the layout bounds of the skin node itself is large
        assertEquals(1000, c.getSkin().getNode().getLayoutBounds().getWidth(), 0);
        assertEquals(1001, c.getSkin().getNode().getLayoutBounds().getHeight(), 0);
        // Test: the layout bounds of the control is still 50, 40
        assertEquals(50, c.getLayoutBounds().getWidth(), 0);
        assertEquals(40, c.getLayoutBounds().getHeight(), 0);
    }

    /****************************************************************************
     * minWidth Tests                                                           *
     ***************************************************************************/

    @Test public void callsToSetMinWidthResultInRequestLayoutBeingCalledOnParentNotOnControl() {
        // Setup and sanity check
        Group parent = new Group();
        parent.getChildren().add(c);
        parent.layout();
        assertTrue(!parent.isNeedsLayout());
        assertTrue(!c.isNeedsLayout());
        // Test
        c.setMinWidth(123.45);
        assertTrue(parent.isNeedsLayout());
        assertTrue(!c.isNeedsLayout());
    }

    @Test public void getMinWidthReturnsTheMinWidthOfTheSkinNode() {
        c.setSkin(s);
        assertEquals(MIN_WIDTH, c.minWidth(-1), 0.0);
    }

    @Test public void getMinWidthReturnsTheCustomSetMinWidthWhenSpecified() {
        c.setSkin(s);
        c.setMinWidth(123.45);
        assertEquals(123.45, c.minWidth(-1), 0.0);
    }

    @Test public void getMinWidthReturnsTheCustomSetMinWidthWhenSpecifiedEvenWhenThereIsNoSkin() {
        c.setMinWidth(123.45);
        assertEquals(123.45, c.minWidth(-1), 0.0);
    }

    @Test public void minWidthWhenNoSkinIsSetIsZeroByDefault() {
        assertEquals(0, c.minWidth(-1), 0.0);
    }

    @Test public void resettingMinWidthTo_USE_PREF_SIZE_ReturnsThePrefWidthOfTheSkinNodeWhenThereIsASkin() {
        c.setSkin(s);
        c.setMinWidth(123.45);
        c.setMinWidth(Control.USE_PREF_SIZE);
        assertEquals(PREF_WIDTH, c.minWidth(-1), 0.0);
    }

    @Test public void resettingMinWidthTo_USE_PREF_SIZE_ReturnsZeroWhenThereIsNoSkinAndPrefWidthIsNotSet() {
        c.setMinWidth(123.45);
        c.setMinWidth(Control.USE_PREF_SIZE);
        assertEquals(0, c.minWidth(-1), 0.0);
    }

    @Test public void resettingMinWidthTo_USE_PREF_SIZE_ReturnsPrefWidthWhenThereIsNoSkinAndPrefWidthIsSet() {
        c.setMinWidth(123.45);
        c.setPrefWidth(98.6);
        c.setMinWidth(Control.USE_PREF_SIZE);
        assertEquals(98.6, c.minWidth(-1), 0.0);
    }

    @Test public void resettingMinWidthTo_USE_COMPUTED_SIZE_ReturnsTheMinWidthOfTheSkinNodeWhenThereIsASkin() {
        c.setSkin(s);
        c.setMinWidth(123.45);
        c.setMinWidth(Control.USE_COMPUTED_SIZE);
        assertEquals(MIN_WIDTH, c.minWidth(-1), 0.0);
    }

    @Test public void resettingMinWidthTo_USE_COMPUTED_SIZE_ReturnsZeroWhenThereIsNoSkinAndPrefWidthIsNotSet() {
        c.setMinWidth(123.45);
        c.setMinWidth(Control.USE_COMPUTED_SIZE);
        assertEquals(0, c.minWidth(-1), 0.0);
    }

    @Test public void minWidthIs_USE_COMPUTED_SIZE_ByDefault() {
        assertEquals(Control.USE_COMPUTED_SIZE, c.getMinWidth(), 0);
        assertEquals(Control.USE_COMPUTED_SIZE, c.minWidthProperty().get(), 0);
    }

    @Test public void minWidthCanBeSet() {
        c.setMinWidth(234);
        assertEquals(234, c.getMinWidth(), 0);
        assertEquals(234, c.minWidthProperty().get(), 0);
    }

    @Test public void minWidthCanBeBound() {
        DoubleProperty other = new SimpleDoubleProperty(939);
        c.minWidthProperty().bind(other);
        assertEquals(939, c.getMinWidth(), 0);
        other.set(332);
        assertEquals(332, c.getMinWidth(), 0);
    }

    @Test public void minWidthPropertyHasBeanReference() {
        assertSame(c, c.minWidthProperty().getBean());
    }

    @Test public void minWidthPropertyHasName() {
        assertEquals("minWidth", c.minWidthProperty().getName());
    }

    /****************************************************************************
     * minHeight Tests                                                          *
     ***************************************************************************/

    @Test public void callsToSetMinHeightResultInRequestLayoutBeingCalledOnParentNotOnControl() {
        // Setup and sanity check
        Group parent = new Group();
        parent.getChildren().add(c);
        parent.layout();
        assertTrue(!parent.isNeedsLayout());
        assertTrue(!c.isNeedsLayout());
        // Test
        c.setMinHeight(98.76);
        assertTrue(parent.isNeedsLayout());
        assertTrue(!c.isNeedsLayout());
    }

    @Test public void getMinHeightReturnsTheMinHeightOfTheSkinNode() {
        c.setSkin(s);
        assertEquals(MIN_HEIGHT, c.minHeight(-1), 0.0);
    }

    @Test public void getMinHeightReturnsTheCustomSetMinHeightWhenSpecified() {
        c.setSkin(s);
        c.setMinHeight(98.76);
        assertEquals(98.76, c.minHeight(-1), 0.0);
    }

    @Test public void getMinHeightReturnsTheCustomSetMinHeightWhenSpecifiedEvenWhenThereIsNoSkin() {
        c.setMinHeight(98.76);
        assertEquals(98.76, c.minHeight(-1), 0.0);
    }

    @Test public void minHeightWhenNoSkinIsSetIsZeroByDefault() {
        assertEquals(0, c.minHeight(-1), 0.0);
    }

    @Test public void resettingMinHeightTo_USE_PREF_SIZE_ReturnsThePrefHeightOfTheSkinNodeWhenThereIsASkin() {
        c.setSkin(s);
        c.setMinHeight(98.76);
        c.setMinHeight(Control.USE_PREF_SIZE);
        assertEquals(PREF_HEIGHT, c.minHeight(-1), 0.0);
    }

    @Test public void resettingMinHeightTo_USE_PREF_SIZE_ReturnsZeroWhenThereIsNoSkinAndPrefHeightIsNotSet() {
        c.setMinHeight(98.76);
        c.setMinHeight(Control.USE_PREF_SIZE);
        assertEquals(0, c.minHeight(-1), 0.0);
    }

    @Test public void resettingMinHeightTo_USE_PREF_SIZE_ReturnsPrefHeightWhenThereIsNoSkinAndPrefHeightIsSet() {
        c.setMinHeight(98.76);
        c.setPrefHeight(105.2);
        c.setMinHeight(Control.USE_PREF_SIZE);
        assertEquals(105.2, c.minHeight(-1), 0.0);
    }

    @Test public void resettingMinHeightTo_USE_COMPUTED_SIZE_ReturnsTheMinHeightOfTheSkinNodeWhenThereIsASkin() {
        c.setSkin(s);
        c.setMinHeight(98.76);
        c.setMinHeight(Control.USE_COMPUTED_SIZE);
        assertEquals(MIN_HEIGHT, c.minHeight(-1), 0.0);
    }

    @Test public void resettingMinHeightTo_USE_COMPUTED_SIZE_ReturnsZeroWhenThereIsNoSkinAndPrefHeightIsNotSet() {
        c.setMinHeight(98.76);
        c.setMinHeight(Control.USE_COMPUTED_SIZE);
        assertEquals(0, c.minHeight(-1), 0.0);
    }

    @Test public void minHeightIs_USE_COMPUTED_SIZE_ByDefault() {
        assertEquals(Control.USE_COMPUTED_SIZE, c.getMinHeight(), 0);
        assertEquals(Control.USE_COMPUTED_SIZE, c.minHeightProperty().get(), 0);
    }

    @Test public void minHeightCanBeSet() {
        c.setMinHeight(98.76);
        assertEquals(98.76, c.getMinHeight(), 0);
        assertEquals(98.76, c.minHeightProperty().get(), 0);
    }

    @Test public void minHeightCanBeBound() {
        DoubleProperty other = new SimpleDoubleProperty(939);
        c.minHeightProperty().bind(other);
        assertEquals(939, c.getMinHeight(), 0);
        other.set(332);
        assertEquals(332, c.getMinHeight(), 0);
    }

    @Test public void minHeightPropertyHasBeanReference() {
        assertSame(c, c.minHeightProperty().getBean());
    }

    @Test public void minHeightPropertyHasName() {
        assertEquals("minHeight", c.minHeightProperty().getName());
    }

    /****************************************************************************
     * maxWidth Tests                                                           *
     ***************************************************************************/

    @Test public void callsToSetMaxWidthResultInRequestLayoutBeingCalledOnParentNotOnControl() {
        // Setup and sanity check
        Group parent = new Group();
        parent.getChildren().add(c);
        parent.layout();
        assertTrue(!parent.isNeedsLayout());
        assertTrue(!c.isNeedsLayout());
        // Test
        c.setMaxWidth(500);
        assertTrue(parent.isNeedsLayout());
        assertTrue(!c.isNeedsLayout());
    }

    @Test public void getMaxWidthReturnsTheMaxWidthOfTheSkinNode() {
        c.setSkin(s);
        assertEquals(MAX_WIDTH, c.maxWidth(-1), 0.0);
    }

    @Test public void getMaxWidthReturnsTheCustomSetMaxWidthWhenSpecified() {
        c.setSkin(s);
        c.setMaxWidth(500);
        assertEquals(500, c.maxWidth(-1), 0.0);
    }

    @Test public void getMaxWidthReturnsTheCustomSetMaxWidthWhenSpecifiedEvenWhenThereIsNoSkin() {
        c.setMaxWidth(500);
        assertEquals(500, c.maxWidth(-1), 0.0);
    }

    @Test public void maxWidthWhenNoSkinIsSetIsZeroByDefault() {
        assertEquals(0, c.maxWidth(-1), 0.0);
    }

    @Test public void resettingMaxWidthTo_USE_PREF_SIZE_ReturnsThePrefWidthOfTheSkinNodeWhenThereIsASkin() {
        c.setSkin(s);
        c.setMaxWidth(500);
        c.setMaxWidth(Control.USE_PREF_SIZE);
        assertEquals(PREF_WIDTH, c.maxWidth(-1), 0.0);
    }

    @Test public void resettingMaxWidthTo_USE_PREF_SIZE_ReturnsZeroWhenThereIsNoSkinAndPrefWidthIsNotSet() {
        c.setMaxWidth(500);
        c.setMaxWidth(Control.USE_PREF_SIZE);
        assertEquals(0, c.maxWidth(-1), 0.0);
    }

    @Test public void resettingMaxWidthTo_USE_PREF_SIZE_ReturnsPrefWidthWhenThereIsNoSkinAndPrefWidthIsSet() {
        c.setMaxWidth(500);
        c.setPrefWidth(98.6);
        c.setMaxWidth(Control.USE_PREF_SIZE);
        assertEquals(98.6, c.maxWidth(-1), 0.0);
    }

    @Test public void resettingMaxWidthTo_USE_COMPUTED_SIZE_ReturnsTheMaxWidthOfTheSkinNodeWhenThereIsASkin() {
        c.setSkin(s);
        c.setMaxWidth(500);
        c.setMaxWidth(Control.USE_COMPUTED_SIZE);
        assertEquals(MAX_WIDTH, c.maxWidth(-1), 0.0);
    }

    @Test public void resettingMaxWidthTo_USE_COMPUTED_SIZE_ReturnsZeroWhenThereIsNoSkinAndPrefWidthIsNotSet() {
        c.setMaxWidth(500);
        c.setMaxWidth(Control.USE_COMPUTED_SIZE);
        assertEquals(0, c.maxWidth(-1), 0.0);
    }

    @Test public void maxWidthIs_USE_COMPUTED_SIZE_ByDefault() {
        assertEquals(Control.USE_COMPUTED_SIZE, c.getMaxWidth(), 0);
        assertEquals(Control.USE_COMPUTED_SIZE, c.maxWidthProperty().get(), 0);
    }

    @Test public void maxWidthCanBeSet() {
        c.setMaxWidth(500);
        assertEquals(500, c.getMaxWidth(), 0);
        assertEquals(500, c.maxWidthProperty().get(), 0);
    }

    @Test public void maxWidthCanBeBound() {
        DoubleProperty other = new SimpleDoubleProperty(939);
        c.maxWidthProperty().bind(other);
        assertEquals(939, c.getMaxWidth(), 0);
        other.set(332);
        assertEquals(332, c.getMaxWidth(), 0);
    }

    @Test public void maxWidthPropertyHasBeanReference() {
        assertSame(c, c.maxWidthProperty().getBean());
    }

    @Test public void maxWidthPropertyHasName() {
        assertEquals("maxWidth", c.maxWidthProperty().getName());
    }

    /****************************************************************************
     * maxHeight Tests                                                          *
     ***************************************************************************/

    @Test public void callsToSetMaxHeightResultInRequestLayoutBeingCalledOnParentNotOnControl() {
        // Setup and sanity check
        Group parent = new Group();
        parent.getChildren().add(c);
        parent.layout();
        assertTrue(!parent.isNeedsLayout());
        assertTrue(!c.isNeedsLayout());
        // Test
        c.setMaxHeight(450);
        assertTrue(parent.isNeedsLayout());
        assertTrue(!c.isNeedsLayout());
    }

    @Test public void getMaxHeightReturnsTheMaxHeightOfTheSkinNode() {
        c.setSkin(s);
        assertEquals(MAX_HEIGHT, c.maxHeight(-1), 0.0);
    }

    @Test public void getMaxHeightReturnsTheCustomSetMaxHeightWhenSpecified() {
        c.setSkin(s);
        c.setMaxHeight(450);
        assertEquals(450, c.maxHeight(-1), 0.0);
    }

    @Test public void getMaxHeightReturnsTheCustomSetMaxHeightWhenSpecifiedEvenWhenThereIsNoSkin() {
        c.setMaxHeight(500);
        assertEquals(500, c.maxHeight(-1), 0.0);
    }

    @Test public void maxHeightWhenNoSkinIsSetIsZeroByDefault() {
        assertEquals(0, c.maxHeight(-1), 0.0);
    }

    @Test public void resettingMaxHeightTo_USE_PREF_SIZE_ReturnsThePrefHeightOfTheSkinNodeWhenThereIsASkin() {
        c.setSkin(s);
        c.setMaxHeight(500);
        c.setMaxHeight(Control.USE_PREF_SIZE);
        assertEquals(PREF_HEIGHT, c.maxHeight(-1), 0.0);
    }

    @Test public void resettingMaxHeightTo_USE_PREF_SIZE_ReturnsZeroWhenThereIsNoSkinAndPrefHeightIsNotSet() {
        c.setMaxHeight(500);
        c.setMaxHeight(Control.USE_PREF_SIZE);
        assertEquals(0, c.maxHeight(-1), 0.0);
    }

    @Test public void resettingMaxHeightTo_USE_PREF_SIZE_ReturnsPrefHeightWhenThereIsNoSkinAndPrefHeightIsSet() {
        c.setMaxHeight(500);
        c.setPrefHeight(105.2);
        c.setMaxHeight(Control.USE_PREF_SIZE);
        assertEquals(105.2, c.maxHeight(-1), 0.0);
    }

    @Test public void resettingMaxHeightTo_USE_COMPUTED_SIZE_ReturnsTheMaxHeightOfTheSkinNodeWhenThereIsASkin() {
        c.setSkin(s);
        c.setMaxHeight(500);
        c.setMaxHeight(Control.USE_COMPUTED_SIZE);
        assertEquals(MAX_HEIGHT, c.maxHeight(-1), 0.0);
    }

    @Test public void resettingMaxHeightTo_USE_COMPUTED_SIZE_ReturnsZeroWhenThereIsNoSkinAndPrefHeightIsNotSet() {
        c.setMaxHeight(500);
        c.setMaxHeight(Control.USE_COMPUTED_SIZE);
        assertEquals(0, c.maxHeight(-1), 0.0);
    }

    @Test public void maxHeightIs_USE_COMPUTED_SIZE_ByDefault() {
        assertEquals(Control.USE_COMPUTED_SIZE, c.getMaxHeight(), 0);
        assertEquals(Control.USE_COMPUTED_SIZE, c.maxHeightProperty().get(), 0);
    }

    @Test public void maxHeightCanBeSet() {
        c.setMaxHeight(500);
        assertEquals(500, c.getMaxHeight(), 0);
        assertEquals(500, c.maxHeightProperty().get(), 0);
    }

    @Test public void maxHeightCanBeBound() {
        DoubleProperty other = new SimpleDoubleProperty(939);
        c.maxHeightProperty().bind(other);
        assertEquals(939, c.getMaxHeight(), 0);
        other.set(332);
        assertEquals(332, c.getMaxHeight(), 0);
    }

    @Test public void maxHeightPropertyHasBeanReference() {
        assertSame(c, c.maxHeightProperty().getBean());
    }

    @Test public void maxHeightPropertyHasName() {
        assertEquals("maxHeight", c.maxHeightProperty().getName());
    }

    /****************************************************************************
     * prefWidth Tests                                                          *
     ***************************************************************************/

    @Test public void callsToSetPrefWidthResultInRequestLayoutBeingCalledOnParentNotOnControl() {
        // Setup and sanity check
        Group parent = new Group();
        parent.getChildren().add(c);
        parent.layout();
        assertTrue(!parent.isNeedsLayout());
        assertTrue(!c.isNeedsLayout());
        // Test
        c.setPrefWidth(80);
        assertTrue(parent.isNeedsLayout());
        assertTrue(!c.isNeedsLayout());
    }

    @Test public void getPrefWidthReturnsThePrefWidthOfTheSkinNode() {
        c.setSkin(s);
        assertEquals(PREF_WIDTH, c.prefWidth(-1), 0.0);
    }

    @Test public void getPrefWidthReturnsTheCustomSetPrefWidthWhenSpecified() {
        c.setSkin(s);
        c.setPrefWidth(80);
        assertEquals(80, c.prefWidth(-1), 0.0);
    }

    @Test public void getPrefWidthReturnsTheCustomSetPrefWidthWhenSpecifiedEvenWhenThereIsNoSkin() {
        c.setPrefWidth(80);
        assertEquals(80, c.prefWidth(-1), 0.0);
    }

    @Test public void prefWidthWhenNoSkinIsSetIsZeroByDefault() {
        assertEquals(0, c.prefWidth(-1), 0.0);
    }

    @Ignore ("What should happen when the pref width is set to USE_PREF_SIZE? Seems it should be an exception")
    @Test public void resettingPrefWidthTo_USE_PREF_SIZE_ThrowsExceptionWhenThereIsASkin() {
        c.setSkin(s);
        c.setPrefWidth(80);
        c.setPrefWidth(Control.USE_PREF_SIZE);
        assertEquals(PREF_WIDTH, c.prefWidth(-1), 0.0);
    }

    @Test public void resettingPrefWidthTo_USE_COMPUTED_SIZE_ReturnsThePrefWidthOfTheSkinNodeWhenThereIsASkin() {
        c.setSkin(s);
        c.setPrefWidth(80);
        c.setPrefWidth(Control.USE_COMPUTED_SIZE);
        assertEquals(PREF_WIDTH, c.prefWidth(-1), 0.0);
    }

    @Test public void resettingPrefWidthTo_USE_COMPUTED_SIZE_ReturnsZeroWhenThereIsNoSkinAndPrefWidthIsNotSet() {
        c.setPrefWidth(80);
        c.setPrefWidth(Control.USE_COMPUTED_SIZE);
        assertEquals(0, c.prefWidth(-1), 0.0);
    }

    @Test public void prefWidthIs_USE_COMPUTED_SIZE_ByDefault() {
        assertEquals(Control.USE_COMPUTED_SIZE, c.getPrefWidth(), 0);
        assertEquals(Control.USE_COMPUTED_SIZE, c.prefWidthProperty().get(), 0);
    }

    @Test public void prefWidthCanBeSet() {
        c.setPrefWidth(80);
        assertEquals(80, c.getPrefWidth(), 0);
        assertEquals(80, c.prefWidthProperty().get(), 0);
    }

    @Test public void prefWidthCanBeBound() {
        DoubleProperty other = new SimpleDoubleProperty(939);
        c.prefWidthProperty().bind(other);
        assertEquals(939, c.getPrefWidth(), 0);
        other.set(332);
        assertEquals(332, c.getPrefWidth(), 0);
    }

    @Test public void prefWidthPropertyHasBeanReference() {
        assertSame(c, c.prefWidthProperty().getBean());
    }

    @Test public void prefWidthPropertyHasName() {
        assertEquals("prefWidth", c.prefWidthProperty().getName());
    }

    /****************************************************************************
     * prefHeight Tests                                                         *
     ***************************************************************************/

    @Test public void callsToSetPrefHeightResultInRequestLayoutBeingCalledOnParentNotOnControl() {
        // Setup and sanity check
        Group parent = new Group();
        parent.getChildren().add(c);
        parent.layout();
        assertTrue(!parent.isNeedsLayout());
        assertTrue(!c.isNeedsLayout());
        // Test
        c.setPrefHeight(92);
        assertTrue(parent.isNeedsLayout());
        assertTrue(!c.isNeedsLayout());
    }

    @Test public void getPrefHeightReturnsThePrefHeightOfTheSkinNode() {
        c.setSkin(s);
        assertEquals(PREF_HEIGHT, c.prefHeight(-1), 0.0);
    }

    @Test public void getPrefHeightReturnsTheCustomSetPrefHeightWhenSpecified() {
        c.setSkin(s);
        c.setPrefHeight(92);
        assertEquals(92, c.prefHeight(-1), 0.0);
    }

    @Test public void getPrefHeightReturnsTheCustomSetPrefHeightWhenSpecifiedEvenWhenThereIsNoSkin() {
        c.setPrefHeight(92);
        assertEquals(92, c.prefHeight(-1), 0.0);
    }

    @Test public void prefHeightWhenNoSkinIsSetIsZeroByDefault() {
        assertEquals(0, c.prefHeight(-1), 0.0);
    }

    @Ignore ("What should happen when the pref width is set to USE_PREF_SIZE? Seems it should be an exception")
    @Test public void resettingPrefHeightTo_USE_PREF_SIZE_ThrowsExceptionWhenThereIsASkin() {
        c.setSkin(s);
        c.setPrefHeight(92);
        c.setPrefHeight(Control.USE_PREF_SIZE);
        assertEquals(PREF_HEIGHT, c.prefHeight(-1), 0.0);
    }

    @Test public void resettingPrefHeightTo_USE_COMPUTED_SIZE_ReturnsThePrefHeightOfTheSkinNodeWhenThereIsASkin() {
        c.setSkin(s);
        c.setPrefHeight(92);
        c.setPrefHeight(Control.USE_COMPUTED_SIZE);
        assertEquals(PREF_HEIGHT, c.prefHeight(-1), 0.0);
    }

    @Test public void resettingPrefHeightTo_USE_COMPUTED_SIZE_ReturnsZeroWhenThereIsNoSkinAndPrefHeightIsNotSet() {
        c.setPrefHeight(92);
        c.setPrefHeight(Control.USE_COMPUTED_SIZE);
        assertEquals(0, c.prefHeight(-1), 0.0);
    }

    @Test public void prefHeightIs_USE_COMPUTED_SIZE_ByDefault() {
        assertEquals(Control.USE_COMPUTED_SIZE, c.getPrefHeight(), 0);
        assertEquals(Control.USE_COMPUTED_SIZE, c.prefHeightProperty().get(), 0);
    }

    @Test public void prefHeightCanBeSet() {
        c.setPrefHeight(98.76);
        assertEquals(98.76, c.getPrefHeight(), 0);
        assertEquals(98.76, c.prefHeightProperty().get(), 0);
    }

    @Test public void prefHeightCanBeBound() {
        DoubleProperty other = new SimpleDoubleProperty(939);
        c.prefHeightProperty().bind(other);
        assertEquals(939, c.getPrefHeight(), 0);
        other.set(332);
        assertEquals(332, c.getPrefHeight(), 0);
    }

    @Test public void prefHeightPropertyHasBeanReference() {
        assertSame(c, c.prefHeightProperty().getBean());
    }

    @Test public void prefHeightPropertyHasName() {
        assertEquals("prefHeight", c.prefHeightProperty().getName());
    }

    /*********************************************************************
     * Tests for the skin property                                       *
     ********************************************************************/

    @Test public void skinIsNullByDefault() {
        assertNull(c.getSkin());
        assertNull(c.contextMenuProperty().get());
    }

    @Test public void skinCanBeSet() {
        c.setSkin(s);
        assertSame(s, c.getSkin());
        assertSame(s, c.skinProperty().get());
    }

    @Test public void skinCanBeCleared() {
        c.setSkin(s);
        c.setSkin(null);
        assertNull(c.getSkin());
        assertNull(c.skinProperty().get());
    }

    @Test public void skinCanBeBound() {
        ObjectProperty other = new SimpleObjectProperty(s);
        c.skinProperty().bind(other);
        assertSame(s, c.getSkin());
        other.set(null);
        assertNull(c.getSkin());
    }

    @Test public void skinPropertyHasBeanReference() {
        assertSame(c, c.skinProperty().getBean());
    }

    @Test public void skinPropertyHasName() {
        assertEquals("skin", c.skinProperty().getName());
    }

    @Test public void canSpecifySkinViaCSS() {
        disableLogging();
        try {
            ((StyleableProperty)ControlShim.skinClassNameProperty(c)).applyStyle(null, "test.javafx.scene.control.SkinStub");
            assertNotNull(c.getSkin());
            assertTrue(c.getSkin() instanceof SkinStub);
            assertSame(c, c.getSkin().getSkinnable());
        } finally {
            enableLogging();
        }
    }

    @Test public void specifyingSameSkinTwiceViaCSSDoesntSetTwice() {
        disableLogging();
        try {
            SkinChangeListener listener = new SkinChangeListener();
            c.skinProperty().addListener(listener);
            ((StyleableProperty)ControlShim.skinClassNameProperty(c)).applyStyle(null, "test.javafx.scene.control.SkinStub");
            assertTrue(listener.changed);
            listener.changed = false;
            ((StyleableProperty)ControlShim.skinClassNameProperty(c)).applyStyle(null, "test.javafx.scene.control.SkinStub");
            assertFalse(listener.changed);
        } finally {
            enableLogging();
        }
    }

    @Test public void specifyingNullSkinNameHasNoEffect() {
        disableLogging();
        try {
            SkinChangeListener listener = new SkinChangeListener();
            c.skinProperty().addListener(listener);
            ((StyleableProperty)ControlShim.skinClassNameProperty(c)).applyStyle(null, null);
            assertFalse(listener.changed);
            assertNull(c.getSkin());
        } finally {
            enableLogging();
        }
    }

    @Test public void specifyingEmptyStringSkinNameHasNoEffect() {
        disableLogging();
        try {
            SkinChangeListener listener = new SkinChangeListener();
            c.skinProperty().addListener(listener);
            ((StyleableProperty)ControlShim.skinClassNameProperty(c)).applyStyle(null, "");
            assertFalse(listener.changed);
            assertNull(c.getSkin());
        } finally {
            enableLogging();
        }
    }

    @Test public void loadingSkinWithNoAppropriateConstructorResultsInNoSkin() {
        disableLogging();
        try {
            SkinChangeListener listener = new SkinChangeListener();
            c.skinProperty().addListener(listener);
            ((StyleableProperty)ControlShim.skinClassNameProperty(c)).applyStyle(null, "test.javafx.scene.control.ControlTest$BadSkin");
            assertFalse(listener.changed);
            assertNull(c.getSkin());
        } finally {
            enableLogging();
        }
    }

    @Test public void exceptionThrownDuringSkinConstructionResultsInNoSkin() {
        disableLogging();
        try {
            SkinChangeListener listener = new SkinChangeListener();
            c.skinProperty().addListener(listener);
            ((StyleableProperty)ControlShim.skinClassNameProperty(c)).applyStyle(null, "test.javafx.scene.control.ControlTest$ExceptionalSkin");
            assertFalse(listener.changed);
            assertNull(c.getSkin());
        } finally {
            enableLogging();
        }
    }

    /*********************************************************************
     * Tests for the tooltip property                                    *
     ********************************************************************/

    @Test public void tooltipIsNullByDefault() {
        assertNull(c.getTooltip());
        assertNull(c.tooltipProperty().get());
    }

    @Test public void tooltipCanBeSet() {
        Tooltip tip = new Tooltip("Hello");
        c.setTooltip(tip);
        assertSame(tip, c.getTooltip());
        assertSame(tip, c.tooltipProperty().get());
    }

    @Test public void tooltipCanBeCleared() {
        Tooltip tip = new Tooltip("Hello");
        c.setTooltip(tip);
        c.setTooltip(null);
        assertNull(c.getTooltip());
        assertNull(c.tooltipProperty().get());
    }

    @Test public void tooltipCanBeBound() {
        Tooltip tip = new Tooltip("Hello");
        ObjectProperty<Tooltip> other = new SimpleObjectProperty<Tooltip>(tip);
        c.tooltipProperty().bind(other);
        assertSame(tip, c.getTooltip());
        assertSame(tip, c.tooltipProperty().get());
        other.set(null);
        assertNull(c.getTooltip());
        assertNull(c.tooltipProperty().get());
    }

    @Test public void tooltipPropertyHasBeanReference() {
        assertSame(c, c.tooltipProperty().getBean());
    }

    @Test public void tooltipPropertyHasName() {
        assertEquals("tooltip", c.tooltipProperty().getName());
    }

    /*********************************************************************
     * Tests for the contextMenu property                                    *
     ********************************************************************/

    @Test public void contextMenuIsNullByDefault() {
        assertNull(c.getContextMenu());
        assertNull(c.contextMenuProperty().get());
    }

    @Test public void contextMenuCanBeSet() {
        ContextMenu menu = new ContextMenu();
        c.setContextMenu(menu);
        assertSame(menu, c.getContextMenu());
        assertSame(menu, c.contextMenuProperty().get());
    }

    @Test public void contextMenuCanBeCleared() {
        ContextMenu menu = new ContextMenu();
        c.setContextMenu(menu);
        c.setContextMenu(null);
        assertNull(c.getContextMenu());
        assertNull(c.getContextMenu());
    }

    @Test public void contextMenuCanBeBound() {
        ContextMenu menu = new ContextMenu();
        ObjectProperty<ContextMenu> other = new SimpleObjectProperty<ContextMenu>(menu);
        c.contextMenuProperty().bind(other);
        assertSame(menu, c.getContextMenu());
        assertSame(menu, c.contextMenuProperty().get());
        other.set(null);
        assertNull(c.getContextMenu());
        assertNull(c.contextMenuProperty().get());
    }

    @Test public void contextMenuPropertyHasBeanReference() {
        assertSame(c, c.contextMenuProperty().getBean());
    }

    @Test public void contextMenuPropertyHasName() {
        assertEquals("contextMenu", c.contextMenuProperty().getName());
    }

    /*********************************************************************
     * Miscellaneous tests                                               *
     ********************************************************************/

    @Test public void setMinSizeUpdatesBothMinWidthAndMinHeight() {
        c.setMinSize(123.45, 98.6);
        assertEquals(123.45, c.getMinWidth(), 0);
        assertEquals(98.6, c.getMinHeight(), 0);
    }

    @Test public void setMaxSizeUpdatesBothMaxWidthAndMaxHeight() {
        c.setMaxSize(658.9, 373.4);
        assertEquals(658.9, c.getMaxWidth(), 0);
        assertEquals(373.4, c.getMaxHeight(), 0);
    }

    @Test public void setPrefSizeUpdatesBothPrefWidthAndPrefHeight() {
        c.setPrefSize(720, 540);
        assertEquals(720, c.getPrefWidth(), 0);
        assertEquals(540, c.getPrefHeight(), 0);
    }

    @Test public void baselineOffsetIsZeroWhenThereIsNoSkin() {
        assertEquals(0, c.getBaselineOffset(), 0f);
    }

    @Test public void baselineOffsetUpdatedWhenTheSkinChanges() {
        c.setSkin(s);
        assertEquals(BASELINE_OFFSET, c.getBaselineOffset(), 0);
    }

    @Test
    public void testRT18097() {
        try {
            File f = System.getProperties().containsKey("CSS_META_DATA_TEST_DIR") ?
                    new File(System.getProperties().get("CSS_META_DATA_TEST_DIR").toString()) :
                    null;
            if (f == null) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                URL base = cl.getResource("test/javafx/../javafx");
                f = new File(base.toURI());
            }
            //System.err.println(f.getPath());
            assertTrue("" + f.getCanonicalPath() + " is not a directory", f.isDirectory());
            recursiveCheck(f, f.getPath().length() - 7);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            fail(ex.getMessage());
        }
    }

    private static void checkClass(Class someClass) {

        // Ignore inner classes
        if (someClass.getEnclosingClass() != null) return;

        if (javafx.scene.control.Control.class.isAssignableFrom(someClass) &&
                Modifier.isAbstract(someClass.getModifiers()) == false &&
                Modifier.isPrivate(someClass.getModifiers()) == false) {
            String what = someClass.getName();
            try {
                // should get NoSuchMethodException if ctor is not public
                Method m = someClass.getMethod("getClassCssMetaData", (Class[]) null);
                Node node = (Node)someClass.getDeclaredConstructor().newInstance();
                for (CssMetaData styleable : (List<CssMetaData<? extends Styleable, ?>>) m.invoke(null)) {

                    what = someClass.getName() + " " + styleable.getProperty();
                    WritableValue writable = styleable.getStyleableProperty(node);
                    assertNotNull(what, writable);

                    Object defaultValue = writable.getValue();
                    Object initialValue = styleable.getInitialValue((Node) someClass.getDeclaredConstructor().newInstance());

                    if (defaultValue instanceof Number) {
                        // 5 and 5.0 are not the same according to equals,
                        // but they should be...
                        assert(initialValue instanceof Number);
                        double d1 = ((Number)defaultValue).doubleValue();
                        double d2 = ((Number)initialValue).doubleValue();
                        assertEquals(what, d1, d2, .001);

                    } else if (defaultValue != null && defaultValue.getClass().isArray()) {
                        assertTrue(what, Arrays.equals((Object[])defaultValue, (Object[])initialValue));
                    } else {
                        assertEquals(what, defaultValue, initialValue);
                    }

                }

            } catch (NoSuchMethodException ex) {
                fail("NoSuchMethodException: RT-18097 cannot be tested on " + what);
            } catch (IllegalAccessException ex) {
                System.err.println("IllegalAccessException:  RT-18097 cannot be tested on " + what);
            } catch (IllegalArgumentException ex) {
                fail("IllegalArgumentException:  RT-18097 cannot be tested on " + what);
            } catch (InvocationTargetException ex) {
                fail("InvocationTargetException:  RT-18097 cannot be tested on " + what);
            } catch (InstantiationException ex) {
                fail("InstantiationException:  RT-18097 cannot be tested on " + what);
            }
        }
    }

    private static void checkDirectory(File directory, final int pathLength) {
        if (directory.isDirectory()) {

            for (File file : directory.listFiles()) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    final String filePath = file.getPath();
                    final int len = file.getPath().length() - ".class".length();
                    final String clName =
                        file.getPath().substring(pathLength+1, len).replace(File.separatorChar,'.');
                    if (clName.startsWith("javafx.scene") == false) continue;
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
//            System.out.println(directory.getPath());
            checkDirectory(directory, pathLength);

            for (File subFile : directory.listFiles()) {
                recursiveCheck(subFile, pathLength);
            }
        }
    }


    // TODO need to test key event dispatch

//    // test that the Control.Behavior methods correctly fix the coordinate
//    // space values to match based on old node & new
//    function testControlBehaviorCorrectlyMapsEventCoordinates() {
//        var c = ControlStub { width: 20, height: 20, skin: SkinStub {} }
//        var n = Rectangle { width: 100, height: 100 }
//        var g:Group;
//        // setting up a fairly complicated scene to see if the coordinate
//        // values on the copied event are correct. This scene has two different
//        // groups, and each group has a translate transform applied.
//        // The node "n" where the event originated is in a different group
//        // from the "c" node.
//        //
//        // in scene coords, n is 100x100 at 40, 52
//        //                  c is 20x20 at 5, 30
//        // in local coords n is 100x100 at 0, 0
//        //                 c is 20x20 at 0, 0
//        Scene {
//            content: g = Group {
//                // n is 100x100, child group is 40x10
//                translateX: 40
//                translateY: 52
//                content: [
//                    n,
//                    Group {
//                        // the content is 20x20. Scale makes it 40x10
//                        translateX: -35
//                        translateY: -22
//                        content: c // this is 20x20
//                    }
//                ]
//            }
//        }
//
//        // BEGIN SANITY CHECK
//        // start off with a couple sanity checks to make sure I have my math
//        // right such that if the real portion of this test fails, I know it
//        // is not due to a faulty test
//
//        // test local coords of n
//        assertEquals(0, n.boundsInLocal.minX);
//        assertEquals(0, n.boundsInLocal.minY);
//        assertEquals(100, n.boundsInLocal.width);
//        assertEquals(100, n.boundsInLocal.height);
//
//        // test local coords of c
//        assertEquals(0, c.boundsInLocal.minX);
//        assertEquals(0, c.boundsInLocal.minY);
//        assertEquals(20, c.boundsInLocal.width);
//        assertEquals(20, c.boundsInLocal.height);
//
//        // test scene coords of n
//        assertEquals(40, n.localToScene(n.boundsInLocal).minX);
//        assertEquals(52, n.localToScene(n.boundsInLocal).minY);
//        assertEquals(100, n.localToScene(n.boundsInLocal).width);
//        assertEquals(100, n.localToScene(n.boundsInLocal).height);
//
//        // test scene coords of c
//        assertEquals(5, c.localToScene(c.boundsInLocal).minX);
//        assertEquals(30, c.localToScene(c.boundsInLocal).minY);
//        assertEquals(20, c.localToScene(c.boundsInLocal).width);
//        assertEquals(20, c.localToScene(c.boundsInLocal).height);
//        // END SANITY CHECK
//
//        // create a simple mouse event on node. The x/y are 0,0, thus in the
//        // top left corner of the fill area of the rectangle.
//        c.onMousePressed = function(evt:MouseEvent):Void {
//            // this mouse event should have been translated from "g" space
//            // to "c" space. Relative to "g", "c" is 40x10 at -35, -22
//            //  so (0,0) in c space relative to n would be at 35, 22
//            assertEquals(35.0, evt.x);
//            assertEquals(22.0, evt.y);
//        }
//        // invoke the code path that leads to calling the previously defined
//        // onMousePressed function handler
//        mousePress(g, 0, 0);
//    }

    /**
     * Used for representing the "node" of the Skin, such that each of the fields
     * minWidth, minHeight, maxWidth, maxHeight, prefWidth, and prefHeight have
     * different values, and thus can more easily detect bugs if the various
     * test methods fail (if they all had the same value, it would potentially
     * mask bugs where maxWidth was called instead of minWidth, and so forth).
     */
    public class ResizableRectangle extends Rectangle {
        private double minWidth;
        private double maxWidth;
        private double minHeight;
        private double maxHeight;
        private double prefWidth;
        private double prefHeight;
        private double baselineOffset;

        @Override public boolean isResizable() { return true; }
        @Override public double minWidth(double h) { return minWidth; }
        @Override public double minHeight(double w) { return minHeight; }
        @Override public double prefWidth(double height) { return prefWidth; }
        @Override public double prefHeight(double width) { return prefHeight; }
        @Override public double maxWidth(double h) { return maxWidth; }
        @Override public double maxHeight(double w) { return maxHeight; }
        @Override public double getBaselineOffset() { return baselineOffset; }

        @Override public void resize(double width, double height) {
            setWidth(width);
            setHeight(height);
        }
    }

    /**
     * This is a Skin without an appropriate constructor, and will fail
     * to load!
     * @param <C>
     */
    public static final class BadSkin<C extends Control> extends SkinStub<C> {
        public BadSkin() {
            super(null);
        }
    }

    /**
     * This is a Skin without an appropriate constructor, and will fail
     * to load!
     * @param <C>
     */
    public static final class ExceptionalSkin<C extends Control> extends SkinStub<C> {
        public ExceptionalSkin(C control) {
            super(control);
            throw new NullPointerException("I am EXCEPTIONAL!");
        }
    }

    public class SkinChangeListener implements ChangeListener<Skin> {
        boolean changed = false;
        @Override public void changed(ObservableValue<? extends Skin> observable, Skin oldValue, Skin newValue) {
            changed = true;
        }
    }
}
