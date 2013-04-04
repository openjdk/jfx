/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.*;
import org.junit.Test;

public class SubSceneTest {

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

    @Test(expected = IllegalArgumentException.class)
    public void testSetIllegalCameraFromOtherScene() {
        Camera camera = new PerspectiveCamera();

        Scene scene = new Scene(new Group(camera));

        SubScene subScene = new SubScene(new Group(), 150, 150);
        Scene otherScene = new Scene(new Group(subScene));

        scene.setCamera(camera);
        subScene.setCamera(camera);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetIllegalCameraFromItsScene() {
        Camera camera = new PerspectiveCamera();

        SubScene subScene = new SubScene(new Group(), 150, 150);
        Scene scene = new Scene(new Group(camera, subScene));

        scene.setCamera(camera);
        subScene.setCamera(camera);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetIllegalCameraFromOtherSubScene() {
        Camera camera = new PerspectiveCamera();

        SubScene subScene = new SubScene(new Group(), 150, 150);
        Scene scene = new Scene(new Group(subScene));

        SubScene otherSubScene = new SubScene(new Group(camera), 100, 100);
        Scene otherScene = new Scene(new Group(otherSubScene));

        otherSubScene.setCamera(camera);
        subScene.setCamera(camera);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetIllegalCameraFromSubScene() {
        Camera camera = new PerspectiveCamera();

        SubScene subScene = new SubScene(new Group(), 150, 150);
        Scene scene = new Scene(new Group(subScene));

        SubScene otherSubScene = new SubScene(new Group(camera), 100, 100);

        otherSubScene.setCamera(camera);
        subScene.setCamera(camera);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetIllegalCameraFromNestedSubScene() {
        Camera camera = new PerspectiveCamera();

        SubScene nestedSubScene = new SubScene(new Group(camera), 100, 100);
        SubScene subScene = new SubScene(new Group(nestedSubScene), 150, 150);
        Scene scene = new Scene(new Group(subScene));

        nestedSubScene.setCamera(camera);
        subScene.setCamera(camera);
    }
}
