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

/**
 *
 */
public interface AccessibleStageProvider {
    // Summary:
    //     Retrieves the element in this fragment that is at the specified point.
    //
    // Parameters:
    //   x:
    //     The X coordinate,.
    //
    //   y:
    //     The Y coordinate.
    //
    // Returns:
    //     The provider for the child element at the specified point, if one exists,
    //     or the root provider if the point is on this element but not on any child
    //     element. Otherwise returns null.
    public Object elementProviderFromPoint(double x, double y);
    //
    // Summary:
    //     Retrieves the element in this fragment that has the input focus.
    //
    // Returns:
    //     The provider for the element in this fragment that has the input focus, if
    //     any; otherwise, a null reference (Nothing in Visual Basic).
    public Object getFocus();
}
