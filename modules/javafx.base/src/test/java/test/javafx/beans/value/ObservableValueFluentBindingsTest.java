package test.javafx.beans.value;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/*
 * This is a JUnit 5 style test which has been backported to JUnit 4.
 * Once JUnit 5 is available, the declared annotations and all
 * JUnit 4 tests (marked with @org.junit.Test) should be removed.
 *
 * The used static imports for assertions can be upgraded as well.
 */

public class ObservableValueFluentBindingsTest {
    private StringProperty property = new SimpleStringProperty("A");

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
            private ObservableValue<String> observableValue = property.map(v -> v + "Z");

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
                    assertEquals("AZ", observableValue.getValue());

                    property.set("B");

                    assertEquals("BZ", observableValue.getValue());
                }

                @Test
                void shouldNotOperateOnNull() {
                    property.set(null);

                    assertNull(observableValue.getValue());
                }
            }

            @Nested
            class WhenObserved {
                private List<String> values = new ArrayList<>();
                private ChangeListener<String> changeListener = (obs, old, current) -> values.add(current);

                {
                    observableValue.addListener(changeListener);
                }

                @Test
                void shouldApplyOperation() {
                    assertTrue(values.isEmpty());

                    property.set("C");

                    assertEquals(List.of("CZ"), values);
                }

                @Test
                void shouldNotOperateOnNull() {
                    property.set(null);

                    assertEquals(Arrays.asList((String) null), values);
                }

                @Test
                void shouldBeStronglyReferenced() {
                    ReferenceAsserts.testIfStronglyReferenced(observableValue, () -> observableValue = null);
                }

                @Nested
                class AndWhenUnobserved {
                    {
                        property.setValue("B");
                        property.setValue("A");

                        assertEquals(List.of("BZ", "AZ"), values);

                        values.clear();

                        observableValue.removeListener(changeListener);
                    }

                    @Test
                    void shouldNoLongerBeCalled() {
                        assertTrue(values.isEmpty());

                        property.set("C");

                        assertTrue(values.isEmpty());
                    }

                    @Test
                    void shouldNoLongerBeStronglyReferenced() {
                        ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> observableValue = null);
                    }
                }
            }
        }
    }

    @Nested
    class When_orElse_CalledReturns_ObservableValue_Which {
        private ObservableValue<String> observableValue = property.orElse("null");

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
                assertEquals("A", observableValue.getValue());

                property.set(null);

                assertEquals("null", observableValue.getValue());
            }
        }

        @Nested
        class WhenObserved {
            private List<String> values = new ArrayList<>();
            private ChangeListener<String> changeListener = (obs, old, current) -> values.add(current);

            {
                observableValue.addListener(changeListener);
            }

            @Test
            void shouldApplyOperation() {
                assertTrue(values.isEmpty());

                property.set("C");

                assertEquals(List.of("C"), values);

                values.clear();
                property.set(null);

                assertEquals(List.of("null"), values);
            }

            @Test
            void shouldBeStronglyReferenced() {
                ReferenceAsserts.testIfStronglyReferenced(observableValue, () -> observableValue = null);
            }

            @Nested
            class AndWhenUnobserved {
                {
                    property.setValue("B");
                    property.setValue(null);

                    assertEquals(List.of("B", "null"), values);

                    values.clear();

                    observableValue.removeListener(changeListener);
                }

                @Test
                void shouldNoLongerBeCalled() {
                    assertTrue(values.isEmpty());

                    property.set("C");

                    assertTrue(values.isEmpty());
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
        // TODO test for when something is flatMapped to null in getValue call

        @Nested
        class WithNotNullReturns_ObservableValue_Which {
            private ObjectProperty<Integer> altA = new SimpleObjectProperty<>(65);
            private ObjectProperty<Integer> altOther = new SimpleObjectProperty<>(0);
            private ObservableValue<Integer> observableValue = property.flatMap(v -> "A".equals(v) ? altA : altOther);

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
                    assertEquals((Integer) 65, observableValue.getValue());

                    property.set("D");

                    assertEquals((Integer) 0, observableValue.getValue());

                    altOther.setValue(1);

                    assertEquals((Integer) 1, observableValue.getValue());

                    altA.setValue(66);

                    assertEquals((Integer) 1, observableValue.getValue());

                    property.set("A");

                    assertEquals((Integer) 66, observableValue.getValue());
                }

                @Test
                void shouldNotOperateOnNull() {
                    property.set(null);

                    assertNull(observableValue.getValue());
                }

                @Test
                void shouldIgnoreFlatMapsToNull() {
                    altA = null;

                    assertNull(observableValue.getValue());
                }
            }

            @Nested
            class WhenObserved {
                private List<Integer> values = new ArrayList<>();
                private ChangeListener<Integer> changeListener = (obs, old, current) -> values.add(current);

                {
                    observableValue.addListener(changeListener);
                }

                @Test
                void shouldApplyOperation() {
                    assertTrue(values.isEmpty());

                    altA.set(66);

                    assertEquals(List.of(66), values);

                    values.clear();
                    property.set("D");

                    assertEquals(List.of(0), values);

                    values.clear();
                    altA.set(67);

                    assertEquals(List.of(), values);

                    values.clear();
                    altOther.set(1);

                    assertEquals(List.of(1), values);

                    values.clear();
                    property.set("A");

                    assertEquals(List.of(67), values);
                }

                @Test
                void shouldNotOperateOnNull() {
                    property.set(null);

                    assertEquals(Arrays.asList((String) null), values);
                }

                @Test
                void shouldIgnoreFlatMapsToNull() {
                    altOther = null;

                    property.set("D");

                    assertEquals(Arrays.asList((String) null), values);
                }

                @Test
                void shouldBeStronglyReferenced() {
                    ReferenceAsserts.testIfStronglyReferenced(observableValue, () -> observableValue = null);
                }

                @Nested
                class AndWhenUnobserved {
                    {
                        property.setValue("B");
                        property.setValue("A");

                        assertEquals(List.of(0, 65), values);

                        values.clear();

                        observableValue.removeListener(changeListener);
                    }

                    @Test
                    void shouldNoLongerBeCalled() {
                        assertTrue(values.isEmpty());

                        property.set("C");

                        assertTrue(values.isEmpty());
                    }

                    @Test
                    void shouldNoLongerBeStronglyReferenced() {
                        ReferenceAsserts.testIfNotStronglyReferenced(observableValue, () -> observableValue = null);
                    }
                }
            }
        }
    }

    /*
     * Backported code for JUnit 4 which can be removed starts here:
     */

    @interface Nested {}

    @interface Test {}

    public static <T extends Throwable> T assertThrows(Class<T> expected, Runnable runnable) {
        try {
            runnable.run();
        }
        catch (Throwable t) {
            if (expected.isInstance(t)) {
                return (T) t;
            }

            throw new AssertionError("unexpected exception thrown: " + t);
        }

        throw new AssertionError("expected " + expected + ", but nothing was thrown");
    }

    @org.junit.Test
    public void When_map_Called__WithNull__shouldThrowNullPointerException() {
        new When_map_Called().new WithNull().shouldThrowNullPointerException();
    }

    @org.junit.Test
    public void When_map_Called__WithNotNullReturns_ObservableValue_Which__shouldNotBeNull() {
        new When_map_Called().new WithNotNullReturns_ObservableValue_Which().shouldNotBeNull();
    }

    @org.junit.Test
    public void When_map_Called__WithNotNullReturns_ObservableValue_Which__shouldNotBeStronglyReferenced() {
        new When_map_Called().new WithNotNullReturns_ObservableValue_Which().shouldNotBeStronglyReferenced();
    }

    @org.junit.Test
    public void When_map_Called__WithNotNullReturns_ObservableValue_Which__When_getValue_Called__shouldReturnPropertyValuesWithOperationApplied() {
        new When_map_Called().new WithNotNullReturns_ObservableValue_Which().new When_getValue_Called()
                .shouldReturnPropertyValuesWithOperationApplied();
    }

    @org.junit.Test
    public void When_map_Called__WithNotNullReturns_ObservableValue_Which__When_getValue_Called__shouldNotOperateOnNull() {
        new When_map_Called().new WithNotNullReturns_ObservableValue_Which().new When_getValue_Called()
                .shouldNotOperateOnNull();
    }

    @org.junit.Test
    public void When_map_Called__WithNotNullReturns_ObservableValue_Which__WhenObserved__shouldApplyOperation() {
        new When_map_Called().new WithNotNullReturns_ObservableValue_Which().new WhenObserved().shouldApplyOperation();
    }

    @org.junit.Test
    public void When_map_Called__WithNotNullReturns_ObservableValue_Which__WhenObserved__shouldNotOperateOnNull() {
        new When_map_Called().new WithNotNullReturns_ObservableValue_Which().new WhenObserved()
                .shouldNotOperateOnNull();
    }

    @org.junit.Test
    public void When_map_Called__WithNotNullReturns_ObservableValue_Which__WhenObserved__shouldBeStronglyReferenced() {
        new When_map_Called().new WithNotNullReturns_ObservableValue_Which().new WhenObserved()
                .shouldBeStronglyReferenced();
    }

    @org.junit.Test
    public void When_map_Called__WithNotNullReturns_ObservableValue_Which__WhenObserved__AndWhenUnobserved__shouldNoLongerBeCalled() {
        new When_map_Called().new WithNotNullReturns_ObservableValue_Which().new WhenObserved().new AndWhenUnobserved()
                .shouldNoLongerBeCalled();
    }

    @org.junit.Test
    public void When_map_Called__WithNotNullReturns_ObservableValue_Which__WhenObserved__AndWhenUnobserved__shouldNoLongerBeStronglyReferenced() {
        new When_map_Called().new WithNotNullReturns_ObservableValue_Which().new WhenObserved().new AndWhenUnobserved()
                .shouldNoLongerBeStronglyReferenced();
    }

    @org.junit.Test
    public void When_orElse_CalledReturns_ObservableValue_Which__shouldNotBeNull() {
        new When_orElse_CalledReturns_ObservableValue_Which().shouldNotBeNull();
    }

    @org.junit.Test
    public void When_orElse_CalledReturns_ObservableValue_Which__shouldNotBeStronglyReferenced() {
        new When_orElse_CalledReturns_ObservableValue_Which().shouldNotBeStronglyReferenced();
    }

    @org.junit.Test
    public void When_orElse_CalledReturns_ObservableValue_Which__When_getValue_Called__shouldReturnPropertyValuesWithOperationApplied() {
        new When_orElse_CalledReturns_ObservableValue_Which().new When_getValue_Called()
                .shouldReturnPropertyValuesWithOperationApplied();
    }

    @org.junit.Test
    public void When_orElse_CalledReturns_ObservableValue_Which__WhenObserved__shouldApplyOperation() {
        new When_orElse_CalledReturns_ObservableValue_Which().new WhenObserved().shouldApplyOperation();
    }

    @org.junit.Test
    public void When_orElse_CalledReturns_ObservableValue_Which__WhenObserved__shouldBeStronglyReferenced() {
        new When_orElse_CalledReturns_ObservableValue_Which().new WhenObserved().shouldBeStronglyReferenced();
    }

    @org.junit.Test
    public void When_orElse_CalledReturns_ObservableValue_Which__WhenObserved__AndWhenUnobserved__shouldNoLongerBeCalled() {
        new When_orElse_CalledReturns_ObservableValue_Which().new WhenObserved().new AndWhenUnobserved()
                .shouldNoLongerBeCalled();
    }

    @org.junit.Test
    public void When_orElse_CalledReturns_ObservableValue_Which__WhenObserved__AndWhenUnobserved__shouldNoLongerBeStronglyReferenced() {
        new When_orElse_CalledReturns_ObservableValue_Which().new WhenObserved().new AndWhenUnobserved()
                .shouldNoLongerBeStronglyReferenced();
    }

    @org.junit.Test
    public void When_flatMap_Called__WithNull__shouldThrowNullPointerException() {
        new When_flatMap_Called().new WithNull().shouldThrowNullPointerException();
    }

    @org.junit.Test
    public void When_flatMap_Called__WithNotNullReturns_ObservableValue_Which__shouldNotBeNull() {
        new When_flatMap_Called().new WithNotNullReturns_ObservableValue_Which().shouldNotBeNull();
    }

    @org.junit.Test
    public void When_flatMap_Called__WithNotNullReturns_ObservableValue_Which__shouldNotBeStronglyReferenced() {
        new When_flatMap_Called().new WithNotNullReturns_ObservableValue_Which().shouldNotBeStronglyReferenced();
    }

    @org.junit.Test
    public void When_flatMap_Called__WithNotNullReturns_ObservableValue_Which__When_getValue_Called__shouldReturnPropertyValuesWithOperationApplied() {
        new When_flatMap_Called().new WithNotNullReturns_ObservableValue_Which().new When_getValue_Called()
                .shouldReturnPropertyValuesWithOperationApplied();
    }

    @org.junit.Test
    public void When_flatMap_Called__WithNotNullReturns_ObservableValue_Which__When_getValue_Called__shouldNotOperateOnNull() {
        new When_flatMap_Called().new WithNotNullReturns_ObservableValue_Which().new When_getValue_Called()
                .shouldNotOperateOnNull();
    }

    @org.junit.Test
    public void When_flatMap_Called__WithNotNullReturns_ObservableValue_Which__When_getValue_Called__shouldIgnoreFlatMapsToNull() {
        new When_flatMap_Called().new WithNotNullReturns_ObservableValue_Which().new When_getValue_Called()
                .shouldIgnoreFlatMapsToNull();
    }

    @org.junit.Test
    public void When_flatMap_Called__WithNotNullReturns_ObservableValue_Which__WhenObserved__shouldApplyOperation() {
        new When_flatMap_Called().new WithNotNullReturns_ObservableValue_Which().new WhenObserved()
                .shouldApplyOperation();
    }

    @org.junit.Test
    public void When_flatMap_Called__WithNotNullReturns_ObservableValue_Which__WhenObserved__shouldNotOperateOnNull() {
        new When_flatMap_Called().new WithNotNullReturns_ObservableValue_Which().new WhenObserved()
                .shouldNotOperateOnNull();
    }

    @org.junit.Test
    public void When_flatMap_Called__WithNotNullReturns_ObservableValue_Which__WhenObserved__shouldIgnoreFlatMapsToNull() {
        new When_flatMap_Called().new WithNotNullReturns_ObservableValue_Which().new WhenObserved()
                .shouldIgnoreFlatMapsToNull();
    }

    @org.junit.Test
    public void When_flatMap_Called__WithNotNullReturns_ObservableValue_Which__WhenObserved__shouldBeStronglyReferenced() {
        new When_flatMap_Called().new WithNotNullReturns_ObservableValue_Which().new WhenObserved()
                .shouldBeStronglyReferenced();
    }

    @org.junit.Test
    public void When_flatMap_Called__WithNotNullReturns_ObservableValue_Which__WhenObserved__AndWhenUnobserved__shouldNoLongerBeCalled() {
        new When_flatMap_Called().new WithNotNullReturns_ObservableValue_Which().new WhenObserved().new AndWhenUnobserved()
                .shouldNoLongerBeCalled();
    }

    @org.junit.Test
    public void When_flatMap_Called__WithNotNullReturns_ObservableValue_Which__WhenObserved__AndWhenUnobserved__shouldNoLongerBeStronglyReferenced() {
        new When_flatMap_Called().new WithNotNullReturns_ObservableValue_Which().new WhenObserved().new AndWhenUnobserved()
                .shouldNoLongerBeStronglyReferenced();
    }
}
