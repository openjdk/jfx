/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.beans.metadata;

import java.lang.reflect.Method;

/**
 * An EventMetaData is a specialization of a PropertyMetaData. All events in
 * JavaFX are simply a special type of property. They all are prefixed with
 * "on" in their name, and all have a data type of EventHandler. They also
 * usually have some specialized {@link javafx.event.Event} they must be used
 * with. This EventMetaData will return what this type of Event is. Since this
 * information is not available via reflection due to type erasure, the
 * bean author must specify this information by using the Event annotation
 * on each getter for events.
 *
 * @author Richard
 */
public class EventMetaData extends PropertyMetaData {
    /**
     * The type of the javafx.event.Event used with the EventHandler for this
     * Event. This must be specified via the com.sun.javafx.beans.metadata.Event
     * annotation on the getter for the event.
     */
    private Class<? extends javafx.event.Event> type;

    /**
     * Creates a new EventMetaData based on the given beanClass and getter.
     * Both the beanClass and getter must be specified or a NullPointerException
     * will be thrown. The getter must be a method on the specified beanClass,
     * and it must have a return type of EventHandler, or an
     * IllegalArgumentException will be thrown.
     *
     * @param beanClass The bean class, cannot be null
     * @param getter The getter on the bean class of the property,
     *        cannot be null and must have a return type of EventHandler
     */
    public EventMetaData(Class<?> beanClass, Method getter) {
        super(beanClass, getter);
        init(getter);
    }

    /**
     * A constructor used by BeanMetaData to create an EventMetaData without
     * having to do redundant checks and redundant resource bundle lookup.
     *
     * @param beanClass The bean class, cannot be null
     * @param getter The getter, cannot be null
     * @param bundle The bundle, cannot be null
     */
    EventMetaData(Class<?> beanClass, Method getter, Resources bundle) {
        super(beanClass, getter, bundle);
        init(getter);
    }

    /**
     * @InheritDoc
     */
    @Override MetaDataAnnotation getMetaDataAnnotation(Method getter) {
        final Event a = getter.getAnnotation(Event.class);
        if (a == null) return null;
        return new MetaDataAnnotation() {
            @Override public String displayName() {
                return a.displayName();
            }

            @Override public String shortDescription() {
                return a.shortDescription();
            }

            @Override public String category() {
                return a.category();
            }
        };
    }

    /**
     * Extracts annotation information from the Event annotation specific to
     * the Event.
     * 
     * @param getter The getter on the bean class of the property,
     *        cannot be null and must have a return type of EventHandler
     */
    private void init(Method getter) {
        // Get the annotations on this method. Look for the event specific
        // "eventType" annotation and use that as the event type
        Event eventAnnotation = getter.getAnnotation(Event.class);
        type = eventAnnotation == null ?
                javafx.event.Event.class :
                eventAnnotation.eventType();

    }

    /**
     * Gets the type of the javafx.event.Event used with the EventHandler for
     * this Event. This must be specified via the
     * com.sun.javafx.beans.metadata.Event annotation on the getter for the
     * event.
     *
     * @return The event type
     */
    public final Class<? extends javafx.event.Event> getEventType() {
        return type;
    }
}
