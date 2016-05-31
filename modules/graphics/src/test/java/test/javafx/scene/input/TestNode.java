/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.input;

import test.com.sun.javafx.scene.input.TestNodeHelper;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.sg.prism.NGGroup;
import com.sun.javafx.sg.prism.NGNode;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Node;

/**
 * Subclass of javafx.scene.Node used for input testing.
 */
public class TestNode extends Node {
    static {
         // This is used by classes in different packages to get access to
         // private and package private methods.
        TestNodeHelper.setTestNodeAccessor(new TestNodeHelper.TestNodeAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((TestNode) node).doCreatePeer();
            }
        });
    }

    private float offsetInScene;

    {
        // To initialize the class helper at the begining each constructor of this class
        TestNodeHelper.initHelper(this);
    }
    public TestNode() {
    }

    public TestNode(float offsetInScene) {
        this.offsetInScene = offsetInScene;
    }

    @Override
    public Point2D sceneToLocal(double x, double y) {
        return new Point2D(x - offsetInScene, y - offsetInScene);
    }

    @Override
    public Point2D localToScene(double x, double y) {
        return new Point2D(x + offsetInScene, y + offsetInScene);
    }

    @Override
    public Point3D sceneToLocal(double x, double y, double z) {
        return new Point3D(x - offsetInScene, y - offsetInScene, z);
    }

    @Override
    public Point3D localToScene(double x, double y, double z) {
        return new Point3D(x + offsetInScene, y + offsetInScene, z);
    }

    @Override
    protected boolean impl_computeContains(double f, double f1) {
        return false;
    }

    @Override
    public BaseBounds impl_computeGeomBounds(BaseBounds bd, BaseTransform bt) {
        return null;
    }

    private NGNode doCreatePeer() {
        return new NGGroup();
    }

    @Override
    public Object impl_processMXNode(MXNodeAlgorithm alg, MXNodeAlgorithmContext ctx) {
        return null;
    }
}
