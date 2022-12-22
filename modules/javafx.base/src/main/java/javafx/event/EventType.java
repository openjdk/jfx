/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

package javafx.event;

// PENDING_DOC_REVIEW

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * This class represents a specific event type associated with an {@code Event}.
 * <p>
 * Event types form a hierarchy with the {@link EventType#ROOT} (equals to
 * {@link Event#ANY}) as its root. This is useful in event filter / handler
 * registration where a single event filter / handler can be registered to a
 * super event type and will be receiving its sub type events as well.
 * Note that you cannot construct two different EventType objects with the same
 * name and parent.
 *
 * <p>
 * <b>Note about deserialization</b>: All EventTypes that are going to be deserialized
 * (e.g. as part of {@link Event} deserialization), need to exist at the time of
 * deserialization. Deserialization of EventType will not create new EventType
 * objects.
 *
 * @param <T> the event class to which this type applies
 * @since JavaFX 2.0
 */
public final class EventType<T extends Event> implements Serializable{

    /**
     * The root event type. All other event types are either direct or
     * indirect sub types of it. It is also the only event type which
     * has its super event type set to {@code null}.
     */
    public static final EventType<Event> ROOT =
            new EventType<>("EVENT", null);

    private WeakHashMap<EventType<? extends T>, Void> subTypes;

    private final EventType<? super T> superType;

    private final String name;

    /**
     * Constructs a new {@code EventType} with the {@code EventType.ROOT} as its
     * super type and the name set to {@code null}.
     * @deprecated Do not use this constructor, as only one such EventType can exist
     */
    @Deprecated
    public EventType() {
        this(ROOT, null);
    }

    /**
     * Constructs a new {@code EventType} with the specified name and the
     * {@code EventType.ROOT} as its super type.
     *
     * @param name the name
     * @throws IllegalArgumentException if an EventType with the same name and
     * {@link EventType#ROOT}/{@link Event#ANY} as parent
     */
    public EventType(final String name) {
        this(ROOT, name);
    }

    /**
     * Constructs a new {@code EventType} with the specified super type and
     * the name set to {@code null}.
     *
     * @param superType the event super type
     * @throws IllegalArgumentException if an EventType with "null" name and
     * under this supertype exists
     */
    public EventType(final EventType<? super T> superType) {
        this(superType, null);
    }

    /**
     * Constructs a new {@code EventType} with the specified super type and
     * name.
     *
     * @param superType the event super type
     * @param name the name
     * @throws IllegalArgumentException if an EventType with the same name and
     * superType exists
     */
    public EventType(final EventType<? super T> superType,
            final String name) {
        if (superType == null) {
            throw new NullPointerException(
                    "Event super type must not be null!");
        }

        this.superType = superType;
        this.name = name;
        superType.register(this);
    }

    /**
     * Internal constructor that skips various checks
     */
    EventType(final String name,
                      final EventType<? super T> superType) {
        this.superType = superType;
        this.name = name;
        if (superType != null) {
            if (superType.subTypes != null) {
                for (Iterator i = superType.subTypes.keySet().iterator(); i.hasNext();) {
                    EventType t  = (EventType) i.next();
                    if (name == null && t.name == null || (name != null && name.equals(t.name))) {
                        i.remove();
                    }
                }
            }
            superType.register(this);
        }
    }

    /**
     * Gets the super type of this event type. The returned value is
     * {@code null} only for the {@code EventType.ROOT}.
     *
     * @return the super type
     */
    public final EventType<? super T> getSuperType() {
        return superType;
    }

    /**
     * Gets the name of this event type.
     *
     * @return the name
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns a string representation of this {@code EventType} object.
     * @return a string representation of this {@code EventType} object.
     */
    @Override
    public String toString() {
        return (name != null) ? name : super.toString();
    }

    private void register(javafx.event.EventType<? extends T> subType) {
        if (subTypes == null) {
            subTypes = new WeakHashMap<>();
        }
        for (EventType<? extends T> t : subTypes.keySet()) {
            if (((t.name == null && subType.name == null) || (t.name != null && t.name.equals(subType.name)))) {
                throw new IllegalArgumentException("EventType \"" + subType + "\""
                        + "with parent \"" + subType.getSuperType()+"\" already exists");
            }
        }
        subTypes.put(subType, null);
    }

    private Object writeReplace() {
        Deque<String> path = new LinkedList<>();
        EventType<?> t = this;
        while (t != ROOT) {
            path.addFirst(t.name);
            t = t.superType;
        }
        return new EventTypeSerialization(new ArrayList<>(path));
    }

    static class EventTypeSerialization implements Serializable {
        private List<String> path;

        public EventTypeSerialization(List<String> path) {
            this.path = path;
        }

        private Object readResolve() throws ObjectStreamException {
            EventType t = ROOT;
            for (int i = 0; i < path.size(); ++i) {
                String p = path.get(i);
                if (t.subTypes != null) {
                    EventType s = findSubType(t.subTypes.keySet(), p);
                    if (s == null) {
                        throw new InvalidObjectException("Cannot find event type \"" + p + "\" (of " + t + ")");
                    }
                    t = s;
                } else {
                    throw new InvalidObjectException("Cannot find event type \"" + p + "\" (of " + t + ")");
                }
            }
            return t;
        }

        private EventType findSubType(Set<EventType> subTypes, String name) {
            for (EventType t : subTypes) {
                if (((t.name == null && name == null) || (t.name != null && t.name.equals(name)))) {
                    return t;
                }
            }
            return null;
        }

    }
}
