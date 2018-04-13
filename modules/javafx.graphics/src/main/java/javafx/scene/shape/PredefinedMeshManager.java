/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.shape;

import java.util.HashMap;
import java.util.Map;
import java.lang.ref.SoftReference;
import javafx.scene.shape.Shape3D.Key;

final class PredefinedMeshManager {
    private static final int INITAL_CAPACITY = 17; // TODO
    private static final float LOAD_FACTOR = 0.75f;

    private static class TriangleMeshCache {
        Map<Key, SoftReference<TriangleMesh>> cache = new HashMap<>(INITAL_CAPACITY, LOAD_FACTOR);

        private TriangleMesh get(Key key) {
            cleanCache();
            return (cache.containsKey(key))? cache.get(key).get() : null;
        }

        private void put(Key key, TriangleMesh mesh) {
            cleanCache();
            if (mesh != null) {
                cache.put(key, new SoftReference<TriangleMesh>(mesh));
            }
        }

        private void cleanCache() {
            cache.values().removeIf(ref -> ref.get() == null);
        }

        private void clear() {
            cache.clear();
        }

        private int size() {
            cleanCache();
            return cache.size();
        }

        // for testing purpose
        private void printStats(String name) {
            System.out.println(name + " size:    " + size());
        }

        private void invalidateMesh(Key key) {
            if (cache.containsKey(key)) {
                TriangleMesh mesh = cache.get(key).get();
                if (mesh != null) {
                    mesh.decRef();
                    int count = mesh.getRefCount();
                    if (count == 0) {
                        cache.remove(key);
                    }
                } else {
                    cache.remove(key);
                }
            }
        }
    }

    private static final PredefinedMeshManager INSTANCE = new PredefinedMeshManager();
    private TriangleMeshCache boxCache = null;
    private TriangleMeshCache sphereCache = null;
    private TriangleMeshCache cylinderCache = null;

    private PredefinedMeshManager() {}

    static PredefinedMeshManager getInstance() {
        return INSTANCE;
    }

    synchronized TriangleMesh getBoxMesh(float w, float h, float d, Key key) {
        if (boxCache == null) {
            boxCache = BoxCacheLoader.INSTANCE;
        }

        TriangleMesh mesh = boxCache.get(key);
        if (mesh == null) {
            mesh = Box.createMesh(w, h, d);
            boxCache.put(key, mesh);
        } else {
            mesh.incRef();
        }
        return mesh;
    }

    synchronized TriangleMesh getSphereMesh(float r, int div, Key key) {
        if (sphereCache == null) {
            sphereCache = SphereCacheLoader.INSTANCE;
        }

        TriangleMesh mesh = sphereCache.get(key);
        if (mesh == null) {
            mesh = Sphere.createMesh(div, r);
            sphereCache.put(key, mesh);
        } else {
            mesh.incRef();
        }
        return mesh;
    }

    synchronized TriangleMesh getCylinderMesh(float h, float r, int div, Key key) {
        if (cylinderCache == null) {
            cylinderCache = CylinderCacheLoader.INSTANCE;
        }

        TriangleMesh mesh = cylinderCache.get(key);
        if (mesh == null) {
            mesh = Cylinder.createMesh(div, h, r);
            cylinderCache.put(key, mesh);
        } else {
            mesh.incRef();
        }
        return mesh;
    }

    synchronized void invalidateBoxMesh(Key key) {
        if (boxCache != null) {
            boxCache.invalidateMesh(key);
        }
    }

    synchronized void invalidateSphereMesh(Key key) {
        if (sphereCache != null) {
            sphereCache.invalidateMesh(key);
        }
    }

    synchronized void invalidateCylinderMesh(Key key) {
        if (cylinderCache != null) {
            cylinderCache.invalidateMesh(key);
        }
    }

    synchronized void dispose() {
        // just clearing references to them
        if (boxCache != null) {
            boxCache.clear();
        }
        if (sphereCache != null) {
            sphereCache.clear();
        }
        if (cylinderCache != null) {
            cylinderCache.clear();
        }
    }

    // for testing purpose
    synchronized void printStats() {
        if (boxCache != null) {
            boxCache.printStats("BoxCache");
        }

        if (sphereCache != null) {
            sphereCache.printStats("SphereCache");
        }

        if (cylinderCache != null) {
            cylinderCache.printStats("CylinderCache");
        }
    }

    /**
     * Note: The only user of this method is in unit test: PredefinedMeshManagerTest.
     */
    void test_clearCaches() {
        INSTANCE.dispose();
    }

    /**
     * Note: The only user of this method is in unit test: PredefinedMeshManagerTest.
     */
    int test_getBoxCacheSize() {
        return INSTANCE.boxCache.size();
    }

    /**
     * Note: The only user of this method is in unit test: PredefinedMeshManagerTest.
     */
    int test_getSphereCacheSize() {
        return INSTANCE.sphereCache.size();
    }

    /**
     * Note: The only user of this method is in unit test: PredefinedMeshManagerTest.
     */
    int test_getCylinderCacheSize() {
        return INSTANCE.cylinderCache.size();
    }

    private final static class BoxCacheLoader {

        // lazy & thread-safe instantiation
        private static final TriangleMeshCache INSTANCE = new TriangleMeshCache();
    }

    private final static class SphereCacheLoader {

        // lazy & thread-safe instantiation
        private static final TriangleMeshCache INSTANCE = new TriangleMeshCache();
    }

    private final static class CylinderCacheLoader {

        // lazy & thread-safe instantiation
        private static final TriangleMeshCache INSTANCE = new TriangleMeshCache();
    }

};
