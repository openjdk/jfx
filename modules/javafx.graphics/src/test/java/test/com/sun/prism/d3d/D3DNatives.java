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

package test.com.sun.prism.d3d;

import com.sun.javafx.PlatformUtil;
import com.sun.prism.d3d.D3DNativeShim;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.abort;

/**
 * The one decision the D3D tests make about the native layer: is the {@code prism_d3d} library
 * expected in this JVM, or is this a build that legitimately has none?
 * <p>
 * This module compiles {@code prism_d3d} from source ({@code modules/javafx.graphics/native}, bound to
 * {@code process-classes}) on Windows, so a library that is reachable and does not work is a broken
 * build, never an environment fact. A missing export, a {@code d3d_abi_version} that moved, or the
 * module left out of {@code --enable-native-access} are all failures and must not turn into skips,
 * which is how a green run comes to report zero binding tests. This is
 * {@code test.com.sun.pisces.PiscesNatives} for the Direct3D library.
 * <p>
 * The rule: <em>skip on any platform but Windows; on Windows skip only when no {@code prism_d3d.dll}
 * exists on {@code java.library.path} and no other javafx.graphics native library does either;
 * otherwise it has to load and bind.</em> Whether a Direct3D <em>device</em> can be created in the
 * session is a separate question that the tests decide per case, through {@code d3d_pipeline_init}.
 */
public final class D3DNatives {

    private static final String LIBRARY_NAME = "prism_d3d";

    /** {@code prism_d3d.dll}. */
    private static final String LIBRARY_FILE = System.mapLibraryName(LIBRARY_NAME);

    /**
     * Other libraries the javafx.graphics CMake build writes into the same directory on Windows. Any
     * of them next to no {@code prism_d3d} is a broken build, not a tree without natives.
     */
    private static final List<String> SIBLING_FILES = List.of(
            System.mapLibraryName("prism_sw"), System.mapLibraryName("glass"),
            System.mapLibraryName("javafx_iio"));

    private static boolean decided;
    private static String skipReason;
    private static String failureMessage;
    private static Throwable failureCause;

    private D3DNatives() {
    }

    /**
     * Loads {@code prism_d3d}, or skips the calling test when this build has none anywhere.
     * <p>
     * Decided once per JVM and then replayed: the library, the lookup and any failure are per class
     * loader, and the D3D test classes share a surefire fork, so a second caller has to get the first
     * caller's verdict rather than a second load attempt.
     *
     * @throws AssertionError if a {@code prism_d3d} is reachable and cannot be used, which is a broken
     *         build; the message names every candidate on {@code java.library.path}
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
        if (!PlatformUtil.isWindows()) {
            skipReason = LIBRARY_NAME + " is Windows-only; nothing to test on this platform";
            return;
        }
        List<Path> entries = libraryPathEntries();
        List<Path> reachable = new ArrayList<>();
        List<Path> builtWithoutIt = new ArrayList<>();
        for (Path dir : entries) {
            if (Files.isRegularFile(dir.resolve(LIBRARY_FILE))) {
                reachable.add(dir.resolve(LIBRARY_FILE));
            } else if (SIBLING_FILES.stream().anyMatch(name -> Files.isRegularFile(dir.resolve(name)))) {
                builtWithoutIt.add(dir);
            }
        }
        if (reachable.isEmpty()) {
            if (!builtWithoutIt.isEmpty()) {
                failureMessage = "this build produced javafx.graphics natives but not the one under test: "
                        + builtWithoutIt + " hold other libraries of this module and no " + LIBRARY_FILE
                        + ", so the native build ran and reported success without building the library"
                        + " these tests exist for. A renamed target, a condition that skipped it or a"
                        + " changed output directory is a broken build, not a tree that has no natives,"
                        + " and skipping here would report zero D3D binding tests and a green run.";
                return;
            }
            skipReason = "this build has no javafx.graphics natives: no " + LIBRARY_FILE
                    + " on java.library.path " + entries + ", so there is nothing here to test. Build"
                    + " them with \"mvn -pl modules/javafx.graphics test\", that is, without"
                    + " -DskipNative=true.";
            return;
        }
        try {
            D3DNativeShim.loadLibrary();
        } catch (UnsatisfiedLinkError | RuntimeException e) {
            failureMessage = failureText(reachable, e);
            failureCause = e;
        }
    }

    private static String failureText(List<Path> reachable, Throwable cause) {
        StringBuilder text = new StringBuilder(512);
        text.append("the prism_d3d library is reachable but unusable, which is a broken build and never")
                .append(" an environment fact: ").append(cause).append('\n');
        for (Path library : reachable) {
            text.append("  reachable on java.library.path: ").append(library).append(describe(library))
                    .append('\n');
        }
        text.append("Rebuild the natives with \"mvn -pl modules/javafx.graphics process-classes\" and")
                .append(" check that the library exports every d3d_* symbol the facade binds.");
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
    private static List<Path> libraryPathEntries() {
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
