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

import com.sun.javafx.beans.design.author.DesignInfo;
import com.sun.javafx.beans.metadata.BeanMetaData;
import com.sun.javafx.beans.metadata.EventMetaData;
import com.sun.javafx.beans.metadata.PropertyMetaData;

/**
 * <P>A DesignBean represents an instance of a JavaBean class at design-time.  There is one
 * DesignBean instance 'wrapping' each instance of a component class in a bean design tool. All
 * access to properties and events should be done via the DesignBean interface at design-time, so
 * that the tool is able to track changes and persist them.  Think of the "DesignBean" as the
 * design-time proxy for an instance of a JavaBean.</p>
 *
 * <P><B>IMPLEMENTED BY THE IDE</B> - This interface is implemented by the IDE for use by the
 * component (bean) author.</P>
 *
 * @author Joe Nuxoll
 * @version 1.0
 */
public interface DesignBean<T> {

    /**
     * Returns the BeanInfo descriptor for this bean instance's type.
     *
     * @return The BeanInfo descriptor for this bean instance's type.
     */
    public BeanMetaData getBeanInfo();

    /**
     * Returns the DesignInfo instance for this bean instance.
     *
     * @return The DesignInfo instance for this bean instance.
     */
    public DesignInfo<T> getDesignInfo();

    /**
     * Returns the instance that this DesignBean is marshalling.
     *
     * @return The instance of the wrapped bean instance.
     */
    public T getInstance();

    /**
     * Returns the instance name of this bean - as declared in source code.
     *
     * @return The source code instance name of this bean.
     */
    public String getInstanceName();

    /**
     * Returns <code>true</code> if this instance can be renamed via this interface.
     *
     * @return <code>true</code> if this instance can be renamed via this interface, or 
     *         <code>false</code> if not.
     */
    public boolean canSetInstanceName();

    /**
     * Renames the instance variable for this bean instance in the source code.  If successful,
     * this method returns <code>true</code>, if there is a problem, including the existence of a
     * duplicate instance variable name, this method returns <code>false</code>.
     *
     * @param name The desired source code instance name for this bean.
     * @return <code>true</code> if the rename was successful, or <code>false</code> if not.
     */
    public boolean setInstanceName(String name);

    /**
     * Renames the instance variable for this bean instance in the source code, and appends an
     * auto-incremented number.  For example:  setInstanceName("button", true) --> button1 -->
     * button2 --> button3, etc.  If successful, this method returns <code>true</code>, if there is 
     * a problem, this method returns <code>false</code>.
     *
     * @param name The desired source code instance name (base) for this bean.
     * @param autoNumber <code>true</code> to auto-number the instance name, <code>false</code> to 
     *        strictly attempt the specified name.
     * @return <code>true</code> if the rename was successful, or <code>false</code> if not.
     */
    public boolean setInstanceName(String name, boolean autoNumber);

    /**
     * Returns the DesignContext that 'owns' this bean instance.
     *
     * @return The DesignContext 'owner' of this bean instance.
     */
    public DesignContext getDesignContext();

    /**
     * Returns the DesignBean parent of this bean instance, or null if this is a top-level bean.
     *
     * @return The DesignBean parent of this bean instance, or null if this is a top-level bean.
     */
    public DesignBean getParentBean();

    /**
     * Returns an array of DesignProperty objects representing the properties of this DesignBean.
     *
     * @return An array of DesignProperty objects representing the properties of this DesignBean.
     */
    public DesignProperty[] getProperties();

    /**
     * Returns a single DesignProperty object representing the specified property (by name).
     *
     * @param propertyName The name of the desired DesignProperty to retrieve.
     * @return The DesignProperty representing the desired property, or null if the specified 
     *         property does not exist in this DesignBean.
     */
    public DesignProperty getProperty(String propertyName);

    /**
     * Returns a single DesignProperty object representing the specified property (by descriptor).
     *
     * @param property The PropertyDescriptor of the desired DesignProperty to retrieve.
     * @return The DesignProperty representing the desired property, or null if the specified 
     *         property does not exist in this DesignBean.
     */
    public DesignProperty getProperty(PropertyMetaData property);

    /**
     * Returns an array of DesignEvent objects representing the events of this DesignBean.
     *
     * @return An array of DesignEvent object representing the events of this DesignBean.
     */
    public DesignEvent[] getEvents();

    /**
     * Returns the DesignEvent objects for a particular event set.
     *
     * @param eventSet The EventSetDescriptor containing the desired events.
     * @return An array of DesignEvent objects representing the events contained in the specified
     *         event set.
     */
    public DesignEvent[] getEvents(EventMetaData eventSet);

    /**
     * Returns the DesignEvent from within the specified event set and having the specified
     * MethodDescriptor.
     *
     * @param eventSet The desired EventSetDescriptor
     * @param event The desired MethodDescriptor
     * @return The DesignEvent representing the event desired, or null if none matched criteria
     */
//    public DesignEvent getEvent(EventMetaData eventSet, MethodDescriptor event);

    /**
     * Returns a DesignEvent with the specified EventDescriptor.
     *
     * @param event The desired event's EventDescriptor
     * @return The DesignEvent representing the event desired, or null if none matched criteria
     */
    public DesignEvent getEvent(EventMetaData event);

    /**
     * Returns <code>true</code> if this DesignBean can be a logical container for other 
     * DesignBeans, or <code>false</code> if not.  For example, if a DesignBean is representing a 
     * HtmlCommandButton instance, it will return <code>false</code> from this method, whereas a 
     * DesignBean representing an HtmlDataTable will return <code>true</code>.  You can only add 
     * children to a DesignBean that returns <code>true</code> from this method.
     *
     * @return <code>true</code> if this DesignBean is a container, and <code>false</code> if it is 
     *         not
     */
    public boolean isContainer();

    /**
     * Returns <code>true</code> is this DesignBean is considered valid. This is the
     * normal state for a DesignBean. However, after a DesignBean has been deleted,
     * a DesignBean is no longer valid and this method will return <code>false</code>.
     *
     * @return <code>true</code> if the DesignBean is valid, and <code>false</code> if it is not
     */
    public boolean isValid();

    /**
     * Returns the count of child DesignBeans contained in this DesignBean.  Children are "logical"
     * children in that they represent the sub-components contained inside of another component in
     * the markup (JSP) or containership hierarchy.
     *
     * @return The count of DesignBean children contained by this DesignBean
     */
    public int getChildBeanCount();

    /**
     * Returns the child DesignBean at the specified cardinal index (zero-based).
     *
     * @param index The zero-based cardinal index for the desired DesignBean child
     * @return the DesignBean at the specified index
     */
    public DesignBean getChildBean(int index);

    /**
     * Returns an array of DesignBean children of this DesignBean
     *
     * @return An array of DesignBean children of this DesignBean
     */
    public DesignBean[] getChildBeans();

    /**
     * Adds a DesignBeanListener event listener to this DesignBean
     *
     * @param beanListener the event listener to add
     */
    public void addDesignBeanListener(DesignBeanListener beanListener);

    /**
     * Removes a DesignBeanListener event listener from this DesignBean
     *
     * @param beanListener the event listener to remove
     */
    public void removeDesignBeanListener(DesignBeanListener beanListener);

    /**
     * Returns an array of DesignBeanListener currently listening to this DesignBean
     * @return An array of DesignBeanListener currently listening to this DesignBean
     */
    public DesignBeanListener[] getDesignBeanListeners();
}
