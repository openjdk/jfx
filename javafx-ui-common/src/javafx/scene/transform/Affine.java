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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;

import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import javafx.geometry.Point3D;


/**
 * <p>
 * The {@code Affine} class represents an affine transform. An affine
 * transform performs a linear mapping from 2D/3D coordinates to other 2D/3D
 * coordinates while preserving the "straightness" and "parallelness"
 * of lines.
 * Affine transformations can be constructed using sequence rotations,
 * translations, scales, and shears.</p>
 *
 * <p><b>
 * Note: application developers should not normally use this class directly,
 * but instead use the specific {@code Translate}, {@code Scale},
 * {@code Rotate}, or {@code Shear} transforms instead.</b></p>

 * <p>
 * Such a coordinate transformation can be represented by a 3 row by
 * 4 column matrix. This matrix transforms source coordinates {@code (x,y,z)}
 * into destination coordinates {@code (x',y',z')} by considering
 * them to be a column vector and multiplying the coordinate vector
 * by the matrix according to the following process:</p>
 *
 * <pre>
 *      [ x']   [  mxx  mxy  mxz  tx  ] [ x ]   [ mxx * x + mxy * y + mxz * z + tx ]
 *      [ y'] = [  myx  myy  myz  ty  ] [ y ] = [ myx * x + myy * y + myz * z + ty ]
 *      [ z']   [  mzx  mzy  mzz  tz  ] [ z ]   [ mzx * x + mzy * y + mzz * z + tz ]
 *                                      [ 1 ]
 * </pre>
 */
public class Affine extends Transform {

    public Affine() {
    }

    // to be published
    Affine(Double mxx, Double mxy, Double mxz, Double tx,
            Double myx, Double myy, Double myz, Double ty,
            Double mzx, Double mzy, Double mzz, Double tz) {
        if (mxx != 1.0) this.mxxProperty().set(mxx);
        if (mxy != 0.0) this.mxyProperty().set(mxy);
        if (mxz != 0.0) this.mxzProperty().set(mxz);
        if (tx != 0.0) this.txProperty().set(tx);
        if (myx != 0.0) this.myxProperty().set(myx);
        if (myy != 1.0) this.myyProperty().set(myy);
        if (myz != 0.0) this.myzProperty().set(myz);
        if (ty != 0.0) this.tyProperty().set(ty);
        if (mzx != 0.0) this.mzxProperty().set(mzx);
        if (mzy != 0.0) this.mzyProperty().set(mzy);
        if (mzz != 1.0) this.mzzProperty().set(mzz);
        if (tz != 0.0) this.tzProperty().set(tz);
    }

    // to be published
    void setTransform(Transform t) {
        setMxx(t.getMxx());
        setMxy(t.getMxy());
        setMxz(t.getMxz());
        setTx(t.getTx());
        setMyx(t.getMyx());
        setMyy(t.getMyy());
        setMyz(t.getMyz());
        setTy(t.getTy());
        setMzx(t.getMzx());
        setMzy(t.getMzy());
        setMzz(t.getMzz());
        setTz(t.getTz());
    }

    /**
     * Defines the X coordinate scaling element of the 3x4 matrix.
     */
    private DoubleProperty mxx;


    public final void setMxx(double value) {
        mxxProperty().set(value);
    }

    public final double getMxx() {
        return mxx == null ? 1.0F : mxx.get();
    }

    public final DoubleProperty mxxProperty() {
        if (mxx == null) {
            mxx = new DoublePropertyBase(1.0F) {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Affine.this;
                }

                @Override
                public String getName() {
                    return "mxx";
                }
            };
        }
        return mxx;
    }

    /**
     * Defines the XY coordinate element of the 3x4 matrix.
     */
    private DoubleProperty mxy;


    public final void setMxy(double value) {
        mxyProperty().set(value);
    }

    public final double getMxy() {
        return mxy == null ? 0.0 : mxy.get();
    }

    public final DoubleProperty mxyProperty() {
        if (mxy == null) {
            mxy = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Affine.this;
                }

                @Override
                public String getName() {
                    return "mxy";
                }
            };
        }
        return mxy;
    }

    /**
     * Defines the XZ coordinate element of the 3x4 matrix.
     */
    private DoubleProperty mxz;


    public final void setMxz(double value) {
        mxzProperty().set(value);
    }

    public final double getMxz() {
        return mxz == null ? 0.0 : mxz.get();
    }

    public final DoubleProperty mxzProperty() {
        if (mxz == null) {
            mxz = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Affine.this;
                }

                @Override
                public String getName() {
                    return "mxz";
                }
            };
        }
        return mxz;
    }

    /**
     * Defines the X coordinate translation element of the 3x4 matrix.
     */
    private DoubleProperty tx;


    public final void setTx(double value) {
        txProperty().set(value);
    }

    public final double getTx() {
        return tx == null ? 0.0 : tx.get();
    }

    public final DoubleProperty txProperty() {
        if (tx == null) {
            tx = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Affine.this;
                }

                @Override
                public String getName() {
                    return "tx";
                }
            };
        }
        return tx;
    }

    /**
     * Defines the YX coordinate element of the 3x4 matrix.
     */
    private DoubleProperty myx;


    public final void setMyx(double value) {
        myxProperty().set(value);
    }

    public final double getMyx() {
        return myx == null ? 0.0 : myx.get();
    }

    public final DoubleProperty myxProperty() {
        if (myx == null) {
            myx = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Affine.this;
                }

                @Override
                public String getName() {
                    return "myx";
                }
            };
        }
        return myx;
    }

    /**
     * Defines the Y coordinate scaling element of the 3x4 matrix.
     */
    private DoubleProperty myy;


    public final void setMyy(double value) {
        myyProperty().set(value);
    }

    public final double getMyy() {
        return myy == null ? 1.0F : myy.get();
    }

    public final DoubleProperty myyProperty() {
        if (myy == null) {
            myy = new DoublePropertyBase(1.0F) {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Affine.this;
                }

                @Override
                public String getName() {
                    return "myy";
                }
            };
        }
        return myy;
    }

    /**
     * Defines the YZ coordinate element of the 3x4 matrix.
     */
    private DoubleProperty myz;


    public final void setMyz(double value) {
        myzProperty().set(value);
    }

    public final double getMyz() {
        return myz == null ? 0.0 : myz.get();
    }

    public final DoubleProperty myzProperty() {
        if (myz == null) {
            myz = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Affine.this;
                }

                @Override
                public String getName() {
                    return "myz";
                }
            };
        }
        return myz;
    }

    /**
     * Defines the Y coordinate translation element of the 3x4 matrix.
     */
    private DoubleProperty ty;


    public final void setTy(double value) {
        tyProperty().set(value);
    }

    public final double getTy() {
        return ty == null ? 0.0 : ty.get();
    }

    public final DoubleProperty tyProperty() {
        if (ty == null) {
            ty = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Affine.this;
                }

                @Override
                public String getName() {
                    return "ty";
                }
            };
        }
        return ty;
    }

    /**
     * Defines the ZX coordinate element of the 3x4 matrix.
     */
    private DoubleProperty mzx;


    public final void setMzx(double value) {
        mzxProperty().set(value);
    }

    public final double getMzx() {
        return mzx == null ? 0.0 : mzx.get();
    }

    public final DoubleProperty mzxProperty() {
        if (mzx == null) {
            mzx = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Affine.this;
                }

                @Override
                public String getName() {
                    return "mzx";
                }
            };
        }
        return mzx;
    }

    /**
     * Defines the ZY coordinate element of the 3x4 matrix.
     */
    private DoubleProperty mzy;


    public final void setMzy(double value) {
        mzyProperty().set(value);
    }

    public final double getMzy() {
        return mzy == null ? 0.0 : mzy.get();
    }

    public final DoubleProperty mzyProperty() {
        if (mzy == null) {
            mzy = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Affine.this;
                }

                @Override
                public String getName() {
                    return "mzy";
                }
            };
        }
        return mzy;
    }

    /**
     * Defines the Z coordinate scaling element of the 3x4 matrix.
     */
    private DoubleProperty mzz;


    public final void setMzz(double value) {
        mzzProperty().set(value);
    }

    public final double getMzz() {
        return mzz == null ? 1.0F : mzz.get();
    }

    public final DoubleProperty mzzProperty() {
        if (mzz == null) {
            mzz = new DoublePropertyBase(1.0F) {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Affine.this;
                }

                @Override
                public String getName() {
                    return "mzz";
                }
            };
        }
        return mzz;
    }

    /**
     * Defines the Z coordinate translation element of the 3x4 matrix.
     */
    private DoubleProperty tz;


    public final void setTz(double value) {
        tzProperty().set(value);
    }

    public final double getTz() {
        return tz == null ? 0.0 : tz.get();
    }

    public final DoubleProperty tzProperty() {
        if (tz == null) {
            tz = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    transformChanged();
                }

                @Override
                public Object getBean() {
                    return Affine.this;
                }

                @Override
                public String getName() {
                    return "tz";
                }
            };
        }
        return tz;
    }

    
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_apply(final Affine3D trans) {
        trans.concatenate(getMxx(), getMxy(), getMxz(), getTx(),
                          getMyx(), getMyy(), getMyz(), getTy(),
                          getMzx(), getMzy(), getMzz(), getTz());
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public Transform impl_copy() {
        return affine(getMxx(), getMxy(), getMxz(), getTx(),
                      getMyx(), getMyy(), getMyz(), getTy(),
                      getMzx(), getMzy(), getMzz(), getTz());
    }

    /**
     * Returns a string representation of this {@code Affine} object.
     * @return a string representation of this {@code Affine} object.
     */ 
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Affine [");

        sb.append("mxx=").append(getMxx());
        sb.append(", mxy=").append(getMxy());
        sb.append(", mxz=").append(getMxz());
        sb.append(", tx=").append(getTx());

        sb.append(", myx=").append(getMyx());
        sb.append(", myy=").append(getMyy());
        sb.append(", myz=").append(getMyz());
        sb.append(", ty=").append(getTy());

        sb.append(", mzx=").append(getMzx());
        sb.append(", mzy=").append(getMzy());
        sb.append(", mzz=").append(getMzz());
        sb.append(", tz=").append(getTz());

        return sb.append("]").toString();
    }
}
