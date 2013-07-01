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
package com.sun.glass.ui.accessible.win;

import com.sun.glass.ui.Window;
import com.sun.glass.ui.accessible.AccessibleLogger;
import com.sun.glass.ui.accessible.AccessibleRoot;
import com.sun.javafx.accessible.providers.AccessibleProvider;
import com.sun.javafx.accessible.utils.NavigateDirection;
import java.util.HashMap;

/**
 * Windows platform implementation class for AccessibleRoot.
 */
public final class WinAccessibleRoot extends AccessibleRoot {
    
    private static final HashMap<Integer, NavigateDirection> directionMap =
        new HashMap<Integer, NavigateDirection>();

    static {
        // Initialize the JNI method IDs.
        _initIDs();
        
        // Initialize directionMap
        for (NavigateDirection type : NavigateDirection.values()) {
            directionMap.put(type.ordinal(), type);
        }
    }
    
    native private static void _initIDs();
    native private long _createAccessible();
    native private void _setAccessibilityInitIsComplete(long hwnd, long nativeAccessible);
    native private void _destroyAccessible(long nativeAccessible);
    native private void _fireEvent(long nativeAccessible, int eventID);
    
    private long hwnd;  // the native window handle
    private long nativeAccessible;  // the native accessible
    
    /**
     * Construct the platform dependent Java side of the native accessible.  This
     * will be used when firing events or when destroying the native accessible.
     * 
     * @param node      the related FX node object.
     * @param window    the associated top level Glass Window object.
     */
    public WinAccessibleRoot(Object node, Window window) {
        super(node);
        nativeAccessible = _createAccessible();
        hwnd = window.getNativeWindow();
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
    
    /*
     * Signal that initialization is complete.
     */
    @Override
    public void setAccessibilityInitIsComplete() {
        _setAccessibilityInitIsComplete(hwnd, nativeAccessible);
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
     * Fire an event
     * 
     * @param eventID   identifies the event.
     */
    @Override
    public void fireEvent(int eventID) {
        AccessibleLogger.getLogger().fine("this: " + this);
        AccessibleLogger.getLogger().fine("nativeAccessible: " + Long.toHexString(nativeAccessible));
        AccessibleLogger.getLogger().fine("eventID: " + eventID);
        _fireEvent(nativeAccessible, eventID);
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
    //   These upcalls are from a native UIA implementation.  This code translates
    //   the upcalls to the UIA-like implementation used in the JavaFX accessibility 
    //   implementation.
    
    /**
     * For AccessibleProvider - get_HostRawElementProvider
     *
     * @return address of native accessible of root object
     */
    private long getHostHwnd() {
        //AccessibleLogger.getLogger().fine("  Thread ID: " + Thread.currentThread().getId());
        AccessibleLogger.getLogger().fine("Returning hwnd: " + Long.toHexString(hwnd));
        return hwnd;
    }
    
    /**
     * For AccessibleProvider - Navigate
     * 
     * @param direction  parent, first/last child, prev/next sibling
     *
     * @return address of the requested native accessible or 0 if there is no target
     *         in the requested direction.
     * 
     * Note: Roots have no parents or siblings.
     */
    private long navigate(int direction) {
        //AccessibleLogger.getLogger().fine("  Thread ID: " + Thread.currentThread().getId());
        AccessibleLogger.getLogger().fine("direction: " + directionMap.get(direction));
        Object target =
            ((AccessibleProvider)node).navigate(directionMap.get(direction));
        if (target == null) {
            AccessibleLogger.getLogger().fine("No object in that direction.");
            return 0;
        } else {
            long nativeTarget = ((WinAccessibleBaseProvider)target).getNativeAccessible();
            AccessibleLogger.getLogger().fine("nativeTarget:  " + Long.toHexString(nativeTarget));
            return nativeTarget;
        }
    }
    
    //////////////////////////////////
    //
    // End of upcalls from native code
    //
    //////////////////////////////////
    
}
