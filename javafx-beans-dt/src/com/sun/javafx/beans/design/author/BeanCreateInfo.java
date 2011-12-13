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
package com.sun.javafx.beans.design.author;

import com.sun.javafx.beans.design.DisplayItem;
import com.sun.javafx.beans.design.tool.DesignBean;

/**
 * <P>A BeanCreateInfo describes an item on a Palette that will create a bean in a visual designer.
 * This includes a display name, description, icon, etc.  There is also (most importantly) a hook
 * to programmatically manipulate the newly created bean immediately after is has been created.
 * This is useful for setting a default state for the newly created bean.</P>
 *
 * <P>If the specified JavaBean has an associated DesignInfo, the DesignInfo's 'beanCreatedSetup'
 * method will be called before the BeanCreateInfo's 'beanCreatedSetup' method is called.  This 
 * gives the DesignInfo the "first crack", but it gives the BeanCreateInfo the "last word".</P>
 *
 * <P><B>IMPLEMENTED BY THE COMPONENT AUTHOR</B> - This interface is designed to be implemented by
 * the component (bean) author.  The BasicBeanCreateInfo class can be used for convenience.</P>
 *
 * @author Joe Nuxoll
 * @version 1.0
 * @see javax.beans.impl.BasicBeanCreateInfo
 */
public interface BeanCreateInfo<T> extends DisplayItem {

    /**
     * Returns the class name of the new JavaBean to create when this BeanCreateInfo is invoked in
     * a visual designer.
     *
     * @return The String fully qualified class name for the JavaBean to create.
     */
    public String getBeanClassName();

    /**
     * <p>A hook that gets called after this JavaBean gets created initially.  This is useful for a
     * component author to setup an initial state for their JavaBean when it is first created.  Note
     * that this method is only called one time after the JavaBeans are initially created from the
     * palette.  This is *not* a hook that is called each time the project is reopened.</p>

     * <p>NOTE: If the specified bean has an associated DesignInfo class - it will have "first
     * crack" at modifying the initial state of the bean.  This method will be called after the
     * DesignInfo one is called.</p>
     *
     * @return A standard Result object, indicating success or failure - and optionally including
     *         messages for the user.
     */
    public Result beanCreatedSetup(DesignBean<T> designBean);
}
