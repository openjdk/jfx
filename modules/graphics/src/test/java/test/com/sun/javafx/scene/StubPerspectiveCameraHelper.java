/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene;

import com.sun.javafx.scene.PerspectiveCameraHelper;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.util.Utils;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import test.javafx.scene.CameraTest;

public class StubPerspectiveCameraHelper extends PerspectiveCameraHelper {

    private static final StubPerspectiveCameraHelper theInstance;
    private static StubPerspectiveCameraAccessor stubPerspectiveCameraAccessor;

    static {
        theInstance = new StubPerspectiveCameraHelper();
        Utils.forceInit(CameraTest.StubPerspectiveCamera.class);
    }

    private static StubPerspectiveCameraHelper getInstance() {
        return theInstance;
    }

    public static void initHelper(PerspectiveCamera perspectiveCamera) {
        setHelper(perspectiveCamera, getInstance());
    }

    @Override
    protected NGNode createPeerImpl(Node node) {
        return stubPerspectiveCameraAccessor.doCreatePeer(node);
    }

    public static void setStubPerspectiveCameraAccessor(final StubPerspectiveCameraAccessor newAccessor) {
        if (stubPerspectiveCameraAccessor != null) {
            throw new IllegalStateException();
        }

        stubPerspectiveCameraAccessor = newAccessor;
    }

    public interface StubPerspectiveCameraAccessor {
        NGNode doCreatePeer(Node node);
    }

}


