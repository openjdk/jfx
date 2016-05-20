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

package com.sun.javafx.scene.shape;

import com.sun.javafx.geom.PickRay;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.util.Utils;
import javafx.scene.Node;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Mesh;

/**
 * Used to access internal methods of Mesh.
 */
public abstract class MeshHelper {
    private static MeshAccessor meshAccessor;

    static {
        Utils.forceInit(Mesh.class);
    }

    protected MeshHelper() {
    }

    private static MeshHelper getHelper(Mesh mesh) {
        return meshAccessor.getHelper(mesh);
    }

    protected static void setHelper(Mesh mesh, MeshHelper meshHelper) {
        meshAccessor.setHelper(mesh, meshHelper);
    }

    /*
     * Static helper methods for cases where the implementation is done in an
     * instance method that is overridden by subclasses.
     * These methods exist in the base class only.
     */

    /*
     * Picking implementation.
     * @param pickRay The pick ray
     * @param pickResult The pick result to be updated (if a closer intersection is found)
     * @param candidate The Node that owns this mesh to be filled in the pick
     *                  result in case a closer intersection is found
     * @param cullFace The cull face of the node that owns this mesh
     * @param reportFace Whether to report the hit face
     * @return true if the pickRay intersects this mesh (regardless of whether
     *              the pickResult has been updated)
     */
    public static boolean computeIntersects(Mesh mesh,
            PickRay pickRay, PickResultChooser pickResult, Node candidate,
            CullFace cullFace, boolean reportFace) {
        return getHelper(mesh).computeIntersectsImpl(mesh,
                pickRay, pickResult, candidate, cullFace, reportFace);
    }

    /*
     * Methods that will be overridden by subclasses
     */

    protected abstract boolean computeIntersectsImpl(Mesh mesh,
            PickRay pickRay, PickResultChooser pickResult, Node candidate,
            CullFace cullFace, boolean reportFace);

    /*
     * Methods used by Mesh (base) class only
     */

    public static void setMeshAccessor(final MeshAccessor newAccessor) {
        if (meshAccessor != null) {
            throw new IllegalStateException();
        }

        meshAccessor = newAccessor;
    }

    public interface MeshAccessor {
        MeshHelper getHelper(Mesh mesh);
        void setHelper(Mesh mesh, MeshHelper meshHelper);
    }

}

