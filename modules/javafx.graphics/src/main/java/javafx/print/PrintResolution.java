/*
 * Copyright (c) 2013, 2025, Oracle and/or its affiliates. All rights reserved.
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

package javafx.print;

/**
 * Class to represent a supported device resolution of a printer in
 * the feed and crossfeed directionsin dots-per-inch (DPI).
 * When printing in a portrait orientation
 * cross feed direction is usually x/horizontal resolution, and
 * feed direction is usually y/horizontal resolution.
 * On most printers these are the same value, but it is possible
 * for them to be different.
 * @since JavaFX 8.0
 */

public final class PrintResolution {

    private int cfRes;
    private int fRes;

    /**
     * Represents the dots-per-inch (DPI) resolution of a printer device.
     * When printing in a portrait orientation
     * cross feed direction is usually x/horizontal resolution, and
     * feed direction is usually y/horizontal resolution.
     * On most printers these are the same value, but rarely they may be
     * different.
     * @param crossFeedResolution - resolution across the paper feed direction.
     * @param feedResolution - resolution in the paper feed direction.
     * @throws IllegalArgumentException if the values are not greater
     * than zero.
     */
     PrintResolution(int crossFeedResolution, int feedResolution)
        throws IllegalArgumentException
    {
        if (crossFeedResolution <= 0 || feedResolution <= 0) {
            throw new IllegalArgumentException("Values must be positive");
        }
        cfRes = crossFeedResolution;
        fRes  = feedResolution;
    }

    /**
     * Returns the resolution in dpi. across the paper feed direction.
     * @return cross feed resolution.
     */
    public int getCrossFeedResolution() {
        return cfRes;
    }

    /**
     * Returns the resolution in dpi. in the paper feed direction.
     * @return feed resolution.
     */
    public int getFeedResolution() {
        return fRes;
    }

    @Override
    public boolean equals(Object o) {
        try {
            PrintResolution other = (PrintResolution)o;
            return this.cfRes == other.cfRes && this.fRes == other.fRes;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return cfRes << 16 | fRes;
    }

    @Override
    public String toString() {
        return "Feed res=" + fRes + "dpi. Cross Feed res=" + cfRes + "dpi.";
    }

}
