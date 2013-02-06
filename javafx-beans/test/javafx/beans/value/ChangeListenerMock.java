/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package javafx.beans.value;

import static org.junit.Assert.assertEquals;

public class ChangeListenerMock<T> implements ChangeListener<T> {

    private final static double EPSILON_DOUBLE = 1e-12;
    private final static float EPSILON_FLOAT = 1e-6f;

	private final T undefined;
	
    private ObservableValue<? extends T> valueModel = null;
    private T oldValue;
    private T newValue;
    private int counter = 0;
    
    public ChangeListenerMock(T undefined) {
    	this.undefined = undefined;
    	this.oldValue = undefined;
    	this.newValue = undefined;
    }

    @Override public void changed(ObservableValue<? extends T> valueModel, T oldValue, T newValue) {
    	this.valueModel = valueModel;
    	this.oldValue = oldValue;
    	this.newValue = newValue;
        counter++;
    }
    
    public void reset() {
        valueModel = null;
        oldValue = undefined;
        newValue = undefined;
        counter = 0;
    }

    public void check(ObservableValue<? extends T> valueModel, T oldValue, T newValue, int counter) {
    	assertEquals(valueModel, this.valueModel);
        if ((oldValue instanceof Double) && (this.oldValue instanceof Double)) {
            assertEquals((Double)oldValue, (Double)this.oldValue, EPSILON_DOUBLE);
        } else if ((oldValue instanceof Float) && (this.oldValue instanceof Float)) {
            assertEquals((Float)oldValue, (Float)this.oldValue, EPSILON_FLOAT);
        } else {
        	assertEquals(oldValue, this.oldValue);
        }
        if ((newValue instanceof Double) && (this.newValue instanceof Double)) {
            assertEquals((Double)newValue, (Double)this.newValue, EPSILON_DOUBLE);
        } else if ((newValue instanceof Float) && (this.newValue instanceof Float)) {
            assertEquals((Float)newValue, (Float)this.newValue, EPSILON_FLOAT);
        } else {
        	assertEquals(newValue, this.newValue);
        }
        assertEquals(counter, this.counter);
        reset();
    }
}
