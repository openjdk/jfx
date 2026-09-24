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

package test.com.sun.javafx.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.abort;

/**
 * The "oracle must have run" rule for the parity tests of this module: a test that could not compare is
 * not the same as a test that compared and passed, and must never be reported as one.
 * <p>
 * The {@code *Natives} gate classes ({@code test.com.sun.pisces.PiscesNatives},
 * {@code test.com.sun.prism.d3d.D3DNatives}, ...) decide <em>library</em> availability: skip only when this
 * build has no natives at all, fail when a library is reachable and broken. This class lifts the same rule
 * one level up, to <em>comparison</em> availability. A golden captured from the JNI build is an oracle only
 * on the machine whose fonts, DirectWrite, adapter or window station it describes; a device test only
 * proves something where the device exists. Everywhere else those tests skip, which is correct - and
 * indistinguishable, in a green report, from having run. Two mechanisms close that:
 * <ol>
 * <li><b>{@code -Djfx.parity.require=true}</b> (default {@code false}). With it, every oracle gate that
 * would have skipped fails instead, naming what was missing: the font inventory that differs from the
 * golden's and where the golden was captured, the adapter {@code d3d_pipeline_init} could not open, the
 * {@code CreateMenu} that returned NULL. It is set on the one machine or CI job that owns each golden
 * and stated in the PR; without it nothing changes. See {@link Ledger#requireOracle}.</li>
 * <li><b>{@link Ledger#assertOracleRan()}</b> from an {@code @AfterAll}, unconditionally. Comparisons are
 * counted where they happen - per golden key, per snapshot, per device body - and a class whose oracle
 * was available and which nevertheless compared nothing fails, with or without the property: an empty
 * corpus, a golden whose body keys were all filtered out, a device loop that ran zero bodies, are bugs
 * on every machine. It is silent when the oracle was never available in this JVM, because then the gate
 * itself has already reported the skip (or, under the property, the failure).</li>
 * </ol>
 * <b>What the property must not touch.</b> A library that this platform does not build <em>by
 * design</em> is not a missing oracle: {@code prism_es2} is left out of every default Windows build
 * ({@code INCLUDE_ES2} defaults off in {@code native/CMakeLists.txt}), so {@code ES2Natives} skips the
 * ES2 classes there and Windows contributes no ES2 signal on purpose; the same holds for any mac- or
 * Linux-only library on another platform, and for a tree whose natives were never built at all
 * ({@code -DskipNative=true} with an empty {@code target/native/bin}). Those decisions stay with the
 * {@code *Natives} classes, which do not consult this property. What the property catches is the other
 * case only: the library loaded, the comparison exists, and it could not run on a machine where it
 * should have.
 */
public final class ParityGate {

    /** {@code -Djfx.parity.require=true}: an oracle that cannot run is a failure, not a skip. */
    public static final String REQUIRE_PROPERTY = "jfx.parity.require";

    private static final Map<Class<?>, Ledger> LEDGERS = new ConcurrentHashMap<>();

    private ParityGate() {
    }

    /** Whether this JVM was started with {@code -Djfx.parity.require=true}. */
    public static boolean required() {
        return Boolean.getBoolean(REQUIRE_PROPERTY);
    }

    /**
     * The ledger of {@code testClass}: one per class per JVM, so the counts survive across the class's
     * test methods and are read back by its {@code @AfterAll}. Surefire runs this module in one fork, and
     * every parity class reads only its own ledger, so the shared map is not a coupling between them.
     */
    public static Ledger ledger(Class<?> testClass) {
        return LEDGERS.computeIfAbsent(testClass, Ledger::new);
    }

    /**
     * The gate, with the mode explicit instead of read from the property - what
     * {@link Ledger#requireOracle(boolean, Supplier)} delegates to, kept visible so that the tests of the
     * gates themselves ({@code FontGoldensTest}, {@link ParityGateTest}) can pin both behaviours in one JVM.
     *
     * @throws org.opentest4j.TestAbortedException if {@code available} is false and {@code required} is
     *         false: the test is skipped with {@code whatIsMissing}, exactly as {@code assumeTrue} would
     * @throws AssertionError if {@code available} is false and {@code required} is true
     */
    public static void requireOracle(Class<?> testClass, boolean available, boolean required,
                                     Supplier<String> whatIsMissing) {
        if (available) {
            return;
        }
        String missing = whatIsMissing.get();
        if (required) {
            fail("-D" + REQUIRE_PROPERTY + "=true says this machine owns the oracle of "
                    + testClass.getSimpleName() + ", and the oracle could not run: " + missing
                    + " On any other machine this would be a skip; here it is the failure the property"
                    + " exists to report.");
        }
        abort(missing);
    }

    /**
     * The per-class record: how often the oracle was found available, how often it was not, and how many
     * comparisons were actually performed.
     */
    public static final class Ledger {

        private final Class<?> testClass;
        private final AtomicInteger available = new AtomicInteger();
        private final AtomicInteger unavailable = new AtomicInteger();
        private final AtomicInteger comparisons = new AtomicInteger();

        Ledger(Class<?> testClass) {
            this.testClass = testClass;
        }

        /**
         * The gate. Call it where the test would otherwise call {@code assumeTrue}: the golden's machine
         * header matches, the pipeline created a device, {@code CreateMenu} returned a handle. When
         * {@code available} the ledger is armed and {@link #assertOracleRan()} will insist on a comparison;
         * when not, the test is skipped - or, under {@code -Djfx.parity.require=true}, failed - with the
         * message {@code whatIsMissing} supplies, which must name the missing thing precisely enough to
         * act on: not "no device" but which call refused and what it said.
         */
        public void requireOracle(boolean available, Supplier<String> whatIsMissing) {
            requireOracle(available, required(), whatIsMissing);
        }

        /** {@link #requireOracle(boolean, Supplier)} with the mode explicit, for tests of the gates. */
        public void requireOracle(boolean available, boolean required, Supplier<String> whatIsMissing) {
            if (available) {
                oracleAvailable();
                return;
            }
            unavailable.incrementAndGet();
            ParityGate.requireOracle(testClass, false, required, whatIsMissing);
        }

        /**
         * Arms the ledger without a gate, for comparison methods that have no precondition beyond the
         * library (the Pisces and JPEG goldens run on every machine). Call it at the point the comparison is
         * about to start - after any capture-mode branch, which compares nothing and must not arm.
         */
        public void oracleAvailable() {
            available.incrementAndGet();
        }

        /** One comparison was performed: a golden key checked, a snapshot diffed, a device body run. */
        public void compared() {
            comparisons.incrementAndGet();
        }

        /** {@code count} comparisons were performed. */
        public void compared(int count) {
            comparisons.addAndGet(count);
        }

        public int comparisons() {
            return comparisons.get();
        }

        /** Whether the gate passed, or {@link #oracleAvailable()} was called, at least once in this JVM. */
        public boolean oracleWasAvailable() {
            return available.get() > 0;
        }

        /** How often the gate passed, or {@link #oracleAvailable()} was called, in this JVM. */
        public int timesAvailable() {
            return available.get();
        }

        /** How often the gate found the oracle missing in this JVM. */
        public int timesUnavailable() {
            return unavailable.get();
        }

        /**
         * For the class's {@code @AfterAll}: if the oracle was available at least once, at least one
         * comparison must have been performed. Unconditional - it does not read the property.
         *
         * @throws AssertionError if the oracle was available and nothing was compared
         */
        public void assertOracleRan() {
            if (!oracleWasAvailable()) {
                return;
            }
            int performed = comparisons.get();
            if (performed == 0) {
                fail(testClass.getSimpleName() + " found its oracle available " + available.get()
                        + " time(s) and performed 0 comparisons against it. A run that compares nothing"
                        + " passes for nothing: the golden has no body keys left, the corpus or the script"
                        + " is empty, or the device loop ran no body. Fix the test, not this assertion.");
            }
        }
    }
}
