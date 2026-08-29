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
 * wkjstub runtime: the hand-written half of the recording stub.
 *
 * Holds the call ring buffer, the per-thread exception slot, the query-string
 * arena, the programmed-return table and the installed host table,
 * and exports the wkjstub_* query ABI that tests drive it through. The stub
 * bodies for the wkj_* ABI itself are generated into wkjstub_generated.c;
 * the three functions that need real behaviour (wkj_init, wkj_abi_version,
 * wkj_exception_slot) are implemented here and skipped by the generator.
 */

#include "wkjstub.h"
#include "webkit_java_api.h"

#include <stdlib.h>
#include <string.h>

#if defined(_WIN32)
#  include <windows.h>
#  define WKJSTUB_TLS __declspec(thread)
static SRWLOCK g_lock = SRWLOCK_INIT;
#  define WKJSTUB_LOCK()   AcquireSRWLockExclusive(&g_lock)
#  define WKJSTUB_UNLOCK() ReleaseSRWLockExclusive(&g_lock)
#else
#  include <pthread.h>
#  define WKJSTUB_TLS __thread
static pthread_mutex_t g_lock = PTHREAD_MUTEX_INITIALIZER;
#  define WKJSTUB_LOCK()   pthread_mutex_lock(&g_lock)
#  define WKJSTUB_UNLOCK() pthread_mutex_unlock(&g_lock)
#endif

#define WKJSTUB_RING_CAPACITY  1024
#define WKJSTUB_NAME_MAX        192
#define WKJSTUB_PROGRAMMED_MAX  128
#define WKJSTUB_ARMED_MAX        64
#define WKJSTUB_HOST_MAX       8192

static const uint16_t g_empty_string[1] = { 0 };

/* ------------------------------------------------------------------ arenas */

typedef struct WKJStubArena {
    uint16_t* buffer;
    size_t    capacity;   /* in uint16_t units */
    size_t    used;
} WKJStubArena;

/*
 * Returns of the wkjstub_* query ABI only, reset at the start of every query.
 * The wkj_* ABI itself owns no string memory: a string comes out through a
 * caller-supplied buffer, so there is no lifetime rule to model here.
 */
static WKJSTUB_TLS WKJStubArena t_query_arena;

static void arena_reset(WKJStubArena* a)
{
    if (a->buffer != NULL && a->used > 0) {
        /* Poison, so a Java facade that retained a stale pointer is visible. */
        memset(a->buffer, 0xDE, a->used * sizeof(uint16_t));
    }
    a->used = 0;
}

static uint16_t* arena_alloc(WKJStubArena* a, size_t count)
{
    uint16_t* p;
    if (a->used + count + 1 > a->capacity) {
        size_t want = (a->capacity != 0) ? a->capacity * 2 : 8192;
        while (want < a->used + count + 1) {
            want *= 2;
        }
        p = (uint16_t*) realloc(a->buffer, want * sizeof(uint16_t));
        if (p == NULL) {
            return NULL;
        }
        a->buffer = p;
        a->capacity = want;
    }
    p = a->buffer + a->used;
    a->used += count + 1;
    p[count] = 0;
    return p;
}

static const uint16_t* arena_copy(WKJStubArena* a, const uint16_t* s, int32_t length,
                                  int32_t* out_length)
{
    uint16_t* p;
    if (s == NULL) {
        if (out_length != NULL) {
            *out_length = -1;
        }
        return NULL;
    }
    if (length < 0) {
        length = 0;
    }
    p = arena_alloc(a, (size_t) length);
    if (p == NULL) {
        if (out_length != NULL) {
            *out_length = 0;
        }
        return NULL;
    }
    if (length > 0) {
        memcpy(p, s, (size_t) length * sizeof(uint16_t));
    }
    if (out_length != NULL) {
        *out_length = length;
    }
    return p;
}

/* Widens an ASCII C string into the per-thread query arena. */
static const uint16_t* query_string(const char* s, int32_t* out_length)
{
    size_t i, n;
    uint16_t* p;
    arena_reset(&t_query_arena);
    if (s == NULL) {
        if (out_length != NULL) {
            *out_length = -1;
        }
        return NULL;
    }
    n = strlen(s);
    p = arena_alloc(&t_query_arena, n);
    if (p == NULL) {
        if (out_length != NULL) {
            *out_length = 0;
        }
        return NULL;
    }
    for (i = 0; i < n; i++) {
        p[i] = (uint16_t) (unsigned char) s[i];
    }
    if (out_length != NULL) {
        *out_length = (int32_t) n;
    }
    return p;
}

static int ascii_eq_utf16(const char* a, const uint16_t* b, int32_t b_length)
{
    int32_t i;
    if (a == NULL || b == NULL || b_length < 0) {
        return 0;
    }
    for (i = 0; i < b_length; i++) {
        if (a[i] == 0 || (uint16_t) (unsigned char) a[i] != b[i]) {
            return 0;
        }
    }
    return a[b_length] == 0;
}

static void copy_name(char* dest, const uint16_t* name, int32_t length)
{
    int32_t i;
    if (name == NULL || length < 0) {
        length = 0;
    }
    if (length > WKJSTUB_NAME_MAX - 1) {
        length = WKJSTUB_NAME_MAX - 1;
    }
    for (i = 0; i < length; i++) {
        dest[i] = (char) (name[i] & 0x7f);
    }
    dest[length] = 0;
}

/* --------------------------------------------------------------- recording */

typedef struct WKJStubRecord {
    char       name[WKJSTUB_NAME_MAX];
    int32_t    argc;
    WKJStubArg args[WKJSTUB_MAX_ARGS];
} WKJStubRecord;

static WKJStubRecord g_ring[WKJSTUB_RING_CAPACITY];
static int64_t       g_total;

typedef struct WKJStubProgrammed {
    char      name[WKJSTUB_NAME_MAX];
    int32_t   in_use;
    int32_t   has_i64;
    int64_t   i64;
    int32_t   has_f64;
    double    f64;
    int32_t   has_string;
    uint16_t* string;
    int32_t   string_length;
    int32_t   has_bytes;
    void*     bytes;
    int32_t   bytes_length;
} WKJStubProgrammed;

static WKJStubProgrammed g_programmed[WKJSTUB_PROGRAMMED_MAX];

typedef struct WKJStubArmed {
    char      name[WKJSTUB_NAME_MAX];
    int32_t   in_use;
    int32_t   fired;
    int32_t   type;
    int32_t   code;
    uint16_t* message;
    int32_t   message_length;
} WKJStubArmed;

static WKJStubArmed g_armed[WKJSTUB_ARMED_MAX];

/*
 * The per-thread exception slot. The message lives inline in the slot, so there
 * is nothing to own and nothing to free; a message longer than
 * WKJ_EXC_MESSAGE_MAX code units is truncated and message_length reports what
 * was actually stored, exactly as webkit_java_api.h specifies.
 */
static WKJSTUB_TLS WKJExceptionSlot t_slot;

static void set_slot(int32_t type, int32_t code, const uint16_t* message, int32_t message_length)
{
    t_slot.type = type;
    t_slot.code = code;
    t_slot.message_length = 0;
    if (message != NULL && message_length > 0) {
        if (message_length > WKJ_EXC_MESSAGE_MAX) {
            message_length = WKJ_EXC_MESSAGE_MAX;
        }
        memcpy(t_slot.message, message, (size_t) message_length * sizeof(uint16_t));
        t_slot.message_length = message_length;
    }
}

static void free_args(WKJStubRecord* r)
{
    int32_t i;
    for (i = 0; i < r->argc; i++) {
        free(r->args[i].blob);
        r->args[i].blob = NULL;
        r->args[i].blob_bytes = 0;
    }
    r->argc = 0;
    r->name[0] = 0;
}

void wkjstub_arg_scalar(WKJStubArg* a, int32_t kind, int64_t bits)
{
    a->kind = kind;
    a->bits = bits;
    a->blob = NULL;
    a->blob_bytes = 0;
    a->is_null = 0;
}

void wkjstub_arg_pointer(WKJStubArg* a, const void* p)
{
    a->kind = WKJSTUB_KIND_POINTER;
    a->bits = (int64_t) (intptr_t) p;
    a->blob = NULL;
    a->blob_bytes = 0;
    a->is_null = (p == NULL) ? 1 : 0;
}

static void capture(WKJStubArg* a, int32_t kind, const void* data, int32_t bytes)
{
    a->kind = kind;
    a->bits = (int64_t) (intptr_t) data;
    a->blob = NULL;
    a->blob_bytes = 0;
    a->is_null = (data == NULL) ? 1 : 0;
    if (data != NULL && bytes > 0) {
        a->blob = malloc((size_t) bytes);
        if (a->blob != NULL) {
            memcpy(a->blob, data, (size_t) bytes);
            a->blob_bytes = bytes;
        }
    }
}

void wkjstub_arg_string(WKJStubArg* a, const uint16_t* s, int32_t length)
{
    capture(a, WKJSTUB_KIND_STRING, s, (length > 0) ? length * (int32_t) sizeof(uint16_t) : 0);
}

void wkjstub_arg_array(WKJStubArg* a, const void* data, int32_t length, int32_t element_size)
{
    capture(a, WKJSTUB_KIND_ARRAY, data, (length > 0) ? length * element_size : 0);
}

static WKJStubProgrammed* find_programmed(const char* name, int create)
{
    int32_t i;
    int32_t free_slot = -1;
    for (i = 0; i < WKJSTUB_PROGRAMMED_MAX; i++) {
        if (g_programmed[i].in_use != 0) {
            if (strcmp(g_programmed[i].name, name) == 0) {
                return &g_programmed[i];
            }
        } else if (free_slot < 0) {
            free_slot = i;
        }
    }
    if (create == 0 || free_slot < 0) {
        return NULL;
    }
    memset(&g_programmed[free_slot], 0, sizeof(WKJStubProgrammed));
    strncpy(g_programmed[free_slot].name, name, WKJSTUB_NAME_MAX - 1);
    g_programmed[free_slot].name[WKJSTUB_NAME_MAX - 1] = 0;
    g_programmed[free_slot].in_use = 1;
    return &g_programmed[free_slot];
}

void wkjstub_record(const char* name, const WKJStubArg* args, int32_t argc)
{
    int32_t i;
    int32_t armed_type = 0;
    int32_t armed_code = 0;
    int32_t armed_length = 0;
    uint16_t* armed_message = NULL;
    WKJStubRecord* r;

    /* The library clears the slot on entry to every wkj_* function. */
    t_slot.type = WKJ_EXC_NONE;
    t_slot.code = 0;
    t_slot.message_length = 0;

    if (argc > WKJSTUB_MAX_ARGS) {
        argc = WKJSTUB_MAX_ARGS;
    }

    WKJSTUB_LOCK();
    r = &g_ring[(size_t) (g_total % WKJSTUB_RING_CAPACITY)];
    free_args(r);
    strncpy(r->name, name, WKJSTUB_NAME_MAX - 1);
    r->name[WKJSTUB_NAME_MAX - 1] = 0;
    r->argc = argc;
    for (i = 0; i < argc; i++) {
        /* Takes ownership of any blob capture() allocated for this argument. */
        r->args[i] = args[i];
    }
    g_total++;
    for (i = 0; i < WKJSTUB_ARMED_MAX; i++) {
        if (g_armed[i].in_use != 0 && g_armed[i].fired == 0
                && strcmp(g_armed[i].name, name) == 0) {
            g_armed[i].fired = 1;
            armed_type = g_armed[i].type;
            armed_code = g_armed[i].code;
            armed_length = g_armed[i].message_length;
            armed_message = g_armed[i].message;
            break;
        }
    }
    WKJSTUB_UNLOCK();

    if (armed_type != 0) {
        set_slot(armed_type, armed_code, armed_message, armed_length);
    }
}

int32_t wkjstub_programmed_i64(const char* name, int64_t* out)
{
    int32_t found = 0;
    WKJStubProgrammed* p;
    WKJSTUB_LOCK();
    p = find_programmed(name, 0);
    if (p != NULL && p->has_i64 != 0) {
        *out = p->i64;
        found = 1;
    }
    WKJSTUB_UNLOCK();
    return found;
}

int32_t wkjstub_programmed_f64(const char* name, double* out)
{
    int32_t found = 0;
    WKJStubProgrammed* p;
    WKJSTUB_LOCK();
    p = find_programmed(name, 0);
    if (p != NULL && p->has_f64 != 0) {
        *out = p->f64;
        found = 1;
    }
    WKJSTUB_UNLOCK();
    return found;
}

int32_t wkjstub_programmed_out_string(const char* name, uint16_t* result_buf, int32_t result_cap,
                                      int32_t* out_length)
{
    int32_t status = WKJ_STR_NULL;
    int32_t length = 0;
    WKJStubProgrammed* p;

    *out_length = 0;
    WKJSTUB_LOCK();
    p = find_programmed(name, 0);
    if (p != NULL && p->has_string != 0 && p->string != NULL) {
        length = p->string_length;
        if (result_buf == NULL || length > result_cap) {
            /* Nothing written; the caller learns the capacity it needs. */
            status = WKJ_STR_OVERFLOW;
            *out_length = length;
        } else {
            if (length > 0) {
                memcpy(result_buf, p->string, (size_t) length * sizeof(uint16_t));
            }
            status = WKJ_STR_OK;
            *out_length = length;
        }
    }
    WKJSTUB_UNLOCK();
    return status;
}

int32_t wkjstub_programmed_fill(const char* name, void* out, int32_t out_capacity,
                                int32_t element_size)
{
    int32_t count = -1;
    WKJStubProgrammed* p;
    WKJSTUB_LOCK();
    p = find_programmed(name, 0);
    if (p != NULL && p->has_bytes != 0) {
        int32_t bytes = p->bytes_length;
        int32_t capacity_bytes = (out_capacity > 0) ? out_capacity * element_size : 0;
        if (bytes > capacity_bytes) {
            bytes = capacity_bytes;
        }
        if (out != NULL && bytes > 0) {
            memcpy(out, p->bytes, (size_t) bytes);
        }
        count = (element_size > 0) ? bytes / element_size : 0;
    }
    WKJSTUB_UNLOCK();
    return count;
}

/* ------------------------------------------------------ hand-written wkj_* */

static int32_t wkjstub_host_is_installed(void);

static uint32_t g_abi_version;
static int32_t  g_abi_version_read;

static unsigned char g_host[WKJSTUB_HOST_MAX];
static int32_t       g_host_installed;
static int32_t       g_host_size;
static uint32_t      g_host_abi_version;
static int32_t       g_host_init_result;

/*
 * The library-internal pointer webkit_java_api.h declares. The real library
 * keeps the caller's table and reads it on the WKJHandle hot path; the stub
 * keeps it too, for fidelity, but dispatches upcalls through its own copy so
 * that a test cannot crash the JVM by firing into freed memory.
 */
const WKJHost* wkj_host = NULL;

const void* wkjstub_host_bytes(void)
{
    return (g_host_installed != 0) ? (const void*) g_host : NULL;
}

WKJ_EXPORT uint32_t wkj_abi_version(void)
{
    uint32_t version;
    WKJSTUB_LOCK();
    if (g_abi_version_read == 0) {
        const char* text = getenv("WKJSTUB_ABI_VERSION");
        g_abi_version = (text != NULL) ? (uint32_t) strtoul(text, NULL, 10)
                                       : (uint32_t) WKJ_ABI_VERSION;
        g_abi_version_read = 1;
    }
    version = g_abi_version;
    WKJSTUB_UNLOCK();
    return version;
}

WKJ_EXPORT WKJExceptionSlot* wkj_exception_slot(void)
{
    return &t_slot;
}

WKJ_EXPORT int32_t wkj_init(const WKJHost* host, int32_t host_size, uint32_t abi_version)
{
    WKJStubArg args[3];
    int32_t result;

    wkjstub_arg_pointer(&args[0], host);
    wkjstub_arg_scalar(&args[1], WKJSTUB_KIND_INT, (int64_t) host_size);
    wkjstub_arg_scalar(&args[2], WKJSTUB_KIND_INT, (int64_t) abi_version);
    wkjstub_record("wkj_init", args, 3);

    /*
     * The same checks, in the same order and with the same result codes, that
     * webkit_java_api.h documents. host_size must equal both host->size and the
     * library's own sizeof(WKJHost), so a Java-side layout that disagrees with
     * the C struct is rejected here instead of corrupting memory later.
     */
    if (host == NULL) {
        result = WKJ_INIT_ERR_NULL_HOST;
    } else if (abi_version != wkj_abi_version()) {
        result = WKJ_INIT_ERR_ABI_VERSION;
    } else if (host_size != (int32_t) sizeof(WKJHost) || host->size != host_size
            || host_size > WKJSTUB_HOST_MAX) {
        result = WKJ_INIT_ERR_HOST_SIZE;
    } else if (wkjstub_host_is_installed() != 0) {
        result = WKJ_INIT_ERR_ALREADY_INITED;
    } else {
        WKJSTUB_LOCK();
        memset(g_host, 0, sizeof(g_host));
        memcpy(g_host, host, (size_t) host_size);
        g_host_size = host_size;
        g_host_abi_version = abi_version;
        g_host_installed = 1;
        wkj_host = host;
        WKJSTUB_UNLOCK();
        result = WKJ_INIT_OK;
    }
    WKJSTUB_LOCK();
    g_host_init_result = result;
    WKJSTUB_UNLOCK();
    return result;
}

/* ----------------------------------------------------- wkjstub_* query ABI */

WKJSTUB_EXPORT uint32_t wkjstub_stub_version(void)
{
    return 1u;
}

WKJSTUB_EXPORT void wkjstub_set_abi_version(uint32_t version)
{
    WKJSTUB_LOCK();
    g_abi_version = version;
    g_abi_version_read = 1;
    WKJSTUB_UNLOCK();
}

WKJSTUB_EXPORT void wkjstub_clear_returns(void)
{
    int32_t i;
    WKJSTUB_LOCK();
    for (i = 0; i < WKJSTUB_PROGRAMMED_MAX; i++) {
        free(g_programmed[i].string);
        free(g_programmed[i].bytes);
        memset(&g_programmed[i], 0, sizeof(WKJStubProgrammed));
    }
    for (i = 0; i < WKJSTUB_ARMED_MAX; i++) {
        free(g_armed[i].message);
        memset(&g_armed[i], 0, sizeof(WKJStubArmed));
    }
    WKJSTUB_UNLOCK();
}

/*
 * Clears the call ring, the programmed returns, the armed exceptions and this
 * thread's exception slot. Deliberately leaves the installed host table alone:
 * Java installs it once per process, so a per-test reset must not drop it.
 * Use wkjstub_clear_host() for that.
 */
WKJSTUB_EXPORT void wkjstub_reset(void)
{
    int32_t i;
    wkjstub_clear_returns();
    WKJSTUB_LOCK();
    for (i = 0; i < WKJSTUB_RING_CAPACITY; i++) {
        free_args(&g_ring[i]);
    }
    g_total = 0;
    WKJSTUB_UNLOCK();
    set_slot(0, 0, NULL, 0);
}

WKJSTUB_EXPORT void wkjstub_clear_host(void)
{
    WKJSTUB_LOCK();
    memset(g_host, 0, sizeof(g_host));
    wkj_host = NULL;
    g_host_installed = 0;
    g_host_size = 0;
    g_host_abi_version = 0;
    g_host_init_result = 0;
    WKJSTUB_UNLOCK();
}

WKJSTUB_EXPORT int32_t wkjstub_ring_capacity(void)
{
    return WKJSTUB_RING_CAPACITY;
}

WKJSTUB_EXPORT int64_t wkjstub_call_total(void)
{
    int64_t total;
    WKJSTUB_LOCK();
    total = g_total;
    WKJSTUB_UNLOCK();
    return total;
}

WKJSTUB_EXPORT int32_t wkjstub_call_count(void)
{
    int64_t total = wkjstub_call_total();
    return (total > WKJSTUB_RING_CAPACITY) ? WKJSTUB_RING_CAPACITY : (int32_t) total;
}

/* Caller must hold the lock. */
static WKJStubRecord* record_at(int32_t index)
{
    int64_t count = (g_total > WKJSTUB_RING_CAPACITY) ? WKJSTUB_RING_CAPACITY : g_total;
    if (index < 0 || (int64_t) index >= count) {
        return NULL;
    }
    return &g_ring[(size_t) ((g_total - count + index) % WKJSTUB_RING_CAPACITY)];
}

WKJSTUB_EXPORT const uint16_t* wkjstub_call_name(int32_t index, int32_t* out_length)
{
    char name[WKJSTUB_NAME_MAX];
    WKJStubRecord* r;
    WKJSTUB_LOCK();
    r = record_at(index);
    if (r == NULL) {
        WKJSTUB_UNLOCK();
        return query_string(NULL, out_length);
    }
    strncpy(name, r->name, WKJSTUB_NAME_MAX - 1);
    name[WKJSTUB_NAME_MAX - 1] = 0;
    WKJSTUB_UNLOCK();
    return query_string(name, out_length);
}

WKJSTUB_EXPORT int32_t wkjstub_call_argc(int32_t index)
{
    int32_t argc = -1;
    WKJStubRecord* r;
    WKJSTUB_LOCK();
    r = record_at(index);
    if (r != NULL) {
        argc = r->argc;
    }
    WKJSTUB_UNLOCK();
    return argc;
}

WKJSTUB_EXPORT int32_t wkjstub_call_arg_kind(int32_t index, int32_t arg)
{
    int32_t kind = -1;
    WKJStubRecord* r;
    WKJSTUB_LOCK();
    r = record_at(index);
    if (r != NULL && arg >= 0 && arg < r->argc) {
        kind = r->args[arg].kind;
    }
    WKJSTUB_UNLOCK();
    return kind;
}

WKJSTUB_EXPORT int64_t wkjstub_call_arg(int32_t index, int32_t arg)
{
    int64_t bits = 0;
    WKJStubRecord* r;
    WKJSTUB_LOCK();
    r = record_at(index);
    if (r != NULL && arg >= 0 && arg < r->argc) {
        bits = r->args[arg].bits;
    }
    WKJSTUB_UNLOCK();
    return bits;
}

WKJSTUB_EXPORT int32_t wkjstub_call_arg_is_null(int32_t index, int32_t arg)
{
    int32_t is_null = -1;
    WKJStubRecord* r;
    WKJSTUB_LOCK();
    r = record_at(index);
    if (r != NULL && arg >= 0 && arg < r->argc) {
        is_null = r->args[arg].is_null;
    }
    WKJSTUB_UNLOCK();
    return is_null;
}

/*
 * Returns the captured UTF-16 argument. A NULL return with *out_length == -1
 * means the argument itself was NULL, i.e. Java null; a non-NULL return with
 * *out_length == 0 means the empty string. Keeping those two apart is the
 * whole point of this export.
 */
WKJSTUB_EXPORT const uint16_t* wkjstub_call_arg_string(int32_t index, int32_t arg,
                                                       int32_t* out_length)
{
    const uint16_t* result;
    uint16_t* copy = NULL;
    int32_t length = -1;
    WKJStubRecord* r;

    WKJSTUB_LOCK();
    r = record_at(index);
    if (r != NULL && arg >= 0 && arg < r->argc && r->args[arg].kind == WKJSTUB_KIND_STRING
            && r->args[arg].is_null == 0) {
        length = r->args[arg].blob_bytes / (int32_t) sizeof(uint16_t);
        if (length > 0) {
            copy = (uint16_t*) malloc(((size_t) length + 1) * sizeof(uint16_t));
            if (copy != NULL) {
                memcpy(copy, r->args[arg].blob, (size_t) length * sizeof(uint16_t));
                copy[length] = 0;
            } else {
                length = 0;
            }
        }
    }
    WKJSTUB_UNLOCK();

    if (length < 0) {
        return query_string(NULL, out_length);
    }
    arena_reset(&t_query_arena);
    result = arena_copy(&t_query_arena, (copy != NULL) ? copy : g_empty_string, length, out_length);
    free(copy);
    return result;
}

WKJSTUB_EXPORT int32_t wkjstub_call_arg_bytes(int32_t index, int32_t arg, void* out,
                                              int32_t out_capacity)
{
    int32_t bytes = -1;
    WKJStubRecord* r;
    WKJSTUB_LOCK();
    r = record_at(index);
    if (r != NULL && arg >= 0 && arg < r->argc && r->args[arg].is_null == 0
            && (r->args[arg].kind == WKJSTUB_KIND_ARRAY
                || r->args[arg].kind == WKJSTUB_KIND_STRING)) {
        bytes = r->args[arg].blob_bytes;
        if (bytes > out_capacity) {
            bytes = out_capacity;
        }
        if (out != NULL && bytes > 0) {
            memcpy(out, r->args[arg].blob, (size_t) bytes);
        }
    }
    WKJSTUB_UNLOCK();
    return bytes;
}

WKJSTUB_EXPORT int32_t wkjstub_find_call(const uint16_t* name, int32_t name_length, int32_t from)
{
    int32_t i;
    int32_t count;
    int32_t found = -1;
    WKJSTUB_LOCK();
    count = (g_total > WKJSTUB_RING_CAPACITY) ? WKJSTUB_RING_CAPACITY : (int32_t) g_total;
    for (i = (from < 0) ? 0 : from; i < count; i++) {
        WKJStubRecord* r = record_at(i);
        if (r != NULL && ascii_eq_utf16(r->name, name, name_length) != 0) {
            found = i;
            break;
        }
    }
    WKJSTUB_UNLOCK();
    return found;
}

WKJSTUB_EXPORT int32_t wkjstub_count_calls(const uint16_t* name, int32_t name_length)
{
    int32_t i;
    int32_t count;
    int32_t hits = 0;
    WKJSTUB_LOCK();
    count = (g_total > WKJSTUB_RING_CAPACITY) ? WKJSTUB_RING_CAPACITY : (int32_t) g_total;
    for (i = 0; i < count; i++) {
        WKJStubRecord* r = record_at(i);
        if (r != NULL && ascii_eq_utf16(r->name, name, name_length) != 0) {
            hits++;
        }
    }
    WKJSTUB_UNLOCK();
    return hits;
}

WKJSTUB_EXPORT void wkjstub_set_return_i64(const uint16_t* name, int32_t name_length, int64_t value)
{
    char key[WKJSTUB_NAME_MAX];
    WKJStubProgrammed* p;
    copy_name(key, name, name_length);
    WKJSTUB_LOCK();
    p = find_programmed(key, 1);
    if (p != NULL) {
        p->has_i64 = 1;
        p->i64 = value;
    }
    WKJSTUB_UNLOCK();
}

WKJSTUB_EXPORT void wkjstub_set_return_f64(const uint16_t* name, int32_t name_length, double value)
{
    char key[WKJSTUB_NAME_MAX];
    WKJStubProgrammed* p;
    copy_name(key, name, name_length);
    WKJSTUB_LOCK();
    p = find_programmed(key, 1);
    if (p != NULL) {
        p->has_f64 = 1;
        p->f64 = value;
    }
    WKJSTUB_UNLOCK();
}

/* value == NULL programs a NULL return, i.e. Java null. */
WKJSTUB_EXPORT void wkjstub_set_return_string(const uint16_t* name, int32_t name_length,
                                              const uint16_t* value, int32_t value_length)
{
    char key[WKJSTUB_NAME_MAX];
    WKJStubProgrammed* p;
    uint16_t* copy = NULL;
    if (value != NULL) {
        if (value_length < 0) {
            value_length = 0;
        }
        copy = (uint16_t*) malloc(((size_t) value_length + 1) * sizeof(uint16_t));
        if (copy != NULL) {
            if (value_length > 0) {
                memcpy(copy, value, (size_t) value_length * sizeof(uint16_t));
            }
            copy[value_length] = 0;
        }
    }
    copy_name(key, name, name_length);
    WKJSTUB_LOCK();
    p = find_programmed(key, 1);
    if (p != NULL) {
        free(p->string);
        p->has_string = 1;
        p->string = copy;
        p->string_length = (copy != NULL) ? value_length : 0;
        copy = NULL;
    }
    WKJSTUB_UNLOCK();
    free(copy);
}

WKJSTUB_EXPORT void wkjstub_set_return_bytes(const uint16_t* name, int32_t name_length,
                                             const void* data, int32_t data_bytes)
{
    char key[WKJSTUB_NAME_MAX];
    WKJStubProgrammed* p;
    void* copy = NULL;
    if (data != NULL && data_bytes > 0) {
        copy = malloc((size_t) data_bytes);
        if (copy != NULL) {
            memcpy(copy, data, (size_t) data_bytes);
        }
    }
    copy_name(key, name, name_length);
    WKJSTUB_LOCK();
    p = find_programmed(key, 1);
    if (p != NULL) {
        free(p->bytes);
        p->has_bytes = 1;
        p->bytes = copy;
        p->bytes_length = (copy != NULL) ? data_bytes : 0;
        copy = NULL;
    }
    WKJSTUB_UNLOCK();
    free(copy);
}

/* Sets this thread's exception slot immediately. */
WKJSTUB_EXPORT void wkjstub_raise(int32_t type, int32_t code, const uint16_t* message,
                                  int32_t message_length)
{
    set_slot(type, code, message, message_length);
}

/*
 * Arms a one-shot exception: the next call to the named wkj_* function sets
 * the calling thread's exception slot, exactly as the real library does when
 * a DOM operation raises.
 */
WKJSTUB_EXPORT void wkjstub_arm_exception(const uint16_t* name, int32_t name_length, int32_t type,
                                          int32_t code, const uint16_t* message,
                                          int32_t message_length)
{
    char key[WKJSTUB_NAME_MAX];
    int32_t i;
    uint16_t* copy = NULL;
    if (message != NULL) {
        if (message_length < 0) {
            message_length = 0;
        }
        copy = (uint16_t*) malloc(((size_t) message_length + 1) * sizeof(uint16_t));
        if (copy != NULL) {
            if (message_length > 0) {
                memcpy(copy, message, (size_t) message_length * sizeof(uint16_t));
            }
            copy[message_length] = 0;
        }
    }
    copy_name(key, name, name_length);
    WKJSTUB_LOCK();
    for (i = 0; i < WKJSTUB_ARMED_MAX; i++) {
        if (g_armed[i].in_use == 0) {
            memset(&g_armed[i], 0, sizeof(WKJStubArmed));
            strncpy(g_armed[i].name, key, WKJSTUB_NAME_MAX - 1);
            g_armed[i].name[WKJSTUB_NAME_MAX - 1] = 0;
            g_armed[i].in_use = 1;
            g_armed[i].type = type;
            g_armed[i].code = code;
            g_armed[i].message = copy;
            g_armed[i].message_length = (copy != NULL) ? message_length : 0;
            copy = NULL;
            break;
        }
    }
    WKJSTUB_UNLOCK();
    free(copy);
}

WKJSTUB_EXPORT int32_t wkjstub_exception_pending(void)
{
    return t_slot.type;
}

static int32_t wkjstub_host_is_installed(void)
{
    int32_t installed;
    WKJSTUB_LOCK();
    installed = g_host_installed;
    WKJSTUB_UNLOCK();
    return installed;
}

WKJSTUB_EXPORT int32_t wkjstub_host_installed(void)
{
    return wkjstub_host_is_installed();
}

WKJSTUB_EXPORT int32_t wkjstub_host_size(void)
{
    int32_t size;
    WKJSTUB_LOCK();
    size = g_host_size;
    WKJSTUB_UNLOCK();
    return size;
}

WKJSTUB_EXPORT uint32_t wkjstub_host_abi_version(void)
{
    uint32_t version;
    WKJSTUB_LOCK();
    version = g_host_abi_version;
    WKJSTUB_UNLOCK();
    return version;
}

WKJSTUB_EXPORT int32_t wkjstub_host_init_result(void)
{
    int32_t result;
    WKJSTUB_LOCK();
    result = g_host_init_result;
    WKJSTUB_UNLOCK();
    return result;
}

WKJSTUB_EXPORT int32_t wkjstub_host_slot_count(void)
{
    return wkjstub_host_slot_table_size;
}

WKJSTUB_EXPORT const uint16_t* wkjstub_host_slot_name(int32_t index, int32_t* out_length)
{
    if (index < 0 || index >= wkjstub_host_slot_table_size) {
        return query_string(NULL, out_length);
    }
    return query_string(wkjstub_host_slot_table[index].name, out_length);
}

WKJSTUB_EXPORT const uint16_t* wkjstub_host_slot_signature(int32_t index, int32_t* out_length)
{
    if (index < 0 || index >= wkjstub_host_slot_table_size) {
        return query_string(NULL, out_length);
    }
    return query_string(wkjstub_host_slot_table[index].signature, out_length);
}

WKJSTUB_EXPORT int64_t wkjstub_host_slot_offset(int32_t index)
{
    if (index < 0 || index >= wkjstub_host_slot_table_size) {
        return -1;
    }
    return wkjstub_host_slot_table[index].offset;
}

WKJSTUB_EXPORT int64_t wkjstub_host_slot_pointer(int32_t index)
{
    int64_t value = 0;
    WKJSTUB_LOCK();
    if (g_host_installed != 0 && index >= 0 && index < wkjstub_host_slot_table_size) {
        int32_t offset = wkjstub_host_slot_table[index].offset;
        if (offset >= 0 && offset + (int32_t) sizeof(void*) <= g_host_size) {
            void* p = NULL;
            memcpy(&p, g_host + offset, sizeof(void*));
            value = (int64_t) (intptr_t) p;
        }
    }
    WKJSTUB_UNLOCK();
    return value;
}

WKJSTUB_EXPORT int32_t wkjstub_find_host_slot(const uint16_t* name, int32_t name_length)
{
    int32_t i;
    for (i = 0; i < wkjstub_host_slot_table_size; i++) {
        if (ascii_eq_utf16(wkjstub_host_slot_table[i].name, name, name_length) != 0) {
            return i;
        }
    }
    return -1;
}

/*
 * Calls one installed host callback. argv carries one entry per parameter:
 * integers sign-extended, float as its raw 32-bit pattern, double as its raw
 * 64-bit pattern, pointers as addresses. out_ret receives the return value in
 * the same encoding. Returns 0 on success, -1 unknown slot, -2 slot is NULL,
 * -3 wrong argument count, -4 no host installed.
 */
WKJSTUB_EXPORT int32_t wkjstub_fire_host(int32_t slot, const int64_t* argv, int32_t argc,
                                         int64_t* out_ret)
{
    int64_t ignored = 0;
    if (wkjstub_host_installed() == 0) {
        return -4;
    }
    if (slot < 0 || slot >= wkjstub_host_slot_table_size) {
        return -1;
    }
    return wkjstub_fire_host_slot(slot, argv, argc, (out_ret != NULL) ? out_ret : &ignored);
}

typedef struct WKJStubFireRequest {
    int32_t        slot;
    const int64_t* argv;
    int32_t        argc;
    int64_t        ret;
    int32_t        status;
} WKJStubFireRequest;

#if defined(_WIN32)
static DWORD WINAPI fire_thread_main(LPVOID parameter)
{
    WKJStubFireRequest* r = (WKJStubFireRequest*) parameter;
    r->status = wkjstub_fire_host(r->slot, r->argv, r->argc, &r->ret);
    return 0;
}
#else
static void* fire_thread_main(void* parameter)
{
    WKJStubFireRequest* r = (WKJStubFireRequest*) parameter;
    r->status = wkjstub_fire_host(r->slot, r->argv, r->argc, &r->ret);
    return NULL;
}
#endif

/*
 * Same as wkjstub_fire_host, but from a thread the JVM has never seen. This is
 * the case the JNI port needed AttachCurrentThread for and the one an FFM
 * upcall stub has to survive without any attach at all: WebKit calls Java back
 * from its own worker threads. Blocks until the callback returns.
 * Returns the status of the call, or -5 when the thread could not be started.
 */
WKJSTUB_EXPORT int32_t wkjstub_fire_host_on_foreign_thread(int32_t slot, const int64_t* argv,
                                                           int32_t argc, int64_t* out_ret)
{
    WKJStubFireRequest request;
    request.slot = slot;
    request.argv = argv;
    request.argc = argc;
    request.ret = 0;
    request.status = -5;

#if defined(_WIN32)
    {
        HANDLE thread = CreateThread(NULL, 0, fire_thread_main, &request, 0, NULL);
        if (thread == NULL) {
            return -5;
        }
        WaitForSingleObject(thread, INFINITE);
        CloseHandle(thread);
    }
#else
    {
        pthread_t thread;
        if (pthread_create(&thread, NULL, fire_thread_main, &request) != 0) {
            return -5;
        }
        pthread_join(thread, NULL);
    }
#endif

    if (out_ret != NULL) {
        *out_ret = request.ret;
    }
    return request.status;
}

WKJSTUB_EXPORT int32_t wkjstub_symbol_count(void)
{
    return wkjstub_symbol_table_size;
}

WKJSTUB_EXPORT const uint16_t* wkjstub_symbol_name(int32_t index, int32_t* out_length)
{
    if (index < 0 || index >= wkjstub_symbol_table_size) {
        return query_string(NULL, out_length);
    }
    return query_string(wkjstub_symbol_table[index].name, out_length);
}

WKJSTUB_EXPORT const uint16_t* wkjstub_symbol_signature(int32_t index, int32_t* out_length)
{
    if (index < 0 || index >= wkjstub_symbol_table_size) {
        return query_string(NULL, out_length);
    }
    return query_string(wkjstub_symbol_table[index].signature, out_length);
}

WKJSTUB_EXPORT int32_t wkjstub_struct_count(void)
{
    return wkjstub_struct_table_size;
}

WKJSTUB_EXPORT const uint16_t* wkjstub_struct_name(int32_t index, int32_t* out_length)
{
    if (index < 0 || index >= wkjstub_struct_table_size) {
        return query_string(NULL, out_length);
    }
    return query_string(wkjstub_struct_table[index].name, out_length);
}

WKJSTUB_EXPORT int64_t wkjstub_struct_size(int32_t index)
{
    if (index < 0 || index >= wkjstub_struct_table_size) {
        return -1;
    }
    return wkjstub_struct_table[index].size;
}

WKJSTUB_EXPORT int32_t wkjstub_struct_field_count(int32_t index)
{
    if (index < 0 || index >= wkjstub_struct_table_size) {
        return -1;
    }
    return wkjstub_struct_table[index].field_count;
}

WKJSTUB_EXPORT const uint16_t* wkjstub_struct_field_name(int32_t index, int32_t field,
                                                         int32_t* out_length)
{
    if (index < 0 || index >= wkjstub_struct_table_size || field < 0
            || field >= wkjstub_struct_table[index].field_count) {
        return query_string(NULL, out_length);
    }
    return query_string(wkjstub_struct_table[index].fields[field].name, out_length);
}

WKJSTUB_EXPORT int64_t wkjstub_struct_field_offset(int32_t index, int32_t field)
{
    if (index < 0 || index >= wkjstub_struct_table_size || field < 0
            || field >= wkjstub_struct_table[index].field_count) {
        return -1;
    }
    return wkjstub_struct_table[index].fields[field].offset;
}

WKJSTUB_EXPORT int64_t wkjstub_struct_field_size(int32_t index, int32_t field)
{
    if (index < 0 || index >= wkjstub_struct_table_size || field < 0
            || field >= wkjstub_struct_table[index].field_count) {
        return -1;
    }
    return wkjstub_struct_table[index].fields[field].size;
}

/* For an array member this is the ELEMENT kind; pair it with _elements below. */
WKJSTUB_EXPORT int32_t wkjstub_struct_field_kind(int32_t index, int32_t field)
{
    if (index < 0 || index >= wkjstub_struct_table_size || field < 0
            || field >= wkjstub_struct_table[index].field_count) {
        return -1;
    }
    return (int32_t) (unsigned char) wkjstub_struct_table[index].fields[field].kind;
}

/* 1 for a scalar member, the extent for a fixed-size array member. */
WKJSTUB_EXPORT int32_t wkjstub_struct_field_elements(int32_t index, int32_t field)
{
    if (index < 0 || index >= wkjstub_struct_table_size || field < 0
            || field >= wkjstub_struct_table[index].field_count) {
        return -1;
    }
    return wkjstub_struct_table[index].fields[field].elements;
}

WKJSTUB_EXPORT int32_t wkjstub_symbol_throws(int32_t index)
{
    if (index < 0 || index >= wkjstub_symbol_table_size) {
        return -1;
    }
    return wkjstub_symbol_table[index].throws;
}
