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

package webterminal;
import java.io.*;

/** A Simple OutputStream that decodes UTF-8 to a WebWriter.
 */

public class WebOutputStream extends OutputStream {
    WebWriter out;
    int partialChar;
    int bytesNeeded = 0;

    public WebOutputStream(WebWriter out) { this.out = out;}

    public void write(int b) {
        if (b >= 0) {
            partialChar = b;
            bytesNeeded = 0;
        }
        else if ((b & 0xC0) == 0x40) { // continuation byte
            partialChar = (partialChar << 6) | (b & 0x3F);
            bytesNeeded--;
        }
        else if ((b & 0xE0) == 0xC0) { // 1st of 2
            partialChar = b & 0x1F;
            bytesNeeded = 1;
        }
        else if ((b & 0xF0) == 0xE0) { // 1st of 3
            partialChar = b & 0xF;
            bytesNeeded = 2;
        }
        else if ((b & 0xF8) == 0xF0) { // 1st of 4
            partialChar = b & 0x7;
            bytesNeeded = 3;
        }
        else if ((b & 0xFC0) == 0xF8) { // 1st of 5
            partialChar = b & 0x3;
            bytesNeeded = 4;
        }
        if (bytesNeeded == 0) {
            int ch = partialChar;
            if (ch > 0x10000) {
                out.write((char) (((ch - 0x10000) >> 10) + 0xD800));
                ch = ((ch & 0x3FF) + 0xDC00);
            }
            out.write((char) ch);
        }
    }
    public void flush() { out.flush(); }
}
