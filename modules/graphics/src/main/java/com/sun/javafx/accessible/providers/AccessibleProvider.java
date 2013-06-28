/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.accessible.providers;

import com.sun.javafx.accessible.utils.NavigateDirection;
import com.sun.javafx.accessible.utils.Rect;

/**
 *
 */
public interface AccessibleProvider {
    ////
    // Simple methods
    ////
    
    // Summary:
    //     Gets a base provider for this element.
    //
    // Returns:
    //     The base provider, or null.
    public AccessibleProvider hostRawElementProvider();

    // Summary:
    //     Retrieves an object that provides support for a control pattern on a UI Automation
    //     element.
    //
    // Parameters:
    //   patternId:
    //     Identifier of the pattern.
    //
    // Returns:
    //     Object that implements the pattern interface, or null if the pattern is not
    //     supported.
    public Object getPatternProvider(int patternId);
    //
    // Summary:
    //     Retrieves the value of a property supported by the UI Automation provider.
    //
    // Parameters:
    //   propertyId:
    //     The property identifier.
    //
    // Returns:
    //     The property value, or a null if the property is not supported by this provider,
    //     or System.Windows.Automation.AutomationElementIdentifiers.NotSupported if
    //     it is not supported at all.
    public Object getPropertyValue(int propertyId);

    ////
    // Fragment methods
    ////
    
    // Summary:
    //     Gets the bounding rectangle of this element.
    //
    // Returns:
    //     The bounding rectangle, in screen coordinates.
    //public Rectangle BoundingRectangle() ;
    // FIXIT to Rectangle
    public Rect boundingRectangle() ;
    //
    // Summary:
    //     Retrieves the root node of the fragment.
    //
    // Returns:
    //     The root node.
//    public AccessibleStageProvider fragmentRoot();
    public Object fragmentRoot();

    // Summary:
    //     Retrieves an array of fragment roots that are embedded in the UI Automation
    //     element tree rooted at the current element.
    //
    // Returns:
    //     An array of root fragments, or null.
    public AccessibleProvider[] getEmbeddedFragmentRoots();
    //
    // Summary:
    //     Retrieves the runtime identifier of an element.
    //
    // Returns:
    //     The unique run-time identifier of the element.
    public int[] getRuntimeId();
    //
    // Summary:
    //     Retrieves the UI Automation element in a specified direction within the tree.
    //
    // Parameters:
    //   direction:
    //     The direction in which to navigate.
    //
    // Returns:
    //     The element in the specified direction, or null if there is no element in
    //     that direction
    public Object navigate(NavigateDirection direction);
    //
    // Summary:
    //     Sets the focus to this element.
    public void setFocus();
}
