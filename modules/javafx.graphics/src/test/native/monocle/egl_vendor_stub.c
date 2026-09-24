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
 * egl_vendor_stub.c - a stand-in for the library -Dmonocle.egl.lib names, for
 * test.com.sun.glass.ui.monocle.EglVendorNativeTest. Not built by CMake: the test compiles it at
 * run time with the host C compiler,
 *
 *   cc -shared -fPIC -o libmonocle_egl_stub.so egl_vendor_stub.c            (the normal variant)
 *   cc -shared -fPIC -DSTUB_CHOOSE_CONFIG_FAILS -o ... egl_vendor_stub.c    (doEglChooseConfig -> -1)
 *
 * and skips when no compiler is on PATH. Including monocle_egl_ext.h by path with nothing but the C
 * library on the include path is itself part of the test: the vendor contract must compile with no
 * JNI header. Keep this file dependency-free (libc only).
 *
 * Every function of the contract records one entry "name(arg, ...)" and returns a fixed value.
 * Two sinks carry the same entries:
 *   - in memory: doTestGetLog (one extra export, not part of the contract) returns all entries so far
 *     as a single "; "-separated string, never cleared;
 *   - a file: when the environment variable MONOCLE_EGL_STUB_LOG names a file, every entry is appended
 *     to it as one newline-terminated line (fopen "a", flushed and closed per entry, so a parent
 *     process can read it after the child that loaded the stub has exited). Same tokens as the
 *     in-memory form.
 * Handles are logged as decimal integers. doEglChooseConfig logs exactly the SEVEN int32_t of Prism's
 * GLPixelFormat.Attributes array the contract passes (RED_SIZE, GREEN_SIZE, BLUE_SIZE, ALPHA_SIZE,
 * DEPTH_SIZE, DOUBLEBUFFER, ONSCREEN; no terminator, nothing is scanned for), e.g.
 *   doEglChooseConfig(22136, [8, 8, 8, 8, 0, 1, 1])
 * The cursor image is logged as its length and the sum of its bytes.
 */

#include "../../../main/native-glass/monocle/egl/monocle_egl_ext.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define STUB_EXPORT __attribute__((visibility("default")))

#define LOG_CAPACITY 8192
#define ENTRY_CAPACITY 256

static char log_buf[LOG_CAPACITY];
static size_t log_len = 0;

/* Appends one complete entry to the in-memory log and, when MONOCLE_EGL_STUB_LOG is set, to that file. */
static void log_entry(const char *entry) {
    const char *path;
    size_t n = strlen(entry);
    size_t sep = (log_len > 0) ? 2 : 0;
    if (log_len + sep + n < LOG_CAPACITY) {
        if (sep) {
            memcpy(log_buf + log_len, "; ", 2);
            log_len += 2;
        }
        memcpy(log_buf + log_len, entry, n);
        log_len += n;
        log_buf[log_len] = '\0';
    }
    path = getenv("MONOCLE_EGL_STUB_LOG");
    if (path != NULL && path[0] != '\0') {
        FILE *f = fopen(path, "a");
        if (f != NULL) {
            fputs(entry, f);
            fputc('\n', f);
            fflush(f);
            fclose(f);
        }
    }
}

/* ------------------------------------------------------------------------------------------------
 * EGLAcceleratedScreen
 * ---------------------------------------------------------------------------------------------- */

STUB_EXPORT int64_t getNativeWindowHandle(const char *cardId) {
    char e[ENTRY_CAPACITY];
    snprintf(e, sizeof e, "getNativeWindowHandle(%s)", cardId == NULL ? "null" : cardId);
    log_entry(e);
    return 0x1234;
}

STUB_EXPORT int64_t getEglDisplayHandle(void) {
    log_entry("getEglDisplayHandle()");
    return 0x5678;
}

STUB_EXPORT uint8_t doEglInitialize(void *eglDisplay) {
    char e[ENTRY_CAPACITY];
    snprintf(e, sizeof e, "doEglInitialize(%lld)", (long long) (intptr_t) eglDisplay);
    log_entry(e);
    return 1;
}

STUB_EXPORT uint8_t doEglBindApi(int32_t api) {
    char e[ENTRY_CAPACITY];
    snprintf(e, sizeof e, "doEglBindApi(%d)", (int) api);
    log_entry(e);
    return 1;
}

STUB_EXPORT int64_t doEglChooseConfig(int64_t eglDisplay, int32_t *attribs) {
    char e[ENTRY_CAPACITY];
    if (attribs == NULL) {
        snprintf(e, sizeof e, "doEglChooseConfig(%lld, [null])", (long long) eglDisplay);
    } else {
        /* exactly the seven ints of GLPixelFormat.Attributes; there is no terminator to look for */
        snprintf(e, sizeof e, "doEglChooseConfig(%lld, [%d, %d, %d, %d, %d, %d, %d])",
                 (long long) eglDisplay, (int) attribs[0], (int) attribs[1], (int) attribs[2],
                 (int) attribs[3], (int) attribs[4], (int) attribs[5], (int) attribs[6]);
    }
    log_entry(e);
#ifdef STUB_CHOOSE_CONFIG_FAILS
    return -1;
#else
    return 0x9ABC;
#endif
}

STUB_EXPORT int64_t doEglCreateWindowSurface(int64_t eglDisplay, int64_t config, int64_t nativeWindow) {
    char e[ENTRY_CAPACITY];
    snprintf(e, sizeof e, "doEglCreateWindowSurface(%lld, %lld, %lld)",
             (long long) eglDisplay, (long long) config, (long long) nativeWindow);
    log_entry(e);
    return 0xDEF0;
}

STUB_EXPORT int64_t doEglCreateContext(int64_t eglDisplay, int64_t config) {
    char e[ENTRY_CAPACITY];
    snprintf(e, sizeof e, "doEglCreateContext(%lld, %lld)", (long long) eglDisplay, (long long) config);
    log_entry(e);
    return 0x1357;
}

STUB_EXPORT uint8_t doEglMakeCurrent(int64_t eglDisplay, int64_t drawSurface, int64_t readSurface,
                                     int64_t eglContext) {
    char e[ENTRY_CAPACITY];
    snprintf(e, sizeof e, "doEglMakeCurrent(%lld, %lld, %lld, %lld)", (long long) eglDisplay,
             (long long) drawSurface, (long long) readSurface, (long long) eglContext);
    log_entry(e);
    return 1;
}

STUB_EXPORT uint8_t doEglSwapBuffers(int64_t eglDisplay, int64_t eglSurface) {
    char e[ENTRY_CAPACITY];
    snprintf(e, sizeof e, "doEglSwapBuffers(%lld, %lld)", (long long) eglDisplay, (long long) eglSurface);
    log_entry(e);
    return 1;
}

/* ------------------------------------------------------------------------------------------------
 * EGLPlatform / EGLScreen
 * ---------------------------------------------------------------------------------------------- */

STUB_EXPORT int32_t doGetNumberOfScreens(void) {
    log_entry("doGetNumberOfScreens()");
    return 2;
}

static void log_screen_query(const char *name, int32_t idx) {
    char e[ENTRY_CAPACITY];
    snprintf(e, sizeof e, "%s(%d)", name, (int) idx);
    log_entry(e);
}

STUB_EXPORT int64_t doGetHandle(int32_t idx) {
    log_screen_query("doGetHandle", idx);
    return (int64_t) idx + 1;
}

STUB_EXPORT int32_t doGetDepth(int32_t idx) {
    log_screen_query("doGetDepth", idx);
    return 32;
}

STUB_EXPORT int32_t doGetWidth(int32_t idx) {
    log_screen_query("doGetWidth", idx);
    return 800;
}

STUB_EXPORT int32_t doGetHeight(int32_t idx) {
    log_screen_query("doGetHeight", idx);
    return 480;
}

STUB_EXPORT int32_t doGetOffsetX(int32_t idx) {
    log_screen_query("doGetOffsetX", idx);
    return 0;
}

STUB_EXPORT int32_t doGetOffsetY(int32_t idx) {
    log_screen_query("doGetOffsetY", idx);
    return 0;
}

STUB_EXPORT int32_t doGetDpi(int32_t idx) {
    log_screen_query("doGetDpi", idx);
    return 96;
}

STUB_EXPORT int32_t doGetNativeFormat(int32_t idx) {
    log_screen_query("doGetNativeFormat", idx);
    return 1;
}

STUB_EXPORT float doGetScale(int32_t idx) {
    log_screen_query("doGetScale", idx);
    return 1.0f;
}

/* ------------------------------------------------------------------------------------------------
 * EGLCursor
 * ---------------------------------------------------------------------------------------------- */

STUB_EXPORT void doInitCursor(int32_t width, int32_t height) {
    char e[ENTRY_CAPACITY];
    snprintf(e, sizeof e, "doInitCursor(%d, %d)", (int) width, (int) height);
    log_entry(e);
}

STUB_EXPORT void doSetCursorVisibility(uint8_t val) {
    char e[ENTRY_CAPACITY];
    snprintf(e, sizeof e, "doSetCursorVisibility(%u)", (unsigned) val);
    log_entry(e);
}

STUB_EXPORT void doSetLocation(int32_t x, int32_t y) {
    char e[ENTRY_CAPACITY];
    snprintf(e, sizeof e, "doSetLocation(%d, %d)", (int) x, (int) y);
    log_entry(e);
}

STUB_EXPORT void doSetCursorImage(const uint8_t *img, int32_t length) {
    char e[ENTRY_CAPACITY];
    unsigned long sum = 0;
    int32_t i;
    if (img != NULL) {
        for (i = 0; i < length; i++) {
            sum += img[i];
        }
    }
    snprintf(e, sizeof e, "doSetCursorImage(%s, %d, sum=%lu)", img == NULL ? "null" : "img", (int) length, sum);
    log_entry(e);
}

/* ------------------------------------------------------------------------------------------------
 * Test hook (not part of the contract): the in-memory sink
 * ---------------------------------------------------------------------------------------------- */

STUB_EXPORT const char *doTestGetLog(void) {
    return log_buf;
}
