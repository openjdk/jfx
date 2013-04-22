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
package com.sun.glass.ui.accessible.mac;

import com.sun.glass.ui.Window;
import com.sun.glass.ui.accessible.AccessibleLogger;
import com.sun.glass.ui.accessible.AccessibleRoot;
import com.sun.glass.ui.accessible.mac.MacAccessibleAttributes.MacAttribute;
import com.sun.glass.ui.accessible.mac.MacAccessibleEventIds.MacEventId;
import com.sun.glass.ui.accessible.mac.MacAccessibleRoles.MacRole;
import com.sun.javafx.accessible.providers.AccessibleProvider;
import com.sun.javafx.accessible.providers.AccessibleStageProvider;
import com.sun.javafx.accessible.utils.ControlTypeIds;
import com.sun.javafx.accessible.utils.EventIds;
import com.sun.javafx.accessible.utils.NavigateDirection;
import com.sun.javafx.accessible.utils.PropertyIds;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;

/**
 * Mac platform implementation class for AccessibleRoot.
 */
public final class MacAccessibleRoot extends AccessibleRoot {
    
    final private static HashMap<Integer, MacRole>MacRoleMap =
        new HashMap<Integer, MacRole>();
    final private static HashMap<Integer, MacEventId>MacEventIdMap =
        new HashMap<Integer, MacEventId>();
    final private static EnumMap<MacAttribute, Integer>FxaAttributeMap =
        new EnumMap<MacAttribute, Integer>(MacAttribute.class);
    
    static {
        _initIDs();  // Initialize the method Ids
        
        // TODO: Add more roles later - in alpha order
        MacRoleMap.put(ControlTypeIds.BUTTON, MacRole.BUTTON);
        MacRoleMap.put(ControlTypeIds.CHECK_BOX, MacRole.CHECK_BOX);
        MacRoleMap.put(ControlTypeIds.RADIO_BUTTON, MacRole.RADIO_BUTTON);
        
        // TODO: Add more later - in alpha order
        MacEventIdMap.put(EventIds.AUTOMATION_FOCUS_CHANGED, MacEventId.FOCUSED_UI_ELEMENT_CHANGED);
        
        // TODO: Add more attributes later - in alpha order
        FxaAttributeMap.put(MacAttribute.ROLE, PropertyIds.CONTROL_TYPE);
    }
    
    native private static void _initIDs();
    native private long _createAccessible();
    native private void _setAccessibilityInitIsComplete(long nativeWindow, long nativeAccessible);
    native private void _destroyAccessible(long nativeAccessible);
    native private void _fireEvent(long nativeAccessible, int eventID);
    
    private long nativeWindow;  // the native window object
    private long nativeAccessible;  // the native accessible
    
    /**
     * Construct the platform dependent Java side of the native accessible.  This
     * will be used when firing events or when destroying the native accessible.
     * 
     * @param node      the related FX node object.
     * @param window    the top level Glass Window object (not used in this implementation).
     */
    public MacAccessibleRoot(Object node, Window window) {
        super(node);
        nativeAccessible = _createAccessible();
        nativeWindow = window.getNativeWindow();
    }
    
    /**
     * Get the reference to the native accessible.
     * 
     * @return a reference to the native accessible.
     */
    long getNativeAccessible() {
        return nativeAccessible;
    }
    
    ////////////////////////////////////
    //
    // Start of downcalls to native code
    //
    ////////////////////////////////////
    
    /**
     * Signal that initialization is complete.
     */
    @Override
    public void setAccessibilityInitIsComplete() {
        _setAccessibilityInitIsComplete(nativeWindow, nativeAccessible);
    }
    
    /**
     * Destroy the native accessible
     */
    @Override
    public void destroyAccessible() {
        if (nativeAccessible != 0) {
            _destroyAccessible(nativeAccessible);
        }
    }
    
    /**
     * Downcall to fire an event.
     * 
     * @param eventID   the FXA event ID.
     */
    @Override
    public void fireEvent(int eventID) {
        AccessibleLogger.getLogger().fine("this: " + this);
        AccessibleLogger.getLogger().fine("nativeAccessible: " + Long.toHexString(nativeAccessible));
        AccessibleLogger.getLogger().fine("eventID: " + eventID);
        _fireEvent(nativeAccessible, MacEventIdMap.get(eventID).ordinal());
    }
    
    //////////////////////////////////
    //
    // End of downcalls to native code
    //
    //////////////////////////////////
    
    ////////////////////////////////////
    //
    // Start of upcalls from native code
    //
    ////////////////////////////////////
    
    // Note:
    //   These upcalls are from a native NSAccessibility implementation.  This code
    //   translates the upcalls to s UIA-like implementation used in the JavaFX
    //   accessibility implementation.
    
    /**
     * Get the children
     * 
     * @offset      the offset to start fetching
     * @maxCount    the max number of children to fetch, -1 means fetch all
     * 
     * @return array of addresses of native accessibles or null if no children
     */
    private long[] getChildren(int offset, int maxCount) {
        AccessibleLogger.getLogger().fine("this: " + this);
        AccessibleLogger.getLogger().fine("offset: " + offset);
        AccessibleLogger.getLogger().fine("maxCount: " + maxCount);
        List<Long> children = new ArrayList<Long>(100);
        if (maxCount == 0 || maxCount < -1 || offset < 0) {
            return null;
        }
        boolean getAll = (maxCount == -1);
        // get the first child
        Object firstGlassChild =
            ((AccessibleProvider)node).navigate(NavigateDirection.FirstChild);
        if (firstGlassChild == null) {
            AccessibleLogger.getLogger().fine("No children.");
        } else {
            Object glassChild;
            AccessibleProvider childFXNode;
            int index = 0;
            int count = 0;
            // if caller wants the first one, save it
            if (offset == 0) {
                Long nativeChild =
                    new Long(((MacAccessibleBaseProvider)firstGlassChild).getNativeAccessible());
                children.add(nativeChild);
                ++count;
            }
            ++index;
            // Fill the array with sequential children, starting at offset.
            // The first fetch might be all that's needed.
            if (getAll || count < maxCount) {  
                // get next sibling until done
                glassChild = firstGlassChild;                
                do {
                    childFXNode = (AccessibleProvider)((MacAccessibleBaseProvider)glassChild).getNode();
                    glassChild = childFXNode.navigate(NavigateDirection.NextSibling);
                    if (index >= offset && glassChild != null) {
                        Long nativeChild =
                            new Long(((MacAccessibleBaseProvider)glassChild).getNativeAccessible());
                        children.add(nativeChild);
                        ++count;
                    }
                    ++index;
                } while ((getAll || count < maxCount) && glassChild != null);
            }
        }
        AccessibleLogger.getLogger().fine("children.size: " + children.size());
        if (children.isEmpty()) {
            return null;
        } else {
            long[] longs = new long[children.size()];
            int i = 0;
            AccessibleLogger.getLogger().fine("returning");
            for (Long e : children)  {
                AccessibleLogger.getLogger().fine("child [" + i + "]: " + Long.toHexString(e));
                longs[i++] = e.longValue();
            }
            return longs;
        }
    }
    
    /**
     * Get a property value
     * 
     * @param attributeId   the Mac attribute ID
     * @return the property value
     */
    @Override
    protected Object getPropertyValue(int attributeId) {
        AccessibleLogger.getLogger().fine("this: " + this);
        AccessibleLogger.getLogger().fine("attributeId: " + attributeId);
        Object value;
        if (attributeId == MacAttribute.ROLE.ordinal()) {
            Integer role = (Integer)super.getPropertyValue(FxaAttributeMap.get(MacAttribute.ROLE));
            value = MacRoleMap.get(role);
            AccessibleLogger.getLogger().fine("returning Mac role: " + value);
        }
        if (attributeId == MacAttribute.TITLE.ordinal()) {
            value = null;  // todo
        } else {
            // catch if attribute not handled
            // TODO: raise exception or just return null?
            value = null;
            AccessibleLogger.getLogger().fine("Attribute not handled, returning null");
        }
        return value;
    }
    
    /**
     * Get the native provider at the specified point.
     * 
     * @param   x
     * @param   y 
     * 
     * @return the native provider
     */
    private long elementProviderFromPoint(double x, double y) { 
        AccessibleLogger.getLogger().fine("this: " + this);
        if (node instanceof AccessibleStageProvider) {
            AccessibleLogger.getLogger().fine("x: " + x + " y: " + y);
            Object accElement = ((AccessibleStageProvider)node).elementProviderFromPoint(x, y);
            if (accElement != null) {
                long nativeObject =
                    ((MacAccessibleBaseProvider)accElement).getNativeAccessible();
                AccessibleLogger.getLogger().fine("native element: " + nativeObject);
                return nativeObject;
            } else {
                AccessibleLogger.getLogger().fine("Not Found");
                return 0;
            }
        } else {
            return 0;            
        }

   }
    //////////////////////////////////
    //
    // End of upcalls from native code
    //
    //////////////////////////////////
    
}
