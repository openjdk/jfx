/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.transform;

import java.util.Iterator;

import javafx.scene.Node;

import com.sun.javafx.WeakReferenceQueue;
import com.sun.javafx.geom.transform.Affine3D;

/**
 * This class provides functions to perform rotating, scaling, shearing,
 * and translation transformations for {@code Affine} objects.
 *
 * <p>Example:</p>
 *
 * <pre><code>
 *  Rectangle rect = new Rectangle(50,50, Color.RED);
 *  rect.getTransforms().add(new Rotate(45,0,0)); //rotate by 45 degrees
 * </code></pre>
 */
public abstract class Transform {

    /**
     * Returns a new {@code Affine} object from 12 number
     * values representing the 6 specifiable entries of the 3x4
     * Affine transformation matrix.
     *
     * @param mxx the X coordinate scaling element of the 3x4 matrix
     * @param myx the Y coordinate shearing element of the 3x4 matrix
     * @param mxy the X coordinate shearing element of the 3x4 matrix
     * @param myy the Y coordinate scaling element of the 3x4 matrix
     * @param tx the X coordinate translation element of the 3x4 matrix
     * @param ty the Y coordinate translation element of the 3x4 matrix
     * @return a new {@code Affine} object derived from specified parameters
     */
    public static Affine affine(
        double mxx, double myx, double mxy, double myy, double tx, double ty) {
        final Affine affine = new Affine();
        affine.setMxx(mxx);
        affine.setMxy(mxy);
        affine.setTx(tx);
        affine.setMyx(myx);
        affine.setMyy(myy);
        affine.setTy(ty);
        return affine;
    }


    /**
     * Returns a new {@code Affine} object from 12 number
     * values representing the 12 specifiable entries of the 3x4
     * Affine transformation matrix.
     *
     * @param mxx the X coordinate scaling element of the 3x4 matrix
     * @param mxy the XY element of the 3x4 matrix
     * @param mxz the XZ element of the 3x4 matrix
     * @param tx the X coordinate translation element of the 3x4 matrix
     * @param myx the YX element of the 3x4 matrix
     * @param myy the Y coordinate scaling element of the 3x4 matrix
     * @param myz the YZ element of the 3x4 matrix
     * @param ty the Y coordinate translation element of the 3x4 matrix
     * @param mzx the ZX element of the 3x4 matrix
     * @param mzy the ZY element of the 3x4 matrix
     * @param mzz the Z coordinate scaling element of the 3x4 matrix
     * @param tz the Z coordinate translation element of the 3x4 matrix
     * @return a new {@code Affine} object derived from specified parameters
     *
     * @since JavaFX 1.3
     */
    public static Affine affine(
        double mxx, double mxy, double mxz, double tx,
        double myx, double myy, double myz, double ty,
        double mzx, double mzy, double mzz, double tz) {
        final Affine affine = new Affine();
        affine.setMxx(mxx);
        affine.setMxy(mxy);
        affine.setMxz(mxz);
        affine.setTx(tx);
        affine.setMyx(myx);
        affine.setMyy(myy);
        affine.setMyz(myz);
        affine.setTy(ty);
        affine.setMzx(mzx);
        affine.setMzy(mzy);
        affine.setMzz(mzz);
        affine.setTz(tz);
        return affine;
    }


    /**
     * Returns a {@code Translate} object representing a translation transformation.
     * <p>
     * This is equivalent to:
     * <pre>
     *    new Translate(x, y);
     * </pre>
     */
    public static Translate translate(double x, double y) {
        final Translate translate = new Translate();
        translate.setX(x);
        translate.setY(y);
        return translate;
    }


    /**
     * Returns a {@code Rotate} object that rotates coordinates around a pivot
     * point.
     * <p>
     * This is equivalent to:
     * <pre>
     *    new Rotate(angle, pivotX, pivotY);
     * </pre>
     */
    public static Rotate rotate(double angle, double pivotX, double pivotY) {
        final Rotate rotate = new Rotate();
        rotate.setAngle(angle);
        rotate.setPivotX(pivotX);
        rotate.setPivotY(pivotY);
        return rotate;
    }


    /**
     * Returns a {@code Scale} object representing a scaling transformation.
     * <p>
     * This is equivalent to:
     * <pre>
     *    new Scale(x, y);
     * </pre>
     */
    public static Scale scale(double x, double y) {
        final Scale scale = new Scale();
        scale.setX(x);
        scale.setY(y);
        return scale;
    }


    /**
     * Returns a {@code Scale} object representing a scaling transformation.
     * The returned scale operation will be about the given pivot point.
     * <p>
     * This is equivalent to:
     * <pre>
     *    new Scale(x, y, pivotX, pivotY);
     * </pre>
     */
    public static Scale scale(double x, double y, double pivotX, double pivotY) {
        final Scale scale = new Scale();
        scale.setX(x);
        scale.setY(y);
        scale.setPivotX(pivotX);
        scale.setPivotY(pivotY);
        return scale;
    }


    /**
     * Returns a {@code Shear} object representing a shearing transformation.
     * <p>
     * This is equivalent to:
     * <pre>
     *    new Shear(x, y);
     * </pre>
     */
    public static Shear shear(double x, double y) {
        final Shear shear = new Shear();
        shear.setX(x);
        shear.setY(y);
        return shear;
    }

    /**
     * Returns a {@code Shear} object representing a shearing transformation.
     * <p>
     * This is equivalent to:
     * <pre>
     *    new Shear(x, y, pivotX, pivotY);
     * </pre>
     */
    public static Shear shear(double x, double y, double pivotX, double pivotY) {
        final Shear shear = new Shear();
        shear.setX(x);
        shear.setY(y);
        shear.setPivotX(pivotX);
        shear.setPivotY(pivotY);
        return shear;
    }

    private WeakReferenceQueue impl_nodes = new WeakReferenceQueue();


    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public abstract void impl_apply(Affine3D t);

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public abstract Transform impl_copy();

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public void impl_add(final Node node) {
        impl_nodes.add(node);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public void impl_remove(final Node node) {
        impl_nodes.remove(node);
    }

    void transformChanged() {
        final Iterator iterator = impl_nodes.iterator();
        while (iterator.hasNext()) {
            ((Node) iterator.next()).impl_transformsChanged();
        }
    }
}
