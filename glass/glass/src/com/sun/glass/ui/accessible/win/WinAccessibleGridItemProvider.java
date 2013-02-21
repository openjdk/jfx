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

import com.sun.glass.ui.accessible.AccessibleLogger;
import com.sun.javafx.accessible.providers.AccessibleProvider;
import com.sun.javafx.accessible.providers.GridItemProvider;
import com.sun.javafx.accessible.utils.PatternIds;

public class WinAccessibleGridItemProvider extends WinAccessibleBasePatternProvider {
    native private static void _initIDs();
    native private long _createAccessible(long nativeSimple);
    native private void _destroyAccessible(long nativeAccessible);

    /**
     * A class static block that initializes the JNI method IDs.
     */
    static {
        _initIDs();
    }
 
    /**
     * Downcall to create the native accessible.  This will be used when firing
     * events or when destroying the native accessible.
     * 
     * @param node      the related FX node object.
     * @param nativeSimple  the native accessible.
     */
    public WinAccessibleGridItemProvider(Object node, long nativeSimple) {
        super(node, nativeSimple);
        nativeAccessible = _createAccessible(nativeSimple);
    }
    
    @Override
    final public void destroyAccessible() {
        _destroyAccessible(nativeAccessible);
    }
    
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
     * For WinAccessibleGridItemProvider - getColumnCount
     *
     * @return the ColumnCount
     */
    private int getRow() {
        AccessibleLogger.getLogger().fine("In WinAccessibleGridItemProvider.getRow");
        return ((GridItemProvider)node).getRow();
    }

    private int getRowSpan() {
        AccessibleLogger.getLogger().fine("In WinAccessibleGridItemProvider.getRowSpan");
        return ((GridItemProvider)node).getRowSpan();
    }
    
    private int getColumn() {
        AccessibleLogger.getLogger().fine("In WinAccessibleGridItemProvider.getColumn");
        return ((GridItemProvider)node).getColumn();
    }

    private int getColumnSpan() {
        AccessibleLogger.getLogger().fine("In WinAccessibleGridItemProvider.getColumnSpan");
        return ((GridItemProvider)node).getColumnSpan();
    }
    
    private long getContainingGrid() {
        AccessibleLogger.getLogger().fine("In WinAccessibleGridItemProvider.getContainingGrid");
        AccessibleProvider container = ((GridItemProvider)node).getContainingGrid() ;
        if (container == null) {
            return 0;
        } else {
            long nativeContainer = ((WinAccessibleBasePatternProvider)container).getNativeAccessible();
            AccessibleLogger.getLogger().fine("  nativeContainer:  " + Long.toHexString(nativeContainer));
            return nativeContainer;
        }
    }
    
    // Return the pattern supported by this class
    @Override
    final public int getPatternId() {
        return PatternIds.GRID_ITEM ;
    }
    
    //////////////////////////////////
    //
    // End of upcalls from native code
    //
    //////////////////////////////////
    
}
