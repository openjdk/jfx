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
 * <P>A BeanCreateInfoSet is a group version of the BeanCreateInfo interface.  It describes a
 * single item on a Palette that will create a set of beans in a visual designer.  This includes a
 * display name, description, icon, etc.  There is also (most importantly) a hook to
 * programmatically manipulate the newly created beans immediately after they have been created.
 * This is useful for setting the default state for the newly created set of beans.</P>
 *
 * <P>If the any of the specified JavaBeans have an associated DesignInfo, the DesignInfo's
 * 'beanCreatedSetup' method will be called before the BeanCreateInfoSet's 'beansCreatedSetup' method
 * will be called.  This gives the DesignInfo the "first crack", but it ultimately gives the
 * BeanCreateInfoSet the "last word".</P>
 *
 * <P><B>IMPLEMENTED BY THE COMPONENT AUTHOR</B> - This interface is designed to be implemented by
 * the component (bean) author.  The BasicBeanCreateInfoSet class can be used for convenience.</P>
 *
 * @author Joe Nuxoll
 * @version 1.0
 */
public interface BeanCreateInfoSet extends DisplayItem {

    /**
     * Returns an array of class names of the new JavaBeans to create when this BeanCreateInfoSet
     * is invoked in a visual designer.
     *
     * @return A String[] of fully qualified class names for the JavaBeans to create.
     */
    public String[] getBeanClassNames();

    /**
     * <p>A hook that gets called after the full set of JavaBean gets created.  This is useful for
     * a component author to setup an initial state for a set of JavaBeans when they are first
     * created.  Note that this method is only called one time after the JavaBeans are initially
     * created from the palette.  This is *not* a hook that is called each time the project is
     * reopened.</p>
     *
     * <P>If the any of the specified JavaBeans have an associated DesignInfo, the DesignInfo's
     * 'beanCreatedSetup' method will be called before each of the BeanCreateInfo's 'beanCreatedSetup'
     * methods are called.  Once all of the beans have been created, and the individual 
     * 'beanCreatedSetup' methods have been called, this 'beansCreatedSetup' method will be called.
     * This gives the DesignInfo the "first crack", but it ultimately gives the BeanCreateInfoSet the
     * "last word".</P>
     *
     * @param designBeans The array of DesignBean objects representing the JavaBeans that have just been
     *        created.
     * @return A standard Result object, indicating success or failure - and optionally including
     *         messages for the user.
     */
    public Result beansCreatedSetup(DesignBean[] designBeans);
}
