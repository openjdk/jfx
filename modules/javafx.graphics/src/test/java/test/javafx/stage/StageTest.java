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
import javafx.scene.image.Image;
import com.sun.javafx.stage.WindowHelper;
import javafx.scene.Group;
import javafx.scene.Scene;
import test.com.sun.javafx.pgstub.StubStage;
import test.com.sun.javafx.pgstub.StubToolkit;
import test.com.sun.javafx.pgstub.StubToolkit.ScreenConfiguration;
import com.sun.javafx.tk.Toolkit;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StageTest {

    private StubToolkit toolkit;
    private Stage s;
    private StubStage peer;

    private int initialNumTimesSetSizeAndLocation;

    @BeforeEach
    public void setUp() {
        toolkit = (StubToolkit) Toolkit.getToolkit();
        s = new Stage();
        s.setOnShown(_ -> {
            peer = (StubStage) WindowHelper.getPeer(s);
            initialNumTimesSetSizeAndLocation = peer.numTimesSetSizeAndLocation;
        });
    }

    @AfterEach
    public void tearDown() {
        s.hide();
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

    /**
     * Tests that a stage that is shown with an anchor and placed such that it extends slightly beyond
     * the edges of the screen is repositioned so that it fits within the screen.
     */
    @ParameterizedTest(name = "Clamps to {0} edges with {1} anchor")
    @MethodSource("showWithAnchorClampsWindowToScreenEdges_arguments")
    public void showWithAnchorClampsWindowToScreenEdges(
            @SuppressWarnings("unused") String edge,
            @SuppressWarnings("unused") String anchorName,
            Stage.Anchor anchor,
            double screenW, double screenH,
            double stageW, double stageH,
            double requestX, double requestY) {
        toolkit.setScreens(
            new ScreenConfiguration(
                0, 0, (int)screenW, (int)screenH,
                0, 0, (int)screenW, (int)screenH,
                96));

        try {
            s.setWidth(stageW);
            s.setHeight(stageH);
            s.show(requestX, requestY, anchor);
            pulse();

            assertWithinScreenBounds(peer, toolkit.getScreens().getFirst());
        } finally {
            toolkit.resetScreens();
        }
    }

    private static Stream<Arguments> showWithAnchorClampsWindowToScreenEdges_arguments() {
        final double screenW = 800;
        final double screenH = 600;
        final double stageW = 200;
        final double stageH = 200;
        final double overshoot = 10; // push past the edge to force clamping

        Stream.Builder<Arguments> b = Stream.builder();
        b.add(Arguments.of("bottom and right", "top left", Stage.Anchor.ofRelative(0, 0), screenW, screenH,
                           stageW, stageH, screenW - stageW - overshoot, screenH - stageH - overshoot));
        b.add(Arguments.of("bottom and left", "top right", Stage.Anchor.ofRelative(0, 1), screenW, screenH,
                           stageW, stageH, screenW - stageW - overshoot, stageH - overshoot));
        b.add(Arguments.of("top and right", "bottom left", Stage.Anchor.ofRelative(1, 0), screenW, screenH,
                           stageW, stageH, stageW - overshoot, screenH - stageH - overshoot));
        b.add(Arguments.of("top and left", "bottom right", Stage.Anchor.ofRelative(1, 1), screenW, screenH,
                           stageW, stageH, stageW - overshoot, stageH - overshoot));
        return b.build();
    }

    @Test
    public void showWithAbsoluteAnchorNoClamping() {
        toolkit.setScreens(new ScreenConfiguration(0, 0, 800, 600, 0, 0, 800, 600, 96));

        try {
            s.setWidth(200);
            s.setHeight(100);
            s.show(50, 70, Stage.Anchor.ofAbsolute(0, 0));
            pulse();

            assertTrue(s.isShowing());
            assertEquals(50, peer.x, 0.0001);
            assertEquals(70, peer.y, 0.0001);
            assertWithinScreenBounds(peer, toolkit.getScreens().getFirst());
        } finally {
            toolkit.resetScreens();
        }
    }

    @Test
    public void showWithRelativeCenterAnchor() {
        toolkit.setScreens(new ScreenConfiguration(0, 0, 800, 600, 0, 0, 800, 600, 96));

        try {
            s.setWidth(200);
            s.setHeight(100);
            s.show(400, 300, Stage.Anchor.ofRelative(0.5, 0.5));
            pulse();

            assertEquals(300, peer.x, 0.0001);
            assertEquals(250, peer.y, 0.0001);
            assertWithinScreenBounds(peer, toolkit.getScreens().getFirst());
        } finally {
            toolkit.resetScreens();
        }
    }

    @Test
    public void showWithRelativeCenterAnchorClampsToEdges() {
        toolkit.setScreens(new ScreenConfiguration(0, 0, 800, 600, 0, 0, 800, 600, 96));

        try {
            s.setWidth(200);
            s.setHeight(100);

            // Center at x=790 would imply top-left x=690, which would extend past the right edge (800)
            s.show(790, 100, Stage.Anchor.ofRelative(0.5, 0.5));
            pulse();

            // Clamped to x=800-200=600, y stays as computed (100-50=50) since it's in range
            assertEquals(600, peer.x, 0.0001);
            assertEquals(50, peer.y, 0.0001);
            assertWithinScreenBounds(peer, toolkit.getScreens().getFirst());
        } finally {
            toolkit.resetScreens();
        }
    }

    @Test
    public void showWithAnchorMovesStageWhenAlreadyShowing() {
        toolkit.setScreens(new ScreenConfiguration(0, 0, 800, 600, 0, 0, 800, 600, 96));

        try {
            s.setWidth(200);
            s.setHeight(100);

            s.show(10, 10, Stage.Anchor.ofAbsolute(0, 0));
            pulse();
            assertTrue(s.isShowing());
            assertEquals(10, peer.x, 0.0001);
            assertEquals(10, peer.y, 0.0001);

            // Calling show again should reposition
            s.show(120, 140, Stage.Anchor.ofAbsolute(0, 0));
            pulse();

            assertTrue(s.isShowing());
            assertEquals(120, peer.x, 0.0001);
            assertEquals(140, peer.y, 0.0001);
            assertWithinScreenBounds(peer, toolkit.getScreens().getFirst());
        } finally {
            toolkit.resetScreens();
        }
    }

    @Test
    public void showWithAnchorOnSecondScreenUsesSecondScreenBoundsForClamping() {
        toolkit.setScreens(
            new ScreenConfiguration(0, 0, 1920, 1200, 0, 0, 1920, 1172, 96),
            new ScreenConfiguration(1920, 160, 1440, 900, 1920, 160, 1440, 900, 96));

        try {
            s.setWidth(400);
            s.setHeight(300);

            // Request a position inside the second screen, but would overflow its right/bottom edges.
            double requestX = 1920 + 1440 - 10;
            double requestY = 160 + 900 - 10;

            s.show(requestX, requestY, Stage.Anchor.ofAbsolute(0, 0));
            pulse();

            // Expected clamp against *second* screen bounds:
            assertEquals(2960, peer.x, 0.0001);
            assertEquals(760, peer.y, 0.0001);
            assertWithinScreenBounds(peer, toolkit.getScreens().get(1));
        } finally {
            toolkit.resetScreens();
        }
    }

    private static void assertWithinScreenBounds(StubStage peer, ScreenConfiguration screen) {
        assertTrue(screen.getMinX() <= peer.x);
        assertTrue(screen.getMinY() <= peer.y);
        assertTrue(screen.getMinX() + screen.getWidth() >= peer.x + peer.width);
        assertTrue(screen.getMinY() + screen.getHeight() >= peer.y + peer.height);
    }
}
