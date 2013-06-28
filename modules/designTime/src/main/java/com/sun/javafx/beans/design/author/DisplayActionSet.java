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

package com.sun.javafx.beans.design.author;

/**
 * <p>An extension of the DisplayAction interface that introduces a hierarchy (tree) structure.
 * This allows a DisplayAction to become an arbitrary tree of items.  If displayed in a menu,
 * the DisplayActionSet is either a popup item (if isPopup() returns true), or it is displayed as
 * a flat list of items between separators.  If displayed as a button (in an option dialog), the
 * DisplayActionSet defines a button with a popup menu on it.  Note that the 'invoke()' method will
 * never be called in a DisplayActionSet.</p>
 *
 * <P><B>IMPLEMENTED BY THE COMPONENT AUTHOR</B> - This interface is designed to be implemented by
 * the component (bean) author.</P>
 *
 * @author Joe Nuxoll
 * @version 1.0
 */
public interface DisplayActionSet extends DisplayAction {

    /**
     * Returns the list of contained DisplayAction objects.  These will either be shown in a popup
     * or a flat list depending on the 'isPopup' return value.
     *
     * @return An array of DisplayAction objects
     */
    public DisplayAction[] getDisplayActions();

    /**
     * Returns <code>true</code> if this DisplayActionSet should be displayed as a pop-up, or 
     * <code>false</code> if it is should be represented as a flat container (for example, between 
     * separators in a context menu).
     *
     * @return <code>true</code> if this DisplayActionSet should be displayed as a pop-up, or 
     *         <code>false</code> if it is should be represented as a flat container (for example, 
     *         between separators in a context menu)
     */
    public boolean isPopup();
}
