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

import java.util.EventListener;

/**
 * DesignBeanListener is the event listener interface for DesignBeans.  These methods are called
 * when a DesignBean is changed, a DesignProperty is changed (on a DesignBean), or a DesignEvent is
 * changed (on a DesignBean).
 *
 * <P><B>IMPLEMENTED BY THE COMPONENT AUTHOR</B> - This interface is designed to be implemented by
 * the component (bean) author.</P>
 *
 * @author Joe Nuxoll
 * @version 1.0
 * @see DesignBean#addDesignBeanListener(DesignBeanListener)
 */
public interface DesignBeanListener<T> extends EventListener {
    /**
     * The specified DesignBean's DesignContext has been "activated" in the project
     *
     * @param designBean the DesignBean who's DesignContext that has been activated
     */
    public void beanContextActivated(DesignBean<T> designBean);

    /**
     * The specified DesignBean's DesignContext has been "deactivated" in the project
     *
     * @param designBean the DesignBean who's DesignContext that has been deactivated
     */
    public void beanContextDeactivated(DesignBean<T> designBean);

    /**
     * The specified DesignBean's instance name was changed.  This is the source-code instance name
     * of the bean component.
     *
     * @param designBean The DesignBean that has a new instance name
     * @param oldInstanceName The old instance name
     */
    public void instanceNameChanged(DesignBean<T> designBean, String oldInstanceName);

    /**
     * The specified DesignBean has changed.  This represents a larger-scale change than a single
     * property. For example, this event will be called when a bean has a child added or
     * removed from it, or if the instance name has changed. This method will be called whenever
     * a "more than just a property" aspect of the DesignBean has changed.
     *
     * @param designBean The DesignBean that has changed
     */
    public void beanChanged(DesignBean<T> designBean);

    /**
     * The specified DesignProperty has changed.  This could mean that a new value was set, or the
     * property was 'unset', or anything that results in the DesignProperty being different.  The
     * oldValue will be passed in if applicable and possible.
     *
     * @param prop The DesignProperty that has changed
     * @param oldValue The prior value of the property (may be null)
     */
    public void propertyChanged(DesignProperty prop, Object oldValue);

    /**
     * The specified DesignEvent has changed.  This could mean that the event was hooked, unhooked,
     * or the handler method name was changed, or anything that results in the DesignEvent being
     * different.
     *
     * @param event The DesignEvent that has changed
     */
    public void eventChanged(DesignEvent event);
}
