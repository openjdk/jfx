/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

extern char *strJavaToC(JNIEnv *env, jstring str);

void printGLError(GLenum errCode) {
    char const glCString[] = "*** GLError Code = ";
    switch (errCode) {
        case GL_NO_ERROR:
            //fprintf(stderr, "%sGL_NO_ERROR\n", glCString);
            break;
        case GL_INVALID_ENUM:
            fprintf(stderr, "%sGL_INVALID_ENUM\n", glCString);
            break;
        case GL_INVALID_VALUE:
            fprintf(stderr, "%sGL_INVALID_VALUE\n", glCString);
            break;
        case GL_INVALID_OPERATION:
            fprintf(stderr, "%sGL_INVALID_OPERATION\n", glCString);
            break;
        case GL_STACK_OVERFLOW:
            fprintf(stderr, "%sGL_STACK_OVERFLOW\n", glCString);
            break;
        case GL_STACK_UNDERFLOW:
            fprintf(stderr, "%sGL_STACK_UNDERFLOW\n", glCString);
            break;
        case GL_OUT_OF_MEMORY:
            fprintf(stderr, "%sGL_OUT_OF_MEMORY\n", glCString);
            break;
        default:
            fprintf(stderr, "%s*** UNKNOWN ERROR CODE ***\n", glCString);
    }
}

void initializeCtxInfo(ContextInfo *ctxInfo) {
    if (ctxInfo == NULL) {
        return;
    }
    // Initialize structure to all zeros
    memset(ctxInfo, 0, sizeof (ContextInfo));
}

void deleteCtxInfo(ContextInfo *ctxInfo) {
    if (ctxInfo == NULL) {
        return;
    }

    if (ctxInfo->versionStr != NULL) {
        free(ctxInfo->versionStr);
    }
    if (ctxInfo->vendorStr != NULL) {
        free(ctxInfo->vendorStr);
    }
    if (ctxInfo->rendererStr != NULL) {
        free(ctxInfo->rendererStr);
    }
    if (ctxInfo->glExtensionStr != NULL) {
        free(ctxInfo->glExtensionStr);
    }

#ifdef WIN32 /* WIN32 */
    if (ctxInfo->wglExtensionStr != NULL) {
        free(ctxInfo->wglExtensionStr);
    }
    if (ctxInfo->hglrc != NULL) {
        wglDeleteContext(ctxInfo->hglrc);
        ctxInfo->hglrc = NULL;
    }
#endif

#ifdef UNIX
    if (ctxInfo->glxExtensionStr != NULL) {
        free(ctxInfo->glxExtensionStr);
    }
    if (ctxInfo->context != NULL) {
#if defined(IS_GLX)
        glXDestroyContext(ctxInfo->display, ctxInfo->context);
#endif
#ifdef IS_EGL
        eglDestroyContext(ctxInfo->display, ctxInfo->context);
#endif
    }
#endif
    // Initialize structure to all zeros
    memset(ctxInfo, 0, sizeof (ContextInfo));
}

void initState(ContextInfo *ctxInfo) {
    if (ctxInfo == NULL) {
        return;
    }

    glEnable(GL_BLEND);
    glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

    // initialize states and properties to
    // match cached states and properties

    // depthtest is set to false
    // Note: This state is cached in GLContext.java
    ctxInfo->state.depthWritesEnabled = JNI_FALSE;
    glDepthMask(ctxInfo->state.depthWritesEnabled);
    glDisable(GL_DEPTH_TEST);

    if (ctxInfo->state.scissorEnabled) {
        ctxInfo->state.scissorEnabled = JNI_FALSE;
        glDisable(GL_SCISSOR_TEST);
    }

    ctxInfo->state.clearColor[0] = 0.0;
    ctxInfo->state.clearColor[1] = 0.0;
    ctxInfo->state.clearColor[2] = 0.0;
    ctxInfo->state.clearColor[3] = 0.0;
    glClearColor(ctxInfo->state.clearColor[0], ctxInfo->state.clearColor[1],
            ctxInfo->state.clearColor[2], ctxInfo->state.clearColor[3]);

    ctxInfo->vbFloatData = NULL;
    ctxInfo->vbByteData = NULL;
    ctxInfo->state.fillMode = GL_FILL;
    ctxInfo->state.cullEnable = JNI_FALSE;
    ctxInfo->state.cullMode = GL_BACK;
    ctxInfo->state.fbo = 0;
}

void clearBuffers(ContextInfo *ctxInfo,
        GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha,
        jboolean clearColor, jboolean clearDepth, jboolean ignoreScissor) {
    GLbitfield clearBIT = 0;

    if (ctxInfo == NULL) {
        return;
    }

    if (ignoreScissor && ctxInfo->state.scissorEnabled) {
        // glClear() honors the current scissor, so disable it
        // temporarily if ignoreScissor is true
        glDisable(GL_SCISSOR_TEST);
    }

    if (clearColor) {
        clearBIT = GL_COLOR_BUFFER_BIT;
        if ((ctxInfo->state.clearColor[0] != red)
                || (ctxInfo->state.clearColor[1] != green)
                || (ctxInfo->state.clearColor[2] != blue)
                || (ctxInfo->state.clearColor[3] != alpha)) {
            glClearColor(red, green, blue, alpha);
            ctxInfo->state.clearColor[0] = red;
            ctxInfo->state.clearColor[1] = green;
            ctxInfo->state.clearColor[2] = blue;
            ctxInfo->state.clearColor[3] = alpha;
        }
    }

    if (clearDepth) {
        clearBIT |= GL_DEPTH_BUFFER_BIT;
        // also make sure depth writes are enabled for the clear operation
        if (!ctxInfo->state.depthWritesEnabled) {
            glDepthMask(GL_TRUE);
        }
        glClear(clearBIT);
        if (!ctxInfo->state.depthWritesEnabled) {
            glDepthMask(GL_FALSE);
        }
    } else {
        glClear(clearBIT);
    }

    // restore previous state
    if (ignoreScissor && ctxInfo->state.scissorEnabled) {
        glEnable(GL_SCISSOR_TEST);
    }
}

void bindFBO(ContextInfo *ctxInfo, GLuint fboId) {
    if ((ctxInfo == NULL) || (ctxInfo->glBindFramebuffer == NULL)) {
        return;
    }
    ctxInfo->glBindFramebuffer(GL_FRAMEBUFFER, fboId);
    ctxInfo->state.fbo = fboId;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nActiveTexture
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nActiveTexture
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint texUnit) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glActiveTexture == NULL)) {
        return;
    }
    ctxInfo->glActiveTexture(GL_TEXTURE0 + texUnit);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nBindFBO
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nBindFBO
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint fboId) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    bindFBO(ctxInfo, (GLuint)fboId);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nBindTexture
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nBindTexture
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint texID) {
    glBindTexture(GL_TEXTURE_2D, texID);
}

GLenum translateScaleFactor(jint scaleFactor) {
    switch (scaleFactor) {
        case com_sun_prism_es2_GLContext_GL_ZERO:
            return GL_ZERO;
        case com_sun_prism_es2_GLContext_GL_ONE:
            return GL_ONE;
        case com_sun_prism_es2_GLContext_GL_SRC_COLOR:
            return GL_SRC_COLOR;
        case com_sun_prism_es2_GLContext_GL_ONE_MINUS_SRC_COLOR:
            return GL_ONE_MINUS_SRC_COLOR;
        case com_sun_prism_es2_GLContext_GL_DST_COLOR:
            return GL_DST_COLOR;
        case com_sun_prism_es2_GLContext_GL_ONE_MINUS_DST_COLOR:
            return GL_ONE_MINUS_DST_COLOR;
        case com_sun_prism_es2_GLContext_GL_SRC_ALPHA:
            return GL_SRC_ALPHA;
        case com_sun_prism_es2_GLContext_GL_ONE_MINUS_SRC_ALPHA:
            return GL_ONE_MINUS_SRC_ALPHA;
        case com_sun_prism_es2_GLContext_GL_DST_ALPHA:
            return GL_DST_ALPHA;
        case com_sun_prism_es2_GLContext_GL_ONE_MINUS_DST_ALPHA:
            return GL_ONE_MINUS_DST_ALPHA;
        case com_sun_prism_es2_GLContext_GL_CONSTANT_COLOR:
            return GL_CONSTANT_COLOR;
        case com_sun_prism_es2_GLContext_GL_ONE_MINUS_CONSTANT_COLOR:
            return GL_ONE_MINUS_CONSTANT_COLOR;
        case com_sun_prism_es2_GLContext_GL_CONSTANT_ALPHA:
            return GL_CONSTANT_ALPHA;
        case com_sun_prism_es2_GLContext_GL_ONE_MINUS_CONSTANT_ALPHA:
            return GL_ONE_MINUS_CONSTANT_ALPHA;
        case com_sun_prism_es2_GLContext_GL_SRC_ALPHA_SATURATE:
            return GL_SRC_ALPHA_SATURATE;
        default:
            fprintf(stderr, "Error: Unknown scale factor. Returning GL_ZERO (default)\n");
    }
    return GL_ZERO;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nBlendFunc
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nBlendFunc
(JNIEnv *env, jclass class, jint sFactor, jint dFactor) {
    glBlendFunc(translateScaleFactor(sFactor), translateScaleFactor(dFactor));
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nClearBuffers
 * Signature: (JFFFFZZZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nClearBuffers
(JNIEnv *env, jclass class, jlong nativeCtxInfo,
        jfloat red, jfloat green, jfloat blue, jfloat alpha,
        jboolean clearColor, jboolean clearDepth, jboolean ignoreScissor) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return;
    }

    clearBuffers(ctxInfo,
            (GLclampf) red, (GLclampf) green, (GLclampf) blue, (GLclampf) alpha,
            clearColor, clearDepth, ignoreScissor);
}

int checkFramebufferStatus(ContextInfo *ctxInfo) {
    GLenum status;
    status = ctxInfo->glCheckFramebufferStatus(GL_FRAMEBUFFER);
    if (status != GL_FRAMEBUFFER_COMPLETE) {
        switch(status) {
            case GL_FRAMEBUFFER_COMPLETE:
                return GL_FALSE;
                break;
            case GL_FRAMEBUFFER_UNSUPPORTED:
            //Choose different formats
                fprintf(stderr, "Framebuffer object format is unsupported by the video hardware. (GL_FRAMEBUFFER_UNSUPPORTED)(FBO - 820)\n");
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                fprintf(stderr, "Incomplete attachment. (GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT)(FBO - 820)\n");
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                fprintf(stderr, "Incomplete missing attachment. (GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT)(FBO - 820)\n");
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT:
                fprintf(stderr, "Incomplete dimensions. (GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT)(FBO - 820)\n");
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT:
                fprintf(stderr, "Incomplete formats. (GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT)(FBO - 820)\n");
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                fprintf(stderr, "Incomplete draw buffer. (GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER)(FBO - 820)\n");
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                fprintf(stderr, "Incomplete read buffer. (GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER)(FBO - 820)\n");
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE:
                fprintf(stderr, "Incomplete multisample buffer. (GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE)(FBO - 820)\n");
                break;
            default:
                //Programming error; will fail on all hardware
                fprintf(stderr, "Some video driver error or programming error occurred. Framebuffer object status is invalid. (FBO - 823)\n");
                break;
        }
        return GL_TRUE;
    }
    return GL_FALSE;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nBlit
 * Signature: (JIIIIIIIIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nBlit
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint srcFBO, jint dstFBO,
            jint jsrcX0, jint jsrcY0, jint srcX1, jint srcY1,
            jint jdstX0, jint jdstY0, jint dstX1, jint dstY1) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glGenFramebuffers == NULL)
            || (ctxInfo->glBindFramebuffer == NULL)
            || (ctxInfo->glBlitFramebuffer == NULL)) {
        return;
    }

    // Temporarily disable scissor to avoid a problem with some GL drivers
    // that honor the scissor test if enabled
    if (ctxInfo->state.scissorEnabled) {
        glDisable(GL_SCISSOR_TEST);
    }

    if (dstFBO == 0) {
        dstFBO = ctxInfo->state.fbo;
    }
    //Bind the FBOs
    ctxInfo->glBindFramebuffer(GL_READ_FRAMEBUFFER, (GLuint)srcFBO);
    ctxInfo->glBindFramebuffer(GL_DRAW_FRAMEBUFFER, (GLuint)dstFBO);
    ctxInfo->glBlitFramebuffer(jsrcX0, jsrcY0, srcX1, srcY1,
                               jdstX0, jdstY0, dstX1, dstY1,
                               GL_COLOR_BUFFER_BIT, GL_LINEAR);
    /* TODO: iOS MSAA support:
     * We are using glBlitFramebuffer to "resolve" the mutlisample buffer,
     * to a color destination. iOS does things differently, it uses
     * glResolveMultisampleFramebufferAPPLE() in place of glBlit...
     * Problem is glResolve.. does not take arguments so we can't flip
     * coordinate system.
     */

    // Restore previous FBO
    ctxInfo->glBindFramebuffer(GL_FRAMEBUFFER, ctxInfo->state.fbo);

    // Restore previous scissor
    if (ctxInfo->state.scissorEnabled) {
        glEnable(GL_SCISSOR_TEST);
    }
}

GLuint attachRenderbuffer(ContextInfo *ctxInfo, GLuint rbID, GLenum attachment) {
    //GLenum status;
    ctxInfo->glFramebufferRenderbuffer(GL_FRAMEBUFFER, attachment,
            GL_RENDERBUFFER, rbID);
    ctxInfo->glBindRenderbuffer(GL_RENDERBUFFER, 0);
    //status = ctxInfo->glCheckFramebufferStatus(GL_FRAMEBUFFER);
    if (checkFramebufferStatus(ctxInfo)) {
        ctxInfo->glDeleteRenderbuffers(1, &rbID);
        rbID = 0;
        fprintf(stderr, "Error creating render buffer object %d\n", rbID);
    } else {
        // explicitly clear the render buffers, since it may contain
        // garbage after initialization
        clearBuffers(ctxInfo, 0, 0, 0, 0, JNI_FALSE, JNI_TRUE, JNI_TRUE);
    }
    return rbID;
}

GLuint createAndAttachRenderBuffer(ContextInfo *ctxInfo, GLsizei width, GLsizei height, GLsizei msaa, GLenum attachment) {
    GLuint rbID;
    GLenum internalFormat;

    if ((ctxInfo == NULL) || (ctxInfo->glGenRenderbuffers == NULL)
            || (ctxInfo->glBindRenderbuffer == NULL)
            || (ctxInfo->glRenderbufferStorage == NULL)
            || (ctxInfo->glFramebufferRenderbuffer == NULL)
#ifndef IS_EGL
            || (ctxInfo->glRenderbufferStorageMultisample == NULL)
#endif
            || (ctxInfo->glCheckFramebufferStatus == NULL)
            || (ctxInfo->glDeleteRenderbuffers == NULL)) {
        return 0;
    }

    if (attachment == GL_DEPTH_ATTACHMENT) {
#ifdef IS_EGL
        internalFormat = GL_DEPTH_COMPONENT16;
#else
        internalFormat = GL_DEPTH_COMPONENT;
#endif
    } else {
        internalFormat = GL_RGBA8; //TODO verify format on RGBA or RGBA8
    }
    // create a depth buffer
    ctxInfo->glGenRenderbuffers(1, &rbID);
    ctxInfo->glBindRenderbuffer(GL_RENDERBUFFER, rbID);
#ifdef IS_EGL
    ctxInfo->glRenderbufferStorage(GL_RENDERBUFFER, internalFormat, width, height);
#else
    if (msaa) {
        ctxInfo->glRenderbufferStorageMultisample(GL_RENDERBUFFER, msaa, internalFormat, width, height);
    } else {
        ctxInfo->glRenderbufferStorage(GL_RENDERBUFFER, internalFormat, width, height);
    }
#endif
    return attachRenderbuffer(ctxInfo, rbID, attachment);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nCreateDepthBuffer
 * Signature: (JIII)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_GLContext_nCreateDepthBuffer
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint width, jint height, jint msaa) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    return createAndAttachRenderBuffer(ctxInfo, width, height, msaa, GL_DEPTH_ATTACHMENT);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nCreateRenderBuffer
 * Signature: (JIII)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_GLContext_nCreateRenderBuffer
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint width, jint height, jint msaa) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    return createAndAttachRenderBuffer(ctxInfo, width, height, msaa, GL_COLOR_ATTACHMENT0);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nCreateFBO
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_GLContext_nCreateFBO
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint texID) {
    GLuint fboID;

    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glGenFramebuffers == NULL)
            || (ctxInfo->glFramebufferTexture2D == NULL)
            || (ctxInfo->glCheckFramebufferStatus == NULL)
            || (ctxInfo->glDeleteFramebuffers == NULL)) {
        return 0;
    }

    // initialize framebuffer object
    ctxInfo->glGenFramebuffers(1, &fboID);
    bindFBO(ctxInfo, fboID);

    if (texID) {
        // Attach color texture to framebuffer object
        ctxInfo->glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                                        GL_TEXTURE_2D, (GLuint)texID, 0);
        // Can't check status of FBO until after a buffer is attached to it
        if (checkFramebufferStatus(ctxInfo)) {
            ctxInfo->glDeleteFramebuffers(1, &fboID);
            fprintf(stderr,
                    "Error creating framebuffer object with TexID %d)\n", (int) texID);
            return 0;
        }
        // explicitly clear the color buffer, since it may contain garbage
        // after initialization
        clearBuffers(ctxInfo, 0, 0, 0, 0, JNI_TRUE, JNI_FALSE, JNI_TRUE);
    }

    return (jint)fboID;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nCreateProgram
 * Signature: (JI[II[Ljava/lang/String;[I)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_GLContext_nCreateProgram
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint vertID, jintArray fragIDArr,
        jint numAttrs, jobjectArray attrs, jintArray indexs) {
    GLuint shaderProgram;
    GLint success;
    int i;
    jstring attrName;
    jint *indexsPtr;
    char *attrNameString;
    jint *fragIDs;
    jsize length;
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (attrs == NULL) || (indexs == NULL)
            || (ctxInfo->glCreateProgram == NULL)
            || (ctxInfo->glAttachShader == NULL)
            || (ctxInfo->glBindAttribLocation == NULL)
            || (ctxInfo->glLinkProgram == NULL)
            || (ctxInfo->glGetProgramiv == NULL)
            || (ctxInfo->glGetProgramInfoLog == NULL)
            || (ctxInfo->glDetachShader == NULL)
            || (ctxInfo->glDeleteShader == NULL)
            || (ctxInfo->glDeleteProgram == NULL)) {
        return 0;
    }

    if (fragIDArr != NULL) {
        length = (*env)->GetArrayLength(env, fragIDArr);
        fragIDs = (*env)->GetIntArrayElements(env, fragIDArr, NULL);
    } else {
        return 0;
    }
    // create the program object and attach it to the shader
    shaderProgram = ctxInfo->glCreateProgram();
    ctxInfo->glAttachShader(shaderProgram, vertID);
    for (i = 0; i < length; i++) {
        ctxInfo->glAttachShader(shaderProgram, fragIDs[i]);
    }

    // bind any user-defined index values to their corresponding names
    indexsPtr = (*env)->GetIntArrayElements(env, indexs, NULL);
    for (i = 0; i < numAttrs; i++) {
        attrName = (*env)->GetObjectArrayElement(env, attrs, i);
        attrNameString = strJavaToC(env, attrName);
        ctxInfo->glBindAttribLocation(shaderProgram, indexsPtr[i], attrNameString);
        free(attrNameString);
    }

    // link the program
    ctxInfo->glLinkProgram(shaderProgram);
    ctxInfo->glGetProgramiv(shaderProgram, GL_LINK_STATUS, &success);

    if (success == GL_FALSE) {
        GLint  length;

        ctxInfo->glGetProgramiv(shaderProgram, GL_INFO_LOG_LENGTH, &length);
        if (length) {
            char* msg  =  (char *) malloc(length * sizeof(char));
            ctxInfo->glGetProgramInfoLog(shaderProgram, length, NULL, msg);
            fprintf(stderr, "Program link log: %.*s\n", length, msg);
            free(msg);
        } else {
            fprintf(stderr, "glLinkProgram: GL_LINK_STATUS returns GL_FALSE but GL_INFO_LOG_LENGTH returns 0\n");
        }

        ctxInfo->glDetachShader(shaderProgram, vertID);
        ctxInfo->glDeleteShader(vertID);
        for(i = 0; i < length; i++) {
            ctxInfo->glDetachShader(shaderProgram, fragIDs[i]);
            ctxInfo->glDeleteShader(fragIDs[i]);
        }
        ctxInfo->glDeleteProgram(shaderProgram);
        return 0;
    }

    (*env)->ReleaseIntArrayElements(env, fragIDArr, fragIDs, JNI_ABORT);

    return shaderProgram;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nCompileShader
 * Signature: (JLjava/lang/String;Z)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_GLContext_nCompileShader
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jstring src, jboolean vertex) {
    GLenum shaderType;
    GLuint shaderID;
    GLint success;

    /* Null-terminated "C" strings */
    GLchar *shaderString = NULL;
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (src == NULL)
            || (ctxInfo->glCreateShader == NULL)
            || (ctxInfo->glShaderSource == NULL)
            || (ctxInfo->glCompileShader == NULL)
            || (ctxInfo->glGetShaderiv == NULL)
            || (ctxInfo->glGetShaderInfoLog == NULL)
            || (ctxInfo->glDeleteShader == NULL)) {
        return 0;
    }

    // create the shader object and compile the shader source code
    shaderType = vertex ? GL_VERTEX_SHADER : GL_FRAGMENT_SHADER;
    shaderID = ctxInfo->glCreateShader(shaderType);
    shaderString = (GLchar *) strJavaToC(env, src);
    if (shaderString == NULL) {
        /* Just return, since strJavaToC will throw OOM if it returns NULL */
        return 0;
    }
    ctxInfo->glShaderSource(shaderID, 1, (const GLchar **) &shaderString, (GLint *) NULL);
    ctxInfo->glCompileShader(shaderID);
    ctxInfo->glGetShaderiv(shaderID, GL_COMPILE_STATUS, &success);

    free(shaderString);

    if (success == GL_FALSE) {
        GLint  length;

        ctxInfo->glGetShaderiv(shaderID, GL_INFO_LOG_LENGTH, &length);
        if (length) {
            char* msg = (char *) malloc(length * sizeof(char));
            ctxInfo->glGetShaderInfoLog(shaderID, length, NULL, msg);
            fprintf(stderr, "Shader compile log: %.*s\n", length, msg);
            free(msg);
        } else {
            fprintf(stderr, "glCompileShader: GL_COMPILE_STATUS returns GL_FALSE but GL_INFO_LOG_LENGTH returns 0\n");
        }

        ctxInfo->glDeleteShader(shaderID);
        return 0;
    }

    return shaderID;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nCreateTexture
 * Signature: (JII)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_GLContext_nCreateTexture
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint width, jint height) {
    GLuint texID = 0;
    GLenum err;

    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glActiveTexture == NULL)) {
        return 0;
    }

    glGenTextures(1, &texID);
    if (texID == 0) {
        // fprintf(stderr, "nCreateTexture: Failed to generate texture.\n");
        return (jint) texID;
    }

    glBindTexture(GL_TEXTURE_2D, texID);

    // Reset Error
    glGetError();
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height,
            0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);

    err  = glGetError();
    // printGLError(err);

    if (err != GL_NO_ERROR) {
        glDeleteTextures(1, &texID);
        texID = 0;
    } else {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    }
    return (jint) texID;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nDisposeShaders
 * Signature: (JII[I)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nDisposeShaders
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint shaderProgram,
        jint vertID, jintArray fragIDArr) {
    jsize length;
    jint* fragIDs;
    int i;
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glDetachShader == NULL)
            || (ctxInfo->glDeleteShader == NULL)
            || (ctxInfo->glDeleteProgram == NULL)) {
        return;
    }

    if (vertID != 0) {
        ctxInfo->glDetachShader(shaderProgram, vertID);
        ctxInfo->glDeleteShader(vertID);
    }

    if (fragIDArr == NULL) {
        return;
    }

    length = (*env)->GetArrayLength(env, fragIDArr);
    fragIDs = (*env)->GetIntArrayElements(env, fragIDArr, NULL);

    for (i = 0; i < length; i++) {
        if (fragIDs[i] != 0) {
            ctxInfo->glDetachShader(shaderProgram, fragIDs[i]);
            ctxInfo->glDeleteShader(fragIDs[i]);
        }
    }

    (*env)->ReleaseIntArrayElements(env, fragIDArr, fragIDs, JNI_ABORT);

    ctxInfo->glDeleteProgram(shaderProgram);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nDeleteFBO
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nDeleteFBO
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint fboID) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glDeleteFramebuffers == NULL)) {
        return;
    }
    if (fboID != 0) {
        ctxInfo->glDeleteFramebuffers(1, (GLuint *) &fboID);
    }
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nDeleteRenderBuffer
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nDeleteRenderBuffer
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint rbID) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glDeleteRenderbuffers == NULL)) {
        return;
    }
    if (rbID != 0) {
        ctxInfo->glDeleteRenderbuffers(1, (GLuint *) &rbID);
    }
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nDeleteShader
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nDeleteShader
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint shaderID) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glDeleteShader == NULL)) {
        return;
    }
    if (shaderID != 0) {
        ctxInfo->glDeleteShader(shaderID);
    }
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nDeleteTexture
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nDeleteTexture
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint texID) {
    GLuint tID = (GLuint) texID;
    if (tID != 0) {
        glDeleteTextures(1, &tID);
    }
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nFinish
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nFinish
(JNIEnv *env, jclass class) {
    glFinish();
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nGenAndBindTexture
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_GLContext_nGenAndBindTexture
(JNIEnv *env, jclass class) {
    GLuint texID;
    glGenTextures(1, &texID);
    glBindTexture(GL_TEXTURE_2D, texID);
    return texID;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nGetFBO
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_GLContext_nGetFBO
(JNIEnv *env, jclass class) {
    GLint param;
    /* The caching logic has been done on Java side if
     * platform isn't MAC or IOS. On these platforms Glass
     * can change the FBO under us. We should be able to simplify the
     * logic in Java and remove this method once once Glass stop doing it.
     */
    glGetIntegerv(GL_FRAMEBUFFER_BINDING, &param);
    return (jint) param;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nGetMaxSampleSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_GLContext_nGetMaxSampleSize
(JNIEnv *env, jclass class) {
    GLint samples;
    glGetIntegerv(GL_MAX_SAMPLES, &samples);
    return (jint)samples;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nGetUniformLocation
 * Signature: (JILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_GLContext_nGetUniformLocation
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint programID, jstring name) {
    GLint result;
    char *nameString;
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);

    if ((ctxInfo == NULL) || (name == NULL)
            || (ctxInfo->glGetUniformLocation == NULL)) {
        return 0;
    }

    nameString = strJavaToC(env, name);
    result = ctxInfo->glGetUniformLocation(programID, nameString);
    free(nameString);
    return result;
}

int translatePrismToGL(int value) {
    switch (value) {
        case com_sun_prism_es2_GLContext_GL_FLOAT:
            return GL_FLOAT;
        case com_sun_prism_es2_GLContext_GL_UNSIGNED_BYTE:
            return GL_UNSIGNED_BYTE;
        case com_sun_prism_es2_GLContext_GL_UNSIGNED_INT_8_8_8_8_REV:
            return GL_UNSIGNED_INT_8_8_8_8_REV;
        case com_sun_prism_es2_GLContext_GL_UNSIGNED_INT_8_8_8_8:
            return GL_UNSIGNED_INT_8_8_8_8;
        case com_sun_prism_es2_GLContext_GL_UNSIGNED_SHORT_8_8_APPLE:
            /* not using symbolic name may not be available on all platform - DrD*/
            return 0x85BA;

        case com_sun_prism_es2_GLContext_GL_RGBA:
            return GL_RGBA;
        case com_sun_prism_es2_GLContext_GL_BGRA:
            return GL_BGRA;
        case com_sun_prism_es2_GLContext_GL_RGB:
            return GL_RGB;
        case com_sun_prism_es2_GLContext_GL_LUMINANCE:
            return GL_LUMINANCE;
        case com_sun_prism_es2_GLContext_GL_ALPHA:
            return GL_ALPHA;
        case com_sun_prism_es2_GLContext_GL_RGBA32F:
            return GL_RGBA32F;
        case com_sun_prism_es2_GLContext_GL_YCBCR_422_APPLE:
            /* not using symbolic name may not be available on all platform - DrD*/
            return 0x85B9;

        case com_sun_prism_es2_GLContext_GL_TEXTURE_2D:
            return GL_TEXTURE_2D;
        case com_sun_prism_es2_GLContext_GL_TEXTURE_BINDING_2D:
            return GL_TEXTURE_BINDING_2D;
        case com_sun_prism_es2_GLContext_GL_NEAREST:
            return GL_NEAREST;
        case com_sun_prism_es2_GLContext_GL_LINEAR:
            return GL_LINEAR;
        case com_sun_prism_es2_GLContext_GL_NEAREST_MIPMAP_NEAREST:
            return GL_NEAREST_MIPMAP_NEAREST;
        case com_sun_prism_es2_GLContext_GL_LINEAR_MIPMAP_LINEAR:
            return GL_LINEAR_MIPMAP_LINEAR;

        case com_sun_prism_es2_GLContext_WRAPMODE_REPEAT:
            return GL_REPEAT;
        case com_sun_prism_es2_GLContext_WRAPMODE_CLAMP_TO_EDGE:
            return GL_CLAMP_TO_EDGE;
        case com_sun_prism_es2_GLContext_WRAPMODE_CLAMP_TO_BORDER:
            return GL_CLAMP_TO_BORDER;

        case com_sun_prism_es2_GLContext_GL_MAX_FRAGMENT_UNIFORM_COMPONENTS:
            return GL_MAX_FRAGMENT_UNIFORM_COMPONENTS;
        case com_sun_prism_es2_GLContext_GL_MAX_FRAGMENT_UNIFORM_VECTORS:
            return GL_MAX_FRAGMENT_UNIFORM_VECTORS;
        case com_sun_prism_es2_GLContext_GL_MAX_TEXTURE_IMAGE_UNITS:
            return GL_MAX_TEXTURE_IMAGE_UNITS;
        case com_sun_prism_es2_GLContext_GL_MAX_TEXTURE_SIZE:
            return GL_MAX_TEXTURE_SIZE;
        case com_sun_prism_es2_GLContext_GL_MAX_VARYING_COMPONENTS:
            return GL_MAX_VARYING_COMPONENTS;
        case com_sun_prism_es2_GLContext_GL_MAX_VARYING_VECTORS:
            return GL_MAX_VARYING_VECTORS;
        case com_sun_prism_es2_GLContext_GL_MAX_VERTEX_ATTRIBS:
            return GL_MAX_VERTEX_ATTRIBS;
        case com_sun_prism_es2_GLContext_GL_MAX_VERTEX_UNIFORM_COMPONENTS:
            return GL_MAX_VERTEX_UNIFORM_COMPONENTS;
        case com_sun_prism_es2_GLContext_GL_MAX_VERTEX_UNIFORM_VECTORS:
            return GL_MAX_VERTEX_UNIFORM_VECTORS;
        case com_sun_prism_es2_GLContext_GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS:
            return GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS;

        default:
            fprintf(stderr, "warning: Unknown value. Returning value = %d\n", value);
    }
    return value;
}

GLint translatePixelStore(int pname) {
    switch (pname) {
            // Use by glPixelStorei
        case com_sun_prism_es2_GLContext_GL_UNPACK_ALIGNMENT:
            return GL_UNPACK_ALIGNMENT;
        case com_sun_prism_es2_GLContext_GL_UNPACK_ROW_LENGTH:
            return GL_UNPACK_ROW_LENGTH;
        case com_sun_prism_es2_GLContext_GL_UNPACK_SKIP_PIXELS:
            return GL_UNPACK_SKIP_PIXELS;
        case com_sun_prism_es2_GLContext_GL_UNPACK_SKIP_ROWS:
            return GL_UNPACK_SKIP_ROWS;

        default:
            fprintf(stderr, "warning: Unknown pname. Returning pname = %d\n", pname);
    }
    return (GLint) pname;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nGetIntParam
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_GLContext_nGetIntParam
(JNIEnv *env, jclass class, jint pname) {
    GLint param;

    glGetIntegerv((GLenum) translatePrismToGL(pname), &param);
    return (jint) param;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nPixelStorei
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nPixelStorei
(JNIEnv *env, jclass class, jint pname, jint value) {
    glPixelStorei((GLenum) translatePixelStore(pname), (GLint) value);
}

jboolean doReadPixels(JNIEnv *env, jlong nativeCtxInfo, jint length, jobject buffer,
        jarray pixelArr, jint x, jint y, jint width, jint height) {
    GLvoid *ptr = NULL;

    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        fprintf(stderr, "doReadPixels: ctxInfo is NULL\n");
        return JNI_FALSE;
    }

    // sanity check, do we have enough memory
    // length, width and height are non-negative
    if ((length / 4 / width) < height) {
        fprintf(stderr, "doReadPixels: pixel buffer too small - length = %d\n",
                (int) length);
        return JNI_FALSE;
    }

    ptr = (GLvoid *) (pixelArr ?
            ((char *) (*env)->GetPrimitiveArrayCritical(env, pixelArr, NULL)) :
            ((char *) (*env)->GetDirectBufferAddress(env, buffer)));

    if (ptr == NULL) {
        fprintf(stderr, "doReadPixels: pixel buffer is NULL\n");
        return JNI_FALSE;
    }

    if (ctxInfo->gl2) {
        glReadPixels((GLint) x, (GLint) y, (GLsizei) width, (GLsizei) height,
                    GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, ptr);
    } else {
        jint i;
        GLubyte* c = (GLubyte*) ptr;
        GLubyte temp;
        glReadPixels((GLint) x, (GLint) y, (GLsizei) width, (GLsizei) height,
                GL_RGBA, GL_UNSIGNED_BYTE, ptr);

        for (i = 0; i < width * height; i++) {
            temp = c[0];
            c[0] = c[2];
            c[2] = temp;
            c += 4;
        }
    }

    if (pixelArr != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, pixelArr, ptr, 0);
    }
    return JNI_TRUE;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nReadPixelsByte
 * Signature: (JILjava/nio/Buffer;[BIIII)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_es2_GLContext_nReadPixelsByte
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint length, jobject buffer,
        jbyteArray pixelArr, jint x, jint y, jint w, jint h) {
    return doReadPixels(env, nativeCtxInfo, length, buffer, pixelArr, x, y, w, h);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nReadPixelsInt
 * Signature: (JILjava/nio/Buffer;[IIIII)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_es2_GLContext_nReadPixelsInt
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint length, jobject buffer,
        jintArray pixelArr, jint x, jint y, jint w, jint h) {
    return doReadPixels(env, nativeCtxInfo, length, buffer, pixelArr, x, y, w, h);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nScissorTest
 * Signature: (JZIIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nScissorTest
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jboolean enable,
        jint x, jint y, jint w, jint h) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return;
    }

    if (enable) {
        if (!ctxInfo->state.scissorEnabled) {
            glEnable(GL_SCISSOR_TEST);
            ctxInfo->state.scissorEnabled = JNI_TRUE;
        }
        glScissor(x, y, w, h);
    } else if (ctxInfo->state.scissorEnabled) {
        glDisable(GL_SCISSOR_TEST);
        ctxInfo->state.scissorEnabled = JNI_FALSE;
    }
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nTexParamsMinMax
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nTexParamsMinMax
(JNIEnv *env, jclass class, jint min, jint max) {
    GLenum param = translatePrismToGL(max);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, param);
    param = translatePrismToGL(min);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, param);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nTexImage2D0
 * Signature: (IIIIIIIILjava/lang/Object;I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_es2_GLContext_nTexImage2D0
(JNIEnv *env, jclass class, jint target, jint level, jint internalFormat,
        jint width, jint height, jint border, jint format, jint type,
        jobject pixels, jint pixelsByteOffset, jboolean useMipmap) {
    GLvoid *ptr = NULL;
    GLenum err;

    if (pixels != NULL) {
        ptr = (GLvoid *) (((char *) (*env)->GetDirectBufferAddress(env, pixels))
                + pixelsByteOffset);
    }

    glGetError();
    if (useMipmap) {
        glTexParameteri(GL_TEXTURE_2D, GL_GENERATE_MIPMAP, GL_TRUE);
    }
    glTexImage2D((GLenum) translatePrismToGL(target), (GLint) level,
            (GLint) translatePrismToGL(internalFormat),
            (GLsizei) width, (GLsizei) height, (GLint) border,
            (GLenum) translatePrismToGL(format),
            (GLenum) translatePrismToGL(type), (GLvoid *) ptr);
    err  = glGetError();

    // printGLError(err);
    return err == GL_NO_ERROR ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nTexImage2D1
 * Signature: (IIIIIIIILjava/lang/Object;I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_es2_GLContext_nTexImage2D1
(JNIEnv *env, jclass class, jint target, jint level, jint internalFormat,
        jint width, jint height, jint border, jint format, jint type,
        jobject pixels, jint pixelsByteOffset, jboolean useMipmap) {
    char *ptr = NULL;
    char *ptrPlusOffset = NULL;
    GLenum err;

    if (pixels != NULL) {
        ptr = (char *) (*env)->GetPrimitiveArrayCritical(env, pixels, NULL);
        if (ptr == NULL) {
            fprintf(stderr, "nTexImage2D1: GetPrimitiveArrayCritical returns NULL: out of memory\n");
            return JNI_FALSE;
        }
        ptrPlusOffset = ptr + pixelsByteOffset;
    }

    glGetError();
    if (useMipmap) {
        glTexParameteri(GL_TEXTURE_2D, GL_GENERATE_MIPMAP, GL_TRUE);
    }

    // It is okay if ptrPlusOffset is null.
    // In this case, a call to glTexImage2D will cause texture memory to be allocated
    // to accommodate a texture of width and height.
    glTexImage2D((GLenum) translatePrismToGL(target), (GLint) level,
            (GLint) translatePrismToGL(internalFormat),
            (GLsizei) width, (GLsizei) height, (GLint) border,
            (GLenum) translatePrismToGL(format),
            (GLenum) translatePrismToGL(type), (GLvoid *) ptrPlusOffset);

    err  = glGetError();

    if (pixels != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, pixels, ptr, 0);
    }

    // printGLError(err);
    return err == GL_NO_ERROR ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nTexSubImage2D0
 * Signature: (IIIIIIIILjava/lang/Object;I)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nTexSubImage2D0
(JNIEnv *env, jclass class, jint target, jint level,
        jint xoffset, jint yoffset, jint width, jint height, jint format,
        jint type, jobject pixels, jint pixelsByteOffset) {
    GLvoid *ptr = NULL;
    if (pixels != NULL) {
        ptr = (GLvoid *) (((char *) (*env)->GetDirectBufferAddress(env, pixels))
                + pixelsByteOffset);
    }
    glTexSubImage2D((GLenum) translatePrismToGL(target), (GLint) level,
            (GLint) xoffset, (GLint) yoffset,
            (GLsizei) width, (GLsizei) height, (GLenum) translatePrismToGL(format),
            (GLenum) translatePrismToGL(type), (GLvoid *) ptr);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nTexSubImage2D1
 * Signature: (IIIIIIIILjava/lang/Object;I)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nTexSubImage2D1
(JNIEnv *env, jclass class, jint target, jint level,
        jint xoffset, jint yoffset, jint width, jint height, jint format,
        jint type, jobject pixels, jint pixelsByteOffset) {
    char *ptr = NULL;
    char *ptrPlusOffset = NULL;
    if (pixels != NULL) {
        ptr = (char *) (*env)->GetPrimitiveArrayCritical(env, pixels, NULL);
        if (ptr == NULL) {
            fprintf(stderr, "nTexSubImage2D1: GetPrimitiveArrayCritical returns NULL: out of memory\n");
            return;
        }
        ptrPlusOffset = ptr + pixelsByteOffset;
    }
    glTexSubImage2D((GLenum) translatePrismToGL(target), (GLint) level,
            (GLint) xoffset, (GLint) yoffset,
            (GLsizei) width, (GLsizei) height, (GLenum) translatePrismToGL(format),
            (GLenum) translatePrismToGL(type), (GLvoid *) ptrPlusOffset);
    if (pixels != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, pixels, ptr, 0);
    }
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nUpdateViewport
 * Signature: (JIIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nUpdateViewport
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint x, jint y,
        jint w, jint h) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return;
    }

    glViewport((GLint) x, (GLint) y, (GLsizei) w, (GLsizei) h);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nSetMSAA
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nSetMSAA
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jboolean msaa) {
#ifndef IS_EGL
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return;
    }

    if (msaa) {
        glEnable(GL_MULTISAMPLE);
    } else {
        glDisable(GL_MULTISAMPLE);
    }
#endif
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nSetDepthTest
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nSetDepthTest
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jboolean depthTest) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return;
    }

    if (depthTest) {
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glDepthMask(GL_TRUE);
        ctxInfo->state.depthWritesEnabled = JNI_TRUE;
    } else {
        glDisable(GL_DEPTH_TEST);
        glDepthMask(GL_FALSE);
        ctxInfo->state.depthWritesEnabled = JNI_FALSE;
    }
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nUniform1f
 * Signature: (JIF)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nUniform1f
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint location, jfloat v0) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return;
    }
    ctxInfo->glUniform1f(location, v0);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nUniform2f
 * Signature: (JIFF)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nUniform2f
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint location,
        jfloat v0, jfloat v1) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return;
    }
    ctxInfo->glUniform2f(location, v0, v1);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nUniform3f
 * Signature: (JIFFF)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nUniform3f
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint location,
        jfloat v0, jfloat v1, jfloat v2) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return;
    }
    ctxInfo->glUniform3f(location, v0, v1, v2);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nUniform4f
 * Signature: (JIFFFF)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nUniform4f
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint location,
        jfloat v0, jfloat v1, jfloat v2, jfloat v3) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return;
    }
    ctxInfo->glUniform4f(location, v0, v1, v2, v3);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nUniform4fv0
 * Signature: (JIILjava/lang/Object;I)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nUniform4fv0
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint location, jint count,
        jobject value, jint valueByteOffset) {
    GLfloat *_ptr2 = NULL;
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((env == NULL) || (ctxInfo == NULL)) {
        return;
    }
    if (value != NULL) {
        _ptr2 = (GLfloat *) (((char *) (*env)->GetDirectBufferAddress(env, value))
                + valueByteOffset);
    }
    ctxInfo->glUniform4fv((GLint) location, (GLsizei) count, (GLfloat *) _ptr2);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nUniform4fv1
 * Signature: (JIILjava/lang/Object;I)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nUniform4fv1
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint location, jint count,
        jobject value, jint valueByteOffset) {
    char *ptr = NULL;
    char *ptrPlusOffset = NULL;
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((env == NULL) || (ctxInfo == NULL)) {
        return;
    }
    if (value != NULL) {
        ptr = (char *) (*env)->GetPrimitiveArrayCritical(env, value, NULL);
        if (ptr == NULL) {
            fprintf(stderr, "nUniform4fv1: GetPrimitiveArrayCritical returns NULL: out of memory\n");
            return;
        }
        ptrPlusOffset = ptr + valueByteOffset;

    }
    ctxInfo->glUniform4fv((GLint) location, (GLsizei) count, (GLfloat *) ptrPlusOffset);
    if (value != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, value, ptr, 0);
    }
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nUniform1i
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nUniform1i
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint location, jint v0) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glUniform1i == NULL)) {
        return;
    }
    ctxInfo->glUniform1i(location, v0);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nUniform2i
 * Signature: (JIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nUniform2i
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint location, jint v0, jint v1) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glUniform2i == NULL)) {
        return;
    }
    ctxInfo->glUniform2i(location, v0, v1);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nUniform3i
 * Signature: (JIIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nUniform3i
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint location,
        jint v0, jint v1, jint v2) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glUniform3i == NULL)) {
        return;
    }
    ctxInfo->glUniform3i(location, v0, v1, v2);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nUniform4i
 * Signature: (JIIIII)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nUniform4i
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint location,
        jint v0, jint v1, jint v2, jint v3) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glUniform4i == NULL)) {
        return;
    }
    ctxInfo->glUniform4i(location, v0, v1, v2, v3);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nUniform4iv0
 * Signature: (JIILjava/lang/Object;I)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nUniform4iv0
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint location, jint count,
        jobject value, jint valueByteOffset) {
    GLint *_ptr2 = NULL;
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glUniform4iv == NULL)) {
        return;
    }

    if (value != NULL) {
        _ptr2 = (GLint *) (((char *) (*env)->GetDirectBufferAddress(env, value))
                + valueByteOffset);
    }
    ctxInfo->glUniform4iv((GLint) location, (GLsizei) count, (GLint *) _ptr2);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nUniform4iv1
 * Signature: (JIILjava/lang/Object;I)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nUniform4iv1
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint location, jint count,
        jobject value, jint valueByteOffset) {
    char *ptr = NULL;
    char *ptrPlusOffset = NULL;
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glUniform4iv == NULL)) {
        return;
    }

    if (value != NULL) {
        ptr = (char *) (*env)->GetPrimitiveArrayCritical(env, value, NULL);
        if (ptr == NULL) {
            fprintf(stderr, "nUniform4iv1: GetPrimitiveArrayCritical returns NULL: out of memory\n");
            return;
        }
        ptrPlusOffset = ptr + valueByteOffset;
    }
    ctxInfo->glUniform4iv((GLint) location, (GLsizei) count, (GLint *) ptrPlusOffset);
    if (value != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, value, ptr, 0);
    }
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nUniformMatrix4fv0
 * Signature: (JIIZLjava/lang/Object;I)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nUniformMatrix4fv
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint location,
        jboolean transpose, jfloatArray values) {
    GLfloat *_ptr = NULL;
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glUniformMatrix4fv == NULL)) {
        return;
    }

    if (values != NULL) {
        _ptr = (float *)(*env)->GetPrimitiveArrayCritical(env, values, NULL);
        if (_ptr == NULL) {
            fprintf(stderr, "nUniformMatrix4fv: GetPrimitiveArrayCritical returns NULL: out of memory\n");
            return;
        }
    }
    ctxInfo->glUniformMatrix4fv((GLint) location, 1, (GLboolean) transpose, _ptr);

    if (_ptr) (*env)->ReleasePrimitiveArrayCritical(env, values, _ptr, JNI_ABORT);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nUpdateFilterState
 * Signature: (JIZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nUpdateFilterState
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint texID, jboolean linearFiler) {
    int glFilter;

    glFilter = linearFiler ? GL_LINEAR : GL_NEAREST;
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, glFilter);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, glFilter);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nUpdateWrapState
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nUpdateWrapState
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint texID, jint wrapMode) {
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S,
            (GLenum) translatePrismToGL(wrapMode));
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T,
            (GLenum) translatePrismToGL(wrapMode));
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nUseProgram
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nUseProgram
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jint pID) {
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glUseProgram == NULL)) {
        return;
    }
    ctxInfo->glUseProgram(pID);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nDisableVertexAttributes
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nDisableVertexAttributes
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    int i;
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glDisableVertexAttribArray == NULL)) {
        return;
    }

    for (i = 0; i != 4; ++i) {
        ctxInfo->glDisableVertexAttribArray(i);
    }
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nEnableVertexAttributes
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nEnableVertexAttributes
(JNIEnv *env, jclass class, jlong nativeCtxInfo) {
    int i;
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glEnableVertexAttribArray == NULL)) {
        return;
    }

    for (i = 0; i != 4; ++i) {
        ctxInfo->glEnableVertexAttribArray(i);
    }
}

#define FLOATS_PER_TC 2
#define FLOATS_PER_VC 3
#define FLOATS_PER_VERT (FLOATS_PER_TC * 2 + FLOATS_PER_VC)

#define coordStride (sizeof(float) * FLOATS_PER_VERT)
#define colorStride 4

/* NOTE: the ctx->vbFloatData and ctx->vbByteData pointers must be updated
 * whenever calling glVertexAttribPointer. Failing to do this could leave
 * the pointers in an inconsistent state.
 */

static void setVertexAttributePointers(ContextInfo *ctx, float *pFloat, char *pByte) {
    if (pFloat != ctx->vbFloatData) {
        ctx->glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, coordStride, pFloat);
        ctx->glVertexAttribPointer(2, 2, GL_FLOAT, GL_FALSE, coordStride,
            pFloat + FLOATS_PER_VC);
        ctx->glVertexAttribPointer(3, 2, GL_FLOAT, GL_FALSE, coordStride,
            pFloat + FLOATS_PER_VC + FLOATS_PER_TC);
        ctx->vbFloatData = pFloat;
    }

    if (pByte != ctx->vbByteData) {
        ctx->glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, GL_TRUE, colorStride, pByte);
        ctx->vbByteData = pByte;
    }
}
/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nDrawIndexedQuads
 * Signature: (JI[F[B)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nDrawIndexedQuads
  (JNIEnv *env, jclass class, jlong nativeCtxInfo, jint numVertices,
   jfloatArray dataf, jbyteArray datab)
{
    float *pFloat;
    char *pByte;
    int numQuads = numVertices / 4;

    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glVertexAttribPointer == NULL)) {
        return;
    }

    pFloat = (float *)(*env)->GetPrimitiveArrayCritical(env, dataf, NULL);
    pByte = (char *)(*env)->GetPrimitiveArrayCritical(env, datab, NULL);

    if (pFloat && pByte) {
        setVertexAttributePointers(ctxInfo, pFloat, pByte);
        glDrawElements(GL_TRIANGLES, numQuads * 2 * 3, GL_UNSIGNED_SHORT, 0);
    }

    if (pByte)  (*env)->ReleasePrimitiveArrayCritical(env, datab, pByte, JNI_ABORT);
    if (pFloat) (*env)->ReleasePrimitiveArrayCritical(env, dataf, pFloat, JNI_ABORT);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nCreateIndexBuffer16
 * Signature: (J[SI)I
 */
JNIEXPORT jint JNICALL Java_com_sun_prism_es2_GLContext_nCreateIndexBuffer16
  (JNIEnv *env, jclass class, jlong nativeCtxInfo, jshortArray array, jint n)
{
    GLuint id = 0;
    void *pData;

    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glBindBuffer == NULL) ||
            (ctxInfo->glBufferData == NULL) || (ctxInfo->glGenBuffers == NULL)) {
        return 0;
    }

    pData = (*env)->GetPrimitiveArrayCritical(env, array, NULL);
    if (pData) {
        ctxInfo->glGenBuffers(1, &id);
        if (id) {
            ctxInfo->glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, id);
            ctxInfo->glBufferData(GL_ELEMENT_ARRAY_BUFFER, sizeof(short) * n, pData, GL_STATIC_DRAW);
        }
    }
    if (pData) (*env)->ReleasePrimitiveArrayCritical(env, array, pData, JNI_ABORT);
    return id;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nSetIndexBuffer
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nSetIndexBuffer
  (JNIEnv *env, jclass class, jlong nativeCtxInfo, jint buffer)
{
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glBindBuffer == NULL)) {
        return;
    }
    ctxInfo->glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffer);
}

JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nSetDeviceParametersFor2D
  (JNIEnv *env, jclass class, jlong nativeCtxInfo)
{
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glBindBuffer == NULL) ||
            (ctxInfo->glBufferData == NULL) ||
            (ctxInfo->glDisableVertexAttribArray == NULL)) {
        return;
    }

    // Disable 3D states
    ctxInfo->glBindBuffer(GL_ARRAY_BUFFER, 0);
    ctxInfo->glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    ctxInfo->glDisableVertexAttribArray(VC_3D_INDEX);
    ctxInfo->glDisableVertexAttribArray(NC_3D_INDEX);
    ctxInfo->glDisableVertexAttribArray(TC_3D_INDEX);

    ctxInfo->vbFloatData = NULL;
    ctxInfo->vbByteData = NULL;

    glEnable(GL_BLEND);
    glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

    if (ctxInfo->state.scissorEnabled) {
        ctxInfo->state.scissorEnabled = JNI_FALSE;
        glDisable(GL_SCISSOR_TEST);
    }

    glCullFace(GL_BACK);
    ctxInfo->state.cullMode = GL_BACK;
    glDisable(GL_CULL_FACE);
    ctxInfo->state.cullEnable = JNI_FALSE;
#ifndef IS_EGL
    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
#endif
    ctxInfo->state.fillMode = GL_FILL;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nSetDeviceParametersFor3D
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nSetDeviceParametersFor3D
  (JNIEnv *env, jclass class, jlong nativeCtxInfo)
{
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return;
    }
    // Note: projViewTx and camPos are handled above in the Java layer

    // This setting matches 2D ((1,1-alpha); premultiplied alpha case.
    // Will need to evaluate when support proper 3D blending (alpha,1-alpha).
    glEnable(GL_BLEND);
    glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

    if (ctxInfo->state.scissorEnabled) {
        ctxInfo->state.scissorEnabled = JNI_FALSE;
        glDisable(GL_SCISSOR_TEST);
    }

    glEnable(GL_CULL_FACE);
    ctxInfo->state.cullEnable = GL_TRUE;
    glCullFace(GL_BACK);
    ctxInfo->state.cullMode = GL_BACK;
    glFrontFace(GL_CW); // set clockwise order as front-facing
#ifndef IS_EGL
    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
#endif
    ctxInfo->state.fillMode = GL_FILL;

}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nCreateES2Mesh
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_GLContext_nCreateES2Mesh
  (JNIEnv *env, jclass class, jlong nativeCtxInfo)
{
    MeshInfo *meshInfo = NULL;
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if ((ctxInfo == NULL) || (ctxInfo->glGenBuffers == NULL)) {
        return 0;
    }

    /* allocate the structure */
    meshInfo = (MeshInfo *) malloc(sizeof (MeshInfo));
    if (meshInfo == NULL) {
        fprintf(stderr, "nCreateES2Mesh: Failed in malloc\n");
        return 0;
    }

    /* initialize the structure */
    meshInfo->vboIDArray[MESH_VERTEXBUFFER] = 0;
    meshInfo->vboIDArray[MESH_INDEXBUFFER] = 0;
    meshInfo->indexBufferSize = 0;
    meshInfo->indexBufferType = 0;

    /* create vbo ids */
    ctxInfo->glGenBuffers(MESH_MAX_BUFFERS, (meshInfo->vboIDArray));

    return ptr_to_jlong(meshInfo);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nReleaseES2Mesh
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nReleaseES2Mesh
  (JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativeMeshInfo)
{
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    MeshInfo *meshInfo = (MeshInfo *) jlong_to_ptr(nativeMeshInfo);
    if ((ctxInfo == NULL) || (meshInfo == NULL) ||
            (ctxInfo->glDeleteBuffers == NULL)) {
        return;
    }

    // TODO: 3D - Native clean up. Need to determine do we have to free what
    //            is held by ES2MeshInfo.
    ctxInfo->glDeleteBuffers(MESH_MAX_BUFFERS, (GLuint *) (meshInfo->vboIDArray));
    free(meshInfo);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nBuildNativeGeometryShort
 * Signature: (JJ[FI[SI)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_es2_GLContext_nBuildNativeGeometryShort
  (JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativeMeshInfo,
        jfloatArray vbArray, jint vbSize, jshortArray ibArray, jint ibSize)
{
    GLuint vertexBufferSize;
    GLuint indexBufferSize;
    GLushort *indexBuffer;
    GLfloat *vertexBuffer;
    jboolean status = JNI_TRUE;
    GLuint uvbSize;
    GLuint uibSize;

    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    MeshInfo *meshInfo = (MeshInfo *) jlong_to_ptr(nativeMeshInfo);
    if ((ctxInfo == NULL) || (meshInfo == NULL) ||
            (vbArray == NULL) || (ibArray == NULL) ||
            (ctxInfo->glBindBuffer == NULL) ||
            (ctxInfo->glBufferData == NULL) ||
            (meshInfo->vboIDArray[MESH_VERTEXBUFFER] == 0)||
            (meshInfo->vboIDArray[MESH_INDEXBUFFER] == 0) ||
            vbSize < 0 || ibSize < 0) {
        return JNI_FALSE;
    }

    vertexBufferSize = (*env)->GetArrayLength(env, vbArray);
    indexBufferSize = (*env)->GetArrayLength(env, ibArray);
    vertexBuffer = (GLfloat *) ((*env)->GetPrimitiveArrayCritical(env, vbArray, NULL));
    indexBuffer = (GLushort *) ((*env)->GetPrimitiveArrayCritical(env, ibArray, NULL));

    uvbSize = (GLuint) vbSize;
    uibSize = (GLuint) ibSize;
    if (vertexBuffer == NULL || indexBuffer == NULL
            || uvbSize > vertexBufferSize || uibSize > indexBufferSize) {
        status = JNI_FALSE;
    }

    if (status) {
        // Initialize vertex buffer
        ctxInfo->glBindBuffer(GL_ARRAY_BUFFER, meshInfo->vboIDArray[MESH_VERTEXBUFFER]);
        ctxInfo->glBufferData(GL_ARRAY_BUFFER, uvbSize * sizeof (GLfloat),
                vertexBuffer, GL_STATIC_DRAW);

        // Initialize index buffer
        ctxInfo->glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, meshInfo->vboIDArray[MESH_INDEXBUFFER]);
        ctxInfo->glBufferData(GL_ELEMENT_ARRAY_BUFFER, uibSize * sizeof (GLushort),
                indexBuffer, GL_STATIC_DRAW);
        meshInfo->indexBufferSize = uibSize;
        meshInfo->indexBufferType = GL_UNSIGNED_SHORT;

        // Unbind VBOs
        ctxInfo->glBindBuffer(GL_ARRAY_BUFFER, 0);
        ctxInfo->glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    if (indexBuffer) {
        (*env)->ReleasePrimitiveArrayCritical(env, ibArray, indexBuffer, JNI_ABORT);
    }
    if (vertexBuffer) {
        (*env)->ReleasePrimitiveArrayCritical(env, vbArray, vertexBuffer, JNI_ABORT);
    }

    return status;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nBuildNativeGeometryInt
 * Signature: (JJ[FI[II)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_prism_es2_GLContext_nBuildNativeGeometryInt
(JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativeMeshInfo,
        jfloatArray vbArray, jint vbSize, jintArray ibArray, jint ibSize)
{
    GLuint vertexBufferSize;
    GLuint indexBufferSize;
    GLuint *indexBuffer;
    GLfloat *vertexBuffer;
    jboolean status = JNI_TRUE;
    GLuint uvbSize;
    GLuint uibSize;

    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    MeshInfo *meshInfo = (MeshInfo *) jlong_to_ptr(nativeMeshInfo);
    if ((ctxInfo == NULL) || (meshInfo == NULL) ||
            (vbArray == NULL) || (ibArray == NULL) ||
            (ctxInfo->glBindBuffer == NULL) ||
            (ctxInfo->glBufferData == NULL) ||
            (meshInfo->vboIDArray[MESH_VERTEXBUFFER] == 0)||
            (meshInfo->vboIDArray[MESH_INDEXBUFFER] == 0) ||
            vbSize < 0 || ibSize < 0) {
        return JNI_FALSE;
    }

    vertexBufferSize = (*env)->GetArrayLength(env, vbArray);
    indexBufferSize = (*env)->GetArrayLength(env, ibArray);
    vertexBuffer = (GLfloat *) ((*env)->GetPrimitiveArrayCritical(env, vbArray, NULL));
    indexBuffer = (GLuint *) ((*env)->GetPrimitiveArrayCritical(env, ibArray, NULL));

    uvbSize = (GLuint) vbSize;
    uibSize = (GLuint) ibSize;
    if (vertexBuffer == NULL || indexBuffer == NULL
            || uvbSize > vertexBufferSize || uibSize > indexBufferSize) {
        status = JNI_FALSE;
    }

    if (status) {
        // Initialize vertex buffer
        ctxInfo->glBindBuffer(GL_ARRAY_BUFFER, meshInfo->vboIDArray[MESH_VERTEXBUFFER]);
        ctxInfo->glBufferData(GL_ARRAY_BUFFER, uvbSize * sizeof (GLfloat),
                vertexBuffer, GL_STATIC_DRAW);

        // Initialize index buffer
        ctxInfo->glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, meshInfo->vboIDArray[MESH_INDEXBUFFER]);
        ctxInfo->glBufferData(GL_ELEMENT_ARRAY_BUFFER, uibSize * sizeof (GLuint),
                indexBuffer, GL_STATIC_DRAW);
        meshInfo->indexBufferSize = uibSize;
        meshInfo->indexBufferType = GL_UNSIGNED_INT;

        // Unbind VBOs
        ctxInfo->glBindBuffer(GL_ARRAY_BUFFER, 0);
        ctxInfo->glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    if (indexBuffer) {
        (*env)->ReleasePrimitiveArrayCritical(env, ibArray, indexBuffer, JNI_ABORT);
    }
    if (vertexBuffer) {
        (*env)->ReleasePrimitiveArrayCritical(env, vbArray, vertexBuffer, JNI_ABORT);
    }

    return status;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nCreateES2PhongMaterial
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_GLContext_nCreateES2PhongMaterial
  (JNIEnv *env, jclass class, jlong nativeCtxInfo)
{
    PhongMaterialInfo *pmInfo = NULL;
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    if (ctxInfo == NULL) {
        return 0;
    }

    /* allocate the structure */
    pmInfo = (PhongMaterialInfo *) malloc(sizeof (PhongMaterialInfo));
    if (pmInfo == NULL) {
        fprintf(stderr, "nCreateES2PhongMaterial: Failed in malloc\n");
        return 0;
    }

    /* initialize the structure */
    pmInfo->diffuseColor[0] = 0.0f;
    pmInfo->diffuseColor[1] = 0.0f;
    pmInfo->diffuseColor[2] = 0.0f;
    pmInfo->diffuseColor[3] = 0.0f;
    pmInfo->maps[0] = 0;
    pmInfo->maps[1] = 0;
    pmInfo->maps[2] = 0;
    pmInfo->maps[3] = 0;
    return ptr_to_jlong(pmInfo);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nReleaseES2PhongMaterial
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nReleaseES2PhongMaterial
  (JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativePhongMaterialInfo)
{
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    PhongMaterialInfo *pmInfo = (PhongMaterialInfo *) jlong_to_ptr(nativePhongMaterialInfo);
    if ((ctxInfo == NULL) || (pmInfo == NULL)) {
        return;
    }

    // We shouldn't free maps (texture) here. This freeing should be handled higher
    // in the Java layer in dealing with Texture object.

    free(pmInfo);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nSetSolidColor
 * Signature: (JJFFFF)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nSetSolidColor
  (JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativePhongMaterialInfo,
        jfloat r, jfloat g, jfloat b, jfloat a)
{
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    PhongMaterialInfo *pmInfo = (PhongMaterialInfo *) jlong_to_ptr(nativePhongMaterialInfo);
    if ((ctxInfo == NULL) || (pmInfo == NULL)) {
        return;
    }

    pmInfo->diffuseColor[0] = r;
    pmInfo->diffuseColor[1] = g;
    pmInfo->diffuseColor[2] = b;
    pmInfo->diffuseColor[3] = a;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nSetMap
 * Signature: (JJII)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nSetMap
  (JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativePhongMaterialInfo,
        jint mapType, jint texID)
{
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    PhongMaterialInfo *pmInfo = (PhongMaterialInfo *) jlong_to_ptr(nativePhongMaterialInfo);
    if ((ctxInfo == NULL) || (pmInfo == NULL)) {
        return;
    }

    // Must within the range of DIFFUSE, SPECULAR, BUMP, SELFILLUMINATION
    if ((mapType < 0) || (mapType > 3)) {
        fprintf(stderr, "nSetMap: mapType is out of bounds\n");
        return;
    }

    pmInfo->maps[mapType] = texID;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nCreateES2MeshView
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_prism_es2_GLContext_nCreateES2MeshView
  (JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativeMeshInfo)
{
    MeshViewInfo *meshViewInfo;
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    MeshInfo *meshInfo = (MeshInfo *) jlong_to_ptr(nativeMeshInfo);
    if ((ctxInfo == NULL) || (meshInfo == NULL)) {
        return 0;
    }

    /* allocate the structure */
    meshViewInfo = (MeshViewInfo *) malloc(sizeof (MeshViewInfo));
    if (meshViewInfo == NULL) {
        fprintf(stderr, "nCreateES2MeshView: Failed in malloc\n");
        return 0;
    }

    /* initialize the structure */
    meshViewInfo->meshInfo = meshInfo;
    meshViewInfo->phongMaterialInfo = NULL;
    meshViewInfo->cullEnable = GL_TRUE;
    meshViewInfo->cullMode = GL_BACK;
    meshViewInfo->fillMode = GL_FILL;
    meshViewInfo->ambientLightColor[0] = 0;
    meshViewInfo->ambientLightColor[1] = 0;
    meshViewInfo->ambientLightColor[2] = 0;
    meshViewInfo->pointLightIndex = 0;
    meshViewInfo->pointLightColor[0] = 0;
    meshViewInfo->pointLightColor[1] = 0;
    meshViewInfo->pointLightColor[2] = 0;
    meshViewInfo->pointLightPosition[0] = 0;
    meshViewInfo->pointLightPosition[1] = 0;
    meshViewInfo->pointLightPosition[2] = 0;
    meshViewInfo->pointLightWeight = 0;
    meshViewInfo->pointLightAttenuation[0] = 1;
    meshViewInfo->pointLightAttenuation[1] = 0;
    meshViewInfo->pointLightAttenuation[2] = 0;
    meshViewInfo->pointLightMaxRange = 0;

    return ptr_to_jlong(meshViewInfo);
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nReleaseES2MeshView
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nReleaseES2MeshView
  (JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativeMeshInfo)
{
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    MeshViewInfo *mvInfo = (MeshViewInfo *) jlong_to_ptr(nativeMeshInfo);
    if ((ctxInfo == NULL) || (mvInfo == NULL)) {
        return;
    }

    // TODO: 3D - Native clean up. Need to determine do we have to free what
    //            is held by ES2MeshViewInfo.
    free(mvInfo);
}

void setCullMode(ContextInfo *ctxInfo, MeshViewInfo *mvInfo) {
    if (mvInfo->cullEnable != ctxInfo->state.cullEnable) {
        if (mvInfo->cullEnable) {
            glEnable(GL_CULL_FACE);
        } else {
            glDisable(GL_CULL_FACE);
        }
        ctxInfo->state.cullEnable = mvInfo->cullEnable;
    }

    if (mvInfo->cullMode != ctxInfo->state.cullMode) {
        glCullFace(mvInfo->cullMode);
        ctxInfo->state.cullMode = mvInfo->cullMode;
    }
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nSetCullingMode
 * Signature: (JJI)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nSetCullingMode
  (JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativeMeshViewInfo,
        jint cullMode)
{
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    MeshViewInfo *meshViewInfo = (MeshViewInfo *) jlong_to_ptr(nativeMeshViewInfo);
    if ((ctxInfo == NULL) || (meshViewInfo == NULL)) {
        return;
    }
    switch (cullMode) {
        case com_sun_prism_es2_GLContext_GL_BACK:
            meshViewInfo->cullEnable = GL_TRUE;
            meshViewInfo->cullMode = GL_BACK;
            break;
        case com_sun_prism_es2_GLContext_GL_FRONT:
            meshViewInfo->cullEnable = GL_TRUE;
            meshViewInfo->cullMode = GL_FRONT;
            break;
        case com_sun_prism_es2_GLContext_GL_NONE:
            meshViewInfo->cullEnable = GL_FALSE;
            meshViewInfo->cullMode = GL_BACK;
            break;
    }
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nSetMaterial
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nSetMaterial
  (JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativeMeshViewInfo,
        jlong nativePhongMaterialInfo)
{
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    MeshViewInfo *mvInfo = (MeshViewInfo *) jlong_to_ptr(nativeMeshViewInfo);
    PhongMaterialInfo *pmInfo = (PhongMaterialInfo *) jlong_to_ptr(nativePhongMaterialInfo);
    if ((ctxInfo == NULL) || (mvInfo == NULL) || (pmInfo == NULL)) {
        return;
    }
    mvInfo->phongMaterialInfo = pmInfo;
}

void setPolyonMode(ContextInfo *ctxInfo, MeshViewInfo *mvInfo) {
#ifndef IS_EGL
    if (mvInfo->fillMode != ctxInfo->state.fillMode) {
        glPolygonMode(GL_FRONT_AND_BACK, mvInfo->fillMode);
        ctxInfo->state.fillMode = mvInfo->fillMode;
    }
#endif
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nSetWireframe
 * Signature: (JJZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nSetWireframe
  (JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativeMeshViewInfo,
        jboolean wireframe)
{
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    MeshViewInfo *meshViewInfo = (MeshViewInfo *) jlong_to_ptr(nativeMeshViewInfo);
    if ((ctxInfo == NULL) || (meshViewInfo == NULL)) {
        return;
    }
    if (wireframe) {
        meshViewInfo->fillMode = GL_LINE;
    } else {
        meshViewInfo->fillMode = GL_FILL;
    }
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nSetAmbientLight
 * Signature: (JJFFF)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nSetAmbientLight
  (JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativeMeshViewInfo,
        jfloat r, jfloat g, jfloat b)
{
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    MeshViewInfo *meshViewInfo = (MeshViewInfo *) jlong_to_ptr(nativeMeshViewInfo);
    if ((ctxInfo == NULL) || (meshViewInfo == NULL)) {
        return;
    }
    meshViewInfo->ambientLightColor[0] = r;
    meshViewInfo->ambientLightColor[1] = g;
    meshViewInfo->ambientLightColor[2] = b;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nSetPointLight
 * Signature: (JJIFFFFFFF)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nSetPointLight
  (JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativeMeshViewInfo,
        jint index, jfloat x, jfloat y, jfloat z, jfloat r, jfloat g, jfloat b, jfloat w,
        jfloat ca, jfloat la, jfloat qa, jfloat maxRange)
{
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    MeshViewInfo *meshViewInfo = (MeshViewInfo *) jlong_to_ptr(nativeMeshViewInfo);
    // NOTE: We only support up to 3 point lights at the present
    if ((ctxInfo == NULL) || (meshViewInfo == NULL) || (index < 0) || (index > 2)) {
        return;
    }
    meshViewInfo->pointLightIndex = index;
    meshViewInfo->pointLightPosition[0] = x;
    meshViewInfo->pointLightPosition[1] = y;
    meshViewInfo->pointLightPosition[2] = z;
    meshViewInfo->pointLightColor[0] = r;
    meshViewInfo->pointLightColor[1] = g;
    meshViewInfo->pointLightColor[2] = b;
    meshViewInfo->pointLightWeight = w;
    meshViewInfo->pointLightAttenuation[0] = ca;
    meshViewInfo->pointLightAttenuation[1] = la;
    meshViewInfo->pointLightAttenuation[2] = qa;
    meshViewInfo->pointLightMaxRange = maxRange;
}

/*
 * Class:     com_sun_prism_es2_GLContext
 * Method:    nRenderMeshView
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_prism_es2_GLContext_nRenderMeshView
  (JNIEnv *env, jclass class, jlong nativeCtxInfo, jlong nativeMeshViewInfo)
{
    GLuint offset = 0;
    MeshInfo *mInfo;
    ContextInfo *ctxInfo = (ContextInfo *) jlong_to_ptr(nativeCtxInfo);
    MeshViewInfo *mvInfo = (MeshViewInfo *) jlong_to_ptr(nativeMeshViewInfo);
    if ((ctxInfo == NULL) || (mvInfo == NULL) ||
            (ctxInfo->glBindBuffer == NULL) ||
            (ctxInfo->glBufferData == NULL) ||
            (ctxInfo->glDisableVertexAttribArray == NULL) ||
            (ctxInfo->glEnableVertexAttribArray == NULL) ||
            (ctxInfo->glVertexAttribPointer == NULL)) {
        return;
    }

    if ((mvInfo->phongMaterialInfo == NULL) || (mvInfo->meshInfo == NULL)) {
        return;
    }

    setCullMode(ctxInfo, mvInfo);
    setPolyonMode(ctxInfo, mvInfo);

    // Draw triangles ...
    mInfo = mvInfo->meshInfo;
    ctxInfo->glBindBuffer(GL_ARRAY_BUFFER, mInfo->vboIDArray[MESH_VERTEXBUFFER]);
    ctxInfo->glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, mInfo->vboIDArray[MESH_INDEXBUFFER]);

    ctxInfo->glEnableVertexAttribArray(VC_3D_INDEX);
    ctxInfo->glEnableVertexAttribArray(TC_3D_INDEX);
    ctxInfo->glEnableVertexAttribArray(NC_3D_INDEX);

    ctxInfo->glVertexAttribPointer(VC_3D_INDEX, VC_3D_SIZE, GL_FLOAT, GL_FALSE,
            VERT_3D_STRIDE, (const GLvoid *) jlong_to_ptr((jlong) offset));
    offset += VC_3D_SIZE * sizeof(GLfloat);
    ctxInfo->glVertexAttribPointer(TC_3D_INDEX, TC_3D_SIZE, GL_FLOAT, GL_FALSE,
            VERT_3D_STRIDE, (const GLvoid *) jlong_to_ptr((jlong) offset));
    offset += TC_3D_SIZE * sizeof(GLfloat);
    ctxInfo->glVertexAttribPointer(NC_3D_INDEX, NC_3D_SIZE, GL_FLOAT, GL_FALSE,
            VERT_3D_STRIDE, (const GLvoid *) jlong_to_ptr((jlong) offset));

    glDrawElements(GL_TRIANGLES, mvInfo->meshInfo->indexBufferSize,
            mvInfo->meshInfo->indexBufferType, 0);

    // Reset states
    ctxInfo->glDisableVertexAttribArray(VC_3D_INDEX);
    ctxInfo->glDisableVertexAttribArray(NC_3D_INDEX);
    ctxInfo->glDisableVertexAttribArray(TC_3D_INDEX);
    ctxInfo->glBindBuffer(GL_ARRAY_BUFFER, 0);
    ctxInfo->glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
}

