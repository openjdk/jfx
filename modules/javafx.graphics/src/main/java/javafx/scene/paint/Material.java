/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.paint;

import com.sun.javafx.scene.paint.MaterialHelper;
import com.sun.javafx.sg.prism.NGPhongMaterial;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import com.sun.javafx.logging.PlatformLogger;

/**
 * Base class for representing the material of a 3D surface.
 *
 * Note that this is a conditional feature. See
 * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
 * for more information.
 *
 * @since JavaFX 8.0
 */
public abstract class Material {

    static {
        // This is used by classes in different packages to get access to
        // private and package private methods.
        MaterialHelper.setMaterialAccessor(new MaterialHelper.MaterialAccessor() {

            @Override
            public BooleanProperty dirtyProperty(Material material) {
                return material.dirtyProperty();
            }

            @Override
            public void updatePG(Material material) {
                material.updatePG();
            }
            @Override
            public NGPhongMaterial getNGMaterial(Material material) {
                return material.getNGMaterial();
            }

        });
    }

    /*
     *     Material (including Shaders and Textures)
     Material is not Paint
     PhongMaterial maybe the first and only material in FX8 (see 3D conceptual implementation for details)
     Bump map: Normal Map and Height Map -- We may generate a Normal Map when given a Height Map
     Displacement map? Not in FX8 -- May do Parallex correction mapping to improve quality at performance cost
     Support auto generated Mipmap
     No plan to support Multi-texture
     */

    /**
     * Constructor for subclasses to call.
     */
    protected Material() {
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            String logname = Material.class.getName();
            PlatformLogger.getLogger(logname).warning("System can't support "
                                                      + "ConditionalFeature.SCENE3D");
        }
    }

    // Material isn't a Node. It can't use the standard dirtyBits pattern that is
    // in Node
    private final BooleanProperty dirty = new SimpleBooleanProperty(true);

    final boolean isDirty() {
        return dirty.getValue();
    }

    void setDirty(boolean value) {
        dirty.setValue(value);
    }

    final BooleanProperty dirtyProperty() {
        return dirty;
    }

    abstract void updatePG();

    abstract NGPhongMaterial getNGMaterial();
}
