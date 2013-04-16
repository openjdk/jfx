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

package com.sun.javafx.scene.control;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.util.Callback;

public final class MultiplePropertyChangeListenerHandler {
    
    private final Callback<String, Void> propertyChangedHandler;
    
    private Map<Object,String> propertyReferenceMap = new WeakHashMap<Object,String>();
    
    public MultiplePropertyChangeListenerHandler(Callback<String, Void> propertyChangedHandler) {
        this.propertyChangedHandler = propertyChangedHandler;
    }
    
    public void dispose() {
        if (propertyReferenceMap != null) {
            // unhook listeners
            for (Object obj : propertyReferenceMap.keySet()) {
                if (obj instanceof ObservableValue) {
                    ((ObservableValue<?>)obj).removeListener(weakPropertyChangedListener);
                } else if (obj instanceof ObservableList) {
                    ((ObservableList<?>)obj).removeListener(weakListChangedListener);
                }
            }
            propertyReferenceMap.clear();
            propertyReferenceMap = null;
            propertyChangedListener = null;
            weakPropertyChangedListener = null;
        }
    }
    
    
    
    /***************************************************************************
     * 
     * Property listener 
     *
     **************************************************************************/
    
    private ChangeListener<Object> propertyChangedListener;
    private WeakChangeListener<Object> weakPropertyChangedListener;
    
    /**
     * Subclasses can invoke this method to register that we want to listen to
     * property change events for the given property.
     *
     * @param property
     * @param reference
     */
    public final void registerChangeListener(ObservableValue<?> property, String reference) {
        if (weakPropertyChangedListener == null) {
            propertyChangedListener = new ChangeListener<Object>() {
                @Override public void changed(ObservableValue<?> property, 
                        @SuppressWarnings("unused") Object oldValue, 
                        @SuppressWarnings("unused") Object newValue) {
                    propertyChangedHandler.call(propertyReferenceMap.get(property));
                }
            };
            weakPropertyChangedListener = new WeakChangeListener<Object>(propertyChangedListener);
        }
        
        if (!propertyReferenceMap.containsKey(property)) {
            propertyReferenceMap.put(property, reference);
            property.addListener(weakPropertyChangedListener);
        }
    }
    
    public final void unregisterChangeListener(ObservableValue<?> property) {
        if (propertyReferenceMap == null) {
            return;
        }
        
        if (propertyReferenceMap.containsKey(property)) {
            propertyReferenceMap.remove(property);
            property.removeListener(weakPropertyChangedListener);
        }
    }
    
    
    
    /***************************************************************************
     * 
     * ObservableList listener 
     *
     **************************************************************************/
    
    private ListChangeListener<Object> listChangedListener;
    private WeakListChangeListener<Object> weakListChangedListener;
    
    public final void registerChangeListener(ObservableList list, String reference) {
        if (weakListChangedListener == null) {
            listChangedListener = new ListChangeListener<Object>() {
                @Override public void onChanged(javafx.collections.ListChangeListener.Change<? extends Object> c) {
                    propertyChangedHandler.call(propertyReferenceMap.get(c.getList()));
                }
            };
            weakListChangedListener = new WeakListChangeListener<Object>(listChangedListener);
        }
        
        if (!propertyReferenceMap.containsKey(list)) {
            propertyReferenceMap.put(list, reference);
            list.addListener(weakListChangedListener);
        }
    }
    
    public final void unregisterChangeListener(ObservableList list) {
        if (propertyReferenceMap == null) {
            return;
        }
        
        if (propertyReferenceMap.containsKey(list)) {
            propertyReferenceMap.remove(list);
            list.removeListener(weakListChangedListener);
        }
    }
}
