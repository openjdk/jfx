/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.networking.worker;

import javafx.concurrent.Task;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * This is a base class for tasks which are going to execute some kind of command at
 * the process level against the operation system.
 */
public abstract class BaseCommandTask<V> extends Task<V> {
    private static enum StandardStream {STDERR, STDOUT};
    private JobModel jobModel;
    private int exitVal;
    private CountDownLatch doneSignal;
    private static final Executor executor = Executors.newFixedThreadPool(4, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread th = new Thread(r);
            th.setDaemon(true);
            return th;
        }
    });

    BaseCommandTask() { }

    protected final JobModel executeCommand(String... command) throws Exception {
        jobModel = new JobModel();
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            Process proc = pb.start();

            doneSignal = new CountDownLatch(2);
            executor.execute(new StreamProcessor(proc.getInputStream(), StandardStream.STDOUT));
            executor.execute(new StreamProcessor(proc.getErrorStream(), StandardStream.STDERR));

            exitVal = proc.waitFor();
            jobModel.setResult(exitVal);
            doneSignal.await();

        } catch (Throwable t) {
            t.printStackTrace();
            jobModel.setResult(-1);
        }
        return jobModel;
    }

    private void captureOutputStream(InputStream is, CountDownLatch inDoneSignal) {
        try {
            String delimiter = System.getProperty("line.separator");
            Scanner scanner = new Scanner(is);
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine()).append(delimiter);
            }
            jobModel.setStdOutText(sb.toString());
            inDoneSignal.countDown();
        } finally {
            try {
                is.close();
            } catch (IOException ie) {
                ie.printStackTrace();
            }
        }
    }

    private class StreamProcessor extends Thread {
        private InputStream is;
        private StandardStream type;

        StreamProcessor(InputStream is, StandardStream type) {
            this.is = is;
            this.type = type;
        }

        @Override
        public void run() {
            if (type.equals(StandardStream.STDOUT)) {
                captureOutputStream(is, doneSignal);
            } else { //process STDERR
                try {

                    String delimiter = System.getProperty("line.separator");
                    Scanner scanner = new Scanner(is);
                    StringBuilder sb = new StringBuilder();
                    while (scanner.hasNextLine()) {
                        sb.append(scanner.nextLine()).append(delimiter);
                    }
                    jobModel.setStdErrText(sb.toString());
                    doneSignal.countDown();
                } catch (java.util.NoSuchElementException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (type.equals(StandardStream.STDERR)) {
                            is.close();
                        }
                    } catch (IOException ie) {
                        ie.printStackTrace();
                    }
                }
            }
        }
    }
}
