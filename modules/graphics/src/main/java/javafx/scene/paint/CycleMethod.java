/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * This enum defines one of the following methods to use when painting
 * outside the gradient bounds: {@code  CycleMethod.NO_CYCLE},
 * {@code CycleMethod.REFLECT}, or {@code  CycleMethod.REPEAT}.
 * @since JavaFX 2.0
 */
public enum CycleMethod {

    /**
     * Defines the cycle method that uses the terminal colors to fill the remaining area.
     */
    NO_CYCLE, // MultipleGradientPaint.CycleMethod.NO_CYCLE

    /**
     * Defines the cycle method that reflects the gradient colors start-to-end,
     * end-to-start to fill the remaining area.
     */
    REFLECT, // MultipleGradientPaint.CycleMethod.REFLECT

    /**
     * Defines the cycle method that repeats the gradient colors to fill the remaining area.
     */
    REPEAT; //MultipleGradientPaint.CycleMethod.REPEAT
}
