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

package test.com.sun.glass.ui.monocle;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Monocle on X11 boots: a child JVM with {@code -Dglass.platform=Monocle -Dmonocle.platform=X11
 * -Dprism.order=sw -Dx11.geometry=640x480 -Dx11.input=true} shows a Stage, prints the primary screen bounds and
 * exits 0, and the bounds are the X11 window geometry. The child gets this JVM's options (the module path, the
 * export argfiles and the native-access grant of the surefire argLine) and class path. Windowed geometry keeps
 * X11Screen from grabbing the keyboard; {@code monocle.cursor.enabled=false} keeps it from creating an
 * X11Cursor, which reaches for the accelerated screen the sw pipeline never creates. Skips only without a
 * {@code DISPLAY}. Pointer input through XTest is left to the touch suite the manager runs on this platform.
 */
@EnabledOnOs(OS.LINUX)
public class X11MonocleBootTest {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    /** The child: shows a Stage on Monocle X11, prints the primary screen bounds and exits. */
    public static final class Child extends Application {

        public static void main(String[] args) {
            Application.launch(Child.class, args);
        }

        @Override
        public void start(Stage stage) {
            stage.setScene(new Scene(new StackPane(), WIDTH, HEIGHT));
            stage.show();
            Rectangle2D bounds = Screen.getPrimary().getBounds();
            System.out.println("bounds=" + (int) bounds.getWidth() + "x" + (int) bounds.getHeight());
            System.out.println("platform=" + System.getProperty("glass.platform") + "/"
                    + System.getProperty("monocle.platform"));
            Platform.exit();
        }
    }

    private static Path moduleDirectory() {
        Path directory = Path.of("").toAbsolutePath();
        if (!Files.isRegularFile(directory.resolve("pom.xml"))) {
            fail("expected the working directory to be modules/javafx.graphics but it is " + directory);
        }
        return directory;
    }

    @Test
    public void monocleX11ShowsAStageOfTheRequestedGeometry() throws IOException, InterruptedException {
        assumeTrue(System.getenv("DISPLAY") != null, "no DISPLAY: Monocle X11 needs an X server");
        Path work = moduleDirectory().resolve("target").resolve("x11-monocle-boot");
        Files.createDirectories(work);
        Path log = work.resolve("child.log");
        Path errors = work.resolve("hs_err");
        Files.createDirectories(errors);
        clearStaleErrorFiles(errors);
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (argument.startsWith("-agentlib") || argument.startsWith("-javaagent")
                    || argument.startsWith("-Xrunjdwp") || argument.startsWith("-Xdebug")
                    || argument.startsWith("-XX:ErrorFile") || argument.startsWith("-Djavafx.toolkit")
                    || argument.startsWith("-Dglass.platform") || argument.startsWith("-Dprism.order")) {
                continue;
            }
            command.add(argument);
        }
        command.add("-XX:ErrorFile=" + errors.resolve("hs_err_pid%p.log"));
        command.add("-Dglass.platform=Monocle");
        command.add("-Dmonocle.platform=X11");
        command.add("-Dprism.order=sw");
        command.add("-Dx11.geometry=" + WIDTH + "x" + HEIGHT);
        command.add("-Dx11.input=true");
        command.add("-Dmonocle.cursor.enabled=false");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Child.class.getName());
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(moduleDirectory().toFile())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile());
        builder.environment().remove("WAYLAND_DISPLAY");
        Process process = builder.start();
        if (!process.waitFor(120, TimeUnit.SECONDS)) {
            process.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
            fail("the Monocle X11 child did not exit; log:\n" + tail(log));
        }
        List<Path> errorFiles;
        try (Stream<Path> files = Files.list(errors)) {
            errorFiles = files.sorted().toList();
        }
        String output = Files.readString(log, StandardCharsets.UTF_8);
        if (process.exitValue() != 0 || !errorFiles.isEmpty()) {
            fail("the Monocle X11 child exited with " + process.exitValue() + ", error files " + errorFiles
                    + "; log:\n" + tail(log) + "\ncommand: " + command);
        }
        assertTrue(output.contains("platform=Monocle/X11"), () -> "child log:\n" + output);
        String bounds = output.lines().filter(line -> line.startsWith("bounds=")).findFirst()
                .orElseGet(() -> fail("no bounds line in the child log:\n" + output));
        assertEquals("bounds=" + WIDTH + "x" + HEIGHT, bounds);
    }

    /** Removes the crash files of earlier runs, so that only a crash of this run can fail it. */
    private static void clearStaleErrorFiles(Path errors) throws IOException {
        try (Stream<Path> files = Files.list(errors)) {
            for (Path file : files.toList()) {
                Files.deleteIfExists(file);
            }
        }
    }

    private static String tail(Path file) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            return String.join("\n", lines.subList(Math.max(0, lines.size() - 60), lines.size()));
        } catch (IOException e) {
            return "(no log: " + e + ")";
        }
    }
}
