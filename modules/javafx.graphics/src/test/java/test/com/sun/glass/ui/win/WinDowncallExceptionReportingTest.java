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

package test.com.sun.glass.ui.win;

import com.sun.glass.ui.win.WinGlassNativeShim;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test: a Java exception that {@code glass.dll}'s JNI code reports from inside an FFM downcall
 * reaches {@code Application.reportException}, instead of disappearing.
 * <p>
 * <b>The bug.</b> JNI {@code FindClass} resolves against the class loader of the nearest Java frame, and
 * inside an FFM downcall that frame is {@code java.base}'s boot-loaded downcall stub, which cannot see
 * {@code com.sun.glass.ui.Application}. {@code CheckAndClearException} (then in {@code Utils.cpp}; in
 * {@code GlassAccessibleJni.cpp} since it moved there with the fix) looked that class
 * up at report time, got a {@code NoClassDefFoundError}, cleared it and returned - losing the exception it
 * existed to report. Since the ABI 3 run-loop flip the toolkit thread lives inside the
 * {@code gwin_run_loop} downcall, so every exception from a UI Automation callback (accessibility's 74
 * report sites, screen reader running) was silently dropped. A ten-JVM probe on JDK 26 settled it:
 * {@code FindClass} of an application class answered NULL from a downcall
 * and succeeded from a JNI native method in every configuration tried - class path and module path,
 * interpreter, C1 and C2, before and after 20,000 warm-up calls, and through a downcall-upcall-downcall
 * chain. The fix caches {@code Application} and its {@code reportException} id from inside a real JNI native:
 * {@code WinAccessible._initIDs}, the static initializer of the class whose UI Automation callbacks report.
 * {@code WinApplication.initIDs} cached them too until ABI 5 deleted it, so {@code WinAccessible} is the
 * only writer left, and this test initializes it first or fails.
 * <p>
 * <b>Why nothing caught it.</b> The bug was invisible to every earlier test: the module suite has no toolkit
 * and no screen reader, {@code tests/system} drives no UI Automation client, and the only symptom is an
 * exception that is not printed. Nothing reached {@code CheckAndClearException} from a downcall at all
 * until {@code gwin_test_report_exception_in_downcall} existed.
 * <p>
 * <b>What is asserted, and why the subject moved.</b> The JNI sink this test used to drive -
 * {@code gwin_test_report_exception_in_downcall} over {@code CheckAndClearException} - had exactly one
 * writer for its cached {@code Application} class, {@code WinAccessible._initIDs}, and that native method
 * no longer exists: the accessibility flip turned all nine into downcalls, so {@code glass.dll} looks
 * nothing up any more and the regression is structurally impossible rather than fixed. What replaced the
 * reporting is the Java side of an accessibility upcall stub, so that is what this test drives now: a
 * table whose target throws is installed, a slot is fired from inside a downcall (the same failing frame
 * shape), and delivery is observed where {@code Application.reportException} delivers - the <em>calling
 * thread's</em> {@code Thread.UncaughtExceptionHandler} ({@code Application.java:448-453}), installed here
 * around the call. The library must see {@code GWIN_ERR_UPCALL} and not a {@code Throwable}: one escaping
 * an upcall stub terminates the JVM. That nothing is left pending is observed by a JNI native method
 * ({@code Runtime.availableProcessors}) completing afterwards.
 */
@EnabledOnOs(OS.WINDOWS)
public class WinDowncallExceptionReportingTest {

    /** The message the throwing slot's target gives its {@code RuntimeException}. */
    static final String MESSAGE = WinGlassNativeShim.THROWING_SLOT_MESSAGE;

    /** How often the path is driven in a row, so a state left behind by one call would show in the next. */
    static final int CALLS = 3;

    /** The accessible slot the throwing table is fired at; any one would do, slot 0 is the first. */
    private static final int SLOT = 0;

    /** An id no registry holds: the throwing table answers before any peer is looked up. */
    private static final long UNKNOWN_ID = 0x7FFF_0000_0002L;

    /** {@code GWIN_ERR_UPCALL} - what a provider method turns into {@code E_FAIL}. */
    private static final long GWIN_ERR_UPCALL = -3L;

    @BeforeAll
    static void requireNatives() {
        WinGlassNatives.require();
        Throwable accessibleFailure = WinGlassNativeShim.initializeWinAccessible();
        assertNull(accessibleFailure, "WinAccessible could not initialize, so the accessibility tables"
                + " - the path this test drives - were never installed: " + accessibleFailure);
    }

    /**
     * The same guarantee, on the path the flip left behind: a {@code Throwable} raised inside an
     * accessibility upcall stub, entered from inside the {@code gwin_test_fire_accessible_callback}
     * downcall, reaches {@code Application.reportException} - and the library sees
     * {@code GWIN_ERR_UPCALL} rather than a {@code Throwable} unwinding into a COM vtable, which
     * terminates the JVM.
     */
    @Test
    public void anExceptionReportedInsideADowncallReachesTheUncaughtExceptionHandler() {
        Thread current = Thread.currentThread();
        for (int call = 1; call <= CALLS; call++) {
            List<Throwable> delivered = new ArrayList<>();
            List<Thread> deliveredOn = new ArrayList<>();
            Thread.UncaughtExceptionHandler previous = current.getUncaughtExceptionHandler();
            // getUncaughtExceptionHandler() answers the ThreadGroup when the thread has no handler of its own.
            boolean hadOwnHandler = !(previous instanceof ThreadGroup);
            long result;
            try (Arena arena = Arena.ofConfined()) {
                // gwin_test_fire_accessible_callback wants at least gwin_sizeof_variant() bytes, and it
                // guards `out` with a reinterpret, which cannot re-check this arena's real size.
                MemorySegment out = arena.allocate(Math.max(WinGlassNativeShim.sizeOfVariant(), 64), 8);
                try {
                    current.setUncaughtExceptionHandler((thread, throwable) -> {
                        deliveredOn.add(thread);
                        delivered.add(throwable);
                    });
                    result = WinGlassNativeShim.fireAccessibleIntoThrowingTable(SLOT, UNKNOWN_ID, out);
                } finally {
                    current.setUncaughtExceptionHandler(hadOwnHandler ? previous : null);
                }
            }
            String which = "call " + call;

            assertTrue(Runtime.getRuntime().availableProcessors() > 0, which);
            assertEquals(GWIN_ERR_UPCALL, result, which + ": the library did not see GWIN_ERR_UPCALL");
            assertEquals(1, delivered.size(), which + ": the Throwable was not delivered to"
                    + " Application.reportException - the FindClass-inside-a-downcall regression"
                    + " (delivered: " + delivered + ")");
            Throwable throwable = delivered.get(0);
            assertEquals(RuntimeException.class, throwable.getClass(), which + ": " + throwable);
            assertEquals(MESSAGE, throwable.getMessage(), which);
            assertSame(current, deliveredOn.get(0), which + ": reportException delivers on the calling thread");
        }
    }
}
