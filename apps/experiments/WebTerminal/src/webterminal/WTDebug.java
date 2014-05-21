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
import org.w3c.dom.*;

/** Some debugging utilities. */

public class WTDebug {
static 
    PrintStream origErr;
    public static void init() {
        if (origErr == null)
            origErr = System.err;
    }

    static {
        init();
    }

    public static void print(Object obj) {
        origErr.print(""+obj);
    }

    public static void println(Object obj) {
        origErr.println(""+obj);
    }

    public static String pnode(org.w3c.dom.Node n) {

        if (n == null) return "(null)";
        if (n instanceof CharacterData)
            return n.toString()+'\"'+toQuoted(((CharacterData)n).getData())+'\"'+"@"+Integer.toHexString(System.identityHashCode(n));
        return n+"/"+n.getNodeName()+"@"+Integer.toHexString(System.identityHashCode(n));
    }

    public static String toQuoted(String str) {
        int len = str.length();
        StringBuilder buf = new StringBuilder();
        for (int i = 0;  i < len;  i++) {
            char ch = str.charAt(i);
            if (ch == '\n')
                buf.append("\\n");
            else if (ch == '\r')
                buf.append("\\r");
            else if (ch == '\t')
                buf.append("\\t");
            else if (ch == '\033')
                buf.append("\\E");
            else if (ch < ' ' || ch >= 127)
                buf.append("\\"+(char)(((ch>>6)&7)+'0')+(char)(((ch>>3)&7)+'0')+(char)((ch&7)+'0'));
            else {
                if (ch == '\"' || ch == '\'')
                    buf.append('\\');
                buf.append(ch);
            }
        }
        return buf.toString();
    }

}
