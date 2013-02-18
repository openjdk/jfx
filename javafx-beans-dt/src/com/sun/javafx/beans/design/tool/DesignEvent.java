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

import com.sun.javafx.beans.metadata.EventMetaData;

/**
 * <p>A DesignEvent represents a single event listener method (and possibly handler) on a single
 * instance of a DesignBean at design-time.</p>
 *
 * <P><B>IMPLEMENTED BY THE IDE</B> - This interface is implemented by the IDE for use by the
 * component (bean) author.</P>
 *
 * @author Joe Nuxoll
 * @version 1.0
 */
public interface DesignEvent {

    /**
     * Returns the EventDescriptor that defines the meta-data for this DesignEvent
     *
     * @return The EventDescriptor that defines teh meta-data for this DesignEvent
     */
    public EventMetaData getEventDescriptor();

    /**
     * Returns the DesignBean that owns this DesignEvent
     *
     * @return the DesignBean that owns this DesignEvent
     */
    public DesignBean getDesignBean();

    /**
     * Returns the default event handler method name.  For example on a Button component's 'click'
     * event, the default handler name might be "button1_click".
     *
     * @return the default event handler method name, same as setHandlerName would use if passed null
     */
    public String getDefaultHandlerName();

    /**
     * Sets the method name for this DesignEvent.  If the event is not currently 'hooked', this will
     * 'hook' it and add the required wiring to direct the event handler code to this method name.
     * If 'null' is passed as the handlerName, then the default event handler method name will be
     * used.
     *
     * @param handlerMethodName The desired event handler method name - may be null to use default
     *        event handler method name
     * @return <code>true</code> if the event was successfully 'hooked', and the specified name was unique
     */
    public boolean setHandlerName(String handlerMethodName);

    /**
     * Returns the current event method handler name, or null if the event is currently not 'hooked'
     *
     * @return the current event method handler name, or null if the event is currently not 'hooked'
     */
    public String getHandlerName();

    /**
     * Returns <code>true</code> if this DesignEvent is currently 'hooked', or <code>false</code> 
     * if it is not.
     *
     * @return <code>true</code> if this DesignEvent is currently 'hooked', or <code>false</code> 
     *         if it is not
     */
    public boolean isHandled();

    /**
     * Removes and unwires an event handler method from this DesignEvent, if one exists.  Returns
     * <code>true</code> if successful, <code>false</code> if not.
     *
     * @return <code>true</code> if successful, <code>false</code> if not
     */
    public boolean removeHandler();

    /**
     * Sets the Java source for the method body of the handler method.  This is expected to be valid
     * Java source to be injected into the body of this event handler method.  If it is not, an
     * IllegalArgumentException is thrown.
     *
     * @param methodBody The Java source for the method body of this event handler method
     * @throws IllegalArgumentException thrown if the Java source is invalid
     */
    public void setHandlerMethodSource(String methodBody) throws IllegalArgumentException;

    /**
     * Returns the Java source code from the body of the handler method
     *
     * @return the Java source code from the body of the handler method
     */
    public String getHandlerMethodSource();
}
