/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.graphics;

import com.sun.prism.paint.Color;

public abstract class WCGradient<G> {

    /* The GradientSpreadMethod should be compliant with
     * WebCore/platform/graphics/GraphicsTypes.h
     */
    public static final int PAD = 1;
    public static final int REFLECT = 2;
    public static final int REPEAT = 3;

    private int spreadMethod = PAD;
    private boolean proportional;

    void setSpreadMethod(int spreadMethod) {
        if (spreadMethod != REFLECT && spreadMethod != REPEAT) {
            spreadMethod = PAD;
        }
        this.spreadMethod = spreadMethod;
    }

    public int getSpreadMethod() {
        return this.spreadMethod;
    }

    void setProportional(boolean proportional) {
        this.proportional = proportional;
    }

    public boolean isProportional() {
        return this.proportional;
    }

    protected abstract void addStop(Color color, float offset);

    public abstract G getPlatformGradient();
}
