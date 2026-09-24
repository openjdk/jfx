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

import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.ClipboardAssistance;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.glass.ui.gtk.GtkGlassShim;
import com.sun.glass.ui.gtk.GtkTraceWindow;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import javafx.scene.input.MouseButton;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The drop-target natives of {@code GtkDnDClipboard} - {@code mimesFromSystem}, {@code popFromSystem} and
 * {@code supportedSourceActionsFromSystem}, {@code glass_dnd.cpp} at commit {@code 033187ad90} - before, across and
 * after four drags, in a child JVM driven by the glass robot on a display without a window manager:
 * <ul>
 * <li>before any drag has entered a Glass window each of them throws the {@code IllegalStateException} of
 * {@code check_state_in_drag}, with its message - {@code popFromSystem(null)} too;</li>
 * <li>the mime list is computed once per drag enter: every call within one drag answers the same array, and the next
 * drag, whose source offers another mime, answers a new array with that mime in it;</li>
 * <li>a URI list whose file URI follows a non-file one answers that path as the one element of the file list;</li>
 * <li>once an accepted drop has finished each of them throws the {@code IllegalStateException} again;</li>
 * <li>a drag whose target throws from its enter and is released over it is rejected at the drop, and once that
 * drop has finished each of them throws the {@code IllegalStateException} again.</li>
 * </ul>
 * The peer methods are called themselves, not through {@code ClipboardAssistance}, which in one process answers from
 * the drag source's own data.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(300)
public class GtkDnDTargetTest {

    static final String MESSAGE = "Cannot get supported actions. Drag pointer haven't entered the application window";

    static final String CUSTOM_MIME = "application/x-jfx-dnd-second";

    static final String FILE_LIST_MIME = "application/x-java-file-list";

    static final String URI_LIST_MIME = "text/uri-list";

    /** The one file of {@link #MIXED_URI_LIST}; it need not exist, the URI is only parsed. */
    static final String MIXED_FILE = "/tmp/jfx-dnd-files.txt";

    static final String MIXED_URL = "http://example.invalid/x";

    /**
     * A URI list whose file URI follows a non-file one, which no {@code application/x-java-file-list} content can
     * produce - the source writes the files first - so it is offered as the raw URI list of the third drag.
     */
    static final String MIXED_URI_LIST = MIXED_URL + "\r\n" + "file://" + MIXED_FILE;

    /** Drags 1 to 3 are accepted at the drop; the fourth, {@link #REJECTED_DRAG}, is rejected there. */
    static final int DRAGS = 4;

    static final int ACCEPTED_DRAGS = 3;

    static final int REJECTED_DRAG = 4;

    static final String BEFORE = "before.";

    static final String AFTER_ACCEPTED_DROP = "afterAcceptedDrop.";

    static final String AFTER_REJECTED_DROP = "afterRejectedDrop.";

    static final List<String> NATIVES = List.of("mimesFromSystem", "popFromSystem", "popFromSystemNull",
            "supportedSourceActionsFromSystem");

    /** The two values the rejected drag's {@code dragEnd} may report; see {@link #theRejectedDragWasNotDropped}. */
    static final Set<String> REJECTED_DRAG_END_ACTIONS = Set.of("COPY", "NONE");

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(2 * GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() {
        GtkGlassChildJvm.requireRobot();
        GtkTraceGolden.requireNoWindowManager(GtkDnDTargetTest.class);
        run = GtkGlassChildJvm.run(GtkDnDTargetTest.class, "targetScenario", List.of("-DUSE_ROBOT=true"));
    }

    private static String value(String key) {
        String value = run.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return value;
    }

    private static void assertEveryNativeThrowsOutsideADrag(String prefix) {
        for (String name : NATIVES) {
            assertEquals(IllegalStateException.class.getName() + ": " + MESSAGE, value(prefix + name), prefix + name);
        }
    }

    /** Outside a drag each of the three natives throws {@code check_state_in_drag}'s exception. */
    @Test
    public void outsideADragTheTargetNativesThrowIllegalStateException() {
        assertEveryNativeThrowsOutsideADrag(BEFORE);
    }

    /**
     * Once an accepted drop has finished each of them throws it again: {@code process_dnd_target_drop_start} clears
     * the {@code GdkDragContext} the guard reads. Commit {@code 033187ad90} left it behind, so after a drag had
     * ended these calls passed the guard and worked on a context GDK may already have released.
     */
    @Test
    public void afterAnAcceptedDropTheTargetNativesThrowIllegalStateException() {
        assertEveryNativeThrowsOutsideADrag(AFTER_ACCEPTED_DROP);
    }

    /**
     * Once a rejected drop has finished each of them throws it again. The fourth drag's target throws from its enter,
     * which leaves the enter unanswered ({@code process_dnd_target_drag_motion} returns before it clears
     * {@code just_entered}), so the drop that the release over the target sends is rejected by
     * {@code process_dnd_target_drop_start} without a {@code dragDrop}. That exit of commit {@code 033187ad90}
     * returned with the {@code GdkDragContext} still set, so after a rejected drop these calls passed the guard and
     * worked on the released context of a drag that had ended.
     */
    @Test
    public void afterARejectedDropTheTargetNativesThrowIllegalStateException() {
        assertEveryNativeThrowsOutsideADrag(AFTER_REJECTED_DROP);
    }

    /**
     * The fourth drag was rejected, not dropped: its target threw from its one enter and no {@code dragDrop} ran,
     * which the two absent keys prove. What the source's {@code dragEnd} reports is GTK's routing of the rejected
     * {@code XdndFinished}, not Glass's, so either value is accepted: GTK 3.24.52, the capture environment, ends
     * its managed X11 drag through {@code drag-end}, where {@code dnd_end_callback} reads the selected action of the
     * context - {@code COPY}, which the source window itself accepted before the pointer left it and a rejected
     * drop does not reset; a GTK that ends it through {@code drag-failed} reports {@code NONE} from
     * {@code dnd_drag_failed_callback}.
     */
    @Test
    public void theRejectedDragWasNotDropped() {
        assertEquals("1", value("drag4.enter.throws"));
        assertNull(run.values().get("drag4.dropped"), "drag4.dropped");
        assertNull(run.values().get("drag4.drop.sameArrayAsEnter"), "drag4.drop.sameArrayAsEnter");
        String performed = value("drag4.performed");
        assertTrue(REJECTED_DRAG_END_ACTIONS.contains(performed), "drag4.performed: " + performed);
    }

    /**
     * The file list of a URI list whose file URI follows a non-file one: the path is its one element, and the URI
     * list itself answers the non-file URI. Commit {@code 033187ad90} stored the path at the index of the URI
     * within the whole list, outside an array sized by the number of files, which left the file list
     * {@code [null]} and reported an {@code ArrayIndexOutOfBoundsException}.
     */
    @Test
    public void aFileUriAfterANonFileUriKeepsItsPlace() {
        assertEquals("true", value("drag3.dropped"));
        assertEquals("[" + MIXED_FILE + "]", value("drag3.drop.files"));
        assertEquals(MIXED_URL, value("drag3.drop.uriList"));
        List<String> mimes = List.of(value("drag3.enter.mimes").split("\\|"));
        assertTrue(mimes.contains(FILE_LIST_MIME) && mimes.contains(URI_LIST_MIME), String.join("|", mimes));
    }

    /** Every accepted drag reached the target and ended with the action it answered. */
    @Test
    public void everyAcceptedDragWasDropped() {
        for (int drag = 1; drag <= ACCEPTED_DRAGS; drag++) {
            assertEquals("COPY", value("drag" + drag + ".performed"), "drag " + drag);
            assertEquals("true", value("drag" + drag + ".dropped"), "drag " + drag);
        }
    }

    /**
     * Within one accepted drag, the drop reads the very array the enter read, and so does a second read at the
     * enter.
     */
    @Test
    public void everyReadWithinADragAnswersTheSameArray() {
        for (int drag = 1; drag <= ACCEPTED_DRAGS; drag++) {
            assertEquals("true", value("drag" + drag + ".enter.sameArray"), "drag " + drag);
            assertEquals("true", value("drag" + drag + ".drop.sameArrayAsEnter"), "drag " + drag);
        }
    }

    /** The second drag, whose source offers one mime more, has a list of its own, with that mime in it. */
    @Test
    public void theNextDragHasItsOwnMimes() {
        List<String> first = List.of(value("drag1.enter.mimes").split("\\|"));
        List<String> second = List.of(value("drag2.enter.mimes").split("\\|"));
        assertTrue(first.contains("text/plain") && !first.contains(CUSTOM_MIME), String.join("|", first));
        assertTrue(second.contains("text/plain") && second.contains(CUSTOM_MIME), String.join("|", second));
        assertEquals("false", value("drag2.sameArrayAsDrag1"));
        assertEquals("first", value("drag1.drop.text/plain"));
        assertEquals("second", value("drag2.drop.text/plain"));
    }

    // ---------------------------------------------------------------------------------------------
    // The scenario (child JVM)
    // ---------------------------------------------------------------------------------------------

    /** The drag source: offers {@link #content} and records what the drag performed. */
    static final class SourceHooks extends GtkEventTrace.ViewHooks {

        volatile Map<String, Object> content;
        volatile boolean flushReturned;
        volatile String performed;

        @Override
        void dragStart(View view, ClipboardAssistance assistant) {
            for (Map.Entry<String, Object> entry : content.entrySet()) {
                assistant.setData(entry.getKey(), entry.getValue());
            }
            assistant.setSupportedActions(Clipboard.ACTION_COPY | Clipboard.ACTION_MOVE);
            assistant.flush();
            flushReturned = true;
        }

        @Override
        void dragEnd(int performedAction) {
            performed = GtkEventTrace.action(performedAction);
        }
    }

    /**
     * The drop target: reads through the peer methods at the enter and at the drop, into {@code out}; with
     * {@link #throwsAtEnter} it reads nothing and throws from every enter instead, counting them.
     */
    static final class TargetHooks extends GtkEventTrace.ViewHooks {

        private final Map<String, String> out;
        private final List<String[]> enterArrays = new ArrayList<>();
        private int entersThrown;
        volatile String prefix;
        volatile boolean uriList;
        volatile boolean throwsAtEnter;

        TargetHooks(Map<String, String> out) {
            this.out = out;
        }

        @Override
        int dragAction(String phase, int recommended) {
            return Clipboard.ACTION_COPY;
        }

        @Override
        void dragEnter(ClipboardAssistance target) {
            if (throwsAtEnter) {
                out.put(prefix + "enter.throws", Integer.toString(++entersThrown));
                throw new UnsupportedOperationException(prefix + "enter: this target throws at every enter");
            }
            String[] first = GtkGlassShim.dndMimesFromSystem();
            String[] second = GtkGlassShim.dndMimesFromSystem();
            enterArrays.add(first);
            String[] sorted = first.clone();
            Arrays.sort(sorted);
            out.put(prefix + "enter.mimes", String.join("|", sorted));
            out.put(prefix + "enter.sameArray", Boolean.toString(first == second));
        }

        @Override
        void dragDrop(ClipboardAssistance target) {
            String[] atDrop = GtkGlassShim.dndMimesFromSystem();
            out.put(prefix + "drop.sameArrayAsEnter", Boolean.toString(atDrop == enterArrays.getLast()));
            if (uriList) {
                Object files = GtkGlassShim.dndPopFromSystem(FILE_LIST_MIME);
                out.put(prefix + "drop.files", files == null ? "null" : Arrays.toString((String[]) files));
                out.put(prefix + "drop.uriList", String.valueOf(GtkGlassShim.dndPopFromSystem(URI_LIST_MIME)));
            } else {
                out.put(prefix + "drop.text/plain", String.valueOf(GtkGlassShim.dndPopFromSystem("text/plain")));
            }
            out.put(prefix + "dropped", "true");
        }

        String[] enterArray(int drag) {
            return enterArrays.get(drag - 1);
        }
    }

    static final int LEFT_X = 100;
    static final int RIGHT_X = 600;
    static final int TOP = 100;
    static final int WIDTH = 300;
    static final int HEIGHT = 200;

    /** Runs in {@link GtkGlassChild}. */
    static void targetScenario(Map<String, String> out) throws Exception {
        recordOutcomes(out, BEFORE);
        GtkEventTrace t = new GtkEventTrace();
        SourceHooks source = new SourceHooks();
        TargetHooks target = new TargetHooks(out);
        GtkTraceWindow w1 = window(t, "w1", "v1", LEFT_X, source);
        GtkTraceWindow w2 = window(t, "w2", "v2", RIGHT_X, target);
        try {
            for (int drag = 1; drag <= DRAGS; drag++) {
                Map<String, Object> content = new LinkedHashMap<>();
                switch (drag) {
                    case 1 -> content.put("text/plain", "first");
                    case 2 -> {
                        content.put("text/plain", "second");
                        content.put(CUSTOM_MIME, ByteBuffer.wrap(new byte[] {1, 2, 3}));
                    }
                    case 3 -> content.put(URI_LIST_MIME, MIXED_URI_LIST);
                    case REJECTED_DRAG -> content.put("text/plain", "rejected");
                    default -> throw new IllegalStateException("drag " + drag);
                }
                source.content = content;
                source.flushReturned = false;
                target.prefix = "drag" + drag + ".";
                target.uriList = drag == 3;
                target.throwsAtEnter = drag == REJECTED_DRAG;
                drag(t);
                if (!GtkGlassChild.waitFor(30_000, () -> source.flushReturned)) {
                    throw new IllegalStateException("drag " + drag + ": the source's flush did not return");
                }
                t.settle();
                out.put("drag" + drag + ".performed", String.valueOf(source.performed));
                if (drag == ACCEPTED_DRAGS) {
                    recordOutcomes(out, AFTER_ACCEPTED_DROP);
                }
            }
            out.put("drag2.sameArrayAsDrag1", Boolean.toString(GtkGlassChild.onFx(() ->
                    target.enterArray(2) == target.enterArray(1))));
            recordOutcomes(out, AFTER_REJECTED_DROP);
        } finally {
            t.act(w1::close);
            t.act(w2::close);
        }
    }

    private static GtkTraceWindow window(GtkEventTrace t, String name, String viewName, int x,
                                         GtkEventTrace.ViewHooks hooks) throws Exception {
        GtkTraceWindow window = GtkGlassChild.onFx(() -> t.window(name, null, Window.TITLED | Window.CLOSABLE,
                viewName, hooks));
        t.act(() -> window.setBounds(x, TOP, true, true, -1, -1, WIDTH, HEIGHT, 0, 0));
        t.act(() -> window.setVisible(true));
        return window;
    }

    /** Press in the source, a move that starts the drag, into the target without leaving it, release. */
    private static void drag(GtkEventTrace t) throws Exception {
        t.mouseMove(LEFT_X + 100, TOP + 100);
        t.mousePress(MouseButton.PRIMARY);
        t.mouseMove(LEFT_X + 110, TOP + 105);
        t.mouseMove(LEFT_X + 130, TOP + 110);
        t.mouseMove(RIGHT_X + 50, TOP + 100);
        t.mouseMove(RIGHT_X + 60, TOP + 110);
        t.mouseRelease(MouseButton.PRIMARY);
    }

    /** What each of the four natives answers on the FX thread, under {@code prefix}. */
    private static void recordOutcomes(Map<String, String> out, String prefix) throws Exception {
        GtkGlassChild.onFx(() -> {
            out.put(prefix + "mimesFromSystem", outcome(GtkGlassShim::dndMimesFromSystem));
            out.put(prefix + "popFromSystem", outcome(() -> GtkGlassShim.dndPopFromSystem("text/plain")));
            out.put(prefix + "popFromSystemNull", outcome(() -> GtkGlassShim.dndPopFromSystem(null)));
            out.put(prefix + "supportedSourceActionsFromSystem", outcome(GtkGlassShim::dndSupportedSourceActions));
            return null;
        });
    }

    /** What {@code call} answered, or the class and message of what it threw. */
    private static String outcome(Callable<?> call) {
        try {
            Object result = call.call();
            return "returned " + (result instanceof String[] array ? String.join("|", array) : result);
        } catch (Throwable t) {
            return t.getClass().getName() + ": " + t.getMessage();
        }
    }
}
