/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGParallelCamera;
import com.sun.javafx.sg.prism.NGPerspectiveCamera;
import com.sun.javafx.test.TransformHelper;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.stage.Stage;
import org.junit.Test;

import static org.junit.Assert.*;

public class CameraTest {

    static final GeneralTransform3D DEFAULT_PROJVIEW_TX;
    static {
        GeneralTransform3D expected = new GeneralTransform3D();
        expected.perspective(true, Math.toRadians(30), 1.5, 0.1, 100);
        final double tanOfHalfFOV = Math.tan(Math.toRadians(30) / 2.0);
        Affine3D view = new Affine3D();
        final double scale = 2.0 * tanOfHalfFOV / 200;
        view.setToTranslation(-tanOfHalfFOV * 1.5, tanOfHalfFOV, 0.0);
        view.translate(0, 0, -1);
        view.rotate(Math.PI, 1, 0, 0);
        view.scale(scale, scale, scale);
        expected.mul(view);
        DEFAULT_PROJVIEW_TX = expected;
    }

    /**
     * Test of setNearClip method, of class Camera.
     */
    @Test
    public void testSetNearClip() {
        Camera camera = new PerspectiveCamera();
        camera.setTranslateZ(-10);
        camera.setNearClip(10);

        assertEquals(10, camera.getNearClip(), 1e-3);
        assertEquals(90, camera.getFarClipInScene(), 1e-3);
        assertEquals(0, camera.getNearClipInScene(), 1e-3);
    }

    /**
     * Test of getNearClipInScene method, of class Camera.
     */
    @Test
    public void testGetNearClipInScene() {
        Camera camera = new PerspectiveCamera();
        camera.setTranslateZ(-10);

        assertEquals(-9.9, camera.getNearClipInScene(), 1e-3);
    }

    /**
     * Test of setFarClip method, of class Camera.
     */
    @Test
    public void testSetFarClip() {
        Camera camera = new PerspectiveCamera();
        camera.setTranslateZ(-10);
        camera.setFarClip(200);

        assertEquals(200, camera.getFarClip(), 1e-3);
        assertEquals(-9.9, camera.getNearClipInScene(), 1e-3);
        assertEquals(190, camera.getFarClipInScene(), 1e-3);
    }

    /**
     * Test of getFarClipInScene method, of class Camera.
     */
    @Test
    public void testGetFarClipInScene() {
        Camera camera = new PerspectiveCamera();
        camera.setTranslateZ(-10);
        camera.setFarClip(60);

        assertEquals(50, camera.getFarClipInScene(), 1e-3);
    }

    @Test
    public void testLocalToSceneTxChange() {
        Camera camera = new PerspectiveCamera();
        camera.setTranslateZ(-10);
        assertEquals(0.1, camera.getNearClip(), 1e-3);
        assertEquals(100, camera.getFarClip(), 1e-3);
        assertEquals(-9.9, camera.getNearClipInScene(), 1e-3);
        assertEquals(90, camera.getFarClipInScene(), 1e-3);

        camera.setTranslateZ(100);
        assertEquals(0.1, camera.getNearClip(), 1e-3);
        assertEquals(100, camera.getFarClip(), 1e-3);
        assertEquals(100.1, camera.getNearClipInScene(), 1e-3);
        assertEquals(200, camera.getFarClipInScene(), 1e-3);
    }

    /**
     * Test of getSceneToLocalTransform method, of class Camera.
     */
    @Test
    public void testGetSceneToLocalTransform() {
        Camera camera = new PerspectiveCamera();
        new Scene(new Group(camera));
        camera.setTranslateX(300);
        camera.getParent().setTranslateY(100);
        Affine3D expected = new Affine3D();
        try {
            camera.getLocalToSceneTransform().createInverse().impl_apply(expected);
        } catch (NonInvertibleTransformException ex) {
            fail("NonInvertibleTransformException when compute sceneToLocalTx.");
        }
        assertEquals(expected, camera.getSceneToLocalTransform());

        camera.setTranslateZ(-10);
        camera.setScaleX(10);
        expected.setToIdentity();
        try {
            camera.getLocalToSceneTransform().createInverse().impl_apply(expected);
        } catch (NonInvertibleTransformException ex) {
            fail("NonInvertibleTransformException when compute sceneToLocalTx.");
        }
        assertEquals(expected, camera.getSceneToLocalTransform());
    }

    /**
     * Test of getSceneToLocalTransform method when camera is not in scene.
     */
    @Test
    public void testGetSceneToLocalTransformWhenNotInScene() {
        Camera camera = new PerspectiveCamera();
        Affine3D expected = new Affine3D();
        assertEquals(expected, camera.getSceneToLocalTransform());

        try {
            camera.getLocalToSceneTransform().createInverse().impl_apply(expected);
        } catch (NonInvertibleTransformException ex) {
            fail("NonInvertibleTransformException when compute sceneToLocalTx.");
        }
        assertEquals(expected, camera.getSceneToLocalTransform());

        camera.setTranslateZ(-10);
        camera.setScaleX(10);
        expected.setToIdentity();
        try {
            camera.getLocalToSceneTransform().createInverse().impl_apply(expected);
        } catch (NonInvertibleTransformException ex) {
            fail("NonInvertibleTransformException when compute sceneToLocalTx.");
        }
        assertEquals(expected, camera.getSceneToLocalTransform());
    }

    @Test
    public void testViewSize() {
        final Scene scene = new Scene(new Group(), 300, 200);
        Camera camera = new PerspectiveCamera();
        scene.setCamera(camera);
        assertEquals(300.0, camera.getViewWidth(), 1.0e-20);
        assertEquals(200.0, camera.getViewHeight(), 1.0e-20);
    }

    @Test
    public void testDefaultCamera() {
        final Scene scene = new Scene(new Group(), 300, 200);
        Camera camera = scene.getEffectiveCamera();

        assertTrue(camera instanceof ParallelCamera);
        assertEquals(300.0, camera.getViewWidth(), 1.0e-20);
        assertEquals(200.0, camera.getViewHeight(), 1.0e-20);
    }

    @Test
    public void testParallelProjViewTx() {
        final Scene scene = new Scene(new Group(), 300, 200);
        Camera camera = new ParallelCamera();
        scene.setCamera(camera);

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.ortho(0.0, 300, 200, 0.0, -150, 150);

        TransformHelper.assertMatrix(camera.getProjViewTransform(), expected);
    }

    @Test
    public void testParallelProjViewTxWithMovedCamera() {
        final Scene scene = new Scene(new Group(), 300, 200);
        Camera camera = new ParallelCamera();
        scene.setCamera(camera);
        scene.getRoot().getChildren().add(camera);
        scene.getRoot().setTranslateX(50);
        camera.setTranslateY(60);

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.ortho(0.0, 300, 200, 0.0, -150, 150);
        expected.mul(Affine3D.getTranslateInstance(-50, -60));

        TransformHelper.assertMatrix(camera.getProjViewTransform(), expected);
    }

    @Test
    public void testParallelProjViewTxWithMovedCameraNotInScene() {
        final Scene scene = new Scene(new Group(), 300, 200);
        Camera camera = new ParallelCamera();
        scene.setCamera(camera);
        scene.getRoot().setTranslateX(50);
        camera.setTranslateX(50);
        camera.setTranslateY(60);

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.ortho(0.0, 300, 200, 0.0, -150, 150);
        expected.mul(Affine3D.getTranslateInstance(-50, -60));

        TransformHelper.assertMatrix(camera.getProjViewTransform(), expected);
    }

    @Test
    public void testParallelProjViewTxWithMovedCameraNotInScene2() {
        final Scene scene = new Scene(new Group(), 300, 200);
        Camera camera = new ParallelCamera();
        scene.setCamera(camera);
        scene.getRoot().setTranslateX(50);
        new Group(camera);
        camera.getParent().setTranslateX(50);
        camera.setTranslateY(60);

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.ortho(0.0, 300, 200, 0.0, -150, 150);
        expected.mul(Affine3D.getTranslateInstance(-50, -60));

        TransformHelper.assertMatrix(camera.getProjViewTransform(), expected);
    }

    @Test
    public void testPerspectiveProjViewTx() {
        final Scene scene = new Scene(new Group(), 300, 200);
        PerspectiveCamera camera = new PerspectiveCamera();
        scene.setCamera(camera);

        TransformHelper.assertMatrix(camera.getProjViewTransform(), DEFAULT_PROJVIEW_TX);
    }

    @Test
    public void testPerspectiveProjViewTxWithModifiedParams() {
        final Scene scene = new Scene(new Group(), 300, 200);
        PerspectiveCamera camera = new PerspectiveCamera();
        camera.setVerticalFieldOfView(false);
        camera.setFieldOfView(40);
        camera.setNearClip(1);
        camera.setFarClip(200);
        scene.setCamera(camera);

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.perspective(false, Math.toRadians(40), 1.5, 1.0, 200);

        final double tanOfHalfFOV = Math.tan(Math.toRadians(40) / 2.0);

        Affine3D view = new Affine3D();
        final double scale = 2.0 * tanOfHalfFOV / 300;

        view.setToTranslation(-tanOfHalfFOV, tanOfHalfFOV / 1.5, 0.0);
        view.translate(0, 0, -1);
        view.rotate(Math.PI, 1, 0, 0);
        view.scale(scale, scale, scale);

        expected.mul(view);

        TransformHelper.assertMatrix(camera.getProjViewTransform(), expected);
    }

    @Test
    public void testPerspectiveProjViewTxWithMovedCamera() {
        final Scene scene = new Scene(new Group(), 300, 200);
        PerspectiveCamera camera = new PerspectiveCamera();
        scene.setCamera(camera);
        scene.getRoot().getChildren().add(camera);
        scene.getRoot().setTranslateX(50);
        camera.setTranslateY(60);

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.perspective(true, Math.toRadians(30), 1.5, 0.1, 100);

        final double tanOfHalfFOV = Math.tan(Math.toRadians(30) / 2.0);

        Affine3D view = new Affine3D();
        final double scale = 2.0 * tanOfHalfFOV / 200;

        view.setToTranslation(-tanOfHalfFOV * 1.5, tanOfHalfFOV, 0.0);
        view.translate(0, 0, -1);
        view.rotate(Math.PI, 1, 0, 0);
        view.scale(scale, scale, scale);

        expected.mul(view);
        expected.mul(Affine3D.getTranslateInstance(-50, -60));

        TransformHelper.assertMatrix(camera.getProjViewTransform(), expected);
    }

    @Test
    public void testPerspectiveProjViewTxWithMovedCameraNotInScene() {
        final Scene scene = new Scene(new Group(), 300, 200);
        PerspectiveCamera camera = new PerspectiveCamera();
        scene.setCamera(camera);
        scene.getRoot().setTranslateX(50);
        camera.setTranslateX(50);
        camera.setTranslateY(60);

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.perspective(true, Math.toRadians(30), 1.5, 0.1, 100);

        final double tanOfHalfFOV = Math.tan(Math.toRadians(30) / 2.0);

        Affine3D view = new Affine3D();
        final double scale = 2.0 * tanOfHalfFOV / 200;

        view.setToTranslation(-tanOfHalfFOV * 1.5, tanOfHalfFOV, 0.0);
        view.translate(0, 0, -1);
        view.rotate(Math.PI, 1, 0, 0);
        view.scale(scale, scale, scale);

        expected.mul(view);
        expected.mul(Affine3D.getTranslateInstance(-50, -60));

        TransformHelper.assertMatrix(camera.getProjViewTransform(), expected);
    }

    @Test
    public void testPerspectiveProjViewTxWithMovedCameraNotInScene2() {
        final Scene scene = new Scene(new Group(), 300, 200);
        PerspectiveCamera camera = new PerspectiveCamera();
        scene.setCamera(camera);
        scene.getRoot().setTranslateX(50);
        new Group(camera);
        camera.getParent().setTranslateX(50);
        camera.setTranslateY(60);

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.perspective(true, Math.toRadians(30), 1.5, 0.1, 100);

        final double tanOfHalfFOV = Math.tan(Math.toRadians(30) / 2.0);

        Affine3D view = new Affine3D();
        final double scale = 2.0 * tanOfHalfFOV / 200;

        view.setToTranslation(-tanOfHalfFOV * 1.5, tanOfHalfFOV, 0.0);
        view.translate(0, 0, -1);
        view.rotate(Math.PI, 1, 0, 0);
        view.scale(scale, scale, scale);

        expected.mul(view);
        expected.mul(Affine3D.getTranslateInstance(-50, -60));

        TransformHelper.assertMatrix(camera.getProjViewTransform(), expected);
    }

    @Test
    public void testPerspectiveProjViewTxWithFixedEye() {
        final Scene scene = new Scene(new Group(), 300, 200);
        PerspectiveCamera camera = new PerspectiveCamera(true);
        scene.setCamera(camera);

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.perspective(true, Math.toRadians(30), 1.5, 0.1, 100);

        Affine3D view = new Affine3D();
        view.rotate(Math.PI, 1, 0, 0);

        expected.mul(view);

        TransformHelper.assertMatrix(camera.getProjViewTransform(), expected);
    }

    @Test
    public void testPerspectiveProjViewTxWithFixedEyeAndModifiedParams() {
        final Scene scene = new Scene(new Group(), 300, 200);
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setVerticalFieldOfView(false);
        camera.setFieldOfView(40);
        camera.setNearClip(1);
        camera.setFarClip(200);
        scene.setCamera(camera);

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.perspective(false, Math.toRadians(40), 1.5, 1.0, 200);

        Affine3D view = new Affine3D();
        view.rotate(Math.PI, 1, 0, 0);

        expected.mul(view);

        TransformHelper.assertMatrix(camera.getProjViewTransform(), expected);
    }

    @Test
    public void testPerspectiveProjViewTxWithFixedEyeAndMovedCamera() {
        final Scene scene = new Scene(new Group(), 300, 200);
        PerspectiveCamera camera = new PerspectiveCamera(true);
        scene.setCamera(camera);
        scene.getRoot().getChildren().add(camera);
        scene.getRoot().setTranslateX(50);
        camera.setTranslateY(60);

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.perspective(true, Math.toRadians(30), 1.5, 0.1, 100);

        Affine3D view = new Affine3D();
        view.rotate(Math.PI, 1, 0, 0);

        expected.mul(view);
        expected.mul(Affine3D.getTranslateInstance(-50, -60));

        TransformHelper.assertMatrix(camera.getProjViewTransform(), expected);
    }

    @Test
    public void testPerspectiveProjViewTxWithFixedEyeAndMovedCameraNotInScene() {
        final Scene scene = new Scene(new Group(), 300, 200);
        PerspectiveCamera camera = new PerspectiveCamera(true);
        scene.setCamera(camera);
        scene.getRoot().setTranslateX(50);
        camera.setTranslateX(50);
        camera.setTranslateY(60);

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.perspective(true, Math.toRadians(30), 1.5, 0.1, 100);

        Affine3D view = new Affine3D();
        view.rotate(Math.PI, 1, 0, 0);

        expected.mul(view);
        expected.mul(Affine3D.getTranslateInstance(-50, -60));

        TransformHelper.assertMatrix(camera.getProjViewTransform(), expected);
    }

    @Test
    public void testPerspectiveProjViewTxWithFixedEyeAndMovedCameraNotInScene2() {
        final Scene scene = new Scene(new Group(), 300, 200);
        PerspectiveCamera camera = new PerspectiveCamera(true);
        scene.setCamera(camera);
        scene.getRoot().setTranslateX(50);
        new Group(camera);
        camera.getParent().setTranslateX(50);
        camera.setTranslateY(60);

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.perspective(true, Math.toRadians(30), 1.5, 0.1, 100);

        Affine3D view = new Affine3D();
        view.rotate(Math.PI, 1, 0, 0);

        expected.mul(view);
        expected.mul(Affine3D.getTranslateInstance(-50, -60));

        TransformHelper.assertMatrix(camera.getProjViewTransform(), expected);
    }

    @Test
    public void testParallelCameraPosition() {
        Scene scene = new Scene(new Group(), 300, 200);
        Camera cam = new ParallelCamera();
        scene.setCamera(cam);
        Vec3d v = cam.computePosition(null);
        assertEquals(150.0, v.x, 0.000001);
        assertEquals(100.0, v.y, 0.000001);
        assertEquals(-373.205080, v.z, 0.000001);
    }

    @Test
    public void testPerspectiveCameraPositionWithFixedEye() {
        Scene scene = new Scene(new Group(), 300, 200);
        Camera cam = new PerspectiveCamera(true);
        scene.setCamera(cam);
        Vec3d v = cam.computePosition(null);
        assertEquals(0.0, v.x, 0.000001);
        assertEquals(0.0, v.y, 0.000001);
        assertEquals(0.0, v.z, 0.000001);
    }

    @Test
    public void testPerspectiveCameraPosition() {
        Scene scene = new Scene(new Group(), 300, 200);
        Camera cam = new PerspectiveCamera();
        scene.setCamera(cam);
        Vec3d v = cam.computePosition(null);
        assertEquals(150.0, v.x, 0.000001);
        assertEquals(100.0, v.y, 0.000001);
        assertEquals(-373.205080, v.z, 0.000001);
    }

    @Test
    public void perspectiveCameraShouldSyncWhenAssignedToScene() {
        final Scene scene = new Scene(new Group(), 300, 200);
        PerspectiveCamera camera = new StubPerspectiveCamera();
        scene.setCamera(camera);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        Toolkit.getToolkit().firePulse();

        StubNGPerspectiveCamera pc = camera.impl_getPeer();

        assertEquals(300, pc.getViewWidth(), 0.00001);
        assertEquals(200, pc.getViewHeight(), 0.00001);

        TransformHelper.assertMatrix(pc.getProjViewTx(), DEFAULT_PROJVIEW_TX);
        assertTrue(pc.isVerticalFieldOfView());
        assertEquals(30, pc.getFieldOfView(), 0.00001);
        assertEquals(150.0, pc.getPosition().x, 0.000001);
        assertEquals(100.0, pc.getPosition().y, 0.000001);
        assertEquals(-373.205080, pc.getPosition().z, 0.000001);
    }

    @Test
    public void perspectiveCameraShouldSyncWhenAssignedToSubScene() {
        SubScene sub = new SubScene(new Group(), 300, 200);
        final Scene scene = new Scene(new Group(sub), 600, 300);
        PerspectiveCamera camera = new StubPerspectiveCamera();
        sub.setCamera(camera);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        Toolkit.getToolkit().firePulse();

        StubNGPerspectiveCamera pc = camera.impl_getPeer();

        assertEquals(300, pc.getViewWidth(), 0.00001);
        assertEquals(200, pc.getViewHeight(), 0.00001);

        TransformHelper.assertMatrix(pc.getProjViewTx(), DEFAULT_PROJVIEW_TX);
        assertTrue(pc.isVerticalFieldOfView());
        assertEquals(30, pc.getFieldOfView(), 0.00001);
        assertEquals(150.0, pc.getPosition().x, 0.000001);
        assertEquals(100.0, pc.getPosition().y, 0.000001);
        assertEquals(-373.205080, pc.getPosition().z, 0.000001);
    }

    @Test
    public void parallelCameraShouldSyncWhenAssignedToScene() {
        final Scene scene = new Scene(new Group(), 300, 200);
        ParallelCamera camera = new StubParallelCamera();
        scene.setCamera(camera);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        Toolkit.getToolkit().firePulse();

        StubNGParallelCamera pc = camera.impl_getPeer();

        assertEquals(300, pc.getViewWidth(), 0.00001);
        assertEquals(200, pc.getViewHeight(), 0.00001);

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.ortho(0.0, 300, 200, 0.0, -150, 150);

        TransformHelper.assertMatrix(pc.getProjViewTx(), expected);
        assertEquals(150.0, pc.getPosition().x, 0.000001);
        assertEquals(100.0, pc.getPosition().y, 0.000001);
        assertEquals(-373.205080, pc.getPosition().z, 0.000001);
    }

    @Test
    public void parallelCameraShouldSyncWhenAssignedToSubScene() {
        SubScene sub = new SubScene(new Group(), 300, 200);
        final Scene scene = new Scene(new Group(sub), 600, 300);
        ParallelCamera camera = new StubParallelCamera();
        sub.setCamera(camera);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        Toolkit.getToolkit().firePulse();

        StubNGParallelCamera pc = camera.impl_getPeer();

        assertEquals(300, pc.getViewWidth(), 0.00001);
        assertEquals(200, pc.getViewHeight(), 0.00001);

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.ortho(0.0, 300, 200, 0.0, -150, 150);

        TransformHelper.assertMatrix(pc.getProjViewTx(), expected);
        assertEquals(150.0, pc.getPosition().x, 0.000001);
        assertEquals(100.0, pc.getPosition().y, 0.000001);
        assertEquals(-373.205080, pc.getPosition().z, 0.000001);
    }

    @Test
    public void sceneDefaultCameraShouldSyncInTheBeginning() {
        final Scene scene = new Scene(new Group(), 300, 200);
        ParallelCamera camera = new StubParallelCamera();
        scene.setCamera(camera);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        Toolkit.getToolkit().firePulse();

        StubNGParallelCamera pc = scene.getEffectiveCamera().impl_getPeer();

        assertEquals(300, pc.getViewWidth(), 0.00001);
        assertEquals(200, pc.getViewHeight(), 0.00001);

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.ortho(0.0, 300, 200, 0.0, -150, 150);

        TransformHelper.assertMatrix(pc.getProjViewTx(), expected);
        assertEquals(150.0, pc.getPosition().x, 0.000001);
        assertEquals(100.0, pc.getPosition().y, 0.000001);
        assertEquals(-373.205080, pc.getPosition().z, 0.000001);
    }

    @Test
    public void subSceneDefaultCameraShouldSyncInTheBeginning() {
        SubScene sub = new SubScene(new Group(), 300, 200);
        final Scene scene = new Scene(new Group(sub), 600, 300);
        ParallelCamera camera = new StubParallelCamera();
        sub.setCamera(camera);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        Toolkit.getToolkit().firePulse();

        StubNGParallelCamera pc = sub.getEffectiveCamera().impl_getPeer();

        assertEquals(300, pc.getViewWidth(), 0.00001);
        assertEquals(200, pc.getViewHeight(), 0.00001);

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.ortho(0.0, 300, 200, 0.0, -150, 150);

        TransformHelper.assertMatrix(pc.getProjViewTx(), expected);
        assertEquals(150.0, pc.getPosition().x, 0.000001);
        assertEquals(100.0, pc.getPosition().y, 0.000001);
        assertEquals(-373.205080, pc.getPosition().z, 0.000001);
    }

    @Test
    public void resizeShouldTriggerSync() {
        SubScene sub = new SubScene(new Group(), 400, 300);
        final Scene scene = new Scene(new Group(sub), 600, 300);
        PerspectiveCamera camera = new StubPerspectiveCamera();
        sub.setCamera(camera);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        Toolkit.getToolkit().firePulse();

        sub.setWidth(300);
        sub.setHeight(200);
        Toolkit.getToolkit().firePulse();

        StubNGPerspectiveCamera pc = camera.impl_getPeer();

        assertEquals(300, pc.getViewWidth(), 0.00001);
        assertEquals(200, pc.getViewHeight(), 0.00001);

        TransformHelper.assertMatrix(pc.getProjViewTx(), DEFAULT_PROJVIEW_TX);
        assertEquals(150.0, pc.getPosition().x, 0.000001);
        assertEquals(100.0, pc.getPosition().y, 0.000001);
        assertEquals(-373.205080, pc.getPosition().z, 0.000001);
    }

    @Test
    public void clipChangeShouldTriggerSync() {
        final Scene scene = new Scene(new Group(), 300, 200);
        PerspectiveCamera camera = new StubPerspectiveCamera();
        scene.setCamera(camera);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        Toolkit.getToolkit().firePulse();

        camera.setFarClip(250.0);
        Toolkit.getToolkit().firePulse();

        StubNGPerspectiveCamera pc = camera.impl_getPeer();

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.perspective(true, Math.toRadians(30), 1.5, 0.1, 250);
        final double tanOfHalfFOV = Math.tan(Math.toRadians(30) / 2.0);
        Affine3D view = new Affine3D();
        final double scale = 2.0 * tanOfHalfFOV / 200;
        view.setToTranslation(-tanOfHalfFOV * 1.5, tanOfHalfFOV, 0.0);
        view.translate(0, 0, -1);
        view.rotate(Math.PI, 1, 0, 0);
        view.scale(scale, scale, scale);
        expected.mul(view);

        TransformHelper.assertMatrix(pc.getProjViewTx(), expected);
    }

    @Test
    public void fieldOfViewChangeShouldTriggerSync() {
        final Scene scene = new Scene(new Group(), 300, 200);
        PerspectiveCamera camera = new StubPerspectiveCamera();
        scene.setCamera(camera);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        Toolkit.getToolkit().firePulse();

        camera.setFieldOfView(45.0);
        Toolkit.getToolkit().firePulse();

        StubNGPerspectiveCamera pc = camera.impl_getPeer();

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.perspective(true, Math.toRadians(45), 1.5, 0.1, 100);
        final double tanOfHalfFOV = Math.tan(Math.toRadians(45) / 2.0);
        Affine3D view = new Affine3D();
        final double scale = 2.0 * tanOfHalfFOV / 200;
        view.setToTranslation(-tanOfHalfFOV * 1.5, tanOfHalfFOV, 0.0);
        view.translate(0, 0, -1);
        view.rotate(Math.PI, 1, 0, 0);
        view.scale(scale, scale, scale);
        expected.mul(view);

        TransformHelper.assertMatrix(pc.getProjViewTx(), expected);
        assertEquals(150.0, pc.getPosition().x, 0.000001);
        assertEquals(100.0, pc.getPosition().y, 0.000001);
        assertEquals(-241.421356, pc.getPosition().z, 0.000001);
    }

    @Test
    public void verticalFOVChangeShouldTriggerSync() {
        final Scene scene = new Scene(new Group(), 300, 200);
        PerspectiveCamera camera = new StubPerspectiveCamera();
        scene.setCamera(camera);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        Toolkit.getToolkit().firePulse();

        camera.setVerticalFieldOfView(false);
        Toolkit.getToolkit().firePulse();

        StubNGPerspectiveCamera pc = camera.impl_getPeer();

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.perspective(false, Math.toRadians(30), 1.5, 0.1, 100);
        final double tanOfHalfFOV = Math.tan(Math.toRadians(30) / 2.0);
        Affine3D view = new Affine3D();
        final double scale = 2.0 * tanOfHalfFOV / 300;
        view.setToTranslation(-tanOfHalfFOV, tanOfHalfFOV / 1.5, 0.0);
        view.translate(0, 0, -1);
        view.rotate(Math.PI, 1, 0, 0);
        view.scale(scale, scale, scale);
        expected.mul(view);

        TransformHelper.assertMatrix(pc.getProjViewTx(), expected);
        assertEquals(150.0, pc.getPosition().x, 0.000001);
        assertEquals(100.0, pc.getPosition().y, 0.000001);
        assertEquals(-559.80762, pc.getPosition().z, 0.00001);
    }

    @Test
    public void localToSceneChangeShouldTriggerSync() {
        PerspectiveCamera camera = new StubPerspectiveCamera();
        final Scene scene = new Scene(new Group(new Group(camera)), 300, 200);
        scene.setCamera(camera);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        Toolkit.getToolkit().firePulse();

        camera.getParent().setTranslateX(200);
        Toolkit.getToolkit().firePulse();

        StubNGPerspectiveCamera pc = camera.impl_getPeer();

        GeneralTransform3D expected = new GeneralTransform3D();
        expected.set(DEFAULT_PROJVIEW_TX);
        expected.mul(Affine3D.getTranslateInstance(-200, 0));

        TransformHelper.assertMatrix(pc.getProjViewTx(), expected);
        assertEquals(350.0, pc.getPosition().x, 0.000001);
        assertEquals(100.0, pc.getPosition().y, 0.000001);
        assertEquals(-373.205080, pc.getPosition().z, 0.000001);
        TransformHelper.assertMatrix(pc.getWorldTransform(), Affine3D.getTranslateInstance(200, 0));
    }

    private class StubPerspectiveCamera extends PerspectiveCamera {
        @Override
        protected NGNode impl_createPeer() {
            return new StubNGPerspectiveCamera(isFixedEyeAtCameraZero());
        }
    }

    private class StubNGPerspectiveCamera extends NGPerspectiveCamera {
        double viewWidth, viewHeight;
        Vec3d position;
        GeneralTransform3D projViewTx;
        Affine3D localToWorldTx;
        float fieldOfView;
        boolean verticalFieldOfView;

        public StubNGPerspectiveCamera(boolean fixedEyeAtCameraZero) {
            super(fixedEyeAtCameraZero);
        }

        @Override public void setViewWidth(double viewWidth) { this.viewWidth = viewWidth; }
        public double getViewWidth() { return viewWidth; }

        @Override public void setViewHeight(double viewHeight) { this.viewHeight = viewHeight; }
        public double getViewHeight() { return viewHeight; }

        @Override public void setPosition(Vec3d position) { this.position = position; }
        public Vec3d getPosition() { return position; }

        @Override public void setWorldTransform(Affine3D localToWorldTx) { this.localToWorldTx = localToWorldTx; }
        public Affine3D getWorldTransform() { return localToWorldTx; }

        @Override public void setProjViewTransform(GeneralTransform3D projViewTx) { this.projViewTx = projViewTx; }
        public GeneralTransform3D getProjViewTx() { return projViewTx; }

        @Override public void setFieldOfView(float fieldOfView) { this.fieldOfView = fieldOfView; }
        public double getFieldOfView() { return fieldOfView; }

        @Override public void setVerticalFieldOfView(boolean verticalFieldOfView) { this.verticalFieldOfView = verticalFieldOfView; }
        public boolean isVerticalFieldOfView() { return this.verticalFieldOfView; }
    }

    private class StubParallelCamera extends ParallelCamera {
        @Override
        protected NGNode impl_createPeer() {
            return new StubNGParallelCamera();
        }
    }

    private class StubNGParallelCamera extends NGParallelCamera {
        double viewWidth, viewHeight;
        Vec3d position;
        GeneralTransform3D projViewTx;
        Affine3D localToWorldTx;

        @Override public void setViewWidth(double viewWidth) { this.viewWidth = viewWidth; }
        public double getViewWidth() { return viewWidth; }

        @Override public void setViewHeight(double viewHeight) { this.viewHeight = viewHeight; }
        public double getViewHeight() { return viewHeight; }

        @Override public void setPosition(Vec3d position) { this.position = position; }
        public Vec3d getPosition() { return position; }

        @Override public void setWorldTransform(Affine3D localToWorldTx) { this.localToWorldTx = localToWorldTx; }
        public Affine3D getWorldTransform() { return localToWorldTx; }

        @Override public void setProjViewTransform(GeneralTransform3D projViewTx) { this.projViewTx = projViewTx; }
        public GeneralTransform3D getProjViewTx() { return projViewTx; }
    }
}
