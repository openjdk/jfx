/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.stage;

import java.util.ArrayList;
import java.util.stream.Stream;
import javafx.geometry.AnchorPoint;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.AnchorPolicy;
import javafx.stage.Stage;
import test.com.sun.javafx.pgstub.StubStage;
import test.com.sun.javafx.pgstub.StubToolkit;
import test.com.sun.javafx.pgstub.StubToolkit.ScreenConfiguration;
import com.sun.javafx.stage.WindowHelper;
import com.sun.javafx.tk.Toolkit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

public class StageTest {

    private StubToolkit toolkit;
    private Stage s;
    private StubStage peer;

    private int initialNumTimesSetSizeAndLocation;

    @BeforeEach
    public void setUp() {
        toolkit = (StubToolkit) Toolkit.getToolkit();
        toolkit.setScreens(new ScreenConfiguration(0, 0, 800, 600, 0, 0, 800, 600, 96));

        s = new Stage();
        s.setOnShown(_ -> {
            peer = (StubStage) WindowHelper.getPeer(s);
            initialNumTimesSetSizeAndLocation = peer.numTimesSetSizeAndLocation;
        });
    }

    @AfterEach
    public void tearDown() {
        s.hide();
        toolkit.resetScreens();
    }

    private void pulse() {
        toolkit.fireTestPulse();
    }

    /**
     * Simple test which checks whether changing the x/y position of the Stage
     * ends up invoking the appropriate methods on the TKStage interface.
     */
    @Test
    public void testMovingStage() {
        s.show();
        s.setX(100);
        pulse();
        assertEquals(100f, peer.x);
        // Setting X should result in a single call to peer.setBounds()
        assertEquals(1, peer.numTimesSetSizeAndLocation - initialNumTimesSetSizeAndLocation);
    }

    /**
     * Simple test which checks whether changing the w/h size of the Stage
     * ends up invoking the appropriate methods on the TKStage interface.
     */
    @Test
    public void testResizingStage() {
        s.show();
        s.setWidth(100);
        s.setHeight(100);
        pulse();
        assertEquals(100f, peer.width);
        assertEquals(100f, peer.height);
        // Setting W and H should result in a single call to peer.setBounds()
        assertEquals(1, peer.numTimesSetSizeAndLocation - initialNumTimesSetSizeAndLocation);
    }

    /**
     * Simple test which checks whether changing the w/h size and x/y position of the Stage
     * ends up invoking the appropriate methods on the TKStage interface.
     */
    @Test
    public void testMovingAndResizingStage() {
        s.show();
        s.setX(101);
        s.setY(102);
        s.setWidth(103);
        s.setHeight(104);
        pulse();
        assertEquals(101f, peer.x);
        assertEquals(102f, peer.y);
        assertEquals(103f, peer.width);
        assertEquals(104f, peer.height);
        // Setting X, Y, W and H should result in a single call to peer.setBounds()
        assertEquals(1, peer.numTimesSetSizeAndLocation - initialNumTimesSetSizeAndLocation);
    }

    /**
     * Simple test which checks whether changing the minimum w/h of the Stage
     * resize the window if necessary
     */
    @Test
    public void testResizingTooSmallStage() {
        s.show();
        s.setWidth(60);
        s.setHeight(70);
        s.setMinWidth(150);
        s.setMinHeight(140);
        pulse();
        assertEquals(150.0, peer.width, 0.0001);
        assertEquals(140.0, peer.height, 0.0001);
    }

    /**
     * Simple test which checks whether changing the maximum w/h of the Stage
     * resize the window if necessary
     */
    @Test
    public void testResizingTooBigStage() {
        s.show();
        s.setWidth(100);
        s.setHeight(100);
        s.setMaxWidth(60);
        s.setMaxHeight(70);
        pulse();
        assertEquals(60.0, peer.width, 0.0001);
        assertEquals(70.0, peer.height, 0.0001);
    }

    /**
     * Test to make sure that when we initialize, the Stage doesn't notify
     * the peer of size and location more than once.
     */
    @Test
    public void testSizeAndLocationChangedOverTime() {
        s.show();
        pulse();
        assertTrue((peer.numTimesSetSizeAndLocation - initialNumTimesSetSizeAndLocation) <= 1);
        initialNumTimesSetSizeAndLocation = peer.numTimesSetSizeAndLocation;
        // Oncethe width/height is set it is synced once on pulse
        s.setWidth(300);
        s.setHeight(400);
        pulse();
        assertEquals(300f, peer.width);
        assertEquals(400f, peer.height);
        assertEquals(1, peer.numTimesSetSizeAndLocation - initialNumTimesSetSizeAndLocation);
        // Setting y will trigger one more sync
        s.setY(200);
        pulse();
        assertEquals(200f, peer.y);
        assertEquals(2, peer.numTimesSetSizeAndLocation - initialNumTimesSetSizeAndLocation);
        // .. same for setting x
        s.setX(100);
        pulse();
        assertEquals(100f, peer.x);
        assertEquals(3, peer.numTimesSetSizeAndLocation - initialNumTimesSetSizeAndLocation);
    }

    @Test
    public void testSecondCenterOnScreenNotIgnored() {
        s.show();
        s.centerOnScreen();

        s.setX(0);
        s.setY(0);

        s.centerOnScreen();

        pulse();

        assertTrue(Math.abs(peer.x) > 0.0001);
        assertTrue(Math.abs(peer.y) > 0.0001);
    }

    @Test
    public void testSecondSizeToSceneNotIgnored() {
        s.show();
        final Scene scene = new Scene(new Group(), 200, 100);
        s.setScene(scene);

        s.sizeToScene();

        s.setWidth(400);
        s.setHeight(300);

        s.sizeToScene();

        pulse();

        assertTrue(Math.abs(peer.width - 400) > 0.0001);
        assertTrue(Math.abs(peer.height - 300) > 0.0001);
    }

    @Test
    public void testCenterOnScreenForWindowOnSecondScreen() {
        toolkit.setScreens(
                new ScreenConfiguration(0, 0, 1920, 1200, 0, 0, 1920, 1172, 96),
                new ScreenConfiguration(1920, 160, 1440, 900,
                                        1920, 160, 1440, 900, 96));

        try {
            s.show();
            s.setX(1920);
            s.setY(160);
            s.setWidth(300);
            s.setHeight(200);

            s.centerOnScreen();
            pulse();

            assertTrue(peer.x > 1930);
            assertTrue(peer.y > 170);
        } finally {
            toolkit.resetScreens();
        }
    }

    @Test
    public void testCenterOnScreenForOwnerOnSecondScreen() {
        toolkit.setScreens(
                new ScreenConfiguration(0, 0, 1920, 1200, 0, 0, 1920, 1172, 96),
                new ScreenConfiguration(1920, 160, 1440, 900,
                                        1920, 160, 1440, 900, 96));

        try {
            s.show();
            s.setX(1920);
            s.setY(160);
            s.setWidth(300);
            s.setHeight(200);

            final Stage childStage = new Stage();
            childStage.setWidth(100);
            childStage.setHeight(100);
            childStage.initOwner(s);
            childStage.show();

            childStage.centerOnScreen();

            assertTrue(childStage.getX() > 1930);
            assertTrue(childStage.getY() > 170);
        } finally {
            toolkit.resetScreens();
        }
    }

    @Test
    public void testSwitchSceneWithFixedSize() {
        s.show();
        Scene scene = new Scene(new Group(), 200, 100);
        s.setScene(scene);

        s.setWidth(400);
        s.setHeight(300);

        pulse();

        assertEquals(400, peer.width, 0.0001);
        assertEquals(300, peer.height, 0.0001);
        assertEquals(400, scene.getWidth(), 0.0001);
        assertEquals(300, scene.getHeight(), 0.0001);

        s.setScene(scene = new Scene(new Group(), 220, 110));

        pulse();

        assertEquals(400, peer.width, 0.0001);
        assertEquals(300, peer.height, 0.0001);
        assertEquals(400, scene.getWidth(), 0.0001);
        assertEquals(300, scene.getHeight(), 0.0001);
    }

    @Test
    public void testSetBoundsNotLostForAsyncNotifications() {
        s.show();
        s.setX(20);
        s.setY(50);
        s.setWidth(400);
        s.setHeight(300);

        peer.holdNotifications();
        pulse();

        s.setX(40);
        s.setY(70);
        s.setWidth(380);
        s.setHeight(280);

        peer.releaseNotifications();
        pulse();

        assertEquals(40.0, peer.x, 0.0001);
        assertEquals(70.0, peer.y, 0.0001);
        assertEquals(380.0, peer.width, 0.0001);
        assertEquals(280.0, peer.height, 0.0001);
    }

    @Test
    public void testFullscreenNotLostForAsyncNotifications() {
        s.show();
        peer.holdNotifications();

        s.setFullScreen(true);
        assertTrue(s.isFullScreen());

        s.setFullScreen(false);
        assertFalse(s.isFullScreen());

        peer.releaseSingleNotification();
        assertTrue(s.isFullScreen());

        peer.releaseNotifications();

        assertFalse(s.isFullScreen());
    }

    @Test
    public void testFullScreenNotification() {
        s.show();
        peer.setFullScreen(true);
        assertTrue(s.isFullScreen());
        peer.setFullScreen(false);
        assertFalse(s.isFullScreen());
    }

    @Test
    public void testResizableNotLostForAsyncNotifications() {
        s.show();
        peer.holdNotifications();

        s.setResizable(false);
        assertFalse(s.isResizable());

        s.setResizable(true);
        assertTrue(s.isResizable());

        peer.releaseSingleNotification();
        assertFalse(s.isResizable());

        peer.releaseNotifications();

        assertTrue(s.isResizable());
    }

    @Test
    public void testResizableNotification() {
        s.show();
        peer.setResizable(false);
        assertFalse(s.isResizable());
        peer.setResizable(true);
        assertTrue(s.isResizable());
    }

    @Test
    public void testIconifiedNotLostForAsyncNotifications() {
        s.show();
        peer.holdNotifications();

        s.setIconified(true);
        assertTrue(s.isIconified());

        s.setIconified(false);
        assertFalse(s.isIconified());

        peer.releaseSingleNotification();
        assertTrue(s.isIconified());

        peer.releaseNotifications();

        assertFalse(s.isIconified());
    }

    @Test
    public void testIconifiedNotification() {
        s.show();
        peer.setIconified(true);
        assertTrue(s.isIconified());
        peer.setIconified(false);
        assertFalse(s.isIconified());
    }

    @Test
    public void testMaximixedNotLostForAsyncNotifications() {
        s.show();
        peer.holdNotifications();

        s.setMaximized(true);
        assertTrue(s.isMaximized());

        s.setMaximized(false);
        assertFalse(s.isMaximized());

        peer.releaseSingleNotification();
        assertTrue(s.isMaximized());

        peer.releaseNotifications();

        assertFalse(s.isMaximized());
    }

    @Test
    public void testMaximizedNotification() {
        s.show();
        peer.setMaximized(true);
        assertTrue(s.isMaximized());
        peer.setMaximized(false);
        assertFalse(s.isMaximized());
    }

    @Test
    public void testAlwaysOnTopNotLostForAsyncNotifications() {
        s.show();
        peer.holdNotifications();

        s.setAlwaysOnTop(true);
        assertTrue(s.isAlwaysOnTop());

        s.setAlwaysOnTop(false);
        assertFalse(s.isAlwaysOnTop());

        peer.releaseSingleNotification();
        assertTrue(s.isAlwaysOnTop());

        peer.releaseNotifications();

        assertFalse(s.isAlwaysOnTop());
    }

    @Test
    public void testAlwaysOnTopNotification() {
        s.show();
        peer.setAlwaysOnTop(true);
        assertTrue(s.isAlwaysOnTop());
        peer.setAlwaysOnTop(false);
        assertFalse(s.isAlwaysOnTop());
    }

    @Test
    public void testBoundsSetAfterPeerIsRecreated() {
        s.show();
        s.setX(20);
        s.setY(50);
        s.setWidth(400);
        s.setHeight(300);

        pulse();

        assertEquals(20.0, peer.x, 0.0001);
        assertEquals(50.0, peer.y, 0.0001);
        assertEquals(400.0, peer.width, 0.0001);
        assertEquals(300.0, peer.height, 0.0001);

        // recreates the peer
        s.hide();
        s.show();

        pulse();

        peer = (StubStage) WindowHelper.getPeer(s);
        assertEquals(20.0, peer.x, 0.0001);
        assertEquals(50.0, peer.y, 0.0001);
        assertEquals(400.0, peer.width, 0.0001);
        assertEquals(300.0, peer.height, 0.0001);
    }

    @Test
    public void testAddAndSetNullIcon() {
        String failMessage = "NullPointerException is expected.";
        ArrayList<Image> imageList = new ArrayList<>();
        imageList.add(null);
        try {
            s.getIcons().add(null);
            throw new Exception();
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException, failMessage);
        }
        try {
            s.getIcons().add(0, null);
            throw new Exception();
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException, failMessage);
        }
        try {
            s.getIcons().addAll(null, null);
            throw new Exception();
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException, failMessage);
        }
        try {
            s.getIcons().addAll(imageList);
            throw new Exception();
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException, failMessage);
        }
        try {
            s.getIcons().addAll(0, imageList);
            throw new Exception();
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException, failMessage);
        }
        try {
            s.getIcons().set(0, null);
            throw new Exception();
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException, failMessage);
        }
        try {
            s.getIcons().setAll(imageList);
            throw new Exception();
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException, failMessage);
        }
        try {
            s.getIcons().setAll(null, null);
            throw new Exception();
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException, failMessage);
        }
    }

    @Test
    public void relocateNullArgumentsThrowNPE() {
        s.show();
        assertNotNull(peer);
        assertThrows(NullPointerException.class, () -> s.relocate(null, AnchorPoint.TOP_LEFT, AnchorPolicy.FIXED, Insets.EMPTY));
        assertThrows(NullPointerException.class, () -> s.relocate(AnchorPoint.TOP_LEFT, null, AnchorPolicy.FIXED, Insets.EMPTY));
        assertThrows(NullPointerException.class, () -> s.relocate(AnchorPoint.TOP_LEFT, AnchorPoint.TOP_LEFT, null, Insets.EMPTY));
        assertThrows(NullPointerException.class, () -> s.relocate(AnchorPoint.TOP_LEFT, AnchorPoint.TOP_LEFT, AnchorPolicy.FIXED, null));
    }

    @Test
    public void relocateBeforeShowPositionsStageOnShow() {
        s.setWidth(300);
        s.setHeight(200);
        s.relocate(AnchorPoint.absolute(100, 120), AnchorPoint.TOP_LEFT, AnchorPolicy.FIXED, Insets.EMPTY);
        s.show();

        assertEquals(100, peer.x, 0.0001);
        assertEquals(120, peer.y, 0.0001);
        assertWithinBounds(peer, toolkit.getScreens().getFirst(), Insets.EMPTY);
    }

    @Test
    public void relocateAfterShowMovesStageImmediately() {
        s.setWidth(300);
        s.setHeight(200);
        s.show();
        s.relocate(AnchorPoint.absolute(200, 220), AnchorPoint.TOP_LEFT, AnchorPolicy.FIXED, Insets.EMPTY);
        pulse();

        assertEquals(200, peer.x, 0.0001);
        assertEquals(220, peer.y, 0.0001);
        assertWithinBounds(peer, toolkit.getScreens().getFirst(), Insets.EMPTY);
    }

    @Test
    public void relocateWithProportionalScreenAnchorResolvesAgainstVisualBounds() {
        // Visual bounds differ from full bounds (e.g., task bar / menu bar reserved area).
        toolkit.setScreens(new ScreenConfiguration(0, 0, 800, 600, 0, 30, 800, 570, 96));

        s.setWidth(200);
        s.setHeight(100);

        // Proportional screen anchors are resolved against visual bounds when no fullscreen stage is present.
        s.relocate(AnchorPoint.proportional(0, 0), AnchorPoint.TOP_LEFT, AnchorPolicy.FIXED, Insets.EMPTY);
        s.show();

        assertEquals(0, peer.x, 0.0001);
        assertEquals(30, peer.y, 0.0001);
        assertWithinBounds(peer, toolkit.getScreens().getFirst(), Insets.EMPTY);
    }

    @Test
    public void relocateWithProportionalScreenAnchorUsesCurrentScreen() {
        toolkit.setScreens(
            new ScreenConfiguration(0, 0, 800, 600, 0, 0, 800, 600, 96),
            new ScreenConfiguration(800, 0, 800, 600, 800, 40, 800, 560, 96));

        // Ensure the stage is on screen 2 when resolving the proportional screen anchor.
        s.setX(850);
        s.setY(10);
        s.setWidth(200);
        s.setHeight(200);

        // Center stage on screen 2's visual bounds:
        // screen center = (800 + 0.5*800, 40 + 0.5*560) = (1200, 320)
        // stage top-left = center - (100, 100) = (1100, 220)
        s.relocate(AnchorPoint.proportional(0.5, 0.5), AnchorPoint.CENTER, AnchorPolicy.FIXED, Insets.EMPTY);
        s.show();

        assertEquals(1100, peer.x, 0.0001);
        assertEquals(220, peer.y, 0.0001);
        assertNotWithinBounds(peer, toolkit.getScreens().get(0), Insets.EMPTY);
        assertWithinBounds(peer, toolkit.getScreens().get(1), Insets.EMPTY);
    }

    @Test
    public void relocateCancelsCenterOnScreenWhenCalledBeforeShow() {
        s.setWidth(200);
        s.setHeight(200);
        s.centerOnScreen();

        // If centerOnScreen were honored, we'd expect (300, 200) on 800x600.
        // relocate should override/cancel it.
        s.relocate(AnchorPoint.absolute(0, 0), AnchorPoint.TOP_LEFT, AnchorPolicy.FIXED, Insets.EMPTY);
        s.show();

        assertEquals(0, peer.x, 0.0001);
        assertEquals(0, peer.y, 0.0001);
        assertWithinBounds(peer, toolkit.getScreens().getFirst(), Insets.EMPTY);
    }

    @Test
    public void relocateHonorsPaddingForEnabledEdges() {
        s.setWidth(200);
        s.setHeight(200);

        var padding = new Insets(10, 20, 30, 40); // top, right, bottom, left

        // Ask to place the TOP_LEFT anchor beyond the bottom-right safe area to force adjustment
        s.relocate(AnchorPoint.absolute(800, 600), AnchorPoint.TOP_LEFT, AnchorPolicy.FIXED, padding);
        s.show();

        // Allowed top-left: x <= 800 - 20 - 200 = 580, y <= 600 - 30 - 200 = 370
        assertEquals(580, peer.x, 0.0001);
        assertEquals(370, peer.y, 0.0001);
        assertWithinBounds(peer, toolkit.getScreens().getFirst(), padding);
    }

    @Test
    public void relocateNegativeInsetsDisableConstraintsPerEdge() {
        s.setWidth(300);
        s.setHeight(200);

        // Disable right and bottom constraints (negative), keep left/top enabled at 0.
        var padding = new Insets(0, -1, -1, 0);
        s.relocate(AnchorPoint.absolute(790, 590), AnchorPoint.TOP_LEFT, AnchorPolicy.FIXED, padding);
        s.show();

        assertEquals(790, peer.x, 0.0001);
        assertEquals(590, peer.y, 0.0001);
        assertNotWithinBounds(peer, toolkit.getScreens().getFirst(), padding);
    }

    @Test
    public void relocateOneSidedLeftConstraintOnly() {
        s.setWidth(300);
        s.setHeight(200);

        // Enable left constraint (10), disable others
        var padding = new Insets(-1, -1, -1, 10);
        s.relocate(AnchorPoint.absolute(0, 100), AnchorPoint.TOP_LEFT, AnchorPolicy.FIXED, padding);
        s.show();

        assertEquals(10, peer.x, 0.0001);
        assertEquals(100, peer.y, 0.0001);
        assertWithinBounds(peer, toolkit.getScreens().getFirst(), padding);
    }

    @Test
    public void relocateFlipHorizontalFitsWithoutAdjustment() {
        s.setWidth(300);
        s.setHeight(200);

        // TOP_LEFT at (790,10) overflows to the right.
        // TOP_RIGHT at (790,10) => rawX=790-300=490 fits.
        s.relocate(AnchorPoint.absolute(790, 10), AnchorPoint.TOP_LEFT, AnchorPolicy.FLIP_HORIZONTAL, Insets.EMPTY);
        s.show();

        assertEquals(490, peer.x, 0.0001);
        assertEquals(10, peer.y, 0.0001);
        assertWithinBounds(peer, toolkit.getScreens().getFirst(), Insets.EMPTY);
    }

    @Test
    public void relocateAutoDiagonalBeatsAdjustOnly() {
        s.setWidth(300);
        s.setHeight(200);

        // TOP_LEFT at (790,590) overflows right and bottom.
        // AUTO should choose BOTTOM_RIGHT (diagonal flip) => raw=(490,390) fits with no adjustment.
        s.relocate(AnchorPoint.absolute(790, 590), AnchorPoint.TOP_LEFT, AnchorPolicy.AUTO, Insets.EMPTY);
        s.show();

        assertEquals(490, peer.x, 0.0001);
        assertEquals(390, peer.y, 0.0001);
        assertWithinBounds(peer, toolkit.getScreens().getFirst(), Insets.EMPTY);
    }

    @Test
    public void relocateFlipHorizontalStillRequiresVerticalAdjustment() {
        s.setWidth(300);
        s.setHeight(200);

        // Flip horizontally resolves X, but Y still needs adjustment.
        // TOP_RIGHT raw = (490,590) => y clamps to 400.
        s.relocate(AnchorPoint.absolute(790, 590), AnchorPoint.TOP_LEFT, AnchorPolicy.FLIP_HORIZONTAL, Insets.EMPTY);
        s.show();

        assertEquals(490, peer.x, 0.0001);
        assertEquals(400, peer.y, 0.0001);
        assertWithinBounds(peer, toolkit.getScreens().getFirst(), Insets.EMPTY);
    }

    @Test
    public void relocateFlipVerticalStillRequiresHorizontalAdjustment() {
        s.setWidth(300);
        s.setHeight(200);

        // Flip vertically resolves Y, but X still needs adjustment.
        // BOTTOM_LEFT raw = (790,390) => x clamps to 500.
        s.relocate(AnchorPoint.absolute(790, 590), AnchorPoint.TOP_LEFT, AnchorPolicy.FLIP_VERTICAL, Insets.EMPTY);
        s.show();

        assertEquals(500, peer.x, 0.0001);
        assertEquals(390, peer.y, 0.0001);
        assertWithinBounds(peer, toolkit.getScreens().getFirst(), Insets.EMPTY);
    }

        @Test
    public void relocateAutoWithRightOnlyConstraintFlipsHorizontally() {
        s.setWidth(300);
        s.setHeight(200);

        // Only right edge constrained, others disabled
        var constraints = new Insets(-1, 0, -1, -1);

        // Preferred TOP_LEFT: rawX=790 => violates right constraint (maxX=500)
        // AUTO should choose TOP_RIGHT: rawX = 790-300 = 490 (fits without adjustment)
        s.relocate(AnchorPoint.absolute(790, 10), AnchorPoint.TOP_LEFT, AnchorPolicy.AUTO, constraints);
        s.show();

        assertEquals(490, peer.x, 0.0001);
        assertEquals(10, peer.y, 0.0001);
        assertWithinBounds(peer, toolkit.getScreens().getFirst(), Insets.EMPTY);
    }

    @Test
    public void relocateAutoWithLeftOnlyConstraintDoesNotFlipWhenFlipWouldBeWorse() {
        s.setWidth(300);
        s.setHeight(200);

        // Only left edge constrained to x >= 10, others disabled
        var constraints = new Insets(-1, -1, -1, 10);

        // Preferred TOP_LEFT: rawX = 0 -> adjusted to 10 (cost 10)
        // Flipped TOP_RIGHT: rawX = 0-300 = -300 -> adjusted to 10 (cost 310)
        // AUTO may consider the flip, but should keep the original anchor as "better".
        s.relocate(AnchorPoint.absolute(0, 10), AnchorPoint.TOP_LEFT, AnchorPolicy.AUTO, constraints);
        s.show();

        assertEquals(10, peer.x, 0.0001);
        assertEquals(10, peer.y, 0.0001);
        assertWithinBounds(peer, toolkit.getScreens().getFirst(), Insets.EMPTY);
    }

    @Test
    public void relocateAutoWithBottomOnlyConstraintFlipsVertically() {
        s.setWidth(300);
        s.setHeight(200);

        // Only bottom constrained, others disabled
        var constraints = new Insets(-1, -1, 0, -1);

        // Preferred TOP_LEFT at y=590 => rawY=590 violates bottom maxY=400
        // Vertical flip to BOTTOM_LEFT yields rawY=590-200=390 (fits)
        s.relocate(AnchorPoint.absolute(100, 590), AnchorPoint.TOP_LEFT, AnchorPolicy.AUTO, constraints);
        s.show();

        assertEquals(100, peer.x, 0.0001);
        assertEquals(390, peer.y, 0.0001);
        assertWithinBounds(peer, toolkit.getScreens().getFirst(), Insets.EMPTY);
    }

    @Test
    public void relocateAutoIgnoresDisabledEdgesWhenDecidingWhetherToFlip() {
        s.setWidth(300);
        s.setHeight(200);

        // Disable right constraint, enable left constraint (x >= 0).
        // This means "overflow to the right is allowed", so AUTO should not flip horizontally
        // just because rawX would exceed the screen width.
        var constraints = new Insets(-1, -1, -1, 0);

        s.relocate(AnchorPoint.absolute(790, 10), AnchorPoint.TOP_LEFT, AnchorPolicy.AUTO, constraints);
        s.show();

        // With only left constraint, rawX=790 is allowed (since right is disabled).
        assertEquals(790.0, peer.x, 0.0001);
        assertEquals(10.0, peer.y, 0.0001);
        assertNotWithinBounds(peer, toolkit.getScreens().getFirst(), Insets.EMPTY);
    }

    @Test
    public void relocateWhenStageDoesNotFitInConstrainedSpanUsesAnchorToChooseSide() {
        // Make a screen smaller than the stage, so maxX < minX (and maxY < minY).
        toolkit.setScreens(new ScreenConfiguration(0, 0, 200, 200, 0, 0, 200, 200, 96));
        s.setWidth(300);
        s.setHeight(250);

        // With TOP_LEFT, choose minX/minY in non-fit scenario.
        s.relocate(AnchorPoint.absolute(0, 0), AnchorPoint.TOP_LEFT, AnchorPolicy.FIXED, Insets.EMPTY);
        s.show();
        assertEquals(0, peer.x, 0.0001);
        assertEquals(0, peer.y, 0.0001);

        // Now recreate with TOP_RIGHT and ensure we choose maxX/minY in non-fit scenario.
        s.hide();
        s.setWidth(300);
        s.setHeight(250);
        s.relocate(AnchorPoint.absolute(0, 0), AnchorPoint.TOP_RIGHT, AnchorPolicy.FIXED, Insets.EMPTY);
        s.show();

        assertEquals(-100, peer.x, 0.0001); // maxX = 200 - 300 = -100
        assertEquals(0, peer.y, 0.0001); // choose minY because TOP_RIGHT has y = 0
    }

    @Test
    public void relocateUsesSecondScreenBoundsForConstraints() {
        toolkit.setScreens(
            new ScreenConfiguration(0, 0, 1920, 1200, 0, 0, 1920, 1172, 96),
            new ScreenConfiguration(1920, 160, 1440, 900, 1920, 160, 1440, 900, 96));

        s.setWidth(400);
        s.setHeight(300);

        // Point on screen 2, but near its bottom-right corner.
        var p = AnchorPoint.absolute(1920 + 1440 - 1, 160 + 900 - 1);
        s.relocate(p, AnchorPoint.TOP_LEFT, AnchorPolicy.FIXED, Insets.EMPTY);
        s.show();

        // Clamp within screen 2: x <= 1920+1440-400 = 2960, y <= 160+900-300 = 760
        assertEquals(2960, peer.x, 0.0001);
        assertEquals(760, peer.y, 0.0001);
        assertNotWithinBounds(peer, toolkit.getScreens().get(0), Insets.EMPTY);
        assertWithinBounds(peer, toolkit.getScreens().get(1), Insets.EMPTY);
    }

    @Test
    public void relocateWithZeroSizeAndProportionalAnchorDoesNotProduceNaNAndConstrainsNormally() {
        // Force zero size at positioning time.
        s.setWidth(0);
        s.setHeight(0);

        // Enable all edges (Insets.EMPTY), so negative coordinate requests are constrained.
        s.relocate(AnchorPoint.absolute(-10, -20), AnchorPoint.CENTER, AnchorPolicy.AUTO, Insets.EMPTY);
        s.show();

        // With width/height == 0, maxX == 800, and maxY == 600; raw is (-10, -20) => constrained to (0,0)
        assertEquals(0, peer.x, 0.0001);
        assertEquals(0, peer.y, 0.0001);
        assertFalse(Double.isNaN(peer.x) || Double.isInfinite(peer.x));
        assertFalse(Double.isNaN(peer.y) || Double.isInfinite(peer.y));
    }

    @Test
    public void relocateWithZeroSizeAndImpossibleConstraintsChoosesSideUsingAnchorPosition() {
        s.setWidth(0);
        s.setHeight(0);

        // Make the constrained space impossible even for a zero-size window:
        // Horizontal: minX = 500, maxX = 800 - 400 - 0 = 400 => maxX < minX
        // Vertical:   minY = 300, maxY = 600 - 400 - 0 = 200 => maxY < minY
        var constraints = new Insets(300, 400, 400, 500);

        // x = 0.25 => choose minX (since x <= 0.5)
        // y = 0.75 => choose maxY (since y > 0.5)
        var anchor = AnchorPoint.proportional(0.25, 0.75);

        s.relocate(AnchorPoint.absolute(0, 0), anchor, AnchorPolicy.FIXED, constraints);
        s.show();

        assertEquals(500, peer.x, 0.0001);
        assertEquals(200, peer.y, 0.0001);
    }

    @Test
    public void relocateWithZeroSizeAndAbsoluteAnchorDoesNotDivideByZero() {
        s.setWidth(0);
        s.setHeight(0);

        // Force max < min to exercise the "choose side" fallback.
        var constraints = new Insets(300, 400, 400, 500);
        var anchor = AnchorPoint.absolute(10, 10);

        s.relocate(AnchorPoint.absolute(0, 0), anchor, AnchorPolicy.FIXED, constraints);
        s.show();

        assertEquals(500, peer.x, 0.0001); // minX
        assertEquals(300, peer.y, 0.0001); // minY
    }

    @ParameterizedTest
    @MethodSource("relocateHonorsScreenBounds_arguments")
    public void relocateWithFixedAnchorPolicyHonorsScreenBounds(
            AnchorPoint stageAnchor,
            Insets screenPadding,
            double stageW, double stageH,
            double requestX, double requestY) {
        s.setWidth(stageW);
        s.setHeight(stageH);
        s.relocate(AnchorPoint.absolute(requestX, requestY), stageAnchor, AnchorPolicy.FIXED, screenPadding);
        s.show();

        assertWithinBounds(peer, toolkit.getScreens().getFirst(), screenPadding);
    }

    @ParameterizedTest
    @MethodSource("relocateHonorsScreenBoundsWithPadding_arguments")
    public void relocateWithFixedAnchorPolicyHonorsScreenBoundsWithPadding(
            AnchorPoint stageAnchor,
            Insets screenPadding,
            double stageW, double stageH,
            double requestX, double requestY) {
        s.setWidth(stageW);
        s.setHeight(stageH);
        s.relocate(AnchorPoint.absolute(requestX, requestY), stageAnchor, AnchorPolicy.FIXED, screenPadding);
        s.show();

        assertWithinBounds(peer, toolkit.getScreens().getFirst(), screenPadding);
    }

    private static Stream<Arguments> relocateHonorsScreenBounds_arguments() {
        return relocateHonorsScreenBounds_argumentsImpl(false);
    }

    private static Stream<Arguments> relocateHonorsScreenBoundsWithPadding_arguments() {
        return relocateHonorsScreenBounds_argumentsImpl(true);
    }

    private static Stream<Arguments> relocateHonorsScreenBounds_argumentsImpl(boolean padding) {
        final double screenW = 800;
        final double screenH = 600;
        final double stageW = 200;
        final double stageH = 200;
        final double overshoot = 10; // push past the edge to force adjustment
        final var insets = padding ? new Insets(10, 20, 30, 40) : Insets.EMPTY;

        Stream.Builder<Arguments> b = Stream.builder();
        b.add(Arguments.of(AnchorPoint.TOP_LEFT, insets, stageW, stageH,
                           screenW - stageW + overshoot, screenH - stageH + overshoot));
        b.add(Arguments.of(AnchorPoint.TOP_RIGHT, insets, stageW, stageH,
                           stageW - overshoot, screenH - stageH + overshoot));
        b.add(Arguments.of(AnchorPoint.BOTTOM_LEFT, insets, stageW, stageH,
                           screenW - stageW + overshoot, stageH - overshoot));
        b.add(Arguments.of(AnchorPoint.BOTTOM_RIGHT, insets, stageW, stageH,
                           stageW - overshoot, stageH - overshoot));
        return b.build();
    }

    private static void assertWithinBounds(StubStage peer, ScreenConfiguration screen, Insets padding) {
        assertTrue(isWithinBounds(peer, screen, padding), "Stage is not within bounds");
    }

    private static void assertNotWithinBounds(StubStage peer, ScreenConfiguration screen, Insets padding) {
        assertFalse(isWithinBounds(peer, screen, padding), "Stage is within bounds");
    }

    private static boolean isWithinBounds(StubStage peer, ScreenConfiguration screen, Insets padding) {
        return screen.getMinX() + padding.getLeft() <= peer.x
            && screen.getMinY() + padding.getTop() <= peer.y
            && screen.getMinX() + screen.getWidth() - padding.getRight() >= peer.x + peer.width
            && screen.getMinY() + screen.getHeight() - padding.getBottom() >= peer.y + peer.height;
    }
}
