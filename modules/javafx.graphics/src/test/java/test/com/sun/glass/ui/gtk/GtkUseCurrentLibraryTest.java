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
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.glass.ui.gtk.GtkGlassShim;
import com.sun.glass.ui.gtk.GtkSyntheticEvents;
import com.sun.glass.ui.gtk.GtkTraceWindow;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The GTK glass started from a {@code glassgtk3} build that is loaded as the {@code glass} library itself - the
 * deployment {@code Java_com_sun_glass_ui_gtk_GtkApplication__1queryLibrary} of {@code GlassApplication.cpp} (commit
 * {@code 033187ad90}) answers {@code QUERY_USE_CURRENT} for ("renaming the gtk versioned native libraries to be
 * libglass.so"), so that {@code GtkApplication} loads no {@code glassgtk3}. A child JVM whose
 * {@code java.library.path} starts with a temporary directory holding a copy of this build's
 * {@code libglassgtk3.so} named {@code libglass.so} takes that branch, and:
 * <ul>
 * <li>maps that copy as its only glass library and says so ({@code -Djdk.gtk.verbose=true});</li>
 * <li>has the callback tables of {@code glass_gtk_api.h} installed in it, one stub per slot;</li>
 * <li>receives a window's events through those stubs, not through JNI, and a closed window leaves the window and
 * view registries as they were - without the tables, nothing would ever take it out again.</li>
 * </ul>
 */
@EnabledOnOs(OS.LINUX)
@Timeout(300)
public class GtkUseCurrentLibraryTest {

    private static final int STYLE = Window.TITLED | Window.CLOSABLE;

    private static GtkGlassChildJvm.Run run;
    private static Path renamedLibrary;

    @BeforeAll
    @Timeout(GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() throws IOException {
        GtkGlassChildJvm.requireDisplay();
        String libraryPath = System.getProperty("java.library.path", "");
        Path glassgtk3 = Stream.of(libraryPath.split(File.pathSeparator)).filter(dir -> !dir.isEmpty())
                .map(dir -> Path.of(dir, "libglassgtk3.so")).filter(Files::isRegularFile).findFirst().orElse(null);
        assumeTrue(glassgtk3 != null, "this build has no GTK glass natives in " + libraryPath);
        Path directory = Files.createTempDirectory("gtk-use-current");
        try {
            // /proc/self/maps names a mapped file by its real path
            renamedLibrary = Files.copy(glassgtk3, directory.resolve("libglass.so")).toRealPath();
            run = GtkGlassChildJvm.runWithoutToolkit(GtkUseCurrentLibraryTest.class, "useCurrentScenario", List.of(
                    "-Djava.library.path=" + directory + File.pathSeparator + libraryPath,
                    "-Djdk.gtk.verbose=true"), true);
        } finally {
            deleteTree(directory);
        }
    }

    private static String value(String key) {
        String value = run.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return value;
    }

    @Test
    public void theRenamedCopyIsTheOnlyGlassLibrary() throws IOException {
        assertEquals(List.of(renamedLibrary.toString()), List.of(value("mapped").split("\n")), run.describe());
        String stdout = Files.readString(run.stdout(), StandardCharsets.ISO_8859_1);
        assertTrue(stdout.contains("Glass GTK library to load is already loaded"), stdout);
        assertTrue(!stdout.contains("Glass GTK library to load is glassgtk3"), stdout);
    }

    @Test
    public void theCallbackTablesAreInstalledInIt() {
        assertEquals("true", value("installed"));
        assertEquals("1/1", value("abi"));
        assertEquals("2 12 10 5 distinct=29", value("stubs"));
    }

    @Test
    public void eventsComeThroughTheTablesAndAClosedWindowLeavesTheRegistries() {
        String kinds = value("kinds");
        for (String kind : List.of("notifyResize", "notifyMove", "view", "notifyDestroy")) {
            assertTrue(kinds.contains(kind), "no " + kind + " through the tables: " + run.describe());
        }
        assertEquals("", value("offTable"), run.describe());
        assertEquals(value("registry.before"), value("registry.after"));
    }

    // ---------------------------------------------------------------------------------------------
    // The scenario (child JVM)
    // ---------------------------------------------------------------------------------------------

    /**
     * Runs in {@link GtkGlassChild} without its toolkit: sets {@code GDK_BACKEND=x11}, then starts the toolkit, which
     * loads the renamed library. The {@code _queryLibrary} of the {@code libglass.so} launcher ({@code launcher.c})
     * sets that variable before GTK starts, the one of {@code glassgtk3} does not, and a session that also offers
     * GDK a Wayland display (WSLg does, whatever {@code WAYLAND_DISPLAY} says) would otherwise get a GDK that is not
     * the X11 one the glass code calls into.
     */
    static void useCurrentScenario(Map<String, String> out) throws Exception {
        GtkSyntheticEvents.setenv("GDK_BACKEND", "x11");
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        if (!started.await(60, TimeUnit.SECONDS)) {
            throw new IllegalStateException("the toolkit did not start within 60 s");
        }
        out.put("mapped", String.join("\n", mappedGlassLibraries()));
        out.put("installed", Boolean.toString(GtkGlassShim.callbacksInstalled()));
        out.put("abi", GtkGlassShim.facadeAbiVersion() + "/" + GtkGlassShim.libraryAbiVersion());
        out.put("stubs", GtkGlassShim.installedStubs());
        out.put("registry.before", GtkGlassChild.onFx(GtkUseCurrentLibraryTest::registries));

        Set<String> kinds = new TreeSet<>();
        List<String> offTable = new ArrayList<>();
        GtkEventTrace t = new GtkEventTrace();
        GtkTraceWindow window = GtkGlassChild.onFx(() -> {
            GtkTraceWindow w = new GtkTraceWindow(null, Screen.getMainScreen(), STYLE,
                    call -> GtkCallbackTableTest.stack(call.split("[ =]")[0], kinds, offTable));
            View view = Application.GetApplication().createView();
            view.setEventHandler(new View.EventHandler() {
                @Override
                public void handleViewEvent(View v, long time, int type) {
                    GtkCallbackTableTest.stack("view", kinds, offTable);
                }
            });
            w.setView(view);
            w.setBounds(100, 100, true, true, -1, -1, 300, 200, 0, 0);
            w.setVisible(true);
            return w;
        });
        t.settle();
        t.act(() -> window.setBounds(120, 110, true, true, -1, -1, 310, 210, 0, 0));
        t.act(window::close);
        out.put("registry.after", GtkGlassChild.onFx(GtkUseCurrentLibraryTest::registries));
        GtkGlassChild.onFx(() -> {
            out.put("kinds", String.join(",", kinds));
            out.put("offTable", String.join("\n", offTable));
            return null;
        });
    }

    private static String registries() {
        return GtkGlassShim.windowRegistrySize() + " windows " + GtkGlassShim.viewRegistrySize() + " views";
    }

    /**
     * The real paths of the files named {@code libglass*.so} this process has mapped, sorted, no duplicates.
     * Package-private: {@link GtkLibraryQueryHeadlessTest} drives the same deployment without a display.
     */
    static List<String> mappedGlassLibraries() throws IOException {
        Set<String> paths = new TreeSet<>();
        for (String line : Files.readAllLines(Path.of("/proc/self/maps"))) {
            int slash = line.indexOf('/');
            if (slash >= 0) {
                Path path = Path.of(line.substring(slash).trim());
                String name = path.getFileName().toString();
                if (name.startsWith("libglass") && name.endsWith(".so")) {
                    paths.add(path.toString());
                }
            }
        }
        return List.copyOf(paths);
    }

    /** Package-private for the same reason as {@link #mappedGlassLibraries}. */
    static void deleteTree(Path root) {
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
