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
import com.sun.javafx.stage.StandardStageBackdrop;
import com.sun.javafx.stage.PlatformStageBackdrop;

/**
 * The backdrop of a {@code Stage}. Backdrops are visual effects drawn across
 * the entire stage behind the Scene's fill and background. The specific
 * effects vary but in general the backdrop will track the window's color
 * scheme. The effect is drawn behind the Scene's fill and background.
 *
 * <p>
 *
 * Backdrops are not supported on all platforms. To check if it is supported
 * see {@link javafx.application.ConditionalFeature#WINDOW_BACKDROP
 * ConditionalFeature.WINDOW_BACKDROP}.
 *
 * <p>
 *
 * Platforms which support backdrops will always provide two standard
 * backdrops, WINDOW and PARTIAL. A platform may also provide a set of
 * backdrops specific to that platform.
 *
 * <p>
 *
 * Backdrop objects are immutable and can be shared across stages.
 *
 * @since 27
 */
@Deprecated(since = "27")
public sealed interface StageBackdrop permits StandardStageBackdrop, PlatformStageBackdrop {

    /**
     * Gets the backdrop's name
     *
     * @return The name of the backdrop.
     */
    public String getName();

    /**
     * Returns a map where each key is the name of a supported backdrop option
     * and each value is the class of object allowed when setting the option.
     *
     * @return A map of the available option names and classes.
     */
    default public Map<String, Class<?>> getAvailableOptions() {
        return Map.of();
    }

    /**
     * Defines a {@code StageBackdrop} appropriate when the backdrop will be
     * visible across the entire stage.
     */
    StageBackdrop WINDOW = StandardStageBackdrop.WINDOW;

    /**
     * Defines a {@code StageBackdrop} appropriate when the backdrop will be
     * visible only along one edge of the stage, perhaps in a tab bar or side
     * panel.
     */
    StageBackdrop PARTIAL = StandardStageBackdrop.PARTIAL;

    /**
     * Gets a list of the standard backdrops.
     *
     * @return A list containing the standard backdrops.
     */
    public static List<StageBackdrop> getStandardBackdrops() {
        return List.of(WINDOW, PARTIAL);
    }

    /**
     * Gets the names of platform backdrops supported on this system. The list
     * may be empty.
     *
     * @return An unmodifiable list of names of the supported platform backdrops.
     */
    public static List<String> getPlatformBackdropNames() {
        return Toolkit.getToolkit().getPlatformBackdropNames();
    }

    /**
     * Creates a platform backdrop for the specified name.
     *
     * @param name The name of the backdrop.
     * @return The backdrop if supported. Otherwise null.
     */
    public static StageBackdrop backdrop(String name) {
        return Toolkit.getToolkit().createPlatformBackdrop(name);
    }
}

