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

package com.sun.javafx.webkit.prism;

import com.sun.prism.paint.Color;
import com.sun.prism.paint.LinearGradient;
import com.sun.prism.paint.Stop;
import com.sun.webkit.graphics.WCGradient;
import com.sun.webkit.graphics.WCPoint;
import com.sun.javafx.geom.transform.BaseTransform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class WCLinearGradient extends WCGradient<LinearGradient> {

    private final WCPoint p1;
    private final WCPoint p2;
    private final List<Stop> stops = new ArrayList<Stop>();

    WCLinearGradient(WCPoint p1, WCPoint p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    @Override
    protected void addStop(Color color, float offset) {
        this.stops.add(new Stop(color, offset));
    }

    public LinearGradient getPlatformGradient() {
        Collections.sort(this.stops, WCRadialGradient.COMPARATOR);
        return new LinearGradient(
                this.p1.getX(),
                this.p1.getY(),
                this.p2.getX(),
                this.p2.getY(),
                BaseTransform.IDENTITY_TRANSFORM,
                isProportional(),
                getSpreadMethod() - 1, // convert webkit to prism
                this.stops);
    }

    @Override
    public String toString() {
        return WCRadialGradient.toString(this, this.p1, this.p2, null, this.stops);
    }
}
