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

package javafx.print;

/**
 * A PaperSource is the input tray to be used for the Paper.
 * The enumerated values here cover many of the most common sources,
 * which may map to platform IDs. However there are also printer specific
 * tray names which may be in use. So queries of the supported paper
 * sources may include other values.
 *
 * @since JavaFX 8.0
 */
public final class PaperSource {

    /**
     * Specify to automatically select the tray.
     */
    public static final PaperSource AUTOMATIC = new PaperSource("Automatic");

    /**
     * Specify to select the MAIN tray.
     */
    public static final PaperSource MAIN      = new PaperSource("Main");

    /**
     * Specify to select the MANUAL tray.
     */
    public static final PaperSource MANUAL    = new PaperSource("Manual");

    /**
     * Specify to select the BOTTOM tray.
     */
    public static final PaperSource BOTTOM    = new PaperSource("Bottom");

    /**
     * Specify to select the MIDDLE tray.
     */
    public static final PaperSource MIDDLE    = new PaperSource("Middle");

    /**
     * Specify to select the TOP tray.
     */
    public static final PaperSource TOP       = new PaperSource("Top");

    /**
     * Specify to select the SIDE tray.
     */
    public static final PaperSource SIDE      = new PaperSource("Side");

    /**
     * Specify to select the ENVELOPE tray.
     */
    public static final PaperSource ENVELOPE  = new PaperSource("Envelope");

    /**
     * Specify to select the LARGE_CAPACITY tray.
     */
    public static final PaperSource LARGE_CAPACITY =
        new PaperSource("Large Capacity");


    private String name;

    PaperSource(String sourceName) {
        if (sourceName != null) {
            name = sourceName;
        } else {
            name = "Unknown";
        }
    }

    /**
     * Returns the name of this paper source.
     * @return paper source name.
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Paper source : " + getName();
    }
}
