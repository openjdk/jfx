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

package com.sun.javafx.stage;

import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.util.Utils;
import javafx.application.ColorScheme;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.util.function.DoubleSupplier;

public final class StageUtils {

    private StageUtils() {}

    public static boolean isDarkFrame(Stage stage) {
        ColorScheme colorScheme = stage.getScene() instanceof Scene scene
            ? scene.getPreferences().getColorScheme()
            : PlatformImpl.getPlatformPreferences().getColorScheme();

        return colorScheme == ColorScheme.DARK;
    }

    public static Color calculateRepresentativeColor(Stage stage) {
        if (!(stage.getScene() instanceof Scene scene)) {
            return isDarkFrame(stage) ? Color.BLACK : Color.WHITE;
        }

        // Try to get the background size from the scene or stage, and assume 512x512 as a fallback.
        // The background size is required to approximate a sensible background color for gradient
        // paints that are specified in absolute coordinates.
        double width = getDoubleValue(scene::getWidth, stage::getWidth, 512);
        double height = getDoubleValue(scene::getHeight, stage::getHeight, 512);
        var background = scene.getPreferences().getColorScheme() == ColorScheme.DARK ? Color.BLACK : Color.WHITE;
        return Utils.calculateRepresentativeColor(scene.getFill(), background, width, height);
    }

    private static double getDoubleValue(DoubleSupplier first, DoubleSupplier second, double fallback) {
        double value = Double.isNaN(first.getAsDouble()) ? 0 : first.getAsDouble();
        if (value != 0) {
            return value;
        }

        value = Double.isNaN(second.getAsDouble()) ? 0 : second.getAsDouble();
        if (value != 0) {
            return value;
        }

        return fallback;
    }
}
