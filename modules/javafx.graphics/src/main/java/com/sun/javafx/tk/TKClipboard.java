/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk;

import java.security.AccessControlContext;
import java.util.Set;
import javafx.scene.image.Image;

import javafx.scene.input.DataFormat;
import javafx.scene.input.TransferMode;
import javafx.util.Pair;

/**
 * We use this interface to represent the PG peer for the public Clipboard
 * and Dragboard APIs, so that we don't leak the QuantumClipboard to
 * callers (and so forth).
 */
public interface TKClipboard {

    /**
     * This method is used to set security context of the Stage.
     */
    public void setSecurityContext(@SuppressWarnings("removal") AccessControlContext ctx);

    /**
     * Gets the set of DataFormat types on this Clipboard instance which have
     * associated data registered on the clipboard. This set will always
     * be non-null and immutable. If the Clipboard is subsequently modifed,
     * this returned set is not updated.
     *
     * @return A non-null immutable set of content types.
     */
    public Set<DataFormat> getContentTypes();

    /**
     * Puts the specified content onto the clipboard. Note that all existing content is
     * cleared from the clipboard prior to the specified content being added.
     * A <code>NullPointerException</code> is thrown if either the <code>DataFormat</code>
     * or the <code>Object</code> data in the content <code>Pair</code> is null.
     *
     * @param content The content to put on the clipboard.
     * @return True if successful, false if the content fails to be added.
     */
    public boolean putContent(Pair<DataFormat, Object>... content);

    /**
     * Returns the content stored in this clipboard of the given type, or null
     * if there is no content with this type.
     * @return The content associated with this type, or null if there is none
     */
    public Object getContent(DataFormat dataFormat);

    /**
     * Tests whether there is any content on this clipboard of the given DataFormat type.
     * @return true if there is content on this clipboard for this type
     */
    public boolean hasContent(DataFormat dataFormat);

    // for DnD
    public Set<TransferMode> getTransferModes();

    /**
     * Sets the visual representation of data being transfered in a drag and drop gesture.
     * @param image image to use for the drag view
     */
    public void setDragView(Image image);

    /**
     * Sets the x position of the cursor of the drag view image.
     * @param offsetX x position of the cursor over the image
     */
    public void setDragViewOffsetX(double offsetX);

    /**
     * Sets the y position of the cursor of the drag view image.
     * @param offsetY x position of the cursor over the image
     */
    public void setDragViewOffsetY(double offsetY);

    /**
     * Gets the image used as a drag view.
     * @return the image used as a drag view
     */
    public Image getDragView();

    /**
     * Gets the x position of the cursor of the drag view image.
     * @return x position of the cursor over the image
     */
    public double getDragViewOffsetX();

    /**
     * Gets the y position of the cursor of the drag view image.
     * @return y position of the cursor over the image
     */
    public double getDragViewOffsetY();
}
