package test.javafx.beans.value;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ObservableValueWhenTest {

    @Nested
    class WhenNotObserved {

        @Nested
        class AndConditionStartsFalse {
            BooleanProperty condition = new SimpleBooleanProperty(false);

            @Test
            void shouldNeverCallDownstreamMapFunction() {
                StringProperty property = new SimpleStringProperty("a");
                List<String> observedMappings = new ArrayList<>();

                property.when(condition).map(observedMappings::add);

                assertEquals(List.of(), observedMappings);

                property.set("b");

                assertEquals(List.of(), observedMappings);

                condition.set(true);

                assertEquals(List.of(), observedMappings);

                property.set("c");

                assertEquals(List.of(), observedMappings);

                condition.set(false);

                assertEquals(List.of(), observedMappings);

                property.set("d");

                assertEquals(List.of(), observedMappings);

                condition.set(true);

                assertEquals(List.of(), observedMappings);
            }
        }

        @Nested
        class AndConditionStartsTrue {
            BooleanProperty condition = new SimpleBooleanProperty(true);

            @Test
            void shouldNeverCallDownstreamMapFunction() {
                StringProperty property = new SimpleStringProperty("a");
                List<String> observedMappings = new ArrayList<>();

                property.when(condition).map(observedMappings::add);

                assertEquals(List.of(), observedMappings);

                property.set("b");

                assertEquals(List.of(), observedMappings);

                condition.set(false);

                assertEquals(List.of(), observedMappings);

                property.set("c");

                assertEquals(List.of(), observedMappings);

                condition.set(true);

                assertEquals(List.of(), observedMappings);

                property.set("d");

                assertEquals(List.of(), observedMappings);

                condition.set(false);

                assertEquals(List.of(), observedMappings);
            }
        }
    }

    @Nested
    class WhenObserved {
        @Nested
        class AndConditionStartsFalse {
            BooleanProperty condition = new SimpleBooleanProperty(false);

            @Test
            void shouldCallDownstreamMapFunctionOnlyWhenAbsolutelyNecessary() {
                StringProperty property = new SimpleStringProperty("a");
                List<String> observedMappings = new ArrayList<>();
                List<String> observedChanges = new ArrayList<>();

                property.when(condition)
                    .map(x -> { observedMappings.add(x); return x; })
                    .addListener((obs, old, current) -> observedChanges.add(old + " -> " + current));

                assertEquals(List.of("a"), observedMappings);
                assertEquals(List.of(), observedChanges);

                property.set("b");

                assertEquals(List.of("a"), observedMappings);
                assertEquals(List.of(), observedChanges);

                condition.set(true);

                assertEquals(List.of("a", "b"), observedMappings);
                assertEquals(List.of("a -> b"), observedChanges);

                property.set("c");

                assertEquals(List.of("a", "b", "c"), observedMappings);
                assertEquals(List.of("a -> b", "b -> c"), observedChanges);

                condition.set(false);

                assertEquals(List.of("a", "b", "c"), observedMappings);
                assertEquals(List.of("a -> b", "b -> c"), observedChanges);

                property.set("d");

                assertEquals(List.of("a", "b", "c"), observedMappings);
                assertEquals(List.of("a -> b", "b -> c"), observedChanges);

                condition.set(true);

                assertEquals(List.of("a", "b", "c", "d"), observedMappings);
                assertEquals(List.of("a -> b", "b -> c", "c -> d"), observedChanges);
            }
        }

        @Nested
        class AndConditionStartsTrue {
            BooleanProperty condition = new SimpleBooleanProperty(true);

            @Test
            void shouldCallDownstreamMapFunctionOnlyWhenAbsolutelyNecessary() {
                StringProperty property = new SimpleStringProperty("a");
                List<String> observedMappings = new ArrayList<>();
                List<String> observedChanges = new ArrayList<>();

                property.when(condition)
                    .map(x -> { observedMappings.add(x); return x; })
                    .addListener((obs, old, current) -> observedChanges.add(old + " -> " + current));

                assertEquals(List.of("a"), observedMappings);
                assertEquals(List.of(), observedChanges);

                property.set("b");

                assertEquals(List.of("a", "b"), observedMappings);
                assertEquals(List.of("a -> b"), observedChanges);

                condition.set(false);

                assertEquals(List.of("a", "b"), observedMappings);
                assertEquals(List.of("a -> b"), observedChanges);

                property.set("c");

                assertEquals(List.of("a", "b"), observedMappings);
                assertEquals(List.of("a -> b"), observedChanges);

                condition.set(true);

                assertEquals(List.of("a", "b", "c"), observedMappings);
                assertEquals(List.of("a -> b", "b -> c"), observedChanges);

                property.set("d");

                assertEquals(List.of("a", "b", "c", "d"), observedMappings);
                assertEquals(List.of("a -> b", "b -> c", "c -> d"), observedChanges);

                condition.set(false);

                assertEquals(List.of("a", "b", "c", "d"), observedMappings);
                assertEquals(List.of("a -> b", "b -> c", "c -> d"), observedChanges);
            }
        }
    }
}
