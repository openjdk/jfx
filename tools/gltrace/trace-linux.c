/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 
#define _GNU_SOURCE
#include <dlfcn.h>
#include <stdio.h>

#include "os.h"
#include "iolib.h"
#include "trace.h"

static int tLevel = trcLevel;

struct dlfcn_hook {
    void *(*dlopen)(const char *, int, void *);
    int (*dlclose)(void *);
    void *(*dlsym)(void *, const char *, void *);
    void *(*dlvsym)(void *, const char *, const char *, void *);
    char *(*dlerror)(void);
    int (*dladdr)(const void *, Dl_info *);
    int (*dladdr1)(const void *, Dl_info *, void **, int);
    int (*dlinfo)(void *, int, void *, void *);
    void *(*dlmopen)(Lmid_t, const char *, int, void *);
    void *pad[4];
};

static void *trace_dlopen(const char*, int, void*);
static int   trace_dlclose(void *);
static void *trace_dlsym(void *, const char *, void *);
static void *trace_dlvsym(void *, const char *, const char *, void *);
static char *trace_dlerror(void);
static int   trace_dladdr(const void *, Dl_info *);
static int   trace_dladdr1(const void *, Dl_info *, void **, int);
static int   trace_dlinfo(void *, int, void *, void *);
static void *trace_dlmopen(Lmid_t, const char *, int, void *);

static struct dlfcn_hook dlfcn_hook = {
    .dlopen   = trace_dlopen,
    .dlclose  = trace_dlclose,
    .dlsym    = trace_dlsym,
    .dlvsym   = trace_dlvsym,
    .dlerror  = trace_dlerror,
    .dladdr   = trace_dladdr,
    .dladdr1  = trace_dladdr1,
    .dlinfo   = trace_dlinfo,
    .dlmopen  = trace_dlmopen,
    .pad      = {0, 0, 0, 0},
};

struct dlfcn_hook *dlfcn_hook_orig;
struct dlfcn_hook *dlfcn_hook_trace = &dlfcn_hook;

static void *libSelf = NULL;

void    *
trace_dlopen(const char *file, int mode, void *dl_caller)
{
    DLFCN_HOOK_POP();
    void *result = dlopen(file, mode);
    DLFCN_HOOK_PUSH();
    return result;
}

int   
trace_dlclose(void *handle)
{
    DLFCN_HOOK_POP();
    int result = dlclose(handle);
    DLFCN_HOOK_PUSH();
    return result;
}

void *
trace_dlsym(void *handle, const char *name, void *dl_caller)
{
    DLFCN_HOOK_POP();
    void *result = dlsym(libSelf, name);
    if (tLevel >= dbgLevel && result != NULL) {
        fprintf(stderr, "INTERCEPTION: %p %s = %p\n", handle, name, result);
    }
    if (result == NULL) {
        result = dlsym(handle, name);
    }
    DLFCN_HOOK_PUSH();
    return result;
}

void *
trace_dlvsym(void *handle, const char *name, const char *version, void *dl_caller)
{
    DLFCN_HOOK_POP();
    void *result = dlvsym(libSelf, name, version);
    if (tLevel >= dbgLevel && result != NULL) {
        fprintf(stderr, "INTERCEPTION: %p %s.%s = %p\n", handle, name, version, result);
    }
    if (result == NULL) {
        result = dlvsym(handle, name, version);
    }
    DLFCN_HOOK_PUSH();
    return result;
}

char *
trace_dlerror(void)
{
    DLFCN_HOOK_POP();
    char *result = dlerror();
    DLFCN_HOOK_PUSH();
    return result;
}

int   
trace_dladdr(const void *address, Dl_info *info)
{
    DLFCN_HOOK_POP();
    int result = dladdr(address, info);
    DLFCN_HOOK_PUSH();
    return result;
}

int   
trace_dladdr1(const void *address, Dl_info *info, void **extra, int flags)
{
    DLFCN_HOOK_POP();
    int result = dladdr1(address, info, extra, flags);
    DLFCN_HOOK_PUSH();
    return result;
}

int   
trace_dlinfo(void *handle, int request, void *arg, void *dl_caller)
{
    DLFCN_HOOK_POP();
    int result = dlinfo(handle, request, arg);
    DLFCN_HOOK_PUSH();
    return result;
}

void *
trace_dlmopen(Lmid_t nsid, const char *file, int mode, void *dl_caller)
{
    DLFCN_HOOK_POP();
    void *result = dlmopen(nsid, file, mode);
    DLFCN_HOOK_PUSH();
    return result;
}


/*
 *    Init/fini
 */

static void init() __attribute__ ((constructor));
static void
init()
{
    Dl_info info;
    if (dladdr(&init, &info)) {
        libSelf = dlopen(info.dli_fname, RTLD_LAZY|RTLD_NOLOAD);
    }

    iolib_init(IO_WRITE, NULL);
    
    DLFCN_HOOK_INIT();
    DLFCN_HOOK_PUSH();

    if (tLevel >= dbgLevel) {
         fprintf(stderr, "INTERPOSITION STARTED\n");
    }
}

static void fini() __attribute__ ((destructor));
static void fini()
{
    DLFCN_HOOK_POP();
    iolib_fini();
    
    if (tLevel >= dbgLevel) {
         fprintf(stderr, "INTERPOSITION FINISHED\n");
    }
}
