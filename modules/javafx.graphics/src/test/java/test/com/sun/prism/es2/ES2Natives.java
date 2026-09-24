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

package test.com.sun.prism.es2;

import com.sun.prism.es2.ES2NativeShim;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.abort;

/**
 * The one decision the ES2 tests make about the native layer: is the library the facade binds present in
 * this JVM?
 * <p>
 * This is {@code test.com.sun.pisces.PiscesNatives} for the ES2 libraries, with one difference that
 * matters: they are <em>optional</em>. {@code INCLUDE_ES2} defaults off on Windows (see
 * {@code modules/javafx.graphics/native/CMakeLists.txt}) and can be turned off on Linux and macOS, and
 * {@code prism_es2_monocle} - the library {@code ES2Native} binds when {@code -Dglass.platform=Monocle}
 * selected the Monocle embedded type - is built on Linux only when {@code INCLUDE_ES2_MONOCLE} resolves to
 * on ({@code AUTO}: {@code INCLUDE_ES2} and a {@code glesv2} pkg-config, i.e. libgles-dev). A build with no
 * such library is a legitimate configuration, not the broken build that a missing {@code prism_sw} or
 * {@code prism_d3d} would be. Nothing here can tell "left out" from "the native target broke", and a spurious
 * failure on every default Windows build is the worse error, so the absence of the library is always a skip.
 * What is never a skip is a library that is present and does not load, bind or match its ABI version: that is
 * a broken build and has to fail, which is how the binding tests keep meaning something on a build that did
 * include it.
 * <p>
 * The rule: <em>skip when the library the facade binds does not exist on {@code java.library.path}; otherwise
 * it has to load and bind.</em> The surefire {@code argLine} of {@code modules/javafx.graphics/pom.xml} sets
 * {@code java.library.path} to this module's own {@code target/native/bin}, so that path is where the
 * build's output is looked for. Build the libraries with {@code -DINCLUDE_ES2=true} (and, for the Monocle
 * one, libgles-dev installed or {@code -DINCLUDE_ES2_MONOCLE=true}) to run these tests.
 * <p>
 * {@code -Djfx.parity.require=true} ({@code test.com.sun.javafx.test.ParityGate}) deliberately does not reach
 * this class: a library left out by design is not a missing oracle, and turning this skip into a failure would
 * make every default Windows build red for a signal Windows is not expected to give.
 */
public final class ES2Natives {

    private static final String DESKTOP_LIBRARY_NAME = "prism_es2";

    /** The library the facade binds in this JVM: {@code prism_es2}, or {@code prism_es2_monocle} on Monocle. */
    private static final String LIBRARY_NAME = ES2NativeShim.libraryName();

    /**
     * {@code prism_es2.dll}, {@code libprism_es2.so} or {@code libprism_es2.dylib}; {@code libprism_es2_monocle.so}
     * on Monocle.
     */
    private static final String LIBRARY_FILE = System.mapLibraryName(LIBRARY_NAME);

    /**
     * Other libraries the javafx.graphics CMake build writes into the same directory. Any of them
     * present with no library under test means the natives were built but this one was left out - still a
     * skip, because the ES2 libraries are optional, but a more specific one. The list is a union of witnesses,
     * so an entry a platform does not build only shrinks it: {@code javafx_font} is built on macOS only (its
     * CoreText sources). Windows lost its {@code font} target with {@code directwrite.cpp}; Linux, which
     * still builds {@code prism_es2}, lost it when {@code fontpath_linux.c}, {@code freetype.c} and
     * {@code pango.c} of commit {@code 7b43255b30} were replaced by Java bindings of the system libraries,
     * and lost {@code glass} with {@code launcher.c}, the library that held its
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1queryLibrary} at commit {@code 033187ad90}. For the
     * Monocle library the desktop {@code prism_es2} is one more witness: built, but without libgles-dev.
     */
    private static final List<String> SIBLING_FILES = siblingFiles();

    private static boolean decided;
    private static String skipReason;
    private static String failureMessage;
    private static Throwable failureCause;

    private ES2Natives() {
    }

    private static List<String> siblingFiles() {
        List<String> files = new ArrayList<>(List.of(
                System.mapLibraryName("prism_sw"), System.mapLibraryName("glass"),
                System.mapLibraryName("javafx_font"), System.mapLibraryName("javafx_iio")));
        if (!DESKTOP_LIBRARY_NAME.equals(LIBRARY_NAME)) {
            files.add(System.mapLibraryName(DESKTOP_LIBRARY_NAME));
        }
        return List.copyOf(files);
    }

    /** The CMake switch that includes the library under test. */
    private static String includeSwitch() {
        return DESKTOP_LIBRARY_NAME.equals(LIBRARY_NAME) ? "INCLUDE_ES2" : "INCLUDE_ES2_MONOCLE";
    }

    /**
     * Loads the library the facade binds, or skips the calling test when this build did not include it.
     * <p>
     * Decided once per JVM and then replayed: the library, the lookup and any failure are per class
     * loader, and the ES2 test classes share a surefire fork, so a second caller has to get the first
     * caller's verdict rather than a second load attempt.
     *
     * @throws AssertionError if the library is present and cannot be used, which is a broken build; the
     *         message names every candidate on {@code java.library.path}
     */
    public static synchronized void require() {
        if (!decided) {
            decide();
            decided = true;
        }
        if (failureMessage != null) {
            throw new AssertionError(failureMessage, failureCause);
        }
        if (skipReason != null) {
            abort(skipReason);
        }
    }

    private static void decide() {
        List<Path> entries = libraryPathEntries();
        List<Path> reachable = new ArrayList<>();
        boolean sawOtherNatives = false;
        for (Path dir : entries) {
            if (Files.isRegularFile(dir.resolve(LIBRARY_FILE))) {
                reachable.add(dir.resolve(LIBRARY_FILE));
            } else if (SIBLING_FILES.stream().anyMatch(name -> Files.isRegularFile(dir.resolve(name)))) {
                sawOtherNatives = true;
            }
        }
        if (reachable.isEmpty()) {
            skipReason = sawOtherNatives
                    ? "this build has javafx.graphics natives but no " + LIBRARY_FILE + " on"
                            + " java.library.path " + entries + ": the ES2 pipeline is optional and this"
                            + " library was not included in this build (" + includeSwitch() + " off, or"
                            + " AUTO without libgles-dev for the Monocle one). Build it with \"-D"
                            + includeSwitch() + "=true\" to run these tests."
                    : "this build has no javafx.graphics natives: no " + LIBRARY_FILE + " on"
                            + " java.library.path " + entries + ", so there is nothing here to test."
                            + " Build them with \"mvn -pl modules/javafx.graphics test"
                            + " -DINCLUDE_ES2=true\", that is, without -DskipNative=true.";
            return;
        }
        try {
            ES2NativeShim.loadLibrary();
        } catch (UnsatisfiedLinkError | RuntimeException e) {
            failureMessage = failureText(reachable, e);
            failureCause = e;
        }
    }

    private static String failureText(List<Path> reachable, Throwable cause) {
        StringBuilder text = new StringBuilder(512);
        text.append("the ").append(LIBRARY_NAME).append(" library is present but unusable, which is a broken")
                .append(" build and never an environment fact: ").append(cause).append('\n');
        for (Path library : reachable) {
            text.append("  present on java.library.path: ").append(library).append(describe(library))
                    .append('\n');
        }
        text.append("Rebuild the natives with \"mvn -pl modules/javafx.graphics process-classes -D")
                .append(includeSwitch()).append("=true\" and check that the library exports every es2_* symbol")
                .append(" the facade binds.");
        return text.toString();
    }

    /** {@code " (N bytes, modified T)"}, or an empty string when the file cannot be inspected. */
    private static String describe(Path library) {
        try {
            return " (" + Files.size(library) + " bytes, modified " + Files.getLastModifiedTime(library)
                    + ")";
        } catch (IOException e) {
            return "";
        }
    }

    /** The {@code java.library.path} entries, in the order the loader tries them. */
    public static List<Path> libraryPathEntries() {
        List<Path> entries = new ArrayList<>();
        for (String entry : System.getProperty("java.library.path", "").split(File.pathSeparator)) {
            if (entry.isEmpty()) {
                continue;
            }
            try {
                entries.add(Path.of(entry));
            } catch (InvalidPathException e) {
                // An entry this platform cannot even parse holds no library; the next one might.
            }
        }
        return entries;
    }
}
