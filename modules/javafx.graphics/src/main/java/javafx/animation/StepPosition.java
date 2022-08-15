/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package javafx.animation;

/**
 * Specifies the step position of an interpolator defined by {@link Interpolator#STEPS(int, StepPosition)}.
 * <p>
 * The step position determines the location of rise points in the input progress interval, which are the
 * locations on the input progress axis where the output progress value jumps from one step to the next.
 *
 * @since 20
 */
public enum StepPosition {

    /**
     * The interval starts with a rise point when the input progress value is 0.
     */
    START,

    /**
     * The interval ends with a rise point when the input progress value is 1.
     */
    END,

    /**
     * All rise points are within the open interval (0..1).
     */
    BOTH,

    /**
     * The interval starts with a rise point when the input progress value is 0,
     * and ends with a rise point when the input progress value is 1.
     */
    NONE

}
