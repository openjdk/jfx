/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javafx.scene.control;

import javafx.geometry.Orientation;

/**
 * <p>
 * A {@link MenuItem} that as the name suggests allows for a horizontal Separator to be embedded within it,
 * by assigning a {@link Separator} to the {@link #contentProperty() content} property of the {@link CustomMenuItem}
 * This is provided for convenience as groups of {@link MenuItem menuitems} can be separated
 * by a separator. Instead of a creating a {@link CustomMenuItem}  for this purpose, the user
 * can use this class as indicated below.
 *
<pre><code>
SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();
</code></pre>
 *
 * @see CustomMenuItem
 * @see MenuItem
 * @see Menu
 * @since JavaFX 2.0
 */
public class SeparatorMenuItem extends CustomMenuItem {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default SeparatorMenuItem instance.
     */
    public SeparatorMenuItem() {
        super(new Separator(Orientation.HORIZONTAL), false);
        getStyleClass().add(DEFAULT_STYLE_CLASS);
    }



    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "separator-menu-item";
}
