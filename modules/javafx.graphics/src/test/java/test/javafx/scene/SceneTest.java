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

package test.javafx.scene;

import com.sun.javafx.scene.KeyboardShortcutsHandler;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.SceneEventDispatcher;
import com.sun.javafx.scene.SceneHelper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

import test.com.sun.javafx.pgstub.StubScene;
import test.com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.sg.prism.NGCamera;
import test.com.sun.javafx.test.MouseEventGenerator;
import com.sun.javafx.tk.Toolkit;

import javafx.application.Platform;
import javafx.scene.input.MouseEvent;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.scene.Camera;
import javafx.scene.Cursor;
import javafx.scene.CursorShim;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.ParallelCamera;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneShim;
import javafx.scene.SubScene;
import test.util.memory.JMemoryBuddy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests various aspects of Scene.
 *
 */
public class SceneTest {

    private Stage stage;
    private boolean handler1Called = false;
    private boolean handler2Called = false;


    @BeforeEach
    public void setUp() {
        stage = new Stage();
        stage.show();
        stage.requestFocus();
    }

    @AfterEach
    public void tearDown() {
        stage.hide();
    }

    /***************************************************************************
     *                                                                         *
     *                           Lookup related tests                          *
     *                                                                         *
     **************************************************************************/
    @Test
    public void testLookupCorrectId() {
        Node n;
        Group root = new Group();
        Scene scene = new Scene(root);
        Rectangle a = new Rectangle(); a.setId("a");
        Rectangle b = new Rectangle(); a.setId("b");
        Rectangle c = new Rectangle(); a.setId("c");
        Group g = new Group();
        g.setId("d");

        Rectangle r1 = new Rectangle(); a.setId("1");
        Rectangle r2 = new Rectangle(); a.setId("2");
        Rectangle r3 = new Rectangle(); a.setId("3");
        n = new Rectangle(); n.setId("4");
        Rectangle r5 = new Rectangle(); a.setId("5");
        Rectangle r6 = new Rectangle(); a.setId("6");

        Rectangle e = new Rectangle(); a.setId("e");
        Rectangle f = new Rectangle(); a.setId("f");

        g.getChildren().addAll(r1,r2,r3,n,r5,r6);

        root.getChildren().addAll(a,b,c,g,e,f);

        assertEquals(n, scene.lookup("#4"));
    }

    @Test
    public void testLookupBadId() {
        Node n;
        Group root = new Group();
        Scene scene = new Scene(root);
        Rectangle a = new Rectangle(); a.setId("a");
        Rectangle b = new Rectangle(); a.setId("b");
        Rectangle c = new Rectangle(); a.setId("c");
        Group g = new Group();
        g.setId("d");

        Rectangle r1 = new Rectangle(); a.setId("1");
        Rectangle r2 = new Rectangle(); a.setId("2");
        Rectangle r3 = new Rectangle(); a.setId("3");
        n = new Rectangle(); n.setId("4");
        Rectangle r5 = new Rectangle(); a.setId("5");
        Rectangle r6 = new Rectangle(); a.setId("6");

        Rectangle e = new Rectangle(); a.setId("e");
        Rectangle f = new Rectangle(); a.setId("f");

        g.getChildren().addAll(r1,r2,r3,n,r5,r6);

        root.getChildren().addAll(a,b,c,g,e,f);

        assertNull(scene.lookup("#4444"));
    }

    /***************************************************************************
     *                                                                         *
     *                          Scene Content Tests                            *
     *                                                                         *
     **************************************************************************/
    @Test
    public void isOnFxAppThread() {
        assertTrue(Platform.isFxApplicationThread());
    }

    @Test
    public void testNullRoot() {
        assertThrows(NullPointerException.class, () -> {
            Scene scene = new Scene(null);
        });
    }

    @Test
    public void testSetNullRoot() {
        assertThrows(NullPointerException.class, () -> {
            Scene scene = new Scene(new Group());
            scene.setRoot(null);
        });
    }

    @Test
    public void testRootInitializedInConstructor() {
        Group g = new Group();
        Scene scene = new Scene(g);

        assertEquals(g, scene.getRoot());
        assertEquals(scene, g.getScene());
    }

    @Test
    public void testDepthBufferInitializedInConstructor() {
        Group g = new Group();
        Scene scene = new Scene(g, 100, 100, true);

        assertTrue(scene.isDepthBuffer());
    }

    @Test
    public void testRootUpdatedWhenAddedToScene() {
        Scene scene = new Scene(new Group());

        Group g = new Group();
        scene.setRoot(g);

        assertEquals(g, scene.getRoot());
        assertEquals(scene, g.getScene());
    }

    @Test
    public void testRootUpdatedWhenChangedInScene() {
        Group g = new Group();
        Scene scene = new Scene(g);

        Group g2 = new Group();
        scene.setRoot(g2);

        assertNull(g.getScene());
        assertEquals(g2, scene.getRoot());
        assertEquals(scene, g2.getScene());
    }

    @Test
    public void testRootStyleClassIsClearedWhenRootNodeIsRemovedFromScene() {
        Scene scene = new Scene(new Group());
        Group g = new Group();
        assertFalse(g.getStyleClass().contains("root"));
        scene.setRoot(g);
        assertTrue(g.getStyleClass().contains("root"));
        scene.setRoot(new Group());
        assertFalse(g.getStyleClass().contains("root"));
    }

    @Test
    public void testRootPseudoClassIsSetOnRootNode() {
        var root = PseudoClass.getPseudoClass("root");
        Scene scene = new Scene(new Group());
        Group g = new Group();
        assertFalse(g.getPseudoClassStates().contains(root));
        scene.setRoot(g);
        assertTrue(g.getPseudoClassStates().contains(root));
        scene.setRoot(new Group());
        assertFalse(g.getPseudoClassStates().contains(root));
    }

    @Test
    public void testNodeUpdatedWhenAddedToScene() {
        Group root = new Group();
        Scene scene = new Scene(root);
        Rectangle rect = new Rectangle();

        assertNull(rect.getScene());

        root.getChildren().add(rect);

        assertEquals(scene, rect.getScene());
    }

    @Test
    public void testNodeUpdatedWhenRemovedFromScene() {
        Rectangle rect;
        Group root = new Group();
        Scene scene = new Scene(root);
        root.getChildren().add(rect = new Rectangle());

        assertEquals(scene, rect.getScene());

        root.getChildren().remove(rect);

        assertNull(rect.getScene());
    }

    @Test
    public void testNodeTreeUpdatedWhenAddedToScene() {
        Rectangle rect;
        Group root = new Group();
        Scene scene = new Scene(root);
        Group g = new Group();

        g.getChildren().add(rect = new Rectangle());

        assertNull(rect.getScene());
        assertNull(g.getScene());

        root.getChildren().add(g);

        assertEquals(scene, g.getScene());
        assertEquals(scene, rect.getScene());
    }

    @Test
    public void testNodeTreeUpdatedWhenRemovedFromScene() {
        Rectangle rect;
        Group g;
        Group root = new Group();
        Scene scene = new Scene(root);
        root.getChildren().add(g = new Group());

        g.getChildren().add(rect = new Rectangle());

        assertEquals(scene, g.getScene());
        assertEquals(scene, rect.getScene());

        root.getChildren().remove(g);

        assertNull(rect.getScene());
        assertNull(g.getScene());
    }

    @Test
    public void testNodeTreeUpdatedWhenAddedToChildOfScene() {
        Group parentGroup;
        Group root = new Group();
        Scene scene = new Scene(root);
        root.getChildren().add(parentGroup = new Group());

        Rectangle rect;
        Group childGroup = new Group();
        childGroup.getChildren().add(rect = new Rectangle());

        assertNull(rect.getScene());
        assertNull(childGroup.getScene());
        assertEquals(scene, parentGroup.getScene());

        parentGroup.getChildren().add(childGroup);

        assertEquals(scene, rect.getScene());
        assertEquals(scene, childGroup.getScene());
        assertEquals(scene, parentGroup.getScene());
    }

    @Test
    public void testNodeTreeUpdatedWhenRemovedFromChildOfScene() {
        Group parentGroup;
        Group root = new Group();
        Scene scene = new Scene(root);
        root.getChildren().add(parentGroup = new Group());

        Rectangle rect;
        Group childGroup = new Group();
        parentGroup.getChildren().add(childGroup);
        childGroup.getChildren().add(rect = new Rectangle());

        assertEquals(scene, rect.getScene());
        assertEquals(scene, childGroup.getScene());
        assertEquals(scene, parentGroup.getScene());

        parentGroup.getChildren().remove(childGroup);

        assertNull(rect.getScene());
        assertNull(childGroup.getScene());
        assertEquals(scene, parentGroup.getScene());
    }

    @Test
    public void testSceneSizeSetWhenNotInitialized() {
        Group g = new Group();

        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        r.setWidth(200);
        r.setHeight(200);
        g.getChildren().add(r);

        Scene scene = new Scene(g);
        stage.setScene(scene);

        assertEquals(180, (int) scene.getWidth());
        assertEquals(180, (int) scene.getHeight());
    }

    @Test
    public void testSceneSizeSetWithEffectOnRoot() {
        Group g = new Group();

        g.setEffect(new javafx.scene.effect.DropShadow());

        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        g.getChildren().add(r);
        r.setWidth(200);
        r.setHeight(200);

        Scene scene = new Scene(g);
        stage.setScene(scene);

        assertEquals(189, (int) scene.getWidth());
        assertEquals(189, (int) scene.getHeight());
    }

    @Test
    public void testSceneSizeSetWithClipOnRoot() {
        Group g = new Group();

        Rectangle clip = new Rectangle();
        clip.setX(20); clip.setY(20); clip.setWidth(150); clip.setHeight(150);

        g.setClip(clip);

        Rectangle r = new Rectangle();

        r.setX(20);
        r.setY(20);
        g.getChildren().add(r);
        r.setWidth(200);
        r.setHeight(200);

        Scene scene = new Scene(g);
        stage.setScene(scene);

        assertEquals(170,(int) scene.getWidth());
        assertEquals(170, (int) scene.getHeight());

    }

    @Test
    public void testSceneSizeSetWithTransformOnRoot() {
        Group g = new Group();

        Scale s = new Scale(); s.setX(2.0f); s.setY(2.0f);
        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        g.getChildren().add(r);
        r.setWidth(200);
        r.setHeight(200);

        g.getTransforms().add(s);
        Scene scene = new Scene(g);
        stage.setScene(scene);

        assertEquals(360,(int) scene.getWidth());
        assertEquals(360, (int) scene.getHeight());
    }

    @Test
    public void testSceneSizeSetWithScaleOnRoot() {
        Group g = new Group();

        g.setScaleX(2);
        g.setScaleY(2);
        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        r.setWidth(200);
        r.setHeight(200);
        g.getChildren().add(r);

        Scene scene = new Scene(g);
        stage.setScene(scene);

        assertEquals(280,(int) scene.getWidth());
        assertEquals(280, (int) scene.getHeight());
    }

    @Test
    public void testSceneSizeSetWithRotationOnRoot() {
        Group g = new Group();
        g.setRotate(45);
        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        r.setWidth(200);
        r.setHeight(200);
        g.getChildren().add(r);

        Scene scene = new Scene(g);
        stage.setScene(scene);

        assertTrue(scene.getWidth() > 220.0f && scene.getWidth() < 222.0f);
        assertTrue(scene.getHeight() > 220.0f && scene.getHeight() < 222.0f);
    }

    @Test
    public void testSceneSizeSetWithTranslateOnRoot() {
        Group g = new Group();

        g.setTranslateX(10);
        g.setTranslateY(10);
        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        r.setWidth(200);
        r.setHeight(200);
        g.getChildren().add(r);
        Scene scene = new Scene(g);
        stage.setScene(scene);

        assertEquals(190, (int)scene.getWidth());
        assertEquals(190, (int)scene.getHeight());
    }

    @Test
    public void testSceneSizeSetWithResizableAsRoot() {
        StackPane st = new StackPane();

        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        r.setWidth(200);
        r.setHeight(200);
        st.getChildren().add(r);

        Scene scene = new Scene(st);
        stage.setScene(scene);

        assertEquals(200,(int) scene.getWidth());
        assertEquals(200, (int) scene.getHeight());
    }

    @Test
    public void testSceneSizeWhenWidthInitialized() {
        Group g = new Group();

        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        r.setWidth(100);
        r.setHeight(100);
        g.getChildren().add(r);

        Scene scene = new Scene(g, 200, -1);
        stage.setScene(scene);

        assertEquals(200,(int) scene.getWidth());
        assertEquals(80, (int) scene.getHeight());
    }

    @Test
    public void testSceneSizeWhenHeightInitialized() {
        Group g = new Group();

        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        r.setWidth(100);
        r.setHeight(100);
        g.getChildren().add(r);

        Scene scene = new Scene(g, -1, 300);
        stage.setScene(scene);

        assertEquals(80,(int) scene.getWidth());
        assertEquals(300,(int) scene.getHeight());
    }

    @Test
    public void testSceneSizeWhenWidthAndHeightInitialized() {
        Group g = new Group();

        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        r.setWidth(100);
        r.setHeight(100);
        g.getChildren().add(r);

        Scene scene = new Scene(g, 400, 400);
        stage.setScene(scene);

        assertEquals(400,(int) scene.getWidth());
        assertEquals(400, (int) scene.getHeight());
    }

    @Test
    public void testSceneSizeOverridesResizableRootPrefSize() {
        StackPane s = new StackPane();

        Rectangle r = new Rectangle();
        r.setX(-20);
        r.setY(-20);
        r.setWidth(100);
        r.setHeight(100);
        s.getChildren().add(r);

        Scene scene = new Scene(s, 600, 600);
        stage.setScene(scene);

        assertEquals(600, (int) scene.getWidth());
        assertEquals(600, (int) scene.getHeight());
    }

    @Test
    public void testSceneSizeWithContentBiasOnRoot() {
        Rectangle r1 = new Rectangle(20, 20);
        Rectangle r2 = new Rectangle(20, 20);
        Rectangle r3 = new Rectangle(100, 20);

        TilePane tilePane = new TilePane();
        tilePane.getChildren().addAll(r1, r2);

        final VBox root = new VBox();
        root.getChildren().addAll(tilePane, r3);
        Scene scene = new Scene(root);
        stage.setScene(scene);

        assertEquals(100, (int) scene.getWidth());
        assertEquals(40, (int) scene.getHeight());
    }

    @Test
    public void focusChangeShouldBeAtomic() {
        final Group root = new Group();

        final Rectangle r1 = new Rectangle();
        final Rectangle r2 = new Rectangle();

        root.getChildren().addAll(r1, r2);
        final Scene scene = new Scene(root, 600, 600);
        stage.setScene(scene);

        r1.requestFocus();

        assertTrue(r1.isFocused());
        assertFalse(r2.isFocused());

        handler1Called = false;
        handler2Called = true;

        r1.focusedProperty().addListener((arg0, arg1, focused) -> {
            assertFalse(focused); // r1 is being defocused
            assertTrue(r2.isFocused()); // r2 is already focused
            handler1Called = true;

            root.getChildren().remove(r2); // be evil: remove r2
        });

        r2.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean focused) {
                assertTrue(focused); // r2 is being focused
                assertFalse(r1.isFocused()); // r1 is already defocused
                assertTrue(handler1Called); // r1 listener was called first
                handler2Called = true;
                // remove the listener otherwise thi final defocus calls it again
                r2.focusedProperty().removeListener(this);
            }
        });

        r2.requestFocus();
        assertTrue(handler2Called); // both listeners were called
    }

    @Test
    public void testSetCamera() {
        Camera camera = new PerspectiveCamera();
        Scene scene = new Scene(new Group(camera));
        scene.setCamera(camera);
        assertEquals(scene.getCamera(), camera);
        scene.setCamera(camera);
    }

    @Test
    public void testGetDefaultCamera() {
        Scene scene = new Scene(new Group());
        assertNull(scene.getCamera());
    }

    @Test
    public void testSetNullCamera() {
        Scene scene = new Scene(new Group());
        scene.setCamera(null);
        assertNull(scene.getCamera());
    }

    @Test
    public void testSetIllegalCameraFromOtherScene() {
        assertThrows(IllegalArgumentException.class, () -> {
            Camera camera = new PerspectiveCamera();

            Scene scene1 = new Scene(new Group(camera));
            Scene scene2 = new Scene(new Group());

            scene1.setCamera(camera);
            scene2.setCamera(camera);
        });
    }

    @Test
    public void testSetIllegalCameraFromItsSubScene() {
        assertThrows(IllegalArgumentException.class, () -> {
            Camera camera = new PerspectiveCamera();

            SubScene subScene = new SubScene(new Group(camera), 150, 150);
            Scene scene = new Scene(new Group(subScene));

            subScene.setCamera(camera);
            scene.setCamera(camera);
        });
    }

    @Test
    public void testSetIllegalCameraFromOtherSubScene() {
        assertThrows(IllegalArgumentException.class, () -> {
            Camera camera = new PerspectiveCamera();

            Scene scene = new Scene(new Group());

            SubScene subScene = new SubScene(new Group(camera), 150, 150);
            Scene otherScene = new Scene(new Group(subScene));

            subScene.setCamera(camera);
            scene.setCamera(camera);
        });
    }

    @Test
    public void testSetIllegalCameraFromSubScene() {
        assertThrows(IllegalArgumentException.class, () -> {
            Camera camera = new PerspectiveCamera();

            SubScene subScene = new SubScene(new Group(camera), 150, 150);
            Scene scene = new Scene(new Group());

            subScene.setCamera(camera);
            scene.setCamera(camera);
        });
    }

    @Test
    public void testSetIllegalCameraFromNestedSubScene() {
        assertThrows(IllegalArgumentException.class, () -> {
            Camera camera = new PerspectiveCamera();

            SubScene nestedSubScene = new SubScene(new Group(camera), 100, 100);
            SubScene subScene = new SubScene(new Group(nestedSubScene), 150, 150);
            Scene scene = new Scene(new Group(subScene));

            nestedSubScene.setCamera(camera);
            scene.setCamera(camera);
        });
    }

    @Test
    public void testCameraUpdatesPG() {
        Scene scene = new Scene(new Group(), 300, 200);
        Camera cam = new ParallelCamera();
        stage.setScene(scene);

        scene.setCamera(cam);
        Toolkit.getToolkit().firePulse();

        // verify it has correct owner
        cam.setNearClip(20);
        Toolkit.getToolkit().firePulse();
        NGCamera ngCamera = ((StubScene) SceneHelper.getPeer(scene)).getCamera();
        assertEquals(20, ngCamera.getNearClip(), 0.00001);

        scene.setCamera(null);
        Toolkit.getToolkit().firePulse();
        // verify owner was removed
        cam.setNearClip(30);
        Toolkit.getToolkit().firePulse();
        assertEquals(20, ngCamera.getNearClip(), 0.00001);

        scene.setCamera(cam);
        Toolkit.getToolkit().firePulse();
        // verify it has correct owner
        cam.setNearClip(40);
        Toolkit.getToolkit().firePulse();
        assertEquals(40, ngCamera.getNearClip(), 0.00001);

        NGCamera oldCam = ngCamera;
        scene.setCamera(new ParallelCamera());
        Toolkit.getToolkit().firePulse();
        // verify owner was removed
        cam.setNearClip(50);
        Toolkit.getToolkit().firePulse();
        ngCamera = NodeHelper.getPeer(scene.getCamera());
        assertEquals(40, oldCam.getNearClip(), 0.00001);
        assertEquals(0.1, ngCamera.getNearClip(), 0.00001);
    }

    @Test
    public void testDefaultCameraUpdatesPG() {
        Scene scene = new Scene(new Group(), 300, 200);
        stage.setScene(scene);
        Toolkit.getToolkit().firePulse();
        Camera cam = SceneShim.getEffectiveCamera(scene);

        cam.setNearClip(20);
        Toolkit.getToolkit().firePulse();
        NGCamera camera = ((StubScene) SceneHelper.getPeer(scene)).getCamera();
        assertEquals(20, camera.getNearClip(), 0.00001);
    }

    @Test
    public void scenesCannotShareCamera() {
        assertThrows(IllegalArgumentException.class, () -> {
            Scene scene = new Scene(new Group(), 300, 200);
            Scene scene2 = new Scene(new Group(), 300, 200);
            Camera cam = new ParallelCamera();
            scene.setCamera(cam);
            scene2.setCamera(cam);
        });
    }

    @Test
    public void subSceneAndSceneCannotShareCamera() {
        assertThrows(IllegalArgumentException.class, () -> {
            SubScene sub = new SubScene(new Group(), 100, 100);
            Scene scene = new Scene(new Group(sub), 300, 200);
            Camera cam = new ParallelCamera();
            sub.setCamera(cam);
            scene.setCamera(cam);
        });
    }

    @Test
    public void shouldBeAbleToSetCameraTwiceToScene() {
        Scene scene = new Scene(new Group(), 300, 200);
        Camera cam = new ParallelCamera();
        try {
            scene.setCamera(cam);
            scene.setCamera(cam);
        } catch (IllegalArgumentException e) {
            fail("It didn't allow to 'share' camera with myslef");
        }
    }

    @Test
    public void scenePropertyListenerShouldBeCalledForInitializedScene() {
        final Group root = new Group();
        final Rectangle rect = new Rectangle();
        root.getChildren().add(rect);

        root.sceneProperty().addListener(o -> {
            root.getChildren().remove(rect);
        });

        Scene scene = new Scene(root, 600, 450);
        // if there is no exception, the test passed
    }

    @Test
    public void testSceneCursorChangePropagatesToScenePeer() {
        final StubToolkit toolkit = (StubToolkit) Toolkit.getToolkit();
        final MouseEventGenerator generator = new MouseEventGenerator();

        final Scene scene = new Scene(new Group(), 300, 200);
        stage.setScene(scene);
        SceneHelper.processMouseEvent(scene,
                generator.generateMouseEvent(MouseEvent.MOUSE_ENTERED,
                                             100, 100));
        toolkit.firePulse();

        scene.setCursor(Cursor.TEXT);
        assertTrue(toolkit.isPulseRequested());
        toolkit.firePulse();

        assertSame(CursorShim.getCurrentFrame(Cursor.TEXT),
                   ((StubScene) SceneHelper.getPeer(scene)).getCursor());
    }

    @Test
    public void testNodeCursorChangePropagatesToScenePeer() {
        final StubToolkit toolkit = (StubToolkit) Toolkit.getToolkit();
        final MouseEventGenerator generator = new MouseEventGenerator();

        final Parent root = new Group(new Rectangle(300, 200));
        final Scene scene = new Scene(root, 300, 200);
        stage.setScene(scene);
        SceneHelper.processMouseEvent(scene,
                generator.generateMouseEvent(MouseEvent.MOUSE_ENTERED,
                                             100, 100));
        toolkit.firePulse();

        root.setCursor(Cursor.TEXT);
        assertTrue(toolkit.isPulseRequested());
        toolkit.firePulse();

        assertSame(CursorShim.getCurrentFrame(Cursor.TEXT),
                   ((StubScene) SceneHelper.getPeer(scene)).getCursor());
    }

    @Test public void testProperties() {
        final Scene scene = new Scene(new Group(), 300, 200);

        javafx.collections.ObservableMap<Object, Object> properties = scene.getProperties();

        /* If we ask for it, we should get it.
         */
        assertNotNull(properties);

        /* What we put in, we should get out.
         */
        properties.put("MyKey", "MyValue");
        assertEquals("MyValue", properties.get("MyKey"));

        /* If we ask for it again, we should get the same thing.
         */
        javafx.collections.ObservableMap<Object, Object> properties2 = scene.getProperties();
        assertEquals(properties2, properties);

        /* What we put in to the other one, we should get out of this one because
         * they should be the same thing.
         */
        assertEquals("MyValue", properties2.get("MyKey"));
    }

    /***************************************************************************
     *                                                                         *
     *                   Scene Pulse Listener Tests                            *
     *                                                                         *
     **************************************************************************/

    @Test
    public void testAddNullPreLayoutPulseListener() {
        assertThrows(NullPointerException.class, () -> {
            Scene scene = new Scene(new Group(), 300, 300);
            scene.addPreLayoutPulseListener(null);
        });
    }

    @Test
    public void testAddNullPostLayoutPulseListener() {
        assertThrows(NullPointerException.class, () -> {
            Scene scene = new Scene(new Group(), 300, 300);
            scene.addPostLayoutPulseListener(null);
        });
    }

    @Test public void testRemoveNullPreLayoutPulseListener_nullListenersList() {
        Scene scene = new Scene(new Group(), 300, 300);
        scene.removePreLayoutPulseListener(null);
        // no failure expected
    }

    @Test public void testRemoveNullPostLayoutPulseListener_nullListenersList() {
        Scene scene = new Scene(new Group(), 300, 300);
        scene.removePostLayoutPulseListener(null);
        // no failure expected
    }

    @Test public void testRemoveNullPreLayoutPulseListener_nonNullListenersList() {
        Scene scene = new Scene(new Group(), 300, 300);
        scene.addPreLayoutPulseListener(() -> { });
        scene.removePreLayoutPulseListener(null);
        // no failure expected
    }

    @Test public void testRemoveNullPostLayoutPulseListener_nonNullListenersList() {
        Scene scene = new Scene(new Group(), 300, 300);
        scene.addPostLayoutPulseListener(() -> { });
        scene.removePostLayoutPulseListener(null);
        // no failure expected
    }

    @Test public void testPreLayoutPulseListenerIsFired() {
        Scene scene = new Scene(new Group(), 300, 300);
        final AtomicInteger counter = new AtomicInteger(0);

        assertEquals(0, counter.get());
        scene.addPreLayoutPulseListener(() -> counter.incrementAndGet());
        assertEquals(0, counter.get());

        SceneShim.scenePulseListener_pulse(scene);
        assertEquals(1, counter.get());

        SceneShim.scenePulseListener_pulse(scene);
        assertEquals(2, counter.get());
    }

    @Test public void testPostLayoutPulseListenerIsFired() {
        Scene scene = new Scene(new Group(), 300, 300);
        final AtomicInteger counter = new AtomicInteger(0);

        assertEquals(0, counter.get());
        scene.addPostLayoutPulseListener(() -> counter.incrementAndGet());
        assertEquals(0, counter.get());

        SceneShim.scenePulseListener_pulse(scene);
        assertEquals(1, counter.get());

        SceneShim.scenePulseListener_pulse(scene);
        assertEquals(2, counter.get());
    }

    @Test public void testPreLayoutPulseListenerIsFired_untilRemoved() {
        Scene scene = new Scene(new Group(), 300, 300);
        final AtomicInteger counter = new AtomicInteger(0);

        Runnable r = () -> counter.incrementAndGet();

        assertEquals(0, counter.get());
        scene.addPreLayoutPulseListener(r);
        assertEquals(0, counter.get());

        SceneShim.scenePulseListener_pulse(scene);
        assertEquals(1, counter.get());

        scene.removePreLayoutPulseListener(r);
        SceneShim.scenePulseListener_pulse(scene);
        assertEquals(1, counter.get());
    }

    @Test public void testPostLayoutPulseListenerIsFired_untilRemoved() {
        Scene scene = new Scene(new Group(), 300, 300);
        final AtomicInteger counter = new AtomicInteger(0);

        Runnable r = () -> counter.incrementAndGet();

        assertEquals(0, counter.get());
        scene.addPostLayoutPulseListener(r);
        assertEquals(0, counter.get());

        SceneShim.scenePulseListener_pulse(scene);
        assertEquals(1, counter.get());

        scene.removePostLayoutPulseListener(r);
        SceneShim.scenePulseListener_pulse(scene);
        assertEquals(1, counter.get());
    }

    @Test public void testNoReferencesRemainToRemovedNodeAfterBeingClicked() {
        StubToolkit toolkit = (StubToolkit) Toolkit.getToolkit();
        TilePane pane = new TilePane();
        VBox vbox = new VBox(pane);
        Scene scene = new Scene(vbox, 300, 200);
        WeakReference<TilePane> ref = new WeakReference<>(pane);

        pane.setMinSize(1000, 1000);  // ensure mouse click will hit this node

        stage.setScene(scene);

        // Press mouse on TilePane so it gets picked up as a potential node to drag:
        SceneHelper.processMouseEvent(
            scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 100, 100)
        );

        // Release mouse on TilePane to trigger clean up code as there will be no dragging:
        SceneHelper.processMouseEvent(
            scene,
            MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, 100, 100)
        );

        // Remove TilePane, and replace with something else:
        vbox.getChildren().setAll(new StackPane());

        // Generate a MOUSE_EXITED event for the removed node and a pulse as otherwise many unrelated Scene references
        // hang around to the removed node:
        SceneHelper.processMouseEvent(
                scene,
                new MouseEvent(
                        MouseEvent.MOUSE_EXITED, 100, 100, 100, 100, MouseButton.NONE, 0, false, false, false,
                        false, false, false, false, false, false, true, null
                )
        );

        toolkit.firePulse();

        // Clear our own reference and see if the TilePane is now not referenced anywhere:
        pane = null;

        // Verify TilePane was GC'd:
        JMemoryBuddy.assertCollectable(ref);
    }

    @Test public void testNoReferencesRemainToRemovedNodeAfterStartingFullDrag() {
        TilePane pane = new TilePane();
        pane.setMinSize(200, 200);

        WeakReference<TilePane> ref = new WeakReference<>(pane);

        Group root = new Group(pane);
        final Scene scene = new Scene(root, 400, 400);
        stage.setScene(scene);

        pane.setOnDragDetected(event -> ((Node) event.getSource()).startFullDrag());

        // Simulate a drag operation from the user
        SceneHelper.processMouseEvent(scene,
                MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50));

        SceneHelper.processMouseEvent(scene,
                MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_DRAGGED, 70, 70));

        SceneHelper.processMouseEvent(scene,
                MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_RELEASED, 50, 50));

        root.getChildren().setAll(new StackPane());

        // Generate a MOUSE_EXITED event for the removed node and a pulse as otherwise many unrelated Scene references
        // hang around to the removed node:
        SceneHelper.processMouseEvent(
                scene,
                new MouseEvent(
                        MouseEvent.MOUSE_EXITED, 50, 50, 50, 50, MouseButton.NONE, 0, false, false, false,
                        false, false, false, false, false, false, true, null
                )
        );

        Toolkit.getToolkit().firePulse();

        pane = null;

        JMemoryBuddy.assertCollectable(ref);
    }

    @Test
    public void sceneShouldSet_MnemonicsDisplayEnabled_ToFalseWhenWindowFocusIsLost() {
        Group root = new Group();

        root.setFocusTraversable(true);

        Scene scene = new Scene(root);
        SceneEventDispatcher dispatcher = (SceneEventDispatcher) scene.getEventDispatcher();
        KeyboardShortcutsHandler keyboardShortcutsHandler = dispatcher.getKeyboardShortcutsHandler();

        stage.setScene(scene);
        stage.show();
        stage.requestFocus();

        assertFalse(keyboardShortcutsHandler.isMnemonicsDisplayEnabled());

        keyboardShortcutsHandler.setMnemonicsDisplayEnabled(true);

        assertTrue(keyboardShortcutsHandler.isMnemonicsDisplayEnabled());

        stage.close();

        assertFalse(keyboardShortcutsHandler.isMnemonicsDisplayEnabled());
    }
}
