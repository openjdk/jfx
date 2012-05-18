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

package com.sun.javafx.scene.transform;

import com.sun.javafx.geom.transform.Affine3D;
import javafx.scene.transform.Transform;

/**
 * Internal utilities for transformations
 */
public class TransformUtils {

    /**
     * Creates an immutable arbitrary Affine transformation.
     * This method is not intended for public use, users should use the Affine
     * class.
     */
    public static Transform immutableTransform(
                double mxx, double mxy, double mxz, double tx,
                double myx, double myy, double myz, double ty,
                double mzx, double mzy, double mzz, double tz) {
        return new ImmutableTransform(
                mxx, mxy, mxz, tx,
                myx, myy, myz, ty,
                mzx, mzy, mzz, tz);
    }


    private static class ImmutableTransform extends Transform {
        private final double mxx, mxy, mxz, tx;
        private final double myx, myy, myz, ty;
        private final double mzx, mzy, mzz, tz;

        public ImmutableTransform(
                double mxx, double mxy, double mxz, double tx,
                double myx, double myy, double myz, double ty,
                double mzx, double mzy, double mzz, double tz) {
            this.mxx = mxx;
            this.mxy = mxy;
            this.mxz = mxz;
            this.tx = tx;
            this.myx = myx;
            this.myy = myy;
            this.myz = myz;
            this.ty = ty;
            this.mzx = mzx;
            this.mzy = mzy;
            this.mzz = mzz;
            this.tz = tz;
        }

        @Override
        public double getMxx() {
            return mxx;
        }

        @Override
        public double getMxy() {
            return mxy;
        }

        @Override
        public double getMxz() {
            return mxz;
        }

        @Override
        public double getTx() {
            return tx;
        }

        @Override
        public double getMyx() {
            return myx;
        }

        @Override
        public double getMyy() {
            return myy;
        }

        @Override
        public double getMyz() {
            return myz;
        }

        @Override
        public double getTy() {
            return ty;
        }

        @Override
        public double getMzx() {
            return mzx;
        }

        @Override
        public double getMzy() {
            return mzy;
        }

        @Override
        public double getMzz() {
            return mzz;
        }

        @Override
        public double getTz() {
            return tz;
        }

        @Override
        public void impl_apply(Affine3D t) {
            t.concatenate(
                    getMxx(), getMxy(), getMxz(), getTx(),
                    getMyx(), getMyy(), getMyz(), getTy(),
                    getMzx(), getMzy(), getMzz(), getTz());
        }

        @Override
        public Transform impl_copy() {
            return new ImmutableTransform(
                    getMxx(), getMxy(), getMxz(), getTx(),
                    getMyx(), getMyy(), getMyz(), getTy(),
                    getMzx(), getMzy(), getMzz(), getTz());
        }

        /**
         * Returns a string representation of this {@code Affine} object.
         * @return a string representation of this {@code Affine} object.
         */
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Transform [");

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
}
