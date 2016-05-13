/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;

public class TransformShim {

    public static boolean computeIs2D(Transform t) {
        return t.computeIs2D();
    }

    public static void clearInverseCache(Transform t) {
        t.clearInverseCache();
    }

    public static class ImmutableTransformShim extends Transform.ImmutableTransform  {

    }

    public static Transform getImmutableTransform(Transform transform) {
        return new Transform.ImmutableTransform(transform);
    }

    public static Transform getImmutableTransform(
            double mxx, double mxy, double mxz, double tx,
            double myx, double myy, double myz, double ty,
            double mzx, double mzy, double mzz, double tz) {
        return new Transform.ImmutableTransform(
                mxx, mxy, mxz, tx,
                myx, myy, myz, ty,
                mzx, mzy, mzz, tz);
    }

    public static int getImmutableState2d(Transform t) {
        return ((Transform.ImmutableTransform) t).getState2d();
    }

    public static int getImmutableState3d(Transform t) {
        return ((Transform.ImmutableTransform) t).getState3d();
    }

    /**
     * Creates a raw transformation to test the Transform class.
     */
    public static Transform createRawTransform(
            double mxx, double mxy, double mxz, double tx,
            double myx, double myy, double myz, double ty,
            double mzx, double mzy, double mzz, double tz) {
        return new RawTransform(
                mxx, mxy, mxz, tx,
                myx, myy, myz, ty,
                mzx, mzy, mzz, tz);
    }

    private static class RawTransform extends Transform {

        private final double mxx, mxy, mxz, tx;
        private final double myx, myy, myz, ty;
        private final double mzx, mzy, mzz, tz;

        public RawTransform(
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
        void apply(Affine3D t) {
            t.concatenate(
                    getMxx(), getMxy(), getMxz(), getTx(),
                    getMyx(), getMyy(), getMyz(), getTy(),
                    getMzx(), getMzy(), getMzz(), getTz());
        }

        @Override
        BaseTransform derive(BaseTransform t) {
            return t.deriveWithConcatenation(
                    getMxx(), getMxy(), getMxz(), getTx(),
                    getMyx(), getMyy(), getMyz(), getTy(),
                    getMzx(), getMzy(), getMzz(), getTz());
        }
    }
}
