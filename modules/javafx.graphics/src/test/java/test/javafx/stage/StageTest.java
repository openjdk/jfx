/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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
        s.show();
        peer = (StubStage) WindowHelper.getPeer(s);
        initialNumTimesSetSizeAndLocation = peer.numTimesSetSizeAndLocation;
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
        peer.setFullScreen(true);
        assertTrue(s.isFullScreen());
        peer.setFullScreen(false);
        assertFalse(s.isFullScreen());
    }

    @Test
    public void testResizableNotLostForAsyncNotifications() {
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
        peer.setResizable(false);
        assertFalse(s.isResizable());
        peer.setResizable(true);
        assertTrue(s.isResizable());
    }

    @Test
    public void testIconifiedNotLostForAsyncNotifications() {
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
        peer.setIconified(true);
        assertTrue(s.isIconified());
        peer.setIconified(false);
        assertFalse(s.isIconified());
    }

    @Test
    public void testMaximixedNotLostForAsyncNotifications() {
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
        peer.setMaximized(true);
        assertTrue(s.isMaximized());
        peer.setMaximized(false);
        assertFalse(s.isMaximized());
    }

    @Test
    public void testAlwaysOnTopNotLostForAsyncNotifications() {
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
        peer.setAlwaysOnTop(true);
        assertTrue(s.isAlwaysOnTop());
        peer.setAlwaysOnTop(false);
        assertFalse(s.isAlwaysOnTop());
    }

    @Test
    public void testBoundsSetAfterPeerIsRecreated() {
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
}
