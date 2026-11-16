/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

#ifndef __MF_TRACE_H__
#define __MF_TRACE_H__

#include <stdio.h>
#include <stdarg.h>
#include <string.h>

#define TRACE_ENABLE 0

// Enable trace categories
// Decoder (mfwrapper)
#define DECODER_SINK_EVENTS 0
#define DECODER_FIRST_PTS   0
#define DECODER_OUTPUT_PTS  0
#define DECODER_INPUT_PTS   0

// Demux
#define DEMUX_SINK_EVENTS        0
#define DEMUX_SRC_EVENTS         0
#define DEMUX_RELOAD             0
#define DEMUX_READ_SAMPLE        0
#define DEMUX_FIRST_AND_LAST_PTS 0
#define DEMUX_OUTPUT_PTS         0
#define DEMUX_TASK               0

// Byte Stream
#define BYTE_STREAM 0

#if TRACE_ENABLE
static inline const char *trace_filename(const char *path)
{
    const char *p;

    p = strrchr(path, '\\');
    if (p)
        return p + 1;

    p = strrchr(path, '/');
    if (p)
        return p + 1;

    return path;
}

static inline void trace_impl(
    const char *file,
    int line,
    const char *func,
    const char *fmt,
    ...)
{
    va_list ap;

    fprintf(stderr, "JFXMEDIA [%s:%d %s] ",
            trace_filename(file),
            line,
            func);

    va_start(ap, fmt);
    vfprintf(stderr, fmt, ap);
    va_end(ap);
}

#define TRACE(cat, fmt, ...)                                     \
    do {                                                         \
        if (cat) {                                               \
            trace_impl(__FILE__, __LINE__, __func__,             \
                       fmt, ##__VA_ARGS__);                      \
        }                                                        \
    } while (0)
#else // MF_TRACE_ENABLE
#define TRACE(cat, fmt, ...) ((void)0)
#endif //MF_TRACE_ENABLE

#endif // __MF_TRACE_H__
