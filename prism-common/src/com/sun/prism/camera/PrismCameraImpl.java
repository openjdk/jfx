/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.camera;

import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.GeneralTransform3D;

/**
 * Abstract class that specifies the camera used to render a 3D scene.
 * The camera consists of a viewing transform, a projection transform,
 * and a viewport. These parameters are controlled by the concrete camera
 * subclasses.
 */
public abstract class PrismCameraImpl {

    protected Affine3D worldTransform = new Affine3D();

    // Viewport -- note that, except in the case of GeneralCamera, this will
    // be set to the size of the panel.
    protected double viewWidth = 1.0;
    protected double viewHeight = 1.0;
    
    private double zNear = 0.1;
    private double zFar = 100.0;

    // Camera position in world coord.
    private Vec3d worldPosition = new Vec3d();

    protected GeneralTransform3D projViewTx = new GeneralTransform3D();

    /**
     * Constructs a camera object with default parameters.
     */
    protected PrismCameraImpl() {
    }

    public double getNearClip() {
        return zNear;
    }

    public void setNearClip(float nearClip) {
        this.zNear = nearClip;
    }

    public double getFarClip() {
        return zFar;
    }

    public void setFarClip(float farClip) {
        this.zFar = farClip;
    }

    public void setViewWidth(double viewWidth) {
        this.viewWidth = viewWidth;
    }

    public void setViewHeight(double viewHeight) {
        this.viewHeight = viewHeight;
    }

    public double getViewWidth() {
        return viewWidth;
    }

    public double getViewHeight() {
        return viewHeight;
    }

    public void setProjViewTransform(GeneralTransform3D projViewTx) {
        this.projViewTx.set(projViewTx);
    }

    public void setPosition(Vec3d position) {
        worldPosition.set(position);
    }

    public GeneralTransform3D getProjViewTx(GeneralTransform3D tx) {
        if (tx == null) {
            tx = new GeneralTransform3D();
        }
        return tx.set(projViewTx);
    }

    public Vec3d getPositionInWorld(Vec3d pos) {
        if (pos == null) {
            pos = new Vec3d();
        }
        pos.set(worldPosition);
        return pos;
    }
    
    public abstract PickRay computePickRay(float x, float y, PickRay pickRay);

    public void setWorldTransform(Affine3D localToWorldTx) {
        worldTransform.setTransform(localToWorldTx);
    }

}
