package test.com.sun.javafx.tk.quantum;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.sun.glass.events.TouchEvent;
import com.sun.glass.ui.Accessible;
import com.sun.javafx.tk.TKSceneListener;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.tk.quantum.PrivilegedSceneListenerAccessor;
import com.sun.javafx.tk.quantum.RotateGestureRecognizer;

import javafx.collections.ObservableList;
import javafx.event.EventType;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodTextRun;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.input.TouchPoint.State;
import javafx.scene.input.ZoomEvent;
import test.com.sun.javafx.pgstub.StubToolkit;

public class RotateGestureRecognizerTest {
    private static final double EPSILON = 0.0001;

    private final List<RotationEvent> rotationEvents = new ArrayList<>();
    private final StubToolkit toolkit = (StubToolkit) Toolkit.getToolkit();
    private final RotateGestureRecognizer recognizer = new RotateGestureRecognizer(createAccessor());

    private long nanos;

    @Nested
    class WhenThereAreTwoTouches {
        {
            clearEvents();
            passTime(0);
            //passTime(Long.MAX_VALUE / 1000 / 1000 - 10000000);  // a value close to Long.MAX_VALUE in nanos to test accuracy

            recognizer.notifyBeginTouchEvent(nanos, 0, false, 0);
            recognizer.notifyNextTouchEvent(nanos, TouchEvent.TOUCH_PRESSED, 1, 100, 100, 100, 100);
            recognizer.notifyNextTouchEvent(nanos, TouchEvent.TOUCH_PRESSED, 2, 150, 100, 150, 100);
            recognizer.notifyEndTouchEvent(nanos);
        }

        @Test
        void shouldHaveNoEvents() {
            assertNoRotationEvents();
        }

        @Nested
        class AndTouchesAreReleased {
            {
                clearEvents();
                passTime(100);

                recognizer.notifyBeginTouchEvent(nanos, 0, false, 0);
                recognizer.notifyNextTouchEvent(nanos, TouchEvent.TOUCH_RELEASED, 1, 100, 100, 100, 100);
                recognizer.notifyNextTouchEvent(nanos, TouchEvent.TOUCH_RELEASED, 2, 150, 100, 150, 100);
                recognizer.notifyEndTouchEvent(nanos);
            }

            @Test
            void shouldSendNoEvents() {
                assertNoRotationEvents();
            }
        }

        @Nested
        class AndSecondTouchIsMoved45DegreesCCW {
            {
                clearEvents();
                passTime(100);

                recognizer.notifyBeginTouchEvent(nanos, 0, false, 0);
                recognizer.notifyNextTouchEvent(nanos, TouchEvent.TOUCH_MOVED, 2, 150, 50, 150, 50);
                recognizer.notifyEndTouchEvent(nanos);
            }

            @Test
            void shouldSendStartRotationEvents() {
                assertRotationEvent(new RotationEvent(RotateEvent.ROTATION_STARTED, 0, 0, 125, 75, 125, 75, false));
                assertRotationEvent(new RotationEvent(RotateEvent.ROTATE, -45, -45, 125, 75, 125, 75, false));
            }

            @Nested
            class AndSecondTouchIsReleased {
                {
                    clearEvents();
                    passTime(100);

                    recognizer.notifyBeginTouchEvent(nanos, 0, false, 0);
                    recognizer.notifyNextTouchEvent(nanos, TouchEvent.TOUCH_RELEASED, 2, 150, 50, 150, 50);
                    recognizer.notifyEndTouchEvent(nanos);
                }

                @Test
                void shouldSendRotationFinishedEventAndDoNoInertia() {
                    assertRotationEvent(new RotationEvent(RotateEvent.ROTATION_FINISHED, 0, -45, 125, 75, 125, 75, false));

                    passTime(250);

                    assertNoRotationEvents();
                }
            }

            @Nested
            class AndSecondTouchIsMovedAnother45DegreesCCW {
                {
                    clearEvents();
                    passTime(100);

                    recognizer.notifyBeginTouchEvent(nanos, 0, false, 0);
                    recognizer.notifyNextTouchEvent(nanos, TouchEvent.TOUCH_MOVED, 2, 100, 50, 100, 50);
                    recognizer.notifyEndTouchEvent(nanos);
                }

                @Test
                void shouldSendAnotherRotationEvent() {
                    assertRotationEvent(new RotationEvent(RotateEvent.ROTATE, -45, -90, 100, 75, 100, 75, false));
                }

                @Nested
                class AndBothTouchesAreReleased {
                    {
                        clearEvents();

                        recognizer.notifyBeginTouchEvent(nanos, 0, false, 0);
                        recognizer.notifyNextTouchEvent(nanos, TouchEvent.TOUCH_RELEASED, 1, 100, 100, 100, 100);
                        recognizer.notifyNextTouchEvent(nanos, TouchEvent.TOUCH_RELEASED, 2, 100, 50, 100, 50);
                        recognizer.notifyEndTouchEvent(nanos);
                    }

                    @Test
                    void shouldSendRotationFinishedEvent() {
                        assertRotationEvent(new RotationEvent(RotateEvent.ROTATION_FINISHED, 0, -90, 100, 75, 100, 75, false));
                    }

                    @Nested
                    class AndTimePasses {
                        {
                            clearEvents();
                            passTime(250);
                        }

                        @Test
                        void shouldDoInertia() {
                            assertRotationEvent(new RotationEvent(RotateEvent.ROTATE, -91.6666, -181.6666, 100, 75, 100, 75, true));

                            // Trigger a few more inertia events:
                            passTime(250);
                            passTime(250);
                            passTime(250);
                            passTime(250);
                            passTime(250);

                            // Assert Inertia events; angle moved slowly reduces to 0:
                            assertRotationEvent(new RotationEvent(RotateEvent.ROTATE, -70.8333, -252.5000, 100, 75, 100, 75, true));
                            assertRotationEvent(new RotationEvent(RotateEvent.ROTATE, -50.0000, -302.5000, 100, 75, 100, 75, true));
                            assertRotationEvent(new RotationEvent(RotateEvent.ROTATE, -29.1666, -331.6666, 100, 75, 100, 75, true));
                            assertRotationEvent(new RotationEvent(RotateEvent.ROTATE,  -8.3333, -340.0000, 100, 75, 100, 75, true));
                            assertRotationEvent(new RotationEvent(RotateEvent.ROTATE,      0.0, -340.0000, 100, 75, 100, 75, true));
                        }

                        @Nested
                        class AndASingleTouchOccurs {
                            {
                                // Initial inertia event:
                                assertRotationEvent(new RotationEvent(RotateEvent.ROTATE, -91.6666, -181.6666, 100, 75, 100, 75, true));

                                passTime(250);

                                // Second intetia event:
                                assertRotationEvent(new RotationEvent(RotateEvent.ROTATE, -70.8333, -252.5000, 100, 75, 100, 75, true));

                                // Halt inertia:
                                recognizer.notifyBeginTouchEvent(nanos, 0, false, 0);
                                recognizer.notifyNextTouchEvent(nanos, TouchEvent.TOUCH_PRESSED, 3, 200, 200, 200, 200);
                                recognizer.notifyEndTouchEvent(nanos);
                            }

                            @Test
                            void shouldHaltInertia() {
                                assertNoRotationEvents();
                                passTime(250);
                                assertNoRotationEvents();
                            }
                        }
                    }
                }
            }
        }
    }

    private void assertRotationEvent(RotationEvent rotationEvent) {
        assertTrue(rotationEvents.size() > 0, "Expected rotation event, but none available");

        RotationEvent remove = rotationEvents.remove(0);

        assertTrue(rotationEvent.anglesCloseToEquals(remove), remove + " must match " + rotationEvent);
    }

    private void assertNoRotationEvents() {
        assertTrue(rotationEvents.isEmpty(), "No rotation event expected, but there were some available: " + rotationEvents);
    }

    private void clearEvents() {
        rotationEvents.clear();
    }

    private void passTime(long millis) {
        nanos += millis * 1000 * 1000;
        toolkit.setAnimationTime(nanos / 1000 / 1000);
    }

    private record RotationEvent(EventType<RotateEvent> eventType, double angle, double totalAngle, double x,
            double y, double screenX, double screenY, boolean inertia) {

        public boolean anglesCloseToEquals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            RotationEvent other = (RotationEvent) obj;
            return Math.abs(angle - other.angle) < EPSILON
                && Objects.equals(eventType, other.eventType)
                && inertia == other.inertia
                && Double.doubleToLongBits(screenX) == Double.doubleToLongBits(other.screenX)
                && Double.doubleToLongBits(screenY) == Double.doubleToLongBits(other.screenY)
                && Math.abs(totalAngle - other.totalAngle) < EPSILON
                && Double.doubleToLongBits(x) == Double.doubleToLongBits(other.x)
                && Double.doubleToLongBits(y) == Double.doubleToLongBits(other.y);
        }
    }

    private PrivilegedSceneListenerAccessor createAccessor() {
        return consumer -> consumer.accept(new TKSceneListener() {
            @Override
            public void changedLocation(float x, float y) {
            }

            @Override
            public void changedSize(float width, float height) {
            }

            @Override
            public void mouseEvent(EventType<MouseEvent> type, double x, double y, double screenX, double screenY,
                    MouseButton button, boolean popupTrigger, boolean synthesized, boolean shiftDown,
                    boolean controlDown, boolean altDown, boolean metaDown, boolean primaryDown, boolean middleDown,
                    boolean secondaryDown, boolean backDown, boolean forwardDown) {
            }

            @Override
            public void keyEvent(KeyEvent keyEvent) {
            }

            @Override
            public void inputMethodEvent(EventType<InputMethodEvent> type,
                    ObservableList<InputMethodTextRun> composed, String committed, int caretPosition) {
            }

            @Override
            public void scrollEvent(EventType<ScrollEvent> eventType, double scrollX, double scrollY,
                    double totalScrollX, double totalScrollY, double xMultiplier, double yMultiplier,
                    int touchCount, int scrollTextX, int scrollTextY, int defaultTextX, int defaultTextY, double x,
                    double y, double screenX, double screenY, boolean _shiftDown, boolean _controlDown,
                    boolean _altDown, boolean _metaDown, boolean _direct, boolean _inertia) {
            }

            @Override
            public void menuEvent(double x, double y, double xAbs, double yAbs, boolean isKeyboardTrigger) {
            }

            @Override
            public void zoomEvent(EventType<ZoomEvent> eventType, double zoomFactor, double totalZoomFactor,
                    double x, double y, double screenX, double screenY, boolean _shiftDown, boolean _controlDown,
                    boolean _altDown, boolean _metaDown, boolean _direct, boolean _inertia) {
            }


            @Override
            public void rotateEvent(EventType<RotateEvent> eventType, double angle, double totalAngle, double x,
                    double y, double screenX, double screenY, boolean _shiftDown, boolean _controlDown,
                    boolean _altDown, boolean _metaDown, boolean _direct, boolean _inertia) {
                rotationEvents.add(new RotationEvent(eventType, angle, totalAngle, x, y, screenX, screenY, _inertia));
            }

            @Override
            public void swipeEvent(EventType<SwipeEvent> eventType, int touchCount, double x, double y,
                    double screenX, double screenY, boolean _shiftDown, boolean _controlDown, boolean _altDown,
                    boolean _metaDown, boolean _direct) {
            }

            @Override
            public void touchEventBegin(long time, int touchCount, boolean isDirect, boolean _shiftDown,
                    boolean _controlDown, boolean _altDown, boolean _metaDown) {
            }

            @Override
            public void touchEventNext(State state, long touchId, double x, double y, double xAbs, double yAbs) {
            }

            @Override
            public void touchEventEnd() {
            }

            @Override
            public Accessible getSceneAccessible() {
                return null;
            }
        });
    }
}
