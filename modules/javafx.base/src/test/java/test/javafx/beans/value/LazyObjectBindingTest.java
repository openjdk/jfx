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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.sun.javafx.binding.LazyObjectBindingStub;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;

public class LazyObjectBindingTest {

    private LazyObjectBindingStub<String> binding = new LazyObjectBindingStub<>();

    void resetCounters() {
        binding.startObservingCalls = 0;
        binding.computeValueCalls = 0;
        binding.stopObservingCalls = 0;
    }

    @Test
    void shouldBeInvalidInitially() {
        assertFalse(binding.isValid());
    }

    @Test
    void invalidationWhichBecomesValidDuringCallbacksShouldReturnCorrectValue() {
        LazyObjectBindingStub<String> binding = new LazyObjectBindingStub<>() {
            @Override
            protected String computeValue() {
                return "A";
            }
        };

        binding.addListener(obs -> {
            assertEquals("A", binding.get());
        });

        binding.invalidate();  // becomes valid again immediately

        assertEquals("A", binding.get());
    }

    @Nested
    class WhenObservedWithInvalidationListener {
        private InvalidationListener invalidationListener = obs -> {};

        {
            binding.addListener(invalidationListener);
        }

        @Test
        void shouldBeValid() {
            assertTrue(binding.isValid());
        }

        @Test
        void shouldStartObservingSource() {
            assertEquals(1, binding.startObservingCalls);
        }

        @Test
        void shouldNotStopObservingSource() {
            assertEquals(0, binding.stopObservingCalls);
        }

        @Test
        void shouldCallComputeValueOneOrTwoTimes() {

            /*
             * The binding is made valid twice currently, once when
             * the listener is registered, and again after the observing of
             * inputs starts. The first time the binding does not become
             * valid because it is not yet considered "observed" as the
             * computeValue call occurs in the middle of the listener
             * registration process.
             *
             * See also the explanation in LazyObjectBinding#updateSubcriptionAfterAdd
             */

            assertTrue(binding.computeValueCalls >= 1 && binding.computeValueCalls <= 2);
        }

        @Nested
        class AndWhenObservedAgain {
            private ChangeListener<String> changeListener = (obs, old, current) -> {};

            {
                resetCounters();
                binding.addListener(changeListener);
            }

            @Test
            void shouldStillBeValid() {
                assertTrue(binding.isValid());
            }

            @Test
            void shouldNotStartObservingSourceAgain() {
                assertEquals(0, binding.startObservingCalls);
            }

            @Test
            void shouldNotStopObservingSource() {
                assertEquals(0, binding.stopObservingCalls);
            }

            @Test
            void shouldNotComputeValueAgain() {
                assertEquals(0, binding.computeValueCalls);
            }

            @Nested
            class AndThenOneObserverIsRemoved {
                {
                    resetCounters();
                    binding.removeListener(changeListener);
                }

                @Test
                void shouldStillBeValid() {
                    assertTrue(binding.isValid());
                }

                @Test
                void shouldNotStartObservingSourceAgain() {
                    assertEquals(0, binding.startObservingCalls);
                }

                @Test
                void shouldNotStopObservingSource() {
                    assertEquals(0, binding.stopObservingCalls);
                }

                @Test
                void shouldNotComputeValueAgain() {
                    assertEquals(0, binding.computeValueCalls);
                }

                @Nested
                class AndThenTheLastObserverIsRemoved {
                    {
                        resetCounters();
                        binding.removeListener(invalidationListener);
                    }

                    @Test
                    void shouldNotStartObservingSource() {
                        assertEquals(0, binding.startObservingCalls);
                    }

                    @Test
                    void shouldStopObservingSource() {
                        assertEquals(1, binding.stopObservingCalls);
                    }

                    @Test
                    void shouldNotComputeValue() {
                        assertEquals(0, binding.computeValueCalls);
                    }

                    @Test
                    void shouldNoLongerBeValid() {
                        assertFalse(binding.isValid());
                    }

                    @Nested
                    class AndTheListenerIsRemovedAgain {
                        {
                            resetCounters();
                            binding.removeListener(invalidationListener);
                        }

                        @Test
                        void shouldNotStartObservingSource() {
                            assertEquals(0, binding.startObservingCalls);
                        }

                        @Test
                        void shouldNotStopObservingSource() {
                            assertEquals(0, binding.stopObservingCalls);
                        }

                        @Test
                        void shouldNotComputeValue() {
                            assertEquals(0, binding.computeValueCalls);
                        }

                        @Test
                        void shouldNotBeValid() {
                            assertFalse(binding.isValid());
                        }
                    }
                }
            }
        }
    }
}
