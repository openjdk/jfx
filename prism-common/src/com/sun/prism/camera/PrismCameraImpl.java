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
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import com.sun.javafx.tk.Toolkit;

/**
 * Abstract class that specifies the camera used to render a 3D scene.
 * The camera consists of a viewing transform, a projection transform,
 * and a viewport. These parameters are controlled by the concrete camera
 * subclasses.
 */
public abstract class PrismCameraImpl {

    private Affine3D worldTransform = new Affine3D();
    
    // View transform
    private Affine3D viewTx = new Affine3D();

    // Projection transform
    protected GeneralTransform3D projTx = new GeneralTransform3D();

    // Viewport -- note that, except in the case of GeneralCamera, this will
    // be set to the size of the panel.
    protected Rectangle viewport = new Rectangle(1, 1);
    
    protected double aspect;
    private double zNear = 0.1;
    private double zFar = 100.0;

    // Camera position in local coord.
    protected Vec3d localPosition = new Vec3d();

    // Camera position in world coord.    
    private Vec3d worldPosition = new Vec3d();

    // projViewTx is the product of projTx * viewTx * inverse of worldTransform
    protected boolean projViewTxDirty = true;
    private int cachedWidth;
    private int cachedHeight;
    private GeneralTransform3D projViewTx = new GeneralTransform3D();

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
        projViewTxDirty = true;
    }

    public double getFarClip() {
        return zFar;
    }

    public void setFarClip(float farClip) {
        this.zFar = farClip;
        projViewTxDirty = true;
    }

//    /**
//     * Sets the view transform, which is used as the view portion of
//     * the ModelView matrix. The specified transform is copied into this
//     * camera object. The input transform must not be null.
//     *
//     * @param t the new view transform.
//     */
//    protected void setViewTransform(Affine3D t) {
//        viewTx.set(t);
//    }
//
    /**
     * Gets the current view transform.
     * If {@code rv} is non-null, then this node's view transform
     * is copied into {@code rv}. Otherwise, a new {@code Affine3D}
     * is allocated. Either way, the copy of the transform is returned.
     *
     * @param rv the return value, or null.
     *
     * @return a copy of the current view transform.
     */
    public synchronized Affine3D getViewTransform(Affine3D rv) {
//        if (Toolkit.getToolkit().isFxUserThread()) {
//            System.err.println("PrismCameraImpl.getViewTransform(): Error! In FX User Thread");
//        }
        if (rv == null) {
            rv = new Affine3D();
        }
        rv.setTransform(viewTx);
        return rv;
    }

//
//    /**
//     * Sets the projection transform. This may be either a perspective or
//     * parallel (orthographic) transform. The specified transform is copied
//     * into this camera object. The input transform must not be null.
//     *
//     * @param t the new projection transform.
//     */
//    protected void setProjectionTransform(GeneralTransform3D t) {
//        projTx.set(t);
//    }
//
    /**
     * Gets the current projection transform.
     * If {@code rv} is non-null, then this node's projection transform
     * is copied into {@code rv}. Otherwise, a new {@code Transform3D}
     * is allocated. Either way, the copy of the transform is returned.
     *
     * @param rv the return value, or null.
     *
     * @return a copy of the current projection transform.
     */
    public synchronized GeneralTransform3D getProjectionTransform(GeneralTransform3D rv) {
//        if (Toolkit.getToolkit().isFxUserThread()) {
//            System.err.println("PrismCameraImpl.getProjectionTransform(): Error! In FX User Thread");
//        }
        if (rv == null) {
            rv = new GeneralTransform3D();
        }
        rv.set(projTx);
        return rv;
    }
//
//    protected void setViewport(Rectangle viewport) {
//        this.viewport.setRect(viewport);
//    }
//
    public Rectangle getViewport(Rectangle rv) {
        if (rv == null) {
            rv = new Rectangle();
        }
        rv.setBounds(viewport);
        return rv;
    }

    public synchronized GeneralTransform3D getScreenProjViewTx(GeneralTransform3D tx, double w, double h) {
        validate((int) w, (int) h);
        return getProjViewTx(tx);
    }

    public synchronized GeneralTransform3D getProjViewTx(GeneralTransform3D tx) {
        if (tx == null) {
            tx = new GeneralTransform3D();
        }
        return tx.set(projViewTx);        
    }

    public synchronized void validate(int w, int h) {
//        if (Toolkit.getToolkit().isFxUserThread()) {
//            System.err.println("PrismCameraImpl.validate(): Error! In FX User Thread");
//        }
        if (projViewTxDirty || (w != cachedWidth) || (h != cachedHeight)) {
//            System.err.println("compute projViewTx");
            aspect = (double) w / (double) h;
            viewport.setBounds(0, 0, w, h);
            computeProjection(projTx);
            computeViewTransform(viewTx);
            projViewTx.set(projTx);
            projViewTx.mul(viewTx);

            // Set the inverse world transform
            Affine3D camWToLTx = new Affine3D(worldTransform);
            try {
                camWToLTx.invert();
            } catch (NoninvertibleTransformException ex) {
                ex.printStackTrace();
            }
            projViewTx.mul(camWToLTx);

            // transform camera position to world coord.
            worldTransform.transform(localPosition, worldPosition);

            // cache w and h
            cachedWidth = w;
            cachedHeight = h;
            projViewTxDirty = false;
        }
    }

    public Vec3d getPositionInWorld(Vec3d pos) {
        if (pos == null) {
            pos = new Vec3d();
        }
        pos.set(worldPosition);
        return pos;
    }
    
    protected abstract void computeProjection(GeneralTransform3D proj);
    protected abstract void computeViewTransform(Affine3D view);
    public abstract PickRay computePickRay(float x, float y, PickRay pickRay);

    public void setWorldTransform(Affine3D localToWorldTx) {
        worldTransform.setTransform(localToWorldTx);
        projViewTxDirty = true;
    }

}
