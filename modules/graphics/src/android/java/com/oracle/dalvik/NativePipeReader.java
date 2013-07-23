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
package com.oracle.dalvik;

import android.os.Handler;

public class NativePipeReader extends Thread { 
   
    /**
     * onTextReceived 
     * callback to return read text from pipe 
     * called on the same thread that NativePipeReader was 
     * created on.
     * 
     * @param text
     */
    public interface OnTextReceivedListener {
        public void onTextReceived(String text);
    }

    public interface Client extends OnTextReceivedListener {
        /**
         * initPipe 
         * called to set up pipe and redirect 
         * 
         * @return int, fd of output end of pipe
         */
        public int initPipe();


        public void onTextReceived(String text);

        /**
         * cleanupPipe 
         * called after last data is read from pipe and thread 
         * is about to exit
         *
         */
        public void cleanupPipe();
    }

    private Client client;
    private Handler handler;

    public NativePipeReader(Client client) {
        this.client = client;
        this.handler = new Handler();    
    }

    private volatile boolean stop = false;

    public void stopReading() {
        stop = true;
    }

    public void run() {
        int fd = client.initPipe();
        while (!stop) {
            String text = readPipe(fd);
            if (text.length() > 0) {
                // only notify client if we
                // actually got something
                client.onTextReceived(text);
            }
        }
        client.cleanupPipe();
    }

    public static NativePipeReader 
    getDefaultReader(OnTextReceivedListener listener) {
        return new NativePipeReader(
                                   new StdoutStderrClient(listener));
    }

    /**
     * readPipe 
     * native method to read text from the 
     * pipe. 
     * this method may(should) block
     * 
     * @return String 
     */
    private native String readPipe(int fd);

    private static class StdoutStderrClient implements Client {

        Handler handler;
        OnTextReceivedListener listener;

        public StdoutStderrClient(OnTextReceivedListener listener) {
            this.handler = new Handler();
            this.listener = listener;
        }

        private native int nativeInitPipe();
        private native void nativeCleanupPipe();

        /**
         * initPipe
         * called to set up pipe and redirect
         *
         * @return int, fd of read end of pipe
         */
        public int initPipe() {
            return nativeInitPipe();
        }

        /**
         * onTextReceived
         * callback to return read text from pipe
         * called on the same thread that NativePipeReader was
         * created on.
         *
         * @param text
         */
        public void onTextReceived(final String text) {
            handler.post(new Runnable() {
                             public void run() {
                                 listener.onTextReceived(text); 
                             }
                         });
        }

        /**
         * cleanupPipe
         * called after last data is read from pipe and thread
         * is about to exit
         */
        public void cleanupPipe() {
            nativeCleanupPipe();
        }
    }
}

