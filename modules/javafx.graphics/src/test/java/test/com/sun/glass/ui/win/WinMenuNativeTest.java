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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.abort;

/**
 * The parity oracle for the Windows menus: every Windows menu operation {@code GlassMenu.cpp}
 * performed, run twice over real {@code HMENU}s and compared.
 * <p>
 * <b>Why this can be an automated test at all.</b> Menus are USER objects, not windows: creating one,
 * filling it and reading it back needs no window, no toolkit, no FX thread and no display. That is
 * what makes the eleven deleted JNI bodies provable rather than merely plausible - and it matters,
 * because <em>nothing else covers menus</em>. There was no test in the tree that so much as named
 * {@code WinMenuImpl} before this one, and there cannot be an end-to-end one: on Windows
 * {@code Application._supportsSystemMenu()} is {@code false}, so {@code WindowStage.init} never calls
 * {@code createMenuBar()} and {@code MenuBarSkin} never routes to {@code TKSystemMenu}. The one thing
 * this file cannot reach - a real {@code WM_COMMAND} from a real click - is covered by a manual
 * check outside this tree and by nothing else.
 * <p>
 * <b>The oracle.</b> While {@code WinMenuImpl} still declared its eleven natives, one fixed operation
 * script was run through them and then through {@code WinGlassNative}'s {@code user32} binds, in one
 * JVM, and the two transcripts had to be identical; the JNI arm's transcript was captured as
 * {@code menu-golden.txt} and committed. The natives, the JNI arm and the shim that drove it
 * ({@code WinMenuImplShim}) are gone, and {@link #facadeAgreesWithTheGolden()} is what carries the
 * parity evidence from here on: the facade against the transcript the JNI produced.
 * <p>
 * <b>The read-back was shared on purpose.</b> Both arms were inspected with the same
 * {@code GetMenuItemInfoW} calls and the same {@code MENUITEMINFOW} layout, which is what made the
 * JNI arm a test <em>of the layout</em>: the menus it produced were built by the C compiler's own
 * {@code MENUITEMINFOW}, so a Java layout that disagreed with the C struct could not have read them
 * back correctly - and the golden is that read-back. It also means a read-back that silently returns
 * nothing would look like agreement, which is what {@link #assertNotVacuous} exists to prevent.
 * <p>
 * <b>The twelfth body, and the last menu native.</b> {@code HandleMenuCommand} could not be flipped
 * with the other eleven: it is called <em>by</em> {@code GlassWindow::WindowProc} from inside
 * {@code DispatchMessage}, so it needed a callback table and therefore a new export. ABI 3 added it,
 * and with it {@code WinMenuImpl}'s last native, {@code _initIDs}, is gone. What the last four tests
 * here assert is everything about that upcall a JVM with no window can assert: the table is the size
 * the C compiler made it, it is installed at exactly the point {@code _initIDs()} used to run, an
 * unknown command id is not handled, and a target that throws is <em>also</em> not handled and has
 * its throwable reported rather than propagated. A real {@code WM_COMMAND} from a real click is still
 * only covered by that manual check.
 * <p>
 * Line numbers into the {@code native-glass/win} C++ sources refer to those files at commit {@code 8492cb03b0}
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-glass/win/<file>}).
 */
@EnabledOnOs(OS.WINDOWS)
public class WinMenuNativeTest {

    /** {@code -Djfx.menu.golden.capture=true} writes the golden instead of comparing against it. */
    static final String CAPTURE_PROPERTY = "jfx.menu.golden.capture";

    /** {@code -Djfx.menu.golden.regenerate=true} allows an existing golden to be overwritten. */
    static final String REGENERATE_PROPERTY = "jfx.menu.golden.regenerate";

    private static final String GOLDEN_FILE = "menu-golden.txt";
    private static final String RESOURCE_DIR = "src/test/resources/test/com/sun/glass/ui/win";
    private static final String CLASSES_DIR = "target/test-classes/test/com/sun/glass/ui/win";

    /** A handle no menu has; used for the "not a menu" arm of every operation. */
    private static final long BOGUS_HANDLE = 0x00000000DEADBEEFL;

    /** A command id no item carries; {@code CheckMenuItem} answers {@code (DWORD)-1} for it. */
    private static final int ABSENT_COMMAND_ID = 0xBEEF;

    /** winuser.h values, re-stated here so the facade's constants are compared against something. */
    private static final int WINUSER_MIIM_STATE = 0x00000001;
    private static final int WINUSER_MIIM_ID = 0x00000002;
    private static final int WINUSER_MIIM_SUBMENU = 0x00000004;
    private static final int WINUSER_MIIM_DATA = 0x00000020;
    private static final int WINUSER_MIIM_STRING = 0x00000040;
    private static final int WINUSER_MIIM_FTYPE = 0x00000100;
    private static final int WINUSER_MFT_STRING = 0x00000000;
    private static final int WINUSER_MFT_SEPARATOR = 0x00000800;
    private static final int WINUSER_MFS_GRAYED = 0x00000003;
    private static final int WINUSER_MFS_CHECKED = 0x00000008;
    private static final int WINUSER_MF_BYPOSITION = 0x00000400;
    private static final int WINUSER_MF_SEPARATOR = 0x00000800;
    private static final int WINUSER_MF_GRAYED = 0x00000001;

    private static boolean menusAvailable;

    private static final ParityGate.Ledger LEDGER = ParityGate.ledger(WinMenuNativeTest.class);

    @BeforeAll
    static void requireNatives() {
        WinGlassNatives.require();
        menusAvailable = probeMenus();
    }

    /**
     * The transcript comparison must have happened. Menus are USER objects that exist in every window
     * station, session 0 included, so on a machine where {@link #probeMenus()} says no the right report
     * is the gate's own message, and on a machine where it says yes the golden must have been compared.
     * A capture run compares nothing by design and is excused.
     */
    @AfterAll
    static void theOracleRan() {
        if (!Boolean.getBoolean(CAPTURE_PROPERTY)) {
            LEDGER.assertOracleRan();
        }
    }

    /**
     * Whether this session can create a menu at all. Menus are USER objects and should not need an
     * interactive window station, but that is an assumption until it has run somewhere that has none,
     * so it is a probe rather than a claim - the same shape as
     * {@code WinGlassNativeTest.probeDesktop()}. Unlike that one it does not describe a display: a NULL
     * here is a broken machine, which is why {@code -Djfx.parity.require=true} turns the skip it causes
     * into a failure ({@link ParityGate}).
     */
    private static boolean probeMenus() {
        long menu = WinGlassNativeShim.menuCreate();
        if (menu == 0) {
            return false;
        }
        WinGlassNativeShim.menuDestroy(menu);
        return true;
    }

    private static final String MENUS_REQUIRED =
            "user32 refused to create a menu in this session (CreateMenu returned NULL), so there is"
            + " nothing to drive: this JVM has no USER handle quota or no window station.";

    // ---------------------------------------------------------------------------------------------
    // The layout and the constants: what the transcript rests on
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code MENUITEMINFOW} (winuser.h {@code tagMENUITEMINFOW}) on LLP64. Getting this wrong is not
     * caught by {@code cbSize}: user32 also accepts {@code sizeof(MENUITEMINFOW) - sizeof(HBITMAP)},
     * which is exactly the 72 bytes the layout would have if the padding after {@code wID} were
     * forgotten - so the wrong layout is <em>accepted</em> and every pointer field lands eight bytes
     * low. What really pinned it was the JNI arm of the A/B, which read structures the C compiler
     * laid out, and what pins it now is the golden that arm produced; these assertions only say so
     * out loud and fail first, with a diagnosis.
     */
    @Test
    public void menuItemInfoLayoutMatchesTheWinuserAbi() {
        assertEquals(80L, WinGlassNativeShim.layoutByteSize("MENUITEMINFOW"),
                "MENUITEMINFOW is 80 bytes on x64 and arm64");
        assertEquals(0L, WinGlassNativeShim.offset("MENUITEMINFOW", "cbSize"));
        assertEquals(4L, WinGlassNativeShim.offset("MENUITEMINFOW", "fMask"));
        assertEquals(8L, WinGlassNativeShim.offset("MENUITEMINFOW", "fType"));
        assertEquals(12L, WinGlassNativeShim.offset("MENUITEMINFOW", "fState"));
        assertEquals(16L, WinGlassNativeShim.offset("MENUITEMINFOW", "wID"));
        assertEquals(24L, WinGlassNativeShim.offset("MENUITEMINFOW", "hSubMenu"),
                "hSubMenu is pointer-aligned, so four bytes of padding follow wID");
        assertEquals(32L, WinGlassNativeShim.offset("MENUITEMINFOW", "hbmpChecked"));
        assertEquals(40L, WinGlassNativeShim.offset("MENUITEMINFOW", "hbmpUnchecked"));
        assertEquals(48L, WinGlassNativeShim.offset("MENUITEMINFOW", "dwItemData"));
        assertEquals(56L, WinGlassNativeShim.offset("MENUITEMINFOW", "dwTypeData"));
        assertEquals(64L, WinGlassNativeShim.offset("MENUITEMINFOW", "cch"));
        assertEquals(72L, WinGlassNativeShim.offset("MENUITEMINFOW", "hbmpItem"),
                "hbmpItem is pointer-aligned, so four bytes of padding follow cch");
    }

    /**
     * The menu flags the facade hard-codes. {@code MFS_GRAYED} (3) and {@code MF_GRAYED} (1) are the
     * pair that matters: {@code GlassMenu.cpp} wrote the first when inserting an item and the second
     * when disabling one, and normalising them would change what a menu looks like.
     */
    @Test
    public void menuConstantsMatchWinuserH() {
        assertEquals(WINUSER_MIIM_STATE, WinGlassNativeShim.constant("MIIM_STATE"));
        assertEquals(WINUSER_MIIM_ID, WinGlassNativeShim.constant("MIIM_ID"));
        assertEquals(WINUSER_MIIM_SUBMENU, WinGlassNativeShim.constant("MIIM_SUBMENU"));
        assertEquals(WINUSER_MIIM_DATA, WinGlassNativeShim.constant("MIIM_DATA"));
        assertEquals(WINUSER_MIIM_STRING, WinGlassNativeShim.constant("MIIM_STRING"));
        assertEquals(WINUSER_MIIM_FTYPE, WinGlassNativeShim.constant("MIIM_FTYPE"));
        assertEquals(WINUSER_MFT_STRING, WinGlassNativeShim.constant("MFT_STRING"));
        assertEquals(WINUSER_MFT_SEPARATOR, WinGlassNativeShim.constant("MFT_SEPARATOR"));
        assertEquals(0, WinGlassNativeShim.constant("MFS_ENABLED"));
        assertEquals(WINUSER_MFS_GRAYED, WinGlassNativeShim.constant("MFS_GRAYED"));
        assertEquals(WINUSER_MFS_CHECKED, WinGlassNativeShim.constant("MFS_CHECKED"));
        assertEquals(0, WinGlassNativeShim.constant("MFS_UNCHECKED"));
        assertEquals(0, WinGlassNativeShim.constant("MF_BYCOMMAND"));
        assertEquals(WINUSER_MF_BYPOSITION, WinGlassNativeShim.constant("MF_BYPOSITION"));
        assertEquals(WINUSER_MF_SEPARATOR, WinGlassNativeShim.constant("MF_SEPARATOR"));
        assertEquals(0, WinGlassNativeShim.constant("MF_ENABLED"));
        assertEquals(WINUSER_MF_GRAYED, WinGlassNativeShim.constant("MF_GRAYED"));
        assertEquals(WINUSER_MFS_CHECKED, WinGlassNativeShim.constant("MF_CHECKED"));
        assertEquals(0, WinGlassNativeShim.constant("MF_UNCHECKED"));
        assertNotEquals(WinGlassNativeShim.constant("MFS_GRAYED"),
                WinGlassNativeShim.constant("MF_GRAYED"),
                "MFS_GRAYED is MF_GRAYED | MF_DISABLED and the C used both, one per call family");
    }

    // ---------------------------------------------------------------------------------------------
    // The golden the A/B left behind
    // ---------------------------------------------------------------------------------------------

    /**
     * The facade against {@code menu-golden.txt}, the transcript the JNI arm produced when it was
     * captured. It outlived the deletion of the natives and is the permanent parity gate: the same
     * script and the same read-back, compared line for line with what {@code GlassMenu.cpp} did.
     */
    @Test
    public void facadeAgreesWithTheGolden() throws IOException {
        LEDGER.requireOracle(menusAvailable, () -> MENUS_REQUIRED);
        List<String> facade = runScript(FACADE);
        assertNotVacuous(facade, "facade");
        if (Boolean.getBoolean(CAPTURE_PROPERTY)) {
            // The golden is the record of what GlassMenu.cpp's JNI bodies did, and that arm is gone:
            // a capture from here can only come from the code the golden is meant to hold to
            // account, which is why captureGolden refuses to overwrite one without REGENERATE_PROPERTY.
            captureGolden(facade, "the user32 binds (WinGlassNative) - NOT from the JNI, which is gone");
            abort("golden captured to " + GOLDEN_FILE + "; a capture run verifies nothing");
        }
        assertTranscriptsEqual(loadGolden(), facade, "the golden", "the user32 binds");
    }

    /**
     * The state of the migration, asserted rather than assumed: {@code WinMenuImpl} declares no
     * native at all. The eleven menu natives went with the menu flip; {@code _initIDs}, which
     * cached the {@code notifyCommand} method id that {@code GlassMenu.cpp}'s
     * {@code HandleMenuCommand} called from the WndProc, went with the ABI 3 change set, because it could
     * only be deleted together with the {@code gwin_menu_set_callbacks} export that gives the C its
     * target now, installed from the same static block. A native reappearing here means a Java
     * caller was added without its C, or a C body was kept for a caller that no longer exists.
     */
    @Test
    public void winMenuImplDeclaresNoNatives() {
        assertEquals(List.of(), WinGlassNativeShim.nativeMethodsOf("WinMenuImpl"),
                "the natives WinMenuImpl still declares");
    }

    /**
     * The one deliberate behaviour divergence of the menus, and the only operation the golden does
     * not cover.
     * <p>
     * {@code JString} ({@code Utils.h:108-114}) did not null-check, so a {@code null} title reached
     * {@code GetStringLength(NULL)} - undefined behaviour inside the VM. A defined
     * {@code NullPointerException} replaces it. The JNI arm was <b>never</b> given a {@code null}
     * title, here or in the script: asserting the old behaviour would have meant deliberately
     * corrupting the JVM running the test suite.
     */
    @Test
    public void aNullTitleThrowsInsteadOfReachingUser32() {
        LEDGER.requireOracle(menusAvailable, () -> MENUS_REQUIRED);
        long menu = WinGlassNativeShim.menuCreate();
        assertNotEquals(0L, menu);
        LEDGER.compared();
        try {
            assertThrows(NullPointerException.class,
                    () -> WinGlassNativeShim.menuInsertItem(menu, 0, 1, null, true, false));
            assertThrows(NullPointerException.class,
                    () -> WinGlassNativeShim.menuSetItemTitle(menu, 1, null));
            assertEquals(0, WinGlassNativeShim.menuItemCount(menu),
                    "nothing may have been inserted before the throw");
        } finally {
            WinGlassNativeShim.menuDestroy(menu);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The operation script
    // ---------------------------------------------------------------------------------------------

    /**
     * Every operation of {@code GlassMenu.cpp}, in a fixed order, over a menu bar with three
     * submenus, a nested submenu, separators, enable/disable, check/uncheck and rename - plus the
     * "not a menu" and "no such command id" arm of each. Positions are chosen so that no step's
     * validity depends on whether an earlier step succeeded, which keeps the transcript comparable
     * even when Windows refuses something.
     */
    private static List<String> runScript(MenuApi api) {
        Transcript log = new Transcript(api);
        try {
            long bar = log.create("bar");
            long file = log.create("file");
            long edit = log.create("edit");
            long view = log.create("view");
            long recent = log.create("recent");

            // The bar. Three submenus, the third inserted disabled.
            log.insertSubmenu("bar", bar, 0, "file", file, "File", true);
            log.insertSubmenu("bar", bar, 1, "edit", edit, "Edit", true);
            log.insertSubmenu("bar", bar, 2, "view", view, "View", false);
            log.insertSubmenu("bar", bar, 3, "zero", 0, "Nothing", true);
            log.insertSubmenu("bar", bar, 3, "bogus", BOGUS_HANDLE, "Bogus", true);
            log.dump("bar", bar);

            // Items, covering MFS_GRAYED on insert and MFS_CHECKED on insert.
            log.insertItem("file", file, 0, 101, "New", true, false);
            log.insertItem("file", file, 1, 102, "Open", false, false);
            log.insertItem("file", file, 2, 103, "Auto-save", true, true);
            log.insertItem("file", file, 3, 104, "Exit", true, false);
            log.dump("file", file);

            // Separators. InsertMenuW is called without MF_BYPOSITION, so the position argument is
            // read as a command id: 3 is not one, 104 is.
            log.insertSeparator("file", file, 3);
            log.dump("file", file);
            log.insertSeparator("file", file, 104);
            log.dump("file", file);

            // A nested submenu, the path a real menu bar uses for "Recent".
            log.insertSubmenu("edit", edit, 0, "recent", recent, "Recent", true);
            log.insertItem("recent", recent, 0, 201, "One", true, false);
            log.insertItem("recent", recent, 1, 202, "Two", true, false);
            log.dump("edit", edit);
            log.dump("recent", recent);

            // Renaming a submenu: by position, and only when the position is > 0.
            log.setSubmenuTitle("bar", bar, "file", file, "Fichier");
            log.setSubmenuTitle("bar", bar, "edit", edit, "Edition");
            log.setSubmenuTitle("bar", bar, "recent", recent, "NotAChildOfTheBar");
            log.setSubmenuTitle("bar", bar, "zero", 0, "NullSubmenu");
            log.dump("bar", bar);

            // Enabling a submenu: same position rule, and MF_GRAYED (1), not MFS_GRAYED (3).
            log.enableSubmenu("bar", bar, "file", file, false);
            log.enableSubmenu("bar", bar, "view", view, true);
            log.enableSubmenu("bar", bar, "view", view, false);
            log.dump("bar", bar);

            // Enabling an item: EnableMenuItem returns BOOL, so an absent id really does fail.
            log.enableItem("file", file, 102, true);
            log.enableItem("file", file, ABSENT_COMMAND_ID, false);
            log.dump("file", file);

            // Checking an item: CheckMenuItem returns DWORD, so an absent id reports success.
            log.checkItem("file", file, 103, false);
            log.checkItem("file", file, 103, true);
            log.checkItem("file", file, ABSENT_COMMAND_ID, true);
            log.dump("file", file);

            // Item titles, addressed by command id. Includes a surrogate pair and a long title, to
            // pin that cch counts UTF-16 code units and that nothing goes through modified UTF-8.
            log.setItemTitle("file", file, 101, "Neu");
            log.setItemTitle("file", file, ABSENT_COMMAND_ID, "Nowhere");
            log.setItemTitle("file", file, 104, "Exit \uD83D\uDE00 \u00E9\u00DF");
            log.setItemTitle("file", file, 102, "O".repeat(300));
            log.dump("file", file);

            // Removal, and the proof that a removed submenu's HMENU survives.
            log.removeAtPos("edit", edit, 0);
            log.removeAtPos("edit", edit, 99);
            log.dump("edit", edit);
            log.note("recent still a menu after removeAtPos: "
                    + WinGlassNativeShim.menuIsMenu(recent));

            // Every operation against a handle that is not a menu: silent false, no exception.
            log.insertItem("zero", 0, 0, 1, "X", true, false);
            log.insertSubmenu("zero", 0, 0, "file", file, "X", true);
            log.insertSeparator("zero", 0, 0);
            log.removeAtPos("zero", 0, 0);
            log.setItemTitle("zero", 0, 1, "X");
            log.setSubmenuTitle("zero", 0, "file", file, "X");
            log.enableItem("zero", 0, 1, true);
            log.enableSubmenu("zero", 0, "file", file, true);
            log.checkItem("zero", 0, 1, true);
            log.destroy("zero", 0);
            log.insertItem("bogus", BOGUS_HANDLE, 0, 1, "X", true, false);
            log.enableItem("bogus", BOGUS_HANDLE, 1, true);
            log.checkItem("bogus", BOGUS_HANDLE, 1, true);
            log.destroy("bogus", BOGUS_HANDLE);

            // Destroying the bar detaches its children first, so each child outlives it.
            log.destroy("bar", bar);
            log.note("after destroy(bar): bar=" + WinGlassNativeShim.menuIsMenu(bar)
                    + " file=" + WinGlassNativeShim.menuIsMenu(file)
                    + " edit=" + WinGlassNativeShim.menuIsMenu(edit)
                    + " view=" + WinGlassNativeShim.menuIsMenu(view)
                    + " recent=" + WinGlassNativeShim.menuIsMenu(recent));
            log.dump("file", file);

            // Destroying a menu that has a child detaches that child too.
            log.destroy("edit", edit);
            log.note("after destroy(edit): edit=" + WinGlassNativeShim.menuIsMenu(edit)
                    + " recent=" + WinGlassNativeShim.menuIsMenu(recent));

            log.destroy("file", file);
            log.destroy("view", view);
            log.destroy("recent", recent);
            log.note("all destroyed: file=" + WinGlassNativeShim.menuIsMenu(file)
                    + " view=" + WinGlassNativeShim.menuIsMenu(view)
                    + " recent=" + WinGlassNativeShim.menuIsMenu(recent));
            return log.lines();
        } finally {
            log.destroyAnythingLeft();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Anti-vacuity
    // ---------------------------------------------------------------------------------------------

    /**
     * Two transcripts of nothing are equal, and so are two transcripts of nothing but {@code false}.
     * Neither may pass. This asserts that the script really built the menu it describes: five
     * distinct handles, both outcomes present in quantity, the submenu linkage actually read back,
     * and the four states the quirks turn on.
     */
    private static void assertNotVacuous(List<String> transcript, String arm) {
        assertTrue(transcript.size() >= 70,
                () -> arm + ": a transcript of " + transcript.size() + " lines cannot be the whole"
                        + " script; something aborted it");
        long successes = transcript.stream().filter(line -> line.endsWith("-> true")).count();
        long failures = transcript.stream().filter(line -> line.endsWith("-> false")).count();
        assertTrue(successes >= 15,
                () -> arm + ": only " + successes + " operations reported success, so the menu was"
                        + " never really built - every following comparison would be vacuous");
        assertTrue(failures >= 10,
                () -> arm + ": only " + failures + " operations reported failure, so the 'not a menu'"
                        + " and 'no such command id' arms did not run");
        assertContains(transcript, arm, "title='File'");
        assertContains(transcript, arm, "title='Edition'");
        assertAbsent(transcript, arm, "title='Fichier'");
        assertContains(transcript, arm, "sub=file");
        assertContains(transcript, arm, "sub=recent");
        assertContains(transcript, arm, String.format("state=0x%04x", WINUSER_MFS_GRAYED));
        assertContains(transcript, arm, String.format("state=0x%04x", WINUSER_MFS_CHECKED));
        // Both grayed encodings have to be observable, or the disabled state is untested: an item inserted disabled
        // carries MFS_GRAYED (3, which is MF_GRAYED | MF_DISABLED) because that is what
        // GlassMenu.cpp:172 wrote into MENUITEMINFOW.fState, and a submenu disabled afterwards
        // carries MF_GRAYED (1) because that is what GlassMenu.cpp:346 passed to EnableMenuItem.
        // Windows stores them verbatim and does not normalise one into the other.
        assertContains(transcript, arm, String.format("state=0x%04x", WINUSER_MF_GRAYED));
        assertContains(transcript, arm, String.format("type=0x%04x", WINUSER_MFT_SEPARATOR));
        assertContains(transcript, arm, "cch=300");
        assertContains(transcript, arm, "\\ud83d\\ude00");
    }

    private static void assertContains(List<String> transcript, String arm, String fragment) {
        assertTrue(transcript.stream().anyMatch(line -> line.contains(fragment)),
                () -> arm + ": no line of the transcript contains \"" + fragment + "\", so the state"
                        + " it stands for was never observed and the comparison would prove nothing."
                        + "\n" + String.join("\n", transcript));
    }

    private static void assertAbsent(List<String> transcript, String arm, String fragment) {
        assertTrue(transcript.stream().noneMatch(line -> line.contains(fragment)),
                () -> arm + ": \"" + fragment + "\" appears in the transcript. GlassMenu.cpp:298"
                        + " tests \"pos > 0\" while FindItemBySubmenu returns 0 for the first item, so"
                        + " the first submenu of a bar cannot be renamed. If it now can, the port"
                        + " fixed a bug inside a migration and is no longer behaviour-neutral."
                        + "\n" + String.join("\n", transcript));
    }

    private static void assertTranscriptsEqual(List<String> expected, List<String> actual,
                                               String expectedName, String actualName) {
        LEDGER.compared(expected.size());
        if (expected.equals(actual)) {
            return;
        }
        StringBuilder report = new StringBuilder(1024);
        report.append(expectedName).append(" and ").append(actualName)
                .append(" disagree. Every difference:\n");
        int lines = Math.max(expected.size(), actual.size());
        for (int i = 0; i < lines; i++) {
            String left = i < expected.size() ? expected.get(i) : "<absent>";
            String right = i < actual.size() ? actual.get(i) : "<absent>";
            if (!left.equals(right)) {
                report.append("  line ").append(i + 1).append('\n')
                        .append("    ").append(expectedName).append(": ").append(left).append('\n')
                        .append("    ").append(actualName).append(": ").append(right).append('\n');
            }
        }
        fail(report.toString());
    }

    // ---------------------------------------------------------------------------------------------
    // The golden file
    // ---------------------------------------------------------------------------------------------

    private static List<String> loadGolden() throws IOException {
        try (InputStream stream = WinMenuNativeTest.class.getResourceAsStream(GOLDEN_FILE)) {
            if (stream == null) {
                throw new AssertionError("no " + GOLDEN_FILE + " on the classpath. It is the record"
                        + " of what GlassMenu.cpp's JNI bodies did; capture it with \""
                        + captureCommand() + "\" while those bodies still exist, and commit it."
                        + " An absent golden is a capture that was never run, never a reason to skip.");
            }
            List<String> lines = new ArrayList<>();
            for (String line : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                String trimmed = line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    lines.add(trimmed);
                }
            }
            return lines;
        }
    }

    private static void captureGolden(List<String> transcript, String source) throws IOException {
        Path module = moduleDirectory();
        Path golden = module.resolve(RESOURCE_DIR).resolve(GOLDEN_FILE);
        if (Files.exists(golden) && !Boolean.getBoolean(REGENERATE_PROPERTY)) {
            fail("a golden already exists at " + golden + ". It records what the JNI menu bodies did;"
                    + " overwriting it changes what this test proves. If that is intended, re-run with"
                    + " -D" + REGENERATE_PROPERTY + "=true and review the diff as a behaviour change,"
                    + " not as a test fix.");
        }
        StringBuilder text = new StringBuilder(8192);
        text.append("# glass/win menu parity golden: the transcript of WinMenuNativeTest's operation")
                .append(" script.\n")
                .append("# Captured from ").append(source)
                .append(" on ").append(System.getProperty("os.name")).append(' ')
                .append(System.getProperty("os.version")).append(".\n")
                .append("# Lines beginning with # are ignored by the comparison.\n");
        for (String line : transcript) {
            text.append(line).append('\n');
        }
        Files.createDirectories(golden.getParent());
        Files.writeString(golden, text.toString(), StandardCharsets.UTF_8);
        Path classes = module.resolve(CLASSES_DIR).resolve(GOLDEN_FILE);
        Files.createDirectories(classes.getParent());
        Files.writeString(classes, text.toString(), StandardCharsets.UTF_8);
        System.out.println("captured " + transcript.size() + " transcript lines to " + golden);
    }

    private static String captureCommand() {
        return "mvn -pl modules/javafx.graphics test -DskipNative=true -Dtest="
                + WinMenuNativeTest.class.getSimpleName() + " -D" + CAPTURE_PROPERTY + "=true";
    }

    private static Path moduleDirectory() {
        Path directory = Path.of("").toAbsolutePath();
        if (!Files.isRegularFile(directory.resolve("pom.xml"))
                || !Files.isDirectory(directory.resolve("src/test/resources"))) {
            fail("expected the working directory to be modules/javafx.graphics but it is " + directory
                    + ", so the golden would be written somewhere unexpected");
        }
        return directory;
    }

    // ---------------------------------------------------------------------------------------------
    // The facade, behind the interface both arms shared
    // ---------------------------------------------------------------------------------------------

    /** The eleven operations, as the JNI declared them and as the facade now offers them. */
    private interface MenuApi {
        long create();

        void destroy(long hMenu);

        boolean insertItem(long hMenu, int pos, int cmdID, String title, boolean enabled,
                           boolean checked);

        boolean insertSubmenu(long hMenu, int pos, long hSubmenu, String title, boolean enabled);

        boolean insertSeparator(long hMenu, int pos);

        boolean removeAtPos(long hMenu, int pos);

        boolean setItemTitle(long hMenu, int cmdID, String title);

        boolean setSubmenuTitle(long hMenu, long hSubmenu, String title);

        boolean enableItem(long hMenu, int cmdID, boolean enable);

        boolean enableSubmenu(long hMenu, long hSubmenu, boolean enable);

        boolean checkItem(long hMenu, int cmdID, boolean check);
    }

    /** {@code WinGlassNative}, that is {@code user32} bound directly from Java. */
    private static final MenuApi FACADE = new MenuApi() {
        @Override public long create() {
            return WinGlassNativeShim.menuCreate();
        }

        @Override public void destroy(long hMenu) {
            WinGlassNativeShim.menuDestroy(hMenu);
        }

        @Override public boolean insertItem(long hMenu, int pos, int cmdID, String title,
                                            boolean enabled, boolean checked) {
            return WinGlassNativeShim.menuInsertItem(hMenu, pos, cmdID, title, enabled, checked);
        }

        @Override public boolean insertSubmenu(long hMenu, int pos, long hSubmenu, String title,
                                               boolean enabled) {
            return WinGlassNativeShim.menuInsertSubmenu(hMenu, pos, hSubmenu, title, enabled);
        }

        @Override public boolean insertSeparator(long hMenu, int pos) {
            return WinGlassNativeShim.menuInsertSeparator(hMenu, pos);
        }

        @Override public boolean removeAtPos(long hMenu, int pos) {
            return WinGlassNativeShim.menuRemoveAtPos(hMenu, pos);
        }

        @Override public boolean setItemTitle(long hMenu, int cmdID, String title) {
            return WinGlassNativeShim.menuSetItemTitle(hMenu, cmdID, title);
        }

        @Override public boolean setSubmenuTitle(long hMenu, long hSubmenu, String title) {
            return WinGlassNativeShim.menuSetSubmenuTitle(hMenu, hSubmenu, title);
        }

        @Override public boolean enableItem(long hMenu, int cmdID, boolean enable) {
            return WinGlassNativeShim.menuEnableItem(hMenu, cmdID, enable);
        }

        @Override public boolean enableSubmenu(long hMenu, long hSubmenu, boolean enable) {
            return WinGlassNativeShim.menuEnableSubmenu(hMenu, hSubmenu, enable);
        }

        @Override public boolean checkItem(long hMenu, int cmdID, boolean check) {
            return WinGlassNativeShim.menuCheckItem(hMenu, cmdID, check);
        }
    };

    // ---------------------------------------------------------------------------------------------
    // The transcript
    // ---------------------------------------------------------------------------------------------

    /**
     * Records what was asked and what Windows made of it, in a form that has no machine-specific
     * content: handles appear only as the names this script gave them, and every title is escaped to
     * ASCII, so the transcript is byte-identical on any machine that behaves the same.
     */
    private static final class Transcript {

        private final MenuApi api;
        private final List<String> lines = new ArrayList<>();
        private final Map<Long, String> names = new LinkedHashMap<>();
        private int step;

        Transcript(MenuApi api) {
            this.api = api;
            names.put(0L, "zero");
            names.put(BOGUS_HANDLE, "bogus");
        }

        List<String> lines() {
            return List.copyOf(lines);
        }

        long create(String name) {
            long handle = api.create();
            step(name + " = create() -> " + (handle != 0 ? "a menu" : "NULL"));
            if (handle != 0) {
                names.put(handle, name);
            }
            return handle;
        }

        void destroy(String name, long hMenu) {
            api.destroy(hMenu);
            step("destroy(" + name + ")");
        }

        void insertItem(String name, long hMenu, int pos, int cmdID, String title, boolean enabled,
                        boolean checked) {
            boolean result = api.insertItem(hMenu, pos, cmdID, title, enabled, checked);
            step("insertItem(" + name + ", pos=" + pos + ", id=" + cmdID + ", " + quote(title)
                    + ", enabled=" + enabled + ", checked=" + checked + ") -> " + result);
        }

        void insertSubmenu(String name, long hMenu, int pos, String subName, long hSubmenu,
                           String title, boolean enabled) {
            boolean result = api.insertSubmenu(hMenu, pos, hSubmenu, title, enabled);
            step("insertSubmenu(" + name + ", pos=" + pos + ", sub=" + subName + ", " + quote(title)
                    + ", enabled=" + enabled + ") -> " + result);
        }

        void insertSeparator(String name, long hMenu, int pos) {
            boolean result = api.insertSeparator(hMenu, pos);
            step("insertSeparator(" + name + ", pos=" + pos + ") -> " + result);
        }

        void removeAtPos(String name, long hMenu, int pos) {
            boolean result = api.removeAtPos(hMenu, pos);
            step("removeAtPos(" + name + ", pos=" + pos + ") -> " + result);
        }

        void setItemTitle(String name, long hMenu, int cmdID, String title) {
            boolean result = api.setItemTitle(hMenu, cmdID, title);
            step("setItemTitle(" + name + ", id=" + cmdID + ", " + quote(title) + ") -> " + result);
        }

        void setSubmenuTitle(String name, long hMenu, String subName, long hSubmenu, String title) {
            boolean result = api.setSubmenuTitle(hMenu, hSubmenu, title);
            step("setSubmenuTitle(" + name + ", sub=" + subName + ", " + quote(title) + ") -> "
                    + result);
        }

        void enableItem(String name, long hMenu, int cmdID, boolean enable) {
            boolean result = api.enableItem(hMenu, cmdID, enable);
            step("enableItem(" + name + ", id=" + cmdID + ", enable=" + enable + ") -> " + result);
        }

        void enableSubmenu(String name, long hMenu, String subName, long hSubmenu, boolean enable) {
            boolean result = api.enableSubmenu(hMenu, hSubmenu, enable);
            step("enableSubmenu(" + name + ", sub=" + subName + ", enable=" + enable + ") -> "
                    + result);
        }

        void checkItem(String name, long hMenu, int cmdID, boolean check) {
            boolean result = api.checkItem(hMenu, cmdID, check);
            step("checkItem(" + name + ", id=" + cmdID + ", check=" + check + ") -> " + result);
        }

        void note(String text) {
            step(text);
        }

        /** What user32 now says the menu holds: one line per item, plus the count. */
        void dump(String name, long hMenu) {
            int count = WinGlassNativeShim.menuItemCount(hMenu);
            step("dump " + name + ": isMenu=" + WinGlassNativeShim.menuIsMenu(hMenu)
                    + " count=" + count);
            for (int pos = 0; pos < count; pos++) {
                int[] flags = WinGlassNativeShim.menuItemFlags(hMenu, pos);
                String title = WinGlassNativeShim.menuItemTitle(hMenu, pos);
                long submenu = WinGlassNativeShim.menuItemSubmenu(hMenu, pos);
                if (flags == null) {
                    lines.add("      " + name + "[" + pos + "] unreadable");
                    continue;
                }
                lines.add(String.format("      %s[%d] type=0x%04x state=0x%04x id=%d sub=%s cch=%d"
                        + " title=%s", name, pos, flags[0], flags[1], flags[2], nameOf(submenu),
                        title == null ? -1 : title.length(), quote(title)));
            }
        }

        /** A safety net for an aborted script; the script's own destroy steps come first. */
        void destroyAnythingLeft() {
            for (Map.Entry<Long, String> entry : names.entrySet()) {
                if (entry.getKey() != 0 && entry.getKey() != BOGUS_HANDLE) {
                    api.destroy(entry.getKey());
                }
            }
        }

        private String nameOf(long handle) {
            String name = names.get(handle);
            return name != null ? name : "unknown";
        }

        private void step(String text) {
            lines.add(String.format("%03d %s", ++step, text));
        }
    }

    /**
     * A title as a quoted ASCII literal: everything outside printable ASCII becomes {@code \\uXXXX},
     * so a surrogate pair is visible as two code units - which is what {@code cch} counts, and the
     * point of putting one in the script.
     */
    private static String quote(String text) {
        if (text == null) {
            return "null";
        }
        StringBuilder quoted = new StringBuilder(text.length() + 2).append('\'');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'' || c == '\\') {
                quoted.append('\\').append(c);
            } else if (c >= 0x20 && c < 0x7F) {
                quoted.append(c);
            } else {
                quoted.append(String.format("\\u%04x", (int) c));
            }
        }
        return quoted.append('\'').toString();
    }


    // ---------------------------------------------------------------------------------------------
    // The one WM_COMMAND upcall: GwinMenuCallbacks (the menus' only new C, and the last menu native)
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code GwinMenuCallbacks} against the {@code sizeof} the C compiler computed. Same reason as
     * every other {@code gwin_sizeof_*} probe: a slot appended in C without the layout following it
     * would have the facade write one pointer into a two-pointer table and leave the second holding
     * whatever the arena's allocation left there.
     */
    @Test
    public void theMenuCallbackTableHasTheSizeTheCCompilerGaveIt() {
        assertEquals(8, WinGlassNativeShim.sizeOfMenuCallbacks());
        assertEquals(8, WinGlassNativeShim.layoutByteSize("GwinMenuCallbacks"));
        assertEquals(WinGlassNativeShim.sizeOfMenuCallbacks(),
                WinGlassNativeShim.layoutByteSize("GwinMenuCallbacks"));
        assertEquals(0, WinGlassNativeShim.offset("GwinMenuCallbacks", "notify_command"));
    }

    /**
     * The table is installed by {@code WinMenuImpl}'s static initializer, at exactly the point
     * {@code _initIDs()} occupied - so touching the class at all is enough, and nothing before that
     * has a target for {@code WM_COMMAND}. Installing again is a no-op.
     */
    @Test
    public void theMenuCallbackTableIsInstalledByWinMenuImplsStaticInitializer() {
        WinGlassNativeShim.touchWinMenuImpl();
        assertTrue(WinGlassNativeShim.menuCallbackInstalled());
        WinGlassNativeShim.installMenuCallback();
        assertTrue(WinGlassNativeShim.menuCallbackInstalled());
    }

    /**
     * An id no menu item holds is <em>not handled</em>, which makes {@code GlassWindow::WindowProc}
     * fall through to {@code DefWindowProc}. {@code CommandIDManager.getHandler} answers {@code null}
     * and {@code notifyCommand} turns that into {@code false}, exactly as it did under JNI - the id
     * space is unchanged, because it never crossed into C as anything but a {@code WORD}.
     */
    @Test
    public void anUnknownCommandIdIsNotHandled() {
        assertEquals(0, WinGlassNativeShim.notifyMenuCommand(0xFFFF));
        assertEquals(0, WinGlassNativeShim.notifyMenuCommand(0));
    }

    /**
     * <b>A throwing target counts as unhandled, and the throwable is reported rather than propagated.</b>
     * This is the failure semantics of {@code CheckAndClearException} (the former {@code Utils.cpp:50-71}):
     * {@code CallStaticBooleanMethod} had already yielded {@code JNI_FALSE} before the report ran, so
     * 0 is parity and not a design choice. Letting the throwable out of the upcall stub instead would
     * terminate the JVM.
     * <p>
     * The throw is produced the only way a JVM with no Glass event thread can produce one on this
     * path: a {@code WinMenuItemDelegate} with a {@code null} owner, whose {@code getOwner()
     * .getCallback()} is a {@code NullPointerException}. A real {@code MenuItem} cannot be built here
     * because its constructor and every getter call {@code Application.checkEventThread()}.
     */
    @Test
    public void aThrowingTargetIsUnhandledAndIsReported() {
        int cmdID = WinGlassNativeShim.registerOwnerlessMenuItem();
        Thread current = Thread.currentThread();
        Thread.UncaughtExceptionHandler previous = current.getUncaughtExceptionHandler();
        AtomicReference<Throwable> reported = new AtomicReference<>();
        current.setUncaughtExceptionHandler((thread, throwable) -> reported.set(throwable));
        try {
            assertEquals(0, WinGlassNativeShim.notifyMenuCommand(cmdID),
                    "a target that throws has not handled the command");
        } finally {
            current.setUncaughtExceptionHandler(previous);
            WinGlassNativeShim.freeMenuCommandID(cmdID);
        }
        assertNotNull(reported.get(), "the throwable must reach Application.reportException");
        assertInstanceOf(NullPointerException.class, reported.get());
    }

}
