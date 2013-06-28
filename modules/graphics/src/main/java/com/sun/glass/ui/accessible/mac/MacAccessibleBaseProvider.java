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

import com.sun.glass.ui.accessible.AccessibleBaseProvider;
import com.sun.glass.ui.accessible.AccessibleLogger;
import com.sun.glass.ui.accessible.mac.MacAccessibleAttributes.MacAttribute;
import com.sun.glass.ui.accessible.mac.MacAccessibleEventIds.MacEventId;
import com.sun.glass.ui.accessible.mac.MacAccessibleRoles.MacRole;
import com.sun.javafx.accessible.providers.AccessibleProvider;
import com.sun.javafx.accessible.utils.ControlTypeIds;
import com.sun.javafx.accessible.utils.EventIds;
import com.sun.javafx.accessible.utils.NavigateDirection;
import com.sun.javafx.accessible.utils.PropertyIds;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;

/**
 * Mac platform implementation class for Accessible.
 */
public final class MacAccessibleBaseProvider extends AccessibleBaseProvider {
    
    // map from fx role ID constant to mac role ID enum value
    private static final HashMap<Integer, MacRole>MacRoleMap =
        new HashMap<Integer, MacRole>();
    // map from fx event ID constant to mac event ID enum value
    private static final HashMap<Integer, MacEventId>MacEventIdMap =
        new HashMap<Integer, MacEventId>();
    // map from fx property ID constant to mac attribute ID enum value
    private static final HashMap<Integer, MacAttribute>MacAttributeMap =
        new HashMap<Integer, MacAttribute>();
    // map from MacAttribute enum value to fx property ID constant
    private static final EnumMap<MacAttribute, Integer>FxaAttributeMap =
        new EnumMap<MacAttribute, Integer>(MacAttribute.class);
    
    static {
        _initIDs();  // Initialize JNI method IDs.
        
        // TODO: Add more roles later - in alpha order
        MacRoleMap.put(ControlTypeIds.BUTTON, MacRole.BUTTON);
        MacRoleMap.put(ControlTypeIds.CHECK_BOX, MacRole.CHECK_BOX);
        MacRoleMap.put(ControlTypeIds.RADIO_BUTTON, MacRole.RADIO_BUTTON);
        MacRoleMap.put(ControlTypeIds.TEXT, MacRole.TEXT_FIELD);
        MacRoleMap.put(ControlTypeIds.LIST, MacRole.LIST);
        // TODO: Fix the following later.  Currrently an FX cell is given control ID
        // of List Item with no children.  However it should be List Item with 
        // image/text/edit children.  For Mac the FX cell node should be role cell
        // and the children a relevant role.  For now list item is mapped to static text.
        MacRoleMap.put(ControlTypeIds.LIST_ITEM, MacRole.CELL);
        
        // TODO: Add more later - in alpha order
        MacEventIdMap.put(EventIds.AUTOMATION_FOCUS_CHANGED, MacEventId.FOCUSED_UI_ELEMENT_CHANGED);
        MacEventIdMap.put(EventIds.AUTOMATION_PROPERTY_CHANGED, MacEventId.VALUE_CHANGED);
        
        // TODO: Add more attributes later - in alpha order
        FxaAttributeMap.put(MacAttribute.ENABLED, PropertyIds.IS_ENABLED);
        MacAttributeMap.put(PropertyIds.IS_ENABLED, MacAttribute.ENABLED);
        FxaAttributeMap.put(MacAttribute.FOCUSED, PropertyIds.HAS_KEYBOARD_FOCUS);
        MacAttributeMap.put(PropertyIds.HAS_KEYBOARD_FOCUS, MacAttribute.FOCUSED);
        FxaAttributeMap.put(MacAttribute.ROLE, PropertyIds.CONTROL_TYPE);
        MacAttributeMap.put(PropertyIds.CONTROL_TYPE, MacAttribute.ROLE);
        FxaAttributeMap.put(MacAttribute.SELECTED, PropertyIds.SELECTION_ITEM_IS_SELECTED);
        MacAttributeMap.put(PropertyIds.SELECTION_ITEM_IS_SELECTED, MacAttribute.SELECTED);
        FxaAttributeMap.put(MacAttribute.TITLE, PropertyIds.NAME);
        MacAttributeMap.put(PropertyIds.NAME, MacAttribute.TITLE);
    }
    
    native private static void _initIDs();
    native private long _createAccessible();
    native private void _destroyAccessible(long nativeAccessible);
    native private void _fireEvent(long nativeAccessible, int eventID);
    
    private long nativeAccessible;  // the native accessible
    
    /**
     * Downcall to create the native accessible.  This will be used when firing
     * events or when destroying the native accessible.
     * 
     * @param node      the related FX node object.
     * 
     * @return the native accessible.
     */
    public MacAccessibleBaseProvider(Object node) {
        super(node);
        nativeAccessible = _createAccessible();
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
     * Destroy the native accessible
     */
    @Override
    public void destroyAccessible() {
        _destroyAccessible(nativeAccessible);
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
    
    /** 
     * Fire a property change event
     *
     * @param propertyID        identifies the property
     * @param oldProperty       the old value of the property
     * @param newProperty       the new value of the property
     */
    @Override
    public void firePropertyChange(int propertyID, int oldProperty, int newProperty ) {
        AccessibleLogger.getLogger().fine("this: " + this);
        AccessibleLogger.getLogger().fine("nativeAccessible: " + Long.toHexString(nativeAccessible));
        AccessibleLogger.getLogger().fine("propertyID: " + propertyID);
        AccessibleLogger.getLogger().fine("old: " + oldProperty);
        AccessibleLogger.getLogger().fine("new: " + newProperty);
        _fireEvent(nativeAccessible, MacEventId.VALUE_CHANGED.ordinal());                    
    }
    
    /** 
     * Fire a property change event
     *
     * @param propertyId    identifies the property
     * @param oldProperty   the old value of the property
     * @param newProperty   the new value of the property
     */
    @Override
    public void firePropertyChange(int propertyID, boolean oldProperty, boolean newProperty) {
        AccessibleLogger.getLogger().fine("this: " + this);
        AccessibleLogger.getLogger().fine("nativeAccessible: " + Long.toHexString(nativeAccessible));
        AccessibleLogger.getLogger().fine("propertyID: " + propertyID);
        AccessibleLogger.getLogger().fine("old: " + oldProperty);
        AccessibleLogger.getLogger().fine("new: " + newProperty);
        _fireEvent(nativeAccessible, MacEventId.VALUE_CHANGED.ordinal());                    
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
    //   translates the upcalls to a UIA-like implementation used in the JavaFX
    //   accessibility implementation.
    
    /**
     * Get the children
     * 
     * @offset       the offset to start fetching
     * @maxCount    the max number of children to fetch, -1 means fetch all
     * 
     * @return array of addresses of native accessibles or NULL if no children
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
     * Get the parent
     * 
     * @return address of native accessible of parent or NULL if no parent or -1
     *         if the parent is a root.
     */
    private long getParent() {
        AccessibleLogger.getLogger().fine("this: " + this);
        Object parent;
        long nativeParent = 0;
        // get the parent
        parent = ((AccessibleProvider)node).navigate(NavigateDirection.Parent);
        if (parent == null) {
            AccessibleLogger.getLogger().fine("No parent.");
        } else if (parent instanceof MacAccessibleRoot) {
            nativeParent = -1;
        } else {
            nativeParent = ((MacAccessibleBaseProvider)parent).getNativeAccessible();
        }
        AccessibleLogger.getLogger().fine("returning parent: " + nativeParent);
        return nativeParent;
    }
    
    /**
     * Get the root
     * 
     * Checks the parent and returns it if it is a MacAccessibleRoot.
     * 
     * @return address of native accessible of the root or NULL if no parent
     *         or the parent is not a MacAccessibleRoot
     */
    private long getRoot() {
        AccessibleLogger.getLogger().fine("this: " + this);
        Object root;
        long nativeRoot = 0;
        // get the parent
        root = ((AccessibleProvider)node).navigate(NavigateDirection.Parent);
        if (root == null) {
            AccessibleLogger.getLogger().fine("No parent.");
        } else if (!(root instanceof MacAccessibleRoot)) {
            AccessibleLogger.getLogger().fine("parent is not a MacAccessibleRoot");
            AccessibleLogger.getLogger().fine("it's a: " + root.toString());
        } else {
            nativeRoot = ((MacAccessibleRoot)root).getNativeAccessible();
        }
        AccessibleLogger.getLogger().fine("returning root: " + Long.toHexString(nativeRoot));
        return nativeRoot;
    }
    
    /**
     * Get a property value
     * 
     * @param attributeId   the Mac attribute ID
     * 
     * @return the property value or null if not handled
     */
    @Override
    protected Object getPropertyValue(int attributeId) {
        AccessibleLogger.getLogger().fine("this: " + this);
        AccessibleLogger.getLogger().fine("attributeId: " + attributeId);
        Object value;
        if (attributeId == MacAttribute.ROLE.ordinal()) {
            Integer fxRole =
                (Integer)super.getPropertyValue(FxaAttributeMap.get(MacAttribute.ROLE));
            if (fxRole == null) {
                AccessibleLogger.getLogger().fine("role not in role map");
                value = null;
            } else {
                AccessibleLogger.getLogger().fine("fxRole: " + fxRole);
                value = (Integer)MacRoleMap.get(fxRole).ordinal();
            }
        } else if (attributeId == MacAttribute.TITLE.ordinal()) {
            value = (String)super.getPropertyValue(FxaAttributeMap.get(MacAttribute.TITLE));
        } else if (attributeId == MacAttribute.ENABLED.ordinal()) {
            value = (Boolean)super.getPropertyValue(FxaAttributeMap.get(MacAttribute.ENABLED));
        } else if (attributeId == MacAttribute.FOCUSED.ordinal()) {
            value = (Boolean)super.getPropertyValue(FxaAttributeMap.get(MacAttribute.FOCUSED));
        } else {
            // catch if attribute not handled
            // TODO: raise exception or just return null?
            value = null;
            AccessibleLogger.getLogger().fine("Attribute not handled, returning null");
        }
        AccessibleLogger.getLogger().fine("returning: " + value);
        return value;
    }
    
    //////////////////////////////////
    //
    // End of upcalls from native code
    //
    //////////////////////////////////
    
}
