/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.layout;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HeaderBar;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import test.util.ReflectionUtils;

import static org.junit.jupiter.api.Assertions.*;

public class HeaderBarTest {

    HeaderBar headerBar;

    @BeforeEach
    void setup() {
        headerBar = new HeaderBar();
    }

    @Test
    void emptyHeaderBar() {
        assertNull(headerBar.getLeading());
        assertNull(headerBar.getCenter());
        assertNull(headerBar.getTrailing());
    }

    @ParameterizedTest
    @CsvSource({
        "TOP_LEFT, 10, 10, 100, 80",
        "TOP_CENTER, 10, 10, 100, 80",
        "TOP_RIGHT, 10, 10, 100, 80",
        "CENTER_LEFT, 10, 10, 100, 80",
        "CENTER, 10, 10, 100, 80",
        "CENTER_RIGHT, 10, 10, 100, 80",
        "BOTTOM_LEFT, 10, 10, 100, 80",
        "BOTTOM_CENTER, 10, 10, 100, 80",
        "BOTTOM_RIGHT, 10, 10, 100, 80"
    })
    void alignmentOfLeadingChildOnly_resizable(Pos pos, double x, double y, double width, double height) {
        var content = new MockResizable(100, 50);
        HeaderBar.setAlignment(content, pos);
        HeaderBar.setMargin(content, new Insets(10));
        headerBar.setLeading(content);
        headerBar.resize(1000, 100);
        headerBar.layout();

        assertBounds(x, y, width, height, content);
    }

    @ParameterizedTest
    @CsvSource({
        "TOP_LEFT, 10, 10, 100, 50",
        "TOP_CENTER, 10, 10, 100, 50",
        "TOP_RIGHT, 10, 10, 100, 50",
        "CENTER_LEFT, 10, 25, 100, 50",
        "CENTER, 10, 25, 100, 50",
        "CENTER_RIGHT, 10, 25, 100, 50",
        "BOTTOM_LEFT, 10, 40, 100, 50",
        "BOTTOM_CENTER, 10, 40, 100, 50",
        "BOTTOM_RIGHT, 10, 40, 100, 50"
    })
    void alignmentOfLeadingChildOnly_notResizable(Pos pos, double x, double y, double width, double height) {
        var content = new Rectangle(100, 50);
        HeaderBar.setAlignment(content, pos);
        HeaderBar.setMargin(content, new Insets(10));
        headerBar.setLeading(content);
        headerBar.resize(1000, 100);
        headerBar.layout();

        assertBounds(x, y, width, height, content);
    }

    @ParameterizedTest
    @CsvSource({
        "TOP_LEFT, 890, 10, 100, 80",
        "TOP_CENTER, 890, 10, 100, 80",
        "TOP_RIGHT, 890, 10, 100, 80",
        "CENTER_LEFT, 890, 10, 100, 80",
        "CENTER, 890, 10, 100, 80",
        "CENTER_RIGHT, 890, 10, 100, 80",
        "BOTTOM_LEFT, 890, 10, 100, 80",
        "BOTTOM_CENTER, 890, 10, 100, 80",
        "BOTTOM_RIGHT, 890, 10, 100, 80"
    })
    void alignmentOfTrailingChildOnly_resizable(Pos pos, double x, double y, double width, double height) {
        var content = new MockResizable(100, 50);
        HeaderBar.setAlignment(content, pos);
        HeaderBar.setMargin(content, new Insets(10));
        headerBar.setTrailing(content);
        headerBar.resize(1000, 100);
        headerBar.layout();

        assertBounds(x, y, width, height, content);
    }

    @ParameterizedTest
    @CsvSource({
        "TOP_LEFT, 890, 10, 100, 50",
        "TOP_CENTER, 890, 10, 100, 50",
        "TOP_RIGHT, 890, 10, 100, 50",
        "CENTER_LEFT, 890, 25, 100, 50",
        "CENTER, 890, 25, 100, 50",
        "CENTER_RIGHT, 890, 25, 100, 50",
        "BOTTOM_LEFT, 890, 40, 100, 50",
        "BOTTOM_CENTER, 890, 40, 100, 50",
        "BOTTOM_RIGHT, 890, 40, 100, 50"
    })
    void alignmentOfTrailingChildOnly_notResizable(Pos pos, double x, double y, double width, double height) {
        var content = new Rectangle(100, 50);
        HeaderBar.setAlignment(content, pos);
        HeaderBar.setMargin(content, new Insets(10));
        headerBar.setTrailing(content);
        headerBar.resize(1000, 100);
        headerBar.layout();

        assertBounds(x, y, width, height, content);
    }

    @ParameterizedTest
    @CsvSource({
        "TOP_LEFT, 10, 10, 100, 80",
        "TOP_CENTER, 450, 10, 100, 80",
        "TOP_RIGHT, 890, 10, 100, 80",
        "CENTER_LEFT, 10, 10, 100, 80",
        "CENTER, 450, 10, 100, 80",
        "CENTER_RIGHT, 890, 10, 100, 80",
        "BOTTOM_LEFT, 10, 10, 100, 80",
        "BOTTOM_CENTER, 450, 10, 100, 80",
        "BOTTOM_RIGHT, 890, 10, 100, 80"
    })
    void alignmentOfCenterChildOnly_resizable(Pos pos, double x, double y, double width, double height) {
        var content = new MockResizable(100, 50);
        HeaderBar.setAlignment(content, pos);
        HeaderBar.setMargin(content, new Insets(10));
        headerBar.setCenter(content);
        headerBar.resize(1000, 100);
        headerBar.layout();

        assertBounds(x, y, width, height, content);
    }

    @ParameterizedTest
    @CsvSource({
        "TOP_LEFT, 10, 10, 100, 50",
        "TOP_CENTER, 450, 10, 100, 50",
        "TOP_RIGHT, 890, 10, 100, 50",
        "CENTER_LEFT, 10, 25, 100, 50",
        "CENTER, 450, 25, 100, 50",
        "CENTER_RIGHT, 890, 25, 100, 50",
        "BOTTOM_LEFT, 10, 40, 100, 50",
        "BOTTOM_CENTER, 450, 40, 100, 50",
        "BOTTOM_RIGHT, 890, 40, 100, 50"
    })
    void alignmentOfCenterChildOnly_notResizable(Pos pos, double x, double y, double width, double height) {
        var content = new Rectangle(100, 50);
        HeaderBar.setAlignment(content, pos);
        HeaderBar.setMargin(content, new Insets(10));
        headerBar.setCenter(content);
        headerBar.resize(1000, 100);
        headerBar.layout();

        assertBounds(x, y, width, height, content);
    }

    @ParameterizedTest
    @CsvSource({
        "TOP_LEFT, 60, 10, 100, 80",
        "TOP_CENTER, 450, 10, 100, 80",
        "TOP_RIGHT, 740, 10, 100, 80",
        "CENTER_LEFT, 60, 10, 100, 80",
        "CENTER, 450, 10, 100, 80",
        "CENTER_RIGHT, 740, 10, 100, 80",
        "BOTTOM_LEFT, 60, 10, 100, 80",
        "BOTTOM_CENTER, 450, 10, 100, 80",
        "BOTTOM_RIGHT, 740, 10, 100, 80"
    })
    void alignmentOfCenterChild_resizable_withNonEmptyLeadingAndTrailingChild(
            Pos pos, double x, double y, double width, double height) {
        var leading = new MockResizable(50, 50);
        var center = new MockResizable(100, 50);
        var trailing = new MockResizable(150, 50);
        HeaderBar.setAlignment(center, pos);
        HeaderBar.setMargin(center, new Insets(10));
        headerBar.setLeading(leading);
        headerBar.setCenter(center);
        headerBar.setTrailing(trailing);
        headerBar.resize(1000, 100);
        headerBar.layout();

        assertBounds(x, y, width, height, center);
    }

    @ParameterizedTest
    @CsvSource({
        "TOP_LEFT, 60, 10, 100, 50",
        "TOP_CENTER, 450, 10, 100, 50",
        "TOP_RIGHT, 740, 10, 100, 50",
        "CENTER_LEFT, 60, 25, 100, 50",
        "CENTER, 450, 25, 100, 50",
        "CENTER_RIGHT, 740, 25, 100, 50",
        "BOTTOM_LEFT, 60, 40, 100, 50",
        "BOTTOM_CENTER, 450, 40, 100, 50",
        "BOTTOM_RIGHT, 740, 40, 100, 50"
    })
    void alignmentOfCenterChild_notResizable_withNonEmptyLeadingAndTrailingChild(
            Pos pos, double x, double y, double width, double height) {
        var leading = new Rectangle(50, 50);
        var center = new Rectangle(100, 50);
        var trailing = new Rectangle(150, 50);
        HeaderBar.setAlignment(center, pos);
        HeaderBar.setMargin(center, new Insets(10));
        headerBar.setLeading(leading);
        headerBar.setCenter(center);
        headerBar.setTrailing(trailing);
        headerBar.resize(1000, 100);
        headerBar.layout();

        assertBounds(x, y, width, height, center);
    }

    @ParameterizedTest
    @SuppressWarnings("unchecked")
    @CsvSource({
        "TOP_LEFT, 160, 10, 100, 80",
        "TOP_CENTER, 450, 10, 100, 80",
        "TOP_RIGHT, 740, 10, 100, 80",
        "CENTER_LEFT, 160, 10, 100, 80",
        "CENTER, 450, 10, 100, 80",
        "CENTER_RIGHT, 740, 10, 100, 80",
        "BOTTOM_LEFT, 160, 10, 100, 80",
        "BOTTOM_CENTER, 450, 10, 100, 80",
        "BOTTOM_RIGHT, 740, 10, 100, 80"
    })
    void alignmentOfCenterChild_withLeftSystemInset(Pos pos, double x, double y, double width, double height) {
        var leftSystemInset = (ObjectProperty<Dimension2D>)ReflectionUtils.getFieldValue(headerBar, "leftSystemInset");
        leftSystemInset.set(new Dimension2D(100, 100));
        var leading = new MockResizable(50, 50);
        var center = new MockResizable(100, 50);
        var trailing = new MockResizable(150, 50);
        HeaderBar.setAlignment(center, pos);
        HeaderBar.setMargin(center, new Insets(10));
        headerBar.setLeading(leading);
        headerBar.setCenter(center);
        headerBar.setTrailing(trailing);
        headerBar.resize(1000, 100);
        headerBar.layout();

        assertBounds(x, y, width, height, center);
    }

    @ParameterizedTest
    @SuppressWarnings("unchecked")
    @CsvSource({
        "TOP_LEFT, 60, 10, 100, 80",
        "TOP_CENTER, 450, 10, 100, 80",
        "TOP_RIGHT, 640, 10, 100, 80",
        "CENTER_LEFT, 60, 10, 100, 80",
        "CENTER, 450, 10, 100, 80",
        "CENTER_RIGHT, 640, 10, 100, 80",
        "BOTTOM_LEFT, 60, 10, 100, 80",
        "BOTTOM_CENTER, 450, 10, 100, 80",
        "BOTTOM_RIGHT, 640, 10, 100, 80"
    })
    void alignmentOfCenterChild_withRightSystemInset(Pos pos, double x, double y, double width, double height) {
        var rightSystemInset = (ObjectProperty<Dimension2D>)ReflectionUtils.getFieldValue(headerBar, "rightSystemInset");
        rightSystemInset.set(new Dimension2D(100, 100));
        var leading = new MockResizable(50, 50);
        var center = new MockResizable(100, 50);
        var trailing = new MockResizable(150, 50);
        HeaderBar.setAlignment(center, pos);
        HeaderBar.setMargin(center, new Insets(10));
        headerBar.setLeading(leading);
        headerBar.setCenter(center);
        headerBar.setTrailing(trailing);
        headerBar.resize(1000, 100);
        headerBar.layout();

        assertBounds(x, y, width, height, center);
    }

    @ParameterizedTest
    @SuppressWarnings("unchecked")
    @CsvSource({
        "TOP_CENTER, 260, 10, 80, 80",
        "CENTER, 260, 10, 80, 80",
        "BOTTOM_CENTER, 260, 10, 80, 80"
    })
    void alignmentOfCenterChild_withLeftSystemInset_andOffsetCausedByInsufficientHorizontalSpace(
            Pos pos, double x, double y, double width, double height) {
        var leftSystemInset = (ObjectProperty<Dimension2D>)ReflectionUtils.getFieldValue(headerBar, "leftSystemInset");
        leftSystemInset.set(new Dimension2D(200, 100));
        var leading = new MockResizable(50, 50);
        var center = new MockResizable(100, 50);
        var trailing = new MockResizable(150, 50);
        HeaderBar.setAlignment(center, pos);
        HeaderBar.setMargin(center, new Insets(10));
        headerBar.setLeading(leading);
        headerBar.setCenter(center);
        headerBar.setTrailing(trailing);
        headerBar.resize(500, 100);
        headerBar.layout();

        assertBounds(x, y, width, height, center);
    }

    @ParameterizedTest
    @SuppressWarnings("unchecked")
    @CsvSource({
        "TOP_CENTER, 60, 10, 80, 80",
        "CENTER, 60, 10, 80, 80",
        "BOTTOM_CENTER, 60, 10, 80, 80"
    })
    void alignmentOfCenterChild_withRightSystemInset_andOffsetCausedByInsufficientHorizontalSpace(
            Pos pos, double x, double y, double width, double height) {
        var rightSystemInset = (ObjectProperty<Dimension2D>)ReflectionUtils.getFieldValue(headerBar, "rightSystemInset");
        rightSystemInset.set(new Dimension2D(200, 100));
        var leading = new MockResizable(50, 50);
        var center = new MockResizable(100, 50);
        var trailing = new MockResizable(150, 50);
        HeaderBar.setAlignment(center, pos);
        HeaderBar.setMargin(center, new Insets(10));
        headerBar.setLeading(leading);
        headerBar.setCenter(center);
        headerBar.setTrailing(trailing);
        headerBar.resize(500, 100);
        headerBar.layout();

        assertBounds(x, y, width, height, center);
    }

    private void assertBounds(double x, double y, double width, double height, Node node) {
        var bounds = node.getLayoutBounds();
        assertEquals(x, node.getLayoutX());
        assertEquals(y, node.getLayoutY());
        assertEquals(width, bounds.getWidth());
        assertEquals(height, bounds.getHeight());
    }
}
