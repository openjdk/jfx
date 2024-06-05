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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import javafx.util.Subscription;

public class SubscriptionTest {

    @Test
    void ofShouldReturnSubscriptionWhichCanCancelAllGivenSubscriptions() {
        AtomicReference<String> a = new AtomicReference<>();
        AtomicReference<String> b = new AtomicReference<>();
        AtomicReference<String> c = new AtomicReference<>();

        Subscription subscription = Subscription.combine(
            () -> a.set("canceled"),
            () -> b.set("canceled"),
            () -> c.set("canceled")
        );

        assertNull(a.get());
        assertNull(b.get());
        assertNull(c.get());

        subscription.unsubscribe();

        assertEquals("canceled", a.get());
        assertEquals("canceled", b.get());
        assertEquals("canceled", c.get());
    }

    @Test
    void ofShouldRejectNulls() {
        assertThrows(NullPointerException.class, () -> Subscription.combine((Subscription[]) null));
        assertThrows(NullPointerException.class, () -> Subscription.combine((Subscription) null));
        assertThrows(NullPointerException.class, () -> Subscription.combine(Subscription.EMPTY, null, () -> {}));
    }

    @Test
    void andShouldReturnSubscriptionWhichCanCancelBothSubscriptions() {
        AtomicReference<String> a = new AtomicReference<>();
        AtomicReference<String> b = new AtomicReference<>();

        Subscription subscription1 = () -> a.set("canceled");
        Subscription subscription2 = () -> b.set("canceled");

        Subscription combined = subscription1.and(subscription2);

        assertNull(a.get());
        assertNull(b.get());

        combined.unsubscribe();

        assertEquals("canceled", a.get());
        assertEquals("canceled", b.get());
    }

    @Test
    void andShouldRejectNull() {
        assertThrows(NullPointerException.class, () -> Subscription.EMPTY.and(null));
    }
}
