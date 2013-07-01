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
import com.sun.javafx.accessible.providers.AccessibleProvider;
import com.sun.javafx.accessible.utils.Rect;
import java.util.ArrayList;
import java.util.List;

/**
 * The Accessible for a Glass accessible object.
 * <p>
 * Accessibles are created via a call to the static createProvider method.  Once
 * the platform (Windows, Linux, OS X) is determined a platform specific Accessible
 * is returned.
 * <p>
 * This class is subclassed by platform specific specializations, e.g.
 * WinAccessibleBaseProvider, MacAccessibleBaseProvider.  During construction each
 * subclass downcalls to native code to create a native accessible.
 */

public abstract class AccessibleBaseProvider {
    
    protected List<AccessibleBasePatternProvider> patternProviders;
    
    protected Object node;  // the JavaFX node

    /**
     * Static method to create a Glass accessible
     * 
     * Determines which platform and then calls the platform specific factory to
     * construct and return a platform specific accessible.
     * 
     * @param node  the related FX node object.
     *
     * @return a newly created platform specific AccessibleBaseProvider.
     */
    public static AccessibleBaseProvider createProvider(Object node) {
        AccessibleLogger.getLogger().fine("node: " + node);            
        return PlatformFactory.getPlatformFactory().createAccessibleProvider(node);
    }
    
    /**
     * Constructor
     * 
     * @param node  the related FX node object.
     */
    public AccessibleBaseProvider(Object node) {
        this.node = node;
        patternProviders = new ArrayList<AccessibleBasePatternProvider>();
    }
    
    /**
     * Get the FX accessible node.
     * 
     * @return the FX accessible node.
     */
    public Object getNode() {
        return node;
    }
    
    /**
     * Add a pattern provider to the base provider.
     * 
     * @param pattern the pattern provider to add
     */
    public void addPatternProviders(AccessibleBasePatternProvider pattern) {
        patternProviders.add(pattern);
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

    /** Fire a property change event
     * 
     * @param propertyId    identifies the property
     * @param oldProperty   the old value of the property
     * @param newProperty   the new value of the property
     */
    abstract public void firePropertyChange(int propertyId, int oldProperty, int newProperty);

    /** Fire a property change event
     * 
     * @param propertyId    identifies the property
     * @param oldProperty   the old value of the property
     * @param newProperty   the new value of the property
     */
    abstract public void firePropertyChange( int propertyId,
                                             boolean oldProperty, boolean newProperty );
    
    // Upcalls
    
    /**
     * Gets the value of a property.
     * 
     * @param propertyId    identifies the requested property.
     * 
     * @return the value of the property.
     */
    protected Object getPropertyValue(int propertyId) {
        AccessibleLogger.getLogger().fine("propertyID: " + propertyId);
        Object value;
        if (node instanceof AccessibleProvider) {
            value = ((AccessibleProvider)node).getPropertyValue(propertyId);
        } else {
            // TODO: Should we do something else like throw an acception?
            value = null;
        }
        AccessibleLogger.getLogger().fine("returning: " + value);
        return value;
    }

    /**
     * Get the bounding rectangle of this element.
     *
     * @return the bounding rectangle (x, y, w, h), in screen coordinates, 
     * with respect to upper left corner.
     */
    private Rect boundingRectangle() {
        Rect rect;
        if (node instanceof AccessibleProvider) {
            rect = ((AccessibleProvider)node).boundingRectangle();
            AccessibleLogger.getLogger().fine("returning: " +
                "MinX=" + rect.getMinX() + "MinY=" + rect.getMinY() + 
                "MaxX=" + rect.getMaxX() + "MaxY=" + rect.getMaxY());
        } else {
            // TODO: Should we do something else like throw an acception?
            rect = null;
        }
        return rect;
    }    

    /**
     * Set focus to this object.
     */
    private void setFocus() {
        if (node instanceof AccessibleProvider) {
            ((AccessibleProvider)node).setFocus();
        }
    }

}
