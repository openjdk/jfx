/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
#include "glass_evloop.h"

#include <glib.h>
#include <malloc.h>

static GSList * evloopHookList;

#define GEVL_HOOK_REGISTRATION_IMPL(ptr) ((GevlHookRegistrationImpl *) ptr)

typedef struct _GevlHookRegistrationImpl {
    GevlHookFunction hookFn;
    void * data;
} GevlHookRegistrationImpl;

void
glass_evloop_initialize() {
}

void
glass_evloop_finalize() {
    GSList * ptr = evloopHookList;
    while (ptr != NULL) {
        free(ptr->data);
        ptr = g_slist_next(ptr);
    }

    g_slist_free(evloopHookList);
    evloopHookList = NULL;
}

void
glass_evloop_call_hooks(GdkEvent * event) {
    GSList * ptr = evloopHookList;
    while (ptr != NULL) {
        GevlHookRegistrationImpl * hookReg =
                GEVL_HOOK_REGISTRATION_IMPL(ptr->data);
        hookReg->hookFn(event, hookReg->data);

        ptr = g_slist_next(ptr);
    }
}

GevlHookRegistration
glass_evloop_hook_add(GevlHookFunction hookFn, void * data) {
    GevlHookRegistrationImpl * hookReg =
            (GevlHookRegistrationImpl *)
                malloc(sizeof(GevlHookRegistrationImpl));

    if (hookReg != NULL) {
        hookReg->hookFn = hookFn;
        hookReg->data = data;

        evloopHookList = g_slist_prepend(evloopHookList, hookReg);
    }

    return hookReg;
}

void
glass_evloop_hook_remove(GevlHookRegistration hookReg) {
    evloopHookList = g_slist_remove(evloopHookList, hookReg);
    free(hookReg);
}

