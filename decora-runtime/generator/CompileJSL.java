/*
 * Copyright (c) 2008, 2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.scenario.effect.compiler.JSLC;
import com.sun.scenario.effect.compiler.JSLC.JSLCInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class is only used at build time to generate EffectPeer
 * implementations from JSL definitions, and shouldn't be included in the
 * resulting runtime jar file.
 */
public class CompileJSL {

    private static String readStream(InputStream in) {
        StringBuilder sb = new StringBuilder(1024);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {
            char[] chars = new char[1024];
            int numRead = 0;
            while ((numRead = reader.read(chars)) > -1) {
                sb.append(String.valueOf(chars, 0, numRead));
            }
        } catch (IOException e) {
            System.err.println("Error reading stream");
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                System.err.println("Error closing reader");
            }
        }
        return sb.toString();
    }

    public static String readFile(File file) throws Exception {
        return readStream(new FileInputStream(file));
    }

    public static void main(String[] args) throws Exception {
        JSLCInfo jslcinfo = new JSLCInfo("<jslfile>+");
        int index = jslcinfo.parseArgs(args);
        if (index >= args.length) {
            jslcinfo.error("No JSL file specified");
        }
        while (index < args.length) {
            jslcinfo.shaderName = args[index++];
            JSLC.compile(jslcinfo, jslcinfo.getJSLFile());
        }
    }
}
