/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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

#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include "Trace.h"

static int decTraceLevel = NWT_TRACE_INVALID;
static FILE *decTraceFile = NULL;

void TraceInit();

void TraceImpl(int level, jboolean cr, const char *string, ...)
{
    va_list args;
    if (decTraceLevel < NWT_TRACE_OFF) {
        TraceInit();
    }
    if (level <= decTraceLevel) {
        if (cr) {
            switch (level) {
            case NWT_TRACE_ERROR:
                fprintf(decTraceFile, "(E) ");
                break;
            case NWT_TRACE_WARNING:
                fprintf(decTraceFile, "(W) ");
                break;
            case NWT_TRACE_INFO:
                fprintf(decTraceFile, "(I) ");
                break;
            case NWT_TRACE_VERBOSE:
                fprintf(decTraceFile, "(V) ");
                break;
            case NWT_TRACE_VERBOSE2:
                fprintf(decTraceFile, "(X) ");
                break;
            default:
                fprintf(decTraceFile, "(%d) ", level);
                break;
            }
        }

        va_start(args, string);
        vfprintf(decTraceFile, string, args);
        va_end(args);

        if (cr) {
            fprintf(decTraceFile, "\n");
        }
        fflush(decTraceFile);
    }
}

void TraceInit() {
    char *decTraceLevelString = NULL;
    size_t size = 0;

    decTraceLevel = NWT_TRACE_OFF;
    if ((_dupenv_s(&decTraceLevelString, &size, "NWT_TRACE_LEVEL") == 0)
            && (decTraceLevelString != NULL)) {
        int traceLevelTmp = -1;
        int args = sscanf_s(decTraceLevelString, "%d", &traceLevelTmp);
        if (args > 0 && traceLevelTmp > NWT_TRACE_INVALID) {
            decTraceLevel = traceLevelTmp;
        }
        free(decTraceLevelString);
    }

    char *decTraceFileName = NULL;
    if ((_dupenv_s(&decTraceFileName, &size, "NWT_TRACE_FILE") == 0)
            && (decTraceFileName != NULL)) {
        if (fopen_s(&decTraceFile, decTraceFileName, "w") != 0) {
            printf("(E): Error opening trace file %s\n", decTraceFileName);
        }
        free(decTraceFileName);
    }
    if (!decTraceFile) {
        decTraceFile = stdout;
    }
}
