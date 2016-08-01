/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _Included_Trace
#define _Included_Trace

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

/**
 * Trace
 * Trace utility used throughout Java 2D code.  Uses a "level"
 * parameter that allows user to specify how much detail
 * they want traced at runtime.  Tracing is only enabled
 * in debug mode, to avoid overhead running release build.
 */

#define NWT_TRACE_INVALID       -1
#define NWT_TRACE_OFF           0
#define NWT_TRACE_ERROR         1
#define NWT_TRACE_WARNING       2
#define NWT_TRACE_INFO          3
#define NWT_TRACE_VERBOSE       4
#define NWT_TRACE_VERBOSE2      5
#define NWT_TRACE_MAX           (NWT_TRACE_VERBOSE2+1)

void TraceImpl(int level, jboolean cr, const char *string, ...);

#if !defined DEBUG && !defined _DEBUG
    #define Trace(level, string)
    #define Trace1(level, string, arg1)
    #define Trace2(level, string, arg1, arg2)
    #define Trace3(level, string, arg1, arg2, arg3)
    #define Trace4(level, string, arg1, arg2, arg3, arg4)
    #define Trace5(level, string, arg1, arg2, arg3, arg4, arg5)
    #define Trace6(level, string, arg1, arg2, arg3, arg4, arg5, arg6)
    #define Trace7(level, string, arg1, arg2, arg3, arg4, arg5, arg6, arg7)
    #define Trace8(level, string, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8)
    #define TraceLn(level, string)
    #define TraceLn1(level, string, arg1)
    #define TraceLn2(level, string, arg1, arg2)
    #define TraceLn3(level, string, arg1, arg2, arg3)
    #define TraceLn4(level, string, arg1, arg2, arg3, arg4)
    #define TraceLn5(level, string, arg1, arg2, arg3, arg4, arg5)
    #define TraceLn6(level, string, arg1, arg2, arg3, arg4, arg5, arg6)
    #define TraceLn7(level, string, arg1, arg2, arg3, arg4, arg5, arg6, arg7)
    #define TraceLn8(level, string, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8)
#else /* DEBUG  or _DEBUG */
#define Trace(level, string) { \
            TraceImpl(level, JNI_FALSE, string); \
        }
#define Trace1(level, string, arg1) { \
            TraceImpl(level, JNI_FALSE, string, arg1); \
        }
#define Trace2(level, string, arg1, arg2) { \
            TraceImpl(level, JNI_FALSE, string, arg1, arg2); \
        }
#define Trace3(level, string, arg1, arg2, arg3) { \
            TraceImpl(level, JNI_FALSE, string, arg1, arg2, arg3); \
        }
#define Trace4(level, string, arg1, arg2, arg3, arg4) { \
            TraceImpl(level, JNI_FALSE, string, arg1, arg2, arg3, arg4); \
        }
#define Trace5(level, string, arg1, arg2, arg3, arg4, arg5) { \
            TraceImpl(level, JNI_FALSE, string, arg1, arg2, arg3, arg4, arg5); \
        }
#define Trace6(level, string, arg1, arg2, arg3, arg4, arg5, arg6) { \
            TraceImpl(level, JNI_FALSE, string, arg1, arg2, arg3, arg4, arg5, arg6); \
        }
#define Trace7(level, string, arg1, arg2, arg3, arg4, arg5, arg6, arg7) { \
            TraceImpl(level, JNI_FALSE, string, arg1, arg2, arg3, arg4, arg5, arg6, arg7); \
        }
#define Trace8(level, string, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8) { \
            TraceImpl(level, JNI_FALSE, string, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8); \
        }
#define TraceLn(level, string) { \
            TraceImpl(level, JNI_TRUE, string); \
        }
#define TraceLn1(level, string, arg1) { \
            TraceImpl(level, JNI_TRUE, string, arg1); \
        }
#define TraceLn2(level, string, arg1, arg2) { \
            TraceImpl(level, JNI_TRUE, string, arg1, arg2); \
        }
#define TraceLn3(level, string, arg1, arg2, arg3) { \
            TraceImpl(level, JNI_TRUE, string, arg1, arg2, arg3); \
        }
#define TraceLn4(level, string, arg1, arg2, arg3, arg4) { \
            TraceImpl(level, JNI_TRUE, string, arg1, arg2, arg3, arg4); \
        }
#define TraceLn5(level, string, arg1, arg2, arg3, arg4, arg5) { \
            TraceImpl(level, JNI_TRUE, string, arg1, arg2, arg3, arg4, arg5); \
        }
#define TraceLn6(level, string, arg1, arg2, arg3, arg4, arg5, arg6) { \
            TraceImpl(level, JNI_TRUE, string, arg1, arg2, arg3, arg4, arg5, arg6); \
        }
#define TraceLn7(level, string, arg1, arg2, arg3, arg4, arg5, arg6, arg7) { \
            TraceImpl(level, JNI_TRUE, string, arg1, arg2, arg3, arg4, arg5, arg6, arg7); \
        }
#define TraceLn8(level, string, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8) { \
            TraceImpl(level, JNI_TRUE, string, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8); \
        }
#endif /* DEBUG */


/**
 * NOTE: Use the following RlsTrace calls very carefully; they are compiled
 * into the code and should thus not be put in any performance-sensitive
 * areas.
 */

#define RlsTrace(level, string) { \
            TraceImpl(level, JNI_FALSE, string); \
        }
#define RlsTrace1(level, string, arg1) { \
            TraceImpl(level, JNI_FALSE, string, arg1); \
        }
#define RlsTrace2(level, string, arg1, arg2) { \
            TraceImpl(level, JNI_FALSE, string, arg1, arg2); \
        }
#define RlsTrace3(level, string, arg1, arg2, arg3) { \
            TraceImpl(level, JNI_FALSE, string, arg1, arg2, arg3); \
        }
#define RlsTrace4(level, string, arg1, arg2, arg3, arg4) { \
            TraceImpl(level, JNI_FALSE, string, arg1, arg2, arg3, arg4); \
        }
#define RlsTrace5(level, string, arg1, arg2, arg3, arg4, arg5) { \
            TraceImpl(level, JNI_FALSE, string, arg1, arg2, arg3, arg4, arg5); \
        }
#define RlsTraceLn(level, string) { \
            TraceImpl(level, JNI_TRUE, string); \
        }
#define RlsTraceLn1(level, string, arg1) { \
            TraceImpl(level, JNI_TRUE, string, arg1); \
        }
#define RlsTraceLn2(level, string, arg1, arg2) { \
            TraceImpl(level, JNI_TRUE, string, arg1, arg2); \
        }
#define RlsTraceLn3(level, string, arg1, arg2, arg3) { \
            TraceImpl(level, JNI_TRUE, string, arg1, arg2, arg3); \
        }
#define RlsTraceLn4(level, string, arg1, arg2, arg3, arg4) { \
            TraceImpl(level, JNI_TRUE, string, arg1, arg2, arg3, arg4); \
        }
#define RlsTraceLn5(level, string, arg1, arg2, arg3, arg4, arg5) { \
            TraceImpl(level, JNI_TRUE, string, arg1, arg2, arg3, arg4, arg5); \
        }

#ifdef __cplusplus
};
#endif /* __cplusplus */

#endif /* _Included_Trace */
