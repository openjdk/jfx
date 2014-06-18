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

import org.w3c.dom.*;
import java.io.*;

/** Write out a Node in XML text format.
 * Currently only used for debugging.
 */

public class NodeWriter {
    Writer out;
    public boolean asciiOnly = true;

    public NodeWriter(Writer out) {
        this.out = out;
    }

    public static void writeNode (Node node, Writer out) throws IOException {
        new NodeWriter(out).writeNode(node);
    }

    public static String writeNodeToString(Node node) {
        try {
            StringWriter wr = new StringWriter();
            new NodeWriter(wr).writeNode(node);
            return wr.toString();
        }
        catch (Throwable ex) {
            return "writeNodeToString threw "+ex;
            }
    }
    
    void writeNodeName(Node node) throws IOException {
        out.write(node.getNodeName());
    }

    /** Control abbreviation of repeated characters in text.
        If there are more than this many identical characters
        in a row, abbreviate.  If 0, don't abbreviate.  */
    int abbrevRepeatedMinimum = 3;

    void writeData(String data, boolean attribute) throws IOException {
        int start = 0;
        int end = data.length();
        for (int i = 0; i < end;  i++) {
            char ch = data.charAt(i);
            String ent = null;
            int count = 1;
            if (abbrevRepeatedMinimum > 0) {
                while (i+count < end && data.charAt(i+count)==ch)
                    count++;
            }
            if (ch == '&')
                ent = "&amp;";
            else if (attribute && ch == '\"')
                ent = "&quot;";
            else if (! attribute && ch == '<')
                ent = "&lt;";
            else if (! attribute && ch == '>')
                ent = "&gt;";
            else if (ch < ' ' || (asciiOnly && ch >= 127))
                ent = "&#x"+Integer.toHexString(ch)+';';
            if (ent != null) {
                if (i > start)
                    out.write(data, start, i-start);
                start = i+1;
                out.write(ent);
            } else if (count >= abbrevRepeatedMinimum) {
                out.write(data, start, i-start+1);
                start = i+1;
            }
            if (count >= abbrevRepeatedMinimum) {
                out.write("\\[*"+count+']');
                i += count-1;
                start = i+1;
            }
        }
        if (end > start)
            out.write(data, start, end-start);
    }

    public void writeNode (Node node) throws IOException {
        switch (node.getNodeType()) {
        case Node.DOCUMENT_NODE:
        case Node.DOCUMENT_FRAGMENT_NODE:
            writeNodeChildren(node);
            break;

        case Node.ELEMENT_NODE:
            Element el = (Element) node;
            out.write('<');
            writeNodeName(node);
            out.write("@"+Integer.toHexString(System.identityHashCode(el)));
            NamedNodeMap attrs = el.getAttributes();
            int nattrs = attrs.getLength();
            for (int i = 0;  i < nattrs;  i++) {
                writeNode(attrs.item(i));
            }
            if (node.getFirstChild() == null)
                out.write("/>");
            else {
                out.write('>');
                writeNodeChildren(node);
                out.write("</");
                writeNodeName(node);
                out.write('>');
            }
            break;

        case Node.ATTRIBUTE_NODE:
            Attr at = (Attr) node;
            out.write(' ');
            writeNodeName(node);
            out.write('=');
            out.write('"');
            writeData(at.getValue(), true);
            out.write('"');
            break;
        
        case Node.TEXT_NODE:
            writeData(((Text) node).getData(), false);
            break;

        case Node.CDATA_SECTION_NODE:
            writeData(node.getNodeValue(), false);
            break;
       
        case Node.DOCUMENT_TYPE_NODE:
        default:
            ;
        }
    }

    public void writeNodeChildren (Node node) throws IOException {
        for (Node ch = node.getFirstChild(); ch != null;
             ch = ch.getNextSibling()) {
            writeNode(ch);
        }
    }
}
