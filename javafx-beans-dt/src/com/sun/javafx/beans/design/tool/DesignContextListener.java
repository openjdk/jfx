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

/**
 * DesignContextListener is the event listener interface for DesignContexts.  These methods are called
 * when a DesignBean is created, deleted, changed, or moved.  This includes the DesignBeanListener
 * methods as well, so effectively a DesignContextListener is a listener to *all* DesignBeans in a
 * context.
 *
 * <p><b>GLOBAL CONTEXT LISTENERS:</b>  If you wish to provide a global IDE-wide
 * DesignContextListener, you can declare the following static method in your DesignInfo class.
 * When the DesignInfo for your bean is loaded, this static method will be looked for via
 * reflection and called if it exists:</p>
 *
 * <code>
 *    public static DesignContextListener getGlobalDesignContextListener() { ... }
 * </code>
 *
 * <p>If this methods is declared in a DesignInfo implementation class, it will be called when the
 * DesignInfo is loaded, and added to a static list of global listeners. This listener will be
 * notified of *every* event that happens in the IDE, so please use sparingly!</p>
 *
 * <P><B>IMPLEMENTED BY THE COMPONENT AUTHOR</B> - This interface is designed to be implemented by
 * the component (bean) author.</P>
 *
 * @author Joe Nuxoll
 * @version 1.0
 */
public interface DesignContextListener extends DesignBeanListener {
    /**
     * A DesignContext has been "activated" in the project
     *
     * @param context the DesignContext that has been activated
     */
    public void contextActivated(DesignContext context);

    /**
     * A DesignContext has been "deactivated" in the project
     *
     * @param context the DesignContext that has been deactivated
     */
    public void contextDeactivated(DesignContext context);

    /**
     * Something at the context-level changed.  This is a large-grain change like a file rename or
     * something that cannot be represented by one of the smaller-grain methods.
     *
     * @param context DesignContext The DesignContext that changed
     */
    public void contextChanged(DesignContext context);

    /**
     * A new DesignBean has been created.  This corresponds to a new instance bean being dropped from
     * the palette or programmatically created via the Design-Time API.
     *
     * @param designBean DesignBean The newly created DesignBean
     */
    public void beanCreated(DesignBean designBean);

    /**
     * A DesignBean has been deleted.
     *
     * @param designBean DesignBean The DesignBean that was deleted.  At this point, it is a goner, so any
     *        manipulations done to the passed bean will be tossed out immediately after this method
     *        returns
     */
    public void beanDeleted(DesignBean designBean);

    /**
     * A DesignBean was moved either within its parent DesignBean, or to another parent DesignBean.
     *
     * @param designBean DesignBean The DesignBean that was moved
     * @param oldParent DesignBean The old parent DesignBean (may match the new parent)
     * @param pos Position The new parent DesignBean (may match the old parent)
     */
    public void beanMoved(DesignBean designBean, DesignBean oldParent, Position pos);
}
