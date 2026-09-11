/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.beans.property;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javafx.beans.property.DeclaringClassCacheShim;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleStringProperty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DeclaringClassCacheTest {

    private static final int THREAD_COUNT = 16;

    @Test
    void cachesResolvedClassForBeanClassAndPropertyName() {
        var invocationCount = new AtomicInteger();
        Function<ReadOnlyProperty<?>, Class<?>> supplier = _ -> {
            invocationCount.incrementAndGet();
            return FirstDeclaringClass.class;
        };

        assertSame(FirstDeclaringClass.class, compute(new SharedBean(), "shared", supplier));
        assertSame(FirstDeclaringClass.class, compute(new SharedBean(), "shared", supplier));
        assertEquals(1, invocationCount.get());
    }

    @Test
    void cachesNullResult() {
        var invocationCount = new AtomicInteger();
        Function<ReadOnlyProperty<?>, Class<?>> supplier = _ -> {
            invocationCount.incrementAndGet();
            return null;
        };

        assertNull(compute(new NullBean(), "missing", supplier));
        assertNull(compute(new NullBean(), "missing", supplier));
        assertEquals(1, invocationCount.get());
    }

    @Test
    void keepsPropertyNamesSeparate() {
        var bean = new PropertyNameBean();

        assertSame(FirstDeclaringClass.class, compute(bean, "first", _ -> FirstDeclaringClass.class));
        assertSame(SecondDeclaringClass.class, compute(bean, "second", _ -> SecondDeclaringClass.class));
        assertSame(FirstDeclaringClass.class, compute(bean, "first", _ -> fail("Supplier invoked for cached property")));
        assertSame(SecondDeclaringClass.class, compute(bean, "second", _ -> fail("Supplier invoked for cached property")));
    }

    @Test
    void keepsBeanClassesSeparate() {
        assertSame(FirstDeclaringClass.class, compute(new FirstBean(), "shared", _ -> FirstDeclaringClass.class));
        assertSame(SecondDeclaringClass.class, compute(new SecondBean(), "shared", _ -> SecondDeclaringClass.class));
    }

    @Test
    void supportsConcurrentClassResults() throws Exception {
        assertConcurrentResult(new ConcurrentClassBean(), "concurrentClass", FirstDeclaringClass.class);
    }

    @Test
    void supportsConcurrentNullResults() throws Exception {
        assertConcurrentResult(new ConcurrentNullBean(), "concurrentNull", null);
    }

    private static Class<?> compute(Object bean,
                                    String propertyName,
                                    Function<ReadOnlyProperty<?>, Class<?>> supplier) {
        var property = new SimpleStringProperty(bean, "ignored");
        return DeclaringClassCacheShim.computeIfAbsent(property, propertyName, supplier);
    }

    private static void assertConcurrentResult(Object bean, String propertyName, Class<?> expected) throws Exception {
        var barrier = new CyclicBarrier(THREAD_COUNT);
        List<Callable<Class<?>>> tasks = new ArrayList<>(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            tasks.add(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return compute(bean, propertyName, _ -> expected);
            });
        }

        try (var executor = Executors.newFixedThreadPool(THREAD_COUNT)) {
            List<Future<Class<?>>> futures = executor.invokeAll(tasks);

            for (Future<Class<?>> future : futures) {
                assertSame(expected, future.get());
            }
        }

        assertSame(expected, compute(bean, propertyName, _ -> fail("Supplier invoked for cached property")));
    }

    private static final class SharedBean {}
    private static final class NullBean {}
    private static final class PropertyNameBean {}
    private static final class FirstBean {}
    private static final class SecondBean {}
    private static final class ConcurrentClassBean {}
    private static final class ConcurrentNullBean {}
    private static final class FirstDeclaringClass {}
    private static final class SecondDeclaringClass {}
}
