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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The Windows Glass application peer, {@code WinApplication}, and the two natives of it that ABI 3
 * deleted: {@code _supportsUnifiedWindows} ({@code GlassApplication.cpp:504-508}, whose whole body was
 * {@code return (IS_WINVISTA);}) and {@code _getDefaultBrowser} ({@code :537-559}, one
 * {@code AssocQueryStringW} and a {@code CreateJString}). Both were {@code WRAPPER}s in the
 * native-necessity triage, so there is no C left for either: the facade binds
 * {@code kernel32!GetVersion} and {@code shlwapi!AssocQueryStringW} itself.
 * <p>
 * <b>What makes this parity and not a rewrite</b> is that Java calls the same exported symbol, in the
 * same process, on the same thread, so the oracle is byte-equality against the JNI - and that oracle
 * only exists while both halves do. {@link #unifiedWindowsSupportMatchesTheJniBodyItReplaces()} and
 * {@link #theDefaultBrowserMatchesTheJniBodyItReplaces()} were written and run <em>before</em> the two
 * methods stopped being {@code native}, which is the only time that comparison could be made. After the
 * flip they compare the same Java through one more frame and keep proving that the peer routes to the
 * facade, which is worth as much: a peer that silently lost its override would otherwise show up only
 * in a running application.
 * <p>
 * Each A/B carries an <b>anti-vacuity</b> assertion, because both of these have a value that a broken
 * binding would produce as readily as a working one: {@code GetVersion} answering 0 makes every
 * version predicate false, and {@code AssocQueryStringW} failing makes every browser {@code null}, and
 * in both cases the JNI and the FFM arm would agree on nothing at all. So the predicate is also
 * required to discriminate ({@link #theVersionPredicateIsTheMacroAtEveryBoundary()}) and the string
 * call is required to answer at least one association this machine really registers.
 * <p>
 * The ABI 3 change set then took six more, which are {@code OS-CALL} rather than {@code WRAPPER}: the
 * message pump, its nested twin, {@code DestroyWindow} on the toolkit window and the
 * {@code SendMessage} / {@code PostMessage} pair that schedules a {@code Runnable}. They have no A/B,
 * and cannot have one: each needs a live toolkit window and a running message loop, which a module
 * unit-test JVM has neither of. What is asserted here instead is everything that can be asserted
 * without one - that {@code GwinAppCallbacks} is the size the C compiler made it, that the table is
 * installed by the peer's static initializer, and that the no-toolkit arm of both scheduling exports
 * runs nothing, throws nothing and leaves the runnable registry empty. The rest is
 * {@code tests/system} work.
 * <p>
 * The five natives that change set left on {@code WinApplication} were not wrappers either, and ABI 5 removed
 * them all; {@link #winApplicationDeclaresNoNativeAnyMore()} pins that, so that the boundary is a test
 * rather than a sentence in a commit message.
 * <p>
 * Line numbers into the {@code native-glass/win} C++ sources refer to those files at commit {@code 8492cb03b0}
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-glass/win/<file>}).
 */
@EnabledOnOs(OS.WINDOWS)
public class WinApplicationNativeTest {

    /**
     * Associations Windows registers on every installation, most-specific first. The anti-vacuity
     * check needs one association whose {@code ASSOCSTR_COMMAND} really exists; {@code .exe} and
     * {@code .txt} come from the base OS registry ({@code HKCR\exefile}, {@code HKCR\txtfile}) and
     * survive an image with no browser and no user profile, which {@code https} does not.
     */
    private static final List<String> CONTROL_ASSOCIATIONS = List.of("https", "http", ".html", ".txt", ".exe");

    /**
     * A scheme nothing registers. This is the input that really takes the {@code FAILED(hr) -> null}
     * arm of {@code GlassApplication.cpp:554-556}: measured, an unregistered <em>scheme</em> fails
     * while an unregistered <em>extension</em> succeeds with the shell's fallback handler - see
     * {@link #anUnknownExtensionFallsBackToTheShellsHandler()}.
     */
    private static final String UNREGISTERED_SCHEME = "jfx-no-such-scheme-8f3c1d";

    /** An extension nothing registers; {@code AssocQueryStringW} answers for it anyway. */
    private static final String UNKNOWN_EXTENSION = ".jfx-no-such-association-8f3c1d";

    /** {@code MAX_PATH}: the size of the C's stack buffer, in characters ({@code GlassApplication.cpp:541}). */
    private static final int MAX_PATH = 260;

    @BeforeAll
    static void requireNatives() {
        WinGlassNatives.require();
        // All three lazy library holders, in the order WinGlassNativeTest's exact BOUND_SYMBOLS list
        // expects them: which test class runs first in this shared JVM is not fixed, the order within
        // each @BeforeAll is. Without the first two, a run of this class alone would bind shlwapi
        // before winmm and gdi32 and make that assertion order-dependent.
        WinGlassNativeShim.bindTimerSymbols();
        WinGlassNativeShim.bindCursorSymbols();
        WinGlassNativeShim.bindBrowserSymbols();
    }

    // ---------------------------------------------------------------------------------------------
    // _supportsUnifiedWindows: kernel32!GetVersion and Utils.h's IS_WINVER_ATLEAST (L61-66)
    // ---------------------------------------------------------------------------------------------

    /**
     * The A/B. {@code Application.supportsUnifiedWindows()} calls
     * {@code WinApplication._supportsUnifiedWindows}, which was the JNI body
     * {@code return (IS_WINVISTA);} and is now {@code isWindowsVersionAtLeast(6, 0)} over the same
     * {@code kernel32!GetVersion}.
     * <p>
     * Anti-vacuity: both arms answering {@code false} on a machine where {@code GetVersion} returned
     * 0 would agree just as well, so the raw value is required to describe a Windows that can run
     * Glass at all. Glass needs Vista ({@code GlassApplication.cpp:363} gates the DPI functions on
     * exactly this macro), and {@code GetVersion} on an unmanifested process reports 6.2 whatever the
     * true build is, so a major version below 6 here means the call did not work, not that the
     * machine is old.
     */
    @Test
    public void unifiedWindowsSupportMatchesTheJniBodyItReplaces() {
        int version = WinGlassNativeShim.windowsVersion();
        int major = version & 0xFF;
        assertTrue(major >= 6, "kernel32!GetVersion reported major version " + major + " (raw 0x"
                + Integer.toHexString(version) + "), which no Windows that can run Glass does: the"
                + " agreement asserted below would then be an agreement on nothing.");

        assertEquals(WinGlassNativeShim.supportsUnifiedWindowsThroughPeer(),
                WinGlassNativeShim.isWindowsVersionAtLeast(6, 0));
        assertTrue(WinGlassNativeShim.isWindowsVersionAtLeast(6, 0),
                "IS_WINVISTA is false, so _supportsUnifiedWindows would now report no support for"
                + " StageStyle.UNIFIED: raw GetVersion 0x" + Integer.toHexString(version));
    }

    /**
     * The macro's arithmetic, pinned against the raw value rather than against a platform:
     * {@code LOBYTE(LOWORD(v))} is the major version, {@code HIBYTE(LOWORD(v))} the minor, and the C
     * reads {@code a > maj || (a == maj && b >= min)} because {@code &&} binds tighter than
     * {@code ||} ({@code Utils.h:61-64}).
     * <p>
     * This is also the anti-vacuity check for the predicate itself: it has to be able to answer both
     * ways on this machine, which a binding that always returned 0 could not.
     */
    @Test
    public void theVersionPredicateIsTheMacroAtEveryBoundary() {
        int version = WinGlassNativeShim.windowsVersion();
        int major = version & 0xFF;
        int minor = (version >>> 8) & 0xFF;

        assertTrue(WinGlassNativeShim.isWindowsVersionAtLeast(major, minor), "the reported version");
        assertTrue(WinGlassNativeShim.isWindowsVersionAtLeast(major, 0), "minor 0 of the same major");
        assertTrue(WinGlassNativeShim.isWindowsVersionAtLeast(major - 1, 255),
                "any minor of an older major, which the > arm answers before the minor is looked at");
        assertFalse(WinGlassNativeShim.isWindowsVersionAtLeast(major, minor + 1), "one minor above");
        assertFalse(WinGlassNativeShim.isWindowsVersionAtLeast(major + 1, 0), "one major above");
        assertFalse(WinGlassNativeShim.isWindowsVersionAtLeast(255, 255), "no Windows is 255.255");
        assertTrue(WinGlassNativeShim.isWindowsVersionAtLeast(0, 0), "every Windows is at least 0.0");
    }

    /**
     * The two bytes are read out of the low word only. The high word of {@code GetVersion} is the
     * build number - 19045 on this machine, 0x4A65, whose low byte 0x65 would be major 101 if the
     * masking were wrong - so a predicate that ignored the mask would answer the same as one that
     * applied it for {@code (6, 0)} and differently for everything near the real version. Asserting
     * that the decoded pair is a plausible Windows is what catches that.
     */
    @Test
    public void theVersionIsDecodedFromTheLowWordOnly() {
        int version = WinGlassNativeShim.windowsVersion();
        int major = version & 0xFF;
        int minor = (version >>> 8) & 0xFF;
        assertTrue(major >= 6 && major <= 10, "major " + major + " from raw 0x"
                + Integer.toHexString(version) + ": either the low-word masking is wrong, or Windows"
                + " has shipped a major version above 10 and this bound needs raising");
        assertTrue(minor <= 4, "minor " + minor + " from raw 0x" + Integer.toHexString(version));
    }

    // ---------------------------------------------------------------------------------------------
    // _getDefaultBrowser: shlwapi!AssocQueryStringW (GlassApplication.cpp:537-559)
    // ---------------------------------------------------------------------------------------------

    /**
     * The A/B. {@code WinApplication._showDocument} asks {@code _getDefaultBrowser} for the command
     * line registered for {@code https}; that was the JNI body and is now
     * {@code WinGlassNative.defaultBrowser()} over the same {@code shlwapi!AssocQueryStringW}, with
     * the same {@code ASSOCF_NONE} / {@code ASSOCSTR_COMMAND} and the same 260-character buffer.
     * <p>
     * Anti-vacuity: {@code null == null} would pass the equality whatever the binding did, including
     * a binding that can only fail, so the same call is required to discriminate on this machine -
     * an association that cannot exist must be {@code null}, and at least one association Windows
     * really registers must come back as a non-blank command line.
     */
    @Test
    public void theDefaultBrowserMatchesTheJniBodyItReplaces() {
        String peer = WinGlassNativeShim.defaultBrowserThroughPeer();
        String facade = WinGlassNativeShim.defaultBrowser();
        assertEquals(peer, facade, "WinApplication._getDefaultBrowser and WinGlassNative.defaultBrowser");

        assertNull(WinGlassNativeShim.assocQueryCommand(UNREGISTERED_SCHEME),
                "a scheme nothing registers has to take the FAILED(hr) -> null arm, so that the"
                + " equality above is an equality of answers and not of failures");
        String registered = null;
        for (String association : CONTROL_ASSOCIATIONS) {
            String command = WinGlassNativeShim.assocQueryCommand(association);
            if (command != null && !command.isBlank()) {
                registered = association + " -> " + command;
                break;
            }
        }
        assertNotNull(registered, "AssocQueryStringW answered nothing for any of " + CONTROL_ASSOCIATIONS
                + ", so the equality above compared two nulls and would pass against a binding that"
                + " never works. .exe and .txt come from the base OS registry, so this is a broken"
                + " binding or a broken machine, not a machine without a browser.");

        if (peer != null) {
            assertFalse(peer.isBlank(), "a successful AssocQueryStringW must not yield a blank command");
        }
    }

    /**
     * The command line is a command line: whatever the handler is, it names an executable and takes
     * the URL either as {@code %1} or as a trailing argument - which is exactly the shape
     * {@code _showDocument} parses ({@code WinApplication.java:313-343}). Skipped rather than failed
     * when the machine registers no {@code https} handler, because that is the one part of this the
     * machine decides.
     */
    @Test
    public void theDefaultBrowserLooksLikeACommandLine() {
        String browser = WinGlassNativeShim.defaultBrowser();
        assumeTrue(browser != null, "this machine registers no https handler, so there is no command"
                + " line to look at; the parity assertion of the A/B does not depend on this one.");
        assertFalse(browser.isBlank(), browser);
        assertTrue(browser.toLowerCase().contains(".exe"), browser);
        assertTrue(browser.length() < 260, "the buffer is 260 characters including the NUL: " + browser);
    }

    /** Two calls of a registry read answer the same; nothing here caches, so this is the OS's answer. */
    @Test
    public void theDefaultBrowserIsStableAcrossCalls() {
        assertEquals(WinGlassNativeShim.defaultBrowser(), WinGlassNativeShim.defaultBrowser());
    }

    /**
     * The failure arm, {@code FAILED(hr) -> null} ({@code GlassApplication.cpp:554-556}): a scheme
     * nothing registers, and the empty string, which is not an association at all. Neither throws and
     * neither yields a partially-read buffer. An unregistered <em>extension</em> is not in this list
     * on purpose - see {@link #anUnknownExtensionFallsBackToTheShellsHandler()}.
     */
    @Test
    public void anUnregisteredSchemeIsNull() {
        assertNull(WinGlassNativeShim.assocQueryCommand(UNREGISTERED_SCHEME));
        assertNull(WinGlassNativeShim.assocQueryCommand(""));
    }

    /**
     * What an unregistered <em>extension</em> does, which is not what an unregistered scheme does and
     * is the reason this test exists at all: {@code AssocQueryStringW} answers {@code S_OK} with the
     * shell's fallback handler rather than failing. Measured on Windows 10.0.19045,
     * {@code ".jfx-no-such-association-8f3c1d"} gives {@code C:\WINDOWS\system32\OpenWith.exe "%1"},
     * which is a perfectly good command line that opens the "Open with" dialog.
     * <p>
     * The literal is deliberately not asserted: it is the shell's fallback, not this binding's
     * behaviour, and the JNI body would have returned exactly the same string on the same machine -
     * including for {@code https} on a machine with no browser registered, where
     * {@code _showDocument} therefore opens the "Open with" dialog rather than printing
     * "Could not retrieve default browser" ({@code WinApplication.java:315-318}). What is asserted is
     * the part a wrong binding could break: no exception, and no half-read buffer.
     */
    @Test
    public void anUnknownExtensionFallsBackToTheShellsHandler() {
        String command = WinGlassNativeShim.assocQueryCommand(UNKNOWN_EXTENSION);
        if (command != null) {
            assertFalse(command.isBlank(), "a command line, or null; never blank");
            assertTrue(command.length() < MAX_PATH, command);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The boundary of the peer
    // ---------------------------------------------------------------------------------------------

    /**
     * The two {@code WRAPPER}s are gone, and so are the six {@code OS-CALL}s the ABI 3 change set flipped.
     * While the two wrappers were still {@code native} this test asserted the list of thirteen, which
     * is what made the two A/B comparisons above JNI-against-FFM rather than Java-against-itself; that
     * run could only be made while the list was thirteen long.
     */
    @Test
    public void winApplicationHasLostTheEightNativesThatWereFlipped() {
        List<String> natives = WinGlassNativeShim.nativeMethodsOf("WinApplication");
        assertFalse(natives.contains("_getDefaultBrowser"), natives.toString());
        assertFalse(natives.contains("_supportsUnifiedWindows"), natives.toString());
        assertFalse(natives.contains("_runLoop"), natives.toString());
        assertFalse(natives.contains("_terminateLoop"), natives.toString());
        assertFalse(natives.contains("_enterNestedEventLoopImpl"), natives.toString());
        assertFalse(natives.contains("_leaveNestedEventLoopImpl"), natives.toString());
        assertFalse(natives.contains("_invokeAndWait"), natives.toString());
        assertFalse(natives.contains("_submitForLaterInvocation"), natives.toString());
    }

    /**
     * {@code WinApplication} declares no {@code native} method. The last five went with ABI 5:
     * {@code getPlatformPreferences}, the last {@code java.util.Map} built in C, is
     * {@code WinPreferences.collect(GWIN_PT_ALL)}, with its A/B in {@code tests/system}
     * ({@code WinPreferencesParityTest}); {@code _init} ({@code OS-CALL}, {@code CreateWindowEx} of the toolkit
     * window) is {@code gwin_app_create} after a Java {@code loadDpiFuncs}; {@code staticScreen_getScreens} is
     * the Java enumeration and arrangement, against the JNI's table in {@code WinScreenParityTest} and on a
     * started toolkit in {@code tests/system}; and the two JNI-only bodies have no replacement -
     * {@code initIDs}, whose cached {@code reportException} id {@code WinAccessible._initIDs} caches too, and
     * {@code _setClassLoader}, which fed {@code GlassApplication::ClassForName} for that enumeration.
     * <p>
     * Asserted as an exact (empty) list, so that adding a native to this peer fails here rather than in a
     * commit message.
     */
    @Test
    public void winApplicationDeclaresNoNativeAnyMore() {
        List<String> natives = WinGlassNativeShim.nativeMethodsOf("WinApplication");
        assertEquals(List.of(), natives, "the natives WinApplication declares");
    }

    /**
     * {@code GwinScreenCallbacks} is installed by {@code WinApplication}'s static initializer, beside the other
     * tables, in the change set that made the enumeration Java - so that {@code HandleDisplayChange} never ran
     * its JNI path, which would have looked {@code Screen} up through a class loader nothing hands the C any
     * more. Since that path was deleted, a display change that finds no table is dropped, so the
     * install is still required. Touching the peer at all is enough to prove it.
     */
    @Test
    public void theScreenCallbackTableIsInstalledByWinApplicationsStaticInitializer() {
        WinGlassNativeShim.platformKeys();   // forces the peer, and so WinApplication's initializer
        assertTrue(WinGlassNativeShim.screenCallbacksInstalled());
    }

    // ---------------------------------------------------------------------------------------------
    // The ABI-3 loop/invoke family: GwinAppCallbacks and the two scheduling exports
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code GwinAppCallbacks} against the {@code sizeof} the C compiler computed. The probe exists
     * for exactly one purpose: to fail here the day a second slot is appended to the struct in C
     * without {@link WinGlassNativeShim}'s layout following it. Without it the facade would write one
     * pointer into a two-pointer table and the second slot would hold whatever the arena's allocation
     * left there - a call through an uninitialised function pointer, with no symptom until the first
     * message that reads it.
     */
    @Test
    public void theApplicationCallbackTableHasTheSizeTheCCompilerGaveIt() {
        assertEquals(8, WinGlassNativeShim.sizeOfAppCallbacks());
        assertEquals(8, WinGlassNativeShim.layoutByteSize("GwinAppCallbacks"));
        assertEquals(WinGlassNativeShim.sizeOfAppCallbacks(),
                WinGlassNativeShim.layoutByteSize("GwinAppCallbacks"));
        assertEquals(0, WinGlassNativeShim.offset("GwinAppCallbacks", "run_runnable"));
    }

    /**
     * The table is installed by {@code WinApplication}'s static initializer, beside the preferences
     * one, and touching the peer at all is enough to prove it. Installing again is a no-op, which is
     * what makes the assertion safe to make from a test.
     */
    @Test
    public void theApplicationCallbackTableIsInstalledByWinApplicationsStaticInitializer() {
        WinGlassNativeShim.platformKeys();   // forces the peer, and so WinApplication's initializer
        assertTrue(WinGlassNativeShim.applicationCallbackInstalled());
        WinGlassNativeShim.installApplicationCallback();
        assertTrue(WinGlassNativeShim.applicationCallbackInstalled());
    }

    /**
     * {@code gwin_invoke_later} with no toolkit window - the state a unit-test JVM is always in, and
     * the one the C answers with {@link WinGlassNativeShim#constant} {@code GWIN_ERR_NO_TOOLKIT}
     * after destroying the action it built. No callback can arrive for that id, so the facade drops
     * the registry entry: this is the one place where a missing removal would be a real leak rather
     * than the JNI's own.
     * <p>
     * The runnable must not run. {@code ExecActionLater} deleted the action and returned silently
     * ({@code GlassApplication.cpp:238-245}), and nothing was thrown to the submitter.
     */
    @Test
    public void submittingWithNoToolkitRunsNothingAndLeavesNoRegistryEntry() {
        assertEquals(-2, WinGlassNativeShim.constant("GWIN_ERR_NO_TOOLKIT"));
        AtomicBoolean ran = new AtomicBoolean();
        assertEquals(0, WinGlassNativeShim.submitWithNoToolkit(() -> ran.set(true)),
                "gwin_invoke_later reported GWIN_ERR_NO_TOOLKIT, so the registry entry must be gone");
        assertFalse(ran.get(), "there is no toolkit window, so nothing can have run the runnable");
    }

    /**
     * {@code gwin_invoke_and_wait} with no toolkit window must neither throw nor block. The JNI did
     * neither: {@code ExecAction} returned early ({@code GlassApplication.cpp:229-235}) without
     * sending anything and without touching the runnable, so the submitting thread carried on. This
     * runs on shutdown paths, which is why turning it into an exception would be a behaviour change
     * and not an improvement.
     */
    @Test
    public void invokingAndWaitingWithNoToolkitRunsNothingAndDoesNotThrow() {
        AtomicBoolean ran = new AtomicBoolean();
        assertEquals(0, WinGlassNativeShim.invokeAndWaitWithNoToolkit(() -> ran.set(true)),
                "the finally in invokeAndWait removes the entry whatever the status was");
        assertFalse(ran.get(), "there is no toolkit window, so nothing can have run the runnable");
    }

    /**
     * {@code gwin_terminate_loop} with no toolkit window returns without throwing and touches
     * nothing: {@code GetToolkitHWND()} is {@code NULL} when there is no instance and
     * {@code IsWindow(NULL)} is false, which is the arm the JNI's {@code _terminateLoop} took on any
     * call after the window had gone. It is the one loop export whose wrapper nothing else executes
     * - {@code boundSymbols()} pins its name, not its {@code FunctionDescriptor} - and the first real
     * execution would otherwise be a toolkit teardown, where a wrong descriptor shows up as a JVM
     * that does not exit.
     */
    @Test
    public void terminatingWithNoToolkitIsANoOpAndDoesNotThrow() {
        assertEquals(0, WinGlassNativeShim.terminateLoopWithNoToolkit(),
                "IsWindow(NULL) is false, so nothing was destroyed and no registry entry moved");
        assertEquals(0, WinGlassNativeShim.terminateLoopWithNoToolkit(),
                "and it is idempotent, as DestroyWindow-if-IsWindow was");
    }

    /**
     * {@code gwin_leave_nested_event_loop} with no nested loop running only raises a flag that
     * {@code GlassApplication::EnterNestedEventLoop} clears as its first statement, so it neither
     * throws nor leaves residue for a later pump. The only "no residue" a JVM without a pump can
     * observe is that the scheduling exports still behave afterwards: {@code gwin_invoke_later} still
     * answers {@code GWIN_ERR_NO_TOOLKIT}, runs nothing and cleans up.
     */
    @Test
    public void leavingANestedLoopThatIsNotRunningDoesNotThrow() {
        WinGlassNativeShim.leaveNestedEventLoopWithNoLoop();
        WinGlassNativeShim.leaveNestedEventLoopWithNoLoop();
        AtomicBoolean ran = new AtomicBoolean();
        assertEquals(0, WinGlassNativeShim.submitWithNoToolkit(() -> ran.set(true)));
        assertFalse(ran.get(), "there is still no toolkit window, so nothing can have run it");
        assertEquals(0, WinGlassNativeShim.runnableRegistrySize());
    }

    /**
     * The registry is empty at rest. Every entry has exactly one owner that removes it - the upcall
     * when it runs one, the {@code finally} in {@code invokeAndWait}, and the
     * {@code GWIN_ERR_NO_TOOLKIT} arm of {@code submitForLaterInvocation} - and there is deliberately
     * no sweeper, so an entry with no owner would sit here for the life of the JVM and show up as a
     * non-zero count.
     */
    @Test
    public void theRunnableRegistryIsEmptyOnceNothingIsScheduled() {
        WinGlassNativeShim.submitWithNoToolkit(() -> { });
        WinGlassNativeShim.invokeAndWaitWithNoToolkit(() -> { });
        assertEquals(0, WinGlassNativeShim.runnableRegistrySize(),
                "no runnable is scheduled, so nothing may be held on glass.dll's behalf");
    }

    /**
     * The {@code run_runnable} target itself, driven without a pump: a runnable that throws is
     * reported through {@code Application.reportException} and nothing escapes - the
     * {@code CheckAndClearException} contract every upcall target of the facade keeps - it is
     * one-shot, so the registry is empty afterwards and a second callback for the same id runs
     * nothing, and an id that was never registered is a no-op. Before this test the catch arm of
     * this target was the one of the facade's three that nothing exercised; narrowing it to
     * {@code RuntimeException} would have passed the whole suite and let the first {@code Error}
     * thrown by an {@code invokeLater} body out of an upcall stub.
     */
    @Test
    public void aThrowingRunnableIsReportedAndRunsOnce() {
        Error deliberate = new AssertionError("WinApplicationNativeTest: deliberate");
        AtomicInteger ran = new AtomicInteger();
        long id = WinGlassNativeShim.registerRunnable(() -> {
            ran.incrementAndGet();
            throw deliberate;
        });
        assertEquals(1, WinGlassNativeShim.runnableRegistrySize());
        Thread current = Thread.currentThread();
        Thread.UncaughtExceptionHandler previous = current.getUncaughtExceptionHandler();
        AtomicReference<Throwable> reported = new AtomicReference<>();
        current.setUncaughtExceptionHandler((thread, throwable) -> reported.compareAndSet(null, throwable));
        try {
            WinGlassNativeShim.runRunnableTarget(id);
            assertSame(deliberate, reported.get(), "the Error must reach Application.reportException");
            assertEquals(1, ran.get());
            assertEquals(0, WinGlassNativeShim.runnableRegistrySize(), "one-shot: removed before it ran");

            reported.set(null);
            WinGlassNativeShim.runRunnableTarget(id);
            WinGlassNativeShim.runRunnableTarget(id + 1_000_000L);
            assertEquals(1, ran.get(), "a second callback for the same id, or an unknown id, runs nothing");
            assertNull(reported.get());
        } finally {
            current.setUncaughtExceptionHandler(previous);
        }
    }
}
