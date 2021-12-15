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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

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
}
