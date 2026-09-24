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
 * The exports of screencast_api.h that are not the work of a former native: the ABI version, the sizeof
 * probe, and the token callback table with its installer. The functions of the natives live in
 * screencast_pipewire.c, where the Java_* functions of commit 033187ad90 are, and storeRestoreToken of
 * that file is what dials the slot.
 */

#include "screencast_api.h"
#include "screencast_pipewire.h"

#include <string.h>

/*
 * The installed table. Static storage, so the slot is NULL - and storeRestoreToken takes its other path -
 * until Java installs a table.
 */
ScTokenCallbacks sc_token_cb;

/*
 * The values screencast_api.h shares with Java are this library's own enumerations; both are visible
 * here, so the two sides are pinned against each other at compile time.
 */
#if defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L
_Static_assert(SC_METHOD_SCREENCAST == XDG_METHOD_SCREENCAST,
               "SC_METHOD_SCREENCAST must be XDG_METHOD_SCREENCAST");
_Static_assert(SC_METHOD_REMOTE_DESKTOP == XDG_METHOD_REMOTE_DESKTOP,
               "SC_METHOD_REMOTE_DESKTOP must be XDG_METHOD_REMOTE_DESKTOP");
_Static_assert(SC_RESULT_OK == RESULT_OK, "SC_RESULT_OK must be RESULT_OK");
_Static_assert(SC_RESULT_ERROR == RESULT_ERROR, "SC_RESULT_ERROR must be RESULT_ERROR");
_Static_assert(SC_RESULT_DENIED == RESULT_DENIED, "SC_RESULT_DENIED must be RESULT_DENIED");
_Static_assert(SC_RESULT_OUT_OF_BOUNDS == RESULT_OUT_OF_BOUNDS,
               "SC_RESULT_OUT_OF_BOUNDS must be RESULT_OUT_OF_BOUNDS");
_Static_assert(SC_RESULT_NO_STREAMS == RESULT_NO_STREAMS,
               "SC_RESULT_NO_STREAMS must be RESULT_NO_STREAMS");
#endif

int32_t sc_abi_version(void) {
    return GLASS_SCREENCAST_ABI_VERSION;
}

int32_t sc_sizeof_token_callbacks(void) {
    return (int32_t) sizeof(ScTokenCallbacks);
}

int32_t sc_set_token_callbacks(const ScTokenCallbacks* cb) {
    if (cb != NULL) {
        sc_token_cb = *cb;
    } else {
        memset(&sc_token_cb, 0, sizeof(sc_token_cb));
    }
    return SC_OK;
}
