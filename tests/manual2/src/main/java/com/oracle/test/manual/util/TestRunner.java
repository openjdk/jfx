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
package com.oracle.test.manual.util;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.concurrent.Future;

/**
 * Runs a manual test via a separate java process.
 */
public class TestRunner {

    public interface Client {
        public void onProcessFinished(int exitCode, Throwable error, LocalDateTime time);

        public void onOutput(char ch, boolean stdout);
    }

    private static final String javaExecutablePath = initJavaExecutablePath();

    public static void execute(String className, Client client) {
        // locations
        File processDir = new File(".");
        String classPath = "build/classes";

        String[] cmd = {
            javaExecutablePath,
            "-ea",
            "-Djavafx.enablePreview=true",
            "--enable-native-access=javafx.graphics",
            "-Dfile.encoding=UTF-8",
            "-Dstdout.encoding=UTF-8",
            "-Dstderr.encoding=UTF-8",
            "-p", "../../build/sdk/lib",
            "--add-modules=javafx.base,javafx.graphics,javafx.controls,javafx.fxml",
            "-cp", classPath,
            className
        };
        String[] env = null;
        try {
            Process p = Runtime.getRuntime().exec(cmd, env, processDir);
            new Monitor(p.getInputStream(), true, client).start();
            new Monitor(p.getErrorStream(), false, client).start();
            new StatusTracker(p.onExit(), client).start();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static class StatusTracker extends Thread {
        private final Future<Process> future;
        private final Client client;

        public StatusTracker(Future<Process> f, Client client) {
            this.future = f;
            this.client = client;
        }

        @Override
        public void run() {
            try {
                Process p = future.get();
                int result = p.exitValue();
                setResult(result, null);
            } catch (Throwable e) {
                setResult(-1, e);
            }
        }

        private void setResult(int result, Throwable err) {
            client.onProcessFinished(result, err, LocalDateTime.now());
        }
    }

    private static class Monitor extends Thread {
        private final InputStream in;
        private final boolean stdout;
        private final Client client;

        public Monitor(InputStream in, boolean stdout, Client client) {
            this.in = in;
            this.stdout = stdout;
            this.client = client;
        }

        @Override
        public void run() {
            try {
                for (;;) {
                    int c = in.read();
                    if (c < 0) {
                        return;
                    }
                    char ch = (char)c;
                    (stdout ? System.out : System.err).append(ch);
                    client.onOutput(ch, stdout);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private static String initJavaExecutablePath() {
        String path = ProcessHandle.current().info().command().orElseThrow();
        IO.println(path);
        return path;
    }
}
