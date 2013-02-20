/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.sg.PGPhongMaterial;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Base class for representing the material of a 3D surface.
 * 
 * @since JavaFX 8
 */
public abstract class Material {
    /*
     *     Material (including Shaders and Textures)
     Material is not Paint
     PhongMaterial maybe the first and only material in FX8 (see 3D conceptual implementation for details)
     Bump map: Normal Map and Height Map -- We may generate a Normal Map when given a Height Map
     Displacement map? Not in FX8 -- May do Parallex correction mapping to improve quality at performance cost
     Support auto generated Mipmap
     No plan to support Multi-texture
     */

    protected Material() {
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

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final BooleanProperty impl_dirtyProperty() {
        return dirty;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    abstract public void impl_updatePG();

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    abstract public PGPhongMaterial impl_getPGMaterial();
}
