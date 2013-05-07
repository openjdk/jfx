/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.PickRay;
import com.sun.javafx.test.MouseEventGenerator;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import static org.junit.Assert.*;
import org.junit.Test;

public class Mouse3DTest {

    private static final int NOFACE = PickResult.FACE_UNDEFINED;
    private static final double PERSPECTIVE_CAMERA_X = 500;
    private static final double PERSPECTIVE_CAMERA_Y = 400;
    private static final double PERSPECTIVE_CAMERA_Z = -1492.820323027551;

    private EventHolder<MouseEvent> me = new EventHolder<MouseEvent>();
    private EventHolder<MouseEvent> pme = new EventHolder<MouseEvent>();
    private EventHolder<MouseEvent> sme = new EventHolder<MouseEvent>();

    /***************** pick ray ********************/
    // We use moving camera for the picking tests, so test pick ray sanity first

    @Test
    public void shouldComputeCorrectPerspectivePickRay() {
        Camera cam = new PerspectiveCamera();
        scene(group(), cam, true);
        cam.impl_updatePG();
        PickRay pickRay = cam.computePickRay(10, 20, null);
        assertEquals(PERSPECTIVE_CAMERA_X, pickRay.getOriginNoClone().x, 0.00001);
        assertEquals(PERSPECTIVE_CAMERA_Y, pickRay.getOriginNoClone().y, 0.00001);
        assertEquals(PERSPECTIVE_CAMERA_Z, pickRay.getOriginNoClone().z, 0.00001);
        assertEquals(10 - PERSPECTIVE_CAMERA_X, pickRay.getDirectionNoClone().x, 0.00001);
        assertEquals(20 - PERSPECTIVE_CAMERA_Y, pickRay.getDirectionNoClone().y, 0.00001);
        assertEquals(-PERSPECTIVE_CAMERA_Z, pickRay.getDirectionNoClone().z, 0.00001);
    }

    @Test
    public void shouldComputeCorrectPerspectivePickRayWithFixedEye() {
        Camera cam = new PerspectiveCamera(true);
        scene(group(), cam, true);
        cam.impl_updatePG();
        PickRay pickRay = cam.computePickRay(10, 20, null);
        assertEquals(0.0, pickRay.getOriginNoClone().x, 0.00001);
        assertEquals(0.0, pickRay.getOriginNoClone().y, 0.00001);
        assertEquals(-1.0, pickRay.getOriginNoClone().z, 0.00001);
        assertEquals(10 - PERSPECTIVE_CAMERA_X, pickRay.getDirectionNoClone().x, 0.00001);
        assertEquals(20 - PERSPECTIVE_CAMERA_Y, pickRay.getDirectionNoClone().y, 0.00001);
        assertEquals(-PERSPECTIVE_CAMERA_Z, pickRay.getDirectionNoClone().z, 0.00001);
    }

    @Test
    public void shouldComputeCorrectMovedPerspectivePickRay() {
        Camera cam = new PerspectiveCamera();
        scene(group(cam), cam, true);
        cam.setTranslateX(50);
        cam.getParent().setTranslateY(30);
        cam.impl_updatePG();
        PickRay pickRay = cam.computePickRay(10, 20, null);
        assertEquals(50 + PERSPECTIVE_CAMERA_X, pickRay.getOriginNoClone().x, 0.00001);
        assertEquals(30 + PERSPECTIVE_CAMERA_Y, pickRay.getOriginNoClone().y, 0.00001);
        assertEquals(PERSPECTIVE_CAMERA_Z, pickRay.getOriginNoClone().z, 0.00001);
        assertEquals(10 - PERSPECTIVE_CAMERA_X, pickRay.getDirectionNoClone().x, 0.00001);
        assertEquals(20 - PERSPECTIVE_CAMERA_Y, pickRay.getDirectionNoClone().y, 0.00001);
        assertEquals(-PERSPECTIVE_CAMERA_Z, pickRay.getDirectionNoClone().z, 0.00001);
    }

    @Test
    public void shouldComputeCorrectParallelPickRay() {
        Camera cam = new ParallelCamera();
        scene(group(), cam, true);
        PickRay pickRay = cam.computePickRay(10, 20, null);
        assertEquals(10.0, pickRay.getOriginNoClone().x, 0.00001);
        assertEquals(20.0, pickRay.getOriginNoClone().y, 0.00001);
        assertEquals(-1, pickRay.getOriginNoClone().z, 0.1);
        assertEquals(0.0, pickRay.getDirectionNoClone().x, 0.00001);
        assertEquals(0.0, pickRay.getDirectionNoClone().y, 0.00001);
        assertEquals(1.0, pickRay.getDirectionNoClone().z, 0.1);
    }

    @Test
    public void shouldComputeCorrectMovedParallelPickRay() {
        Camera cam = new ParallelCamera();
        scene(group(cam), cam, true);
        cam.setTranslateX(50);
        cam.getParent().setTranslateY(30);
        cam.impl_updatePG();
        PickRay pickRay = cam.computePickRay(10, 20, null);
        assertEquals(60.0, pickRay.getOriginNoClone().x, 0.00001);
        assertEquals(50.0, pickRay.getOriginNoClone().y, 0.00001);
        assertEquals(-1, pickRay.getOriginNoClone().z, 0.1);
        assertEquals(0.0, pickRay.getDirectionNoClone().x, 0.00001);
        assertEquals(0.0, pickRay.getDirectionNoClone().y, 0.00001);
        assertEquals(1.0, pickRay.getDirectionNoClone().z, 0.1);
    }


    /*****************  BOX picking ********************/

    @Test
    public void shouldPickBoxFromFront() {
        Box b = box().handleMove(me);
        b.setCullFace(CullFace.BACK);
        Scene s = scene(group(b), perspective(), true);

        makeParallel(s, 10, 40);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 40, -200);
        assertPickResult(e.getPickResult(),
                b, point(10, 40, -200), 800, NOFACE, point(0.6, 0.7));
    }

    @Test
    public void shouldPickBoxInteriorFromFront() {
        Box b = box().handleMove(me);
        b.setCullFace(CullFace.FRONT);
        Scene s = scene(group(b), perspective(), true);

        makeParallel(s, 10, 40);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 40, 200);
        assertPickResult(e.getPickResult(),
                b, point(10, 40, 200), 1200, NOFACE, point(0.4, 0.7));
    }

    @Test
    public void shouldPickNotCulledBoxFromFront() {
        Box b = box().handleMove(me);
        b.setCullFace(CullFace.NONE);
        Scene s = scene(group(b), perspective(), true);

        makeParallel(s, 10, 40);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 40, -200);
        assertPickResult(e.getPickResult(),
                b, point(10, 40, -200), 800, NOFACE, point(0.6, 0.7));
    }

    @Test
    public void shouldPickBoxFromTop() {
        Node b = box().rotate('x', 90).handleMove(me);
        Scene s = scene(group(b), perspective(), true);

        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, -100, -20);
        assertPickResult(e.getPickResult(),
                b, point(10, -100, -20), 900, NOFACE, point(0.6, 0.55));
    }

    @Test
    public void shouldPickBoxFromBack() {
        Node b = box().rotate('x', 180).handleMove(me);
        Scene s = scene(group(b), perspective(), true);

        makeParallel(s, 10, 40);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, -40, 200);
        assertPickResult(e.getPickResult(),
                b, point(10, -40, 200), 800, NOFACE, point(0.4, 0.3));
    }

    @Test
    public void shouldPickBoxFromBottom() {
        Node b = box().rotate('x', -90).handleMove(me);
        Scene s = scene(group(b), perspective(), true);

        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 100, 20);
        assertPickResult(e.getPickResult(),
                b, point(10, 100, 20), 900, NOFACE, point(0.6, 0.55));
    }

    @Test
    public void shouldPickBoxFromLeft() {
        Node b = box().rotate('y', -90).handleMove(me);
        Scene s = scene(group(b), perspective(), true);

        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, -50, 20, -10);
        assertPickResult(e.getPickResult(),
                b, point(-50, 20, -10), 950, NOFACE, point(0.525, 0.6));
    }

    @Test
    public void shouldPickBoxFromRight() {
        Node b = box().rotate('y', 90).handleMove(me);
        Scene s = scene(group(b), perspective(), true);

        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 50, 20, 10);
        assertPickResult(e.getPickResult(),
                b, point(50, 20, 10), 950, NOFACE, point(0.525, 0.6));
    }

    @Test
    public void shouldPickBoxByParallelCameraFromFront() {
        MouseEventGenerator g = new MouseEventGenerator();
        Box b = box().handleMove(me);
        b.setCullFace(CullFace.BACK);
        Scene s = scene(group(b), parallel(), true);
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_MOVED, 10, 40));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 40, 10, 40, -200);
        assertPickResult(e.getPickResult(),
                b, point(10, 40, -200), Double.POSITIVE_INFINITY, NOFACE, point(0.6, 0.7));
    }

    @Test
    public void shouldPickBoxByParallelCameraFromBack() {
        MouseEventGenerator g = new MouseEventGenerator();
        Node b = box().rotate('x', 180).handleMove(me);
        Scene s = scene(group(b), parallel(), true);
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_MOVED, 10, 40));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 40, 10, -40, 200);
        assertPickResult(e.getPickResult(),
                b, point(10, -40, 200), Double.POSITIVE_INFINITY, NOFACE, point(0.4, 0.3));
    }

    @Test
    public void shouldNotPickBoxByParallelCameraFromFrontNextToIt() {
        MouseEventGenerator g = new MouseEventGenerator();
        Box b = box().handleMove(me);
        b.setCullFace(CullFace.BACK);
        Scene s = scene(group(b), parallel(), true);
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_MOVED, 500, 40));
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_MOVED, -500, 40));
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_MOVED, 10, 400));
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_MOVED, 10, -400));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldPickBoxFromAngle() {
        Node b = box().rotate('y', 90).handleMove(me);
        Scene s = scene(group(group(b).rotate('x', 40)), perspective(), true);

        makeParallel(s, 0, 0);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 50, -41.95498, 0);
        assertPickResult(e.getPickResult(),
                b, point(50, -41.95498, 0), 934.729635, NOFACE, point(0.5, 0.290226));
    }

    @Test
    public void shouldNotPickBoxOutside() {
        Node b = box().handleMove(me);
        b.setTranslateX(300);
        Scene s = scene(group(b), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        makeParallel(s, 1000, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        makeParallel(s, 10, -500);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        makeParallel(s, 10, 500);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldNotPickRotatedBoxOutside() {
        Node b = box().rotate('y', 30).handleMove(me);
        b.setTranslateX(300);
        Scene s = scene(group(b), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        makeParallel(s, 1000, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        makeParallel(s, 300, -500);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        makeParallel(s, 300, 500);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldPickBoxOnBounds() {
        Box b = box().handleMove(me);
        b.setPickOnBounds(true);
        Scene s = scene(group(b), perspective(), true);

        makeParallel(s, 10, 40);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 40, -200);
        assertPickResult(e.getPickResult(),
                b, point(10, 40, -200), 800, NOFACE, null);
    }

    @Test
    public void shouldNotPickBoxFromInsideIfCulled() {
        Box b = box().handleMove(me);
        b.setCullFace(CullFace.BACK);
        Scene s = scene(group(b), perspective(), true);
        b.setTranslateZ(-1000);
        makeParallel(s, 10, 40);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldPickBoxInteriorByPerspectiveCameraFromInside() {
        Box b = box().handleMove(me);
        b.setCullFace(CullFace.NONE);
        b.setTranslateZ(-1000);
        Scene s = scene(group(b), perspective(), true);
        makeParallel(s, 10, 40);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 40, 200);
        assertPickResult(e.getPickResult(),
                b, point(10, 40, 200), 200, NOFACE, point(0.4, 0.7));
    }

    @Test
    public void shouldPickBoxByParallelCameraFromInside() {
        MouseEventGenerator g = new MouseEventGenerator();
        Box b = box().handleMove(me);
        b.setCullFace(CullFace.NONE);
        Scene s = scene(group(b), parallel(), true);
        b.setTranslateZ(-1000);
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_MOVED, 10, 40));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 40, 10, 40, -200);
        assertPickResult(e.getPickResult(),
                b, point(10, 40, -200), Double.POSITIVE_INFINITY, NOFACE, point(0.6, 0.7));
    }

    @Test
    public void shouldNotPickBoxByPerspectiveCameraFromBehind() {
        Box b = box().handleMove(me);
        b.setCullFace(CullFace.NONE);
        b.setTranslateZ(-3000);
        Scene s = scene(group(b), perspective(), true);
        makeParallel(s, 10, 40);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldPickBoxByParallelCameraFromBehind() {
        MouseEventGenerator g = new MouseEventGenerator();
        Box b = box().handleMove(me);
        b.setCullFace(CullFace.NONE);
        Scene s = scene(group(b), parallel(), true);
        b.setTranslateZ(-3000);
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_MOVED, 10, 40));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 40, 10, 40, -200);
        assertPickResult(e.getPickResult(),
                b, point(10, 40, -200), Double.POSITIVE_INFINITY, NOFACE, point(0.6, 0.7));
    }

    /*****************  SPHERE picking ********************/


    @Test
    public void shouldPickSphereFromFront() {
        Sphere sph = sphere().handleMove(me);
        sph.setCullFace(CullFace.BACK);
        Scene s = scene(group(sph), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, -97.46794);
        assertPickResult(e.getPickResult(),
                sph, point(10, 20, -97.46794), 902.53205, NOFACE, point(0.516273, 0.6));
    }

    @Test
    public void shouldPickSphereInteriorFromFront() {
        Sphere sph = sphere().handleMove(me);
        sph.setCullFace(CullFace.FRONT);
        Scene s = scene(group(sph), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, 97.46794);
        assertPickResult(e.getPickResult(),
                sph, point(10, 20, 97.46794), 1097.46794, NOFACE, point(0.98373, 0.6));
    }

    @Test
    public void shouldPickNotCulledSphereFromFront() {
        Sphere sph = sphere().handleMove(me);
        sph.setCullFace(CullFace.NONE);
        Scene s = scene(group(sph), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, -97.46794);
        assertPickResult(e.getPickResult(),
                sph, point(10, 20, -97.46794), 902.53205, NOFACE, point(0.516273, 0.6));
    }

    @Test
    public void shouldPickSphereFromBack() {
        Sphere sph = sphere().rotate('y', 180).handleMove(me);
        sph.setCullFace(CullFace.BACK);
        Scene s = scene(group(sph), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, -10, 20, 97.46794);
        assertPickResult(e.getPickResult(),
                sph, point(-10, 20, 97.46794), 902.53205, NOFACE, point(0.01628, 0.6));
    }

    @Test
    public void shouldNotPickSphereOutside() {
        Node sph = sphere().handleMove(me);
        sph.setTranslateX(100);
        sph.setTranslateY(110);
        Scene s = scene(group(sph), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldPickRoughSphere() {
        Sphere sph = sphereWith4Divs().handleMove(me);
        Scene s = scene(group(sph), perspective(), true);
        sph.impl_updatePG();
        makeParallel(s, 50, 25);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 50, 25, -25);
        assertPickResult(e.getPickResult(),
                sph, point(50, 25, -25), 975, NOFACE, point(0.59375, 0.62402));
    }

    @Test
    public void shouldNotPickRoughSphereOutsideOfItsTriangles() {
        Sphere sph = sphereWith4Divs().handleMove(me);
        Scene s = scene(group(sph), perspective(), true);
        sph.impl_updatePG();
        makeParallel(s, 50, 60);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldPickRoughSphereFrontFaceInsideOfShape() {
        Sphere sph = sphereWith4Divs().handleMove(me);
        sph.setTranslateZ(-974);
        sph.setCullFace(CullFace.BACK);
        Scene s = scene(group(sph), perspective(), true);
        sph.impl_updatePG();
        makeParallel(s, 50, 25);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 50, 25, -25);
        assertPickResult(e.getPickResult(),
                sph, point(50, 25, -25), 1, NOFACE, point(0.59375, 0.62402));
    }

    @Test
    public void shouldNotPickSphereFromInsideIfCulled() {
        Sphere sph = sphere().handleMove(me);
        sph.setTranslateZ(-980);
        sph.setCullFace(CullFace.BACK);
        Scene s = scene(group(sph), perspective(), true);
        sph.impl_updatePG();
        makeParallel(s, 50, 25);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldPickSphereOnBounds() {
        Sphere sph = sphere().handleMove(me);
        sph.setPickOnBounds(true);
        Scene s = scene(group(sph), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, -100);
        assertPickResult(e.getPickResult(),
                sph, point(10, 20, -100), 900, NOFACE, null);
    }

    @Test
    public void shouldPickRoughSphereOnBounds() {
        Sphere sph = sphereWith4Divs().handleMove(me);
        sph.setPickOnBounds(true);
        Scene s = scene(group(sph), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, -100);
        assertPickResult(e.getPickResult(),
                sph, point(10, 20, -100), 900, NOFACE, null);
    }

    @Test
    public void shouldPickSphereOnBoundsOutsideOfShape() {
        Sphere sph = sphere().handleMove(me);
        Scene s = scene(group(sph), perspective(), true);
        makeParallel(s, 99, 1);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        MouseEvent e = me.event;
        assertNotNull(e);

        sph.setPickOnBounds(true);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 99, 1, -100);
        assertPickResult(e.getPickResult(),
                sph, point(99, 1, -100), 900, NOFACE, null);
    }

    @Test
    public void shouldPickSphereInteriorByPerspectiveCameraFromInside() {
        Sphere sph = sphere().handleMove(me);
        sph.setTranslateZ(-1000);
        sph.setCullFace(CullFace.NONE);
        Scene s = scene(group(sph), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, 97.46794);
        assertPickResult(e.getPickResult(),
                sph, point(10, 20, 97.46794), 97.46794, NOFACE, point(0.98373, 0.6));
    }

    @Test
    public void shouldPickSphereByParallelCameraFromInside() {
        MouseEventGenerator g = new MouseEventGenerator();
        Sphere sph = sphere().handleMove(me);
        sph.setTranslateZ(-1000);
        sph.setCullFace(CullFace.NONE);
        Scene s = scene(group(sph), parallel(), true);
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_MOVED, 10, 20));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, 10, 20, -97.46794);
        assertPickResult(e.getPickResult(),
                sph, point(10, 20, -97.46794), Double.POSITIVE_INFINITY, NOFACE, point(0.516273, 0.6));
    }

    @Test
    public void shouldNotPickSphereByPerspectiveCameraFromBehind() {
        Sphere sph = sphere().handleMove(me);
        sph.setTranslateZ(-1098);
        sph.setCullFace(CullFace.NONE);
        Scene s = scene(group(sph), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldPickSphereByParallelCameraFromBehind() {
        MouseEventGenerator g = new MouseEventGenerator();
        Sphere sph = sphere().handleMove(me);
        sph.setTranslateZ(-1098);
        sph.setCullFace(CullFace.NONE);
        Scene s = scene(group(sph), parallel(), true);
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_MOVED, 10, 20));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, 10, 20, -97.46794);
        assertPickResult(e.getPickResult(),
                sph, point(10, 20, -97.46794), Double.POSITIVE_INFINITY, NOFACE, point(0.516273, 0.6));
    }


    /*****************  CYLINDER picking ********************/


    @Test
    public void shouldPickCylinderFromFront() {
        Cylinder c = cylinder().handleMove(me);
        c.setCullFace(CullFace.BACK);
        Scene s = scene(group(c), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, -48.98979);
        assertPickResult(e.getPickResult(),
                c, point(10, 20, -48.98979), 951.01020, NOFACE, point(0.532048, 0.6));
    }

    @Test
    public void shouldPickCylinderInteriorFromFront() {
        Cylinder c = cylinder().handleMove(me);
        c.setCullFace(CullFace.FRONT);
        Scene s = scene(group(c), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, 48.98979);
        assertPickResult(e.getPickResult(),
                c, point(10, 20, 48.98979), 1048.98979, NOFACE, point(0.96796, 0.6));
    }

    @Test
    public void shouldPickCylinderFromBack() {
        Cylinder c = cylinder().rotate('y', 180).handleMove(me);
        c.setCullFace(CullFace.BACK);
        Scene s = scene(group(c), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, -10, 20, 48.98979);
        assertPickResult(e.getPickResult(),
                c, point(-10, 20, 48.98979), 951.01020, NOFACE, point(0.032048, 0.6));
    }

    @Test
    public void shouldPickNotCulledCylinderFromFront() {
        Cylinder c = cylinder().handleMove(me);
        c.setCullFace(CullFace.NONE);
        Scene s = scene(group(c), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, -48.98979);
        assertPickResult(e.getPickResult(),
                c, point(10, 20, -48.98979), 951.01020, NOFACE, point(0.532048, 0.6));
    }

    @Test
    public void shouldPickCylinderFromTop() {
        Node c = cylinder().rotate('x', 90).handleMove(me);
        Scene s = scene(group(c), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, -100, -20);
        assertPickResult(e.getPickResult(),
                c, point(10, -100, -20), 900, NOFACE, point(0.6, 0.7));
    }

    @Test
    public void shouldPickCylinderInteriorFromTop() {
        Cylinder c = cylinder().rotate('x', 90).handleMove(me);
        c.setCullFace(CullFace.FRONT);
        Scene s = scene(group(c), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 100, -20);
        assertPickResult(e.getPickResult(),
                c, point(10, 100, -20), 1100, NOFACE, point(0.6, 0.3));
    }

    @Test
    public void shouldPickNotCulledCylinderFromTop() {
        Cylinder c = cylinder().rotate('x', 90).handleMove(me);
        c.setCullFace(CullFace.NONE);
        Scene s = scene(group(c), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, -100, -20);
        assertPickResult(e.getPickResult(),
                c, point(10, -100, -20), 900, NOFACE, point(0.6, 0.7));
    }

    @Test
    public void shouldPickCylinderFromBottom() {
        Node c = cylinder().rotate('x', -90).handleMove(me);
        Scene s = scene(group(c), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 100, 20);
        assertPickResult(e.getPickResult(),
                c, point(10, 100, 20), 900, NOFACE, point(0.6, 0.7));
    }

    @Test
    public void shouldNotPickCylinderAboveIt() {
        Node c = cylinder().handleMove(me);
        c.setTranslateY(130);
        Scene s = scene(group(c), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        makeParallel(s, 10, 520);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldNotPickCylinderNextToIt() {
        Node c = cylinder().rotate('y', 45).handleMove(me);
        c.setTranslateX(-48);
        Scene s = scene(group(c), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldNotPickCylinderParallelToIt() {
        Node c = cylinder().rotate('x', 90).handleMove(me);
        Scene s = scene(group(c), perspective(), true);
        makeParallel(s, 48, 48);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        makeParallel(s, -48, 48);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        makeParallel(s, 48, -48);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        makeParallel(s, -48, -48);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        makeParallel(s, 148, 148);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        makeParallel(s, -148, 148);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        makeParallel(s, 148, -148);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        makeParallel(s, -148, -148);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldPickRoughCylinder() {
        Cylinder c = cylinderWith4Divs().handleMove(me);
        Scene s = scene(group(c), perspective(), true);
        c.impl_updatePG();
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, -40);
        assertPickResult(e.getPickResult(),
                c, point(10, 20, -40), 960, NOFACE, point(0.55, 0.59922));
    }

    @Test
    public void shouldNotPickRoughCylinderOutsideOfItsTriangles() {
        Cylinder c = cylinderWith4Divs().rotate('y', 45).handleMove(me);
        Scene s = scene(group(c), perspective(), true);
        c.impl_updatePG();
        makeParallel(s, 48, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldPickRoughCylinderFrontFaceInsideOfShape() {
        Cylinder c = cylinderWith4Divs().handleMove(me);
        c.setTranslateZ(-959);
        c.setCullFace(CullFace.BACK);
        Scene s = scene(group(c), perspective(), true);
        c.impl_updatePG();
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, -40);
        assertPickResult(e.getPickResult(),
                c, point(10, 20, -40), 1, NOFACE, point(0.55, 0.59922));
    }

    @Test
    public void shouldNotPickCylinderFromInsideIfCulled() {
        Cylinder c = cylinder().handleMove(me);
        c.setTranslateZ(-959);
        c.setCullFace(CullFace.BACK);
        Scene s = scene(group(c), perspective(), true);
        c.impl_updatePG();
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldPickCylinderOnBounds() {
        Cylinder c = cylinder().handleMove(me);
        c.setPickOnBounds(true);
        Scene s = scene(group(c), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, -50);
        assertPickResult(e.getPickResult(),
                c, point(10, 20, -50), 950, NOFACE, null);
    }

    @Test
    public void shouldPickRoughCylinderOnBounds() {
        Cylinder c = cylinderWith4Divs().handleMove(me);
        c.setPickOnBounds(true);
        Scene s = scene(group(c), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, -50);
        assertPickResult(e.getPickResult(),
                c, point(10, 20, -50), 950, NOFACE, null);
    }

    @Test
    public void shouldPickCylinderOnBoundsOutsideOfShape() {
        Node c = cylinder().rotate('x', 90).handleMove(me);
        Scene s = scene(group(c), perspective(), true);
        makeParallel(s, 49, 48);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        MouseEvent e = me.event;
        assertNull(e);

        c.setPickOnBounds(true);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 49, -100, -48);
        assertPickResult(e.getPickResult(),
                c, point(49, -100, -48), 900, NOFACE, null);
    }

    @Test
    public void shouldPickCylinderInteriorByPerspectiveCameraFromInside() {
        Cylinder c = cylinder().handleMove(me);
        c.setTranslateZ(-1000);
        c.setCullFace(CullFace.NONE);
        Scene s = scene(group(c), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, 48.98979);
        assertPickResult(e.getPickResult(),
                c, point(10, 20, 48.98979), 48.98979, NOFACE, point(0.96796, 0.6));
    }

    @Test
    public void shouldPickCylinderByParallelCameraFromInside() {
        MouseEventGenerator g = new MouseEventGenerator();
        Cylinder c = cylinder().handleMove(me);
        c.setTranslateZ(-1000);
        c.setCullFace(CullFace.NONE);
        Scene s = scene(group(c), parallel(), true);
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_MOVED, 10, 20));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, 10, 20, -48.98979);
        assertPickResult(e.getPickResult(),
                c, point(10, 20, -48.98979), Double.POSITIVE_INFINITY, NOFACE, point(0.532048, 0.6));
    }

    @Test
    public void shouldNotPickCylinderByPerspectiveCameraFromBehind() {
        Cylinder c = cylinder().handleMove(me);
        c.setTranslateZ(-1049);
        c.setCullFace(CullFace.NONE);
        Scene s = scene(group(c), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldPickCylinderByParallelCameraFromBehind() {
        MouseEventGenerator g = new MouseEventGenerator();
        Cylinder c = cylinder().handleMove(me);
        c.setTranslateZ(-1049);
        c.setCullFace(CullFace.NONE);
        Scene s = scene(group(c), parallel(), true);
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_MOVED, 10, 20));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, 10, 20, -48.98979);
        assertPickResult(e.getPickResult(),
                c, point(10, 20, -48.98979), Double.POSITIVE_INFINITY, NOFACE, point(0.532048, 0.6));
    }


    /*****************  MESH picking ********************/


    @Test
    public void shouldPickMeshXY() {
        Node m = meshXY().handleMove(me);
        Scene s = scene(group(m), perspective(), true);
        makeParallel(s, 60, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 60, 20, 0);
        assertPickResult(e.getPickResult(),
                m, point(60, 20, 0), 1000, 0, point(0.6, 0.2));
    }

    @Test
    public void shouldNotPickMeshXYOutsideOfIt() {
        Node m = meshXY().handleMove(me);
        Scene s = scene(group(m), perspective(), true);
        makeParallel(s, 60, 70);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldNotPickCulledMeshXY() {
        MeshView m = meshXY().handleMove(me);
        m.setCullFace(CullFace.FRONT);
        Scene s = scene(group(m), perspective(), true);
        makeParallel(s, 60, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldPickNotCulledMeshXY() {
        MeshView m = meshXY().handleMove(me);
        m.setCullFace(CullFace.NONE);
        Scene s = scene(group(m), perspective(), true);
        makeParallel(s, 60, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 60, 20, 0);
        assertPickResult(e.getPickResult(),
                m, point(60, 20, 0), 1000, 0, point(0.6, 0.2));
    }

    @Test
    public void shouldPickMeshXYParallel() {
        MeshView m = meshXYParallel().handleMove(me);
        m.setCullFace(CullFace.BACK);
        Scene s = scene(group(m), perspective(), true);
        makeParallel(s, 60, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 60, 20, 7);
        assertPickResult(e.getPickResult(),
                m, point(60, 20, 7), 1007, 0, point(0.6, 0.2));
    }

    @Test
    public void shouldPickMeshXYFlippedTexture() {
        Node m = meshXYFlippedTexture().handleMove(me);
        Scene s = scene(group(m), perspective(), true);
        makeParallel(s, 60, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 60, 20, 7);
        assertPickResult(e.getPickResult(),
                m, point(60, 20, 7), 1007, 0, point(0.4, 0.6));
    }

    @Test
    public void shouldNotPickMeshXYBack() {
        Node m = meshXYBack().handleMove(me);
        Scene s = scene(group(m), perspective(), true);
        makeParallel(s, 60, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldPickMeshYZ() {
        Node m = meshYZ().rotate('y', 90).handleMove(me);
        Scene s = scene(group(m), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 7, 20, 53);
        assertPickResult(e.getPickResult(),
                m, point(7, 20, 53), 1050, 0, point(0.2, 0.53));
    }

    @Test
    public void shouldPickMeshGeneral() {
        Node m = meshGeneral().handleMove(me);
        Scene s = scene(group(m), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, 10);
        assertPickResult(e.getPickResult(),
                m, point(10, 20, 10), 1010, 0, point(0.1, 0.2));
    }

    @Test
    public void shouldPickMeshGeneralStretchedTexture() {
        Node m = meshGeneralStretchedTexture().handleMove(me);
        Scene s = scene(group(m), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, 10);
        assertPickResult(e.getPickResult(),
                m, point(10, 20, 10), 1010, 0, point(0.025, 0.1));
    }

    @Test
    public void shouldPickMeshOnBounds() {
        Node m = meshGeneral().handleMove(me);
        m.setPickOnBounds(true);
        Scene s = scene(group(m), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, 0);
        assertPickResult(e.getPickResult(),
                m, point(10, 20, 0), 1000, NOFACE, null);
    }

    @Test
    public void shouldPickMeshOnBoundsOutsideOfTriangles() {
        Node m = meshGeneral().handleMove(me);
        Scene s = scene(group(m), perspective(), true);

        makeParallel(s, 90, 90);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        MouseEvent e = me.event;
        assertNull(e);

        m.setPickOnBounds(true);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 90, 90, 0);
        assertPickResult(e.getPickResult(),
                m, point(90, 90, 0), 1000, NOFACE, null);
    }

    @Test
    public void shouldNotPickMeshXYByPerspectiveCameraFromBehind() {
        Node m = meshXY().handleMove(me);
        m.setTranslateZ(-3000);
        Scene s = scene(group(m), perspective(), true);
        makeParallel(s, 60, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldPickMeshXYByParallelCameraFromBehind() {
        MouseEventGenerator g = new MouseEventGenerator();
        Node m = meshXY().handleMove(me);
        m.setTranslateZ(-3000);
        Scene s = scene(group(m), parallel(), true);
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_MOVED, 60, 20));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 60, 20, 60, 20, 0);
        assertPickResult(e.getPickResult(),
                m, point(60, 20, 0), Double.POSITIVE_INFINITY, 0, point(0.6, 0.2));
    }

    @Test
    public void shouldNotPickMeshGeneralByPerspectiveCameraFromBehind() {
        Node m = meshGeneral().handleMove(me);
        m.setTranslateZ(-1011);
        Scene s = scene(group(m), perspective(), true);
        makeParallel(s, 10, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);
    }

    @Test
    public void shouldPickMeshGeneralByParallelCameraFromBehind() {
        MouseEventGenerator g = new MouseEventGenerator();
        Node m = meshGeneral().handleMove(me);
        m.setTranslateZ(-1011);
        Scene s = scene(group(m), parallel(), true);
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_MOVED, 10, 20));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 20, 10, 20, 10);
        assertPickResult(e.getPickResult(),
                m, point(10, 20, 10), Double.POSITIVE_INFINITY, 0, point(0.1, 0.2));
    }


    /*****************  DEPTH BUFFER ********************/


    @Test
    public void shouldPickNearestFace() {
        Node m = meshesXY().handleMove(me);
        Scene s = scene(group(m), perspective(), true);
        makeParallel(s, 60, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 60, 20, -7);
        assertPickResult(e.getPickResult(),
                m, point(60, 20, -7), 993, 1, point(0.6, 0.2));
    }

    @Test
    public void shouldPickNearestFace2() {
        Node m = meshesXY2().handleMove(me);
        Scene s = scene(group(m), perspective(), true);
        makeParallel(s, 60, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 60, 20, -7);
        assertPickResult(e.getPickResult(),
                m, point(60, 20, -7), 993, 0, point(0.6, 0.2));
    }

    @Test
    public void shouldPickNearestThroughBackFace() {
        Node m = meshesXYFacingEachOther().handleMove(me);
        Scene s = scene(group(m), perspective(), true);
        makeParallel(s, 60, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 60, 20, 0);
        assertPickResult(e.getPickResult(),
                m, point(60, 20, 0), 1000, 0, point(0.6, 0.2));
    }

    @Test
    public void shouldPickNearestBackIfNotCulled() {
        MeshView m = meshesXYFacingEachOther().handleMove(me);
        m.setCullFace(CullFace.NONE);
        Scene s = scene(group(m), perspective(), true);
        makeParallel(s, 60, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 60, 20, -7);
        assertPickResult(e.getPickResult(),
                m, point(60, 20, -7), 993, 1, point(0.6, 0.2));
    }

    @Test
    public void shouldNotPickShapesIfNearerPickExists() {
        MeshView m = meshXY().handleMove(me);
        m.setTranslateZ(-500);
        Scene s = scene(group(meshXY(), cylinder(), sphere(), box(), m), perspective(), true);
        makeParallel(s, 40, 10);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 40, 10, 0);
        assertPickResult(e.getPickResult(),
                m, point(40, 10, 0), 500, 0, point(0.4, 0.1));
    }

    @Test
    public void shouldPickNearestShapeOfMany() {
        MeshView m = meshXY().handleMove(me);
        m.setTranslateZ(-500);
        m.setTranslateX(-30);
        Scene s = scene(group(m, box(), sphere(), cylinder(), meshXY()), perspective(), true);
        makeParallel(s, 30, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 60, 20, 0);
        assertPickResult(e.getPickResult(),
                m, point(60, 20, 0), 500, 0, point(0.6, 0.2));
    }

    @Test
    public void shouldPickNodeWithDepthTestDisabledCoveredBySibling() {
        MeshView m = meshXY();
        m.setTranslateZ(-500);
        m.setTranslateX(-30);

        Box b = box().handleMove(me);
        b.setDepthTest(DepthTest.DISABLE);

        Scene s = scene(group(m, b), perspective(), true);
        makeParallel(s, 30, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 30, 20, -200);
        assertPickResult(e.getPickResult(),
                b, point(30, 20, -200), 800, NOFACE, point(0.8, 0.6));
    }

    @Test
    public void shouldPickNodeCoveringCloserSiblingWithDepthTestDisabled() {
        MeshView m = meshXY();
        m.setTranslateZ(-500);
        m.setTranslateX(-30);
        m.setDepthTest(DepthTest.DISABLE);

        Box b = box().handleMove(me);
        b.setDepthTest(DepthTest.ENABLE);

        Scene s = scene(group(m, b), perspective(), true);
        makeParallel(s, 30, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 30, 20, -200);
        assertPickResult(e.getPickResult(),
                b, point(30, 20, -200), 800, NOFACE, point(0.8, 0.6));
    }

    @Test
    public void shouldPickNodeWithDisabledDepthtestCoveredByOtherNode() {

        Box closer = box();
        closer.setTranslateZ(-10);

        Box b = box().handleMove(me);
        b.setDepthTest(DepthTest.DISABLE);

        Scene s = scene(group(group(closer), group(b)), perspective(), true);
        makeParallel(s, 30, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 30, 20, -200);
        assertPickResult(e.getPickResult(),
                b, point(30, 20, -200), 800, NOFACE, point(0.8, 0.6));
    }

    @Test
    public void shouldPickNodeCoveringNodeWithDisabledDepthtest() {

        Box b1 = box();

        Box b = box();
        b.setDepthTest(DepthTest.DISABLE);

        Box b2 = box().handleMove(me);
        b2.setTranslateZ(-10);

        Scene s = scene(group(group(b1), group(b), group(b2)), perspective(), true);
        makeParallel(s, 30, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 30, 20, -200);
        assertPickResult(e.getPickResult(),
                b2, point(30, 20, -200), 790, NOFACE, point(0.8, 0.6));
    }

    @Test
    public void shouldPickByOrderIfParentsDepthTestDisabled() {
        MeshView m = meshXY();
        m.setTranslateZ(-500);
        m.setTranslateX(-30);

        Box b = box().handleMove(me);

        Group parent = group(m, b);
        parent.setDepthTest(DepthTest.DISABLE);

        Scene s = scene(parent, perspective(), true);
        makeParallel(s, 30, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 30, 20, -200);
        assertPickResult(e.getPickResult(),
                b, point(30, 20, -200), 800, NOFACE, point(0.8, 0.6));
    }

    @Test
    public void depthTestShouldHaveNoEffectWithoutDepthBuffer() {
        MeshView m = meshXY();
        m.setTranslateZ(-500);
        m.setTranslateX(-30);
        m.setDepthTest(DepthTest.ENABLE);

        Box b = box().handleMove(me);
        b.setDepthTest(DepthTest.ENABLE);

        Scene s = scene(group(m, b), perspective(), false);
        makeParallel(s, 30, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 30, 20, -200);
        assertPickResult(e.getPickResult(),
                b, point(30, 20, -200), 800, NOFACE, point(0.8, 0.6));
    }

    @Test
    public void disabledDepthTestShouldNotInfluencePickingIfNotPicked() {

        Box b1 = box().handleMove(me);
        b1.setDepthTest(DepthTest.ENABLE);

        Box b2 = box();
        b2.setTranslateX(500);
        b2.setDepthTest(DepthTest.DISABLE);

        Scene s = scene(group(b1, b2), perspective(), false);
        makeParallel(s, 30, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 30, 20, -200);
        assertPickResult(e.getPickResult(),
                b1, point(30, 20, -200), 800, NOFACE, point(0.8, 0.6));
    }

    @Test
    public void disabledDepthTestShouldNotInfluencePickingIfNotPickedByNonEmptyResult() {

        Box odd = box();
        odd.setTranslateZ(10);

        Box b1 = box().handleMove(me);
        b1.setDepthTest(DepthTest.ENABLE);

        Box b2 = box();
        b2.setTranslateX(500);
        b2.setDepthTest(DepthTest.DISABLE);

        Scene s = scene(group(b1, b2, odd), perspective(), true);
        makeParallel(s, 30, 20);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 30, 20, -200);
        assertPickResult(e.getPickResult(),
                b1, point(30, 20, -200), 800, NOFACE, point(0.8, 0.6));
    }




    /*****************  SCENE picking ********************/


    @Test
    public void shouldPickScene() {
        Rectangle r = rect().handleMove(me);

        Group parent = group(r).handleMove(pme);

        Scene s = scene(parent, perspective(), true).handleMove(sme);
        makeParallel(s, 150, 160);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        assertNull(me.event);
        assertNull(pme.event);

        MouseEvent e = sme.event;
        assertNotNull(e);
        assertCoordinates(e, 150, 160, 0);
        assertPickResult(e.getPickResult(), null, point(150, 160, 0), 1000,
                NOFACE, null);
    }


    /*****************  SHAPE COMBINATION picking ********************/


    @Test
    public void shouldPickFirstShapeWithoutDepthBuffer() {
        Node trg;
        Group root = group(sphere(), trg = cylinder());
        Scene s = scene(root, perspective(), false).handleMove(me);
        makeParallel(s, 0, 0);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertSame(trg, e.getPickResult().getIntersectedNode());
    }

    @Test
    public void shouldPickNearestShapeWithDepthBuffer() {
        Node trg;
        Group root = group(trg = sphere(), cylinder());
        Scene s = scene(root, perspective(), true).handleMove(me);
        makeParallel(s, 0, 0);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertSame(trg, e.getPickResult().getIntersectedNode());
    }


    /*****************  RECTANGLE 3D picking ********************/


    @Test
    public void shouldPickRectWithPickRay() {
        Rectangle r = rect().handleMove(me);

        Scene s = scene(group(r), perspective(), true);
        makeParallel(s, 50, 60);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 50, 60, 0);
        assertPickResult(e.getPickResult(),
                r, point(50, 60, 0), 1000, NOFACE, null);
    }

    @Test
    public void localCoordinatesShouldBeCorrectDuringBubbling() {
        Rectangle r = rect().handleMove(me);
        r.setTranslateX(100);
        r.setTranslateZ(50);

        Group parent = group(r).handleMove(pme);
        parent.setTranslateZ(-20);

        Scene s = scene(parent, perspective(), true).handleMove(sme);
        makeParallel(s, 150, 60);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 50, 60, 0);

        e = pme.event;
        assertNotNull(e);
        assertCoordinates(e, 150, 60, 50);

        e = sme.event;
        assertNotNull(e);
        assertCoordinates(e, 150, 60, 30);
    }


    @Test
    public void shouldPickRectRotatedIn3D() {
        Rectangle r = rect().rotate('y', 45).handleMove(me);

        Scene s = scene(group(r), perspective(), true).handleMove(sme);

        makeParallel(s, 10, 50);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));

        MouseEvent e = me.event;
        assertNull(e);

        e = sme.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 50, 0);
        assertPickResult(e.getPickResult(), null, point(10, 50, 0), 1000,
                NOFACE, null);
        sme.clear();

        makeParallel(s, 30, 50);
        s.impl_processMouseEvent(generateMouseEvent(MouseEvent.MOUSE_MOVED));
        e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 21.71572, 50, 0);
        assertPickResult(e.getPickResult(), r, point(21.71572, 50, 0), 1020,
                NOFACE, null);

        e = sme.event;
        assertNotNull(e);
        assertCoordinates(e, 30, 50, 20);
        assertPickResult(e.getPickResult(), r, point(21.71572, 50, 0), 1020,
                NOFACE, null);
    }

    @Test
    public void shouldPickRectTranslatedAlongZByParallelCamera() {
        MouseEventGenerator g = new MouseEventGenerator();
        Rectangle r = rect().handleMove(me);
        r.setTranslateZ(70);

        Scene s = scene(group(r), parallel(), true).handleMove(sme);

        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_MOVED, 10, 50));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 50, 10, 50, 0);
        assertPickResult(e.getPickResult(), r, point(10, 50, 0),
                Double.POSITIVE_INFINITY, NOFACE, null);

        e = sme.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 50, 10, 50, 70);
        assertPickResult(e.getPickResult(), r, point(10, 50, 0),
                Double.POSITIVE_INFINITY, NOFACE, null);
    }

    @Test
    public void shouldPickRectRotatedIn3DByParallelCamera() {
        MouseEventGenerator g = new MouseEventGenerator();
        Rectangle r = rect().rotate('y', 45).handleMove(me);

        Scene s = scene(group(r), parallel(), true).handleMove(sme);

        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_MOVED, 10, 50));

        MouseEvent e = me.event;
        assertNull(e);

        e = sme.event;
        assertNotNull(e);
        assertCoordinates(e, 10, 50, 10, 50, 0);
        assertPickResult(e.getPickResult(), null, point(10, 50, 0),
                Double.POSITIVE_INFINITY, NOFACE, null);
        sme.clear();

        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_MOVED, 30, 50));
        e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 30, 50, 21.71572, 50, 0);
        assertPickResult(e.getPickResult(), r, point(21.71572, 50, 0),
                Double.POSITIVE_INFINITY, NOFACE, null);

        e = sme.event;
        assertNotNull(e);
        assertCoordinates(e, 30, 50, 30, 50, 20);
        assertPickResult(e.getPickResult(), r, point(21.71572, 50, 0),
                Double.POSITIVE_INFINITY, NOFACE, null);
    }


    /*****************  Scenegraph-generated events ********************/

    @Test
    public void shouldReportCorrectPickResultForClick() {
        MouseEventGenerator g = new MouseEventGenerator();
        Box b = box().handle(MouseEvent.MOUSE_CLICKED, me);

        Scene s = scene(group(b), perspective(), true);

        makeParallel(s, 20, 50);
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_PRESSED,
                PERSPECTIVE_CAMERA_X - 10, PERSPECTIVE_CAMERA_Y));
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_RELEASED,
                PERSPECTIVE_CAMERA_X, PERSPECTIVE_CAMERA_Y));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 20, 50, -200);
        assertPickResult(e.getPickResult(), b, point(20, 50, -200), 800,
                NOFACE, point(0.7, 0.75));
    }

    @Test
    public void shouldReportCorrectPickResultForEnteredExited() {
        MouseEventGenerator g = new MouseEventGenerator();
        Box b1 = box().handle(MouseEvent.MOUSE_EXITED, me);
        b1.setTranslateX(55);
        Box b2 = box().handle(MouseEvent.MOUSE_ENTERED, pme);
        b2.setTranslateX(100);
        b2.setTranslateZ(-1);

        Scene s = scene(group(b1, b2), perspective(), true);

        makeParallel(s, 70, 50);
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_PRESSED,
                PERSPECTIVE_CAMERA_X - 60, PERSPECTIVE_CAMERA_Y));
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_RELEASED,
                PERSPECTIVE_CAMERA_X, PERSPECTIVE_CAMERA_Y));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 15, 50, -201);
        assertPickResult(e.getPickResult(), b2, point(-30, 50, -200), 799,
                NOFACE, point(0.2, 0.75));

        e = pme.event;
        assertNotNull(e);
        assertCoordinates(e, -30, 50, -200);
        assertPickResult(e.getPickResult(), b2, point(-30, 50, -200), 799,
                NOFACE, point(0.2, 0.75));
    }

    @Test
    public void shouldReportCorrectPickResultForDragDetected() {
        MouseEventGenerator g = new MouseEventGenerator();
        Box b = box().handle(MouseEvent.DRAG_DETECTED, me);

        Scene s = scene(group(b), perspective(), true);

        makeParallel(s, 20, 50);
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_PRESSED,
                PERSPECTIVE_CAMERA_X - 10, PERSPECTIVE_CAMERA_Y));
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_DRAGGED,
                PERSPECTIVE_CAMERA_X, PERSPECTIVE_CAMERA_Y));
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_RELEASED,
                PERSPECTIVE_CAMERA_X, PERSPECTIVE_CAMERA_Y));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 20, 50, -200);
        assertPickResult(e.getPickResult(), b, point(20, 50, -200), 800,
                NOFACE, point(0.7, 0.75));
    }



    /*****************  PDR ********************/


    @Test
    public void shouldReportCorrectPickResultDuringPDR() {
        MouseEventGenerator g = new MouseEventGenerator();
        Box b1 = box().handleDrag(me);
        b1.setTranslateX(55);
        Box b2 = box();
        b2.setTranslateX(100);
        b2.setTranslateZ(-1);

        Scene s = scene(group(b1, b2), perspective(), true);

        makeParallel(s, 70, 50);
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_PRESSED,
                PERSPECTIVE_CAMERA_X - 60, PERSPECTIVE_CAMERA_Y));
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_DRAGGED,
                PERSPECTIVE_CAMERA_X, PERSPECTIVE_CAMERA_Y));
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_RELEASED,
                PERSPECTIVE_CAMERA_X, PERSPECTIVE_CAMERA_Y));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 15, 50, -201);
        assertPickResult(e.getPickResult(), b2, point(-30, 50, -200), 799,
                NOFACE, point(0.2, 0.75));
    }

    @Test
    public void shouldReportCorrectPickResultForFullPDR() {
        EventHolder<MouseDragEvent> smde = new EventHolder<MouseDragEvent>();
        EventHolder<MouseDragEvent> tmde = new EventHolder<MouseDragEvent>();
        EventHolder<MouseDragEvent> tmde2 = new EventHolder<MouseDragEvent>();

        MouseEventGenerator g = new MouseEventGenerator();
        final Box b1 = box().handleFullPDR(MouseDragEvent.MOUSE_DRAG_EXITED, smde);
        b1.setTranslateX(55);
        b1.setOnDragDetected(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                b1.startFullDrag();
            }
        });

        Box b2 = box().handleFullPDR(MouseDragEvent.MOUSE_DRAG_ENTERED, tmde)
                .handleFullPDR(MouseDragEvent.MOUSE_DRAG_OVER, tmde2);
        b2.setTranslateX(100);
        b2.setTranslateZ(-1);

        Scene s = scene(group(b1, b2), perspective(), true);

        makeParallel(s, 70, 50);
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_PRESSED,
                PERSPECTIVE_CAMERA_X - 60, PERSPECTIVE_CAMERA_Y));
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_DRAGGED,
                PERSPECTIVE_CAMERA_X - 60, PERSPECTIVE_CAMERA_Y + 10));
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_DRAGGED,
                PERSPECTIVE_CAMERA_X - 59, PERSPECTIVE_CAMERA_Y + 10));
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_DRAGGED,
                PERSPECTIVE_CAMERA_X, PERSPECTIVE_CAMERA_Y));
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_RELEASED,
                PERSPECTIVE_CAMERA_X, PERSPECTIVE_CAMERA_Y));

        MouseEvent e = smde.event;
        assertNotNull(e);
        assertCoordinates(e, 15, 50, -201);
        assertPickResult(e.getPickResult(), b2, point(-30, 50, -200), 799,
                NOFACE, point(0.2, 0.75));

        e = tmde.event;
        assertNotNull(e);
        assertCoordinates(e, -30, 50, -200);
        assertPickResult(e.getPickResult(), b2, point(-30, 50, -200), 799,
                NOFACE, point(0.2, 0.75));

        e = tmde.event;
        assertNotNull(e);
        assertCoordinates(e, -30, 50, -200);
        assertPickResult(e.getPickResult(), b2, point(-30, 50, -200), 799,
                NOFACE, point(0.2, 0.75));
    }


    /***************** moving camera ********************/

    @Test
    public void takesPerspectiveCameraMovesIntoAccount() {
        MouseEventGenerator g = new MouseEventGenerator();
        Box b = box().handleMove(me);
        Camera cam = perspective();
        Group camGroup = group(cam);
        cam.setTranslateX(-143);
        camGroup.setTranslateX(123);
        cam.impl_updatePG();
        cam.setTranslateX(-43);
        camGroup.setTranslateX(23);
        cam.impl_updatePG();

        Scene s = scene(group(b), cam, true);
        s.getRoot().getChildren().add(camGroup);
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_MOVED, 30, 40));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 30, 40, 50, 70.638298, -85.106383);
        assertPickResult(e.getPickResult(),
                b, point(50, 70.638298, -85.106383), 1063.207158, NOFACE, point(0.287234, 0.853192));
    }

    @Test
    public void takesParallelCameraMovesIntoAccount() {
        MouseEventGenerator g = new MouseEventGenerator();
        Box b = box().handleMove(me);
        Camera cam = parallel();
        Group camGroup = group(cam);
        cam.setTranslateX(-143);
        camGroup.setTranslateX(123);
        cam.impl_updatePG();
        cam.setTranslateX(-43);
        camGroup.setTranslateX(23);
        cam.impl_updatePG();

        Scene s = scene(group(b), cam, true);
        s.getRoot().getChildren().add(camGroup);
        s.impl_processMouseEvent(g.generateMouseEvent(MouseEvent.MOUSE_MOVED, 30, 40));

        MouseEvent e = me.event;
        assertNotNull(e);
        assertCoordinates(e, 30, 40, 10, 40, -200);
        assertPickResult(e.getPickResult(),
                b, point(10, 40, -200), Double.POSITIVE_INFINITY, NOFACE, point(0.6, 0.7));
    }


    /***************** helper stuff ********************/

    private MouseEvent generateMouseEvent(EventType<MouseEvent> type) {
        MouseEventGenerator g = new MouseEventGenerator();
        return g.generateMouseEvent(type, PERSPECTIVE_CAMERA_X, PERSPECTIVE_CAMERA_Y);
    }

    /**
     * Moves the camera so that picking on point
     * [PERSPECTIVE_CAMERA_X, PERSPECTIVE_CAMERA_Y] results in pick ray
     * with origin [x, y, -1000] and direction [0, 0, 1000].
     */
    private void makeParallel(Scene s, double x, double y) {
        if (!s.getRoot().getChildren().contains(s.getCamera())) {
            s.getRoot().getChildren().add(s.getCamera());
        }
        s.getCamera().setTranslateX(x - PERSPECTIVE_CAMERA_X);
        s.getCamera().setTranslateY(y - PERSPECTIVE_CAMERA_Y);
        s.getCamera().impl_updatePG();
    }

    private Camera perspective() {
        PerspectiveCamera cam = new PerspectiveCamera();
        // this field of view makes camera Z position to be -1000
        cam.setFieldOfView(43.60281897);
        return cam;
    }

    private Camera parallel() {
        return new ParallelCamera();
    }

    private static TestRect rect() {
        return new TestRect();
    }

    private static TestBox box() {
        return new TestBox();
    }

    private static TestSphere sphere() {
        return new TestSphere();
    }

    private static TestSphere sphereWith4Divs() {
        return new TestSphere(4);
    }

    private static TestCylinder cylinder() {
        return new TestCylinder();
    }

    private static TestCylinder cylinderWith4Divs() {
        return new TestCylinder(4);
    }

    private static TestMesh meshXY() {
        return new TestMesh(
            new float[] {0f, 0f, 0f,   100f, 0f, 0f,   100f, 100f, 0f },
            new float[] {0f, 0f,   1f, 0f,   1f, 1f},
            new int[] {0, 0, 2, 2, 1, 1});
    }

    private static TestMesh meshXYParallel() {
        return new TestMesh(
            new float[] {0f, 0f, 7f,   100f, 0f, 7f,   100f, 100f, 7f },
            new float[] {0f, 0f,   1f, 0f,   1f, 1f},
            new int[] {0, 0, 2, 2, 1, 1});
    }

    private static TestMesh meshesXY() {
        return new TestMesh(
            new float[] {0f, 0f, 0f,   100f, 0f, 0f,   100f, 100f, 0f,
                         0f, 0f, -7f,   100f, 0f, -7f,   100f, 100f, -7f },
            new float[] {0f, 0f,   1f, 0f,   1f, 1f},
            new int[] {0, 0, 2, 2, 1, 1,
                       3, 0, 5, 2, 4, 1});
    }

    private static TestMesh meshesXY2() {
        return new TestMesh(
            new float[] {0f, 0f, 0f,   100f, 0f, 0f,   100f, 100f, 0f,
                         0f, 0f, -7f,   100f, 0f, -7f,   100f, 100f, -7f },
            new float[] {0f, 0f,   1f, 0f,   1f, 1f},
            new int[] {3, 0, 5, 2, 4, 1,
                       0, 0, 2, 2, 1, 1});
    }

    private static TestMesh meshesXYFacingEachOther() {
        return new TestMesh(
            new float[] {0f, 0f, 0f,   100f, 0f, 0f,   100f, 100f, 0f,
                         0f, 0f, -7f,   100f, 0f, -7f,   100f, 100f, -7f },
            new float[] {0f, 0f,   1f, 0f,   1f, 1f},
            new int[] {0, 0, 2, 2, 1, 1,
                       3, 0, 4, 1, 5, 2,});
    }

    private static TestMesh meshXYBack() {
        return new TestMesh(
            new float[] {0f, 0f, 7f,   100f, 0f, 7f,   100f, 100f, 7f },
            new float[] {0f, 0f,   1f, 0f,   1f, 1f},
            new int[] {0, 0, 1, 1, 2, 2});
    }

    private static TestMesh meshXYFlippedTexture() {
        return new TestMesh(
            new float[] {0f, 0f, 7f,   100f, 0f, 7f,   100f, 100f, 7f },
            new float[] {0f, 0f,   0f, 1f,   1f, 1f},
            new int[] {0, 0, 2, 1, 1, 2});
    }

    private static TestMesh meshYZ() {
        return new TestMesh(
            new float[] {7f, 0f, 0f,   7f, 0f, 100f,   7f, 100f, 100f },
            new float[] {0f, 0f,   0f, 1f,   1f, 1f},
            new int[] {0, 0, 2, 2, 1, 1});
    }

    private static TestMesh meshGeneral() {
        return new TestMesh(
            new float[] {0f, 100f, 0f,   100f, 0f, 100f,   0f, 0f, 0f },
            new float[] {0f, 1f,   1f, 0f,   0f, 0f},
            new int[] {0, 0, 1, 1, 2, 2});
    }

    private static TestMesh meshGeneralStretchedTexture() {
        return new TestMesh(
            new float[] {0f, 100f, 0f,   100f, 0f, 100f,   0f, 0f, 0f },
            new float[] {0f, 0.5f,   0.25f, 0f,   0f, 0f},
            new int[] {0, 0, 1, 1, 2, 2});
    }

    private static TestGroup group(Node... children) {
        return new TestGroup(children);
    }

    private Point3D point(double x, double y, double z) {
        return new Point3D(x, y, z);
    }

    private Point2D point(double x, double y) {
        return new Point2D(x, y);
    }

    private static TestScene scene(Parent root, Camera camera, boolean depthBuffer) {
        return new TestScene(root, camera, depthBuffer);
    }

    private static void doRotate(Node node, char ax, double angle) {
        Point3D axis = null;
        switch(ax) {
            case 'x': axis = Rotate.X_AXIS; break;
            case 'y': axis = Rotate.Y_AXIS; break;
            case 'z': axis = Rotate.Z_AXIS; break;
        }
        node.setRotationAxis(axis);
        node.setRotate(angle);
    }

    private static void doHandleMove(Node node, final EventHolder<MouseEvent> holder) {
        holder.event = null;
        node.setOnMouseMoved(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                holder.event = event;
            }
        });
    }

    private static class TestScene extends Scene {

        public TestScene(Parent root, Camera camera, boolean depthBuffer) {
            super(root, 1000, 800, depthBuffer);
            setCamera(camera);
            Stage stage = new Stage();
            stage.setScene(this);
            stage.show();
        }

        public TestScene handleMove(final EventHolder<MouseEvent> holder) {
            holder.event = null;
            setOnMouseMoved(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    holder.event = event;
                }
            });
            return this;
        }
    }

    private static class TestGroup extends Group {
        public TestGroup(Node... nodes) {
            super(nodes);
        }

        public TestGroup rotate(char ax, double angle) {
            doRotate(this, ax, angle);
            return this;
        }

        public TestGroup handleMove(final EventHolder<MouseEvent> holder) {
            doHandleMove(this, holder);
            return this;
        }
    }

    private static class TestRect extends Rectangle {
        public TestRect() {
            super(100, 100);
        }

        public TestRect rotate(char ax, double angle) {
            doRotate(this, ax, angle);
            return this;
        }

        public TestRect handleMove(final EventHolder<MouseEvent> holder) {
            doHandleMove(this, holder);
            return this;
        }
    }

    private static class TestBox extends Box {
        public TestBox() {
            super(100, 200, 400);
        }

        public TestBox rotate(char ax, double angle) {
            doRotate(this, ax, angle);
            return this;
        }

        public TestBox handleMove(final EventHolder<MouseEvent> holder) {
            doHandleMove(this, holder);
            return this;
        }

        public TestBox handleDrag(final EventHolder<MouseEvent> holder) {
            holder.event = null;
            setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    holder.event = event;
                }
            });
            return this;
        }

        public TestBox handle(final EventType<MouseEvent> type,
                final EventHolder<MouseEvent> holder) {
            holder.event = null;
            addEventHandler(type, new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    holder.event = event;
                }
            });
            return this;
        }

        public TestBox handleFullPDR(final EventType<MouseDragEvent> type,
                final EventHolder<MouseDragEvent> holder) {
            holder.event = null;
            addEventHandler(type, new EventHandler<MouseDragEvent>() {
                @Override public void handle(MouseDragEvent event) {
                    holder.event = event;
                }
            });
            return this;
        }
    }

    private static class TestSphere extends Sphere {
        public TestSphere() {
            super(100);
        }

        public TestSphere(int divs) {
            super(100, divs);
        }

        public TestSphere rotate(char ax, double angle) {
            doRotate(this, ax, angle);
            return this;
        }

        public TestSphere handleMove(final EventHolder<MouseEvent> holder) {
            doHandleMove(this, holder);
            return this;
        }
    }

    private static class TestCylinder extends Cylinder {
        public TestCylinder() {
            super(50, 200);
        }

        public TestCylinder(int divs) {
            super(50, 200, divs);
        }

        public TestCylinder rotate(char ax, double angle) {
            doRotate(this, ax, angle);
            return this;
        }

        public TestCylinder handleMove(final EventHolder<MouseEvent> holder) {
            doHandleMove(this, holder);
            return this;
        }
    }

    private static class TestMesh extends MeshView {
        public TestMesh(float[] points, float[] tex, int[] faces) {
            super(new TriangleMesh(points, tex, faces));
        }

        public TestMesh rotate(char ax, double angle) {
            doRotate(this, ax, angle);
            return this;
        }

        public TestMesh handleMove(final EventHolder<MouseEvent> holder) {
            doHandleMove(this, holder);
            return this;
        }
    }

    private static class EventHolder<T extends Event> {
        public T event = null;

        public void clear() {
            event = null;
        }
    }

    private void assertCoordinates(MouseEvent e, double sceneX, double sceneY,
            double x, double y, double z) {
        assertEquals(sceneX, e.getSceneX(), 0.00001);
        assertEquals(sceneY, e.getSceneY(), 0.00001);
        assertEquals(x, e.getX(), 0.00001);
        assertEquals(y, e.getY(), 0.00001);
        assertEquals(z, e.getZ(), 0.00001);
    }

    private void assertCoordinates(MouseEvent e,
            double x, double y, double z) {
        assertEquals(PERSPECTIVE_CAMERA_X, e.getSceneX(), 0.00001);
        assertEquals(PERSPECTIVE_CAMERA_Y, e.getSceneY(), 0.00001);
        assertEquals(x, e.getX(), 0.00001);
        assertEquals(y, e.getY(), 0.00001);
        assertEquals(z, e.getZ(), 0.00001);
    }

    private void assertPickResult(PickResult res, Node n, Point3D p,
            double distance, int face, Point2D tex) {
        assertSame(n, res.getIntersectedNode());
        assertEquals(p.getX(), res.getIntersectedPoint().getX(), 0.00001);
        assertEquals(p.getY(), res.getIntersectedPoint().getY(), 0.00001);
        assertEquals(p.getZ(), res.getIntersectedPoint().getZ(), 0.00001);
        assertEquals(distance, res.getIntersectedDistance(), 0.00001);
        assertEquals(face, res.getIntersectedFace());
        if (tex == null) {
            assertNull(res.getIntersectedTexCoord());
        } else {
            assertEquals(tex.getX(), res.getIntersectedTexCoord().getX(), 0.00001);
            assertEquals(tex.getY(), res.getIntersectedTexCoord().getY(), 0.00001);
        }
    }

}
