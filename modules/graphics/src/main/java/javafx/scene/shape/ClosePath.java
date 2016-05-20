/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.shape;

import com.sun.javafx.geom.Path2D;
import com.sun.javafx.scene.shape.ClosePathHelper;
import com.sun.javafx.sg.prism.NGPath;

/**
 * A path element which closes the current path.
 *
 * <p>For more information on path elements see the {@link Path} and
 * {@link PathElement} classes.
 * @since JavaFX 2.0
 */
public class ClosePath extends PathElement {
    static {
        ClosePathHelper.setClosePathAccessor(new ClosePathHelper.ClosePathAccessor() {
            @Override
            public void doAddTo(PathElement pathElement, Path2D path) {
                ((ClosePath) pathElement).doAddTo(path);
            }
        });
    }

    /**
     * Creates an empty instance of ClosePath.
     */
    public ClosePath() {
        ClosePathHelper.initHelper(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void addTo(NGPath pgPath) {
        pgPath.addClosePath();
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doAddTo(Path2D path) {
        path.closePath();
    }

    /**
     * Returns a string representation of this {@code ArcTo} object.
     * @return a string representation of this {@code ArcTo} object.
     */
    @Override
    public String toString() {
        return "ClosePath";
    }
}
