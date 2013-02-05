/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.binding;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Variable {
    public int numChangedListenersForNext = 0;
    public int numChangedListenersForName = 0;

    public Variable(String name) {
        this.name.set(name);
    }

    private final ObjectProperty<Object> next = new SimpleObjectProperty<Object>() {
        @Override
        public void addListener(InvalidationListener listener) {
            super.addListener(listener);
            numChangedListenersForNext++;
        }
        @Override
        public void removeListener(InvalidationListener listener) {
            super.removeListener(listener);
            numChangedListenersForNext = Math.max(0, numChangedListenersForNext-1);
        }
    };
    public Object getNext() {return next.get();}
    public void setNext(Object value) {next.set(value);}
    public ObjectProperty<Object> nextProperty() {return next;}

    
    
    private final StringProperty name = new SimpleStringProperty() {
        @Override
        public void addListener(InvalidationListener listener) {
            super.addListener(listener);
            numChangedListenersForName++;
        }
        @Override
        public void removeListener(InvalidationListener listener) {
            super.removeListener(listener);
            numChangedListenersForName = Math.max(0, numChangedListenersForName-1);
        }
    };
    public final String getName() {return name.get();}
    public void setName(String value) {name.set(value);}
    public StringProperty nameProperty() {return name;}
    
    @Override public String toString() {return name.get();}
}
