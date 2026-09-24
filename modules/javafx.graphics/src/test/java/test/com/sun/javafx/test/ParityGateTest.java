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

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The gate and the ledger themselves, with the mode passed explicitly so that both halves of the rule
 * are pinned whether or not this JVM was started with {@code -Djfx.parity.require=true}.
 */
public class ParityGateTest {

    /** A stand-in for the test class whose oracle is being gated; never registered in the shared map. */
    private static final class Oracle {
    }

    private static ParityGate.Ledger fresh() {
        return new ParityGate.Ledger(Oracle.class);
    }

    @Test
    public void anAvailableOracleNeverThrowsInEitherMode() {
        assertDoesNotThrow(() -> ParityGate.requireOracle(Oracle.class, true, false, () -> "unused"));
        assertDoesNotThrow(() -> ParityGate.requireOracle(Oracle.class, true, true, () -> "unused"));
    }

    @Test
    public void aMissingOracleIsASkipWithoutTheProperty() {
        TestAbortedException aborted = assertThrows(TestAbortedException.class,
                () -> ParityGate.requireOracle(Oracle.class, false, false, () -> "CreateMenu returned NULL."));
        assertEquals("CreateMenu returned NULL.", aborted.getMessage(),
                "without the property the message is the one the test would have given assumeTrue");
    }

    @Test
    public void aMissingOracleIsAFailureWithThePropertyAndNamesWhatIsMissing() {
        AssertionFailedError failure = assertThrows(AssertionFailedError.class,
                () -> ParityGate.requireOracle(Oracle.class, false, true, () -> "CreateMenu returned NULL."));
        String message = failure.getMessage();
        assertTrue(message.contains("-D" + ParityGate.REQUIRE_PROPERTY + "=true"), message);
        assertTrue(message.contains("Oracle"), message);
        assertTrue(message.contains("CreateMenu returned NULL."), message);
    }

    @Test
    public void theMessageIsOnlyBuiltWhenTheOracleIsMissing() {
        ParityGate.requireOracle(Oracle.class, true, true, () -> {
            throw new IllegalStateException("the supplier must not run for an available oracle");
        });
    }

    @Test
    public void theLedgerOfAClassIsOneObjectPerJvm() {
        assertSame(ParityGate.ledger(ParityGateTest.class), ParityGate.ledger(ParityGateTest.class));
    }

    @Test
    public void anUnarmedLedgerSaysNothingAfterAll() {
        ParityGate.Ledger ledger = fresh();
        assertFalse(ledger.oracleWasAvailable());
        assertEquals(0, ledger.comparisons());
        assertDoesNotThrow(ledger::assertOracleRan);
    }

    @Test
    public void aGateThatFoundTheOracleMissingDoesNotArmTheLedger() {
        ParityGate.Ledger ledger = fresh();
        assertThrows(TestAbortedException.class, () -> ledger.requireOracle(false, false, () -> "missing"));
        assertThrows(AssertionFailedError.class, () -> ledger.requireOracle(false, true, () -> "missing"));
        assertFalse(ledger.oracleWasAvailable());
        assertEquals(2, ledger.timesUnavailable());
        assertDoesNotThrow(ledger::assertOracleRan,
                "the gate has already reported the skip or the failure; the AfterAll must not report it again");
    }

    @Test
    public void theTwoArgumentGateReadsTheProperty() {
        ParityGate.Ledger ledger = fresh();
        Class<? extends Throwable> expected = ParityGate.required() ? AssertionFailedError.class
                : TestAbortedException.class;
        assertThrows(expected, () -> ledger.requireOracle(false, () -> "missing"));
        assertEquals(1, ledger.timesUnavailable());
    }

    @Test
    public void anArmedLedgerWithNoComparisonFailsAfterAll() {
        ParityGate.Ledger ledger = fresh();
        ledger.requireOracle(true, () -> "unused");
        assertTrue(ledger.oracleWasAvailable());
        AssertionFailedError failure = assertThrows(AssertionFailedError.class, ledger::assertOracleRan);
        assertTrue(failure.getMessage().contains("performed 0 comparisons"), failure.getMessage());
        assertTrue(failure.getMessage().contains("Oracle"), failure.getMessage());
    }

    @Test
    public void anArmedLedgerWithComparisonsPassesAfterAll() {
        ParityGate.Ledger ledger = fresh();
        ledger.oracleAvailable();
        ledger.compared();
        ledger.compared(25);
        assertEquals(26, ledger.comparisons());
        assertDoesNotThrow(ledger::assertOracleRan);
    }

    @Test
    public void comparisonsBeforeTheOracleWasAvailableStillCount() {
        // The order is not prescribed: a device body may count itself before a later test's gate arms
        // the ledger again. What matters is the pair at @AfterAll time.
        ParityGate.Ledger ledger = fresh();
        ledger.compared(3);
        ledger.oracleAvailable();
        assertDoesNotThrow(ledger::assertOracleRan);
    }

    @Test
    public void thePropertyIsReadFromTheSystemProperties() {
        assertEquals(Boolean.getBoolean("jfx.parity.require"), ParityGate.required());
        assertEquals("jfx.parity.require", ParityGate.REQUIRE_PROPERTY);
    }
}
