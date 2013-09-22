/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.core;

import javafx.scene.text.Font;

/**
 * Util class for getting platform fonts
 */
public final class Fonts {
    private static final String DOSIS_EXTRA_LIGHT_NAME;
    private static final String DOSIS_SEMI_BOLD_NAME;
    private static String extraLightName;
    private static String semiBoldName;

    static {
        try {
            extraLightName = Font.loadFont(Fonts.class.getResourceAsStream("/fonts/Dosis-ExtraLight.ttf"),10).getName();
            semiBoldName = Font.loadFont(Fonts.class.getResourceAsStream("/fonts/Dosis-SemiBold.ttf"),10).getName();
        } catch (Exception e) { }
        DOSIS_EXTRA_LIGHT_NAME = extraLightName;
        DOSIS_SEMI_BOLD_NAME = semiBoldName;
        System.out.println("DOSIS_SEMI_BOLD_NAME = " + DOSIS_SEMI_BOLD_NAME);
    }

    public static Font dosisExtraLight(double size) {
        return new Font(DOSIS_EXTRA_LIGHT_NAME, size);
    }

    public static Font dosisSemiBold(double size) {
        return new Font(DOSIS_SEMI_BOLD_NAME, size);
    }
}
