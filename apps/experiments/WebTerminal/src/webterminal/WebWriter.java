/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates.
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

/** A Writer that inserts the written text into a WebTerminal.
 */

public class WebWriter extends java.io.Writer
{
    protected WebTerminal terminal;

    protected StringBuilder sbuf = new StringBuilder();
    char kind;

    /** Which port is this?
     * @return 'O': if output; 'E': if error stream; 'P': if prompt text
     */
    public char getKind() { return kind; }

    public WebWriter (WebTerminal terminal, char kind) {
        this.terminal = terminal;
        this.kind = kind;
    }

    public synchronized void write (int x) {
        sbuf.append((char) x);
        //WTDebug.println("after write1 "+WTDebug.toQuoted(String.valueOf(new char[]{(char)x})));
        if (x == '\n')
            flush();
    }

    public void write (String str) {
        sbuf.append(str);
        //WTDebug.println("after writeS "+WTDebug.toQuoted(str));
        flush();
    }

    public synchronized void write (char[] data, int off, int len) {
        sbuf.append(data, off, len);
        //WTDebug.println("after writeN len:"+String.valueOf(data,off,len));
        flush();
    }

    public synchronized void flush() {
        StringBuilder s = sbuf;
        //WTDebug.println("WebWr.flush "+WTDebug.toQuoted(sbuf.toString()));

        if (s.length() > 0) {
            // FIXME optimize by passing sbuf to insertOutput?
            terminal.insertOutput(s.toString(), kind);
            s.setLength(0);
        }
    }

    public void close () {
        flush();
    }
}
