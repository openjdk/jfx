/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.transform;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventTarget;
import org.junit.Test;

import static org.junit.Assert.*;

public class TransformChangedEventTest {

    // Event generation is tested in TransformOperationsTest

    @Test
    public void testDefaultConstructor() {
        TransformChangedEvent e = new TransformChangedEvent();
        assertSame(TransformChangedEvent.TRANSFORM_CHANGED, e.getEventType());
        assertSame(Event.NULL_SOURCE_TARGET, e.getSource());
        assertSame(Event.NULL_SOURCE_TARGET, e.getTarget());
    }

    @Test
    public void testConstructor() {
        final EventTarget src = new EventTarget() {
            @Override
            public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
                return null;
            }
        };
        final EventTarget trg = new EventTarget() {
            @Override
            public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
                return null;
            }
        };
        TransformChangedEvent e = new TransformChangedEvent(src, trg);
        assertSame(TransformChangedEvent.TRANSFORM_CHANGED, e.getEventType());
        assertSame(src, e.getSource());
        assertSame(trg, e.getTarget());
    }

    // RT-28932
    @Test
    public void canCreateActionEventToo() {
        TransformChangedEvent event = new TransformChangedEvent(null, null);
        ActionEvent actionEvent = new ActionEvent(null, null);
    }
}
