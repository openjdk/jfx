/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.attemptGC;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.javafx.scene.control.LambdaMultiplePropertyChangeListenerHandler;

/**
 * Test LambdaMultiplePropertyChangeListenerHandler.
 * <p>
 *
 * This test is parameterized in testing change- or invalidationListener api.
 */
public class LambdaMultipleObservableHandlerTest {

    private LambdaMultiplePropertyChangeListenerHandler handler;

// -------------- unregister

    /**
     * Single consumer for multiple observables: test that
     * removing from one observable doesn't effect listening to other observable
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testUnregistersSingleConsumerMultipleObservables(boolean useChangeListener) {
        IntegerProperty p = new SimpleIntegerProperty();
        IntegerProperty other = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        Consumer<ObservableValue<?>> consumer = c -> count[0]++;
        registerListener(useChangeListener, p, consumer);
        registerListener(useChangeListener, other, consumer);
        unregisterListeners(useChangeListener, other);
        p.set(100);
        other.set(100);
        assertEquals(1, count[0]);
    }

    /**
     * Test that all consumers for a single observable are removed
     * and manually adding the removed consumer chain as listener
     * has the same effect as when invoked via handler.
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testUnregistersMultipleConsumers(boolean useChangeListener) {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] action = new int[] {0};
        int actionValue = 10;
        int[] secondAction = new int[] {0};
        // register multiple consumers
        registerListener(useChangeListener, p, c -> action[0] = actionValue);
        registerListener(useChangeListener, p, c -> secondAction[0] = action[0]);
        // remove all
        Consumer removedChain = unregisterListeners(useChangeListener, p);
        p.set(100);
        assertEquals(0, action[0] + secondAction[0], "none of the removed listeners must be notified");

        // manually add the chained consumers
        addListener(useChangeListener, p, removedChain);
        p.set(200);
        // assert effect of manually added chain is same
        assertEquals(actionValue, action[0], "effect of removed consumer chain");
        assertEquals(action[0], secondAction[0], "effect of removed consumer chain");
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testUnregistersSingleConsumer(boolean useChangeListener) {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        Consumer<Observable> consumer = c -> count[0]++;
        registerListener(useChangeListener, p, consumer);
        Consumer<Observable> removed = unregisterListeners(useChangeListener, p);
        p.set(100);
        assertEquals(0, count[0]);
        assertSame(consumer, removed, "single registered listener must be returned");
    }

    /**
     * Test unregisters not registered observable.
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testUnregistersNotRegistered(boolean useChangeListener) {
        IntegerProperty p = new SimpleIntegerProperty();
        assertNull(unregisterListeners(useChangeListener, p));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testUnregistersNull(boolean useChangeListener) {
        assertNull(unregisterListeners(useChangeListener, null));
    }


//------------ register

    @ParameterizedTest
    @MethodSource("data")
    public void testRegisterConsumerToMultipleObservables(boolean useChangeListener) {
        IntegerProperty p = new SimpleIntegerProperty();
        IntegerProperty other = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        Consumer<Observable> consumer = c -> count[0]++;
        registerListener(useChangeListener, p, consumer);
        registerListener(useChangeListener, other, consumer);
        p.set(100);
        other.set(100);
        assertEquals(2, count[0]);
    }

    /**
     * Test that multiple consumers to same observable are invoked in order
     * of registration.
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testRegisterMultipleConsumerToSingleObservable(boolean useChangeListener) {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] action = new int[] {0};
        int actionValue = 10;
        int[] secondAction = new int[] {0};
        registerListener(useChangeListener, p, c -> action[0] = actionValue);
        registerListener(useChangeListener, p, c -> secondAction[0] = action[0]);
        p.set(100);
        assertEquals(actionValue, action[0]);
        assertEquals(action[0], secondAction[0]);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testRegister(boolean useChangeListener) {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        registerListener(useChangeListener, p, c -> count[0]++);
        p.set(100);
        assertEquals(1, count[0]);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testRegisterNullConsumer(boolean useChangeListener) {
        IntegerProperty p = new SimpleIntegerProperty();
        registerListener(useChangeListener, p, null);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testRegisterNullObservable(boolean useChangeListener) {
        registerListener(useChangeListener, null, c -> {});
    }

//--------- dispose

    @ParameterizedTest
    @MethodSource("data")
    public void testDispose(boolean useChangeListener) {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        registerListener(useChangeListener, p, c -> count[0]++);
        handler.dispose();
        p.set(100);
        assertEquals(0, count[0], "listener must not be invoked after dispose");
        // re-register
        registerListener(useChangeListener, p, c -> count[0]++);
        p.set(200);
        assertEquals(1, count[0], "listener must be invoked when re-registered after dispose");
    }


//--------- test weak registration

    /**
     * Test that handler is gc'ed and listener no longer notified.
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testRegisterMemoryLeak(boolean useChangeListener) {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        Consumer<ObservableValue<?>> consumer = c -> count[0]++;
        LambdaMultiplePropertyChangeListenerHandler handler = new LambdaMultiplePropertyChangeListenerHandler();
        WeakReference<LambdaMultiplePropertyChangeListenerHandler> ref =
                new WeakReference<>(handler);
        registerListener(useChangeListener, handler, p, consumer);
        p.setValue(100);
        int notified = count[0];
        assertEquals(notified, count[0], "sanity: listener invoked");
        assertNotNull(ref.get());
        handler = null;
        attemptGC(ref);
        assertNull(ref.get(), "handler must be gc'ed");
        p.setValue(200);
        assertEquals(notified, count[0], "listener must not be invoked after gc");
    }


//-------------- not-parameterized tests
// guard against cross-over effects for change/invalidationListener on same observable

    /**
     * Register both invalidation/change listener on same property.
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testRegisterBoth() {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        handler.registerChangeListener(p, c -> count[0]++);
        handler.registerInvalidationListener(p, c -> count[0]++);
        p.set(100);
        assertEquals(2, count[0], "both listener types must be invoked");
    }

    /**
     * Register both invalidation/change listener, remove change listener
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testRegisterBothRemoveChangeListener() {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        handler.registerChangeListener(p, c -> count[0]++);
        handler.registerInvalidationListener(p, c -> count[0]++);
        handler.unregisterChangeListeners(p);
        p.set(200);
        assertEquals(1, count[0]);
    }

    /**
     * Register both invalidation/change listener, remove invalidationListener.
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testRegisterBothRemoveInvalidationListener() {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        handler.registerChangeListener(p, c -> count[0]++);
        handler.registerInvalidationListener(p, c -> count[0]++);
        handler.unregisterInvalidationListeners(p);
        p.set(200);
        assertEquals(1, count[0]);
    }

    /**
     * Test that binding is invalid.
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testBindingInvalid() {
        IntegerProperty num1 = new SimpleIntegerProperty(1);
        IntegerProperty num2 = new SimpleIntegerProperty(2);
        NumberBinding p = Bindings.add(num1,num2);
        int[] count = new int[] {0};
        handler.registerChangeListener(p, c -> count[0]++);
        handler.registerInvalidationListener(p, c -> count[0]++);
        handler.unregisterChangeListeners(p);
        num1.set(200);
        assertEquals(1, count[0], "sanity: received invalidation");
        assertFalse(p.isValid(), "binding must not be valid");
    }


//----------------------- helpers to un/registration listeners

    /**
     * Registers the consumer for notification from the observable,
     * using the default handler.
     *
     */
    protected void registerListener(boolean useChangeListener, Observable p, Consumer consumer) {
        registerListener(useChangeListener, handler, p, consumer);
    }

    /**
     * Registers the consumer for notification from the observable,
     * using the given handler.
     */
    protected void registerListener(
        boolean useChangeListener,
        LambdaMultiplePropertyChangeListenerHandler handler,
        Observable p,
        Consumer consumer
    ) {
        if (useChangeListener) {
            handler.registerChangeListener((ObservableValue<?>) p, consumer);
        } else {
            handler.registerInvalidationListener(p, consumer);
        }
    }

    /**
     * Unregisters listeners from observable, using default handler
     */
    protected Consumer unregisterListeners(boolean useChangeListener, Observable p) {
        return unregisterListeners(useChangeListener, handler, p);
    }

    /**
     * Unregisters listeners from observable, using default handler
     */
    protected Consumer unregisterListeners(boolean useChangeListener, LambdaMultiplePropertyChangeListenerHandler handler, Observable p) {
        if (useChangeListener) {
            return handler.unregisterChangeListeners((ObservableValue<?>) p);
        }
        return handler.unregisterInvalidationListeners(p);
    }

    protected void addListener(boolean useChangeListener, ObservableValue<?> p, Consumer<Observable> consumer) {
        if (useChangeListener) {
           p.addListener((obs, ov, nv) -> consumer.accept(obs));
        } else {
           p.addListener(obs -> consumer.accept(obs));
        }
    }


    /** parameters */
    private static Collection<Boolean> data() {
        return List.of(
            true, // test changeListener api
            false // test invalidationListener api
        );
    }


//------------ setup and initial

    @BeforeEach
    public void setup() {
        this.handler = new LambdaMultiplePropertyChangeListenerHandler();
    }
}
