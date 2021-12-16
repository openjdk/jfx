/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geometry.BoundsUtils;
import javafx.event.EventDispatchChain;

import javafx.scene.Node;

import com.sun.javafx.util.WeakReferenceQueue;
import com.sun.javafx.binding.ExpressionHelper;
import com.sun.javafx.event.EventHandlerManager;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.transform.TransformHelper;
import com.sun.javafx.scene.transform.TransformUtils;
import java.lang.ref.SoftReference;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;

// PENDING_DOC_REVIEW of this whole class
/**
 * This class is a base class for different affine transformations.
 * It provides factory methods for the simple transformations - rotating,
 * scaling, shearing, and translation. It allows to get the transformation
 * matrix elements for any transform.
 *
 * <p>Example:</p>
 *
 * <pre>{@code
 *  Rectangle rect = new Rectangle(50,50, Color.RED);
 *  rect.getTransforms().add(new Rotate(45,0,0)); //rotate by 45 degrees
 * }</pre>
 * @since JavaFX 2.0
 */
public abstract class Transform implements Cloneable, EventTarget {

    static {
        // This is used by classes in different packages to get access to
        // private and package private methods.
        TransformHelper.setTransformAccessor(new TransformHelper.TransformAccessor() {

            @Override
            public void add(Transform transform, Node node) {
                transform.add(node);
            }

            @Override
            public void remove(Transform transform, Node node) {
                transform.remove(node);
            }

            @Override
            public void apply(Transform transform, Affine3D affine3D) {
                transform.apply(affine3D);
            }

            @Override
            public BaseTransform derive(Transform transform, BaseTransform baseTransform) {
                return transform.derive(baseTransform);
            }

            @Override
            public Transform createImmutableTransform() {
                return Transform.createImmutableTransform();
            }

            @Override
            public Transform createImmutableTransform(
                    double mxx, double mxy, double mxz, double tx,
                    double myx, double myy, double myz, double ty,
                    double mzx, double mzy, double mzz, double tz) {
                return Transform.createImmutableTransform(mxx, mxy, mxz, tx,
                        myx, myy, myz, ty, mzx, mzy, mzz, tz);
            }

            @Override
            public Transform createImmutableTransform(Transform transform,
                    double mxx, double mxy, double mxz, double tx,
                    double myx, double myy, double myz, double ty,
                    double mzx, double mzy, double mzz, double tz) {
                return Transform.createImmutableTransform(transform,
                        mxx, mxy, mxz, tx, myx, myy, myz, ty, mzx, mzy, mzz, tz);
            }

            @Override
            public Transform createImmutableTransform(Transform transform,
                    Transform left, Transform right) {
                return Transform.createImmutableTransform(transform, left, right);
            }
        });
    }

    /**
     * Constructor for subclasses to call.
     */
    public Transform() {
    }

    /* *************************************************************************
     *                                                                         *
     *                            Factories                                    *
     *                                                                         *
     **************************************************************************/

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
     * @param x the translate x value
     * @param y the translate y value
     * @return the Translate object representing a translation transformation
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
     * @param angle the rotation angle
     * @param pivotX the pivot x value
     * @param pivotY the pivot y value
     * @return the Rotate object that rotates coordinates around a pivot point
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
     * @param x the scale x value
     * @param y the scale y value
     * @return the Scale object representing a scaling transformation
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
     * @param x the scale x value
     * @param y the scale y value
     * @param pivotX the pivot x value
     * @param pivotY the pivot y value
     * @return the Scale object representing a scaling transformation
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
     * @param x the shear x value
     * @param y the shear y value
     * @return the Shear object representing a shearing transformation
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
     * @param x the shear x value
     * @param y the shear y value
     * @param pivotX the pivot x value
     * @param pivotY the pivot y value
     * @return the Shear object representing a shearing transformation
     */
    public static Shear shear(double x, double y, double pivotX, double pivotY) {
        final Shear shear = new Shear();
        shear.setX(x);
        shear.setY(y);
        shear.setPivotX(pivotX);
        shear.setPivotY(pivotY);
        return shear;
    }

    /**
     * For transforms with expensive inversion we cache the inverted matrix
     * once it is needed and computed for some operation.
     */
    private SoftReference<Transform> inverseCache = null;

    private WeakReferenceQueue nodes = new WeakReferenceQueue();

    /* *************************************************************************
     *                                                                         *
     *                         Element getters                                 *
     *                                                                         *
     **************************************************************************/

    /**
     * Gets the X coordinate scaling element of the 3x4 matrix.
     *
     * @return the X coordinate scaling element of the 3x4 matrix
     * @since JavaFX 2.2
     */
    public  double getMxx() {
        return 1.0;
    }

    /**
     * Gets the XY coordinate element of the 3x4 matrix.
     *
     * @return the XY coordinate element of the 3x4 matrix
     * @since JavaFX 2.2
     */
    public  double getMxy() {
        return 0.0;
    }

    /**
     * Gets the XZ coordinate element of the 3x4 matrix.
     *
     * @return the XZ coordinate element of the 3x4 matrix
     * @since JavaFX 2.2
     */
    public  double getMxz() {
        return 0.0;
    }

    /**
     * Gets the X coordinate translation element of the 3x4 matrix.
     *
     * @return the X coordinate translation element of the 3x4 matrix
     * @since JavaFX 2.2
     */
    public  double getTx() {
        return 0.0;
    }

    /**
     * Gets the YX coordinate element of the 3x4 matrix.
     *
     * @return the YX coordinate element of the 3x4 matrix
     * @since JavaFX 2.2
     */
    public  double getMyx() {
        return 0.0;
    }

    /**
     * Gets the Y coordinate scaling element of the 3x4 matrix.
     *
     * @return the Y coordinate scaling element of the 3x4 matrix
     * @since JavaFX 2.2
     */
    public  double getMyy() {
        return 1.0;
    }

    /**
     * Gets the YZ coordinate element of the 3x4 matrix.
     *
     * @return the YZ coordinate element of the 3x4 matrix
     * @since JavaFX 2.2
     */
    public  double getMyz() {
        return 0.0;
    }

    /**
     * Gets the Y coordinate translation element of the 3x4 matrix.
     *
     * @return the Y coordinate translation element of the 3x4 matrix
     * @since JavaFX 2.2
     */
    public  double getTy() {
        return 0.0;
    }

    /**
     * Gets the ZX coordinate element of the 3x4 matrix.
     *
     * @return the ZX coordinate element of the 3x4 matrix
     * @since JavaFX 2.2
     */
    public  double getMzx() {
        return 0.0;
    }

    /**
     * Gets the ZY coordinate element of the 3x4 matrix.
     *
     * @return the ZY coordinate element of the 3x4 matrix
     * @since JavaFX 2.2
     */
    public  double getMzy() {
        return 0.0;
    }

    /**
     * Gets the Z coordinate scaling element of the 3x4 matrix.
     *
     * @return the Z coordinate scaling element of the 3x4 matrix
     * @since JavaFX 2.2
     */
    public  double getMzz() {
        return 1.0;
    }

    /**
     * Gets the Z coordinate translation element of the 3x4 matrix.
     *
     * @return the Z coordinate translation element of the 3x4 matrix
     * @since JavaFX 2.2
     */
    public  double getTz() {
        return 0.0;
    }

    /**
     * Gets the specified element of the transformation matrix.
     * @param type type of matrix to get the value from
     * @param row zero-based row number
     * @param column zero-based column number
     * @return value of the specified transformation matrix element
     * @throws IllegalArgumentException if a 2D matrix type is requested for
     *         a 3D transform
     * @throws IndexOutOfBoundsException if the indices are not within
     *         the specified matrix type
     * @throws NullPointerException if the specified {@code type} is null
     * @since JavaFX 8.0
     */
    public double getElement(MatrixType type, int row, int column) {
        if (row < 0 || row >= type.rows() || column < 0 || column >= type.columns()) {
            throw new IndexOutOfBoundsException("Index outside of affine "
                    + "matrix " + type + ": [" + row + ", " + column + "]");
        }
        switch(type) {
            case MT_2D_2x3:
                // fall-through
            case MT_2D_3x3:
                if (!isType2D()) {
                    throw new IllegalArgumentException("Cannot access 2D matrix "
                            + "of a 3D transform");
                }
                switch(row) {
                    case 0:
                        switch(column) {
                            case 0: return getMxx();
                            case 1: return getMxy();
                            case 2: return getTx();
                        }
                    case 1:
                        switch(column) {
                            case 0: return getMyx();
                            case 1: return getMyy();
                            case 2: return getTy();
                        }
                    case 2:
                        switch(column) {
                            case 0: return 0.0;
                            case 1: return 0.0;
                            case 2: return 1.0;
                        }
                }
                break;
            case MT_3D_3x4:
                // fall-through
            case MT_3D_4x4:
                switch(row) {
                    case 0:
                        switch(column) {
                            case 0: return getMxx();
                            case 1: return getMxy();
                            case 2: return getMxz();
                            case 3: return getTx();
                        }
                    case 1:
                        switch(column) {
                            case 0: return getMyx();
                            case 1: return getMyy();
                            case 2: return getMyz();
                            case 3: return getTy();
                        }
                    case 2:
                        switch(column) {
                            case 0: return getMzx();
                            case 1: return getMzy();
                            case 2: return getMzz();
                            case 3: return getTz();
                        }
                    case 3:
                        switch(column) {
                            case 0: return 0.0;
                            case 1: return 0.0;
                            case 2: return 0.0;
                            case 3: return 1.0;
                        }
                }
                break;
        }
        // cannot reach here
        throw new InternalError("Unsupported matrix type " + type);
    }

    /* *************************************************************************
     *                                                                         *
     *                           State getters                                 *
     *                                                                         *
     **************************************************************************/

    /**
     * Computes if this transform is currently a 2D transform (has no effect
     * in the direction of Z axis).
     * Used by the subclasses to effectively provide value of the type2D
     * property.
     * @return true if this transform is currently 2D-only
     */
    boolean computeIs2D() {
        return getMxz() == 0.0 && getMzx() == 0.0 && getMzy() == 0.0 &&
                    getMzz() == 1.0 && getTz() == 0.0;
    }

    /**
     * Computes if this transform is currently an identity (has
     * no effect in any direction).
     * Used by the subclasses to effectively provide value of the identity
     * property.
     * @return true if this transform is currently an identity transform
     */
    boolean computeIsIdentity() {
        return
            getMxx() == 1.0 && getMxy() == 0.0 && getMxz() == 0.0 && getTx() == 0.0 &&
            getMyx() == 0.0 && getMyy() == 1.0 && getMyz() == 0.0 && getTy() == 0.0 &&
            getMzx() == 0.0 && getMzy() == 0.0 && getMzz() == 1.0 && getTz() == 0.0;
    }

    /**
     * Computes determinant of the transformation matrix.
     * Among other things, determinant can be used for testing this transform's
     * invertibility - it is invertible if determinant is not equal to zero.
     * @return Determinant of the transformation matrix
     * @since JavaFX 8.0
     */
    public double determinant() {
        final double myx = getMyx();
        final double myy = getMyy();
        final double myz = getMyz();
        final double mzx = getMzx();
        final double mzy = getMzy();
        final double mzz = getMzz();

        return (getMxx() * (myy * mzz - mzy * myz) +
                getMxy() * (myz * mzx - mzz * myx) +
                getMxz() * (myx * mzy - mzx * myy));
    }

    /**
     * Determines if this is currently a 2D transform.
     * Transform is 2D if it has no effect along the Z axis.
     * @since JavaFX 8.0
     */
    private LazyBooleanProperty type2D;

    public final boolean isType2D() {
        return type2D == null ? computeIs2D() : type2D.get();
    }

    public final ReadOnlyBooleanProperty type2DProperty() {
        if (type2D == null) {
            type2D = new LazyBooleanProperty() {

                @Override
                protected boolean computeValue() {
                    return computeIs2D();
                }

                @Override
                public Object getBean() {
                    return Transform.this;
                }

                @Override
                public String getName() {
                    return "type2D";
                }
            };
        }
        return type2D;
    }

    /**
     * Determines if this is currently an identity transform.
     * Identity transform has no effect on the transformed nodes.
     * @since JavaFX 8.0
     */
    private LazyBooleanProperty identity;

    public final boolean isIdentity() {
        return identity == null ? computeIsIdentity() : identity.get();
    }

    public final ReadOnlyBooleanProperty identityProperty() {
        if (identity == null) {
            identity = new LazyBooleanProperty() {

                @Override
                protected boolean computeValue() {
                    return computeIsIdentity();
                }

                @Override
                public Object getBean() {
                    return Transform.this;
                }

                @Override
                public String getName() {
                    return "identity";
                }
            };
        }
        return identity;
    }

    /**
     * Lazily computed read-only boolean property implementation.
     * Used for type2D and identity properties.
     */
    private static abstract class LazyBooleanProperty
            extends ReadOnlyBooleanProperty {

        private ExpressionHelper<Boolean> helper;
        private boolean valid;
        private boolean value;

        @Override
        public void addListener(InvalidationListener listener) {
            helper = ExpressionHelper.addListener(helper, this, listener);
        }

        @Override
        public void removeListener(InvalidationListener listener) {
            helper = ExpressionHelper.removeListener(helper, listener);
        }

        @Override
        public void addListener(ChangeListener<? super Boolean> listener) {
            helper = ExpressionHelper.addListener(helper, this, listener);
        }

        @Override
        public void removeListener(ChangeListener<? super Boolean> listener) {
            helper = ExpressionHelper.removeListener(helper, listener);
        }

        @Override
        public boolean get() {
            if (!valid) {
                value = computeValue();
                valid = true;
            }

            return value;
        }

        public void invalidate() {
            if (valid) {
                valid = false;
                ExpressionHelper.fireValueChangedEvent(helper);
            }
        }

        protected abstract boolean computeValue();
    }

    /**
     * Transforms the specified point by this transform and by the specified
     * transform and returns distance of the result points. Used for similarTo
     * method. Has to be used only for 2D transforms (otherwise throws an
     * exception).
     * @param t the other transform
     * @param x point's X coordinate
     * @param y point's Y coordinate
     * @return distance of the transformed points
     */
    private double transformDiff(Transform t, double x, double y) {
        final Point2D byThis = transform(x, y);
        final Point2D byOther = t.transform(x, y);
        return byThis.distance(byOther);
    }

    /**
     * Transforms the specified point by this transform and by the specified
     * transform and returns distance of the result points. Used for similarTo
     * method.
     * @param t the other transform
     * @param x point's X coordinate
     * @param y point's Y coordinate
     * @param z point's Z coordinate
     * @return distance of the transformed points
     */
    private double transformDiff(Transform t, double x, double y, double z) {
        final Point3D byThis = transform(x, y, z);
        final Point3D byOther = t.transform(x, y, z);
        return byThis.distance(byOther);
    }

    /**
     * Checks if this transform is similar to the specified transform.
     * The two transforms are considered similar if any point from
     * {@code range} is transformed by them to points that are no farther
     * than {@code maxDelta} from each other.
     * @param transform transform to be compared to this transform
     * @param range region of interest on which the two transforms are compared
     * @param maxDelta maximum allowed distance for the results of transforming
     *                 any single point from {@code range} by the two transforms
     * @return true if the transforms are similar according to the specified
     *              criteria
     * @throws NullPointerException if the specified {@code transform}
     *         or {@code range} is null
     * @since JavaFX 8.0
     */
    public boolean similarTo(Transform transform, Bounds range, double maxDelta) {

        double cornerX, cornerY, cornerZ;

        if (isType2D() && transform.isType2D()) {
            cornerX = range.getMinX();
            cornerY = range.getMinY();

            if (transformDiff(transform, cornerX, cornerY) > maxDelta) {
                return false;
            }

            cornerY = range.getMaxY();
            if (transformDiff(transform, cornerX, cornerY) > maxDelta) {
                return false;
            }

            cornerX = range.getMaxX();
            cornerY = range.getMinY();
            if (transformDiff(transform, cornerX, cornerY) > maxDelta) {
                return false;
            }

            cornerY = range.getMaxY();
            if (transformDiff(transform, cornerX, cornerY) > maxDelta) {
                return false;
            }

            return true;
        }

        cornerX = range.getMinX();
        cornerY = range.getMinY();
        cornerZ = range.getMinZ();
        if (transformDiff(transform, cornerX, cornerY, cornerZ) > maxDelta) {
            return false;
        }

        cornerY = range.getMaxY();
        if (transformDiff(transform, cornerX, cornerY, cornerZ) > maxDelta) {
            return false;
        }

        cornerX = range.getMaxX();
        cornerY = range.getMinY();
        if (transformDiff(transform, cornerX, cornerY, cornerZ) > maxDelta) {
            return false;
        }

        cornerY = range.getMaxY();
        if (transformDiff(transform, cornerX, cornerY, cornerZ) > maxDelta) {
            return false;
        }

        if (range.getDepth() != 0.0) {
            cornerX = range.getMinX();
            cornerY = range.getMinY();
            cornerZ = range.getMaxZ();
            if (transformDiff(transform, cornerX, cornerY, cornerZ) > maxDelta) {
                return false;
            }

            cornerY = range.getMaxY();
            if (transformDiff(transform, cornerX, cornerY, cornerZ) > maxDelta) {
                return false;
            }

            cornerX = range.getMaxX();
            cornerY = range.getMinY();
            if (transformDiff(transform, cornerX, cornerY, cornerZ) > maxDelta) {
                return false;
            }

            cornerY = range.getMaxY();
            if (transformDiff(transform, cornerX, cornerY, cornerZ) > maxDelta) {
                return false;
            }
        }

        return true;
    }

    /* *************************************************************************
     *                                                                         *
     *                           Array getters                                 *
     *                                                                         *
     **************************************************************************/

    /**
     * Core of the toArray implementation for the 2D case.
     * All of the checks has been made by the enclosing method as well as
     * the constant elements filled, this method only fills the varying
     * elements to the array. Used by subclasses to fill
     * the elements efficiently.
     * @param array array to be filled with the 6 2D elements
     */
    void fill2DArray(double[] array) {
        array[0] = getMxx();
        array[1] = getMxy();
        array[2] = getTx();
        array[3] = getMyx();
        array[4] = getMyy();
        array[5] = getTy();
    }

    /**
     * Core of the toArray implementation for the 3D case.
     * All of the checks has been made by the enclosing method as well as
     * the constant elements filled, this method only fills the varying
     * elements to the array. Used by subclasses to fill
     * the elements efficiently.
     * @param array array to be filled with the 12 3D elements
     */
    void fill3DArray(double[] array) {
        array[0] = getMxx();
        array[1] = getMxy();
        array[2] = getMxz();
        array[3] = getTx();
        array[4] = getMyx();
        array[5] = getMyy();
        array[6] = getMyz();
        array[7] = getTy();
        array[8] = getMzx();
        array[9] = getMzy();
        array[10] = getMzz();
        array[11] = getTz();
    }

    /**
     * Returns an array containing the flattened transformation matrix.
     * If the requested matrix type fits in the specified array, it is returned
     * therein. Otherwise, a new array is created.
     * @param type matrix type to be filled in the array
     * @param array array into which the elements of the matrix are to be
     *              stored, if it is non-null and big enough; otherwise,
     *              a new array is created for this purpose.
     * @return an array containing the elements of the requested matrix type
     *         representing this transform
     * @throws IllegalArgumentException if a 2D matrix type is requested for
     *         a 3D transform
     * @throws NullPointerException if the specified {@code type} is null
     * @since JavaFX 8.0
     */
    public double[] toArray(MatrixType type, double[] array) {
        checkRequestedMAT(type);

        if (array == null || array.length < type.elements()) {
            array = new double[type.elements()];
        }

        switch (type) {
            case MT_2D_3x3:
                array[6] = 0.0;
                array[7] = 0.0;
                array[8] = 1.0;
                // fall-through
            case MT_2D_2x3:
                fill2DArray(array);
                break;
            case MT_3D_4x4:
                array[12] = 0.0;
                array[13] = 0.0;
                array[14] = 0.0;
                array[15] = 1.0;
                // fall-through
            case MT_3D_3x4:
                fill3DArray(array);
                break;
            default:
                throw new InternalError("Unsupported matrix type " + type);
        }

        return array;
    }

    /**
     * Returns an array containing the flattened transformation matrix.
     * @param type matrix type to be filled in the array
     * @return an array containing the elements of the requested matrix type
     *         representing this transform
     * @throws IllegalArgumentException if a 2D matrix type is requested for
     *         a 3D transform
     * @throws NullPointerException if the specified {@code type} is null
     * @since JavaFX 8.0
     */
    public double[] toArray(MatrixType type) {
        return toArray(type, null);
    }

    /**
     * Returns an array containing a row of the transformation matrix.
     * If the row of the requested matrix type fits in the specified array,
     * it is returned therein. Otherwise, a new array is created.
     * @param type matrix type whose row is to be filled in the array
     * @param row zero-based index of the row
     * @param array array into which the elements of the row are to be
     *              stored, if it is non-null and big enough; otherwise,
     *              a new array is created for this purpose.
     * @return an array containing the requested row of the requested matrix
     *         type representing this transform
     * @throws IllegalArgumentException if a 2D matrix type is requested for
     *         a 3D transform
     * @throws IndexOutOfBoundsException if the {@code row} index is not within
     *         the number of rows of the specified matrix type
     * @throws NullPointerException if the specified {@code type} is null
     * @since JavaFX 8.0
     */
    public double[] row(MatrixType type, int row, double[] array) {

        checkRequestedMAT(type);

        if (row < 0 || row >= type.rows()) {
            throw new IndexOutOfBoundsException(
                    "Cannot get row " + row + " from " + type);
        }

        if (array == null || array.length < type.columns()) {
            array = new double[type.columns()];
        }

        switch(type) {
            case MT_2D_2x3:
            case MT_2D_3x3:
                switch (row) {
                    case 0:
                        array[0] = getMxx();
                        array[1] = getMxy();
                        array[2] = getTx();
                        break;
                    case 1:
                        array[0] = getMyx();
                        array[1] = getMyy();
                        array[2] = getTy();
                        break;
                    case 2:
                        array[0] = 0.0;
                        array[1] = 0.0;
                        array[2] = 1.0;
                        break;
                }
                break;
            case MT_3D_3x4:
            case MT_3D_4x4:
                switch (row) {
                    case 0:
                        array[0] = getMxx();
                        array[1] = getMxy();
                        array[2] = getMxz();
                        array[3] = getTx();
                        break;
                    case 1:
                        array[0] = getMyx();
                        array[1] = getMyy();
                        array[2] = getMyz();
                        array[3] = getTy();
                        break;
                    case 2:
                        array[0] = getMzx();
                        array[1] = getMzy();
                        array[2] = getMzz();
                        array[3] = getTz();
                        break;
                    case 3:
                        array[0] = 0.0;
                        array[1] = 0.0;
                        array[2] = 0.0;
                        array[3] = 1.0;
                        break;
                }
                break;
            default:
                throw new InternalError("Unsupported row " + row + " of " + type);
        }
        return array;
    }

    /**
     * Returns an array containing a row of the transformation matrix.
     * @param type matrix type whose row is to be filled in the array
     * @param row zero-based index of the row
     * @return an array containing the requested row of the requested matrix
     *         type representing this transform
     * @throws IllegalArgumentException if a 2D matrix type is requested for
     *         a 3D transform
     * @throws IndexOutOfBoundsException if the {@code row} index is not within
     *         the number of rows of the specified matrix type
     * @throws NullPointerException if the specified {@code type} is null
     * @since JavaFX 8.0
     */
    public double[] row(MatrixType type, int row) {
        return row(type, row, null);
    }

    /**
     * Returns an array containing a column of the transformation matrix.
     * If the column of the requested matrix type fits in the specified array,
     * it is returned therein. Otherwise, a new array is created.
     * @param type matrix type whose column is to be filled in the array
     * @param column zero-based index of the column
     * @param array array into which the elements of the column are to be
     *              stored, if it is non-null and big enough; otherwise,
     *              a new array is created for this purpose.
     * @return an array containing the requested column of the requested matrix
     *         type representing this transform
     * @throws IllegalArgumentException if a 2D matrix type is requested for
     *         a 3D transform
     * @throws IndexOutOfBoundsException if the {@code column} index
     *         is not within the number of columns of the specified matrix type
     * @throws NullPointerException if the specified {@code type} is null
     * @since JavaFX 8.0
     */
    public double[] column(MatrixType type, int column, double[] array) {

        checkRequestedMAT(type);

        if (column < 0 || column >= type.columns()) {
            throw new IndexOutOfBoundsException(
                    "Cannot get row " + column + " from " + type);
        }

        if (array == null || array.length < type.rows()) {
            array = new double[type.rows()];
        }

        switch(type) {
            case MT_2D_2x3:
                switch (column) {
                    case 0:
                        array[0] = getMxx();
                        array[1] = getMyx();
                        break;
                    case 1:
                        array[0] = getMxy();
                        array[1] = getMyy();
                        break;
                    case 2:
                        array[0] = getTx();
                        array[1] = getTy();
                        break;
                }
                break;
            case MT_2D_3x3:
                switch (column) {
                    case 0:
                        array[0] = getMxx();
                        array[1] = getMyx();
                        array[2] = 0.0;
                        break;
                    case 1:
                        array[0] = getMxy();
                        array[1] = getMyy();
                        array[2] = 0.0;
                        break;
                    case 2:
                        array[0] = getTx();
                        array[1] = getTy();
                        array[2] = 1.0;
                        break;
                }
                break;
            case MT_3D_3x4:
                switch (column) {
                    case 0:
                        array[0] = getMxx();
                        array[1] = getMyx();
                        array[2] = getMzx();
                        break;
                    case 1:
                        array[0] = getMxy();
                        array[1] = getMyy();
                        array[2] = getMzy();
                        break;
                    case 2:
                        array[0] = getMxz();
                        array[1] = getMyz();
                        array[2] = getMzz();
                        break;
                    case 3:
                        array[0] = getTx();
                        array[1] = getTy();
                        array[2] = getTz();
                        break;
                }
                break;
            case MT_3D_4x4:
                switch (column) {
                    case 0:
                        array[0] = getMxx();
                        array[1] = getMyx();
                        array[2] = getMzx();
                        array[3] = 0.0;
                        break;
                    case 1:
                        array[0] = getMxy();
                        array[1] = getMyy();
                        array[2] = getMzy();
                        array[3] = 0.0;
                        break;
                    case 2:
                        array[0] = getMxz();
                        array[1] = getMyz();
                        array[2] = getMzz();
                        array[3] = 0.0;
                        break;
                    case 3:
                        array[0] = getTx();
                        array[1] = getTy();
                        array[2] = getTz();
                        array[3] = 1.0;
                        break;
                }
                break;
            default:
                throw new InternalError("Unsupported column " + column + " of "
                        + type);
        }
        return array;
    }

    /**
     * Returns an array containing a column of the transformation matrix.
     * @param type matrix type whose column is to be filled in the array
     * @param column zero-based index of the column
     * @return an array containing the requested column of the requested matrix
     *         type representing this transform
     * @throws IllegalArgumentException if a 2D matrix type is requested for
     *         a 3D transform
     * @throws IndexOutOfBoundsException if the {@code column} index
     *         is not within the number of columns of the specified matrix type
     * @throws NullPointerException if the specified {@code type} is null
     * @since JavaFX 8.0
     */
    public double[] column(MatrixType type, int column) {
        return column(type, column, null);
    }

    /* *************************************************************************
     *                                                                         *
     *                         Transform creators                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Returns the concatenation of this transform and the specified transform.
     * Applying the resulting transform to a node has the same effect as
     * adding the two transforms to its {@code getTransforms()} list,
     * {@code this} transform first and the specified {@code transform} second.
     * @param transform transform to be concatenated with this transform
     * @return The concatenated transform
     * @throws NullPointerException if the specified {@code transform} is null
     * @since JavaFX 8.0
     */
    public Transform createConcatenation(Transform transform) {
        final double txx = transform.getMxx();
        final double txy = transform.getMxy();
        final double txz = transform.getMxz();
        final double ttx = transform.getTx();
        final double tyx = transform.getMyx();
        final double tyy = transform.getMyy();
        final double tyz = transform.getMyz();
        final double tty = transform.getTy();
        final double tzx = transform.getMzx();
        final double tzy = transform.getMzy();
        final double tzz = transform.getMzz();
        final double ttz = transform.getTz();
        return new Affine(
            (getMxx() * txx + getMxy() * tyx + getMxz() * tzx),
            (getMxx() * txy + getMxy() * tyy + getMxz() * tzy),
            (getMxx() * txz + getMxy() * tyz + getMxz() * tzz),
            (getMxx() * ttx + getMxy() * tty + getMxz() * ttz + getTx()),
            (getMyx() * txx + getMyy() * tyx + getMyz() * tzx),
            (getMyx() * txy + getMyy() * tyy + getMyz() * tzy),
            (getMyx() * txz + getMyy() * tyz + getMyz() * tzz),
            (getMyx() * ttx + getMyy() * tty + getMyz() * ttz + getTy()),
            (getMzx() * txx + getMzy() * tyx + getMzz() * tzx),
            (getMzx() * txy + getMzy() * tyy + getMzz() * tzy),
            (getMzx() * txz + getMzy() * tyz + getMzz() * tzz),
            (getMzx() * ttx + getMzy() * tty + getMzz() * ttz + getTz()));
    }

    /**
     * Returns the inverse transform of this transform.
     * @return the inverse transform
     * @throws NonInvertibleTransformException if this transform
     *         cannot be inverted
     * @since JavaFX 8.0
     */
    public Transform createInverse() throws NonInvertibleTransformException {
        return getInverseCache().clone();
    }

    /**
     * Returns a deep copy of this transform.
     * @return a copy of this transform
     * @since JavaFX 8.0
     */
    @Override
    public Transform clone() {
        return TransformUtils.immutableTransform(this);
    }

    /* *************************************************************************
     *                                                                         *
     *                     Transform, Inverse Transform                        *
     *                                                                         *
     **************************************************************************/

    /**
     * Transforms the specified point by this transform.
     * This method can be used only for 2D transforms.
     * @param x the X coordinate of the point
     * @param y the Y coordinate of the point
     * @return the transformed point
     * @throws IllegalStateException if this is a 3D transform
     * @since JavaFX 8.0
     */
    public Point2D transform(double x, double y) {
        ensureCanTransform2DPoint();

        return new Point2D(
            getMxx() * x + getMxy() * y + getTx(),
            getMyx() * x + getMyy() * y + getTy());
    }

    /**
     * Transforms the specified point by this transform.
     * This method can be used only for 2D transforms.
     * @param point the point to be transformed
     * @return the transformed point
     * @throws IllegalStateException if this is a 3D transform
     * @throws NullPointerException if the specified {@code point} is null
     * @since JavaFX 8.0
     */
    public Point2D transform(Point2D point) {
        return transform(point.getX(), point.getY());
    }

    /**
     * Transforms the specified point by this transform.
     * @param x the X coordinate of the point
     * @param y the Y coordinate of the point
     * @param z the Z coordinate of the point
     * @return the transformed point
     * @since JavaFX 8.0
     */
    public Point3D transform(double x, double y, double z) {
        return new Point3D(
            getMxx() * x + getMxy() * y + getMxz() * z + getTx(),
            getMyx() * x + getMyy() * y + getMyz() * z + getTy(),
            getMzx() * x + getMzy() * y + getMzz() * z + getTz());
    }

    /**
     * Transforms the specified point by this transform.
     * @param point the point to be transformed
     * @return the transformed point
     * @throws NullPointerException if the specified {@code point} is null
     * @since JavaFX 8.0
     */
    public Point3D transform(Point3D point) {
        return transform(point.getX(), point.getY(), point.getZ());
    }

    /**
     * Transforms the specified bounds by this transform.
     * @param bounds the bounds to be transformed
     * @return the transformed bounds
     * @since JavaFX 8.0
     */
    public Bounds transform(Bounds bounds) {
        if (isType2D() && (bounds.getMinZ() == 0) && (bounds.getMaxZ() == 0)) {
            Point2D p1 = transform(bounds.getMinX(), bounds.getMinY());
            Point2D p2 = transform(bounds.getMaxX(), bounds.getMinY());
            Point2D p3 = transform(bounds.getMaxX(), bounds.getMaxY());
            Point2D p4 = transform(bounds.getMinX(), bounds.getMaxY());

            return BoundsUtils.createBoundingBox(p1, p2, p3, p4);
        }
        Point3D p1 = transform(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ());
        Point3D p2 = transform(bounds.getMinX(), bounds.getMinY(), bounds.getMaxZ());
        Point3D p3 = transform(bounds.getMinX(), bounds.getMaxY(), bounds.getMinZ());
        Point3D p4 = transform(bounds.getMinX(), bounds.getMaxY(), bounds.getMaxZ());
        Point3D p5 = transform(bounds.getMaxX(), bounds.getMaxY(), bounds.getMinZ());
        Point3D p6 = transform(bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
        Point3D p7 = transform(bounds.getMaxX(), bounds.getMinY(), bounds.getMinZ());
        Point3D p8 = transform(bounds.getMaxX(), bounds.getMinY(), bounds.getMaxZ());

        return BoundsUtils.createBoundingBox(p1, p2, p3, p4, p5, p6, p7, p8);
    }

    /**
     * Core of the transform2DPoints method.
     * All the checks has been performed and the care of the overlaps has been
     * taken by the enclosing method, this method only transforms the points
     * and fills them to the array. Used by the subclasses to perform
     * the transform efficiently.
     */
    void transform2DPointsImpl(double[] srcPts, int srcOff,
            double[] dstPts, int dstOff, int numPts) {
        final double xx = getMxx();
        final double xy = getMxy();
        final double tx = getTx();
        final double yx = getMyx();
        final double yy = getMyy();
        final double ty = getTy();

        while (--numPts >= 0) {
            final double x = srcPts[srcOff++];
            final double y = srcPts[srcOff++];

            dstPts[dstOff++] = xx * x + xy * y + tx;
            dstPts[dstOff++] = yx * x + yy * y + ty;
        }
    }

    /**
     * Core of the transform3DPoints method.
     * All the checks has been performed and the care of the overlaps has been
     * taken by the enclosing method, this method only transforms the points
     * and fills them to the array. Used by the subclasses to perform
     * the transform efficiently.
     */
    void transform3DPointsImpl(double[] srcPts, int srcOff,
            double[] dstPts, int dstOff, int numPts) {

        final double xx = getMxx();
        final double xy = getMxy();
        final double xz = getMxz();
        final double tx = getTx();
        final double yx = getMyx();
        final double yy = getMyy();
        final double yz = getMyz();
        final double ty = getTy();
        final double zx = getMzx();
        final double zy = getMzy();
        final double zz = getMzz();
        final double tz = getTz();

        while (--numPts >= 0) {
            final double x = srcPts[srcOff++];
            final double y = srcPts[srcOff++];
            final double z = srcPts[srcOff++];

            dstPts[dstOff++] = xx * x + xy * y + xz * z + tx;
            dstPts[dstOff++] = yx * x + yy * y + yz * z + ty;
            dstPts[dstOff++] = zx * x + zy * y + zz * z + tz;
        }
    }

    /**
     * Transforms an array of coordinates by this transform.
     * The two coordinate array sections can be exactly the same or
     * can be overlapping sections of the same array without affecting the
     * validity of the results.
     * This method ensures that no source coordinates are overwritten by a
     * previous operation before they can be transformed.
     * The coordinates are stored in the arrays starting at the specified
     * offset in the order <code>[x0, y0, x1, y1, ..., xn, yn]</code>.
     * This method can be used only for 2D transforms.
     * @param srcPts the array containing the source point coordinates.
     * Each point is stored as a pair of x,&nbsp;y coordinates.
     * @param srcOff the offset to the first point to be transformed
     * in the source array
     * @param dstPts the array into which the transformed point coordinates
     * are returned.  Each point is stored as a pair of x,&nbsp;y
     * coordinates.
     * @param dstOff the offset to the location of the first
     * transformed point that is stored in the destination array
     * @param numPts the number of points to be transformed
     * @throws IllegalStateException if this is a 3D transform
     * @throws NullPointerException if {@code srcPts} or (@code dstPts} is null
     * @since JavaFX 8.0
     */
    public void transform2DPoints(double[] srcPts, int srcOff,
                          double[] dstPts, int dstOff,
                          int numPts) {

        if (srcPts == null || dstPts == null) {
            throw new NullPointerException();
        }

        if (!isType2D()) {
            throw new IllegalStateException("Cannot transform 2D points "
                    + "with a 3D transform");
        }

        // deal with overlapping arrays
        srcOff = getFixedSrcOffset(srcPts, srcOff, dstPts, dstOff, numPts, 2);

        // do the transformations
        transform2DPointsImpl(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /**
     * Transforms an array of floating point coordinates by this transform.
     * The three coordinate array sections can be exactly the same or
     * can be overlapping sections of the same array without affecting the
     * validity of the results.
     * This method ensures that no source coordinates are overwritten by a
     * previous operation before they can be transformed.
     * The coordinates are stored in the arrays starting at the specified
     * offset in the order <code>[x0, y0, z0, x1, y1, z1, ..., xn, yn, zn]</code>.
     * @param srcPts the array containing the source point coordinates.
     * Each point is stored as a tiplet of x,&nbsp;y,&nbsp;z coordinates.
     * @param srcOff the offset to the first point to be transformed
     * in the source array
     * @param dstPts the array into which the transformed point coordinates
     * are returned.  Each point is stored as a triplet of x,&nbsp;y,&nbsp;z
     * coordinates.
     * @param dstOff the offset to the location of the first
     * transformed point that is stored in the destination array
     * @param numPts the number of points to be transformed
     * @throws NullPointerException if {@code srcPts} or (@code dstPts} is null
     * @since JavaFX 8.0
     */
    public void transform3DPoints(double[] srcPts, int srcOff,
                          double[] dstPts, int dstOff,
                          int numPts) {

        if (srcPts == null || dstPts == null) {
            throw new NullPointerException();
        }

        // deal with overlapping arrays
        srcOff = getFixedSrcOffset(srcPts, srcOff, dstPts, dstOff, numPts, 3);

        // do the transformations
        transform3DPointsImpl(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /**
     * Transforms the relative magnitude vector by this transform.
     * The vector is transformed without applying the translation components
     * of the affine transformation matrix.
     * This method can be used only for a 2D transform.
     * @param x vector magnitude in the direction of the X axis
     * @param y vector magnitude in the direction of the Y axis
     * @return the transformed relative magnitude vector represented
     *         by a {@code Point2D} instance
     * @throws IllegalStateException if this is a 3D transform
     * @since JavaFX 8.0
     */
    public Point2D deltaTransform(double x, double y) {
        ensureCanTransform2DPoint();

        return new Point2D(
            getMxx() * x + getMxy() * y,
            getMyx() * x + getMyy() * y);
    }

    /**
     * Transforms the relative magnitude vector represented by the specified
     * {@code Point2D} instance by this transform.
     * The vector is transformed without applying the translation components
     * of the affine transformation matrix.
     * This method can be used only for a 2D transform.
     * @param point the relative magnitude vector
     * @return the transformed relative magnitude vector represented
     *         by a {@code Point2D} instance
     * @throws IllegalStateException if this is a 3D transform
     * @throws NullPointerException if the specified {@code point} is null
     * @since JavaFX 8.0
     */
    public Point2D deltaTransform(Point2D point) {
        return deltaTransform(point.getX(), point.getY());
    }

    /**
     * Transforms the relative magnitude vector by this transform.
     * The vector is transformed without applying the translation components
     * of the affine transformation matrix.
     * @param x vector magnitude in the direction of the X axis
     * @param y vector magnitude in the direction of the Y axis
     * @param z vector magnitude in the direction of the Z axis
     * @return the transformed relative magnitude vector represented
     *         by a {@code Point3D} instance
     * @since JavaFX 8.0
     */
    public Point3D deltaTransform(double x, double y, double z) {
        return new Point3D(
            getMxx() * x + getMxy() * y + getMxz() * z,
            getMyx() * x + getMyy() * y + getMyz() * z,
            getMzx() * x + getMzy() * y + getMzz() * z);
    }

    /**
     * Transforms the relative magnitude vector represented by the specified
     * {@code Point3D} instance by this transform.
     * The vector is transformed without applying the translation components
     * of the affine transformation matrix.
     * @param point the relative magnitude vector
     * @return the transformed relative magnitude vector represented
     *         by a {@code Point3D} instance
     * @throws NullPointerException if the specified {@code point} is null
     * @since JavaFX 8.0
     */
    public Point3D deltaTransform(Point3D point) {
        return deltaTransform(point.getX(), point.getY(), point.getZ());
    }

    /**
     * Transforms the specified point by the inverse of this transform.
     * This method can be used only for 2D transforms.
     * @param x the X coordinate of the point
     * @param y the Y coordinate of the point
     * @return the inversely transformed point
     * @throws IllegalStateException if this is a 3D transform
     * @throws NonInvertibleTransformException if this transform
     *         cannot be inverted
     * @since JavaFX 8.0
     */
    public Point2D inverseTransform(double x, double y)
            throws NonInvertibleTransformException {

        ensureCanTransform2DPoint();

        return getInverseCache().transform(x, y);
    }

    /**
     * Transforms the specified point by the inverse of this transform.
     * This method can be used only for 2D transforms.
     * @param point the point to be transformed
     * @return the inversely transformed point
     * @throws IllegalStateException if this is a 3D transform
     * @throws NonInvertibleTransformException if this transform
     *         cannot be inverted
     * @throws NullPointerException if the specified {@code point} is null
     * @since JavaFX 8.0
     */
    public Point2D inverseTransform(Point2D point)
            throws NonInvertibleTransformException {
        return inverseTransform(point.getX(), point.getY());
    }

    /**
     * Transforms the specified point by the inverse of this transform.
     * @param x the X coordinate of the point
     * @param y the Y coordinate of the point
     * @param z the Z coordinate of the point
     * @return the inversely transformed point
     * @throws NonInvertibleTransformException if this transform
     *         cannot be inverted
     * @since JavaFX 8.0
     */
    public Point3D inverseTransform(double x, double y, double z)
            throws NonInvertibleTransformException {

        return getInverseCache().transform(x, y, z);
    }

    /**
     * Transforms the specified point by the inverse of this transform.
     * @param point the point to be transformed
     * @return the inversely transformed point
     * @throws NonInvertibleTransformException if this transform
     *         cannot be inverted
     * @throws NullPointerException if the specified {@code point} is null
     * @since JavaFX 8.0
     */
    public Point3D inverseTransform(Point3D point)
            throws NonInvertibleTransformException {
        return inverseTransform(point.getX(), point.getY(), point.getZ());
    }

    /**
     * Transforms the specified bounds by the inverse of this transform.
     * @param bounds the bounds to be transformed
     * @return the inversely transformed bounds
     * @throws NonInvertibleTransformException if this transform
     *         cannot be inverted
     * @throws NullPointerException if the specified {@code bounds} is null
     * @since JavaFX 8.0
     */
    public Bounds inverseTransform(Bounds bounds)
            throws NonInvertibleTransformException {
        if (isType2D() && (bounds.getMinZ() == 0) && (bounds.getMaxZ() == 0)) {
            Point2D p1 = inverseTransform(bounds.getMinX(), bounds.getMinY());
            Point2D p2 = inverseTransform(bounds.getMaxX(), bounds.getMinY());
            Point2D p3 = inverseTransform(bounds.getMaxX(), bounds.getMaxY());
            Point2D p4 = inverseTransform(bounds.getMinX(), bounds.getMaxY());

            return BoundsUtils.createBoundingBox(p1, p2, p3, p4);
        }
        Point3D p1 = inverseTransform(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ());
        Point3D p2 = inverseTransform(bounds.getMinX(), bounds.getMinY(), bounds.getMaxZ());
        Point3D p3 = inverseTransform(bounds.getMinX(), bounds.getMaxY(), bounds.getMinZ());
        Point3D p4 = inverseTransform(bounds.getMinX(), bounds.getMaxY(), bounds.getMaxZ());
        Point3D p5 = inverseTransform(bounds.getMaxX(), bounds.getMaxY(), bounds.getMinZ());
        Point3D p6 = inverseTransform(bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
        Point3D p7 = inverseTransform(bounds.getMaxX(), bounds.getMinY(), bounds.getMinZ());
        Point3D p8 = inverseTransform(bounds.getMaxX(), bounds.getMinY(), bounds.getMaxZ());

        return BoundsUtils.createBoundingBox(p1, p2, p3, p4, p5, p6, p7, p8);

    }

    /**
     * Core of the inverseTransform2DPoints method.
     * All the checks has been performed and the care of the overlaps has been
     * taken by the enclosing method, this method only transforms the points
     * and fills them to the array. Used by the subclasses to perform
     * the transform efficiently.
     */
    void inverseTransform2DPointsImpl(double[] srcPts, int srcOff,
            double[] dstPts, int dstOff, int numPts)
            throws NonInvertibleTransformException {

        getInverseCache().transform2DPointsImpl(srcPts, srcOff,
                dstPts, dstOff, numPts);
    }

    /**
     * Core of the inverseTransform3DPoints method.
     * All the checks has been performed and the care of the overlaps has been
     * taken by the enclosing method, this method only transforms the points
     * and fills them to the array. Used by the subclasses to perform
     * the transform efficiently.
     */
    void inverseTransform3DPointsImpl(double[] srcPts, int srcOff,
            double[] dstPts, int dstOff, int numPts)
            throws NonInvertibleTransformException {

        getInverseCache().transform3DPointsImpl(srcPts, srcOff,
                dstPts, dstOff, numPts);
    }

    /**
     * Transforms an array of coordinates by the inverse of this transform.
     * The two coordinate array sections can be exactly the same or
     * can be overlapping sections of the same array without affecting the
     * validity of the results.
     * This method ensures that no source coordinates are overwritten by a
     * previous operation before they can be transformed.
     * The coordinates are stored in the arrays starting at the specified
     * offset in the order <code>[x0, y0, x1, y1, ..., xn, yn]</code>.
     * This method can be used only for 2D transforms.
     * @param srcPts the array containing the source point coordinates.
     * Each point is stored as a pair of x,&nbsp;y coordinates.
     * @param srcOff the offset to the first point to be transformed
     * in the source array
     * @param dstPts the array into which the transformed point coordinates
     * are returned.  Each point is stored as a pair of x,&nbsp;y
     * coordinates.
     * @param dstOff the offset to the location of the first
     * transformed point that is stored in the destination array
     * @param numPts the number of points to be transformed
     * @throws IllegalStateException if this is a 3D transform
     * @throws NonInvertibleTransformException if this transform
     *         cannot be inverted
     * @throws NullPointerException if {@code srcPts} or (@code dstPts} is null
     * @since JavaFX 8.0
     */
    public void inverseTransform2DPoints(double[] srcPts, int srcOff,
                          double[] dstPts, int dstOff,
                          int numPts) throws NonInvertibleTransformException{

        if (srcPts == null || dstPts == null) {
            throw new NullPointerException();
        }

        if (!isType2D()) {
            throw new IllegalStateException("Cannot transform 2D points "
                    + "with a 3D transform");
        }

        // deal with overlapping arrays
        srcOff = getFixedSrcOffset(srcPts, srcOff, dstPts, dstOff, numPts, 2);

        // do the transformations
        inverseTransform2DPointsImpl(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /**
     * Transforms an array of floating point coordinates by the inverse
     * of this transform.
     * The three coordinate array sections can be exactly the same or
     * can be overlapping sections of the same array without affecting the
     * validity of the results.
     * This method ensures that no source coordinates are overwritten by a
     * previous operation before they can be transformed.
     * The coordinates are stored in the arrays starting at the specified
     * offset in the order <code>[x0, y0, z0, x1, y1, z1, ..., xn, yn, zn]</code>.
     * @param srcPts the array containing the source point coordinates.
     * Each point is stored as a triplet of x,&nbsp;y,&nbsp;z coordinates.
     * @param srcOff the offset to the first point to be transformed
     * in the source array
     * @param dstPts the array into which the transformed point coordinates
     * are returned.  Each point is stored as a triplet of x,&nbsp;y,&nbsp;z
     * coordinates.
     * @param dstOff the offset to the location of the first
     * transformed point that is stored in the destination array
     * @param numPts the number of points to be transformed
     * @throws NonInvertibleTransformException if this transform
     *         cannot be inverted
     * @throws NullPointerException if {@code srcPts} or (@code dstPts} is null
     * @since JavaFX 8.0
     */
    public void inverseTransform3DPoints(double[] srcPts, int srcOff,
                          double[] dstPts, int dstOff,
                          int numPts) throws NonInvertibleTransformException {

        if (srcPts == null || dstPts == null) {
            throw new NullPointerException();
        }

        // deal with overlapping arrays
        srcOff = getFixedSrcOffset(srcPts, srcOff, dstPts, dstOff, numPts, 3);

        // do the transformations
        inverseTransform3DPointsImpl(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /**
     * Transforms the relative magnitude vector by the inverse of this transform.
     * The vector is transformed without applying the translation components
     * of the affine transformation matrix.
     * This method can be used only for a 2D transform.
     * @param x vector magnitude in the direction of the X axis
     * @param y vector magnitude in the direction of the Y axis
     * @return the inversely transformed relative magnitude vector represented
     *         by a {@code Point2D} instance
     * @throws IllegalStateException if this is a 3D transform
     * @throws NonInvertibleTransformException if this transform
     *         cannot be inverted
     * @since JavaFX 8.0
     */
    public Point2D inverseDeltaTransform(double x, double y)
            throws NonInvertibleTransformException {

        ensureCanTransform2DPoint();

        return getInverseCache().deltaTransform(x, y);
    }

    /**
     * Transforms the relative magnitude vector represented by the specified
     * {@code Point2D} instance by the inverse of this transform.
     * The vector is transformed without applying the translation components
     * of the affine transformation matrix.
     * This method can be used only for a 2D transform.
     * @param point the relative magnitude vector
     * @return the inversely transformed relative magnitude vector represented
     *         by a {@code Point2D} instance
     * @throws IllegalStateException if this is a 3D transform
     * @throws NonInvertibleTransformException if this transform
     *         cannot be inverted
     * @throws NullPointerException if the specified {@code point} is null
     * @since JavaFX 8.0
     */
    public Point2D inverseDeltaTransform(Point2D point)
            throws NonInvertibleTransformException {
        return inverseDeltaTransform(point.getX(), point.getY());
    }

    /**
     * Transforms the relative magnitude vector by the inverse of this transform.
     * The vector is transformed without applying the translation components
     * of the affine transformation matrix.
     * @param x vector magnitude in the direction of the X axis
     * @param y vector magnitude in the direction of the Y axis
     * @param z vector magnitude in the direction of the Z axis
     * @return the inversely transformed relative magnitude vector represented
     *         by a {@code Point3D} instance
     * @throws NonInvertibleTransformException if this transform
     *         cannot be inverted
     * @since JavaFX 8.0
     */
    public Point3D inverseDeltaTransform(double x, double y, double z)
            throws NonInvertibleTransformException {

        return getInverseCache().deltaTransform(x, y, z);
    }

    /**
     * Transforms the relative magnitude vector represented by the specified
     * {@code Point3D} instance by the inverse of this transform.
     * The vector is transformed without applying the translation components
     * of the affine transformation matrix.
     * @param point the relative magnitude vector
     * @return the inversely transformed relative magnitude vector represented
     *         by a {@code Point3D} instance
     * @throws NonInvertibleTransformException if this transform
     *         cannot be inverted
     * @throws NullPointerException if the specified {@code point} is null
     * @since JavaFX 8.0
     */
    public Point3D inverseDeltaTransform(Point3D point)
            throws NonInvertibleTransformException {
        return inverseDeltaTransform(point.getX(), point.getY(), point.getZ());
    }

    /**
     * Helper method for transforming arrays of points that deals with
     * overlapping arrays.
     * @return the (if necessary fixed) srcOff
     */
    private int getFixedSrcOffset(double[] srcPts, int srcOff,
            double[] dstPts, int dstOff,
            int numPts, int dimensions) {

        if (dstPts == srcPts &&
            dstOff > srcOff && dstOff < srcOff + numPts * dimensions)
        {
            // If the arrays overlap partially with the destination higher
            // than the source and we transform the coordinates normally
            // we would overwrite some of the later source coordinates
            // with results of previous transformations.
            // To get around this we use arraycopy to copy the points
            // to their final destination with correct overwrite
            // handling and then transform them in place in the new
            // safer location.
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * dimensions);
            return dstOff;
        }

        return srcOff;
    }

    /* *************************************************************************
     *                                                                         *
     *                         Event Dispatch                                  *
     *                                                                         *
     **************************************************************************/

    private EventHandlerManager internalEventDispatcher;
    private EventHandlerManager getInternalEventDispatcher() {
        if (internalEventDispatcher == null) {
            internalEventDispatcher = new EventHandlerManager(this);
        }
        return internalEventDispatcher;
    }
    private ObjectProperty<EventHandler<? super TransformChangedEvent>>
            onTransformChanged;

    @Override
    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        return internalEventDispatcher == null
                ? tail : tail.append(getInternalEventDispatcher());
    }

    /**
     * <p>
     * Registers an event handler to this transform. Any event filters are first
     * processed, then the specified onFoo event handlers, and finally any
     * event handlers registered by this method.
     * </p><p>
     * Currently the only event delivered to a {@code Transform} is the
     * {@code TransformChangedEvent} with it's single type
     * {@code TRANSFORM_CHANGED}.
     * </p>
     *
     * @param <T> the specific event class of the handler
     * @param eventType the type of the events to receive by the handler
     * @param eventHandler the handler to register
     * @throws NullPointerException if the event type or handler is null
     * @since JavaFX 8.0
     */
    public final <T extends Event> void addEventHandler(
            final EventType<T> eventType,
            final EventHandler<? super T> eventHandler) {
        getInternalEventDispatcher()
                .addEventHandler(eventType, eventHandler);
        // need to validate all properties to get the change events
        validate();
    }

    /**
     * Unregisters a previously registered event handler from this transform.
     * One handler might have been registered for different event types, so the
     * caller needs to specify the particular event type from which to
     * unregister the handler.
     *
     * @param <T> the specific event class of the handler
     * @param eventType the event type from which to unregister
     * @param eventHandler the handler to unregister
     * @throws NullPointerException if the event type or handler is null
     * @since JavaFX 8.0
     */
    public final <T extends Event> void removeEventHandler(
            final EventType<T> eventType,
            final EventHandler<? super T> eventHandler) {
        getInternalEventDispatcher()
                .removeEventHandler(eventType, eventHandler);
    }

    /**
     * <p>
     * Registers an event filter to this transform. Registered event filters get
     * an event before any associated event handlers.
     * </p><p>
     * Currently the only event delivered to a {@code Transform} is the
     * {@code TransformChangedEvent} with it's single type
     * {@code TRANSFORM_CHANGED}.
     *
     * @param <T> the specific event class of the filter
     * @param eventType the type of the events to receive by the filter
     * @param eventFilter the filter to register
     * @throws NullPointerException if the event type or filter is null
     * @since JavaFX 8.0
     */
    public final <T extends Event> void addEventFilter(
            final EventType<T> eventType,
            final EventHandler<? super T> eventFilter) {
        getInternalEventDispatcher()
                .addEventFilter(eventType, eventFilter);
        // need to validate all properties to get the change events
        validate();
    }

    /**
     * Unregisters a previously registered event filter from this transform. One
     * filter might have been registered for different event types, so the
     * caller needs to specify the particular event type from which to
     * unregister the filter.
     *
     * @param <T> the specific event class of the filter
     * @param eventType the event type from which to unregister
     * @param eventFilter the filter to unregister
     * @throws NullPointerException if the event type or filter is null
     * @since JavaFX 8.0
     */
    public final <T extends Event> void removeEventFilter(
            final EventType<T> eventType,
            final EventHandler<? super T> eventFilter) {
        getInternalEventDispatcher()
                .removeEventFilter(eventType, eventFilter);
    }

    /**
     * Sets the onTransformChanged event handler which is called whenever
     * the transform changes any of its parameters.
     *
     * @param value the event handler, can be null to clear it
     * @since JavaFX 8.0
     */
    public final void setOnTransformChanged(
            EventHandler<? super TransformChangedEvent> value) {
        onTransformChangedProperty().set(value);
        // need to validate all properties to get the change events
        validate();
    }

    /**
     * Gets the onTransformChanged event handler.
     * @return the event handler previously set by {@code setOnTransformChanged}
     * method, null if the handler is not set.
     * @since JavaFX 8.0
     */
    public final EventHandler<? super TransformChangedEvent> getOnTransformChanged() {
        return (onTransformChanged == null) ? null : onTransformChanged.get();
    }

    /**
     * The onTransformChanged event handler is called whenever the transform
     * changes any of its parameters.
     * @return the onTransformChanged event handler
     * @since JavaFX 8.0
     */
    public final ObjectProperty<EventHandler<? super TransformChangedEvent>>
            onTransformChangedProperty() {
        if (onTransformChanged == null) {

            onTransformChanged = new SimpleObjectProperty<EventHandler
                    <? super TransformChangedEvent>>(this, "onTransformChanged") {

                @Override protected void invalidated() {
                    getInternalEventDispatcher().setEventHandler(
                            TransformChangedEvent.TRANSFORM_CHANGED, get());
                }
            };
        }

        return onTransformChanged;
    }

    /* *************************************************************************
     *                                                                         *
     *                    Internal implementation stuff                        *
     *                                                                         *
     **************************************************************************/

    /**
     * Makes sure the specified matrix type can be requested from this transform.
     * Is used for convenience in various methods that accept
     * the MatrixType argument.
     * @param type matrix type to check
     * @throws IllegalArgumentException if this is a 3D transform and
     *                                  a 2D type is requested
     */
    void checkRequestedMAT(MatrixType type) throws IllegalArgumentException{
        if (type.is2D() && !isType2D()) {
            throw new IllegalArgumentException("Cannot access 2D matrix "
                    + "for a 3D transform");
        }
    }

    /**
     * Makes sure this is a 2D transform.
     * Is used for convenience in various 2D point transformation methods.
     * @throws IllegalStateException if this is a 2D transform
     */
    void ensureCanTransform2DPoint() throws IllegalStateException {
        if (!isType2D()) {
            throw new IllegalStateException("Cannot transform 2D point "
                    + "with a 3D transform");
        }
    }

    /**
     * Needed for the proper delivery of the TransformChangedEvent.
     * If the members are invalid, the transformChanged() notification
     * is not called and the event is not delivered. To avoid that
     * we need to manually validate all properties. Subclasses validate
     * their specific properties.
     */
    void validate() {
        getMxx(); getMxy(); getMxz(); getTx();
        getMyx(); getMyy(); getMyz(); getTy();
        getMzx(); getMzy(); getMzz(); getTz();
    }

    abstract void apply(Affine3D t);

    abstract BaseTransform derive(BaseTransform t);

    void add(final Node node) {
        nodes.add(node);
    }

    void remove(final Node node) {
        nodes.remove(node);
    }

    /**
     * This method must be called by all transforms whenever any of their
     * parameters changes. It is typically called when any of the transform's
     * properties is invalidated (it is OK to skip the call if an invalid
     * property is set).
     * @since JavaFX 8.0
     */
    protected void transformChanged() {
        inverseCache = null;
        final Iterator iterator = nodes.iterator();
        while (iterator.hasNext()) {
            NodeHelper.transformsChanged(((Node) iterator.next()));
        }

        if (type2D != null) {
            type2D.invalidate();
        }

        if (identity != null) {
            identity.invalidate();
        }

        if (internalEventDispatcher != null) {
            // need to validate all properties for the event to be fired next time
            validate();
            Event.fireEvent(this, new TransformChangedEvent(this, this));
        }
    }

    /**
     * Visitor from {@code Affine} class which provides an efficient
     * {@code append} operation for the subclasses.
     * @param a {@code Affine} instance to append to
     */
    void appendTo(Affine a) {
        a.append(getMxx(), getMxy(), getMxz(), getTx(),
                 getMyx(), getMyy(), getMyz(), getTy(),
                 getMzx(), getMzy(), getMzz(), getTz());
    }

    /**
     * Visitor from {@code Affine} class which provides an efficient
     * {@code prepend} operation for the subclasses.
     * @param a {@code Affine} instance to prepend to
     */
    void prependTo(Affine a) {
        a.prepend(getMxx(), getMxy(), getMxz(), getTx(),
                  getMyx(), getMyy(), getMyz(), getTy(),
                  getMzx(), getMzy(), getMzz(), getTz());
    }

    /**
     * <p>
     * Gets the inverse transform cache.
     * </p><p>
     * Computing the inverse transform is generally an expensive operation,
     * so once it is needed we cache the result (throwing it away when the
     * transform changes). The subclasses may avoid using the cache if their
     * inverse can be computed quickly on the fly.
     * </p><p>
     * This method computes the inverse if the cache is not valid.
     * </p>
     * @return the cached inverse transformation
     * @throws NonInvertibleTransformException if this transform
     *         cannot be inverted
     */
    private Transform getInverseCache() throws NonInvertibleTransformException {
        if (inverseCache == null || inverseCache.get() == null) {
            Affine inv = new Affine(
                    getMxx(), getMxy(), getMxz(), getTx(),
                    getMyx(), getMyy(), getMyz(), getTy(),
                    getMzx(), getMzy(), getMzz(), getTz());
            inv.invert();
            inverseCache = new SoftReference<Transform>(inv);
            return inv;
        }

        return inverseCache.get();
    }

    /**
     * Used only by tests to emulate garbage collecting the soft references
     */
    void clearInverseCache() {
        if (inverseCache != null) {
            inverseCache.clear();
        }
    }

    /* ************************************************************************
     *  ImmutableTransform Class and supporting methods
     **************************************************************************/

    static Transform createImmutableTransform() {
        return new ImmutableTransform();
    }

    static Transform createImmutableTransform(
            double mxx, double mxy, double mxz, double tx,
            double myx, double myy, double myz, double ty,
            double mzx, double mzy, double mzz, double tz) {
        return new ImmutableTransform(
                mxx, mxy, mxz, tx,
                myx, myy, myz, ty,
                mzx, mzy, mzz, tz);
    }

    static Transform createImmutableTransform(Transform transform,
            double mxx, double mxy, double mxz, double tx,
            double myx, double myy, double myz, double ty,
            double mzx, double mzy, double mzz, double tz) {
        if (transform == null) {
            return new ImmutableTransform(
                    mxx, mxy, mxz, tx,
                    myx, myy, myz, ty,
                    mzx, mzy, mzz, tz);
        }
        ((Transform.ImmutableTransform) transform).setToTransform(
                mxx, mxy, mxz, tx,
                myx, myy, myz, ty,
                mzx, mzy, mzz, tz);
        return transform;
    }

    static Transform createImmutableTransform(Transform transform,
            Transform left, Transform right) {
        if (transform == null) {
            transform = new ImmutableTransform();
        }
        ((Transform.ImmutableTransform) transform).setToConcatenation(
                (ImmutableTransform) left, (ImmutableTransform) right);
        return transform;
    }

    /**
     * Immutable transformation with performance optimizations based on Affine.
     *
     * From user's perspective, this transform is immutable. However, we can
     * modify it internally. This allows for reusing instances that were
     * not handed to users. The caller is responsible for not modifying
     * user-visible instances.
     *
     * Note: can't override Transform's package private methods so they cannot
     * be optimized. Currently not a big deal.
     */
    static class ImmutableTransform extends Transform {

        private static final int APPLY_IDENTITY = 0;
        private static final int APPLY_TRANSLATE = 1;
        private static final int APPLY_SCALE = 2;
        private static final int APPLY_SHEAR = 4;
        private static final int APPLY_NON_3D = 0;
        private static final int APPLY_3D_COMPLEX = 4;
        private transient int state2d;
        private transient int state3d;

        private double xx;
        private double xy;
        private double xz;
        private double yx;
        private double yy;
        private double yz;
        private double zx;
        private double zy;
        private double zz;
        private double xt;
        private double yt;
        private double zt;

        ImmutableTransform() {
            xx = yy = zz = 1.0;
        }

        ImmutableTransform(Transform transform) {
            this(transform.getMxx(), transform.getMxy(), transform.getMxz(),
                                                                 transform.getTx(),
                 transform.getMyx(), transform.getMyy(), transform.getMyz(),
                                                                 transform.getTy(),
                 transform.getMzx(), transform.getMzy(), transform.getMzz(),
                                                                 transform.getTz());
        }

        ImmutableTransform(double mxx, double mxy, double mxz, double tx,
                      double myx, double myy, double myz, double ty,
                      double mzx, double mzy, double mzz, double tz) {
            xx = mxx;
            xy = mxy;
            xz = mxz;
            xt = tx;

            yx = myx;
            yy = myy;
            yz = myz;
            yt = ty;

            zx = mzx;
            zy = mzy;
            zz = mzz;
            zt = tz;

            updateState();
        }

        // Beware: this is modifying immutable transform!
        // It is private and it is there just for the purpose of reusing
        // instances not given to users
        private void setToTransform(double mxx, double mxy, double mxz, double tx,
                                    double myx, double myy, double myz, double ty,
                                    double mzx, double mzy, double mzz, double tz)
        {
            xx = mxx;
            xy = mxy;
            xz = mxz;
            xt = tx;
            yx = myx;
            yy = myy;
            yz = myz;
            yt = ty;
            zx = mzx;
            zy = mzy;
            zz = mzz;
            zt = tz;
            updateState();
        }

        // Beware: this is modifying immutable transform!
        // It is private and it is there just for the purpose of reusing
        // instances not given to users
        private void setToConcatenation(ImmutableTransform left, ImmutableTransform right) {
            if (left.state3d == APPLY_NON_3D && right.state3d == APPLY_NON_3D) {
                xx = left.xx * right.xx + left.xy * right.yx;
                xy = left.xx * right.xy + left.xy * right.yy;
                xt = left.xx * right.xt + left.xy * right.yt + left.xt;
                yx = left.yx * right.xx + left.yy * right.yx;
                yy = left.yx * right.xy + left.yy * right.yy;
                yt = left.yx * right.xt + left.yy * right.yt + left.yt;
                if (state3d != APPLY_NON_3D) {
                    xz = yz = zx = zy = zt = 0.0;
                    zz = 1.0;
                    state3d = APPLY_NON_3D;
                }
                updateState2D();
            } else {
                xx = left.xx * right.xx + left.xy * right.yx + left.xz * right.zx;
                xy = left.xx * right.xy + left.xy * right.yy + left.xz * right.zy;
                xz = left.xx * right.xz + left.xy * right.yz + left.xz * right.zz;
                xt = left.xx * right.xt + left.xy * right.yt + left.xz * right.zt + left.xt;
                yx = left.yx * right.xx + left.yy * right.yx + left.yz * right.zx;
                yy = left.yx * right.xy + left.yy * right.yy + left.yz * right.zy;
                yz = left.yx * right.xz + left.yy * right.yz + left.yz * right.zz;
                yt = left.yx * right.xt + left.yy * right.yt + left.yz * right.zt + left.yt;
                zx = left.zx * right.xx + left.zy * right.yx + left.zz * right.zx;
                zy = left.zx * right.xy + left.zy * right.yy + left.zz * right.zy;
                zz = left.zx * right.xz + left.zy * right.yz + left.zz * right.zz;
                zt = left.zx * right.xt + left.zy * right.yt + left.zz * right.zt + left.zt;
                updateState();
            }
            // could be further optimized using the states, but that would
            // require a lot of code (see Affine and all its append* methods)
        }

        @Override
        public double getMxx() {
            return xx;
        }

        @Override
        public double getMxy() {
            return xy;
        }

        @Override
        public double getMxz() {
            return xz;
        }

        @Override
        public double getTx() {
            return xt;
        }

        @Override
        public double getMyx() {
            return yx;
        }

        @Override
        public double getMyy() {
            return yy;
        }

        @Override
        public double getMyz() {
            return yz;
        }

        @Override
        public double getTy() {
            return yt;
        }

        @Override
        public double getMzx() {
            return zx;
        }

        @Override
        public double getMzy() {
            return zy;
        }

        @Override
        public double getMzz() {
            return zz;
        }

        @Override
        public double getTz() {
            return zt;
        }

    /* *************************************************************************
     *                                                                         *
     *                           State getters                                 *
     *                                                                         *
     **************************************************************************/

        @Override
        public double determinant() {
            switch(state3d) {
                default:
                    stateError();
                    // cannot reach
                case APPLY_NON_3D:
                    switch (state2d) {
                        default:
                            stateError();
                            // cannot reach
                        case APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE:
                        case APPLY_SHEAR | APPLY_SCALE:
                            return xx * yy - xy * yx;
                        case APPLY_SHEAR | APPLY_TRANSLATE:
                        case APPLY_SHEAR:
                            return -(xy* yx);
                        case APPLY_SCALE | APPLY_TRANSLATE:
                        case APPLY_SCALE:
                            return xx * yy;
                        case APPLY_TRANSLATE:
                        case APPLY_IDENTITY:
                            return 1.0;
                    }
                case APPLY_TRANSLATE:
                    return 1.0;
                case APPLY_SCALE:
                case APPLY_SCALE | APPLY_TRANSLATE:
                    return xx * yy * zz;
                case APPLY_3D_COMPLEX:
                    return (xx* (yy * zz - zy * yz) +
                            xy* (yz * zx - zz * yx) +
                            xz* (yx * zy - zx * yy));
            }
        }

        @Override
        public Transform createConcatenation(Transform transform) {
            javafx.scene.transform.Affine a = new Affine(this);
            a.append(transform);
            return a;
        }

        @Override
        public javafx.scene.transform.Affine createInverse() throws NonInvertibleTransformException {
            javafx.scene.transform.Affine t = new Affine(this);
            t.invert();
            return t;
        }

        @Override
        public Transform clone() {
            return new ImmutableTransform(this);
        }

        /* *************************************************************************
         *                                                                         *
         *                     Transform, Inverse Transform                        *
         *                                                                         *
         **************************************************************************/

        @Override
        public Point2D transform(double x, double y) {
            ensureCanTransform2DPoint();

            switch (state2d) {
                default:
                    stateError();
                    // cannot reach
                case APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE:
                    return new Point2D(
                        xx * x + xy * y + xt,
                        yx * x + yy * y + yt);
                case APPLY_SHEAR | APPLY_SCALE:
                    return new Point2D(
                        xx * x + xy * y,
                        yx * x + yy * y);
                case APPLY_SHEAR | APPLY_TRANSLATE:
                    return new Point2D(
                            xy * y + xt,
                            yx * x + yt);
                case APPLY_SHEAR:
                    return new Point2D(xy * y, yx * x);
                case APPLY_SCALE | APPLY_TRANSLATE:
                    return new Point2D(
                            xx * x + xt,
                            yy * y + yt);
                case APPLY_SCALE:
                    return new Point2D(xx * x, yy * y);
                case APPLY_TRANSLATE:
                    return new Point2D(x + xt, y + yt);
                case APPLY_IDENTITY:
                    return new Point2D(x, y);
            }
        }

        @Override
        public Point3D transform(double x, double y, double z) {
            switch (state3d) {
                default:
                    stateError();
                    // cannot reach
                case APPLY_NON_3D:
                    switch (state2d) {
                        default:
                            stateError();
                            // cannot reach
                        case APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE:
                            return new Point3D(
                                xx * x + xy * y + xt,
                                yx * x + yy * y + yt, z);
                        case APPLY_SHEAR | APPLY_SCALE:
                            return new Point3D(
                                xx * x + xy * y,
                                yx * x + yy * y, z);
                        case APPLY_SHEAR | APPLY_TRANSLATE:
                            return new Point3D(
                                    xy * y + xt, yx * x + yt,
                                    z);
                        case APPLY_SHEAR:
                            return new Point3D(xy * y, yx * x, z);
                        case APPLY_SCALE | APPLY_TRANSLATE:
                            return new Point3D(
                                    xx * x + xt, yy * y + yt,
                                    z);
                        case APPLY_SCALE:
                            return new Point3D(xx * x, yy * y, z);
                        case APPLY_TRANSLATE:
                            return new Point3D(x + xt, y + yt, z);
                        case APPLY_IDENTITY:
                            return new Point3D(x, y, z);
                    }
                case APPLY_TRANSLATE:
                    return new Point3D(x + xt, y + yt, z + zt);
                case APPLY_SCALE:
                    return new Point3D(xx * x, yy * y, zz * z);
                case APPLY_SCALE | APPLY_TRANSLATE:
                    return new Point3D(
                            xx * x + xt,
                            yy * y + yt,
                            zz * z + zt);
                case APPLY_3D_COMPLEX:
                    return new Point3D(
                        xx * x + xy * y + xz * z + xt,
                        yx * x + yy * y + yz * z + yt,
                        zx * x + zy * y + zz * z + zt);
            }
        }

        @Override
        public Point2D deltaTransform(double x, double y) {
            ensureCanTransform2DPoint();

            switch (state2d) {
                default:
                    stateError();
                    // cannot reach
                case APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE:
                case APPLY_SHEAR | APPLY_SCALE:
                    return new Point2D(
                        xx * x + xy * y,
                        yx * x + yy * y);
                case APPLY_SHEAR | APPLY_TRANSLATE:
                case APPLY_SHEAR:
                    return new Point2D(xy * y, yx * x);
                case APPLY_SCALE | APPLY_TRANSLATE:
                case APPLY_SCALE:
                    return new Point2D(xx * x, yy * y);
                case APPLY_TRANSLATE:
                case APPLY_IDENTITY:
                    return new Point2D(x, y);
            }
        }

        @Override
        public Point3D deltaTransform(double x, double y, double z) {
            switch (state3d) {
                default:
                    stateError();
                    // cannot reach
                case APPLY_NON_3D:
                    switch (state2d) {
                        default:
                            stateError();
                            // cannot reach
                        case APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE:
                        case APPLY_SHEAR | APPLY_SCALE:
                            return new Point3D(
                                xx * x + xy * y,
                                yx * x + yy * y, z);
                        case APPLY_SHEAR | APPLY_TRANSLATE:
                        case APPLY_SHEAR:
                            return new Point3D(xy * y, yx * x, z);
                        case APPLY_SCALE | APPLY_TRANSLATE:
                        case APPLY_SCALE:
                            return new Point3D(xx * x, yy * y, z);
                        case APPLY_TRANSLATE:
                        case APPLY_IDENTITY:
                            return new Point3D(x, y, z);
                    }
                case APPLY_TRANSLATE:
                    return new Point3D(x, y, z);
                case APPLY_SCALE:
                case APPLY_SCALE | APPLY_TRANSLATE:
                    return new Point3D(xx * x, yy * y, zz * z);
                case APPLY_3D_COMPLEX:
                    return new Point3D(
                        xx * x + xy * y + xz * z,
                        yx * x + yy * y + yz * z,
                        zx * x + zy * y + zz * z);
            }
        }

        @Override
        public Point2D inverseTransform(double x, double y)
                throws NonInvertibleTransformException {
            ensureCanTransform2DPoint();

            switch (state2d) {
                default:
                    return super.inverseTransform(x, y);
                case APPLY_SHEAR | APPLY_TRANSLATE:
                    if (xy == 0.0 || yx == 0.0) {
                        throw new NonInvertibleTransformException("Determinant is 0");
                    }
                    return new Point2D(
                            (1.0 / yx) * y - yt / yx,
                            (1.0 / xy) * x - xt / xy);
                case APPLY_SHEAR:
                    if (xy == 0.0 || yx == 0.0) {
                        throw new NonInvertibleTransformException("Determinant is 0");
                    }
                    return new Point2D((1.0 / yx) * y, (1.0 / xy) * x);
                case APPLY_SCALE | APPLY_TRANSLATE:
                    if (xx == 0.0 || yy == 0.0) {
                        throw new NonInvertibleTransformException("Determinant is 0");
                    }
                    return new Point2D(
                            (1.0 / xx) * x - xt / xx,
                            (1.0 / yy) * y - yt / yy);
                case APPLY_SCALE:
                    if (xx == 0.0 || yy == 0.0) {
                        throw new NonInvertibleTransformException("Determinant is 0");
                    }
                    return new Point2D((1.0 / xx) * x, (1.0 / yy) * y);
                case APPLY_TRANSLATE:
                    return new Point2D(x - xt, y - yt);
                case APPLY_IDENTITY:
                    return new Point2D(x, y);
            }
        }

        @Override
        public Point3D inverseTransform(double x, double y, double z)
                throws NonInvertibleTransformException {
            switch(state3d) {
                default:
                    stateError();
                    // cannot reach
                case APPLY_NON_3D:
                    switch (state2d) {
                        default:
                            return super.inverseTransform(x, y, z);
                        case APPLY_SHEAR | APPLY_TRANSLATE:
                            if (xy == 0.0 || yx == 0.0) {
                                throw new NonInvertibleTransformException(
                                        "Determinant is 0");
                            }
                            return new Point3D(
                                    (1.0 / yx) * y - yt / yx,
                                    (1.0 / xy) * x - xt / xy, z);
                        case APPLY_SHEAR:
                            if (xy == 0.0 || yx == 0.0) {
                                throw new NonInvertibleTransformException(
                                        "Determinant is 0");
                            }
                            return new Point3D(
                                    (1.0 / yx) * y,
                                    (1.0 / xy) * x, z);
                        case APPLY_SCALE | APPLY_TRANSLATE:
                            if (xx == 0.0 || yy == 0.0) {
                                throw new NonInvertibleTransformException(
                                        "Determinant is 0");
                            }
                            return new Point3D(
                                    (1.0 / xx) * x - xt / xx,
                                    (1.0 / yy) * y - yt / yy, z);
                        case APPLY_SCALE:
                            if (xx == 0.0 || yy == 0.0) {
                                throw new NonInvertibleTransformException(
                                        "Determinant is 0");
                            }
                            return new Point3D((1.0 / xx) * x, (1.0 / yy) * y, z);
                        case APPLY_TRANSLATE:
                            return new Point3D(x - xt, y - yt, z);
                        case APPLY_IDENTITY:
                            return new Point3D(x, y, z);
                    }
                case APPLY_TRANSLATE:
                    return new Point3D(x - xt, y - yt, z - zt);
                case APPLY_SCALE:
                    if (xx == 0.0 || yy == 0.0 || zz == 0.0) {
                        throw new NonInvertibleTransformException("Determinant is 0");
                    }
                    return new Point3D(
                            (1.0 / xx) * x,
                            (1.0 / yy) * y,
                            (1.0 / zz) * z);
                case APPLY_SCALE | APPLY_TRANSLATE:
                    if (xx == 0.0 || yy == 0.0 || zz == 0.0) {
                        throw new NonInvertibleTransformException("Determinant is 0");
                    }
                    return new Point3D(
                            (1.0 / xx) * x - xt / xx,
                            (1.0 / yy) * y - yt / yy,
                            (1.0 / zz) * z - zt / zz);
                case APPLY_3D_COMPLEX:
                    return super.inverseTransform(x, y, z);
            }
        }

        @Override
        public Point2D inverseDeltaTransform(double x, double y)
                throws NonInvertibleTransformException {
            ensureCanTransform2DPoint();

            switch (state2d) {
                default:
                    return super.inverseDeltaTransform(x, y);
                case APPLY_SHEAR | APPLY_TRANSLATE:
                case APPLY_SHEAR:
                    if (xy == 0.0 || yx == 0.0) {
                        throw new NonInvertibleTransformException("Determinant is 0");
                    }
                    return new Point2D((1.0 / yx) * y, (1.0 / xy) * x);
                case APPLY_SCALE | APPLY_TRANSLATE:
                case APPLY_SCALE:
                    if (xx == 0.0 || yy == 0.0) {
                        throw new NonInvertibleTransformException("Determinant is 0");
                    }
                    return new Point2D((1.0 / xx) * x, (1.0 / yy) * y);
                case APPLY_TRANSLATE:
                case APPLY_IDENTITY:
                    return new Point2D(x, y);
            }
        }

        @Override
        public Point3D inverseDeltaTransform(double x, double y, double z)
                throws NonInvertibleTransformException {
            switch(state3d) {
                default:
                    stateError();
                    // cannot reach
                case APPLY_NON_3D:
                    switch (state2d) {
                        default:
                            return super.inverseDeltaTransform(x, y, z);
                        case APPLY_SHEAR | APPLY_TRANSLATE:
                        case APPLY_SHEAR:
                            if (xy == 0.0 || yx == 0.0) {
                                throw new NonInvertibleTransformException(
                                        "Determinant is 0");
                            }
                            return new Point3D(
                                    (1.0 / yx) * y,
                                    (1.0 / xy) * x, z);
                        case APPLY_SCALE | APPLY_TRANSLATE:
                        case APPLY_SCALE:
                            if (xx == 0.0 || yy == 0.0) {
                                throw new NonInvertibleTransformException(
                                        "Determinant is 0");
                            }
                            return new Point3D(
                                    (1.0 / xx) * x,
                                    (1.0 / yy) * y, z);
                        case APPLY_TRANSLATE:
                        case APPLY_IDENTITY:
                            return new Point3D(x, y, z);
                    }

                case APPLY_TRANSLATE:
                    return new Point3D(x, y, z);
                case APPLY_SCALE | APPLY_TRANSLATE:
                case APPLY_SCALE:
                    if (xx == 0.0 || yy == 0.0 || zz == 0.0) {
                        throw new NonInvertibleTransformException("Determinant is 0");
                    }
                    return new Point3D(
                            (1.0 / xx) * x,
                            (1.0 / yy) * y,
                            (1.0 / zz) * z);
                case APPLY_3D_COMPLEX:
                    return super.inverseDeltaTransform(x, y, z);
            }
        }

        /* *************************************************************************
         *                                                                         *
         *                               Other API                                 *
         *                                                                         *
         **************************************************************************/

        @Override
        public String toString() {
           final StringBuilder sb = new StringBuilder("Transform [\n");

            sb.append("\t").append(xx);
            sb.append(", ").append(xy);
            sb.append(", ").append(xz);
            sb.append(", ").append(xt);
            sb.append('\n');
            sb.append("\t").append(yx);
            sb.append(", ").append(yy);
            sb.append(", ").append(yz);
            sb.append(", ").append(yt);
            sb.append('\n');
            sb.append("\t").append(zx);
            sb.append(", ").append(zy);
            sb.append(", ").append(zz);
            sb.append(", ").append(zt);

            return sb.append("\n]").toString();
        }

        /* *************************************************************************
         *                                                                         *
         *                    Internal implementation stuff                        *
         *                                                                         *
         **************************************************************************/

        private void updateState() {
            updateState2D();

            state3d = APPLY_NON_3D;

            if (xz != 0.0 ||
                yz != 0.0 ||
                zx != 0.0 ||
                zy != 0.0)
            {
                state3d = APPLY_3D_COMPLEX;
            } else {
                if ((state2d & APPLY_SHEAR) == 0) {
                    if (zt != 0.0) {
                        state3d |= APPLY_TRANSLATE;
                    }
                    if (zz != 1.0) {
                        state3d |= APPLY_SCALE;
                    }
                    if (state3d != APPLY_NON_3D) {
                        state3d |= (state2d & (APPLY_SCALE | APPLY_TRANSLATE));
                    }
                } else {
                    if (zz != 1.0 || zt != 0.0) {
                        state3d = APPLY_3D_COMPLEX;
                    }
                }
            }
        }

        private void updateState2D() {
            if (xy == 0.0 && yx == 0.0) {
                if (xx == 1.0 && yy == 1.0) {
                    if (xt == 0.0 && yt == 0.0) {
                        state2d = APPLY_IDENTITY;
                    } else {
                        state2d = APPLY_TRANSLATE;
                    }
                } else {
                    if (xt == 0.0 && yt == 0.0) {
                        state2d = APPLY_SCALE;
                    } else {
                        state2d = (APPLY_SCALE | APPLY_TRANSLATE);
                    }
                }
            } else {
                if (xx == 0.0 && yy == 0.0) {
                    if (xt == 0.0 && yt == 0.0) {
                        state2d = APPLY_SHEAR;
                    } else {
                        state2d = (APPLY_SHEAR | APPLY_TRANSLATE);
                    }
                } else {
                    if (xt == 0.0 && yt == 0.0) {
                        state2d = (APPLY_SHEAR | APPLY_SCALE);
                    } else {
                        state2d = (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE);
                    }
                }
            }
        }

        void ensureCanTransform2DPoint() throws IllegalStateException {
            if (state3d != APPLY_NON_3D) {
                throw new IllegalStateException("Cannot transform 2D point "
                        + "with a 3D transform");
            }
        }

        private static void stateError() {
            throw new InternalError("missing case in a switch");
        }


        @Override
        void apply(final Affine3D trans) {
            trans.concatenate(xx, xy, xz, xt,
                              yx, yy, yz, yt,
                              zx, zy, zz, zt);
        }

        @Override
        BaseTransform derive(final BaseTransform trans) {
            return trans.deriveWithConcatenation(xx, xy, xz, xt,
                                                 yx, yy, yz, yt,
                                                 zx, zy, zz, zt);
        }

        /**
         * Used only by tests to check the 2d matrix state
         */
        int getState2d() {
            return state2d;
        }

        /**
         * Used only by tests to check the 3d matrix state
         */
        int getState3d() {
            return state3d;
        }

    }

}
