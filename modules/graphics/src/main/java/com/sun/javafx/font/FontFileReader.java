/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;

/*
 * Utility class to read font files.
 */
class FontFileReader implements FontConstants {
    String filename;
    long filesize;
    RandomAccessFile raFile;

    public FontFileReader(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    /**
     * Opens the file.
     * @return returns true if the file opened, false if the file was opened
     *  already or if it failed to open the file.
     * @throws PrivilegedActionException
     */
    public synchronized boolean openFile() throws PrivilegedActionException {
        if (raFile != null) {
            return false;
        }
        raFile = AccessController.doPrivileged(
                (PrivilegedAction<RandomAccessFile>) () -> {
                    try {
                        return new RandomAccessFile(filename, "r");
                    } catch (FileNotFoundException fnfe) {
                        return null;
                    }
                }
        );
        if (raFile != null) {
            try {
                filesize = raFile.length();
                return true;
            } catch (IOException e) {
            }
        }
        return false;
    }

    public synchronized void closeFile() throws IOException {
        if (raFile != null) {
            raFile.close();
            raFile = null;
            readBuffer = null;
        }
    }

    public synchronized long getLength() {
        return filesize;
    }

    public synchronized void reset() throws IOException {
        if (raFile != null) {
            raFile.seek(0);
        }
    }

    static class Buffer {
        byte[] data;
        int pos;
        int orig;

        /**
         * @param data the buffer
         * @param bufStart the starting position within the data array.
         * {@code pos} is considered to be the start of this Buffer object.
         * There is no protection against reading past the end, caller is
         * assumed to be careful.
         */
        Buffer(byte[] data, int bufStart) {
            this.orig = this.pos = bufStart;
            this.data = data;
        }

        int getInt(int tpos) {
            tpos += orig;
            int val = data[tpos++]&0xff;
            val <<= 8;
            val |= data[tpos++]&0xff;
            val <<= 8;
            val |= data[tpos++]&0xff;
            val <<= 8;
            val |= data[tpos++]&0xff;
            return val;
        }

        int getInt() {
            int val = data[pos++]&0xff;
            val <<= 8;
            val |= data[pos++]&0xff;
            val <<= 8;
            val |= data[pos++]&0xff;
            val <<= 8;
            val |= data[pos++]&0xff;
            return val;
        }

        short getShort(int tpos) {
            tpos += orig;
            int val = data[tpos++]&0xff;
            val <<= 8;
            val |= data[tpos++]&0xff;
            return (short)val;
        }

        short getShort() {
            int val = data[pos++]&0xff;
            val <<= 8;
            val |= data[pos++]&0xff;
            return (short)val;
        }

        char getChar(int tpos) {
            tpos += orig;
            int val = data[tpos++]&0xff;
            val <<= 8;
            val |= data[tpos++]&0xff;
            return (char)val;
        }

        char getChar() {
            int val = data[pos++]&0xff;
            val <<= 8;
            val |= data[pos++]&0xff;
            return (char)(val);
        }

        void position(int newPos) {
            pos = orig + newPos;
        }

        int capacity() {
            return data.length-orig;
        }

        byte get() {
            return data[pos++];
        }

        byte get(int tpos) {
            tpos += orig;
            return data[tpos];
        }

        void skip(int nbytes) {
            pos += nbytes;
        }

        void get(int startPos, byte[] dest, int destPos, int destLen) {
            System.arraycopy(data, orig+startPos, dest, destPos, destLen);
        }
    }

    /**
     * Called internally to readBlock(). Don't use directly.
     * Caller must ensure dataLen < buffer length
     * This method will sanity check that you aren't reading past
     * the end of the file.
     * @return 0 if there was a problem, else number of bytes read.
     */
    synchronized private int readFromFile(byte[] buffer,
                                  long seekPos, int requestedLen) {
        try {
            raFile.seek(seekPos);
            /* Remind - if bytesRead < requestedLen, repeat */
            int bytesRead = raFile.read(buffer, 0, requestedLen);
            return bytesRead;
        } catch (IOException e) {
            if (PrismFontFactory.debugFonts) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    /* To read the file header we issue several small reads
     * Since these will be unbuffered, try to help performance by
     * doing it ourselves.
     * In the typical organisation of TrueType fonts, at least
     * those shipped by MS, 1024 is more than sufficient to read the
     * TTC header, directory offset table, and 'head', 'OS/2', and 'hhea'
     * tables. We will read the 'name' table too and that tends to be
     * large and far into the file. Since we expect to read that after
     * the other tables, it should be possible to rework or abstract
     * this into something that doesn't use RandomAccessFile for the
     * ME platforms that don't have that API support.
     */
    private static final int READBUFFERSIZE = 1024;
    private byte[] readBuffer;
    private int readBufferLen;
    private int readBufferStart;
    synchronized public Buffer readBlock(int offset, int len) {
        if (readBuffer == null) {
            readBuffer = new byte[READBUFFERSIZE];
            readBufferLen = 0; // length of valid contents.
        }

        if (len <= READBUFFERSIZE) { /* use cache*/
            if (readBufferStart <= offset &&
                readBufferStart+readBufferLen >= offset+len) {
                /* cache hit */
                return new Buffer(readBuffer, offset - readBufferStart);
            } else { /* fill cache */
                readBufferStart = offset;
                readBufferLen = (offset+READBUFFERSIZE > filesize) ?
                    (int)filesize - offset : READBUFFERSIZE;
                readFromFile(readBuffer, readBufferStart, readBufferLen);
                return new Buffer(readBuffer, 0);
            }
        } else {
            byte[] data = new byte[len];
            readFromFile(data, offset, len);
            return new Buffer(data, 0);
        }
    }

}
