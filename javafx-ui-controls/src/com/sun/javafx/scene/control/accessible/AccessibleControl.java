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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Control;
import sun.util.logging.PlatformLogger;

import com.sun.javafx.Logging;
import com.sun.javafx.accessible.AccessibleNode;
import com.sun.javafx.accessible.utils.EventIds;

public class AccessibleControl extends AccessibleNode {
    Control control ;
    public AccessibleControl(Control control)
    {
	super(control);
        this.control = control ;
        
        control.focusedProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                PlatformLogger logger = Logging.getAccessibilityLogger();
                if(!t && t1)
                {
                    if (logger.isLoggable(PlatformLogger.FINER)) {
                        logger.finer(this.toString() + " Focus Change: true");
                    }
                    fireEvent(EventIds.AUTOMATION_FOCUS_CHANGED);
                }
                else
                    if (logger.isLoggable(PlatformLogger.FINER)) {
                        logger.finer(this.toString() + " Focus Change: false");
                    }
            }
        } );        
    }
        
    public void fireEvent(int id)
    {
        super.fireEvent(id);
    }

    public void firePropertyChange(int propertyId, int oldProperty, int newProperty) {
        super.firePropertyChange(propertyId, oldProperty, newProperty);
    }
    public void firePropertyChange(int propertyId, boolean oldProperty, boolean newProperty) {
        super.firePropertyChange(propertyId, oldProperty, newProperty);
    }
    
}
