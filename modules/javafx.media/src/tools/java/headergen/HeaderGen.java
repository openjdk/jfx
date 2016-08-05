/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package headergen;

import com.sun.media.jfxmedia.MediaError;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

public class HeaderGen {

    private static void generateFile(File file) throws IOException {
        PrintStream os = new PrintStream(file);
        os.println("/*");
        os.println("* Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.");
        os.println("* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.");
        os.println("*");
        os.println("* This code is free software; you can redistribute it and/or modify it");
        os.println(" * under the terms of the GNU General Public License version 2 only, as");
        os.println("* published by the Free Software Foundation.  Oracle designates this");
        os.println("* particular file as subject to the \"Classpath\" exception as provided");
        os.println("* by Oracle in the LICENSE file that accompanied this code.");
        os.println("*");
        os.println("* This code is distributed in the hope that it will be useful, but WITHOUT");
        os.println("* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or");
        os.println("* FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License");
        os.println("* version 2 for more details (a copy is included in the LICENSE file that");
        os.println("* accompanied this code).");
        os.println("*");
        os.println("* You should have received a copy of the GNU General Public License version");
        os.println("* 2 along with this work; if not, write to the Free Software Foundation,");
        os.println("* Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.");
        os.println("*");
        os.println("* Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA");
        os.println("* or visit www.oracle.com if you need additional information or have any");
        os.println("* questions.");
        os.println("*");
        os.println("* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        os.println("* This header is automatically generated from MediaError java class.");
        os.println("* Don't edit this file. Rather add or remove values from the class and");
        os.println("* make the build system to rebuild the file.");
        os.println(" */");
        os.println();
        os.println("#ifndef _JFXMEDIA_ERRORS_H_");
        os.println("#define _JFXMEDIA_ERRORS_H_");
        os.println();

        for (MediaError error : MediaError.values()) {
            os.println("#define    " + error.name() + "    " + error.code());
        }

        os.println();
        os.println("#endif // _JFXMEDIA_ERRORS_H_");

        os.close();
    }

    public static void main(String[] arg) {
        if (arg.length == 2) {
            File file = null;
            try {
                file = new File(arg[0]);
                if (!file.exists())
                {
                    System.out.println("HeaderGen: Creating header file at path: " + file.getAbsolutePath());
                    file.createNewFile();
                    generateFile(file);
                } else {
                    String errorFilePath = arg[1] + "/" + MediaError.class.getCanonicalName().replace(".", "/") + ".java";
                    File errorFile = new File(errorFilePath);
                    if (errorFile.lastModified() <= file.lastModified()) {
                        System.out.println("HeaderGen: no need to regenerate header file.");
                    } else {
                        System.out.println("HeaderGen: re-creating header file at path: " + file.getAbsolutePath());
                        generateFile(file);
                    }
                }
            } catch (IOException ioex) {
                String path = (file != null) ? file.getAbsolutePath() : null;
                System.err.println("HeaderGen IO error: " + ioex.getMessage() + ": " + path);
                System.exit(-2);
            }
        } else {
            System.err.println("Too few arguments.\nUsage:\n\tHeaderGen <absolute path to header file> <absolute path to sources root>\n");
            System.exit(-1);
        }
    }
}
