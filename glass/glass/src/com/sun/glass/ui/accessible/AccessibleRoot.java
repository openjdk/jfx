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
package com.sun.glass.ui.accessible;

import com.sun.glass.ui.PlatformFactory;
import com.sun.javafx.accessible.utils.NavigateDirection;
import com.sun.javafx.accessible.utils.Rect;
import com.sun.javafx.accessible.providers.AccessibleProvider;

/**
 * The Accessible for a top level Glass object.
 * <p>
 * Accessibles are created via a call to the static createAccessible method.  Once
 * the platform (Windows, Linux, OS X) is determined a platform specific Accessible
 * is returned.
 * <p>
 * This class is subclassed by platform specific specializations, e.g. WinAccessibleRoot,
 * GtkAccessibleRoot, and MacAccessibleRoot.  During construction each subclass
 * downcalls to native code to create a native accessible.
 * <p>
 * This class implements AccessibleProvider, AccessibleProvider,
 * and AccessibleStageProvider.  Their methods are callable from the native
 * code via JNI.
 */

// PTB ToDo:  This used to implement AccessibleProvider/AccessibleStageProvider.
// This needs to be changed to a different interface so as to not cause conflicts 
// that result in methods in Glass to be called instead of methods in the FX peers
// when objects are cast to interfaces.

//public abstract class AccessibleRoot implements AccessibleProvider, AccessibleStageProvider {
public abstract class AccessibleRoot {
    
    protected Object node;  // This is the node that will be accessed.
    protected long nativeAccessible;  // the native accessible

    /**
     * Static method to create a Glass root accessible
     * 
     * @param node  the related FX node object.
     * @param ptr   the native window handle, e.g HWND, NSWindow*, etc.
     *
     * Determines which platform and then calls the platform specific factory to
     * construct and return a platform specific accessible.
     */
    public static AccessibleRoot createAccessible(Object node, long ptr) {
        AccessibleRoot acc =
            PlatformFactory.getPlatformFactory().createAccessibleRoot(node, ptr);
        return acc;
    }
    
    /**
     * Construct the Java side of the native accessible.  This will be used when
     * firing events or when destroying the native accessible.
     * 
     * @param node  the related FX node object.
     */
    public AccessibleRoot(Object node) {
        this.node = node;
    }

    public long getNativeAccessible() {
        return nativeAccessible;
    }

    // Downcalls
    
    /**
     * Destroy the native accessible
     */
    abstract public void destroyAccessible();
    
    /**
     * Fire an event
     * 
     * @param eventID   identifies the event.
     */
    abstract public void fireEvent(int eventID);
    
    // Upcalls
    
    /**
     * Get a base provider for the control.
     * 
     * @return null for now.
     *
     * PTB: This may not be needed; using UiaHostProviderFromHwnd in the native
     * code might be enough.  We'll have to experiment.
     */
    private AccessibleProvider hostRawElementProvider() {
        return null;
    }

    /**
     * Gets an object providing support for a control pattern.
     * 
     * @param patternId identifies the requested pattern.
     * 
     * @return the object implementing the pattern or null.
     */
    private Object getPatternProvider(int patternId) {
        return null;
    }
    
    /**
     * Gets the value of a property.
     * 
     * @param propertyId    identifies the requested property.
     * 
     * @return the value of the property.
     */
    protected Object getPropertyValue(int propertyId) {
        AccessibleLogger.getLogger().fine("In AccessibleRoot.getPropetyValue propertyID : "+propertyId);
        Object value;
        if (node instanceof AccessibleProvider) {
            value = ((AccessibleProvider)node).getPropertyValue(propertyId);
        } else {
            // PTB: Should we do something else like throw an acception?
            value = null;
        }
        AccessibleLogger.getLogger().fine("  returning: " + value);
        return value;
    }
      
    /**
     * Get the bounding rectangle of this element.
     *
     * @return the bounding rectangle (x, y, w, h), in screen coordinates, 
     * with respect to upper left corner.
     */
    private Rect boundingRectangle() {
        AccessibleLogger.getLogger().fine("In AccessibleRoot.boundingRectangle");
        Rect rect;
        if (node instanceof AccessibleProvider) {
            rect = ((AccessibleProvider)node).boundingRectangle();
        } else {
            // PTB: Should I do something else like throw an acception?
            rect = null;
        }
        AccessibleLogger.getLogger().fine("  returning: " + rect);
        return rect;
    }
    
    /**
     * Get the root node of the fragment.
     * 
     * @return the root node.
     */
    private Object fragmentRoot() {
        if (node instanceof AccessibleProvider) {
            return ((AccessibleProvider)node).fragmentRoot();
        } else {
            return null; 
        }
    }

    /**
     * Get an array of fragment roots that are embedded in the UI Automation
     * element tree rooted at the current element.
     * 
     * @return an array of root fragments, or null.
     */
    private AccessibleProvider[] getEmbeddedFragmentRoots() {
        return null;
    }

    /**
     * Get the runtime identifier of an element.
     * 
     * @return the unique run-time identifier of the element.
     */
    private int[] getRuntimeId() {
        return null;
    }
    
    /**
     * Get the UI Automation element in a specified direction within the tree.
     * 
     * @param direction the direction in which to navigate.
     * 
     * @return the element in the specified direction, or null if there is no element
     *         in that direction
     */
    private Object navigate(NavigateDirection direction) {
        if (node instanceof AccessibleProvider) {
            return ((AccessibleProvider)node).navigate(direction);
        } else {
            return null; // PTB: Can this ever happen?  Throw exception instead?
        }
    }
    
    private void setFocus() {}
   
    private long elementProviderFromPoint(double x, double y) {
        return 0;
    }
    
    private AccessibleProvider getFocus() {
        return null;
    }
}
