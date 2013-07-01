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

package com.sun.javafx.scene.control.accessible;

import com.sun.javafx.accessible.providers.ToggleProvider;
import com.sun.javafx.accessible.providers.ExpandCollapseProvider;
import com.sun.javafx.accessible.providers.InvokeProvider;
import com.sun.javafx.accessible.utils.ExpandCollapseState;
import com.sun.javafx.accessible.utils.ToggleState;
import javafx.scene.control.MenuButton;


public class AccessibleMenuButton extends AccessibleButton implements ToggleProvider, InvokeProvider, 
        ExpandCollapseProvider {
    
    MenuButton menuButton;
    public AccessibleMenuButton(MenuButton b) {
        super(b);
        this.menuButton = b;
    }
    
    @Override public void toggle() {
        if ( menuButton.isShowing()) 
            menuButton.hide();
        else menuButton.show();
    }

    @Override public ToggleState getToggleState() {
        if(menuButton.isShowing())
            return ToggleState.ON;
        else return ToggleState.OFF;
    }

    @Override public void invoke() {
        menuButton.show();
        // need to fire action event as well or may be only action event which 
        // will automatically call menuButton.show. 
    }

    @Override public ExpandCollapseState getExpandCollapseState() {
        if (menuButton.isShowing()) {
            return ExpandCollapseState.ExpandCollapseState_Expanded;
        } else {
            return ExpandCollapseState.ExpandCollapseState_Collapsed;
        }
    }

    @Override public void expand() {
        menuButton.show();
    }

    @Override public void collapse() {
        menuButton.hide();
    }
    
    @Override public Object getPatternProvider(int patternId) {
        return (Object)super.getAccessibleElement() ;
    }
}

