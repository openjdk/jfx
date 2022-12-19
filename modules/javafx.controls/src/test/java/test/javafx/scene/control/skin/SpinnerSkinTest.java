/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import javafx.animation.Animation.Status;
import javafx.geometry.BoundingBox;
import javafx.geometry.Insets;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerShim;
import javafx.scene.control.skin.SpinnerSkin;
import javafx.scene.control.skin.SpinnerSkinShim;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.sun.javafx.scene.control.behavior.SpinnerBehavior;
import com.sun.javafx.scene.control.behavior.SpinnerBehaviorShim;

/**
 * Tests for SpinnerSkin
 */
public class SpinnerSkinTest {
    private static final double CONTROL_WIDTH = 300;
    private static final double CONTROL_HEIGHT = 300;
    private static final double PADDING_TOP = 5;
    private static final double PADDING_RIGHT = 7;
    private static final double PADDING_BOTTOM = 11;
    private static final double PADDING_LEFT = 13;
    private static final double WIDTH = CONTROL_WIDTH - PADDING_LEFT - PADDING_RIGHT;
    private static final double HEIGHT = CONTROL_HEIGHT - PADDING_TOP - PADDING_BOTTOM;
    private static final double PADDING_DECREMENT_ARROW_TOP = 1;
    private static final double PADDING_DECREMENT_ARROW_RIGHT = 2;
    private static final double PADDING_DECREMENT_ARROW_BOTTOM = 3;
    private static final double PADDING_DECREMENT_ARROW_LEFT = 4;
    private static final double PADDING_INCREMENT_ARROW_TOP = 2;
    private static final double PADDING_INCREMENT_ARROW_RIGHT = 1;
    private static final double PADDING_INCREMENT_ARROW_BOTTOM = 4;
    private static final double PADDING_INCREMENT_ARROW_LEFT = 3;
    private static final double DECREMENT_ARROW_WIDTH = PADDING_DECREMENT_ARROW_LEFT + PADDING_DECREMENT_ARROW_RIGHT;
    private static final double DECREMENT_ARROW_HEIGHT = PADDING_DECREMENT_ARROW_TOP + PADDING_DECREMENT_ARROW_BOTTOM;
    private static final double INCREMENT_ARROW_WIDTH = PADDING_INCREMENT_ARROW_LEFT + PADDING_INCREMENT_ARROW_RIGHT;
    private static final double INCREMENT_ARROW_HEIGHT = PADDING_INCREMENT_ARROW_TOP + PADDING_INCREMENT_ARROW_BOTTOM;

    private Spinner<?> spinner;

    private Region decrementArrowButton;
    private Region incrementArrowButton;

    @Before
    public void before() {
        spinner = new Spinner<>();
        spinner.resize(CONTROL_WIDTH, CONTROL_HEIGHT);
        spinner.setSkin(new SpinnerSkin<>(spinner));

        decrementArrowButton = (Region)spinner.lookup(".decrement-arrow-button");
        incrementArrowButton = (Region)spinner.lookup(".increment-arrow-button");

        // Give everything some weird paddings so anomalies should not go undetected:
        spinner.setPadding(new Insets(PADDING_TOP, PADDING_RIGHT, PADDING_BOTTOM, PADDING_LEFT));
        decrementArrowButton.setPadding(new Insets(PADDING_DECREMENT_ARROW_TOP, PADDING_DECREMENT_ARROW_RIGHT, PADDING_DECREMENT_ARROW_BOTTOM, PADDING_DECREMENT_ARROW_LEFT));
        incrementArrowButton.setPadding(new Insets(PADDING_INCREMENT_ARROW_TOP, PADDING_INCREMENT_ARROW_RIGHT, PADDING_INCREMENT_ARROW_BOTTOM, PADDING_INCREMENT_ARROW_LEFT));
    }

    @Test
    public void shouldPositionArrowsRightAndAboveEachOther() {  // This is the default style
        spinner.layout();

        double widest = Math.max(DECREMENT_ARROW_WIDTH, INCREMENT_ARROW_WIDTH);

        assertEquals(new BoundingBox(PADDING_LEFT + WIDTH - widest, PADDING_TOP + HEIGHT / 2, widest, HEIGHT / 2), decrementArrowButton.getBoundsInParent());
        assertEquals(new BoundingBox(PADDING_LEFT + WIDTH - widest, PADDING_TOP, widest, HEIGHT / 2), incrementArrowButton.getBoundsInParent());
    }

    @Test
    public void shouldPositionArrowsLeftAndAboveEachOther() {
        spinner.getStyleClass().setAll(Spinner.STYLE_CLASS_ARROWS_ON_LEFT_VERTICAL);
        spinner.layout();

        double widest = Math.max(DECREMENT_ARROW_WIDTH, INCREMENT_ARROW_WIDTH);

        assertEquals(new BoundingBox(PADDING_LEFT, PADDING_TOP + HEIGHT / 2, widest, HEIGHT / 2), decrementArrowButton.getBoundsInParent());
        assertEquals(new BoundingBox(PADDING_LEFT, PADDING_TOP, widest, HEIGHT / 2), incrementArrowButton.getBoundsInParent());
    }

    @Test
    public void shouldPositionArrowsLeftAndNextToEachOther() {
        spinner.getStyleClass().setAll(Spinner.STYLE_CLASS_ARROWS_ON_LEFT_HORIZONTAL);
        spinner.layout();

        assertEquals(new BoundingBox(PADDING_LEFT, PADDING_TOP, DECREMENT_ARROW_WIDTH, HEIGHT), decrementArrowButton.getBoundsInParent());
        assertEquals(new BoundingBox(PADDING_LEFT + DECREMENT_ARROW_WIDTH, PADDING_TOP, INCREMENT_ARROW_WIDTH, HEIGHT), incrementArrowButton.getBoundsInParent());
    }

    @Test
    public void shouldPositionArrowsRightAndNextToEachOther() {
        spinner.getStyleClass().setAll(Spinner.STYLE_CLASS_ARROWS_ON_RIGHT_HORIZONTAL);
        spinner.layout();

        assertEquals(new BoundingBox(PADDING_LEFT + WIDTH - DECREMENT_ARROW_WIDTH - INCREMENT_ARROW_WIDTH, PADDING_TOP, DECREMENT_ARROW_WIDTH, HEIGHT), decrementArrowButton.getBoundsInParent());
        assertEquals(new BoundingBox(PADDING_LEFT + WIDTH - INCREMENT_ARROW_WIDTH, PADDING_TOP, INCREMENT_ARROW_WIDTH, HEIGHT), incrementArrowButton.getBoundsInParent());
    }

    @Test
    public void shouldPositionArrowsSplitLeftAndRight() {
        spinner.getStyleClass().setAll(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
        spinner.layout();

        double widest = Math.max(DECREMENT_ARROW_WIDTH, INCREMENT_ARROW_WIDTH);

        assertEquals(new BoundingBox(PADDING_LEFT, PADDING_TOP, widest, HEIGHT), decrementArrowButton.getBoundsInParent());
        assertEquals(new BoundingBox(PADDING_LEFT + WIDTH - widest, PADDING_TOP, widest, HEIGHT), incrementArrowButton.getBoundsInParent());
    }

    @Test
    public void shouldPositionArrowsSplitTopAndBottom() {
        spinner.getStyleClass().setAll(Spinner.STYLE_CLASS_SPLIT_ARROWS_VERTICAL);
        spinner.layout();

        double tallest = Math.max(DECREMENT_ARROW_HEIGHT, INCREMENT_ARROW_HEIGHT);

        assertEquals(new BoundingBox(PADDING_LEFT, PADDING_TOP + HEIGHT - tallest, WIDTH, tallest), decrementArrowButton.getBoundsInParent());
        assertEquals(new BoundingBox(PADDING_LEFT, PADDING_TOP, WIDTH, tallest), incrementArrowButton.getBoundsInParent());
    }

    /** Tests JDK-8245145: IAE when replacing skins */
    @Test
    public void testSpinnerSkin() {
        Spinner<?> spinner = new Spinner<>();
        spinner.setSkin(new SpinnerSkin<>(spinner));
        spinner.setSkin(new SpinnerSkin<>(spinner));
    }

    @Test
    public void testSpinnerIncrementOnRemovingFromScene() {
        spinner = new Spinner<>(0, 1000, 0);
        spinner.setSkin(new SpinnerSkin<>(spinner));

        HBox root = new HBox(spinner);
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        SpinnerBehavior behavior = SpinnerSkinShim.getSpinnerBehavior((SpinnerSkin)spinner.getSkin());
        behavior.startSpinning(true);

        assertEquals(Status.RUNNING, SpinnerBehaviorShim.getTimeline(behavior).getStatus());
        root.getChildren().clear();
        assertEquals(Status.STOPPED, SpinnerBehaviorShim.getTimeline(behavior).getStatus());
        root.getChildren().setAll(spinner);
        assertEquals(Status.STOPPED, SpinnerBehaviorShim.getTimeline(behavior).getStatus());
    }
}
