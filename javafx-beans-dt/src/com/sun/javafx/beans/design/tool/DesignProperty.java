/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.beans.design.tool;

import com.sun.javafx.beans.metadata.PropertyMetaData;

/**
 * <p>A DesignProperty represents a single property (setter/getter method pair) on a single instance
 * of a DesignBean at design-time.  All manipulation of properties at design-time should be done via
 * this interface.  This allows the IDE to both persist the changes as well as reflect them in the
 * design-time session.</p>
 *
 * <P><B>IMPLEMENTED BY THE IDE</B> - This interface is implemented by the IDE for use by the
 * component (bean) author.</P>
 *
 * @author Joe Nuxoll
 * @version 1.0
 * @see DesignBean#getProperties()
 * @see DesignBean#getProperty(String)
 */
public interface DesignProperty {

    /**
     * Returns the PropertyDescriptor associated with this DesignProperty
     *
     * @return the PropertyDescriptor associated with this DesignProperty
     */
    public PropertyMetaData getPropertyDescriptor();

    /**
     * Returns the DesignBean that this DesignProperty is associated with
     *
     * @return the DesignBean that this DesignProperty is associated with
     */
    public DesignBean getDesignBean();

    /**
     * Returns the current value of this DesignProperty.  The returned value is the *actual* value
     * that the design-time instance of the DesignBean has set for this property.
     *
     * @return the current value of this DesignProperty
     */
    public Object getValue();

    /**
     * Sets the current value of this DesignProperty.  This will set the *actual* value of this
     * property on the design-time instance of this DesignBean.  The associated PropertyEditor will
     * be used to produce the appropriate Java or markup code to set the property.  Calling this
     * method results in the persistence being written, and will cause the backing file buffer to
     * become "dirty".
     *
     * @param value The Object value to set as the current value of this property
     * @return <code>true</code> if the property setting was successful, <code>false</code> if it 
     *         was not
     * @see java.beans.PropertyEditor
     */
    public boolean setValue(Object value);

    /**
     * Returns the source-persistence String value of this property.  This is the value that
     * the associated PropertyEditor would use to persist the property's current value in source
     * code.
     *
     * @return the source-persistence String value of this property
     * @see java.beans.PropertyEditor#getJavaInitializationString()
     */
    public String getValueSource();

    /**
     * Sets the source-persistence String value for this property.  This is the value that will
     * actually appear in Java source code (or markup), depending on how the property setting is
     * persisted.
     *
     * @param source the source-persistence String value for this property
     * @return <code>true</code> if the property source setting was successful, <code>false</code> 
     *         if not
     */
    public boolean setValueSource(String source);

    /**
     * Returns the value that this property would have if it were unset.  This is the property's
     * original (default) state, which is determined by reading the property value on a fresh 
     * instance of the associated class (that owns this property).
     * 
     * @return The unset (default) value for this property
     */
    public Object getUnsetValue();

    /**
     * Removes the property setting (if it exists) from the source code, and reverts the property
     * setting back to its original (default) state.  The original state is determined by reading
     * the property value on a fresh instance of the associated class (that owns this property),
     * and reading the default value of the property.
     *
     * @return <code>true</code> if the unset operation was successful, <code>false</code> if not
     */
    public boolean unset();

    /**
     * Returns <code>true</code> if this DesignProperty has been modified from the 'default' value.
     * A 'modified' property is one that differs in value (== and .equals()) from a newly 
     * constructed instance of the DesignBean.
     *
     * @return <code>true</code> if this DesignProperty has been modified from the 'default' value, 
     *         <code>false</code> if not
     */
    public boolean isModified();
    
    /**
     * Returns <code>true</code> if this DesignProperty has been assigned a value. 
     * This method differs from {@link #isModified} in that a value may be set to the
     * default value, in which case {@link #isModified} would return false, and this method would return
     * true. However, a property which has not been defined will also not be modified, so !isDefined
     * implies !isModified.
     *
     * @return <code>true</code> is this DesignProperty has been assigned any value,
     *         <code>false</code> if not.
     */
    public boolean isDefined();

    /**
     * Returns the parent property that this property is a child of.  This may be null if this is
     * a top-level property.
     * 
     * @return Parent DesignProperty for this DesignProperty
     */
    public DesignProperty getParentProperty();

    /**
     * Returns an array of DesignProperty objects representing the sub-properties of this property
     * based on the static type of this property.
     *
     * @return An array of DesignProperty objects representing the sub-properties of this property
     */
    public DesignProperty[] getProperties();

    /**
     * Returns a single DesignProperty object representing the specified sub-property (by name).
     *
     * @param propertyName The name of the desired sub-property to retrieve.
     * @return The DesignProperty representing the desired property, or null if the specified 
     *         property does not exist as a child of this property.
     */
    public DesignProperty getProperty(String propertyName);

    /**
     * Returns a single DesignProperty object representing the specified sub-property (by descriptor).
     *
     * @param property The PropertyDescriptor of the desired sub-property to retrieve.
     * @return The DesignProperty representing the desired property, or null if the specified 
     *         property does not exist as a child of this property.
     */
    public DesignProperty getProperty(PropertyMetaData property);
}
