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

package test.com.sun.javafx.font;

import com.sun.javafx.font.FontResource;
import com.sun.javafx.font.PGFont;
import com.sun.javafx.font.PrismFontFactory;
import com.sun.javafx.font.PrismFontFactoryScanShim;
import com.sun.javafx.font.freetype.FTFactory;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * {@code FTFactory.registerEmbeddedFont} given a font file that FreeType cannot open.
 * <p>
 * {@code FT_New_Face} stores a face in its out parameter only when it succeeds. At commit {@code 7b43255b30} the
 * method passed an array that still held the {@code FT_Library} as that out parameter and called
 * {@code FT_Done_Face} on it when {@code FT_New_Face} failed, so FreeType treated the library record as a face and
 * the JVM died with SIGSEGV in {@code FT_Done_Face}. Each case therefore runs in a child JVM ({@link Child}), so
 * that a crash is reported as a failure carrying its {@code hs_err} file instead of ending the test fork. Both
 * production paths into the method are covered:
 * <ul>
 * <li>the font-directory scan that {@code PrismFontFactory} runs over {@code ${java.home}/lib/fonts}: it offers each
 * file with a font suffix to the rasterizer before parsing it, skips the files the rasterizer rejects and keeps
 * going;</li>
 * <li>{@code loadEmbeddedFont} with a path (what {@code Font.loadFont} calls) whose font resource is already cached
 * while FreeType can no longer open its file: the load returns {@code null}.</li>
 * </ul>
 */
@EnabledOnOs(OS.LINUX)
public class LinuxFreetypeNewFaceFailureTest {

    static final String SCAN = "scan";
    static final String RELOAD = "reload";

    static final String WORK_DIRECTORY = "target/linux-font-newface";
    static final String RESULT = "result.txt";
    static final long CHILD_TIMEOUT_SECONDS = 90;

    static final byte[] NOT_A_FONT = "This is not a font file.\n".repeat(16).getBytes(StandardCharsets.US_ASCII);

    @Test
    public void fontDirectoryScanSkipsFilesFreetypeCannotOpen() throws Exception {
        Path ahem = ahem();
        Path work = prepare(SCAN);
        Path fonts = Files.createDirectories(work.resolve("fonts"));
        Path font = Files.copy(ahem, fonts.resolve("ahem.ttf"));
        Files.write(fonts.resolve("garbage.ttf"), NOT_A_FONT);
        Files.createFile(fonts.resolve("empty.otf"));
        Files.createDirectory(fonts.resolve("directory.ttc"));
        Files.createSymbolicLink(fonts.resolve("dangling.ttf"), work.resolve("absent.ttf"));
        Path unreadable = Files.write(fonts.resolve("unreadable.TTF"), NOT_A_FONT);
        Files.setPosixFilePermissions(unreadable, Set.of());

        Map<String, String> result = runChild(SCAN, work);
        assertEquals(FTFactory.class.getName(), result.get("factory"), "font factory");
        assertEquals(font.toString(), result.get("scan.ahem.file"), "file of the Ahem resource after the scan");
    }

    @Test
    public void loadingACachedFontWhoseFileFreetypeCannotOpenReturnsNull() throws Exception {
        Path ahem = ahem();
        Path work = prepare(RELOAD);
        Path first = copy(work, "first");
        Path second = copy(work, "second");
        for (Path target : List.of(first, second)) {
            Files.createDirectories(target.getParent());
            Files.copy(ahem, target);
        }

        Map<String, String> result = runChild(RELOAD, work);
        assertEquals(FTFactory.class.getName(), result.get("factory"), "font factory");
        assertEquals("Ahem;" + first, result.get("reload.first"), "load of the first copy");
        assertEquals("Ahem;" + second, result.get("reload.second"), "load of the second copy");
        assertEquals(FontGoldens.NULL, result.get("reload.replaced"),
                     "load of the first path after its file was replaced");
    }

    private static Path ahem() {
        Path ahem = LinuxFontGoldens.pinned("ahem");
        assumeTrue(Files.isRegularFile(ahem), "the font " + ahem + " is not in this source tree");
        return ahem;
    }

    private static Path prepare(String scenario) throws IOException {
        Path work = LinuxFontGoldens.moduleDirectory().resolve(WORK_DIRECTORY).resolve(scenario);
        LinuxFontGoldens.deleteTree(work);
        return Files.createDirectories(work);
    }

    static Path copy(Path work, String name) {
        return work.resolve(name).resolve("ahem.ttf");
    }

    /**
     * Runs {@link Child} with this JVM's module, native and headless options. The test fails when the child does not
     * exit with 0, leaves a fatal error log or writes no result.
     */
    private static Map<String, String> runChild(String scenario, Path work) throws IOException, InterruptedException {
        Path errors = Files.createDirectories(work.resolve("hs_err"));
        Path stdout = work.resolve("child.out");
        Path stderr = work.resolve("child.err");
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (argument.startsWith("-agentlib") || argument.startsWith("-javaagent")
                    || argument.startsWith("-Xrunjdwp") || argument.startsWith("-Xdebug")
                    || argument.startsWith("-XX:ErrorFile")) {
                continue;
            }
            command.add(argument);
        }
        command.add("-XX:ErrorFile=" + errors.resolve("hs_err_pid%p.log"));
        command.add("-XX:-CreateCoredumpOnCrash");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Child.class.getName());
        command.add(scenario);
        command.add(work.toString());

        Process process = new ProcessBuilder(command)
                .directory(LinuxFontGoldens.moduleDirectory().toFile())
                .redirectOutput(stdout.toFile())
                .redirectError(stderr.toFile())
                .start();
        if (!process.waitFor(CHILD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
            fail("child JVM '" + scenario + "' did not finish within " + CHILD_TIMEOUT_SECONDS + " s; stderr:\n"
                    + LinuxFontGoldens.tail(stderr, 40));
        }
        List<Path> errorFiles;
        try (Stream<Path> files = Files.list(errors)) {
            errorFiles = files.sorted().toList();
        }
        Path result = work.resolve(RESULT);
        if (process.exitValue() != 0 || !errorFiles.isEmpty() || !Files.isRegularFile(result)) {
            StringBuilder message = new StringBuilder("child JVM '").append(scenario).append("' exited with ")
                    .append(process.exitValue()).append("; fatal error logs: ").append(errorFiles);
            for (Path errorFile : errorFiles) {
                message.append('\n').append(fatalErrorHeader(errorFile));
            }
            message.append("\nlast stderr lines:\n").append(LinuxFontGoldens.tail(stderr, 40))
                   .append("\ncommand: ").append(command);
            fail(message.toString());
        }
        return FontGoldens.parse(Files.readString(result, StandardCharsets.UTF_8));
    }

    /** The lines of an {@code hs_err} file up to and including the problematic frame. */
    private static String fatalErrorHeader(Path errorFile) throws IOException {
        List<String> lines = Files.readAllLines(errorFile, StandardCharsets.ISO_8859_1);
        StringBuilder header = new StringBuilder();
        for (int i = 0; i < Math.min(lines.size(), 24); i++) {
            header.append(lines.get(i)).append('\n');
            if (i > 0 && lines.get(i - 1).startsWith("# Problematic frame:")) {
                break;
            }
        }
        return header.toString();
    }

    /**
     * The child JVM: {@code Child <scenario> <work directory>}, working directory {@code modules/javafx.graphics}.
     * Writes {@link #RESULT} into the work directory and exits with 0. It is not a test class.
     */
    static final class Child {

        private Child() {
        }

        public static void main(String[] args) throws IOException {
            String scenario = args[0];
            Path work = Path.of(args[1]);
            PrismFontFactory factory = PrismFontFactory.getFontFactory();
            Map<String, String> result = new TreeMap<>();
            result.put("factory", factory.getClass().getName());
            switch (scenario) {
                case SCAN -> scan(factory, work, result);
                case RELOAD -> reload(factory, work, result);
                default -> throw new IllegalArgumentException("unknown scenario " + scenario);
            }
            Files.writeString(work.resolve(RESULT), FontGoldens.format(result, List.of(scenario)),
                              StandardCharsets.UTF_8);
            System.exit(0);
        }

        private static void scan(PrismFontFactory factory, Path work, Map<String, String> result) {
            // The lookup builds the platform maps that the scan adds to.
            result.put("scan.ahem.before", fileName(factory.getFontResource("Ahem", null, false)));
            PrismFontFactoryScanShim.populateFontFileNameMapGeneric(factory, work.resolve("fonts").toString());
            result.put("scan.ahem.file", fileName(factory.getFontResource("Ahem", null, false)));
        }

        private static void reload(PrismFontFactory factory, Path work, Map<String, String> result)
                throws IOException {
            Path first = copy(work, "first");
            result.put("reload.first", load(factory, first));
            // A second file with the same full name takes over the embedded-font entry of the first one, so the
            // next load of the first path registers its cached resource with the rasterizer again.
            result.put("reload.second", load(factory, copy(work, "second")));
            // A new file under the first path; the face FreeType opened on the old file is not affected.
            Path replacement = Files.write(work.resolve("replacement.tmp"), NOT_A_FONT);
            Files.move(replacement, first, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            result.put("reload.replaced", load(factory, first));
        }

        /** {@code loadEmbeddedFont} as {@code Font.loadFont(url, 12)} calls it for a file. */
        private static String load(PrismFontFactory factory, Path file) {
            PGFont[] fonts = factory.loadEmbeddedFont(null, file.toString(), 12f, true, false);
            return fonts == null ? null : fonts[0].getFullName() + ";" + fileName(fonts[0].getFontResource());
        }

        private static String fileName(FontResource resource) {
            return resource == null ? null : resource.getFileName();
        }
    }
}
