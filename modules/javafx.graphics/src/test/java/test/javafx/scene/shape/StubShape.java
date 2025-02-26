/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.shape;

import com.sun.javafx.scene.shape.AbstractShape;
import com.sun.javafx.sg.prism.NGNode;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;

public class StubShape extends AbstractShape {
    static {
        StubShapeHelper.setStubShapeAccessor(new StubShapeHelper.StubShapeAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((StubShape) node).doCreatePeer();
            }

            @Override
            public com.sun.javafx.geom.Shape doConfigShape(Shape shape) {
                return ((StubShape) shape).doConfigShape();
            }
        });
    }

    {
        // To initialize the class helper at the begining each constructor of this class
        StubShapeHelper.initHelper(this);
    }

    public StubShape() {
        setStroke(Color.BLACK);
    }

    private NGNode doCreatePeer() {
        return new StubNGShape();
    }

    private com.sun.javafx.geom.Shape doConfigShape() {
        return new com.sun.javafx.geom.RoundRectangle2D(0, 0, 10, 10, 4, 4);
    }
}
