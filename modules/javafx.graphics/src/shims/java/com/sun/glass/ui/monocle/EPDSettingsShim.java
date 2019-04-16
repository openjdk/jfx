/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.monocle;

/**
 * Provides access to the {@link EPDSettings} class by making its
 * package-private fields and methods public for test cases in
 * {@link test.com.sun.glass.ui.monocle.EPDSettingsTest EPDSettingsTest}.
 */
public class EPDSettingsShim {

    public final int bitsPerPixel;
    public final int rotate;
    public final int waveformMode;
    public final boolean noWait;
    public final int grayscale;
    public final int flags;

    /**
     * Obtains a new instance of this class with the current values of the EPD
     * system properties.
     *
     * @return a new {@code EPDSettingsShim} instance
     */
    public static EPDSettingsShim newInstance() {
        return new EPDSettingsShim(EPDSettings.newInstance());
    }

    /**
     * Sets the public fields of this object to the corresponding
     * package-private fields of the {@code EPDSettings} instance.
     *
     * @param settings an instance of {@code EPDSettings}
     */
    private EPDSettingsShim(EPDSettings settings) {
        bitsPerPixel = settings.bitsPerPixel;
        rotate = settings.rotate;
        waveformMode = settings.waveformMode;
        noWait = settings.noWait;
        grayscale = settings.grayscale;
        flags = settings.flags;
    }
}
