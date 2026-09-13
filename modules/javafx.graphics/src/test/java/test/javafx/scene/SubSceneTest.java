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

import com.sun.javafx.scene.NodeHelper;
import javafx.css.PseudoClass;
import javafx.stage.Stage;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.javafx.sg.prism.NGSubScene;
import com.sun.javafx.tk.Toolkit;
import javafx.application.Platform;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.NodeShim;
import javafx.scene.ParallelCamera;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.SubSceneShim;
import javafx.scene.layout.Pane;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class SubSceneTest {

    @Test
    public void isOnFxAppThread() {
        assertTrue(Platform.isFxApplicationThread());
    }

    @Test
    public void testNullRoot() {
        assertThrows(NullPointerException.class, () -> {
            SubScene subScene = new SubScene(null, 10, 10);
        });
    }

    @Test
    public void testSetNullRoot() {
        assertThrows(NullPointerException.class, () -> {
            SubScene subScene = new SubScene(new Group(), 10, 10);
            subScene.setRoot(null);
        });
    }

    @Test
    public void testRootInitializedInConstructor() {
        Group g = new Group();
        SubScene subScene = new SubScene(g, 10, 10);

        assertEquals(g, subScene.getRoot());
        assertEquals(subScene, NodeShim.getSubScene(g));
    }

    @Test
    public void testWidthHeightInitializedInConstructor() {
        int width = 600;
        int height = 400;
        SubScene subScene = new SubScene(new Group(), width, height);

        assertTrue(subScene.getWidth() == width);
        assertTrue(subScene.getHeight() == height);
    }

    @Test
    public void testDepthBufferInitializedInConstructor() {
        Group g = new Group();
        SubScene subScene = new SubScene(g, 10, 10, true, SceneAntialiasing.DISABLED);

        assertTrue(subScene.isDepthBuffer());
    }

   @Test
       public void testNullAntiAliasingInitializedInConstructor() {
        Group g = new Group();
        SubScene subScene = new SubScene(g, 10, 10, false, null);

        assertTrue(subScene.getAntiAliasing() == null);
    }

   @Test
    public void testAntiAliasingInitializedInConstructor() {
        Group g = new Group();
        SubScene subScene = new SubScene(g, 10, 10, false, SceneAntialiasing.BALANCED);

        assertTrue(subScene.getAntiAliasing() == SceneAntialiasing.BALANCED);
    }

    @Test
    public void testRootUpdatedWhenAddedToSubScene() {
        SubScene subScene = new SubScene(new Group(), 10, 10);

        Group g = new Group();
        subScene.setRoot(g);

        assertEquals(g, subScene.getRoot());
        assertEquals(subScene, NodeShim.getSubScene(g));
    }

    @Test
    public void testRootUpdatedWhenChangedInSubScene() {
        Group g = new Group();
        SubScene subScene = new SubScene(g, 10, 10);

        Group g2 = new Group();
        subScene.setRoot(g2);

        assertNull(NodeShim.getSubScene(g));
        assertEquals(g2, subScene.getRoot());
        assertEquals(subScene, NodeShim.getSubScene(g2));
    }

    @Test
    public void testRootStyleClassIsClearedWhenRootNodeIsRemovedFromSubScene() {
        SubScene scene = new SubScene(new Group(), 10, 10);
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
        SubScene scene = new SubScene(new Group(), 10, 10);
        Group g = new Group();
        assertFalse(g.getPseudoClassStates().contains(root));
        scene.setRoot(g);
        assertTrue(g.getPseudoClassStates().contains(root));
        scene.setRoot(new Group());
        assertFalse(g.getPseudoClassStates().contains(root));
    }

    @Test
    public void testSetCamera() {
        Camera camera = new PerspectiveCamera();
        SubScene subScene = new SubScene(new Group(camera), 100, 100);
        Scene scene = new Scene(new Group(subScene));
        subScene.setCamera(camera);
        assertEquals(subScene.getCamera(), camera);
        assertNull(scene.getCamera());
        subScene.setCamera(camera);
    }

    @Test
    public void testSetCameraToNestedSubScene() {
        Camera camera = new PerspectiveCamera();
        SubScene nestedSubScene = new SubScene(new Group(camera), 100, 100);
        SubScene subScene = new SubScene(new Group(nestedSubScene), 150, 150);
        Scene scene = new Scene(new Group(subScene));
        nestedSubScene.setCamera(camera);
        assertEquals(nestedSubScene.getCamera(), camera);
        assertNull(subScene.getCamera());
        assertNull(scene.getCamera());
    }

    @Test
    public void testGetDefaultCamera() {
        SubScene subScene = new SubScene(new Group(), 100, 100);
        Scene scene = new Scene(new Group(subScene));
        assertNull(subScene.getCamera());
    }

    @Test
    public void testSetNullCamera() {
        SubScene subScene = new SubScene(new Group(), 100, 100);
        Scene scene = new Scene(new Group(subScene));
        subScene.setCamera(null);
        assertNull(subScene.getCamera());
    }

    @Test
    public void testSetIllegalCameraFromOtherScene() {
        assertThrows(IllegalArgumentException.class, () -> {
            Camera camera = new PerspectiveCamera();

            Scene scene = new Scene(new Group(camera));

            SubScene subScene = new SubScene(new Group(), 150, 150);
            Scene otherScene = new Scene(new Group(subScene));

            scene.setCamera(camera);
            subScene.setCamera(camera);
        });
    }

    @Test
    public void testSetIllegalCameraFromItsScene() {
        assertThrows(IllegalArgumentException.class, () -> {
            Camera camera = new PerspectiveCamera();

            SubScene subScene = new SubScene(new Group(), 150, 150);
            Scene scene = new Scene(new Group(camera, subScene));

            scene.setCamera(camera);
            subScene.setCamera(camera);
        });
    }

    @Test
    public void testSetIllegalCameraFromOtherSubScene() {
        assertThrows(IllegalArgumentException.class, () -> {
            Camera camera = new PerspectiveCamera();

            SubScene subScene = new SubScene(new Group(), 150, 150);
            Scene scene = new Scene(new Group(subScene));

            SubScene otherSubScene = new SubScene(new Group(camera), 100, 100);
            Scene otherScene = new Scene(new Group(otherSubScene));

            otherSubScene.setCamera(camera);
            subScene.setCamera(camera);
        });
    }

    @Test
    public void testSetIllegalCameraFromSubScene() {
        assertThrows(IllegalArgumentException.class, () -> {
            Camera camera = new PerspectiveCamera();

            SubScene subScene = new SubScene(new Group(), 150, 150);
            Scene scene = new Scene(new Group(subScene));

            SubScene otherSubScene = new SubScene(new Group(camera), 100, 100);

            otherSubScene.setCamera(camera);
            subScene.setCamera(camera);
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
            subScene.setCamera(camera);
        });
    }

    @Test
    public void testCameraUpdatesPG() {
        SubScene sub = new SubScene(new Group(), 100, 100);
        Scene scene = new Scene(new Group(sub), 300, 200);
        Camera cam = new ParallelCamera();
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        sub.setCamera(cam);
        Toolkit.getToolkit().firePulse();

        // verify it has correct owner
        cam.setNearClip(20);
        Toolkit.getToolkit().firePulse();
        NGSubScene peer = NodeHelper.getPeer(sub);
        assertEquals(20, peer.getCamera().getNearClip(), 0.00001);

        sub.setCamera(null); // Sets the default camera, which is parallel camera
        ParallelCamera pCam = new ParallelCamera(); // Like default cam
        Toolkit.getToolkit().firePulse();
        // verify owner was removed
        cam.setNearClip(30);
        Toolkit.getToolkit().firePulse();
        assertEquals(pCam.getNearClip(), peer.getCamera().getNearClip(), 0.00001);

        sub.setCamera(cam);
        Toolkit.getToolkit().firePulse();
        // verify it has correct owner
        cam.setNearClip(40);
        Toolkit.getToolkit().firePulse();
        assertEquals(40, peer.getCamera().getNearClip(), 0.00001);

        NGCamera oldCam = peer.getCamera();
        sub.setCamera(new ParallelCamera());
        Toolkit.getToolkit().firePulse();
        // verify owner was removed
        cam.setNearClip(50);
        Toolkit.getToolkit().firePulse();
        assertEquals(40, oldCam.getNearClip(), 0.00001);
        assertEquals(0.1, peer.getCamera().getNearClip(), 0.00001);
    }

    @Test
    public void testDefaultCameraUpdatesPG() {
        SubScene sub = new SubScene(new Group(), 100, 100);
        Scene scene = new Scene(new Group(sub), 300, 200);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        Toolkit.getToolkit().firePulse();
        Camera cam = SubSceneShim.getEffectiveCamera(sub);

        cam.setNearClip(20);
        Toolkit.getToolkit().firePulse();
        NGSubScene peer = NodeHelper.getPeer(sub);
        assertEquals(20, peer.getCamera().getNearClip(), 0.00001);
    }

    @Test
    public void subScenesCannotShareCamera() {
        assertThrows(IllegalArgumentException.class, () -> {
            SubScene sub = new SubScene(new Group(), 100, 100);
            SubScene sub2 = new SubScene(new Group(), 100, 100);
            Scene scene = new Scene(new Group(sub, sub2), 300, 200);
            Camera cam = new ParallelCamera();
            sub.setCamera(cam);
            sub2.setCamera(cam);
        });
    }

    @Test
    public void sceneAndSubSceneCannotShareCamera() {
        assertThrows(IllegalArgumentException.class, () -> {
            SubScene sub = new SubScene(new Group(), 100, 100);
            Scene scene = new Scene(new Group(sub), 300, 200);
            Camera cam = new ParallelCamera();
            scene.setCamera(cam);
            sub.setCamera(cam);
        });
    }

    @Test
    public void shouldBeAbleToSetCameraTwiceToSubScene() {
        SubScene sub = new SubScene(new Group(), 100, 100);
        Scene scene = new Scene(new Group(sub), 300, 200);
        Camera cam = new ParallelCamera();
        try {
            sub.setCamera(cam);
            sub.setCamera(cam);
        } catch (IllegalArgumentException e) {
            fail("It didn't allow to 'share' camera with myslef");
        }
    }

    @Test
    public void testLayout() {
        Pane pane = new Pane();
        pane.setPrefWidth(100);

        SubScene sub = new SubScene(new Group(pane), 500, 500);
        Scene scene = new Scene(new Group(sub), 600, 600);

        scene.getRoot().layout();
        assertEquals(100, pane.getWidth(), 1e-10);

        pane.setPrefWidth(110);
        scene.getRoot().layout();
        assertEquals(110, pane.getWidth(), 1e-10);


    }
}
