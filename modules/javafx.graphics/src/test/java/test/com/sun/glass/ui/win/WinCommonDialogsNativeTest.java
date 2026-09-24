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

import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.glass.ui.win.WinGlassNativeShim;
import com.sun.glass.ui.win.WinGlassNativeShim.FilterBlock;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The common dialogs, {@code WinCommonDialogs}, on the {@code gwin_dialog_*} ABI.
 * The two dialogs block in a modal loop on the calling thread and are NOT marshalled, so neither
 * export is ever invoked here - a call would show a real dialog and wait for a human. What is
 * asserted instead is every piece of marshalling that stands between {@code showFileChooser_impl}
 * and the C, at the layer the C sees it: the {@code GwinFileFilter} block (a NULL pointer for a null
 * array against a real block for an empty one, which the C treats differently), the description /
 * extension flattening with its {@code ';'} join, the never-NULL dialog strings, and the mapping of
 * what the C answers back into a {@code FileChooserResult} - null for an unreadable result, empty for
 * cancel, the unclamped filter index otherwise. The dialogs themselves are checked by hand (for example
 * with the {@code FileChooser} page of {@code tests/manual/monkey}).
 */
@EnabledOnOs(OS.WINDOWS)
public class WinCommonDialogsNativeTest {

    @BeforeAll
    static void requireNatives() {
        WinGlassNatives.require();
        // The three lazy holders, in the order WinGlassNativeTest's exact BOUND_SYMBOLS expects them.
        WinGlassNativeShim.bindTimerSymbols();
        WinGlassNativeShim.bindCursorSymbols();
        WinGlassNativeShim.bindBrowserSymbols();
    }

    @Test
    public void theTwoDialogExportsAndTheFilterProbeResolve() {
        assertTrue(WinGlassNativeShim.resolves("glass!gwin_dialog_file"));
        assertTrue(WinGlassNativeShim.resolves("glass!gwin_dialog_folder"));
        assertTrue(WinGlassNativeShim.resolves("glass!gwin_sizeof_file_filter"));
        assertEquals(16, WinGlassNativeShim.sizeOfFileFilter());
        assertEquals(16, WinGlassNativeShim.layoutByteSize("GwinFileFilter"));
        assertEquals(0L, WinGlassNativeShim.offset("GwinFileFilter", "description"));
        assertEquals(8L, WinGlassNativeShim.offset("GwinFileFilter", "extensions"));
        assertEquals(0, WinGlassNativeShim.constant("GWIN_DIALOG_OK"));
        assertEquals(1, WinGlassNativeShim.constant("GWIN_DIALOG_CANCELLED"));
        assertEquals(2, WinGlassNativeShim.constant("GWIN_DIALOG_FAILED"));
    }

    /**
     * The one distinction of the filter marshalling that is observable: a null filter array is a NULL
     * pointer, which makes the C skip the filter setup entirely; an empty array is a real, non-NULL
     * block with a count of 0, for which the C still calls {@code SetDefaultExtension(L"")} and
     * {@code SetFileTypes(0, ...)} - what stops the shell appending an extension to a typed SAVE
     * name. {@code javafx.stage.FileChooser} without filters hands down the empty array.
     */
    @Test
    public void aNullFilterArrayIsANullPointerAndAnEmptyOneIsNot() {
        FilterBlock none = WinGlassNativeShim.fileFilterBlock(null, null);
        assertTrue(none.isNull());
        FilterBlock empty = WinGlassNativeShim.fileFilterBlock(new String[0], new String[0]);
        assertFalse(empty.isNull(), "an empty array is a real block the C will read a count of 0 from");
        assertEquals(List.of(), empty.descriptions());
        assertEquals(List.of(), empty.extensions());
    }

    /**
     * The block carries each filter's two strings in order, NUL-terminated UTF-16, read back through
     * the layout's offsets; a null string becomes {@code ""} (the C's {@code JString} crashed on a
     * null, so this is hardening, not parity), and non-ASCII crosses as code units.
     */
    @Test
    public void theFilterBlockCarriesEachFiltersTwoStringsInOrder() {
        FilterBlock block = WinGlassNativeShim.fileFilterBlock(
                new String[] {"Images", null, "Tout (\u00e9)"},
                new String[] {"*.png;*.jpg", "*.txt", null});
        assertFalse(block.isNull());
        assertEquals(List.of("Images", "", "Tout (\u00e9)"), block.descriptions());
        assertEquals(List.of("*.png;*.jpg", "*.txt", ""), block.extensions());
    }

    /**
     * {@code WinCommonDialogs} flattens an {@code ExtensionFilter[]} into the descriptions and the
     * extensions joined with {@code ';'} - no leading, no trailing semicolon, the join
     * {@code ConcatJStrings} / {@code DNTString::append} did over {@code extensionsToArray()}; a
     * null array stays null (the NULL pointer), a null element becomes two empty strings.
     */
    @Test
    public void filtersAreFlattenedInOrderWithTheSemicolonJoin() {
        ExtensionFilter[] filters = WinGlassNativeShim.extensionFilters(
                new String[] {"Images", "Text", null},
                List.of(List.of("*.png", "*.jpg", "*.gif"), List.of("*.txt"), List.of()));
        assertArrayEquals(new String[] {"Images", "Text", ""}, WinGlassNativeShim.filterDescriptions(filters));
        assertArrayEquals(new String[] {"*.png;*.jpg;*.gif", "*.txt", ""},
                WinGlassNativeShim.filterExtensions(filters));
        assertNull(WinGlassNativeShim.filterDescriptions(null));
        assertNull(WinGlassNativeShim.filterExtensions(null));
        assertArrayEquals(new String[0], WinGlassNativeShim.filterDescriptions(new ExtensionFilter[0]));
    }

    /**
     * {@code folder}, {@code filename} and {@code title} are never NULL and possibly empty: the JNI
     * always allocated a {@code JString}, so {@code SetFolder} / {@code SetTitle} always ran and an FX
     * chooser with no title got an empty title bar, where a NULL would give the shell's default.
     */
    @Test
    public void theDialogStringsAreNeverNull() {
        assertEquals("", WinGlassNativeShim.dialogString(null));
        assertEquals("", WinGlassNativeShim.dialogString(""));
        assertEquals("C:\\Users", WinGlassNativeShim.dialogString("C:\\Users"));
    }

    /**
     * What comes back: a NULL file list is a {@code null} result (the JNI fed a null {@code String[]}
     * to {@code createFileChooserResult}, whose NPE was cleared into a null return); an empty list is
     * an empty result (cancel); paths become files with null entries skipped; and the filter index
     * is passed through untouched - {@code -1} (a failed {@code GetFileTypeIndex}) and an index past
     * the array both yield no filter, in range yields that filter.
     */
    @Test
    public void theResultMapsNullToNullEmptyToEmptyAndTheIndexUnclamped() {
        ExtensionFilter[] filters = WinGlassNativeShim.extensionFilters(new String[] {"A", "B"},
                List.of(List.of("*.a"), List.of("*.b")));
        assertNull(WinGlassNativeShim.fileChooserResult(null, 0, filters));
        FileChooserResult cancelled = WinGlassNativeShim.fileChooserResult(new String[0], 0, filters);
        assertNotNull(cancelled);
        assertEquals(List.of(), cancelled.getFiles());
        assertSame(filters[0], cancelled.getExtensionFilter());

        FileChooserResult chosen = WinGlassNativeShim.fileChooserResult(
                new String[] {"C:\\a.txt", null, "C:\\dir\\b.txt"}, 1, filters);
        assertEquals(List.of(new File("C:\\a.txt"), new File("C:\\dir\\b.txt")), chosen.getFiles());
        assertSame(filters[1], chosen.getExtensionFilter());
        assertNull(WinGlassNativeShim.fileChooserResult(new String[] {"C:\\a.txt"}, -1, filters).getExtensionFilter());
        assertNull(WinGlassNativeShim.fileChooserResult(new String[] {"C:\\a.txt"}, 2, filters).getExtensionFilter());
        assertNull(WinGlassNativeShim.fileChooserResult(new String[] {"C:\\a.txt"}, 0, null).getExtensionFilter());
    }

    /** {@code _initIDs} cached the six upcall ids Java does itself now; nothing is native here. */
    @Test
    public void winCommonDialogsDeclaresNoNative() {
        assertEquals(List.of(), WinGlassNativeShim.nativeMethodsOf("WinCommonDialogs"));
    }
}
