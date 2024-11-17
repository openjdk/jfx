/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.control.infrastructure;

import static javafx.scene.layout.AnchorPane.setBottomAnchor;
import static javafx.scene.layout.AnchorPane.setLeftAnchor;
import static javafx.scene.layout.AnchorPane.setRightAnchor;
import static javafx.scene.layout.AnchorPane.setTopAnchor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Collection;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.javafx.tk.Toolkit;

/**
 * Test for MouseEventFirer.
 * <p>
 * The test is parameterized on not/using (old/new) the alternative mouseEvent creation
 * path.
 */
public class MouseEventFirerTest {

    //------------ fields

    private Scene scene;
    private Stage stage;
    private AnchorPane content;

    private static final double EPS = 1;
    // margins for center node
    private final double VERTICAL_DISTANCE = 100.;
    private final double HORIZONTAL_DISTANCE = 20.;

    private Node topLeft, center, bottomRight;

//------- standalone node

    @ParameterizedTest
    @MethodSource("data")
    public void testLocalStandaloneDeltaNegative(boolean useAlternative) {
        Button button = new Button("standalone button, a bit longish");
        assertLocal(useAlternative, button, - 10, - 5);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testLocalStandaloneDelta(boolean useAlternative) {
        Button button = new Button("standalone button, a bit longish");
        assertLocal(useAlternative, button, 10, 5);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testLocalStandalone(boolean useAlternative) {
        Button button = new Button("standalone button, a bit longish");
        assertLocal(useAlternative, button, 0, 0);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testMouseCoordinatesStandaloneDeltaNegative(boolean useAlternative) {
        Button button = new Button("standalone button, a bit longish");
        assertMouseCoordinatesDelta(useAlternative, button, - 10, - 5);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testMouseCoordinatesStandaloneDelta(boolean useAlternative) {
        Button button = new Button("standalone button, a bit longish");
        assertMouseCoordinatesDelta(useAlternative, button, 10, 5);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testMouseCoordinatesStandalone(boolean useAlternative) {
        Button button = new Button("standalone button, a bit longish");
        assertMouseCoordinatesDelta(useAlternative, button, 0, 0);
    }

// --------- test local coordinates

    @ParameterizedTest
    @MethodSource("data")
    public void testLocalDeltaNegative(boolean useAlternative) {
        content.getChildren().forEach(child -> assertLocal(useAlternative, child, - 10, - 5));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testLocalDelta(boolean useAlternative) {
        content.getChildren().forEach(child -> assertLocal(useAlternative, child, 10, 5));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testLocal(boolean useAlternative) {
        content.getChildren().forEach(child -> assertLocal(useAlternative, child, 0, 0));
    }

    /**
     * Fires a mousePressed with the given x/y location on the given target
     * and asserts the local mouse coordinates.
     *
     */
    protected void assertLocal(boolean useAlternative, Node target, double deltaX, double deltaY) {
        MouseEventFirer firer = new MouseEventFirer(target, useAlternative);
        String text = target instanceof Labeled ? ((Labeled) target).getText() : target.getId();
        target.setOnMousePressed(e -> {
            double width = target.getLayoutBounds().getWidth();
            double height = target.getLayoutBounds().getHeight();
            assertEquals(width /2 + deltaX, e.getX(), EPS, "local x of " + text);
            assertEquals(height / 2 + deltaY, e.getY(), EPS, "local y of " + text);
        });
        firer.fireMousePressed(deltaX, deltaY);
    }

//------------ test scene/screen coordinates

    @ParameterizedTest
    @MethodSource("data")
    public void testMouseCoordinatesDeltaNegative(boolean useAlternative) {
        content.getChildren().forEach(child -> assertMouseCoordinatesDelta(useAlternative, child, - 10, - 5));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testMouseCoordinatesDelta(boolean useAlternative) {
        content.getChildren().forEach(child -> assertMouseCoordinatesDelta(useAlternative, child, 10, 5));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testMouseCoordinates(boolean useAlternative) {
        content.getChildren().forEach(child -> assertMouseCoordinatesDelta(useAlternative, child, 0, 0));
    }

    /**
     * Fires a mousePressed with the given x/y location on the given target
     *  and asserts basic mouseEvent constraints.
     */
    protected void assertMouseCoordinatesDelta(boolean useAlternative, Node target, double deltaX, double deltaY) {
        MouseEventFirer firer = new MouseEventFirer(target, useAlternative);
        target.setOnMousePressed(this::assertMouseEventCoordinates);
        firer.fireMousePressed(deltaX, deltaY);
    }

    /**
     * Asserts scene/screen coordinates of event are same as localToScene/Screen.
     */
    protected void assertMouseEventCoordinates(MouseEvent mouse) {
        assertSame(mouse.getTarget(), mouse.getSource());
        Node receiver = (Node) mouse.getTarget();
        String text = receiver instanceof Labeled ? ((Labeled) receiver).getText() : receiver.getId();

        Point2D sceneP = receiver.localToScene(mouse.getX(), mouse.getY());
        assertEquals(sceneP.getX(), mouse.getSceneX(), EPS, "sceneX of " + text);
        assertEquals(sceneP.getY(), mouse.getSceneY(), EPS, "sceneY of " + text);
        Point2D screenP = receiver.localToScreen(mouse.getX(), mouse.getY());
        assertEquals(screenP.getX(), mouse.getScreenX(), EPS, "screenX of " + text);
        assertEquals(screenP.getY(), mouse.getScreenY(), EPS, "screenY of " + text);
    }

 // ------------- parameterized in not/alternative mouseEvent creation

    private static Collection<Boolean> data() {
        // current / alternative mouseEvent creation
        return List.of(
            // @Ignore("8253769")
            // false,
            true
        );
    }

 // ------------ setup/cleanup/intial

    @Test
    public void testFirer() {
        new MouseEventFirer(topLeft, true);
        assertSame(scene, topLeft.getScene(), "sanity: firer must not change hierarchy");
        assertSame(stage, topLeft.getScene().getWindow(), "sanity: firer must not change hierarchy");
    }

    @Test
    public void testAnchorRight() {
        setLeftAnchor(topLeft, null);
        setRightAnchor(topLeft, 0.);
        Toolkit.getToolkit().firePulse();
        assertEquals(content.getWidth() - topLeft.prefWidth(-1), topLeft.getBoundsInParent().getMinX(), EPS);
    }

    @Test
    public void testLayoutBounds() {
        Bounds initial = topLeft.getLayoutBounds();
        setLeftAnchor(topLeft, null);
        setRightAnchor(topLeft, 0.);
        Toolkit.getToolkit().firePulse();
        assertEquals(initial, topLeft.getLayoutBounds(), "sanity: layout bounds unchanged");
    }

    @Test
    public void testContentLayout() {
        assertTrue(stage.isShowing());
        // content sizing controlled by big middle node
        assertEquals(2* VERTICAL_DISTANCE + center.prefHeight(-1), content.getHeight(), EPS);
        assertEquals(2* HORIZONTAL_DISTANCE + center.prefWidth(-1), content.getWidth(), EPS);
        // middle
        assertEquals(HORIZONTAL_DISTANCE, center.getBoundsInParent().getMinX(), EPS);
        assertEquals(VERTICAL_DISTANCE, center.getBoundsInParent().getMinY(),EPS);
        // top
        assertEquals(0, topLeft.getBoundsInParent().getMinX(), EPS);
        assertEquals(0, topLeft.getBoundsInParent().getMinY(), EPS);
        // bottom
        assertEquals(0, bottomRight.getBoundsInParent().getMinX(), EPS);
        assertEquals(content.getHeight() - bottomRight.prefHeight(-1), bottomRight.getBoundsInParent().getMinY(), EPS);
    }

    @BeforeEach
    public void setup() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });
        topLeft = new Button("topLeft");
        // glue to topLeft
        setTopAnchor(topLeft, 0.);
        setLeftAnchor(topLeft, 0.);

        center = new Button("center: a longish text nearly filling horizontally");
        // glue into center
        setTopAnchor(center, VERTICAL_DISTANCE);
        setBottomAnchor(center, VERTICAL_DISTANCE);
        setLeftAnchor(center, HORIZONTAL_DISTANCE);
        setRightAnchor(center, HORIZONTAL_DISTANCE);

        bottomRight = new Button("botRight");
        // glue to bottom-right
        setBottomAnchor(bottomRight, 0.);
        setLeftAnchor(bottomRight, 0.);

        content = new AnchorPane(center, topLeft, bottomRight);
        scene = new Scene(content);
        stage = new Stage();
        stage.setScene(scene);
        stage.show();
    }

    @AfterEach
    public void tearDown() {
        if (stage != null) stage.hide();
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }
}
