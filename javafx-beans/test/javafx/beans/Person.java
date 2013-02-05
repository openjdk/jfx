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

package javafx.beans;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import com.sun.javafx.property.PropertyReference;

/**
 *
 */
public class Person {
    private final StringProperty name = new SimpleStringProperty();
    public final String getName() {return name.get();}
    public void setName(String value) {name.set(value);}
    public StringProperty nameProperty() {return name;}

    private final IntegerProperty age = new SimpleIntegerProperty();
    public final int getAge() {return age.get();}
    public void setAge(int value) {age.set(value);}
    public IntegerProperty ageProperty() {return age;}

    private final BooleanProperty retired = new SimpleBooleanProperty();
    public final boolean getRetired() {return retired.get();}
    public void setRetired(boolean value) {retired.set(value);}
    public BooleanProperty retiredProperty() {return retired;}

    private final IntegerProperty weight = new SimpleIntegerProperty(); // in cm?? :-)
    public final int getWeight() {return weight.get();}
    public void setWeight(int value) {weight.set(value);}
    public IntegerProperty weightProperty() {return weight;}

    private final LongProperty income = new SimpleLongProperty(); // wow, can have a HUGE income!
    public final long getIncome() {return income.get();}
    public void setIncome(long value) {income.set(value);}
    public LongProperty incomeProperty() {return income;}

    private final FloatProperty miles = new SimpleFloatProperty();
    public final float getMiles() {return miles.get();}
    public void setMiles(float value) {miles.set(value);}
    public FloatProperty milesProperty() {return miles;}

    private final DoubleProperty something = new SimpleDoubleProperty(); // I have no idea...
    public final double getSomething() {return something.get();}
    public void setSomething(double value) {something.set(value);}
    public DoubleProperty somethingProperty() {return something;}
    
    private final ObjectProperty<Object> data = new SimpleObjectProperty<Object>();
    public final Object getData() {return data.get();}
    public void setData(Object value) {data.set(value);}
    public ObjectProperty<Object> dataProperty() {return data;}
    
    public final ReadOnlyIntegerWrapper noWrite = new ReadOnlyIntegerWrapper();
    public static final PropertyReference<Integer> NO_WRITE = new PropertyReference<Integer>(Person.class, "noWrite");
    public final int getNoWrite() { return noWrite.get(); }
    public ReadOnlyIntegerProperty noWriteProperty() {return noWrite.getReadOnlyProperty();}
    
    public final IntegerProperty noRead = new SimpleIntegerProperty(); // do not expect it back
    public static final PropertyReference<Integer> NO_READ = new PropertyReference<Integer>(Person.class, "noRead");
    public void setNoRead(int value) {noRead.set(value);}
    
    int noReadWrite;
    public static final PropertyReference<Integer> NO_READ_WRITE = new PropertyReference<Integer>(Person.class, "noReadWrite");

}
