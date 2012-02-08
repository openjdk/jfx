/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.jmx;

import javafx.geometry.Rectangle2D;

import com.sun.javafx.tk.TKScene;

/**
 * This class encapsulates Rectangle2D and Scene to create a convenience class
 * for highlight region representation.
 */
public class HighlightRegion extends Rectangle2D {

    private TKScene tkScene;

    /**
     * Cache the hash code to make computing hashes faster.
     */
    private int hash = 0;

    /**
     * Constructs a new HighlightRegion.
     * 
     * @param tkScene the scene this region belongs to.
     * @param x the x coordinate of the region
     * @param y the y coordinate of the region
     * @param w the width of the region
     * @param h the height of the region
     */
    public HighlightRegion(TKScene tkScene, double x, double y, double w, double h) {
        super(x, y, w, h);
        this.tkScene = tkScene;
    }

    /**
     * Getter for the scene attribute.
     *
     * @return the scene.
     */
    public TKScene getTKScene() {
        return tkScene;
    }

    /**
     * @inheritDoc
     */
    @Override public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof HighlightRegion) {
            HighlightRegion other = (HighlightRegion) obj;
            return tkScene.equals(other.tkScene)
                && super.equals(other);
        } else return false;
    }

    /**
     * @inheritDoc
     */
    @Override public int hashCode() {
        if (hash == 0) {
            long bits = 7L;
            bits = 31L * bits + super.hashCode();
            bits = 31L * bits + tkScene.hashCode();
	          hash = (int) (bits ^ (bits >> 32));
        }
        return hash;
    }

    /**
     * Returns a string representation of this {@code HighlighRegion}.
     * This method is intended to be used only for informational purposes.
     * The content and format of the returned string might vary between
     * implementations.
     * The returned string might be empty but cannot be {@code null}.
     *
     * @profile common
     */
    @Override public String toString() {
        return "HighlighRegion [tkScene = " + tkScene
                + ", rectangle = " + super.toString()
                + "]";
    }
}
