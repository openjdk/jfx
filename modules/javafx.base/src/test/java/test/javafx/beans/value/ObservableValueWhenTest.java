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
