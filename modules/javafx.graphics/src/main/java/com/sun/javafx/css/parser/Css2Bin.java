/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

import javafx.css.Stylesheet;

import java.io.File;
import java.io.IOException;

/** Css2Bin <input file> [<output file name>]
 * java com.sun.javafx.css.parser.Css2Bin input.css output.bss
 *  If no output file is given, then the input file name is used with an extension of 'bss'x
 */
public final class Css2Bin {
    public static void main(String args[]) throws Exception {

        if ( args.length < 1 ) throw new IllegalArgumentException("expected file name as argument");

        try {
            String ifname = args[0];
            String ofname = (args.length > 1) ?
                args[1] : ifname.substring(0, ifname.lastIndexOf('.')+1).concat("bss");

            convertToBinary(ifname, ofname);

        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }

    public static void convertToBinary(String ifname, String ofname) throws IOException {

        if (ifname == null || ofname == null) {
            throw new IllegalArgumentException("parameters cannot be null");
        }

        if (ifname.equals(ofname)) {
            throw new IllegalArgumentException("input file and output file cannot be the same");
        }

        final File source = new File(ifname);
        final File destination = new File(ofname);
        Stylesheet.convertToBinary(source, destination);

    }

}
