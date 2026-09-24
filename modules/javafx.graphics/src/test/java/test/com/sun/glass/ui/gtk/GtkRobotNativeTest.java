/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.glass.ui.gtk;

import com.sun.glass.events.KeyEvent;
import com.sun.glass.ui.Application;
import com.sun.glass.ui.GlassRobot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * {@code GtkRobot} (all nine natives) and {@code GtkApplication._isKeyLocked} on a running toolkit: what
 * {@code GlassRobot.cpp} and {@code glass_key.cpp} did at commit {@code 033187ad90}, pinned so that the
 * {@code java.lang.foreign} replacement can be held to it.
 * <ul>
 * <li>{@code _getScreenCapture} turns the root window's pixels into ARGB ints (a known two-colour stage reads back
 * exactly; an area off the screen reads as opaque black), and leaves the array untouched for an empty size or too
 * small an array;</li>
 * <li>{@code _isKeyLocked} answers OFF for Caps Lock and Num Lock on a fresh X server and UNKNOWN for other keys;</li>
 * <li>with {@code -DUSE_ROBOT=true} only (they inject input into the display): the pointer lands where
 * {@code _mouseMove} put it and {@code _getMouseX}/{@code _getMouseY} read it back (clamped by the X server at the
 * screen edges); each button of {@code _mousePress}/{@code _mouseRelease} arrives as that button, wheel notches as
 * scroll events of the right direction and count; {@code _keyPress}/{@code _keyRelease} of Caps Lock toggles the
 * lock state.</li>
 * </ul>
 * All of it runs in {@link GtkGlassChild} on the X11 display of {@code DISPLAY}. The capture colours, the lock
 * states and the pointer clamp hold only on a display like the one they were captured on, and are checked only
 * where {@link GtkGlassChildJvm#requireEnvironment} finds one.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(300)
public class GtkRobotNativeTest {

    static final int GREEN = 0xFF0AC81E;
    static final int ORANGE = 0xFFFA8003;
    static final int FILL = 0x12345678;

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() {
        GtkGlassChildJvm.requireDisplay();
        run = GtkGlassChildJvm.run(GtkRobotNativeTest.class, "robotScenario",
                List.of("-DUSE_ROBOT=" + Boolean.getBoolean("USE_ROBOT")));
    }

    private static String value(String key) {
        String value = run.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return value;
    }

    private static String repeat(int value, int count) {
        String[] parts = new String[count];
        Arrays.fill(parts, Integer.toHexString(value));
        return String.join(",", parts);
    }

    @Test
    public void screenCaptureReadsARGBFromTheRootWindow() {
        // a stage at 100,100 reads back as painted only on a bare display: no window manager, no compositor
        GtkGlassChildJvm.requireEnvironment(GtkRobotNativeTest.class, run, "env.windowManager", "env.compositing",
                "env.scale", "env.depth");
        assertEquals(repeat(GREEN, 20) + "," + repeat(ORANGE, 20), value("capture.row"));
        assertEquals(value("capture.row"), value("capture.row2"));
        Color green = Color.rgb(10, 200, 30);
        assertEquals(green.getRed() + "," + green.getGreen() + "," + green.getBlue() + "," + green.getOpacity(),
                value("capture.pixelColor"));
    }

    @Test
    public void screenCaptureLeavesTheArrayUntouchedWhenItReturnsEarly() {
        String untouched = repeat(FILL, 4);
        assertEquals(untouched, value("capture.zeroWidth"));
        assertEquals(untouched, value("capture.zeroHeight"));
        assertEquals(untouched, value("capture.arrayTooSmall"));
        assertEquals(untouched, value("capture.overflowGuard"));
        GtkGlassChildJvm.requireEnvironment(GtkRobotNativeTest.class, run, "env.gtk", "env.depth");
        // Not an early return: gdk_pixbuf_get_from_window answers opaque black for an area off the root window,
        // which is what the JNI build of commit 033187ad90 copied into the array.
        assertEquals(repeat(0xFF000000, 4), value("capture.offScreen"));
    }

    @Test
    public void keyLockStatesOfAFreshServer() {
        assertEquals("Optional.empty", value("keyLock.other"));
        // the lock states of the private server they were captured on, which runs no window manager; a desktop
        // session may well have Num Lock on
        GtkGlassChildJvm.requireEnvironment(GtkRobotNativeTest.class, run, "env.windowManager");
        assertEquals("Optional[false]", value("keyLock.caps"));
        assertEquals("Optional[false]", value("keyLock.num"));
    }

    @Test
    public void pointerLandsWhereItWasMoved() {
        assumeTrue(Boolean.getBoolean("USE_ROBOT"), "needs -DUSE_ROBOT=true");
        GtkGlassChildJvm.requireEnvironment(GtkRobotNativeTest.class, run, "env.screen", "env.scale");
        assertEquals("123,77", value("mouse.move.123.77"));
        assertEquals("0,0", value("mouse.move.-5.-5"));
        assertEquals("1919,1079", value("mouse.move.5000.5000"));
        assertEquals("1,1", value("mouse.move.1.1"));
    }

    @Test
    public void eachButtonArrivesAsItself() {
        assumeTrue(Boolean.getBoolean("USE_ROBOT"), "needs -DUSE_ROBOT=true");
        assertEquals("PRESSED:PRIMARY,RELEASED:PRIMARY,PRESSED:SECONDARY,RELEASED:SECONDARY,"
                + "PRESSED:MIDDLE,RELEASED:MIDDLE,PRESSED:BACK,RELEASED:BACK,PRESSED:FORWARD,RELEASED:FORWARD",
                value("mouse.buttons"));
    }

    @Test
    public void wheelNotchesArriveAsScrollEvents() {
        assumeTrue(Boolean.getBoolean("USE_ROBOT"), "needs -DUSE_ROBOT=true");
        assertEquals(value("mouse.wheel.expected"), value("mouse.wheel"));
    }

    @Test
    public void capsLockKeyPressTogglesTheLockState() {
        assumeTrue(Boolean.getBoolean("USE_ROBOT"), "needs -DUSE_ROBOT=true");
        assertEquals("Optional[true],Optional[false]", value("key.capsToggle"));
    }

    private static String hex(int[] data) {
        List<String> parts = new ArrayList<>();
        for (int v : data) {
            parts.add(Integer.toHexString(v));
        }
        return String.join(",", parts);
    }

    /** Runs in {@link GtkGlassChild}. */
    static void robotScenario(Map<String, String> out) throws Exception {
        boolean robot = Boolean.getBoolean("USE_ROBOT");
        List<String> mouseEvents = new CopyOnWriteArrayList<>();
        List<String> scrollEvents = new CopyOnWriteArrayList<>();
        Stage stage = GtkGlassChild.onFx(() -> {
            Rectangle left = new Rectangle(0, 0, 30, 40);
            left.setFill(Color.rgb(10, 200, 30));
            Rectangle right = new Rectangle(30, 0, 30, 40);
            right.setFill(Color.rgb(250, 128, 3));
            Scene scene = new Scene(new Group(left, right), 60, 40);
            scene.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> mouseEvents.add("PRESSED:" + e.getButton()));
            scene.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> mouseEvents.add("RELEASED:" + e.getButton()));
            scene.addEventHandler(ScrollEvent.SCROLL, e -> scrollEvents.add(e.getDeltaY() > 0 ? "up" : "down"));
            Stage s = new Stage(StageStyle.UNDECORATED);
            s.setScene(scene);
            s.setX(100);
            s.setY(100);
            s.show();
            return s;
        });
        Thread.sleep(1000);

        GtkGlassChild.onFx(() -> {
            GlassRobot glassRobot = Application.GetApplication().createRobot();
            int[] row = new int[40];
            glassRobot.getScreenCapture(110, 110, 40, 1, row, false);
            out.put("capture.row", hex(row));
            int[] block = new int[40 * 2];
            glassRobot.getScreenCapture(110, 120, 40, 2, block, false);
            out.put("capture.row2", hex(Arrays.copyOfRange(block, 40, 80)));
            Color color = glassRobot.getPixelColor(115, 115);
            out.put("capture.pixelColor", color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ","
                    + color.getOpacity());
            out.put("capture.zeroWidth", capture(glassRobot, 110, 110, 0, 1));
            out.put("capture.zeroHeight", capture(glassRobot, 110, 110, 1, 0));
            out.put("capture.arrayTooSmall", capture(glassRobot, 110, 110, 5, 1));
            out.put("capture.overflowGuard", capture(glassRobot, 110, 110, 0x10000, 0x2000));
            out.put("capture.offScreen", capture(glassRobot, -100, -100, 2, 2));

            out.put("keyLock.caps", Application.GetApplication().isKeyLocked(KeyEvent.VK_CAPS_LOCK).toString());
            out.put("keyLock.num", Application.GetApplication().isKeyLocked(KeyEvent.VK_NUM_LOCK).toString());
            out.put("keyLock.other", Application.GetApplication().isKeyLocked(KeyEvent.VK_A).toString());
            return null;
        });
        GtkGlassChild.recordEnvironment(out);

        if (robot) {
            GlassRobot glassRobot = GtkGlassChild.onFx(() -> Application.GetApplication().createRobot());
            for (int[] target : new int[][] {{123, 77}, {-5, -5}, {5000, 5000}, {1, 1}}) {
                String position = GtkGlassChild.onFx(() -> {
                    glassRobot.mouseMove(target[0], target[1]);
                    return (int) glassRobot.getMouseX() + "," + (int) glassRobot.getMouseY();
                });
                out.put("mouse.move." + target[0] + "." + target[1], position);
            }

            GtkGlassChild.onFx(() -> {
                glassRobot.mouseMove(115, 115);
                return null;
            });
            Thread.sleep(200);
            for (MouseButton button : new MouseButton[] {MouseButton.PRIMARY, MouseButton.SECONDARY,
                    MouseButton.MIDDLE, MouseButton.BACK, MouseButton.FORWARD}) {
                GtkGlassChild.onFx(() -> {
                    glassRobot.mousePress(button);
                    return null;
                });
                Thread.sleep(100);
                GtkGlassChild.onFx(() -> {
                    glassRobot.mouseRelease(button);
                    return null;
                });
                Thread.sleep(100);
            }
            GtkGlassChild.waitFor(3_000, () -> mouseEvents.size() >= 10);
            out.put("mouse.buttons", String.join(",", mouseEvents));

            GtkGlassChild.onFx(() -> {
                glassRobot.mouseWheel(1);
                return null;
            });
            GtkGlassChild.waitFor(3_000, () -> scrollEvents.size() >= 1);
            Thread.sleep(200);
            GtkGlassChild.onFx(() -> {
                glassRobot.mouseWheel(-2);
                return null;
            });
            GtkGlassChild.waitFor(3_000, () -> scrollEvents.size() >= 3);
            Thread.sleep(200);
            out.put("mouse.wheel", String.join(",", scrollEvents));
            out.put("mouse.wheel.expected", "down,up,up");

            List<String> toggles = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                GtkGlassChild.onFx(() -> {
                    glassRobot.keyPress(KeyCode.CAPS);
                    glassRobot.keyRelease(KeyCode.CAPS);
                    return null;
                });
                Thread.sleep(300);
                toggles.add(GtkGlassChild.onFx(
                        () -> Application.GetApplication().isKeyLocked(KeyEvent.VK_CAPS_LOCK).toString()));
            }
            out.put("key.capsToggle", String.join(",", toggles));
        }

        GtkGlassChild.onFx(() -> {
            stage.hide();
            return null;
        });
    }

    private static String capture(GlassRobot glassRobot, int x, int y, int w, int h) {
        int[] data = new int[4];
        Arrays.fill(data, FILL);
        glassRobot.getScreenCapture(x, y, w, h, data, false);
        return hex(data);
    }
}
