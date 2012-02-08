/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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
 *
 * Use is subject to license terms.
 */

package javafx.application;

/**
 * Defines a set of conditional (optional) features. These features
 * may not be available on all platforms. An application that wants to
 * know whether a particular feature is available may query this using
 * the {@link javafx.application.Platform#isSupported(javafx.application.ConditionalFeature)
 * Platform.isSupported()} function. Using a conditional feature
 * on a platform that does not support it will not cause an exception. In
 * general, the conditional feature will just be ignored. See the documentation
 * for each feature for more detail.
 *
 * @profile common
 */
public enum ConditionalFeature {

    /**
     * Indicates that 3D is available on the platform.
     * If an application attempts to use 3D transforms or a 3D camera on a
     * platform that does not support 3D, then the transform or camera is
     * ignored; it effectively becomes the identity transform.
     */
    SCENE3D,

    /**
     * Indicates that filter effects are available on the platform.
     * If an application uses an effect on a platform that does
     * not support it, the effect will be ignored.
     */
    EFFECT,

    /**
     * Indicates that clipping against an arbitrary shape is available
     * on the platform. If an application specifies a clip node on a
     * platform that does not support clipping against an arbitrary shape,
     * the node will be clipped to the bounds of the specified clip node
     * rather than its geometric shape.
     */
    SHAPE_CLIP,

    /**
     * Indicates that text input method is available on the platform.
     * If an application specifies an input method on a platform that does
     * not support it, the input method will be ignored.
     */
    INPUT_METHOD
}
