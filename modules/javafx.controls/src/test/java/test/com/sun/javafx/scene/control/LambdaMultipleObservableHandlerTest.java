/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.sun.javafx.scene.control.LambdaMultiplePropertyChangeListenerHandler;

import static org.junit.Assert.*;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.*;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;

/**
 * Test LambdaMultiplePropertyChangeListenerHandler.
 * <p>
 *
 * This test is parameterized in testing change- or invalidationListener api.
 */
@RunWith(Parameterized.class)
public class LambdaMultipleObservableHandlerTest {

    private LambdaMultiplePropertyChangeListenerHandler handler;
    private boolean useChangeListener;

// -------------- unregister

    /**
     * Single consumer for multiple observables: test that
     * removing from one observable doesn't effect listening to other observable
     */
    @Test
    public void testUnregistersSingleConsumerMultipleObservables() {
        IntegerProperty p = new SimpleIntegerProperty();
        IntegerProperty other = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        Consumer<ObservableValue<?>> consumer = c -> count[0]++;
        registerListener(p, consumer);
        registerListener(other, consumer);
        unregisterListeners(other);
        p.set(100);
        other.set(100);
        assertEquals(1, count[0]);
    }

    /**
     * Test that all consumers for a single observable are removed
     * and manually adding the removed consumer chain as listener
     * has the same effect as when invoked via handler.
     */
    @Test
    public void testUnregistersMultipleConsumers() {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] action = new int[] {0};
        int actionValue = 10;
        int[] secondAction = new int[] {0};
        // register multiple consumers
        registerListener(p, c -> action[0] = actionValue);
        registerListener(p, c -> secondAction[0] = action[0]);
        // remove all
        Consumer removedChain = unregisterListeners(p);
        p.set(100);
        assertEquals("none of the removed listeners must be notified", 0, action[0] + secondAction[0]);

        // manually add the chained consumers
        addListener(p, removedChain);
        p.set(200);
        // assert effect of manually added chain is same
        assertEquals("effect of removed consumer chain", actionValue, action[0]);
        assertEquals("effect of removed consumer chain", action[0], secondAction[0]);
    }

    @Test
    public void testUnregistersSingleConsumer() {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        Consumer<Observable> consumer = c -> count[0]++;
        registerListener(p, consumer);
        Consumer<Observable> removed = unregisterListeners(p);
        p.set(100);
        assertEquals(0, count[0]);
        assertSame("single registered listener must be returned", consumer, removed);
    }

    /**
     * Test unregisters not registered observable.
     */
    @Test
    public void testUnregistersNotRegistered() {
        IntegerProperty p = new SimpleIntegerProperty();
        assertNull(unregisterListeners(p));
    }

    @Test
    public void testUnregistersNull() {
        assertNull(unregisterListeners(null));
    }


//------------ register

    @Test
    public void testRegisterConsumerToMultipleObservables() {
        IntegerProperty p = new SimpleIntegerProperty();
        IntegerProperty other = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        Consumer<Observable> consumer = c -> count[0]++;
        registerListener(p, consumer);
        registerListener(other, consumer);
        p.set(100);
        other.set(100);
        assertEquals(2, count[0]);
    }

    /**
     * Test that multiple consumers to same observable are invoked in order
     * of registration.
     */
    @Test
    public void testRegisterMultipleConsumerToSingleObservable() {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] action = new int[] {0};
        int actionValue = 10;
        int[] secondAction = new int[] {0};
        registerListener(p, c -> action[0] = actionValue);
        registerListener(p, c -> secondAction[0] = action[0]);
        p.set(100);
        assertEquals(actionValue, action[0]);
        assertEquals(action[0], secondAction[0]);
    }

    @Test
    public void testRegister() {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        registerListener(p, c -> count[0]++);
        p.set(100);
        assertEquals(1, count[0]);
    }

    @Test
    public void testRegisterNullConsumer() {
        IntegerProperty p = new SimpleIntegerProperty();
        registerListener(p, null);
    }

    @Test
    public void testRegisterNullObservable() {
        registerListener(null, c -> {});
    }

//--------- dispose

    @Test
    public void testDispose() {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        registerListener(p, c -> count[0]++);
        handler.dispose();
        p.set(100);
        assertEquals("listener must not be invoked after dispose", 0, count[0]);
        // re-register
        registerListener(p, c -> count[0]++);
        p.set(200);
        assertEquals("listener must be invoked when re-registered after dispose", 1, count[0]);
    }


//--------- test weak registration

    /**
     * Test that handler is gc'ed and listener no longer notified.
     */
    @Test
    public void testRegisterMemoryLeak() {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        Consumer<ObservableValue<?>> consumer = c -> count[0]++;
        LambdaMultiplePropertyChangeListenerHandler handler = new LambdaMultiplePropertyChangeListenerHandler();
        WeakReference<LambdaMultiplePropertyChangeListenerHandler> ref =
                new WeakReference<>(handler);
        registerListener(handler, p, consumer);
        p.setValue(100);
        int notified = count[0];
        assertEquals("sanity: listener invoked", notified, count[0]);
        assertNotNull(ref.get());
        handler = null;
        attemptGC(ref);
        assertNull("handler must be gc'ed", ref.get());
        p.setValue(200);
        assertEquals("listener must not be invoked after gc", notified, count[0]);
    }


//-------------- not-parameterized tests
// guard against cross-over effects for change/invalidationListener on same observable

    /**
     * Register both invalidation/change listener on same property.
     */
    @Test
    public void testRegisterBoth() {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        handler.registerChangeListener(p, c -> count[0]++);
        handler.registerInvalidationListener(p, c -> count[0]++);
        p.set(100);
        assertEquals("both listener types must be invoked", 2, count[0]);
    }

    /**
     * Register both invalidation/change listener, remove change listener
     */
    @Test
    public void testRegisterBothRemoveChangeListener() {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        handler.registerChangeListener(p, c -> count[0]++);
        handler.registerInvalidationListener(p, c -> count[0]++);
        handler.unregisterChangeListeners(p);
        p.set(200);
        assertEquals("", 1, count[0]);
    }

    /**
     * Register both invalidation/change listener, remove invalidationListener.
     */
    @Test
    public void testRegisterBothRemoveInvalidationListener() {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        handler.registerChangeListener(p, c -> count[0]++);
        handler.registerInvalidationListener(p, c -> count[0]++);
        handler.unregisterInvalidationListeners(p);
        p.set(200);
        assertEquals("", 1, count[0]);
    }

    /**
     * Test that binding is invalid.
     */
    @Test
    public void testBindingInvalid() {
        IntegerProperty num1 = new SimpleIntegerProperty(1);
        IntegerProperty num2 = new SimpleIntegerProperty(2);
        NumberBinding p = Bindings.add(num1,num2);
        int[] count = new int[] {0};
        handler.registerChangeListener(p, c -> count[0]++);
        handler.registerInvalidationListener(p, c -> count[0]++);
        handler.unregisterChangeListeners(p);
        num1.set(200);
        assertEquals("sanity: received invalidation", 1, count[0]);
        assertFalse("binding must not be valid", p.isValid());
    }


//----------------------- helpers to un/registration listeners

    /**
     * Registers the consumer for notification from the observable,
     * using the default handler.
     *
     */
    protected void registerListener(Observable p, Consumer consumer) {
        registerListener(handler, p, consumer);
    }

    /**
     * Registers the consumer for notification from the observable,
     * using the given handler.
     */
    protected void registerListener(LambdaMultiplePropertyChangeListenerHandler handler, Observable p, Consumer consumer) {
        if (useChangeListener) {
            handler.registerChangeListener((ObservableValue<?>) p, consumer);
        } else {
            handler.registerInvalidationListener(p, consumer);
        }
    }

    /**
     * Unregisters listeners from observable, using default handler
     */
    protected Consumer unregisterListeners(Observable p) {
        return unregisterListeners(handler, p);
    }

    /**
     * Unregisters listeners from observable, using default handler
     */
    protected Consumer unregisterListeners(LambdaMultiplePropertyChangeListenerHandler handler, Observable p) {
        if (useChangeListener) {
            return handler.unregisterChangeListeners((ObservableValue<?>) p);
        }
        return handler.unregisterInvalidationListeners(p);
    }

    protected void addListener(ObservableValue<?> p, Consumer<Observable> consumer) {
        if (useChangeListener) {
           p.addListener((obs, ov, nv) -> consumer.accept(obs));
        } else {
           p.addListener(obs -> consumer.accept(obs));
        }
    }


//-------------- parameters

    // Note: name property not supported before junit 4.11
    @Parameterized.Parameters //(name = "{index}: changeListener {0} ")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] {
                {true}, // test changeListener api
                {false} // test invalidationListener api
        };
        return Arrays.asList(data);
    }

    public LambdaMultipleObservableHandlerTest(boolean useChangeListener) {
        this.useChangeListener = useChangeListener;
    }


//------------ setup and initial

    @Before
    public void setup() {
        this.handler = new LambdaMultiplePropertyChangeListenerHandler();
    }

}
