/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.control.Separator;
import javafx.scene.control.SkinBaseAccessor;
import javafx.scene.layout.Region;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 */
public class SeparatorSkinTest {
    private Separator separator;
    private SeparatorSkinMock skin;
    private Region line;

    @Before public void setup() {
        separator = new Separator();
        skin = new SeparatorSkinMock(separator);
        // Set some padding so that any places where padding was being
        // computed but wasn't expected will be caught.
        separator.setPadding(new Insets(10, 8, 6, 4));
        separator.setSkin(skin);
        // It so happens that SeparatorSkin has exactly one child: line
        line = (Region) SkinBaseAccessor.getChildren(skin).get(0);
        line.setPadding((new Insets(4, 3, 2, 1)));
        separator.layout();
    }

    /****************************************************************************
     *                                                                          *
     * Tests for change notification                                            *
     *                                                                          *
     ***************************************************************************/

    @Test public void orientationChangesOnSeparatorShouldInvoke_handleControlPropertyChanged() {
        assertFalse(skin.propertyChanged); // sanity check
        separator.setOrientation(Orientation.VERTICAL);
        assertTrue(skin.propertyChanged);
        assertEquals(1, skin.propertyChangeCount); // sanity check
    }

    @Test public void halignmentChangesOnSeparatorShouldInvoke_handleControlPropertyChanged() {
        assertFalse(skin.propertyChanged); // sanity check
        separator.setHalignment(HPos.RIGHT);
        assertTrue(skin.propertyChanged);
        assertEquals(1, skin.propertyChangeCount); // sanity check
    }

    @Test public void valignmentChangesOnSeparatorShouldInvoke_handleControlPropertyChanged() {
        assertFalse(skin.propertyChanged); // sanity check
        separator.setValignment(VPos.BASELINE);
        assertTrue(skin.propertyChanged);
        assertEquals(1, skin.propertyChangeCount); // sanity check
    }

    /****************************************************************************
     *                                                                          *
     * Tests for invalidation. When each of various properties change, we need  *
     * to invalidate some state, such as via requestLayout().                   *
     *                                                                          *
     ***************************************************************************/

    @Test public void orientationChangeShouldInvalidateLayout() {
        assertFalse(separator.isNeedsLayout());
        separator.setOrientation(Orientation.VERTICAL);
        assertTrue(separator.isNeedsLayout());
    }

    @Test public void halignmentChangeShouldInvalidateLayout() {
        assertFalse(separator.isNeedsLayout());
        separator.setHalignment(HPos.RIGHT);
        assertTrue(separator.isNeedsLayout());
    }

    @Test public void valignmentChangeShouldInvalidateLayout() {
        assertFalse(separator.isNeedsLayout());
        separator.setValignment(VPos.BASELINE);
        assertTrue(separator.isNeedsLayout());
    }

    /****************************************************************************
     *                                                                          *
     * Tests for minWidth                                                       *
     *                                                                          *
     ***************************************************************************/

    @Test public void minWidthWhenVerticalShouldBePaddingOfLinePlusPaddingOfSeparator() {
        separator.setOrientation(Orientation.VERTICAL);
        final Insets linePadding = line.getInsets();
        final Insets skinPadding = separator.getInsets();
        assertEquals(linePadding.getLeft() + linePadding.getRight() +
                skinPadding.getLeft() + skinPadding.getRight(),
                separator.minWidth(-1), 0);
    }

    @Ignore
    @Test public void minWidthWhenHorizontalShouldBePositiveNonZeroPlusPadding() {
        separator.setOrientation(Orientation.HORIZONTAL);
        final Insets linePadding = line.getInsets();
        final Insets skinPadding = separator.getInsets();
        assertTrue(separator.minWidth(-1) > 0);
        assertEquals(linePadding.getLeft() + linePadding.getRight() +
                skinPadding.getLeft() + skinPadding.getRight(),
                separator.minWidth(-1), 0);
    }

    /****************************************************************************
     *                                                                          *
     * Tests for minHeight                                                       *
     *                                                                          *
     ***************************************************************************/

    @Ignore
    @Test public void minHeightWhenVerticalShouldBePositiveNonZeroPlusPadding() {
        separator.setOrientation(Orientation.VERTICAL);
        final Insets linePadding = line.getInsets();
        final Insets skinPadding = separator.getInsets();
        assertTrue(separator.minHeight(-1) > 0);
        assertEquals(linePadding.getTop() + linePadding.getBottom() +
                skinPadding.getTop() + skinPadding.getBottom(),
                separator.minHeight(-1), 0);
    }

    @Test public void minHeightWhenHorizontalShouldBePaddingOfLinePlusPaddingOfSeparator() {
        separator.setOrientation(Orientation.HORIZONTAL);
        final Insets linePadding = line.getInsets();
        final Insets skinPadding = separator.getInsets();
        assertEquals(linePadding.getTop() + linePadding.getBottom() +
                skinPadding.getTop() + skinPadding.getBottom(),
                separator.minHeight(-1), 0);
    }

    /****************************************************************************
     *                                                                          *
     * Tests for maxWidth                                                       *
     *                                                                          *
     ***************************************************************************/

    @Test public void maxWidthWhenVerticalShouldBePaddingOfLinePlusPaddingOfSeparator() {
        separator.setOrientation(Orientation.VERTICAL);
        final Insets linePadding = line.getInsets();
        final Insets skinPadding = separator.getInsets();
        assertEquals(linePadding.getLeft() + linePadding.getRight() +
                skinPadding.getLeft() + skinPadding.getRight(),
                separator.maxWidth(-1), 0);
    }

    @Test public void maxWidthWhenHorizontalShouldBeMAX_VALUE() {
        separator.setOrientation(Orientation.HORIZONTAL);
        assertEquals(Double.MAX_VALUE, separator.maxWidth(-1), 0);
    }

    /****************************************************************************
     *                                                                          *
     * Tests for maxHeight                                                       *
     *                                                                          *
     ***************************************************************************/

    @Test public void maxHeightWhenVerticalShouldBeMAX_VALUE() {
        separator.setOrientation(Orientation.VERTICAL);
        assertEquals(Double.MAX_VALUE, separator.maxHeight(-1), 0);
    }

    @Test public void maxHeightWhenHorizontalShouldBePaddingOfLinePlusPaddingOfSeparator() {
        separator.setOrientation(Orientation.HORIZONTAL);
        final Insets linePadding = line.getInsets();
        final Insets skinPadding = separator.getInsets();
        assertEquals(linePadding.getTop() + linePadding.getBottom() +
                skinPadding.getTop() + skinPadding.getBottom(),
                separator.maxHeight(-1), 0);
    }

    @Test public void onVerticalMaxWidthTracksPreferred() {
        separator.setOrientation(Orientation.VERTICAL);
        separator.setPrefWidth(100);
        assertEquals(100, separator.maxWidth(-1), 0);
    }

    @Test public void onHorizontalMaxHeightTracksPreferred() {
        separator.setOrientation(Orientation.HORIZONTAL);
        separator.setPrefHeight(100);
        assertEquals(100, separator.maxHeight(-1), 0);
    }

    public static final class SeparatorSkinMock extends SeparatorSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public SeparatorSkinMock(Separator sep) {
            super(sep);
        }

        @Override protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);
            propertyChanged = true;
            propertyChangeCount++;
        }
    }


}
