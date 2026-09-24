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

import com.sun.glass.events.KeyEvent;
import com.sun.glass.events.MouseEvent;
import com.sun.glass.events.ViewEvent;
import com.sun.glass.events.WindowEvent;
import com.sun.glass.ui.Application;
import com.sun.glass.ui.ClipboardAssistance;
import com.sun.glass.ui.GlassRobot;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.glass.ui.gtk.GtkGlassShim;
import com.sun.glass.ui.gtk.GtkTraceWindow;
import com.sun.javafx.tk.HeaderAreaType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

/**
 * The child-JVM side of the GTK glass boundary trace: a recorder that sits behind every Java entry point the GTK
 * glass event code ({@code glass_window.cpp}, {@code glass_window_ime.cpp}, {@code glass_dnd.cpp},
 * {@code GlassView.cpp}, {@code glass_screen.cpp}, {@code glass_general.cpp} at commit {@code 033187ad90}) calls,
 * and the input driver of the scenarios that make it call them.
 * <p>
 * Where the recorder sits:
 * <ul>
 * <li>windows are {@link GtkTraceWindow}s, whose {@code isEnabled} and {@code notify*} overrides see every call
 * the C makes into a window with the arguments it made it with, and whose {@code Window.EventHandler} records what
 * {@code Window} made of it ({@code w1.notifyMove 100 120}, {@code w1.handler MOVE});</li>
 * <li>{@code GtkView} is {@code final}, so a view is recorded at its {@code View.EventHandler}, the first place a
 * test can reach: every {@code View.notify*} the C calls ends there, except that {@code View} drops the dirty
 * rectangle of {@code notifyRepaint}, a {@code notifyResize} to the size the view already has, and the time stamps
 * ({@code v1.mouse DOWN BUTTON_LEFT 20,30 120,130 mods=0x20 popup=false synth=false});</li>
 * <li>the {@code Screen.EventHandler} and the {@code Application.EventHandler} the toolkit installed are wrapped
 * ({@code screen.handleSettingsChanged}, {@code app.<method>});</li>
 * <li>the FX thread's uncaught-exception handler, which is where {@code Application.reportException} delivers every
 * exception the C caught after an upcall ({@code reported <class>: <message>}).</li>
 * </ul>
 * A call that does not arrive on the FX thread is marked {@code !off-fx-thread}. {@link #arm} makes the next
 * recorded call with a given prefix throw into the C ({@code !throws}), which is how the exception paths are driven.
 * Every line is ASCII: text is written as {@code \\uXXXX} escapes where it is not printable ASCII.
 */
final class GtkEventTrace {

    /**
     * The prefix of every line recorded inside a nested read of the scenario ({@link #beginNested}); the
     * normalisation {@link GtkTraceGolden#SORT_NESTED_READS} sorts runs of such lines.
     */
    static final String NESTED_MARK = "~ ";

    /** How long {@link #settle} waits for the event stream to stay unchanged. */
    static final long QUIET_MILLIS = 120;

    /** What a view's recorder answers where the C expects a value, and what it does beyond recording. */
    static class ViewHooks {

        /** The action {@code handleDragEnter}, {@code handleDragOver} and {@code handleDragDrop} answer. */
        int dragAction(String phase, int recommended) {
            return recommended;
        }

        /** What {@code pickHeaderArea} answers, which {@code GtkWindow.nonClientHitTest} turns into an HT code. */
        HeaderAreaType headerArea(double x, double y) {
            return null;
        }

        /** {@code getInputMethodCandidatePos}: absolute screen coordinates. */
        double[] candidatePos(int offset) {
            return new double[] {420.0, 260.0};
        }

        /** Called from {@code handleDragStart} after recording; may start a drag with {@code source.flush()}. */
        void dragStart(View view, ClipboardAssistance source) {
        }

        /** Called from {@code handleDragEnd} after recording. */
        void dragEnd(int performedAction) {
        }

        /** Called from {@code handleDragEnter} after recording. */
        void dragEnter(ClipboardAssistance target) {
        }

        /** Called from {@code handleDragLeave} after recording. */
        void dragLeave() {
        }

        /** Called from {@code handleDragDrop} after recording. */
        void dragDrop(ClipboardAssistance target) {
        }
    }

    /** Thrown into the C by an armed recorder. */
    static final class ArmedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        ArmedException(String message) {
            super(message);
        }
    }

    private static final Map<Integer, String> MOUSE_TYPES = names(MouseEvent.class, 221, 228);
    private static final Map<Integer, String> MOUSE_BUTTONS = names(MouseEvent.class, 211, 216);
    private static final Map<Integer, String> WINDOW_EVENTS = names(WindowEvent.class, 511, 546);
    private static final Map<Integer, String> VIEW_EVENTS = names(ViewEvent.class, 411, 432);

    private final List<String> lines = new ArrayList<>();
    private final Map<String, Integer> armed = new LinkedHashMap<>();
    private int nested;
    private volatile int version;
    private GlassRobot robot;

    /** Records {@code line} and throws into the caller if it is armed for it. */
    void record(String line) {
        String text = Application.isEventThread() ? line : line + " !off-fx-thread";
        boolean fire = false;
        synchronized (this) {
            for (Map.Entry<String, Integer> entry : armed.entrySet()) {
                if (text.startsWith(entry.getKey())) {
                    fire = true;
                    if (entry.getValue() == 1) {
                        armed.remove(entry.getKey());
                    } else {
                        entry.setValue(entry.getValue() - 1);
                    }
                    text = text + " !throws";
                    break;
                }
            }
            add(text);
        }
        if (fire) {
            throw new ArmedException("armed " + line);
        }
    }

    /** A step boundary of the scenario, {@code == name}. */
    synchronized void step(String name) {
        add("== " + name);
    }

    /** A value the scenario read back, {@code # text}; part of the trace. */
    synchronized void note(String text) {
        add("# " + text);
    }

    /** Adds {@code text}, marked with {@link #NESTED_MARK} inside a nested read; holding the monitor. */
    private void add(String text) {
        lines.add(nested > 0 ? NESTED_MARK + text : text);
        version++;
    }

    /**
     * Opens a nested read of the scenario: until the matching {@link #endNested}, every recorded line is marked, so
     * that the normalisation can order what the read's nested main loop dispatched meanwhile.
     */
    synchronized void beginNested() {
        nested++;
    }

    synchronized void endNested() {
        nested--;
    }

    /**
     * The next {@code times} recorded calls starting with {@code prefix} throw an {@link ArmedException}; several
     * prefixes can be armed at once.
     */
    synchronized void arm(String prefix, int times) {
        armed.put(prefix, times);
    }

    synchronized List<String> lines() {
        return List.copyOf(lines);
    }

    /** The sink of a {@link GtkTraceWindow} named {@code name}. */
    Consumer<String> windowSink(String name) {
        return call -> record(name + "." + call);
    }

    /** The uncaught-exception handler of the FX thread: what {@code Application.reportException} delivers. */
    Thread.UncaughtExceptionHandler reporter() {
        return (thread, throwable) -> record("reported " + throwable.getClass().getSimpleName() + ": "
                + escape(throwable.getMessage()));
    }

    /**
     * Creates a {@link GtkTraceWindow} named {@code name} with a recording {@code Window.EventHandler}, and a view
     * named {@code viewName} (none if {@code null}) with a recording {@code View.EventHandler}; FX thread.
     */
    GtkTraceWindow window(String name, Window owner, int styleMask, String viewName, ViewHooks hooks) {
        GtkTraceWindow window = new GtkTraceWindow(owner, Screen.getMainScreen(), styleMask, windowSink(name));
        window.setEventHandler(windowHandler(name));
        if (viewName != null) {
            View view = Application.GetApplication().createView();
            view.setEventHandler(viewHandler(viewName, hooks));
            window.setView(view);
        }
        return window;
    }

    /** A recording {@code Window.EventHandler}. */
    Window.EventHandler windowHandler(String name) {
        return new Window.EventHandler() {
            @Override
            public void handleWindowEvent(Window window, long time, int type) {
                record(name + ".handler " + WINDOW_EVENTS.getOrDefault(type, Integer.toString(type)));
            }

            @Override
            public void handleScreenChangedEvent(Window window, long time, Screen oldScreen, Screen newScreen) {
                record(name + ".handler screenChanged " + GtkTraceWindow.describe(oldScreen) + " -> "
                        + GtkTraceWindow.describe(newScreen));
            }

            @Override
            public void handleLevelEvent(int level) {
                record(name + ".handler level " + level);
            }
        };
    }

    /** A recording {@code View.EventHandler}. */
    View.EventHandler viewHandler(String name, ViewHooks hooks) {
        return new View.EventHandler() {
            @Override
            public void handleViewEvent(View view, long time, int type) {
                String kind = VIEW_EVENTS.getOrDefault(type, Integer.toString(type));
                if (type == ViewEvent.RESIZE) {
                    record(name + ".view " + kind + " " + view.getWidth() + "x" + view.getHeight());
                } else if (type == ViewEvent.MOVE) {
                    Window host = view.getWindow();
                    boolean placed = !view.isClosed() && host != null && !host.isClosed();
                    record(name + ".view " + kind + (placed ? " at " + view.getX() + "," + view.getY() : ""));
                } else {
                    record(name + ".view " + kind);
                }
            }

            @Override
            public boolean handleKeyEvent(View view, long time, int action, int keyCode, char[] keyChars,
                                          int modifiers) {
                record(name + ".key " + KeyEvent.getTypeString(action) + " 0x" + Integer.toHexString(keyCode)
                        + " chars=" + chars(keyChars) + " mods=0x" + Integer.toHexString(modifiers));
                return false;
            }

            @Override
            public boolean handleMenuEvent(View view, int x, int y, int xAbs, int yAbs, boolean isKeyboardTrigger) {
                record(name + ".menu " + x + "," + y + " " + xAbs + "," + yAbs + " keyboard=" + isKeyboardTrigger);
                return false;
            }

            @Override
            public void handleMouseEvent(View view, long time, int type, int button, int x, int y, int xAbs,
                                         int yAbs, int modifiers, boolean isPopupTrigger, boolean isSynthesized) {
                record(name + ".mouse " + MOUSE_TYPES.getOrDefault(type, Integer.toString(type)) + " "
                        + MOUSE_BUTTONS.getOrDefault(button, Integer.toString(button)) + " " + x + "," + y + " "
                        + xAbs + "," + yAbs + " mods=0x" + Integer.toHexString(modifiers) + " popup=" + isPopupTrigger
                        + " synth=" + isSynthesized);
            }

            @Override
            public void handleScrollEvent(View view, long time, int x, int y, int xAbs, int yAbs, double deltaX,
                                          double deltaY, int modifiers, int lines, int chars, int defaultLines,
                                          int defaultChars, double xMultiplier, double yMultiplier) {
                record(name + ".scroll " + x + "," + y + " " + xAbs + "," + yAbs + " d=" + deltaX + "," + deltaY
                        + " mods=0x" + Integer.toHexString(modifiers) + " lines=" + lines + " chars=" + chars
                        + " default=" + defaultLines + "," + defaultChars + " mult=" + xMultiplier + ","
                        + yMultiplier);
            }

            @Override
            public void handleInputMethodEvent(long time, String text, int[] clauseBoundary, int[] attrBoundary,
                                               byte[] attrValue, int commitCount, int cursorPos) {
                record(name + ".im text=" + escape(text) + " clause=" + ints(clauseBoundary) + " attrBounds="
                        + ints(attrBoundary) + " attrs=" + bytes(attrValue) + " commit=" + commitCount + " cursor="
                        + cursorPos);
            }

            @Override
            public double[] getInputMethodCandidatePos(int offset) {
                record(name + ".imCandidatePos " + offset);
                return hooks.candidatePos(offset);
            }

            @Override
            public void handleDragStart(View view, int button, int x, int y, int xAbs, int yAbs,
                                        ClipboardAssistance dropSourceAssistant) {
                record(name + ".dragStart " + MOUSE_BUTTONS.getOrDefault(button, Integer.toString(button)) + " "
                        + x + "," + y + " " + xAbs + "," + yAbs);
                hooks.dragStart(view, dropSourceAssistant);
            }

            @Override
            public void handleDragEnd(View view, int performedAction) {
                record(name + ".dragEnd " + action(performedAction));
                hooks.dragEnd(performedAction);
            }

            @Override
            public int handleDragEnter(View view, int x, int y, int xAbs, int yAbs, int recommendedDropAction,
                                       ClipboardAssistance dropTargetAssistant) {
                record(name + ".dragEnter " + x + "," + y + " " + xAbs + "," + yAbs + " "
                        + action(recommendedDropAction));
                hooks.dragEnter(dropTargetAssistant);
                return hooks.dragAction("enter", recommendedDropAction);
            }

            @Override
            public int handleDragOver(View view, int x, int y, int xAbs, int yAbs, int recommendedDropAction,
                                      ClipboardAssistance dropTargetAssistant) {
                record(name + ".dragOver " + x + "," + y + " " + xAbs + "," + yAbs + " "
                        + action(recommendedDropAction));
                return hooks.dragAction("over", recommendedDropAction);
            }

            @Override
            public void handleDragLeave(View view, ClipboardAssistance dropTargetAssistant) {
                record(name + ".dragLeave");
                hooks.dragLeave();
            }

            @Override
            public int handleDragDrop(View view, int x, int y, int xAbs, int yAbs, int recommendedDropAction,
                                      ClipboardAssistance dropTargetAssistant) {
                record(name + ".dragDrop " + x + "," + y + " " + xAbs + "," + yAbs + " "
                        + action(recommendedDropAction));
                hooks.dragDrop(dropTargetAssistant);
                return hooks.dragAction("drop", recommendedDropAction);
            }

            @Override
            public HeaderAreaType pickHeaderArea(double x, double y) {
                HeaderAreaType area = hooks.headerArea(x, y);
                record(name + ".pickHeaderArea " + x + "," + y + " -> " + area);
                return area;
            }
        };
    }

    /**
     * Wraps the toolkit's {@code Screen.EventHandler} and {@code Application.EventHandler} and makes the FX thread's
     * uncaught-exception handler record; FX thread.
     */
    void installApplicationRecorders() {
        Thread.currentThread().setUncaughtExceptionHandler(reporter());
        GtkGlassShim.wrapScreenEventHandler(() -> record("screen.handleSettingsChanged"));
        Application application = Application.GetApplication();
        Application.EventHandler previous = application.getEventHandler();
        application.setEventHandler(new ApplicationRecorder(previous));
    }

    /** Records every {@code Application.EventHandler} call and passes it on. */
    private final class ApplicationRecorder extends Application.EventHandler {

        private final Application.EventHandler next;

        ApplicationRecorder(Application.EventHandler next) {
            this.next = next == null ? new Application.EventHandler() { } : next;
        }

        @Override
        public void handleWillFinishLaunchingAction(Application app, long time) {
            record("app.handleWillFinishLaunchingAction");
            next.handleWillFinishLaunchingAction(app, time);
        }

        @Override
        public void handleDidFinishLaunchingAction(Application app, long time) {
            record("app.handleDidFinishLaunchingAction");
            next.handleDidFinishLaunchingAction(app, time);
        }

        @Override
        public void handleWillBecomeActiveAction(Application app, long time) {
            record("app.handleWillBecomeActiveAction");
            next.handleWillBecomeActiveAction(app, time);
        }

        @Override
        public void handleDidBecomeActiveAction(Application app, long time) {
            record("app.handleDidBecomeActiveAction");
            next.handleDidBecomeActiveAction(app, time);
        }

        @Override
        public void handleWillResignActiveAction(Application app, long time) {
            record("app.handleWillResignActiveAction");
            next.handleWillResignActiveAction(app, time);
        }

        @Override
        public void handleDidResignActiveAction(Application app, long time) {
            record("app.handleDidResignActiveAction");
            next.handleDidResignActiveAction(app, time);
        }

        @Override
        public void handleDidReceiveMemoryWarning(Application app, long time) {
            record("app.handleDidReceiveMemoryWarning");
            next.handleDidReceiveMemoryWarning(app, time);
        }

        @Override
        public void handleWillHideAction(Application app, long time) {
            record("app.handleWillHideAction");
            next.handleWillHideAction(app, time);
        }

        @Override
        public void handleDidHideAction(Application app, long time) {
            record("app.handleDidHideAction");
            next.handleDidHideAction(app, time);
        }

        @Override
        public void handleWillUnhideAction(Application app, long time) {
            record("app.handleWillUnhideAction");
            next.handleWillUnhideAction(app, time);
        }

        @Override
        public void handleDidUnhideAction(Application app, long time) {
            record("app.handleDidUnhideAction");
            next.handleDidUnhideAction(app, time);
        }

        @Override
        public void handleOpenFilesAction(Application app, long time, String[] files) {
            record("app.handleOpenFilesAction " + (files == null ? "null" : String.join("|", files)));
            next.handleOpenFilesAction(app, time, files);
        }

        @Override
        public void handleQuitAction(Application app, long time) {
            record("app.handleQuitAction");
            next.handleQuitAction(app, time);
        }

        @Override
        public void handlePreferencesChanged(Map<String, Object> preferences) {
            record("app.handlePreferencesChanged " + (preferences == null ? "null" : preferences.keySet().size()));
            next.handlePreferencesChanged(preferences);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Driving
    // ---------------------------------------------------------------------------------------------

    /**
     * Waits until every event the X server has produced so far has been dispatched and the recorded stream has
     * stayed unchanged for two rounds of {@link #QUIET_MILLIS}: {@code XSync} of GDK's connection, then a
     * {@code runLater} round trip, which GLib dispatches only when no X event is pending (the GDK event source has
     * the higher priority), then the quiet time. Gives up after 5 s and says so in the trace.
     */
    void settle() throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        int stable = 0;
        int seen = version;
        while (System.nanoTime() < deadline) {
            GtkGlassChild.onFx(() -> {
                GtkGlassShim.syncGdkDisplay();
                return null;
            });
            GtkGlassChild.onFx(() -> null);
            Thread.sleep(QUIET_MILLIS);
            int now = version;
            if (now == seen) {
                if (++stable >= 2) {
                    return;
                }
            } else {
                stable = 0;
                seen = now;
            }
        }
        note("settle gave up after 5 s");
    }

    /** Runs {@code body} on the FX thread, then {@link #settle}s. */
    void act(ThrowingRunnable body) throws Exception {
        GtkGlassChild.onFx(() -> {
            body.run();
            return null;
        });
        settle();
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    GlassRobot robot() throws Exception {
        if (robot == null) {
            robot = GtkGlassChild.onFx(() -> Application.GetApplication().createRobot());
        }
        return robot;
    }

    void mouseMove(int x, int y) throws Exception {
        GlassRobot r = robot();
        act(() -> r.mouseMove(x, y));
    }

    void mousePress(MouseButton button) throws Exception {
        GlassRobot r = robot();
        act(() -> r.mousePress(button));
    }

    void mouseRelease(MouseButton button) throws Exception {
        GlassRobot r = robot();
        act(() -> r.mouseRelease(button));
    }

    void click(MouseButton button) throws Exception {
        mousePress(button);
        mouseRelease(button);
    }

    void wheel(int amount) throws Exception {
        GlassRobot r = robot();
        act(() -> r.mouseWheel(amount));
    }

    void keyPress(KeyCode code) throws Exception {
        GlassRobot r = robot();
        act(() -> r.keyPress(code));
    }

    void keyRelease(KeyCode code) throws Exception {
        GlassRobot r = robot();
        act(() -> r.keyRelease(code));
    }

    /** Presses {@code chord} in order and releases it in reverse order. */
    void type(KeyCode... chord) throws Exception {
        for (KeyCode code : chord) {
            keyPress(code);
        }
        for (int i = chord.length - 1; i >= 0; i--) {
            keyRelease(chord[i]);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Formatting
    // ---------------------------------------------------------------------------------------------

    static String action(int action) {
        return switch (action) {
            case 0 -> "NONE";
            case 1 -> "COPY";
            case 2 -> "MOVE";
            case 3 -> "COPY_OR_MOVE";
            case 0x40000000 -> "REFERENCE";
            default -> "0x" + Integer.toHexString(action);
        };
    }

    static String chars(char[] chars) {
        if (chars == null) {
            return "null";
        }
        List<String> parts = new ArrayList<>();
        for (char c : chars) {
            parts.add(Integer.toHexString(c));
        }
        return "[" + String.join(",", parts) + "]";
    }

    static String ints(int[] values) {
        if (values == null) {
            return "null";
        }
        List<String> parts = new ArrayList<>();
        for (int v : values) {
            parts.add(Integer.toString(v));
        }
        return "[" + String.join(",", parts) + "]";
    }

    static String bytes(byte[] values) {
        if (values == null) {
            return "null";
        }
        List<String> parts = new ArrayList<>();
        for (byte v : values) {
            parts.add(Integer.toString(v));
        }
        return "[" + String.join(",", parts) + "]";
    }

    /** Printable ASCII as is, a backslash doubled, anything else as {@code \\uXXXX}; {@code null} as {@code null}. */
    static String escape(String text) {
        if (text == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' || c == '"') {
                sb.append('\\').append(c);
            } else if (c >= 0x20 && c < 0x7F) {
                sb.append(c);
            } else {
                sb.append(String.format("\\u%04x", (int) c));
            }
        }
        return sb.append('"').toString();
    }

    /** {@code public static final int} constants of {@code type} whose value is in {@code [from, to]}, by value. */
    private static Map<Integer, String> names(Class<?> type, int from, int to) {
        Map<Integer, String> result = new HashMap<>();
        for (Field field : type.getFields()) {
            int modifiers = field.getModifiers();
            if (field.getType() == int.class && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)
                    && !field.getName().startsWith("_")) {
                try {
                    int value = field.getInt(null);
                    if (value >= from && value <= to) {
                        result.putIfAbsent(value, field.getName());
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return Map.copyOf(result);
    }
}
