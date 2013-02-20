/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.impl.state;

import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;

/**
 * An abstract helper intermediate implementation class for
 * {@code LinearConvolve} effects that break down into a horizontal and
 * a vertical pass.
 */
public abstract class HVSeparableKernel extends LinearConvolveKernel {
    @Override
    public final int getNumberOfPasses() {
        return 2;
    }

    @Override
    public PassType getPassType(int pass) {
        return (pass == 0)
            ? PassType.HORIZONTAL_CENTERED
            : PassType.VERTICAL_CENTERED;
    }

    @Override
    public final Rectangle getResultBounds(Rectangle srcdimension, int pass) {
        int ksize = getKernelSize(pass);
        Rectangle ret = new Rectangle(srcdimension);
        if (pass == 0) {
            ret.grow(ksize/2, 0);
        } else {
            ret.grow(0, ksize/2);
        }
        return ret;
    }

    @Override
    public final Rectangle getScaledResultBounds(Rectangle srcdimension, int pass) {
        int ksize = getScaledKernelSize(pass);
        Rectangle ret = new Rectangle(srcdimension);
        if (pass == 0) {
            ret.grow(ksize/2, 0);
        } else {
            ret.grow(0, ksize/2);
        }
        return ret;
    }

    @Override
    public final float[] getVector(Rectangle srcnativedimensions,
                                   BaseTransform transform, int pass)
    {
        float ret[] = new float[4];
        float ps;
        if (transform.isTranslateOrIdentity()) {
            ps = 1f;
        } else {
            ret[0] = (pass == 0) ? 1f : 0f;
            ret[1] = (pass == 1) ? 1f : 0f;
            try {
                transform.inverseDeltaTransform(ret, 0, ret, 0, 1);
                ps = (float) Math.hypot(ret[0], ret[1]);
            } catch (NoninvertibleTransformException e) {
                ps = 0f;
            }
        }
        float xoff = (pass == 0) ? ps / srcnativedimensions.width  : 0f;
        float yoff = (pass == 1) ? ps / srcnativedimensions.height : 0f;
        int ksize = getScaledKernelSize(pass);
        int center = ksize / 2;
        ret[0] = xoff;
        ret[1] = yoff;
        ret[2] = -center * xoff;
        ret[3] = -center * yoff;
        return ret;
    }
}
