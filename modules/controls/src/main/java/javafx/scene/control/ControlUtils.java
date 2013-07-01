/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ObservableMap;
import javafx.event.Event;

class ControlUtils {
    private static final String SCROLL_TO_INDEX_KEY = "util.scroll.index";
    private static final String SCROLL_TO_COLUMN_KEY = "util.scroll.column";

    private ControlUtils() { }
    
    public static void scrollToIndex(final Control control, int index) {
        if(control.getSkin() == null) {
            installScrollToIndexCallback(control, control.skinProperty(), index);
        } else {
            fireScrollToIndexEvent(control, index);  
        }
    }
    
    private static void installScrollToIndexCallback(final Control control, final Observable property, final int index) {
        final ObservableMap<Object, Object> properties = control.getProperties();
            
        if(! properties.containsKey(SCROLL_TO_INDEX_KEY)) {
            property.addListener(new InvalidationListener() {
                @Override public void invalidated(Observable observable) {
                    Integer idx = (Integer) properties.remove(SCROLL_TO_INDEX_KEY);
                    if(idx != null) {
                        fireScrollToIndexEvent(control, idx);  
                    }
                    property.removeListener(this);
                }
            });
        }
        properties.put(SCROLL_TO_INDEX_KEY, index);
    }
    
    private static void fireScrollToIndexEvent(final Control control, final int index) {
        Event.fireEvent(control, new ScrollToEvent<Integer>(control, control, ScrollToEvent.scrollToTopIndex(), index));
    }
    
    
    
    public static void scrollToColumn(final Control control, final TableColumnBase<?, ?> column) {
        if(control.getSkin() == null) {
            installScrollToColumnCallback(control, control.skinProperty(), column);
        } else {
            fireScrollToColumnEvent(control, column);
        }
    }
    
    private static void installScrollToColumnCallback(final Control control, final Observable property, final TableColumnBase<?, ?> column) {
        final ObservableMap<Object, Object> properties = control.getProperties();
            
        if(! properties.containsKey(SCROLL_TO_COLUMN_KEY)) {
            property.addListener(new InvalidationListener() {
                @Override public void invalidated(Observable observable) {
                    TableColumnBase<?, ?> col = (TableColumnBase<?, ?>) control.getProperties().remove(SCROLL_TO_COLUMN_KEY);
                    if( col != null ) {
                        fireScrollToColumnEvent(control, col);
                    }
                    property.removeListener(this);
                }
            });
        }
        properties.put(SCROLL_TO_COLUMN_KEY, column);
    }
    
    private static void fireScrollToColumnEvent(final Control control, final TableColumnBase<?, ?> column) {
        control.fireEvent(new ScrollToEvent<TableColumnBase<?, ?>>(control, control, ScrollToEvent.scrollToColumn(), column));
    }
}
