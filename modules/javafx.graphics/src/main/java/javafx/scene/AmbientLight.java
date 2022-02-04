/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.AmbientLightHelper;
import com.sun.javafx.sg.prism.NGAmbientLight;
import com.sun.javafx.sg.prism.NGNode;
import javafx.scene.paint.Color;

/**
 * A light that illuminates an object from all directions equally regardless of its position and orientation. An
 * {@code AmbientLight} adds a constant term to the amount of light reflected by each point on the surface of an object,
 * thereby increasing the brightness of the object uniformly.
 * <p>
 * {@code AmbientLight}s are often used to represent the base amount of illumination in a scene. In the real world,
 * light gets reflected off of surfaces, causing areas that are not in direct line-of-sight of the light to be lit (more
 * dimly). Using a dark colored (weak) {@code AmbientLight} can achieve the effect of the lighting of those areas.
 *
 * @since JavaFX 8.0
 */
public class AmbientLight extends LightBase {
    static {
        AmbientLightHelper.setAmbientLightAccessor(new AmbientLightHelper.AmbientLightAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((AmbientLight) node).doCreatePeer();
            }
        });
    }

    {
        // To initialize the class helper at the beginning each constructor of this class
        AmbientLightHelper.initHelper(this);
    }

    /**
     * Creates a new instance of {@code AmbientLight} class with a default Color.WHITE light source.
     */
    public AmbientLight() {
        super();
    }

    /**
     * Creates a new instance of {@code AmbientLight} class using the specified color.
     *
     * @param color the color of the light source
     */
    public AmbientLight(Color color) {
        super(color);
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private NGNode doCreatePeer() {
        return new NGAmbientLight();
    }
}
