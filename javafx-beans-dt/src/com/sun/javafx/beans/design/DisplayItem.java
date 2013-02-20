/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.beans.design;

import javafx.scene.image.Image;

/**
 * <P>The DisplayItem interface describes the basic information needed to
 * display an action in a menu or a button. Several interfaces in this API
 * extend this one to provide a basic name, description, icon, etc.</P>
 *
 * @author Joe Nuxoll
 * @version 1.0
 */
public interface DisplayItem {
    /**
     * Returns a display name for this item. This will be used to show in a
     * menu or as a button label, depending on the subinterface.
     *
     * @return A String representing the display name for this item.
     */
    public String getDisplayName();

    /**
     * Returns a description for this item. This will be used as a tooltip in a
     * menu or on a button, depending on the subinterface.
     *
     * @return A String representing the description for this item.
     */
    public String getDescription();

    /**
     * Returns a large image icon for this item. Generally "large" means 32x32
     * pixels.
     *
     * @return An Image representing the large icon for this item.
     */
    public Image getLargeIcon(); //TODO I would prefer "findImage"

    /**
     * Returns a small image icon for this item. Generally "small" means 16x16
     * pixels.
     *
     * @return An Image representing the large icon for this item.
     */
    public Image getSmallIcon(); //TODO I would prefer "findImage"

    /**
     * Returns the help key for this item. This is usually a key used to look up
     * a help context item in an online help facility.
     *
     * @return A String representing the help key for this item.
     */
    public String getHelpKey();
}
