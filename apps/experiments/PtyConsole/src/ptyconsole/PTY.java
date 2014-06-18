/*
 * Copyright (c) 2011, 2014 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ptyconsole;

import java.io.*;

/** Wrap a process running in a pseudo-teletype (pty).
 * Uses native code. It should probably be portable to most modern Unix-like
 * platforms, but this has not been tested.
 */

public class PTY {
    int pid;
    int fdm;

    public InputStream fromChildOutput;
    public OutputStream toChildInput;

    public PTY(String[] args, String termname) {
        String ldpath = System.getProperty("java.library.path");
        int startpath = 0;
        String pathsep = System.getProperty("path.separator");
        String filesep = System.getProperty("file.separator");
        String termdir = "";
        while (ldpath.length() > 0 ) {
            int seppos = ldpath.indexOf(pathsep, startpath);
            String dir;
            if (seppos < 0) {
                dir = ldpath.substring(startpath);
                ldpath = "";
            } else {
                dir = ldpath.substring(startpath, seppos);
                ldpath = ldpath.substring(seppos+1);
            }
            String tname = dir + filesep
                + termname.charAt(0) + filesep + termname;
            if (new File(tname).exists()) {
                termdir = new File(dir).getAbsolutePath();
                break;
            }
        }
        int nargs = args.length;
        byte[][] bargs = new byte[nargs][];
        for (int i = 0; i < nargs;  i++) {
            bargs[i] = args[i].getBytes();
        }
        System.err.println("before init termname:"+termname+" termdir:"+termdir+" filesep:"+filesep);
        fdm = init(bargs, termname, termdir + filesep);
        toChildInput = new OutputStream() {
                public void write(int b) {
                    writeToChildInput(fdm, b);
                }
                public void write(byte[] b, int off, int len) {
                    writeToChildInput(fdm, b, off, len);
                };
                public void write(byte[] b) {
                    writeToChildInput(fdm, b, 0, b.length);
                };
            };
        fromChildOutput = new InputStream() {
                public int read() {
                    return readFromChildOutput(fdm);
                };
                public int read(byte[] b, int off, int len) {
                    return readFromChildOutput(fdm, b, off, len);
                };
                public int read(byte[] b) {
                    return readFromChildOutput(fdm, b, 0, b.length);
                };
            };
    }

    private static native int init(byte[][] args, String termname, String termdir);

    private static native void writeToChildInput(int fdm, byte[] buf, int start, int length);
    private static native void writeToChildInput(int fdm, int b);

    private static native int readFromChildOutput(int fdm, byte[] buf, int start, int length);
    private static native int readFromChildOutput(int fdm);

    /** Should be called by application when window size changes.
     * This notifies the pty system of this fact.
     * @param nrows number of rows (width of logical screen in lines)
     * @param ncols number of coluns (width of logical screen in characters)
     * @param pixw width of logical screen in pixels
     * @param pixh height of logical screen in pixels
     */
    public void setWindowSize(int nrows, int ncols, int pixw, int pixh) {
        setWindowSize(fdm, nrows, ncols, pixw, pixh);
    }

    private static native void setWindowSize(int fdm, int nrows, int ncols, int pixw, int pixh);

    static { System.loadLibrary("pty"); }
}
