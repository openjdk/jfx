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

package test.com.sun.javafx.iio;

import com.sun.javafx.iio.ImageLoader;
import com.sun.javafx.iio.jpeg.JPEGImageLoaderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.abort;

/**
 * The one decision the JPEG tests make about the native layer: is {@code javafx_iio} expected in this
 * JVM, or is this a build that legitimately has none?
 * <p>
 * This module compiles {@code javafx_iio} from source ({@code modules/javafx.graphics/native/*.cmake},
 * target {@code iio}), so a library that is reachable and does not work is a broken build, never an
 * environment fact. A missing export, an ABI version the facade was not written for, a library left out of
 * the CMake build or a stale copy earlier on {@code java.library.path} are all failures and would all
 * be skips under a plain {@code assumeTrue(libraryLoaded)} - which is how a green run comes to report
 * zero JPEG tests.
 * <p>
 * The rule, in one sentence: <em>skip only when this build produced no {@code javafx_iio} and none is
 * on {@code java.library.path} either; otherwise it has to load and decode.</em> A normal local or CI
 * build has just written one into {@code target/native/bin}, which the surefire {@code argLine} of
 * {@code modules/javafx.graphics/pom.xml} passes as {@code -Djava.library.path}, so it always takes
 * the failing branch; the skip is reachable only through {@code -DskipNative=true} on a tree whose
 * natives were never built. A build that produced this module's other libraries and not this one is
 * not that tree - the native build ran and said it succeeded - so that fails too.
 * <p>
 * Modelled on {@code test.com.sun.media.jfxmediaimpl.MediaNatives}.
 */
public final class JpegNatives {

    /** {@code javafx_iio.dll}, {@code libjavafx_iio.so} or {@code libjavafx_iio.dylib}. */
    private static final String LIBRARY_FILE = System.mapLibraryName("javafx_iio");

    /**
     * Other libraries the javafx.graphics CMake build writes into the same directory. Any of them next
     * to no {@code javafx_iio} means the native build ran and reported success without producing the
     * library these tests exist for. {@code prism_sw} is the one built on every platform. The list is a
     * union of witnesses, so an entry a platform does not build only shrinks it: {@code javafx_font} is
     * built on macOS only (its CoreText sources). Windows lost its {@code font} target with
     * {@code directwrite.cpp}; Linux lost it when {@code fontpath_linux.c}, {@code freetype.c} and
     * {@code pango.c} of commit {@code 7b43255b30} were replaced by Java bindings of the system
     * libraries, and lost {@code glass} with {@code launcher.c}, the library that held its
     * {@code Java_com_sun_glass_ui_gtk_GtkApplication__1queryLibrary} at commit {@code 033187ad90}.
     */
    private static final List<String> SIBLING_FILES = List.of(
            System.mapLibraryName("prism_sw"), System.mapLibraryName("glass"),
            System.mapLibraryName("javafx_font"));

    private static boolean decided;
    private static String skipReason;
    private static String failureMessage;
    private static Throwable failureCause;

    private JpegNatives() {
    }

    /**
     * Loads the iio natives, or skips the calling test when this build has none anywhere.
     * <p>
     * Decided once per JVM and then replayed: {@code JPEGImageLoader}'s static initializer runs once
     * per class loader and the JPEG test classes share a surefire fork, so a second caller has to get
     * the first caller's verdict rather than a second load attempt.
     *
     * @throws AssertionError if a {@code javafx_iio} is reachable and cannot be used, which is a
     *         broken build; the message names every candidate on {@code java.library.path}
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
        List<Path> reachable = findOnLibraryPath(LIBRARY_FILE);
        if (reachable.isEmpty()) {
            List<Path> siblings = new ArrayList<>();
            for (String sibling : SIBLING_FILES) {
                siblings.addAll(findOnLibraryPath(sibling));
            }
            if (!siblings.isEmpty()) {
                failureMessage = "this build produced graphics natives but not the one under test: "
                        + siblings + " are on java.library.path and no " + LIBRARY_FILE + " is, so the"
                        + " native build ran and reported success without building the library these"
                        + " tests exist for. A renamed target, a condition that skipped it or a changed"
                        + " output directory is a broken build, not a tree that has no natives, and"
                        + " skipping here would report zero JPEG tests and a green run.";
                return;
            }
            skipReason = "this build has no graphics natives: no " + LIBRARY_FILE + " exists on"
                    + " java.library.path (" + System.getProperty("java.library.path", "") + "), so"
                    + " there is nothing here to test. Build them with"
                    + " \"mvn -pl modules/javafx.graphics test\", that is, without -DskipNative=true.";
            return;
        }
        try {
            forceNativeInit();
            failureMessage = "an empty stream produced a usable JPEG loader, which cannot happen"
                    + " with a working javafx_iio: the source manager reports the missing data as an"
                    + " early EOI, injects one, and first_marker rejects it.";
        } catch (IOException expected) {
            // The library loaded, every iio_* symbol bound and libjpeg rejected an empty stream.
            // That round trip is what "the natives work" means here.
        } catch (Throwable broken) {
            failureMessage = failureText(reachable, broken);
            failureCause = broken;
        }
    }

    /**
     * Runs {@code JPEGNative}'s static initializer - {@code NativeLibLoader.loadLibrary}, the binding of
     * every {@code iio_*} symbol and the ABI check - and one real trip through libjpeg. An empty stream
     * cannot produce a loader: the C source manager reports the missing data as an early EOI, injects
     * one, and {@code first_marker} rejects it, so a working build throws {@code IOException} here.
     */
    private static void forceNativeInit() throws IOException {
        ImageLoader loader = JPEGImageLoaderFactory.getInstance()
                .createImageLoader(new ByteArrayInputStream(new byte[0]));
        loader.dispose();
    }

    private static String failureText(List<Path> reachable, Throwable cause) {
        StringBuilder text = new StringBuilder(512);
        text.append("javafx_iio is reachable but unusable, which is a broken build and never an")
                .append(" environment fact: ").append(cause).append('\n');
        for (Path library : reachable) {
            text.append("  reachable on java.library.path: ").append(library).append(describe(library))
                    .append('\n');
        }
        text.append("Rebuild with \"mvn -pl modules/javafx.graphics process-classes\", that is,")
                .append(" without -DskipNative=true.");
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

    /**
     * Every copy of {@code name} on {@code java.library.path}, in the order {@code NativeLibLoader}
     * tries the entries - it takes the first one that loads, so the order is the whole point.
     */
    private static List<Path> findOnLibraryPath(String name) {
        List<Path> found = new ArrayList<>();
        for (String entry : System.getProperty("java.library.path", "").split(File.pathSeparator)) {
            if (entry.isEmpty()) {
                continue;
            }
            try {
                Path library = Path.of(entry).resolve(name);
                if (Files.isRegularFile(library)) {
                    found.add(library);
                }
            } catch (InvalidPathException e) {
                // An entry this platform cannot even parse holds no library; the next one might.
            }
        }
        return found;
    }
}
