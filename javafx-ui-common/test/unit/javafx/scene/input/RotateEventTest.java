/*
 * Copyright (c) 2000, 2010, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.input;

import com.sun.javafx.pgstub.StubScene;
import com.sun.javafx.test.MouseEventGenerator;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.Test;
import static org.junit.Assert.*;

public class RotateEventTest {

    private boolean rotated;
    private boolean rotated2;
    
    @Test
    public void shouldDeliverRotateEventToPickedNode() {
        Scene scene = createScene();
        Rectangle rect = 
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        
        rotated = false;
        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                rotated = true;
            }
        });
        
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 1, 1,
                50, 50, 50, 50, false, false, false, false, false, false);
        
        assertFalse(rotated);

        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 1, 1,
                150, 150, 150, 150, false, false, false, false, false, false);
        
        assertTrue(rotated);
    }
    
    @Test
    public void shouldPassAngles() {
        Scene scene = createScene();
        Rectangle rect = 
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        
        rotated = false;
        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertEquals(90, event.getAngle(), 0.0001);
                assertEquals(-180, event.getTotalAngle(), 0.0001);
                rotated = true;
            }
        });
        
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 90, -180,
                150, 150, 150, 150, false, false, false, false, false, false);
        
        assertTrue(rotated);
    }

    @Test
    public void shouldPassModifiers() {
        Scene scene = createScene();
        Rectangle rect = 
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        
        rotated = false;
        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertTrue(event.isShiftDown());
                assertFalse(event.isControlDown());
                assertTrue(event.isAltDown());
                assertFalse(event.isMetaDown());
                rotated = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                150, 150, 150, 150, true, false, true, false, false, false);
        assertTrue(rotated);

        rotated = false;
        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertFalse(event.isShiftDown());
                assertTrue(event.isControlDown());
                assertFalse(event.isAltDown());
                assertTrue(event.isMetaDown());
                rotated = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                150, 150, 150, 150, false, true, false, true, false, false);
        assertTrue(rotated);
    }

    @Test
    public void shouldPassDirect() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        rotated = false;
        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertTrue(event.isDirect());
                rotated = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(rotated);

        rotated = false;
        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertFalse(event.isDirect());
                rotated = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                150, 150, 150, 150, false, true, false, true, false, false);
        assertTrue(rotated);
    }

    @Test
    public void shouldPassInertia() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        rotated = false;
        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertTrue(event.isInertia());
                rotated = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, true);
        assertTrue(rotated);

        rotated = false;
        rect.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertFalse(event.isInertia());
                rotated = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                150, 150, 150, 150, false, true, false, true, true, false);
        assertTrue(rotated);
    }

    @Test
    public void shouldPassEventType() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        rotated = false;
        rect.setOnRotationStarted(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                rotated = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_STARTED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(rotated);

        rotated = false;
        rect.setOnRotationFinished(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                rotated = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_FINISHED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(rotated);
    }

    @Test
    public void handlingAnyShouldGetAllTypes() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);

        rect.addEventHandler(RotateEvent.ANY, new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                rotated = true;
            }
        });

        rotated = false;
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_STARTED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(rotated);

        rotated = false;
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(rotated);

        rotated = false;
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_FINISHED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(rotated);
    }

    @Test
    public void shouldDeliverWholeGestureToOneNode() {
        Scene scene = createScene();
        Rectangle rect1 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        Rectangle rect2 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(1);

        rect1.addEventHandler(RotateEvent.ANY, new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                rotated = true;
            }
        });
        rect2.addEventHandler(RotateEvent.ANY, new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                rotated2 = true;
            }
        });

        rotated = false;
        rotated2 = false;
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_STARTED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);
        assertTrue(rotated);
        assertFalse(rotated2);

        rotated = false;
        rotated2 = false;
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                250, 250, 250, 250, true, false, true, false, true, false);
        assertTrue(rotated);
        assertFalse(rotated2);

        rotated = false;
        rotated2 = false;
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_FINISHED, 2, 3,
                250, 250, 250, 250, true, false, true, false, true, false);
        assertTrue(rotated);
        assertFalse(rotated2);
    }

    @Test
    public void unknownLocationShouldBeReplacedByMouseLocation() {
        Scene scene = createScene();
        Rectangle rect1 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        Rectangle rect2 =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(1);
        rect1.addEventHandler(RotateEvent.ANY, new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                rotated = true;
            }
        });

        MouseEventGenerator generator = new MouseEventGenerator();

        rotated = false;
        rotated2 = false;
        rect2.setOnRotationStarted(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertEquals(250.0, event.getSceneX(), 0.0001);
                assertEquals(250.0, event.getSceneY(), 0.0001);
                rotated2 = true;
            }
        });
        scene.impl_processMouseEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 250, 250));
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_STARTED, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);
        assertFalse(rotated);
        assertTrue(rotated2);

        rotated = false;
        rotated2 = false;
        rect2.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertEquals(150.0, event.getSceneX(), 0.0001);
                assertEquals(150.0, event.getSceneY(), 0.0001);
                rotated2 = true;
            }
        });
        scene.impl_processMouseEvent(generator.generateMouseEvent(
                MouseEvent.MOUSE_MOVED, 150, 150));
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);
        assertFalse(rotated);
        assertTrue(rotated2);

        rotated = false;
        rotated2 = false;
        rect2.setOnRotationFinished(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertEquals(150.0, event.getSceneX(), 0.0001);
                assertEquals(150.0, event.getSceneY(), 0.0001);
                rotated2 = true;
            }
        });
        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_FINISHED, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);
        assertFalse(rotated);
        assertTrue(rotated2);
    }

    @Test
    public void finishedLocationShouldBeFixed() {
        Scene scene = createScene();
        Rectangle rect =
                (Rectangle) scene.getRoot().getChildrenUnmodifiable().get(0);
        rect.setOnRotationFinished(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                assertEquals(250.0, event.getSceneX(), 0.0001);
                assertEquals(250.0, event.getSceneY(), 0.0001);
                rotated = true;
            }
        });

        rotated = false;

        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_STARTED, 2, 3,
                150, 150, 150, 150, true, false, true, false, true, false);

        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATE, 2, 3,
                250, 250, 250, 250, true, false, true, false, true, false);

        assertFalse(rotated);

        ((StubScene) scene.impl_getPeer()).getListener().rotateEvent(
                RotateEvent.ROTATION_FINISHED, 2, 3,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                true, false, true, false, true, false);

        assertTrue(rotated);
    }

    private Scene createScene() {
        final Group root = new Group();
        
        final Scene scene = new Scene(root, 400, 400);

        Rectangle rect = new Rectangle(100, 100, 100, 100);
        Rectangle rect2 = new Rectangle(200, 200, 100, 100);

        root.getChildren().addAll(rect, rect2);

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        
        return scene;
    }
}
