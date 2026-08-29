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

/*
 * wkjstub - a recording stub implementation of the wkj_* C ABI.
 *
 * This header declares the pieces shared between the hand-written runtime
 * (wkjstub_runtime.c) and the generated stub bodies and tables
 * (wkjstub_generated.c, produced by gen-wkjstub.pl from the ABI header).
 *
 * Test library only. It is never shipped and never linked into jfxwebkit.
 */

#ifndef WKJSTUB_H
#define WKJSTUB_H

#include <stdint.h>
#include <stddef.h>

#if defined(_MSC_VER)
#  define WKJSTUB_EXPORT __declspec(dllexport)
#else
#  define WKJSTUB_EXPORT __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Type kinds. One character per C type, chosen so that a signature string
 * ("<return><param>...") maps one to one onto an FFM FunctionDescriptor:
 *
 *   v void            (no layout)
 *   b int8_t/uint8_t  JAVA_BYTE
 *   h int16_t/uint16_t JAVA_SHORT
 *   i int32_t/uint32_t JAVA_INT
 *   l int64_t/uint64_t JAVA_LONG   (includes wkj_ref)
 *   f float           JAVA_FLOAT
 *   d double          JAVA_DOUBLE
 *   p any pointer     ADDRESS
 *
 * Two further kinds appear only in recorded arguments, never in a signature:
 *
 *   s  a UTF-16 string argument (const uint16_t* followed by its int32_t length);
 *      the pointed-to characters were copied at call time
 *   a  a primitive array argument (const T* followed by its int32_t length);
 *      the pointed-to bytes were copied at call time
 *
 * A recorded call has exactly one argument entry per C parameter, so argument
 * indices line up with FunctionDescriptor argument indices on the Java side.
 */
#define WKJSTUB_KIND_VOID    'v'
#define WKJSTUB_KIND_BYTE    'b'
#define WKJSTUB_KIND_SHORT   'h'
#define WKJSTUB_KIND_INT     'i'
#define WKJSTUB_KIND_LONG    'l'
#define WKJSTUB_KIND_FLOAT   'f'
#define WKJSTUB_KIND_DOUBLE  'd'
#define WKJSTUB_KIND_POINTER 'p'
#define WKJSTUB_KIND_STRING  's'
#define WKJSTUB_KIND_ARRAY   'a'

#define WKJSTUB_MAX_ARGS 24

typedef struct WKJStubArg {
    int32_t  kind;
    int64_t  bits;       /* scalar value, raw float/double bits, or pointer address */
    void*    blob;       /* captured payload for 's' and 'a', otherwise NULL */
    int32_t  blob_bytes;
    int32_t  is_null;    /* 1 when the pointer parameter was NULL */
} WKJStubArg;

/* ---- entry points used by the generated stub bodies --------------------- */

void wkjstub_arg_scalar(WKJStubArg* a, int32_t kind, int64_t bits);
void wkjstub_arg_pointer(WKJStubArg* a, const void* p);
void wkjstub_arg_string(WKJStubArg* a, const uint16_t* s, int32_t length);
void wkjstub_arg_array(WKJStubArg* a, const void* data, int32_t length, int32_t element_size);

/*
 * Records one call, clears the calling thread's exception slot exactly as the
 * real library clears it on entry to every wkj_* function, and then arms a
 * pending exception if one was scheduled for this function name.
 */
void wkjstub_record(const char* name, const WKJStubArg* args, int32_t argc);

/* Programmed return values; each returns 1 when a value was programmed. */
int32_t wkjstub_programmed_i64(const char* name, int64_t* out);
int32_t wkjstub_programmed_f64(const char* name, double* out);
/*
 * The caller-provides-the-buffer string return of contract 2. Copies a
 * programmed string into result_buf and returns WKJ_STR_OK, or WKJ_STR_NULL
 * when the programmed value is null or nothing was programmed, or
 * WKJ_STR_OVERFLOW with the required capacity in *out_length.
 */
int32_t wkjstub_programmed_out_string(const char* name, uint16_t* result_buf,
                                      int32_t result_cap, int32_t* out_length);
/* Fills an out-array from programmed bytes; returns the element count, or -1. */
int32_t wkjstub_programmed_fill(const char* name, void* out, int32_t out_capacity,
                                int32_t element_size);

/* ---- tables emitted by gen-wkjstub.pl ----------------------------------- */

typedef struct WKJStubSymbol {
    const char* name;
    const char* signature;   /* return kind followed by one kind per parameter */
    int32_t     throws;      /* 1 when the DOM spec marks the function as raising */
} WKJStubSymbol;

typedef struct WKJStubField {
    const char* name;
    int32_t     offset;
    int32_t     size;     /* bytes, including every element of an array member  */
    char        kind;     /* the ELEMENT kind for an array member               */
    int32_t     elements; /* 1 for a scalar member, the extent for an array     */
} WKJStubField;

typedef struct WKJStubStruct {
    const char*         name;
    int32_t             size;
    int32_t             field_count;
    const WKJStubField* fields;
} WKJStubStruct;

typedef struct WKJStubHostSlot {
    const char* name;        /* dotted path, e.g. "core.release" */
    int32_t     offset;      /* byte offset inside WKJHost */
    const char* signature;   /* return kind followed by one kind per parameter */
} WKJStubHostSlot;

extern const WKJStubSymbol   wkjstub_symbol_table[];
extern const int32_t         wkjstub_symbol_table_size;
extern const WKJStubStruct   wkjstub_struct_table[];
extern const int32_t         wkjstub_struct_table_size;
extern const WKJStubHostSlot wkjstub_host_slot_table[];
extern const int32_t         wkjstub_host_slot_table_size;

/* The installed host table, or NULL. Owned by the runtime. */
const void* wkjstub_host_bytes(void);

/* Generated typed dispatch over the installed host table. */
int32_t wkjstub_fire_host_slot(int32_t slot, const int64_t* argv, int32_t argc, int64_t* out_ret);

#ifdef __cplusplus
}
#endif

#endif /* WKJSTUB_H */
