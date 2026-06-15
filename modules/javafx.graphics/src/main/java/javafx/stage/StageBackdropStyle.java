/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package javafx.stage;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;

import java.util.List;
import java.util.Map;

import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.stage.StandardStageBackdropStyle;
import com.sun.javafx.stage.PlatformStageBackdropStyle;

/**
 * The backdrop style of a {@code Stage}. Backdrops are visual effects drawn
 * across the entire stage behind the Scene's fill and background. The
 * specific effects vary but in general the backdrop will track the window's
 * color scheme.
 *
 * <p>
 *
 * Backdrops are not supported on all platforms. To check if backdrops are
 * supported see {@link javafx.application.ConditionalFeature#WINDOW_BACKDROP
 * ConditionalFeature.WINDOW_BACKDROP}.
 *
 * <p>
 *
 * Platforms which support backdrops will always provide two standard backdrop
 * styles, WINDOW and PARTIAL. A platform may also provide a set of backdrop
 * styles specific to that platform.
 *
 * <p>
 *
 * Backdrop style objects are immutable and can be shared across stages.
 *
 * <p>
 *
 * Some backdrop styles support options which can be set dynamically. The
 * style provides the list of available options. To set an option for a
 * stage's backdrop see {@link Stage#getBackdrop}.
 * @since 27
 */
@Deprecated(since = "27")
public sealed interface StageBackdropStyle permits StandardStageBackdropStyle, PlatformStageBackdropStyle {

    /**
     * Gets the backdrop style's name.
     *
     * @return the name of the backdrop style
     */
    public String getName();

    /**
     * Returns a map where each key is the name of a supported backdrop option
     * and each value is the class of object allowed when setting the option.
     *
     * @return a map of the available option names and classes
     */
    default public Map<String, Class<?>> getAvailableOptions() {
        return Map.of();
    }

    /**
     * Defines a {@code StageBackdropStyle} appropriate when the backdrop will
     * be visible across the entire stage.
     */
    StageBackdropStyle WINDOW = StandardStageBackdropStyle.WINDOW;

    /**
     * Defines a {@code StageBackdropStyle} appropriate when the backdrop will
     * be visible only along one edge of the stage, perhaps in a tab bar or
     * side panel.
     */
    StageBackdropStyle PARTIAL = StandardStageBackdropStyle.PARTIAL;

    /**
     * Gets a list of the standard backdrop styles.
     *
     * @return a list containing the standard backdrop styles
     */
    public static List<StageBackdropStyle> getStandardStyles() {
        return List.of(WINDOW, PARTIAL);
    }

    /**
     * Gets the names of platform backdrop styles supported on this system.
     * The list may be empty.
     *
     * @return an unmodifiable list of names of the supported platform
     *  backdrop styles
     */
    public static List<String> getPlatformStyleNames() {
        return Toolkit.getToolkit().getPlatformBackdropStyleNames();
    }

    /**
     * Creates a platform backdrop style for the specified name.
     *
     * @param name the name of the backdrop style
     * @return the backdrop style if supported, otherwise null.
     */
    public static StageBackdropStyle style(String name) {
        return Toolkit.getToolkit().createPlatformBackdropStyle(name);
    }
}
