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

package test.com.sun.glass.ui.gtk;

import com.sun.glass.ui.CommonDialogs;
import com.sun.glass.ui.CommonDialogs.ExtensionFilter;
import com.sun.glass.ui.CommonDialogs.FileChooserResult;
import com.sun.glass.ui.gtk.GtkGlassShim;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code GtkCommonDialogs} on a running toolkit, as {@code GlassCommonDialogs.cpp} answered it at commit
 * {@code 033187ad90}, pinned so that the {@code java.lang.foreign} replacement can be held to it.
 * <p>
 * {@code gtk_native_dialog_run} blocks the FX thread in a nested main loop, so the dialog is driven from a GLib
 * timeout source that the same thread dispatches inside that loop: the source finds the mapped
 * {@code GtkFileChooserDialog} among GTK's toplevels, reads back its title, its current folder and the names of
 * its filters, optionally types a file name into it or selects files in it, and ends it with a response. A
 * chooser loads its browse folder asynchronously, so the source keeps looking until the folder is there. Nothing
 * here needs a window manager, a robot or keyboard focus.
 * <ul>
 * <li>the title, the initial folder and the filter names reach the dialog in the order they were given;</li>
 * <li>a cancelled dialog answers an empty file array and the index of the filter that was selected;</li>
 * <li>an accepted dialog answers the typed name under the initial folder;</li>
 * <li>a cancelled folder chooser answers {@code null};</li>
 * <li>an empty filter array adds no filter, and the cancelled dialog answers no filter either;</li>
 * <li>the title and the filter description reach GTK as modified UTF-8, byte for byte, and a multiple-selection
 * dialog answers every chosen file - including one named outside ASCII - decoded with the default charset;</li>
 * <li>a null {@code ExtensionFilter[]} throws before the dialog is ever run.</li>
 * </ul>
 * It runs in {@link GtkGlassChild} on the X11 display of {@code DISPLAY}.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(300)
public class GtkCommonDialogsNativeTest {

    /** {@code GTK_RESPONSE_CANCEL} and {@code GTK_RESPONSE_ACCEPT} of {@code gtkdialog.h}. */
    static final int GTK_RESPONSE_CANCEL = -6;

    static final int GTK_RESPONSE_ACCEPT = -3;

    static final String TITLE = "JFX file chooser probe";

    static final String FOLDER = "/tmp";

    static final String TYPED_NAME = "jfx-chosen-file.txt";

    static final String FIRST_FILTER = "Text files";

    static final String SECOND_FILTER = "All files";

    /** A title with a supplementary character, which the two UTF-8 flavours encode differently. */
    static final String UNICODE_TITLE = "JFX " + (char) 0xD83D + (char) 0xDE00 + " probe";

    /**
     * The modified UTF-8 of {@link #UNICODE_TITLE}, which is what {@code GetStringUTFChars} gave GTK: the
     * supplementary character as its two surrogates, three bytes each. Standard UTF-8 would be {@code f09f9880}.
     */
    static final String UNICODE_TITLE_HEX = "4a465820eda0bdedb8802070726f6265";

    /** A filter description with a two-byte and a supplementary character. */
    static final String UNICODE_FILTER = "Filtre " + (char) 0xE9 + " " + (char) 0xD83D + (char) 0xDE00;

    /** The modified UTF-8 of {@link #UNICODE_FILTER}; standard UTF-8 would end in {@code f09f9880}. */
    static final String UNICODE_FILTER_HEX = "46696c74726520c3a920eda0bdedb880";

    /** The name of the second file the multiple-selection case creates and chooses. */
    static final String UNICODE_FILE = "jfx-" + (char) 0xE9 + "-" + (char) 0xD83D + (char) 0xDE00 + ".txt";

    /** How long the probe waits before it looks at the dialog, and how often it looks again. */
    static final int FIRST_PROBE_MILLIS = 1200;

    static final int RETRY_PROBE_MILLIS = 200;

    static final int PROBE_ATTEMPTS = 40;

    /** {@code ExtensionFilter}'s constructor checks the event thread, so the list is built there. */
    static List<ExtensionFilter> filters() {
        return List.of(new ExtensionFilter(FIRST_FILTER, List.of("*.txt", "*.text")),
                new ExtensionFilter(SECOND_FILTER, List.of("*.*")));
    }

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() {
        GtkGlassChildJvm.requireDisplay();
        run = GtkGlassChildJvm.run(GtkCommonDialogsNativeTest.class, "dialogScenario", List.of());
    }

    private static String value(String key) {
        String result = run.values().get(key);
        if (result == null && !run.values().containsKey(key)) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return result;
    }

    /** The title, the folder and the filter names reach the dialog as they were given. */
    @Test
    public void theDialogCarriesTheTitleFolderAndFilters() {
        assertEquals(TITLE + "|" + FOLDER + "|" + FIRST_FILTER + "|" + SECOND_FILTER, value("open.dialog"));
    }

    /** A cancelled open dialog answers no file and the filter that was selected. */
    @Test
    public void aCancelledOpenAnswersNoFile() {
        assertEquals("0", value("open.fileCount"));
        assertEquals(SECOND_FILTER, value("open.filter"));
    }

    /** An accepted save dialog answers the typed name under the folder it started in. */
    @Test
    public void anAcceptedSaveAnswersTheTypedName() {
        assertEquals(TITLE + "|" + FOLDER + "|" + FIRST_FILTER + "|" + SECOND_FILTER, value("save.dialog"));
        assertEquals("1", value("save.fileCount"));
        assertEquals(FOLDER + "/" + TYPED_NAME, value("save.file"));
    }

    /** A cancelled folder chooser answers null, and offers its title and folder but no filter. */
    @Test
    public void aCancelledFolderChooserAnswersNull() {
        assertEquals(TITLE + "|" + FOLDER, value("folder.dialog"));
        assertEquals("null", value("folder.result"));
    }

    /**
     * The title and the filter description reach GTK as the modified UTF-8 of {@code GetStringUTFChars}: a
     * supplementary character as its two surrogates in three bytes each, not as the four bytes of standard UTF-8.
     */
    @Test
    public void theStringsReachGtkAsModifiedUtf8() {
        assertEquals(UNICODE_TITLE_HEX + "|" + UNICODE_FILTER_HEX, value("multi.hex"));
    }

    /**
     * A multiple-selection dialog answers every file that was selected, decoded with the default charset - the
     * file name with characters outside ASCII included, which the modified UTF-8 of the title would not decode.
     */
    @Test
    public void aMultipleSelectionAnswersEveryChosenFile() {
        assertEquals("2", value("multi.selected"), "the probe could not select both files");
        assertEquals("2", value("multi.fileCount"));
        assertEquals(value("multi.created"), value("multi.files"));
        assertEquals(HexFormat.of().formatHex(UNICODE_FILE.getBytes(StandardCharsets.UTF_8)),
                value("multi.createdHex"),
                "this machine cannot name a file outside ASCII (sun.jnu.encoding " + value("multi.jnuEncoding")
                        + "), so the chosen name did not exercise the decoder");
    }

    /**
     * An empty {@code ExtensionFilter[]} adds no filter to the dialog, as it added none in the C, and the
     * cancelled dialog answers no file and no filter - the index of a {@code NULL} filter list is -1.
     */
    @Test
    public void anEmptyFilterArrayAddsNoFilter() {
        assertEquals(TITLE + "|" + FOLDER, value("empty.dialog"));
        assertEquals("0", value("empty.fileCount"));
        assertEquals("null", value("empty.filter"));
    }

    /**
     * A null {@code ExtensionFilter[]} - what {@code CommonDialogs.showFileChooser} hands on when its caller
     * passes null filters - throws a {@code NullPointerException}. It is thrown where the C called
     * {@code GetArrayLength} on that null array and took the process down with it, which is after the
     * {@code GtkFileChooserNative} exists and before it is run: no probe is armed for this case, so a dialog that
     * did run would block the FX thread until the child JVM was killed and none of these values would exist.
     */
    @Test
    public void aNullFilterArrayThrowsBeforeTheDialogRuns() {
        assertEquals(NullPointerException.class.getName(), value("nullFilters.outcome"));
    }

    /** The probe never had to give up waiting for the chooser to load its folder. */
    @Test
    public void everyDialogReportedItsFolder() {
        for (String key : List.of("open", "save", "folder", "empty", "multi")) {
            assertTrue(Integer.parseInt(value(key + ".attempts")) < PROBE_ATTEMPTS,
                    key + " never reported a current folder: " + value(key + ".dialog"));
        }
    }

    /** Runs in {@link GtkGlassChild}. */
    static void dialogScenario(Map<String, String> out) throws Exception {
        GtkGlassChild.onFx(() -> {
            armProbe(out, "open", GTK_RESPONSE_CANCEL, null, null, 0);
            FileChooserResult result = CommonDialogs.showFileChooser(null, new File(FOLDER), null, TITLE,
                    CommonDialogs.Type.OPEN, false, filters(), 1);
            out.put("open.fileCount", result == null ? "null" : Integer.toString(result.getFiles().size()));
            out.put("open.filter", result == null || result.getExtensionFilter() == null ? "null"
                    : result.getExtensionFilter().getDescription());
            return null;
        });

        GtkGlassChild.onFx(() -> {
            armProbe(out, "save", GTK_RESPONSE_ACCEPT, TYPED_NAME, null, 0);
            FileChooserResult result = CommonDialogs.showFileChooser(null, new File(FOLDER), "start.txt", TITLE,
                    CommonDialogs.Type.SAVE, false, filters(), 0);
            out.put("save.fileCount", result == null ? "null" : Integer.toString(result.getFiles().size()));
            out.put("save.file", result == null || result.getFiles().isEmpty() ? "null"
                    : result.getFiles().get(0).getPath());
            return null;
        });

        GtkGlassChild.onFx(() -> {
            armProbe(out, "folder", GTK_RESPONSE_CANCEL, null, null, 0);
            File result = CommonDialogs.showFolderChooser(null, new File(FOLDER), TITLE);
            out.put("folder.result", String.valueOf(result));
            return null;
        });

        GtkGlassChild.onFx(() -> {
            armProbe(out, "empty", GTK_RESPONSE_CANCEL, null, null, 0);
            FileChooserResult result = CommonDialogs.showFileChooser(null, new File(FOLDER), null, TITLE,
                    CommonDialogs.Type.OPEN, false, List.of(), 0);
            out.put("empty.fileCount", result == null ? "null" : Integer.toString(result.getFiles().size()));
            out.put("empty.filter", result == null || result.getExtensionFilter() == null ? "null"
                    : result.getExtensionFilter().getDescription());
            return null;
        });

        multipleSelectionScenario(out);

        GtkGlassChild.onFx(() -> {
            String outcome;
            try {
                outcome = "returned " + CommonDialogs.showFileChooser(null, new File(FOLDER), null, TITLE,
                        CommonDialogs.Type.OPEN, false, null, 0);
            } catch (Throwable t) {
                outcome = t.getClass().getName();
            }
            out.put("nullFilters.outcome", outcome);
            return null;
        });
    }

    /**
     * A second open dialog, with a title and a filter description carrying a supplementary character, multiple
     * selection on and two existing files - one of them named with characters outside ASCII - chosen from inside
     * the dialog. It pins the two string codecs of the C at byte level and the {@code > 1} branch of the
     * {@code gtk_file_chooser_get_filenames} walk.
     */
    private static void multipleSelectionScenario(Map<String, String> out) throws Exception {
        Path directory = Files.createTempDirectory("jfx-dialog-");
        Path first = Files.createFile(directory.resolve("jfx-plain-file.txt"));
        Path second = Files.createFile(directory.resolve(UNICODE_FILE));
        out.put("multi.charset", Charset.defaultCharset().name());
        out.put("multi.jnuEncoding", String.valueOf(System.getProperty("sun.jnu.encoding")));
        out.put("multi.created", join(List.of(first.toString(), second.toString())));
        out.put("multi.createdHex", HexFormat.of().formatHex(
                second.getFileName().toString().getBytes(StandardCharsets.UTF_8)));
        try {
            GtkGlassChild.onFx(() -> {
                armProbe(out, "multi", GTK_RESPONSE_ACCEPT, null,
                        List.of(first.toString(), second.toString()), 0);
                FileChooserResult result = CommonDialogs.showFileChooser(null, directory.toFile(), null,
                        UNICODE_TITLE, CommonDialogs.Type.OPEN, true,
                        List.of(new ExtensionFilter(UNICODE_FILTER, List.of("*.txt"))), 0);
                out.put("multi.fileCount", result == null ? "null" : Integer.toString(result.getFiles().size()));
                out.put("multi.files", result == null ? "null"
                        : join(result.getFiles().stream().map(File::getPath).toList()));
                return null;
            });
        } finally {
            Files.deleteIfExists(first);
            Files.deleteIfExists(second);
            Files.deleteIfExists(directory);
        }
    }

    /** The recorded paths, sorted so that the order the chooser answers them in does not matter. */
    private static String join(List<String> paths) {
        return String.join("|", paths.stream().sorted().toList());
    }

    /**
     * Arms a GLib timeout source that runs inside the nested main loop of the dialog about to be shown: it waits
     * for the chooser to have loaded its browse folder - GTK loads it asynchronously, so one snapshot at a fixed
     * delay is a race - then records what the dialog offers, selects {@code select} if it is not {@code null} and
     * ends the dialog with {@code response}.
     */
    private static void armProbe(Map<String, String> out, String key, int response, String typedName,
                                 List<String> select, int attempt) {
        GtkGlassShim.addGlibTimeout(0, attempt == 0 ? FIRST_PROBE_MILLIS : RETRY_PROBE_MILLIS, () -> {
            String state = GtkGlassShim.fileChooserState();
            boolean ready = folderLoaded(state);
            int selected = 0;
            if (ready && select != null) {
                // the file list is still filling, so a selection can take a few rounds to hold
                GtkGlassShim.selectInFileChooser(select);
                selected = GtkGlassShim.fileChooserSelectionCount();
                if (selected < select.size()) {
                    selected = Math.max(selected, GtkGlassShim.selectAllInFileChooser());
                }
                out.put(key + ".selectMultiple", Integer.toString(GtkGlassShim.fileChooserSelectMultiple()));
                ready = selected >= select.size();
            }
            if (!ready && attempt < PROBE_ATTEMPTS) {
                armProbe(out, key, response, typedName, select, attempt + 1);
                return;
            }
            out.put(key + ".attempts", Integer.toString(attempt));
            out.put(key + ".hex", GtkGlassShim.fileChooserHex());
            if (select != null) {
                out.put(key + ".selected", Integer.toString(selected));
            }
            out.put(key + ".dialog", GtkGlassShim.respondToFileChooser(response, typedName));
        });
    }

    /** Whether {@code <title>|<folder>|<filters>} already carries a current folder. */
    private static boolean folderLoaded(String state) {
        String[] fields = state == null ? new String[0] : state.split("\\|", -1);
        return fields.length > 1 && !fields[1].isEmpty();
    }
}
