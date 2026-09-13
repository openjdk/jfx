/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.beans.value;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Subscription;

public class ObservableValueSubscriptionsTest {
    private final StringProperty value = new SimpleStringProperty("Initial");

    @Test
    void subscribeConsumerShouldCallSubscriberImmediatelyAndAfterEachChange() {
        AtomicReference<String> lastCall = new AtomicReference<>();

        assertNull(lastCall.get());

        Subscription subscription = value.subscribe(lastCall::set);

        assertEquals("Initial", lastCall.get());  // provides initial upon subscribing

        value.set("A");

        assertEquals("A", lastCall.get());

        value.set("B");

        assertEquals("B", lastCall.get());

        lastCall.set(null);

        value.set("B");

        assertNull(lastCall.get());  // unchanged when changing from B to B

        subscription.unsubscribe();

        value.set("C");

        assertNull(lastCall.get());  // unchanged as unsubscribed
    }

    @Test
    void subscribeConsumerShouldRejectNull() {
        assertThrows(NullPointerException.class, () -> value.subscribe((Consumer<String>) null));
    }

    @Test
    void subscribeBiConsumerShouldCallSubscriberAfterEachChange() {
        AtomicReference<String> lastCall = new AtomicReference<>();

        assertNull(lastCall.get());

        Subscription subscription = value.subscribe((old, current) -> lastCall.set(old + " -> " + current));

        assertNull(lastCall.get());  // Nothing happens upon subscribing

        value.set("A");

        assertEquals("Initial -> A", lastCall.get());

        value.set("B");

        assertEquals("A -> B", lastCall.get());

        lastCall.set(null);

        value.set("B");

        assertNull(lastCall.get());  // unchanged when changing from B to B

        subscription.unsubscribe();

        value.set("C");

        assertNull(lastCall.get());  // unchanged as unsubscribed
    }

    @Test
    void subscribeBiConsumerShouldRejectNull() {
        assertThrows(NullPointerException.class, () -> value.subscribe((BiConsumer<String, String>) null));
    }
}
