/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @since JavaFX 2.0
 */
public enum ConditionalFeature {

    /**
     * Indicates that JavaFX classes providing graphics capabilities are
     * available on this platform.
     * @since JavaFX 8.0
     */
    GRAPHICS,

    /**
     * Indicates that JavaFX classes providing UI controls are available on
     * this platform.
     * @since JavaFX 8.0
     */
    CONTROLS,

    /**
     * Indicates that the javafx.scene.media package is available on this
     * platform.
     * @since JavaFX 8.0
     */
    MEDIA,

    /**
     * Indicates that the javafx.scene.web packages is available on this
     * platform.
     * @since JavaFX 8.0
     */
    WEB,

    /**
     * Indicates that SWT integration is available on the platform. This tests
     * for SWT integration with JavaFX, but does not test for the presence of
     * the full SWT library.
     * @since JavaFX 8.0
     */
    SWT,

    /**
     * Indicates that the Swing library is present in the Java Runtime
     * Environment and that Swing integration with JavaFX is available on the
     * platform.
     * @since JavaFX 8.0
     */
    SWING,


    /**
     * Indicates that XML libraries are present in the JRE and that the FXML
     * API is available on the platform. Note that it is possible for FXML APIs
     * to be present but unusable if the underlying Java Runtime Environment
     * does not have XML support. In this case
     * Platform.isSupported(ConditionalFeature.FXML) wll return false.
     * @since JavaFX 8.0
     */
    FXML,

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
    INPUT_METHOD,

    /**
     * Indicates that the system supports full window transparency.
     * Transparent windows will have only limited or no functionality on a platform that
     * doesn't support it.
     * <p>
     * Currently, this support is available on all desktop platforms
     * except Linux systems without the XComposite extension. The
     * XShape extension is used in that case, so the window edges are aliased.
     * </p>
     * <p>
     * If a security manager is present, the application must have
     * the {@link javafx.util.FXPermission} "createTransparentWindow"
     * in order to create a transparent window.
     * If the application doesn't have the required permission, it
     * won't be able to use this feature; it will appear as if the the platform
     * doesn't support transparent windows, and
     * {@code Platform.isSupported(TRANSPARENT_WINDOW)} will return {@code false}.
     * </p>
     *
     * @since JavaFX 2.2
     */
    TRANSPARENT_WINDOW,

    /**
     *  Indicates that a system supports {@link javafx.stage.StageStyle#UNIFIED}
     *  <p>
     *  NOTE: Currently, supported on:
     *  <ul>
     *      <li>Windows Vista+: a window is completely filled with the frozen glass effect</li>
     *      <li>Mac OS X: a window has the brushed-metal textured background</li>
     *  </ul>
     * @since JavaFX 8.0
     */
    UNIFIED_WINDOW,

    /**
     * Indicates whether or not controls should use two-level focus. Two-level
     * focus is when separate operations are needed in some controls to first
     * enter a control and then to perform operations on the control. Two-level
     * focus is needed on devices that provide directional arrow keys and a
     * select key but do not provide keys for navigating between one control
     * and another. On these devices a focused control may be either internally
     * focused - in which case arrow keys operate on the control - or
     * externally focused, in which case arrow keys are used to navigate focus
     * to other controls.
     * <p>
     * On embedded platforms JavaFX makes an attempt to initialize this
     * ConditionalFeature based on what input peripherals are attached. On
     * desktop platforms this ConditionalFeature will typically default to
     * false.
     * @since JavaFX 8.0
     */
     TWO_LEVEL_FOCUS,

    /**
     * Indicates whether an on-screen virtual keyboard is used for text input.
     * <p>
     * On embedded platforms JavaFX makes an attempt to initialize this
     * ConditionalFeature based on what input peripherals are attached. On
     * desktop platforms this ConditionalFeature will typically default to
     * false.
     * @since JavaFX 8.0
     */
     VIRTUAL_KEYBOARD,

    /**
     * Indicates whether or not a touch screen is attached to the device on
     * which JavaFX in running.
     * <p>
     * On embedded platforms JavaFX makes an attempt to initialize this
     * ConditionalFeature based on what input peripherals are attached. On
     * desktop platforms this ConditionalFeature will typically default to
     * false.
     * @since JavaFX 8.0
     */
     INPUT_TOUCH,

    /**
     * Indicates whether or not a touch screen providing multi-touch input is
     * attached to the device on which JavaFX in running.
     * <p>
     * On embedded platforms JavaFX makes an attempt to initialize this
     * ConditionalFeature based on what input peripherals are attached. On
     * desktop platforms this ConditionalFeature will typically default to
     * false.
     * <p>
     * If INPUT_MULTITOUCH is available then INPUT_TOUCH is also available.
     * @since JavaFX 8.0
     */
     INPUT_MULTITOUCH,

    /**
     * Indicates whether or not a relative motion pointer device such as a
     * mouse, trackpad or trackball is attached.
     * <p>
     * On embedded platforms JavaFX makes an attempt to initialize this
     * ConditionalFeature based on what input peripherals are attached. On
     * desktop platforms this ConditionalFeature will typically default to
     * true.
     * @since JavaFX 8.0
     */
    INPUT_POINTER

}
