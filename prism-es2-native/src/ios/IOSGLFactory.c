/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "PrismES2Defs.h"
#include "com_sun_prism_es2_IOSGLFactory.h"

void printAndReleaseResources(jlong pf, jlong ctx, const char *message)
{
    fprintf(stderr, "%s\n", message);

    makeCurrentContext(NULL);
    if (pf != 0) {
        deletePixelFormat((void *) (intptr_t) pf);
    }
    if (ctx != 0) {
        deleteContext((void *) (intptr_t) ctx);
    }
}

/*
 * Class:     com_sun_prism_es2_IOSGLFactory
 * Method:    nInitialize
 * Signature: ([I)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_IOSGLFactory_nInitialize
(JNIEnv *env, jclass class, jintArray attrArr)
{

    jint *attrs;
    jlong pixelFormat;
    jlong context = 0;
    int viewNotReady;
    jboolean result = JNI_FALSE;

    ContextInfo *ctxInfo = NULL;

    const char *glVersion;
    const char *glVendor;
    const char *glRenderer;
    char *tmpVersionStr;
    int  versionNumbers[2];
    const char *glExtensions;

    if (attrArr == NULL) {
        return 0;
    }

    attrs = (*env)->GetIntArrayElements(env, attrArr, NULL);
    pixelFormat = (jlong) (intptr_t) createPixelFormat(attrs);
    (*env)->ReleaseIntArrayElements(env, attrArr, attrs, JNI_ABORT);

    context = (jlong) (intptr_t) createContext(NULL, NULL,
            (void *) (intptr_t) pixelFormat, &viewNotReady);

    if (context == 0) {
        printAndReleaseResources(pixelFormat, 0,
                "Fail in createContext");
        return 0;
    }

    result = makeCurrentContext((void *) (intptr_t) context);
    if (!result) {
        printAndReleaseResources(pixelFormat, context,
                "Fail in CGLSetCurrentContext");
        return 0;
    }

     /* Get the OpenGL version */
    glVersion = (char *)glGetString(GL_VERSION);
    if (glVersion == NULL) {
        printAndReleaseResources(pixelFormat, context,
                "glVersion == null");
        return 0;
    }

    /* find out the version, major and minor version number */
    tmpVersionStr = strdup(glVersion);
    extractVersionInfo(tmpVersionStr, versionNumbers);
    free(tmpVersionStr);

    fprintf(stderr, "GL_VERSION string = %s\n", glVersion);
    fprintf(stderr, "GL_VERSION (major.minor) = %d.%d\n",
            versionNumbers[0], versionNumbers[1]);

    /*
     * Check for OpenGL 2.0 or later.
     */
#ifndef __APPLE__
    if (versionNumbers[0] < 2) {
       fprintf(stderr,
               "Prism-ES2 Error : GL_VERSION (major.minor) = %d.%d\n",
               versionNumbers[0], versionNumbers[1]);
        printAndReleaseResources(pixelFormat, context, NULL);
        return 0;
    }
#endif

    /* Get the OpenGL vendor and renderer */
    glVendor = (char *)glGetString(GL_VENDOR);
    if (glVendor == NULL) {
        glVendor = "<UNKNOWN>";
    }
    glRenderer = (char *)glGetString(GL_RENDERER);
    if (glRenderer == NULL) {
        glRenderer = "<UNKNOWN>";
    }

    glExtensions = (char *)glGetString(GL_EXTENSIONS);
    if (glExtensions == NULL) {
        printAndReleaseResources(pixelFormat, context,
                "Prism-ES2 Error : glExtensions == null");
        return 0;
    }
    if (isExtensionSupported(glExtensions, "GL_ARB_pixel_buffer_object")) {
        fprintf(stderr, "GL_ARB_pixel_buffer_object detected.\n");
    }

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
    ctxInfo->versionNumbers[0] = versionNumbers[0];
    ctxInfo->versionNumbers[1] = versionNumbers[1];
    ctxInfo->gl2 = JNI_FALSE;

    // Save the context.
    ctxInfo->context = context;

    /*
     *  Do not free context as we need it for iOS to use as a shareContext for
     * GLass
     */

    return ptr_to_jlong(ctxInfo);

}

/*
 * Class:     com_sun_prism_es2_IOSGLFactory
 * Method:    nGetAdapterOrdinal
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_IOSGLFactory_nGetAdapterOrdinal
(JNIEnv *env, jclass class, jlong screen)
{
    // Needs implementation to handle multi-monitors (RT-27437)
    return 0;
}

/*
 * Class:     com_sun_prism_es2_IOSGLFactory
 * Method:    nGetAdapterCount
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_IOSGLFactory_nGetAdapterCount
(JNIEnv *env, jclass class)
{
    // Needs implementation to handle multi-monitors (RT-27437)
    return 1;

}

/*
 * Class:     com_sun_prism_es2_IOSGLFactory
 * Method:    nGetIsGL2
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_es2_IOSGLFactory_nGetIsGL2
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    return ((ContextInfo *)jlong_to_ptr(nativeCtxInfo))->gl2;
}
