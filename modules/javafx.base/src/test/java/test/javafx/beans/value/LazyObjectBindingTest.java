/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.LazyObjectBindingStub;

/*
 * This is a JUnit 5 style test which has been backported to JUnit 4.
 * Once JUnit 5 is available, the declared annotations and all
 * JUnit 4 tests (marked with @org.junit.Test) should be removed.
 *
 * The used static imports for assertions can be upgraded as well.
 */

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

    /*
     * Backported code for JUnit 4 which can be removed starts here:
     */

    @interface Nested {}
    @interface Test {}

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__shouldBeValid() {
        new WhenObservedWithInvalidationListener()
            .shouldBeValid();
    }

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__shouldStartObservingSource() {
        new WhenObservedWithInvalidationListener()
            .shouldStartObservingSource();
    }

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__shouldNotStopObservingSource() {
        new WhenObservedWithInvalidationListener()
            .shouldNotStopObservingSource();
    }

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__shouldCallComputeValueOneOrTwoTimes() {
        new WhenObservedWithInvalidationListener()
            .shouldCallComputeValueOneOrTwoTimes();
    }

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__AndWhenObservedAgain__shouldStillBeValid() {
        new WhenObservedWithInvalidationListener()
            .new AndWhenObservedAgain()
            .shouldStillBeValid();
    }

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__AndWhenObservedAgain__shouldNotStartObservingSourceAgain() {
        new WhenObservedWithInvalidationListener()
            .new AndWhenObservedAgain()
            .shouldNotStartObservingSourceAgain();
    }

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__AndWhenObservedAgain__shouldNotStopObservingSource() {
        new WhenObservedWithInvalidationListener()
            .new AndWhenObservedAgain()
            .shouldNotStopObservingSource();
    }

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__AndWhenObservedAgain__shouldNotComputeValueAgain() {
        new WhenObservedWithInvalidationListener()
            .new AndWhenObservedAgain()
            .shouldNotComputeValueAgain();
    }

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__AndWhenObservedAgain__AndThenOneObserverIsRemoved__shouldStillBeValid() {
        new WhenObservedWithInvalidationListener()
            .new AndWhenObservedAgain()
            .new AndThenOneObserverIsRemoved()
            .shouldStillBeValid();
    }

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__AndWhenObservedAgain__AndThenOneObserverIsRemoved__shouldNotStartObservingSourceAgain() {
        new WhenObservedWithInvalidationListener()
            .new AndWhenObservedAgain()
            .new AndThenOneObserverIsRemoved()
            .shouldNotStartObservingSourceAgain();
    }

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__AndWhenObservedAgain__AndThenOneObserverIsRemoved__shouldNotStopObservingSource() {
        new WhenObservedWithInvalidationListener()
            .new AndWhenObservedAgain()
            .new AndThenOneObserverIsRemoved()
            .shouldNotStopObservingSource();
    }

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__AndWhenObservedAgain__AndThenOneObserverIsRemoved__shouldNotComputeValueAgain() {
        new WhenObservedWithInvalidationListener()
            .new AndWhenObservedAgain()
            .new AndThenOneObserverIsRemoved()
            .shouldNotComputeValueAgain();
    }

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__AndWhenObservedAgain__AndThenOneObserverIsRemoved__AndThenTheLastObserverIsRemoved__shouldNotStartObservingSource() {
        new WhenObservedWithInvalidationListener()
            .new AndWhenObservedAgain()
            .new AndThenOneObserverIsRemoved()
            .new AndThenTheLastObserverIsRemoved()
            .shouldNotStartObservingSource();
    }

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__AndWhenObservedAgain__AndThenOneObserverIsRemoved__AndThenTheLastObserverIsRemoved__shouldStopObservingSource() {
        new WhenObservedWithInvalidationListener()
            .new AndWhenObservedAgain()
            .new AndThenOneObserverIsRemoved()
            .new AndThenTheLastObserverIsRemoved()
            .shouldStopObservingSource();
    }

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__AndWhenObservedAgain__AndThenOneObserverIsRemoved__AndThenTheLastObserverIsRemoved__shouldNotComputeValue() {
        new WhenObservedWithInvalidationListener()
            .new AndWhenObservedAgain()
            .new AndThenOneObserverIsRemoved()
            .new AndThenTheLastObserverIsRemoved()
            .shouldNotComputeValue();
    }

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__AndWhenObservedAgain__AndThenOneObserverIsRemoved__AndThenTheLastObserverIsRemoved__shouldNoLongerBeValid() {
        new WhenObservedWithInvalidationListener()
            .new AndWhenObservedAgain()
            .new AndThenOneObserverIsRemoved()
            .new AndThenTheLastObserverIsRemoved()
            .shouldNoLongerBeValid();
    }

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__AndWhenObservedAgain__AndThenOneObserverIsRemoved__AndThenTheLastObserverIsRemoved__AndTheListenerIsRemovedAgain__shouldNotStartObservingSource() {
        new WhenObservedWithInvalidationListener()
            .new AndWhenObservedAgain()
            .new AndThenOneObserverIsRemoved()
            .new AndThenTheLastObserverIsRemoved()
            .new AndTheListenerIsRemovedAgain()
            .shouldNotStartObservingSource();
    }

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__AndWhenObservedAgain__AndThenOneObserverIsRemoved__AndThenTheLastObserverIsRemoved__AndTheListenerIsRemovedAgain__shouldNotStopObservingSource() {
        new WhenObservedWithInvalidationListener()
            .new AndWhenObservedAgain()
            .new AndThenOneObserverIsRemoved()
            .new AndThenTheLastObserverIsRemoved()
            .new AndTheListenerIsRemovedAgain()
            .shouldNotStopObservingSource();
    }

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__AndWhenObservedAgain__AndThenOneObserverIsRemoved__AndThenTheLastObserverIsRemoved__AndTheListenerIsRemovedAgain__shouldNotComputeValue() {
        new WhenObservedWithInvalidationListener()
            .new AndWhenObservedAgain()
            .new AndThenOneObserverIsRemoved()
            .new AndThenTheLastObserverIsRemoved()
            .new AndTheListenerIsRemovedAgain()
            .shouldNotComputeValue();
    }

    @org.junit.Test
    public void WhenObservedWithInvalidationListener__AndWhenObservedAgain__AndThenOneObserverIsRemoved__AndThenTheLastObserverIsRemoved__AndTheListenerIsRemovedAgain__shouldNotBeValid() {
        new WhenObservedWithInvalidationListener()
            .new AndWhenObservedAgain()
            .new AndThenOneObserverIsRemoved()
            .new AndThenTheLastObserverIsRemoved()
            .new AndTheListenerIsRemovedAgain()
            .shouldNotBeValid();
    }
}
