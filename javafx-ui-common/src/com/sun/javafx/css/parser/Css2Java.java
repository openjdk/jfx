/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.css.parser;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import com.sun.javafx.css.Stylesheet;
import java.io.*;

/** Css2Java <input file> [<package name> <output file name>]
 * java -classpath <sdk desktop lib path>/javafx-ui-common.jar com.sun.javafx.css.parser.Css2Java input.css full.package.name output.java
 *  If no output file is given, then the input file name is used with an extension of 'java'
 */
public class Css2Java {
    public static void main(String args[]) throws Exception {
        
        if ( args.length < 1) throw new IllegalArgumentException("expected <input file> [<package name> <output file name>] as arguments");

        try {
            String ifname = args[0];
            String packageName = (args.length > 1 ? args[1] : null);
            String ofname = (args.length > 2) ?
                args[2] : ifname.substring(0, ifname.lastIndexOf('.')+1).concat("java");
            
            convertToJava(ifname, ofname, packageName);
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace(System.err);
            System.exit(-1);
        } 
    }

    public static void convertToJava(String ifname, String ofname, String packageName) throws IOException {
        final URL urlIn = new java.net.URL("file", null, ifname);

        CSSParser.EXIT_ON_ERROR = true;
        Stylesheet stylesheet = CSSParser.getInstance().parse(urlIn);

        File outFile = new File(ofname);
        PrintWriter writer = new PrintWriter(outFile);
        
        if (packageName != null && ! packageName.isEmpty()) {
            writer.write("package ");
            writer.write(packageName);
            writer.write(";");
            writer.write("\r\n\r\n");
        }
        
        String className = outFile.getName();
        className = className.substring(0, className.indexOf("."));
        stylesheet.writeJava(writer, className);
        writer.flush();
        writer.close();
    }
}
