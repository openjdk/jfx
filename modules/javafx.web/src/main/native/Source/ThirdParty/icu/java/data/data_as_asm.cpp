/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "pkg_genc.h"
#include "toolutil.h"
#include <stdio.h>

// Simple tool to convert the icudt*.dat into icudt*_dat.s, it is based
// on pkgdata.cpp.
int main(int argc, char* argv[]) {
    if (argc < 5) {
        fprintf(stderr, "%s: <assembler_type> <data_file> <out_dir> <entry_point>\n", argv[0]);
        return 1;
    }

#ifdef CAN_GENERATE_OBJECTS
    // For Windows, icu tools can generate object code directly without going to
    // assembly route.
    // Generate icudt*l_dat.obj file into the <out_dir>.
    // UNUSED(argv[1])
    writeObjectCode(argv[2], argv[3], argv[4], NULL, NULL, NULL, 0, TRUE);
#else
    if (!checkAssemblyHeaderName(argv[1])) {
        fprintf(stderr, "%s: Unable to recogonize assembler type:%s\n", argv[0], argv[1]);
        return 2;
    }

    // Generates icudt*l_dat.s file into the <out_dir>.
    writeAssemblyCode(argv[2], argv[3], argv[4], NULL, NULL, 0);
#endif
    return 0;
}
