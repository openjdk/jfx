/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * Event related to {@link ScrollPane} and virtualised controls such as
 * {@link ListView}, {@link TableView}, {@link TreeView} and {@link TreeTableView}.
 * @since JavaFX 8.0
 */
public class ScrollToEvent<T> extends Event {
//    /**
//     * This event occurs if the user requests scrolling a node into view.
//     */
//    @SuppressWarnings("unchecked")
//    public static EventType<ScrollToEvent<Node>> scrollToNode() {
//        return SCROLL_TO_NODE;
//    }
//    private static final EventType<ScrollToEvent<Node>> SCROLL_TO_NODE =
//            new EventType<ScrollToEvent<Node>>(ScrollToEvent.ANY, "SCROLL_TO_NODE");

    /**
     * Common supertype for all scroll-to event types.
     */
    public static final EventType<ScrollToEvent> ANY =
            new EventType<ScrollToEvent> (Event.ANY, "SCROLL_TO");

    /**
     * This event occurs if the user requests scrolling a given index into view.
     * @return the scroll to event type
     */
    public static EventType<ScrollToEvent<Integer>> scrollToTopIndex() {
        return SCROLL_TO_TOP_INDEX;
    }
    private static final EventType<ScrollToEvent<Integer>> SCROLL_TO_TOP_INDEX =
            new EventType<ScrollToEvent<Integer>>(ScrollToEvent.ANY, "SCROLL_TO_TOP_INDEX");


    /**
     * This event occurs if the user requests scrolling a {@link TableColumnBase}
     * (i.e. {@link TableColumn} or {@link TreeTableColumn}) into view.
     * @param <T> the type
     * @return the scroll to event type
     */
    @SuppressWarnings("unchecked")
    public static <T extends TableColumnBase<?, ?>> EventType<ScrollToEvent<T>> scrollToColumn() {
        return (EventType<ScrollToEvent<T>>) SCROLL_TO_COLUMN;
    }
    private static final EventType<?> SCROLL_TO_COLUMN =
            new EventType<>(ScrollToEvent.ANY, "SCROLL_TO_COLUMN");

    private static final long serialVersionUID = -8557345736849482516L;

    private final T scrollTarget;

    /**
     * Construct a new {@code Event} with the specified event source, target
     * and type. If the source or target is set to {@code null}, it is replaced
     * by the {@code NULL_SOURCE_TARGET} value.
     *
     * @param source the event source which sent the event
     * @param target the event source which sent the event
     * @param type the event type
     * @param scrollTarget the target of the scroll to operation
     */
    public ScrollToEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target, @NamedArg("type") EventType<ScrollToEvent<T>> type, @NamedArg("scrollTarget") T scrollTarget) {
        super(source, target, type);
        assert scrollTarget != null;
        this.scrollTarget = scrollTarget;

    }

    public T getScrollTarget() {
        return scrollTarget;
    }
}
