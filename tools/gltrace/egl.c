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
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "os.h"
#include "iolib.h"
#include "egl.h"

static void *libEGL = NULL;

/*
 *    Init/fini
 */

static void init() __attribute__ ((constructor));
static void 
init()
{
    libEGL = dlopen("libEGL.so", RTLD_LAZY);
}


/*
 *    EGL
 */

EGLAPI EGLint EGLAPIENTRY 
eglGetError(void)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLDisplay EGLAPIENTRY 
eglGetDisplay(EGLNativeDisplayType display_id)
{
    EGLPROLOG(eglGetDisplay);
    
    putCmd(OPC_eglGetDisplay);
    putPtr((void*)display_id);
    
    uint64_t bgn = gethrtime();
    EGLDisplay res = (*(EGLDisplay(*)())orig)(display_id);
    uint64_t end = gethrtime();
    
    putPtr(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglInitialize(EGLDisplay dpy, EGLint *major, EGLint *minor)
{
    EGLPROLOG(eglInitialize);
    
    putCmd(OPC_eglInitialize);
    putPtr(dpy);
    
    uint64_t bgn = gethrtime();
    EGLBoolean res = (*(EGLBoolean(*)())orig)(dpy, major, minor);
    uint64_t end = gethrtime();
    
    putIntPtr(major);
    putIntPtr(minor);
    putInt(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglTerminate(EGLDisplay dpy)
{
    EGLPROLOG(eglTerminate);
    
    putCmd(OPC_eglTerminate);
    putPtr(dpy);
    
    uint64_t bgn = gethrtime();
    EGLBoolean res = (*(EGLBoolean(*)())orig)(dpy);
    uint64_t end = gethrtime();

    putInt(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI const char * EGLAPIENTRY 
eglQueryString(EGLDisplay dpy, EGLint name)
{
    EGLPROLOG(eglQueryString);
    
    putCmd(OPC_eglQueryString);
    putPtr(dpy);
    putInt(name);
    
    uint64_t bgn = gethrtime();
    const char *res = (*(const char*(*)())orig)(dpy, name);
    uint64_t end = gethrtime();
    
    putString(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglGetConfigs(EGLDisplay dpy, EGLConfig *configs, EGLint config_size, EGLint *num_config)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglChooseConfig(EGLDisplay dpy, const EGLint *attrib_list,
                           EGLConfig *configs, EGLint config_size,
                           EGLint *num_config)
{
    EGLPROLOG(eglChooseConfig);
    
    putCmd(OPC_eglChooseConfig);
    putPtr(dpy);
    const EGLint *ptr = attrib_list;
    for (;ptr && *ptr != EGL_NONE; ptr += 2) {
        putInt(ptr[0]);
        putInt(ptr[1]);
    }
    putInt(EGL_NONE);
    putInt(configs == NULL ? 0 : config_size);
    
    uint64_t bgn = gethrtime();
    EGLBoolean res = (*(EGLBoolean(*)())orig)(dpy, attrib_list, configs, config_size, num_config);
    uint64_t end = gethrtime();
    
    putInt(*num_config);
    int i;
    for (i=0; i<*num_config && i < config_size; ++i) {
        putPtr(configs[i]);
    }
    putInt(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglGetConfigAttrib(EGLDisplay dpy, EGLConfig config, EGLint attribute, EGLint *value)
{
    EGLPROLOG(eglGetConfigAttrib);
    
    putCmd(OPC_eglGetConfigAttrib);
    putPtr(dpy);
    putPtr(config);
    putInt(attribute);
    
    uint64_t bgn = gethrtime();
    EGLBoolean res = (*(EGLBoolean(*)())orig)(dpy, config, attribute, value);
    uint64_t end = gethrtime();
    
    putInt(*value);
    putInt(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLSurface EGLAPIENTRY 
eglCreateWindowSurface(EGLDisplay dpy, EGLConfig config, EGLNativeWindowType win, const EGLint *attrib_list)
{
    EGLPROLOG(eglCreateWindowSurface);
    
    putCmd(OPC_eglCreateWindowSurface);
    putPtr(dpy);
    putPtr(config);
    putPtr(win);
    const EGLint *ptr = attrib_list;
    for (;ptr && *ptr != EGL_NONE; ptr += 2) {
        putInt(ptr[0]);
        putInt(ptr[1]);
    }
    putInt(EGL_NONE);
    
    uint64_t bgn = gethrtime();
    EGLSurface res = (*(EGLSurface(*)())orig)(dpy, config, win, attrib_list);
    uint64_t end = gethrtime();

    putPtr(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLSurface EGLAPIENTRY 
eglCreatePbufferSurface(EGLDisplay dpy, EGLConfig config, const EGLint *attrib_list)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLSurface EGLAPIENTRY 
eglCreatePixmapSurface(EGLDisplay dpy, EGLConfig config,
                                  EGLNativePixmapType pixmap,
                                  const EGLint *attrib_list)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglDestroySurface(EGLDisplay dpy, EGLSurface surface)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglQuerySurface(EGLDisplay dpy, EGLSurface surface, EGLint attribute, EGLint *value)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglBindAPI(EGLenum api)
{
    EGLPROLOG(eglBindAPI);
    
    putCmd(OPC_eglBindAPI);
    putInt(api);
    
    uint64_t bgn = gethrtime();
    EGLBoolean res = (*(EGLBoolean(*)())orig)(api);
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLenum EGLAPIENTRY 
eglQueryAPI(void)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglWaitClient(void)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglReleaseThread(void)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLSurface EGLAPIENTRY 
eglCreatePbufferFromClientBuffer(
              EGLDisplay dpy, EGLenum buftype, EGLClientBuffer buffer,
              EGLConfig config, const EGLint *attrib_list)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglSurfaceAttrib(EGLDisplay dpy, EGLSurface surface, EGLint attribute, EGLint value)
{
    EGLPROLOG(eglSurfaceAttrib);
    
    putCmd(OPC_eglSurfaceAttrib);
    putPtr(dpy);
    putPtr(surface);
    putInt(attribute);
    putInt(value);
    
    uint64_t bgn = gethrtime();
    EGLBoolean res = (*(EGLBoolean(*)())orig)(dpy, surface, attribute, value);
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglBindTexImage(EGLDisplay dpy, EGLSurface surface, EGLint buffer)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglReleaseTexImage(EGLDisplay dpy, EGLSurface surface, EGLint buffer)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglSwapInterval(EGLDisplay dpy, EGLint interval)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLContext EGLAPIENTRY 
eglCreateContext(EGLDisplay dpy, EGLConfig config,
                 EGLContext share_context, const EGLint *attrib_list)
{
    EGLPROLOG(eglCreateContext);
    
    putCmd(OPC_eglCreateContext);
    putPtr(dpy);
    putPtr(config);
    putPtr(share_context);
    const EGLint *ptr = attrib_list;
    for (;ptr && *ptr != EGL_NONE; ptr += 2) {
        putInt(ptr[0]);
        putInt(ptr[1]);
    }
    putInt(EGL_NONE);
    
    uint64_t bgn = gethrtime();
    EGLContext res = (*(EGLContext(*)())orig)(dpy, config, share_context, attrib_list);
    uint64_t end = gethrtime();
    
    putPtr(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglDestroyContext(EGLDisplay dpy, EGLContext ctx)
{
    EGLPROLOG(eglDestroyContext);
    
    putCmd(OPC_eglDestroyContext);
    putPtr(dpy);
    putPtr(ctx);
    
    uint64_t bgn = gethrtime();
    EGLBoolean res = (*(EGLBoolean(*)())orig)(dpy, ctx);
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglMakeCurrent(EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext ctx)
{
    EGLPROLOG(eglMakeCurrent);
    
    putCmd(OPC_eglMakeCurrent);
    putPtr(dpy);
    putPtr(draw);
    putPtr(read);
    putPtr(ctx);

    uint64_t bgn = gethrtime();
    EGLBoolean res = (*(EGLBoolean(*)())orig)(dpy, draw, read, ctx);
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLContext EGLAPIENTRY 
eglGetCurrentContext(void)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLSurface EGLAPIENTRY 
eglGetCurrentSurface(EGLint readdraw)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLDisplay EGLAPIENTRY 
eglGetCurrentDisplay(void)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglQueryContext(EGLDisplay dpy, EGLContext ctx, EGLint attribute, EGLint *value)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglWaitGL(void)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglWaitNative(EGLint engine)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglSwapBuffers(EGLDisplay dpy, EGLSurface surface)
{
    EGLPROLOG(eglSwapBuffers);
    
    putCmd(OPC_eglSwapBuffers);
    putPtr(dpy);
    putPtr(surface);
    
    uint64_t bgn = gethrtime();
    EGLBoolean res = (*(EGLBoolean(*)())orig)(dpy, surface);
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglCopyBuffers(EGLDisplay dpy, EGLSurface surface, EGLNativePixmapType target)
{
    NOT_IMPLEMENTED();
}    

