/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Subscription;

public class ObservableSubscriptionsTest {
    private final StringProperty value = new SimpleStringProperty("Initial");

    @Test
    void subscribeRunnableShouldCallSubscriberWhenObservableInvalidated() {
        AtomicInteger calls = new AtomicInteger();

        assertEquals(0, calls.get());

        Subscription subscription = value.subscribe(() -> calls.addAndGet(1));

        assertEquals(0, calls.get());

        value.set("A");

        assertEquals(1, calls.get());

        value.set("B");

        assertEquals(1, calls.get());  // already invalid, not called again

        value.get();
        value.set("C");

        assertEquals(2, calls.get());

        subscription.unsubscribe();

        value.get();
        value.set("D");

        assertEquals(2, calls.get());  // unsubscribed, not called
    }

    @Test
    void subscribeRunnableShouldRejectNull() {
        assertThrows(NullPointerException.class, () -> value.subscribe((Runnable) null));
    }
}
