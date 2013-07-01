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

package com.sun.javafx.pgstub;

import com.sun.javafx.sg.PGPhongMaterial;
import com.sun.javafx.sg.PGShape3D;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;

public class StubShape3D extends StubNode implements PGShape3D {

    private PGPhongMaterial material;
    private DrawMode drawMode;
    private CullFace cullFace;

    @Override
    public void setMaterial(PGPhongMaterial material) {
        this.material = material;
    }

    @Override
    public void setDrawMode(Object drawMode) {
        this.drawMode = (DrawMode) drawMode;
    }

    @Override
    public void setCullFace(Object cullFace) {
        this.cullFace = (CullFace) cullFace;
    }

    public PGPhongMaterial getMaterial() {
        return material;
    }

    public DrawMode getDrawMode() {
        return drawMode;
    }

    public CullFace getCullFace() {
        return cullFace;
    }
}
