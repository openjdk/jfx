/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.DirectionalLightHelper;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.sg.prism.NGDirectionalLight;
import com.sun.javafx.sg.prism.NGNode;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * A light that illuminates an object from a specific direction. The <a href="LightBase.html#Direction">direction</a>
 * is defined by the {@link #directionProperty() direction} vector property.
 * <p>
 * {@code DirectionalLight}s can represent strong light sources that are far enough from the objects they illuminate
 * that their light rays appear to be parallel. Because these light sources are considered to be infinitely far, they
 * cannot be attenuated. The sun is a common light source that can be simulated with this light type.
 *
 * @since 18
 * @see PhongMaterial
 */
public class DirectionalLight extends LightBase {
    static {
        DirectionalLightHelper.setDirectionalLightAccessor(new DirectionalLightHelper.DirectionalLightAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((DirectionalLight) node).doCreatePeer();
            }

            @Override
            public void doUpdatePeer(Node node) {
                ((DirectionalLight) node).doUpdatePeer();
            }
        });
    }

    {
        // To initialize the class helper at the beginning of each constructor of this class
        DirectionalLightHelper.initHelper(this);
    }

    /**
     * Creates a new {@code DirectionalLight} with a default {@code Color.WHITE} color.
     */
    public DirectionalLight() {
    }

    /**
     * Creates a new {@code DirectionalLight} with the specified color.
     *
     * @param color the color of the light source
     */
    public DirectionalLight(Color color) {
        super(color);
    }


    /**
     * The direction vector of the directional light. It can be rotated by setting a rotation transform on the
     * {@code DirectionalLight}. The vector need not be normalized.
     *
     * @defaultValue {@code Point3D(0, 0, 1)}
     */
    private ObjectProperty<Point3D> direction;

    public final void setDirection(Point3D value) {
        directionProperty().set(value);
    }

    private static final Point3D DEFAULT_DIRECTION = NGDirectionalLight.getDefaultDirection();

    public final Point3D getDirection() {
        return direction == null ? DEFAULT_DIRECTION : direction.get();
    }

    public final ObjectProperty<Point3D> directionProperty() {
        if (direction == null) {
            direction = new SimpleObjectProperty<>(this, "direction", DEFAULT_DIRECTION) {
                @Override
                protected void invalidated() {
                    NodeHelper.markDirty(DirectionalLight.this, DirtyBits.NODE_LIGHT);
                }
            };
        }
        return direction;
    }


    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private NGNode doCreatePeer() {
        return new NGDirectionalLight();
    }

    private void doUpdatePeer() {
        if (isDirty(DirtyBits.NODE_LIGHT)) {
            NGDirectionalLight peer = getPeer();
            peer.setDirection(getDirection());
        }
    }
}
