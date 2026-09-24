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

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.ClipboardAssistance;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.glass.ui.gtk.GtkGlassShim;
import com.sun.glass.ui.gtk.GtkTraceWindow;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.input.MouseButton;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drag and drop through {@code glass_dnd.cpp} and {@code GtkDnDClipboard} as the JNI build of commit
 * {@code 033187ad90} did it, driven by the glass robot: a drag from a view of one window to a view of another, in
 * both directions, in one process and between two processes on the same X11 display. The source offers text, an
 * HTML fragment, a URL, a file list, a custom mime with a {@code ByteBuffer}, an image and a drag image; the pointer
 * leaves the target once and comes back before the drop.
 * <p>
 * The traces pin, per window: the target notifications ({@code dragEnter}, {@code dragOver}, {@code dragLeave},
 * {@code dragDrop}) with their coordinates and actions, the mimes and data the target read at the drop, the source's
 * {@code dragStart}, the {@code flush} that runs the drag loop and the {@code dragEnd} with the performed action.
 * In one process the target's {@code ClipboardAssistance} answers from the source's own data (the DnD clipboard is
 * then the owner), so that scenario also reads through the peer methods ({@code mimesFromSystem},
 * {@code popFromSystem}), which fetch the drag selection through the X server as another process would. A third
 * scenario drives the exception paths of the target upcalls: a {@code dragEnter} that throws leaves the drag
 * without a status reply, and a {@code dragDrop} that throws still finishes the drop; its source offers no drag
 * image, so {@code DragView::get_drag_image} makes one from the raw image.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(600)
public class GtkDnDTraceTest {

    static final String IN_PROCESS = "gtk-dnd-in-process";
    static final String BETWEEN_PROCESSES = "gtk-dnd-between-processes";
    static final String THROWING_TARGET = "gtk-dnd-throwing-target";

    /** The directory the two child JVMs hand their markers through. */
    static final String EXCHANGE_PROPERTY = "gtk.dnd.exchange";

    /** {@code P} or {@code Q}: which of the two child JVMs this is. */
    static final String ROLE_PROPERTY = "gtk.dnd.role";

    /**
     * The normalisation: {@link GtkTraceGolden#SORT_NESTED_READS} - five runs of the in-process drag put the
     * crossing and focus events that arrive while the drop target's reads spin their nested main loops at different
     * places among those reads - and {@link GtkTraceGolden#REPAINT_LAST_IN_STEP}, the frame-clock expose, which
     * lands inside or after those reads. The two-process scenario creates its windows one after the other instead:
     * a window restacked while the other process shows its own gets an extra ConfigureNotify.
     */
    static final List<GtkTraceGolden.Rule> RULES = List.of(GtkTraceGolden.SORT_NESTED_READS,
            GtkTraceGolden.REPAINT_LAST_IN_STEP);

    /** A view's pointer {@code ENTER} as {@link GtkEventTrace} records it, inside a nested read or not. */
    private static final Pattern ENTER = Pattern.compile("(" + Pattern.quote(GtkEventTrace.NESTED_MARK)
            + ")?v([0-9a-z]+)\\.mouse ENTER .*");

    /**
     * Moves the pointer {@code ENTER} of a step that has a nested read, and the {@code isEnabled} of its
     * {@code EnterNotify}, from inside or after the nested read to the end of the step. When the drop in one process
     * ends the drag, the source's pointer ungrab makes GDK deliver an {@code ENTER} to the view under the pointer:
     * idle, it is dispatched inside the drop target's nested reads, as the golden has it; under load (the gate's
     * 8-18 on 8 cpus, on the JNI build as on the table build) it came after the drag source's loop had returned.
     * The {@code ENTER} keeps its arguments and its step; only where in the step it was dispatched is dropped. A view
     * {@code vN} is in the window {@code wN} in these scenarios. Applied to the golden too, which was captured
     * before the rule ({@link GtkTraceGolden#verify(Class, String, GtkGlassChildJvm.Run, List, List, List, List)}),
     * and idempotent.
     */
    static final GtkTraceGolden.Rule ENTER_AFTER_THE_DROP = trace -> {
        List<String> result = new ArrayList<>();
        List<String> step = new ArrayList<>();
        for (String line : trace) {
            if (line.startsWith("== ")) {
                result.addAll(enterLast(step));
                step.clear();
                result.add(line);
            } else {
                step.add(line);
            }
        }
        result.addAll(enterLast(step));
        return result;
    };

    private static List<String> enterLast(List<String> step) {
        int firstNested = -1;
        for (int i = 0; i < step.size() && firstNested < 0; i++) {
            if (step.get(i).startsWith(GtkEventTrace.NESTED_MARK)) {
                firstNested = i;
            }
        }
        if (firstNested < 0) {
            return step;
        }
        List<String> lines = new ArrayList<>(step);
        List<String> moved = new ArrayList<>();
        int i = firstNested;
        while (i < lines.size()) {
            Matcher enter = ENTER.matcher(lines.get(i));
            if (!enter.matches()) {
                i++;
                continue;
            }
            boolean nested = enter.group(1) != null;
            String isEnabled = "w" + enter.group(2) + ".isEnabled=true";
            String line = lines.remove(i).substring(nested ? GtkEventTrace.NESTED_MARK.length() : 0);
            boolean paired = false;
            if (nested) {
                int k = lines.lastIndexOf(GtkEventTrace.NESTED_MARK + isEnabled);
                if (k >= firstNested) {
                    lines.remove(k);
                    paired = true;
                    if (k < i) {
                        i--;
                    }
                }
            } else if (i > 0 && lines.get(i - 1).equals(isEnabled)) {
                lines.remove(i - 1);
                paired = true;
                i--;
            }
            if (paired) {
                moved.add(isEnabled);
            }
            moved.add(line);
        }
        lines.addAll(moved);
        return lines;
    }

    /** The in-process scenario's normalisation: {@link #RULES}, then {@link #ENTER_AFTER_THE_DROP}. */
    static final List<GtkTraceGolden.Rule> IN_PROCESS_RULES = List.of(GtkTraceGolden.SORT_NESTED_READS,
            GtkTraceGolden.REPAINT_LAST_IN_STEP, ENTER_AFTER_THE_DROP);

    static final List<String> LEGEND = List.of(
            "Lines as in the event trace; '# source ...' is what the drag source did around",
            "ClipboardAssistance.flush, '# target ...' what the drop target read through its ClipboardAssistance,",
            "'# peer ...' what it read through the GtkDnDClipboard peer methods themselves. Two-process traces are",
            "the two children's, P then Q.");

    /**
     * The mimes the target reads at the drop, in this order. {@code text/html} is read before and after
     * {@code text/plain}: a {@code text/*} mime other than {@code text/plain} is served by
     * {@code dnd_source_set_raw} through {@code gtk_selection_data_set_text}, which only takes text targets, and what
     * the target then reads depends on the transfer before it.
     */
    static final List<String> MIMES = List.of("text/html", "text/plain", "text/html", "text/uri-list",
            "application/x-java-file-list", "application/x-jfx-dnd-test", "application/x-java-rawimage");

    static final int LEFT_X = 100;
    static final int RIGHT_X = 600;
    static final int TOP = 100;
    static final int WIDTH = 300;
    static final int HEIGHT = 200;

    private static GtkGlassChildJvm.Run inProcess;
    private static GtkGlassChildJvm.Run throwing;
    private static List<GtkGlassChildJvm.Run> betweenProcesses;

    @BeforeAll
    @Timeout(20 * GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenarios() {
        GtkGlassChildJvm.requireRobot();
        GtkTraceGolden.requireNoWindowManager(GtkDnDTraceTest.class);
        List<String> options = List.of("-DUSE_ROBOT=true");
        inProcess = GtkTraceGolden.runRepeated(GtkDnDTraceTest.class, "inProcessScenario", options, IN_PROCESS,
                IN_PROCESS_RULES);
        throwing = GtkTraceGolden.runRepeated(GtkDnDTraceTest.class, "throwingTargetScenario", options,
                THROWING_TARGET, RULES);
        int repeat = Math.max(1, Integer.getInteger(GtkTraceGolden.REPEAT_PROPERTY, 1));
        for (int i = 1; i <= repeat; i++) {
            List<GtkGlassChildJvm.Run> runs = runPeers();
            GtkTraceGolden.collect(BETWEEN_PROCESSES, i, runs.get(0), combined(runs), RULES);
            if (betweenProcesses == null) {
                betweenProcesses = runs;
            }
        }
    }

    private static List<GtkGlassChildJvm.Run> runPeers() {
        Path exchange = exchangeDirectory();
        List<String> p = List.of("-DUSE_ROBOT=true", "-D" + EXCHANGE_PROPERTY + "=" + exchange,
                "-D" + ROLE_PROPERTY + "=P");
        List<String> q = List.of("-DUSE_ROBOT=true", "-D" + EXCHANGE_PROPERTY + "=" + exchange,
                "-D" + ROLE_PROPERTY + "=Q");
        return GtkGlassChildJvm.runTogether(List.of(
                GtkGlassChildJvm.start(GtkDnDTraceTest.class, "peerScenarioP", p),
                GtkGlassChildJvm.start(GtkDnDTraceTest.class, "peerScenarioQ", q)));
    }

    /** P's trace, then Q's, each headed by its name. */
    static List<String> combined(List<GtkGlassChildJvm.Run> runs) {
        List<String> lines = new ArrayList<>();
        lines.add("=== child P");
        lines.addAll(GtkTraceGolden.rawTrace(runs.get(0)));
        lines.add("=== child Q");
        lines.addAll(GtkTraceGolden.rawTrace(runs.get(1)));
        return lines;
    }

    private static Path exchangeDirectory() {
        try {
            Path exchange = Path.of("target", "gtk-glass-child", "dnd-exchange").toAbsolutePath();
            if (Files.isDirectory(exchange)) {
                try (var files = Files.list(exchange)) {
                    for (Path file : files.toList()) {
                        Files.delete(file);
                    }
                }
            }
            Files.createDirectories(exchange);
            return exchange;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @AfterAll
    static void oracleRan() {
        ParityGate.ledger(GtkDnDTraceTest.class).assertOracleRan();
    }

    @Test
    public void inProcessTraceMatchesTheJniGolden() {
        List<String> raw = GtkTraceGolden.rawTrace(inProcess);
        GtkTraceGolden.verify(GtkDnDTraceTest.class, IN_PROCESS, inProcess, raw,
                GtkTraceGolden.normalise(raw, IN_PROCESS_RULES), LEGEND, IN_PROCESS_RULES);
    }

    @Test
    public void betweenProcessesTraceMatchesTheJniGolden() {
        List<String> raw = combined(betweenProcesses);
        GtkTraceGolden.verify(GtkDnDTraceTest.class, BETWEEN_PROCESSES, betweenProcesses.get(0), raw,
                GtkTraceGolden.normalise(raw, RULES), LEGEND);
    }

    @Test
    public void throwingTargetTraceMatchesTheJniGolden() {
        List<String> raw = GtkTraceGolden.rawTrace(throwing);
        GtkTraceGolden.verify(GtkDnDTraceTest.class, THROWING_TARGET, throwing, raw,
                GtkTraceGolden.normalise(raw, RULES), LEGEND);
    }

    /**
     * The target of the other process read what the source offered, in both directions: modified UTF-8 text through
     * {@code UTF8_STRING}, the URL and the files through {@code text/uri-list}, the whole backing array of the
     * {@code ByteBuffer}, and the image as a {@code GtkPixels} of the same size - and, for the HTML fragment, not the
     * fragment but whatever the transfer before it left behind.
     */
    @Test
    public void theOtherProcessReadsWhatTheSourceOffered() {
        for (int i = 0; i < 2; i++) {
            GtkGlassChildJvm.Run target = betweenProcesses.get(i);
            String source = i == 0 ? "Q" : "P";
            Map<String, String> values = target.values();
            // text/html is never served: dnd_source_set_raw hands it to gtk_selection_data_set_text, which refuses a
            // target that is not a text target, and dnd_target_get_raw then reads the GDK_SELECTION property the
            // previous transfer left on the drop window - the URI list mimesFromSystem fetched at dragEnter, then
            // the text read just before (commit 033187ad90 behaviour, kept)
            assertEquals(escaped("file:///tmp/jfx-dnd-a.txt\r\nfile:///tmp/jfx%20dnd%20b.txt\r\n" + URL + "\r\n\r\n"),
                    values.get("drop.0.text/html"), target.describe());
            assertEquals(escaped(text(source)), values.get("drop.1.text/plain"), target.describe());
            assertEquals(escaped(text(source)), values.get("drop.2.text/html"));
            assertEquals(escaped(URL), values.get("drop.3.text/uri-list"));
            assertEquals(String.join("|", FILES), values.get("drop.4.application/x-java-file-list"));
            assertEquals(HexFormat.of().formatHex(CUSTOM_BYTES), values.get("drop.5.application/x-jfx-dnd-test"));
            assertEquals(IMAGE_WIDTH + "x" + IMAGE_HEIGHT + " " + HexFormat.of().formatHex(imageBytes()),
                    values.get("drop.6.application/x-java-rawimage"));
            assertEquals("COPY", values.get("dragEnd"), "the source's performed action");
        }
    }

    /** The target sees enter, over, leave, enter, over, drop - in that order - for the out-and-back path. */
    @Test
    public void theTargetSeesEnterOverLeaveAndDrop() {
        for (GtkGlassChildJvm.Run run : List.of(inProcess, betweenProcesses.get(0), betweenProcesses.get(1))) {
            assertEquals("dragEnter,dragOver,dragLeave,dragEnter,dragOver,dragDrop", run.values().get("targetOrder"),
                    run.describe());
        }
    }

    /** Every upcall of the drag-and-drop code arrives on the FX thread. */
    @Test
    public void everyUpcallArrivesOnTheEventThread() {
        List<String> all = new ArrayList<>(GtkTraceGolden.rawTrace(inProcess));
        all.addAll(GtkTraceGolden.rawTrace(throwing));
        all.addAll(combined(betweenProcesses));
        for (String line : all) {
            assertTrue(!line.contains("!off-fx-thread"), line);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Content
    // ---------------------------------------------------------------------------------------------

    static final String HTML = "<b>" + (char) 0xE9 + "</b>";
    static final String URL = "https://example.invalid/dnd";
    static final List<String> FILES = List.of("/tmp/jfx-dnd-a.txt", "/tmp/jfx dnd b.txt");
    static final byte[] CUSTOM_BYTES = customBytes();
    static final int IMAGE_WIDTH = 3;
    static final int IMAGE_HEIGHT = 2;

    /** Text with a non-Latin-1 character and a supplementary one, tagged with the source's name. */
    static String text(String source) {
        return "dnd from " + source + " " + (char) 0xE9 + " " + (char) 0x2603 + " " + (char) 0xD83D + (char) 0xDE00;
    }

    private static byte[] customBytes() {
        byte[] bytes = new byte[16];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (0xF0 - i * 13);
        }
        return bytes;
    }

    /** {@code IMAGE_WIDTH * IMAGE_HEIGHT} BGRA pixels, each a distinct opaque colour. */
    static byte[] imageBytes() {
        byte[] bytes = new byte[IMAGE_WIDTH * IMAGE_HEIGHT * 4];
        for (int i = 0; i < IMAGE_WIDTH * IMAGE_HEIGHT; i++) {
            bytes[i * 4] = (byte) (0x20 + i);
            bytes[i * 4 + 1] = (byte) (0x60 + i);
            bytes[i * 4 + 2] = (byte) (0xA0 + i);
            bytes[i * 4 + 3] = (byte) 0xFF;
        }
        return bytes;
    }

    /** {@code application/x-java-drag-image}: big-endian width and height, then 2x2 BGRA pixels. */
    private static ByteBuffer dragImage() {
        ByteBuffer buffer = ByteBuffer.allocate(8 + 2 * 2 * 4);
        buffer.putInt(2).putInt(2);
        for (int i = 0; i < 4; i++) {
            buffer.put((byte) (0x10 * i)).put((byte) 0x80).put((byte) (0xF0 - i)).put((byte) 0xFF);
        }
        buffer.rewind();
        return buffer;
    }

    /**
     * What the source offers; set on its {@code ClipboardAssistance} in this order. Without {@code dragImage} the
     * source offers no {@code application/x-java-drag-image}, and glass makes the drag image from the raw image.
     */
    static Map<String, Object> content(String source, boolean dragImage) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("text/plain", text(source));
        data.put("text/html", HTML);
        data.put("text/uri-list", URL);
        data.put("application/x-java-file-list", FILES.toArray(new String[0]));
        ByteBuffer custom = ByteBuffer.wrap(CUSTOM_BYTES.clone());
        custom.position(4);
        custom.limit(8);
        data.put("application/x-jfx-dnd-test", custom);
        data.put("application/x-java-rawimage", Application.GetApplication().createPixels(IMAGE_WIDTH, IMAGE_HEIGHT,
                ByteBuffer.wrap(imageBytes())));
        if (dragImage) {
            data.put("application/x-java-drag-image", dragImage());
            data.put("application/x-java-drag-image-offset", ByteBuffer.allocate(8).putInt(1).putInt(1).rewind());
        }
        return data;
    }

    static String escaped(String text) {
        return GtkEventTrace.escape(text);
    }

    /** A value read from a drag, as the trace and the values record it. */
    static String describe(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String text) {
            return escaped(text);
        }
        if (value instanceof String[] array) {
            return String.join("|", array);
        }
        if (value instanceof ByteBuffer buffer) {
            ByteBuffer copy = buffer.duplicate();
            copy.rewind();
            copy.limit(copy.capacity());
            byte[] bytes = new byte[copy.remaining()];
            copy.get(bytes);
            return HexFormat.of().formatHex(bytes);
        }
        if (value instanceof Pixels pixels) {
            ByteBuffer copy = pixels.asByteBuffer().duplicate();
            byte[] bytes = new byte[copy.remaining()];
            copy.get(bytes);
            return pixels.getWidth() + "x" + pixels.getHeight() + " " + HexFormat.of().formatHex(bytes);
        }
        return value.getClass().getName() + ":" + value;
    }

    // ---------------------------------------------------------------------------------------------
    // The drag machinery (child JVM)
    // ---------------------------------------------------------------------------------------------

    /** A view that is a drag source and a drop target, recording both sides into its trace and {@code out}. */
    static final class DndHooks extends GtkEventTrace.ViewHooks {

        private final GtkEventTrace t;
        private final String source;
        private final boolean peerReads;
        private final boolean dragImage;
        private final Map<String, String> out;
        private final List<String> targetOrder = new ArrayList<>();

        /** Whether the flush of the last drag this view started has returned; true before the first. */
        private volatile boolean flushReturned = true;

        DndHooks(GtkEventTrace t, String source, boolean peerReads, Map<String, String> out) {
            this(t, source, peerReads, true, out);
        }

        DndHooks(GtkEventTrace t, String source, boolean peerReads, boolean dragImage, Map<String, String> out) {
            this.t = t;
            this.source = source;
            this.peerReads = peerReads;
            this.dragImage = dragImage;
            this.out = out;
        }

        @Override
        int dragAction(String phase, int recommended) {
            targetOrder.add("drag" + Character.toUpperCase(phase.charAt(0)) + phase.substring(1));
            return Clipboard.ACTION_COPY;
        }

        @Override
        void dragStart(View view, ClipboardAssistance assistant) {
            for (Map.Entry<String, Object> entry : content(source, dragImage).entrySet()) {
                assistant.setData(entry.getKey(), entry.getValue());
            }
            assistant.setSupportedActions(Clipboard.ACTION_COPY | Clipboard.ACTION_MOVE);
            t.note("source flush");
            flushReturned = false;
            try {
                assistant.flush();
                t.note("source flush returned");
            } finally {
                flushReturned = true;
            }
        }

        @Override
        void dragEnd(int performedAction) {
            out.put("dragEnd", GtkEventTrace.action(performedAction));
        }

        @Override
        void dragLeave() {
            targetOrder.add("dragLeave");
        }

        @Override
        void dragEnter(ClipboardAssistance target) {
            t.beginNested();
            try {
                String[] mimes = target.getMimeTypes();
                t.note("target mimes " + (mimes == null ? "null" : String.join("|", mimes)));
                t.note("target source actions " + GtkEventTrace.action(target.getSupportedSourceActions()));
            } finally {
                t.endNested();
            }
        }

        @Override
        void dragDrop(ClipboardAssistance target) {
            // the reads spin nested main loops (cross-process, and every peer read): their notes carry the read's
            // index, because the normalisation sorts what a nested read recorded. The XSync first sends what GDK
            // still buffers - in one process, the drag source's pointer ungrab - so that the crossing and focus
            // events it causes are always dispatched inside the reads, not sometimes after them
            t.beginNested();
            try {
                GtkGlassShim.syncGdkDisplay();
                for (int i = 0; i < MIMES.size(); i++) {
                    String mime = MIMES.get(i);
                    String value = describe(target.getData(mime));
                    t.note("target data " + i + " " + mime + " = " + value);
                    out.put("drop." + i + "." + mime, value);
                }
                if (peerReads) {
                    String[] mimes = GtkGlassShim.dndMimesFromSystem();
                    t.note("peer mimes " + (mimes == null ? "null" : String.join("|", mimes)));
                    t.note("peer source actions " + GtkEventTrace.action(GtkGlassShim.dndSupportedSourceActions()));
                    t.note("peer isOwner " + GtkGlassShim.dndIsOwner());
                    for (int i = 0; i < MIMES.size(); i++) {
                        String mime = MIMES.get(i);
                        t.note("peer data " + i + " " + mime + " = " + describe(GtkGlassShim.dndPopFromSystem(mime)));
                    }
                }
            } finally {
                t.endNested();
            }
        }

        /** The target notifications this view received, in order: what the robot path must produce. */
        String targetOrder() {
            return String.join(",", targetOrder);
        }

        /** Forgets the target notifications recorded so far. */
        void resetTargetOrder() {
            targetOrder.clear();
        }
    }

    /** Creates, places and shows a window with a drag-and-drop view; FX thread for the creation. */
    static GtkTraceWindow dndWindow(GtkEventTrace t, String name, String viewName, int x, DndHooks hooks)
            throws Exception {
        GtkTraceWindow window = GtkGlassChild.onFx(() -> t.window(name, null, Window.TITLED | Window.CLOSABLE,
                viewName, hooks));
        t.act(() -> window.setBounds(x, TOP, true, true, -1, -1, WIDTH, HEIGHT, 0, 0));
        t.act(() -> window.setVisible(true));
        return window;
    }

    /**
     * Drags from the window whose content starts at {@code fromX} to the one at {@code toX}: press inside the
     * source, two moves (the first starts the drag), across the gap, into the target and over it, out of the target
     * and back, and release over it.
     */
    static void drag(GtkEventTrace t, int fromX, int toX, String label) throws Exception {
        t.step(label + ": press in the source");
        t.mouseMove(fromX + 100, TOP + 100);
        t.mousePress(MouseButton.PRIMARY);
        t.step(label + ": drag start");
        t.mouseMove(fromX + 110, TOP + 105);
        t.mouseMove(fromX + 130, TOP + 110);
        t.step(label + ": across the gap");
        int gap = (Math.max(fromX, toX) + Math.min(fromX, toX) + WIDTH) / 2;
        t.mouseMove(gap, TOP + 110);
        t.step(label + ": into the target and over it");
        t.mouseMove(toX + 50, TOP + 100);
        t.mouseMove(toX + 60, TOP + 110);
        t.step(label + ": out of the target");
        t.mouseMove(toX + 150, TOP + HEIGHT + 150);
        t.step(label + ": back into the target and over it");
        t.mouseMove(toX + 80, TOP + 120);
        t.mouseMove(toX + 90, TOP + 130);
        t.step(label + ": release over the target");
        t.mouseRelease(MouseButton.PRIMARY);
    }

    /**
     * Waits until the flush of the drag {@code source} started has returned - the drag loop, the drop and the
     * {@code dragEnd} are over - and settles, so that all of it lands in the step of the release. The release's own
     * settle ends at the first quiet quarter second, and under load the drop's reads could outlast that: the
     * {@code dragEnd} then fell into the next step.
     */
    static void awaitDragEnd(GtkEventTrace t, DndHooks source) throws Exception {
        if (!GtkGlassChild.waitFor(60_000, () -> source.flushReturned)) {
            throw new IllegalStateException("the drag source's flush did not return within 60 s");
        }
        t.settle();
    }

    /** Runs in {@link GtkGlassChild}: both windows in this process, a drag each way, peer reads at the drops. */
    static void inProcessScenario(Map<String, String> out) throws Exception {
        GtkEventTrace t = new GtkEventTrace();
        try {
            GtkTraceGolden.recordMachine(out);
            GtkGlassChild.onFx(() -> {
                t.installApplicationRecorders();
                return null;
            });
            DndHooks left = new DndHooks(t, "left", true, new LinkedHashMap<>());
            DndHooks right = new DndHooks(t, "right", true, out);
            t.step("create and show w1 (left) and w2 (right)");
            GtkTraceWindow w1 = dndWindow(t, "w1", "v1", LEFT_X, left);
            GtkTraceWindow w2 = dndWindow(t, "w2", "v2", RIGHT_X, right);
            GtkGlassChild.onFx(() -> {
                left.resetTargetOrder();
                right.resetTargetOrder();
                return null;
            });
            drag(t, LEFT_X, RIGHT_X, "left to right");
            awaitDragEnd(t, left);
            out.put("targetOrder", GtkGlassChild.onFx(right::targetOrder));
            drag(t, RIGHT_X, LEFT_X, "right to left");
            awaitDragEnd(t, right);
            t.step("close both");
            t.act(w1::close);
            t.act(w2::close);
        } finally {
            out.put(GtkTraceGolden.TRACE_KEY, String.join("\n", t.lines()));
        }
    }

    /**
     * Runs in {@link GtkGlassChild}: a drag from w1 to w2 whose target throws from its first {@code dragEnter} and
     * from its {@code dragDrop}; the source offers no drag image.
     */
    static void throwingTargetScenario(Map<String, String> out) throws Exception {
        GtkEventTrace t = new GtkEventTrace();
        try {
            GtkTraceGolden.recordMachine(out);
            GtkGlassChild.onFx(() -> {
                t.installApplicationRecorders();
                return null;
            });
            // no drag image: DragView::get_drag_image makes it from the raw image (Pixels.attachData)
            DndHooks left = new DndHooks(t, "left", false, false, new LinkedHashMap<>());
            DndHooks right = new DndHooks(t, "right", false, out);
            t.step("create and show w1 (left) and w2 (right)");
            GtkTraceWindow w1 = dndWindow(t, "w1", "v1", LEFT_X, left);
            GtkTraceWindow w2 = dndWindow(t, "w2", "v2", RIGHT_X, right);
            t.arm("v2.dragEnter", 1);
            t.arm("v2.dragDrop", 1);
            drag(t, LEFT_X, RIGHT_X, "left to right, the target throwing");
            awaitDragEnd(t, left);
            t.step("close both");
            t.act(w1::close);
            t.act(w2::close);
        } finally {
            out.put(GtkTraceGolden.TRACE_KEY, String.join("\n", t.lines()));
        }
    }

    /** Runs in {@link GtkGlassChild}: the left window; drags to Q first, then is Q's drop target. */
    static void peerScenarioP(Map<String, String> out) throws Exception {
        peerScenario(out, "P", LEFT_X, RIGHT_X);
    }

    /** Runs in {@link GtkGlassChild}: the right window; Q's drop target first, then drags to P. */
    static void peerScenarioQ(Map<String, String> out) throws Exception {
        peerScenario(out, "Q", RIGHT_X, LEFT_X);
    }

    private static void peerScenario(Map<String, String> out, String role, int ownX, int otherX) throws Exception {
        Path exchange = Path.of(System.getProperty(EXCHANGE_PROPERTY));
        GtkEventTrace t = new GtkEventTrace();
        try {
            GtkTraceGolden.recordMachine(out);
            GtkGlassChild.onFx(() -> {
                t.installApplicationRecorders();
                return null;
            });
            DndHooks hooks = new DndHooks(t, role, false, out);
            // one window after the other: a window shown while the other process's is being created is restacked,
            // and the X server reports a stacking change as a ConfigureNotify - an extra notifyResize/notifyMove
            if ("Q".equals(role)) {
                await(exchange.resolve("P.ready"));
            }
            t.step("create and show w1");
            GtkTraceWindow w1 = dndWindow(t, "w1", "v1", ownX, hooks);
            Files.writeString(exchange.resolve(role + ".ready"), "1");
            if ("P".equals(role)) {
                await(exchange.resolve("Q.ready"));
            }
            GtkGlassChild.onFx(() -> {
                hooks.resetTargetOrder();
                return null;
            });
            if ("P".equals(role)) {
                drag(t, ownX, otherX, "P drags to Q");
                awaitDragEnd(t, hooks);
                // the drag started over P's own window, which is a drop target as well: forget those notifications
                GtkGlassChild.onFx(() -> {
                    hooks.resetTargetOrder();
                    return null;
                });
                Files.writeString(exchange.resolve("phase1.done"), "1");
                await(exchange.resolve("phase2.done"));
                t.settle();
                out.put("targetOrder", GtkGlassChild.onFx(hooks::targetOrder));
            } else {
                await(exchange.resolve("phase1.done"));
                t.settle();
                out.put("targetOrder", GtkGlassChild.onFx(hooks::targetOrder));
                drag(t, ownX, otherX, "Q drags to P");
                awaitDragEnd(t, hooks);
                Files.writeString(exchange.resolve("phase2.done"), "1");
            }
            t.step("close w1");
            t.act(w1::close);
        } finally {
            out.put(GtkTraceGolden.TRACE_KEY, String.join("\n", t.lines()));
        }
    }

    /** Waits up to two minutes for the other child to create {@code marker}. */
    private static void await(Path marker) throws Exception {
        if (!GtkGlassChild.waitFor(120_000, () -> Files.isRegularFile(marker))) {
            throw new IllegalStateException("the other child JVM never created " + marker);
        }
    }
}
