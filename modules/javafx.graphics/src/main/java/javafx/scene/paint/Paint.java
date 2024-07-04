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

package javafx.scene.paint;

import com.sun.javafx.beans.event.AbstractNotifyListener;
import com.sun.javafx.tk.Toolkit;

/**
 * Base class for a color or gradients used to fill shapes and backgrounds when
 * rendering the scene graph.
 * @since JavaFX 2.0
 */
public abstract class Paint {

    static {
        Toolkit.setPaintAccessor(new Toolkit.PaintAccessor() {

            @Override
            public boolean isMutable(Paint paint) {
                return paint.acc_isMutable();
            }

            @Override
            public Object getPlatformPaint(Paint paint) {
                return paint.acc_getPlatformPaint();
            }

            @Override
            public void addListener(Paint paint, AbstractNotifyListener platformChangeListener) {
                paint.acc_addListener(platformChangeListener);
            }

            @Override
            public void removeListener(Paint paint, AbstractNotifyListener platformChangeListener) {
                paint.acc_removeListener(platformChangeListener);
            }


        });
    }

    /**
     *  Only allow classes in this package to subclass Paint
     */
    Paint() { }

    boolean acc_isMutable() {
        return false;
    }

    abstract Object acc_getPlatformPaint();

    void acc_addListener(AbstractNotifyListener platformChangeListener) {
        throw new UnsupportedOperationException("Not Supported.");
    }

    void acc_removeListener(AbstractNotifyListener platformChangeListener) {
        throw new UnsupportedOperationException("Not Supported.");
    }

    /**
     * Gets whether this Paint is completely opaque. An opaque paint is one that
     * has no alpha component in any of its colors. It may be possible for a Paint
     * to be opaque and yet return false, if it cannot be easily determined
     * whether the paint is actually opaque. For example, the ImagePattern may not
     * be able to cheaply determine its opacity.
     *
     * @return true if the Paint is opaque, false if it is not opaque or if
     *         it cannot be determined.
     * @since JavaFX 8.0
     */
    public abstract boolean isOpaque();

    /**
     * Creates a paint value from a string representation. Recognizes strings
     * representing {@code Color}, {@code RadialGradient} or {@code LinearGradient}.
     * String specifying LinearGradient must begin with linear-gradient keyword
     * and string specifying RadialGradient must begin with radial-gradient.
     *
     * @param value the string to convert
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} cannot be parsed
     * @return a {@code Color}, {@code RadialGradient} or {@code LinearGradient}
     * object holding the value represented by the string argument.
     *
     * @see Color#valueOf(String)
     * @see LinearGradient#valueOf(String)
     * @see RadialGradient#valueOf(String)
     * @since JavaFX 2.1
     */
    public static Paint valueOf(String value) {
        if (value == null) {
            throw new NullPointerException("paint must be specified");
        }

        if (value.startsWith("linear-gradient(")) {
            return LinearGradient.valueOf(value);
        } else if (value.startsWith("radial-gradient(")) {
            return RadialGradient.valueOf(value);
        } else {
            return Color.valueOf(value);
        }
    }
}
