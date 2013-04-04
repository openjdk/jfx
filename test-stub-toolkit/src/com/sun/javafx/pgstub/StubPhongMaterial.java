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

public class StubPhongMaterial extends StubShape3D implements PGPhongMaterial {
    private float specularPower;
    private Object diffuseColor;
    private Object specularColor;
    private Object diffuseMap;
    private Object specularMap;
    private Object bumpMap;
    private Object selfIlluminationMap;

    @Override
    public void setDiffuseColor(Object diffuseColor) {
        this.diffuseColor = diffuseColor;
    }

    @Override
    public void setSpecularColor(Object specularColor) {
        this.specularColor = specularColor;
    }

    @Override
    public void setSpecularPower(float specularPower) {
        this.specularPower = specularPower;
    }

    @Override
    public void setDiffuseMap(Object diffuseMap) {
        this.diffuseMap = diffuseMap;
    }

    @Override
    public void setSpecularMap(Object specularMap) {
        this.specularMap = specularMap;
    }

    @Override
    public void setBumpMap(Object bumpMap) {
        this.bumpMap = bumpMap;
    }

    @Override
    public void setSelfIlluminationMap(Object selfIlluminationMap) {
        this.selfIlluminationMap = selfIlluminationMap;
    }

    public Object getBumpMap() {
        return bumpMap;
    }

    public Object getDiffuseColor() {
        return diffuseColor;
    }

    public Object getDiffuseMap() {
        return diffuseMap;
    }

    public Object getSelfIlluminationMap() {
        return selfIlluminationMap;
    }

    public Object getSpecularColor() {
        return specularColor;
    }

    public Object getSpecularMap() {
        return specularMap;
    }

    public float getSpecularPower() {
        return specularPower;
    }

}
