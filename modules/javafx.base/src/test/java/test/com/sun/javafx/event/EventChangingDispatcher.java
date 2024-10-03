/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.event;

import javafx.event.Event;
import javafx.event.EventDispatchChain;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class EventChangingDispatcher extends LabeledEventDispatcher {
    private final Operation capturingPhaseOperation;
    private final Operation bubblingPhaseOperation;

    public EventChangingDispatcher(final Operation capturingPhaseOperation,
                                   final Operation bubblingPhaseOperation) {
        this(null, capturingPhaseOperation, bubblingPhaseOperation);
    }

    public EventChangingDispatcher(final String label,
                                   final Operation capturingPhaseOperation,
                                   final Operation bubblingPhaseOperation) {
        super(label);
        this.capturingPhaseOperation = capturingPhaseOperation;
        this.bubblingPhaseOperation = bubblingPhaseOperation;
    }

    @Override
    public Event dispatchEvent(final Event event,
                               final EventDispatchChain tail) {
        assertTrue(event instanceof ValueEvent);
        ValueEvent valueEvent = (ValueEvent) event;

        if (capturingPhaseOperation != null) {
            valueEvent.setValue(capturingPhaseOperation.applyTo(
                    valueEvent.getValue()));
        }
        valueEvent = (ValueEvent) tail.dispatchEvent(valueEvent);
        if (bubblingPhaseOperation != null) {
            valueEvent.setValue(bubblingPhaseOperation.applyTo(
                    valueEvent.getValue()));
        }

        return valueEvent;
    }
}
