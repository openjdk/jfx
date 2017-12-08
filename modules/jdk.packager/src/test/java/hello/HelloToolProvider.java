/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package hello;

import java.io.FileNotFoundException;
import java.util.spi.ToolProvider;

public class HelloToolProvider {

    public static void main(String... args) throws FileNotFoundException {
        ToolProvider javaPackager = ToolProvider.findFirst("javapackager").get();
        if (javaPackager == null) {
            System.err.println("Cannot find tool provider for javapackager");
        } else {
            if (args.length == 0) {
                System.out.println("Running packager with arg[0]: -version");
                javaPackager.run(System.out, System.err, "-version");
            } else {
                int i = 0;
                for (String arg : args) {
                    System.out.println("Running packager with arg[" + i++ + "]: " + arg);
                }
                javaPackager.run(System.out, System.err, args);
            }
        }
    }
}

