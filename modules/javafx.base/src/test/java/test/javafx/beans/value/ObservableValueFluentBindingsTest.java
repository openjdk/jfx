/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class ObservableValueFluentBindingsTest {

    private int invalidations;

    private final List<String> values = new ArrayList<>();
    private final ChangeListener<String> changeListener = (obs, old, current) -> values.add(current);
    private final InvalidationListener invalidationListener = obs -> invalidations++;

    private StringProperty property = new SimpleStringProperty("Initial");

    @Nested
    class When_map_Called {

        @Nested
        class WithNull {

            @Test
            void shouldThrowNullPointerException() {
                assertThrows(NullPointerException.class, () -> property.map(null));
            }
        }

        @Nested
        class WithNotNullReturns_ObservableValue_Which {
            private ObservableValue<String> observableValue = property.map(v -> v + "+map");

            @Test
            void shouldNotBeNull() {
                assertNotNull(observableValue);
            }

            @Test
            void shouldNotBeStronglyReferenced() {
                ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> observableValue = null);
            }

            @Nested
            class When_getValue_Called {

                @Test
                void shouldReturnPropertyValuesWithOperationApplied() {
                    assertEquals("Initial+map", observableValue.getValue());

                    property.set("Left");

                    assertEquals("Left+map", observableValue.getValue());
                }

                @Test
                void shouldNotOperateOnNull() {
                    property.set(null);

                    assertEquals((String) null, observableValue.getValue());
                }
            }

            @Nested
            class WhenObservedForInvalidations {
                {
                    startObservingInvalidations(observableValue);
                }

                @Test
                void shouldOnlyInvalidateOnce() {
                    assertNotInvalidated();

                    property.set("Left");

                    assertInvalidated();

                    property.set("Right");

                    assertNotInvalidated();
                }

                @Test
                void shouldBeStronglyReferenced() {
                    ReferenceAsserts.testIfStronglyReferenced(observableValue, () -> observableValue = null);
                }

                @Nested
                class AndWhenUnobserved {
                    {
                        stopObservingInvalidations(observableValue);
                    }

                    @Test
                    void shouldNoLongerBeCalled() {
                        assertNotInvalidated();

                        property.set("Left");
                        property.set("Right");

                        assertNotInvalidated();
                    }

                    @Test
                    void shouldNoLongerBeStronglyReferenced() {
                        ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> observableValue = null);
                    }
                }
            }

            @Nested
            class WhenObservedForChanges {
                {
                    startObservingChanges(observableValue);
                }

                @Test
                void shouldApplyOperation() {
                    assertNothingIsObserved();

                    property.set("Right");

                    assertObserved("Right+map");
                }

                @Test
                void shouldNotOperateOnNull() {
                    property.set(null);

                    assertObserved((String) null);  // map operation is skipped (as it would NPE otherwise) and the resulting value is null
                }

                @Test
                void shouldBeStronglyReferenced() {
                    ReferenceAsserts.testIfStronglyReferenced(observableValue, () -> observableValue = null);
                }

                @Nested
                class AndWhenUnobserved {
                    {
                        stopObservingChanges(observableValue);
                    }

                    @Test
                    void shouldNoLongerBeCalled() {
                        assertNothingIsObserved();

                        property.set("Right");

                        assertNothingIsObserved();
                    }

                    @Test
                    void shouldNoLongerBeStronglyReferenced() {
                        ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> observableValue = null);
                    }
                }
            }

            @Nested
            class When_orElse_CalledReturns_ObservableValue_Which {
                {
                    observableValue = observableValue.orElse("Empty");
                }

                @Test
                void shouldNotBeNull() {
                    assertNotNull(observableValue);
                }

                @Test
                void shouldNotBeStronglyReferenced() {
                    ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> observableValue = null);
                }

                @Nested
                class WhenObservedForChanges {
                    {
                        startObservingChanges(observableValue);
                    }

                    @Test
                    void shouldApplyMapThenOrElseOperation() {
                        assertNothingIsObserved();

                        property.set("Left");

                        assertObserved("Left+map");

                        property.set(null);

                        assertObserved("Empty");
                    }

                    @Test
                    void shouldBeStronglyReferenced() {
                        ReferenceAsserts.testIfStronglyReferenced(observableValue, () -> observableValue = null);
                    }

                    @Nested
                    class AndWhenUnobserved {
                        {
                            stopObservingChanges(observableValue);
                        }

                        @Test
                        void shouldNoLongerBeCalled() {
                            assertNothingIsObserved();

                            property.set("Left");

                            assertNothingIsObserved();
                        }

                        @Test
                        void shouldNoLongerBeStronglyReferenced() {
                            ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> observableValue = null);
                        }
                    }
                }
            }

            @Nested
            class When_map_CalledAgainReturns_ObservableValue_Which {
                {
                    observableValue = observableValue.map(v -> v + "+map2");
                }

                @Test
                void shouldNotBeNull() {
                    assertNotNull(observableValue);
                }

                @Test
                void shouldNotBeStronglyReferenced() {
                    ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> observableValue = null);
                }

                @Nested
                class WhenObservedForChanges {
                    {
                        startObservingChanges(observableValue);
                    }

                    @Test
                    void shouldApplyMapThenSecondMapOperation() {
                        assertNothingIsObserved();

                        property.set("Left");

                        assertObserved("Left+map+map2");

                        property.set(null);

                        assertObserved((String) null);
                    }

                    @Test
                    void shouldBeStronglyReferenced() {
                        ReferenceAsserts.testIfStronglyReferenced(observableValue, () -> observableValue = null);
                    }

                    @Nested
                    class AndWhenUnobserved {
                        {
                            stopObservingChanges(observableValue);
                        }

                        @Test
                        void shouldNoLongerBeCalled() {
                            assertNothingIsObserved();

                            property.set("Left");

                            assertNothingIsObserved();
                        }

                        @Test
                        void shouldNoLongerBeStronglyReferenced() {
                            ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> observableValue = null);
                        }
                    }
                }
            }
        }
    }

    @Nested
    class When_orElse_CalledReturns_ObservableValue_Which {
        private ObservableValue<String> observableValue = property.orElse("Empty");

        @Test
        void shouldNotBeNull() {
            assertNotNull(observableValue);
        }

        @Test
        void shouldNotBeStronglyReferenced() {
            ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> observableValue = null);
        }

        @Nested
        class When_getValue_Called {

            @Test
            void shouldReturnPropertyValuesWithOperationApplied() {
                assertEquals("Initial", observableValue.getValue());

                property.set(null);

                assertEquals("Empty", observableValue.getValue());
            }
        }

        @Nested
        class WhenObservedForInvalidations {
            {
                startObservingInvalidations(observableValue);
            }

            @Test
            void shouldOnlyInvalidateOnce() {
                assertNotInvalidated();

                property.set("Left");

                assertInvalidated();

                property.set(null);

                assertNotInvalidated();
            }

            @Test
            void shouldBeStronglyReferenced() {
                ReferenceAsserts.testIfStronglyReferenced(observableValue, () -> observableValue = null);
            }

            @Nested
            class AndWhenUnobserved {
                {
                    stopObservingInvalidations(observableValue);
                }

                @Test
                void shouldNoLongerBeCalled() {
                    assertNotInvalidated();

                    property.set("Left");
                    property.set(null);

                    assertNotInvalidated();
                }

                @Test
                void shouldNoLongerBeStronglyReferenced() {
                    ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> observableValue = null);
                }
            }
        }

        @Nested
        class WhenObservedForChanges {
            {
                startObservingChanges(observableValue);
            }

            @Test
            void shouldApplyOperation() {
                assertNothingIsObserved();

                property.set("Left");

                assertObserved("Left");

                property.set(null);

                assertObserved("Empty");
            }

            @Test
            void shouldBeStronglyReferenced() {
                ReferenceAsserts.testIfStronglyReferenced(observableValue, () -> observableValue = null);
            }

            @Nested
            class AndWhenUnobserved {
                {
                    stopObservingChanges(observableValue);
                }

                @Test
                void shouldNoLongerBeCalled() {
                    assertNothingIsObserved();

                    property.set("Left");

                    assertNothingIsObserved();
                }

                @Test
                void shouldNoLongerBeStronglyReferenced() {
                    ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> observableValue = null);
                }
            }
        }
    }

    @Nested
    class When_flatMap_Called {

        @Nested
        class WithNull {

            @Test
            void shouldThrowNullPointerException() {
                assertThrows(NullPointerException.class, () -> property.flatMap(null));
            }
        }

        @Nested
        class WithNotNullReturns_ObservableValue_Which {
            private int subscribeCount;
            private int unsubscribeCount;

            private StringProperty left = new SimpleStringProperty("LEFT");
            private StringProperty right = new SimpleStringProperty("RIGHT");
            private StringProperty unknown = new SimpleStringProperty("UNKNOWN") {
                @Override
                public void addListener(InvalidationListener listener) {
                    super.addListener(listener);

                    subscribeCount++;
                }

                @Override
                public void removeListener(InvalidationListener listener) {
                    super.removeListener(listener);

                    unsubscribeCount++;
                }
            };

            private ObservableValue<String> observableValue =
                    property.flatMap(v -> "Left".equals(v) ? left : "Right".equals(v) ? right : unknown);

            @Test
            void shouldNotBeNull() {
                assertNotNull(observableValue);
            }

            @Test
            void shouldNotBeStronglyReferenced() {
                ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> observableValue = null);
            }

            @Nested
            class When_getValue_Called {

                @Test
                void shouldReturnPropertyValuesWithOperationApplied() {
                    assertEquals("UNKNOWN", observableValue.getValue());  // initially it is not left or right, so unknown

                    property.set("Right");

                    assertEquals("RIGHT", observableValue.getValue());

                    right.setValue("RIGHT+1");

                    assertEquals("RIGHT+1", observableValue.getValue());

                    left.setValue("LEFT+1");
                    unknown.setValue("UNKNOWN+1");

                    assertEquals("RIGHT+1", observableValue.getValue());  // changing left or unknown value should have no effect

                    property.set("Left");

                    assertEquals("LEFT+1", observableValue.getValue());  // after switching to left, it switches to the left value
                }

                @Test
                void shouldNotOperateOnNull() {
                    property.set(null);

                    assertNull(observableValue.getValue());
                }

                @Test
                void shouldIgnoreFlatMapsToNull() {
                    unknown = null;

                    assertNull(observableValue.getValue());
                }
            }

            @Nested
            class WhenObservedForInvalidations {
                {
                    startObservingInvalidations(observableValue);
                }

                @Test
                void shouldOnlyInvalidateOnce() {
                    assertNotInvalidated();

                    unknown.set("UNKNOWN+1");

                    assertInvalidated();

                    property.set("Right");

                    assertNotInvalidated();
                }

                @Test
                void shouldNotResubscribeToMappedPropertyOnEachValidation() {
                    assertEquals(1, subscribeCount);
                    assertEquals(0, unsubscribeCount);

                    unknown.set("A");
                    observableValue.getValue();
                    unknown.set("B");
                    observableValue.getValue();
                    unknown.set("C");
                    observableValue.getValue();

                    assertEquals(1, subscribeCount);
                    assertEquals(0, unsubscribeCount);
                }

                @Test
                void shouldBeStronglyReferenced() {
                    ReferenceAsserts.testIfStronglyReferenced(observableValue, () -> observableValue = null);
                }

                @Test
                void shouldStronglyReferMappedProperty() {
                    ReferenceAsserts.testIfStronglyReferenced(unknown, () -> unknown = null);
                }

                @Test
                void shouldNotStronglyReferOldMappedProperty() {
                    property.set("Right");

                    ReferenceAsserts.testIfNotStronglyReferenced(unknown, () -> unknown = null);
                }

                @Nested
                class AndWhenUnobserved {
                    {
                        stopObservingInvalidations(observableValue);
                    }

                    @Test
                    void shouldNoLongerBeCalled() {
                        assertNotInvalidated();

                        property.set("Left");
                        property.set("Right");
                        property.set("Unknown");

                        assertNotInvalidated();
                    }

                    @Test
                    void shouldNoLongerBeStronglyReferenced() {
                        ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> observableValue = null);
                        ReferenceAsserts.testIfNotStronglyReferenced(unknown, () -> unknown = null);
                    }
                }
            }

            @Nested
            class WhenObservedForChanges {
                {
                    startObservingChanges(observableValue);
                }

                @Test
                void shouldApplyOperation() {
                    assertNothingIsObserved();

                    unknown.set("UNKNOWN+1");

                    assertObserved("UNKNOWN+1");  // as it initially is unknown, changing the unknown property results in a change

                    property.set("Right");

                    assertObserved("RIGHT");  // switching to right gives the value of the right property

                    unknown.set("UNKNOWN+2");
                    left.set("LEFT+1");

                    assertNothingIsObserved();  // changing left or unknown has no effect when currently observing right

                    right.set("RIGHT+1");

                    assertObserved("RIGHT+1");  // changing right value has an effect as right is observed

                    property.set("Left");

                    assertObserved("LEFT+1");  // switching to left sees latest left value
                }

                @Test
                void shouldNotResubscribeToMappedPropertyOnEachValidation() {
                    assertEquals(1, subscribeCount);
                    assertEquals(0, unsubscribeCount);

                    unknown.set("A");
                    unknown.set("B");
                    unknown.set("C");

                    assertEquals(1, subscribeCount);
                    assertEquals(0, unsubscribeCount);
                }

                @Test
                void shouldNotOperateOnNull() {
                    property.set(null);

                    assertObserved((String)null);  // flatMap operation is skipped (as it would NPE otherwise) and the resulting value is null
                }

                @Test
                void shouldIgnoreFlatMapsToNull() {
                    right = null;

                    property.set("Right");

                    assertObserved((String)null);  // flatMap maps to right property which is now null, this results in null
                }

                @Test
                void shouldObserveNullWhenFlatMappedPropertyIsSetToNull() {
                    property.set("Right");

                    assertObserved("RIGHT");

                    property.set(null);

                    assertObserved((String)null);
                }

                @Test
                void shouldBeStronglyReferenced() {
                    ReferenceAsserts.testIfStronglyReferenced(observableValue, () -> observableValue = null);
                }

                @Test
                void shouldStronglyReferMappedProperty() {
                    ReferenceAsserts.testIfStronglyReferenced(unknown, () -> unknown = null);
                }

                @Test
                void shouldNotStronglyReferOldMappedProperty() {
                    property.set("Right");

                    ReferenceAsserts.testIfNotStronglyReferenced(unknown, () -> unknown = null);
                }

                @Nested
                class AndWhenUnobserved {
                    {
                        stopObservingChanges(observableValue);
                    }

                    @Test
                    void shouldNoLongerBeCalled() {
                        assertNothingIsObserved();

                        property.set("Left");
                        property.set("Right");
                        property.set("Unknown");

                        assertNothingIsObserved();
                    }

                    @Test
                    void shouldNoLongerBeStronglyReferenced() {
                        ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> observableValue = null);
                        ReferenceAsserts.testIfNotStronglyReferenced(unknown, () -> unknown = null);
                    }
                }
            }

            @Nested
            class When_map_CalledReturns_ObservableValue_Which {
                {
                    observableValue = observableValue.map(v -> v + "+map");
                }

                @Test
                void shouldNotBeNull() {
                    assertNotNull(observableValue);
                }

                @Test
                void shouldNotBeStronglyReferenced() {
                    ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> observableValue = null);
                }

                @Nested
                class WhenObservedForChanges {
                    {
                        startObservingChanges(observableValue);
                    }

                    @Test
                    void shouldApplyFlatMapThenMapOperation() {
                        assertNothingIsObserved();

                        property.set("Left");

                        assertObserved("LEFT+map");

                        property.set("Right");

                        assertObserved("RIGHT+map");

                        left.set("LEFT-LEFT");  // should have no effect

                        assertNothingIsObserved();

                        right.set("RIGHT-RIGHT");

                        assertObserved("RIGHT-RIGHT+map");
                    }

                    @Test
                    void shouldBeStronglyReferenced() {
                        ReferenceAsserts.testIfStronglyReferenced(observableValue, () -> observableValue = null);
                    }

                    @Nested
                    class AndWhenUnobserved {
                        {
                            stopObservingChanges(observableValue);
                        }

                        @Test
                        void shouldNoLongerBeCalled() {
                            assertNothingIsObserved();

                            property.set("Left");

                            assertNothingIsObserved();
                        }

                        @Test
                        void shouldNoLongerBeStronglyReferenced() {
                            ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> observableValue = null);
                        }
                    }
                }
            }

            @Nested
            class When_orElse_CalledReturns_ObservableValue_Which {
                {
                    observableValue = observableValue.orElse("Empty");
                }

                @Test
                void shouldNotBeNull() {
                    assertNotNull(observableValue);
                }

                @Test
                void shouldNotBeStronglyReferenced() {
                    ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> observableValue = null);
                }

                @Nested
                class WhenObservedForChanges {
                    {
                        startObservingChanges(observableValue);
                    }

                    @Test
                    void shouldApplyFlatMapThenMapOperation() {
                        assertNothingIsObserved();

                        property.set("Left");

                        assertObserved("LEFT");

                        property.set("Right");

                        assertObserved("RIGHT");

                        left.set("LEFT-LEFT");  // should have no effect as right branch is observed

                        assertNothingIsObserved();

                        right.set("RIGHT-RIGHT");

                        assertObserved("RIGHT-RIGHT");

                        right.set(null);

                        assertObserved("Empty");

                        property.set("Left");

                        assertObserved("LEFT-LEFT");

                        property.set(null);

                        assertObserved("Empty");
                    }

                    @Test
                    void shouldBeStronglyReferenced() {
                        ReferenceAsserts.testIfStronglyReferenced(observableValue, () -> observableValue = null);
                    }

                    @Nested
                    class AndWhenUnobserved {
                        {
                            stopObservingChanges(observableValue);
                        }

                        @Test
                        void shouldNoLongerBeCalled() {
                            assertNothingIsObserved();

                            property.set("Left");

                            assertNothingIsObserved();
                        }

                        @Test
                        void shouldNoLongerBeStronglyReferenced() {
                            ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> observableValue = null);
                        }
                    }
                }
            }
        }
    }

    @Nested
    class When_when_Called {

        @Nested
        class WithNull {

            @Test
            void shouldThrowNullPointerException() {
                assertThrows(NullPointerException.class, () -> property.when(null));
            }
        }

        @Nested
        class WithNotNullAndInitiallyFalseConditionReturns_ObservableValue_Which {
            private BooleanProperty condition = new SimpleBooleanProperty(false);
            private ObservableValue<String> observableValue = property.when(condition);

            @Test
            void shouldNotBeNull() {
                assertNotNull(observableValue);
            }

            @Test
            void shouldNotBeStronglyReferenced() {
                ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> {
                    observableValue = null;
                    condition = null;
                });
            }

            @Nested
            class When_getValue_Called {

                @Test
                void shouldReturnInitialValueAtTimeOfCreation() {
                    property.set("Not Initial");

                    assertEquals("Initial", observableValue.getValue());
                }
            }
        }

        @Nested
        class WithNotNullReturns_ObservableValue_Which {
            // using object property here so it can be set to null for testing
            private ObjectProperty<Boolean> condition = new SimpleObjectProperty<>(true);
            private ObservableValue<String> observableValue = property.when(condition);

            @Test
            void shouldNotBeNull() {
                assertNotNull(observableValue);
            }

            @Test
            void shouldNotBeStronglyReferenced() {
                ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> {
                    observableValue = null;
                    condition = null;
                });
            }

            @Nested
            class When_getValue_Called {

                @Test
                void shouldReturnCurrentPropertyValuesWhileConditionIsTrue() {
                    assertEquals("Initial", observableValue.getValue());

                    property.set(null);

                    assertNull(observableValue.getValue());

                    property.set("Left");

                    assertEquals("Left", observableValue.getValue());

                    condition.set(false);

                    property.set("Right");

                    assertEquals("Left", observableValue.getValue());

                    property.set("Middle");

                    assertEquals("Left", observableValue.getValue());

                    condition.set(true);

                    assertEquals("Middle", observableValue.getValue());
                }
            }

            @Nested
            class WhenObservedForInvalidations {
                {
                    startObservingInvalidations(observableValue);
                }

                @Test
                void shouldOnlyInvalidateOnce() {
                    assertNotInvalidated();

                    property.set("Left");

                    assertInvalidated();

                    property.set("Right");

                    assertNotInvalidated();
                }

                @Test
                void shouldOnlyInvalidateWhileConditionIsTrue() {
                    assertNotInvalidated();

                    property.set("Left");  // trigger invalidation

                    assertInvalidated();

                    condition.set(false);

                    assertNotInvalidated();  // already invalid, changing condition won't change that

                    observableValue.getValue();  // this would normally make the property valid, but not when condition is false

                    property.set("Right");  // trigger invalidation

                    assertNotInvalidated();  // nothing happened

                    condition.setValue(null);  // null is false as well, should not change result

                    assertNotInvalidated();  // nothing happened

                    condition.set(true);

                    assertInvalidated();

                    observableValue.getValue();  // make property valid

                    assertNotInvalidated();

                    property.set("Middle");  // trigger invalidation

                    assertInvalidated();
                }

                @Test
                void shouldBeStronglyReferenced() {
                    ReferenceAsserts.testIfStronglyReferenced(observableValue, () -> {
                        observableValue = null;
                        condition = null;
                    });
                }

                @Test
                void shouldNotBeStronglyReferencedWhenConditionIsFalse() {
                    condition.set(false);

                    ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> {
                        observableValue = null;
                        condition = null;
                    });
                }

                @Nested
                class AndWhenUnobserved {
                    {
                        stopObservingInvalidations(observableValue);
                    }

                    @Test
                    void shouldNoLongerBeCalled() {
                        assertNotInvalidated();

                        property.set("Left");
                        property.set("Right");

                        assertNotInvalidated();
                    }

                    @Test
                    void shouldNoLongerBeStronglyReferenced() {
                        ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> {
                            observableValue = null;
                            condition = null;
                        });
                    }
                }
            }

            @Nested
            class WhenObservedForChanges {
                {
                    startObservingChanges(observableValue);
                }

                @Test
                void shouldReceiveCurrentPropertyValues() {
                    assertNothingIsObserved();

                    property.set("Right");

                    assertObserved("Right");
                }

                @Test
                void shouldOnlyReceiveCurrentPropertyValuesWhileConditionIsTrue() {
                    assertNothingIsObserved();

                    property.set("Right");

                    assertObserved("Right");

                    condition.set(false);

                    assertNothingIsObserved();

                    property.set("Left");

                    assertNothingIsObserved();

                    property.set("Middle");

                    assertNothingIsObserved();

                    condition.setValue(null);  // null is false as well, should not change result

                    assertNothingIsObserved();

                    condition.set(true);

                    assertObserved("Middle");
                }

                @Test
                void shouldBeStronglyReferenced() {
                    ReferenceAsserts.testIfStronglyReferenced(observableValue, () -> {
                        observableValue = null;
                        condition = null;
                    });
                }

                @Test
                void shouldNotBeStronglyReferencedWhenConditionIsFalse() {
                    condition.set(false);

                    ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> {
                        observableValue = null;
                        condition = null;
                    });
                }

                @Nested
                class AndWhenUnobserved {
                    {
                        stopObservingChanges(observableValue);
                    }

                    @Test
                    void shouldNoLongerBeCalled() {
                        assertNothingIsObserved();

                        property.set("Right");

                        assertNothingIsObserved();
                    }

                    @Test
                    void shouldNoLongerBeStronglyReferenced() {
                        ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> {
                            observableValue = null;
                            condition = null;
                        });
                    }
                }
            }
        }
    }

    /**
     * Ensures nothing has been observed since the last check.
     */
    private void assertNothingIsObserved() {
        assertObserved();
    }

    /**
     * Ensures that given values have been observed since last call.
     *
     * @param expectedValues an array of expected values
     */
    private void assertObserved(String... expectedValues) {
        assertEquals(Arrays.asList(expectedValues), values);
        values.clear();
    }

    /**
     * Starts observing the given observable value for changes. This will do
     * a sanity check that the observer is currently not working before adding it.
     *
     * @param observableValue an {@code ObservableValue}, cannot be {@code null}
     */
    private void startObservingChanges(ObservableValue<String> observableValue) {
        values.clear();

        property.setValue("Left");
        property.setValue("Right");
        property.setValue("Initial");

        assertTrue(values.isEmpty());

        observableValue.addListener(changeListener);
    }

    /**
     * Stops observing the given observable value for changes. This will do a
     * sanity check that the observer is currently working before removing it.
     *
     * @param observableValue an {@code ObservableValue}, cannot be {@code null}
     */
    private void stopObservingChanges(ObservableValue<String> observableValue) {
        values.clear();

        property.setValue("Left");
        property.setValue("Right");

        assertEquals(2, values.size());

        values.clear();

        observableValue.removeListener(changeListener);
    }

    /**
     * Ensures no invalidations occurred since the last check.
     */
    private void assertNotInvalidated() {
        assertEquals(0, invalidations);
    }

    /**
     * Ensures that an invalidation occurred since last check.
     */
    private void assertInvalidated() {
        assertEquals(1, invalidations);
        invalidations = 0;
    }

    /**
     * Starts observing the given observable value for invalidations. This will do
     * a sanity check that the observer is currently not working before adding it.
     *
     * @param observableValue an {@code ObservableValue}, cannot be {@code null}
     */
    private void startObservingInvalidations(ObservableValue<String> observableValue) {
        invalidations = 0;

        property.getValue();
        property.setValue("Left");
        property.setValue("Right");
        property.setValue("Initial");

        assertNotInvalidated();

        observableValue.addListener(invalidationListener);
    }

    /**
     * Stops observing the given observable value for invalidations. This will do a
     * sanity check that the observer is currently working before removing it.
     *
     * @param observableValue an {@code ObservableValue}, cannot be {@code null}
     */
    private void stopObservingInvalidations(ObservableValue<String> observableValue) {
        invalidations = 0;

        property.getValue();
        property.setValue("Left");
        property.setValue("Right");
        property.setValue("Initial");

        assertInvalidated();

        observableValue.removeListener(invalidationListener);
    }
}
