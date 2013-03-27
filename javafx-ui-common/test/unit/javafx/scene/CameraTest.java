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

import com.sun.javafx.geom.transform.Affine3D;
import javafx.scene.transform.NonInvertibleTransformException;
import static org.junit.Assert.*;
import org.junit.Test;

public class CameraTest {

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
}
