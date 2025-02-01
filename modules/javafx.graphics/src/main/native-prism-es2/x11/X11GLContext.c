/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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

#include <jni.h>
#include <stdlib.h>
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

#include "../PrismES2Defs.h"
#include "com_sun_prism_es2_X11GLContext.h"

/*
 * Class:     com_sun_prism_es2_X11GLContext
 * Method:    nInitialize
 * Signature: (JJZ)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_X11GLContext_nInitialize
(JNIEnv *env, jclass class, jlong nativeDInfo, jlong nativePFInfo,
        jboolean vSyncRequested) {
    const char *glVersion;
    const char *glVendor;
    const char *glRenderer;
    char *tmpVersionStr;
    int versionNumbers[2];
    const char *glExtensions;
    const char *glxExtensions;

    Window win = None;
    GLXFBConfig fbConfig = NULL;
    GLXContext ctx = NULL;
    XVisualInfo *visualInfo = NULL;
    int numFBConfigs, index, visualID;
    Display *display = NULL;
    ContextInfo *ctxInfo = NULL;
    DrawableInfo *dInfo = (DrawableInfo *) jlong_to_ptr(nativeDInfo);
    PixelFormatInfo *pfInfo = (PixelFormatInfo *) jlong_to_ptr(nativePFInfo);

    if ((dInfo == NULL) || (pfInfo == NULL)) {
        return 0;
    }
    display = pfInfo->display;
    fbConfig = pfInfo->fbConfig;
    win = dInfo->win;

    ctx = glXCreateNewContext(display, fbConfig, GLX_RGBA_TYPE, NULL, True);

    if (ctx == NULL) {
        fprintf(stderr, "Failed in glXCreateNewContext");
        return 0;
    }

    if (!glXMakeCurrent(display, win, ctx)) {
        glXDestroyContext(display, ctx);
        fprintf(stderr, "Failed in glXMakeCurrent");
        return 0;
    }

    /* Get the OpenGL version */
    glVersion = (char *) glGetString(GL_VERSION);
    if (glVersion == NULL) {
        glXDestroyContext(display, ctx);
        fprintf(stderr, "glVersion == null");
        return 0;
    }

    /* find out the version, major and minor version number */
    tmpVersionStr = strdup(glVersion);
    extractVersionInfo(tmpVersionStr, versionNumbers);
    free(tmpVersionStr);

/*
    fprintf(stderr, "GL_VERSION string = %s\n", glVersion);
    fprintf(stderr, "GL_VERSION (major.minor) = %d.%d\n",
            versionNumbers[0], versionNumbers[1]);
*/

    /*
     * Targeted Cards: Intel HD Graphics, Intel HD Graphics 2000/3000,
     * Radeon HD 2350, GeForce FX (with newer drivers), GeForce 7 series or higher
     *
     * Check for OpenGL 2.1 or later.
     */
    if ((versionNumbers[0] < 2) || ((versionNumbers[0] == 2) && (versionNumbers[1] < 1))) {
        glXDestroyContext(display, ctx);
        fprintf(stderr, "Prism-ES2 Error : GL_VERSION (major.minor) = %d.%d\n",
                versionNumbers[0], versionNumbers[1]);
        return 0;
    }

    /* Get the OpenGL vendor and renderer */
    glVendor = (const char *) glGetString(GL_VENDOR);
    if (glVendor == NULL) {
        glVendor = "<UNKNOWN>";
    }
    glRenderer = (const char *) glGetString(GL_RENDERER);
    if (glRenderer == NULL) {
        glRenderer = "<UNKNOWN>";
    }

    glExtensions = (const char *) glGetString(GL_EXTENSIONS);
    if (glExtensions == NULL) {
        glXDestroyContext(display, ctx);
        fprintf(stderr, "glExtensions == null");
        return 0;
    }

    // We use GL_ARB_pixel_buffer_object as an guide to
    // determine PS 3.0 capable.
    if (!isExtensionSupported(glExtensions, "GL_ARB_pixel_buffer_object")) {
        glXDestroyContext(display, ctx);
        fprintf(stderr, "GL profile isn't PS 3.0 capable");
        return 0;
    }

    glxExtensions = (const char *) glXGetClientString(display, GLX_EXTENSIONS);
    if (glxExtensions == NULL) {
        glXDestroyContext(display, ctx);
        fprintf(stderr, "glxExtensions == null");
        return 0;
    }

    /*
        fprintf(stderr, "glExtensions: %s\n", glExtensions);
        fprintf(stderr, "glxExtensions: %s\n", glxExtensions);
     */

    /* allocate the structure */
    ctxInfo = (ContextInfo *) malloc(sizeof (ContextInfo));
    if (ctxInfo == NULL) {
        fprintf(stderr, "nInitialize: Failed in malloc\n");
        return 0;
    }

    /* initialize the structure */
    initializeCtxInfo(ctxInfo);
    ctxInfo->versionStr = strdup(glVersion);
    ctxInfo->vendorStr = strdup(glVendor);
    ctxInfo->rendererStr = strdup(glRenderer);
    ctxInfo->glExtensionStr = strdup(glExtensions);
    ctxInfo->glxExtensionStr = strdup(glxExtensions);
    ctxInfo->versionNumbers[0] = versionNumbers[0];
    ctxInfo->versionNumbers[1] = versionNumbers[1];
    ctxInfo->display = display;
    ctxInfo->context = ctx;

    /* set function pointers */
    ctxInfo->glActiveTexture = (PFNGLACTIVETEXTUREPROC)
            dlsym(RTLD_DEFAULT, "glActiveTexture");
    ctxInfo->glAttachShader = (PFNGLATTACHSHADERPROC)
            dlsym(RTLD_DEFAULT, "glAttachShader");
    ctxInfo->glBindAttribLocation = (PFNGLBINDATTRIBLOCATIONPROC)
            dlsym(RTLD_DEFAULT, "glBindAttribLocation");
    ctxInfo->glBindFramebuffer = (PFNGLBINDFRAMEBUFFERPROC)
            dlsym(RTLD_DEFAULT, "glBindFramebuffer");
    ctxInfo->glBindRenderbuffer = (PFNGLBINDRENDERBUFFERPROC)
            dlsym(RTLD_DEFAULT, "glBindRenderbuffer");
    ctxInfo->glCheckFramebufferStatus = (PFNGLCHECKFRAMEBUFFERSTATUSPROC)
            dlsym(RTLD_DEFAULT, "glCheckFramebufferStatus");
    ctxInfo->glCreateProgram = (PFNGLCREATEPROGRAMPROC)
            dlsym(RTLD_DEFAULT, "glCreateProgram");
    ctxInfo->glCreateShader = (PFNGLCREATESHADERPROC)
            dlsym(RTLD_DEFAULT, "glCreateShader");
    ctxInfo->glCompileShader = (PFNGLCOMPILESHADERPROC)
            dlsym(RTLD_DEFAULT, "glCompileShader");
    ctxInfo->glDeleteBuffers = (PFNGLDELETEBUFFERSPROC)
            dlsym(RTLD_DEFAULT, "glDeleteBuffers");
    ctxInfo->glDeleteFramebuffers = (PFNGLDELETEFRAMEBUFFERSPROC)
            dlsym(RTLD_DEFAULT, "glDeleteFramebuffers");
    ctxInfo->glDeleteProgram = (PFNGLDELETEPROGRAMPROC)
            dlsym(RTLD_DEFAULT, "glDeleteProgram");
    ctxInfo->glDeleteRenderbuffers = (PFNGLDELETERENDERBUFFERSPROC)
            dlsym(RTLD_DEFAULT, "glDeleteRenderbuffers");
    ctxInfo->glDeleteShader = (PFNGLDELETESHADERPROC)
            dlsym(RTLD_DEFAULT, "glDeleteShader");
    ctxInfo->glDetachShader = (PFNGLDETACHSHADERPROC)
            dlsym(RTLD_DEFAULT, "glDetachShader");
    ctxInfo->glDisableVertexAttribArray = (PFNGLDISABLEVERTEXATTRIBARRAYPROC)
            dlsym(RTLD_DEFAULT, "glDisableVertexAttribArray");
    ctxInfo->glEnableVertexAttribArray = (PFNGLENABLEVERTEXATTRIBARRAYPROC)
            dlsym(RTLD_DEFAULT, "glEnableVertexAttribArray");
    ctxInfo->glFramebufferRenderbuffer = (PFNGLFRAMEBUFFERRENDERBUFFERPROC)
            dlsym(RTLD_DEFAULT, "glFramebufferRenderbuffer");
    ctxInfo->glFramebufferTexture2D = (PFNGLFRAMEBUFFERTEXTURE2DPROC)
            dlsym(RTLD_DEFAULT, "glFramebufferTexture2D");
    ctxInfo->glGenFramebuffers = (PFNGLGENFRAMEBUFFERSPROC)
            dlsym(RTLD_DEFAULT, "glGenFramebuffers");
    ctxInfo->glGenRenderbuffers = (PFNGLGENRENDERBUFFERSPROC)
            dlsym(RTLD_DEFAULT, "glGenRenderbuffers");
    ctxInfo->glGetProgramiv = (PFNGLGETPROGRAMIVPROC)
            dlsym(RTLD_DEFAULT, "glGetProgramiv");
    ctxInfo->glGetShaderiv = (PFNGLGETSHADERIVPROC)
            dlsym(RTLD_DEFAULT, "glGetShaderiv");
    ctxInfo->glGetUniformLocation = (PFNGLGETUNIFORMLOCATIONPROC)
            dlsym(RTLD_DEFAULT, "glGetUniformLocation");
    ctxInfo->glLinkProgram = (PFNGLLINKPROGRAMPROC)
            dlsym(RTLD_DEFAULT, "glLinkProgram");
    ctxInfo->glRenderbufferStorage = (PFNGLRENDERBUFFERSTORAGEPROC)
            dlsym(RTLD_DEFAULT, "glRenderbufferStorage");
    ctxInfo->glShaderSource = (PFNGLSHADERSOURCEPROC)
            dlsym(RTLD_DEFAULT, "glShaderSource");
    ctxInfo->glUniform1f = (PFNGLUNIFORM1FPROC)
            dlsym(RTLD_DEFAULT, "glUniform1f");
    ctxInfo->glUniform2f = (PFNGLUNIFORM2FPROC)
            dlsym(RTLD_DEFAULT, "glUniform2f");
    ctxInfo->glUniform3f = (PFNGLUNIFORM3FPROC)
            dlsym(RTLD_DEFAULT, "glUniform3f");
    ctxInfo->glUniform4f = (PFNGLUNIFORM4FPROC)
            dlsym(RTLD_DEFAULT, "glUniform4f");
    ctxInfo->glUniform4fv = (PFNGLUNIFORM4FVPROC)
            dlsym(RTLD_DEFAULT, "glUniform4fv");
    ctxInfo->glUniform1i = (PFNGLUNIFORM1IPROC)
            dlsym(RTLD_DEFAULT, "glUniform1i");
    ctxInfo->glUniform2i = (PFNGLUNIFORM2IPROC)
            dlsym(RTLD_DEFAULT, "glUniform2i");
    ctxInfo->glUniform3i = (PFNGLUNIFORM3IPROC)
            dlsym(RTLD_DEFAULT, "glUniform3i");
    ctxInfo->glUniform4i = (PFNGLUNIFORM4IPROC)
            dlsym(RTLD_DEFAULT, "glUniform4i");
    ctxInfo->glUniform4iv = (PFNGLUNIFORM4IVPROC)
            dlsym(RTLD_DEFAULT, "glUniform4iv");
    ctxInfo->glUniformMatrix4fv = (PFNGLUNIFORMMATRIX4FVPROC)
            dlsym(RTLD_DEFAULT, "glUniformMatrix4fv");
    ctxInfo->glUseProgram = (PFNGLUSEPROGRAMPROC)
            dlsym(RTLD_DEFAULT, "glUseProgram");
    ctxInfo->glValidateProgram = (PFNGLVALIDATEPROGRAMPROC)
            dlsym(RTLD_DEFAULT, "glValidateProgram");
    ctxInfo->glVertexAttribPointer = (PFNGLVERTEXATTRIBPOINTERPROC)
            dlsym(RTLD_DEFAULT, "glVertexAttribPointer");
    ctxInfo->glGenBuffers = (PFNGLGENBUFFERSPROC)
            dlsym(RTLD_DEFAULT, "glGenBuffers");
    ctxInfo->glBindBuffer = (PFNGLBINDBUFFERPROC)
            dlsym(RTLD_DEFAULT, "glBindBuffer");
    ctxInfo->glBufferData = (PFNGLBUFFERDATAPROC)
            dlsym(RTLD_DEFAULT, "glBufferData");
    ctxInfo->glBufferSubData = (PFNGLBUFFERSUBDATAPROC)
            dlsym(RTLD_DEFAULT, "glBufferSubData");
    ctxInfo->glGetShaderInfoLog = (PFNGLGETSHADERINFOLOGPROC)
            dlsym(RTLD_DEFAULT, "glGetShaderInfoLog");
    ctxInfo->glGetProgramInfoLog = (PFNGLGETPROGRAMINFOLOGPROC)
            dlsym(RTLD_DEFAULT, "glGetProgramInfoLog");
    ctxInfo->glTexImage2DMultisample = (PFNGLTEXIMAGE2DMULTISAMPLEPROC)
            dlsym(RTLD_DEFAULT,"glTexImage2DMultisample");
    ctxInfo->glRenderbufferStorageMultisample = (PFNGLRENDERBUFFERSTORAGEMULTISAMPLEPROC)
            dlsym(RTLD_DEFAULT,"glRenderbufferStorageMultisample");
    ctxInfo->glBlitFramebuffer = (PFNGLBLITFRAMEBUFFERPROC)
            dlsym(RTLD_DEFAULT,"glBlitFramebuffer");

    if (isExtensionSupported(ctxInfo->glxExtensionStr,
            "GLX_SGI_swap_control")) {
        ctxInfo->glXSwapIntervalSGI = (PFNGLXSWAPINTERVALSGIPROC)
                dlsym(RTLD_DEFAULT, "glXSwapIntervalSGI");

        if (ctxInfo->glXSwapIntervalSGI == NULL) {
            ctxInfo->glXSwapIntervalSGI = (PFNGLXSWAPINTERVALSGIPROC)
                glXGetProcAddress((const GLubyte *)"glXSwapIntervalSGI");
        }

    }

    // initialize platform states and properties to match
    // cached states and properties
    if (ctxInfo->glXSwapIntervalSGI != NULL) {
        ctxInfo->glXSwapIntervalSGI(0);
    }
    ctxInfo->state.vSyncEnabled = JNI_FALSE;
    ctxInfo->vSyncRequested = vSyncRequested;

    initState(ctxInfo);

    // Release context once we are all done
    glXMakeCurrent(display, None, NULL);

    return ptr_to_jlong(ctxInfo);
}

/*
 * Class:     com_sun_prism_es2_X11GLContext
 * Method:    nGetNativeHandle
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_X11GLContext_nGetNativeHandle
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return 0;
    }
    return ptr_to_jlong(ctxInfo->context);
}

/*
 * Class:     com_sun_prism_es2_X11GLContext
 * Method:    nMakeCurrent
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_X11GLContext_nMakeCurrent
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativeDInfo) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    DrawableInfo *dInfo = (DrawableInfo *) jlong_to_ptr(nativeDInfo);
    int interval;
    jboolean vSyncNeeded;

    if (!glXMakeCurrent(ctxInfo->display, dInfo->win, ctxInfo->context)) {
        fprintf(stderr, "Failed in glXMakeCurrent");
    }

    vSyncNeeded = ctxInfo->vSyncRequested && dInfo->onScreen;
    if (vSyncNeeded == ctxInfo->state.vSyncEnabled) {
        return;
    }
    interval = (vSyncNeeded) ? 1 : 0;
    ctxInfo->state.vSyncEnabled = vSyncNeeded;
    if (ctxInfo->glXSwapIntervalSGI != NULL) {
        ctxInfo->glXSwapIntervalSGI(interval);
    }
}
