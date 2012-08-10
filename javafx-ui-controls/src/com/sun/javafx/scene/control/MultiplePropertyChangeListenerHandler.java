/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control;

import java.util.HashMap;
import java.util.Map;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.util.Callback;

/**
 * 
 */
public final class MultiplePropertyChangeListenerHandler {
    
    private final Callback<String, Void> propertyChangedHandler;
    
    public MultiplePropertyChangeListenerHandler(Callback<String, Void> propertyChangedHandler) {
        this.propertyChangedHandler = propertyChangedHandler;
    }
    
    /**
     * This is part of the workaround introduced during delomboking. We probably will
     * want to adjust the way listeners are added rather than continuing to use this
     * map (although it doesn't really do much harm).
     */
    private Map<ObservableValue,String> propertyReferenceMap =
            new HashMap<ObservableValue,String>();
    
    private final ChangeListener propertyChangedListener = new ChangeListener() {
        @Override public void changed(ObservableValue property, Object oldValue, Object newValue) {
            propertyChangedHandler.call(propertyReferenceMap.get(property));
        }
    };
    
    private final WeakChangeListener weakPropertyChangedListener = 
            new WeakChangeListener(propertyChangedListener);
    
    /**
     * Subclasses can invoke this method to register that we want to listen to
     * property change events for the given property.
     *
     * @param property
     * @param reference
     */
    public final void registerChangeListener(ObservableValue property, String reference) {
        if (!propertyReferenceMap.containsKey(property)) {
            propertyReferenceMap.put(property, reference);
            property.addListener(weakPropertyChangedListener);
        }
    }
    
    public final void unregisterChangeListener(ObservableValue property, String reference) {
        if (propertyReferenceMap.containsKey(property)) {
            propertyReferenceMap.remove(property);
            property.removeListener(weakPropertyChangedListener);
        }
    }

    public void dispose() {
        // unhook listeners
        for (ObservableValue value : propertyReferenceMap.keySet()) {
            value.removeListener(weakPropertyChangedListener);
        }
        propertyReferenceMap.clear();
    }
}
