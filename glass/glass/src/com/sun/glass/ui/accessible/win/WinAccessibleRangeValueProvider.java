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

import com.sun.javafx.accessible.providers.RangeValueProvider;
import com.sun.javafx.accessible.utils.PatternIds;

public class WinAccessibleRangeValueProvider extends WinAccessibleBasePatternProvider {
    double storeValue = -1;
    double smallChange = -1;
    
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
     * @param node          the related FX node object.
     * @param nativeSimple  the native accessible.
     */
    public WinAccessibleRangeValueProvider(Object node, long nativeSimple) {
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
    
    private double getValue() {
        if (storeValue == -1) {
            storeValue = ((RangeValueProvider)node).getValue();
        }
        double currentValue = ((RangeValueProvider)node).getValue();
        smallChange = Math.abs(storeValue - currentValue);
        return ((RangeValueProvider)node).getValue();
    }
    
    private void setValue() {
        // do nothing: not sure we want to setValue on the slider?
    }
    
    private double getMinimum() {
        return ((RangeValueProvider)node).getMinimum();
    }
    
    private double getMaximum() {
        return ((RangeValueProvider)node).getMaximum();
    }
    
    private boolean getIsReadOnly() {
        return ((RangeValueProvider)node).isReadOnly();
    }
    
    private double getSmallChange() {
        return smallChange;
    }

    // Return the pattern supported by this class
    @Override
    final public int getPatternId() {
        return PatternIds.RANGE_VALUE;
    }
    
    //////////////////////////////////
    //
    // End of upcalls from native code
    //
    //////////////////////////////////

}
